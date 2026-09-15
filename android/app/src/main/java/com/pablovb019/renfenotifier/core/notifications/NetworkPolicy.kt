package com.pablovb019.renfenotifier.core.notifications

import java.io.IOException
import retrofit2.HttpException

/**
 * Política de reintentos de los trabajos de red (registro del token FCM y
 * acciones de notificación). Pura y sin dependencias de Android para probarla
 * en JVM. Los reintentos los acota WorkManager (backoff exponencial) y aquí se
 * decide si conviene reintentar o tratar el resultado como definitivo.
 */
object NetworkPolicy {

    const val MAX_ATTEMPTS = 3

    /** Errores que merecen reintento (agotados en [shouldRetry] por intento). */
    fun isRetryable(throwable: Throwable): Boolean =
        throwable is IOException ||
            (throwable is HttpException && (throwable.code() == 429 || throwable.code() >= 500))

    /** Resultados idempotentes: la acción ya está en el estado deseado y no hay que reintentarla. */
    fun isIdempotentSuccess(throwable: Throwable): Boolean =
        throwable is HttpException && (throwable.code() == 404 || throwable.code() == 409)

    /** Credencial revocada: el interceptor ya limpió el token; hay que re-emparejar. */
    fun isRevocation(throwable: Throwable): Boolean =
        throwable is HttpException && throwable.code() == 401

    fun shouldRetry(throwable: Throwable, runAttemptCount: Int): Boolean =
        isRetryable(throwable) && runAttemptCount < MAX_ATTEMPTS
}