package com.pablovb019.renfenotifier.feature.pairing

import com.pablovb019.renfenotifier.core.network.FakeRenfeApi
import com.pablovb019.renfenotifier.core.network.model.ClaimRequest
import com.pablovb019.renfenotifier.core.network.model.ClaimResponse
import com.pablovb019.renfenotifier.core.security.InMemoryTokenVault
import com.pablovb019.renfenotifier.core.security.SessionStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var api: FakeRenfeApi
    private lateinit var vault: InMemoryTokenVault
    private lateinit var session: FakeSessionStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api = FakeRenfeApi()
        vault = InMemoryTokenVault()
        session = FakeSessionStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PairingViewModel(
        api = api,
        vault = vault,
        sessionStore = session,
        deviceIdProvider = { "test-device" },
        deviceNameProvider = { "Test Phone" },
    )

    @Test
    fun `claim exitoso guarda token y marca sesion emparejada solo tras respuesta OK`() = runTest(dispatcher) {
        api.claimHandler = { request ->
            assertEquals("RF-VALID", request.code)
            assertEquals("test-device", request.deviceId)
            ClaimResponse(deviceToken = "tok-1", deviceId = "test-device")
        }

        val vm = viewModel()
        vm.onCodeChange("RF-VALID")
        vm.claim()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.paired)
        assertEquals("tok-1", vault.getToken())
        assertTrue(session.paired)
        assertNull(state.error)
    }

    @Test
    fun `codigo vacio muestra error y no contacta con el servidor`() = runTest(dispatcher) {
        var called = false
        api.claimHandler = {
            called = true
            ClaimResponse("t", "d")
        }

        val vm = viewModel()
        vm.claim()
        advanceUntilIdle()

        assertFalse(called)
        assertFalse(vm.uiState.value.paired)
        assertTrue(vm.uiState.value.error!!.contains("Introduce el código"))
        assertNull(vault.getToken())
    }

    @Test
    fun `un 401 muestra codigo invalido y no guarda la operacion como exitosa`() = runTest(dispatcher) {
        api.claimHandler = {
            throw HttpException(Response.error<Any>(401, "denegado".toResponseBody("text/plain".toMediaType())))
        }

        val vm = viewModel()
        vm.onCodeChange("RF-BAD")
        vm.claim()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.paired)
        assertTrue(state.error!!.contains("no válido"))
        assertNull(vault.getToken())
        assertFalse(session.paired)
    }

    @Test
    fun `un 429 mapea a limite de intentos`() = runTest(dispatcher) {
        api.claimHandler = {
            throw HttpException(Response.error<Any>(429, "too many".toResponseBody("text/plain".toMediaType())))
        }

        val vm = viewModel()
        vm.onCodeChange("RF-TEST")
        vm.claim()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.paired)
        assertTrue(vm.uiState.value.error!!.contains("Demasiados intentos"))
    }

    @Test
    fun `error de red muestra error de conexion y no marca emparejado`() = runTest(dispatcher) {
        api.claimHandler = { throw IOException("sin red") }

        val vm = viewModel()
        vm.onCodeChange("RF-TEST")
        vm.claim()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.paired)
        assertTrue(state.error!!.contains("Error de conexión"))
        assertNull(vault.getToken())
    }

    @Test
    fun `sanitiza la entrada y solo conserva letras digitos y guiones`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCodeChange("RF-á 123!")
        assertEquals("RF-123", vm.uiState.value.code)
    }
}

private class FakeSessionStore : SessionStore {
    private val pairedFlow = MutableStateFlow(false)
    override val isPaired: Flow<Boolean> = pairedFlow

    var paired: Boolean = false
        private set

    override suspend fun setPaired(deviceId: String, deviceName: String) {
        paired = true
        pairedFlow.value = true
    }

    override suspend fun clearAll() {
        paired = false
        pairedFlow.value = false
    }
}