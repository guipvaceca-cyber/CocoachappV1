plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}
android {
    namespace = "com.example.coachapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.coachapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.3-RC"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".beta"
            manifestPlaceholders["appLabel"] = "CoCoach Debug"
        }
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            manifestPlaceholders["appLabel"] = "CoCoach Beta"
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")

            firebaseAppDistribution {
                artifactType = "APK"
                // Vous pouvez ajouter releaseNotes = "Version beta" ou testerGroups = "beta-testers"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["appLabel"] = "CoCoach"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Supabase & Network
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.functions)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // QR Code
    implementation("com.google.zxing:core:3.5.3")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    annotationProcessor(libs.androidx.room.compiler)
}
