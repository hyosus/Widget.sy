import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "com.example.widgetsy"
    compileSdk = 35

    // Load properties outside buildTypes
    val keystoreFile = project.rootProject.file("apiKeys.properties")
    val properties = Properties()
    properties.load(keystoreFile.inputStream())

    val SPOTIFY_CLIENT_ID = properties.getProperty("SPOTIFY_CLIENT_ID") ?: ""
    val SPOTIFY_REDIRECT_URI = properties.getProperty("SPOTIFY_REDIRECT_URI") ?: ""

    defaultConfig {
        applicationId = "com.example.widgetsy"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$SPOTIFY_CLIENT_ID\"")
            buildConfigField("String", "SPOTIFY_REDIRECT_URI", "\"$SPOTIFY_REDIRECT_URI\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$SPOTIFY_CLIENT_ID\"")
            buildConfigField("String", "SPOTIFY_REDIRECT_URI", "\"$SPOTIFY_REDIRECT_URI\"")
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
        viewBinding = true
        buildConfig = true
    }
}


dependencies {
    val appcompat_version = "1.7.0"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(files("../spotify-app-remote-release-0.8.0.aar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.4")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("androidx.lifecycle:lifecycle-process:2.9.4")
    // For Glance support
    implementation("androidx.glance:glance:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // For AppWidgets support
    implementation("androidx.glance:glance-appwidget:1.1.1")

    implementation("com.google.code.gson:gson:2.6.1")

    // For spotify music widget
    implementation("com.spotify.android:auth:1.2.5") // Maven dependency
    implementation("androidx.browser:browser:1.0.0")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    implementation("androidx.palette:palette:1.0.0")

}