# Container / Docker readiness recce

Phase 3 infrastructure modernization. Doc-only; no code changes. Baseline
commit: `08e760bdaf` on `phase2-acl-migrate`.

This recce maps the path from "no runtime container infra" to a packaged,
deployable Gemma image suitable for cloud / k8s. It pairs with the
`gemma-rest` standalone roadmap (unmerged `worktree-gemma-rest-standalone-recce`,
commit `e634e0009e`, `GEMMA_REST_STANDALONE_ROADMAP.md`) whose Phase 2
introduces embedded Tomcat — the natural prerequisite for an
executable-jar image. Until that lands, the immediate path is
war-on-Tomcat.

---

## 1. Current state

`find` for `Dockerfile*`, `docker-compose*`, `Chart.yaml`, `values.yaml`,
`kustomization.yaml`, `.dockerignore` and `grep` for
`jib-maven-plugin` / `docker-maven-plugin` in `pom.xml` files
turned up exactly one artifact:

- `docker-compose.yml` (root) — a single-service test fixture:
  `mysql:5.7` bound to host port `3307`, database `gemdtest`. Used by
  the test suite (matches MEMORY entry "Local test database
  connection"). NOT a runtime composition; no `gemma` service, no
  app-data volume, no reverse proxy.

No `Dockerfile`, no Maven container plugin, no k8s manifests, no Helm
chart, no `.dockerignore`. The project has never been packaged as a
container image.

### Module packaging (baseline)

- root `pom.xml` -> `<packaging>pom</packaging>`
- `gemma-web/pom.xml` -> `<packaging>war</packaging>` (finalName
  `Gemma`, produces `Gemma.war`)
- `gemma-rest/pom.xml` -> no `<packaging>` line (defaults to `jar`)
- `gemma-cli/pom.xml` -> jar (executable via `gemma-cli` launcher)

The `gemma-rest-war` profile referenced in the task brief is NOT on
baseline. It lives on the unmerged `worktree-gemma-rest-bootstrap`
branch (commit `13501bf4f8`). Container plan must either depend on
that profile landing OR target `gemma-web/Gemma.war` first.

### Build toolchain

- `maven.compiler.release=17` (root pom line 1006). JDK17.
- Convention (per project MEMORY): amazon-corretto JDK17.
- Tomcat target: 10.1.x (jakarta.servlet 6 — see `agent-servlet6-audit`,
  `agent-jstl-jakarta` parallel work).

### Configuration injection

`gemma-core/src/main/java/ubic/gemma/core/config/SettingsConfig.java`
resolves user config in this priority order:

1. `-Dgemma.config=<path>` system property (highest priority).
2. `$CATALINA_BASE/Gemma.properties` (servlet-container convention).
3. `$HOME/Gemma.properties` (CLI / dev convention).

For container deployments, `-Dgemma.config=/etc/gemma/Gemma.properties`
is the cleanest hook. No code change required — the loader already
honours an arbitrary filesystem path.

### App-data directory

`gemma.appdata.home` default: `/var/tmp/gemmaData`
(`gemma-core/src/main/resources/default.properties:9`). Multiple
sub-paths derive from it (`gemma.download.path`, `gemma.analysis.dir`,
`gemma.search.dir`, `gemma.cache.dir`, `gemma.scratch.dir`,
`gemma.fastq.headers.dir`, `gemma.gene2cs.path`,
`gemma.staticAssetServer.internal.logFile`). All must be on a mounted
volume in production — losing this dir loses cached analysis output,
search indices, and queued data files.

`gemma.log.dir` defaults to `.` and is normally overridden via system
property — in a container, `/var/log/gemma` is the sensible mount.

---

## 2. Phase 2 prerequisites and decision

Two viable paths to a runtime image:

| Path | When | Image base | Image type |
|---|---|---|---|
| A. war-on-Tomcat | Now (baseline ready) | `tomcat:10.1-jdk17-temurin` | external servlet container |
| B. Executable-jar (Spring Boot embedded Tomcat) | After standalone Phase 2 | `eclipse-temurin:17-jre-jammy` | self-contained |

**Recommendation: pursue Path A first.** It works against today's
baseline, validates the operational story (volume mounts, config
injection, health probes, log routing) independent of the Spring
Boot migration, and produces a working image weeks before standalone
lands. Path B is the eventual target — leaner, faster cold-start, no
servlet-container layer to patch — but should not gate container
work.

What we package in Path A:
- `gemma-web/target/Gemma.war` -> `gemma.example.com/` (the web
  frontend) OR
- `gemma-rest/target/gemma-rest.war` (once `gemma-rest-war` profile
  lands from `worktree-gemma-rest-bootstrap`) -> `gemma.example.com/rest/`

Two separate images is the cleanest model: independent scaling,
independent rollout, independent health surface. A single Tomcat with
both wars co-deployed is possible but couples release cycles.

---

## 3. Dockerfile design (Path A — war-on-Tomcat)

Single-stage, since the war is built outside the image (by Maven on
the build host or in CI). Sketch:

```dockerfile
# syntax=docker/dockerfile:1.7
FROM tomcat:10.1-jdk17-temurin AS gemma-rest

# Strip the default ROOT app and examples; we don't want them exposed.
RUN rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist

# Volume mount points (declared so `docker volume inspect` is honest;
# the actual mount comes from compose / k8s).
VOLUME ["/var/lib/gemma/appdata", "/var/log/gemma"]

# Config: caller must mount /etc/gemma/Gemma.properties (read-only).
ENV CATALINA_OPTS="\
    -Dgemma.config=/etc/gemma/Gemma.properties \
    -Dgemma.log.dir=/var/log/gemma \
    -Dgemma.appdata.home=/var/lib/gemma/appdata \
    -Djava.security.egd=file:/dev/./urandom \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError"

# The war is the deployment unit.
COPY --chown=root:root gemma-rest.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# Health probe lands once actuator-impl ships /rest/v2/health.
# Until then: HEAD / returns 200 once webapp is up (less precise).
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
    CMD curl -fsS http://localhost:8080/rest/v2/health || exit 1

# tomcat:10.1-jdk17-temurin defaults to catalina.sh run; keep it.
```

Notes:

- `tomcat:10.1-jdk17-temurin` is the right base for Jakarta EE 10 /
  Servlet 6 (after `agent-servlet6-audit` lands). If baseline Tomcat
  is still 9.x at image-build time, use `tomcat:9.0-jdk17-temurin`
  and bump in lockstep with the servlet6 cutover.
- `-XX:MaxRAMPercentage` (NOT `-Xmx`) so the JVM right-sizes itself
  to the container's cgroup memory limit. JDK17 honours this natively.
- `ExitOnOutOfMemoryError` lets the orchestrator restart on OOM
  instead of leaving a zombie tomcat with a poisoned heap.
- Run as non-root in a hardened build (`USER 1000:1000` + chown the
  webapps and work dirs); deferred to a security pass after the basic
  image is working.

`.dockerignore` at repo root (must exist before the build):

```
**/target
**/.git
**/.idea
**/*.iml
**/node_modules
docs/
.claude/
**/*.md
```

---

## 4. jib-maven-plugin vs Dockerfile

**Verdict: use jib-maven-plugin.** Recommended even though Path A
above shows a Dockerfile.

Why jib wins here:

- **No Docker daemon required.** Build images on CI runners that
  don't (or can't) run dockerd.
