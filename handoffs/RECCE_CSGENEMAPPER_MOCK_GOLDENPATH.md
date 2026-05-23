# Recce — `CompositeSequenceGeneMapperServiceTest`: mock GoldenPath, don't hunt a pool leak

**Filed:** 2026-05-23, mid-session. **Supersedes** the earlier "fix the JDBC connection leak" framing in `handoffs/SLOW_SWEEP_FINDINGS_2026_05_23.md` action item #1.
**Owner:** Whatever agent runs after `agent-fix-csgenemap-pool-leak` lands (which is on the OLD framing — review its output critically; revert if it changed production pool code on a false-positive premise).

## Paul's call, 2026-05-23

> "GoldenPath is the problem with that CompositeSeqGeneMapper test failure. it needs the database. it's not a big deal to install them, but this should be *mock* again for tests. We have hg38, hg19 and mm10 loaded up on this *local* mysql (/tmp/mysql.sock) for what it is worth, so you can get the real output to put in the test fixture for mocking"

## What's actually happening

The test is tagged `@Tag("goldenPath") @Tag("slow")` (lines 66-67 of `gemma-core/src/test/java/ubic/gemma/core/analysis/service/CompositeSequenceGeneMapperServiceTest.java`). The `goldenPath` tag is the smoking gun — this test has always needed the **UCSC GoldenPath MySQL databases** (hg38 / hg19 / mm10 / etc.) configured at `gemma.goldenpath.db.host` / `gemma.goldenpath.db.port` (defaults in `gemma-core/src/main/resources/default.properties:161-165`).

When GoldenPath isn't configured / not reachable, the `ArrayDesignProbeMapperServiceImpl.processArrayDesign(ad)` call inside `blatCollapsedSequences:194` tries to acquire a connection from the GoldenPath Hikari pool, blocks until the pool's `connectionTimeout` fires, then surfaces as `CannotGetJdbcConnection: Failed to obtain JDBC Connection`. **Not a leak in production code.** The pool is exhausted because every attempted query is waiting forever on an unreachable host.

So the `agent-fix-csgenemap-pool-leak` brief was framed wrong. If that agent shipped a "pool leak fix" in production code, that fix should be reverted — there's no leak.

## Right approach: mock the GoldenPath surface in the test

GoldenPath access in the test goes through:

- `ArrayDesignProbeMapperServiceImpl.processArrayDesign(ArrayDesign)` (the test calls at line 197)
- Internally that uses `GoldenPathSequenceAnalysis` (`gemma-core/src/main/java/ubic/gemma/core/goldenpath/GoldenPathSequenceAnalysis.java`), which is a thin wrapper over JDBC queries against UCSC tables (`knownGene`, `refGene`, `chromInfo`, etc.).
- `ProbeMapperImpl` (`.../analysis/sequence/ProbeMapperImpl.java`) accepts `GoldenPathSequenceAnalysis` as a parameter — it's already abstracted via the type. That IS the mock surface.

### Generating the fixture from Paul's local GoldenPath

Paul has GoldenPath loaded on his local MySQL via `/tmp/mysql.sock`, with `hg38`, `hg19`, `mm10`. Use that to capture the queries + their results once, freeze them, then mock `GoldenPathSequenceAnalysis` (or whatever interface the production code consumes) to return the frozen output.

Steps:

1. **Identify the exact methods called.** Open `ProbeMapperImpl.processSequence(GoldenPathSequenceAnalysis, BioSequence)` and the methods downstream of it; list the `GoldenPathSequenceAnalysis` calls the test exercises (e.g. `findRefGenesByLocation(chrom, start, end, strand)`, etc.). Likely 3-6 distinct methods.

2. **Capture real outputs.** Run the test once against Paul's local GoldenPath (point `gemma.goldenpath.db.host=/tmp/mysql.sock` + appropriate user/password). Add a temporary trace-logging interceptor or `@Spy` that records every `(method, args) → result` pair to a JSON file. Test passes; fixture lands at `gemma-core/src/test/resources/data/loader/genome/goldenpath/gpl96-hg38-fixture.json` (or similar).

3. **Wire the mock.** In the test's `@Configuration`, register a `@Bean GoldenPathSequenceAnalysis` that's a Mockito mock loaded from the JSON fixture — for each captured `(method, args)`, `when(mock.method(args)).thenReturn(result)`. Or a hand-rolled stub class that switches on args. Either pattern's fine; Mockito is the smaller diff.

