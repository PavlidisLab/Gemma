# JUnit 5 (Jupiter) Migration Roadmap

Phase 3 Spring 6+ infrastructure modernization. **Recce only — no
test files were modified in this commit.** The maven failsafe/surefire
wiring around `@Category(IntegrationTest.class)` is load-bearing and
requires a design decision before any mechanical migration begins.

Baseline: `phase2-acl-migrate` @ `08e760bdaf`. JDK 17 amazon-corretto,
Spring 6.1.20, Hibernate 6.4.10, Mockito 5.21.0.

---

## 1. Versions in use

| Component | Version | Notes |
|---|---|---|
| `junit:junit` | 4.x (transitive) | Declared in root POM `<dependencies>` (line 580) **with no `<version>`** — version is pulled transitively. Inherited by every module. |
| `org.junit.vintage:junit-vintage-engine` | 5.11.4 | Already declared in `gemma-rest/pom.xml` (line 147) because Jersey 3's test framework brings the JUnit 5 platform; Surefire would otherwise silently run zero tests. This is the **only** Jupiter-adjacent artifact already in the build. |
| `org.junit.jupiter:junit-jupiter` (api/engine) | **not present** | No `import org.junit.jupiter.*` exists anywhere in the source tree. Tests are 100% JUnit 4. |
| `org.springframework:spring-test` | 6.1.20 | Spring 6 supports both `SpringJUnit4ClassRunner` (legacy) and `SpringExtension` (Jupiter). Currently consumed via legacy runner inheritance. |
| `org.mockito:mockito-core` | 5.21.0 | Used via classic `MockitoRule`/`MockitoJUnit.rule()`. **Not yet using `mockito-junit-jupiter` (`MockitoExtension`).** |
| `org.assertj:assertj-core` | 3.27.7 | Framework-neutral — survives migration unchanged. |
| `io.takari.junit:takari-cpsuite` | 1.2.7 | Powers `@RunWith(ClasspathSuite.class)` in `AllTests.java` — used by all `Categories.class` suite aggregators. **No JUnit 5 equivalent that's a drop-in.** See blockers. |
| Surefire / Failsafe | 3.5.4 | Surefire and Failsafe are split by JUnit 4 `@Category` (see blocker #1). |

---

## 2. Inventory (counts by JUnit 4 feature)

Counts are file-level (`grep -rln`) unless noted; computed against the
worktree at `08e760bdaf`. Files under `/target/` and `/.claude/` are
excluded.

| Feature | Files | Notes |
|---|---:|---|
| `import org.junit.Test` | **399** | Every test class. |
| `import org.junit.{Before,After,BeforeClass,AfterClass}` | 170 | Lifecycle hooks. |
| `import org.junit.Ignore` | 29 | → `@Disabled`. |
| `import org.junit.{Rule,ClassRule}` | 53 | → `@ExtendWith` + per-rule rewrite. See blocker #2. |
| `import org.junit.runner.RunWith` | 9 | All sites listed in §5. |
| `import org.junit.experimental.categories.Category` | 91 | But only ~141 *uses* of `@Category(...)` (see §5). Bulk inherit category via `BaseIntegrationTest`. |
| `import [static] org.junit.Assert` | 223 | Mechanical rewrite to `org.junit.jupiter.api.Assertions` (argument-order swap on `assertEquals(msg, expected, actual)`). |
| `@Test(expected = Foo.class)` | 33 | → `assertThrows(Foo.class, () -> ...)`. |
| `@Test(timeout = ...)` | 0 | Nothing to migrate. |
| `TemporaryFolder` `@Rule` | 0 | No `@TempDir` migration needed. |
| `@Category(IntegrationTest.class)` (direct annotation) | 5 | Direct uses. The other ~140 integration tests inherit `@Category` through `BaseIntegrationTest`. **Total integration tests via inheritance: ~172** (count of files extending `BaseIntegrationTest` / `BaseSpringContextTest` / `BaseTest` / `BaseDatabaseTest`). |
| `@Category(SlowTest.class)` | 127 | Powers the suite aggregators. |
| `@Category(GeoTest.class)` | 6 + 9 mixed | |
| `extends AbstractJUnit4SpringContextTests` | 13 (BaseTest +12 others extend it directly) | Source of the implicit Spring JUnit 4 runner. |
| Test files total (`src/test/java/**/*.java`) | 484 | |

### @RunWith targets (9 sites)

| Runner | Count | Files |
|---|---:|---|
| `Categories.class` | 6 | The six suite aggregators in `gemma-core/src/test/java/ubic/gemma/core/util/test/suite/` |
| `ClasspathSuite.class` | 1 | `AllTests.java` (`takari-cpsuite`) |
| `SpringJUnit4ClassRunner.class` | 1 | `gemma-rest/.../BaseJerseyTest.java` |
| `Parameterized.class` | 1 | `gemma-core/.../GeoConverterTest2.java` |

### @Rule targets (53 sites, two distinct rule classes)

| Rule class | Count | Migration |
|---|---:|---|
| `NetworkAvailableRule` (project-custom) | 50 | Must port to a JUnit 5 `Extension` — see blocker #2. |
| `MockitoRule rule = MockitoJUnit.rule()` | 3 (`BaseSpringContextTest`, `FilterArgTest`, `SlackAppenderTest`) | Replace with `@ExtendWith(MockitoExtension.class)` after adding `mockito-junit-jupiter`. |

---

## 3. Hotspot files (top 10 by JUnit 4 surface)

Ranked by count of `@Test` / `@Before` / `@After` / `@Rule` / `@Ignore`
/ `@Category` / `@RunWith` / `Assert.*` occurrences per file.

| Rank | File | Surface |
|---:|---|---:|
| 1 | `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/GeoSingleCellDetectorTest.java` | 66 |
| 2 | `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentDaoTest.java` | 61 |
| 3 | `gemma-rest/src/test/java/ubic/gemma/rest/DatasetsWebServiceTest.java` | 40 |
| 4 | `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/GeoConverterTest.java` | 38 |
| 5 | `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceIntegrationTest.java` | 36 |
| 6 | `gemma-core/src/test/java/ubic/gemma/core/analysis/preprocess/batcheffects/RNASeqBatchInfoPopulationTest.java` | 33 |
| 7 | `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/service/GeoBrowserTest.java` | 32 |
| 8 | `gemma-core/src/test/java/ubic/gemma/persistence/util/FilterTest.java` | 26 |
| 9 | `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/singleCell/GeoMexSingleCellDataLoaderConfigurerTest.java` | 25 |
| 10 | `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/service/GeoDatasetServiceTest.java` | 24 |

The much larger migration-cost lever is **the four base classes**
(`BaseTest`, `BaseIntegrationTest`, `BaseSpringContextTest`,
`BaseDatabaseTest`) — they propagate JUnit 4 runner inheritance and
`@Before` / `@After` lifecycle into ~172 downstream files. Migrate the
base classes first; most descendants will then need only an import
swap.

---

## 4. Translation table

| JUnit 4 | JUnit 5 (Jupiter) | Notes |
|---|---|---|
| `import org.junit.Test` | `import org.junit.jupiter.api.Test` | Package-only change. |
| `@Test(expected = Foo.class)` | `assertThrows(Foo.class, () -> { ... })` | Per-method body rewrite. 33 sites. |
| `@Test(timeout = N)` | `@Timeout(value = N, unit = MILLISECONDS)` or `assertTimeout(...)` | 0 sites — skip. |
| `@Before` | `@BeforeEach` | |
| `@After` | `@AfterEach` | |
| `@BeforeClass` | `@BeforeAll` (must be `static` *or* test class is `@TestInstance(PER_CLASS)`) | |
| `@AfterClass` | `@AfterAll` | |
| `@Ignore("reason")` | `@Disabled("reason")` | 29 sites. |
| `@RunWith(SpringJUnit4ClassRunner.class)` | `@ExtendWith(SpringExtension.class)` or shorthand `@SpringJUnitConfig` | 1 explicit site (`BaseJerseyTest`); the implicit ones via `AbstractJUnit4SpringContextTests` are handled by switching base classes to `AbstractJUnit5SpringContextTests` (does **not** exist — see blocker #4). |
| `@RunWith(MockitoJUnitRunner.class)` / `MockitoRule` | `@ExtendWith(MockitoExtension.class)` | Requires `mockito-junit-jupiter`. 3 rule sites. |
| `@RunWith(Parameterized.class)` + `@Parameters` | `@ParameterizedTest` + `@MethodSource` (per method, not per class) | 1 site (`GeoConverterTest2`). Structural rewrite: tests become per-method parameterized rather than class-level. |
| `@RunWith(Categories.class)` + `@IncludeCategory` + `@SuiteClasses` | Surefire/Failsafe `groups`/`excludedGroups` selectors over `@Tag` | See blocker #1. The six suite-aggregator classes become obsolete; selection moves to the build. |
| `@RunWith(ClasspathSuite.class)` (takari-cpsuite) | No drop-in. Surefire `<includes>**/*Test.java</includes>` already covers discovery; `AllTests.java` becomes obsolete. | |
| `@Category(IntegrationTest.class)` | `@Tag("integration")` | See blocker #1. |
| `@Rule public X r = new X()` | `@ExtendWith(XExtension.class)` (class-level) + Extension impl | Project-custom rules must be ported. |
| `@Rule MockitoRule` | `@ExtendWith(MockitoExtension.class)` | |
| `org.junit.Assert.assertX(msg, expected, actual)` | `org.junit.jupiter.api.Assertions.assertX(expected, actual, msg)` | **Argument order swap** for the message parameter. Mechanical but easy to miss. |
| `org.junit.Assume.*` | `org.junit.jupiter.api.Assumptions.*` (or `Assumptions.assumingThat(...)`) | Used inside `NetworkAvailableRule` — migrate together. |
| `org.junit.rules.TestRule` / `Statement` / `Description` API | `BeforeEachCallback`, `AfterEachCallback`, `ExecutionCondition` (returns `ConditionEvaluationResult.disabled(...)`), `InvocationInterceptor` | The `assumeNoException`-based skip pattern in `NetworkAvailableRule` maps cleanly to `ExecutionCondition` returning `disabled()`. |

---

## 5. Blockers

### Blocker #1 — Surefire/Failsafe category split is the central design decision

The root `pom.xml` splits unit vs integration tests via the JUnit 4
category system:

```xml
<!-- maven-surefire-plugin -->
<excludedGroups>ubic.gemma.core.util.test.category.IntegrationTest,${excludedGroups}</excludedGroups>
<!-- maven-failsafe-plugin -->
<groups>ubic.gemma.core.util.test.category.IntegrationTest</groups>
```

Surefire 3.x supports JUnit 5 `@Tag`s in the same `groups` /
`excludedGroups` slot — but **only when the included tests are
JUnit 5**. With the vintage engine running JUnit 4 tests, the
`groups` filter selects on JUnit 5 tags only; JUnit 4 `@Category`
selection is configured through a different `<dependencies>`
mechanism on the plugin (`surefire-junit47` provider). Mixing the
two during a phased migration is the single biggest risk.

**Options:**

- **(a) Tag-and-Category dual annotation during Phase A.** Add a
  meta-annotation `@IntegrationTest` that bundles
  `@Category(IntegrationTest.class)` + `@Tag("integration")` and
  retrofit it onto `BaseIntegrationTest`. Run both engines (vintage
  + jupiter). Configure Surefire to exclude both selectors. Pay a
  small build-time cost for the duplicate annotation until Phase C.
- **(b) Migrate base classes first, then downstream.** Once
  `BaseIntegrationTest` is Jupiter, downstream tests that extend it
  are also Jupiter, so `@Tag` selection suffices. The 5 direct
  `@Category(IntegrationTest.class)` annotations get a manual swap.

Option (a) is safer (mechanical, file-by-file rollback possible).
Option (b) is faster but requires migrating all 172 base-class
descendants in lockstep with the base class itself.

### Blocker #2 — `NetworkAvailableRule` (50 sites) is project-custom

`gemma-core/src/test/java/ubic/gemma/core/util/test/NetworkAvailableRule.java`
implements `org.junit.rules.TestRule` and is paired with a
`@NetworkAvailable(url=..., timeoutMillis=...)` annotation that 50
test classes use. It reaches into `Description.getTestClass()` and
`description.getAnnotations()` to discover URLs.

Migration: rewrite as a JUnit 5 `ExecutionCondition` (skip-when)
plus optionally an `InvocationInterceptor` (skip-on-exception). The
`ExecutionCondition.evaluateExecutionCondition()` API exposes
`ExtensionContext.getElement()` (a `java.lang.reflect.AnnotatedElement`)
which is the JUnit 5 equivalent of `Description.getAnnotations()`.
The class-level `getTestClass()` lookup maps to
`ExtensionContext.getTestClass()`.

This is a **one-time port** but it's a real piece of engineering,
not a mechanical rewrite, and it must land before any of the 50
consuming tests can be flipped to Jupiter.

### Blocker #3 — `MockitoRule` lives in `BaseSpringContextTest`

`BaseSpringContextTest` (the base class for ~150 integration tests)
holds `@Rule public MockitoRule rule = MockitoJUnit.rule();`. When
`BaseSpringContextTest` flips to Jupiter, replace with
`@ExtendWith(MockitoExtension.class)` on the class — but
`MockitoExtension` doesn't play nicely with Spring's `@ExtendWith(SpringExtension.class)`
when both want to manage the test instance lifecycle. The accepted
pattern is to keep `@MockitoSettings(strictness = Strictness.LENIENT)`
or to use `@ExtendWith({SpringExtension.class, MockitoExtension.class})`
with Mockito 5 (which is more cooperative). Verify against Mockito
5.21.0 release notes; the rule-to-extension swap in a base class
with hundreds of descendants needs a green smoke test on the first
few descendants before bulk migration.

Requires adding `org.mockito:mockito-junit-jupiter:5.21.0` to root
`<dependencyManagement>`.

### Blocker #4 — `AbstractJUnit4SpringContextTests` has no Jupiter equivalent

`BaseTest extends AbstractJUnit4SpringContextTests` (from
`org.springframework.test.context.junit4`). Spring 6 still ships
this class for backward compatibility — but the JUnit 5 idiom is
**not** to extend a base class. Instead, you annotate the test
class with `@ExtendWith(SpringExtension.class)` (or the meta
`@SpringJUnitConfig`).

Migration shape: `BaseTest` loses `extends
AbstractJUnit4SpringContextTests` and gains
`@ExtendWith(SpringExtension.class)`. Any references in
`BaseSpringContextTest` / subclasses to inherited methods or fields
from `AbstractJUnit4SpringContextTests` (e.g., `applicationContext`,
`logger`) need to be re-provided. There are 13 direct extenders of
`AbstractJUnit4SpringContextTests`; the rest reach it via base
classes. The blast radius is moderate but each extender needs eyes
on it.

### Blocker #5 — `takari-cpsuite` + `Categories` suite aggregators

The six aggregators in
`gemma-core/src/test/java/ubic/gemma/core/util/test/suite/`
(`AllTests`, `IntegrationTests`, `FastTests`, `FastUnitTests`,
`FastIntegrationTests`, `UnitTests`, `SlowTests`) layer
`@RunWith(Categories.class)` on top of `@RunWith(ClasspathSuite.class)`.
None of this has a clean JUnit 5 analogue, but **none of it is
needed** — Surefire's `<includes>`/`<excludes>` plus `@Tag`
selectors replicate the same selection at build time. These seven
files can simply be deleted at the end of Phase C. Until then they
hold pinned JUnit 4 dependencies (`takari-cpsuite`,
`org.junit.experimental.categories.Categories`,
`org.junit.runners.Suite`) and prevent dropping the vintage engine.

---

## 6. Recommended migration phases

### Phase A — dependency bump (low risk, no test files touched)

Land before any test code changes.

- Add `org.junit:junit-bom:5.11.4` to root `<dependencyManagement>`.
- Add `org.junit.jupiter:junit-jupiter` (api + engine) as a managed
  test dependency. **Keep `junit:junit` 4.x** so existing tests
  compile and run unchanged.
- Add `org.junit.vintage:junit-vintage-engine` to **every** module
  (currently only `gemma-rest` has it). The vintage engine is what
  lets the Jupiter platform pick up JUnit 4 tests during the
  multi-phase migration.
- Add `org.mockito:mockito-junit-jupiter:${mockito.version}` to the
  managed deps.
- Verify Surefire/Failsafe pick up both engines. The current
  `groups` / `excludedGroups` filter still targets the JUnit 4
  `@Category` class name — leave it alone for now. JUnit 4 tests
  continue to run via vintage; the (currently zero) JUnit 5 tests
  will run via jupiter.
- Validation: `mvn -pl gemma-core test` runs the same test count
  before and after.

**Effort: 0.5 day. Risk: low** — purely additive.

### Phase B0 — pre-migration: port `NetworkAvailableRule` and the base classes

Single coordinated migration of:

1. `NetworkAvailableRule` → `NetworkAvailableExtension`
   (`ExecutionCondition` + `InvocationInterceptor`). Keep the old
   rule class around as deprecated for the duration of Phase B.
2. `BaseTest`, `BaseIntegrationTest`, `BaseSpringContextTest`,
   `BaseDatabaseTest` → Jupiter (`@ExtendWith(SpringExtension.class)`
   on `BaseTest`; `@BeforeEach` / `@AfterEach` swap;
   `MockitoExtension` instead of `MockitoRule`; `@Tag("integration")`
   replacing `@Category(IntegrationTest.class)` — but **keep the
   `@Category` for now** so the existing Surefire/Failsafe split
   still works while subclasses are still JUnit 4).
3. Pick the dual-annotation Surefire/Failsafe wiring (option (a)
   from blocker #1).

Validation: `mvn verify` runs the same set of tests in the same
surefire vs failsafe split, with the same pass count.

**Effort: 2–3 days. Risk: medium** — the base classes are
load-bearing; one botched lifecycle method breaks 172 downstream
tests.

### Phase B — mechanical migration, file-by-file

Once the base classes and `NetworkAvailableExtension` are landed,
the remaining 484 test files are mechanical rewrites:

- `org.junit.Test` → `org.junit.jupiter.api.Test`
- `@Before`/`@After` → `@BeforeEach`/`@AfterEach`
- `Assert.assertX(msg, exp, act)` → `Assertions.assertX(exp, act, msg)`
  (argument-order swap)
- `@Ignore` → `@Disabled`
- `@Test(expected = X.class)` → `assertThrows(X.class, () -> ...)`
- `@Rule NetworkAvailableRule` → `@ExtendWith(NetworkAvailableExtension.class)`
- Remove now-redundant `MockitoRule` instances
- Convert `@Category(IntegrationTest.class)` (direct, 5 sites) to
  `@Tag("integration")`
- Manual: `GeoConverterTest2` (Parameterized) gets a
  `@ParameterizedTest` rewrite — single file.
- Manual: `BaseJerseyTest` drops `@RunWith(SpringJUnit4ClassRunner.class)`
  in favour of `@ExtendWith(SpringExtension.class)`.

Migrate in module slices: `gemma-rest` (~50 files) → `gemma-cli`
(~40) → `gemma-core` (~400). Within `gemma-core`, slice by package
to keep PRs reviewable.

**Effort: 5–8 days of mechanical work + spot-checks. Risk: low per
file, but compounds.** Could be parallelized across worktrees once
the base classes are settled.

### Phase C — drop the vintage engine

- Delete the six `Categories` suite aggregators + `AllTests.java` +
  the `takari-cpsuite` dep.
- Remove `org.junit.vintage:junit-vintage-engine` from all modules.
- Remove `junit:junit` (4.x) from the root `<dependencies>` block.
- Switch Surefire/Failsafe `groups` selector from
  `ubic.gemma.core.util.test.category.IntegrationTest` to
  `integration` (the JUnit 5 tag).
- Delete `BaseSpringContextTest`'s deprecation candidates if any are
  still in use, or leave for a future cleanup. (Already marked
  `@Deprecated`.)
- Delete the JUnit 4 `IntegrationTest`, `SlowTest`, etc. marker
  classes if no longer referenced — confirm with `grep`.
- Wire a build-fail check: if any `import org.junit.Test` (no
  `jupiter`) survives, fail the build. Either a Maven Enforcer
  custom rule, an `error-prone` check, or a simple `grep`-in-CI
  step.

**Effort: 0.5–1 day. Risk: low** if Phase B was thorough.

---

## 7. Risk / effort estimate

| Phase | Effort | Risk | Reversible? |
|---|---|---|---|
| A — dep bump | 0.5 day | low | yes (revert single POM) |
| B0 — port base classes + `NetworkAvailableRule` | 2–3 days | **medium** — base classes touch 172 files | yes (revert) but pricey if 172 downstream tests rotted in the interim |
| B — mechanical migration of 484 test files | 5–8 days | low-per-file, **compounds** | yes (per file) |
| C — drop vintage, fail build on JUnit 4 | 0.5–1 day | low | yes |

**Total estimate: ~10–13 working days of focused work**, plus
review/CI iteration. The first ~3 days (Phase A + B0) unlock the
remaining work as parallelizable, so the wall-clock cost depends on
how aggressively Phase B can be sliced across worktrees / agents.

The longest-tail risk is **Mockito + Spring extension interaction**
in `BaseSpringContextTest` — verify against the project's actual
mock-heavy test fixtures (e.g., the top-10 hotspot files) before
bulk migration of descendants.

---

## Appendix — sanity-check commands

```bash
# Inventory
grep -rln "import org.junit.Test;" --include='*.java' . | grep -v /target/ | grep -v /.claude/ | wc -l
grep -rln "import org.junit.jupiter" --include='*.java' . | grep -v /target/ | grep -v /.claude/ | wc -l  # should grow each Phase B PR
grep -rn  "@RunWith" --include='*.java' . | grep -v /.claude/

# Phase C exit check
grep -rln "import org.junit\.\(Test\|Before\|After\|Rule\|Ignore\|runner\|experimental\)" --include='*.java' . | grep -v /target/ | grep -v /.claude/
# Expected: empty when Phase C is done.
```
