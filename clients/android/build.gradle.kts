import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

android {
    namespace = "com.wishlist.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wishlist.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SUPABASE_URL", "\"${secrets.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${secrets.getProperty("SUPABASE_PUBLISHABLE_KEY", "")}\"")
        buildConfigField("String", "API_BASE_URL", "\"${secrets.getProperty("API_BASE_URL", "http://10.0.2.2:4000")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))

    // Android Compose entry point
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Supabase Compose Auth (Android-specific deep link handling)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.compose.auth)

    // Koin Android
    implementation(libs.koin.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
}
