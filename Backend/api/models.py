"""
Database models for the Fitness Training Tracker.

Schema overview:
    User (Django built-in)
    ├── Exercise       — flat per-user catalog ("Bench Press", "Squat", …)
    ├── TrainingPlan   — a named collection of exercises
    │   └── PlanExercise — one exercise inside a plan (sets, reps, weight, order)
    └── WorkoutSession — one logged workout
        └── ExerciseLog  — one exercise performed in that session, with status
"""

from django.conf import settings
from django.db import models


class Exercise(models.Model):
    """
    Flat, per-user exercise catalog entry. Just a name ("Bench Press").
    Plans reference these through PlanExercise; logs reference them by FK
    but also cache the name so logs survive if the exercise is later deleted.
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="exercises",
    )
    name = models.CharField(max_length=200)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["name"]
        unique_together = [("user", "name")]

    def __str__(self):
        return self.name


class TrainingPlan(models.Model):
    """A reusable training plan created by a user ('Push/Pull/Legs', …)."""
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


class PlanExercise(models.Model):
    """
    Join table: one Exercise placed inside one TrainingPlan with the
    default sets/reps/weight the user plans to perform.
    """
    training_plan = models.ForeignKey(
        TrainingPlan,
        on_delete=models.CASCADE,
        related_name="plan_exercises",
    )
    exercise = models.ForeignKey(
        Exercise,
        on_delete=models.CASCADE,
        related_name="plan_usages",
    )
    sets = models.PositiveIntegerField(default=3)
    reps = models.PositiveIntegerField(default=10)
    weight = models.DecimalField(
        max_digits=6, decimal_places=2, default=0,
        help_text="Default weight in kg",
    )
    order = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["order", "id"]

    def __str__(self):
        return f"{self.exercise.name} in {self.training_plan.name}"


class WorkoutSession(models.Model):
    """
    One workout session. Optionally linked to a TrainingPlan.
    duration_seconds is the chronometer time recorded on the client.
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
    duration_seconds = models.PositiveIntegerField(default=0)
    notes = models.TextField(blank=True, default="")
    completed = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-date", "-id"]

    def __str__(self):
        plan_name = self.training_plan.name if self.training_plan else "Freestyle"
        return f"{self.date} — {plan_name} ({self.user.username})"


class ExerciseLog(models.Model):
    """
    One exercise performed during a workout session, with the user's
    actual sets/reps/weight and the per-exercise status flag.
    """
    STATUS_DONE = "done"
    STATUS_SKIPPED = "skipped"
    STATUS_NOT_DONE = "not_done"
    STATUS_CHOICES = [
        (STATUS_DONE, "Done"),
        (STATUS_SKIPPED, "Skipped"),
        (STATUS_NOT_DONE, "Not done"),
    ]

    workout_session = models.ForeignKey(
        WorkoutSession,
        on_delete=models.CASCADE,
        related_name="exercise_logs",
    )
    # FK may be null if the catalog entry was deleted later — name is the fallback.
    exercise = models.ForeignKey(
        Exercise,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="logs",
    )
    exercise_name = models.CharField(max_length=200)
    sets = models.PositiveIntegerField()
    reps = models.PositiveIntegerField()
    weight = models.DecimalField(max_digits=6, decimal_places=2)
    status = models.CharField(
        max_length=10,
        choices=STATUS_CHOICES,
        default=STATUS_DONE,
    )

    class Meta:
        ordering = ["id"]

    def __str__(self):
        return (
            f"{self.exercise_name}: "
            f"{self.sets}×{self.reps} @ {self.weight}kg [{self.status}]"
        )
