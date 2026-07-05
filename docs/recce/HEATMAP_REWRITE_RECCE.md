# Heatmap data generation — client-side rewrite recce

Branch: `heatmap-rewrite-recce` baselined at `c45c236fd2`. This is a design
recce, not a refactor. No Java code is edited here.

## 1. Executive summary

Today, Gemma's "expression heatmap" data pipeline does three jobs on the
server in one shot: (1) load `DoubleVectorValueObject`s, (2) reorder each
vector's `double[]` in-place to follow an experimental-design-driven
sample layout, and (3) flatten the layout into a presentation-grade
structure (per-bar factor swatches, hex colours, "No value" placeholders,
legend maps). The result is the
`VisualizationValueObject[]` graph returned by `DEDVController.getDEDV*`
endpoints (DWR, legacy gemma-web) and rendered by `Gemma.Heatmap` in
JavaScript. The server is making rendering decisions (palette choice,
"No value" fill colour, factor-name uniquification, sample sort order).

Paul's directive: ship a raw matrix + meta to the client and let the
client sort, palette, sort-by-factor, group-by-level interactively. The
existing gemma-curation-ui `HeatmapWidget` already wants exactly this:
`{ values: number[][], rowLabels, colLabels, colAnnotations: [{ name,
values, palette }] }` (`apps/browser/src/lib/heatmap/types.ts` —
`HeatmapData`). The new endpoint just has to materialise that shape from
the existing vector + design loaders, with NO server-side reordering and
NO server-side colour assignment.

Phased plan: (S1) this recce; (S2) add a NEW JAX-RS endpoint
`/datasets/{id}/heatmap-data` (and probably
`/datasets/{id}/heatmap-data/diffex`, `/heatmap-data/coexpression`)
shipping `{ matrix, rows[], columns[], factors[] }`, leaving DEDV
controller intact; (S3) curation-ui consumes new endpoint, replacing the
synthetic `HeatmapDemo` data; (S4) once gemma-curation-ui is fronting all
heatmap consumers, retire `DEDVController.getDEDVFor*` + the legacy
`MetaheatmapVisualizationPanel` JS and delete
`ExperimentalDesignVisualizationServiceImpl.sortVectorDataByDesign`,
`DEDVController.prepareFactorsForFrontEndDisplay`, and the
`VisualizationValueObject` / `FactorProfile` / `GeneExpressionProfile`
chain. The single biggest reduction is the deletion of
`ExperimentalDesignVisualizationServiceImpl` (538 LOC, almost all of it
is the layout cache + in-place data reorder dance) and
`DEDVController.prepareFactorsForFrontEndDisplay` plus
`createFactorNameToColoursMap` (~250 LOC of colour-queue plumbing).

## 2. Current state — server-side organization

### 2.1 Endpoint(s)

Expression-heatmap data is served by DWR endpoints on `DEDVController`
(`gemma-web/src/main/java/ubic/gemma/web/controller/expression/experiment/DEDVController.java`,
1269 LOC, `@RequestMapping("/dedv")`). Six methods all return
`VisualizationValueObject[]`:

- `getDEDVForVisualization(eeIds, geneIds)` — line 483. Per-EE +
  per-gene fetch, then `sortVectorDataByDesign(...)`, then assemble.
- `getDEDVForVisualizationByProbe(eeIds, probeIds)` — line 552.
- `getDEDVForDiffExVisualization(eeIds, geneIds, threshold, factorMap)`
  — line 271. Adds `getProbeDiffExValidation(...)` (looks up
  `DifferentialExpressionValueObject`s above a threshold) and feeds them
  to `makeDiffVisCollection` which sorts EEs by min p-value.
- `getDEDVForDiffExVisualizationByExperiment(...)` — line 334.
- `getDEDVForDiffExVisualizationByThreshold(resultSetId, threshold,
  primaryFactorID)` — line 420. Only callsite that honours the
  `primaryFactor` argument to `sortVectorDataByDesign`.
- `getDEDVForPcaVisualization(eeId, component, count)` — line 457. Pulls
  top loaded vectors for an SVD component.
- `getDEDVForCoexpressionVisualization(eeIds, queryGeneId,
  coexpressedGeneId)` — line 217.

Also static-image (PNG) endpoints, separate but related:
- `GET /expressionExperiment/visualizeHeatmap.html` — line 574 in
  `ExpressionExperimentQCController`. Server-side rasteriser using JFreeChart
  via `ExpressionDataHeatmap.createImage(...)`.
