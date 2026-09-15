#!/bin/bash
# Set up the Gemma 2.0 dev environment on a fresh macOS box.
#
# Idempotent: re-runs are safe; each step skips its work if already done.
# Designed to be invoked by the setup-dev-env Claude skill OR by hand —
# either way the flags drive the launch mode and clone paths.
#
# Usage:
#   scripts/setup-dev-env.sh --check
#       Just verify prerequisites; no clone, no writes.
#
#   scripts/setup-dev-env.sh \
#       --mode={frink|ghcr|source} \
#       --gemma-dir=<path> \
#       --ui-dir=<path> \
#       --frontend={browser|curation|both}
#
# See .claude/skills/setup-dev-env/SKILL.md for the orchestration.

set -u
set -o pipefail

# --- defaults ----------------------------------------------------------------

MODE="frink"
GEMMA_DIR="$HOME/Dev/eclipseworkspace/Gemma"
# Default UI clone location. Repo was renamed to 'gemma-ui' upstream so new
# clones land there; legacy machines may still have 'gemma-curation-ui' next
# to it — if that path exists we prefer it so this script doesn't try to
# clone a duplicate.
UI_DIR="$HOME/Dev/gemma-ui"
if [ -d "$HOME/Dev/gemma-curation-ui/.git" ] && [ ! -d "$UI_DIR/.git" ]; then
    UI_DIR="$HOME/Dev/gemma-curation-ui"
fi
FRONTEND="both"
CHECK_ONLY=0

GEMMA_REPO="https://github.com/PavlidisLab/Gemma.git"
UI_REPO="git@github.com:PavlidisLab/gemma-ui.git"

FRINK_URL="http://frink.msl.ubc.ca:8080"
GHCR_IMAGE="ghcr.io/pavlidislab/gemma-rest:2.0.0-alpha"
GHCR_LOCAL_URL="http://localhost:8080"

# --- arg parse ---------------------------------------------------------------

for arg in "$@"; do
    case "$arg" in
        --check)                 CHECK_ONLY=1 ;;
        --mode=*)                MODE="${arg#--mode=}" ;;
        --gemma-dir=*)           GEMMA_DIR="${arg#--gemma-dir=}" ;;
        --ui-dir=*)              UI_DIR="${arg#--ui-dir=}" ;;
        --frontend=*)            FRONTEND="${arg#--frontend=}" ;;
        -h|--help)
            sed -n '1,/^$/p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "unknown arg: $arg" >&2
            exit 2
            ;;
    esac
done

case "$MODE" in
    frink|ghcr|source) ;;
    *) echo "--mode must be one of: frink, ghcr, source (got $MODE)" >&2; exit 2 ;;
esac
case "$FRONTEND" in
    browser|curation|both) ;;
    *) echo "--frontend must be one of: browser, curation, both (got $FRONTEND)" >&2; exit 2 ;;
esac

# --- helpers -----------------------------------------------------------------

red()    { printf '\033[31m%s\033[0m\n' "$*"; }
green()  { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }
bold()   { printf '\033[1m%s\033[0m\n' "$*"; }

step()  { bold ""; bold "==> $*"; }
ok()    { green   "  ✓ $*"; }
miss()  { red     "  ✗ $*"; }
warn()  { yellow  "  ! $*"; }

have() { command -v "$1" >/dev/null 2>&1; }

require() {
    local name="$1" hint="$2"
    if have "$name"; then
        ok "$name → $(command -v "$name")"
        return 0
    else
        miss "$name not installed — $hint"
        return 1
    fi
}

# --- step 1: prerequisites ---------------------------------------------------

check_prereqs() {
    step "Checking prerequisites"
    local missing=0
    require git    "install Xcode CLT (xcode-select --install) or 'brew install git'" || missing=$((missing+1))
    require node   "brew install node — need ≥20"                                       || missing=$((missing+1))
    require npm    "comes with node"                                                    || missing=$((missing+1))
    if [ "$MODE" != "frink" ]; then
        require docker "brew install --cask docker; launch Docker Desktop once" || missing=$((missing+1))
    fi
    if [ "$MODE" = "source" ]; then
        if [ -d "/Library/Java/JavaVirtualMachines/temurin-25.jdk" ]; then
            ok "JDK 25 (temurin) installed"
        else
            miss "temurin-25 JDK not found — 'brew install --cask temurin@25'"
            missing=$((missing+1))
        fi
        require mvn  "brew install maven" || missing=$((missing+1))
    fi
    require security "macOS Keychain CLI; standard on macOS" || missing=$((missing+1))

    # node version check
    if have node; then
        local nv
        nv=$(node --version | sed 's/v//')
        local major="${nv%%.*}"
        if [ "$major" -lt 20 ] 2>/dev/null; then
            warn "node $nv is older than 20; upgrade with 'brew upgrade node'"
            missing=$((missing+1))
        fi
    fi

    if [ "$missing" -gt 0 ]; then
        echo
        red "$missing prerequisite(s) missing. Address and re-run."
        return 1
    fi
    return 0
}

