package com.pablovb019.renfenotifier.feature.pairing

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentales del formulario de emparejamiento: teclado de texto (no
 * numérico), campo con error asociado y CTA deshabilitado mientras no haya
 * código o se esté validando.
 */
@RunWith(AndroidJUnit4::class)
class PairingContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    private fun setPairingContent(
        uiState: PairingUiState,
        onCodeChange: (String) -> Unit = {},
        onClaim: () -> Unit = {},
    ) {
        composeRule.setContent {
            RenfeNotifierTheme {
                PairingContent(
                    uiState = uiState,
                    onCodeChange = onCodeChange,
                    onClaim = onClaim,
                    modifier = Modifier.padding(com.pablovb019.renfenotifier.ui.theme.RenfeSpacing.screenMargin),
                )
            }
        }
    }

    private val codeField = "Código de emparejamiento"

    @Test
    fun hintVisibleAlInicio() {
        setPairingContent(uiState = PairingUiState())
        composeRule.onNodeWithText(text(R.string.pairing_hint)).assertIsDisplayed()
    }

    @Test
    fun botonDeshabilitadoSinCodigoYHabilitadoConCodigo() {
        var claims = 0
        setPairingContent(uiState = PairingUiState(), onClaim = { claims++ })
        composeRule.onNodeWithTag("pairing_claim_button").assertIsNotEnabled()

        setPairingContent(uiState = PairingUiState(code = "RF-12AB34"), onClaim = { claims++ })
        composeRule.onNodeWithTag("pairing_claim_button").performClick()
        assertEquals(1, claims)
    }

    @Test
    fun cargandoDeshabilitaCampoYBoton() {
        setPairingContent(uiState = PairingUiState(code = "RF-12AB34", isLoading = true))
        composeRule.onNodeWithContentDescription(codeField).assertIsNotEnabled()
        composeRule.onNodeWithTag("pairing_claim_button").assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.pairing_button)).assertDoesNotExist()
    }

    @Test
    fun errorAsociadoSeMuestraBajoElCampo() {
        val message = "Código de emparejamiento no válido o caducado."
        setPairingContent(uiState = PairingUiState(code = "RF-000", error = message))
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Error de emparejamiento").assertIsDisplayed()
    }

    @Test
    fun escribirCodigoInvocaCambioDeValor() {
        val changes = mutableListOf<String>()
        setPairingContent(uiState = PairingUiState(), onCodeChange = { changes.add(it) })
        composeRule.onNodeWithContentDescription(codeField).performTextInput("RF-12AB34")
        assertEquals(listOf("RF-12AB34"), changes)
    }
}