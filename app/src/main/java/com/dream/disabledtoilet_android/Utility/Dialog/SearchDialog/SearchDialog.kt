package com.dream.disabledtoilet_android.Utility.Dialog.SearchDialog

import android.app.Dialog
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.Utility.Dialog.SearchDialog.Listener.SearchDialogListener
import com.dream.disabledtoilet_android.Utility.Dialog.SearchDialog.ViewModel.SearchDialogViewModel
import com.dream.disabledtoilet_android.databinding.DialogSearchLocationBinding


class SearchDialog(val x: Int, val y: Int, val searchDialogListener: SearchDialogListener) : DialogFragment() {
    lateinit var binding: DialogSearchLocationBinding
    lateinit var viewModel: SearchDialogViewModel


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 뷰모델 받아오기
        viewModel = ViewModelProvider(this)[SearchDialogViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSearchLocationBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(
            R.drawable.filter_dialog_round
        )
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes
        val deviceWidth = x
        val deviceHeight = y
        params?.width = (deviceWidth)
        params?.height = (deviceHeight)
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}