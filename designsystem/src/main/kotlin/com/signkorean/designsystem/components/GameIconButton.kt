@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.theme.SignKoreanPalette

/** A labelled, 48dp-minimum icon action for the prototype's circular controls. */
@Composable
fun GameIconButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .widthIn(min = 48.dp)
                .heightIn(min = 48.dp)
                .border(1.dp, Color(SignKoreanPalette.INK), CircleShape)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = Color(SignKoreanPalette.SURFACE),
                contentColor = Color(SignKoreanPalette.INK),
            ),
        shape = CircleShape,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}
