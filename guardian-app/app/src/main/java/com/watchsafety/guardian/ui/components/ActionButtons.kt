package com.watchsafety.guardian.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GuardianButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun EmergencyActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GuardianButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun GuardianButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    containerColor: Color,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
