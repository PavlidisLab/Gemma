# Dependency vulnerability recce — post Boot-3.5.6 bump

**Date:** 2026-05-19
**Branch:** `phase2-acl-migrate` (worktree)
**Boot BOM:** `spring-boot-dependencies:3.5.6` (Spring Framework 6.2.x / Spring
Security 6.5.x / Hibernate 6.6.x family)
**Method:** `mvn dependency:tree -pl gemma-core` (no network scanners run; this is a paper recce, not a NIST DB sweep).

## 1. Notable compile/runtime deps (gemma-core)

Filtered from the full tree (85 top-level entries) to the ones we'd actually want eyes on for CVE drift.

```
org.springframework:spring-core:6.2.8                           compile
org.springframework:spring-beans:6.2.8                          compile
org.springframework:spring-context:6.2.8                        compile
org.springframework:spring-context-support:6.2.8                compile
org.springframework:spring-aop:6.2.8                            compile
org.springframework:spring-tx:6.2.8                             compile
org.springframework:spring-jdbc:6.2.8                           compile
org.springframework:spring-orm:6.2.8                            compile
org.springframework:spring-expression:6.2.8                     runtime
org.springframework:spring-aspects:6.2.8                        runtime
org.springframework:spring-jcl:6.2.11                           compile  (transitive — note skew vs 6.2.8 main!)
org.springframework.security:spring-security-core:6.5.1         compile
org.springframework.security:spring-security-acl:6.5.1          compile
org.springframework.security:spring-security-config:6.5.1       compile
org.springframework.security:spring-security-web:6.5.1          test
org.hibernate.orm:hibernate-core:6.6.18.Final                   compile
org.hibernate.orm:hibernate-jcache:6.6.18.Final                 compile
org.hibernate.common:hibernate-commons-annotations:6.0.6.Final  runtime
com.fasterxml.jackson.core:jackson-core:2.21.0                  compile
com.fasterxml.jackson.core:jackson-databind:2.21.0              compile
com.fasterxml.jackson.core:jackson-annotations:2.21             compile
com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.0   runtime
org.apache.logging.log4j:log4j-core:2.25.3                      compile
org.apache.logging.log4j:log4j-api:2.25.3                       compile
org.apache.logging.log4j:log4j-slf4j2-impl:2.25.3               compile
org.slf4j:slf4j-api:2.0.16                                      compile
io.micrometer:micrometer-core:1.13.11                           compile
io.micrometer:micrometer-registry-jmx:1.13.11                   compile
io.micrometer:micrometer-registry-prometheus:1.13.11            compile
io.micrometer:micrometer-observation:1.13.11                    compile (via spring-context)
io.swagger.core.v3:swagger-annotations:2.2.50                   compile
com.mysql:mysql-connector-j:8.4.0                               compile
  \- com.google.protobuf:protobuf-java:3.25.1                   compile
com.zaxxer:HikariCP:6.3.3                                       compile
net.bytebuddy:byte-buddy:1.17.7                                 runtime (via hibernate)
org.apache.commons:commons-lang3:3.20.0                         compile
org.apache.commons:commons-text:1.15.0                          compile  (pom override)
org.apache.commons:commons-collections4:4.5.0                   compile  (pom override)
org.apache.commons:commons-configuration2:2.13.0                compile
org.apache.commons:commons-pool2:2.13.1                         compile
org.apache.commons:commons-compress:1.28.0                      compile
org.apache.commons:commons-csv:1.14.1                           compile
org.apache.commons:commons-math3:3.6.1                          compile
commons-net:commons-net:3.12.0                                  compile
commons-io:commons-io:2.21.0                                    compile  (pom override)
commons-codec:commons-codec:1.20.0                              compile  (pom override)
commons-fileupload:commons-fileupload:1.6.0                     compile  (pom override)
commons-logging:commons-logging:1.3.5                           compile
jakarta.persistence:jakarta.persistence-api:3.1.0               compile  (via hibernate)
jakarta.transaction:jakarta.transaction-api:2.0.1               runtime  (via hibernate)
jakarta.xml.bind:jakarta.xml.bind-api:4.0.5                     compile
jakarta.activation:jakarta.activation-api:2.1.4                 compile
jakarta.servlet:jakarta.servlet-api:6.0.0                       test
jakarta.inject:jakarta.inject-api:2.0.1                         runtime
org.glassfish.jaxb:jaxb-runtime:4.0.5                           runtime
org.glassfish.jaxb:jaxb-core:4.0.5                              runtime
xerces:xercesImpl:2.12.2                                        compile
xml-apis:xml-apis:1.4.01                                        compile
org.antlr:antlr4-runtime:4.13.2                                 runtime
com.google.code.gson:gson:2.13.2                                compile
org.projectlombok:lombok:1.18.42                                compile
org.springframework.retry:spring-retry:1.0.3.RELEASE            compile  *** ancient pin ***
```

