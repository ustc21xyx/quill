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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.quill.R
import com.quill.ui.chat.ChatMessageUi
import com.quill.ui.theme.AiBubbleShape
import com.quill.ui.theme.UserBubbleShape
import io.getstream.chat.android.ai.compose.ui.component.StreamingText as StreamAiText

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
        // Header — minimal, literary style
        Text(
            text = if (message.isStreaming) stringResource(R.string.chat_composing) else stringResource(R.string.chat_quill),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )

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

                // Content — always use StreamAiText for consistent rendering
                if (message.content.isNotEmpty() || message.isStreaming) {
                    StreamAiText(
                        text = message.content,
                        animate = message.isStreaming,
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
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.chat_copy),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        TextButton(onClick = onRegenerate) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.chat_regenerate),
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
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.85f).dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        // Header — minimal, literary style
        Text(
            text = stringResource(R.string.chat_you),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(end = 4.dp, bottom = 6.dp).align(Alignment.End),
        )

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
                modifier = Modifier.padding(16.dp),
            )
        }

        // Edit button outside bubble
        TextButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.chat_edit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
