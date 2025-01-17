package com.dream.disabledtoilet_android.User

import User
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Firebase에서 사용자 데이터를 가져와 User 데이터 형식으로 반환
     * @param email 사용자 이메일
     * @return User 객체 (null 가능)
     */
    suspend fun loadUser(email: String): User? {
        return try {
            val document = db.collection("users").document(email).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to load user: ${e.message}")
            null
        }
    }


    /**
     * Firebase에 사용자 데이터를 업데이트
     * @param email 사용자 이메일
     * @param updatedUser 업데이트할 User 데이터
     */
    fun uploadFirebase(email: String, updatedUser: User) {
        val userMap = hashMapOf(
            "email" to updatedUser.email,
            "name" to updatedUser.name,
            "photoURL" to updatedUser.photoURL,
            "likedToilets" to updatedUser.likedToilets.map { it.toString() },
            "registedToilets" to updatedUser.registedToilets
        )

        db.collection("users")
            .document(email)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("UserRepository", "User data successfully uploaded to Firebase.")
            }
            .addOnFailureListener { exception ->
                Log.e("UserRepository", "Failed to upload user data: ${exception.message}")
            }
    }

    /**
     * 좋아요 추가
     */
    fun addLike(toiletId: Int, userId: String) {
        val postRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likedToilets") as? MutableList<String> ?: mutableListOf()

            if (likes.contains(toiletId.toString())) {
                likes.add(toiletId.toString())
                transaction.update(postRef, "likedToilets", likes)
            }
        }
    }

    /**
     * 좋아요 삭제
     */
    fun removeLike(toiletId: Int, userId: String) {
        val postRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likedToilets") as? MutableList<String> ?: mutableListOf()

            if (likes.contains(toiletId.toString())) {
                likes.remove(toiletId.toString())
                transaction.update(postRef, "likedToilets", likes)
            }
        }
    }

    /**
     * 사용자 실시간 업데이트
     */
    fun observeUserLikes(userId: String, callback: (List<String>) -> Unit) {
        db.collection("users").document(userId.toString())
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val likes = snapshot.get("likedToilets") as? List<String> ?: emptyList()
                    Log.d("test", "update toilet : ${likes}")
                    callback(likes)
                }
            }
    }

}
