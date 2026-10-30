package com.driftwalls.app.data.remote.met

import com.driftwalls.app.data.local.ArtworkDao
import com.driftwalls.app.data.local.MetRejectedEntity
import com.driftwalls.app.data.local.toDomain
import com.driftwalls.app.data.local.toEntity
import com.driftwalls.app.data.remote.ArtSource
import com.driftwalls.app.data.remote.SourcePage
import com.driftwalls.app.domain.image.PortraitFit
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.model.Museum
import com.driftwalls.app.domain.model.SearchQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Met's search returns object ids only, and doesn't filter by public domain, so each id costs
 * a request. Usable objects are kept in Room and rejected ids are remembered, so a page is only
 * expensive the first time.
 */
@Singleton
class MetArtSource @Inject constructor(
    private val api: MetApi,
    private val dao: ArtworkDao,
) : ArtSource {
    override val museum = Museum.MET

    override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): SourcePage {
        val (q, departmentId) = when (query) {
            is SearchQuery.Text -> query.text to null
            is SearchQuery.InCategory -> categoryQuery(query.category) ?: return SourcePage.EMPTY
        }
        val offset = (page - 1) * pageSize
        if (offset + pageSize > MetApi.MAX_RESULTS) return SourcePage.EMPTY

        val response = api.search(q = q, departmentId = departmentId, offset = offset, limit = pageSize)
        val ids = response.objectIDs.orEmpty()
        val total = minOf(response.total, MetApi.MAX_RESULTS)
        return SourcePage(
            items = PortraitFit.rank(resolve(ids)),
            hasMore = ids.isNotEmpty() && offset + ids.size < total,
        )
    }

    override suspend fun fetch(sourceId: String): Artwork? {
        val id = sourceId.toLongOrNull() ?: return null
        return resolve(listOf(id)).firstOrNull()
    }

    /** Looks up ids in search order, fetching the ones we haven't seen. */
    private suspend fun resolve(ids: List<Long>): List<Artwork> = coroutineScope {
        if (ids.isEmpty()) return@coroutineScope emptyList()
        val now = System.currentTimeMillis()
        val cached = dao.byIds(ids.map { Artwork.wallpaperId(museum, it.toString()) })
            .associateBy { it.sourceId.toLong() }
        val rejected = dao.metRejectedAmong(ids, since = now - REJECTED_TTL_MS).toSet()

        val gate = Semaphore(MAX_PARALLEL_FETCHES)
        val lookups = ids.filterNot { it in rejected }.map { id ->
            val hit = cached[id]
            if (hit != null) {
                async { Lookup.Found(hit.toDomain(), fresh = false) }
            } else {
                async { gate.withPermit { fetchObject(id) } }
            }
        }.awaitAll()

        val found = lookups.filterIsInstance<Lookup.Found>()
        found.filter { it.fresh }.takeIf { it.isNotEmpty() }
            ?.let { fresh -> dao.upsert(fresh.map { it.artwork.toEntity(now) }) }
        lookups.filterIsInstance<Lookup.Rejected>().takeIf { it.isNotEmpty() }
            ?.let { rejects -> dao.markMetRejected(rejects.map { MetRejectedEntity(it.objectId, now) }) }

        // Some objects failing is fine; all of them failing means we're offline or the API is down.
        val failure = lookups.filterIsInstance<Lookup.Failed>().firstOrNull()
        if (found.isEmpty() && failure != null) throw failure.error
        found.map { it.artwork }
    }

    private suspend fun fetchObject(id: Long): Lookup = try {
        MetMapper.toArtwork(api.objectById(id))?.let { Lookup.Found(it, fresh = true) } ?: Lookup.Rejected(id)
    } catch (e: HttpException) {
        if (e.code() == 404) Lookup.Rejected(id) else Lookup.Failed(e)
    } catch (e: IOException) {
        Lookup.Failed(e)
    }

    private sealed interface Lookup {
        data class Found(val artwork: Artwork, val fresh: Boolean) : Lookup
        data class Rejected(val objectId: Long) : Lookup
        data class Failed(val error: Exception) : Lookup
    }

    companion object {
        private const val MAX_PARALLEL_FETCHES = 4

        /** Rights can change, so rejected ids are re-checked after a month. */
        private const val REJECTED_TTL_MS = 30L * 24 * 60 * 60 * 1000

        fun categoryQuery(category: Category): Pair<String, Int>? = when (category) {
            Category.IMPRESSIONISM -> "impressionism" to MetApi.DEPT_EUROPEAN_PAINTINGS
            Category.LANDSCAPES -> "landscape" to MetApi.DEPT_EUROPEAN_PAINTINGS
            Category.JAPANESE_PRINTS -> "ukiyo-e" to MetApi.DEPT_ASIAN_ART
            Category.BOTANICAL -> "flowers" to MetApi.DEPT_DRAWINGS_AND_PRINTS
            Category.GENERATED -> null
        }
    }
}
