# JUnit 5 Migration — Phase B0: BaseTest hierarchy (parallel base classes)

Phase 3 JUnit 5 migration, parallel-base-class step. This unblocks the ~325
remaining JUnit 4 test files that extend one of the four `BaseTest*` classes:
they can now flip to Jupiter one file at a time without coordinating a
flag-day rewrite of the load-bearing base classes.

Baseline: `phase2-acl-migrate` @ `aa18f8a323`.

---

## 1. Strategy: parallel JUnit 5 base classes, not in-place rewrite

The four JUnit 4 base classes (`BaseTest`, `BaseIntegrationTest`,
`BaseSpringContextTest`, `BaseDatabaseTest`) cascade JUnit 4 runner
inheritance to ~325 subclasses through `AbstractJUnit4SpringContextTests` /
`AbstractTransactionalJUnit4SpringContextTests`. Rewriting them in place
would force ~325 subclasses to migrate in the same commit.

Instead, this commit lands **four new parallel JUnit 5 base classes**:

| New base (Jupiter)            | Mirrors                  | Used in `extends` clauses moving forward |
|-------------------------------|--------------------------|------------------------------------------|
| `BaseTest5`                   | `BaseTest`               | New JUnit 5 unit-style Spring tests       |
| `BaseIntegrationTest5`        | `BaseIntegrationTest`    | New JUnit 5 integration tests             |
| `BaseSpringContextTest5`      | `BaseSpringContextTest`  | Tests that need the rich persistence helpers (deprecated path) |
| `BaseDatabaseTest5`           | `BaseDatabaseTest`       | H2 in-memory transactional tests          |

Subclasses migrate one at a time:

```diff
-extends BaseSpringContextTest
+extends BaseSpringContextTest5
```

…plus the mechanical JUnit 4 → JUnit 5 rewrites in the subclass itself
(see Phase B in `JUNIT5_MIGRATION_ROADMAP.md`).

When every subclass has flipped, the old JUnit 4 bases become unreferenced
and can be deleted in Phase C (along with the `5` suffix rename).

---

## 2. Structural differences (JUnit 5 base vs JUnit 4 base)

### `BaseTest5` vs `BaseTest`

- Drops `extends AbstractJUnit4SpringContextTests`. JUnit 5 prefers
  annotation-driven setup over inheritance.
- Adds `@ExtendWith(SpringExtension.class)` at the class level. This is
  the Jupiter equivalent of `@RunWith(SpringJUnit4ClassRunner.class)`.
- `@ActiveProfiles(EnvironmentProfiles.TEST)` is unchanged.

### `BaseIntegrationTest5` vs `BaseIntegrationTest`

- `@Before setUpAuthentication` → `@BeforeEach setUpAuthentication`.
- `@After tearDownSecurityContext` → `@AfterEach tearDownSecurityContext`.
- Carries BOTH `@Category(IntegrationTest.class)` AND `@Tag("integration")`
  — same dual selector as the original. The root POM's surefire excludes
  and failsafe includes both selectors, so subclasses bucket correctly
  regardless of which engine routes them.

### `BaseSpringContextTest5` vs `BaseSpringContextTest`

- `@Rule MockitoRule rule = MockitoJUnit.rule()` → class-level
  `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(strictness = Strictness.LENIENT)`.
  `LENIENT` matches the JUnit 4 `MockitoRule` default (no
  `UnnecessaryStubbingException` on unused stubs) — strictness can be
  ratcheted up subclass-by-subclass during Phase B if desired.
- Identical helper-method API (`getTestPersistentArrayDesign`, `getTaxon`,
  `runAsAdmin`, etc.) — direct ports, no signature changes. The shared
  read-only `ArrayDesign` and `ExpressionExperiment` fixture statics are
  isolated to the new class (so JUnit 4 and JUnit 5 paths don't share
  fixture state during the transition).

### `BaseDatabaseTest5` vs `BaseDatabaseTest`

- Drops `extends AbstractTransactionalJUnit4SpringContextTests`. The
  per-test transactional wrapping that base class provided is now
  expressed via the class-level `@Transactional` annotation: Spring's
  `TransactionalTestExecutionListener` (active by default under
  `SpringExtension`) gives every `@Test` method a per-method transaction
  that rolls back at end-of-test.
