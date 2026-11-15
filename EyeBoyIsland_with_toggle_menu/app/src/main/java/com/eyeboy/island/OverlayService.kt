package com.eyeboy.island

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private var web: WebView? = null
    private var sr: SpeechRecognizer? = null
    private val h = Handler(Looper.getMainLooper())
    private var alive = true

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return START_NOT_STICKY }
        val ch = "eyeboy"
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(ch, "Eye Boy", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, ch).setContentTitle("Eye Boy is listening").setContentText("Say “Eye Boy” to start").setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) else startForeground(1, n)
        getSharedPreferences("eyeboy", MODE_PRIVATE).edit().putBoolean("island_on", true).apply()
        if (web == null) addOverlay()
        listen()
        return START_STICKY
    }

    private fun px(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun addOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lp = WindowManager.LayoutParams(px(160), px(40), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 0 }
        web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(object { @JavascriptInterface fun resize(w: Int, hh: Int) { h.post { lp.width = px(w); lp.height = px(hh); if (web != null) wm.updateViewLayout(web, lp) } } }, "EyeBoyNative")
            loadUrl("file:///android_asset/island.html")
        }
        wm.addView(web, lp)
    }

    private fun send(t: String) { web?.evaluateJavascript("hear(" + JSONObject.quote(t) + ")", null) }
    private fun again(ms: Long) { if (alive) h.postDelayed({ listen() }, ms) }

    private fun listen() {
        if (!alive || !SpeechRecognizer.isRecognitionAvailable(this)) return
        sr?.destroy()
        sr = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(b: Bundle?) { b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { send(it) }; again(300) }
                override fun onError(e: Int) { again(if (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500 else 500) }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"))
        }
    }

    override fun onDestroy() {
        alive = false; h.removeCallbacksAndMessages(null); sr?.destroy()
        web?.let { try { wm.removeView(it) } catch (_: Exception) {}; it.destroy() }
        web = null
        getSharedPreferences("eyeboy", MODE_PRIVATE).edit().putBoolean("island_on", false).apply()
        super.onDestroy()
    }
}
