# Session snapshot — 2026-05-23

End-of-session snapshot of `phase2-acl-migrate`. Tip:
`0b5539eec2` — `Merge agent-fix-design-batch-thaw` (/design batched
BioMaterial source-chain thaw). 30 commits since the 05-22 round
opener `1b2e30755e`; 92 commits since the prior snapshot tip
`488204bbc7`. Branch is ~2378 commits ahead of `development`.

Today's session was the perf push that converted yesterday's recces
into shipped code, plus the curation-UI lifecycle endpoints and a
Rocky 9 / podman deploy scaffold for ops.

## Test signal

No fresh `mvn verify` was run today (heavy + serialized against
gemdtest, and many of the changes are HBM lazy flips that warrant a
dedicated run). Last known result (05-22 snapshot): **376 tests, 1F +
1E + 9S** — F = `testStreamExperiments` (defect #1 fixed today, defect
#2 still open), E = `GeneSearchTest.testSearchGenes` (cleared in
`0cbd57fd91`). A fresh run is overdue and should precede any release
gate.

## What landed this session (30 merges since 1b2e30755e)

The perf wave is the headline: vector retrieval, DEA retrieval,
visualization (heatmap + design), and HSearch result hydration all
got batched / lazy-flipped / hoisted. Curation API added the
finalisation lifecycle and public GEEQ. Ops side, `*_FILE` indirection
for secrets + Hikari pool tuning + Rocky 9 deploy scaffolding.

### Perf wave

| Area | Commit(s) | What changed |
|---|---|---|
| Vector HBM (processed) | `c646639fa9` | `ProcessedExpressionDataVector.bioAssayDimension`+`quantitationType` flipped to `lazy=proxy`; 4 hot loaders gain `JOIN FETCH` (~850 follow-up SELECTs per 54k-vector fetch eliminated) |
| Vector HBM (raw) | `1520096ae2` | Same flip for `RawExpressionDataVector` |
| Vector DAO (raw-and-processed) | `e994308f1f` | `LEFT JOIN FETCH bad+qt` added to 6 HQL fragments in `RawAndProcessedExpressionDataVectorDaoImpl`; closes the post-lazy-flip sweep |
| Vector dim batch | `3ce06a419e` | `getBioAssayDimensions(Collection)` batches dim fetch + `thawBioMaterialsForBioAssays` across all EEs (was per-EE source-chain walks) |
| DEA result-set facets | `13a4da6270` | `findByExperimentIds` prefetches `experimentalFactors`+`baselineGroup` per page (3 lazy loads per RS eliminated on `/analyses/differential`) |
| DEA thaw batching | `67fb79a103` | `thawAll(Collection<EARS>)` batches 6 single-valued chain inits via `JOIN FETCH`; 2 queries/page vs 5–7×N |
| DEA includeAssays default | `f4a416f4d0` | `/datasets/{id}/analyses/differential includeAssays` defaults to false; exposed as `@QueryParam` |
| DEA hit-list cache | `d09951c583` | `DiffExResultSetCountsCache` short-circuits `getHitListSizes` init (UIB §3, ~11s → warm sub-second) |
| DiffEx hoist | `4749697691` | `getDiffExVectors` hoisted out of per-EE loop in `getExpressionLevelsDiffEx` (N→1 calls; RS scope is fixed) |
| Heatmap factor map | `9c095a4578` | Precompute `fvId→bmId` for continuous-FV pass (was O(numFVs × numBAs)); swap `loadAndThawLite(id)` for `thawLite(ee)` |
| /design batch thaw | `9ca42c8aff` | `thawBioMaterialsForBioAssays` replaces per-BA loop; redundant per-EF `getFactorValues` init dropped. Cold-cache 15.8s → ~150ms projection (mirrors 352118e781 /samples win) |
| CurationDetails.lastXEvent | `b57d679e8e` | Three `last*Event` associations flipped to `lazy=proxy fetch=select`; 4 callsites null-tolerant |
| HSearch loadVO grouping | `a8a26229f6` | `SearchServiceImpl.loadValueObjects` groups by result type + dispatches one batched call per type (~65 → ~9 queries on 25-hit mixed page) |

Together these address every P1/P2 hotspot called out in the perf-priority memo.

### Curation API surface

- `7469d156a0` — Flyway **V15/V17**: `AgentProposal` lifecycle columns
  (STATUS / DISPOSITION / FINALIZED_AT).
