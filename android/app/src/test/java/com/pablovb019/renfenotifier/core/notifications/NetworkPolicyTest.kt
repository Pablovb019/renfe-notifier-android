package com.pablovb019.renfenotifier.core.notifications

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NetworkPolicyTest {

    @Test
    fun `un fallo de red es reintentable`() {
        assertTrue(NetworkPolicy.isRetryable(IOException("sin red")))
    }

    @Test
    fun `429 y 5xx son reintentables`() {
        assertTrue(NetworkPolicy.isRetryable(http(429)))
        assertTrue(NetworkPolicy.isRetryable(http(500)))
        assertTrue(NetworkPolicy.isRetryable(http(503)))
    }

    @Test
    fun `errores de cliente 4xx no son reintentables`() {
        assertFalse(NetworkPolicy.isRetryable(http(400)))
        assertFalse(NetworkPolicy.isRetryable(http(404)))
        assertFalse(NetworkPolicy.isRetryable(http(409)))
        assertFalse(NetworkPolicy.isRetryable(http(401)))
    }

    @Test
    fun `404 y 409 son exito idempotente`() {
        assertTrue(NetworkPolicy.isIdempotentSuccess(http(404)))
        assertTrue(NetworkPolicy.isIdempotentSuccess(http(409)))
        assertFalse(NetworkPolicy.isIdempotentSuccess(http(400)))
        assertFalse(NetworkPolicy.isIdempotentSuccess(IOException("red")))
    }

    @Test
    fun `401 es revocacion de credencial`() {
        assertTrue(NetworkPolicy.isRevocation(http(401)))
        assertFalse(NetworkPolicy.isRevocation(http(403)))
    }

    @Test
    fun `el reintento se corta al agotar los intentos`() {
        assertTrue(NetworkPolicy.shouldRetry(http(503), runAttemptCount = 1))
        assertTrue(NetworkPolicy.shouldRetry(http(503), runAttemptCount = 2))
        assertFalse(NetworkPolicy.shouldRetry(http(503), runAttemptCount = 3))
        assertFalse(NetworkPolicy.shouldRetry(http(400), runAttemptCount = 1))
    }

    private fun http(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "err".toResponseBody("text/plain".toMediaType())))
}