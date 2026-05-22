# HQL / native SQL audit — gemma-core

Recce-only inventory and prioritized findings across every `createQuery`,
`createNativeQuery`, JPA Criteria, and HBM-XML query block in
`gemma-core`. Baseline: `ce9738722b` (branch `phase2-acl-migrate`).
No production code changed in this round; orchestrator picks the fixes.

## 1. Inventory

| Surface | Count | Notes |
|---|---:|---|
| HQL via `createQuery(...)` | 620 | The dominant surface; sampled ~70 deeply. |
| Native SQL via `createNativeQuery(...)` | 80 | Concentrated in 14 files, mostly perf-critical paths. |
| Deprecated `createSQLQuery(...)` | 0 | Already purged in HB6 migration. |
| Deprecated `createMutationQuery(...)` | 0 | Not used. |
| `getSession().createCriteria(...)` (legacy) | 0 | Already purged. |
| JPA Criteria `cb.createQuery(...)` | 38 | Mostly in `AbstractDao` + `AbstractCriteriaFilteringVoEnabledDao`. |
| HBM `<query>` / `<sql-query>` blocks | 0 | No embedded XML queries — all in Java. |
| `@NamedQuery` / `@NamedNativeQuery` | 0 | None. |
| HBM XML mapping files total | 70 | Reviewed structure only. |

Top files by HQL count (sampled deeply):

| File | createQuery |
|---|---:|
| `ExpressionExperimentDaoImpl` | 161 |
| `GeneDaoImpl` | 49 |
| `ArrayDesignDaoImpl` | 45 |
| `BusinessKey` | 34 |
| `CompositeSequenceDaoImpl` | 21 |
| `DifferentialExpressionAnalysisDaoImpl` | 19 |
| `AbstractDao` | 16 |
| `ExpressionAnalysisResultSetDaoImpl` | 14 |
| `GeneSetDaoImpl` | 13 |
| `QuantitationTypeDaoImpl` | 13 |
| `FactorValueDaoImpl` | 12 |
| `BibliographicReferenceDaoImpl` | 12 |
| `DifferentialExpressionResultDaoImpl` | 12 |
| `BioSequenceDaoImpl` | 10 |
| `BlacklistedEntityDaoImpl` | 10 |

Files with at least one native SQL: `ExpressionExperimentDaoImpl`,
`CompositeSequenceDaoImpl`, `CharacteristicDaoImpl`, `ArrayDesignDaoImpl`,
`GeneSetDaoImpl`, `GeneDaoImpl`, `DifferentialExpressionAnalysisDaoImpl`,
`DifferentialExpressionResultDaoImpl`, `ExpressionAnalysisResultSetDaoImpl`,
`PrincipalComponentAnalysisDaoImpl`,
`SingleCellDimensionExperimentDaoImpl`, `TableMaintenanceUtilImpl`,
`CommonQueries`, `AclLinterServiceImpl`.

## 2. Findings by category

Severity scale:
- **high**: incorrect behaviour, dataset-corruption potential, build-vs-prod
  drift, or a measured perf regression
- **medium**: silent perf cliff for realistic inputs, fragile coupling, or
  correctness pitfall behind a rarely-exercised branch
- **low**: code-hygiene / micro-perf / docs / future-proofing

### Category P — N+1 / performance smells

