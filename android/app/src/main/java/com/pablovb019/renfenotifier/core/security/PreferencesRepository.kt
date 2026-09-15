package com.pablovb019.renfenotifier.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pablovb019.renfenotifier.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/** Sesión local: estado de emparejamiento y credencial derivada del backend. */
interface SessionStore {
    val isPaired: Flow<Boolean>
    suspend fun setPaired(deviceId: String, deviceName: String)
    suspend fun clearAll()
}

/** Ajustes de la app: tema (system/light/dark) y avisos activados. */
interface SettingsSource {
    val theme: Flow<String>
    val alertsEnabled: Flow<Boolean>
    suspend fun setTheme(theme: String)
    suspend fun setAlertsEnabled(enabled: Boolean)
}

/**
 * Preferencias no secretas (DataStore). Lo secreto (token) vive en [TokenVault]
 * cifrado con Android Keystore. El backend es la fuente de verdad remota; aquí
 * solo se refleja el estado local de emparejamiento.
 */
class PreferencesRepository(context: Context) : SessionStore, SettingsSource {

    private val dataStore = context.applicationContext.dataStore

    override val isPaired: Flow<Boolean> = dataStore.data.map { it[KEY_IS_PAIRED] ?: false }
    val deviceId: Flow<String?> = dataStore.data.map { it[KEY_DEVICE_ID] }
    val deviceName: Flow<String?> = dataStore.data.map { it[KEY_DEVICE_NAME] }
    val backendUrl: Flow<String> = dataStore.data.map { it[KEY_BACKEND_URL] ?: BuildConfig.BACKEND_URL }

    override val theme: Flow<String> = dataStore.data.map { it[KEY_THEME] ?: THEME_SYSTEM }
    override val alertsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ALERTS_ENABLED] ?: true }

    override suspend fun setPaired(deviceId: String, deviceName: String) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_PAIRED] = true
            prefs[KEY_DEVICE_ID] = deviceId
            prefs[KEY_DEVICE_NAME] = deviceName
        }
    }

    suspend fun setBackendUrl(url: String) {
        dataStore.edit { prefs -> prefs[KEY_BACKEND_URL] = url }
    }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { prefs -> prefs[KEY_THEME] = theme }
    }

    override suspend fun setAlertsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ALERTS_ENABLED] = enabled }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private val KEY_IS_PAIRED = booleanPreferencesKey("is_paired")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
    }
}