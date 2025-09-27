plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("com.google.gms.google-services") // Add this line
}

android {
    namespace = "com.lonyitrade.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lonyitrade.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnit4"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.android.material:material:1.12.0")

    // Added to support fragments and activityViewModels
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Retrofit for network calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Updated version
    implementation("com.squareup.okhttp3:okhttp:4.9.3") // NEW: For WebSocket support

    // Add this line for the Gson library
    implementation("com.google.code.gson:gson:2.10.1")

    // Add this line for Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // New dependency for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")

    // --- New Firebase Dependencies ---
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    // Add the dependency for the Firebase Cloud Messaging library
    implementation("com.google.firebase:firebase-messaging-ktx")
}