package com.qzainsami.edith

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BootScreen() {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit){while(frame<40){delay(35);frame++}}
    val green=Color(0xFFC9F575)
    Box(Modifier.fillMaxSize().background(Color(0xFF080E14)),contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
            for(i in 0..30){
                val y=size.height*i/30
                drawLine(Color(0xFF16202B),Offset(0f,y),Offset(size.width,y),1f)
            }
            val scan=(frame/40f)*size.height
            drawLine(green.copy(alpha=.3f),Offset(0f,scan),Offset(size.width,scan),2f)
        }
        Column(Modifier.fillMaxWidth().padding(40.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(18.dp)){
            Text("[ COMPANION CORE ]",fontFamily=FontFamily.Monospace,color=green,letterSpacing=3.sp,fontSize=12.sp)
            Box(contentAlignment=Alignment.Center){
                if(frame in 8..25 && frame%7<2) Text("EDITH",fontSize=58.sp,fontWeight=FontWeight.Black,
                    letterSpacing=7.sp,color=Color(0xFF53DDE8),modifier=Modifier.offset(x=3.dp))
                Text("EDITH",fontSize=58.sp,fontWeight=FontWeight.Black,letterSpacing=7.sp,
                    color=Color.White,modifier=Modifier.offset(x=if(frame in 8..25 && frame%7<2)(-2).dp else 0.dp))
            }
            Text(when{frame<14->"SIGNAL ACQUIRED";frame<28->"SYNCING NEURAL CORE";else->"SYSTEM READY"},
                color=green,fontFamily=FontFamily.Monospace,fontSize=12.sp)
            LinearProgressIndicator(progress={frame/40f},modifier=Modifier.fillMaxWidth().height(3.dp),color=green)
            Text("DESKTOP COMPANION // ONLINE INTERFACE",fontFamily=FontFamily.Monospace,
                fontSize=9.sp,color=Color(0xFF8395A8))
        }
    }
}
