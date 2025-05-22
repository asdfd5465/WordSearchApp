# 기본 Android 최적화 규칙을 포함합니다.
# -include proguard-android-optimize.txt # This is usually included via build.gradle.kts

# --- AndroidX and Material Components ---
# These are generally handled well by the default rules and R8,
# but adding explicit keep rules for specific components can sometimes be necessary
# if you face issues with custom views or specific Material theming attributes.
# For now, we'll rely on the defaults.

# --- Kotlin Coroutines ---
# Keep suspend functions and their metadata.
-keepclasseswithmembernames class * {
    @kotlin.jvm.JvmName <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
# For Reflection on Coroutines
-keepclassmembers class kotlinx.coroutines.android.MainCoroutineDispatcher {
    <init>(...);
}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    <init>(...);
}
-keepnames class kotlinx.coroutines.JobSupport* { *; }


# --- Gson ---
# Keep data classes that Gson will serialize/deserialize.
# Replace 'com.offlinedictionary.pro.WordDefinitionEntry' and 'com.offlinedictionary.pro.DefinitionDetail'
# with your actual package name if it's different.
-keep class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keep class com.offlinedictionary.pro.DefinitionDetail { *; }

# Keep a GSON specific annotation if you were using it, though it seems not in WordModels.kt
# -keepattributes Signature
# -keepattributes *Annotation*

# For GSON 2.8.5+, if you are not using @SerializedName and rely on field names matching JSON keys.
# If you use @SerializedName, this might not be strictly necessary but doesn't hurt.
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}


# --- TextToSpeech ---
# Generally, TextToSpeech doesn't require special ProGuard rules if used normally.
# However, if any reflection is used by the TTS engine internally or by your specific usage,
# you might need to add rules. Start without specific TTS rules unless issues arise.


# --- SQLite (via Android's SQLiteOpenHelper) ---
# Android's built-in SQLite classes are handled by the default Android ProGuard rules.
# If you were using Room, it would have its own annotation processor to generate rules.
# For direct SQLiteOpenHelper usage, usually no extra rules are needed unless you're
# doing something very custom with reflection around database classes.


# --- Keep your Application and Activities (and other entry points) ---
# This is crucial. The default Android rules usually handle this, but being explicit doesn't hurt.
# Replace 'com.offlinedictionary.pro' with your actual package name.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep custom Parcelable implementations (if you had any, doesn't seem like it now)
# -keep class * implements android.os.Parcelable {
#   public static final android.os.Parcelable$Creator *;
# }


# --- General good practices ---
# Keep enums (already covered by coroutines section but good to know)
# -keepclassmembers enum * {
#     public static **[] values();
#     public static ** valueOf(java.lang.String);
# }

# Keep native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep R class and its members (usually handled, but good for safety)
# Replace 'com.offlinedictionary.pro' with your package name
-keep class com.offlinedictionary.pro.R$* {
    *;
}


# --- Add any specific rules for third-party libraries if their documentation suggests it ---
# For example, if you were using a specific networking library with reflection, etc.
# Your current dependencies (OkHttp, if still present, is usually fine with default R8)

# If you see "ClassNotFoundException" or "NoSuchMethodError" for specific classes
# after enabling ProGuard, you'll need to add a -keep rule for that class/method.
# Example: -keep class com.example.mycustomlibrary.SomeClass { *; }
