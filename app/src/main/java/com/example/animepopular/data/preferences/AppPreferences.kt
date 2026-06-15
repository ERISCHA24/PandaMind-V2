package com.example.animepopular.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.animepopular.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val LANGUAGE_KEY      = stringPreferencesKey(Constants.PREF_LANGUAGE)
        val ACCESS_TOKEN_KEY  = stringPreferencesKey(Constants.PREF_ACCESS_TOKEN)
        val REFRESH_TOKEN_KEY = stringPreferencesKey(Constants.PREF_REFRESH_TOKEN)
        val TOKEN_EXPIRY_KEY  = longPreferencesKey(Constants.PREF_TOKEN_EXPIRY)
        val USERNAME_KEY      = stringPreferencesKey(Constants.PREF_USERNAME)
        val DARK_MODE_KEY     = booleanPreferencesKey(Constants.PREF_DARK_MODE)
        val IS_LOGGED_IN_KEY  = booleanPreferencesKey("is_logged_in")
        // ── NEW ───────────────────────────────────────────────────────────────
        val USER_MODE_KEY     = stringPreferencesKey(Constants.PREF_USER_MODE)
        val USER_ID_KEY       = stringPreferencesKey(Constants.PREF_USER_ID)
    }

    // ── Language ──────────────────────────────────────────────────────────────

    val languageFlow: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[LANGUAGE_KEY] ?: "en" }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = lang }
    }

    // ── Auth Tokens ───────────────────────────────────────────────────────────

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY]  = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
            prefs[TOKEN_EXPIRY_KEY]  = System.currentTimeMillis() + (expiresIn * 1000L)
            prefs[IS_LOGGED_IN_KEY]  = true
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[ACCESS_TOKEN_KEY]

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[REFRESH_TOKEN_KEY]

    suspend fun isTokenExpired(): Boolean {
        val expiry = context.dataStore.data.first()[TOKEN_EXPIRY_KEY] ?: 0L
        return System.currentTimeMillis() >= expiry
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(TOKEN_EXPIRY_KEY)
            prefs[IS_LOGGED_IN_KEY] = false
        }
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[IS_LOGGED_IN_KEY] ?: false }

    // ── Username ──────────────────────────────────────────────────────────────

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[USERNAME_KEY] = username }
    }

    val usernameFlow: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USERNAME_KEY] ?: "Guest" }

    // ── Dark Mode ──────────────────────────────────────────────────────────────

    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DARK_MODE_KEY] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    // ── User Mode (USER | GUEST) ──────────────────────────────────────────────

    /** Flow yang emit Constants.USER_MODE_USER atau Constants.USER_MODE_GUEST */
    val userModeFlow: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_MODE_KEY] ?: Constants.USER_MODE_GUEST }

    suspend fun setUserMode(mode: String) {
        context.dataStore.edit { it[USER_MODE_KEY] = mode }
    }

    suspend fun getUserMode(): String =
        context.dataStore.data.first()[USER_MODE_KEY] ?: Constants.USER_MODE_GUEST

    // ── User ID (username yang sedang aktif) ──────────────────────────────────

    /** Flow userId — username login atau "__guest__" */
    val userIdFlow: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_ID_KEY] ?: Constants.GUEST_USER_ID }

    suspend fun getUserId(): String =
        context.dataStore.data.first()[USER_ID_KEY] ?: Constants.GUEST_USER_ID

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { it[USER_ID_KEY] = userId }
    }

    // ── Login as USER ─────────────────────────────────────────────────────────

    /**
     * Dipanggil setelah login MangaDex berhasil.
     * Menyimpan token + set mode USER + set userId = username.
     */
    suspend fun loginAsUser(
        username: String,
        accessToken: String,
        refreshToken: String,
        expiresIn: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY]  = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
            prefs[TOKEN_EXPIRY_KEY]  = System.currentTimeMillis() + (expiresIn * 1000L)
            prefs[IS_LOGGED_IN_KEY]  = true
            prefs[USERNAME_KEY]      = username
            prefs[USER_MODE_KEY]     = Constants.USER_MODE_USER
            prefs[USER_ID_KEY]       = username
        }
    }

    // ── Login as GUEST ────────────────────────────────────────────────────────

    /**
     * Dipanggil saat user memilih "Lanjutkan sebagai Tamu".
     * Tidak ada token, userId = "__guest__".
     */
    suspend fun loginAsGuest() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN_KEY] = false
            prefs[USERNAME_KEY]     = "Guest"
            prefs[USER_MODE_KEY]    = Constants.USER_MODE_GUEST
            prefs[USER_ID_KEY]      = Constants.GUEST_USER_ID
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    suspend fun fullLogout() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(TOKEN_EXPIRY_KEY)
            prefs[IS_LOGGED_IN_KEY] = false
            prefs[USERNAME_KEY]     = "Guest"
            prefs[USER_MODE_KEY]    = Constants.USER_MODE_GUEST
            prefs[USER_ID_KEY]      = Constants.GUEST_USER_ID
        }
    }
}