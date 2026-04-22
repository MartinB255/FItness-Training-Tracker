package com.fittrack.app.ui.session

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.model.CreateLogRequest
import com.fittrack.app.data.model.CreateSessionRequest
import com.fittrack.app.data.model.PlanExercise
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.dashboard.DashboardActivity
import com.fittrack.app.util.ExerciseStatus
import com.fittrack.app.util.SessionTimerStore
import com.fittrack.app.util.StatusUi
import com.fittrack.app.util.formatWeight
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Active workout screen. Chronometer ticks while the user trains; each exercise has
 * editable sets/reps/weight and a tap-to-cycle status dot. Save is only enabled
 * once every exercise has a concrete status (no TODO).
 *
 * On save we POST /sessions/ with nested exercise_logs and the total duration
 * in seconds (pulled from the chronometer).
 */
class NewSessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
    }

    /** Mutable per-row state for the active session. */
    private data class Row(
        val planExercise: PlanExercise,
        var sets: Int,
        var reps: Int,
        var weight: Double,
        var status: ExerciseStatus = ExerciseStatus.TODO,
    )

    private lateinit var chronometer: Chronometer
    private lateinit var tvPlanName: TextView
    private lateinit var rv: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private val rows = mutableListOf<Row>()
    private var planId: Int = -1
    private var planName: String = ""
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
        btnCancel = findViewById(R.id.btnCancel)

        planId = intent.getIntExtra(EXTRA_PLAN_ID, -1)
        if (planId < 0) {
            finishWithMissingPlan()
            return
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnSave.setOnClickListener { onSaveClicked() }
        btnSave.isEnabled = false
        btnCancel.setOnClickListener { confirmCancel() }

        loadPlan()
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel workout?")
            .setMessage("The current session will be discarded.")
            .setPositiveButton("Discard") { _, _ ->
                chronometer.stop()
                SessionTimerStore.clear(this)
                startActivity(Intent(this, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                finish()
            }
            .setNegativeButton("Keep training", null)
            .show()
    }

    private fun loadPlan() {
        lifecycleScope.launch {
            FitTrackRepository.getPlan(planId)
                .onSuccess { plan ->
                    planName = plan.name
                    tvPlanName.text = planName
                    rows.clear()
                    rows.addAll(plan.planExercises.map {
                        Row(
                            planExercise = it,
                            sets = it.sets,
                            reps = it.reps,
                            weight = it.weight.toDoubleOrNull() ?: 0.0,
                        )
                    })
                    adapter.notifyDataSetChanged()
                    startOrResumeTimer()
                    refreshSaveEnabled()
                }
                .onFailure {
                    Toast.makeText(this@NewSessionActivity,
                        "Couldn't load plan: ${it.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    private fun finishWithMissingPlan() {
        AlertDialog.Builder(this)
            .setTitle("Plan missing")
            .setMessage("Couldn't load the plan for this workout.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    /** Use the stored base if one exists for this plan, otherwise start fresh. */
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
        btnSave.isEnabled = rows.isNotEmpty() && rows.none { it.status == ExerciseStatus.TODO }
    }

    private fun onSaveClicked() {
        val elapsedMs = SystemClock.elapsedRealtime() - chronometer.base
        val durationSeconds = (elapsedMs / 1000L).toInt().coerceAtLeast(0)
        btnSave.isEnabled = false
        chronometer.stop()

        val logs = rows.map { row ->
            CreateLogRequest(
                exercise = row.planExercise.exercise,
                exerciseName = row.planExercise.exerciseName,
                sets = row.sets,
                reps = row.reps,
                weight = formatWeight(row.weight),
                status = row.status.apiValue ?: "done",
            )
        }
        val body = CreateSessionRequest(
            trainingPlan = planId,
            date = LocalDate.now().toString(),
            durationSeconds = durationSeconds,
            completed = true,
            exerciseLogs = logs,
        )

        lifecycleScope.launch {
            FitTrackRepository.createSession(body)
                .onSuccess {
                    syncPlanDefaults()
                    onSessionSaved()
                }
                .onFailure {
                    btnSave.isEnabled = true
                    chronometer.start()
                    Toast.makeText(this@NewSessionActivity,
                        "Couldn't save session: ${it.message}",
                        Toast.LENGTH_LONG).show()
                }
        }
    }

    /**
     * Persist the edited sets/reps/weight back to the plan so the next session
     * starts from the latest numbers. Only rows marked DONE update the plan —
     * skipped/not-done entries keep the old defaults.
     */
    private suspend fun syncPlanDefaults() = coroutineScope {
        rows.mapNotNull { row ->
            if (row.status != ExerciseStatus.DONE) return@mapNotNull null
            val original = row.planExercise
            val newWeight = formatWeight(row.weight)
            if (row.sets == original.sets &&
                row.reps == original.reps &&
                newWeight == original.weight) return@mapNotNull null
            async {
                FitTrackRepository.updatePlanExercise(
                    id = original.id,
                    sets = row.sets,
                    reps = row.reps,
                    weight = newWeight,
                )
            }
        }.awaitAll()
    }

    private fun onSessionSaved() {
        val done = rows.count { it.status == ExerciseStatus.DONE }
        val skipped = rows.count { it.status == ExerciseStatus.SKIPPED }

        SessionTimerStore.clear(this)

        val message = buildString {
            append("$done/${rows.size} exercises done")
            if (skipped > 0) append(", $skipped skipped")
            append(".")
        }
        AlertDialog.Builder(this)
            .setTitle("Great workout!")
            .setMessage(message)
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

            // Kept so we can detach watchers when rebinding (avoid stale callbacks).
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
            holder.name.text = row.planExercise.exerciseName

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

}
