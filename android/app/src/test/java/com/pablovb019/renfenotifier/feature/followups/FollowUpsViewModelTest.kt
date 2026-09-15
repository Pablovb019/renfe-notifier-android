package com.pablovb019.renfenotifier.feature.followups

import com.pablovb019.renfenotifier.core.network.FakeRenfeApi
import com.pablovb019.renfenotifier.core.network.model.FollowUpListOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class FollowUpsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var api: FakeRenfeApi

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api = FakeRenfeApi()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun followUp(id: String, lifecycle: String = "active") = FollowUpOut(
        followupId = id,
        originCode = "MADCH",
        originName = null,
        destinationCode = "BARBE",
        destinationName = null,
        travelDate = "2026-09-13",
        mode = "first",
        plazaH = false,
        specificTrainId = null,
        lifecycle = lifecycle,
        availability = "unknown",
        alertState = "idle",
        episode = 0,
        expiresAt = "2026-09-15T00:00:00+00:00",
    )

    @Test
    fun `carga el listado con filtro por defecto todos`() = runTest(dispatcher) {
        api.listFollowUpsHandler = { lifecycle ->
            assertEquals(null, lifecycle)
            FollowUpListOut(items = listOf(followUp("fu-1"), followUp("fu-2")), total = 2)
        }

        val vm = FollowUpsViewModel(api)
        vm.load()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.items.size)
        assertEquals(2, state.total)
        assertFalse(state.loading)
        assertNull(state.error)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `cambiar el filtro recarga consultando el estado de ciclo de vida`() = runTest(dispatcher) {
        var queriedLifecycle: String? = "dummy"
        api.listFollowUpsHandler = { lifecycle ->
            queriedLifecycle = lifecycle
            FollowUpListOut(items = emptyList(), total = 0)
        }

        val vm = FollowUpsViewModel(api)
        vm.onFilterSelected(LifecycleFilter.PAUSED)
        advanceUntilIdle()

        assertEquals("paused", queriedLifecycle)
        assertEquals(LifecycleFilter.PAUSED, vm.uiState.value.filter)
    }

    @Test
    fun `listado vacio es estado vacio y no error`() = runTest(dispatcher) {
        api.listFollowUpsHandler = {
            FollowUpListOut(items = emptyList(), total = 0)
        }

        val vm = FollowUpsViewModel(api)
        vm.load()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `un 503 se muestra como error y no como listado vacio`() = runTest(dispatcher) {
        api.listFollowUpsHandler = {
            throw HttpException(
                Response.error<Any>(503, "caido".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = FollowUpsViewModel(api)
        vm.load()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.error!!.contains("Renfe"))
        assertTrue(state.items.isEmpty())
        assertFalse(state.isEmpty)
    }

    @Test
    fun `error de red se muestra como error de conexion`() = runTest(dispatcher) {
        api.listFollowUpsHandler = { throw IOException("sin red") }

        val vm = FollowUpsViewModel(api)
        vm.load()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.error!!.contains("conexión"))
    }
}