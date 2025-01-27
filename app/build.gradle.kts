
import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// Kakao scheme을 읽어옵니다.
val kakaoScheme = localProperties.getProperty("KAKAO_SCHEME")
val kakaoRestAPI = localProperties.getProperty("KAKAO_REST_API")
val firebaseLink = localProperties.getProperty("FIREBASE")
val webClientId = localProperties.getProperty("WEB_CLIENT_ID")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.dream.disabledtoilet_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dream.disabledtoilet_android"
        minSdk = 24
        targetSdk = 34
        versionCode = 10
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["KAKAO_SCHEME"] = kakaoScheme
        buildConfigField("String", "KAKAO_SCHEME", "\"$kakaoScheme\"")
        buildConfigField("String", "KAKAO_REST_API", "\"$kakaoRestAPI\"")
        buildConfigField("String", "FIREBASE", "\"$firebaseLink\"")
        buildConfigField("String","WEB_CLIENT_ID", "\"$webClientId\"")
    }

    // 여기에 buildFeatures 블록 추가
    buildFeatures {
        buildConfig = true // BuildConfig 기능 활성화
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation ("com.kakao.sdk:v2-all:2.20.1") // 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation ("com.kakao.sdk:v2-navi:2.20.1") // 카카오내비 API 모듈
    implementation("com.google.android.material:material:1.9.0")
    implementation ("com.kakao.maps.open:android:2.11.9")

    implementation(libs.play.services.maps)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.compilercommon)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.media3.common.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database-ktx")

    implementation ("com.google.android.gms:play-services-auth:20.5.0")

    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // drawerlayout
    implementation ("androidx.drawerlayout:drawerlayout:1.2.0")

    //Glide 사진
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    //카카오톡 공유
    implementation ("com.kakao.sdk:v2-share:2.11.2")

    //카카오톡 공유
    implementation ("com.kakao.sdk:v2-user:2.14.0") // 최신 버전 사용
    implementation ("com.kakao.sdk:v2-talk:2.14.0")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.0")

    //카카오 네비
    implementation ("com.kakao.sdk:v2-navi:2.13.0")

    // 코루틴 테스트 코드
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    //UI 테스트
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    testImplementation ("org.mockito:mockito-core:4.0.0")

    implementation ("com.google.android.material:material:1.12.0")
}