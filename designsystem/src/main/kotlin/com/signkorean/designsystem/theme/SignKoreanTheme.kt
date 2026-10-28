@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signkorean.designsystem.R

object SignKoreanPalette {
    const val INK: Long = 0xFF0E0F0C
    const val INK_DEEP: Long = 0xFF163300
    const val OUTLINE: Long = 0xFF0E0F0C
    const val BACKGROUND: Long = 0xFFE8EBE6
    const val SURFACE: Long = 0xFFFFFFFF
    const val SURFACE_SOFT: Long = 0xFFE8EBE6
    const val SURFACE_STRONG: Long = 0xFFE2F6D5
    const val SURFACE_DARK: Long = 0xFF0E0F0C
    const val SURFACE_DARK_ELEVATED: Long = 0xFF1B1D18
    const val PRIMARY: Long = 0xFF9FE870
    const val PRIMARY_DARK: Long = 0xFF163300
    const val GOT_IT: Long = 0xFF2EAD4B
    const val GOT_IT_DARK: Long = 0xFF054D28
    const val REVIEW: Long = 0xFFD03238
    const val REVIEW_DARK: Long = 0xFFA72027
    const val Y2K_YELLOW: Long = 0xFFFFD11A
    const val LILAC: Long = 0xFFE8EBE6
    const val MUTED: Long = 0xFF868685
    const val BODY: Long = 0xFF454745
    const val BORDER: Long = 0xFFE8EBE6
}

object SignKoreanSpacing {
    val xxSmall = 4.dp
    val xSmall = 8.dp
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp
    val xLarge = 24.dp
    val xxLarge = 32.dp
}

private val LightColors =
    lightColorScheme(
        primary = Color(SignKoreanPalette.PRIMARY),
        onPrimary = Color(SignKoreanPalette.INK),
        secondary = Color(SignKoreanPalette.SURFACE_SOFT),
        onSecondary = Color(SignKoreanPalette.INK),
        tertiary = Color(SignKoreanPalette.Y2K_YELLOW),
        onTertiary = Color(SignKoreanPalette.INK),
        background = Color(SignKoreanPalette.BACKGROUND),
        onBackground = Color(SignKoreanPalette.INK),
        surface = Color(SignKoreanPalette.SURFACE),
        onSurface = Color(SignKoreanPalette.INK),
        outline = Color(SignKoreanPalette.INK),
        surfaceVariant = Color(SignKoreanPalette.SURFACE_SOFT),
        onSurfaceVariant = Color(SignKoreanPalette.BODY),
        outlineVariant = Color(SignKoreanPalette.SURFACE_SOFT),
    )

private val Pretendard =
    FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal),
        Font(R.font.pretendard_bold, FontWeight.Bold),
        Font(R.font.pretendard_black, FontWeight.Black),
    )

private val SignKoreanTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
            displayMedium = displayMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
            displaySmall = displaySmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            headlineLarge = headlineLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
            headlineMedium = headlineMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            headlineSmall = headlineSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
            titleLarge = titleLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
            titleMedium = titleMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
            titleSmall = titleSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
            bodyLarge = bodyLarge.copy(fontFamily = Pretendard),
            bodyMedium = bodyMedium.copy(fontFamily = Pretendard),
            bodySmall = bodySmall.copy(fontFamily = Pretendard),
            labelLarge = labelLarge.copy(fontFamily = Pretendard),
            labelMedium = labelMedium.copy(fontFamily = Pretendard),
            labelSmall = labelSmall.copy(fontFamily = Pretendard),
        )
    }

@Composable
fun SignKoreanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = SignKoreanTypography,
        shapes = SignKoreanShapes,
        content = content,
    )
}
