package com.pablovb019.renfenotifier.ui.theme

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.core.security.SettingsSource
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Estado del modo de tema. Lee el valor guardado de DataStore al arrancar y
 * persiste cada cambio de forma optimista y serializada (solo la ultima escritura
 * gana). Un fallo de lectura/escritura se expone en [saveError] sin romper el
 * estado visible; `CancellationException` nunca se captura.
 */
class ThemeViewModel(
    private val settings: SettingsSource,
) : ViewModel() {

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            settings.theme
                .map { ThemeMode.fromStorage(it) }
                .onEach { loaded ->
                    _mode.value = loaded
                    _isLoading.value = false
                }
                .catch { cause ->
                    _isLoading.value = false
                    _saveError.value = cause.message ?: "No se pudo leer el tema guardado."
                }
                .collect { }
        }
    }

    fun setMode(newMode: ThemeMode) {
        if (newMode == _mode.value) return
        _saveError.value = null
        _mode.value = newMode
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            try {
                settings.setTheme(newMode.toStorage())
            } catch (e: IOException) {
                _saveError.value = e.message ?: "No se pudo guardar el tema."
            }
        }
    }
}

/** Fabrica con el contexto de la aplicacion para crear [ThemeViewModel]. */
class ThemeViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return ThemeViewModel(PreferencesRepository(app)) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}