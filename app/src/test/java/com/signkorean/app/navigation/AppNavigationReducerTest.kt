package com.signkorean.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationReducerTest {
    private val reducer = AppNavigationReducer()

    @Test
    fun onboarding_routes_reach_learn_home_in_order() {
        val state =
            reducer.reduceAll(
                listOf(
                    AppNavigationAction.Navigate(AppRoute.Tutorial),
                    AppNavigationAction.Navigate(AppRoute.Level),
                    AppNavigationAction.Navigate(AppRoute.Interests),
                    AppNavigationAction.CompleteOnboarding,
                ),
            )

        assertEquals(listOf(AppRoute.Home), state.backStack)
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
    fun selecting_the_current_top_level_tab_does_not_duplicate_its_route() {
        val state = AppNavigationUiState(backStack = listOf(AppRoute.Home))

        assertEquals(
            listOf(AppRoute.Home),
            reducer.reduce(state, AppNavigationAction.SelectTab(AppTopLevelTab.Learn)).backStack,
        )
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
        val state = AppNavigationUiState(backStack = listOf(AppRoute.Welcome))

        assertEquals(state, reducer.reduce(state, AppNavigationAction.Back))
    }
}
