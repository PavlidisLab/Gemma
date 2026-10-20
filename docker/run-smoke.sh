#!/usr/bin/env bash
# Smoke-test runner: stand up a transient MySQL + the gemma-rest container,
# poll /rest/v2/health, dump the result, and tear down.
#
# Prereqs: docker daemon, docker compose v2, an image tagged `gemma-rest:dev`
# (build with `docker build -t gemma-rest:dev .` from the repo root).
#
# NOTE: this is for local validation only -- it stubs MySQL with an
# uninitialised gemd database, so anything past startup that touches schema
# objects (every real REST query) WILL fail until Flyway has run against the
# stub DB. The smoke target is "does the container start and serve a TCP
# socket". For a fuller test, point GEMMA_DB_* at a Flyway-migrated DB.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${IMAGE:-gemma-rest:dev}"
NET="${NET:-gemma-smoke}"
MYSQL_NAME="${MYSQL_NAME:-gemma-smoke-mysql}"
APP_NAME="${APP_NAME:-gemma-smoke-app}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-smokepw}"

cleanup() {
    echo "--- cleanup ---"
    docker rm -f "$APP_NAME"   2>/dev/null || true
    docker rm -f "$MYSQL_NAME" 2>/dev/null || true
    docker network rm "$NET"   2>/dev/null || true
}
trap cleanup EXIT

echo "--- network ---"
docker network create "$NET" >/dev/null

echo "--- mysql (stub) ---"
docker run -d --name "$MYSQL_NAME" --network "$NET" \
  -e MYSQL_ROOT_PASSWORD="$MYSQL_PASSWORD" \
  -e MYSQL_DATABASE=gemd \
  -e MYSQL_USER=gemmauser \
  -e MYSQL_PASSWORD="$MYSQL_PASSWORD" \
  mysql:8.0 >/dev/null

echo "--- waiting for mysql to accept connections ---"
for i in $(seq 1 30); do
    if docker exec "$MYSQL_NAME" mysqladmin ping -h localhost -u root -p"$MYSQL_PASSWORD" --silent 2>/dev/null; then
        echo "mysql up after ${i}s"
        break
    fi
    sleep 1
done

echo "--- gemma-rest ---"
docker run -d --name "$APP_NAME" --network "$NET" \
  -p 8080:8080 \
  -e GEMMA_DB_HOST="$MYSQL_NAME" \
  -e GEMMA_DB_PORT=3306 \
  -e GEMMA_DB_NAME=gemd \
  -e GEMMA_DB_USER=gemmauser \
  -e GEMMA_DB_PASSWORD="$MYSQL_PASSWORD" \
  -e GEMMA_AGENT_PASSWORD=smokeagent \
  -e GEMMA_RUNAS_PASSWORD=smokerunas \
  -e GEMMA_ANONYMOUSAUTH_KEY=smokeanon \
  -e GEMMA_HOSTURL=http://localhost:8080 \
  -e GEMMA_ADMIN_EMAIL=admin@example.com \
  -e GEMMA_NOREPLY_EMAIL=noreply@example.com \
  -e GEMMA_SUPPORT_EMAIL=support@example.com \
  -e MAIL_HOST=localhost \
  "$IMAGE" >/dev/null

echo "--- waiting for /rest/v2/health (up to 180s) ---"
ok=0
for i in $(seq 1 36); do
    code=$(curl -s -o /tmp/gemma-smoke.out -w "%{http_code}" "http://localhost:8080/rest/v2/health" || true)
    if [[ "$code" == "200" || "$code" == "503" ]]; then
        # 200 = healthy, 503 = up but some indicator down -- both prove
        # Tomcat is serving the dispatcher.
        echo "HTTP $code from /rest/v2/health after ${i}*5s"
        cat /tmp/gemma-smoke.out
        echo
        ok=1
        break
    fi
    sleep 5
done

if [[ "$ok" != "1" ]]; then
    echo "FAIL: /rest/v2/health never came up. Container log tail:"
    docker logs --tail 80 "$APP_NAME"
    exit 1
fi

echo "--- /rest/v2/datasets?limit=1 (will likely 500 against empty schema) ---"
curl -i "http://localhost:8080/rest/v2/datasets?limit=1" || true

echo "--- DONE ---"
