package com.fittrack.app.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════════
//  AUTH
// ═══════════════════════════════════════════════════════════════════

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
)

data class AuthResponse(val token: String, val user: User)

data class User(val id: Int, val username: String, val email: String)

// ═══════════════════════════════════════════════════════════════════
//  EXERCISE CATALOG  —  flat list of exercise names per user.
// ═══════════════════════════════════════════════════════════════════

data class Exercise(val id: Int, val name: String)

data class CreateExerciseRequest(val name: String)

// ═══════════════════════════════════════════════════════════════════
//  TRAINING PLANS  (with nested plan_exercises)
// ═══════════════════════════════════════════════════════════════════

data class TrainingPlan(
    val id: Int,
    val user: Int,
    val name: String,
    val description: String = "",
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("plan_exercises") val planExercises: List<PlanExercise> = emptyList(),
)

data class CreatePlanRequest(
    val name: String,
    val description: String = "",
)

/** One exercise placed inside a plan with the user's default sets/reps/weight. */
data class PlanExercise(
    val id: Int,
    @SerializedName("training_plan") val trainingPlan: Int,
    val exercise: Int,
    @SerializedName("exercise_name") val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: String,
    val order: Int,
)

data class CreatePlanExerciseRequest(
    @SerializedName("training_plan") val trainingPlan: Int,
    val exercise: Int,
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: String = "0",
    val order: Int = 0,
)

data class UpdatePlanExerciseRequest(
    val sets: Int,
    val reps: Int,
    val weight: String,
)

// ═══════════════════════════════════════════════════════════════════
//  WORKOUT SESSIONS  (with nested exercise_logs)
// ═══════════════════════════════════════════════════════════════════

data class WorkoutSession(
    val id: Int,
    val user: Int,
    @SerializedName("training_plan") val trainingPlan: Int?,
    @SerializedName("plan_name") val planName: String = "Freestyle",
    val date: String,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    val notes: String = "",
    val completed: Boolean = true,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("exercise_logs") val exerciseLogs: List<ExerciseLog> = emptyList(),
)

data class CreateSessionRequest(
    @SerializedName("training_plan") val trainingPlan: Int?,
    val date: String,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    val notes: String = "",
    val completed: Boolean = true,
    @SerializedName("exercise_logs") val exerciseLogs: List<CreateLogRequest>,
)

// ═══════════════════════════════════════════════════════════════════
//  EXERCISE LOGS
// ═══════════════════════════════════════════════════════════════════

data class ExerciseLog(
    val id: Int,
    @SerializedName("workout_session") val workoutSession: Int,
    val exercise: Int?,
    @SerializedName("exercise_name") val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: String,
    val status: String,          // "done" | "skipped" | "not_done"
)

data class CreateLogRequest(
    @SerializedName("workout_session") val workoutSession: Int? = null,
    val exercise: Int? = null,
    @SerializedName("exercise_name") val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: String,
    val status: String,
)

// ═══════════════════════════════════════════════════════════════════
//  PROGRESS / CHARTS
// ═══════════════════════════════════════════════════════════════════

/** One point on the weight-over-time line for an exercise. */
data class ProgressPoint(
    val date: String,
    val sets: Int,
    val reps: Int,
    val weight: String,
)

data class WeeklyVolume(
    val week: String,
    @SerializedName("total_volume") val totalVolume: Double,
    @SerializedName("session_count") val sessionCount: Int,
)

data class DashboardData(
    @SerializedName("total_workouts") val totalWorkouts: Int,
    @SerializedName("sessions_this_week") val sessionsThisWeek: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("personal_records") val personalRecords: List<PersonalRecord>,
    @SerializedName("last_workout") val lastWorkout: LastWorkout?,
)

data class PersonalRecord(
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("max_weight") val maxWeight: String,
)

data class LastWorkout(
    val id: Int,
    val date: String,
    @SerializedName("plan_name") val planName: String,
    val done: Int,
    val total: Int,
)
