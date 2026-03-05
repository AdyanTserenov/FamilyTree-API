# Family Tree — CI/CD Guide

## Архитектура CI/CD

```
GitHub Push (main)
       │
       ├── FamilyTree-API repo ──────► GitHub Actions ──► SSH ──► ~/vkr/FamilyTree-API
       │   github.com/AdyanTserenov/       deploy.yml              git pull + docker compose up
       │   FamilyTree-API                                          (postgres, auth-service, tree-service)
       │
       └── FamilyTree-Frontend repo ─► GitHub Actions ──► SSH ──► ~/vkr/FamilyTree-Frontend
           github.com/AdyanTserenov/       deploy.yml              git pull + docker compose up
           FamilyTree-Frontend                                      (frontend nginx container)
```

**Структура на VM (`158.160.46.186`):**
```
~/vkr/
├── FamilyTree-API/          ← git repo (github.com/AdyanTserenov/FamilyTree-API)
│   ├── docker-compose.yml   ← управляет: postgres, auth-service, tree-service
│   ├── .env                 ← секреты (не в git!)
│   ├── vm-setup.sh          ← скрипт первоначальной настройки
│   └── CI-CD.md             ← этот файл
│
└── FamilyTree-Frontend/     ← git repo (github.com/AdyanTserenov/FamilyTree-Frontend)
    ├── docker-compose.yml   ← управляет: frontend (nginx)
    └── Dockerfile
```

**Docker-сеть:** оба compose-проекта используют общую сеть `familytree-net`.

---

## Шаг 1 — Первоначальная настройка VM

> Выполняется **один раз** вручную. Нужно превратить `~/vkr/FamilyTree-Frontend` из обычной папки в git-репозиторий.

### 1.1 Подключитесь к VM

```bash
ssh atserenov@158.160.46.186
```

### 1.2 Настройте FamilyTree-Frontend как git-репозиторий

```bash
# Сделайте резервную копию текущей папки
mv ~/vkr/FamilyTree-Frontend ~/vkr/FamilyTree-Frontend.bak

# Клонируйте репозиторий
cd ~/vkr
git clone https://github.com/AdyanTserenov/FamilyTree-Frontend.git

# Проверьте
ls ~/vkr/FamilyTree-Frontend/
git -C ~/vkr/FamilyTree-Frontend remote -v
```

### 1.3 Создайте общую Docker-сеть

```bash
# Проверьте существующие сети
docker network ls

# Если сети familytree-net нет — создайте её
docker network create familytree-net
```

### 1.4 Запустите API-сервисы (если не запущены)

```bash
cd ~/vkr/FamilyTree-API
docker compose up -d
```

### 1.5 Запустите Frontend

```bash
cd ~/vkr/FamilyTree-Frontend
docker compose up --build -d
```

### 1.6 Проверьте работу

```bash
docker ps
curl http://localhost:3000/health
curl http://localhost:8081/auth/ping
```

---

## Шаг 1.7 — Настройка `.env` на VM

> Выполняется **один раз** вручную после клонирования репозитория. Файл `.env` не хранится в git — его нужно создать на VM вручную.

### Минимальный `.env` для production

```bash
ssh atserenov@158.160.46.186
cat > ~/vkr/FamilyTree-API/.env << 'EOF'
# JWT
JWT_SECRET=<сгенерируйте_случайную_строку_минимум_64_символа>

# Database
POSTGRES_DB=familytree
POSTGRES_USER=familytree
POSTGRES_PASSWORD=<надёжный_пароль>

# App base URL (используется в ссылках в письмах)
APP_BASE_URL=http://158.160.46.186:8081

# CORS — список разрешённых origins через запятую (без пробелов)
# Должен совпадать с адресом, с которого браузер открывает фронтенд
CORS_ALLOWED_ORIGINS=http://158.160.46.186:3000

# S3 / Object Storage
S3_ENDPOINT=<endpoint>
S3_ACCESS_KEY=<access_key>
S3_SECRET_KEY=<secret_key>
S3_BUCKET=<bucket_name>
S3_REGION=<region>

# Mail
MAIL_HOST=<smtp_host>
MAIL_PORT=465
MAIL_USERNAME=<email>
MAIL_PASSWORD=<password>
EOF
```

