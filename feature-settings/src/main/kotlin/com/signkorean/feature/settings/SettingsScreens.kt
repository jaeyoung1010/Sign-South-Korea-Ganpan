@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.testing.SignKoreanTestTags

data class SettingsUiState(
    val title: String = "Settings",
    val selectedVoice: String = "Eunkyung",
)

data class LegalUiState(
    val title: String = "Legal",
    val summary: String = "Terms, privacy, and content acknowledgements.",
)

sealed interface SettingsUiAction {
    data object SelectVoice : SettingsUiAction

    data object OpenLegal : SettingsUiAction
}

sealed interface SettingsUiEffect {
    data object OpenVoiceChooser : SettingsUiEffect

    data object NavigateToLegal : SettingsUiEffect
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-settings")) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        GameButton(
            text = "Voice: ${state.selectedVoice}",
            onClick = { onAction(SettingsUiAction.SelectVoice) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.SETTINGS_VOICE),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = "Legal",
            onClick = { onAction(SettingsUiAction.OpenLegal) },
            modifier = Modifier.fillMaxWidth(),
            style = GameButtonStyle.Secondary,
        )
    }
}

@Composable
fun LegalScreen(
    state: LegalUiState,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-legal")) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.summary, style = MaterialTheme.typography.bodyLarge)
    }
}