- **Automatic layering.** jib splits the image into: base, deps,
  resources, classes — keyed by their mtimes. Iterating on Java code
  invalidates only the `classes` layer; deps stay cached. For a
  project of Gemma's size this turns a 2-minute push into a 5-second
  push.
- **Reproducible.** jib zeroes timestamps and sorts file entries, so
  the same source produces the same image digest. Matters for
  supply-chain attestations later.
- **Native to Maven.** Configured in `pom.xml`, runs in the existing
  `mvn package` flow, picks up the version from the parent pom
  automatically.

Sketch for `gemma-rest/pom.xml` (once `gemma-rest-war` profile lands):

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.4</version>
    <configuration>
        <from>
            <image>tomcat:10.1-jdk17-temurin</image>
        </from>
        <to>
            <image>${gemma.image.registry}/gemma-rest:${project.version}</image>
            <tags>
                <tag>latest</tag>
                <tag>${git.commit.id.abbrev}</tag>
            </tags>
        </to>
        <container>
            <ports><port>8080</port></ports>
            <jvmFlags>
                <jvmFlag>-Dgemma.config=/etc/gemma/Gemma.properties</jvmFlag>
                <jvmFlag>-Dgemma.appdata.home=/var/lib/gemma/appdata</jvmFlag>
                <jvmFlag>-Dgemma.log.dir=/var/log/gemma</jvmFlag>
                <jvmFlag>-XX:MaxRAMPercentage=75.0</jvmFlag>
            </jvmFlags>
            <volumes>
                <volume>/var/lib/gemma/appdata</volume>
                <volume>/var/log/gemma</volume>
            </volumes>
            <appRoot>/usr/local/tomcat/webapps/ROOT</appRoot>
        </container>
    </configuration>
