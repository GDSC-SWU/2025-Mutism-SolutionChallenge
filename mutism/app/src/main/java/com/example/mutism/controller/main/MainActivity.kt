package com.example.mutism.controller.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mutism.R
import com.example.mutism.controller.myPage.MyPageActivity
import com.example.mutism.controller.noiseSelectPage.NoiseSelectActivity
import com.example.mutism.databinding.ActivityMainBinding
import com.example.mutism.dialog.SelectNoiseDialog

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isRecording = false
    private var selectNoiseDialog: SelectNoiseDialog? = null
    private val noiseSelectLauncher = registerNoiseSelectLauncher()
    private lateinit var listContainer: LinearLayout
    private lateinit var whiteNoiseDialogReceiver: BroadcastReceiver

    private val broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    ForegroundService.ACTION_UPDATE -> {
                        val newText = intent.getStringExtra("new_text") ?: return
                        Log.d("MainActivity", "Broadcast 수신: $newText")
                        runOnUiThread {
                            addTextItem(newText)
                        }
                    }
                    "com.mutism.ACTION_EMERGENCY_CALL" -> {
                        if (intent.getBooleanExtra("emergency", false)) {
                            makeEmergencyCall()
                        }
                    }

                    "com.mutism.ACTION_SHOW_STOP_WHITE_NOISE" -> {
                        runOnUiThread {
                            binding.btnStopWhiteNoiseContainer.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

    @SuppressLint("ImplicitSamInstance")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listContainer = binding.listContainer

        binding.tvRecording.visibility = View.GONE
        binding.btnStopWhiteNoiseContainer.visibility = View.GONE

        binding.btnStart.setOnClickListener {
            val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
            val selectedNoiseTags = sharedPrefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet())

            if (!checkUserInfoFilled()) {
                if (!checkUserInfoFilled()) {
                    com.example.mutism.dialog
                        .NoUserInfoDialog(this)
                        .show()
                    return@setOnClickListener
                }
            }

            if (selectedNoiseTags.isNullOrEmpty()) {
                if (selectNoiseDialog?.isShowing != true) {
                    selectNoiseDialog =
                        SelectNoiseDialog(this) {
                            val intent = Intent(this, NoiseSelectActivity::class.java)
                            noiseSelectLauncher.launch(intent)
                        }
                    selectNoiseDialog?.show()
                }
            } else {
                isRecording = !isRecording
                updateRecordingUI()

                if (isRecording) {
                    if (hasRecordPermission()) {
                        startForegroundService()
                        Toast.makeText(this, "Start recording", Toast.LENGTH_SHORT).show()
                    } else {
                        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
                    }
                } else {
                    stopService(Intent(this, ForegroundService::class.java))
                    Toast.makeText(this, "Stop recording", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnMyPage.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        binding.btnSos.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                makeEmergencyCall()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
            }
        }

        binding.btnStopWhiteNoise.setOnClickListener {
            WhiteNoiseManager.stopWhiteNoise {
                binding.btnStopWhiteNoiseContainer.visibility = View.GONE
            }
        }

        checkNotificationPermissionAndStatus()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter =
            IntentFilter().apply {
                addAction(ForegroundService.ACTION_UPDATE)
                addAction("com.mutism.ACTION_EMERGENCY_CALL")
                addAction("com.mutism.ACTION_SHOW_STOP_WHITE_NOISE")
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiver)
    }

    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun updateRecordingUI() {
        val rootLayout = findViewById<View>(R.id.main)
        Log.d("MainActivity", "updateRecordingUI: isRecording=$isRecording, isRunning=${ForegroundService.isRunning}")

        val layoutParams = binding.btnStart.layoutParams
        if (isRecording) {
            binding.btnStart.setImageResource(R.drawable.btn_stop)
            layoutParams.width = 214.dpToPx()
            layoutParams.height = 214.dpToPx()
            rootLayout.setBackgroundResource(R.drawable.bg_main2)
            binding.tvWelcome.visibility = View.GONE
            binding.tvRecording.visibility = View.VISIBLE
            binding.tvNowHear.visibility = View.VISIBLE
            binding.mainTvExplain.text = getString(R.string.text_main_stop)
            binding.listContainer.visibility = View.VISIBLE
        } else {
            binding.btnStart.setImageResource(R.drawable.btn_start)
            layoutParams.width = 264.dpToPx()
            layoutParams.height = 264.dpToPx()
            rootLayout.setBackgroundResource(R.drawable.bg_main3)
            binding.tvWelcome.visibility = View.VISIBLE
            binding.tvRecording.visibility = View.GONE
            binding.tvNowHear.visibility = View.GONE
            binding.mainTvExplain.text = getString(R.string.text_main_start)
            binding.listContainer.visibility = View.GONE
        }
    }

    fun makeEmergencyCall() {
        try {
            val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val rawContact = sharedPrefs.getString(KEY_EMERGENCY_CONTACT, null)
            val contact = if (rawContact.isNullOrBlank()) EMERGENCY_NUMBER else rawContact
            val intent = Intent(Intent.ACTION_CALL).apply { data = "tel:$contact".toUri() }
            startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission error", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Call failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTextItem(newText: String) {
        val count = listContainer.childCount
        Log.d("MainActivity", "listContainer count: $count")
        if (count < 3) {
            listContainer.addView(createTextView(newText))
        } else {
            listContainer.removeAllViews()
            listContainer.addView(createTextView(newText))
        }
    }

    private fun createTextView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.color_noise_bg))
            textSize = 24f
            setPadding(20, 6, 20, 6)
            background = ContextCompat.getDrawable(context, R.drawable.bg_classified_sound)
            layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 12
                    gravity = Gravity.CENTER
                }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startForegroundService()
        }
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun registerNoiseSelectLauncher() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Noise selection is complete.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkNotificationPermissionAndStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) {
            AlertDialog
                .Builder(this)
                .setTitle("Notifications are off")
                .setMessage("Enable notifications to receive sound alerts.")
                .setPositiveButton("Settings") { _, _ ->
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = "package:$packageName".toUri()
                            }
                        }
                    startActivity(intent)
                }.setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun checkUserInfoFilled(): Boolean {
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val requiredKeys = listOf("name", "autism_level", "gender", "age", "emergency contact", "relax_method")

        return requiredKeys.all { key ->
            val value = userPrefs.getString(key, null)
            !value.isNullOrBlank()
        }
    }

    companion object {
        const val REQUEST_CALL_PERMISSION = 100
        const val EMERGENCY_NUMBER = "1234"
        const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
        const val REQUEST_RECORD_AUDIO = 1337
        const val MODEL_FILE = "yamnet.tflite"
        const val MINIMUM_DISPLAY_THRESHOLD: Float = 0.3f
        private const val KEY_EMERGENCY_CONTACT = "emergency contact"
    }
}
