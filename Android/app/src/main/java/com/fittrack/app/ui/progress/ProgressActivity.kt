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
import com.fittrack.app.data.model.WeeklyVolume
import com.fittrack.app.data.model.WorkoutSession
import com.fittrack.app.data.repository.FitTrackRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
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
 * Progress screen with two MPAndroidChart charts:
 *   1. Line chart — weight progression per exercise over time, filterable by plan.
 *   2. Bar chart  — weekly total training volume (from /weekly-volume/).
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

    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var spinnerPlan: Spinner

    /** Sessions cached so the spinner can re-filter without re-fetching. */
    private var sessions: List<WorkoutSession> = emptyList()

    /** Spinner entries: [(label, planId-or-null)]. null == "All Plans". */
    private var planOptions: List<Pair<String, Int?>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_progress)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        lineChart = findViewById(R.id.lineChart)
        barChart = findViewById(R.id.barChart)
        spinnerPlan = findViewById(R.id.spinnerPlan)

        lineChart.setNoDataText("Loading…")
        barChart.setNoDataText("Loading…")

        loadCharts()
    }

    private fun loadCharts() {
        // Line chart driven by sessions + plans (so we can filter by plan).
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
                    lineChart.setNoDataText("No progress data yet")
                    lineChart.invalidate()
                    Toast.makeText(this@ProgressActivity,
                        "Progress: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
        // Bar chart still uses the dedicated weekly-volume endpoint.
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

    private fun setupPlanSpinner(plans: List<com.fittrack.app.data.model.TrainingPlan>) {
        // Build options from plans actually used in sessions, plus a top "All Plans".
        val planIdsInSessions = sessions.mapNotNull { it.trainingPlan }.toSet()
        val plansInUse = plans.filter { it.id in planIdsInSessions }

        planOptions = listOf<Pair<String, Int?>>("All Plans" to null) +
            plansInUse.map { it.name to it.id }

        spinnerPlan.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            planOptions.map { it.first },
        )
        spinnerPlan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                renderLineChart(planOptions[pos].second)
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        // Initial render; the spinner listener will also fire onItemSelected.
        renderLineChart(null)
    }

    /** Re-build the line chart from cached sessions, optionally filtered to a plan. */
    private fun renderLineChart(planFilterId: Int?) {
        // Group: exerciseName -> list of (date, weightDouble), only "done" logs.
        val grouped = mutableMapOf<String, MutableList<Pair<String, Float>>>()
        for (session in sessions) {
            if (planFilterId != null && session.trainingPlan != planFilterId) continue
            for (log in session.exerciseLogs) {
                if (log.status != "done") continue
                val w = log.weight.toFloatOrNull() ?: continue
                grouped.getOrPut(log.exerciseName) { mutableListOf() }
                    .add(session.date to w)
            }
        }
        if (grouped.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No progress data yet")
            lineChart.invalidate()
            return
        }
        // Sort each series by date.
        grouped.values.forEach { it.sortBy { p -> p.first } }

        val allDates = grouped.values.flatten().map { it.first }.distinct().sorted()
        val dateIndex = allDates.withIndex().associate { (i, d) -> d to i.toFloat() }

        val dataSets = grouped.entries.mapIndexed { i, (exerciseName, points) ->
            val entries = points.map { (date, w) -> Entry(dateIndex.getValue(date), w) }
            LineDataSet(entries, exerciseName).apply {
                color = seriesColors[i % seriesColors.size]
                setCircleColor(color)
                lineWidth = 2.5f
                circleRadius = 4f
                valueTextSize = 10f
                valueTextColor = axisTextColor
            }
        }

        lineChart.data = LineData(dataSets)
        styleAxes(lineChart.xAxis, lineChart.axisLeft, allDates.map { it.takeLast(5) })
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.setBackgroundColor(Color.TRANSPARENT)
        lineChart.setNoDataTextColor(axisTextColor)
        styleLegend(lineChart.legend)
        lineChart.animateX(500)
        lineChart.invalidate()
    }

    /** Bar chart: total volume (sets × reps × weight) per week. */
    private fun setupBarChart(weeks: List<WeeklyVolume>) {
        if (weeks.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No volume data yet")
            barChart.invalidate()
            return
        }
        val entries = weeks.mapIndexed { i, w -> BarEntry(i.toFloat(), w.totalVolume.toFloat()) }
        val set = BarDataSet(entries, "Volume (kg)").apply {
            color = accentOrange
            valueTextSize = 10f
            valueTextColor = axisTextColor
        }

        barChart.data = BarData(set).apply { barWidth = 0.55f }
        styleAxes(barChart.xAxis, barChart.axisLeft, weeks.map { it.week.takeLast(3) })
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setBackgroundColor(Color.TRANSPARENT)
        barChart.setNoDataTextColor(axisTextColor)
        styleLegend(barChart.legend)
        barChart.animateY(500)
        barChart.invalidate()
    }

    private fun styleAxes(
        xAxis: XAxis,
        yAxis: com.github.mikephil.charting.components.YAxis,
        xLabels: List<String>,
    ) {
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
