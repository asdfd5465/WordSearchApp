import java.util.Properties // Still might be needed if you add local properties loading later

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.offlinedictionary.pro"
    compileSdk = 34

    // Define these variables INSIDE the android block so they are in scope
    // for signingConfigs
    val storeFileFromEnv = System.getenv("MYAPP_RELEASE_STORE_FILE")
    val storePasswordFromEnv = System.getenv("MYAPP_RELEASE_STORE_PASSWORD")
    val keyAliasFromEnv = System.getenv("MYAPP_RELEASE_KEY_ALIAS")
    val keyPasswordFromEnv = System.getenv("MYAPP_RELEASE_KEY_PASSWORD")

    // --- SIGNING CONFIGURATION START ---
    signingConfigs {
        create("release") {
            if (storeFileFromEnv != null && storeFileFromEnv.isNotEmpty() &&
                storePasswordFromEnv != null && storePasswordFromEnv.isNotEmpty() &&
                keyAliasFromEnv != null && keyAliasFromEnv.isNotEmpty() &&
                keyPasswordFromEnv != null && keyPasswordFromEnv.isNotEmpty()) {

                // Now storeFileFromEnv is correctly in scope
                storeFile = project.file(storeFileFromEnv)
                storePassword = storePasswordFromEnv
                this.keyAlias = keyAliasFromEnv
                this.keyPassword = keyPasswordFromEnv
                println("Release signing configured using environment variables. Keystore path: $storeFileFromEnv")
            } else {
                println("WARNING: Release signing information not fully provided via environment variables. Release build may not be signed or may fail.")
            }
        }
    }
    // --- SIGNING CONFIGURATION END ---

    defaultConfig {
        applicationId = "com.offlinedictionary.pro"
        minSdk = 23
        targetSdk = 34
        versionCode = 1 // Stable Release
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Gson for parsing JSON strings from DB columns (examples, synonyms, etc.)
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