</plugin>
```

jib's `war` packaging mode auto-detects `<packaging>war</packaging>`
and explodes the war into `appRoot`. Faster cold-start than mounting
the raw war (Tomcat skips its unpack step).

Caveat: if the team wants a tooling-light "I can read the Dockerfile
and trace every byte" path (e.g. for an air-gapped customer), keep
the Dockerfile from section 3 as the secondary route. jib is the
primary; Dockerfile is the backup.

---

## 5. docker-compose for local dev

Replaces the test-only `docker-compose.yml` with a runtime
composition. The existing test fixture stays useful — keep it as
`docker-compose.test.yml` and add a runtime sibling.

`docker-compose.yml` (new, runtime):

```yaml
services:
  gemma-rest:
    image: gemma-rest:dev
    build: .
    ports:
      - "8080:8080"
    volumes:
      - appdata:/var/lib/gemma/appdata
      - ./conf/Gemma.properties:/etc/gemma/Gemma.properties:ro
      - logs:/var/log/gemma
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:8080/rest/v2/health"]
      interval: 30s
      timeout: 5s
      start_period: 120s
      retries: 3

  mysql:
    image: mysql:8.0   # or mariadb:10.11 to match prod
    environment:
      MYSQL_DATABASE: gemd
      MYSQL_USER: gemmauser
      MYSQL_PASSWORD_FILE: /run/secrets/db_password
      MYSQL_ROOT_PASSWORD_FILE: /run/secrets/db_root_password
    volumes:
      - dbdata:/var/lib/mysql
    secrets:
      - db_password
      - db_root_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 3s
      retries: 5

  prometheus:
    image: prom/prometheus:latest
    profiles: ["observability"]
    volumes:
      - ./conf/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    profiles: ["observability"]
    ports:
      - "3000:3000"

volumes:
  appdata: {}
  dbdata: {}
  logs: {}

secrets:
  db_password:
    file: ./conf/db_password.txt
  db_root_password:
    file: ./conf/db_root_password.txt
