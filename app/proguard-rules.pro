# Keep kotlinx.serialization classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Transient <fields>;
}
-keep class **$$serializer { *; }
-keepnames class kotlinx.serialization.SerializersKt
-keepnames class kotlinx.serialization.internal.**
-keepnames class kotlinx.serialization.modules.**
-keepnames class kotlinx.serialization.encoding.**
-keepnames class kotlinx.serialization.descriptors.**

# For Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <fields>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.LaunchedEffect <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.remember <methods>;
}
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <fields>;
}
