# Container image recce -- gemma-rest WAR on Tomcat 10.1

Phase 3 cloud-readiness. This recce documents the first working `Dockerfile`
for the Gemma codebase, the env-var surface required to boot it, and the
known gaps before the image is production-deployable.

Sibling docs (read these first if you want background):

- `CONTAINER_RECCE.md` -- broader strategy: Dockerfile vs jib, k8s manifests,
  observability, open questions for Paul. This recce is the *narrow* "ship a
  Dockerfile" execution of that plan's Path A.
- `CONTAINER_CONFIG.md` -- the `GEMMA_FOO_BAR` -> `gemma.foo.bar` env-var
  contract, the property-resolution priority order, and the example
  `docker run` invocation that this Dockerfile is designed around.
- `GEMMA_REST_STANDALONE_RECCE.md` -- the longer-term plan to move
  `gemma-rest` to an embedded Tomcat (executable JAR), which would replace
  this WAR-on-Tomcat image with a self-contained one.
- `CONFIG_AUDIT.md` -- the full inventory of `@Value` injection sites and
  Spring profiles. HIGH issue #3 (dev-profile fallback) is the only known
  config blocker for production deployment of this image.

Baseline commit: `1cc7560f07` on `phase2-acl-migrate`.

---

## 1. What works today

Two recent commits unblock containerization for the first time in the
codebase's history:

| Commit | Effect |
|---|---|
| `c02334416e` | `gemma-rest-war` Maven profile produces `gemma-rest.war` (~106 MB). |
| `23be090ba2` | `SettingsConfig` no longer throws when `Gemma.properties` is missing; env vars supply config; `gemma.appdata.home` defaults to `${java.io.tmpdir}/gemmaData`. |

**Verified locally (this branch):**

```
$ JAVA_HOME=$(/usr/libexec/java_home -v 17) \
    mvn clean package -pl gemma-rest -am -P gemma-rest-war -DskipTests
...
[INFO] Building war: .../gemma-rest/target/gemma-rest.war
[INFO] BUILD SUCCESS
[INFO] Total time:  01:08 min

$ ls -lh gemma-rest/target/gemma-rest.war
-rw-r--r--  1 pzoot  staff   106M ... gemma-rest.war
```

The Dockerfile re-runs that exact `mvn -P gemma-rest-war` invocation inside
a `maven:3.9-eclipse-temurin-17` build stage, then copies the WAR onto
`tomcat:10.1-jdk17-temurin` at `webapps/ROOT.war`.

### Why ROOT.war (not gemma-rest.war)

Deploying as `ROOT.war` lands every REST endpoint at `/rest/v2/...` --
matching `HealthWebService` at `/rest/v2/health`, every `@Path` in
`gemma-rest/`, the production hostname layout, and the `web.xml`
`<url-pattern>/rest/v2/*</url-pattern>` (`gemma-rest/src/main/webapp/WEB-INF/web.xml:140`).

The alternative -- copying as `webapps/gemma-rest.war` -- would expose
`/gemma-rest/rest/v2/...` and require every in-tree client (and
`gemma-curation-ui`) to learn the extra `/gemma-rest` prefix. Only useful
if multiple WARs share one Tomcat, which is not the target deployment. The
Dockerfile is one COPY-target edit away from that alternative if a future
co-deployment scenario demands it.

### Why Tomcat 10.1 (not 9.x)

`gemma-rest` is on the jakarta.servlet 6 baseline (the servlet6 audit
pre-work has already landed -- imports are `jakarta.*`, not `javax.*`).
Tomcat 9 ships `javax.servlet 4` and will refuse to load the WAR; Tomcat
10.1 is the jakarta-namespace minimum.

---

## 2. Required env vars to boot

Translation rule (per `CONTAINER_CONFIG.md`):
`GEMMA_FOO_BAR` -> `gemma.foo.bar`. Mail variables follow `MAIL_HOST` ->
`mail.host`.

### Hard requirements (no default value; startup fails without them)

