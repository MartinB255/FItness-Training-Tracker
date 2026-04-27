"""
API views — all endpoints for the fitness tracker.

Endpoints (all prefixed with /api/):
    POST   auth/register/            — create user, return token
    POST   auth/login/               — return token for username+password
    GET    auth/me/                  — current user info

    GET/POST       exercises/        — flat exercise catalog
    GET/PUT/DELETE exercises/{id}/

    GET/POST       plans/            — training plans (with nested plan_exercises)
    GET/PUT/DELETE plans/{id}/

    GET/POST       plan-exercises/   — add/remove exercises in a plan
    GET/PUT/DELETE plan-exercises/{id}/

    GET/POST       sessions/         — workout sessions (with nested exercise_logs)
    GET/DELETE     sessions/{id}/

    GET/POST       exercise-logs/    — individual logs
    GET/PUT/DELETE exercise-logs/{id}/

    GET weekly-volume/     — weekly total volume since first session (bar chart)
    GET dashboard/         — dashboard summary (totals, streak, PRs)
"""

from datetime import timedelta

from django.contrib.auth.models import User
from django.db.models import Max
from django.utils import timezone
from rest_framework import generics, permissions, status, viewsets
from rest_framework.authtoken.models import Token
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response

from .models import (
    Exercise,
    TrainingPlan,
    PlanExercise,
    WorkoutSession,
    ExerciseLog,
)
from .serializers import (
    RegisterSerializer,
    UserSerializer,
    ExerciseSerializer,
    TrainingPlanSerializer,
    PlanExerciseSerializer,
    WorkoutSessionSerializer,
    WorkoutSessionCreateSerializer,
    ExerciseLogSerializer,
)


# ═══════════════════════════════════════════════════════════════════
#  AUTH
# ═══════════════════════════════════════════════════════════════════

class RegisterView(generics.CreateAPIView):
    """POST /api/auth/register/ — create user, return token."""
    queryset = User.objects.all()
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        token, _ = Token.objects.get_or_create(user=user)
        return Response(
            {"token": token.key, "user": UserSerializer(user).data},
            status=status.HTTP_201_CREATED,
        )


