# Phase 3 framework bump — feasibility recce

Branch: `worktree-framework-bump-recce`
Baseline: `08e760bdaf` (`phase2-acl-migrate` HEAD at session start)
Date: 2026-05-18
Scope: Spring Framework / Spring Security / Hibernate minor-version bumps that the Maven plugin audit (`worktree-maven-modernize`, doc `MAVEN_MODERNIZATION.md`) explicitly deferred.

## 1. Current versions

Resolved from `pom.xml` lines 1022–1024:

| property | before | after (applied) |
|---|---|---|
| `spring.version` | 6.1.20 | **6.2.8** |
| `spring.security.version` | 6.3.10 | **6.5.1** |
| `hibernate.version` | 6.4.10.Final | **6.6.18.Final** |

Latest stable Maven Central versions verified via the `solrsearch` REST API at recce time.

## 2. Per-framework deprecation inventory

### Spring Framework 6.1 → 6.2

| candidate symbol | hits in `gemma-*/**/*.java` |
|---|---|
| `AbstractHttpMessageConverter` (overrides) | 0 |
| `ResponseStatusException` constructors | 0 |
| `RestTemplate` (instantiations) | 2 (`GoogleAnalytics4Provider`, one test) — still supported in 6.2, only "discouraged" |
| `SimpleAsyncTaskExecutor` (in main) | 0 (test-only; JDK17 — VirtualThread path not applicable) |
| `WebMvcConfigurer` (extends/implements) | 0 |

No code-level changes required.

### Spring Security 6.3 → 6.5

