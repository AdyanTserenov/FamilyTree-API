# Family Tree API — Детальный план реализации

> **Статус проекта:** В разработке  
> **Версия документа:** 1.0  
> **Дата:** 2026-02-25  
> **Стек:** Java 17 · Spring Boot 3.4.2 · PostgreSQL · JWT · Maven

---

## Содержание

1. [Обзор архитектуры](#1-обзор-архитектуры)
2. [Структура модулей](#2-структура-модулей)
3. [Доменная модель и база данных](#3-доменная-модель-и-база-данных)
4. [Модуль: family-tree-auth-starter](#4-модуль-family-tree-auth-starter)
5. [Модуль: auth-service](#5-модуль-auth-service)
6. [Модуль: tree-service](#6-модуль-tree-service)
7. [Полный справочник API](#7-полный-справочник-api)
8. [Безопасность и авторизация](#8-безопасность-и-авторизация)
9. [Управление медиафайлами](#9-управление-медиафайлами)
10. [Стратегия тестирования](#10-стратегия-тестирования)
11. [Выявленные пробелы и задачи](#11-выявленные-пробелы-и-задачи)
12. [Стратегия развёртывания](#12-стратегия-развёртывания)
13. [Дорожная карта](#13-дорожная-карта)

---

## 1. Обзор архитектуры

### 1.1 Концепция

Веб-сервис для **совместного создания и визуализации семейных деревьев**. Несколько пользователей могут работать с одним деревом одновременно, имея разные уровни доступа (OWNER / EDITOR / VIEWER). Каждый узел дерева — это персона с биографическими данными и медиафайлами; рёбра — это родственные связи (`PARENT_CHILD`, `PARTNERSHIP`).

### 1.2 Архитектурный стиль

```
┌─────────────────────────────────────────────────────────────────┐
│                    Клиент (Browser / Mobile)                    │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTPS / REST JSON
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                 Spring Boot Monolith (tree-service)              │
│                                                                  │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐   │
│  │  TreeController │  │ PersonController │  │MediaController│   │
│  └────────┬────────┘  └────────┬─────────┘  └──────┬────────┘   │
│           │                    │                    │            │
│  ┌────────▼────────────────────▼────────────────────▼─────────┐  │
│  │                        TreeService                          │  │
│  └────────────────────────────┬────────────────────────────────┘  │
│                               │                                  │
│  ┌────────────────────────────▼────────────────────────────────┐  │
│  │           family-tree-auth-starter (embedded lib)            │  │
│  │   UserService · TokenService · JwtUtils · SecurityConfig     │  │
│  └────────────────────────────┬────────────────────────────────┘  │
│                               │                                  │
│  ┌────────────────────────────▼────────────────────────────────┐  │
│  │                    PostgreSQL Database                       │  │
│  │  users · tokens · trees · tree_memberships · invitations    │  │
│  │  persons · relationships · media_files                      │  │
│  └─────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  auth-service (отдельный сервис)                 │
│  Регистрация · Вход · Верификация email · Сброс пароля           │
│  Использует тот же family-tree-auth-starter                      │
└──────────────────────────────────────────────────────────────────┘
```

### 1.3 Принципы проектирования

| Принцип | Реализация |
|---------|-----------|
| **Единая ответственность** | Каждый сервис отвечает за одну предметную область |
| **Переиспользование** | `family-tree-auth-starter` — общая библиотека для auth-логики |
| **Безопасность по умолчанию** | JWT-фильтр на всех эндпоинтах, RBAC на уровне сервиса |
| **Fail-fast** | Валидация входных данных через Bean Validation (`@Valid`) |
| **Тестируемость** | Сервисы не зависят от HTTP-контекста, легко мокируются |
| **Идемпотентность** | Уникальные ограничения в БД предотвращают дублирование связей |

---

## 2. Структура модулей

```
FamilyTree-API/
├── pom.xml                              # Parent POM (агрегатор)
│
├── family-tree-auth-starter/            # Переиспользуемая auth-библиотека
│   └── src/main/java/.../auth/
│       ├── AuthAutoConfiguration.java   # Spring Boot Auto-configuration
│       ├── config/                      # SecurityConfig, SwaggerConfig, PasswordEncoder, Scheduler
│       ├── controllers/                 # SecurityController, ProfileController
│       ├── dto/                         # SignUpRequest, SignInRequest, UserDTO, ...
│       ├── exceptions/                  # GlobalExceptionHandler + кастомные исключения
│       ├── impls/                       # TokenType, TokenDetails
│       ├── models/                      # User, Token, ResetToken, VerifyToken
│       ├── repositories/                # UserRepository, TokenRepository
│       ├── security/                    # JwtUtils, TokenFilter
│       └── services/                    # UserService, TokenService, MailSenderService
│
├── auth-service/                        # Standalone сервис аутентификации
│   └── src/main/java/.../
│       ├── controllers/                 # SecurityController, ProfileController, TreeController
│       ├── models/                      # Tree, TreeMembership, Invitation
│       ├── repositories/                # TreeRepository, TreeMembershipRepository, InvitationRepository
│       └── services/                    # TreeService (управление деревьями в auth-service)
│
└── tree-service/                        # Основной сервис семейных деревьев
    └── src/main/java/.../tree/
        ├── controllers/                 # TreeController, PersonController, MediaFileController
        ├── dto/                         # TreeDTO, PersonDTO, RelationshipDTO, MediaFileDTO, ...
        ├── impls/                       # Gender, RelationshipType, MediaFileType, TreeRole
        ├── models/                      # Tree, Person, Relationship, MediaFile, TreeMembership, Invitation
        ├── repositories/                # все JPA-репозитории
        └── services/                    # TreeService, MediaFileService
```

### 2.1 Зависимости между модулями

```
auth-service  ──────────────────────────────────────────────────────┐
                                                                     │
tree-service  ──────────────────────────────────────────────────────┤
                                                                     ▼
                                               family-tree-auth-starter
                                               (UserService, JwtUtils,
                                                SecurityConfig, MailSender)
```

> **Важно:** `tree-service` напрямую использует `UserService` и `MailSenderService` из стартера. Оба сервиса разделяют одну БД (таблица `users`).

---

## 3. Доменная модель и база данных

### 3.1 ER-диаграмма

```
users (1) ──────────────── (N) tree_memberships (N) ──────────── (1) trees
  │                                                                    │
  │ (1)                                                           (1)  │
  │                                                                    │
  └──── (N) tokens                                    (N) ────────────┘
            (VERIFY/RESET)                         persons
                                                       │
                                              (N) relationships
                                              (PARENT_CHILD / PARTNERSHIP)
                                                       │
                                              (N) media_files ──── (1) users (uploaded_by)
```

### 3.2 Таблицы и их назначение

| Таблица | Назначение | Ключевые ограничения |
|---------|-----------|----------------------|
| `users` | Зарегистрированные пользователи | `email` UNIQUE, `enabled` DEFAULT FALSE |
| `tokens` | Токены верификации и сброса пароля | `token_hash` UNIQUE, `type` IN ('VERIFY','RESET') |
| `trees` | Семейные деревья | — |
| `tree_memberships` | Участие пользователей в деревьях | UNIQUE(tree_id, user_id), `role` IN ('OWNER','EDITOR','VIEWER') |
| `invitations` | Приглашения по email или ссылке | `token` UNIQUE, `accepted` DEFAULT FALSE |
| `persons` | Персоны в дереве | `gender` IN ('MALE','FEMALE','OTHER') |
| `relationships` | Связи между персонами | UNIQUE(tree_id, person1_id, person2_id, type) |
| `media_files` | Медиафайлы персон | `file_type` IN ('PHOTO','DOCUMENT','VIDEO','AUDIO') |

### 3.3 Семантика связей

```
PARENT_CHILD:
  person1 = РОДИТЕЛЬ
  person2 = РЕБЁНОК
  Направленная связь. Для одного ребёнка может быть несколько записей (мать + отец).
  Пример: (Пётр, Иван, PARENT_CHILD) → Пётр является отцом Ивана

PARTNERSHIP:
  person1 и person2 = ПАРТНЁРЫ (супруги, сожители)
  Симметричная связь. При поиске проверяются оба порядка (p1,p2) и (p2,p1).
  Пример: (Пётр, Мария, PARTNERSHIP) → Пётр и Мария — партнёры
```

### 3.4 Индексы производительности

```sql
-- Уже созданы в init.sql:
idx_tokens_expires_at        -- для планировщика очистки токенов
idx_persons_tree_id          -- для выборки всех персон дерева
idx_relationships_tree_id    -- для выборки всех связей дерева
idx_relationships_person1    -- для поиска связей по person1
idx_relationships_person2    -- для поиска связей по person2
idx_media_files_tree_id      -- для выборки медиа дерева
idx_media_files_person_id    -- для выборки медиа персоны
```

---

## 4. Модуль: family-tree-auth-starter

### 4.1 Назначение

Spring Boot Auto-configuration библиотека, которая предоставляет готовую инфраструктуру аутентификации для любого сервиса в экосистеме Family Tree.

### 4.2 Что предоставляет стартер

#### Модели
- `User` — сущность пользователя (`id`, `firstName`, `lastName`, `middleName`, `email`, `password`, `enabled`)
- `Token` — унифицированный токен верификации/сброса (`type`, `token_hash`, `expires_at`, `consumed`)
- `ResetToken` — токен сброса пароля
- `VerifyToken` — токен верификации email

#### Сервисы
- `UserService` — CRUD пользователей, поиск по email/id/UserDetails, `findIdByDetails()`
- `TokenService` — создание, валидация, потребление токенов
- `MailSenderService` — отправка email через SMTP (`sendEmail(to, subject, text)`)
- `EmailTemplateService` — шаблоны писем (верификация, сброс пароля)

#### Безопасность
- `JwtUtils` — генерация и валидация JWT (HMAC-SHA256)
- `TokenFilter` — Spring Security `OncePerRequestFilter` для JWT

#### Конфигурация
- `SecurityConfig` — настройка Spring Security (публичные пути, JWT-фильтр)
- `SwaggerConfig` — OpenAPI 3 документация с поддержкой Bearer Auth
- `TokenCleanUpScheduler` — `@Scheduled` задача очистки истёкших токенов

### 4.3 Конфигурационные свойства

```properties
# JWT
family.auth.jwt.secret=<минимум 32 символа для HMAC-SHA256>
family.auth.jwt.lifetime=3600000   # миллисекунды (1 час по умолчанию)

# SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@email.com
spring.mail.password=app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 5. Модуль: auth-service

### 5.1 Назначение

Standalone HTTP-сервис для управления учётными записями пользователей. Использует `family-tree-auth-starter` как зависимость.

### 5.2 Эндпоинты auth-service

#### SecurityController — `/api/auth`

| Метод | Путь | Описание | Тело запроса |
|-------|------|----------|-------------|
| `POST` | `/signup` | Регистрация нового пользователя | `SignUpRequest` |
| `POST` | `/signin` | Вход, получение JWT | `SignInRequest` |
| `GET` | `/verify?token=` | Верификация email по токену | — |
| `POST` | `/forgot-password` | Запрос письма сброса пароля | `{email}` |
| `POST` | `/reset-password` | Установка нового пароля | `PasswordResetRequest` |

#### ProfileController — `/api/profile`

| Метод | Путь | Описание | Auth |
|-------|------|----------|------|
| `GET` | `/` | Получить профиль текущего пользователя | JWT |
| `PUT` | `/` | Обновить профиль | JWT |

#### TreeController (в auth-service) — `/api/trees`

| Метод | Путь | Описание | Auth |
|-------|------|----------|------|
| `GET` | `/` | Список деревьев пользователя | JWT |
| `POST` | `/` | Создать дерево | JWT |
| `POST` | `/{id}/invite` | Пригласить по email | JWT + OWNER |

### 5.3 DTO auth-service

```java
SignUpRequest {
    String firstName;    // @NotBlank
    String lastName;     // @NotBlank
    String middleName;   // nullable
    String email;        // @Email @NotBlank
    String password;     // @Size(min=8)
}

SignInRequest {
    String email;
    String password;
}

ProfileResponse {
    Long id;
    String firstName;
    String lastName;
    String middleName;
    String email;
    Instant createdAt;
}

PasswordResetRequest {
    String token;
    String newPassword;
}
```

---

## 6. Модуль: tree-service

### 6.1 Назначение

Основной сервис приложения. Управляет семейными деревьями, персонами, связями и медиафайлами. Использует `family-tree-auth-starter` для аутентификации.

### 6.2 Слои приложения

```
HTTP Request
    │
    ▼
Controller
  - Валидация входных данных (@Valid)
  - Извлечение userId из SecurityContext
  - Делегирование в Service
    │
    ▼
Service
  - Бизнес-логика
  - Проверка прав доступа (canView / canEdit / isOwner)
  - @Transactional для операций записи
    │
    ▼
Repository (Spring Data JPA)
  - Стандартные CRUD-методы
  - Кастомные @Query для сложных выборок
    │
    ▼
PostgreSQL Database
```

### 6.3 Модели tree-service

#### Tree
```
id          BIGSERIAL PK
name        VARCHAR(255) NOT NULL
created_at  TIMESTAMP WITH TIME ZONE
```

#### TreeMembership
```
id          BIGSERIAL PK
tree_id     FK → trees(id) CASCADE
user_id     FK → users(id) CASCADE
role        VARCHAR(20) CHECK IN ('OWNER','EDITOR','VIEWER')
created_at  TIMESTAMP WITH TIME ZONE
UNIQUE(tree_id, user_id)
```

#### Person
```
id          BIGSERIAL PK
tree_id     FK → trees(id) CASCADE
first_name  VARCHAR(100) NOT NULL
last_name   VARCHAR(100) NOT NULL
middle_name VARCHAR(100)
gender      VARCHAR(10) CHECK IN ('MALE','FEMALE','OTHER')
birth_date  DATE
death_date  DATE
birth_place VARCHAR(255)
death_place VARCHAR(255)
biography   TEXT
created_at  TIMESTAMP WITH TIME ZONE
updated_at  TIMESTAMP WITH TIME ZONE
```

#### Relationship
```
id          BIGSERIAL PK
tree_id     FK → trees(id) CASCADE
person1_id  FK → persons(id) CASCADE   -- родитель / первый партнёр
person2_id  FK → persons(id) CASCADE   -- ребёнок / второй партнёр
type        VARCHAR(20) CHECK IN ('PARENT_CHILD','PARTNERSHIP')
created_at  TIMESTAMP WITH TIME ZONE
UNIQUE(tree_id, person1_id, person2_id, type)
```

#### MediaFile
```
id          BIGSERIAL PK
person_id   FK → persons(id) CASCADE   -- nullable
tree_id     FK → trees(id) CASCADE
file_name   VARCHAR(255) NOT NULL      -- оригинальное имя
file_path   VARCHAR(512) NOT NULL      -- путь на диске (UUID + расширение)
file_type   VARCHAR(20) CHECK IN ('PHOTO','DOCUMENT','VIDEO','AUDIO')
file_size   BIGINT NOT NULL            -- байты
description TEXT
uploaded_at TIMESTAMP WITH TIME ZONE
uploaded_by FK → users(id)
```

#### Invitation
```
id          BIGSERIAL PK
token       VARCHAR(255) UNIQUE        -- UUID
tree_id     FK → trees(id) CASCADE
email       VARCHAR(255) NOT NULL
role        VARCHAR(20) CHECK IN ('OWNER','EDITOR','VIEWER')
created_at  TIMESTAMP WITH TIME ZONE
expires_at  TIMESTAMP WITH TIME ZONE  -- +7 дней от создания
accepted    BOOLEAN DEFAULT FALSE
```

### 6.4 Перечисления

```java
// Иерархия прав (ordinal: VIEWER=0, EDITOR=1, OWNER=2)
// hasPermission(required): this.ordinal() >= required.ordinal()
enum TreeRole { VIEWER, EDITOR, OWNER }

// Типы родственных связей
enum RelationshipType { PARENT_CHILD, PARTNERSHIP }

// Пол персоны
enum Gender { MALE, FEMALE, OTHER }

// Типы медиафайлов
enum MediaFileType { PHOTO, DOCUMENT, VIDEO, AUDIO }
```

### 6.5 TreeService — бизнес-логика

#### Управление деревьями

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `createTree(name, ownerId)` | Создаёт дерево + запись OWNER в memberships | Любой авторизованный |
| `getById(treeId)` | Получить дерево по ID (или RuntimeException) | — |
| `getUserTrees(userId)` | Список деревьев пользователя | — |
| `getMembers(treeId)` | Список участников дерева | VIEWER |
| `addMember(treeId, userId, role)` | Добавить участника | OWNER |
| `hasRole(treeId, userId, required)` | Проверка роли | — |
| `canView(treeId, userId)` | VIEWER или OWNER | — |
| `canEdit(treeId, userId)` | EDITOR или OWNER | — |
| `isOwner(treeId, userId)` | Только OWNER | — |

#### Приглашения

| Метод | Описание |
|-------|----------|
| `createInviteToken(treeId, email, role, inviterId)` | Создаёт UUID-токен, сохраняет в БД, возвращает токен |
| `sendInviteByEmail(treeId, email, role, inviterId)` | Создаёт токен + отправляет email со ссылкой |
| `acceptInvitation(token, userId)` | Проверяет токен, email, срок → добавляет участника |

#### Управление персонами

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `createPerson(treeId, request, userId)` | Создать персону | EDITOR |
| `getPersons(treeId, userId)` | Список персон (сортировка по lastName, firstName) | VIEWER |
| `getPerson(treeId, personId, userId)` | Персона с её связями | VIEWER |
| `updatePerson(treeId, personId, request, userId)` | Обновить все поля | EDITOR |
| `deletePerson(treeId, personId, userId)` | Удалить персону + связи + медиафайлы | EDITOR |

#### Управление связями

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `addRelationship(treeId, request, userId)` | Добавить связь (с проверкой дублей) | EDITOR |
| `removeRelationship(treeId, request, userId)` | Удалить связь | EDITOR |
| `getTreeGraph(treeId, userId)` | Все персоны со всеми связями для графа | VIEWER |

### 6.6 MediaFileService — управление файлами

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `uploadFile(treeId, personId, file, type, desc, userId)` | Сохранить файл на диск + запись в БД | EDITOR |
| `getPersonMedia(treeId, personId, userId)` | Список файлов персоны | VIEWER |
| `getTreeMedia(treeId, userId)` | Все файлы дерева | VIEWER |
| `downloadFile(treeId, personId, fileId, userId)` | Скачать файл (Resource) | VIEWER |
| `deleteFile(treeId, personId, fileId, userId)` | Удалить файл с диска + из БД | EDITOR |

---

## 7. Полный справочник API

### 7.1 Базовый URL

```
tree-service:  http://localhost:8080
auth-service:  http://localhost:8081  (если запущен отдельно)
Swagger UI:    http://localhost:8080/swagger-ui.html
OpenAPI JSON:  http://localhost:8080/v3/api-docs
```

### 7.2 Аутентификация

Все эндпоинты (кроме `/api/auth/**`) требуют заголовок:
```
Authorization: Bearer <JWT-токен>
```

### 7.3 Формат ответа (`CustomApiResponse<T>`)

```json
// Успех с данными
{
  "success": true,
  "message": null,
  "data": { ... }
}

// Успех с сообщением
{
  "success": true,
  "message": "Дерево создано",
  "data": null
}

// Ошибка
{
  "success": false,
  "message": "Описание ошибки",
  "data": null
}
```

### 7.4 TreeController — `/trees`

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `GET` | `/trees` | Список деревьев текущего пользователя | Любой |
| `POST` | `/trees` | Создать дерево | Любой |
| `POST` | `/trees/{treeId}/invite` | Пригласить по email | OWNER |
| `POST` | `/trees/{treeId}/invite-link` | Получить ссылку-приглашение | OWNER |
| `GET` | `/trees/invite/{token}` | Принять приглашение | Любой |
| `GET` | `/trees/{treeId}/members` | Список участников | VIEWER+ |

**POST /trees** — тело запроса:
```json
{ "name": "Семья Ивановых" }
```

**POST /trees/{treeId}/invite** — тело запроса:
```json
{
  "email": "user@example.com",
  "role": "EDITOR"
}
```

**POST /trees/{treeId}/invite-link** — ответ:
```json
{
  "success": true,
  "data": {
    "inviteLink": "https://familytree.example.com/invite/550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**GET /trees** — ответ:
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "Семья Ивановых", "createdAt": "2026-01-15T10:00:00Z" },
    { "id": 2, "name": "Семья Петровых", "createdAt": "2026-02-01T08:30:00Z" }
  ]
}
```

### 7.5 PersonController — `/trees/{treeId}/persons`

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `GET` | `/trees/{treeId}/persons` | Список персон (сортировка по фамилии) | VIEWER+ |
| `POST` | `/trees/{treeId}/persons` | Создать персону | EDITOR+ |
| `GET` | `/trees/{treeId}/persons/{personId}` | Получить персону со связями | VIEWER+ |
| `PUT` | `/trees/{treeId}/persons/{personId}` | Обновить персону | EDITOR+ |
| `DELETE` | `/trees/{treeId}/persons/{personId}` | Удалить персону | EDITOR+ |
| `GET` | `/trees/{treeId}/persons/graph` | Граф дерева (все персоны + связи) | VIEWER+ |
| `POST` | `/trees/{treeId}/persons/relationships` | Добавить связь | EDITOR+ |
| `DELETE` | `/trees/{treeId}/persons/relationships` | Удалить связь | EDITOR+ |

**POST /trees/{treeId}/persons** — тело запроса:
```json
{
  "firstName": "Иван",
  "lastName": "Иванов",
  "middleName": "Иванович",
  "birthDate": "1990-01-15",
  "deathDate": null,
  "birthPlace": "Москва",
  "deathPlace": null,
  "biography": "Текст биографии...",
  "gender": "MALE"
}
```

**GET /trees/{treeId}/persons/{personId}** — ответ:
```json
{
  "success": true,
  "data": {
    "id": 42,
    "treeId": 1,
    "firstName": "Иван",
    "lastName": "Иванов",
    "middleName": "Иванович",
    "birthDate": "1990-01-15",
    "deathDate": null,
    "birthPlace": "Москва",
    "deathPlace": null,
    "biography": "...",
    "gender": "MALE",
    "fullName": "Иванов Иван Иванович",
    "relationships": [
      {
        "id": 10,
        "person1Id": 41,
        "person2Id": 42,
        "type": "PARENT_CHILD"
      }
    ]
  }
}
```

**POST /trees/{treeId}/persons/relationships** — тело запроса:
```json
{
  "person1Id": 41,
  "person2Id": 42,
  "type": "PARENT_CHILD"
}
```

**GET /trees/{treeId}/persons/graph** — ответ (массив PersonDTO со всеми связями):
```json
{
  "success": true,
  "data": [
    {
      "id": 41,
      "fullName": "Иванов Пётр Сергеевич",
      "gender": "MALE",
      "relationships": [
        { "id": 10, "person1Id": 41, "person2Id": 42, "type": "PARENT_CHILD" },
        { "id": 11, "person1Id": 40, "person2Id": 41, "type": "PARTNERSHIP" }
      ]
    },
    {
      "id": 42,
      "fullName": "Иванов Иван Петрович",
      "gender": "MALE",
      "relationships": [
        { "id": 10, "person1Id":