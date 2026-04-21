package com.fittrack.app.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════════
//  AUTH
// ═══════════════════════════════════════════════════════════════════

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val username: String,
    val email: String
)

// ═══════════════════════════════════════════════════════════════════
//  TRAINING PLANS
// ═══════════════════════════════════════════════════════════════════

data class TrainingPlan(
    val id: Int,
    val user: Int,
    val name: String,
    val description: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val exercises: List<Exercise>
)

data class CreatePlanRequest(
    val name: String,
    val description: String = ""
)

// ═══════════════════════════════════════════════════════════════════
//  EXERCISES
// ═══════════════════════════════════════════════════════════════════

data class Exercise(
    val id: Int,
    @SerializedName("training_plan") val trainingPlan: Int,
    val name: String,
    @SerializedName("default_sets") val defaultSets: Int,
    @SerializedName("default_reps") val defaultReps: Int,
    @SerializedName("default_weight") val defaultWeight: String,
    val order: Int
)

data class CreateExerciseRequest(
    @SerializedName("training_plan") val trainingPlan: Int,
    val name: String,
    @SerializedName("default_sets") val defaultSets: Int,
    @SerializedName("default_reps") val defaultReps: Int,
    @SerializedName("default_weight") val defaultWeight: String,
    val order: Int = 0
)

// ═══════════════════════════════════════════════════════════════════
//  WORKOUT SESSIONS
// ═══════════════════════════════════════════════════════════════════

data class WorkoutSession(
    val id: Int,
    val user: Int,
    @SerializedName("training_plan") val trainingPlan: Int?,
    val date: String,
    val notes: String,
    val completed: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("exercise_logs") val exerciseLogs: List<ExerciseLog>
)

data class CreateSessionRequest(
    @SerializedName("training_plan") val trainingPlan: Int?,
    val date: String,
    val notes: String = "",
    val completed: Boolean = true,
    @SerializedName("exercise_logs") val exerciseLogs: List<CreateLogRequest>
)

// ═══════════════════════════════════════════════════════════════════
//  EXERCISE LOGS
// ═══════════════════════════════════════════════════════════════════

data class ExerciseLog(
    val id: Int,
    @SerializedName("workout_session") val workoutSession: Int,
    @SerializedName("exercise_name") val exerciseName: String,
    val exercise: Int?,
    val sets: Int,
    val reps: Int,
    val weight: String
)

data class CreateLogRequest(
    @SerializedName("workout_session") val workoutSession: Int? = null,
    @SerializedName("exercise_name") val exerciseName: String,
    val exercise: Int? = null,
    val sets: Int,
    val reps: Int,
    val weight: String
)

// ═══════════════════════════════════════════════════════════════════
//  PROGRESS / CHARTS
// ═══════════════════════════════════════════════════════════════════

data class ProgressEntry(
    val date: String,
    val sets: Int,
    val reps: Int,
    val weight: String
)

data class WeeklyVolume(
    val week: String,
    @SerializedName("total_volume") val totalVolume: Double,
    @SerializedName("session_count") val sessionCount: Int
)

data class DashboardData(
    @SerializedName("total_workouts") val totalWorkouts: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("personal_records") val personalRecords: List<PersonalRecord>
)

data class PersonalRecord(
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("max_weight") val maxWeight: String
)
