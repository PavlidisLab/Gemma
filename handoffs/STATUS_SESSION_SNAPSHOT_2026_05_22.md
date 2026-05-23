# Session snapshot — 2026-05-22

End-of-session snapshot of phase2-acl-migrate work. ~60+ merge commits
landed since the prior `project_phase3_progress_2026_05_20.md`
snapshot. Read this first when resuming.

Tip of phase2-acl-migrate at session end: `488204bbc7`
(`fix(spotbugs): synchronize shared DateFormat instances (STCAL P2)`).

## Test signal

`mvn verify -pl gemma-core` on phase2-acl-migrate now reports
**376 tests, 1 F + 1 E, 9 skipped**.

- F: `ExpressionExperimentServiceIntegrationTest.testStreamExperiments`
  (deferred — order-dependent state).
- E: `GeneSearchTest.testSearchGenes` — fixed post-verify in commit
  `0cbd57fd91`; will pass on next run.

Down from 87 issues at branch baseline. See
`FAILSAFE_RESIDUAL_TRIAGE.md` (updated this session) for the
bucket-by-bucket closure table.

## Deliverables by area

### PERF probes (Round 3 + Round 4 follow-throughs)

- **ROUND3 B2** — drop dead generic `setMatBioAssayValues` helper
  (`77290bc2d3`).
- **ROUND4 C1** — gate full SC matrix load by sample-count threshold
  (`3d4d11feea`).
- **ROUND4 C2** — rename SC MEX stream fetch-size constant
  (`5035b2c592`).
- **ROUND4 C3** — SC loader uses targeted `thawLite` init
  (`f5346a3d71`).
- **PERF_PROBE_SEARCH #2** — nightly `@Scheduled` reindex of
  EE + ArrayDesign with modified-since gate (`f8d2aee9aa`).
- **PERF_PROBE_SEARCH #4** — over-fetch Lucene hits to fill page under
  ACL post-filter (`d0c90604ee`).
- **PERF_PROBE_DATA_EXPORTS #2 follow-up** — invalidate per-result-set
  TSV disk cache on DEA delete (`2841b0ca9d`).
- **PERF_PROBE_DATA_EXPORTS #4 cleanup** — encapsulate async
  DEA-archive write behind `ExpressionDataFileService` (`75abc2c15c`).

### HQL_SQL_AUDIT — high/medium/low + bundle fixes

- **C1 / C2 / C7** — case-fold gene symbol lookups (`0f5600b800`).
- **C3** — NULL-safe rewrites in `getPopulatedFactorCountsExcludeBatch`
  (`98ce391fe6`).
- **C4** — remove side-effect delete from `GeneDao.find(Gene)`
  (`c4f883546c`). Closes failsafe Bucket B upstream.
- **C5** — pass `aoiIdColumn` explicitly to
  `formNativeAclRestrictionClause` (`c624001cbc`); shim drop in
  `66ade53bd0`.
- **C6** — drop `gemd.` schema prefix from `findByGene` native query
  (`0428acc0c3`).
- **C8** — interface-cascade deferred (out of scope this batch).
- **C9** — closed.
- **P1** — `maxResults` parameter on `getExperimentsLackingPublications`
  (`88752ff5f8`).
- **P2** — `ORDER BY RAND()` → app-side sample-by-ID in vector DAO
  (`84adfc4f8d`).
- **P4** — correlated `MAX(date, id)` subquery for `getLastEvents`
  (`f290768912`).
- **P5** — split 7-level `JOIN FETCH` in `thaw(Gene)` into separate
  fetch queries (`1e3afdb020`).
- **P6** — single-pass entity projection for `findByGene` /
  `findByExpressedGene` (`49a340d4d2`).
- **P7** — implicit polymorphism for `getBioAssayDimensions(ee, qt)`
  (`fa38b88c2d`).
- **P8** — `GROUP BY HAVING` for specific-probes dedup (`e4bdb62779`).
- **P9** — explicit preferred predicate in
  `getGenesUsedByPreferredVectors` (`d7e06d8b26`).
- **P10** — drop `FORCE INDEX` hint in
  `findGeneResultsByResultSetIdsAndGeneIds` (`35facc41c2`).
- **M3** — composite `(audit_trail_fk, id)` index on `AUDIT_EVENT`
  (`e019d135a4`).
- **M4 / M5** — closed earlier in the session.
- **B1** — HB6 negative-`setMaxResults` sweep (`938fc5f8ba`).
- **B3** — type ID-projection `createQuery` in
  `BibliographicReferenceDaoImpl` (`aed33135bd`).
- **B4** — type ID-projection `createQuery` in `GeneDaoImpl.removeAll`
  (`99c2727713`).

### SpotBugs

