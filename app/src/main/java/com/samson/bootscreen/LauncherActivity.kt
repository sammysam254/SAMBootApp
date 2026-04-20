package com.samson.bootscreen

import android.content.*
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    private var startMenuVisible = false
    private var allAppsVisible = false
    private val apps = mutableListOf<AppInfo>()
    private var soundPool: SoundPool? = null
    private var soundOpen = 0
    private var soundClose = 0
    private var soundNotif = 0
    private var soundClick = 0

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
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_launcher)

        clockText     = findViewById(R.id.clockText)
        dateText      = findViewById(R.id.dateText)
        startMenuPanel = findViewById(R.id.startMenuPanel)
        allAppsPanel  = findViewById(R.id.allAppsPanel)
        notifToast    = findViewById(R.id.notifToast)

        setupSounds()
        loadApps()
        setupTaskbar()
        setupStartMenu()
        setupAllApps()

        handler.post(clockRunnable)

        // Show cinematic welcome notification after 1 second
        handler.postDelayed({ showNotification("SAM", "Welcome back, Samson ✦") }, 1200)
    }

    private fun setupSounds() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            soundOpen  = soundPool?.load(this, R.raw.tone_chime, 1) ?: 0
            soundClose = soundPool?.load(this, R.raw.tone_m,     1) ?: 0
            soundNotif = soundPool?.load(this, R.raw.tone_a,     1) ?: 0
            soundClick = soundPool?.load(this, R.raw.tone_s,     1) ?: 0
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun playSound(id: Int, vol: Float = 0.6f) {
        try { soundPool?.play(id, vol, vol, 1, 0, 1f) } catch (e: Exception) {}
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm",      Locale.getDefault()).format(now)
        dateText.text  = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
    }

    private fun loadApps() {
        apps.clear()
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val pm = packageManager
        pm.queryIntentActivities(intent, 0).forEach { ri ->
            if (ri.activityInfo.packageName != packageName) {
                apps.add(AppInfo(ri.loadLabel(pm).toString(), ri.activityInfo.packageName, ri.loadIcon(pm)))
            }
        }
        apps.sortBy { it.label }
    }

    private fun setupTaskbar() {
        findViewById<View>(R.id.startBtn).setOnClickListener {
            playSound(soundClick)
            toggleStartMenu()
        }
        findViewById<View>(R.id.searchBtn).setOnClickListener {
            playSound(soundClick)
            launchAction(Intent.ACTION_WEB_SEARCH)
        }
        findViewById<View>(R.id.filesBtn).setOnClickListener {
            playSound(soundClick)
            launchPackage("com.google.android.apps.nbu.files") ?:
            launchAction("android.intent.action.VIEW")
        }
        findViewById<View>(R.id.browserBtn).setOnClickListener {
            playSound(soundClick)
            launchUrl("https://google.com")
        }
        findViewById<View>(R.id.cameraBtn).setOnClickListener {
            playSound(soundClick)
            launchAction("android.media.action.IMAGE_CAPTURE")
        }
        findViewById<View>(R.id.chevronBtn).setOnClickListener {
            playSound(soundClick)
        }
        // Close menus on desktop tap
        findViewById<View>(R.id.desktopRoot).setOnClickListener {
            if (startMenuVisible) hideStartMenu()
            else if (allAppsVisible) hideAllApps()
        }
    }

    private fun setupStartMenu() {
        val pinnedGrid = startMenuPanel.findViewById<GridView>(R.id.pinnedGrid)
        pinnedGrid.adapter = AppGridAdapter(this, apps.take(15), compact = true)
        pinnedGrid.setOnItemClickListener { _, _, pos, _ ->
            playSound(soundClick)
            hideStartMenu()
            launchPackage(apps[pos].packageName)
        }

        startMenuPanel.findViewById<TextView>(R.id.allAppsBtn)?.setOnClickListener {
            playSound(soundClick)
            hideStartMenu()
            showAllApps()
        }

        startMenuPanel.findViewById<View>(R.id.powerBtn)?.setOnClickListener {
            playSound(soundClose)
            showPowerMenu()
        }
    }

    private fun setupAllApps() {
        val allGrid = allAppsPanel.findViewById<GridView>(R.id.allAppsGrid)
        allGrid.adapter = AppGridAdapter(this, apps)
        allGrid.setOnItemClickListener { _, _, pos, _ ->
            playSound(soundClick)
            hideAllApps()
            launchPackage(apps[pos].packageName)
        }
        allAppsPanel.findViewById<TextView>(R.id.backToStartBtn)?.setOnClickListener {
            playSound(soundClose)
            hideAllApps()
            showStartMenu()
        }
    }

    private fun toggleStartMenu() {
        if (allAppsVisible) { hideAllApps(); return }
        if (startMenuVisible) hideStartMenu() else showStartMenu()
    }

    private fun showStartMenu() {
        startMenuVisible = true
        playSound(soundOpen, 0.4f)
        startMenuPanel.visibility = View.VISIBLE
        startMenuPanel.alpha = 0f
        startMenuPanel.translationY = 300f
        startMenuPanel.scaleX = 0.92f
        startMenuPanel.scaleY = 0.92f
        startMenuPanel.animate()
            .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    private fun hideStartMenu() {
        startMenuVisible = false
        startMenuPanel.animate()
            .alpha(0f).translationY(200f).scaleX(0.94f).scaleY(0.94f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { startMenuPanel.visibility = View.GONE }
            .start()
    }

    private fun showAllApps() {
        allAppsVisible = true
        allAppsPanel.visibility = View.VISIBLE
        allAppsPanel.alpha = 0f
        allAppsPanel.translationX = 300f
        allAppsPanel.animate()
            .alpha(1f).translationX(0f)
            .setDuration(280)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideAllApps() {
        allAppsVisible = false
        allAppsPanel.animate()
            .alpha(0f).translationX(300f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { allAppsPanel.visibility = View.GONE }
            .start()
    }

    fun showNotification(title: String, message: String) {
        try {
            playSound(soundNotif, 0.5f)
            val toast = notifToast
            toast.findViewById<TextView>(R.id.notifTitle)?.text = title
            toast.findViewById<TextView>(R.id.notifText)?.text = message
            toast.findViewById<TextView>(R.id.notifTime)?.text =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            toast.visibility = View.VISIBLE
            toast.alpha = 0f
            toast.translationY = -80f
            toast.animate()
                .alpha(1f).translationY(0f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()

            // Auto hide after 4 seconds
            handler.postDelayed({
                toast.animate()
                    .alpha(0f).translationY(-60f)
                    .setDuration(300)
                    .withEndAction { toast.visibility = View.GONE }
                    .start()
            }, 4000)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun launchPackage(pkg: String): Unit? {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) { startActivity(intent); Unit } else null
        } catch (e: Exception) { null }
    }

    private fun launchAction(action: String) {
        try { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: Exception) { Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show() }
    }

    private fun launchUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) { Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show() }
    }

    private fun showPowerMenu() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Power")
            .setItems(arrayOf("🔄 Restart", "⏻ Power Off", "✕ Cancel")) { _, w ->
                when (w) {
                    0 -> Toast.makeText(this, "Hold power button to restart", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Hold power button to power off", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    override fun onBackPressed() {
        when {
            allAppsVisible -> { playSound(soundClose); hideAllApps() }
            startMenuVisible -> { playSound(soundClose); hideStartMenu() }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
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
