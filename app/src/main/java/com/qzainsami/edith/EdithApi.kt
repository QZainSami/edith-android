package com.qzainsami.edith

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.io.ByteArrayOutputStream

data class Connection(val base: String, val username: String = "", val password: String = "")

/** No model/device secrets belong in the phone app. Only the backend web login is used. */
object EdithApi {
    fun isLocalAddress(host: String): Boolean {
        val pieces = host.split('.')
        val octets = pieces.mapNotNull { it.toIntOrNull() }
        return host == "localhost" || (pieces.size == 4 && octets.size == 4 && octets.all { it in 0..255 } &&
            (octets[0] == 10 || (octets[0] == 192 && octets[1] == 168) ||
             (octets[0] == 172 && octets[1] in 16..31) || octets[0] == 127))
    }
    fun normalizeAddress(raw: String): String {
        val uri = try { URI(raw.trim()) } catch (_: Exception) {
            throw IllegalArgumentException("Enter a complete http:// or https:// backend address.")
        }
        require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.path in listOf("", "/") && uri.port in -1..65535 && uri.port != 0) {
            "Use the backend address only, without /api, a login, or a query."
        }
        val host = uri.host.lowercase()
        require(uri.scheme == "https" || (BuildConfig.DEBUG && isLocalAddress(host))) {
            "Use HTTPS for remote servers. Debug builds also support a local IPv4 address."
        }
        return uri.toASCIIString().trimEnd('/')
    }

    fun request(connection: Connection, path: String, body: JSONObject? = null): JSONObject {
        val base = normalizeAddress(connection.base)
        val http = URL(base + path).openConnection() as HttpURLConnection
        try {
            http.instanceFollowRedirects = false
            http.connectTimeout = 8_000
            http.readTimeout = if (path == "/api/chat") 105_000 else 15_000
            http.setRequestProperty("Accept", "application/json")
            if (connection.username.isNotBlank()) {
                val login = "${connection.username}:${connection.password}"
                http.setRequestProperty("Authorization", "Basic " +
                    Base64.encodeToString(login.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            }
            if (body != null) {
                http.requestMethod = "POST"
                http.doOutput = true
                http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                http.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = http.responseCode
            if (code == 401) error("Sign-in failed. Check the backend web username and password in Settings.")
            if (code in 300..399) error("Server redirected the request. Enter its final HTTPS address in Settings.")
            val stream = if (code in 200..299) http.inputStream else http.errorStream
            val bytes = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 1_048_576) { "Server response was too large." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            check(bytes.size <= 1_048_576) { "Server response was too large." }
            val text = String(bytes, Charsets.UTF_8)
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(text).optString("error") }.getOrDefault("")
                error(if (detail.isNotBlank()) detail else "Backend returned HTTP $code. Check its address and settings.")
            }
            return try { JSONObject(text) } catch (_: Exception) {
                error("The address did not return EDITH data. Use the Python backend, normally port 8000.")
            }
        } finally { http.disconnect() }
    }
}
