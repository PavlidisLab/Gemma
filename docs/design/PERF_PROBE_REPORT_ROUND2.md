# Live-gemd perf probe — round 2 (post-ACL-EXISTS) (2026-05-20 phase2-acl-migrate)

## Setup

- Worktree tip: `c45c236fd209b1eabcdd3b15786c024be08a4690` (perf-probe-round2, baselined at c45c236fd2).
- Probe machine: Darwin 24.6.0 (PAVNOTE-18).
- mysql client: `mysql Ver 14.14 Distrib 5.7.31, for macos10.14 (x86_64)`.
- Server: MySQL 5.7.44 (Percona-flavoured `5.7.44-48-log`).
- Connect+ping baseline against `127.0.0.1:8000` tunnel: **~110-150 ms per connection** (`SELECT 1` standalone). This is the dominant single-round-trip cost — any analysis below subtracts ~120ms from per-connection wall times to back out "SQL only".
- READ-ONLY: SELECT/EXPLAIN only. No writes, no ANALYZE, no schema changes.
- All probes target prod database `gemd`.

### Cardinality reference (gemd, prod, refresh of round-1 numbers where relevant)

| table | rows | notes |
|---|---|---|
| INVESTIGATION (class=ExpressionExperiment) | 25,668 | unchanged |
| CURATION_DETAILS | 25,814 | 1:1 with EE; ATTENTION_AUDIT_EVENT_FK cardinality 16,767 (most EEs have one) |
| AUDIT_EVENT | 3,657,351 (PK cardinality on tunnel) | round-1 reported 26.68 M; the discrepancy is MySQL's index-statistics estimate vs `SELECT COUNT(*)`. Don't read too much into the absolute |
| AUDIT_EVENT_TYPE | 838,176 | one row per AE essentially — a polymorphic discriminator table |
| BIO_ASSAY (for EE 92401) | 92 | sample-rich representative |
| FACTOR_VALUE (for EE 57493, GSE Malhotra-2025.2) | 175 | rich-design representative |
| CHARACTERISTIC under those FVs | 83 | for the same EE |

### Test EE selection

- **EE 92401 (GSE254569)** — 92 samples, 18,578 vectors, 20 audit events. Recent. Used for the detail / sample / audit-event probes.
- **EE 57493 (Malhotra-2025.2)** — 7 factors, 175 factor values, 83 FV characteristics. Used for the `/design` N+1 simulation.

## Probe inventory

### Probe 1: `/datasets/{id}` detail load — single EE + eager CurationDetails + 3 AuditEvent + 3 AuditEventType

- DAO surface: `ExpressionExperimentService.loadValueObjectsWithCache` → `ExpressionExperimentDaoImpl.doLoadDetailsValueObjects` (line 2110); the entity-side eager joins come from `Investigation.hbm.xml` (curationDetails `fetch="join"`, lazy=false), `CurationDetails.hbm.xml` (three lastXEvent `fetch="join"`, lazy=false), and `AuditEvent.hbm.xml` (eventType `fetch="join"`, lazy=false). Hibernate compiles these into ONE `SELECT` against `INVESTIGATION` with seven LEFT JOINs.
- SQL (status quo):
  ```sql
  SELECT ee.*, cd.*, lne.*, lne_t.*, lnu.*, lnu_t.*, lt.*, lt_t.*
  FROM INVESTIGATION ee
  JOIN CURATION_DETAILS cd ON cd.ID = ee.CURATION_DETAILS_FK
  LEFT JOIN AUDIT_EVENT lne ON lne.ID = cd.ATTENTION_AUDIT_EVENT_FK
  LEFT JOIN AUDIT_EVENT_TYPE lne_t ON lne_t.ID = lne.EVENT_TYPE_FK
  LEFT JOIN AUDIT_EVENT lnu ON lnu.ID = cd.NOTE_AUDIT_EVENT_FK
  LEFT JOIN AUDIT_EVENT_TYPE lnu_t ON lnu_t.ID = lnu.EVENT_TYPE_FK
  LEFT JOIN AUDIT_EVENT lt  ON lt.ID  = cd.TROUBLE_AUDIT_EVENT_FK
  LEFT JOIN AUDIT_EVENT_TYPE lt_t ON lt_t.ID = lt.EVENT_TYPE_FK
  WHERE ee.ID = ? AND ee.class='ExpressionExperiment';
  ```
