package com.pablovb019.renfenotifier.feature.search

import com.pablovb019.renfenotifier.core.model.FollowUpMode
import com.pablovb019.renfenotifier.core.network.FakeRenfeApi
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.core.network.model.StationOut
import com.pablovb019.renfenotifier.core.network.model.TrainOut
import com.pablovb019.renfenotifier.core.network.model.TrainSearchRequest
import com.pablovb019.renfenotifier.core.network.model.TrainSearchResponse
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val fixedNow: LocalDate = LocalDate.of(2026, 9, 13)
    private val horizonDate: LocalDate = fixedNow.plusDays(62)

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

    private fun viewModel(debounceMillis: Long = 0) = SearchViewModel(
        api = api,
        now = { fixedNow },
        suggestionsDebounceMillis = debounceMillis,
    )

    private val chamartin = StationOut(name = "Madrid Chamartín", code = "CHAM", isGroup = false)
    private val atocha = StationOut(name = "Madrid Atocha Cercanías", code = "ATOC", isGroup = false)

    private fun train(
        identity: String,
        arrival: String? = "09:30",
        price: String? = "12.5",
        availability: String = "no_availability",
    ) = TrainOut(
        identifier = "id-$identity",
        identity = identity,
        departure = "08:00",
        arrival = arrival,
        price = price,
        availability = availability,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sugerencias de origen se cargan tras el debounce`() = runTest(dispatcher) {
        api.searchStationsHandler = { query, _ ->
            listOf(StationOut(name = "Madrid Chamartín", code = "CHAM", isGroup = false))
        }

        val vm = viewModel(debounceMillis = 1000)
        vm.onOriginQueryChange("  madrid  ")
        runCurrent()
        assertTrue("debe esperar al debounce", vm.uiState.value.originSuggestions.isEmpty())

        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.originSuggestions.size)
        assertEquals("CHAM", vm.uiState.value.originSuggestions[0].code)
    }

    @Test
    fun `seleccionar estacion fija su codigo y nombre en el formulario`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onOriginQueryChange("chamartin")
        advanceUntilIdle()
        vm.onOriginSelected(chamartin)

        assertEquals("CHAM", vm.uiState.value.selectedOrigin?.code)
        assertEquals(chamartin.name, vm.uiState.value.originQuery)
    }

    @Test
    fun `busqueda exitosa pide al backend y expone los trenes`() = runTest(dispatcher) {
        api.searchTrainsHandler = { request ->
            assertEquals("CHAM", request.originCode)
            assertEquals("ATOC", request.destinationCode)
            assertEquals(fixedNow.toString(), request.travelDate)
            TrainSearchResponse(status = "ok", plazaHRequested = false, trains = listOf(train("t1")))
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.trains.isNotEmpty())
        assertEquals("t1", state.trains[0].identity)
        assertNull(state.searchError)
        assertEquals(false, state.isSearching)
    }

    @Test
    fun `sin estaciones seleccionadas muestra error de validacion y no consulta`() = runTest(dispatcher) {
        var called = false
        api.searchTrainsHandler = {
            called = true
            TrainSearchResponse(status = "ok", plazaHRequested = false, trains = listOf(train("t1")))
        }

        val vm = viewModel()
        vm.search()
        advanceUntilIdle()

        assertTrue(called == false)
        assertTrue(vm.uiState.value.searchError!!.contains("Selecciona"))
        assertTrue(vm.uiState.value.trains.isEmpty())
    }

    @Test
    fun `origen igual que destino muestra error y no consulta`() = runTest(dispatcher) {
        var called = false
        api.searchTrainsHandler = {
            called = true
            TrainSearchResponse(status = "ok", plazaHRequested = false, trains = emptyList())
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(chamartin)
        vm.search()
        advanceUntilIdle()

        assertTrue(called == false)
        assertTrue(vm.uiState.value.searchError!!.contains("coincidir"))
    }

    @Test
    fun `fecha anterior a hoy muestra error de fecha`() = runTest(dispatcher) {
        api.searchTrainsHandler = {
            TrainSearchResponse(status = "ok", plazaHRequested = false, trains = emptyList())
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.onDateSelected(fixedNow.minusDays(1))
        vm.search()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchError!!.contains("fecha"))
    }

    @Test
    fun `un 503 se muestra como error y no como ausencia de plazas`() = runTest(dispatcher) {
        api.searchTrainsHandler = {
            throw HttpException(
                Response.error<Any>(503, "renfe caido".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.searchError!!.contains("Renfe"))
        assertTrue(state.trains.isEmpty())
        assertEquals(false, state.isEmptyResult)
    }

    @Test
    fun `error de red se muestra como error de conexion`() = runTest(dispatcher) {
        api.searchTrainsHandler = { throw IOException("sin red") }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchError!!.contains("conexión"))
    }

    @Test
    fun `trenes vacios y estado ok es estado vacio y no error`() = runTest(dispatcher) {
        api.searchTrainsHandler = {
            TrainSearchResponse(status = "no_trains", plazaHRequested = false, trains = emptyList())
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.searchError)
        assertTrue(state.isEmptyResult)
    }

    @Test
    fun `crear seguimiento reutiliza ruta fecha y modo selected`() = runTest(dispatcher) {
        api.searchTrainsHandler = {
            TrainSearchResponse(status = "ok", plazaHRequested = false, trains = listOf(train("t-av-7")))
        }
        api.createFollowUpHandler = { request ->
            assertEquals("CHAM", request.originCode)
            assertEquals("ATOC", request.destinationCode)
            assertEquals(fixedNow.toString(), request.travelDate)
            assertEquals("specific", request.mode)
            assertEquals("t-av-7", request.specificTrainId)
            FollowUpOut(
                followupId = "fu-1",
                originCode = "CHAM",
                originName = null,
                destinationCode = "ATOC",
                destinationName = null,
                travelDate = fixedNow.toString(),
                mode = "specific",
                plazaH = true,
                specificTrainId = "t-av-7",
                lifecycle = "pending",
                availability = "unknown",
                alertState = "inactive",
                episode = 0,
                expiresAt = fixedNow.toString(),
            )
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.onPlazaHChange(true)
        vm.search()
        advanceUntilIdle()
        vm.onTrainSelected("t-av-7")
        assertTrue(vm.uiState.value.mode == FollowUpMode.SPECIFIC)

        vm.createFollowUp()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("fu-1", state.createdFollowUpId)
        assertNull(state.followUpError)
        assertEquals("CHAM", state.selectedOrigin?.code)
        assertTrue("la fecha debe reutilizarse", state.travelDate == fixedNow)
    }

    @Test
    fun `modo specific sin tren elegido pide seleccionar tren y no crea`() = runTest(dispatcher) {
        api.createFollowUpHandler = {
            FollowUpOut(
                followupId = "fu-x",
                originCode = "CHAM",
                originName = null,
                destinationCode = "ATOC",
                destinationName = null,
                travelDate = fixedNow.toString(),
                mode = "specific",
                plazaH = false,
                specificTrainId = null,
                lifecycle = "pending",
                availability = "unknown",
                alertState = "inactive",
                episode = 0,
                expiresAt = fixedNow.toString(),
            )
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.onModeSelected(FollowUpMode.SPECIFIC)
        vm.createFollowUp()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(true, state.mode == FollowUpMode.SPECIFIC)
        assertTrue(state.followUpError!!.contains("tren"))
        assertNull(state.createdFollowUpId)
    }

    @Test
    fun `tren disponible en modo specific no crea seguimiento y avisa`() = runTest(dispatcher) {
        var createCalled = false
        api.searchTrainsHandler = {
            TrainSearchResponse(
                status = "ok",
                plazaHRequested = false,
                trains = listOf(train("t-dis", availability = "available")),
            )
        }
        api.createFollowUpHandler = {
            createCalled = true
            throw AssertionError("no debe crear seguimiento si el tren tiene plazas")
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()
        vm.onTrainSelected("t-dis")
        vm.createFollowUp()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("no debe llamar a la API de creación", !createCalled)
        assertTrue(state.availableTrainNotice)
        assertNull(state.createdFollowUpId)

        vm.onCreatedAccepted()
        assertTrue("al aceptar debe cerrarse el aviso", !vm.uiState.value.availableTrainNotice)
    }

    @Test
    fun `tren disponible en modo first no crea seguimiento y avisa`() = runTest(dispatcher) {
        var createCalled = false
        api.searchTrainsHandler = {
            TrainSearchResponse(
                status = "ok",
                plazaHRequested = false,
                trains = listOf(
                    train("t-1", availability = "no_availability"),
                    train("t-2", arrival = "10:30", availability = "available"),
                ),
            )
        }
        api.createFollowUpHandler = {
            createCalled = true
            throw AssertionError("no debe crear seguimiento si hay un tren con plazas")
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()
        vm.onModeSelected(FollowUpMode.FIRST)
        vm.createFollowUp()
        advanceUntilIdle()

        assertTrue(!createCalled)
        assertTrue(vm.uiState.value.availableTrainNotice)
    }

    @Test
    fun `modo first sin trenes disponibles si crea seguimiento`() = runTest(dispatcher) {
        api.searchTrainsHandler = {
            TrainSearchResponse(
                status = "ok",
                plazaHRequested = false,
                trains = listOf(
                    train("t-1"),
                    train("t-2", arrival = "10:30"),
                ),
            )
        }
        api.createFollowUpHandler = { request ->
            assertEquals("first", request.mode)
            FollowUpOut(
                followupId = "fu-first",
                originCode = "CHAM",
                originName = null,
                destinationCode = "ATOC",
                destinationName = null,
                travelDate = fixedNow.toString(),
                mode = "first",
                plazaH = false,
                specificTrainId = null,
                lifecycle = "pending",
                availability = "unknown",
                alertState = "inactive",
                episode = 0,
                expiresAt = fixedNow.toString(),
            )
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.search()
        advanceUntilIdle()
        vm.onModeSelected(FollowUpMode.FIRST)
        vm.createFollowUp()
        advanceUntilIdle()

        assertEquals("fu-first", vm.uiState.value.createdFollowUpId)
        assertTrue(!vm.uiState.value.availableTrainNotice)
    }

    @Test
    fun `un 409 al crear seguimiento se muestra como error y no como exito`() = runTest(dispatcher) {
        api.createFollowUpHandler = {
            throw HttpException(
                Response.error<Any>(409, "estado invalido".toResponseBody("text/plain".toMediaType())),
            )
        }

        val vm = viewModel()
        vm.onOriginSelected(chamartin)
        vm.onDestinationSelected(atocha)
        vm.createFollowUp()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.followUpError!!.contains("no admite"))
        assertNull(state.createdFollowUpId)
    }
}