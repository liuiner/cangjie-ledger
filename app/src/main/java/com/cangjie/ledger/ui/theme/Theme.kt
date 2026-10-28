package com.cangjie.ledger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val IncomeGreen = Color(0xFF087A56)
val ExpenseRed = Color(0xFFC4443E)
val MutedInk = Color(0xFF66736E)
val WarmAmber = Color(0xFFB86B18)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E6B58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3EFE6),
    onPrimaryContainer = Color(0xFF073E33),
    secondary = Color(0xFF9A4D30),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCCF),
    onSecondaryContainer = Color(0xFF5A1F0A),
    tertiary = Color(0xFF3C5D8F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9E3FF),
    onTertiaryContainer = Color(0xFF18365F),
    background = Color(0xFFF4F6F2),
    onBackground = Color(0xFF19211E),
    surface = Color(0xFFFCFDF9),
    onSurface = Color(0xFF19211E),
    surfaceVariant = Color(0xFFE4EAE5),
    onSurfaceVariant = Color(0xFF5B6661),
    outline = Color(0xFF8B9690),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5C0),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF0B5141),
    onPrimaryContainer = Color(0xFFD3EFE6),
    secondary = Color(0xFFFFB69B),
    onSecondary = Color(0xFF5A1F0A),
    secondaryContainer = Color(0xFF79351C),
    onSecondaryContainer = Color(0xFFFFDCCF),
    tertiary = Color(0xFFADC7F5),
    onTertiary = Color(0xFF18365F),
    tertiaryContainer = Color(0xFF2F4C78),
    onTertiaryContainer = Color(0xFFD9E3FF),
    background = Color(0xFF101613),
    onBackground = Color(0xFFE0E7E2),
    surface = Color(0xFF171E1B),
    onSurface = Color(0xFFE0E7E2),
    surfaceVariant = Color(0xFF3A423E),
    onSurfaceVariant = Color(0xFFBEC9C3),
    outline = Color(0xFF88938D),
    error = Color(0xFFFFB4AB),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CangJieTheme(
    themeMode: String,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}

