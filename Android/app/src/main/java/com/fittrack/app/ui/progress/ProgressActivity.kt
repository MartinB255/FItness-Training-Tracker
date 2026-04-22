package com.fittrack.app.ui.progress

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.R
import com.fittrack.app.data.model.ProgressPoint
import com.fittrack.app.data.model.WeeklyVolume
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
 *   1. Line chart — weight progression per exercise over time (from /progress/).
 *   2. Bar chart  — weekly total training volume (from /weekly-volume/).
 */
class ProgressActivity : AppCompatActivity() {

    private val accentOrange = Color.parseColor("#FF7A00")
    private val axisTextColor = Color.parseColor("#EEEEEE")
    private val mutedGridColor = Color.parseColor("#555555")

    // Palette for per-exercise series on the line chart.
    private val seriesColors = intArrayOf(
        Color.parseColor("#FF7A00"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#9C27B0"),
    )

    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_progress)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        lineChart = findViewById(R.id.lineChart)
        barChart = findViewById(R.id.barChart)

        lineChart.setNoDataText("Loading…")
        barChart.setNoDataText("Loading…")

        loadCharts()
    }

    private fun loadCharts() {
        lifecycleScope.launch {
            FitTrackRepository.getProgress()
                .onSuccess { setupLineChart(it) }
                .onFailure {
                    lineChart.setNoDataText("No progress data yet")
                    lineChart.invalidate()
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

    /** Line chart: weight (kg) vs. dates, one line per exercise. */
    private fun setupLineChart(series: Map<String, List<ProgressPoint>>) {
        if (series.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No progress data yet")
            lineChart.invalidate()
            return
        }

        // Unified x-axis built from the union of all dates (sorted).
        val allDates = series.values.flatten().map { it.date }.distinct().sorted()
        val dateIndex = allDates.withIndex().associate { (i, d) -> d to i.toFloat() }

        val dataSets = series.entries.mapIndexed { i, (exerciseName, points) ->
            val entries = points.map {
                Entry(
                    dateIndex.getValue(it.date),
                    it.weight.toFloatOrNull() ?: 0f,
                )
            }
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
        // Show YYYY-MM-DD as MM-DD to keep the x-axis readable.
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
        // "2026-W17" -> "W17"
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
