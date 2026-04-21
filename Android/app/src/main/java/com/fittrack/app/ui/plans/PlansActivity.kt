package com.fittrack.app.ui.plans

import android.content.Intent
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
import com.fittrack.app.data.model.TrainingPlan
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.exercises.ExercisesActivity
import kotlinx.coroutines.launch

/**
 * Displays the user's training plans.
 * Tap a plan to see/edit its exercises.
 * FAB to create a new plan.
 */
class PlansActivity : AppCompatActivity() {

    private lateinit var rvPlans: RecyclerView
    private lateinit var btnAddPlan: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private var plans = mutableListOf<TrainingPlan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plans)

        rvPlans = findViewById(R.id.rvPlans)
        btnAddPlan = findViewById(R.id.btnAddPlan)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvPlans.layoutManager = LinearLayoutManager(this)
        rvPlans.adapter = PlansAdapter()

        btnAddPlan.setOnClickListener { showCreateDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun loadPlans() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = FitTrackRepository.getPlans()
            progressBar.visibility = View.GONE
            result.onSuccess { list ->
                plans.clear()
                plans.addAll(list)
                rvPlans.adapter?.notifyDataSetChanged()
                tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure { e ->
                Toast.makeText(this@PlansActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateDialog() {
        val input = EditText(this).apply {
            hint = "Plan name"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("New Training Plan")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) createPlan(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createPlan(name: String) {
        lifecycleScope.launch {
            val result = FitTrackRepository.createPlan(name)
            result.onSuccess { loadPlans() }
                .onFailure { e ->
                    Toast.makeText(this@PlansActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deletePlan(plan: TrainingPlan) {
        AlertDialog.Builder(this)
            .setTitle("Delete Plan")
            .setMessage("Delete \"${plan.name}\"? This will also delete all exercises in it.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FitTrackRepository.deletePlan(plan.id)
                    loadPlans()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── RecyclerView Adapter ────────────────────────────────────

    inner class PlansAdapter : RecyclerView.Adapter<PlansAdapter.ViewHolder>() {

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
            val plan = plans[position]
            holder.tvName.text = plan.name
            holder.tvSub.text = "${plan.exercises.size} exercises"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@PlansActivity, ExercisesActivity::class.java)
                intent.putExtra("plan_id", plan.id)
                intent.putExtra("plan_name", plan.name)
                startActivity(intent)
            }
            holder.btnDelete.setOnClickListener { deletePlan(plan) }
        }

        override fun getItemCount() = plans.size
    }
}
