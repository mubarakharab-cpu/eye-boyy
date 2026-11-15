package com.eyeboy.island

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/** Runs inside Shizuku's process with shell rights. */
class ShellService : IUserService.Stub() {
    override fun destroy() { System.exit(0) }
    override fun exec(cmd: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor()
        return out
    }
}

object EyeBoyShizuku {
    private const val REQ = 4242
    private val FAIL = Regex("(?i)exception|error|denied|unknown")
    fun running() = Shizuku.pingBinder()
    fun authorized() = running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    fun requestAccess() = Shizuku.requestPermission(REQ)

    fun grantAll(ctx: Context, done: (List<Pair<String, Boolean>>) -> Unit) {
        val args = Shizuku.UserServiceArgs(ComponentName(ctx.packageName, ShellService::class.java.name))
            .processNameSuffix("shell").version(1)
        Shizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(n: ComponentName?, b: IBinder?) {
                val sh = IUserService.Stub.asInterface(b)
                val pkg = ctx.packageName
                val steps = listOf(
                    "Display over other apps" to "appops set $pkg SYSTEM_ALERT_WINDOW allow",
                    "Microphone for Eye Boy" to "pm grant $pkg android.permission.RECORD_AUDIO",
                    "Notifications" to "pm grant $pkg android.permission.POST_NOTIFICATIONS",
                    "Write secure settings" to "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS",
                    "Notification access" to "cmd notification allow_listener $pkg/$pkg.EyeBoyNotificationListener",
                    "Usage access" to "appops set $pkg GET_USAGE_STATS allow",
                    "Run in background" to "dumpsys deviceidle whitelist +$pkg"
                )
                Thread { done(steps.map { (l, c) -> l to !FAIL.containsMatchIn(sh.exec(c)) }) }.start()
            }
            override fun onServiceDisconnected(n: ComponentName?) {}
        })
    }
}
