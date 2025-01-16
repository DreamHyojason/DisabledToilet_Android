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
import android.widget.TextView
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
    private lateinit var scrollViewContainer : LinearLayout

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
        binding.toiletOpeningHours.text = toilet.opening_hours
        binding.toiletLocationAddress.text = toilet.address_road
        binding.toiletManageOfficeName.text = toilet.management_agency_name
        binding.toiletManageOfficeNumber.text = toilet.phone_number?.replace("[\"']".toRegex(), "") ?: ""

        copyAddress(toilet)
        clickPhoneNumber(toilet)
        addToiletInfo(toilet)
    }

    /**
     * 화장실 상세 정보 추가
     */
    private fun addToiletInfo(toilet: ToiletModel){
        scrollViewContainer = binding.filterOptionContainer

        filterOptionAddScrollView(toilet)

        //남성 화장실 정보 표시
        // 남성 화장실 정보 표시
        val maleContentLinear = binding.toiletInfoManListLinear
        addToiletInfo(maleContentLinear, "화장실 개수", toilet.male_toilet_count)
        addToiletInfo(maleContentLinear, "소변기 개수", toilet.male_urinal_count)
        addToiletInfo(maleContentLinear, "장애인용 화장실 개수", toilet.male_disabled_toilet_count)
        addToiletInfo(maleContentLinear, "장애인용 소변기 개수", toilet.male_disabled_urinal_count)

        val femaleContentLinear = binding.toiletInfoWomanListLinear
        // 여성 화장실 정보 표시 (필요한 경우)
        addToiletInfo(femaleContentLinear, "화장실 개수", toilet.female_toilet_count)
        addToiletInfo(femaleContentLinear, "장애인용 화장실 개수", toilet.female_disabled_toilet_count)
        addToiletInfo(femaleContentLinear, "어린이용 화장실 개수", toilet.female_child_toilet_count)

        // 기타 정보 표시
        binding.toiletManageOfficeName.text = if (toilet.management_agency_name.isNullOrBlank() ||
            toilet.management_agency_name == "\"" ||
            toilet.management_agency_name == "\"\"" ||
            toilet.management_agency_name == "") {
            "-"
        } else {
            toilet.management_agency_name
        }

        binding.toiletOpeningHours.text = if (toilet.opening_hours_detail.isNullOrBlank() ||
            toilet.opening_hours_detail == "\"" ||
            toilet.opening_hours_detail == "\"\"" ||
            toilet.opening_hours_detail == "") {
            "-"
        } else {
            toilet.opening_hours_detail
        }
    }

    //해당 화장실의 조건 스크롤 뷰
    private fun filterOptionAddScrollView(toilet : ToiletModel){

        if (toilet.emergency_bell_location == "Y") {
            addTextView("비상벨")
        }

        if (toilet.restroom_entrance_cctv_installed == "Y") {
            addTextView("입구 CCTV 설치")
        }

        //공중화장실 or 개방화장실
        if (toilet.category != ""){
            addTextView(toilet.category)
        }

        //민간소유
        if(toilet.restroom_ownership_type.contains("민간소유")){
            addTextView("민간 소유")
        }

        //공공기관 소유
        if(toilet.restroom_ownership_type.contains("공공기관")){
            addTextView("공공기관")
        }

        if (toilet.male_disabled_toilet_count > 0) {
            addTextView("남성 장애인용 화장실")
        }

        if (toilet.male_disabled_urinal_count > 0) {
            addTextView("남성 장애인용 소변기")
        }

        if (toilet.female_disabled_toilet_count > 0) {
            addTextView("여성 장애인용 화장실")
        }

    }

    private fun addToiletInfo(container: ViewGroup, optionName: String, optionValue: Int?) {
        if (optionValue != null && optionValue > 0) {
            val itemView = layoutInflater.inflate(R.layout.fragment_toilet_detail_content, container, false)
            itemView.findViewById<TextView>(R.id.toilet_detail_option_name).text = optionName
            itemView.findViewById<TextView>(R.id.toilet_detail_option_num).text = optionValue.toString()
            container.addView(itemView)
        }
    }

    //option scrollView 추가
    private fun addTextView(optionText: String) {
        // fragment_filter_option을 inflate
        val optionView = layoutInflater.inflate(R.layout.fragment_filter_option, null)

        // optionTextView의 텍스트를 설정
        val optionViewText = optionView.findViewById<TextView>(R.id.optionTextView)
        optionViewText.text = optionText

        // scrollViewContainer에 추가
        scrollViewContainer.addView(optionView)
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
