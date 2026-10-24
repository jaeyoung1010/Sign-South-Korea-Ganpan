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
import com.signkorean.designsystem.R

object SignKoreanPalette {
    const val INK: Long = 0xFF201A24
    const val OUTLINE: Long = 0xFF3B2C43
    const val BACKGROUND: Long = 0xFFFFF7FD
    const val SURFACE: Long = 0xFFFFFAFD
    const val PRIMARY: Long = 0xFFFF5E8E
    const val PRIMARY_DARK: Long = 0xFFD92C6C
    const val GOT_IT: Long = 0xFF2DD4BF
    const val GOT_IT_DARK: Long = 0xFF087F72
    const val REVIEW: Long = 0xFFFF5E8E
    const val REVIEW_DARK: Long = 0xFFA91D56
    const val Y2K_YELLOW: Long = 0xFFFFED85
    const val LILAC: Long = 0xFFEADCFF
    const val MUTED: Long = 0xFF62566B
    const val BORDER: Long = 0xFFD7C5DF
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
        secondary = Color(SignKoreanPalette.GOT_IT),
        onSecondary = Color(SignKoreanPalette.INK),
        tertiary = Color(SignKoreanPalette.Y2K_YELLOW),
        onTertiary = Color(SignKoreanPalette.INK),
        background = Color(SignKoreanPalette.BACKGROUND),
        onBackground = Color(SignKoreanPalette.INK),
        surface = Color(SignKoreanPalette.SURFACE),
        onSurface = Color(SignKoreanPalette.INK),
        outline = Color(SignKoreanPalette.OUTLINE),
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
            displayLarge = displayLarge.copy(fontFamily = Pretendard),
            displayMedium = displayMedium.copy(fontFamily = Pretendard),
            displaySmall = displaySmall.copy(fontFamily = Pretendard),
            headlineLarge = headlineLarge.copy(fontFamily = Pretendard),
            headlineMedium = headlineMedium.copy(fontFamily = Pretendard),
            headlineSmall = headlineSmall.copy(fontFamily = Pretendard),
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
