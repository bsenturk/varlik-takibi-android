package com.xptlabs.varliktakibi.di

import com.xptlabs.varliktakibi.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    /**
     * Uygulama tamamen anonim: Auth ve Realtime modülleri kurulmuyor.
     * Supabase'e giden tek yazma push token RPC'leri.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Functions)
        httpEngine = OkHttp.create()

        // Backend ileride assets_prices'a yeni kolon eklerse istemci düşmesin.
        defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
            Json { ignoreUnknownKeys = true }
        )
    }
}
