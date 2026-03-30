package com.quill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.quill.ui.chat.ChatMessageUi
import com.quill.ui.theme.AiBubbleShape
import com.quill.ui.theme.UserBubbleShape

@Composable
fun AiBubble(
    message: ChatMessageUi,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.85f).dp
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Q",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.surface,
                )
            }
            Spacer(Modifier.padding(start = 8.dp))
            Text(
                text = if (message.isStreaming) "COMPOSING..." else "QUILL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        // Bubble
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = AiBubbleShape,
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .drawBehind {
                    drawRect(
                        color = primaryColor.copy(alpha = 0.2f),
                        topLeft = Offset.Zero,
                        size = size.copy(width = 2.dp.toPx()),
                    )
                },
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Thinking block
                if (!message.thinkingContent.isNullOrEmpty()) {
                    ThinkingBlock(
                        content = message.thinkingContent,
                        isStreaming = message.isStreaming && message.content.isEmpty(),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Content
                if (message.content.isNotEmpty() || message.isStreaming) {
                    StreamingText(
                        text = message.content,
                        isStreaming = message.isStreaming,
                    )
                }

                // Actions (only when not streaming)
                if (!message.isStreaming && message.content.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(message.content))
                        }) {
                            Text(
                                "COPY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        TextButton(onClick = onRegenerate) {
                            Text(
                                "REGENERATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserBubble(
    message: ChatMessageUi,
    modifier: Modifier = Modifier,
) {
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.85f).dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 4.dp, bottom = 6.dp),
        ) {
            Text(
                text = "YOU",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.padding(start = 8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "U",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Bubble
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = UserBubbleShape,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
