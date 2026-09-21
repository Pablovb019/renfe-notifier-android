package com.pablovb019.renfenotifier.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pablovb019.renfenotifier.R

private const val PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

@Preview(
    name = "Tema claro",
    widthDp = 360,
    heightDp = 800,
    device = PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun RenfeThemePreviewLight() {
    RenfeThemePreviewContent()
}

@Preview(
    name = "Tema oscuro",
    widthDp = 360,
    heightDp = 800,
    device = PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun RenfeThemePreviewDark() {
    RenfeThemePreviewContent()
}

@Preview(
    name = "Claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun RenfeThemePreviewLightLargeFont() {
    RenfeThemePreviewContent()
}

@Preview(
    name = "Oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun RenfeThemePreviewDarkLargeFont() {
    RenfeThemePreviewContent()
}

/** Muestra de los componentes base (fase 4) sobre el tema de la fase 2. */
@Composable
private fun RenfeThemePreviewContent() {
    RenfeNotifierTheme {
        var text by remember { mutableStateOf("Texto de ejemplo") }
        var switchChecked by remember { mutableStateOf(true) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.diag_refresh))
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tarjeta",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Cuerpo de la tarjeta con tipografía del tema.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Campo de texto") },
            )
            RadioButton(selected = true, onClick = null)
            Switch(
                checked = switchChecked,
                onCheckedChange = { switchChecked = it },
            )
        }
    }
}