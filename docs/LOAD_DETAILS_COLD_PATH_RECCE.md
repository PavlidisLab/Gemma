# `loadDetailsValueObjectsByIds` cold-path anatomy

Baseline commit: `3de192d8f858ce3e2d64632866d8200238636981`.

Pure recce. Source paths are absolute-from-repo-root unless noted; line numbers
are valid at the baseline commit.

## 0. Call graph (top-down)

```
GET /rest/v2/datasets/{id}/pipelineStatus
  └─ DatasetsWebService.getDatasetPipelineStatus
       gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:1314
        └─ expressionExperimentReportService.retrieveSummaryObjects([id])
             gemma-core/.../report/ExpressionExperimentReportServiceImpl.java:340
              └─ cache miss → generateSummary(id)            (line 109)
                   ├─ expressionExperimentService.loadDetailsValueObjectsByIds([id])
                   │    └─ EE DAO loadDetailsValueObjectsByIds
                   │         gemma-core/.../experiment/ExpressionExperimentDaoImpl.java:2074
                   │          └─ doLoadDetailsValueObjects(filters, …)   (2105)
                   │              ├─ getFilteringQuery(filters, sort)    (3883)
                   │              ├─ result transformer (2157)
                   │              │    ├─ getExpressionExperimentDetailsById  (2009)
                   │              │    ├─ Session.get(ArrayDesign,...)        (2196)
                   │              │    │   for each arrayDesignUsed + each original platform
                   │              │    ├─ loadValueObjectsByIds(otherPartsIds) (2223)
                   │              │    └─ populateAnalysisInformation         (2231 → 4143)
                   │              │         └─ cached query (cheap once warm)
                   │              └─ getFilteringCountQuery (3924) — only when limit>0; SKIPPED
                   └─ getStats(vo)  (444)
                        └─ differentialExpressionAnalysisService.findByExperimentIds
```

The 10–14s cold cost is dominated by **`getFilteringQuery`** — i.e. the
~30-join `select ee, aoi, sid from ExpressionExperiment ee …` statement that
the user pasted into the brief.

---

## 1. Query anatomy — which JOIN comes from which mapping

The HQL `getFilteringQuery` (`ExpressionExperimentDaoImpl.java:3883-3897`):

```hql
select ee, aoi, sid from ExpressionExperiment as ee
left join fetch ee.accession acc                -- (A)
left join fetch ee.experimentalDesign as EDES   -- (B)
left join fetch ee.curationDetails as s         -- (C)
left join fetch s.lastNeedsAttentionEvent as eAttn   -- (C-1)
left join fetch eAttn.eventType                 -- (C-2)
left join fetch s.lastNoteUpdateEvent as eNote  -- (C-3)
left join fetch eNote.eventType                 -- (C-4)
left join fetch s.lastTroubledEvent as eTrbl    -- (C-5)
left join fetch eTrbl.eventType                 -- (C-6)
left join fetch ee.geeq as geeq                 -- (D)
, AclObjectIdentity as aoi                      -- (E) ACL
  join aoi.ownerSid sid                         -- (E-1)
  left join aoi.entries ace                     -- (E-2)
where aoi.identifier = ee.id and aoi.type = :aoiType …
group by ee.id
order by …
```

| Join | HQL alias | Source (.hbm.xml) | Why it's joined |
|------|-----------|-------------------|-----------------|
| (A)  | `acc`     | `Investigation.hbm.xml:102` (`accession`, fetch=select, lazy=proxy) | Forced via `left join fetch` in the query string |
| (B)  | `EDES`    | `Investigation.hbm.xml:107` (`experimentalDesign`, fetch=select, lazy=proxy) | Forced via `left join fetch` in query string |
| (C)  | `s`       | `Investigation.hbm.xml:128` (`curationDetails`, **fetch=join, lazy=false**) | Mapped eager-join. Would be joined even without the `left join fetch` |
| (C-1)| `eAttn`   | `CurationDetails.hbm.xml:20` (`lastNeedsAttentionEvent`, **fetch=join, lazy=false**) | Mapped eager-join |
| (C-2)| —         | `AuditEvent.hbm.xml` (`eventType`, **fetch=join, lazy=false**) | Mapped eager-join |
| (C-3)| `eNote`   | `CurationDetails.hbm.xml:24` (`lastNoteUpdateEvent`, **fetch=join, lazy=false**) | Mapped eager-join |
| (C-4)| —         | same as C-2 | Mapped eager-join |
| (C-5)| `eTrbl`   | `CurationDetails.hbm.xml:16` (`lastTroubledEvent`, **fetch=join, lazy=false**) | Mapped eager-join |
| (C-6)| —         | same as C-2 | Mapped eager-join |
| (D)  | `geeq`    | `Investigation.hbm.xml:91` (`geeq`, fetch=select, lazy=proxy) | Forced via `left join fetch` |
| (E)  | `aoi`     | (synthetic, cartesian-style "from + where") | ACL discrimination, `AclQueryUtils.formAclRestrictionClause` (line 127) |
| (E-1)| `sid`     | `aoi.ownerSid` | ACL owner check |
| (E-2)| `ace`    | `aoi.entries` | Non-admin: per-row ACL entries — **one-to-many ⇒ row-multiplication** |

