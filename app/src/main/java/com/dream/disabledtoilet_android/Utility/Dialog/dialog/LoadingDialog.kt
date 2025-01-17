package com.dream.disabledtoilet_android.Utility.Dialog.dialog

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.DialogFragment
import com.dream.disabledtoilet_android.databinding.LoadingDialogBinding

class LoadingDialog: DialogFragment() {
    lateinit var binding: LoadingDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LoadingDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뱅글뱅글 회전 애니메이션 설정
        val animator = ObjectAnimator.ofFloat(binding.loadingCircle, "rotation", 0f, 360f)
        animator.duration = 2000 // 1초 동안 한 바퀴 회전
        animator.repeatCount = ObjectAnimator.INFINITE // 무한 반복
        animator.interpolator = LinearInterpolator() // 일정한 속도로 회전
        animator.start()
    }
}