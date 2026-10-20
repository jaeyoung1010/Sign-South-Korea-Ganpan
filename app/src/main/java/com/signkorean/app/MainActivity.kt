@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.signkorean.app.home.LearnHomeViewModel
import com.signkorean.app.navigation.AppNavigationAction
import com.signkorean.app.navigation.AppNavigationIntent
import com.signkorean.app.navigation.AppNavigationUiState
import com.signkorean.app.navigation.AppNavigationViewModel
import com.signkorean.app.navigation.AppRoute
import com.signkorean.app.navigation.AppSheet
import com.signkorean.app.navigation.AppTopLevelTab
import com.signkorean.app.navigation.LaunchFailure
import com.signkorean.app.navigation.LaunchUiState
import com.signkorean.app.navigation.LaunchViewModel
import com.signkorean.app.onboarding.OnboardingViewModel
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameSheet
import com.signkorean.designsystem.components.GameToast
import com.signkorean.designsystem.layout.SignKoreanScreen
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.feature.learn.CompletionScreen
import com.signkorean.feature.learn.CompletionUiState
import com.signkorean.feature.learn.LearnHomeScreen
import com.signkorean.feature.learn.LearnHomeUiState
import com.signkorean.feature.learn.LearnSessionScreen
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnUiAction
import com.signkorean.feature.onboarding.OnboardingChoiceUiState
import com.signkorean.feature.onboarding.OnboardingScreen
import com.signkorean.feature.onboarding.OnboardingStep
import com.signkorean.feature.onboarding.OnboardingUiAction
import com.signkorean.feature.onboarding.OnboardingUiState
import com.signkorean.feature.progress.ProgressScreen
import com.signkorean.feature.progress.ProgressUiState
import com.signkorean.feature.settings.LegalScreen
import com.signkorean.feature.settings.LegalUiState
import com.signkorean.feature.settings.SettingsScreen
import com.signkorean.feature.settings.SettingsUiAction
import com.signkorean.feature.settings.SettingsUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val navigationViewModel: AppNavigationViewModel by viewModels()
    private val launchViewModel: LaunchViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val learnHomeViewModel: LearnHomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        navigationViewModel.openDebugDestination(intent.getStringExtra(AppNavigationIntent.EXTRA_DEBUG_SCREEN))
        setContent { SignKoreanApp(navigationViewModel, launchViewModel, onboardingViewModel, learnHomeViewModel) }
    }
}

@Composable
private fun SignKoreanApp(
    navigationViewModel: AppNavigationViewModel,
    launchViewModel: LaunchViewModel,
    onboardingViewModel: OnboardingViewModel,
    learnHomeViewModel: LearnHomeViewModel,
) {
    val state by navigationViewModel.uiState.collectAsState()
    val launchState by launchViewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()
    val learnHomeState by learnHomeViewModel.uiState.collectAsState()
    LaunchedEffect(state.currentRoute) {
        if (state.currentRoute == AppRoute.Home) {
            learnHomeViewModel.refresh()
        }
    }
    LaunchedEffect(learnHomeViewModel) {
        learnHomeViewModel.effects.collect { effect ->
            if (effect is com.signkorean.app.home.HomeSessionEffect.NavigateToLearn) {
                navigationViewModel.dispatch(AppNavigationAction.Navigate(AppRoute.Learn))
            }
        }
    }
    SignKoreanAppContent(
        state = state,
        onAction = navigationViewModel::dispatch,
        launchState = launchState,
        onLaunchRetry = launchViewModel::retry,
        onboardingState = onboardingState,
        onSelectLevel = onboardingViewModel::selectLevel,
        onSkipLevel = onboardingViewModel::skipLevel,
        onToggleInterest = onboardingViewModel::toggleInterest,
        onCompleteOnboarding = onboardingViewModel::completeOnboarding,
        learnHomeState = learnHomeState,
        onStartSession = learnHomeViewModel::startDailySession,
        onStartCategorySession = learnHomeViewModel::startCategorySession,
        onConfirmCategoryReplacement = learnHomeViewModel::confirmCategoryReplacement,
        onCancelCategoryReplacement = learnHomeViewModel::cancelCategoryReplacement,
    )
}

