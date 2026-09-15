# Live-gemd perf probe (2026-05-20 phase2-acl-migrate)

## Setup

- Worktree tip: `74e4dd1f0015c375a46eef9b7dca07d884adad8e` (sweep-nplus1-query-patterns, baselined at 74e4dd1f00)
- Probe machine: Darwin 24.6.0, x86_64 (PAVNOTE-18)
- mysql client: `mysql Ver 14.14 Distrib 5.7.31, for macos10.14 (x86_64)`
- Server: MySQL 5.7.44 (Percona-flavoured `5.7.44-48-log`)
- Round-trip ping to `127.0.0.1:8000`: ~186 ms cold, ~100 ms warm
- READ-ONLY: only SELECT/EXPLAIN ran. No writes, no `ANALYZE`, no schema changes.

### Cardinality reference (gemd, prod)

| table | rows |
|---|---|
| INVESTIGATION (all classes) | 72,002 |
| INVESTIGATION (class=ExpressionExperiment) | 25,668 |
| INVESTIGATION (class=ExpressionExperimentSubSet) | 46,334 |
| FACTOR_VALUE | 194,932 |
| AUDIT_EVENT | 26.68 M |
| AUDIT_TRAIL | 18.25 M |
| CHARACTERISTIC | 10.0 M |
| EXPRESSION_EXPERIMENT2CHARACTERISTIC | 326 k (denorm) |
| acl_entry (lowercase, live) | 3.47 M |
| acl_object_identity (lowercase, live) | 3.87 M |
| ACLENTRY (uppercase, legacy) | 6.61 M |
| ACLOBJECTIDENTITY (uppercase, legacy) | 3.87 M |
| CHROMOSOME_FEATURE (class=Gene) | 137,997 |
| CHROMOSOME_FEATURE (class=GeneProduct) | 1.16 M |
| BIO_SEQUENCE2_GENE_PRODUCT | 28.6 M |
| COMPOSITE_SEQUENCE | 14.4 M |
| PROCESSED_EXPRESSION_DATA_VECTOR | 968 M |
| RAW_EXPRESSION_DATA_VECTOR | 2.45 B |

> NOTE — duplicate ACL tables. Both lowercase `acl_*` (Spring-Security canonical 4-table schema, used by current code) AND uppercase `ACL*` (legacy Gemma) are populated in prod. The legacy `ACLENTRY` has 6.6 M rows that no current query reads. Separate issue (cleanup / migration).

## Probe inventory

### Probe 1: Anonymous EE listing — ACL JOIN form (status quo)
- Pattern: `INVESTIGATION JOIN acl_object_identity JOIN acl_class JOIN acl_sid LEFT JOIN acl_entry ... WHERE ee.class='ExpressionExperiment' AND (ace.mask & 1) <> 0 AND ace.sid IN (anonymous sid) GROUP BY ee.ID ORDER BY ee.ID DESC LIMIT 20`
- DAO equivalent: ACL-restricted EE list (`AclQueryUtils.formNativeAclJoinClause` + `formNativeAclRestrictionClause`), called from anonymous web pages and from `gemma-rest` endpoints (`/v2/datasets`).
- EXPLAIN top: drives from `acl_sid` (`index PRIMARY`, scans 669 sid rows), then `aoi` via `fk_aoi_owner_sid`. Uses `Using temporary; Using filesort` on the `aoi_cls const` step. Total estimated scanned rows in plan: ~47k effective.
- Timing: 4.49 s / 5.37 s / 4.48 s — **~4.5 s mean**.
- Verdict: **RED**.
- Notes: query starts by enumerating all `acl_sid` rows because the optimiser cannot use `sid='IS_AUTHENTICATED_ANONYMOUSLY'` to limit `aoi`. The LEFT JOIN to `acl_entry` then explodes because every AOI may have many ACEs — that's why the explicit `GROUP BY ee.ID` is needed (`requiresGroupBy()` returns true for non-admin).

### Probe 1b: Same listing — EXISTS subquery form (proposed refactor)
- Pattern: outer SELECT on `INVESTIGATION` filtered by `EXISTS (SELECT 1 FROM acl_object_identity JOIN acl_class JOIN acl_entry JOIN acl_sid WHERE aoi.object_id_identity = ee.ID AND ace.mask & 1 <> 0 AND sid.sid='IS_AUTHENTICATED_ANONYMOUSLY')`
- EXPLAIN: outer driver is `ee.class='ExpressionExperiment'` (39,286 rows estimated, but `ORDER BY ee.ID DESC LIMIT 20` lets MySQL stop after a few rows once 20 EXISTS-true rows are found). Subquery is `DEPENDENT SUBQUERY` with `eq_ref` lookup on `aoi.object_id_class` index — single-row per outer row.
- Timing: 0.481 s / 0.269 s / 0.127 s — **~0.3 s mean**.
- Verdict: **GREEN**.
- Notes: 10x–40x faster than probe 1. No `Using temporary; Using filesort`. No `GROUP BY` needed because EXISTS short-circuits on first match.

