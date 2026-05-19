# Logging stack audit — Phase 3 Spring 6+ infrastructure modernization

Branch: `worktree-logging-modernize` (off `phase2-acl-migrate` HEAD `08e760bdaf`)
Date: 2026-05-18
Author: agent-logging-modernize

## TL;DR

- 385 Java files reference a logging API; **187 import** `org.apache.commons.logging.Log{,Factory}` directly and **188** use Lombok `@CommonsLog`. **Zero** files use SLF4J, log4j2, or `java.util.logging` directly. Gemma is a pure JCL-API shop at the source level.
- Runtime backend is **log4j-core 2.25.3** (well above the Log4Shell 2.17.2 floor — **CVE-clean**).
- All JCL traffic is routed to log4j2 via **`log4j-jcl` 2.25.3** (and `spring-jcl` 6.1.20 for Spring's own usage). `jul-to-slf4j` is NOT present; instead `log4j-jul` is wired through `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`.
- The SLF4J shim **does exist** as a runtime dependency (`log4j-slf4j-impl` 2.25.3 → `slf4j-api` 1.7.36) — any transitive dep that calls SLF4J still funnels into log4j2 — but **slf4j-api is on the EOL 1.7.x line**.
- No bumps applied in this commit. All logging artifacts (log4j-core, slf4j-api, commons-logging) are version-managed by `ubc.pavlab:pavlab-starter-parent:1.2.29`; bumping requires a parent release.

## 1. Logging API usage by family (source-level counts)

| Family | Pattern matched | File count |
|---|---|---|
| Apache Commons Logging (direct) | `import org.apache.commons.logging.Log/LogFactory` | 187 |
| Lombok `@CommonsLog` | annotation | 188 |
| SLF4J direct | `import org.slf4j.Logger/LoggerFactory/MDC` | 0 |
| Lombok `@Slf4j` | annotation | 0 |
| log4j2 direct | `import org.apache.logging.log4j.Logger` | 0 |
| Lombok `@Log4j2` | annotation | 0 |
| `java.util.logging` direct | `import java.util.logging.Logger` | 0 |
| **Total files w/ any logging import** | | **385** |

The 187 + 188 split is the field-vs-annotation pattern — older classes hand-roll `private static final Log log = LogFactory.getLog(...)`; newer ones use Lombok's `@CommonsLog`. **No source file directly imports SLF4J**; that API only enters the runtime via transitive deps (Hibernate, Spring, jersey, jackson, etc.).

## 2. POM declarations

### Root `pom.xml` (dependencyManagement)

| GroupId:artifactId | Version | Notes |
|---|---|---|
| `commons-logging:commons-logging` | (inherited, resolves 1.3.5) | JCL API + LogFactoryImpl |
| `commons-logging:commons-logging-api` | **1.1** (pinned locally) | Vestigial — strict subset of `commons-logging`, an ancient artifact. Recommend remove. |
| `org.apache.logging.log4j:log4j-core` | (inherited, resolves 2.25.3) | log4j2 core |
| `org.apache.logging.log4j:log4j-slf4j-impl` | (inherited, resolves 2.25.3) | SLF4J 1.x → log4j2 binding |
| `org.apache.logging.log4j:log4j-jcl` | `${log4j.version}` → 2.25.3 | JCL → log4j2 bridge |
| `org.apache.logging.log4j:log4j-jul` | `${log4j.version}` → 2.25.3 | j.u.l. → log4j2 bridge |

`${log4j.version}` and `${slf4j.version}` are NOT declared in Gemma's root `pom.xml` — they come from `pavlab-starter-parent` (`log4j.version=2.25.3`, `slf4j.version=1.7.36`).

### gemma-web/pom.xml

- `org.apache.logging.log4j:log4j-web` (runtime, inherited 2.25.3) — servlet context lifecycle hooks.

### gemma-cli/pom.xml

- `org.apache.logging.log4j:log4j-api` (inherited 2.25.3) — explicit API dep (transitive of log4j-core anyway).

## 3. Runtime stack

### Bridges (all → log4j2)

- **JCL** (`org.apache.commons.logging.Log`) — has THREE potential providers on the classpath:
  - `org.springframework:spring-jcl:6.1.20` (transitive of `spring-core`; provides JCL → SLF4J/log4j2 routing for Spring's own JCL usage)
  - `commons-logging:commons-logging:1.3.5` (full impl)
  - `org.apache.logging.log4j:log4j-jcl:2.25.3` (log4j2's JCL bridge — replaces commons-logging's `LogFactory` via `META-INF/services`)

  Classpath ordering decides the winner. In practice log4j2 wins because `log4j-jcl` ships a `META-INF/services/org.apache.commons.logging.LogFactory` SPI hook and Spring also defers to log4j2 when both are present. The fact that production-grade logging works (per current deployment) confirms this. **However**, the coexistence of `commons-logging:1.3.5` AND `log4j-jcl` is a classic JCL-discovery antipattern: it works by accident of classpath ordering, not by intent. See Open Questions.

- **j.u.l.** — routed via `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager` (see `pom.xml:1062`). No `jul-to-slf4j` in the tree; this is the log4j2-native route.

- **SLF4J** — `log4j-slf4j-impl:2.25.3` is a runtime dep; any transitive dep using `org.slf4j.LoggerFactory` (Hibernate, jersey, jackson, etc.) gets a log4j2-backed logger.

### Backend

**log4j-core 2.25.3** is the sole backend. No logback anywhere. No slf4j-simple.

## 4. Logging configuration files

Production configs (under `src/main/config/`):

- `gemma-web/src/main/config/log4j2.xml` — production web config. 140 lines: root INFO with file/warn/error/slack appenders; per-namespace levels for Hibernate, Spring, Jersey, Jena, DWR, basecode, gemma; dedicated AuditLogger + ProgressUpdate + JS appenders.
- `gemma-web/src/main/config/log4j2-dev.xml` — dev overlay
- `gemma-cli/src/main/config/log4j2.xml` — CLI tool config
- `gemma-cli/src/main/config/log4j2-dev.xml` — CLI dev overlay

Test configs (under `src/test/resources/`):

- `gemma-core/src/test/resources/log4j2-test.xml`
- `gemma-web/src/test/resources/log4j2-test.xml`
- `gemma-rest/src/test/resources/log4j2-test.xml`

All configs use the log4j2 XML namespace (`https://logging.apache.org/xml/ns`) with the 2.x schema.

## 5. Version drift + CVE risk

| Artifact | Current resolved | Latest (mid-2026) | EOL? | CVE risk | Status |
|---|---|---|---|---|---|
| log4j-core | 2.25.3 | 2.25.3 | no | Log4Shell (CVE-2021-44228/45046/45105) closed since 2.17.2 | **OK** |
| log4j-api | 2.25.3 | 2.25.3 | no | OK | **OK** |
| log4j-slf4j-impl | 2.25.3 | (replaced by `log4j-slf4j2-impl` for SLF4J 2.x) | uses SLF4J 1.7 API | none | **drift — see slf4j-api** |
| slf4j-api | **1.7.36** | 2.0.16 | **YES — 1.7.x is EOL** | no known CVEs | **DRIFT (cosmetic; not security-critical)** |
| commons-logging | 1.3.5 | 1.3.5 | no | none | **OK** |
| commons-logging-api | 1.1 (vestigial) | (artifact long-retired) | YES | none | **REMOVE** (not security, hygiene) |

The Log4Shell bullet from the task brief: **resolved**. log4j-core 2.25.3 is roughly 3 minor releases behind absolute bleeding edge but well past the security-critical floor. No action required.

The SLF4J 1.7 → 2.0 jump matters only if Gemma starts using SLF4J's `fluent` builder API or wants to consume libraries that ship SLF4J 2-only (rare in mid-2026). Migration is a coordinated parent bump: `slf4j-api` 2.0.x + `log4j-slf4j-impl` → `log4j-slf4j2-impl`.

## 6. Applied bumps

**None.** All logging version pins live in `pavlab-starter-parent:1.2.29`, not in Gemma's POMs. Per the task convention ("apply trivial version bumps if any deps are pinned in Gemma's POMs, not just inherited from parent"), this audit is doc-only.

Compile-verify (`mvn compile test-compile -DskipTests -q`) passes clean on baseline.

## 7. Recommendations (deferred work)

In rough priority order:

1. **Bump `pavlab-starter-parent` so it pulls SLF4J 2.0.16+ and switches `log4j-slf4j-impl` → `log4j-slf4j2-impl`.** This is a one-line change in the parent + a swap of artifactId in Gemma's logging dependencyManagement block. Cross-org coordination required.

2. **Remove the `commons-logging:commons-logging-api:1.1` pin in `pom.xml` lines 423-427.** The artifact has been superseded by the unified `commons-logging:commons-logging` (which Gemma already has at 1.3.5) for fifteen-plus years. The 1.1 jar is a strict subset and likely contributes nothing but a stale classloader entry. **Low risk; recommend doing this as a follow-up commit in this branch.**

3. **Mass-rename `@CommonsLog` → `@Slf4j` and `import org.apache.commons.logging.Log{,Factory}` → `import org.slf4j.Logger; import org.slf4j.LoggerFactory;` across all 375 sites.** This is the largest cosmetic-modernization item in Phase 3 logging. Mechanical, scope-bounded, and benefits:
   - Removes Gemma's last reliance on the JCL API surface (which Spring itself has been moving away from since 5.x).
   - Lets the `commons-logging:commons-logging:1.3.5` + `log4j-jcl:2.25.3` co-existing-providers antipattern go away (recommendation 4).
   - SLF4J 2 has placeholder-only logging (no `if (log.isDebugEnabled()) log.debug(...)` guard needed); reduces verbosity.

   **Not in this commit** — it's worth its own dedicated mechanical-sweep agent, ideally driven by an AST tool (`OpenRewrite`, `error-prone`'s patches) rather than `sed`, because Gemma has multiple flavors of declaration to catch: Lombok-injected `log`, hand-declared `private static final Log log = LogFactory.getLog(...)`, `private static final Log log = LogFactory.getLog(MyClass.class)`, and rare `protected`/`public` visibility variants.

4. **After (3), drop `commons-logging:commons-logging` and `log4j-jcl` from `dependencyManagement` + the `commons-logging-api:1.1` pin.** Spring 6 + SLF4J at the source level + the existing `spring-jcl` covers all remaining JCL traffic.

5. **Consider `log4j-jul` removal** — but only after measuring whether any third-party lib in Gemma still uses j.u.l. directly. The `log4j-jul` route via `-Djava.util.logging.manager` works fine; this is a "tidiness" item, not a problem to fix.

## 8. Open questions

- Is the choice of `log4j-jul` over `jul-to-slf4j` deliberate, or accidental? They produce the same result with log4j2 underneath; the difference matters only if SLF4J becomes the primary API (in which case `jul-to-slf4j` is the more conventional pick).
- Does any deployment target rely on the `commons-logging:1.3.5` `LogFactoryImpl` (rather than `log4j-jcl`'s `LogManagerFactory` SPI) being the JCL provider? Unlikely, but the answer determines whether deletion of `commons-logging:commons-logging` from the runtime classpath in recommendation (4) is risk-free.
- Should `log4j2.xml` configuration be consolidated? `gemma-web` and `gemma-cli` have separate prod configs; the test configs differ from each other across the three module test resources. Some duplication is intentional (different appenders for different deploy targets), but a configuration-fragment file (`log4j2.xml` with `<Include>` directives, or a shared `log4j2-common.xml`) could reduce drift. Out of scope here.