- EXPLAIN: all `const` lookups (PRIMARY key). Plan is trivial — every join is `const` because the driving `ee.ID=?` resolves to a unique row, then every FK is PRIMARY-key resolved.
- Timing (3 runs incl ~120ms connect): 110 / 124 / 124 ms — `~120 ms mean`, of which the SQL itself is **<10 ms**.
- Compare: bare `SELECT ee.* WHERE ee.ID=?` is 103-165 ms; the 7 eager joins add **nothing measurable** at single-EE scale.
- Verdict: **GREEN** at single-EE scale.

### Probe 2: Same eager-join shape at LIMIT-20 listing scale (the actual detail-VO surface)

- DAO surface: this is what `loadValueObjectsWithCache` actually fires when feeding `/datasets?limit=20` and similar — the eager joins multiply across the LIMIT-window of EE rows. Now post-ACL-EXISTS, the outer ACL filter is a subquery rather than a join.
- SQL:
  ```sql
  SELECT ee.ID, ee.SHORT_NAME, cd.ID, cd.TROUBLED, cd.NEEDS_ATTENTION,
         lne.ID, lne.DATE, lne_t.class,
         lnu.ID, lnu.DATE, lnu_t.class,
         lt.ID,  lt.DATE,  lt_t.class
  FROM INVESTIGATION ee
  JOIN CURATION_DETAILS cd ON cd.ID = ee.CURATION_DETAILS_FK
  LEFT JOIN AUDIT_EVENT      lne   ON lne.ID   = cd.ATTENTION_AUDIT_EVENT_FK
  LEFT JOIN AUDIT_EVENT_TYPE lne_t ON lne_t.ID = lne.EVENT_TYPE_FK
  LEFT JOIN AUDIT_EVENT      lnu   ON lnu.ID   = cd.NOTE_AUDIT_EVENT_FK
  LEFT JOIN AUDIT_EVENT_TYPE lnu_t ON lnu_t.ID = lnu.EVENT_TYPE_FK
  LEFT JOIN AUDIT_EVENT      lt    ON lt.ID    = cd.TROUBLE_AUDIT_EVENT_FK
  LEFT JOIN AUDIT_EVENT_TYPE lt_t  ON lt_t.ID  = lt.EVENT_TYPE_FK
  WHERE ee.class='ExpressionExperiment'
    AND EXISTS (… anonymous-ACL subselect …)
  ORDER BY ee.ID DESC LIMIT 20;
  ```
- Timing (2 runs): 502 / 515 ms — **~510 ms mean**.
- Compare to same query WITHOUT the 7 eager joins (bare `SELECT ee.ID, ee.SHORT_NAME … LIMIT 20`): 167 / 233 ms — **~200 ms mean**.
- Marginal cost of eager joins at LIMIT-20 listing: **~310 ms / 20 rows ≈ 15 ms/row of pure JOIN overhead**, on top of the EE fetch itself.
- EXPLAIN: every join is `eq_ref` via PRIMARY (the FKs are unique). No `Using temporary; Using filesort` from the joins; the `filesort` you see is the outer `ORDER BY ee.ID DESC` only.
- Verdict: **YELLOW**. Not catastrophic — but the eager joins do double wall time on the listing surface (167ms → 510ms). At single-EE scale they're free; at LIMIT-20 the multiplier shows up. At LIMIT-100 it would likely show 4-5x.
- Suggested fix direction: making `lastTroubledEvent` / `lastNeedsAttentionEvent` / `lastNoteUpdateEvent` LAZY on `CurationDetails` would defer 6 of the 7 LEFT JOINs (the 3 AE + 3 AET). The cost would be N+1 risk in callers that DO read the event data — but those are largely admin/curation surfaces (curationDetails endpoint, troubled-EE dashboard), not the bulk listing endpoint. Audit caller patterns first.

### Probe 3: AuditEvent.eventType eager-join — is it the canonical "fetch=join everywhere" tax?

