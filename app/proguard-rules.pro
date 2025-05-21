#===============================================================================
# Standard Android Optimizations (already included by getDefaultProguardFile)
#===============================================================================
# You typically don't need to repeat rules from proguard-android-optimize.txt,
# but this section is for general awareness.

#===============================================================================
# Keep Application, Activities, Services, BroadcastReceivers, ContentProviders
#===============================================================================
# Keep all public classes that extend Application, Activity, Service, etc.
# and their public/protected members.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends androidx.core.app.JobIntentService
-keep public class * extends androidx.lifecycle.ViewModel

# Keep specific main activity if not covered above (though it should be)
# Replace com.offlinedictionary.pro with your actual package name if different
-keep public class com.offlinedictionary.pro.MainActivity {
    public *;
}

# Keep any custom views that are referenced from XML layouts if their constructors
# are not found. (You don't have custom views yet, but good to know)
# -keep public class com.yourpackage.YourCustomView {
#    public <init>(android.content.Context);
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
# }

#===============================================================================
# Keep Data Classes / Models (especially if used with reflection/serialization)
#===============================================================================
# Replace com.offlinedictionary.pro with your actual package name
# Keep all fields and constructors of your data classes.
-keep class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keep class com.offlinedictionary.pro.DefinitionDetail { *; }
# Add any other model/data classes here

# If using Gson for more complex object serialization/deserialization:
# Keep constructors and fields for classes specifically used with Gson.
# -keepclassmembers class com.offlinedictionary.pro.** {
#   <init>(...); # Keep constructors
#   private <fields>; # Keep fields
# }
# -keepattributes Signature # Needed for generic types with Gson
# -keepattributes InnerClasses # If you have inner classes used with Gson

# More specific Gson rule for data classes (often sufficient):
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; } # Keep TypeToken if used
-keep class sun.misc.Unsafe { *; } # Gson might use Unsafe internally on some JVMs/Android versions

# For your specific Gson usage (parsing List<String> from DB):
# The TypeToken rule above and keeping your DefinitionDetail fields should suffice.


#===============================================================================
# Keep Enums (if used with valueOf() or values())
#===============================================================================
# -keepclassmembers enum * {
#     public static **[] values();
#     public static ** valueOf(java.lang.String);
# }

#===============================================================================
# Keep Parcelable classes
#===============================================================================
# -keep class * implements android.os.Parcelable {
#   public static final android.os.Parcelable$Creator *;
#   private static final long serialVersionUID; # If also Serializable
# }
# -keepclassmembers class * implements android.os.Parcelable {
#   public void writeToParcel(android.os.Parcel,int);
# }


#===============================================================================
# Keep Callback Methods (e.g., View onClick in XML, JNI)
#===============================================================================
# If you use android:onClick="methodName" in your XML layouts:
# -keepclassmembers class * extends android.app.Activity {
#    public void methodName(android.view.View);
# }
# (You are using programmatic listeners, so this is not strictly needed for you now)


#===============================================================================
# Kotlin Specific Rules
#===============================================================================
# Keep Kotlin metadata. This is very important for Kotlin reflection and other features.
-keepattributes RuntimeVisibleAnnotations,Signature,KotlinIdentifiers,Kotlin ब्रह्मांड_प्रकाशित
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect. ανέκφραστοFunction { *; } # For coroutines and lambdas

# Keep default_നോട്ടുകൾ classes and methods (often generated for default arguments)
-keepclassmembers class **$DefaultImpls {
    *;
}

# Keep companion objects' members if they are accessed via reflection or from native code
# -keepclassmembers class * {
#    static <fields>;
#    static <methods>;
# }

# Keep suspend functions for coroutines (generally handled by Kotlin metadata, but can be explicit)
# -keepclassmembers class ** {
#    public static java.lang.Object *(kotlin.coroutines.Continuation);
# }
# -keepnames class kotlinx.coroutines.flow.* # Keep names for coroutine flow classes


#===============================================================================
# AndroidX and Material Components Library Rules
#===============================================================================
# These libraries usually include their own consumer ProGuard rules.
# However, if you experience issues specifically with these, you might need to add rules.
# For example, for Material Components, if a specific component isn't working:
# -keep class com.google.android.material.** { *; }
# -keep interface com.google.android.material.** { *; }
# But this is usually overly broad and not needed.


#===============================================================================
# Specific Rule for BuildConfig (used in your DatabaseHelper)
#===============================================================================
# Replace com.offlinedictionary.pro with your actual package name
-keep class com.offlinedictionary.pro.BuildConfig { *; }


#===============================================================================
# TextToSpeech (TTS)
#===============================================================================
# Generally, TTS doesn't require special ProGuard rules if used directly.
# However, if any callbacks or internal classes were being obfuscated causing issues:
# -keep class android.speech.tts.** { *; }
# -keep interface android.speech.tts.** { *; }


#===============================================================================
# SQLiteOpenHelper and Database operations
#===============================================================================
# Your DatabaseHelper class and its methods that are public or accessed by the system.
# The rule for `* extends android.app.Activity` implicitly covers keeping public methods
# if your `DatabaseHelper` is instantiated there.
# If `DatabaseHelper` methods were private and accessed via reflection (not your case),
# they would need specific rules.
# Replace com.offlinedictionary.pro with your actual package name
-keep class com.offlinedictionary.pro.DatabaseHelper {
   public <init>(android.content.Context); # Keep constructor
   public *; # Keep all public members (methods and fields)
}


#===============================================================================
# General good practices
#===============================================================================
# Do not warn about classes not found if they are optional dependencies
# (e.g., if a library can work with or without another library)
# -dontwarn com.example.optional.library.**

# Keep names of classes and members annotated with @Keep from androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

# Preserve line numbers for better stack traces in release builds (optional)
# This slightly increases APK size but is invaluable for debugging crashes from production.
-keepattributes SourceFile,LineNumberTable

# If you use any JNI (Java Native Interface), you'll need to keep native methods.
# -keepclasseswithmembernames class * {
#    native <methods>;
# }
