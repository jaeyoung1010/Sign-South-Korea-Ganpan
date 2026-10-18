@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanShapes
import com.signkorean.designsystem.theme.SignKoreanSpacing

enum class GameButtonStyle {
    Primary,
    GotIt,
    Review,
    Secondary,
}

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GameButtonStyle = GameButtonStyle.Primary,
) {
    val (container, content) =
        when (style) {
            GameButtonStyle.Primary -> Color(SignKoreanPalette.PRIMARY) to Color(SignKoreanPalette.INK)
            GameButtonStyle.GotIt -> Color(SignKoreanPalette.GOT_IT) to Color(SignKoreanPalette.INK)
            GameButtonStyle.Review -> Color(SignKoreanPalette.REVIEW) to Color(SignKoreanPalette.INK)
            GameButtonStyle.Secondary -> Color.White to Color(SignKoreanPalette.INK)
        }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .defaultMinSize(minHeight = 48.dp)
                .heightIn(min = 52.dp)
                .shadow(3.dp, SignKoreanShapes.medium, clip = false)
                .semantics { role = Role.Button },
        shape = SignKoreanShapes.medium,
        border = BorderStroke(2.dp, Color(SignKoreanPalette.OUTLINE)),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container.copy(alpha = 0.42f),
                disabledContentColor = content.copy(alpha = 0.68f),
            ),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = SignKoreanSpacing.large,
                vertical = SignKoreanSpacing.small,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
fun GameChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Color(SignKoreanPalette.Y2K_YELLOW),
) {
    Surface(
        modifier = modifier,
        color = accent,
        contentColor = Color(SignKoreanPalette.INK),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(2.dp, Color(SignKoreanPalette.OUTLINE)),
        shadowElevation = 2.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = SignKoreanSpacing.small, vertical = SignKoreanSpacing.xSmall),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.shadow(4.dp, SignKoreanShapes.large, clip = false),
        color = Color(SignKoreanPalette.SURFACE),
        contentColor = Color(SignKoreanPalette.INK),
        shape = SignKoreanShapes.large,
        border = BorderStroke(2.dp, Color(SignKoreanPalette.OUTLINE)),
    ) {
        Column(
            modifier = Modifier.padding(SignKoreanSpacing.large),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            content = content,
        )
    }
}

@Composable
fun GameSheet(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(4.dp, SignKoreanShapes.extraLarge, clip = false),
        color = Color(SignKoreanPalette.SURFACE),
        contentColor = Color(SignKoreanPalette.INK),
        shape = SignKoreanShapes.extraLarge,
        border = BorderStroke(2.dp, Color(SignKoreanPalette.OUTLINE)),
    ) {
        Column(
            modifier = Modifier.padding(SignKoreanSpacing.large),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
fun GameToast(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 360.dp),
        color = Color(SignKoreanPalette.INK),
        contentColor = Color.White,
        shape = SignKoreanShapes.medium,
        shadowElevation = 6.dp,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = SignKoreanSpacing.medium, vertical = SignKoreanSpacing.small),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun GameIcon(
    symbol: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = symbol,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun GameButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
