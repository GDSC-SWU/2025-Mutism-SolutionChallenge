package com.example.mutism

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mutism.databinding.ActivityWhiteNoiseBinding
import com.example.mutism.model.NoiseItem
import com.example.mutism.model.WhiteNoiseData
import com.example.mutism.model.WhiteNoiseItem

class WhiteNoiseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhiteNoiseBinding
    private lateinit var adapter: WhiteNoiseAdapter
    private var whiteNoiseList = mutableListOf<WhiteNoiseItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWhiteNoiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        loadNoises()

        binding.btnSelect.setOnClickListener {
            val selected = whiteNoiseList.filter { it.isSelected }
            Toast.makeText(this,"선택된 항목: ${selected.joinToString { it.name }}", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener { finish() }

    }

    private fun initRecyclerView(){
        adapter = WhiteNoiseAdapter{selectedItem ->
            whiteNoiseList = whiteNoiseList.map {
                if (it.name == selectedItem.name) it.copy(isSelected = !it.isSelected)
                else it
            }.toMutableList()

            adapter.submitList(whiteNoiseList.toList())
        }

        binding.recyclerViewWhiteNoises.layoutManager = GridLayoutManager(this,3)
        binding.recyclerViewWhiteNoises.adapter = adapter
    }

    private fun loadNoises() {
        whiteNoiseList = WhiteNoiseData.list.toMutableList()
        adapter.submitList(whiteNoiseList.toList())
    }

}