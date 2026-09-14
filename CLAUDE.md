# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`edu10-restapp-pro` (Spring Boot 4.1.1, Java 21, package `gr.aueb.cf.eduapp`) is a REST API for managing a registry of teachers, with JWT-based authentication and fine-grained, capability-based authorization. Persistence is MySQL via Spring Data JPA, with Flyway managing schema migrations.

## Commands

Build tool is Gradle (wrapper included). On Windows use `gradlew.bat`; `gradlew` (bash) also works via the Bash tool.

```
gradlew.bat build              # compile + run tests
gradlew.bat test                # run all tests
gradlew.bat test --tests gr.aueb.cf.eduapp.EduAppApplicationTests   # run a single test class
gradlew.bat bootRun             # run the app (requires DB + .env, see below)
```

There is currently only one (empty) test, `EduAppApplicationTests#contextLoads`, so `bootRun`/manual API exercising (e.g. via the Swagger UI) is the main way to verify behavior end to end.

### Running locally

- Requires a MySQL instance. Connection and JWT secrets are supplied via a `.env` file at the project root (see `.env.example` for the required keys: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB`, `MYSQL_USER`, `MYSQL_PASSWORD`, `JWT_SECRET_KEY`). `.env` is loaded by `application-dev.properties` via `spring.config.import`.
- Active profile is `dev` (set in `application.properties`). `spring.jpa.hibernate.ddl-auto=validate` — schema changes must go through Flyway migrations (`src/main/resources/db/migration/V*__*.sql`), not Hibernate auto-DDL.
- CORS allowed origin is `http://localhost:5174` (a separate frontend, not in this repo).
- Uploaded files are written under `file.upload.dir` (`uploads/`); logs go to `logs/eduapp.log`.
- Swagger UI / OpenAPI docs are exposed at `/swagger-ui/**` and `/v3/api-docs/**` and are permitted without authentication.

## Architecture

### Layering

`api` (REST controllers) → `service` (business logic, `@Transactional`) → `repository` (Spring Data JPA) → `model` (JPA entities). DTOs (`dto` package, Java records) cross the controller/service boundary; `mapper.Mapper` converts between entities and DTOs by hand (no MapStruct/ModelMapper). `ITeacherService` is the service interface implemented by `TeacherService`; controllers depend on the interface.

### Authentication & authorization (the trickiest part of this codebase)

Two layers work together and both must be kept in sync when adding endpoints:

1. **URL-level rules** in `security/SecurityConfiguration.java` (`authorizeHttpRequests`) — coarse per-route/method rules (e.g. `POST /api/v1/teachers` is `permitAll`, `GET /api/v1/teachers` requires `VIEW_TEACHERS`).
2. **Method-level rules** via `@PreAuthorize` on service methods in `TeacherService` (method security is enabled via `@EnableMethodSecurity`). This is where ownership checks happen, e.g.:
   ```java
   @PreAuthorize("hasAuthority('VIEW_TEACHER') or (hasAuthority('VIEW_ONLY_TEACHER') and @securityService.isOwnTeacherProfile(#uuid, authentication))")
   ```
   `security/SecurityService.java` (bean name `securityService`) implements these custom ownership checks (e.g. "does this authenticated user own this teacher profile?").

Authorization model: `User` → `Role` (ADMIN / EMPLOYEE / TEACHER) → many `Capability` (e.g. `VIEW_TEACHERS`, `EDIT_TEACHER`, `DELETE_TEACHER`, `VIEW_ONLY_TEACHER`). `User.getAuthorities()` emits both `ROLE_<name>` and each capability name as a `GrantedAuthority`, so `@PreAuthorize`/`hasAuthority` checks reference capability names directly (not role names). Seed data for roles/capabilities/assignments lives in `V3__insert_roles_capabilites.sql`. When adding a new permission, add the capability, assign it to the right role(s) via migration, and use it in both `SecurityConfiguration` and any needed `@PreAuthorize`.

JWT: `JwtAuthenticationFilter` reads the bearer token, `JwtService` issues/validates tokens (HS256, secret from `app.security.secret-key`, expiry from `app.security.jwt-expiration`), `CustomUserDetailsService` loads the `User` (which itself implements `UserDetails`). `CustomAuthenticationEntryPoint` / `CustomAccessDeniedHandler` produce the 401/403 JSON bodies for unauthenticated/unauthorized requests respectively; unhandled auth exceptions are also normalized centrally in `ErrorHandler`.

### Entities

`AbstractEntity` (mapped superclass) gives every entity `createdAt`/`updatedAt` (via `AuditingEntityListener`) plus a `deleted`/`deletedAt` pair for **soft deletes** — deletion (`TeacherService.deleteTeacherByUUID`) calls `softDelete()` on the `Teacher`, its `PersonalInfo`, and its `User` rather than removing rows. Because of this, most read queries have both a plain variant and a `...DeletedFalse` variant (e.g. `getTeacherByUUID` vs `getTeacherByUUIDDeletedFalse`, `findByUuid` vs `findByUuidAndDeletedFalse`) — pick the right one depending on whether soft-deleted rows should be visible.

Core entity graph: `Teacher` 1:1 `User` (owning side is `Teacher.user`), `Teacher` 1:1 `PersonalInfo` (cascade ALL + orphan removal — personal info's lifecycle is owned by the teacher), `Teacher` N:1 `Region`, `User` N:1 `Role`, `Role` N:N `Capability`. Bidirectional associations use paired add/remove helper methods (e.g. `Teacher.addUser`/`removeUser`, `Region.addTeacher`, `Role.addCapability`) to keep both sides in sync — always use these instead of setting one side directly. `PersonalInfo` holds an optional `Attachment` (the AMKA file) added/removed via `addAmkaFile`/`removeAmkaFile`.

All entity IDs exposed externally are `UUID` (`BINARY(16)` columns), not the internal auto-increment `Long id`.

### File uploads

`TeacherService.saveAmkaFile` detects content type with Apache Tika, and — because the DB write and the filesystem write cannot be atomic — defers the actual file write (and deletion of any previous file) to `TransactionSynchronizationManager.registerSynchronization(...).afterCommit()`, so the file is only written once the DB transaction has committed. Follow this pattern for any future file-writing service methods that must stay consistent with a DB transaction. The method is also `@Retryable` on `IOException`/`HttpServerErrorException`.

### Error handling

All exceptions are centralized in `core/ErrorHandler.java` (`@RestControllerAdvice`), mapping the custom exceptions in `core/exceptions/` to HTTP status codes and a uniform `ErrorResponseDTO`/`ValidationErrorResponseDTO` body (`code` + `message`, plus a field-error map for validation failures). Custom checked exceptions (`EntityNotFoundException`, `EntityAlreadyExistsException`, `EntityInvalidArgumentException`, `ValidationException`, `FileUploadException`) carry an application-specific `code` string separate from the HTTP status — controllers/services declare these as `throws` rather than catching them.

### Filtering & pagination

List endpoints (`GET /api/v1/teachers`) take a `Pageable` plus a `TeacherFilters` (`core/filters`) bound via `@ModelAttribute`. `TeacherService.getTeachersPaginatedFiltered` special-cases unique-key filters (`uuid`, `amka`, `vat`) to do a direct single-result lookup instead of going through `TeacherSpecification` (JPA Specifications used for the general filtered/paginated case).
