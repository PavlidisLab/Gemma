# Real Nextflow dispatch — resolutions & open items (tasks 7–8)

Living log of decisions for implementing the real `NextflowSlurmScheduler` (§7 task 7,
sc-annotation-pipeline first; then task 8 rnaseq), under the delegated / Nextflow-native model
(Slice 0, decided 2026-07-13). Records what discussion has settled so the eventual build isn't
guessing, and what's still open. **Updated as points resolve — most recent context wins.**

Target pipeline: `../sc-annotation-pipeline` (retires its Jenkins-button launch).

---

## Resolved

| # | Topic | Resolution | When |
|---|---|---|---|
| R1 | **Network path** | Slurm compute nodes share Gemma's network, so Nextflow `-with-weblog` POSTs reach Gemma's internal callback (`/rest/v2/internal/pipeline/jobs/{id}/events`) with no firewall/proxy work. | 2026-07-15 (Ogan) |
| R2 | **Work-dir / `-resume`** | Gemma's `gemma.appdata.home` = `/space/gemmaData`, mounted on all Slurm nodes. The Nextflow `-work-dir` lives under it (e.g. `/space/gemmaData/pipeline/<batchId>`); shared FS ⇒ `-resume` works across attempts. Directory config only, no cluster ask. | 2026-07-15 (Ogan) |
| R3 | **Submit capability** | Every cluster node is Slurm-submit-capable (example: `scratchy`); any of them can `sbatch` into the cluster. | 2026-07-15 (Ogan) |
| R4 | **Execution model** | Gemma 2.0 runs as a **container** (no Slurm client / munge inside it). Dispatch by **SSH to a submit node** and launch `nextflow` there — this keeps the Slurm+munge stack on the node (a solved environment) and out of Gemma's container. Container needs only an SSH client + key. Avoids the container-munge problem entirely. | 2026-07-15 |
| R5 | **SSH auth** | Standard **public-key** auth: a dedicated Gemma service keypair, its public half in the submit **service account**'s `authorized_keys`; non-interactive (`BatchMode=yes`, pinned `known_hosts`). Precedent: the Jenkins `frink-deploy-key`. Private key + `known_hosts` mounted into the gemma-rest container. The service account = Slurm job owner **and** `/space/gemmaData` work-dir writer. | 2026-07-15 (Ogan) |
| R6 | **Concurrency caps** | Pipeline-owned: `nextflow.config` sets `executor.queueSize = 25` and `process.clusterOptions = '-C thrd64 --cpus-per-task=8 --mem=32G'`. Gemma's batch `maxConcurrent` (task 4) sits *above* this as a higher-level throttle; no separate cluster ask for sc-annotation. | 2026-07-15 (from pipeline) |
| R7 | **Results round-trip** | Unchanged from today: the `GEMMA_UPLOAD` subworkflow calls `gemma-cli[-staging] loadSingleCellData -loadCta -e <study>` (+ clc/mask/multiqc toggles). Gemma dispatches + monitors; the pipeline uploads results as it does now. | 2026-07-15 (from pipeline) |
| R8 | **Identity model** | The cluster-relevant identity is the **SSH target account on the node** (Slurm job owner + work-dir writer), **decoupled** from the container's internal uid (which is irrelevant to Slurm/`/space`). The adapter is config-driven (`submitHost` / `submitUser` / `sshKeyPath`), so "who Gemma is" is deploy config, not a code fork. The historical discrete Tomcat-user pattern maps forward as a discrete service account *on the submit node*. | 2026-07-15 (Ogan) |
| R9 | **Dev-time privilege stance** | During development, assume access to whatever the **current (dev) account** can reach — don't block on provisioning. The dedicated discrete service account + least-privilege **hardening is deferred**; the exact grants needed are tracked in §"Privileges …" below so the eventual account gets precisely what's required. | 2026-07-15 (Ogan) |
| R10 | **Gemma home bind-mount** | `/space/gemmaData` **is bind-mounted** into the gemma-rest container (settling O7 toward the mount, not pure-SSH). **Subjective preference (Ogan):** the mount makes the work-dir / logs / artifacts easily accessible externally without going through the SSH channel — worth it over the stateless-container purity of pure-SSH. Gemma can then read logs/artifacts directly; SSH is still the *write/submit* path, and the same-uid write rule (O7) still holds so nextflow-owned files stay consistent. | 2026-07-15 (Ogan) |
| R11 | **Run ↔ job mapping** | **One `nextflow run` per EE** (option a; settles O2). Each `PipelineJob` → its own `nextflow run` for a single study, fitting the existing per-job `PipelineScheduler.submit(SubmitRequest)` SPI **unchanged** (no batch-granularity submit primitive needed). Consequences: (1) **correlation is trivial** — one run = one EE = one job, so weblog events map straight to the job with **no study-tag routing**; (2) cross-EE queueing is **Slurm's** job (each run `sbatch`es its tasks into the shared queue); (3) the pipeline's `queueSize=25` stops being a *global* cap (it's per-run → up to N×25 tasks), so total load is bounded by **Gemma's batch `maxConcurrent`** (limits concurrent runs = concurrent nextflow driver processes) + Slurm QOS/user submit limits. Trade-off accepted: N driver head-jobs (on compute nodes, R13) instead of one, in exchange for the simpler adapter/event model. **Performance caveat → O9:** the driver-JVM cost is minor, but per-EE runs *duplicate shared organism-level work* — `PREPARE_REFERENCE` (`SETUP_SCVI` + `GET_CENSUS_ADATA`, keyed on `organism`+`census_version`) runs **once per run** and is broadcast to all studies in a combined run; under one-run-per-EE each study would redo the scVI setup + census download. **Resolved by O9** (a cached `PREPARE_CACHE` reference), so this is a non-issue and the granularity choice stands. | 2026-07-15 (Ogan) |
| R12 | **Concurrency cap model** | **Per-pipeline default `maxConcurrent`, stamped onto the existing per-batch column at submit** (reconciles the built §3.4 per-batch mechanism with D2's "per-pipeline-kind cap"). Source: a small per-pipeline config/registry (default keyed by pipeline kind, since D2 notes rnaseq ≠ sc-annotation). **sc-annotation default = `25`.** **Caveat (informed choice):** this `25` is *not* the pipeline's `queueSize=25` — that caps Slurm tasks *within one run*; this caps *concurrent runs* (= 25 driver head-jobs, on compute nodes per R13, each able to fan out up to `queueSize` tasks → up to ~625 concurrent Slurm tasks). Slurm per-user submit limits are the real backstop; tunable per-batch via `updateBatch`, so safe to start here and revise. **Global cross-batch ceiling: deferred** (D2 wants one but flags it needs cluster-admin numbers; lean on Slurm submit limits until two concurrent batches actually occur). | 2026-07-15 (Ogan) |
| R13 | **Detached launch + handle** | **A2 — submit the nextflow head process *as a Slurm job*** (nf-core best practice). Launch via `sbatch --parsable <wrapper.sh>` (wrapper runs `nextflow run …`); `--parsable` prints just the job id → SSH captures it synchronously. **`SchedulerHandle.id` = the Slurm head-job id.** Cancel = `scancel`; poll (reconciler fallback) = `squeue`/`sacct -j`. Driver JVMs run **on compute nodes, not the login node** — directly bounds R11's N-driver footprint. Not part of the handle (Gemma already controls): work-dir (`-work-dir …/<jobId>`, known for `-resume`/logs/artifacts) and weblog correlation (callback URL carries `jobId`). *Dependency:* confirm a partition/QOS tolerant of many small long-lived head jobs (pair with the O1/cluster conversation). *Alternative recorded, not taken:* A1 (login-node `setsid … &`, handle = PID) — simpler but hosts the JVMs on the login node and PID is fragile. | 2026-07-15 (Ogan) |

---

## Pipeline facts (sc-annotation, from `../sc-annotation-pipeline`)

- Entry `main.nf`; today launched (Jenkins) as:
  `nextflow run main.nf -profile conda -params-file params.mm.json --study_names <file> -process.executor slurm -resume`
- Input: `params.input` = a **samplesheet CSV** (preferred / nf-core path), or legacy `--study_names` / `--study_paths` (list/file of studies). One run ingests a **set** of studies.
- Per-organism params: `params.hs.json` / `params.mm.json` (organism, census version, ref collections, `use_staging`, `preferredCtaLevel`, …).
- Config: `process.executor='slurm'`, `queueSize=25`, `clusterOptions='-C thrd64 --cpus-per-task=8 --mem=32G'`; profiles `conda` (used) / `apptainer` / `singularity` / `test`.
- **No** `-with-weblog` / `-with-trace` / `-with-report` yet — task 7 adds these.
- Checkout/assets referenced under `/space/grp/Pipelines/sc-annotation-pipeline/…`.

---

## Open items / to confirm

- **O1 — SSH service account & key wiring** *(deferred per R9; not a dev blocker)*. Which account on the submit node Gemma authenticates as. **Reuse the existing pipeline account if one exists** (whatever the current sc-annotation Jenkins run executes as — already the de-facto automation identity); else a discrete `gemma`/`gemma-pipeline` account, mirroring the historical discrete-Tomcat-user pattern (R8). During dev, run under the current dev account and log required grants in §"Privileges…" below. At hardening time: generate the dedicated keypair, put its public half in the account's `authorized_keys`, mount private key + `known_hosts` into the gemma-rest container (deploy-side, `update.sh`/run config). *Not to be confused with the Gemma-app user (`GEMMA_USERNAME`) the pipeline's `GEMMA_UPLOAD`/`gemma-cli` step uses to authenticate to the REST API — a separate credential.*
- **O2 — Run ↔ job mapping** — *resolved by R11: one `nextflow run` per EE.* No study-tag routing needed; fits the per-job SPI as-is. Follow-on gap → O8.
- **O8 — Concurrency cap** — **BUILT (2026-07-16):** `PipelineDefaults` (`sc-annotation = 25`, overridable via `gemma.pipeline.sc-annotation.maxConcurrent`); `PipelineJobBatchServiceImpl.submit` stamps the pipeline default onto the batch when the caller omits an explicit cap (unknown pipelines → `null`/unlimited, unchanged). Tested: `PipelineDefaultsTest` + two `PipelineJobBatchServiceMockIT` cases (sc-annotation → 25, other → null). Global cross-batch ceiling still deferred (needs cluster-admin numbers, D2).
- **O3 — Weblog payload → `recordEvent` translation** — *shape captured & pinned (Nextflow 24.10.3), see §"Weblog payload shape" below.* **Version confirmed (Ogan): the remote clusters run the identical Nextflow version** — and the local `nextflow` used for the capture is `/space/opt/bin/nextflow` on the shared `/space` mount, i.e. the same binary, so the captured payloads are authoritative. **`tag` confirmed too:** the pipeline's study-level processes set `tag "$study_name"` (`load_cta`, `get_meta`, `download_studies`, `run_multiqc`, `combine_*`, `load_*`, … — a dozen of them; a few per-sample/organism steps use `$query_name`/`$organism`), so `trace.tag` carries the study name on the meaningful stages (readable stage labels; not needed for correlation under R11). **O3 BUILT (2026-07-16):** `NextflowWeblogTranslator` (gemma-core, `ubic.gemma.core.pipeline`) + `NextflowWeblogTranslatorTest` (10 cases vs. real captured fixtures under `data/pipeline/weblog/`); REST ingest `POST /internal/pipeline/jobs/{id}/weblog` on `InternalPipelineWebService` (bearer-token auth, translate → `recordEvent`, 204 for ignored messages) + `InternalPipelineWebServiceTest` (5 cases). The failed-task-≠-job-failure guard is tested at both layers.
- **O4 — Detached launch + handle capture** — *resolved by R13: A2 — `sbatch --parsable` the nextflow head process as a Slurm job; handle = Slurm head-job id; cancel `scancel`, poll `squeue`/`sacct`.* Residual dependency: confirm a partition/QOS tolerant of many small long-lived head jobs (fold into the O1/cluster conversation).
- **O5 — Pipeline checkout location** — *confirmed 2026-07-16:* `/space/grp/Pipelines/sc-annotation-pipeline` exists on scratchy with `main.nf` + `params.hs.json` + `params.mm.json`, readable by the dev account. Set `gemma.pipeline.nextflow.checkoutDir` to that path for the dev run (per Ogan, use this current version now; the cached `PREPARE_CACHE` version is a later config flip, O9). Update ownership (who refreshes the checkout) still a Rachel/ops detail, not a blocker.
- **O6 — Work-dir cleanup policy** for `/space/gemmaData/pipeline/*` (D5's cleanup) — *deliberately deferred (Ogan): inspect real runs' output first before writing a policy.* Rationale: **no-cleanup is the safe default** — `-resume` (R2) depends on the work-dir persisting, so eager cleanup is what breaks retries, not its absence. The policy should be *selective* (keep artifact-worthy outputs — `web_summary.html`/MultiQC/CTA; drop heavy intermediates/scratch; respect resume windows), which can't be designed well until we've seen a real work-dir's contents/sizes. Only deferred risk = disk growth on `/space`; monitor headroom, not a correctness issue for the bounded first runs.
- **O9 — Reference-sharing so per-EE runs don't redo census + scVI per study** — *resolved in principle via Rachel (2026-07-16): a cached `PREPARE_CACHE` approach caches the organism-level reference so per-EE runs reuse it instead of redoing `SETUP_SCVI` + `GET_CENSUS_ADATA` per study.* We only need to know it **can be done** — it can — so the per-EE duplication concern is off the table and R11 stands unambiguously; no Gemma change, no granularity revisit. The concrete caching version just gets pointed to when the pipeline checkout is wired (O5); exact branch/mechanism isn't a design blocker.
- **O7 — Container mount vs. pure-SSH** — *resolved by R10:* `/space/gemmaData` **is bind-mounted** (Ogan's subjective preference for easy external access). Gemma reads logs/artifacts (task 5's `readLog`/`readArtifact`) directly off the mount; **SSH stays the write/submit path**, and the same-uid write rule holds — whatever writes into the work-dir (the samplesheet) does so **as the service account** over SSH, so everything under `/space/gemmaData/pipeline/<batch>` is owned by the same uid the Slurm tasks run as (no uid mismatch on nextflow-owned files). The pure-SSH / no-mount alternative is recorded but not taken.

---

## Privileges the service account will need (running checklist)

Filled in as development surfaces requirements (R9). The eventual dedicated account gets **exactly**
this set; during dev we run under the current account and assume access. *(OS/cluster grants only —
the Gemma-app `GEMMA_UPLOAD` credential is separate, see O1.)*

*Cluster verification 2026-07-16 (as `omancarci@scratchy.msl.ubc.ca`, read-only probe — no jobs submitted):*

- [x] **SSH login** — non-interactive pubkey (`BatchMode=yes`) works as `omancarci@scratchy.msl.ubc.ca`.
- [~] **Submit the nextflow head job** — `sbatch` present (`/usr/bin/sbatch`); the *partition/QOS for many small long-lived head jobs* is still O4.
- [~] **Submit Slurm task jobs** — same (`-C thrd64 …`); depends on O4.
- [x] **Cancel jobs** — `scancel` present (`/usr/bin/scancel`).
- [x] **Query Slurm** — `squeue`/`scontrol` present; poll uses `squeue` then `scontrol show job` (**not `sacct` — accounting is disabled**, `AccountingStorageType=(null)`, so `sacct` returns nothing).
- [x] **Read** the pipeline checkout — `/space/grp/Pipelines/sc-annotation-pipeline` readable, with `main.nf` + `params.hs.json` + `params.mm.json` (O5).
- [x] **Read/write** the work-dir — `/space/gemmaData` is `tomcat:pavlab` mode `775`; the account is in **`pavlab`**, so it can create/write `/space/gemmaData/pipeline/*` (dir doesn't exist yet — the scheduler creates it).
- [~] **Execute `nextflow`** — at `/space/opt/bin/nextflow` (v24.10.3, matches fixtures), java 11 present, BUT **not on `PATH` in a non-login SSH shell**. *Handled:* the nextflow executable is now configurable — `gemma.pipeline.nextflow.executable` (default `nextflow`); set it to `/space/opt/bin/nextflow` in `Gemma.properties` for scratchy. (Ogan is also fixing the `PATH`.)
- *Note:* a **`pavlab-sa`** group exists on scratchy — likely the existing service-account infrastructure, and `/space/gemmaData` is owned by `tomcat` (the historical account, R8) → candidates for O1's dedicated account.

---

## Weblog payload shape (captured against Nextflow **24.10.3**, 2026-07-15)

Captured locally by pointing `-with-weblog` at a throwaway HTTP sink and running a tiny 2-process
pipeline (once clean-ish, once with a runtime `exit 3`). Local executor, but the envelope/event/trace
structure is executor-independent — only some values differ under Slurm (`native_id` becomes the
Slurm job id; `queue`/`memory`/`time` populate from `clusterOptions`). **Version confirmed identical on
the cluster** (shared `/space/opt/bin/nextflow` = same binary), so the captured payloads are
authoritative — no re-capture needed. Repro recipe + raw captures: scratchpad `weblog_sink.py` /
`main.nf` / `fail.nf` / `weblog*.ndjson`.

**Envelope (every message):**
```json
{ "runId": "<uuid == sessionId>", "runName": "silly_cray",
  "event": "<see below>", "utcTime": "2026-07-16T06:25:04Z",
  "trace": { ... },      // present ONLY on process_* events
  "metadata": { ... } }  // present ONLY on started / completed
```

**`event` enum & payload:**
| event | carries | notes |
|---|---|---|
| `started` | `metadata.workflow` (huge) | params under `metadata.parameters` (**not** `params`); `workflow.start`/`complete` are serialized Java `ZonedDateTime` **objects**, not strings; `success:false` initially. |
| `process_submitted` | `trace` | `status:"SUBMITTED"`, `exit:2147483647` (=unset), `start:0`. |
| `process_started` | `trace` | `status:"RUNNING"`. |
| `process_completed` | `trace` | terminal per task — adds `complete`,`duration`,`realtime`,`error_action`. `status` ∈ COMPLETED / FAILED / ABORTED; `exit` = code (or `2147483647` if aborted before run). |
| `error` | **envelope only — EMPTY** | no message, no trace. **Do not** rely on it for failure detail. |
| `completed` | `metadata.workflow` | workflow-level terminal; the authoritative disposition. |

**Key `trace` fields:** `task_id`, `process` (e.g. `"SAY"`), `tag` (the `tag` directive value — e.g.
`"gamma"`; where a study/EE id would land), `status`, `exit`, `attempt`, `native_id`, `workdir`,
`hash`, `name` (`"SAY (gamma)"`), `submit`/`start`/`complete` (epoch ms), `duration`/`realtime` (ms).

**Terminal-disposition rule (the one correctness point):**
- Authoritative success = **`completed` event → `metadata.workflow.success`** (boolean). Not the
  `error` event (empty), not inferred from per-task traces.
- Failure message: prefer **`errorReport`** (always populated on failure), fall back to `errorMessage`.
  `exitStatus` may be `null`.
- Two failure modes seen: **runtime task failure** (`exit≠0`) → `completed` has `exitStatus`=code,
  `errorMessage`=captured stdout/stderr, `errorReport`=full report, plus a `process_completed`
  `status:FAILED`; **script/wiring error** → `completed` has `exitStatus:null`, `errorMessage:null`,
  only `errorReport` set, and **no** FAILED task trace (task never ran).

**Proposed `kind` mapping (R11: one run = one job):**
- `completed` + `success:true` → `completed` (job DONE).
- `completed`/`error` + `success:false` → `error` (job FAILED; message = `errorReport`).
- `process_started` / `process_completed` → `progress`/`stage` (optional timeline; label from `process`+`tag`).
- `started`, `process_submitted` → ignorable (or a single `stage` "started"). The `error` event alone → ignore (wait for `completed`).

---

## Design direction — `submit`/`poll`/`cancel` **BUILT 2026-07-16**

`NextflowSlurmScheduler` (replaces the stub, profile `scheduler-nextflow`), **one run per EE (R11)**.
Built: `NextflowSlurmScheduler` (wires config + EE lookup + SSH + file writes), `NextflowSlurmCommandBuilder`
(pure command/parse core, unit-tested), `SshCommandRunner`/`SshCommandRunnerImpl` (the sole cluster edge).
Tests: `NextflowSlurmCommandBuilderTest` (9) + `NextflowSlurmSchedulerTest` (9, fake SSH + mocked EE +
temp work-dir). **Not yet built:** per-pipeline `maxConcurrent` default wiring (O8), `readLog`/`readArtifact`
off the mount (later), and the end-to-end cluster run.
- **submit** (per `PipelineJob` / one EE): build a single-study samplesheet (or `--study_names`) for that EE
  under its own work-dir; SSH to a submit node; **`sbatch --parsable <wrapper.sh>`** where the wrapper runs
  `nextflow run <checkout>/main.nf -profile conda -params-file params.<hs|mm>.json --input <samplesheet-for-this-EE> -process.executor slurm -with-weblog <gemma>/rest/v2/internal/pipeline/jobs/{id}/weblog -with-trace -resume -work-dir /space/gemmaData/pipeline/<jobId>`; capture `sbatch`'s stdout (the head-job id) and return `SchedulerHandle(NEXTFLOW, <slurmHeadJobId>)` (R13). **Per-job** work-dir (`<jobId>`, not `<batchId>`) so each run's `-resume` cache is isolated.
- **live status** *(BUILT)*: Nextflow `-with-weblog <gemma>/internal/pipeline/jobs/{id}/weblog` → `InternalPipelineWebService.postWeblog` → `NextflowWeblogTranslator` → `recordEvent`. One run = one job ⇒ events map directly, no study-tag routing (R11).
- **concurrency**: batch `maxConcurrent` (O8 default) bounds concurrent runs = concurrent head jobs; Slurm queues the tasks within each run.
- **cancel**: `scancel <slurmHeadJobId>` over SSH (R13).
- **poll** (reconciler fallback): `squeue` then **`scontrol show job <slurmHeadJobId>`** over SSH. *NOT `sacct`* — Slurm **accounting is disabled** on the cluster (`AccountingStorageType=(null)`; verified 2026-07-16), so `sacct` always returns empty. `scontrol` needs no accounting but forgets a job after `MinJobAge` (300 s); beyond that the terminal state comes from the weblog push (primary), with the work-dir `trace.txt`/`.nextflow.log` on the mount as a possible deep fallback later.
- **results**: unchanged — `GEMMA_UPLOAD` → `gemma-cli`.
- Testable pre-cluster: command assembly, per-EE samplesheet writer, and the weblog→`recordEvent` translation are unit-testable against the `ScriptedMock`; only the final end-to-end run needs the node.

---

*Update log:*
- *2026-07-15 — created; R1–R7 recorded from the design discussion, O1–O6 open.*
- *2026-07-15 — added R8 (identity model: SSH-target account decoupled from container uid; config-driven; discrete-user pattern maps forward) and R9 (dev under current account, hardening deferred, track grants below); refined O1 (reuse existing pipeline account; app-user distinction); added O7 (no-`/space`-mount / pure-SSH option + same-uid write rule) and the "Privileges … running checklist" seeded with the known grants.*
- *2026-07-15 — R10: `/space/gemmaData` bind-mounted (Ogan's subjective preference, external accessibility); resolves O7 toward the mount — Gemma reads logs/artifacts off the mount, SSH stays the write/submit path, same-uid rule retained.*
- *2026-07-15 — R11: one `nextflow run` per EE (option a); resolves O2 (no study-tag routing, fits per-job SPI as-is). Added O8 (need a default/global `maxConcurrent` cap — currently null = launch all — to bound concurrent driver processes). Updated the proposed design direction to per-EE runs with per-job work-dir.*
- *2026-07-15 — R12: per-pipeline default `maxConcurrent` over the per-batch column (reconciles §3.4 mechanism with D2 policy); sc-annotation default = 25, with the caveat that it caps concurrent runs (not tasks, unlike the pipeline's `queueSize=25`); global ceiling deferred. Resolves O8.*
- *2026-07-15 — O3: captured the real `-with-weblog` payload (Nextflow 24.10.3) via a local sink + tiny pipeline; added the "Weblog payload shape" reference (envelope, event enum, trace fields, terminal-disposition rule, kind mapping). Key gotchas: `error` event is empty; success comes from `completed.metadata.workflow.success`; failure msg from `errorReport`. Remaining: confirm cluster Nextflow version + that sc-annotation sets `tag`=study id.*
- *2026-07-15 — O6: cleanup policy deliberately deferred (Ogan wants to see real output first); no-cleanup is the safe default (resume depends on work-dir persistence), only risk is disk growth — monitor `/space` headroom.*
- *2026-07-15 — O5: checkout path to be a config property; interim default = local copy `/home/omancarci/git_repos/sc-annotation-pipeline`; canonical `/space/grp/...` location pending Rachel. Config flip, not code.*
- *2026-07-15 — O3 fully settled: cluster Nextflow version confirmed identical (24.10.3, shared `/space/opt/bin` binary → captured payloads authoritative); sc-annotation sets `tag "$study_name"` on study-level processes. Translation + fixture test buildable now.*
- *2026-07-15 — O9 added (+ R11 perf caveat): per-EE runs duplicate the organism-level `PREPARE_REFERENCE` (scVI setup + census download) that a combined run shares; no `storeDir` today. Fix = add `storeDir`/pre-stage keyed on (organism, census_version) — a **pipeline-side change for Rachel**. Keep R11 + storeDir recommended. Also recorded the O2(b) combined-run approach as a live alternative.*
- *2026-07-15 — R13 (resolves O4): A2 — `sbatch --parsable` the nextflow head process as a Slurm job; handle = Slurm head-job id; cancel `scancel`, poll `squeue`/`sacct`. Drivers run on compute nodes (bounds R11 footprint). Dependency: partition/QOS for many small long-lived head jobs. Updated R12 (drivers on compute nodes), design direction, and privileges checklist (head-job submit, scancel, sacct).*
- *2026-07-16 — O3 BUILT: `NextflowWeblogTranslator` + 10-case unit test (real captured fixtures); REST ingest `POST /internal/pipeline/jobs/{id}/weblog` + 5-case Jersey test. Weblog URL in the design direction repointed `/events` → `/weblog`. All green (gemma-core + gemma-rest).*
- *2026-07-16 — O9 resolved (Rachel): a cached `PREPARE_CACHE` reference exists/can be done, so per-EE runs don't redo census+scVI — reference-sharing concern off the table, R11 stands. Updated R11 caveat + appendix.*
- *2026-07-16 — `NextflowSlurmScheduler` BUILT: `submit`/`poll`/`cancel` via `sbatch --parsable`/`squeue`+`sacct`/`scancel`, one run per EE, samplesheet+wrapper written to the `/space` mount, `-with-weblog … /weblog`. Pure `NextflowSlurmCommandBuilder` + `SshCommandRunner` seam (only impure edge). 18 unit tests green; existing pipeline mock ITs unaffected. Remaining: O8 default wiring, log/artifact off mount, end-to-end run.*
- *2026-07-16 — O8 BUILT: `PipelineDefaults` (sc-annotation=25) + `submit` stamps the pipeline default when no explicit cap; unknown pipelines unchanged (unlimited). `PipelineDefaultsTest` + 2 mock-IT cases green.*
- *2026-07-16 — Cluster verification (read-only, `omancarci@scratchy`): SSH pubkey ✓, sbatch/squeue/sacct/scancel present ✓, `/space/gemmaData` group-writable via `pavlab` ✓, O5 checkout + params files confirmed at `/space/grp/Pipelines/sc-annotation-pipeline`, nextflow at `/space/opt/bin/nextflow` (24.10.3) but not on non-login `PATH` (wrapper needs abs path — follow-up). `pavlab-sa` group / `tomcat` owner = O1 account candidates. No jobs submitted.*
- *2026-07-16 — Smoke test Step A (real `sbatch` of a trivial `sleep` job): `sbatch --parsable`→id, `squeue -o %T` PENDING→RUNNING, ran on a compute node & completed, `scancel` OK. **Finding:** Slurm **accounting is disabled** (`AccountingStorageType=(null)`) so `sacct` is always empty → swapped the poll fallback from `sacct` to `scontrol show job` (works without accounting; JobState token; MinJobAge 300 s window). Code + tests updated (builder `scontrolShowJobCommand`/`parseScontrolState`; scheduler poll ladder). 19 tests green.*

---

## Appendix — run-granularity comparison (R11 per-EE vs. O2(b) combined run)

The trade study behind R11. **Chosen: one run per EE (R11)**, paired with O9's `storeDir` fix which
neutralizes the combined run's only real advantage (native reference sharing). The combined run only
pulls ahead if adding a `storeDir` to the pipeline proves infeasible (confirm with Rachel).

| Axis | One run per EE (R11, **chosen**) | One run per batch (O2(b), alternative) |
|---|---|---|
| Correlation | Trivial — 1 run = 1 job, no routing | Needs study-tag → job routing |
| SPI fit | Existing per-job SPI unchanged | Needs batch-submit path / attach convention |
| Reference (scVI + census) | **Redone per study** unless O9 `storeDir` | **Shared once** natively — the combined run's big win |
| Driver footprint | N head jobs | 1 head job |
| Slurm task throttle | Per-run `queueSize` → up to N×25 tasks | One shared `queueSize=25` |
| Failure isolation | One study fails → only its job fails | One study's failure can affect the whole run |
| Cancel one EE | `scancel` its head job | Hard — can't cleanly cancel one study from a shared run |
| Retry one EE | Independent re-run | Re-runs touch the shared run / rely on `-resume` |

*Read: every axis except reference-sharing favours per-EE; O9's cached `PREPARE_CACHE` removes the
reference-sharing gap (confirmed feasible with Rachel), so per-EE wins outright.*
