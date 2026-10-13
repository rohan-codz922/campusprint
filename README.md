# CampusPrint

Folders: `web/` (React), `backend/` (Kotlin + Spring Boot), `mobile/` (Flutter, not yet updated for payments/OTP), `docs/`.

## Run locally

**Backend** (JDK 17 + Maven). Nothing secret is stored in any file. The script asks for an admin email/password and makes a random token secret:
```
cd backend
.\run-local.ps1
```
Verification and reset codes are printed in that window (no email service locally).

**Web** (Node 18+):
```
cd web
copy .env.example .env      # VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

## Required settings (Render environment variables)

| Name | What it is |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL (Neon). `DB_URL` looks like `jdbc:postgresql://HOST/neondb?sslmode=require` |
| `JWT_SECRET` | random, 32+ characters. The app refuses to start without it |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | the print desk login (password 8-72 chars). Change the password by editing this and restarting |
| `CORS_ORIGINS` | your website address, e.g. `https://campusprint-seven.vercel.app` (no trailing slash) |
| `BREVO_API_KEY`, `MAIL_FROM`, `MAIL_FROM_NAME` | email for verification and reset codes (an `xkeysib-` API key) |
| `PAYMENT_REQUIRED_PAGES` | optional, default 20: prints with this many pages (pages x copies) or more must be paid first |

Vercel needs one variable: `VITE_API_URL` = your Render address (no trailing slash).
The Render address is also written in `web/vercel.json` under `connect-src`. If the API address ever changes, update both.

See `docs/SECURITY.md` for what is protected and how.
