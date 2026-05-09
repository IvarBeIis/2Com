-keep class cz.twocom.core.crypto.** { *; }
-keep class cz.twocom.core.transport.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn org.bouncycastle.**
