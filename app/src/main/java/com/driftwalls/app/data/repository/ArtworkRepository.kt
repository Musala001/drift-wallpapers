package com.driftwalls.app.data.repository

import com.driftwalls.app.data.local.ArtworkDao
import com.driftwalls.app.data.local.FavoriteDao
import com.driftwalls.app.data.local.FavoriteEntity
import com.driftwalls.app.data.local.toDomain
import com.driftwalls.app.data.local.toEntity
import com.driftwalls.app.data.remote.ArtSource
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum
import com.driftwalls.app.domain.model.SearchQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One page of results merged from every museum. If one museum fails but another answers, the page
 * still loads and [failedSources] says which were missing.
 */
data class SearchPage(
    val items: List<Artwork>,
    val hasMore: Boolean,
    val failedSources: List<Museum> = emptyList(),
)

@Singleton
class ArtworkRepository @Inject constructor(
    private val sources: List<@JvmSuppressWildcards ArtSource>,
    private val artworkDao: ArtworkDao,
    private val favoriteDao: FavoriteDao,
) {
    /** [page] is 1-based. Throws only when every source failed. */
    suspend fun search(query: SearchQuery, page: Int, pageSize: Int): SearchPage = coroutineScope {
        val perSource = (pageSize / sources.size.coerceAtLeast(1)).coerceAtLeast(1)
        val results = sources.map { source ->
            async {
                try {
                    Result.success(source.search(query, page, perSource))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }.awaitAll()

        val pages = results.mapNotNull { it.getOrNull() }
        if (pages.isEmpty() && results.isNotEmpty()) throw results.first().exceptionOrNull()!!

        val items = interleave(pages.map { it.items }).distinctBy { it.id }
        if (items.isNotEmpty()) artworkDao.upsert(items.map { it.toEntity(System.currentTimeMillis()) })
        SearchPage(
            items = items,
            hasMore = pages.any { it.hasMore },
            failedSources = sources.zip(results).filter { it.second.isFailure }.map { it.first.museum },
        )
    }

    /** Cached copy first; otherwise asks the museum. Null for unknown or unusable ids. */
    suspend fun artwork(id: String): Artwork? {
        artworkDao.byId(id)?.let { return it.toDomain() }
        val (museum, sourceId) = Artwork.parseId(id) ?: return null
        val artwork = sources.firstOrNull { it.museum == museum }?.fetch(sourceId) ?: return null
        artworkDao.upsert(listOf(artwork.toEntity(System.currentTimeMillis())))
        return artwork
    }

    fun favorites(): Flow<List<Artwork>> =
        favoriteDao.observeFavoriteArtworks().map { list -> list.map { it.toDomain() } }

    fun isFavorite(id: String): Flow<Boolean> = favoriteDao.observeIsFavorite(id)

    suspend fun setFavorite(id: String, favorite: Boolean) {
        if (favorite) favoriteDao.add(FavoriteEntity(id, System.currentTimeMillis())) else favoriteDao.remove(id)
    }

    companion object {
        /** Round-robin merge so the grid alternates museums instead of showing one block then the other. */
        fun <T> interleave(lists: List<List<T>>): List<T> {
            val out = ArrayList<T>(lists.sumOf { it.size })
            val longest = lists.maxOfOrNull { it.size } ?: 0
            for (i in 0 until longest) lists.forEach { list -> list.getOrNull(i)?.let(out::add) }
            return out
        }
    }
}
