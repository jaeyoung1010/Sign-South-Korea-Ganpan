@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (() -> Unit)? = null,
) {
    var imageSize by remember(imageAssetUri) { mutableStateOf<PhotoSize?>(null) }
    var hasLoadError by remember(imageAssetUri) { mutableStateOf(imageAssetUri.isBlank()) }

    Box(
        modifier =
            modifier
                .heightIn(min = MIN_PHOTO_HEIGHT)
                .background(Color(SignKoreanPalette.INK))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .testTag(SignKoreanTestTags.LEARN_PHOTO),
        contentAlignment = Alignment.Center,
    ) {
        if (imageAssetUri.isNotBlank()) {
            AsyncImage(
                model = imageAssetUri,
                contentDescription = contentDescription?.takeIf { it.isNotBlank() } ?: stringResource(R.string.learn_photo_alt),
                contentScale = contentScale,
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
                val container = PhotoSize(size.width, size.height)
                val image = checkNotNull(imageSize)
                val frame =
                    if (contentScale == ContentScale.Crop) {
                        focusFrameInCropForeground(focus, container, image)
                    } else {
                        focusFrameInFitForeground(focus, container, image)
                    }
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
                text = stringResource(R.string.learn_photo_unavailable),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private val MIN_PHOTO_HEIGHT = 200.dp
private val FOCUS_STROKE_WIDTH = 3.dp
