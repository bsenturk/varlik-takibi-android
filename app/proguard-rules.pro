# Crashlytics'in okunabilir stack trace üretebilmesi için.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Release'te Log çağrıları tamamen elensin — kodda BuildConfig.DEBUG kontrolü
# yerine tek noktadan. R8 gövdeyi de argüman hesaplamalarını da atar.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# kotlinx.serialization — Supabase modelleri reflection ile değil üretilmiş
# serializer ile çözülüyor; serializer'ların tutulması şart.
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static <1>$Companion Companion;
    static **$* *;
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.xptlabs.varliktakibi.**$$serializer { *; }

# Ktor / OkHttp
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Play In-App Review, derleme zamanı bir GMS anotasyonuna atıf yapıyor ama
# anotasyon sınıfı hiçbir bağımlılıkta yok. Çalışma zamanında kullanılmıyor.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
