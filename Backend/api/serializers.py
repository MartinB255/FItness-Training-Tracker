"""
DRF serializers — convert model instances to/from JSON.

Notable shapes:
  - Exercise is a flat catalog entry: {id, name}
  - TrainingPlan embeds its plan_exercises (each with exercise name + defaults)
  - WorkoutSession embeds its exercise_logs (with status)
  - WorkoutSessionCreateSerializer accepts a whole workout in one POST
"""

from django.contrib.auth.models import User
from rest_framework import serializers

from .models import (
    Exercise,
    TrainingPlan,
    PlanExercise,
    WorkoutSession,
    ExerciseLog,
)


# ── Auth ────────────────────────────────────────────────────────

class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = User
        fields = ["id", "username", "email", "password"]

    def create(self, validated_data):
        return User.objects.create_user(**validated_data)


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["id", "username", "email"]


# ── Exercise catalog ────────────────────────────────────────────

class ExerciseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Exercise
        fields = ["id", "name"]
        read_only_fields = ["id"]


# ── Plan exercises (nested inside TrainingPlan) ─────────────────

class PlanExerciseSerializer(serializers.ModelSerializer):
    """Read/write form used both standalone and nested in a plan."""
    exercise_name = serializers.CharField(
        source="exercise.name", read_only=True,
    )

    class Meta:
        model = PlanExercise
        fields = [
            "id", "training_plan", "exercise", "exercise_name",
            "sets", "reps", "weight", "order",
        ]
        read_only_fields = ["id", "exercise_name"]
        # training_plan is optional when nested inside a TrainingPlan payload
        extra_kwargs = {"training_plan": {"required": False}}


# ── Training plan (with nested plan_exercises) ──────────────────

class TrainingPlanSerializer(serializers.ModelSerializer):
    plan_exercises = PlanExerciseSerializer(many=True, read_only=True)

    class Meta:
        model = TrainingPlan
        fields = [
            "id", "user", "name", "description",
            "created_at", "updated_at", "plan_exercises",
        ]
        read_only_fields = ["id", "user", "created_at", "updated_at"]


# ── Exercise log ────────────────────────────────────────────────

class ExerciseLogSerializer(serializers.ModelSerializer):
    class Meta:
        model = ExerciseLog
        fields = [
            "id", "workout_session", "exercise", "exercise_name",
            "sets", "reps", "weight", "status",
        ]
        read_only_fields = ["id"]
        # workout_session may be filled in later when nested inside a session.
        extra_kwargs = {"workout_session": {"required": False}}


# ── Workout session ─────────────────────────────────────────────

class WorkoutSessionSerializer(serializers.ModelSerializer):
    """Read form — embeds logs and the plan name for easy list rendering."""
    exercise_logs = ExerciseLogSerializer(many=True, read_only=True)
    plan_name = serializers.SerializerMethodField()

    class Meta:
        model = WorkoutSession
        fields = [
            "id", "user", "training_plan", "plan_name", "date",
            "duration_seconds", "notes", "completed",
            "created_at", "exercise_logs",
        ]
        read_only_fields = ["id", "user", "created_at", "plan_name"]

    def get_plan_name(self, obj):
        return obj.training_plan.name if obj.training_plan else "Freestyle"


class WorkoutSessionCreateSerializer(serializers.ModelSerializer):
    """
    Accepts a whole session + nested logs in one POST.
    Example payload:
        {
          "training_plan": 1,
          "date": "2026-04-22",
          "duration_seconds": 1830,
          "completed": true,
          "exercise_logs": [
            {"exercise": 3, "exercise_name": "Bench Press",
             "sets": 3, "reps": 10, "weight": "62.5", "status": "done"}
          ]
        }
    """
    exercise_logs = ExerciseLogSerializer(many=True)

    class Meta:
        model = WorkoutSession
        fields = [
            "id", "training_plan", "date",
            "duration_seconds", "notes", "completed",
            "exercise_logs",
        ]

    def create(self, validated_data):
        logs = validated_data.pop("exercise_logs")
        session = WorkoutSession.objects.create(**validated_data)
        for log in logs:
            # drop any stale workout_session key the client might send
            log.pop("workout_session", None)
            ExerciseLog.objects.create(workout_session=session, **log)
        return session
