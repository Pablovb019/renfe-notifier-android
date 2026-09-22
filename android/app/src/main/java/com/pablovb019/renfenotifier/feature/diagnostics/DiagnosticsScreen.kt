package com.pablovb019.renfenotifier.feature.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.diagnostics.AndroidDeviceEnvironment
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing
import com.pablovb019.renfenotifier.ui.theme.ThemeMode
import com.pablovb019.renfenotifier.ui.theme.ThemeViewModel

/** Ajustes: apariencia, avisos y diagnóstico técnico (ruta `diagnostics`). */
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de diagnóstico")
        ApiModule.init(app)
        DiagnosticsViewModel(
            api = ApiModule.api(),
            environment = AndroidDeviceEnvironment(app),
            settings = PreferencesRepository(app),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
    val themeLoading by themeViewModel.isLoading.collectAsStateWithLifecycle()
    val themeSaveError by themeViewModel.saveError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshEnvironment()
        viewModel.load()
    }

    RenfeScreenScaffold(
        title = stringResource(R.string.diag_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            Button(onClick = {
                viewModel.refreshEnvironment()
                viewModel.load()
            }) {
                Text(text = stringResource(R.string.diag_refresh))
            }
        },
    ) { innerPadding ->
        DiagnosticsContent(
            uiState = uiState,
            themeMode = themeMode,
            themeLoading = themeLoading,
            themeSaveError = themeSaveError,
            onSelectTheme = themeViewModel::setMode,
            onSetAlertsEnabled = viewModel::setAlertsEnabled,
            onSendTest = viewModel::sendTestNotification,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Contenido de Ajustes: sección Apariencia (tema), sección Avisos (switch) y
 * sección "Diagnóstico técnico" plegable. Márgenes 16 dp.
 */
@Composable
fun DiagnosticsContent(
    uiState: DiagnosticsUiState,
    themeMode: ThemeMode,
    themeLoading: Boolean,
    themeSaveError: String?,
    onSelectTheme: (ThemeMode) -> Unit,
    onSetAlertsEnabled: (Boolean) -> Unit,
    onSendTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RenfeSpacing.screenMargin, vertical = RenfeSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RenfeSpacing.md),
    ) {
        SectionTitle(title = stringResource(R.string.diag_section_appearance))
        DiagnosticsThemeCard(
            themeMode = themeMode,
            isLoading = themeLoading,
            saveError = themeSaveError,
            onSelectTheme = onSelectTheme,
        )

        SectionTitle(title = stringResource(R.string.diag_section_alerts))
        DiagnosticsAlertsCard(
            alertsEnabled = uiState.alertsEnabled,
            onSetAlertsEnabled = onSetAlertsEnabled,
        )

        DiagnosticsTechSection(
            uiState = uiState,
            onSendTest = onSendTest,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = RenfeSpacing.sm),
    )
}