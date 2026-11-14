@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.ProfileAvatar

private data class HelpPhone(
    val number: String,
    @StringRes val labelRes: Int,
)

private data class HelpService(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val websiteLabelRes: Int,
    val websiteUrl: String,
    val phones: List<HelpPhone>,
    val isEmergency: Boolean = false,
)

private val helpServices =
    listOf(
        HelpService(
            titleRes = R.string.help_1330_title,
            descriptionRes = R.string.help_1330_description,
            websiteLabelRes = R.string.help_1330_website,
            websiteUrl = "https://english.seoul.go.kr/wp-content/uploads/2026/03/guidebook_en.pdf",
            phones = listOf(HelpPhone("1330", R.string.help_call_1330)),
        ),
        HelpService(
            titleRes = R.string.help_emergency_title,
            descriptionRes = R.string.help_emergency_description,
            websiteLabelRes = R.string.help_emergency_website,
            websiteUrl = "https://english.visitkorea.or.kr/svc/contents/contentsView.do?vcontsId=248145",
            phones = listOf(HelpPhone("112", R.string.help_call_112), HelpPhone("119", R.string.help_call_119)),
            isEmergency = true,
        ),
        HelpService(
            titleRes = R.string.help_bbb_title,
            descriptionRes = R.string.help_bbb_description,
            websiteLabelRes = R.string.help_bbb_website,
            websiteUrl = "https://www.bbbkorea.org/en/volunteer.php",
            phones = listOf(HelpPhone("1588-5644", R.string.help_call_1588)),
        ),
        HelpService(
            titleRes = R.string.help_1345_title,
            descriptionRes = R.string.help_1345_description,
            websiteLabelRes = R.string.help_1345_website,
            websiteUrl = "https://immigration.go.kr/immigration/1530/subview.do",
            phones = listOf(HelpPhone("1345", R.string.help_call_1345)),
        ),
        HelpService(
            titleRes = R.string.help_lost_title,
            descriptionRes = R.string.help_lost_description,
            websiteLabelRes = R.string.help_lost_website,
            websiteUrl = "https://www.lost112.go.kr/",
            phones = listOf(HelpPhone("182", R.string.help_call_182)),
        ),
    )

@Composable
fun HelpScreen(
    profileAvatar: ProfileAvatar?,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(
            onOpenNotices = onOpenNotices,
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
            profileAvatar = profileAvatar,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = SignKoreanSpacing.xSmall,
                    end = SignKoreanSpacing.xSmall,
                    top = SignKoreanSpacing.medium,
                    bottom = 104.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            item {
                Text(
                    text = stringResource(R.string.help_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(R.string.help_description),
                    modifier = Modifier.padding(top = SignKoreanSpacing.xxSmall, bottom = SignKoreanSpacing.small),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(SignKoreanPalette.BODY),
                )
            }
            items(helpServices, key = { it.titleRes }) { service ->
                HelpServiceCard(service)
            }
        }
    }
}

@Composable
private fun HelpServiceCard(service: HelpService) {
    val uriHandler = LocalUriHandler.current
    val accent = if (service.isEmergency) Color(SignKoreanPalette.REVIEW_DARK) else Color(SignKoreanPalette.PRIMARY_DARK)
    Surface(
        color = Color(SignKoreanPalette.SURFACE),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(SignKoreanSpacing.medium)) {
            Text(
                text = stringResource(service.titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = accent,
            )
            Text(
                text = stringResource(service.descriptionRes),
                modifier = Modifier.padding(top = SignKoreanSpacing.xxSmall),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(SignKoreanPalette.BODY),
            )
            Spacer(modifier = Modifier.height(SignKoreanSpacing.medium))
            service.phones.forEach { phone ->
                Button(
                    onClick = { uriHandler.openUri("tel:${phone.number}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                ) {
                    Text(stringResource(phone.labelRes), fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(SignKoreanSpacing.xxSmall))
            }
            OutlinedButton(
                onClick = { uriHandler.openUri(service.websiteUrl) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(SignKoreanSpacing.small),
            ) {
                Text(stringResource(service.websiteLabelRes), fontWeight = FontWeight.Bold)
            }
        }
    }
}
