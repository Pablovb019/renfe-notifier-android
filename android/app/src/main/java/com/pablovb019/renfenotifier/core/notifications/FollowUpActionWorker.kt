package com.pablovb019.renfenotifier.core.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pablovb019.renfenotifier.core.network.ApiModule
import java.time.Duration

/**
 * Ejecuta una acción de notificación (confirmar aviso / pausar) contra el
 * backend autenticado. Trabajo diferido único por (acción, seguimiento):
 * reintenta fallos de red con backoff exponencial y, al agotarlos o ante un
 * error definitivo, muestra una notificación informativa. No hay vigilancia
 * periódica ni polling.
 */
class FollowUpActionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val followupId = inputData.getString(KEY_FOLLOWUP_ID).orEmpty()
        val action = inputData.getString(KEY_ACTION).orEmpty()
        if (followupId.isEmpty() || action.isEmpty()) return Result.failure()

        return try {
            val api = ApiModule.api()
            when (action) {
                ACTION_CONFIRM -> api.acknowledgeFollowUp(followupId)
                ACTION_PAUSE -> api.pauseFollowUp(followupId)
                else -> return Result.failure()
            }
            Result.success()
        } catch (throwable: Throwable) {
            when {
                NetworkPolicy.isIdempotentSuccess(throwable) || NetworkPolicy.isRevocation(throwable) ->
                    // Ya confirmado/pausado o sesión revocada (la UI guía a re-emparejar).
                    Result.success()

                NetworkPolicy.shouldRetry(throwable, runAttemptCount) -> Result.retry()

                else -> {
                    NotificationDisplayer.showActionFailed(applicationContext, followupId)
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val KEY_FOLLOWUP_ID = "followup_id"
        const val KEY_ACTION = "action"
        const val ACTION_CONFIRM = "confirm"
        const val ACTION_PAUSE = "pause"

        fun enqueue(context: Context, action: String, followupId: String) {
            val request = OneTimeWorkRequestBuilder<FollowUpActionWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_FOLLOWUP_ID, followupId)
                        .putString(KEY_ACTION, action)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(15))
                .build()
            // KEEP: si ya hay uno en curso (reintentando) no se duplica la acción.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "followup-action:$action:$followupId",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}