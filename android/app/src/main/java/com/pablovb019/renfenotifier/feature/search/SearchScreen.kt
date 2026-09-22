package com.pablovb019.renfenotifier.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pablovb019.renfenotifier.core.model.FollowUpMode
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.network.model.StationOut
import com.pablovb019.renfenotifier.ui.components.RenfeErrorState
import com.pablovb019.renfenotifier.ui.components.RenfeLoadingState
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pantalla de búsqueda: estaciones predictivas (alias/tildes/grupos), origen,
 * destino, fecha y Plaza H; resultados con precio desconocido representado
 * como tal; creación de seguimiento (tren concreto, primero, último o todos)
 * reutilizando la ruta y fecha vigentes.
 */
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
    RenfeScreenScaffold(
        title = stringResource(R.string.search_title),
        onBack = onBack,
        modifier = modifier,
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
            onCreatedAccepted = {
                viewModel.onCreatedAccepted()
                onBack()
            },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

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
    onCreatedAccepted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy") }
    var showDatePicker by remember { mutableStateOf(false) }
    val plazaHDescription = stringResource(R.string.cd_search_plaza_h)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = RenfeSpacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(RenfeSpacing.md),
    ) {
        item {
            SearchStationField(
                label = stringResource(R.string.search_origin),
                value = uiState.originQuery,
                suggestions = uiState.originSuggestions,
                onQueryChange = onOriginQueryChange,
                onSelected = onOriginSelected,
            )
        }
        item {
            SearchStationField(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(text = stringResource(R.string.search_button))
            }
        }

        if (uiState.isSearching) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                ) {
                    RenfeLoadingState()
                }
            }
        } else if (uiState.searchError != null) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                ) {
                    RenfeErrorState(
                        icon = Icons.Filled.Warning,
                        title = stringResource(R.string.search_error_title),
                        text = uiState.searchError.orEmpty(),
                        onRetry = onSearch,
                    )
                }
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
            items(uiState.trains, key = { it.identity }) { train ->
                SearchTrainCard(
                    train = train,
                    selected = uiState.mode == FollowUpMode.SPECIFIC &&
                        uiState.selectedTrainIdentity == train.identity,
                    onSelected = { onTrainSelected(train.identity) },
                )
            }
        }

        if (uiState.trains.isNotEmpty()) {
            item {
                SearchFollowUpCreator(
                    uiState = uiState,
                    onModeSelected = onModeSelected,
                    onCreateFollowUp = onCreateFollowUp,
                    onCreatedAccepted = onCreatedAccepted,
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