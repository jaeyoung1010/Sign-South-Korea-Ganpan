package com.signkorean.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.LearnHomeDashboard
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.feature.learn.HomeCategoryUiModel
import com.signkorean.feature.learn.HomeSessionUiModel
import com.signkorean.feature.learn.LearnHomeUiState
import com.signkorean.feature.learn.WeekActivityUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LearnHomeViewModel
    @Inject
    constructor(
        private val loadLearnHome: LoadLearnHome,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(LearnHomeUiState())
        private var refreshJob: Job? = null

        val uiState = mutableUiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    mutableUiState.value = loadLearnHome().toUiState()
                }
        }
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
