package com.pablovb019.renfenotifier.core.notifications

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Payload ``data`` de las notificaciones FCM, contrato acordado con el backend
 * (ver ``docs/notificaciones-fcm.md``). Mensajes *data-only*: la app monta la
 * notificación nativa con toda la información, sin consultas adicionales.
 *
 * La clase es inmutable y sin dependencias de Android para poder probarse en JVM.
 */
data class AlertPayload(
    val type: String,
    val eventId: String,
    val followupId: String,
    val episodeId: Int,
    val title: String,
    val body: String,
    val channelId: String,
    val priorityHigh: Boolean,
    val originCode: String? = null,
    val origin: String? = null,
    val destinationCode: String? = null,
    val destination: String? = null,
    val travelDate: String? = null,
    val departure: String? = null,
    val arrival: String? = null,
    val price: String? = null,
    val observedAt: Instant? = null,
    val expiresAt: Instant? = null,
) {
    val isTest: Boolean get() = type == AlertPayloadKeys.TYPE_TEST
}

object AlertPayloadKeys {
    const val TYPE = "type"
    const val EVENT_ID = "event_id"
    const val FOLLOWUP_ID = "followup_id"
    const val EPISODE_ID = "episode_id"
    const val TITLE = "title"
    const val BODY = "body"
    const val CHANNEL_ID = "channel_id"
    const val PRIORITY = "priority"
    const val ORIGIN_CODE = "origin_code"
    const val ORIGIN = "origin"
    const val DESTINATION_CODE = "destination_code"
    const val DESTINATION = "destination"
    const val TRAVEL_DATE = "travel_date"
    const val DEPARTURE = "departure"
    const val ARRIVAL = "arrival"
    const val PRICE = "price"
    const val OBSERVED_AT = "observed_at"
    const val EXPIRES_AT = "expires_at"

    const val TYPE_ALERT = "alert"
    const val TYPE_TEST = "test"
    const val PRIORITY_HIGH = "high"
    const val CHANNEL_ALERT = "disponibilidad_plazas_v2"
    const val CHANNEL_SERVICE = "resumen_y_servicio"
}

object AlertPayloadParser {

    /** Avisos observados hace más de [STALE_WINDOW] o tras caducar se descartan. */
    val STALE_WINDOW: Duration = Duration.ofMinutes(30)

    /**
     * Convierte el mapa ``data`` de FCM en [AlertPayload]. Devuelve `null` si el
     * mensaje no es un payload válido del contrato (claves obligatorias ausentes,
     * timestamps malformados, tipo desconocido, event_id vacío).
     */
    fun parse(data: Map<String, String>): AlertPayload? {
        val type = data[AlertPayloadKeys.TYPE] ?: return null
        if (type != AlertPayloadKeys.TYPE_ALERT && type != AlertPayloadKeys.TYPE_TEST) return null
        val eventId = data[AlertPayloadKeys.EVENT_ID]?.trim().orEmpty()
        if (eventId.isEmpty()) return null
        val followupId = data[AlertPayloadKeys.FOLLOWUP_ID]?.trim().orEmpty()
        if (followupId.isEmpty()) return null
        val episodeText = data[AlertPayloadKeys.EPISODE_ID]?.trim()
        val episodeId = episodeText?.toIntOrNull()
        if (episodeId == null && type == AlertPayloadKeys.TYPE_ALERT) return null
        val title = data[AlertPayloadKeys.TITLE]?.trim().orEmpty()
        val body = data[AlertPayloadKeys.BODY]?.trim().orEmpty()
        if (title.isEmpty() && body.isEmpty()) return null
        val channelId = data[AlertPayloadKeys.CHANNEL_ID]?.trim()?.takeIf { it.isNotEmpty() }
            ?: AlertPayloadKeys.CHANNEL_ALERT
        val priorityHigh = data[AlertPayloadKeys.PRIORITY] == AlertPayloadKeys.PRIORITY_HIGH
        val observedAt = parseInstantOrNull(data[AlertPayloadKeys.OBSERVED_AT])
        val expiresAt = parseInstantOrNull(data[AlertPayloadKeys.EXPIRES_AT])
        return AlertPayload(
            type = type,
            eventId = eventId,
            followupId = followupId,
            episodeId = episodeId ?: 0,
            title = title,
            body = body,
            channelId = channelId,
            priorityHigh = priorityHigh,
            originCode = data[AlertPayloadKeys.ORIGIN_CODE]?.trim()?.takeIf { it.isNotEmpty() && it != "-" },
            origin = data[AlertPayloadKeys.ORIGIN]?.trim()?.takeIf { it.isNotEmpty() && it != "-" },
            destinationCode = data[AlertPayloadKeys.DESTINATION_CODE]?.trim()?.takeIf { it.isNotEmpty() && it != "-" },
            destination = data[AlertPayloadKeys.DESTINATION]?.trim()?.takeIf { it.isNotEmpty() && it != "-" },
            travelDate = data[AlertPayloadKeys.TRAVEL_DATE]?.trim()?.takeIf { it.isNotEmpty() && it != "-" },
            departure = data[AlertPayloadKeys.DEPARTURE]?.trim()?.takeIf { it.isNotEmpty() },
            arrival = data[AlertPayloadKeys.ARRIVAL]?.trim()?.takeIf { it.isNotEmpty() },
            price = data[AlertPayloadKeys.PRICE]?.trim()?.takeIf { it.isNotEmpty() },
            observedAt = observedAt,
            expiresAt = expiresAt,
        )
    }

    fun isFresh(payload: AlertPayload, now: Instant, staleWindow: Duration = STALE_WINDOW): Boolean {
        val observedAt = payload.observedAt
        if (observedAt != null && now.isAfter(observedAt.plus(staleWindow))) return false
        val expiresAt = payload.expiresAt
        if (expiresAt != null && now.isAfter(expiresAt)) return false
        return true
    }

    private fun parseInstantOrNull(value: String?): Instant? {
        if (value.isNullOrBlank() || value == "-") return null
        return runCatching { Instant.from(OffsetDateTime.parse(value)) }.getOrNull()
    }
}