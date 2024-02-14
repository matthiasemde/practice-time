/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import app.musikus.ui.activesession.ActiveSession
import app.musikus.ui.home.HomeScreen
import app.musikus.ui.sessions.editsession.EditSession
import app.musikus.ui.settings.addSettingsNavigationGraph
import app.musikus.ui.statistics.addStatisticsNavigationGraph
import app.musikus.ui.theme.MusikusTheme
import app.musikus.utils.TimeProvider
import java.util.UUID

const val DEEP_LINK_KEY = "argument"

@Composable
fun MusikusApp(
    timeProvider: TimeProvider,
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = mainViewModel::onUiEvent

    // This line ensures, that the app is only drawn when the proper theme is loaded
    // TODO: make sure this is the right way to do it
    val theme = uiState.activeTheme ?: return


    MusikusTheme(
        theme = theme
    ) {

        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                getEnterTransition()
            },
            exitTransition = {
                getExitTransition()
            }
        ) {
            navigation(
                route = Screen.Home.route,
                startDestination = Screen.HomeTab.defaultTab.route
            ) {
                composable(
                    route = "home/{tab}",
                    arguments = listOf(navArgument("tab") {
                        nullable = true
                    })
                ) { backStackEntry ->
                    val tabRoute = backStackEntry.arguments?.getString("tab")
                    val tab = Screen.HomeTab.allTabs.firstOrNull { it.subRoute == tabRoute }

                    HomeScreen(
                        mainUiState = uiState,
                        mainEventHandler = eventHandler,
                        initialTab = tab,
                        navController = navController,
                        timeProvider = timeProvider
                    )
                }
            }

            composable(
                route = Screen.EditSession.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType})
            ) {backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                    ?: return@composable navController.navigate(Screen.HomeTab.Sessions.route)

                EditSession(
                    sessionToEditId = UUID.fromString(sessionId),
                    navigateUp = navController::navigateUp
                )
            }
            composable(
                route = Screen.ActiveSession.route,
                deepLinks = listOf(navDeepLink {
                    uriPattern = "musikus://activeSession/{$DEEP_LINK_KEY}"
                })
            ) { backStackEntry ->
                ActiveSession(
                    navigateUp = navController::navigateUp,
                    deepLinkArgument = backStackEntry.arguments?.getString(DEEP_LINK_KEY)
                )
            }

            // Statistics
            addStatisticsNavigationGraph(
                navController = navController,
            )

            // Settings
            addSettingsNavigationGraph(navController)
        }
    }
}

fun NavController.navigateTo(screen: Screen) {
    navigate(screen.route)
}


const val ANIMATION_BASE_DURATION = 400

fun AnimatedContentTransitionScope<NavBackStackEntry>.getEnterTransition() : EnterTransition {
    val initialRoute = initialState.destination.route ?: return fadeIn()
    val targetRoute = targetState.destination.route ?: return fadeIn()

    return when {
        // when changing to active session, slide in from the bottom
        targetRoute == Screen.ActiveSession.route -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseOut),
                initialOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when changing from active session, stay invisible until active session has slid in from the bottom
        initialRoute == Screen.ActiveSession.route -> {
            fadeIn(
                initialAlpha = 1f,
                animationSpec = tween(durationMillis = ANIMATION_BASE_DURATION)
            )
        }
        else -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetY = { fullHeight -> -(fullHeight / 10) }
            ) + fadeIn(
                animationSpec = tween(
                    ANIMATION_BASE_DURATION / 2,
                    ANIMATION_BASE_DURATION / 2
                )
            )
        }
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.getExitTransition() : ExitTransition {
    val initialRoute = initialState.destination.route ?: return fadeOut()
    val targetRoute = targetState.destination.route ?: return fadeOut()

    return when {
        // when changing to active session, show immediately
        targetRoute == Screen.ActiveSession.route -> {
            fadeOut(tween(durationMillis = 1, delayMillis = ANIMATION_BASE_DURATION))
        }

        // when changing from active session, slide out to the bottom
        initialRoute == Screen.ActiveSession.route -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseIn),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        }

        // default animation
        else -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetY = { fullHeight -> (fullHeight / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }
    }
}
