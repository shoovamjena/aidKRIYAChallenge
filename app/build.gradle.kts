
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.aidkriyachallenge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aidkriyachallenge"
        minSdk = 28
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

    // --- Core AndroidX & Lifecycle & Activity ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activityKtx) // From version catalog
    implementation(libs.androidx.lifecycle.runtime.compose)

    // --- Compose (Import BOM, then declare without versions) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material.iconsExtended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.windowSizeClass)

    // --- Firebase (Import BOM, then declare without versions) ---
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    // Add the dependencies for the Firebase products you want to use
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-common")


    // --- Google Maps & Location ---
    implementation(libs.google.playServices.maps)
    implementation(libs.google.playServices.location)
    implementation(libs.google.maps.compose)
    implementation(libs.google.places)
    implementation(libs.google.maps.utils)
    implementation(libs.maps.compose.utils)

    // --- Credentials & Google ID ---
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Networking (Retrofit) ---
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converterGson)

    // --- UI & Utilities ---
    implementation(libs.airbnb.lottieCompose)
    implementation(libs.androidx.window)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material3)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Import Compose BOM for consistent test library versions
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)// e.g., libs.androidx.ui.test.manifest (Add version)

    //Async Image
    implementation("io.coil-kt:coil-compose:2.7.0")

    //Razorpay Payment gateway
    implementation("com.razorpay:checkout:1.6.41")


}