- `422b30b74d` — flip curation-UI PATCH / finalize / reopen stubs to
  real handlers (lifecycle endpoints).
- `b5db2f02c7` + `a59fec4a3e` — cross-experiment proposal summary +
  inbox GET endpoints.
- `57025a9631` — **per-dataset routing fix**: curation routes 404 → 200;
  modern curation surface unhidden in OpenAPI.
- `cf09999f85` — `GET /datasets/{id}/geeq/public` (anon-readable
  per-factor breakdown; admin-override fields stripped). Powers the
  badge popover.
- `7f3d8f6af5` — heatmap `?subSet={id}` ACL-gated post-filter;
  classpath `@ExampleObject` wired.

### Deploy + ops

- `e70320bb2b` — `*_FILE` env-var indirection in
  `SettingsConfig.filterEnvironmentVariables` (Docker secrets +
  systemd-creds compatible).
- `456c2be231` — `deploy/` scaffold: Rocky Linux 9 + podman + Quadlet
  + Caddy sample.
- `d35697da9d` — wire `HikariDataSource` `setKeepaliveTime`,
  `setMaxLifetime`, `setConnectionTimeout`, `setIdleTimeout` from
  `gemma.db.hikari.*` properties; dev defaults in `default.properties`.
- `2fd5365dee` — `deploy/env.example` JDBC URL keepalive/timeout knobs
  + `deploy/README.md` tunnel keepalive section.
- `88819088ff` / `85e95600e3` / `8feae9f5ba` / `6cbca32028` — Java 25
  base image + Docker idempotency fixes. (Includes
  `JAVA_TOOL_OPTIONS preferIPv4Stack=true` via deploy env.)

### Misc

- `7af4fec9bb` — **SpotBugs P3 sweep**: 26 findings cleared in
  `gemma-core` + 2 in `gemma-cli` (UPM/DLS/WMI/UUF). Note: plugin needs
  `release=21` rebuild to read Java 25 bytecode.
- `493730ff8a` — test reshape for `testStreamExperiments` defect #1
  (persist-as-admin + `makeOwnedByUser` + `makePrivate` per
  `SecureValueObjectAuthorizationTest` pattern).
- `c09f155676` — empty-EE guard on `/datasets/{id}/design` TSV+QT
  branch (matches plain-TSV 404 shape; other 5 branches already safe).
- `c8bb03143c` / `b9eab16ab1` — **STATUS_SLIM_LIST_VO_NOT_NEEDED**
  (Path A): nominated endpoints don't load fat EE VOs; residual waste
  is the `JOIN FETCH lastXEvent` lines in `getFilteringQuery`.

## Open recces — verdicts

