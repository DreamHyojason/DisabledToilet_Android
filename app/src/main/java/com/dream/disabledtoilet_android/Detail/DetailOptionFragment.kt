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
import androidx.lifecycle.lifecycleScope
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.dream.disabledtoilet_android.User.ToiletPostViewModel
import com.dream.disabledtoilet_android.User.ViewModel.UserViewModel
import com.dream.disabledtoilet_android.databinding.FragmentDetailOptionBinding
import kotlinx.coroutines.launch

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
        Log.d("test", "onCreate email: $email")
        if (email != null) {
            userViewModel.observePostLikesUser(email)

            lifecycleScope.launch {
                // 비동기적으로 사용자 정보를 가져옵니다.
                userViewModel.fetchUserByEmail(email)
                Log.d("test1", "user : ${userViewModel.currentUser.value}")

                // 사용자 정보가 로드된 후 setupSaveButton 호출
                currentToilet?.let { toilet ->
                    setupSaveButton(toilet)
                }
            }
        }

        // currentUser를 observe하여 값이 업데이트될 때마다 로그를 찍습니다.
        userViewModel.currentUser.observe(this) { user ->
            Log.d("test1", "user : $user")
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
//            setupSaveButton(toilet)
        }

        return binding.root
    }

    private fun setupUI(toilet: ToiletModel) {
        // 기본 정보 표시
        binding.toiletName.text = toilet.restroom_name
        binding.toiletLocationAddress.text = toilet.address_road
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

        Log.d("test4", "first post:  ${postViewModel.toiletLikes.value}")
        Log.d("test4", "first user:  ${userViewModel.currentUser?.value?.likedToilets}")

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
            val userId = userViewModel.currentUser.value?.email ?: return@setOnClickListener
            val isLiked = postViewModel.isLikedByUser(userId)
            val isLiked2 = userViewModel.isLikedUser(toilet.number.toString())

            Log.d("test4", "post:  ${postViewModel.toiletLikes.value}")
            Log.d("test4", "user:  ${userViewModel.currentUser?.value?.likedToilets}")
            Log.d("test4", "isLiked1:  ${isLiked}")
            Log.d("test4", "isLiked2:  ${isLiked2}")


            if(isLiked2){
                postViewModel.removeLike(toilet.number, userId)
                userViewModel.removeLikeUser(toilet.number, userId)
                Log.d("test3", "user 삭제:  ${userViewModel.currentUser.value?.likedToilets}")
                
            }else{
                postViewModel.addLike(toilet.number, userId)
                userViewModel.addLikeUser(toilet.number, userId)
                Log.d("test3", "user 추가:  ${userViewModel.currentUser.value?.likedToilets}")

            }

            Log.d("test4", "after post:  ${postViewModel.toiletLikes.value}")
            Log.d("test4", "after user:  ${userViewModel.currentUser?.value?.likedToilets}")

        }
    }

    private fun updateLikeButtonIcon(likeButton: ImageView, userId: String, toilet: ToiletModel?){
        val isLiked = userViewModel.isLikedUser(toilet?.number.toString())

        Log.d("test4", "isLiked UPDATEBUTTON 1 : ${userViewModel.currentUser.value}")
        Log.d("test4", "isLiked UPDATEBUTTON 2 :  ${isLiked}")

        if(isLiked){
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
