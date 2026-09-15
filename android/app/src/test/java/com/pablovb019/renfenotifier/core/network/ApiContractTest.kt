package com.pablovb019.renfenotifier.core.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.pablovb019.renfenotifier.core.network.model.ClaimRequest
import com.pablovb019.renfenotifier.core.network.model.DiagnosticsOut
import com.pablovb019.renfenotifier.core.network.model.FcmTokenRequest
import com.pablovb019.renfenotifier.core.network.model.HealthResponse
import com.pablovb019.renfenotifier.core.network.model.TestNotificationOut
import com.pablovb019.renfenotifier.core.network.model.TrainSearchRequest
import com.pablovb019.renfenotifier.core.network.model.TrainSearchResponse
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Pruebas de los contratos de la API contra un servidor simulado (MockWebServer,
 * ficheros de datos identificables). Verifica rutas, cuerpos y mapeo
 * snake_case <-> camelCase.
 */
class ApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RenfeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = buildApi(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `health devuelve estado de prueba del servidor simulado`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"ok","uptime_s":12.5}"""),
        )

        val result: HealthResponse = api.health()

        assertEquals("ok", result.status)
        assertEquals(12.5, result.uptimeS, 0.001)
        assertEquals("/health", server.takeRequest().path)
    }

    @Test
    fun `claim empareja y mapea device_token a camelCase`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"device_token":"tok-abc-123","device_id":"dev-1"}
                    """.trimIndent(),
                ),
        )

        val response = api.claimPairing(ClaimRequest(code = "RF-TEST", deviceId = "dev-1", deviceName = "Realme"))

        assertEquals("dev-1", response.deviceId)
        assertEquals("tok-abc-123", response.deviceToken)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/pairing/claim", recorded.path)
        assertEquals("POST", recorded.method)
        val sentBody = recorded.body.readUtf8()
        assertNotNull(sentBody.substringAfter("\"code\":").substringBefore(",").contains("RF-TEST"))
    }

    @Test
    fun `search stations mapea is_group y devuelve resultados de prueba`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [{"name":"Madrid Chamartín","code":"MADCH","is_group":false}]
                    """.trimIndent(),
                ),
        )

        val stations = api.searchStations(query = "madrid", limit = 10)

        assertEquals(1, stations.size)
        assertEquals(false, stations[0].isGroup)
        assertEquals("MADCH", stations[0].code)
        assertEquals("/api/v1/search/stations?q=madrid&limit=10", server.takeRequest().path)
    }

    @Test
    fun `search trains envia body en snake_case y deserializa trains con availability`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "status": "ok",
                      "plaza_h_requested": false,
                      "trains": [
                        {
                          "identifier": "ident-001",
                          "identity": "idt-001",
                          "departure": "08:00",
                          "arrival": "09:30",
                          "price": "15.00",
                          "availability": "available"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result: TrainSearchResponse = api.searchTrains(
            TrainSearchRequest(
                originCode = "MADCH",
                destinationCode = "BARBE",
                travelDate = "2026-09-13",
                plazaH = false,
            ),
        )

        assertEquals("ok", result.status)
        assertFalse(result.plazaHRequested)
        assertEquals(1, result.trains.size)
        assertEquals("idt-001", result.trains[0].identity)
        assertEquals("15.00", result.trains[0].price)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/search/trains", recorded.path)
        assertEquals("POST", recorded.method)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"origin_code\""))
        assertTrue(body.contains("\"travel_date\""))
    }

    @Test
    fun `create follow up acepta mode y plaza_h y devuelve followup_id`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "followup_id": "fu-abc-123",
                      "origin_code": "MADCH",
                      "destination_code": "BARBE",
                      "travel_date": "2026-09-13",
                      "mode": "first",
                      "plaza_h": true,
                      "lifecycle": "pending",
                      "availability": "unknown",
                      "alert_state": "inactive",
                      "episode": 0,
                      "expires_at": "2026-09-15T00:00:00"
                    }
                    """.trimIndent(),
                ),
        )

        val result = api.createFollowUp(
            com.pablovb019.renfenotifier.core.network.model.FollowUpCreateRequest(
                originCode = "MADCH",
                destinationCode = "BARBE",
                travelDate = "2026-09-13",
                mode = "first",
                plazaH = true,
            ),
        )

        assertEquals("fu-abc-123", result.followupId)
        assertEquals("MADCH", result.originCode)
        assertEquals(true, result.plazaH)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/followups", recorded.path)
        assertEquals("POST", recorded.method)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"mode\""))
        assertTrue(body.contains("\"plaza_h\""))
        assertTrue(body.contains("\"travel_date\""))
    }

    @Test
    fun `list followups envia filtro de ciclo de vida y mapea el listado`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "items": [{
                        "followup_id": "fu-1",
                        "origin_code": "MADCH",
                        "destination_code": "BARBE",
                        "travel_date": "2026-09-13",
                        "mode": "specific",
                        "plaza_h": false,
                        "specific_train_id": "real:123",
                        "lifecycle": "active",
                        "availability": "unknown",
                        "alert_state": "idle",
                        "episode": 0,
                        "expires_at": "2026-09-15T00:00:00+00:00"
                      }],
                      "total": 1
                    }
                    """.trimIndent(),
                ),
        )

        val result = api.listFollowUps(lifecycle = "active")

        assertEquals(1, result.total)
        assertEquals("fu-1", result.items[0].followupId)
        assertEquals("specific", result.items[0].mode)
        assertEquals("/api/v1/followups?lifecycle=active", server.takeRequest().path)
    }

    @Test
    fun `detail de follow up devuelve episodios con observed_at`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "followup_id": "fu-1",
                      "origin_code": "MADCH",
                      "destination_code": "BARBE",
                      "travel_date": "2026-09-13",
                      "mode": "first",
                      "plaza_h": false,
                      "specific_train_id": null,
                      "lifecycle": "active",
                      "availability": "available",
                      "alert_state": "pending_alert",
                      "episode": 2,
                      "expires_at": "2026-09-15T00:00:00+00:00",
                      "episodes": [
                        {
                          "episode_id": 10,
                          "episode": 1,
                          "observed_at": "2026-09-13T06:10:00+00:00",
                          "train_ids": ["real:111"]
                        },
                        {
                          "episode_id": 11,
                          "episode": 2,
                          "observed_at": "2026-09-13T07:05:00+00:00",
                          "train_ids": ["real:222"]
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = api.getFollowUp("fu-1")

        assertEquals("pending_alert", result.alertState)
        assertEquals(2, result.episodes.size)
        assertEquals(2, result.episodes.last().episode)
        assertEquals(listOf("real:222"), result.episodes.last().trainIds)
        assertEquals("/api/v1/followups/fu-1", server.takeRequest().path)
    }

    @Test
    fun `acciones de follow up usan sus rutas y devuelven estado actualizado`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"followup_id":"fu-1","origin_code":"MADCH","destination_code":"BARBE",
                     "travel_date":"2026-09-13","mode":"first","plaza_h":false,
                     "specific_train_id":null,"lifecycle":"paused","availability":"unknown",
                     "alert_state":"idle","episode":0,"expires_at":"2026-09-15T00:00:00+00:00"}
                    """.trimIndent(),
                ),
        )

        val result = api.pauseFollowUp("fu-1")

        assertEquals("paused", result.lifecycle)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/followups/fu-1/pause", recorded.path)
        assertEquals("POST", recorded.method)
    }

    @Test
    fun `delete de follow up responde 204 y devuelve unidad`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204).setBody(""))

        api.deleteFollowUp("fu-1")

        val recorded = server.takeRequest()
        assertEquals("/api/v1/followups/fu-1", recorded.path)
        assertEquals("DELETE", recorded.method)
    }

    @Test
    fun `registro de token FCM usa PUT y cuerpo snake_case`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"registered": true}"""),
        )

        val result = api.registerFcmToken(FcmTokenRequest(fcmToken = "abc:123:XYZ_456"))

        assertTrue(result.registered)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/fcm/token", recorded.path)
        assertEquals("PUT", recorded.method)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"fcm_token\""))
        assertTrue(body.contains("abc:123:XYZ_456"))
    }

    @Test
    fun `registro de token FCM no activo responde 409 sin marcar exito`() {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail": "Dispositivo no activo"}"""))

        runBlocking {
            assertThrows(HttpException::class.java) {
                runBlocking { api.registerFcmToken(FcmTokenRequest(fcmToken = "abc:123:XYZ_456")) }
            }
        }
    }

    @Test
    fun `diagnostics muestra cifras y search_stats mapeados a camelCase`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "status": "ok",
                      "app_version": "0.1.0",
                      "db_ok": true,
                      "devices": 1,
                      "followups": {"active": 2, "paused": 0, "expired": 0, "deleted": 1, "total": 3},
                      "episodes": 4,
                      "alerts_pending": 0,
                      "search_stats": {"logical_queries": 7, "http_requests": 35, "bytes_received": 12000}
                    }
                    """.trimIndent(),
                ),
        )

        val result: DiagnosticsOut = api.diagnostics()

        assertEquals("ok", result.status)
        assertEquals("0.1.0", result.appVersion)
        assertEquals(true, result.dbOk)
        assertEquals(2, result.followups.active)
        assertEquals(4, result.episodes)
        assertEquals(7L, result.searchStats.logicalQueries)
        assertEquals(35L, result.searchStats.httpRequests)
        assertEquals(12000L, result.searchStats.bytesReceived)
        assertEquals("/api/v1/diagnostics", server.takeRequest().path)
    }

    @Test
    fun `notificar prueba POST devuelve message_id`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"message_id": "msg-0001"}"""),
        )

        val result: TestNotificationOut = api.sendTestNotification()

        assertEquals("msg-0001", result.messageId)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/diagnostics/test-notification", recorded.path)
        assertEquals("POST", recorded.method)
    }

    private fun buildApi(baseUrl: String): RenfeApi {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RenfeApi::class.java)
    }
}