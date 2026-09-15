package com.pablovb019.renfenotifier.core.network

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.pablovb019.renfenotifier.BuildConfig
import com.pablovb019.renfenotifier.core.diagnostics.HeartbeatInterceptor
import com.pablovb019.renfenotifier.core.diagnostics.ServerContactStore
import com.pablovb019.renfenotifier.core.security.KeystoreTokenVault
import com.pablovb019.renfenotifier.core.security.TokenVault
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica del cliente HTTP de la app (sin framework de inyección; paso 29 lo
 * aportará). Enforces TLS: solo https fuera del bucle local (desarrollo/test).
 */
object ApiModule {

    private const val TIMEOUT_CONNECT_S = 15L
    private const val TIMEOUT_READ_S = 30L

    private val loopbackHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")

    private lateinit var tokenVault: TokenVault
    private lateinit var serverContactStore: ServerContactStore

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenVault))
            .addInterceptor(HeartbeatInterceptor(serverContactStore))
            .connectTimeout(TIMEOUT_CONNECT_S, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_READ_S, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        if (!::tokenVault.isInitialized) {
            tokenVault = KeystoreTokenVault(context)
        }
        if (!::serverContactStore.isInitialized) {
            serverContactStore = ServerContactStore(context)
        }
    }

    fun api(): RenfeApi {
        if (!::tokenVault.isInitialized) {
            error("ApiModule.init(context) debe llamarse antes de api()")
        }
        return newApi(BuildConfig.BACKEND_URL, httpClient)
    }

    /** Permite a los tests construir la API contra un servidor simulado. */
    fun newApi(baseUrl: String, client: OkHttpClient): RenfeApi {
        validateBaseUrl(baseUrl)
        return retrofit(baseUrl, client).create(RenfeApi::class.java)
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun validateBaseUrl(url: String) {
        val uri = URI(url)
        val host = uri.host ?: error("URL de backend inválida: $url")
        val scheme = uri.scheme
        val allowHttp = host in loopbackHosts
        check(scheme == "https" || (scheme == "http" && allowHttp)) {
            "Esquema o host inseguro: se exige HTTPS salvo hosts locales de test."
        }
    }
}