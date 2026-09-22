package com.pablovb019.renfenotifier.feature.diagnostics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.network.model.DiagnosticsOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpCountsOut
import com.pablovb019.renfenotifier.core.network.model.SearchStatsOut
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.ThemeMode
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentales del contenido de Ajustes: título, secciones
 * Apariencia/Avisos/Diagnóstico técnico plegable, botón de prueba y switch.
 */
@RunWith(AndroidJUnit4::class)
class DiagnosticsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun sampleDiagnostics() = DiagnosticsOut(
        status = "ok",
        appVersion = "2.4.1",
        dbOk = true,
        devices = 1,
        followups = FollowUpCountsOut(active = 2, paused = 0, expired = 0, deleted = 0, total = 2),
        episodes = 3,
        alertsPending = 1,
        searchStats = SearchStatsOut(logicalQueries = 42, httpRequests = 55, bytesReceived = 4096),
    )

    private fun sampleUiState(
        testState: TestNotificationState = TestNotificationState.Idle,
        alertsEnabled: Boolean = true,
    ) = DiagnosticsUiState(
        loading = false,
        diagnostics = sampleDiagnostics(),
        error = null,
        lastServerContactAt = Instant.now(),
        googlePlayServicesAvailable = true,
        postNotificationsGranted = true,
        fcmChannelEnabled = true,
        fcmConfigured = true,
        alertsEnabled = alertsEnabled,
        testNotificationState = testState,
        stages = computeStages(
            StageFacts(
                serverReachable = true,
                logicalQueries = 42L,
                testState = testState,
                fcmConfigured = true,
                postNotificationsGranted = true,
                fcmChannelEnabled = true,
                alertsEnabled = alertsEnabled,
            ),
        ),
    )

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    private fun setDiagnosticsContent(
        uiState: DiagnosticsUiState,
        onSelectTheme: (ThemeMode) -> Unit = {},
        onSetAlertsEnabled: (Boolean) -> Unit = {},
        onSendTest: () -> Unit = {},
    ) {
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeScreenScaffold(
                    title = text(R.string.diag_title),
                    onBack = {},
                ) { innerPadding ->
                    DiagnosticsContent(
                        uiState = uiState,
                        themeMode = ThemeMode.LIGHT,
                        themeLoading = false,
                        themeSaveError = null,
                        onSelectTheme = onSelectTheme,
                        onSetAlertsEnabled = onSetAlertsEnabled,
                        onSendTest = onSendTest,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun expandTechSection() {
        composeRule.onNodeWithTag("diagnostics_section_tech").performScrollTo().performClick()
    }

    @Test
    fun tituloYSeccionesVisiblesAlInicio() {
        setDiagnosticsContent(uiState = sampleUiState())
        composeRule.onNodeWithText(text(R.string.diag_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_section_appearance)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_section_alerts)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_section_tech)).assertIsDisplayed()
    }

    @Test
    fun diagnosticoTecnicoEmpiezaPlegado() {
        setDiagnosticsContent(uiState = sampleUiState())
        composeRule.onNodeWithText(text(R.string.diag_stage_query)).assertDoesNotExist()
    }

    @Test
    fun expandirDiagnosticoMuestraLasEtapas() {
        setDiagnosticsContent(uiState = sampleUiState())
        expandTechSection()
        composeRule.onNodeWithText(text(R.string.diag_stage_query)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_stage_detection)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_stage_send)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_stage_receive)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_stage_notify)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun recolapsarDiagnosticoOcultaEtapas() {
        setDiagnosticsContent(uiState = sampleUiState())
        expandTechSection()
        composeRule.onNodeWithText(text(R.string.diag_stage_query)).assertIsDisplayed()
        expandTechSection()
        composeRule.onNodeWithText(text(R.string.diag_stage_query)).assertDoesNotExist()
    }

    @Test
    fun expandirMuestraInfoPreviaDelServidor() {
        setDiagnosticsContent(uiState = sampleUiState())
        expandTechSection()
        val counts = composeRule.activity.getString(R.string.diag_count_devices, 1)
        composeRule.onNodeWithText(counts).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_google_play_ok)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun botonEnviarPruebaInvocaCallbackUnaVez() {
        var sends = 0
        setDiagnosticsContent(uiState = sampleUiState(), onSendTest = { sends++ })
        expandTechSection()
        composeRule.onNodeWithText(text(R.string.diag_test_button))
            .performScrollTo().performClick()
        assertEquals(1, sends)
    }

    @Test
    fun enviandoDeshabilitaBotonYMuestraEstado() {
        setDiagnosticsContent(uiState = sampleUiState(testState = TestNotificationState.Sending))
        expandTechSection()
        composeRule.onNodeWithText(text(R.string.diag_test_button))
            .performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.diag_test_pending))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun enviandoNoReinvocaClicEnBotonDeshabilitado() {
        var sends = 0
        setDiagnosticsContent(uiState = sampleUiState(testState = TestNotificationState.Sending), onSendTest = { sends++ })
        expandTechSection()
        composeRule.onNodeWithText(text(R.string.diag_test_button))
            .performScrollTo().performClick()
        assertEquals(0, sends)
    }

    @Test
    fun switchDeAvisosAlternaEstado() {
        var lastEnabled: Boolean? = null
        setDiagnosticsContent(uiState = sampleUiState(), onSetAlertsEnabled = { lastEnabled = it })
        composeRule.onNodeWithTag("diagnostics_alerts_switch").performScrollTo().performClick()
        assertEquals(false, lastEnabled)
    }

    @Test
    fun selectorDeTemaInvocableConLasTresOpciones() {
        val selections = mutableListOf<ThemeMode>()
        setDiagnosticsContent(uiState = sampleUiState(), onSelectTheme = { selections.add(it) })
        composeRule.onNodeWithTag("theme_option_light").performScrollTo().performClick()
        composeRule.onNodeWithTag("theme_option_dark").performScrollTo().performClick()
        composeRule.onNodeWithTag("theme_option_system").performScrollTo().performClick()
        assertEquals(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM), selections)
        composeRule.onAllNodesWithText(text(R.string.diag_section_tech)).assertCountEquals(1)
    }
}