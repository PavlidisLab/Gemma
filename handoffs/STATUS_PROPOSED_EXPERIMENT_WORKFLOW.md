# STATUS — Proposed-experiment workflow (Preboarding + AgentProposal)

**From:** Gemma side (phase2-acl-migrate)
**Date:** 2026-05-21 (implementation landed)
**Re:** `HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md`

## State

LANDED on branch `feat-proposed-experiment-workflow` (worktree under
`.claude/worktrees/agent-proposed-experiment-workflow`). Three commits:

| sha (short) | scope |
|---|---|
| `b6a7c62519` | entities + HBM + services + Flyway V11(mysql)/V13(h2); 15 service-level tests |
| `c85fbae2fb` | `PreboardingWebService` REST endpoints; 18 web-service tests |
| `<this commit>` | wire `CurationWebService` private endpoints to `AgentProposalService`; this STATUS file |

Tip SHA: `<this commit>` (see git log on `feat-proposed-experiment-workflow`).

Total: 33 pure-Mockito tests green. Compile-clean across `gemma-core` +
`gemma-rest`. Branch is NOT pushed and NOT merged; orchestrator's call when
to integrate.

## Decision log

| open question | spec recommendation | this implementation | rationale |
|---|---|---|---|
| #1 promotion mechanics | agnostic | **new-row + FK rebind** | Hibernate single-table discriminator UPDATE is awkward (`UPDATE INVESTIGATION SET class = ...` on a class column with cached collections invalidates the L2 cache for the row + locks it); the EE often already exists from the loader pipeline (we're rebinding *to* a known EE id, not flipping the preboarding's class). FK rebind is one HQL `UPDATE AgentProposal SET investigation = :to WHERE investigation = :from` plus two `session.update` calls on the workflow-state columns. The audit-trail continuity question (preboarding's `PreboardingCreatedEvent` + `AgentProposalEvent` rows stay on the preboarding's `AUDIT_TRAIL_FK`) is left as-is for v1 — the preboarding row is retained as history, the EE picks up its own `PreboardingPromotedEvent`. A follow-on cycle can rebind `audit_event.audit_trail_fk` if the curator UI complains about a split trail. |
| #2 payload size | MySQL JSON or LONGTEXT (either OK) | **MySQL JSON, H2 CLOB** | JSON gives queryability over the payload (`JSON_EXTRACT`, `JSON_CONTAINS`) without a full LONGTEXT scan. Hibernate's `MaterializedClobType` reads/writes the same `String` against either column type, so the H2 sibling Flyway (V13) uses CLOB and tests are agnostic. |
| #3 where state 1 lives | create preboarding on agent run | **same — collapse states 1+2 on agent run** | Per spec recommendation. The agent creates the preboarding when it targets an unknown accession; curators have a single triage surface. |
| #4 ACL on preboarding rows | team-visible permissive default | **same** | `PreboardingExperiment` inherits Investigation's ACL machinery; no per-row ACE created explicitly. The ACL aspect treats new rows with the same default the EE creation path does. Tightening at promotion is the curator's choice via the existing ACL endpoints; it isn't automatic. |
| #5 auto-promote on data-load detection | yes, `apply_latest_proposal=false` | **endpoint shipped; loader integration deferred** | `POST /preboarding/{id}/promote` is in place. Wiring it into the GEO loader pipeline so it auto-fires when a preboarding's accession is loaded is out of scope for this commit (the spec's "Recommendation" reads as a future-direction note, not an acceptance-criteria item). The explicit-POST path covers the manual flow today. |

## Promotion mechanics: chosen approach (new-row + FK rebind)

- The endpoint `POST /preboarding/{id}/promote` takes `{ee_id, apply_latest_proposal}` in the body.
- The service:
  1. Loads the preboarding + EE; throws `PreboardingAlreadyPromotedException` if the
     preboarding's workflow state is past `Preboarding` (terminal-promoted marker
     check).
  2. Calls `AgentProposalService.rebindInvestigation(preboarding, ee)` —
     `UPDATE AgentProposal p SET p.investigation = :to WHERE p.investigation
     = :from`. Returns the rebind count.
  3. Sets the preboarding's workflow state to `Loaded` (terminal marker; the
     preboarding row is retained as history with no curatable artifacts).
  4. Sets the EE's workflow state to `Loaded` if it isn't already further
     along (`Curate`/`Process`/`Audit`/`Public` are preserved).
  5. Emits `PreboardingPromotedEvent` declaratively via `@Audited`; the EE is
     the FIRST arg so the audit row attaches to the EE's trail.