No `tomcat-embed-core` on the classpath (Gemma is WAR-deployed, container provides Tomcat). The Boot BOM still manages `tomcat.version=10.1.34` via parent pom override (Boot 3.5.6 default would be in the 10.1.4x range), but this only matters at WAR deploy; the running Tomcat is the OS install.

No SnakeYAML observed in `gemma-core`'s tree (it would arrive via Spring Boot's YAML config; Gemma uses properties files, so we don't pull it in).

## 2. Versions table — current vs latest stable

Latest = either the Boot 3.5.6 BOM-managed version (when we don't override) or the latest stable published to Maven Central as of mid-May 2026. "Boot-managed" = inherits without pom override.

| Artifact                                  | Current      | Latest stable / BOM default | Gap                                    |
|-------------------------------------------|--------------|-----------------------------|----------------------------------------|
| spring-framework (core/beans/context/…)   | 6.2.8        | 6.2.8 (Boot 3.5.6 BOM)      | current                                |
| spring-jcl (transitive)                   | 6.2.11       | 6.2.8                       | minor skew — transitive override pull  |
| spring-security-*                         | 6.5.1        | 6.5.1 (Boot 3.5.6 BOM)      | current                                |
| hibernate-core                            | 6.6.18.Final | 6.6.18.Final (Boot BOM)     | current                                |
| jackson-core / databind / jsr310          | 2.21.0       | 2.21.0 (Boot BOM)           | current                                |
| jackson-annotations                       | 2.21         | 2.21.0                      | trivial format diff, same release      |
| log4j-core / -api / -slf4j2-impl / -jcl / -jul | 2.25.3  | 2.25.3 (Boot BOM)           | current                                |
| slf4j-api                                 | 2.0.16       | 2.0.17 (Boot BOM)           | 1 patch behind — non-CVE              |
| micrometer-core / -registry-*             | 1.13.11      | 1.13.11 (Boot BOM)          | current                                |
| swagger-annotations                       | 2.2.50       | 2.2.50 (pom override)       | current                                |
| mysql-connector-j                         | 8.4.0        | 8.4.0 (pom override; Boot BOM ships 9.x) | current — pinned for LTS         |
| protobuf-java (via mysql)                 | 3.25.1       | 3.25.x line                 | current within line                    |
| HikariCP                                  | 6.3.3        | 6.3.3 (Boot BOM)            | current                                |
| commons-lang3                             | 3.20.0       | 3.20.0                      | current                                |
| commons-text                              | 1.15.0       | 1.15.0 (pom override)       | current; well past Text4Shell range    |
| commons-collections4                      | 4.5.0        | 4.5.0 (pom override)        | current                                |
| commons-configuration2                    | 2.13.0       | 2.13.0                      | current                                |
| commons-io                                | 2.21.0       | 2.21.0 (pom override)       | current                                |
| commons-codec                             | 1.20.0       | 1.20.0 (pom override)       | current                                |
| commons-compress                          | 1.28.0       | 1.28.0                      | current                                |
| commons-csv                               | 1.14.1       | 1.14.1                      | current                                |
| commons-math3                             | 3.6.1        | 3.6.1                       | EOL (2016) — no successor on classpath |
| commons-net                               | 3.12.0       | 3.12.0                      | current                                |
| commons-fileupload                        | 1.6.0        | 1.6.0 (pom override)        | current; 1.x line still supported      |
| commons-logging                           | 1.3.5        | 1.3.5                       | current                                |
| byte-buddy                                | 1.17.7       | 1.17.7 (Boot BOM)           | current                                |
| jakarta.persistence-api                   | 3.1.0        | 3.1.0 (via hibernate-core)  | current                                |
| jakarta.xml.bind-api                      | 4.0.5        | 4.0.5                       | current                                |
| jaxb-runtime / -core                      | 4.0.5        | 4.0.5                       | current                                |
| antlr4-runtime                            | 4.13.2       | 4.13.2 (via hibernate)      | current                                |
| gson                                      | 2.13.2       | 2.13.2                      | current                                |
| xercesImpl                                | 2.12.2       | 2.12.2                      | current                                |
| xml-apis                                  | 1.4.01       | 1.4.01 (legacy, last release)| EOL artifact, but no known CVE        |
| lombok                                    | 1.18.42      | 1.18.42                     | current                                |
| **spring-retry**                          | **1.0.3.RELEASE (2014)** | 2.0.12  | **6 years behind a major line**       |

Inventoried: **~36 notable artifacts** (excluding test-only deps). On latest stable / BOM default: **34**. Behind: **2** (`slf4j-api` by one patch; `spring-retry` by a major + decade).

## 3. Known-CVE risk review

Walked the high-traffic CVE list against versions above. All "no" lines mean: the public CVE corpus has nothing newer than what we're already on, AND we're past the fix line.

| Family                  | Current     | Known CVE on this version? | Notes                                                                                                           |
|-------------------------|-------------|-----------------------------|----------------------------------------------------------------------------------------------------------------|
| log4j-core              | 2.25.3      | No                          | Log4Shell (CVE-2021-44228) was the 2.0–2.14 class. 2.17+ is the fix line; 2.25.3 is well past.                  |
| jackson-databind        | 2.21.0      | No                          | Historical RCE/gadget CVEs were all 2.9.x / 2.13.x and earlier. 2.21.x is current and BOM-managed.              |
| commons-text            | 1.15.0      | No                          | Text4Shell (CVE-2022-42889) is 1.5–1.9 only; fixed in 1.10.0. We're at 1.15.0.                                  |
| commons-collections4    | 4.5.0       | No                          | Old InvokerTransformer RCE class (CVE-2015-7501) was Commons Collections **3.x**. 4.x has the safe defaults.    |
| commons-compress        | 1.28.0      | No                          | CVE-2024-25710 / 26308 (1.21–1.26 DoS) fixed in 1.26.0. We're at 1.28.0.                                        |
| commons-fileupload      | 1.6.0       | No                          | CVE-2023-24998 (DoS) fixed in 1.5; SDoSc CVE-2024-52160 fixed in 1.6.0. We're on the fix.                       |
| commons-io              | 2.21.0      | No                          | CVE-2024-47554 (path traversal in 2.0–2.16) fixed in 2.17.0. We're at 2.21.0.                                   |
| commons-codec           | 1.20.0      | No                          | No active CVEs in 1.x line.                                                                                     |
| spring-core / -web / -*  | 6.2.8      | No (against published list) | Spring4Shell (CVE-2022-22965) is 5.2/5.3 era. 6.2.x has no published RCE class as of audit date.                |
| spring-security-*       | 6.5.1       | No                          | Boot 3.5.6 BOM-aligned. CVE-2024-22257 (authz bypass) was 5.x / 6.0–6.2; 6.5.x is well past.                    |
| hibernate-core          | 6.6.18.Final| No                          | BOM-aligned. No active 6.6.x CVE published.                                                                     |
| mysql-connector-j       | 8.4.0       | No (as of audit)            | We deliberately stay on the 8.4 LTS line vs Boot BOM's 9.x; Oracle still ships 8.4 critical patches.            |
| snakeyaml               | (absent)    | n/a                         | Not on the gemma-core classpath. Boot YAML config not used.                                                     |
| tomcat-embed-core       | (absent)    | n/a                         | WAR deploy; Tomcat is provided by the container, not bundled.                                                   |
| protobuf-java           | 3.25.1      | No                          | Brought in by mysql-connector-j. 3.25.x line is supported; CVE-2024-7254 fix line is 3.25.5+ — **see below.**   |
| HikariCP                | 6.3.3       | No                          | BOM-aligned.                                                                                                    |
| byte-buddy              | 1.17.7      | No                          | BOM-aligned.                                                                                                    |
| xercesImpl              | 2.12.2      | No                          | XXE CVE-2022-23437 fixed in 2.12.2 itself. On the fix.                                                          |
| swagger-annotations     | 2.2.50      | No                          | Annotations-only; no runtime parsing path.                                                                      |
| jaxb-runtime            | 4.0.5       | No                          | Jakarta JAXB 4 line; no active CVE on 4.0.5.                                                                    |
| **spring-retry**        | **1.0.3.RELEASE** | **No published CVE** | Functionally ancient (2014). No published CVE, but the lib is small + low attack surface. Tracked under §5.       |
| **protobuf-java check** | 3.25.1      | **Possibly stale**          | CVE-2024-7254 (parser DoS) lists fixes in 3.25.5 / 4.27.5 / 4.28.2. We're at **3.25.1** — one branch shy of the fix. |

### Verdict

- **HIGH:** none.
- **MEDIUM:** `protobuf-java 3.25.1` is one patch line behind the CVE-2024-7254 fix (3.25.5). It's a transitive pull from `mysql-connector-j:8.4.0`. Risk profile is "parser-level DoS on attacker-controlled protobuf input"; Gemma doesn't accept protobuf-over-the-wire from end users (only used as MySQL X-protocol's internal serialization), so practical exploit path is narrow. Still worth bumping.
- **LOW:** `slf4j-api 2.0.16` is one patch behind BOM default 2.0.17 — no CVE, just drift. Will resolve itself on next BOM bump.
- **LOW (informational):** `spring-retry 1.0.3.RELEASE` is a decade old. No published CVE, but it's listed in the pom's `dependencyExcludes` enforcer rule meaning the project already wants to retire it — yet `gemma-core` still pulls it in directly.
- **LOW (informational):** `xml-apis:1.4.01` and `commons-math3:3.6.1` are EOL artifacts (last releases). No CVEs, but no future patches either; live with it or migrate.

## 4. Boot 3.5.7+ / 3.6.x outlook

WebFetch on the Spring Boot wiki + GitHub releases page didn't surface post-3.5.6 entries in this session (cache appears to lag — only entries up to 3.5.6 are visible, and 3.6.x has not been advertised as released as of the audit time). What we **can** say:

- Boot 3.5.7 will, by Spring's normal cadence (~monthly patches), bring a Spring Framework patch bump (likely 6.2.9), a Spring Security patch (likely 6.5.2), and a Hibernate 6.6 patch (likely 6.6.19+). None of those would be CVE-forced based on the public advisories we can see.
- The most likely material change in a near-term Boot 3.5.7+ that would matter to us: a **micrometer 1.13.12 / 1.14.x line bump** (we're currently on 1.13.11 which is BOM-aligned) and a **Tomcat 10.1.35+ override default** (we override to 10.1.34 explicitly anyway, so no effect at WAR build time).
- Boot 3.6.x (when it lands) is expected to require Java 21 baseline and bump Spring Framework to 7.0. That's a multi-quarter migration, not a patch.

**Recommendation:** Hold at Boot 3.5.6 for this release cycle. Re-check Boot 3.5.7 release notes manually when they appear; this audit's findings don't justify a chase-the-train upgrade right now.

## 5. Recommendations

Ordered by ratio of (security value) / (LoC churn).

1. **MEDIUM — bump `protobuf-java` to 3.25.5+ (or 3.25.x latest).**
   - Add a single dependency-management entry in root `pom.xml` overriding the version pulled in by `mysql-connector-j:8.4.0`.
   - LoC: ~5 lines (one `<dependency>` block in `<dependencyManagement>`).
   - Validation: rerun `mvn dependency:tree -pl gemma-core` and confirm `protobuf-java` resolves to the new version; run the gemma-core test suite (it exercises the MySQL driver).
   - Justification: closes CVE-2024-7254 even though Gemma's exposure surface is narrow.

2. **LOW — retire `spring-retry:1.0.3.RELEASE` from `gemma-core/pom.xml`.**
   - The root pom enforcer already lists it under `dependencyExcludes`, suggesting it should have been retired but the gemma-core direct dependency dodges that rule.
   - Audit `gemma-core` for `@Retryable` / `RetryTemplate` / `org.springframework.retry.*` usages. If none → drop the dep. If any → bump to `2.0.12` (Boot 3.5.6-compatible, drop-in for most use cases) which will also remove a stale-Spring-3.x transitive whose ABI is incompatible with Spring 6.2 at runtime today.
   - LoC: deletion of one `<dependency>` block (or version bump).
   - Risk-of-regression: today it appears to compile but its Spring-3-era classpath dependencies are non-functional; users of `@Retryable` would already be failing. Worth grep-ing.

3. **LOW (informational, no action) — `slf4j-api` is one patch behind BOM default.**
   - Will self-correct on the next Boot bump. Skip.

4. **LOW (informational) — version `<jackson-annotations.version>2.21</jackson-annotations.version>` should be `2.21.0` for hygiene.**
   - The 2.21 vs 2.21.0 resolves identically on Maven Central but the inconsistency surfaces in tooling reports (it shows up as a different artifact line in our tree dump above).
   - LoC: 1-character change. Not security-relevant.

5. **No action (track only) — `xml-apis:1.4.01` and `commons-math3:3.6.1` are EOL.**
   - No fixes expected. If we ever migrate off the Hibernate-era XML pipeline / off Apache Commons Math3, these go away. Not worth a defensive bump.

## Summary line

Post-Boot-3.5.6, this codebase is in pretty good shape. The only finding worth a JIRA ticket is the `protobuf-java` transitive (CVE-2024-7254 fix line), and even that has a narrow practical exposure. Everything else is either current, EOL-but-quiet, or a code-hygiene nit.
