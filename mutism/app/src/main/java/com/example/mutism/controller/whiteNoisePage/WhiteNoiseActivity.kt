package com.example.mutism.controller.whiteNoisePage

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mutism.R
import com.example.mutism.databinding.ActivityWhiteNoiseBinding
import com.example.mutism.model.WhiteNoiseData
import com.example.mutism.model.WhiteNoiseItem
import com.example.mutism.model.WhiteNoiseSoundMap

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

        initRecyclerView()
        loadNoises()

        binding.btnSelect.setOnClickListener {
            selectedNoiseName?.let {
                saveSelection(it)
                Toast.makeText(this, "Selected: $it", Toast.LENGTH_SHORT).show()
                finish()
            } ?: Toast.makeText(this, "No noise selected.", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    private fun initRecyclerView() {
        adapter =
            WhiteNoiseAdapter { selectedItem ->
                val newSelectedName = selectedItem.name

                if (newSelectedName == selectedNoiseName) {
                    selectedNoiseName = null
                    stopPlayback()
                    updateSelection(null)
                } else {
                    selectedNoiseName = newSelectedName
                    updateSelection(selectedNoiseName)
                    playWhiteNoise(newSelectedName)
                    saveSelection(newSelectedName)
                }
            }

        binding.recyclerViewWhiteNoises.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerViewWhiteNoises.adapter = adapter
    }

    private fun loadNoises() {
        whiteNoiseList = WhiteNoiseData.list.toMutableList()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val saved = prefs.getString(KEY_SELECTED_NOISE, null)
        selectedNoiseName = saved

        whiteNoiseList =
            whiteNoiseList
                .map {
                    it.copy(isSelected = it.name == saved)
                }.toMutableList()
        adapter.submitList(whiteNoiseList.toList())

        saved?.let { playWhiteNoise(it) }

        updateSelectButtonState()
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
                if (hasSelection) {
                    getDrawable(R.drawable.btn_selected_noise)
                } else {
                    getDrawable(R.drawable.btn_unselect_noise)
                }
        }
    }

    private fun playWhiteNoise(name: String) {
        val resId = WhiteNoiseSoundMap.map[name] ?: return
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

    private fun saveSelection(name: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit { putString(KEY_SELECTED_NOISE, name) }
    }

    companion object {
        private const val PREFS_NAME = "WhiteNoisePrefs"
        private const val KEY_SELECTED_NOISE = "selected_white_noise"
    }
}
