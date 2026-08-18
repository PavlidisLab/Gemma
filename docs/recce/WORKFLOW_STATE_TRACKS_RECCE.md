# Workflow state: the curation/analysis fork — proposal

**Status:** proposal. No code touched. Needs a team decision before anything lands.
**Proposed owner:** Ogan Mancarcı (`oganm`). **Branch:** `phase2-acl-migrate` @ `20e8433db2`.
**Prompted by:** `handoffs/GEB_HANDOFF_2026_08_11_WORKFLOW_STATE_TRACKS.md` — the
manuscript figure `fig_gemma2_2_workflow` could not be drawn honestly against
`WorkflowState`.
**Reviewed:** agents side agreed the shape and withdrew its two-column proposal
(`handoffs/GEB_REPLY_2026_08_11_WORKFLOW_STATE_TRACKS.md`), with one correction
now folded in — the curator/agent split is `TicketType`, not `TicketMode` (§2).
**Related:** `AUDIT_AS_WORKFLOW_RECCE.md` (the ticket layer, shipped from it),
`WORKFLOW_GROUPS_RECCE.md` (unbuilt), `PIPELINE_COMPUTE_AND_JOB_MANAGEMENT.md`,
`docs/audit/PIPELINESTATUS_WIRE_AUDIT.md`.

---

## 0. The problem

**How do we track an experiment through curation?** We need to know where every
experiment stands, for the whole corpus, at any moment, without asking a person.

Questions the system has to answer:

* Where is GSE12345 right now?
* What is waiting on a curator? — a staffing question.
* What is waiting on a re-run? — a compute question.
* What is curated but not processed, or processed but not curated?
* Is the design settled enough to run the DEA?
* What is ready to release?
* Why has this one not moved in three weeks, and which ticket is holding it?

Two things make this harder than a status field. **The work forks:** after
preboarding, curation (design, tags, publication) and data processing (import,
batch, preprocessing, DEA) run concurrently, so an experiment is in two places
at once. **There are two QC gates, not one:** the curation side should pass most
proposals automatically and route only flagged ones to a person, while the data
side is cleared by a re-run. Those two queues are staffed differently and must
not share a bucket.

---

## 1. Recommendation

**Track the two tracks separately: name a vocabulary for each (§2) and compute
both from tables we already have (§6). Add no state columns.** Keep the single
indexed `WORKFLOW_STATE` column as a stored projection with one writer,
recomputed on events that already fire — never asserted by callers.

The agents-side handoff proposes two new `VARCHAR(32)` columns instead. Against
that: **nothing maintains `WorkflowState` today.** Production writes it in
exactly two places, both in the preboarding path
(`PreboardedExperimentServiceImpl:90,173`). `Curate`, `Process`, `Audit` and
`Public` are set only by a human calling `PUT /datasets/{id}/workflow`; the rest
of the corpus still reads `Loaded` from the V10 backfill. Two more columns
would be two more fields nobody writes — and the state they'd hold is already
stored in `ANNOTATION_SET`, `TICKET`, `PIPELINE_JOB` and `/pipelineStatus`.

Steps 1 and 2 below need no Flyway migration.

---

## 2. The states

### `WorkflowState` — "where is this overall" (8 values, unchanged)

| Value | Means | Written by, today | Proposed |
|---|---|---|---|
| `Discovery` | seen in a scrape, not triaged | nothing | scraper |
| `Candidate` | worth loading, not preboarded | nothing | curator / agent |
| `Preboarded` | skeleton row, no data | preboarding service | unchanged |
| `Loaded` | real EE, data present | preboarding service | unchanged |
| `Curate` | either track in flight | manual PUT | derived |
| `Process` | *(redundant under the fork — see Q3)* | manual PUT | derived or retired |
| `Audit` | either track at a gate awaiting a human | manual PUT | derived |
| `Public` | released, both tracks settled | manual PUT | derived from ACL |

### `CurationTrackState` — "where is the annotation work" (5 values)

