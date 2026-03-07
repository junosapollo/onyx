package com.onyx.cashflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Finance-oriented teal/green palette
private val Teal200 = Color(0xFF80CBC4)
private val Teal500 = Color(0xFF009688)
private val Teal700 = Color(0xFF00796B)
private val Green400 = Color(0xFF66BB6A)
private val DarkSurface = Color(0xFF121212)
private val DarkSurfaceVariant = Color(0xFF1E1E2E)
private val DarkCard = Color(0xFF252537)

private val DarkColorScheme = darkColorScheme(
    primary = Teal200,
    onPrimary = Color(0xFF003731),
    primaryContainer = Teal700,
    onPrimaryContainer = Color(0xFFA7F3EC),
    secondary = Green400,
    onSecondary = Color(0xFF00391C),
    secondaryContainer = Color(0xFF005229),
    onSecondaryContainer = Color(0xFF78DC77),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF462B00),
    background = DarkSurface,
    onBackground = Color(0xFFE6E1E5),
    surface = DarkSurfaceVariant,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFEF5350),
    onError = Color.White,
    outline = Color(0xFF49454F),
)

private val LightColorScheme = lightColorScheme(
    primary = Teal500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Color(0xFFFF9800),
    onTertiary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB3261E),
    onError = Color.White,
    outline = Color(0xFF79747E),
)

@Composable
fun CashFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