- `GET /expressionExperiment/visualizeSubSetHeatmap.html` — line 611.

These PNG endpoints (used by `ExpressionDataHeatmapTag` JSP tag) are a
SEPARATE problem from the DEDV "interactive client heatmap" rewrite —
the PNG path is the lo-fi static path used by `expressionExperiment.detail.jsp`,
and `ExpressionDataHeatmap` is a clean class (257 LOC) that doesn't go
through the "horrible" factor-organization code.

### 2.2 Service-layer assembler(s)

The "horrible" code lives in two layers:

**Layer 1 — vector reorder + layout cache.**
`gemma-core/src/main/java/ubic/gemma/core/visualization/ExperimentalDesignVisualizationServiceImpl.java`
(538 LOC). The public entrypoint is
`sortVectorDataByDesign(Collection<DoubleVectorValueObject> dedVs,
@Nullable ExperimentalFactor primaryFactor)` (line 80). It returns
`Map<Long /*eeId*/, LinkedHashMap<BioAssayValueObject,
LinkedHashMap<ExperimentalFactor, Double>>>`. The return type itself
is the smell: three levels of nesting, one of which (`ExperimentalFactor
-> Double`) abuses a Double as either a factor-value ID (categorical) or
a measurement (continuous) — see `getExperimentalDesignLayout` line 347
("we use IDs to stratify the groups").

The method also MUTATES every `DoubleVectorValueObject` passed in:
copies the `data` array (line 175), iterates the old BioAssay ordering,
re-indexes into the new ordering (lines 183–198), and sets
`vec.setReorganized(true)`. It also mutates the
`BioAssayDimensionValueObject.reorder(...)` after the loop. The vector
list is a side-effect output.

**Layer 2 — flatten layout to colour swatches + legends.**
`DEDVController.prepareFactorsForFrontEndDisplay(VisualizationValueObject vvo,
LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor,
Double>> eeLayouts)` (line 1131). This walks every BioAssay, walks every
ExperimentalFactor, looks up factor-value-id-as-double in a `Map<Long,
FactorValue> fvs` cache to recover the FV, picks a hex colour out of a
queue keyed on the factor, falls back to a random hex if the queue is
empty, populates two parallel structures (`factorToValueNames` for the
legend, `factorValueMaps` for the per-sample column-header swatch
strip), and finally calls `vvo.setFactorNames(...)` and
`vvo.setFactorValuesToNames(...)`. The factor-value-id round-trip
through Double is here too (`Long id = Math.round(valueOrId)` line
1264).

Adjacent helpers all in `DEDVController`:
- `createFactorNameToColoursMap(...)` — line 115. Defines a hardcoded
  palette of blues/purples/red-yellows/greens (categorical) and
  blues/greens (continuous), assigns one queue per factor.
- `getRandomColour(Random)` — line 838.
- `getUniqueFactorName(ExperimentalFactor)` — line 900. Suffixes the
  factor name with `" [ID=" + factor.getId() + "]"` because the legend
  keys are bare strings.
- `getFacValsStr(...)` / `composeFacvalStr(...)` — line 1243+. Reach
  through `FactorValueUtils.getSummaryString(facVal)` to produce a
  display string from a factor-value ID.

Layer 3, downstream, the `VisualizationValueObject` itself
(`gemma-web/src/main/java/ubic/gemma/web/controller/visualization/VisualizationValueObject.java`,
312 LOC) carries:
- `Collection<GeneExpressionProfile> profiles` — wrapped vectors with
  colour + valid-flag.
- `LinkedHashMap<String, LinkedHashMap<String, String>> factorNames` —
  legend map: `factorDisplayName -> (factorValueDisplay -> hexColour)`.
- `List<LinkedHashMap<String, String[]>> factorValueMaps` — per-sample
  swatch list: one map per sample, each entry `factorDisplayName ->
  [factorValueDisplay, hexColour]`.
- `Collection<FactorProfile> factorProfiles` — derived plot-points
  produced by `setUpFactorProfiles(layout)` (line 200), each one a
  `List<List<DoublePoint>>` of run-length-coded factor levels along the
  sample axis. Used by the legacy `Gemma.Heatmap` JS to render a
  coloured strip above each row.
- `List<String> sampleNames`.

