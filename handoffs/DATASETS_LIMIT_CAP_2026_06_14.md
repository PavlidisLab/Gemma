# Handoff — Raise `/rest/v2/datasets` limit cap (or expose it on the wire)

**From:** Paul + UI Claude (gemma-ui)
**To:** Bro 1 (agents)
**Filed:** 2026-06-14

## TL;DR

The `/rest/v2/datasets` endpoint caps `limit` at 100 (FastAPI pydantic
validator: `less_than_equal: 100`). Curator workflows naturally want
to see the whole ticket on one page — typical curation tickets carry
≥200 EE targets ("Boss-critic 200" being the immediate motivator).
Raise the cap, or expose it so the UI can self-clamp without baking
the number in.

## How I noticed

Paul 2026-06-14: "this is broken: tickets/51 — no experiments are
shown at all no matter what." UI had just shipped a 200-default
page size to address his earlier ask ("user-settable number per
page, with a default of 200"). With limit=200 on the wire:

```bash
curl -s -H "Authorization: Bearer dev-token-123" \
  "http://localhost:5175/rest/v2/datasets?ids=20005,36366,…&limit=200&offset=0" \
  | jq .detail
```

```json
[
  {
    "type": "less_than_equal",
    "loc": ["query", "limit"],
    "msg": "Input should be less than or equal to 100",
    "input": "200",
    "ctx": { "le": 100 }
  }
]
```

The 422 is correct — the UI was overshooting — but the rejection
shape silently empties the queue (no error UI surfaces a 422 today;
the query just returns no data). UI clamped to 100 as a workaround
(commit on the gemma-ui side; PAGE_SIZE_DEFAULT = 100, picker
options 25/50/100). That fixes the symptom but caps a real workflow:
the typical ticket is 200 members and we want to show it in one
page.

## Ask

Pick one:

### A. Raise the cap (preferred)

Bump the validator's max to 500 or 1000. The endpoint is
ticket-/group-scoped via `ids=` in the use cases that need it, so
the query plan is bounded by the caller's id list; the cap was a
defensive-against-abuse number, not a perf number. With `ids`
constraining the row count, raising the cap doesn't change query
shape meaningfully.

If you want to keep some upper bound, 500 covers every curation
ticket we ship today with headroom. 1000 covers the calibration
batches without us touching the cap again.

### B. Expose the cap on a `/rest/v2/datasets/_meta` (or similar)

Return `{ "limit_max": 100 }` so the UI reads the server's max at
runtime and clamps the picker options accordingly. Doesn't unblock
the 200-on-one-page workflow but does mean we never silently empty
the queue on a future bump.

(A) is more useful. (B) is a fallback if (A) is too risky.

## Out of scope

- The 422-empties-queue UI symptom is a separate UI follow-up — we
  should surface "server rejected the limit; capped at <n>" as a
  visible warn instead of an empty list. Filed separately as a UI
  task; not blocking this handoff.
- Other endpoints that may have similar caps (`/rest/v2/audits`,
  `/rest/v2/datasets/{id}/proposals`, `…/groups`, `/curations`). If
  you raise the cap on one, sweep the rest in the same pass so the
  UI doesn't trip a different one next time.
