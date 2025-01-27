package com.dream.disabledtoilet_android.ToiletSearch

import ToiletModel
import User
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dream.disabledtoilet_android.BuildConfig
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData.currentUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ToiletData {
    private val TAG = "[ToiletData]"

    var toiletListInit = false
    // 전체 화장실 리스트 (불변)
    var cachedToiletList: List<ToiletModel> = listOf()

    //좋아요가 변동된 화장실 리스트
    // 좋아요가 변동된 화장실 리스트
    val _updatedToilets = MutableLiveData<Map<Int, ToiletModel>>(mapOf())
    val updatedToilets: LiveData<Map<Int, ToiletModel>> get() = _updatedToilets

    //사용자
    var currentUser : String? = null

    // Firestore 인스턴스
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun initialize(): Boolean = suspendCoroutine { continuation ->
        GlobalScope.launch{
            withContext(Dispatchers.Default){
                // Firestore에서 데이터 로드
                firestore.collection("dreamhyoja") // "dreamhyoja" 컬렉션에서 데이터 가져오기
                    .get()
                    .addOnSuccessListener { documents ->
                        GlobalScope.launch {
                            withContext(Dispatchers.IO){
                                // ToiletModel로 변환하여 cachedToiletList에 저장
                                cachedToiletList = documents.mapNotNull { doc ->
                                    ToiletModel.fromDocument(doc) // null이 아닌 경우만 포함
                                }
                                toiletListInit = true
                                continuation.resume(true)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error loading data: ${exception.message}")
                        continuation.resume(false)
                    }
            }
        }
    }
    /**
     * 특정 화장실의 LiveData 관찰 함수
     */
    fun observeToilet(toiletId: Int): LiveData<ToiletModel?> {
        val specificToiletLiveData = MutableLiveData<ToiletModel?>()

        // updatedToilets LiveData를 관찰하여 특정 화장실의 변화만 필터링
        updatedToilets.observeForever { updatedMap ->
            specificToiletLiveData.value = updatedMap[toiletId]
        }

        return specificToiletLiveData
    }

    fun getToiletAllData() : List<ToiletModel>? {
        return ToiletData.cachedToiletList
    }


    fun getCurretnUser() : String?{
        return currentUser
    }

    /**
     * 로그아웃 -> 저장된 사용자 데이터 삭제
     */
    fun clearCurrentUser(){
        currentUser = null
    }
}
