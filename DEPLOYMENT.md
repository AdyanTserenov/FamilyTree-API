# Family Tree — Deployment Guide

## Содержание
1. [Архитектура системы](#1-архитектура-системы)
2. [Шаг 1 — Подготовка секретов (.env)](#2-шаг-1--подготовка-секретов-env)
3. [Шаг 2 — Yandex Mail: App-пароль](#3-шаг-2--yandex-mail-app-пароль)
4. [Шаг 3 — Yandex Cloud GPT (опционально)](#4-шаг-3--yandex-cloud-gpt-опционально)
5. [Шаг 4 — JWT Secret](#5-шаг-4--jwt-secret)
6. [Шаг 5 — Redis (нужен ли?)](#6-шаг-5--redis-нужен-ли)
7. [Шаг 6 — Локальный запуск (Docker Compose)](#7-шаг-6--локальный-запуск-docker-compose)
8. [Шаг 7 — Деплой на реальный сервер (VPS/облако)](#8-шаг-7--деплой-на-реальный-сервер-vpsоблако)
9. [Шаг 8 — HTTPS через Nginx + Certbot](#9-шаг-8--https-через-nginx--certbot)
10. [Шаг 9 — Проверка работоспособности](#10-шаг-9--проверка-работоспособности)
11. [Переменные окружения — полный справочник](#11-переменные-окружения--полный-справочник)

---

## 1. Архитектура системы

```
Браузер
  │
  │ :443 (HTTPS)
  ▼
[Nginx на хосте]  ← SSL termination, проксирует на Docker
  │
  │ :3000 (HTTP внутри Docker)
  ▼
[frontend container — nginx:alpine]
  │  /api/auth/*  → auth-service:8081
  │  /api/profile → auth-service:8081
  │  /api/*       → tree-service:8080
  ▼
[auth-service:8081]   [tree-service:8080]
        │                     │
        └──────────┬──────────┘
                   ▼
          [postgres:5432]
```

**5 модулей проекта:**

| Модуль | Роль | Порт |
|--------|------|------|
| `postgres` | Единая БД для обоих сервисов | 5432 (внутренний) |
| `auth-service` | Регистрация, вход, JWT, профиль, email | 8081 |
| `tree-service` | Деревья, персоны, медиа, AI, уведомления | 8080 |
| `frontend` | React SPA + nginx reverse proxy | 3000→80 |
| `family-tree-auth-starter` | Shared Maven lib (JWT verify для tree-service) | — (jar) |

---

## 2. Шаг 1 — Подготовка секретов (.env)

Создайте файл `FamilyTree-API/.env` (он уже в `.gitignore` через `.env.example`):

```bash
cd FamilyTree-API
cp .env.example .env
nano .env   # или любой редактор
```

Содержимое `.env`:

```dotenv
# ── PostgreSQL ──────────────────────────────────────────────
POSTGRES_PASSWORD=ВашНадёжныйПароль123!

# ── JWT (одинаковый для auth-service и tree-service) ────────
# Генерация: openssl rand -base64 64
JWT_SECRET=сгенерируйте_строку_минимум_64_символа_base64

# ── Email (Yandex SMTP) ─────────────────────────────────────
MAIL_USERNAME=ваш-адрес@yandex.ru
MAIL_PASSWORD=app-пароль-из-яндекс-id   # НЕ пароль от аккаунта!

# ── Yandex GPT (опционально, оставьте пустым чтобы отключить) ─
AI_YANDEX_API_KEY=
AI_YANDEX_FOLDER_ID=
```

> **Важно:** `.env` никогда не коммитить в git. Добавьте в `.gitignore` если ещё нет.

---

## 3. Шаг 2 — Yandex Mail: App-пароль

Сервис использует Yandex SMTP (`smtp.yandex.ru:587`) для отправки писем (подтверждение email, сброс пароля, приглашения).

**Алгоритм получения app-пароля:**

1. Войдите на [id.yandex.ru](https://id.yandex.ru)
2. Перейдите: **Безопасность** → **Пароли приложений**
3. Нажмите **Создать новый пароль**
4. Выберите тип: **Почта**
5. Введите название: `FamilyTree`
6. Скопируйте сгенерированный пароль (показывается один раз!)
7. Вставьте в `.env` как `MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx`

> **Важно:** В настройках почты Yandex должен быть включён **IMAP/SMTP доступ**:
> Почта → Настройки → Все настройки → Почтовые программы → Включить IMAP

---

## 4. Шаг 3 — Yandex Cloud GPT (опционально)

Используется для функции «Извлечь факты из биографии» в `PersonPage`. Если оставить пустым — кнопка AI просто не будет работать, остальное работает нормально.

**Алгоритм получения ключей:**

1. Зарегистрируйтесь на [console.yandex.cloud](https://console.yandex.cloud)
2. Создайте **каталог** (folder) — запомните его ID (`AI_YANDEX_FOLDER_ID`)
3. Перейдите: **IAM** → **Сервисные аккаунты** → **Создать**
4. Назначьте роль: `ai.languageModels.user`
5. Перейдите в созданный аккаунт → **API-ключи** → **Создать API-ключ**
6. Скопируйте ключ (`AI_YANDEX_API_KEY=AQVN...`)
7. Вставьте оба значения в `.env`

---

## 5. Шаг 4 — JWT Secret

JWT-секрет **должен быть одинаковым** в `auth-service` и `tree-service` — они оба подписывают/верифицируют токены одним ключом.

**Генерация надёжного секрета:**

```bash
openssl rand -base64 64
# Пример вывода: K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=...
```

Вставьте результат в `.env` как `JWT_SECRET=...`

В `docker-compose.yml` оба сервиса уже получают его через `${JWT_SECRET:-...}`:
- `auth-service`: `FAMILY_AUTH_JWT_SECRET: ${JWT_SECRET:-...}`
- `tree-service`: `FAMILY_AUTH_JWT_SECRET: ${JWT_SECRET:-...}`

---

## 6. Шаг 5 — Redis (нужен ли?)

**Текущий ответ: Redis в проекте НЕ используется и НЕ нужен.**

Вот что используется вместо него:

| Задача | Решение в проекте |
|--------|-------------------|
| Rate limiting для AI | `ConcurrentHashMap` в памяти (`AiService.rateLimitMap`) |
| Сессии / токены | JWT (stateless) — хранятся на клиенте |
| Email-токены (verify, reset) | Таблицы `verify_tokens`, `reset_tokens` в PostgreSQL |
| Invite-токены | Таблица `invitations` в PostgreSQL |

**Когда Redis понадобится (в будущем):**
- Если нужен rate limiting при горизонтальном масштабировании (несколько инстансов tree-service)
- Если нужен distributed cache для сессий
- Если нужны WebSocket-уведомления в реальном времени через pub/sub

**Для текущего проекта Redis добавлять не нужно.**

---

## 7. Шаг 6 — Локальный запуск (Docker Compose)

```bash
# 1. Перейдите в директорию с docker-compose.yml
cd FamilyTree-API

# 2. Убедитесь что .env заполнен (шаг 1)
cat .env

# 3. Соберите и запустите все сервисы
docker compose up --build -d

# 4. Проверьте статус
docker compose ps

# 5. Посмотрите логи (если что-то не стартует)
docker compose logs auth-service --tail=50
docker compose logs tree-service --tail=50
docker compose logs postgres --tail=30

# 6. Проверьте доступность
curl http://localhost:3000/health          # frontend nginx
curl http://localhost:8081/auth/ping       # auth-service
curl http://localhost:8080/actuator/health # tree-service (если есть actuator)
```

**Ожидаемый порядок старта:**
1. `postgres` — ждёт healthcheck (pg_isready)
2. `auth-service` + `tree-service` — стартуют после postgres healthy
3. `frontend` — стартует после обоих сервисов

**Остановка:**
```bash
docker compose down          # остановить, сохранить данные
docker compose down -v       # остановить + удалить volumes (СБРОС БД!)
```

---

## 8. Шаг 7 — Деплой на реальный сервер (VPS/облако)

### 8.1 Требования к серверу

| Параметр | Минимум | Рекомендуется |
|----------|---------|---------------|
| CPU | 2 vCPU | 4 vCPU |
| RAM | 2 GB | 4 GB |
| Диск | 20 GB SSD | 40 GB SSD |
| ОС | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| Docker | 24+ | 24+ |
| Docker Compose | v2.20+ | v2.20+ |

### 8.2 Установка Docker на Ubuntu

```bash
# Обновление пакетов
sudo apt update && sudo apt upgrade -y

# Установка зависимостей
sudo apt install -y ca-certificates curl gnupg lsb-release

# Добавление GPG ключа Docker
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Добавление репозитория
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Установка Docker Engine + Compose
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Добавление пользователя в группу docker (без sudo)
sudo usermod -aG docker $USER
newgrp docker

# Проверка
docker --version
docker compose version
```

### 8.3 Копирование кода на сервер

**Вариант A — через git (рекомендуется):**
```bash
# На сервере
git clone https://github.com/ваш-репозиторий/vkr.git /opt/familytree
cd /opt/familytree/FamilyTree-API
```

**Вариант B — через scp:**
```bash
# С локальной машины
scp -r ./FamilyTree-API user@your-server-ip:/opt/familytree/
scp -r ./FamilyTree-Frontend user@your-server-ip:/opt/familytree/
```

### 8.4 Настройка .env на сервере

```bash
cd /opt/familytree/FamilyTree-API
nano .env
```

```dotenv
POSTGRES_PASSWORD=УльтраНадёжныйПароль!2024
JWT_SECRET=$(openssl rand -base64 64)
MAIL_USERNAME=ваш@yandex.ru
MAIL_PASSWORD=app-пароль-яндекс
AI_YANDEX_API_KEY=ваш-ключ-или-пусто
AI_YANDEX_FOLDER_ID=ваш-folder-id-или-пусто
```

### 8.5 Настройка docker-compose.yml для продакшена

Измените в [`docker-compose.yml`](docker-compose.yml) следующее:

```yaml
# 1. Убрать проброс портов postgres наружу (безопасность):
postgres:
  # ports:          # ← закомментировать или удалить
  #   - "5432:5432"

# 2. Убрать проброс портов бэкенда наружу (доступ только через nginx):
auth-service:
  # ports:
  #   - "8081:8081"

tree-service:
  # ports:
  #   - "8080:8080"

# 3. Оставить только frontend:
frontend:
  ports:
    - "3000:80"   # nginx на хосте будет проксировать сюда
```

### 8.6 Запуск на сервере

```bash
cd /opt/familytree/FamilyTree-API

# Первый запуск (сборка образов)
docker compose up --build -d

# Проверка
docker compose ps
docker compose logs --tail=100 -f
```

---

## 9. Шаг 8 — HTTPS через Nginx + Certbot

Nginx на хосте (не в Docker) принимает HTTPS и проксирует на Docker-контейнер frontend.

### 9.1 Установка Nginx и Certbot

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
```

### 9.2 Конфигурация Nginx (HTTP, до получения сертификата)

```bash
sudo nano /etc/nginx/sites-available/familytree
```

```nginx
server {
    listen 80;
    server_name ваш-домен.ru www.ваш-домен.ru;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        client_max_body_size 50M;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/familytree /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 9.3 Получение SSL-сертификата (Let's Encrypt)

```bash
# Убедитесь что домен указывает на IP сервера (DNS A-запись)
sudo certbot --nginx -d ваш-домен.ru -d www.ваш-домен.ru

# Certbot автоматически обновит конфиг nginx для HTTPS
# Автообновление сертификата (проверка):
sudo certbot renew --dry-run
```

После этого Certbot автоматически добавит в конфиг:
- `listen 443 ssl`
- `ssl_certificate` / `ssl_certificate_key`
- Редирект HTTP → HTTPS

### 9.4 Обновление CORS в application.properties

После получения домена обновите CORS в [`tree-service/application.properties`](tree-service/src/main/resources/application.properties):

```properties
spring.web.cors.allowed-origins=https://ваш-домен.ru,https://www.ваш-домен.ru
```

И в `auth-service` — найдите `CorsConfig.java` и добавьте ваш домен в список разрешённых origins.

---

## 10. Шаг 9 — Проверка работоспособности

После деплоя проверьте каждый слой:

```bash
# 1. PostgreSQL
docker compose exec postgres psql -U postgres -d familytree -c "\dt"
# Должны быть таблицы: users, trees, tree_memberships, persons, ...

# 2. auth-service
curl https://ваш-домен.ru/api/auth/ping
# Ответ: {"success":true,"message":"pong","data":null}

# 3. Регистрация пользователя
curl -X POST https://ваш-домен.ru/api/auth/sign-up \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","password":"Test1234!"}'

# 4. Проверьте что письмо пришло на email

# 5. Вход
curl -X POST https://ваш-домен.ru/api/auth/sign-in \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'
# Ответ должен содержать token

# 6. Профиль (с токеном из шага 5)
curl https://ваш-домен.ru/api/profile \
  -H "Authorization: Bearer ВАШ_ТОКЕН"

# 7. Создание дерева
curl -X POST https://ваш-домен.ru/api/trees \
  -H "Authorization: Bearer ВАШ_ТОКЕН" \
  -H "Content-Type: application/json" \
  -d '{"name":"Моё дерево"}'
```

---

## 11. Переменные окружения — полный справочник

| Переменная | Сервис | Обязательна | Описание |
|------------|--------|-------------|----------|
| `POSTGRES_PASSWORD` | postgres | ✅ | Пароль БД |
| `JWT_SECRET` | auth + tree | ✅ | Секрет подписи JWT, мин. 32 символа |
| `MAIL_USERNAME` | auth + tree | ✅ | Email для отправки писем |
| `MAIL_PASSWORD` | auth + tree | ✅ | App-пароль Yandex (не пароль аккаунта) |
| `MAIL_HOST` | auth + tree | ❌ | SMTP хост (default: smtp.yandex.ru) |
| `MAIL_PORT` | auth + tree | ❌ | SMTP порт (default: 587) |
| `AI_YANDEX_API_KEY` | tree | ❌ | API-ключ Yandex GPT (пусто = AI отключён) |
| `AI_YANDEX_FOLDER_ID` | tree | ❌ | Folder ID Yandex Cloud |
| `SPRING_DATASOURCE_URL` | auth + tree | ❌ | JDBC URL (default: postgres:5432) |
| `SPRING_DATASOURCE_USERNAME` | auth + tree | ❌ | Пользователь БД (default: postgres) |
| `SPRING_DATASOURCE_PASSWORD` | auth + tree | ❌ | Пароль БД (default: postgres) |
| `FAMILY_AUTH_JWT_LIFETIME` | auth + tree | ❌ | Время жизни токена мс (default: 3600000 = 1ч) |

---

## Быстрый старт (TL;DR)

```bash
# 1. Клонировать репозиторий
git clone <repo> && cd vkr/FamilyTree-API

# 2. Создать .env
cat > .env << 'EOF'
POSTGRES_PASSWORD=MySecurePass123!
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
MAIL_USERNAME=your@yandex.ru
MAIL_PASSWORD=your-app-password
AI_YANDEX_API_KEY=
AI_YANDEX_FOLDER_ID=
EOF

# 3. Запустить
docker compose up --build -d

# 4. Открыть в браузере
open http://localhost:3000
```
