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
    const val INK: Long = 0xFF0A0B0D
    const val OUTLINE: Long = 0xFFDEE1E6
    const val BACKGROUND: Long = 0xFFFFFFFF
    const val SURFACE: Long = 0xFFFFFFFF
    const val SURFACE_SOFT: Long = 0xFFF7F7F7
    const val SURFACE_STRONG: Long = 0xFFEEF0F3
    const val SURFACE_DARK: Long = 0xFF0A0B0D
    const val SURFACE_DARK_ELEVATED: Long = 0xFF16181C
    const val PRIMARY: Long = 0xFF0052FF
    const val PRIMARY_DARK: Long = 0xFF003ECC
    const val GOT_IT: Long = 0xFF05B169
    const val GOT_IT_DARK: Long = 0xFF038B53
    const val REVIEW: Long = 0xFFCF202F
    const val REVIEW_DARK: Long = 0xFFA51724
    const val Y2K_YELLOW: Long = 0xFFF4B000
    const val LILAC: Long = 0xFFEEF0F3
    const val MUTED: Long = 0xFF7C828A
    const val BODY: Long = 0xFF5B616E
    const val BORDER: Long = 0xFFDEE1E6
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
        onPrimary = Color.White,
        secondary = Color(SignKoreanPalette.SURFACE_STRONG),
        onSecondary = Color(SignKoreanPalette.INK),
        tertiary = Color(SignKoreanPalette.Y2K_YELLOW),
        onTertiary = Color(SignKoreanPalette.INK),
        background = Color(SignKoreanPalette.BACKGROUND),
        onBackground = Color(SignKoreanPalette.INK),
        surface = Color(SignKoreanPalette.SURFACE),
        onSurface = Color(SignKoreanPalette.INK),
        outline = Color(SignKoreanPalette.OUTLINE),
        surfaceVariant = Color(SignKoreanPalette.SURFACE_STRONG),
        onSurfaceVariant = Color(SignKoreanPalette.BODY),
        outlineVariant = Color(SignKoreanPalette.OUTLINE),
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
            displayLarge = displayLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Normal, letterSpacing = (-1.5).sp),
            displayMedium = displayMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp),
            displaySmall = displaySmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp),
            headlineLarge = headlineLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Normal, letterSpacing = (-0.8).sp),
            headlineMedium = headlineMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp),
            headlineSmall = headlineSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Normal),
            titleLarge = titleLarge.copy(fontFamily = Pretendard),
            titleMedium = titleMedium.copy(fontFamily = Pretendard),
            titleSmall = titleSmall.copy(fontFamily = Pretendard),
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
