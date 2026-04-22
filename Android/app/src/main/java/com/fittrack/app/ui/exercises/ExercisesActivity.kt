package com.fittrack.app.ui.exercises

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.model.Exercise
import com.fittrack.app.data.repository.FitTrackRepository
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Shows all exercises in a training plan.
 * User can add new exercises with default sets/reps/weight.
 */
class ExercisesActivity : AppCompatActivity() {

    private var planId: Int = 0
    private lateinit var rvExercises: RecyclerView
    private lateinit var btnAddExercise: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private var exercises = mutableListOf<Exercise>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_exercises)

        planId = intent.getIntExtra("plan_id", 0)
        val planName = intent.getStringExtra("plan_name") ?: "Exercises"

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = planName
        toolbar.setNavigationOnClickListener { finish() }

        rvExercises = findViewById(R.id.rvExercises)
        btnAddExercise = findViewById(R.id.btnAddExercise)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvExercises.layoutManager = LinearLayoutManager(this)
        rvExercises.adapter = ExercisesAdapter()

        btnAddExercise.setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadExercises()
    }

    private fun loadExercises() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = FitTrackRepository.getExercises(planId)
            progressBar.visibility = View.GONE
            result.onSuccess { list ->
                exercises.clear()
                exercises.addAll(list)
                rvExercises.adapter?.notifyDataSetChanged()
                tvEmpty.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure { e ->
                Toast.makeText(this@ExercisesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_exercise, null)
        val etName = view.findViewById<EditText>(R.id.etExerciseName)
        val etSets = view.findViewById<EditText>(R.id.etSets)
        val etReps = view.findViewById<EditText>(R.id.etReps)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)

        AlertDialog.Builder(this)
            .setTitle("Add Exercise")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val sets = etSets.text.toString().toIntOrNull() ?: 3
                val reps = etReps.text.toString().toIntOrNull() ?: 10
                val weight = etWeight.text.toString().ifEmpty { "0" }
                if (name.isNotEmpty()) {
                    addExercise(name, sets, reps, weight)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addExercise(name: String, sets: Int, reps: Int, weight: String) {
        lifecycleScope.launch {
            val result = FitTrackRepository.createExercise(planId, name, sets, reps, weight)
            result.onSuccess { loadExercises() }
                .onFailure { e ->
                    Toast.makeText(this@ExercisesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteExercise(exercise: Exercise) {
        AlertDialog.Builder(this)
            .setTitle("Delete Exercise")
            .setMessage("Delete \"${exercise.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FitTrackRepository.deleteExercise(exercise.id)
                    loadExercises()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Adapter ─────────────────────────────────────────────────

    inner class ExercisesAdapter : RecyclerView.Adapter<ExercisesAdapter.ViewHolder>() {

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
            val ex = exercises[position]
            holder.tvName.text = ex.name
            holder.tvSub.text = "${ex.defaultSets} sets × ${ex.defaultReps} reps @ ${ex.defaultWeight} kg"
            holder.btnDelete.setOnClickListener { deleteExercise(ex) }
        }

        override fun getItemCount() = exercises.size
    }
}