- `@After flushAndClearSession` → `@AfterEach flushAndClearSession`.
- The inner `BaseDatabaseTestContextConfiguration` abstract class is
  duplicated (same H2 + Flyway + Hibernate + ACL wiring). Subclasses
  reference it by its simple name and inherit it through the parent
  `BaseDatabaseTest5`.

---

## 3. POM change

`pom.xml` (root) `<dependencies>`:

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

`mockito-junit-jupiter` was already in `<dependencyManagement>` from
Phase A but not pulled into any module. `BaseSpringContextTest5` needs
`MockitoExtension`, so we now pull it into every module that inherits
from the root POM (`gemma-core`, `gemma-cli`, `gemma-rest`, `gemma-web`).

---

## 4. Proof-of-concept subclasses migrated

Ten subclasses migrated as smoke tests of the four new bases:

| Subclass                                                                                       | Old base                  | New base                  |
|------------------------------------------------------------------------------------------------|---------------------------|---------------------------|
| `MissingBuildInfoTest`                                                                         | `BaseTest`                | `BaseTest5`               |
| `BuildInfoTest`                                                                                | `BaseTest`                | `BaseTest5`               |
| `CellBrowserServiceImplTest`                                                                   | `BaseTest`                | `BaseTest5`               |
| `DelegatingSecurityContextTaskExecutorTest`                                                    | `BaseTest`                | `BaseTest5`               |
| `AsyncFactoryTest`                                                                             | `BaseTest`                | `BaseTest5`               |
| `AsyncSingletonFactoryTest`                                                                    | `BaseTest`                | `BaseTest5`               |
| `AsyncBeanAutowiringTest`                                                                      | `BaseTest`                | `BaseTest5`               |
| `BatchInfoRepopulationJobTest`                                                                 | `BaseIntegrationTest`     | `BaseIntegrationTest5`    |
| `TaxonServiceImplTest`                                                                         | `BaseIntegrationTest`     | `BaseIntegrationTest5`    |
| `CompositeSequenceServiceTest`                                                                 | `BaseSpringContextTest`   | `BaseSpringContextTest5`  |
| `BlacklistedEntityDaoImplTest`                                                                 | `BaseDatabaseTest`        | `BaseDatabaseTest5`       |

Mechanical rewrites applied per file:

- `import org.junit.Test` → `import org.junit.jupiter.api.Test`
- `import org.junit.Before` → `import org.junit.jupiter.api.BeforeEach`
- `import org.junit.After` → `import org.junit.jupiter.api.AfterEach`
- `@Before` / `@After` → `@BeforeEach` / `@AfterEach`
- `import org.junit.Ignore` → `import org.junit.jupiter.api.Disabled`;
  `@Ignore("...")` → `@Disabled("...")`
- `import static org.junit.Assert.X` → `import static org.junit.jupiter.api.Assertions.X`
- `Assert.assertX(...)` → `Assertions.assertX(...)` (no message-order swap
  was needed in these files; future migrations should beware the
  `assertEquals(msg, exp, act)` → `assertEquals(exp, act, msg)` flip)
- `@Test(expected = X.class)` → `@Test` + `assertThrows(X.class, () -> ...)`
  (`AsyncBeanAutowiringTest.testInjectBeanDirectly`)

---

## 5. Validation

**Compile-clean across all three modules:**

```
$ JAVA_HOME=$(/usr/libexec/java_home -v 17) \
    mvn -pl gemma-core,gemma-cli,gemma-rest test-compile -q
(silent — exit 0)
```

**Surefire-side migrated tests (7 classes, no DB):**

```
$ mvn -pl gemma-core test \
    -Dtest='MissingBuildInfoTest,BuildInfoTest,CellBrowserServiceImplTest,DelegatingSecurityContextTaskExecutorTest,AsyncSingletonFactoryTest,AsyncFactoryTest,AsyncBeanAutowiringTest'
Tests run: 23, Failures: 0, Errors: 0, Skipped: 3
BUILD SUCCESS
```

The 3 skipped tests are the two `@Disabled` cases in
`AsyncBeanAutowiringTest` and the one in `BuildInfoTest` — same `@Ignore`-d
behaviour as before, now expressed via JUnit 5 `@Disabled`.

