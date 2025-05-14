package com.example.mutism.controller.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import com.example.mutism.databinding.DialogSelectNoiseBinding

class SelectNoiseDialog(
    context: Context,
    private val onSelectNoiseClicked: () -> Unit,
) : Dialog(context) {
    private var binding: DialogSelectNoiseBinding = DialogSelectNoiseBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(), // 전체 폭의 90%
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // 배경 투명 처리

        binding.cvBtnSelectNoise.setOnClickListener {
            onSelectNoiseClicked.invoke() // 버튼 클릭 시 콜백 호출
            dismiss()
        }
    }
}
