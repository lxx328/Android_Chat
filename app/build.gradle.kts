plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.dexter.little_smart_chat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dexter.little_smart_chat"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // 添加 buildConfigField 配置字段
        // samrt match dify
        buildConfigField("String", "yzsASRKey", "\"vbuxkpnjhc3mowpxd7su234ju6ujxy67kdwx65ah\"")
        buildConfigField("String", "yzsASRSecret", "\"f35d200d3e7bb49bdcfa13d15908ecc2\"")

        buildConfigField ("String", "yzsTTSOnlineKey", "\"jw6uckedg5uvjpayeftn5okjld3v5h3wwzee7aiw\"")
        buildConfigField ("String", "yzsTTSOnlinSecret", "\"b26d5c633db78f0cbe03d57dcd0c5b35\"")

        buildConfigField("String", "yzsKey", "\"imtyrjeffxaypusrywdi6xwcpzakuzs5m2ttfjil\"")
        buildConfigField("String", "yzsSecret", "\"3155e1c46a331f934f5088c3c2cbc692\"")

        buildConfigField("String", "llm", "\"aaaaaaaaaaaaaaaaaaaaaaa\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-Xskip-metadata-version-check"
        )
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.jzplayer)
    implementation(libs.glide)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2ConverterGson)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.okhttp3.logging.interceptor)

    implementation(libs.mqttv5)
    implementation(libs.timber)
    implementation(libs.localbroadcastmanager)
    implementation(fileTree("libs") {
        include("*.jar", "*.aar")
    })

    //生命周期服务lifecycle-service
    implementation(libs.androidx.lifecycle.service)

    implementation(libs.gson)

}