| Value | Means | Computed from |
|---|---|---|
| `NotStarted` | nothing proposed | no `ANNOTATION_SET` row |
| `Proposed` | a hypothesis is on file | `ROLE='PROPOSAL'`, `FINALIZED_AT IS NULL` |
| `QcPassed` | machine check ran, did not flag | `Proposed` and no open curator-facing ticket — **see Q1** |
| `AwaitingCurator` | a person must look | open `TICKET` (`OPEN`/`IN_PROGRESS`) of a **curator-facing `TYPE`** with a `TICKET_TARGET` for this EE |
| `Committed` | accepted annotation is live | `ROLE='SNAPSHOT'` with `FINALIZED_AT`, newer than the latest `PROPOSAL` |

Precedence when several apply: `AwaitingCurator` > `QcPassed` > `Proposed`. A
fresh proposal on a committed dataset moves it back to `Proposed`.

### `AnalysisTrackState` — "where is the data work" (5 values)

| Value | Means | Computed from |
|---|---|---|
| `NotStarted` | no expression data | no vectors / still `Preboarded` |
| `Loaded` | data present, preprocessing incomplete | `/pipelineStatus`: `preprocess`/`pca` = `notRun` |
| `Processing` | a run is in flight | `PIPELINE_JOB.STATE ∈ {PENDING, QUEUED, RUNNING}` |
| `Processed` | every applicable step ok | all six steps ∈ `{ok, notApplicable}` |
| `Failed` | a step failed, nothing re-run since | any step `failed`, or `PIPELINE_JOB.STATE='FAILED'` |

The six steps are already named on the wire — `batchInfo`, `preprocess`, `pca`,
`dea`, `coexpression`, `missingValue` — with status `ok | failed | notRun |
notApplicable`. The track state is a roll-up of them, not a second vocabulary.

### The two QC gates, named separately

| Gate | Track | Cleared by | Query |
|---|---|---|---|
| **Curation QC** | curation | automated triage; only flagged proposals reach a person | open ticket of a curator-facing `TICKET.TYPE` |
| **Data QC** | analysis | a re-run, or a curator accepting a diagnostic | `PIPELINE_JOB.STATE='FAILED'` / step `failed` |

The curator/agent split is **`TicketType`**, whose javadoc assigns the audience:
`CURATION`, `QUALITY_REVIEW`, `BATCH_INFO_NEEDED`, `REALIGNMENT_NEEDED` are
curator work; `PRELOAD` and `LITERATURE_SEARCH` are agent work.

> **Not `TicketMode`.** An earlier draft used it. `TicketMode {MANUAL, AUTO}`
> is about **action chaining** — whether the server schedules the ticket's next
> action itself once all targets are `DONE` (`TicketMode.java:14-30`) — not
> about who is blocked. It also defaults to `MANUAL` (`Ticket.java:113`), so
> "open and `MANUAL`" matches nearly every ticket, including agent work.
> Correction from the agents side, `GEB_REPLY_2026_08_11`.

If `TicketType` keeps growing, the durable answer is an explicit
`TICKET.AUDIENCE {CURATOR, AGENT}` column. Start with type; add the column when
the list makes the implicit set unmanageable. `TicketTargetStatus {NOT_DONE,
UNDERWAY, DONE}` gives per-target progress either way.

### The fork

```
                                     ┌──────────── CURATION TRACK ─────────────┐
                                     │ NotStarted → Proposed → QcPassed ─┐     │
                                     │                   ↓               ↓     │
                                     │            AwaitingCurator → Committed  │
   Discovery → Candidate → Preboarded┤                                         │
        (one writer, total order)    │                                         ├→ Public
                                     │ ┌───────── ANALYSIS TRACK ───────────┐  │
                                     │ │ NotStarted → Loaded → Processing ─┐ │  │
                                     │ │                  ↑         ↓      ↓ │  │
                                     │ │                Failed ← ── Processed│  │
                                     │ └────────────────────────────────────┘  │
                                     └─────────────────────────────────────────┘
                                       one coupling point: the committed design
                                       is what the DEA contrasts
```

---

## 3. Implementation plan

### Step 1 — no schema; answers the queue question (~1 day, ~250 LOC)

1. Make "waiting on a person" expressible on `GET /tickets`. It filters on
   `openOnly / assignee / priority / type / state / targetType / updatedSince`
   (`TicketsWebService:145-157`), but `type` is single-valued
   (`TicketsWebService:151`), so a caller cannot ask for *any* curator-facing
   type. Make `type` repeatable, and add `audience=curator|agent` resolving to
   the type list in one place so the split has a single definition.
