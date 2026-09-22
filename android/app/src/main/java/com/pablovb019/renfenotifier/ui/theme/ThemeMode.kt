package com.pablovb019.renfenotifier.ui.theme

import com.pablovb019.renfenotifier.core.security.PreferencesRepository

/**
 * Modo de tema de la app. `toStorage()` devuelve siempre los strings exactos
 * que ya usaba la app ("system"/"light"/"dark"): nunca `enum.name`.
 * Valores vacíos, nulos o desconocidos se mapean a [SYSTEM].
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun toStorage(): String = when (this) {
        SYSTEM -> PreferencesRepository.THEME_SYSTEM
        LIGHT -> PreferencesRepository.THEME_LIGHT
        DARK -> PreferencesRepository.THEME_DARK
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode = when (value) {
            PreferencesRepository.THEME_LIGHT -> LIGHT
            PreferencesRepository.THEME_DARK -> DARK
            else -> SYSTEM
        }
    }
}