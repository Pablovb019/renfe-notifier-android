package com.pablovb019.renfenotifier.core.diagnostics

import android.content.Context
import java.util.concurrent.atomic.AtomicLong

/** Última respuesta 2xx del backend (epoch ms), persistida en SharedPreferences. */
class ServerContactStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mirror = AtomicLong(prefs.getLong(KEY_LAST_SUCCESS_AT, 0L))

    fun lastSuccessAt(): Long? = mirror.get().takeIf { it > 0L }

    fun recordSuccess(epochMs: Long) {
        mirror.set(epochMs)
        prefs.edit().putLong(KEY_LAST_SUCCESS_AT, epochMs).apply()
    }

    private companion object {
        const val PREFS_NAME = "server_contact"
        const val KEY_LAST_SUCCESS_AT = "last_success_at"
    }
}