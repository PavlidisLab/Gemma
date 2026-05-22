# GENE_PAGE_MONITORING_RECCE

**Filed-by:** agent-gene-endpoints (Claude Code) — 2026-05-21
**Baseline:** worktree `feat-gene-page-endpoints` off phase2 tip `05d48bbff3`.

Inventory of gene-page and system-monitoring endpoints that
gemma-curation-ui needs to fully replace gemma-web. Pairs with the
existing recce `GENE_PAGE_REWORK_RECCE.md` (read-only deep-dive) and
the `GEMMA_UI_ENDPOINT_GAP.md` summary.

---

## 1. Scope and method

Surface inspected:

- `gemma-web/src/main/webapp/scripts/api/entities/gene/` — legacy
  Ext.js gene page (`GenePage.js`, `GeneDetailsTab.js`,
  `GeneElementsPanel.js`).
- `gemma-web/src/main/java/ubic/gemma/web/controller/genome/gene/` —
  legacy controllers (`GeneController.java`, `GeneSetController.java`,
  `GenePickerController.java`).
- `gemma-rest/src/main/java/ubic/gemma/rest/GeneWebService.java` —
  current REST surface for genes.
- `gemma-web/src/main/webapp/scripts/app/monitoring.js` and
  `gemma-web/src/main/java/ubic/gemma/web/controller/monitoring/` —
  legacy admin/cache/Hibernate monitoring.
- `gemma-rest/src/main/java/ubic/gemma/rest/monitoring/` — current
  REST monitoring (Prometheus + health indicators only).

What is in scope: the BACKING DATA the gemma-curation-ui SPA needs
to render the gene page and admin panel. JSP / Spring MVC controllers
are explicitly out of scope (gemma-web is retiring).

---

## 2. Gene page — endpoint inventory

### 2.1 Already in REST (no work)

| route                                      | status   |
|--------------------------------------------|----------|
| `GET /genes/{gene}`                        | exists   |
| `GET /genes/{gene}/locations`              | exists   |
| `GET /genes/{gene}/goTerms`                | exists   |
| `GET /genes/{gene}/probes`                 | exists (paginated list of `CompositeSequenceValueObject`) |
| `GET /platforms/{p}/elements/{probe}/genes`| exists   |

### 2.2 Must port — implemented in this batch

| route                                                   | backing service call                                              | replaces (legacy DWR)                                       |
|---------------------------------------------------------|-------------------------------------------------------------------|-------------------------------------------------------------|
| `GET /genes/{gene}/overview`                            | `GeneService.loadFullyPopulatedValueObject(id)` + `findGOTerms(id).size()` | `GeneController.loadGeneDetails(id)` (Ext Overview tab)     |
| `GET /genes/{gene}/homologues`                          | `loadFullyPopulatedValueObject` then `.getHomologues()` (cheaper to compute alongside overview but worth exposing separately for incremental UI fetch) | covered by `loadGeneDetails` in legacy; no standalone DWR  |
| `GET /platforms/{platform}/elements/{probe}/mappingSummary` | `CompositeSequenceService.loadValueObjectWithGeneMappingSummary(cs)` — the `geneMappingSummaries` field of the returned VO carries BLAT alignments + sequence metadata + supported genes | `CompositeSequenceController.getGeneMappingSummary(cs)` (Ext Elements drill-down) |

`loadFullyPopulatedValueObject` is on `GeneService` already and powers
the legacy `loadGeneDetails` DWR — it populates `aliases`, `homologues`,
`geneSets`, `multifunctionalityRank`, `compositeSequenceCount`,
`platformCount`. The new `/overview` endpoint adds the GO-term count
to match the legacy shape exactly.

### 2.3 Deferred to orchestrator — leave a TODO

| route                                              | why not here                                                                 |
|----------------------------------------------------|------------------------------------------------------------------------------|
| `GET /genes/{gene}/differentialExpression`         | Hot-path candidate — `findByGene` measured at 4 s cold for TP53 (PERF_PROBE_REPORT_ROUND3 §C1). Adding this endpoint without the cold-cache mitigation (denorm table OR startup warm-up) would expose the worst-case latency to the new UI on day one. Park until Paul decides between warm-up vs denorm. |
| Enriched `GET /genes/{gene}/probes?summary=true`   | Legacy `getGeneCsSummaries` returns `CompositeSequenceMapValueObject` (per-row gene-list aggregation + numBlatHits). The service-layer call exists (`getCompositeSequencesById` → `compositeSequenceService.getRawSummary(Collection<CompositeSequence>)`) but returns `Collection<Object[]>` — there is no clean DTO. Designing the response DTO and wiring the SQL aggregation through is a non-trivial shape decision. Defer.                                                       |

### 2.4 Don't port — legacy bloat / retired features

