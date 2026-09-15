package com.pablovb019.renfenotifier.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Almacén del token de dispositivo. Sincronizado a propósito: lo consume el
 * interceptor OkHttp (hilo de fondo) sin arrancar coroutines.
 */
interface TokenVault {
    fun save(token: String)
    fun getToken(): String?
    fun clear()
}

/**
 * Implementación real: cifra el token con AES/GCM y una clave generada en el
 * Android Keystore (no exportable). El blob cifrado (IV + datos) se guarda en
 * SharedPreferences; la clave nunca sale del Keystore del dispositivo.
 */
class KeystoreTokenVault(context: Context) : TokenVault {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun save(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_BLOB, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    override fun getToken(): String? {
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null
        val blobB64 = prefs.getString(KEY_BLOB, null) ?: return null
        val key = getKey() ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(blobB64, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            prefs.edit().clear().apply()
            null
        }
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        getKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun getKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private companion object {
        const val PREFS_NAME = "token_vault"
        const val KEY_ALIAS = "renfe_notifier_device_token"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_IV = "iv"
        const val KEY_BLOB = "blob"
    }
}

/** TokenVault de prueba/sin cifrado real (tests unitarios y emulaciones). */
class InMemoryTokenVault : TokenVault {
    private var token: String? = null
    override fun save(token: String) {
        this.token = token
    }

    override fun getToken(): String? = token
    override fun clear() {
        token = null
    }
}