package com.pablovb019.renfenotifier.core.model

/**
 * Modo de seguimiento, espejo de `FollowUpMode` del backend, con la semántica
 * documentada del bot:
 * - [FIRST]: vigila el tren más temprano del día.
 * - [LAST]: vigila el tren más tardío del día.
 * - [ALL]: vigila todos los trenes de la ruta/fecha.
 * - [SPECIFIC]: vigila un tren concreto (requiere su identidad).
 */
enum class FollowUpMode(val apiValue: String, val requiresTrain: Boolean) {
    FIRST("first", requiresTrain = false),
    LAST("last", requiresTrain = false),
    ALL("all", requiresTrain = false),
    SPECIFIC("specific", requiresTrain = true),
}

/** Valores de disponibilidad de un tren, espejo del parser del backend. */
object Availability {
    const val AVAILABLE = "available"
    const val NO_AVAILABILITY = "no_availability"
    const val UNKNOWN = "unknown"
}

/** Valores de disponibilidad de un seguimiento (estado del episodio actual). */
object AvailabilityStateValue {
    const val AVAILABLE = "available"
    const val UNAVAILABLE = "unavailable"
    const val UNKNOWN = "unknown"
}

/** Ciclo de vida de un seguimiento, espejo de `Lifecycle` del backend. */
object FollowUpLifecycle {
    const val ACTIVE = "active"
    const val PAUSED = "paused"
    const val EXPIRED = "expired"
    const val DELETED = "deleted"
}

/** Estado del aviso de un episodio, espejo de `AlertState` del backend. */
object FollowUpAlertState {
    const val IDLE = "idle"
    const val PENDING = "pending_alert"
    const val ACKNOWLEDGED = "acknowledged"
}