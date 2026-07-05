# Curation commit — composite all-or-none write (`PUT /datasets/{id}/curation`)

**Date:** 2026-07-05
**Status:** spec / in coordination (CAB reply received, Paul steering scope)
**Sources:** `CAB_HANDOFF_2026_07_05_ALL_OR_NONE_CURATION_COMMIT.md`,
`CAB_REPLY_2026_07_05_ALL_OR_NONE_CURATION_COMMIT.md`, CAB draft model
`gemma-curation-agents/.../local_api/design_schemas.py`, and Paul: *"anything editable
in the curation UI"* must be committable, ids round-trip, new/deleted declared explicitly.

## Goal

Curation is a **local draft**. One **commit** ships the whole draft; the **server** diffs it
against current state and applies it **all-or-none** (one transaction). Thin client: it sends
a properly-formatted dump, not a choreographed sequence of per-field writes.

## Editable surface (every section the commit must cover)

Everything a curator can change in the UI. Each already has a per-resource write route except
where noted — those are the building blocks the composite reuses under one transaction.

| Section | Fields | Building block (reuse) | Gap |
|---|---|---|---|
| **basics** | shortName, **name**, **description** | `PUT /{id}/short-name` (admin) | **name + description have no write route** |
| **design** | factors → factorValues → statements; split decision; subset recs | `PUT /{id}/design` | — |
| **experimentTags** | experiment-level annotations | `PUT /{id}/annotations` | — |
| **sampleTags** | per-biomaterial characteristics | `PUT /{id}/samples/{ba}/characteristics` | — |
| **publications** | primary + other-relevant (PubMed / DOI) | `PUT /{id}/publications` | — (done) |
| **curationDetails** | troubled, needsAttention, note | `/tickets` (+ legacy `/curationDetails`) | note→comment migration pending |

Read-only banner fields (title as computed alias, taxon, platform, technologyType, loadedAt…)
are **not** in the commit — Gemma owns them on the read side.

## The envelope contract (from CAB reply, load-bearing)

Every collection on the wire is `items[] + deletedIds[]`:

- **Existing entity** carries its **`gemmaId`** — the server matches **by id, never by
  label/position** (label/position pairing has produced phantom matches). Anchors already in
  the CAB model: `FactorD.gemma_factor_id`, `BiomaterialD.source_biomaterial_id`; to be
  extended to factorValues, statements, sample characteristics, annotations before first commit.
- **New entity** carries **`gemmaId: null` + `clientRef`** (a temp handle unique within the
  document). The server creates it and returns **`idMap: { clientRef → newGemmaId }`** so the
  client reconciles without a reload.
- **Deletions are declared** in **`deletedIds[]`**, never inferred from absence. An entity
  merely missing from the dump is **not** a delete (same safety as `otherRelevantPublications`
  being required).
- **Partial intent:** section **present = authoritative** for that section; section **absent =
  untouched**. "Present + empty items + empty deletedIds" = no change. To **clear** a section,
  list its ids in `deletedIds` — never clear-from-empty.

## Concurrency, transaction, audit

- **Optimistic concurrency:** the envelope carries a **baseline token** (dataset
  `lastModified` / version as loaded). If the dataset moved since, the commit is rejected
  **409** and the client re-syncs. (Agent drafts can be hours-old; last-writer-wins would
  clobber concurrent human edits.)
- **Transaction:** one `@Transactional` facade calls the existing per-resource services in a
  fixed order. Any section failing rolls back the whole commit — nothing persists. **Do not
  fork the diff logic** already in the per-resource services; the facade orchestrates them.
- **Audit:** keep the **per-section / per-entity events** (ManualAnnotationEvent,
  design-updated, publication-updated, DatasetShortNameChangedEvent, …) wrapped under **one
  commit correlation id**, so the change set is queryable as a unit *and* individually.
  ⚠️ **Paul to confirm** (CAB's recommendation, not yet ratified) — vs one opaque
  `CurationCommit` event.

## Endpoints

- `PUT /datasets/{id}/curation` — apply the draft, all-or-none. Requires `ACL_SECURABLE_EDIT`.
- `POST /datasets/{id}/curation/preflight` — dry-run: same diff + report, **`applied:false`**,
  nothing persisted. Generalizes the existing `designPreflight` so the UI can show "what will
  change" before commit.

### Response (both endpoints)

```json
{
  "applied": true,
  "baseVersion": "…",            // the version applied against; new version on success
  "idMap": { "clientRef-1": 90210 },
  "sections": {
    "publications": { "created": 1, "updated": 0, "deleted": 0, "unchanged": 1 },
    "design":       { "created": 0, "updated": 2, "deleted": 1, "unchanged": 5 }
  },
  "correlationId": "…"
}
```

On failure (validation / stale base): HTTP 4xx / **409**, `applied:false`, the offending
entity + reason, nothing persisted.

## Building-block gaps to close first

1. **name + description write.** The only editable fields with no route. Options: standalone
   `PATCH /datasets/{id}` `{name?, description?}`, or fold straight into the commit's `basics`
   section. Auth: short-name is **admin-only**; name/description in the curation UI are curator
   edits → lean **`ACL_SECURABLE_EDIT`** (confirm — mismatch with short-name's admin gate is
   deliberate or not).
2. **Concurrency token source.** Confirm dataset `lastModified` is monotonic enough to be the
   baseline token, or add an explicit version column.

## Open items (need answers)

- **CAB:** send the extended pydantic envelope — `items[]` (gemmaId | clientRef) + `deletedIds[]`
  across *all* sections (currently only factors/biomaterials carry id anchors).
- **Paul:** audit granularity (per-entity + correlation id vs single event); name/description
  auth level (ACL_EDIT vs admin).
- **Phasing (proposed):** (1) add name/description write; (2) build the read-side version token;
  (3) commit facade over publications + basics + tags (smallest useful slice) with preflight;
  (4) fold in design + sampleTags. Each phase ships behind the same envelope.

## Related
`project_publications_write_api` (memory), `DWR_REST_GAP_AUDIT.md` (name/description = the
`updateBasics` gap), the per-resource PUTs listed above.
