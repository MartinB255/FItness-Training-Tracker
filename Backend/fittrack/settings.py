"""
Django settings for the FitTrack project.

Key configuration:
    - PostgreSQL as the database (via psycopg2)
    - Django REST Framework with token authentication
    - CORS headers enabled for Android app communication
"""

from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent


# ── Security ─────────────────────────────────────────────────────
# IMPORTANT: change this in production and keep it secret!
SECRET_KEY = "django-insecure-CHANGE-ME-before-deployment"

DEBUG = True

# Allow connections from Android emulator and local network
ALLOWED_HOSTS = ["*"]


# ── Installed apps ───────────────────────────────────────────────
INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    # Third-party
    "rest_framework",
    "rest_framework.authtoken",
    "corsheaders",
    # Local
    "api",
]


# ── Middleware ────────────────────────────────────────────────────
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "corsheaders.middleware.CorsMiddleware",  # must be before CommonMiddleware
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]


# ── CORS (allow Android app to call the API) ────────────────────
CORS_ALLOW_ALL_ORIGINS = True  # fine for development


# ── URL config ───────────────────────────────────────────────────
ROOT_URLCONF = "fittrack.urls"


# ── Templates ────────────────────────────────────────────────────
TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]


# ── Database (PostgreSQL) ────────────────────────────────────────
# Make sure PostgreSQL is running and the database exists.
# Create it with:  createdb fittrack
# Or in psql:      CREATE DATABASE fittrack;
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "fittrack",
        "USER": "postgres",
        "PASSWORD": "postgres",    # <-- change to your actual password
        "HOST": "localhost",
        "PORT": "5432",
    }
}


# ── REST Framework ───────────────────────────────────────────────
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.TokenAuthentication",
        # Session auth is handy for the browsable API during development
        "rest_framework.authentication.SessionAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
}


# ── Password validation ─────────────────────────────────────────
AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]


# ── Internationalization ─────────────────────────────────────────
LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True


# ── Static files ─────────────────────────────────────────────────
STATIC_URL = "static/"


# ── Auto field ───────────────────────────────────────────────────
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"


WSGI_APPLICATION = "fittrack.wsgi.application"