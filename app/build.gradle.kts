import java.util.Properties // Required for reading gradle.properties if used locally
import java.io.FileInputStream // Required for reading gradle.properties if used locally

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Function to load properties from gradle.properties (optional for local builds)
fun loadProperties(projectDir: java.io.File, fileName: String = "gradle.properties"): Properties {
    val properties = Properties()
    val propertiesFile = java.io.File(projectDir, fileName)
    if (propertiesFile.exists()) {
        FileInputStream(propertiesFile).use {
            properties.load(it)
        }
    }
    return properties
}

// Load local properties (if they exist, primarily for local signing)
// This is kept separate so it doesn't break CI if gradle.properties isn't there.
// val localProperties = loadProperties(project.rootDir) // If gradle.properties is in root project
val localAppProperties = loadProperties(project.projectDir) // If gradle.properties is in app module root

android {
    namespace = "com.offlinedictionary.pro"
    compileSdk = 34

    // --- SIGNING CONFIGURATION START ---
    // Retrieve signing configuration from environment variables (for CI)
    // or from gradle.properties (for local builds, if you set them up there).
    // The names MYAPP_... match what we set in GitHub Actions ENV and can be used in local gradle.properties
    val storeFileVar = System.getenv("MYAPP_RELEASE_STORE_FILE") ?: localAppProperties.getProperty("MYAPP_RELEASE_STORE_FILE")
    val storePasswordVar = System.getenv("MYAPP_RELEASE_STORE_PASSWORD") ?: localAppProperties.getProperty("MYAPP_RELEASE_STORE_PASSWORD")
    val keyAliasVar = System.getenv("MYAPP_RELEASE_KEY_ALIAS") ?: localAppProperties.getProperty("MYAPP_RELEASE_KEY_ALIAS")
    val keyPasswordVar = System.getenv("MYAPP_RELEASE_KEY_PASSWORD") ?: localAppProperties.getProperty("MYAPP_RELEASE_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            // Check if all necessary signing information is present
            if (storeFileVar != null && storePasswordVar != null && keyAliasVar != null && keyPasswordVar != null) {
                val keystore = java.io.File(storeFileVar)
                if (keystore.exists()) {
                    storeFile = keystore
                    storePassword = storePasswordVar
                    this.keyAlias = keyAliasVar // Use 'this.keyAlias' to avoid conflict
                    this.keyPassword = keyPasswordVar
                    println("Release signing configured with: $storeFileVar")
                } else {
                    println("WARNING: Keystore file not found at '$storeFileVar'. Release build will not be signed properly.")
                    // To prevent accidental unsigned release, you might want to throw an error here in CI
                    // if (System.getenv("CI") == "true") {
                    //     throw GradleException("Keystore file not found in CI environment.")
                    // }
                }
            } else {
                println("WARNING: Release signing information not fully provided (keystore path, passwords, or alias missing). Release build may not be signed.")
                // In CI, this block should ideally not be hit if secrets are set up correctly.
                // For local builds, this means gradle.properties isn't set up for release signing.
            }
        }
    }
    // --- SIGNING CONFIGURATION END ---

    defaultConfig {
        applicationId = "com.offlinedictionary.pro"
        minSdk = 23
        targetSdk = 34
        versionCode = 1 // Stable Release
        versionName = "1.0.0" // As per your requirement

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true // Needed for BuildConfig.APPLICATION_ID in DatabaseHelper
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Recommended for release
            isShrinkResources = true // Recommended for release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply the signing configuration to the release build type
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            // For debug builds, Android Studio/Gradle automatically signs with a debug key.
            // No explicit signingConfig needed here unless you want to override it.
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

    // OkHttp was already commented out, which is correct if not used.
    // implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