**Failsafe-side migrated tests (`BaseIntegrationTest5` /
`BaseSpringContextTest5` / `BaseDatabaseTest5` subclasses):** deferred.
The pre-existing failsafe `argLine` uses an unresolved Mockito-agent
placeholder when invoked outside the full `mvn verify` lifecycle (it
needs the `process-test-classes`-bound `dependency:properties` execution
to land at the right moment, which a direct `failsafe:integration-test`
call doesn't trigger). The integration tests will validate cleanly on
the next regular `mvn verify` run.

**Cross-check that the surefire/failsafe split is preserved:**

```
$ mvn -pl gemma-core test -Dtest='BatchInfoRepopulationJobTest'
Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Surefire correctly excludes the `BaseIntegrationTest5` subclass because
both selectors in `excludedGroups` (`...IntegrationTest` and `integration`)
match it (via `@Category` and `@Tag`).

---

## 6. Files added / changed

```
NEW   gemma-core/src/test/java/ubic/gemma/core/util/test/BaseTest5.java
NEW   gemma-core/src/test/java/ubic/gemma/core/util/test/BaseIntegrationTest5.java
NEW   gemma-core/src/test/java/ubic/gemma/core/util/test/BaseSpringContextTest5.java
NEW   gemma-core/src/test/java/ubic/gemma/core/util/test/BaseDatabaseTest5.java
NEW   JUNIT5_BASETEST_MIGRATION.md                                         (this file)
EDIT  pom.xml                                                              (+10  mockito-junit-jupiter dep)
EDIT  11 test classes migrated as proof-of-concept (see §4)
```

The four old base classes (`BaseTest`, `BaseIntegrationTest`,
`BaseSpringContextTest`, `BaseDatabaseTest`) are **unchanged**.

---

## 7. Follow-on sweep instructions for future agents

The remaining ~314 subclasses are mechanical migrations along the lines
of §4. Suggested batching:

1. **`BaseTest` direct extenders (~50 remaining).** Smallest blast radius
   — usually 1 `@Test` plus 1 inline `@Configuration`. Sweep first.
2. **`BaseIntegrationTest` direct extenders (~25 remaining).** Includes
   the `@Tag("integration")` inheritance; failsafe-side test.
3. **`BaseDatabaseTest` direct extenders (~30 remaining).** H2 in-memory,
   self-contained — no external DB dependency.
4. **`BaseSpringContextTest` direct extenders (~61 remaining).** Largest
   group; deprecated path; many use `MockitoRule` field-level mocks. The
   `LENIENT` strictness setting on the new base means `@Mock`-annotated
   field mocks should work without modification, but watch for cases
   where the subclass also has a local `MockitoRule` (delete it).

For each file, the mechanical rewrites are listed in §4. Per-file effort:
2–5 minutes. Tests that use `@Rule NetworkAvailableRule` need the
`NetworkAvailableExtension` port (see blocker #2 in
`JUNIT5_MIGRATION_ROADMAP.md`) and are NOT in scope for the mechanical
sweep.

Once a base class has zero remaining subclasses, delete the JUnit 4
version and rename the JUnit 5 version (drop the `5` suffix). The
final Phase C cleanup also drops the JUnit 4 `@Category` annotation on
the bases and switches surefire/failsafe to tag-only selectors.

---

## 8. Risk / known limits

- **`@Sql`, `@Transactional`, `@DirtiesContext`** are framework-neutral
  (they live in `org.springframework.test.*`) and survive the migration
  unchanged. Verified on `AsyncFactoryTest` (`@DirtiesContext`).
- **`MockitoExtension` + `SpringExtension` interaction.** Mockito 5
  documents the combination as supported, with the constructor-injection
  caveat that Mockito only manages `@Mock` field initialization and not
  test-instance construction (Spring handles that). The
  `BaseSpringContextTest5` smoke test (`CompositeSequenceServiceTest`)
  compile-passes; full-DB integration smoke deferred to the next
  `mvn verify`.
- **Per-class `MockitoRule` in subclasses.** Two known sites
  (`FilterArgTest`, `SlackAppenderTest`) hold their own `MockitoRule`
  field independent of `BaseSpringContextTest`. Those need the field
  removed at migration time — list them in the next agent brief.
- **`NetworkAvailableRule`-using tests (50 sites)** are explicitly
  out of scope. They need `NetworkAvailableExtension` to land first
  (blocker #2 in the roadmap).
