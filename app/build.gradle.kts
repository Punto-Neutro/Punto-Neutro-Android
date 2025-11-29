plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt") // ⬅️ FORMATO CORRECTO - Plugin de KAPT para Room
    kotlin("plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.example.sprint_2_kotlin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sprint_2_kotlin"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    //dependencies for Supabase... check https://github.com/supabase-community/supabase-kt for latest versions
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.4")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.4")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.2.4")
    implementation("io.ktor:ktor-client-android:3.2.2")

    //dependency for serialization, to transform Supabase data into serialized objects
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    //dependency for managing timestamptz and Instant
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    //dependency for coil
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    // Coil para cargar imágenes desde URLs
    implementation("io.coil-kt:coil-compose:2.5.0")

    //dependency for fingerprint sensor
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // DataStore para persistir preferencias (Dark Mode)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ========================================
    // ROOM DATABASE (CACHING)
    // ========================================
    implementation("androidx.room:room-runtime:2.7.0-alpha12")
    implementation("androidx.room:room-ktx:2.7.0-alpha12")
    kapt("androidx.room:room-compiler:2.7.0-alpha12")

    // ========================================
    // COROUTINES (para operaciones async)
    // ========================================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Charts para Jetpack Compose izi de usar
    implementation("co.yml:ycharts:2.1.0")
    // ========================================
    // PULL-TO-REFRESH
    // ========================================
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    //QR code
    implementation("com.google.zxing:core:3.5.2")


    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}