# Gene page rework — call-surface recce

Baseline: worktree branch `gemma-web-gene-page-recce` at `def6cefcb1`. Read-only recce; no source touched.

## 1. Executive summary

The legacy gemma-web "gene details" page (`/gene/showGene.html`) is a thin
`gene.detail.jsp` (38 lines) that renders six hidden form inputs and bootstraps an
Ext.js `Gemma.GenePage` tab panel. The JSP itself is therefore not the surface —
the surface is the Ext.js stack rooted at
`gemma-web/src/main/webapp/scripts/api/entities/gene/GenePage.js`. **Most of that
surface has already been disabled by code comments.** What ACTIVELY runs today is
just two tabs:

- **Overview** (`Gemma.GeneDetails`) — one DWR call (`GeneController.loadGeneDetails`)
  returning a heavily-populated `GeneValueObject` (aliases, homologues, gene-sets,
  multifunctionality, composite-sequence count, platform count).
- **Elements** (`Gemma.GeneElementsPanel`) — `CompositeSequenceController.getGeneCsSummaries`
  for the grid; `CompositeSequenceController.getGeneMappingSummary` for the selected-row
  detail panel.

Five further tabs/sections are present in `GenePage.js` but commented out: GO Terms
grid, Coexpression grid, Differential Expression grid, Phenocarta (PhenotypeEvidence)
grid, Allen Brain Atlas images. None of those bind to live DWR calls in the current
DOM. The COEXPRESSION_ORPHAN_RECCE confirms gene-gene coexpression is dead at the
Java + DB layer; the rest are functionally inert (server-side Java may exist but
the UI never fires them).

The new minimal page in gemma-curation-ui already covers: gene metadata (REST
`/genes/{id}`), locations (`/genes/{id}/locations`), GO terms (`/genes/{id}/goTerms`),
and a DiffEx placeholder section. The big gap vs legacy is the **Elements tab**
(probes + per-probe sequence/alignment details) — there is no client surface in
the new UI and only a partial REST equivalent (`/genes/{gene}/probes` returns
`CompositeSequenceValueObject` but lacks the "raw summary" rollup the legacy grid
uses, and there is no `/probes/{id}/geneMappingSummary` REST endpoint at all).

## 2. Legacy gemma-web gene page surface

### 2.1 Backing controller / JSP

- **Controller:** `gemma-web/src/main/java/ubic/gemma/web/controller/genome/gene/GeneController.java:64`
  — `@RequestMapping(value = { "/gene", "/g" })`. `show(...)` (line 102, mapped to
  `/showGene.html` + `/`) accepts `id` / `ncbiid` / `ensemblId` / `(name, taxon)` and
  pushes a `GeneValueObject` (loaded via `geneService.loadValueObjectById` /
  `findByNCBIIdValueObject` / `findByEnsemblId` / `findByOfficialSymbol`) into the
  Spring MVC model as `gene`. Forwards to view `gene.detail`.
- **JSP:** `gemma-web/src/main/webapp/pages/gene.detail.jsp` — 38 lines, all of which
  are six hidden inputs (`geneId`, `geneNcbiId`, `geneSymbol`, `geneName`,
  `geneTaxonName`, `geneTaxonId`) and an `Ext.onReady` that instantiates
  `Gemma.GenePage` inside a `Gemma.GemmaViewPort`. No server-rendered tables.
- **Ext root:** `gemma-web/src/main/webapp/scripts/api/entities/gene/GenePage.js` —
  defines tab order. Lines 41-55 currently `add()` only **Overview** + **Elements**.
  Lines 57-87 (Coex, GO grid, Allen Brain Atlas) and lines 62-65 (Phenotype tab)
  are commented out. `initDiffExTab` (line 126) and `initCoexTab` (line 105) are
  still defined but never called.
- **Tab-1 (Overview):** `GeneDetailsTab.js:325` — single DWR call fired in
  `afterrender`.
- **Tab-2 (Elements):** `GeneElementsPanel.js` → `PlatformElementGrid` (grid by
  `CompositeSequenceController.getGeneCsSummaries`) + `SequenceDetailsPanel` (per-row
  detail; calls `CompositeSequenceController.getGeneMappingSummary` on row select).

### 2.2 DWR calls (ACTIVE — what fires today)

