package com.dream.disabledtoilet_android.Detail

import ToiletModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.dream.disabledtoilet_android.User.ToiletPostViewModel
import com.dream.disabledtoilet_android.User.ViewModel.UserViewModel
import com.dream.disabledtoilet_android.databinding.FragmentDetailOptionBinding

class DetailOptionFragment : Fragment() {

    private val TAG = "DetailOptionFragment"

    private var _binding: FragmentDetailOptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: ToiletPostViewModel
    private lateinit var userViewModel: UserViewModel

    //현재 화장실 데이터 저장
    private var currentToilet : ToiletModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postViewModel = ViewModelProvider(this).get(ToiletPostViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        val email = ToiletData.currentUser
        Log.d("test" , " onCreate email: ${email}")
        if(email != null){
            userViewModel.fetchUserByEmail(email)
            Log.d("test", "uesrViewmodel : ${userViewModel.currentUser.value}")
        }

        // 전달받은 화장실 데이터
        currentToilet = arguments?.getParcelable<ToiletModel>("TOILET_DATA")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailOptionBinding.inflate(inflater, container, false)
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        currentToilet?.let { toilet ->
            setupUI(toilet)
            setupSaveButton(toilet)
        }

        return binding.root
    }

    private fun setupUI(toilet: ToiletModel) {
        // 기본 정보 표시
        binding.toiletName.text = toilet.restroom_name
        binding.toiletLocationAddress.text = toilet.address_road
        binding.toiletManageOfficeName.text = toilet.management_agency_name
        binding.toiletManageOfficeNumber.text = toilet.phone_number ?: "-"

        copyAddress(toilet)
        clickPhoneNumber(toilet)
    }

    /**
     * 전화번호 클릭시 키패드 연결
     */
    private fun clickPhoneNumber(toilet: ToiletModel){
        // 전화번호 클릭 이벤트
        binding.toiletManageOfficeNumber.setOnClickListener {
            val number = toilet.phone_number
            if (!number.isNullOrEmpty()) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$number")
                }
                startActivity(dialIntent)
            } else {
                Toast.makeText(requireContext(), "전화번호가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 주소 클릭시 복사
     */
    private fun copyAddress(toilet: ToiletModel){
        // 주소 복사 기능
        binding.copyAddressIcon.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("address", toilet.address_road)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "주소가 복사되었습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSaveButton(toilet: ToiletModel) {
        val saveButton : LinearLayout = binding.saveBtn3
        val saveIcon = binding.iconToggle
        val saveTxt = binding.toiletSaveCount

        val email = ToiletData.currentUser
        if(email != null){
            updateLikeButtonIcon(saveIcon, email, toilet)
        }

        postViewModel.toiletLikes.observe(viewLifecycleOwner) {likes ->
            saveTxt.text = "저장 (${likes.size})"
            val userId = userViewModel.currentUser.value?.email ?: return@observe
            updateLikeButtonIcon(saveIcon, userId, toilet)
        }

        saveButton.setOnClickListener {
            Log.d("test", "클릭1  userViewModel.currentUser : ${userViewModel.currentUser}")
            Log.d("test", "클릭1  isLiked : ${postViewModel.toiletLikes}")

            Log.d("test", "userId : ${userViewModel.currentUser.value}")


            val userId = userViewModel.currentUser.value?.email ?: return@setOnClickListener
            val isLiked = postViewModel.isLikedByUser(userId)
            Log.d("test", "클릭  userViewModel.currentUser : ${userViewModel.currentUser}")
            Log.d("test", "클릭  isLiked : ${postViewModel.toiletLikes}")

            if(isLiked){
                postViewModel.removeLike(toilet.number, userId)
            }else{
                postViewModel.addLike(toilet.number, userId)
            }
        }
    }

    private fun updateLikeButtonIcon(likeButton: ImageView, userId: String, toilet: ToiletModel?){
        Log.d("test", "post :  ${postViewModel.toiletLikes.value}")
//        val isLiked = postViewModel.isLikedByUser(userId)
        val isLiked = userViewModel.currentUser.value?.likedToilets?.contains(toilet?.number.toString())
        Log.d("test", "user :  ${userViewModel.currentUser.value?.likedToilets}")

        if(isLiked == true){
            likeButton.setImageResource(R.drawable.saved_star_icon)
        }else{
            likeButton.setImageResource(R.drawable.save_icon)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        if(currentToilet == null){
            return
        }

        currentToilet.let { toilet ->
            postViewModel.observePostLikes(toilet!!.number)
        }
    }
}
