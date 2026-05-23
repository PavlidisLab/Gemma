# GSE125708 chopped MEX fixture

Used by `MexSingleCellDataLoaderTest#testGSE125708` to exercise the
`MexSingleCellDataLoader` path that handles cell types embedded in
`barcodes.tsv.gz` (a GSE125708 quirk: each barcode line has a
`<barcode>\t<cell-type>` shape).

## Source

Full upstream bundle on NCBI GEO:
- `ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE125nnn/GSE125708/`

The test only loads sample GSM3580724; the loader is configured with
`ignoreSamplesLackingData(true)` so other samples in the series can be
absent from the fixture.

The over-the-wire variant (`testGSE125708OverTheWire`) downloads from
that source and asserts against the full numbers — keep it as the
truth-source / faithfulness regression for this chopped fixture.

## Shape

`GSM3580724/`:
- `barcodes.tsv.gz` — first 2 000 barcodes (each `<barcode>\t<cell-type>`)
- `features.tsv.gz` — first 500 features
- `matrix.mtx.gz` — 500 × 2 000 sparse slice with consistent header

Size: ~110 KB.

## Regeneration recipe

Same `chop_mex.py` script as the GSE141552 fixture. Run with:

```bash
python3 chop_mex.py ~/gemma-tmp/singleCellData/GEO/GSM3580724 \
    gemma-core/src/test/resources/data/loader/expression/singleCell/GSE125708-chopped/GSM3580724 \
    2000 500
```

No `extras` needed — the test does not assert on specific gene IDs.

The test only checks `cellIds.hasSize(N)` + `contains("AAACCTGAGGTGACCA-1")`.
After regenerating, re-run the test, capture the new dimension size,
and confirm the named barcode is still in the first slice (it sits at
line 1 of the upstream `barcodes.tsv.gz`).
