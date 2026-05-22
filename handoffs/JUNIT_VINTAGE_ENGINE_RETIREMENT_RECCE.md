# Retiring `junit-vintage-engine` — recce + plan

Date: 2026-05-22
Baseline SHA: f0f13bba0b
Branch: phase2-acl-migrate (worktree agent-cleanup-junit-vintage-engine)

## TL;DR

Vintage engine cannot be dropped yet. **75 test files still rely on JUnit 4
features** — overwhelmingly `@Category` (paired-tag pattern from CLAUDE.md),
plus a handful of test suites, ArchUnit, and three JerseyTest subclasses on
the J4 Jersey bases. Migration is mechanical for most cases; the long pole
is the volume of `@Category` removals (64 files) and updating the
surefire/failsafe `excludedGroups` config in parent `pom.xml` once the
category classes are no longer the source of truth.

## Inventory (residual JUnit 4 footprint)

Sweep:
```bash
grep -rln "^import org\.junit\.Test;\|^import org\.junit\.Before;\|^import org\.junit\.After;\|^import org\.junit\.experimental\.categories\.Category;\|^import org\.junit\.Rule;\|^import org\.junit\.runner\.RunWith;\|^import org\.junit\.runners\." \
  gemma-core/src/test gemma-rest/src/test gemma-cli/src/test
```
→ **82 files**.

Breakdown:

| Bucket | Count | Migration shape |
|---|---|---|
| Hybrid (Jupiter `@Test` + JUnit 4 `@Category` only) | 64 | Mechanical: delete `@Category(...)` annotations + the two J4 category imports. `@Tag` already covers them. |
| Pure JUnit 4 (no Jupiter imports yet) | 9 | Full migration: `@Test` → `org.junit.jupiter.api.Test`, `@Before` → `@BeforeEach`, `@After` → `@AfterEach`, plus `@Category` cleanup. |
| Test suites with `@RunWith(Suite.class)` | 7 | `AllTests.java`, `FastTests.java`, `FastIntegrationTests.java`, `FastUnitTests.java`, `IntegrationTests.java`, `SlowTests.java`, `UnitTests.java`. Need rewrite as JUnit Platform suites (`@Suite` from `org.junit.platform.suite.api`) or deletion if unused. |
| ArchUnit JUnit 4 runner | 1 | `AutowireImplRuleTest` — `@RunWith(ArchUnitRunner.class)`. Swap to `@AnalyzeClasses` (ArchUnit JUnit 5 integration in `archunit-junit5`). The parent pom currently pins `archunit-junit4`. |
| JUnit 4 `TestRule` implementation | 1 | `NetworkAvailableRule.java` — already superseded by `NetworkAvailableExtension`. Verify zero consumers (`grep -r "NetworkAvailableRule" gemma-*`), then delete. |
| JUnit 4 base classes still on disk | 4 | `BaseTest`, `BaseDatabaseTest`, plus the two Jersey bases (`BaseJerseyTest`, `BaseJerseyIntegrationTest`). Their `*5` forks exist; only a few tests still extend the J4 versions (see below). |
| Tests extending J4 base classes | 4 | `TasksWebServiceTest` (BaseJerseyTest), `TasksRestTest` + `AnnotationsRestTest` (BaseJerseyIntegrationTest), `BaseCliTest` (BaseTest — gemma-cli, only 1 known consumer? worth grep). |

## Maven config

Parent `pom.xml` (lines ~957-982):
```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
</dependency>
```

Surefire/failsafe `excludedGroups` (lines ~1121, ~1154, ~1678) currently
filter on BOTH the J4 category class strings AND Jupiter tag strings:
- `excludedGroups` default: `network,slow,ubic.gemma.core.util.test.category.SlowTest`
- failsafe surefire excludedGroups: `ubic.gemma.core.util.test.category.IntegrationTest,integration,${excludedGroups}`
- failsafe groups (run): `ubic.gemma.core.util.test.category.IntegrationTest | integration`

Once `@Category` is gone everywhere, drop the `ubic.gemma.core.util.test.category.*`
fragments from these strings. The `SlowTest` / `IntegrationTest` category classes
themselves (`gemma-core/src/test/java/ubic/gemma/core/util/test/category/`) can
then be deleted.

The `archunit-junit4` dependency (parent pom line ~1021) should be swapped to
`archunit-junit5` in lockstep with the `AutowireImplRuleTest` migration.

## Recommended migration order (multi-session)

This is too large for a single agent. Suggested batches:

