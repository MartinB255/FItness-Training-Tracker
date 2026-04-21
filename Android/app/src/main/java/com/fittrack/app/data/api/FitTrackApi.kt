package com.fittrack.app.data.api

import com.fittrack.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface defining all backend API calls.
 * Base URL is set in RetrofitClient.
 */
interface FitTrackApi {

    // ── Auth ────────────────────────────────────────────────────

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me/")
    suspend fun getMe(): Response<User>

    // ── Training Plans ──────────────────────────────────────────

    @GET("plans/")
    suspend fun getPlans(): Response<List<TrainingPlan>>

    @POST("plans/")
    suspend fun createPlan(@Body plan: CreatePlanRequest): Response<TrainingPlan>

    @GET("plans/{id}/")
    suspend fun getPlan(@Path("id") id: Int): Response<TrainingPlan>

    @PUT("plans/{id}/")
    suspend fun updatePlan(@Path("id") id: Int, @Body plan: CreatePlanRequest): Response<TrainingPlan>

    @DELETE("plans/{id}/")
    suspend fun deletePlan(@Path("id") id: Int): Response<Unit>

    // ── Exercises ───────────────────────────────────────────────

    @GET("exercises/")
    suspend fun getExercises(@Query("plan") planId: Int): Response<List<Exercise>>

    @POST("exercises/")
    suspend fun createExercise(@Body exercise: CreateExerciseRequest): Response<Exercise>

    @PUT("exercises/{id}/")
    suspend fun updateExercise(@Path("id") id: Int, @Body exercise: CreateExerciseRequest): Response<Exercise>

    @DELETE("exercises/{id}/")
    suspend fun deleteExercise(@Path("id") id: Int): Response<Unit>

    // ── Workout Sessions ────────────────────────────────────────

    @GET("sessions/")
    suspend fun getSessions(): Response<List<WorkoutSession>>

    @POST("sessions/")
    suspend fun createSession(@Body session: CreateSessionRequest): Response<WorkoutSession>

    @GET("sessions/{id}/")
    suspend fun getSession(@Path("id") id: Int): Response<WorkoutSession>

    @DELETE("sessions/{id}/")
    suspend fun deleteSession(@Path("id") id: Int): Response<Unit>

    // ── Exercise Logs ───────────────────────────────────────────

    @GET("logs/")
    suspend fun getLogs(@Query("session") sessionId: Int): Response<List<ExerciseLog>>

    @POST("logs/")
    suspend fun createLog(@Body log: CreateLogRequest): Response<ExerciseLog>

    @PUT("logs/{id}/")
    suspend fun updateLog(@Path("id") id: Int, @Body log: CreateLogRequest): Response<ExerciseLog>

    @DELETE("logs/{id}/")
    suspend fun deleteLog(@Path("id") id: Int): Response<Unit>

    // ── Progress / Charts ───────────────────────────────────────

    @GET("progress/exercise/{id}/")
    suspend fun getExerciseProgress(@Path("id") exerciseId: Int): Response<List<ProgressEntry>>

    @GET("progress/weekly/")
    suspend fun getWeeklyVolume(): Response<List<WeeklyVolume>>

    @GET("progress/dashboard/")
    suspend fun getDashboard(): Response<DashboardData>
}
