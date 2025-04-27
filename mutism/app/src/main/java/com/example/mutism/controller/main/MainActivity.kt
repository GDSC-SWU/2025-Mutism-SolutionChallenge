package com.example.mutism.controller.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mutism.R
import com.example.mutism.controller.myPage.MyPageActivity
import com.example.mutism.controller.noiseSelectPage.NoiseSelectActivity
import com.example.mutism.databinding.ActivityMainBinding
import com.example.mutism.dialog.SelectNoiseDialog

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isRecording = false
    private var selectNoiseDialog: SelectNoiseDialog? = null

    // ActivityResultLauncher
    private val noiseSelectLauncher = registerNoiseSelectLauncher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvRecording.visibility = View.GONE

        binding.btnStart.setOnClickListener {
            val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
            val selectedNoiseTags = sharedPrefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet())

            if (selectedNoiseTags.isNullOrEmpty()) {
                if (selectNoiseDialog?.isShowing != true) {
                    selectNoiseDialog =
                        SelectNoiseDialog(this) {
                            // 이 콜백 안에서 NoiseSelectActivity 띄우기
                            val intent = Intent(this, NoiseSelectActivity::class.java)
                            noiseSelectLauncher.launch(intent)
                        }
                    selectNoiseDialog?.show()
                }
            } else {
                isRecording = !isRecording
                updateRecordingUI(isRecording)
            }
        }

        binding.btnMyPage.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }

        binding.btnSos.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                makeEmergencyCall()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                    Toast.makeText(this, "This app needs permission to make emergency calls.", Toast.LENGTH_LONG).show()
                }

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    REQUEST_CALL_PERMISSION,
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        val rootLayout = findViewById<View>(R.id.main)
        if (isRecording) {
            binding.btnStart.setImageResource(R.drawable.btn_stop)
            binding.tvWelcome.visibility = View.GONE
            binding.tvRecording.visibility = View.VISIBLE

            binding.tvNowHear.visibility = View.VISIBLE
            binding.mainTvExplain.text = getString(R.string.text_main_stop)
        } else {
            binding.btnStart.setImageResource(R.drawable.btn_start1)
            binding.tvWelcome.visibility = View.VISIBLE
            binding.tvRecording.visibility = View.GONE

            binding.tvNowHear.visibility = View.GONE
            binding.mainTvExplain.text = getString(R.string.text_main_start)
        }
    }

    private fun makeEmergencyCall() {
        try {
            val intent =
                Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$EMERGENCY_NUMBER".toUri()
                }
            startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to make call. Permission not granted.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Call failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeEmergencyCall()
            } else {
                Toast.makeText(this, "Phone call permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerNoiseSelectLauncher() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "소음 선택이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    companion object {
        const val REQUEST_CALL_PERMISSION = 100
        const val EMERGENCY_NUMBER = "112"
        private const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
    }
}
