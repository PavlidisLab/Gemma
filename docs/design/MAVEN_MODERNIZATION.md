# Maven Plugin + Dependency Modernization

Phase 3 build hygiene pass. Audited every module POM (`pom.xml`,
`gemma-core/pom.xml`, `gemma-cli/pom.xml`, `gemma-rest/pom.xml`,
`gemma-web/pom.xml`) for drift against current upstream stable, then
applied the trivial / low-risk bumps. Spring, Spring Security,
Hibernate, Jakarta EE majors-and-minors are intentionally left for
dedicated migration sessions.

## Method

Inventory was driven by `mvn versions:display-plugin-updates` and
`mvn versions:display-dependency-updates` on the full reactor. Each
proposed bump was applied, the reactor was re-compiled
(`mvn compile -DskipTests` + `mvn test-compile -DskipTests`), and any
bump that broke `dependencyConvergence` (the
`maven-enforcer-plugin` rule reinstated in Phase 2 Step 9) was
reverted. No source files were touched.

`mvn verify` was deliberately **not** run — `gemdtest` is in use by
other work in flight.

## Plugin versions

| Plugin | Current | Target | Status | Reason |
|---|---|---|---|---|
| maven-enforcer-plugin | 3.6.2 | 3.6.3 | **applied** | patch |
| maven-surefire-plugin | 3.5.4 | 3.5.5 | **applied** | patch |
| maven-failsafe-plugin | 3.5.4 | 3.5.5 | **applied** | patch |
| maven-resources-plugin | 3.4.0 | 3.5.0 | **applied** | minor, stable |
| dependency-check-maven (OWASP) | 12.2.0 | 12.2.2 | **applied** | patch |
| git-commit-id-maven-plugin | 9.0.2 | 10.0.0 | **deferred** | major bump — different config keys; revisit when explicitly modernizing CI metadata |

All other reactor plugins (maven-compiler 3.15.0, maven-jar 3.5.0,
maven-source 3.4.0, maven-javadoc 3.12.0, maven-war 3.5.1,
maven-site 3.21.0, maven-clean 3.5.0, maven-deploy 3.1.4,
maven-install 3.1.4, maven-project-info-reports 3.9.0,
maven-dependency-plugin 3.10.0, versions-maven-plugin 2.21.0,
appassembler-maven-plugin 2.1.0, build-helper-maven-plugin 3.6.1,
exec-maven-plugin 3.6.3, antlr4-maven-plugin 4.13.2,
lombok-maven-plugin 1.18.20.0, gitflow-maven-plugin) are already on
latest stable or pinned for reasons documented in the POM.

## Dependency versions

### Applied (trivial / low-risk)

| Dependency | Current | Target | Module | Notes |
|---|---|---|---|---|
| jackson (core / databind / jsr310 / yaml) | 2.21.0 | 2.21.3 | root via property | patch within 2.21 |
| swagger (annotations + jakarta variants) | 2.2.42 | 2.2.50 | root via property | patch |
| slack-api-client / slack-api-model | 1.47.0 | 1.49.0 | root via property | minor, no API changes consumed |
| mockito-core | 5.21.0 | 5.23.0 | root via property | minor, low-risk |
| jakarta.xml.bind-api | 4.0.2 | 4.0.5 | root + gemma-core | patch within 4.0 |
| jaxb-runtime | 4.0.5 | 4.0.8 | root | patch within 4.0 |
| ant | 1.10.15 | 1.10.17 | root | patch |
| yauaa | 7.31.0 | 7.32.0 | gemma-web | minor |
| junit-vintage-engine | 5.11.4 | 5.14.4 | gemma-rest | minor; only used so JUnit 4 tests run under JUnit Platform |

### Added (transitive pin)

| Dependency | Version | Why |
|---|---|---|
| com.google.errorprone:error_prone_annotations | 2.40.0 | yauaa 7.32 (via caffeine 3.2.2) and slack 1.49 (via gson 2.12.1) pull different errorprone versions; pinning to the newer satisfies dependencyConvergence. |

### Deferred (blocked by baseCode 1.1.34-SNAPSHOT)

These bumps trigger `dependencyConvergence` because `baseCode` (a
Pavlab dependency) transitively pins the older version. Revisit once
baseCode is rebuilt against the newer libraries.

