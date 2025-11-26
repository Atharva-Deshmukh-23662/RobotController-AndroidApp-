@file:Suppress("DEPRECATION")

import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.robotcontroller"
    compileSdk = 35
    buildFeatures{
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.example.robotcontroller"
        // Lower minSdk for better device compatibility
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "apiKey", "\"${properties["GENERATIVEAI_API_KEY"]}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtension.get()
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Add packaging options to avoid conflicts
    fun Packaging.() {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "META-INF/DEPENDENCIES"
        excludes += "META-INF/LICENSE"
        excludes += "META-INF/LICENSE.txt"
        excludes += "META-INF/license.txt"
        excludes += "META-INF/NOTICE"
        excludes += "META-INF/NOTICE.txt"
        excludes += "META-INF/notice.txt"
        excludes += "META-INF/ASL2.0"
    }
}

dependencies {
    // Gemini client
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))

    // Compose core
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Picovoice for wake word detection
    implementation("ai.picovoice:porcupine-android:3.0.3")

    // Additional permissions helper (optional but recommended)
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    //json dependencies
    implementation("org.json:json:20240303")

    // CameraX for camera access & lifecycle management
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // TensorFlow Lite for object detection
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // ML Kit Object Detection (alternative to raw TFLite)
    implementation("com.google.android.gms:play-services-mlkit-object-detection:17.0.1")
    implementation("com.google.mlkit:object-detection:17.0.2")
    // Image processing & utility
    implementation("androidx.graphics:graphics-core:1.0.0-alpha03")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

}