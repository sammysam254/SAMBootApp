package com.samson.bootscreen

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class WallpaperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val handler = Handler(Looper.getMainLooper())
    private var frame = 0f
    private var w = 0f; private var h = 0f

    private val bgPaint = Paint().apply {
        shader = null
    }

    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9A84C")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15C9A84C")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#08C9A84C")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val drawRunnable = object : Runnable {
        override fun run() {
            frame += 0.5f
            invalidate()
            handler.postDelayed(this, 50) // 20fps — smooth but battery friendly
        }
    }

    override fun onSizeChanged(newW: Int, newH: Int, oldW: Int, oldH: Int) {
        w = newW.toFloat(); h = newH.toFloat()
        // Background gradient
        bgPaint.shader = RadialGradient(
            w * 0.5f, h * 0.4f, maxOf(w, h) * 0.8f,
            intArrayOf(
                Color.parseColor("#0D0D1E"),
                Color.parseColor("#07070F"),
                Color.parseColor("#000000")
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        textPaint.textSize = w * 0.45f
        subtextPaint.textSize = w * 0.04f
    }

    override fun onDraw(canvas: Canvas) {
        if (w == 0f) return
        val t = frame * 0.02f
        val cx = w / 2; val cy = h * 0.42f

        // Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Subtle grid
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06C9A84C")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        var gx = 0f; while (gx <= w) { canvas.drawLine(gx, 0f, gx, h, gridPaint); gx += 60f }
        var gy = 0f; while (gy <= h) { canvas.drawLine(0f, gy, w, gy, gridPaint); gy += 60f }

        // Breathing rings
        for (i in 0..4) {
            val phase = (t + i * 0.4f) % (2 * PI.toFloat())
            val alpha = ((sin(phase) + 1f) / 2f * 0.12f + 0.02f)
            val radius = w * (0.15f + i * 0.13f) + sin(t * 0.3f + i) * w * 0.015f
            goldPaint.alpha = (alpha * 255).toInt()
            goldPaint.strokeWidth = if (i == 2) 1.5f else 0.7f
            canvas.drawCircle(cx, cy, radius, goldPaint)
        }

        // Corner accents
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25C9A84C")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        val m = 28f; val cl = 45f
        canvas.drawLine(m, m, m+cl, m, accentPaint); canvas.drawLine(m, m, m, m+cl, accentPaint)
        canvas.drawLine(w-m, m, w-m-cl, m, accentPaint); canvas.drawLine(w-m, m, w-m, m+cl, accentPaint)
        canvas.drawLine(m, h-m, m+cl, h-m, accentPaint); canvas.drawLine(m, h-m, m, h-m-cl, accentPaint)
        canvas.drawLine(w-m, h-m, w-m-cl, h-m, accentPaint); canvas.drawLine(w-m, h-m, w-m, h-m-cl, accentPaint)

        // SAM watermark
        val pulse = (sin(t * 0.8f) + 1f) / 2f
        textPaint.alpha = (pulse * 18 + 8).toInt()
        canvas.drawText("SAM", cx, cy + textPaint.textSize * 0.35f, textPaint)

        // Gold divider
        val divW = w * 0.55f; val divY = cy + textPaint.textSize * 0.6f + 18f
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx-divW/2, divY, cx+divW/2, divY,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#30C9A84C"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            strokeWidth = 1f; style = Paint.Style.STROKE
        }
        canvas.drawLine(cx-divW/2, divY, cx+divW/2, divY, divPaint)

        // Subtext
        subtextPaint.alpha = (pulse * 12 + 5).toInt()
        canvas.drawText("SAMSON MATATA MWINZI", cx, divY + 30f, subtextPaint)
        canvas.drawText("DEVELOPER · ENTREPRENEUR · FOUNDER", cx, divY + 52f, subtextPaint)

        // Vignette
        val vigPaint = Paint().apply {
            shader = RadialGradient(cx, h/2, maxOf(w,h)*0.7f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#AA000000")),
                floatArrayOf(0.4f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, vigPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(drawRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(drawRunnable)
    }
}
