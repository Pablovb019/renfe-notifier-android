package com.pablovb019.renfenotifier.feature.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.feature.followups.MadridFormat
import com.pablovb019.renfenotifier.ui.components.BadgeType
import com.pablovb019.renfenotifier.ui.components.RenfeStatusBadge
import com.pablovb019.renfenotifier.ui.components.ThemeModeSelector
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing
import com.pablovb019.renfenotifier.ui.theme.ThemeMode

/** Tarjeta de apariencia: reutiliza el [ThemeModeSelector] de la fase 3. */
@Composable
fun DiagnosticsThemeCard(
    themeMode: ThemeMode,
    isLoading: Boolean,
    saveError: String?,
    onSelectTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.diag_theme_label),
                style = MaterialTheme.typography.titleSmall,
            )
            ThemeModeSelector(
                selected = themeMode,
                onSelect = onSelectTheme,
                isLoading = isLoading,
                saveError = saveError,
            )
        }
    }
}

/** Tarjeta de avisos: interruptor maestro de las notificaciones de la app. */
@Composable
fun DiagnosticsAlertsCard(
    alertsEnabled: Boolean,
    onSetAlertsEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.lg),
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
                modifier = Modifier.testTag("diagnostics_alerts_switch"),
            )
        }
    }
}

/**
 * Sección "Diagnóstico técnico": las 5 etapas del flujo de avisos, el estado
 * del servidor, las cifras y la notificación de prueba. Plegable con
 * `rememberSaveable` para que no ocupe toda la pantalla de Ajustes: la
 * información sigue siendo accesible al expandir.
 */
@Composable
fun DiagnosticsTechSection(
    uiState: DiagnosticsUiState,
    onSendTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = RenfeSpacing.sm, vertical = RenfeSpacing.xs)
                    .testTag("diagnostics_section_tech"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.diag_section_tech),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.diag_section_tech_toggle_cd),
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(RenfeSpacing.md),
                ) {
                    uiState.stages.forEach { stage -> StageCard(stage) }

                    DiagnosticsServerCard(uiState)

                    DiagnosticsCountsCard(uiState)

                    DiagnosticsTestCard(
                        testState = uiState.testNotificationState,
                        onSendTest = onSendTest,
                    )
                }
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
                .padding(RenfeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RenfeStatusBadge(
                label = stringResource(statusLabel(stage.status)),
                icon = null,
                type = statusType(stage.status),
            )
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
private fun DiagnosticsServerCard(uiState: DiagnosticsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (uiState.loading && uiState.diagnostics == null) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
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
            }
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
                    MaterialTheme.colorScheme.onPrimaryContainer
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
                    MaterialTheme.colorScheme.onPrimaryContainer
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
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DiagnosticsCountsCard(uiState: DiagnosticsUiState) {
    val stats = uiState.diagnostics?.searchStats
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.lg),
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

/**
 * Botón de prueba con título corto y resultado en texto adjunto. Durante el
 * envío se deshabilita. Nunca muestra tokens, OTP ni credenciales.
 */
@Composable
private fun DiagnosticsTestCard(
    testState: TestNotificationState,
    onSendTest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
        ) {
            Button(
                onClick = onSendTest,
                enabled = testState != TestNotificationState.Sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.diag_test_button))
            }
            when (testState) {
                TestNotificationState.Idle -> Unit
                TestNotificationState.Sending -> Text(
                    text = stringResource(R.string.diag_test_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is TestNotificationState.Sent -> Text(
                    text = stringResource(R.string.diag_test_sent, testState.messageId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is TestNotificationState.Failed -> Text(
                    text = stringResource(R.string.diag_test_failed, testState.reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
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

private fun statusType(status: StageStatus): BadgeType = when (status) {
    StageStatus.OK -> BadgeType.SUCCESS
    StageStatus.ATTENTION -> BadgeType.WARNING
    StageStatus.BLOCKED -> BadgeType.ERROR
    StageStatus.UNKNOWN -> BadgeType.NEUTRAL
}

private fun formatBytes(value: Long): String = when {
    value >= 1_048_576 -> "%.1f MB".format(value / 1_048_576.0)
    value >= 1_024 -> "%.1f KB".format(value / 1_024.0)
    else -> "$value B"
}