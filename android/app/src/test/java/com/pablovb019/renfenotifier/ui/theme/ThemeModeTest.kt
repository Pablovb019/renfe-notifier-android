package com.pablovb019.renfenotifier.ui.theme

import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `fromStorage mapea los tres valores conocidos`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(PreferencesRepository.THEME_SYSTEM))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage(PreferencesRepository.THEME_LIGHT))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorage(PreferencesRepository.THEME_DARK))
    }

    @Test
    fun `fromStorage mapea null y valores desconocidos a SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("SOCIAL_ICE_CREAM"))
    }

    @Test
    fun `toStorage devuelve los strings exactos de DataStore y nunca enum name`() {
        assertEquals("system", ThemeMode.SYSTEM.toStorage())
        assertEquals("light", ThemeMode.LIGHT.toStorage())
        assertEquals("dark", ThemeMode.DARK.toStorage())
        assertEquals(ThemeMode.SYSTEM.toStorage(), PreferencesRepository.THEME_SYSTEM)
        assertEquals(ThemeMode.LIGHT.toStorage(), PreferencesRepository.THEME_LIGHT)
        assertEquals(ThemeMode.DARK.toStorage(), PreferencesRepository.THEME_DARK)
    }

    @Test
    fun `round trip desde storage preserva el modo`() {
        for (mode in ThemeMode.entries) {
            assertEquals(mode, ThemeMode.fromStorage(mode.toStorage()))
        }
    }
}