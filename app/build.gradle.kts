// NO top-level imports for java.io.File or FileInputStream for now.
// Let Gradle handle file resolution within its DSL blocks.
import java.util.Properties // This one is usually fine for Gradle itself.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.offlinedictionary.pro"
    compileSdk = 34

    // --- SIGNING CONFIGURATION START ---
    signingConfigs {
        create("release") {
            // Read directly from environment variables.
            // These are expected to be set by the GitHub Actions workflow.
            val storeFileFromEnv = System.getenv("MYAPP_RELEASE_STORE_FILE")
            val storePasswordFromEnv = System.getenv("MYAPP_RELEASE_STORE_PASSWORD")
            val keyAliasFromEnv = System.getenv("MYAPP_RELEASE_KEY_ALIAS")
            val keyPasswordFromEnv = System.getenv("MYAPP_RELEASE_KEY_PASSWORD")

            if (storeFileFromEnv != null && !storeFileFromEnv.isEmpty() &&
                storePasswordFromEnv != null && !storePasswordFromEnv.isEmpty() &&
                keyAliasFromEnv != null && !keyAliasFromEnv.isEmpty() &&
                keyPasswordFromEnv != null && !keyPasswordFromEnv.isEmpty()) {

                // Gradle's 'storeFile' property can take a String path and resolve it.
                // It will then internally convert it to a File object.
                storeFile = project.file(storeFileVar) // Use project.file() to resolve path correctly
                storePassword = storePasswordFromEnv
                this.keyAlias = keyAliasFromEnv // Use 'this.keyAlias' to avoid Kotlin property/method name clash
                this.keyPassword = keyPasswordFromEnv
                println("Release signing configured using environment variables. Keystore path: $storeFileFromEnv")
            } else {
                println("WARNING: Release signing information not fully provided via environment variables. Release build may not be signed or may fail.")
                // For CI, if secrets are not set, this will be the state.
                // The build might fail later if signing is strictly required and no valid config is found.
            }
        }
    }
    // --- SIGNING CONFIGURATION END ---

    defaultConfig {
        applicationId = "com.offlinedictionary.pro"
        minSdk = 23
        targetSdk = 34
        versionCode = 1 // Stable Release
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Changed to true for release
            isShrinkResources = true // Added for release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply the signing configuration to the release build type
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
