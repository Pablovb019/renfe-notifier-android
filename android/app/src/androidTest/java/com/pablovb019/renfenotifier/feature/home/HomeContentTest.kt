package com.pablovb019.renfenotifier.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentales del contenido de la pantalla de inicio. */
@RunWith(AndroidJUnit4::class)
class HomeContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clickVincularDispositivoLlamaUnaVezAlCallback() {
        var clicks = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(
                    uiState = HomeUiState(isPaired = false),
                    onNavigateToPairing = { clicks++ },
                )
            }
        }
        composeRule.onNodeWithText("Vincular dispositivo").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun clickBuscarTrenesLlamaUnaVezAlCallback() {
        var clicks = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(
                    uiState = HomeUiState(isPaired = true),
                    onNavigateToSearch = { clicks++ },
                )
            }
        }
        composeRule.onNodeWithText("Buscar trenes").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun clickMisSeguimientosLlamaUnaVezAlCallback() {
        var clicks = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(
                    uiState = HomeUiState(isPaired = true),
                    onNavigateToFollowUps = { clicks++ },
                )
            }
        }
        composeRule.onNodeWithText("Mis seguimientos").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun clickAjustesLlamaUnaVezAlCallback() {
        var clicks = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(
                    uiState = HomeUiState(isPaired = true),
                    onNavigateToDiagnostics = { clicks++ },
                )
            }
        }
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun noExisteBotonRecargar() {
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(uiState = HomeUiState(isPaired = true))
            }
        }
        composeRule.onAllNodesWithText("Recargar").assertCountEquals(0)
    }

    @Test
    fun emparejadoMuestraDispositivoVinculado() {
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(uiState = HomeUiState(isPaired = true))
            }
        }
        composeRule.onNodeWithText("Dispositivo vinculado").assertIsDisplayed()
    }
}