package com.fittrack.app.util

import android.content.Context
import android.os.SystemClock

/**
 * Persists the active workout's Chronometer base across screens via SharedPreferences.
 * Stores a SystemClock.elapsedRealtime() value so both the NewSession screen and
 * the Dashboard can resume the same timer.
 */
object SessionTimerStore {
    private const val PREFS = "fittrack_session"
    private const val KEY_BASE = "chronometer_base"
    private const val KEY_PLAN = "plan_name"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Start (or restart) the session clock right now. */
    fun start(ctx: Context, planName: String) {
        prefs(ctx).edit()
            .putLong(KEY_BASE, SystemClock.elapsedRealtime())
            .putString(KEY_PLAN, planName)
            .apply()
    }

    /** Clear the session clock — session has ended or was discarded. */
    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    /** null when no session is active. */
    fun baseOrNull(ctx: Context): Long? {
        val p = prefs(ctx)
        return if (p.contains(KEY_BASE)) p.getLong(KEY_BASE, 0L) else null
    }

    fun planName(ctx: Context): String? = prefs(ctx).getString(KEY_PLAN, null)

    fun isActive(ctx: Context): Boolean = baseOrNull(ctx) != null
}
