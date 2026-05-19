# Spring 6 / Spring Security 6 / Hibernate 6 deprecation roadmap

Companion to Phase 2 (`@Config` migration: *where* configuration lives) — this
doc covers Phase 3 *what APIs* the configuration uses. Recce-only inventory of
deprecated runtime APIs in the Gemma codebase as of branch
`worktree-spring6-deprecation-hunt` off `phase2-acl-migrate` HEAD `08e760bd`.

## 1. Versions in use

| Component           | Version            | Notes                                    |
|---------------------|--------------------|------------------------------------------|
| Spring Framework    | 6.1.20             | `pom.xml` `spring.version`               |
| Spring Security     | 6.3.10             | `pom.xml` `spring.security.version`      |
| Hibernate ORM       | 6.4.10.Final       | `pom.xml` `hibernate.version`            |
| Jakarta EE          | servlet 6.0, jakarta.xml.bind 4, JAX-RS via Jersey 3.1.10 | `javax.*` flag day done in Phase 2 |
| JUnit               | 4.13.2             | No JUnit 5 / Jupiter on classpath yet    |
| Mockito             | 5.21.0             | Modern; no migration needed              |
| JDK                 | 17 (corretto)      | Bytecode v61                              |

## 2. Inventory by API

Bucket key: **T** trivial rename (one line) · **1F** one-file rework ·
**MF** multi-file · **XM** cross-module.

### Spring Security 6

| API / pattern                       | Count | Replacement                            | Bucket | Notes |
|-------------------------------------|------:|----------------------------------------|--------|-------|
| `WebSecurityConfigurerAdapter`      | 0     | component-style `SecurityFilterChain` bean | —    | Already gone (was XML, never extended in Java) |
| `authorizeRequests`, `antMatchers`  | 0     | `authorizeHttpRequests`, `requestMatchers` | —  | No Java `HttpSecurity` builders in tree; security is XML and owned by another agent |
| `AbstractAuthenticationProcessingFilter` | 0 | (still supported but signature drift) | — | None found |
| `@EnableGlobalMethodSecurity`       | 1     | `@EnableMethodSecurity` (AuthorizationManager) | **MF** | `gemma-core/src/main/java/ubic/gemma/core/security/MethodSecurityConfig.java`. **Deliberately retained** — see in-file docblock. Migration requires re-architecting all 14 `AfterInvocationProvider` beans into post-invocation `MethodInterceptor`s. Out of Phase 3 scope. |
| `AfterInvocationProviderManager`    | 1     | custom post-invocation `MethodInterceptor` | **XM** | Same call site as above; `@SuppressWarnings("deprecation")` already applied. |
| `@Secured("GROUP_ADMIN")`           | ~25   | `@PreAuthorize("hasAuthority(...)")`   | **MF** | `@Secured` is *not* deprecated, but is the older idiom. Mixing `@Secured` + `@PreAuthorize` works; consolidating is hygiene, not deprecation. Concentrated in `gemma-rest/`. |

### Spring Framework 6

| API / pattern                       | Count | Replacement                            | Bucket | Notes |
|-------------------------------------|------:|----------------------------------------|--------|-------|
| `extends ResponseEntityExceptionHandler` | 0 | (signature change in 6)            | —      | Not used |
| `WebMvcConfigurer` removed methods  | 0     | new signatures                         | —      | None found |
| `ResourceUrlProvider`, `ResourceUrlEncodingFilter` | 0 | new resolver chain        | —      | Not used |
| `JdbcTemplate` deprecated overloads (`sql, Object[], …`) | 0 | varargs overloads | —    | All 10 `jdbcTemplate.*` call sites use modern varargs |
| `CommonsMultipartResolver`          | 0     | `StandardServletMultipartResolver`     | —      | Already migrated (`gemma-servlet.xml:98`) |
| `AsyncRestTemplate`, `ListenableFuture` | 0  | `CompletableFuture` / `WebClient`     | —      | Not used |

### Hibernate 6

