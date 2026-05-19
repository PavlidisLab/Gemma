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

### 1. `FactorValueNeedsAttentionServiceImpl`

Two emitters at `gemma-core/.../FactorValueNeedsAttentionServiceImpl.java`
remain on the legacy path:

| Method | Emits | Why it's tricky |
|---|---|---|
| `markAsNeedsAttention(FactorValue, note)` | `FactorValueNeedsAttentionEvent` (extends `NeedsAttentionEvent`) | Has cross-entity propagation logic: flipping a single FV's `needsAttention` flag should also raise the *owning EE's* needs-attention flag. The legacy event flows the EE flip through the audit-log + curation-details write hook. |
| `clearNeedsAttentionFlag(FactorValue, note)` | `DoesNotNeedAttentionEvent` (conditionally — only when all sibling FVs are also OK) | Has a non-trivial "is this the last outstanding FV?" check that walks the experimental design before deciding whether to clear the EE-level flag. |

**Suggested migration**: a `FactorValue` is not currently a `TicketTargetType`
(Decision 2 picked `EXPRESSION_EXPERIMENT` and `ARRAY_DESIGN` only, with new
values addable without schema migration). Add `FACTOR_VALUE` to `TicketTargetType`,
then `markAsNeedsAttention` opens a `GENERIC` ticket targeting BOTH the FV and
its owning EE; `clearNeedsAttentionFlag` resolves the FV-targeted ticket and,
when no other FV-targeted tickets remain on the EE, also resolves the
EE-targeted ticket (matches the existing "all FVs OK → clear EE flag"
predicate). One session of work.

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
  gemma-core/.../FactorValueNeedsAttentionServiceImpl.java              (needs FACTOR_VALUE TicketTargetType + cross-entity logic)
  gemma-rest/.../DatasetsWebService.java::updateDatasetCurationDetails  curationNote branch (note-to-comment mapping TBD)
  gemma-core/.../AbstractCuratableDao.java::updateCurationDetailsFromAuditEvent  (the legacy write hook; retire when no callers emit Troubled/NeedsAttention/CurationNote events)
```

## Status

Phase 1 of the write-path retirement complete: legacy events deprecated with
Javadoc breadcrumbs, the highest-traffic production caller (the REST PUT
endpoint) migrated to the Ticket layer, doc filed. Remaining migration
(FactorValueNeedsAttentionService, curationNote→comment, entity removal) is
queued per §1–§4 above.
