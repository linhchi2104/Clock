package com.example.floatingsaleclock

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("clock", MODE_PRIVATE) }
    private lateinit var opacityLabel: TextView
    private lateinit var sizeLabel: TextView
    private lateinit var countdownInput: EditText
    private lateinit var preview: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val previewTick = object : Runnable {
        override fun run() {
            if (::preview.isInitialized) {
                preview.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            }
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        buildScreen()
        handler.post(previewTick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(previewTick)
        super.onDestroy()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101810.toInt())
        }
        val title = TextView(this).apply {
            text = "ĐỒNG HỒ SĂN SALE"
            textSize = 23f; setTextColor(0xFF00E676.toInt()); gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())
        val subtitle = TextView(this).apply {
            text = "Đồng hồ nổi trên Shopee, TikTok Shop và ứng dụng khác"
            textSize = 14f; setTextColor(0xFFB7C8B9.toInt()); gravity = Gravity.CENTER
            setPadding(0, 10, 0, 24)
        }
        root.addView(subtitle, matchWrap())

        preview = TextView(this).apply {
            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            textSize = 34f; typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(0xFF00E676.toInt()); gravity = Gravity.CENTER
            setPadding(10, 18, 10, 18)
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, -2))

        val start = Button(this).apply {
            text = "BẬT ĐỒNG HỒ NỔI"
            setOnClickListener { startOverlay() }
        }
        root.addView(start, matchWrap())
        val stop = Button(this).apply {
            text = "TẮT ĐỒNG HỒ NỔI"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingClockService::class.java)) }
        }
        root.addView(stop, matchWrap())

        addLabel(root, "Độ trong suốt")
        opacityLabel = addValue(root, "80%")
        val opacity = SeekBar(this).apply {
            max = 100; progress = prefs.getInt("opacity", 80)
            setOnSeekBarChangeListener(listener { p ->
                prefs.edit().putInt("opacity", p).apply(); opacityLabel.text = "$p%"; updateOverlay()
            })
        }
        root.addView(opacity, matchWrap())

        addLabel(root, "Kích thước chữ")
        sizeLabel = addValue(root, "24 sp")
        val size = SeekBar(this).apply {
            max = 40; progress = (prefs.getInt("size", 24) - 16).coerceIn(0, 40)
            setOnSeekBarChangeListener(listener { p ->
                val v = p + 16; prefs.edit().putInt("size", v).apply(); sizeLabel.text = "$v sp"; updateOverlay()
            })
        }
        root.addView(size, matchWrap())

        addLabel(root, "Giờ mở sale (HH:mm, ví dụ 20:00)")
        countdownInput = EditText(this).apply {
            setSingleLine(); hint = "20:00"; textSize = 18f
            setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF829082.toInt())
            setText(prefs.getString("sale_time", "") ?: "")
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xFF263326.toInt())
        }
        root.addView(countdownInput, matchWrap())
        val save = Button(this).apply {
            text = "LƯU GIỜ MỞ SALE"
            setOnClickListener {
                val value = countdownInput.text.toString().trim()
                if (value.isNotEmpty() && !Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)) {
                    Toast.makeText(this@MainActivity, "Nhập giờ theo định dạng HH:mm", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("sale_time", value).apply(); updateOverlay()
                    Toast.makeText(this@MainActivity, "Đã lưu giờ mở sale", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(save, matchWrap())
        val note = TextView(this).apply {
            text = "Mẹo: Sau khi bật, cấp quyền ‘Hiển thị trên ứng dụng khác’. Kéo đồng hồ để di chuyển; chạm vào đồng hồ để ẩn/hiện phần đếm ngược."
            textSize = 13f; setTextColor(0xFFB7C8B9.toInt()); setPadding(0, 20, 0, 0)
        }
        root.addView(note, matchWrap())
        setContentView(ScrollView(this).apply { addView(root) })
        opacityLabel.text = "${opacity.progress}%"
        sizeLabel.text = "${size.progress + 16} sp"
    }

    private fun listener(action: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { action(progress) }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun addLabel(root: LinearLayout, text: String) {
        root.addView(TextView(this).apply { this.text = text; textSize = 15f; setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 18, 0, 4) }, matchWrap())
    }
    private fun addValue(root: LinearLayout, text: String) = TextView(this).apply {
        this.text = text; textSize = 13f; setTextColor(0xFF00E676.toInt())
        root.addView(this, matchWrap())
    }
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).setTitle("Cần cấp quyền")
                .setMessage("Cho phép ứng dụng hiển thị đồng hồ nổi trên ứng dụng khác.")
                .setPositiveButton("Mở cài đặt") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }.setNegativeButton("Để sau", null).show()
            return
        }
        val serviceIntent = Intent(this, FloatingClockService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        Toast.makeText(this, "Đã bật đồng hồ nổi", Toast.LENGTH_SHORT).show()
    }
    private fun updateOverlay() { sendBroadcast(Intent(FloatingClockService.ACTION_UPDATE).setPackage(packageName)) }
}
