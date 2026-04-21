package com.fittrack.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.api.RetrofitClient
import com.fittrack.app.data.model.PersonalRecord
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.ui.auth.LoginActivity
import com.fittrack.app.ui.plans.PlansActivity
import com.fittrack.app.ui.progress.ProgressActivity
import com.fittrack.app.ui.session.SessionListActivity
import kotlinx.coroutines.launch

/**
 * Dashboard — the main screen after login.
 * Shows key metrics (total workouts, streak, PRs)
 * and provides navigation to other sections.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTotalWorkouts: TextView
    private lateinit var tvStreak: TextView
    private lateinit var rvPersonalRecords: RecyclerView
    private lateinit var btnPlans: Button
    private lateinit var btnNewSession: Button
    private lateinit var btnProgress: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts)
        tvStreak = findViewById(R.id.tvStreak)
        rvPersonalRecords = findViewById(R.id.rvPersonalRecords)
        btnPlans = findViewById(R.id.btnPlans)
        btnNewSession = findViewById(R.id.btnNewSession)
        btnProgress = findViewById(R.id.btnProgress)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)

        rvPersonalRecords.layoutManager = LinearLayoutManager(this)

        btnPlans.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }
        btnNewSession.setOnClickListener {
            startActivity(Intent(this, SessionListActivity::class.java))
        }
        btnProgress.setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }
        btnLogout.setOnClickListener {
            RetrofitClient.clearToken()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = FitTrackRepository.getDashboard()
            progressBar.visibility = View.GONE
            result.onSuccess { data ->
                tvTotalWorkouts.text = "Total Workouts: ${data.totalWorkouts}"
                tvStreak.text = "Current Streak: ${data.currentStreak} days"
                rvPersonalRecords.adapter = PRAdapter(data.personalRecords)
            }.onFailure { e ->
                Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Simple adapter for the personal records list. */
    inner class PRAdapter(private val records: List<PersonalRecord>) :
        RecyclerView.Adapter<PRAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRecord: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pr = records[position]
            holder.tvRecord.text = "${pr.exerciseName}: ${pr.maxWeight} kg"
        }

        override fun getItemCount() = records.size
    }
}
