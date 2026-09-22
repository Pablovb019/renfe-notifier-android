package com.pablovb019.renfenotifier.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Duraciones del movimiento en milisegundos. Cortas y sutiles (sin Lottie ni
 * shared transitions). Las animaciones de Compose con especificación finita
 * escalan por el "animator duration scale" del sistema; 0 detiene el
 * movimiento y salta al estado final.
 */
object RenfeMotion {
    const val Short = 150
    const val Normal = 200
    const val Medium = 250
}

/**
 * Resorte de Renfe: suave (damping 0.9) y de rigidez media, pensado para
 * cambios de estado discretos sin rebotes.
 */
fun renfeSpring(): SpringSpec<Float> =
    spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)