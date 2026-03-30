package com.quill.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isStreaming) {
        val transition = rememberInfiniteTransition(label = "cursor")
        val cursorAlpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1060
                    1f at 0
                    1f at 530
                    0f at 531
                    0f at 1060
                },
            ),
            label = "cursorAlpha",
        )

        val annotated = buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))) {
                append("\u2758") // vertical bar cursor
            }
        }

        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
    } else {
        // When not streaming, render as markdown
        MarkdownText(content = text, modifier = modifier)
    }
}
