package com.example.disabledtoilet_android.Utility.Dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.disabledtoilet_android.R
import com.example.disabledtoilet_android.databinding.DialogFilterBinding
import com.example.disabledtoilet_android.databinding.LoadingDialogBinding

class FilterDialog: DialogFragment() {
    lateinit var binding: DialogFilterBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFilterBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.filter_dialog_round)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setUi()
    }

    fun setUi(){
        binding.cancelButton.setOnClickListener{
            this.dismiss()
        }
    }

}