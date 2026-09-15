package com.pablovb019.renfenotifier.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.core.notifications.NotificationChannels
import java.time.format.DateTimeFormatter
import com.pablovb019.renfenotifier.R

@Composable
fun HomeScreen(
    onNavigateToPairing: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(notificationsPermissionGranted(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = notificationsPermissionGranted(context)
        onPauseOrDispose { }
    }
    HomeContent(
        uiState = uiState,
        onRefresh = {},
        onNavigateToPairing = onNavigateToPairing,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToFollowUps = onNavigateToFollowUps,
        onNavigateToDiagnostics = onNavigateToDiagnostics,
        notificationsDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onNavigateToPairing: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    notificationsDenied: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val refreshLabel = stringResource(R.string.home_refresh)
    val refreshDescription = stringResource(R.string.cd_refresh)
    val context = LocalContext.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_title)) },
                actions = {
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.semantics {
                            contentDescription = refreshDescription
                        },
                    ) {
                        Text(text = refreshLabel)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (notificationsDenied) {
                Text(
                    text = stringResource(R.string.home_notifs_denied_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.home_notifs_denied_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { NotificationChannels.openSystemSettings(context) }) {
                    Text(text = stringResource(R.string.home_notifs_open_settings))
                }
            }
            if (!uiState.isPaired) {
                Text(
                    text = stringResource(R.string.home_not_paired_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onNavigateToPairing) {
                    Text(text = stringResource(R.string.home_not_paired_cta))
                }
            } else {
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.home_status_pending),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (uiState.loading) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                uiState.now?.let { now ->
                    Text(
                        text = stringResource(R.string.home_clock, now.format(HOUR_FORMAT)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.home_version, uiState.versionName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onNavigateToSearch,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.home_search_button))
            }
            Button(
                onClick = onNavigateToFollowUps,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.home_followups_button))
            }
            Button(
                onClick = onNavigateToDiagnostics,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.home_diagnostics_button))
            }
        }
    }
}

private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun notificationsPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}