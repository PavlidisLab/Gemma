# Pipelines + scheduler architecture recce

Baseline: 36940467cc · Branch: pipelines-and-scheduler-recce · Date: 2026-05-20
Sources read: ~12 Confluence pages, `rnaseq-pipeline` repo top-level, `sc-annotation-pipeline` repo top-level, Gemma `gemma-core/job/*`, `gemma-rest/TasksWebService.java`, `gemma-rest/DatasetsWebService.java` dispatch endpoints, `SchedulerConfig.java`.

## 1. Executive summary

Today, a Gemma curator's day is split between a browser tab (Gemma UI for ED / tags / outliers) and an SSH terminal on `lisa` where they invoke `gemma-cli` for ~20 distinct subcommands (`addGEOData`, `downloadSingleCellData`, `loadSingleCellData`, `aggregateSingleCellData`, `diffExAnalyze`, `rnaseqDataAdd`, `addMetadataFile`, `affyFromCel`, ...). Bulk RNA-seq goes a step further: curators run a Luigi pipeline (`rnaseq-pipeline-cli run rnaseq_pipeline.tasks.SubmitExperimentToGemma --experiment-id GSEXXX --workers 30`) whose state lives in a Google Sheet (`experiment_id`, `priority`, `data`, `batch_info`) and a Luigi daemon at `localhost:8082`. Cell-typing already migrated: `sc-annotation-pipeline` is nf-core-shaped DSL2 Nextflow on Slurm, driven by a Jenkins button. Three threads converge on the same answer: **build a Gemma-side job-queue REST surface, run Nextflow against Slurm as the executor, and wire the curator UI to dispatch + observe.** The foundation is already half-built — `TaskRunningService` + `TasksWebService` + `DatasetsWebService` `/tasks/preprocess|diagnostics|batchInfo|differential` endpoints already do exactly this pattern for in-JVM tasks (HTTP 202 + `Location: /tasks/{id}`, in-memory store, 10-minute eviction). The gap is (a) persistence (the store is in-memory), (b) external executors (no Slurm/Nextflow wiring), and (c) curator-UI surface.

**Recommended sequencing:** session 1 — design a persistent `PIPELINE_RUN` table + a `PipelineExecutor` SPI with in-JVM and `nextflow run` implementations; session 2 — port `addGEOData` / `loadSingleCellData` / `aggregateSingleCellData` / `diffExAnalyze` dispatch into REST endpoints that submit through the new SPI; session 3 — port `rnaseq-pipeline` from Luigi to Nextflow (medium-effort but the only blocker for unified executor); session 4 — curator-UI "Run Pipeline" panel per experiment + bulk-run view, replacing the Google Sheet.

## 2. Curator CLI surface today

### 2.1 Sequencing-mode breakdown

**Microarray (Affymetrix CEL / two-colour / single-channel):**
1. Browser: search GEO; decide to load.
2. Terminal: `gemma-cli addGEOData -e GSEXXX -nopost -allowsuper`.
3. Terminal: `gemma-cli affyFromCel -e GSEXXX` (Affy only — extracts raw signal from CEL).
4. Browser: curate experimental design + tags; click "Diagnostics"; outlier removal.
5. Terminal: `gemma-cli processedDataCompute -e GSEXXX` (or REST `/tasks/preprocess`).
6. Terminal: `gemma-cli diffExAnalyze -e GSEXXX`.
7. Browser: review DEA tab; make public.

**Bulk RNA-seq:** identical to microarray for steps 1, 4, 7, but steps 2-3-5-6 are subsumed by the Luigi pipeline:
1. Curator adds GSE accession + priority + `data=resubmit` to the RNA-seq Google Sheet (`Maintaining-The-RNA-seq-Google-Sheet`).
2. `submit-experiments-from-gsheet --spreadsheet-id ... --sheet-name ...` (or the Luigi daemon polling) picks it up.
3. Luigi DAG: `DownloadExperiment` → `TrimExperiment` → `AlignExperiment` (STAR/RSEM via Bioluigi on Slurm) → `CountExperiment` → `GenerateReportForExperiment` → `SubmitBulkExperimentDataToGemma` (shells out to `gemma-cli rnaseqDataAdd -e GSEXXX -a Generic_<sp>_ncbiIds -rpkm ... -counts ...`) → `SubmitExperimentBatchInfoToGemma` → `SubmitExperimentReportToGemma`.
4. Curator monitors `http://localhost:8082/` (Luigi UI), reads Slack notifications, and `scp`s a `web_summary.html` to their laptop if Cell Ranger needs diagnosing.

