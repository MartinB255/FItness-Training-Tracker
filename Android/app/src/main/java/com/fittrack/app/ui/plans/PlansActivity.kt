package com.fittrack.app.ui.plans

import android.content.Intent
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
import com.fittrack.app.data.model.PlanExercise
import com.fittrack.app.data.model.TrainingPlan
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.session.NewSessionActivity
import com.fittrack.app.util.dp
import com.fittrack.app.util.formatWeightKg
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Training Plans list. Each row opens a detail dialog where the user can add
 * exercises from the Exercise Database, remove exercises from the plan, or
 * start a workout using the plan.
 */
class PlansActivity : AppCompatActivity() {

    private lateinit var rvPlans: RecyclerView
    private lateinit var tvEmpty: TextView
    private val plans = mutableListOf<TrainingPlan>()
    private val adapter = PlansAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_plans)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvPlans = findViewById(R.id.rvPlans)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvPlans.layoutManager = LinearLayoutManager(this)
        rvPlans.adapter = adapter

        findViewById<Button>(R.id.btnAddPlan).setOnClickListener { showCreatePlanDialog() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            FitTrackRepository.getPlans()
                .onSuccess { list ->
                    plans.clear()
                    plans.addAll(list)
                    adapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { toast("Failed to load plans: ${it.message}") }
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    // ── Create-plan dialog ────────────────────────────────────────────

    private fun showCreatePlanDialog() {
        val input = EditText(this).apply {
            hint = "Plan name"
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("New Plan")
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
            FitTrackRepository.createPlan(name)
                .onSuccess { plan ->
                    plans.add(0, plan)
                    adapter.notifyItemInserted(0)
                    rvPlans.scrollToPosition(0)
                    tvEmpty.visibility = View.GONE
                }
                .onFailure { toast("Couldn't create plan: ${it.message}") }
        }
    }

    // ── Plan detail dialog ────────────────────────────────────────────

    private fun showPlanDetailDialog(plan: TrainingPlan) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_plan_detail, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvPlanExercises)
        val tvEmptyDlg = view.findViewById<TextView>(R.id.tvEmpty)
        val btnAdd = view.findViewById<Button>(R.id.btnAddExerciseToPlan)
        val btnStart = view.findViewById<Button>(R.id.btnStartWorkout)

        // Local mutable copy so remove reflects immediately in the dialog.
        val planExercises = plan.planExercises.toMutableList()

        fun syncOuterList() {
            val idx = plans.indexOfFirst { it.id == plan.id }
            if (idx >= 0) {
                plans[idx] = plans[idx].copy(planExercises = planExercises.toList())
                adapter.notifyItemChanged(idx)
            }
        }

        lateinit var planAdapter: PlanExercisesAdapter
        planAdapter = PlanExercisesAdapter(planExercises) { idx ->
            val removed = planExercises[idx]
            lifecycleScope.launch {
                FitTrackRepository.removePlanExercise(removed.id)
                    .onSuccess {
                        planExercises.removeAt(idx)
                        planAdapter.notifyItemRemoved(idx)
                        syncOuterList()
                        tvEmptyDlg.visibility =
                            if (planExercises.isEmpty()) View.VISIBLE else View.GONE
                    }
                    .onFailure { toast("Couldn't remove: ${it.message}") }
            }
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = planAdapter
        tvEmptyDlg.visibility = if (planExercises.isEmpty()) View.VISIBLE else View.GONE

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(plan.name)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        btnAdd.setOnClickListener {
            showExercisePickerDialog(plan.id) { added ->
                planExercises.add(added)
                planAdapter.notifyItemInserted(planExercises.size - 1)
                tvEmptyDlg.visibility = View.GONE
                syncOuterList()
            }
        }
        btnStart.setOnClickListener {
            if (planExercises.isEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Empty plan")
                    .setMessage("Add at least one exercise before starting a workout.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            dialog.dismiss()
            startActivity(
                Intent(this, NewSessionActivity::class.java)
                    .putExtra(NewSessionActivity.EXTRA_PLAN_ID, plan.id)
            )
        }

        dialog.show()
    }

    /** Pick an exercise from the backend catalog or create a new one, then attach it. */
    private fun showExercisePickerDialog(
        planId: Int,
        onAdded: (PlanExercise) -> Unit,
    ) {
        lifecycleScope.launch {
            FitTrackRepository.getExercises()
                .onSuccess { catalog ->
                    val createLabel = "+ Create new exercise…"
                    val items = (listOf(createLabel) + catalog.map { it.name }).toTypedArray()
                    MaterialAlertDialogBuilder(this@PlansActivity)
                        .setTitle("Add exercise")
                        .setItems(items) { _, which ->
                            if (which == 0) showCreateExerciseDialog(planId, onAdded)
                            else attachExercise(planId, catalog[which - 1], onAdded)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .onFailure { toast("Couldn't load exercises: ${it.message}") }
        }
    }

    /** Prompt for a new exercise name, save it to the catalog, then attach to the plan. */
    private fun showCreateExerciseDialog(
        planId: Int,
        onAdded: (PlanExercise) -> Unit,
    ) {
        val input = EditText(this).apply {
            hint = "Exercise name"
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("New Exercise")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                lifecycleScope.launch {
                    FitTrackRepository.createExercise(name)
                        .onSuccess { exercise -> attachExercise(planId, exercise, onAdded) }
                        .onFailure { toast("Couldn't create exercise: ${it.message}") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun attachExercise(
        planId: Int,
        exercise: Exercise,
        onAdded: (PlanExercise) -> Unit,
    ) {
        lifecycleScope.launch {
            FitTrackRepository.addPlanExercise(planId, exercise.id)
                .onSuccess(onAdded)
                .onFailure { toast("Couldn't add: ${it.message}") }
        }
    }

    // ── Adapter: top-level plans list ────────────────────────────────

    private fun confirmDeletePlan(plan: TrainingPlan) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete plan?")
            .setMessage("\"${plan.name}\" will be removed. Past sessions that used it are kept.")
            .setPositiveButton("Delete") { _, _ -> deletePlan(plan) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePlan(plan: TrainingPlan) {
        lifecycleScope.launch {
            FitTrackRepository.deletePlan(plan.id)
                .onSuccess {
                    val idx = plans.indexOfFirst { it.id == plan.id }
                    if (idx >= 0) {
                        plans.removeAt(idx)
                        adapter.notifyItemRemoved(idx)
                        tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                .onFailure { toast("Couldn't delete plan: ${it.message}") }
        }
    }

    private inner class PlansAdapter : RecyclerView.Adapter<PlansAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvPlanName)
            val meta: TextView = view.findViewById(R.id.tvPlanMeta)
            val delete: ImageButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plan, parent, false)
            return VH(v)
        }

        override fun getItemCount() = plans.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val plan = plans[position]
            holder.name.text = plan.name
            val count = plan.planExercises.size
            holder.meta.text = "$count " + if (count == 1) "exercise" else "exercises"
            holder.itemView.setOnClickListener { showPlanDetailDialog(plan) }
            holder.delete.setOnClickListener {
                val idx = holder.bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) confirmDeletePlan(plans[idx])
            }
        }
    }

    // ── Adapter: exercises inside a plan (detail dialog) ─────────────

    private inner class PlanExercisesAdapter(
        private val items: MutableList<PlanExercise>,
        private val onRemove: (Int) -> Unit,
    ) : RecyclerView.Adapter<PlanExercisesAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvName)
            val meta: TextView = view.findViewById(R.id.tvMeta)
            val remove: ImageButton = view.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plan_exercise, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.name.text = e.exerciseName
            holder.meta.text = "${e.sets} × ${e.reps} - ${formatWeightKg(e.weight)}"
            holder.remove.setOnClickListener {
                val idx = holder.bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) onRemove(idx)
            }
        }
    }

}
