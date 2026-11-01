@file:Suppress("FunctionNaming")

package com.signkorean.feature.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.ProfileAvatar
import kotlinx.coroutines.delay

private const val AUTO_SLIDE_INTERVAL_MILLIS = 2_000L

private data class TipCard(
    val id: String,
    val imageAssetUri: String,
)

private val tipCards =
    listOf(
        TipCard("myeongdong-kyoja", "file:///android_asset/signs/generated/streets-2026-10-31/01-myeongdong-kyoja.png"),
        TipCard("ssamziegil", "file:///android_asset/signs/generated/streets-2026-10-31/03-ssamziegil.png"),
        TipCard("gyeongnidan-gil", "file:///android_asset/signs/generated/streets-2026-10-31/05-gyeongnidan-gil.png"),
        TipCard("lotte-dutyfree", "file:///android_asset/signs/generated/streets-2026-10-31-additional/10-lotte-dutyfree.png"),
        TipCard("bukchon-hanbok-rental", "file:///android_asset/signs/generated/streets-2026-10-31-additional/13-bukchon-hanbok-rental.png"),
        TipCard("gwangjang-bindaetteok", "file:///android_asset/signs/generated/streets-2026-10-31-additional/18-gwangjang-bindaetteok.png"),
    )

@Composable
fun TipsScreen(
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
        Spacer(modifier = Modifier.height(SignKoreanSpacing.medium))
        TipsCarousel(cards = tipCards)
        Text(
            text = stringResource(R.string.learn_tips_all_cards),
            modifier = Modifier.padding(horizontal = SignKoreanSpacing.xSmall, vertical = SignKoreanSpacing.medium),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            items(tipCards, key = TipCard::id) { card ->
                TipThumbnail(card)
            }
        }
    }
}

@Composable
private fun TipsCarousel(cards: List<TipCard>) {
    val pagerState = rememberPagerState(pageCount = { Int.MAX_VALUE })
    LaunchedEffect(pagerState) {
        while (true) {
            delay(AUTO_SLIDE_INTERVAL_MILLIS)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(350.dp),
        contentPadding = PaddingValues(horizontal = SignKoreanSpacing.large),
        pageSpacing = SignKoreanSpacing.small,
        pageSize = PageSize.Fixed(290.dp),
    ) { page ->
        TipFeatureCard(cards[page % cards.size])
    }
}

@Composable
private fun TipFeatureCard(card: TipCard) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(SignKoreanPalette.SURFACE),
        shape = RoundedCornerShape(28.dp),
    ) {
        AsyncImage(
            model = card.imageAssetUri,
            contentDescription = stringResource(R.string.learn_tips_card_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
        )
    }
}

@Composable
private fun TipThumbnail(card: TipCard) {
    Surface(
        modifier = Modifier.height(144.dp),
        color = Color(SignKoreanPalette.SURFACE),
        shape = RoundedCornerShape(20.dp),
    ) {
        Image(
            painter = coil3.compose.rememberAsyncImagePainter(card.imageAssetUri),
            contentDescription = stringResource(R.string.learn_tips_card_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
        )
    }
}
