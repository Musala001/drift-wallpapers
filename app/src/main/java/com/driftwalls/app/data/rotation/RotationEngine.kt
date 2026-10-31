package com.driftwalls.app.data.rotation

import android.graphics.Bitmap
import com.driftwalls.app.data.local.FavoriteDao
import com.driftwalls.app.data.local.RotationHistoryDao
import com.driftwalls.app.data.local.RotationHistoryEntity
import com.driftwalls.app.data.local.RotationPickDao
import com.driftwalls.app.data.repository.ArtworkRepository
import com.driftwalls.app.data.wallpaper.LiveWallpaperStore
import com.driftwalls.app.data.wallpaper.WallpaperApplier
import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.generator.GeneratorSpec
import com.driftwalls.app.domain.model.SearchQuery
import com.driftwalls.app.domain.rotation.RotationOrder
import com.driftwalls.app.domain.rotation.RotationPlanner
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface RotationResult {
    data class Applied(val wallpaperId: String) : RotationResult

    /** Nothing to rotate to, e.g. an empty favourites list. [reason] is shown to the user. */
    data class Skipped(val reason: String) : RotationResult
}

/** Picks the next wallpaper from the configured source and applies it. Used by the UI and the scheduler. */
@Singleton
class RotationEngine @Inject constructor(
    private val settingsStore: RotationSettingsStore,
    private val repository: ArtworkRepository,
    private val favoriteDao: FavoriteDao,
    private val pickDao: RotationPickDao,
    private val historyDao: RotationHistoryDao,
    private val applier: WallpaperApplier,
    private val live: LiveWallpaperStore,
) {
    // A manual "next" and a scheduled run must not apply two wallpapers at once.
    private val mutex = Mutex()

    /**
     * Throws [IOException] when the image or the candidate list couldn't be downloaded.
     *
     * @param forceLive send the image to the live wallpaper even if the system doesn't report it
     *   as active yet (it calls this right after being chosen). Never falls back to replacing it.
     */
    suspend fun rotateNow(forceLive: Boolean = false): RotationResult = mutex.withLock {
        val settings = settingsStore.current()
        // A lock-only rotation still sets a still image on the lock screen beside the live wallpaper.
        val toLive = forceLive || (live.isActive() && settings.target != WallpaperTarget.LOCK)
        val history = historyDao.recentIds(HISTORY_LOOKBACK)
        val candidates = candidates(settings).toMutableList()
        if (candidates.isEmpty()) return RotationResult.Skipped(emptyReason(settings.source))

        // An artwork can disappear (withdrawn, no longer public domain); try a few before giving up.
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) {
            val id = RotationPlanner.next(candidates, history, settings.order) ?: return@repeat
            try {
                val bitmap = load(id, if (toLive) LiveWallpaperStore.PARALLAX_WIDTH else 1f)
                if (bitmap != null) {
                    if (toLive) live.publish(bitmap) else applier.apply(bitmap, settings.target)
                    historyDao.insert(RotationHistoryEntity(wallpaperId = id, appliedAt = System.currentTimeMillis()))
                    historyDao.prune(HISTORY_KEEP)
                    return RotationResult.Applied(id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
            candidates.remove(id)
        }
        lastError?.let { throw it as? IOException ?: IOException("Couldn't set the next wallpaper", it) }
        RotationResult.Skipped("None of the wallpapers in this source are available right now.")
    }

    private suspend fun candidates(settings: RotationSettings): List<String> = when (settings.source) {
        RotationSource.FAVORITES -> favoriteDao.ids()
        RotationSource.PICKS -> pickDao.ids()
        RotationSource.CATEGORY -> if (settings.category.isGenerated) {
            // A fresh design every time; nothing to download.
            listOf(GeneratorSpec.random().id)
        } else {
            // Shuffle draws from a few pages so a category doesn't settle on the same 30 images.
            val page = if (settings.order == RotationOrder.SHUFFLE) Random.nextInt(1, CATEGORY_PAGES + 1) else 1
            repository.search(SearchQuery.InCategory(settings.category), page, CATEGORY_PAGE_SIZE).items.map { it.id }
        }
    }

    private suspend fun load(id: String, widthFactor: Float): Bitmap? {
        GeneratorSpec.parseId(id)?.let { return applier.renderGenerated(it, widthFactor) }
        val artwork = repository.artwork(id) ?: return null
        return applier.loadScreenBitmap(artwork.imageUrl, widthFactor)
    }

    private fun emptyReason(source: RotationSource) = when (source) {
        RotationSource.FAVORITES -> "Your favourites are empty. Tap the heart on a wallpaper to add it."
        RotationSource.PICKS -> "Your rotation list is empty. Add wallpapers from their detail screen."
        RotationSource.CATEGORY -> "This category has no wallpapers right now."
    }

    private companion object {
        const val HISTORY_LOOKBACK = 100
        const val HISTORY_KEEP = 200
        const val MAX_ATTEMPTS = 3
        const val CATEGORY_PAGES = 4
        const val CATEGORY_PAGE_SIZE = 30
    }
}
