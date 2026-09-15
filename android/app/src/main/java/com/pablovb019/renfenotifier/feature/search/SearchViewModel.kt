package com.pablovb019.renfenotifier.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.core.model.FollowUpMode
import com.pablovb019.renfenotifier.core.network.RenfeApi
import com.pablovb019.renfenotifier.core.network.model.FollowUpCreateRequest
import com.pablovb019.renfenotifier.core.network.model.StationOut
import com.pablovb019.renfenotifier.core.network.model.TrainOut
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Estado de la pantalla de búsqueda. Distingue explícitamente error de ausencia
 * de plazas: `searchError` es un fallo de red/servidor; que `trains` esté vacío
 * es un resultado legítimo y se representa como estado vacío.
 */
data class SearchUiState(
    val originQuery: String = "",
    val originSuggestions: List<StationOut> = emptyList(),
    val selectedOrigin: StationOut? = null,
    val destinationQuery: String = "",
    val destinationSuggestions: List<StationOut> = emptyList(),
    val selectedDestination: StationOut? = null,
    val travelDate: LocalDate = LocalDate.now(),
    val plazaH: Boolean = false,
    val isSearching: Boolean = false,
    val searchStatus: String? = null,
    val searchError: String? = null,
    val trains: List<TrainOut> = emptyList(),
    val mode: FollowUpMode = FollowUpMode.FIRST,
    val selectedTrainIdentity: String? = null,
    val isCreatingFollowUp: Boolean = false,
    val createdFollowUpId: String? = null,
    val followUpError: String? = null,
) {
    val hasSearched: Boolean get() = searchStatus != null || searchError != null
    val isEmptyResult: Boolean
        get() = hasSearched && searchError == null && trains.isEmpty()
}

/**
 * ViewModel de búsqueda: estaciones con alias/tildes/grupos vía el backend
 * (anti-SSRF), búsqueda de trenes y creación de seguimientos reutilizando la
 * ruta y fecha vigentes. Dependencias inyectadas para poder probarlo en JVM.
 */
