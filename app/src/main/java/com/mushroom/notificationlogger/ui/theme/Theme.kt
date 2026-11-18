package com.mushroom.notificationlogger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mushroom.notificationlogger.data.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = MushroomRedLight,
    onPrimary = OnMushroomRedLight,
    primaryContainer = MushroomRedContainerLight,
    onPrimaryContainer = OnMushroomRedContainerLight,
    secondary = WarmMossSecondaryLight,
    onSecondary = OnWarmMossSecondaryLight,
    secondaryContainer = WarmMossContainerLight,
    onSecondaryContainer = OnWarmMossContainerLight,
    tertiary = ForestSlateTertiaryLight,
    onTertiary = OnForestSlateTertiaryLight,
    tertiaryContainer = ForestSlateContainerLight,
    onTertiaryContainer = OnForestSlateTertiaryContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val DarkColors = darkColorScheme(
    primary = MushroomRedDark,
    onPrimary = OnMushroomRedDark,
    primaryContainer = MushroomRedContainerDark,
    onPrimaryContainer = OnMushroomRedContainerDark,
    secondary = WarmMossSecondaryDark,
    onSecondary = OnWarmMossSecondaryDark,
    secondaryContainer = WarmMossContainerDark,
    onSecondaryContainer = OnWarmMossContainerDark,
    tertiary = ForestSlateTertiaryDark,
    onTertiary = OnForestSlateTertiaryDark,
    tertiaryContainer = ForestSlateContainerDark,
    onTertiaryContainer = OnForestSlateTertiaryContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

@Composable
fun NotificationLoggerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