2. Fill the two `TODO(ticket-integration)` holes in
   `WorkflowServiceImpl.queue()`: populate `currentAssignee` and
   `ticketCountOpen` (`:200`, both hardcoded null/0), and make the `assignee`
   filter filter instead of returning an empty page (`:153`).
3. Add `blockingTrack` (`curation | analysis | none`) + `blockingReason` to
   `WorkflowQueueEntry` — the field a curator actually sorts by.
4. Make `state` optional on `GET /workflow/queue` (`WorkflowWebService:277`
   currently 400s without it), so "everything blocked" is one call.

### Step 2 — one writer for the coarse state (~2–3 days, ~600 LOC)

5. `WorkflowServiceImpl.recompute(Investigation)` implementing §2's derived
   column from §6's sources.
6. Four listeners calling it: ticket opened/resolved, annotation set finalized,
   pipeline job terminal, ACL visibility change. Recompute on write, never on
   read — `/pipelineStatus` walks audit events and is too slow for a list query.
7. Expose both track states as computed fields on `GET /datasets/{id}/workflow`.
   This is what the UI's existing `CurationTrack` / `AnalysisTrack` types bind to.
8. Finish the deferred V10 backfill by running the recompute across the corpus.
   Under this design it's a job, not a curator-signoff SQL mapping — which is
   the main reason it never happened.
9. Write `{from, to, ticketId, reason}` into `AUDIT_EVENT.PAYLOAD` instead of
   the note string (`WorkflowServiceImpl:96`), making transition history
   queryable. No new table — V2 already added the JSON column.

`advance()` keeps working unchanged for the pre-fork segment
(`Discovery → … → Loaded`) where a total order genuinely holds. In the forked
region it stops taking a caller-supplied target and recomputes.

### Step 3 — only if 1 and 2 leave a real query unanswered

10. Persist the track states in a child table `INVESTIGATION_TRACK_STATE
    (investigation, track, state, entered_at)` rather than widening
    `INVESTIGATION` — a third track (single-cell? platform?) is plausible.
    ~500 LOC **and a Flyway migration, so ask first.**

### Before step 2, measure two things

- The `/pipelineStatus` audit-event walk against prod-shape data
  (`scripts/perf_search.py` is the probe pattern) — it sets the projection
  refresh budget.
- How many prod rows are stale at `Loaded`, to size the backfill job
  (read-only port-forward per `memory/reference_production_database.md`).

---

## 4. Decisions needed

**Q1 — Where does the machine-QC verdict get recorded?** *(the one genuine
gap.)* The agents side computes triage — flagged or not — and stores nothing.
`ANNOTATION_SET` has `FINALIZED_AT` (curator finalization) but no status or
disposition column; those lived on `AGENT_PROPOSAL`, which V21 dropped.

  a. **A ticket** — the agent opens a `CURATION` ticket when it flags, and
     nothing when it passes. "Flagged" is then "an open curator-facing ticket
     exists", already the definition of needing attention. No schema change.
     **Recommended; agreed by the agents side.**
  b. A `STATUS`/`DISPOSITION` column back on `ANNOTATION_SET` — migration;
     re-adds what V21 just removed.
  c. Leave it agents-side and have Gemma ask — rejected; the queue stops being
     a single query.

**Q2 — Does `QcPassed` earn its own box?** Under (a) it is "proposed, no open
ticket". Folding it into `Proposed` is a presentation choice with no storage
consequence.

**Q3 — Does `Process` survive?** Under the fork `Curate` and `Process` are no
longer sequential — they are the two tracks. Retiring the value costs a
migration (the string is stored verbatim); redefining it as "analysis in
flight, curation settled" costs nothing. Recommend redefining now, retiring at
a later consolidation.

**Q4 — `Preboarded → Curate` edge.** The agent can propose a design from the
GEO record before data lands, but `Preboarded` exits only to `Loaded` or
`Candidate` (`WorkflowState.java:67`), forcing the data track to go first.
Free under this proposal; needs a new edge under the handoff's options A/B.

