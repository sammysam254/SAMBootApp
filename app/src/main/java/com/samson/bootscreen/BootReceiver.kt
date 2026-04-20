package com.samson.bootscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SAMBoot", "Received: ${intent.action}")
        val bootActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.USER_PRESENT"
        )
        if (intent.action in bootActions) {
            try {
                val splash = Intent(context, SplashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(splash)
                Log.d("SAMBoot", "SAM launched!")
            } catch (e: Exception) {
                Log.e("SAMBoot", "Error: ${e.message}")
            }
        }
    }
}
