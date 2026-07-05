# CurationDetails write-path retirement — phase 1

**Filed:** 2026-05-19. Companion to `AUDIT_AS_WORKFLOW_RECCE.md` Decision 1
("Tickets REPLACE `CurationDetailsService`"). This doc tracks the staged
retirement of the legacy `CurationDetails` write path.

## What this session landed

- **Deprecated** the five `CurationDetailsEvent` subclasses + the two abstract
  parents that drive the legacy `CurationDetails` write path:
  - `CurationDetailsEvent` (abstract base) — `@Deprecated`
  - `TroubledStatusFlagAlteringEvent` (abstract) — `@Deprecated`
  - `NeedsAttentionAlteringEvent` (abstract) — `@Deprecated`
  - `TroubledStatusFlagEvent` — `@Deprecated`, points at `QUALITY_REVIEW` ticket
  - `NotTroubledStatusFlagEvent` — `@Deprecated`, points at resolving the open ticket
  - `NeedsAttentionEvent` — `@Deprecated`, points at `GENERIC` / `BATCH_INFO_NEEDED` ticket
  - `DoesNotNeedAttentionEvent` — `@Deprecated`, points at resolving the open ticket
  - `CurationNoteUpdateEvent` — `@Deprecated`, points at ticket comments (deferred mapping)

  Javadoc on each subclass names the Ticket-layer equivalent.

- **Migrated** `DatasetsWebService.updateDatasetCurationDetails`
  (`PUT /datasets/{id}/curationDetails`) — the only production emitter of the
  Troubled / NeedsAttention event family. The endpoint is now a back-compat
  shim:
  - `troubled=true`  → opens a `QUALITY_REVIEW` ticket targeting the EE (no-op
    if one is already open).
  - `troubled=false` → resolves every open `QUALITY_REVIEW` ticket targeting
    the EE.
  - `needsAttention=true`  → opens a `GENERIC` ticket targeting the EE.
  - `needsAttention=false` → resolves every open `GENERIC` /
    `BATCH_INFO_NEEDED` ticket targeting the EE.
  - The optional `note` field becomes the ticket title on open / the
    transition reason on resolve.
  - The endpoint itself is `@Deprecated` and the OpenAPI summary points new
    callers at `/tickets`.

- `curationNote` still routes through the legacy `CurationNoteUpdateEvent`.
  The note-to-comment mapping is deferred (Decision 1 of the recce explicitly
  left "free-text notes" out of scope; revisit when the ticket-comment edit
  semantics are nailed down — see Decision 4).

## Validation

- Compile-clean: `mvn -pl gemma-core,gemma-rest -am clean test-compile -q`
  exits zero.
- Tests: `mvn -pl gemma-core,gemma-rest test -Dtest='CurationDetailsServiceImplTest,TicketServiceImplTest,TicketsWebServiceTest,DatasetsWebServiceTest'`
  passes 87/87 (the DatasetsWebServiceTest Spring context now resolves the
  ticket-layer beans — pre-existing breakage from the
  `aa18f8a323` ticket-write merge is also fixed by this change).

## What's left (queued for follow-on sessions)

### 1. `FactorValueNeedsAttentionServiceImpl` — **DONE 2026-05-19**

Migrated. `FACTOR_VALUE` added to `TicketTargetType` (the recce's suggested
new enum value lands without a schema migration — the column is
`VARCHAR(32)`). `FactorValueNeedsAttentionServiceImpl` now:

- `markAsNeedsAttention(fv, note)`: flips `fv.needsAttention=true` (a
  first-class field on FactorValue, NOT a CurationDetails projection — so
  it stays), then opens a `GENERIC` ticket via `TicketService.openTicket`
  whose targets are BOTH `FACTOR_VALUE`(fv.id) and
  `EXPRESSION_EXPERIMENT`(ee.id). Title is `"{fv}: {note}"`. Idempotent:
  no-op if an open GENERIC ticket already targets the FV.