These are the properties whose `default.properties` value is `XXXXXX` /
`XXXXXXXX` -- placeholder substitution will leak the literal `XXXXXX` into
the runtime config and break things downstream (see "Known gaps" #4).

| Env var | Gemma key | Source |
|---|---|---|
| `GEMMA_DB_PASSWORD` | `gemma.db.password` | `default.properties:59` |
| `GEMMA_AGENT_PASSWORD` | `gemma.agent.password` | `default.properties:72` |
| `GEMMA_RUNAS_PASSWORD` | `gemma.runas.password` | `default.properties:66` |
| `GEMMA_ANONYMOUSAUTH_KEY` | `gemma.anonymousAuth.key` | `default.properties:68` |

### Soft requirements (have weak defaults; production must override)

| Env var | Gemma key | Default | Why override |
|---|---|---|---|
| `GEMMA_DB_HOST` | `gemma.db.host` | `localhost` | Container localhost is the container itself. |
| `GEMMA_DB_PORT` | `gemma.db.port` | `3306` | Usually fine; override if MySQL on a non-standard port. |
| `GEMMA_DB_NAME` | `gemma.db.name` | `gemd` | Override for `gemdtest` / staging. |
| `GEMMA_DB_USER` | `gemma.db.user` | `gemmauser` | Override per environment. |
| `GEMMA_HOSTURL` | `gemma.hosturl` | `https://gemma.msl.ubc.ca` | Public URL leaks into generated links and CORS. |
| `GEMMA_ADMIN_EMAIL` | `gemma.admin.email` | `gemma@chibi.msl.ubc.ca` | Notifications. |
| `GEMMA_NOREPLY_EMAIL` | `gemma.noreply.email` | `pavlab-apps@msl.ubc.ca` | Notifications. |
| `GEMMA_SUPPORT_EMAIL` | `gemma.support.email` | `pavlab-support@msl.ubc.ca` | Notifications. |
| `GEMMA_APPDATA_HOME` | `gemma.appdata.home` | `${java.io.tmpdir}/gemmaData` | **Critical**: the JVM tmpdir is ephemeral. Mount a volume and point this at it. The image pre-sets `/data/gemma` (see Dockerfile). |
| `MAIL_HOST` | `mail.host` | `localhost` | No MTA inside the container; point at a real SMTP relay. |

### Optional / advanced

| Env var | Effect |
|---|---|
| `GEMMA_DB_URL` | Override the JDBC URL entirely (default composes from `host`/`port`/`name`). |
| `GEMMA_DB_MAXIMUMPOOLSIZE` | HikariCP max pool size; default 10. |
| `LOAD_ONTOLOGIES` -> `load.ontologies` | Default `false`. Set `true` to warm ontologies at boot (adds 60-120s to cold start). |
| `CATALINA_OPTS` | Override the JVM tuning the image pre-sets (`MaxRAMPercentage=75`, `ExitOnOutOfMemoryError`). |

The Dockerfile also pre-sets `-Dspring.profiles.active=production` inside
`CATALINA_OPTS` to dodge the `dev`-profile fallback documented in
`CONFIG_AUDIT.md` HIGH #3. **Do not unset this.**

A concrete `docker run --env-file` template lives at
`docker/env.smoke.example`.

---

## 3. External services needed

| Service | Required? | Port | Notes |
|---|---|---|---|
| MySQL 8 | yes | 3306 | Database `gemd` with Flyway-applied schema. The image does NOT bundle Flyway -- the DB must be pre-migrated. (Path forward: run `gemma-cli` Flyway commands once, or wire a separate Flyway init container; see "Known gaps".) |
| SMTP relay | no, but recommended | 25 | Required for password-reset / admin-notification emails. Default `mail.host=localhost` will silently fail if no MTA is reachable. |
| GoldenPath MySQL | optional | 3306 | Only needed for the sequence-annotation features. Not on the smoke path. |
| BLAT (`gfServer`) | optional | various | Required only if doing sequence searches. Not on the smoke path. |
| `RepeatMasker` / `apt-probeset-summarize` / Rserve | optional | n/a | All preprocessing-side tools. Not needed for the REST surface. |

Notably absent: **no Redis, no RabbitMQ/AMQP, no Memcached, no Elasticsearch.**
`grep -i 'rabbit\|redis\|amqp\|memcache'` against `default.properties` and
`project.properties` returns nothing. The L2 cache is in-JVM (ehcache3 /
JCache), the search index is local-FS (under `gemma.appdata.home`), and
the message-passing surface is JMS over an internal `ActiveMQ`-style queue
(also in-JVM in the current architecture).

This makes the runtime minimum: one MySQL + the gemma-rest container.

---

## 4. First-boot smoke sequence

`docker/run-smoke.sh` automates the steps below. The agent did NOT execute
them (no Docker daemon access in this environment); the user runs them
interactively.

```bash
# (1) Build the image. Multi-stage: maven build, tomcat runtime.
docker build -t gemma-rest:dev .

# (2) Stand up a stub MySQL on a private network.
docker network create gemma-smoke
docker run -d --name gemma-smoke-mysql --network gemma-smoke \
  -e MYSQL_ROOT_PASSWORD=smokepw -e MYSQL_DATABASE=gemd \
  -e MYSQL_USER=gemmauser -e MYSQL_PASSWORD=smokepw \
  mysql:8.0

# (3) Run gemma-rest pointed at it.
docker run -d --name gemma-smoke-app --network gemma-smoke -p 8080:8080 \
  -e GEMMA_DB_HOST=gemma-smoke-mysql -e GEMMA_DB_NAME=gemd \
  -e GEMMA_DB_USER=gemmauser -e GEMMA_DB_PASSWORD=smokepw \
  -e GEMMA_AGENT_PASSWORD=smoke -e GEMMA_RUNAS_PASSWORD=smoke \
  -e GEMMA_ANONYMOUSAUTH_KEY=smoke \
  -e GEMMA_HOSTURL=http://localhost:8080 \
  -e GEMMA_ADMIN_EMAIL=admin@example.com \
  -e GEMMA_NOREPLY_EMAIL=noreply@example.com \
  -e GEMMA_SUPPORT_EMAIL=support@example.com \
  gemma-rest:dev

# (4) Wait for the dispatcher to come up, hit health.
curl -fsS http://localhost:8080/rest/v2/health
# Expect: 200 with a JSON payload listing every HealthIndicator's status,
# OR 503 if the DB schema is unmigrated (Tomcat is up but the DataSource
# indicator reports DOWN). 503 still proves the WAR loaded and the
# dispatcher is serving; that's the smoke threshold.

# (5) Hit a real endpoint.
curl -i http://localhost:8080/rest/v2/datasets?limit=1
# Expect: 500 against an empty/un-migrated DB (no schema -> Hibernate
# fails the query). Against a Flyway-migrated gemd dump: 200 with a JSON
# response envelope wrapping zero or one dataset.

# (6) Tear down.
docker rm -f gemma-smoke-app gemma-smoke-mysql
docker network rm gemma-smoke
```

The HEALTHCHECK in the Dockerfile uses `/rest/v2/datasets?limit=1` per
the task brief. Liveness ought to migrate to `/rest/v2/health` once the
schema-migration story (gap #1 below) is sorted -- that endpoint
distinguishes "tomcat is up" from "tomcat is up and the app is healthy",
which the datasets probe cannot.

---

## 5. Known gaps before production-deployable

### Gap 1: Flyway migrations are NOT in-container

The image bundles only the REST WAR. Database schema management lives in
`gemma-cli` (Flyway commands) and `gemma-core/src/main/resources/db/migration`
(the SQL itself). Production deployment needs one of:

- **(a)** A separate `gemma-cli` image (or sidecar) that runs
  `flyway migrate` against the target DB before the REST container starts.
  This is the cleanest k8s pattern (init container).
- **(b)** A startup hook inside the REST container that runs Flyway as the
  first thing in `ContextLoaderListener`. Couples lifecycles -- not
  recommended.
- **(c)** Ops-driven: humans run Flyway out-of-band before deploy. Works
  today, doesn't scale.

Action: add a second Maven profile / Dockerfile target that produces a
`gemma-cli` image; wire (a) as the canonical pattern.

### Gap 2: `dev` Spring profile fallback (`CONFIG_AUDIT.md` HIGH #3)

`SpringContextUtils` falls back to the `dev` profile when no explicit
profile is set, and the `dev` profile shares the production `dataSource`
bean -- meaning "I forgot to set the profile" silently lands on production
config. The Dockerfile pre-sets
`-Dspring.profiles.active=production` in `CATALINA_OPTS`, which papers
over the bug for this image, but the underlying fallback is still a
landmine for anyone who overrides `CATALINA_OPTS` and forgets the flag.

Action: fix the fallback in `SpringContextUtils` to fail-fast if no
explicit profile is provided (or, better, to default to `production`
rather than `dev`). Tracked in `CONFIG_AUDIT.md`.

### Gap 3: `XXXXXX`-sentinel passwords substitute literally

`default.properties` ships `gemma.db.password=XXXXXX` (and three other
similar sentinels). If a deployer forgets to set the env var, Spring's
`@Value` does NOT fail -- it substitutes the literal `XXXXXX` into the
JDBC URL / security tokens, and the failure surfaces deep into the
runtime as a confusing auth error.

Action: add a startup hook that detects `XXXXXX` in any resolved property
value and fails fast with a clear "you forgot to set GEMMA_FOO_BAR"
message. Noted as a follow-up in `CONTAINER_CONFIG.md`.

### Gap 4: `/rest/v2/datasets` requires a migrated schema for HEALTHCHECK

The current HEALTHCHECK pings `/rest/v2/datasets?limit=1`, which requires
a fully Flyway-migrated DB. Against an empty stub MySQL the probe
returns 500 and the container shows up as unhealthy -- correct semantics
but not useful as a smoke signal. Once gap #1 lands, switch the probe to
`/rest/v2/health`, which decomposes into per-indicator status.

### Gap 5: `gemma.appdata.home` on `/data/gemma` -- ownership, no init

The Dockerfile creates `/data/gemma` owned by `gemma:gemma` (UID/GID 1000)
and declares `VOLUME ["/data/gemma"]`. If the orchestrator mounts a
host-path or PVC over that path, the mount inherits the host's ownership
-- which is frequently root or some other UID. Either:
- mount with `:Z` / `fsGroup: 1000` (k8s) so the volume is chowned, OR
- run an init container that chowns the mount, OR
- accept that the runtime user must match the volume owner externally.

The image cannot solve this on its own; docs need to flag it.

### Gap 6: No image-build automation

There is no CI workflow that builds + pushes this image. The Dockerfile
sits in-repo waiting for a `Build & Push Container` GitHub Action (or
equivalent). Tag policy, registry, signing -- all open questions
documented in `CONTAINER_RECCE.md` section 8.

### Gap 7: No log-routing to stdout

Tomcat logs to `$CATALINA_HOME/logs/` by default; Gemma's own log4j config
writes to files under `gemma.log.dir`. In a containerized world both
should stream to stdout/stderr so the orchestrator can pick them up. The
image inherits Tomcat's default catalina.out -> stdout, but the
Gemma-specific log4j config needs a `ConsoleAppender` profile to match.
Tracked in `LOGGING_AUDIT.md`.

### Gap 8: gemma-web is NOT in this image

The image deploys only `gemma-rest.war`. The legacy `gemma-web` Spring
MVC frontend is "walking dead" per project MEMORY -- being replaced by
`gemma-curation-ui` -- so this is intentional, not a regression. But
anything currently served by `gemma-web` (the Wro4j-built JS bundles,
the JSP-rendered pages) is missing from the container surface until
`gemma-curation-ui` ships and is wired into the deploy.

---

## 6. Phase-N roadmap

What needs to happen before this image is production-deployable, in
dependency order:

| Phase | Work | Blocker for |
|---|---|---|
| 3.0 (now) | This commit: Dockerfile + recce. WAR build verified. | Manual smoke runs. |
| 3.1 | Fix `SpringContextUtils` profile fallback (`CONFIG_AUDIT.md` HIGH #3). | Removing the `-Dspring.profiles.active=production` papered-over hack. |
| 3.2 | Add `XXXXXX`-sentinel fail-fast detection. | Reliable error messages on misconfig. |
| 3.3 | Companion `gemma-cli` container image (Flyway runner). | Init-container schema migrations in k8s. |
| 3.4 | CI workflow: build, push, sign image. Registry / tag policy decisions per `CONTAINER_RECCE.md`. | Reproducible image artifacts; release cadence. |
| 3.5 | Switch HEALTHCHECK to `/rest/v2/health` (after 3.3 makes DB readiness reliable). | Production-grade liveness / readiness probes. |
| 3.6 | Log4j console-appender profile; route Gemma logs to stdout. | Container log-aggregation. |
| 3.7 | k8s manifest set (Deployment + Service + Ingress + PVC + ConfigMap + Secret), per `CONTAINER_RECCE.md` section 6. | Cluster deployment. |
| 3.8 | (Optional, longer horizon) Migrate to embedded Tomcat / executable JAR per `GEMMA_REST_STANDALONE_RECCE.md`. Replaces the WAR-on-Tomcat base with `eclipse-temurin:17-jre-jammy`. | Smaller image, faster cold-start, no servlet-container layer to patch. |
| 3.9 | `gemma-curation-ui` integration -- separate container, sibling Service in the same Ingress. | Replacing the gemma-web frontend that this image deliberately omits. |

Each row is a discrete worktree-sized chunk of work; none of them are
blocked on this commit landing.

---

## Recap

- Multi-stage Dockerfile: `maven:3.9-eclipse-temurin-17` build,
  `tomcat:10.1-jdk17-temurin` runtime, ROOT context, non-root user,
  `/data/gemma` volume, `MaxRAMPercentage=75` JVM tuning,
  `spring.profiles.active=production`, HEALTHCHECK on
  `/rest/v2/datasets?limit=1`.
- 4 hard-required env vars, 10 soft-required, 4 optional. All map via
  the `GEMMA_FOO_BAR` -> `gemma.foo.bar` rule from
  `CONTAINER_CONFIG.md`.
- One external service required (MySQL 3306). SMTP optional but
  recommended. Notably NO Redis / RabbitMQ / Elasticsearch.
- WAR build verified locally: `mvn -P gemma-rest-war clean package -pl
  gemma-rest -am -DskipTests` -> SUCCESS, 106 MB WAR, ~68 s on
  this hardware.
- Image build NOT executed in-agent (no daemon access). User can run
  `docker build -t gemma-rest:dev .` followed by `docker/run-smoke.sh`.
- 8 known gaps before production-deployable; the biggest is Flyway
  schema migrations, which need a sibling `gemma-cli` image.
