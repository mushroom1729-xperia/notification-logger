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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mushroom.notificationlogger.data.preferences.ThemeMode
import com.mushroom.notificationlogger.service.BackgroundExecutionHelper
import com.mushroom.notificationlogger.service.BackgroundRestrictionState
import com.mushroom.notificationlogger.service.BatteryOptimizationState
import com.mushroom.notificationlogger.service.ListenerHealthCheckWorker
import com.mushroom.notificationlogger.service.NotificationAccessHelper
import com.mushroom.notificationlogger.service.NotificationCaptureService
import com.mushroom.notificationlogger.service.SystemHealthSnapshot
import com.mushroom.notificationlogger.ui.DetailViewModel
import com.mushroom.notificationlogger.ui.MainViewModel
import com.mushroom.notificationlogger.ui.screens.NotificationDetailScreen
import com.mushroom.notificationlogger.ui.screens.NotificationListScreen
import com.mushroom.notificationlogger.ui.theme.NotificationLoggerTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val systemHealthState = MutableStateFlow(
        SystemHealthSnapshot(
            isNotificationAccessGranted = false,
            isServiceConnected = false,
            batteryOptimizationState = BatteryOptimizationState.IGNORED,
            backgroundRestrictionState = BackgroundRestrictionState.UNRESTRICTED
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshSystemStatus(ensureBoundIfPermitted = true)
        ListenerHealthCheckWorker.schedulePeriodicCheck(this)

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val systemHealth by systemHealthState.collectAsStateWithLifecycle()

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

            NotificationLoggerTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotificationLoggerNavHost(
                        mainViewModel = mainViewModel,
                        systemHealth = systemHealth
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemStatus(ensureBoundIfPermitted = true)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            refreshSystemStatus(ensureBoundIfPermitted = false)
        }
    }

    private fun refreshSystemStatus(ensureBoundIfPermitted: Boolean) {
        val snapshot = BackgroundExecutionHelper.readSystemHealthSnapshot(
            context = this,
            isServiceConnected = NotificationCaptureService.isServiceConnected.value
        )
        systemHealthState.value = snapshot
        if (ensureBoundIfPermitted && snapshot.isNotificationAccessGranted) {
            NotificationAccessHelper.ensureListenerBound(this)
        }
    }
}

@Composable
private fun NotificationLoggerNavHost(
    mainViewModel: MainViewModel,
    systemHealth: SystemHealthSnapshot
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(route = ROUTE_LIST) {
            NotificationListScreen(
                viewModel = mainViewModel,
                isNotificationAccessGranted = systemHealth.isNotificationAccessGranted,
                batteryOptimizationState = systemHealth.batteryOptimizationState,
                backgroundRestrictionState = systemHealth.backgroundRestrictionState,
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
