# gemma-rest standalone bootstrap — Phase 1 landed

Date: 2026-05-18.
Companion to `GEMMA_REST_STANDALONE_ROADMAP.md` (the recce on
`worktree-gemma-rest-standalone-recce`, commit `e634e0009e`).
Branch this lands on: `worktree-gemma-rest-bootstrap`
(baseline `1fb94ff038`).

## What landed in this slice

1. **`gemma-rest/src/main/webapp/WEB-INF/web.xml`** — Servlet 6 / Jakarta EE
   namespace (`https://jakarta.ee/xml/ns/jakartaee`, `version="6.0"`). The
   minimum bootstrap to serve `/rest/v2/**`:
   - `contextConfigLocation = classpath*:ubic/gemma/applicationContext-*.xml`
     (same glob gemma-web uses today — when running under the standalone
     gemma-rest WAR classpath, gemma-web's XML contributions are simply absent).
   - Listeners: `IntrospectorCleanupListener`, `ContextLoaderListener`,
     `HttpSessionEventPublisher`. **Dropped** (gemma-web-specific):
     `StartupListener`, `UserCounterListener`.
   - Filters (registered in the order Servlet spec evaluates them):
     `encodingFilter` (UTF-8) → `cors`
     (`ubic.gemma.rest.servlet.CorsFilter`) → `springSecurityFilterChain`
     (`DelegatingFilterProxy`). The CORS-before-security ordering matches
     gemma-web's existing comment-justified ordering at gemma-web web.xml:117-118.
   - Servlet: Jersey `ServletContainer` mapped at `/rest/v2/*` with init params
     copied verbatim from gemma-web web.xml:176-198 (Swagger jaxrs2 + ubic.gemma.rest
     package scan, GZip + Spring RequestContextFilter providers, OpenAPI config
     anchored to context id `ubic.gemma.rest`).
   - **No `<session-config>`, no `<error-page>`, no `<jsp-config>`, no
     `<mime-mapping>`.** This WAR is JSON-API-only; `RestSecurityConfig` runs
     stateless HTTP Basic, errors are emitted as JSON by `RestAuthEntryPoint`
     + Jersey `ExceptionMapper`s, no static assets.

2. **`gemma-rest/pom.xml`** — added a profile-driven packaging switch.
   Default packaging remains `jar` (so gemma-web's WAR build, which depends on
   gemma-rest as a compile-scope jar, is untouched). Activating the new
   `gemma-rest-war` profile flips packaging to `war`:

       mvn -pl gemma-rest -am package -P gemma-rest-war -DskipTests

   Mechanics: `<packaging>${gemma.rest.packaging}</packaging>` with
   `gemma.rest.packaging=jar` in the default `<properties>`; the profile
   overrides the property to `war`, sets `<finalName>gemma-rest</finalName>`,
   and configures `maven-war-plugin` with `failOnMissingWebXml=true`.

3. **`RestSecurityConfig` javadoc updated** — removed the "NOT YET WIRED"
   warning, replaced with a "Wiring status" block that describes how the
   `@Configuration` class is picked up (via gemma-rest's existing
   component-scan), the gemma-web↔gemma-rest XML-vs-Java coexistence during
   the migration, and the cutover plan.

## What is deferred to later phases

Per `GEMMA_REST_STANDALONE_ROADMAP.md` §8:

| roadmap phase | scope | status |
|---:|---|---|
| 1 | `RestSecurityConfig` (`SecurityFilterChain` @Bean) | **done** (earlier session, before this branch's baseline `1fb94ff038`) |
| 1 (this slice) | gemma-rest web.xml + war-ready profile | **done** (this commit) |
| 2 | Delete the `<s:http pattern="/rest/v2/**">` block from gemma-web's `applicationContext-security.xml` | deferred |
| 3 | Port `InitializeContext` (active-profile injection etc.) to a gemma-rest-owned class; move `restapidocs/` webResources from gemma-web to gemma-rest | deferred |
| 4 (the cutover) | Flip default packaging from jar to war; delete gemma-web's bootstrap of gemma-rest beans | deferred (this is the merge-break point) |
| 5 | CI smoke job: deploy WAR to a stub Tomcat, hit `GET /rest/v2/`, fix anything that breaks | deferred |
| 6-7 | DNS cutover, gemma-web deletion | deferred |
| 8-9 | Embedded-Tomcat fat-jar + container image | deferred |

This slice deliberately stops at "WAR builds cleanly via the optional profile"
to defer all merge-break risk to the cutover session. gemma-web's build is
unaffected.

## How to test what landed

Default build (gemma-web's expected jar dep):

    JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl gemma-rest package \
        -DskipTests -am -q

Should produce `gemma-rest/target/gemma-rest-1.32.7-SNAPSHOT.jar` (no WAR,
no profile-activated config). Verified on the landing commit.

Standalone WAR build:

    JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl gemma-rest package \
        -DskipTests -am -P gemma-rest-war -q

Should produce `gemma-rest/target/gemma-rest.war` (~100 MB; bundles
gemma-core + Spring + Jersey + Hibernate transitively in `WEB-INF/lib/`).
Verified on the landing commit.

Sanity-check that the WAR contains the descriptor we wrote:

    unzip -p gemma-rest/target/gemma-rest.war WEB-INF/web.xml | head -20

gemma-web compatibility check (must still compile against the default jar):

    JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl gemma-web compile \
        -DskipTests -DskipWebpack=true -am -q

Verified clean on the landing commit.

## What is NOT verified yet

- The WAR has not been deployed to Tomcat. The first real boot is roadmap
  §8 row 5 (a CI smoke job). Expected gaps surfaced by that smoke test:
  - `${cors.allowedOrigins}` resolution requires `Gemma.properties` on the
    Tomcat classpath (already the production convention, but a test stub
    must provide it).
  - No `InitializeContext` is registered; the gemma-rest WAR boots without
    active-profile injection. Anything that depends on `cac.getEnvironment().addActiveProfile("web")`
    in gemma-web's `InitializeContext` will fail in standalone mode. Port
    is roadmap §6 item 7 / §8 row 3.
  - `restapidocs/` static resources are NOT served by this WAR (no
    `DefaultServlet` mapping). Swagger UI access requires roadmap §6 item 6.

- Filter chain ordering with both gemma-web's `applicationContext-security.xml`
  XML chain AND `RestSecurityConfig`'s Java-config chain active in the same
  context (the gemma-web WAR boot path). Both register a `SecurityFilterChain`
  for `/rest/v2/**`; resolution order matters and has not been smoke-tested
  end-to-end. The standalone gemma-rest WAR (this slice's deliverable) does
  NOT have the XML chain on its classpath, so the ambiguity only affects
  gemma-web's existing boot — which is the same state it was in before this
  slice landed.