Additional implicit joins on the SELECT side, populated by Hibernate after
the JOIN ROWS come back (NOT in the JOIN graph, but in the row materialization
cost):

- `ee.taxon` — `Investigation.hbm.xml:119` (`lazy=false, fetch=select`).
  Triggers an extra SELECT-per-fetch (per-EE, but in practice cached).
- `accession.externalDatabase` — `DatabaseEntry.hbm.xml`
  (`lazy=false, fetch=select`). Extra SELECT once `acc` is materialized.
- `ee.auditTrail` — `Investigation.hbm.xml:13` (`lazy=proxy`) — proxy only,
  not loaded yet, but the audit-trail bag (`AuditTrail.hbm.xml:25`,
  `lazy=false fetch=select`) WILL load if any code touches it later.

### What's NOT in the main query but the transformer still triggers

After the main query returns, `getDetailedValueObjectTransformer`
(line 2157) issues:

- **One extra HQL query** in `getExpressionExperimentDetailsById` (line 2009-2032)
  — a `LEFT JOIN ee.bioAssays ba LEFT JOIN ba.arrayDesignUsed LEFT JOIN
  ba.originalPlatform LEFT JOIN ee.otherParts GROUP BY ee, ad, op, oe`
  query. Cached but cold-miss still expensive.
- **N `Session.get(ArrayDesign, id)` round-trips** (line 2196 and 2207) — one
  per distinct arrayDesignUsed + one per distinct originalPlatform.
- **`loadValueObjectsByIds(otherPartsIds)`** (line 2223) — N+M extra loads for
  every "other part" of split experiments. Each of those, in turn, eagerly
  joins curationDetails+3 audit events+3 eventTypes per the mappings.
- **`populateAnalysisInformation`** (line 4143) — single cached query
  `select experimentAnalyzed.id from DifferentialExpressionAnalysis`.
  Cached, but a full-table scan on the cold path (one row per DEA in the DB).
- **`getStats(vo)`** in `ExpressionExperimentReportServiceImpl.java:444` —
  `differentialExpressionAnalysisService.findByExperimentIds([id], true, true)`
  — additional analyses query.

Total cold round-trips for a single `id`: 1 huge JOIN query + 1 details query
+ several `Session.get(ArrayDesign,…)` + 1 DEA-ids cache-warm query + 1
findByExperimentIds query + the proxy-fault SELECTs for `taxon`,
`externalDatabase`. The dominant wall-clock cost is the JOIN explosion in the
main query.

---

## 2. Bottleneck candidates, ranked

### Rank 1 — ACL one-to-many join (`acl_entry`)

`AclQueryUtils.formAclRestrictionClause` (line 127–157) emits
`, AclObjectIdentity as aoi join aoi.ownerSid sid left join aoi.entries ace`
plus a `where aoi.identifier = ee.id and aoi.type = :aoiType` clause. The
`aoi.entries` side is **one-to-many**, so each EE row multiplies by the
number of ACL entries. Add the `acl_class` subquery and the principal
sub-check (lines 144-153) and you have a cartesian-ish path the planner has
to think about. The forced `group by ee.id` (`groupByIfNecessary` collapsing
the dup rows) is correct but expensive on the join side. Comment on line 119
literally says "this ACL jointure is really annoying because it is one-to-many".

**How to verify:** run the same HQL with `SET SECURITY_CONTEXT_USER=admin`
(which short-circuits the ACL filter, line 136). Should cut the time
substantially. Or: capture the SQL plan against prod gemd via tunnel
(`EXPLAIN ANALYZE` on the rendered SQL) — the `acl_entry` join should be
the dominant cost row.

### Rank 2 — Six joins from CurationDetails+AuditEvent eager mappings

