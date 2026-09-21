package com.pablovb019.renfenotifier.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.ui.components.ThemeModeSelector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test instrumental (emulador/dispositivo) del selector de modo de tema. */
@RunWith(AndroidJUnit4::class)
class ThemeModeSelectorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun muestraLasTresOpciones() {
        composeRule.setContent {
            RenfeNotifierTheme {
                ThemeModeSelector(selected = ThemeMode.SYSTEM, onSelect = {})
            }
        }
        composeRule.onNodeWithText("Según el sistema").assertIsDisplayed()
        composeRule.onNodeWithText("Claro").assertIsDisplayed()
        composeRule.onNodeWithText("Oscuro").assertIsDisplayed()
    }

    @Test
    fun alPulsarOscuroSeNotificaElModo() {
        var selected: ThemeMode? = null
        composeRule.setContent {
            RenfeNotifierTheme {
                ThemeModeSelector(selected = ThemeMode.LIGHT, onSelect = { selected = it })
            }
        }
        composeRule.onNodeWithTag("theme_option_dark").performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    @Test
    fun alPulsarClaroSeNotificaElModo() {
        var selected: ThemeMode? = null
        composeRule.setContent {
            RenfeNotifierTheme {
                ThemeModeSelector(selected = ThemeMode.DARK, onSelect = { selected = it })
            }
        }
        composeRule.onNodeWithTag("theme_option_light").performClick()
        assertEquals(ThemeMode.LIGHT, selected)
    }

    @Test
    fun cargaMostradaMientrasSeLeeElTema() {
        composeRule.setContent {
            RenfeNotifierTheme {
                ThemeModeSelector(selected = ThemeMode.SYSTEM, onSelect = {}, isLoading = true)
            }
        }
        composeRule.onNodeWithText("Cargando el tema…").assertIsDisplayed()
    }

    @Test
    fun errorDeGuardadoVisible() {
        composeRule.setContent {
            RenfeNotifierTheme {
                ThemeModeSelector(
                    selected = ThemeMode.SYSTEM,
                    onSelect = {},
                    saveError = "No se pudo guardar el tema.",
                )
            }
        }
        composeRule.onNodeWithText("No se pudo guardar el tema.").assertIsDisplayed()
    }
}