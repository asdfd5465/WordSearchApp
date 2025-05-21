// Imports should still be fine here, but the usage below will change.
import java.util.Properties
import java.io.FileInputStream // We will still use this for reading the stream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Function to load properties from gradle.properties
fun loadProperties(project: Project, fileName: String = "gradle.properties"): Properties {
    val properties = Properties()
    // Use project.file() to get a File object relative to the project directory
    val propertiesFile = project.file(fileName)
    if (propertiesFile.exists()) {
        try {
            FileInputStream(propertiesFile).use { inputStream ->
                properties.load(inputStream)
            }
        } catch (e: Exception) {
            project.logger.warn("Warning: Could not load properties from ${propertiesFile.path}: ${e.message}")
        }
    } else {
        project.logger.info("Info: Properties file not found at ${propertiesFile.path}")
    }
    return properties
}

// Load local properties. Pass 'project' which refers to the current app module's project.
val localAppProperties = loadProperties(project, "gradle.properties") // Assumes gradle.properties in app module root
// Or if it's in the root project: val localRootProperties = loadProperties(project.rootDir, "gradle.properties")


android {
    namespace = "com.offlinedictionary.pro"
    compileSdk = 34

    // --- SIGNING CONFIGURATION START ---
    val storeFileVar = System.getenv("MYAPP_RELEASE_STORE_FILE") ?: localAppProperties.getProperty("MYAPP_RELEASE_STORE_FILE")
    val storePasswordVar = System.getenv("MYAPP_RELEASE_STORE_PASSWORD") ?: localAppProperties.getProperty("MYAPP_RELEASE_STORE_PASSWORD")
    val keyAliasVar = System.getenv("MYAPP_RELEASE_KEY_ALIAS") ?: localAppProperties.getProperty("MYAPP_RELEASE_KEY_ALIAS")
    val keyPasswordVar = System.getenv("MYAPP_RELEASE_KEY_PASSWORD") ?: localAppProperties.getProperty("MYAPP_RELEASE_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (storeFileVar != null && storePasswordVar != null && keyAliasVar != null && keyPasswordVar != null) {
                // Use project.file() to resolve the keystore path string to a File object
                // This is more robust within Gradle scripts.
                val keystoreFileObject = project.file(storeFileVar)
                if (keystoreFileObject.exists()) {
                    storeFile = keystoreFileObject // Gradle's 'storeFile' property expects a java.io.File
                    storePassword = storePasswordVar
                    this.keyAlias = keyAliasVar
                    this.keyPassword = keyPasswordVar
                    println("Release signing configured with: ${keystoreFileObject.absolutePath}")
                } else {
                    println("WARNING: Keystore file not found at '${storeFileVar}'. Evaluated path: ${keystoreFileObject.absolutePath}. Release build will not be signed properly.")
                }
            } else {
                println("WARNING: Release signing information not fully provided (keystore path, passwords, or alias missing). Release build may not be signed.")
            }
        }
    }
    // --- SIGNING CONFIGURATION END ---

    defaultConfig {
        applicationId = "com.offlinedictionary.pro"
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
