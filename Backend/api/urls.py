"""URL routing for the api app. All endpoints live under /api/."""

from django.urls import include, path
from rest_framework.routers import DefaultRouter

from . import views

router = DefaultRouter()
router.register(r"exercises", views.ExerciseViewSet, basename="exercise")
router.register(r"plans", views.TrainingPlanViewSet, basename="plan")
router.register(
    r"plan-exercises", views.PlanExerciseViewSet, basename="planexercise",
)
router.register(r"sessions", views.WorkoutSessionViewSet, basename="session")
router.register(
    r"exercise-logs", views.ExerciseLogViewSet, basename="exerciselog",
)

urlpatterns = [
    # Auth
    path("auth/register/", views.RegisterView.as_view(), name="register"),
    path("auth/login/", views.login_view, name="login"),
    path("auth/me/", views.me_view, name="me"),

    # CRUD (auto-generated)
    path("", include(router.urls)),

    # Charts / summary
    path("progress/", views.progress, name="progress"),
    path("weekly-volume/", views.weekly_volume, name="weekly-volume"),
    path("dashboard/", views.dashboard, name="dashboard"),
]
