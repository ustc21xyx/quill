package com.quill.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quill.domain.model.MessageRole
import com.quill.ui.components.AiBubble
import com.quill.ui.components.UserBubble
import com.quill.ui.theme.InputPillShape

@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive or streaming
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            // Top bar
            ChatTopBar(
                title = state.conversationTitle,
                selectedModelName = state.selectedModel?.displayName,
                availableModels = state.availableModels.map { it.id to it.displayName },
                onSelectModel = viewModel::selectModel,
                onOpenDrawer = onOpenDrawer,
            )

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Quill",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Start typing...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    when (message.role) {
                        MessageRole.ASSISTANT -> AiBubble(
                            message = message,
                            onRegenerate = viewModel::regenerate,
                        )
                        MessageRole.USER -> UserBubble(message = message)
                        MessageRole.SYSTEM -> {} // Not displayed
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Input bar - always visible and ready
            ChatInputBar(
                text = state.inputText,
                onTextChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                isStreaming = state.isStreaming,
                modifier = Modifier
                    .navigationBarsPadding(),
            )
        }

        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                action = {
                    TextButton(onClick = viewModel::dismissError) {
                        Text("DISMISS")
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(error, maxLines = 3)
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    selectedModelName: String?,
    availableModels: List<Pair<String, String>>,
    onSelectModel: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    var showModelMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hamburger menu for conversation drawer
        TextButton(onClick = onOpenDrawer) {
            Text(
                text = "\u2630",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.width(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Model selector
        if (selectedModelName != null) {
            Box {
                TextButton(onClick = { showModelMenu = true }) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MODEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = selectedModelName,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                ) {
                    availableModels.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onSelectModel(id)
                                showModelMenu = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = InputPillShape,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "Start typing...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        )
                    }
                    innerTextField()
                },
            )

            Spacer(Modifier.width(8.dp))

            // Send button
            Surface(
                onClick = { if (!isStreaming && text.isNotBlank()) onSend() },
                color = if (text.isNotBlank() && !isStreaming) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                },
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "\u2191", // up arrow
                        style = MaterialTheme.typography.titleMedium,
                        color = if (text.isNotBlank() && !isStreaming) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            }
        }
    }
}
