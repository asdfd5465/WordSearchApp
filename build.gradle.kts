// WordFinderApp/build.gradle.kts
// This file can be intentionally blank if plugins are managed in settings.gradle.kts
// and applied in module-level build.gradle.kts.
// However, often you define top-level plugin versions here using the older `buildscript`
// if not using version catalogs.
//
// Given your setup, and if you create libs.versions.toml as hinted above, this works:
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

// If NOT using libs.versions.toml, for this CLI setup, you can do:
// buildscript {
//     ext.kotlin_version = "1.9.22" // Example
//     repositories {
//         google()
//         mavenCentral()
//     }
//     dependencies {
//         classpath 'com.android.tools.build:gradle:8.3.0' // AGP
//         classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
//     }
// }
//
// allprojects {
//     repositories {
//         google()
//         mavenCentral()
//     }
// }
