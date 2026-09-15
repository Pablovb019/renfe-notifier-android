package com.pablovb019.renfenotifier.core.network.model

/**
 * Contratos de la API del backend (espejo de los modelos Pydantic).
 *
 * La nomenclatura Gson usa LOWER_CASE_WITH_UNDERSCORES: los campos Kotlin
 * camelCase se serializan/deserializan como snake_case (p. ej. `deviceId`
 * <-> `device_id`, `plazaHRequested` <-> `plaza_h_requested`).
 */

data class HealthResponse(
    val status: String,
    val uptimeS: Double,
)

data class ClaimRequest(
    val code: String,
    val deviceId: String,
    val deviceName: String,
)

data class ClaimResponse(
    val deviceToken: String,
    val deviceId: String,
)

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val createdAt: String,
    val revokedAt: String?,
)

data class StationOut(
    val name: String,
    val code: String,
    val isGroup: Boolean,
)

data class TrainSearchRequest(
    val originCode: String,
    val destinationCode: String,
    val travelDate: String,
    val plazaH: Boolean = false,
)

data class TrainOut(
    val identifier: String,
    val identity: String,
    val departure: String?,
    val arrival: String?,
    val price: String?,
    val availability: String,
)

data class TrainSearchResponse(
    val status: String,
    val plazaHRequested: Boolean,
    val trains: List<TrainOut>,
)

data class FollowUpCreateRequest(
    val originCode: String,
    val destinationCode: String,
    val travelDate: String,
    val mode: String,
    val plazaH: Boolean = false,
    val specificTrainId: String? = null,
    val departureTime: String? = null,
)

data class FollowUpOut(
    val followupId: String,
    val originCode: String,
    val originName: String?,
    val destinationCode: String,
    val destinationName: String?,
    val travelDate: String,
    val mode: String,
    val plazaH: Boolean,
    val specificTrainId: String?,
    val lifecycle: String,
    val availability: String,
    val alertState: String,
    val episode: Int,
    val expiresAt: String,
)

data class EpisodeOut(
    val episodeId: Int,
    val episode: Int,
    val observedAt: String,
    val trainIds: List<String>,
)

data class FollowUpDetailOut(
    val followupId: String,
    val originCode: String,
    val originName: String?,
    val destinationCode: String,
    val destinationName: String?,
    val travelDate: String,
    val mode: String,
    val plazaH: Boolean,
    val specificTrainId: String?,
    val lifecycle: String,
    val availability: String,
    val alertState: String,
    val episode: Int,
    val expiresAt: String,
    val episodes: List<EpisodeOut>,
)

data class FollowUpListOut(
    val items: List<FollowUpOut>,
    val total: Int,
)

data class FcmTokenRequest(
    val fcmToken: String,
)

data class FcmTokenResponse(
    val registered: Boolean,
)

data class FollowUpCountsOut(
    val active: Int,
    val paused: Int,
    val expired: Int,
    val deleted: Int,
    val total: Int,
)

/** Cifras de búsqueda del backend: consultas lógicas y peticiones HTTP (desde el reinicio). */
data class SearchStatsOut(
    val logicalQueries: Long,
    val httpRequests: Long,
    val bytesReceived: Long,
)

data class DiagnosticsOut(
    val status: String,
    val appVersion: String,
    val dbOk: Boolean,
    val devices: Int,
    val followups: FollowUpCountsOut,
    val episodes: Int,
    val alertsPending: Int,
    val searchStats: SearchStatsOut,
)

data class TestNotificationOut(
    val messageId: String,
)