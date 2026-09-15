package com.pablovb019.renfenotifier.core.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.time.Instant
import kotlinx.coroutines.tasks.await

/**
 * Puente con Firebase Cloud Messaging. Guarda cada acceso por si aún no hay
 * configuración (google-services.json / proyecto Firebase pendiente, ver paso 33):
 * así la app arranca y funciona sin FCM, y todo se activa al configurarlo.
 */
object FcmTokenGateway {

    fun isConfigured(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    /** Permite al arranque conectar el ajuste de avisos sin acoplar FCM a DataStore. */
    suspend fun alertsEnabled(): Boolean = alertsEnabledProvider?.invoke() ?: true
    var alertsEnabledProvider: (suspend () -> Boolean)? = null

    suspend fun obtainToken(): String? =
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()

    suspend fun onMessage(context: Context, message: RemoteMessage) {
        if (!alertsEnabled()) return // avisos desactivados: se silencia sin marcar como entregado
        val payload = AlertPayloadParser.parse(message.data)
        if (payload == null || !AlertPayloadParser.isFresh(payload, Instant.now())) {
            // Firma fuera de contrato, caducada o demasiado antigua: nunca se muestra.
            return
        }
        val store = PrefsEventIdStore(context)
        synchronized(lock) {
            if (store.contains(payload.eventId)) return // duplicado (entrega "al menos una vez")
            store.remember(payload.eventId)
        }
        NotificationDisplayer.showAlert(context, payload)
    }

    private val lock = Any()
}