package com.pablovb019.renfenotifier.feature.followups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.ui.components.RenfeErrorState
import com.pablovb019.renfenotifier.ui.components.RenfeLoadingState

/**
 * Listado de seguimientos con filtro por ciclo de vida. Tocar un elemento abre
 * su detalle; al volver, el listado se recarga (fuente de verdad = backend).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpsScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowUpsViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de seguimientos")
        ApiModule.init(app)
        FollowUpsViewModel(api = ApiModule.api())
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.followups_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.followups_back_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        FollowUpsContent(
            uiState = uiState,
            onFilterSelected = viewModel::onFilterSelected,
            onRefresh = viewModel::load,
            onOpenDetail = onOpenDetail,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun FollowUpsContent(
    uiState: FollowUpsUiState,
    onFilterSelected: (LifecycleFilter) -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LifecycleFilter.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = filterLabel(filter)) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.loading) {
                RenfeLoadingState(modifier = Modifier.padding(vertical = 32.dp))
            } else {
                uiState.error?.let { error ->
                    RenfeErrorState(
                        icon = Icons.Filled.Warning,
                        title = stringResource(R.string.followups_load_error),
                        text = error,
                        onRetry = onRefresh,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }

                if (uiState.isEmpty) {
                    Text(
                        text = stringResource(R.string.followups_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.items, key = { it.followupId }) { followUp ->
                        FollowUpCard(
                            followUp = followUp,
                            onClick = { onOpenDetail(followUp.followupId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpCard(followUp: FollowUpOut, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .semantics { contentDescription = "Seguimiento ${followUp.followupId}" }
        .clickable(onClick = onClick)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = routeLabel(followUp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = lifecycleLabel(followUp.lifecycle),
                    color = lifecycleColor(followUp.lifecycle),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(R.string.followups_card_date, MadridFormat.showTravelDate(followUp.travelDate)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = availabilityLabel(followUp.availability),
                    color = availabilityColor(followUp.availability),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = alertLabel(followUp.alertState),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun routeLabel(followUp: FollowUpOut): String {
    val origin = followUp.originName ?: followUp.originCode
    val destination = followUp.destinationName ?: followUp.destinationCode
    return stringResource(R.string.followups_card_route, origin, destination)
}

@Composable
private fun filterLabel(filter: LifecycleFilter): String = when (filter) {
    LifecycleFilter.ALL -> stringResource(R.string.followups_filter_all)
    LifecycleFilter.ACTIVE -> stringResource(R.string.followups_filter_active)
    LifecycleFilter.PAUSED -> stringResource(R.string.followups_filter_paused)
    LifecycleFilter.EXPIRED -> stringResource(R.string.followups_filter_expired)
    LifecycleFilter.DELETED -> stringResource(R.string.followups_filter_deleted)
}

@Composable
private fun lifecycleLabel(value: String): String = when (value) {
    "active" -> stringResource(R.string.followups_lifecycle_active)
    "paused" -> stringResource(R.string.followups_lifecycle_paused)
    "expired" -> stringResource(R.string.followups_lifecycle_expired)
    "deleted" -> stringResource(R.string.followups_lifecycle_deleted)
    else -> value
}

@Composable
private fun availabilityLabel(value: String): String = when (value) {
    "available" -> stringResource(R.string.av_available)
    "unavailable" -> stringResource(R.string.av_none)
    else -> stringResource(R.string.av_unknown)
}

@Composable
private fun alertLabel(value: String): String = when (value) {
    "pending_alert" -> stringResource(R.string.followups_alert_pending)
    "acknowledged" -> stringResource(R.string.followups_alert_acknowledged)
    else -> stringResource(R.string.followups_alert_idle)
}

@Composable
private fun lifecycleColor(value: String)
        : androidx.compose.ui.graphics.Color = when (value) {
    "active" -> MaterialTheme.colorScheme.onPrimaryContainer
    "paused" -> MaterialTheme.colorScheme.tertiary
    "expired" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun availabilityColor(value: String)
        : androidx.compose.ui.graphics.Color = when (value) {
    "available" -> MaterialTheme.colorScheme.onPrimaryContainer
    "unavailable" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}