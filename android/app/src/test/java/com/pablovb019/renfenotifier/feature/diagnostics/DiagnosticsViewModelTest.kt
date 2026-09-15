package com.pablovb019.renfenotifier.feature.diagnostics

import com.pablovb019.renfenotifier.core.diagnostics.DeviceEnvironment
import com.pablovb019.renfenotifier.core.network.FakeRenfeApi
import com.pablovb019.renfenotifier.core.network.model.DiagnosticsOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpCountsOut
import com.pablovb019.renfenotifier.core.network.model.SearchStatsOut
import com.pablovb019.renfenotifier.core.network.model.TestNotificationOut
import com.pablovb019.renfenotifier.core.security.SettingsSource
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var api: FakeRenfeApi
    private lateinit var environment: FakeDeviceEnvironment
    private lateinit var settings: FakeSettingsSource

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api = FakeRenfeApi()
        environment = FakeDeviceEnvironment()
        settings = FakeSettingsSource()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun diagnostics(logicalQueries: Long = 4L) = DiagnosticsOut(
        status = "ok",
        appVersion = "0.1.0",
        dbOk = true,
        devices = 1,
        followups = FollowUpCountsOut(active = 2, paused = 0, expired = 0, deleted = 0, total = 2),
        episodes = 3,
        alertsPending = 0,
        searchStats = SearchStatsOut(
            logicalQueries = logicalQueries,
            httpRequests = logicalQueries * 5,
            bytesReceived = logicalQueries * 1500,
        ),
    )

    @Test
    fun `carga diagnostico y marca la consulta como OK`() = runTest(dispatcher) {
        api.diagnosticsHandler = { diagnostics() }

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.load()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(null, state.error)
        assertEquals(4L, state.diagnostics?.searchStats?.logicalQueries)
        val queryStage = state.stages.first { it.kind == StageKind.QUERY }
        assertEquals(StageStatus.OK, queryStage.status)
        val detectionStage = state.stages.first { it.kind == StageKind.DETECTION }
        assertEquals(StageStatus.OK, detectionStage.status)
        assertTrue(detectionStage.detail.contains("4"))
    }

    @Test
    fun `sin comprobaciones la deteccion pide atencion`() = runTest(dispatcher) {
        api.diagnosticsHandler = { diagnostics(logicalQueries = 0L) }

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.load()
        advanceUntilIdle()

        val detectionStage = vm.uiState.value.stages.first { it.kind == StageKind.DETECTION }
        assertEquals(StageStatus.ATTENTION, detectionStage.status)
    }

    @Test
    fun `error de red bloquea la consulta`() = runTest(dispatcher) {
        api.diagnosticsHandler = { throw IOException("sin red") }

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.load()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.error!!.contains("conexión"))
        val queryStage = state.stages.first { it.kind == StageKind.QUERY }
        assertEquals(StageStatus.BLOCKED, queryStage.status)
        val sendStage = state.stages.first { it.kind == StageKind.SEND }
        assertEquals(StageStatus.UNKNOWN, sendStage.status)
    }

    @Test
    fun `envio de prueba exitoso marca el envio como OK con su id`() = runTest(dispatcher) {
        api.sendTestNotificationHandler = { TestNotificationOut(messageId = "msg-42") }

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.sendTestNotification()
        advanceUntilIdle()

        val sendStage = vm.uiState.value.stages.first { it.kind == StageKind.SEND }
        assertEquals(StageStatus.OK, sendStage.status)
        assertTrue(sendStage.detail.contains("msg-42"))
    }

    @Test
    fun `fallo del envio de prueba bloquea el envio`() = runTest(dispatcher) {
        api.sendTestNotificationHandler = {
            throw HttpException(
                Response.error<Any>(503, "caido".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.sendTestNotification()
        advanceUntilIdle()

        val sendStage = vm.uiState.value.stages.first { it.kind == StageKind.SEND }
        assertEquals(StageStatus.BLOCKED, sendStage.status)
        assertTrue(sendStage.detail.contains("503"))
    }

    @Test
    fun `firebase sin configurar pone la entrega en atencion`() = runTest(dispatcher) {
        environment.fcmConfigured = false
        environment.postNotificationsGranted = true
        environment.fcmChannelEnabled = true

        api.diagnosticsHandler = { diagnostics() }
        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.refreshEnvironment()
        vm.load()
        advanceUntilIdle()

        val receiveStage = vm.uiState.value.stages.first { it.kind == StageKind.RECEIVE }
        assertEquals(StageStatus.ATTENTION, receiveStage.status)
    }

    @Test
    fun `canal bloqueado bloquea la entrega`() = runTest(dispatcher) {
        environment.fcmConfigured = true
        environment.fcmChannelEnabled = false

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.refreshEnvironment()
        advanceUntilIdle()

        val receiveStage = vm.uiState.value.stages.first { it.kind == StageKind.RECEIVE }
        assertEquals(StageStatus.BLOCKED, receiveStage.status)
    }

    @Test
    fun `avisos desactivados ponen la visualizacion en atencion`() = runTest(dispatcher) {
        settings.alertsEnabled.value = false

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.refreshEnvironment()
        advanceUntilIdle()

        val notifyStage = vm.uiState.value.stages.first { it.kind == StageKind.NOTIFY }
        assertEquals(StageStatus.ATTENTION, notifyStage.status)
    }

    @Test
    fun `cambiar el tema persiste en los ajustes`() = runTest(dispatcher) {
        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.setTheme("dark")
        advanceUntilIdle()

        assertEquals("dark", settings.theme.value)
        assertEquals("dark", vm.uiState.value.theme)
    }

    @Test
    fun `desactivar avisos llega al flujo de la interfaz`() = runTest(dispatcher) {
        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.setAlertsEnabled(false)
        advanceUntilIdle()

        assertEquals(false, settings.alertsEnabled.value)
        assertEquals(false, vm.uiState.value.alertsEnabled)
    }

    @Test
    fun `google play services ausente se refleja sin inventar credenciales`() = runTest(dispatcher) {
        environment.googlePlayServicesAvailable = false

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.refreshEnvironment()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.googlePlayServicesAvailable)
    }

    @Test
    fun `sin contactos previos el servidor permanece desconocido hasta cargar`() = runTest(dispatcher) {
        api.diagnosticsHandler = { throw IOException("sin red") }
        environment.lastServerContact = null

        val vm = DiagnosticsViewModel(api, environment, settings)
        vm.refreshEnvironment()
        advanceUntilIdle()

        assertNull(vm.uiState.value.lastServerContactAt)
        vm.load()
        advanceUntilIdle()
        assertEquals(StageStatus.BLOCKED, vm.uiState.value.stages.first { it.kind == StageKind.QUERY }.status)
    }
}

/** Entorno falso con valores ajustables: nunca toca el dispositivo en JVM. */
private class FakeDeviceEnvironment : DeviceEnvironment {
    var googlePlayServicesAvailable = true
    var postNotificationsGranted = true
    var fcmChannelEnabled = true
    var fcmConfigured = true
    var lastServerContact: Long? = 1_700_000_000_000L

    override fun googlePlayServicesAvailable(): Boolean = googlePlayServicesAvailable

    override fun postNotificationsGranted(): Boolean = postNotificationsGranted

    override fun fcmChannelEnabled(): Boolean = fcmChannelEnabled

    override fun fcmConfigured(): Boolean = fcmConfigured

    override fun lastServerContactAt(): Long? = lastServerContact
}

/** Ajustes falsos con StateFlow para el test (evita DataStore en JVM). */
private class FakeSettingsSource : SettingsSource {
    override val theme: MutableStateFlow<String> = MutableStateFlow("system")
    override val alertsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun setTheme(theme: String) {
        this.theme.value = theme
    }

    override suspend fun setAlertsEnabled(enabled: Boolean) {
        this.alertsEnabled.value = enabled
    }
}