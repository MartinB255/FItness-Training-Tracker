package com.fittrack.app.ui.plans

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.session.NewSessionActivity
import kotlinx.coroutines.launch

/**
 * Reusable "pick a training plan, then start a workout" flow.
 * Used by the Dashboard's "Start New Workout" button and the Sessions screen.
 */
object PlanPicker {

    fun show(activity: AppCompatActivity) {
        activity.lifecycleScope.launch {
            FitTrackRepository.getPlans()
                .onSuccess { plans ->
                    if (plans.isEmpty()) {
                        AlertDialog.Builder(activity)
                            .setTitle("No plans yet")
                            .setMessage("Create a training plan before starting a workout.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@onSuccess
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
                .onFailure {
                    Toast.makeText(
                        activity,
                        "Couldn't load plans: ${it.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }
}
