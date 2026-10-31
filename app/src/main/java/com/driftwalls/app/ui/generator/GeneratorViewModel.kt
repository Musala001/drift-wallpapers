package com.driftwalls.app.ui.generator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.driftwalls.app.data.repository.ArtworkRepository
import com.driftwalls.app.data.wallpaper.WallpaperApplier
import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.generator.GeneratorSpec
import com.driftwalls.app.domain.generator.Harmony
import com.driftwalls.app.domain.generator.PatternStyle
import com.driftwalls.app.ui.detail.ApplyState
import com.driftwalls.app.ui.navigation.GeneratorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val applier: WallpaperApplier,
    private val repository: ArtworkRepository,
) : ViewModel() {
    private val initialId = savedStateHandle.toRoute<GeneratorRoute>().spec
        ?.takeIf { GeneratorSpec.parseId(it) != null }
        ?: GeneratorSpec.random().id

    /** Kept as an id in the saved state so the design survives process death. */
    val spec: StateFlow<GeneratorSpec> = savedStateHandle.getStateFlow(KEY_SPEC, initialId)
        .map { GeneratorSpec.parseId(it) ?: GeneratorSpec.random() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GeneratorSpec.parseId(initialId)!!)

    @OptIn(ExperimentalCoroutinesApi::class)
    val isInRotation: StateFlow<Boolean> = spec.flatMapLatest { repository.isInRotation(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _apply = MutableStateFlow<ApplyState>(ApplyState.Idle)
    val apply: StateFlow<ApplyState> = _apply.asStateFlow()

    fun setStyle(style: PatternStyle) = set(spec.value.copy(style = style))

    fun setHarmony(harmony: Harmony) = set(spec.value.copy(harmony = harmony))

    /** New colours and shapes, same style and harmony. */
    fun reroll() = set(spec.value.copy(seed = Random.nextLong(0, 1_000_000_000L)))

    fun toggleRotation() {
        val id = spec.value.id
        viewModelScope.launch { repository.setInRotation(id, !isInRotation.value) }
    }

    fun setWallpaper(target: WallpaperTarget) {
        if (_apply.value == ApplyState.Applying) return
        val current = spec.value
        _apply.value = ApplyState.Applying
        viewModelScope.launch {
            _apply.value = try {
                applier.apply(applier.renderGenerated(current), target)
                ApplyState.Done(target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApplyState.Failed("Couldn't set the wallpaper.")
            }
        }
    }

    fun applyMessageShown() {
        _apply.value = ApplyState.Idle
    }

    private fun set(spec: GeneratorSpec) {
        savedStateHandle[KEY_SPEC] = spec.id
    }

    private companion object {
        // Not "spec": that key holds the navigation argument.
        const val KEY_SPEC = "current_spec"
    }
}
