import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Supabase ve RevenueCat anahtarları kaynak koda gömülmez; local.properties'ten
// okunur (dosya .gitignore'da). CI için aynı isimli ortam değişkenleri de kabul edilir.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String, fallback: String = ""): String =
    localProps.getProperty(key) ?: System.getenv(key) ?: fallback

android {
    namespace = "com.xptlabs.varliktakibi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xptlabs.varliktakibi"
        minSdk = 24
        targetSdk = 36
        // Play'e gönderilen en yüksek sürüm 8 (reddedildi); üstüne çıkılıyor.
        versionCode = 9
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "BUILD_TYPE_NAME", "\"release\"")

            // AdMob Production IDs
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-2545255000258244/9897563908\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-2545255000258244/2703290545\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-2545255000258244/4022429115\"")
        }

        debug {
            isDebuggable = true

            buildConfigField("String", "BUILD_TYPE_NAME", "\"debug\"")

            // AdMob Test IDs for debug
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // minSdk 24'te java.time kullanabilmek için. Tüm tarih matematiği
        // (gün başlangıcı, aralık gezinme, ISO8601 ayrıştırma) buna dayanıyor.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room şemasını dışa aktar — migration testleri ve şema diff'i için gerekli.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compiler)

    // Persistence
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Supabase (salt okunur piyasa verisi + push token RPC)
    implementation(platform(libs.supabase.bom))
    implementation(libs.bundles.supabase)

    // Abonelik
    implementation(libs.revenuecat.purchases)

    // Play In-App Review
    implementation(libs.play.review.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Permissions
    implementation(libs.accompanist.permissions)

    // WorkManager (günlük anlık görüntü işi)
    implementation(libs.bundles.workmanager)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase.core)

    // AdMob
    implementation(libs.play.services.ads)


    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
