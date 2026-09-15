package com.pablovb019.renfenotifier.feature.followups

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formato de fechas según la especificación: los instantes se guardan en UTC
 * pero se muestran a la usuaria en Europe/Madrid; las fechas de viaje ya son
 * expresión local (YYYY-MM-DD).
 */
object MadridFormat {
    private val madrid: ZoneId = ZoneId.of("Europe/Madrid")
    private val dateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(madrid)
    private val date: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Parsea un instante ISO-8601 (con offset) y lo muestra en Europe/Madrid. */
    fun showInstant(iso: String): String =
        Instant.from(OffsetDateTime.parse(iso)).let(dateTime::format)

    /** Parsea y muestra una fecha de viaje local "YYYY-MM-DD". */
    fun showTravelDate(iso: String): String =
        LocalDate.parse(iso).format(date)

    /** Formatea un instante ya resuelto para mostrarlo en Europe/Madrid. */
    fun showInstant(instant: Instant): String = dateTime.format(instant)
}