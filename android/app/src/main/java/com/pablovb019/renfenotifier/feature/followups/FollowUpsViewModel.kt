package com.pablovb019.renfenotifier.feature.followups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.RenfeApi
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Filtro del listado por ciclo de vida (null = todos). */
enum class LifecycleFilter(val apiValue: String?) {
    ALL(null),
    ACTIVE(FollowUpLifecycle.ACTIVE),
    PAUSED(FollowUpLifecycle.PAUSED),
    EXPIRED(FollowUpLifecycle.EXPIRED),
    DELETED(FollowUpLifecycle.DELETED),
}

data class FollowUpsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val filter: LifecycleFilter = LifecycleFilter.ALL,
    val items: List<FollowUpOut> = emptyList(),
    val total: Int = 0,
) {
    val isEmpty: Boolean get() = !loading && error == null && items.isEmpty()
}

/**
 * Listado de seguimientos con la true_fuente en el backend. Los errores se
 * exponen sin marcar éxito falso; el listado se recarga al volver del detalle.
 */
class FollowUpsViewModel(private val api: RenfeApi) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowUpsUiState())
    val uiState: StateFlow<FollowUpsUiState> = _uiState.asStateFlow()

    fun onFilterSelected(filter: LifecycleFilter) {
        if (_uiState.value.filter == filter) return
        _uiState.update { it.copy(filter = filter) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val response = api.listFollowUps(lifecycle = _uiState.value.filter.apiValue)
                _uiState.update {
                    it.copy(loading = false, items = response.items, total = response.total)
                }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(loading = false, error = httpMessage(error.code()))
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(loading = false, error = "Sin conexión con el servidor. Reintenta.")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = error.localizedMessage ?: "Error inesperado al cargar.",
                    )
                }
            }
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401 -> "Emparejamiento revocado. Vuelve a emparejar el dispositivo."
        503 -> "Renfe no respondió. Reintenta en unos minutos."
        else -> "Error del servidor ($code)."
    }
}