**scRNA-seq:** the most ceremony-heavy path. From `Single-cell-curation-workflow-for-loading-experiments-onto-Gemma`:
1. `gemma-cli addGEOData -e GSEXXX -allowsuper`
2. `gemma-cli downloadSingleCellData -e GSEXXX`
3. Manual decision: split by `organism_part`? If yes: re-load, `gemma-cli deleteExperiments` parent.
4. `gemma-cli loadSingleCellData -e GSEXXX -a Generic_<sp>_ncbiIds --preferred-quantitation-type` (with possible `--renaming-file`, `-ignoreUnmatchedCellIds`, `--infer-samples-from-cell-ids-overlap`)
5. Browser: curate ED.
6. Cell-type annotations branch:
   - 6a (author-submitted): manual R wrangling using a community helper function (paste into console), then `gemma-cli addMetadataFile -e GSEXXX <file>` + `gemma-cli loadSingleCellData --load-cell-type-assignment --cell-type-assignment-file ... --cell-type-assignment-name "Author-submitted annotations" -e GSEXXX --preferred-cell-type-assignment --cell-type-assignment-protocol author-submitted`.
   - 6b (no annotations, brain only): Jenkins-trigger `sc-annotation-pipeline` (Nextflow) → produces `results/<GSE>/cell_type_annotations.tsv` → optionally auto-uploads (the `GEMMA_UPLOAD` subworkflow already exists) → curator manually fetches and runs `addMetadataFile`.
7. `gemma-cli aggregateSingleCellData -e GSEXXX --make-preferred -nopost` (with `--redo` if re-run needed)
8. `gemma-cli diffExAnalyze -e GSEXXX -subset cell_type -factors cell_type,...`
9. Track ~17 status columns in the Gemma Single-Cell Experiment Tracker Google Sheet by hand.

### 2.2 Inventory of curator-facing commands

| Command | Invokes | Inputs | Outputs | Manual prerequisite |
|---|---|---|---|---|
| `gemma-cli addGEOData -e GSEXXX -allowsuper` | Gemma SOFT parser | GEO accession | EE row + samples + platform-load if new | Decided to load this GSE |
| `gemma-cli addTSVData` | Gemma TSV loader | TSV file | EE row | Non-GEO data already on disk |
| `gemma-cli affyFromCel -e GSEXXX` | Affy R/Aroma | CEL files at `/space/gemmaData/...` | Raw vectors | EE already loaded |
| `gemma-cli downloadSingleCellData -e GSEXXX` | GEO/SRA fetch | GEO accession | MEX/AnnData on disk | EE already loaded |
| `gemma-cli loadSingleCellData -e GSEXXX -a Generic_<sp>_ncbiIds --preferred-quantitation-type` | SC loader | EE + MEX/AnnData on disk + platform | SC vectors + cell-type EFC if `--load-cell-type-assignment` | Downloaded; renaming-file written if needed |
| `gemma-cli addMetadataFile -e GSEXXX <file>` | metadata writer | EE + tsv | metadata file under `/space/gemmaData/metadata/GSE.../` | Cell-type tsv wrangled |
| `gemma-cli aggregateSingleCellData -e GSEXXX --make-preferred -nopost` | aggregator | SC vectors + cell-type CTA | pseudo-bulk QT | CTA loaded |
| `gemma-cli diffExAnalyze -e GSEXXX [-subset cell_type] -factors a,b,...` | DEA | EE + ED | DEA result rows | ED curated |
| `gemma-cli rnaseqDataAdd -e GSEXXX -a Generic_<sp>_ncbiIds -rpkm ... -counts ...` | RNA-seq loader | RPKM + counts matrices on disk | Raw vectors | Luigi pipeline produced the matrices |
| `gemma-cli deleteExperiments -e GSEXXX` | EE deleter | EE | (EE gone) | Decided to redo from scratch |
| `gemma-cli deleteSingleCellDataAggregate -e GSEXXX -qt "..."` | aggregator | EE | aggregated QT removed | `aggregateSingleCellData` succeeded earlier |
| `gemma-cli processedDataCompute -e GSEXXX` | post-processor | EE + raw vectors | processed vectors + PCA + sample-corr | Raw vectors present |
| `gemma-cli blackList ...` | blacklist | accession + reason | blacklist row | Decided unusable |
| `rnaseq-pipeline-cli run rnaseq_pipeline.tasks.SubmitExperimentToGemma --experiment-id GSEXXX --workers 30 [...]` | Luigi DAG | GSE accession | full quantification + Gemma load | Bulk RNA-seq experiment |
| `./scripts/remove-experiment <GSEXXX>` (rnaseq-pipeline) | rm -rf intermediate dirs | GSE accession | (intermediate state gone) | Re-running with different params |
| `submit-experiments-from-gsheet --spreadsheet-id ... --sheet-name ...` | Luigi + Google Sheets | Sheet ID | scheduled Luigi tasks | Sheet maintained |
| `nextflow run main.nf -profile conda -params-file params.<sp>.json --input samplesheet.csv` (sc-annotation) | Nextflow on Slurm | study names or paths | cell-type tsv + MultiQC + optional Gemma upload | Brain + non-cancer + adult + no annotations |

