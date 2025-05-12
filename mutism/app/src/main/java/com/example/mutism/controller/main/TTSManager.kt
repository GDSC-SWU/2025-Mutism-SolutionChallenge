package com.example.mutism.controller.main

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager {
    private var tts: TextToSpeech? = null

    fun initTTS(
        context: Context,
        onReady: (() -> Unit)? = null,
        preferredGender: String = "Female",
    ) {
        tts =
            TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.ENGLISH)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTSManager", "Language not supported.")
                    } else {
                        val voices = tts?.voices
                        val selectedVoice =
                            voices?.firstOrNull { voice ->
                                voice.locale == Locale.ENGLISH &&
                                    when (preferredGender) {
                                        "Female" -> voice.name.contains("female", ignoreCase = true)
                                        "Male" -> voice.name.contains("male", ignoreCase = true)
                                        else -> false
                                    }
                            }

                        if (selectedVoice != null) {
                            tts?.voice = selectedVoice
                            Log.d("TTSManager", "Selected voice: ${selectedVoice.name}")
                        } else {
                            Log.w("TTSManager", "No matching voice found for $preferredGender")
                        }

                        onReady?.invoke()
                    }
                } else {
                    Log.e("TTSManager", "Initialization failed.")
                }
            }
    }

    fun speak(
        text: String,
        onDone: (() -> Unit)? = null,
    ) {
        tts?.setOnUtteranceProgressListener(
            object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    onDone?.let {
                        Handler(Looper.getMainLooper()).post {
                            it()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            },
        )
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