- `apply_latest_proposal=true` is accepted but not yet wired to the
  design/annotation-write chain; the response carries
  `applied_proposal_id=null` so the caller knows the server-side apply did
  NOT happen. Curator applies via the existing design-write /
  annotation-write endpoints. This is the conservative-default path called
  out in the spec ("agents propose, curators apply").

## Audit-trail continuity caveat

With new-row + FK rebind the `AgentProposalEvent` and `PreboardingCreatedEvent`
rows stay on the preboarding's `AUDIT_TRAIL_FK`. The promoted EE's audit trail
only carries the `PreboardingPromotedEvent` (and whatever the loader pipeline
already wrote on it). Reading "the full agent history of this EE" needs a
two-trail query.

This is a deliberate v1 simplification — moving `audit_event.audit_trail_fk`
from the preboarding's trail to the EE's would be a per-row UPDATE we'd rather
defer until the curator UI demonstrates it actually needs the unified view.
The preboarding row + its trail are retained, so no history is lost; only the
join key is split.

If the curator UI decides the split trail is unworkable, the next iteration
would add `auditTrailService.rebindAuditEvents(fromTrail, toTrail)` to the
promotion path, gated on a feature flag so the rebind cost (one UPDATE per
event) is only paid when desired.

## Acceptance criteria (handoff §"Acceptance criteria")

- [x] `PreboardingExperiment` JPA entity extends `Investigation`; Flyway
      migration adds discriminator value (single-table inheritance) + the
      three preboarding-specific columns (PREBOARDING_ACCESSION, PREBOARDING_SOURCE,
      PREBOARDING_IDENTIFYING_METADATA).
- [x] `AgentProposal` JPA entity exists with the column shape from the
      handoff's §"The model".
- [x] `POST /preboarding` creates a preboarding; idempotency on accession (409
      with existing id+type); emits `PreboardingCreatedEvent`.
- [x] `GET /preboarding/{id}` returns preboarding + latest proposal.
- [x] `GET /preboarding?accession=...` resolves accession → preboarding.
- [x] `POST /preboarding/{id}/proposals` appends a proposal; idempotent on
      `run_id`; emits `AgentProposalEvent` (via `@AuditedConditional`, only
      on the actual insert path).
