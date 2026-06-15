package com.example.animepopular.data.remote.api

import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.util.Constants
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys  = true
        isLenient          = true
        coerceInputValues  = true
    }

    private val contentType = "application/json".toMediaType()

    // ── Auth interceptor ──────────────────────────────────────────────────────

    private fun authInterceptor(preferences: AppPreferences) = Interceptor { chain ->
        val token = runBlocking { preferences.getAccessToken() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ── OkHttp clients ────────────────────────────────────────────────────────

    fun provideApiOkHttpClient(preferences: AppPreferences): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor(preferences))
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // Images can be slow
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    fun provideAuthOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    // ── Service factories ─────────────────────────────────────────────────────

    fun provideMangaDexApiService(preferences: AppPreferences): MangaDexApiService =
        Retrofit.Builder()
            .baseUrl(Constants.MANGADEX_BASE_URL)
            .client(provideApiOkHttpClient(preferences))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MangaDexApiService::class.java)

    fun provideMangaDexAuthService(): MangaDexAuthService =
        Retrofit.Builder()
            .baseUrl(Constants.MANGADEX_AUTH_URL)
            .client(provideAuthOkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MangaDexAuthService::class.java)

    fun provideMangaDexAtHomeService(preferences: AppPreferences): MangaDexAtHomeService =
        Retrofit.Builder()
            .baseUrl(Constants.MANGADEX_BASE_URL)
            .client(provideApiOkHttpClient(preferences))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MangaDexAtHomeService::class.java)
}