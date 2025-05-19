plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.wordsearchapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wordsearchapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Set to true for production builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    // If you don't have ic_launcher files, you might need this to avoid build errors,
    // or just ensure default placeholder icons are available to the build system.
    // sourceSets.getByName("main") {
    //     res.srcDirs("src/main/res", "src/main/res-defaults") // if you add a res-defaults with placeholders
    // }
    // For simplicity with no custom icons, default behavior might be fine.
    // If build fails on missing mipmaps, create empty `mipmap-anydpi-v26/ic_launcher.xml`
    // and `mipmap-anydpi-v26/ic_launcher_round.xml` in `src/main/res`.
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0") // Use a recent stable version
    implementation("androidx.appcompat:appcompat:1.6.1") // Use a recent stable version
    implementation("com.google.android.material:material:1.11.0") // Use a recent stable version
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Use a recent stable version

    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // For lifecycleScope

    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
