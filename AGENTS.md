# AGENTS.md — FamilyTree API

Permanent context document for sub-agents working on the FamilyTree backend.

---

## 1. Project Overview

**FamilyTree** is a ВКР (graduation project) at НИУ ВШЭ — a genealogy web application.  
This repository (`FamilyTree-API`) contains the backend: two Spring Boot microservices plus a shared auth starter.

---

## 2. Repository Structure

```
FamilyTree-API/
├── auth-service/          # Authentication microservice (port 8081)
├── tree-service/          # Core business logic microservice (port 8080)
├── family-tree-auth-starter/  # Shared Spring Boot starter (JWT filter, UserService, SecurityConfig)
├── docker-compose.yml     # Runs both services + depends on external PostgreSQL
├── CI-CD.md               # CI/CD setup guide
└── README.md              # Full API documentation
```

### auth-service (port 8081)
- Handles: registration, login, email verification, password reset, profile management
- Base path: `/auth/**` (no `/api` prefix on the service itself; nginx adds `/api/auth` prefix)
- Key classes: `SecurityController`, `ProfileController`, `UserService`, `TokenService`, `MailSenderService`
- JWT generation: `JwtCore`

### tree-service (port 8080)
- Handles: trees, persons, relationships, media files, comments, notifications, AI analysis, public links, invitations
- Base path: `/api/**`
- Key classes: `TreeController`, `PersonController`, `MediaFileController`, `CommentController`, `NotificationController`, `AiController`
- Key services: `TreeService` (main business logic), `MediaFileService` (S3), `AiService` (YandexGPT), `NotificationService`

### family-tree-auth-starter
- Shared library used by tree-service for JWT validation
- Contains: `JwtUtils`, `TokenFilter` (OncePerRequestFilter), `SecurityConfig`, `UserService` (loads user from DB)
- Published to local Maven repo; tree-service depends on it

---

## 3. Infrastructure

### Yandex Cloud VM
- **IP**: `158.160.55.234`
- **SSH**: `ssh atserenov@158.160.55.234`
- **Project directory on VM**: `~/vkr/FamilyTree-API/`

### PostgreSQL — Yandex Managed PostgreSQL (EXTERNAL, NOT Docker)
- **Master node** (READ + WRITE): `rc1a-728tmdljsqu1cq9u.mdb.yandexcloud.net:6432`
- **Replica node** (READ-ONLY — **DO NOT USE FOR WRITES**): `rc1a-158d8jc0e2kd4q08.mdb.yandexcloud.net:6432`
- **Database**: `familytree`
- **User**: `familytree`
- ⚠️ **CRITICAL**: Always use the master node URL in `SPRING_DATASOURCE_URL`. Using the replica causes HTTP 500 on all write operations.

### Yandex Object Storage (S3-compatible)
- Used for: person avatars, media files (photos, documents, audio, video)
- Endpoint: `https://storage.yandexcloud.net`
- Bucket: configured via `S3_BUCKET_NAME` env var
- ⚠️ CORS must be configured on the bucket for avatar URLs to load in browser

### Docker Compose
- `familytree-auth` container → port 8081
- `familytree-tree` container → port 8080
- Both containers share the `familytree-net` Docker network
- PostgreSQL is NOT a Docker container — it's the external managed service

### nginx (in Frontend container)
- Proxies `/api/auth/**` → `familytree-auth:8081` (strips `/api` prefix → `/auth/**`)
- Proxies `/api/**` → `familytree-tree:8080`
- All frontend API calls go through port 3000 via nginx

### CI/CD
- GitHub Actions workflow: `.github/workflows/deploy.yml`
- On push to `main`: SSH into VM → `git pull && docker compose build && docker compose up -d`
- Secrets: `VM_HOST`, `VM_USER`, `VM_SSH_KEY`

---

## 4. Local Development

### Prerequisites
- Java 17+, Maven 3.8+
- Docker + Docker Compose
- Access to Yandex Managed PostgreSQL (or local PostgreSQL)

### Build & Run Locally

```bash
# 1. Build the shared auth starter first
cd family-tree-auth-starter
mvn install -DskipTests

# 2. Build auth-service
cd ../auth-service
mvn package -DskipTests

# 3. Build tree-service
cd ../tree-service
mvn package -DskipTests

# 4. Run via Docker Compose (requires .env file)
cd ..
docker compose up --build
```

### Environment Variables (`.env` file in repo root)

```env
# Database (Yandex Managed PostgreSQL — use MASTER node)
SPRING_DATASOURCE_URL=jdbc:postgresql://rc1a-728tmdljsqu1cq9u.mdb.yandexcloud.net:6432/familytree?ssl=true&sslmode=require
SPRING_DATASOURCE_USERNAME=familytree
SPRING_DATASOURCE_PASSWORD=<secret>

# JWT (must match between auth-service and tree-service)
JWT_SECRET=<secret>
JWT_EXPIRATION=86400000

# Mail (SMTP)
MAIL_HOST=smtp.yandex.ru
MAIL_PORT=465
MAIL_USERNAME=<email>
MAIL_PASSWORD=<secret>
MAIL_FROM=<email>

# App URLs
APP_URL=http://158.160.55.234:3000
AUTH_SERVICE_URL=http://familytree-auth:8081

# AI (tree-service only)
YANDEX_GPT_API_KEY=<secret>
YANDEX_GPT_FOLDER_ID=<secret>

# S3 / Yandex Object Storage (tree-service only)
S3_ACCESS_KEY=<secret>
S3_SECRET_KEY=<secret>
S3_BUCKET_NAME=familytree-media
S3_ENDPOINT=https://storage.yandexcloud.net
S3_REGION=ru-central1
```

