package com.pablovb019.renfenotifier.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receptor de las acciones de las notificaciones ("Confirmar aviso" y "Pausar").
 * Encola el trabajo diferido autenticado ([FollowUpActionWorker]) y acaba; no
 * ejecuta red aquí (los fallos de red los gestiona el trabajador con reintentos
 * acotados).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            ACTION_CONFIRM -> FollowUpActionWorker.ACTION_CONFIRM
            ACTION_PAUSE -> FollowUpActionWorker.ACTION_PAUSE
            else -> return
        }
        val followupId = intent.getStringExtra(NotificationDisplayer.EXTRA_FOLLOWUP_ID).orEmpty()
        if (followupId.isEmpty()) return
        Log.i(TAG, "Acción de notificación $action sobre $followupId")
        FollowUpActionWorker.enqueue(context, action, followupId)
    }

    companion object {
        const val ACTION_CONFIRM = "com.pablovb019.renfenotifier.action.CONFIRM"
        const val ACTION_PAUSE = "com.pablovb019.renfenotifier.action.PAUSE"
        private const val TAG = "NotificationActionReceiver"
    }
}