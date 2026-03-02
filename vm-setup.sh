#!/bin/bash
# ============================================================
# Family Tree — VM Initial Setup Script
# Run this ONCE on the VM to prepare everything for CI/CD
#
# Usage:
#   chmod +x vm-setup.sh
#   ./vm-setup.sh <FRONTEND_REPO_URL>
#
# Example:
#   ./vm-setup.sh https://github.com/your-org/FamilyTree-Frontend.git
# ============================================================

set -e

FRONTEND_REPO_URL="${1}"
WORK_DIR="$HOME/vkr"
API_DIR="$WORK_DIR/FamilyTree-API"
FRONTEND_DIR="$WORK_DIR/FamilyTree-Frontend"

# ── Colors ──────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Validate args ────────────────────────────────────────────
if [ -z "$FRONTEND_REPO_URL" ]; then
  err "Usage: $0 <FRONTEND_REPO_URL>\nExample: $0 https://github.com/your-org/FamilyTree-Frontend.git"
fi

echo "============================================================"
echo " Family Tree VM Setup"
echo " API dir:      $API_DIR"
echo " Frontend dir: $FRONTEND_DIR"
echo " Frontend repo: $FRONTEND_REPO_URL"
echo "============================================================"
echo ""

# ── Step 1: Check prerequisites ──────────────────────────────
echo "=== Step 1: Checking prerequisites ==="

command -v docker >/dev/null 2>&1 || err "Docker is not installed. Install it first."
docker compose version >/dev/null 2>&1 || err "Docker Compose plugin is not installed."
command -v git >/dev/null 2>&1 || err "Git is not installed."

log "Docker: $(docker --version)"
log "Docker Compose: $(docker compose version)"
log "Git: $(git --version)"

# ── Step 2: Setup FamilyTree-Frontend as git repo ────────────
echo ""
echo "=== Step 2: Setting up FamilyTree-Frontend git repository ==="

if [ -d "$FRONTEND_DIR/.git" ]; then
  log "FamilyTree-Frontend is already a git repository. Pulling latest..."
  cd "$FRONTEND_DIR"
  git pull origin main
else
  if [ -d "$FRONTEND_DIR" ] && [ "$(ls -A $FRONTEND_DIR)" ]; then
    warn "Directory $FRONTEND_DIR exists but is not a git repo. Backing up and re-cloning..."
    mv "$FRONTEND_DIR" "${FRONTEND_DIR}.backup.$(date +%Y%m%d_%H%M%S)"
    log "Backup created at ${FRONTEND_DIR}.backup.*"
  fi

  log "Cloning FamilyTree-Frontend..."
  cd "$WORK_DIR"
  git clone "$FRONTEND_REPO_URL" FamilyTree-Frontend
  log "FamilyTree-Frontend cloned successfully"
fi

# ── Step 3: Check API git repo ───────────────────────────────
echo ""
echo "=== Step 3: Checking FamilyTree-API git repository ==="

if [ -d "$API_DIR/.git" ]; then
  log "FamilyTree-API is already a git repository"
  cd "$API_DIR"
  git remote -v
  git branch
else
  err "FamilyTree-API is not a git repository at $API_DIR. Something is wrong."
fi

# ── Step 4: Check .env file ──────────────────────────────────
echo ""
echo "=== Step 4: Checking .env file ==="

if [ -f "$API_DIR/.env" ]; then
  log ".env file exists at $API_DIR/.env"
else
  warn ".env file NOT found. Creating from template..."
  cp "$API_DIR/.env.example" "$API_DIR/.env"
  warn "IMPORTANT: Edit $API_DIR/.env with real values before starting!"
  warn "  nano $API_DIR/.env"
fi

# ── Step 5: Create shared Docker network ─────────────────────
echo ""
echo "=== Step 5: Creating shared Docker network ==="

if docker network ls | grep -q "familytree-net"; then
  log "Docker network 'familytree-net' already exists"
else
  docker network create familytree-net
  log "Docker network 'familytree-net' created"
fi

# ── Step 6: Start API services ───────────────────────────────
echo ""
echo "=== Step 6: Starting API services (postgres, auth-service, tree-service) ==="

cd "$API_DIR"
docker compose up -d --build postgres auth-service tree-service
log "API services started"

# ── Step 7: Start Frontend service ───────────────────────────
echo ""
echo "=== Step 7: Starting Frontend service ==="

cd "$FRONTEND_DIR"
docker compose up -d --build
log "Frontend service started"

# ── Step 8: Health checks ────────────────────────────────────
echo ""
echo "=== Step 8: Health checks (waiting 30s for services) ==="
sleep 30

echo "Container status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
curl -sf http://localhost:3000/health > /dev/null && log "Frontend (port 3000): OK" || warn "Frontend (port 3000): NOT READY"
curl -sf http://localhost:8081/auth/ping > /dev/null && log "Auth service (port 8081): OK" || warn "Auth service (port 8081): NOT READY"
curl -sf http://localhost:8080/actuator/health > /dev/null && log "Tree service (port 8080): OK" || warn "Tree service (port 8080): NOT READY (may not have actuator)"

# ── Done ─────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo -e "${GREEN} Setup complete!${NC}"
echo ""
echo " Next steps:"
echo "  1. Add GitHub Secrets to both repositories (see CI-CD.md)"
echo "  2. Push to 'main' branch to trigger auto-deploy"
echo ""
echo " Useful commands:"
echo "  API logs:      cd ~/vkr/FamilyTree-API && docker compose logs -f"
echo "  Frontend logs: cd ~/vkr/FamilyTree-Frontend && docker compose logs -f"
echo "  All containers: docker ps"
echo "============================================================"
