# ======================================================================
# PeakConnect Proguard / R8 Configuration
# ======================================================================

# --- Kotlinx Serialization ---
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * { *; }
-keep class com.peakconnect.app.data.dto.** { *; }
-keep class com.peakconnect.app.domain.model.** { *; }
-keep class com.peakconnect.app.data.entity.** { *; }
-keep class com.peakconnect.app.core.images.** { *; }
-keep class com.peakconnect.app.core.network.** { *; }

# --- Retrofit & OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Hilt & Dagger ---
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# --- Razorpay Checkout SDK ---
# Published rules from Razorpay Android Standard Checkout SDK
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** { *; }
-optimizations !class/merging/vertical*,!class/merging/horizontal*
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# --- OSMDroid ---
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# --- Coil 3 ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- Paging 3 & WorkManager ---
-keep class * extends androidx.paging.PagingSource { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class androidx.work.WorkerParameters { *; }

