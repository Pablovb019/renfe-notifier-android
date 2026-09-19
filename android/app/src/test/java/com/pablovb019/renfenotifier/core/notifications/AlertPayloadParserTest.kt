package com.pablovb019.renfenotifier.core.notifications

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPayloadParserTest {

    private fun testPayload(overrides: Map<String, String> = emptyMap()): Map<String, String> {
        val base = mapOf(
            "type" to "alert",
            "event_id" to "evt-42",
            "followup_id" to "fu-7",
            "episode_id" to "3",
            "title" to "¡Plazas disponibles!",
            "body" to "Madrid → Barcelona el 13/09",
            "channel_id" to "disponibilidad_plazas_v2",
            "priority" to "high",
            "origin_code" to "60000",
            "origin" to "MADRID (TODAS)",
            "destination_code" to "71801",
            "destination" to "BARCELONA-SANTS",
            "travel_date" to "2026-09-13",
            "departure" to "08:00:00",
            "arrival" to "09:30:00",
            "price" to "42.50",
            "observed_at" to "2026-09-13T06:10:00+00:00",
            "expires_at" to "2026-09-15T00:00:00+00:00",
        )
        return base + overrides
    }

    @Test
    fun `parsea un payload de alerta completo`() {
        val payload = AlertPayloadParser.parse(testPayload())!!

        assertEquals("evt-42", payload.eventId)
        assertEquals("fu-7", payload.followupId)
        assertEquals(3, payload.episodeId)
        assertEquals("¡Plazas disponibles!", payload.title)
        assertTrue(payload.priorityHigh)
        assertEquals("60000", payload.originCode)
        assertEquals("2026-09-13", payload.travelDate)
        assertEquals("42.50", payload.price)
        assertNotNull(payload.observedAt)
        assertNotNull(payload.expiresAt)
        assertFalse(payload.isTest)
    }

    @Test
    fun `payload de prueba del backend es valido y se identifica como test`() {
        val payload = AlertPayloadParser.parse(
            mapOf(
                "type" to "test",
                "event_id" to "test",
                "followup_id" to "-",
                "episode_id" to "0",
                "title" to "Test de conexión",
                "body" to "Si ves esta notificación, la app recibe correctamente los avisos.",
                "channel_id" to "disponibilidad_plazas_v2",
                "priority" to "high",
                "observed_at" to "-",
                "expires_at" to "-",
            ),
        )!!

        assertTrue(payload.isTest)
        assertNull(payload.observedAt)
        assertNull(payload.expiresAt)
    }

    @Test
    fun `sin event_id el payload se descarta`() {
        assertNull(AlertPayloadParser.parse(testPayload(overrides = mapOf("event_id" to " "))))
    }

    @Test
    fun `tipo desconocido se descarta`() {
        assertNull(AlertPayloadParser.parse(testPayload(overrides = mapOf("type" to "otro"))))
    }

    @Test
    fun `sin followup_id se descarta`() {
        assertNull(AlertPayloadParser.parse(testPayload(overrides = mapOf("followup_id" to ""))))
    }

    @Test
    fun `alerta sin episode_id numerico se descarta`() {
        assertNull(AlertPayloadParser.parse(testPayload(overrides = mapOf("episode_id" to "abc"))))
    }

    @Test
    fun `timestamp malformado no rompe el parseo`() {
        val payload = AlertPayloadParser.parse(
            testPayload(overrides = mapOf("observed_at" to "no-es-fecha", "expires_at" to "tampoco")),
        )!!

        assertNull(payload.observedAt)
        assertNull(payload.expiresAt)
    }

    @Test
    fun `aviso observado hace mas de media hora se rechaza como viejo`() {
        val now = Instant.parse("2026-09-13T10:00:00Z")
        val payload = AlertPayloadParser.parse(
            testPayload(overrides = mapOf("observed_at" to "2026-09-13T09:00:00+00:00")),
        )!!

        assertFalse(AlertPayloadParser.isFresh(payload, now))
    }

    @Test
    fun `aviso observado recientemente es fresco`() {
        val now = Instant.parse("2026-09-13T10:00:00Z")
        val payload = AlertPayloadParser.parse(
            testPayload(overrides = mapOf("observed_at" to "2026-09-13T09:50:00+00:00")),
        )!!

        assertTrue(AlertPayloadParser.isFresh(payload, now))
    }

    @Test
    fun `tras la caducidad breve el aviso se rechaza`() {
        val now = Instant.parse("2026-09-15T01:00:00Z")
        val payload = AlertPayloadParser.parse(testPayload())!!

        assertFalse(AlertPayloadParser.isFresh(payload, now))
    }

    @Test
    fun `el payload de prueba sin timestamps siempre es fresco`() {
        val payload = AlertPayloadParser.parse(
            mapOf(
                "type" to "test",
                "event_id" to "test",
                "followup_id" to "-",
                "episode_id" to "0",
                "title" to "Test de conexión",
                "body" to "Texto",
            ),
        )!!

        assertTrue(AlertPayloadParser.isFresh(payload, Instant.now().plus(1, ChronoUnit.DAYS)))
    }
}