package com.pablovb019.renfenotifier.core.diagnostics

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.pablovb019.renfenotifier.core.notifications.FcmTokenGateway
import com.pablovb019.renfenotifier.core.notifications.NotificationChannels

/** Hechos del dispositivo consultados por la pantalla de diagnóstico. */
interface DeviceEnvironment {
    fun googlePlayServicesAvailable(): Boolean
    fun postNotificationsGranted(): Boolean
    fun fcmChannelEnabled(): Boolean
    fun fcmConfigured(): Boolean
    fun lastServerContactAt(): Long?
}

/** Implementación real (Android). Válida para la máquina local y el dispositivo. */
class AndroidDeviceEnvironment(context: Context) : DeviceEnvironment {

    private val appContext = context.applicationContext
    private val serverContact = ServerContactStore(appContext)

    override fun googlePlayServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS

    override fun postNotificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun fcmChannelEnabled(): Boolean {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.getNotificationChannel(NotificationChannels.CHANNEL_ALERT)?.importance
            ?.takeIf { it != NotificationManager.IMPORTANCE_NONE } != null
    }

    override fun fcmConfigured(): Boolean = FcmTokenGateway.isConfigured(appContext)

    override fun lastServerContactAt(): Long? = serverContact.lastSuccessAt()
}