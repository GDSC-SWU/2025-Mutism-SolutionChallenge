package com.example.mutism.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.example.mutism.databinding.DialogSelectNoiseBinding

class SelectNoiseDialog(
    context: Context,
    private val onSelectNoiseClicked: () -> Unit,
) : Dialog(context) {
    private var binding: DialogSelectNoiseBinding = DialogSelectNoiseBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 배경 투명 처리

        binding.cvBtnSelectNoise.setOnClickListener {
            onSelectNoiseClicked.invoke() // 버튼 클릭 시 콜백 호출
            dismiss()
        }
    }
}
