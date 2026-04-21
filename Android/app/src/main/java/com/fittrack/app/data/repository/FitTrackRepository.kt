package com.fittrack.app.data.repository

import com.fittrack.app.data.api.RetrofitClient
import com.fittrack.app.data.model.*
import retrofit2.Response

/**
 * Repository wraps all API calls with simple Result-based error handling.
 * ViewModels call repository methods instead of the API directly.
 */
object FitTrackRepository {

    private val api get() = RetrofitClient.api

    // ── Helper: unwrap Retrofit Response into Result ────────────

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Special version for DELETE calls that return no body
    private suspend fun safeDeleteCall(call: suspend () -> Response<Unit>): Result<Unit> {
        return try {
            val response = call()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Auth ────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return safeCall { api.login(LoginRequest(username, password)) }
    }

    suspend fun register(username: String, email: String, password: String): Result<AuthResponse> {
        return safeCall { api.register(RegisterRequest(username, email, password)) }
    }

    // ── Training Plans ──────────────────────────────────────────

    suspend fun getPlans(): Result<List<TrainingPlan>> {
        return safeCall { api.getPlans() }
    }

    suspend fun createPlan(name: String, description: String = ""): Result<TrainingPlan> {
        return safeCall { api.createPlan(CreatePlanRequest(name, description)) }
    }

    suspend fun updatePlan(id: Int, name: String, description: String = ""): Result<TrainingPlan> {
        return safeCall { api.updatePlan(id, CreatePlanRequest(name, description)) }
    }

    suspend fun deletePlan(id: Int): Result<Unit> {
        return safeDeleteCall { api.deletePlan(id) }
    }

    // ── Exercises ───────────────────────────────────────────────

    suspend fun getExercises(planId: Int): Result<List<Exercise>> {
        return safeCall { api.getExercises(planId) }
    }

    suspend fun createExercise(
        planId: Int, name: String, sets: Int, reps: Int, weight: String
    ): Result<Exercise> {
        return safeCall {
            api.createExercise(CreateExerciseRequest(planId, name, sets, reps, weight))
        }
    }

    suspend fun updateExercise(
        id: Int, planId: Int, name: String, sets: Int, reps: Int, weight: String
    ): Result<Exercise> {
        return safeCall {
            api.updateExercise(id, CreateExerciseRequest(planId, name, sets, reps, weight))
        }
    }

    suspend fun deleteExercise(id: Int): Result<Unit> {
        return safeDeleteCall { api.deleteExercise(id) }
    }

    // ── Workout Sessions ────────────────────────────────────────

    suspend fun getSessions(): Result<List<WorkoutSession>> {
        return safeCall { api.getSessions() }
    }

    suspend fun createSession(request: CreateSessionRequest): Result<WorkoutSession> {
        return safeCall { api.createSession(request) }
    }

    suspend fun deleteSession(id: Int): Result<Unit> {
        return safeDeleteCall { api.deleteSession(id) }
    }

    // ── Progress ────────────────────────────────────────────────

    suspend fun getExerciseProgress(exerciseId: Int): Result<List<ProgressEntry>> {
        return safeCall { api.getExerciseProgress(exerciseId) }
    }

    suspend fun getWeeklyVolume(): Result<List<WeeklyVolume>> {
        return safeCall { api.getWeeklyVolume() }
    }

    suspend fun getDashboard(): Result<DashboardData> {
        return safeCall { api.getDashboard() }
    }
}
