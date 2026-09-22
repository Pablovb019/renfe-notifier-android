package com.pablovb019.renfenotifier.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pablovb019.renfenotifier.MainActivity
import com.pablovb019.renfenotifier.R

/**
 * Muestra las notificaciones nativas a partir del payload data-only, con sus
 * acciones (abrir, confirmar, pausar). Un único camino para primer y segundo
 * plano: la deduplicación por `event_id` la aplica el llamador.
 */
object NotificationDisplayer {

    private const val TAG = "NotificationDisplayer"

    const val EXTRA_FOLLOWUP_ID = "notif_followup_id"

    fun showAlert(context: Context, payload: AlertPayload) {
        val channel = resolveChannel(payload)
        val builder = baseBuilder(context, channel, payload)
            .setAutoCancel(true)
            .setContentIntent(openDetailPendingIntent(context, payload.followupId))
            .setOnlyAlertOnce(true)
        if (!payload.isTest) {
            builder
                .addAction(actionPendingIntent(context, NotificationActionReceiver.ACTION_CONFIRM, payload.followupId, R.string.notif_action_confirm))
                .addAction(actionPendingIntent(context, NotificationActionReceiver.ACTION_PAUSE, payload.followupId, R.string.notif_action_pause))
        }
        notify(context, notificationId(payload.eventId), builder.build())
    }

    /** Notificación informativa de fallo final de una acción (confirmar/pausar). */
    fun showActionFailed(context: Context, followupId: String) {
        val finalBuilder = notificationCompat(
            context,
            NotificationChannels.CHANNEL_SERVICE,
            context.getString(R.string.notif_action_failed_title),
            context.getString(R.string.notif_action_failed_body, followupId),
        )
            .setAutoCancel(true)
            .setContentIntent(pendingIntentToHome(context))
        notify(context, notificationId("action-failed-$followupId"), finalBuilder.build())
    }

    private fun notificationCompat(
        context: Context,
        channelId: String,
        title: CharSequence,
        body: CharSequence,
    ) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_stat_train)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))

    private fun baseBuilder(
        context: Context,
        channelId: String,
        payload: AlertPayload,
    ): NotificationCompat.Builder = notificationCompat(context, channelId, payload.title, payload.body)

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (exception: SecurityException) {
            // POST_NOTIFICATIONS denegado (Android 13+): la pantalla de inicio
            // ofrece el enlace a ajustes; aquí no podemos mostrar nada.
            Log.w(TAG, "Permiso de notificaciones denegado: $exception")
        }
    }

    private fun openDetailPendingIntent(context: Context, followupId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_FOLLOWUP_ID, followupId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntentToHome(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN_HOME,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionPendingIntent(
        context: Context,
        action: String,
        followupId: String,
        labelRes: Int,
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_FOLLOWUP_ID, followupId)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCodeFor(action),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, context.getString(labelRes), pending)
    }

    private fun resolveChannel(payload: AlertPayload): String =
        if (payload.channelId == NotificationChannels.CHANNEL_SERVICE) {
            NotificationChannels.CHANNEL_SERVICE
        } else {
            NotificationChannels.CHANNEL_ALERT
        }

    private fun notificationId(eventId: String): Int = eventId.hashCode() and 0x7fffffff

    private fun requestCodeFor(action: String): Int = action.hashCode()

        private const val REQUEST_OPEN = 1001
        private const val REQUEST_OPEN_HOME = 1002
    }