# --- step 2: clone repos -----------------------------------------------------

clone_or_update() {
    local label="$1" repo="$2" dir="$3"
    step "Repo: $label"
    if [ -d "$dir/.git" ]; then
        ok "$dir already a git checkout"
        return 0
    fi
    if [ -e "$dir" ]; then
        miss "$dir exists but isn't a git checkout — move it aside first"
        return 1
    fi
    mkdir -p "$(dirname "$dir")"
    echo "  cloning $repo → $dir"
    if git clone "$repo" "$dir"; then
        ok "$label cloned"
    else
        miss "clone failed — confirm SSH/HTTPS access to $repo"
        return 1
    fi
}

# --- step 3: backend launch --------------------------------------------------

start_backend_ghcr() {
    step "Backend: pulling and running $GHCR_IMAGE"
    if ! docker info >/dev/null 2>&1; then
        miss "Docker daemon isn't running — start Docker Desktop and re-run"
        return 1
    fi
    if [ -z "$(docker ps -a --filter "name=^gemma-rest$" -q)" ]; then
        docker pull "$GHCR_IMAGE"
        # Run with an env file so the user can extend it later; create a minimal
        # one if missing. The image expects a real gemd reachable from inside
        # the container; the default GHCR build is configured for the prod SSH
        # tunnel via host.docker.internal:8000. New devs without the tunnel
        # will need to swap GEMMA_DB_HOST/PORT to point at their own MySQL.
        local env_file="$HOME/Gemma2.0/env.gemma"
        if [ ! -f "$env_file" ]; then
            mkdir -p "$(dirname "$env_file")"
            cat > "$env_file" <<'EOF'
# Minimal env.gemma for the GHCR-pulled gemma-rest container.
# Replace placeholders before starting against a real DB.
GEMMA_DB_HOST=host.docker.internal
GEMMA_DB_PORT=8000
GEMMA_DB_NAME=gemd
GEMMA_DB_USER=replaceme
GEMMA_DB_PASSWORD=replaceme
GEMMA_AGENT_PASSWORD=replaceme
GEMMA_RUNAS_PASSWORD=replaceme
GEMMA_ANONYMOUSAUTH_KEY=$(openssl rand -hex 32)
MAIL_USERNAME=noreply@localhost
MAIL_HOST=localhost
GEMMA_HOSTURL=http://localhost:8080
EOF
            warn "Created starter $env_file — fill in the DB / SSH-tunnel creds before the container will boot cleanly"
        fi
        docker run -d --name gemma-rest --restart unless-stopped \
            --env-file "$env_file" \
            -p 8080:8080 \
            "$GHCR_IMAGE"
    else
        ok "gemma-rest container already exists; not recreating"
    fi

    # Wait briefly for /rest/v2/ to answer
    echo "  waiting for backend to be ready (max 60s)..."
    for i in $(seq 1 60); do
        if curl -fsS -o /dev/null --max-time 2 "$GHCR_LOCAL_URL/rest/v2/" 2>/dev/null; then
            ok "backend responding on $GHCR_LOCAL_URL"
            return 0
        fi
        sleep 1
    done
    warn "backend didn't respond within 60s — check 'docker logs gemma-rest'"
    return 0  # don't fail the whole setup; the SPA can still start
}

build_backend_source() {
    step "Backend: building from source"
    if [ ! -d "/Library/Java/JavaVirtualMachines/temurin-25.jdk" ]; then
        miss "temurin-25 missing; install before --mode=source"
        return 1
    fi
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
    pushd "$GEMMA_DIR" >/dev/null || return 1
    if mvn -pl gemma-rest -am package -P gemma-rest-war -DskipTests -q; then
        ok "WAR built"
        # Inject the freshly-built WAR into a running container if there is one;
        # otherwise leave it to the operator since the docker-run command depends
        # on the user's gemd setup (matches ~/Gemma2.0/bin/update.sh on frink).
        if docker ps --filter "name=^gemma-rest$" -q | grep -q .; then
            docker restart gemma-rest
            ok "Existing gemma-rest container restarted"
        else
            warn "WAR built but no gemma-rest container running. Start one with --mode=ghcr first, then re-run with --mode=source to refresh."
        fi
    else
        miss "mvn package failed; inspect output above"
        popd >/dev/null
        return 1
    fi
    popd >/dev/null
}

# --- step 4: frontend wiring -------------------------------------------------

write_env_files() {
    local target_url="$1"
    step "Frontend env files (target $target_url)"

    case "$FRONTEND" in
        browser|both)
            local browser_env="$UI_DIR/apps/browser/.env.local"
            if [ -f "$browser_env" ] && grep -q "^GEMMA_BASE_URL=$target_url\$" "$browser_env"; then
                ok "$browser_env already targets $target_url"
            else
                cat > "$browser_env" <<EOF
