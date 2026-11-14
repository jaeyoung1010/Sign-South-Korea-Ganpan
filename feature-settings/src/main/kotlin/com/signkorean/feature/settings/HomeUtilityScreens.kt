@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanShapes
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.ProfileAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    profileAvatar: ProfileAvatar?,
    onSave: (String, ProfileAvatar) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftName by rememberSaveable(profileName) { mutableStateOf(profileName) }
    var selectedAvatar by rememberSaveable(profileAvatar) { mutableStateOf(profileAvatar) }
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
            Text(
                text = stringResource(R.string.profile_choose_avatar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            ProfileAvatar.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
                    row.forEach { avatar ->
                        ProfileAvatarChoice(
                            avatar = avatar,
                            selected = selectedAvatar == avatar,
                            onClick = { selectedAvatar = avatar },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            selectedAvatar?.let { avatar ->
                Text(
                    text = stringResource(R.string.profile_language, avatar.languageName()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(SignKoreanPalette.MUTED),
                )
            }
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
                enabled = draftName.isNotBlank() && selectedAvatar != null,
                onClick = { selectedAvatar?.let { onSave(draftName.trim(), it) } },
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

@Composable
private fun ProfileAvatarChoice(
    avatar: ProfileAvatar,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberProfileAssetBitmap(avatar.assetPath)
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
        } ?: Box(
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
            "ja" -> R.string.language_japanese
            "zh" -> R.string.language_chinese
            else -> R.string.language_english
        },
    )

@Composable
private fun rememberProfileAssetBitmap(assetPath: String): Bitmap? {
    val context = LocalContext.current
    val bitmap by
        produceState<Bitmap?>(initialValue = null, context, assetPath) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                        context.assets.open(assetPath).use { input -> BitmapFactory.decodeStream(input, null, options) }
                    }.getOrNull()
                }
        }
    return bitmap
}