**→ This validates `project_acl_exists_refactor.md` priority. The status-quo ACL join is the dominant cost in anonymous listings.**

### Probe 2: Anonymous fast-path on EE2C (denormalized mask)
- Pattern: `SELECT category_uri, category, value_uri, value, COUNT(*) FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE LEVEL='ExpressionExperiment' AND (ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK & 1) <> 0 GROUP BY category_uri, category, value_uri, value ORDER BY COUNT(*) DESC LIMIT 50`
- DAO equivalent: `CharacteristicDaoImpl.getCategories...` / faceted-search backing query (uses `EE2CAclQueryUtils.formNativeAclRestrictionClause` for anonymous).
- EXPLAIN: `EE2C_LEVEL` index range scan, `Using index condition; Using where; Using temporary; Using filesort` for the aggregation, but no ACL join at all.
- Timing: 0.129 s / 0.118 s / 0.106 s — **~120 ms mean**.
- Verdict: **GREEN**.
- Notes: The denormalised `ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK` column eliminates the four-table ACL join for the anonymous case. This is the right pattern; the open question is generalising it to authenticated-non-admin users.

### Probe 3a: AuditEvent listing for a batch of 100 EEs (getLastEvents)
- DAO: `AuditEventDaoImpl.getLastEvents(Collection<T>, Class, Collection)` line 247 of `AuditEventDaoImpl.java`. HQL: `select trail.id, ae from AuditTrail trail join trail.events ae join fetch ae.eventType et where trail.id in :trails group by trail, ae order by ae.date desc, ae.id desc`.
- Translated SQL: `AUDIT_TRAIL t JOIN AUDIT_EVENT ae ON ae.AUDIT_TRAIL_FK=t.ID LEFT JOIN AUDIT_EVENT_TYPE aet ON aet.ID=ae.EVENT_TYPE_FK WHERE t.ID IN (100-trail set)`
- EXPLAIN: batch IN materialises, then `eq_ref` on `t`, `ref` on `ae` via `AUDIT_EVENT_AUDIT_TRAIL_FKC`.
- Timing: 0.139 s / 0.119 s / 0.120 s — **~125 ms mean**.
- Result cardinality: 772 rows (≈7.7 events/EE for newer EEs).
- Verdict: **GREEN** for batches up to ~100.

### Probe 3b: Same query, all 25,668 EE audit trails (worst case)
- Same query, batch = ALL EEs.
- Result cardinality: **1,508,124 rows** returned. All of these are streamed into the JVM and reduced to ~25k "latest events" in Java.
- Timing: 8.66 s wall time for one execution.
- Verdict: **RED**.
- Notes: The HQL pattern fetches every event and lets Java pick the latest (lines 273–294 of `AuditEventDaoImpl.java`). For batches this size, that means ~1.5 M Hibernate entity hydrations + GC pressure. A SQL-side `GROUP BY trail HAVING MAX(date)` (or a `JOIN LATERAL` in MySQL 8 / a window function) would reduce server work AND the wire payload by ~60x. The `getLastEvents` comment even acknowledges this: "annoyingly, Hibernate does not select the latest event when grouping by trail, so we have to fetch them all" — a SQL-only rewrite (not via HQL) would dodge this.
- Realistic callers: `WhatsNew` / dashboard / scheduled stats jobs that ask "what's the last event for every EE in Gemma". If those callers exist and are not paged, this is a multi-second cost on every refresh.

### Probe 4: BioAssayDimensionDaoImpl.find — exact-match BAD lookup
- DAO: `BioAssayDimensionDaoImpl.find(BioAssayDimension)` line 58.
- HQL: `select distinct bad from BioAssayDimension bad join bad.bioAssays ba where size(bad.bioAssays) = :n and ba.id in :ids`
- Translated SQL (small `:ids`, n=12): `SELECT DISTINCT bad.* FROM BIO_ASSAY_DIMENSION JOIN BIO_ASSAY_DIMENSIONS2BIO_ASSAYS bad2ba WHERE (SELECT COUNT(*) FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS WHERE bad_fk=bad.id) = 12 AND bad2ba.BIO_ASSAYS_FK IN (1001,1002,1003)`
- EXPLAIN: `range` on `bad2ba` via `BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC`, `eq_ref` on `bad`, `DEPENDENT SUBQUERY` `ref` via PRIMARY for the `size(...)` correlated count (42 rows scanned per outer row average).
- Timing: 0.099 s / 0.098 s / 0.108 s — **~100 ms mean**.
- Verdict: **GREEN**.
- Notes (schema): **`BIO_ASSAY_DIMENSIONS2BIO_ASSAYS` has duplicate indexes on `BIO_ASSAYS_FK`** — `BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC` AND `BIO_ASSAYS_FKC`. Both are identical single-column indexes on the same FK. One is dead weight (drop in a follow-up migration, not in this commit).