- `clearNeedsAttentionFlag(fv, note)`: flips `fv.needsAttention=false`,
  then transitions every open FV-targeted ticket to `RESOLVED` via
  `TicketService.transition`, using `note` as the reason. Tickets already
  in `RESOLVED` / `CANCELLED` are skipped.

The legacy `FactorValueNeedsAttentionEvent` / `DoesNotNeedAttentionEvent`
emissions are gone — same pattern as the
`DatasetsWebService.updateDatasetCurationDetails` migration in
`f8496c04b4`. As with that migration, this leaves the embedded
`ee.curationDetails.needsAttention` boolean stale for callers that flow
through this service; per §3 the remaining ~5 read sites will be migrated
to the read shim and the boolean retired.

The legacy "is this the last outstanding FV?" cross-entity predicate is
no longer needed: every per-FV ticket also targets the EE, so the
aggregate "this EE has open tickets?" query
(`ticketService.findOpenForTarget(EXPRESSION_EXPERIMENT, eeId)`)
automatically reflects remaining sibling-FV work without a per-EE
predicate.

Files touched in this slice:

```
gemma-core/.../model/.../curation/TicketTargetType.java                 + FACTOR_VALUE enum value
gemma-core/.../service/.../experiment/FactorValueNeedsAttentionService.java       Javadoc rewritten to point at TicketService
gemma-core/.../service/.../experiment/FactorValueNeedsAttentionServiceImpl.java   ticket-based reimplementation
gemma-core/.../service/.../experiment/FactorValueNeedsAttentionServiceTest.java   asserts ticket open/transition shape
```

`mvn -pl gemma-core test -Dtest='*FactorValue*,*Ticket*'` passes 57/57.

### 2. `curationNote` → ticket comment

`PUT /datasets/{id}/curationDetails` still fires `CurationNoteUpdateEvent`
directly. Two open design questions before migration:

1. **Which ticket gets the comment?** A free-text note is currently a property
   of the EE, not a property of any specific work item. Options:
   - Hang notes off the most-recently-opened ticket targeting the EE.
   - Open an implicit "NOTE" ticket (new `TicketType`?) on first note write.
   - Keep notes as an EE-level surface (separate from tickets) — i.e. retire
     only the trouble/needsAttention surface and leave notes alone.
2. **Append-only vs in-place edit.** The sibling repo's
   `AUDIT_DISPOSITION_EDIT_HANDOFF.md` ships edit-in-place for comments;
   Gemma's ticket layer is currently append-only (Decision 4). Need to pick
   one before the migration.

Defer until the user weighs in.

### 3. `CurationDetails` entity + `lastNeedsAttentionEvent` / `lastTroubledEvent` / `lastNoteUpdateEvent` columns

The `CurationDetails` entity itself + its hibernate mapping + the three
`AuditEvent` FK columns still exist. They are still maintained by the
legacy `updateCurationDetailsFromAuditEvent` hook in
`AbstractCuratableDao.java` (triggered from `AuditTrailServiceImpl` when a
`CurationDetailsEvent`-typed event lands). This keeps the read-side flags
(`troubled` / `needsAttention` on `ExpressionExperiment`) in sync for the
~18 read callers that still go through `curatable.getCurationDetails()`.

**Migration sequence** for a future session:

1. Migrate all ~18 read callers from `curatable.getCurationDetails().getXxx()`
   to `CurationDetailsService.xxx(curatable)` (the read shim).
2. Once no reader touches the embedded `CurationDetails` directly, the
   columns can be dropped and the legacy `updateCurationDetailsFromAuditEvent`
   hook can be retired.
3. After that: delete `CurationDetails.java`, its `.hbm.xml`, and the column
   on `Investigation` / `ArrayDesign`. Final retirement.

A grep at filing time confirmed 5 production read sites still touch
`getCurationDetails()` directly:
- `ExpressionExperimentServiceImpl.java` (2 — troubled-walk for parent ADs)
- `GeeqServiceImpl.java` (1 — needs-attention check)
- `ExpressionExperimentDaoImpl.java` (1 — `setLastNeedsAttentionEvent(null)`
  during a thaw; needs careful handling)
