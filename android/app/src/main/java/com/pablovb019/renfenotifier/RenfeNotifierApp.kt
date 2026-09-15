package com.pablovb019.renfenotifier

import android.app.Application
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.notifications.FcmTokenRegistrationWorker
import com.pablovb019.renfenotifier.core.notifications.NotificationChannels
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Application: inicializa el cliente HTTP y los canales de notificación, y
 * registra el token FCM en cuanto el dispositivo está emparejado (y en cada
 * emparejamiento nuevo). Sin polling: el registro es un trabajo diferido único.
 */
class RenfeNotifierApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ApiModule.init(this)
        NotificationChannels.create(this)
        observePairingForTokenRegistration()
    }

    private fun observePairingForTokenRegistration() {
        scope.launch {
            val prefs = PreferencesRepository(this@RenfeNotifierApp)
            prefs.isPaired.distinctUntilChanged().collect { paired ->
                if (paired) {
                    FcmTokenRegistrationWorker.enqueue(this@RenfeNotifierApp)
                }
            }
        }
    }
}