| API / pattern                       | Count | Replacement                            | Bucket | Notes |
|-------------------------------------|------:|----------------------------------------|--------|-------|
| `org.hibernate.Criteria`            | 0     | JPA `CriteriaBuilder`                  | —      | Already migrated in Phase 2 (see `QueryUtils`, `AbstractCriteriaFilteringVoEnabledDao` docblocks) |
| `org.hibernate.criterion.*`         | 0     | JPA Criteria API                       | —      | Already gone |
| `Session.load(Class, id)`           | 7     | `Session.getReference(Class, id)`      | **1F per call** | Deprecated since HB 6.0; call sites: `ExpressionPersister:238`, `DifferentialExpressionResultDaoImpl:193`, `ExpressionAnalysisResultSetDaoImpl:433/434`, `CharacteristicDaoImpl:231`, `QuantitationTypeDaoImpl:110`, `ExpressionExperimentDaoImpl:2930`. Pure mechanical rename, but semantics differ slightly: `getReference` returns a proxy without forcing eager initialization. Verify each call site doesn't rely on `load`-style proxy unwrapping behaviour. |
| `createSQLQuery`                    | 0     | `createNativeQuery`                    | —      | Already migrated |
| `import org.hibernate.Query`        | 0     | `org.hibernate.query.Query`            | —      | 30 imports of the new `org.hibernate.query.Query`; legacy interface gone. |
| `@Type(type="…")` string form       | 0     | `@Type(value=…)` class form            | —      | No `@Type` annotations in Java code (custom types registered via `applicationContext-hibernate.xml`) |
| `MySQL5InnoDBDialect` dialect class | 1 (props), 0 (Java) | `MySQLDialect` w/ `DatabaseVersion` | **T (done)** | `gemma-core/src/main/resources/hibernate.properties` referenced the removed class. Runtime is overridden by `applicationContext-hibernate.xml` to `ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect` (a lab shim). Trivial rename applied below. |
| `TypedQuery.setMaxResults(-1)` special-case | 0 | drop the call                  | —      | Not used |
| `HibernateTemplate`, `HibernateDaoSupport` | 0 | direct `SessionFactory.getCurrentSession()` | — | Clean — already using session-direct pattern |
| `UserType<T>` (HB 6 generic form)   | 2     | (already generic)                      | —      | `CompressedStringListType`, `ByteArrayType` already implement the HB6 generic `UserType<T>`. No work. |

### Java / Jakarta EE 9+

| API / pattern                       | Count | Replacement                            | Bucket | Notes |
|-------------------------------------|------:|----------------------------------------|--------|-------|
| `import javax.servlet`              | 0     | `jakarta.servlet`                      | —      | Done |
| `import javax.persistence`          | 0     | `jakarta.persistence`                  | —      | Done |
| `import javax.validation`           | 0     | `jakarta.validation`                   | —      | Done |
| `import javax.ws.rs`                | 0     | `jakarta.ws.rs`                        | —      | Done |
| `import javax.annotation.*`         | 712   | (see note)                             | **XM** | **NOT a Jakarta-migration target.** These are JSR-305 (`com.google.code.findbugs:jsr305`) `@Nullable` / `@Nonnull` / `@ParametersAreNonnullByDefault` / `@CheckReturnValue`. JSR-305 itself is moribund. Modern options: (a) stay on JSR-305 (unmaintained but works); (b) migrate to `org.springframework.lang.{Nullable,NonNull}` (zero new deps, Spring-aligned); (c) migrate to JSpecify (`org.jspecify.annotations.*`) once stable. Breakdown: 571 `@Nullable`, 126 `@ParametersAreNonnullByDefault`, 33 `@Nonnull`, 21 `@CheckReturnValue`, 7 `@OverridingMethodsMustInvokeSuper`, 3 `@WillClose`. **Not deprecated by Spring 6 itself** — separate hygiene story. |

### Test infrastructure

| API / pattern                       | Count | Replacement                            | Bucket | Notes |
|-------------------------------------|------:|----------------------------------------|--------|-------|
| `import org.junit.Test` (JUnit 4)   | 399   | `org.junit.jupiter.api.Test` (JUnit 5) | **XM** | 334 gemma-core, 23 gemma-web, 21 gemma-rest, 21 gemma-cli. Big migration. JUnit 4 is *not* Spring-6-incompatible — `spring-test` retains JUnit 4 support — so this is hygiene/modernization, not a forcing function. |
| `@RunWith(SpringJUnit4ClassRunner.class)` | 1 | `@ExtendWith(SpringExtension.class)` | **1F** | `gemma-rest/src/test/java/ubic/gemma/rest/util/BaseJerseyTest.java:38`. Only when migrating the file to Jupiter. |
| `@RunWith(Categories.class)`, `@RunWith(ClasspathSuite.class)`, `@RunWith(Parameterized.class)` | 8 | JUnit 5 `@Suite`, `@ParameterizedTest` | **MF** | `gemma-core/src/test/.../suite/` (test suites) + 1 `Parameterized` test. Tied to JUnit 5 migration. |

## 3. Hotspot files

The five files with the highest deprecation density (sum across all rows above):

| File                                                                         | Hits | Why |
|------------------------------------------------------------------------------|-----:|-----|
| `gemma-core/src/main/java/ubic/gemma/core/security/MethodSecurityConfig.java` | 1+   | Sole `@EnableGlobalMethodSecurity` + `AfterInvocationProviderManager` user; documented deliberate hold |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentDaoImpl.java` | 2 | 2× `Session.load`, plus `LockMode` import (not deprecated) |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/analysis/expression/diff/ExpressionAnalysisResultSetDaoImpl.java` | 2 | 2× `Session.load` |
| `gemma-rest/src/test/java/ubic/gemma/rest/util/BaseJerseyTest.java`           | 1    | `@RunWith(SpringJUnit4ClassRunner)` — gates the entire gemma-rest test base class |
| `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java`            | ~10  | High `@Secured` density (REST endpoints) — hygiene migration |

