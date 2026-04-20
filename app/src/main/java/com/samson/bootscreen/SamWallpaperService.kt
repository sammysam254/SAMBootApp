package com.samson.bootscreen

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.*

data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float,
    var size: Float
)

class SamWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SamWallpaperEngine()

    inner class SamWallpaperEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var frame = 0L
        private var w = 0f
        private var h = 0f
        private val random = java.util.Random()
        private val particles = mutableListOf<Particle>()

        private val bgPaint = Paint().apply { color = Color.BLACK }

        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C9A84C")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C9A84C")
            style = Paint.Style.FILL
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        private val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A6020")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        private val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A4510")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        private val vignettePaint = Paint()

        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 33)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            w = width.toFloat()
            h = height.toFloat()
            textPaint.textSize    = w * 0.22f
            subtextPaint.textSize = w * 0.032f
            taglinePaint.textSize = w * 0.022f
            vignettePaint.shader = RadialGradient(
                w / 2, h / 2,
                maxOf(w, h) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#CC000000")),
                floatArrayOf(0.3f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        override fun onVisibilityChanged(vis: Boolean) {
            visible = vis
            if (vis) handler.post(drawRunnable)
            else handler.removeCallbacks(drawRunnable)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun spawnParticles() {
            if (random.nextFloat() > 0.3f) return
            val cx = w / 2
            val cy = h * 0.42f
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 1.5f + 0.3f
            particles.add(
                Particle(
                    cx + cos(angle) * (w * 0.12f),
                    cy + sin(angle) * (w * 0.12f),
                    cos(angle) * speed,
                    sin(angle) * speed,
                    1f,
                    random.nextFloat() * 2f + 0.5f
                )
            )
            if (particles.size > 60) particles.removeAt(0)
        }

        private fun draw() {
            val canvas = surfaceHolder.lockCanvas() ?: return
            frame++
            try {
                val cx = w / 2
                val cy = h * 0.42f
                val t = frame / 60f

                canvas.drawRect(0f, 0f, w, h, bgPaint)

                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#08C9A84C")
                    strokeWidth = 0.5f
                    style = Paint.Style.STROKE
                }
                var gx = 0f
                while (gx <= w) { canvas.drawLine(gx, 0f, gx, h, gridPaint); gx += 60f }
                var gy = 0f
                while (gy <= h) { canvas.drawLine(0f, gy, w, gy, gridPaint); gy += 60f }

                for (i in 0 until 4) {
                    val phase = (t * 0.7f + i * 0.5f) % (2 * PI.toFloat())
                    val alpha = ((sin(phase) + 1f) / 2f * 0.25f + 0.05f)
                    val radius = w * (0.18f + i * 0.12f) + sin(t * 0.5f + i) * w * 0.02f
                    ringPaint.alpha = (alpha * 255).toInt()
                    canvas.drawCircle(cx, cy, radius, ringPaint)
                }

                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#30C9A84C")
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                val m = 24f; val cL = 40f
                canvas.drawLine(m, m, m+cL, m, accentPaint)
                canvas.drawLine(m, m, m, m+cL, accentPaint)
                canvas.drawLine(w-m, m, w-m-cL, m, accentPaint)
                canvas.drawLine(w-m, m, w-m, m+cL, accentPaint)
                canvas.drawLine(m, h-m, m+cL, h-m, accentPaint)
                canvas.drawLine(m, h-m, m, h-m-cL, accentPaint)
                canvas.drawLine(w-m, h-m, w-m-cL, h-m, accentPaint)
                canvas.drawLine(w-m, h-m, w-m, h-m-cL, accentPaint)

                spawnParticles()
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.x += p.vx; p.y += p.vy; p.vy -= 0.02f; p.life -= 0.012f
                    if (p.life <= 0f) { iter.remove(); continue }
                    particlePaint.alpha = (p.life * 180).toInt()
                    canvas.drawCircle(p.x, p.y, p.size * p.life, particlePaint)
                }

                val glowAlpha = (180 + sin(t * 1.5f) * 50).toInt().coerceIn(100, 255)
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#C9A84C")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    textSize = textPaint.textSize * 1.02f
                    alpha = (glowAlpha * 0.3f).toInt()
                    maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawText("SAM", cx, cy + textPaint.textSize * 0.35f, glowPaint)
                textPaint.alpha = glowAlpha
                canvas.drawText("SAM", cx, cy + textPaint.textSize * 0.35f, textPaint)

                val divW = w * 0.5f
                val divY = cy + textPaint.textSize * 0.6f + 20f
                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        cx - divW/2, divY, cx + divW/2, divY,
                        intArrayOf(Color.TRANSPARENT, Color.parseColor("#C9A84C"), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP
                    )
                    strokeWidth = 1.2f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(cx - divW/2, divY, cx + divW/2, divY, divPaint)

                subtextPaint.alpha = (120 + sin(t * 0.8f) * 30).toInt()
                canvas.drawText("SAMSON MATATA MWINZI", cx, divY + 36f, subtextPaint)
                canvas.drawText("DEVELOPER  ·  ENTREPRENEUR  ·  FOUNDER", cx, divY + 62f, subtextPaint)

                taglinePaint.alpha = (80 + sin(t * 0.6f) * 20).toInt()
                canvas.drawText("VERTEXT DIGITAL  ·  RUIRU, KENYA", cx, h - 80f, taglinePaint)
                canvas.drawText("Building software that solves real problems", cx, h - 56f, taglinePaint)

                canvas.drawRect(0f, 0f, w, h, vignettePaint)

                val slPaint = Paint().apply { color = Color.parseColor("#0AFFFFFF") }
                var sy = 0f
                while (sy < h) { canvas.drawLine(0f, sy, w, sy, slPaint); sy += 4f }

            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
