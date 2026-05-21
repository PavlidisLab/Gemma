# Coexpression-tables orphan recce

PERF_PROBE_ROUND3 flagged that the coexpression Java DAO appears gone (only `.png` / `.ucls` UML artifacts remain under `gemma-core/src/main/java/ubic/gemma/model/association/coexpression/`). The 4 `*_GENE_COEXPRESSION` tables, plus 13 sibling tables from the same retired subsystem, are still resident on prod `gemd`. This document audits the situation and confirms verdict.

**Verdict: ORPHAN — safe to drop.** The Phase 2 Java retirement happened in Phase 1c (per repo comments) and a drop migration is already drafted at `gemma-core/src/main/resources/sql/migrations/db.1.34.0_drop_coexpression.sql`. It has not been promoted into the Flyway sequence (`db/migration/mysql/V*.sql`) yet, pending ops sign-off (see `FLYWAY_PROD_FOLLOWUP.md`). This recce confirms there is no remaining barrier in code.

## 1. Production footprint

Live counts (queried via `gemd` port-forward 8000, 2026-05-20):

| Table                          | TABLE_ROWS  | DATA_LENGTH   | INDEX_LENGTH  | UPDATE_TIME |
|--------------------------------|-------------|---------------|---------------|-------------|
| `HUMAN_GENE_COEXPRESSION`      | 444,213,993 | 31,165,775,872 | 40,438,218,752 | NULL        |
| `MOUSE_GENE_COEXPRESSION`      | 353,316,348 | 24,794,628,096 | 32,179,601,408 | NULL        |
| `RAT_GENE_COEXPRESSION`        |  87,636,996 |  6,140,461,056 |  8,106,459,136 | NULL        |
| `OTHER_GENE_COEXPRESSION`      |           0 |         16,384 |         32,768 | NULL        |

`UPDATE_TIME = NULL` on all four — InnoDB reports NULL when the table has not been mutated since the server's last restart; combined with the row counts being unchanged across earlier audits, the tables are dormant.

**Headline:** the four `*_GENE_COEXPRESSION` tables alone consume **~146 GB** (data + index). Dropping them is the single largest disk-reclaim available on prod that doesn't require a data migration.

### Sibling orphans in the same retired subsystem