**Q5 — Who owns the recompute?** `WorkflowServiceImpl` is the natural home but
would then depend on `TicketService`, `AnnotationSetService` and the job layer.
If that dependency direction is objectionable, a `WorkflowProjectionService` in
the same package is the alternative. Either way it must be **idempotent and
directly callable**, not listener-only: agent batches post proposals in bursts
and would rather call recompute once at the end than depend on N listener
firings settling correctly (agents-side request).

---

## 5. Why this shape and not the others

The handoff offers three options. Its own objection to its pick (A) is "two
sources of truth unless the derivation lives in exactly one place" — and if the
derivation must live in one place anyway, the derived values do not need
columns. They need a computation and a cache.

| Option | Verdict |
|---|---|
| **A** — enum + two track-state columns | Right vocabulary, wrong storage. Requires every writer (agents, pipeline, curator UI) to remember to set them. The evidence says writers don't remember: that is why `WORKFLOW_STATE` is stale corpus-wide. |
| **B** — grow the enum (`CurationQc`, `CuratorReview`, `DataQc`) | Rejected. Still cannot say "both tracks in flight" — you must pick which track the value names, which is the lie the figure was about to tell. |
| **C** — a state per track, no global state | Rejected. Breaks every consumer and the cheap coarse query the V10 index exists for. |
| **This** — A's vocabulary, derived, one stored projection | Writes nothing new. A dataset cannot be `AwaitingCurator` with no open ticket, because the open ticket *is* the definition. |

The honest cost: four listeners and a recompute method versus A's two setters.
More code up front. The failure mode is a stale cache with a refresh path,
rather than a field nobody writes.

Two smaller handoff ideas fold in for free: splitting `Audit` into machine and
human checks is `TicketType`'s existing audience split; a transition history keyed by
ticket is `AUDIT_EVENT.PAYLOAD` (V2, shipped), not a new table. And
`Public → Curate` mislabelling a reader-reported batch confound stops being a
transition question — it opens a ticket and the projection recomputes.

---

## 6. Where the state already lives

Most of this landed after the handoff's inventory was written, which is why it
proposes building it.

| Concern | Already stored in | Ref |
|---|---|---|
| Proposal / draft / committed annotation | `ANNOTATION_SET` (`ROLE ∈ {PROPOSAL, DRAFT, SNAPSHOT}`, `SOURCE`, `KIND`, `FINALIZED_AT/BY`) | `V20`; `V21` dropped `AGENT_PROPOSAL` + `CURATION_DRAFT` |
| Per-element disposition in a draft | derived by diffing payload vs parent payload | `CurationDraftDispositions` |
| "A person must look at this" | `TICKET` (`STATE`, `TYPE` — carries the audience, `PRIORITY`, assignee) + `TICKET_TARGET` (`TARGET_TYPE`, `TARGET_ID`, `STATUS`) | `V3`, `V19` |
| Open work for one dataset | `GET /datasets/{id}/tickets` | `DatasetsWebService:1620` |
| Legacy `troubled` / `needsAttention` | deprecated shim that opens/resolves tickets | `DatasetsWebService:1977` |
| Per-step analysis status | `GET /datasets/{id}/pipelineStatus`, derived from audit events | `DatasetsWebService:3590` |
| In-flight compute | `PIPELINE_JOB.STATE` + `PIPELINE_JOB_BATCH` | `V18` |
| Structured event payloads | `AUDIT_EVENT.PAYLOAD` (JSON) | `V2` |
| Released / not | ACL, `securityService.isPublic(ee)` | `DatasetsWebService:3385` |

Both queries the handoff says must not share a bucket are already indexed, with
no migration:

* **waiting on a curator** — `TICKET_STATE` (`V3:33`) +
  `TICKET_TARGET_LOOKUP (TARGET_TYPE, TARGET_ID)` (`V3:43`) + `TICKET_TYPE`
  (`V3:32`), filtering to curator-facing types.
* **waiting on a re-run** — `IDX_PIPELINE_JOB_STATE` (`V18:55`).

One open wire question, for the UI handoff rather than this one: the UI's
`StepStatus` vocabulary (`not_run, ok, failed, in_progress, needs_attention,
na`) has two values the server's does not.
