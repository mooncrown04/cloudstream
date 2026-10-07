package com.mihon.presentation.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.compose.SingleSelectDialog
import com.lagradost.cloudstream4.generated.resources.Res
import com.lagradost.cloudstream4.generated.resources.cancel
import com.lagradost.cloudstream4.generated.resources.ok
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: Painter?,
    entries: Map<out T, String>,
    onValueChange: (T) -> Unit,
    iconProvider: (@Composable (key: T, value: String) -> Unit)? = null
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        // M3 MaterialTheme içinden seçilen aktif fontu ve tipografiyi alır
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography
        ) {
            SingleSelectDialog(
                dismiss = { isDialogShown = false },
                title = title,
                entries = entries,
                selectedKey = value,
                confirm = { key ->
                    if (key != null) {
                        onValueChange(key)
                    }
                    isDialogShown = false
                },
                confirmText = stringResource(Res.string.ok),
                dismissText = stringResource(Res.string.cancel),
                iconProvider = iconProvider
            )
        }
    }
}

/**
 * Alternatif: Eğer SingleSelectDialog yerine kendi AlertDialog yapınızı kullanmak isterseniz
 * aşağıdaki composable bileşenini aktif edip kullanabilirsiniz.
 */
@Composable
private fun <T> CustomListPreferenceDialog(
    title: String,
    entries: Map<out T, String>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Box {
                val state = rememberLazyListState()
                LazyColumn(state = state) {
                    items(entries.entries.toList()) { entry ->
                        val isSelected = selectedValue == entry.key
                        DialogRow(
                            label = entry.value,
                            isSelected = isSelected,
                            onSelected = {
                                onValueChange(entry.key)
                                onDismissRequest()
                            }
                        )
                    }
                }
                if (state.canScrollBackward) {
                    HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                }
                if (state.canScrollForward) {
                    HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(Res.string.cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun DialogRow(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(
                selected = isSelected,
                onClick = { if (!isSelected) onSelected() },
            )
            .fillMaxWidth()
            .minimumInteractiveComponentSize(),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}
