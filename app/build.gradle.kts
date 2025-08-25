import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.kapt")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.sentry.android.gradle") version "5.9.0"
    id("com.google.dagger.hilt.android")
}

val secretsProps =
    Properties().apply {
        val f = rootProject.file("secrets.properties")
        if (f.exists()) load(f.inputStream())
    }

android {
    namespace = "tech.arcent"
    compileSdk = 36

    sentry {
        org = secretsProps.getProperty("SENTRY_ORG")
        projectName = secretsProps.getProperty("SENTRY_PROJECT")
        uploadNativeSymbols = true
    }

    defaultConfig {
        applicationId = "tech.arcent"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.10 Beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        getByName("debug") {
            val sentryDsnRaw = secretsProps.getProperty("SENTRY_DSN", "")
            val sentryDsn = sentryDsnRaw.trim().removeSurrounding("\"")
            buildConfigField("String", "SENTRY_DSN", "\"${sentryDsn}\"")
            buildConfigField("boolean", "SENTRY_ENABLED", sentryDsn.isNotBlank().toString())
            /*
            val profileFun = secretsProps.getProperty("APPWRITE_PROFILE_IMAGES_FUN", "").trim()
            buildConfigField("String", "APPWRITE_PROFILE_IMAGES_FUN", "\"${profileFun}\"")
            val profileBucket = secretsProps.getProperty("APPWRITE_PROFILE_IMAGES_BUCKET", "").trim()
            buildConfigField("String", "APPWRITE_PROFILE_IMAGES_BUCKET", "\"${profileBucket}\"")
            */
        }
        getByName("release") {
            val sentryDsnRaw = secretsProps.getProperty("SENTRY_DSN", "")
            val sentryDsn = sentryDsnRaw.trim().removeSurrounding("\"")
            buildConfigField("String", "SENTRY_DSN", "\"${sentryDsn}\"")
            buildConfigField("boolean", "SENTRY_ENABLED", sentryDsn.isNotBlank().toString())
            /*
            val profileFun = secretsProps.getProperty("APPWRITE_PROFILE_IMAGES_FUN", "").trim()
            buildConfigField("String", "APPWRITE_PROFILE_IMAGES_FUN", "\"${profileFun}\"")
            val profileBucket = secretsProps.getProperty("APPWRITE_PROFILE_IMAGES_BUCKET", "").trim()
            buildConfigField("String", "APPWRITE_PROFILE_IMAGES_BUCKET", "\"${profileBucket}\"")
             */
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { buildConfig = true }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "secrets.properties.example"
    ignoreList.add("sdk.*")
}

configurations.configureEach { exclude(group = "com.squareup.okhttp3", module = "okhttp-bom") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.appwrite:sdk-for-android:4.0.1") { exclude(group = "com.squareup.okhttp3", module = "okhttp-bom") }
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.compose.animation:animation")
    implementation("io.sentry:sentry-android:8.19.1")
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test:core:1.5.0")
}
