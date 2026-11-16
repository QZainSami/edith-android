package com.qzainsami.edith

import android.app.TimePickerDialog
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimeTools(model: EdithViewModel) {
    val state = model.status?.optJSONObject("companion")
    val context = LocalContext.current
    val colors = EdithTheme.colors
    val enabled = !model.busy

    var selectedTool by rememberSaveable { mutableIntStateOf(0) } // 0: Timer, 1: Stopwatch, 2: Alarm
    var customDuration by rememberSaveable { mutableStateOf("5") }
    var unitIndex by rememberSaveable { mutableIntStateOf(1) } // 0: Sec, 1: Min, 2: Hr

    val running = state?.optBoolean("stopwatch_running") == true
    val stopwatchSupported = state?.has("stopwatch_elapsed_ms") == true

    var elapsedStopwatch by remember { mutableLongStateOf(0) }
    var alarmRemaining by remember { mutableLongStateOf(-1) }
    var timerRemaining by remember { mutableLongStateOf(-1) }

    LaunchedEffect(state) {
        val received = SystemClock.elapsedRealtime()
        val timerStartRemaining = state?.optLong("timer_remaining_ms", -1) ?: -1
        val alarmStartRemaining = state?.optLong("alarm_remaining_ms", -1) ?: -1
        while(true) {
            val delta = SystemClock.elapsedRealtime() - received
            elapsedStopwatch = ((state?.optLong("stopwatch_elapsed_ms", 0) ?: 0) + if(running) delta else 0).coerceAtMost(359999990)
            alarmRemaining = if(alarmStartRemaining < 0) -1 else (alarmStartRemaining - delta).coerceAtLeast(0)
            timerRemaining = if(timerStartRemaining < 0) -1 else (timerStartRemaining - delta).coerceAtLeast(0)
            delay(50)
        }
    }

    val activeBadge = when {
        timerRemaining > 0 -> {
            val s = (timerRemaining + 999) / 1000
            if (s >= 3600) String.format(Locale.ROOT, "⏳ %02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
            else String.format(Locale.ROOT, "⏳ %02d:%02d", s / 60, s % 60)
        }
        running -> "⏱ Running"
        alarmRemaining > 0 -> "⏰ Alarm Set"
        else -> null
    }

    SectionCard(
        title = "Time Tools",
        headerAction = {
            activeBadge?.let {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary
                    )
                }
            }
        }
    ) {
        // Segmented Switcher for Timer, Stopwatch, Alarm
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("Focus Timer", "Stopwatch", "Alarm")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTool == index
                val hasActive = when(index) {
                    0 -> timerRemaining > 0
                    1 -> running || elapsedStopwatch > 0
                    2 -> alarmRemaining > 0
                    else -> false
                }
                OutlinedButton(
                    onClick = { selectedTool = index },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if(hasActive) {
                            Text("●", fontSize = 8.sp, color = colors.accentPink)
                        }
                        Text(
                            title,
                            fontSize = 11.sp,
                            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        when(selectedTool) {
            0 -> {
                // FOCUS TIMER TAB
                val timerActive = timerRemaining >= 0
                val timerText = when {
                    timerRemaining < 0 -> "No active timer"
                    timerRemaining == 0L -> "Time up ✦"
                    else -> {
                        val s = (timerRemaining + 999) / 1000
                        if (s >= 3600) String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                        else String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60)
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        timerText,
                        fontSize = if (timerActive) 28.sp else 16.sp,
                        fontWeight = if (timerActive) FontWeight.Light else FontWeight.Medium,
                        fontFamily = if (timerActive) FontFamily.Monospace else FontFamily.Default,
                        color = if (timerRemaining == 0L) colors.secondary else colors.textPrimary
                    )
                    if(timerActive) {
                        OutlinedButton(
                            onClick = model::cancelTimer,
                            enabled = enabled,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = colors.statusOffline)
                        }
                    }
                }

                // Quick Preset Chips (1-tap start)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Presets:", fontSize = 10.sp, color = colors.textSecondary)
                    listOf(1 to "1m", 5 to "5m", 15 to "15m", 25 to "25m").forEach { (mins, label) ->
                        SuggestionChip(
                            onClick = { model.timer(mins.toString(), 60) },
                            enabled = enabled,
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Custom Duration Row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customDuration,
                        onValueChange = { if(it.length <= 5 && it.all(Char::isDigit)) customDuration = it },
                        label = { Text("Duration", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("s", "m", "h").forEachIndexed { index, unitLabel ->
                            FilterChip(
                                selected = unitIndex == index,
                                onClick = { unitIndex = index },
                                label = { Text(unitLabel, fontSize = 11.sp) },
                                enabled = enabled,
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { model.timer(customDuration, listOf(1, 60, 3600)[unitIndex]) },
                        enabled = enabled && customDuration.isNotBlank() && (customDuration.toIntOrNull() ?: 0) > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Start", fontSize = 11.sp)
                    }
                }
            }
            1 -> {
                // STOPWATCH TAB
                val seconds = elapsedStopwatch / 1000
                val display = if (seconds >= 3600) String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                else String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
                val millis = String.format(Locale.ROOT, ".%02d", elapsedStopwatch / 10 % 100)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            display,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                        Text(
                            millis,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        if(running) "RUNNING" else if(elapsedStopwatch > 0) "PAUSED" else "IDLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if(running) colors.statusOnline else colors.textSecondary
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { model.stopwatch(if(running) "pause" else "start") },
                        enabled = enabled && stopwatchSupported,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if(running) "Pause" else if(elapsedStopwatch > 0) "Resume" else "Start")
                    }
                    OutlinedButton(
                        onClick = { model.stopwatch("reset") },
                        enabled = enabled && stopwatchSupported && elapsedStopwatch > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset")
                    }
                    OutlinedButton(
                        onClick = { model.screen("stopwatch") },
                        enabled = enabled && stopwatchSupported,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Show on OLED", fontSize = 11.sp)
                    }
                }
                if(!stopwatchSupported) {
                    Text(
                        "Restart updated backend to enable stopwatch & alarm.",
                        color = colors.statusOffline,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            2 -> {
                // ALARM TAB
                val at = state?.optLong("alarm_at", 0) ?: 0
                val alarmSet = alarmRemaining >= 0
                val alarmText = when {
                    alarmRemaining == 0L -> "ALARM ✦ (Triggered)"
                    alarmRemaining > 0 -> {
                        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at * 1000))
                        val formattedDate = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(at * 1000))
                        "$formattedTime ($formattedDate)"
                    }
                    else -> "No alarm set"
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            alarmText,
                            fontSize = if (alarmSet) 20.sp else 15.sp,
                            fontWeight = if (alarmSet) FontWeight.SemiBold else FontWeight.Normal,
                            color = if(alarmRemaining == 0L) colors.secondary else colors.textPrimary
                        )
                        if(alarmRemaining > 0) {
                            val hours = alarmRemaining / 3600000
                            val mins = (alarmRemaining / 60000) % 60
                            Text(
                                "Rings in ${hours}h ${mins}m",
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> model.setAlarm(hour, minute) },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Choose Time", fontSize = 11.sp)
                    }
                    if(alarmSet) {
                        OutlinedButton(
                            onClick = model::dismissAlarm,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if(alarmRemaining == 0L) "Dismiss" else "Cancel", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
