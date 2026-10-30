package com.driftwalls.app.data.remote.aic

import com.driftwalls.app.data.remote.ArtSource
import com.driftwalls.app.data.remote.SourcePage
import com.driftwalls.app.domain.image.PortraitFit
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.model.Museum
import com.driftwalls.app.domain.model.SearchQuery
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AicArtSource @Inject constructor(private val api: AicApi) : ArtSource {
    override val museum = Museum.AIC

    override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): SourcePage {
        val limit = pageSize.coerceAtMost(AicApi.MAX_LIMIT)
        if (page * limit > AicApi.MAX_RESULTS) return SourcePage.EMPTY
        val params = when (query) {
            is SearchQuery.Text -> AicQuery.search(query.text, page, limit)
            is SearchQuery.InCategory -> categoryParams(query.category, page, limit) ?: return SourcePage.EMPTY
        }
        val response = api.search(params)
        // The server already filtered to public domain; check again in case that ever changes.
        val items = response.data.mapNotNull { AicMapper.toArtwork(it, response.config?.iiifUrl) }
        return SourcePage(
            items = PortraitFit.rank(items),
            hasMore = page < response.pagination.totalPages && (page + 1) * limit <= AicApi.MAX_RESULTS,
        )
    }

    override suspend fun fetch(sourceId: String): Artwork? = try {
        val response = api.artwork(sourceId)
        AicMapper.toArtwork(response.data, response.config?.iiifUrl)
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    companion object {
        /** `q` only ranks results; the extra filter keeps deep pages on topic. */
        fun categoryParams(category: Category, page: Int, limit: Int): String? = when (category) {
            Category.IMPRESSIONISM ->
                AicQuery.search("impressionism", page, limit, listOf(AicQuery.match("style_titles", "impressionism")))
            Category.LANDSCAPES ->
                AicQuery.search("landscape", page, limit, listOf(AicQuery.match("classification_titles", "painting")))
            Category.JAPANESE_PRINTS ->
                AicQuery.search(
                    "japanese landscape",
                    page,
                    limit,
                    listOf(AicQuery.term("classification_titles.keyword", "woodblock print")),
                )
            Category.BOTANICAL -> AicQuery.search("botanical flowers", page, limit)
            Category.GENERATED -> null
        }
    }
}
