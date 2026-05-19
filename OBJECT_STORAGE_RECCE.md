# Object-Storage Abstraction — Reconnaissance

**Status:** recce only. No production code touched.
**Scope:** Phase 3, cloud-readiness — **BONUS work, not required**.
**Reference:** PHASE_3_VISION.md L133-135 (object storage); CONFIG_AUDIT.md
HIGH#2 (`gemma.appdata.home` default now `${java.io.tmpdir}/gemmaData`).

---

## 0. Why object storage at all? (read this first)

**Gemma today** runs on a single server (`homer.msl.ubc.ca`) with files on
local disk under `${gemma.appdata.home}`. Backups via filesystem snapshots
/ rsync. No horizontal scaling. **For this deployment, object storage is
unnecessary.** The HDF5 / Lucene / Jena "local-path" problem only exists
when you try to back those engines onto S3 — keep files on the same
machine as the JVM and they just open local paths. Done.

Object storage (S3 / GCS / MinIO) buys you four things, none of which
Gemma currently needs:

1. **Multi-replica deployment** — N copies of `gemma-rest` behind a load
   balancer all see the same files. Today: one replica, irrelevant.
2. **Container persistence across restarts** — ephemeral container disk
   gets wiped; object storage outlives it. Today: bare-metal JVM, not a
   concern.
3. **Independent backup / DR** — object stores already replicated. Today:
   filesystem snapshots cover this.
4. **Compute / data separation** — scale compute up/down without moving
   data around. Today: not scaling.

So **this whole doc is "what to do IF deployment changes"**, not a Phase 3
priority. The one piece that DID matter — env-var configurability of
`gemma.appdata.home` so containers can mount a path from outside the
image — already landed (`23be090ba2`). That's enough for the current
deployment to be container-aware without object storage in the mix at
all.

**If/when Gemma needs to move to cloud or run multiple replicas**, the
inventory + phased plan below is the starting point. Until then, file it
as planning material.

---

## 1. Inventory of file I/O against `gemma.appdata.home`

### 1.1 How the appdata tree is wired

`default.properties` (`gemma-core/src/main/resources/default.properties`)
defines one root and a dozen derived sub-roots:

| Property | Default | Used by |
|---|---|---|
| `gemma.appdata.home` | `${java.io.tmpdir}/gemmaData` | everything |
| `gemma.download.path` | `${gemma.appdata.home}/download` | GEO / NCBI / ArrayExpress fetchers |
| `gemma.analysis.dir` | `${gemma.appdata.home}/analysis` | analysis temp + outputs |
| `gemma.search.dir` | `${gemma.appdata.home}/searchIndices` | Compass/Lucene indices |
| `gemma.fastq.headers.dir` | `${gemma.appdata.home}/fastqHeaders` | per-GSM FASTQ headers |
| `gemma.cache.dir` | `${gemma.appdata.home}/cache` | ontology / generic caches |
| `gemma.gene2cs.path` | `${gemma.appdata.home}/DBReports/gene2cs.info` | serialized gene→CS map |
| `gemma.scratch.dir` | `${gemma.appdata.home}/scratch` | scratch / temp work |
| `gemma.ontology.dir` | `${gemma.appdata.home}/ontology` | OWL sources + Jena TDB |
| `gemma.ontology.unified.tdb.dir` | `${gemma.ontology.dir}/tdb` | Apache Jena triple store |
| `gemma.cellBrowser.dir` | `${gemma.appdata.home}/cellBrowser` | exported cell browser |
| `tgfvo.path` | `${gemma.ontology.dir}/TGFVO.OWL.gz` | trans-genus factor ontology |
| `gemma.log.dir` | `.` | runtime logs |

Additional derived sub-roots inside `ExpressionDataFileServiceImpl`:
`${gemma.appdata.home}/metadata` (per-EE metadata, locked) and
`${gemma.appdata.home}/dataFiles` (generated flat-file exports).
`ArrayDesignReportServiceImpl` uses `appdataHome + "/arrayDesignReports"`
(serialized Java objects).

### 1.2 Raw I/O site counts (read directly from the source tree)

