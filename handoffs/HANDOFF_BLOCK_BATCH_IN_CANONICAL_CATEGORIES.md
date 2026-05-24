# Handoff — add `block` and `batch` to `/rest/v2/categories`

**Filed:** 2026-05-24 (gemma-ui side, Paul + Claude).
**For:** cab (bro 1 / agents repo — `gemma-curation-agents`, local_api)
**Status:** UI workaround landed in commit `a93b9b5` (allowlists `block` and `batch` so they don't render with the "off-list" amber outline). The server should ship them in the canonical list so the workaround becomes a no-op.

## Problem

`/rest/v2/categories` returns the canonical EFO factor-category list — 12 entries. Curators legitimately use `block` and `batch` as factor categories (nuisance factors for batch / blocking-design bookkeeping). They're recognized everywhere in the curation UI: `NO_BASELINE_CATEGORIES`, `isNuisanceFactor`, `NUISANCE_KEYWORDS`, the SampleDetailsPanel's "push to the right" sorter, etc.

But the EFO-derived endpoint doesn't list them, so `CategoryPicker.tsx` flagged them with a thin amber "off-list" outline:

```
const isUnknown =
  !!label &&
  list.length > 0 &&
  !list.some((c) => c.label.trim().toLowerCase() === label.trim().toLowerCase());
```

Paul confirmed today: "block is a canonical category, curators have to be able to use that."

## Ask

Add `block` and `batch` to whatever serves `/rest/v2/categories` on local_api (and on real Gemma if that endpoint diverges). Either:

1. Inject them into the static EFO-derived list at load time (cheap, code-local).
2. Maintain a parallel "non-EFO recognized categories" list and concatenate at response time (cleaner, more discoverable).

Both work for the UI's purpose. The UI only cares that `block` and `batch` appear in the array — URI is not required.

Suggested record (no URI; matches how the UI handles `OntologyTerm` with `uri: null`):

```json
{ "label": "block", "uri": null }
{ "label": "batch", "uri": null }
```

The UI's CategoryPicker auto-fills `value: { label, uri: null }` when the curator picks from the list, so URI-less entries flow through cleanly.

## After this lands

The UI workaround (`knownNuisanceCategory` allowlist in CategoryPicker.tsx) becomes redundant — both checks pass for these labels and the off-list flag stays off. Safe to keep the allowlist as a belt-and-suspenders, or drop it in a follow-up once the server response is verified.

## Verification

```bash
curl http://localhost:8095/rest/v2/categories \
  -H "Authorization: Bearer dev-token-123" \
  | jq '.data[] | select(.label == "block" or .label == "batch")'
```

Expect: two records, one per label.
