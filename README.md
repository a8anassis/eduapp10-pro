# edu10-restapp-pro

A REST API for managing a registry of teachers, built with Spring Boot. It provides JWT-based authentication and capability-based authorization, teacher CRUD with soft deletes, filtered/paginated search, and AMKA document upload.

## Tech stack

- Java 21, Spring Boot 4.1.1
- Spring Data JPA + MySQL (Flyway for schema migrations)
- Spring Security (stateless JWT authentication, method-level `@PreAuthorize` authorization)
- springdoc-openapi (Swagger UI)
- Apache Tika (file content-type detection)
- Gradle (wrapper included)

## Prerequisites

- JDK 21
- A running MySQL instance

## Setup

1. Copy `.env.example` to `.env` and fill in the values:

   ```
   MYSQL_HOST=
   MYSQL_PORT=
   MYSQL_DB=
   MYSQL_USER=
   MYSQL_PASSWORD=
   JWT_SECRET_KEY=
   ```

   `JWT_SECRET_KEY` must be a Base64-encoded key suitable for HS256 signing.

2. Run the app (Flyway migrations under `src/main/resources/db/migration` run automatically on startup):

   ```
   gradlew.bat bootRun
   ```

   The active profile is `dev` (`application-dev.properties`), which loads `.env` via `spring.config.import`.

## Common commands

```
gradlew.bat build              # compile and run tests
gradlew.bat test               # run tests
gradlew.bat bootRun            # run the app
```

## API

Once running, interactive API docs are available at:

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

Key endpoints (all under `/api/v1`):

| Method | Path                        | Notes                                             |
|--------|-----------------------------|----------------------------------------------------|
| POST   | `/auth/authenticate`        | Authenticate with username/password, get a JWT     |
| POST   | `/teachers`                 | Register a new teacher                             |
| POST   | `/teachers/{uuid}/amka-file`| Upload/replace a teacher's AMKA document            |
| GET    | `/teachers`                 | Paginated, filterable teacher list (auth required)  |
| GET    | `/teachers/{uuid}`          | Get a teacher by UUID (auth required)               |
| PUT    | `/teachers/{uuid}`          | Update a teacher (auth required)                    |
| DELETE | `/teachers/{uuid}`          | Soft-delete a teacher (auth required)                |

Authenticated requests use `Authorization: Bearer <token>`. Access to individual endpoints is further restricted by capability (e.g. `VIEW_TEACHERS`, `EDIT_TEACHER`, `DELETE_TEACHER`), granted per role (`ADMIN`, `EMPLOYEE`, `TEACHER`) — see `CLAUDE.md` for details on the authorization model.

## Configuration reference

| Property                          | Purpose                                              |
|------------------------------------|-------------------------------------------------------|
| `file.upload.dir`                  | Directory where uploaded files (e.g. AMKA docs) are stored |
| `app.security.secret-key`          | JWT signing key (from `JWT_SECRET_KEY`)               |
| `app.security.jwt-expiration`      | JWT expiration in milliseconds                        |
| `allowed.origins`                  | CORS allowed origin(s) for the frontend                |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | Upload size limits              |

Logs are written to `logs/eduapp.log`.

## Architecture

See [CLAUDE.md](./CLAUDE.md) for a deeper dive into the layering, authorization model, entity relationships, and other architectural notes.
