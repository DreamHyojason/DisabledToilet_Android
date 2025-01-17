package com.dream.disabledtoilet_android.Near.DataLayer

import ToiletModel
import User
import android.icu.number.Scale
import androidx.compose.animation.scaleIn
import androidx.lifecycle.MutableLiveData
import com.android.tools.build.jetifier.core.utils.Log
import com.dream.disabledtoilet_android.R
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.dream.disabledtoilet_android.User.UserRepository
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition

/**
 *      카카오맵 레이블
 */
class LabelBuilder(val kakaoMap: KakaoMap) {
    private var toiletLabelMap = mutableMapOf<Label, ToiletModel>()
    private val repository = UserRepository()

    /**
     *      ToiletModel 리스트를 기반으로 Label 리스트 생성
     */
    fun makeToiletLabelList(toiletList: List<ToiletModel>): List<Label> {
        val toiletLabelList = mutableListOf<Label>()
        if(toiletList.isNotEmpty()){
            for (i in toiletList.indices){
                val toiletLabel = makeToiletLabel(toiletList.get(i))
                if (toiletLabel != null){
                    toiletLabelList.add(toiletLabel)
                    toiletLabelMap.put(toiletLabel, toiletList.get(i))
                }
            }
        }
        else {
            Log.d("test log: ToiletLabelBuilder", "toiletList is empty")
        }

        return toiletLabelList.toList()
    }
    /**
     *      Label, ToiletModel 맵 반환
     *      반환 후, toiletLabelMap 초기화
     */
    fun getToiletLabelMap(): MutableMap<Label, ToiletModel> {
        val map = toiletLabelMap
        // 항상 get 이용하면 초기화
        resetToiletLabelMap()
        return map
    }
    /**
     *      맵에 화장실 레이블 생성
     */
    fun makeToiletLabel(toiletModel: ToiletModel): Label? {
//        // 사용자 정보를 비동기적으로 가져오기
//        val user = ToiletData.currentUser?.let {
//            repository.loadUser(it)
//        }
//
//        // 좋아요 여부 확인
//        val isLiked = user?.likedToilets?.contains(toiletModel.number.toString())
        val isLiked = ToiletData.cachedToiletList.contains(toiletModel)

        // 좋아요 스타일 설정
        val styles = if (isLiked == true) {
            kakaoMap.labelManager?.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(R.drawable.saved_pin1).setZoomLevel(10),
                    LabelStyle.from(R.drawable.saved_pin2).setZoomLevel(13),
                    LabelStyle.from(R.drawable.saved_pin3).setZoomLevel(16),
                    LabelStyle.from(R.drawable.saved_pin4).setZoomLevel(19)
                )
            )
        } else {
            kakaoMap.labelManager?.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(R.drawable.map_pin1).setZoomLevel(10),
                    LabelStyle.from(R.drawable.map_pin2).setZoomLevel(13),
                    LabelStyle.from(R.drawable.map_pin3).setZoomLevel(16),
                    LabelStyle.from(R.drawable.map_pin4).setZoomLevel(19)
                )
            )
        }

        val position = LatLng.from(toiletModel.wgs84_latitude, toiletModel.wgs84_longitude)
        val options = LabelOptions.from(position)
            .setStyles(styles)
            .setClickable(true)

        val layer = kakaoMap.labelManager?.layer
        val label = layer?.addLabel(options)
        return label
    }

    /**
     *      toiletLabelMap 초기화
     */
    private fun resetToiletLabelMap(){
        toiletLabelMap = mutableMapOf<Label, ToiletModel>()
    }
}