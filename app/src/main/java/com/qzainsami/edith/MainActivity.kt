package com.qzainsami.edith

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[EdithViewModel::class.java]
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFC9F575), onPrimary = Color(0xFF192312),
                background = Color(0xFF101720), surface = Color(0xFF192331),
                surfaceVariant = Color(0xFF253347)
            )) {
                LaunchedEffect(model) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (true) { model.autoRefresh(); delay(750) }
                    }
                }
                var booting by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1500); booting = false }
                if(booting) BootScreen() else EdithApp(model)
            }
        }
    }
}

@Composable
private fun EdithApp(model: EdithViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf("Chat", "Controls", "Settings")
    val glyphs = listOf("✦", "◉", "⚙")
    Scaffold(
        bottomBar = {
            NavigationBar { labels.forEachIndexed { index, label ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index },
                    icon = { Text(glyphs[index], fontSize = 22.sp) }, label = { Text(label) })
            } }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("EDITH", fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Text("Your little desktop companion", style = MaterialTheme.typography.labelMedium) }
                TextButton(onClick = model::refresh, enabled = !model.busy && !model.refreshing) { Text("Refresh") }
            }
            CompanionHeader(model)
            model.error?.let { Text(it, Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            // Background status checks stay silent. Reserve space even when no action is running.
            Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp)) {
                if(model.busy) LinearProgressIndicator(Modifier.fillMaxSize())
            }
            when(tab) { 0 -> ChatScreen(model); 1 -> ControlScreen(model); else -> SettingsScreen(model) }
        }
    }
}

