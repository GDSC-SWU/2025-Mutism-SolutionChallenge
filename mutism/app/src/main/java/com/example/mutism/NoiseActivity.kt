package com.example.mutism

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mutism.databinding.ActivityNoiseBinding
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
        noiseList = mutableListOf(
            NoiseItem("car",R.drawable.noise_car),
            NoiseItem("horn",R.drawable.noise_car),
            NoiseItem("coffee\n" + "machine",R.drawable.noise_car),
            NoiseItem("dog bark",R.drawable.noise_car),
            NoiseItem("vacuum\n" + "cleaner",R.drawable.noise_car),
            NoiseItem("phone\n" + "ringing",R.drawable.noise_car),
            NoiseItem("clock\n" + "ticking",R.drawable.noise_car),
            NoiseItem("door\n" + "opening",R.drawable.noise_car),
            NoiseItem("air conditioner\n" + "/ heater",R.drawable.noise_car),
            NoiseItem("fan",R.drawable.noise_car),
            NoiseItem("kitchen\n" + "appliance",R.drawable.noise_car),
            NoiseItem("talking in\n" + "restaurants",R.drawable.noise_car)



        )
        adapter.submitList(noiseList.toList())
    }

}