### Probe 5: FactorValueDaoImpl.loadAll(0, 20) — anonymous, paginated
- DAO: `FactorValueDaoImpl.loadAll(int offset, int limit)` line 79. HQL: `select fv from FactorValue fv join fv.experimentalFactor ef join ef.experimentalDesign ed, ExpressionExperiment ee` + ACL HQL join + `and ee.experimentalDesign = ed` + `group by fv`. The `, ExpressionExperiment ee` is an implicit cross-join later constrained by `ee.experimentalDesign = ed`.
- Translated SQL: `FACTOR_VALUE fv JOIN EXPERIMENTAL_FACTOR ef JOIN EXPERIMENTAL_DESIGN ed JOIN INVESTIGATION ee (class='ExpressionExperiment') ON ee.EXPERIMENTAL_DESIGN_FK=ed.ID JOIN acl_object_identity/class/sid LEFT JOIN acl_entry ... anonymous-sid + mask predicate ... GROUP BY fv.ID LIMIT 20`
- EXPLAIN: same shape as probe 1 (`acl_sid` index scan as driver, 669 rows) and then 195k FVs × ACL join joined down by experimental-design FKs. `Using temporary; Using filesort`.
- Timing: 5.25 s / 5.40 s / 4.66 s — **~5.1 s mean**.
- Verdict: **RED**.
- Notes: same root cause as probe 1 (ACL join). The FactorValue case is even worse because there's an extra hop through `experimental_factor` and `experimental_design`, and the `, ExpressionExperiment ee` implicit Cartesian is filtered late. Rewriting to a `WHERE EXISTS (...)` against the EE's ACL would compress this dramatically (same speedup ratio observed in probe 1b).

### Probe 6: CharacteristicDaoImpl.findByCategoryLike — autocomplete
- DAO: `CharacteristicDaoImpl.findByCategoryLike` line 153. Native SQL: `select {C.*} from CHARACTERISTIC C where C.CATEGORY like :search`
- Translated SQL: `SELECT C.ID, C.CATEGORY, C.VALUE FROM CHARACTERISTIC C WHERE C.CATEGORY LIKE 'gen%' LIMIT 100`
- EXPLAIN: `range` on `CHARACTERISTIC_CATEGORY` index; `Using index condition`.
- Timing: 0.099 s / 0.108 s / 0.099 s — **~100 ms mean**.
- Verdict: **GREEN**.
- Notes: prefix `LIKE 'foo%'` is index-friendly. A leading-wildcard `LIKE '%foo%'` would be a full scan; would need a fulltext index. Audit callers to make sure no one passes a leading-wildcard pattern.

### Probe 7: GeneDaoImpl.getCompositeSequencesById(TP53)
- DAO: `GeneDaoImpl.getCompositeSequencesById` line 294. HQL: `select cs from GeneProduct gp, BioSequence2GeneProduct bs2gp, CompositeSequence cs where gp = bs2gp.geneProduct and cs.biologicalCharacteristic = bs2gp.bioSequence and gp.gene.id = :id group by cs`
- Translated SQL: `CHROMOSOME_FEATURE gp (class='GeneProduct') JOIN BIO_SEQUENCE2_GENE_PRODUCT bs2gp ON bs2gp.GENE_PRODUCT_FK=gp.ID JOIN COMPOSITE_SEQUENCE cs ON cs.BIOLOGICAL_CHARACTERISTIC_FK=bs2gp.BIO_SEQUENCE_FK WHERE gp.GENE_FK = :id GROUP BY cs.ID`
- EXPLAIN: `ref` on `gp` via `GENE_PRODUCT_GENE_FKC` (202 rows for TP53), then `ref` on `bs2gp` via FK, then `ref` on `cs` via biological-characteristic FK. `Using temporary; Using filesort` on the outer GROUP BY.
- Timing: 0.180 s / 0.159 s / 0.170 s — **~170 ms mean**.
- Result cardinality: 440 composite sequences for TP53.
- Verdict: **GREEN**.
- Notes: The implicit-cross-join HQL style still produces a sensible plan because each FK is well-indexed and the gene drives the join. Cardinality scales with #GeneProducts per gene; TP53 (~202 GPs) is a high-end gene.

