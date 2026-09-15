package com.pablovb019.renfenotifier.feature.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.diagnostics.AndroidDeviceEnvironment
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.feature.followups.MadridFormat

/** Panel de diagnóstico y ajustes (paso 25). Nunca muestra tokens ni credenciales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de diagnóstico")
        ApiModule.init(app)
        DiagnosticsViewModel(
            api = ApiModule.api(),
            environment = AndroidDeviceEnvironment(app),
            settings = PreferencesRepository(app),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshEnvironment()
        viewModel.load()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.diag_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.diag_back_cd),
                        )
                    }
                },
                actions = {
                    Button(onClick = {
                        viewModel.refreshEnvironment()
                        viewModel.load()
                    }) {
                        Text(text = stringResource(R.string.diag_refresh))
                    }
                },
            )
        },
    ) { innerPadding ->
        DiagnosticsContent(
            uiState = uiState,
            onSetTheme = viewModel::setTheme,
            onSetAlertsEnabled = viewModel::setAlertsEnabled,
            onSendTest = viewModel::sendTestNotification,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun DiagnosticsContent(
    uiState: DiagnosticsUiState,
    onSetTheme: (String) -> Unit,
    onSetAlertsEnabled: (Boolean) -> Unit,
    onSendTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.diag_stages_title),
            style = MaterialTheme.typography.titleMedium,
        )
        uiState.stages.forEach { stage -> StageCard(stage) }

        Text(
            text = stringResource(R.string.diag_server_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.loading && uiState.diagnostics == null) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            ServerCard(uiState)
        }

        Text(
            text = stringResource(R.string.diag_counts_title),
            style = MaterialTheme.typography.titleMedium,
        )
        CountsCard(uiState)

        Text(
            text = stringResource(R.string.diag_settings_title),
            style = MaterialTheme.typography.titleMedium,
        )
        SettingsCard(
            theme = uiState.theme,
            alertsEnabled = uiState.alertsEnabled,
            onSetTheme = onSetTheme,
            onSetAlertsEnabled = onSetAlertsEnabled,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.diag_test_button),
                    style = MaterialTheme.typography.titleSmall,
                )
                Button(
                    onClick = onSendTest,
                    enabled = uiState.testNotificationState != TestNotificationState.Sending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = when (val state = uiState.testNotificationState) {
                            TestNotificationState.Sending -> stringResource(R.string.diag_test_pending)
                            TestNotificationState.Idle -> stringResource(R.string.diag_test_button)
                            is TestNotificationState.Sent -> stringResource(
                                R.string.diag_test_sent,
                                state.messageId,
                            )
                            is TestNotificationState.Failed -> stringResource(
                                R.string.diag_test_failed,
                                state.reason,
                            )
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.diag_force_stop_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StageCard(stage: DiagnosticStage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = statusColor(stage.status),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(statusLabel(stage.status)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(stageLabel(stage.kind)),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stage.detail,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ServerCard(uiState: DiagnosticsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            uiState.lastServerContactAt?.let { last ->
                Text(
                    text = stringResource(
                        R.string.diag_last_check,
                        MadridFormat.showInstant(last),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } ?: Text(
                text = stringResource(R.string.diag_no_last_check),
                style = MaterialTheme.typography.bodyMedium,
            )
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(
                    if (uiState.googlePlayServicesAvailable) {
                        R.string.diag_google_play_ok
                    } else {
                        R.string.diag_google_play_missing
                    },
                ),
                color = if (uiState.googlePlayServicesAvailable) {
                    Color(0xFF1B7F3A)
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (uiState.postNotificationsGranted) {
                        R.string.diag_permission_granted
                    } else {
                        R.string.diag_permission_missing
                    },
                ),
                color = if (uiState.postNotificationsGranted) {
                    Color(0xFF1B7F3A)
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (uiState.fcmChannelEnabled) {
                        R.string.diag_channel_enabled
                    } else {
                        R.string.diag_channel_disabled
                    },
                ),
                color = if (uiState.fcmChannelEnabled) {
                    Color(0xFF1B7F3A)
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CountsCard(uiState: DiagnosticsUiState) {
    val stats = uiState.diagnostics?.searchStats
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.diag_count_devices, uiState.diagnostics?.devices ?: 0),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.diag_count_followups,
                    uiState.diagnostics?.followups?.active ?: 0,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.diag_count_episodes,
                    uiState.diagnostics?.episodes ?: 0,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.diag_count_logical,
                    stats?.logicalQueries?.toString() ?: "0",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.diag_count_http,
                    stats?.httpRequests?.toString() ?: "0",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.diag_count_bytes,
                    formatBytes(stats?.bytesReceived ?: 0L),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    theme: String,
    alertsEnabled: Boolean,
    onSetTheme: (String) -> Unit,
    onSetAlertsEnabled: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.diag_theme_label),
                style = MaterialTheme.typography.titleSmall,
            )
            val options = listOf(
                PreferencesRepository.THEME_SYSTEM to R.string.diag_theme_system,
                PreferencesRepository.THEME_LIGHT to R.string.diag_theme_light,
                PreferencesRepository.THEME_DARK to R.string.diag_theme_dark,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, labelRes) ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { onSetTheme(value) },
                        label = { Text(text = stringResource(labelRes)) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.diag_alerts_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onSetAlertsEnabled,
                )
            }
        }
    }
}

@Composable
private fun stageLabel(kind: StageKind): Int = when (kind) {
    StageKind.QUERY -> R.string.diag_stage_query
    StageKind.DETECTION -> R.string.diag_stage_detection
    StageKind.SEND -> R.string.diag_stage_send
    StageKind.RECEIVE -> R.string.diag_stage_receive
    StageKind.NOTIFY -> R.string.diag_stage_notify
}

@Composable
private fun statusLabel(status: StageStatus): Int = when (status) {
    StageStatus.OK -> R.string.diag_status_ok
    StageStatus.ATTENTION -> R.string.diag_status_attention
    StageStatus.BLOCKED -> R.string.diag_status_blocked
    StageStatus.UNKNOWN -> R.string.diag_status_unknown
}

private fun statusColor(status: StageStatus): Color = when (status) {
    StageStatus.OK -> Color(0xFF1B7F3A)
    StageStatus.ATTENTION -> Color(0xFFB87333)
    StageStatus.BLOCKED -> Color(0xFFB00020)
    StageStatus.UNKNOWN -> Color(0xFF75777F)
}

private fun formatBytes(value: Long): String = when {
    value >= 1_048_576 -> "%.1f MB".format(value / 1_048_576.0)
    value >= 1_024 -> "%.1f KB".format(value / 1_024.0)
    else -> "$value B"
}