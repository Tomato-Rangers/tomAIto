package com.elensiel.camer_.presentation.components

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingsItemRow(
        title = title,
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onToggle,
            enabled = enabled,
        ),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}
