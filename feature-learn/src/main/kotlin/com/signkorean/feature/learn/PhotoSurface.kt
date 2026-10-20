@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.domain.model.FocusRect

@Composable
internal fun PhotoSurface(
    imageAssetUri: String,
    contentDescription: String?,
    focus: FocusRect?,
    modifier: Modifier = Modifier,
) {
    var imageSize by remember(imageAssetUri) { mutableStateOf<PhotoSize?>(null) }
    var hasLoadError by remember(imageAssetUri) { mutableStateOf(imageAssetUri.isBlank()) }
    val backgroundModifier =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(BLUR_RADIUS)
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .heightIn(min = MIN_PHOTO_HEIGHT)
                .background(Color(SignKoreanPalette.INK))
                .testTag(SignKoreanTestTags.LEARN_PHOTO),
        contentAlignment = Alignment.Center,
    ) {
        if (imageAssetUri.isNotBlank()) {
            AsyncImage(
                model = imageAssetUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = backgroundModifier.fillMaxSize(),
            )
            AsyncImage(
                model = imageAssetUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { success ->
                    val image = success.result.image
                    if (image.width > 0 && image.height > 0) {
                        imageSize = PhotoSize(image.width.toFloat(), image.height.toFloat())
                    }
                },
                onError = { hasLoadError = true },
            )
        }
        if (imageSize != null && focus != null) {
            Canvas(modifier = Modifier.fillMaxSize().testTag(SignKoreanTestTags.LEARN_FOCUS_FRAME)) {
                val frame =
                    focusFrameInFitForeground(
                        focus = focus,
                        container = PhotoSize(size.width, size.height),
                        image = checkNotNull(imageSize),
                    )
                drawRect(
                    color = Color(SignKoreanPalette.Y2K_YELLOW),
                    topLeft = Offset(frame.left, frame.top),
                    size = Size(frame.width, frame.height),
                    style = Stroke(width = FOCUS_STROKE_WIDTH.toPx()),
                )
            }
        }
        if (hasLoadError) {
            Text(
                text = "Photo unavailable",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = "↗",
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clearAndSetSemantics {},
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private val MIN_PHOTO_HEIGHT = 180.dp
private val BLUR_RADIUS = 18.dp
private val FOCUS_STROKE_WIDTH = 3.dp
