package com.fittrack.app.util

import android.graphics.Color
import android.widget.TextView

/** Helpers for rendering and cycling the per-exercise status dot used on the workout screen. */
object StatusUi {

    /** Next status after a tap. Once out of TODO, cycles DONE → SKIPPED → NOT_DONE → DONE. */
    fun next(current: ExerciseStatus): ExerciseStatus = when (current) {
        ExerciseStatus.TODO -> ExerciseStatus.DONE
        ExerciseStatus.DONE -> ExerciseStatus.SKIPPED
        ExerciseStatus.SKIPPED -> ExerciseStatus.NOT_DONE
        ExerciseStatus.NOT_DONE -> ExerciseStatus.DONE
    }

    /** Paint the circle background + symbol for the given status onto a TextView. */
    fun apply(tv: TextView, status: ExerciseStatus) {
        val (colorHex, symbol) = when (status) {
            ExerciseStatus.TODO -> "#757575" to ""
            ExerciseStatus.DONE -> "#4CAF50" to "✓"      // ✓
            ExerciseStatus.SKIPPED -> "#9C27B0" to "—"   // —
            ExerciseStatus.NOT_DONE -> "#F44336" to "✕"  // ✕
        }
        tv.background?.mutate()?.setTint(Color.parseColor(colorHex))
        tv.text = symbol
    }
}
