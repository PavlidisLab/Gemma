# syntax=docker/dockerfile:1.7
#
# Gemma REST container image -- multi-stage build.
#
# Build:    docker build -t gemma-rest:dev .
# Run:      see docker/run-smoke.sh and CONTAINER_IMAGE_RECCE.md
#
# Stage 1 (build): compile gemma-rest into a WAR using the gemma-rest-war
# Maven profile (landed in c02334416e). Output: /build/gemma-rest/target/gemma-rest.war
#
# Stage 2 (runtime): drop the WAR onto Tomcat 10.1 (jakarta.servlet 6) running
# Temurin JDK 21. Deployed at the ROOT context so REST endpoints land at
# /rest/v2/... (matching web.xml's <url-pattern>/rest/v2/*</url-pattern>).
#
# Config: SettingsConfig (23be090ba2) tolerates a missing Gemma.properties; all
# required props can be supplied via GEMMA_* env vars. See CONTAINER_CONFIG.md
# for the env-var pattern and CONTAINER_IMAGE_RECCE.md for the required set.

# ---------------------------------------------------------------------------
# Stage 1: build the WAR
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

# Copy POMs first for dependency-resolution layer caching. If the source tree
# changes but no POM moves, Maven's local repo cache survives the rebuild.
COPY pom.xml ./
COPY gemma-core/pom.xml      gemma-core/pom.xml
COPY gemma-rest/pom.xml      gemma-rest/pom.xml
COPY gemma-cli/pom.xml       gemma-cli/pom.xml

# Stage the hdf5 jar (org.hdf5group:hdf5:1.12.3 is not on Maven Central or the
# pavlab mirror; vendored in-repo at vendor/hdf5/ so CI and fresh dev clones
# build without a hand-installed host-side maven repo).
#
# Critical: the staged files land at a stable image-layer path (/opt/local-mvn-repo),
# NOT in a cache-mounted /root/.m2/repository. Cache mounts (`type=cache`) do
# not persist across builds in GHA — BuildKit caches the LAYER ("install-file
# already ran"), but the mount it wrote to is empty next time, so a later
# source-changed build runs against an empty repo and can't resolve hdf5.
# Seeding from /opt/local-mvn-repo into the cache mount at the start of each
# subsequent RUN makes the layer-cache + cache-mount combo work correctly.
COPY vendor/hdf5/hdf5-1.12.3.jar vendor/hdf5/hdf5-1.12.3.pom /opt/local-mvn-repo/org/hdf5group/hdf5/1.12.3/

