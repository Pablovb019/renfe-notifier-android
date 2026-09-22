package com.pablovb019.renfenotifier.feature.pairing

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.security.KeystoreTokenVault
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

/**
 * Pantalla de emparejamiento: el usuario introduce el código OTP emitido por el
 * administrador. Solo llama a `onPaired()` cuando el backend respondió OK.
 */
@Composable
fun PairingScreen(
    onPaired: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de emparejamiento")
        ApiModule.init(app)
        PairingViewModel(
            api = ApiModule.api(),
            vault = KeystoreTokenVault(app),
            sessionStore = PreferencesRepository(app),
            deviceIdProvider = {
                Settings.Secure.getString(
                    app.contentResolver,
                    Settings.Secure.ANDROID_ID,
                ) ?: "unknown"
            },
            deviceNameProvider = { "${Build.MANUFACTURER} ${Build.MODEL}".trim() },
        )
    },
) {
    val uiState by remember(viewModel) { viewModel.uiState }.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.paired) {
        if (uiState.paired) onPaired()
    }

    RenfeScreenScaffold(
        title = stringResource(R.string.pairing_title),
        onBack = null,
        modifier = modifier,
    ) { innerPadding ->
        PairingContent(
            uiState = uiState,
            onCodeChange = viewModel::onCodeChange,
            onClaim = viewModel::claim,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Contenido del formulario de emparejamiento: campo RF-XXXXXX con teclado de
 * texto (no numérico), error asociado al campo y botón deshabilitado mientras
 * no haya código o se esté validando. Columna scrollable, márgenes 16 dp e
 * `imePadding` para que el teclado no tape el campo.
 */
@Composable
fun PairingContent(
    uiState: PairingUiState,
    onCodeChange: (String) -> Unit,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val codeDescription = stringResource(R.string.cd_pairing_code)
    val errorDescription = stringResource(R.string.cd_pairing_error)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = RenfeSpacing.screenMargin, vertical = RenfeSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RenfeSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.pairing_hint),
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = uiState.code,
            onValueChange = onCodeChange,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = codeDescription },
            label = { Text(text = stringResource(R.string.pairing_code_label)) },
            placeholder = { Text(text = "RF-XXXXXX") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )

        uiState.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { contentDescription = errorDescription },
            )
        }

        Button(
            onClick = onClaim,
            enabled = !uiState.isLoading && uiState.code.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("pairing_claim_button"),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(text = stringResource(R.string.pairing_button))
            }
        }
    }
}