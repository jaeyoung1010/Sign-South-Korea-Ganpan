package com.signkorean.app.navigation

import androidx.navigation3.runtime.NavKey
import com.signkorean.domain.model.LaunchDestination
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    val screenId: String

    @Serializable
    data object Launch : AppRoute {
        override val screenId: String = "screen-launch"
    }

    @Serializable
    data object Welcome : AppRoute {
        override val screenId: String = "screen-welcome"
    }

    @Serializable
    data object Tutorial : AppRoute {
        override val screenId: String = "screen-tutorial"
    }

    @Serializable
    data object Level : AppRoute {
        override val screenId: String = "screen-level"
    }

    @Serializable
    data object Interests : AppRoute {
        override val screenId: String = "screen-interests"
    }

    @Serializable
    data object Home : AppRoute {
        override val screenId: String = "screen-home"
    }

    @Serializable
    data object Learn : AppRoute {
        override val screenId: String = "screen-learn"
    }

    @Serializable
    data object Done : AppRoute {
        override val screenId: String = "screen-done"
    }

    @Serializable
    data object Progress : AppRoute {
        override val screenId: String = "screen-progress"
    }

    @Serializable
    data object Settings : AppRoute {
        override val screenId: String = "screen-settings"
    }

    @Serializable
    data object Legal : AppRoute {
        override val screenId: String = "screen-legal"
    }
}

enum class AppTopLevelTab {
    Learn,
    Progress,
    Settings,
}

enum class AppSheet {
    Category,
    Voice,
    SessionLength,
    SessionLeave,
    Report,
    Account,
    Language,
}

data class AppNavigationUiState(
    val backStack: List<AppRoute> = listOf(AppRoute.Launch),
    val sheet: AppSheet? = null,
    val isViewerVisible: Boolean = false,
    val isAnswerVisible: Boolean = false,
    val toastMessage: String? = null,
) {
    val currentRoute: AppRoute
        get() = backStack.last()

    val handlesBack: Boolean
        get() = isViewerVisible || sheet != null || toastMessage != null || backStack.size > 1
}

sealed interface AppNavigationAction {
    data class LaunchReady(
        val destination: LaunchDestination,
    ) : AppNavigationAction

    data class Navigate(
        val route: AppRoute,
    ) : AppNavigationAction

    data class SelectTab(
        val tab: AppTopLevelTab,
    ) : AppNavigationAction

    data class ShowSheet(
        val sheet: AppSheet,
    ) : AppNavigationAction

    data object ShowViewer : AppNavigationAction

    data object ToggleAnswer : AppNavigationAction

    data class ShowToast(
        val message: String,
    ) : AppNavigationAction

    data object CompleteOnboarding : AppNavigationAction

    data object StartOnboarding : AppNavigationAction

    data object Back : AppNavigationAction

    data object BackHome : AppNavigationAction
}

sealed interface AppNavigationEffect {
    data class Announce(
        val message: String,
    ) : AppNavigationEffect
}

private typealias NavigationActions = Iterable<AppNavigationAction>

private typealias State = AppNavigationUiState

class AppNavigationReducer {
    fun reduce(
        state: AppNavigationUiState,
        action: AppNavigationAction,
    ): AppNavigationUiState =
        when (action) {
            is AppNavigationAction.LaunchReady -> state.resolveLaunch(action.destination)
            is AppNavigationAction.Navigate -> state.withRoute(action.route)
            is AppNavigationAction.SelectTab -> state.selectTopLevelTab(action.tab)
            is AppNavigationAction.ShowSheet -> state.copy(sheet = action.sheet)
            AppNavigationAction.ShowViewer -> state.copy(isViewerVisible = true)
            AppNavigationAction.ToggleAnswer -> state.copy(isAnswerVisible = !state.isAnswerVisible)
            is AppNavigationAction.ShowToast -> state.copy(toastMessage = action.message)
            AppNavigationAction.CompleteOnboarding -> state.copy(backStack = listOf(AppRoute.Home)).clearOverlays()
            AppNavigationAction.StartOnboarding -> state.startOnboarding()
            AppNavigationAction.Back -> state.goBack()
            AppNavigationAction.BackHome -> state.copy(backStack = listOf(AppRoute.Home)).clearOverlays()
        }

    fun reduceAll(actions: NavigationActions): State = actions.fold(AppNavigationUiState(), ::reduce)

    private fun State.withRoute(route: AppRoute): State = copy(backStack = backStack + route).clearOverlays()

    private fun State.resolveLaunch(destination: LaunchDestination): State {
        if (currentRoute != AppRoute.Launch) return this
        return copy(backStack = listOf(destination.route)).clearOverlays()
    }

    private fun State.startOnboarding(): State =
        when (currentRoute) {
            AppRoute.Welcome -> withRoute(AppRoute.Tutorial)
            else -> this
        }

    private fun AppNavigationUiState.selectTopLevelTab(tab: AppTopLevelTab): AppNavigationUiState =
        if (currentRoute == tab.route) {
            clearOverlays()
        } else {
            withRoute(tab.route)
        }

    private fun AppNavigationUiState.goBack(): AppNavigationUiState =
        when {
            isViewerVisible -> copy(isViewerVisible = false)
            sheet != null -> copy(sheet = null)
            toastMessage != null -> copy(toastMessage = null)
            backStack.size > 1 -> copy(backStack = backStack.dropLast(1))
            else -> this
        }

    private fun AppNavigationUiState.clearOverlays(): AppNavigationUiState =
        copy(sheet = null, isViewerVisible = false, isAnswerVisible = false, toastMessage = null)

    private val AppTopLevelTab.route: AppRoute
        get() =
            when (this) {
                AppTopLevelTab.Learn -> AppRoute.Home
                AppTopLevelTab.Progress -> AppRoute.Progress
                AppTopLevelTab.Settings -> AppRoute.Settings
            }

    private val LaunchDestination.route: AppRoute
        get() =
            when (this) {
                LaunchDestination.ONBOARDING -> AppRoute.Welcome

                LaunchDestination.HOME,
                LaunchDestination.LEARN,
                -> AppRoute.Home
            }
}

object AppNavigationIntent {
    const val EXTRA_DEBUG_SCREEN = "com.signkorean.app.extra.DEBUG_SCREEN"
    const val EXTRA_OPEN_REVIEWS = "com.signkorean.app.extra.OPEN_REVIEWS"
    const val ACTION_OPEN_REVIEWS = "com.signkorean.app.action.OPEN_REVIEWS"

    fun routeForScreenId(screenId: String?): AppRoute? = allRoutes.firstOrNull { it.screenId == screenId }

    val prototypeScreenIds: List<String>
        get() = allRoutes.filterNot { it == AppRoute.Legal || it == AppRoute.Launch }.map(AppRoute::screenId)

    private val allRoutes =
        listOf(
            AppRoute.Launch,
            AppRoute.Welcome,
            AppRoute.Tutorial,
            AppRoute.Level,
            AppRoute.Interests,
            AppRoute.Home,
            AppRoute.Learn,
            AppRoute.Done,
            AppRoute.Progress,
            AppRoute.Settings,
            AppRoute.Legal,
        )
}
