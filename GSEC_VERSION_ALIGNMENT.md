# gsec version alignment — feasibility recce

Branch: `worktree-gsec-version-align`
Baseline: `08e760bdaf` (`phase2-acl-migrate` HEAD at session start)
Date: 2026-05-18
Scope: doc-only recce — propose a coordinated bump of gsec's pinned Spring 6.1 / Security 6.3 / Hibernate 6.4 versions to align with the Phase 3 framework bump (Spring 6.2.8 / Security 6.5.1 / Hibernate 6.6.18) recorded in `FRAMEWORK_BUMP_FEASIBILITY.md` (commit `5812f71be5` on unmerged branch `worktree-framework-bump-recce`).

No code or POMs modified.

## 1. Current state

### 1.1 gsec

Source: `/Users/pzoot/Dev/gsec/gsec/pom.xml`, branch `renovations`, current tip `2685b1b` (`javac -parameters: fix @PreAuthorize SpEL named-parameter resolution`).

| field | value |
|---|---|
| `<groupId>` | `pavlab` |
| `<artifactId>` | `gemma-gsec` |
| `<version>` | `0.0.23-RENOVATIONS-SNAPSHOT` |
| parent | `ubc.pavlab:pavlab-starter-parent:1.2.28` |
| `<spring.version>` | `6.1.20` |
| `<spring.security.version>` | `6.3.10` |
| `<hibernate.version>` | `6.4.10.Final` |
| `<maven.compiler.release>` | `17` |
| `<maven.compiler.parameters>` | `true` |

Notes from the pom comments:
- Renovations Phase 2 already moved gsec to Spring 6 / Security 6 / Hibernate 6 / jakarta.
- `hibernate-commons-annotations` pinned to `6.0.6.Final` in `dependencyManagement` to override a stale 4.0.5 pin in the parent pom — without that pin, the Hibernate 6 JPA reflection path fails (missing `ReflectionManager.reset()`).
- `dependencyConvergence` enforcer rule is **disabled** ("disabled while we churn through Spring 6 + Hibernate 6").

### 1.2 Gemma's import

Source: `/Users/pzoot/Dev/eclipseworkspace/Gemma/pom.xml` line 224 and line 1021.

```xml
<gsec.version>0.0.23-RENOVATIONS-SNAPSHOT</gsec.version>
...
<dependency>
    <groupId>pavlab</groupId>
    <artifactId>gemma-gsec</artifactId>
    <version>${gsec.version}</version>
</dependency>
```

Gemma's properties (lines 1022-1024):

| property | value |
|---|---|
| `<spring.version>` | `6.1.20` |
| `<spring.security.version>` | `6.3.10` |
| `<hibernate.version>` | `6.4.10.Final` |

Versions match gsec exactly on the `phase2-acl-migrate` HEAD. **There is no version drift today.** The drift only opens up once `worktree-framework-bump-recce` lands.

### 1.3 The drift the framework bump introduces

Per `FRAMEWORK_BUMP_FEASIBILITY.md`, the bump on `worktree-framework-bump-recce` moves Gemma to:

| property | Gemma (post-bump) | gsec (still) | drift |
|---|---|---|---|
| spring | 6.2.8 | 6.1.20 | yes |
| spring-security | 6.5.1 | 6.3.10 | yes |
| hibernate | 6.6.18.Final | 6.4.10.Final | yes |

Spring 6.1 / 6.2 is binary-compatible (Spring team policy), so gsec's compiled bytecode runs on the bumped runtime. The break is **dependencyConvergence at the Maven enforcer level** — not Java semantics. The fix lives in a new `<dependencyManagement>` block in Gemma's root pom.

## 2. The 20-artifact dependencyManagement block

Recorded verbatim from `git show 50cccc5a57 -- pom.xml`. Lines 213-318 of the post-bump pom.xml. Used today purely to override gsec's transitive 6.1.20 / 6.3.10 / 6.4.10 graph.

### 2.1 Spring Framework pins — 12 artifacts

`spring-core`, `spring-beans`, `spring-context`, `spring-context-support`, `spring-expression`, `spring-aop`, `spring-aspects`, `spring-tx`, `spring-jdbc`, `spring-orm`, `spring-web`, `spring-webmvc`.

All set to `${spring.version}` = `6.2.8`.

### 2.2 Spring Security pins — 6 artifacts

`spring-security-core`, `spring-security-acl`, `spring-security-config`, `spring-security-web`, `spring-security-crypto`, `spring-security-taglibs`.

All set to `${spring.security.version}` = `6.5.1`.

