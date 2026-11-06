package com.qzainsami.edith

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Ciphertext stays in noBackupFilesDir; the encryption key never leaves Android Keystore. */
class CredentialStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "connection.enc")
    private val alias = "edith.connection.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun load(address: String, username: String): String = runCatching {
        if (!file.exists()) return ""
        val record = JSONObject(file.readText())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(record.getString("iv"), Base64.NO_WRAP)))
        val value = JSONObject(String(cipher.doFinal(Base64.decode(record.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8))
        if (value.getString("address") == address && value.getString("username") == username) value.getString("password") else ""
    }.getOrDefault("")
    fun save(address: String, username: String, password: String) {
        if (password.isEmpty()) { file.delete(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val value = JSONObject().put("address", address).put("username", username).put("password", password)
        val record = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        val atomic = android.util.AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(record.toString().toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream) }
        catch (exc: Exception) { atomic.failWrite(stream); throw exc }
    }
}
