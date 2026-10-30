package com.driftwalls.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.model.SearchQuery
import com.driftwalls.app.ui.components.ArtworkTile
import com.driftwalls.app.ui.components.LoadingState
import com.driftwalls.app.ui.components.MessageState
import com.driftwalls.app.ui.components.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWallpaper: (String) -> Unit,
    onOpenAbout: () -> Unit,
    generatedContent: @Composable (Modifier) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val artworks = viewModel.artworks.collectAsLazyPagingItems()
    var searchText by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drift") },
                actions = {
                    IconButton(onClick = onOpenAbout) { Icon(Icons.Outlined.Info, contentDescription = "About and credits") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search public-domain art") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            viewModel.search("")
                        }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search") }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search(searchText)
                    focus.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Category.entries) { category ->
                    FilterChip(
                        selected = (query as? SearchQuery.InCategory)?.category == category,
                        onClick = {
                            searchText = ""
                            focus.clearFocus()
                            viewModel.selectCategory(category)
                        },
                        label = { Text(category.displayName) },
                    )
                }
            }
            val current = query
            if (current is SearchQuery.InCategory && current.category.isGenerated) {
                generatedContent(Modifier.fillMaxSize())
            } else {
                ArtworkGrid(artworks, onOpenWallpaper)
            }
        }
    }
}

@Composable
private fun ArtworkGrid(artworks: LazyPagingItems<Artwork>, onOpen: (String) -> Unit) {
    val refresh = artworks.loadState.refresh
    when {
        refresh is LoadState.Loading && artworks.itemCount == 0 -> LoadingState()
        refresh is LoadState.Error && artworks.itemCount == 0 -> MessageState(
            title = "Couldn't load artworks",
            detail = refresh.error.userMessage(),
            actionLabel = "Retry",
            onAction = artworks::retry,
        )
        refresh is LoadState.NotLoading && artworks.itemCount == 0 -> MessageState(
            title = "No artworks found",
            detail = "Nothing public-domain matched. Try another search or category.",
        )
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(artworks.itemCount, key = artworks.itemKey { it.id }) { index ->
                artworks[index]?.let { art -> ArtworkTile(art, onClick = { onOpen(art.id) }) }
            }
            when (val append = artworks.loadState.append) {
                is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = artworks::retry) { Text("Couldn't load more. Tap to retry") }
                    }
                }
                else -> Unit
            }
        }
    }
}