| service.method | returns | freq | cost | replacement |
|---|---|---|---|---|
| `GeneController.loadGeneDetails(Long id)` | `GeneValueObject` populated with `aliases`, `homologues`, `geneSets`, `multifunctionalityRank`, `numGoTerms`, `compositeSequenceCount`, `platformCount`, `phenotypes`, `nodeDegreesPos/Neg/Ranks` | 1× per page render (Overview tab) | medium aggregate; fans into homologene service (futureUtils), gene-set search, multifunctionality, plus 2 DAO count queries (`getCompositeSequenceCountById` + `getPlatformCountById`) | partial — `/rest/v2/genes/{id}` returns `GeneValueObject` but lacks homologues, gene-sets, multifunctionality rank, and the two counts. **Needs new endpoint** OR enrichment of existing one. |
| `CompositeSequenceController.getGeneCsSummaries(Long geneId)` | `Collection<CompositeSequenceMapValueObject>` — one row per probe across all platforms, with embedded `genes` list and `numBlatHits` | 1× when Elements tab activated (deferred — first activation only since `GenePage.deferredRender = true`) | medium-to-heavy. Fans into `geneService.getCompositeSequencesById(id, true)` → `compositeSequenceService.getRawSummary(...)` → `arrayDesignMapResultService.getSummaryMapValueObjects(...)`. The `getRawSummary` path is a SQL aggregation. TP53-class genes have hundreds of probes; cost roughly tracks probe count. | partial — `/rest/v2/genes/{gene}/probes` (GeneWebService line 148) returns paginated `CompositeSequenceValueObject`, NOT the same shape. Lacks per-row gene-list aggregation + BLAT hit count. **Needs new endpoint OR a new "summary" projection on the existing one.** |
| `CompositeSequenceController.getGeneMappingSummary(EntityDelegator<CompositeSequence> csd)` | `Collection<GeneMappingSummary>` — for one composite sequence, returns its BLAT alignments + biological-sequence metadata + the genes each alignment supports | 1× per row click in the Elements grid | medium. `compositeSequenceService.getGeneMappingSummary(biologicalCharacteristic, voWithGeneMappingSummary)` — joins BLAT results + sequence metadata + gene-product mappings. | **needs new endpoint** — no REST equivalent. This is the per-probe drill-down panel ("Sequence: name, type, repeat-masked %, polyA, BLAT alignments table"). |

### 2.3 DWR calls (DORMANT — defined but unwired)

| service.method | what it would return | status |
|---|---|---|
| `GeneController.findGOTerms(Long id)` | `Collection<AnnotationValueObject>` | called by `loadGeneDetails` to populate `numGoTerms` count, but the full GO-grid tab is commented out (`GenePage.js:67-76`). REST: covered by `/genes/{gene}/goTerms`. |
| `GeneController.getProducts(Long id)` | `Collection<GeneProductValueObject>` | no caller in the active JS. REST: no direct equivalent — drilldown only. |
| `DifferentialExpressionSearchController.getDifferentialExpressionWithoutBatch` | per-experiment DEA hits for one gene | called only by `ProbeLevelDiffExGrid` which is no longer added to the tab panel (`GenePage.js:57` commented). PERF_PROBE_REPORT_ROUND3 §C1 measured the underlying `findByGene` DAO: 29k rows for TP53, 4 s cold / 0.55 s warm. **Hot-path candidate for the redo.** |
| `DEDVController.getDEDVForDiffExVisualizationByExperiment` | DEDV visualization for one EE × gene | only fired from the now-dormant DiffEx grid. |
| `CoexpressionGridLight.doSearch(...)` → `ExtCoexpressionSearchController` | coex links | tab commented out; underlying tables are confirmed orphans (`COEXPRESSION_ORPHAN_RECCE.md`). |
| `PhenotypeController.*` (Phenocarta) | phenotype-evidence rows | tab commented out; Phenocarta is functionally retired. |
| `GeneAllenBrainAtlasImages` (xtype) | ABA in-situ thumbnails | tab commented out. |

### 2.4 Server-rendered data

Only the four `<input type="hidden">` values in `gene.detail.jsp` and the page
`<title>`. Everything else is client-rendered from DWR responses.

## 3. New minimal gene page (gemma-curation-ui) — current calls

Source: `~/Dev/gemma-curation-ui/apps/browser/src/features/gene/GenePage.tsx`
+ `~/Dev/gemma-curation-ui/apps/browser/src/api/endpoints.ts` (lines 594-617).

