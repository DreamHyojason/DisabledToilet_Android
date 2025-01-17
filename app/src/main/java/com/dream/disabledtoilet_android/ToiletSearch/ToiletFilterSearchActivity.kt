package com.dream.disabledtoilet_android.ToiletSearch

import ToiletModel
import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.tasks.await
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.ToiletSearch.Adapter.ToiletListViewAdapter
import com.dream.disabledtoilet_android.ToiletSearch.SearchFilter.FilterSearchDialog
import com.dream.disabledtoilet_android.ToiletSearch.SearchFilter.ViewModel.FilterStatus
import com.dream.disabledtoilet_android.ToiletSearch.ViewModel.SortViewModel
import com.dream.disabledtoilet_android.ToiletSearch.ViewModel.FilterDialogStatus
import com.dream.disabledtoilet_android.ToiletSearch.ViewModel.ToiletSearchViewModel
import com.dream.disabledtoilet_android.Utility.Dialog.dialog.SortDialog
import com.dream.disabledtoilet_android.Utility.Dialog.dialog.LoadingDialog
import com.dream.disabledtoilet_android.databinding.ActivityToiletFilterSearchBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class ToiletFilterSearchActivity : AppCompatActivity() {

    lateinit var binding: ActivityToiletFilterSearchBinding
    val toiletRepository = ToiletRepository()
    lateinit var toiletListViewAdapter: ToiletListViewAdapter
    val loadingDialog = LoadingDialog()
    /**
     * 필터 적용 다이얼로그에서 사용할 뷰모델
     */
    lateinit var viewModel: ToiletSearchViewModel

    /**
     * 정렬 기준
     */
    var sortDialog = SortDialog()
    lateinit var sortViewModel: SortViewModel

    private lateinit var fabScrollToTop: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToiletFilterSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 정렬 적용 다이얼로그에서 사용할 뷰모델
        sortViewModel = ViewModelProvider(this)[SortViewModel::class.java]
        // 뷰모델 세팅
        viewModel = ViewModelProvider(this)[ToiletSearchViewModel::class.java]

        // 혹시 화장실 리스트가 캐시되지 않았을 경우
        if (!ToiletData.toiletListInit) {
            Log.d("test log", "ToiletData toiletList 캐시 안됨")
            // 비어있는 리스트 생성
            val toiletList = mutableListOf<ToiletModel>()
            viewModel.setCachedToiletList(toiletList)
        } else {
            Log.d("test log", "ToiletData toiletList 캐시 됨")
            // 전처리 한번 해서 toiletList 생성
            val toiletList = removeEmptyData(ToiletData.cachedToiletList!!.toMutableList())
            viewModel.setCachedToiletList(toiletList)
            viewModel.setFilteredToiletList(toiletList)
        }

        // 권한이 있으면 바로 UI 세팅
        if (getLocationPermission()) {
            setUi()
        }

        //RecyclerView 맨 위로 스크롤
        fabScrollToTop = findViewById(R.id.fab_scroll_to_top)
        fabScrollToTop.setOnClickListener {
            binding.toiletRecyclerView.smoothScrollToPosition(0)
        }
        //애니메이션 시작
        startBounceAnimation()
    }

    /**
     * UI 세팅
     */
    private fun setUi() {
        // 로딩 띄우기
        loadingDialog.show(supportFragmentManager, loadingDialog.tag)
        // 리사이클러뷰 어댑터는 비동기로 세팅
        CoroutineScope(Dispatchers.IO).launch {
            val userLocation: LatLng? = getUserLocation()

            //사용자 위치 기반 거리 업데이트
            if (userLocation != null) {
                toiletRepository.updateDistance(userLocation)
            }

            // 어댑터 세팅, 초기 리스트 UI에 띄우기
            toiletListViewAdapter = ToiletListViewAdapter(this@ToiletFilterSearchActivity, userLocation)
            Log.d("test log", "toiletListViewAdapter 생성")
            toiletListViewAdapter.updateList(
                viewModel.getSearchedToiletList(),
                sortViewModel.SortCheck
            )

            // 필터 적용 시, 옵저빙 -> 여기서 리사이클러뷰 업데이트
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED){
                    viewModel.toiletListState.collect{
                        toiletListViewAdapter.updateList(
                            viewModel.getSearchedToiletList(),
                            sortViewModel.SortCheck
                        )
                    }
                }
            }

            // 메인 스레드에서
            withContext(Dispatchers.Main) {
                // 어댑터 초기화
                binding.toiletRecyclerView.layoutManager =
                    LinearLayoutManager(this@ToiletFilterSearchActivity, LinearLayoutManager.VERTICAL, false)
                binding.toiletRecyclerView.adapter = toiletListViewAdapter
                //초기화 후 바로 리사이클러뷰 업데이트 리스너 세틷
                setSearchKeyWordListener()
            }

            // 로딩화면 dismiss
            loadingDialog.dismiss()
        }


        // 버튼들 리스너
        setButtonsListener()
        // 정렬 적용 다이얼로그 사라지면 필터 적용
        setSortDialogObserver()
    }

    /**
     * UI상의 버튼 리스너 세팅
     */
    fun setButtonsListener() {
        binding.filterButton.setOnClickListener {
            showFilter()
        }
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
        binding.filter.setOnClickListener {
            showSortDialog()
        }
        binding.toggle.setOnClickListener {
            showSortDialog()
        }
    }

    /**
     * ToiletRepository에 getToiletWithSearchKeyowrd 호출
     * query 변수 이용
     * 검색데이터 존재 시, editText에서 값 받아옴
     * updatelist로 바로 화장실 검색결과 업데이트
     */
    fun setSearchKeyWordListener() {
        // 검색창에 textChange 리스너 세팅
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // 혹시 나중에 필요하면 추가
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                viewModel.setQuery(binding.searchBar.text.toString())
                toiletListViewAdapter.updateList(
                    viewModel.getSearchedToiletList(),
                    sortViewModel.SortCheck
                )
            }

            override fun afterTextChanged(p0: Editable?) {
                // 혹시 나중에 필요하면 추가
            }
        })
    }

    /**
     * sortDialog show
     */
    private fun showSortDialog() {
        sortDialog = SortDialog.newInstance()
        sortDialog.show(supportFragmentManager, loadingDialog.tag)
        sortViewModel.isDialogDismissed.value = false
    }

    /**
     * 정렬 검색 다이얼로그 UI 표출 옵저버
     */
    fun setSortDialogObserver() {
        sortViewModel.isDialogDismissed.observe(this) { isDismissed ->
            if (isDismissed) {
                applySort()
            }
        }
        sortViewModel.SortCheck.observe(this) { sortCheckValue ->
            binding.filter.text = when (sortCheckValue) {
                0 -> "거리 순"
                1 -> "저장 순"
                else -> "거리 순"
            }
        }
    }

    /**
     * 정렬 조건
     */
    fun applySort() {
        applyFilter()
    }

    /**
     * 필터 다이얼로그 생성
     */
    private fun showFilter() {
        // 필터 다이얼로그, 리스너 세팅
        val filterSearchDialog: FilterSearchDialog = FilterSearchDialog(
            viewModel.filterDialogStatus.value.filterStatus,
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
                }
            }
        )
        filterSearchDialog.show(supportFragmentManager, filterSearchDialog.tag)
    }

    /**
     * 조건 적용 다이얼로그 꺼지면 실행되는 함수
     * 다이얼로그 꺼지면 필터내용 바로 적용
     */
    fun applyFilter() {
        Log.d("test log", "[applyFilter]: dismissed")
        toiletListViewAdapter.updateList(
            viewModel.getSearchedToiletList(),
            sortViewModel.SortCheck
        )
    }

    /**
     * 비어있는 데이터는 삭제
     * 여기에 들어가야할 값은 현재 액티비티에 있는 toiletData
     */
    fun removeEmptyData(toiletList: MutableList<ToiletModel>): MutableList<ToiletModel> {
        for (i in toiletList.size - 1 downTo 0) {
            val toiletName = toiletList[i].restroom_name
            if (toiletName == "") {
                toiletList.removeAt(i)
            }
        }
        return toiletList
    }

    /**
     * 유저 위치 정보 받아 오기
     * 코루틴에서 비동기 처리
     */
    suspend fun getUserLocation(): LatLng? {
        var currentPosition: LatLng? = null
        // 권한부터 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("test userLocation", "LocationPermission Granted")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            try {
                // 로케이션 받아올때까지 await()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).await()
                //currentPosition 생성
                currentPosition = LatLng.from(location.latitude, location.longitude)
            } catch (e: Exception) {
                Log.e("test userLocation", "Failed to get location: ${e.message}")
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 권한 확인
            Log.e("test userLocation", "Location permission not granted")
        }
        Log.d("test userLocation", "현재 위치: $currentPosition")
        return currentPosition
    }

    /**
     * 권한 받기 실행 함수
     */
    fun getLocationPermission(): Boolean {
        var isGranted = false
        // 권한이 기존에 있는지 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 기존에 없으면 받음
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
        } else {
            isGranted = true
        }
        return isGranted
    }

    /**
     * 위치 권한 받았을 때 콜백
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1000 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 권한 승인
                    setUi()
                } else {
                    // 권한 미승인
                    onBackPressed()
                }
                return
            }
        }
    }

    /**
     * FloatigButton 공처럼 튕기는 Animation 효과
     */
    private fun startBounceAnimation() {
        val animator = ObjectAnimator.ofFloat(fabScrollToTop, "translationY", 0f, 20f, 0f)
        animator.duration = 600 // 애니메이션 지속 시간
        animator.interpolator = AccelerateDecelerateInterpolator() // 애니메이션 속도 조절
        animator.repeatCount = ObjectAnimator.INFINITE // 무한 반복
        animator.repeatMode = ObjectAnimator.REVERSE // 원래 위치로 돌아오는 모드
        animator.start() // 애니메이션 시작
    }
}