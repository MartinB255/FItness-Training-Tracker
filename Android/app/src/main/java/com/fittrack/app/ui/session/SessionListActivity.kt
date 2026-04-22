package com.fittrack.app.ui.session

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.model.WorkoutSession
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.plans.PlanPicker
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/** List of past workout sessions; tap one to open SessionDetailActivity. */
class SessionListActivity : AppCompatActivity() {

    private lateinit var rvSessions: RecyclerView
    private lateinit var tvEmpty: TextView
    private val sessions = mutableListOf<WorkoutSession>()
    private val adapter = SessionsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_session_list)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvSessions = findViewById(R.id.rvSessions)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvSessions.layoutManager = LinearLayoutManager(this)
        rvSessions.adapter = adapter

        findViewById<Button>(R.id.btnNewSession).setOnClickListener {
            PlanPicker.show(this)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSessions()
    }

    private fun loadSessions() {
        lifecycleScope.launch {
            FitTrackRepository.getSessions()
                .onSuccess { list ->
                    sessions.clear()
                    sessions.addAll(list)
                    adapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure {
                    Toast.makeText(this@SessionListActivity,
                        "Couldn't load sessions: ${it.message}",
                        Toast.LENGTH_LONG).show()
                }
        }
    }

    private inner class SessionsAdapter : RecyclerView.Adapter<SessionsAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val date: TextView = view.findViewById(R.id.tvDate)
            val plan: TextView = view.findViewById(R.id.tvPlan)
            val summary: TextView = view.findViewById(R.id.tvSummary)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session_history, parent, false)
            return VH(v)
        }

        override fun getItemCount() = sessions.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = sessions[position]
            holder.date.text = s.date
            holder.plan.text = s.planName
            val done = s.exerciseLogs.count { it.status == "done" }
            val total = s.exerciseLogs.size
            holder.summary.text = "$done/$total exercises completed"
            holder.itemView.setOnClickListener {
                startActivity(
                    Intent(this@SessionListActivity, SessionDetailActivity::class.java)
                        .putExtra(SessionDetailActivity.EXTRA_SESSION_ID, s.id)
                )
            }
        }
    }
}
