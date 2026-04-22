package com.fittrack.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.fittrack.app.R
import com.fittrack.app.data.dummy.DummyData
import com.fittrack.app.ui.auth.LoginActivity
import com.fittrack.app.ui.exercises.ExercisesActivity
import com.fittrack.app.ui.plans.PlanPicker
import com.fittrack.app.ui.plans.PlansActivity
import com.fittrack.app.ui.progress.ProgressActivity
import com.fittrack.app.ui.session.SessionDetailActivity
import com.fittrack.app.ui.session.SessionListActivity
import com.fittrack.app.util.SessionTimerStore
import com.fittrack.app.util.UserStore
import com.google.android.material.button.MaterialButton

/**
 * Dashboard — home screen after login.
 *
 * From top to bottom:
 *   Greeting, optional active-session chronometer, 3-stat row, "Start New Workout",
 *   Last Workout card, 2×2 navigation grid, red logout button.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView

    private lateinit var sessionTimerBar: LinearLayout
    private lateinit var tvTimerPlan: TextView
    private lateinit var chronometer: Chronometer

    private lateinit var cardLastWorkout: LinearLayout
    private lateinit var tvLastPlan: TextView
    private lateinit var tvLastMeta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_dashboard)

        tvGreeting = findViewById(R.id.tvGreeting)

        sessionTimerBar = findViewById(R.id.sessionTimerBar)
        tvTimerPlan = findViewById(R.id.tvTimerPlan)
        chronometer = findViewById(R.id.chronometer)

        cardLastWorkout = findViewById(R.id.cardLastWorkout)
        tvLastPlan = findViewById(R.id.tvLastPlan)
        tvLastMeta = findViewById(R.id.tvLastMeta)

        findViewById<MaterialButton>(R.id.btnStartWorkout).setOnClickListener {
            PlanPicker.show(this)
        }
        findViewById<Button>(R.id.btnPlans).setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }
        findViewById<Button>(R.id.btnExercises).setOnClickListener {
            startActivity(Intent(this, ExercisesActivity::class.java))
        }
        findViewById<Button>(R.id.btnSessions).setOnClickListener {
            startActivity(Intent(this, SessionListActivity::class.java))
        }
        findViewById<Button>(R.id.btnProgress).setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }
    }

    override fun onResume() {
        super.onResume()
        tvGreeting.text = "Hey ${UserStore.username(this)}!"
        refreshSessionTimer()
        refreshLastWorkout()
    }

    override fun onPause() {
        super.onPause()
        chronometer.stop()
    }

    /** Show the chronometer bar only while a workout session is running. */
    private fun refreshSessionTimer() {
        val base = SessionTimerStore.baseOrNull(this)
        if (base == null) {
            sessionTimerBar.visibility = View.GONE
            chronometer.stop()
            return
        }
        sessionTimerBar.visibility = View.VISIBLE
        tvTimerPlan.text = SessionTimerStore.planName(this) ?: "Active session"
        chronometer.base = base
        chronometer.start()
    }

    /** Fill in the Last Workout card from dummy history; hide when there is none. */
    private fun refreshLastWorkout() {
        val last = DummyData.sessions.firstOrNull()
        if (last == null) {
            cardLastWorkout.visibility = View.GONE
            return
        }
        cardLastWorkout.visibility = View.VISIBLE
        tvLastPlan.text = last.planName
        tvLastMeta.text = "${last.date}  •  ${last.completed}/${last.total} exercises completed"
        cardLastWorkout.setOnClickListener {
            startActivity(
                Intent(this, SessionDetailActivity::class.java)
                    .putExtra(SessionDetailActivity.EXTRA_SESSION_ID, last.id)
            )
        }
    }

    private fun logout() {
        SessionTimerStore.clear(this)
        UserStore.clear(this)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
