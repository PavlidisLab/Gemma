# GSE141552 chopped MEX fixture

Used by `MexSingleCellDataLoaderTest#testGSE141552` to exercise the
`MexSingleCellDataLoader` end-to-end without a real 10 GB GEO download.

## Source

Full upstream bundle (8 samples, ~50–200 MB) lives on NCBI GEO at:
- `ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE141nnn/GSE141552/`
- per-sample `barcodes.tsv.gz` / `features.tsv.gz` / `matrix.mtx.gz`

The over-the-wire variant (`testGSE141552OverTheWire`) downloads from
that source and asserts against the full numbers — keep it as the
truth-source / faithfulness regression for this chopped fixture.

## Shape

Each sample directory holds:
- `barcodes.tsv.gz` — first 50 000 of the upstream barcodes
- `features.tsv.gz` — first 1 000 features UNION the three required
  gene IDs that the test asserts against
  (`ENSG00000223972.5`, `ENSG00000163930.10`, `ENSG00000210082.2`)
- `matrix.mtx.gz` — the 10× MEX matrix sliced to (kept features × 50 000
  cells), with the Market-format header rewritten so row/col/nnz counts
  match the body

Per-sample size: ~155 KB. Total: ~1.25 MB across 8 samples.

The chopped fixture keeps the discard-empty-cells path exercised: most
of the first 50 000 barcodes are empty droplets, so the loader still
filters them down to a much smaller `getNumberOfCellIds()` than the raw
barcode count.

## Regeneration recipe

1. Download the upstream samples (or reuse a cached copy under
   `~/gemma-tmp/singleCellData/GEO/GSM4206900` … `GSM4206907`). Easiest:
   run the `testGSE141552OverTheWire` variant once with
   `-DexcludedGroups=` and let `GeoSingleCellDetector.downloadSingleCellData`
   populate the cache.
2. Use the chop script (NOT checked in; one-time tool):

   ```python
   # chop_mex.py — keeps first N cells × first M features ∪ required IDs.
   # Reads gzipped MEX, slices via scipy.sparse, rewrites matrix.mtx.gz
   # with a consistent header so the loader parses cleanly.
   import gzip, sys
   from pathlib import Path
   import scipy.io

   src, dst = Path(sys.argv[1]), Path(sys.argv[2])
   n_cells, n_top = int(sys.argv[3]), int(sys.argv[4])
   extras = set(sys.argv[5:])
   dst.mkdir(parents=True, exist_ok=True)

   with gzip.open(src / "features.tsv.gz", "rt") as f:
       features = [l.rstrip("\n") for l in f]
   keep_g = sorted(set(range(min(n_top, len(features)))) |
                   {i for i, l in enumerate(features) if l.split("\t", 1)[0] in extras})

   with gzip.open(src / "barcodes.tsv.gz", "rt") as f:
       barcodes = [l.rstrip("\n") for l in f]
   nc = min(n_cells, len(barcodes))

   with gzip.open(src / "matrix.mtx.gz", "rb") as f:
       m = scipy.io.mmread(f).tocsr()[keep_g, :][:, :nc]

   with gzip.open(dst / "features.tsv.gz", "wt") as f:
       for i in keep_g: f.write(features[i] + "\n")
   with gzip.open(dst / "barcodes.tsv.gz", "wt") as f:
       for b in barcodes[:nc]: f.write(b + "\n")
   tmp = dst / "matrix.mtx"
   scipy.io.mmwrite(str(tmp), m, field="integer")
   with open(tmp, "rb") as a, gzip.open(dst / "matrix.mtx.gz", "wb") as z:
       z.write(a.read())
   tmp.unlink()
   ```

3. Run it per sample:

   ```bash
   for s in GSM4206900 GSM4206901 GSM4206902 GSM4206903 \
            GSM4206904 GSM4206905 GSM4206906 GSM4206907; do
     python3 chop_mex.py ~/gemma-tmp/singleCellData/GEO/$s \
         gemma-core/src/test/resources/data/loader/expression/singleCell/GSE141552-chopped/$s \
         50000 1000 ENSG00000223972.5 ENSG00000163930.10 ENSG00000210082.2
   done
   ```

4. Re-run `MexSingleCellDataLoaderTest#testGSE141552` and capture the
   new dimension / data-indices / data-as-ints assertion values from
   the failure messages. Freeze them in the test.

## Caveats

- The Market-format header (`rows cols nnz`) is rewritten by
  `scipy.io.mmwrite`. Hand-editing the gzipped file with `sed` will
  desynchronise the header from the body and the loader will throw.
  Always use scipy or an equivalent format-aware tool.
- The chop preserves the GEO 10x convention: rows are features, columns
  are cells. Don't transpose.
- The 3 required gene IDs are the ones the test asserts against today;
  if more assertions land on additional gene IDs, extend the `extras`
  list and re-chop.
