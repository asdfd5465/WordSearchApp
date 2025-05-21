# Default ProGuard rules that come with Android Studio are good starting points
# but often need additions. (These are usually included by getDefaultProguardFile)

# Keep application classes that are entry points (Activities, Services, etc.)
# This is generally handled by AGP, but being explicit can help.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends androidx.core.app.CoreComponentFactory
-keep public class * extends androidx.lifecycle.ViewModel
-keep public class * extends androidx.room.RoomDatabase

# Keep custom views and their constructors
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep all public and protected methods and fields of Activities
# to ensure lifecycle methods and methods referenced from XML are kept.
-keepclassmembers class * extends android.app.Activity {
   public protected *;
}

# Keep enums that might be used by name (e.g., with Gson or serialization)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep callback methods referenced from XML layout files (e.g., android:onClick)
# This is a general rule. If you have specific onClick methods, you might list them.
# However, modern AGP often handles this well with ViewBinding or if methods are public.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
    void *(android.view.View);
}
-keepclassmembers class * implements android.view.View$OnClickListener {
    void onClick(android.view.View);
}


# --- Gson ---
# Keep your data model classes that are serialized/deserialized by Gson.
# Replace com.offlinedictionary.pro with your actual package name if different.
-keep class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keep class com.offlinedictionary.pro.DefinitionDetail { *; }

# Keep fields in classes annotated with Gson's @SerializedName,
# and the annotation itself if R8 is configured to remove annotations.
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep constructors of GSON types
-keepclassmembers class com.google.gson.reflect.TypeToken {
    <init>(); # Keep the default constructor
}
-keepclassmembers class * {
    # If you use @Expose, keep fields and methods annotated with it
    # @com.google.gson.annotations.Expose <fields>;
    # @com.google.gson.annotations.Expose <methods>;
}

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepclassmembernames class kotlinx.coroutines.flow.internal.AbortFlowException { *; }
-keepclassmembernames class kotlinx.coroutines.JobCancellationException { *; }
-keepclassmembernames class kotlinx.coroutines.flow.internal.ChildCancelledException { *; }
-keepclassmembernames class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    kotlin.coroutines.Continuation get jakościowo_completion();
    java.lang.Object get jakościowo_result();
}
-keepclassmembernames class kotlin.coroutines.jvm.internal.ContinuationImpl {
    java.lang.Object _result;
    int _controller;
    int label;
    java.lang.Object get jakościowo_result();
    kotlin.coroutines.Continuation get jakościowo_completion();
}
-dontwarn kotlin.Unit # Don't warn about kotlin.Unit not being found. It's a language construct.
-dontwarn kotlinx.coroutines.flow.** # Suppress warnings for internal coroutine flow classes if any

# --- AndroidX and Material Components ---
# These libraries usually provide their own consumer ProGuard rules.
# However, if you encounter specific issues, you might need to add rules here.
# For example, for ViewBinding (though you are not explicitly using it with findViewById):
# -keepclassmembers class **.databinding.*Binding {
#    public <fields>;
#    public <methods>;
# }

# Keep certain Parcelable implementations if using them directly and they are obfuscated
# -keep class * implements android.os.Parcelable {
#   public static final android.os.Parcelable$Creator *;
# }
# -keepclassmembers class **.R$* {
#    public static <fields>;
# }

# For androidx.appcompat and other common libraries, their included rules are usually sufficient.
# If you use reflection extensively on library classes, you might need to -keep them.

# --- General good practices ---
# Keep names of classes used in JNI (if any)
# -keepclasseswithmembernames class * {
#    native <methods>;
# }

# Keep annotations (some libraries rely on them at runtime)
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,InnerClasses,EnclosingMethod,Signature,Exceptions,Deprecated

# If using reflection, you might need to be more specific about keeping classes/methods/fields.
# For example: -keep public class MyClass { public void myReflectedMethod(); }

# Don't warn about missing classes from optional dependencies if you know they are not used.
# -dontwarn com.example.OptionalDependencyClass

# --- SQLite ---
# Generally, SQLite and Room don't require extensive ProGuard rules if you're using Room annotations
# correctly and not reflecting on database classes directly.
# If you were using raw SQLite and reflecting on column names, you'd need to keep those.

# Ensure your Application class (if you have a custom one) is kept.
# -keep class com.offlinedictionary.pro.MyApplication { *; } # If you have MyApplication.kt

# This is a robust starting point. Specific issues might require adding more targeted rules.
# Always test thoroughly after enabling minification.
