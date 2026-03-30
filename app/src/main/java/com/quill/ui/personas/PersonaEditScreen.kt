package com.quill.ui.personas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quill.ui.theme.ButtonShape

@Composable
fun PersonaEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonaEditViewModel = hiltViewModel(),
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
                text = "< BACK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (state.isNew) "Create Persona" else "Edit Persona",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Give your AI a personality. The system prompt is sent at the start of every conversation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // Name
        LabeledField(label = "NAME") {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                placeholder = { Text("e.g. Code Assistant, Creative Writer") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Description
        LabeledField(label = "DESCRIPTION (optional)") {
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                placeholder = { Text("Brief description of this persona") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(20.dp))

        // System prompt
        LabeledField(label = "SYSTEM PROMPT") {
            OutlinedTextField(
                value = state.systemPrompt,
                onValueChange = viewModel::updateSystemPrompt,
                placeholder = { Text("You are a helpful assistant that...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = fieldColors(),
                maxLines = 20,
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving && state.name.isNotBlank() && state.systemPrompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = if (state.isNew) "Create" else "Save",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    cursorColor = MaterialTheme.colorScheme.primary,
)
