package com.example.clocklivewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ClockWallpaperApp()
                }
            }
        }
    }
}

@Composable
fun ClockWallpaperApp() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Clock Wallpaper Interactive Live Preview
        ClockWallpaperPreview(modifier = Modifier.fillMaxSize())

        // Bottom Action Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.wallpaper_instructions),
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, MinimalistClockWallpaperService::class.java)
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF222226),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(id = R.string.set_wallpaper),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ClockWallpaperPreview(modifier: Modifier = Modifier) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val cx = width * 0.5f
        val cy = height * 0.58f
        val rx = width * 0.40f
        val tiltFactor = 0.36f
        val ry = rx * tiltFactor

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }

        val millis = calendar[Calendar.MILLISECOND]
        val sec = calendar[Calendar.SECOND] + (millis / 1000f)
        val min = calendar[Calendar.MINUTE] + (sec / 60f)
        val hour = (calendar[Calendar.HOUR] % 12) + (min / 60f)

        val hourAngle = hour * 30.0f
        val minAngle = min * 6.0f
        val secAngle = sec * 6.0f

        val hourLen = rx * 0.52f
        val minLen = rx * 0.82f
        val secLen = rx * 0.90f

        // 1. Render exactly 4 evenly spaced vertical light strands ONLY on Second Hand
        val rad = Math.toRadians((secAngle - 90).toDouble())
        val endX = cx + secLen * cos(rad).toFloat()
        val endY = cy + secLen * sin(rad).toFloat() * tiltFactor

        val strandCount = 4
        val alphas = intArrayOf(180, 220, 255, 230)
        val strokeWidthsPx = floatArrayOf(3.2f, 4.8f, 5.5f, 3.8f)

        for (i in 0 until strandCount) {
            val t = (i + 1).toFloat() / strandCount
            val px = cx + t * (endX - cx)
            val py = cy + t * (endY - cy)

            val alpha = alphas[i]
            val strokeW = strokeWidthsPx[i]

            val brush = Brush.verticalGradient(
                colors = listOf(
                    Color(245, 248, 255, 0),
                    Color(225, 235, 250, (alpha * 0.7f).toInt()),
                    Color(245, 248, 255, alpha)
                ),
                startY = 0f,
                endY = py
            )

            drawLine(
                brush = brush,
                start = Offset(px, py),
                end = Offset(px, 0f),
                strokeWidth = strokeW
            )
        }

        // 2. 3D Perspective Ticks
        val hourTickLen = 32f
        val minTickLen = 16f

        for (i in 0 until 60) {
            val angleDeg = i * 6.0
            val radTick = Math.toRadians(angleDeg - 90)
            val cosVal = cos(radTick).toFloat()
            val sinVal = sin(radTick).toFloat()

            val isHourTick = i % 5 == 0
            val tickLen = if (isHourTick) hourTickLen else minTickLen

            val outerX = cx + rx * cosVal
            val outerY = cy + ry * sinVal

            val innerX = cx + (rx - tickLen) * cosVal
            val innerY = cy + (ry - tickLen * tiltFactor) * sinVal

            drawLine(
                color = if (isHourTick) Color(0xFFFFFFFF) else Color(0x90DDDDDD),
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = if (isHourTick) 5.5f else 3.0f,
                cap = StrokeCap.Round
            )
        }

        // 3. 3D Perspective Clock Hands
        fun drawHand(angleDeg: Float, length: Float, strokeW: Float, color: Color) {
            val radHand = Math.toRadians((angleDeg - 90).toDouble())
            val cosVal = cos(radHand).toFloat()
            val sinVal = sin(radHand).toFloat()

            val stopX = cx + length * cosVal
            val stopY = cy + length * sinVal * tiltFactor

            val tailLen = length * 0.14f
            val startX = cx - tailLen * cosVal
            val startY = cy - tailLen * sinVal * tiltFactor

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(stopX, stopY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        drawHand(hourAngle, hourLen, 9f, Color(0xFFFFFFFF))
        drawHand(minAngle, minLen, 6f, Color(0xFFFFFFFF))
        drawHand(secAngle, secLen, 3.5f, Color(0xE0E8F0FF))

        // Center Pivot Hub
        drawCircle(
            color = Color.White,
            center = Offset(cx, cy),
            radius = 12f
        )
        drawCircle(
            color = Color.Black,
            center = Offset(cx, cy),
            radius = 4f
        )
    }
}
