package com.signkorean.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.LearnHomeDashboard
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.StartSessionRequest
import com.signkorean.domain.port.input.StartSessionResult
import com.signkorean.feature.learn.HomeCategoryUiModel
import com.signkorean.feature.learn.HomeSessionUiModel
import com.signkorean.feature.learn.LearnHomeUiState
import com.signkorean.feature.learn.WeekActivityUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LearnHomeViewModel
    @Inject
    constructor(
        private val loadLearnHome: LoadLearnHome,
        private val startSession: StartSession,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(LearnHomeUiState())
        private val effectChannel = Channel<HomeSessionEffect>(Channel.BUFFERED)
        private var refreshJob: Job? = null
        private val startMutex = Mutex()

        val uiState = mutableUiState.asStateFlow()
        val effects = effectChannel.receiveAsFlow()

        init {
            refresh()
        }

        fun refresh() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    val dashboard = loadLearnHome()
                    val transientState = mutableUiState.value
                    mutableUiState.value =
                        dashboard.toUiState().copy(
                            isStarting = transientState.isStarting,
                            hasStartError = transientState.hasStartError,
                            pendingCategoryReplacement = transientState.pendingCategoryReplacement,
                        )
                }
        }

        fun startDailySession() {
            start(StartSessionRequest.daily())
        }

        fun startCategorySession(category: String) {
            start(StartSessionRequest.category(category))
        }

        fun confirmCategoryReplacement() {
            val category = mutableUiState.value.pendingCategoryReplacement ?: return
            mutableUiState.value = mutableUiState.value.copy(pendingCategoryReplacement = null)
            start(StartSessionRequest.category(category, replaceActiveSession = true))
        }

        fun cancelCategoryReplacement() {
            mutableUiState.value = mutableUiState.value.copy(pendingCategoryReplacement = null)
        }

        private fun start(request: StartSessionRequest) {
            if (!startMutex.tryLock()) return

            viewModelScope.launch {
                mutableUiState.value = mutableUiState.value.copy(isStarting = true, hasStartError = false)
                try {
                    when (val result = startSession(request)) {
                        is StartSessionResult.Started,
                        is StartSessionResult.Resumed,
                        -> {
                            effectChannel.trySend(HomeSessionEffect.NavigateToLearn)
                        }

                        is StartSessionResult.ReplacementRequired -> {
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    pendingCategoryReplacement = request.category,
                                )
                        }

                        StartSessionResult.NoCardsAvailable -> {
                            mutableUiState.value = mutableUiState.value.copy(hasStartError = true)
                        }
                    }
                } catch (_: Exception) {
                    mutableUiState.value = mutableUiState.value.copy(hasStartError = true)
                } finally {
                    mutableUiState.value = mutableUiState.value.copy(isStarting = false)
                    startMutex.unlock()
                }
            }
        }
    }

sealed interface HomeSessionEffect {
    data object NavigateToLearn : HomeSessionEffect
}

private fun LearnHomeDashboard.toUiState(): LearnHomeUiState =
    LearnHomeUiState(
        streakDays = streakDays,
        session =
            HomeSessionUiModel(
                length = session.length,
                estimatedMinutes = session.estimatedMinutes,
                reviewCount = session.reviewCount,
                newCount = session.newCount,
                completedCount = session.activeSession?.completedCount,
            ),
        categories = categories.map { HomeCategoryUiModel(it.key, it.label, it.cardCount) },
        week =
            weeklyActivity.map {
                WeekActivityUiModel(
                    label = it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    count = it.count,
                )
            },
    )
