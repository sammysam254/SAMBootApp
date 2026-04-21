package com.samson.bootscreen

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.*

class SamWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SamWallpaperEngine()

    inner class SamWallpaperEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var frame = 0L
        private var w = 0f; private var h = 0f

        // ── Paints ─────────────────────────────────────────────────
        private val bgPaint = Paint().apply { color = Color.BLACK }

        private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C9A84C")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

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
            textSize = 0f // set after surface size known
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        private val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A6020")
            textSize = 0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        private val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A4510")
            textSize = 0f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        private val vignettePaint = Paint()

        // Particles
        data class Particle(
            var x: Float, var y: Float,
            var vx: Float, var vy: Float,
            var life: Float, var maxLife: Float,
            var size: Float
        )
        private val particles = mutableListOf<Particle>()
        private val random = java.util.Random()

        // ── Runnable loop ──────────────────────────────────────────
        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 33) // ~30fps
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            w = width.toFloat(); h = height.toFloat()
            textPaint.textSize    = w * 0.22f
            subtextPaint.textSize = w * 0.032f
            taglinePaint.textSize = w * 0.022f

            // Vignette radial gradient
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
            if (vis) handler.post(drawRunnable) else handler.removeCallbacks(drawRunnable)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun spawnParticles() {
            if (random.nextFloat() > 0.3f) return
            val cx = w / 2; val cy = h * 0.42f
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 1.5f + 0.3f
            particles.add(Particle(
                cx + cos(angle) * (w * 0.12f),
                cy + sin(angle) * (w * 0.12f),
                cos(angle) * speed, sin(angle) * speed,
                1f, 1f,
                random.nextFloat() * 2f + 0.5f
            ))
            if (particles.size > 60) particles.removeAt(0)
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas() ?: return
            frame++

            try {
                val cx = w / 2; val cy = h * 0.42f
                val t = frame / 60f // time in seconds

                // ── Background ─────────────────────────────────────
                canvas.drawRect(0f, 0f, w, h, bgPaint)

                // ── Subtle noise grid ──────────────────────────────
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#08C9A84C")
                    strokeWidth = 0.5f
                    style = Paint.Style.STROKE
                }
                val gridSize = 60f
                var gx = 0f
                while (gx <= w) {
                    canvas.drawLine(gx, 0f, gx, h, gridPaint); gx += gridSize
                }
                var gy = 0f
                while (gy <= h) {
                    canvas.drawLine(0f, gy, w, gy, gridPaint); gy += gridSize
                }

                // ── Breathing rings ────────────────────────────────
                val numRings = 4
                for (i in 0 until numRings) {
                    val phase = (t * 0.7f + i * 0.5f) % (2 * PI.toFloat())
                    val alpha = ((sin(phase) + 1f) / 2f * 0.25f + 0.05f)
                    val radius = w * (0.18f + i * 0.12f) + sin(t * 0.5f + i) * w * 0.02f
                    ringPaint.alpha = (alpha * 255).toInt()
                    canvas.drawCircle(cx, cy, radius, ringPaint)
                }

                // ── Corner accent lines ────────────────────────────
                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#30C9A84C")
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                val cLen = 40f; val margin = 24f
                // Top-left
                canvas.drawLine(margin, margin, margin + cLen, margin, accentPaint)
                canvas.drawLine(margin, margin, margin, margin + cLen, accentPaint)
                // Top-right
                canvas.drawLine(w - margin, margin, w - margin - cLen, margin, accentPaint)
                canvas.drawLine(w - margin, margin, w - margin, margin + cLen, accentPaint)
                // Bottom-left
                canvas.drawLine(margin, h - margin, margin + cLen, h - margin, accentPaint)
                canvas.drawLine(margin, h - margin, margin, h - margin - cLen, accentPaint)
                // Bottom-right
                canvas.drawLine(w - margin, h - margin, w - margin - cLen, h - margin, accentPaint)
                canvas.drawLine(w - margin, h - margin, w - margin, h - margin - cLen, accentPaint)

                // ── Particles ──────────────────────────────────────
                spawnParticles()
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.x += p.vx; p.y += p.vy
                    p.vy -= 0.02f // slight upward drift
                    p.life -= 0.012f
                    if (p.life <= 0f) { iter.remove(); continue }
                    particlePaint.alpha = (p.life * 180).toInt()
                    canvas.drawCircle(p.x, p.y, p.size * p.life, particlePaint)
                }

                // ── SAM text (pulsing glow) ─────────────────────────
                val glowScale = 1f + sin(t * 1.2f) * 0.015f
                val glowAlpha = (180 + sin(t * 1.5f) * 50).toInt().coerceIn(100, 255)

                // Glow layer (larger, blurred-ish via multiple draws)
                val glowPaint = Paint(textPaint).apply {
                    alpha = (glowAlpha * 0.3f).toInt()
                    color = Color.parseColor("#C9A84C")
                    textSize = textPaint.textSize * glowScale * 1.02f
                    maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawText("SAM", cx, cy + textPaint.textSize * 0.35f, glowPaint)

                // Main SAM text
                textPaint.alpha = glowAlpha
                textPaint.textSize = textPaint.textSize // already set
                canvas.drawText("SAM", cx, cy + textPaint.textSize * 0.35f, textPaint)

                // ── Gold divider ───────────────────────────────────
                val dividerW = w * 0.5f
                val dividerY = cy + textPaint.textSize * 0.6f + 20f
                val divGrad = LinearGradient(
                    cx - dividerW / 2, dividerY, cx + dividerW / 2, dividerY,
                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#C9A84C"), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = divGrad; strokeWidth = 1.2f; style = Paint.Style.STROKE
                }
                canvas.drawLine(cx - dividerW / 2, dividerY, cx + dividerW / 2, dividerY, divPaint)

                // ── Sub text ───────────────────────────────────────
                subtextPaint.alpha = (120 + sin(t * 0.8f) * 30).toInt()
                canvas.drawText(
                    "SAMSON MATATA MWINZI",
                    cx, dividerY + 36f, subtextPaint
                )
                canvas.drawText(
                    "DEVELOPER  ·  ENTREPRENEUR  ·  FOUNDER",
                    cx, dividerY + 62f, subtextPaint
                )

                // ── Tagline ────────────────────────────────────────
                taglinePaint.alpha = (80 + sin(t * 0.6f) * 20).toInt()
                canvas.drawText(
                    "VERTEXT DIGITAL  ·  RUIRU, KENYA",
                    cx, h - 80f, taglinePaint
                )
                canvas.drawText(
                    "Building software that solves real problems",
                    cx, h - 56f, taglinePaint
                )

                // ── Vignette ───────────────────────────────────────
                canvas.drawRect(0f, 0f, w, h, vignettePaint)

                // ── Scanlines ──────────────────────────────────────
                val slPaint = Paint().apply { color = Color.parseColor("#0AFFFFFF") }
                var sy = 0f
                while (sy < h) { canvas.drawLine(0f, sy, w, sy, slPaint); sy += 4f }

            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
