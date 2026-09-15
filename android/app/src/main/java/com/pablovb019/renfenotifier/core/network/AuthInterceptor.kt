package com.pablovb019.renfenotifier.core.network

import com.pablovb019.renfenotifier.core.security.TokenVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Añade `Authorization: Bearer <token>` a cada petición salvo las públicas
 * (health y emparejamiento). Si el backend responde 401 (credencial revocada o
 * dispositivos borrados), limpia el token almacenado y emite [credentialsRevoked].
 */
class AuthInterceptor(
    private val tokenVault: TokenVault,
    private val onCredentialRevoked: (() -> Unit)? = null,
) : Interceptor {

    companion object {
        /** Flujo compartido: 1 = revocación de credencial (401) que exige re-emparejar. */
        private val _credentialsRevoked = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val credentialsRevoked: SharedFlow<Unit> = _credentialsRevoked

        private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        val publicPath = path == "/health" || path == "/api/v1/pairing/claim"
        val request = if (publicPath) {
            chain.request()
        } else {
            val token = tokenVault.getToken()
            if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
        }

        val response = chain.proceed(request)
        if (response.code == 401 && !publicPath) {
            tokenVault.clear()
            eventScope.launch { _credentialsRevoked.emit(Unit) }
            onCredentialRevoked?.invoke()
        }
        return response
    }
}