package com.example.animepopular.util

object Constants {
    const val MANGADEX_BASE_URL  = "https://api.mangadex.org/"
    const val MANGADEX_AUTH_URL  = "https://auth.mangadex.org/"
    const val MANGADEX_COVER_URL = "https://uploads.mangadex.org/covers/"

    // ── Credentials akun PandaMind ────────────────────────────────────────────
    const val USERNAME      = "erischa"
    const val PASSWORD      = "123456789"
    const val CLIENT_ID     = "personal-client-a0fe4088-d069-4cf7-b606-c80d8d4c2be4-1fb97177"
    const val CLIENT_SECRET = "M5UTlNzqNKuzXHKpZ7hIFBVYuOwXj8db"

    // ── User Mode ─────────────────────────────────────────────────────────────
    const val USER_MODE_USER  = "USER"   // login → data persisten per user
    const val USER_MODE_GUEST = "GUEST"  // tamu  → data tidak disimpan

    // ── Guest user ID ─────────────────────────────────────────────────────────
    // Dipakai sebagai userId sementara saat mode GUEST.
    // Data dengan userId ini DIHAPUS saat logout/keluar dari guest.
    const val GUEST_USER_ID = "__guest__"

    // ── Cache ─────────────────────────────────────────────────────────────────
    const val CACHE_DURATION_MS = 5 * 60 * 1000L   // 5 menit
    const val MANGA_PAGE_SIZE   = 20

    // ── DataStore keys ────────────────────────────────────────────────────────
    const val PREF_LANGUAGE      = "language"
    const val PREF_ACCESS_TOKEN  = "access_token"
    const val PREF_REFRESH_TOKEN = "refresh_token"
    const val PREF_TOKEN_EXPIRY  = "token_expiry"
    const val PREF_USERNAME      = "username"
    const val PREF_DARK_MODE     = "dark_mode"
    const val PREF_USER_MODE     = "user_mode"   // USER | GUEST
    const val PREF_USER_ID       = "user_id"     // username yg sedang login

    // ── Cover thumbnail ───────────────────────────────────────────────────────
    const val COVER_THUMB_256 = ".256.jpg"
    const val COVER_THUMB_512 = ".512.jpg"
}