| # | Where | Issue | Severity | Suggested fix |
|---|---|---|---:|---|
| P1 | `ExpressionExperimentDaoImpl.java:1278` `getExperimentsLackingPublications` | `from ExpressionExperiment e where e.primaryPublication = null and e.shortName like 'GSE%'` — unbounded result, fetches all EEs, no fetch plan, no LIMIT. Called by curation tasks. | medium | Add `setMaxResults` / batched paging; consider returning only IDs. |
| P2 | `ProcessedExpressionDataVectorDaoImpl.java:155,167` `getRandomProcessedVectors` | `order by RAND()` over the entire `dedv.expressionExperiment = :ee` set. MySQL evaluates RAND() per row and full-sorts; cost ∝ N for the experiment regardless of `limit`. | high | Approach 1: sample IDs in app code (cheap ID scan + random pick + IN query). Approach 2: `WHERE RAND() < threshold` with adaptive threshold from `numberOfDataVectors`. |
| P3 | `ArrayDesignDaoImpl.java:507-514` `getGenesByCompositeSequence(Collection<ArrayDesign>)` | Calls the per-AD variant inside `.stream().map().reduce(...)` — one query per platform. Real N+1 over the AD list. | high | Single query with `where gene2cs.AD in :adIds`; group in-memory. The single-AD HQL is already general enough. |
| P4 | `AuditEventDaoImpl.java:267-294` `getLastEvents` | Pulls every event for every trail (`group by trail, ae order by date desc, id desc`) then `putIfAbsent` keeps only the latest in Java. For long-lived auditables this materialises thousands of rows to discard most of them. | medium | Use a windowed approach (`row_number() over (partition by trail order by date desc, id desc)` ≤ 1) via native query, or correlated `max(date)` sub-query. |
| P5 | `GeneDaoImpl.java:388-400` `thaw(Gene)` | 7-level deep `JOIN FETCH` (`aliases` × `accessions` × `products.accessions` × `products.physicalLocation.chromosome` × `taxon`). Cartesian product hazard: rows = aliases × accessions × products × accessions-per-product. With `distinct` Hibernate dedupes in memory but the DB still ships the full cross-product. | medium | Split into separate fetch queries (one per collection root) and dedupe via `Hibernate.initialize`. Pattern already used in `BioMaterialDaoImpl.thawBioMaterialsForBioAssays`. |
| P6 | `ExpressionExperimentDaoImpl.java:498-512` `findByExpressedGene` & line 641-651 `findByGene` | `findByGene` form: `joinINVESTIGATION → BIO_ASSAY → ARRAY_DESIGN → COMPOSITE_SEQUENCE → GENE2CS`. Returns IDs, then `this.load(eeIds)` rehydrates EEs in a second pass. Two DB round trips when one (`join fetch`) would do. | low | Project EE entity directly via native `{ee.*}` + `addEntity`, OR keep two-pass but batch the load via `loadAsMap` (the pattern from probe #8). |
| P7 | `ExpressionExperimentDaoImpl.java:1551-1561` `getBioAssayDimensions(ee, qt)` | Iterates over `bulkDataVectorTypes` calling a per-vector-type query inside `.map()`. N queries for N vector types (currently 2). Cheap individually but compounding. | low | Single HQL with `from BulkExpressionDataVector v` (the polymorphic base) — Hibernate generates a UNION ALL automatically. |
| P8 | `DifferentialExpressionResultDaoImpl.java:432-450` `getProbeIdsForGene` (non-useGene2Cs branch) | Correlated `(select count(distinct gp2.gene) from ... where cs2 = cs) = 1` runs once per outer row in the worst case. MySQL may or may not flatten this; when it doesn't, the cost is N². | medium | Rewrite the dedup as `group by cs.id having count(distinct gp.gene.id) = 1` on the same scan. Same shape, deterministic plan. |
| P9 | `ExpressionExperimentDaoImpl.java:1341-1356` `getGenesUsedByPreferredVectors` | No filter on `quantitationType.isPreferred` — relies on `PROCESSED_EXPRESSION_DATA_VECTOR` containing only preferred vectors. Method name says "preferred" so the implicit assumption is documented elsewhere but not enforced by the query. | low | Add `and pedv.quantitationType.isPreferred = true` for defence-in-depth, or rename the method. |
| P10 | `DifferentialExpressionResultDaoImpl.java:509` `findGeneResultsByResultSetIdsAndGeneIds` | `FORCE INDEX (probeResultSets)` hint hard-codes an index name. Fragile to schema renames; optimiser hints are guard-rails not crutches. | low | Drop the hint and verify the optimiser picks the index. If it doesn't, add a covering index instead. |

### Category C — Correctness smells

| # | Where | Issue | Severity | Suggested fix |
|---|---|---|---:|---|
| C1 | `BibliographicReferenceDaoImpl.java:176` `browse(start, limit, orderField, descending)` | `"from BibliographicReference order by :orderField " + (descending ? "desc" : "")` — **parameterized column name in ORDER BY**. HQL won't bind `:orderField` as an expression; either silently ignored (rows come back in DB-natural order, breaking the contract) or fails at runtime depending on driver. Open-coded `desc` concatenation also makes the `descending=false` arm emit `order by ?` with no direction. | high | Whitelist sortable columns and inject the name; reuse `Sort` / `FilterQueryUtils.formOrderByClause`. Same pattern lives in `AbstractFilteringVoEnabledDao`. |
| C2 | `CharacteristicDaoImpl.java:400-407` `findBestByUri` | HQL clause `having c.value <> null` — `<> null` is unknown in three-valued logic, never true. The HAVING filter is silently no-op; all rows including null-value characteristics pass through. | high | Change to `having c.value is not null`. |
| C3 | `ExpressionExperimentDaoImpl.java:1791-1801` `getPopulatedFactorCountsExcludeBatch` | `cat.category != :category and cat.categoryUri != :categoryUri and ef.name != :name` — three-way `!=` against potentially-NULL columns. Any factor with a NULL category or categoryUri gets silently excluded (NULL `!=` literal evaluates to UNKNOWN in SQL, filtered out by WHERE). | medium | Wrap each predicate with `coalesce` or change to `(cat.category is null or cat.category != :category)`. |
| C4 | `GeneDaoImpl.java:540-621` `find(Gene)` | Side-effect delete inside a `find` method (when duplicates found). The code self-comments: `// FIXME this can fail because 'find' methods are read-only`. Read-only transactions throw; the comment confirms the bug surfaces in nested-call paths. | medium | Hoist the dedup-on-find side effect into a separate write method; never mutate from a `find`. |
| C5 | `AclQueryUtils.java:253,264,288` native EXISTS clause builder | `formNativeAclJoinClause` stashes the AOI id column on a `ThreadLocal<String>` for `formNativeAclRestrictionClause` to read. The two methods MUST be called in order in the same call chain. Self-documented and currently in-tree callers respect it, but the coupling is invisible at the call site and breaks if any future helper interleaves them. | medium | Pass the id column explicitly as a parameter to `formNativeAclRestrictionClause`. Bonus: the `formNativeAclJoinClause` then becomes a no-op that can be retired. |
| C6 | `ExpressionExperimentDaoImpl.java:645` `findByGene` | Native query hard-codes `join gemd.COMPOSITE_SEQUENCE` — explicit schema prefix `gemd.`. Test database is `gemdtest`; this query would fail there if MySQL `lower_case_table_names`=0 and the schema-qualified reference is honoured. The other tables in the same query are unqualified, so the inconsistency is the actual bug. | medium | Drop the `gemd.` prefix. Single-occurrence (verified by grep). |
| C7 | `GeneDaoImpl.java:163-167,170-175` `findByOfficialSymbol(symbol, taxon)` / `findByOfficialSymbolInexact` | Case-sensitive `=` and `LIKE` against user-supplied gene symbols. Gene symbols are mixed-case in source data (BRCA1, p53, miR-21). Callers that pass un-uppercased input get empty results silently. | medium | `lower(g.officialSymbol) = lower(:symbol)` — or document the contract that callers normalise. The existing `findByOfficialSymbols(Collection,Long)` callsite (line 191) confirms callers expect case folding via `toLowerCase()` post-fetch. |
| C8 | `ExpressionExperimentDaoImpl.java:286` `browse(start, limit, orderField, descending)` | Throws `NotImplementedException`. The interface contract advertises ordering but the implementation refuses; clients silently work around. Either implement or remove from the interface. | low | Remove from interface; downgrade signature to the un-ordered `browse(start, limit)`. |
| C9 | `TableMaintenanceUtilImpl.java:405` `updateExpressionExperiment2ArrayDesignEntries(truncate=true)` | `.addScalar(EE2AD_QUERY_SPACE)` on a DELETE query — `addScalar` is for SELECT result-column registration and is meaningless on DML. Likely a typo for `addSynchronizedQuerySpace`, which would actually invalidate the query cache. The cache may not be invalidated after the truncate; subsequent reads see stale rows until the immediately-following insert lands. | high | Change to `addSynchronizedQuerySpace(EE2AD_QUERY_SPACE)`. |

### Category I — Security (HQL injection / parameter binding)

The previous SQL_INJECTION_HIBERNATE triage classified ~80 sites as
internal-safe (string-concatenated keywords, never user input) and ~5 as
whitelist-validated. Re-confirming at the current code level:

| # | Where | Issue | Severity | Suggested fix |
|---|---|---|---:|---|
| I1 | `ExpressionExperimentDaoImpl.java:1322,1568,1610,1622` | `dataVectorType.getSimpleName()` interpolated into HQL `from ` clause. Source: a `Class<? extends BulkExpressionDataVector>` argument. Caller-supplied at the service layer, never reaches HTTP. Risk: low (typed Class, not String). | low | No action; the type-system gate is sufficient. Worth a `// language=HQL` comment so static analysers don't false-positive. |
| I2 | `AuditEventDaoImpl.java:191,194,214` | `getEntityName(sessionFactory, auditableClass)` result is interpolated into the HQL `from <entityName> adb`. `getEntityName` resolves Class → string entity name; safe by construction. | low | No action. |
| I3 | `CharacteristicDaoImpl.java:251,343` `buildFindExperimentsByUrisUnionAll` | Builds UNION ALL by string concatenation of hard-coded column names (`VALUE_URI`, `PREDICATE_URI`, `OBJECT_URI`, `SECOND_PREDICATE_URI`, `SECOND_OBJECT_URI`). Column names come from a closed list, not user input. | low | No action. The UNION ALL rewrite is well-documented and stays inside the closed alphabet. |
| I4 | `BibliographicReferenceDaoImpl.java:176` (see C1) | `:orderField` as a *parameter* — Hibernate may bind it as a literal string, which would render `order by 'someColumn' desc` and effectively no-op the sort. NOT injection (the value is bound, not concatenated), but a correctness bug. | high (already filed under C1) | See C1. |

Outside the audit scope but worth flagging: `formNativeAclRestrictionClause` 
interpolates `BitwiseUtils.bitand(dialect, ...)` SQL into the EXISTS body. 
The dialect-derived snippet is closed-alphabet (`(a & b)` or `BITAND(a, b)`). 
Safe.

### Category M — Missing-index implications

Note: schema-managed indexes live in `gemma-core/src/main/resources/sql/migrations/db.*.sql` 
and in each entity's HBM file. The post-create `init-entities.sql` only adds the 
`CHARACTERISTIC` URI-prefix indexes. A full index inventory is out of scope; the 
findings below are queries where the join/filter shape clearly requires an index 
that may not exist.

| # | Where | Issue | Severity | Suggested fix |
|---|---|---|---:|---|
| M1 | `BioMaterialDaoImpl.java:185-189` `thawBioMaterialsForBioAssays` BFS loop | `where bm.id in :ids and bm.sourceBioMaterial is not null` — `BIO_MATERIAL.SOURCE_BIO_MATERIAL_FK` is the self-FK. Indexed via the implicit FK index in MySQL InnoDB, fine. The full query is cheap. *Listed here as confirmation*. | n/a | No action. |
| M2 | `ExpressionExperimentDaoImpl.java:1296-1300` `countBioMaterials` | `count(distinct bm) from ExpressionExperiment ee join ee.bioAssays ba join ba.sampleUsed bm` + Filters. Join chain is FK-indexed (default). No issue. | low | No action. |
| M3 | `AuditEventDaoImpl.java:117-123` cursor pagination | `where t = :at and e.id > :lastSeenId order by e.id`. Composite `(audit_trail_fk, id)` index would help; otherwise relies on PK + FK index intersection. | low | Verify via `EXPLAIN`; add composite index only if production traffic shows the seek-pagination is slow. |
| M4 | `CharacteristicDaoImpl.java:545,566` `findByValueLike` `LIKE :search` over `CHARACTERISTIC.\`VALUE\`` | Leading-wildcard LIKE bypasses any normal index. Already documented (see line 244 perf-probe comment) for the cross-column case. For single-column it's a full scan. | medium | Document the expected `:search` shape (must be prefix, not substring). If substring search is required, full-text index or trigram is the proper fix. |
| M5 | `ExpressionAnalysisResultSetDaoImpl.java:484-497` `getBaselinesForInteractionsByIds` | `select cr.* from CONTRAST_RESULT cr left join FACTOR_VALUE fv1 on cr.FACTOR_VALUE_FK = fv1.ID left join FACTOR_VALUE fv2 on cr.SECOND_FACTOR_VALUE_FK = fv2.ID where cr.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK in :resultIds group by cr.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK`. The GROUP BY without aggregate on `fv1.EXPERIMENTAL_FACTOR_FK` etc. relies on MySQL's "any value" relaxed group-by — sql_mode `ONLY_FULL_GROUP_BY` (default in MySQL 5.7+) rejects this. | high | Wrap the non-grouped columns in `MIN(...)` / `MAX(...)` (the comment confirms the assumption that all CRs for a given result have identical FV refs — so MAX is correct). |

### Category B — HB6 behaviour drift

| # | Where | Issue | Severity | Suggested fix |
|---|---|---|---:|---|
| B1 | `ExpressionAnalysisResultSetDaoImpl.java:138-165` `loadWithResultsAndContrasts(id, threshold, offset, limit)` | Inline comment notes HB6 stricter handling of negative `setMaxResults` and that the legacy path passed negative to mean "no limit". Already handled in this file but worth a sweep — there are 77 `setMaxResults` callsites; spot-check 5 for the same legacy pattern. | low | One-off targeted audit. |
| B2 | `ExpressionExperimentDaoImpl.java:1542-1546` `getBioAssayDimensions(ExpressionExperiment)` | Inline comment notes the prior HQL hit an HB6 internal `AssertionError` ("at BaseSqmToSqlAstConverter.visitTableGroup") and was rewritten to use a sub-query. Indicates HB6 SQM translator brittleness on multi-root + multi-collection joins. Flagging as a known gotcha class — there may be other multi-root HQL forms in the codebase that haven't surfaced yet. | low | Add a Hibernate-version probe test that exercises the known-fragile HQL shapes against the HB6 SQM translator. |
| B3 | `BibliographicReferenceDaoImpl.java:153-167` `findAllAccessions` / `findAll` | `createQuery("from BibliographicReference")` returns an untyped `Query` then `.list()`. HB6 emits a deprecation warning on the untyped form. | low | Add the result type: `createQuery("from BibliographicReference", BibliographicReference.class)`. |
| B4 | `GeneDaoImpl.java:466,481,493` `removeAll` ID-collection queries | `createQuery("select gp.id from Gene g join g.products gp")` etc. Same untyped pattern as B3. | low | Same fix. |

## 3. Top 10 highest-value fixes (orchestrator's pick list)

Ordered by impact-per-effort. Each one is independently shippable.

1. **C9** — `TableMaintenanceUtilImpl.java:405`: `addScalar` → `addSynchronizedQuerySpace` on the DELETE. One-line fix; restores expected cache invalidation after truncate. **High severity / trivial effort.**

2. **C2** — `CharacteristicDaoImpl.java:400-407`: `<> null` → `is not null` in `findBestByUri` HAVING. One-line fix; restores intended filter. **High severity / trivial effort.**

3. **C6** — `ExpressionExperimentDaoImpl.java:645`: drop `gemd.` schema prefix from `findByGene` native query. One-token fix; removes test-vs-prod schema drift. **Medium severity / trivial effort.**

4. **M5** — `ExpressionAnalysisResultSetDaoImpl.java:484-497`: wrap non-grouped columns in `MAX(...)` so the query is ONLY_FULL_GROUP_BY-compliant. **High severity / small effort** (handful of column references).

5. **C1** — `BibliographicReferenceDaoImpl.java:176`: replace parameterized `order by :orderField` with whitelist+injected column name. **High severity / small effort.**

6. **P3** — `ArrayDesignDaoImpl.java:507-514`: collapse the per-AD loop into a single `gene2cs.AD in :adIds` query. **High severity / small effort.** Real N+1 over the AD list.

7. **P2** — `ProcessedExpressionDataVectorDaoImpl.java:155,167`: replace `order by RAND()` with sample-IDs-in-app-code. **High severity / medium effort.** Measured perf cliff for large EEs.

8. **C3** — `ExpressionExperimentDaoImpl.java:1791-1801`: NULL-safe rewrites for `getPopulatedFactorCountsExcludeBatch`. **Medium severity / small effort.**

9. **C5** — `AclQueryUtils.java` ThreadLocal coupling: pass `aoiIdColumn` explicitly to `formNativeAclRestrictionClause`. **Medium severity / small effort.** Removes a hidden landmine for future native-ACL callers.

10. **P5** — `GeneDaoImpl.thaw(Gene)`: split the 7-level JOIN FETCH into separate fetch queries. **Medium severity / medium effort.** Cartesian-product hazard.

Cumulative effort estimate: 1–2 sessions if grouped; the first three are sub-30-minute fixes.

## 4. Cross-cutting recommendations

1. **Native-SQL parameter-name + schema discipline.** Most native queries are
   correct. The two outliers (`gemd.` prefix in C6, `addScalar` typo in C9)
   suggest the codebase would benefit from a `@VerifyNativeQuery` static
   check that lints for: schema prefixes, `addScalar` on DML, and
   `addSynchronizedQuerySpace` presence on cache-relevant tables. Could
   live as a Spotbugs custom detector.

2. **`<> null` vs `is null` lint.** C2 surfaced one occurrence; the pattern
   is easy to overlook. A grep-based pre-commit check (`grep -rn '<>\s*null\|!=\s*null' --include='*.java' -- (HQL strings only)`) would catch
   future ones. Best place: `mvn verify` hookable rule, or a custom
   Checkstyle / Spotbugs detector.

3. **Polymorphic dispatch over `bulkDataVectorTypes`.** Multiple places
   (`ExpressionExperimentDaoImpl.getBioAssayDimensions`,
   `.getBioAssayDimension`, similar in `Vector` DAOs) iterate over the
   bulk vector types calling per-type queries. HQL is polymorphism-aware
   (`from BulkExpressionDataVector v`) — switching to the base class
   collapses N queries to 1 in each case. P7 is the headline; survey for
   sibling patterns.

4. **`order by RAND()` is the wrong shape.** P2 is the only current
   occurrence (in `getRandomProcessedVectors`). Add a `// FIXME no RAND()`
   comment near the offending lines so this doesn't get copy-pasted into
   future DAOs.

5. **Cursor + windowed-aggregate pattern is under-used.** P4 in
   `AuditEventDaoImpl.getLastEvents` is the prime example: fetching all
   rows to keep one is wasteful. There are probably other "latest-per-X"
   queries — sweep for `order by ... desc` + `Java putIfAbsent` patterns
   and migrate to `row_number() over (partition by ...)` or correlated
   `max(...)`.

6. **JPA Criteria API consistency.** `AbstractDao` + `AbstractCriteriaFilteringVoEnabledDao` 
   already use JPA Criteria. The four typed `createQuery(string, EntityType.class)` 
   misses (B3, B4) are low-risk untyped queries that pre-date the typed-form 
   migration. A find-replace pass over `createQuery("...")` returning entities 
   would clean these up without semantic risk.

7. **Native ACL EXISTS clause: rethink the API surface.** C5 (the
   `ThreadLocal<String>` between `formNativeAclJoinClause` and
   `formNativeAclRestrictionClause`) is a hidden cross-call coupling
   inherited from the EXISTS rewrite. The HQL form already takes
   `aoiIdColumn` explicitly. Aligning the native form to the same
   signature is mechanical and removes the cross-call invariant.

8. **`thaw*` methods deserve a uniform pattern.** `GeneDao.thaw` uses
   one giant JOIN FETCH (P5); `BioMaterialDao.thawBioMaterialsForBioAssays`
   uses split fetches per collection (modern, no Cartesian). The split
   pattern is the correct one — apply uniformly to the other `thaw*`
   methods in `GeneDao`, `ArrayDesignDao`, etc.

---

End of audit. No code changed.
