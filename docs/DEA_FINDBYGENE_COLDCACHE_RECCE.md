# DEA findByGene cold-cache — fix recce

Recce date: 2026-05-20. Baseline SHA: `5fd891344d`.
Source for cold/warm probe numbers: `PERF_PROBE_REPORT_ROUND3.md` §C1.
DAO under microscope: `DifferentialExpressionResultDaoImpl.findByGene(Gene, useGene2Cs, keepNonSpecificProbes)` line 322 — and the REST-UI sister method `findByGeneAndExperimentAnalyzed(...)` line 88 (which carries the StopWatch breakdown).

## 1. Executive summary

**Recommend Strategy A — periodic gene-list warm-up — as the first move.** It is the
cheapest, additive, leaves the schema untouched, and matches the observed
behaviour: the cold-warm delta on the round-3 probe (4.0 s → 0.55 s, 3.5 s gap)
is the InnoDB-page / Hibernate-region warm-up cost of three medium tables
(`DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT`, `CONTRAST_RESULT`, and the
gene's `GENE2CS` block), not a missing index. The probe says so explicitly —
"plan is fine; only the cold path bites". Strategy B (denormalised summary)
costs disk + a maintenance pipeline for ~5-10× speed-up vs already-warm path —
not worth it for a problem warm-up solves. Strategy C (index work) shows no
gap on EXPLAIN: the existing `probeResultSets (PROBE_FK, RESULT_SET_FK)`
composite covers the query.

Build cost for A: a `@Scheduled` job + a list of N (~100) gene IDs. ~half a
day. Maintenance: trivial. **However** there is a real second-order cost —
the `findByGene` REST path also does ~230 batched `Hibernate.initialize`
SELECTs on `getProbe()` / `getContrasts()` (probe-report line 124). Warming
the query cache does NOT warm the N+1-style hydration; only the InnoDB pages
behind those entities. That's fine — page-cache warm is what we want.

## 2. Current state — measurements

### 2.1 Table cardinality (live `gemd`, port-forward :8000)

| table | rows | data | index |
|---|---:|---:|---:|
| `DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT` | 1,557,791,219 | 120.5 GB | 163.9 GB |
| `CONTRAST_RESULT` | 2,697,895,723 | 349.0 GB | 186.4 GB |
| `COMPOSITE_SEQUENCE` | 14,420,347 | 1.2 GB | 1.9 GB |
| `GENE2CS` | 9,670,319 | 0.8 GB | 1.3 GB |
| `GENE` (distinct in GENE2CS) | 119,206 | — | — |

Total DEA+contrast hot footprint: ~820 GB. InnoDB buffer pool on the server
is `innodb_buffer_pool_size = 800 GB` — close to the working-set size but
not enough to hold both tables fully resident. Cold reads on a recently
restarted server will fault pages from disk; the warm path runs entirely
in-buffer.

### 2.2 Indexes on `DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT`

```
PRIMARY                                       (ID)
DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_RESULT_SET_FKC  (RESULT_SET_FK)
probeResultSets                               (PROBE_FK, RESULT_SET_FK)
DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_CORRECTED_PVALUE (CORRECTED_PVALUE)
```

For the gene-centric query, `probeResultSets` is the right index (composite
on `(PROBE_FK, RESULT_SET_FK)` — i.e. "results for this probe, grouped by
result-set"). EXPLAIN confirms it's chosen.

### 2.3 EXPLAIN for the `findByGene` shape

Translated HQL → SQL (TP53, gene_id=162841):

```sql
SELECT a.EXPERIMENT_ANALYZED_FK, dear.ID
FROM ANALYSIS a
JOIN ANALYSIS_RESULT_SET rs ON rs.ANALYSIS_FK = a.ID
JOIN DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear ON dear.RESULT_SET_FK = rs.ID
WHERE a.class='DifferentialExpressionAnalysis'
  AND dear.PROBE_FK IN (SELECT CS FROM GENE2CS WHERE GENE=162841)
GROUP BY a.EXPERIMENT_ANALYZED_FK, dear.ID;
```

EXPLAIN:

```
1 SIMPLE a            ref class                                 const           1   Using temporary; Using filesort
1 SIMPLE rs           ref EXPRESSION_ANALYSIS_RESULT_SET_ANALYSIS_FKC  gemd.a.ID 1   Using index
1 SIMPLE <subquery2>  ALL  NULL                                 NULL            NULL Block Nested Loop
1 SIMPLE dear         ref probeResultSets                       <sub2>.CS, rs.ID 1
2 MATERIALIZED GENE2CS ref GENE                                 const           440 Using index
```

440 probes for TP53 (GENE2CS), 29,138 result rows, 20,193 distinct result-sets.

Wall time against an already-warm prod buffer pool: **~0.3 s** for several
test genes (TP53, EGFR, ZNF583, OR4F5, TMEM89, FAM221A, C1orf127). All under
half a second. The buffer pool was clearly hot when probed — these are not
"cold-cache" measurements; they are a sanity check that the plan + indexes
themselves are healthy.

### 2.4 Hibernate cache config

| layer | DEA-result entity | findByGene query |
|---|---|---|
| L1 (session) | yes (default) | n/a |
| L2 entity cache | **NO** — `mutable="false"`, no `<cache>` element. Intentional. The HBM comment cites `HIBERNATE6_CASCADE_AUDIT.md` risk #3: a read-only L2 cache on a `mutable="false"` child of a `mutable="false"` parent caused HB6 to serve stale empty-bag results after cross-tx writes. Do NOT add it back without re-validating that audit. |
| L2 query cache | yes — `setCacheable(true)` is set on the findByGene query (line 338, 368). Caches `(query+params) → List<ID>`. |
| App-level `DiffExResultCache` | per-`(resultSet, gene)` cache used by `findGeneResultsByResultSetIdsAndGeneIds` (the heatmap path) — NOT by `findByGene`. |

Implication: the query cache returns a list of result IDs in ~ms on a hit,
but those IDs still have to be reloaded from MySQL — the entity itself is
not cached. So:
- query-cache miss + InnoDB-cold = full pay (multi-second)
- query-cache hit + InnoDB-warm = ~0.3-0.5 s (the warm probe number)
- query-cache miss + InnoDB-warm = ~0.5-1 s (one DB round-trip)

The 3.5 s gap in the round-3 probe is the InnoDB-cold path, not the query
cache. (The probe was issued against a freshly restarted JVM, but the MySQL
buffer pool would also have been recently churned by the parallel probe
agent — see PERF_PROBE_REPORT_ROUND3.md preamble.)

### 2.5 Cold-warm gap breakdown (where the 3.5 s lives)

Numbers from the probe report `findByGeneAndExperimentAnalyzed` StopWatch
breakdown (line 226-234 of the DAO), inferred for the cold path:

| phase | rows touched | typical cold | typical warm |
|---|---:|---:|---:|
| `getProbeIdsForGene` (GENE2CS) | ~440 | ~50 ms | ~5 ms |
| main `select dear,…` query | ~29k | ~2.0-2.5 s | ~0.30-0.40 s |
| `Hibernate.initialize(r.getProbe())` × 230 batches of 128 | ~29k probe rows | ~0.5-0.8 s | ~0.1 s |
| `Hibernate.initialize(r.getContrasts())` × 230 batches | ~60k contrast rows | ~1.0-1.5 s | ~0.15 s |
| total | | ~4.0 s | ~0.55 s |

Cross-check on contrast cost: a side query `SELECT COUNT(*) FROM CONTRAST_RESULT
WHERE DEAR_FK IN (29k TP53 dear IDs)` came back in **4.5 s** against a hot prod
buffer pool — and `findByGene` doesn't actually need contrasts. The
`findByGeneAndExperimentAnalyzed` path that the REST UI uses does. So the
contrast-initialization step is itself a sizeable chunk of the cold cost.

## 3. Strategy A — warm-up

### 3.1 Where the top-N gene list comes from

No `geneViewCount`, `pageView`, or analytics counter exists in
`gemma-core` today (`grep` is empty). Three viable sources, in order of build
cost:

1. **Hard-coded** — a static list of curator-favourite genes (TP53, BRCA1,
   TNF, MYC, EGFR, INS, BCL2, SOX2, PAX6, OLIG2, …). 50-100 genes,
   maintained in a properties file. **Cheapest. Ship this first.**
2. **Server-log mining** — scrape `nginx`/Tomcat access logs for
   `/gene/{id}` and `/api/v2/genes/{id}/differentialExpression` hits over the
   last N days. One-off script that emits the properties file.
   Refresh quarterly. Modest extra cost.
3. **In-process counter** — add a `Counter` bean that increments on every
   `findByGene` call, persisted to a small `GENE_VIEW_COUNT` table. Adaptive
   warm-up driven by real traffic. Higher cost, gives feedback loop.

### 3.2 Background job shape

`@Scheduled(fixedDelay = ...)` in a Spring `@Component`. On boot (delay 60s,
giving the Hibernate region time to wire up), iterate the gene list and
call `differentialExpressionResultService.findByGene(gene, true, false)`,
discarding the result. After that, re-fire every N hours to push back any
LRU eviction.

Risk: warm-up holds an open Hibernate session. Solution: a fresh session
per gene (or wrap each call in a `@Transactional` boundary). Risk: spikes
on warm-up start if N is large. Solution: stagger — one gene every 5s, so
100 genes takes 8 min and no single moment slams MySQL.

### 3.3 Memory cost estimate

Each warmed gene puts ~29k result-ID Longs in the Hibernate query-cache region
(query cache stores `List<Long>` of PKs). 29k × 8 bytes ≈ 230 KB per gene.
100 genes ≈ 23 MB. Negligible.

The bigger effect is **MySQL InnoDB page residency** — touching those
result rows + contrast rows brings their pages into the buffer pool and
keeps them warm via LRU as long as nothing else evicts them. That's the
actual win.

### 3.4 What it doesn't solve

A user clicking on a gene NOT in the top-N still pays the full cold cost.
For a curation tool this is acceptable — curators look at the same hot
genes repeatedly. For a public gene-detail page this would be a YELLOW
verdict at best.

## 4. Strategy B — denormalised summary

### 4.1 Schema sketch

```sql
CREATE TABLE GENE_DEA_RESULT_SUMMARY (
    GENE_FK              BIGINT NOT NULL,
    EXPERIMENT_ANALYZED_FK BIGINT NOT NULL,
    RESULT_SET_FK        BIGINT NOT NULL,
    DEAR_FK              BIGINT NOT NULL,
    PROBE_FK             BIGINT NOT NULL,
    PVALUE               DOUBLE,
    CORRECTED_PVALUE     DOUBLE,
    LOG_FOLD_CHANGE      DOUBLE,  -- representative contrast
    -- ...as many of the columns the gene-detail page actually needs
    PRIMARY KEY (GENE_FK, RESULT_SET_FK, DEAR_FK),
    INDEX gene_idx (GENE_FK, CORRECTED_PVALUE),
    INDEX dear_idx (DEAR_FK)
);
```

`findByGene(gene)` becomes a single `WHERE GENE_FK = ?` scan.

### 4.2 Cardinality estimate

If average gene has ~30k results (TP53's number) and there are ~120k
distinct genes in GENE2CS, upper bound is **~3.6 billion rows** — more than
double the current DEA result table. In practice most genes will have far
fewer (TP53 is in the extreme tail), so realistic estimate is closer to
**~1.5-2 billion rows**. Roughly the same size as the current DEA table or
larger, because the join expansion `(gene × probe × result-set)` is
super-linear when a gene maps to multiple probes per platform.

### 4.3 Maintenance shape

Three options:

1. **DB trigger on DEA result insert/delete** — couples the curation
   pipeline to a derived table. Brittle.
2. **Hibernate `@PostInsert` listener** — same problem in Java.
3. **Periodic batch rebuild** — a CLI tool that drops and rebuilds the
   summary table from scratch nightly. Simple, reliable, but the table is
   stale for up to 24h and the rebuild touches every row of the source
   table (CPU + IO cost = "redo the joins for every gene").

Plus partial-rebuild for newly-analysed experiments via a job that runs
after each DEA analysis lands.

### 4.4 Disk + write-amplification cost

Lower bound: similar to DEA result table — ~100-150 GB data + 100-200 GB
index. Doubles the storage of the slow path. Write-amp: every DEA result
insert (the curation pipeline writes millions during a `DEA` CLI run) gets
a multiplied write into the summary table proportional to the number of
genes a probe maps to (typically 1, but can be ~10 for promiscuous
probes).

### 4.5 Verdict on B

Real cost. Real complexity. Real maintenance surface. Worth it ONLY if
Strategy A doesn't move the needle for non-top-N gene queries AND the
gene-detail page is on the public web with end-user SLAs. For an internal
curation tool with a finite gene-of-interest list, A is sufficient.

## 5. Strategy C — index / query rewrite

### 5.1 The query

Already shown in §2.3. HQL is `select e, r from DifferentialExpressionAnalysis a join a.experimentAnalyzed e join a.resultSets rs join rs.results r where r.probe.id in :probeIds group by e, r`.

### 5.2 The plan

Already shown in §2.3. EXPLAIN does not show a fundamental problem:

- All four joined tables resolve via `ref` access (the cheapest non-PK
  access type).
- The covering composite `probeResultSets (PROBE_FK, RESULT_SET_FK)` is
  selected — exactly what this query wants.
- The only `Using temporary; Using filesort` is on the outer GROUP BY,
  and that's required by the query semantics ("dedupe by `(experiment,
  dear)`").

### 5.3 Index gap analysis — minor

Two micro-improvements possible:

1. **`(PROBE_FK, RESULT_SET_FK, ID, CORRECTED_PVALUE)`** — extending
   `probeResultSets` to be index-only-coverable. Saves the trip from the
   index leaf to the data page for each of the 29k matched rows. Disk:
   ~16 bytes × 1.55 B rows ≈ **~25 GB** added to an already 164 GB index.
   Probably not worth it for ~10-15% improvement on a query that runs in
   ~300 ms already when warm.
2. **`GROUP BY` could be removed** — the outer `group by e, r` exists to
   dedupe when multiple `rs` link to the same `dear` (it shouldn't happen
   given the schema's `RESULT_SET_FK` is on `dear`, so each `dear` belongs
   to exactly one `rs`). The `rs.results` join makes the relationship
   one-to-many in HQL though, which forces the dedup. A rewrite that drops
   the `group by` and writes directly via `rs.results` rather than going
   through `dea.resultSets.results` could shave a small constant.

Neither is the headline fix.

### 5.4 The real C-shaped opportunity — eager-fetch contrasts

The probe report calls out that `findByGeneAndExperimentAnalyzed` (the REST
path) does 230 batched `Hibernate.initialize(r.getContrasts())` calls
post-fetch. Each is a `WHERE DEAR_FK IN (?,?,...,?_128)` lookup. Replacing
that with a single `JOIN FETCH` on the main query — or one big batched
fetch keyed on the 29k `dear` IDs — collapses 230 round-trips to one. That
is a real Strategy C, but it's in a different DAO method (line 88, not
line 322) and probably wants its own recce.

For `findByGene` line 322 specifically: no Strategy C improvement is
clearly worth the cost.

## 6. Recommendation

**Strategy A.** Ship a hard-coded ~50-100 gene warm-up list driven by
`@Scheduled` on app start (60s delay) + every 6 hours. The warm-up calls
`findByGene` per gene, discarding the result; this seeds InnoDB pages
for the dear+contrast hot rows and the Hibernate query cache for those
gene queries.

Rationale: the cold-warm gap is page-cache + Hibernate-query-cache, not a
plan problem; warm-up addresses exactly that with minimal code, no schema
changes, no write-path side effects, and a Hibernate L2-cache-collision
risk of zero (we touch nothing that the HBM comment in
`DifferentialExpressionAnalysisResult.hbm.xml` rules out).

If Paul wants top-N driven by real traffic later, Strategy A.2 (server-log
mining) is an incremental upgrade on A.1, not a replacement.

The contrast-initialization N+1 in the REST `findByGeneAndExperimentAnalyzed`
path is a separate, additive optimisation — recommend tracking as a
distinct follow-up; ~3-4× speed-up on the REST cold path.

## 7. Open questions for Paul

1. **Is `findByGene` actually the gene-detail page hot path, or is it
   `findByGeneAndExperimentAnalyzed` (line 88)?** The probe report calls
   the line-88 variant "the variant used by the REST UI". If so, the
   warm-up should call line-88 with `experimentAnalyzedIds = all current
   EE IDs` rather than line 322 — different cache key.
2. **Top-N gene list — do you want curator-favourites hard-coded, or
   should we wire up server-log mining first?** A.1 ships in half a day;
   A.2 needs an offline script plus a sanity loop.
3. **Are you OK with a `@Scheduled` job that holds a `@Transactional`
   read session for ~30s on app boot?** It will inflate the boot-time
   memory profile slightly (29k Longs × 100 genes ≈ 23 MB in the query
   cache region). Trivial in absolute terms, but worth confirming if the
   prod heap is tight.
