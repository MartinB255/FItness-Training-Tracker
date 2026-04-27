package com.fittrack.app.data.repository

import com.fittrack.app.data.api.RetrofitClient
import com.fittrack.app.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

/**
 * Thin wrapper around [FitTrackApi] that returns Kotlin [Result]s and turns
 * DRF error bodies into human-readable messages. Activities call into here
 * from `lifecycleScope`; no caching — each screen re-fetches on resume.
 */
object FitTrackRepository {

    private val api get() = RetrofitClient.api

    // ── Helpers ──────────────────────────────────────────────────

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response body"))
            } else {
                val raw = response.errorBody()?.string().orEmpty()
                Result.failure(Exception(parseErrorMessage(raw, response.code())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** DELETE and other no-body calls — success == 2xx, body isn't required. */
    private suspend fun safeUnitCall(call: suspend () -> Response<Unit>): Result<Unit> {
        return try {
            val response = call()
            if (response.isSuccessful) Result.success(Unit)
            else {
                val raw = response.errorBody()?.string().orEmpty()
                Result.failure(Exception(parseErrorMessage(raw, response.code())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Turn a DRF error body into a readable string. */
    private fun parseErrorMessage(raw: String, code: Int): String {
        if (raw.isBlank()) return "HTTP $code"
        return try {
            val json = JSONObject(raw)
            json.optString("detail").takeIf { it.isNotEmpty() }
                ?: buildString {
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = when (val v = json.get(key)) {
                            is JSONArray ->
                                (0 until v.length()).joinToString(" ") { v.getString(it) }
                            else -> v.toString()
                        }
                        if (isNotEmpty()) append("\n")
                        append(value)
                    }
                }.ifEmpty { "HTTP $code" }
        } catch (_: Exception) {
            raw
        }
    }

    // ── Auth ─────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): Result<AuthResponse> =
        safeCall { api.login(LoginRequest(username, password)) }

    suspend fun register(
        username: String, email: String, password: String,
    ): Result<AuthResponse> =
        safeCall { api.register(RegisterRequest(username, email, password)) }

    // ── Exercise catalog ─────────────────────────────────────────

    suspend fun getExercises(): Result<List<Exercise>> =
        safeCall { api.getExercises() }

    suspend fun createExercise(name: String): Result<Exercise> =
        safeCall { api.createExercise(CreateExerciseRequest(name)) }

    suspend fun updateExercise(id: Int, name: String): Result<Exercise> =
        safeCall { api.updateExercise(id, CreateExerciseRequest(name)) }

    suspend fun deleteExercise(id: Int): Result<Unit> =
        safeUnitCall { api.deleteExercise(id) }

    // ── Training plans ───────────────────────────────────────────

    suspend fun getPlans(): Result<List<TrainingPlan>> =
        safeCall { api.getPlans() }

    suspend fun getPlan(id: Int): Result<TrainingPlan> =
        safeCall { api.getPlan(id) }

    suspend fun createPlan(name: String, description: String = ""): Result<TrainingPlan> =
        safeCall { api.createPlan(CreatePlanRequest(name, description)) }

    suspend fun deletePlan(id: Int): Result<Unit> =
        safeUnitCall { api.deletePlan(id) }

    // ── Plan-exercise links ──────────────────────────────────────

    suspend fun addPlanExercise(
        planId: Int,
        exerciseId: Int,
        sets: Int = 3,
        reps: Int = 10,
        weight: String = "0",
    ): Result<PlanExercise> = safeCall {
        api.addPlanExercise(
            CreatePlanExerciseRequest(planId, exerciseId, sets, reps, weight),
        )
    }

    suspend fun removePlanExercise(id: Int): Result<Unit> =
        safeUnitCall { api.removePlanExercise(id) }

    suspend fun updatePlanExercise(
        id: Int,
        sets: Int,
        reps: Int,
        weight: String,
    ): Result<PlanExercise> = safeCall {
        api.updatePlanExercise(id, UpdatePlanExerciseRequest(sets, reps, weight))
    }

    // ── Workout sessions ─────────────────────────────────────────

    suspend fun getSessions(): Result<List<WorkoutSession>> =
        safeCall { api.getSessions() }

    suspend fun getSession(id: Int): Result<WorkoutSession> =
        safeCall { api.getSession(id) }

    suspend fun createSession(body: CreateSessionRequest): Result<WorkoutSession> =
        safeCall { api.createSession(body) }

    suspend fun deleteSession(id: Int): Result<Unit> =
        safeUnitCall { api.deleteSession(id) }

    // ── Progress / charts ────────────────────────────────────────

    suspend fun getWeeklyVolume(): Result<List<WeeklyVolume>> =
        safeCall { api.getWeeklyVolume() }

    suspend fun getDashboard(): Result<DashboardData> =
        safeCall { api.getDashboard() }
}
