"""
API views — all endpoints for the fitness tracker.

Endpoints:
    POST   /api/auth/register/        — create a new user
    POST   /api/auth/login/           — get auth token
    GET    /api/auth/me/              — current user info

    GET    /api/plans/                — list user's training plans
    POST   /api/plans/                — create a plan
    GET    /api/plans/{id}/           — plan detail with exercises
    PUT    /api/plans/{id}/           — update a plan
    DELETE /api/plans/{id}/           — delete a plan

    GET    /api/exercises/            — list exercises (filterable by plan)
    POST   /api/exercises/            — create an exercise
    GET    /api/exercises/{id}/       — exercise detail
    PUT    /api/exercises/{id}/       — update an exercise
    DELETE /api/exercises/{id}/       — delete an exercise

    GET    /api/sessions/             — list user's workout sessions
    POST   /api/sessions/             — create session with logs
    GET    /api/sessions/{id}/        — session detail with logs
    PUT    /api/sessions/{id}/        — update a session
    DELETE /api/sessions/{id}/        — delete a session

    GET    /api/logs/                 — list exercise logs (filterable)
    POST   /api/logs/                 — create a single log
    PUT    /api/logs/{id}/            — update a log
    DELETE /api/logs/{id}/            — delete a log

    GET    /api/progress/exercise/{id}/   — weight/reps history for one exercise
    GET    /api/progress/weekly/          — weekly workout volume summary
    GET    /api/progress/dashboard/       — key metrics (streaks, PRs, totals)
"""

from datetime import timedelta

from django.contrib.auth.models import User
from django.db.models import Sum, Max, Count, F
from django.utils import timezone
from rest_framework import generics, permissions, status, viewsets
from rest_framework.authtoken.models import Token
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response

from .models import TrainingPlan, Exercise, WorkoutSession, ExerciseLog
from .serializers import (
    RegisterSerializer,
    UserSerializer,
    TrainingPlanSerializer,
    ExerciseSerializer,
    WorkoutSessionSerializer,
    WorkoutSessionCreateSerializer,
    ExerciseLogSerializer,
)


# ═══════════════════════════════════════════════════════════════════
#  AUTH VIEWS
# ═══════════════════════════════════════════════════════════════════

class RegisterView(generics.CreateAPIView):
    """
    POST /api/auth/register/
    Create a new user and return an auth token.
    """
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
    """
    POST /api/auth/login/
    Accepts {"username": "...", "password": "..."} and returns a token.
    """
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
    """
    GET /api/auth/me/
    Returns the currently authenticated user's info.
    """
    return Response(UserSerializer(request.user).data)


# ═══════════════════════════════════════════════════════════════════
#  TRAINING PLAN CRUD
# ═══════════════════════════════════════════════════════════════════

class TrainingPlanViewSet(viewsets.ModelViewSet):
    """
    Full CRUD for training plans.
    Users can only see and edit their own plans.
    """
    serializer_class = TrainingPlanSerializer

    def get_queryset(self):
        return TrainingPlan.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        # automatically assign the logged-in user
        serializer.save(user=self.request.user)


# ═══════════════════════════════════════════════════════════════════
#  EXERCISE CRUD
# ═══════════════════════════════════════════════════════════════════

class ExerciseViewSet(viewsets.ModelViewSet):
    """
    Full CRUD for exercises.
    Optionally filter by training plan: GET /api/exercises/?plan=1
    Users can only access exercises in their own plans.
    """
    serializer_class = ExerciseSerializer

    def get_queryset(self):
        qs = Exercise.objects.filter(
            training_plan__user=self.request.user,
        )
        # optional filter by plan id
        plan_id = self.request.query_params.get("plan")
        if plan_id:
            qs = qs.filter(training_plan_id=plan_id)
        return qs


# ═══════════════════════════════════════════════════════════════════
#  WORKOUT SESSION CRUD
# ═══════════════════════════════════════════════════════════════════

class WorkoutSessionViewSet(viewsets.ModelViewSet):
    """
    Full CRUD for workout sessions.
    POST creates a session with nested exercise logs in one request.
    """

    def get_queryset(self):
        return WorkoutSession.objects.filter(user=self.request.user)

    def get_serializer_class(self):
        if self.action == "create":
            return WorkoutSessionCreateSerializer
        return WorkoutSessionSerializer

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


# ═══════════════════════════════════════════════════════════════════
#  EXERCISE LOG CRUD
# ═══════════════════════════════════════════════════════════════════

