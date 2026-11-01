@file:Suppress("FunctionNaming")

package com.signkorean.feature.learn

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.ProfileAvatar
private data class TipPhrase(
    val korean: String,
    @StringRes val meaningRes: Int,
)

private data class TipCard(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val imageAssetUri: String,
    val phrases: List<TipPhrase>,
)

private val tipCards =
    listOf(
        TipCard(
            id = "price-numbers",
            titleRes = R.string.learn_tip_price_title,
            descriptionRes = R.string.learn_tip_price_description,
            imageAssetUri = "file:///android_asset/tips/price-numbers-helper.png",
            phrases = listOf(
                TipPhrase("₩5,000  →  오천 원", R.string.learn_tip_price_five_thousand),
                TipPhrase("₩12,000  →  만 이천 원", R.string.learn_tip_price_twelve_thousand),
                TipPhrase("2인분  →  이 인분", R.string.learn_tip_price_two_servings),
                TipPhrase("1+1  →  원 플러스 원", R.string.learn_tip_price_one_plus_one),
            ),
        ),
        TipCard(
            id = "dietary-allergy",
            titleRes = R.string.learn_tip_dietary_title,
            descriptionRes = R.string.learn_tip_dietary_description,
            imageAssetUri = "file:///android_asset/tips/dietary-allergy-card.png",
            phrases = listOf(
                TipPhrase("저는 채식주의자입니다.", R.string.learn_tip_dietary_vegetarian),
                TipPhrase("돼지고기를 먹지 않습니다.", R.string.learn_tip_dietary_no_pork),
                TipPhrase("매운 음식을 못 먹습니다.", R.string.learn_tip_dietary_not_spicy),
                TipPhrase("땅콩이 들어갔나요?", R.string.learn_tip_dietary_peanuts),
                TipPhrase("할랄 음식인가요?", R.string.learn_tip_dietary_halal),
                TipPhrase("고수는 빼 주세요.", R.string.learn_tip_dietary_no_cilantro),
            ),
        ),
        TipCard(
            id = "staff-phrases",
            titleRes = R.string.learn_tip_staff_title,
            descriptionRes = R.string.learn_tip_staff_description,
            imageAssetUri = "file:///android_asset/tips/staff-phrase-board.png",
            phrases = listOf(
                TipPhrase("천천히 말해 주세요.", R.string.learn_tip_staff_slowly),
                TipPhrase("영어를 할 수 있나요?", R.string.learn_tip_staff_english),
                TipPhrase("메뉴판을 보여 주세요.", R.string.learn_tip_staff_menu),
                TipPhrase("포장해 주세요.", R.string.learn_tip_staff_takeaway),
                TipPhrase("여기서 먹을게요.", R.string.learn_tip_staff_dine_in),
                TipPhrase("화장실이 어디예요?", R.string.learn_tip_staff_restroom),
                TipPhrase("예약하지 않았어요.", R.string.learn_tip_staff_no_reservation),
            ),
        ),
        TipCard(
            id = "emergency",
            titleRes = R.string.learn_tip_emergency_title,
            descriptionRes = R.string.learn_tip_emergency_description,
            imageAssetUri = "file:///android_asset/tips/emergency-phrases.png",
            phrases = listOf(
                TipPhrase("도와주세요.", R.string.learn_tip_emergency_help),
                TipPhrase("경찰을 불러 주세요.", R.string.learn_tip_emergency_police),
                TipPhrase("병원이 어디예요?", R.string.learn_tip_emergency_hospital),
                TipPhrase("여권을 잃어버렸어요.", R.string.learn_tip_emergency_passport),
                TipPhrase("길을 잃었어요.", R.string.learn_tip_emergency_lost),
                TipPhrase("알레르기가 있어요.", R.string.learn_tip_emergency_allergy),
            ),
        ),
        TipCard(
            id = "transport",
            titleRes = R.string.learn_tip_transport_title,
            descriptionRes = R.string.learn_tip_transport_description,
            imageAssetUri = "file:///android_asset/tips/transport-mini-card.png",
            phrases = listOf(
                TipPhrase("어느 역에서 내려요?", R.string.learn_tip_transport_stop),
                TipPhrase("몇 호선이에요?", R.string.learn_tip_transport_line),
                TipPhrase("환승해야 하나요?", R.string.learn_tip_transport_transfer),
                TipPhrase("막차가 몇 시예요?", R.string.learn_tip_transport_last_train),
                TipPhrase("이 버스가 명동에 가나요?", R.string.learn_tip_transport_myeongdong),
            ),
        ),
    )

@Composable
fun TipsScreen(
    profileAvatar: ProfileAvatar?,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var phraseToShow by remember { mutableStateOf<TipPhrase?>(null) }
    val selectedCard = tipCards.firstOrNull { it.id == selectedCardId }
    BackHandler(enabled = selectedCard != null && phraseToShow == null) {
        selectedCardId = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(
            onOpenNotices = onOpenNotices,
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
            profileAvatar = profileAvatar,
        )
        if (selectedCard == null) {
            TipsOverview(onOpenCard = { selectedCardId = it.id })
        } else {
            TipDetail(
                card = selectedCard,
                onBack = { selectedCardId = null },
                onShowPhrase = { phraseToShow = it },
            )
        }
    }
    phraseToShow?.let { phrase ->
        PhraseBoard(phrase = phrase, onDismiss = { phraseToShow = null })
    }
}

