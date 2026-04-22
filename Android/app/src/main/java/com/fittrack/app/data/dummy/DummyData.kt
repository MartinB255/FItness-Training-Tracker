package com.fittrack.app.data.dummy

/**
 * Dummy data store — stands in for the backend API while the UI is being built.
 * Everything lives in memory; changes survive for the app process lifetime only.
 */
object DummyData {

    enum class ExerciseStatus { TODO, DONE, SKIPPED, REFUSED }

    data class Exercise(val id: Int, val name: String)

    data class PlanExercise(
        val exerciseId: Int,
        val exerciseName: String,
        var sets: Int,
        var reps: Int,
        var weight: Double,
    )

    data class TrainingPlan(
        val id: Int,
        var name: String,
        val exercises: MutableList<PlanExercise>,
    )

    data class SessionLog(
        val exerciseName: String,
        val sets: Int,
        val reps: Int,
        val weight: Double,
        val status: ExerciseStatus,
    )

    data class WorkoutSession(
        val id: Int,
        val date: String,
        val planName: String,
        val logs: List<SessionLog>,
    ) {
        val completed: Int get() = logs.count { it.status == ExerciseStatus.DONE }
        val total: Int get() = logs.size
    }

    data class ProgressPoint(val date: String, val weight: Double)
    data class WeeklyVolume(val week: String, val volume: Double)

    // ── Exercise database ──────────────────────────────────────────
    val exercises = mutableListOf(
        Exercise(1, "Bench Press"),
        Exercise(2, "Squat"),
        Exercise(3, "Deadlift"),
        Exercise(4, "Overhead Press"),
        Exercise(5, "Barbell Row"),
        Exercise(6, "Pull-up"),
        Exercise(7, "Bicep Curl"),
    )
    private var nextExerciseId = 8

    fun addExercise(name: String) {
        exercises.add(Exercise(nextExerciseId++, name))
    }

    fun removeExercise(id: Int) {
        exercises.removeAll { it.id == id }
        plans.forEach { plan -> plan.exercises.removeAll { it.exerciseId == id } }
    }

    // ── Training plans ─────────────────────────────────────────────
    val plans = mutableListOf(
        TrainingPlan(1, "Fullbody", mutableListOf(
            PlanExercise(1, "Bench Press", 3, 10, 60.0),
            PlanExercise(2, "Squat", 3, 10, 80.0),
            PlanExercise(3, "Deadlift", 3, 5, 100.0),
        )),
        TrainingPlan(2, "Push Pull Legs", mutableListOf(
            PlanExercise(4, "Overhead Press", 3, 8, 40.0),
            PlanExercise(5, "Barbell Row", 3, 10, 55.0),
        )),
        TrainingPlan(3, "Upper/Lower", mutableListOf(
            PlanExercise(1, "Bench Press", 4, 8, 65.0),
            PlanExercise(2, "Squat", 4, 6, 90.0),
        )),
    )
    private var nextPlanId = 4

    fun addPlan(name: String): TrainingPlan {
        val plan = TrainingPlan(nextPlanId++, name, mutableListOf())
        plans.add(plan)
        return plan
    }

    fun planById(id: Int): TrainingPlan? = plans.firstOrNull { it.id == id }

    // ── Workout session history ────────────────────────────────────
    val sessions = mutableListOf(
        WorkoutSession(1, "2026-04-15", "Fullbody", listOf(
            SessionLog("Bench Press", 3, 10, 60.0, ExerciseStatus.DONE),
            SessionLog("Squat", 3, 10, 80.0, ExerciseStatus.DONE),
            SessionLog("Deadlift", 3, 5, 100.0, ExerciseStatus.SKIPPED),
        )),
        WorkoutSession(2, "2026-04-18", "Push Pull Legs", listOf(
            SessionLog("Overhead Press", 3, 8, 40.0, ExerciseStatus.DONE),
            SessionLog("Barbell Row", 3, 10, 55.0, ExerciseStatus.DONE),
        )),
        WorkoutSession(3, "2026-04-20", "Fullbody", listOf(
            SessionLog("Bench Press", 3, 10, 62.5, ExerciseStatus.DONE),
            SessionLog("Squat", 3, 10, 82.5, ExerciseStatus.DONE),
            SessionLog("Deadlift", 3, 5, 102.5, ExerciseStatus.REFUSED),
        )),
    )
    private var nextSessionId = 4

    /** Insert the newest session at the top of the history. */
    fun addSession(date: String, planName: String, logs: List<SessionLog>) {
        sessions.add(0, WorkoutSession(nextSessionId++, date, planName, logs))
    }

    fun sessionById(id: Int): WorkoutSession? = sessions.firstOrNull { it.id == id }

    // ── Progress / charts ──────────────────────────────────────────
    val exerciseProgress: Map<String, List<ProgressPoint>> = mapOf(
        "Bench Press" to listOf(
            ProgressPoint("04-01", 55.0),
            ProgressPoint("04-05", 57.5),
            ProgressPoint("04-10", 60.0),
            ProgressPoint("04-15", 60.0),
            ProgressPoint("04-20", 62.5),
        ),
        "Squat" to listOf(
            ProgressPoint("04-01", 75.0),
            ProgressPoint("04-05", 77.5),
            ProgressPoint("04-10", 80.0),
            ProgressPoint("04-15", 80.0),
            ProgressPoint("04-20", 82.5),
        ),
        "Deadlift" to listOf(
            ProgressPoint("04-01", 95.0),
            ProgressPoint("04-10", 100.0),
            ProgressPoint("04-20", 102.5),
        ),
    )

    val weeklyVolume = listOf(
        WeeklyVolume("W13", 5400.0),
        WeeklyVolume("W14", 6200.0),
        WeeklyVolume("W15", 5800.0),
        WeeklyVolume("W16", 7100.0),
    )
}
