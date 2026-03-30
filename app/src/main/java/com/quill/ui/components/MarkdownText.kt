package com.quill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quill.R
import dev.jeziellago.compose.markdowntext.MarkdownText as LibMarkdownText

private data class MarkdownSegment(
    val content: String,
    val isCode: Boolean,
    val language: String = "",
)

private fun parseMarkdownSegments(markdown: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val codeBlockRegex = Regex("```(\\w*)\\s*\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
    var lastEnd = 0

    for (match in codeBlockRegex.findAll(markdown)) {
        val beforeCode = markdown.substring(lastEnd, match.range.first)
        if (beforeCode.isNotBlank()) {
            segments.add(MarkdownSegment(content = beforeCode.trim(), isCode = false))
        }
        val language = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        segments.add(MarkdownSegment(content = code, isCode = true, language = language))
        lastEnd = match.range.last + 1
    }

    val remaining = markdown.substring(lastEnd)
    if (remaining.isNotBlank()) {
        segments.add(MarkdownSegment(content = remaining.trim(), isCode = false))
    }

    return segments
}

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    val segments = parseMarkdownSegments(content)

    Column(modifier = modifier) {
        for (segment in segments) {
            if (segment.isCode) {
                CodeBlock(
                    code = segment.content,
                    language = segment.language,
                )
            } else {
                LibMarkdownText(
                    markdown = segment.content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    linkColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(
    code: String,
    language: String,
) {
    val clipboardManager = LocalClipboardManager.current
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape),
    ) {
        // Header row with language label and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language.ifEmpty { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { clipboardManager.setText(AnnotatedString(code)) },
            ) {
                Text(
                    text = stringResource(R.string.code_copy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
