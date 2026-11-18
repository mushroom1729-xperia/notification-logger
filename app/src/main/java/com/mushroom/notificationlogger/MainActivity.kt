package com.mushroom.notificationlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mushroom.notificationlogger.data.preferences.ThemeMode
import com.mushroom.notificationlogger.service.NotificationAccessHelper
import com.mushroom.notificationlogger.ui.DetailViewModel
import com.mushroom.notificationlogger.ui.MainViewModel
import com.mushroom.notificationlogger.ui.screens.NotificationDetailScreen
import com.mushroom.notificationlogger.ui.screens.NotificationListScreen
import com.mushroom.notificationlogger.ui.theme.NotificationLoggerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(useDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDark) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(
                            Color.Transparent.toArgb(),
                            Color.Transparent.toArgb()
                        )
                    },
                    navigationBarStyle = if (useDark) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(
                            Color.Transparent.toArgb(),
                            Color.Transparent.toArgb()
                        )
                    }
                )
                onDispose {}
            }

            var isAccessGranted by remember {
                mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(this))
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val granted =
                            NotificationAccessHelper.isNotificationAccessGranted(this@MainActivity)
                        isAccessGranted = granted
                        if (granted) {
                            NotificationAccessHelper.ensureListenerBound(this@MainActivity)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            NotificationLoggerTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotificationLoggerNavHost(
                        mainViewModel = mainViewModel,
                        isNotificationAccessGranted = isAccessGranted
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationLoggerNavHost(
    mainViewModel: MainViewModel,
    isNotificationAccessGranted: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(route = ROUTE_LIST) {
            NotificationListScreen(
                viewModel = mainViewModel,
                isNotificationAccessGranted = isNotificationAccessGranted,
                onOpenNotificationDetail = { id ->
                    navController.navigate("detail/$id") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(
                navArgument("notificationId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getLong("notificationId") ?: -1L
            val detailViewModel: DetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = DetailViewModel.provideFactory(notificationId)
            )
            NotificationDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{notificationId}"
