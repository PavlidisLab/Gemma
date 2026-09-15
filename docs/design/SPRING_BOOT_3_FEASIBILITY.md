# Spring Boot 3 — Feasibility Recce

**Date:** 2026-05-18
**Baseline:** `phase2-acl-migrate` @ `08e760bdaf`
**Status:** Recce only. No code changes.
**Scope:** Should Gemma adopt Spring Boot 3.x on top of the recently-completed Phase 2 / Phase 3 Spring 6 work?

---

## 1. Current state inventory

| Aspect | Value |
|---|---|
| Spring Framework | 6.1.20 (6.2.x bump pending on unmerged branch) |
| Spring Security | 6.3.10 (with `@EnableGlobalMethodSecurity` — legacy form) |
| Hibernate | 6.4.10.Final — **native bootstrap** via `HibernateSessionFactoryBean` (subclass), NOT JPA |
| Transaction mgr | `org.springframework.orm.hibernate5.HibernateTransactionManager` |
| DataSource | HikariCP, wired via `applicationContext-dataSource.xml` |
| Cache | JCache (`JCacheCacheManager`) backed by Ehcache 3.10 jakarta |
| Servlet container | Tomcat 10.1.34 — **external WAR deploy** |
| REST | Jersey 3.1.10 (jakarta) — **not** Spring MVC |
| Metrics | Micrometer 1.13.11 with JMX backend |
| Packaging | `gemma-web=war`, `gemma-rest=jar`, `gemma-core=jar`, `gemma-cli=jar` |
| Property roots | `gemma.db.*`, `gemma.hibernate.*`, `gemma.*` (in `default.properties` + `Gemma.properties`) |
| XML configs | ~14 `applicationContext-*.xml` files still present across modules (component-scan, dataSource, hibernate, security, serviceBeans, schedule, analytics, metrics) |
| Java configs | 30+ `@Configuration` classes (mostly feature-scoped, not infrastructure-scoped) |
| web.xml | Hand-rolled: 6 `<filter>`s, `StartupListener`, `IntrospectorCleanupListener`, Jersey + Spring DispatcherServlet, SiteMesh |
| Tests | 117 files use `@ContextConfiguration` (zero use `@SpringBootTest`) |
| Profiles | `production`, `dev`, `test`, `testdb`, `web`, `cli`, `scheduler`, `metrics`, `profiling` |
| Boot compatibility floor | Boot 3.0+ requires SF 6.0+, Boot 3.4 requires SF 6.2 — Gemma is already inside the window |

---

## 2. Boot's value-add — specific to Gemma

What Boot would actually buy us, ranked by realistic ROI:

### 2a. Embedded Tomcat + actuator (highest ROI)
- `spring-boot-starter-tomcat` collapses external Tomcat install + WAR deploy into `java -jar gemma-web.jar`.
- Eliminates the `tomcat.version=10.1.34` coordination problem (currently the version is pinned in a `<dependencyManagement>` block and must track ops Tomcat).
- `spring-boot-starter-actuator` provides `/actuator/health`, `/actuator/metrics`, `/actuator/info`, `/actuator/prometheus` out of the box. Unblocks the metrics-profile cleanup that the metrics audit flagged (custom JMX wiring → Micrometer endpoint).
- Container-friendly: a fat jar slots into a Docker image / k8s deployment trivially, where WAR-on-external-Tomcat is awkward.

### 2b. Externalized config via `application.yml` + profiles
- Boot's `application-{profile}.yml` model is strictly more powerful than the current `default.properties` + Spring `PropertyPlaceholderConfigurer` chain.
- Type-safe `@ConfigurationProperties` for the `gemma.*` namespace, with IDE autocomplete (via `spring-configuration-metadata.json`).
- Profile activation via `SPRING_PROFILES_ACTIVE` env var lines up with existing deployment patterns.

### 2c. Curated BOM / dependency convergence
- `spring-boot-dependencies` BOM pins ~400 third-party versions to known-compatible combinations (Jackson, Hibernate, Tomcat, Micrometer, Logback, etc.).
- Today the parent `pom.xml` manually pins ~30 versions; every minor bump requires hand-checking compatibility. Boot's BOM moves this to "trust upstream".
- Note: Gemma can adopt the BOM as `<dependencyManagement><scope>import</scope>` **without** using `spring-boot-starter-parent` — preserves the existing parent-pom layout.

### Honourable mention — auto-configuration
Less useful for Gemma than the marketing suggests:
- `DataSourceAutoConfiguration`: works but reads `spring.datasource.*` not `gemma.db.*`. Either rename the property tree (breaking ops change) or keep manual `DataSource` bean.
- `HibernateJpaAutoConfiguration`: **does not apply** — see friction 3a below.
- `SecurityAutoConfiguration`: would short-circuit Gemma's custom ACL + method security setup; we'd disable it.

