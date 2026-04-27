package com.fittrack.app.ui.progress

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.R
import com.fittrack.app.data.model.TrainingPlan
import com.fittrack.app.data.model.WeeklyVolume
import com.fittrack.app.data.model.WorkoutSession
import com.fittrack.app.data.repository.FitTrackRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Progress screen.
 *   • Three line charts — weight / reps / sets per exercise over time.
 *     Filtered by training plan via the top spinner; defaults to the plan
 *     used in the most recent session.
 *   • One bar chart   — weekly total volume (kg) since the user's first
 *     session, re-numbered starting at "Week 1".
 */
class ProgressActivity : AppCompatActivity() {

    private val accentOrange = Color.parseColor("#FF7A00")
    private val axisTextColor = Color.parseColor("#EEEEEE")
    private val mutedGridColor = Color.parseColor("#555555")

    private val seriesColors = intArrayOf(
        Color.parseColor("#FF7A00"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#9C27B0"),
    )

    private lateinit var weightChart: LineChart
    private lateinit var repsChart: LineChart
    private lateinit var setsChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var spinnerPlan: Spinner

    private var sessions: List<WorkoutSession> = emptyList()

    /** Spinner entries: [(label, planId-or-null)]. null == "All Plans". */
    private var planOptions: List<Pair<String, Int?>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_progress)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        weightChart = findViewById(R.id.lineChart)
        repsChart = findViewById(R.id.repsChart)
        setsChart = findViewById(R.id.setsChart)
        barChart = findViewById(R.id.barChart)
        spinnerPlan = findViewById(R.id.spinnerPlan)

        weightChart.setNoDataText("Loading…")
        repsChart.setNoDataText("Loading…")
        setsChart.setNoDataText("Loading…")
        barChart.setNoDataText("Loading…")

