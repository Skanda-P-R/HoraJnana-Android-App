package com.hora.jnana.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(val colorName: String, val mainColor: Color) {
    PURPLE("purple", Purple40),
    YELLOW("yellow", Yellow40),
    GREEN("green", Green40),
    BLUE("blue", Blue40),
    RED("red", Red40)
}

// --- Light Color Schemes ---

private val PurpleLightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple40,
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = PurpleGrey80,
    onSecondaryContainer = PurpleGrey40,
    tertiary = Pink40,
    onTertiary = Color.White,
    tertiaryContainer = Pink80,
    onTertiaryContainer = Pink40,
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF4EFF4),
    onSurfaceVariant = PurpleGrey40
)

private val YellowLightColorScheme = lightColorScheme(
    primary = Yellow40,
    onPrimary = Color.Black,
    primaryContainer = Yellow80,
    onPrimaryContainer = Color(0xFF5F4300),
    secondary = YellowGrey40,
    onSecondary = Color.Black,
    secondaryContainer = YellowGrey80,
    onSecondaryContainer = Color(0xFF5F4300),
    tertiary = YellowPink40,
    onTertiary = Color.Black,
    tertiaryContainer = YellowPink80,
    onTertiaryContainer = Color(0xFF5F4300),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF4F1E8),
    onSurfaceVariant = Color(0xFF5F4300)
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green80,
    onPrimaryContainer = Color(0xFF00390A),
    secondary = GreenGrey40,
    onSecondary = Color.White,
    secondaryContainer = GreenGrey80,
    onSecondaryContainer = Color(0xFF00390A),
    tertiary = GreenPink40,
    onTertiary = Color.White,
    tertiaryContainer = GreenPink80,
    onTertiaryContainer = Color(0xFF00390A),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFEDF1EA),
    onSurfaceVariant = Color(0xFF00390A)
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue80,
    onPrimaryContainer = Color(0xFF003258),
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = BlueGrey80,
    onSecondaryContainer = Color(0xFF003258),
    tertiary = BluePink40,
    onTertiary = Color.White,
    tertiaryContainer = BluePink80,
    onTertiaryContainer = Color(0xFF003258),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFE9EFF4),
    onSurfaceVariant = Color(0xFF003258)
)

private val RedLightColorScheme = lightColorScheme(
    primary = Red40,
    onPrimary = Color.White,
    primaryContainer = RedPink80,
    onPrimaryContainer = Color(0xFF410002),
    secondary = RedGrey40,
    onSecondary = Color.White,
    secondaryContainer = RedGrey80,
    onSecondaryContainer = Color(0xFF410002),
    tertiary = RedPink40,
    onTertiary = Color.White,
    tertiaryContainer = RedPink80,
    onTertiaryContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF4EEEB),
    onSurfaceVariant = Color(0xFF410002)
)

// --- Dark Color Schemes ---

private val PurpleDarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple40,
    primaryContainer = Purple40,
    onPrimaryContainer = Purple80,
    secondary = PurpleGrey80,
    onSecondary = PurpleGrey40,
    secondaryContainer = PurpleGrey40,
    onSecondaryContainer = PurpleGrey80,
    tertiary = Pink80,
    onTertiary = Pink40,
    tertiaryContainer = Pink40,
    onTertiaryContainer = Pink80,
    background = Color(0xFF1C1B1E),
    surface = Color(0xFF1C1B1E),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = PurpleGrey80
)

private val YellowDarkColorScheme = darkColorScheme(
    primary = Yellow80,
    onPrimary = Color(0xFF422C00),
    primaryContainer = Color(0xFF5F4300),
    onPrimaryContainer = Yellow80,
    secondary = YellowGrey80,
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5F4300),
    onSecondaryContainer = YellowGrey80,
    tertiary = YellowPink80,
    onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5F4300),
    onTertiaryContainer = YellowPink80,
    background = Color(0xFF1D1B16),
    surface = Color(0xFF1D1B16),
    surfaceVariant = Color(0xFF4B4739),
    onSurfaceVariant = YellowGrey80
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF005313),
    onPrimaryContainer = Green80,
    secondary = GreenGrey80,
    onSecondary = Color(0xFF00390A),
    secondaryContainer = Color(0xFF005313),
    onSecondaryContainer = GreenGrey80,
    tertiary = GreenPink80,
    onTertiary = Color(0xFF00390A),
    tertiaryContainer = Color(0xFF005313),
    onTertiaryContainer = GreenPink80,
    background = Color(0xFF1A1C19),
    surface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = GreenGrey80
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Blue80,
    secondary = BlueGrey80,
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF00497D),
    onSecondaryContainer = BlueGrey80,
    tertiary = BluePink80,
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF00497D),
    onTertiaryContainer = BluePink80,
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = BlueGrey80
)

private val RedDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF5D1212),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF3D2625),
    onSecondaryContainer = Color(0xFFE7BDB8),
    tertiary = Color(0xFFFFB4AB),
    onTertiary = Color(0xFF690005),
    tertiaryContainer = Color(0xFF5D1212),
    onTertiaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF140C0B),
    surface = Color(0xFF140C0B),
    surfaceVariant = Color(0xFF352221),
    onSurfaceVariant = Color(0xFFD8C2BF)
)

@Composable
fun HoraJnanaTheme(
    themeName: String = "green",
    themeMode: String = "light",
    customColor: Color? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }

    val baseColorScheme = if (darkTheme) {
        when (themeName) {
            "yellow" -> YellowDarkColorScheme
            "green" -> GreenDarkColorScheme
            "blue" -> BlueDarkColorScheme
            "red" -> RedDarkColorScheme
            else -> PurpleDarkColorScheme
        }
    } else {
        when (themeName) {
            "yellow" -> YellowLightColorScheme
            "green" -> GreenLightColorScheme
            "blue" -> BlueLightColorScheme
            "red" -> RedLightColorScheme
            else -> PurpleLightColorScheme
        }
    }

    val colorScheme = if (themeName == "custom" && customColor != null) {
        if (darkTheme) {
            baseColorScheme.copy(
                primary = customColor,
                onPrimary = Color.Black,
                primaryContainer = customColor.copy(alpha = 0.7f),
                onPrimaryContainer = Color.White,
                secondary = customColor.copy(alpha = 0.8f),
                onSecondary = Color.Black
            )
        } else {
            baseColorScheme.copy(
                primary = customColor,
                onPrimary = Color.White,
                primaryContainer = customColor.copy(alpha = 0.2f),
                onPrimaryContainer = customColor,
                secondary = customColor.copy(alpha = 0.7f),
                onSecondary = Color.White
            )
        }
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primaryContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
