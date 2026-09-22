package com.pablovb019.renfenotifier.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val COMPONENTS_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

@Preview(
    name = "Componentes claro",
    widthDp = 360,
    heightDp = 800,
    device = COMPONENTS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun RenfeComponentsPreviewLight() {
    RenfeComponentsPreviewContent()
}

@Preview(
    name = "Componentes oscuro",
    widthDp = 360,
    heightDp = 800,
    device = COMPONENTS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun RenfeComponentsPreviewDark() {
    RenfeComponentsPreviewContent()
}

@Preview(
    name = "Componentes claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = COMPONENTS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun RenfeComponentsPreviewLightLargeFont() {
    RenfeComponentsPreviewContent()
}

@Preview(
    name = "Componentes oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = COMPONENTS_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun RenfeComponentsPreviewDarkLargeFont() {
    RenfeComponentsPreviewContent()
}

@Composable
private fun RenfeComponentsPreviewContent() {
    RenfeScreenScaffold(title = "Componentes", onBack = {}) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RenfeStatusBadge(label = "Activo", icon = Icons.Filled.CheckCircle, type = BadgeType.SUCCESS)
            RenfeStatusBadge(label = "Revisar", icon = Icons.Filled.Info, type = BadgeType.WARNING)
            RenfeStatusBadge(label = "Bloqueado", icon = Icons.Filled.Warning, type = BadgeType.ERROR)
            RenfeStatusBadge(label = "Desconocido", icon = null, type = BadgeType.NEUTRAL)
            RenfeEmptyState(
                icon = Icons.Filled.Info,
                title = "Sin datos",
                text = "No hay contenido que mostrar todavía.",
                modifier = Modifier.weight(1f),
            )
            RenfeErrorState(
                icon = Icons.Filled.Warning,
                title = "No se pudo cargar",
                text = "Revisa la conexión e inténtalo de nuevo.",
                onRetry = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}