| feature                              | why not                                                                                   |
|--------------------------------------|-------------------------------------------------------------------------------------------|
| Coexpression tab + node-degree sparkline | `COEXPRESSION_ORPHAN_RECCE.md` confirmed the gene-gene coex tables are orphaned (~146 GB on prod, retirement migration drafted). Overview field `nodeDegreesPos/Neg/Ranks` is dead-on-arrival. |
| Phenocarta / PhenotypeEvidence tab   | Commented out in legacy JS; system functionally retired.                                  |
| Allen Brain Atlas image strip        | Commented out in legacy JS; only consumer was the dormant `Gemma.GeneAllenBrainAtlasImages` xtype. |
| `GET /genes/{gene}/products`         | `GeneController.getProducts` exists on the DWR side but has no caller in any active JS. Skip. |
| Legacy `showGene.html` JSP           | Out of scope. gemma-web is retiring; the SPA renders directly from REST.                  |

---

## 3. System monitoring — endpoint inventory

### 3.1 Already in REST (no work)

| route                            | status                                                              |
|----------------------------------|---------------------------------------------------------------------|
| `GET /metrics`                   | Prometheus scrape (token-gated, see `MetricsWebService`)            |
| Health / liveness indicators     | `CacheHealthIndicator`, `DbHealthIndicator`, `DiskSpaceHealthIndicator` exist but are wired through the actuator surface (`HealthWebService`), not exposed individually for admin UI |

### 3.2 Must port — implemented in this batch

| route                                  | backing service call                                  | replaces (legacy DWR / page)                |
|----------------------------------------|-------------------------------------------------------|---------------------------------------------|
| `GET /admin/caches`                    | `CacheManager.getCacheNames()`                        | `SystemMonitorController.getCacheStatus()`  |
| `DELETE /admin/caches`                 | `CacheMonitor.clearAllCaches()`                       | `SystemMonitorController.clearAllCaches()`  |
| `DELETE /admin/caches/{cacheName}`     | `CacheMonitor.clearCache(name)`                       | `SystemMonitorController.clearCache(name)`  |
| `GET /admin/hibernate/stats`           | `HibernateMonitor.getStats(false,false,false)` (plain-text response, same as the legacy `getHibernateStatus` DWR) | `SystemMonitorController.getHibernateStatus()` |

All four are admin-gated (`@PreAuthorize("hasAuthority('GROUP_ADMIN')")`).

### 3.3 Deferred to orchestrator

| route                                  | why not here                                                                  |
|----------------------------------------|-------------------------------------------------------------------------------|
| `POST /admin/hibernate/reset`          | The legacy DWR (`resetHibernateStatus`) exists and is trivial to wire, but the new UI's admin-panel design is unsettled — uncertain whether the SPA wants a reset-button at all (Hibernate stats are a developer tool, not a curator tool). Defer pending UX decision. |
| `POST /admin/caches/stats-enable` / `…disable` | The current `CacheMonitorImpl` stubs `enableStatistics`/`disableStatistics` to no-ops (post-EhCache-2 era); no real behavior to surface. Skip until / unless the cache-stats subsystem is reinstated. |
| `GET /admin/jobs` (queued / running tasks) | Existing `TasksWebService` covers single-task status; aggregated admin view of the queue is not currently exposed. Backing service `gemma-core/.../background/JobRunningEvent` machinery is more complex — defer. |
| `GET /admin/search/indices`            | Search-index sizes / status. No clean service-level accessor today; the legacy `indexer.js` flow calls the Hibernate Search indexer directly. Non-trivial to surface cleanly. Defer.                                                |

### 3.4 Don't port

| feature                              | why not                                                                                   |
|--------------------------------------|-------------------------------------------------------------------------------------------|
| Per-cache statistics HTML            | `CacheMonitorImpl.getStats()` returns an HTML blob from the pre-EhCache-2 era stubbed to a cache-names list. Don't pipe HTML through REST — the new endpoint returns JSON cache-name list instead. |
| `/admin/systemStats.html` JSP        | Out of scope (gemma-web JSP).                                                             |

---

## 4. Summary

Implemented in this batch (6 endpoints):

1. `GET /genes/{gene}/overview` — `GeneWebService`
2. `GET /genes/{gene}/homologues` — `GeneWebService`
3. `GET /platforms/{platform}/elements/{probe}/mappingSummary` — `PlatformsWebService`
4. `GET /admin/caches` — new `AdminWebService`
5. `DELETE /admin/caches` and `DELETE /admin/caches/{cacheName}` — `AdminWebService`
6. `GET /admin/hibernate/stats` — `AdminWebService`

Deferred:

- `GET /genes/{gene}/differentialExpression` — needs cold-cache mitigation first.
- `GET /genes/{gene}/probes?summary=true` — response DTO design needed.
- `POST /admin/hibernate/reset` — UX decision pending.
- `GET /admin/jobs`, `GET /admin/search/indices` — non-trivial backing.
