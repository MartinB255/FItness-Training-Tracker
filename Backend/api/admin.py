"""Admin site wiring. Registers every model under /admin/."""

from django.contrib import admin
from .models import (
    Exercise,
    TrainingPlan,
    PlanExercise,
    WorkoutSession,
    ExerciseLog,
)


class PlanExerciseInline(admin.TabularInline):
    model = PlanExercise
    extra = 1


class ExerciseLogInline(admin.TabularInline):
    model = ExerciseLog
    extra = 1


@admin.register(Exercise)
class ExerciseAdmin(admin.ModelAdmin):
    list_display = ["name", "user", "created_at"]
    list_filter = ["user"]
    search_fields = ["name"]


@admin.register(TrainingPlan)
class TrainingPlanAdmin(admin.ModelAdmin):
    list_display = ["name", "user", "created_at", "updated_at"]
    list_filter = ["user"]
    search_fields = ["name"]
    inlines = [PlanExerciseInline]


@admin.register(PlanExercise)
class PlanExerciseAdmin(admin.ModelAdmin):
    list_display = ["exercise", "training_plan", "sets", "reps", "weight", "order"]
    list_filter = ["training_plan"]


@admin.register(WorkoutSession)
class WorkoutSessionAdmin(admin.ModelAdmin):
    list_display = ["date", "user", "training_plan", "duration_seconds", "completed"]
    list_filter = ["completed", "user", "date"]
    search_fields = ["notes"]
    inlines = [ExerciseLogInline]


@admin.register(ExerciseLog)
class ExerciseLogAdmin(admin.ModelAdmin):
    list_display = ["exercise_name", "sets", "reps", "weight", "status", "workout_session"]
    list_filter = ["exercise_name", "status"]
    search_fields = ["exercise_name"]