class SearchViewModel(
    private val api: RenfeApi,
    private val now: () -> LocalDate = LocalDate::now,
    private val suggestionsDebounceMillis: Long = 300,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(travelDate = now()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var originSuggestionsJob: Job? = null
    private var destinationSuggestionsJob: Job? = null

    fun onOriginQueryChange(query: String) {
        _uiState.update { it.copy(originQuery = query, searchError = null) }
        originSuggestionsJob?.cancel()
        originSuggestionsJob = debounceSuggestions(query.trim()) { suggestions ->
            _uiState.update { it.copy(originSuggestions = suggestions) }
        }
    }

    fun onDestinationQueryChange(query: String) {
        _uiState.update { it.copy(destinationQuery = query, searchError = null) }
        destinationSuggestionsJob?.cancel()
        destinationSuggestionsJob = debounceSuggestions(query.trim()) { suggestions ->
            _uiState.update { it.copy(destinationSuggestions = suggestions) }
        }
    }

    fun onOriginSelected(station: StationOut) {
        _uiState.update {
            it.copy(
                selectedOrigin = station,
                originQuery = station.name,
                originSuggestions = emptyList(),
                searchError = null,
            )
        }
    }

    fun onDestinationSelected(station: StationOut) {
        _uiState.update {
            it.copy(
                selectedDestination = station,
                destinationQuery = station.name,
                destinationSuggestions = emptyList(),
                searchError = null,
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(travelDate = date, searchError = null) }
    }

    fun onPlazaHChange(enabled: Boolean) {
        _uiState.update { it.copy(plazaH = enabled, searchError = null) }
    }

    fun onModeSelected(mode: FollowUpMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun onTrainSelected(identity: String) {
        _uiState.update {
            it.copy(mode = FollowUpMode.SPECIFIC, selectedTrainIdentity = identity)
        }
    }

    fun search() {
        val state = _uiState.value
        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(searchError = validationError, trains = emptyList(), searchStatus = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    searchError = null,
                    searchStatus = null,
                    trains = emptyList(),
                )
            }
            try {
                val origin = state.selectedOrigin!!
                val destination = state.selectedDestination!!
                val response = api.searchTrains(
                    com.pablovb019.renfenotifier.core.network.model.TrainSearchRequest(
                        originCode = origin.code,
                        destinationCode = destination.code,
                        travelDate = state.travelDate.toString(),
                        plazaH = state.plazaH,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchStatus = response.status,
                        trains = response.trains,
                    )
                }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(isSearching = false, searchError = httpSearchMessage(error.code()))
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(isSearching = false, searchError = "Sin conexión con el servidor. Reintenta.")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, searchError = error.localizedMessage ?: "Error inesperado.")
                }
            }
        }
    }

    fun createFollowUp() {
        val state = _uiState.value
        val origin = state.selectedOrigin
        val destination = state.selectedDestination
        if (origin == null || destination == null) {
            _uiState.update { it.copy(followUpError = "Selecciona origen y destino.") }
            return
        }
        if (state.mode.requiresTrain && state.selectedTrainIdentity == null) {
            _uiState.update { it.copy(followUpError = "Selecciona un tren de los resultados.") }
            return
        }
        if (state.isCreatingFollowUp) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingFollowUp = true, followUpError = null) }
            try {
                val created = api.createFollowUp(
                    FollowUpCreateRequest(
                        originCode = origin.code,
                        destinationCode = destination.code,
                        travelDate = state.travelDate.toString(),
                        mode = state.mode.apiValue,
                        plazaH = state.plazaH,
                        specificTrainId = state.selectedTrainIdentity,
                        departureTime = null,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isCreatingFollowUp = false,
                        createdFollowUpId = created.followupId,
                    )
                }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(
                        isCreatingFollowUp = false,
                        followUpError = httpCreateMessage(error.code()),
                    )
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(
                        isCreatingFollowUp = false,
                        followUpError = "Sin conexión con el servidor. Reintenta.",
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingFollowUp = false,
                        followUpError = error.localizedMessage ?: "Error inesperado.",
                    )
                }
            }
        }
    }

    private fun debounceSuggestions(query: String, onResult: (List<StationOut>) -> Unit): Job {
        if (query.isEmpty()) {
            onResult(emptyList())
            return Job().apply { cancel() }
        }
        return viewModelScope.launch {
            delay(suggestionsDebounceMillis)
            val suggestions = runCatching { api.searchStations(query, SUGGESTION_LIMIT) }
                .getOrDefault(emptyList())
            onResult(suggestions)
        }
    }

    private fun validate(state: SearchUiState): String? {
        val origin = state.selectedOrigin
        val destination = state.selectedDestination
        if (origin == null || destination == null) return "Selecciona origen y destino de la lista."
        if (origin.code == destination.code) return "Origen y destino no pueden coincidir."
        if (state.travelDate.isBefore(now())) return "La fecha no puede ser anterior a hoy."
        if (state.travelDate.isAfter(now().plusDays(MAX_HORIZON_DAYS))) {
            return "La fecha supera el horizonte de $MAX_HORIZON_DAYS días."
        }
        return null
    }

    private fun httpSearchMessage(code: Int): String = when (code) {
        400 -> "Ruta o fecha no válida. Revisa los datos."
        401 -> "Emparejamiento revocado. Vuelve a emparejar el dispositivo."
        503 -> "Renfe no respondió. Reintenta en unos minutos."
        else -> "Error del servidor ($code)."
    }

    private fun httpCreateMessage(code: Int): String = when (code) {
        400 -> "No se pudo crear el seguimiento. Revisa la ruta y la fecha."
        401 -> "Emparejamiento revocado. Vuelve a emparejar el dispositivo."
        409 -> "El seguimiento no admite esa acción en su estado actual."
        else -> "Error del servidor ($code)."
    }

    private companion object {
        const val SUGGESTION_LIMIT = 10
        const val MAX_HORIZON_DAYS = 62L
    }
}