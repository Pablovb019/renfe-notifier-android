package com.pablovb019.renfenotifier.feature.home

import java.time.LocalDateTime

/** Estado inmutable de la pantalla de inicio. */
data class HomeUiState(
    val versionName: String = "–",
    val now: LocalDateTime? = null,
    val loading: Boolean = true,
    val isPaired: Boolean = false,
)