# CampusPrint: Viva Guide

**Project:** Enhancing Campus Printing Service Management using RESTful APIs and Role-Based Authentication in a Web and Mobile Platform (SDG 9, Target 9.5)
**Student:** Rohan Mishra, Roll 114, Div B

## 1. One-minute explanation
Students upload a document, choose a print option, and see the price. The print desk (admin) sees every job in a queue and moves it through Submitted, Printing, Ready, Collected. A React web app and a Flutter mobile app both talk to one Kotlin + Spring Boot REST API, which stores data in a database.

```
React (web)  --\
                 >--  HTTPS + JSON  -->  Spring Boot API (Kotlin)  -->  Database
Flutter (app) --/                        Spring Security + JWT          (H2 local / PostgreSQL production)
                                         File storage (uploads/)
```

## 2. Objectives mapped to code
| Objective | Where it is |
|---|---|
| RESTful APIs in Kotlin + Spring Boot | `backend/src/main/kotlin/com/campusprint/web/*Controller.kt` |
| Secure auth + role-based access | `security/Jwt.kt`, `config/SecurityConfig.kt` |
| React web interface | `web/src/pages/*`, `web/src/components/*` |
| Flutter mobile app | `mobile/lib/*` |
| Database for users, requests, services, history | `domain/Entities.kt`, `repository/Repositories.kt` |
| Deploy for real use | `backend/Dockerfile`, `web/vercel.json` |

## 3. Why each technology
- **React:** UI split into reusable components (Navbar, Ticket, StatusTrack). State with hooks (`useState`, `useEffect`). React Router gives separate pages without reloads.
- **Vite:** fast dev server and production build tool for React.
- **Kotlin:** concise, null-safe language, fully compatible with Java and Spring. Matches the Kotlin course.
- **Spring Boot:** auto-configures an embedded Tomcat server, JSON, validation, security and JPA. We write controllers, not setup.
- **Spring Data JPA / Hibernate:** maps Kotlin classes (`@Entity`) to tables and writes SQL from method names like `findByStudentIdOrderByIdDesc`.
- **PostgreSQL (production) / H2 (local):** data is relational (a user has many requests, each request has one service), so a SQL database with foreign keys fits. H2 needs no install for development.
- **Spring Security + JWT:** stateless login. The server signs a token with the user's email and role. Every request carries it in the `Authorization: Bearer` header and the server checks it. No session stored on the server, so the same API serves web and mobile.
- **BCrypt:** one-way, salted, slow hash. Passwords are never stored in plain text.
- **Flutter:** one Dart codebase for Android and iOS, calling the same API.
- **Vercel:** hosts the built React files on a CDN. It cannot run a JVM server, so the Spring Boot API runs on Render or Railway (Docker).
- **Maven:** builds the Kotlin project and downloads dependencies (`pom.xml`).

## 4. Key flows to be able to explain
**Login:** `POST /api/auth/login` -> `AuthService.login` checks the BCrypt hash -> `JwtService.generate` returns a token -> the frontend stores it and sends it on later calls. `GET /api/auth/me` restores the session on page refresh.

**Role-based access:** in `SecurityConfig`, `hasRole("ADMIN")` protects `GET /api/requests`, status changes, stats and price edits. `hasRole("STUDENT")` protects submitting a job. Wrong role gives 403, no token gives 401. The React `ProtectedRoute` only hides pages; the server is the real protection.

**Submit a job:** multipart form (file + serviceId + pages + copies + notes) -> `PrintRequestService.create` validates, saves the file under a random UUID name, **calculates the price on the server** (`pricePerPage x pages x copies`), saves the row with status PENDING.

**Status flow:** admin calls `PATCH /api/requests/{id}/status`. A student can cancel only their own job, and only while PENDING. Others' jobs return 404, so their existence isn't revealed.

## 5. REST API reference
| Method | URL | Who | Purpose |
|---|---|---|---|
| POST | /api/auth/register | public | create student account |
| POST | /api/auth/login | public | get token |
| GET | /api/auth/me | signed in | current user |
| GET | /api/services | public | price list |
| POST / PUT | /api/services, /api/services/{id} | admin | add / edit options |
| POST | /api/requests | student | submit job (multipart) |
| GET | /api/requests/mine | student | my jobs |
| GET | /api/requests | admin | all jobs |
| PATCH | /api/requests/{id}/status | admin | change status |
| PATCH | /api/requests/{id}/cancel | student | cancel own pending job |
| GET | /api/requests/{id}/file | owner or admin | download file |
| GET | /api/admin/stats | admin | dashboard numbers |

## 6. Likely questions and answers
- **What is REST?** An API style using HTTP methods (GET read, POST create, PUT update, PATCH partial update) on resource URLs, returning JSON.
- **What is a DTO and why use one?** A class that defines the JSON shape. It keeps fields like `passwordHash` out of responses.
- **What is JWT?** `header.payload.signature`. The signature proves the server made it and nobody edited it. It expires after 24 hours.
- **JWT vs session?** JWT is stateless, so no server memory, and it works for web and mobile equally.
- **Why is CSRF disabled?** We send the token in a header, not a cookie, so browsers don't attach it automatically.
- **What is CORS?** Browsers block a site on one origin from calling an API on another. `SecurityConfig` whitelists the Vercel URL.
- **What does `ddl-auto=update` do?** Hibernate creates or updates tables from the entity classes.
- **What does `@Transactional` do?** Runs the method in one database transaction: all changes save together or none do.
- **Why store files with a UUID name?** Prevents name collisions and path-traversal tricks. We also check the extension and block `../`.
- **Weaknesses you know of?** Uploaded files sit on the server disk, which is wiped on free hosts when redeployed. Production would use object storage (S3 or Supabase Storage). No email notifications or online payment yet (future work).
- **What is "demo mode" in the web app?** If `VITE_API_URL` is empty the site uses a browser-stored copy of the API so it can be shown without a server. With the variable set, it calls the real Kotlin API.

## 7. Testing evidence you can show
Browser test of the web app covered: price estimator, registration, PDF page auto-count, job submission, student blocked from `/admin`, admin changing status, admin adding a price option, student seeing "Ready", wrong-password error, and no horizontal scroll on a 390 px phone.