| endpoint | returns | already-exists | needed-for-redo |
|---|---|---|---|
| `GET /rest/v2/genes/{id}` | `GeneValueObject` (id, ncbiId, ensemblId, officialSymbol/Name, aliases, description, taxon) | yes — `GeneWebService.getGenesByIds` line 115 (handles single-element list path) | **enrich** — add homologues, gene-sets, multifunctionality rank, GO-term count, composite-sequence count, platform count, OR add a sibling `/genes/{id}/overview` "fat VO" endpoint that mirrors `loadGeneDetails` |
| `GET /rest/v2/genes/{id}/locations` | `List<PhysicalLocationValueObject>` | yes — `GeneWebService.getGeneLocations` line 132 | no change |
| `GET /rest/v2/genes/{id}/goTerms` | `List<GeneOntologyTermValueObject>` | yes — `GeneWebService.getGeneGoTerms` line 210 | no change |
| (diffex placeholder — no call yet) | — | partial; see §4.2 | needs new endpoint (see §4.2) |

## 4. Gap analysis

### 4.1 Features in legacy not in new (live features only — ignoring dormant tabs)

1. **Elements grid** — paginated probe listing across all platforms with per-row
   gene-mapping count and BLAT-hit count. Legacy: `getGeneCsSummaries` (one DWR
   call, full list). New: `/genes/{gene}/probes` exists but returns a different
   shape and is paginated. Needs either a "summary" projection or an enrichment.
2. **Per-probe drill-down (SequenceDetailsPanel + GenomeAlignmentsGrid)** —
   BLAT alignments, sequence type, length, repeat-masked %, polyA, NCBI link,
   "genes assayed by this probe" cross-link. No REST equivalent.
3. **Multifunctionality rank** — surfaced in the Overview field list. Lives on
   `GeneValueObject.multifunctionalityRank` but is only populated by
   `loadFullyPopulatedValueObject` (line 240, `GeneServiceImpl`). `/genes/{id}`
   uses the lighter loader and skips it.
4. **Homologues** — same as multifunctionality: present on the VO field but only
   populated by `loadFullyPopulatedValueObject` (uses `homologeneService` +
   `thawLite`). Not on the REST path.
5. **Gene-set membership** — same pattern: VO has `geneSets`, only populated by
   `loadFullyPopulatedValueObject` via `geneSetSearch.findByGene(gene)`.
6. **Composite-sequence + platform counts** — displayed as "N elements on M
   different platforms" in the Overview. Lives on `GeneValueObject.compositeSequenceCount`
   / `platformCount`, only populated by the fat loader.
7. **NCBI symbol icon link** — trivial; new page already has equivalent
   ExternalLink chip. No work.

### 4.2 Endpoints that need NEW backing in gemma-rest

Ranked by likely value-per-effort:

1. **`GET /genes/{gene}/overview`** (or enrich `/genes/{gene}`) — returns the
   `loadFullyPopulatedValueObject` shape: homologues + gene-sets + MF rank + GO
   count + CS count + platform count. Replaces the legacy `GeneController.loadGeneDetails`
   DWR. One round-trip per page render.
2. **`GET /genes/{gene}/probes/summary`** (or add a `?summary=true` projection to
   the existing `/genes/{gene}/probes`) — returns `CompositeSequenceMapValueObject`
   shape (per-row genes list + numBlatHits + arrayDesign short-name/long-name).
   Replaces `CompositeSequenceController.getGeneCsSummaries`.
3. **`GET /platforms/{platform}/probes/{cs}/mappingSummary`** (or
   `/probes/{cs}/mappingSummary` if probe IDs are global) — returns
   `GeneMappingSummary` (BLAT alignments + biological-sequence metadata + supported
   genes for one probe). Replaces `CompositeSequenceController.getGeneMappingSummary`.
4. **`GET /genes/{gene}/differentialExpression`** — paginated per-experiment DEA
   hits for one gene. PERF_PROBE_REPORT_ROUND3 §C1 documented the underlying
   `DifferentialExpressionResultDaoImpl.findByGene`: 29,138 rows for TP53, 4 s cold
   / 0.55 s warm. **Add this only after the cold-cache mitigation lands** (§5).
   Required for the DiffEx-coming-soon section to ship.

### 4.3 Orphaned legacy features (don't reimplement)

