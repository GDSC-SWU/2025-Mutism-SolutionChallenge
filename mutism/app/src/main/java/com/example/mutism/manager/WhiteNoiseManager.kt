package com.example.mutism.manager

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.mutism.controller.main.MainActivity
import com.example.mutism.model.whiteNoise.WhiteNoiseSoundMap

object WhiteNoiseManager {
    private var whiteNoisePlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Start White Noise Playback
    fun playWhiteNoise(
        name: String,
        onStarted: (() -> Unit)? = null,
    ) {
        val formattedKey = name.lowercase().replace("\n", "_").replace(" ", "_")
        val resId =
            WhiteNoiseSoundMap.map[formattedKey] ?: run {
                Log.e("com.example.mutism.utils.WhiteNoiseManager", "리소스를 찾을 수 없음: $formattedKey")
                return
            }

        stopWhiteNoise()

        whiteNoisePlayer =
            MediaPlayer.create(appContext, resId)?.apply {
                isLooping = true
                setVolume(1.0f, 1.0f)
                setOnErrorListener { _, what, extra ->
                    Log.e("WhiteNoiseManager", "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
                start()
                Log.d("WhiteNoiseManager", "White noise started: $formattedKey")
            }

        onStarted?.invoke() // Callback to show stop button in UI
        notifyShowStopButton()

        handler.postDelayed(reminderRunnable, 5 * 60 * 1000L) // Trigger reminder after 5 minutes
    }

    // Stop White Noise Playback
    fun stopWhiteNoise(onStopped: (() -> Unit)? = null) {
        whiteNoisePlayer?.apply {
            stop()
            release()
        }
        whiteNoisePlayer = null
        onStopped?.invoke()
    }

    fun isPlaying(): Boolean = whiteNoisePlayer?.isPlaying == true

    // Reminder Loop Every 5 Minutes
    private val reminderRunnable =
        object : Runnable {
            override fun run() {
                if (isPlaying()) {
                    if (isAppInForeground()) {
                        Toast
                            .makeText(
                                appContext,
                                "5 minutes have passed. You can stop the white noise.",
                                Toast.LENGTH_LONG,
                            ).show()
                    } else {
                        showStopWhiteNoiseNotification()
                    }
                    handler.postDelayed(this, 5 * 60 * 1000L)
                }
            }
        }

    // Check if App is in Foreground
    private fun isAppInForeground(): Boolean {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        return appProcesses.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                it.processName == appContext.packageName
        }
    }

    // Broadcast to Show Stop Button
    private fun notifyShowStopButton() {
        val intent = Intent("com.mutism.ACTION_SHOW_STOP_WHITE_NOISE")
        appContext.sendBroadcast(intent)
    }

    // Show Notification if App is in Background
    private fun showStopWhiteNoiseNotification() {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent =
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        "white_noise_notify",
                        "White Noise Reminder",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Reminder to stop white noise after 5 minutes."
                    }
                notificationManager.createNotificationChannel(channel)
                Notification.Builder(appContext, "white_noise_notify")
            } else {
                Notification.Builder(appContext)
            }.setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setContentTitle("5 minutes have passed!")
                .setContentText("Would you like to stop the white noise? Open the app to decide.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(1004, notification)
    }
}