> **Важно:** `CORS_ALLOWED_ORIGINS` — это **отдельная** переменная от `APP_BASE_URL`.
> - `APP_BASE_URL` используется только для формирования ссылок в письмах (confirm email, reset password).
> - `CORS_ALLOWED_ORIGINS` — список origins, которым разрешено делать cross-origin запросы к API.
>   Если фронтенд доступен по нескольким адресам, перечислите их через запятую:
>   ```
>   CORS_ALLOWED_ORIGINS=http://158.160.46.186:3000,https://yourdomain.com
>   ```

### Проверка после создания `.env`

```bash
# Убедитесь что переменная подхватывается
cd ~/vkr/FamilyTree-API
grep CORS_ALLOWED_ORIGINS .env

# Перезапустите сервисы чтобы применить изменения
docker compose up -d --no-deps auth-service tree-service

# Проверьте CORS-заголовок в ответе
curl -v -H "Origin: http://158.160.46.186:3000" \
     http://localhost:8081/auth/ping 2>&1 | grep -i "access-control"
# Ожидаемый вывод: Access-Control-Allow-Origin: http://158.160.46.186:3000
```

---

## Шаг 2 — Настройка GitHub Secrets

Нужно добавить **одинаковые** секреты в **оба** репозитория.

### 2.1 Создайте SSH-ключ для GitHub Actions на VM

```bash
# Подключитесь к VM
ssh atserenov@158.160.46.186

# Создайте ключ специально для деплоя
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_deploy -N ""

# Добавьте публичный ключ в authorized_keys
cat ~/.ssh/github_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Выведите ПРИВАТНЫЙ ключ — скопируйте его целиком
cat ~/.ssh/github_deploy
```

### 2.2 Добавьте секреты в FamilyTree-API репозиторий