| Probe | Count |
|---|---|
| `@Value("${gemma.appdata.home}...")` injections | 11 |
| `Settings.getString("gemma.appdata.home")` direct lookups | 2 |
| Raw stream open (`new FileInput/OutputStream`, `Files.newInput/OutputStream`, `Files.write`, etc.) | 60 |
| All `Files.*` filesystem calls (read/write/exists/copy/move/delete/createDirectories) | 169 |
| Distinct source files touching `Files.*` | 52 |

Searches used (re-runnable):
```
grep -rn "gemma.appdata.home\|getAppdataDir\|appdataHome" gemma-{core,rest,cli}/src/main/java
grep -rn "new FileOutputStream\|Files.write\|Files.newOutputStream\|new FileInputStream\|Files.newInputStream\|Files.readAllBytes" gemma-{core,rest,cli}/src/main/java
grep -rn "Files\.\(write\|newOutputStream\|newInputStream\|readAllBytes\|copy\|move\|delete\|createDirectories\|exists\|size\|list\|walk\)" gemma-{core,rest,cli}/src/main/java
```

### 1.3 Categorization

| Cat. | Description | Example call sites | Typical size | R/W pattern |
|---|---|---|---|---|
| **A. Bulk artifacts** | large blobs, write-once / read-many. Generated user-facing exports + downloaded source data. | `ExpressionDataFileServiceImpl` (metadata + dataFiles, locked); `DatabaseViewGeneratorImpl` (gzipped TSV reports); `MexMatrixWriter` (`matrix.mtx.gz`, `features.tsv.gz`, `barcodes.tsv.gz` per EE); `ArrayDesignAnnotationServiceImpl` (`microAnnots`); `GeoFetcher` / `MexDetector` (downloaded GEO archives, `Files.newOutputStream(dest)`); `RawExpressionDataWriterCli`, `SingleCellDataWriterCli`, `DifferentialExpressionAnalysisWriterCli`, `ExpressionDataMatrixWriterCLI`, `CellLevelMetadataWriterCli` (all gz-streaming CLI exporters); HDF5 `.h5ad` reads via `AnnData.open(Path)` → native `H5File`. | 10 MB - 50 GB (single-cell .h5ad) | append-once via `OutputStream`; subsequent reads either stream or random-access (HDF5) |
| **B. Logs / job state** | write-many, read-rare. Scheduler bookkeeping, lock files, progress trackers. | `FileLockManagerImpl` (per-path advisory locks); `WhatsNewServiceImpl` (`newObjects` / `updatedObjects` serialized `ObjectOutputStream`); `GeoBrowserServiceImpl` (`getInfoStoreFile()`, serialized Java); `TableMaintenanceUtilImpl` (`gene2cs.info`, `ObjectOutputStream`). | < 1 MB | overwrite per run, occasionally locked |
| **C. Caches (re-derivable)** | rebuilt if absent. | `ArrayDesignReportServiceImpl` (serialized `ArrayDesignReport` objects); `OntologyConfig` (Jena TDB on disk); `basecode.ontology.cache.dir`; downloaded NCBI / homologene / GO source files under `${gemma.download.path}/databases/...`; parsed GEO family files; ontology OWL sources. | < 100 MB usually | atomic replace |
| **D. User uploads / config** | metadata curators / batch jobs provide as input. | `ExperimentalDesignImportCli` (`Files.newInputStream(experimentalDesignFile)`); `ArrayDesignAnnotationFileCli`, `ArrayDesignProbeRenamerCli`, `BioSequenceCleanupCli`, `LoadSimpleExpressionDataCli`, `SingleCellDataTransformCli` (writes a `requirements.txt`); plus the CLI `DataFileOptionsUtils` flag flow. | KB-MB | one-shot read or write |

### 1.4 Critical incompatibilities (footguns for an S3 swap)

1. **HDF5 / Jena TDB / Lucene need a real local path.** `AnnData.open(Path)`
   delegates to a native HDF5 library; Jena TDB and Compass/Lucene indices
   mmap files. S3 cannot back these directly — either FUSE-mount, stage to
   local scratch, or keep these on a local volume. Plan must accommodate.