- **Coexpression tab + node-degree sparkline** — gene-gene coex tables confirmed
  orphans (`COEXPRESSION_ORPHAN_RECCE.md`, ~146 GB on prod, retirement migration
  drafted). The Overview field "Coexpression: max support N" is dead-on-arrival.
- **Phenocarta / PhenotypeEvidence tab** — service still present in core (used by
  the dormant `PhenotypeEvidenceGridPanel`), but the tab is commented out in
  legacy and the system is functionally retired. Confirm with Paul before deleting.
- **Allen Brain Atlas image strip** — also commented out; the only consumer was
  the dormant `Gemma.GeneAllenBrainAtlasImages` xtype.
- **GO-term grid tab** (`Gemma.GeneGoGrid`) — replaced by the new page's
  `GoTermsSection`. Keep the new flat list; don't port the grid.

## 5. Hot-path candidates for the redo

Ranked by cold-cache risk based on the PERF_PROBE_REPORT and the underlying DAO
shape (per `project_perf_hotspot_priorities.md` memory — DEA result retrieval is
one of the four canonical hotspots).

1. **`findByGene` (DEA results for one gene)** — measured. PERF_PROBE_REPORT_ROUND3
   §C1, YELLOW: TP53 = 29,138 rows, 4 s cold, 0.55 s warm. The cold path is what
   bites — query-cache is engaged (`setCacheable(true)`), but ehcache region size
   needs auditing in prod. The follow-on `Hibernate.initialize(r.getProbe())` +
   `Hibernate.initialize(r.getContrasts())` walks fire one SELECT per batch of 128
   (so ~230 batches per association × N associations). **Mitigation direction:**
   either a startup warm-up for top-N most-viewed genes (TP53 / BRCA1 / TNF /
   etc. from server logs), or a denormalised `GENE_DEA_RESULT_SUMMARY` cache
   table (similar precedent: `EE2CHARACTERISTIC` denorm).
2. **`getGeneCsSummaries` (per-gene probe rollup)** — not directly probed but
   the call shape is N+1-prone: `getCompositeSequencesById(id, true)` followed by
   `getRawSummary(Collection<CompositeSequence>)`. TP53 maps to hundreds of
   composite sequences across all platforms, each contributing a row aggregation.
   Suggests batching the raw-summary SQL and/or memoising per gene+
   platform-set.
3. **`loadFullyPopulatedValueObject` (the Overview fat VO)** — fans into 5+
   sub-queries (`thaw(gene)`, `geneSetSearch.findByGene`, `homologeneService` via
   `FutureUtils.get(...)`, `thawLite(geneHomologues)`, `loadValueObjects(homologues)`,
   `getCompositeSequenceCountById`, `getPlatformCountById`). Each is cheap
   individually but they're sequential. A single-shot SQL projection (or even a
   parallel `CompletableFuture.allOf` rewrite) would compress page-render
   latency for the Overview from "perceptibly slow" to "instant".
4. **`getGeneMappingSummary` (per-probe drilldown)** — fires only on row-click so
   the user-perceived cost is one-off, not page-render. Lower priority. Should
   still ship as a REST endpoint for the Elements drill-down.

## 6. Open questions for Paul

1. **Scope of the redo** — do you want the new page to MATCH the legacy live
   surface (Overview + Elements only), or to ADD back the dormant tabs
   (DiffEx is the one with real value; coex / phenocarta / ABA appear dead)?
2. **Elements view shape** — keep the legacy "all probes across all platforms,
   one big table" model, or restructure as "group by platform, one mini-table
   per AD"? The legacy `PlatformElementGrid` is dual-purpose (also used by the
   ArrayDesign page) which constrained its design; the new SPA can decouple.
3. **Multifunctionality + homologues + gene-sets in `/genes/{id}`** — do we
   enrich the existing endpoint (one fat VO, breaking-change risk for other
   `/genes/{id}` consumers) or add `/genes/{id}/overview` as a new path?
   Pattern in DatasetsWebService leans toward the latter.
4. **DiffEx cold-cache strategy** — startup warm-up for TP53-class genes vs.
   building `GENE_DEA_RESULT_SUMMARY` denorm table. The denorm path is more
   work but pays off on every cold gene; the warm-up only helps a known list.
   PERF_PROBE_REPORT_ROUND3 §5 leans toward the denorm but defers the call.
5. **Phenocarta retirement** — confirm we can delete `PhenotypeEvidenceGridPanel`
   + backing services in the same sweep, or keep the Java behind a feature flag
   for the curation team's own use.
