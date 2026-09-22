package com.pablovb019.renfenotifier.feature.diagnostics

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pablovb019.renfenotifier.core.network.model.DiagnosticsOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpCountsOut
import com.pablovb019.renfenotifier.core.network.model.SearchStatsOut
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.ThemeMode
import java.time.Instant

private const val DIAGNOSTICS_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

private fun previewDiagnostics() = DiagnosticsOut(
    status = "ok",
    appVersion = "2.4.1",
    dbOk = true,
    devices = 1,
    followups = FollowUpCountsOut(active = 2, paused = 0, expired = 0, deleted = 0, total = 2),
    episodes = 3,
    alertsPending = 1,
    searchStats = SearchStatsOut(logicalQueries = 42, httpRequests = 55, bytesReceived = 4096),
)

private fun previewUiState(
    testState: TestNotificationState = TestNotificationState.Idle,
) = DiagnosticsUiState(
    loading = false,
    diagnostics = previewDiagnostics(),
    error = null,
    lastServerContactAt = Instant.now(),
    googlePlayServicesAvailable = true,
    postNotificationsGranted = true,
    fcmChannelEnabled = true,
    fcmConfigured = true,
    alertsEnabled = true,
    testNotificationState = testState,
    stages = computeStages(
        StageFacts(
            serverReachable = true,
            logicalQueries = 42L,
            testState = testState,
            fcmConfigured = true,
            postNotificationsGranted = true,
            fcmChannelEnabled = true,
            alertsEnabled = true,
        ),
    ),
)

@Preview(
    name = "Ajustes claro",
    widthDp = 360,
    heightDp = 800,
    device = DIAGNOSTICS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun DiagnosticsPreviewLight() {
    DiagnosticsPreviewContent()
}

@Preview(
    name = "Ajustes oscuro",
    widthDp = 360,
    heightDp = 800,
    device = DIAGNOSTICS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun DiagnosticsPreviewDark() {
    DiagnosticsPreviewContent()
}

@Preview(
    name = "Ajustes claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = DIAGNOSTICS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun DiagnosticsPreviewLightLargeFont() {
    DiagnosticsPreviewContent()
}

@Preview(
    name = "Ajustes oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = DIAGNOSTICS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun DiagnosticsPreviewDarkLargeFont() {
    DiagnosticsPreviewContent()
}

@Composable
private fun DiagnosticsPreviewContent() {
    RenfeNotifierTheme {
        DiagnosticsContent(
            uiState = previewUiState(),
            themeMode = ThemeMode.LIGHT,
            themeLoading = false,
            themeSaveError = null,
            onSelectTheme = {},
            onSetAlertsEnabled = {},
            onSendTest = {},
            modifier = Modifier.padding(vertical = com.pablovb019.renfenotifier.ui.theme.RenfeSpacing.screenMargin),
        )
    }
}