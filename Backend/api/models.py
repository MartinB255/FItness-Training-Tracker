"""
Database models for the Fitness Training Tracker.

Schema overview:
    User (Django built-in)
    └── TrainingPlan  — a named collection of exercises (e.g. "Push/Pull/Legs")
        └── Exercise  — a single exercise inside a plan (e.g. "Bench Press")
    └── WorkoutSession — one logged workout on a specific date
        └── ExerciseLog — one exercise performed in that session (sets, reps, weight)
"""

from django.conf import settings
from django.db import models


class TrainingPlan(models.Model):
    """
    A reusable training plan created by a user.
    Example: "Upper Body Day", "Full Body Strength", etc.
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="training_plans",
    )
    name = models.CharField(max_length=200)
    description = models.TextField(blank=True, default="")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-updated_at"]

    def __str__(self):
        return f"{self.name} ({self.user.username})"


class Exercise(models.Model):
    """
    A single exercise that belongs to a training plan.
    Stores the *template* — default sets, reps, and weight that
    the user plans to do. Actual logged values go in ExerciseLog.
    """
    training_plan = models.ForeignKey(
        TrainingPlan,
        on_delete=models.CASCADE,
        related_name="exercises",
    )
    name = models.CharField(max_length=200)
    default_sets = models.PositiveIntegerField(default=3)
    default_reps = models.PositiveIntegerField(default=10)
    default_weight = models.DecimalField(
        max_digits=6, decimal_places=2, default=0,
        help_text="Default weight in kg",
    )
    order = models.PositiveIntegerField(
        default=0,
        help_text="Display order within the training plan",
    )

    class Meta:
        ordering = ["order", "id"]

    def __str__(self):
        return f"{self.name} ({self.training_plan.name})"


class WorkoutSession(models.Model):
    """
    One workout session — represents a single visit to the gym.
    Optionally linked to a training plan (user might do a freestyle workout).
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="workout_sessions",
    )
    training_plan = models.ForeignKey(
        TrainingPlan,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="workout_sessions",
    )
    date = models.DateField()
    notes = models.TextField(blank=True, default="")
    completed = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-date"]

    def __str__(self):
        plan_name = self.training_plan.name if self.training_plan else "Freestyle"
        return f"{self.date} — {plan_name} ({self.user.username})"


class ExerciseLog(models.Model):
    """
    One exercise performed during a workout session.
    Tracks the actual sets, reps, and weight used.
    This is what feeds the progress charts.
    """
    workout_session = models.ForeignKey(
        WorkoutSession,
        on_delete=models.CASCADE,
        related_name="exercise_logs",
    )
    exercise_name = models.CharField(
        max_length=200,
        help_text="Stored as text so logs survive exercise deletion",
    )
    exercise = models.ForeignKey(
        Exercise,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="logs",
    )
    sets = models.PositiveIntegerField()
    reps = models.PositiveIntegerField()
    weight = models.DecimalField(
        max_digits=6, decimal_places=2,
        help_text="Weight in kg",
    )

    class Meta:
        ordering = ["id"]

    def __str__(self):
        return (
            f"{self.exercise_name}: "
            f"{self.sets}×{self.reps} @ {self.weight}kg"
        )
