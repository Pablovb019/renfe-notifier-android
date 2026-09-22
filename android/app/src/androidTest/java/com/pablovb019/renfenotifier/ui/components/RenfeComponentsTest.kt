package com.pablovb019.renfenotifier.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentales de los componentes base reutilizables. */
@RunWith(AndroidJUnit4::class)
class RenfeComponentsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun scaffoldMuestraElTituloYSoportaElBotonAtras() {
        var backClicks = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeScreenScaffold(title = "Ajustes", onBack = { backClicks++ }) {
                    RenfeEmptyState(
                        icon = Icons.Filled.Info,
                        title = "Vacío",
                        text = "Sin elementos.",
                    )
                }
            }
        }
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule.onNodeWithText("Sin elementos.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, backClicks)
    }

    @Test
    fun loadingStateMuestraElIndicadorCentrado() {
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeLoadingState()
            }
        }
        composeRule.onNodeWithTag("renfe_loading_state").assertIsDisplayed()
    }

    @Test
    fun emptyStateMuestraTituloTextoYAccionOpcional() {
        var actionClicks = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeEmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "Sin datos",
                    text = "No hay elementos.",
                    actionLabel = "Crear",
                    onAction = { actionClicks++ },
                )
            }
        }
        composeRule.onNodeWithText("Sin datos").assertIsDisplayed()
        composeRule.onNodeWithText("No hay elementos.").assertIsDisplayed()
        composeRule.onNodeWithText("Crear").assertIsDisplayed().performClick()
        assertEquals(1, actionClicks)
    }

    @Test
    fun emptyStateSinAccionNoMuestraBoton() {
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeEmptyState(
                    icon = Icons.Filled.Info,
                    title = "Sin datos",
                    text = "No hay elementos.",
                )
            }
        }
        composeRule.onNodeWithText("Sin datos").assertIsDisplayed()
        composeRule.onAllNodesWithText("Crear").assertCountEquals(0)
    }

    @Test
    fun errorStateSinCallbackNoMuestraReintentar() {
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeErrorState(
                    icon = Icons.Filled.Info,
                    title = "Error",
                    text = "Algo salió mal.",
                )
            }
        }
        composeRule.onNodeWithText("Error").assertIsDisplayed()
        composeRule.onAllNodesWithText("Reintentar").assertCountEquals(0)
    }

    @Test
    fun errorStateConCallbackMuestraReintentar() {
        var retries = 0
        composeRule.setContent {
            RenfeNotifierTheme {
                RenfeErrorState(
                    icon = Icons.Filled.Info,
                    title = "Error",
                    text = "Algo salió mal.",
                    onRetry = { retries++ },
                )
            }
        }
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun statusBadgeMuestraEtiquetaYTipos() {
        composeRule.setContent {
            RenfeNotifierTheme {
                Column {
                    RenfeStatusBadge(label = "Activo", icon = Icons.Filled.CheckCircle, type = BadgeType.SUCCESS)
                    RenfeStatusBadge(label = "Revisar", icon = Icons.Filled.Info, type = BadgeType.WARNING)
                    RenfeStatusBadge(label = "Bloqueado", icon = null, type = BadgeType.ERROR)
                    RenfeStatusBadge(label = "Desconocido", icon = null, type = BadgeType.NEUTRAL)
                }
            }
        }
        composeRule.onNodeWithText("Activo").assertIsDisplayed()
        composeRule.onNodeWithText("Revisar").assertIsDisplayed()
        composeRule.onNodeWithText("Bloqueado").assertIsDisplayed()
        composeRule.onNodeWithText("Desconocido").assertIsDisplayed()
    }
}