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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.dummy.DummyData
import com.fittrack.app.data.dummy.DummyData.PlanExercise
import com.fittrack.app.data.dummy.DummyData.TrainingPlan
import com.fittrack.app.ui.session.NewSessionActivity
import com.google.android.material.appbar.MaterialToolbar

/**
 * Training Plans list. Tapping a plan opens a detail dialog where the
 * user can add exercises from the Exercise Database, remove exercises,
 * or start a workout based on the plan.
 */
class PlansActivity : AppCompatActivity() {

    private lateinit var rvPlans: RecyclerView
    private lateinit var tvEmpty: TextView
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
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (DummyData.plans.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Create-plan dialog ────────────────────────────────────────────

    private fun showCreatePlanDialog() {
        val input = EditText(this).apply {
            hint = "Plan name"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("New Plan")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    DummyData.addPlan(name)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Plan detail dialog ────────────────────────────────────────────

    private fun showPlanDetailDialog(plan: TrainingPlan) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_plan_detail, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvPlanExercises)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val btnAdd = view.findViewById<Button>(R.id.btnAddExerciseToPlan)
        val btnStart = view.findViewById<Button>(R.id.btnStartWorkout)

        val planAdapter = PlanExercisesAdapter(plan.exercises) {
            tvEmpty.visibility = if (plan.exercises.isEmpty()) View.VISIBLE else View.GONE
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = planAdapter
        tvEmpty.visibility = if (plan.exercises.isEmpty()) View.VISIBLE else View.GONE

        val dialog = AlertDialog.Builder(this)
            .setTitle(plan.name)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        btnAdd.setOnClickListener {
            showExercisePickerDialog(plan) {
                planAdapter.notifyDataSetChanged()
                tvEmpty.visibility = if (plan.exercises.isEmpty()) View.VISIBLE else View.GONE
                adapter.notifyDataSetChanged() // update count in outer list
            }
        }
        btnStart.setOnClickListener {
            if (plan.exercises.isEmpty()) {
                AlertDialog.Builder(this)
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

    /** Pick an exercise from the database and add it to `plan` with default values. */
    private fun showExercisePickerDialog(plan: TrainingPlan, onAdded: () -> Unit) {
        val names = DummyData.exercises.map { it.name }.toTypedArray()
        if (names.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No exercises")
                .setMessage("Add exercises to the database first.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Add exercise")
            .setItems(names) { _, which ->
                val ex = DummyData.exercises[which]
                plan.exercises.add(PlanExercise(ex.id, ex.name, 3, 10, 0.0))
                onAdded()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Adapter: top-level plans list ────────────────────────────────

    private inner class PlansAdapter : RecyclerView.Adapter<PlansAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvPlanName)
            val meta: TextView = view.findViewById(R.id.tvPlanMeta)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plan, parent, false)
            return VH(v)
        }

        override fun getItemCount() = DummyData.plans.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val plan = DummyData.plans[position]
            holder.name.text = plan.name
            val count = plan.exercises.size
            holder.meta.text = "$count " + if (count == 1) "exercise" else "exercises"
            holder.itemView.setOnClickListener { showPlanDetailDialog(plan) }
        }
    }

    // ── Adapter: exercises inside a plan (detail dialog) ─────────────

    private inner class PlanExercisesAdapter(
        private val items: MutableList<PlanExercise>,
        private val onChanged: () -> Unit,
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
            holder.meta.text = "${e.sets} × ${e.reps} @ ${formatWeight(e.weight)}"
            holder.remove.setOnClickListener {
                val idx = holder.bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) {
                    items.removeAt(idx)
                    notifyItemRemoved(idx)
                    onChanged()
                }
            }
        }
    }

    private fun formatWeight(w: Double): String =
        if (w == w.toLong().toDouble()) "${w.toLong()} kg" else "$w kg"
}
