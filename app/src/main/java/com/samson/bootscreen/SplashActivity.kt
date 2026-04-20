package com.samson.bootscreen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "SAMSplash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            setContentView(R.layout.activity_splash)
            Log.d(TAG, "layout set OK")
        } catch (e: Exception) {
            Log.e(TAG, "Layout error: ${e.message}")
            launchMain()
            return
        }

        // Simple delayed launch — no animations yet
        handler.postDelayed({
            Log.d(TAG, "launching main")
            launchMain()
        }, 5000)
    }

    private fun launchMain() {
        try {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Launch error: ${e.message}")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
