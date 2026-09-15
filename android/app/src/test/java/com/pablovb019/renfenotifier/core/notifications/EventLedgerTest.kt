package com.pablovb019.renfenotifier.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventLedgerTest {

    @Test
    fun `un evento desconocido no esta registrado`() {
        assertFalse(EventLedger().contains("evt-1"))
    }

    @Test
    fun `recordar un evento lo hace presente`() {
        val ledger = EventLedger()
        ledger.remember("evt-1", nowMillis)
        assertTrue(ledger.contains("evt-1"))
    }

    @Test
    fun `volver a recordar el mismo evento no duplica`() {
        val ledger = EventLedger()
        ledger.remember("evt-1", nowMillis)
        ledger.remember("evt-1", nowMillis + 1_000)
        assertEquals(1, ledger.snapshot().size)
    }

    @Test
    fun `se respeta el tope de entradas descartando las mas antiguas`() {
        val ledger = EventLedger(capacity = 2)
        ledger.remember("evt-1", nowMillis)
        ledger.remember("evt-2", nowMillis + 1_000)
        ledger.remember("evt-3", nowMillis + 2_000)

        assertFalse(ledger.contains("evt-1"))
        assertTrue(ledger.contains("evt-2"))
        assertTrue(ledger.contains("evt-3"))
    }

    @Test
    fun `se podan las entradas mas antiguas que la ventana`() {
        val ledger = EventLedger.restore(
            listOf(
                "viejo" to (nowMillis - 31L * 24 * 60 * 60 * 1_000),
                "reciente" to nowMillis,
            ),
        )

        val removed = ledger.prune(nowMillis)

        assertEquals(1, removed)
        assertFalse(ledger.contains("viejo"))
        assertTrue(ledger.contains("reciente"))
    }

    @Test
    fun `se puede restaurar un ledger persistido`() {
        val ledger = EventLedger.restore(listOf("evt-1" to nowMillis))
        assertTrue(ledger.contains("evt-1"))
        assertFalse(ledger.contains("evt-2"))
    }

    private companion object {
        val nowMillis = 1_700_000_000_000L
    }
}