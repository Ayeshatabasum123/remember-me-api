# Remember Me API — Backend REST API

Spring Boot REST API for the Remember Me System (based on the project report).

## Tech Stack
- Java 17 + Spring Boot 3.2.5
- PostgreSQL + PostGIS (for GPS coordinates)
- Spring Data JPA / Hibernate
- Spring Security + JWT
- Lombok
- Swagger / OpenAPI (springdoc)
- AWS S3 (media storage)
- Firebase Cloud Messaging (notifications)

## Folder Structure
```
src/main/java/com/rememberme/api/
 ├── config/          → SecurityConfig, SwaggerConfig
 ├── entity/           → JPA entities (User, RememberMe, Grave, DeceasedPerson, Relationship,
 │                        Memorial, Photo, Favorite, Report, AuditLog)
 ├── repository/       → Spring Data JPA repositories
 ├── dto/
 │    ├── request/     → Request DTOs
 │    └── response/    → Response DTOs
 ├── controller/       → REST controllers (one per API module)
 ├── service/          → Business logic (AuthService done; others use repos directly for now)
 ├── security/         → JwtUtil, JwtAuthFilter, CustomUserDetailsService
 └── exception/        → ApiException, GlobalExceptionHandler, ErrorResponse
src/main/resources/
 └── application.yml   → DB, JWT, AWS, Firebase, mail config
```

## API Modules Implemented (matches report section 6 & the API table)

| Module | Endpoint base | Controller |
|---|---|---|
| Authentication | `/api/auth` | AuthController |
| User Profile | `/api/users` | UserController |
| Remember Me | `/api/graveyards` | RememberMeController |
| Grave | `/api/graves` | GraveController |
| Deceased Person | `/api/deceased` | DeceasedPersonController |
| Relationship | `/api/relationships` | RelationshipController |
| Memorial | `/api/memorials` | MemorialController |
| Media Upload | `/api/media` | MediaController |
| Search | `/api/search` | SearchController |
| Favourite/Saved Graves | `/api/favourites` | FavouriteController |
| Report | `/api/reports` | ReportController |
| Admin | `/api/admin` | AdminController |
| Notifications | `/api/notifications` | NotificationController |

## Setup

1. **Create the database**
   ```sql
   CREATE DATABASE graveyard_mapping_db;
   CREATE EXTENSION postgis;
   ```

2. **Update `src/main/resources/application.yml`**
   - `spring.datasource.username` / `password`
   - `jwt.secret` (use a long random string)
   - AWS S3 keys (for media upload)
   - Firebase service account file (for notifications)
   - Mail credentials (for OTP/email verification)

3. **Run the project**
   ```bash
   mvn spring-boot:run
   ```

4. **Open Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

## What's already working
- User registration & login with JWT (`/api/auth/register`, `/api/auth/login`)
- JWT-secured endpoints (add `Authorization: Bearer <token>` header)
- Full CRUD skeleton for graveyards, graves, deceased persons, relationships, memorials, favourites, reports
- Admin approve/reject workflow for graveyards + reports
- Swagger docs auto-generated for every controller

## What still needs to be built out (TODOs left in code)
- `/api/auth`: forgot-password, reset-password, OTP verification, email verification
- `/api/media`: actual AWS S3 upload logic (currently returns a placeholder URL)
- `/api/notifications`: actual Firebase Cloud Messaging integration
- Move remaining controllers (Graveyard, Grave, Deceased, etc.) from direct-repository calls into a proper Service layer (only AuthService is done — same pattern you used in `marriage-match-api`)
- Duplicate-detection logic for Deceased Person records (report objective #5)
- Role-based method security fine-tuning (`@PreAuthorize`) per endpoint
- PostGIS-based "nearby graveyards" search using lat/long + radius
- Unit tests (`src/test/...` folder is scaffolded but empty)

## Notes
- `ddl-auto: update` is set for development — switch to Flyway/Liquibase migrations before production.
- Passwords are hashed with BCrypt.
- All entities use `LocalDateTime` timestamps — consider adding `@PrePersist`/`@PreUpdate` hooks to auto-set them (currently set manually in a couple of places).
