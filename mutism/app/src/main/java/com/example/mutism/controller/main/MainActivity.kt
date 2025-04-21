package com.example.mutism.controller.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mutism.R
import com.example.mutism.controller.myPage.MyPageActivity
import com.example.mutism.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvRecording.visibility = View.GONE

        binding.btnStart.setOnClickListener {
            isRecording = !isRecording
            updateRecordingUI(isRecording)
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
            rootLayout.setBackgroundResource(R.drawable.bg_main2)
            binding.tvWelcome.visibility = View.GONE
            binding.tvRecording.visibility = View.VISIBLE
            binding.spaceBetween.visibility = View.GONE
            binding.tvNowHear.visibility = View.VISIBLE
            binding.mainTvExplain.text = getString(R.string.text_main_stop)
        } else {
            binding.btnStart.setImageResource(R.drawable.btn_start1)
            rootLayout.setBackgroundResource(R.drawable.bg_main1)
            binding.tvWelcome.visibility = View.VISIBLE
            binding.tvRecording.visibility = View.GONE
            binding.spaceBetween.visibility = View.VISIBLE
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

    companion object {
        const val REQUEST_CALL_PERMISSION = 100
        const val EMERGENCY_NUMBER = "112"
    }
}
