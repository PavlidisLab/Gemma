# RECCE — `AgentProposal` -> `AgentCuration` unification

**Filed:** 2026-05-22.
**From:** Gemma claude (orchestrator-spawned recce sub-agent).
**Worktree:** `.claude/worktrees/agent-recce-agent-curation` (branch `recce-agent-curation`).
**Status:** Reconnaissance / design doc. No code changes. Specifies the Gemma-side migration path to align with the unified-justification schema designed by the curation-agents team.

**Counterparts:**

* `handoffs/SCHEMA_UNIFIED_JUSTIFICATION.md` — the agent-side payload contract this storage layer must hold.
* `handoffs/HANDOFF_UNIFIED_JUSTIFICATION_SCHEMA.md` — agent-side design proposal + rationale.
* `handoffs/STATUS_UNIFIED_JUSTIFICATION_SCHEMA.md` — refinements after UI-bro review.
* `handoffs/HANDOFF_EVAL_PKG_AS_DATASET_PROPOSALS.md` — eval-pkg load that exposed the proposal-vs-audit shape gap.
* `handoffs/STATUS_UNIFIED_CURATION_DRAFT.md` — the unified `CurationDraft` (already landed in this branch — see §4).

---

## 0. The decisions already made

Paul has pre-decided four things; this recce builds on them rather than relitigating:

1. **One table, with a `kind` discriminator.** `'proposal' | 'audit'` on the existing `AGENT_PROPOSAL`. Entity renamed to `AgentCuration` to match Paul's mental model ("AGENT_CURATION — more general than PROPOSAL or AUDIT").
2. **Big JSON payloads stay opaque to Java.** Per-row 50–100 KB target with 2× headroom. The existing `MaterializedClobType` + MySQL `JSON` / H2 `CLOB` mapping handles this already.
3. **Thin metadata projection for list pages.** New summary value object; new `?shape=meta|full` query param on the GET endpoints.
4. **Dispositions on `CurationDraft`.** Per-finding dispositions live on the draft, NOT a new table.

The recce sections below give concrete, minimal changes for each decision plus phasing.

---

## 1. Entity rename + `kind` discriminator

### 1.1 Should `AgentProposal` -> `AgentCuration`?

**Recommendation: yes, but defer to the final phase** (§7 step 5). Reasoning:

* **Pro** — Paul's mental model is the canonical one; "AgentProposal" reads as "only proposals" and obscures the audit-side use. A reader six months from now is going to find one table, one entity, one DAO; calling it `AgentCuration` makes the audit/proposal symmetry self-evident.
* **Pro** — every doc landing from here on (`SCHEMA_UNIFIED_JUSTIFICATION`, audit-side STATUS, this recce) is using "curation" as the umbrella term. Java code drifting from the doc vocabulary is friction.
* **Con** — 25 files in `gemma-core/src` + `gemma-rest/src` reference the bare token `AgentProposal` today (exact `grep -rl 'AgentProposal\b'` count, excluding `target/` + parallel worktrees). Rename is mechanical (IDE refactor) but it touches: the entity, the HBM, the DAO + DAO impl + service + service impl, the audit event type, the audit-type discriminator in `AuditEventType.hbm.xml`, four REST classes (`PreboardedWebService`, `CurationWebService`, `DraftsWebService`, plus DTOs), two test classes (`AgentProposalServiceTest`, `PreboardedWebServiceTest`), the `CurationDraft` entity (FK named `proposal`), the `CURATION_DRAFT` table's FK constraint name (`FK_CURATION_DRAFT_PROPOSAL`), the V12 mysql + V14 h2 migrations (if we touch them), and `hibernate.cfg.xml`.
* **Con** — `AgentProposalEvent` rename ripples to `AuditEventType.hbm.xml`'s `discriminator-value="AgentProposalEvent"`. Production audit rows already carry that discriminator. **Changing the discriminator string is a data-migration**, not a code rename — every prior row keeps the old string and the new code can't load them. So if the audit event is renamed, the HBM `discriminator-value` MUST stay `"AgentProposalEvent"` while the Java class becomes `AgentCurationEvent` (Hibernate accepts the mismatch — `discriminator-value` is the wire string, the `name=` attribute is the FQN).

