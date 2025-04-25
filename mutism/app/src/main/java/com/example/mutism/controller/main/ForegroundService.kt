package com.example.mutism.controller.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null
    private var handler: Handler? = null
    private val classificationInterval = 500L

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

                        // Process results
                        val filtered =
                            output[0]
                                .categories
                                .filter {
                                    it.score > MainActivity.MINIMUM_DISPLAY_THRESHOLD
                                }.sortedBy { -it.score }

                        for (category in filtered) {
                            Log.d("ForegroundService", "${category.label} : ${category.score}")
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

    private fun stopAudioClassification() {
        handler?.removeCallbacksAndMessages(null)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        audioClassifier = null
        handler?.looper?.quit()
        handler = null
    }
}
