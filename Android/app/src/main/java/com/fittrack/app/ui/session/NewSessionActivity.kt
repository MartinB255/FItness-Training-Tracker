package com.fittrack.app.ui.session

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.model.*
import com.fittrack.app.data.repository.FitTrackRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Screen for logging a new workout session.
 *
 * Flow:
 * 1. User picks a training plan (or freestyle)
 * 2. Exercises from the plan are pre-loaded
 * 3. User adjusts sets/reps/weight for each exercise
 * 4. User hits "Save Workout" to send everything to the API
 */
class NewSessionActivity : AppCompatActivity() {

    private lateinit var spinnerPlan: Spinner
    private lateinit var rvLogs: RecyclerView
    private lateinit var btnAddLog: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private var plans = listOf<TrainingPlan>()
    private var selectedPlanId: Int? = null
    private var logs = mutableListOf<LogEntry>()

    /** Temporary holder for an exercise log before saving. */
    data class LogEntry(
        var exerciseName: String,
        var exerciseId: Int? = null,
        var sets: Int = 3,
        var reps: Int = 10,
        var weight: String = "0"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_session)

        spinnerPlan = findViewById(R.id.spinnerPlan)
        rvLogs = findViewById(R.id.rvLogs)
        btnAddLog = findViewById(R.id.btnAddLog)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        rvLogs.layoutManager = LinearLayoutManager(this)
        rvLogs.adapter = LogsAdapter()

        btnAddLog.setOnClickListener { showAddLogDialog() }
        btnSave.setOnClickListener { saveSession() }

        loadPlans()
    }

    private fun loadPlans() {
        lifecycleScope.launch {
            val result = FitTrackRepository.getPlans()
            result.onSuccess { list ->
                plans = list
                val names = listOf("-- Freestyle --") + list.map { it.name }
                spinnerPlan.adapter = ArrayAdapter(
                    this@NewSessionActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
                spinnerPlan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos == 0) {
                            selectedPlanId = null
                            logs.clear()
                        } else {
                            val plan = plans[pos - 1]
                            selectedPlanId = plan.id
                            prefillFromPlan(plan)
                        }
                        rvLogs.adapter?.notifyDataSetChanged()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    /** Pre-fill log entries from the selected plan's exercises. */
    private fun prefillFromPlan(plan: TrainingPlan) {
        logs.clear()
        for (ex in plan.exercises) {
            logs.add(LogEntry(
                exerciseName = ex.name,
                exerciseId = ex.id,
                sets = ex.defaultSets,
                reps = ex.defaultReps,
                weight = ex.defaultWeight
            ))
        }
    }

    private fun showAddLogDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_exercise, null)
        val etName = view.findViewById<EditText>(R.id.etExerciseName)
        val etSets = view.findViewById<EditText>(R.id.etSets)
        val etReps = view.findViewById<EditText>(R.id.etReps)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)

        AlertDialog.Builder(this)
            .setTitle("Add Exercise Log")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    logs.add(LogEntry(
                        exerciseName = name,
                        sets = etSets.text.toString().toIntOrNull() ?: 3,
                        reps = etReps.text.toString().toIntOrNull() ?: 10,
                        weight = etWeight.text.toString().ifEmpty { "0" }
                    ))
                    rvLogs.adapter?.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSession() {
        if (logs.isEmpty()) {
            Toast.makeText(this, "Add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateSessionRequest(
            trainingPlan = selectedPlanId,
            date = LocalDate.now().toString(),
            notes = "",
            completed = true,
            exerciseLogs = logs.map { log ->
                CreateLogRequest(
                    exerciseName = log.exerciseName,
                    exercise = log.exerciseId,
                    sets = log.sets,
                    reps = log.reps,
                    weight = log.weight
                )
            }
        )

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false
        lifecycleScope.launch {
            val result = FitTrackRepository.createSession(request)
            progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this@NewSessionActivity, "Workout saved!", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { e ->
                btnSave.isEnabled = true
                Toast.makeText(this@NewSessionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Adapter for the exercise log list ───────────────────────

    inner class LogsAdapter : RecyclerView.Adapter<LogsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvItemTitle)
            val tvSub: TextView = view.findViewById(R.id.tvItemSubtitle)
            val btnDelete: ImageButton = view.findViewById(R.id.btnItemDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_list_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val log = logs[position]
            holder.tvName.text = log.exerciseName
            holder.tvSub.text = "${log.sets} × ${log.reps} @ ${log.weight} kg"
            holder.btnDelete.setOnClickListener {
                logs.removeAt(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = logs.size
    }
}
