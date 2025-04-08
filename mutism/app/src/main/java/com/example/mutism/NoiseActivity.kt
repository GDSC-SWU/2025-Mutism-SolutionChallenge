package com.example.mutism

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mutism.databinding.ActivityNoiseBinding
import com.example.mutism.model.NoiseData
import com.example.mutism.model.NoiseItem

class NoiseActivity : AppCompatActivity() {

    private lateinit var binding : ActivityNoiseBinding
    private lateinit var adapter: NoiseAdapter
    private var noiseList = mutableListOf<NoiseItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        loadNoises()

        binding.btnSelect.setOnClickListener {
            val selected = noiseList.filter { it.isSelected }
            Toast.makeText(this,"선택된 항목: ${selected.joinToString { it.name }}",Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun initRecyclerView(){
        adapter = NoiseAdapter{selectedItem ->
            noiseList = noiseList.map {
                if (it.name == selectedItem.name) it.copy(isSelected = !it.isSelected)
                else it
            }.toMutableList()

            adapter.submitList(noiseList.toList())
        }

        binding.recyclerViewNoises.layoutManager = GridLayoutManager(this,3)
        binding.recyclerViewNoises.adapter = adapter
    }

    private fun loadNoises(){
        noiseList = NoiseData.list.toMutableList()
        adapter.submitList(noiseList.toList())
    }

}