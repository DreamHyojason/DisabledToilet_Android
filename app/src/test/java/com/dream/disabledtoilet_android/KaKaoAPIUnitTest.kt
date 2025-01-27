package com.dream.disabledtoilet_android

import com.dream.disabledtoilet_android.Utility.KaKaoAPI.KakaoApiRepository
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Test

class KaKaoAPIUnitTest {
    val testLocation = LatLng.from(37.5447087, 127.0744097)
    val jangHeung = LatLng.from(34.68021954868047, 126.90850255802565)

    @Test
    fun searchWithKeyword() = runTest{
        val kakaoRepository = KakaoApiRepository.KakaoLocalRepository()

        val query = "건대 입구"
        val result = kakaoRepository.searchWithKeyword(
            query = query,
            x = jangHeung.longitude.toString(),
            y = jangHeung.latitude.toString(),
            sort = "accuracy"
        ).getOrNull()

        val resultList = result?.documents
        val resultMeta = result?.meta
        if (resultList != null) {
            for (i in resultList.indices) {
                System.out.println(
                    resultList[i].toString() + "\n"
                )
            }
        } else {
            System.out.println("검색 결과가 없습니다.")
        }
        if (resultMeta != null) {
            System.out.println(resultMeta.toString())
        }


    }

    @Test
    fun getAddressByCoordinate() = runTest{
        val kakaoRepository = KakaoApiRepository.KakaoLocalRepository()

        System.out.println(kakaoRepository.getAddressFromCoordinate(testLocation.longitude,testLocation.latitude).toString() )
    }

}