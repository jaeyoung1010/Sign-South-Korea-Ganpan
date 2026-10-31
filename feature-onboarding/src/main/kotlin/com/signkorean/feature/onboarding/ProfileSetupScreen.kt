@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanShapes
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.ProfileAvatar

@Composable
fun ProfileSetupScreen(
    state: OnboardingChoiceUiState,
    onAction: (OnboardingChoiceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftName by rememberSaveable(state.profileName) { mutableStateOf(state.profileName) }
    Column(modifier = modifier.fillMaxSize()) {
        OnboardingProgressIndicator(
            activeSteps = 5,
            modifier = Modifier.padding(top = SignKoreanSpacing.small),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = SignKoreanSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.onboarding_profile_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_profile_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            ProfileAvatar.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
                    row.forEach { avatar ->
                        ProfileAvatarChoice(
                            avatar = avatar,
                            selected = state.selectedProfileAvatar == avatar,
                            onClick = { onAction(OnboardingChoiceAction.SelectProfileAvatar(avatar)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            state.selectedProfileAvatar?.let { avatar ->
                Text(
                    text = stringResource(R.string.onboarding_profile_language, avatar.languageName()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(SignKoreanPalette.MUTED),
                )
            }
            OutlinedTextField(
                value = draftName,
                onValueChange = {
                    draftName = it.take(MAX_PROFILE_NAME_LENGTH)
                    onAction(OnboardingChoiceAction.UpdateProfileName(draftName))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.onboarding_profile_name_label)) },
                placeholder = { Text(stringResource(R.string.onboarding_profile_name_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
            if (state.hasSaveError) {
                Text(
                    text = stringResource(R.string.onboarding_save_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        GameButton(
            text = stringResource(R.string.onboarding_start_learning),
            enabled = state.canCompleteProfile && !state.isSaving && !state.isCompleted,
            onClick = { onAction(OnboardingChoiceAction.CompleteProfile) },
            modifier = Modifier.fillMaxWidth().testTag(OnboardingChoiceTestTags.PROFILE_CONTINUE),
        )
    }
}

@Composable
private fun ProfileAvatarChoice(
    avatar: ProfileAvatar,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberAssetBitmap(avatar.assetPath, sampleSize = 4)
    Surface(
        modifier =
            modifier
                .aspectRatio(1f)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = SignKoreanShapes.medium,
        color = Color(SignKoreanPalette.SURFACE),
        border = BorderStroke(if (selected) 4.dp else 2.dp, Color(if (selected) SignKoreanPalette.PRIMARY else SignKoreanPalette.OUTLINE)),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = avatar.languageName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(SignKoreanShapes.medium),
            )
        } ?: androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().background(Color(SignKoreanPalette.BORDER)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = avatar.languageName(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProfileAvatar.languageName(): String =
    stringResource(
        when (languageTag) {
            "ja" -> R.string.onboarding_profile_language_japanese
            "zh" -> R.string.onboarding_profile_language_chinese
            else -> R.string.onboarding_profile_language_english
        },
    )

private const val MAX_PROFILE_NAME_LENGTH = 40