## 4. Trivial renames applied in this commit

1. **`gemma-core/src/main/resources/hibernate.properties`** — changed the
   dialect line from the removed `org.hibernate.dialect.MySQL5InnoDBDialect`
   to the lab's HB6-compatible shim
   `ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect`. The runtime
   dialect is set by `applicationContext-hibernate.xml` (which already uses
   the shim); this file is only loaded by tooling that consults
   `hibernate.properties` directly (schema-export), so the rename closes a
   dormant landmine without changing observed runtime behaviour. Verified
   `mvn -pl gemma-core -am compile -DskipTests` clean.

No other trivial rename targets exist: every `antMatchers` / `authorizeRequests`
candidate is in `applicationContext-security.xml` (owned by another agent), and
the `@EnableGlobalMethodSecurity` annotation is a deliberate hold (not a
trivial rename — see in-file docblock).

## 5. Migration order recommendation

Priority order, weighted by (a) blast radius, (b) dependency on Phase 3 XML
security migration, (c) effort.

| # | Migration                                            | Why first / why later |
|---|------------------------------------------------------|-----------------------|
| 1 | **Wait for security XML → Java migration to land**   | Once `applicationContext-security.xml` becomes Java config, `antMatchers` / `authorizeRequests` deprecations will appear *in the new Java config* — fix them in the same agent's commit, not retrofitted later. Don't pre-empt. |
| 2 | **`Session.load` → `Session.getReference`** (7 sites) | Pure mechanical, all in `gemma-core/.../persistence/service/`. Risk: `getReference` returns a proxy without eager init; review each call site to confirm none relies on `load`'s subtle behaviour difference (e.g., `ObjectNotFoundException` vs `EntityNotFoundException` timing). One PR, one reviewer, finite. |
| 3 | **`@Secured` → `@PreAuthorize` consolidation** in `gemma-rest/` | ~25 sites, all in REST endpoint annotations. Mechanical; no runtime behaviour change (both walk through `MethodSecurityInterceptor`). Sets up for eventual `@EnableMethodSecurity` migration (item 6). |
| 4 | **JSR-305 (`javax.annotation.*`) → Spring `lang.NonNull`/`Nullable`** | 712 imports, but pure search-and-replace and zero risk. Defer until tooling (IntelliJ + Checker Framework + javac) settles around JSpecify; doing it via Spring's `lang.*` package is the conservative bet. Optional. |
| 5 | **JUnit 4 → JUnit 5** (399 files)                    | Big lift, no Spring-6 forcing function. Best done module-by-module with `junit-vintage-engine` in the classpath during transition. Coordinate with the test-fixture migration (Phase 3 `TEST_FIXTURE_FACTORIES.md`) so we're not rewriting the same files twice. |
| 6 | **`@EnableGlobalMethodSecurity` → `@EnableMethodSecurity`** | Documented hold. Requires porting all 14 `AfterInvocationProvider` beans to post-invocation `MethodInterceptor`s. Large architectural change, no functional benefit beyond removing a deprecation warning that's already `@SuppressWarnings`'d. Defer until the AfterInvocation provider list is otherwise being touched (e.g., ACL Phase 3 follow-up). |

## 6. Open questions

1. **JSpecify vs Spring `lang.*` vs JSR-305.** No urgency, but pick a target
   nullness vocabulary before any large refactor. Recommendation: Spring
   `lang.*` short-term, JSpecify long-term once Hibernate / Mockito / Spring
   all converge on it.
2. **JUnit 5 timing.** Is there a Phase 3 milestone where the test-fixture
   refactor reaches enough mass that JUnit 4 → 5 should ride along? If yes,
   coordinate; if no, defer.
3. **`@EnableMethodSecurity` migration path.** The 14
   `AfterInvocationProvider` beans encode read-time filtering on ACL-protected
   value objects. A future-state `AuthorizationManager` design would need a
   post-invocation `MethodInterceptor` that walks the same provider list — or
   a wholesale rewrite of ACL filtering into the new authorization SPI. This
   is a multi-week design task; not a Phase 3 candidate.
4. **`hibernate.properties` dormancy.** The runtime dialect is set in XML; the
   properties file is only consulted by schema-export tooling. Should the
   file be deleted once the XML→`@Config` migration completes? Probably yes;
   the dialect rename in this commit is a band-aid until that decision lands.

## Provenance

- Branch: `worktree-spring6-deprecation-hunt` off `phase2-acl-migrate@08e760bd`
- Date: 2026-05-18
- Method: grep-based static survey of `*.java`, `*.xml`, `*.properties`
- Compile check: `mvn -pl gemma-core -am compile -DskipTests` clean after the
  one trivial rename above.
