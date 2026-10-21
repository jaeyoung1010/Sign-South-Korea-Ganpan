@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.GuestAccount
import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId

data class SettingsUiState(
    val title: String = "Settings",
    val selectedVoice: AudioVoice = AudioVoice.TYPECAST_EUNKYUNG,
    val sessionLength: Int = 20,
    val showRomanization: Boolean = false,
    val autoPlayAudio: Boolean = false,
    val reduceMotion: Boolean = false,
    val followSystemReduceMotion: Boolean = true,
    val reminderEnabled: Boolean = false,
    val appVersion: String = "0.1.0",
)

data class LegalUiState(
    val title: String = "Legal",
    val summary: String = "Terms, privacy, and content acknowledgements.",
    val documents: List<PolicyDocument> = emptyList(),
    val selectedDocumentId: PolicyDocumentId = PolicyDocumentId.PRIVACY,
    val isLoading: Boolean = false,
    val usingBundledFallback: Boolean = true,
    val loadError: String? = null,
) {
    val selectedDocument: PolicyDocument?
        get() = documents.firstOrNull { it.id == selectedDocumentId }
}

data class AccountUiState(
    val title: String = "Account & sync",
    val account: GuestAccount = GuestAccount.local(),
    val connectEnabled: Boolean = false,
)

sealed interface SettingsUiAction {
    data object SelectVoice : SettingsUiAction

    data object SelectSessionLength : SettingsUiAction

    data object ToggleRomanization : SettingsUiAction

    data object ToggleAutoPlay : SettingsUiAction

    data object ToggleReduceMotion : SettingsUiAction

    data object ToggleReminder : SettingsUiAction

    data object OpenLegal : SettingsUiAction

    data object OpenAccount : SettingsUiAction
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
        SettingsSwitchRow(
            label = "Review reminder",
            description = "Daily at 7:00 PM when cards are due",
            checked = state.reminderEnabled,
            onCheckedChange = { onAction(SettingsUiAction.ToggleReminder) },
        )
        GameButton(
            text = "Legal",
            onClick = { onAction(SettingsUiAction.OpenLegal) },
            modifier = Modifier.fillMaxWidth(),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = "Account & sync",
            onClick = { onAction(SettingsUiAction.OpenAccount) },
            modifier = Modifier.fillMaxWidth().testTag("settings.account"),
            style = GameButtonStyle.Secondary,
        )
        Text(
            text = "Version ${state.appVersion}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.End),
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
    onAction: (LegalUiAction) -> Unit = {},
) {
    val selectedDocument = state.selectedDocument
    GameCard(
        modifier =
            modifier
                .testTag("screen-legal")
                .verticalScroll(rememberScrollState()),
    ) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.summary, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
            GameButton(
                text = "Privacy policy",
                onClick = { onAction(LegalUiAction.SelectDocument(PolicyDocumentId.PRIVACY)) },
                modifier = Modifier.weight(1f).testTag("legal-privacy"),
                style =
                    if (state.selectedDocumentId == PolicyDocumentId.PRIVACY) {
                        GameButtonStyle.Primary
                    } else {
                        GameButtonStyle.Secondary
                    },
            )
            GameButton(
                text = "Terms of use",
                onClick = { onAction(LegalUiAction.SelectDocument(PolicyDocumentId.TERMS)) },
                modifier = Modifier.weight(1f).testTag("legal-terms"),
                style =
                    if (state.selectedDocumentId == PolicyDocumentId.TERMS) {
                        GameButtonStyle.Primary
                    } else {
                        GameButtonStyle.Secondary
                    },
            )
        }
        when {
            state.isLoading -> Text("Loading policy…", style = MaterialTheme.typography.bodyLarge)
            selectedDocument == null -> Text("The bundled policy could not be loaded.", style = MaterialTheme.typography.bodyLarge)
            else -> {
                if (state.usingBundledFallback) {
                    Text(
                        text = "Offline copy · available without an internet connection",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.testTag("legal-offline"),
                    )
                }
                Text(
                    text = "Version ${selectedDocument.version} · Effective ${selectedDocument.effectiveDate}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("legal-version"),
                )
                Text(text = selectedDocument.body, style = MaterialTheme.typography.bodyMedium)
                GameButton(
                    text = "Read online",
                    onClick = { onAction(LegalUiAction.OpenOnline) },
                    modifier = Modifier.fillMaxWidth().testTag("legal-online"),
                    style = GameButtonStyle.Secondary,
                )
            }
        }
        state.loadError?.let { error ->
            Text(text = error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("legal-error"))
        }
        GameButton(
            text = "Back to settings",
            onClick = { onAction(LegalUiAction.Back) },
            modifier = Modifier.fillMaxWidth().testTag("legal-back"),
            style = GameButtonStyle.Secondary,
        )
    }
}

sealed interface LegalUiAction {
    data class SelectDocument(val id: PolicyDocumentId) : LegalUiAction

    data object OpenOnline : LegalUiAction

    data object Back : LegalUiAction
}

@Composable
fun AccountSheet(
    modifier: Modifier = Modifier,
    state: AccountUiState = AccountUiState(),
) {
    Column(modifier = modifier.testTag("sheet-account")) {
        Text(text = "You're learning as a guest", style = MaterialTheme.typography.titleMedium)
        Text(text = "Your progress is saved on this device.", style = MaterialTheme.typography.bodyLarge)
        Text(text = "We do not save your email address.", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Deleting or uninstalling the app removes this local progress.",
            style = MaterialTheme.typography.bodyLarge,
        )
        GameButton(
            text = "Connect account",
            onClick = {},
            enabled = state.connectEnabled,
            modifier = Modifier.fillMaxWidth().testTag("account-connect"),
            style = GameButtonStyle.Secondary,
        )
        Text(
            text = "Cloud sync is not available in this version.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