`CurationDetails.hbm.xml:16/20/24` map all three `last*Event` associations
with `fetch="join" lazy="false"`. `AuditEvent.eventType` is also `fetch="join"`.
That cascades into 6 LEFT JOINs (3 audit events × {AuditEvent table, AuditEventType
table}) every time an `ExpressionExperiment` row is materialized — even if
the caller doesn't need them. Each adds a join over `AUDIT_EVENT`
(non-tiny: every audit event Gemma has ever recorded) keyed by a nullable FK
on the `CurationDetails` row.

**How to verify:** rewrite `getFilteringQuery` without the three
`left join fetch s.last*Event` lines and add a manual
`fetch=select`-equivalent that triggers per-EE SELECTs only when needed.
Time the difference on a single-id query. Better yet: prove the planner is
spending time here by `SHOW PROFILE`/`EXPLAIN ANALYZE`.

### Rank 3 — `GROUP BY ee.id` over a 30-column row + `ORDER BY` with CASE

`groupByIfNecessary` (call site at line 3897) forces a `group by` because the
ACL `aoi.entries` left-join multiplies rows. With ~30 fetched columns and a
multi-table join graph, MySQL has to keep the entire join result in a temp
table to dedupe. Combined with the `ORDER BY` (with the NULL-handling CASE
when sort=null path is exercised by callers like
`loadDetailsValueObjectsByIdsWithCache` with a sort), this forces a temp
sort over the deduped rows. On a single-id query there's no sort coming in,
but `groupByIfNecessary` still emits `group by` (line 3897 passes
`groupByIfNecessary(sort, ONE_TO_MANY_ALIASES)`).