@Composable
@Suppress("LongMethod")
internal fun SignKoreanAppContent(
    state: AppNavigationUiState,
    onAction: (AppNavigationAction) -> Unit,
    launchState: LaunchUiState = LaunchUiState.Loading,
    onLaunchRetry: () -> Unit = {},
    onboardingState: OnboardingChoiceUiState = OnboardingChoiceUiState(),
    onSelectLevel: (com.signkorean.domain.model.LearningLevel) -> Unit = {},
    onSkipLevel: () -> Unit = {},
    onToggleInterest: (com.signkorean.domain.model.Interest) -> Unit = {},
    onCompleteOnboarding: () -> Unit = {},
    learnHomeState: LearnHomeUiState = LearnHomeUiState(),
    onStartSession: () -> Unit = {},
    onStartCategorySession: (String) -> Unit = {},
    onConfirmCategoryReplacement: () -> Unit = {},
    onCancelCategoryReplacement: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(AppRoute.Launch)
    val entries =
        remember(
            onAction,
            state,
            launchState,
            onLaunchRetry,
            onboardingState,
            onSelectLevel,
            onSkipLevel,
            onToggleInterest,
            onCompleteOnboarding,
            learnHomeState,
            onStartSession,
            onStartCategorySession,
            onConfirmCategoryReplacement,
            onCancelCategoryReplacement,
        ) {
            entryProvider<NavKey> {
                entry<AppRoute.Launch> { LaunchScreen(launchState, onLaunchRetry) }
                entry<AppRoute.Welcome> { OnboardingDestination(OnboardingStep.Welcome, onAction) }
                entry<AppRoute.Tutorial> { OnboardingDestination(OnboardingStep.Tutorial, onAction) }
                entry<AppRoute.Level> {
                    LevelDestination(
                        state = onboardingState,
                        onSelectLevel = onSelectLevel,
                        onSkipLevel = onSkipLevel,
                        onContinue = { onAction(AppNavigationAction.Navigate(AppRoute.Interests)) },
                    )
                }
                entry<AppRoute.Interests> {
                    InterestsDestination(
                        state = onboardingState,
                        onToggleInterest = onToggleInterest,
                        onStartLearning = onCompleteOnboarding,
                    )
                }
                entry<AppRoute.Home> {
                    LearnHomeDestination(
                        state = learnHomeState,
                        onAction = onAction,
                        onStartSession = onStartSession,
                        onStartCategorySession = onStartCategorySession,
                        onConfirmCategoryReplacement = onConfirmCategoryReplacement,
                        onCancelCategoryReplacement = onCancelCategoryReplacement,
                    )
                }
                entry<AppRoute.Learn> { LearnDestination(state, onAction) }
                entry<AppRoute.Done> { DoneDestination(onAction) }
                entry<AppRoute.Progress> { ProgressScreen(ProgressUiState(), onAction = {}) }
                entry<AppRoute.Settings> { SettingsDestination(onAction) }
                entry<AppRoute.Legal> { LegalScreen(LegalUiState()) }
            }
        }

    NavigationEffects(
        state = state,
        launchState = launchState,
        onboardingState = onboardingState,
        onAction = onAction,
        synchronizeBackStack = {
            if (backStack.toList() != state.backStack) {
                backStack.clear()
                backStack.addAll(state.backStack)
            }
        },
    )

    SignKoreanTheme {
        SignKoreanScreen {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    entryProvider = entries,
                    modifier = Modifier.fillMaxSize().padding(SignKoreanSpacing.large),
                )
                if (state.currentRoute in setOf(AppRoute.Home, AppRoute.Progress, AppRoute.Settings)) {
                    TopLevelTabs(
                        selectedTab = state.currentRoute.topLevelTab,
                        onAction = onAction,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(SignKoreanSpacing.medium),
                    )
                }
                AppOverlayHost(
                    state = state,
                    categories = learnHomeState.categories,
                    onAction = onAction,
                    onStartCategorySession = onStartCategorySession,
                )
            }
        }
    }
}

