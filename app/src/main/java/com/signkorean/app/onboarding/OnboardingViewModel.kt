package com.signkorean.app.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.port.input.CompleteOnboarding
import com.signkorean.feature.onboarding.OnboardingChoiceReducer
import com.signkorean.feature.onboarding.OnboardingChoiceUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val completeOnboarding: CompleteOnboarding,
    ) : ViewModel() {
        private val reducer = OnboardingChoiceReducer()
        private val mutableUiState = MutableStateFlow(savedChoiceState())

        val uiState = mutableUiState.asStateFlow()

        fun selectLevel(level: LearningLevel) {
            updateChoice {
                reducer.reduce(
                    it,
                    com.signkorean.feature.onboarding.OnboardingChoiceAction
                        .SelectLevel(level),
                )
            }
        }

        fun skipLevel() {
            updateChoice { reducer.reduce(it, com.signkorean.feature.onboarding.OnboardingChoiceAction.SkipLevel) }
        }

        fun toggleInterest(interest: Interest) {
            updateChoice {
                reducer.reduce(
                    it,
                    com.signkorean.feature.onboarding.OnboardingChoiceAction
                        .ToggleInterest(interest),
                )
            }
        }

        fun completeOnboarding() {
            val state = mutableUiState.value
            val level = state.selectedLevel ?: return
            if (state.isSaving || state.isCompleted) return

            mutableUiState.value = state.copy(isSaving = true, hasSaveError = false)
            viewModelScope.launch {
                runCatching { completeOnboarding(level, state.selectedInterests) }
                    .onSuccess {
                        mutableUiState.value = mutableUiState.value.copy(isSaving = false, isCompleted = true)
                    }.onFailure {
                        mutableUiState.value = mutableUiState.value.copy(isSaving = false, hasSaveError = true)
                    }
            }
        }

        private fun updateChoice(transform: (OnboardingChoiceUiState) -> OnboardingChoiceUiState) {
            if (mutableUiState.value.isSaving || mutableUiState.value.isCompleted) return
            mutableUiState.value = transform(mutableUiState.value)
            persistChoiceState(mutableUiState.value)
        }

        private fun savedChoiceState(): OnboardingChoiceUiState =
            OnboardingChoiceUiState(
                selectedLevel = savedStateHandle.get<String>(LEVEL_KEY)?.toLearningLevelOrNull(),
                selectedInterests =
                    savedStateHandle
                        .get<ArrayList<String>>(INTERESTS_KEY)
                        .orEmpty()
                        .mapNotNull { it.toInterestOrNull() }
                        .toSet(),
            )

        private fun persistChoiceState(state: OnboardingChoiceUiState) {
            savedStateHandle[LEVEL_KEY] = state.selectedLevel?.name
            savedStateHandle[INTERESTS_KEY] = ArrayList(state.selectedInterests.map(Interest::name))
        }

        private fun String.toLearningLevelOrNull(): LearningLevel? =
            LearningLevel.entries
                .firstOrNull { it.name == this }

        private fun String.toInterestOrNull(): Interest? = Interest.entries.firstOrNull { it.name == this }

        private companion object {
            const val LEVEL_KEY = "onboarding-level"
            const val INTERESTS_KEY = "onboarding-interests"
        }
    }
