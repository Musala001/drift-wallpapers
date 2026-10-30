package com.driftwalls.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.driftwalls.app.data.repository.ArtworkPagingSource
import com.driftwalls.app.data.repository.ArtworkRepository
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.model.SearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: ArtworkRepository) : ViewModel() {
    private val _query = MutableStateFlow<SearchQuery>(SearchQuery.InCategory(Category.IMPRESSIONISM))
    val query: StateFlow<SearchQuery> = _query.asStateFlow()

    private var lastCategory = Category.IMPRESSIONISM

    @OptIn(ExperimentalCoroutinesApi::class)
    val artworks: Flow<PagingData<Artwork>> = _query
        .filterNot { it is SearchQuery.InCategory && it.category.isGenerated }
        .flatMapLatest { query ->
            Pager(
                PagingConfig(
                    pageSize = ArtworkPagingSource.PAGE_SIZE,
                    initialLoadSize = ArtworkPagingSource.PAGE_SIZE,
                    prefetchDistance = 8,
                    enablePlaceholders = false,
                ),
            ) { ArtworkPagingSource(repository, query) }.flow
        }
        .cachedIn(viewModelScope)

    fun selectCategory(category: Category) {
        lastCategory = category
        _query.value = SearchQuery.InCategory(category)
    }

    fun search(text: String) {
        val trimmed = text.trim()
        _query.value = if (trimmed.isEmpty()) SearchQuery.InCategory(lastCategory) else SearchQuery.Text(trimmed)
    }
}