Branch URL is `https://gemma.msl.ubc.ca` (prod) or `https://dev.gemma.msl.ubc.ca` (dev) per `example.luigi.cfg` `[rnaseq_pipeline.gemma]`. The CLIs typically need `JAVA_HOME`, `JAVA_OPTS`, plus `gemma-cli-staging` is a separate binary pointing at the staging instance.

### 2.3 Pain points called out in the manuals

- **Manual R wrangling** for cell-type annotations: curators paste a function definition into their R console to munge barcodes/prefixes/suffixes. Source: `Single-cell-curation-workflow…` step 6.1.
- **`--redo` archaeology**: when `aggregateSingleCellData` was previously run, the curator must either pass `--redo` or invoke `deleteSingleCellDataAggregate -qt "<the exact name>"` — and the QT name is only discoverable by running with a deliberately wrong `-qt` and reading the error message. Verbatim from the doc.
- **Single-cell split-after-curate**: if the split-by-organism-part decision is made after ED curation, the workflow says "this will eventually be resolved in #1318" (GitHub issue) — i.e. it isn't resolved.
- **Sequencing-mode parameter passing for SC** is hand-glued: `rnaseq-pipeline-cli run rnaseq_pipeline.tasks.SubmitExperimentToGemma --experiment-id GSEXXX --rnaseq-pipeline.tasks.AlignSingleCellExperiment-chemistry SC5P-R2 --rnaseq-pipeline.tasks.GenerateReportForSingleCellExperiment-chemistry SC5P-R2 --workers 30`. Issue #108 on the repo is to simplify this.
- **Cell Ranger diagnostic loop**: `scp lisa:/cosmos/data/pipeline-output/rnaseq/quantified-single-cell/refdata-gex-XXXXX/GSEXXXXX/GSMXXXX/outs/web_summary.html . && xdg-open` — a curator must manually copy a 5MB HTML to their laptop to look at one chart.
- **Google Sheet column-order coupling**: "the first 4 columns (experiment_id, priority, data, and batch_info) must exist so please do not touch them" — the pipeline parses by index, not header.
- **Status-column proliferation**: the Single-Cell Experiment Tracker sheet has ~17 hand-filled columns (Gemma status, Data status, ED done?, Cell types in GEO?, Cell type extraction status, Cell typing pipeline ran, Cell type loaded, Barcode collisions, Samples empty, Needs splitting?, DEA ready?, Data aggregated?, DEA performed?, Comments). Most of these are derivable from Gemma DB state.
- **Manual email loop** for cell-type annotations: the doc includes a verbatim email template. Three reminder emails, one week apart. Pure ceremony.