- `AbstractCuratableValueObject.java` (1 — VO population)
- Plus 5 read-only `getTroubled()` calls in gemma-cli that read for branching
  but never write.

### 4. `gemma-rest` OpenAPI / wire deprecation policy

The `PUT /datasets/{id}/curationDetails` endpoint stays for one release as a
back-compat shim. Two things to coordinate before its hard removal:

1. **gemma-curation-ui migration.** The UI repo's
   `CROSS_REPO_COMPAT.md` matrix needs an entry once the new
   `POST /tickets` path lands as the canonical write surface. Until the UI
   moves over, removing the shim breaks the curator workflow.
2. **OpenAPI deprecation marker** is in place — the endpoint is
   `@Deprecated` at the Java level and the OpenAPI `summary` flags it. Pick
   a release version for hard removal and add it to `CHANGES.md` once the UI
   has moved.

## File-level audit

```
Deprecated:
  gemma-core/.../eventType/CurationDetailsEvent.java                    @Deprecated
  gemma-core/.../eventType/TroubledStatusFlagAlteringEvent.java         @Deprecated
  gemma-core/.../eventType/NeedsAttentionAlteringEvent.java             @Deprecated
  gemma-core/.../eventType/TroubledStatusFlagEvent.java                 @Deprecated
  gemma-core/.../eventType/NotTroubledStatusFlagEvent.java              @Deprecated
  gemma-core/.../eventType/NeedsAttentionEvent.java                     @Deprecated
  gemma-core/.../eventType/DoesNotNeedAttentionEvent.java               @Deprecated
  gemma-core/.../eventType/CurationNoteUpdateEvent.java                 @Deprecated

Migrated to Ticket layer:
  gemma-rest/.../DatasetsWebService.java::updateDatasetCurationDetails  → TicketService.openTicket / transition
    - troubled=true   → openTicket(QUALITY_REVIEW)
    - troubled=false  → transition(open QUALITY_REVIEW tickets, RESOLVED)
    - needsAttention=true  → openTicket(GENERIC)
    - needsAttention=false → transition(open GENERIC/BATCH_INFO_NEEDED tickets, RESOLVED)
  (endpoint also marked @Deprecated; OpenAPI points at /tickets)

Test fix (pre-existing breakage from aa18f8a323 ticket-write merge):
  gemma-rest/src/test/.../DatasetsWebServiceTest.java
    + TicketService, UserManager, UserService mock @Beans
    + real TicketsWebService bean (mocks injected)

Still on the legacy path (deferred):
  gemma-rest/.../DatasetsWebService.java::updateDatasetCurationDetails  curationNote branch (note-to-comment mapping TBD)
  gemma-core/.../AbstractCuratableDao.java::updateCurationDetailsFromAuditEvent  (the legacy write hook; retire when no callers emit Troubled/NeedsAttention/CurationNote events)

Migrated 2026-05-19 (FactorValueNeedsAttention → Ticket):
  gemma-core/.../FactorValueNeedsAttentionServiceImpl.java              → TicketService.openTicket / transition
    - markAsNeedsAttention   → openTicket(GENERIC, targets=[FV, EE])
    - clearNeedsAttentionFlag → transition(open FV-targeted tickets, RESOLVED)
  gemma-core/.../model/.../curation/TicketTargetType.java               + FACTOR_VALUE enum value (no schema migration; VARCHAR(32))
```

## Status

Phase 1 of the write-path retirement complete: legacy events deprecated with
Javadoc breadcrumbs, the highest-traffic production caller (the REST PUT
endpoint) migrated to the Ticket layer, doc filed.

**Update 2026-05-19**: §1 (`FactorValueNeedsAttentionService`) also migrated
to the Ticket layer — see §1 above for details. Remaining migration
(curationNote→comment, entity removal, read-side flag retirement) is
queued per §2–§4.