### 2.3 The "organized by factors" code — what makes it horrible

Three layered offences:

1. **Double-overloaded layout type.** `Map<BioAssayValueObject,
   LinkedHashMap<ExperimentalFactor, Double>>` — the `Double` value
   means a measurement for continuous factors and a factor-value ID for
   categorical factors. Round-tripping factor-value IDs through doubles
   means `Math.round(valueOrId)` on the read side and constant
   defensive `assert facVal != null` everywhere. Compare against the
   honest schema: `factors[].levels[] -> samples[].factorValues[factorId]`,
   indexed.

2. **In-place vector mutation as a side-effect.** The "sort vectors by
   design" call mutates every vector's `data[]` AND its
   `BioAssayDimensionValueObject.reorder(...)`, with a `setReorganized(true)`
   guard so it doesn't happen twice — but the cache key
   (`LayoutSelection(experimentId, factorId)`) excludes the primaryFactor
   slot from the comment "FIXME: if primaryFactor is non-null we can't use
   the cache as it stands" (line 95). The class is a cache around a
   mutating operation, and the FIXMEs (lines 95, 127, 240, 453, 723, 765,
   814, 826) flag the design as known-broken.

3. **Presentation layer baked into the wire format.** Hex colours
   (`#85c6ff`, `#DCDCDC` for "No value"), factor display-name
   uniquification (`name [ID=123]`), and JS-targeted nested maps are all
   on the server. The client gets a fully-coloured legend and per-sample
   swatch strip and can do nothing interactive with it — to re-sort by
   a different factor, the client has to throw the whole response away
   and call `getDEDVForDiffExVisualizationByThreshold(...,
   primaryFactorID)` again.

Concrete example — the `prepareFactorsForFrontEndDisplay` body
(`DEDVController.java:1131–1241`) is 110 lines of code for one
job: turn an already-ordered layout map into TWO parallel
nested-string-map structures with hex-colour strings interleaved.

### 2.4 Response shape today

`VisualizationValueObject` (one per EE) over DWR:

```json
{
  "eevo": { /* ExpressionExperimentValueObject, includes minPvalue */ },
  "sampleNames": ["GSM12345", "GSM12346", ...],
  "profiles": [
    {
      "id": <probeId>,
      "name": "<probeName>",
      "genes": [ { /* GeneValueObject */ } ],
      "data": [<double>, <double>, ...],     // already reordered to match layout
      "color": "red",                          // one of {red, black, blue, green, orange}
      "factor": 1,                             // valid flag, 1=normal 2=highlighted
      "pvalue": 0.0021,
      "allMissing": false
    }
  ],
  "factorProfiles": [
    {
      "isContinuous": false,
      "plots": [ [ {"x":0,"y":0}, {"x":3,"y":0} ], ... ]
    }
  ],
  "factorNames": {
    "tissue [ID=78]": {
      "liver":  "#85c6ff",
      "kidney": "#6b90ff",
      "No value": "#DCDCDC"
    },
    "treatment [ID=92]": { ... }
  },
  "factorValuesToNames": [
    { "tissue [ID=78]": ["liver", "#85c6ff"], "treatment [ID=92]": ["control", "#82b998"] },
    /* one entry per sample, in layout order */
  ]
}
```

Already-reordered double arrays. Hex colours baked in. Factor IDs
encoded in display names. No way for the client to recompute "what if I
sort by treatment first".

### 2.5 Vector loader feeding the assembler

The loader chain is clean and we keep it. Same pattern across all DEDV
endpoints:

```
ProcessedExpressionDataVectorService
    .getProcessedDataArrays(ees, geneIds)       // for genes
    .getProcessedDataArraysByProbe(ees, probes) // for probes
    .getRandomProcessedDataArrays(ee, n)        // sample
    .getDiffExVectors(resultSetId, threshold, max)
    .getExpressionLevelsByIds(...)              // already used by REST
SVDService.getTopLoadedVectors(ee, component, count)  // PCA
```

These all return `Collection<DoubleVectorValueObject>` (the
single-vector wire type, which is fine — it carries
`bioAssayDimension`, `data`, `genes`, `pvalue`, designElement, eevo).
This is the right cleavage line for a new endpoint: load the same
vectors, ship them with `bioAssays + factorValues` meta INSTEAD of
piping them through `sortVectorDataByDesign + prepareFactorsForFrontEndDisplay`.

