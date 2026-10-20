# Spring Boot Dependencies BOM Adoption (Phase 3)

## Scope

Gemma now imports `org.springframework.boot:spring-boot-dependencies` as
a BOM in the root pom's `<dependencyManagement>`. This is **purely a
version-management** change. Gemma is **NOT** a Spring Boot application:

- No `@SpringBootApplication`, no embedded Tomcat, no `SpringApplication.run`.
- No `spring-boot-autoconfigure`, no `spring-boot-starter-*` (yet).
- The build still produces a Spring 6 WAR deployed under Tomcat 10 by the
  ops team. Wiring still comes from Gemma's hand-written XML and Java
  `@Configuration` classes.

What the BOM buys us:

- A single coherent set of versions for the ~250 transitive libraries
  shared between Spring Framework, Spring Security, Jersey, Jackson,
  micrometer, jakarta.*, jboss-logging, commons-*, JUnit, Mockito,
  AssertJ, Logback, Hibernate-companion artifacts, etc.
- Drop-in compatibility verification: when we bump the BOM, Spring's
  release team has already tested those versions together.
- Convergence: fewer pinning surprises when a transitive pulls a
  slightly different patch and the enforcer's `dependencyConvergence`
  rule fails.

## BOM version chosen

`spring-boot.version = 3.3.13` (the latest 3.3.x patch as of 2026-05).

Why 3.3.x and not 3.4 / 3.5:

- Boot 3.3.x targets **Spring Framework 6.1 + Spring Security 6.3 +
  Hibernate 6.5** -- aligned with Gemma's current stack
  (`spring.version=6.1.21`, `spring.security.version=6.3.10`,
  `hibernate.version=6.4.10.Final`).
- Boot 3.4 / 3.5 jump to **Spring Framework 6.2 + Spring Security 6.4/6.5**,
  which is the territory of the `worktree-framework-bump-recce` branch.
  That bump is its own coordinated change and is not on this branch.
- Picking Boot 3.3.13 keeps this commit as "version management only,
  no compile-time API surface changes." Bumping the BOM minor is then a
  follow-up step locked to the framework-bump worktree merging.

## Side effect: Spring 6.1.20 -> 6.1.21

Boot 3.3.13 pins Spring Framework to **6.1.21**. Gemma was on 6.1.20.
Holding 6.1.20 produced ~10 convergence errors where transitives (gsec,
spring-security-config) arrived at 6.1.21. Bumped Gemma's
`<spring.version>` one patch (6.1.20 -> 6.1.21) to align. Reviewed the
Spring 6.1.21 release notes -- no API breaks; pure security/bugfix
patches.

## What the BOM now manages for us

Previously hand-pinned in Gemma's `<dependencyManagement>`, **dropped**
(the BOM provides the same version):

- `org.apache.httpcomponents:httpcore` 4.4.16
- `jakarta.xml.bind:jakarta.xml.bind-api` 4.0.2
- `org.glassfish.jaxb:jaxb-runtime` 4.0.5
- `javax.cache:cache-api` 1.1.1

That's **4 entries reclaimed** from the Phase 2 Step 9 convergence pins.
The remaining 4 of the original 8 (`micrometer-commons`,
`micrometer-observation`, `jackson-core`, `antlr4-runtime`,
`jackson-module-jakarta-xmlbind-annotations`) are documented below as
either kept-overridden or moved into the explicit-pin block.

## What we still override (and why)

These artifacts stay pinned in Gemma's `<dependencyManagement>` because
either (a) the BOM does not manage them or (b) Gemma needs a different
version than the BOM picks.

### (a) Not managed by Boot 3.3 BOM (4 entries)

| Artifact | Version | Reason |
|---|---|---|
| `org.hibernate.common:hibernate-commons-annotations` | 6.0.6.Final | Hibernate 6 companion; not in spring-boot-dependencies |
| `org.apache.commons:commons-collections4` | 4.5.0 | Not in BOM (BOM only manages commons-collections, the older library) |
| `com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations` | 2.19.2 | Not in jackson-bom (separate module) |
| `org.antlr:antlr4-runtime` | 4.13.2 | Not in BOM (gemma-rest hard-pins for its grammar) |

### (b) BOM-managed, Gemma overrides (15 entries)

