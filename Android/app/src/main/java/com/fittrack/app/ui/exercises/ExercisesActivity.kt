package com.fittrack.app.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.model.Exercise
import com.fittrack.app.data.repository.FitTrackRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** The Exercise Database — flat list of saved exercise names. */
class ExercisesActivity : AppCompatActivity() {

    private lateinit var rvExercises: RecyclerView
    private lateinit var tvEmpty: TextView
    private val items = mutableListOf<Exercise>()
    private val adapter = ExercisesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_exercises)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvExercises = findViewById(R.id.rvExercises)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvExercises.layoutManager = LinearLayoutManager(this)
        rvExercises.adapter = adapter

        findViewById<Button>(R.id.btnAddExercise).setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadExercises()
    }

    private fun loadExercises() {
        lifecycleScope.launch {
            FitTrackRepository.getExercises()
                .onSuccess { list ->
                    items.clear()
                    items.addAll(list)
                    adapter.notifyDataSetChanged()
                    refreshEmptyState()
                }
                .onFailure { toast("Failed to load exercises: ${it.message}") }
        }
    }

    private fun refreshEmptyState() {
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Exercise name"
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("New Exercise")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) createExercise(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createExercise(name: String) {
        lifecycleScope.launch {
            FitTrackRepository.createExercise(name)
                .onSuccess { created ->
                    items.add(created)
                    // Keep the list alphabetised like the backend does.
                    items.sortBy { it.name.lowercase() }
                    adapter.notifyDataSetChanged()
                    refreshEmptyState()
                }
                .onFailure { toast("Couldn't create: ${it.message}") }
        }
    }

    private fun deleteExercise(exercise: Exercise) {
        lifecycleScope.launch {
            FitTrackRepository.deleteExercise(exercise.id)
                .onSuccess {
                    val idx = items.indexOfFirst { it.id == exercise.id }
                    if (idx >= 0) {
                        items.removeAt(idx)
                        adapter.notifyItemRemoved(idx)
                        refreshEmptyState()
                    }
                }
                .onFailure { toast("Couldn't delete: ${it.message}") }
        }
    }

    private fun showEditDialog(exercise: Exercise) {
        val input = EditText(this).apply {
            hint = "Exercise name"
            setText(exercise.name)
            setSelection(text.length)
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Rename Exercise")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && name != exercise.name) {
                    renameExercise(exercise, name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameExercise(exercise: Exercise, newName: String) {
        lifecycleScope.launch {
            FitTrackRepository.updateExercise(exercise.id, newName)
                .onSuccess { updated ->
                    val idx = items.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) items[idx] = updated
                    items.sortBy { it.name.lowercase() }
                    adapter.notifyDataSetChanged()
                }
                .onFailure { toast("Couldn't rename: ${it.message}") }
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private inner class ExercisesAdapter : RecyclerView.Adapter<ExercisesAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvExerciseName)
            val delete: ImageButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.name.text = e.name
            holder.itemView.setOnClickListener { showEditDialog(e) }
            holder.delete.setOnClickListener {
                MaterialAlertDialogBuilder(this@ExercisesActivity)
                    .setTitle("Delete ${e.name}?")
                    .setMessage("This will also remove it from any training plans.")
                    .setPositiveButton("Delete") { _, _ -> deleteExercise(e) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}
