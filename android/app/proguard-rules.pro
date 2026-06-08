# Proguard rules for AI Network Config Diff Reviewer

# Retrofit obfuscation keeping rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Gson model serialization keeping rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room keeping rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt keeping rules
-keep class dagger.hilt.internal.GeneratedEntryPoint { *; }
-keep class * implements dagger.hilt.internal.GeneratedEntryPoint { *; }
