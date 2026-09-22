package com.pablovb019.renfenotifier.feature.followups

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme

private const val FOLLOWUPS_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

private val previewFollowUps = listOf(
    FollowUpOut(
        followupId = "fu-1",
        originCode = "ZAR",
        originName = "Zaragoza-Delicias",
        destinationCode = "MAD",
        destinationName = "Madrid Puerta de Atocha",
        travelDate = "2026-09-25",
        mode = "specific",
        plazaH = false,
        specificTrainId = "AVANT 8492",
        lifecycle = FollowUpLifecycle.ACTIVE,
        availability = "available",
        alertState = "pending_alert",
        episode = 1,
        expiresAt = "2026-09-30T12:00:00+02:00",
    ),
    FollowUpOut(
        followupId = "fu-2",
        originCode = "VAL",
        originName = "Estación de València Joaquín Sorolla",
        destinationCode = "BCN",
        destinationName = "Barcelona-Sants",
        travelDate = "2026-10-02",
        mode = "all",
        plazaH = true,
        specificTrainId = null,
        lifecycle = FollowUpLifecycle.PAUSED,
        availability = "unavailable",
        alertState = "acknowledged",
        episode = 2,
        expiresAt = "2026-10-10T08:00:00+02:00",
    ),
    FollowUpOut(
        followupId = "fu-3",
        originCode = "SEV",
        originName = "Sevilla-Santa Justa",
        destinationCode = "MAD",
        destinationName = null,
        travelDate = "2026-08-15",
        mode = "first",
        plazaH = false,
        specificTrainId = null,
        lifecycle = FollowUpLifecycle.EXPIRED,
        availability = "available",
        alertState = "idle",
        episode = 3,
        expiresAt = "2026-08-20T21:00:00+02:00",
    ),
    FollowUpOut(
        followupId = "fu-4",
        originCode = "COR",
        originName = "Córdoba Central",
        destinationCode = "GRA",
        destinationName = "Granada",
        travelDate = "2026-07-10",
        mode = "last",
        plazaH = false,
        specificTrainId = null,
        lifecycle = FollowUpLifecycle.DELETED,
        availability = "unavailable",
        alertState = "idle",
        episode = 0,
        expiresAt = "2026-07-15T10:00:00+02:00",
    ),
)

private fun previewUiState() = FollowUpsUiState(
    loading = false,
    filter = LifecycleFilter.ALL,
    items = previewFollowUps,
    total = previewFollowUps.size,
)

@Preview(
    name = "Seguimientos claro",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun FollowUpsPreviewLight() {
    FollowUpsPreviewContent()
}

@Preview(
    name = "Seguimientos oscuro",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun FollowUpsPreviewDark() {
    FollowUpsPreviewContent()
}

@Preview(
    name = "Seguimientos claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun FollowUpsPreviewLightLargeFont() {
    FollowUpsPreviewContent()
}

@Preview(
    name = "Seguimientos oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun FollowUpsPreviewDarkLargeFont() {
    FollowUpsPreviewContent()
}

@Composable
private fun FollowUpsPreviewContent() {
    RenfeNotifierTheme {
        FollowUpsContent(
            uiState = previewUiState(),
            onFilterSelected = {},
            onRefresh = {},
            onOpenDetail = {},
            modifier = Modifier.padding(vertical = RenfeSpacing.screenMargin),
        )
    }
}