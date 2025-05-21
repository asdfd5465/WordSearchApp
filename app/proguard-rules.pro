# Keep data classes used by Gson for database JSON parsing
-keep class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keep class com.offlinedictionary.pro.DefinitionDetail { *; }

# Keep class members for these data classes that Gson needs
-keepclassmembers class com.offlinedictionary.pro.WordDefinitionEntry { *; }
-keepclassmembers class com.offlinedictionary.pro.DefinitionDetail { *; }

# If you are using @SerializedName, ensure those fields are kept.
# For data classes, keeping all members (*) is often easiest.

# General rules for libraries if needed (though usually handled by their own consumer Proguard rules)
# -keep class com.google.gson.** { *; } # Usually not needed as Gson often includes its own rules
