package com.example.mutism.controller.myPage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.mutism.model.AutismLevel
import com.example.mutism.model.Gender
import com.google.android.material.chip.Chip

class MyPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPageBinding
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var genderType: ArrayList<Gender>
    private lateinit var autismLevelType: ArrayList<AutismLevel>
    private var selectedAutismLevel: AutismLevel? = null
    private var selectedGender: Gender? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.edtRelaxMethod.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.myPage.postDelayed({
                    binding.myPage.smoothScrollTo(0, binding.edtRelaxMethod.bottom + 500)
                }, 200)
            }
        }

        binding.tvEmergencyContactValue.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.myPage.postDelayed({
                    binding.myPage.smoothScrollTo(0, binding.tvEmergencyContactValue.bottom)
                }, 200)
            }
        }

        binding.tvNameValue.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.myPage.postDelayed({
                    binding.myPage.smoothScrollTo(0, binding.tvNameValue.top)
                }, 200)
            }
        }

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

        autismLevelType =
            arrayListOf(
                AutismLevel("Mild", 1),
                AutismLevel("Severe", 2),
            )

        genderType =
            arrayListOf(
                Gender("Male", "Male"),
                Gender("Female", "Female"),
                Gender("Other", "Other"),
            )

        val autismLevelSpinnerAdapter = ArrayAdapter(this, R.layout.item_spinner, autismLevelType)
        autismLevelSpinnerAdapter.setDropDownViewResource(R.layout.item_spinner)
        binding.autismLevelSpinner.adapter = autismLevelSpinnerAdapter

        val genderSpinnerAdapter = ArrayAdapter(this, R.layout.item_spinner, genderType)
        genderSpinnerAdapter.setDropDownViewResource(R.layout.item_spinner)
        binding.genderSpinner.adapter = genderSpinnerAdapter

        val savedAutismLevelType = sharedPrefs.getString(KEY_AUTISM_LEVEL, null)
        savedAutismLevelType?.let {
            val index = autismLevelType.indexOfFirst { level -> level.type == it }
            if (index != -1) {
                binding.autismLevelSpinner.setSelection(index)
                selectedAutismLevel = autismLevelType[index]
            }
        }

        val savedGenderType = sharedPrefs.getString(KEY_GENDER, null)
        savedGenderType?.let {
            val index = genderType.indexOfFirst { gender -> gender.type == it }
            if (index != -1) {
                binding.genderSpinner.setSelection(index)
                selectedGender = genderType[index]
            }
        }

        binding.autismLevelSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selectedAutismLevel = autismLevelType[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedAutismLevel = null
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

        setupPhoneNumberFormatting()
    }

    override fun onResume() {
        super.onResume()
        updateSelectedNoiseChips()
    }

    private fun saveUserInfo() {
        val name = binding.tvNameValue.text.toString()
        val autismLevel = selectedAutismLevel?.type ?: ""
        val gender = selectedGender?.type ?: ""
        val emergencyNumber =
            binding.tvEmergencyContactValue.text
                .toString()
                .replace("-", "")
        val relaxMethod = binding.edtRelaxMethod.text.toString()

        if (name.isBlank() || autismLevel.isBlank() || gender.isBlank() || emergencyNumber.isBlank() || relaxMethod.isBlank()) {
            Toast.makeText(this, "Please fill out all required information.", Toast.LENGTH_SHORT).show()
            return
        }

        sharedPrefs.edit {
            putString(KEY_NAME, name)
            putString(KEY_AUTISM_LEVEL, autismLevel)
            putString(KEY_GENDER, gender)
            putString(KEY_EMERGENCY_CONTACT, emergencyNumber)
            putString(KEY_RELAX_METHOD, relaxMethod)
        }

        Log.d("saveUserInfo", "saveUserInfo : $autismLevel,$gender,$relaxMethod")
        Toast.makeText(this, "User information saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadUserInfo() {
        val name = sharedPrefs.getString(KEY_NAME, "")
        val emergencyNumber = sharedPrefs.getString(KEY_EMERGENCY_CONTACT, "")
        val relaxMethod = sharedPrefs.getString(KEY_RELAX_METHOD, "")

        binding.tvNameValue.setText(name)
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

    private fun setupPhoneNumberFormatting() {
        binding.tvEmergencyContactValue.addTextChangedListener(
            object : TextWatcher {
                private var isFormatting = false
                private var lastText = ""

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isFormatting) return

                    val digitsOnly = s.toString().replace("-", "")
                    if (digitsOnly == lastText) return

                    isFormatting = true
                    lastText = digitsOnly

                    val formatted = formatPhoneNumber(digitsOnly)
                    binding.tvEmergencyContactValue.setText(formatted)
                    binding.tvEmergencyContactValue.setSelection(formatted.length)
                    isFormatting = false
                }

                private fun formatPhoneNumber(number: String): String =
                    when {
                        number.length >= 11 -> number.replaceFirst("(\\d{3})(\\d{4})(\\d+)".toRegex(), "$1-$2-$3")
                        number.length >= 10 -> number.replaceFirst("(\\d{3})(\\d{3})(\\d+)".toRegex(), "$1-$2-$3")
                        number.length >= 7 -> number.replaceFirst("(\\d{3})(\\d{4})".toRegex(), "$1-$2")
                        else -> number
                    }
            },
        )
    }

    companion object {
        private const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
        const val KEY_NAME = "name"
        private const val KEY_AUTISM_LEVEL = "autism_level"
        const val KEY_GENDER = "gender"
        private const val KEY_EMERGENCY_CONTACT = "emergency contact"
        const val KEY_RELAX_METHOD = "relax_method"
    }
}
