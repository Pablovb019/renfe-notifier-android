package com.pablovb019.renfenotifier.ui.theme

import com.pablovb019.renfenotifier.core.security.PreferencesRepository
import com.pablovb019.renfenotifier.core.security.SettingsSource
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga el tema guardado y deja de estar en loading`() = runTest(dispatcher) {
        val vm = ThemeViewModel(FakeSettingsSource(initialTheme = PreferencesRepository.THEME_DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.mode.value)
        assertFalse(vm.isLoading.value)
        assertNull(vm.saveError.value)
    }

    @Test
    fun `setMode actualiza al instante y persiste con el string exacto`() = runTest(dispatcher) {
        val settings = FakeSettingsSource()
        val vm = ThemeViewModel(settings)
        advanceUntilIdle()

        vm.setMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.mode.value)
        assertNull(vm.saveError.value)

        advanceUntilIdle()
        assertEquals(listOf("light"), settings.saved)
        assertNull(vm.saveError.value)
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage(settings.themeFlow.value))
    }

    @Test
    fun `setMode con el mismo valor no persiste de nuevo`() = runTest(dispatcher) {
        val settings = FakeSettingsSource(initialTheme = PreferencesRepository.THEME_SYSTEM)
        val vm = ThemeViewModel(settings)
        advanceUntilIdle()

        vm.setMode(ThemeMode.SYSTEM)
        advanceUntilIdle()
        assertTrue(settings.saved.isEmpty())
    }

    @Test
    fun `fallo al leer el tema muestra error y termina el loading`() = runTest(dispatcher) {
        val vm = ThemeViewModel(FakeSettingsSource(readError = IOException("almacen corrupto")))
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.mode.value)
        assertFalse(vm.isLoading.value)
        assertNotNull(vm.saveError.value)
    }

    @Test
    fun `fallo al guardar muestra error sin revertir el modo visible`() = runTest(dispatcher) {
        val settings = FakeSettingsSource()
        settings.errorOnSet = IOException("sin espacio")
        val vm = ThemeViewModel(settings)
        advanceUntilIdle()

        vm.setMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.mode.value)
        assertNotNull(vm.saveError.value)
        assertTrue(settings.saved.isEmpty())
    }

    @Test
    fun `el guardado lineal gana la ultima escritura`() = runTest(dispatcher) {
        val settings = FakeSettingsSource()
        val vm = ThemeViewModel(settings)
        advanceUntilIdle()

        vm.setMode(ThemeMode.DARK)
        vm.setMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, vm.mode.value)
        assertEquals(listOf("light"), settings.saved)
        assertNull(vm.saveError.value)
    }

    @Test
    fun `una CancellationException durante el guardado no se captura como error`() = runTest(dispatcher) {
        val settings = FakeSettingsSource()
        settings.errorOnSet = CancellationException("guardado cancelado")
        val vm = ThemeViewModel(settings)
        advanceUntilIdle()

        vm.setMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertNull(vm.saveError.value)
        assertEquals(ThemeMode.DARK, vm.mode.value)
    }
}

private class FakeSettingsSource(
    initialTheme: String = PreferencesRepository.THEME_SYSTEM,
    private val readError: Throwable? = null,
) : SettingsSource {

    val themeFlow = MutableStateFlow(initialTheme)
    var errorOnSet: Throwable? = null
    val saved: MutableList<String> = mutableListOf()

    override val theme: Flow<String>
        get() = readError?.let { flow { throw it } } ?: themeFlow

    override val alertsEnabled: Flow<Boolean> = MutableStateFlow(true)

    override suspend fun setTheme(theme: String) {
        errorOnSet?.let { throw it }
        saved += theme
        themeFlow.value = theme
    }

    override suspend fun setAlertsEnabled(enabled: Boolean): Unit = Unit
}