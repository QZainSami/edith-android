package com.qzainsami.edith

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun CompanionHeader(model: EdithViewModel) {
    val backendOnline = model.status != null
    val device = model.status?.optJSONObject("device")
    val espOnline = backendOnline && device?.optBoolean("ok") == true
    val modelInfo = model.modelStatus
    val selected = modelInfo?.optString("selected").orEmpty()
    val models = modelInfo?.optJSONArray("models")
    val loaded = selected.isNotBlank() && models != null &&
        (0 until models.length()).any { models.optString(it) == selected }
    val modelOnline = backendOnline && modelInfo?.optBoolean("ok") == true && loaded && modelInfo.optString("mode") == "auto"
    val modelLabel = when {
        !backendOnline || modelInfo == null -> "Unknown"
        modelInfo.optString("mode") == "off" -> "Off"
        !modelInfo.optBoolean("ok") -> "Offline"
        !loaded -> "Not loaded"
        else -> "Online"
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HealthLight("Backend", if(backendOnline) "Online" else "Offline", backendOnline, Modifier.weight(1f))
            HealthLight("Model", modelLabel, modelOnline, Modifier.weight(1f))
            HealthLight("ESP32", if(espOnline) "Online" else "Offline", espOnline, Modifier.weight(1f))
        }
        val response = device?.optJSONObject("response")
        val reportedMood = response?.optString("mood", "normal") ?: "normal"
        val pending = response?.optBoolean("pending") == true
        val mood = model.previewMood ?: if(pending) response?.optString("desired_mood", reportedMood) ?: reportedMood else reportedMood
        val panel = model.status?.optJSONObject("companion")
        val requestedMode = model.previewMode ?: panel?.optString("mode") ?: "face"
        val dashboard = requestedMode == "dashboard"
        val syncing = model.previewMood != null || model.previewMode != null ||
            mood != reportedMood || (response?.has("reported_mode") == true && requestedMode != response.optString("reported_mode"))
        val timer = (panel?.optLong("timer_remaining_ms", -1) ?: -1) >= 0
        var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        val received = remember(panel) { SystemClock.elapsedRealtime() }
        LaunchedEffect(Unit) { while(true) { tick = SystemClock.elapsedRealtime(); delay(100) } }
        val speaking = espOnline && !dashboard && !timer &&
            (panel?.optLong("speech_remaining_ms", 0) ?: 0) > tick - received
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if(espOnline && (timer || dashboard)) DashboardPreview(panel, timer, tick - received)
            else OledFace(mood, espOnline, speaking, tick)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("EDITH display", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(when { !espOnline -> "Unavailable"; timer -> "Timer"; dashboard -> "Sleep dashboard"; else -> mood.replaceFirstChar { it.uppercase() } },
                    color = if(espOnline) MaterialTheme.colorScheme.primary else Color(0xFFFF8585), fontSize = 13.sp)
                Text(when {
                    !espOnline -> "Waiting for ESP32"
                    syncing -> "Syncing with OLED…"
                    timer || dashboard -> "Display preview"
                    speaking -> "Speaking animation"
                    else -> "Reported expression"
                }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardPreview(panel: JSONObject?, timer: Boolean, elapsed: Long) {
    val initialRemaining = remember(panel?.optString("timer_id")) {
        (panel?.optLong("timer_remaining_ms",1) ?: 1).coerceAtLeast(1)
    }
    Canvas(Modifier.size(128.dp,64.dp).background(Color.Black,RoundedCornerShape(8.dp))
        .semantics { contentDescription = if(timer) "EDITH countdown preview" else "EDITH clock and weather dashboard preview" }) {
        withTransform({ scale(size.width/128f,size.height/64f,Offset.Zero) }) {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                typeface = android.graphics.Typeface.MONOSPACE
            }
            fun text(value: String, x: Float, y: Float, font: Float) {
                paint.textSize = font
                drawContext.canvas.nativeCanvas.drawText(value,x,y,paint)
            }
            if(timer) {
                val remaining = ((panel?.optLong("timer_remaining_ms",0) ?: 0)-elapsed).coerceAtLeast(0)
                val seconds = (remaining+999)/1000
                val value = if(seconds >= 3600) java.lang.String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60)
                    else java.lang.String.format(java.util.Locale.ROOT,"%02d:%02d",seconds/60,seconds%60)
                val title = if(remaining == 0L) "TIME UP" else "FOCUS TIMER"
                paint.textSize = 7f
                text(title,(128-paint.measureText(title))/2,12f,7f)
                val font = if(seconds>=3600) 20f else 29f
                paint.textSize = font
                text(value,(128-paint.measureText(value))/2,43f,font)
                drawRoundRect(Color.White,Offset(14f,53f),Size(100f,5f),CornerRadius(2f),style=Stroke(1f))
                val width = (98f*remaining/initialRemaining.toFloat()).coerceIn(0f,98f)
                if(width>0) drawRect(Color.White,Offset(15f,54f),Size(width,3f))
            } else {
                val zone = java.util.SimpleTimeZone((panel?.optInt("utc_offset",0) ?: 0)*60000,"EDITH")
                val epoch = ((panel?.optLong("now",0) ?: 0)*1000)+elapsed
                val date = java.util.Date(epoch)
                val dateFormat = java.text.SimpleDateFormat("EEE dd MMM",java.util.Locale.ENGLISH).apply { timeZone=zone }
                val timeFormat = java.text.SimpleDateFormat("HH:mm",java.util.Locale.ROOT).apply { timeZone=zone }
                text(dateFormat.format(date),4f,10f,7f)
                drawLine(Color.White,Offset(0f,13f),Offset(128f,13f),1f)
                drawLine(Color.White,Offset(83f,14f),Offset(83f,54f),1f)
                text(timeFormat.format(date),3f,40f,25f)
                val weather = panel?.optJSONObject("weather")
                text("WEATHER",87f,24f,6f)
                text(if(weather==null) "--" else java.lang.String.format(java.util.Locale.ROOT,"%.1fC",weather.optDouble("temperature")),87f,37f,7f)
                text(if(weather==null) "--" else "RH ${weather.optInt("humidity")}%",87f,49f,6f)
                text(panel?.optString("city", "Set city")?.take(18).orEmpty().ifBlank { "Set city" },4f,51f,6f)
                text("RESTING  zzz",30f,61f,6f)
            }
        }
    }
}

@Composable
private fun HealthLight(name: String, state: String, online: Boolean, modifier: Modifier) {
    val color = if(online) Color(0xFFC9F575) else Color(0xFFFF8585)
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)).padding(8.dp)) {
        Text(name, style = MaterialTheme.typography.labelMedium)
        Text("● $state", color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** A vector expression preview, not a streamed screenshot of the physical display. */
@Composable
private fun OledFace(mood: String, online: Boolean, speaking: Boolean, tick: Long) {
    val description = if(online) "EDITH $mood face${if(speaking) ", speaking" else ""}" else "EDITH face unavailable"
    Canvas(Modifier.size(128.dp, 64.dp).background(Color.Black, RoundedCornerShape(8.dp))
        .semantics { contentDescription = description }) {
        withTransform({ scale(size.width / 128f, size.height / 64f, Offset.Zero) }) {
            val ink = if(online) Color.White else Color(0xFF616B78)
            if(!online) {
                drawLine(ink, Offset(28f,32f), Offset(47f,32f), 3f)
                drawLine(ink, Offset(81f,32f), Offset(100f,32f), 3f)
                return@withTransform
            }
            val blink = mood in listOf("normal", "curious") && tick % 4500 < 130
            fun eye(x: Float, y: Float = 13f, w: Float = 30f, h: Float = 29f) {
                drawRoundRect(ink, Offset(x,y), Size(w,h), CornerRadius(8f,8f))
            }
            if(blink) {
                drawLine(ink, Offset(22f,30f),Offset(50f,30f),4f)
                drawLine(ink, Offset(78f,30f),Offset(106f,30f),4f)
            } else when(mood) {
                "sleepy" -> {
                    eye(27f,43f,26f,8f); eye(75f,43f,26f,8f)
                    for(i in 0..2) {
                        val x = 104f+i*5; val y = 27f-i*8
                        linePath(ink, listOf(Offset(x,y),Offset(x+4,y),Offset(x,y+4),Offset(x+4,y+4)),1.3f)
                    }
                }
                "happy", "uwu" -> {
                    for(x in listOf(23f,79f)) {
                        val path = Path().apply { moveTo(x,33f); quadraticTo(x+14,8f,x+28,33f) }
                        drawPath(path,ink,style=Stroke(4f))
                    }
                }
                "sad" -> {
                    for(x in listOf(22f,78f)) {
                        val path = Path().apply { moveTo(x,29f); quadraticTo(x+14,44f,x+28,29f) }
                        drawPath(path,ink,style=Stroke(4f))
                    }
                }
                "angry" -> {
                    eye(22f,22f,28f,21f); eye(78f,22f,28f,21f)
                    drawLine(Color.Black,Offset(19f,19f),Offset(53f,29f),11f)
                    drawLine(Color.Black,Offset(75f,29f),Offset(109f,19f),11f)
                }
                "curious" -> { eye(21f,10f,31f,35f); eye(82f,22f,21f,22f) }
                "thinking" -> {
                    eye(22f); eye(78f)
                    drawCircle(Color.Black,5f,Offset(43f,28f)); drawCircle(Color.Black,5f,Offset(99f,28f))
                }
                else -> { eye(22f); eye(78f) }
            }
            when {
                speaking -> {
                    val h = listOf(3f,7f,4f,9f,3f,6f)[((tick/110)%6).toInt()]
                    drawRoundRect(ink,Offset(56f,52f-h/2),Size(16f,h),CornerRadius(3f,3f),style=Stroke(2f))
                }
                mood == "uwu" -> linePath(ink,listOf(Offset(55f,49f),Offset(59f,55f),Offset(64f,50f),Offset(69f,55f),Offset(73f,49f)),2f)
                mood == "sad" || mood == "angry" -> {
                    val p = Path().apply { moveTo(56f,55f); quadraticTo(64f,47f,72f,55f) }
                    drawPath(p,ink,style=Stroke(2f))
                }
                mood == "thinking" -> drawLine(ink,Offset(58f,52f),Offset(70f,52f),2f)
                mood == "curious" -> drawCircle(ink,4f,Offset(64f,52f),style=Stroke(2f))
                else -> {
                    val p = Path().apply { moveTo(56f,52f); quadraticTo(64f,59f,72f,52f) }
                    drawPath(p,ink,style=Stroke(2f))
                }
            }
        }
    }
}

private fun DrawScope.linePath(ink: Color, points: List<Offset>, width: Float) {
    val p = Path().apply { moveTo(points[0].x,points[0].y); points.drop(1).forEach { lineTo(it.x,it.y) } }
    drawPath(p,ink,style=Stroke(width))
}
