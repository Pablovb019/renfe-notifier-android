package com.pablovb019.renfenotifier.feature.followups

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.EpisodeOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

private const val FOLLOWUPS_DETAIL_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

private val previewDetail = FollowUpDetailOut(
    followupId = "fu-1",
    originCode = "ZAR",
    originName = "Zaragoza-Delicias",
    destinationCode = "MAD",
    destinationName = "Madrid Puerta de Atocha",
    travelDate = "2026-09-25",
    mode = "specific",
    plazaH = true,
    specificTrainId = "AVANT 8492|11:08|12:13",
    lifecycle = FollowUpLifecycle.ACTIVE,
    availability = "available",
    alertState = "pending_alert",
    episode = 2,
    expiresAt = "2026-09-30T12:00:00+02:00",
    episodes = listOf(
        EpisodeOut(
            episodeId = 1,
            episode = 1,
            observedAt = "2026-09-22T10:00:00+02:00",
            trainIds = listOf("AVANT 8492", "AVANT 8482"),
        ),
        EpisodeOut(
            episodeId = 2,
            episode = 2,
            observedAt = "2026-09-23T08:30:00+02:00",
            trainIds = listOf("AVANT 8492"),
        ),
    ),
)

private fun previewUiState() = FollowUpDetailUiState(
    loading = false,
    actionInProgress = false,
    actionError = null,
    detail = previewDetail,
    deleted = false,
)

@Preview(
    name = "Detalle claro",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_DETAIL_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun FollowUpDetailPreviewLight() {
    FollowUpDetailPreviewContent()
}

@Preview(
    name = "Detalle oscuro",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_DETAIL_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun FollowUpDetailPreviewDark() {
    FollowUpDetailPreviewContent()
}

@Preview(
    name = "Detalle claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_DETAIL_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun FollowUpDetailPreviewLightLargeFont() {
    FollowUpDetailPreviewContent()
}

@Preview(
    name = "Detalle oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = FOLLOWUPS_DETAIL_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun FollowUpDetailPreviewDarkLargeFont() {
    FollowUpDetailPreviewContent()
}

@Composable
private fun FollowUpDetailPreviewContent() {
    RenfeNotifierTheme {
        FollowUpDetailContent(
            uiState = previewUiState(),
            onRefresh = {},
            onPause = {},
            onResume = {},
            onRenew = {},
            onAcknowledge = {},
            onDelete = {},
            modifier = Modifier.padding(top = RenfeSpacing.screenMargin),
        )
    }
}