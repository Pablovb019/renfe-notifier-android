package com.pablovb019.renfenotifier.feature.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import java.time.LocalDateTime

private const val HOME_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

@Preview(
    name = "Home claro",
    widthDp = 360,
    heightDp = 800,
    device = HOME_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun HomePreviewLight() {
    HomePreviewContent(isPaired = true)
}

@Preview(
    name = "Home oscuro",
    widthDp = 360,
    heightDp = 800,
    device = HOME_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun HomePreviewDark() {
    HomePreviewContent(isPaired = true)
}

@Preview(
    name = "Home claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = HOME_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun HomePreviewLightLargeFont() {
    HomePreviewContent(isPaired = false)
}

@Preview(
    name = "Home oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = HOME_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun HomePreviewDarkLargeFont() {
    HomePreviewContent(isPaired = false)
}

@Composable
private fun HomePreviewContent(isPaired: Boolean) {
    RenfeNotifierTheme {
        HomeContent(
            uiState = HomeUiState(
                versionName = "0.1.10",
                now = LocalDateTime.of(2026, 9, 22, 12, 34, 56),
                loading = false,
                isPaired = isPaired,
            ),
            notificationsDenied = !isPaired,
        )
    }
}