**How to verify:** for a single-id query, the GROUP BY is structurally
unnecessary if ACL deduplication is moved to a sub-query. The TODO at
AclQueryUtils.java line 95 already calls this out
(#784: "remove the need for a count distinct altogether by using a sub-query
to apply ACLs").

### Rank 4 — Cold cache for `getExpressionExperimentDetailsById` and DEA-ids

The post-query transformer (line 2183) issues an additional HQL query that
joins `ee → bioAssays → arrayDesignUsed → originalPlatform → otherParts`.
This is cached (line 2027 sets `FILTERED_VO_CACHE_REGION`), so on the second
call it's free — but the first cold call has to materialize all bioAssay
fan-out for the EE. For a real dataset that's hundreds of bioAssays.
Then `populateAnalysisInformation` (line 4143) issues
`select experimentAnalyzed.id from DifferentialExpressionAnalysis`
**unfiltered** (line 4159) — a full scan of `DIFFERENTIAL_EXPRESSION_ANALYSIS`
on cold. With however many DEAs Gemma has, that's not free.

**How to verify:** Time the StopWatch the code already prints
(`details: %d ms` in the warn log at line 2145). The current production-tunnel
trace would already show those broken out; check the logs.

### Rank 5 — N+1 `Session.get(ArrayDesign,…)`

Lines 2196 + 2207: one Session.get per distinct ArrayDesign id, executed
serially. For an EE that uses 3 platforms with switched-from origins, that's
~6 individual SELECTs (mostly L2-cached after warm, but not guaranteed on
cold). On a single id this is small (single-digit roundtrips), but on a list
of N EEs it's quadratic-feeling.

---

## 3. Cheap removals (what `loadValueObjectsWithCache` ≈ `loadValueObjectsByIds` does NOT do)

The 64ms warm-cold path is `loadValueObjectsByIds` from `AbstractVoEnabledDao.loadValueObjectsByIds`
(line 94): it just does `loadValueObjects(load(ids))`. That calls
`AbstractFilteringVoEnabledDao.load(ids)` which is a plain `from ExpressionExperiment where id in (:ids)`
with the EAGER associations the entity mapping forces — i.e. (A)(B)(C)(C-1..6)(D)
from the table above STILL happen (they're mapped eager), but **without**:

- **(E) the ACL join** — not added, because `load` doesn't go through
  `finishFilteringQuery`.
- **`group by ee.id`** — not needed, because no ACL multiplication.
- **`order by` with NULL-handling CASE** — no sort.
- **`getExpressionExperimentDetailsById` second query** — `doLoadValueObject`
  for the plain VO doesn't fan out into bioAssays/arrayDesigns/otherParts.
- **`Session.get(ArrayDesign,…)` round-trips** — same as above.
- **`populateAnalysisInformation`** — only `loadDetailsValueObjects*` calls it.
- **`getStats(vo)`** — only `generateSummary` does.

So the gap between 64ms (warm-DAO-cache list lookup) and 10-14s (cold
`loadDetailsValueObjectsByIds`) is, in priority order: ACL one-to-many join,
the per-row group-by/order-by, and the post-query fan-out queries
(`getExpressionExperimentDetailsById`, `populateAnalysisInformation`, `getStats`).

**The audit-event/eventType joins (C-1..6) are NOT what makes the difference**
between fast and slow — they're already in the fast path (the mappings force
them). The differentiator is the **ACL join + group by**.

---

## 4. Proposed three-tier API

Based on what the `pipelineStatus` endpoint actually consumes from the VO:
`bioMaterialCount`, `processedExpressionVectorCount`, `hasCoexpressionAnalysis`,
`hasDifferentialExpressionAnalysis`, `troubled`, `troubleDetails`, `geeq`,
`differentialExpressionAnalyses`, `lastUpdated`, `dateCached`.

It does NOT consume from the VO: `arrayDesigns`, `originalPlatforms`,
`otherParts`, `numberOfBioAssays`, the audit-event-derived dates on the
detailed VO (it builds its own `auditEventService.getLastEvents` batch
call at line 1336-1337 anyway).

So `/pipelineStatus` is paying for `getExpressionExperimentDetailsById` +
all the `Session.get(ArrayDesign,…)` + `loadValueObjectsByIds(otherPartsIds)`
fan-out that it then throws away.

### Tier A: `loadShallowValueObjectsByIds(ids)` — base VO only

- Single SELECT, ACL still applied (security boundary), but **no LEFT JOIN
  FETCH** of geeq / experimentalDesign / accession beyond what the entity
  mapping forces.
- No second-query for bioAssays/ArrayDesigns/otherParts.
- No DEA-ids full-scan.
- Returns `ExpressionExperimentValueObject` (not the Details subclass).
- Target: <200ms cold for a single id.

### Tier B: `loadCuratedValueObjectsByIds(ids)`

- Tier A + curation summary (troubled flag, lastUpdated, needsAttention,
  curationNote-when-admin). These are already on the `CurationDetails` row
  the entity mapping joins eagerly, so adding them costs zero extra joins.
- Optionally + `geeq` (which is one extra `left join` — currently always done).
- No second-query fan-out.
- No audit-event LEFT JOINs in the main query — if a caller wants the
  last-of-type event dates, route them through `auditEventService.getLastEvents`
  (a separate batched query), exactly what `pipelineStatus` already does.
- Returns a `ExpressionExperimentCuratedValueObject` or a `Details` with the
  bioAssay/platform/otherParts/dateLinkAnalysis/etc fields all null.
- Target: ~300ms cold for a single id.

### Tier C: `loadDetailsValueObjectsByIds(ids)` — current behaviour

- Kept as-is for backward compat.
- Migrate callers as they get touched. The `pipelineStatus` endpoint is the
  obvious first migration target — it can move to **Tier B** today with no
  functional loss because it doesn't read the bioAssay/platform/otherParts
  fields of the detailed VO.

### Implementation sketch

- Tier A: new method on `ExpressionExperimentDaoImpl` that calls a
  trimmed-down `getFilteringQuery` (no `left join fetch` of geeq, no
  detail-transformer post-processing). Goes via `finishFilteringQuery` so
  ACL is still applied.
- Tier B: same as Tier A but keeps `left join fetch ee.geeq`. Also skips
  `getExpressionExperimentDetailsById` and `populateAnalysisInformation`.
  Possibly skips the `group by` if ACL is rewritten to a sub-query (#784).

---

## 5. The single biggest win

**Move the ACL filter from a `from … , AclObjectIdentity` cartesian join to a
correlated EXISTS-subquery** at `AclQueryUtils.formAclRestrictionClause`
(`gemma-core/src/main/java/ubic/gemma/persistence/util/AclQueryUtils.java:127-157`),
and drop the resulting unnecessary `group by ee.id` from the EE-DAO call sites
(`ExpressionExperimentDaoImpl.java:3897, 3920, 3934`).

There is an explicit FIXME at AclQueryUtils.java line 95 and line 119 calling
this out. Issue #784 tracks it. The current implementation forces:

1. one-to-many row multiplication on the ACL join → forces GROUP BY
2. GROUP BY over 30+ columns → forces temp table
3. ORDER BY → forces temp sort over the temp table

A sub-query rewrite collapses (1) into a single-row-per-EE filter; eliminating
(1) eliminates (2) and (3) for the single-id path. That's the path from
~10s to (estimated) ~300ms cold without touching any mapping.

Second-biggest win (if the sub-query rewrite is too risky for one batch):
**call `pipelineStatus` through a new Tier B method that skips
`getExpressionExperimentDetailsById` + `populateAnalysisInformation` +
`Session.get(ArrayDesign,...)` fan-out**. That alone cuts the
`/pipelineStatus` endpoint from `~10s` to whatever the main JOIN+ACL takes
in isolation (estimate: 2-4s cold, dominated by the ACL join).

---

## 6. Risks of dropping each LEFT JOIN

| Join | Risk if removed |
|------|------------------|
| `ee.accession` (A) | EE VO uses `accession.accession` (the shortname) and `externalDatabase.name` in many display paths. Removing breaks accession display. |
| `ee.experimentalDesign` (B) | Used for batch-info detection, factor-value display in some VOs. Safer to keep. |
| `ee.curationDetails` (C) | **Cannot remove** — mapped `lazy=false`, plus `troubled` flag drives non-admin filtering (`addNonTroubledFilter` line 3945). |
| `s.last*Event` (C-1, C-3, C-5) | These populate `dateLastUpdated`, `troubleDetails`, etc. on `ExpressionExperimentDetailsValueObject`. Removing makes those fields null on the VO — fine for `/pipelineStatus` (which doesn't read them off the VO) but breaks any UI that reads `vo.getDateLastNeedsAttention()` etc. **Need callsite audit.** |
| `*.eventType` (C-2, C-4, C-6) | Same risk as parent — these are AuditEventType subclasses used for "is this an OK / Failed event" display. |
| `ee.geeq` (D) | `ExpressionExperimentDetailsValueObject.setGeeq(geeq)` populates the `geeq` field. Used by `/pipelineStatus` (line 1360). Cannot drop for that endpoint. Could LAZY-fetch instead of eager. |
| `aoi`/`sid`/`ace` (E) | **Cannot drop the security check.** Can replace it with a correlated EXISTS sub-query (the rank-1 win above). |
| `getExpressionExperimentDetailsById` second query (post-transformer) | Removing means `arrayDesigns`, `originalPlatforms`, `otherParts`, `numberOfBioAssays` are unset on the VO. Many UI callsites read these. Need a tiered API so the cheap caller doesn't pay for it. |
| `populateAnalysisInformation` | Removing means `hasDifferentialExpressionAnalysis` and `hasCoexpressionAnalysis` are unset. `pipelineStatus` reads these (line 1350-1351). Need either: keep it (it's cached), or move the flag-population into a dedicated lightweight query. |
| `getStats(vo)` | Removing means `differentialExpressionAnalyses` collection is unset. `pipelineStatus` does NOT read this (it uses its own `auditEventService.getLastEvents` for DEA timing). Drop-able for that endpoint. |

---

## Appendix: cross-references

- `loadDetailsValueObjectsByIds`: ExpressionExperimentDaoImpl.java:2074
- `doLoadDetailsValueObjects`: ExpressionExperimentDaoImpl.java:2105
- `getFilteringQuery`: ExpressionExperimentDaoImpl.java:3883
- `getFilteringIdQuery`: ExpressionExperimentDaoImpl.java:3910
- `getFilteringCountQuery`: ExpressionExperimentDaoImpl.java:3924
- `finishFilteringQuery`: ExpressionExperimentDaoImpl.java:3937
- `getExpressionExperimentDetailsById`: ExpressionExperimentDaoImpl.java:2009
- `getDetailedValueObjectTransformer`: ExpressionExperimentDaoImpl.java:2157
- `populateAnalysisInformation`: ExpressionExperimentDaoImpl.java:4143
- `loadWithRelationsAndCache`: ExpressionExperimentDaoImpl.java:2036
- `AbstractVoEnabledDao.loadValueObjectsByIds`: line 94
- `AclQueryUtils.formAclRestrictionClause`: AclQueryUtils.java:127
- `AclQueryUtils.requiresGroupBy`: AclQueryUtils.java:97
- `ExpressionExperimentReportServiceImpl.generateSummary`: line 109
- `ExpressionExperimentReportServiceImpl.retrieveSummaryObjects`: line 340
- `ExpressionExperimentReportServiceImpl.getStats`: line 444
- `DatasetsWebService.getDatasetPipelineStatus`: line 1314
- HBM: `Investigation.hbm.xml` lines 91 (geeq), 102 (accession), 107 (expDesign), 119 (taxon), 128 (curationDetails fetch=join)
- HBM: `CurationDetails.hbm.xml` lines 16, 20, 24 (3x audit events, all fetch=join)
- HBM: `AuditEvent.hbm.xml` — `eventType` fetch=join
- HBM: `Geeq.hbm.xml` — properties only, no further joins
- HBM: `DatabaseEntry.hbm.xml` — `externalDatabase` fetch=select, lazy=false (extra SELECT after `acc` materializes)
- Open issue: PavlidisLab/Gemma#784 (ACL sub-query rewrite)