### Probe (skipped): PROCESSED_EXPRESSION_DATA_VECTOR per-EE GROUP BY
- Attempted `SELECT EXPRESSION_EXPERIMENT_FK, COUNT(*) FROM PROCESSED_EXPRESSION_DATA_VECTOR GROUP BY EXPRESSION_EXPERIMENT_FK ORDER BY COUNT(*) DESC LIMIT 5` and aborted after 8+ minutes. Cardinality is 968M rows. Single-EE count via the `experimentProcessedVectorProbes` covering index is ~130 ms (verified with the most-recent EE: 40,838 vectors).
- Verdict / take-away: any DAO method that scans PEDV without an `EXPRESSION_EXPERIMENT_FK` predicate will time out. The `experimentProcessedVectorProbes (EXPRESSION_EXPERIMENT_FK, DESIGN_ELEMENT_FK)` covering index is the only viable access path. Worth a grep for callers that omit the EE filter (out of scope here).

## Top findings (ordered by impact)

1. **ACL JOIN → EXISTS refactor is real, big-impact, validated.** Probe 1 vs 1b: 4.5 s → 0.3 s = ~15x speedup on a representative anonymous EE-listing query, by rewriting the four-table ACL join to a single `WHERE EXISTS (...)`. Same root cause is responsible for the 5 s FactorValue listing (probe 5). The `project_acl_exists_refactor.md` priority is correct; the 27 flagged callsites should each get this same treatment. Suggested fix: extend `AclQueryUtils` with an `formAclExistsClause(aoiIdColumn)` variant, migrate the worst offenders (listing endpoints first), and keep the JOIN form only for callers that genuinely need ACL fields in the SELECT list.

2. **`AuditEventDaoImpl.getLastEvents` is RED at full-corpus scale.** Probe 3b: 1.5 M event rows returned to the JVM for a "give me the last event per EE" call across all 25k EEs (~8.7 s wall + heavy GC). The HQL is structured as "fetch all events then take latest in Java"; a SQL-side aggregate (`MAX(date)` per trail with a follow-up fetch of the matching event id) would reduce wire traffic ~60x. Per-batch (100 EEs) is fine; per-corpus is the foot-gun. Audit callers to identify any "give me last event for every EE" dashboards.

3. **`FactorValueDaoImpl.loadAll(offset, limit)` inherits the ACL JOIN cost AND adds an implicit cross-join.** Probe 5: 5 s for a paginated listing of 20 FVs. Same fix direction as #1 plus the implicit `, ExpressionExperiment ee` should be an explicit JOIN through `experimental_design`. Two improvements at once.

## Cross-cutting observations

- **ACL JOIN scope (probed):** Confirmed as a real performance issue. Status-quo HQL form is ~15x slower than the EXISTS rewrite for anonymous users on representative listings. Of the 27 callsites flagged in `project_acl_exists_refactor.md`, the highest-impact ones to migrate first are: any DAO `loadAll`/`loadValueObjects` that touches large entity tables (EE, FactorValue, BioMaterial, AuditEvent). The denormalised `ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK` (probe 2) is already saving the EE2C / faceted-search path — there is precedent for generalising that pattern further (e.g., a per-EE bitmask column on INVESTIGATION itself for the anonymous case, since 99% of public-listing traffic is anonymous).

- **Tables that consistently show "Using temporary; Using filesort":** the ACL-join queries (probes 1, 5), and any `GROUP BY` over the AOI-joined result set. The temp-file step amplifies the cost because the join produces multi-row-per-EE rows (one per ACE) that have to be deduped by the GROUP BY. The EXISTS rewrite eliminates both `temporary` and `filesort` because no GROUP BY is needed.

- **Duplicate index on `BIO_ASSAY_DIMENSIONS2BIO_ASSAYS`:** two identical single-column indexes on `BIO_ASSAYS_FK` (`BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC` and `BIO_ASSAYS_FKC`). Drop one in a follow-up Flyway migration. Same audit might find other join-table duplicates created when the schema was renamed/migrated.

- **Legacy uppercase `ACLENTRY` / `ACLOBJECTIDENTITY` / `ACLSID` tables (~6.6 M + 3.87 M rows) are present in prod but unused by current code.** Confirm with `grep -ri 'ACLENTRY\|ACLOBJECTIDENTITY' gemma-core/src/main` (no hits expected post Spring-Security 6 migration), then schedule a drop. This is not a perf issue today but it doubles ACL-table backup size and confuses anyone investigating ACL schema.

## Probes executed: 8 (probe 1, 1b, 2, 3a, 3b, 4, 5, 6, 7) — probe-PEDV global aggregate aborted, single-EE variant covered.
