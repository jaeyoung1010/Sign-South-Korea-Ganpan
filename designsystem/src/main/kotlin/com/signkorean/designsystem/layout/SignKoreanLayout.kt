@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.designsystem.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.theme.SignKoreanPalette

@Composable
fun SignKoreanScreen(
    modifier: Modifier = Modifier,
    applySafeDrawingInsets: Boolean = true,
    content: @Composable () -> Unit,
) {
    val safeDrawingModifier =
        if (applySafeDrawingInsets) {
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
        } else {
            Modifier
        }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(SignKoreanPalette.BACKGROUND))
                .diaryGridBackground()
                .then(safeDrawingModifier),
    ) {
        content()
    }
}

fun Modifier.diaryGridBackground(): Modifier =
    drawBehind {
        val step = 26.dp.toPx()
        val gridColor = Color(SignKoreanPalette.LILAC).copy(alpha = 0.42f)
        val dotColor = Color(SignKoreanPalette.PRIMARY).copy(alpha = 0.18f)

        var x = 0f
        while (x <= size.width) {
            drawLine(
                gridColor,
                start =
                    androidx.compose.ui.geometry
                        .Offset(x, 0f),
                end =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                gridColor,
                start =
                    androidx.compose.ui.geometry
                        .Offset(0f, y),
                end =
                    androidx.compose.ui.geometry
                        .Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += step
        }
        drawCircle(
            dotColor,
            radius = 2.dp.toPx(),
            center =
                androidx.compose.ui.geometry
                    .Offset(size.width * 0.18f, size.height * 0.14f),
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            dotColor,
            radius = 2.dp.toPx(),
            center =
                androidx.compose.ui.geometry
                    .Offset(size.width * 0.82f, size.height * 0.34f),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
