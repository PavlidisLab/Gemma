# HANDOFF: CAB embedding-rerank endpoint for `/annotations/search`

**Filed-by:** Paul Pavlidis (via gemma-rest Claude) — 2026-05-23
**Status:** agreed direction; design ready for CAB-side implementation.
**Repo touched:** `gemma-curation-agents` (CAB). gemma-rest side is queued in `gemma/handoffs/RECCE_ANNOTATION_SEARCH_RANKING.md` section 7.
**Priority:** behind the lexical ranker shipments (`?limit=20` cap + per-hit attribution); ships when those land.

---

## Why

Gemma's `/annotations/search` typeahead returns up to 200 hits ranked by Lucene relevance + lexical scoring (the new `AnnotationSearchRankingStrategy` family in gemma-rest: `lucene` / `usage` / `coverage` / `composite`). It works but isn't smart enough — "chronic itch" surfaces 386 zero-usage terms before the 49 actually-used ones. We want **embedding-based reranking** to push semantically-closest terms to the top, then cap the response at ~20 hits.

CAB already has a small embedding model loaded (used elsewhere in the curation pipeline). Rather than embed the model in gemma-rest's JVM (50-100MB heap, model-load cost on startup, GC pause risk on inference), gemma-rest **calls CAB** for the rerank. CAB stays the model owner; gemma-rest stays the single client contract.

## The endpoint

`POST /rerank/annotations` on CAB's `local_api`. Auth via the existing bearer (same as the rest of CAB's endpoints).

### Request

```json
{
  "query": "chronic itch",
  "hits": [
    {
      "uri": "http://purl.obolibrary.org/obo/UBERON_0001954",
      "label": "ammon's horn",
      "definition": "A part of the hippocampus characterized by...",
      "category": null,
      "category_uri": null,
      "usage_count": 0
    },
    ... up to 200 hits ...
  ]
}
```

- `query`: the raw curator-typed string from the search box.
- `hits`: the lexical-stage candidate set from gemma-rest (already Lucene/Jena-Text matched).
- Each hit carries the fields CAB might want to embed against — `label` (always), `definition` (when available; gemma-rest already batch-fetches it for the top-25), `usage_count` (signal for "is this term actually in use").
- If CAB later wants synonyms in the embedding, gemma-rest adds an optional `synonyms: [...]` field per hit to the payload. **Don't make CAB go fetch them.**

### Response

```json
{
  "reranked": [
    { "uri": "http://purl.obolibrary.org/obo/MP_0010073", "score": 0.91 },
    { "uri": "http://purl.obolibrary.org/obo/CHEBI_59683", "score": 0.83 },
    ...
  ]
}
```

- `reranked` is the SAME URIs from the request, in CAB's preferred order, with a similarity score.
- Optional `score` field (gemma-rest can use it for tie-breaking against lexical signals or just for telemetry; not required).
- CAB MUST return every URI from the request. If a URI is filtered out (e.g. embedding is degenerate / below threshold), put it at the bottom with `score: 0.0` rather than dropping it — gemma-rest decides what to truncate.

## **THE CYCLE-PREVENTION RULE — non-negotiable**

**Your rerank handler must NOT make outbound HTTP calls.** Specifically:

- No `gemma_api.get(...)`, no `requests.get('https://gemma.msl.ubc.ca/...')`, no `gemmapy.something()`.
- No re-fetching term parents / definitions / synonyms from gemma-rest while you're handling the rerank.
- The handler is **pure**: input = the request body verbatim, output = derived ONLY from those inputs + your local in-process embedding model.

### Why

If CAB calls back into gemma-rest during a rerank, we get cycles:

```
client → gemma-rest /annotations/search?rank=embedding
       → CAB /rerank/annotations
       → gemma-rest /annotations/term?uri=X  ← bad
       → ... potentially another rerank ...
```

Even one level of recursion bloats latency and creates a deadlock window where gemma-rest is blocked on CAB which is blocked on gemma-rest. Under any concurrent load, this is a thundering-herd into a livelock.

### What if your reranker needs richer context?

Whatever you need, **gemma-rest sends it in the request payload**. The contract above includes `definition` and `usage_count` because we anticipate you'll embed against more than the bare label. If you find you want synonyms, parents, the ontology namespace, anything — file a request to gemma-rest, and we extend the payload. Push the context to CAB. Don't pull it from CAB.

### How to enforce

- Don't import `gemmapy` (or any HTTP client) into the rerank module's call stack.
- Add a CI lint check that fails if anything under `gemma_curation_agents/local_api/rerank.py` (or wherever the handler lives) does `import requests` / `from gemmapy import`.
- Integration test the handler against a mocked network that's wired to refuse all outbound connections — handler runs to completion with zero network access.

## Performance budget

Typeahead latency budget end-to-end is ~500ms (curator types, sees results inline). Gemma-rest's lexical search is typically <100ms; CAB rerund budget is ~200-300ms. Beyond that, the typeahead feels slow.

- 200 hits × small-embedding inference is realistic in ~50-200ms on modern hardware for a 256-768 dim sentence model. Confirm with whatever model you're loading.
- If your model can't hit this budget, gemma-rest will degrade to the lexical `composite` ranker (see graceful-degradation below) and curator gets pre-embedding results — that's fine, log it.

## Graceful degradation (gemma-rest side, here for visibility)

gemma-rest's `EmbeddingRankingStrategy` MUST handle:

- CAB unreachable (connection refused) → fall back to `composite` ranker.
- CAB returns 5xx → fall back to `composite` ranker.
- CAB exceeds timeout (~500ms hard cap) → fall back to `composite` ranker.
- CAB returns malformed JSON or a URI set that doesn't match the request → fall back to `composite` ranker.

Each fallback logs a WARN with the trigger reason and bumps a Micrometer counter (`gemma.annotation_search.embedding_fallback_count{reason="..."}`) so we can observe CAB health from gemma-rest's metrics scrape.

## Sequence of work

1. **Behind the gate**: gemma-rest ships `?limit=20` cap + per-hit attribution first (existing recce, sections 6 and 7 of `RECCE_ANNOTATION_SEARCH_RANKING.md`). UI lands.
2. **CAB**: implements `POST /rerank/annotations` per this contract. Local-only first; no integration with gemma-rest yet.
3. **gemma-rest**: ships `EmbeddingRankingStrategy` bean + `gemma.curationAgent.url` config + the graceful-degradation paths. Tests CAB integration in a local-mode docker-compose where both are running.
4. **UI**: switches to `?rank=embedding` once both sides are deployed to staging and observed stable for a week.

## Cross-references

- `gemma/handoffs/RECCE_ANNOTATION_SEARCH_RANKING.md` section 7 — gemma-rest side design.
- `gemma/gemma-rest/src/main/java/ubic/gemma/rest/ranking/` — existing strategy beans (lucene/usage/coverage/composite). `EmbeddingRankingStrategy` lives here.
- `gemma-curation-agents/gemma_curation_agents/local_api/server.py` — where the new `/rerank/annotations` handler attaches.
- `RECCE_LOCAL_API_REST_V2_ROOT_HANDLER.md` (CAB-side, prior session) — pattern for adding a new endpoint without scope creep.