### 1.2 Should the **table** be renamed too?

**Recommendation: NO.** Keep `AGENT_PROPOSAL` as the SQL identifier. Reasoning:

* Flyway history stays clean — no rename migration, no risk of double-applying.
* The mapping layer lets the Java class be named whatever we want; HBM `class name="...AgentCuration" table="AGENT_PROPOSAL"` works fine.
* Future devs reading the schema dump will see `AGENT_PROPOSAL` and grep for "AgentProposal" — they'll find the entity (which is `AgentCuration` but lives at `AgentCuration.java`) via the HBM mapping. One indirection.
* Cost of renaming the table: a Flyway V14 (mysql) + V16 (h2) `RENAME TABLE` migration + an extra `ALTER TABLE ... RENAME CONSTRAINT FK_CURATION_DRAFT_PROPOSAL ...` + an HBM `table=` attribute update. Net zero benefit; the only people who see the table name are devs reading raw SQL.

So the final shape: Java class `AgentCuration`, table `AGENT_PROPOSAL`, audit-type discriminator string `"AgentProposalEvent"` (frozen for backwards-compat with already-persisted audit rows), Java class for the audit type `AgentCurationEvent`.

### 1.3 Files affected by the rename (count + list)

`grep -rl 'AgentProposal\b' gemma-core/src gemma-rest/src` = 25 files. The split is:

| Bucket | Files |
|---|---|
| Entity + mapping | `AgentProposal.java`, `AgentProposal.hbm.xml`, `hibernate.cfg.xml` (one line) |
| Persistence | `AgentProposalDao.java`, `AgentProposalDaoImpl.java`, `AgentProposalService.java`, `AgentProposalServiceImpl.java` |
| Audit-event type | `AgentProposalEvent.java`, `AuditEventType.hbm.xml` (subclass `name=` only; discriminator string frozen) |
| Co-entity refs | `CurationDraft.java` (`AgentProposal proposal` field + `setProposal` / `getProposal`), `CurationDraft.hbm.xml`, `CurationDraftDao(Impl)`, `CurationDraftService(Impl)` |
| Promotion + lifecycle | `PreboardedExperimentService.java`, `PreboardedExperimentServiceImpl.java` (proposal rebind on promote) |
| REST | `PreboardedWebService.java` (`AttachProposalRequest`, `ProposalResponse`, etc.), `CurationWebService.java` (`CurationProposalRequest`/`Response`), `DraftsWebService.java` (`AgentProposal` field, `ReviewResponse`) |
| Tests | `AgentProposalServiceTest.java`, `PreboardedWebServiceTest.java`, `DraftsWebServiceTest.java`, `CurationDraftServiceTest.java` |

The rename is mechanical via IDE "Rename type" — 25 files but mostly automatic. **Defer to last phase** (§7 step 5) so the higher-priority work (kind column, thin projection, dispositions) ships first against a working baseline.

### 1.4 The `kind` column

* SQL: `KIND VARCHAR(16) NOT NULL DEFAULT 'proposal'`. Lowercase string values; not an SQL `ENUM` (MySQL `ENUM` is fragile under schema evolution — adding `audit` would need a table-rewrite if it were an `ENUM`, but `VARCHAR(16)` shrugs).
* Java enum: `AgentCurationKind { PROPOSAL, AUDIT }` mapped via Hibernate's `EnumType` in `STRING` mode (lowercase via `@Type` or via a `UserType` that does the case conversion — simpler: name the enum constants `proposal` / `audit` to match the DB strings, OR use the existing pattern in the codebase (TBD — quick `grep` for `EnumType.STRING` will surface it)).
* HBM: `<property name="kind" type="...EnumType"><column name="KIND" sql-type="VARCHAR(16)" not-null="true"/></property>` — exact `type=` string depends on which enum-mapping helper the rest of the codebase uses; this is a 10-minute lookup, not a design call.