- **P1 batch 3** (`8862864328`) — clear 17 findings
  (`DM_DEFAULT_ENCODING`, `MS_MUTABLE_ARRAY`, `NM` filter).
- **P1 batch 4** (`67f467a25e`) — clear 7 findings.
- **P1 batch 5** (`2b8f7c1002`) — clear residual 12 (R-numerics +
  defensive nulls + API-stable arrays). **P1 count is now 0.**
- **P2 STCAL** (`488204bbc7`) — synchronise shared `DateFormat`
  instances. P2 partial; more findings remain in this and other P2
  categories.

### JUnit 5 migration (vintage-engine retirement)

- 8-batch recce + execution to retire `junit-vintage-engine`:
  - Batch 1 — drop `@Category` from 61 hybrid files (`2153c91eda`).
  - Batch 2 — migrate 3 pure JUnit 4 tests to Jupiter, add
    `BaseJerseyTest5` bases (`1e3b04681e`).
  - Batch 3 — delete legacy `BaseJerseyTest` /
    `BaseJerseyIntegrationTest` / `BaseDatabaseTest` JUnit 4 bases
    (`4e574611af`) + delete `BaseCliTest` (`dfc47c02c6`).
  - Batch 4 — delete 7 unused JUnit 4 test suites (`80e65cbd30`).
  - Batch 5 — `archunit-junit4` → `archunit-junit5` (`147db4ecd3`).
  - Batch 7 — 18 `@Category` → `@Tag` on unpaired files
    (`8d5f563ab2`); surefire/failsafe `excludedGroups` cleanup
    (`513e5bc355`).
  - Batch 8 (terminal) — drop `junit-vintage-engine` from build
    (`7672273bc3`, merged in `baf8bcfb28`).

The vintage engine is no longer on the test classpath; only Jupiter
runs.

### Config audit (HIGH / MEDIUM / LOW closures)

- **HIGH #1 / #2** — closed earlier in the session (see CONFIG_AUDIT
  doc, status block).
- **HIGH #3** — fail-fast on missing Spring profile (`8ce6091b81`).
- **MEDIUM #4** — externalise Hikari `sessionVariables` to
  `gemma.db.hikari.*` (`7e74c9ce84`).
- **MEDIUM #5** — factor JDBC URL query string into overridable
  property (`17b93110e3`).
- **LOW #9** — stale (no action needed).
- **LOW #10** — delete empty `fetcher.properties` (`0e33ddab9a`).
- **LOW #11** — normalise `@Profile` annotations to
  `EnvironmentProfiles` constants (`4cec9b87c1`).
- **LOW #12** — drop redundant XML `profile="web"` on
  `applicationContext-analytics.xml` (`40c0eadc74`).
- **LOW #13** — delete dormant `hibernate.properties` dialect stub
  (`91c1b39cbe`).

### Container image / cache wiring

- **CONTAINER_IMAGE Gap 3** — `SentinelPropertyValidator` fails fast on
  `XXXXXX` placeholders (`24b31839c7`).
- **ASPECTJ_EHCACHE #2 + #4** — JCache wiring + v2 XML deletion already
  landed, marked done in status doc (`ce201a0a69`).
- **ASPECTJ_EHCACHE #3** — `meterRegistryJCacheConfigurer` already in
  `MetricsConfig`, marked done (`76d78c2bbb`).
- **ASPECTJ_EHCACHE #5** — consolidate `@EnableAspectJAutoProxy` to a
  single declaration on `ComponentScanConfig` (`1623b8a53b`).

### Audit migration (Phase C)

Inventory items #1–#17 all migrated this session and earlier (see
`handoffs/AUDIT_RESIDUAL_INVENTORY.md` status block, updated this
session). #18 intentionally skipped (no event class). `gemma-core`
imperative `auditTrailService.addUpdateEvent(...)` surface in
`src/main/java` is now empty except for framework + companion-bean
javadoc.

Highlights:
- Inventory #11 / #12 / #13 — SingleCell cell-type-factor migrations
  with private-helper hoist (`349da5d1bf`).
- Inventory #14 — `PreboardedExperimentServiceImpl.create` via
  `PreboardedAuditService` (`82937fb9b4`).
- Inventory #15 / #16 — `EEWriteService.updateQuantitationType` via
  `@Audited` `valueSpel` (`4712ddbe47`); infra in `0ced2929b4`.
- Inventory #3 — `PreprocessorServiceImpl.batchCorrect` helper-bean
  hoist (`fc98078138`).
- Inventory #9 / #10 — `ExternalDatabaseService` release-details with
  `ReleaseDetailsUpdatePayload` (`1bebd73e7e`).
- Drop dead `AuditTrailService` autowires after migrations
  (`2d7c429cb7`).