1. **Batch 1 — @Category sweep (64 files, ~1-2 hr).** Mostly mechanical:
   delete `@Category(...)` annotations and the two J4 category imports from
   each hybrid file. **Caveat: 12 of the 64 hybrid files have more
   `@Category` markers than `@Tag` markers** — meaning some
   methods/classes have @Category without a paired @Tag. For those, the
   @Tag must be added before dropping @Category (otherwise the test
   silently changes from "excluded by default" to "runs by default"). The
   12 files:
   - `MeanVarianceServiceTest`, `EutilFetchTest`,
     `ExpressionExperimentBibRefFinderTest`, `PubMedSearchTest`,
     `PubMedXMLFetcherTest`, `ExpressionExperimentPlatformSwitchTest`,
     `SingleCell10xMexFilterTest`,
     `ArrayDesignSequenceAlignmentandMappingTest`, `GeoBrowserTest`,
     `GeoDatasetServiceTest`, `GeoBrowserServiceTest`,
     `GeoSingleCellDataDownloaderCliTest`.
   Validate with `mvn -pl gemma-core,gemma-rest,gemma-cli compile test-compile -q`.
   Commit: `test: drop @Category from hybrid Jupiter+JUnit4 tests (vintage retirement step 1)`.

2. **Batch 2 — pure J4 tests (9 files).** Per-file migration to Jupiter
   (`@Test`, `@BeforeEach`, `@AfterEach`, assertions). These are mostly
   simple Mockito unit tests by feel. Compile-clean + focused `mvn -Dtest=Foo`.
   Commit: `test: migrate residual JUnit 4 tests to Jupiter`.

3. **Batch 3 — Jersey bases (3 tests + 2 base classes).**
   - Migrate `TasksWebServiceTest` to extend `BaseJerseyTest5`.
   - Migrate `TasksRestTest` + `AnnotationsRestTest` to extend `BaseJerseyIntegrationTest5`.
   - Delete `BaseJerseyTest.java` + `BaseJerseyIntegrationTest.java`.
   Validate: `mvn -pl gemma-rest test -Dtest=TasksWebServiceTest,TasksRestTest,AnnotationsRestTest`.

4. **Batch 4 — CLI base.** Check if anything extends `BaseCliTest` (J4); if
   so, migrate to `BaseCliTest5` and delete the J4 file. If not, just
   delete it.

5. **Batch 5 — base classes + ArchUnit.**
   - Delete `BaseTest.java`, `BaseDatabaseTest.java` (verify zero subclasses
     first — the `extends BaseTest5` count of 60 is the J5 successor).
   - Migrate `AutowireImplRuleTest` to ArchUnit's JUnit 5 integration; swap
     `archunit-junit4` → `archunit-junit5` in parent pom.

6. **Batch 6 — test suites.** Decide per-suite: rewrite to JUnit Platform
   `@Suite` or delete. The Maven flow doesn't need these (failsafe/surefire
   pick up `**/*Test.java` automatically); they look like legacy IDE
   conveniences.

7. **Batch 7 — delete NetworkAvailableRule** after verifying zero consumers.

8. **Batch 8 — retire vintage engine.**
   - Remove `junit:junit` and `org.junit.vintage:junit-vintage-engine`
     dependencies from parent `pom.xml`.
   - Drop `ubic.gemma.core.util.test.category.SlowTest` /
     `IntegrationTest` substrings from the three `excludedGroups` /
     `groups` strings; delete the category classes themselves.
   - Validate with full `mvn -pl gemma-core,gemma-rest,gemma-cli test-compile`
     followed by `mvn -pl gemma-core test -DfailIfNoTests=true` (skip
     `verify` because gemdtest is single-tenant — coordinate with other
     agents).
   - Commit: `deps: drop junit-vintage-engine (all tests now Jupiter)`.

## Validation done in this recce

- `git log -1` → confirmed baseline SHA `f0f13bba0b`.
- Inventory greps run as above; counts cross-checked.
- One representative hybrid file (`DateStorageTest`) inspected end-to-end —
  confirms the mechanical migration is purely `@Category` annotation + two
  imports.

## Open questions for Paul

1. Should Batch 1 (the 64 @Category cleanups) land as one big commit or be
   chunked per package? One commit is cleaner; per-package keeps the diff
   reviewable.
2. The test suites (`AllTests.java` etc.) — does anyone use these from
   their IDE, or can they be deleted outright?
3. ArchUnit's JUnit 5 integration — the parent pom has
   `archunit-junit4`. Worth confirming the JUnit 5 variant is on Maven
   Central at a compatible version before scheduling Batch 5.
