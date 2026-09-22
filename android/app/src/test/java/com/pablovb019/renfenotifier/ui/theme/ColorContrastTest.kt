package com.pablovb019.renfenotifier.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

private fun contrastRatio(first: Color, second: Color): Double {
    val luminanceFirst = relativeLuminance(first)
    val luminanceSecond = relativeLuminance(second)
    val lighter = maxOf(luminanceFirst, luminanceSecond)
    val darker = minOf(luminanceFirst, luminanceSecond)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun checkContrast(pairs: List<Pair<String, Pair<Color, Color>>>, minimum: Double, scheme: String) {
    pairs.forEach { (label, colors) ->
        val ratio = contrastRatio(colors.first, colors.second)
        assertTrue(
            "$scheme: contraste $label debe ser >= $minimum (ratio = $ratio)",
            ratio >= minimum,
        )
    }
}

/**
 * Contrastes WCAG AA de la paleta real de Color.kt:
 * texto >= 4.5:1 (1.4.3) y componentes no textuales >= 3:1 (1.4.11), en claro y oscuro.
 */
class ColorContrastTest {

    private val lightTextPairs = listOf(
        "primary/onPrimary" to (LightPrimary to LightOnPrimary),
        "primary/primaryContainer (texto)" to (LightPrimary to LightPrimaryContainer),
        "secondary/onSecondary" to (LightSecondary to LightOnSecondary),
        "tertiary/onTertiary" to (LightTertiary to LightOnTertiary),
        "error/onError" to (LightError to LightOnError),
        "background/onBackground" to (LightBackground to LightOnBackground),
        "surface/onSurface" to (LightSurface to LightOnSurface),
        "surfaceVariant/onSurfaceVariant" to (LightSurfaceVariant to LightOnSurfaceVariant),
        "inverseSurface/inverseOnSurface" to (LightInverseSurface to LightInverseOnSurface),
        "onPrimaryContainer/primaryContainer (texto badge SUCCESS)" to
            (LightOnPrimaryContainer to LightPrimaryContainer),
        "onTertiaryContainer/tertiaryContainer (texto badge WARNING)" to
            (LightOnTertiaryContainer to LightTertiaryContainer),
        "onErrorContainer/errorContainer (texto badge ERROR)" to
            (LightOnErrorContainer to LightErrorContainer),
        "onPrimaryContainer/surfaceContainerLow (texto acento exito en tarjetas)" to
            (LightOnPrimaryContainer to LightSurfaceContainerLow),
        "onSurface/primaryContainer (texto en tarjeta seleccionada)" to
            (LightOnSurface to LightPrimaryContainer),
        "onSurfaceVariant/primaryContainer (texto secundario en tarjeta seleccionada)" to
            (LightOnSurfaceVariant to LightPrimaryContainer),
        "onSurface/surfaceContainerLow (texto en tarjetas)" to
            (LightOnSurface to LightSurfaceContainerLow),
        "onSurfaceVariant/surfaceContainerLow (texto secundario en tarjetas)" to
            (LightOnSurfaceVariant to LightSurfaceContainerLow),
    )

    private val darkTextPairs = listOf(
        "primary/onPrimary" to (DarkPrimary to DarkOnPrimary),
        "primary/primaryContainer (texto)" to (DarkPrimary to DarkPrimaryContainer),
        "secondary/onSecondary" to (DarkSecondary to DarkOnSecondary),
        "tertiary/onTertiary" to (DarkTertiary to DarkOnTertiary),
        "error/onError" to (DarkError to DarkOnError),
        "background/onBackground" to (DarkBackground to DarkOnBackground),
        "surface/onSurface" to (DarkSurface to DarkOnSurface),
        "surfaceVariant/onSurfaceVariant" to (DarkSurfaceVariant to DarkOnSurfaceVariant),
        "inverseSurface/inverseOnSurface" to (DarkInverseSurface to DarkInverseOnSurface),
        "onPrimaryContainer/primaryContainer (texto badge SUCCESS)" to
            (DarkOnPrimaryContainer to DarkPrimaryContainer),
        "onTertiaryContainer/tertiaryContainer (texto badge WARNING)" to
            (DarkOnTertiaryContainer to DarkTertiaryContainer),
        "onErrorContainer/errorContainer (texto badge ERROR)" to
            (DarkOnErrorContainer to DarkErrorContainer),
        "onPrimaryContainer/surfaceContainerLow (texto acento exito en tarjetas)" to
            (DarkOnPrimaryContainer to DarkSurfaceContainerLow),
        "onSurface/primaryContainer (texto en tarjeta seleccionada)" to
            (DarkOnSurface to DarkPrimaryContainer),
        "onSurfaceVariant/primaryContainer (texto secundario en tarjeta seleccionada)" to
            (DarkOnSurfaceVariant to DarkPrimaryContainer),
        "onSurface/surfaceContainerLow (texto en tarjetas)" to
            (DarkOnSurface to DarkSurfaceContainerLow),
        "onSurfaceVariant/surfaceContainerLow (texto secundario en tarjetas)" to
            (DarkOnSurfaceVariant to DarkSurfaceContainerLow),
    )

    private val lightNonTextPairs = listOf(
        "outline/background" to (LightOutline to LightBackground),
        "outline/surface" to (LightOutline to LightSurface),
        "primary/background" to (LightPrimary to LightBackground),
        "error/background" to (LightError to LightBackground),
    )

    private val darkNonTextPairs = listOf(
        "outline/background" to (DarkOutline to DarkBackground),
        "outline/surface" to (DarkOutline to DarkSurface),
        "primary/background" to (DarkPrimary to DarkBackground),
        "error/background" to (DarkError to DarkBackground),
    )

    @Test
    fun `pares de texto AA (4_5) en claro`() = checkContrast(lightTextPairs, 4.5, "Claro")

    @Test
    fun `pares de texto AA (4_5) en oscuro`() = checkContrast(darkTextPairs, 4.5, "Oscuro")

    @Test
    fun `pares no textuales AA (3) en claro`() = checkContrast(lightNonTextPairs, 3.0, "Claro")

    @Test
    fun `pares no textuales AA (3) en oscuro`() = checkContrast(darkNonTextPairs, 3.0, "Oscuro")
}