Realistically, ~60% of Gemma's wiring is too project-specific to delegate to auto-config. The win is `EmbeddedTomcatAutoConfiguration` + actuator, not the big stuff.

---

## 3. Costs / friction

### 3a. Hibernate native vs JPA bootstrap — the load-bearing one
`applicationContext-hibernate.xml` documents this explicitly (lines 10–21):

> Bootstrap path: native Hibernate Configuration via HibernateSessionFactoryBean (not JPA). We tried the JPA-bootstrap approach … but Hibernate's JPA bootstrap hard-codes `hibernate.current_session_context_class=jpa` which breaks `SessionFactory.getCurrentSession()` through Spring's SpringSessionContext bridge to HibernateTransactionManager. Gemma's DAOs use getCurrentSession() throughout, so we go native.

Boot's `HibernateJpaAutoConfiguration` is JPA-only — it builds a `LocalContainerEntityManagerFactoryBean` + `HibernateJpaVendorAdapter` + `JpaTransactionManager`. That's exactly the shape Phase 2 already rejected for correctness reasons.

**Options under Boot:**
1. `@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})` and keep the existing `HibernateSessionFactoryBean` + `HibernateTransactionManager`. Works, but loses most JPA-flavoured Boot benefits (Spring Data JPA repositories, `@DataJpaTest` slicing, etc. — none of which Gemma uses anyway, so the loss is theoretical).
2. Switch every DAO from `sessionFactory.getCurrentSession()` to `entityManager.unwrap(Session.class)`. Touches ~hundreds of DAO methods. Out of scope for this push.

**Verdict:** option 1 is fine. Bootstrap stays native; Boot just wraps it. But this means a meaningful chunk of Boot's "magic" is opt-out from day one.

### 3b. Jersey vs Spring MVC
Gemma uses Jersey 3.1 for REST (`gemma-rest` module). Boot supports this via `spring-boot-starter-jersey`, but:
- The starter is less polished than `spring-boot-starter-web` (Spring MVC) — fewer auto-config knobs, fewer `@WebMvcTest`-style test slices.
- Some Boot features (Springdoc OpenAPI auto-detection, `WebMvcConfigurer` integration) don't apply to Jersey paths.
- Existing `JerseyConfig` + `applicationContext-analytics.xml` would need to be reshaped to Boot's `ResourceConfig` registration model.

Not a blocker — `spring-boot-starter-jersey` exists and works — but it's a lower-status path in the Boot ecosystem.

### 3c. WAR → jar packaging + web.xml dissolution
`gemma-web` is `<packaging>war</packaging>` with a hand-built `web.xml` (6 filters, 2 listeners, sitemesh, Jersey servlet, Spring `DispatcherServlet`). Boot expects:
- Jar packaging (or "executable WAR" via `spring-boot-starter-tomcat` + `provided`).
- web.xml replaced by programmatic registration: `@Bean FilterRegistrationBean`, `ServletContextInitializer`, etc.
- `StartupListener` becomes `ApplicationListener<ApplicationReadyEvent>` or `@Component implements CommandLineRunner`.

This is ~1–2 days of mechanical rewrite, not a fundamental conflict. But it's churn on top of the just-completed Phase 2 wiring rationalisation.

### 3d. Test infrastructure migration cost (medium)
117 test classes use `@ContextConfiguration(locations=...)` pointing at `applicationContext-*.xml` test variants. Boot tests typically use `@SpringBootTest` + `@TestConfiguration`. Migration paths:
- Keep `@ContextConfiguration` — Boot supports this; tests would continue working unchanged.
- Migrate to `@SpringBootTest(classes = GemmaApplication.class)` — better tooling but rewrites 117 files.

**Verdict:** keep `@ContextConfiguration` as the migration default; convert opportunistically. Avoid a forced rewrite.

### 3e. CLI module
`gemma-cli` has its own `GemmaCLI.main()` that bootstraps a Spring context. Boot has `CommandLineRunner` / `ApplicationRunner`, and `SpringApplication` works fine without a servlet container. Migration is straightforward but mandatory if the parent moves to Boot conventions.

### 3f. Property-key churn risk
Boot's auto-config keys (`spring.datasource.url`, `spring.jpa.properties.hibernate.*`, `management.endpoints.*`) are different from Gemma's (`gemma.db.url`, `gemma.hibernate.*`). If we disable auto-config (per 3a) the conflict is avoided. If we adopt auto-config piecewise, every adopted bit needs ops-side rename coordination.

---

## 4. Phased adoption path

Three-phase option, each independently shippable:

### Phase 0 — preconditions (already mostly done)
- Spring Framework ≥ 6.1 (✓ 6.1.20; 6.2 bump pending).
- Spring Security ≥ 6.x (✓ 6.3.10).
- Hibernate 6.x jakarta (✓ 6.4.10).
- Jakarta EE namespace migration (✓ done in Phase 2).
- Phase 2 XML→Java config migration **complete** (currently ~50% — finish first).