```

Notes:
- `profiles: ["observability"]` keeps prom/grafana opt-in
  (`docker compose --profile observability up`).
- DB image: keep mysql 8 here for parity with the existing test
  fixture; if prod is MariaDB swap to `mariadb:10.11`. (Open question
  below.)
- Secrets via file mounts, not env vars — matches the keychain
  preference and stops `docker inspect` from leaking creds.
- Existing test-only compose moves to `docker-compose.test.yml`;
  the test suite gets pointed at the new path. That's a separate
  small commit.

---

## 6. k8s manifests outline

Minimum viable manifest set, in `deploy/k8s/`:

- `namespace.yaml` — dedicated namespace `gemma`.
- `configmap.yaml` — `Gemma.properties` content, mounted to
  `/etc/gemma/Gemma.properties`. Keep secrets OUT — pure config only.
- `secret.yaml` — DB password, any API keys. Pulled from a real
  secret backend (Vault / sealed-secrets / k8s External Secrets) at
  apply time, never committed.
- `pvc.yaml` — `gemma-appdata` PersistentVolumeClaim. Size sized to
  current prod `/var/lib/gemma/appdata` (open question: how big? See
  below). ReadWriteOnce is fine for a single-pod deployment; if we
  ever go multi-replica, ReadWriteMany backed by NFS / CephFS is
  required because the app caches index files there.
- `deployment.yaml` — 1 replica initially; resource requests/limits
  matched to current prod JVM footprint. `volumeMounts` for appdata,
  config, logs. `env` for `CATALINA_OPTS`. Pod-level
  `securityContext` with `runAsNonRoot: true`,
  `readOnlyRootFilesystem: true` (Tomcat needs `/usr/local/tomcat/work`
  and `/usr/local/tomcat/temp` writeable — provide as `emptyDir`
  volumes).
- `service.yaml` — ClusterIP, port 8080.
- `ingress.yaml` — TLS via cert-manager, hostname `gemma.example.com`,
  routes `/` to gemma-web service, `/rest/` to gemma-rest service.
  Ingress class TBD (open question).
- `servicemonitor.yaml` (Prometheus-Operator CRD, optional) — scrapes
  `/metrics` once the metrics-jcache-restore agent lands.

Replica strategy: stick to 1 replica until either (a) the
search-index cache is moved out of the local FS, or (b) the appdata
PVC moves to RWX storage. The current architecture assumes
single-node FS semantics for `gemma.appdata.home`.

---

## 7. Health / liveness probe wiring

The actuator-impl agent (parallel work, branch `worktree-actuator-impl`)
is introducing `/rest/v2/health` and likely `/rest/v2/health/live` and
`/rest/v2/health/ready` (Spring Boot 3 convention even when not
running Boot). Container probes once that lands:

| Probe | Path | Failure semantics |
|---|---|---|
| readiness | `/rest/v2/health/ready` | Pull pod from Service endpoints; don't kill |
| liveness | `/rest/v2/health/live` | Kill pod and restart |
| startup | `/rest/v2/health/live` | Long `failureThreshold` for warmup |

`startupProbe` is critical for Gemma — ontology loading
(`load.ontologies=true`) can take 60-120s on first boot, and the JVM
warming the L2 cache + search index can push readiness back several
minutes. Without a startupProbe, the liveness check kills the pod
mid-warmup and the pod restarts forever.

Recommended initial values (tune from real prod startup metrics):

```yaml
startupProbe:
  httpGet:
    path: /rest/v2/health/live
    port: 8080
  periodSeconds: 10
  failureThreshold: 30      # 5 minutes of warmup
livenessProbe:
  httpGet:
    path: /rest/v2/health/live
    port: 8080
  periodSeconds: 30
  failureThreshold: 3
readinessProbe:
  httpGet:
    path: /rest/v2/health/ready
    port: 8080
  periodSeconds: 10
  failureThreshold: 3