## 3. Precedent — endpoints already shipping raw matrix + meta

**Yes, sort of: SVD.** `GET /datasets/{dataset}/svd` returns
`SimpleSVDValueObject` (`DatasetsWebService.java:4055`):

```java
@Value
public static class SimpleSVDValueObject {
    List<Long> bioAssayIds;       // axis label IDs, in V-matrix row order
    List<Long> bioMaterialIds;
    double[] variances;
    double[][] vMatrix;
}
```

This is exactly the pattern: raw `double[][]`, parallel ID arrays for
the rows, NO ordering or palette decisions on the server. The new
heatmap-data endpoint should mirror this — `double[][] values`, plus
ID-bearing row+column metadata objects, plus a flat factor catalogue.

The other obvious precedent is `GET /datasets/{dataset}/data/processed`
(line 2899) and `/data/raw` (line 2965), which both ship gzip-streamed
TSV. Same value (raw matrix + meta) but in a different transport
(streaming text). Worth considering as an alternative transport for the
big-matrix case (see §5.3).

There is no `sampleCorrelation` REST endpoint today —
`SampleCorrelationAnalysisService` is internal-only, exposed only as an
audit event type. So the SVD endpoint is the only existing
"matrix-of-doubles + axis meta" precedent.

## 4. gemma-curation-ui sketch widget

### 4.1 Location

`~/Dev/gemma-curation-ui/apps/browser/src/lib/heatmap/`:
- `HeatmapWidget.tsx` (696 LOC) — controls strip, legend, footer.
- `Heatmap.tsx` — react wrapper around the renderer.
- `render.ts`, `layout.ts`, `color.ts` — canvas renderer.
- `palettes.ts` — `ambsky` (diverging) and `blackbody` (sequential).
- `types.ts` — `HeatmapData`, `CategoricalAnnotation`, `HeatmapConfig`.

Two consumers in the repo:
- `apps/browser/src/features/heatmap-demo/HeatmapDemo.tsx` — uses
  synthetic `buildSyntheticData(100, 60)`. Not wired to Gemma data.
- `apps/browser/src/features/home/variants/HomeHeatmap.tsx` — uses
  taxon × annotation coverage placeholder data, not vector data.

So zero current Gemma-API callers. The widget is purely a sketch; we
are free to define the wire format around it.

### 4.2 Current API expectations

From `apps/browser/src/lib/heatmap/types.ts`:

```ts
export interface HeatmapData {
  values: CellValue[][];          // row-major; CellValue = number | null
  rowLabels?: string[];
  colLabels?: string[];
  colAnnotations?: CategoricalAnnotation[];
}

export interface CategoricalAnnotation {
  name: string;
  values: Array<string | null>;   // length == numCols
  palette: Record<string, string>; // category -> CSS color
}
```

Client-side controls already shipped: palette switcher (diverging vs
sequential), clip slider, row-standardize toggle, cell-size sliders,
fit-mode (squeeze vs expand-and-scroll), hover tooltip. The client owns
all colouring, scaling, and sizing decisions.

### 4.3 What the widget would want (if we were designing fresh)

The widget's `HeatmapData` is a presentation-grade shape — it already
HAS the palette baked in (each `CategoricalAnnotation` carries its own
`palette: Record<string, string>`). That's a sketch convenience for
demo data. For a real Gemma payload, we want to push the palette
decision down even further — ship ONLY category values per sample, let
the widget compute/configure the palette.

So the desired wire format is more honest:

```ts
// What the wire ships:
interface HeatmapPayload {
  matrix: { values: number[][]; encoding: "json" | "base64f32" };
  rows: RowMeta[];     // probe / gene rows
  columns: ColumnMeta[]; // sample columns
  factors: FactorMeta[];
}
interface ColumnMeta {
  bioAssayId: number;
  bioMaterialId: number;
  name: string;
  factorValues: Record<number /*factorId*/, number /*factorValueId*/ | null>;
}
interface FactorMeta {
  id: number;
  name: string;
  type: "categorical" | "continuous";
  levels: { id: number; value: string }[];   // empty for continuous
  measurements?: Record<number /*sampleId*/, number>; // continuous only
}
```

Then a small client-side adapter turns `HeatmapPayload` into the
widget's `HeatmapData` + computes palettes from `factors`. The sort /
group-by-factor / reorder-rows logic is then trivial client-side
operations on `columns[]` and the indexed `factorValues` lookup.

