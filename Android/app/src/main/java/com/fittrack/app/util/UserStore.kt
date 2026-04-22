package com.fittrack.app.util

import android.content.Context

/** Small SharedPreferences wrapper for the logged-in user's display info. */
object UserStore {
    private const val PREFS = "fittrack_user"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, userId: Int, username: String) {
        prefs(ctx).edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    /** Returns the stored username, or "Athlete" as a friendly fallback. */
    fun username(ctx: Context): String =
        prefs(ctx).getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: "Athlete"

    fun userId(ctx: Context): Int = prefs(ctx).getInt(KEY_USER_ID, -1)

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