@Composable
private fun NavigationEffects(
    state: AppNavigationUiState,
    launchState: LaunchUiState,
    onboardingState: OnboardingChoiceUiState,
    onAction: (AppNavigationAction) -> Unit,
    synchronizeBackStack: () -> Unit,
) {
    LaunchedEffect(launchState) {
        if (launchState is LaunchUiState.Ready) {
            onAction(AppNavigationAction.LaunchReady(launchState.destination))
        }
    }
    LaunchedEffect(onboardingState.isCompleted) {
        if (onboardingState.isCompleted) {
            onAction(AppNavigationAction.CompleteOnboarding)
        }
    }
    LaunchedEffect(state.backStack) { synchronizeBackStack() }
    BackHandler(enabled = state.handlesBack) { onAction(AppNavigationAction.Back) }
}

@Composable
private fun LaunchScreen(
    state: LaunchUiState,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            when (state) {
                LaunchUiState.Loading -> {
                    Text(stringResource(R.string.launch_loading), style = MaterialTheme.typography.bodyLarge)
                }

                is LaunchUiState.Error -> {
                    val message =
                        when (state.failure) {
                            LaunchFailure.Database -> stringResource(R.string.launch_database_error)
                            LaunchFailure.Content -> stringResource(R.string.launch_content_error)
                        }
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    GameButton(
                        text = stringResource(R.string.launch_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is LaunchUiState.Ready -> {
                    Unit
                }
            }
        }
    }
}

@Composable
private fun OnboardingDestination(
    step: OnboardingStep,
    onAction: (AppNavigationAction) -> Unit,
) {
    OnboardingScreen(
        state = OnboardingUiState(step),
        onAction = { action ->
            if (action == OnboardingUiAction.Continue) {
                onAction(
                    when (step) {
                        OnboardingStep.Welcome -> AppNavigationAction.StartOnboarding
                        OnboardingStep.Tutorial -> AppNavigationAction.Navigate(AppRoute.Level)
                        OnboardingStep.Level -> AppNavigationAction.Navigate(AppRoute.Interests)
                        OnboardingStep.Interests -> AppNavigationAction.CompleteOnboarding
                    },
                )
            }
        },
    )
}

@Composable
private fun LearnHomeDestination(
    state: LearnHomeUiState,
    onAction: (AppNavigationAction) -> Unit,
    onStartSession: () -> Unit,
    onStartCategorySession: (String) -> Unit,
    onConfirmCategoryReplacement: () -> Unit,
    onCancelCategoryReplacement: () -> Unit,
) {
    LearnHomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                LearnUiAction.StartSession -> onStartSession()
                is LearnUiAction.StartCategorySession -> onStartCategorySession(action.category)
                LearnUiAction.ConfirmCategoryReplacement -> onConfirmCategoryReplacement()
                LearnUiAction.CancelCategoryReplacement -> onCancelCategoryReplacement()
                LearnUiAction.ShowDeckSheet -> onAction(AppNavigationAction.ShowSheet(AppSheet.Category))
                LearnUiAction.OpenProgress -> onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Progress))
                LearnUiAction.OpenSettings -> onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Settings))
                else -> Unit
            }
        },
    )
}

@Composable
private fun LearnDestination(
    state: AppNavigationUiState,
    onAction: (AppNavigationAction) -> Unit,
) {
    LearnSessionScreen(
        state = LearnSessionUiState(isAnswerVisible = state.isAnswerVisible),
        onAction = { action ->
            when (action) {
                LearnUiAction.RevealAnswer -> onAction(AppNavigationAction.ToggleAnswer)
                LearnUiAction.ShowImageViewer -> onAction(AppNavigationAction.ShowViewer)
                LearnUiAction.FinishSession -> onAction(AppNavigationAction.Navigate(AppRoute.Done))
                else -> Unit
            }
        },
    )
}

