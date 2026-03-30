package com.quill.ui.models

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quill.ui.theme.ButtonShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // Back button
        TextButton(onClick = onBack) {
            Text(
                text = "< BACK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (state.isNew) "Register Model" else "Edit Model",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(32.dp))

        // Provider dropdown
        var providerExpanded by remember { mutableStateOf(false) }
        val selectedProvider = state.providers.find { it.id == state.providerId }

        Column {
            Text(
                text = "PROVIDER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedProvider?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text(
                            text = "Select a provider",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false },
                ) {
                    state.providers.forEach { provider ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = provider.baseUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            },
                            onClick = {
                                viewModel.updateProviderId(provider.id)
                                providerExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (state.providers.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No providers configured. Add a provider first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(20.dp))

        QuillTextField(
            value = state.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = "DISPLAY NAME",
            placeholder = "e.g. Claude Sonnet",
        )

        Spacer(Modifier.height(20.dp))

        QuillTextField(
            value = state.modelId,
            onValueChange = viewModel::updateModelId,
            label = "MODEL ID",
            placeholder = "e.g. gpt-4o, claude-sonnet-4-20250514",
        )

        Spacer(Modifier.height(20.dp))

        // Temperature
        Column {
            Text(
                text = "TEMPERATURE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = state.temperature,
                    onValueChange = viewModel::updateTemperature,
                    valueRange = 0f..2f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "%.1f".format(state.temperature),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Max tokens
        QuillTextField(
            value = state.maxTokens.toString(),
            onValueChange = { viewModel.updateMaxTokens(it.toIntOrNull() ?: 4096) },
            label = "MAX TOKENS",
            placeholder = "4096",
            keyboardType = KeyboardType.Number,
        )

        Spacer(Modifier.height(24.dp))

        // Test connection
        Row {
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !state.isTesting && state.providerId.isNotBlank(),
                shape = ButtonShape,
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp).width(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Test Connection", style = MaterialTheme.typography.labelLarge)
            }
        }

        state.testResult?.let { result ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = result,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.startsWith("Connected")) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving && state.displayName.isNotBlank()
                    && state.providerId.isNotBlank() && state.modelId.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = if (state.isNew) "Register" else "Save",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun QuillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                )
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            singleLine = true,
        )
    }
}
