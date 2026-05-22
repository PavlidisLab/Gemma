# Gene-loader test triage — findings without fix

Baseline SHA: `f816777140`
Worktree: `.claude/worktrees/agent-fix-gene-loader-tests`

## ExternalFileGeneLoaderServiceTest.testLoad — already fixed; triage entry stale

The brief lists `ExternalFileGeneLoaderServiceTest.testLoad:123 —
EntityNotFoundException: GeneProduct#2745` as a live Bucket H failure. The
worktree HEAD already contains commit `f508146ee8` (`test(externalGeneLoader):
reload gene after GP removal to avoid stale merge`), which is the exact fix
the symptom calls for. `FAILSAFE_RESIDUAL_TRIAGE.md` lines 280-284 confirm:

> `f508146ee8` `test(externalGeneLoader): reload gene after GP removal to
> avoid stale merge` — Bucket H. ... Confirmed:
> ExternalFileGeneLoaderServiceTest 4/4 (was 1E).

The fix replaces `gene.getProducts().clear(); geneProductService.remove(gp);
geneService.update(gene);` with `geneProductService.remove(gp);
gene = geneService.thaw(geneService.load(gene.getId()));` — exactly the
HB6 merge-trap remediation. No further test-side action available without
a runtime trace proving the current code still fails. Recommendation:
re-run focused IT to confirm, and if green, prune the entry from Bucket H.

## NCBIGeneLoadingTest.testGeneLoader — needs runtime trace

Symptom: `assertEquals(4, loader.getLoadedGeneCount())` returns 0.

Test is `@Tag("slow")` (excluded from default `mvn verify`); the entry
must come from a `-DexcludedGroups=` "run everything" pass. The expected
count of 4 is correct given the current fixture: only NCBI gene IDs 1, 2,
3, 7003 have RNA accessions in `gene2accession.human.sample`, and
`NcbiGeneConverter` (line 256-258) drops genes whose product set is
empty.

Two candidate root causes consistent with `getLoadedGeneCount() == 0`:

1. **Converter thread exits early.** `NcbiGeneConverter.convert(...)` runs
   in a worker thread (`gemma-core/.../NcbiGeneConverter.java:244-275`)
   that breaks out of the consume loop on any `Exception` after a
   single `log.error(e, e)`. If `convert(NcbiGeneData)` throws on the
   first record (e.g. a transient state on `Chromosome.taxon`, or a NPE
   on `info.getHistory()` for gene id 1 which has no history row), no
   genes ever reach `geneQueue` and `loadedGeneCount` stays at 0.
2. **Every `geneWriteService.upsert(gene)` throws.** `NcbiGeneLoader.doLoad`
   (line 175-209) increments `loadedGeneCount` only after a successful
   upsert and bails on the first exception with `loaderDone.set(true);
   throw new RuntimeException(e);` — count would also stay at 0. The
   GeneWriteServiceImpl was rewritten in commit `876c5208eb` (the
   strangler-fig cutover) and its caches were tuned for this exact
   test, so a fresh regression seems less likely than (1) but is
   possible if a later refactor (`aa97a0c7c4` keySet→entrySet,
   `9876c900d1` Persister shrink) perturbed it.

Both paths swallow the failure into a logger call. The Surefire/Failsafe
report cannot tell us which one fired without the test stdout.

**To progress this:**

- Run `mvn -pl gemma-core failsafe:integration-test -Dit.test=NCBIGeneLoadingTest -DexcludedGroups= -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w)` and capture stdout. The first `ERROR` line under `NcbiGeneConverter` or `NcbiGeneLoader` identifies which thread died and why.
- If converter-side: trace through `NcbiGeneConverter.convert(NcbiGeneData)` for the gene id where it dies. Likely candidates: a null `info.getHistory()` on `getCurrentId()` (NPE), or a `Chromosome` BK find that auto-flushes an inconsistent state.
- If loader-side: log the full exception from `geneWriteService.upsert(gene)`. Recent GeneWriteServiceImpl changes are the natural suspect.

No code change applied. No commit. Compile-clean.
