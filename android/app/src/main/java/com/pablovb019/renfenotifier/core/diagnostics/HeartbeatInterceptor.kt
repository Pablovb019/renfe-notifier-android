package com.pablovb019.renfenotifier.core.diagnostics

import java.time.Instant
import okhttp3.Interceptor
import okhttp3.Response

/** Registra la última respuesta 2xx del backend para el panel de diagnóstico. */
class HeartbeatInterceptor(private val store: ServerContactStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) {
            store.recordSuccess(Instant.now().toEpochMilli())
        }
        return response
    }
}