package com.peakconnect.app.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.peakconnect.app.R

sealed class TrekkerTab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Explore : TrekkerTab(
        route = ExploreTabRoute,
        labelRes = R.string.nav_tab_explore,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Saved : TrekkerTab(
        route = SavedTabRoute,
        labelRes = R.string.nav_tab_saved,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder
    )

    data object Bookings : TrekkerTab(
        route = BookingsTabRoute,
        labelRes = R.string.nav_tab_bookings,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Profile : TrekkerTab(
        route = ProfileTabRoute,
        labelRes = R.string.nav_tab_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

private val trekkerTabs = listOf(
    TrekkerTab.Explore,
    TrekkerTab.Saved,
    TrekkerTab.Bookings,
    TrekkerTab.Profile
)

@Composable
fun TrekkerNavShell(
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                trekkerTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(id = tab.labelRes)
                            )
                        },
                        label = {
                            Text(text = stringResource(id = tab.labelRes))
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ExploreTabRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() },
            popEnterTransition = { defaultPopEnterTransition() },
            popExitTransition = { defaultPopExitTransition() }
        ) {
            composable<ExploreTabRoute> {
                PlaceholderScreen(title = stringResource(id = R.string.nav_tab_explore))
            }
            composable<SavedTabRoute> {
                PlaceholderScreen(title = stringResource(id = R.string.nav_tab_saved))
            }
            composable<BookingsTabRoute> {
                PlaceholderScreen(title = stringResource(id = R.string.nav_tab_bookings))
            }
            composable<ProfileTabRoute> {
                com.peakconnect.app.feature.profile.ProfileScreen(
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }
    }
}
