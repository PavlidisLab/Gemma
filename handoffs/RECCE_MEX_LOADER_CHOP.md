# Recce — `MexSingleCellDataLoaderTest` fixture chop

**Filed:** 2026-05-23, post-slow-sweep.
**Status:** Queued for next perf-probe agent after `agent-fix-csgenemap-pool-leak`.
**Owner:** Single focused agent; gemma-core IT.

## Why

Agent F's slow-sweep run found `MexSingleCellDataLoaderTest` as the longest single test class at **213 s**. Per Paul's perf-probe framing (`RECCE_SLOW_SWEEP_AS_PERF_PROBE.md`): "watch for performance bottlenecks that are due to code (fix them) or to the fixture being massive (mock/chop)". This is the canonical **massive-fixture** case.

The 213 s is dominated by two test methods that download full real GEO MEX bundles from NCBI FTP at test time:

| Method | GEO | Cells | Approx download |
|---|---|---|---|
| `testGSE141552` | GSE141552 | 561 738 | ~50-200 MB |
| `testGSE125708` | GSE125708 | unknown (similar shape) | similar |

Both are tagged `@Tag("slow") @Tag("integration") @Tag("geo") @NetworkAvailable(ftp://ftp.ncbi.nlm.nih.gov/geo/series/)`. They earn every one of those tags honestly.

## Why mocking the loader is wrong

`MexSingleCellDataLoader` IS the unit under test. The test validates that the parser correctly handles the MEX wire format — gene/cell indexing, sparse-matrix triples, multi-sample bundling, barcode-to-sample offset arithmetic. Mocking the loader erases the coverage. Stub the file inputs, not the parser.

## Plan — chop + split

For each of `testGSE141552` and `testGSE125708`, split into two variants:

### Variant A — `@Tag("integration")` fast (default-run)

Loads a **chopped MEX bundle** from `src/test/resources/data/loader/expression/singleCell/<accession>-chopped/`. Targets:
- `barcodes.tsv.gz`: first 1 000 barcodes (vs 561 738)
- `features.tsv.gz`: first ~500 genes, **including** the specific gene IDs the test asserts on (e.g. `ENSG00000223972.5`, `ENSG00000163930.10`, `ENSG00000210082.2` for `testGSE141552`)
- `matrix.mtx.gz`: re-rendered from upstream, keeping only triples (gene_idx, cell_idx, count) where both indices land in the kept ranges; **rewrite the MEX header** to reflect the trimmed dimensions

Assertions in this variant are computed **from the chopped fixture** (not the upstream) — concrete `dataIndices` / `dataAsInts` derived by running the loader once locally and freezing the output, then asserting against that. Test runs in seconds, not minutes; runs in default `mvn verify` failsafe.

### Variant B — `@Tag("slow") @Tag("network") @NetworkAvailable` over-the-wire (opt-in)

The current test, unchanged. Stays the truth source for the assertion numbers that match the full upstream dataset. Runs only when explicitly requested.

## How to chop the MEX cleanly

Don't hand-edit `matrix.mtx.gz` — the Market-format header has line counts that must match the body, and the indices reference the trimmed `features.tsv` / `barcodes.tsv`. Use a Python script with `scipy.io.mmread` / `scipy.sparse`:

```python
# scripts/chop_mex.py (sketch, not for repo)
import gzip, sys
import scipy.io
import scipy.sparse

src_dir = sys.argv[1]      # e.g. .../GSE141552 cached download
dst_dir = sys.argv[2]      # e.g. src/test/resources/.../GSE141552-chopped
keep_cells = 1000
keep_genes_with_ids = {"ENSG00000223972.5", "ENSG00000163930.10", "ENSG00000210082.2"}
extra_genes = 500          # plus this many of the first

# Read features.tsv.gz to get all gene ids
with gzip.open(f"{src_dir}/features.tsv.gz", "rt") as f:
    features = [line.rstrip("\n").split("\t") for line in f]
keep_gene_idx = sorted({i for i, f in enumerate(features) if f[0] in keep_genes_with_ids}
                       | set(range(min(extra_genes, len(features)))))

# Read matrix.mtx.gz as a sparse matrix
with gzip.open(f"{src_dir}/matrix.mtx.gz", "rb") as f:
    m = scipy.io.mmread(f).tocsc()    # genes × cells in MEX
m_chopped = m[keep_gene_idx, :][:, :keep_cells]

# Write chopped triplet to mtx; gzip in place
scipy.io.mmwrite(f"{dst_dir}/matrix.mtx", m_chopped)
# gzip + drop the .mtx; trim features.tsv.gz and barcodes.tsv.gz accordingly
```

(The above is sketch-only. The real script lives outside the repo; the chopped fixture lands in `src/test/resources/` and the script is documented as the regeneration recipe in a sibling `README.md`.)

**Validation that the chop is parseable.** Round-trip: load the chopped MEX with `MexSingleCellDataLoader`, assert the loader doesn't throw + cell count matches the expected chopped value. Then freeze the loader's output for the specific genes the assertions check.

## Estimated fixture size after chop

- 1 000 cells × 500 genes × ~1% sparsity ≈ 5 000 non-zero triples
- Gzipped: ~50 KB matrix.mtx.gz, ~5 KB each tsv.gz → ~60 KB total per accession
- Two accessions → ~120 KB added to `src/test/resources/`
- vs the ~50-200 MB they currently download at test time → 1000× faster + zero network dependency.

## Cross-references

- `feedback_fast_tests_playbook.md` (user memory) — diagnostic ladder; this is step 3 (MEX).
- `handoffs/SLOW_SWEEP_FINDINGS_2026_05_23.md` — the broader 6-error queue this work belongs to.
- `handoffs/RECCE_SLOW_SWEEP_AS_PERF_PROBE.md` — Paul's perf-probe lens.
- Existing chopped-fixture precedents — search for "chopped" or "trimmed" under `src/test/resources/data/loader/expression/` for prior examples.

## Constraints for the agent

- **Don't remove `@Tag("slow")`** from the over-the-wire variant. The fast variant lands beside it with `@Tag("integration")` only.
- **Don't change `MexSingleCellDataLoader` production code** to make chopping easier. The loader works; the fixture is the problem.
- The chopped fixture is committed; the chop script is NOT (one-time regeneration, documented in fixture-dir `README.md`).
- Validate: `mvn -pl gemma-core verify -Dit.test='MexSingleCellDataLoaderTest#testGSE141552Fast'` (or whatever the new method names are) passes in default-run posture (no `-DexcludedGroups=` override); the original `testGSE141552` still passes when slow is enabled.
