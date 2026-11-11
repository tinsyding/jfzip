package com.tinsyding.jfzip.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Vermilion = Color(0xFFA83A2A)
private val VermilionNight = Color(0xFFD96C58)

private val LightColors = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color(0xFFFFFBF5),
    primaryContainer = Color(0xFFF3DFD9),
    onPrimaryContainer = Color(0xFF3A0D06),
    secondary = Color(0xFF5F594E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E2D5),
    onSecondaryContainer = Color(0xFF1A1712),
    tertiary = Vermilion,
    onTertiary = Color(0xFFFFFBF5),
    tertiaryContainer = Color(0xFFF3DFD9),
    onTertiaryContainer = Color(0xFF3A0D06),
    background = Color(0xFFF4F0E6),
    onBackground = Color(0xFF191712),
    surface = Color(0xFFFBF8F1),
    onSurface = Color(0xFF191712),
    surfaceVariant = Color(0xFFEDE7D9),
    onSurfaceVariant = Color(0xFF5F594E),
    outline = Color(0xFFDED6C5),
    outlineVariant = Color(0xFFE9E2D4),
    error = Color(0xFFA02B1F),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEFE9DC),
    onPrimary = Color(0xFF171511),
    primaryContainer = Color(0xFF2B2720),
    onPrimaryContainer = Color(0xFFEFE9DC),
    secondary = Color(0xFFB7B0A1),
    onSecondary = Color(0xFF15130F),
    secondaryContainer = Color(0xFF2A2620),
    onSecondaryContainer = Color(0xFFEFE9DC),
    tertiary = VermilionNight,
    onTertiary = Color(0xFF1C0D09),
    tertiaryContainer = Color(0xFF3A1B14),
    onTertiaryContainer = Color(0xFFF6DDD7),
    background = Color(0xFF131210),
    onBackground = Color(0xFFEFE9DC),
    surface = Color(0xFF1C1A16),
    onSurface = Color(0xFFEFE9DC),
    surfaceVariant = Color(0xFF26231D),
    onSurfaceVariant = Color(0xFFB7B0A1),
    outline = Color(0xFF302C25),
    outlineVariant = Color(0xFF3A352C),
    error = Color(0xFFE58A7C),
    onError = Color(0xFF2A0A05),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun JFZipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
