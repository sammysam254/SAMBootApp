package com.samson.samlauncher

import android.Manifest
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.*

class LauncherActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var startMenuPanel: View
    private lateinit var allAppsPanel: View
    private lateinit var notifToast: View
    private lateinit var aiPanel: View
    private lateinit var aiText: TextView
    private lateinit var aiMicBtn: View
    private lateinit var aiStatusText: TextView
    private lateinit var searchBar: EditText

    private var startMenuVisible = false
    private var allAppsVisible = false
    private var aiPanelVisible = false

    private val apps = mutableListOf<AppInfo>()
    private var soundPool: SoundPool? = null
    private var soundOpen = 0
    private var soundClose = 0
    private var soundNotif = 0
    private var soundClick = 0
    private lateinit var samAI: SamAI

    companion object {
        private const val REQ_PERMS = 101
        private const val REQ_DEFAULT = 104
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_launcher)

        clockText      = findViewById(R.id.clockText)
        dateText       = findViewById(R.id.dateText)
        startMenuPanel = findViewById(R.id.startMenuPanel)
        allAppsPanel   = findViewById(R.id.allAppsPanel)
        notifToast     = findViewById(R.id.notifToast)
        aiPanel        = findViewById(R.id.aiPanel)
        aiText         = findViewById(R.id.aiText)
        aiMicBtn       = findViewById(R.id.aiMicBtn)
        aiStatusText   = findViewById(R.id.aiStatusText)

        setupSounds()
        setupAI()
        loadApps()
        setupTaskbar()
        setupStartMenu()
        setupAllApps()
        setupAIPanel()

        handler.post(clockRunnable)
        handler.postDelayed({ checkDefaultLauncher() }, 1500)
        handler.postDelayed({
            showNotification("SAM", "Welcome back, Samson ✦ Tap 🤖 to talk to SAM AI")
        }, 2000)
    }

    private fun checkDefaultLauncher() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (info?.activityInfo?.packageName != packageName) {
                AlertDialog.Builder(this)
                    .setTitle("Set SAM as Default Launcher")
                    .setMessage("Set SAM as your home screen to enable all features — AI assistant, boot animation, live wallpaper.")
                    .setPositiveButton("Set Now") { _, _ -> requestDefaultLauncher() }
                    .setNegativeButton("Later", null)
                    .show()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun requestDefaultLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_DEFAULT)
                    return
                }
            }
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun setupSounds() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            soundPool?.setOnLoadCompleteListener { _, _, _ -> }
            soundOpen  = soundPool?.load(this, R.raw.tone_chime, 1) ?: 0
            soundClose = soundPool?.load(this, R.raw.tone_m,     1) ?: 0
            soundNotif = soundPool?.load(this, R.raw.tone_a,     1) ?: 0
            soundClick = soundPool?.load(this, R.raw.tone_s,     1) ?: 0
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun playSound(id: Int, vol: Float = 0.5f) {
        try { if (id != 0) soundPool?.play(id, vol, vol, 1, 0, 1f) } catch (e: Exception) {}
    }

    private fun setupAI() {
        samAI = SamAI(
            context = this,
            onResult = { result -> runOnUiThread { aiText.text = result } },
            onListening = { listening ->
                runOnUiThread {
                    aiStatusText.text = if (listening) "🔴  Listening..." else "Tap mic to speak"
                    aiMicBtn.animate()
                        .scaleX(if (listening) 1.2f else 1f)
                        .scaleY(if (listening) 1.2f else 1f)
                        .setDuration(200).start()
                }
            },
            onSpeak = { text ->
                runOnUiThread {
                    aiText.text = "SAM: $text"
                    showNotification("SAM AI", text)
                }
            }
        )
        // Request permissions
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CALL_PHONE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_CONTACTS)
        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQ_PERMS)
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText.text  = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
    }

    private fun loadApps() {
        apps.clear()
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA).forEach { ri ->
            if (ri.activityInfo.packageName != packageName) {
                apps.add(AppInfo(
                    ri.loadLabel(packageManager).toString(),
                    ri.activityInfo.packageName,
                    ri.loadIcon(packageManager)
                ))
            }
        }
        apps.sortBy { it.label }
    }

    private fun setupTaskbar() {
        findViewById<View>(R.id.startBtn).setOnClickListener {
            playSound(soundClick); toggleStartMenu()
        }
        findViewById<View>(R.id.aiBtn).setOnClickListener {
            playSound(soundClick); toggleAIPanel()
        }
        findViewById<View>(R.id.filesBtn).setOnClickListener {
            playSound(soundClick)
            if (!safelaunch("com.google.android.apps.nbu.files"))
                if (!safelaunch("com.android.documentsui"))
                    safelaunch("com.android.fileexplorer")
        }
        findViewById<View>(R.id.browserBtn).setOnClickListener {
            playSound(soundClick)
            safeStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")))
        }
        findViewById<View>(R.id.cameraBtn).setOnClickListener {
            playSound(soundClick)
            safeStartActivity(Intent("android.media.action.IMAGE_CAPTURE"))
        }
        findViewById<View>(R.id.desktopRoot).setOnClickListener {
            when {
                startMenuVisible -> hideStartMenu()
                allAppsVisible   -> hideAllApps()
                aiPanelVisible   -> hideAIPanel()
            }
        }
    }

    private fun setupStartMenu() {
        val pinnedGrid = startMenuPanel.findViewById<GridView>(R.id.pinnedGrid)
        pinnedGrid.adapter = AppGridAdapter(this, apps.take(15), true)
        pinnedGrid.setOnItemClickListener { _, _, pos, _ ->
            playSound(soundClick)
            hideStartMenu()
            handler.postDelayed({ safelaunch(apps[pos].packageName) }, 250)
        }

        searchBar = startMenuPanel.findViewById(R.id.startMenuSearch)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { filterApps(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        startMenuPanel.findViewById<TextView>(R.id.allAppsBtn)?.setOnClickListener {
            playSound(soundClick); hideStartMenu(); showAllApps()
        }
        startMenuPanel.findViewById<View>(R.id.powerBtn)?.setOnClickListener {
            playSound(soundClose); showPowerMenu()
        }
    }

    private fun filterApps(query: String) {
        val searchGrid  = startMenuPanel.findViewById<GridView>(R.id.searchResultGrid)
        val pinnedGrid  = startMenuPanel.findViewById<GridView>(R.id.pinnedGrid)
        val pinnedLabel = startMenuPanel.findViewById<TextView>(R.id.pinnedLabel)
        val pinnedRow   = startMenuPanel.findViewById<View>(R.id.pinnedLabelRow)

        if (query.isEmpty()) {
            searchGrid?.visibility  = View.GONE
            pinnedGrid?.visibility  = View.VISIBLE
            pinnedLabel?.visibility = View.VISIBLE
            pinnedRow?.visibility   = View.VISIBLE
        } else {
            val results = apps.filter { it.label.lowercase().contains(query.lowercase()) }
            searchGrid?.visibility  = View.VISIBLE
            pinnedGrid?.visibility  = View.GONE
            pinnedRow?.visibility   = View.GONE
            searchGrid?.adapter = AppGridAdapter(this, results)
            searchGrid?.setOnItemClickListener { _, _, pos, _ ->
                playSound(soundClick)
                hideStartMenu()
                handler.postDelayed({ safelaunch(results[pos].packageName) }, 250)
            }
        }
    }

    private fun setupAllApps() {
        val grid = allAppsPanel.findViewById<GridView>(R.id.allAppsGrid)
        grid.adapter = AppGridAdapter(this, apps)
        grid.setOnItemClickListener { _, _, pos, _ ->
            playSound(soundClick)
            hideAllApps()
            handler.postDelayed({ safelaunch(apps[pos].packageName) }, 250)
        }
        allAppsPanel.findViewById<TextView>(R.id.backToStartBtn)?.setOnClickListener {
            playSound(soundClose); hideAllApps(); showStartMenu()
        }
    }

    private fun setupAIPanel() {
        aiMicBtn.setOnClickListener { playSound(soundClick); samAI.startListening() }
        aiPanel.findViewById<View>(R.id.aiCloseBtn)?.setOnClickListener {
            playSound(soundClose); hideAIPanel()
        }
    }

    // ── Open app safely ────────────────────────────────────────────────────
    private fun safelaunch(pkg: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun safeStartActivity(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Menus ──────────────────────────────────────────────────────────────
    private fun toggleStartMenu() {
        when {
            allAppsVisible -> hideAllApps()
            aiPanelVisible -> hideAIPanel()
            startMenuVisible -> hideStartMenu()
            else -> showStartMenu()
        }
    }

    private fun toggleAIPanel() {
        if (aiPanelVisible) hideAIPanel() else showAIPanel()
    }

    private fun showStartMenu() {
        startMenuVisible = true
        playSound(soundOpen, 0.4f)
        startMenuPanel.visibility = View.VISIBLE
        startMenuPanel.alpha = 0f
        startMenuPanel.translationY = 300f
        startMenuPanel.scaleX = 0.92f
        startMenuPanel.scaleY = 0.92f
        startMenuPanel.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(320).setInterpolator(OvershootInterpolator(1.2f)).start()
    }

    private fun hideStartMenu() {
        startMenuVisible = false
        searchBar.setText("")
        filterApps("")
        startMenuPanel.animate().alpha(0f).translationY(200f).scaleX(0.94f).scaleY(0.94f)
            .setDuration(220).setInterpolator(DecelerateInterpolator())
            .withEndAction { startMenuPanel.visibility = View.GONE }.start()
    }

    private fun showAllApps() {
        allAppsVisible = true
        allAppsPanel.visibility = View.VISIBLE
        allAppsPanel.alpha = 0f
        allAppsPanel.translationX = 400f
        allAppsPanel.animate().alpha(1f).translationX(0f)
            .setDuration(280).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun hideAllApps() {
        allAppsVisible = false
        allAppsPanel.animate().alpha(0f).translationX(400f)
            .setDuration(220).setInterpolator(DecelerateInterpolator())
            .withEndAction { allAppsPanel.visibility = View.GONE }.start()
    }

    private fun showAIPanel() {
        aiPanelVisible = true
        playSound(soundOpen, 0.4f)
        aiPanel.visibility = View.VISIBLE
        aiPanel.alpha = 0f
        aiPanel.translationY = 400f
        aiPanel.animate().alpha(1f).translationY(0f)
            .setDuration(350).setInterpolator(OvershootInterpolator(1.1f)).start()
    }

    private fun hideAIPanel() {
        aiPanelVisible = false
        aiPanel.animate().alpha(0f).translationY(300f)
            .setDuration(250).setInterpolator(DecelerateInterpolator())
            .withEndAction { aiPanel.visibility = View.GONE }.start()
    }

    fun showNotification(title: String, message: String) {
        try {
            playSound(soundNotif, 0.4f)
            notifToast.findViewById<TextView>(R.id.notifTitle)?.text = title
            notifToast.findViewById<TextView>(R.id.notifText)?.text = message
            notifToast.findViewById<TextView>(R.id.notifTime)?.text =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            notifToast.visibility = View.VISIBLE
            notifToast.alpha = 0f
            notifToast.translationY = -100f
            notifToast.animate().alpha(1f).translationY(0f)
                .setDuration(400).setInterpolator(OvershootInterpolator(1.3f)).start()
            handler.postDelayed({
                notifToast.animate().alpha(0f).translationY(-80f).setDuration(300)
                    .withEndAction { notifToast.visibility = View.GONE }.start()
            }, 4000)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showPowerMenu() {
        AlertDialog.Builder(this)
            .setTitle("⏻  Power")
            .setItems(arrayOf("🔄  Restart", "⏻  Power Off", "✕  Cancel")) { _, w ->
                when (w) {
                    0 -> Toast.makeText(this, "Hold power button → Restart", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Hold power button → Power off", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    override fun onBackPressed() {
        when {
            aiPanelVisible   -> { playSound(soundClose); hideAIPanel() }
            allAppsVisible   -> { playSound(soundClose); hideAllApps() }
            startMenuVisible -> { playSound(soundClose); hideStartMenu() }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockRunnable)
        loadApps()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        samAI.destroy()
        soundPool?.release()
        handler.removeCallbacksAndMessages(null)
    }
}

class AppGridAdapter(
    context: Context,
    private val list: List<AppInfo>,
    private val compact: Boolean = false
) : ArrayAdapter<AppInfo>(context, 0, list) {
    override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
        val v = cv ?: LayoutInflater.from(context).inflate(R.layout.item_app, parent, false)
        val app = list[pos]
        v.findViewById<ImageView>(R.id.appIcon)?.setImageDrawable(app.icon)
        v.findViewById<TextView>(R.id.appLabel)?.text = app.label
        return v
    }
}