### 1.5 Idempotency key — `(INVESTIGATION_FK, RUN_ID)` vs `(INVESTIGATION_FK, KIND, RUN_ID)`?

**Recommendation: `(INVESTIGATION_FK, KIND, RUN_ID)`**. Reasoning:

* An audit run and a proposal run on the same EE could share a `run_id` shape if the agent-side run identifier (e.g. timestamp + run_label) happens to collide. That's not impossible: the audit and proposal pipelines could share scheduling infrastructure.
* The cost of widening the key is zero — it's a unique-index column-list change, no behavioural impact on the existing rows (all current rows are `kind='proposal'` so the key remains unique).
* Without `KIND` in the key, an idempotent re-attach on `(EE, 'audit', run-X)` would silently match an existing `(EE, 'proposal', run-X)` and return the wrong row — a silent correctness bug. With `KIND` in the key, the audit row gets created cleanly.

The agent-side STATUS doc (`HANDOFF_EVAL_PKG_AS_DATASET_PROPOSALS.md`) shows the wire body already carries `run_id` per investigation; nothing on the wire constrains uniqueness across kinds. **The unique key has to include `KIND` to be correct.**

---

## 2. Flyway migration

### 2.1 The minimal additive migration

```
-- gemma-core/src/main/resources/db/migration/mysql/V13__agent_curation_kind.sql
-- (h2 sibling: V15__agent_curation_kind.sql with identical statements)

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN KIND VARCHAR(16) NOT NULL DEFAULT 'proposal' AFTER RUN_ID;

-- Drop the old key + recreate widened. Two-step rather than one ALTER
-- because MySQL's "drop + add unique key in same ALTER" syntax is
-- engine-flavour-sensitive and Flyway prefers explicit statements.
ALTER TABLE AGENT_PROPOSAL
    DROP INDEX UK_AGENT_PROPOSAL_INVESTIGATION_RUN;

ALTER TABLE AGENT_PROPOSAL
    ADD CONSTRAINT UK_AGENT_PROPOSAL_INVESTIGATION_KIND_RUN
        UNIQUE KEY (INVESTIGATION_FK, KIND, RUN_ID);

-- List-page index for the typical "latest curation rows for this EE,
-- newest first" query the new ?shape=meta path will issue.
CREATE INDEX IDX_AGENT_PROPOSAL_INVESTIGATION_KIND_RAN_AT
    ON AGENT_PROPOSAL (INVESTIGATION_FK, KIND, RAN_AT DESC);
```

Notes:

