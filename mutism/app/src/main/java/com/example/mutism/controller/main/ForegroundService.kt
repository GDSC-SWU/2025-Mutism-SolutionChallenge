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
    private var lastLabel: String? = null

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
                            Log.d("ForegroundService", "db: %.2f, category: %s (%.2f)".format(decibel, category.label, category.score))

                            // ✅ 이전 결과와 다를 경우에만 보내기
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
        Log.d("foregroundService", newText)
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
