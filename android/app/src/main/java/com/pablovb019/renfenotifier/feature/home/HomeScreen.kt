package com.pablovb019.renfenotifier.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.notifications.NotificationChannels
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

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
        onNavigateToPairing = onNavigateToPairing,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToFollowUps = onNavigateToFollowUps,
        onNavigateToDiagnostics = onNavigateToDiagnostics,
        notificationsDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled,
    )
}

/**
 * Contenido de la pantalla de inicio: cabecera, estado de emparejamiento,
 * informacion de meta y CTAs principales. Sin llamadas de red.
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onNavigateToPairing: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    notificationsDenied: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    RenfeScreenScaffold(
        title = stringResource(R.string.home_title),
        onBack = null,
        actions = {
            TextButton(onClick = onNavigateToDiagnostics) {
                Text(text = stringResource(R.string.home_settings))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = RenfeSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(RenfeSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (notificationsDenied) {
                HomeNotificationNotice(
                    onOpenSettings = { NotificationChannels.openSystemSettings(context) },
                )
            }

            if (uiState.isPaired) {
                HomeHeader(title = stringResource(R.string.home_subtitle))
                HomePairedBadge()
            } else {
                HomeHeader(title = stringResource(R.string.home_not_paired_title))
                Button(
                    onClick = onNavigateToPairing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(text = stringResource(R.string.home_not_paired_cta))
                }
            }

            HomeMeta(now = uiState.now, versionName = uiState.versionName)

            Button(
                onClick = onNavigateToSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(text = stringResource(R.string.home_search_button))
            }

            OutlinedButton(
                onClick = onNavigateToFollowUps,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(text = stringResource(R.string.home_followups_button))
            }
        }
    }
}

private fun notificationsPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}