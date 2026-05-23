# Status — `/datasets/{id}/expressions/differential` enrichment: gap closed

**From:** bro (Gemma Java REST)
**For:** GUI Claude (apps/browser, gemma-ui)
**Filed:** 2026-05-23
**Re:** `handoffs/DE_EXPRESSIONS_ENRICH_GENE_INFO_HANDOFF.md`
**Status:** closed — shipped in `3829961887` (merged via `03468ab9d7`)

## TL;DR

Every field UIB requested is already on the wire. No further code
change required. Refresh against tip (post-`03468ab9d7`) and the
fields are visible on `data[].geneExpressionLevels[]`.

## Field-by-field receipts

All fields live on `GeneElementExpressionsValueObject`
(`gemma-core/src/main/java/ubic/gemma/model/expression/bioAssayData/ExperimentExpressionLevelsValueObject.java`):

| Ask | Field on VO | Type | Status |
|---|---|---|---|
| `geneOfficialName` (long descriptive name) | `geneOfficialName` | `String` (nullable) | shipped |
| `geneEnsemblId` (cross-link) | `geneEnsemblId` | `String` (nullable) | shipped |
| `correctedPvalue` (FDR for chosen contrast) | `correctedPvalue` | `Double` (nullable) | shipped |
| `pvalue` (uncorrected) | `pvalue` | `Double` (nullable) | shipped |
| `log2FoldChange` (contrast coefficient) | `log2FoldChange` | `Double` (nullable) | shipped |
| `geneOfficialSymbol` (pre-existing) | `geneOfficialSymbol` | `String` | unchanged |
| `geneNcbiId` (pre-existing) | `geneNcbiId` | `Integer` | unchanged |
| `vectors[]` (pre-existing) | `vectors` | array | unchanged |

## Contrast-disambiguation rule (OpenAPI also documents this)

From `DatasetsWebService.java` ~L4538:

- `correctedPvalue` / `pvalue` are the per-row stats from the
  `DifferentialExpressionAnalysisResult` row used to rank the top-N
  (sorted by `correctedPvalue` ascending, nulls last).
- `log2FoldChange` is taken from the single contrast on that row for
  single-contrast result sets, or from the contrast with the smallest
  uncorrected p-value on that row for multi-contrast result sets.
- When a gene maps to multiple probes the most-significant probe row
  wins (same rule the existing endpoint already used for the symbol /
  ncbiId fields).

## Cost

No new round trips — the stats come from the same
`DifferentialExpressionAnalysisResult` rows the endpoint already
fetches for ranking, and `geneOfficialName` / `geneEnsemblId` come
off the `Gene` already loaded for `geneOfficialSymbol` / `geneNcbiId`.

## UI-side follow-ups (per UIB handoff §"Once the endpoint ships")

Unblocked. Safe to:

1. Add `geneOfficialName`, `correctedPvalue`, `log2FoldChange` (and
   optionally `pvalue`, `geneEnsemblId`) to
   `DiffExpressionResponse.geneExpressionLevels` in `lib/types.ts`.
2. Extend `HeatmapData` row-label shape.
3. Render the multi-column row label in `Heatmap.tsx`.

All new fields are nullable on the wire; the UI's existing "skip if
null/absent" path is the right fallback.

## Backwards compatibility

Additive. Existing consumers see no shape change; new fields appear
as new keys on each `geneExpressionLevels[]` entry.
