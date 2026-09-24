# gemma-rest standalone packaging — RECCE

Recce date: 2026-05-19.
Branch: `worktree-agent-a8fd0b9ced991d4d3` off `phase2-acl-migrate` HEAD
`35ee463e2d` ("Phase 3 build: bump spring-boot-dependencies BOM 3.3.13 -> 3.5.6").

Static read only — no maven, no edits to production code, no pom changes.

This document complements two existing planning docs in the repo root:

- `GEMMA_REST_STANDALONE_ROADMAP.md` (409 lines, recce + roadmap from 2026-05-18 on baseline `08e760bdaf`)
- `GEMMA_REST_BOOTSTRAP_PHASE1.md` (122 lines, "what landed" notes from a partial Phase 1 slice)

What's new in this RECCE relative to those docs: a focused inventory pinned to the **current** branch HEAD, with exact file paths + line numbers, framing the work as the six questions the recce was asked to answer. Where prior work has already landed (the gemma-rest `web.xml`, the `RestSecurityConfig` Java class), this doc records that and identifies the remaining gaps.

---

## TL;DR

- **Current state**: gemma-rest packages as a plain JAR (no `<packaging>` element in `gemma-rest/pom.xml`). Its REST surface only boots inside gemma-web's WAR because gemma-web's `web.xml` registers the Jersey servlet + filters and gemma-web's `applicationContext-security.xml` registers the `/rest/v2/**` Spring Security chain.
- **Already landed on this HEAD**:
  - `gemma-rest/src/main/webapp/WEB-INF/web.xml` (a complete Jakarta EE 6 descriptor; 193 lines)
  - `gemma-rest/src/main/java/ubic/gemma/rest/security/RestSecurityConfig.java` (Spring Security 6 Java config; 216 lines; picked up by gemma-rest component-scan; coexists with the legacy XML chain)
- **Not yet landed**:
  - The `gemma-rest-war` Maven profile to flip packaging to `war` (described in `GEMMA_REST_BOOTSTRAP_PHASE1.md` but **NOT** in `gemma-rest/pom.xml` on this HEAD)
  - Promotion of `RestAuthEntryPoint` from XML bean to `@Component` (or to a `@Bean` in `RestSecurityConfig`) — required because the XML definition lives in the soon-to-die `gemma-web/.../applicationContext-security.xml:35-39`
  - Removal of the duplicate `<s:http pattern="/rest/v2/**">` block from `gemma-web/.../applicationContext-security.xml:41-47`
  - A gemma-rest-owned `InitializeContext` (active-profile injection)
  - Relocation of `restapidocs/` static assets from gemma-web's WAR to gemma-rest's WAR
- **Recommendation**: ship Option C (standalone WAR) first. Defer Option B (embedded Tomcat / executable JAR) and Option A (Spring Boot 3) until after gemma-web deletion.
- **Estimated first-slice effort to "gemma-rest WAR boots standalone in Tomcat 10.1"**: ~1.5 agent-sessions of net new work (the Maven profile + RestAuthEntryPoint promotion + a CI smoke job). Phases 2–7 (XML deletion, cutover, gemma-web deletion) add ~3.5 sessions.

---

## 1. Current packaging

### 1.1 `gemma-rest/pom.xml` (on HEAD `35ee463e2d`)

- No `<packaging>` element → defaults to `jar`.
- No `gemma-rest-war` profile despite `GEMMA_REST_BOOTSTRAP_PHASE1.md` describing one (the profile work was scoped but not committed on the merge into `phase2-acl-migrate`).
- Key compile-scope deps relevant to a standalone WAR:
  - `gemma-core` (line 35-39) — pulls in the bulk of Spring config
  - `spring-webmvc` (60-62) — already a compile dep
  - `spring-security-web` (66-69) — already a compile dep
  - `jersey-server`, `jersey-container-servlet`, `jersey-spring6`, `jersey-media-json-jackson` (72-133)
  - `jakarta.servlet-api 6.0.0` **provided** (162-167)
  - `jakarta.ws.rs-api 3.1.0` (157-160)
  - Swagger v3 jakarta deps (170-259)
- **Missing for WAR packaging**: `tomcat-servlet-api` / `tomcat-catalina` provided-scope deps (currently in `gemma-web/pom.xml:172-186`). When `gemma-rest/pom.xml` flips to `<packaging>war</packaging>`, these need to be inherited or duplicated. The `tomcat.version` property (`10.1.34`) is in the root pom (`pom.xml:1355`).

### 1.2 `gemma-web/pom.xml` (the current "war assembly")

