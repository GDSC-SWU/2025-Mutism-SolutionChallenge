package com.example.mutism.controller.myPage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mutism.controller.noiseSelectPage.NoiseSelectActivity
import com.example.mutism.controller.whiteNoisePage.WhiteNoiseActivity
import com.example.mutism.databinding.ActivityMyPageBinding
import com.google.android.material.chip.Chip

class MyPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPageBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.selectedNoiseContainer.setOnClickListener {
            val intent = Intent(this, NoiseSelectActivity::class.java)
            startActivity(intent)
        }

        binding.whiteNoiseContainer.setOnClickListener {
            val intent = Intent(this, WhiteNoiseActivity::class.java)
            startActivity(intent)
        }

        sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        loadUserInfo()
        updateSelectedNoiseChips()

        binding.btnSave.setOnClickListener {
            saveUserInfo()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelectedNoiseChips()
    }

    private fun saveUserInfo() {
        val autismLevel = binding.tvAutismLevelValue.text.toString()
        val gender = binding.tvGenderValue.text.toString()
        val age = binding.tvAgeValue.text.toString()

        val editor = sharedPrefs.edit()
        editor.putString(KEY_AUTISM_LEVEL, autismLevel)
        editor.putString(KEY_GENDER, gender)
        editor.putString(KEY_AGE, age)
        editor.apply()

        Toast.makeText(this, "User information saved!", Toast.LENGTH_SHORT).show()
    }

    private fun loadUserInfo() {
        val autismLevel = sharedPrefs.getString(KEY_AUTISM_LEVEL, "")
        val gender = sharedPrefs.getString(KEY_GENDER, "")
        val age = sharedPrefs.getString(KEY_AGE, "")

        binding.tvAutismLevelValue.setText(autismLevel)
        binding.tvGenderValue.setText(gender)
        binding.tvAgeValue.setText(age)
    }

    private fun updateSelectedNoiseChips() {
        val noisePrefs = getSharedPreferences("NoiseSelectPrefs", Context.MODE_PRIVATE)
        val savedTags = noisePrefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet())

        val chipGroup = binding.chipGroupSelectedNoises
        chipGroup.removeAllViews() // remove chips

        savedTags?.forEach { tag ->
            val chip =
                Chip(this).apply {
                    text = tag
                    isCloseIconVisible = false
                    isClickable = false
                    isCheckable = false
                }
            chipGroup.addView(chip)
        }
    }

    companion object {
        private const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
        const val KEY_AUTISM_LEVEL = "autism_level"
        const val KEY_GENDER = "gender"
        const val KEY_AGE = "age"
    }
}
