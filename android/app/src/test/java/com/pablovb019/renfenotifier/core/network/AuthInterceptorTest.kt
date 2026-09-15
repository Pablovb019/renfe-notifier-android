package com.pablovb019.renfenotifier.core.network

import com.pablovb019.renfenotifier.core.security.InMemoryTokenVault
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Verifica que el interceptor añade el Bearer token en endpoints protegidos,
 * lo omite en los públicos y limpia la credencial ante un 401 (revocada).
 */
class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var vault: InMemoryTokenVault

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        vault = InMemoryTokenVault()
        vault.save("token-secreto")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `anyade bearer token a una peticion protegida`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val client = clientWithInterceptor()

        client.newCall(okHttpRequest("/api/v1/pairing/devices")).execute().use { response ->
            assertEquals(200, response.code)
        }

        val authorization = server.takeRequest().getHeader("Authorization")
        assertEquals("Bearer token-secreto", authorization)
    }

    @Test
    fun `no anyade token al endpoint publico de salud`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok","uptime_s":1}"""))
        val client = clientWithInterceptor()

        client.newCall(okHttpRequest("/health")).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `un 401 limpia la credencial almacenada`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"No autorizado"}"""))
        val client = clientWithInterceptor()

        client.newCall(okHttpRequest("/api/v1/followups")).execute().use { response ->
            assertEquals(401, response.code)
        }

        assertNull(vault.getToken())
    }

    private fun clientWithInterceptor(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(vault))
            .build()
    }

    private fun okHttpRequest(path: String): okhttp3.Request {
        return okhttp3.Request.Builder()
            .url(server.url(path))
            .get()
            .build()
    }
}