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

    private var lastDetectedSound: String? = null
    private var lastNotificationTime: Long = 0L
    private var repeatCount = 0

    private var previousSound: String? = null
    private var lastSensitiveSoundTime: Long = 0L
    private var lastSensitiveSound: String? = null
    private var sensitiveSoundCount = 0

    private val promptManager = RolePromptManager()
    private val ttsManager = TTSManager()
    private lateinit var sensitiveSoundList: List<String>

    // User info
    private var userName: String? = null
    private var relaxMethod: String? = null
    private var selectedWhiteNoise: String? = null
    private var currentSensitiveSound: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate() {
        super.onCreate()
        WhiteNoiseManager.init(applicationContext)
        createNotificationChannels()
        startForeground(1, buildForegroundNotification())
        loadPreferences()
        ttsManager.initTTS(this, preferredGender = getUserGender())
        startAudioClassification()
    }

    override fun onDestroy() {
        stopAudioClassification()
        ttsManager.shutdown()
        sendBroadcast(Intent("com.mutism.FOREGROUND_STOP"))
        super.onDestroy()
    }

    private fun loadPreferences() {
        val noisePrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
        val whiteNoisePrefs = getSharedPreferences("WhiteNoisePrefs", MODE_PRIVATE)
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        val selectedTags = noisePrefs.getStringSet(MainActivity.KEY_SELECTED_NOISE_TAGS, emptySet()) ?: emptySet()
        sensitiveSoundList = selectedTags.map { it.lowercase() }

        userName = userPrefs.getString(MyPageActivity.KEY_NAME, "")
        relaxMethod = userPrefs.getString(MyPageActivity.KEY_RELAX_METHOD, "")
        selectedWhiteNoise = whiteNoisePrefs.getString("selected_white_noise", "")
    }

    private fun getUserGender(): String {
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return userPrefs.getString(MyPageActivity.KEY_GENDER, "Female") ?: "Female"
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val foregroundChannel =
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Foreground Audio Service",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Used by ForegroundService to stay alive" }
            manager.createNotificationChannel(foregroundChannel)

            val soundChannel =
                NotificationChannel(
                    SOUND_DETECTED_CHANNEL_ID,
                    "Sound Detected Notification",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts when selected sounds are detected"
                    enableVibration(true)
                    enableLights(true)
                }
            manager.createNotificationChannel(soundChannel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, FOREGROUND_CHANNEL_ID)
            } else {
                Notification.Builder(this)
            }
        return builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Audio Classification Service")
            .setContentText("Running...")
            .build()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioClassification() {
        try {
            val classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
            val record = classifier.createAudioRecord()
            record.startRecording()

            val thread = HandlerThread("ServiceHandlerThread").apply { start() }
            handler = Handler(thread.looper)
            audioClassifier = classifier

            handler?.post(
                object : Runnable {
                    override fun run() {
                        classifySound(classifier, record)
                        handler?.postDelayed(this, CLASSIFICATION_INTERVAL)
                    }
                },
            )
        } catch (e: Exception) {
            Log.e("ForegroundService", "Audio classification error", e)
            stopSelf()
        }
    }

    private fun stopAudioClassification() {
        handler?.removeCallbacksAndMessages(null)
        audioClassifier = null
        handler?.looper?.quit()
        handler = null
    }

    private fun classifySound(
        classifier: AudioClassifier,
        record: AudioRecord,
    ) {
        val audioTensor = classifier.createInputTensorAudio()
        audioTensor.load(record)
        val result = classifier.classify(audioTensor)

        val topCategory =
            result[0]
                .categories
                .filter { it.score > MIN_DISPLAY_THRESHOLD }
                .maxByOrNull { it.score } ?: return

        Log.d("ForegroundService", "Detected: ${topCategory.label} (${topCategory.score})")
        processDetectedSound(topCategory.label)
    }

    private fun processDetectedSound(label: String) {
        val sound = label.lowercase()
        val now = System.currentTimeMillis()

        if (sensitiveSoundList.contains(sound)) {
            val timeSinceLast = now - lastSensitiveSoundTime
            val isSpeaking = ttsManager.isSpeaking()
            val isWhiteNoisePlaying = WhiteNoiseManager.isPlaying()

            val shouldCallGemini =
                (timeSinceLast >= GEMINI_CALL_INTERVAL || (timeSinceLast >= 60000 && sound != lastSensitiveSound)) &&
                    !isSpeaking &&
                    !isWhiteNoisePlaying

            if (shouldCallGemini) {
                currentSensitiveSound = sound
                val prompt = promptManager.generatePrompt(userName, relaxMethod, currentSensitiveSound, sensitiveSoundList)
                callGemini(prompt) {
                    if (!selectedWhiteNoise.isNullOrBlank()) {
                        ttsManager.speak("I'll play you some white noise of $selectedWhiteNoise") {
                            if (!WhiteNoiseManager.playWhiteNoise(selectedWhiteNoise!!)) {
                                Log.e("TTS→WhiteNoise", "Playback failed: $selectedWhiteNoise")
                            }
                        }
                    } else {
                        ttsManager.speak("No white noise selected. Please set one in your settings.")
                    }
                }

                sensitiveSoundCount = if (sound == lastSensitiveSound) sensitiveSoundCount + 1 else 1
                if (sensitiveSoundCount == 10) sendBroadcast(Intent("com.mutism.ACTION_EMERGENCY_CALL"))

                lastSensitiveSound = sound
                lastSensitiveSoundTime = now
            }
        }

        if (sound != previousSound && sound != "silence") {
            val intent = Intent("com.mutism.UPDATE_CURRENT_SOUND_LIST")
            intent.putExtra("classifiedSound", label)
            sendBroadcast(intent)
            previousSound = sound
        }

        // Notify every 10 seconds for same sound
        val shouldNotify = (now - lastNotificationTime >= 10000)
        if (sensitiveSoundList.contains(sound) && shouldNotify) {
            showNotification(sound)
            lastDetectedSound = sound
            lastNotificationTime = now
        }
    }

    private fun callGemini(
        prompt: String,
        onComplete: (() -> Unit)? = null,
    ) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$API_KEY"
        val json = """{"contents":[{"parts":[{"text":"$prompt"}]}]}"""

        val client = OkHttpClient()
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
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
                        val text =
                            JSONObject(response.body?.string())
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                        ttsManager.speak(text) { onComplete?.invoke() }
                    } else {
                        Log.e("GeminiAPI", "Response error: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAPI", "Request failed", e)
            }
        }.start()
    }

    private fun showNotification(label: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, SOUND_DETECTED_CHANNEL_ID)
            } else {
                Notification.Builder(this)
            }

        val notification =
            builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Sensitive Sound Detected")
                .setContentText("Detected: $label")
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(false)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build()

        manager.notify(1002, notification)
    }

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "ForegroundServiceChannel"
        private const val SOUND_DETECTED_CHANNEL_ID = "SoundDetectedChannel"
        private const val API_KEY = BuildConfig.GEMINI_API_KEY
        private const val MODEL_FILE = "yamnet.tflite"
        private const val MIN_DISPLAY_THRESHOLD = 0.3f
        private const val CLASSIFICATION_INTERVAL = 500L
        private const val GEMINI_CALL_INTERVAL = 60000L
    }
}
