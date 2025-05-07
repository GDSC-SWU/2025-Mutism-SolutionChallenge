package com.example.mutism.controller.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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

    // ActivityResultLauncher
    private val noiseSelectLauncher = registerNoiseSelectLauncher()
    private lateinit var listContainer: LinearLayout
    private lateinit var receiver: BroadcastReceiver

    private val updateListReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                Log.d("mainActivity", "BroadcastReceiver received")
                val newText = intent?.getStringExtra("new_text") ?: return
                Log.d("mainActivity", newText)
                addTextItem(newText)
            }
        }

    @SuppressLint("ImplicitSamInstance")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listContainer = binding.listContainer

        receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (intent.action == "com.mutism.UPDATE_LIST") {
                        // 원하는 작업 수행
                    }
                }
            }

        binding.tvRecording.visibility = View.GONE

        // 코드 통합 -> 확인 필요
        binding.btnStart.setOnClickListener {
            val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
            val selectedNoiseTags = sharedPrefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet())

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

                if (ForegroundService.isRunning) {
                    stopService(Intent(this, ForegroundService::class.java))
                    Toast.makeText(this, "Stop recording", Toast.LENGTH_SHORT).show()
                } else {
                    if (hasRecordPermission()) {
                        startForegroundService()
                        Toast.makeText(this, "Start recording", Toast.LENGTH_SHORT).show()
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            REQUEST_RECORD_AUDIO,
                        )
                    }
                }
            }
        }

        binding.btnMyPage.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }

        binding.btnSos.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                makeEmergencyCall()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                    Toast.makeText(this, "This app needs permission to make emergency calls.", Toast.LENGTH_LONG).show()
                }

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    REQUEST_CALL_PERMISSION,
                )
            }
        }

        // 알림 권한 및 설정 상태 확인
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
        val filter = IntentFilter("com.mutism.UPDATE_LIST")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateListReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateListReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(updateListReceiver)
    }

    private fun updateRecordingUI() {
        val rootLayout = findViewById<View>(R.id.main)
        if (!ForegroundService.isRunning) {
            binding.btnStart.setImageResource(R.drawable.btn_stop)
            rootLayout.setBackgroundResource(R.drawable.bg_main2)
            binding.tvWelcome.visibility = View.GONE
            binding.tvRecording.visibility = View.VISIBLE
            binding.tvNowHear.visibility = View.VISIBLE
            binding.mainTvExplain.text = getString(R.string.text_main_stop)
            binding.listContainer.visibility = View.VISIBLE
        } else {
            binding.btnStart.setImageResource(R.drawable.btn_start)
            rootLayout.setBackgroundResource(R.drawable.bg_main3)
            binding.tvWelcome.visibility = View.VISIBLE
            binding.tvRecording.visibility = View.GONE
            binding.tvNowHear.visibility = View.GONE
            binding.mainTvExplain.text = getString(R.string.text_main_start)
            binding.listContainer.visibility = View.GONE
        }
    }

    private fun makeEmergencyCall() {
        try {
            val intent =
                Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$EMERGENCY_NUMBER".toUri()
                }
            startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to make call. Permission not granted.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Call failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun addTextItem(newText: String) {
        val count = listContainer.childCount
        if (count < 3) {
            val textView = createTextView(newText)
            listContainer.addView(textView)
        } else {
            listContainer.removeAllViews()
            val textView = createTextView(newText)
            listContainer.addView(textView)
        }
    }

    private fun createTextView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.color_noise_bg))
            textSize = 24f
            setPadding(13, 6, 6, 13)
            background = ContextCompat.getDrawable(context, R.drawable.bg_classified_sound)
            layoutParams =
                LinearLayout
                    .LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = 8
                        gravity = Gravity.CENTER
                    }
        }
    // MARK: - Voice Recording Functions

    // request permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Audio permission granted :)")
                startForegroundService()
            } else {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Audio permission not granted :(")
            }
        }
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

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
        // Android 13 이상이면 POST_NOTIFICATIONS 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001,
                )
            }
        }

        // 알림 설정이 꺼져 있는지 확인하고 안내
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) {
            AlertDialog
                .Builder(this)
                .setTitle("Notifications are turned off")
                .setMessage("Please enable notifications to get sound alerts.")
                .setPositiveButton("Go to Settings") { _, _ ->
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

    companion object {
        const val REQUEST_CALL_PERMISSION = 100
        const val EMERGENCY_NUMBER = "112"
        private const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
        const val REQUEST_RECORD_AUDIO = 1337
        const val MODEL_FILE = "yamnet.tflite"
        const val MINIMUM_DISPLAY_THRESHOLD: Float = 0.3f
    }
}