- Mapping: `AuditEvent.hbm.xml` line 56-66 declares `eventType` as `fetch="join" lazy="false"`. Confirmed.
- Cardinality: `AUDIT_EVENT_TYPE` has **838,176 rows** — essentially one row per AuditEvent (it's a polymorphic discriminator table, joined via `EVENT_TYPE_FK` which is also indexed UNIQUE).
- Impact: every time Hibernate loads an `AuditEvent`, a join to `AUDIT_EVENT_TYPE` fires. For probe 2 above, this is 3 extra LEFT JOINs on top of the 3 AE joins. Each is `eq_ref` so the per-row cost is small, but the *count* of joins doubles. Each row in the LIMIT 20 page returns 7 joined sub-rows (CD + 3 AE + 3 AET).
- Timing isolated: building the same probe-2 query but DROPPING the 3 AET joins (keeping the 3 AE joins) would cut the row width but not the join count meaningfully; the dominant cost on the listing path is the per-row fanout of CD+AE.
- Verdict: **YELLOW**. The mapping is "always-eager" because `AuditEvent` is rarely interesting without its type, and at single-event scale (auditEvents endpoint) the join is free. The cost only stings when AuditEvents are themselves transitively eager-loaded by a parent that's listed in bulk — which is exactly what `CurationDetails → lastXEvent` does. Fix is upstream: make the CurationDetails associations lazy, NOT the AuditEvent.eventType association.

### Probe 4: `/datasets/{id}/samples` — bioassay list (collection init)

- DAO surface: `DatasetArgService.getSamples(DatasetArg)` → `thawLite(ee)` + `baService.loadValueObjects(ee.getBioAssays(), …)`. The `ee.getBioAssays()` collection init issues one SELECT to populate the assays for the EE; each BioAssay then has eager `ArrayDesign`, `originalPlatform`, `BioMaterial`, `accession` per `BioAssay.hbm.xml`.
- SQL (single batched form):
  ```sql
  SELECT ba.*, acc.*, ad.*, op.*, bm.* FROM BIO_ASSAY ba
  LEFT JOIN DATABASE_ENTRY acc ON acc.ID = ba.ACCESSION_FK
  LEFT JOIN ARRAY_DESIGN ad ON ad.ID = ba.ARRAY_DESIGN_USED_FK
  LEFT JOIN ARRAY_DESIGN op ON op.ID = ba.ORIGINAL_PLATFORM_FK
  LEFT JOIN BIO_MATERIAL bm ON bm.ID = ba.SAMPLE_USED_FK
  WHERE ba.EXPRESSION_EXPERIMENT_FK = 92401;
  ```
- Timing (3 runs, 92 assays): 167 / 170 / 166 ms — **~167 ms mean** (incl ~120ms connect).
- Verdict for the assay-list query itself: **GREEN**. The collection init + eager-per-assay joins resolve in <50ms server-side.
- The **real `/samples` cost** is per-assay BioMaterial chain thaw (next probe).

### Probe 5: BioMaterial sourceBioMaterial chain (per-assay thaw, the actual `/samples` bottleneck)

- DAO surface: `Thaws.thawBioMaterial(bm)` initialises the lazy `sourceBioMaterial` chain; per call it issues separate queries for `bm.characteristics`, `bm.factorValues`, `bm.treatments`, plus walks the source chain. Per `SAMPLES_DESIGN_PERF_RECCE.md`, `BioMaterialValueObject` ctor (BMV with `allFactorValuesAndCharacteristics=true`) reads through this chain.
- Probe: simulate the cost of three lazy initialisations per BM × N BMs for EE 92401 (92 BMs).
  - Worst case (3 queries × 20 BMs = 60 round-trips, sequential connect): **3,432 ms**.
  - Linearly projected to 92 BMs: **~15.8 s**.
  - Single batched query (1 connect, JOIN bm + chars + bm2fv across all 92 BMs): **150 ms**.
- Verdict: **RED** if Hibernate fires per-BM lazy inits without batching; **GREEN** when batched. The actual behaviour depends on Hibernate's `default_batch_fetch_size` and the BioMaterial mapping. The deferred recommendation in `SAMPLES_DESIGN_PERF_RECCE.md` (replace `service.thawLite(ee)` with the narrower `Thaws.thawBioAssay` loop) attacks the QT/publications side; **the BIGGER win is forcing all BM-chain inits into a single batched fetch**.
- Suggested fix direction: a dedicated `thawBioMaterialsForBioAssays(Collection<BioAssay>)` that issues one HQL with `left join fetch bm.characteristics left join fetch bm.allFactorValues left join fetch bm.sourceBioMaterial.characteristics …` and warms the entire chain in a single query. The 150ms-vs-15s gap is the absolute upper bound on this win.

### Probe 6: `/datasets/{id}/design` — ExperimentalDesign + FactorValues + Characteristics tree

- DAO surface: `ExpressionExperimentReadServiceImpl.getExperimentalDesignValueObject` (line 666) iterates `ed.experimentalFactors` and calls `Hibernate.initialize(fv.getCharacteristics())` per FV (and `fv.getMeasurement()`). Per `SAMPLES_DESIGN_PERF_RECCE.md`, this is the canonical "N+1 over factor-values" pattern.
- EE 57493: 7 factors, **175 factor values**, 83 characteristics under those FVs.
- Probe: simulate per-FV characteristic init.
  - True N+1 (one connect+query per FV, 30 FVs sampled): **4,138 ms**, average **137 ms/FV**.
  - Linearly projected to 175 FVs: **~24 s**.
  - Hibernate batch=10 (one connect per batch, 18 batches): **2,369 ms**.
  - Single query (all FVs of EE, one connect): **158 ms**.
- Verdict: **RED** in worst case. Even the `batch=10` middle path is **15x slower** than the single-query path. The deferred fix in `SAMPLES_DESIGN_PERF_RECCE.md` ("replace per-FV `Hibernate.initialize` loop with a single HQL fetch query") is correct and the win is bigger than the recce estimated.
- Suggested fix direction (verbatim from recce):
  ```hql
  select fv from FactorValue fv
  left join fetch fv.characteristics
  left join fetch fv.measurement
  where fv.experimentalFactor.experimentalDesign = :ed
  ```
  Then walk the in-memory list to build the VO. Single round-trip, ~150 ms vs current ~2-24 s.

### Probe 7: `/datasets/{id}/auditEvents` — direct AuditEvent listing for a single EE

- DAO surface: `AuditEventService.getEvents(ee)`. SQL is one join per the AuditTrail FK + the always-eager `eventType` join.
- SQL:
  ```sql
  SELECT ae.ID, ae.DATE, ae.ACTION, ae.NOTE, ae_t.ID, ae_t.class
  FROM INVESTIGATION ee
  JOIN AUDIT_TRAIL t ON t.ID = ee.AUDIT_TRAIL_FK
  JOIN AUDIT_EVENT  ae ON ae.AUDIT_TRAIL_FK = t.ID
  LEFT JOIN AUDIT_EVENT_TYPE ae_t ON ae_t.ID = ae.EVENT_TYPE_FK
  WHERE ee.ID = 92401
  ORDER BY ae.DATE DESC, ae.ID DESC;
  ```
- EXPLAIN: `ee.ID` PRIMARY const → `t` PRIMARY const → `ae` ref on `AUDIT_EVENT_AUDIT_TRAIL_FKC` (20 rows for this EE) → `ae_t` eq_ref PRIMARY. Filesort on `ORDER BY ae.DATE DESC` (small set, no impact).
- Timing (3 runs, 20 events for EE 92401): 138 / 128 / 127 ms — **~130 ms mean** (incl ~120ms connect).
- Verdict: **GREEN**. Pure connect-RTT-dominated. The eager `AUDIT_EVENT_TYPE` join is invisible here.

### Probe 8: N+1 platform lookups (`Session.get(ArrayDesign, …)` per EE)

- DAO surface: per `project_acl_exists_refactor.md` ("Reduce Session.get(ArrayDesign,…) N-per-EE platform lookups to a single batch"). The canonical antipattern: 20 sequential `Session.get(ArrayDesign, adId)` calls in a loop over a page of EEs.
- Probe: simulate 20 distinct ArrayDesign primary-key lookups, one sequential connect+query each, vs one `WHERE ID IN (…20…)` batch.
  - Per-EE Session.get × 20: **2,766 ms** (138 ms/lookup avg — pure RTT).
  - Batch IN(20): **121 ms** — single connect.
- Verdict: **RED** if the loop pattern exists in any served endpoint. The fix is the well-known "loop hoist into a batch fetch" — 23x speedup.
- Suggested fix direction: grep the codebase for `arrayDesignDao.load(` / `Session.get(ArrayDesign.class` within loop bodies; replace with `arrayDesignDao.load(Collection<Long>)` (a batch loader that does one IN-list query). The candidate hot loops are likely in `ExpressionExperimentDaoImpl.populateAnalysisInformation` / detail-VO post-processing and the per-EE `getPlatforms` paths.

### Probe 9: pipelineStatus per-EE — verify c64080aa4f's rewrite

- DAO surface: `DatasetsWebService.getDatasetPipelineStatus` (line 1398) — post-c64080aa4f, replaces `generateSummary(id)` with three targeted calls: `differentialExpressionAnalysisService.getExperimentsWithAnalysis({id}, true)`, `auditEventService.getLastEvents({ee}, ~14 types)`, `expressionExperimentBatchInformationService.checkHasBatchInfo(ee)`.
- Probe:
  - hasDea (`SELECT EXPERIMENT_ANALYZED_FK FROM ANALYSIS WHERE class IN (…) AND EXPERIMENT_ANALYZED_FK IN (?)`): **134-143 ms**.
  - getLastEvents for ~14 audit-event types via single audit-trail join (full type-filter scan): **143-151 ms**.
- Verdict: **GREEN**. The rewrite has the intended shape: 2-3 short, indexed queries, each one connect-bound. The old "30-join SELECT + getStats fanout" path is gone.

### Probe 10: AuditTrail.getEvents per-EE (explicit, not via getLastEvents)

- Same shape as probe 7, called from a different layer.
- Timing: 116 / 135 ms — **~125 ms mean**. Connect-bound.
- Verdict: **GREEN**.

### Probe (skipped): SearchService.indexAll

- Inspected `IndexerServiceImpl` — uses Hibernate Search 7's `MassIndexer` (batch=25, parallel object loaders). It doesn't issue one canonical "feed query"; it streams ids and batch-loads in worker threads. Out of scope for a single-SELECT probe.

## Compared to round 1

| Round-1 probe | Round-1 timing | Re-probed in round 2 | Round-2 timing | Delta |
|---|---|---|---|---|
| Probe 1: ACL JOIN (status quo) | 4.49 / 5.37 / 4.48 s | Re-ran with current optimiser state | 327 / 375 / 310 ms (`~340 ms`) | **~13x faster** but plan unchanged (still has `Using temporary; Using filesort`); the round-1 4.5s appears to have been cold-cache. The plan still drives `aoi_cls const → aoi ref (48,944 rows)` and the result-set deduplication still costs. |
| Probe 1b: ACL EXISTS form | 0.481 / 0.269 / 0.127 s (`~290 ms`) | Re-ran | 143 / 125 / 122 ms (`~130 ms`) | Stable. Still ~3x faster than the JOIN form on warm cache; the structural advantage (no `Using temporary; Using filesort`, single-row EXISTS short-circuit) is the durable win. |
| Probe 3a: getLastEvents batch=100 | 125 ms | (Not re-run; structurally unchanged.) | — | — |
| **Probe 1 vs 1b (warm-cache delta)** | 4.5s vs 0.29s — 15x | 340ms vs 130ms — **~3x** | | The relative ratio narrowed once both queries had warm buffer pool. The structural argument (EXISTS avoids `temporary;filesort` and the multi-row GROUP BY dedup) is still real; the absolute cold-cache 15x win remains the headline for first-page-of-the-day cold loads. |

### Has the ACL EXISTS rewrite landed visibly?

Yes — Session 2's rewrite landed in `c45c236fd2`, and the EXISTS form is now what `doLoadDetailsValueObjects` uses (see comment at line 2166-2168: "After the ACL EXISTS rewrite (Session 2), the filtering query projects only `ee` — ACL info … is post-fetched in transformListTyped via AclQueryUtils.loadAclInfoFor() and applied with ExpressionExperimentValueObject.populateAclInfo()"). The detail-listing query is now structurally what probe 1b measured: short, no `Using temporary; Using filesort` on the ACL portion.

## Top findings (ordered by impact)

1. **`/design` endpoint per-FV `Hibernate.initialize(fv.characteristics)` N+1 is the largest unfixed cost on the dataset-detail surface.** Probe 6: linearly projected **24 s worst-case** for a rich-design EE (175 FVs, true N+1), **2.4 s** with Hibernate batch=10, **150 ms** with a single fetch query. The fix is one HQL rewrite (`select fv from FactorValue fv left join fetch fv.characteristics left join fetch fv.measurement where fv.experimentalFactor.experimentalDesign = :ed`) and the win is up to 160x on the worst-case rich design. **Single biggest concrete fix surfaced by this round.** Already enumerated as deferred in `SAMPLES_DESIGN_PERF_RECCE.md`; the data here strengthens the case.

2. **`/samples` endpoint per-BM source-chain thaw N+1 is the second-largest unfixed cost.** Probe 5: **~15.8 s worst-case** projected for an EE with 92 BMs vs **150 ms** with a single batched HQL. The recommended fix is a dedicated `thawBioMaterialsForBioAssays(Collection<BioAssay>)` that warms the whole chain in one fetch. This is structurally identical to the design-N+1 fix (one HQL, fetch joins, in-memory walk). The `SAMPLES_DESIGN_PERF_RECCE.md` "thawLite-replacement" path attacks a different (smaller) component of the same endpoint.

3. **N+1 platform-lookup loops (`Session.get(ArrayDesign, …)` per EE) are RED wherever they occur.** Probe 8: **2,766 ms vs 121 ms** for 20 sequential PK lookups vs one batch IN. The fix is mechanical — hoist the loop into a batch loader. Need a focused grep audit to identify the callers (the round-1 memory note flagged this; this round confirms the per-lookup cost is purely RTT-bound at ~138 ms/lookup on the tunnel, so even small loops are expensive).

## Cross-cutting observations

- **CurationDetails eager-fetch — confirms the memory note's "bigger wins lie elsewhere" claim.** At single-EE scale (probe 1), the 7 eager joins (CD + 3 AE + 3 AET) are free (sub-millisecond, all `const` plans). At LIMIT-20 listing scale (probe 2), they roughly double wall time (167ms → 510ms), but that's still bounded — and the listing path now post-fetches ACL info separately, so the eager-fetch overhead is the only listing-amplified cost. **It is NOT the dominant cost on `/datasets/{id}` detail loads** — the detail endpoint goes through `loadValueObjectsWithCache` which calls `doLoadDetailsValueObjects` on a 1-id filter, where the eager joins are free. The bigger wins lie on `/samples` and `/design` per-EE detail surfaces (probes 5, 6). Making the lastXEvent associations LAZY would still be a clean win on LIMIT-20+ listing pages (cuts ~310ms on a 20-row page), but rank it #4-5 not #1.

- **AuditEvent.eventType eager-join — not a problem in isolation, but amplifies through CurationDetails.** Per probe 3 analysis: the join itself is `eq_ref PRIMARY` so per-row cost is negligible. The problem is structural — eager-AE + eager-AET means every CurationDetails load drags 6 LEFT JOINs even when the caller only wants `cd.troubled`. Fix is one level up (CurationDetails associations), not on AuditEvent itself.

- **Connect RTT (~120 ms) is the dominant single-query cost on the tunnel.** All "good" queries hit the floor of one connect = ~120 ms. The N+1 patterns (probes 5, 6, 8) are pathological precisely because they multiply the connect cost by N. The fix is always "one batched query" rather than "N batched-of-10 queries" — even `batch=10` (Hibernate's typical `default_batch_fetch_size`) is 15x slower than a single fetch on the design N+1 case.

- **Hibernate `default_batch_fetch_size` is not the same as "single fetch".** Probe 6 shows: batch=10 across 175 FVs = 18 round-trips = 2.4 s; single query = 1 round-trip = 150 ms. Increasing the batch size won't get you to the single-query performance — the only way is to write the HQL fetch-join explicitly. Worth noting in design docs around the design and samples endpoint fixes.

- **AUDIT_EVENT_TYPE has 838k rows — one per AuditEvent.** Verified at probe 3. The polymorphic discriminator-table pattern is at full saturation; there's no realistic "compress AET" win because the rows ARE the type-per-event facts. The fix has to be on the consuming side (don't load AEs you don't need), not on AET itself.

- **Round-1 probe-1 (4.5s ACL JOIN) was likely cold-cache.** Re-probed warm at 340ms. Two takeaways:
  1. The ACL-EXISTS rewrite IS validated — but the headline win is 15x on cold-cache, not the steady-state 3x.
  2. For first-page-of-the-day latency, ACL EXISTS still matters a lot; for sustained-load p50, it's a smaller wins (3x).
  3. The structural advantages (no `temporary;filesort`, no multi-row GROUP BY dedup) make the EXISTS form scale better as `INVESTIGATION` grows — which it will.

## Probes executed: 10 (1, 2, 3, 4, 5, 6, 7, 8, 9, 10) + round-1 re-validation. Search-indexing probe deferred (Hibernate Search MassIndexer is not a single-SQL surface).
