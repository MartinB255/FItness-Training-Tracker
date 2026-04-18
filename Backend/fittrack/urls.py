"""
Root URL configuration for the FitTrack project.

/admin/     — Django admin interface
/api/       — all REST API endpoints (see api/urls.py)
"""

from django.contrib import admin
from django.urls import include, path

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/", include("api.urls")),
]