| Dependency | Current | Available | Blocked by |
|---|---|---|---|
| commons-codec | 1.20.0 | 1.22.0 | baseCode transitive 1.20.0 |
| commons-io | 2.21.0 | 2.22.0 | baseCode transitive 2.21.0 |
| commons-net | 3.12.0 | 3.13.0 | baseCode transitive 3.12.0 (gemma-core) |
| commons-configuration2 | 2.13.0 | 2.15.0 | baseCode transitive 2.13.0 |

### Deferred (parent-managed, conservative)

These come from `pavlab-starter-parent` 1.2.29 and updates may need
to land in the parent first to avoid losing other downstreams. Low
priority since they're all transitives within the same major.

| Dependency | Current | Available | Notes |
|---|---|---|---|
| log4j (core / api / slf4j-impl / jcl / jul / web) | 2.25.3 | 2.26.0 | managed by parent |
| jboss-logging | 3.6.2.Final | 3.6.3.Final | managed by parent |
| commons-logging | 1.3.5 | 1.3.6 | managed by parent |
| lombok | 1.18.42 | 1.18.46 | managed by parent |

### Off-limits (separate migration projects)

| Dependency | Current | Available | Why deferred |
|---|---|---|---|
| spring-framework | 6.1.20 | 6.2.18 | Spring 6.1 -> 6.2 is its own migration; defer until Phase 3 stabilizes |
| spring-security | 6.3.10 | 6.5.10 | Two minor bumps; security framework re-audit needed |
| hibernate-core / hibernate-jcache | 6.4.10.Final | 6.6.50.Final | HB 6.4 -> 6.6 brings TenantId, ColumnTransformer behavior changes; Phase 3 expects HB6.4 |
| ehcache (jakarta) | 3.10.8 | 3.12.0 | L2 cache; behavioral risk |
| jakarta.servlet-api | 6.0.0 | 6.1.0 | container coupling (Tomcat 10.1) |
| tomcat (catalina / jsp-api / servlet-api) | 10.1.34 | 10.1.55 | production deployment constraint |
| mysql-connector-j | (parent) | n/a | test config drift risk (per task) |
| HDF5 | 1.12.3 | n/a | native library lock-in |

## Verification

```
mvn compile -DskipTests        # PASS, ~4s
mvn test-compile -DskipTests   # PASS, ~44s
```

dependencyConvergence enforcer rule passed across all 5 modules with
the new `error_prone_annotations` pin in place.

## Recommendations / next steps

1. **baseCode refresh** — release a new baseCode that brings
   commons-codec / commons-io / commons-net / commons-configuration2
   to current. That unblocks four trivial bumps here in one move.
2. **pavlab-starter-parent 1.3** — when that lands, drop the
   `<pluginManagement>` overrides at the bottom of root `pom.xml`
   (already TODO-commented) and pick up log4j 2.26 +
   jboss-logging 3.6.3 + lombok 1.18.46 transitively.
3. **Spring 6.1 -> 6.2** — dedicated session. The big-ticket changes
   are: deprecated `LocalSessionFactoryBean` constructor variants,
   new `RestClient` API (we don't use it), `@Async` propagation
   tightening, JdkClientHttpRequestFactory promotions. Run the
   reactor's full test suite under both versions for regression.
4. **Spring Security 6.3 -> 6.5** — two minor bumps. 6.4 added
   `WebAuthn`, OAuth2 token introspection client filters; 6.5
   tightened CSRF token loading. Mostly additive but the SS test
   matchers have changed signatures.
5. **Hibernate 6.4 -> 6.6** — wait until Phase 3 stabilizes;
   `@TenantId`, `@SoftDelete` upgrades, and the `H2Dialect` schema
   metadata changes need an integration-test pass.
6. **git-commit-id 9 -> 10** — 10.x changed `<includeOnlyProperties>`
   to a more permissive matcher and moved several configuration
   keys; review against gemma-core's `<configuration>` block.

## Commits

- `9c6b044234` — Phase 3 build: bump maven plugin versions (trivial)
- `43a98535bb` — Phase 3 build: bump dependency versions (low-risk)
