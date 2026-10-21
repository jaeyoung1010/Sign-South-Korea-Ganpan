@file:Suppress("FunctionNaming", "TooManyFunctions", "ktlint:standard:function-naming")

package com.signkorean.app

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.signkorean.app.home.LearnHomeViewModel
import com.signkorean.app.legal.LegalViewModel
import com.signkorean.app.learn.LearnViewModel
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
import com.signkorean.app.progress.ProgressViewModel
import com.signkorean.app.settings.SettingsEffect
import com.signkorean.app.settings.SettingsViewModel
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameSheet
import com.signkorean.designsystem.components.GameToast
import com.signkorean.designsystem.layout.SignKoreanScreen
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AudioPlaybackFailure
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.model.SessionCompletion
import com.signkorean.feature.learn.CompletionScreen
import com.signkorean.feature.learn.CompletionUiState
import com.signkorean.feature.learn.LearnFeedbackUiState
import com.signkorean.feature.learn.LearnHomeScreen
import com.signkorean.feature.learn.LearnHomeUiState
import com.signkorean.feature.learn.LearnImageViewer
import com.signkorean.feature.learn.LearnSessionScreen
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnUiAction
import com.signkorean.feature.learn.ReportSheet
import com.signkorean.feature.learn.ReportUiState
import com.signkorean.feature.onboarding.OnboardingChoiceUiState
import com.signkorean.feature.onboarding.OnboardingScreen
import com.signkorean.feature.onboarding.OnboardingStep
import com.signkorean.feature.onboarding.OnboardingUiAction
import com.signkorean.feature.onboarding.OnboardingUiState
import com.signkorean.feature.progress.ProgressScreen
import com.signkorean.feature.progress.ProgressUiAction
import com.signkorean.feature.progress.ProgressUiState
import com.signkorean.feature.settings.LegalScreen
import com.signkorean.feature.settings.LegalUiAction
import com.signkorean.feature.settings.LegalUiState
import com.signkorean.feature.settings.AccountSheet
import com.signkorean.feature.settings.AccountUiState
import com.signkorean.feature.settings.SettingsScreen
import com.signkorean.feature.settings.SettingsUiAction
import com.signkorean.feature.settings.SettingsUiState
import com.signkorean.feature.settings.displayName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val navigationViewModel: AppNavigationViewModel by viewModels()
    private val launchViewModel: LaunchViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val learnHomeViewModel: LearnHomeViewModel by viewModels()
    private val learnViewModel: LearnViewModel by viewModels()
    private val progressViewModel: ProgressViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val legalViewModel: LegalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        navigationViewModel.openDebugDestination(intent.getStringExtra(AppNavigationIntent.EXTRA_DEBUG_SCREEN))
        if (intent.getBooleanExtra(AppNavigationIntent.EXTRA_OPEN_REVIEWS, false)) {
            navigationViewModel.openReminderDestination()
        }
        setContent {
            SignKoreanApp(
                navigationViewModel,
                launchViewModel,
                onboardingViewModel,
                learnHomeViewModel,
                learnViewModel,
                progressViewModel,
                settingsViewModel,
                legalViewModel,
            )
        }
    }

    override fun onStop() {
        learnViewModel.stopAudio()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(AppNavigationIntent.EXTRA_OPEN_REVIEWS, false)) {
            navigationViewModel.openReminderDestination()
            learnHomeViewModel.refresh()
        }
    }
}

