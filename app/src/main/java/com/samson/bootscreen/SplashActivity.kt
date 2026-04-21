package com.samson.bootscreen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Catch ALL crashes and show them on screen
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                val tv = TextView(this).apply {
                    text = "CRASH:\n${throwable.message}\n\n${throwable.stackTraceToString().take(500)}"
                    textSize = 10f
                    setTextColor(0xFFFF4444.toInt())
                    setPadding(20, 20, 20, 20)
                }
                val ll = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundColor(0xFF000000.toInt())
                    addView(tv)
                }
                setContentView(ll)
            }
        }

        try {
            setContentView(R.layout.activity_splash)
            Log.d("SAM", "Splash layout OK")
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("SAM", "Launching LauncherActivity")
                    startActivity(Intent(this, LauncherActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Log.e("SAM", "Launch error: ${e.message}", e)
                }
            }, 3000)
        } catch (e: Exception) {
            Log.e("SAM", "Splash error: ${e.message}", e)
        }
    }
}
