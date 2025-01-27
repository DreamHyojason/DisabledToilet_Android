package com.dream.disabledtoilet_android.Utility.KaKaoAPI.Model

/**
 * Response 받는 Data Class
 */
data class KakaoApiResponse<T>(
    val documents: List<T>,
    val meta: Meta
)
data class SearchResultDocument(
    val id: String,                      // 장소 ID
    val place_name: String,              // 장소명, 업체명
    val category_name: String,           // 카테고리 이름
    val category_group_code: String,     // 중요 카테고리만 그룹핑한 카테고리 그룹 코드
    val category_group_name: String,     // 중요 카테고리만 그룹핑한 카테고리 그룹명
    val phone: String,                   // 전화번호
    val address_name: String,            // 전체 지번 주소
    val road_address_name: String,       // 전체 도로명 주소
    val x: String,                       // X 좌표값 (경도)
    val y: String,                       // Y 좌표값 (위도)
    val place_url: String,               // 장소 상세페이지 URL
    val distance: String?
)
/**
 * 주소 내용 받는 Data Class
 */
data class AddressDocument(
    val address: Address?,
    val road_address: RoadAddress?
)
/**
 * 주소 Data Class
 */
data class Address(
    val address_name: String,
    val region_1depth_name: String,
    val region_2depth_name: String,
    val region_3depth_name: String,
    val mountain_yn: String,
    val main_address_no: String,
    val sub_address_no: String
)
/**
 * 도로명 주소
 */
data class RoadAddress(
    val address_name: String,
    val region_1depth_name: String,
    val region_2depth_name: String,
    val region_3depth_name: String,
    val road_name: String,
    val underground_yn: String,
    val main_building_no: String,
    val sub_building_no: String,
    val building_name: String,
    val zone_no: String
)
/**
 * 도로명, 지번 주소 모두 넣는 모델
 */
data class AddressNameModel(
    val roadAddressName: String,
    val lotAddressName: String
)
/**
 * API 메타데이터
 */
data class Meta(
    val total_count: Int
)