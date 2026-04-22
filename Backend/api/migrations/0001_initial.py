# Clean initial migration for the restructured schema.
# If you previously applied the old migration, drop the api tables first:
#     python manage.py migrate api zero
# then run `python manage.py migrate` again to apply this one.

import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="Exercise",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("name", models.CharField(max_length=200)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="exercises", to=settings.AUTH_USER_MODEL)),
            ],
            options={
                "ordering": ["name"],
                "unique_together": {("user", "name")},
            },
        ),
        migrations.CreateModel(
            name="TrainingPlan",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("name", models.CharField(max_length=200)),
                ("description", models.TextField(blank=True, default="")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="training_plans", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["-updated_at"]},
        ),
        migrations.CreateModel(
            name="PlanExercise",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("sets", models.PositiveIntegerField(default=3)),
                ("reps", models.PositiveIntegerField(default=10)),
                ("weight", models.DecimalField(decimal_places=2, default=0, help_text="Default weight in kg", max_digits=6)),
                ("order", models.PositiveIntegerField(default=0)),
                ("exercise", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="plan_usages", to="api.exercise")),
                ("training_plan", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="plan_exercises", to="api.trainingplan")),
            ],
            options={"ordering": ["order", "id"]},
        ),
        migrations.CreateModel(
            name="WorkoutSession",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("date", models.DateField()),
                ("duration_seconds", models.PositiveIntegerField(default=0)),
                ("notes", models.TextField(blank=True, default="")),
                ("completed", models.BooleanField(default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("training_plan", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="workout_sessions", to="api.trainingplan")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="workout_sessions", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["-date", "-id"]},
        ),
        migrations.CreateModel(
            name="ExerciseLog",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("exercise_name", models.CharField(max_length=200)),
                ("sets", models.PositiveIntegerField()),
                ("reps", models.PositiveIntegerField()),
                ("weight", models.DecimalField(decimal_places=2, max_digits=6)),
                ("status", models.CharField(choices=[("done", "Done"), ("skipped", "Skipped"), ("not_done", "Not done")], default="done", max_length=10)),
                ("exercise", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="logs", to="api.exercise")),
                ("workout_session", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="exercise_logs", to="api.workoutsession")),
            ],
            options={"ordering": ["id"]},
        ),
    ]
