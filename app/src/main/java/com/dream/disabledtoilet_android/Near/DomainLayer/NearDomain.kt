package com.dream.disabledtoilet_android.Near.DomainLayer

import ToiletModel
import com.dream.disabledtoilet_android.Near.DataLayer.LabelBuilder
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.label.Label

class NearDomain {
    /**
     *      ToiletModel 리스트 기반으로 Label 리스트 생성
     *      KakaoMap 객체 필요
     */
    suspend fun makeToiletLabelList(toiletList: List<ToiletModel>, kakaoMap: KakaoMap): List<Label>{
        val toiletLabelBuilder = LabelBuilder(kakaoMap)
        return toiletLabelBuilder.makeToiletLabelList(toiletList)
    }
}