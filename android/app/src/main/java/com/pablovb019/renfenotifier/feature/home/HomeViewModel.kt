package com.pablovb019.renfenotifier.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.BuildConfig
import com.pablovb019.renfenotifier.core.network.AuthInterceptor
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de inicio: expone un [StateFlow] inmutable con la
 * hora local y el estado de emparejamiento (fuente de verdad local, el estado
 * remoto vivo llega en pasos posteriores).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState(versionName = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(now = LocalDateTime.now(), loading = false) }
                delay(TICK_MILLIS)
            }
        }

        viewModelScope.launch {
            prefs.isPaired.collect { paired ->
                _uiState.update { it.copy(isPaired = paired) }
            }
        }

        // Una credencial revocada (401) deriva a re-emparejamiento.
        viewModelScope.launch {
            AuthInterceptor.credentialsRevoked.collect {
                prefs.clearAll()
                _uiState.update { it.copy(isPaired = false) }
            }
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}