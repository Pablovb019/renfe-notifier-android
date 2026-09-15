package com.pablovb019.renfenotifier.core.network

import com.pablovb019.renfenotifier.core.network.model.ClaimRequest
import com.pablovb019.renfenotifier.core.network.model.ClaimResponse
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

/** Fake del contrato [RenfeApi] para pruebas de ViewModel sin red real. */
class FakeRenfeApi : RenfeApi {

    var claimHandler: (suspend (ClaimRequest) -> ClaimResponse)? = null
    var searchStationsHandler: (suspend (query: String, limit: Int) -> List<StationOut>)? = null
    var searchTrainsHandler: (suspend (TrainSearchRequest) -> TrainSearchResponse)? = null
    var createFollowUpHandler: (suspend (FollowUpCreateRequest) -> FollowUpOut)? = null
    var listFollowUpsHandler: (suspend (lifecycle: String?) -> FollowUpListOut)? = null
    var getFollowUpHandler: (suspend (followupId: String) -> FollowUpDetailOut)? = null
    var pauseFollowUpHandler: (suspend (followupId: String) -> FollowUpOut)? = null
    var resumeFollowUpHandler: (suspend (followupId: String) -> FollowUpOut)? = null
    var renewFollowUpHandler: (suspend (followupId: String) -> FollowUpOut)? = null
    var acknowledgeFollowUpHandler: (suspend (followupId: String) -> FollowUpOut)? = null
    var deleteFollowUpHandler: (suspend (followupId: String) -> Unit)? = null
    var registerFcmTokenHandler: (suspend (FcmTokenRequest) -> FcmTokenResponse)? = null
    var diagnosticsHandler: (suspend () -> DiagnosticsOut)? = null
    var sendTestNotificationHandler: (suspend () -> TestNotificationOut)? = null

    override suspend fun health(): HealthResponse {
        throw NotImplementedError("no usado en estos tests")
    }

    override suspend fun claimPairing(
        request: ClaimRequest,
    ): ClaimResponse {
        return claimHandler?.invoke(request)
            ?: throw NotImplementedError("claimHandler sin configurar")
    }

    override suspend fun listDevices(): List<com.pablovb019.renfenotifier.core.network.model.DeviceInfo> =
        throw NotImplementedError()

    override suspend fun searchStations(query: String, limit: Int): List<StationOut> =
        searchStationsHandler?.invoke(query, limit)
            ?: throw NotImplementedError("searchStationsHandler sin configurar")

    override suspend fun searchTrains(request: TrainSearchRequest): TrainSearchResponse =
        searchTrainsHandler?.invoke(request)
            ?: throw NotImplementedError("searchTrainsHandler sin configurar")

    override suspend fun createFollowUp(
        request: FollowUpCreateRequest,
    ): FollowUpOut = createFollowUpHandler?.invoke(request)
        ?: throw NotImplementedError("createFollowUpHandler sin configurar")

    override suspend fun listFollowUps(lifecycle: String?): FollowUpListOut =
        listFollowUpsHandler?.invoke(lifecycle)
            ?: throw NotImplementedError("listFollowUpsHandler sin configurar")

    override suspend fun getFollowUp(followupId: String): FollowUpDetailOut =
        getFollowUpHandler?.invoke(followupId)
            ?: throw NotImplementedError("getFollowUpHandler sin configurar")

    override suspend fun pauseFollowUp(followupId: String): FollowUpOut =
        pauseFollowUpHandler?.invoke(followupId)
            ?: throw NotImplementedError("pauseFollowUpHandler sin configurar")

    override suspend fun resumeFollowUp(followupId: String): FollowUpOut =
        resumeFollowUpHandler?.invoke(followupId)
            ?: throw NotImplementedError("resumeFollowUpHandler sin configurar")

    override suspend fun renewFollowUp(followupId: String): FollowUpOut =
        renewFollowUpHandler?.invoke(followupId)
            ?: throw NotImplementedError("renewFollowUpHandler sin configurar")

    override suspend fun acknowledgeFollowUp(followupId: String): FollowUpOut =
        acknowledgeFollowUpHandler?.invoke(followupId)
            ?: throw NotImplementedError("acknowledgeFollowUpHandler sin configurar")

    override suspend fun deleteFollowUp(followupId: String) {
        deleteFollowUpHandler?.invoke(followupId)
            ?: throw NotImplementedError("deleteFollowUpHandler sin configurar")
    }

    override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse =
        registerFcmTokenHandler?.invoke(request)
            ?: throw NotImplementedError("registerFcmTokenHandler sin configurar")

    override suspend fun diagnostics(): DiagnosticsOut =
        diagnosticsHandler?.invoke()
            ?: throw NotImplementedError("diagnosticsHandler sin configurar")

    override suspend fun sendTestNotification(): TestNotificationOut =
        sendTestNotificationHandler?.invoke()
            ?: throw NotImplementedError("sendTestNotificationHandler sin configurar")
}