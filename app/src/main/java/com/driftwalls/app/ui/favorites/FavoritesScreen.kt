package com.driftwalls.app.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.driftwalls.app.data.repository.ArtworkRepository
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.ui.components.ArtworkTile
import com.driftwalls.app.ui.components.LoadingState
import com.driftwalls.app.ui.components.MessageState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(repository: ArtworkRepository) : ViewModel() {
    /** Null until the first emission, so the screen can tell "loading" from "empty". */
    val favorites: StateFlow<List<Artwork>?> =
        repository.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onOpenWallpaper: (String) -> Unit, viewModel: FavoritesViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Favourites") }) }) { padding ->
        val items = favorites
        when {
            items == null -> LoadingState(Modifier.padding(padding))
            items.isEmpty() -> MessageState(
                "No favourites yet",
                "Tap the heart on any wallpaper to keep it here. Favourites can also be your rotation playlist.",
                Modifier.padding(padding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                items(items, key = { it.id }) { art -> ArtworkTile(art, onClick = { onOpenWallpaper(art.id) }) }
            }
        }
    }
}
