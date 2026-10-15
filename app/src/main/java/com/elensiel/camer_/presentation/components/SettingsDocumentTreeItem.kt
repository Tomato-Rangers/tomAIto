package com.elensiel.camer_.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDocumentTreeItem(
    title: String,
    location: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsItemRow(
        title = title,
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Text(
            text = location,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
