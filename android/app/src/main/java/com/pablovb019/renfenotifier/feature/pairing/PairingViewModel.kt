package com.pablovb019.renfenotifier.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.core.network.RenfeApi
import com.pablovb019.renfenotifier.core.network.model.ClaimRequest
import com.pablovb019.renfenotifier.core.security.SessionStore
import com.pablovb019.renfenotifier.core.security.TokenVault
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class PairingUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val paired: Boolean = false,
)

/**
 * ViewModel de emparejamiento con dependencias inyectadas (probable sin
 * Android). El token solo se guarda en el Keystore cuando el backend responde
 * 200; nunca se marca como emparejado una operación remota fallida.
 */
class PairingViewModel(
    private val api: RenfeApi,
    private val vault: TokenVault,
    private val sessionStore: SessionStore,
    private val deviceIdProvider: () -> String,
    private val deviceNameProvider: () -> String = { "Renfe Notifier" },
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onCodeChange(newCode: String) {
        val sanitized = newCode.filter { it.isDigit() || it in 'A'..'Z' || it in 'a'..'z' || it == '-' }
        _uiState.update { it.copy(code = sanitized, error = null) }
    }

    fun claim() {
        val code = _uiState.value.code.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(error = "Introduce el código de emparejamiento.") }
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.claimPairing(
                    ClaimRequest(
                        code = code,
                        deviceId = deviceIdProvider(),
                        deviceName = deviceNameProvider(),
                    ),
                )

                // Solo se guarda localmente cuando el backend respondió OK con token.
                vault.save(response.deviceToken)
                sessionStore.setPaired(response.deviceId, deviceNameProvider())
                _uiState.update { it.copy(isLoading = false, paired = true, error = null) }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(isLoading = false, error = error.httpMessage(error.code()))
                }
            } catch (error: UnknownHostException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Sin conexión. Comprueba tu red.")
                }
            } catch (error: SocketTimeoutException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Se agotó el tiempo de espera. Reintenta.")
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error de conexión. Reintenta.")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = error.localizedMessage ?: "Error inesperado.")
                }
            }
        }
    }

    private fun HttpException.httpMessage(code: Int): String = when (code) {
        401 -> "Código de emparejamiento no válido o caducado."
        429 -> "Demasiados intentos. Espera un minuto."
        in 500..599 -> "Error del servidor. Inténtalo más tarde."
        else -> "Error inesperado ($code)."
    }
}