2. **`FileLockManager` is advisory + filesystem-specific.** All bulk-artifact
   producers already go through it. Any S3 backend needs a stand-in
   (per-key conditional writes / object-version preconditions, or a
   metadata table) — see §3.4.
3. **`gemma.scratch.dir` is sometimes used as a hand-off** to R / Python
   sub-processes. Sub-processes can't read `s3://` URIs without an SDK; a
   stage-to-local-scratch helper is required.

---

## 2. API options

| Option | Dep weight (transitive) | Vendor lock-in | On-prem (Ceph / MinIO) | Cloud (S3 / GCS) | Spring integration | Notes |
|---|---|---|---|---|---|---|
| **Spring `Resource` + custom backends** | none new (Spring is already on cp) | none | yes (write a `Resource` impl) | yes | native | Spring's `Resource` is read-leaning (`getInputStream`). Writes need `WritableResource`. Forces us to write the S3 protocol code by hand — minimal deps, maximum work. |
| **Apache Commons VFS (2.x)** | ~1.5 MB; brings `commons-vfs2-sandbox` for S3 | none | yes (S3 via sandbox or hdfs provider) | partial (S3 only, GCS unofficial) | none | Old (last major release 2.9, late 2022). Loose maintenance. Useful glue but S3 support is sandbox-quality. |
| **AWS SDK v2 (`software.amazon.awssdk:s3`)** | ~10 MB (core + s3 + netty client) | strong: S3 + AWS auth model, but works against any S3-compatible | yes (signature v4 against MinIO / Ceph RGW) | best-in-class for S3; GCS via the GCS-S3-compat layer | none (BYO config) | Async-first, non-blocking. Full feature coverage (multipart, signed URLs, lifecycle ops). Largest ecosystem and the de-facto industry standard. |
| **MinIO Java SDK** | ~3 MB | low: pure S3 API surface | yes (its native target) | yes (works on real AWS S3) | none | Lighter than AWS SDK v2. Blocking API. Smaller feature set: no per-request retry policies, lifecycle, transfer manager — fine for our shape (read/write/list/delete/exists) but limiting for advanced use. |
| **Spring Cloud AWS (`io.awspring.cloud:spring-cloud-aws-s3`)** | ~12 MB (pulls in AWS SDK v2 + Spring Cloud context) | medium: Spring Cloud AWS-specific config keys, but the underlying SDK is v2 | yes (via `endpoint-override`) | yes | best-in-class — provides `S3Resource` (a Spring `Resource` impl), `S3Template`, `@Value("s3://bucket/key")` resolution | Layered on AWS SDK v2 so the SDK is still pluggable. Higher abstraction → faster integration but more magic. |

### 2.1 Recommendation

**AWS SDK v2 (`software.amazon.awssdk:s3`)** behind a thin
`BlobStorageService` interface that Gemma owns.

Reasons:
- Works on real S3, MinIO, Ceph RGW, and Google Cloud Storage's
  S3-compatibility endpoint. Single dependency covers every realistic
  deployment target.
- Doesn't drag in Spring Cloud or Spring Boot auto-config. Our Spring
  surface is XML + javaconfig; Spring Cloud AWS expects Spring Boot.
- Async client is available when we need it (Phase 4+); blocking
  `S3Client` is fine for Phase A.
- Our interface stays narrow (read/write/exists/list/delete) so the SDK
  choice is reversible.

Fallback if dep budget tightens: **MinIO Java SDK**, identical wire
protocol, ~7 MB lighter, works everywhere AWS SDK does. Switch is
mechanical (both expose the same S3 verbs).