@api_view(["POST"])
@permission_classes([permissions.AllowAny])
def login_view(request):
    """POST /api/auth/login/ — return token for valid credentials."""
    username = request.data.get("username")
    password = request.data.get("password")
    if not username or not password:
        return Response(
            {"error": "Username and password are required."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    try:
        user = User.objects.get(username=username)
    except User.DoesNotExist:
        return Response(
            {"error": "Invalid credentials."},
            status=status.HTTP_401_UNAUTHORIZED,
        )
    if not user.check_password(password):
        return Response(
            {"error": "Invalid credentials."},
            status=status.HTTP_401_UNAUTHORIZED,
        )

    token, _ = Token.objects.get_or_create(user=user)
    return Response({"token": token.key, "user": UserSerializer(user).data})


@api_view(["GET"])
def me_view(request):
    """GET /api/auth/me/ — currently authenticated user."""
    return Response(UserSerializer(request.user).data)


# ═══════════════════════════════════════════════════════════════════
#  CRUD VIEWSETS
# ═══════════════════════════════════════════════════════════════════

class ExerciseViewSet(viewsets.ModelViewSet):
    """Flat exercise catalog, scoped to the current user."""
    serializer_class = ExerciseSerializer

    def get_queryset(self):
        return Exercise.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


class TrainingPlanViewSet(viewsets.ModelViewSet):
    """Training plans, scoped to the current user. Embeds plan_exercises."""
    serializer_class = TrainingPlanSerializer

    def get_queryset(self):
        return TrainingPlan.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


class PlanExerciseViewSet(viewsets.ModelViewSet):
    """
    Manage the exercises inside a plan.
    Optional filter: ?plan={id}
    """
    serializer_class = PlanExerciseSerializer

    def get_queryset(self):
        qs = PlanExercise.objects.filter(
            training_plan__user=self.request.user,
        )
        plan_id = self.request.query_params.get("plan")
        if plan_id:
            qs = qs.filter(training_plan_id=plan_id)
        return qs


class WorkoutSessionViewSet(viewsets.ModelViewSet):
    """Sessions, scoped to the current user. POST creates session + logs."""

    def get_queryset(self):
        return WorkoutSession.objects.filter(user=self.request.user)

    def get_serializer_class(self):
        if self.action == "create":
            return WorkoutSessionCreateSerializer
        return WorkoutSessionSerializer

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


class ExerciseLogViewSet(viewsets.ModelViewSet):
    """
    Individual log CRUD. Filters: ?session={id}&exercise={id}
    """
    serializer_class = ExerciseLogSerializer

    def get_queryset(self):
        qs = ExerciseLog.objects.filter(
            workout_session__user=self.request.user,
        )
        session_id = self.request.query_params.get("session")
        if session_id:
            qs = qs.filter(workout_session_id=session_id)
        exercise_id = self.request.query_params.get("exercise")
        if exercise_id:
            qs = qs.filter(exercise_id=exercise_id)
        return qs


# ═══════════════════════════════════════════════════════════════════
#  PROGRESS / CHARTS
# ═══════════════════════════════════════════════════════════════════

@api_view(["GET"])
def weekly_volume(request):
    """
    GET /api/weekly-volume/
    Total volume (sets × reps × weight) per ISO week, since the user's
    first tracked session.
    """
    logs = (
        ExerciseLog.objects
        .filter(
            workout_session__user=request.user,
            status=ExerciseLog.STATUS_DONE,
        )
        .select_related("workout_session")
    )

    weekly = {}
    for log in logs:
        iso = log.workout_session.date.isocalendar()
        key = f"{iso[0]}-W{iso[1]:02d}"
        bucket = weekly.setdefault(
            key, {"total_volume": 0.0, "sessions": set()}
        )
        bucket["total_volume"] += float(log.sets * log.reps * log.weight)
        bucket["sessions"].add(log.workout_session_id)

    return Response(sorted(
        [
            {
                "week": w,
                "total_volume": round(info["total_volume"], 2),
                "session_count": len(info["sessions"]),
            }
            for w, info in weekly.items()
        ],
        key=lambda x: x["week"],
    ))


@api_view(["GET"])
def dashboard(request):
    """
    GET /api/dashboard/
    Summary stats for the dashboard screen.
    """
    user = request.user
    sessions = WorkoutSession.objects.filter(user=user)

    total_workouts = sessions.count()

    # ── Sessions this week (Monday-to-Sunday) ───────────────────
    today = timezone.now().date()
    start_of_week = today - timedelta(days=today.weekday())
    sessions_this_week = sessions.filter(date__gte=start_of_week).count()

    # ── Current streak (consecutive days with a workout) ────────
    dates = list(
        sessions.values_list("date", flat=True).distinct().order_by("-date")
    )
    streak = 0
    expected = today
    for i, d in enumerate(dates):
        if d == expected:
            streak += 1
            expected = d - timedelta(days=1)
        elif i == 0 and d == today - timedelta(days=1):
            # haven't worked out today yet — let the streak start from yesterday
            streak += 1
            expected = d - timedelta(days=1)
        else:
            break

    # ── Personal records (max weight per exercise) ──────────────
    prs = (
        ExerciseLog.objects
        .filter(
            workout_session__user=user,
            status=ExerciseLog.STATUS_DONE,
        )
        .values("exercise_name")
        .annotate(max_weight=Max("weight"))
        .order_by("-max_weight")
    )
    personal_records = [
        {"exercise_name": pr["exercise_name"], "max_weight": str(pr["max_weight"])}
        for pr in prs
    ]

    # ── Last workout (plan name + date + done/total counts) ─────
    last_session = sessions.order_by("-date", "-id").first()
    last = None
    if last_session is not None:
        total = last_session.exercise_logs.count()
        done = last_session.exercise_logs.filter(
            status=ExerciseLog.STATUS_DONE,
        ).count()
        last = {
            "id": last_session.id,
            "date": last_session.date.isoformat(),
            "plan_name": (
                last_session.training_plan.name
                if last_session.training_plan else "Freestyle"
            ),
            "done": done,
            "total": total,
        }

    return Response({
        "total_workouts": total_workouts,
        "sessions_this_week": sessions_this_week,
        "current_streak": streak,
        "personal_records": personal_records,
        "last_workout": last,
    })
