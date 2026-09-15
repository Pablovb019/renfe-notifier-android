package com.pablovb019.renfenotifier.core.network

import com.pablovb019.renfenotifier.core.network.model.ClaimRequest
import com.pablovb019.renfenotifier.core.network.model.ClaimResponse
import com.pablovb019.renfenotifier.core.network.model.DeviceInfo
import com.pablovb019.renfenotifier.core.network.model.DiagnosticsOut
import com.pablovb019.renfenotifier.core.network.model.FcmTokenRequest
import com.pablovb019.renfenotifier.core.network.model.FcmTokenResponse
import com.pablovb019.renfenotifier.core.network.model.FollowUpCreateRequest
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpListOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.core.network.model.HealthResponse
import com.pablovb019.renfenotifier.core.network.model.StationOut
import com.pablovb019.renfenotifier.core.network.model.TestNotificationOut
import com.pablovb019.renfenotifier.core.network.model.TrainSearchRequest
import com.pablovb019.renfenotifier.core.network.model.TrainSearchResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contratos HTTP del backend (v1). Los endpoints que devuelven el `DeviceDep`
 * requieren `Authorization: Bearer <device_token>`; lo añade [AuthInterceptor].
 */
interface RenfeApi {

    @GET("health")
    suspend fun health(): HealthResponse

    @POST("api/v1/pairing/claim")
    suspend fun claimPairing(@Body request: ClaimRequest): ClaimResponse

    @GET("api/v1/pairing/devices")
    suspend fun listDevices(): List<DeviceInfo>

    @GET("api/v1/search/stations")
    suspend fun searchStations(@Query("q") query: String, @Query("limit") limit: Int = 10): List<StationOut>

    @POST("api/v1/search/trains")
    suspend fun searchTrains(@Body request: TrainSearchRequest): TrainSearchResponse

    @POST("api/v1/followups")
    suspend fun createFollowUp(@Body request: FollowUpCreateRequest): FollowUpOut

    @GET("api/v1/followups")
    suspend fun listFollowUps(@Query("lifecycle") lifecycle: String? = null): FollowUpListOut

    @GET("api/v1/followups/{followup_id}")
    suspend fun getFollowUp(@Path("followup_id") followupId: String): FollowUpDetailOut

    @POST("api/v1/followups/{followup_id}/pause")
    suspend fun pauseFollowUp(@Path("followup_id") followupId: String): FollowUpOut

    @POST("api/v1/followups/{followup_id}/resume")
    suspend fun resumeFollowUp(@Path("followup_id") followupId: String): FollowUpOut

    @POST("api/v1/followups/{followup_id}/renew")
    suspend fun renewFollowUp(@Path("followup_id") followupId: String): FollowUpOut

    @POST("api/v1/followups/{followup_id}/acknowledge")
    suspend fun acknowledgeFollowUp(@Path("followup_id") followupId: String): FollowUpOut

    @DELETE("api/v1/followups/{followup_id}")
    suspend fun deleteFollowUp(@Path("followup_id") followupId: String)

    @PUT("api/v1/fcm/token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): FcmTokenResponse

    @GET("api/v1/diagnostics")
    suspend fun diagnostics(): DiagnosticsOut

    @POST("api/v1/diagnostics/test-notification")
    suspend fun sendTestNotification(): TestNotificationOut
}