# Java 21 Phase 1 — Pre-bump audit (Lombok / AspectJ / JaCoCo)

**Date:** 2026-05-18
**Branch:** `worktree-java21-phase1` (forked from `phase2-acl-migrate` HEAD `08e760bdaf`)
**JDK in use:** 17 (Corretto) — Phase 1 stays on 17; Phase 2 will flip to 21.
**Plan source:** `JAVA21_READINESS.md` (commit `86858b756` on unmerged `worktree-java21-readiness`).

## TL;DR

**No overrides needed.** All three deps were already at or above the
JDK-21 compatibility floor before this branch was cut. Phase 1 reduces
to a documentation update: capturing the audit and the rationale for
skipping the pom edits, so the Phase 2 JDK flip isn't blocked by stale
floor assumptions.

## Audit table

| Dep | JDK-21 floor (recce) | Recommended | Actual pin | Source | Action |
|---|---|---|---|---|---|
| `org.projectlombok:lombok` | 1.18.30 | 1.18.36+ | **1.18.42** | `pavlab-starter-parent:1.2.29` `<properties>` `lombok.version` | none — already above floor |
| `org.aspectj:aspectjweaver` | 1.9.21 | 1.9.22+ | **1.9.25.1** | Gemma root `pom.xml` direct dependency (line 274-278) | none — already above floor |
| `org.jacoco:jacoco-maven-plugin` | 0.8.11 | 0.8.12+ | **not configured** | — | none — JaCoCo isn't used by Gemma |

Notes:
- The recce assumed all three were "hidden in `pavlab-starter-parent:1.2.29`". In
  fact only Lombok lives in the parent. AspectJ is pinned directly in Gemma's root
  pom (above the parent's default Spring-AOP-pulled version, which is why
  `spring-aspects` has the `aspectjweaver` exclusion).
- JaCoCo isn't wired into Gemma at all (no plugin, no dependency, no property).
  Effective-pom (`gemma-core`) confirms zero hits. If Phase 2 wants coverage,
  introducing JaCoCo would be a NEW item, not a bump.
- The parent pom (`/Users/pzoot/maven.repository/ubc/pavlab/pavlab-starter-parent/1.2.29/pavlab-starter-parent-1.2.29.pom`)
  has only one floor-relevant property: `<lombok.version>1.18.42</lombok.version>`.

## Verification

```
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean compile -DskipTests
  -> BUILD SUCCESS (57.78s) — all 4 modules + lombok-maven-plugin:delombok clean

JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test-compile -DskipTests
  -> BUILD SUCCESS (33.31s) — test sources compile cleanly
```

No code or pom changes were required to land on JDK-21-safe versions of
the three audited deps. The compile + delombok runs at the existing
pins are the artifact-level evidence that the pre-bump step is a no-op.

## Phase 2 implication

The JDK 21 flip is not blocked by Lombok, AspectJ, or JaCoCo at their
current pins. The remaining Phase 2 work (per `JAVA21_READINESS.md`)
focuses on toolchain config, Surefire/Failsafe argLine adjustments for
JDK 21 module-access flags, and any test-time reflection sites — none
of which Phase 1 was scoped to touch.
