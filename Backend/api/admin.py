"""
Admin interface configuration.
Registers all models so you can manage data at /admin/.
"""

from django.contrib import admin
from .models import TrainingPlan, Exercise, WorkoutSession, ExerciseLog


# ── Inline editors (show child records inside parent) ────────────

class ExerciseInline(admin.TabularInline):
    """Show exercises inline when editing a training plan."""
    model = Exercise
    extra = 1  # one empty row for adding new exercises


class ExerciseLogInline(admin.TabularInline):
    """Show exercise logs inline when editing a workout session."""
    model = ExerciseLog
    extra = 1


# ── Model admins ─────────────────────────────────────────────────

@admin.register(TrainingPlan)
class TrainingPlanAdmin(admin.ModelAdmin):
    list_display = ["name", "user", "created_at", "updated_at"]
    list_filter = ["user"]
    search_fields = ["name"]
    inlines = [ExerciseInline]


@admin.register(Exercise)
class ExerciseAdmin(admin.ModelAdmin):
    list_display = [
        "name", "training_plan", "default_sets",
        "default_reps", "default_weight", "order",
    ]
    list_filter = ["training_plan"]
    search_fields = ["name"]


@admin.register(WorkoutSession)
class WorkoutSessionAdmin(admin.ModelAdmin):
    list_display = ["date", "user", "training_plan", "completed", "created_at"]
    list_filter = ["completed", "user", "date"]
    search_fields = ["notes"]
    inlines = [ExerciseLogInline]


@admin.register(ExerciseLog)
class ExerciseLogAdmin(admin.ModelAdmin):
    list_display = [
        "exercise_name", "sets", "reps", "weight", "workout_session",
    ]
    list_filter = ["exercise_name"]
    search_fields = ["exercise_name"]
