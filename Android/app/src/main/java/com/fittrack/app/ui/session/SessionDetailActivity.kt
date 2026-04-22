package com.fittrack.app.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.model.ExerciseLog
import com.fittrack.app.data.repository.FitTrackRepository
import com.fittrack.app.util.ExerciseStatus
import com.fittrack.app.util.StatusUi
import com.fittrack.app.util.formatWeightKg
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/** Detail view for one historical session — lists every exercise log with its status. */
class SessionDetailActivity : AppCompatActivity() {

    companion object { const val EXTRA_SESSION_ID = "session_id" }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvHeader: TextView
    private lateinit var rvLogs: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_session_detail)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        tvHeader = findViewById(R.id.tvHeader)
        rvLogs = findViewById(R.id.rvLogs)
        rvLogs.layoutManager = LinearLayoutManager(this)

        val id = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        if (id < 0) { finish(); return }

        loadSession(id)
    }

    private fun loadSession(id: Int) {
        lifecycleScope.launch {
            FitTrackRepository.getSession(id)
                .onSuccess { s ->
                    toolbar.title = s.planName
                    val done = s.exerciseLogs.count { it.status == "done" }
                    tvHeader.text =
                        "${s.date}  •  $done/${s.exerciseLogs.size} completed  •  " +
                        formatDuration(s.durationSeconds)
                    rvLogs.adapter = LogsAdapter(s.exerciseLogs)
                }
                .onFailure {
                    Toast.makeText(this@SessionDetailActivity,
                        "Couldn't load session: ${it.message}",
                        Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    /** "1h 23m 04s" / "23m 04s" / "45s" */
    private fun formatDuration(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> "%dh %02dm %02ds".format(h, m, s)
            m > 0 -> "%dm %02ds".format(m, s)
            else -> "%ds".format(s)
        }
    }

    private class LogsAdapter(private val logs: List<ExerciseLog>) :
        RecyclerView.Adapter<LogsAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvName)
            val meta: TextView = view.findViewById(R.id.tvMeta)
            val status: TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session_log, parent, false)
            return VH(v)
        }

        override fun getItemCount() = logs.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val log = logs[position]
            holder.name.text = log.exerciseName
            holder.meta.text = "${log.sets} × ${log.reps} @ ${formatWeightKg(log.weight)}"
            StatusUi.apply(holder.status, ExerciseStatus.fromApi(log.status))
        }
    }
}
