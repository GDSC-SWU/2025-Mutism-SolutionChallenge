package com.example.mutism.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.example.mutism.databinding.DialogWhiteNoiseEndPromptBinding

class WhiteNoiseRelaxationDialog(
    context: Context,
    // 'Turn off' 버튼 콜백
    private val onStopRequested: () -> Unit,
    // 'keep playing' 버튼 콜백
    private val onContinue: () -> Unit,
) : Dialog(context) {
    private val binding = DialogWhiteNoiseEndPromptBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 'keep playing' → 재생 유지
        binding.cvBtnKeepPlaying.setOnClickListener {
            onContinue.invoke()
            dismiss()
        }

        // 'Turn off' → 재생 종료
        binding.cvBtnTurnOff.setOnClickListener {
            onStopRequested.invoke()
            dismiss()
        }
    }
}
