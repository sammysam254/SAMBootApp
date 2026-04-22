package com.samson.bootscreen

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private var soundS = 0
    private var soundA = 0
    private var soundM = 0
    private var soundChime = 0
    private var soundBoot = 0

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full immersive — hide status bar & nav bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        setContentView(R.layout.activity_splash)

        setupSoundPool()
        startBootSequence()
    }

    private fun setupSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build()

        // Load sounds — we generate tones programmatically via ToneGenerator fallback
        // Raw resources loaded if present
        try {
            soundS     = soundPool.load(this, R.raw.tone_s,     1)
            soundA     = soundPool.load(this, R.raw.tone_a,     1)
            soundM     = soundPool.load(this, R.raw.tone_m,     1)
            soundChime = soundPool.load(this, R.raw.tone_chime, 1)
            soundBoot  = soundPool.load(this, R.raw.tone_boot,  1)
        } catch (e: Exception) {
            // Sounds optional — animation still runs
        }
    }

    private fun playSound(id: Int) {
        if (id != 0) soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    private fun startBootSequence() {
        val letterS      = findViewById<TextView>(R.id.letterS)
        val letterA      = findViewById<TextView>(R.id.letterA)
        val letterM      = findViewById<TextView>(R.id.letterM)
        val tagline      = findViewById<TextView>(R.id.tagline)
        val divider      = findViewById<View>(R.id.divider)
        val introBlock   = findViewById<View>(R.id.introBlock)
        val progressBar  = findViewById<ProgressBar>(R.id.progressBar)
        val progressPct  = findViewById<TextView>(R.id.progressPct)
        val scanlines    = findViewById<View>(R.id.scanlines)
        val ring1        = findViewById<View>(R.id.ring1)
        val ring2        = findViewById<View>(R.id.ring2)
        val ring3        = findViewById<View>(R.id.ring3)

        // Boot thud at start
        handler.postDelayed({ playSound(soundBoot) }, 200)

        // Start ring pulse animations
        handler.postDelayed({
            startRingPulse(ring1, 0L)
            startRingPulse(ring2, 600L)
            startRingPulse(ring3, 1200L)
        }, 300)

        // Scanlines fade in
        handler.postDelayed({
            scanlines.animate().alpha(1f).setDuration(400).start()
        }, 300)

        // ── Letter S ─────────────────────────
        handler.postDelayed({
            animateLetter(letterS)
            playSound(soundS)
        }, 700)

        // ── Letter A ─────────────────────────
        handler.postDelayed({
            animateLetter(letterA)
            playSound(soundA)
        }, 1150)

        // ── Letter M ─────────────────────────
        handler.postDelayed({
            animateLetter(letterM)
            playSound(soundM)
        }, 1600)

        // ── Tagline ───────────────────────────
        handler.postDelayed({
            tagline.visibility = View.VISIBLE
            tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 2300)

        // ── Divider expand ────────────────────
        handler.postDelayed({
            animateDivider(divider)
        }, 2700)

        // ── Intro block ───────────────────────
        handler.postDelayed({
            introBlock.visibility = View.VISIBLE
            introBlock.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 3200)

        // ── Progress bar ──────────────────────
        handler.postDelayed({
            progressBar.visibility = View.VISIBLE
            progressPct.visibility = View.VISIBLE
            animateProgress(progressBar, progressPct)
        }, 3700)

        // ── Final chime + launch home ─────────
        handler.postDelayed({
            playSound(soundChime)
        }, 6800)

        handler.postDelayed({
            fadeOutAndLaunch()
        }, 7500)
    }

    private fun animateLetter(view: TextView) {
        view.visibility = View.VISIBLE
        val fadeIn    = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).setDuration(400)
        val slideUp   = ObjectAnimator.ofFloat(view, "translationY", 60f, 0f).setDuration(450)
        val scaleX    = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f).setDuration(450)
        val scaleY    = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f).setDuration(450)

        val set = AnimatorSet()
        set.playTogether(fadeIn, slideUp, scaleX, scaleY)
        set.interpolator = OvershootInterpolator(1.8f)
        set.start()
    }

    private fun animateDivider(divider: View) {
        divider.visibility = View.VISIBLE
        val anim = ValueAnimator.ofInt(0, 600)
        anim.duration = 900
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.addUpdateListener { animator ->
            val params = divider.layoutParams
            params.width = animator.animatedValue as Int
            divider.layoutParams = params
        }
        anim.start()
    }

    private fun startRingPulse(ring: View, initialDelay: Long) {
        fun pulse() {
            ring.scaleX = 0.5f; ring.scaleY = 0.5f; ring.alpha = 0f
            ring.animate()
                .scaleX(1.8f).scaleY(1.8f).alpha(0.6f)
                .setDuration(700)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    ring.animate()
                        .alpha(0f).scaleX(2.4f).scaleY(2.4f)
                        .setDuration(700)
                        .withEndAction { handler.postDelayed({ pulse() }, 800) }
                        .start()
                }
                .start()
        }
        handler.postDelayed({ pulse() }, initialDelay)
    }

    private fun animateProgress(progressBar: ProgressBar, pctText: TextView) {
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 3000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Int
            progressBar.progress = value
            pctText.text = "$value%"
        }
        animator.start()
    }

    private fun fadeOutAndLaunch() {
        val root = findViewById<View>(R.id.splashRoot)
        root.animate()
            .alpha(0f)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            })
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        handler.removeCallbacksAndMessages(null)
    }
}