---

## 5. Backend Architecture

### Authentication Flow
1. Client sends credentials to `POST /api/auth/sign-in` (via nginx → auth-service)
2. auth-service validates, returns JWT
3. Client includes JWT in `Authorization: Bearer <token>` header
4. tree-service validates JWT via `TokenFilter` from the shared starter
5. `TokenFilter` loads user from DB by email extracted from JWT

### Role Model (per tree)
- `OWNER` — full control, can delete tree, manage members
- `EDITOR` — can add/edit persons, relationships, comments, media
- `VIEWER` — read-only access

### Key Business Rules
- `birthDate` must be before `deathDate` (validated in `TreeService.createPerson()` and `updatePerson()`)
- Media files: max 10 per person, enforced server-side in `MediaFileService`
- AI rate limit: 5 requests per hour per user (in-memory `ConcurrentHashMap` in `AiService` — resets on restart)
- Invitation tokens expire; accepted invitations cannot be reused (idempotency check in `acceptInvitation()`)
- Public tree links use UUID tokens stored in DB

### Exception Handling
- `GlobalExceptionHandler` in tree-service handles: `AccessDeniedException` (403), `ResourceNotFoundException` (404), `BusinessException` (400), `MethodArgumentNotValidException` (400), `MaxUploadSizeExceededException` (413), generic `Exception` (500)
- auth-service has its own `GlobalExceptionHandler`

### Notifications
- Created automatically on: tree invitation accepted, new comment on person, new member joined tree
- Stored in DB, fetched via `GET /api/notifications`
- Mark as read: `PUT /api/notifications/{id}/read` or `PUT /api/notifications/read-all`

---

## 6. Data Model (Key Tables)

| Table | Description |
|-------|-------------|
| `users` | User accounts (id, email, password_hash, first_name, last_name, enabled) |
| `trees` | Family trees (id, name, owner_id, public_token) |
| `tree_members` | Tree membership (tree_id, user_id, role: OWNER/EDITOR/VIEWER) |
| `persons` | People in trees (id, tree_id, first_name, last_name, birth_date, death_date, biography, avatar_url) |
| `relationships` | Person relationships (person_id, related_person_id, type: PARENT/CHILD/PARTNER/SIBLING) |
| `media_files` | Uploaded files (id, person_id, tree_id, s3_key, file_name, content_type, type: PHOTO/DOCUMENT/AUDIO/VIDEO) |
| `comments` | Person comments (id, person_id, tree_id, author_id, content, created_at) |
| `notifications` | User notifications (id, user_id, type, message, read, created_at) |
| `tokens` | Email verification and password reset tokens |
| `person_history` | Audit log of person field changes |
| `invite_tokens` | Tree invitation tokens (token, tree_id, email, role, inviter_id, used) |

---

## 7. Deployment Commands (on VM)

```bash
# SSH into VM
ssh atserenov@158.160.55.234

# Navigate to API repo
cd ~/vkr/FamilyTree-API

# Pull latest changes
git pull

# Rebuild and restart containers
docker compose build && docker compose up -d

# View logs
docker compose logs -f familytree-auth
docker compose logs -f familytree-tree

# Check container status
docker compose ps

# Restart a single service
docker compose restart familytree-tree
```

---

## 8. Important Gotchas

1. **PostgreSQL master vs replica**: ALWAYS use `rc1a-728tmdljsqu1cq9u` (master) in `SPRING_DATASOURCE_URL`. The replica `rc1a-158d8jc0e2kd4q08` is read-only and causes HTTP 500 on all write operations.

2. **Auth-service URL path**: auth-service itself has no `/api` prefix. Its endpoints are `/auth/sign-in`, `/auth/sign-up`, etc. nginx adds the `/api` prefix. When calling auth-service directly (not via nginx), use port 8081 without `/api`.

3. **JWT secret must match**: `JWT_SECRET` env var must be identical in both auth-service and tree-service (tree-service uses the starter to validate tokens generated by auth-service).

4. **AI rate limit is in-memory**: `AiService` uses a `ConcurrentHashMap` for rate limiting. It resets on container restart. Not suitable for multi-instance deployment.

5. **S3 CORS**: Avatar URLs are direct S3 URLs. If CORS is not configured on the S3 bucket, browser canvas operations (PNG export) will be tainted and fail.

6. **Email verification**: New users have `enabled=false` until they verify email. Disabled users cannot log in (returns 401). For test users, set `enabled=true` directly in DB.

7. **Build order**: `family-tree-auth-starter` must be built and installed to local Maven repo BEFORE building tree-service.

8. **Swagger UI**: Available at `http://158.160.55.234:8080/swagger-ui/index.html` (tree-service only).

