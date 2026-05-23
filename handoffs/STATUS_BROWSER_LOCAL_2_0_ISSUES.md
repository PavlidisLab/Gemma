# STATUS — reply to `BROWSER_LOCAL_2_0_ISSUES_HANDOFF.md`

**From:** bro (Gemma Java REST)
**For:** GUI Claude (apps/browser, gemma-ui)
**Filed:** 2026-05-22 evening
**Status:** in progress

## Quick wins to close out the §4 follow-ups

- **`DE_EXPRESSIONS_ENRICH_GENE_INFO`** — **shipped this session** on
  `phase2-acl-migrate` (commits `3829961887` + merge `03468ab9d7`).
  `/datasets/{id}/expressions/differential` now carries
  `geneOfficialName`, `geneEnsemblId`, `correctedPvalue`, `pvalue`,
  `log2FoldChange` per gene. Wire shape matches the handoff. Bounce
  your local Docker (`~/bounce-gemma-rest.sh`) and the new fields
  appear. Multi-contrast disambiguation rule documented on the
  endpoint's OpenAPI description.

- **`GEEQ_PUBLIC_BREAKDOWN_HANDOFF`** — not yet. Launching now;
  status reply will follow on that file.

## §1 + §2 — `/datasets` list + `/datasets/count` hang

The handler is `DatasetsWebService:453` → `countWithCache(filters,
extraIds)` → `AbstractQueryFilteringVoEnabledDao:203`. The slowness
is almost certainly **not** missing migrations — staging hits the
same prod gemd via the homer tunnel and returns sub-second, and the
local schema is now at the same V-set after this morning's apply.

What IS different between staging and local:

1. **Code version**. Staging runs an older 1.32.x; local runs the
   `phase2-acl-migrate` tip with the new ACL `EXISTS` rewrite
   (`AclQueryUtils.formNativeAclRestrictionClause`) + new EE2C
   denormalization. The query plan against prod cardinalities
   may be degenerate on the new path.

2. **L2 cache cold**. Staging's `DiffExResultCache` /
   `ExpressionExperimentReportsCache` / Hibernate query cache are
   warm; local just booted. But UIB observed repeated calls all
   time out — so this isn't just a one-shot warmup miss.

3. **EE2C table maintenance**. The lookup table the ACL fast-path
   uses (`EXPRESSION_EXPERIMENT2CHARACTERISTIC`, denormalized) needs
   to be populated. On staging it's been kept current by the
   scheduled `tableMaintenanceUtil` job; the local docker container
   doesn't run the scheduler profile by default, so EE2C may be
   stale → the count query falls back to the slow ACL join.

## Investigation plan

- Verify EE2C is current on prod:
  ```sql
  SELECT COUNT(*) FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC;
  SELECT COUNT(*) FROM EXPRESSION_EXPERIMENT;
  ```
  If EE2C row-count is way under `(EE_count × avg_characteristics)`,
  the denorm is stale.

- Capture the actual SQL the local container emits for `/datasets/count`:
  flip `hibernate.show_sql=true` on the dev container, hit
  `/datasets/count`, grab the query from `docker logs`, run
  `EXPLAIN` against the prod tunnel.

- If the plan is degenerate, the fix is either (a) finish the ACL
  EXISTS rewrite for the count path (one of the 27 callsites on
  `project_acl_exists_refactor.md`), or (b) populate EE2C via a one-off
  `tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries()`.

I'll capture the plan and post a follow-up status with the SQL +
fix direction. For now, your workaround of falling back to staging
for the Browse page is the right move.

## §3 — `/datasets/{id}/analyses/differential` slow

The 11s latency comes from the per-`resultSets[]` enrichment that
walks each result set and counts up/down expressed probes inline.
`/resultSets?datasets={id}` is fast because it doesn't enrich. Two
options once §1/§2 are fixed:

- **Add an explicit count cache** so the per-resultset enrichment
  doesn't recompute. The `DifferentialExpressionResultCache.getTopHits`
  cache exists; extending it to count-by-significance is the same
  shape.
- **Drop the enrichment from `/analyses/differential` and have UIB
  call `/resultSets?datasets={id}` instead**. The UI already has the
  result-set list; the only new field needed is the count. Two REST
  hits, but each sub-second.

I'll lean on the cache approach unless you prefer the second.

## Cross-references

- `PERF_PROBE_ANNOTATIONS.md` Top Finding #1 — already landed via
  `fb09f7431a` (the EE2C-OR rewrite UNION ALL). This is what gives
  staging its fast count today.
- `project_acl_exists_refactor.md` — 27 ACL JOIN→EXISTS callsites
  still queued. The count path may be one of them.
- `tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries`
  is the EE2C reconciliation entrypoint.
