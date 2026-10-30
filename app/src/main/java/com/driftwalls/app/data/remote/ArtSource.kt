package com.driftwalls.app.data.remote

import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum
import com.driftwalls.app.domain.model.SearchQuery

/** One museum's API. Implementations return only public-domain artworks with an image. */
interface ArtSource {
    val museum: Museum

    /** [page] is 1-based. Throws on network/HTTP failure so callers can tell "failed" from "empty". */
    suspend fun search(query: SearchQuery, page: Int, pageSize: Int): SourcePage

    /** Null if the artwork doesn't exist or isn't usable (not public domain, no image). */
    suspend fun fetch(sourceId: String): Artwork?
}

data class SourcePage(val items: List<Artwork>, val hasMore: Boolean) {
    companion object {
        val EMPTY = SourcePage(emptyList(), hasMore = false)
    }
}
