package com.dream.disabledtoilet_android.User

import android.util.Log
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.google.firebase.firestore.FirebaseFirestore

/**
 * 화장실 좋아요 추가 , 삭제 및 firebase 실시간 업데이트
 */
class ToiletPostRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * 좋아요 추가
     */
    fun addLike(toiletId: Int, userId: String) {
        val postRef = db.collection("dreamhyoja").document(toiletId.toString())
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("save") as? MutableList<String> ?: mutableListOf()
            if (!likes.contains(userId)) {
                likes.add(userId)
                transaction.update(postRef, "save", likes)
            }
        }.addOnSuccessListener {
            updateCachedToilet(toiletId)
        }
    }

    /**
     * 좋아요 제거
      */
    fun removeLike(toiletId: Int, userId: String) {
        val postRef = db.collection("dreamhyoja").document(toiletId.toString())
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("save") as? MutableList<String> ?: mutableListOf()
            if (likes.contains(userId)) {
                likes.remove(userId)
                transaction.update(postRef, "save", likes)
            }
        }.addOnSuccessListener {
            updateCachedToilet(toiletId)
        }
    }

    /**
     * 특정 화장실의 데이터를 Firestore에서 가져와서 cachedToiletList 업데이트
     */
    private fun updateCachedToilet(toiletId: Int) {
        val postRef = db.collection("dreamhyoja").document(toiletId.toString())
        postRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val updatedToilet = ToiletModel.fromDocument(document)
                // 기존 리스트에서 해당 화장실을 업데이트
                ToiletData.cachedToiletList = ToiletData.cachedToiletList?.map {
                    if (it.number == toiletId) updatedToilet else it
                } ?: listOf(updatedToilet)
                // 변경된 화장실 정보를 updatedToilets에 업데이트
                ToiletData._updatedToilets.value = ToiletData._updatedToilets.value?.plus(Pair(toiletId, updatedToilet))
            }
        }.addOnFailureListener { exception ->
            Log.e("ToiletPostRepository", "Error updating cached toilet: ${exception.message}")
        }
    }

    /**
     * 게시글 좋아요 실시간 업데이트
     */
    fun observePostLikes(toiletId: Int, callback: (List<String>) -> Unit) {
        db.collection("dreamhyoja").document(toiletId.toString())
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val likes = snapshot.get("save") as? List<String> ?: emptyList()
                    Log.d("test", "update toilet : ${likes}")
                    callback(likes)
                }
            }
    }
}