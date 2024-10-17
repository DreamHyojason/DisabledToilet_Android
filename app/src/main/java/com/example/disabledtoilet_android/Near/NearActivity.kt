package com.example.disabledtoilet_android.Near

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.disabledtoilet_android.DetailActivity
import com.example.disabledtoilet_android.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NearActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var kakaoMap: KakaoMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KakaoMapSdk.init(this, "ce27585c8cc7c468ac7c46901d87199d")
        setContentView(R.layout.activity_near)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 권한 체크 및 요청
        checkLocationPermission()

        // 뒤로 가기 버튼 설정
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initializeBottomSheet() {
        // detail_bottomsheet 레이아웃을 바텀시트로 사용
        val bottomSheetView = layoutInflater.inflate(R.layout.detail_bottomsheet, null)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(bottomSheetView)

        // 배경을 투명하게 설정
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // BottomSheetBehavior를 통해 슬라이드 가능하도록 설정
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet!!)

        // BottomSheetDialog 표시
        bottomSheetDialog.show()

        // GestureDetector 설정
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // 아래에서 위로 스크롤하는 경우
                if (e1 != null && e2.y < e1.y) {
                    // BottomSheet를 위로 움직이는 애니메이션
                    bottomSheet.animate()
                        .translationY(-bottomSheet.height.toFloat())
                        .setDuration(300)
                        .withEndAction {
                            // 애니메이션이 끝난 후 DetailActivity로 이동
                            val intent = Intent(this@NearActivity, DetailActivity::class.java)
                            startActivity(intent)
                            bottomSheetDialog.dismiss()  // DetailActivity로 이동 시 다이얼로그 닫기
                        }
                    return true
                }
                return false
            }
        })

        // BottomSheet 터치 이벤트 처리
        bottomSheet.setOnTouchListener { v, event ->
            // GestureDetector 이벤트 처리
            gestureDetector.onTouchEvent(event)
            false
        }

        // 더보기 버튼 클릭 시 DetailActivity 실행
        val moreButton: TextView = bottomSheetView.findViewById(R.id.more_button)
        moreButton.setOnClickListener {
            val intent = Intent(this@NearActivity, DetailActivity::class.java)
            startActivity(intent)
            bottomSheetDialog.dismiss()  // DetailActivity로 이동 시 다이얼로그 닫기
        }
    }


    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            initializeMap()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                initializeMap()
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeMap() {
        mapView = findViewById(R.id.map_view)

        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("NearActivity", "MapView destroyed")
            }

            override fun onMapError(error: Exception) {
                Log.e("NearActivity", "Map error: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                Log.d("NearActivity", "KakaoMap is ready")

                // 현재 위치를 중심으로 지도를 이동
                setMapToCurrentLocation()
            }
        })
    }

    private fun setMapToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 현재 위치 가져오기
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    location!!.let {
                        // 현재 위치를 기반으로 지도의 중심을 설정
                        val startPosition = LatLng.from(it.latitude, it.longitude)

                        // 지도 중심 설정 및 줌 레벨 설정
                        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(startPosition, 16))

                        addMarkerToMap(startPosition)

                    }
                }
                .addOnFailureListener {
                    // 위치 가져오기 실패 시 처리
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addMarkerToMap(position: LatLng) {
        // 1. LabelStyles 생성하기 - Icon 이미지 하나만 있는 스타일
        val styles = kakaoMap.labelManager
            ?.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.saved_star_icon))
            )

        // 2. LabelOptions 생성하기
        val options = LabelOptions.from(position)
            .setStyles(styles)
            .setClickable(true)

        // 3. LabelLayer 가져오기 (또는 커스텀 Layer 생성)
        val layer = kakaoMap.labelManager?.layer

        // 4. LabelLayer에 LabelOptions을 넣어 Label 생성하기
        val label = layer?.addLabel(options)


        // 5. Label 클릭 이벤트 처리
        kakaoMap.setOnLabelClickListener { kakaoMap, layer, clickedLabel ->
            if (clickedLabel == label) {
                initializeBottomSheet()
                true
            } else {
                false  // 다른 이벤트 리스너로 이벤트 전달
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
