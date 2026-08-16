# Open Flow ProGuard / R8 Rules

# Room Database & Entities
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class app.openflow.data.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Compose Runtime
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Android KeyStore & Crypto
-keep class app.openflow.secrets.** { *; }
