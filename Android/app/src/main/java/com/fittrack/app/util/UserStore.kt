package com.fittrack.app.util

import android.content.Context

/** Small SharedPreferences wrapper for the logged-in user's display name. */
object UserStore {
    private const val PREFS = "fittrack_user"
    private const val KEY_USERNAME = "username"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveUsername(ctx: Context, username: String) {
        prefs(ctx).edit().putString(KEY_USERNAME, username).apply()
    }

    /** Returns the stored username, or "Athlete" as a friendly fallback. */
    fun username(ctx: Context): String =
        prefs(ctx).getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: "Athlete"

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY_USERNAME).apply()
    }
}
