import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "com.aritradas.medai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aritradas.medai"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "API_KEY", "\"${project.findProperty("API_KEY") ?: ""}\"")
        buildConfigField(
            "String",
            "GOOGLE_SHEETS_API_KEY",
            "\"${project.findProperty("GOOGLE_SHEETS_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_SHEETS_ID",
            "\"${project.findProperty("GOOGLE_SHEETS_ID") ?: ""}\""
        )
        buildConfigField("String", "MIXPANEL_PROJECT_TOKEN", "\"${project.findProperty("MIXPANEL_PROJECT_TOKEN") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: ""}\"")
        buildConfigField(
            "String",
            "GOOGLE_SHEET_WEB_APP_URL",
            "\"${project.findProperty("GOOGLE_SHEET_WEB_APP_URL") ?: ""}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        buildConfig = true
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"

    // Don't fail build on issues, just generate reports
    ignoreFailures = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.runtime.livedata)

    //Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    // Timber
    implementation (libs.timber)

    // Coil
    implementation(libs.coil.compose)

    // Gemini
    implementation(libs.generativeai)

    // Coroutine
    implementation (libs.kotlinx.coroutines.android)

    //Gson
    implementation(libs.gson)

    //DataStore
    implementation(libs.androidx.datastore.preferences)

    // Biometric
    implementation(libs.androidx.biometric)

    // Splash
    implementation(libs.androidx.core.splashscreen)

    // In-App Update
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.lifecycle.livedata.ktx)

    // OkHttp for Google Sheets API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    //MixPanel
    implementation(libs.mixpanel.android)

    //Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