@Composable
private fun ColumnScope.TipsOverview(onOpenCard: (TipCard) -> Unit) {
    Spacer(modifier = Modifier.height(SignKoreanSpacing.medium))
    Text(
        text = stringResource(R.string.learn_tips_title),
        modifier = Modifier.padding(horizontal = SignKoreanSpacing.xSmall),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
    )
    Text(
        text = stringResource(R.string.learn_tips_description),
        modifier = Modifier.padding(horizontal = SignKoreanSpacing.xSmall, vertical = SignKoreanSpacing.xxSmall),
        style = MaterialTheme.typography.bodyLarge,
        color = Color(SignKoreanPalette.BODY),
    )
    TipsCarousel(cards = tipCards, onOpenCard = onOpenCard)
    Text(
        text = stringResource(R.string.learn_tips_all_cards),
        modifier = Modifier.padding(horizontal = SignKoreanSpacing.xSmall, vertical = SignKoreanSpacing.medium),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(start = SignKoreanSpacing.xSmall, end = SignKoreanSpacing.xSmall, bottom = 104.dp),
        horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
    ) {
        items(tipCards, key = TipCard::id) { card ->
            TipThumbnail(card = card, onClick = { onOpenCard(card) })
        }
    }
}

@Composable
private fun TipsCarousel(cards: List<TipCard>, onOpenCard: (TipCard) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { cards.size })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(350.dp),
        contentPadding = PaddingValues(horizontal = SignKoreanSpacing.large),
        pageSpacing = SignKoreanSpacing.small,
        pageSize = PageSize.Fixed(290.dp),
    ) { page ->
        val card = cards[page]
        TipFeatureCard(card = card, onClick = { onOpenCard(card) })
    }
}

@Composable
private fun TipFeatureCard(card: TipCard, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
        color = Color(SignKoreanPalette.SURFACE),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 2.dp,
    ) {
        Column {
            AsyncImage(
                model = card.imageAssetUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(190.dp),
            )
            Column(modifier = Modifier.padding(SignKoreanSpacing.medium)) {
                Text(
                    text = stringResource(card.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(SignKoreanSpacing.xxSmall))
                Text(
                    text = stringResource(card.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(SignKoreanPalette.BODY),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.learn_tips_open),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(SignKoreanPalette.PRIMARY_DARK),
                )
            }
        }
    }
}

@Composable
private fun TipThumbnail(card: TipCard, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(168.dp).clickable(onClick = onClick),
        color = Color(SignKoreanPalette.SURFACE),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box {
            AsyncImage(
                model = card.imageAssetUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f)).padding(SignKoreanSpacing.small),
            ) {
                Text(
                    text = stringResource(card.titleRes),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TipDetail(card: TipCard, onBack: () -> Unit, onShowPhrase: (TipPhrase) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = SignKoreanSpacing.xSmall),
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(SignKoreanPalette.INK)),
            contentPadding = PaddingValues(vertical = SignKoreanSpacing.small),
        ) {
            Text("← ${stringResource(R.string.learn_tips_back)}", fontWeight = FontWeight.Bold)
        }
        AsyncImage(
            model = card.imageAssetUri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(24.dp)),
        )
        Spacer(modifier = Modifier.height(SignKoreanSpacing.medium))
        Text(stringResource(card.titleRes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            text = stringResource(card.descriptionRes),
            modifier = Modifier.padding(top = SignKoreanSpacing.xxSmall),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(SignKoreanPalette.BODY),
        )
        Text(
            text = stringResource(R.string.learn_tips_tap_to_show),
            modifier = Modifier.padding(top = SignKoreanSpacing.medium, bottom = SignKoreanSpacing.small),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(SignKoreanPalette.PRIMARY_DARK),
        )
        card.phrases.forEach { phrase ->
            PhraseRow(phrase = phrase, onClick = { onShowPhrase(phrase) })
            Spacer(modifier = Modifier.height(SignKoreanSpacing.small))
        }
        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun PhraseRow(phrase: TipPhrase, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color(SignKoreanPalette.SURFACE),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(SignKoreanSpacing.medium)) {
            Text(phrase.korean, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                text = stringResource(phrase.meaningRes),
                modifier = Modifier.padding(top = SignKoreanSpacing.xxSmall),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(SignKoreanPalette.BODY),
            )
        }
    }
}

@Composable
private fun PhraseBoard(phrase: TipPhrase, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(SignKoreanPalette.BACKGROUND)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(SignKoreanSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(SignKoreanPalette.SURFACE)),
                    ) {
                        Text(stringResource(R.string.learn_close), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = phrase.korean,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(phrase.meaningRes),
                    modifier = Modifier.padding(top = SignKoreanSpacing.medium),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = Color(SignKoreanPalette.BODY),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.learn_tips_show_to_staff),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(SignKoreanPalette.BODY),
                )
            }
        }
    }
}
