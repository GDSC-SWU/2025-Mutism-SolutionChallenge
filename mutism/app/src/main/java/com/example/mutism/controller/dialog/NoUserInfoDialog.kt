package com.example.mutism.controller.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import com.example.mutism.controller.myPage.MyPageActivity
import com.example.mutism.databinding.DialogNoUserInfoBinding

class NoUserInfoDialog(
    context: Context,
) : Dialog(context) {
    private val binding: DialogNoUserInfoBinding =
        DialogNoUserInfoBinding.inflate(LayoutInflater.from(context))

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(), // 전체 폭의 90%
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // 배경 투명 처리

        setCancelable(true)

        binding.cvBtnGoToMyPage.setOnClickListener {
            context.startActivity(Intent(context, MyPageActivity::class.java))
            dismiss()
        }
    }
}
