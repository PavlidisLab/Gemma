# Search Index Operations — Hibernate Search 7 / Lucene 9

**Audience:** Gemma operators running production / staging deployments.
**Companion doc:** [`SEARCH_RECCE.md`](./SEARCH_RECCE.md) (design rationale,
HS-5 → HS-7 port plan).
**Status:** Step 6 of the Phase-3 search restoration (initial reindex
procedure + smoke testing). Steps 1–5 (POM + bootstrap, annotation port,
`SearchSource` chain, `IndexerService` + CLI, REST re-wire) are merged.

This document is the operator's reference for building, rebuilding, and
recovering the on-disk Lucene index that backs `/search`, `/datasets?query=…`,
`/annotations/…`, and the `gemma-curation-ui` free-text widgets.

---

## 1. What the index is and where it lives

Gemma's free-text search is served by **Hibernate Search 7** writing one
Lucene 9 index directory per `@Indexed` entity root under
`${gemma.search.dir}`. The eight roots, as of Step 5:

```
${gemma.search.dir}/
  ExpressionExperiment/
  Gene/
  ArrayDesign/
  CompositeSequence/
  BioSequence/
  GeneSet/
  ExpressionExperimentSet/
  BibliographicReference/
```

`gemma.search.dir` is set in `Settings.properties` (or any of the
`default.properties` overrides) and defaults to
`${gemma.appdata.home}/searchIndices`. The `gemma.compass.dir` alias is
preserved for backward compatibility with operator scripts that referenced
the pre-Phase-2 Compass directory name.

Hibernate Search bootstrap in `HibernateConfig.java` wires four HS-7
properties on top of `gemma.search.dir`:

| Property | Value | Why |
|---|---|---|
| `hibernate.search.backend.type` | `lucene` | Pins the backend so a future ES jar on classpath can't silently flip the default. |
| `hibernate.search.backend.directory.type` | `local-filesystem` | Vs. heap / NIO mmap — Lucene indexes are designed for a real filesystem. |
| `hibernate.search.backend.directory.root` | `${gemma.search.dir}` | Index root (HS-7's successor to HS-5's `hibernate.search.default.indexBase`). |
| `hibernate.search.indexing.listeners.enabled` | `false` | **Manual reindex only.** No write-through indexing — Gemma's pre-strip pattern was CLI / cron-driven, not on-write. |
| `hibernate.search.schema_management.strategy` | `create-or-update` | HS-7 creates per-entity directories on first boot; safe to keep across mapping additions. |

> **Disk layout — not portable across HS major versions.** HS 7's Lucene 9
> segment format is **not backward-compatible** with HS 5.11's Lucene 5
> segments. Any pre-Phase-2 search-index content in `${gemma.search.dir}`
> must be deleted before the first HS-7 reindex runs. See section 4.

---

## 2. Building / rebuilding the index — `IndexGemmaCLI`

The canonical entry point is the `searchIndex` sub-command of `gemma-cli`,
implemented in `ubic.gemma.apps.IndexGemmaCLI`. It is **destructive**: each
selected entity's on-disk Lucene index is purged before the rebuild
(`MassIndexer.purgeAllOnStart(true)` in `IndexerServiceImpl`).

### 2.1 Invocation

After a fresh build, the appassembler-packaged CLI lands at
`gemma-cli/target/appassembler/bin/gemma-cli` (or under
`${GEMMA_CLI_PREFIX}/<ref>/bin/gemma-cli` on a deployed host — see
`gemma-cli/deploy.sh`):

```sh
# Full reindex (no entity flags → rebuild every @Indexed root).
gemma-cli searchIndex

# Targeted reindex: datasets + genes only.
gemma-cli searchIndex -e -g

# Override thread count (default 4; uses MassIndexer.threadsToLoadObjects).
gemma-cli searchIndex -threads 8
```

Entity option letters are kept stable with the pre-strip HS-5 CLI:

| Flag | Entity |
|---|---|
| `-e` | `ExpressionExperiment` (datasets) |
| `-g` | `Gene` |
| `-a` | `ArrayDesign` (platforms) |
| `-b` | `BibliographicReference` |
| `-s` | `CompositeSequence` (probes) |
| `-q` | `BioSequence` (sequences) |
| `-x` | `ExpressionExperimentSet` (dataset groups) |
| `-y` | `GeneSet` |
| _(none)_ | All of the above |

The CLI validates that `gemma.search.dir` is configured at startup and
aborts with a clear stderr message if it isn't.

### 2.2 What happens under the hood

