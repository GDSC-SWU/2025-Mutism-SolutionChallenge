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
import com.example.mutism.controller.dialog.NoUserInfoDialog
import com.example.mutism.controller.dialog.SelectNoiseDialog
import com.example.mutism.controller.myPage.MyPageActivity
import com.example.mutism.controller.noiseSelectPage.NoiseSelectActivity
import com.example.mutism.databinding.ActivityMainBinding
import com.example.mutism.manager.WhiteNoiseManager
import com.example.mutism.service.ForegroundService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isRecording = false
    private var selectNoiseDialog: SelectNoiseDialog? = null
    private val noiseSelectLauncher = registerNoiseSelectLauncher()
    private lateinit var currentSoundListContainer: LinearLayout

    // Receiver for broadcast actions
    private val broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    "com.mutism.UPDATE_CURRENT_SOUND_LIST" -> {
                        val newText = intent.getStringExtra("classifiedSound") ?: return
                        runOnUiThread { addTextItem(newText) }
                    }

                    "com.mutism.ACTION_EMERGENCY_CALL" -> makeEmergencyCall()

                    "com.mutism.ACTION_SHOW_STOP_WHITE_NOISE" ->
                        runOnUiThread {
                            binding.btnStopWhiteNoiseContainer.visibility = View.VISIBLE
                        }

                    "com.mutism.FOREGROUND_STOP" -> runOnUiThread { clearTextItems() }
                }
            }
        }

    @SuppressLint("ImplicitSamInstance")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentSoundListContainer = binding.listContainer

        // Initial UI setup
        binding.tvRecording.visibility = View.GONE
        binding.btnStopWhiteNoiseContainer.visibility = View.GONE

        // Start/Stop button
        binding.btnStart.setOnClickListener {
            val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
            val selectedNoiseTags = sharedPrefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet())

            if (!checkUserInfoFilled()) {
                NoUserInfoDialog(this)
                    .show()
                return@setOnClickListener
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
                        requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            REQUEST_RECORD_AUDIO,
                        )
                    }
                } else {
                    stopService(Intent(this, ForegroundService::class.java))
                    Toast.makeText(this, "Stop recording", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // My Page navigation
        binding.btnMyPage.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        // Emergency SOS button
        binding.btnSos.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                makeEmergencyCall()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    REQUEST_CALL_PERMISSION,
                )
            }
        }

        // Stop white noise
        binding.btnStopWhiteNoise.setOnClickListener {
            WhiteNoiseManager.stopWhiteNoise {
                binding.btnStopWhiteNoiseContainer.visibility = View.GONE
            }
        }

        checkNotificationPermissionAndStatus()

        // Apply system insets
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
                addAction("com.mutism.UPDATE_CURRENT_SOUND_LIST")
                addAction("com.mutism.ACTION_EMERGENCY_CALL")
                addAction("com.mutism.ACTION_SHOW_STOP_WHITE_NOISE")
                addAction("com.mutism.FOREGROUND_STOP")
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()

        if (WhiteNoiseManager.isPlaying()) {
            binding.btnStopWhiteNoiseContainer.visibility = View.VISIBLE
        } else {
            binding.btnStopWhiteNoiseContainer.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiver)
    }

    // Convert dp to pixels
    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    // Update UI depending on recording state
    private fun updateRecordingUI() {
        val rootLayout = findViewById<View>(R.id.main)
        val layoutParams = binding.btnStart.layoutParams

        if (isRecording) {
            binding.btnStart.setImageResource(R.drawable.btn_main_stop)
            layoutParams.width = 214.dpToPx()
            layoutParams.height = 214.dpToPx()
            rootLayout.setBackgroundResource(R.drawable.bg_main_not_recording)
            binding.tvWelcome.visibility = View.GONE
            binding.tvRecording.visibility = View.VISIBLE
            binding.tvNowHear.visibility = View.VISIBLE
            binding.mainTvExplain.text = getString(R.string.text_main_stop)
            binding.listContainer.visibility = View.VISIBLE
        } else {
            binding.btnStart.setImageResource(R.drawable.btn_main_start)
            layoutParams.width = 264.dpToPx()
            layoutParams.height = 264.dpToPx()
            rootLayout.setBackgroundResource(R.drawable.bg_main_recording)
            binding.tvWelcome.visibility = View.VISIBLE
            binding.tvRecording.visibility = View.GONE
            binding.tvNowHear.visibility = View.GONE
            binding.mainTvExplain.text = getString(R.string.text_main_start)
            binding.listContainer.visibility = View.GONE
        }
    }

    // Make an emergency call
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

    // Add a new text item to currentSoundListContainer
    private fun addTextItem(classifiedSound: String) {
        if (currentSoundListContainer.childCount < 3) {
            currentSoundListContainer.addView(createTextView(classifiedSound))
        } else {
            currentSoundListContainer.removeAllViews()
            currentSoundListContainer.addView(createTextView(classifiedSound))
        }
    }

    // Clear all items from currentSoundListContainer
    private fun clearTextItems() {
        currentSoundListContainer.removeAllViews()
    }

    // Create a styled TextView
    private fun createTextView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.color_noise_bg))
            textSize = 24f
            setPadding(20, 6, 20, 6)
            background = ContextCompat.getDrawable(context, R.drawable.bg_main_current_sound)
            layoutParams =
                LinearLayout
                    .LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = 12
                        gravity = Gravity.CENTER
                    }
        }

    // Handle permission result
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
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    // Start the recording foreground service
    private fun startForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // Register result launcher for noise selection activity
    private fun registerNoiseSelectLauncher() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Noise selection is complete.", Toast.LENGTH_SHORT).show()
            }
        }

    // Check notification permissions and show dialog if disabled
    private fun checkNotificationPermissionAndStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001,
                )
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

    // Check whether required user info is filled
    private fun checkUserInfoFilled(): Boolean {
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val requiredKeys =
            listOf("name", "autism_level", "gender", "emergency contact", "relax_method")
        return requiredKeys.all { key ->
            val value = userPrefs.getString(key, null)
            !value.isNullOrBlank()
        }
    }

    companion object {
        private const val REQUEST_CALL_PERMISSION = 100
        private const val EMERGENCY_NUMBER = "1234"
        private const val REQUEST_RECORD_AUDIO = 1337
        private const val KEY_EMERGENCY_CONTACT = "emergency contact"
        const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
    }
}
