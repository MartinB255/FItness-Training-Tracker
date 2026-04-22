package com.fittrack.app.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.dummy.DummyData
import com.google.android.material.appbar.MaterialToolbar

/** The Exercise Database — flat list of saved exercise names. */
class ExercisesActivity : AppCompatActivity() {

    private lateinit var rvExercises: RecyclerView
    private lateinit var tvEmpty: TextView
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
        refreshEmptyState()
    }

    private fun refreshEmptyState() {
        tvEmpty.visibility = if (DummyData.exercises.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Exercise name"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("New Exercise")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    DummyData.addExercise(name)
                    adapter.notifyItemInserted(DummyData.exercises.size - 1)
                    refreshEmptyState()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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

        override fun getItemCount() = DummyData.exercises.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = DummyData.exercises[position]
            holder.name.text = e.name
            holder.delete.setOnClickListener {
                val idx = holder.bindingAdapterPosition
                if (idx == RecyclerView.NO_POSITION) return@setOnClickListener
                AlertDialog.Builder(this@ExercisesActivity)
                    .setTitle("Delete ${e.name}?")
                    .setMessage("This will also remove it from any training plans.")
                    .setPositiveButton("Delete") { _, _ ->
                        DummyData.removeExercise(e.id)
                        notifyItemRemoved(idx)
                        refreshEmptyState()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}
