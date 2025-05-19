WordSearchApp/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/  (or kotlin/)
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           └── wordsearchapp/
│   │   │   │               ├── MainActivity.kt
│   │   │   │               ├── DictionaryApiService.kt
│   │   │   │               └── WordModels.kt
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       │   └── activity_main.xml
│   │   │       └── values/
│   │   │           ├── strings.xml
│   │   │           ├── colors.xml
│   │   │           └── themes.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat

JDK: 17
Kotlin (standalone): 1.9.22
Gradle: 8.4 (via wrapper)
Android Gradle Plugin (AGP): Version to be set in build.gradle.kts (target 8.3.x)
Android SDK:
Root: /workspaces/android-sdk (with ANDROID_HOME set)
Command-line Tools: 19.0 (Excellent)
Build-Tools: 34.0.0
Platform-Tools: 35.0.2
Platforms: android-34
Note: for keeping repo files intact and fetch them in codesapce:
git fetch origin
git reset --hard origin/main
git clean -fdx
Note: To build 
chmod +x gradlew
./gradlew clean
./gradlew assembleDebug