Per `IndexerServiceImpl.index(Class)`:

```java
try ( Session session = sessionFactory.openSession() ) {
    SearchSession searchSession = Search.session( session );
    MassIndexer indexer = searchSession.massIndexer( classToIndex )
            .threadsToLoadObjects( numThreads )
            .batchSizeToLoadObjects( 25 )
            .idFetchSize( Integer.MIN_VALUE )   // MySQL streaming
            .mergeSegmentsOnFinish( true )
            .purgeAllOnStart( true );           // DESTRUCTIVE
    indexer.startAndWait();
}
```

Notable knobs:

- `openSession()` rather than `getCurrentSession()` — mass indexing manages
  its own connections + transactions and must not contaminate a
  request-scoped session.
- `idFetchSize(Integer.MIN_VALUE)` flips the MySQL JDBC driver into
  streaming mode, essential for `GENE` and `COMPOSITE_SEQUENCE` which carry
  millions of rows.
- `mergeSegmentsOnFinish(true)` squashes Lucene segments after the rebuild
  for faster subsequent queries.
- `startAndWait()` **blocks** the calling thread until indexing is complete.
  Run the CLI inside a maintenance window or under `nohup` / a job
  scheduler.

### 2.3 Expected runtime

Rough estimates from the pre-Phase-2 HS-5 era; HS-7 mass-indexer is
broadly comparable (same I/O profile, slightly better segment merge):

| Entity | Approx. row count (prod) | Estimated time @ 4 threads |
|---|---|---|
| `Gene` | ~5–10 M (across all taxa) | 30–90 min |
| `CompositeSequence` | ~20 M (probes across all platforms) | 2–6 hours |
| `ExpressionExperiment` | ~15 K | 5–15 min |
| `ArrayDesign` | ~3 K | 1–3 min |
| `BibliographicReference` | ~50 K | 5–10 min |
| `BioSequence` | ~20 M | 2–6 hours |
| `GeneSet` | ~100 K | 5–15 min |
| `ExpressionExperimentSet` | ~5 K | 1–3 min |

**Full-reindex budget: plan for an overnight window** (~8 hours wall time
with comfortable headroom). Probe + BioSequence dominate. Targeted partial
reindexes (e.g. just `-e -g`) are minutes, not hours.

Get a current row-count baseline before starting (replace `gemd` with the
running schema):

```sh
mysql -u<user> gemd -e "SELECT 'GENE', COUNT(*) FROM GENE UNION ALL \
    SELECT 'EE', COUNT(*) FROM INVESTIGATION WHERE class='ExpressionExperiment' UNION ALL \
    SELECT 'COMPOSITE_SEQUENCE', COUNT(*) FROM COMPOSITE_SEQUENCE UNION ALL \
    SELECT 'ARRAY_DESIGN', COUNT(*) FROM ARRAY_DESIGN UNION ALL \
    SELECT 'BIO_SEQUENCE', COUNT(*) FROM BIO_SEQUENCE UNION ALL \
    SELECT 'GENE_SET', COUNT(*) FROM GENE_SET UNION ALL \
    SELECT 'EE_SET', COUNT(*) FROM EXPRESSION_EXPERIMENT_SET UNION ALL \
    SELECT 'BIB_REF', COUNT(*) FROM BIBLIOGRAPHIC_REFERENCE;"
```

Cross-check the resulting Lucene doc counts via the HS-7 metadata API or
by inspecting `${gemma.search.dir}/<Entity>/_segments_*` after the run.

---

## 3. Downtime expectations during a reindex

Hibernate Search 7's mass-indexer with `purgeAllOnStart(true)` **wipes
the per-entity index before rebuilding**. Until the rebuild completes,
`SearchService.search(...)` returns **zero hits for that entity**.

What this means in practice:

| Operation in flight | Behaviour during reindex |
|---|---|
| `GET /search?query=X` (multi-type) | Returns hits for entity types whose index is still intact, empty for the type currently being rebuilt. |
| `GET /datasets?query=…` (single-type filter for EEs) | Returns empty until the `ExpressionExperiment` index is rebuilt. |
| `gemma-curation-ui` free-text widgets | Show "no results" for the in-progress entity. |
| Database queries (non-search) | Unaffected — search index ≠ source of truth. |

The pre-strip code had a maintenance-banner mechanism in
`IndexController` that's not yet restored (see SEARCH_RECCE.md
section 4 Step 4 — "low priority since the curation UI is the consumer of
record now"). Until that lands, communicate the reindex window out-of-band
(Slack / Confluence notice) so curators know free-text discovery will be
intermittently empty.

