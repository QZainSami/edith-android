package com.qzainsami.edith

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
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
            EdithTheme(appTheme = model.currentTheme) {
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
    val colors = EdithTheme.colors

    Scaffold(
        bottomBar = {
            Surface(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = colors.navIndicator,
                                selectedTextColor = colors.navSelectedText,
                                unselectedTextColor = colors.navUnselected,
                                selectedIconColor = colors.navSelectedText,
                                unselectedIconColor = colors.navUnselected
                            ),
                            icon = { NavGlyph(index, tab == index) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.background, colors.backgroundGradientEnd)))
                .padding(padding)
                .imePadding()
        ) {
            // Top Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EdithLogo(Modifier.size(42.dp))
                    Column {
                        Text(
                            "EDITH",
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            "A LITTLE COMPANION",
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                            color = colors.secondary
                        )
                    }
                }
                // Fixed: Refresh button does NOT blink during background sync
                TextButton(
                    onClick = model::refresh,
                    enabled = !model.busy,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.primary
                    )
                ) {
                    Text(if (model.refreshing) "↻ Syncing…" else "↻ Refresh", fontSize = 12.sp)
                }
            }
            CompanionHeader(model)
            model.error?.let {
                Text(
                    it,
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = colors.statusOffline,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Background status checks stay silent. Reserve space even when no action is running.
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp)) {
                if(model.busy) LinearProgressIndicator(Modifier.fillMaxSize(), color = colors.primary)
            }
            when(tab) {
                0 -> ChatScreen(model)
                1 -> ControlScreen(model)
                else -> SettingsScreen(model)
            }
        }
    }
}