4. **Drop `@Tag("goldenPath")`** from the test once it no longer needs the real GoldenPath. Keep `@Tag("slow")` if the BLAT + NCBI gene-loader portions remain heavy; the agent doing this should measure post-mock wall clock and decide.

5. **Keep the over-the-wire path as a sibling.** A new `*OverTheWireTest` (or `*GoldenPathLiveTest`) class with `@Tag("goldenPath")@Tag("slow")` that bypasses the mock and queries the real GoldenPath — used to refresh the fixture when UCSC ships new assemblies. Paul: "I doubt we'll run it" — that's fine, same pattern as the MEX-loader recce.

## What NOT to do

- **Don't change Hikari pool sizing** for GoldenPath. The pool isn't broken; it's correctly waiting for an unreachable host.
- **Don't add a `@Disabled` to skip the test on dev boxes.** Mock + run by default is the win.
- **Don't fabricate the fixture by hand.** Use Paul's local GoldenPath to capture the real queries — that's the whole point of him mentioning hg38/hg19/mm10 are loaded there.
- **Don't bundle the mock work with anything else.** One commit: fixture file + mock wiring + test passes with `goldenPath` tag removed.

## Validation

- `mvn -pl gemma-core verify -Dit.test='CompositeSequenceGeneMapperServiceTest' -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) -Dgemma.hibernate.hbm2ddl.auto=create` passes WITHOUT `gemma.goldenpath.*` configured.
- Default-run `mvn -pl gemma-core verify` still at `surefire 1526/0F+0E+18S, failsafe 376/0F+0E+9S` (the test was excluded by `slow` anyway, so the default-run baseline shouldn't move unless the new test is no longer `slow`).

## Same pattern for BLAT (any test that invokes the subprocess)

Paul, follow-up 2026-05-23: "Same for BLAT if that is involved: again, we don't want to run BLAT for fast tests, configuring it is a pain."

**Status in *this* test:** `CompositeSequenceGeneMapperServiceTest` uses `ShellDelegatingBlat` but does NOT actually invoke the BLAT subprocess — it reads pre-computed `.psl` results from `data/loader/genome/gpl96.blatresults.psl.gz` (line 190) and just parses them. So BLAT itself isn't part of *this* test's slowness.

**Pattern for tests that DO invoke BLAT:** mirror the GoldenPath mock strategy. The BLAT subprocess surface is `ShellDelegatingBlat.blatQuery(sequences, taxon)` returning `Map<BioSequence, List<BlatResult>>`. Mock that map from a frozen `.psl` capture. The agent inventory at `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md` already flags `ShellDelegatingBlatTest` as `@Tag("slow")` + `@Disabled` ("way too slow"); when picking up that work, apply the same mock-the-external-resource pattern.

## Priority

Microarray plumbing. The active product frontier is single-cell / RNA-seq (see `RECCE_MEX_LOADER_CHOP.md` for the MEX loader work). These GoldenPath + BLAT mocks are worth doing, but **the MEX-loader chop ships first** — same perf-probe slot, higher product weight. Pick this work up when the single-cell queue is clear or when a microarray-side regression actually bites.

## Cross-references

- `gemma-core/src/main/java/ubic/gemma/core/goldenpath/GoldenPath.java` — the abstract base.
- `gemma-core/src/main/java/ubic/gemma/core/goldenpath/GoldenPathSequenceAnalysis.java` — the surface the test uses transitively.
- `gemma-core/src/main/java/ubic/gemma/core/goldenpath/GoldenPathQuery.java` — query helper.
- `gemma-core/src/test/java/ubic/gemma/core/util/test/category/GoldenPathTest.java` — the JUnit 4 category equivalent (vintage; the live tag is `@Tag("goldenPath")`).
- `default.properties:161-165` — GoldenPath datasource config keys.
- `handoffs/RECCE_MEX_LOADER_CHOP.md` — sibling perf-probe recce; same "mock the external resource, keep one default-run test, keep one opt-in truth-source" pattern.

## Connection to the currently-running agent

`agent-fix-csgenemap-pool-leak` (branch `agent-fix-csgenemap-pool-leak`, started ~12:30 PDT) is running on the OLD "find the pool leak in production code" brief. When it lands, review the diff:

- **If it changed production code** (e.g. pool sizing, connection cleanup): revert. There's no leak.
- **If it filed a recce concluding "GoldenPath unreachable"**: great, this doc supersedes that recce — proceed with the mock.
- **If it did the mock work itself**: even better, just review the fixture-generation method (must be from real GoldenPath, not fabricated).