- `<packaging>war</packaging>` (line 10), `<finalName>Gemma</finalName>` (line 14).
- Has `gemma-rest` as a compile-scope dep (`gemma-web/pom.xml:103-107`) — so gemma-rest's classes + `applicationContext-{analytics,component-scan}.xml` + `webapp/WEB-INF/web.xml` end up bundled inside `Gemma.war`.
- `maven-war-plugin` `<webResources>` block (lines 69-74) pulls `gemma-rest/src/main/resources/restapidocs` into `Gemma.war` at `resources/restapidocs`. **This webResources block must move to `gemma-rest/pom.xml` when gemma-rest goes WAR** — currently the Swagger UI assets live in gemma-rest's classpath but the WAR-merge happens at the gemma-web level.
- Note: `gemma-rest/src/main/webapp/WEB-INF/web.xml` is currently **shadowed** when gemma-rest is bundled as a JAR inside gemma-web — JAR-packed webapp dirs aren't honored by the servlet container. So the descriptor exists but is dead weight in the current build.

### 1.3 Direction of dependency

`gemma-web → gemma-rest` (compile). gemma-rest does NOT depend on gemma-web. So splitting them is non-circular: flipping gemma-rest to WAR is purely additive — gemma-web continues to consume gemma-rest's classes via its WEB-INF/lib JAR bundling.

### 1.4 The bootstrap problem in one sentence

The Jersey servlet (`/rest/v2/*`), the CORS filter, the Spring Security filter chain, the listeners, the `contextConfigLocation` glob, the `InitializeContext` initializer, the session config, and the error pages are all declared in `gemma-web/src/main/webapp/WEB-INF/web.xml`. Delete gemma-web and the REST API stops booting.

---

## 2. Spring Security wiring for `/rest/v2/**`

The relevant file is `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml` (86 lines total). This is the **non-migrated** XML — distinct from `gemma-core/src/main/resources/ubic/gemma/applicationContext-security.xml` which was already replaced by `SecurityConfig.java` in merge `a59d5c27c3` ("Merge xml-security: applicationContext-security.xml -> SecurityConfig.java").

### 2.1 Beans defined in this file (lines 9-39)

| line | bean id                              | class                                                                                        | notes                                                                                                       |
|-----:|---------------------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
|  9-18| `httpAccessDecisionManager`           | `AffirmativeBased(WebExpressionVoter, roleHierarchyVoter, AuthenticatedVoter)`               | `allowIfAllAbstainDecisions=true`. **Already duplicated** in `RestSecurityConfig.java:203-215` as `@Bean(name="httpAccessDecisionManager")` — Spring will fail on bean-id collision when both are loaded. |
| 21-25| `defaultWebSecurityExpressionHandler` | `DefaultWebSecurityExpressionHandler`                                                        | For JSP `<sec:authorize>` tags. Web-UI only; not needed in standalone gemma-rest.                           |
| 27-29| `ajaxAuthenticationSuccessHandler`    | `gemma.gsec.authentication.AjaxAuthenticationSuccessHandler`                                 | Form-login response handler. Web-UI only.                                                                   |
| 31-33| `ajaxAuthenticationFailureHandler`    | `gemma.gsec.authentication.AjaxAuthenticationFailureHandler`                                 | Form-login response handler. Web-UI only.                                                                   |
| 35-39| `restAuthEntryPoint`                  | `ubic.gemma.rest.security.RestAuthEntryPoint`                                                | **CRITICAL** — class lives in gemma-rest but bean is declared in gemma-web XML. Standalone gemma-rest WAR has no other producer for this bean. |

### 2.2 The `/rest/v2/**` `<s:http>` block (lines 41-47)

```xml
<s:http access-decision-manager-ref="httpAccessDecisionManager" pattern="/rest/v2/**"
        entry-point-ref="restAuthEntryPoint" realm="Gemma RESTful API">
    <s:anonymous granted-authority="IS_AUTHENTICATED_ANONYMOUSLY"/>
    <s:http-basic entry-point-ref="restAuthEntryPoint"/>
    <s:intercept-url pattern="/rest/v2/users/**" access="GROUP_USER"/>
</s:http>
```

This block has already been translated to Spring Security 6 idioms in
`gemma-rest/src/main/java/ubic/gemma/rest/security/RestSecurityConfig.java:151-172`. The Java config currently runs **alongside** the XML chain inside the gemma-web WAR; both produce a `SecurityFilterChain` bean for `/rest/v2/**`. The standalone gemma-rest WAR's classpath would NOT include `gemma-web/applicationContext-security.xml`, so only the Java config contributes. See `RestSecurityConfig.java:34-51` for the wiring-status javadoc.

### 2.3 The catch-all `<s:http pattern="/**">` block (lines 51-85)

This is gemma-web's UI chain. Form-login at `/login.jsp`, remember-me, session-management with `max-sessions=1`, a long list of `<s:intercept-url>` rules for `/admin/**`, `/userProfile.html`, etc. **None of this is needed in standalone gemma-rest** — but care is required at cutover: when gemma-web is deleted, this chain disappears, which means gemma-rest's `/rest/v2/**` chain becomes the only registered chain. The matcher `.securityMatcher("/rest/v2/**")` on the REST chain means anything else returns 404 from Spring (which is fine; nothing else exists in the gemma-rest WAR).