## 3. rnaseq-pipeline (Luigi today)

### 3.1 DAG shape + Bioluigi/Slurm integration

Top-level entry: `SubmitExperimentToGemma` (a `WrapperTask`). Its dependency closure (from `tasks.py`):

```
SubmitExperimentToGemma
├── SubmitExperimentDataToGemma  (bulk OR single-cell branch)
│   ├── SubmitBulkExperimentDataToGemma  -> gemma-cli rnaseqDataAdd
│   │   └── CountExperiment
│   │       └── AlignExperiment
│   │           └── AlignSample (ScheduledExternalProgramTask)  ← Slurm submit
│   │               └── TrimSample / QualityControlSample
│   │                   └── DownloadSample (per-sample, source=geo|sra|arrayexpress|gemma)
│   │                       └── PrepareReference (STAR/RSEM index)
│   └── SubmitSingleCellExperimentDataToGemma  -> gemma-cli loadSingleCellData
│       └── AlignSingleCellExperiment
│           └── AlignSingleCellSample (Cell Ranger, ScheduledExternalProgramTask)
├── SubmitExperimentBatchInfoToGemma  -> gemma-cli (batch info)
└── SubmitExperimentReportToGemma  -> gemma-cli (MultiQC report attach)
```

Slurm integration is via Bioluigi (`scheduler=slurm` in `[bioluigi]` of `example.luigi.cfg`). Resource caps in `[resources]`: `slurm_jobs=384`, `prefetch_jobs=2`, `fastq_dump_jobs=40`, etc. There's also `[rnaseq_pipeline.wrapped_tools]` that swaps in `rnaseq-pipeline-cellranger` / `rnaseq-pipeline-rsem-calculate-expression` wrappers — these handle local-scratch staging of references (Cell Ranger) and shared-memory preload of STAR genomes (RSEM).

The Luigi daemon (`luigid`) runs at `localhost:8082` providing a per-task DAG visualization and live progress. State is in a luigi pickle directory.

### 3.2 Curator-facing CLI surface

Three entry points (from `cli.py`):

- `rnaseq-pipeline-cli run <task> <task_args>` — generic Luigi runner with `--workers`, `--local-scheduler`, `--umask`.
- `rnaseq-pipeline-cli submit-experiment --experiment-id GSEXXX [--rerun] [--priority N] [--chemistry X]` — convenience for the most common path.
- `submit-experiments-from-gsheet --spreadsheet-id ID --sheet-name NAME` — pulls a column-positional sheet (`experiment_id`, `priority`, `data`, `batch_info`) and schedules positive-priority rows.

Plus the housekeeping scripts under `scripts/`: `luigi-wrapper`, `remove-experiment`, `remove-old-data`, `sync-multiqc-reports`, `purge-problematic-sra-data`, `map-gene-ids`, `prepare-ncbi-gtf-for-gemma`.

### 3.3 Outputs back to Gemma

Three Gemma CLI roundtrips (via `gemma.py` shelling `gemma-cli`):
1. `rnaseqDataAdd -e GSEXXX -a Generic_<sp>_ncbiIds -rpkm <data.rpkm.txt> -counts <data.counts.txt>` (bulk) or `loadSingleCellData ...` (SC).
2. Batch info: a separate `gemma-cli` call writing the per-sample sequencing-batch metadata.
3. MultiQC report: attached as metadata file via `addMetadataFile`.

Plus the Slack webhook (`SLACK_WEBHOOK_URL` in config) for human notifications.

### 3.4 Luigi → Nextflow migration assessment

**Effort: medium.** ~25 task classes in `tasks.py`, ~5 source plugins (`sources/` for geo/sra/arrayexpress/gemma), Bioluigi-specific resource-counting and dynamic-task patterns. The hardest parts are not the DAG translation but: (a) Bioluigi's resource accounting (`slurm_jobs=384`, `geo_http_connections=4`) doesn't map 1:1 to Nextflow's `executor.queueSize=N` — you'd need per-process limits or a global `maxForks`; (b) the `RerunnableTaskMixin` / `DynamicWrapperTask` patterns flatten naturally in Nextflow but the conditional bulk-vs-SC branch in `SubmitExperimentDataToGemma` requires `branch` operator or two separate entry workflows.

