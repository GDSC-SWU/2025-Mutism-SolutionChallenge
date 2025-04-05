package com.example.mutism

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mutism.databinding.ItemNoiseBinding
import com.example.mutism.model.NoiseItem

class NoiseAdapter (
    private val onClick: (NoiseItem)-> Unit
): ListAdapter<NoiseItem,NoiseAdapter.NoiseViewHolder>(DIFF_CALLBACK){

    inner class NoiseViewHolder(private val binding: ItemNoiseBinding):
            RecyclerView.ViewHolder(binding.root){
        fun bind(item: NoiseItem){
            binding.label.text = item.name
            binding.icon.setImageResource(item.iconResId)

            binding.root.alpha = if(item.isSelected) 0.5f else 1f

            binding.root.setOnClickListener{
                onClick(item)
            }
        }}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoiseViewHolder {
        val binding = ItemNoiseBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return NoiseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoiseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object{
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NoiseItem>(){
            override fun areItemsTheSame(oldItem: NoiseItem, newItem: NoiseItem): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: NoiseItem, newItem: NoiseItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}



