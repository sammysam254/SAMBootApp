package com.samson.samlauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Global crash catcher - shows error on screen instead of closing
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                val msg = "CRASH DETAILS:\n\n" +
                    "Error: ${throwable.message}\n\n" +
                    "Cause: ${throwable.cause?.message}\n\n" +
                    throwable.stackTraceToString().take(800)

                val tv = TextView(this).apply {
                    text = msg
                    textSize = 9f
                    setTextColor(Color.parseColor("#FF6B6B"))
                    setPadding(24, 24, 24, 24)
                    setBackgroundColor(Color.BLACK)
                }
                val sv = ScrollView(this).apply { addView(tv) }
                val ll = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.BLACK)
                    gravity = Gravity.TOP
                    addView(TextView(context).apply {
                        text = "⚠ SAM Launcher Crash Report"
                        textSize = 14f
                        setTextColor(Color.parseColor("#C9A84C"))
                        setPadding(24, 40, 24, 16)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    })
                    addView(sv)
                }
                setContentView(ll)
            }
        }

        try {
            setContentView(R.layout.activity_splash)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    startActivity(Intent(this, LauncherActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    throw e
                }
            }, 3000)
        } catch (e: Exception) {
            throw e
        }
    }
}
