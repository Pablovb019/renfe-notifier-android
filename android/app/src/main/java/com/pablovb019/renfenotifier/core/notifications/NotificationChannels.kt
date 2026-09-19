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
 * [CHANNEL_ALERT] usa un id versionado (`..._v2`): Android no permite cambiar
 * los atributos de sonido de un canal ya creado y, al borrarlo y recrearlo con
 * el mismo id, el sistema restaura los ajustes bloqueados por el usuario
 * (por eso el id original usaba USAGE_ALARM, que ignoraba vibración/silencio).
 */
object NotificationChannels {

    const val CHANNEL_ALERT = "disponibilidad_plazas_v2"
    const val CHANNEL_SERVICE = "resumen_y_servicio"

    private const val LEGACY_CHANNEL_ALERT = "disponibilidad_plazas"

    fun create(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Migración: eliminar el canal antiguo de avisos (quedó con uso de
        // alarma y no se puede corregir en sistema instalado).
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ALERT) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ALERT)
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