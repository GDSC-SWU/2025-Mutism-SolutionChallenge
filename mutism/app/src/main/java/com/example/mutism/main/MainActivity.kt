package com.example.mutism.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mutism.R
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
            if (!isRecording) {
                // Enter recording mode
                binding.btnStart.setImageResource(R.drawable.btn_stop)
                val rootLayout = findViewById<View>(R.id.main)
                rootLayout.setBackgroundResource(R.drawable.bg_main2)
                binding.tvWelcome.visibility = View.GONE
                binding.tvRecording.visibility = View.VISIBLE
                binding.spaceBetween.visibility = View.GONE
                binding.tvNowHear.visibility = View.VISIBLE
                binding.mainTvExplain.text = getString(R.string.text_main_stop)
            } else {
                // Revert to original state
                binding.btnStart.setImageResource(R.drawable.btn_start1)
                val rootLayout = findViewById<View>(R.id.main)
                rootLayout.setBackgroundResource(R.drawable.bg_main1)
                binding.tvWelcome.visibility = View.VISIBLE
                binding.tvRecording.visibility = View.GONE
                binding.spaceBetween.visibility = View.VISIBLE
                binding.tvNowHear.visibility = View.GONE
                binding.mainTvExplain.text = getString(R.string.text_main_start)
            }

            isRecording = !isRecording
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
