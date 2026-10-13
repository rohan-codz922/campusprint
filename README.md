# CampusPrint

Folders: `web/` (React), `backend/` (Kotlin + Spring Boot), `mobile/` (Flutter), `docs/VIVA_GUIDE.md`.

## 1. Run the backend (needs JDK 17 and Maven, or open `backend/` in IntelliJ)
```
cd backend
mvn spring-boot:run
```
API on http://localhost:8080. First start creates an admin: `admin@campus.edu` / `admin123` and 4 print options. Data is in `backend/data/` (H2).

## 2. Run the web app (needs Node 18+)
```
cd web
npm install
cp .env.example .env      # set VITE_API_URL=http://localhost:8080  (leave empty for demo mode)
npm run dev
```

## 3. Run the mobile app (needs Flutter 3.22+)
```
cd mobile
flutter create .          # generates android/ and ios/ (keeps lib/ and pubspec.yaml)
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080
```
Android only: in `android/app/src/main/AndroidManifest.xml` add `<uses-permission android:name="android.permission.INTERNET"/>` before `<application>`, and `android:usesCleartextTraffic="true"` inside `<application ...>` for local http testing.

## 4. Deploy
**Database:** create a free PostgreSQL (Supabase or Neon). Note the JDBC URL, user, password.

**Backend on Render:** New > Web Service > connect the repo, Root Directory `backend`, Runtime Docker. Environment variables:
```
DB_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require
DB_USER=...   DB_PASSWORD=...
JWT_SECRET=<random 40+ characters>
ADMIN_EMAIL=...   ADMIN_PASSWORD=<strong password>
CORS_ORIGINS=https://YOUR-APP.vercel.app
```
Health check path: `/api/services`. The free tier sleeps; the first request can take about a minute.

**Web on Vercel:** import the repo, Root Directory `web`, framework Vite. Add env var `VITE_API_URL=https://YOUR-API.onrender.com`. Or from `web/`: `npm i -g vercel && vercel --prod`.

**Mobile:** `flutter build apk --dart-define=API_URL=https://YOUR-API.onrender.com`

## Known limits
Uploaded files are stored on the server disk (lost on redeploy on free hosts). Use object storage for production.
