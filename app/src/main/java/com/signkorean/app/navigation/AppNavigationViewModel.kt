package com.signkorean.app.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel
    @Inject
    constructor() : ViewModel() {
        private val reducer = AppNavigationReducer()
        private val mutableUiState = MutableStateFlow(AppNavigationUiState())
        private val mutableEffects = MutableSharedFlow<AppNavigationEffect>(extraBufferCapacity = 1)

        val uiState = mutableUiState.asStateFlow()
        val effects = mutableEffects.asSharedFlow()

        fun dispatch(action: AppNavigationAction) {
            mutableUiState.value = reducer.reduce(mutableUiState.value, action)
            if (action is AppNavigationAction.ShowToast) {
                mutableEffects.tryEmit(AppNavigationEffect.Announce(action.message))
            }
        }

        fun openDebugDestination(screenId: String?) {
            AppNavigationIntent.routeForScreenId(screenId)?.let { route ->
                mutableUiState.value = AppNavigationUiState(backStack = listOf(route))
            }
        }
    }
