plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dict.nick" // Your package name
    compileSdk = 34

    defaultConfig {
        applicationId = "dict.nick"
        minSdk = 24 // Keep as is, or adjust if needed
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Set to false for initial debugging if needed
            isShrinkResources = true // Set to false for initial debugging if needed
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug" // Good practice for debug builds
            versionNameSuffix = "-debug"   // Good practice
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Or 17 if your JDK is 17+
        targetCompatibility = JavaVersion.VERSION_1_8 // Or 17
    }
    kotlinOptions {
        jvmTarget = "1.8" // Or "17"
    }
    buildFeatures {
        compose = true // Assuming you still want Jetpack Compose
    }
    composeOptions {
        // For Kotlin 1.9.23, this is the compatible Compose Compiler version
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0") // Or latest stable
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Or latest stable
    implementation("androidx.activity:activity-compose:1.8.2") // Or latest stable

    // Material Components (for standard XML themes, very stable)
    implementation("com.google.android.material:material:1.11.0") // Latest stable Material Components for XML

    // Jetpack Compose BOM and Dependencies
    // Using a slightly older but very stable BOM initially for baseline
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // Material 3 for Compose UI
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Datastore for Theme Preference
    implementation("androidx.datastore:datastore-preferences:1.0.0") // Using 1.0.0 as it's very stable

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