        loadCharts()
    }

    private fun loadCharts() {
        lifecycleScope.launch {
            val sessionsResult = FitTrackRepository.getSessions()
            val plansResult = FitTrackRepository.getPlans()

            sessionsResult
                .onSuccess { fetchedSessions ->
                    sessions = fetchedSessions
                    val plans = plansResult.getOrNull().orEmpty()
                    setupPlanSpinner(plans)
                }
                .onFailure {
                    showLineChartsEmpty("No progress data yet")
                    Toast.makeText(this@ProgressActivity,
                        "Progress: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
        lifecycleScope.launch {
            FitTrackRepository.getWeeklyVolume()
                .onSuccess { setupBarChart(it) }
                .onFailure {
                    barChart.setNoDataText("No volume data yet")
                    barChart.invalidate()
                    Toast.makeText(this@ProgressActivity,
                        "Volume: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupPlanSpinner(plans: List<TrainingPlan>) {
        // Build options from plans actually used in sessions, plus "All Plans" at the top.
        val planIdsInSessions = sessions.mapNotNull { it.trainingPlan }.toSet()
        val plansInUse = plans.filter { it.id in planIdsInSessions }

        planOptions = listOf<Pair<String, Int?>>("All Plans" to null) +
            plansInUse.map { it.name to it.id }

        spinnerPlan.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            planOptions.map { it.first },
        )

        // Default to the plan used in the most recent session, if any.
        val lastSession = sessions.maxByOrNull { it.date }
        val defaultPlanId = lastSession?.trainingPlan
        val defaultIndex = planOptions.indexOfFirst { it.second == defaultPlanId }
            .takeIf { it >= 0 } ?: 0

        spinnerPlan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                renderLineCharts(planOptions[pos].second)
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        spinnerPlan.setSelection(defaultIndex)
    }

    /** Group "done" logs by exercise name, applying [selector] to each log. */
    private fun groupLogs(
        planFilterId: Int?,
        selector: (com.fittrack.app.data.model.ExerciseLog) -> Float?,
    ): Map<String, List<Pair<String, Float>>> {
        val grouped = mutableMapOf<String, MutableList<Pair<String, Float>>>()
        for (session in sessions) {
            if (planFilterId != null && session.trainingPlan != planFilterId) continue
            for (log in session.exerciseLogs) {
                if (log.status != "done") continue
                val v = selector(log) ?: continue
                grouped.getOrPut(log.exerciseName) { mutableListOf() }
                    .add(session.date to v)
            }
        }
        grouped.values.forEach { it.sortBy { p -> p.first } }
        return grouped
    }

    private fun renderLineCharts(planFilterId: Int?) {
        renderLineChart(
            chart = weightChart,
            grouped = groupLogs(planFilterId) { it.weight.toFloatOrNull() },
            emptyText = "No weight data yet",
        )
        renderLineChart(
            chart = repsChart,
            grouped = groupLogs(planFilterId) { it.reps.toFloat() },
            emptyText = "No rep data yet",
        )
        renderLineChart(
            chart = setsChart,
            grouped = groupLogs(planFilterId) { it.sets.toFloat() },
            emptyText = "No set data yet",
        )
    }

    private fun renderLineChart(
        chart: LineChart,
        grouped: Map<String, List<Pair<String, Float>>>,
        emptyText: String,
    ) {
        if (grouped.isEmpty()) {
            chart.clear()
            chart.setNoDataText(emptyText)
            chart.setNoDataTextColor(axisTextColor)
            chart.invalidate()
            return
        }
        val allDates = grouped.values.flatten().map { it.first }.distinct().sorted()
        val dateIndex = allDates.withIndex().associate { (i, d) -> d to i.toFloat() }

        val dataSets = grouped.entries.mapIndexed { i, (exerciseName, points) ->
            val entries = points.map { (date, v) -> Entry(dateIndex.getValue(date), v) }
            LineDataSet(entries, exerciseName).apply {
                color = seriesColors[i % seriesColors.size]
                setCircleColor(color)
                lineWidth = 2.5f
                circleRadius = 4f
                valueTextSize = 10f
                valueTextColor = axisTextColor
            }
        }

        chart.data = LineData(dataSets)
        styleAxes(chart.xAxis, chart.axisLeft, allDates.map { it.takeLast(5) })
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataTextColor(axisTextColor)
        styleLegend(chart.legend)
        chart.animateX(500)
        chart.invalidate()
    }

    private fun showLineChartsEmpty(message: String) {
        listOf(weightChart, repsChart, setsChart).forEach {
            it.setNoDataText(message)
            it.invalidate()
        }
    }

    /**
     * Bar chart: total volume per training week. The backend keys weeks as
     * ISO calendar strings ("2026-W17"); we re-number them sequentially as
     * "Week 1", "Week 2", … starting from the user's first tracked week.
     */
    private fun setupBarChart(weeks: List<WeeklyVolume>) {
        if (weeks.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No volume data yet")
            barChart.invalidate()
            return
        }
        val sorted = weeks.sortedBy { it.week }
        val entries = sorted.mapIndexed { i, w -> BarEntry(i.toFloat(), w.totalVolume.toFloat()) }
        val labels = sorted.indices.map { "W${it + 1}" }

        val set = BarDataSet(entries, "Volume (kg)").apply {
            color = accentOrange
            valueTextSize = 10f
            valueTextColor = axisTextColor
        }

        barChart.data = BarData(set).apply { barWidth = 0.55f }
        styleAxes(barChart.xAxis, barChart.axisLeft, labels)
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setBackgroundColor(Color.TRANSPARENT)
        barChart.setNoDataTextColor(axisTextColor)
        styleLegend(barChart.legend)
        barChart.animateY(500)
        barChart.invalidate()
    }

    private fun styleAxes(xAxis: XAxis, yAxis: YAxis, xLabels: List<String>) {
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textColor = axisTextColor
        xAxis.gridColor = mutedGridColor
        xAxis.axisLineColor = mutedGridColor
        xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)

        yAxis.textColor = axisTextColor
        yAxis.gridColor = mutedGridColor
        yAxis.axisLineColor = mutedGridColor
    }

    private fun styleLegend(legend: Legend) {
        legend.textColor = axisTextColor
        legend.textSize = 12f
    }
}
