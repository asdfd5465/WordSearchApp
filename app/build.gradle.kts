// Add these imports at the VERY TOP of the file
import java.io.File // For java.io.File
import java.io.FileInputStream // For FileInputStream
import java.util.Properties // Already there, but ensure it's at the top

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Function to load properties from gradle.properties (optional for local builds)
fun loadProperties(projectDir: File, fileName: String = "gradle.properties"): Properties { // Changed java.io.File to File
    val properties = Properties()
    val propertiesFile = File(projectDir, fileName) // Changed java.io.File to File
    if (propertiesFile.exists()) {
        FileInputStream(propertiesFile).use { inputStream -> // Explicitly name the lambda parameter
            properties.load(inputStream)
        }
    }
    return properties
}

// Load local properties (if they exist, primarily for local signing)
// This is kept separate so it doesn't break CI if gradle.properties isn't there.
// val localProperties = loadProperties(project.rootDir) // If gradle.properties is in root project
val localAppProperties = loadProperties(project.projectDir) // If gradle.properties is in app module root

android {
    namespace = "com.offlinedictionary.pro" // Your actual package name
    compileSdk = 34

    // --- SIGNING CONFIGURATION START ---
    val storeFileVar = System.getenv("MYAPP_RELEASE_STORE_FILE") ?: localAppProperties.getProperty("MYAPP_RELEASE_STORE_FILE")
    val storePasswordVar = System.getenv("MYAPP_RELEASE_STORE_PASSWORD") ?: localAppProperties.getProperty("MYAPP_RELEASE_STORE_PASSWORD")
    val keyAliasVar = System.getenv("MYAPP_RELEASE_KEY_ALIAS") ?: localAppProperties.getProperty("MYAPP_RELEASE_KEY_ALIAS")
    val keyPasswordVar = System.getenv("MYAPP_RELEASE_KEY_PASSWORD") ?: localAppProperties.getProperty("MYAPP_RELEASE_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (storeFileVar != null && storePasswordVar != null && keyAliasVar != null && keyPasswordVar != null) {
                val keystore = File(storeFileVar) // Changed java.io.File to File
                if (keystore.exists()) {
                    storeFile = keystore
                    storePassword = storePasswordVar
                    this.keyAlias = keyAliasVar
                    this.keyPassword = keyPasswordVar
                    println("Release signing configured with: $storeFileVar")
                } else {
                    println("WARNING: Keystore file not found at '$storeFileVar'. Release build will not be signed properly.")
                }
            } else {
                println("WARNING: Release signing information not fully provided. Release build may not be signed.")
            }
        }
    }
    // --- SIGNING CONFIGURATION END ---

    defaultConfig {
        applicationId = "com.offlinedictionary.pro" // Your actual package name
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
