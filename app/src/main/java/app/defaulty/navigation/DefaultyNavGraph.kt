package app.defaulty.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import app.defaulty.R
import app.defaulty.domain.model.MediaHandlerType
import app.defaulty.domain.model.SupportedRole
import app.defaulty.ui.details.DefaultAppDetailsScreen
import app.defaulty.ui.details.MediaHandlerDetailsScreen
import app.defaulty.ui.home.HomeScreen
import app.defaulty.ui.links.OpeningLinksScreen
import app.defaulty.ui.onboarding.OnboardingScreen
import app.defaulty.ui.others.OthersScreen
import app.defaulty.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Others : Screen("others")
    data object Settings : Screen("settings")
    data object Links : Screen("links")
    data object Details : Screen("details/{roleId}") {
        fun createRoute(role: SupportedRole): String = "details/${role.name}"
    }
    data object MediaDetails : Screen("media_details/{mediaId}") {
        fun createRoute(type: MediaHandlerType): String = "media_details/${type.id}"
    }
}

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
) {
    data object Home : BottomNavItem(
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        labelRes = R.string.nav_home,
    )

    data object Settings : BottomNavItem(
        route = Screen.Settings.route,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelRes = R.string.nav_settings,
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Settings,
)

/**
 * Main application navigation graph and layout.
 *
 * Edge-to-edge rules (Spec Section 5):
 * - Insets handled exactly once at the top Scaffold level.
 * - Bottom navigation bar sits above the Android system navigation gesture area.
 * - Predictive Back supported via Navigation Compose.
 */
@Composable
fun DefaultyNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Settings.route,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes),
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                fadeIn(animationSpec = tween(200)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(200),
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(200),
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(200),
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(200),
                )
            },
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetails = { role ->
                        navController.navigate(Screen.Details.createRoute(role))
                    },
                    onNavigateToOthers = {
                        navController.navigate(Screen.Others.route)
                    },
                )
            }

            composable(Screen.Others.route) {
                OthersScreen(
                    onNavigateToDetails = { role ->
                        navController.navigate(Screen.Details.createRoute(role))
                    },
                    onNavigateToMediaDetails = { type ->
                        navController.navigate(Screen.MediaDetails.createRoute(type))
                    },
                    onNavigateToLinks = {
                        navController.navigate(Screen.Links.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.Links.route) {
                OpeningLinksScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument("roleId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roleId = backStackEntry.arguments?.getString("roleId")
                val role = roleId?.let { SupportedRole.fromId(it) }

                if (role != null) {
                    DefaultAppDetailsScreen(
                        role = role,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    HomeScreen(
                        onNavigateToDetails = { r ->
                            navController.navigate(Screen.Details.createRoute(r))
                        },
                        onNavigateToOthers = {
                            navController.navigate(Screen.Others.route)
                        },
                    )
                }
            }

            composable(
                route = Screen.MediaDetails.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId")
                val mediaType = mediaId?.let { MediaHandlerType.fromId(it) }

                if (mediaType != null) {
                    MediaHandlerDetailsScreen(
                        type = mediaType,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    HomeScreen(
                        onNavigateToDetails = { r ->
                            navController.navigate(Screen.Details.createRoute(r))
                        },
                        onNavigateToOthers = {
                            navController.navigate(Screen.Others.route)
                        },
                    )
                }
            }
        }
    }
}
