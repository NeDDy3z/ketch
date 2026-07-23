package com.neddy.ketch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neddy.ketch.data.settings.ThemeMode
import com.neddy.ketch.ui.editor.WatcherEditScreen
import com.neddy.ketch.ui.home.HomeScreen
import com.neddy.ketch.ui.navigation.Routes
import com.neddy.ketch.ui.settings.SettingsScreen
import com.neddy.ketch.ui.theme.KetchTheme
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

    // Each screen hosts its own Scaffold with a TopAppBar, so there is no app
    // level chrome here. Settings is reached from the home menu instead of a
    // bottom navigation bar.
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.fillMaxSize(),
    ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCreateWatcher = { navController.navigate(Routes.watcherEdit()) },
                    onEditWatcher = { id -> navController.navigate(Routes.watcherEdit(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
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
