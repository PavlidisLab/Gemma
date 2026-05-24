# Handoff to UIB — audit trail compact + exclude-empty options

**Filed:** 2026-05-23
**For:** UIB (curation-UI side)
**Status:** Server-side landed; deploys with the next build-hook redeploy.

## What landed

`GET /rest/v2/datasets/{id}/auditEvents` grew two optional query parameters. Defaults are unchanged — full fidelity, no collapsing, no filtering.

| Param | Default | Effect |
|---|---|---|
| `compact=true` | `false` | Collapse consecutive same-(eventType, performer) events into ONE entry carrying `collapsedCount` + `lastOccurrence`. The first event's `note` is kept verbatim; the others' messages are not concatenated. |
| `excludeEmpty=true` | `false` | Drop entries with NO eventType AND blank `note` AND blank `detail` — i.e. the boring "something was touched" ticks that don't add anything to a curator scanning the trail. |

Both compose. When both are set, `excludeEmpty` runs FIRST, then `compact` collapses over the survivors. This is the recommended "story-telling" view for the curator UI.

## Response shape

### Default mode (no params, or `compact=false`)
Unchanged. Each entry is the existing `AuditEventValueObject`:
```json
{ "data": [ { "id": ..., "date": "...", "performer": "alice", "action": "U", "actionName": "Update", "eventType": "ubic.gemma...CommentedEvent", "eventTypeName": "CommentedEvent", "note": "...", "detail": "..." }, ... ] }
```

### `compact=true`
Each entry is now a `CompactAuditEventValueObject` — a SUPERSET of the legacy shape via `@JsonUnwrapped`. All the old fields stay at top level; two new fields are added:

```json
{
  "data": [
    {
      // ... all legacy AuditEventValueObject fields, flat (no nesting) ...
      "id": ...,
      "date": "2026-05-20T14:00:00.000+00:00",
      "performer": "alice",
      "eventType": "ubic.gemma...CommentedEvent",
      "note": "first message of the run",
      "detail": null,
      // NEW:
      "collapsedCount": 3,
      "lastOccurrence": "2026-05-20T14:05:23.000+00:00"
    },
    ...
  ]
}
```

- `collapsedCount` — run length. `1` for solo entries.
- `lastOccurrence` — date of the LAST event in the collapsed run. Equals `date` for solo entries.

### `excludeEmpty=true`
Same shape as default — just a subset of the entries.

### `compact=true&excludeEmpty=true`
`CompactAuditEventValueObject` shape, applied to the filtered list.

## Cursor mode

Both options work with cursor pagination (`?cursor=...&limit=...`). Compression and filtering happen **within the response page only** — runs are never merged across cursor boundaries. The cursor token in the response navigates the underlying unfiltered/uncollapsed sequence so prev/next work consistently regardless of which view options are on.

## What to do in the UI

The recommended default view for the dataset audit-trail panel:

```
GET /rest/v2/datasets/{id}/auditEvents?compact=true&excludeEmpty=true
```

Render each entry once. When `collapsedCount > 1`, render a small badge like `×3` next to the entry, and show the time range `date → lastOccurrence` somewhere readable. The user can toggle a "show all" checkbox in the UI that strips both params to fall back to full fidelity (useful for forensic detail).

## Implementation refs

- Server: `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java` (look for `getDatasetAuditEvents`, `collapseAuditEvents`, `isEmptyUpdate`)
- New wrapper VO: `gemma-rest/src/main/java/ubic/gemma/rest/CompactAuditEventValueObject.java`
- Tests: `DatasetsWebServiceTest.testGetDatasetAuditEventsCompact*` + `testGetDatasetAuditEventsExcludeEmpty*` (7 cases total)
- Commits: `757245d5fe` (compact), `78276f7e65` (excludeEmpty + compose-with-compact)