**Risks:**
- Re-validating quantification reproducibility across runners. STAR/RSEM determinism is fine; differences would come from input download (SRA prefetch behaviour) or scratch-staging changes. Plan: replay 10 known-good GSEs side-by-side; diff count matrices.
- Shared-memory STAR preload is a real performance feature in the current pipeline. Nextflow doesn't have an exact equivalent — you'd preload the reference once and pin processes to that node, or accept the cold-start cost.
- Google-Sheet polling re-implementation: Nextflow can read a CSV samplesheet via `nf-schema` (sc-annotation already uses this) but doesn't natively poll a remote sheet. Move polling into Gemma (a scheduled job populates a `PIPELINE_RUN` queue from the sheet) rather than into Nextflow.

**Benefits:**
- One executor across both pipelines (sc-annotation already speaks Nextflow on Slurm). One operational story.
- nf-test snapshots + nf-schema samplesheet validation are real wins over Luigi.
- Container support (Apptainer/Singularity) is first-class in Nextflow; the current pipeline relies on a Conda environment that ships an RSEM patch.
- Multi-org compatibility (nf-core conventions) — strangers can run the pipeline without lab-specific lore.

**Migration path sketch:**
1. Port `PrepareReference` + `AlignSample` + `CountExperiment` as a thin Nextflow workflow against the same STAR/RSEM binaries (do NOT swap binaries; same patched RSEM).
2. Reproduce 5 known-good GSEs end-to-end; diff outputs against Luigi.
3. Port the SC branch (Cell Ranger).
4. Port the Gemma-roundtrip tail (`gemma-cli` calls in a `process` block; or — better — drop these and let Gemma dispatch the pipeline + ingest the outputs directly via the new scheduler API).
5. Retire the Google Sheet poll (replace with Gemma queue, see §5).

**Recommendation: GO, but only after the Gemma scheduler skeleton exists** (session 1-2 below). Doing them in the reverse order (port the pipeline first, then bolt on a scheduler) means writing the Google-Sheet polling adapter twice. The pipeline runs fine on Luigi; the scheduler API is the load-carrying piece.

## 4. sc-annotation-pipeline (Nextflow already)

### 4.1 Architecture summary

nf-core-shaped DSL2. Six subworkflows (`INPUT_CHECK`, `PREPARE_REFERENCE`, `PROCESS_QUERIES`, `CLASSIFY_CELLTYPES`, `QC_REPORTING`, `GEMMA_UPLOAD`). Slurm executor: `process.executor = 'slurm'`, `process.clusterOptions = '-C thrd64 --cpus-per-task=8 --mem=32G'`, `executor.queueSize = 25`. Apptainer is the preferred container engine (`quay.io` registry). Three input modes — samplesheet CSV (preferred), `--study_names` (legacy), `--study_paths` (legacy). Per-species params files: `params.hs.json` / `params.mm.json`. Gemma upload subworkflow exists with `upload_cta`, `upload_clc`, `upload_mask`, `upload_multiqc` toggles; credentials via `GEMMA_USERNAME` / `GEMMA_PASSWORD` env vars.

### 4.2 Curator-facing surface

Today: **Jenkins button**. The `Jenkinsfile` runs `nextflow run sc-annotate.nf -profile conda -params-file params.mm.json --study_names /space/grp/rschwartz/.../study_names_mouse.txt -process.executor slurm -resume`. Curators add GSE accessions to that text file and press the Jenkins build button. The `try { sh '''...''' } catch { error('Pipeline execution failed') }` wrapping is the entirety of the orchestration around Nextflow.

### 4.3 What we can learn for the rnaseq port + web UI

