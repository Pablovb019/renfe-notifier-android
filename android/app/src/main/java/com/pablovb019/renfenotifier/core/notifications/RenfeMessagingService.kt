package com.pablovb019.renfenotifier.core.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Entrada push de FCM (mensajes *data-only*, contrato del backend):
 * - en primer plano y en segundo plano se muestra la notificación nativa por el
 *   mismo camino, con deduplicación persistente por `event_id` y rechazo de
 *   eventos caducados o demasiado antiguos (sin duplicados).
 * - [onNewToken] encola el registro/renovación del token frente al backend.
 *
 * Con FCM todavía sin configurar (paso 33) la app funciona igual: cada acceso a
 * Firebase va protegido y el registro se re-encola al configurarlo o al
 * emparejar.
 */
class RenfeMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (!FcmTokenGateway.isConfigured(this)) {
            // google-services.json / proyecto Firebase pendiente: no hay nada que escuchar.
            Log.i(TAG, "Firebase no configurado todavía; FCM real pendiente (paso 33).")
            return
        }
        NotificationChannels.create(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FcmTokenGateway.isConfigured(this) && token.isNotBlank()) {
            FcmTokenRegistrationWorker.enqueue(this)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!FcmTokenGateway.isConfigured(this)) return
        scope.launch { FcmTokenGateway.onMessage(this@RenfeMessagingService, message) }
    }

    private companion object {
        const val TAG = "RenfeMessagingService"
    }
}