## 5. Proposed wire format

### 5.1 Endpoint shape

New JAX-RS resource on `DatasetsWebService` (or a new
`DatasetVisualizationWebService`):

```
GET /datasets/{dataset}/heatmap-data
    ?genes={geneIds:csv}        # one of genes, probes, or random N
    ?probes={probeIds:csv}
    ?sampleSize={n}             # default 20, max 150
    ?resultSet={resultSetId}    # diffex-driven (replaces getDEDVForDiffExVisualizationByThreshold)
    ?threshold={p}              # diffex
    ?pcaComponent={c}           # PCA-loaded vectors
    ?pcaCount={n}
    ?encoding=json|base64f32    # default json
```

This single endpoint subsumes 5 of the 6 DEDV methods. Coexpression
(`getDEDVForCoexpressionVisualization`) and "by experiment" overlapping
variants can be added as query-param permutations or sibling endpoints
once we see actual gemma-curation-ui call patterns.

For multi-experiment "expressions across datasets" (the existing
`/datasets/{datasets}/expressions/genes/{genes}` endpoint at line 3677),
the new endpoint returns ONE payload per dataset; the caller iterates.
That matches the existing `ExperimentExpressionLevelsValueObject`
pattern but with raw matrix instead of per-vector wrapping.

### 5.2 Schema (with open questions)

```json
{
  "datasetId": 12345,
  "datasetShortName": "GSE6789",
  "matrix": {
    "values": [[0.12, 0.45, null, 0.88], [...]],
    "encoding": "json",
    "rows": 50,
    "cols": 24,
    "quantitationType": {
      "id": 999, "name": "Log2 ratio", "isPreferred": true,
      "isRatio": true, "scale": "LOG2"
    }
  },
  "rows": [
    {
      "designElementId": 555,
      "designElementName": "ILMN_1234",
      "geneIds": [672],
      "geneSymbols": ["BRCA1"],
      "pvalue": 0.0021,        // optional, only for diffex requests
      "validated": true        // optional, only for diffex requests
    }
  ],
  "columns": [
    {
      "bioAssayId": 8888,
      "bioMaterialId": 7777,
      "name": "GSM12345",
      "outlier": false,
      "factorValueIds": {
        "78": 1234,            // factor 78 (tissue) -> FV 1234 (liver)
        "92": 5678             // factor 92 (treatment) -> FV 5678 (control)
      }
    }
  ],
  "factors": [
    {
      "id": 78,
      "name": "tissue",
      "category": "OrganismPart",
      "type": "categorical",
      "isBatch": false,
      "levels": [
        { "id": 1234, "value": "liver", "summary": "liver" },
        { "id": 1235, "value": "kidney", "summary": "kidney" }
      ]
    },
    {
      "id": 92, "name": "age", "type": "continuous", "isBatch": false,
      "measurements": { "8888": 24.0, "8889": 36.0 }
    }
  ]
}
```

Key shape decisions:
- **Row-major `values[row][col]`** — matches `ProcessedExpressionDataVector`
  natural layout (one vector per row) and the curation-ui widget's
  expectation. No transpose on the server.
- **Original BioAssayDimension order** — NO reordering. The client
  sorts.
- **IDs everywhere, display strings alongside** — so the client can
  hash, key, sort, and group without parsing strings.
- **Factor values referenced by ID** — `columns[i].factorValueIds[factorId]
  -> factorValueId`. Look up `factors[].levels[]` for the display label.
  Continuous factors use a separate `measurements: { sampleId ->
  number }` field on the factor (not on the column) — symmetric with the
  existing `MeasurementUtils.measurement2double` path.
- **NO colours, NO sort, NO legend.** Client owns those.

Open questions:
- Should `factorValueIds` for unset/missing factors be `null` or absent?
  (Affects client-side `null`-coalescing complexity.)
- Where do BioAssay → BioMaterial collapse rules live? Today, sometimes
  one sample appears twice (multi-platform). The current code does
  `seenSamples.add(ba.getSample())` and silently drops duplicates
  (`DEDVController.getSampleNames`:861). Either ship the duplicates
  honestly with a `bioMaterialId` link, or collapse on the server with
  a `mergedAssays: number[]` provenance field.
- Subset support: today subsets pass through `SlicedDoubleVectorValueObject`
  with a `sourceBioAssayDimension`. New endpoint should accept
  `subSet={subSetId}` and return only the subset's columns; matrix
  values come pre-sliced.
