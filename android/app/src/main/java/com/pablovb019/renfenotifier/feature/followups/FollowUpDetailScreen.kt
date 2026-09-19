package com.pablovb019.renfenotifier.feature.followups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut

/**
 * Detalle de un seguimiento: disponibilidad y última comprobación válida,
 * caducidad en Europe/Madrid, episodios y acciones. Pausa, confirmación y
 * eliminación son acciones separadas y distintas: confirmar no borra el
 * seguimiento y eliminar exige una confirmación explícita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDetailScreen(
    followupId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowUpDetailViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de seguimiento")
        ApiModule.init(app)
        FollowUpDetailViewModel(api = ApiModule.api(), followupId = followupId)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onDeleted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.followups_detail_title)) },
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
        FollowUpDetailContent(
            uiState = uiState,
            onRefresh = viewModel::load,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onRenew = viewModel::renew,
            onAcknowledge = viewModel::acknowledge,
            onDelete = viewModel::delete,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun FollowUpDetailContent(
    uiState: FollowUpDetailUiState,
    onRefresh: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRenew: () -> Unit,
    onAcknowledge: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val detail = uiState.detail

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.loading && detail == null) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (detail != null) {
            DetailInfoCard(detail = detail, lastValidCheck = uiState.lastValidObservedAt)

            Text(
                text = stringResource(R.string.followups_reminder_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.followups_episodes_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (detail.episodes.isEmpty()) {
                Text(
                    text = stringResource(R.string.followups_no_episodes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                detail.episodes.forEach { episode ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.followups_episodes_item,
                                    episode.episode,
                                    MadridFormat.showInstant(episode.observedAt),
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(
                                    R.string.followups_episodes_trains,
                                    episode.trainIds.size,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.followups_actions_title),
                style = MaterialTheme.typography.titleMedium,
            )
            ActionButtons(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onRenew = onRenew,
                onAcknowledge = onAcknowledge,
                onAskDelete = { showDeleteDialog = true },
            )

            if (detail.alertState == "pending_alert") {
                Text(
                    text = stringResource(R.string.followups_acknowledge_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.actionError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.followups_load_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.followups_retry),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onRefresh),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.followups_delete_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.followups_delete_dialog_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(R.string.followups_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailInfoCard(detail: FollowUpDetailOut, lastValidCheck: java.time.Instant?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val origin = detail.originName ?: detail.originCode
            val destination = detail.destinationName ?: detail.destinationCode
            Text(
                text = stringResource(R.string.followups_card_route, origin, destination),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.followups_card_date, MadridFormat.showTravelDate(detail.travelDate)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = detail.modeText(),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (detail.plazaH) {
                Text(
                    text = stringResource(R.string.followups_plaza_h),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.followups_availability, availabilityLabel(detail.availability)),
                    color = availabilityColor(detail.availability),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.followups_lifecycle_detail, lifecycleLabel(detail.lifecycle)),
                    color = lifecycleColor(detail.lifecycle),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = if (lastValidCheck != null) {
                    stringResource(R.string.followups_last_check, MadridFormat.showInstant(lastValidCheck))
                } else {
                    stringResource(R.string.followups_no_checks)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.followups_expires, MadridFormat.showInstant(detail.expiresAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionButtons(
    uiState: FollowUpDetailUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRenew: () -> Unit,
    onAcknowledge: () -> Unit,
    onAskDelete: () -> Unit,
) {
    val detail = uiState.detail ?: return
    val enabled = !uiState.actionInProgress

    if (uiState.actionInProgress) {
        CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    when (detail.lifecycle) {
        FollowUpLifecycle.ACTIVE -> Button(
            onClick = onPause,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.followups_pause))
        }
        FollowUpLifecycle.PAUSED -> Button(
            onClick = onResume,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.followups_resume))
        }
        else -> Unit
    }

    if (detail.lifecycle != FollowUpLifecycle.DELETED) {
        OutlinedButton(
            onClick = onRenew,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.followups_renew))
        }
    }

    if (detail.alertState == "pending_alert") {
        Button(
            onClick = onAcknowledge,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.followups_acknowledge))
        }
    }

    OutlinedButton(
        onClick = onAskDelete,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(text = stringResource(R.string.followups_delete))
    }
}

@Composable
private fun FollowUpDetailOut.modeText(): String = when (mode) {
    "first" -> stringResource(R.string.search_mode_first)
    "last" -> stringResource(R.string.search_mode_last)
    "all" -> stringResource(R.string.search_mode_all)
    "specific" -> {
        val times = specificTrainTimes()
        if (times == null) {
            stringResource(R.string.followups_mode_specific, "-")
        } else {
            stringResource(R.string.followups_mode_specific_times, times.first, times.second)
        }
    }
    else -> mode
}

/**
 * Extrae los horarios de ida/vuelta de la identity del tren específico
 * (formato `{servicio}|{salida}|{llegada}`) para mostrar "11:08 → 12:13".
 */
private fun FollowUpDetailOut.specificTrainTimes(): Pair<String, String>? {
    val parts = specificTrainId?.split("|").orEmpty()
    if (parts.size < 3) return null
    val departure = parts[parts.size - 2].take(5)
    val arrival = parts[parts.size - 1].take(5)
    if (departure.length < 5 || arrival.length < 5 || departure == "unknown" || arrival == "unknown") {
        return null
    }
    return departure to arrival
}

@Composable
private fun availabilityLabel(value: String): String = when (value) {
    "available" -> stringResource(R.string.av_available)
    "unavailable" -> stringResource(R.string.av_none)
    else -> stringResource(R.string.av_unknown)
}

@Composable
private fun lifecycleLabel(value: String): String = when (value) {
    FollowUpLifecycle.ACTIVE -> stringResource(R.string.followups_lifecycle_active)
    FollowUpLifecycle.PAUSED -> stringResource(R.string.followups_lifecycle_paused)
    FollowUpLifecycle.EXPIRED -> stringResource(R.string.followups_lifecycle_expired)
    FollowUpLifecycle.DELETED -> stringResource(R.string.followups_lifecycle_deleted)
    else -> value
}

@Composable
private fun lifecycleColor(value: String)
        : androidx.compose.ui.graphics.Color = when (value) {
    FollowUpLifecycle.ACTIVE -> androidx.compose.ui.graphics.Color(0xFF1B7F3A)
    FollowUpLifecycle.PAUSED -> MaterialTheme.colorScheme.tertiary
    FollowUpLifecycle.EXPIRED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun availabilityColor(value: String)
        : androidx.compose.ui.graphics.Color = when (value) {
    "available" -> androidx.compose.ui.graphics.Color(0xFF1B7F3A)
    "unavailable" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}