# Prime the local repo. -fae so partial misses (a sibling module that's not
# wired here) don't kill the cache; we re-run the real build below. `cp -rn`
# seeds hdf5 from the image-layer stash into the cache mount idempotently.
RUN --mount=type=cache,target=/root/.m2/repository \
    cp -rn /opt/local-mvn-repo/* /root/.m2/repository/ 2>/dev/null || true && \
    mvn -B -ntp -pl gemma-rest -am dependency:go-offline -DskipTests -fae \
    || true

# Now copy sources and build the WAR.
COPY gemma-core/src   gemma-core/src
COPY gemma-rest/src   gemma-rest/src
COPY gemma-cli/src    gemma-cli/src

# Skip the git-commit-id plugin inside the build container; we don't COPY .git
# (it's 800 MB), and the gitHash field on /rest/v2/info isn't load-bearing for
# the dev image. Production deploy through the Jenkins pipeline still resolves
# .git normally and populates gitHash.
#
# Re-seed hdf5 here too — this RUN's layer cache invalidates on every source
# change, so the seeding step inside it always actually executes (the prime
# RUN above may be cached and not re-run, leaving the cache mount empty).
RUN --mount=type=cache,target=/root/.m2/repository \
    cp -rn /opt/local-mvn-repo/* /root/.m2/repository/ 2>/dev/null || true && \
    mvn -B -ntp -P gemma-rest-war clean package -pl gemma-rest,gemma-cli -am -DskipTests \
        -Dmaven.gitcommitid.skip=true

# Sanity: WAR + CLI launcher must exist before we move to the runtime stages.
RUN ls -lh /build/gemma-rest/target/gemma-rest.war \
 && ls -lh /build/gemma-cli/target/appassembler/bin/gemma-cli

# ---------------------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------------------
# Tomcat 10.1 is the jakarta.servlet 6 baseline. Tomcat 9 ships javax.servlet 4
# and will NOT load gemma-rest -- after the servlet6 cutover the war references
# jakarta.* packages exclusively.
FROM tomcat:10.1-jdk25-temurin AS runtime

# Strip the stock Tomcat sample apps; we do not want them exposed and they
# get in the way of deploying our WAR as ROOT.
RUN rm -rf "$CATALINA_HOME/webapps/"* "$CATALINA_HOME/webapps.dist"

# Deploy gemma-rest at the ROOT context.
#
# Trade-off:
#   - ROOT.war (chosen): endpoints at /rest/v2/...   -- matches HealthWebService
#     and every other @Path on the recommended public surface; mirrors the
#     production hostname layout (gemma.example.com/rest/v2/...).
#   - gemma-rest.war:    endpoints at /gemma-rest/rest/v2/...  -- preserves
#     the WAR's basename in the URL but adds an extra path segment that the
#     in-tree clients (and gemma-curation-ui) would have to learn. Only useful
#     if multiple WARs share one Tomcat, which is NOT the target deployment.
#
# To switch, change the COPY target to webapps/gemma-rest.war.
COPY --from=build /build/gemma-rest/target/gemma-rest.war \
                  $CATALINA_HOME/webapps/ROOT.war

# App-data lives outside the WAR. Mount a host path or named volume here.
# Default value of gemma.appdata.home now resolves via ${java.io.tmpdir}
# (23be090ba2), so the container will start without a mount -- but anything
# written there evaporates on restart. Production MUST mount.
VOLUME ["/data/gemma"]

# JVM and Gemma defaults. CATALINA_OPTS is the Tomcat-honoured hook.
# - MaxRAMPercentage so the JVM right-sizes to the container's cgroup memory
#   limit (do NOT set -Xmx; let cgroup memory drive sizing on k8s).
# - ExitOnOutOfMemoryError so the orchestrator restarts on OOM instead of
#   leaving a poisoned heap.
# - UseZGC: JDK 25 ZGC is generational by default. The `-XX:+ZGenerational`
#   flag was removed in JDK 24 (no-op then hard-reject); do NOT add it back.
#   ZGC needs >2GB heap to benefit; production deploys multi-GB heaps.
#   Memory observability dashboards keyed on "G1 Old Gen" need updating to
#   "ZGC Old Generation".
# - MaxRAMPercentage=75.0 sizes from the *container's* cgroup limit. Run with
#   `--memory=8g` (or your target) so the heap target is bounded; otherwise
#   the container sees the host's entire RAM and ZGC tries to map all of it,
#   blowing past /proc/sys/vm/max_map_count (host kernel default 65530).
# - gemma.appdata.home pointed at the VOLUME path above.
# - spring.profiles.active=production to avoid the SpringContextUtils 'dev'
#   profile fallback documented in CONFIG_AUDIT.md HIGH #3.
ENV GEMMA_APPDATA_HOME=/data/gemma \
    CATALINA_OPTS="-Dgemma.appdata.home=/data/gemma \
                   -Dspring.profiles.active=production \
                   -Djava.security.egd=file:/dev/./urandom \
                   -XX:MaxRAMPercentage=75.0 \
                   -XX:+ExitOnOutOfMemoryError \
                   -XX:+UseZGC \
                   --enable-native-access=ALL-UNNAMED"

# Non-root runtime user. UID/GID idempotent because newer tomcat base images
# (e.g. 10.1-jdk25-temurin / -jdk21-temurin on recent debian) already define
# GID 1000 ("ubuntu" or similar) — groupadd would fail. Use --gid only if it's
# unused; otherwise let groupadd pick the next free GID.
RUN if getent group 1000 >/dev/null; then \
        groupadd --system gemma; \
    else \
        groupadd --system --gid 1000 gemma; \
    fi \
 && if getent passwd 1000 >/dev/null; then \
        useradd  --system --gid gemma --home /home/gemma --create-home gemma; \
    else \
        useradd  --system --uid 1000 --gid gemma --home /home/gemma --create-home gemma; \
    fi \
 && mkdir -p /data/gemma \
 && chown -R gemma:gemma /data/gemma "$CATALINA_HOME"

USER gemma:gemma

EXPOSE 8080

# Health probe.
#
# Once the WAR is deployed Tomcat unpacks ROOT.war into webapps/ROOT/, and
# the gemma-rest dispatcher servlet maps /rest/v2/* (see web.xml). The
# datasets endpoint requires no auth on a small limit query (anonymous read
# is the project default; the /rest/v2/health endpoint exists as well but
# the task brief specifies the datasets probe).
#
# --start-period generous: ontology loading + L2-cache warming can push
# first-ready out past 2 minutes on a cold MySQL.
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3 \
  CMD curl -fsS "http://localhost:8080/rest/v2/datasets?limit=1" || exit 1

# tomcat:10.1-jdk21-temurin's default CMD is `catalina.sh run`; inherit it.

# ---------------------------------------------------------------------------
# Stage 3: gemma-cli (JDK 25 runtime for the command-line tools)
# ---------------------------------------------------------------------------
# The CLI is Java 25 bytecode, so it needs a JDK/JRE 25 to run. This stage
# bundles the self-contained appassembler distribution (bin/ launcher + flat
# lib/ of ~200 dependency jars + etc/ config) produced by the build stage.
#
# NOTE: this is NOT the default build target — the runtime (WAR) stage above is
# last so an untargeted `docker build` still yields the gemma-rest image. The
# publish-image workflow builds this stage explicitly with `target: cli`.
#
# Run:
#   docker run --rm \
#     -v "$HOME/Gemma.properties":/home/gemma/Gemma.properties:ro \
#     ghcr.io/pavlidislab/gemma-cli:2.0.0-alpha <command> [args...]
# (Config is read from $HOME/Gemma.properties; GEMMA_* env vars also work and
#  take precedence — see CONTAINER_CONFIG.md.)
FROM eclipse-temurin:25-jre AS cli

COPY --from=build /build/gemma-cli/target/appassembler /opt/gemma-cli

# Non-root user with a writable HOME so $HOME/Gemma.properties resolves and the
# CLI can write caches/logs under a mounted appdata dir.
RUN groupadd --system gemma \
 && useradd  --system --gid gemma --home /home/gemma --create-home gemma
USER gemma:gemma
ENV HOME=/home/gemma
WORKDIR /home/gemma

ENTRYPOINT ["/opt/gemma-cli/bin/gemma-cli"]
