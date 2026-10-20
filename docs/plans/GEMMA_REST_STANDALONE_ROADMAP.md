# gemma-rest standalone packaging — roadmap

Recce date: 2026-05-18. Branch `worktree-gemma-rest-standalone-recce` off
`phase2-acl-migrate` HEAD `08e760bdaf`. Static read only — no maven, no edits
to source.

Companion to `MIDDLE_TIER_AUDIT.md` §4.2 ("REST has no standalone deployment
artifact") and §4.9 ("gemma-rest has no XML security config of its own").
Project memory: `gemma-web` is walking-dead; `gemma-curation-ui` is the
replacement frontend; gemma-rest is the new public surface.

---

## 1. Why this matters

`gemma-web` is being retired in favour of `gemma-curation-ui` (separate
React-style SPA, talks to gemma-rest over HTTP). gemma-rest must therefore be
deployable **without** the gemma-web WAR around it. Today it cannot: gemma-rest
ships as a plain JAR whose Jersey servlet, security filter chain, CORS filter,
listeners, error pages, session config, and Spring context loading are all
declared in `gemma-web/src/main/webapp/WEB-INF/web.xml` + gemma-web's
`applicationContext-security.xml`. Kill gemma-web and the REST API stops
booting.

This roadmap describes what to change so `mvn -pl gemma-rest package` produces
a self-bootstrapping deployable that GemBrow / gemma-curation-ui can hit at
`/rest/v2/**` with no other Gemma component running on the host.

---

## 2. Current packaging

| module       | packaging | depends on                                                                        |
|--------------|-----------|-----------------------------------------------------------------------------------|
| `gemma-core` | jar       | —                                                                                 |
| `gemma-rest` | **jar**   | `gemma-core` (compile), `gemma-core` test-jar, Jersey 3.1.10, Spring 6.1.20, Spring Security 6.3.10, Swagger v3 (jakarta), Jakarta Servlet API 6.0.0 **provided**, jakarta.ws.rs-api 3.1.0 |
| `gemma-web`  | **war**   | `gemma-core` + `gemma-rest` (compile), Tomcat 10.1.34 (provided), Sitemesh, JSTL, log4j-web |

Direction of the dep edge: `gemma-web → gemma-rest`. gemma-rest does **not**
depend on gemma-web. So the bundling problem is one-directional: gemma-web
WAR's `web.xml` registers gemma-rest's Jersey servlet (its packages get
classpath-scanned for `@Path` resources).

Servlet-container assumption: **Tomcat 10.1.x** (Jakarta EE 9+). Confirmed via
parent `pom.xml` line 1044 and `gemma-web/pom.xml` provided-scope deps. Jersey
3.1.x is wired for Spring 6 via `jersey-spring6`.

Java baseline (parent pom): `maven.compiler.release=17`, JDK17
amazon-corretto.

---

## 3. Entry-point + Spring/Jersey integration today

`gemma-web/src/main/webapp/WEB-INF/web.xml` is the only bootstrap. Relevant
declarations:

- **Spring root context** loads via implicit `ContextLoaderListener` (Spring
  WebApplicationContext init), driven by:
  ```
  <context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>classpath*:ubic/gemma/applicationContext-*.xml</param-value>
  </context-param>
  ```
  This glob hits both `gemma-core/src/main/resources/ubic/gemma/applicationContext-*.xml`
  (gemma-core beans + security + serviceBeans + analytics + etc.) AND
  `gemma-rest/src/main/resources/ubic/gemma/applicationContext-{analytics,component-scan}.xml`
  AND `gemma-web/src/main/resources/ubic/gemma/applicationContext-{security,serviceBeans,component-scan,metrics}.xml`.
  Single shared root context.
- **Context initializer**: `ubic.gemma.web.context.InitializeContext` (in
  gemma-web). Sets active profile(s) etc.
- **Listeners** (web.xml:155–166): `IntrospectorCleanupListener`,
  `ubic.gemma.web.listener.StartupListener`,
  `ubic.gemma.web.listener.UserCounterListener`,
  `HttpSessionEventPublisher`.
- **Jersey servlet** (web.xml:176–198): `org.glassfish.jersey.servlet.ServletContainer`
  registered as servlet `gemma-rest`, mapped at `/rest/v2/*`. Init params:
  `jersey.config.server.provider.packages=io.swagger.v3.jaxrs2.integration.resources,ubic.gemma.rest`,
  `provider.classnames=org.glassfish.jersey.message.GZipEncoder,org.glassfish.jersey.server.spring.scope.RequestContextFilter`,
  Swagger openapi context id + config location.
- **Spring–Jersey integration**: `jersey-spring6` (gemma-rest/pom.xml:84–114)
  registers `SpringComponentProvider`; Jersey resources pick up `@Autowired`
  beans from the Spring root context that `ContextLoaderListener` built.
- **Filters** that affect `/rest/v2/**` in order:
  1. `cors` — `ubic.gemma.rest.servlet.CorsFilter` (the only filter class
     that lives in gemma-rest, ironically — declared in gemma-web's
     web.xml). Reads `${cors.allowedOrigins}` from Gemma.properties.
  2. `springSecurityFilterChain` — `DelegatingFilterProxy` ⟶ bean
     `springSecurityFilterChain` defined by `<s:http>` in
     `gemma-web/.../applicationContext-security.xml`.
  3. `gemmaWebMetricsFilter` — mapped to servlet `gemma`, **not**
     `gemma-rest`. REST has no equivalent today.

---

## 4. Security wiring today

The `/rest/v2/**` filter chain lives in
`gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml`
lines 41–47:

```xml
<s:http access-decision-manager-ref="httpAccessDecisionManager"
        pattern="/rest/v2/**"
        entry-point-ref="restAuthEntryPoint"
        realm="Gemma RESTful API">
    <s:anonymous granted-authority="IS_AUTHENTICATED_ANONYMOUSLY"/>
    <s:http-basic entry-point-ref="restAuthEntryPoint"/>
    <s:intercept-url pattern="/rest/v2/users/**" access="GROUP_USER"/>
</s:http>
```

What this contributes:

| concern                | how it works today                                                                                                                                                                                                                                  |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authn — anonymous      | `<s:anonymous>` grants `IS_AUTHENTICATED_ANONYMOUSLY` to unauthenticated requests. Required because most REST endpoints are public read.                                                                                                            |
| Authn — HTTP Basic     | `<s:http-basic>` with `restAuthEntryPoint` (`ubic.gemma.rest.security.RestAuthEntryPoint`, gemma-rest:security/) which returns a JSON `ResponseErrorObject` (NOT a `WWW-Authenticate` Basic prompt — declines to challenge the browser) on 401.     |
| Authn — session cookie | Inherited from the global `<s:http pattern="/**">` chain via Tomcat JSESSIONID. Form-login at `/login.jsp` produces the session; the same session is honoured on `/rest/v2/**` because both chains share `springSecurityFilterChain` filter.        |
| Authz                  | URL-pattern: `/rest/v2/users/**` requires `GROUP_USER`. Everything else: `IS_AUTHENTICATED_ANONYMOUSLY` at the URL layer, then method-level `@Secured("GROUP_ADMIN")` (19 occurrences) + `@PreAuthorize` (2) + `accessDecisionManager.decide(...)` manual calls (2). |
| Authz infra            | `httpAccessDecisionManager` = `AffirmativeBased(WebExpressionVoter, roleHierarchyVoter, AuthenticatedVoter)`. `roleHierarchy` + voters defined in gemma-core's applicationContext-security.xml (group hierarchy: `GROUP_ADMIN > GROUP_USER > GROUP_ANONYMOUS`). |
| CORS                   | `ubic.gemma.rest.servlet.CorsFilter`, declared in gemma-web's web.xml lines 84–103. Reads `${cors.allowedOrigins}` + hardcoded `Authorization,Content-Type,X-Gemma-Client-ID,X-Requested-With` headers, `allowCredentials=true`, `maxAge=1200`.       |
| Rate limiting          | None observed in the filter chain.                                                                                                                                                                                                                  |
| Logout                 | URL `/j_spring_security_logout` lives on the gemma-web chain, not the REST chain — but `CorsFilter` is mapped to it explicitly so GemBrow's logout button preflights cleanly (web.xml:124).                                                          |
| AuthnManager           | Defined in `gemma-core/src/main/resources/ubic/gemma/applicationContext-security.xml`: `LegacyAwareDaoAuthenticationProvider` + `RunAsImplAuthenticationProvider` + `AnonymousAuthenticationProvider`. Already in core, not gemma-web. Good — survives gemma-web removal.     |
| Method security        | `ubic.gemma.core.security.MethodSecurityConfig` (Java config, `@EnableGlobalMethodSecurity(securedEnabled=true, prePostEnabled=true)`). Already migrated off XML. **Independent of the http chain — `@Secured`/`@PreAuthorize` annotations keep working when the http XML moves.** |

What needs replacing when gemma-web goes: only the `<s:http pattern="/rest/v2/**">`
block above plus `<s:http pattern="/**">` (whatever portion of it gemma-rest
needs to keep — likely just the anonymous + http-basic part).

---

## 5. Target architecture — options + recommendation

### Option A — Spring Boot 3 conversion

Convert gemma-rest into a Spring Boot 3 application with `@SpringBootApplication`,
embedded Tomcat, Jersey via `spring-boot-starter-jersey`,
`spring-boot-starter-security`, Spring-managed config properties.

Pros: idiomatic in 2026, batteries included (actuator, metrics, externalized
config, profiles), `JerseyAutoConfiguration` handles ServletContainer
registration automatically, ergonomic test slices.

Cons: large refactor. gemma-core's Spring config is XML-heavy and uses
`@ImportResource`-style classpath glob (`classpath*:ubic/gemma/applicationContext-*.xml`)
which fights Spring Boot's auto-config. Requires either:
- adopting `@ImportResource` and disabling chunks of auto-config (hybrid), or
- migrating the whole XML zoo to Java config (orthogonal, larger Phase 3 slice).
Risk of two-way breakage with gemma-cli (which shares the same gemma-core
applicationContext-*.xml glob).

### Option B — Spring Framework 6 + programmatic Tomcat embed (recommended)

Build gemma-rest as an executable JAR with `tomcat-embed-core` +
`tomcat-embed-jasper` (if any JSP, otherwise just `tomcat-embed-core`) and a
`main()` that:

1. Instantiates a `Tomcat` programmatically (port from `gemma.properties` or
   `--port` CLI arg).
2. Adds a context, registers `ContextLoaderListener` with
   `contextConfigLocation = classpath*:ubic/gemma/applicationContext-*.xml`
   (same glob as today, minus gemma-web's contribution because it isn't on the
   classpath of the gemma-rest deployable).
3. Programmatically registers the Jersey `ServletContainer` mapped at
   `/rest/v2/*` (same init params as web.xml today).
4. Programmatically registers `springSecurityFilterChain` via
   `DelegatingFilterProxy`.
5. Programmatically registers `CorsFilter`, `CharacterEncodingFilter`.
6. Wires `Gemma.properties` via the same `SettingsConfig` that already exists
   in gemma-core (no new property-loading scheme).

Pros: smallest delta from today. Re-uses the existing XML root-context. Skips
the Spring Boot auto-config impedance entirely. Same Tomcat 10.1.x as
production. The Spring-context loading semantics that gemma-core relies on
(ApplicationContextAware, AOP autoproxy, `@EnableGlobalMethodSecurity`'s
RunAsManager) work identically — they don't care whether the Tomcat that
hosts them was started by Catalina or `new Tomcat()`.

Cons: less idiomatic than Boot. Have to hand-roll the few things Boot would
give for free (graceful shutdown, actuator endpoints, profile-aware property
loading — though gemma-core's `SettingsConfig` already does the latter).

### Option C — gemma-rest WAR

Promote gemma-rest from jar to war. Its own `src/main/webapp/WEB-INF/web.xml`
contains only the REST-relevant declarations (Jersey servlet, security filter,
CORS filter, encoding filter, listeners minus the gemma-web-specific ones).
Deploy into an external Tomcat 10.1 install.

Pros: zero change to the runtime model. Operations team already knows Tomcat.
Smallest code delta — about 80 lines of web.xml + one new
`SecurityFilterChain` `@Configuration` class.

Cons: still requires a Tomcat install on the host. No `./gemma-rest.jar
--port=8080` ergonomics. Not as friendly for container-image deployment, k8s
sidecars, or local dev.

### Recommendation — start with Option C, then Option B

**Phase 1 — Option C (war).** Smallest, lowest-risk path to "gemma-rest
deploys without gemma-web". Get the security filter chain into Java config,
get the Jersey servlet into a gemma-rest-owned `web.xml`, prove it boots in
the existing Tomcat 10.1 production install. Then gemma-web can be deleted
cleanly.

**Phase 2 — Option B (embedded Tomcat).** Once C is in production, swap the
WAR for an executable JAR with embedded Tomcat. Same Spring context, same
filters, same Tomcat version — just programmatic registration replacing
`web.xml`. This unblocks container-image / k8s deployment and matches the
gemma-curation-ui delivery model.

**Option A (Spring Boot 3)** is the long-term destination but not the next
step. Defer until the gemma-core XML zoo is migrated to Java config, which
is a separate Phase 3 slice that does not depend on (and is not a prerequisite
for) standalone gemma-rest.

---

## 6. Migration prerequisites — the blocking-items list

Items that MUST be in place before gemma-rest can be deployed without
gemma-web. Each marked with the phase from §5 it blocks.

1. **`SecurityFilterChain` `@Bean` (Java config) replacing the XML
   `<s:http pattern="/rest/v2/**">` block.** [Phase 1, blocker.] New class
   `ubic.gemma.rest.security.RestSecurityConfig` with
   `@Configuration @EnableWebSecurity`, returning a
   `SecurityFilterChain` bean using Spring Security 6's
   `HttpSecurity.authorizeHttpRequests(...)` lambda DSL (the SS6 idiom — NOT
   the deprecated `WebSecurityConfigurerAdapter` or
   `authorizeRequests(...)`). Wires `restAuthEntryPoint`,
   `httpAccessDecisionManager` (or its SS6 `AuthorizationManager`
   equivalent), anonymous, http-basic, `/rest/v2/users/**`→`GROUP_USER`.
   Live alongside the XML during the migration; remove XML after cutover.

2. **`web.xml` for gemma-rest** (Option C) OR programmatic Tomcat init
   (Option B). [Phase 1 = C, Phase 2 = B.] Must declare:
   - `contextConfigLocation` = `classpath*:ubic/gemma/applicationContext-*.xml`
     (unchanged glob — gemma-web's contributions will just be absent from the
     classpath).
   - `contextInitializerClasses` — replace
     `ubic.gemma.web.context.InitializeContext` with a gemma-rest-owned
     `ubic.gemma.rest.context.InitializeContext`. Same logic (active profiles)
     minus any gemma-web specifics.
   - Jersey servlet identical to web.xml:176–198.
   - `springSecurityFilterChain` DelegatingFilterProxy filter, mapped to
     `/rest/v2/*` (not `/*` — the REST war has nothing else).
   - `CorsFilter` filter mapped to `/rest/v2/*`.
   - `CharacterEncodingFilter` UTF-8.
   - Listeners: `IntrospectorCleanupListener` and either keep
     `HttpSessionEventPublisher` (if sessions remain) or drop (if token-only).
     **Drop** `StartupListener` and `UserCounterListener` — both are
     gemma-web-specific.
   - Session config: drop entirely if going token-based, or keep
     `<session-timeout>60</session-timeout>` + `<cookie-config>` if shared
     JSESSIONID is preserved.
   - Error pages: gemma-rest already returns
     `ResponseErrorObject` JSON via `RestAuthEntryPoint` and Jersey
     `ExceptionMapper`s — no `<error-page>` element needed.

3. **CORS configuration externalisation.** `${cors.allowedOrigins}` lives in
   `Gemma.properties` and is read by the filter declared in web.xml. In the
   new world, `CorsFilter` is instantiated programmatically (Option B) or
   declared in gemma-rest's own web.xml (Option C). Either way, the property
   resolution still works — gemma-core's `SettingsConfig` already populates
   the Spring `Environment` from `Gemma.properties`. No change needed beyond
   making sure `Gemma.properties` is on the classpath (it is — gemma-core
   ships test/main resources include it; production deploys put the live
   `Gemma.properties` on the Tomcat classpath via setenv.sh).

4. **`gemma-rest/pom.xml` packaging switch.** [Phase 1.] `<packaging>war</packaging>`,
   add `<finalName>gemma-rest</finalName>`, add `tomcat-catalina` /
   `tomcat-servlet-api` provided deps (currently gemma-web carries them).
   maven-war-plugin config: nothing to override; no static resources to bundle
   except the swagger restapidocs which gemma-web's
   maven-war-plugin currently pulls from `gemma-rest/src/main/resources/restapidocs`
   — move that webResources block from gemma-web/pom.xml to gemma-rest/pom.xml.

5. **`BaseJerseyTest` and the four Jersey IT classes.** No change needed
   — `BaseJerseyTest` already uses `InMemoryTestContainerFactory` and a
   `@WebAppConfiguration` Spring context built from the
   `classpath*:applicationContext-*.xml` glob. It runs without Tomcat. The
   `<s:http>` XML block in `applicationContext-security.xml` will be removed,
   but tests don't exercise the filter chain — they call Jersey resources
   directly post-Spring-injection.

6. **Static resource handling.** gemma-rest is API-only. No HTML, no JSP, no
   JS bundles. The only static asset gemma-rest contributes is
   `restapidocs/` (Swagger UI), currently webResources-merged into gemma-web's
   WAR. Move to gemma-rest's WAR (or serve from `/resources/restapidocs/*` via
   Tomcat's DefaultServlet declared in the new web.xml).
   `RestapidocsIndexRewriteFilter` (already in gemma-rest:servlet/) handles
   the index.html rewrite — moves naturally.

7. **`InitializeContext` port.** Read
   `gemma-web/src/main/java/ubic/gemma/web/context/InitializeContext.java`
   (out of scope for this recce — flagged for the Phase 1 implementation
   slice). Anything gemma-web-specific stays in gemma-web (which will be
   deleted); anything cross-cutting (e.g. setting the active profile based on
   `Gemma.properties`) gets ported to a new gemma-rest-side equivalent.

---

## 7. Frontend coupling with gemma-curation-ui

Assumed deployment topology once gemma-web is gone:

```
┌──────────────────────┐         ┌─────────────────────────┐
│ gemma-curation-ui    │  HTTPS  │ gemma-rest standalone   │
│ (React SPA, static)  │ ──────► │ /rest/v2/**             │
│ served by nginx /    │         │ Tomcat 10.1 (Phase 1)   │
│ CDN / object store   │         │ or embedded (Phase 2)   │
└──────────────────────┘         └─────────────────────────┘
                                            │
                                            ▼ JDBC
                                  ┌─────────────────────┐
                                  │ MySQL (gemd / prod) │
                                  └─────────────────────┘
```

**AuthN model — open question, see §9.** Two plausible shapes:

- **Cookie/session (status quo)**: gemma-rest keeps `JSESSIONID`,
  `gemma-curation-ui` is served from the same origin (or a subdomain with a
  shared cookie domain) and `credentials: include` on `fetch`. Simplest port;
  requires the new gemma-rest standalone to retain HttpSession + the
  `<s:remember-me>` cookie. CORS preflights already work on
  `/j_spring_security_logout` per current `CorsFilter` config.
- **Token-based (cleaner)**: gemma-rest issues a JWT or opaque token on
  `/rest/v2/auth/login` (does not exist yet — would need to be built), client
  passes `Authorization: Bearer <token>`. No HttpSession on the REST side.
  Cleaner for cross-origin SPAs and k8s. Larger Phase 3 slice — defer past
  the standalone packaging.

**Recommendation**: Phase 1 keeps cookie/session (status quo) so the
standalone packaging change is orthogonal to the auth model change. Phase 3
adds tokens once gemma-curation-ui's shape is clearer.

---

## 8. Migration phases + effort estimate

Each phase is a single agent-session unit unless noted.

| phase | scope                                                                                       | session estimate |
|------:|---------------------------------------------------------------------------------------------|------------------|
|     1 | `RestSecurityConfig` Java-config — `SecurityFilterChain` bean, `@EnableWebSecurity`, anonymous + http-basic + `/rest/v2/users/**`→GROUP_USER. Wire next to XML, prove it boots gemma-web with both active. | 1                |
|     2 | Delete the `<s:http pattern="/rest/v2/**">` XML block. Verify all Jersey ITs still green. Verify BaseJerseyTest + the 4 IT classes. | 0.5              |
|     3 | Move `restapidocs/` webResources from gemma-web/pom.xml to gemma-rest/pom.xml. Move `InitializeContext` logic (the REST-relevant subset) into a new `ubic.gemma.rest.context.InitializeContext`. | 0.5              |
|     4 | Flip `gemma-rest/pom.xml` to `<packaging>war</packaging>`. Add `src/main/webapp/WEB-INF/web.xml` (gemma-rest-owned). Add tomcat-catalina + tomcat-servlet-api provided deps. Run `mvn -pl gemma-rest package`, verify war contents. | 1                |
|     5 | Add a CI smoke job that deploys the gemma-rest WAR to a stub Tomcat 10.1 and hits `GET /rest/v2/`. Fix anything that breaks. (Likely: missing properties resolution, missing `restAuthEntryPoint` constructor arg, listener gaps.) | 1                |
|     6 | **Cutover**: deploy gemma-rest WAR to staging alongside gemma-web. Cut DNS / load-balancer to gemma-rest standalone. Leave gemma-web running for 1 sprint as fallback. | 0.5 (cutover-only, mostly ops) |
|     7 | Delete gemma-web. (~187 Java files, ~29kLOC, 79 JSPs, 15 XML configs.) Update root `pom.xml` `<modules>`. Verify CI green. | 1                |
|     8 | **Phase 2 — embedded Tomcat.** Add `tomcat-embed-core` dep. Hand-write `ubic.gemma.rest.Main` with programmatic context init + filter registration. Add `maven-shade-plugin` or `spring-boot-maven-plugin`-equivalent for the fat-jar. | 2                |
|     9 | Container image (`Dockerfile`) for gemma-rest standalone. Healthcheck endpoint. Externalized `Gemma.properties` via volume mount or env-var resolution. | 1                |

**Total to "gemma-rest deployable as a WAR with no gemma-web": phases 1–5 ≈
4 agent-sessions.** Phase 6–7 cutover/cleanup adds ~1.5. Phase 8–9
(embedded-jar + container) adds ~3. Grand total to Option B fat-jar with
container image: **~8.5 agent-sessions**.

---

## 9. Open questions

1. **AuthN model for gemma-curation-ui ↔ gemma-rest** — cookie/session
   (simplest port) or JWT/Bearer token (cleaner)? See §7. Recommend deferring
   until standalone packaging lands.
2. **Is the SSR `SecurityController` ACL admin UI going away with gemma-web,
   or being ported to gemma-curation-ui?** MIDDLE_TIER_AUDIT §3.4 raised this
   — 49 `securityService.*` callsites concentrate there. If it's being
   ported, gemma-rest needs new `/rest/v2/security/**` endpoints + their
   `@Secured("GROUP_ADMIN")` annotations. Out of scope for *packaging* but
   blocks the gemma-web deletion (phase 7).
3. **JSESSIONID-bearing endpoints currently called by GemBrow but NOT under
   `/rest/v2/**`**: `/home.html`, `/login.jsp`,
   `/j_spring_security_logout`, `/whatsnew/generateCache.html` (per
   `web.xml:124–127` CORS mapping). When gemma-web dies, GemBrow loses
   these. The most load-bearing one is `/j_spring_security_logout` — needs a
   `/rest/v2/auth/logout` equivalent before phase 7.
4. **Tomcat configuration** — connectors, SSL, HTTP/2, max-threads. Phase 1
   inherits from the existing Tomcat install. Phase 2 (embedded) needs these
   defaults baked into `Main.java` and overridable via `Gemma.properties`.
   Recommend a small `gemma-rest.tomcat.*` namespace in `Gemma.properties`.
5. **Metrics filter parity.** `gemmaWebMetricsFilter` is mapped to the
   `gemma` (DispatcherServlet) servlet only, so REST has no per-request
   metrics today. The standalone deployable is the natural moment to add a
   `restMetricsFilter` mapped to `/rest/v2/*`. Out of scope for *blocking*
   the packaging change.
6. **Two manual `accessDecisionManager.decide(...)` callsites** in
   `DatasetsWebService.java:3082` and `PlatformsWebService.java:318` resolve
   the bean by name. They depend on `httpAccessDecisionManager` being in the
   Spring context. The bean comes from gemma-web's
   `applicationContext-security.xml:9–18` today (defined alongside the
   `<s:http>` blocks). **It must move to gemma-rest or gemma-core when the
   gemma-web XML goes** — recommend moving the `httpAccessDecisionManager`
   bean definition into the new `RestSecurityConfig` Java config in Phase 1.
   Or into gemma-core's `applicationContext-security.xml` if both standalone
   gemma-rest and the surviving gemma-cli stack need it. (gemma-cli does
   not — it uses method security only, no http chain.)
7. **`runAsManager` `gemma.runas.password` provenance.** Already loaded from
   `Gemma.properties` by gemma-core's
   `applicationContext-security.xml:67,73`. Works identically in the
   standalone. Just verify the production `Gemma.properties` keeps shipping
   this key.
