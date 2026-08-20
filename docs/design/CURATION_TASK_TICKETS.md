# Curation-task tickets — a durable, curator-facing record of curation work performed

**Status:** specification only. Nothing is implemented. An implementation was built against this design and
deliberately discarded before commit (see [§9](#9-what-was-built-and-thrown-away)); this document is what survives.

**Scope:** a new `TicketType` that records curation work that *was performed*, with the obsolete ontology term
correction as its first producer.

**Baseline:** `c6a8dc51845b693a5b027acbdb754b7ad88ba2ea`. All line numbers below are against that commit.

---

## 1. The problem

Two problems with one shape.

**The result of a maintenance run is unreadable.** `ObsoleteTermCorrectionTaskImpl` returns a rich
`ObsoleteTermCorrectionResult` into a `TaskResult`, and nothing ever reads it:

- `TaskStatusValueObject` (`gemma-rest/.../rest/TaskStatusValueObject.java:31-67`) exposes `taskId`,
  `experimentId`, `step`, `status`, `submittedAt`, `startedAt`, `completedAt`, `message`, `error`. There is no
  field for `TaskResult.getAnswer()`. Polling `/tasks/{taskId}` can tell you a run finished; it cannot tell you
  what it did.
- The job is then thrown away. `SubmittedTasksMaintenance` (`gemma-core/.../core/job/SubmittedTasksMaintenance.java:19,28,82-88`)
  runs every 120 s and removes any COMPLETED or FAILED task whose finish time is more than
  `MAX_KEEP_TRACK_AFTER_COMPLETED_MINUTES = 10` in the past.

So ten minutes after a run, the only trace is one log line
(`ObsoleteTermCorrectionServiceImpl.java:98-100`). A dry run you cannot read is close to useless, and the dry run
is exactly what gates doing a real one safely.

**Curators are never told.** When a run rewrites `EFO_0000546 "injury"` → `MONDO_0021178` across 24 experiments,
nobody who curates those datasets finds out.

Paul's framing, across several messages:

> when we run the ontology term updater, or other such tasks, we need to generate a report for the curator. we
> need to store information -- it's a ticket perhaps. so the curator could see 'highlights of ontology updates you
> might want to know'

> tickets are the natural mechanism

> but the ticket also tracks what was done … it has a separate use for tracking 'overall' curation tasks … or ad
> hoc curation tasks

> they are just passive receivers -- even if they are the same person driving claude to do the task

> I mean, the curator can't drive the job from the UI

> they can say "open a ticket for this obsolete term check" (maybe there _should_ be some controls from the UI,
> but that's a future ask)

The operating flow is therefore: **the job is fired through the admin API — typically by Claude or an agent acting
for a curator — and the UI's only role is to show what happened afterwards.** The reader is a passive receiver
even when they are the person who asked for the run.

---

## 2. What exists today

This section is the part that will save whoever picks this up. Everything here was read at the baseline commit.

### 2.1 What a ticket is

`Ticket` (`gemma-core/.../model/common/auditAndSecurity/curation/Ticket.java:70`) extends `AbstractAuditable`, so
it carries its own governance audit trail in addition to its domain event log. Its fields (line numbers are the
field declarations):

| field | line | notes |
|---|---|---|
| `type` | 74 | `TicketType`, `VARCHAR(64)` |
| `state` | 78 | `TicketState`, defaults `OPEN` |
| `priority` | 82 | `TicketPriority`, defaults `NORMAL` |
| `dueDate` | 86 | nullable |
| `reporter` | 90 | `Contact`, **NOT NULL** |
| `assignee` | 95 | `Contact`, nullable |
| `createdAt` / `updatedAt` | 98, 101 | `updatedAt` is the dashboard sort key |
| `externalIssueUrl` / `externalIssueSyncState` | 105, 109 | GitHub-issue sync |
| `mode` | 113 | `TicketMode` MANUAL / AUTO — about *action advancement*, not authorship |
| `targets` | 116 | `Set<TicketTarget>`, cascade ALL, orphan removal |
| `events` | 123 | `List<TicketEvent>`, `@OrderBy("occurredAt")` |

The **title** is the inherited `NAME` column (`getTitle()`/`setTitle()`, lines 159-165). The **body** is the
inherited `DESCRIPTION` column (`getBody()`/`setBody()`, lines 174-180), documented at line 169-172 as *"what the
detail page renders as multi-line body and the dashboard clamps to 2 lines"*.

### 2.2 The enums, and what is free to extend

| enum | values | column |
|---|---|---|
| `TicketType` | `BATCH_INFO_NEEDED`(25), `REALIGNMENT_NEEDED`(31), `QUALITY_REVIEW`(33), `PRELOAD`(40), `CURATION`(46), `LITERATURE_SEARCH`(53), `GENERIC`(55) | `VARCHAR(64)` |
| `TicketState` | `OPEN`(22), `IN_PROGRESS`(23), `RESOLVED`(24), `CANCELLED`(25) | `VARCHAR(32)` |
| `TicketPriority` | `LOW`, `NORMAL`, `HIGH`, `URGENT` (20-23) | `VARCHAR(16)` |
| `TicketMode` | `MANUAL`(34), `AUTO`(35) | `VARCHAR(16)` |
| `TicketTargetType` | `EXPRESSION_EXPERIMENT`(22), `ARRAY_DESIGN`(23), `FACTOR_VALUE`(32), `GEO_SCRAPE_WATERMARK`(39), `BIBLIOGRAPHIC_REFERENCE`(48) | `VARCHAR(32)` |
| `TicketTargetStatus` | `NOT_DONE`(34), `UNDERWAY`(35), `DONE`(36) | `VARCHAR(16)` |
| `TicketEventType` | `OPENED`, `ASSIGNED`, `COMMENTED`, `STATE_CHANGED`, `RESOLVED`, `CANCELLED`, `REOPENED`, `TARGET_STATUS_CHANGED`, `COMMENT_EDITED` (22-37) | `VARCHAR(64)` |

**All of these are `@Enumerated(EnumType.STRING)` into plain VARCHAR columns with no CHECK constraint** — see
`db/migration/mysql/V3__ticket_layer.sql` and `V19__ticket_mode_and_target_status.sql`. `TicketType`'s own javadoc
(lines 17-19) says so explicitly: *"New values can be added without a schema migration."* Same for
`TicketTargetType` (lines 17-20). **This is the single most useful fact in this document: the design below needs
no migration.**

### 2.3 Targets — how a ticket points at a dataset

`TicketTarget` (`.../curation/TicketTarget.java`) is `(ticket, targetType, targetId, status)`. `targetId` is a
**bare FK, deliberately not JPA-mapped and not DB-constrained**, so one composite index
`TICKET_TARGET_LOOKUP (TARGET_TYPE, TARGET_ID)` serves "tickets for this entity" across heterogeneous target
tables. `TICKET_TARGET` is `UNIQUE (TICKET_FK, TARGET_TYPE, TARGET_ID)` — a duplicate target id fails the insert
rather than being silently ignored.

`TicketTarget.hashCode()` is `Objects.hash(targetType, targetId)`, so distinct ids do not collapse in a `HashSet`.

### 2.4 Events and the payload column

`TicketEvent` is `@Immutable`, append-only, `(ticket, actor, occurredAt, type, payload)`. `payload` is
`@Lob @Column(columnDefinition = "json")`.

- MySQL (`V3__ticket_layer.sql`): `PAYLOAD JSON NULL`.
- H2 (`h2/V5__ticket_layer.sql:46`): `PAYLOAD CLOB NULL`, with a comment explaining the divergence.

⚠️ **A JSON column rejects non-JSON.** `GeoScrapeServiceImpl.openScrapeBatchTicket` (line 662) passes
`buildTicketNote(...)` — a plain string like `matched: foo×3, bar×1` — into `addComment`, which stores it verbatim
into `PAYLOAD`. That looks like a latent bug on MySQL. **It was not verified against a live MySQL instance**, and
it is out of scope here, but anything new writing to `PAYLOAD` must write real JSON.

### 2.5 How tickets are created

`TicketService.openTicket(reporter, type, title, targets)` (`TicketService.java:53`), implemented at
`TicketServiceImpl.java:82-106`. It:

- asserts `reporter != null`, `type != null`, `hasText(title)`, and **`Assert.notEmpty(targets, "A ticket needs at
  least one target.")`** (line 88) — a ticket cannot exist without at least one target;
- seeds an `OPENED` `TicketEvent`;
- writes a companion `TicketOpenedEvent` on the governance audit trail inline (lines 100-104), because the
  `@Audited` aspect inspects method *arguments* and `openTicket` constructs the `Ticket` internally.

**`openTicket` does not accept a body.** The two existing workarounds:

- `TicketsWebService.createTicket` (`TicketsWebService.java:445`) calls `openTicket`, then sets
  `priority`/`dueDate`/`body`/`mode` on the returned entity and calls `updateMetadata(created, "body, …")`.
- `GeoScrapeServiceImpl` appends the note as a `COMMENTED` event instead, with a comment at line 657 saying
  plainly that `openTicket` has nowhere to put it.

### 2.6 System-generated tickets already exist — two of them

There is prior art, and it is consistent.

1. **`GeoScrapeServiceImpl.openScrapeBatchTicket`** (`gemma-core/.../core/geoscrape/GeoScrapeServiceImpl.java:629-668`,
   called from line 310). One ticket per scrape batch. `reporter = userManager.getCurrentUser()` (line 634), and
   if that is null it **logs a warning and skips filing rather than failing the scrape** (lines 635-641). Type is
   `GENERIC`. Target is the `GEO_SCRAPE_WATERMARK` row. The whole thing is wrapped so a ticket failure cannot fail
   the scrape (lines 664-667).
2. **`FactorValueNeedsAttentionServiceImpl`** (`.../expression/experiment/FactorValueNeedsAttentionServiceImpl.java:101,108,116`).
   `actor = userManager.getCurrentUser()`, dedupes against existing open `GENERIC` tickets, dual-targets the
   FactorValue and its owning EE.

**There is no "generated by a process" flag anywhere on `Ticket`.** Both producers record a human `Contact` as
reporter and nothing distinguishes their tickets from hand-filed ones except the wording of the title. `TicketMode`
is *not* that flag — it is about whether the next action auto-schedules.

### 2.7 What the dashboard reads

The curation UI lives in a separate repository (`gemma-curation-ui`) and **was not inspected**. What follows is the
server-side contract it consumes; claims about pixels are marked as unverified.

- **`GET /tickets`** — `TicketsWebService.getTickets` (line 143). Filters: `openOnly`, `assignee`, `priority`,
  `type`, `state`, `targetType`, `updatedSince`; offset or cursor paging. Legacy (offset) mode sorts
  `t.updatedAt desc`, described in the code as *"the dashboard-friendly default"*. **`openOnly` defaults to
  `false`**, so a plain `GET /tickets` returns tickets in every state, newest-updated first.
- **`GET /tickets/mine`** (line 196/206) — `open` (OPEN + IN_PROGRESS assigned to me) and `recentlyResolved`
  (RESOLVED/CANCELLED assigned to me, within `resolvedWithinDays`, default 7). **Both filter on
  `assignee = me`**, so an unassigned ticket appears in neither.
- **`GET /tickets/summary`** (line 321/331) — total open plus a per-`TicketType` breakdown, for "the admin Systems
  Monitoring dashboard panel". It builds an `EnumMap` over every `TicketType` value, so a new type appears
  automatically with a zero count.
- **`GET /datasets/{dataset}/tickets`** — `DatasetsWebService.java:1820`, delegating to
  `TicketsWebService.openTicketsForExpressionExperiment` (line 732) → `loadOpenTargetingVOs` (line 719) →
  `TicketService.findOpenForTarget`.

🛑 **`findOpenForTarget` is open-only.** `TicketDaoImpl.findOpenForTarget` (lines 53-64) filters
`t.state in (OPEN, IN_PROGRESS)` (line 63). **A ticket in RESOLVED or CANCELLED is invisible on the dataset page.**
This is the single most consequential constraint on the design below.

`TicketValueObject.from(...)` embeds **all** of a ticket's targets, in the list view as well as the detail view
(events are omitted in list views for payload economy, targets are not). A ticket with thousands of targets would
bloat every listing that happens to include it.

`TicketTargetValueObject` declares `displayLabel` and `displayName`, documented (lines 23-31) as server-resolved
hints so "the dashboard can render a meaningful card without a follow-up fetch". **A repo-wide grep finds no code
that ever sets them.** Today every ticket target renders as a bare numeric id unless the UI does its own lookup.

### 2.8 Ticket type has semantics beyond labelling

`CurationDetailsServiceImpl` (`.../curation/CurationDetailsServiceImpl.java:51-54`) folds open tickets down into
the legacy CurationDetails shape:

```java
private static final Set<TicketType> NEEDS_ATTENTION_TYPES = EnumSet.of(
        TicketType.GENERIC, TicketType.BATCH_INFO_NEEDED, TicketType.QUALITY_REVIEW );
```

`needsAttention(targetType, targetId)` (line 68) returns true if **any open ticket of one of those types** targets
the entity. `lastUpdated` (line 111) folds the max event timestamp across open tickets for the target.

⇒ **Filing a corpus-wide record as `GENERIC` — the type both existing system producers use — would flip
`needsAttention = true` on every dataset the run touched.** That alone rules out reuse of `GENERIC`.

Mitigating fact: `CurationDetailsServiceImpl` currently has **no production callers** (grep for
`curationDetailsService.` across `gemma-core/src/main` and `gemma-rest/src/main` returns nothing). The hazard is
latent, not live — it becomes real when the CurationDetails migration lands.

### 2.9 The correction run and what it already knows

`ObsoleteTermCorrectionResult` (`gemma-core/.../core/ontology/ObsoleteTermCorrectionResult.java`) carries
`dryRun`(38), `terms`(41), `experimentsAffected`(44), `characteristicsRewritten`(47), `skippedDeferred`(50),
`skippedNotCorrectable`(53), `resync`(57). Each `TermCorrection` has `fromUri`/`fromLabel`/`toUri`/`toLabel`,
`resolvedVia`(73), `characteristicsRewritten`(75), `inCategory`/`inValue`/`inPredicate`/`inObject`(77),
`experimentsAffected`(78).

`resolvedVia` is the justification, and the distinction it draws is the point of the whole feature
(`ObsoleteTermUsage.java:26-34, 84-95`): `IAO:0100001` is *the ontology asserting an exact substitute*;
`IAO:0100001-chain` is that assertion reached through obsolete intermediates; `hasAlternativeId` is a merge
record. `oboInOwl:consider` candidates are suggestions to a human and are **never** auto-applied.

🛑 **The result has experiment *counts* but not experiment *ids*.** `ObsoleteTermCorrectionServiceImpl` computes
them — `eeIds` at line 114, the union `allAffectedExperiments` at line 70 — and then discards both, keeping only
`.size()` (lines 91, 115). **The report needs the identities**, and re-deriving them afterwards is impossible: the
query is "which experiments carry this URI", and the run has just finished removing it. See [§5](#5-changes-to-existing-code).

Other facts about the run:

- Deferred terms (`ObsoleteTermCorrectionService.DEFERRED_URIS`, line 40) are skipped by a blanket run and
  honoured when named explicitly (`ObsoleteTermCorrectionServiceImpl.java:84`).
- `skippedNotCorrectable` is **only** populated for terms the caller named explicitly (see the comment at
  `ObsoleteTermCorrectionServiceImpl.java:77`). On a blanket run it is empty, which must not be read as "nothing
  was undecidable".
- `resync` is null on a dry run (line 95).
- Entry points: `GET /admin/ontologies/obsolete-terms` (`AdminWebService.java:1251/1271`, read-only, returns
  `List<ObsoleteTermUsage>`) and `POST /admin/ontologies/obsolete-terms/apply` (line 1297/1319, 202 + task id,
  `dryRun` defaults true).

### 2.10 Security context does reach the task thread

This matters because the ticket must be filed from the async task, where nobody is on the other end of a request.

- `TaskCommand`'s constructor captures `SecurityContextHolder.getContext()` into a final field
  (`TaskCommand.java:63,80-82`) and the submitter's username into `submitter` (lines 68, 87).
- `TaskRunningServiceImpl.java:142` wraps the callable in
  `new DelegatingSecurityContextCallable<>(executingTask, taskCommand.getSecurityContext())`.

⇒ `userManager.getCurrentUser()` resolves inside the task, and `TaskCommand.getSubmitter()` gives a
context-independent record of who fired it. Both are needed: the first for the `reporter` FK, the second because
the authenticated principal may be an agent or service account rather than the curator who will read the ticket.

### 2.11 An unrelated risk found on the way

`TaskCommand.MAX_RUNTIME_MILLIS = 60 * 1000` (line 46) and `maxRuntimeMillis` defaults to it (line 76).
`SubmittedTasksMaintenance` cancels any RUNNING task that exceeds it (lines 60-73).
**`ObsoleteTermCorrectionTaskCommand` never calls `setMaxRuntimeMillis`**, so a live correction across thousands
of experiments would be cancelled after 60 seconds. Other commands override it
(`ExpressionExperimentReportTaskCommand.java:37`, `IndexerTaskCommand.java:56`) — though several pass what look
like *minutes* into a field measured in *millis*, which is its own pre-existing confusion.

This is a pre-existing bug, not caused by anything here, but it is directly relevant: **a run that gets cancelled
never files a record.** Not verified against a running instance.

---

## 3. The design

### 3.1 One new ticket type: `CURATION_RECORD`

A record of curation work that **was performed**. Not a work item — the work is done and nobody is being asked to
do anything.

Every other `TicketType` names something that needs doing. This one names something that *happened*. The obsolete
ontology term correction is its **first producer, not its definition**: the same type is meant to carry an overall
curation task, or an ad hoc one ("went through thirty datasets and fixed the sex annotations").

Accordingly: **generic in the type, specific in the content.**

| generic — the record's own shape | specific — pushed into body + payload |
|---|---|
| what was done (activity name) | which terms, from what to what |
| when it ran | `resolvedVia` justifications |
| what triggered it, and that a process filed it | per-slot counts |
| which datasets it covered | resync outcome |
| real or rehearsed | requested URI scope |

A second producer then needs no schema change and no new type.

### 3.2 Fields, and where each comes from

| ticket field | value |
|---|---|
| `type` | `CURATION_RECORD` |
| `title` | `[DRY RUN] ` prefix when rehearsed, then the activity, then a one-line headline. Truncated to 255 (`TICKET.NAME` is `VARCHAR(255) NOT NULL` — an over-long title fails the insert rather than truncating), trimmed from the tail so the prefix survives |
| `body` | the account (see [§6](#6-what-a-curator-actually-sees)) |
| `reporter` | `userManager.getCurrentUser()` on the task thread |
| `state` | `RESOLVED` — see [§3.4](#34-state-resolved-and-what-that-costs) |
| `priority` | `NORMAL`, untouched — see [§4](#4-rejected-alternatives) |
| `assignee` | unset |
| `targets` | affected experiments, `EXPRESSION_EXPERIMENT`, capped at 200 |
| target `status` | `DONE` for a real run, `NOT_DONE` for a rehearsal |
| one `COMMENTED` event | the full structured account as JSON |

### 3.3 Dry runs, made unmistakable four ways

The likely first real use is: agent fires a dry run → curator reads it → curator decides whether to authorize the
real one. The dry-run ticket is the decision artifact, so it must never read as a completed change.

1. **`[DRY RUN] ` title prefix** — the only marker visible in a list view, where the reader decides whether to open
   the ticket at all.
2. **The body's first line** — `DRY RUN — nothing was written. This is a rehearsal of what a live run would
   change.` versus `APPLIED — these annotations have been rewritten in the database.`
3. **Target status** — `NOT_DONE` versus `DONE`. This is the marker that survives into any per-dataset view where
   the title is not shown, and it reuses the model's own vocabulary: `TicketTargetStatus.NOT_DONE` already means
   "in the work set, no progress recorded". Note `NOT_DONE` is the *default*, so this only discriminates as long
   as the live path sets `DONE` deliberately — worth a test.
4. **`"dryRun": true` in the detail event payload.**

The body's verbs must agree with the mode too ("Would rewrite" / "Rewrote"), or the body contradicts its own first
line.

### 3.4 State: `RESOLVED`, and what that costs

The record describes finished work, so it is filed directly into the terminal state (`openTicket` always creates
`OPEN`; one `transition(ticket, RESOLVED, reporter, reason)` follows).

**Why:** left OPEN, every record would sit in curators' open-work queues as a backlog item that can only be
cleared by closing each one by hand. It would also feed `CurationDetailsServiceImpl.lastUpdated` for every
targeted dataset once that class acquires callers — and for a *dry run*, which by definition changed nothing, that
would be a rehearsal perturbing live dataset state.

**What it costs — state this plainly to whoever implements it:** `findOpenForTarget` filters to OPEN + IN_PROGRESS
(`TicketDaoImpl.java:63`), so **a RESOLVED record does not appear on `GET /datasets/{dataset}/tickets`.** The
targets still record which datasets were involved and are visible when the ticket is opened, but the
dataset → record reverse lookup does not work.

Discovery therefore rests on:

- `GET /tickets` (default `openOnly=false`, `updatedAt desc`) — a fresh record lands at the top;
- `GET /tickets?type=CURATION_RECORD` — the type filter is the durable route back;
- `GET /tickets/summary` — a per-type count, automatically.

If the dataset page turns out to matter more than the open-queue hygiene, the fix is a `findForTarget` that is not
state-scoped (or one that includes terminal states for this type) plus a REST route to it. **That is a real
decision and it is left open** — see [§8](#8-open-questions).

This is *not* immutability. Nothing prevents a curator commenting on, reopening, or closing one of these, and
nothing should be built to prevent it.

### 3.5 Attribution: say it in the content, because the model cannot

There is no "filed by a process" field on `Ticket` ([§2.6](#26-system-generated-tickets-already-exist--two-of-them)),
and the `reporter` will often be an agent or service account rather than the curator reading it. So the record
states it in words:

- body: `Filed by: the obsolete-term correction task, automatically; triggered by <submitter>.`
- payload: `"automated": true`, `"generatedBy": "ObsoleteTermCorrectionTask"`, `"triggeredBy": <submitter>`.

Adding a real boolean column to `TICKET` would be cleaner and is deliberately not proposed — it is a migration
([§7](#7-schema-impact)) for something the content can carry.

### 3.6 Write for a reader who does not remember what was asked for

The reader is a passive receiver *even when they triggered the run*. By the time they open this they are in a
browser, possibly days later, without the invocation in front of them.

So the body states its **parameters** before its **results**: mode, requested scope (every auto-correctable term,
or a named list), when it ran, who triggered it — then what was skipped and why. `"24 datasets updated"` without
"this was a rehearsal over every auto-correctable term, and two deferred terms were skipped" is the failure to
design against.

The requested URIs come off `ObsoleteTermCorrectionTaskCommand.getUris()`, not off the result.

One honesty requirement: because `skippedNotCorrectable` is only populated for explicitly named terms
([§2.9](#29-the-correction-run-and-what-it-already-knows)), a blanket run must say so, or the absent section reads
as a clean bill of health:

> Obsolete terms whose replacement could not be derived are out of scope for a run over everything and are NOT
> listed above. See `GET /admin/ontologies/obsolete-terms` for the ones still awaiting a curator's decision.

### 3.7 Highlights, not a dump

105 terms over 9,207 experiments is not a curator-facing report. The body:

- shows the **5 widest-reaching terms** (sorted by datasets affected — a curator scanning for "did this touch
  anything of mine" is best served by reach), and counts the rest;
- per term: `from → to` with labels, dataset and annotation counts, which **slots** were rewritten (a *category*
  rewrite reshapes the annotation rather than correcting one of its terms, and deserves a harder look than a
  *value* rewrite), the `resolvedVia` justification in plain words, and up to 5 example datasets;
- calls out **resync failures** separately rather than folding them into a count — that is the one genuinely
  actionable item a record can carry, and the useful framing is "the annotations are correct, the denormalized
  tables are stale, re-run the EE2C update for these";
- says when the target list was truncated, so a curator whose dataset sits past the cut does not read the targets
  as complete.

The exhaustive account — every term, every experiment id, the full skipped and failure lists — goes in the
`COMMENTED` event payload as JSON.

### 3.8 Bounds

| bound | value | why |
|---|---|---|
| targets per ticket | 200 | `TicketValueObject` embeds all targets, in list views too |
| terms shown in body | 5 | the rest are counted; all are in the payload |
| example datasets per term | 5 | enough to recognise one of yours |
| dataset labels resolved | ~40 | `shortName` needs a load each; unlabelled ids print as `dataset 123` |
| title | 255 chars | `TICKET.NAME` is `VARCHAR(255) NOT NULL` |

**No record is filed when no datasets were affected.** A run that changed nothing is not news, and `openTicket`
requires at least one target anyway.

### 3.9 Where the filing happens

In `ObsoleteTermCorrectionTaskImpl.call()`, after `apply(...)` returns — not at the REST layer, which returned 202
long ago and never sees the result. Filing must never fail the task: the work being reported has already happened,
and losing the account of it must not surface as the correction having gone wrong. Follow
`GeoScrapeServiceImpl`'s precedent — log and continue.

---

## 4. Rejected alternatives

**One ticket per affected dataset.** 9,207 tickets from one run floods every queue and destroys the "highlights"
framing. Rejected.

**One ticket per corrected term.** A curator cares about the run, not the term. Rejected.

**A parent record with per-dataset children.** `Ticket` has no parent FK, so this needs a migration for a shape
that buys little over targets. Rejected.

**Reusing `TicketType.GENERIC`** (what both existing system producers use). Would flip `needsAttention = true` on
every touched dataset via `CurationDetailsServiceImpl.NEEDS_ATTENTION_TYPES`
([§2.8](#28-ticket-type-has-semantics-beyond-labelling)). Rejected on that evidence.

**Naming the type for ontology maintenance** (`ONTOLOGY_MAINTENANCE_REPORT` or similar). Rejected after Paul
widened the category to "curation work performed" — an ad hoc curation record must sit in it naturally rather than
look like a foreign object.

**`priority = HIGH` for real runs.** Tempting for dashboard sorting, but HIGH conventionally means "urgent action
needed", and a *completed* run is informational. Mislabelling it would train curators to discount HIGH. Rejected;
both stay NORMAL.

**A generic maintenance-run reporting layer** (`MaintenanceRunReport` + `MaintenanceRunReportService` +
per-producer builders). This was built, and it was too much — a framework with one caller. Collapsed to: one
carrier type, one filing service, one producer. If a second producer appears, generalize then, with two real cases
in hand.

**Wiring a Claude-session reference onto the ticket.** `CurationRunRef` exists only as a REST inner DTO
(`DatasetsWebService.java:2851`, used at 2717/2811/2835-2836) — there is **no persisted run-reference on any
entity**. Putting one on `Ticket` means a new nullable column, i.e. a migration, for a field nothing populates
yet. Paul called this "at best" a nice-to-have. Left out; noted as a follow-up.

**UI controls to launch or retry the job.** Explicitly a future ask. Not built, not designed for, no hooks left.

---

## 5. Changes to existing code

Small, and all additive.

1. **`ObsoleteTermCorrectionResult`** — add `List<Long> experimentIds` at the top level and on `TermCorrection`.
   The run already computes both (`ObsoleteTermCorrectionServiceImpl.java:70,114`) and throws them away. This is
   *retaining*, not re-running: the ids cannot be recovered afterwards
   ([§2.9](#29-the-correction-run-and-what-it-already-knows)). Two one-line assignments in the service impl.
2. **`TicketType`** — add `CURATION_RECORD`. No migration.
3. **`TicketService.openTicket`** — add a `body` overload. Optional but worth it: the two existing workarounds
   ([§2.5](#25-how-tickets-are-created)) each leave a window in which a record can be published without its
   content, and for a record the body *is* the point. The 4-arg form delegates with `body = null`.
4. **`AdminWebService`** — update the `apply` endpoint's OpenAPI description to say where the outcome can be read,
   including that `/tasks/{taskId}` will not show it.

New: a small carrier (activity, dryRun, headline, body, affectedExperimentIds, detailJson), a filing service, and
a **pure static builder** that renders `ObsoleteTermCorrectionResult` → body/headline/payload. Keeping the
rendering pure is what makes the selection logic — the part actually worth testing — testable without a database,
a Spring context or an ontology.

---

## 6. What a curator actually sees

Dashboard list row (`GET /tickets`, newest first):

```
[DRY RUN] Obsolete ontology term correction: 7 terms, 412 annotations, 24 datasets
CURATION_RECORD · RESOLVED · agent-svc · 2026-08-19
```

Ticket body:

```
DRY RUN — nothing was written. This is a rehearsal of what a live run would change.

What was asked for
  Mode: dry run — the rehearsal writes nothing and reports the counts a live run would produce.
  Scope: every auto-correctable obsolete term, except the deferred ones.
  Ran: 2026-08-19T22:14:07Z
  Filed by: the obsolete-term correction task, automatically; triggered by pavlidis.

Would rewrite 412 annotations across 24 datasets, over 7 obsolete terms.

Changes, widest reach first:

  EFO_0000546 "injury"  ->  MONDO_0021178 "injury"
    24 datasets, 31 annotations (value ×31).
    Safe to derive: the ontology itself asserts this replacement (IAO:0100001 "term replaced by").
    Following it reads the answer the ontology published; it is not a guess.
    e.g. GSE12345, GSE20881, GSE33409, GSE41102, GSE55220, and 19 more

  OBI_0002119 "..."  ->  OBI_0002631 "..."
    6 datasets, 9 annotations (category ×2, value ×7).
    Safe to derive: the obsolete term was merged into the replacement, which records it as an
    alternative ID (hasAlternativeId). The two are the same term under two identifiers.
    e.g. GSE9901, GSE10022, ...

  …and 2 further terms. Every one of them is in this ticket's detail event payload.

Deliberately skipped as deferred — a blanket run leaves these alone; naming one in `uris`
overrides that: EFO_0000408, OBI_0003109.

Obsolete terms whose replacement could not be derived are out of scope for a run over everything
and are NOT listed above. See `GET /admin/ontologies/obsolete-terms` for the ones still awaiting
a curator's decision.
```

A live run additionally ends with:

```
Denormalizations rebuilt for 24 datasets (24 EE2C rows, 18 relation rows).

ACTION NEEDED — 2 datasets failed to resync. The annotations themselves are correct; it is the
denormalized tables that are stale, so search and the annotation counts will disagree with the
dataset page until the EE2C update is re-run for them:
  4471: HibernateOptimisticLockingFailureException: ...
  9902: ...
```

Attached as a `COMMENTED` event: the full JSON — `activity`, `generatedBy`, `automated`, `dryRun`, `triggeredBy`,
`ranAt`, `requestedUris`, `scopeWasEverything`, and the entire `ObsoleteTermCorrectionResult` including every term
and every experiment id.

⚠️ Target rendering is **unverified**: `displayLabel` is never populated server-side
([§2.7](#27-what-the-dashboard-reads)), so unless the UI does its own lookup, the target list shows bare numeric
ids. This is why the body names example datasets itself rather than relying on the targets to be legible.

---

## 7. Schema impact

**None. No Flyway migration is required, and none should be written.**

`TicketType` and `TicketTargetType` are `EnumType.STRING` into `VARCHAR(64)` / `VARCHAR(32)` with no CHECK
constraint (`V3__ticket_layer.sql`), and `TicketType`'s javadoc states outright that new values need no migration.
`DESCRIPTION` is `TEXT` and `TICKET_EVENT.PAYLOAD` is `JSON`, both large enough.

Two things *would* need a migration, and both are deliberately excluded:

- a boolean "filed by a process" column on `TICKET` — the content carries it instead ([§3.5](#35-attribution-say-it-in-the-content-because-the-model-cannot));
- a Claude-session / run reference on `TICKET` — deferred ([§4](#4-rejected-alternatives)).

**If a future revision wants either, that requires Paul's explicit approval before the migration is written.**

---

## 8. Open questions

1. **"Tracks what was done" versus "tracking overall curation tasks" may be two different things.** Paul used both
   phrasings for the same type. A completed-work record needs no lifecycle and is filed terminal. An "overall
   curation task" — a broader piece of work spanning datasets — sounds like it *has* a lifecycle: opened, worked,
   finished. If it does, one type cannot serve both, because the state at filing is opposite in the two cases.
   **This specification builds the first (a record) and does not resolve the second.** The options are: a second
   type for in-flight curation work; or one type whose initial state the producer chooses. Needs Paul.
2. **Is the dataset page more important than open-queue hygiene?** RESOLVED buys clean queues and costs the
   dataset → record reverse lookup ([§3.4](#34-state-resolved-and-what-that-costs)). Not resolvable from the code.
3. **The "check" case is specified but not designed.** Paul's own example was *"open a ticket for this obsolete
   term check"* — a record of `GET /admin/ontologies/obsolete-terms`, where nothing has changed. It does **not**
   fall out of this design for free: `ObsoleteTermUsage` has `experimentCount` (line 68) but **no experiment
   ids**, so a check-derived record has no targets, and `openTicket` requires at least one
   (`TicketServiceImpl.java:88`). Getting ids means a per-term
   `characteristicReadService.findExperimentIdsByUriInAnySlot` sweep — a second pass over the corpus and a
   genuinely different code path. Also, `dryRun` is the wrong vocabulary for a read-only check. **What the check
   case needs:** either (a) relax the non-empty-targets invariant for target-less records, (b) a
   `TicketTargetType` for something a check can point at, or (c) accept the extra id-resolution pass. Worth
   noting an agent can *already* file such a ticket today via `POST /tickets` with `type=CURATION_RECORD`, a body,
   and its own targets — no new endpoint needed, provided it supplies at least one target.
4. **Does the curation UI filter by ticket type, and will it render an unknown one?** Not inspected — separate
   repository. If it has a hardcoded type list or a TypeScript enum mirror, `CURATION_RECORD` needs adding there
   too.
5. **Is `GeoScrapeServiceImpl`'s non-JSON `PAYLOAD` write actually failing on MySQL?**
   ([§2.4](#24-events-and-the-payload-column).) Unverified. If it is, it is a live bug worth its own fix.
6. **Will the correction task survive long enough to file anything?** The 60-second default runtime cap
   ([§2.11](#211-an-unrelated-risk-found-on-the-way)) is not overridden by `ObsoleteTermCorrectionTaskCommand`.
   Unverified against a running instance; should be checked before the first real run, independent of this work.
7. **Who is "their datasets"?** Nothing in the ticket model or ACL was found that maps a dataset to a responsible
   curator, so the record cannot be routed to individuals. It is corpus-wide and unassigned. Whether per-curator
   routing is wanted is unresolved.

---

## 9. What was built, and thrown away

An implementation of the above reached compile-clean with 35 passing unit tests before Paul parked the work
(*"just write a spec; let's leave it aside for now"*). It was **discarded uncommitted** — the branch contains only
this document. Recorded here so the next person knows what was and was not established by running code:

- `ObsoleteTermCorrectionResult` gained `experimentIds` (top level and per term); the service retained them.
- `TicketType.CURATION_RECORD`; `TicketService.openTicket` body overload.
- A `CurationRecord` carrier, a `CurationRecordService`/`Impl` doing the ticket mechanics, and a pure static
  `ObsoleteTermCorrectionReportBuilder` doing the rendering.
- The task filed the record after `apply(...)` returned, passing `cmd.getUris()` and `cmd.getSubmitter()`.

Four guard tests were **confirmed failing before their fix and passing after** — worth re-creating, because each
guards a failure that is silent rather than loud:

| guard | failure it catches |
|---|---|
| targets capped at 200 | unbounded targets bloat every ticket listing |
| title trimmed to 255 | over-long title fails the `TICKET.NAME` insert; the whole record is lost |
| live-run targets are `DONE` | `NOT_DONE` is the default, so without a deliberate set a real run is indistinguishable from a rehearsal |
| body admits a truncated target list | a curator past the cut reads the targets as complete and concludes their dataset was untouched |

An intermediate shape — a generic `MaintenanceRunReport` + `MaintenanceRunReportService` layer intended for future
maintenance tasks — was built and then collapsed as over-abstraction ([§4](#4-rejected-alternatives)).

Never verified, in either the built version or this spec: anything about the curation UI, and anything requiring a
running MySQL or a real ontology load.
