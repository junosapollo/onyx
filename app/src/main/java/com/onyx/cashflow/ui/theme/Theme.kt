package com.onyx.cashflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Rounded Dark Gray Palette
private val Background   = Color(0xFF0D0D0D) // Very dark gray, almost black
private val Surface      = Color(0xFF1A1A1A) // Slightly lighter for cards
private val SurfaceVar   = Color(0xFF222222) // Lighter still for nested elements
private val PrimaryGreen = Color(0xFF81C784) // Soft mint green accent
private val TextWhite    = Color(0xFFF5F5F5)
private val TextGray     = Color(0xFFAAAAAA)
private val OutlineColor = Color(0xFF333333)

private val ErrorRed     = Color(0xFFE57373)
private val Secondary    = Color(0xFF64B5F6) // Soft blue

private val RoundedDarkColorScheme = darkColorScheme(
    primary             = PrimaryGreen,
    onPrimary           = Background,
    primaryContainer    = Color(0xFF1B5E20),
    onPrimaryContainer  = PrimaryGreen,
    secondary           = Secondary,
    onSecondary         = Background,
    secondaryContainer  = Color(0xFF0D47A1),
    onSecondaryContainer= Secondary,
    background          = Background,
    onBackground        = TextWhite,
    surface             = Surface,
    onSurface           = TextWhite,
    surfaceVariant      = SurfaceVar,
    onSurfaceVariant    = TextGray,
    error               = ErrorRed,
    onError             = Background,
    errorContainer      = Color(0xFFB71C1C),
    onErrorContainer    = ErrorRed,
    outline             = OutlineColor,
    outlineVariant      = Color(0xFF3A3A3A),
    inverseSurface      = TextWhite,
    inverseOnSurface    = Background,
)

// Rounded Geometry
private val RoundedShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun CashFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoundedDarkColorScheme,
        typography = AppTypography,
        shapes = RoundedShapes,
        content = content
    )
}
