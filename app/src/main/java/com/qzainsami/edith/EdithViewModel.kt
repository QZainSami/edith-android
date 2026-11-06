package com.qzainsami.edith

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.TimeZone

data class ChatLine(val user: Boolean, val text: String, val source: String = "")
data class City(val name: String, val latitude: Double, val longitude: Double)

class EdithViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("connection", 0)
    private val credentials = CredentialStore(application)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null
    private var refreshVersion = 0
    private var modelJob: Job? = null
    private var modelCheckedAt = 0L
    private var statusSequence = 0L
    var previewMood by mutableStateOf<String?>(null)
        private set
    var previewMode by mutableStateOf<String?>(null)
        private set
    var address by mutableStateOf(prefs.getString("address", "http://10.0.2.2:8000")!!)
        private set
    var username by mutableStateOf(prefs.getString("username", "")!!)
        private set
    // Restored from encrypted, device-bound storage after a process restart.
    var password by mutableStateOf(credentials.load(address, username))
        private set
    var status by mutableStateOf<JSONObject?>(null)
        private set
    var modelStatus by mutableStateOf<JSONObject?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf("Connect to your backend in Settings to get started.")
        private set
    var chat by mutableStateOf(listOf<ChatLine>())
        private set
    var cities by mutableStateOf(listOf<City>())
        private set

    init { if (prefs.contains("address")) refresh() }

    private fun connection() = Connection(address, username, password)
    private fun offset() = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
    private suspend fun call(config: Connection, path: String, body: JSONObject? = null): JSONObject {
        val host = runCatching { java.net.URI(config.base).host.orEmpty() }.getOrDefault("")
        if (android.os.Build.VERSION.SDK_INT >= 37 && EdithApi.isLocalAddress(host) &&
            getApplication<Application>().checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            error("Allow local network access in the app’s Settings tab before connecting to your PC.")
        }
        return withContext(Dispatchers.IO) { EdithApi.request(config, path, body) }
    }
    private suspend fun readStatus(config: Connection) {
        val sequence = ++statusSequence
        val version = refreshVersion
        val result = call(config, "/api/status")
        if(sequence == statusSequence && version == refreshVersion) status = result
    }
    private fun message(exc: Exception): String = when (exc) {
        is SocketTimeoutException -> "Request timed out. Check the backend, Wi-Fi and whether the model is loaded."
        is java.io.IOException -> "Cannot reach the backend. Check its address, Wi-Fi and firewall. ${exc.localizedMessage.orEmpty()}"
        else -> exc.message ?: "The request failed. Try again."
    }
    private fun acceptDevice(result: JSONObject, fallback: String) {
        val device = result.optJSONObject("device")
        notice = when {
            device?.optBoolean("ok") == false -> device.optString("error", "OLED is unavailable.")
            device?.optBoolean("queued") == true -> "$fallback Waiting for the OLED to receive it."
            else -> fallback
        }
    }
    fun saveConnection(raw: String, user: String, pass: String) {
        if (busy) return
        try {
            val normalized = EdithApi.normalizeAddress(raw)
            require(!user.contains(':')) { "The username cannot contain a colon." }
            credentials.save(normalized, user.trim(), pass)
            refreshVersion++
            refreshJob?.cancel()
            modelJob?.cancel()
            modelCheckedAt = 0
            refreshing = false
            address = normalized
            username = user.trim(); password = pass
            prefs.edit().putString("address", address).putString("username", username).apply()
            status = null; modelStatus = null; chat = emptyList(); cities = emptyList(); error = null
            notice = "Connection saved. Password is encrypted on this device."
            refresh()
        } catch (exc: Exception) { error = message(exc) }
    }
    fun refresh() = refreshStatus(false)
    private fun refreshStatus(automatic: Boolean) {
        if ((!automatic && busy) || refreshing) return
        val config = connection()
        val version = refreshVersion
        refreshJob = scope.launch {
            refreshing = true
            try {
                readStatus(config)
                error = null
                if (!automatic || android.os.SystemClock.elapsedRealtime() - modelCheckedAt >= 15000 || modelCheckedAt == 0L) {
                    if (modelJob?.isActive != true) {
                        modelCheckedAt = android.os.SystemClock.elapsedRealtime()
                        modelJob = scope.launch {
                            val result = try { call(config, "/api/lmstudio") }
                                catch (exc: CancellationException) { throw exc }
                                catch (_: Exception) { null }
                            if(version == refreshVersion && status != null) modelStatus = result
                        }
                    }
                }
            } catch (exc: CancellationException) { throw exc }
            catch (exc: Exception) { if(version == refreshVersion) { error = message(exc); status = null; modelStatus = null } }
            finally { if(version == refreshVersion) refreshing = false }
        }
    }
    // Automatic checks resume after a successful manual connection and pause on failure.
    fun autoRefresh() { if(status != null) refreshStatus(true) }
    private fun action(block: suspend (Connection) -> Unit) {
        if (busy) return
        refreshVersion++
        refreshJob?.cancel()
        refreshing = false
        val config = connection()
        scope.launch {
            busy = true; error = null
            try {
                block(config)
                try { readStatus(config) }
                catch (exc: CancellationException) { throw exc }
                catch (_: Exception) { status = null; notice += " Status refresh failed; tap Refresh." }
            } catch (exc: CancellationException) { throw exc }
            catch (exc: Exception) { error = message(exc) }
            finally { busy = false; previewMood = null; previewMode = null }
        }
    }
    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 2000) { error = "Enter 1–2000 characters."; return }
        action { config ->
            val history = JSONArray()
            chat.takeLast(12).forEach { history.put(JSONObject().put("role", if(it.user) "user" else "assistant").put("content", it.text)) }
            val result = call(config, "/api/chat", JSONObject().put("message", clean).put("history", history).put("utc_offset", offset()))
            chat = chat + listOf(ChatLine(true, clean), ChatLine(false, result.getString("reply"), result.optString("source")))
            acceptDevice(result, "Reply received.")
            val warning = result.optString("warning", "")
            if (warning.isNotBlank() && warning != "null") notice += " $warning"
        }
    }
    fun clearChat() { if(!busy) chat = emptyList() }
    fun mood(name: String) {
        if(busy) return
        previewMood = name; previewMode = "face"
        action { config ->
        acceptDevice(call(config, "/api/mood", JSONObject().put("name", name)), "Face requested: $name.")
        }
    }
    fun screen(mode: String) {
        if(busy) return
        previewMode = mode
        action { config ->
        acceptDevice(call(config, "/api/screen", JSONObject().put("mode", mode).put("utc_offset", offset())), "Screen requested: $mode.")
        }
    }
    fun timer(raw: String, multiplier: Int) {
        val value = raw.toLongOrNull()
        if (value == null || value < 1 || value > 86400L / multiplier) {
            error = "Choose a timer between 1 second and 24 hours."; return
        }
        action { config ->
            acceptDevice(call(config, "/api/timer", JSONObject().put("seconds", value * multiplier).put("utc_offset", offset())), "Timer started.")
        }
    }
    fun cancelTimer() = action { config ->
        acceptDevice(call(config, "/api/timer", JSONObject().put("seconds", 0)), "Timer dismissed.")
    }
    fun stopwatch(command: String) = action { config ->
        acceptDevice(call(config, "/api/stopwatch", JSONObject().put("action", command)), "Stopwatch: $command.")
    }
    fun setAlarm(hour: Int, minute: Int) {
        val next = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY,hour); set(java.util.Calendar.MINUTE,minute)
            set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0)
            if(timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR,1)
        }
        action { config ->
            acceptDevice(call(config,"/api/alarm",JSONObject().put("at",next.timeInMillis/1000).put("utc_offset",offset())),
                "One-time alarm saved for ${java.text.SimpleDateFormat("EEE HH:mm",java.util.Locale.getDefault()).format(next.time)}.")
        }
    }
    fun dismissAlarm() = action { config ->
        acceptDevice(call(config,"/api/alarm",JSONObject().put("at",JSONObject.NULL)), "Alarm dismissed.")
    }
    fun searchCity(query: String) {
        if(query.trim().length !in 2..80) { error = "Enter a city name between 2 and 80 characters."; return }
        action { config ->
            cities = emptyList()
            val results = call(config, "/api/locations?q=" + java.net.URLEncoder.encode(query.trim(), "UTF-8")).getJSONArray("results")
            cities = (0 until results.length()).map { i -> results.getJSONObject(i).let { City(it.getString("city"), it.getDouble("latitude"), it.getDouble("longitude")) } }
            notice = if(cities.isEmpty()) "No matching cities. Try another name." else "Choose your city below."
        }
    }
    fun chooseCity(city: City) = action { config ->
        acceptDevice(call(config, "/api/dashboard", JSONObject().put("city", city.name).put("latitude", city.latitude)
            .put("longitude", city.longitude).put("utc_offset", offset())), "Weather location saved.")
        cities = emptyList()
    }
    fun checkModel() = action { config ->
        val result = call(config, "/api/lmstudio")
        modelStatus = result
        notice = if(result.optBoolean("ok")) "LM Studio is reachable. Model: ${result.optString("selected")}."
            else result.optString("error", "LM Studio is unavailable. Rules remain available.")
    }
    override fun onCleared() { scope.cancel(); super.onCleared() }
}
