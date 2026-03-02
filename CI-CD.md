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
