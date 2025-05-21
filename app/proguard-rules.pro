# Default ProGuard rules that come with Android Studio are good starting points
# but often need additions. (These are usually included by getDefaultProguardFile)

# Keep application classes that are entry points (Activities, Services, etc.)
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
-keepclassmembers class * extends android.app.Activity {
   public protected *;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep callback methods referenced from XML layout files
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
    void *(android.view.View);
}
-keepclassmembers class * implements android.view.View$OnClickListener {
    void onClick(android.view.View);
}


# --- Gson ---
# Replace com.offlinedictionary.pro with your actual package name if different.
-keep class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keep class com.offlinedictionary.pro.DefinitionDetail { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.google.gson.reflect.TypeToken {
    <init>();
}


# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepclassmembernames class kotlinx.coroutines.flow.internal.AbortFlowException { *; }
-keepclassmembernames class kotlinx.coroutines.JobCancellationException { *; }
-keepclassmembernames class kotlinx.coroutines.flow.internal.ChildCancelledException { *; }
-keepclassmembernames class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    kotlin.coroutines.Continuation getCompletion();
    java.lang.Object getResult();
}
-keepclassmembernames class kotlin.coroutines.jvm.internal.ContinuationImpl {
    java.lang.Object _result;
    int _controller;
    int label;
    java.lang.Object getResult();
    kotlin.coroutines.Continuation getCompletion();
}
-dontwarn kotlin.Unit
-dontwarn kotlinx.coroutines.flow.**


# --- AndroidX and Material Components ---
# These libraries usually provide their own consumer ProGuard rules.


# --- General good practices ---
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,InnerClasses,EnclosingMethod,Signature,Exceptions,Deprecated
