package com.driftwalls.app.ui.rotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftwalls.app.data.local.FavoriteDao
import com.driftwalls.app.data.local.RotationHistoryDao
import com.driftwalls.app.data.local.RotationHistoryItem
import com.driftwalls.app.data.local.RotationPickDao
import com.driftwalls.app.data.rotation.RotationEngine
import com.driftwalls.app.data.rotation.RotationResult
import com.driftwalls.app.data.rotation.RotationScheduler
import com.driftwalls.app.data.rotation.RotationSettings
import com.driftwalls.app.data.rotation.RotationSettingsStore
import com.driftwalls.app.data.rotation.networkNeed
import com.driftwalls.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RotateNowState {
    data object Idle : RotateNowState
    data object Running : RotateNowState
    data class Message(val text: String) : RotateNowState
}

@HiltViewModel
class RotationViewModel @Inject constructor(
    private val settingsStore: RotationSettingsStore,
    private val engine: RotationEngine,
    private val scheduler: RotationScheduler,
    favoriteDao: FavoriteDao,
    pickDao: RotationPickDao,
    historyDao: RotationHistoryDao,
) : ViewModel() {
    /** Null until DataStore has been read, so the screen doesn't flash the defaults. */
    val settings: StateFlow<RotationSettings?> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val favoriteCount = favoriteDao.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val pickCount = pickDao.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val history: StateFlow<List<RotationHistoryItem>> =
        historyDao.observeRecent(HISTORY_SHOWN).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _rotateNow = MutableStateFlow<RotateNowState>(RotateNowState.Idle)
    val rotateNow: StateFlow<RotateNowState> = _rotateNow.asStateFlow()

    val nextRunMillis: StateFlow<Long?> =
        scheduler.nextRunMillis().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (RotationSettings) -> RotationSettings) {
        viewModelScope.launch {
            val before = settingsStore.current()
            val after = settingsStore.update(transform)
            // Only touch WorkManager when something it depends on changed.
            if (after.enabled != before.enabled || after.interval != before.interval || after.networkNeed() != before.networkNeed()) {
                scheduler.sync(after)
            }
        }
    }

    fun next() {
        if (_rotateNow.value == RotateNowState.Running) return
        _rotateNow.value = RotateNowState.Running
        viewModelScope.launch {
            _rotateNow.value = try {
                when (val result = engine.rotateNow()) {
                    is RotationResult.Applied -> RotateNowState.Message("New wallpaper set")
                    is RotationResult.Skipped -> RotateNowState.Message(result.reason)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RotateNowState.Message(e.userMessage())
            }
        }
    }

    fun messageShown() {
        _rotateNow.value = RotateNowState.Idle
    }

    private companion object {
        const val HISTORY_SHOWN = 20
    }
}
