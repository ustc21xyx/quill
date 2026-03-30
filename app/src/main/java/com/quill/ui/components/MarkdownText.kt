package com.quill.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jeziellago.compose.markdowntext.MarkdownText as LibMarkdownText

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    LibMarkdownText(
        markdown = content,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        linkColor = MaterialTheme.colorScheme.primary,
    )
}
