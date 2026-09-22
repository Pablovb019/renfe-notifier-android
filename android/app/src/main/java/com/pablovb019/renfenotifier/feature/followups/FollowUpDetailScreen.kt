package com.pablovb019.renfenotifier.feature.followups

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import com.pablovb019.renfenotifier.ui.components.RenfeErrorState
import com.pablovb019.renfenotifier.ui.components.RenfeLoadingState
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeMotion
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

/**
 * Detalle de un seguimiento: disponibilidad y última comprobación válida,
 * caducidad en Europe/Madrid, episodios y acciones. Pausa, confirmación y
 * eliminación son acciones separadas y distintas: confirmar no borra el
 * seguimiento y eliminar exige una confirmación explícita.
 */
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

    RenfeScreenScaffold(
        title = stringResource(R.string.followups_detail_title),
        onBack = onBack,
        modifier = modifier,
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
fun FollowUpDetailContent(
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
            .padding(horizontal = RenfeSpacing.screenMargin, vertical = RenfeSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RenfeSpacing.md),
    ) {
        Crossfade(
            targetState = uiState.loading && detail == null,
            animationSpec = tween(RenfeMotion.Normal),
        ) { isLoading ->
            if (isLoading) {
                RenfeLoadingState(modifier = Modifier.padding(vertical = 40.dp))
            } else if (detail != null) {
                FollowUpDetailHeader(
                    detail = detail,
                    lastValidCheck = uiState.lastValidObservedAt,
                )

                Text(
                    text = stringResource(R.string.followups_reminder_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(R.string.followups_episodes_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FollowUpDetailEpisodes(detail = detail)

                Text(
                    text = stringResource(R.string.followups_actions_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FollowUpDetailActionButtons(
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
                RenfeErrorState(
                    icon = Icons.Filled.Warning,
                    title = stringResource(R.string.followups_load_error),
                    text = uiState.actionError ?: stringResource(R.string.followups_no_checks),
                    onRetry = onRefresh,
                )
            }
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