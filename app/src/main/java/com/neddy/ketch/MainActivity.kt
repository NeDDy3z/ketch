package com.neddy.ketch

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import com.neddy.ketch.data.settings.ColorPalette
import com.neddy.ketch.ui.detail.WatcherDetailScreen
import com.neddy.ketch.ui.editor.WatcherEditScreen
import com.neddy.ketch.ui.help.HelpScreen
import com.neddy.ketch.ui.home.HomeScreen
import com.neddy.ketch.ui.navigation.Routes
import com.neddy.ketch.ui.settings.SettingsScreen
import com.neddy.ketch.ui.theme.KetchTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Dark-only: the bars stay transparent with light icons whatever the
        // system light/dark setting is, since every palette is a dark surface.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val paletteFlow = appContainer.settingsRepository.settings.map { it.palette }
        setContent {
            val palette by paletteFlow.collectAsStateWithLifecycle(ColorPalette.DEFAULT)
            KetchTheme(palette = palette) {
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
                    onOpenWatcher = { id -> navController.navigate(Routes.watcherDetail(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenHelp = { navController.navigate(Routes.HELP) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenHelp = { navController.navigate(Routes.HELP) },
                )
            }
            composable(Routes.HELP) {
                HelpScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.WATCHER_DETAIL,
                arguments = listOf(
                    navArgument("watcherId") { type = NavType.LongType },
                ),
            ) { entry ->
                WatcherDetailScreen(
                    watcherId = entry.arguments?.getLong("watcherId") ?: -1L,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.watcherEdit(id)) },
                )
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
