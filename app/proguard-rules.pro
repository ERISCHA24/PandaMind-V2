-keep class com.example.animepopular.data.remote.dto.** { *; }
-keep class com.example.animepopular.data.local.entity.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# Kotlin serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep @kotlinx.serialization.Serializable class * { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil
-dontwarn coil.**

# Timber
-dontwarn timber.log.**