package com.fittrack.app.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fittrack.app.R
import com.fittrack.app.data.dummy.DummyData
import com.fittrack.app.data.dummy.DummyData.SessionLog
import com.fittrack.app.util.StatusUi
import com.google.android.material.appbar.MaterialToolbar

/** Detail view for one historical session — lists every exercise log with its status dot. */
class SessionDetailActivity : AppCompatActivity() {

    companion object { const val EXTRA_SESSION_ID = "session_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_session_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val id = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        val session = DummyData.sessionById(id)
        if (session == null) {
            finish()
            return
        }

        toolbar.title = session.planName
        findViewById<TextView>(R.id.tvHeader).text =
            "${session.date}  •  ${session.completed}/${session.total} completed"

        findViewById<RecyclerView>(R.id.rvLogs).apply {
            layoutManager = LinearLayoutManager(this@SessionDetailActivity)
            adapter = LogsAdapter(session.logs)
        }
    }

    private class LogsAdapter(private val logs: List<SessionLog>) :
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
            holder.meta.text = "${log.sets} × ${log.reps} @ ${formatWeight(log.weight)}"
            StatusUi.apply(holder.status, log.status)
        }

        private fun formatWeight(w: Double): String =
            if (w == w.toLong().toDouble()) "${w.toLong()} kg" else "$w kg"
    }
}
