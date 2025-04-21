package com.example.mutism.controller.whiteNoisePage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mutism.R
import com.example.mutism.databinding.ItemWhiteNoiseBinding
import com.example.mutism.model.WhiteNoiseItem

class WhiteNoiseAdapter(
    private val onClick: (WhiteNoiseItem) -> Unit,
) : ListAdapter<WhiteNoiseItem, WhiteNoiseAdapter.WhiteNoiseViewHolder>(DIFF_CALLBACK) {
    inner class WhiteNoiseViewHolder(
        private val binding: ItemWhiteNoiseBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhiteNoiseItem) {
            binding.label.text = item.name
            binding.icon.setImageResource(item.iconResId)

            val iconBg =
                if (item.isSelected) {
                    R.drawable.item_noise_selected_bg
                } else {
                    R.drawable.item_noise_unselect_bg
                }
            binding.iconContainer.background = ContextCompat.getDrawable(binding.root.context, iconBg)

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): WhiteNoiseViewHolder {
        val binding = ItemWhiteNoiseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WhiteNoiseViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: WhiteNoiseViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<WhiteNoiseItem>() {
                override fun areItemsTheSame(
                    oldItem: WhiteNoiseItem,
                    newItem: WhiteNoiseItem,
                ): Boolean = oldItem.name == newItem.name

                override fun areContentsTheSame(
                    oldItem: WhiteNoiseItem,
                    newItem: WhiteNoiseItem,
                ): Boolean = oldItem == newItem
            }
    }
}
