package com.pablovb019.renfenotifier.feature.followups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.core.network.RenfeApi
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class FollowUpDetailUiState(
    val loading: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionError: String? = null,
    val detail: FollowUpDetailOut? = null,
    val deleted: Boolean = false,
) {
    /** Última comprobación válida: el instante (UTC) del episodio más reciente. */
    val lastValidObservedAt: Instant? =
        detail?.episodes?.maxByOrNull { episode ->
            runCatching { OffsetDateTime.parse(episode.observedAt).toInstant().toEpochMilli() }
                .getOrDefault(0L)
        }?.observedAt?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
}

/**
 * Detalle y acciones de un seguimiento: pausar, reanudar, renovar, confirmar
 * aviso (sin borrar el seguimiento) y eliminar. El servidor es la fuente de
 * verdad: tras cada acción se recarga el detalle y nunca se muestra éxito si
 * la llamada remota falló.
 */
class FollowUpDetailViewModel(
    private val api: RenfeApi,
    private val followupId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowUpDetailUiState())
    val uiState: StateFlow<FollowUpDetailUiState> = _uiState.asStateFlow()

    private val detail: FollowUpDetailOut?
        get() = _uiState.value.detail

    fun load() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, actionError = null) }
            try {
                val loaded = api.getFollowUp(followupId)
                _uiState.update { it.copy(loading = false, detail = loaded) }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(loading = false, actionError = httpMessage(error.code()))
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(loading = false, actionError = "Sin conexión con el servidor. Reintenta.")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        actionError = error.localizedMessage ?: "Error inesperado.",
                    )
                }
            }
        }
    }

    fun pause() = runAction { api.pauseFollowUp(followupId) }

    fun resume() = runAction { api.resumeFollowUp(followupId) }

    fun renew() = runAction { api.renewFollowUp(followupId) }

    fun acknowledge() = runAction { api.acknowledgeFollowUp(followupId) }

    fun delete() {
        if (detail == null || _uiState.value.actionInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            try {
                api.deleteFollowUp(followupId)
                _uiState.update {
                    it.copy(actionInProgress = false, deleted = true)
                }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(actionInProgress = false, actionError = httpMessage(error.code()))
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionError = "Sin conexión con el servidor. Reintenta.",
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionError = error.localizedMessage ?: "Error inesperado.",
                    )
                }
            }
        }
    }

    private fun runAction(action: suspend () -> Unit) {
        if (detail == null || _uiState.value.actionInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            try {
                action()
                _uiState.update { it.copy(actionInProgress = false) }
                load() // fuente de verdad: recargar desde el servidor
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(actionInProgress = false, actionError = httpMessage(error.code()))
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionError = "Sin conexión con el servidor. Reintenta.",
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionError = error.localizedMessage ?: "Error inesperado.",
                    )
                }
            }
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401 -> "Emparejamiento revocado. Vuelve a emparejar el dispositivo."
        404 -> "Seguimiento no encontrado."
        409 -> "El estado actual del seguimiento no permite esta acción."
        else -> "Error del servidor ($code)."
    }
}