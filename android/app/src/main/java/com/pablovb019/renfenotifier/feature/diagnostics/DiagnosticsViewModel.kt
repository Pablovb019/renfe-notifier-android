package com.pablovb019.renfenotifier.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablovb019.renfenotifier.core.diagnostics.DeviceEnvironment
import com.pablovb019.renfenotifier.core.network.RenfeApi
import com.pablovb019.renfenotifier.core.network.model.DiagnosticsOut
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.core.security.SettingsSource
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Etapas del flujo de avisos explicadas en la pantalla de diagnóstico. */
enum class StageKind { QUERY, DETECTION, SEND, RECEIVE, NOTIFY }

enum class StageStatus { OK, ATTENTION, BLOCKED, UNKNOWN }

data class DiagnosticStage(val kind: StageKind, val status: StageStatus, val detail: String)

sealed interface TestNotificationState {
    data object Idle : TestNotificationState
    data object Sending : TestNotificationState
    data class Sent(val messageId: String) : TestNotificationState
    data class Failed(val reason: String) : TestNotificationState
}

data class DiagnosticsUiState(
    val loading: Boolean = false,
    val diagnostics: DiagnosticsOut? = null,
    val error: String? = null,
    val lastServerContactAt: Instant? = null,
    val googlePlayServicesAvailable: Boolean = true,
    val postNotificationsGranted: Boolean = true,
    val fcmChannelEnabled: Boolean = true,
    val fcmConfigured: Boolean = false,
    val alertsEnabled: Boolean = true,
    val theme: String = PreferencesRepository.THEME_SYSTEM,
    val testNotificationState: TestNotificationState = TestNotificationState.Idle,
    val stages: List<DiagnosticStage> = emptyList(),
) {
    fun facts(): StageFacts = StageFacts(
        serverReachable = if (loading) null else (error == null),
        logicalQueries = diagnostics?.searchStats?.logicalQueries ?: 0L,
        testState = testNotificationState,
        fcmConfigured = fcmConfigured,
        postNotificationsGranted = postNotificationsGranted,
        fcmChannelEnabled = fcmChannelEnabled,
        alertsEnabled = alertsEnabled,
    )
}

/** Datos que [computeStages] necesita para decidir el estado de cada etapa. */
data class StageFacts(
    val serverReachable: Boolean? = null,
    val logicalQueries: Long = 0L,
    val testState: TestNotificationState = TestNotificationState.Idle,
    val fcmConfigured: Boolean = false,
    val postNotificationsGranted: Boolean = true,
    val fcmChannelEnabled: Boolean = true,
    val alertsEnabled: Boolean = true,
)

/**
 * Estado puro de las cinco etapas del flujo: consulta, detección, envío,
 * entrega y visualización. El envío no se puede verificar desde la app salvo
 * con la notificación de prueba; la entrega real depende de FCM y del sistema.
 */
