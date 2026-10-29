package com.signkorean.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.theme.SignKoreanSpacing

@Composable
fun AnnouncementsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
    ) {
        Text(text = stringResource(R.string.notices_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(text = stringResource(R.string.notices_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        AnnouncementCard(R.string.notices_welcome_title, R.string.notices_welcome_body)
        AnnouncementCard(R.string.notices_content_title, R.string.notices_content_body)
        GameButton(
            text = stringResource(R.string.notices_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            style = GameButtonStyle.Secondary,
        )
    }
}

@Composable
private fun AnnouncementCard(
    titleRes: Int,
    bodyRes: Int,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ProfileScreen(
    profileName: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftName by rememberSaveable(profileName) { mutableStateOf(profileName) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
    ) {
        Text(text = stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        GameCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.profile_intro), style = MaterialTheme.typography.bodyLarge)
            Text(
                text =
                    profileName.takeIf(String::isNotBlank)?.let { stringResource(R.string.profile_saved_name, it) }
                        ?: stringResource(R.string.profile_not_set),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.profile_name_label)) },
                placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
            GameButton(
                text = stringResource(R.string.profile_save),
                onClick = { onSave(draftName.trim()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GameButton(
            text = stringResource(R.string.profile_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            style = GameButtonStyle.Secondary,
        )
    }
}
