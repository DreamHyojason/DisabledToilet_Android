package com.dream.disabledtoilet_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dream.disabledtoilet_android.ToiletSearch.ToiletData
import com.dream.disabledtoilet_android.Utility.Dialog.dialog.LoadingDialog
import com.dream.disabledtoilet_android.Utility.Dialog.utils.GoogleHelper
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.common.KakaoSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.activity.viewModels
import com.dream.disabledtoilet_android.User.ViewModel.UserViewModel

class StartActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var loadingDialog = LoadingDialog()
    private lateinit var googleHelper: GoogleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KakaoSdk.init(this, BuildConfig.KAKAO_SCHEME)

        firebaseAuth = FirebaseAuth.getInstance()
        googleHelper = GoogleHelper.getInstance(this)

        loadingDialog.show(supportFragmentManager, loadingDialog.tag)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 초기화 작업 (예: 데이터 로드)
                val initResult = initializeApp()

                // GoogleHelper 초기화
                googleHelper.initializeGoogleSignIn() // 구글 로그인 초기화

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss() // 로딩 다이얼로그 종료
                    if (initResult) {
                        // 로그인 상태 확인
                        val currentUser = ToiletData.getCurretnUser()

                        Log.d("test", "user : ${currentUser}")
                        if (currentUser != null) {
                            // 로그인 상태일 경우 MainActivity로 이동
                            startActivity(Intent(this@StartActivity, MainActivity::class.java))
                        } else {
                            // 비로그인 상태일 경우 NonloginActivity로 이동
                            startActivity(Intent(this@StartActivity, NonloginActivity::class.java))
                        }
                        finish() // 현재 액티비티 종료
                    } else {
                        // 초기화 실패 시 에러 처리 (필요시 추가)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss() // 로딩 다이얼로그 종료
                    // 에러 처리 (예: Toast 메시지 표시)
                    Toast.makeText(this@StartActivity, "Initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun initializeApp(): Boolean {
        // 초기화 작업 수행 (예: 데이터 로드)
        return try {
            ToiletData.initialize()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