### 2.4 The shared `AuthenticationManager`

Defined in `gemma-core/src/main/resources/ubic/gemma/applicationContext-security.xml` (Java-config migrated in commit `a59d5c27c3`):
- `LegacyAwareDaoAuthenticationProvider`
- `RunAsImplAuthenticationProvider`
- `AnonymousAuthenticationProvider`

Already in core — survives gemma-web removal cleanly. The REST chain inherits it automatically through Spring Security 6's `AuthenticationManagerBuilder` defaulting.

### 2.5 The `RoleHierarchy` bean

Defined in gsec (`gemma/gsec/applicationContext-gsec.xml`). Spring Security 6's `authorizeHttpRequests` picks up `RoleHierarchy` automatically when present as a bean, so `hasAuthority("GROUP_USER")` in `RestSecurityConfig.java:159` correctly grants GROUP_ADMIN too. No work needed.

### 2.6 Direct `accessDecisionManager.decide(...)` callsites

Three callsites in gemma-rest inject the `AccessDecisionManager` bean directly:

| file                                                                            | line | call                                                                                                                  |
|---------------------------------------------------------------------------------|-----:|-----------------------------------------------------------------------------------------------------------------------|
| `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java`              | 3199 | `accessDecisionManager.decide(... GROUP_ADMIN ...)`                                                                   |
| `gemma-rest/src/main/java/ubic/gemma/rest/PlatformsWebService.java`             | 318  | `accessDecisionManager.decide(... GROUP_ADMIN ...)`                                                                   |
| `gemma-rest/src/main/java/ubic/gemma/rest/providers/CacheControlHeaderDecorator.java` | 54   | `accessDecisionManager.decide(auth, null, configAttributes)`                                                          |

These currently resolve to the XML-defined `httpAccessDecisionManager` bean in gemma-web. The Java-config bean in `RestSecurityConfig.java:203-215` is a drop-in replacement. **Risk**: bean-id collision (`httpAccessDecisionManager`) when both XML and Java configs are loaded. The duplicate is already present on this HEAD; needs verification that Spring's allow-bean-definition-overriding policy (or lack thereof) doesn't break the gemma-web boot. (Out of scope for this recce — gemma-web boot smoke test on this HEAD is the next thing to actually run.)

---

## 3. Servlet mappings (gemma-web `web.xml`)

`gemma-web/src/main/webapp/WEB-INF/web.xml` is 308 lines. Mappings relevant to gemma-rest:

### 3.1 Context init (lines 28-43)

- `contextConfigLocation = classpath*:ubic/gemma/applicationContext-*.xml` (line 30). Same glob the gemma-rest WAR re-uses verbatim at `gemma-rest/src/main/webapp/WEB-INF/web.xml:42`.
- `contextInitializerClasses = ubic.gemma.web.context.InitializeContext` (line 35). gemma-web-specific class — see §3.6.
- `defaultHtmlEscape=true` (line 42). Spring JSP/JSTL tag config. Web-UI only.

### 3.2 Listeners (lines 155-166)

| line     | class                                                                  | needed standalone? |
|---------:|------------------------------------------------------------------------|--------------------|
| 156-157  | `org.springframework.web.util.IntrospectorCleanupListener`             | yes                |
| 158-160  | `ubic.gemma.web.listener.StartupListener` (extends ContextLoaderListener) | no — replace with bare `ContextLoaderListener` |
| 161-163  | `ubic.gemma.web.listener.UserCounterListener`                          | no — counts active web-UI sessions only |
| 164-166  | `org.springframework.security.web.session.HttpSessionEventPublisher`   | yes (HTTP Basic + stateless still benefits from session event handling for any inherited session bean wiring) |

All four classes live in `gemma-web/src/main/java/ubic/gemma/web/listener/` (verified via grep). The replacement gemma-rest descriptor at `gemma-rest/src/main/webapp/WEB-INF/web.xml:58-66` drops `StartupListener` and `UserCounterListener` and uses plain `ContextLoaderListener`.

### 3.3 Filters

| filter                          | class                                                              | declared lines | mapped to                                                                                                   | needed standalone? |
|---------------------------------|--------------------------------------------------------------------|---------------:|-------------------------------------------------------------------------------------------------------------|--------------------|
| `gemmaWebMetricsFilter`         | `ubic.gemma.web.metrics.binder.servlet.ServletMetricsFilter`       | 48-55          | `servlet-name=gemma` (DispatcherServlet) only — line 111-114                                                | no — REST has no equivalent today |
| `springSecurityFilterChain`     | `DelegatingFilterProxy`                                            | 58-61          | `/*` — line 131-134                                                                                          | yes                |
| `encodingFilter`                | `CharacterEncodingFilter` UTF-8                                    | 64-75          | `/j_spring_security_check` + servlet `gemma` — line 137-141                                                  | yes (broaden to `/*`) |
| `sitemesh`                      | `com.opensymphony.sitemesh.webapp.SiteMeshFilter`                  | 78-81          | servlet `gemma` — line 144-147                                                                               | no — page decoration |
| `cors`                          | `ubic.gemma.rest.servlet.CorsFilter`                               | 84-103         | `/rest/v2/*` + `/j_spring_security_logout` + `/home.html` + `/login.jsp` + `/whatsnew/generateCache.html` — line 119-128 | yes (`/rest/v2/*` only) |
| `restapidocsFilter`             | `ubic.gemma.rest.servlet.RestapidocsIndexRewriteFilter`            | 106-109        | servlet `default` — line 150-153                                                                             | conditional — only if Swagger UI is served from the standalone WAR |

