package com.pablovb019.renfenotifier.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.Availability
import com.pablovb019.renfenotifier.core.model.FollowUpMode
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.network.model.StationOut
import com.pablovb019.renfenotifier.core.network.model.TrainOut
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pantalla de búsqueda: estaciones predictivas (alias/tildes/grupos), origen,
 * destino, fecha y Plaza H; resultados con precio desconocido representado como
 * tal; creación de seguimiento (tren concreto, primero, último o todos)
 * reutilizando la ruta y fecha vigentes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de búsqueda")
        ApiModule.init(app)
        SearchViewModel(api = ApiModule.api())
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_search_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SearchContent(
            uiState = uiState,
            onOriginQueryChange = viewModel::onOriginQueryChange,
            onOriginSelected = viewModel::onOriginSelected,
            onDestinationQueryChange = viewModel::onDestinationQueryChange,
            onDestinationSelected = viewModel::onDestinationSelected,
            onDateSelected = viewModel::onDateSelected,
            onPlazaHChange = viewModel::onPlazaHChange,
            onSearch = viewModel::search,
            onModeSelected = viewModel::onModeSelected,
            onTrainSelected = viewModel::onTrainSelected,
            onCreateFollowUp = viewModel::createFollowUp,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    uiState: SearchUiState,
    onOriginQueryChange: (String) -> Unit,
    onOriginSelected: (StationOut) -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onDestinationSelected: (StationOut) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPlazaHChange: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onModeSelected: (FollowUpMode) -> Unit,
    onTrainSelected: (String) -> Unit,
    onCreateFollowUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy") }
    var showDatePicker by remember { mutableStateOf(false) }
    val plazaHDescription = stringResource(R.string.cd_search_plaza_h)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StationField(
                label = stringResource(R.string.search_origin),
                value = uiState.originQuery,
                suggestions = uiState.originSuggestions,
                onQueryChange = onOriginQueryChange,
                onSelected = onOriginSelected,
            )
        }
        item {
            StationField(
                label = stringResource(R.string.search_destination),
                value = uiState.destinationQuery,
                suggestions = uiState.destinationSuggestions,
                onQueryChange = onDestinationQueryChange,
                onSelected = onDestinationSelected,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.search_date_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = uiState.travelDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(text = stringResource(R.string.search_date_button))
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_plaza_h),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = uiState.plazaH,
                    onCheckedChange = onPlazaHChange,
                    modifier = Modifier.semantics { contentDescription = plazaHDescription },
                )
            }
        }
        item {
            Button(
                onClick = onSearch,
                enabled = !uiState.isSearching,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.search_button))
            }
        }

        uiState.searchError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (uiState.isSearching) {
            item {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else if (uiState.isEmptyResult) {
            item {
                Text(
                    text = stringResource(R.string.search_no_trains),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (uiState.trains.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_results_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            itemsIndexed(uiState.trains, key = { index, train -> "${train.identity}#$index" }) { index, train ->
                TrainCard(
                    train = train,
                    selected = uiState.mode == FollowUpMode.SPECIFIC &&
                        uiState.selectedTrainIdentity == train.identity,
                    onSelected = { onTrainSelected(train.identity) },
                )
            }
        }

        if (uiState.trains.isNotEmpty()) {
            item {
                FollowUpCreator(
                    uiState = uiState,
                    onModeSelected = onModeSelected,
                    onCreateFollowUp = onCreateFollowUp,
                )
            }
        }
    }

    if (showDatePicker) {
        SearchDatePickerDialog(
            initialDate = uiState.travelDate,
            onDateSelected = onDateSelected,
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDatePickerDialog(
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
private fun StationField(
    label: String,
    value: String,
    suggestions: List<StationOut>,
    onQueryChange: (String) -> Unit,
    onSelected: (StationOut) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onQueryChange(it)
                    expanded = it.isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text(text = label) },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                if (suggestions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.search_no_matches)) },
                        onClick = { expanded = false },
                        enabled = false,
                    )
                } else {
                    suggestions.forEach { station ->
                        DropdownMenuItem(
                            text = {
                                if (station.isGroup) {
                                    Text(text = stringResource(R.string.search_station_group, station.name))
                                } else {
                                    Text(text = station.name)
                                }
                            },
                            onClick = {
                                onSelected(station)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainCard(
    train: TrainOut,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    val trainDescription = stringResource(R.string.search_train_desc, train.identity)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = trainDescription }
            .clickable(onClick = onSelected),
        colors = if (selected) {
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            androidx.compose.material3.CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.search_train_time,
                        formatTime(train.departure),
                        formatTime(train.arrival),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when (train.availability) {
                        Availability.AVAILABLE -> stringResource(R.string.av_available)
                        Availability.NO_AVAILABILITY -> stringResource(R.string.av_none)
                        else -> stringResource(R.string.av_unknown)
                    },
                    color = availabilityColor(train.availability),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(
                    R.string.search_price,
                    train.price ?: stringResource(R.string.search_price_unknown),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.search_train_selected),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowUpCreator(
    uiState: SearchUiState,
    onModeSelected: (FollowUpMode) -> Unit,
    onCreateFollowUp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.search_mode_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.mode == FollowUpMode.FIRST,
                onClick = { onModeSelected(FollowUpMode.FIRST) },
                label = { Text(text = stringResource(R.string.search_mode_first)) },
            )
            FilterChip(
                selected = uiState.mode == FollowUpMode.LAST,
                onClick = { onModeSelected(FollowUpMode.LAST) },
                label = { Text(text = stringResource(R.string.search_mode_last)) },
            )
            FilterChip(
                selected = uiState.mode == FollowUpMode.ALL,
                onClick = { onModeSelected(FollowUpMode.ALL) },
                label = { Text(text = stringResource(R.string.search_mode_all)) },
            )
        }
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
            modifier = Modifier.fillMaxWidth(),
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
        uiState.createdFollowUpId?.let { id ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = stringResource(R.string.search_created)) },
                text = { Text(text = stringResource(R.string.search_created_hint, id)) },
                confirmButton = {
                    TextButton(onClick = { /* se mantiene la ruta y fecha para reutilizarlas */ }) {
                        Text(text = stringResource(R.string.dialog_accept))
                    }
                },
            )
        }
    }
}

private fun formatTime(value: String?): String = value?.take(5) ?: "--:--"

@Composable
private fun availabilityColor(value: String) = when (value) {
    Availability.AVAILABLE -> androidx.compose.ui.graphics.Color(0xFF1B7F3A)
    Availability.NO_AVAILABILITY -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}