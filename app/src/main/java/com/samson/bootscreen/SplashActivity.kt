package com.samson.bootscreen

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.SoundPool
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private var soundPool: SoundPool? = null
    private var soundS = 0
    private var soundA = 0
    private var soundM = 0
    private var soundChime = 0
    private var soundBoot = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContentView(R.layout.activity_splash)
        setupSoundPool()
        startBootSequence()
    }

    private fun setupSoundPool() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attrs).build()
            soundS     = soundPool?.load(this, R.raw.tone_s,     1) ?: 0
            soundA     = soundPool?.load(this, R.raw.tone_a,     1) ?: 0
            soundM     = soundPool?.load(this, R.raw.tone_m,     1) ?: 0
            soundChime = soundPool?.load(this, R.raw.tone_chime, 1) ?: 0
            soundBoot  = soundPool?.load(this, R.raw.tone_boot,  1) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSound(id: Int) {
        try { if (id != 0) soundPool?.play(id, 1f, 1f, 1, 0, 1f) } catch (e: Exception) {}
    }

    private fun startBootSequence() {
        try {
            val letterS     = findViewById<TextView>(R.id.letterS)     ?: return
            val letterA     = findViewById<TextView>(R.id.letterA)     ?: return
            val letterM     = findViewById<TextView>(R.id.letterM)     ?: return
            val tagline     = findViewById<TextView>(R.id.tagline)     ?: return
            val divider     = findViewById<View>(R.id.divider)         ?: return
            val introBlock  = findViewById<View>(R.id.introBlock)      ?: return
            val progressBar = findViewById<ProgressBar>(R.id.progressBar) ?: return
            val progressPct = findViewById<TextView>(R.id.progressPct) ?: return
            val ring1       = findViewById<View>(R.id.ring1)           ?: return
            val ring2       = findViewById<View>(R.id.ring2)           ?: return
            val ring3       = findViewById<View>(R.id.ring3)           ?: return

            handler.postDelayed({ playSound(soundBoot) }, 200)
            handler.postDelayed({ startRingPulse(ring1, 0L) }, 300)
            handler.postDelayed({ startRingPulse(ring2, 500L) }, 300)
            handler.postDelayed({ startRingPulse(ring3, 1000L) }, 300)

            handler.postDelayed({ animateLetter(letterS); playSound(soundS) }, 700)
            handler.postDelayed({ animateLetter(letterA); playSound(soundA) }, 1150)
            handler.postDelayed({ animateLetter(letterM); playSound(soundM) }, 1600)

            handler.postDelayed({
                tagline.visibility = View.VISIBLE
                tagline.animate().alpha(1f).translationY(0f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).start()
            }, 2300)

            handler.postDelayed({ animateDivider(divider) }, 2700)

            handler.postDelayed({
                introBlock.visibility = View.VISIBLE
                introBlock.animate().alpha(1f).translationY(0f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).start()
            }, 3200)

            handler.postDelayed({
                progressBar.visibility = View.VISIBLE
                progressPct.visibility = View.VISIBLE
                animateProgress(progressBar, progressPct)
            }, 3700)

            handler.postDelayed({ playSound(soundChime) }, 6800)
            handler.postDelayed({ fadeOutAndLaunch() }, 7500)

        } catch (e: Exception) {
            e.printStackTrace()
            handler.postDelayed({ fadeOutAndLaunch() }, 2000)
        }
    }

    private fun animateLetter(view: TextView) {
        view.visibility = View.VISIBLE
        val set = AnimatorSet()
        set.playTogether(
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).setDuration(400),
            ObjectAnimator.ofFloat(view, "translationY", 60f, 0f).setDuration(450),
            ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f).setDuration(450),
            ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f).setDuration(450)
        )
        set.interpolator = OvershootInterpolator(1.8f)
        set.start()
    }

    private fun animateDivider(divider: View) {
        divider.visibility = View.VISIBLE
        val anim = ValueAnimator.ofInt(0, 600)
        anim.duration = 900
        anim.addUpdateListener {
            val p = divider.layoutParams
            p.width = it.animatedValue as Int
            divider.layoutParams = p
        }
        anim.start()
    }

    private fun startRingPulse(ring: View, initialDelay: Long) {
        fun pulse() {
            ring.scaleX = 0.5f; ring.scaleY = 0.5f; ring.alpha = 0f
            ring.animate().scaleX(1.8f).scaleY(1.8f).alpha(0.6f)
                .setDuration(700).setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    ring.animate().alpha(0f).scaleX(2.4f).scaleY(2.4f)
                        .setDuration(700)
                        .withEndAction { handler.postDelayed({ pulse() }, 800) }
                        .start()
                }.start()
        }
        handler.postDelayed({ pulse() }, initialDelay)
    }

    private fun animateProgress(bar: ProgressBar, pct: TextView) {
        val anim = ValueAnimator.ofInt(0, 100)
        anim.duration = 3000
        anim.addUpdateListener {
            val v = it.animatedValue as Int
            bar.progress = v
            pct.text = "$v%"
        }
        anim.start()
    }

    private fun fadeOutAndLaunch() {
        try {
            val root = findViewById<View>(R.id.splashRoot)
            root?.animate()?.alpha(0f)?.setDuration(600)?.withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }?.start() ?: run {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
