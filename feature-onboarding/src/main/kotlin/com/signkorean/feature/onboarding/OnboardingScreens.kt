@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class OnboardingStep(
    val screenId: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
) {
    Welcome("screen-welcome", R.string.onboarding_welcome_title, R.string.onboarding_welcome_description),
    Tutorial("screen-tutorial", R.string.onboarding_tutorial_title, R.string.onboarding_tutorial_description),
    Level("screen-level", R.string.onboarding_level_title, R.string.onboarding_level_description),
    Interests("screen-interests", R.string.onboarding_interests_title, R.string.onboarding_interests_description),
    Profile("screen-onboarding-profile", R.string.onboarding_profile_title, R.string.onboarding_profile_description),
}

data class OnboardingUiState(
    val step: OnboardingStep,
)

sealed interface OnboardingUiAction {
    data object Continue : OnboardingUiAction

    data object GoBack : OnboardingUiAction
}

sealed interface OnboardingUiEffect {
    data object ContinueToNextStep : OnboardingUiEffect

    data object FinishOnboarding : OnboardingUiEffect
}

object WelcomeTestTags {
    const val COLLAGE = "welcome-collage"
    const val GET_STARTED = "welcome-get-started"
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.step == OnboardingStep.Welcome) {
        WelcomeScreen(
            onGetStarted = { onAction(OnboardingUiAction.Continue) },
            modifier = modifier,
        )
        return
    }

    if (state.step == OnboardingStep.Tutorial) {
        RememberedTutorialScreen(
            onContinue = { onAction(OnboardingUiAction.Continue) },
            modifier = modifier,
        )
        return
    }

    GameCard(modifier = modifier.testTag(state.step.screenId)) {
        Text(text = stringResource(state.step.titleRes), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(state.step.descriptionRes), style = MaterialTheme.typography.bodyLarge)
        GameButton(
            text =
                if (state.step ==
                    OnboardingStep.Interests
                ) {
                    stringResource(R.string.onboarding_start_learning)
                } else {
                    stringResource(R.string.onboarding_continue)
                },
            onClick = { onAction(OnboardingUiAction.Continue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().testTag(OnboardingStep.Welcome.screenId),
    ) {
        OnboardingProgressIndicator(modifier = Modifier.padding(top = SignKoreanSpacing.small))
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = SignKoreanSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
        ) {
            WelcomeCollage()
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_description),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = SignKoreanSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            GameButton(
                text = stringResource(R.string.onboarding_welcome_get_started),
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().testTag(WelcomeTestTags.GET_STARTED),
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_account_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(SignKoreanPalette.MUTED),
            )
        }
    }
}

@Composable
internal fun OnboardingProgressIndicator(
    modifier: Modifier = Modifier,
    activeSteps: Int = 1,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index < activeSteps) Color(SignKoreanPalette.PRIMARY) else Color(SignKoreanPalette.BORDER),
                        ),
            )
        }
    }
}

@Composable
private fun WelcomeCollage() {
    val collageDescription = stringResource(R.string.onboarding_welcome_collage_description)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(172.dp)
                .testTag(WelcomeTestTags.COLLAGE)
                .semantics {
                    contentDescription = collageDescription
                },
    ) {
        CollagePhoto(
            assetPath = "signs/bunsik.jpg",
            fallbackColor = Color(SignKoreanPalette.Y2K_YELLOW),
            modifier = Modifier.align(Alignment.BottomStart).width(152.dp).fillMaxHeight(0.82f),
        )
        CollagePhoto(
            assetPath = "signs/exit.jpg",
            fallbackColor = Color(SignKoreanPalette.LILAC),
            modifier = Modifier.align(Alignment.TopCenter).width(156.dp).fillMaxHeight(0.88f),
        )
        CollagePhoto(
            assetPath = "signs/pharmacy.jpg",
            fallbackColor = Color(SignKoreanPalette.GOT_IT),
            modifier = Modifier.align(Alignment.BottomEnd).width(126.dp).fillMaxHeight(0.72f),
        )
    }
}

@Composable
private fun CollagePhoto(
    assetPath: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberAssetBitmap(assetPath)
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(18.dp))
                .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun rememberAssetBitmap(
    assetPath: String,
    sampleSize: Int = 1,
): Bitmap? {
    val context = LocalContext.current
    val bitmap by
        produceState<Bitmap?>(initialValue = null, context, assetPath, sampleSize) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) }
                        context.assets.open(assetPath).use { input -> BitmapFactory.decodeStream(input, null, options) }
                    }.getOrNull()
                }
        }
    return bitmap
}
