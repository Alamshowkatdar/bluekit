package com.alamshowkat.bluekit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// BlueKit palette: deep ink panels with an electric-blue signal colour.
val Ink = Color(0xFF060B14)
val Night = Color(0xFF0A1220)
val Panel = Color(0xFF101B2E)
val PanelHigh = Color(0xFF16243B)
val Line = Color(0xFF26374F)
val Electric = Color(0xFF5AA9FF)
val ElectricSoft = Color(0xFF9BCBFF)
val Mint = Color(0xFF5EEAD4)
val Amber = Color(0xFFFFC857)
val Rose = Color(0xFFFF7D93)
val Paper = Color(0xFFE9F1FF)
val Muted = Color(0xFF97A9C6)

private val BlueKitColors = darkColorScheme(
    primary = Electric,
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF12365E),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Mint,
    onSecondary = Color(0xFF00201B),
    secondaryContainer = Color(0xFF0D3B35),
    onSecondaryContainer = Color(0xFFB6FFF3),
    tertiary = Amber,
    onTertiary = Color(0xFF2A1D00),
    tertiaryContainer = Color(0xFF4A3600),
    onTertiaryContainer = Color(0xFFFFE3A1),
    background = Ink,
    onBackground = Paper,
    surface = Night,
    onSurface = Paper,
    surfaceVariant = Panel,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Color(0xFF1B2A40),
    error = Rose,
    onError = Color(0xFF3A0010),
    errorContainer = Color(0xFF5A1022),
    onErrorContainer = Color(0xFFFFD9DF)
)

private val BlueKitTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun BlueKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlueKitColors,
        typography = BlueKitTypography,
        content = content
    )
}
