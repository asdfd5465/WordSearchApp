// WordFinderApp/build.gradle.kts
// Intentionally blank or with just the plugins block if using version catalog
// If not using version catalog, the plugins need versions defined here or in settings.
// The settings.gradle.kts handles repository configuration for plugins.
// Let's assume the plugins block with aliases for now, and you'll create libs.versions.toml

// To make this fully work without manual libs.versions.toml creation for this example,
// let's use the older buildscript block for clarity in a CLI context:

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.0") // AGP
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
