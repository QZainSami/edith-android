package com.qzainsami.edith

import android.app.TimePickerDialog
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TimeTools(model: EdithViewModel) {
    val state=model.status?.optJSONObject("companion")
    val context=LocalContext.current
    var elapsed by remember { mutableLongStateOf(0) }
    var alarmRemaining by remember { mutableLongStateOf(-1) }
    val running=state?.optBoolean("stopwatch_running")==true
    val supported=state?.has("stopwatch_elapsed_ms")==true
    val enabled=!model.busy && supported
    LaunchedEffect(state) {
        val received=SystemClock.elapsedRealtime()
        while(true) {
            val delta=SystemClock.elapsedRealtime()-received
            elapsed=((state?.optLong("stopwatch_elapsed_ms",0)?:0)+if(running)delta else 0).coerceAtMost(359999990)
            val remaining=state?.optLong("alarm_remaining_ms",-1)?:-1
            alarmRemaining=if(remaining<0)-1 else (remaining-delta).coerceAtLeast(0)
            delay(40)
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        SectionCard("Stopwatch") {
                val seconds=elapsed/1000
                Text(if(seconds>=3600) String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60)
                    else String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),fontSize=40.sp,fontWeight=FontWeight.Light,fontFamily=FontFamily.Monospace)
                Text(String.format(Locale.ROOT,".%02d   %s",elapsed/10%100,if(running)"RUNNING" else "PAUSED"),
                    color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={model.stopwatch(if(running)"pause" else "start")},enabled=enabled) {
                        Text(if(running)"Pause" else if(elapsed>0)"Resume" else "Start")
                    }
                    OutlinedButton(onClick={model.stopwatch("reset")},enabled=enabled){Text("Reset")}
                }
                TextButton(onClick={model.screen("stopwatch")},enabled=enabled){Text("Show stopwatch on OLED")}
                Text("Continues on the backend/ESP32 when the app closes. Pausing keeps elapsed time; reset clears it. Limit: 99:59:59.99.",style=MaterialTheme.typography.bodySmall)
                if(!supported)Text("Restart the updated backend to enable stopwatch and alarms.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
        SectionCard("One-time alarm") {
                val at=state?.optLong("alarm_at",0)?:0
                Text(when {
                    alarmRemaining==0L->"ALARM ✦"
                    alarmRemaining<0->"No alarm"
                    else->java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(java.util.Date(at*1000))
                },fontSize=36.sp,color=if(alarmRemaining==0L)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                if(alarmRemaining>0) Text(java.text.SimpleDateFormat("EEE, dd MMM",Locale.getDefault()).format(java.util.Date(at*1000)),style=MaterialTheme.typography.labelLarge)
                Button(onClick={
                    val now=java.util.Calendar.getInstance()
                    TimePickerDialog(context,{_,hour,minute->model.setAlarm(hour,minute)},
                        now.get(java.util.Calendar.HOUR_OF_DAY),now.get(java.util.Calendar.MINUTE),true).show()
                },enabled=enabled,modifier=Modifier.fillMaxWidth()){Text("Choose alarm time")}
                OutlinedButton(onClick=model::dismissAlarm,enabled=enabled && alarmRemaining>=0,
                    modifier=Modifier.fillMaxWidth()){Text(if(alarmRemaining==0L)"Dismiss alarm" else "Cancel alarm")}
                Text("Uses your phone's time zone. A past time schedules tomorrow. Visual alert on OLED until dismissed; no sound or Android background notification yet.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
