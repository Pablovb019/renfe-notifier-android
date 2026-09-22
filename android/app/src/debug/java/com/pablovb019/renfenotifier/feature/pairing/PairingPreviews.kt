package com.pablovb019.renfenotifier.feature.pairing

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

private const val PAIRING_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

private fun previewLoading() = PairingUiState(
    code = "RF-12AB34",
    isLoading = true,
    error = null,
    paired = false,
)

private fun previewError() = PairingUiState(
    code = "RF-12AB34",
    isLoading = false,
    error = "Código de emparejamiento no válido o caducado.",
    paired = false,
)

@Preview(
    name = "Emparejar claro",
    widthDp = 360,
    heightDp = 800,
    device = PAIRING_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun PairingPreviewLight() {
    PairingPreviewContent()
}

@Preview(
    name = "Emparejar oscuro",
    widthDp = 360,
    heightDp = 800,
    device = PAIRING_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun PairingPreviewDark() {
    PairingPreviewContent()
}

@Preview(
    name = "Emparejar claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = PAIRING_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun PairingPreviewLightLargeFont() {
    PairingPreviewLoading()
}

@Preview(
    name = "Emparejar oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = PAIRING_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun PairingPreviewDarkLargeFont() {
    PairingPreviewError()
}

@Composable
private fun PairingPreviewContent() {
    RenfeNotifierTheme {
        PairingContent(
            uiState = PairingUiState(),
            onCodeChange = {},
            onClaim = {},
            modifier = Modifier.padding(vertical = RenfeSpacing.screenMargin),
        )
    }
}

@Composable
private fun PairingPreviewLoading() {
    RenfeNotifierTheme {
        PairingContent(
            uiState = previewLoading(),
            onCodeChange = {},
            onClaim = {},
            modifier = Modifier.padding(vertical = RenfeSpacing.screenMargin),
        )
    }
}

@Composable
private fun PairingPreviewError() {
    RenfeNotifierTheme {
        PairingContent(
            uiState = previewError(),
            onCodeChange = {},
            onClaim = {},
            modifier = Modifier.padding(vertical = RenfeSpacing.screenMargin),
        )
    }
}