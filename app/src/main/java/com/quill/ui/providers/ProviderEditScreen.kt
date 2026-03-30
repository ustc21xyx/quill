package com.quill.ui.providers

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quill.R
import com.quill.ui.theme.ButtonShape

@Composable
fun ProviderEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProviderEditViewModel = hiltViewModel(),
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
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.back),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (state.isNew) stringResource(R.string.prov_edit_add_title) else stringResource(R.string.prov_edit_edit_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.prov_edit_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        ProviderTextField(
            value = state.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = stringResource(R.string.prov_edit_name),
            placeholder = stringResource(R.string.prov_edit_name_hint),
        )

        Spacer(Modifier.height(20.dp))

        ProviderTextField(
            value = state.baseUrl,
            onValueChange = viewModel::updateBaseUrl,
            label = stringResource(R.string.prov_edit_url),
            placeholder = stringResource(R.string.prov_edit_url_hint),
            keyboardType = KeyboardType.Uri,
        )

        Spacer(Modifier.height(20.dp))

        ProviderTextField(
            value = state.apiKey,
            onValueChange = viewModel::updateApiKey,
            label = stringResource(R.string.prov_edit_key),
            placeholder = stringResource(R.string.prov_edit_key_hint),
            isPassword = true,
        )

        Spacer(Modifier.height(24.dp))

        // Fetch models button
        Row {
            OutlinedButton(
                onClick = viewModel::fetchModels,
                enabled = !state.isFetching && state.baseUrl.isNotBlank() && state.apiKey.isNotBlank(),
                shape = ButtonShape,
            ) {
                if (state.isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp).width(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.prov_edit_test), style = MaterialTheme.typography.labelLarge)
            }
        }

        // Fetch result
        state.fetchError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.fetchedModels?.let { models ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Found ${models.size} models",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            models.take(10).forEach { model ->
                Text(
                    text = "  • ${model.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (models.size > 10) {
                Text(
                    text = "  ... and ${models.size - 10} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving && state.displayName.isNotBlank()
                    && state.baseUrl.isNotBlank() && state.apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = if (state.isNew) stringResource(R.string.prov_edit_save_fetch) else stringResource(R.string.prov_edit_save),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ProviderTextField(
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
