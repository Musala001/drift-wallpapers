package com.driftwalls.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.driftwalls.app.data.repository.ArtworkRepository
import com.driftwalls.app.data.wallpaper.WallpaperApplier
import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.ui.components.userMessage
import com.driftwalls.app.ui.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val artwork: Artwork) : DetailState
    data object NotFound : DetailState
    data class Failed(val message: String) : DetailState
}

sealed interface ApplyState {
    data object Idle : ApplyState
    data object Applying : ApplyState
    data class Done(val target: WallpaperTarget) : ApplyState
    data class Failed(val message: String) : ApplyState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ArtworkRepository,
    private val applier: WallpaperApplier,
) : ViewModel() {
    val wallpaperId = savedStateHandle.toRoute<DetailRoute>().wallpaperId

    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _apply = MutableStateFlow<ApplyState>(ApplyState.Idle)
    val apply: StateFlow<ApplyState> = _apply.asStateFlow()

    val isFavorite = repository.isFavorite(wallpaperId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val isInRotation = repository.isInRotation(wallpaperId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load()
    }

    fun load() {
        _state.value = DetailState.Loading
        viewModelScope.launch {
            _state.value = try {
                repository.artwork(wallpaperId)?.let { DetailState.Loaded(it) } ?: DetailState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailState.Failed(e.userMessage())
            }
        }
    }

    fun toggleFavorite() = viewModelScope.launch { repository.setFavorite(wallpaperId, !isFavorite.value) }

    fun toggleRotation() = viewModelScope.launch { repository.setInRotation(wallpaperId, !isInRotation.value) }

    fun setWallpaper(target: WallpaperTarget) {
        val artwork = (state.value as? DetailState.Loaded)?.artwork ?: return
        if (_apply.value == ApplyState.Applying) return
        _apply.value = ApplyState.Applying
        viewModelScope.launch {
            _apply.value = try {
                applier.apply(artwork.imageUrl, target)
                ApplyState.Done(target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApplyState.Failed(e.userMessage())
            }
        }
    }

    fun applyMessageShown() {
        _apply.value = ApplyState.Idle
    }
}