`db.1.34.0_drop_coexpression.sql` also targets these (all empty per the migration's audit; sizes not re-measured this session because the port-forward dropped):

- `HUMAN_EXPERIMENT_COEXPRESSION`, `MOUSE_EXPERIMENT_COEXPRESSION`, `RAT_EXPERIMENT_COEXPRESSION`, `OTHER_EXPERIMENT_COEXPRESSION`
- `HUMAN_LINK_SUPPORT_DETAILS`, `MOUSE_LINK_SUPPORT_DETAILS`, `RAT_LINK_SUPPORT_DETAILS`, `OTHER_LINK_SUPPORT_DETAILS`
- `COEXPRESSION_NODE_DEGREE`, `COEXP_CORRELATION_DISTRIBUTION`, `GENE_COEX_GENES`, `GENE_COEX_TESTED_IN`, `USER_PROBE_CO_EXPRESSION`

The 4 `*_GENE_COEXPRESSION` tables also FK-reference the corresponding `*_LINK_SUPPORT_DETAILS` tables (constraint `FKF9E6557FC02BF5B4` on `HUMAN_GENE_COEXPRESSION.SUPPORT_DETAILS_FK` → `HUMAN_LINK_SUPPORT_DETAILS.ID`, etc.), so a drop must respect ordering — already done correctly in `db.1.34.0_drop_coexpression.sql`.

The legacy migration also pre-drops `ANALYSIS.FKF19622DC6BCD8439` (the FK pinning `COEXP_CORRELATION_DISTRIBUTION`) and leaves the now-unmapped `COEXPRESSION_MATRIX longblob` and `COEXP_CORRELATION_DISTRIBUTION_FK` columns on `ANALYSIS` for a separate cleanup pass.

## 2. Java references — classification

Search: `grep -rni 'coexpression\|Coexpression' gemma-core/src/main/java gemma-rest/src/main/java gemma-cli/src/main/java --include='*.java'`.

Result: **47 hits, none of them touch the 4 orphan tables.** All live references fall under `SampleCoexpression*` (per-EE QC correlation heatmap, still active — see "Not orphan" below) or are stale comments.

| Bucket                                                          | Verdict           | Files |
|-----------------------------------------------------------------|-------------------|-------|
| `SampleCoexpressionAnalysis` / `SampleCoexpressionMatrix` / `SampleCoexpressionAnalysisService` / `SampleCoexpressionAnalysisDao` | **LIVE — not orphan** | `gemma-core/src/main/java/ubic/gemma/model/analysis/expression/coexpression/SampleCoexpressionAnalysis.java`, `gemma-core/src/main/java/ubic/gemma/model/analysis/expression/coexpression/SampleCoexpressionMatrix.java`, `gemma-core/src/main/java/ubic/gemma/persistence/service/analysis/expression/sampleCoexpression/SampleCoexpressionAnalysis{Service,ServiceImpl,Dao,DaoImpl}.java`, plus 6 callsites (`OutlierDetectionService*`, `PreprocessorHelperService*`, `AnalysisUtilService*`, `ExpressionExperimentPlatformSwitchService`, `DataUpdaterImpl`, `ExpressionExperimentReportServiceImpl`). Mapped to `SAMPLE_COEXPRESSION_MATRIX` and `ANALYSIS.SAMPLE_COEXPRESSION_MATRIX_(RAW|REG)_FK`, not the orphan tables. |
| `RelationshipPersister` line 57 comment                          | Stale comment confirming subsystem retirement | `gemma-core/src/main/java/ubic/gemma/persistence/persister/RelationshipPersister.java:57` — "the gene-gene coexpression subsystem was removed, so `CoexpressionAnalysis` handling is gone too." |
| `Analysis.hbm.xml` line 67-70 comment                            | Stale comment confirming subsystem retirement | `gemma-core/src/main/resources/ubic/gemma/model/analysis/Analysis.hbm.xml:67-70` — "Phase 2 Step 3 retired the gene-gene CoexpressionAnalysis subsystem (CoexpressionAnalysis, CoexpCorrelationDistribution, the species-specific Gene2GeneCoexpression and ExperimentCoexpressionLink subclasses, etc.)." |
| `ExpressionExperimentSetDaoImpl.java:171-178`                    | Dead HQL ref to `CoexpressionAnalysis` | This is an `extends Analysis` query that no longer matches anything (the subclass discriminator is gone). It returns zero counts and silently passes through. Not load-bearing for the orphan-drop question — but worth a follow-up cleanup. |
| `EhcacheConfig.java:152`                                         | L2 cache config still names `SampleCoexpressionMatrix` | Live; that class is in the LIVE bucket above. Not an orphan-table reference. |
| `AclClassMetadata.java:7,98,100`                                 | ACL parent-mapping for `SampleCoexpressionAnalysis` | Live. |
| `ExpressionDataFileService*` `deleteCoexpressionDataFile` / `getCoexpressionDataFilename` | Dead file-path stub | These methods delete a hypothetical on-disk `*-coexpression*.txt` file; not table-touching. Either dead code or harmless — out of scope for this recce. |
| `.png` / `.ucls` files                                           | Stale UML diagrams         | `gemma-core/src/main/java/ubic/gemma/model/association/coexpression/Coexpression.png`, `Coexpression.ucls`. Reference Java classes (`Gene2GeneCoexpression`, etc.) that no longer exist. Safe to delete in a separate sweep. |

**Bottom line:** zero live Java code reads or writes `HUMAN_GENE_COEXPRESSION`, `MOUSE_GENE_COEXPRESSION`, `RAT_GENE_COEXPRESSION`, `OTHER_GENE_COEXPRESSION`, or any of the 13 sibling orphan tables. No HBM mapping references them. No HQL/native query touches them. The Hibernate baseline regeneration (`MysqlSchemaBaselineDumper`) doesn't model them.

### Distinct: `SampleCoexpressionMatrix` is NOT orphan

`SAMPLE_COEXPRESSION_MATRIX` (per-EE QC correlation matrix) is mapped by `ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix` and reachable from `ANALYSIS.SAMPLE_COEXPRESSION_MATRIX_(RAW|REG)_FK`. It is **not** in the orphan list and `db.1.34.0_drop_coexpression.sql` explicitly preserves it (see the "NOT dropped here" section of that file).

## 3. Verdict

**ORPHAN. Drop migration is already drafted as `gemma-core/src/main/resources/sql/migrations/db.1.34.0_drop_coexpression.sql`.** No code blocker remains.

Estimated drop savings (the 4 `*_GENE_COEXPRESSION` tables alone):

- Data: ~62 GB
- Indexes: ~80 GB
- **Total: ~146 GB reclaimed on prod after `OPTIMIZE TABLE` / `innodb_file_per_table` reclaim**

The 13 sibling tables in the same drop migration are empty per the migration's audit; their reclaim is symbolic.

## 4. Recommended next step (Phase 2 drop migration)

**Do NOT commit this in the current branch.** This recce documents the verdict only. When ops signs off:

1. Promote `gemma-core/src/main/resources/sql/migrations/db.1.34.0_drop_coexpression.sql` into the Flyway-managed sequence as `gemma-core/src/main/resources/db/migration/mysql/VN__drop_coexpression_orphans.sql` (where `N` is next-in-sequence at the time of cutover).
2. Add the H2 sibling — H2's `V1__hibernate_baseline.sql` does **not** create any of these tables (Hibernate metadata no longer models them), so the H2 migration is a no-op `-- intentionally empty: H2 baseline does not contain these tables` placeholder, OR skip the H2 file entirely and rely on Flyway only applying `mysql/` to MySQL targets (which is the current setup per `BaseDatabaseTest5` Flyway bean).
3. Run against `gemdtest` first (full `mvn verify` to confirm no test reaches an orphan table), then prod during a downtime window with `OPTIMIZE TABLE` follow-up to reclaim space.
4. Separate follow-up migration to drop the still-unmapped `ANALYSIS.COEXPRESSION_MATRIX longblob` and `ANALYSIS.COEXP_CORRELATION_DISTRIBUTION_FK` columns left behind by `db.1.34.0_drop_coexpression.sql`.

## 5. Provenance

- Java reference scan: `grep -rni 'coexpression\|Coexpression' gemma-core/src/main/java gemma-rest/src/main/java gemma-cli/src/main/java --include='*.java'` on worktree branch `schema-raw-vector-index-plus-coex-recce` baselined at `a298adb585`.
- Prod table inventory: `INFORMATION_SCHEMA.TABLES` on `gemd` via local port-8000 forward, READ-ONLY user `gemmaadmin`.
- `SHOW CREATE TABLE HUMAN_GENE_COEXPRESSION` captured the `FKF9E6557FC02BF5B4` → `HUMAN_LINK_SUPPORT_DETAILS` ordering dependency.
- Pre-drafted drop migration: `gemma-core/src/main/resources/sql/migrations/db.1.34.0_drop_coexpression.sql` (not yet Flyway-promoted).
- Cross-reference: `FLYWAY_PROD_FOLLOWUP.md` documents the Coexpression subsystem under "Drift findings from the prod dump → category 2".
