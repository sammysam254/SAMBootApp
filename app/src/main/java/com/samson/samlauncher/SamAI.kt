package com.samson.samlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

class SamAI(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onListening: (Boolean) -> Unit,
    private val onSpeak: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f)
            }
        }
    }

    fun speak(text: String) {
        onSpeak(text)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SAM_${System.currentTimeMillis()}")
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speak("Speech recognition not available.")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onResult("NEED_PERMISSION")
            return
        }
        onListening(true)
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                onListening(false)
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (text.isNotEmpty()) processCommand(text.lowercase(Locale.getDefault()))
            }
            override fun onError(error: Int) {
                onListening(false)
                speak("I didn't catch that. Please try again.")
            }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        onListening(false)
    }

    private fun processCommand(cmd: String) {
        Log.d("SamAI", "Command: $cmd")
        when {
            cmd.contains("call") -> handleCall(cmd)
            cmd.contains("google") || cmd.contains("search") -> handleSearch(cmd)
            cmd.contains("play") || cmd.contains("music") || cmd.contains("song") -> handleMusic(cmd)
            cmd.contains("open") || cmd.contains("launch") -> handleOpenApp(cmd)
            cmd.contains("whatsapp") || cmd.contains("message") -> handleWhatsApp()
            cmd.contains("settings") -> openSettings("settings")
            cmd.contains("wifi") -> openSettings("wifi")
            cmd.contains("bluetooth") -> openSettings("bluetooth")
            cmd.contains("time") || cmd.contains("date") -> handleTimeDate()
            cmd.contains("alarm") -> handleAlarm()
            cmd.contains("weather") -> handleWeather()
            cmd.contains("hello") || cmd.contains("hi sam") || cmd.contains("hey sam") -> {
                speak("Hello Samson! How can I help you today?")
                onResult("SAM: Hello Samson! How can I help?")
            }
            cmd.contains("who are you") -> {
                speak("I am SAM, your personal AI assistant. I can call, search, play music, open apps and more!")
                onResult("SAM: I am your personal AI assistant.")
            }
            else -> {
                speak("Let me search that for you.")
                handleSearch(cmd)
            }
        }
    }

    private fun handleCall(cmd: String) {
        val digitMap = mapOf("zero" to "0","one" to "1","two" to "2","three" to "3",
            "four" to "4","five" to "5","six" to "6","seven" to "7",
            "eight" to "8","nine" to "9","oh" to "0")
        var number = ""
        cmd.split(" ").forEach { word -> digitMap[word]?.let { number += it } }
        val directDigits = cmd.replace(Regex("[^0-9]"), "")
        if (directDigits.length >= 6) number = directDigits
        val simSlot = when {
            cmd.contains("sim one") || cmd.contains("sim 1") -> 1
            cmd.contains("sim two") || cmd.contains("sim 2") -> 2
            else -> 0
        }
        if (number.length >= 6) {
            speak("Calling $number${if (simSlot > 0) " on SIM $simSlot" else ""}.")
            onResult("SAM: Calling $number")
            makeCall(number, simSlot)
        } else {
            val name = cmd.substringAfter("call").replace("sim one","")
                .replace("sim two","").replace("sim 1","").replace("sim 2","").trim()
            if (name.isNotEmpty()) {
                val contactNumber = findContact(name)
                if (contactNumber != null) {
                    speak("Calling $name.")
                    onResult("SAM: Calling $name")
                    makeCall(contactNumber, simSlot)
                } else {
                    speak("I could not find $name in contacts.")
                    onResult("SAM: Contact not found: $name")
                }
            } else {
                speak("Who would you like to call?")
                onResult("SAM: Who should I call?")
            }
        }
    }

    private fun makeCall(number: String, simSlot: Int) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (simSlot > 0) putExtra("com.android.phone.extra.slot", simSlot - 1)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (ex: Exception) { speak("Could not make the call.") }
        }
    }

    private fun findContact(name: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"), null)
            cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) { null }
    }

    private fun handleSearch(cmd: String) {
        val query = cmd.replace("google","").replace("search for","")
            .replace("search","").replace("look up","").trim()
        val q = if (query.isEmpty()) cmd else query
        speak("Searching for $q.")
        onResult("SAM: Searching '$q'")
        try {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}")).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (ex: Exception) { speak("Could not open browser.") }
        }
    }

    private fun handleMusic(cmd: String) {
        val song = cmd.replace("play","").replace("music","").replace("song","").trim()
        speak(if (song.isEmpty()) "Opening music." else "Playing $song.")
        onResult("SAM: Playing '$song'")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://music.youtube.com/search?q=${Uri.encode(song)}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { speak("Could not open music.") }
    }

    private fun handleOpenApp(cmd: String) {
        val appName = cmd.replace("open","").replace("launch","").trim()
        if (appName.isEmpty()) { speak("Which app?"); return }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val match = pm.queryIntentActivities(intent, 0)
            .find { it.loadLabel(pm).toString().lowercase().contains(appName.lowercase()) }
        if (match != null) {
            val label = match.loadLabel(pm).toString()
            speak("Opening $label.")
            onResult("SAM: Opening $label")
            try {
                context.startActivity(
                    pm.getLaunchIntentForPackage(match.activityInfo.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) { speak("Could not open $label.") }
        } else {
            speak("I could not find $appName.")
            onResult("SAM: App not found: $appName")
        }
    }

    private fun handleWhatsApp() {
        speak("Opening WhatsApp.")
        onResult("SAM: Opening WhatsApp")
        try {
            context.startActivity(
                context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { speak("WhatsApp is not installed.") }
    }

    private fun openSettings(type: String) {
        speak("Opening $type settings.")
        onResult("SAM: Opening $type settings")
        val action = when (type) {
            "wifi" -> android.provider.Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            else -> android.provider.Settings.ACTION_SETTINGS
        }
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun handleTimeDate() {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val days = arrayOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
        val months = arrayOf("January","February","March","April","May","June",
            "July","August","September","October","November","December")
        val msg = "It is ${String.format("%02d:%02d", h, m)} on " +
            "${days[cal.get(Calendar.DAY_OF_WEEK)-1]}, " +
            "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}."
        speak(msg); onResult("SAM: $msg")
    }

    private fun handleAlarm() {
        speak("Opening alarm.")
        onResult("SAM: Opening alarm")
        try {
            context.startActivity(
                Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { speak("Could not open clock app.") }
    }

    private fun handleWeather() {
        speak("Checking weather.")
        onResult("SAM: Checking weather")
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=weather+today"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
