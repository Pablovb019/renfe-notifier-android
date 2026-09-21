package com.pablovb019.renfenotifier.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.ui.theme.ThemeMode

/**
 * Selector de modo de tema Material 3. Lista vertical agrupada con
 * `selectableGroup`; cada fila es `selectable` con [Role.RadioButton] y un
 * `RadioButton` decorativo. Altura mínima 56.dp (área táctil >= 48x48).
 * Muestra el estado de carga inicial y los errores de guardado, si los hay.
 */
@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    saveError: String? = null,
) {
    val options = listOf(
        ThemeOption(
            mode = ThemeMode.SYSTEM,
            label = R.string.diag_theme_system,
            description = R.string.diag_theme_system_desc,
        ),
        ThemeOption(
            mode = ThemeMode.LIGHT,
            label = R.string.diag_theme_light,
            description = null,
        ),
        ThemeOption(
            mode = ThemeMode.DARK,
            label = R.string.diag_theme_dark,
            description = null,
        ),
    )
    Column(modifier = modifier.selectableGroup()) {
        options.forEach { option ->
            val isSelected = selected == option.mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(option.mode) },
                        role = Role.RadioButton,
                    )
                    .testTag("theme_option_${option.mode.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(option.label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    option.description?.let { description ->
                        Text(
                            text = stringResource(description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.diag_theme_loading),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (saveError != null) {
            Text(
                text = saveError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private data class ThemeOption(
    val mode: ThemeMode,
    val label: Int,
    val description: Int?,
)