package com.pablovb019.renfenotifier.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Estado de carga a pantalla completa: indicador centrado.
 * El indicador en sí nunca usa fillMaxWidth (mantiene su tamaño nominal).
 */
@Composable
fun RenfeLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("renfe_loading_state"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}