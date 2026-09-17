package com.pablovb019.renfenotifier.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import com.pablovb019.renfenotifier.R

/**
 * Canales de notificación del contrato FCM (ver `docs/notificaciones-fcm.md`):
 * - [CHANNEL_ALERT]: avisos de plazas, urgente y visible (sonido y vibración).
 * - [CHANNEL_SERVICE]: resúmenes y servicio (baja prioridad, silencioso).
 * El usuario puede gestionar sonido/vibración desde los ajustes del sistema.
 */
object NotificationChannels {

    const val CHANNEL_ALERT = "disponibilidad_plazas"
    const val CHANNEL_SERVICE = "resumen_y_servicio"

    fun create(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Migración: los atributos de sonido de un canal son inmutables. Si el
        // canal de avisos quedó creado con uso de alarma (sonaba aunque el
        // sistema estuviera en silencio o vibración), hay que borrarlo para
        // poder recrearlo con uso de notificación.
        val existing = manager.getNotificationChannel(CHANNEL_ALERT)
        if (existing?.audioAttributes?.usage == AudioAttributes.USAGE_ALARM) {
            manager.deleteNotificationChannel(CHANNEL_ALERT)
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                context.getString(R.string.notif_channel_alert),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_alert_desc)
                enableVibration(true)
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.notif_channel_service),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_service_desc)
                enableVibration(false)
                setSound(null, null)
            },
        )
    }

    /** Abre los ajustes de notificaciones de la app (denegación de POST_NOTIFICATIONS). */
    fun openSystemSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(intent)
    }
}