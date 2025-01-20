import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.databinding.FragmentAddlikedBinding

class ShowAddGroupDialog : DialogFragment() {
    private lateinit var binding: FragmentAddlikedBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddlikedBinding.inflate(inflater, container, false)
        val view = binding.root
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.drawable.filter_dialog_round)
        return view
    }

    override fun onStart() {
        super.onStart()

        // 뒤로 가기 버튼
        binding.cancelX.setOnClickListener {
            dismiss()
        }
    }
}
