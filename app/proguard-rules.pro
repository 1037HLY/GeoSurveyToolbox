# Keep domain models
-keep class com.geosurvey.toolbox.domain.** { *; }
-keep class com.geosurvey.toolbox.data.** { *; }

# Keep Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Keep Kotlin reflection
-keep class kotlin.reflect.** { *; }

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
