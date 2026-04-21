package com.fittrack.app.ui.progress

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.R
import com.fittrack.app.data.model.Exercise
import com.fittrack.app.data.model.TrainingPlan
import com.fittrack.app.data.repository.FitTrackRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

/**
 * Progress screen (WP4).
 *
 * Top section: pick an exercise → line chart showing weight over time.
 * Bottom section: bar chart showing weekly workout volume (last 12 weeks).
 */
class ProgressActivity : AppCompatActivity() {

    private lateinit var spinnerPlan: Spinner
    private lateinit var spinnerExercise: Spinner
    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var progressBar: ProgressBar

    private var plans = listOf<TrainingPlan>()
    private var exercises = listOf<Exercise>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        spinnerPlan = findViewById(R.id.spinnerPlan)
        spinnerExercise = findViewById(R.id.spinnerExercise)
        lineChart = findViewById(R.id.lineChart)
        barChart = findViewById(R.id.barChart)
        progressBar = findViewById(R.id.progressBar)

        setupChartDefaults()
        loadPlans()
        loadWeeklyVolume()
    }

    private fun setupChartDefaults() {
        // Line chart defaults
        lineChart.description.isEnabled = false
        lineChart.setNoDataText("Select an exercise to see progress")
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.axisRight.isEnabled = false

        // Bar chart defaults
        barChart.description.isEnabled = false
        barChart.setNoDataText("No weekly data yet")
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.axisRight.isEnabled = false
    }

    private fun loadPlans() {
        lifecycleScope.launch {
            val result = FitTrackRepository.getPlans()
            result.onSuccess { list ->
                plans = list
                val names = list.map { it.name }
                spinnerPlan.adapter = ArrayAdapter(
                    this@ProgressActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
                spinnerPlan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        loadExercisesForPlan(plans[pos].id)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun loadExercisesForPlan(planId: Int) {
        lifecycleScope.launch {
            val result = FitTrackRepository.getExercises(planId)
            result.onSuccess { list ->
                exercises = list
                val names = list.map { it.name }
                spinnerExercise.adapter = ArrayAdapter(
                    this@ProgressActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
                spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        loadExerciseProgress(exercises[pos].id)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    /** Load and display the line chart for a specific exercise. */
    private fun loadExerciseProgress(exerciseId: Int) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = FitTrackRepository.getExerciseProgress(exerciseId)
            progressBar.visibility = View.GONE
            result.onSuccess { entries ->
                if (entries.isEmpty()) {
                    lineChart.clear()
                    lineChart.setNoDataText("No progress data yet for this exercise")
                    lineChart.invalidate()
                    return@onSuccess
                }

                val dataEntries = entries.mapIndexed { index, entry ->
                    Entry(index.toFloat(), entry.weight.toFloat())
                }
                val dates = entries.map { it.date }

                val dataSet = LineDataSet(dataEntries, "Weight (kg)").apply {
                    color = Color.parseColor("#4CAF50")
                    setCircleColor(Color.parseColor("#4CAF50"))
                    lineWidth = 2f
                    circleRadius = 4f
                    valueTextSize = 10f
                }

                lineChart.data = LineData(dataSet)
                lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
                lineChart.xAxis.labelRotationAngle = -45f
                lineChart.animateX(500)
                lineChart.invalidate()
            }
        }
    }

    /** Load and display the bar chart for weekly volume. */
    private fun loadWeeklyVolume() {
        lifecycleScope.launch {
            val result = FitTrackRepository.getWeeklyVolume()
            result.onSuccess { weeks ->
                if (weeks.isEmpty()) {
                    barChart.setNoDataText("No workout data yet")
                    barChart.invalidate()
                    return@onSuccess
                }

                val barEntries = weeks.mapIndexed { index, week ->
                    BarEntry(index.toFloat(), week.totalVolume.toFloat())
                }
                val labels = weeks.map { it.week }

                val dataSet = BarDataSet(barEntries, "Weekly Volume (kg)").apply {
                    color = Color.parseColor("#2196F3")
                    valueTextSize = 10f
                }

                barChart.data = BarData(dataSet)
                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                barChart.xAxis.labelRotationAngle = -45f
                barChart.animateY(500)
                barChart.invalidate()
            }
        }
    }
}
