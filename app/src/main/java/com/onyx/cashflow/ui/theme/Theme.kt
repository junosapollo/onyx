package com.onyx.cashflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── High-contrast retro palette ──────────────────────────────────────────────
private val Black         = Color(0xFF000000)
private val NearBlack     = Color(0xFF0A0A0A)
private val DarkGray      = Color(0xFF111111)
private val MidGray       = Color(0xFF1A1A1A)
private val BorderGray    = Color(0xFF333333)
private val TextGray      = Color(0xFF999999)
private val White         = Color(0xFFFFFFFF)

private val NeonGreen     = Color(0xFF00FF41)   // terminal green — primary
private val DarkGreen     = Color(0xFF003B0F)   // onPrimary bg
private val Amber         = Color(0xFFFFB000)   // secondary
private val DarkAmber     = Color(0xFF3D2B00)   // onSecondary bg
private val BrightRed     = Color(0xFFFF3333)   // error
private val DarkRed       = Color(0xFF3B0000)   // onError bg
private val Cyan          = Color(0xFF00E5FF)   // tertiary accent

private val RetroColorScheme = darkColorScheme(
    primary             = NeonGreen,
    onPrimary           = Black,
    primaryContainer    = DarkGreen,
    onPrimaryContainer  = NeonGreen,
    secondary           = Amber,
    onSecondary         = Black,
    secondaryContainer  = DarkAmber,
    onSecondaryContainer = Amber,
    tertiary            = Cyan,
    onTertiary          = Black,
    background          = Black,
    onBackground        = White,
    surface             = NearBlack,
    onSurface           = White,
    surfaceVariant      = DarkGray,
    onSurfaceVariant    = Color(0xFFCCCCCC),
    error               = BrightRed,
    onError             = White,
    errorContainer      = DarkRed,
    onErrorContainer    = BrightRed,
    outline             = BorderGray,
    outlineVariant      = Color(0xFF222222),
    inverseSurface      = White,
    inverseOnSurface    = Black,
)

// ── Flat shapes — sharp corners everywhere ───────────────────────────────────
private val RetroShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(2.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun CashFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RetroColorScheme,
        typography = AppTypography,
        shapes = RetroShapes,
        content = content
    )
}