- **Samplesheet-CSV-as-input is the right shape.** Gemma scheduler can write a samplesheet at submission time and `nextflow run --input samplesheet.csv`; this also gives nf-schema validation for free.
- **Per-species params files** is a nice pattern that should be preserved when porting rnaseq (`params.hs.json` / `params.mm.json` / future `params.rno.json`).
- **The Gemma upload subworkflow already exists.** Whether to keep it (Nextflow pushes to Gemma) or invert it (Gemma pulls completed outputs and ingests via its own CLI/SPI) is open — but the existing wiring proves it can be done.
- **`executor.queueSize = 25`** is far below Bioluigi's `slurm_jobs=384`. The rnaseq pipeline runs more concurrent jobs per experiment than the SC one does; the per-pipeline queue cap will need to be different.
- **Jenkins is a stopgap.** Replacing it with a Gemma-side dispatch (curator clicks "Run cell-type pipeline" in the browser) is the smaller change to make the SC pipeline curator-friendly, and the same plumbing serves the rnaseq port.

## 5. Proposed web UI + scheduler design

### 5.1 Architecture sketch

```
┌─ gemma-curation-ui (Vue) ────────────────────────────┐
│  per-EE "Pipelines" tab                              │
│   ├─ Submit dropdown (preprocess|DEA|rnaseq|sc-anno) │
│   ├─ Run history table (live)                        │
│   └─ Per-run drawer: stdout tail + DAG link          │
│  Bulk view: queued | running | failed | succeeded    │
└──────────────┬───────────────────────────────────────┘
               │ HTTPS, SSE for run-status stream
┌──────────────┴───────────────────────────────────────┐
│  gemma-rest                                          │
│   ├─ POST /datasets/{ee}/tasks/preprocess (exists)   │
│   ├─ POST /datasets/{ee}/tasks/rnaseq      (new)     │
│   ├─ POST /datasets/{ee}/tasks/scAnnotation (new)    │
│   ├─ GET  /tasks/{id}    (exists; returns VO)        │
│   ├─ GET  /tasks/{id}/events  (new; SSE)             │
│   └─ GET  /pipelines/runs?status=... (new; bulk)     │
└──────────────┬───────────────────────────────────────┘
               │ Spring Service
┌──────────────┴───────────────────────────────────────┐
│  PipelineRunService                                   │
│   ├─ persist PIPELINE_RUN row                        │
│   ├─ pick PipelineExecutor by kind                   │
│   ├─ in-JVM (existing TaskRunningService)            │
│   ├─ NextflowExecutor (sbatch wrapper)               │
│   └─ poll status + ingest outputs                    │
└──────────────┬─────────────────┬─────────────────────┘
               │                 │
   ┌───────────┴────────┐  ┌─────┴─────────────────────┐
   │ in-JVM tasks       │  │ Slurm cluster              │
   │ (preprocess, DEA)  │  │ ├─ rnaseq-pipeline (NF)    │
   │ already wired      │  │ ├─ sc-annotation (NF)      │
   └────────────────────┘  │ └─ future pipelines        │
                           └────────────────────────────┘
```

### 5.2 Job lifecycle

State machine — every transition writes an audit row:

```
SUBMITTED → QUEUED → RUNNING → ┬→ SUCCEEDED
                                ├→ FAILED
                                ├→ CANCELLED
                                └→ TIMEOUT
```

Eviction: rows live forever in DB (with an `expired_at` column); the in-memory progress stream evicts after 10 minutes (existing behaviour preserved). `RUNNING → FAILED` transitions capture the last 4KB of stdout/stderr.

### 5.3 Curator UI surface

- **Per-EE "Pipelines" tab** alongside the existing ED / Diagnostics / DEA tabs: a "Run pipeline" dropdown (preprocess, DEA, rnaseq, sc-annotation, custom), a run-history table sorted by submission time, an expandable drawer per run showing live stdout tail + a link to the upstream DAG (Luigi UI / Nextflow `report.html` / future).
- **Bulk view** at `/curation/pipelines`: faceted by status (queued / running / failed / succeeded / cancelled), by pipeline kind, by curator. "Run all in queue" / "Retry failed". This replaces the RNA-seq Google Sheet *and* the Single-Cell Experiment Tracker — both are queryable from Gemma DB state once the pipeline-run table exists.
- **Cell Ranger web summary fetcher**: a `/datasets/{ee}/pipelineRuns/{id}/artifacts/web_summary.html` endpoint that streams the file from the pipeline workdir. Kills the `scp lisa:...` loop.

### 5.4 Gemma-side data model additions