fun computeStages(facts: StageFacts): List<DiagnosticStage> {
    fun queryStage() = when (facts.serverReachable) {
        true -> DiagnosticStage(
            StageKind.QUERY,
            StageStatus.OK,
            "El servidor responde y se puede consultar la disponibilidad.",
        )
        false -> DiagnosticStage(
            StageKind.QUERY,
            StageStatus.BLOCKED,
            "No se puede contactar con el servidor de avisos.",
        )
        null -> DiagnosticStage(
            StageKind.QUERY,
            StageStatus.UNKNOWN,
            "Todavía no hay contactos con el servidor en esta sesión.",
        )
    }

    fun detectionStage() = if (facts.logicalQueries > 0L) {
        DiagnosticStage(
            StageKind.DETECTION,
            StageStatus.OK,
            "El servidor ha comprobado ${facts.logicalQueries} grupos de seguimientos desde su reinicio.",
        )
    } else {
        DiagnosticStage(
            StageKind.DETECTION,
            StageStatus.ATTENTION,
            "Aún no hay comprobaciones registradas desde el reinicio del servidor.",
        )
    }

    fun sendStage() = when (val state = facts.testState) {
        is TestNotificationState.Sent -> DiagnosticStage(
            StageKind.SEND,
            StageStatus.OK,
            "Notificación de prueba enviada correctamente (id ${state.messageId}).",
        )
        is TestNotificationState.Failed -> DiagnosticStage(
            StageKind.SEND,
            StageStatus.BLOCKED,
            "No se pudo enviar la notificación de prueba: ${state.reason}",
        )
        else -> DiagnosticStage(
            StageKind.SEND,
            StageStatus.UNKNOWN,
            "La app no puede confirmarlo por sí sola; usa la notificación de prueba.",
        )
    }

    fun receiveStage() = if (!facts.fcmConfigured) {
        DiagnosticStage(
            StageKind.RECEIVE,
            StageStatus.ATTENTION,
            "Firebase aún no está configurado en la app, así que la entrega real no se puede verificar.",
        )
    } else if (!facts.postNotificationsGranted) {
        DiagnosticStage(
            StageKind.RECEIVE,
            StageStatus.BLOCKED,
            "Permiso de notificaciones denegado en los ajustes del sistema.",
        )
    } else if (!facts.fcmChannelEnabled) {
        DiagnosticStage(
            StageKind.RECEIVE,
            StageStatus.BLOCKED,
            "El canal de avisos de la app está bloqueado en los ajustes.",
        )
    } else {
        DiagnosticStage(
            StageKind.RECEIVE,
            StageStatus.OK,
            "Firebase está configurado y el canal de avisos está activo.",
        )
    }

    fun notifyStage() = if (!facts.alertsEnabled) {
        DiagnosticStage(
            StageKind.NOTIFY,
            StageStatus.ATTENTION,
            "Tienes los avisos desactivados en los ajustes de la app.",
        )
    } else if (!facts.postNotificationsGranted || !facts.fcmChannelEnabled) {
        DiagnosticStage(
            StageKind.NOTIFY,
            StageStatus.ATTENTION,
            "Revisa el permiso y el canal de notificaciones: sin ellos no se muestran los avisos.",
        )
    } else {
        DiagnosticStage(
            StageKind.NOTIFY,
            StageStatus.OK,
            "El canal de avisos está activo y podrá mostrarlos al llegar.",
        )
    }

    return listOf(queryStage(), detectionStage(), sendStage(), receiveStage(), notifyStage())
}

/**
 * Panel de diagnóstico y ajustes: salud del flujo de avisos por etapas, cifras
 * del servidor (consultas lógicas vs peticiones HTTP), última comprobación
 * válida, notificación de prueba y preferencias de tema y avisos.
 */
class DiagnosticsViewModel(
    private val api: RenfeApi,
    private val environment: DeviceEnvironment,
    private val settings: SettingsSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.theme.collect { theme -> _uiState.update { it.copy(theme = theme) } }
        }
        viewModelScope.launch {
            settings.alertsEnabled.collect { enabled ->
                _uiState.update { it.copy(alertsEnabled = enabled) }
                recomputeStages()
            }
        }
        refreshEnvironment()
    }

    fun refreshEnvironment() {
        _uiState.update {
            it.copy(
                googlePlayServicesAvailable = environment.googlePlayServicesAvailable(),
                postNotificationsGranted = environment.postNotificationsGranted(),
                fcmChannelEnabled = environment.fcmChannelEnabled(),
                fcmConfigured = environment.fcmConfigured(),
                lastServerContactAt = environment.lastServerContactAt()?.let(Instant::ofEpochMilli),
            )
        }
        recomputeStages()
    }

    fun load() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val loaded = api.diagnostics()
                _uiState.update {
                    it.copy(
                        loading = false,
                        diagnostics = loaded,
                        error = null,
                        lastServerContactAt = Instant.now(),
                    )
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
                    it.copy(loading = false, error = error.localizedMessage ?: "Error inesperado.")
                }
            }
            recomputeStages()
        }
    }

    fun sendTestNotification() {
        if (_uiState.value.testNotificationState == TestNotificationState.Sending) return
        viewModelScope.launch {
            _uiState.update { it.copy(testNotificationState = TestNotificationState.Sending) }
            try {
                val result = api.sendTestNotification()
                _uiState.update {
                    it.copy(testNotificationState = TestNotificationState.Sent(result.messageId))
                }
            } catch (error: HttpException) {
                _uiState.update {
                    it.copy(testNotificationState = TestNotificationState.Failed(httpMessage(error.code())))
                }
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(testNotificationState = TestNotificationState.Failed("Sin conexión con el servidor."))
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        testNotificationState = TestNotificationState.Failed(
                            error.localizedMessage ?: "Error inesperado.",
                        ),
                    )
                }
            }
            recomputeStages()
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAlertsEnabled(enabled) }
    }

    private fun recomputeStages() {
        _uiState.update { it.copy(stages = computeStages(it.facts())) }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401 -> "Emparejamiento revocado. Vuelve a emparejar el dispositivo."
        409 -> "Ya hay otra notificación de prueba en curso. Espera y reintenta."
        else -> "Error del servidor ($code)."
    }
}