@Composable
private fun ColumnScope.ChatScreen(model: EdithViewModel) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(model.chat.size) {
        if(model.chat.isNotEmpty()) {
            if(model.chat.size >= 2 && model.chat[model.chat.lastIndex - 1].text == draft.trim()) draft = ""
            listState.animateScrollToItem(model.chat.lastIndex)
        }
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if(model.chat.isEmpty()) item { Panel("Say hello ✦") {
            Text("Chat with EDITH, or type a command like ‘EDITH sleep’ or ‘set a timer for 5 minutes’.")
            Text("Voice and microphone features will come later.", style = MaterialTheme.typography.bodySmall)
        } }
        items(model.chat) { line ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = if(line.user) Alignment.End else Alignment.Start) {
                Column(Modifier.widthIn(max = 340.dp).background(
                    if(line.user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Text(if(line.user) "You" else "EDITH · ${line.source}", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(5.dp)); Text(line.text)
                }
            }
        }
        if(model.busy) item { Text("EDITH is working…", style = MaterialTheme.typography.bodySmall) }
    }
    Text(model.notice, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4)
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = draft, onValueChange = { if(it.length <= 2000) draft = it },
            modifier = Modifier.weight(1f), placeholder = { Text("Message EDITH…") }, maxLines = 4,
            enabled = !model.busy, shape = RoundedCornerShape(18.dp))
        Button(onClick = { model.send(draft) }, enabled = draft.isNotBlank() && !model.busy) { Text("Send") }
    }
    TextButton(onClick = model::clearChat, enabled = !model.busy,
        modifier = Modifier.align(Alignment.End).padding(end = 12.dp)) { Text("Clear conversation") }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ControlScreen(model: EdithViewModel) {
    var duration by rememberSaveable { mutableStateOf("5") }
    var unit by rememberSaveable { mutableIntStateOf(1) }
    var cityQuery by rememberSaveable { mutableStateOf("") }
    val enabled = !model.busy
    val panel = model.status?.optJSONObject("companion")
    var timerMs by remember { mutableLongStateOf(-1) }
    LaunchedEffect(panel) {
        val remaining = panel?.optLong("timer_remaining_ms", -1) ?: -1
        val start = SystemClock.elapsedRealtime()
        while(true) {
            timerMs = if(remaining < 0) -1 else (remaining - (SystemClock.elapsedRealtime() - start)).coerceAtLeast(0)
            delay(250)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(model.notice, style = MaterialTheme.typography.bodySmall) }
        item { Panel("Faces") {
            listOf(listOf("normal", "happy", "sad", "angry"), listOf("sleepy", "curious", "thinking", "uwu")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { mood -> OutlinedButton(onClick = { model.mood(mood) }, enabled = enabled,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)) {
                        Text(mood, fontSize = 11.sp, maxLines = 1)
                    } }
                }
            }
            val device = model.status?.optJSONObject("device")?.optJSONObject("response")
            if(device != null) Text("Reported face: ${device.optString("mood", "unknown")} · ${if(device.optBoolean("pending")) "command pending" else if(device.optBoolean("applied")) "applied" else "idle"}",
                style = MaterialTheme.typography.bodySmall)
        } }
        item { Panel("Sleep dashboard") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { model.screen("dashboard") }, enabled = enabled) { Text("Sleep") }
                OutlinedButton(onClick = { model.screen("face") }, enabled = enabled) { Text("Wake") }
            }
            Text("Requested screen: ${panel?.optString("mode") ?: "unknown"}")
            Text("Sleep keeps EDITH connected and shows the clock and weather. An active timer takes over the display.", style = MaterialTheme.typography.bodySmall)
        } }
        item { TimeTools(model) }
        item { Panel("Focus timer") {
            Text(when { timerMs < 0 -> "No timer"; timerMs == 0L -> "Time up ✦"; else -> {
                val s = (timerMs + 999) / 1000
                if(s >= 3600) String.format(Locale.ROOT, "%02d:%02d:%02d", s/3600, s/60%60, s%60)
                else String.format(Locale.ROOT, "%02d:%02d", s/60, s%60)
            } }, fontSize = 36.sp, fontWeight = FontWeight.Light)
            if(model.status == null) Text("Countdown unavailable until the backend reconnects.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = duration, onValueChange = { if(it.length <= 5 && it.all(Char::isDigit)) duration = it },
                label = { Text("Duration") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = enabled)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Seconds", "Minutes", "Hours").forEachIndexed { index, label ->
                    FilterChip(selected = unit == index, onClick = { unit = index }, label = { Text(label) }, enabled = enabled)
                }
            }
            Button(onClick = { model.timer(duration, listOf(1,60,3600)[unit]) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth()) { Text("Start timer") }
            OutlinedButton(onClick = model::cancelTimer, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Cancel / dismiss") }
            Text("1 second to 24 hours · visual alert only", style = MaterialTheme.typography.bodySmall)
        } }
        item { Panel("Weather location") {
            Text(panel?.optString("city")?.takeIf { it.isNotBlank() } ?: "No city selected")
            panel?.optJSONObject("weather")?.let { weather ->
                Text("${weather.optDouble("temperature")}°C · humidity ${weather.optInt("humidity")}%")
                Text("Reading age: ${panel.optLong("weather_age") / 60} min", style = MaterialTheme.typography.bodySmall)
            }
            val weatherError = panel?.optString("weather_error", "")
            if(!weatherError.isNullOrBlank() && weatherError != "null") Text(weatherError, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = cityQuery, onValueChange = { if(it.length <= 80) cityQuery = it }, label = { Text("City, e.g. Karachi") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled)
            Button(onClick = { model.searchCity(cityQuery) }, enabled = enabled) { Text("Find city") }
            model.cities.forEach { city -> OutlinedButton(onClick = { model.chooseCity(city) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth()) { Text("Use ${city.name}") } }
            Text("Uses your phone’s current time-zone offset when saving. Weather by Open-Meteo (open-meteo.com).", style = MaterialTheme.typography.bodySmall)
        } }
    }
}

@Composable
private fun SettingsScreen(model: EdithViewModel) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(android.os.Build.VERSION.SDK_INT < 37 ||
        context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val localPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if(it) model.refresh()
    }
    var address by rememberSaveable { mutableStateOf(model.address) }
    var username by rememberSaveable { mutableStateOf(model.username) }
    // Deliberately not rememberSaveable: credentials do not enter saved instance state.
    var password by remember { mutableStateOf(model.password) }
    val enabled = !model.busy
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Panel("Connect to EDITH") {
            Text("Enter your Python backend address, not the ESP32 or LM Studio address.")
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Backend URL") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), enabled = enabled)
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Web username (if configured)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Web password") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = enabled)
            Button(onClick = { model.saveConnection(address, username, password) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth()) { Text("Save & connect") }
            Text("Address and username are saved on this phone. Password is kept only while the app process is alive. Enter it again after a cold start.", style = MaterialTheme.typography.bodySmall)
        } }
        item { Panel("Local setup") {
            if(!permissionGranted) {
                Text("Android 17 needs your permission to connect to your PC on the local network.")
                Button(onClick = { localPermission.launch("android.permission.ACCESS_LOCAL_NETWORK") }) { Text("Allow local network") }
            }
            Text("Android emulator on your PC:\nhttp://10.0.2.2:8000\n\nPhysical phone on the same Wi-Fi:\nhttp://YOUR-PC-LAN-IP:8000")
            Text("Start the backend on 0.0.0.0:8000 for a physical phone. Allow port 8000 on your trusted network. HTTP is for local development in debug builds; hosted connections require HTTPS.", style = MaterialTheme.typography.bodySmall)
            Text("The PC must stay awake with the backend and LM Studio running. Installing this app does not host either service.", style = MaterialTheme.typography.bodySmall)
        } }
        item { Panel("Model & privacy") {
            OutlinedButton(onClick = model::checkModel, enabled = enabled) { Text("Check LM Studio") }
            Text("${model.status?.optString("lmstudio_model") ?: "Model not checked"}")
            Text("Chat stays in memory until cleared or the app process closes. The last six exchanges are sent to your backend for context. Switching backend clears the conversation. Microphone, audio and background wake-word listening are not enabled.", style = MaterialTheme.typography.bodySmall)
            Text(model.notice, style = MaterialTheme.typography.bodySmall)
        } }
    }
}
