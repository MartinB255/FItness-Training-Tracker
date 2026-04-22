package com.fittrack.app.ui.session

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.dummy.DummyData
import com.fittrack.app.data.dummy.DummyData.ExerciseStatus
import com.fittrack.app.data.dummy.DummyData.SessionLog
import com.fittrack.app.ui.dashboard.DashboardActivity
import com.fittrack.app.util.SessionTimerStore
import com.fittrack.app.util.StatusUi
import com.google.android.material.appbar.MaterialToolbar
import java.time.LocalDate

/**
 * Active workout screen. Chronometer ticks while you train; each exercise has
 * editable sets/reps/weight and a tap-to-cycle status dot. Save is only enabled
 * once every exercise has a concrete status (no TODO).
 *
 * Chronometer base is persisted to SharedPreferences so the Dashboard can show
 * the same timer if the user navigates away mid-session.
 */
class NewSessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        private const val TAG = "NewSessionActivity"
    }

    /** Mutable per-row state for the active session. */
    private data class Row(
        val exerciseName: String,
        var sets: Int,
        var reps: Int,
        var weight: Double,
        var status: ExerciseStatus = ExerciseStatus.TODO,
    )

    private lateinit var chronometer: Chronometer
    private lateinit var tvPlanName: TextView
    private lateinit var rv: RecyclerView
    private lateinit var btnSave: Button

    private val rows = mutableListOf<Row>()
    private lateinit var planName: String
    private val adapter = WorkoutAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_new_session)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        chronometer = findViewById(R.id.chronometer)
        tvPlanName = findViewById(R.id.tvPlanName)
        rv = findViewById(R.id.rvWorkout)
        btnSave = findViewById(R.id.btnSave)

        val planId = intent.getIntExtra(EXTRA_PLAN_ID, -1)
        val plan = DummyData.planById(planId)
        if (plan == null) {
            AlertDialog.Builder(this)
                .setTitle("Plan missing")
                .setMessage("Couldn't load the plan for this workout.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
            return
        }
        planName = plan.name
        tvPlanName.text = planName

        // Seed rows from the plan's default sets/reps/weight.
        rows.clear()
        rows.addAll(plan.exercises.map {
            Row(it.exerciseName, it.sets, it.reps, it.weight)
        })

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        startOrResumeTimer()
        btnSave.setOnClickListener { onSaveClicked() }
        refreshSaveEnabled()
    }

    /** Use the stored base if one exists, otherwise start a fresh session now. */
    private fun startOrResumeTimer() {
        val existingBase = SessionTimerStore.baseOrNull(this)
        val existingPlan = SessionTimerStore.planName(this)
        if (existingBase != null && existingPlan == planName) {
            chronometer.base = existingBase
        } else {
            SessionTimerStore.start(this, planName)
            chronometer.base = SessionTimerStore.baseOrNull(this)!!
        }
        chronometer.start()
    }

    override fun onPause() {
        super.onPause()
        chronometer.stop()
    }

    override fun onResume() {
        super.onResume()
        if (SessionTimerStore.isActive(this)) chronometer.start()
    }

    private fun refreshSaveEnabled() {
        btnSave.isEnabled = rows.none { it.status == ExerciseStatus.TODO }
    }

    private fun onSaveClicked() {
        val done = rows.count { it.status == ExerciseStatus.DONE }
        val skipped = rows.count { it.status == ExerciseStatus.SKIPPED }
        val refused = rows.count { it.status == ExerciseStatus.REFUSED }

        // Build logs + add to dummy history so the Sessions screen reflects it.
        val logs = rows.map {
            SessionLog(it.exerciseName, it.sets, it.reps, it.weight, it.status)
        }
        DummyData.addSession(LocalDate.now().toString(), planName, logs)

        Log.d(TAG, "Saved session: plan=$planName, logs=$logs")

        SessionTimerStore.clear(this)
        chronometer.stop()

        AlertDialog.Builder(this)
            .setTitle("Great workout!")
            .setMessage("$done/${rows.size} exercises done, $skipped skipped, $refused not done.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                startActivity(Intent(this, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                finish()
            }
            .show()
    }

    // ── RecyclerView adapter ─────────────────────────────────────────

    private inner class WorkoutAdapter : RecyclerView.Adapter<WorkoutAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvName)
            val status: TextView = view.findViewById(R.id.tvStatus)
            val sets: EditText = view.findViewById(R.id.etSets)
            val reps: EditText = view.findViewById(R.id.etReps)
            val weight: EditText = view.findViewById(R.id.etWeight)

            // Keep references so we can detach watchers on rebind.
            var setsWatcher: TextWatcher? = null
            var repsWatcher: TextWatcher? = null
            var weightWatcher: TextWatcher? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_workout, parent, false)
            return VH(v)
        }

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            holder.name.text = row.exerciseName

            // Detach previous watchers before setting text (avoid stray callbacks).
            holder.setsWatcher?.let { holder.sets.removeTextChangedListener(it) }
            holder.repsWatcher?.let { holder.reps.removeTextChangedListener(it) }
            holder.weightWatcher?.let { holder.weight.removeTextChangedListener(it) }

            holder.sets.setText(row.sets.toString())
            holder.reps.setText(row.reps.toString())
            holder.weight.setText(formatWeight(row.weight))

            holder.setsWatcher = simpleWatcher { row.sets = it.toIntOrNull() ?: row.sets }
            holder.repsWatcher = simpleWatcher { row.reps = it.toIntOrNull() ?: row.reps }
            holder.weightWatcher = simpleWatcher { row.weight = it.toDoubleOrNull() ?: row.weight }
            holder.sets.addTextChangedListener(holder.setsWatcher)
            holder.reps.addTextChangedListener(holder.repsWatcher)
            holder.weight.addTextChangedListener(holder.weightWatcher)

            StatusUi.apply(holder.status, row.status)
            holder.status.setOnClickListener {
                row.status = StatusUi.next(row.status)
                StatusUi.apply(holder.status, row.status)
                refreshSaveEnabled()
            }
        }

        private fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher =
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) { onChanged(s?.toString().orEmpty()) }
            }
    }

    private fun formatWeight(w: Double): String =
        if (w == w.toLong().toDouble()) w.toLong().toString() else w.toString()
}
