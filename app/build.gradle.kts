plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "stanissay.wear.pass"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "stanissay.wear.pass"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.5"
    }

    buildTypes {
        release {}
        base.archivesName = "WearPass-" + defaultConfig.versionName
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.text)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.androidx.wear)
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.wearable)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.navigation3)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.kotpass)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.watchface.complications.rendering)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    //noinspection LoginCredentials
    implementation(libs.androidx.credentials)
}