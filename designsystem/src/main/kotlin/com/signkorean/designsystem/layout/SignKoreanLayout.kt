@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.designsystem.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.signkorean.designsystem.theme.SignKoreanPalette

@Composable
fun SignKoreanScreen(
    modifier: Modifier = Modifier,
    applySafeDrawingInsets: Boolean = true,
    bottomInsetColor: Color? = null,
    backgroundColor: Color = Color(SignKoreanPalette.BACKGROUND),
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
                .background(backgroundColor),
    ) {
        bottomInsetColor?.let { color ->
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(color),
            )
        }
        Box(modifier = Modifier.fillMaxSize().then(safeDrawingModifier)) { content() }
    }
}