### 2.3 Hibernate ORM pins — 2 artifacts

`hibernate-core`, `hibernate-jcache`. Both set to `${hibernate.version}` = `6.6.18.Final`.

### 2.4 Reclaim if gsec bumps

Direct count: **20 entries fully reclaimable** if gsec is rebuilt against the same target versions Gemma is moving to. The whole block was added solely to absorb gsec's pinned transitives — without that transitive force-pull, every override becomes dead weight.

Caveat: 1-2 of the spring-security entries might still be useful to lock down what other dependencies pull in (e.g. some test-only library), but the *motivating reason* for all 20 entries is gsec. So count the reclaim as 20-ish, possibly leaving a 1-2-entry residual.

## 3. Source-level gsec changes needed for the bump

### 3.1 Compile-clean estimate against Spring 6.2.8 / Security 6.5.1 / Hibernate 6.6.18

**Spring Framework 6.1 -> 6.2.** Zero gsec source changes expected. Gemma's recce found zero hits on the 6.2 deprecation list (`WebMvcConfigurer`, `AbstractHttpMessageConverter` overrides, deprecated `ResponseStatusException` constructors) across the much larger Gemma codebase; gsec is a tighter library and uses none of those classes.

**Spring Security 6.3 -> 6.5.** One new deprecation surfaced in Gemma's recce: `org.springframework.security.access.ConfigAttribute`. gsec imports that class from **3 files** (visible in this recce's grep):
- `AclEntryVoter` (or sibling voter classes via `gemma/gsec/acl/voter/`)
- `AclEntryAfterInvocationProvider` (and the 11 sibling providers under `gemma/gsec/acl/afterinvocation/`)
- One administrative wiring class

These compile cleanly at 6.5 — `ConfigAttribute` is **deprecated, not removed**. Same status the parallel `AccessDecisionVoter` / `AfterInvocationProvider` / `AccessDecisionManager` APIs already hold and that Gemma's `MethodSecurityConfig` already documents as "deprecated in Spring 6 but still functional". No migration needed at the 6.5 line.

**Hibernate 6.4 -> 6.6.** Zero gsec source changes expected. gsec uses `org.hibernate.orm` packages already; the API surface used (`Session`, `SessionFactory`, `Query`, listener interfaces) is stable between 6.4 and 6.6.

**Net source-change estimate: 0 lines required, 0 lines optional.** gsec compiles clean at the bumped versions. (Subject to actual `mvn compile` verification — out of scope here.)

### 3.2 Spring Security 7 horizon (not 6.5)

Spring Security 7 will remove `ConfigAttribute`, `AccessDecisionManager`, `AccessDecisionVoter`, and `AfterInvocationProvider` outright in favor of the `AuthorizationManager` API. That migration touches:

- 4 voter classes under `gemma/gsec/acl/voter/`
- 12 after-invocation provider classes under `gemma/gsec/acl/afterinvocation/`
- Gemma's `FilteringService.java:67` and `MethodSecurityConfig`

This is a real piece of work — easily 1-2 sessions — but **it is orthogonal to the 6.2 / 6.5 / 6.6 bump**. Don't conflate the two.

### 3.3 Build infra

gsec already has:
- `maven.compiler.release` = 17 (matches Gemma)
- `maven.compiler.parameters` = true (matches Gemma)
- `dependencyConvergence` rule already commented out — no enforcer surprise to absorb
- `hibernate-commons-annotations` `6.0.6.Final` override — would carry forward to 6.6 too (or could be removed if the parent pom is fixed)

No build-infra obstacles.

## 4. Recommended path

### Plan A (recommended): gsec-side bump

1. **gsec change**: bump pom properties to `spring.version=6.2.8`, `spring.security.version=6.5.1`, `hibernate.version=6.6.18.Final`. Cut a new SNAPSHOT, e.g. `0.0.24-RENOVATIONS-SNAPSHOT`.
2. **gsec verify**: `mvn clean install` on gsec — expect compile clean per section 3.1. Run gsec's own test suite (43+ AfterInvocation unit tests, AclEventListener tests, others added in recent commits).
3. **Gemma change**: bump `<gsec.version>` to `0.0.24-RENOVATIONS-SNAPSHOT`. Bump `<spring.version>` / `<spring.security.version>` / `<hibernate.version>` to the same target. **Drop the 20-artifact `<dependencyManagement>` block** (or leave a 1-2-artifact residual after careful check).
4. **Gemma verify**: `mvn clean verify` against gemdtest.

Cost: 1 session of focused work, mostly verification. The actual edits are 3 property bumps in each pom.