| candidate symbol | hits |
|---|---|
| `WebSecurityCustomizer` | 0 |
| `AuthenticationManagerBuilder` | 0 |
| `JdbcMutableAclService` (constructor call) | 1 in `GemmaAclConfiguration` (don't-touch list applies to `AclLinterServiceImpl`, not this file). Constructor signature unchanged between 6.3 → 6.5. |
| `ProviderManager` | test-only + one comment reference |
| **NEW** `org.springframework.security.access.ConfigAttribute` deprecation surfaced by bump | 1 — `FilteringService.java:67` |
| Already-known-deprecated `AccessDecisionManager` / `AfterInvocationProviderManager` | several — already commented in `MethodSecurityConfig` as "deprecated in Spring 6 but still functional" |

The new `ConfigAttribute` deprecation is the same legacy method-security infrastructure already flagged. Not new territory; not a blocker.

### Hibernate 6.4 → 6.6

| candidate symbol | hits |
|---|---|
| `@Type` (string-form) | 0 |
| `StandardBasicType` extensions | 0 |
| `UserType<>` implementations | 2 (`CompressedStringListType`, `ByteArrayType`) — already use the 6.x typed form |
| `createSQLQuery` (deprecated alias) | 0 |
| `createNativeQuery` (Jakarta-standard) | 85 |
| `@Audited` / envers | 0 |

Only Hibernate deprecation in the tree (`org.hibernate.transform.Transformers` in `ExpressionExperimentDaoImpl:97`) was already present at 6.4.10 — not introduced by 6.6.

## 3. Compat test results (Step 4 matrix)

JDK17 amazon-corretto, `mvn clean test-compile -T 1C`. Convergence enforced by `enforcer:enforce` / `DependencyConvergence`.

| trial | spring | security | hibernate | depMgmt augmented? | result |
|---|---|---|---|---|---|
| baseline | 6.1.20 | 6.3.10 | 6.4.10.Final | no | SUCCESS (40s) |
| 4a-naive | 6.2.8 | 6.3.10 | 6.4.10.Final | no | **FAIL** (8 convergence errors: spring-core/beans/context/aop/jdbc/tx/expression/context-support split 6.1.20 ↔ 6.2.8 between gsec & spring-security transitives) |
| 4a-fixed | 6.2.8 | 6.3.10 | 6.4.10.Final | yes (spring-* pinned) | **SUCCESS** (91s) |
| 4b-naive | 6.1.20 | 6.5.1 | 6.4.10.Final | (spring-* only) | **FAIL** (2 convergence errors: spring-security-core/acl split between gsec 6.3.10 & ours 6.5.1) |
| 4b-fixed | 6.1.20 | 6.5.1 | 6.4.10.Final | yes (spring-* + sec-*) | SUCCESS at compile — **but unsupported at runtime: Spring Security 6.5 depends on Spring Framework 6.2 APIs.** Don't ship this combo. |
| 4c-naive | 6.1.20 | 6.3.10 | 6.6.18.Final | (spring-* + sec-*) | **FAIL** (2 convergence errors: hibernate-core/jcache split between gsec 6.4.10 & ours 6.6.18) |
| 4c-fixed | 6.1.20 | 6.3.10 | 6.6.18.Final | yes (+ hibernate-*) | **SUCCESS** (60s) |
| 4d (all three) | 6.2.8 | 6.5.1 | 6.6.18.Final | yes (full block) | **SUCCESS** (71s) |

Key lesson: **the bumps don't fail at the API level. They fail at the dependencyConvergence enforcer rule.** gsec (`0.0.23-RENOVATIONS-SNAPSHOT`) is built with `spring.version=6.1.20` / `hibernate.version=6.4.10.Final` baked into its own pom, and pulls those transitively into Gemma. The `<dependencyManagement>` block now pins every Spring / Spring-Security / Hibernate artifact gsec drags in so the resolved version converges.

## 4. Applied bumps

Three commits on `worktree-framework-bump-recce`:

| commit | bump | verdict |
|---|---|---|
| `50cccc5a57` | Spring Framework 6.1.20 → 6.2.8 (+ dependencyManagement infrastructure for all three frameworks) | GREEN |
| `3937fd7747` | Hibernate 6.4.10.Final → 6.6.18.Final | GREEN |
| `20741beaf3` | Spring Security 6.3.10 → 6.5.1 | YELLOW (1 new `ConfigAttribute` deprecation; otherwise clean) |

After all three: `pom.xml` shows the bumped properties at lines 1022–1024 and a 12-artifact Spring + 6-artifact Security + 2-artifact Hibernate management block.

## 5. Deferred bumps

None. All three target versions made it in.

## 6. Sequencing recommendation

Apply in commit order as already landed:

1. **Spring Framework 6.2.8 first.** Carries the dependencyManagement enlargement that all subsequent bumps need. Safe on its own.
2. **Hibernate 6.6.18 second.** Independent of Spring, drives no Spring API changes; cleanest second.
3. **Spring Security 6.5.1 third.** Hard prerequisite: Spring Framework 6.2.x. Cannot ship without commit (1).

If only one bump is to be merged in a given window, prefer Hibernate alone or Spring Framework alone. Spring Security must travel with Spring Framework.

## 7. Open questions for follow-up agents

1. **Runtime verification via `mvn verify`.** This recce is compile-only by explicit instruction (gemdtest shared with other agents). Before merge, a single-agent window should run `mvn verify` on the combined stack to catch:
   - `JdbcMutableAclService` constructor / wiring drift (the bump touches Gemma's heaviest ACL surface)
   - L2-cache init through `hibernate-jcache` 6.6 against ehcache 3 jakarta
   - Any reflective / proxy-class breakage where gsec's compiled-against-6.1 code calls bumped Spring runtime classes
2. **gsec rebuild.** gsec is still pinned to 6.1.20 / 6.3.10 / 6.4.10 internally. Rebuilding gsec against the bumped versions would tighten the dependency graph and let the `<dependencyManagement>` overrides shrink back down. Tracked separately; not a blocker for the bumps to land.
3. **`ConfigAttribute` removal.** Spring Security 7 will likely remove `ConfigAttribute` entirely (it's already deprecated in 6.5). `FilteringService.java:67` and `MethodSecurityConfig` will need migration to the `AuthorizationManager` API — orthogonal to this bump, but the clock is ticking.
4. **Jackson / Micrometer / Jersey.** Already on Phase 3 follow-ups. No interaction with these framework bumps observed in the recce.

## Provenance

- Maven Central queried 2026-05-18 via `https://search.maven.org/solrsearch/select?q=g:...`.
- Test environment: JDK 17.0.19 Amazon Corretto, Apache Maven 3.8.1, `mvn clean test-compile -T 1C` (no `mvn verify` — gemdtest shared with sibling agents per session policy).
- Worktree path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-framework-bump-recce`.
- Baseline `08e760bdaf` verified at session start via `git log -1`.
