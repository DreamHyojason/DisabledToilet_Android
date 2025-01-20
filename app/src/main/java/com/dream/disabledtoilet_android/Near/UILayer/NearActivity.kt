package com.dream.disabledtoilet_android.Near.UILayer

import ShowAddGroupDialog
import ToiletModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dream.disabledtoilet_android.BuildConfig
import com.dream.disabledtoilet_android.Detail.DetailPageActivity
import com.dream.disabledtoilet_android.Map.MapManager
import com.dream.disabledtoilet_android.Near.UILayer.ViewModel.NearViewModel
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.ToiletSearch.FilterApplyListener
import com.dream.disabledtoilet_android.ToiletSearch.SearchFilter.FilterSearchDialog
import com.dream.disabledtoilet_android.ToiletSearch.SearchFilter.ViewModel.FilterStatus
import com.dream.disabledtoilet_android.ToiletSearch.SearchFilter.ViewModel.FilterViewModel
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.dream.disabledtoilet_android.ToiletSearch.ViewModel.FilterDialogStatus
import com.dream.disabledtoilet_android.User.ToiletPostViewModel
import com.dream.disabledtoilet_android.User.UserRepository
import com.dream.disabledtoilet_android.User.ViewModel.UserViewModel
import com.dream.disabledtoilet_android.Utility.Dialog.dialog.LoadingDialog
import com.dream.disabledtoilet_android.Utility.Dialog.utils.KakaoShareHelper
import com.dream.disabledtoilet_android.databinding.ActivityNearBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Exception
import kotlin.coroutines.resume

