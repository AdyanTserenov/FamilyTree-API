# FamilyTree API

> **Стек:** Java 17 · Spring Boot 3.4 · PostgreSQL · JWT · Maven · Docker  
> **Статус:** Production-ready  
> **Деплой:** Yandex Cloud VM, два Docker-контейнера + внешний PostgreSQL-кластер

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
9. [AI-анализ биографий (YandexGPT)](#9-ai-анализ-биографий-yandexgpt)
10. [Управление медиафайлами](#10-управление-медиафайлами)
11. [Уведомления](#11-уведомления)
12. [Тестирование](#12-тестирование)
13. [Запуск локально](#13-запуск-локально)
14. [Переменные окружения](#14-переменные-окружения)

---

## 1. Обзор архитектуры

### 1.1 Концепция

Веб-сервис для **совместного создания и визуализации семейных деревьев**. Несколько пользователей могут работать с одним деревом одновременно, имея разные уровни доступа (OWNER / EDITOR / VIEWER). Каждый узел дерева — это персона с биографическими данными и медиафайлами; рёбра — это родственные связи (`PARENT_CHILD`, `PARTNERSHIP`).

### 1.2 Архитектурный стиль

```
┌─────────────────────────────────────────────────────────────────┐
│                    Клиент (Browser)                             │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTPS / REST JSON
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌──────────────────────┐         ┌──────────────────────┐
│    auth-service      │         │    tree-service       │
│    (порт 8081)       │         │    (порт 8080)        │
│                      │         │                       │
│  Регистрация         │         │  Деревья, персоны     │
│  Вход / JWT          │         │  Связи, медиафайлы    │
│  Профиль             │         │  Комментарии          │
│  Сброс пароля        │         │  Уведомления          │
│  Email-верификация   │         │  AI-анализ            │
└──────────┬───────────┘         └──────────┬────────────┘
           │                               │
           └──────────────┬────────────────┘
                          ▼
           ┌──────────────────────────────┐
           │  family-tree-auth-starter    │
           │  (embedded library)          │
           │  SecurityConfig · JwtUtils   │
           │  TokenFilter · UserService   │
           └──────────────┬───────────────┘
                          │
                          ▼
           ┌──────────────────────────────┐
           │  Yandex Managed PostgreSQL   │
           │  DB: familytree              │
           │  Schema: public              │
           │  (общая для обоих сервисов)  │
           └──────────────────────────────┘
```

### 1.3 Ключевые архитектурные решения

| Решение | Обоснование |
|---------|-------------|
| **Два сервиса, одна БД** | auth-service и tree-service используют одну PostgreSQL БД (`familytree`). Это упрощает управление пользователями — tree-service читает таблицу `users` напрямую без HTTP-вызовов к auth-service |
| **Нет HTTP-коммуникации между сервисами** | JWT-токен валидируется локально в каждом сервисе через `family-tree-auth-starter`. Нет синхронных зависимостей между сервисами |
| **family-tree-auth-starter** | Spring Boot Auto-configuration библиотека с общей auth-логикой. Подключается как Maven-зависимость в tree-service |
| **Stateless JWT** | `TokenFilter` проверяет подпись JWT криптографически, без обращения к БД. Секрет должен совпадать в обоих сервисах |
| **Yandex Object Storage (S3)** | Медиафайлы хранятся в S3-совместимом хранилище Yandex Cloud, не на диске VM |

---

## 2. Структура модулей

```
FamilyTree-API/
├── pom.xml                              # Parent POM (агрегатор)
│
├── family-tree-auth-starter/            # Переиспользуемая auth-библиотека
│   └── src/main/java/.../auth/
│       ├── AuthAutoConfiguration.java   # Spring Boot Auto-configuration entry point
│       ├── config/
│       │   ├── SecurityConfig.java      # Spring Security + JWT filter chain
│       │   └── SwaggerConfig.java       # OpenAPI 3 с Bearer Auth
│       ├── security/
│       │   ├── JwtUtils.java            # Генерация и валидация JWT (HMAC-SHA256)
│       │   └── TokenFilter.java         # OncePerRequestFilter для JWT
│       ├── services/
│       │   ├── UserService.java         # CRUD пользователей, UserDetailsService
│       │   ├── TokenService.java        # Токены верификации/сброса
│       │   └── MailSenderService.java   # SMTP email
│       ├── models/
│       │   ├── User.java                # @Entity пользователя
│       │   ├── Token.java               # @Entity базового токена
│       │   ├── VerifyToken.java         # Токен верификации email
│       │   └── ResetToken.java          # Токен сброса пароля
│       └── dto/
│           ├── SignUpRequest.java
│           ├── SignInRequest.java
│           └── UserDTO.java
│
├── auth-service/                        # Сервис аутентификации (порт 8081)
│   └── src/main/java/.../
│       ├── controllers/
│       │   ├── SecurityController.java  # /api/auth/** (sign-up, sign-in, verify, reset)
│       │   └── ProfileController.java   # /api/profile/** (get, update, change-password)
│       ├── services/
│       │   └── UserService.java         # Расширяет логику стартера
│       └── configurators/
│           ├── SecurityConfig.java      # Переопределяет SecurityConfig стартера
│           └── CorsConfig.java          # CORS настройки
│
└── tree-service/                        # Основной сервис (порт 8080)
    └── src/main/java/.../tree/
        ├── controllers/
        │   ├── TreeController.java      # /api/trees/**
        │   ├── PersonController.java    # /api/trees/{id}/persons/**
        │   ├── MediaFileController.java # /api/trees/{id}/persons/{id}/media/**
        │   ├── NotificationController.java # /api/notifications/**
        │   ├── CommentController.java   # /api/trees/{id}/persons/{id}/comments/**
        │   └── AiController.java        # /api/ai/extract-facts
        ├── services/
        │   ├── TreeService.java         # Основная бизнес-логика
        │   ├── MediaFileService.java    # Загрузка/скачивание файлов (S3)
        │   ├── NotificationService.java # Создание и управление уведомлениями
        │   ├── CommentService.java      # Комментарии к персонам
        │   └── AiService.java           # YandexGPT Completion API
        ├── models/
        │   ├── Tree.java, Person.java, Relationship.java
        │   ├── TreeMembership.java, Invitation.java
        │   ├── MediaFile.java, Comment.java
        │   ├── Notification.java, PersonHistory.java
        │   └── PublicTreeToken.java
        └── dto/                         # DTO для всех сущностей
```

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
                                              (N) media_files
                                              (N) comments
                                              (N) person_history
```

### 3.2 Таблицы

| Таблица | Назначение | Ключевые ограничения |
|---------|-----------|----------------------|
| `users` | Зарегистрированные пользователи | `email` UNIQUE, `enabled` DEFAULT FALSE |
| `tokens` | Токены верификации и сброса пароля | `token_hash` UNIQUE, `type` IN ('VERIFY','RESET') |
| `trees` | Семейные деревья | — |
| `tree_memberships` | Участие пользователей в деревьях | UNIQUE(tree_id, user_id), `role` IN ('OWNER','EDITOR','VIEWER') |
| `invitations` | Приглашения по email или ссылке | `token` UNIQUE, `accepted` DEFAULT FALSE |
| `public_tree_tokens` | Токены публичного доступа к дереву | UNIQUE(tree_id) |
| `persons` | Персоны в дереве | `gender` IN ('MALE','FEMALE','OTHER') |
| `relationships` | Связи между персонами | UNIQUE(tree_id, person1_id, person2_id, type) |
| `media_files` | Медиафайлы персон (S3) | `file_type` IN ('PHOTO','DOCUMENT','VIDEO','AUDIO') |
| `comments` | Комментарии к персонам | — |
| `person_history` | История изменений полей персоны | — |
| `notifications` | Уведомления пользователей | `read` DEFAULT FALSE |

### 3.3 Семантика связей

```
PARENT_CHILD:
  person1 = РОДИТЕЛЬ
  person2 = РЕБЁНОК
  Направленная связь. Для одного ребёнка может быть несколько записей (мать + отец).

PARTNERSHIP:
  person1 и person2 = ПАРТНЁРЫ (супруги, сожители)
  Симметричная связь. При поиске проверяются оба порядка (p1,p2) и (p2,p1).
```

### 3.4 Перечисления

```java
enum TreeRole        { VIEWER, EDITOR, OWNER }      // ordinal: 0, 1, 2
enum RelationshipType { PARENT_CHILD, PARTNERSHIP }
enum Gender          { MALE, FEMALE, OTHER }
enum MediaFileType   { PHOTO, DOCUMENT, VIDEO, AUDIO }
```

---

## 4. Модуль: family-tree-auth-starter

### 4.1 Назначение

Spring Boot Auto-configuration библиотека. Подключается через `pom.xml` в tree-service как Maven-зависимость. Предоставляет готовую инфраструктуру аутентификации.

> **Важно:** auth-service НЕ использует стартер как зависимость — он дублирует часть кода inline. Это известная архитектурная особенность проекта.

### 4.2 Что предоставляет стартер

#### JwtUtils

```java
// Генерация JWT при входе
String generateJwtToken(Authentication authentication)

// Извлечение email из токена
String getUserNameFromJwtToken(String token)

// Валидация подписи и срока действия
boolean validateJwtToken(String authToken)
```

#### TokenFilter (OncePerRequestFilter)

Выполняется для каждого запроса:
1. Извлекает токен из заголовка `Authorization: Bearer <token>`
2. Вызывает `jwtUtils.validateJwtToken()` — **без обращения к БД**
3. Загружает `UserDetails` из БД по email из токена
4. Устанавливает `SecurityContextHolder`

#### SecurityConfig

Настраивает Spring Security:
- Публичные пути: `/api/auth/**`, `/api/trees/public/**`, `/api/trees/invite/**`
- Все остальные пути требуют аутентификации
- Stateless сессии (JWT)
- CORS из переменной `CORS_ALLOWED_ORIGINS`

### 4.3 Конфигурационные свойства

```properties
# JWT (обязательно совпадает в auth-service и tree-service)
family.auth.jwt.secret=<минимум 32 символа для HMAC-SHA256>
family.auth.jwt.lifetime=3600000   # миллисекунды (1 час)

# SMTP
spring.mail.host=smtp.yandex.ru
spring.mail.port=587
spring.mail.username=your@yandex.ru
spring.mail.password=app-password
```

---

## 5. Модуль: auth-service

### 5.1 Назначение

Standalone HTTP-сервис (порт 8081) для управления учётными записями пользователей.

### 5.2 Эндпоинты

#### SecurityController — `POST/GET /api/auth`

| Метод | Путь | Описание | Тело запроса |
|-------|------|----------|-------------|
| `POST` | `/api/auth/sign-up` | Регистрация | `{ firstName, lastName, middleName?, email, password }` |
| `POST` | `/api/auth/sign-in` | Вход, получение JWT | `{ email, password }` |
| `GET` | `/api/auth/confirm?token=` | Верификация email | — |
| `POST` | `/api/auth/forgot` | Запрос письма сброса | `{ email }` |
| `POST` | `/api/auth/reset` | Установка нового пароля | `{ token, newPassword }` |
| `POST` | `/api/auth/resend-verification` | Повторная отправка письма | `{ email }` |
| `GET` | `/api/auth/ping` | Health check | — |

#### ProfileController — `/api/profile` (требует JWT)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/profile` | Получить профиль текущего пользователя |
| `PATCH` | `/api/profile` | Обновить имя/фамилию/отчество |
| `POST` | `/api/profile/change-password` | Сменить пароль |
| `DELETE` | `/api/profile` | Удалить аккаунт |

### 5.3 Формат ответов

**Успешный вход:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "firstName": "Иван",
      "lastName": "Иванов",
      "emailVerified": true
    }
  }
}
```

**Ошибка:**
```json
{
  "success": false,
  "message": "Неверный email или пароль",
  "data": null
}
```

---

## 6. Модуль: tree-service

### 6.1 Назначение

Основной сервис приложения (порт 8080). Управляет семейными деревьями, персонами, связями, медиафайлами, комментариями, уведомлениями и AI-анализом.

### 6.2 Слои приложения

```
HTTP Request
    │
    ▼
Controller (@RestController)
  - @Valid валидация входных данных
  - Извлечение userId из SecurityContext через UserService.findIdByDetails()
  - Делегирование в Service
    │
    ▼
Service (@Service, @Transactional)
  - Бизнес-логика
  - Проверка прав доступа (canView / canEdit / isOwner)
  - Запись истории изменений (PersonHistory)
  - Отправка уведомлений (NotificationService)
    │
    ▼
Repository (Spring Data JPA)
  - Стандартные CRUD-методы
  - Кастомные @Query для сложных выборок
    │
    ▼
Yandex Managed PostgreSQL
```

### 6.3 TreeService — бизнес-логика

#### Управление деревьями

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `createTree(name, ownerId)` | Создаёт дерево + запись OWNER в memberships | Любой авторизованный |
| `getUserTrees(userId)` | Список деревьев пользователя | — |
| `getMembers(treeId)` | Список участников дерева | VIEWER |
| `hasRole(treeId, userId, required)` | Проверка роли (ordinal-сравнение) | — |

#### Приглашения

| Метод | Описание |
|-------|----------|
| `createInviteToken(treeId, email, role, inviterId)` | UUID-токен, сохраняется в БД, возвращается клиенту |
| `sendInviteByEmail(treeId, email, role, inviterId)` | Токен + email со ссылкой |
| `acceptInvitation(token, userId)` | Проверяет токен, email, срок → добавляет участника |

#### Публичные ссылки

| Метод | Описание |
|-------|----------|
| `generatePublicLink(treeId, userId)` | Создаёт `PublicTreeToken`, возвращает URL |
| `revokePublicLink(treeId, userId)` | Удаляет токен публичного доступа |
| `getPublicTree(token)` | Возвращает PersonDTO[] без аутентификации |

#### Управление персонами

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `createPerson(treeId, request, userId)` | Создать персону | EDITOR |
| `getPersons(treeId, userId)` | Список персон (сортировка по lastName, firstName) | VIEWER |
| `getPerson(treeId, personId, userId)` | Персона с её связями | VIEWER |
| `updatePerson(treeId, personId, request, userId)` | Обновить поля + записать историю | EDITOR |
| `deletePerson(treeId, personId, userId)` | Удалить персону + связи + медиафайлы | EDITOR |
| `getTreeGraph(treeId, userId)` | Все персоны со всеми связями для графа | VIEWER |
| `searchPersons(treeId, query, userId)` | Поиск по имени/фамилии | VIEWER |

#### Управление связями

| Метод | Описание | Минимальная роль |
|-------|----------|-----------------|
| `addRelationship(treeId, request, userId)` | Добавить связь (с проверкой дублей) | EDITOR |
| `removeRelationship(treeId, request, userId)` | Удалить связь | EDITOR |

---

## 7. Полный справочник API

### 7.1 Базовые URL

```
tree-service:  http://localhost:8080
auth-service:  http://localhost:8081
Swagger UI:    http://localhost:8080/swagger-ui.html
OpenAPI JSON:  http://localhost:8080/v3/api-docs
```

### 7.2 Аутентификация

Все эндпоинты (кроме `/api/auth/**`, `/api/trees/public/**`, `/api/trees/invite/**`) требуют:
```
Authorization: Bearer <JWT-токен>
```

### 7.3 Формат ответа

```json
{
  "success": true | false,
  "message": "Описание или null",
  "data": { ... } | null
}
```

### 7.4 TreeController — `/api/trees`

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `GET` | `/api/trees` | Список деревьев пользователя | Любой |
| `POST` | `/api/trees` | Создать дерево `{ name }` | Любой |
| `PUT` | `/api/trees/{treeId}` | Переименовать дерево | OWNER |
| `DELETE` | `/api/trees/{treeId}` | Удалить дерево | OWNER |
| `POST` | `/api/trees/{treeId}/invite` | Пригласить по email `{ email, role }` | OWNER |
| `POST` | `/api/trees/{treeId}/invite-link` | Получить ссылку-приглашение `{ email, role }` | OWNER |
| `GET` | `/api/trees/invite/{token}` | Принять приглашение | Любой |
| `GET` | `/api/trees/{treeId}/members` | Список участников | VIEWER+ |
| `POST` | `/api/trees/{treeId}/public-link` | Создать публичную ссылку | OWNER |
| `DELETE` | `/api/trees/{treeId}/public-link` | Отозвать публичную ссылку | OWNER |
| `GET` | `/api/trees/public/{token}` | Публичный просмотр дерева | Без auth |

### 7.5 PersonController — `/api/trees/{treeId}/persons`

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `GET` | `/persons` | Список персон | VIEWER+ |
| `POST` | `/persons` | Создать персону | EDITOR+ |
| `GET` | `/persons/{personId}` | Получить персону со связями | VIEWER+ |
| `PUT` | `/persons/{personId}` | Обновить персону | EDITOR+ |
| `DELETE` | `/persons/{personId}` | Удалить персону | EDITOR+ |
| `GET` | `/persons/graph` | Граф (все персоны + связи) | VIEWER+ |
| `GET` | `/persons/search?q=` | Поиск по имени | VIEWER+ |
| `POST` | `/persons/relationships` | Добавить связь | EDITOR+ |
| `DELETE` | `/persons/relationships` | Удалить связь | EDITOR+ |
| `GET` | `/persons/{personId}/history` | История изменений | VIEWER+ |
| `POST` | `/persons/{personId}/avatar` | Загрузить аватар (multipart) | EDITOR+ |

**Тело запроса для создания/обновления персоны:**
```json
{
  "firstName": "Иван",
  "lastName": "Иванов",
  "middleName": "Иванович",
  "gender": "MALE",
  "birthDate": "1990-01-15",
  "deathDate": null,
  "birthPlace": "Москва",
  "deathPlace": null,
  "biography": "Текст биографии..."
}
```

**Тело запроса для связи:**
```json
{
  "person1Id": 41,
  "person2Id": 42,
  "type": "PARENT_CHILD"
}
```

**Ответ PersonDTO:**
```json
{
  "id": 42,
  "treeId": 1,
  "firstName": "Иван",
  "lastName": "Иванов",
  "middleName": "Иванович",
  "fullName": "Иванов Иван Иванович",
  "gender": "MALE",
  "birthDate": "1990-01-15",
  "deathDate": null,
  "birthPlace": "Москва",
  "deathPlace": null,
  "biography": "...",
  "avatarUrl": "https://storage.yandexcloud.net/...",
  "relationships": [
    { "id": 10, "person1Id": 41, "person2Id": 42, "type": "PARENT_CHILD" }
  ]
}
```

### 7.6 MediaFileController — `/api/trees/{treeId}/persons/{personId}/media`

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `POST` | `/media` | Загрузить файл (multipart: `file`, `type`, `description?`) | EDITOR+ |
| `GET` | `/media` | Список файлов персоны | VIEWER+ |
| `GET` | `/media/{fileId}/download` | Скачать файл | VIEWER+ |
| `DELETE` | `/media/{fileId}` | Удалить файл | EDITOR+ |

Допустимые типы файлов: `PHOTO`, `DOCUMENT`, `VIDEO`, `AUDIO`  
Максимальный размер: 50 МБ на файл, 50 файлов на персону

### 7.7 CommentController — `/api/trees/{treeId}/persons/{personId}/comments`

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `GET` | `/comments` | Список комментариев | VIEWER+ |
| `POST` | `/comments` | Добавить комментарий `{ content }` | VIEWER+ |
| `PUT` | `/comments/{commentId}` | Редактировать комментарий | Автор |
| `DELETE` | `/comments/{commentId}` | Удалить комментарий | Автор / OWNER |

### 7.8 NotificationController — `/api/notifications`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/notifications` | Список уведомлений пользователя |
| `GET` | `/notifications/unread-count` | Количество непрочитанных |
| `PUT` | `/notifications/{id}/read` | Отметить как прочитанное |
| `PUT` | `/notifications/read-all` | Отметить все как прочитанные |
| `DELETE` | `/notifications/{id}` | Удалить уведомление |

### 7.9 AiController — `/api/ai`

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/ai/extract-facts` | Анализ биографии через YandexGPT |

**Тело запроса:**
```json
{
  "biography": "Иван Иванов родился 15 января 1990 года в Москве...",
  "personId": 42
}
```

**Ответ:**
```json
{
  "success": true,
  "data": {
    "dates": ["15 января 1990 года", "2015 год"],
    "places": ["Москва", "Санкт-Петербург"],
    "professions": ["инженер", "преподаватель"],
    "relatives": ["Иванов Пётр (отец)", "Иванова Мария (мать)"],
    "error": null
  }
}
```

Ограничение: 10 запросов в минуту на пользователя (rate limiting).

---

## 8. Безопасность и авторизация

### 8.1 JWT

- Алгоритм: HMAC-SHA256
- Срок действия: 1 час (настраивается через `FAMILY_AUTH_JWT_LIFETIME`)
- Payload: `sub` = email пользователя
- Секрет: одинаковый в auth-service и tree-service (переменная `JWT_SECRET`)

### 8.2 Модель ролей

```
OWNER  ≥  EDITOR  ≥  VIEWER
  2          1          0      (ordinal)
```

Проверка: `userRole.ordinal() >= requiredRole.ordinal()`

| Действие | Минимальная роль |
|----------|-----------------|
| Просмотр дерева, персон, медиа | VIEWER |
| Добавление/редактирование персон, связей | EDITOR |
| Загрузка медиафайлов | EDITOR |
| Добавление комментариев | VIEWER |
| Приглашение участников | OWNER |
| Удаление дерева | OWNER |
| Управление публичной ссылкой | OWNER |

### 8.3 Верификация email

При регистрации пользователь получает письмо со ссылкой. До верификации `user.enabled = false`, вход невозможен. Токен верификации хранится в таблице `tokens` с типом `VERIFY` и сроком действия 24 часа.

Планировщик `TokenCleanUpScheduler` ежедневно в 3:00 удаляет истёкшие токены.

---

## 9. AI-анализ биографий (YandexGPT)

### 9.1 Как работает

Сервис [`AiService`](tree-service/src/main/java/com/project/familytree/tree/services/AiService.java) отправляет биографию в YandexGPT Completion API и извлекает структурированные факты.

### 9.2 Системный промпт

```
Ты — помощник по генеалогии. Проанализируй биографию и извлеки:
- dates: список дат (рождение, смерть, события)
- places: список мест (города, страны)
- professions: список профессий и занятий
- relatives: список упомянутых родственников с указанием степени родства

Верни ТОЛЬКО валидный JSON без пояснений.
```

### 9.3 Rate limiting

10 запросов в минуту на пользователя. Реализовано через `ConcurrentHashMap<Long, RateLimitData>` в памяти сервиса.

### 9.4 Конфигурация

```properties
ai.yandex.api-key=<IAM или API-ключ>
ai.yandex.folder-id=<ID каталога Yandex Cloud>
ai.yandex.model-uri=gpt://<folder-id>/yandexgpt/latest
```

---

## 10. Управление медиафайлами

### 10.1 Хранилище

Файлы хранятся в **Yandex Object Storage** (S3-совместимый). Путь к файлу: `{treeId}/{personId}/{UUID}.{ext}`.

### 10.2 Ограничения

- Максимальный размер файла: **50 МБ**
- Максимальное количество файлов на персону: **50**
- Допустимые типы: `PHOTO`, `DOCUMENT`, `VIDEO`, `AUDIO`
- Допустимые расширения: jpg, jpeg, png, gif, webp, pdf, doc, docx, mp4, avi, mov, mp3, wav

### 10.3 Аватар персоны

Аватар загружается через отдельный эндпоинт `POST /persons/{personId}/avatar` (multipart). Сохраняется в S3, URL записывается в поле `avatar_url` таблицы `persons`.

---

## 11. Уведомления

### 11.1 Когда создаются уведомления

| Событие | Получатели |
|---------|-----------|
| Добавлен комментарий к персоне | Все EDITOR и OWNER дерева (кроме автора) |
| Принято приглашение в дерево | OWNER дерева |
| Добавлена персона | Все участники дерева (кроме добавившего) |

### 11.2 Структура уведомления

```json
{
  "id": 1,
  "userId": 5,
  "message": "Пользователь Иван добавил комментарий к персоне Пётр Иванов",
  "read": false,
  "createdAt": "2026-03-11T12:00:00Z"
}
```

---

## 12. Тестирование

### 12.1 Покрытие

| Модуль | Тест-класс | Тестов |
|--------|-----------|--------|
| auth-service | `SecurityControllerTest` | 14 |
| auth-service | `TokenServiceTest` | 13 |
| tree-service | `MediaFileServiceTest` | 15 |
| tree-service | `NotificationServiceTest` | 14 |

### 12.2 Запуск тестов

```bash
# Все тесты
cd FamilyTree-API
mvn test

# Только auth-service
mvn test -pl auth-service

# Только tree-service
mvn test -pl tree-service
```

### 12.3 Стратегия

- **Unit-тесты** с `@ExtendWith(MockitoExtension.class)` — сервисы тестируются изолированно
- **Web-тесты** с `@WebMvcTest` — контроллеры тестируются с MockMvc
- Все зависимости мокируются через `@MockBean` / `@Mock`

---

## 13. Запуск локально

### 13.1 Требования

- Java 17+
- Maven 3.9+
- Docker + Docker Compose
- PostgreSQL (локальный или Yandex Managed)

### 13.2 Сборка

```bash
cd FamilyTree-API

# Сборка всех модулей (включая стартер)
mvn clean install -DskipTests

# Только сборка без тестов
mvn package -DskipTests
```

### 13.3 Запуск через Docker Compose

```bash
cd FamilyTree-API

# Создать .env файл (см. раздел 14)
cp .env.example .env
# Заполнить переменные в .env

# Запустить оба сервиса
docker-compose up -d

# Проверить статус
docker-compose ps

# Логи
docker-compose logs -f auth-service
docker-compose logs -f tree-service
```

### 13.4 Проверка работоспособности

```bash
# auth-service
curl http://localhost:8081/api/auth/ping

# tree-service (Swagger)
open http://localhost:8080/swagger-ui.html
```

---

## 14. Переменные окружения

### 14.1 Обязательные

| Переменная | Описание | Пример |
|-----------|----------|--------|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://host:5432/familytree?ssl=true&sslmode=verify-full` |
| `SPRING_DATASOURCE_USERNAME` | Пользователь БД | `familytree_user` |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД | `secret` |
| `JWT_SECRET` | Секрет для HMAC-SHA256 (≥32 символа) | `my-super-secret-key-32-chars-min` |

### 14.2 Почта (SMTP)

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `MAIL_HOST` | SMTP-сервер | `smtp.yandex.ru` |
| `MAIL_PORT` | SMTP-порт | `587` |
| `MAIL_USERNAME` | Email отправителя | — |
| `MAIL_PASSWORD` | Пароль / app-password | — |

### 14.3 Приложение

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `APP_BASE_URL` | URL фронтенда (для ссылок в письмах) | `http://localhost:3000` |
| `CORS_ALLOWED_ORIGINS` | Разрешённые origins (через запятую) | `http://localhost:3000` |
| `APP_EMAIL_VERIFICATION_REQUIRED` | Требовать верификацию email | `true` |

### 14.4 AI (только tree-service)

| Переменная | Описание |
|-----------|----------|
| `AI_YANDEX_API_KEY` | API-ключ YandexGPT |
| `AI_YANDEX_FOLDER_ID` | ID каталога Yandex Cloud |
| `AI_YANDEX_ASSISTANT_ID` | ID ассистента (опционально) |

### 14.5 S3 / Yandex Object Storage (только tree-service)

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `S3_ACCESS_KEY` | Access Key ID | — |
| `S3_SECRET_KEY` | Secret Access Key | — |
| `S3_BUCKET` | Имя бакета | `familytree` |
| `S3_ENDPOINT` | S3 endpoint | `https://storage.yandexcloud.net` |
| `S3_REGION` | Регион | `ru-central1` |

### 14.6 Пример .env файла

```env
# Database (Yandex Managed PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://rc1b-xxx.mdb.yandexcloud.net:6432/familytree?ssl=true&sslmode=verify-full
SPRING_DATASOURCE_USERNAME=familytree_user
SPRING_DATASOURCE_PASSWORD=your_db_password

# JWT (одинаковый для обоих сервисов!)
JWT_SECRET=your-very-long-secret-key-at-least-32-characters

# Mail
MAIL_HOST=smtp.yandex.ru
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.ru
MAIL_PASSWORD=your_app_password

# App
APP_BASE_URL=http://158.160.46.186:3000
CORS_ALLOWED_ORIGINS=http://158.160.46.186:3000,http://localhost:3000

# AI (опционально)
AI_YANDEX_API_KEY=your_yandex_api_key
AI_YANDEX_FOLDER_ID=your_folder_id

# S3
S3_ACCESS_KEY=your_s3_access_key
S3_SECRET_KEY=your_s3_secret_key
S3_BUCKET=familytree
```