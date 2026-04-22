package com.fittrack.app.util

/**
 * Per-exercise status inside a workout session.
 *
 * TODO is a transient UI-only state — an exercise that hasn't been marked yet.
 * The other three values are what gets sent to the backend (mapped via [apiValue]),
 * matching ExerciseLog.STATUS_CHOICES on the Django side.
 */
enum class ExerciseStatus(val apiValue: String?) {
    TODO(null),          // not yet set by the user — not persisted
    DONE("done"),
    SKIPPED("skipped"),
    NOT_DONE("not_done");

    companion object {
        /** Parse the backend's status string. Defaults to DONE if missing/unknown. */
        fun fromApi(value: String?): ExerciseStatus = when (value) {
            "done" -> DONE
            "skipped" -> SKIPPED
            "not_done" -> NOT_DONE
            else -> DONE
        }
    }
}
