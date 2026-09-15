package com.pablovb019.renfenotifier.core.notifications

import android.content.Context

/**
 * Deduplicación persistente de `event_id` de notificaciones.
 *
 * FCM entrega *al menos una vez* y la cola de avisos del backend reintenta
 * dentro de su ventana: la app debe descartar duplicados del mismo evento sin
 * importar reinicios de proceso. [EventIdStore] recuerda los identificadores
 * ya mostrados (con tope de entradas y de antigüedad).
 */
interface EventIdStore {
    fun contains(eventId: String): Boolean
    fun remember(eventId: String)
}

/**
 * Lógica pura del registro de eventos (sin Android) para poder probarla en JVM:
 * conserva identificadores, poda por antigüedad y respeta un tope de entradas.
 */
class EventLedger(
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val records = LinkedHashMap<String, Long>()

    fun contains(eventId: String): Boolean = records.containsKey(eventId)

    fun remember(eventId: String, nowEpochMillis: Long, maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS) {
        records[eventId] = nowEpochMillis
        prune(nowEpochMillis, maxAgeMillis)
        if (records.size > capacity) {
            val iterator = records.iterator()
            repeat(records.size - capacity) { iterator.next(); iterator.remove() }
        }
    }

    /** Elimina registros más antiguos que [maxAgeMillis]; devuelve cuántos borró. */
    fun prune(nowEpochMillis: Long, maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS): Int {
        val cutoff = nowEpochMillis - maxAgeMillis
        val it = records.iterator()
        var removed = 0
        while (it.hasNext()) {
            val (_, timestamp) = it.next()
            if (timestamp < cutoff) {
                it.remove()
                removed++
            }
        }
        return removed
    }

    fun snapshot(): List<Pair<String, Long>> = records.entries.map { it.key to it.value }

    companion object {
        const val DEFAULT_CAPACITY = 500
        const val DEFAULT_MAX_AGE_MILLIS = 30L * 24L * 60L * 60L * 1_000L

        fun restore(entries: List<Pair<String, Long>>, capacity: Int = DEFAULT_CAPACITY): EventLedger {
            val ledger = EventLedger(capacity)
            entries.forEach { (id, timestamp) -> ledger.records[id] = timestamp }
            return ledger
        }
    }
}

/** Implementación real sobre SharedPreferences (persistente entre reinicios). */
class PrefsEventIdStore(context: Context) : EventIdStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val ledger: EventLedger by lazy {
        EventLedger.restore(deserialize(prefs.getString(KEY_LEDGER, "").orEmpty()))
    }

    override fun contains(eventId: String): Boolean = ledger.contains(eventId)

    override fun remember(eventId: String) {
        synchronized(lock) {
            ledger.remember(eventId, System.currentTimeMillis())
            prefs.edit().putString(KEY_LEDGER, serialize(ledger.snapshot())).apply()
        }
    }

    private fun serialize(entries: List<Pair<String, Long>>): String =
        entries.joinToString("\n") { (id, timestamp) -> "$id|$timestamp" }

    private fun deserialize(raw: String): List<Pair<String, Long>> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('|')
                if (separator <= 0) return@mapNotNull null
                val timestamp = line.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
                line.substring(0, separator) to timestamp
            }
            .toList()
    }

    private companion object {
        val lock = Any()
        const val PREFS_NAME = "delivered_event_ids"
        const val KEY_LEDGER = "ledger"
    }
}