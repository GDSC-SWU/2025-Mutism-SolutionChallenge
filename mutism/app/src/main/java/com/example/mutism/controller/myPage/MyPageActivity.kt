package com.example.mutism.controller.myPage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.mutism.R
import com.example.mutism.controller.noiseSelectPage.NoiseSelectActivity
import com.example.mutism.controller.whiteNoisePage.WhiteNoiseActivity
import com.example.mutism.databinding.ActivityMyPageBinding
import com.example.mutism.model.Gender
import com.google.android.material.chip.Chip

class MyPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPageBinding
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var genderType: ArrayList<Gender>
    private var selectedGender: Gender? = null

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

        genderType =
            arrayListOf(
                Gender("Male", "Male"),
                Gender("Female", "Female"),
                Gender("Other", "Other"),
            )

        val spinnerAdapter = ArrayAdapter(this, R.layout.item_spinner_gender, genderType)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_gender)
        binding.genderSpinner.adapter = spinnerAdapter

        val savedGenderType = sharedPrefs.getString(KEY_GENDER, null)
        savedGenderType?.let {
            val index = genderType.indexOfFirst { gender -> gender.type == it }
            if (index != -1) {
                binding.genderSpinner.setSelection(index)
                selectedGender = genderType[index] // 초기값도 변수에 담아둠
            }
        }

        binding.genderSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selectedGender = genderType[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedGender = null
                }
            }

        binding.btnSave.setOnClickListener {
            saveUserInfo()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelectedNoiseChips()
    }

    private fun saveUserInfo() {
        val name = binding.tvNameValue.text.toString()
        val autismLevel = binding.tvAutismLevelValue.text.toString()
        val gender = selectedGender?.type ?: ""
        val age = binding.tvAgeValue.text.toString()
        val emergencyNumber = binding.tvEmergencyContactValue.text.toString()
        val relaxMethod = binding.edtRelaxMethod.text.toString()

        sharedPrefs.edit {
            putString(KEY_NAME, name)
            putString(KEY_AUTISM_LEVEL, autismLevel)
            putString(KEY_GENDER, gender)
            putString(KEY_AGE, age)
            putString(KEY_EMERGENCY_CONTACT, emergencyNumber)
            putString(KEY_RELAX_METHOD, relaxMethod)
        }

        Log.d("saveUserInfo", "saveUserInfo : $autismLevel,$gender,$age,$relaxMethod")
        Toast.makeText(this, "User information saved!", Toast.LENGTH_SHORT).show()
    }

    private fun loadUserInfo() {
        val name = sharedPrefs.getString(KEY_NAME, "")
        val autismLevel = sharedPrefs.getString(KEY_AUTISM_LEVEL, "")
        val age = sharedPrefs.getString(KEY_AGE, "")
        val emergencyNumber = sharedPrefs.getString(KEY_EMERGENCY_CONTACT, "")
        val relaxMethod = sharedPrefs.getString(KEY_RELAX_METHOD, "")

        binding.tvNameValue.setText(name)
        binding.tvAutismLevelValue.setText(autismLevel)
        binding.tvAgeValue.setText(age)
        binding.tvEmergencyContactValue.setText(emergencyNumber)
        binding.edtRelaxMethod.setText(relaxMethod)
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
        private const val KEY_NAME = "name"
        private const val KEY_AUTISM_LEVEL = "autism_level"
        private const val KEY_GENDER = "gender"
        private const val KEY_AGE = "age"
        private const val KEY_EMERGENCY_CONTACT = "emergency contact"
        const val KEY_RELAX_METHOD = "relax_method"
    }
}
