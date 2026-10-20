package com.signkorean.app.navigation

import com.signkorean.domain.model.LaunchDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationReducerTest {
    private val reducer = AppNavigationReducer()

    @Test
    fun onboarding_routes_reach_learn_home_in_order() {
        val state =
            listOf(
                AppNavigationAction.LaunchReady(LaunchDestination.ONBOARDING),
                AppNavigationAction.Navigate(AppRoute.Tutorial),
                AppNavigationAction.Navigate(AppRoute.Level),
                AppNavigationAction.Navigate(AppRoute.Interests),
                AppNavigationAction.CompleteOnboarding,
            ).fold(AppNavigationUiState(), reducer::reduce)

        assertEquals(listOf(AppRoute.Home), state.backStack)
    }

    @Test
    fun welcome_starts_the_tutorial_once() {
        val welcome = AppNavigationUiState(backStack = listOf(AppRoute.Welcome))
        val tutorial = reducer.reduce(welcome, AppNavigationAction.StartOnboarding)

        assertEquals(listOf(AppRoute.Welcome, AppRoute.Tutorial), tutorial.backStack)
        assertEquals(tutorial, reducer.reduce(tutorial, AppNavigationAction.StartOnboarding))
    }

    @Test
    fun tutorial_back_returns_to_welcome() {
        val tutorial = AppNavigationUiState(backStack = listOf(AppRoute.Welcome, AppRoute.Tutorial))

        assertEquals(AppRoute.Welcome, reducer.reduce(tutorial, AppNavigationAction.Back).currentRoute)
    }

    @Test
    fun learn_done_progress_settings_and_legal_routes_are_reachable() {
        val state =
            reducer.reduceAll(
                listOf(
                    AppNavigationAction.CompleteOnboarding,
                    AppNavigationAction.Navigate(AppRoute.Learn),
                    AppNavigationAction.Navigate(AppRoute.Done),
                    AppNavigationAction.SelectTab(AppTopLevelTab.Progress),
                    AppNavigationAction.SelectTab(AppTopLevelTab.Settings),
                    AppNavigationAction.Navigate(AppRoute.Legal),
                ),
            )

        assertEquals(AppRoute.Legal, state.currentRoute)
        assertEquals(
            listOf(AppRoute.Home, AppRoute.Learn, AppRoute.Done, AppRoute.Progress, AppRoute.Settings, AppRoute.Legal),
            state.backStack,
        )
    }

    @Test
    fun top_level_tabs_map_to_their_prototype_destinations() {
        val state = AppNavigationUiState(backStack = listOf(AppRoute.Home))

        assertEquals(
            AppRoute.Home,
            reducer.reduce(state, AppNavigationAction.SelectTab(AppTopLevelTab.Learn)).currentRoute,
        )
        assertEquals(
            AppRoute.Progress,
            reducer.reduce(state, AppNavigationAction.SelectTab(AppTopLevelTab.Progress)).currentRoute,
        )
        assertEquals(
            AppRoute.Settings,
            reducer.reduce(state, AppNavigationAction.SelectTab(AppTopLevelTab.Settings)).currentRoute,
        )
    }

    @Test
    fun launch_resolves_once_to_home_or_onboarding_without_opening_a_session() {
        val launch = AppNavigationUiState()

        assertEquals(
            AppRoute.Welcome,
            reducer
                .reduce(
                    launch,
                    AppNavigationAction.LaunchReady(LaunchDestination.ONBOARDING),
                ).currentRoute,
        )
        assertEquals(
            AppRoute.Home,
            reducer
                .reduce(
                    launch,
                    AppNavigationAction.LaunchReady(LaunchDestination.LEARN),
                ).currentRoute,
        )
    }

    @Test
    fun selecting_the_current_top_level_tab_does_not_duplicate_its_route() {
        val state = AppNavigationUiState(backStack = listOf(AppRoute.Home))

        assertEquals(
            listOf(AppRoute.Home),
            reducer.reduce(state, AppNavigationAction.SelectTab(AppTopLevelTab.Learn)).backStack,
        )
    }

    @Test
    fun switching_top_level_tabs_keeps_prior_tab_routes_for_back_navigation() {
        val home = AppNavigationUiState(backStack = listOf(AppRoute.Home))
        val progress = reducer.reduce(home, AppNavigationAction.SelectTab(AppTopLevelTab.Progress))
        val settings = reducer.reduce(progress, AppNavigationAction.SelectTab(AppTopLevelTab.Settings))

        assertEquals(AppRoute.Progress, reducer.reduce(settings, AppNavigationAction.Back).currentRoute)
        assertEquals(AppRoute.Home, reducer.reduce(progress, AppNavigationAction.Back).currentRoute)
    }

    @Test
    fun debug_qa_screen_ids_resolve_to_every_prototype_route() {
        assertEquals(
            listOf(
                AppRoute.Welcome,
                AppRoute.Tutorial,
                AppRoute.Level,
                AppRoute.Interests,
                AppRoute.Home,
                AppRoute.Learn,
                AppRoute.Done,
                AppRoute.Progress,
                AppRoute.Settings,
            ),
            AppNavigationIntent.prototypeScreenIds.map(AppNavigationIntent::routeForScreenId),
        )
    }

    @Test
    fun back_closes_overlays_before_popping_the_route() {
        val state =
            AppNavigationUiState(backStack = listOf(AppRoute.Home, AppRoute.Learn))
                .copy(sheet = AppSheet.Category, isViewerVisible = true, toastMessage = "Saved")

        val afterViewer = reducer.reduce(state, AppNavigationAction.Back)
        val afterSheet = reducer.reduce(afterViewer, AppNavigationAction.Back)
        val afterToast = reducer.reduce(afterSheet, AppNavigationAction.Back)
        val afterRoute = reducer.reduce(afterToast, AppNavigationAction.Back)

        assertEquals(false, afterViewer.isViewerVisible)
        assertEquals(AppSheet.Category, afterViewer.sheet)
        assertEquals(null, afterSheet.sheet)
        assertEquals("Saved", afterSheet.toastMessage)
        assertEquals(null, afterToast.sheet)
        assertEquals(null, afterToast.toastMessage)
        assertEquals(AppRoute.Learn, afterToast.currentRoute)
        assertEquals(AppRoute.Home, afterRoute.currentRoute)
    }

    @Test
    fun back_on_root_route_keeps_the_user_in_the_app() {
        val state = AppNavigationUiState(backStack = listOf(AppRoute.Launch))

        assertEquals(state, reducer.reduce(state, AppNavigationAction.Back))
    }

    @Test
    fun root_welcome_leaves_back_to_the_activity() {
        assertFalse(AppNavigationUiState(backStack = listOf(AppRoute.Welcome)).handlesBack)
        assertTrue(AppNavigationUiState(backStack = listOf(AppRoute.Welcome), sheet = AppSheet.Category).handlesBack)
        assertTrue(AppNavigationUiState(backStack = listOf(AppRoute.Welcome, AppRoute.Tutorial)).handlesBack)
    }
}