| Doc | State |
|---|---|
| `RECCE_DEA_RETRIEVAL_NPLUS1.md` | Items #2 and #3 shipped (`67fb79a103`, `13a4da6270`); **closed** |
| `RECCE_VECTOR_RETRIEVAL_NPLUS1.md` | **Closed** — `c646639fa9`, `1520096ae2`, `e994308f1f`, `3ce06a419e` cover the full sweep |
| `RECCE_PCA_SVD_NPLUS1.md` | In flight (parallel agent #149) |
| `RECCE_DESIGN_NPLUS1.md` | P1 + P3 shipped (`9ca42c8aff`); P2 deferred |
| `RECCE_HSEARCH_NPLUS1.md` | **Shipped** (`a8a26229f6`) |
| `RECCE_VISUALIZATION_PERF.md` | Heatmap side shipped (`9c095a4578`, `7f3d8f6af5`); PCA in flight under #149 |
| `RECCE_HEATMAP_REWRITE_CLIENT_SIDE.md` | Server side closed; UI work belongs to gemma-curation-ui |
| `RECCE_ACL_EXISTS_REFACTOR_FIXTURE.md` | Closure marker — refactor done; `AclSemanticsContractTest` is the gate |
| `STREAM_EXPERIMENTS_ACL_RECCE.md` | Defect #1 shipped (`493730ff8a`); defect #2 deferred (needs production session-lifecycle surgery, task #63) |
| `LOAD_DETAILS_COLD_PATH_RECCE.md` | Residuals audited; only eager `JOIN FETCH lastXEvent` lines remain (batched hydrator in flight as #144) |

## Open status docs — verdicts

| Doc | State |
|---|---|
| `STATUS_DIRECT_UPLOAD_SAMPLES_GAP.md` | Routed to CAB (upstream) |
| `STATUS_SAMPLES_HIBERNATE_SESSION_500.md` | **Closed** (dead tunnel + IPv4 pin) |
| `STATUS_HIBERNATE_SESSION_EXHAUSTION_AUDIT.md` | **Closed** |
| `STATUS_BROWSER_DATASETS_LIST_TIMEOUT_RECCE.md` | **Closed** (no regression) |
| `STATUS_DE_EXPRESSIONS_ENRICH_GAP_CLOSED.md` | **Closed** (`3829961887`, receipts in `ed12882835`) |
| `STATUS_DE_PVALUE_DISTRIBUTION_GAP_CLOSED.md` | **Closed** (`11ffc7464c`, receipts in `3f277b1c0b`) |
| `STATUS_CURATION_TO_GEMMA_2_0_PROBE_2_REPLY.md` | **Closed** |
| `STATUS_UNIFIED_JUSTIFICATION_SCHEMA_AUDIT.md` | **Closed** (Gemma side) |
| `STATUS_LOAD_DETAILS_COLD_PATH_AUDIT.md` | **Closed** |
| `STATUS_SLIM_LIST_VO_NOT_NEEDED.md` | **Closed** (Path A, `b9eab16ab1`) |

## Open pending tasks

- **#63** — `testStreamExperiments` defect #2 (production
  session-lifecycle surgery on `streamAll` `@Transactional` ResultSet
  path)
- **#89** — Slow-tagged failsafe sweep
- **#96** — Slow-sweep v2 with timeout, excluding
  `DatasetCombinerTest`
- **#144** (in flight at snapshot time) — batched `lastXEvent`
  hydrator to retire the eager `JOIN FETCH` lines in
  `getFilteringQuery`
- **#149** (in flight at snapshot time) — PCA / SVD N+1 recce

## What's next

1. **Run `mvn verify`** — perf wave touched HBM eager→lazy on three
   association points; need a green-bar run to confirm no test
   regressed and to refresh the 376/1F+1E baseline.
2. **Land #144 + #149** — finish the perf wave's tail (lastXEvent
   batched hydrator + PCA N+1). After these, the perf-priority memo's
   four hotspots are all addressed.
3. **Bucket A / Bucket G re-verify** — both deferred from the 05-22
   snapshot; need explicit re-runs of `BaselineDetectionTest`,
   `DiffExTest`, `SplitExperimentTest` (Bucket A) and
   `CompositeSequenceGeneMapperServiceTest.testGetGenesForCompositeSequence`
   (Bucket G HB6 lock-mode).
4. **gemma-cli audit migration** — 12 self-invoke sites still pending
   (helper-bean hoist workstream).
5. **Release gates** — once `mvn verify` is clean, the three gates in
   `project_release_plan.md` are unblocked: hotfix-1.32.7 minor,
   dev → phase2-acl-migrate catch-up merge, ship phase2-acl-migrate
   as Gemma 2.0.

## Delta after close (post-#157 verification)

Confirmed `mvn -pl gemma-core test -Dtest=GeneDaoTest` post-`3e9764df13`
(ACL test-context @Bean registration) and `94a16ddadf` (PCA hoist
merge):

- `IllegalStateException: AclQueryUtils.sessionFactory null` is GONE
  in test contexts wired through `BaseDatabaseTestContextConfiguration`.
  That was the root cause of the 53F+15E surefire signal earlier in
  the day; #157 closed it.
- `GeneDaoTest.testGetCompositeSequences` still errors, but with a
  DIFFERENT shape: `NoResultException: No result found for query
  [select id from acl_class where class = ?]`. The H2 fixture
  (`V9000__acl_contract_fixture.sql`) populates `acl_class` for the
  EE-side rows only — Gene (and likely CompositeSequence,
  ArrayDesign, BioMaterial) need rows too. Pre-existing fixture gap,
  not perf-wave regression. First task for next session.

Tip at session close: `94a16ddadf`.

## Cross-references

- `handoffs/STATUS_SESSION_SNAPSHOT_2026_05_22.md` — prior snapshot.
- `project_perf_hotspot_priorities.md` — the four hotspots the perf
  wave targeted.
- `project_release_plan.md` — the three release gates.
- `FAILSAFE_RESIDUAL_TRIAGE.md` — bucket-by-bucket closure status.
- `AUDIT_ADVICE_RETIREMENT_PLAN.md` — terminal step for Phase C.