CorsFilter class lives in gemma-rest already (`ubic.gemma.rest.servlet.CorsFilter`). RestapidocsIndexRewriteFilter likewise (`gemma-rest/src/main/java/ubic/gemma/rest/servlet/RestapidocsIndexRewriteFilter.java`). Good — no code moves needed, only descriptor moves.

### 3.4 Servlets

| servlet name        | class / jsp-file                                              | lines    | mapping                                                                       | needed standalone? |
|---------------------|---------------------------------------------------------------|---------:|--------------------------------------------------------------------------------|--------------------|
| `gemma`             | `org.springframework.web.servlet.DispatcherServlet`           | 170-174  | `/` — line 215-218                                                            | no                 |
| `gemma-rest`        | `org.glassfish.jersey.servlet.ServletContainer`               | 176-198  | `/rest/v2/*` — line 220-223                                                   | yes                |
| `gemma-restapidocs` | JSP `/resources/restapidocs/index.jsp`                        | 200-203  | `/resources/restapidocs/index.html` — line 230-233                            | conditional        |
| `default`           | `org.apache.catalina.servlets.DefaultServlet` (precompressed) | 205-213  | `/resources/*` + `*.js *.css *.map *.png *.jpg *.gif *.svg *.ico *.otf *.eot *.ttf *.woff *.woff2 *.swf /robots.txt /sitemap.xml` — line 225-260 | conditional — only if Swagger UI ships from this WAR |

The Jersey servlet config (lines 176-198) is the load-bearing one for the REST API. Init params:
- `jersey.config.server.provider.packages = io.swagger.v3.jaxrs2.integration.resources,ubic.gemma.rest`
- `jersey.config.server.provider.classnames = org.glassfish.jersey.message.GZipEncoder,org.glassfish.jersey.server.spring.scope.RequestContextFilter`
- `openapi.context.id = ubic.gemma.rest`
- `openApi.configuration.location = /WEB-INF/classes/openapi-configuration.yaml`
- `load-on-startup=0`

The gemma-rest replacement descriptor copies all of this at `gemma-rest/src/main/webapp/WEB-INF/web.xml:156-181` (load-on-startup bumped to 1, which is harmless).

### 3.5 Session config + error pages + mime mappings (lines 262-309)

- `<session-config>` with 60-minute timeout + `httpOnly` + `secure` cookies (lines 270-276). Standalone gemma-rest is `SessionCreationPolicy.STATELESS` per `RestSecurityConfig.java:168-169` — session-config is moot but harmless.
- `<error-page>` pointing at `/error.jsp` (line 278-280). Standalone gemma-rest returns JSON `ResponseErrorObject` via `RestAuthEntryPoint` + Jersey `ExceptionMapper`s — no JSP fallback exists or is needed.
- `<jsp-config>` (262-268), `<mime-mapping>` (283-308). No JSP in standalone gemma-rest, no static fonts. Drop entirely.

### 3.6 `InitializeContext` (gemma-web context initializer)

File: `gemma-web/src/main/java/ubic/gemma/web/context/InitializeContext.java` (108 lines).

Three things it does:
1. **Activates the `web` Spring profile** (line 44-46: `cac.getEnvironment().addActiveProfile("web")`). Probably gates some gemma-core `@Profile("web")` beans — needs an audit. Standalone gemma-rest will want its own profile token (`rest`?) and any beans currently `@Profile("web")`-gated that REST needs (e.g. a transactional bean exposed via REST endpoints) need to be either un-gated or `@Profile({"web","rest"})`-gated.
2. **Loads all `Settings` keys into a servletContext attribute** (lines 56-83). Used by JSPs to render `${appConfig['...']}` expressions. Not needed for REST.
3. **Loads theme + Google Analytics tracker key** (line 88-105). Not needed for REST.

Most of this is web-UI-only. The one load-bearing item for REST is the `web` profile activation, which needs a gemma-rest equivalent (a small `RestInitializeContext` class activating a `rest` profile, registered via `<context-param>contextInitializerClasses</context-param>` in `gemma-rest/web.xml`). This is currently NOT done — the existing `gemma-rest/src/main/webapp/WEB-INF/web.xml` has no `contextInitializerClasses` element. See `GEMMA_REST_BOOTSTRAP_PHASE1.md:108-111` for the same observation.

---

