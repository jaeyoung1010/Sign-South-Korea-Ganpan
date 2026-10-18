@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.signkorean.designsystem.theme.SignKoreanTheme

@Preview(name = "Light game components", showBackground = true, backgroundColor = 0xFFFFF7FD)
@Composable
private fun DesignSystemPreview() {
    SignKoreanTheme {
        GameCard {
            GameChip(text = "Must memorize")
            GameButton(text = "Show answer", onClick = {})
            GameButton(text = "Got it", onClick = {}, style = GameButtonStyle.GotIt)
            GameIconButton(symbol = "♪", contentDescription = "Play audio", onClick = {})
        }
    }
}
