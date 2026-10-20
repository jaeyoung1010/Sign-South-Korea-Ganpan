package com.signkorean.app.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnSessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnViewModel
    @Inject
    constructor(
        private val loadLearnSession: LoadLearnSession,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<LearnSessionUiState>(LearnSessionUiState.Loading)
        private var loadJob: Job? = null

        val uiState = mutableUiState.asStateFlow()

        fun load() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    mutableUiState.value = loadLearnSession().toUiState()
                }
        }

        fun revealAnswer() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            mutableUiState.value = active.copy(stage = LearnCardStage.Answer)
        }
    }

private fun LoadLearnSessionResult.toUiState(): LearnSessionUiState =
    when (this) {
        is LoadLearnSessionResult.Active -> {
            LearnSessionUiState.Active(
                cardId = card.id,
                sign = card.korean,
                position = position,
                totalCount = totalCount,
            )
        }

        LoadLearnSessionResult.Completed -> {
            LearnSessionUiState.Completed
        }

        is LoadLearnSessionResult.Invalid -> {
            LearnSessionUiState.Error(error)
        }
    }