@RequiresApi(Build.VERSION_CODES.O)
class NearActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNearBinding
    private val loadingDialog = LoadingDialog()
    private lateinit var kakaoMap: KakaoMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: NearViewModel
    lateinit var filterSearchDialog: FilterSearchDialog

    private lateinit var userViewModel: UserViewModel
    private lateinit var postViewModel: ToiletPostViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNearBinding.inflate(layoutInflater)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_SCHEME)
        setContentView(binding.root)

        // 뷰모델 받기
        viewModel = ViewModelProvider(this).get(NearViewModel::class.java)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        postViewModel = ViewModelProvider(this).get(ToiletPostViewModel::class.java)

        val email = ToiletData.currentUser
        if (email != null) {
            lifecycleScope.launch {
                userViewModel.fetchUserByEmail(email)
            }
        }

        // 임시
        viewModel.setFilter()
        // 사용자 위치
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 뒤로 가기
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        // 현재 위치 이동
        binding.mapReturnCurPosBtn.setOnClickListener {
            moveCameraToUser()
        }
        // 조건 적용 버튼
        binding.filterButtonNear.setOnClickListener {
            showFilter()
        }

        // 맵뷰 초기화
        binding.mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    Log.d("test log: Map", "onMapDestroy")
                    viewModel.setIsMapInit(false)
                }

                override fun onMapError(p0: Exception?) {
                    Log.e("test log: Map", "onMapError")
                    viewModel.setIsMapInit(false)
                }
            }, object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    Log.d("test map", "onMapReady")
                    kakaoMap = map
                    viewModel.setIsMapInit(true)
                }
            }
        )
        
        // 맵뷰 초기화 관측 시
        viewModel.isMapInit.observe(this) { state ->
            // 맵뷰 초기화 됐으면
            if (viewModel.isMapInit.value == true) {
                Log.d("test log", "isMapInit: $state observed")

                lifecycleScope.launch {
                    // 현재 위치 받아서 뷰모델에 넣기
                    viewModel.setMyLocation(getMyLocation())
                    // 현재 위치로 카메라 이동
                    handleIntent(kakaoMap)
                }

                // 카메라 이동 끝 감지 리스너
                kakaoMap.setOnCameraMoveEndListener { kakaoMap, cameraPosition, gestureType ->
                    // 이동 감지되면 카메라 포지션 뷰모델에 세팅
                    viewModel.setCurrentCameraPosition(cameraPosition)
                }

                // 카메라 이동 시작 리스너
                kakaoMap.setOnCameraMoveStartListener { kakaoMap, gestureType ->
                    Log.d("test log", kakaoMap.toString() + gestureType.toString())
                }
                // 카카오맵 클릭 리스너
                kakaoMap.setOnLabelClickListener { _, _, clickedLabel ->
                    val labelToToiletMap = viewModel.mapState.value!!.toiletLabelMap
                    // 맵을 통해서 레이블의 화장실 찾기
                    val toilet = labelToToiletMap[clickedLabel]
                    if (toilet != null) {
                        viewModel.setBottomSheetStatus(clickedLabel, toilet)
                        // 바텀시트 생성
                        initBottomSheet(toilet, clickedLabel)
                        true
                    } else {
                        false
                    }
                }

                // 뷰모델의 카메라 포지션 값 변동 시
                viewModel.cameraPosition.observe(this) { cameraPosition ->
                    // 카메라 포지션 기준 20kM 내의 화장실 레이블 받아서
                    val labelsInCamera = viewModel.getToiletLabelListInCamera(kakaoMap)
                    // 화장실 레이블 지도에 표시
                    showLabelList(labelsInCamera)
                }


                // 사용자 위치 변경 관측
                viewModel.myLocation.observe(this) { myLocation ->
                    // 현재 위치 레이블 업데이트
                    updateMyLocationLabel(myLocation)
                }

                // cachedToilet 변경 관측 시
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED){
                        viewModel.cachedToiletStateFlow.collect{
                            //레이블 다시 깔기
                            val labelsInCamera = viewModel.getToiletLabelListInCamera(kakaoMap)
                            // 화장실 레이블 지도에 표시
                            showLabelList(labelsInCamera)
                        }
                    }
                }
            }
        }
    }

    /**
     *      로딩 다이얼로그 띄우기
     */
    private fun showLoading() {
        if (!loadingDialog.isAdded) {
            loadingDialog.show(supportFragmentManager, "loading_dialog")
        }
    }

    /**
     *      로딩 다이얼로그 없애기
     */
    private fun dismissLoading() {
        if (loadingDialog.isAdded) {
            loadingDialog.dismiss()
        }
    }

    /**
     *      현재 위치 받아오기
     */
    private suspend fun getMyLocation(): LatLng {
        var currentPosition = LatLng.from(0.0, 0.0)
        // 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("test permission", "권한 없음")
            return currentPosition // 권한이 없으면 기본값 반환
        }

        // 위치 요청을 위한 코루틴
        currentPosition = suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    currentPosition = LatLng.from(latitude, longitude)
                    Log.d("test location", "현재 위치: $currentPosition")
                    continuation.resume(currentPosition) // 위치 정보가 업데이트되면 코루틴 재개
                } else {
                    Log.d("test log", "위치 정보를 가져올 수 없습니다.")
                    continuation.resume(currentPosition) // 위치 정보가 없을 경우 기본값으로 재개
                }
            }
        }

        return currentPosition
    }

    /**
     *      파라미터 위치로 카메라 이동
     */
    private fun moveCamera(cameraUpdate: CameraUpdate, cameraAnimation: CameraAnimation) {
        Log.d("test mapCamera", "카메라 이동")
        kakaoMap.moveCamera(cameraUpdate, cameraAnimation)
    }

    /**
     *      Label 리스트의 Label 지도에 띄우기
     */
    private fun showLabelList(labelList: List<Label>) {
        var count = 0
        // 이미 떠있는 레이블 중, 현재 레이블 리스트와 안겹치는 레이블 숨기기
        removeDifferentLabel(labelList)
        for (i in labelList.indices) {
            labelList.get(i).show()
            count++
        }
        // 현재 떠있는 레이블 리스트 뷰모델에 세팅
        viewModel.setToiletInCameraList(labelList)
        Log.d("test map", "지도에 띄워진 화장실 수: $count")
    }

    /**
     *      안 겹치는 Label 리스트 지도에서 제거
     */
    private fun removeDifferentLabel(newLabelList: List<Label>) {
        val oldLabelList = viewModel.mapState.value!!.toiletInCameraList
        for (label in oldLabelList) {
            if (!newLabelList.contains(label)) {
                label.hide()
            }
        }
    }

    /**
     *      내 위치 띄우기
     */
    private fun updateMyLocationLabel(position: LatLng) {
        val style = kakaoMap.labelManager?.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(R.drawable.cur2)
            )
        )

        val options = LabelOptions.from(position)
            .setStyles(style)
            .setClickable(true)

        val myLocationLabel = kakaoMap.labelManager?.layer?.addLabel(options)
    }

    /**
     *      사용자 위치로 카메라 옮기기
     */
    private fun moveCameraToUser() {
        // 현재 위치로 카메라 이동
        val cameraUpdate = CameraUpdateFactory.newCenterPosition(viewModel.myLocation.value, 17)
        val cameraAnimation = CameraAnimation.from(100, true, true)
        moveCamera(cameraUpdate, cameraAnimation)
    }

    /**
     *      BottomSheet 생성
     */
    private fun initBottomSheet(toilet: ToiletModel, label: Label) {
        // 카메라 화장실 위치로 이동
        val position = LatLng.from(toilet.wgs84_latitude, toilet.wgs84_longitude)
        val cameraUpdate = CameraUpdateFactory.newCenterPosition(position, 17)
        val cameraAnimation = CameraAnimation.from(100, true, true)
        moveCamera(cameraUpdate, cameraAnimation)


        // 바텀시트 뷰 생성
        val bottomSheetView = this.layoutInflater.inflate(R.layout.detail_bottomsheet, null)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(bottomSheetView)

        setBottomSheetUI(bottomSheetView, toilet)
        bottomSheetDialog.show()
    }

    /**
     *      BottomSheet UI 세팅
     */
    private fun setBottomSheetUI(bottomSheetView: View, toilet: ToiletModel) {
        val toiletName: TextView = bottomSheetView.findViewById(R.id.toilet_name)
        val toiletAddress: TextView = bottomSheetView.findViewById(R.id.toilet_address)
        val toiletOpeningHours: TextView = bottomSheetView.findViewById(R.id.toilet_opening_hours)
        val saveCount: TextView = bottomSheetView.findViewById(R.id.toilet_save_count1)
        val calDis: TextView = bottomSheetView.findViewById(R.id.toilet_distance)

        toiletName.text = toilet.restroom_name
        toiletAddress.text = toilet.address_road ?: "-"
        toiletOpeningHours.text = toilet.opening_hours ?: "-"
        saveCount.text = "저장 (${toilet.save})"
        calDis.text = viewModel.bottomSheetStatus.value!!.distanceString

        // 상세 페이지로 이동
        bottomSheetView.findViewById<TextView>(R.id.more_button).setOnClickListener {
            val intent = Intent(this, DetailPageActivity::class.java)
            intent.putExtra("TOILET_DATA", toilet)
            startActivity(intent)
        }

        // 네비게이션 버튼
        bottomSheetView.findViewById<LinearLayout>(R.id.toilet_navigation_btn).setOnClickListener {
            MapManager(this).showKakaoMap(toilet)
        }

        // 공유 버튼
        bottomSheetView.findViewById<LinearLayout>(R.id.share_btn).setOnClickListener {
            KakaoShareHelper(this).shareKakaoMap(toilet)
        }

        // 특정 화장실의 LiveData를 관찰
        ToiletData.observeToilet(toilet.number).observe(this) { updatedToilet ->
            updatedToilet?.let {
                saveCount.text = "저장 (${it.save})"
            }
        }


        // 사용자 정보 관찰
        userViewModel.currentUser.observe(this) { user ->
            if (user != null) {
                //로그인 상태
                setupPostInteraction(bottomSheetView, toilet.number, user.email)
            } else {
                //로그아웃 상태이면 로그인 시도로
                Log.d("test ", "user : ${user}")
            }
        }
    }

    private fun setupPostInteraction(bottomSheetView: View, toiletId: Int, userId: String) {
        postViewModel.observePostLikes(toiletId)

        //좋아요 버튼 클릭 이벤트
        val savebtn1: LinearLayout = bottomSheetView.findViewById(R.id.save_btn1)
        val savebtn2: LinearLayout = bottomSheetView.findViewById(R.id.save_btn2)

        val saveicon1: ImageView = bottomSheetView.findViewById(R.id.save_icon1)
        val saveicon2: ImageView = bottomSheetView.findViewById(R.id.save_icon2)

        savebtn1.setOnClickListener {
            Log.d("test" , " 좋아요 클릭 : ${postViewModel.toiletLikes.value}")
            
            showLikeAddDialog()
//            Log.d("test" , " 삭제 : ${postViewModel.toiletLikes.value}")
//
//            val isLiked = postViewModel.isLikedByUser(userId)
//
//            if (isLiked) {
//                postViewModel.removeLike(toiletId, userId)
//                userViewModel.removeLikeUser(toiletId,userId)
//                Log.d("test" , " 삭제 : ${postViewModel.toiletLikes.value}")
//            }else{
//                postViewModel.addLike(toiletId, userId)
//                userViewModel.addLikeUser(toiletId, userId)
//                Log.d("test", " 추가 : ${postViewModel.toiletLikes.value}")
//            }
        }

        savebtn2.setOnClickListener {
            showLikeAddDialog()

//            val isLiked = postViewModel.isLikedByUser(userId)
//            if (isLiked) {
//                postViewModel.removeLike(toiletId, userId)
//                userViewModel.removeLikeUser(toiletId,userId)
//                Log.d("test" , " 삭제 : ${postViewModel.toiletLikes.value}")
//            }else{
//                postViewModel.addLike(toiletId, userId)
//                userViewModel.addLikeUser(toiletId, userId)
//                Log.d("test", " 추가 : ${postViewModel.toiletLikes.value}")
//            }
        }

        // 좋아요 실시간 업데이트 관찰
        postViewModel.toiletLikes.observe(this) { likes ->
            val likeCountTextView = bottomSheetView.findViewById<TextView>(R.id.toilet_save_count1)
            likeCountTextView.text = "저장 (${likes.size})"
            updateLikeButtonIcon(saveicon1, saveicon2, userId)
        }
    }

    /**
     * 좋아요 버튼 누를 때 -> 그룹 선택 or 추가할 지 창 띄우기
     */
    private fun showLikeAddDialog() {
        //bottomsheet로 fragment_addliked.xml이 보이게
        val bottomsheetDialog = ShowAddGroupDialog()
        bottomsheetDialog.show(supportFragmentManager, bottomsheetDialog.tag)
    }

    private fun updateLikeButtonIcon(saveBtn1: ImageView, saveBtn2: ImageView, userId: String) {
        val isLiked = postViewModel.isLikedByUser(userId)
        if (isLiked) {
            saveBtn1.setImageResource(R.drawable.saved_star_icon)
            saveBtn2.setImageResource(R.drawable.saved_star_icon)
        } else {
            saveBtn1.setImageResource(R.drawable.save_icon)
            saveBtn2.setImageResource(R.drawable.save_icon)
        }
    }

    /**
     *      인텐트 받아서 처리
     */
    private fun handleIntent(kakaoMap: KakaoMap) {
        val rootActivity = intent.getStringExtra("rootActivity")
        when (rootActivity) {
            // 장소 검색에서 넘어온 경우
            "ToiletFilterSearchActivity" -> {
                // 인텐트 받아오기
                val parcelableData = intent.getParcelableExtra<ToiletModel>("toiletData")
                if (parcelableData is ToiletModel) {
                    // 화장실 위치로 카메라 이동
                    val toilet = parcelableData
                    val position = LatLng.from(toilet.wgs84_latitude, toilet.wgs84_longitude)
                    val cameraUpdate = CameraUpdateFactory.newCenterPosition(position, 17)
                    val cameraAnimation = CameraAnimation.from(100, true, true)
                    moveCamera(cameraUpdate, cameraAnimation)
                    // 화장실 데이터 기반으로 레이블 생성
                    val toiletLabel = viewModel.makeLabel(toilet, kakaoMap)
                    viewModel.setBottomSheetStatus(toiletLabel, toilet)
                    // 바텀시트 생성
                    initBottomSheet(toilet, toiletLabel)
                } else {
                    Log.e("test log", "parcelable data type is not matched")
                }
            }

            else -> {
                // 현재 위치로 카메라 이동
                moveCameraToUser()
                // 현재 위치를 카메라 값을 세팅
                kakaoMap.getCameraPosition { cameraPosition ->
                    viewModel.setCurrentCameraPosition(cameraPosition)
                }
            }
        }
    }

    /**
     *      필터 다이얼로그 띄우기
     */
    private fun showFilter() {
        // 필터 다이얼로그, 리스너 세팅
        val filterSearchDialog = FilterSearchDialog(
            viewModel.filterDialogStatus.value!!.filterStatus,
            object : FilterApplyListener {
                override fun onApplyFilterListener(filterStatus: FilterStatus) {
                    viewModel.setFilterDialogStatus(
                        FilterDialogStatus(
                            true,
                            filterStatus
                        )
                    )
                }

                override fun onDialogDismissListener(isDismissed: Boolean) {
                    viewModel.setIsDialogDismissed(isDismissed)
                    val labelsInCamera = viewModel.getToiletLabelListInCamera(kakaoMap)
                    // 화장실 레이블 지도에 표시
                    showLabelList(labelsInCamera)
                }
            }
        )
        filterSearchDialog.show(supportFragmentManager, filterSearchDialog.tag)
    }

    override fun onResume() {
        super.onResume()

        val currentToilet = viewModel.currentToilet.value
        if (currentToilet == null) {
            Log.e("NearActivity", "Error: currentToilet is null in onResume")
            return
        }

        currentToilet.let { toilet ->
            Log.d("NearActivity", "Observing likes for toilet: ${toilet.number}")
            postViewModel.observePostLikes(toilet.number)
        }
    }

}