package com.samson.bootscreen

import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.*

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class LauncherActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var startMenuPanel: View
    private lateinit var taskbar: View
    private var startMenuVisible = false
    private val apps = mutableListOf<AppInfo>()

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_launcher)

        clockText     = findViewById(R.id.clockText)
        dateText      = findViewById(R.id.dateText)
        startMenuPanel = findViewById(R.id.startMenuPanel)
        taskbar       = findViewById(R.id.taskbar)

        loadApps()
        setupDesktopGrid()
        setupTaskbar()
        setupStartMenu()

        handler.post(clockRunnable)
    }

    private fun updateClock() {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val now = Date()
        clockText.text = timeFmt.format(now)
        dateText.text  = dateFmt.format(now)
    }

    private fun loadApps() {
        apps.clear()
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = packageManager
        val resolvedApps: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        for (ri in resolvedApps) {
            val label = ri.loadLabel(pm).toString()
            val pkg   = ri.activityInfo.packageName
            val icon  = ri.loadIcon(pm)
            if (pkg != packageName) {
                apps.add(AppInfo(label, pkg, icon))
            }
        }
        apps.sortBy { it.label }
    }

    private fun setupDesktopGrid() {
        val grid = findViewById<GridView>(R.id.desktopGrid)
        grid.adapter = AppGridAdapter(this, apps.take(12))
        grid.setOnItemClickListener { _, _, pos, _ ->
            launchApp(apps[pos].packageName)
        }
    }

    private fun setupTaskbar() {
        // Start button
        findViewById<View>(R.id.startBtn).setOnClickListener {
            toggleStartMenu()
        }
        // Settings button
        findViewById<View>(R.id.settingsBtn).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        // Search button
        findViewById<View>(R.id.searchBtn).setOnClickListener {
            try {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                startActivity(searchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupStartMenu() {
        startMenuPanel.visibility = View.GONE
        startMenuPanel.alpha = 0f

        val allAppsGrid = startMenuPanel.findViewById<GridView>(R.id.allAppsGrid)
        allAppsGrid?.adapter = AppGridAdapter(this, apps)
        allAppsGrid?.setOnItemClickListener { _, _, pos, _ ->
            hideStartMenu()
            launchApp(apps[pos].packageName)
        }

        // Power button
        startMenuPanel.findViewById<View>(R.id.powerBtn)?.setOnClickListener {
            showPowerMenu()
        }

        // Close start menu on outside tap
        findViewById<View>(R.id.desktopRoot).setOnClickListener {
            if (startMenuVisible) hideStartMenu()
        }
    }

    private fun toggleStartMenu() {
        if (startMenuVisible) hideStartMenu() else showStartMenu()
    }

    private fun showStartMenu() {
        startMenuVisible = true
        startMenuPanel.visibility = View.VISIBLE
        startMenuPanel.translationY = 200f
        startMenuPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideStartMenu() {
        startMenuVisible = false
        startMenuPanel.animate()
            .alpha(0f)
            .translationY(200f)
            .setDuration(200)
            .withEndAction { startMenuPanel.visibility = View.GONE }
            .start()
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPowerMenu() {
        val options = arrayOf("Restart", "Power Off", "Cancel")
        android.app.AlertDialog.Builder(this)
            .setTitle("Power")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        try {
                            val pm = getSystemService(POWER_SERVICE) as PowerManager
                            pm.reboot(null)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Need root to restart", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        try {
                            startActivity(Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN"))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Use power button", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.show()
    }

    override fun onBackPressed() {
        if (startMenuVisible) hideStartMenu()
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
        handler.removeCallbacksAndMessages(null)
    }
}

class AppGridAdapter(
    context: Context,
    private val appList: List<AppInfo>
) : ArrayAdapter<AppInfo>(context, 0, appList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_app, parent, false)
        val app = appList[position]
        view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(app.icon)
        view.findViewById<TextView>(R.id.appLabel).text = app.label
        return view
    }
}
