package com.example.mutism.myPage

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mutism.NoiseSelectActivity
import com.example.mutism.WhiteNoiseActivity
import com.example.mutism.databinding.ActivityMyPageBinding

class MyPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSelectNoise.setOnClickListener {
            val intent = Intent(this, NoiseSelectActivity::class.java)
            startActivity(intent)
        }

        binding.btnSelectWhiteNoise.setOnClickListener {
            val intent = Intent(this, WhiteNoiseActivity::class.java)
            startActivity(intent)
        }
    }
}
