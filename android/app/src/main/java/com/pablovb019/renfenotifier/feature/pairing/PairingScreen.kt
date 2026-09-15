package com.pablovb019.renfenotifier.feature.pairing

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.security.KeystoreTokenVault
import com.pablovb019.renfenotifier.core.security.PreferencesRepository

/**
 * Pantalla de emparejamiento: el usuario introduce el código OTP emitido por el
 * administrador. Solo llama a `onPaired()` cuando el backend respondió OK.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.pairing_title)) })
        },
    ) { innerPadding ->
        PairingContent(
            uiState = uiState,
            onCodeChange = viewModel::onCodeChange,
            onClaim = viewModel::claim,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun PairingContent(
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
        )

        Button(
            onClick = onClaim,
            enabled = !uiState.isLoading && uiState.code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(text = stringResource(R.string.pairing_button))
            }
        }

        uiState.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { contentDescription = errorDescription },
            )
        }
    }
}