@Composable
private fun ColumnScope.ChatScreen(model: EdithViewModel) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val colors = EdithTheme.colors

    LaunchedEffect(model.chat.size) {
        if(model.chat.isNotEmpty()) {
            if(model.chat.size >= 2 && model.chat[model.chat.lastIndex - 1].text == draft.trim()) draft = ""
            listState.animateScrollToItem(model.chat.lastIndex)
        }
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if(model.chat.isEmpty()) item {
            Panel("Hey, I'm EDITH ✦") {
                Text(
                    "A little company. A little curiosity. What's on your mind?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Text(
                    "TRY A QUICK COMMAND",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = colors.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { model.send("EDITH sleep") },
                        enabled = !model.busy,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sleep", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { model.send("set a timer for 5 minutes") },
                        enabled = !model.busy,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("5-min timer", fontSize = 11.sp)
                    }
                }
            }
        }
        items(model.chat) { line ->
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = if(line.user) Alignment.End else Alignment.Start
            ) {
                Surface(
                    Modifier.widthIn(max = 340.dp),
                    color = if(line.user) colors.chatUserBubble else colors.chatEdithBubble,
                    border = BorderStroke(1.dp, if(line.user) colors.chatUserBorder else colors.chatEdithBorder),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if(line.user) 16.dp else 4.dp,
                        bottomEnd = if(line.user) 4.dp else 16.dp
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if(line.user) "YOU" else if(line.source == "rules") "EDITH · RULES" else "EDITH",
                            color = if(line.user) colors.primary else colors.secondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(line.text, color = colors.textPrimary)
                    }
                }
            }
        }
        if(model.busy) item {
            Text("EDITH is working…", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
    if(model.error != null || model.status == null || model.chat.lastOrNull()?.source == "rules") {
        Text(
            model.notice,
            Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 2
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { if(it.length <= 2000) draft = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message EDITH…") },
            maxLines = 4,
            enabled = !model.busy,
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            onClick = { model.send(draft) },
            enabled = draft.isNotBlank() && !model.busy,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Send")
        }
    }
    TextButton(
        onClick = model::clearChat,
        enabled = !model.busy,
        modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
    ) {
        Text("Clear conversation", fontSize = 11.sp, color = colors.primary)
    }
}

@Composable
private fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SectionCard(title = title, modifier = modifier, headerAction = headerAction, content = content)
}

@Composable
private fun ControlScreen(model: EdithViewModel) {
    var cityQuery by rememberSaveable { mutableStateOf("") }
    val enabled = !model.busy
    val panel = model.status?.optJSONObject("companion")
    val colors = EdithTheme.colors
    val currentMode = panel?.optString("mode") ?: "face"
    val isDashboard = currentMode == "dashboard"

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. DISPLAY & EXPRESSIONS CARD
        item {
            val report = model.status?.optJSONObject("device")?.optJSONObject("response")
            val reportedMood = report?.optString("mood", "normal") ?: "normal"
            val selectedFace = model.previewMood ?: if(report?.optBoolean("pending") == true) report.optString("desired_mood") else reportedMood
            val device = model.status?.optJSONObject("device")?.optJSONObject("response")

            Panel(
                title = "Display & Expressions",
                headerAction = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            if(isDashboard) "🌙 Dashboard" else "😊 Face: $reportedMood",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary
                        )
                    }
                }
            ) {
                // Screen Mode Switcher
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { model.screen("face") },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            if(!isDashboard) 1.5.dp else 1.dp,
                            if(!isDashboard) colors.primary else colors.border
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if(!isDashboard) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
                            contentColor = if(!isDashboard) colors.primary else colors.textSecondary
                        )
                    ) {
                        Text(
                            "😊 Face Mode",
                            fontSize = 11.sp,
                            fontWeight = if(!isDashboard) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    OutlinedButton(
                        onClick = { model.screen("dashboard") },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            if(isDashboard) 1.5.dp else 1.dp,
                            if(isDashboard) colors.primary else colors.border
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if(isDashboard) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
                            contentColor = if(isDashboard) colors.primary else colors.textSecondary
                        )
                    ) {
                        Text(
                            "🌙 Sleep Dashboard",
                            fontSize = 11.sp,
                            fontWeight = if(isDashboard) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Expressions Grid
                Text("Tap expression to preview on OLED:", fontSize = 11.sp, color = colors.textSecondary)
                listOf(
                    listOf("normal", "happy", "sad", "angry"),
                    listOf("sleepy", "curious", "thinking", "uwu"),
                    listOf("shy", "blush", "unknown")
                ).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { mood ->
                            val isSelected = selectedFace == mood
                            OutlinedButton(
                                onClick = { model.mood(mood) },
                                enabled = enabled,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if(isSelected) colors.primary.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if(isSelected) colors.primary else colors.textSecondary
                                ),
                                border = BorderStroke(
                                    if(isSelected) 1.5.dp else 1.dp,
                                    if(isSelected) colors.primary else colors.border
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    mood,
                                    fontSize = 11.sp,
                                    fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if(device != null) {
                    Text(
                        "Reported face: ${device.optString("mood", "unknown")} · ${if(device.optBoolean("pending")) "command pending" else if(device.optBoolean("applied")) "applied" else "idle"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // 2. CONSOLIDATED TIME TOOLS (Focus Timer, Stopwatch, Alarm)
        item {
            TimeTools(model)
        }

        // 3. WEATHER & LOCATION CARD
        item {
            val weather = panel?.optJSONObject("weather")
            val cityName = panel?.optString("city")?.takeIf { it.isNotBlank() } ?: "No city set"
            val temp = weather?.optDouble("temperature")

            Panel(
                title = "Weather & Location",
                headerAction = {
                    if(temp != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.secondary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                String.format(Locale.ROOT, "%.1f°C", temp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.secondary
                            )
                        }
                    }
                }
            ) {
                // Current Weather Summary Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍 $cityName", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                        if(weather != null) {
                            Text(
                                "${weather.optDouble("temperature")}°C · RH ${weather.optInt("humidity")}%",
                                fontSize = 11.sp,
                                color = colors.primary
                            )
                        }
                    }
                    if(panel != null && panel.has("weather_age")) {
                        Text(
                            "Updated ${panel.optLong("weather_age") / 60}m ago",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                val weatherError = panel?.optString("weather_error", "")
                if(!weatherError.isNullOrBlank() && weatherError != "null") {
                    Text(weatherError, style = MaterialTheme.typography.bodySmall, color = colors.statusOffline)
                }

                // Compact Search Row (TextField + Button on the same row)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { if(it.length <= 80) cityQuery = it },
                        placeholder = { Text("Search city, e.g. Karachi", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = { model.searchCity(cityQuery) },
                        enabled = enabled && cityQuery.trim().length >= 2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Search", fontSize = 11.sp)
                    }
                }

                // Search Results
                if(model.cities.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Matches:", fontSize = 10.sp, color = colors.textSecondary)
                        model.cities.take(3).forEach { city ->
                            SuggestionChip(
                                onClick = { model.chooseCity(city) },
                                enabled = enabled,
                                label = { Text(city.name, fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Text(
                    "Uses your phone's time zone offset. Weather provided by Open-Meteo.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: EdithViewModel) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            android.os.Build.VERSION.SDK_INT < 37 ||
            context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val localPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if(it) model.refresh()
    }
    var address by rememberSaveable { mutableStateOf(model.address) }
    var username by rememberSaveable { mutableStateOf(model.username) }
    // Deliberately not rememberSaveable: credentials do not enter saved instance state.
    var password by remember { mutableStateOf(model.password) }
    var showAuth by rememberSaveable { mutableStateOf(model.username.isNotBlank() || model.password.isNotBlank()) }
    val enabled = !model.busy
    val colors = EdithTheme.colors

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. THEME SELECTION CARD
        item {
            Panel(
                title = "Appearance",
                headerAction = {
                    Text(
                        model.currentTheme.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            ) {
                Text(
                    "Choose an app theme for comfortable viewing:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTheme.values().forEach { theme ->
                        val isSelected = model.currentTheme == theme
                        OutlinedButton(
                            onClick = { model.setTheme(theme) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                if(isSelected) 1.5.dp else 1.dp,
                                if(isSelected) colors.primary else colors.border
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if(isSelected) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
                                contentColor = if(isSelected) colors.primary else colors.textSecondary
                            )
                        ) {
                            Text(
                                theme.displayName,
                                fontSize = 11.sp,
                                fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. BACKEND CONNECTION CARD
        item {
            val isConnected = model.status != null
            Panel(
                title = "Backend Connection",
                headerAction = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if(isConnected) colors.statusOnline else colors.statusOffline).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, (if(isConnected) colors.statusOnline else colors.statusOffline).copy(alpha = 0.3f))
                    ) {
                        Text(
                            if(isConnected) "● Connected" else "● Offline",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if(isConnected) colors.statusOnline else colors.statusOffline
                        )
                    }
                }
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Backend URL", fontSize = 12.sp) },
                    placeholder = { Text("http://10.0.2.2:8000", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick Presets Row
                Text("Presets:", fontSize = 10.sp, color = colors.textSecondary)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { address = "http://192.168.137.1:8000" },
                        label = { Text("Laptop Hotspot", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    SuggestionChip(
                        onClick = { address = "http://192.168.43.100:8000" },
                        label = { Text("Phone Hotspot", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { address = "http://10.0.2.2:8000" },
                        label = { Text("Emulator (10.0.2.2)", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    SuggestionChip(
                        onClick = { address = "http://127.0.0.1:8000" },
                        label = { Text("Localhost", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }

                // Authentication Toggle & Fields
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if(username.isNotBlank()) "Auth: $username" else "Authentication (Optional)",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    TextButton(
                        onClick = { showAuth = !showAuth },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(if(showAuth) "Hide auth" else "Edit auth", fontSize = 11.sp, color = colors.primary)
                    }
                }

                if(showAuth) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Web username", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Web password", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        "Saved securely in Android Keystore. Clear & save to remove.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }

                Button(
                    onClick = { model.saveConnection(address, username, password) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Connect")
                }
            }
        }

        // 3. LOCAL SETUP GUIDE CARD
        item {
            Panel(title = "Local Setup Guide") {
                if(!permissionGranted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.statusOffline.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, colors.statusOffline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Local Network Permission Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.statusOffline
                            )
                            Text(
                                "Android 17 needs local network permission to connect to your PC.",
                                fontSize = 10.sp,
                                color = colors.textPrimary
                            )
                            Button(
                                onClick = { localPermission.launch("android.permission.ACCESS_LOCAL_NETWORK") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Allow Local Network", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Laptop Hotspot", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = colors.textPrimary)
                        Text("http://192.168.137.1:8000", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.primary)
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.5.dp)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Phone Hotspot", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = colors.textPrimary)
                        Text("http://192.168.43.100:8000", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.primary)
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.5.dp)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Home Wi-Fi", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = colors.textPrimary)
                        Text("http://<PC-LAN-IP>:8000", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.primary)
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.5.dp)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Emulator", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = colors.textPrimary)
                        Text("http://10.0.2.2:8000", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.primary)
                    }
                }

                Text(
                    "Start backend with 0.0.0.0:8000. Windows Mobile Hotspot IP is always 192.168.137.1.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = colors.textSecondary
                )
            }
        }

        // 4. MODEL & DIAGNOSTICS CARD
        item {
            val modelName = model.status?.optString("lmstudio_model")?.takeIf { it.isNotBlank() && it != "null" }
                ?: model.modelStatus?.optString("selected")?.takeIf { it.isNotBlank() && it != "null" }
                ?: "Model not checked"
            val isModelReady = model.modelStatus?.optBoolean("ok") == true

            Panel(
                title = "AI Model & Diagnostics",
                headerAction = {
                    OutlinedButton(
                        onClick = model::checkModel,
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Check LM Studio", fontSize = 10.sp)
                    }
                }
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LM Studio Model:", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        modelName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if(isModelReady) colors.primary else colors.textPrimary
                    )
                }
                if(model.notice.isNotBlank()) {
                    Text(
                        model.notice,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // 5. PRIVACY & LOCAL DATA CARD
        item {
            Panel(title = "Privacy & Data") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• Chat history stored in local device memory only until cleared.", fontSize = 10.sp, color = colors.textSecondary)
                    Text("• Context window sends only the last 6 exchanges to your backend.", fontSize = 10.sp, color = colors.textSecondary)
                    Text("• Microphone, background listening, and audio recording are disabled.", fontSize = 10.sp, color = colors.textSecondary)
                }
            }
        }
    }
}
