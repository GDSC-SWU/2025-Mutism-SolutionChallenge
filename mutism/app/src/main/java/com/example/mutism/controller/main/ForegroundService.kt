package com.example.mutism.controller.main

import RolePromptGenerator
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mutism.controller.myPage.MyPageActivity.Companion.KEY_RELAX_METHOD
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class ForegroundService : Service() {
    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "ForegroundServiceChannel"
        private const val REFERENCE = 0.00002
    }

    private var audioClassifier: AudioClassifier? = null
    private var handler: Handler? = null
    private val classificationInterval = 500L
    private var lastLabel: String? = null

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
                    CHANNEL_ID,
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
                    .Builder(this, CHANNEL_ID)
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

            val classifyRunnable =
                object : Runnable {
                    override fun run() {
                        val audioTensor = classifier.createInputTensorAudio()
                        audioTensor.load(record)
                        val output = classifier.classify(audioTensor)

                        // Filter categories
                        val filtered =
                            output[0]
                                .categories
                                .filter { it.score > MainActivity.MINIMUM_DISPLAY_THRESHOLD }
                                .sortedByDescending { it.score }

                        // Log formatted output
                        val topCategory = filtered.firstOrNull()
                        topCategory?.let { category ->
                            Log.d("ForegroundService", "category: %s (%.2f)".format(category.label, category.score))

                            // Check if the stored noise tags include the current category label
                            if (selectedTags?.contains(category.label) == true) {
                                // Check if the category has changed or if enough time has passed
                                val currentTime = System.currentTimeMillis()
                                // Check if the detected sound label is different from the last one and if enough time has passed since the last Gemini API call
                                if (category.label != lastCategoryLabel && currentTime - lastCategoryTimestamp > geminiCallIntervalMillis) {
                                    currentNoise = category.label
                                    val prompt = promptGenerator.generatePrompt(name, releasedMethod, currentNoise, sensitiveNoise)
                                    Log.d("callGeminiAPI", "prompt: $prompt")
                                    callGeminiAPI(prompt)

                                    // Update the last called category and timestamp
                                    lastCategoryLabel = category.label
                                    lastCategoryTimestamp = currentTime
                                }
                            }

                            // ✅ Only send when the result differs from the previous one
                            if (category.label != lastLabel) {
                                sendToMainActivity(category.label)
                                lastLabel = category.label
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
        val apiKey = "AIzaSyCGLlDF6GSZ_3Q5QWpT2R7HWSGBti6BlDk"
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
}
