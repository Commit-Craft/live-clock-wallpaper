package com.example.clocklivewallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MinimalistClockWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ClockEngine()
    }

    private inner class ClockEngine : Engine() {
        private val mainHandler = Handler(Looper.getMainLooper())
        private var isWallpaperVisible = false

        // Background Paint (Pure AMOLED Black)
        private val bgPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Clock Tick Paints
        private val hourTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val minuteTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 220, 220, 220)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        // Clock Hand Paints
        private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val minuteHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 245, 248, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val centerHubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        private val centerInnerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Vertical Light Trail Paint
        private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val drawRunnable = Runnable {
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isWallpaperVisible = visible
            if (visible) {
                drawFrame()
            } else {
                mainHandler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            this.isWallpaperVisible = false
            mainHandler.removeCallbacks(drawRunnable)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderWallpaper(canvas)
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // Ignore surface release exception
                    }
                }
            }

            mainHandler.removeCallbacks(drawRunnable)
            if (isWallpaperVisible) {
                // Target smooth ~60 FPS update interval (16ms)
                mainHandler.postDelayed(drawRunnable, 16)
            }
        }

        private fun renderWallpaper(canvas: Canvas) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            if (width <= 0f || height <= 0f) return

            // Pure AMOLED Black Background
            canvas.drawRect(0f, 0f, width, height, bgPaint)

            // Scale factor based on reference design
            val density = width / 1080f
            val dp = density * 2.7f

            // Clock face center & 3D perspective constants matching reference image
            val cx = width * 0.5f
            val cy = height * 0.63f
            val rx = width * 0.40f // Horizontal radius
            val tiltFactor = 0.36f  // Perspective tilt compressing Y axis into 3D plane
            val ry = rx * tiltFactor

            // Real-time system clock calculation
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            val millis = calendar[Calendar.MILLISECOND]
            val sec = calendar[Calendar.SECOND] + (millis / 1000f)
            val min = calendar[Calendar.MINUTE] + (sec / 60f)
            val hour = (calendar[Calendar.HOUR] % 12) + (min / 60f)

            // Hand Angles (0° = 12 o'clock / straight up)
            val hourAngle = hour * 30.0f
            val minAngle = min * 6.0f
            val secAngle = sec * 6.0f

            // Hand lengths on 3D disc plane
            val hourLen = rx * 0.52f
            val minLen = rx * 0.82f
            val secLen = rx * 0.90f

            // 1. Render exactly 4 evenly spaced vertical light strands ONLY along the Second Hand
            drawSecondHandFourStrands(
                canvas = canvas,
                cx = cx,
                cy = cy,
                tiltFactor = tiltFactor,
                secAngle = secAngle,
                secLen = secLen,
                dp = dp
            )

            // 2. Render 3D Perspective Clock Ticks
            drawPerspectiveClockFace(
                canvas = canvas,
                cx = cx,
                cy = cy,
                rx = rx,
                ry = ry,
                tiltFactor = tiltFactor,
                dp = dp
            )

            // 3. Render Clock Hands in 3D Perspective
            drawPerspectiveClockHands(
                canvas = canvas,
                cx = cx,
                cy = cy,
                tiltFactor = tiltFactor,
                hourAngle = hourAngle,
                hourLen = hourLen,
                minAngle = minAngle,
                minLen = minLen,
                secAngle = secAngle,
                secLen = secLen,
                dp = dp
            )
        }

        private fun drawSecondHandFourStrands(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            tiltFactor: Float,
            secAngle: Float,
            secLen: Float,
            dp: Float
        ) {
            val rad = Math.toRadians((secAngle - 90).toDouble())
            val endX = cx + secLen * cos(rad).toFloat()
            val endY = cy + secLen * sin(rad).toFloat() * tiltFactor

            // Exactly 4 evenly spaced strands along second hand (t = 0.25, 0.50, 0.75, 1.00)
            val strandCount = 4
            val alphas = intArrayOf(180, 220, 255, 230)
            val strokeWidthsDp = floatArrayOf(1.2f, 1.8f, 2.2f, 1.5f)

            for (i in 0 until strandCount) {
                val t = (i + 1).toFloat() / strandCount
                val px = cx + t * (endX - cx)
                val py = cy + t * (endY - cy)

                val alpha = alphas[i]
                val strokeW = strokeWidthsDp[i] * dp

                // Vertical linear gradient from second hand point straight up to top of screen
                val shader = LinearGradient(
                    px, py, px, 0f,
                    intArrayOf(
                        Color.argb(alpha, 245, 248, 255),
                        Color.argb((alpha * 0.7f).toInt(), 225, 235, 250),
                        Color.argb(0, 200, 210, 225)
                    ),
                    floatArrayOf(0f, 0.60f, 1f),
                    Shader.TileMode.CLAMP
                )

                trailPaint.shader = shader
                trailPaint.strokeWidth = strokeW
                canvas.drawLine(px, py, px, 0f, trailPaint)
            }
        }

        private fun drawPerspectiveClockFace(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            rx: Float,
            ry: Float,
            tiltFactor: Float,
            dp: Float
        ) {
            val hourTickLen = 14.0f * dp
            val minTickLen = 7.0f * dp

            // 60 Ticks arranged in 3D perspective ellipse
            for (i in 0 until 60) {
                val angleDeg = i * 6.0
                val rad = Math.toRadians(angleDeg - 90)
                val cosVal = cos(rad).toFloat()
                val sinVal = sin(rad).toFloat()

                val isHourTick = i % 5 == 0
                val tickLen = if (isHourTick) hourTickLen else minTickLen

                val outerX = cx + rx * cosVal
                val outerY = cy + ry * sinVal

                val innerX = cx + (rx - tickLen) * cosVal
                val innerY = cy + (ry - tickLen * tiltFactor) * sinVal

                if (isHourTick) {
                    hourTickPaint.strokeWidth = 2.2f * dp
                    canvas.drawLine(innerX, innerY, outerX, outerY, hourTickPaint)
                } else {
                    minuteTickPaint.strokeWidth = 1.2f * dp
                    canvas.drawLine(innerX, innerY, outerX, outerY, minuteTickPaint)
                }
            }
        }

        private fun drawPerspectiveClockHands(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            tiltFactor: Float,
            hourAngle: Float,
            hourLen: Float,
            minAngle: Float,
            minLen: Float,
            secAngle: Float,
            secLen: Float,
            dp: Float
        ) {
            fun drawHand(angleDeg: Float, length: Float, strokeW: Float, paint: Paint) {
                val rad = Math.toRadians((angleDeg - 90).toDouble())
                val cosVal = cos(rad).toFloat()
                val sinVal = sin(rad).toFloat()

                val stopX = cx + length * cosVal
                val stopY = cy + length * sinVal * tiltFactor

                val tailLen = length * 0.14f
                val startX = cx - tailLen * cosVal
                val startY = cy - tailLen * sinVal * tiltFactor

                paint.strokeWidth = strokeW
                canvas.drawLine(startX, startY, stopX, stopY, paint)
            }

            // Draw Hour Hand
            drawHand(hourAngle, hourLen, 3.8f * dp, hourHandPaint)

            // Draw Minute Hand
            drawHand(minAngle, minLen, 2.6f * dp, minuteHandPaint)

            // Draw Second Hand
            drawHand(secAngle, secLen, 1.4f * dp, secondHandPaint)

            // Center Hub & Core Dot
            canvas.drawCircle(cx, cy, 4.8f * dp, centerHubPaint)
            canvas.drawCircle(cx, cy, 1.6f * dp, centerInnerDotPaint)
        }
    }
}