```sql
CREATE TABLE PIPELINE_RUN (
    ID                BIGINT PRIMARY KEY AUTO_INCREMENT,
    EXPERIMENT_FK     BIGINT NULL REFERENCES INVESTIGATION(ID),
    PIPELINE_KIND     VARCHAR(64)  NOT NULL,  -- 'preprocess', 'dea', 'rnaseq', 'sc-annotation', ...
    EXECUTOR_KIND     VARCHAR(32)  NOT NULL,  -- 'in_jvm', 'nextflow_slurm'
    STATUS            VARCHAR(32)  NOT NULL,  -- enum above
    SUBMITTED_BY_FK   BIGINT       NOT NULL REFERENCES CONTACT(ID),
    SUBMITTED_AT      DATETIME(6)  NOT NULL,
    STARTED_AT        DATETIME(6)  NULL,
    FINISHED_AT       DATETIME(6)  NULL,
    PARAMS_JSON       TEXT         NULL,      -- serialized parameter map
    WORK_DIR          VARCHAR(512) NULL,      -- Nextflow workdir / Luigi run dir
    EXTERNAL_RUN_ID   VARCHAR(128) NULL,      -- Slurm job id / Nextflow session id
    LAST_MESSAGE      VARCHAR(4096) NULL,
    EXIT_CODE         INT          NULL,
    INDEX (EXPERIMENT_FK),
    INDEX (STATUS, SUBMITTED_AT)
);
```

Optional `PIPELINE_RUN_EVENT` (log tail captured at structured points) — start with embedded `LAST_MESSAGE` and grow later.

### 5.5 Integration points

- **gemma-rest** hosts the dispatch + polling endpoints. Pattern is already established (`TasksWebService`, the `/datasets/{ee}/tasks/*` `@POST` endpoints all return 202 + Location). Extend `TaskStatusValueObject` to surface `EXTERNAL_RUN_ID`, `WORK_DIR`, executor kind. Add SSE (`/tasks/{id}/events`) by tailing `LAST_MESSAGE` updates — there's precedent in `SubmittedTask.getProgressUpdates()`.
- **Slurm submission** lives in `gemma-core` as a `NextflowExecutor` Spring bean that shells out to `sbatch --wrap "nextflow run /space/grp/Pipelines/<pipeline>/main.nf -profile conda -params-file <species>.json --input <samplesheet> --outdir /space/scratch/gemma-pipelines/<run-id>/ -with-report -with-trace"`. Status is polled via `squeue --json` (every ~10s) and the Nextflow `trace.txt` (for per-process state).
- **Curator UI** in `~/Dev/gemma-curation-ui/apps/curation/`. The browser app already exists; the "Pipelines" tab is one component + a Pinia store + Server-Sent Events wiring. The UI repo's existing pattern for talking to gemma-rest applies.
- **Status streaming**: SSE (one connection per page) is simpler than WebSocket and works fine through nginx. Polling at 5-10s is the fallback for low-volume status updates.

## 6. Phased plan

- **Session 1 — design + skeleton.** Write the `PIPELINE_RUN` Flyway migration; design the `PipelineExecutor` SPI (`submit(params) → externalRunId`, `pollStatus(externalRunId) → Status`, `cancel(externalRunId)`); stub `InJvmPipelineExecutor` that wraps the existing `TaskRunningService`. Outcome: REST surface is in place, no externals yet, behaviour identical to today via the in-JVM path.
- **Session 2 — Nextflow executor.** Implement `NextflowSlurmPipelineExecutor` against the existing `sc-annotation-pipeline` (it already runs end-to-end; this is purely a "Gemma can dispatch it" exercise). Curator gets a "Run sc-annotation" button in the curation UI. Retires the Jenkins button.
- **Session 3 — rnaseq port to Nextflow.** Per §3.4 migration sketch. Validated against a 10-GSE replay panel. Outcome: rnaseq pipeline dispatchable from the same UI; Google Sheet polling retired (replaced by Gemma-DB-backed queue).
- **Session 4 — curator UI polish + bulk view.** "Pipelines" tab on the EE page; `/curation/pipelines` bulk view; SSE wiring; Cell Ranger web-summary fetcher. Retires Single-Cell Experiment Tracker Google Sheet.
- **Session 5 (optional) — ingest the long tail.** `addGEOData`, `downloadSingleCellData`, `loadSingleCellData`, `aggregateSingleCellData` exposed as REST endpoints submitting through the same scheduler. Curators stop SSH'ing entirely.

