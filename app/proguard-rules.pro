# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.realfilters.app.data.model.** { *; }
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# AndroidSVG
-keep class com.caverock.androidsvg.** { *; }

# Compose
-dontwarn androidx.compose.**
