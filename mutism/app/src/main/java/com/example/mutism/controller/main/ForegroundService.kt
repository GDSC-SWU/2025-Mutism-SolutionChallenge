package com.example.mutism.controller.main

import RolePromptGenerator
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mutism.BuildConfig
import com.example.mutism.controller.myPage.MyPageActivity.Companion.KEY_RELAX_METHOD
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class ForegroundService : Service() {

    private var audioClassifier: AudioClassifier? = null
    private var handler: Handler? = null
    private val classificationInterval = 500L
    private var lastLabel: String? = null

    private var lastNotifiedLabel: String? = null
    private var lastNotifyTime: Long = 0L
    private val notifyCooldownMs = 10_000L // 10초 간 중복 알림 금지
  
    // user info
    var promptGenerator: RolePromptGenerator = RolePromptGenerator()
    private lateinit var selectedTags: Set<String>
  
    var name: String? = null
    var releasedMethod: String? = null
    var sensitiveNoise: List<String>? = null
    var currentNoise: String? = null

    // Track the last time Gemini API was called
    private var lastCategoryTimestamp: Long = 0L
    private var lastCategoryLabel: String? = null
    private val geminiCallIntervalMillis: Long = 60 * 1000

    val apiKey = BuildConfig.GEMINI_API_KEY


    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)
        startAudioClassification()

        val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
        selectedTags = sharedPrefs.getStringSet(MainActivity.KEY_SELECTED_NOISE_TAGS, emptySet())!!
//        name = sharedPrefs.getString(KEY_NAME, "") ?: ""
        name = "효진"
        releasedMethod = sharedPrefs.getString(KEY_RELAX_METHOD, "") ?: ""
        sensitiveNoise = selectedTags.toList()
    }

    override fun onDestroy() {
        stopAudioClassification()
        isRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel =
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder =
                Notification
                    .Builder(this, FOREGROUND_CHANNEL_ID)
                    .setContentTitle("Audio Classification Service")
                    .setContentText("Running...")
            builder.build()
        } else {
            Notification
                .Builder(this)
                .setContentTitle("Audio Classification Service")
                .setContentText("Running...")
                .build()
        }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioClassification() {
        try {
            val classifier = AudioClassifier.createFromFile(this, MainActivity.MODEL_FILE)
            val record = classifier.createAudioRecord()
            record.startRecording()

            val handlerThread = HandlerThread("ServiceHandlerThread")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            audioClassifier = classifier

            val classifyRunnable = object : Runnable {
    override fun run() {
        val audioTensor = classifier.createInputTensorAudio()
        audioTensor.load(record)
        val output = classifier.classify(audioTensor)

        val filtered = output[0].categories
            .filter { it.score > MainActivity.MINIMUM_DISPLAY_THRESHOLD }
            .sortedByDescending { it.score }

        val topCategory = filtered.firstOrNull()
        topCategory?.let { category ->
            val label = category.label.lowercase()
            Log.d("ForegroundService", "category: ${category.label} (${category.score})")

            // ✅ Trigger Gemini API if the detected label is among user-selected tags
            if (selectedTags.contains(category.label)) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCall = currentTime - lastCategoryTimestamp

                val shouldCallGemini =
                    timeSinceLastCall >= geminiCallIntervalMillis ||
                    (timeSinceLastCall >= 60_000 && category.label != lastCategoryLabel)

                if (shouldCallGemini) {
                    currentNoise = category.label
                    val prompt = promptGenerator.generatePrompt(name, releasedMethod, currentNoise, sensitiveNoise)
                    Log.d("callGeminiAPI", "prompt: $prompt")
                    callGeminiAPI(prompt)

                    lastCategoryLabel = category.label
                    lastCategoryTimestamp = currentTime
                }
            }

            // ✅ Only send to MainActivity when the label changes
            if (label != lastLabel) {
                sendToMainActivity(category.label)
                lastLabel = label
            }

            // ✅ Show a notification if the detected label is in the selected tags
            val prefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
            val selectedTagsLower = prefs.getStringSet("selected_noise_tags", emptySet())?.map { it.lowercase() } ?: emptyList()
            val currentTime = System.currentTimeMillis()

            val shouldNotify = selectedTagsLower.contains(label) &&
                    (label != lastNotifiedLabel || (currentTime - lastNotifyTime > notifyCooldownMs))

            if (shouldNotify) {
                showSoundDetectedNotification(label)
                lastNotifiedLabel = label
                lastNotifyTime = currentTime
            }
        }

        handler?.postDelayed(this, classificationInterval)
    }
}

            handler?.post(classifyRunnable)
        } catch (e: Exception) {
            Log.e("ForegroundService", "Error in audio classification", e)
            stopSelf()
        }
    }

    private fun callGeminiAPI(prompt: String) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val requestBodyJson =
            """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "$prompt"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val client = okhttp3.OkHttpClient()
        val requestBody =
            okhttp3.RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                requestBodyJson,
            )

        val request =
            okhttp3.Request
                .Builder()
                .url(url)
                .post(requestBody)
                .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GeminiAPI", "Unsuccessful response: ${response.code}")
                    } else {
                        val responseBody = response.body?.string()
                        Log.d("GeminiAPI", "Response: $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAPI", "Error calling Gemini API", e)
            }
        }.start()
    }

    fun sendToMainActivity(newText: String) {
        val intent = Intent("com.mutism.UPDATE_LIST")
        intent.putExtra("new_text", newText)
        sendBroadcast(intent)
    }

    private fun stopAudioClassification() {
        handler?.removeCallbacksAndMessages(null)
        audioClassifier = null
        handler?.looper?.quit()
        handler = null
    }

    @Suppress("DEPRECATION")
    private fun showSoundDetectedNotification(detectedLabel: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification: Notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        SOUND_DETECTED_CHANNEL_ID,
                        "Sound Detected Notification",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Alerts when selected sounds are detected"
                    }
                notificationManager.createNotificationChannel(channel)

                Notification
                    .Builder(this, SOUND_DETECTED_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Sensitive Sound Detected")
                    .setContentText("Detected: $detectedLabel")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
            } else {
                Notification
                    .Builder(this)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Sensitive Sound Detected")
                    .setContentText("Detected: $detectedLabel")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
            }

        notificationManager.notify(1002, notification)
    }

    companion object {
        var isRunning = false
        private const val FOREGROUND_CHANNEL_ID = "ForegroundServiceChannel"
        private const val SOUND_DETECTED_CHANNEL_ID = "sound_detected_channel"
        private const val REFERENCE = 0.00002
    }
}