## 4. What's needed to make gemma-rest standalone

### 4.1 Option A — Spring Boot 3 conversion

Convert gemma-rest into a `@SpringBootApplication` with embedded Tomcat, `spring-boot-starter-jersey`, `spring-boot-starter-security`, externalized config properties.

**Note**: the root pom *already* has the `spring-boot-dependencies` BOM imported (the HEAD commit `35ee463e2d` bumped it 3.3.13 → 3.5.6). So the BOM management is in place; only the application code would need converting.

**Pros**:
- Idiomatic 2026 deployable. Actuator endpoints (health, metrics, info) come free.
- `JerseyAutoConfiguration` registers `ServletContainer` automatically.
- Externalized config (`application.yml`, profiles, `@ConfigurationProperties`).
- Test slices (`@WebMvcTest`, `@JerseyTest`) ergonomic.
- Aligns with the `gemma-curation-ui` delivery model (modern, containerized).

**Cons**:
- Large refactor. gemma-core's `classpath*:ubic/gemma/applicationContext-*.xml` glob fights Boot's auto-config — would require `@ImportResource` + disabling chunks of auto-config (hybrid mode), or porting the XML zoo wholesale to Java config (orthogonal Phase 3 slice; many remaining `applicationContext-*.xml` files in gemma-core).
- Two-way breakage risk with gemma-cli (which shares the same XML glob via `SpringContextUtils.prepareContext`).
- Boot's opinions about logging, datasource config, validation interact awkwardly with the existing `Settings`/Gemma.properties scheme.

**Effort**: 5-8 agent-sessions for the conversion, plus parallel work to clean up gemma-cli interactions. **Risk**: high (boot auto-config impedance with the XML zoo).

### 4.2 Option B — Spring Framework 6 + programmatic Tomcat embed

Build gemma-rest as an executable JAR with `tomcat-embed-core` (no Boot) and a hand-written `main()` that:
1. Instantiates a `Tomcat` programmatically (port from `Gemma.properties` or `--port`).
2. Registers `ContextLoaderListener` with the existing classpath-glob `contextConfigLocation`.
3. Programmatically registers the Jersey `ServletContainer` at `/rest/v2/*`.
4. Programmatically registers `DelegatingFilterProxy` for `springSecurityFilterChain`.
5. Programmatically registers `CorsFilter` + `CharacterEncodingFilter`.
6. Re-uses gemma-core's existing `SettingsConfig` for property loading.

