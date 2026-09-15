package com.pablovb019.renfenotifier.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Azul Renfe con neutros cálidos: paleta fija usada cuando no hay color dinámico. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0057A6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001A3A),
    secondary = Color(0xFF505F79),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF0D1D33),
    tertiary = Color(0xFF00696C),
    onTertiary = Color.White,
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191B21),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191B21),
    outline = Color(0xFF75777F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF002E62),
    primaryContainer = Color(0xFF004388),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFB9C4DE),
    onSecondary = Color(0xFF232F44),
    secondaryContainer = Color(0xFF39455B),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF4DD8DC),
    onTertiary = Color(0xFF003739),
    background = Color(0xFF12141B),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF12141B),
    onSurface = Color(0xFFE2E2E9),
    outline = Color(0xFF8F9099),
)

/**
 * Tema Material 3. Usa color dinámico (Android 12+) y conserva una paleta fija
 * con buen contraste como respaldo; soporta claro y oscuro según el sistema.
 */
@Composable
fun RenfeNotifierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}