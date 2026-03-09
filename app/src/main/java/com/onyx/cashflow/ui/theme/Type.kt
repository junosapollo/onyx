package com.onyx.cashflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.onyx.cashflow.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val SpaceMono = FontFamily(
    Font(
        googleFont = GoogleFont("Space Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Space Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    )
)

private val RobotoMono = FontFamily(
    Font(
        googleFont = GoogleFont("Roboto Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Roboto Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Roboto Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    )
)

private val JetBrainsMono = FontFamily(
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("JetBrains Mono"),
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    )
)

val AppTypography = Typography(
    // Display — Space Mono for big retro headers
    displayLarge = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),

    // Headlines — Space Mono
    headlineLarge = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),

    // Titles — JetBrains Mono
    titleLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),

    // Body — Roboto Mono for data readability
    bodyLarge = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),

    // Labels — Roboto Mono
    labelLarge = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)
