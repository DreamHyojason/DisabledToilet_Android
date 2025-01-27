package com.dream.disabledtoilet_android.Near.UILayer.ViewModel

import ToiletModel
import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dream.disabledtoilet_android.Near.DataLayer.LabelBuilder
import com.dream.disabledtoilet_android.Near.DataLayer.ToiletListGenerator
import com.dream.disabledtoilet_android.Near.DomainLayer.NearDomain
import com.dream.disabledtoilet_android.ToiletSearch.SearchFilter.ViewModel.FilterViewModel
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.dream.disabledtoilet_android.ToiletSearch.ToiletRepository
import com.dream.disabledtoilet_android.ToiletSearch.ViewModel.FilterDialogStatus
import com.dream.disabledtoilet_android.ToiletSearch.ViewModel.ToiletListState
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.label.Label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearViewModel: ViewModel() {
    val nearDomain = NearDomain()
    val toiletListGenerator = ToiletListGenerator()

    @RequiresApi(Build.VERSION_CODES.O)
    val toiletRepository = ToiletRepository()

    // 맵 상태
    private val _mapState = MutableLiveData<MapStatus>()
    val mapState: LiveData<MapStatus> get() = _mapState

    // UI 상태
    private val _uiState = MutableLiveData<UIStatus>()
    val uiState: LiveData<UIStatus> get() = _uiState

    // 지도 초기화 상태
    private val _isMapInit = MutableLiveData<Boolean>()
    val isMapInit: LiveData<Boolean> get() = _isMapInit

    // 카메라 상태
    private val _cameraPosition = MutableLiveData<CameraPosition>()
    val cameraPosition: LiveData<CameraPosition> get() = _cameraPosition

    // 내 위치
    private val _myLocation = MutableLiveData<LatLng>()
    val myLocation: LiveData<LatLng> get() = _myLocation

    // 바텀시트
    private val _bottomSheetStatus = MutableLiveData<BottomSheetStatus>()
    val bottomSheetStatus: LiveData<BottomSheetStatus> get() = _bottomSheetStatus

    // 현재 선택한 화장실
    private val _currentToilet = MutableLiveData<ToiletModel?>()
    val currentToilet : LiveData<ToiletModel?> get() = _currentToilet

    // 필터 다이얼로그 상태
    private val _filterDialogStatus = MutableLiveData(FilterDialogStatus())
    val filterDialogStatus : LiveData<FilterDialogStatus> get() = _filterDialogStatus

    private val _cachedToiletStateFlow = MutableStateFlow<List<ToiletModel>>(ToiletData.cachedToiletList)
    val cachedToiletStateFlow : MutableStateFlow<List<ToiletModel>> get() = _cachedToiletStateFlow

    init {
        val mapStatus = MapStatus(
            ToiletData.cachedToiletList!!,
            listOf(),
            ToiletData.cachedToiletList.isNullOrEmpty(),
            listOf(),
            listOf()
        )
        val uiStatus = UIStatus(
            false,
            false
        )
        _mapState.value = mapStatus
        _uiState.value = uiStatus

        _filterDialogStatus.value = FilterDialogStatus()
    }
    /**
     * 내 위치 세팅
     */
    fun setMyLocation(location: LatLng){
        Log.d("test log", "setMyLocation: $location")
        _myLocation.value = location
    }

    /**
     * 현재 선택된 화장실 세팅
     */
    fun setCurrentToilet(toilet: ToiletModel?){
        _currentToilet.value = toilet
    }

    /**
     * 현재 필터링 된 화장실 리스트 기반으로 레이블 리스트 생성
     * 지도 초기화 된 후 사용해야 함
     */
    fun makeToiletLabelList(kakaoMap: KakaoMap){
        Log.d("test log", "makeToiletLabelList")
        // 카카오맵 객체가 필요하기 때문에 지도 초기화 된 후 사용
        Log.d("test log", "makeToiletLabelList: kakaoMap initialized")
        val labelBuilder = LabelBuilder(kakaoMap)
        Log.d("test log",mapState.value!!.filteredToiletList.size.toString())
        _mapState.value = mapState.value?.copy(
            toiletLabelList = labelBuilder.makeToiletLabelList(
                mapState.value!!.filteredToiletList
            )
        )
    }
    /**
     * 카카오 맵이 세팅되면 isMapInit 상태 변경
     */
    fun setIsMapInit(status: Boolean){
        _isMapInit.value = status
    }
    /**
     * 필터 상태 적용
     */
    fun setFilter(){
        _mapState.value = mapState.value?.copy(
            filteredToiletList = ToiletData.cachedToiletList!!.toList()
        )
        Log.d("test log", "남은 화장실 수: ${mapState.value!!.filteredToiletList.size}")
    }
    /**
     * 현재 카메라 위치 기반으로 20km 이내의 화장실 레이블 리스트 받아오기
     */
    fun getToiletLabelListInCamera(kakaoMap: KakaoMap): List<Label>{
        val labelBuilder = LabelBuilder(kakaoMap)
        if (cameraPosition.value != null){
            val toiletListInCamera = toiletListGenerator.makeToiletListInCamera(
                cameraPosition.value!!,
                mapState.value!!.filteredToiletList
            )
            val resultList = labelBuilder.makeToiletLabelList(toiletListInCamera)
            // 레이블 리스트 생성 후, 맵도 받아오기
            _mapState.value = mapState.value!!.copy(toiletLabelMap = labelBuilder.getToiletLabelMap())
            return resultList
        }
        else{
            return emptyList()
        }
    }
    /**
     * 현재 카메라 위치 받기
     */
    fun setCurrentCameraPosition(cameraPosition: CameraPosition){
        _cameraPosition.value = cameraPosition
    }
    /**
     * 현재 떠있는 화장실 레이블 받기
     */
    fun setToiletInCameraList(labelList: List<Label>){
        _mapState.value = mapState.value?.copy(
            toiletInCameraList = labelList
        )
    }
    /**
     * 바텀 시트 UI 데이터 클래스 세팅
     */
    @SuppressLint("DefaultLocale")
    fun setBottomSheetStatus(label: Label, toilet: ToiletModel){
        // 내 위치와 화장실 위치 사이의 거리 구하기
        val myLocation = Location("").apply{
            latitude = myLocation.value!!.getLatitude()
            longitude = myLocation.value!!.getLongitude()
        }
        val toiletLocation = Location("").apply{
            latitude = toilet.wgs84_latitude
            longitude = toilet.wgs84_longitude
        }
        val distance = toiletListGenerator.calculateDistance(myLocation, toiletLocation)
        // 구한 위치 문자열 형식 맞추기
        val distanceString = when {
            distance < 1000 -> "${distance.toInt()}m"
            else -> String.format("%.1fkm", distance / 1000)
        }
        // status 세팅
        _bottomSheetStatus.value = BottomSheetStatus(
            toilet,
            label,
            distanceString
        )
    }
    /**
     * ToiletModel을 받아서 label 반환
     */
    fun makeLabel(toilet: ToiletModel, kakaoMap: KakaoMap): Label{
        val labelBuilder = LabelBuilder(kakaoMap)
        return labelBuilder.makeToiletLabel(toilet)!!
    }

    fun setFilterDialogStatus(filterDialogStatus: FilterDialogStatus){
        _filterDialogStatus.value = filterDialogStatus
        // 바로 필터링 된 화장실 리스트 세팅
        setFilteredToiletList(getFilteredToiletList())
    }

    private fun setFilteredToiletList(toiletList: List<ToiletModel>) {
        _mapState.value = mapState.value?.copy(
            filteredToiletList = toiletList
        )
    }

    @SuppressLint("NewApi")
    private fun getFilteredToiletList(): List<ToiletModel> {
        return toiletRepository.setFilteredToiletList(
            filterDialogStatus.value!!.filterStatus,
            mapState.value!!.toiletList.toList()
        )
    }

    fun setIsDialogDismissed(isDismissed: Boolean){
        _filterDialogStatus.value = FilterDialogStatus(
            isDismissed = isDismissed,
            // 필터 값은 그대로 유지
            filterStatus = _filterDialogStatus.value!!.filterStatus
        )
    }


}

data class MapStatus(
    val toiletList: List<ToiletModel>,
    val filteredToiletList: List<ToiletModel>,
    val isToiletListEmpty: Boolean,
    val toiletLabelList: List<Label>,
    val toiletInCameraList: List<Label>,
    val toiletLabelMap: MutableMap<Label, ToiletModel> = mutableMapOf()
)

data class UIStatus(
    val isLoading: Boolean,
    val isMapLoading: Boolean
)

data class BottomSheetStatus(
    val toilet: ToiletModel,
    val label: Label,
    var distanceString: String
)