class ExerciseLogViewSet(viewsets.ModelViewSet):
    """
    CRUD for individual exercise logs.
    Optionally filter by session: GET /api/logs/?session=1
    Optionally filter by exercise name: GET /api/logs/?exercise_name=Bench+Press
    """
    serializer_class = ExerciseLogSerializer

    def get_queryset(self):
        qs = ExerciseLog.objects.filter(
            workout_session__user=self.request.user,
        )
        session_id = self.request.query_params.get("session")
        if session_id:
            qs = qs.filter(workout_session_id=session_id)
        exercise_name = self.request.query_params.get("exercise_name")
        if exercise_name:
            qs = qs.filter(exercise_name__icontains=exercise_name)
        return qs


# ═══════════════════════════════════════════════════════════════════
#  PROGRESS / CHART ENDPOINTS
# ═══════════════════════════════════════════════════════════════════

@api_view(["GET"])
def exercise_progress(request, exercise_id):
    """
    GET /api/progress/exercise/{exercise_id}/
    Returns the weight and reps history for a specific exercise,
    sorted by date. Used for the line chart on Android.

    Response:
    [
        {"date": "2025-04-01", "sets": 3, "reps": 10, "weight": "80.00"},
        {"date": "2025-04-03", "sets": 3, "reps": 10, "weight": "82.50"},
        ...
    ]
    """
    logs = (
        ExerciseLog.objects
        .filter(
            exercise_id=exercise_id,
            workout_session__user=request.user,
        )
        .select_related("workout_session")
        .order_by("workout_session__date")
    )

    data = [
        {
            "date": log.workout_session.date.isoformat(),
            "sets": log.sets,
            "reps": log.reps,
            "weight": str(log.weight),
        }
        for log in logs
    ]
    return Response(data)


@api_view(["GET"])
def weekly_volume(request):
    """
    GET /api/progress/weekly/
    Returns total workout volume (sets × reps × weight) per week
    for the last 12 weeks. Used for the bar chart on Android.

    Response:
    [
        {"week": "2025-W14", "total_volume": 12500.0, "session_count": 3},
        {"week": "2025-W15", "total_volume": 14200.0, "session_count": 4},
        ...
    ]
    """
    twelve_weeks_ago = timezone.now().date() - timedelta(weeks=12)

    logs = (
        ExerciseLog.objects
        .filter(
            workout_session__user=request.user,
            workout_session__date__gte=twelve_weeks_ago,
        )
        .select_related("workout_session")
    )

    # group by ISO week
    weekly = {}
    for log in logs:
        iso = log.workout_session.date.isocalendar()
        week_key = f"{iso[0]}-W{iso[1]:02d}"
        if week_key not in weekly:
            weekly[week_key] = {"total_volume": 0, "sessions": set()}
        volume = float(log.sets * log.reps * log.weight)
        weekly[week_key]["total_volume"] += volume
        weekly[week_key]["sessions"].add(log.workout_session_id)

    data = sorted(
        [
            {
                "week": week,
                "total_volume": round(info["total_volume"], 2),
                "session_count": len(info["sessions"]),
            }
            for week, info in weekly.items()
        ],
        key=lambda x: x["week"],
    )
    return Response(data)


@api_view(["GET"])
def dashboard(request):
    """
    GET /api/progress/dashboard/
    Returns key fitness metrics for the dashboard screen.

    Response:
    {
        "total_workouts": 42,
        "current_streak": 5,
        "personal_records": [
            {"exercise_name": "Bench Press", "max_weight": "100.00"},
            {"exercise_name": "Squat", "max_weight": "140.00"},
            ...
        ]
    }
    """
    user = request.user
    sessions = WorkoutSession.objects.filter(user=user, completed=True)

    # ── Total workouts ───────────────────────────────────────────
    total_workouts = sessions.count()

    # ── Current streak (consecutive days with a workout) ─────────
    dates = list(
        sessions
        .values_list("date", flat=True)
        .distinct()
        .order_by("-date")
    )
    streak = 0
    today = timezone.now().date()
    expected = today
    for d in dates:
        if d == expected:
            streak += 1
            expected -= timedelta(days=1)
        elif d == expected + timedelta(days=1):
            # today hasn't happened yet, skip
            continue
        else:
            break

    # ── Personal records (max weight per exercise) ───────────────
    prs = (
        ExerciseLog.objects
        .filter(workout_session__user=user)
        .values("exercise_name")
        .annotate(max_weight=Max("weight"))
        .order_by("-max_weight")
    )
    personal_records = [
        {"exercise_name": pr["exercise_name"], "max_weight": str(pr["max_weight"])}
        for pr in prs
    ]

    return Response({
        "total_workouts": total_workouts,
        "current_streak": streak,
        "personal_records": personal_records,
    })