@Composable
private fun DoneDestination(onAction: (AppNavigationAction) -> Unit) {
    CompletionScreen(
        state = CompletionUiState(),
        onAction = { action ->
            if (action == LearnUiAction.OpenProgress) {
                onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Progress))
            }
        },
    )
}

@Composable
private fun SettingsDestination(onAction: (AppNavigationAction) -> Unit) {
    SettingsScreen(
        state = SettingsUiState(),
        onAction = { action ->
            when (action) {
                SettingsUiAction.SelectVoice -> onAction(AppNavigationAction.ShowSheet(AppSheet.Voice))
                SettingsUiAction.OpenLegal -> onAction(AppNavigationAction.Navigate(AppRoute.Legal))
            }
        },
    )
}

@Composable
private fun TopLevelTabs(
    selectedTab: AppTopLevelTab,
    onAction: (AppNavigationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
            GameButton(
                text = "Learn",
                onClick = { onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Learn)) },
                modifier = Modifier.weight(1f),
                style = if (selectedTab == AppTopLevelTab.Learn) GameButtonStyle.Primary else GameButtonStyle.Secondary,
            )
            GameButton(
                text = "Progress",
                onClick = { onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Progress)) },
                modifier = Modifier.weight(1f),
                style =
                    if (selectedTab == AppTopLevelTab.Progress) {
                        GameButtonStyle.Primary
                    } else {
                        GameButtonStyle.Secondary
                    },
            )
            GameButton(
                text = "Settings",
                onClick = { onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Settings)) },
                modifier = Modifier.weight(1f),
                style =
                    if (selectedTab == AppTopLevelTab.Settings) {
                        GameButtonStyle.Primary
                    } else {
                        GameButtonStyle.Secondary
                    },
            )
        }
    }
}

@Composable
private fun BoxScope.AppOverlayHost(
    state: AppNavigationUiState,
    categories: List<com.signkorean.feature.learn.HomeCategoryUiModel>,
    onAction: (AppNavigationAction) -> Unit,
    onStartCategorySession: (String) -> Unit,
) {
    state.sheet?.let { sheet ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            GameSheet(title = if (sheet == AppSheet.Category) "All categories" else "Choose a voice") {
                if (sheet == AppSheet.Category) {
                    categories.forEach { category ->
                        GameButton(
                            text = "${category.label} · ${category.cardCount} signs",
                            onClick = {
                                onAction(AppNavigationAction.Back)
                                onStartCategorySession(category.key)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("home-sheet-category-${category.key}"),
                            style = GameButtonStyle.Secondary,
                        )
                    }
                } else {
                    Text("Eunkyung is selected.", style = MaterialTheme.typography.bodyLarge)
                }
                GameButton(
                    text = "Close",
                    onClick = { onAction(AppNavigationAction.Back) },
                    modifier = Modifier.fillMaxWidth(),
                    style = GameButtonStyle.Secondary,
                )
            }
        }
    }
    if (state.isViewerVisible) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            GameCard(modifier = Modifier.padding(SignKoreanSpacing.large)) {
                Text("Full-screen sign viewer", style = MaterialTheme.typography.headlineSmall)
                Text("The photo stays visible while you inspect the sign.", style = MaterialTheme.typography.bodyLarge)
                GameButton("Close viewer", { onAction(AppNavigationAction.Back) }, Modifier.fillMaxWidth())
            }
        }
    }
    state.toastMessage?.let { message ->
        GameToast(
            message = message,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(SignKoreanSpacing.large)
                    .testTag(SignKoreanTestTags.GLOBAL_FEEDBACK),
        )
    }
}

private val AppRoute.topLevelTab: AppTopLevelTab
    get() =
        when (this) {
            AppRoute.Home -> AppTopLevelTab.Learn
            AppRoute.Progress -> AppTopLevelTab.Progress
            AppRoute.Settings -> AppTopLevelTab.Settings
            else -> AppTopLevelTab.Learn
        }
