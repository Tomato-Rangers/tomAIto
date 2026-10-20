package com.tomatorangers.detection.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.text.Typography.bullet

@Composable
fun ConfidenceThresholdDialog(
    initial: Float,
    onChange: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableFloatStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confidence threshold") },
        text = {
            Column {
                Text(
                    "The minimum certainty the model needs before it shows a detection.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$bullet Higher: fewer boxes and fewer false alarms, but real tomatoes may be missed.\n" +
                            "$bullet Lower: more tomatoes found, but more wrong boxes.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "${(value * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it; onChange(it) },
                    onValueChangeFinished = { onCommit(value) },
                    valueRange = 0.1f..0.95f,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