| Artifact | Gemma | BOM | Reason |
|---|---|---|---|
| `com.fasterxml.jackson.core:jackson-core` | 2.21.0 | 2.17.3 | Gemma tracks jackson 2.21.x for security fixes and jakarta-xmlbind module compatibility. |
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.0 | 2.17.3 | Same. |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.21 | 2.17.3 | Same. |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 2.21.0 | 2.17.3 | Same. |
| `org.hibernate.orm:hibernate-core` | 6.4.10.Final | 6.5.3.Final | Gemma holds Hibernate 6.4.x; the 6.4 -> 6.5 bump is a coordinated follow-up (separate worktree). |
| `org.hibernate.orm:hibernate-jcache` | 6.4.10.Final | 6.5.3.Final | Same. |
| `io.micrometer:micrometer-core` | 1.13.11 | 1.13.15 | Gemma's pavlab note flags issues on the 1.14 series; we hold 1.13.11 deliberately. |
| `io.micrometer:micrometer-commons` | 1.13.11 | 1.13.15 | Same. |
| `io.micrometer:micrometer-observation` | 1.13.11 | 1.13.15 | Same. |
| `io.micrometer:micrometer-registry-jmx` | 1.13.11 | 1.13.15 | Same. |
| `org.apache.commons:commons-lang3` | 3.20.0 | 3.14.0 | Gemma stays on the latest commons-lang3 line (security + JDK 21 ergonomics). |
| `commons-codec:commons-codec` | 1.20.0 | 1.16.1 | Same rationale. |
| `org.apache.commons:commons-pool2` | 2.13.1 | 2.12.1 | Same rationale. |
| `org.aspectj:aspectjweaver` | 1.9.25.1 | 1.9.24 | The .1 patch carries a Gemma-relevant fix. |
| `com.mysql:mysql-connector-j` | 8.4.0 | 8.3.0 | Gemma pins the 8.4 LTS connector. |
| `org.apache.logging.log4j:log4j-jcl` | ${log4j.version} (2.25.3 via parent) | 2.23.1 | pavlab-starter-parent pins log4j 2.25.3; we honour parent over BOM. |
| `org.apache.logging.log4j:log4j-jul` | ${log4j.version} (2.25.3 via parent) | 2.23.1 | Same. |

(Versions managed but not directly declared like `org.apache.commons:commons-text`,
`commons-io:commons-io`, `org.apache.ant:ant`, `com.h2database:h2`,
`org.projectlombok:lombok`, `org.mockito:mockito-core`, `org.assertj:assertj-core`,
`org.springframework:spring-test`, `org.ehcache:ehcache`, `com.zaxxer:HikariCP`,
etc. keep their `<version>` attribute on the `<dependency>` element in the
root pom's main `<dependencies>` block -- that wins over the BOM at the
direct-declaration level. Migrating those to BOM-managed is a tidy-up
pass that's out of scope here; they are not currently causing
convergence noise.)

## Conflicts surfaced + resolved

The first pass (BOM import only) surfaced **16 distinct convergence
errors** on `mvn validate`:

```
- 8 x Spring 6.1.20 vs 6.1.21 (Gemma direct pin vs BOM transitive)
- 2 x Hibernate 6.4.10 vs 6.5.3 (Gemma direct pin vs BOM transitive)
- 4 x Jackson 2.21.0 vs 2.17.3 (Gemma direct pin vs BOM via jackson-bom)
- 1 x Micrometer 1.13.11 vs 1.13.15
- 1 x AspectJ 1.9.25.1 vs 1.9.24
- 1 x commons-lang3 3.20.0 vs 3.14.0
- 1 x commons-codec 1.20.0 vs 1.16.1
```

Plus another **5 errors** when extended to the full reactor (gemma-cli /
gemma-core / gemma-rest / gemma-web modules):

```
- jackson-datatype-jsr310 2.21.0 vs 2.17.3
- mysql-connector-j 8.4.0 vs 8.3.0
- commons-pool2 2.13.1 vs 2.12.1
- log4j-jcl 2.25.3 vs 2.23.1
- log4j-jul 2.25.3 vs 2.23.1
```

Resolved by:

1. Bumping `spring.version` 6.1.20 -> 6.1.21 (matches BOM, patch only).
2. Adding explicit `<dependencyManagement>` entries for every artifact
   in the (b) table above. Each override pins the version Gemma wants;
   the enforcer's `dependencyConvergence` rule then sees a single
   authoritative source.

After resolution: `mvn validate` clean across the full reactor; `mvn
compile test-compile -DskipTests` SUCCESS across all 4 modules.

## Reactor compile result

```
[INFO] Gemma .............................................. SUCCESS
[INFO] Gemma Core ......................................... SUCCESS [ 59.197 s]
[INFO] Gemma CLI .......................................... SUCCESS [  7.691 s]
[INFO] Gemma REST ......................................... SUCCESS [  9.872 s]
[INFO] Gemma Web .......................................... SUCCESS [  7.451 s]
[INFO] BUILD SUCCESS
```

No source code changed. Pure pom + doc.

## Follow-ups

1. **BOM minor bump coordination.** When the framework-bump worktree
   lands (SF 6.2 + SS 6.5 + HB 6.6), bump `spring-boot.version` to the
   matching Boot 3.5 line (3.5.x at the time of writing) and re-run the
   convergence check. Several of the (b) overrides above will collapse
   when Gemma's framework versions catch up to the BOM's.
2. **Direct-declaration cleanup pass.** Many of the version attributes
   on `<dependency>` blocks inside the root pom's main `<dependencies>`
   block (commons-text, commons-io, h2, hikari, ehcache, spring-test,
   junit, mockito-core, assertj-core, lombok, jboss-logging) can be
   dropped once we are confident the BOM's pick is acceptable in each
   case. Out of scope for this commit; do it artifact-by-artifact.
3. **Drop `${jackson-annotations.version}`.** Currently 2.21 vs
   `${jackson.version}` = 2.21.0 -- slight drift; consolidate when the
   jackson-annotations module re-syncs its versioning with the rest of
   the family.
