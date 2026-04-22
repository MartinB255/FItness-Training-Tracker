package com.fittrack.app.ui.plans

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fittrack.app.data.dummy.DummyData
import com.fittrack.app.ui.session.NewSessionActivity

/**
 * Reusable "pick a training plan, then start a workout" flow.
 * Used by the Dashboard's "Start New Workout" button and the
 * Session history screen's "+ New Session" button.
 */
object PlanPicker {

    fun show(activity: AppCompatActivity) {
        val plans = DummyData.plans
        if (plans.isEmpty()) {
            AlertDialog.Builder(activity)
                .setTitle("No plans yet")
                .setMessage("Create a training plan before starting a workout.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val names = plans.map { it.name }.toTypedArray()
        AlertDialog.Builder(activity)
            .setTitle("Start Workout — pick a plan")
            .setItems(names) { _, which ->
                activity.startActivity(
                    Intent(activity, NewSessionActivity::class.java)
                        .putExtra(NewSessionActivity.EXTRA_PLAN_ID, plans[which].id)
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
