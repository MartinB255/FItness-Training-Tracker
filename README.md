# FitTrack

A fitness tracking app for Android. Create and manage training plans, log workout sessions, and monitor your progress through charts and statistics.

The backend is a REST API built with Django. The Android app communicates with it over your local network.

## Features

- Register and log in with a personal account
- Build training plans with exercises, sets, reps, and weights
- Log workout sessions and track completion of each exercise
- View progress charts — weight over time per exercise and weekly training volume
- Dashboard with streak, personal records, and recent activity

## Tech Stack

- **Android** — Kotlin, Retrofit (HTTP client), MPAndroidChart
- **Backend** — Python, Django REST Framework
- **Database** — PostgreSQL

---

## Getting Started

### 1. Backend

**Requirements:** Python 3.11+, PostgreSQL

Install dependencies:

```bash
cd Backend
pip install django djangorestframework djangorestframework-authtoken django-cors-headers psycopg2-binary
```

Set up the database — create a PostgreSQL database named `fittrack`. The default connection settings (host, user, password) are in `Backend/fittrack/settings.py` and can be changed there.

Apply migrations and start the server:

```bash
python manage.py migrate
python manage.py runserver 0.0.0.0:8000
```

The API will be available at `http://localhost:8000/api/`.

### 2. Android App

**Requirements:** Android Studio, a device or emulator running Android 8.0+

1. Open the `Android/` folder in Android Studio and wait for Gradle sync to complete.
2. In `RetrofitClient.kt`, set `BASE_URL` to your machine's local IP address so the app can reach the backend:
   ```kotlin
   private const val BASE_URL = "http://192.168.x.x:8000/api/"
   ```
   > Use your LAN IP, not `localhost` — the emulator and physical devices cannot reach `localhost` on your machine.
3. Run the app on your device or emulator.

---

## Project Structure

```
FItness-Training-Tracker/
├── Android/
│   └── app/src/main/java/com/fittrack/app/
│       ├── data/
│       │   ├── api/            # Retrofit client and API interface
│       │   ├── model/          # Data models (Exercise, Plan, Session, etc.)
│       │   └── repository/     # Single data access layer for all API calls
│       ├── ui/
│       │   ├── auth/           # Login screen
│       │   ├── dashboard/      # Home screen with stats and quick actions
│       │   ├── exercises/      # Exercise catalog management
│       │   ├── plans/          # Training plan list and builder
│       │   ├── session/        # Starting, logging, and reviewing sessions
│       │   └── progress/       # Progress charts
│       └── util/               # Shared helpers (formatting, storage, timer)
└── Backend/
    ├── api/
    │   ├── models.py           # Exercise, TrainingPlan, WorkoutSession, ExerciseLog
    │   ├── serializers.py      # DRF serializers
    │   ├── views.py            # REST endpoints and analytics views
    │   └── urls.py             # API route definitions
    └── fittrack/
        ├── settings.py         # Django configuration
        └── urls.py             # Root URL config
```
