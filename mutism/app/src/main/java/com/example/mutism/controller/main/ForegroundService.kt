package com.example.mutism.controller.main

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
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class ForegroundService : Service() {
    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null
    private var handler: Handler? = null
    private val classificationInterval = 500L
    private var lastLabel: String? = null

    private var lastNotifiedLabel: String? = null
    private var lastNotifyTime: Long = 0L
    private val notifyCooldownMs = 10_000L // 10초 간 중복 알림 금지

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)
        startAudioClassification()
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

    private fun startAudioClassification() {
        try {
            val classifier = AudioClassifier.createFromFile(this, MainActivity.MODEL_FILE)
            val record = classifier.createAudioRecord()
            record.startRecording()

            val handlerThread = HandlerThread("ServiceHandlerThread")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            audioClassifier = classifier
            audioRecord = record

            val classifyRunnable =
                object : Runnable {
                    override fun run() {
                        val audioTensor = classifier.createInputTensorAudio()
                        audioTensor.load(record)
                        val output = classifier.classify(audioTensor)

                        // Measure decibel
                        val bufferSize =
                            AudioRecord.getMinBufferSize(
                                audioRecord!!.sampleRate,
                                audioRecord!!.channelConfiguration,
                                audioRecord!!.audioFormat,
                            )
                        val buffer = ShortArray(bufferSize)
                        val readSize = record.read(buffer, 0, buffer.size)
                        val decibel = calculateDecibel(buffer, readSize)

                        // Filter categories
                        val filtered =
                            output[0]
                                .categories
                                .filter { it.score > MainActivity.MINIMUM_DISPLAY_THRESHOLD }
                                .sortedByDescending { it.score }

                        // Log formatted output
                        val topCategory = filtered.firstOrNull()
                        topCategory?.let { category ->
                            val label = category.label.lowercase()

                            Log.d("ForegroundService", "db: %.2f, category: %s (%.2f)".format(decibel, category.label, category.score))

                            // 이전 결과와 다를 경우에만 보내기
                            if (label != lastLabel) {
                                sendToMainActivity(category.label)
                                lastLabel = category.label
                            }

                            // 알림 조건 확인: 사용자 설정된 소음에 포함된 경우
                            val prefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
                            val selectedTags = prefs.getStringSet("selected_noise_tags", emptySet())
                            val currentTime = System.currentTimeMillis()

                            if (selectedTags?.map { it.lowercase() }?.contains(label) == true) {
                                val shouldNotify = label != lastNotifiedLabel || (currentTime - lastNotifyTime > notifyCooldownMs)
                                if (shouldNotify) {
                                    showSoundDetectedNotification(label)
                                    lastNotifiedLabel = label
                                    lastNotifyTime = currentTime
                                }
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

    private fun calculateDecibel(
        buffer: ShortArray,
        readSize: Int,
    ): Double {
        if (readSize == 0) return -100.0 // Handle case where there is no data

        var sum: Long = 0
        for (i in 0 until readSize) {
            sum += buffer[i] * buffer[i]
        }

        val rms = Math.sqrt(sum.toDouble() / readSize)
        return if (rms > 0) 20 * Math.log10(rms) else -100.0 // If rms is 0, return -100
    }

    fun sendToMainActivity(newText: String) {
        val intent = Intent("com.mutism.UPDATE_LIST")
        intent.putExtra("new_text", newText)
        sendBroadcast(intent)
    }

    private fun stopAudioClassification() {
        handler?.removeCallbacksAndMessages(null)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
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
    }
}