## 7. Open questions for Paul

1. **Persistence vs in-memory** — the existing `TaskRunningService` is in-memory with 10-minute eviction. Is the goal to persist *all* pipeline runs (audit + history forever) or only the externally-executed ones (Nextflow/Slurm), keeping in-JVM tasks in-memory as today?
2. **One pipeline run table vs polymorphic queue** — do we want one `PIPELINE_RUN` table for all kinds, or separate tables per pipeline (and a union view in the API)? Polymorphic params-as-JSON is simpler; separate tables let each pipeline have a typed schema.
3. **Curator authentication model for the scheduler** — `TasksWebService` is currently `@PreAuthorize("hasAuthority('GROUP_ADMIN')")`. Is the curator UI's pipeline-dispatch permission GROUP_ADMIN, or a new GROUP_CURATOR-and-up role? Same question for who can cancel someone else's run.
4. **Where the Nextflow workdir lives** — `/space/scratch/...` per-run? `/cosmos/data/pipeline-output/...` shared and resumable? Affects `-resume` semantics and disk pressure (some rnaseq runs leave hundreds of GB).
5. **Gemma push vs Gemma pull for pipeline outputs** — sc-annotation's `GEMMA_UPLOAD` subworkflow has the Nextflow pipeline push to Gemma. Alternative: the Nextflow run writes outputs to a known location and Gemma's scheduler picks them up post-success and runs `rnaseqDataAdd` / `addMetadataFile` itself. Pull is cleaner for permissions (no Nextflow → Gemma credentials) but requires Gemma to know each pipeline's output layout.
6. **Should the Google Sheet survive in any form?** — some lab members (Sanja, Salva) curate from the sheet itself, not the Gemma UI. If we retire it, are they on board with browsing the Gemma curation UI instead? Or do we keep one-way export from Gemma → Sheet?
7. **Slurm cluster topology + queue caps** — Bioluigi's `slurm_jobs=384` is generous; sc-annotation's `queueSize=25` is conservative. What's the *aggregate* cap the cluster admin wants to see Gemma submit? Per-pipeline-kind or global?
8. **Pipeline versioning** — when we update `rnaseq-pipeline` (new STAR version, new reference), should the `PIPELINE_RUN` row record the pipeline git SHA + container digest? The provenance-stamp pattern from `~/.claude/CLAUDE.md` suggests yes. Same answer for `sc-annotation-pipeline`.

## Surprises

- **Gemma already has a usable task-dispatch foundation.** `TaskRunningService` + `TasksWebService` + the four `@POST /datasets/{ee}/tasks/*` endpoints in `DatasetsWebService` implement exactly the 202-and-poll pattern this design needs. The work is "extend" not "build from scratch."
- **The Confluence pages describe pain that the curator-UI scope can absorb cleanly.** Manual R wrangling, `--redo` archaeology, `scp web_summary.html`, three-week author-email loop, ~17-column status sheet — all of these have natural homes in either the curator UI or a small structured-input wizard.
- **The single-cell pipeline is already where bulk RNA-seq needs to be.** Same Slurm executor, same nf-core shape. Porting rnaseq to Nextflow is the smaller half of the move, not the larger.
- **`SchedulerConfig.java` is Quartz** — for in-Gemma scheduled jobs (BatchInfoRepopulationJob, Ee2AdUpdateJob, table maintenance), profile-gated on `scheduler` (only one production node runs it). Distinct from the curator-facing pipeline scheduling proposed here, but the precedent for "scheduled background work, profile-gated to one node" is useful for the polling logic that talks to Slurm.
- **Google Sheets is the de-facto job queue today.** Not a Gemma DB table, not a file in version control — a spreadsheet that Sanja and Carlton edit. Replacing it is mostly a curator-onboarding exercise once the UI replacement exists.