Benefit:
- Reclaims ~20 dependencyManagement entries (block reduces back to its pre-bump state plus the few legitimate Phase-2 jakarta/jackson pins).
- Sets the precedent: every future Spring minor bump in Gemma travels with a matching gsec bump. No drift accumulates.
- Lets gsec's `dependencyConvergence` enforcer rule be re-enabled (it's currently commented out — see pom comment "disabled while we churn through Spring 6 + Hibernate 6"). Once gsec is on the same versions as Gemma, convergence becomes meaningful again.

### Plan B (not recommended): defer gsec bump, keep workaround

Leave gsec at 6.1.20 / 6.3.10 / 6.4.10. Keep the 20-artifact block in Gemma forever.

Pros: zero gsec churn this session.

Cons:
- Every future Spring/Security/Hibernate bump in Gemma will need to extend the block (and re-verify it covers the new transitive surface).
- The block is dead-code-shaped: nothing in Gemma actually wants those versions; they exist only to mask gsec's pins.
- gsec's `dependencyConvergence` stays disabled — meaning future gsec changes can land their own transitive drift unnoticed.
- The longer gsec lags, the harder the eventual catch-up.

**Recommend Plan A.**

## 5. Effort estimate

| step | scope | estimate |
|---|---|---|
| gsec pom edit | 3 property changes | 5 min |
| gsec compile + tests | `mvn clean install` | 5 min |
| gsec snapshot publish | `mvn deploy` to pavlab maven2 (or local install for testing) | 5 min |
| Gemma pom edit | `<gsec.version>` bump + drop ~20 entries + framework property bumps | 15 min |
| Gemma `mvn verify` | full suite against gemdtest | 30-60 min |
| follow-up if anything regresses at runtime (proxy/load-time issues) | reactive | 0-30 min |

**Total: 1-2 hours of focused work in a single session.**

Note: a `mvn verify` on the framework-bump worktree is already a prerequisite per `FRAMEWORK_BUMP_FEASIBILITY.md` section 7 item 1. The gsec bump is best done **in the same window** as that verify — both share the same dependency graph and the same gemdtest fixture.

## 6. Open questions for Paul

1. **gsec release cadence.** Are SNAPSHOT versions OK to consume from Gemma indefinitely, or should a `0.0.24-RENOVATIONS` (non-SNAPSHOT) tag be cut once the renovations branch stabilizes? Cutting a real release would let Gemma pin against an immutable version.
2. **gsec test coverage.** gsec has gained substantial test surface recently (43 AfterInvocation tests, AclEventListener tests, InMemoryAclService helpers). Is the suite trusted enough that a compile-clean + green test run on the bumped gsec is sufficient sign-off, or does it warrant a manual smoke on a Gemma stack before publishing the snapshot?
3. **gsec deprecation roadmap.** The Spring Security 7 migration (section 3.2) hits ~16 gsec source files. Is that work planned for the same renovations branch, or is gsec staying on 6.x for the foreseeable future and Gemma will eventually fork/replace those voters and providers? The answer affects whether to invest in keeping gsec aligned past 6.5.
4. **pavlab-starter-parent.** gsec inherits from `ubc.pavlab:pavlab-starter-parent:1.2.28`. Does the parent need its own bump (Spring/Hibernate base versions, plugins) to support a clean gsec 6.2/6.5/6.6 build, or is gsec's per-property override sufficient indefinitely?
5. **dependencyConvergence re-enable.** gsec's `dependencyConvergence` enforcer rule is currently commented out. Once on the bumped versions, should it be re-enabled in gsec to catch future transitive drift early?

## 7. Files referenced

- `/Users/pzoot/Dev/gsec/gsec/pom.xml` (gsec's only pom, branch `renovations`)
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/pom.xml` (Gemma root pom, gsec import + framework properties)
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/FRAMEWORK_BUMP_FEASIBILITY.md` (only present on branch `worktree-framework-bump-recce`, commit `5812f71be5`)
- Gemma commits referenced (all on `worktree-framework-bump-recce`, not yet on `phase2-acl-migrate`):
  - `50cccc5a57` — Spring Framework 6.1.20 -> 6.2.8 (introduced the 20-artifact dependencyManagement block)
  - `3937fd7747` — Hibernate 6.4.10.Final -> 6.6.18.Final
  - `20741beaf3` — Spring Security 6.3.10 -> 6.5.1

## Provenance

- Recce performed at Gemma baseline `08e760bdaf`, gsec tip `2685b1b` (branch `renovations`).
- No `mvn` invocations made; static read of pom.xml + source greps only.
- Worktree: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-version-align`.
