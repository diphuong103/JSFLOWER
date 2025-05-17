plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.jsflower"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jsflower"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.play.services.cast.framework)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation (libs.androidx.navigation.fragment.ktx.v275)
    implementation (libs.androidx.navigation.ui.ktx)

    implementation(libs.imageslideshow)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation("com.google.android.gms:play-services-auth:21.3.0")


    implementation(libs.firebase.database.ktx)

// Thư viện IMGBB (Retrofit, Gson Converter, OkHttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

// Glide
    implementation(libs.glide)

    implementation (libs.circleimageview)
    implementation (libs.facebook.android.sdk)
    implementation (libs.facebook.login.v1803)


    implementation (libs.androidx.core.ktx.v1120)

    // thu vien pdf
    implementation (libs.core)
    implementation (libs.zxing.android.embedded)

    // thu vien ZXing
    implementation (libs.core)

    implementation (libs.firebase.firestore)

    implementation (libs.google.firebase.firestore)


//    implementation (libs.play.services.maps)
//    implementation (libs.play.services.location) // nếu muốn lấy vị trí hiện tại

    // Thư viện map
    implementation (libs.osmdroid.android)


    //thu vien phong to hinh (review_image)
    implementation (libs.photoview)

    implementation (libs.androidx.core)




}