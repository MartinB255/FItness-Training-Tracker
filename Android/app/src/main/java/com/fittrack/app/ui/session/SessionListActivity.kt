package com.fittrack.app.ui.session

import android.content.Intent
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
import com.fittrack.app.data.model.WorkoutSession
import com.fittrack.app.data.repository.FitTrackRepository
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Shows list of past workout sessions.
 * User can start a new workout or delete old ones.
 */
class SessionListActivity : AppCompatActivity() {

    private lateinit var rvSessions: RecyclerView
    private lateinit var btnNewSession: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private var sessions = mutableListOf<WorkoutSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_session_list)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvSessions = findViewById(R.id.rvSessions)
        btnNewSession = findViewById(R.id.btnNewSession)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvSessions.layoutManager = LinearLayoutManager(this)
        rvSessions.adapter = SessionsAdapter()

        btnNewSession.setOnClickListener {
            startActivity(Intent(this, NewSessionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadSessions()
    }

    private fun loadSessions() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = FitTrackRepository.getSessions()
            progressBar.visibility = View.GONE
            result.onSuccess { list ->
                sessions.clear()
                sessions.addAll(list)
                rvSessions.adapter?.notifyDataSetChanged()
                tvEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure { e ->
                Toast.makeText(this@SessionListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSession(session: WorkoutSession) {
        AlertDialog.Builder(this)
            .setTitle("Delete Session")
            .setMessage("Delete workout from ${session.date}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FitTrackRepository.deleteSession(session.id)
                    loadSessions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class SessionsAdapter : RecyclerView.Adapter<SessionsAdapter.ViewHolder>() {

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
            val session = sessions[position]
            holder.tvName.text = session.date
            val logCount = session.exerciseLogs.size
            holder.tvSub.text = "$logCount exercises logged"
            holder.btnDelete.setOnClickListener { deleteSession(session) }
        }

        override fun getItemCount() = sessions.size
    }
}
