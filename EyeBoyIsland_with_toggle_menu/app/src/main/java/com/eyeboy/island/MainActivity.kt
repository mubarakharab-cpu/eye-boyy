package com.eyeboy.island

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var toggleButton: Button
    private val onPerm = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { refresh() } }
    private val onBinder = Shizuku.OnBinderReceivedListener { runOnUiThread { refresh() } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 110, 48, 48); setBackgroundColor(0xFF0B0D14.toInt()) }
        fun text(t: String, sz: Float, c: Int = 0xFFFFFFFF.toInt()) = TextView(this).apply { text = t; textSize = sz; setTextColor(c); setPadding(0, 12, 0, 12) }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
        col.addView(text("Eye Boy Island", 26f))
        status = text("", 14f, 0xFFB0B8CC.toInt()); col.addView(status)
        col.addView(btn("1. Open Shizuku") {
            val i = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (i != null) startActivity(i) else toast("Install Shizuku first")
        })
        col.addView(btn("2. Allow Eye Boy in Shizuku") {
            if (EyeBoyShizuku.running()) EyeBoyShizuku.requestAccess() else toast("Start Shizuku first")
        })
        col.addView(btn("3. Grant all permissions") {
            if (!EyeBoyShizuku.authorized()) { toast("Do step 2 first") } else {
                status.text = "Granting…"
                EyeBoyShizuku.grantAll(this) { r ->
                    runOnUiThread { status.text = r.joinToString("\n") { (if (it.second) "✓ " else "✗ ") + it.first } }
                }
            }
        })
        toggleButton = btn("Turn Eye Boy ON") { toggleIsland() }
        col.addView(toggleButton)
        col.addView(text("Use the button above whenever you want to turn the island on or off.", 13f, 0xFF8890A8.toInt()))
        col.addView(text("Say “Eye Boy, set a timer for 10 minutes”. Commands only run after “Eye Boy”.", 13f, 0xFF8890A8.toInt()))
        setContentView(ScrollView(this).apply { addView(col) })
        Shizuku.addRequestPermissionResultListener(onPerm)
        Shizuku.addBinderReceivedListenerSticky(onBinder)
    }

    private fun toggleIsland() {
        val prefs = getSharedPreferences("eyeboy", MODE_PRIVATE)
        if (prefs.getBoolean("island_on", false)) {
            stopService(Intent(this, OverlayService::class.java))
            prefs.edit().putBoolean("island_on", false).apply()
            refreshToggle()
        } else {
            start()
        }
    }

    private fun refreshToggle() {
        if (!::toggleButton.isInitialized) return
        val on = getSharedPreferences("eyeboy", MODE_PRIVATE).getBoolean("island_on", false)
        toggleButton.text = if (on) "Turn Eye Boy OFF" else "Turn Eye Boy ON"
        status.text = (status.text?.toString()?.substringBefore("\n") ?: "") +
            if (on) "\nEye Boy Island: ON" else "\nEye Boy Island: OFF"
    }

    private fun start() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1); return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        getSharedPreferences("eyeboy", MODE_PRIVATE).edit().putBoolean("island_on", true).apply()
        refreshToggle()
    }

    private fun refresh() {
        status.text = when {
            !EyeBoyShizuku.running() -> "Shizuku: not running"
            !EyeBoyShizuku.authorized() -> "Shizuku: running, not allowed yet"
            else -> "Shizuku: ready"
        }
    }
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
    override fun onResume() { super.onResume(); refresh(); refreshToggle() }
    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(onPerm); Shizuku.removeBinderReceivedListener(onBinder); super.onDestroy()
    }
}
