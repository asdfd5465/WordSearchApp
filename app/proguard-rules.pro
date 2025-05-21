#===============================================================================
# Android Default Optimizations (provided by getDefaultProguardFile)
#===============================================================================
# This file is for YOUR app-specific rules.
# The "proguard-android-optimize.txt" already handles many common Android rules.

#===============================================================================
# Keep Application and Core Android Components
#===============================================================================
# Keep your main Application class if you have one (you don't currently)
# -keep public class com.offlinedictionary.pro.MyApplication extends android.app.Application

# Keep your Activities. The default Android rules usually cover this,
# but being explicit for the main launcher activity is safe.
-keep public class com.offlinedictionary.pro.MainActivity {
    public <init>(); # Keep constructor
    public *;       # Keep all public members
}
# Add other Activities, Services, BroadcastReceivers, ContentProviders here if you add them.
# Example:
# -keep public class * extends android.app.Service
# -keep public class * extends android.content.BroadcastReceiver

#===============================================================================
# Keep Data Classes / Models
#===============================================================================
# Since Gson is used in DatabaseHelper to parse JSON strings into List<String>
# for fields within DefinitionDetail, we need to ensure DefinitionDetail and its
# relevant fields are kept if Gson relies on field names for mapping (which it does).
# WordDefinitionEntry is also important.
-keep class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keep class com.offlinedictionary.pro.DefinitionDetail { *; }

# Gson specific rules (often needed if serializing/deserializing custom objects directly)
# Keep these for safety with Gson, especially if you plan to use it more.
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes *Annotation* # Keep annotations, useful for libraries
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class sun.misc.Unsafe { *; } # Gson might use this

# Keep constructors and fields of classes that Gson will instantiate or populate via reflection.
# This is a common pattern for classes (de)serialized by Gson.
# For your current use (List<String>), the above should be sufficient, but this is more general.
# -keepclassmembers,allowobfuscation class * {
#  @com.google.gson.annotations.SerializedName <fields>;
# }
# -keepclassmembers class com.offlinedictionary.pro.** { # Your model package
#    <init>(...); # Keep constructors
#    private <fields>; # Keep fields, even private, if Gson accesses them
# }


#===============================================================================
# Keep Kotlin Specifics
#===============================================================================
# Keep Kotlin metadata - VERY IMPORTANT.
-keepattributes RuntimeVisibleAnnotations,Signature,KotlinIdentifiers,KotlinUMP
-keep class kotlin.Metadata { *; }

# Keep classes and members related to coroutines and suspend functions.
# The KotlinUMP and Metadata rules usually handle this, but being more explicit
# can sometimes help with more complex coroutine usage.
-keepnames class kotlinx.coroutines.flow.** { *; }
-keepnames class kotlinx.coroutines.channels.** { *; }
-keepclassmembers class ** {
    public static java.lang.Object *(kotlin.coroutines.Continuation);
}
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    kotlin.coroutines.Continuation getCompletion();
    void setCompletion(kotlin.coroutines.Continuation);
}
-keepclassmembers class kotlin.coroutines.jvm.internal.SuspendLambda { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.ContinuationImpl { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.RunSuspend { *; }


# Keep default_implementations classes and methods (generated for default arguments in interfaces)
-keepclassmembers class **$DefaultImpls {
    *;
}

# Keep functions used in lambdas if they are not correctly preserved by metadata.
# -keepclassmembers class kotlin.jvm.functions.Function* { *; }


#===============================================================================
# AndroidX and Material Components Libraries
#===============================================================================
# These libraries generally provide their own consumer ProGuard rules,
# so explicit rules here are often not needed.
# If you encounter issues with a specific component (e.g., a Material dialog, view),
# you might need to add a rule like:
# -keep class com.google.android.material.textfield.MaterialAutoCompleteTextView { *; }
# But only do this if you confirm it's being removed/obfuscated incorrectly.


#===============================================================================
# Specific Rule for BuildConfig (used in your DatabaseHelper)
#===============================================================================
-keep class com.offlinedictionary.pro.BuildConfig { *; }


#===============================================================================
# TextToSpeech (TTS)
#===============================================================================
# Android's TextToSpeech service itself is a system component.
# Your direct interactions (calling methods on TextToSpeech instance, implementing OnInitListener)
# should be fine if your Activity and its methods are kept.
# No specific rules are usually needed for standard TTS usage.
# -keep class android.speech.tts.** { *; } # Usually not needed
# -keep interface android.speech.tts.** { *; } # Usually not needed


#===============================================================================
# SQLiteOpenHelper and Database operations
#===============================================================================
# Ensure your DatabaseHelper class and its public methods are kept.
-keep class com.offlinedictionary.pro.DatabaseHelper {
   public <init>(android.content.Context); # Keep constructor
   public *; # Keep all public members (methods and fields)
}


#===============================================================================
# General Good Practices / Safety Nets
#===============================================================================
# Keep classes and members annotated with @Keep from androidx.annotation.Keep
# This allows you to explicitly mark things in code that ProGuard/R8 must not touch.
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

# Preserve line numbers for better stack traces in release builds.
# This slightly increases APK size but is invaluable for debugging crashes from production.
-keepattributes SourceFile,LineNumberTable

# If you were to use ViewBinding or DataBinding (you are not currently):
# -keepattributes *Binding*
# -keep class **/*Binding { <init>(...); } # Adjust package if needed

# Example: Do not warn about classes from a library that might be missing if that library
# has optional dependencies. (You don't seem to need this now).
# -dontwarn com.example.optional.library.**
