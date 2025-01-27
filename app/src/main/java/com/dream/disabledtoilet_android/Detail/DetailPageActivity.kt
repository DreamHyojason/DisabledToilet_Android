package com.dream.disabledtoilet_android.Detail

import com.kakao.vectormap.camera.CameraUpdateFactory
import ToiletModel
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.dream.disabledtoilet_android.BuildConfig
import com.dream.disabledtoilet_android.Map.MapManager
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.databinding.ActivityDetailBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var mapView: MapView
    private lateinit var kakaoMap: KakaoMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_SCHEME)

        setContentView(binding.root)

        // 뒤로 가기 버튼 설정
        val backButton = binding.backButton
        backButton.setOnClickListener {
            onBackPressed()
        }

        // 전달받은 화장실 데이터
        val toiletData = intent.getParcelableExtra<ToiletModel>("TOILET_DATA")


        //mapView 초기화
        CoroutineScope(Dispatchers.Main).launch{
            val mapInitialized = initializeMapView()

            if(mapInitialized){
                val position = LatLng.from(toiletData!!.wgs84_latitude, toiletData!!.wgs84_longitude)

                /**
                 * 레이블 추가
                 */
                val styles = kakaoMap.labelManager?.addLabelStyles(
                    LabelStyles.from(
                        LabelStyle.from(R.drawable.aim_icon3)
                    )
                )

                val options = LabelOptions.from(position)
                    .setStyles(styles)
                    .setClickable(true)

                val layer = kakaoMap.labelManager?.layer
                layer?.addLabel(options)

                //애니메이션 효과를 적용하면서 지도 이동
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position, 16), CameraAnimation.from(500, true, true))

            }
        }

        // DetailOptionFragment를 fragment_container에 추가
        if (savedInstanceState == null) {
            val fragment = DetailOptionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("TOILET_DATA", toiletData)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    /**
     * 지도 초기화
     */
    suspend fun initializeMapView(): Boolean {
        val isSuccess = CompletableDeferred<Boolean>()

        withContext(Dispatchers.Main) { // UI 스레드에서 실행
            mapView = binding.mapViewDetailpage
            mapView.start(object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    Log.d("MapManager", "MapView destroyed")
                }

                override fun onMapError(error: Exception) {
                    Log.e("MapManager", "Map error: ${error.message}")
                    isSuccess.completeExceptionally(error)
                }
            }, object : KakaoMapReadyCallback() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    Log.d("MapManager", "KakaoMap is ready")
                    isSuccess.complete(true)
                }
            })
        }

        return isSuccess.await()
    }
}
