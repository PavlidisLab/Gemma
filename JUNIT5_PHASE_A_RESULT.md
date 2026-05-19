# JUnit 5 (Jupiter) Migration — Phase A result

Phase 3 Spring 6+ infrastructure modernization. Phase A is the build-wiring
step: get the JUnit 5 (Jupiter) engine onto the test classpath alongside
the existing JUnit 4 (Vintage) engine, prove that both engines run in the
same Surefire invocation, and ship a small pilot test to lock in the
pipeline. No mass test migration (that's Phase B).

Baseline: branch `worktree-junit5-phase-a` cut fresh from
`phase2-acl-migrate` HEAD `08e760bdaf`. JDK 17 amazon-corretto.

Companion recce: `JUNIT5_MIGRATION_ROADMAP.md` on unmerged branch
`worktree-junit5-recce` (commit `4dcb5e77a1`).

---

## 1. Dependencies added

### Root `pom.xml` — property

```xml
<junit.jupiter.version>5.11.4</junit.jupiter.version>
```

Pinned to 5.11.4 to match the pre-existing `junit-vintage-engine` pin in
`gemma-rest/pom.xml` (Jersey 3 test framework already required it for
silently-zero-tests reasons documented inline).

### Root `pom.xml` — `<dependencyManagement>`

Three new managed deps, all test-scoped:

| GAV | Purpose |
|---|---|
| `org.junit.jupiter:junit-jupiter:${junit.jupiter.version}` | Aggregator that pulls `junit-jupiter-api` + `junit-jupiter-params` + `junit-jupiter-engine`. The platform discovers Jupiter tests through this. |
| `org.junit.vintage:junit-vintage-engine:${junit.jupiter.version}` | Lets the JUnit 5 Platform host the existing JUnit 4 tests. Without it, Surefire's auto-detected `JUnitPlatformProvider` runs zero tests on JUnit 4 source. |
| `org.mockito:mockito-junit-jupiter:${mockito.version}` | Ships the `MockitoExtension` that will replace `MockitoRule` in Phase B (currently 3 `MockitoRule` sites: `BaseSpringContextTest`, `FilterArgTest`, `SlackAppenderTest`). Managed only — not yet pulled into any module. |

### Root `pom.xml` — `<dependencies>`

`junit-jupiter` and `junit-vintage-engine` are pulled into every module's
test scope by declaring them in the root `<dependencies>` block (after
the existing `junit:junit` 4.13.2). Every module that inherits from the
root POM gets both engines automatically; no per-module edit needed.

### `gemma-rest/pom.xml`

The previously-hardcoded `junit-vintage-engine:5.11.4` declaration was
removed; the version is now inherited from the root. The explanatory
comment was kept (and updated) so the next reader still understands why
Vintage is required for the Jersey 3 test framework.

### Module dependency-tree check

After the change, all four modules pull both engines at version 5.11.4:

```
gemma-core: +- org.junit.jupiter:junit-jupiter:jar:5.11.4:test
            \- org.junit.vintage:junit-vintage-engine:jar:5.11.4:test
gemma-cli:  (same)
gemma-rest: (same — no longer the lone module with vintage)
gemma-web:  (same)
```

---

## 2. Surefire / Failsafe rewiring

**Intentionally no changes** in Phase A. The existing JUnit 4 category
selectors stay in place:

```xml
<!-- maven-surefire-plugin -->
<excludedGroups>ubic.gemma.core.util.test.category.IntegrationTest,${excludedGroups}</excludedGroups>

<!-- maven-failsafe-plugin -->
<groups>ubic.gemma.core.util.test.category.IntegrationTest</groups>
```

Maven Surefire 3.5.4 auto-detects the JUnit Platform provider as soon as
`junit-jupiter-engine` appears on the test classpath. The platform then
discovers both engines (Jupiter + Vintage) and runs them together. The
`groups` / `excludedGroups` selectors are JUnit-4 `@Category` class-name
selectors today; Surefire's platform provider supports JUnit 5 `@Tag`
strings in the same slots, so once Phase B starts emitting
`@Tag("integration")` we can land a dual-mode selector with no
infrastructure rewrite required.

The recce (blocker #1, option (a)) proposes a meta-annotation that
bundles `@Category(IntegrationTest.class) + @Tag("integration")` for the
duration of the migration. That meta-annotation is **not** part of
Phase A — it lands with `BaseIntegrationTest` in Phase B0.

---

## 3. Pilot test

`gemma-core/src/test/java/ubic/gemma/core/util/JUnit5PilotTest.java`

A pure Jupiter unit test, no Spring context, no DB, no network. Two
trivial assertions that prove:

- `import org.junit.jupiter.api.Test;` resolves
- `import org.junit.jupiter.api.Assertions.*` resolves
- `@Tag("junit5-smoke")` compiles
- Surefire discovers + runs the test via the Jupiter engine

```
$ JAVA_HOME=$(/usr/libexec/java_home -v 17) \
    mvn -pl gemma-core test -Dtest=JUnit5PilotTest -DfailIfNoTests=false

[INFO] --- maven-surefire-plugin:3.5.4:test (default-test) @ gemma-core ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] Running ubic.gemma.core.util.JUnit5PilotTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Class lifecycle: file should be deleted in Phase C cleanup once the
broader corpus has been migrated.

---

## 4. JUnit 4 (Vintage) backcompat

Sanity check that a pre-existing JUnit 4 test still runs under the new
wiring (same Surefire invocation, but routed through the Vintage engine):

```
$ JAVA_HOME=$(/usr/libexec/java_home -v 17) \
    mvn -pl gemma-core test -Dtest=ArrayUtilsTest -DfailIfNoTests=false

[INFO] --- maven-surefire-plugin:3.5.4:test (default-test) @ gemma-core ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] Running ubic.gemma.core.util.ArrayUtilsTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`ArrayUtilsTest` is a pure JUnit 4 unit test (`import org.junit.Test` +
`org.junit.Assert.assertEquals`). It's now being executed by the
Platform Provider via the Vintage engine, transparently. The result
match the legacy `surefire-junit47` provider output exactly (1 test, 1
pass, ~20 ms).

---

## 5. Out of scope (Phase B / B0)

Deferred per the recce; do NOT include in Phase A:

- Migrate any of the 399 `import org.junit.Test` files. The mechanical
  rewrites (`@Before` → `@BeforeEach`, `Assert.assertX(msg, exp, act)` →
  `Assertions.assertX(exp, act, msg)`, etc.) are Phase B.
- Port `NetworkAvailableRule` to `NetworkAvailableExtension`. The
  50 consuming tests can't flip to Jupiter until that lands (Phase B0).
- Migrate the four base classes (`BaseTest`, `BaseIntegrationTest`,
  `BaseSpringContextTest`, `BaseDatabaseTest`). They cascade to 172
  downstream files; touch them in Phase B0.
- Switch Surefire/Failsafe `groups` from the `@Category` class name to
  the `"integration"` `@Tag` string. That's the Phase C deletion of
  the Vintage engine.
- Delete the `Categories` suite aggregators (`AllTests`, `IntegrationTests`,
  `FastTests`, etc.) or the `takari-cpsuite` dep. Phase C cleanup.

---

## 6. Files touched in Phase A

```
pom.xml                                                          (+49)
gemma-rest/pom.xml                                               (-6)
gemma-core/src/test/java/ubic/gemma/core/util/JUnit5PilotTest.java (+57, new)
JUNIT5_PHASE_A_RESULT.md                                          (this file, new)
```

Two POM edits + one pilot test + this doc. No source code touched.

---

## 7. Validation matrix

| Check | Command | Result |
|---|---|---|
| Jupiter engine resolves | `mvn -pl gemma-core dependency:tree -Dincludes='org.junit.jupiter:junit-jupiter'` | 5.11.4 on test scope, transitively in all 4 modules |
| Vintage engine resolves | `mvn -pl gemma-core dependency:tree -Dincludes='org.junit.vintage:junit-vintage-engine'` | 5.11.4 on test scope, transitively in all 4 modules |
| dependencyConvergence enforcer rule | `mvn -pl gemma-core enforce` (implicit; runs every build) | PASS — vintage version 5.11.4 is the only declaration |
| Pilot Jupiter test runs | `mvn -pl gemma-core test -Dtest=JUnit5PilotTest` | 2/2 PASS |
| JUnit 4 vintage backcompat | `mvn -pl gemma-core test -Dtest=ArrayUtilsTest` | 1/1 PASS |
| `mvn verify` full unit-test pass | NOT run (gemdtest may be in use; per agent instructions) | deferred to next session that can grab the DB |
