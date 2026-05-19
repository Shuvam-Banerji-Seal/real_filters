package com.realfilters.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object FilterColors {
    // Primary - Deep Indigo
    val Primary = Color(0xFF4F46E5)
    val PrimaryLight = Color(0xFF818CF8)
    val PrimaryDark = Color(0xFF3730A3)
    val PrimaryContainer = Color(0xFFE0E7FF)
    val OnPrimaryContainer = Color(0xFF1E1B4B)

    // Secondary - Teal
    val Secondary = Color(0xFF0D9488)
    val SecondaryLight = Color(0xFF5EEAD4)
    val SecondaryContainer = Color(0xFFCCFBF1)
    val OnSecondaryContainer = Color(0xFF042F2E)

    // Tertiary - Rose
    val Tertiary = Color(0xFFE11D48)
    val TertiaryLight = Color(0xFFFB7185)
    val TertiaryContainer = Color(0xFFFFE4E6)
    val OnTertiaryContainer = Color(0xFF4C0519)

    // Error
    val Error = Color(0xFFDC2626)
    val ErrorContainer = Color(0xFFFEE2E2)
    val OnErrorContainer = Color(0xFF7F1D1D)

    // Light Surface
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF1F5F9)
    val LightOnSurface = Color(0xFF0F172A)
    val LightOnSurfaceVariant = Color(0xFF475569)
    val LightOutline = Color(0xFFCBD5E1)

    // Dark Surface
    val DarkBackground = Color(0xFF0F172A)
    val DarkSurface = Color(0xFF1E293B)
    val DarkSurfaceVariant = Color(0xFF334155)
    val DarkOnSurface = Color(0xFFF1F5F9)
    val DarkOnSurfaceVariant = Color(0xFF94A3B8)
    val DarkOutline = Color(0xFF475569)
}

private val LightColorScheme = lightColorScheme(
    primary = FilterColors.Primary,
    onPrimary = Color.White,
    primaryContainer = FilterColors.PrimaryContainer,
    onPrimaryContainer = FilterColors.OnPrimaryContainer,
    secondary = FilterColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = FilterColors.SecondaryContainer,
    onSecondaryContainer = FilterColors.OnSecondaryContainer,
    tertiary = FilterColors.Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = FilterColors.TertiaryContainer,
    onTertiaryContainer = FilterColors.OnTertiaryContainer,
    error = FilterColors.Error,
    errorContainer = FilterColors.ErrorContainer,
    onErrorContainer = FilterColors.OnErrorContainer,
    background = FilterColors.LightBackground,
    onBackground = FilterColors.LightOnSurface,
    surface = FilterColors.LightSurface,
    onSurface = FilterColors.LightOnSurface,
    surfaceVariant = FilterColors.LightSurfaceVariant,
    onSurfaceVariant = FilterColors.LightOnSurfaceVariant,
    outline = FilterColors.LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = FilterColors.PrimaryLight,
    onPrimary = FilterColors.PrimaryDark,
    primaryContainer = FilterColors.PrimaryDark,
    onPrimaryContainer = FilterColors.PrimaryContainer,
    secondary = FilterColors.SecondaryLight,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = FilterColors.SecondaryContainer,
    tertiary = FilterColors.TertiaryLight,
    onTertiary = Color(0xFF4C0519),
    tertiaryContainer = Color(0xFF9F1239),
    onTertiaryContainer = FilterColors.TertiaryContainer,
    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
    background = FilterColors.DarkBackground,
    onBackground = FilterColors.DarkOnSurface,
    surface = FilterColors.DarkSurface,
    onSurface = FilterColors.DarkOnSurface,
    surfaceVariant = FilterColors.DarkSurfaceVariant,
    onSurfaceVariant = FilterColors.DarkOnSurfaceVariant,
    outline = FilterColors.DarkOutline
)

@Composable
fun RealFiltersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FilterTypography,
        content = content
    )
}

val FilterTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    titleLarge = Typography().titleLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    ),
    titleMedium = Typography().titleMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    titleSmall = Typography().titleSmall.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    bodyLarge = Typography().bodyLarge.copy(
        lineHeight = Typography().bodyLarge.lineHeight
    ),
    labelLarge = Typography().labelLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
)