### Phase 1 — adopt Boot BOM only (1 sprint, low risk)
- Add `spring-boot-dependencies` as a `<dependencyManagement>` import in parent `pom.xml`.
- Drop manually-pinned versions for anything covered by the BOM (Jackson, Logback, Tomcat, Hibernate, Micrometer, …).
- **Do not** add `@SpringBootApplication` or any Boot starters yet.
- Win: dependency convergence + smaller `pom.xml`. No runtime behavior change.

### Phase 2 — embedded Tomcat for gemma-web (1–2 sprints, medium risk)
- Add `spring-boot-starter-tomcat` + `spring-boot-starter` (minimal — not `-web`).
- Write `GemmaApplication` with `@SpringBootApplication(exclude = {DataSourceAutoConfiguration, HibernateJpaAutoConfiguration, SecurityAutoConfiguration})` so the existing wiring is untouched.
- Migrate `web.xml` filters/listeners to `FilterRegistrationBean` / `ServletContextInitializer`.
- Switch `gemma-web` packaging to executable jar (or stay WAR with `spring-boot-maven-plugin` repackage goal).
- Add `spring-boot-starter-actuator`; expose `/actuator/health`, `/actuator/prometheus`. Retire custom JMX metrics wiring.
- Win: container-friendly deployment + actuator endpoints + metrics cleanup.

### Phase 3 — opportunistic auto-config adoption (open-ended)
- Where a Gemma `@Configuration` class is a near-duplicate of a Boot auto-config (e.g. `JacksonConfig`, basic task scheduler), delete the explicit class and accept the auto-config.
- Keep the load-bearing bits explicit: DataSource, Hibernate, Security, Jersey, ACL.
- Win: less code, but the marginal gain shrinks fast because most of Gemma's config is genuinely project-specific.

### OR — defer indefinitely
The case for **not** adopting Boot:
- Native Hibernate bootstrap means we'd disable Boot's most impactful auto-config from day one.
- Jersey (not Spring MVC) means the second-most-impactful starter is the awkward one.
- Phase 2 just finished a major `@Configuration` migration — Boot would partially obsolete that work.
- gemma-curation-ui handles the modern frontend; gemma-web is on the deprecation glidepath. Embedding Tomcat in a soon-to-be-retired module is wasted effort.
- gemma-rest standalone Phase 1 (separately tracked) already delivers the "modular container-friendly deployment" win without requiring Boot.

**Defer recommendation is defensible** if the embedded-Tomcat + actuator wins are met by `gemma-rest` Phase 1 and a small custom `MicrometerEndpointConfig`.

---

## 5. Recommendation

**Adopt Phase 1 (BOM-only) opportunistically. Defer Phase 2 / Phase 3 until gemma-web's fate is settled.**

Rationale:
- Phase 1 is cheap, low-risk, and pays back via dependency-version sanity.
- Phase 2's main win (embedded Tomcat for gemma-web) lands on a module that gemma-curation-ui is replacing. Investing 1–2 sprints to modernise a module on the deprecation path is hard to justify.
- Phase 2's secondary win (actuator) can be backported via a thin Micrometer config without taking the Boot dependency.
- Phase 3's auto-config wins are blocked by the native-Hibernate constraint, which is itself load-bearing and not changing.

If gemma-web's life is extended (decision pending), revisit Phase 2.

---

## 6. Open questions for Paul

1. **Is gemma-web definitively on the retirement path?** If yes, Boot's biggest win (embedded Tomcat for gemma-web) is wasted. If "maybe", Phase 2 becomes worth scoping.
2. **gemma-rest standalone Phase 1 — does it land before or after this decision?** If the standalone REST app ships first and delivers embedded-Tomcat-style deploys via a different mechanism, Boot's value proposition shrinks further.
3. **Are there ops-side asks that would change the calculus?** Specifically: `/actuator/prometheus` endpoint, container-native deploys, `application.yml`-style config. If any of these are blocked-pending-Boot, Phase 2 becomes priority.
4. **Property-tree rename: `gemma.db.* → spring.datasource.*`?** If we ever want full auto-config, this rename is on the path. Worth a separate doc if seriously considered.
5. **CLI module: does GemmaCLI benefit from Boot conventions?** `SpringApplication.run()` + `CommandLineRunner` is genuinely cleaner than the current bespoke main. Could be the smallest viable Boot pilot if we want a low-stakes proof-of-value.
6. **DAO migration to `EntityManager`?** Out of scope here, but if there's ever an appetite to migrate the ~hundreds of `sessionFactory.getCurrentSession()` calls to `@PersistenceContext EntityManager em`, the native-Hibernate constraint goes away and Boot's JPA auto-config becomes available. Multi-month effort; flag for the long-term roadmap only.

---
*Recce author: agent-spring-boot-3-recce. Read-only on source. No `mvn` executed.*
