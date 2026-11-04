package com.qzainsami.edith

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

val EdithLime=Color(0xFFC9F575)
val EdithCyan=Color(0xFF68DDE6)
val EdithInk=Color(0xFF09121B)
val EdithPanel=Color(0xFF14222E)
val EdithLine=Color(0xFF293C4A)

@Composable
fun EdithTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(
        primary=EdithLime,onPrimary=EdithInk,primaryContainer=Color(0xFF293C24),onPrimaryContainer=Color(0xFFE2F8CF),
        secondary=EdithCyan,onSecondary=EdithInk,secondaryContainer=Color(0xFF153B44),onSecondaryContainer=Color(0xFFC2F5F5),
        background=EdithInk,onBackground=Color(0xFFE6EEF1),surface=EdithPanel,onSurface=Color(0xFFE6EEF1),
        surfaceVariant=Color(0xFF1A2B38),onSurfaceVariant=Color(0xFF9CB1BC),
        surfaceContainer=EdithPanel,surfaceContainerLow=Color(0xFF10202B),surfaceContainerHigh=Color(0xFF1A2B38),
        surfaceContainerHighest=Color(0xFF213441),surfaceContainerLowest=EdithInk,
        outline=Color(0xFF536B77),outlineVariant=EdithLine,error=Color(0xFFFF9299),
        errorContainer=Color(0xFF41242B),onErrorContainer=Color(0xFFFFDADC)
    ),shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp)),
        typography=Typography(
            titleLarge=Typography().titleLarge.copy(fontWeight=FontWeight.SemiBold),
            titleMedium=Typography().titleMedium.copy(fontWeight=FontWeight.SemiBold),
            labelSmall=Typography().labelSmall.copy(letterSpacing=.3.sp)
        ),content=content)
}

@Composable
fun EdithLogo(modifier:Modifier=Modifier) {
    Canvas(modifier.background(EdithInk,RoundedCornerShape(14.dp))) {
        withTransform({scale(size.width/108f,size.height/108f,Offset.Zero)}) {
            drawRoundRect(EdithCyan,Offset(23f,28f),Size(62f,50f),CornerRadius(13f),style=Stroke(3f))
            drawLine(EdithCyan,Offset(54f,28f),Offset(54f,21f),3f)
            drawCircle(EdithLime,3.5f,Offset(54f,19f))
            drawRoundRect(EdithLime,Offset(34f,43f),Size(13f,14f),CornerRadius(4f))
            drawRoundRect(EdithLime,Offset(61f,43f),Size(13f,14f),CornerRadius(4f))
            val mouth=Path().apply{moveTo(45f,64f);quadraticTo(54f,72f,63f,64f)}
            drawPath(mouth,EdithLime,style=Stroke(3f))
        }
    }
}

@Composable
fun NavGlyph(index:Int,selected:Boolean) {
    val ink=if(selected)EdithLime else Color(0xFF8EA5B2)
    Canvas(Modifier.size(22.dp)) {
        withTransform({scale(size.width/24f,size.height/24f,Offset.Zero)}) {
            when(index) {
                0->{drawRoundRect(ink,Offset(3f,3f),Size(18f,14f),CornerRadius(4f),style=Stroke(1.6f))
                    drawLine(ink,Offset(6f,17f),Offset(6f,21f),1.6f)
                    drawLine(ink,Offset(6f,21f),Offset(11f,17f),1.6f)
                    for(x in listOf(8f,12f,16f))drawCircle(ink,.8f,Offset(x,10f))}
                1->{for(i in 0..2){val x=5f+i*7;val y=if(i==1)15f else 8f
                    drawLine(ink,Offset(x,3f),Offset(x,21f),1.6f)
                    drawCircle(EdithInk,3f,Offset(x,y));drawCircle(ink,2.5f,Offset(x,y),style=Stroke(1.6f))}}
                else->{drawCircle(ink,6f,Offset(12f,12f),style=Stroke(1.6f));drawCircle(ink,2f,Offset(12f,12f),style=Stroke(1.6f))
                    for(i in 0..7){val a=i*Math.PI/4;drawLine(ink,Offset(12f+7f*cos(a).toFloat(),12f+7f*sin(a).toFloat()),
                        Offset(12f+9f*cos(a).toFloat(),12f+9f*sin(a).toFloat()),1.6f)}}
            }
        }
    }
}

@Composable
fun SectionCard(title:String, content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=EdithPanel,border=BorderStroke(1.dp,EdithLine)) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(3.dp).height(18.dp).background(EdithCyan,RoundedCornerShape(2.dp)))
                Text(title,style=MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