- Should the response embed gene-level metadata (entrez, ensembl, taxon)
  per row, or just `geneId` and let the caller resolve? (Bandwidth vs.
  round-trips.) Recommend: `geneId + officialSymbol`, no more.

### 5.3 Encoding decision matrix

Typical heatmap is 20–150 probes × 10–500 samples = 200–75,000 doubles
= 1.6–600 KB at 8 bytes each.

| encoding | wire size | parse cost | tooling | recommendation |
|---|---|---|---|---|
| JSON `number[][]` (default) | ~1.6× raw (12–15 chars per double) | native `JSON.parse` | universal | default; ship `Content-Encoding: gzip` always |
| base64-encoded `Float32Array` | 1.33× raw (after base64) | `Uint8Array.from(atob)` + `Float32Array` view | trivial | opt-in via `?encoding=base64f32` for >50k cells |
| MessagePack | ~1.0× raw, NaN-clean | extra dep | nice but extra | skip for v1 |
| Apache Arrow | 1.0× raw, columnar, zero-copy if shared | very heavy client dep | overkill | skip — solves a problem we don't have |

Recommendation: ship JSON `number[][]` by default with gzip (the
`/datasets/{id}/data/processed` endpoint already uses `@GZIP`), and
expose `?encoding=base64f32` as a known opt-in for the
"large-matrix-large-screen" case. Defer MessagePack/Arrow.

Streaming vs one-shot: heatmap matrices are bounded (`MAX_RESULTS_TO_RETURN
= 150` probes today). One-shot is fine. The streaming `/data/processed`
endpoint is for the FULL EE matrix and is a different use case.

Preview tier: probably not needed at these sizes. The widget's
`squeeze` fit mode already handles >>1000-column rendering by merging
sub-pixel columns at render time. If a future use case needs a
1M-cell preview, add `?downsample=preview` later.

## 6. Blast radius

### 6.1 Existing callers of the current endpoint

**Gemma Web (legacy DWR):**
- `gemma-web/src/main/webapp/scripts/api/visualization/VisualizationWidget.js`
  (line 1140, 1153, 1191) — three call sites for diffex / coexpression /
  visualization.
- `gemma-web/src/main/webapp/scripts/api/diff/ProbeLevelDiffExGrid.js`
- `gemma-web/src/main/webapp/scripts/api/entities/analysis/differentialExpression/DifferentialExpressionAnalysesSummaryTree.js`
- `gemma-web/src/main/webapp/scripts/api/entities/experiment/EEManager.js`
- `gemma-web/src/main/webapp/scripts/api/visualization/Heatmap.js` —
  the consumer of the `factorNames` + `factorValueMaps` + `factorProfiles`
  fields. Renders the coloured strip ABOVE the heatmap.

Per user memory `project_gemma_web_replacement.md`: gemma-web is being
replaced by gemma-curation-ui. So these legacy callers are walking dead;
we don't have to migrate them. They can keep calling the old DEDV
endpoints until the gemma-web retirement removes them en bloc.

**gemma-curation-ui:** ZERO current callers. The `HeatmapWidget` only
consumes synthetic + placeholder data. Greenfield client-side migration.

**gemma-rest:** ZERO. No public REST endpoint serves the
`VisualizationValueObject` shape today.

### 6.2 Java DTO/VO classes that go away (eventually)

After phase 4 (DEDV retirement):
- `VisualizationValueObject` (312 LOC) —
  `gemma-web/src/main/java/ubic/gemma/web/controller/visualization/`
- `FactorProfile` (195 LOC) — same package
- `GeneExpressionProfile` — same package (sibling to FactorProfile)
- `DoublePoint` — same package
- The `sortVectorDataByDesign` + `prepareFactorsForFrontEndDisplay` +
  `createFactorNameToColoursMap` chain inside `DEDVController`.
- `ExperimentalDesignVisualizationServiceImpl` (538 LOC) — if NO callers
  outside the DEDV path. The `getExperimentalDesignLayout(ee, bds,
  primaryFactor)` static-ish helper IS called by `plotExperimentalDesign`
  (private test method) only; the public surface
  (`sortVectorDataByDesign`) is DEDV-only. Confirm via final grep before
  deleting.
