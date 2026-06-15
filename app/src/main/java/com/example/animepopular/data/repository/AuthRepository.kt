package com.example.animepopular.data.repository

import com.example.animepopular.data.preferences.AppPreferences
import com.example.animepopular.data.remote.api.MangaDexAuthService
import com.example.animepopular.data.remote.dto.ApiResult
import com.example.animepopular.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AuthRepository(
    private val authService: MangaDexAuthService,
    private val preferences: AppPreferences
) : BaseRepository() {

    /**
     * Login dengan username + password.
     * Jika berhasil → simpan token + set mode USER.
     * Jika network error → masuk offline mode USER (data lokal tetap ada).
     */
    suspend fun login(
        username: String = Constants.USERNAME,
        password: String = Constants.PASSWORD
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = authService.login(
                username     = username,
                password     = password,
                clientId     = Constants.CLIENT_ID,
                clientSecret = Constants.CLIENT_SECRET
            )
            if (response.isSuccessful) {
                val tokenData = response.body()
                if (tokenData != null && tokenData.accessToken.isNotBlank()) {
                    // ✅ Simpan token + set USER mode
                    preferences.loginAsUser(
                        username     = username,
                        accessToken  = tokenData.accessToken,
                        refreshToken = tokenData.refreshToken,
                        expiresIn    = tokenData.expiresIn
                    )
                    Timber.d("Login berhasil: $username → USER mode")
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(401, "Token kosong, coba lagi")
                }
            } else {
                val code = response.code()
                Timber.e("Login gagal HTTP $code")
                ApiResult.Error(code, when (code) {
                    401  -> "Username atau password salah"
                    400  -> "Permintaan tidak valid"
                    429  -> "Terlalu banyak percobaan, tunggu sebentar"
                    else -> "Login gagal (HTTP $code)"
                })
            }
        } catch (e: Exception) {
            Timber.e(e, "Login network error — fallback USER mode offline")
            // Network error tapi credentials valid (kita percaya user) → offline USER mode
            preferences.loginAsUser(
                username     = username,
                accessToken  = "",
                refreshToken = "",
                expiresIn    = 0
            )
            ApiResult.Success(Unit)
        }
    }

    /**
     * Masuk sebagai tamu tanpa login.
     * Set mode GUEST + userId = "__guest__".
     */
    suspend fun loginAsGuest() = withContext(Dispatchers.IO) {
        preferences.loginAsGuest()
        Timber.d("Masuk sebagai Guest")
    }

    suspend fun refreshToken(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val refreshToken = preferences.getRefreshToken()
        if (refreshToken.isNullOrBlank()) return@withContext ApiResult.Error(401, "No refresh token")
        try {
            val response = authService.refreshToken(
                refreshToken = refreshToken,
                clientId     = Constants.CLIENT_ID,
                clientSecret = Constants.CLIENT_SECRET
            )
            if (response.isSuccessful) {
                val tokenData = response.body()
                if (tokenData != null) {
                    preferences.saveTokens(
                        tokenData.accessToken,
                        tokenData.refreshToken,
                        tokenData.expiresIn
                    )
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(401, "Empty token response")
                }
            } else {
                ApiResult.Error(response.code(), "Token refresh failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Refresh error")
        }
    }

    /**
     * Logout penuh: hapus token + reset ke GUEST mode.
     * Data guest (__guest__) akan dihapus di repository.
     */
    suspend fun logout() {
        preferences.fullLogout()
        Timber.d("Logout → kembali ke GUEST mode")
    }
}