package com.pablovb019.renfenotifier.core.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.network.model.FcmTokenRequest
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import java.time.Duration
import kotlinx.coroutines.flow.first

/**
 * Registra o renueva el token FCM del dispositivo en el backend
 * (`PUT /api/v1/fcm/token`). Trabajo diferido de una sola ejecución (nada de
 * vigilancia periódica): se encola al iniciar la app estando emparejado, al
 * emparejar y en cada renovación del token ([RenfeMessagingService]).
 */
class FcmTokenRegistrationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!FcmTokenGateway.isConfigured(applicationContext)) {
            // FCM real aún no configurado (paso 33): nada que registrar.
            return Result.success()
        }
        val session = PreferencesRepository(applicationContext)
        if (!session.isPaired.first()) {
            // Sin emparejar no hay credencial que usen los endpoints autenticados.
            return Result.success()
        }
        val token = FcmTokenGateway.obtainToken()
        if (token.isNullOrEmpty()) {
            return if (runAttemptCount < NetworkPolicy.MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
        return try {
            ApiModule.api().registerFcmToken(FcmTokenRequest(fcmToken = token))
            Result.success()
        } catch (throwable: Throwable) {
            if (NetworkPolicy.isRevocation(throwable) || NetworkPolicy.isIdempotentSuccess(throwable)) {
                Result.success() // el interceptor/UI ya gestionan el re-emparejamiento
            } else if (NetworkPolicy.shouldRetry(throwable, runAttemptCount)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val UNIQUE_NAME = "fcm-token-registration"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<FcmTokenRegistrationWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
                .build()
            // REPLACE: un token recién emitido invalida el intento previo.
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}