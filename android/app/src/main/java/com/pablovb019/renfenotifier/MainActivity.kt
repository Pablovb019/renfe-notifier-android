package com.pablovb019.renfenotifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.core.notifications.FcmTokenGateway
import com.pablovb019.renfenotifier.core.notifications.NotificationDisplayer
import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.navigation.AppNavHost
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.ThemeMode
import com.pablovb019.renfenotifier.ui.theme.ThemeViewModel
import com.pablovb019.renfenotifier.ui.theme.ThemeViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Única Activity del módulo. Solicita POST_NOTIFICATIONS (Android 13+), abre la
 * pantalla indicada por una notificación (abrir seguimiento) y aloja el grafo.
 */
class MainActivity : ComponentActivity() {

    private val pendingFollowupId = MutableStateFlow<String?>(null)

    private val notificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* la pantalla de inicio refleja el estado; aquí solo se marca la pregunta hecha */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiModule.init(applicationContext)
        val prefs = PreferencesRepository(applicationContext)
        FcmTokenGateway.alertsEnabledProvider = { prefs.alertsEnabled.first() }
        requestNotificationsPermissionIfNeeded()
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(application),
            )
            val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            RenfeNotifierTheme(darkTheme = darkTheme) {
                AppNavHost(
                    pendingFollowupId = pendingFollowupId,
                    themeViewModel = themeViewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val followupId = intent?.getStringExtra(NotificationDisplayer.EXTRA_FOLLOWUP_ID).orEmpty()
        if (followupId.isNotEmpty()) {
            pendingFollowupId.value = followupId
        }
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) return
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val PREFS_NAME = "permission_state"
        const val KEY_ASKED_NOTIFICATIONS = "notifications_permission_asked"
    }
}