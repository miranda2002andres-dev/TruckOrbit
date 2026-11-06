plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
}

android {
    namespace = "com.immon.truckorbit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.immon.truckorbit"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        viewBinding = true
        dataBinding = true
        buildConfig = true
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
    // implementation(libs.play.services.maps) // <--- COMENTADO
    implementation(libs.play.services.location)
    implementation(libs.core.splashscreen)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.glide)
    implementation(libs.firebase.storage)
    implementation(libs.gson)
    implementation(libs.loading.button.android)
    implementation(libs.circleimageview)
    
    // <--- NUEVA LIBRERÍA DE MAPAS
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}