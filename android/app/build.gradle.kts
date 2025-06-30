plugins {
    id("com.android.application")
    kotlin("android") version "1.9.21"
    kotlin("kapt") version "1.9.21"
}

android {
    namespace = "com.erinaceous.documind"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        val llmApiKey: String = project.findProperty("LLM_API_KEY") as String? ?: ""
        buildConfigField("String", "LLM_API_KEY", "\"$llmApiKey\"")
        println("Qwen API Key: $llmApiKey")
        applicationId = "com.erinaceous.documind"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/*.kotlin_module"
            )
            excludes -= "com/tom_roush/pdfbox/resources/glyphlist/glyphlist.txt"
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("com.alibaba:fastjson:1.2.78")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("com.google.code.gson:gson:2.10.1")

}