**Mitigation if a no-downtime reindex is needed**: do a **targeted**
reindex one entity at a time during off-hours, accepting that the
currently-rebuilding entity is search-empty for that entity's window only.
This trades a long maintenance window for several short windows.

---

## 4. Initial cutover from HS-5 → HS-7

The HS-7 Lucene 9 segment format is incompatible with HS-5's Lucene 5
segments. The cutover procedure for a host that ran pre-Phase-2 Gemma:

1. **Stop the Gemma webapp + any cron jobs that touch the search index.**
2. **Delete the legacy index directory wholesale**:
   ```sh
   # Be sure $GEMMA_SEARCH_DIR resolves to gemma.search.dir!
   rm -rf "$GEMMA_SEARCH_DIR"
   mkdir -p "$GEMMA_SEARCH_DIR"
   chown <gemma-app-user>:<group> "$GEMMA_SEARCH_DIR"
   ```
   The HS-7 SessionFactory bootstrap will re-create the per-entity
   sub-directories on first boot (`schema_management.strategy=create-or-update`).
3. **Deploy the Phase-3 Gemma artifacts** (webapp + CLI).
4. **Run the full mass-indexer** (section 2.1, no flags). Expect ~8 hours
   for the full set against current prod row counts.
5. **Smoke-test** (section 6) before re-opening the webapp.
6. **Restart the webapp** and verify `/search` round-trips a known query.

---

## 5. Recovery if the indexer fails mid-run

The mass-indexer's `purgeAllOnStart(true)` + `startAndWait()` shape means
that a failure mid-rebuild leaves the **target entity's index in a
partial state**: some documents present, some not. Recovery:

1. **Don't trust the partial index.** Re-running search against a
   half-built index returns inconsistent hit sets.
2. **Re-invoke the CLI for just the failed entity** — `purgeAllOnStart(true)`
   will wipe the partial state before the second attempt:
   ```sh
   gemma-cli searchIndex -e   # if EE was the failed entity
   ```
3. If the failure is reproducible, capture:
   - The CLI's stdout/stderr log (HS-7 mass-indexer logs progress at INFO
     periodically and errors at WARN/ERROR).
   - The Gemma application log around the failure window.
   - The DB row count for the entity (so we know the expected target).
4. Common failure modes:
   - **Out-of-memory** during a segment merge on a large index
     (`BioSequence`, `CompositeSequence`). Bump JVM `-Xmx`; lower
     `--threads` if memory-constrained.
   - **MySQL connection drop** mid-stream. Re-run; if reproducible,
     check the DB's `wait_timeout` / `net_read_timeout` against the
     entity's expected run-time.
   - **Disk-full** in `${gemma.search.dir}`. Lucene needs roughly 2x the
     final index size during merging. Provision accordingly.

A **filesystem snapshot of the working index** taken nightly is the
cheap insurance against catastrophic index corruption — restoration is
just a `rsync` back, faster than a full mass-reindex. Operators who
take regular snapshots should restore from the latest known-good
snapshot before re-running the mass-indexer.

---

## 6. Smoke-test after reindex

### 6.1 Manual smoke checks

Hit each REST endpoint that consumes `SearchService` and confirm at
least one known query returns the expected hit:

```sh
# Should find that specific EE (or 0 results if GSE2018 isn't loaded).
curl -s 'http://<host>/rest/v2/search?query=GSE2018&resultTypes=ExpressionExperiment' | jq

# Should find BRCA1 by official symbol.
curl -s 'http://<host>/rest/v2/search?query=BRCA1&resultTypes=Gene' | jq

# Free-text-over-ontology-tagged characteristics (covers the deep
# bioAssays.sampleUsed.characteristics.value embedded path).
curl -s 'http://<host>/rest/v2/search?query=parkinson&resultTypes=ExpressionExperiment' | jq
```

### 6.2 Automated smoke check — `MassIndexerSmokeIntegrationTest`

`gemma-core/src/test/java/ubic/gemma/core/search/MassIndexerSmokeIntegrationTest.java`
is the JUnit smoke test for the indexer + search round-trip. It:

1. Boots the full Gemma Spring context against `gemdtest`.
2. Persists a synthetic `ExpressionExperiment` + `Gene` with a unique random
   token in their names.
3. Invokes `IndexerService.index(...)` for both entity classes.
4. Issues `searchService.search(...)` with the unique token and asserts
   the fixtures appear in the result list.
5. Cleans up the fixtures.

