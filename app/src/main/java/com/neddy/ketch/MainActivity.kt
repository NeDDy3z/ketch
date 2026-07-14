package com.neddy.ketch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neddy.ketch.data.settings.ThemeMode
import com.neddy.ketch.ui.editor.WatcherEditScreen
import com.neddy.ketch.ui.home.HomeScreen
import com.neddy.ketch.ui.navigation.Routes
import com.neddy.ketch.ui.navigation.bottomNavItems
import com.neddy.ketch.ui.settings.SettingsScreen
import com.neddy.ketch.ui.theme.KetchTheme
import com.neddy.ketch.ui.watchers.WatchersScreen
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeModeFlow = appContainer.settingsRepository.settings.map { it.themeMode }
        setContent {
            val themeMode by themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            KetchTheme(themeMode = themeMode) {
                KetchRoot()
            }
        }
    }
}

@Composable
fun KetchRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        item.selectedIcon
                                    } else {
                                        item.unselectedIcon
                                    },
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Only consume the bottom bar inset here. Each screen hosts its own
        // Scaffold with a TopAppBar that already handles the status bar
        // inset, so applying the full padding would double the top spacing.
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCreateWatcher = { navController.navigate(Routes.watcherEdit()) },
                )
            }
            composable(Routes.WATCHERS) {
                WatchersScreen(
                    onAddWatcher = { navController.navigate(Routes.watcherEdit()) },
                    onEditWatcher = { id -> navController.navigate(Routes.watcherEdit(id)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.WATCHER_EDIT,
                arguments = listOf(
                    navArgument("watcherId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val watcherId = entry.arguments?.getLong("watcherId")?.takeIf { it >= 0 }
                WatcherEditScreen(
                    watcherId = watcherId,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
