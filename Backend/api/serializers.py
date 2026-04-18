"""
DRF serializers — convert model instances to/from JSON.

Nested serializers are used where it makes sense:
  - TrainingPlan includes its Exercises
  - WorkoutSession includes its ExerciseLogs
"""

from django.contrib.auth.models import User
from rest_framework import serializers

from .models import TrainingPlan, Exercise, WorkoutSession, ExerciseLog


# ── Auth serializers ─────────────────────────────────────────────

class RegisterSerializer(serializers.ModelSerializer):
    """Handles user registration. Password is write-only."""
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = User
        fields = ["id", "username", "email", "password"]

    def create(self, validated_data):
        # create_user hashes the password automatically
        return User.objects.create_user(**validated_data)


class UserSerializer(serializers.ModelSerializer):
    """Read-only user info (returned after login / in responses)."""
    class Meta:
        model = User
        fields = ["id", "username", "email"]


# ── Exercise ─────────────────────────────────────────────────────

class ExerciseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Exercise
        fields = [
            "id", "training_plan", "name",
            "default_sets", "default_reps", "default_weight", "order",
        ]
        read_only_fields = ["id"]


# ── Training Plan (with nested exercises) ────────────────────────

class TrainingPlanSerializer(serializers.ModelSerializer):
    """
    Returns the plan with all its exercises embedded.
    The 'user' field is set automatically from the request.
    """
    exercises = ExerciseSerializer(many=True, read_only=True)

    class Meta:
        model = TrainingPlan
        fields = [
            "id", "user", "name", "description",
            "created_at", "updated_at", "exercises",
        ]
        read_only_fields = ["id", "user", "created_at", "updated_at"]


# ── Exercise Log ─────────────────────────────────────────────────

class ExerciseLogSerializer(serializers.ModelSerializer):
    class Meta:
        model = ExerciseLog
        fields = [
            "id", "workout_session", "exercise_name",
            "exercise", "sets", "reps", "weight",
        ]
        read_only_fields = ["id"]


# ── Workout Session (with nested logs) ───────────────────────────

class WorkoutSessionSerializer(serializers.ModelSerializer):
    """
    Returns the session with all exercise logs embedded.
    Supports creating a session with logs in one request via
    the nested 'exercise_logs' field.
    """
    exercise_logs = ExerciseLogSerializer(many=True, read_only=True)

    class Meta:
        model = WorkoutSession
        fields = [
            "id", "user", "training_plan", "date",
            "notes", "completed", "created_at", "exercise_logs",
        ]
        read_only_fields = ["id", "user", "created_at"]


class WorkoutSessionCreateSerializer(serializers.ModelSerializer):
    """
    Accepts a session with nested exercise logs in one POST request.
    Example payload:
    {
        "training_plan": 1,
        "date": "2025-04-19",
        "notes": "Felt strong today",
        "completed": true,
        "exercise_logs": [
            {"exercise_name": "Bench Press", "exercise": 1, "sets": 3, "reps": 10, "weight": 80.0},
            {"exercise_name": "Rows", "exercise": 2, "sets": 3, "reps": 12, "weight": 60.0}
        ]
    }
    """
    exercise_logs = ExerciseLogSerializer(many=True)

    class Meta:
        model = WorkoutSession
        fields = [
            "id", "training_plan", "date",
            "notes", "completed", "exercise_logs",
        ]

    def create(self, validated_data):
        logs_data = validated_data.pop("exercise_logs")
        session = WorkoutSession.objects.create(**validated_data)
        for log_data in logs_data:
            ExerciseLog.objects.create(workout_session=session, **log_data)
        return session
