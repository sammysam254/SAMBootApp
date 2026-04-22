package com.samson.samlauncher

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.*

data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float, var size: Float
)

class SamWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SamEngine()

    inner class SamEngine : Engine() {
        private val h = Handler(Looper.getMainLooper())
        private var on = false
        private var f = 0L
        private var w = 0f
        private var sz = 0f
        private val rnd = java.util.Random()
        private val pts = mutableListOf<Particle>()
        private val bg = Paint().apply { color = Color.BLACK }
        private val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C9A84C")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        private val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C9A84C")
            style = Paint.Style.FILL
        }
        private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A6020")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        private val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A4510")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        private val vp = Paint()
        private val dr = object : Runnable {
            override fun run() { draw(); if (on) h.postDelayed(this, 33) }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, fmt: Int, ww: Int, hh: Int) {
            w = ww.toFloat(); sz = hh.toFloat()
            tp.textSize = w * 0.22f
            sp.textSize = w * 0.032f
            lp.textSize = w * 0.022f
            vp.shader = RadialGradient(w/2, sz/2, maxOf(w, sz)*0.75f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#CC000000")),
                floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
        }

        override fun onVisibilityChanged(v: Boolean) {
            on = v; if (v) h.post(dr) else h.removeCallbacks(dr)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            on = false; h.removeCallbacks(dr)
        }

        private fun spawn() {
            if (rnd.nextFloat() > 0.3f) return
            val cx = w/2; val cy = sz*0.42f
            val a = rnd.nextFloat() * 2 * PI.toFloat()
            val spd = rnd.nextFloat() * 1.5f + 0.3f
            pts.add(Particle(cx+cos(a)*(w*0.12f), cy+sin(a)*(w*0.12f),
                cos(a)*spd, sin(a)*spd, 1f, rnd.nextFloat()*2f+0.5f))
            if (pts.size > 60) pts.removeAt(0)
        }

        private fun draw() {
            val c = surfaceHolder.lockCanvas() ?: return; f++
            try {
                val cx = w/2; val cy = sz*0.42f; val t = f/60f
                c.drawRect(0f, 0f, w, sz, bg)
                val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#08C9A84C")
                    strokeWidth = 0.5f; style = Paint.Style.STROKE
                }
                var x = 0f; while (x <= w) { c.drawLine(x, 0f, x, sz, gp); x += 60f }
                var y = 0f; while (y <= sz) { c.drawLine(0f, y, w, y, gp); y += 60f }
                for (i in 0..3) {
                    val ph = (t*0.7f+i*0.5f) % (2*PI.toFloat())
                    rp.alpha = (((sin(ph)+1f)/2f*0.25f+0.05f)*255).toInt()
                    c.drawCircle(cx, cy, w*(0.18f+i*0.12f)+sin(t*0.5f+i)*w*0.02f, rp)
                }
                val ap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#30C9A84C"); strokeWidth = 1f; style = Paint.Style.STROKE
                }
                val m = 24f; val cl = 40f
                c.drawLine(m,m,m+cl,m,ap); c.drawLine(m,m,m,m+cl,ap)
                c.drawLine(w-m,m,w-m-cl,m,ap); c.drawLine(w-m,m,w-m,m+cl,ap)
                c.drawLine(m,sz-m,m+cl,sz-m,ap); c.drawLine(m,sz-m,m,sz-m-cl,ap)
                c.drawLine(w-m,sz-m,w-m-cl,sz-m,ap); c.drawLine(w-m,sz-m,w-m,sz-m-cl,ap)
                spawn()
                val it = pts.iterator()
                while (it.hasNext()) {
                    val p = it.next(); p.x+=p.vx; p.y+=p.vy; p.vy-=0.02f; p.life-=0.012f
                    if (p.life <= 0f) { it.remove(); continue }
                    pp.alpha = (p.life*180).toInt()
                    c.drawCircle(p.x, p.y, p.size*p.life, pp)
                }
                val ga = (180+sin(t*1.5f)*50).toInt().coerceIn(100, 255)
                val gw = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#C9A84C")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    textSize = tp.textSize * 1.02f
                    alpha = (ga*0.3f).toInt()
                    maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
                }
                c.drawText("SAM", cx, cy+tp.textSize*0.35f, gw)
                tp.alpha = ga; c.drawText("SAM", cx, cy+tp.textSize*0.35f, tp)
                val dw = w*0.5f; val dy = cy+tp.textSize*0.6f+20f
                val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(cx-dw/2, dy, cx+dw/2, dy,
                        intArrayOf(Color.TRANSPARENT, Color.parseColor("#C9A84C"), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP)
                    strokeWidth = 1.2f; style = Paint.Style.STROKE
                }
                c.drawLine(cx-dw/2, dy, cx+dw/2, dy, dp)
                sp.alpha = (120+sin(t*0.8f)*30).toInt()
                c.drawText("SAMSON MATATA MWINZI", cx, dy+36f, sp)
                c.drawText("DEVELOPER · ENTREPRENEUR · FOUNDER", cx, dy+62f, sp)
                lp.alpha = (80+sin(t*0.6f)*20).toInt()
                c.drawText("VERTEXT DIGITAL · RUIRU, KENYA", cx, sz-80f, lp)
                c.drawText("Building software that solves real problems", cx, sz-56f, lp)
                c.drawRect(0f, 0f, w, sz, vp)
                val sl = Paint().apply { color = Color.parseColor("#0AFFFFFF") }
                var sy = 0f; while (sy < sz) { c.drawLine(0f, sy, w, sy, sl); sy += 4f }
            } finally { surfaceHolder.unlockCanvasAndPost(c) }
        }
    }
}
