package com.driftwalls.app.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.SearchQuery
import kotlinx.coroutines.CancellationException

class ArtworkPagingSource(
    private val repository: ArtworkRepository,
    private val query: SearchQuery,
) : PagingSource<Int, Artwork>() {
    // Relevance can shift between pages; a repeated id would crash the keyed grid.
    private val seen = HashSet<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artwork> {
        val page = params.key ?: 1
        return try {
            val result = repository.search(query, page, PAGE_SIZE)
            val fresh = synchronized(seen) { result.items.filter { seen.add(it.id) } }
            LoadResult.Page(
                data = fresh,
                prevKey = null,
                nextKey = if (result.hasMore) page + 1 else null,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    // Always restart from the first page; museum result pages aren't stable enough to resume mid-list.
    override fun getRefreshKey(state: PagingState<Int, Artwork>): Int? = null

    companion object {
        const val PAGE_SIZE = 20
    }
}
