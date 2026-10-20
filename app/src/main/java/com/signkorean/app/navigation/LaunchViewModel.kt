package com.signkorean.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.AppInitializationResult
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.port.input.InitializeApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel
    @Inject
    constructor(
        private val initializeApp: InitializeApp,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<LaunchUiState>(LaunchUiState.Loading)
        private var initializationJob: Job? = null

        val uiState = mutableUiState.asStateFlow()

        init {
            retry()
        }

        fun retry() {
            initializationJob?.cancel()
            initializationJob =
                viewModelScope.launch {
                    mutableUiState.value = LaunchUiState.Loading
                    mutableUiState.value = initializeApp().toUiState()
                }
        }
    }

sealed interface LaunchUiState {
    data object Loading : LaunchUiState

    data class Ready(
        val destination: LaunchDestination,
        val hasResumableSession: Boolean,
    ) : LaunchUiState

    data class Error(
        val failure: LaunchFailure,
    ) : LaunchUiState
}

enum class LaunchFailure {
    Database,
    Content,
}

private fun AppInitializationResult.toUiState(): LaunchUiState =
    when (this) {
        is AppInitializationResult.Ready -> LaunchUiState.Ready(destination, hasResumableSession)
        AppInitializationResult.RecoverableFailure.Database -> LaunchUiState.Error(LaunchFailure.Database)
        AppInitializationResult.RecoverableFailure.Content -> LaunchUiState.Error(LaunchFailure.Content)
    }