**The IT is `@Ignore`'d by default.** It is destructive against the shared
`gemma.search.dir` (purges the per-entity index) and the `gemdtest` DB,
which conflicts with other parallel integration tests reading from the
same shared resources. Invoke it manually as part of the Step-6 cutover
validation against a non-prod host:

```sh
mvn -pl gemma-core failsafe:integration-test \
    -Dit.test=MassIndexerSmokeIntegrationTest \
    -DfailIfNoTests=false \
    -Dgemma.search.dir=/tmp/gemma-it-search
```

(Passing an isolated `gemma.search.dir` keeps the IT's destructive purge
out of any shared / production index location.)

Because `MassIndexerSmokeIntegrationTest` inherits `@Tag("integration")`
from `BaseIntegrationTest`, it is selected by `mvn verify` (Failsafe) and
excluded from `mvn test` (Surefire) — but the `@Ignore` annotation means
even the failsafe run skips it unless `-Dit.test=...` targets it
explicitly.

---

## 7. Scheduled / cron-driven reindex

**Not yet restored.** SEARCH_RECCE.md Step 4 names the deferred pieces:
`IndexerTask`, `IndexerTaskImpl`, `IndexerTaskCommand`, and the Quartz
scheduler wiring under `applicationContext-schedule.xml`. The pre-strip
HS-5 pattern was a midnight Quartz trigger that fired
`IndexerTaskImpl.execute(...)` against all `@Indexed` roots.

Until that lands, the reindex cadence is **operator-driven** — invoke
`gemma-cli searchIndex` from cron on the host itself:

```cron
# /etc/cron.d/gemma-reindex (example; adapt paths to deployment).
# Nightly full reindex at 02:00 local time.
0 2 * * * gemma /opt/gemma-cli/current/bin/gemma-cli searchIndex \
    >> /var/log/gemma/reindex.log 2>&1
```

When the in-app scheduler is restored, this section gets superseded by
the `applicationContext-schedule.xml` configuration (and the
operator-driven cron entry should be removed to avoid double-firing).

---

## 8. Open questions for production rollout

Carried forward from SEARCH_RECCE.md section 9, with Step-6 context:

1. **Acceptable initial-reindex downtime.** A full mass-reindex is ~8
   hours wall time. Overnight maintenance window is the safe call. If
   that's not acceptable, **targeted entity-by-entity reindex** (section
   3) trades one long window for several short ones.
2. **Reindex cadence in production.** Currently operator-driven (section
   7). When the Quartz scheduler comes back online, decide between
   nightly-full vs. incremental-on-write (which requires flipping
   `hibernate.search.indexing.listeners.enabled=true` — a separate
   write-path correctness audit).
3. **Snapshot strategy for `${gemma.search.dir}`.** Filesystem snapshots
   make recovery cheap (section 5). Decide whether to add the search
   index to the nightly backup rotation that already covers the DB.
4. **Disk provisioning headroom.** Lucene needs roughly 2x the final
   index size during merging. Confirm the volume hosting
   `${gemma.search.dir}` has the headroom before the first prod run.
5. **Search-result ranking acceptance criteria.** HS-7 scores normalize
   differently from HS-5's Lucene-5 scores. Pre-strip used a `0.9` static
   penalty on full-text scores vs. exact DB hits; the HS-7
   `HibernateSearchSource` preserves that penalty (`FULL_TEXT_SCORE_PENALTY = 0.9`),
   but ranking parity has not been quantitatively validated. A "for query
   X the top hit must be Y" regression suite is a deferred follow-up.

---

## 9. References

- `gemma-cli/src/main/java/ubic/gemma/apps/IndexGemmaCLI.java` — the CLI.
- `gemma-core/src/main/java/ubic/gemma/core/search/indexer/IndexerServiceImpl.java` —
  the mass-indexer driver.
- `gemma-core/src/main/java/ubic/gemma/persistence/hibernate/HibernateConfig.java`
  L182–226 — HS-7 bootstrap config.
- `gemma-core/src/main/java/ubic/gemma/core/search/source/HibernateSearchSource.java`
  — the search-side consumer of the index.
- `gemma-core/src/main/resources/default.properties` L20–24 —
  `gemma.search.dir` definition + compat aliases.
- `gemma-core/src/test/java/ubic/gemma/core/search/MassIndexerSmokeIntegrationTest.java`
  — the end-to-end smoke IT.
- [`SEARCH_RECCE.md`](./SEARCH_RECCE.md) — design rationale + HS-5 → HS-7
  migration plan.
