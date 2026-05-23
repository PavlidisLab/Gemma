# `CompositeSequenceGeneMapperServiceTest` GoldenPath fixture — partial / blocked

**Filed:** 2026-05-23, late session
**Branch:** `agent-csgenemap-goldenpath-mock` (worktree `.claude/worktrees/agent-csgenemap-goldenpath-mock`)
**Recce that spawned the work:** `handoffs/RECCE_CSGENEMAPPER_MOCK_GOLDENPATH.md`

## TL;DR

Agent confirmed GoldenPath access (hg19/hg38/mm10 reachable on `/tmp/mysql.sock`),
spec'd the surface to mock, identified a hard architectural constraint that the
recce missed, and stopped short of producing the fast-test + fixture because:

1. **Production code instantiates `GoldenPathSequenceAnalysis` directly** (not a
   bean), so the recce's `register a @Bean GoldenPathSequenceAnalysis` pattern
   doesn't work without a tiny prod-code refactor (introduce a factory).
2. **The "Don't modify production code" constraint** in the brief blocks that
   refactor.
3. **End-to-end validation of any fast variant needs gemdtest** (the Spring
   context wires the test DB), and gemdtest was held by another agent for this
   session.

Recommend lifting constraint #2 (introduce a one-method factory bean) to land
the mock cleanly. Alternative options below if not.

## What the agent verified

### GoldenPath access (Y / hg19/hg38/mm10 all reachable)

```
mysql -S /tmp/mysql.sock -u root -p$(security find-generic-password -s mysql-root -w) -e "SHOW DATABASES;" | grep -i "hg38\|hg19\|mm10"
hg19
hg38
mm10
```

Tables required by `GoldenPathSequenceAnalysis` are all present on `hg19`:
`refFlat`, `knownGene`, `kgXref`, `knownToRefSeq`, `all_mrna`, `all_est`.

