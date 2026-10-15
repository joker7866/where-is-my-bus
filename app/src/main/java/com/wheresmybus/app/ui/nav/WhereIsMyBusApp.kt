package com.wheresmybus.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.buddy.BuddyScreen
import com.wheresmybus.app.ui.favorites.FavoritesScreen
import com.wheresmybus.app.ui.feedback.FeedbackScreen
import com.wheresmybus.app.ui.history.HistoryScreen
import com.wheresmybus.app.ui.home.HomeScreen
import com.wheresmybus.app.ui.journey.JourneySearchScreen
import com.wheresmybus.app.ui.overview.OverviewScreen
import com.wheresmybus.app.ui.routedetail.RouteDetailScreen
import com.wheresmybus.app.ui.search.SearchScreen
import com.wheresmybus.app.ui.settings.InfoScreen
import com.wheresmybus.app.ui.settings.SettingsScreen
import com.wheresmybus.app.ui.stopdetail.StopDetailScreen
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.support.SupportScreen

private sealed class Destination(val route: String, val icon: ImageVector) {
    object Home : Destination("home", Icons.Filled.Home)
    object Favorites : Destination("favorites", Icons.Filled.Star)
    object History : Destination("history", Icons.Filled.History)
    object Buddy : Destination("buddy", Icons.Filled.Group)
    object Overview : Destination("overview", Icons.Filled.Info)
    object Settings : Destination("settings", Icons.Filled.Settings)
}

private val bottomDestinations = listOf(
    Destination.Home, Destination.Favorites, Destination.History,
    Destination.Buddy, Destination.Overview, Destination.Settings,
)
private const val SEARCH_ROUTE = "search"
private const val JOURNEY_ROUTE = "journey/{fromName}/{toName}"
private const val STOP_DETAIL_ROUTE = "stop/{stopId}"
private const val ROUTE_DETAIL_ROUTE = "route/{routeId}"
private const val FEEDBACK_ROUTE = "feedback"
private const val SUPPORT_ROUTE = "support"
private const val PRIVACY_ROUTE = "privacy"
private const val ABOUT_ROUTE = "about"

/** `journey/{fromName}/{toName}` args use a blank-space sentinel instead of an empty string
 * because Navigation Compose treats an empty path segment as a missing argument. */
private fun journeyRoute(fromName: String, toName: String = ""): String {
    val from = android.net.Uri.encode(fromName.ifBlank { " " })
    val to = android.net.Uri.encode(toName.ifBlank { " " })
    return "journey/$from/$to"
}

@Composable
fun WhereIsMyBusApp(
    viewModel: BusViewModel,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
    val strings = stringsFor(language)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == null || bottomDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    val items = listOf(
                        Destination.Home to strings.navHome,
                        Destination.Favorites to strings.navFavorites,
                        Destination.History to strings.navHistory,
                        Destination.Buddy to strings.navBuddy,
                        Destination.Overview to strings.navOverview,
                        Destination.Settings to strings.navSettings,
                    )
                    items.forEach { (dest, label) ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    language = language,
                    onLanguageChange = { language = it },
                    onSearchClick = { navController.navigate(SEARCH_ROUTE) },
                    onRouteClick = { routeId -> navController.navigate("route/$routeId") },
                    onJourneySearchClick = { fromName -> navController.navigate(journeyRoute(fromName)) },
                )
            }
            composable(Destination.Favorites.route) {
                FavoritesScreen(
                    viewModel = viewModel,
                    language = language,
                    onStopClick = { stopId -> navController.navigate("stop/$stopId") },
                    onRouteClick = { routeId -> navController.navigate("route/$routeId") },
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    language = language,
                    onEntryClick = { fromName, toName -> navController.navigate(journeyRoute(fromName, toName)) },
                )
            }
            composable(Destination.Buddy.route) {
                BuddyScreen(viewModel = viewModel, language = language)
            }
            composable(Destination.Overview.route) {
                OverviewScreen(viewModel = viewModel, language = language)
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    language = language,
                    darkTheme = darkTheme,
                    onDarkThemeChange = onDarkThemeChange,
                    onPrivacyPolicyClick = { navController.navigate(PRIVACY_ROUTE) },
                    onAboutUsClick = { navController.navigate(ABOUT_ROUTE) },
                    onContactUsClick = { navController.navigate(SUPPORT_ROUTE) },
                    onFeedbackClick = { navController.navigate(FEEDBACK_ROUTE) },
                )
            }
            composable(PRIVACY_ROUTE) {
                InfoScreen(
                    title = strings.privacyPolicyTitle,
                    body = strings.privacyPolicyBody,
                    backLabel = strings.back,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ABOUT_ROUTE) {
                InfoScreen(
                    title = strings.aboutUsTitle,
                    body = strings.aboutUsBody,
                    backLabel = strings.back,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(FEEDBACK_ROUTE) {
                FeedbackScreen(
                    viewModel = viewModel,
                    language = language,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(SUPPORT_ROUTE) {
                SupportScreen(language = language, onBack = { navController.popBackStack() })
            }
            composable(SEARCH_ROUTE) {
                SearchScreen(
                    viewModel = viewModel,
                    onStopClick = { stopId -> navController.navigate("stop/$stopId") },
                    onRouteClick = { routeId -> navController.navigate("route/$routeId") },
                )
            }
            composable(
                route = JOURNEY_ROUTE,
                arguments = listOf(
                    navArgument("fromName") { type = NavType.StringType },
                    navArgument("toName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val fromName = backStackEntry.arguments?.getString("fromName")?.trim().orEmpty()
                val toName = backStackEntry.arguments?.getString("toName")?.trim().orEmpty()
                JourneySearchScreen(
                    viewModel = viewModel,
                    initialFrom = fromName,
                    initialTo = toName,
                    language = language,
                    onBack = { navController.popBackStack() },
                    onRouteClick = { routeId -> navController.navigate("route/$routeId") },
                )
            }
            composable(
                route = STOP_DETAIL_ROUTE,
                arguments = listOf(navArgument("stopId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val stopId = backStackEntry.arguments?.getString("stopId")
                if (stopId != null) {
                    StopDetailScreen(
                        viewModel = viewModel,
                        stopId = stopId,
                        onBack = { navController.popBackStack() },
                        onRouteClick = { routeId -> navController.navigate("route/$routeId") },
                    )
                }
            }
            composable(
                route = ROUTE_DETAIL_ROUTE,
                arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString("routeId")
                if (routeId != null) {
                    RouteDetailScreen(
                        viewModel = viewModel,
                        routeId = routeId,
                        language = language,
                        onLanguageChange = { language = it },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
