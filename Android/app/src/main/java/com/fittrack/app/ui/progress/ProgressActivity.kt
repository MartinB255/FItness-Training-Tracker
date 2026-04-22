package com.fittrack.app.ui.progress

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.fittrack.app.R
import com.fittrack.app.data.dummy.DummyData
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

/**
 * Progress screen with two MPAndroidChart charts:
 *   1. Line chart — weight progression per exercise over time (multi-series).
 *   2. Bar chart  — weekly total training volume.
 */
class ProgressActivity : AppCompatActivity() {

    private val accentOrange = Color.parseColor("#FF7A00")
    private val axisTextColor = Color.parseColor("#EEEEEE")
    private val mutedGridColor = Color.parseColor("#555555")

    // A palette used for the per-exercise series on the line chart.
    private val seriesColors = intArrayOf(
        Color.parseColor("#FF7A00"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#9C27B0"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_progress)

        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        setupLineChart(findViewById(R.id.lineChart))
        setupBarChart(findViewById(R.id.barChart))
    }

    /** Line chart: weight (kg) vs. dates, one line per exercise. */
    private fun setupLineChart(chart: LineChart) {
        val series = DummyData.exerciseProgress
        if (series.isEmpty()) {
            chart.setNoDataText("No progress data yet")
            return
        }

        // Unified x-axis built from the union of all dates (sorted).
        val allDates = series.values.flatten().map { it.date }.distinct().sorted()
        val dateIndex = allDates.withIndex().associate { (i, d) -> d to i.toFloat() }

        val dataSets = series.entries.mapIndexed { i, (exerciseName, points) ->
            val entries = points.map { Entry(dateIndex.getValue(it.date), it.weight.toFloat()) }
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
        styleAxes(chart.xAxis, chart.axisLeft, allDates)
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataTextColor(axisTextColor)
        styleLegend(chart.legend)
        chart.animateX(500)
        chart.invalidate()
    }

    /** Bar chart: total volume (sets × reps × weight) per week. */
    private fun setupBarChart(chart: BarChart) {
        val weeks = DummyData.weeklyVolume
        val entries = weeks.mapIndexed { i, w -> BarEntry(i.toFloat(), w.volume.toFloat()) }
        val set = BarDataSet(entries, "Volume (kg)").apply {
            color = accentOrange
            valueTextSize = 10f
            valueTextColor = axisTextColor
        }

        chart.data = BarData(set).apply { barWidth = 0.55f }
        styleAxes(chart.xAxis, chart.axisLeft, weeks.map { it.week })
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataTextColor(axisTextColor)
        styleLegend(chart.legend)
        chart.animateY(500)
        chart.invalidate()
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
