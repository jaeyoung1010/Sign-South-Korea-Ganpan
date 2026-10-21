@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.AudioVoice

data class SettingsUiState(
    val title: String = "Settings",
    val selectedVoice: AudioVoice = AudioVoice.TYPECAST_EUNKYUNG,
    val sessionLength: Int = 20,
    val showRomanization: Boolean = false,
    val autoPlayAudio: Boolean = false,
    val reduceMotion: Boolean = false,
    val followSystemReduceMotion: Boolean = true,
)

data class LegalUiState(
    val title: String = "Legal",
    val summary: String = "Terms, privacy, and content acknowledgements.",
)

sealed interface SettingsUiAction {
    data object SelectVoice : SettingsUiAction

    data object SelectSessionLength : SettingsUiAction

    data object ToggleRomanization : SettingsUiAction

    data object ToggleAutoPlay : SettingsUiAction

    data object ToggleReduceMotion : SettingsUiAction

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
            text = "Voice: ${state.selectedVoice.displayName()}",
            onClick = { onAction(SettingsUiAction.SelectVoice) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.SETTINGS_VOICE),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = "Session length: ${state.sessionLength} · ~${state.sessionLength.estimatedMinutes()} min",
            onClick = { onAction(SettingsUiAction.SelectSessionLength) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.SETTINGS_SESSION_LENGTH),
            style = GameButtonStyle.Secondary,
        )
        SettingsSwitchRow(
            label = "Show romanization",
            description = "Answer only",
            checked = state.showRomanization,
            onCheckedChange = { onAction(SettingsUiAction.ToggleRomanization) },
            modifier = Modifier.testTag(SignKoreanTestTags.SETTINGS_ROMANIZATION),
        )
        SettingsSwitchRow(
            label = "Auto-play audio",
            description = "When an answer appears",
            checked = state.autoPlayAudio,
            onCheckedChange = { onAction(SettingsUiAction.ToggleAutoPlay) },
            modifier = Modifier.testTag(SignKoreanTestTags.SETTINGS_AUTO_PLAY),
        )
        SettingsSwitchRow(
            label = "Reduce motion",
            description = if (state.followSystemReduceMotion) "Uses system setting" else "App preference",
            checked = state.reduceMotion,
            onCheckedChange = { onAction(SettingsUiAction.ToggleReduceMotion) },
            modifier = Modifier.testTag(SignKoreanTestTags.SETTINGS_REDUCE_MOTION),
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
private fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun AudioVoice.displayName(): String =
    when (this) {
        AudioVoice.TYPECAST_EUNKYUNG -> "Typecast · Eunkyung"
        AudioVoice.GEMINI_KORE -> "Gemini Flash · Kore"
    }

private fun Int.estimatedMinutes(): Int =
    when (this) {
        5 -> 2
        10 -> 4
        20 -> 8
        else -> 1
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