```

Until actuator-impl ships:
- liveness can be `httpGet /` (returns 200 once Tomcat is up; coarse
  but functional)
- readiness should be omitted (we don't have a real readiness signal)
- startupProbe should be `httpGet /` with a generous threshold

The HEALTHCHECK clause in the Dockerfile (section 3) mirrors this and
must be updated in lockstep.

---

## 8. Open questions for Paul

1. **Container registry.** Where do images push? Options: GitHub
   Container Registry (free, public/private, ties to repo perms),
   ghcr.io for the org, AWS ECR, GCR/GAR, a self-hosted Harbor. The
   choice gates secrets in CI and the image-pull-secret in k8s.

2. **Image signing.** cosign / Sigstore? Notary v2? The supply-chain
   story matters once images are pushed to a public registry. Easy
   to bolt on later but cheaper to wire from the first push.

3. **Ingress class.** nginx? Traefik? An institution-managed ingress
   (UBC / NRC infra)? Affects the ingress.yaml annotations and the
   TLS issuer config.

4. **Secret backend.** Plain k8s Secrets are baseline; better
   options are sealed-secrets (Bitnami), External Secrets Operator
   talking to Vault/AWS-SM/GCP-SM, or SOPS-encrypted manifests in
   git. Pick one before writing the production deployment manifests.

5. **Log aggregation.** Where do `/var/log/gemma/*` and stdout end
   up? Loki + Promtail? ELK? An institution-managed sink? Affects
   whether we set `gemma.log.dir` at all in k8s, or just write to
   stdout and let the orchestrator do the routing.

6. **Database.** Existing test fixture uses `mysql:5.7`; prod runs
   `mysql 8` per the Flyway baseline work. Are we standardizing on
   MySQL 8 or moving to MariaDB? The compose / k8s DB image needs a
   definitive answer.

7. **App-data sizing.** What's the current size of
   `/var/lib/gemma/appdata` in prod? Drives the PVC request and the
   storage-class choice (SSD vs spinning rust).

8. **Multi-replica timeline.** Is horizontal scaling on the roadmap?
   If yes, search indices and caches need to move out of the local
   FS (Elasticsearch / Redis); that's a separate architectural
   stream. If no, the single-replica + ReadWriteOnce model above is
   fine.

9. **JDK base image policy.** amazon-corretto JDK17 per MEMORY, but
   the canonical Tomcat image ships with eclipse-temurin. Do we use
   `tomcat:10.1-jdk17-temurin` (off the shelf) or roll our own
   `tomcat-on-corretto`? Corretto is preferred for parity with
   developer workstations but Temurin is the lower-friction default.

10. **CI builder.** Where does `mvn package + jib:build` actually
    run? GitHub Actions? Jenkins? The choice gates the image-push
    credentials, the cache layout, and whether we need a separate
    `dockerfile`-based fallback for environments without jib's network
    posture (jib pushes directly to a registry rather than building
    locally).

11. **Telemetry endpoint.** When the actuator-impl agent ships
    `/metrics`, do we expose it on the same 8080 port or a separate
    management port (Spring Boot convention: 8081)? Affects the
    Service and any Prometheus-Operator ServiceMonitor config.

12. **Image tag policy.** `:latest` + `:${version}` + `:${git-sha}`
    is the obvious triple but the team may want only immutable tags
    in prod (no `:latest`). Confirm before wiring CI tag-push logic.

---

## Recap

- Current container infra: NONE (the existing `docker-compose.yml` is
  test-DB only).
- Immediate path: war-on-Tomcat (Path A) using `gemma-web/Gemma.war`
  today, switching to `gemma-rest/gemma-rest.war` once the
  `gemma-rest-war` profile lands.
- Build automation: jib-maven-plugin primary, hand-rolled Dockerfile
  secondary.
- Local dev: extended `docker-compose.yml` with gemma + mysql +
  optional prom/grafana profile; current test-fixture compose
  renamed to `docker-compose.test.yml`.
- k8s: Deployment + Service + Ingress + PVC + ConfigMap + Secret;
  startupProbe required for warmup time; single-replica until
  appdata is decoupled from local FS.
- Health probes block on the actuator-impl agent landing
  `/rest/v2/health{,/live,/ready}`.
- 12 open questions for Paul before any image gets pushed.

Phase 3 verdict: container work CAN start against today's baseline
(Path A), but the most leverage comes from sequencing it AFTER (a)
the actuator agent ships health endpoints and (b) the standalone
Phase 2 ships embedded Tomcat for Path B. Until then, scaffold the
jib config and `.dockerignore` so the pipeline is one merge away
when those dependencies land.
