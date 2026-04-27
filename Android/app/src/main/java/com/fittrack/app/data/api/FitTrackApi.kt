package com.fittrack.app.data.api

import com.fittrack.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface describing every backend endpoint.
 * Base URL is set in [RetrofitClient]; auth token is attached via interceptor.
 */
interface FitTrackApi {

    // ── Auth ─────────────────────────────────────────────────────

    @POST("auth/register/")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("auth/me/")
    suspend fun getMe(): Response<User>

    // ── Exercise catalog ─────────────────────────────────────────

    @GET("exercises/")
    suspend fun getExercises(): Response<List<Exercise>>

    @POST("exercises/")
    suspend fun createExercise(@Body body: CreateExerciseRequest): Response<Exercise>

    @PATCH("exercises/{id}/")
    suspend fun updateExercise(
        @Path("id") id: Int,
        @Body body: CreateExerciseRequest,
    ): Response<Exercise>

    @DELETE("exercises/{id}/")
    suspend fun deleteExercise(@Path("id") id: Int): Response<Unit>

    // ── Training plans ───────────────────────────────────────────

    @GET("plans/")
    suspend fun getPlans(): Response<List<TrainingPlan>>

    @POST("plans/")
    suspend fun createPlan(@Body body: CreatePlanRequest): Response<TrainingPlan>

    @GET("plans/{id}/")
    suspend fun getPlan(@Path("id") id: Int): Response<TrainingPlan>

    @PUT("plans/{id}/")
    suspend fun updatePlan(
        @Path("id") id: Int,
        @Body body: CreatePlanRequest,
    ): Response<TrainingPlan>

    @DELETE("plans/{id}/")
    suspend fun deletePlan(@Path("id") id: Int): Response<Unit>

    // ── Plan-exercise links ──────────────────────────────────────

    @POST("plan-exercises/")
    suspend fun addPlanExercise(
        @Body body: CreatePlanExerciseRequest,
    ): Response<PlanExercise>

    @PATCH("plan-exercises/{id}/")
    suspend fun updatePlanExercise(
        @Path("id") id: Int,
        @Body body: UpdatePlanExerciseRequest,
    ): Response<PlanExercise>

    @DELETE("plan-exercises/{id}/")
    suspend fun removePlanExercise(@Path("id") id: Int): Response<Unit>

    // ── Workout sessions ─────────────────────────────────────────

    @GET("sessions/")
    suspend fun getSessions(): Response<List<WorkoutSession>>

    @POST("sessions/")
    suspend fun createSession(@Body body: CreateSessionRequest): Response<WorkoutSession>

    @GET("sessions/{id}/")
    suspend fun getSession(@Path("id") id: Int): Response<WorkoutSession>

    @DELETE("sessions/{id}/")
    suspend fun deleteSession(@Path("id") id: Int): Response<Unit>

    // ── Exercise logs (usually created via nested POST /sessions/) ─

    @GET("exercise-logs/")
    suspend fun getLogs(@Query("session") sessionId: Int): Response<List<ExerciseLog>>

    @POST("exercise-logs/")
    suspend fun createLog(@Body body: CreateLogRequest): Response<ExerciseLog>

    @DELETE("exercise-logs/{id}/")
    suspend fun deleteLog(@Path("id") id: Int): Response<Unit>

    // ── Progress / charts ────────────────────────────────────────

    @GET("weekly-volume/")
    suspend fun getWeeklyVolume(): Response<List<WeeklyVolume>>

    @GET("dashboard/")
    suspend fun getDashboard(): Response<DashboardData>
}
