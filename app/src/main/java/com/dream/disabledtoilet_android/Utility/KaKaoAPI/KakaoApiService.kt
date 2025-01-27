package com.dream.disabledtoilet_android.Utility.KaKaoAPI

import com.dream.disabledtoilet_android.Utility.KaKaoAPI.Model.AddressDocument
import com.dream.disabledtoilet_android.Utility.KaKaoAPI.Model.KakaoApiResponse
import com.dream.disabledtoilet_android.Utility.KaKaoAPI.Model.SearchResultDocument
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 *  카카오 Rest Api
 */
interface KakaoApiService {
    /**
     * 좌표로 주소 얻기.
     * 예제: curl -v -G GET "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=127.423084873712&y=37.0789561558879&input_coord=WGS84" \
     *   -H "Authorization: KakaoAK ${REST_API_KEY}"
     */
    @GET("v2/local/geo/coord2address.json")
    suspend fun getAddress(
        @Query("x") x: String,
        @Query("y") y: String,
        @Query("input_coord") inputCoord: String = "WGS84",
        @Header("Authorization") authorization: String
    ): KakaoApiResponse<AddressDocument>

    @GET("v2/local/search/keyword.json")
    suspend fun searchWithKeyword(
        @Query("query") query: String,                          // 검색을 원하는 질의어
        @Query("x") x: String? = null,                          // 중심 좌표의 X 혹은 경도
        @Query("y") y: String? = null,                          // 중심 좌표의 Y 혹은 위도
        @Query("radius") radius: Int? = null,                  // 중심 좌표부터의 반경거리
        @Query("page") page: Int? = 1,                          // 결과 페이지 번호
        @Query("size") size: Int? = 15,                         // 한 페이지에 보여질 문서의 개수
        @Query("sort") sort: String? = "distance",              // 정렬
        @Header("Authorization") authorization: String           // 인증 헤더
    ) : KakaoApiResponse<SearchResultDocument>
}