Перейдите: **[github.com/AdyanTserenov/FamilyTree-API](https://github.com/AdyanTserenov/FamilyTree-API) → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|-------------|-------|
| `SSH_HOST` | `158.160.46.186` |
| `SSH_USER` | `atserenov` |
| `SSH_PORT` | `22` |
| `SSH_PRIVATE_KEY` | содержимое `~/.ssh/github_deploy` (весь текст от `-----BEGIN` до `-----END`) |

### 2.3 Добавьте те же секреты в FamilyTree-Frontend репозиторий

Перейдите: **[github.com/AdyanTserenov/FamilyTree-Frontend](https://github.com/AdyanTserenov/FamilyTree-Frontend) → Settings → Secrets and variables → Actions → New repository secret**

Добавьте те же 4 секрета с теми же значениями.

---

## Шаг 3 — Проверка CI/CD

### 3.1 Убедитесь что workflow-файлы в репозиториях

**FamilyTree-API** должен содержать файл `.github/workflows/deploy.yml`

**FamilyTree-Frontend** должен содержать файл `.github/workflows/deploy.yml`

> Файл для Frontend находится в этом workspace по пути `FamilyTree-Frontend/.github/workflows/deploy.yml` — его нужно скопировать/закоммитить в репозиторий FamilyTree-Frontend.

### 3.2 Триггер деплоя API

```bash
# В локальном репозитории FamilyTree-API
git add .
git commit -m "ci: add GitHub Actions deploy workflow"
git push origin main
```

Перейдите в **[github.com/AdyanTserenov/FamilyTree-API/actions](https://github.com/AdyanTserenov/FamilyTree-API/actions)** и следите за выполнением.

### 3.3 Триггер деплоя Frontend

```bash
# В локальном репозитории FamilyTree-Frontend
# Скопируйте файл FamilyTree-Frontend/.github/workflows/deploy.yml в репозиторий
git add .github/workflows/deploy.yml
git commit -m "ci: add GitHub Actions deploy workflow"
git push origin main
```

Перейдите в **[github.com/AdyanTserenov/FamilyTree-Frontend/actions](https://github.com/AdyanTserenov/FamilyTree-Frontend/actions)** и следите за выполнением.

---

## Как работает деплой

### FamilyTree-API (`.github/workflows/deploy.yml`)

Срабатывает при push в `main`. Выполняет на VM:

```
1. cd ~/vkr/FamilyTree-API && git pull origin main
2. docker compose up --build -d --no-deps postgres auth-service tree-service
3. Health check: curl http://localhost:8081/auth/ping
```

### FamilyTree-Frontend (`.github/workflows/deploy.yml`)

Срабатывает при push в `main`. Выполняет на VM:

```
1. cd ~/vkr/FamilyTree-Frontend && git pull origin main
2. docker compose up --build -d
3. Health check: curl http://localhost:3000/health
```

---

## Полезные команды на VM

```bash
# Статус всех контейнеров
docker ps

# Логи API-сервисов
cd ~/vkr/FamilyTree-API
docker compose logs -f auth-service
docker compose logs -f tree-service
docker compose logs -f postgres

# Логи фронтенда
cd ~/vkr/FamilyTree-Frontend
docker compose logs -f frontend

# Перезапуск API вручную
cd ~/vkr/FamilyTree-API
docker compose up --build -d

# Перезапуск Frontend вручную
cd ~/vkr/FamilyTree-Frontend
docker compose up --build -d

# Остановить всё (данные сохраняются)
cd ~/vkr/FamilyTree-API && docker compose down
cd ~/vkr/FamilyTree-Frontend && docker compose down

# Полный сброс (УДАЛЯЕТ ДАННЫЕ БД!)
cd ~/vkr/FamilyTree-API && docker compose down -v
```

---

## Troubleshooting

### Workflow завершился с ошибкой

1. Откройте **GitHub → Actions → [упавший workflow]** — посмотрите логи
2. Подключитесь к VM и проверьте контейнеры:
   ```bash
   docker ps -a
   docker compose logs --tail=50 auth-service
   ```

### Контейнер не стартует

```bash
docker logs familytree-auth --tail=100
docker logs familytree-tree --tail=100
docker logs familytree-frontend --tail=100
```

### Ошибка "network familytree-net not found"

```bash
# Создать сеть вручную
docker network create familytree-net

# Перезапустить frontend
cd ~/vkr/FamilyTree-Frontend
docker compose up -d
```

### После логина GET /api/trees возвращает 401 (CORS)

**Симптом:** В Network tab браузера запрос к API не содержит заголовка `Authorization`, хотя логин прошёл успешно.

**Причина:** Браузер отправляет `Origin: http://<IP>:3000`. Если этот origin не входит в `CORS_ALLOWED_ORIGINS` — preflight отклоняется, браузер блокирует запрос целиком, и токен не отправляется.

**Решение:**

```bash
# 1. Проверьте текущее значение
grep CORS_ALLOWED_ORIGINS ~/vkr/FamilyTree-API/.env

# 2. Если переменной нет или значение неверное — добавьте/исправьте
#    (замените IP на реальный адрес вашего сервера)
echo 'CORS_ALLOWED_ORIGINS=http://158.160.46.186:3000' >> ~/vkr/FamilyTree-API/.env

# 3. Перезапустите сервисы
cd ~/vkr/FamilyTree-API
docker compose up -d --no-deps auth-service tree-service

# 4. Проверьте CORS-заголовок
curl -v -H "Origin: http://158.160.46.186:3000" \
     http://localhost:8081/auth/ping 2>&1 | grep -i "access-control"
# Ожидаемый вывод: Access-Control-Allow-Origin: http://158.160.46.186:3000
```

> Если фронтенд доступен по нескольким адресам (например, IP и домен), перечислите их через запятую **без пробелов**:
> ```
> CORS_ALLOWED_ORIGINS=http://158.160.46.186:3000,https://yourdomain.com
> ```

---

### git pull требует авторизацию (приватный репозиторий)

```bash
# На VM создайте deploy key
ssh-keygen -t ed25519 -C "vm-deploy-key" -f ~/.ssh/frontend_deploy -N ""
cat ~/.ssh/frontend_deploy.pub
# Добавьте в GitHub → FamilyTree-Frontend → Settings → Deploy keys

# Настройте SSH для использования этого ключа
cat >> ~/.ssh/config << 'EOF'
Host github-frontend
  HostName github.com
  User git
  IdentityFile ~/.ssh/frontend_deploy

Host github-api
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_deploy
EOF

# Перенастройте remote
cd ~/vkr/FamilyTree-Frontend
git remote set-url origin git@github-frontend:AdyanTserenov/FamilyTree-Frontend.git
```

---

## Порты и доступность

| Сервис | Порт | URL |
|--------|------|-----|
| Frontend (nginx) | 3000 | http://158.160.46.186:3000 |
| Auth Service | 8081 | http://158.160.46.186:8081 |
| Tree Service | 8080 | http://158.160.46.186:8080 |
| PostgreSQL | 5432 | только внутри Docker-сети |
