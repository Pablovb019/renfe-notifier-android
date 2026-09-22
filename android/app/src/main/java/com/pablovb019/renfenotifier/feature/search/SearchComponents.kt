package com.pablovb019.renfenotifier.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.Availability
import com.pablovb019.renfenotifier.core.model.FollowUpMode
import com.pablovb019.renfenotifier.core.network.model.StationOut
import com.pablovb019.renfenotifier.core.network.model.TrainOut
import com.pablovb019.renfenotifier.ui.components.BadgeType
import com.pablovb019.renfenotifier.ui.components.RenfeStatusBadge
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Campo de estación con sugerencias EN FLUJO (nunca en popup): el contenedor
 * limita su altura a 192 dp y no anida scroll vertical propio dentro del
 * LazyColumn de la pantalla (el propio LazyColumn es quien scrollea la página).
 * Cada sugerencia ocupa al menos 48 dp y admite 2 líneas con elipsis.
 */
@Composable
fun SearchStationField(
    label: String,
    value: String,
    suggestions: List<StationOut>,
    onQueryChange: (String) -> Unit,
    onSelected: (StationOut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showSuggestions = value.isNotEmpty() && suggestions.isNotEmpty()
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            label = { Text(text = label) },
            singleLine = true,
        )
        if (showSuggestions) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 192.dp)
                    .padding(top = RenfeSpacing.xs),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ) {
                Column {
                    suggestions.forEach { station ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onSelected(station) }
                                .padding(horizontal = RenfeSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (station.isGroup) {
                                    stringResource(R.string.search_station_group, station.name)
                                } else {
                                    station.name
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de tren: hora de salida/llegada, identificador (la API de Renfe no
 * expone un "tipo" de tren, por lo que se muestra el identity), badge de
 * disponibilidad y precio. Un precio ausente se muestra como "Precio no
 * disponible" y jamás como 0 €. Un click selecciona el tren.
 */
@Composable
fun SearchTrainCard(
    train: TrainOut,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trainDescription = stringResource(R.string.search_train_desc, train.identity)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = trainDescription }
            .clickable(onClick = onSelected),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(RenfeSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.search_train_time,
                        formatTime(train.departure),
                        formatTime(train.arrival),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                RenfeStatusBadge(
                    label = availabilityLabel(train.availability),
                    icon = null,
                    type = availabilityBadgeType(train.availability),
                )
            }
            Text(
                text = stringResource(R.string.search_train_type, train.identity),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (train.price != null) {
                Text(
                    text = stringResource(R.string.search_price, train.price),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.search_price_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Text(
                    text = stringResource(R.string.search_train_selected),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Selector de modo de seguimiento: los 4 modos (FIRST/LAST/ALL/SPECIFIC) en FlowRow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFollowUpModes(
    mode: FollowUpMode,
    onModeSelected: (FollowUpMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
    ) {
        FilterChip(
            selected = mode == FollowUpMode.FIRST,
            onClick = { onModeSelected(FollowUpMode.FIRST) },
            label = { Text(text = stringResource(R.string.search_mode_first)) },
        )
        FilterChip(
            selected = mode == FollowUpMode.LAST,
            onClick = { onModeSelected(FollowUpMode.LAST) },
            label = { Text(text = stringResource(R.string.search_mode_last)) },
        )
        FilterChip(
            selected = mode == FollowUpMode.ALL,
            onClick = { onModeSelected(FollowUpMode.ALL) },
            label = { Text(text = stringResource(R.string.search_mode_all)) },
        )
        FilterChip(
            selected = mode == FollowUpMode.SPECIFIC,
            onClick = { onModeSelected(FollowUpMode.SPECIFIC) },
            label = { Text(text = stringResource(R.string.search_mode_specific)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                    )
                }
                onDismiss()
            }) {
                Text(text = stringResource(R.string.dialog_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFollowUpCreator(
    uiState: SearchUiState,
    onModeSelected: (FollowUpMode) -> Unit,
    onCreateFollowUp: () -> Unit,
    onCreatedAccepted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.search_mode_title),
            style = MaterialTheme.typography.titleMedium,
        )
        SearchFollowUpModes(
            mode = uiState.mode,
            onModeSelected = onModeSelected,
        )
        if (uiState.mode == FollowUpMode.SPECIFIC && uiState.selectedTrainIdentity == null) {
            Text(
                text = stringResource(R.string.search_specific_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onCreateFollowUp,
            enabled = !uiState.isCreatingFollowUp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(text = stringResource(R.string.search_create))
        }
        if (uiState.isCreatingFollowUp) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        uiState.followUpError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        uiState.createdFollowUpId?.let {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = stringResource(R.string.search_created)) },
                text = { Text(text = stringResource(R.string.search_created_hint)) },
                confirmButton = {
                    TextButton(onClick = onCreatedAccepted) {
                        Text(text = stringResource(R.string.dialog_accept))
                    }
                },
            )
        }
        if (uiState.availableTrainNotice) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = stringResource(R.string.search_train_available)) },
                text = { Text(text = stringResource(R.string.search_train_available_hint)) },
                confirmButton = {
                    TextButton(onClick = onCreatedAccepted) {
                        Text(text = stringResource(R.string.dialog_accept))
                    }
                },
            )
        }
    }
}

private fun formatTime(value: String?): String = value?.take(5) ?: "--:--"

@Composable
private fun availabilityLabel(value: String): String = when (value) {
    Availability.AVAILABLE -> stringResource(R.string.av_available)
    Availability.NO_AVAILABILITY -> stringResource(R.string.av_none)
    else -> stringResource(R.string.av_unknown)
}

private fun availabilityBadgeType(value: String): BadgeType = when (value) {
    Availability.AVAILABLE -> BadgeType.SUCCESS
    Availability.NO_AVAILABILITY -> BadgeType.ERROR
    else -> BadgeType.NEUTRAL
}