@Composable
private fun SignKoreanApp(
    navigationViewModel: AppNavigationViewModel,
    launchViewModel: LaunchViewModel,
    onboardingViewModel: OnboardingViewModel,
    learnHomeViewModel: LearnHomeViewModel,
    learnViewModel: LearnViewModel,
    progressViewModel: ProgressViewModel,
    settingsViewModel: SettingsViewModel,
    legalViewModel: LegalViewModel,
) {
    val resources = LocalResources.current
    val state by navigationViewModel.uiState.collectAsState()
    val launchState by launchViewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()
    val learnHomeState by learnHomeViewModel.uiState.collectAsState()
    val learnSessionState by learnViewModel.uiState.collectAsState()
    val learnFeedbackState by learnViewModel.feedbackState.collectAsState()
    val completionState by learnViewModel.completionState.collectAsState()
    val progressState by progressViewModel.uiState.collectAsState()
    val reportState by learnViewModel.reportState.collectAsState()
    val audioState by learnViewModel.audioState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val legalState by legalViewModel.uiState.collectAsState()
    val reminderPermissionDeniedMessage = stringResource(R.string.reminder_permission_denied)
    val reminderPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            settingsViewModel.onReminderPermissionResult(granted)
        }
    LaunchedEffect(state.currentRoute) {
        if (state.currentRoute == AppRoute.Home) {
            learnHomeViewModel.refresh()
        }
        if (state.currentRoute == AppRoute.Learn) {
            learnViewModel.load()
        }
    }
    LaunchedEffect(learnHomeViewModel) {
        learnHomeViewModel.effects.collect { effect ->
            if (effect is com.signkorean.app.home.HomeSessionEffect.NavigateToLearn) {
                navigationViewModel.dispatch(AppNavigationAction.Navigate(AppRoute.Learn))
            }
        }
    }
    LaunchedEffect(learnViewModel) {
        learnViewModel.effects.collect { effect ->
            when (effect) {
                com.signkorean.app.learn.LearnSessionEffect.SessionPaused -> {
                    navigationViewModel.dispatch(AppNavigationAction.Navigate(AppRoute.Home))
                }

                com.signkorean.app.learn.LearnSessionEffect.PauseFailed -> {
                    navigationViewModel.dispatch(
                        AppNavigationAction.ShowToast(resources.getString(R.string.toast_save_progress_failed)),
                    )
                }

                is com.signkorean.app.learn.LearnSessionEffect.SessionCompleted -> {
                    navigationViewModel.dispatch(AppNavigationAction.Navigate(AppRoute.Done))
                }

                com.signkorean.app.learn.LearnSessionEffect.ReportSubmitted -> {
                    navigationViewModel.dispatch(AppNavigationAction.Back)
                    navigationViewModel.dispatch(AppNavigationAction.ShowToast(resources.getString(R.string.toast_report_thanks)))
                }

                is com.signkorean.app.learn.LearnSessionEffect.AudioFailed -> {
                    val message =
                        when (effect.reason) {
                            AudioPlaybackFailure.AudioFocusDenied -> resources.getString(R.string.toast_audio_focus_unavailable)

                            AudioPlaybackFailure.AssetMissing,
                            AudioPlaybackFailure.InvalidProvider,
                            AudioPlaybackFailure.Decode,
                            AudioPlaybackFailure.Unknown,
                            -> resources.getString(R.string.toast_audio_unavailable)
                        }
                    navigationViewModel.dispatch(AppNavigationAction.ShowToast(message))
                }
            }
        }
    }
    LaunchedEffect(progressViewModel) {
        progressViewModel.effects.collect { effect ->
            when (effect) {
                com.signkorean.feature.progress.ProgressUiEffect.NavigateToLearn -> {
                    navigationViewModel.dispatch(AppNavigationAction.Navigate(AppRoute.Learn))
                }
            }
        }
    }
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.VoiceChanged -> {
                    navigationViewModel.dispatch(AppNavigationAction.Back)
                    navigationViewModel.dispatch(
                        AppNavigationAction.ShowToast(resources.getString(R.string.toast_voice_saved, resources.getString(effect.voice.appLabelResource))),
                    )
                }

                SettingsEffect.VoiceChangeFailed -> {
                    navigationViewModel.dispatch(AppNavigationAction.ShowToast(resources.getString(R.string.toast_voice_save_failed)))
                }

                is SettingsEffect.SessionLengthChanged -> {
                    navigationViewModel.dispatch(
                        AppNavigationAction.ShowToast(resources.getQuantityString(R.plurals.toast_cards_selected, effect.length, effect.length)),
                    )
                }

                SettingsEffect.SettingsChangeFailed -> {
                    navigationViewModel.dispatch(AppNavigationAction.ShowToast(resources.getString(R.string.toast_setting_save_failed)))
                }

                is SettingsEffect.LanguageChanged -> {
                    navigationViewModel.dispatch(AppNavigationAction.Back)
                    navigationViewModel.dispatch(
                        AppNavigationAction.ShowToast(resources.getString(R.string.toast_language_saved)),
                    )
                }

                SettingsEffect.LanguageChangeFailed -> {
                    navigationViewModel.dispatch(AppNavigationAction.ShowToast(resources.getString(R.string.toast_language_save_failed)))
                }

                SettingsEffect.ReminderPermissionRequested -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settingsViewModel.onReminderPermissionResult(true)
                    }
                }

                SettingsEffect.ReminderPermissionDenied -> {
                    navigationViewModel.dispatch(AppNavigationAction.ShowToast(reminderPermissionDeniedMessage))
                }

                is SettingsEffect.ReminderChanged -> {
                    if (!effect.enabled) {
                        navigationViewModel.dispatch(AppNavigationAction.ShowToast(resources.getString(R.string.toast_reminders_off)))
                    }
                }
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
        learnSessionState = learnSessionState,
        audioState = audioState,
        learnFeedbackState = learnFeedbackState,
        completionState = completionState,
        progressState = progressState,
        onProgressAction = progressViewModel::onAction,
        onOpenProgressReviews = progressViewModel::openReviewList,
        onRevealAnswer = learnViewModel::revealAnswer,
        onRateCard = learnViewModel::rateCard,
        onRatingAnimationFinished = learnViewModel::finishRatingAnimation,
        onUndoRating = learnViewModel::undoRating,
        onPauseSession = learnViewModel::pauseSession,
        onPlayAudio = learnViewModel::playAudio,
        settingsState = settingsState,
        onSelectLanguage = settingsViewModel::selectLanguage,
        onSelectVoice = settingsViewModel::selectVoice,
        onSelectSessionLength = settingsViewModel::selectSessionLength,
        onToggleRomanization = settingsViewModel::toggleRomanization,
        onToggleAutoPlay = settingsViewModel::toggleAutoPlay,
        onToggleReduceMotion = settingsViewModel::toggleReduceMotion,
        onToggleReminder = settingsViewModel::toggleReminder,
        legalState = legalState,
        onLegalAction = { action ->
            when (action) {
                is LegalUiAction.SelectDocument -> legalViewModel.selectDocument(action.id)
                LegalUiAction.OpenOnline -> legalViewModel.openOnline()
                LegalUiAction.Back -> navigationViewModel.dispatch(AppNavigationAction.Back)
            }
        },
        reportState = reportState,
        onShowReport = learnViewModel::openReport,
        onSelectReportReason = learnViewModel::selectReportReason,
        onUpdateReportDetails = learnViewModel::updateReportDetails,
        onSubmitReport = learnViewModel::submitReport,
        onCancelReport = learnViewModel::cancelReport,
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
    learnSessionState: LearnSessionUiState = LearnSessionUiState.Loading,
    audioState: AudioPlaybackState = AudioPlaybackState.Idle,
    learnFeedbackState: LearnFeedbackUiState? = null,
    completionState: SessionCompletion? = null,
    progressState: ProgressUiState = ProgressUiState(),
    onProgressAction: (ProgressUiAction) -> Unit = {},
    onOpenProgressReviews: () -> Unit = {},
    onRevealAnswer: () -> Unit = {},
    onRateCard: (RatingDecision, RatingInputSource) -> Unit = { _, _ -> },
    onRatingAnimationFinished: () -> Unit = {},
    onUndoRating: () -> Unit = {},
    onPauseSession: () -> Unit = {},
    onPlayAudio: () -> Unit = {},
    settingsState: SettingsUiState = SettingsUiState(),
    onSelectLanguage: (String) -> Unit = {},
    onSelectVoice: (AudioVoice) -> Unit = {},
    onSelectSessionLength: (Int) -> Unit = {},
    onToggleRomanization: () -> Unit = {},
    onToggleAutoPlay: () -> Unit = {},
    onToggleReduceMotion: () -> Unit = {},
    onToggleReminder: () -> Unit = {},
    legalState: LegalUiState = LegalUiState(),
    onLegalAction: (LegalUiAction) -> Unit = {},
    reportState: ReportUiState = ReportUiState(),
    onShowReport: () -> Unit = {},
    onSelectReportReason: (CardReportReason) -> Unit = {},
    onUpdateReportDetails: (String) -> Unit = {},
    onSubmitReport: () -> Unit = {},
    onCancelReport: () -> Unit = {},
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
            learnSessionState,
            audioState,
            learnFeedbackState,
            completionState,
            progressState,
            onProgressAction,
            onOpenProgressReviews,
            onRevealAnswer,
            onRateCard,
            onRatingAnimationFinished,
            onUndoRating,
            onPauseSession,
            onPlayAudio,
            settingsState,
            onSelectLanguage,
            onSelectVoice,
            onSelectSessionLength,
            onToggleRomanization,
            onToggleAutoPlay,
            onToggleReduceMotion,
            onToggleReminder,
            legalState,
            onLegalAction,
            onShowReport,
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
                entry<AppRoute.Learn> {
                    LearnDestination(
                        state = learnSessionState,
                        audioState = audioState,
                        feedback = learnFeedbackState,
                        onAction = onAction,
                        onRevealAnswer = onRevealAnswer,
                        onRateCard = onRateCard,
                        onRatingAnimationFinished = onRatingAnimationFinished,
                        onUndoRating = onUndoRating,
                        onPlayAudio = onPlayAudio,
                        reduceMotion = settingsState.reduceMotion,
                        onShowReport = onShowReport,
                    )
                }
                entry<AppRoute.Done> {
                    DoneDestination(
                        completion = completionState,
                        onAction = onAction,
                        onOpenReviewCards = {
                            onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Progress))
                            onOpenProgressReviews()
                        },
                    )
                }
                entry<AppRoute.Progress> {
                    ProgressScreen(
                        state = progressState,
                        onAction = { action ->
                            when (action) {
                                ProgressUiAction.ReturnToLearn -> onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Learn))
                                else -> onProgressAction(action)
                            }
                        },
                    )
                }
                entry<AppRoute.Settings> {
                    SettingsDestination(
                        state = settingsState,
                        onAction = onAction,
                        onToggleRomanization = onToggleRomanization,
                        onToggleAutoPlay = onToggleAutoPlay,
                        onToggleReduceMotion = onToggleReduceMotion,
                        onToggleReminder = onToggleReminder,
                        onOpenAccount = { onAction(AppNavigationAction.ShowSheet(AppSheet.Account)) },
                    )
                }
                entry<AppRoute.Legal> {
                    LegalScreen(
                        state = legalState,
                        onAction = onLegalAction,
                    )
                }
            }
        }

    NavigationEffects(
        state = state,
        launchState = launchState,
        onboardingState = onboardingState,
        learnSessionState = learnSessionState,
        onAction = onAction,
        onRequestLearnClose = { onAction(AppNavigationAction.ShowSheet(AppSheet.SessionLeave)) },
        synchronizeBackStack = {
            if (backStack.toList() != state.backStack) {
                backStack.clear()
                backStack.addAll(state.backStack)
            }
        },
    )
    LaunchedEffect(state.sheet) {
        if (state.sheet != AppSheet.Report) onCancelReport()
    }

    SignKoreanTheme {
        SignKoreanScreen(applySafeDrawingInsets = !state.isViewerVisible) {
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
                    learnSessionState = learnSessionState,
                    onAction = onAction,
                    onStartCategorySession = onStartCategorySession,
                    onPauseSession = onPauseSession,
                    reportState = reportState,
                    onSelectReportReason = onSelectReportReason,
                    onUpdateReportDetails = onUpdateReportDetails,
                    onSubmitReport = onSubmitReport,
                    onCancelReport = onCancelReport,
                    settingsState = settingsState,
                    onSelectLanguage = onSelectLanguage,
                    onSelectVoice = onSelectVoice,
                    onSelectSessionLength = onSelectSessionLength,
                    accountState = AccountUiState(),
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
    learnSessionState: LearnSessionUiState,
    onAction: (AppNavigationAction) -> Unit,
    onRequestLearnClose: () -> Unit,
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
    BackHandler(enabled = state.handlesBack) {
        if (state.isViewerVisible) {
            onAction(AppNavigationAction.Back)
        } else if (state.currentRoute == AppRoute.Done) {
            onAction(AppNavigationAction.BackHome)
        } else if (state.shouldRequestLearnClose(learnSessionState)) {
            onRequestLearnClose()
        } else {
            onAction(AppNavigationAction.Back)
        }
    }
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
    state: LearnSessionUiState,
    audioState: AudioPlaybackState,
    feedback: LearnFeedbackUiState?,
    reduceMotion: Boolean,
    onAction: (AppNavigationAction) -> Unit,
    onRevealAnswer: () -> Unit,
    onRateCard: (RatingDecision, RatingInputSource) -> Unit,
    onRatingAnimationFinished: () -> Unit,
    onUndoRating: () -> Unit,
    onPlayAudio: () -> Unit,
    onShowReport: () -> Unit,
) {
    val resources = LocalResources.current
    LearnSessionScreen(
        state = state,
        audioState = audioState,
        feedback = feedback,
        reduceMotion = reduceMotion,
        onAction = { action ->
            when (action) {
                LearnUiAction.RevealAnswer -> {
                    onRevealAnswer()
                }

                LearnUiAction.PlayAudio -> {
                    onPlayAudio()
                }

                LearnUiAction.ShowImageViewer -> {
                    onAction(AppNavigationAction.ShowViewer)
                }

                LearnUiAction.CloseSession -> {
                    if (state is LearnSessionUiState.Active) {
                        onAction(AppNavigationAction.ShowSheet(AppSheet.SessionLeave))
                    } else {
                        onAction(AppNavigationAction.Back)
                    }
                }

                is LearnUiAction.RateCard -> {
                    onRateCard(action.decision, action.source)
                }

                LearnUiAction.RatingAnimationFinished -> {
                    onRatingAnimationFinished()
                }

                LearnUiAction.UndoRating -> {
                    onUndoRating()
                }

                LearnUiAction.CheckAnswerFirst -> {
                    onAction(AppNavigationAction.ShowToast(resources.getString(R.string.toast_reveal_first)))
                }

                LearnUiAction.ShowSessionMenu -> {
                    if (state is LearnSessionUiState.Active) {
                        onShowReport()
                        onAction(AppNavigationAction.ShowSheet(AppSheet.Report))
                    } else {
                        onAction(AppNavigationAction.ShowToast(resources.getString(R.string.toast_card_unavailable)))
                    }
                }

                else -> {
                    Unit
                }
            }
        },
    )
}

@Composable
private fun DoneDestination(
    completion: SessionCompletion?,
    onAction: (AppNavigationAction) -> Unit,
    onOpenReviewCards: () -> Unit,
) {
    CompletionScreen(
        state = completion?.toCompletionUiState() ?: CompletionUiState(),
        onAction = { action ->
            when (action) {
                LearnUiAction.BackHome -> onAction(AppNavigationAction.BackHome)

                LearnUiAction.OpenReviewCards -> onOpenReviewCards()

                LearnUiAction.OpenProgress -> onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Progress))

                else -> Unit
            }
        },
    )
}

