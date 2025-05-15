package com.example.mutism.controller.whiteNoisePage

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mutism.R
import com.example.mutism.databinding.ActivityWhiteNoiseBinding
import com.example.mutism.model.whiteNoise.WhiteNoiseData
import com.example.mutism.model.whiteNoise.WhiteNoiseItem
import com.example.mutism.model.whiteNoise.WhiteNoiseSoundMap
import com.example.mutism.view.adapter.WhiteNoiseAdapter

class WhiteNoiseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhiteNoiseBinding
    private lateinit var adapter: WhiteNoiseAdapter
    private var whiteNoiseList = mutableListOf<WhiteNoiseItem>()
    private var mediaPlayer: MediaPlayer? = null
    private var selectedNoiseName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWhiteNoiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView() // Initialize RecyclerView and adapter
        loadWhiteNoises() // Load noise list and restore previous selection

        binding.btnSelect.setOnClickListener { onSelectClicked() }
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback() // Release media player resources
    }

    // Initializes RecyclerView with 3-column grid layout and adapter
    private fun setupRecyclerView() {
        adapter = WhiteNoiseAdapter { onNoiseItemClicked(it) }
        binding.recyclerViewWhiteNoises.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerViewWhiteNoises.adapter = adapter
    }

    // Loads the noise list, restores previously selected noise and plays it
    private fun loadWhiteNoises() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val saved = prefs.getString(KEY_SELECTED_NOISE, null)
        selectedNoiseName = saved

        whiteNoiseList =
            WhiteNoiseData.list
                .map { it.copy(isSelected = it.name == saved) }
                .toMutableList()

        adapter.submitList(whiteNoiseList.toList())

        saved?.let { playWhiteNoise(it) }

        updateSelectButtonState()
    }

    // Handles click on a noise item
    private fun onNoiseItemClicked(item: WhiteNoiseItem) {
        val name = item.name

        if (name == selectedNoiseName) {
            selectedNoiseName = null
            stopPlayback()
            updateSelection(null)
        } else {
            selectedNoiseName = name
            playWhiteNoise(name)
            updateSelection(name)
            saveSelection(name)
        }
    }

    private fun updateSelection(name: String?) {
        whiteNoiseList =
            whiteNoiseList
                .map {
                    it.copy(isSelected = it.name == name)
                }.toMutableList()

        adapter.submitList(whiteNoiseList.toList())
        updateSelectButtonState()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateSelectButtonState() {
        val hasSelection = whiteNoiseList.any { it.isSelected }

        binding.btnSelect.apply {
            isEnabled = hasSelection
            background =
                getDrawable(
                    if (hasSelection) R.drawable.btn_select_noise_select else R.drawable.btn_select_noise_unselect,
                )
        }
    }

    // Starts playback of the selected white noise sound
    private fun playWhiteNoise(name: String) {
        val resId = WhiteNoiseSoundMap.map[name]
        if (resId == null) {
            Log.e("WhiteNoise", "No resource found for name: $name")
            return
        }
        stopPlayback()

        mediaPlayer =
            MediaPlayer.create(this, resId).apply {
                isLooping = true
                start()
            }
    }

    private fun stopPlayback() {
        mediaPlayer?.run {
            stop()
            release()
        }
        mediaPlayer = null
    }

    // Saves the selected white noise name into SharedPreferences
    private fun saveSelection(name: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putString(KEY_SELECTED_NOISE, name)
        }
    }

    private fun onSelectClicked() {
        selectedNoiseName?.let {
            saveSelection(it)
            Toast.makeText(this, "Selected white noise: $it", Toast.LENGTH_SHORT).show()
            finish()
        } ?: Toast.makeText(this, "No noise selected.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PREFS_NAME = "WhiteNoisePrefs"
        private const val KEY_SELECTED_NOISE = "selected_white_noise"
    }
}