**Pros**:
- Smallest delta from today. Re-uses the existing XML root context with no changes.
- Same Tomcat 10.1.x as production (root pom `tomcat.version=10.1.34`).
- Skips the Boot auto-config impedance entirely.
- The Spring-context semantics gemma-core relies on (`ApplicationContextAware`, AOP autoproxy, `@EnableGlobalMethodSecurity`'s RunAsManager) work identically — they don't care whether Tomcat was started by Catalina or `new Tomcat()`.

**Cons**:
- Less idiomatic than Boot.
- Have to hand-roll graceful shutdown, health endpoints, log config — though `SettingsConfig` already handles property loading.

**Effort**: 2-3 agent-sessions on top of Option C. **Risk**: medium (Tomcat embed-API quirks; Jersey + embed interactions; shading/uberjar).

### 4.3 Option C — gemma-rest WAR (recommended first slice)

Promote gemma-rest from JAR to WAR. Add its own `src/main/webapp/WEB-INF/web.xml` (**already done** on this HEAD — see `gemma-rest/src/main/webapp/WEB-INF/web.xml`) and deploy into an external Tomcat 10.1 install side-by-side with `Gemma.war` (or replacing it once gemma-web is deleted).

**Pros**:
- Zero change to the runtime model. Operations team already runs Tomcat 10.1.
- Smallest code delta — the descriptor + Java security config already exist. The remaining work is a Maven profile + a handful of XML cleanups.
- The Spring root context loads from the same classpath-glob; gemma-web's contributions simply aren't on the classpath when only gemma-rest is deployed.

**Cons**:
- Still requires Tomcat install on the host. No `./gemma-rest.jar --port=8080` ergonomics.
- Not friendly for container-image / k8s deployment without an extra layer.

**Effort**: ~1.5 agent-sessions of net new work (see §5). **Risk**: low.

### 4.4 Recommendation

**Phase 1 — Option C.** Smallest, lowest-risk. Get the WAR building, deployable, smoke-tested against Tomcat 10.1. This is the first slice — most of the prep work has already landed.

**Phase 2 — Option B.** Once C is stable in production, swap the WAR for an executable JAR with embedded Tomcat. Same Spring context, same filters, same Tomcat version — just programmatic registration replacing `web.xml`. Unblocks container-image / k8s deployment.

**Option A (Boot)** is the long-term destination but not the next step. Defer until the gemma-core XML zoo migration progresses further.

This matches the recommendation in `GEMMA_REST_STANDALONE_ROADMAP.md:201-218`.

---

## 5. Phasing — the minimum-viable first slice

Already landed on `phase2-acl-migrate` HEAD `35ee463e2d`:

- `gemma-rest/src/main/webapp/WEB-INF/web.xml` (193 lines). Servlet 6 / Jakarta EE 6 namespace. Listeners, filters, Jersey servlet, no session-config, no error-page.
- `gemma-rest/src/main/java/ubic/gemma/rest/security/RestSecurityConfig.java` (216 lines). `SecurityFilterChain` + `httpAccessDecisionManager` `@Bean`s.

Remaining work for the first deployable slice ("gemma-rest WAR boots in Tomcat 10.1, no gemma-web"):

| step | scope                                                                                                                                                                                                                                                       | session estimate |
|-----:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
|    1 | **Add `gemma-rest-war` Maven profile** to `gemma-rest/pom.xml`. Property-driven `<packaging>${gemma.rest.packaging}</packaging>` defaulting to `jar` so gemma-web's WAR build is untouched. Profile flips to `war`, adds `<finalName>gemma-rest</finalName>`, configures `maven-war-plugin` with `failOnMissingWebXml=true`, adds `tomcat-servlet-api`/`tomcat-catalina` provided deps. | 0.5              |
|    2 | **Promote `RestAuthEntryPoint` to a Spring-discoverable bean.** Either `@Component` on the class or `@Bean restAuthEntryPoint()` in `RestSecurityConfig`. Required because `gemma-web/applicationContext-security.xml:35-39` is the only current producer and that XML disappears post-cutover. The constructor args (`ObjectMapper`, `Future<OpenAPI>`, `BuildInfo`) all resolve from other beans. | 0.25             |
|    3 | **Add a gemma-rest-owned `InitializeContext`** (or accept the loss of the `web` profile activation — see §3.6 caveat). Audit gemma-core for `@Profile("web")` annotations; decide whether REST needs a `rest` profile or whether the affected beans should be un-gated. | 0.25             |
|    4 | **Move the `restapidocs/` webResources block** from `gemma-web/pom.xml:67-92` (`maven-war-plugin` config) to `gemma-rest/pom.xml`'s war-profile section. Wire the `RestapidocsIndexRewriteFilter` + a `DefaultServlet` in `gemma-rest/web.xml`. (Optional if Swagger UI is served separately; required if it ships in the gemma-rest WAR.) | 0.25             |
|    5 | **CI smoke job**: deploy `gemma-rest.war` to a stub Tomcat 10.1, `curl /rest/v2/`, expect 200 + JSON. Fix whatever breaks. Likely issues: `Gemma.properties` on classpath, `${cors.allowedOrigins}` resolution, missing `restAuthEntryPoint` constructor arg if it ends up `@Component`-only. | 0.5              |

**Subtotal first slice: ~1.75 agent-sessions.**

Subsequent phases (deferred):

| phase | scope                                                                                                                                                                            | session estimate |
|------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
|     6 | Delete the `<s:http pattern="/rest/v2/**">` block from `gemma-web/applicationContext-security.xml:41-47`. Verify gemma-web boot still green. Verify Jersey integration tests still green. | 0.5              |
|     7 | Cutover: deploy `gemma-rest.war` to staging alongside `Gemma.war`. Cut load-balancer. Leave gemma-web running for one sprint as fallback.                                          | 0.5 (ops)        |
|     8 | Delete gemma-web (~187 Java files, ~29kLOC, 79 JSPs, 15 XML configs per `GEMMA_REST_STANDALONE_ROADMAP.md:357`). Update root `pom.xml` `<modules>`.                                | 1                |
|     9 | **Option B**: embedded-Tomcat fat-jar. Add `tomcat-embed-core` dep. Hand-write `ubic.gemma.rest.Main`. Add `maven-shade-plugin`.                                                  | 2                |
|    10 | Container image (Dockerfile) for gemma-rest standalone. Healthcheck endpoint. Externalized `Gemma.properties` via volume mount or env-var resolution.                              | 1                |

**Total to Option C in production**: ~3 sessions.
**Total to Option B fat-jar with container image**: ~6 sessions.

These line up with — but tighten — the estimates in `GEMMA_REST_STANDALONE_ROADMAP.md:349-364` because the descriptor + Java security config have already landed.

---

## 6. Blockers — what's genuinely hard

### 6.1 `RestAuthEntryPoint` bean is XML-defined in gemma-web (HARD-ish)

`gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml:35-39` is the only current producer of the `restAuthEntryPoint` bean. The class `ubic.gemma.rest.security.RestAuthEntryPoint` (in gemma-rest) is **not** `@Component`-annotated. When the standalone gemma-rest WAR is built, `gemma-web/applicationContext-security.xml` is not on its classpath — so `RestSecurityConfig.java:154` (which `@Qualifier("restAuthEntryPoint")`-injects it) fails bean lookup.

**Fix**: add `@Component("restAuthEntryPoint")` to `RestAuthEntryPoint.java` (the constructor args all resolve from existing beans: `ObjectMapper` from gemma-rest, `Future<OpenAPI>` from `OpenApiConfig`, `BuildInfo` from gemma-core). Alternative: define it as a `@Bean` in `RestSecurityConfig`. Either is a 10-line patch.

**Why this is non-obvious**: the existing `RestSecurityConfig.java` Java doc claims the wiring works in both gemma-web and standalone modes. It works in gemma-web mode because the XML provides the bean. In standalone mode it has no producer. This is the most load-bearing finding in this recce.

### 6.2 Bean-id collision: `httpAccessDecisionManager` (MEDIUM, latent)

`gemma-web/applicationContext-security.xml:9-18` defines `<bean id="httpAccessDecisionManager">` AND `RestSecurityConfig.java:203-215` defines `@Bean(name="httpAccessDecisionManager")`. Both are loaded when gemma-web boots today. Spring will either:
- Throw `BeanDefinitionOverrideException` (Spring Boot default; gemma-core may have `allowBeanDefinitionOverriding` set somewhere — needs audit), OR
- Silently let the later definition win.

This needs verification by booting gemma-web on this HEAD and observing. Not strictly a blocker for the standalone WAR (which doesn't have the XML), but a blocker for the **coexistence** period during the migration.

### 6.3 `@Profile("web")` audit (MEDIUM)

`InitializeContext.java:45` activates the `web` Spring profile. Need to grep gemma-core + gemma-rest for `@Profile("web")` annotations and `<beans profile="web">` XML, decide which of those beans REST needs, and either un-gate them or introduce a `rest` profile alongside `web`. Risk: a bean REST needs is `@Profile("web")`-gated and silently absent in standalone mode, manifesting as a runtime NPE on the first REST call that uses it. Out of scope for the descriptor + pom work; needed before the CI smoke job in step 5.

### 6.4 CORS preflight on logout endpoint (MEDIUM)

`gemma-web/web.xml:124` maps the CORS filter to `/j_spring_security_logout` (and `/home.html`, `/login.jsp`, `/whatsnew/generateCache.html`). These don't exist in the standalone gemma-rest WAR — but **GemBrow currently calls `/j_spring_security_logout` cross-origin**. When gemma-web dies, GemBrow's logout button breaks.

**Fix**: add a `/rest/v2/auth/logout` endpoint to gemma-rest that invalidates the session (or, in stateless mode, just returns 204) before gemma-web is deleted. Tracked in `GEMMA_REST_STANDALONE_ROADMAP.md:380-384`. Blocks phase 8 (gemma-web deletion), not the first slice.

### 6.5 Bundle size + shared classpath (LOW)

Building gemma-rest as a WAR will produce ~100 MB of `WEB-INF/lib/` (gemma-core, Spring 6, Jersey 3, Hibernate, all transitively). Same size as gemma-web today. Co-deploying both WARs in the same Tomcat instance during cutover doubles disk + memory. Production has the headroom; staging needs a sanity check.

### 6.6 No shared session state between gemma-web and gemma-rest (NON-ISSUE)

`RestSecurityConfig.java:168-169` sets `SessionCreationPolicy.STATELESS`. The REST chain never reads or writes `JSESSIONID`. So there's no "shared session" concern between gemma-web and gemma-rest — they pass like ships in the night. **Confirmed not a blocker.**

### 6.7 gemma-rest does NOT depend on gemma-web (NON-ISSUE)

Verified via `gemma-rest/pom.xml` (no `gemma-web` dep) and `gemma-web/pom.xml:103-107` (gemma-web depends on gemma-rest, not vice versa). So removing gemma-web from the build is purely a deletion problem, not a re-architecture problem. **Confirmed not a blocker.**

### 6.8 `gemma-rest/src/main/webapp` is currently dead code (LOW)

The descriptor at `gemma-rest/src/main/webapp/WEB-INF/web.xml` is bundled inside the gemma-rest JAR but ignored by Tomcat (only WARs' webapp dirs are processed). It's harmless dead weight until the `gemma-rest-war` profile flips packaging. **Confirmed not a blocker** — just a documentation footnote.

---

## 7. File reference (all paths absolute from the worktree root)

| concern                                  | file                                                                                                                                       | key lines                          |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| gemma-rest packaging                     | `gemma-rest/pom.xml`                                                                                                                       | line 12 (artifactId, no packaging) |
| gemma-rest standalone descriptor (new)   | `gemma-rest/src/main/webapp/WEB-INF/web.xml`                                                                                               | 1-193                              |
| gemma-rest Spring Security 6 Java config | `gemma-rest/src/main/java/ubic/gemma/rest/security/RestSecurityConfig.java`                                                                | 151-215                            |
| gemma-rest auth entry point (no @Component yet) | `gemma-rest/src/main/java/ubic/gemma/rest/security/RestAuthEntryPoint.java`                                                          | 42-54                              |
| gemma-rest CORS filter class             | `gemma-rest/src/main/java/ubic/gemma/rest/servlet/CorsFilter.java`                                                                         | —                                  |
| gemma-rest swagger UI rewrite filter     | `gemma-rest/src/main/java/ubic/gemma/rest/servlet/RestapidocsIndexRewriteFilter.java`                                                      | 18-28                              |
| gemma-rest component-scan stub           | `gemma-rest/src/main/resources/ubic/gemma/applicationContext-component-scan.xml`                                                           | 1-12                               |
| gemma-rest direct `decide()` callsites   | `gemma-rest/src/main/java/ubic/gemma/rest/{DatasetsWebService.java:3199, PlatformsWebService.java:318, providers/CacheControlHeaderDecorator.java:54}` | as noted                          |
| gemma-web packaging                      | `gemma-web/pom.xml`                                                                                                                        | line 10                            |
| gemma-web war assembly (webResources)    | `gemma-web/pom.xml`                                                                                                                        | 67-92                              |
| gemma-web web.xml (filters, listeners, servlets) | `gemma-web/src/main/webapp/WEB-INF/web.xml`                                                                                        | full file                          |
| gemma-web Spring Security XML (REST chain) | `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml`                                                                | 9-47                               |
| gemma-web Spring Security XML (web-UI chain) | `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml`                                                              | 51-85                              |
| gemma-web context initializer            | `gemma-web/src/main/java/ubic/gemma/web/context/InitializeContext.java`                                                                    | 27-107                             |
| gemma-web listeners (drop in standalone) | `gemma-web/src/main/java/ubic/gemma/web/listener/{StartupListener.java, UserCounterListener.java}`                                         | —                                  |
| gemma-core security Java config (migrated) | `gemma-core/src/main/java/.../SecurityConfig.java` (per commit `a59d5c27c3`)                                                              | —                                  |
| Tomcat version pin                       | `pom.xml`                                                                                                                                  | 1355 (`tomcat.version=10.1.34`)    |
| Jersey version pin                       | `pom.xml`                                                                                                                                  | 1336 (`jersey.version=3.1.11`)     |
| spring-boot-dependencies BOM             | (root `pom.xml`, recently bumped to 3.5.6 in commit `35ee463e2d`)                                                                          | —                                  |

---

## 8. Cross-references to existing planning docs

- `GEMMA_REST_STANDALONE_ROADMAP.md` (409 lines, 2026-05-18): full prior recce + roadmap. This RECCE is a tighter, branch-pinned re-do focused on the six asked questions. The roadmap's phasing in §8 lines up with §5 here.
- `GEMMA_REST_BOOTSTRAP_PHASE1.md` (122 lines, 2026-05-18): notes from a partial Phase 1 slice that landed the descriptor + Java config but **did not** land the `gemma-rest-war` Maven profile. Step 1 in §5 here is the un-landed piece.
- `GEMMA_WEB_RETIREMENT_PLAN.md` (413 lines): the broader gemma-web deletion plan; standalone gemma-rest packaging is a prerequisite. Phase 7 here corresponds to the deletion plan's main slice.
- `MIDDLE_TIER_AUDIT.md` §4.2 + §4.9: the original "REST has no standalone deployment artifact" + "gemma-rest has no XML security config of its own" findings.

---

## 9. Open questions (deferred)

These don't block the first slice but need answers before phase 8 (gemma-web deletion):

1. **AuthN model for gemma-curation-ui ↔ gemma-rest**: stay on cookie/session (status quo, requires keeping JSESSIONID) or move to JWT/Bearer tokens (cleaner, requires a `/rest/v2/auth/login` endpoint that doesn't exist yet)? Recommendation: defer the auth-model change past the standalone packaging change so they don't entangle.
2. **SSR `SecurityController` (ACL admin UI)**: 49 `securityService.*` callsites concentrated in gemma-web — port to `gemma-curation-ui` (via new `/rest/v2/security/**` endpoints) or drop the admin UI entirely? Tracked in `MIDDLE_TIER_AUDIT.md` §3.4.
3. **GemBrow-called non-`/rest/v2/**` endpoints**: `/home.html`, `/login.jsp`, `/j_spring_security_logout`, `/whatsnew/generateCache.html` (per the CORS filter mapping in `gemma-web/web.xml:124-127`). The load-bearing one is `/j_spring_security_logout`; needs a `/rest/v2/auth/logout` equivalent before phase 8.
4. **Tomcat configuration in embedded mode (Option B)**: connector config, SSL, HTTP/2, max-threads. Phase 1 inherits from the existing Tomcat install. Phase 2 needs defaults baked into `Main.java`, overridable via `Gemma.properties`. Recommend a `gemma-rest.tomcat.*` namespace.
5. **Metrics filter parity**: `gemmaWebMetricsFilter` is gemma-web's DispatcherServlet only; REST has no per-request metrics. The standalone is the natural moment to add a `restMetricsFilter` mapped to `/rest/v2/*`. Not blocking.