- [x] `POST /preboarding/{id}/promote` promotes to EE; emits
      `PreboardingPromotedEvent`. The `apply_latest_proposal` flag is accepted
      but the server-side apply chain is deferred (out of scope, see
      decision-log row #5).
- [x] Auth: agent role can POST preboarding + proposals (group-based
      `GROUP_CURATOR` / `GROUP_ADMIN` / `GROUP_AGENT` rather than
      fine-grained `preboarding:write`); only curator/admin can promote.
- [x] All write methods use `@Audited(...)` / `@AuditedConditional(...)`
      where the auditable target is on the argument list; `createPreboarding`
      emits its event imperatively via `auditTrailService.addUpdateEvent`
      because the auditable target is constructed in the method body (the
      `AuditedAspect.findAuditable` helper only checks the argument list).
- [x] Audit-event payloads carry the proposal id in the audit NOTE (link
      back to `AgentProposal` row by id, not inline JSON). The structured
      `AUDIT_EVENT.PAYLOAD` column is a separate piece of work — using the
      NOTE field today.
- [ ] **Integration test exercises the full lifecycle.** Service-level
      unit tests (33) cover the surface; an integration test layered on
      `BaseIntegrationTest5` + a real DB would exercise the Flyway
      migrations + the `@Audited` aspect end-to-end. Deferred to the
      orchestrator pass; the unit tests pin the contract, the integration
      test would pin the SQL.
- [ ] **`gemma-rest` OpenAPI spec updated.** Annotations are in place
      (`@Operation` summaries, `@ApiResponse` codes, `@Tag(name = "Preboarding")`);
      the actual `swagger.json` regeneration is part of the orchestrator's
      merge pass (the repo regenerates it at build time).
- [ ] **Python client wrappers in `shared/gemma.py`** (sibling repo) —
      out of scope here; flagged as a follow-up in the agents-side memory.

## Wired consolidation: private `/datasets/{id}/curation-proposals`

Per `STATUS_CURATION_PROPOSALS.md` — the private API's two
501-stubbed proposal endpoints are now wired to the same
`AgentProposalService` the preboarding endpoints use:

- `POST /datasets/{id}/curation-proposals` — attach proposal to a loaded EE
  (idempotent on `run_id` — same 201/200 split as the preboarding path).
- `GET /datasets/{id}/curation-proposals` — list proposals newest first.

One entity, two REST surfaces, idential idempotency semantics. The private
API stays `@Hidden` per the curation-UI convention.

## Files added / modified

**Added (entities + services):**

- `gemma-core/src/main/java/ubic/gemma/model/expression/experiment/PreboardingExperiment.java`
- `gemma-core/src/main/java/ubic/gemma/model/expression/experiment/AgentProposal.java`
- `gemma-core/src/main/resources/ubic/gemma/model/expression/experiment/AgentProposal.hbm.xml`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/AgentProposalDao.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/AgentProposalDaoImpl.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/AgentProposalService.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/AgentProposalServiceImpl.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/PreboardingExperimentService.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/PreboardingExperimentServiceImpl.java`

**Added (audit event types):**

- `gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/eventType/PreboardingCreatedEvent.java`
- `gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/eventType/AgentProposalEvent.java`
- `gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/eventType/PreboardingPromotedEvent.java`

**Added (Flyway migrations):**

- `gemma-core/src/main/resources/db/migration/mysql/V11__agent_proposal_preboarding_experiment.sql`
- `gemma-core/src/main/resources/db/migration/h2/V13__agent_proposal_preboarding_experiment.sql`

**Added (REST + tests):**

- `gemma-rest/src/main/java/ubic/gemma/rest/PreboardingWebService.java`
- `gemma-rest/src/test/java/ubic/gemma/rest/PreboardingWebServiceTest.java`
- `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/AgentProposalServiceTest.java`
- `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/PreboardingExperimentServiceTest.java`

**Modified:**

- `gemma-core/src/main/resources/hibernate.cfg.xml` — register `AgentProposal.hbm.xml`.
- `gemma-core/src/main/resources/ubic/gemma/model/analysis/Investigation.hbm.xml` — add `PreboardingExperiment` subclass entry.
- `gemma-core/src/main/resources/ubic/gemma/model/common/auditAndSecurity/eventType/AuditEventType.hbm.xml` — register the three new event types.
- `gemma-rest/src/main/java/ubic/gemma/rest/CurationWebService.java` — wire `/datasets/{id}/curation-proposals` to `AgentProposalService`.

## Out-of-scope follow-ups

- Auto-promote on data-load detection: the GEO loader pipeline doesn't yet
  call `/promote`. The endpoint is in place; wiring it is a separate cycle.
- Server-side `apply_latest_proposal=true` chain: accepted on the wire
  (Boolean flag), but doesn't drive factor/FV/tag creation yet. The curator
  applies through the existing design-write / annotation-write endpoints
  for v1.
- Audit-trail rebind at promotion: deferred (see "Audit-trail continuity
  caveat" above). The preboarding's trail is retained; the EE picks up its
  own promotion event. If the curator UI needs a unified view, add
  `rebindAuditEvents` to the promotion path.
- Integration test exercising the full Discovery -> Preboarding -> Loaded
  lifecycle against a real DB (Flyway + `@Audited` aspect end-to-end).
- OpenAPI `swagger.json` regeneration at build time.
- Python client wrappers in the sibling `gemma-curation-agents` repo
  (`shared/gemma.py`).
