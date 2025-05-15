package com.example.mutism.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioRecord
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mutism.BuildConfig
import com.example.mutism.controller.main.MainActivity
import com.example.mutism.controller.myPage.MyPageActivity
import com.example.mutism.manager.RolePromptManager
import com.example.mutism.manager.TTSManager
import com.example.mutism.manager.WhiteNoiseManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class ForegroundService : Service() {
    private var audioClassifier: AudioClassifier? = null
    private var handler: Handler? = null

    private var lastNotifiedNoise: String? = null
    private var lastNotifyTime: Long = 0L
    private var sameLabelRepeatCount = 0

    private var lastSound: String? = null
    private var lastNoiseTimestamp: Long = 0L
    private var lastNoise: String? = null
    private var noiseCount = 0

    private var promptGenerator: RolePromptManager = RolePromptManager()
    private var ttsManager = TTSManager()
    private lateinit var selectedTagsLower: List<String>

    // user info
    private var name: String? = null
    private var releasedMethod: String? = null
    private var sensitiveNoise: List<String>? = null
    private var currentNoise: String? = null
    private var selectedWhiteNoise: String? = null

    // region: Lifecycle
    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate() {
        super.onCreate()

        WhiteNoiseManager.init(applicationContext)
        createNotificationChannel()
        startForeground(1, createNotification())
        loadSharedPreferences()
        ttsManager.initTTS(this, preferredGender = getGender())

        startAudioClassification()
    }

    override fun onDestroy() {
        stopAudioClassification()
        super.onDestroy()
        ttsManager.shutdown()
        sendForegroundStopMainActivity()
    }
    // endregion

    // region: Init Helpers
    private fun loadSharedPreferences() {
        val noiseSelectPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
        val whiteNoisePrefs = getSharedPreferences("WhiteNoisePrefs", MODE_PRIVATE)
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val selectedTags =
            noiseSelectPrefs.getStringSet(MainActivity.KEY_SELECTED_NOISE_TAGS, emptySet())
                ?: emptySet()

        selectedTagsLower = selectedTags.map { it.lowercase() }
        name = userPrefs.getString(MyPageActivity.KEY_NAME, "") ?: ""
        releasedMethod = userPrefs.getString(MyPageActivity.KEY_RELAX_METHOD, "") ?: ""
        sensitiveNoise = selectedTags.toList()
        selectedWhiteNoise = whiteNoisePrefs.getString("selected_white_noise", "") ?: ""
    }

    private fun getGender(): String {
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return userPrefs.getString(MyPageActivity.KEY_GENDER, "Female") ?: "Female"
    }
    // endregion

    // region: Notification
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification
                .Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Audio Classification Service")
                .setContentText("Running...")
                .build()
        } else {
            Notification
                .Builder(this)
                .setContentTitle("Audio Classification Service")
                .setContentText("Running...")
                .build()
        }
    // endregion

    // region: Audio Classification
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioClassification() {
        try {
            val classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
            val record = classifier.createAudioRecord()
            record.startRecording()

            val handlerThread = HandlerThread("ServiceHandlerThread").apply { start() }
            handler = Handler(handlerThread.looper)
            audioClassifier = classifier

            val classifyRunnable =
                object : Runnable {
                    override fun run() {
                        classifyAudio(classifier, record)
                        handler?.postDelayed(this, CLASSIFICATIONINTERVAL)
                    }
                }
            handler?.post(classifyRunnable)
        } catch (e: Exception) {
            Log.e("ForegroundService", "Error in audio classification", e)
            stopSelf()
        }
    }

    private fun stopAudioClassification() {
        handler?.removeCallbacksAndMessages(null)
        audioClassifier = null
        handler?.looper?.quit()
        handler = null
    }

    private fun classifyAudio(
        classifier: AudioClassifier,
        record: AudioRecord,
    ) {
        val audioTensor = classifier.createInputTensorAudio()
        audioTensor.load(record)
        val output = classifier.classify(audioTensor)

        val topCategory =
            output[0]
                .categories
                .filter { it.score > MINIMUM_DISPLAY_THRESHOLD }
                .sortedByDescending { it.score }
                .firstOrNull() ?: return

        Log.d("ForegroundService", "category: ${topCategory.label} (${topCategory.score})")

        handleSoundDetection(topCategory.label)
    }

    private fun handleSoundDetection(category: String) {
        val sound = category.lowercase()

        if (selectedTagsLower.contains(sound)) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastCall = currentTime - lastNoiseTimestamp
            val isSpeaking = ttsManager.isSpeaking()
            val isWhiteNoisePlaying = WhiteNoiseManager.isPlaying()

            val shouldCallGemini =
                (
                    timeSinceLastCall >= GEMINICALLINTERVAL ||
                        (timeSinceLastCall >= 60_000 && sound != lastNoise)
                ) &&
                    !isSpeaking &&
                    !isWhiteNoisePlaying

            if (shouldCallGemini) {
                currentNoise = sound
                val prompt =
                    promptGenerator.generatePrompt(
                        name,
                        releasedMethod,
                        currentNoise,
                        sensitiveNoise,
                    )
                callGeminiAPI(prompt) {
                    if (!ttsManager.isSpeaking() && !WhiteNoiseManager.isPlaying()) {
                        selectedWhiteNoise?.let {
                            ttsManager.speak(
                                "I'll play you some white noise of $it",
                            ) {
                                WhiteNoiseManager.playWhiteNoise(it)
                            }
                        }
                            ?: ttsManager.speak("No white noise selected. Please set one in your settings.")
                    }
                }

                if (currentNoise == lastNoise) noiseCount++ else noiseCount = 1
                if (noiseCount == 10) sendEmergencyToMainActivity()

                lastNoise = sound
                lastNoiseTimestamp = currentTime
            }
        }

        if (sound != lastSound && sound != "silence") {
            sendClassifiedResultToMainActivity(category)
            lastSound = sound
        }

        val shouldNotify =
            if (sound == lastNotifiedNoise) {
                sameLabelRepeatCount++
                sameLabelRepeatCount % 10 == 0
            } else {
                sameLabelRepeatCount = 0
                true
            }

        if (selectedTagsLower.contains(sound) && shouldNotify) {
            showSoundDetectedNotification(sound)
            lastNotifiedNoise = sound
            lastNotifyTime = System.currentTimeMillis()
        }
    }
    // endregion

    // Gemini API
    private fun callGeminiAPI(
        prompt: String,
        onComplete: (() -> Unit)? = null,
    ) {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$API_KEY"
        val bodyJson = """{"contents":[{"parts":[{"text":"$prompt"}]}]}"""

        val client = OkHttpClient()
        val requestBody = bodyJson.toRequestBody("application/json".toMediaTypeOrNull())

        val request =
            Request
                .Builder()
                .url(url)
                .post(requestBody)
                .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val text =
                            JSONObject(responseBody)
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                        ttsManager.speak(text) {
                            onComplete?.invoke()
                        }
                    } else {
                        Log.e("GeminiAPI", "Unsuccessful response: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAPI", "Error calling Gemini API", e)
            }
        }.start()
    }

    // Broadcasts & Notifications
    private fun sendClassifiedResultToMainActivity(newText: String) {
        val classifiedResultIntent = Intent("com.mutism.UPDATE_CURRENT_SOUND_LIST")
        classifiedResultIntent.putExtra("classifiedSound", newText)
        sendBroadcast(classifiedResultIntent)
    }

    private fun sendEmergencyToMainActivity() {
        val emergencyIntent = Intent("com.mutism.ACTION_EMERGENCY_CALL")
        sendBroadcast(emergencyIntent)
    }

    private fun sendForegroundStopMainActivity() {
        val foregroundIntent = Intent("com.mutism.FOREGROUND_STOP")
        sendBroadcast(foregroundIntent)
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
        val notification =
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
                Notification.Builder(this, SOUND_DETECTED_CHANNEL_ID)
            } else {
                Notification.Builder(this)
            }.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Sensitive Sound Detected")
                .setContentText("Detected: $detectedLabel")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(1002, notification)
    }

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "ForegroundServiceChannel"
        private const val SOUND_DETECTED_CHANNEL_ID = "sound_detected_channel"
        private const val API_KEY = BuildConfig.GEMINI_API_KEY
        private const val MODEL_FILE = "yamnet.tflite"
        private const val MINIMUM_DISPLAY_THRESHOLD = 0.3f
        private const val CLASSIFICATIONINTERVAL = 500L
        private const val GEMINICALLINTERVAL = 60 * 1000
    }
}