- `ExperimentalDesignVisualizationService` interface (44 LOC).
- `DifferentialExpressionAnalysisResultSetVisualizationValueObject`
  (separate metaheatmap legacy, may also retire).

`ExpressionDataHeatmap` (the JFreeChart static-PNG class) STAYS — it's
used by the PNG endpoints for the static `expressionExperiment.detail.jsp`
heatmap and is independent of the DEDV pipeline. Eventually that PNG
path also goes away when gemma-curation-ui covers the EE-detail page,
but that's a separate cleanup.

### 6.3 Tests that need updating

After phase 4: any test asserting `VisualizationValueObject` shape or
calling `sortVectorDataByDesign`. Quick search:
- `grep -r 'sortVectorDataByDesign\|VisualizationValueObject\|FactorProfile' gemma-core/src/test gemma-web/src/test`
  — defer to actual deletion session.

For phase 2 (additive new endpoint): only NEW tests. No existing tests
break.

## 7. Phased plan

| phase | session | scope | deliverable |
|---|---|---|---|
| S1 | this recce | design + inventory | this document, committed |
| S2 | next | additive — new endpoint | `GET /datasets/{id}/heatmap-data` on `DatasetsWebService` (or a new `DatasetVisualizationWebService`), backed by a thin assembler that reuses `ProcessedExpressionDataVectorService` / `SVDService`. Ships matrix + meta as proposed in §5. NO change to DEDV controller. Add `restapidocs/examples/dataset-heatmap-data.json`. JerseyTest5-style endpoint test against a small EE fixture. |
| S3 | next + 1 | client integration | gemma-curation-ui: thin adapter from new payload to `HeatmapWidget`'s `HeatmapData`, sort-by-factor UI, group-by-level UI. Wire HeatmapDemo + a real "EE detail" page consumer. |
| S4 | after gemma-web retirement | deprecation | once gemma-web is no longer shipped, retire `DEDVController.getDEDVFor*`, delete `VisualizationValueObject` / `FactorProfile` / `GeneExpressionProfile` / `ExperimentalDesignVisualizationServiceImpl`. Migrate legacy `metaheatmap` JS or drop. |

Risk note: S2 + S3 can overlap. S4 cannot start before gemma-web is
demonstrably unshipped (per user memory, that's an in-flight project).

## 8. Open questions for Paul

1. **Endpoint placement.** Should the new endpoint live on
   `DatasetsWebService` (already 4000+ LOC) or carve out a new
   `DatasetVisualizationWebService`? Lean: new service, alongside
   `/svd`, `/{dataset}/data/processed`, and the eventual heatmap-data.
2. **Multi-EE response.** Today `getDEDVForVisualization(eeIds,
   geneIds)` takes a Collection of EEs and returns one VVO per EE.
   Should the new endpoint be single-EE (cleaner) or accept
   `?datasets={csv}` (matches existing `expressions/genes` pattern)?
   Lean: single-EE, let the client batch.
3. **Sample-on-multiple-platforms collapse.** The current code silently
   drops duplicate BioAssays for the same BioMaterial. Drop on the
   server (matches today), or ship duplicates with a `mergeKey`?
4. **Continuous factor representation.** Per-factor `measurements:
   {sampleId -> double}` map, or denormalised onto each column as
   `column.continuousFactorValues[factorId] -> double`? Lean: per-factor
   map, keeps the column shape uniform.
5. **What about the diffex-driven "validated probes" highlight?** Today
   `getDEDVForDiffExVisualization*` returns a `validatedProbes` set and
   the JS uses it to highlight rows. Carry forward as
   `rows[].validated: boolean`?
6. **Coexpression mode.** The coexpression endpoint
   (`getDEDVForCoexpressionVisualization`) is a two-gene join. Does it
   stay as a separate endpoint or fold into the general one with
   `?validateCoexpression=true`?
7. **PNG endpoint future.** `GET /expressionExperiment/visualizeHeatmap.html`
   serves a server-rendered PNG for the static EE detail page. Do we
   keep it forever (lightweight EE-list preview), or retire when
   gemma-curation-ui takes over the EE page? Affects whether
   `ExpressionDataHeatmap` (JFreeChart, 257 LOC) survives.
8. **Subset semantics.** `?subSet={id}` returns only the subset's
   samples — fine — but should the `factors[]` block include factors
   that are present in the parent EE but constant within the subset?
   Lean: include with a `constantWithin: true` flag so client can
   suppress in the UI.
