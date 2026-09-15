package com.pablovb019.renfenotifier.feature.followups

import com.pablovb019.renfenotifier.core.network.FakeRenfeApi
import com.pablovb019.renfenotifier.core.network.model.EpisodeOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
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
class FollowUpDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val followupId = "fu-1"

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

    private fun detail(
        lifecycle: String = "active",
        availability: String = "unknown",
        alertState: String = "idle",
        episodes: List<EpisodeOut> = emptyList(),
    ) = FollowUpDetailOut(
        followupId = followupId,
        originCode = "MADCH",
        originName = null,
        destinationCode = "BARBE",
        destinationName = null,
        travelDate = "2026-09-13",
        mode = "first",
        plazaH = false,
        specificTrainId = null,
        lifecycle = lifecycle,
        availability = availability,
        alertState = alertState,
        episode = episodes.size,
        expiresAt = "2026-09-15T00:00:00+00:00",
        episodes = episodes,
    )

    private fun out(
        lifecycle: String = "active",
        availability: String = "unknown",
        alertState: String = "idle",
    ) = FollowUpOut(
        followupId = followupId,
        originCode = "MADCH",
        originName = null,
        destinationCode = "BARBE",
        destinationName = null,
        travelDate = "2026-09-13",
        mode = "first",
        plazaH = false,
        specificTrainId = null,
        lifecycle = lifecycle,
        availability = availability,
        alertState = alertState,
        episode = 0,
        expiresAt = "2026-09-15T00:00:00+00:00",
    )

    @Test
    fun `carga el detalle y deriva la ultima comprobacion valida del episodio mas reciente`() =
        runTest(dispatcher) {
            api.getFollowUpHandler = {
                detail(
                    availability = "available",
                    alertState = "pending_alert",
                    episodes = listOf(
                        EpisodeOut(episodeId = 1, episode = 1, observedAt = "2026-09-13T06:10:00+00:00", trainIds = listOf("a")),
                        EpisodeOut(episodeId = 2, episode = 2, observedAt = "2026-09-13T07:05:00+00:00", trainIds = listOf("b")),
                    ),
                )
            }

            val vm = FollowUpDetailViewModel(api, followupId)
            vm.load()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.loading)
            assertEquals(2, state.detail?.episodes?.size)
            assertEquals("2026-09-13T07:05:00Z", state.lastValidObservedAt.toString())
        }

    @Test
    fun `detalle 404 se muestra como error y no como exito`() = runTest(dispatcher) {
        api.getFollowUpHandler = {
            throw HttpException(
                Response.error<Any>(404, "no existe".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = FollowUpDetailViewModel(api, followupId)
        vm.load()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.actionError!!.contains("no encontrado"))
        assertNull(vm.uiState.value.detail)
    }

    @Test
    fun `pausar recarga el detalle desde el servidor (fuente de verdad)`() = runTest(dispatcher) {
        var pauseCalls = 0
        api.pauseFollowUpHandler = {
            pauseCalls++
            out(lifecycle = "paused")
        }
        api.getFollowUpHandler = { detail(lifecycle = "paused") }

        val vm = FollowUpDetailViewModel(api, followupId)
        vm.load()
        advanceUntilIdle()
        vm.pause()
        advanceUntilIdle()

        assertEquals(1, pauseCalls)
        assertEquals("paused", vm.uiState.value.detail?.lifecycle)
        assertNull(vm.uiState.value.actionError)
        assertFalse(vm.uiState.value.actionInProgress)
    }

    @Test
    fun `un 409 al pausar muestra error y no marca la accion como hecha`() = runTest(dispatcher) {
        api.getFollowUpHandler = { detail(lifecycle = "active") }
        api.pauseFollowUpHandler = {
            throw HttpException(
                Response.error<Any>(409, "no permite".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = FollowUpDetailViewModel(api, followupId)
        vm.load()
        advanceUntilIdle()
        vm.pause()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.actionError!!.contains("no permite"))
        assertEquals("active", state.detail?.lifecycle)
        assertFalse(state.actionInProgress)
    }

    @Test
    fun `confirmar aviso funciona sin borrar el seguimiento y refresca el detalle`() =
        runTest(dispatcher) {
            var acknowledgeCalls = 0
            api.acknowledgeFollowUpHandler = {
                acknowledgeCalls++
                out(alertState = "acknowledged")
            }
            api.getFollowUpHandler = { detail(alertState = "acknowledged") }

            val vm = FollowUpDetailViewModel(api, followupId)
            vm.load()
            advanceUntilIdle()
            vm.acknowledge()
            advanceUntilIdle()

            assertEquals(1, acknowledgeCalls)
            assertEquals("acknowledged", vm.uiState.value.detail?.alertState)
            assertNull(vm.uiState.value.actionError)
        }

    @Test
    fun `error de red al confirmar muestra error de conexion`() = runTest(dispatcher) {
        api.getFollowUpHandler = { detail() }
        api.acknowledgeFollowUpHandler = { throw IOException("sin red") }

        val vm = FollowUpDetailViewModel(api, followupId)
        vm.load()
        advanceUntilIdle()
        vm.acknowledge()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.actionError!!.contains("conexión"))
        assertFalse(vm.uiState.value.deleted)
    }

    @Test
    fun `eliminar con exito marca borrado y llama a la API una vez`() = runTest(dispatcher) {
        var deleteCalls = 0
        api.getFollowUpHandler = { detail() }
        api.deleteFollowUpHandler = {
            deleteCalls++
        }

        val vm = FollowUpDetailViewModel(api, followupId)
        vm.load()
        advanceUntilIdle()
        vm.delete()
        advanceUntilIdle()

        assertEquals(1, deleteCalls)
        assertTrue(vm.uiState.value.deleted)
        assertNull(vm.uiState.value.actionError)
    }

    @Test
    fun `un 409 al eliminar no marca borrado`() = runTest(dispatcher) {
        api.getFollowUpHandler = { detail() }
        api.deleteFollowUpHandler = {
            throw HttpException(
                Response.error<Any>(409, "no permite".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = FollowUpDetailViewModel(api, followupId)
        vm.load()
        advanceUntilIdle()
        vm.delete()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.deleted)
        assertTrue(vm.uiState.value.actionError!!.contains("no permite"))
    }
}