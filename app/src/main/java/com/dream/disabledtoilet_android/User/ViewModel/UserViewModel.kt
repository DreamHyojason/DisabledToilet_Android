package com.dream.disabledtoilet_android.User.ViewModel

import User
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.dream.disabledtoilet_android.User.UserRepository
import android.util.Log

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository()
    val currentUser = MutableLiveData<User?>()

    init {
        // currentUser의 변경을 감지하여 Firebase에 업데이트
        currentUser.observeForever { user ->
            user?.email?.let { email ->
                Log.d("test", "userviewmodel 변동 : ${currentUser}")
                repository.uploadFirebase(email, user)
            }
        }
    }

    fun fetchUserByEmail(email: String) {
        repository.loadUser(email) { user ->
            Log.d("UserViewModel", "fetchUserByEmail 반환값: $user")
            currentUser.value = user
        }
    }


    fun addLikeUser(toiletId : Int, userId : String){
        repository.addLike(toiletId, userId)
    }

    fun removeLikeUser(toiletId : Int, userId : String){
        repository.removeLike(toiletId, userId)
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel이 소멸되기 전에 사용자 정보를 업데이트
        currentUser.value?.let { user ->
            user.email?.let { email ->
                repository.uploadFirebase(email, user)
            }
        }
    }
}