private fun SessionCompletion.toCompletionUiState(): CompletionUiState =
    CompletionUiState(
        totalCount = totalCount,
        gotItCount = gotItCount,
        reviewCount = reviewCount,
        nextReviewDate = nextReviewAt?.let { DateTimeFormatter.ISO_LOCAL_DATE.format(it.atZone(ZoneId.systemDefault())) },
        reviewCardIds = reviewCardIds,
    )

private fun Int.estimatedMinutes(): Int =
    when (this) {
        5 -> 2
        10 -> 4
        20 -> 8
        else -> 1
    }

private val AudioVoice.appLabelResource: Int
    get() =
        when (this) {
            AudioVoice.TYPECAST_EUNKYUNG -> R.string.voice_typecast
            AudioVoice.GEMINI_KORE -> R.string.voice_gemini
        }

@Composable
private fun SettingsDestination(
    state: SettingsUiState,
    onAction: (AppNavigationAction) -> Unit,
    onToggleRomanization: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onToggleReminder: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    SettingsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SettingsUiAction.SelectLanguage -> onAction(AppNavigationAction.ShowSheet(AppSheet.Language))
                SettingsUiAction.SelectVoice -> onAction(AppNavigationAction.ShowSheet(AppSheet.Voice))
                SettingsUiAction.SelectSessionLength -> onAction(AppNavigationAction.ShowSheet(AppSheet.SessionLength))
                SettingsUiAction.ToggleRomanization -> onToggleRomanization()
                SettingsUiAction.ToggleAutoPlay -> onToggleAutoPlay()
                SettingsUiAction.ToggleReduceMotion -> onToggleReduceMotion()
                SettingsUiAction.ToggleReminder -> onToggleReminder()
                SettingsUiAction.OpenLegal -> onAction(AppNavigationAction.Navigate(AppRoute.Legal))
                SettingsUiAction.OpenAccount -> onOpenAccount()
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
                text = stringResource(R.string.learn_tab),
                onClick = { onAction(AppNavigationAction.SelectTab(AppTopLevelTab.Learn)) },
                modifier = Modifier.weight(1f),
                style = if (selectedTab == AppTopLevelTab.Learn) GameButtonStyle.Primary else GameButtonStyle.Secondary,
            )
            GameButton(
                text = stringResource(R.string.progress_tab),
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
                text = stringResource(R.string.settings_tab),
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
@Suppress("LongMethod")
private fun BoxScope.AppOverlayHost(
    state: AppNavigationUiState,
    categories: List<com.signkorean.feature.learn.HomeCategoryUiModel>,
    learnSessionState: LearnSessionUiState,
    onAction: (AppNavigationAction) -> Unit,
    onStartCategorySession: (String) -> Unit,
    onPauseSession: () -> Unit,
    reportState: ReportUiState,
    onSelectReportReason: (CardReportReason) -> Unit,
    onUpdateReportDetails: (String) -> Unit,
    onSubmitReport: () -> Unit,
    onCancelReport: () -> Unit,
    settingsState: SettingsUiState,
    onSelectLanguage: (String) -> Unit,
    onSelectVoice: (AudioVoice) -> Unit,
    onSelectSessionLength: (Int) -> Unit,
    accountState: AccountUiState,
) {
    val resources = LocalResources.current
    state.sheet?.let { sheet ->
        val dismissSheet = {
            if (sheet == AppSheet.Report) onCancelReport()
            onAction(AppNavigationAction.Back)
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable(onClick = dismissSheet),
            )
            GameSheet(
                title =
                    when (sheet) {
                        AppSheet.Category -> stringResource(R.string.sheet_all_categories)
                        AppSheet.Voice -> stringResource(R.string.sheet_choose_voice)
                        AppSheet.SessionLength -> stringResource(R.string.sheet_session_length)
                        AppSheet.SessionLeave -> stringResource(R.string.sheet_leave_session)
                        AppSheet.Report -> stringResource(R.string.sheet_report_card)
                        AppSheet.Account -> accountState.title
                        AppSheet.Language -> stringResource(com.signkorean.feature.settings.R.string.sheet_choose_language)
                    },
            ) {
                when (sheet) {
                    AppSheet.Category -> {
                        categories.forEach { category ->
                            GameButton(
                                text = resources.getQuantityString(com.signkorean.feature.learn.R.plurals.learn_category_signs, category.cardCount, category.label, category.cardCount),
                                onClick = {
                                    onAction(AppNavigationAction.Back)
                                    onStartCategorySession(category.key)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("home-sheet-category-${category.key}"),
                                style = GameButtonStyle.Secondary,
                            )
                        }
                    }

                    AppSheet.Voice -> {
                        Text(stringResource(R.string.sheet_choose_pronunciation_voice), style = MaterialTheme.typography.bodyLarge)
                        AudioVoice.entries.forEach { voice ->
                            GameButton(
                                text =
                                    if (voice == settingsState.selectedVoice) {
                                        "✓ ${resources.getString(voice.appLabelResource)}"
                                    } else {
                                        resources.getString(voice.appLabelResource)
                                    },
                                onClick = { onSelectVoice(voice) },
                                modifier = Modifier.fillMaxWidth().testTag("settings.voice.${voice.name.lowercase()}"),
                                style =
                                    if (voice == settingsState.selectedVoice) {
                                        GameButtonStyle.Primary
                                    } else {
                                        GameButtonStyle.Secondary
                                    },
                            )
                        }
                    }

                    AppSheet.SessionLength -> {
                        Text(stringResource(R.string.sheet_session_length_question), style = MaterialTheme.typography.bodyLarge)
                        listOf(5, 10, 20).forEach { length ->
                            GameButton(
                                text =
                                    if (length == settingsState.sessionLength) {
                                        "✓ ${resources.getQuantityString(R.plurals.sheet_cards_length, length, length, length.estimatedMinutes())}"
                                    } else {
                                        resources.getQuantityString(R.plurals.sheet_cards_length, length, length, length.estimatedMinutes())
                                    },
                                onClick = {
                                    onSelectSessionLength(length)
                                    onAction(AppNavigationAction.Back)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("settings.session-length.$length"),
                                style =
                                    if (length == settingsState.sessionLength) {
                                        GameButtonStyle.Primary
                                    } else {
                                        GameButtonStyle.Secondary
                                    },
                            )
                        }
                    }

                    AppSheet.SessionLeave -> {
                        SessionLeaveSheetContent(learnSessionState, onAction, onPauseSession)
                    }

                    AppSheet.Report -> {
                        ReportSheet(
                            state = reportState,
                            onReasonSelected = onSelectReportReason,
                            onDetailsChanged = onUpdateReportDetails,
                            onSubmit = onSubmitReport,
                            onCancel = dismissSheet,
                        )
                    }

                    AppSheet.Account -> AccountSheet(state = accountState)

                    AppSheet.Language -> {
                        Text(
                            stringResource(com.signkorean.feature.settings.R.string.sheet_language_question),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        settingsState.supportedLanguageTags.forEach { languageTag ->
                            val selected = languageTag == settingsState.appLanguageTag
                            GameButton(
                                text = (if (selected) "✓ " else "") + languageTag.displayName(resources),
                                onClick = {
                                    onSelectLanguage(languageTag)
                                    onAction(AppNavigationAction.Back)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("settings.language.$languageTag"),
                                style = if (selected) GameButtonStyle.Primary else GameButtonStyle.Secondary,
                            )
                        }
                    }
                }
                if (sheet != AppSheet.SessionLeave && sheet != AppSheet.Report) {
                    GameButton(
                        text = stringResource(R.string.close),
                        onClick = { onAction(AppNavigationAction.Back) },
                        modifier = Modifier.fillMaxWidth(),
                        style = GameButtonStyle.Secondary,
                    )
                }
            }
        }
    }
    if (state.isViewerVisible) {
        val activeSession = learnSessionState as? LearnSessionUiState.Active
        if (activeSession != null) {
            LearnImageViewer(
                state = activeSession,
                onClose = { onAction(AppNavigationAction.Back) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                GameButton(
                    text = stringResource(R.string.close_viewer),
                    onClick = { onAction(AppNavigationAction.Back) },
                    modifier =
                        Modifier
                            .padding(SignKoreanSpacing.large)
                            .testTag(SignKoreanTestTags.LEARN_IMAGE_VIEWER_CLOSE),
                    style = GameButtonStyle.Secondary,
                )
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

private fun String.displayName(resources: android.content.res.Resources): String =
    when (lowercase()) {
        "en" -> resources.getString(com.signkorean.feature.settings.R.string.language_english)
        "ja" -> resources.getString(com.signkorean.feature.settings.R.string.language_japanese)
        else -> this
    }

@Composable
private fun SessionLeaveSheetContent(
    learnSessionState: LearnSessionUiState,
    onAction: (AppNavigationAction) -> Unit,
    onPauseSession: () -> Unit,
) {
    val resources = LocalResources.current
    val activeSession = learnSessionState as? LearnSessionUiState.Active
    activeSession?.let {
        Text(
            text = resources.getQuantityString(R.plurals.session_signs_complete, it.totalCount, it.position - 1, it.totalCount),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Text(
        stringResource(R.string.session_leave_saved),
        style = MaterialTheme.typography.bodyLarge,
    )
    GameButton(
        text = stringResource(R.string.session_keep_learning),
        onClick = { onAction(AppNavigationAction.Back) },
        modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_KEEP_LEARNING),
    )
    GameButton(
        text = stringResource(R.string.session_leave_for_now),
        onClick = {
            onAction(AppNavigationAction.Back)
            onPauseSession()
        },
        modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_LEAVE_FOR_NOW),
        style = GameButtonStyle.Secondary,
    )
}

private fun AppNavigationUiState.shouldRequestLearnClose(learnSessionState: LearnSessionUiState): Boolean =
    if (currentRoute != AppRoute.Learn) {
        false
    } else if (learnSessionState !is LearnSessionUiState.Active) {
        false
    } else if (sheet != null) {
        false
    } else if (isViewerVisible) {
        false
    } else {
        toastMessage == null
    }

private val AppRoute.topLevelTab: AppTopLevelTab
    get() =
        when (this) {
            AppRoute.Home -> AppTopLevelTab.Learn
            AppRoute.Progress -> AppTopLevelTab.Progress
            AppRoute.Settings -> AppTopLevelTab.Settings
            else -> AppTopLevelTab.Learn
        }