Reject: Commons VFS (stagnant + sandbox-only S3); Spring Cloud AWS
(magnetic to Spring Boot we don't run).

---

## 3. Migration shape

### 3.1 Proposed interface

```java
package ubic.gemma.core.storage;

public interface BlobStorageService {
    /** Open a stream for reading. Caller closes. */
    InputStream read(URI uri) throws IOException;

    /** Open a stream for writing. Atomically visible on close (best-effort
     *  per backend). Caller closes. */
    OutputStream write(URI uri) throws IOException;

    /** Convenience: stream upload of a known-size payload. */
    void write(URI uri, InputStream src, long length) throws IOException;

    boolean exists(URI uri) throws IOException;
    void delete(URI uri) throws IOException;

    /** List immediate children (prefix-style for S3, directory listing for
     *  filesystem). */
    Stream<URI> list(URI uri) throws IOException;

    /** Stage a remote blob to a local Path. Required for HDF5 / Jena TDB /
     *  Lucene / sub-process inputs. Returns a handle that deletes the
     *  staged file on close. */
    StagedBlob stage(URI uri) throws IOException;

    /** Optional metadata: size, last-modified, checksum. */
    BlobMetadata stat(URI uri) throws IOException;
}
```

### 3.2 URI scheme

- `gemma://localdata/...` resolves to `<gemma.appdata.home>/...` —
  default, preserves all existing layouts.
- `s3://bucket/key` resolves to S3 via AWS SDK v2.
- `file:///abs/path` passthrough for absolute local paths (existing
  fetcher / sub-process semantics).

The interface accepts `URI`, not `Path`, so call sites stop building
`File` / `Path` directly. A small `BlobUris` utility produces canonical
URIs for the legacy sub-trees (`forMetadata(ee)`, `forDataFile(ee, qt)`,
etc.).

### 3.3 Implementations (Phase A + C)

| Class | Backend | Phase | Notes |
|---|---|---|---|
| `FilesystemBlobStorageService` | local FS, rooted at `gemma.appdata.home` | A | wraps existing `FileLockManager` for `gemma://localdata/...` writes |
| `S3BlobStorageService` | AWS SDK v2 | C | configured by `gemma.storage.s3.endpoint`, `region`, `bucket`, `accessKey`, `secretKey` (resolved via env / keychain per global CLAUDE.md). Streaming uploads use the SDK's multipart transfer when payload size > 8 MB. |

### 3.4 Locking semantics

`FileLockManager` (`gemma-core/src/main/java/ubic/gemma/core/util/locking/FileLockManager.java`)
is `Path`-scoped. Filesystem backend keeps it; S3 backend implements
"at-most-one writer" via:

- preconditioned `PutObject` (`If-None-Match: *`) for the create case;
- object-version preconditions for the modify case;
- a sidecar `<key>.lock` object holding lease metadata + a TTL, with
  ownership scoped per `gemma.storage.instance.id`.

Phase A defines `BlobLock` mirroring `LockedPath` and the filesystem
impl delegates to today's `FileLockManager`. S3 impl ships in Phase C.

### 3.5 Sub-process / native-lib helpers

`stage(URI)` returns a `StagedBlob` carrying a local `Path` whose
lifetime is bounded by `try-with-resources`. Native consumers (HDF5,
R/Python sub-processes, Jena TDB, Lucene) call `stage()` and pass the
local path. For write-back, a paired `Stager.upload(localPath, URI)`
finalizes the artifact.

---

## 4. DB schema impact

Audit of `gemma-core/src/main/resources/db/migration/mysql/V1__prod_baseline.sql`
(prod baseline, 2313 LoC) plus all `*.hbm.xml` Hibernate mappings:

- **No entity column stores a filesystem path** today.
- All `*_URI` columns (`FULL_TEXT_URI`, `TERM_URI`, `CATEGORY_URI`,
  `VALUE_URI`, etc., across `CHARACTERISTIC`, `EE2C`,
  `EXTERNAL_DATABASE`, `Characteristic.hbm.xml`) hold ontology URIs, not
  filesystem URIs.
- `EXTERNAL_DATABASE.WEB_URI` and `.FTP_URI` are external lookup
  endpoints, unrelated to Gemma-owned storage.
- The only Hibernate property whose name suggests a path is the
  ontology-term `uri` on `Characteristic`.

**Implication:** the migration is **pure-code**; no Flyway migration
needs to retrofit a `BLOB_URI` column onto existing tables. File
locations are derived from `${gemma.appdata.home}` + entity IDs by
service code, not persisted. The recommended `gemma://localdata/...`
URIs likewise can be derived on demand from existing entity state.

**Future-looking (out of scope for Phase A-D):** when a single artifact
needs to live in multiple storage backends concurrently (e.g.,
filesystem cache + canonical S3), introduce a new `BLOB_REGISTRY` table
keyed by `(entity_type, entity_id, kind)` → canonical URI + checksum +
size + last-modified. Flyway-migratable on its own without disturbing
existing entity columns.

---

## 5. Phased plan

### Phase A — Interface + filesystem implementation (zero behavior change)

**Scope:** introduce `BlobStorageService`, `BlobUris`, `BlobMetadata`,
`StagedBlob`, `FilesystemBlobStorageService`, and a Spring wiring config
(`BlobStorageConfig`). No call sites converted yet. The filesystem impl
delegates all I/O to today's `FileLockManager` + `Files.*` calls so the
on-disk behavior is bit-identical.

**Files added (estimate):**
- `gemma-core/src/main/java/ubic/gemma/core/storage/BlobStorageService.java` (~80 LoC interface)
- `BlobUris.java` (~120 LoC; static helpers for the legacy sub-trees)
- `BlobMetadata.java`, `StagedBlob.java`, `BlobLock.java` (~150 LoC together)
- `FilesystemBlobStorageService.java` (~250 LoC)
- `BlobStorageConfig.java` (~60 LoC Spring `@Configuration`)
- unit tests: `FilesystemBlobStorageServiceTest.java` (~250 LoC)

**Total: ~900 LoC added, 0 LoC modified.** Risk: low (additive only).
Validation: unit tests + `mvn verify`.

### Phase B — Convert 2-3 hot call sites

**Targets** (chosen because they're heavy and self-contained):
1. `ArrayDesignReportServiceImpl` (8 call sites, single responsibility).
2. `DatabaseViewGeneratorImpl` (3 gzipped TSV writes, simple stream).
3. `WhatsNewServiceImpl`'s `newObjects` / `updatedObjects` serialized
   `ObjectOutputStream` files (2 writes + 1 read).

**Files modified (estimate):** 3-4 files, ~150-250 LoC delta. Risk: low
— same backend, only the call shape changes. Validation: existing unit
tests + targeted integration tests against `gemdtest` (per project
memory, single-tenant: serialize agents).

### Phase C — Add S3BlobStorageService behind a profile

**Scope:**
- Add `software.amazon.awssdk:s3` dep + BOM pin in `pom.xml`.
- Implement `S3BlobStorageService` (~600 LoC) including multipart
  uploads, `If-None-Match` preconditioned puts, and prefix listing.
- Implement S3 variant of `BlobLock` (sidecar-object lease, TTL
  refresh).
- Spring profile `storage-s3` plus config keys: `gemma.storage.backend`
  (`filesystem` | `s3`), `gemma.storage.s3.{endpoint,region,bucket,
  accessKeyEnv,secretKeyEnv}` (env-var-resolved per global CLAUDE.md).
- Integration test harness: testcontainers MinIO image (~100 LoC of
  test infra).

**Files added (estimate):** ~1000 LoC production + ~600 LoC tests.
Risk: medium — first production code that talks to a remote service.
Validation: testcontainers MinIO, plus a manual smoke against a real
MinIO/Ceph cluster.

### Phase D — Full migration of remaining call sites

**Scope:** sweep the 52 distinct source files that call `Files.*` (per
§1.2). For each: replace direct path I/O with `BlobStorageService`,
using `stage()` where native libs / sub-processes are involved.

Sub-batches:
- D1: `ExpressionDataFileServiceImpl` + `ExpressionMetadataChangelogFileServiceImpl` (hottest path, locked, EE-scoped).
- D2: GEO / NCBI / ArrayExpress fetchers (`SimpleDownloader`, `GeoFetcher`, `MexDetector`, `HttpFetcher`, `AbstractSingleFileInSeriesSingleCellDetector`).
- D3: CLI writers (`RawExpressionDataWriterCli`, `SingleCellDataWriterCli`, `DifferentialExpressionAnalysisWriterCli`, `ExpressionDataMatrixWriterCLI`, `CellLevelMetadataWriterCli`, `ExperimentalDesignImportCli`, `LoadSimpleExpressionDataCli`).
- D4: Ontology / Jena TDB / search indices — these likely **stay on local FS** behind a `BlobStorageService` adapter that pins the URI to a known scratch path; document the constraint.
- D5: `TableMaintenanceUtilImpl` gene2cs serialized blob; `GeoBrowserServiceImpl` info-store.

**Files modified (estimate):** ~50 files, ~2-3 KLoC delta. Risk:
medium-high — this is the production hot path. Validation: full
`mvn verify` + targeted `gemdtest` integration runs per sub-batch +
manual end-to-end on a staging deployment.

### Phase-summary table

| Phase | LoC delta | Files touched | Risk | Validation |
|---|---|---|---|---|
| A | +900 / -0 | 0 modified, 7 new | low | unit + `mvn verify` |
| B | +250 / -180 (net +70) | 3-4 modified | low | unit + targeted gemdtest |
| C | +1600 / -0 | 4 new + 1 pom edit | medium | testcontainers MinIO + real-cluster smoke |
| D | +3000 / -2500 (net +500) | ~50 modified | medium-high | full verify + staged sub-batches |

---

## 6. Out of scope (explicitly)

- **Migrating existing on-disk data** to S3. Phase D will read either
  backend transparently; bulk migration is a one-off ops task scheduled
  after Phase D lands.
- **CDN integration** for user-facing artifact downloads. Distinct
  concern; can layer on top of the `BlobStorageService` later.
- **Cross-region replication** / disaster recovery. Backend concern,
  out of code scope.
- **Lucene / Jena TDB / HDF5 remote backends.** These native consumers
  stay pinned to local scratch behind a `stage()` veneer (§3.5, Phase
  D4). Migrating *them* to a true remote backend is a separate
  initiative (requires either FUSE, an alternative index format, or
  per-library remote support — none are free).
- **Per-tenant bucket sharding.** Single bucket + key prefix is fine
  for Phase A-D. Multi-tenant sharding is a Phase 4 concern.
- **Retroactive provenance stamping** of existing artifacts. New
  writes through `BlobStorageService` can optionally emit
  `*_meta.json` sidecars; back-filling old artifacts is ops work.

---

## Appendix A — re-runnable inventory commands

```bash
# appdata-home injection points
grep -rn "gemma.appdata.home\|getAppdataDir\|appdataHome" \
    gemma-{core,rest,cli}/src/main/java

# raw stream open sites
grep -rn "new FileOutputStream\|Files.write\|Files.newOutputStream\|new FileInputStream\|Files.newInputStream\|Files.readAllBytes" \
    gemma-{core,rest,cli}/src/main/java

# all NIO Files.* call sites
grep -rn "Files\.\(write\|newOutputStream\|newInputStream\|readAllBytes\|copy\|move\|delete\|createDirectories\|exists\|size\|list\|walk\)" \
    gemma-{core,rest,cli}/src/main/java

# directory-shape properties
grep -rnE "gemma\..*(\.dir|\.path|\.home|\.file)" gemma-core/src/main/resources

# DB schema column-name audit (no path columns expected)
grep -iE "LOCAL|PATH|URI|FILE_NAME|FILENAME" \
    gemma-core/src/main/resources/db/migration/mysql/V1__prod_baseline.sql
```

## Appendix B — references

- `gemma-core/src/main/java/ubic/gemma/core/util/locking/FileLockManager.java`
  (existing `Path`-scoped streaming + locking facade; the model for
  `BlobStorageService`).
- `gemma-core/src/main/java/ubic/gemma/core/analysis/service/ExpressionDataFileServiceImpl.java`
  (largest single consumer of appdata; lock-aware).
- `gemma-core/src/main/resources/default.properties` (`gemma.appdata.*` tree).
- `PHASE_3_VISION.md` L133-135 (object-storage goal).
- `CONFIG_AUDIT.md` HIGH#2 (appdata-home defaulting).