# Generated by scripts/setup-dev-env.sh on $(date)
# Dev-server /rest proxy target — apps/browser/vite.config.ts honours this.
GEMMA_BASE_URL=$target_url
EOF
                ok "wrote $browser_env"
            fi
            ;;
    esac

    case "$FRONTEND" in
        curation|both)
            local curation_env="$UI_DIR/apps/curation/.env"
            if [ -f "$curation_env" ]; then
                ok "$curation_env already present (not overwriting; .env.example is the template)"
            else
                local rand_key
                rand_key=$(openssl rand -hex 16 2>/dev/null || echo "dev-token-$(date +%s)")
                cat > "$curation_env" <<EOF
# Generated by scripts/setup-dev-env.sh on $(date)
GEMMA_CURATION_URL=$target_url
VITE_GEMMA_CURATION_API_KEY=$rand_key
GEMMA_PROPOSER_URL=http://localhost:8090
VITE_GEMMA_WEB_URL=https://gemma.msl.ubc.ca
EOF
                ok "wrote $curation_env (random API key generated; rotate when wiring the real proposer)"
            fi
            ;;
    esac
}

npm_install() {
    step "npm install (workspaces)"
    pushd "$UI_DIR" >/dev/null || return 1
    if [ -d node_modules ] && [ -f node_modules/.package-lock.json ]; then
        ok "node_modules already populated; skipping (delete node_modules/ to re-install)"
    else
        if npm install --no-audit --no-fund; then
            ok "workspace deps installed"
        else
            miss "npm install failed"
            popd >/dev/null
            return 1
        fi
    fi
    popd >/dev/null
}

# --- step 5: probe -----------------------------------------------------------

probe_backend() {
    local url="$1"
    step "Probing $url/rest/v2/"
    local body
    if body=$(curl -fsS --max-time 8 "$url/rest/v2/" 2>/dev/null); then
        local version
        version=$(echo "$body" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("data",{}).get("version","?"))' 2>/dev/null)
        ok "reachable; version=$version"
    else
        warn "couldn't reach $url/rest/v2/ — check network / VPN / docker status"
    fi
}

# --- step 6: closing summary -------------------------------------------------

print_next_steps() {
    local url="$1"
    bold ""
    bold "================================================================="
    bold "  Setup complete. Next steps:"
    bold "================================================================="
    cat <<EOF

  Backend target: $url
  Repos:
    $GEMMA_DIR
    $UI_DIR

  Start the frontend (from $UI_DIR):

EOF
    case "$FRONTEND" in
        browser|both)   echo "    npm run dev:browser    → http://localhost:5183/browser" ;;
    esac
    case "$FRONTEND" in
        curation|both)  echo "    npm run dev:curation   → http://localhost:5174 (curation app)" ;;
    esac
    cat <<EOF

  Backend control (if --mode=ghcr or source):
    docker logs -f gemma-rest         # tail logs
    docker restart gemma-rest         # bounce after env.gemma edits
    docker stop gemma-rest            # shut down

  Keychain entries (add as needed for /me, /admin/*, curation API):
    security add-generic-password -s GEMMA_USERNAME -a \$USER -w '<your-lab-username>'
    security add-generic-password -s GEMMA_PASSWORD -a \$USER -w '<your-password>'

  Re-run this script any time:
    scripts/setup-dev-env.sh --mode=$MODE --gemma-dir=$GEMMA_DIR --ui-dir=$UI_DIR --frontend=$FRONTEND

EOF
}

# --- main --------------------------------------------------------------------

bold ""
bold "Gemma 2.0 dev environment setup"
bold "  mode:     $MODE"
bold "  gemma:    $GEMMA_DIR"
bold "  ui:       $UI_DIR"
bold "  frontend: $FRONTEND"

check_prereqs || exit 1
if [ "$CHECK_ONLY" -eq 1 ]; then
    bold ""
    green "Prerequisites OK (--check, no other work performed)."
    exit 0
fi

clone_or_update "Gemma (backend)" "$GEMMA_REPO" "$GEMMA_DIR" || exit 1
clone_or_update "gemma-ui (frontend)" "$UI_REPO" "$UI_DIR"  || exit 1

# Resolve the backend URL for env-file writing / probing
BACKEND_URL="$FRINK_URL"
case "$MODE" in
    frink)  BACKEND_URL="$FRINK_URL" ;;
    ghcr)
        BACKEND_URL="$GHCR_LOCAL_URL"
        start_backend_ghcr || true     # don't abort on backend startup hiccup
        ;;
    source)
        BACKEND_URL="$GHCR_LOCAL_URL"
        build_backend_source || exit 1
        ;;
esac

write_env_files "$BACKEND_URL"
npm_install || exit 1
probe_backend "$BACKEND_URL"
print_next_steps "$BACKEND_URL"
