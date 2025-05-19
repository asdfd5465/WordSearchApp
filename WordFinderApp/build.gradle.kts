// WordFinderApp/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

// If you don't have libs.versions.toml, you can define versions directly or
// use a more traditional approach for now. For CLI, simpler might be:
// plugins {
//    id("com.android.application") version "8.3.0" apply false
//    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
// }
//
// To make the alias above work without libs.versions.toml, create a file:
// WordFinderApp/gradle/libs.versions.toml
// and add:
// [versions]
// agp = "8.3.0"
// kotlin = "1.9.22"
//
// [plugins]
// android-application = { id = "com.android.application", version.ref = "agp" }
// jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }

// For simplicity without libs.versions.toml yet, let's use this in project build.gradle.kts:
// buildscript {
//     repositories {
//         google()
//         mavenCentral()
//     }
//     dependencies {
//         classpath("com.android.tools.build:gradle:8.3.0")
//         classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
//     }
// }
//
// allprojects {
//     repositories {
//         google()
//         mavenCentral()
//     }
// }
//
// The pluginManagement in settings.gradle.kts is the modern way,
// so let's stick to that and assume the aliases will resolve if you set up libs.versions.toml
// or use explicit plugin IDs in app/build.gradle.kts