* Existing rows backfill to `'proposal'` via the `DEFAULT` clause — no data migration step needed.
* The HBM `<property name="kind" .../>` adds correspondingly; the `unique-key=` attribute on the existing `<column name="INVESTIGATION_FK">` and `<column name="RUN_ID">` gets renamed to `UK_AGENT_PROPOSAL_INVESTIGATION_KIND_RUN` (Hibernate-side declarative-only — the actual constraint creation is Flyway's job).
* H2's syntax handles `ADD COLUMN ... AFTER` differently; the h2 sibling drops the `AFTER RUN_ID` clause (H2 just appends; column order in H2 is cosmetic).

### 2.2 Numbering

* Mysql top of the stack is `V12__curation_draft.sql` -> next is `V13`.
* H2 top is `V14__curation_draft.sql` -> next is `V15`. The mysql / h2 numbers have drifted apart (mysql lacks H2's `V1__hibernate_baseline.sql` + `V2__schema_extras.sql` + `V3__seed_data.sql` head-rows); both stacks just need the next free number.

---

## 3. Thin list-page projection

### 3.1 The thin DTO

```java
// gemma-rest/src/main/java/ubic/gemma/rest/CurationWebService.java
// (or co-located new file if it gets shared with PreboardedWebService /
// DraftsWebService — recommendation: keep it co-located with the
// existing CurationProposalResponse for symmetry)

public static class AgentCurationSummaryResponse {
    @JsonProperty("proposal_id")  // Wire-compat alias — same field name
    public Long id;               // the UI's existing useProposalsForExperiment
                                  // hook reads, but typed Long not the
                                  // String UUID legacy used to use.
    @JsonProperty("dataset_id")
    public Long investigationId;
    @JsonProperty("kind")
    public String kind;           // "proposal" | "audit"
    @JsonProperty("run_id")
    public String runId;
    @JsonProperty("agent_version")
    @Nullable
    public String agentVersion;
    @JsonProperty("model")
    @Nullable
    public String model;
    @JsonProperty("ran_at")
    @Nullable
    public Date ranAt;
    @JsonProperty("payload_size")
    @Nullable
    public Long payloadSize;      // CHAR_LENGTH(PAYLOAD_JSON) — convenience for
                                  // the UI's "fetch heavy row vs skip"
                                  // decision. Optional; omit if too costly.
}
```

### 3.2 Endpoint shape

* **Query param**: `?shape=meta|full`. Defaults to **`meta`** — UI almost always wants the list first; the full payload is a fetch-on-click. Existing legacy callers that omit `?shape` get the cheap version; if any caller breaks, that's the trigger to update.
* **Status quo wire-shape preservation**: the full response under `?shape=full` is byte-identical to today's `CurationProposalResponse` plus the new `kind` field. So the existing `useProposalsForExperiment` hook keeps reading the response under `?shape=full` with one new optional field, no breakage.
* **Counter-recommendation if Paul wants backwards-compat to be even tighter**: default to `?shape=full` (no behaviour change) and require `?shape=meta` opt-in. Pick one; recommend `meta` because the UI side is being rebuilt for the unified schema anyway.

### 3.3 DAO method shape

```java
// gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/AgentCurationDao.java

/** Newest-first summaries for an investigation. payloadJson NOT loaded. */
List<AgentCurationSummary> listSummariesForInvestigation(
    Investigation investigation,
    @Nullable AgentCurationKind kindFilter,   // null => all kinds
    int offset, int limit );

/** A typed projection record. Could be a small final class or a
    Hibernate ResultTransformer-fed POJO. */
class AgentCurationSummary {
    Long id;
    Long investigationId;
    AgentCurationKind kind;
    String runId;
    String agentVersion;
    String model;
    Date ranAt;
    Long payloadSize;   // SELECT CHAR_LENGTH(PAYLOAD_JSON) — see note
}
```

HQL skeleton:

```
SELECT NEW ubic.gemma....AgentCurationSummary(
    p.id, p.investigation.id, p.kind, p.runId,
    p.agentVersion, p.model, p.ranAt,
    LENGTH(p.payloadJson)  -- only if we keep payload_size; LENGTH on a CLOB
                           -- is full-row on H2, fine on mysql JSON
)
FROM AgentCuration p
WHERE p.investigation = :inv
  AND (:kind IS NULL OR p.kind = :kind)
ORDER BY p.ranAt DESC, p.id DESC
```

`LENGTH(...)` on a CLOB column on H2 traverses the column, so if H2-side test perf matters drop `payloadSize` from the projection (the UI can infer "row is fetchable" without knowing the size). Recommend **keeping `payloadSize` for mysql, optionally NULL on H2** via a simple conditional in the DAO — or just always NULL it and let the UI not show it.

### 3.4 Slice / pagination

Use `Slice<AgentCurationSummary>` (the existing project pattern in datasets endpoints) if pagination is needed; the eval-pkg load currently dumps 50 rows in one response, no slicing. Recommend NOT adding pagination on the first cut — proposals per EE are 1–3 rows in practice; KISS. Add slicing only when a curator-facing "all curations for this study" page emerges that justifies it.

---

## 4. `CurationDraft` extension for dispositions

**Surprise finding**: dispositions are ALREADY supported on `CurationDraft`, just not the way Paul's task brief describes.

The current design (landed in V12 + `CurationDraftDispositions.java`):

* Snapshot the seed-time proposal payload into `PROPOSAL_SNAPSHOT_JSON`.
* Curator edits land in `PAYLOAD_JSON`.
* Dispositions are **DERIVED** at read-time by diffing `payloadJson` vs `proposalSnapshotJson` (shallow per-top-level-key diff): only-in-snapshot -> `REJECTED`, in-both-equal -> `RETAINED`, in-both-different -> `EDITED`. Only `PARKED` needs explicit storage (a JSON-array of opaque element keys in `PARKED_ELEMENTS`).
* `CurationDraftDispositions.derive(draft)` returns `Map<String, Disposition>` keyed by element key.

The wire shape `ReviewResponse.dispositions` in `DraftsWebService` already exposes this to the UI today.

**So Paul's "extend CurationDraft with a JSON disposition map" instruction is partially already done — and the existing design is cleaner.** Two paths forward:

### 4.1 Path A — keep the derive-from-diff design (RECOMMENDED)

* Already works. No new column, no new migration.
* Snapshot is captured automatically when `seedFromProposal(...)` runs.
* Only gap: the disposition vocabulary in the audit-/proposal-side handoffs is `accept / reject / edit / park`, while the existing Java enum is `RETAINED / EDITED / REJECTED / PARKED`. These are **semantically the same** with one subtlety: agent-side "accept" maps to Java-side "RETAINED" only when the curator made no edits. If the curator accepted-with-edits, the Java side returns `EDITED` and the agent-facing wire shape should still call it "accept" with an "edits" sub-field. Two options:
  * **a.** Translate at the REST boundary — `DraftsWebService` already controls the wire shape; map `RETAINED -> "accepted"`, `EDITED -> "accepted_with_edits"`, `REJECTED -> "rejected"`, `PARKED -> "parked"`. Add the optional `edited_payload` (the post-edit JSON value) only when EDITED, sourced from the same diff.
  * **b.** Rename the Java enum to match `Accept / Reject / Edit / Park` — but then `Edit` is ambiguous (edit + accept vs edit + reject?), and the per-element semantics are clearer with the existing four values.
* **Recommendation**: keep the Java enum as-is, do the rename at the wire boundary (option a). Existing infrastructure stays; the agent-side gets the vocabulary it expects.

### 4.2 Path B — explicit disposition map (what Paul's brief describes)

* Add a `disposition_json: TEXT NULL` column to `CURATION_DRAFT`.
* Curator PUTs explicit disposition entries: `{"factor:42:0": {"decision": "accept"}, "tag:42:3": {"decision": "edit", "edited_payload": {...}}}`.
* Drop or reduce reliance on the diff-derive logic.

Cost: extra column, the diff logic becomes redundant (or runs as a fallback when explicit dispositions are absent), the PATCH-vs-PUT merge semantics question opens up.

**Recommendation against Path B for now.** The current design encodes the same information more concisely (the curator's edits ARE the payload; explicit `decision: edit` is redundant). Path B is the migration target only if downstream wants per-disposition rationale strings (curator notes on individual decisions, "I rejected this because...") that don't fit in the payload edit. That's a real feature need but it can come later as a separate column (`disposition_notes: JSON NULL` keyed by element).

### 4.3 PATCH-vs-PUT for disposition updates

Already addressed by the existing `PUT /datasets/{id}/draft` endpoint — it's a whole-payload replacement. The merge semantics question only arises with Path B, so Path A sidesteps it. The agent-side PATCH-style `PATCH /proposals/{proposalId}/reviews/{reviewer}` does exist in `DraftsWebService:327` but it's a whole-payload PATCH (full replacement of the wire body); not a per-element merge.

---

## 5. `AgentProposalEvent` + audit-trail FK

### 5.1 Current behaviour

* `AgentProposalServiceImpl.attach(...)` carries `@AuditedConditional(value = AgentProposalEvent.class, when = "#result.created", messageSpel = "...")`.
* The audit-trail row attaches to the `Investigation` argument (which is the `Auditable` parameter, per Phase C audit-aspect contract).
* The audit NOTE includes the proposal id, run id, agent version, model.
* The HBM discriminator string is `"AgentProposalEvent"` (in `AuditEventType.hbm.xml:168`).

### 5.2 Adding kind=audit support

Two design choices:

**Option 1 — one event type, NOTE varies by kind.**

* Pro: no new event-type subclass, no `AuditEventType.hbm.xml` change, the NOTE-grep query that finds "proposal events" still finds audit ones too.
* Con: filtering audits-vs-proposals at the audit-trail-listing level requires parsing the NOTE (ugly).
* The messageSpel becomes `... + ' kind=' + #result.proposal.kind ...`.

**Option 2 — two event types: `AgentProposalEvent` + `AgentAuditEvent`.**

* Pro: clean filter at the audit-listing level (`WHERE event_type = ...`).
* Con: an HBM `<subclass discriminator-value="AgentAuditEvent" name=".AgentAuditEvent"/>` line + a new Java class.
* Conditional dispatch in the aspect: `@AuditedConditional` doesn't natively support "switch event class on result" — the simplest path is to split `attach(...)` into two service methods (`attachProposal`, `attachAudit`), each with its own `@AuditedConditional` annotation referencing the right event class. Adds a coordination knob.

**Recommendation: Option 1**. The audit-trail listings are infrequently filtered by event type; the NOTE already carries the kind once it includes `kind=`. Saves one Java class + one HBM line. If filterability becomes painful later, splitting is cheap.

When the entity rename lands (phase 5), `AgentProposalEvent` -> `AgentCurationEvent` in Java, **discriminator string stays `"AgentProposalEvent"`** for forward-compat with existing audit rows.

---

## 6. REST endpoint deltas — exact request/response shapes

### 6.1 `POST /datasets/{id}/curation-proposals` (`CurationWebService.submitCurationProposal`)

**Body — additive field:**

```json
{
  "run_id": "...",
  "agent_version": "...",
  "model": "...",
  "ran_at": "...",
  "payload_json": "...",
  "kind": "proposal"     // NEW — optional, defaults to "proposal"
}
```

* `kind` accepts `"proposal"` or `"audit"`; absent / null -> `"proposal"` (backwards-compat).
* 201 on insert, 200 on idempotent re-post (`(investigation, kind, run_id)` collision).
* Unknown `kind` -> 400.

**Response** (unchanged shape, plus `kind` field):

```json
{
  "proposal_id": 8,
  "dataset_id": 91222,
  "kind": "proposal",     // NEW
  "run_id": "...",
  "agent_version": "...",
  "model": "...",
  "ran_at": "...",
  "payload_json": "..."
}
```

### 6.2 `GET /datasets/{id}/curation-proposals` (`CurationWebService.listCurationProposals`)

**Query params — both new:**

* `?kind=proposal|audit|all` — defaults to `all`.
* `?shape=meta|full` — defaults to **`meta`** (see §3.2 caveat).

**Response under `?shape=meta`**: list of `AgentCurationSummaryResponse` (see §3.1). `payload_json` omitted.

**Response under `?shape=full`**: list of the current `CurationProposalResponse` shape (now with `kind` field included), payload_json populated.

### 6.3 `POST /preboarded/{id}/proposals` (`PreboardedWebService.attachProposal`)

Same deltas as §6.1. The two endpoints share the same `attach(...)` service call, so the body shape and behaviour stay symmetric.

### 6.4 `GET /preboarded/{id}` (`PreboardedWebService.getPreboarded`)

* The current shape returns the "latest proposal" as a `ProposalResponse` inline (PreboardedWebService.java:150 fetches `findLatestByInvestigation`). Recommend filtering this to `kind = 'proposal'` (audits on a preboarded shouldn't masquerade as "the proposal" in the preboarded card). Add `latestProposal` + optional `latestAudit` if Paul wants both — but for a preboarded that hasn't been promoted yet, audits don't really make sense. Probably just filter to `kind=proposal` here.

### 6.5 Draft endpoints (`DraftsWebService`)

* No deltas required for the disposition path (already works via the diff-derive on `CurationDraftDispositions.derive`).
* New: nothing for v1. If Path B (§4.2) lands later, add `dispositions_json` to the `CreateOrUpdateDraftRequest` body — but that's deferred.

### 6.6 New endpoint — is one needed?

No. The agent-side handoffs (`HANDOFF_EVAL_PKG_AS_DATASET_PROPOSALS.md:78`) explicitly recommend the `?shape=auto` (or `?shape=meta`) pattern over a separate endpoint, and the existing endpoints serve both audit-side and proposal-side reads once `kind` is in scope.

---

## 7. Migration phasing

Order so each step compiles + tests clean, lands as a single commit (or commit cluster), and is independently revertable.

### Step 1 — Flyway + entity field (no behaviour change)

* Mysql V13, h2 V15 from §2.1.
* Add `AgentCurationKind` enum (in the existing `expression.experiment` package alongside `AgentProposal`).
* Add the `kind` property to `AgentProposal.java` + HBM (default `AgentCurationKind.PROPOSAL` in the constructor + the column DEFAULT in SQL).
* DAO update: `findByInvestigationAndRunId` -> `findByInvestigationAndKindAndRunId` (the unique key changed). All existing callers pass `kind=PROPOSAL` to preserve current behaviour.
* Service update: `attach(...)` gains a `@Nullable AgentCurationKind kind` parameter (defaults to `PROPOSAL` if null). REST callers don't pass it yet.
* Tests pass with `kind=PROPOSAL` default; one new test confirms `(EE, AUDIT, run-X)` is independent of `(EE, PROPOSAL, run-X)`.
* No REST changes yet — all current rows are `kind='proposal'`, all wire bodies stay backwards-compat.

### Step 2 — Thin projection + summary DTO

* `AgentCurationSummary` projection record + the DAO method `listSummariesForInvestigation`.
* No REST endpoint change yet — this is just the persistence-layer surface ready to be wired up.

### Step 3 — REST `?kind=` + `?shape=` support

* `CurationWebService.listCurationProposals` + `CurationWebService.submitCurationProposal` gain the two query params + the new body field.
* `PreboardedWebService.attachProposal` mirror change.
* `AgentCurationSummaryResponse` DTO co-located with the existing `CurationProposalResponse`.
* Wire test + integration test for `?kind=audit&shape=meta`.

### Step 4 — Disposition wire-shape rename at the REST boundary (Path A from §4.1)

* `DraftsWebService.toReviewResponse(...)` maps the Java enum to the agent-side vocab (`RETAINED -> "accepted"`, etc.).
* Optional `edited_payload` sub-field surfaces on `EDITED` entries (sourced from the same diff).
* No Java code outside `DraftsWebService` changes; no migration.

### Step 5 — Entity rename `AgentProposal` -> `AgentCuration` (deferred)

* The mechanical IDE refactor (25 files).
* HBM `table=` stays `AGENT_PROPOSAL`.
* `AgentProposalEvent` -> `AgentCurationEvent` (Java); the HBM `discriminator-value=` stays `"AgentProposalEvent"` to preserve existing audit rows.
* `FK_CURATION_DRAFT_PROPOSAL` constraint name on the V12 migration stays — renaming a FK constraint requires a Flyway migration that gives zero behavioural value; the name is dev-tool fodder.

Steps 1–4 are independent and can be co-developed with the agent / UI sides immediately. Step 5 is cosmetic but should ship before Gemma 2.0 release so the API + entity reads consistently.

---

## 8. Open questions for Paul

1. **Default of `?shape=`** — recce recommends `meta` (UI usually wants the list first). Confirm or override to `full` for stricter backwards-compat.
2. **`payload_size` in the thin DTO** — recce recommends keeping it (mysql `JSON` column makes `LENGTH(...)` cheap; H2 will return null). Confirm; or drop it entirely if you don't want to be that helpful to the UI.
3. **Disposition vocabulary** — recce recommends the existing 4-value enum (`RETAINED / EDITED / REJECTED / PARKED`) translated at the REST boundary to agent-side `accepted / accepted_with_edits / rejected / parked`. Confirm or push for the explicit-map design (Path B in §4.2).
4. **Audit-event type split** — recce recommends one event type (`AgentProposalEvent`, kind in NOTE) over two (split into `AgentAuditEvent`). Confirm.
5. **Rename phase ordering** — recce defers the `AgentProposal -> AgentCuration` rename to step 5 (after the kind / projection / dispositions land). Confirm; or pull it forward if you want the type names + docs aligned from day one (would mean step 1 PR is 25-file IDE refactor + a kind column + a new enum + the unique-key change, all at once).
6. **Idempotency-key migration safety** — the V13 migration drops the existing unique key and re-creates it with `KIND` included. Existing `(EE, run_id)` rows remain unique under the new key (they all become `(EE, 'proposal', run_id)`). Confirm Paul is OK with the brief unique-key-absent window mid-migration; Flyway runs ALTERs serially so there's no race in practice, but if there's belt-and-braces concern, the migration could be: add the new key first as an extra constraint, drop the old one second. (Both work; recce defaults to the cleaner "drop then add" since rows are guaranteed unique by construction.)

---

## Surprises encountered while reading the source

1. **`CurationDraft` already does dispositions, via diff-derive.** Paul's task brief describes adding a `disposition_json` map, but the existing `CurationDraftDispositions.derive()` derives the four-value disposition shape from a JSON diff — no explicit map needed. This recce recommends keeping that design and just renaming the wire vocab at the REST boundary.
2. **The `?shape=auto` behaviour referenced in `HANDOFF_EVAL_PKG_AS_DATASET_PROPOSALS.md` is agent-side mock-server-only.** The Gemma `CurationWebService` currently has no `?shape=` parameter at all — it always returns the legacy-equivalent full payload. So the new `?shape=meta|full` recce specifies is genuinely new, not a wire-shape preservation.
3. **The audit-event discriminator string is data-coupled** — the `AgentProposalEvent` discriminator value in `AuditEventType.hbm.xml` cannot be safely renamed because production audit rows reference it. The Java class name can change freely; the wire-string is frozen.
4. **The `AGENT_PROPOSAL` Hibernate mapping uses `MaterializedClobType`** on `LONGTEXT` (mysql) / `CLOB` (h2) — the prod DDL upgrades to `JSON` but the Hibernate type stays the same. Means the 50–100 KB target with 2× headroom from `SCHEMA_UNIFIED_JUSTIFICATION.md` §8 is already fully accommodated; no column-sizing work.
5. **`CurationDraft` has a `parkedElements` JSON-array column.** This is an explicit per-element disposition store today — Paul's "JSON map" instruction is essentially asking to widen this from "just parked" to "all four dispositions". The existing design picks "park is the only explicit signal, the other three derive from the diff" precisely to keep the column small. Recce keeps that design and recommends only widening if a clear use case emerges (e.g. per-decision curator notes).