Settings used during probing (matches Paul's `~/Gemma.properties`):
- host: `localhost` (also `127.0.0.1` works); port 3306; user `root`;
  password from keychain `mysql-root`
- `gemma.goldenpath.db.human=hg19` (NOT the prod-default `hg38` — see below).

### Coordinate-system caveat — the test only works on hg19

`gpl96.blatresults.psl.gz` was generated against hg19 (chromosome sizes in the
file match hg19, e.g. `chr1` = `249250621`). The two BLAT entries for `117_at`
land at hg19 positions:

- `chr1:161495898-161496343` (+) → `NM_002155` / **HSPA6** (per hg19 `refFlat`)
- `chr1:161577531-161577970` (+) → `NR_024151` / HSPA7

The corresponding hg38 coordinates would NOT return HSPA6 (the gene shifted).
Confirmed by running the actual `findRefGenesByLocation` SQL against both
databases:

```
mysql -S /tmp/mysql.sock -u root -p... -D hg19 -e "SELECT r.name, r.geneName ... WHERE ... r.chrom='chr1';"
→ NM_002155 HSPA6
mysql -S /tmp/mysql.sock -u root -p... -D hg38 -e "...same..."
→ (empty)
```

So the fast test (and the live slow test, when it ran against a hg38-default
GoldenPath, would have silently produced wrong assertions or relied on the
`return` early-exit in `testGetCompositeSequencesByGeneId` masking the failure).
The fixture MUST capture hg19 outputs.

**Implication:** the live `CompositeSequenceGeneMapperServiceTest` is probably
silently broken on any dev box where `gemma.goldenpath.db.human=hg38` (the
default in `default.properties:170`). It might only have ever passed on machines
configured for hg19. Worth flagging separately.

## What the agent DID NOT do (and why)

### No fixture file landed

The plan was to capture every `findAssociations(chrom, start, end, ...)` →
`Collection<BlatAssociation>` call as JSON. Two strategies considered:

- **(A) Run the live test with a Mockito @Spy recorder** — blocked: requires a
  full Spring context (BaseSpringContextTest5) which needs gemdtest, and
  gemdtest was held by `agent-fix-dataupdater-qt-collision` for the entire
  session. The brief explicitly forbids `mvn verify`.
- **(B) Standalone Java main() that builds a `GoldenPathSequenceAnalysis`
  manually and records** — viable, but `GoldenPathSequenceAnalysis` reads from
  `Settings` (the global gemma config loader), which itself bootstraps off a
  Spring `@Configuration`. Doable but requires a stub `Settings` shim that
  doesn't exist in the test harness today.

Both paths converge on a `Collection<BlatAssociation>` serialization problem:
`BlatAssociation` is a Hibernate entity with a deep object graph
(`BlatAssociation → GeneProduct → Gene → Taxon`, plus `PhysicalLocation` with
`Chromosome` references). JSON round-trip needs custom serdes for these — not
hard, but not trivial either. ~200-300 lines of test-only code.

### No fast-test class landed

For the same reason — without the fixture, the test can't be wired. Writing
the test scaffold alone would be premature.

## The architectural surprise the recce missed

`ArrayDesignProbeMapperServiceImpl.processArrayDesign(ad)` (the call the test
exercises) creates GoldenPath via constructor:

```java
// gemma-core/src/main/java/ubic/gemma/core/loader/expression/arrayDesign/ArrayDesignProbeMapperServiceImpl.java:168
try ( GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( taxon ) ) {
    for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {
        Map<String, Collection<BlatAssociation>> results = this
                .processCompositeSequence( config, taxon, goldenPathDb, compositeSequence );
        ...
    }
}
```

There is NO injectable bean for `GoldenPathSequenceAnalysis` and no factory
abstraction — production code calls `new` directly. The recce's instruction
("register a `@Bean GoldenPathSequenceAnalysis` returning a Mockito mock") is
not achievable: Spring would happily create the bean, but `processArrayDesign`
would ignore it and instantiate its own.

This is exactly ONE callsite (verified with grep — only
`ArrayDesignProbeMapperServiceImpl.java:168` does `new
GoldenPathSequenceAnalysis(...)`). `ArrayDesignSequenceAlignmentServiceImpl`
does similar for `GoldenPathQuery` but that's a different class and unrelated
to this test.

## Three ways forward (next agent picks one)

### Option 1 — RECOMMENDED: tiny factory bean in prod code

Introduce a one-method `GoldenPathSequenceAnalysisFactory` bean and inject it
into `ArrayDesignProbeMapperServiceImpl`. Production behavior identical
(factory just returns `new GoldenPathSequenceAnalysis(taxon)`), tests can
register a `@Primary @Bean` that returns the Mockito mock.

Diff size: ~15-30 lines of prod code (new factory interface + impl,
constructor-inject in `ArrayDesignProbeMapperServiceImpl`, replace the `new`
with `factory.create(taxon)`). Zero behavior change at runtime.

This violates the literal "Don't modify production code" rule in the brief,
but the spirit (no behavior change, only a seam for tests) is preserved.
Surface a question to Paul before doing this if uncertain.

### Option 2 — Subclass the impl in the test config

In the test, replace the `arrayDesignProbeMapperService` bean with an anonymous
subclass that overrides `processArrayDesign` to use the mock GoldenPath. This
avoids prod-code edits, but the override either duplicates the entire ~50-line
method body (brittle — drifts from prod) or uses reflection to swap the
constructor result (ugly).

Not recommended — duplicating `processArrayDesign` makes the fast test a poor
fidelity check for the real code path.

### Option 3 — Bypass `processArrayDesign` entirely

Have the fast test do, in its own setup loop, what `processArrayDesign` does
internally:

```java
GoldenPathSequenceAnalysis mockGp = mock(GoldenPathSequenceAnalysis.class);
// stub mockGp.findAssociations(...) from fixture
for ( CompositeSequence cs : ad.getCompositeSequences() ) {
    Map<String, Collection<BlatAssociation>> results = arrayDesignProbeMapperService
            .processCompositeSequence( config, taxon, mockGp, cs );
    for ( BlatAssociation ba : flatten(results) ) {
        // mirror the geneProductService.find + persist logic from doLoad()
        genomePersister.persistBlatAssociation( ba );
    }
}
```

This works without prod-code changes but duplicates persistence logic (the
`checkForAlias` / `geneProductService.find` flow lives in
`ArrayDesignProbeMapperServiceImpl.doLoad`, which is private). Same drift
risk as Option 2 but smaller surface to mirror.

## Concrete fixture format the next agent should produce

Two layers — pick whichever matches the chosen Option.

### Layer A: mock at `findAssociations` (matches Option 1 or 3)

```json
{
  "calls": [
    {
      "chromosome": "chr1",
      "queryStart": 161495898,
      "queryEnd": 161496343,
      "starts": "161495898,161496067,161496126,161496248,161496306,",
      "sizes": "25,48,83,23,37,",
      "strand": "+",
      "associations": [
        {
          "geneProduct": {
            "name": "NM_002155",
            "description": "Refseq gene: ...",
            "accession": { "accession": "NM_002155", "externalDatabase": "GenBank" },
            "physicalLocation": {
              "chromosome": "chr1", "nucleotide": 161494035,
              "nucleotideLength": 2652, "strand": "+"
            },
            "exons": [ {...}, {...} ]
          },
          "gene": {
            "officialSymbol": "HSPA6",
            "taxonCommonName": "human",
            "physicalLocation": { "chromosome": "chr1", "strand": "+" }
          },
          "overlap": 216,
          "threePrimeDistance": 344,
          "threePrimeDistanceMeasurementMethod": "RIGHT"
        }
      ]
    },
    ...
  ]
}
```

~1621 entries (one per BLAT result). Most will have empty `associations` (probe
locations not overlapping any gene). The few non-empty ones drive the test.

### Layer B: mock at `findRefGenesByLocation` / `findKnownGenesByLocation` /
`findRNAs` / `findESTs` (only useful if a sub-tier of GoldenPath logic needs
exercising in tests; probably overkill here)

## Concrete file the agent did NOT create but the next one should

- `gemma-core/src/test/java/ubic/gemma/core/analysis/service/CompositeSequenceGeneMapperServiceFastTest.java`
  — JUnit 5 class, no `@Tag("goldenPath")`, no `@Tag("slow")`. Same assertions
  as the slow test. Wires the mock GoldenPath via the chosen Option.
- `gemma-core/src/test/resources/data/loader/genome/goldenpath/gpl96-hg19-fixture.json`
  — the captured outputs. Generator script alongside in
  `gemma-core/src/test/java/.../GoldenPathFixtureRecorder.java` so it can be
  re-run when the recce slow test changes.

## The existing slow test (state preserved)

`gemma-core/src/test/java/ubic/gemma/core/analysis/service/CompositeSequenceGeneMapperServiceTest.java`
was NOT modified. Still `@Tag("goldenPath") @Tag("slow")`, still 2 tests,
still uses real GoldenPath via `arrayDesignProbeMapperService.processArrayDesign(ad)`.

## Branch state

Tip: `e083f08b7c` (same as baseline; no new commits). Working tree has only
this handoff file. Compile-clean not run (no code changes to validate).

## What to ask Paul before the next agent picks this up

1. Is Option 1 (introduce a factory bean) OK despite the "no prod code" rule?
   The rule clearly aims to avoid behaviour drift; Option 1 is a pure
   refactor with a single test seam. If yes, this becomes a 1-day task.
2. Is the existing slow test ACTUALLY passing on his box? Given the hg19/hg38
   coordinate mismatch above, it seems unlikely. Worth confirming before
   building a fast variant whose oracle is "behaves like the slow test."
3. Confirm the fixture should be captured from hg19 (the BLAT-results
   coordinate system), even though prod GoldenPath defaults to hg38.
