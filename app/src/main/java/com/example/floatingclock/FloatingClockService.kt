package com.example.floatingsaleclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FloatingClockService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var clock: TextView
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var showCountdown = true
    private var downX = 0f; private var downY = 0f; private var startX = 0; private var startY = 0

    private val tick = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 200) }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE) {
                render()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channelId = "floating_clock"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Đồng hồ nổi", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId).setContentTitle("Đồng hồ săn sale đang chạy")
                .setContentText("Đồng hồ nổi đang hiển thị trên màn hình").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("Đồng hồ săn sale đang chạy")
                .setContentText("Đồng hồ nổi đang hiển thị trên màn hình").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).build()
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }

        val filter = IntentFilter(ACTION_UPDATE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val prefs = getSharedPreferences("clock", MODE_PRIVATE)
        clock = TextView(this).apply {
            setTextColor(0xFF00E676.toInt())
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textSize = prefs.getInt("size", 24).toFloat()
            gravity = Gravity.CENTER
            setPadding(16, 10, 16, 10)
            setBackgroundColor(0x66000000)
            alpha = prefs.getInt("opacity", 80) / 100f
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; true }
                    MotionEvent.ACTION_MOVE -> { params.x = startX + (event.rawX - downX).toInt(); params.y = startY + (event.rawY - downY).toInt(); wm.updateViewLayout(clock, params); true }
                    MotionEvent.ACTION_UP -> {
                        if (kotlin.math.abs(event.rawX - downX) < 8 && kotlin.math.abs(event.rawY - downY) < 8) { showCountdown = !showCountdown; render() }
                        true
                    }
                    else -> true
                }
            }
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 180 }
        wm.addView(clock, params)
        handler.post(tick)
    }

    private fun render() {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val sale = getSharedPreferences("clock", MODE_PRIVATE).getString("sale_time", "") ?: ""
        val countdown = if (sale.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) {
            val parts = sale.split(":"); val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt()); set(Calendar.MINUTE, parts[1].toInt()); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            var seconds = ((target - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            val h = seconds / 3600; seconds %= 3600; val m = seconds / 60; val s = seconds % 60
            "\nSALE SAU %02d:%02d:%02d".format(h, m, s)
        } else "\nChạm để ẩn/hiện đếm ngược"
        clock.text = time + if (showCountdown) countdown else ""
        val prefs = getSharedPreferences("clock", MODE_PRIVATE)
        clock.textSize = prefs.getInt("size", 24).toFloat()
        clock.alpha = prefs.getInt("opacity", 80) / 100f
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        try { unregisterReceiver(updateReceiver) } catch (_: Exception) {}
        if (::clock.isInitialized) wm.removeView(clock)
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { const val ACTION_UPDATE = "com.example.floatingsaleclock.UPDATE" }
}
