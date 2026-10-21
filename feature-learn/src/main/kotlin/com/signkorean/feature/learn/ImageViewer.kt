@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanPalette

@Composable
fun LearnImageViewer(
    state: LearnSessionUiState.Active,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo = state.photo
    val imageContentDescription =
        if (state.stage == LearnCardStage.Question) {
            photo.questionContentDescription
        } else {
            photo.answerContentDescription
        }
    var transform by remember(photo.imageAssetUri) { mutableStateOf(ImageViewerTransform()) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(SignKoreanPalette.INK))
                .testTag(SignKoreanTestTags.LEARN_IMAGE_VIEWER),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .pointerInput(photo.imageAssetUri) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            transform =
                                transform.applyGesture(
                                    zoomChange = zoom,
                                    panX = pan.x,
                                    panY = pan.y,
                                    rotationDelta = rotation,
                                )
                        }
                    }.pointerInput(photo.imageAssetUri) {
                        detectTapGestures(
                            onDoubleTap = { transform = transform.reset() },
                        )
                    }.graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offsetX
                        translationY = transform.offsetY
                        rotationZ = transform.rotationDegrees
                    },
            contentAlignment = Alignment.Center,
        ) {
            PhotoSurface(
                imageAssetUri = photo.imageAssetUri,
                contentDescription = imageContentDescription,
                focus = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.stage == LearnCardStage.Answer) {
            Text(
                text = state.sign,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }

        GameButton(
            text = "Close viewer",
            onClick = onClose,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .testTag(SignKoreanTestTags.LEARN_IMAGE_VIEWER_CLOSE),
            style = GameButtonStyle.Secondary,
        )
    }
}
