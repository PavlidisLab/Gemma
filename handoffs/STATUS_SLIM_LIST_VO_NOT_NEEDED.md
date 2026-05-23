# STATUS: slim list VO — not needed today

Audit of residual #2 from `handoffs/STATUS_LOAD_DETAILS_COLD_PATH_AUDIT.md` against
tip `cc68d92abd` (Phase 2 `phase2-acl-migrate`, 2026-05-23).

## Verdict

**No slim VO. Stay on `ExpressionExperimentValueObject`.** The two endpoints the
audit doc nominated as slim-VO consumers (`/datasets/count`, `/datasets/platforms`)
**don't load EE VOs today** — they call frequency/count services directly. The one
endpoint that explicitly metered slow on the fat-VO path (`/datasets/{id}/pipelineStatus`)
was rewritten in-place to bypass `retrieveSummaryObjects` (audit row 2). Cold-path
cost on the remaining multi-EE callers is acceptable per the static receipts below.
Re-open only when production telemetry surfaces a slow multi-EE endpoint that still
goes through `loadValueObjectsByIds`.

## Receipts

### What the nominated endpoints actually do

| Endpoint | File:line | Load path | Multi-EE VO? |
|---|---|---|---|
| `GET /datasets/count` | `DatasetsWebService.java:461` | `expressionExperimentService.countWithCache(filters, extraIds)` | **No** — returns a `long` |
| `GET /datasets/platforms` | `DatasetsWebService.java:492` | `getTechnologyTypeUsageFrequency(...)`, `getArrayDesignUsedOrOriginalPlatformUsageFrequency(...)`, `arrayDesignService.loadValueObjects(adKeys)` | **No** — returns ArrayDesign VOs, not EE VOs |
| `GET /datasets/{id}/pipelineStatus` | `DatasetsWebService.java:1741` | Rewritten to use `auditEventService.getLastEvents` + `differentialExpressionAnalysisService.getExperimentsWithAnalysis` directly | **No** (previously yes, fixed pre-tip) |

The audit doc was carrying a stale assumption from `LOAD_DETAILS_COLD_PATH_RECCE.md`
(2026-05-20) — the count/platforms endpoints have never used the fat VO on this branch.

### Multi-EE VO callers that remain

| Endpoint | File:line | Method | Notes |
|---|---|---|---|
| `GET /datasets` (browse with `query`) | `DatasetsWebService.java:320` | `loadValueObjectsByIdsWithRelationsAndCache` | Full browse — needs the fat shape (UI renders every field). |
| `GET /datasets/search` (typeahead) | `DatasetsWebService.java:411` | `loadValueObjectsByIds(ids, true)` | Projects 5 fields out of the fat VO into the slim `DatasetSearchHitValueObject` (id, shortName, name, accession, taxon). **Only place** where a slim DAO method would shave real cost; brief excluded wholesale migration. |
| `GET /annotations/.../datasets` | `AnnotationsWebService.java:389` | `loadValueObjectsByIds(foundIds)` | Fat shape used by the consumer. |

`/datasets/search` is the one borderline case. The fat-VO cost per row at tip is:
- `accession` (1 SELECT, lazy=proxy, hit once for `externalDatabase.name` + URI)
- `taxon` (1 SELECT, lazy=false but L2-cached)
- `geeq` (1 SELECT, lazy=proxy)
- `experimentalDesign` id projection (1 SELECT for proxy id read)
- `characteristics` (only if already initialized; not forced)

Plus the still-eager `left join fetch s.lastXEvent / eventType` in
`getFilteringQuery` (`ExpressionExperimentDaoImpl.java:4072-4077`). Those are
mapped `lazy="proxy" fetch="select"` in `CurationDetails.hbm.xml` since
`b57d679e8e`, but `loadValueObjectsByIds` paths still call `getFilteringQuery`
which forces them via the HQL `left join fetch`. **That's residual #1** —
killing those `left join fetch` lines would shrink every EE materialization
without a new VO surface. The audit doc already recommends picking that up
next; it dominates the slim-VO opportunity.

### Cold-path components at tip

| Component | Status | Source |
|---|---|---|
| ACL one-to-many join + GROUP BY | Closed — EXISTS sub-query | `06fa730cd8`, `8aaf40d816`, `cefed7588b` |
| `CurationDetails.lastXEvent` mapping eager | Closed — lazy proxy | `b57d679e8e` (`CurationDetails.hbm.xml`) |
| `aoi.objectIdClass` ACL filter | Closed — indexed | `26c7e0d620` |
| Batched `ArrayDesign` fetch | Closed — `loadAsMap(ids)` | `8f8bd3747b` |
| `getFilteringQuery` `left join fetch` of `lastXEvent` | **Open (residual #1)** | `ExpressionExperimentDaoImpl.java:4072-4077` |
| Slim list VO | **Closed (this doc)** — no callers in pain | — |

## When to re-open

Re-open if any of the following surface in prod telemetry:

1. A multi-EE endpoint that goes through `loadValueObjectsByIds` /
   `loadValueObjectsByIdsWithRelationsAndCache` and meters > 1s cold for a
   reasonable batch (say 50-100 EEs).
2. The curation-UI starts paging through the fat browse VO at scroll-rate
   (today it doesn't — it uses the `/search` typeahead and the paginated
   browse separately).
3. Residual #1 lands and post-landing measurement still shows headroom worth
   chasing on the typeahead path.

In any of those cases, the right shape is still
`ExpressionExperimentListValueObject` projecting (`id`, `shortName`, `name`,
`accession`, `taxon`, ACL flags, `numberOfBioAssays`) with no `geeq`, no
`characteristics`, no `experimentalDesign` id projection, and a
`loadShallow(Collection<Long>)` DAO method that skips the
`getFilteringQuery` join graph entirely (custom HQL).

## Tip / baseline

- Audit baseline: `cf09999f85` (per audit doc)
- This recce tip: `cc68d92abd`
- Worktree: `.claude/worktrees/agent-slim-vo-1779529953`
- Branch: `agent-slim-list-vo`