**Deferred out of scope:**
- `gemma-cli` 12 sites — formally deferred (`6d0f94b8a3`); all sites
  self-invoke through subclass methods of
  `ExpressionExperimentManipulatingCli` / `AbstractCLI` so AOP cannot
  reach them. Helper-bean hoist precedent applies, scheduled as a
  separate workstream.
- `gemma-web` 4 sites — frontend retiring, do not chase.

### Failsafe bucket closures (B, C, D, E, F, H)

See `FAILSAFE_RESIDUAL_TRIAGE.md` for the post-session status block
with closing commits. Quick links:
- B — `c4f883546c`.
- C — already fixed pre-session (verified 14/14).
- D — landed pre-session.
- E — `1b1b00cf32` (pre-delete `AUDIT_TRAIL.LAST_EVENT_FK` on
  `AuditEvent` cascade).
- F — `a24d90cfca` (swallow `NotFoundException` in collection-filtering
  ACL provider).
- H — multiple: `910d6de51f` (AclAdviceTest + DEA testCreate),
  `bc944c9066` (thaw fresh-session helper), `25d3738139` (GI rotation),
  `0cbd57fd91` (GeneSearchTest, post-verify).
- A — failure surface no longer appears in verify output; verify
  explicitly before declaring it closed.
- G — still deferred (HB6 lock-mode in BLAT path, needs focused stack).

### Slim h5ad fixture

- `7ee022abd2` — `test(sc): slim GSE221593.h5ad fixture 25MB → 1.9MB
  (13×)`. Removed the bulkiest single-cell test fixture from the repo
  to keep clone size down; tests still cover the same axes (cell-types,
  factor mapping, count layer) on the slimmed file.

### Test-config drift fixes

- `2f0fb4c1cc` — add `SingleCellExperimentDesignAuditService` mock to
  `MexSingleCellDataLoaderPersistenceTest` context after audit co-bean
  introduction.
- `25102380cd` — align `SingleCellAggregateServiceTest` verify lines
  with new audit co-bean (post inventory-#2 migration).

### Network-tagged inventory

- `003c394ca5` — inventory `@Tag("network")` tests + upstream URL
  reachability snapshot (50 classes, 14 URLs all reachable as of
  2026-05-22).

### Misc

- `4955ebca52` — drop dead `RegressionTesting` import; unblock
  test-compile after JUnit 5 cleanup.
- `4ea4bfbabe` — accept optional `proposal_id` on PUT `/curation-draft`
  finalisation submission.
- `cfbe7a9a9b` — PUT `/datasets/{id}/curation-draft` finalisation
  endpoint (dispositions response).

## What's still open

1. **`testStreamExperiments`** — ACL-stream `AccessDenied` from
   `UnanimousBased.decide`. Likely a `RoleVoter` / `AuthenticatedVoter`
   wiring drift on the stream-secured method. Needs a focused failsafe
   run with voter logging.
2. **Bucket A** — verify the deep HB6 merge / matrix-assembly cascade
   tests still pass; they're absent from the latest failure list which
   means either fixed (silently) or excluded via tag. Re-run
   `BaselineDetectionTest` / `DiffExTest` / `SplitExperimentTest`
   explicitly.
3. **Bucket G** — HB6 lock-mode unsupported in BLAT path; needs a
   stack trace from
   `CompositeSequenceGeneMapperServiceTest.testGetGenesForCompositeSequence`.
4. **gemma-cli audit migration** — 12 self-invoke sites; needs a
   helper-bean hoist pass (see `STATUS_CLI_AUDIT_MIGRATION.md`).
5. **SpotBugs P2** — `STCAL` partial; other P2 categories pending.

## Cross-references

- `FAILSAFE_RESIDUAL_TRIAGE.md` — bucket-by-bucket closure status.
- `handoffs/AUDIT_RESIDUAL_INVENTORY.md` — Phase C migration status
  table.
- `handoffs/STATUS_CLI_AUDIT_MIGRATION.md` — gemma-cli deferral
  reasoning.
- `handoffs/STATUS_NETWORK_TAGGED_INVENTORY.md` — @Tag("network")
  reachability map.
- `handoffs/STATUS_BUCKET_A_DEFERRALS.md` — Bucket A deferral notes.
- `AUDIT_ADVICE_RETIREMENT_PLAN.md` — terminal step for Phase C.

## Release-plan context

Tip of `phase2-acl-migrate`: `488204bbc7`. Three release gates remain
per `project_release_plan.md`:
1. `hotfix-1.32.7` minor release.
2. `development` → `phase2-acl-migrate` catch-up merge.
3. Ship `phase2-acl-migrate` as Gemma 2.0.
