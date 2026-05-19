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
# Temurin JDK 17. Deployed at the ROOT context so REST endpoints land at
# /rest/v2/... (matching web.xml's <url-pattern>/rest/v2/*</url-pattern>).
#
# Config: SettingsConfig (23be090ba2) tolerates a missing Gemma.properties; all
# required props can be supplied via GEMMA_* env vars. See CONTAINER_CONFIG.md
# for the env-var pattern and CONTAINER_IMAGE_RECCE.md for the required set.

# ---------------------------------------------------------------------------
# Stage 1: build the WAR
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy POMs first for dependency-resolution layer caching. If the source tree
# changes but no POM moves, Maven's local repo cache survives the rebuild.
COPY pom.xml ./
COPY gemma-core/pom.xml      gemma-core/pom.xml
COPY gemma-rest/pom.xml      gemma-rest/pom.xml
COPY gemma-cli/pom.xml       gemma-cli/pom.xml
COPY gemma-web/pom.xml       gemma-web/pom.xml

# Prime the local repo. -fae so partial misses (a sibling module that's not
# wired here) don't kill the cache; we re-run the real build below.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -ntp -pl gemma-rest -am dependency:go-offline -DskipTests -fae \
    || true

# Now copy sources and build the WAR.
COPY gemma-core/src   gemma-core/src
COPY gemma-rest/src   gemma-rest/src
COPY gemma-cli/src    gemma-cli/src

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -ntp -P gemma-rest-war clean package -pl gemma-rest -am -DskipTests

# Sanity: WAR must exist before we move to the runtime stage.
RUN ls -lh /build/gemma-rest/target/gemma-rest.war

# ---------------------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------------------
# Tomcat 10.1 is the jakarta.servlet 6 baseline. Tomcat 9 ships javax.servlet 4
# and will NOT load gemma-rest -- after the servlet6 cutover the war references
# jakarta.* packages exclusively.
FROM tomcat:10.1-jdk17-temurin AS runtime

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
# - gemma.appdata.home pointed at the VOLUME path above.
# - spring.profiles.active=production to avoid the SpringContextUtils 'dev'
#   profile fallback documented in CONFIG_AUDIT.md HIGH #3.
ENV GEMMA_APPDATA_HOME=/data/gemma \
    CATALINA_OPTS="-Dgemma.appdata.home=/data/gemma \
                   -Dspring.profiles.active=production \
                   -Djava.security.egd=file:/dev/./urandom \
                   -XX:MaxRAMPercentage=75.0 \
                   -XX:+ExitOnOutOfMemoryError"

# Non-root runtime user. UID/GID 1000 by convention. The tomcat base image
# does not pre-create this user, so do it here. /data/gemma and the Tomcat
# work / temp / logs dirs must be writeable.
RUN groupadd --system --gid 1000 gemma \
 && useradd  --system --uid 1000 --gid 1000 --home /home/gemma --create-home gemma \
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

# tomcat:10.1-jdk17-temurin's default CMD is `catalina.sh run`; inherit it.
