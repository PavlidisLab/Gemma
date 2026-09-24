# Cursor-Based Pagination — Reconnaissance

**Status:** recce only. No production code touched.
**Scope:** Phase 3, cloud-readiness / mobile-friendly REST API.
**Reference:** PHASE_3_VISION.md "cursor-based pagination".

---

## 0. Why cursor pagination

Gemma's REST API paginates with `?offset=N&limit=M` everywhere. Classic
OFFSET/LIMIT issues apply:

1. **O(N) on deep pages.** MySQL scans-and-discards the first N rows.
   `offset=10000` is ~10000x slower than `offset=0` on the same query.
2. **Drift under writes.** A row inserted into the middle of the
   sorted order between page requests yields duplicates or skips at
   the page boundary. Hits actively-curated data hard.
3. **Bot-friendly.** "page 1, page 2, …" is the canonical scrape
   shape and advertises total set size.
4. **Total-count cost.** Every paginated response carries
   `totalElements` → a `COUNT(*)` per request over the same filter.

Cursor pagination: server emits a token derived from the last row of
the current page (`{sortField, lastSeenId}`), client passes it back as
`?cursor=X`. DAO runs `WHERE (sortField, id) > (?, ?) ORDER BY sortField,
id LIMIT N` — index-friendly, drift-resistant, constant-cost.

---

## 1. Current state inventory

### 1.1 Pagination plumbing

Two shared types underpin every paginated endpoint:

- `ubic.gemma.rest.util.args.OffsetArg` (REST layer, `@DefaultValue("0")`)
  — accepts non-negative int, wraps it.
- `ubic.gemma.rest.util.args.LimitArg` (REST layer, `@DefaultValue("20")`,
  max 100) — accepts positive int up to `LimitArg.MAXIMUM = 100`.
- `ubic.gemma.persistence.util.Slice<O>` (core layer) — return type
  carrying `data`, `sort`, `offset`, `limit`, `totalElements`. Always
  Nullable on offset/limit/total.
- `ubic.gemma.rest.util.PaginatedResponseDataObject<T>` — response
  wrapper that copies the four Slice fields into a JSON object plus
  `groupBy` (the unique-key field for the response).
- DB layer: every paginated DAO method ends in
  `query.setFirstResult(offset).setMaxResults(limit)` on a HQL/Criteria
  query. See `ExpressionExperimentDaoImpl.loadValueObjects`,
  `FactorValueDaoImpl`, etc.

### 1.2 Endpoints with `?offset=` parameters

26 endpoint methods, across 7 web-service classes:

| File | Endpoint (verb + path) | Sort default | Notes |
| --- | --- | --- | --- |
| `DatasetsWebService` L253 | `GET /datasets` | `+id` (or `-searchResult.score` if query) | High traffic. Pre-loads all IDs when `query` is given, then slices in-memory |
| `DatasetsWebService` L766 | `GET /datasets/{ds}/samples` | (no sort param) | Per-dataset; small set |
| `DatasetsWebService` L787 | `GET /datasets/{ds}/samples/{sa}/...` | (no sort param) | Per-sample |
| `DatasetsWebService` L1528 | `GET /datasets/.../differential` (deprecated `offset`) | n/a | offset already deprecated — skip for cursor work |
| `DatasetsWebService` L1595, L1633 | dataset DE result endpoints | (no sort param) | offset optional |
| `DatasetsWebService` L2641, L2662 | dataset annotations sub-endpoints | (no sort param) | small per-EE sets |
| `PlatformsWebService` L94 | `GET /platforms` | sort param | |
| `PlatformsWebService` L132 | `GET /platforms/{platform}/datasets` | sort param | |
| `PlatformsWebService` L150 | `GET /platforms/{platform}/annotations` | sort param | |
| `PlatformsWebService` L172 | `GET /platforms/{platform}/experiments` | (no sort) | uses `paginate(Slice, ...)` |
| `PlatformsWebService` L193 | `GET /platforms/{platform}/elements` | (no sort) | |
| `PlatformsWebService` L220 | `GET /platforms/{platform}/probes` | fixed `id ASC` | |
| `PlatformsWebService` L250 | `GET /platforms/{platform}/...` | sort param | |
| `GeneWebService` L67 | `GET /genes` | (no sort) | |
| `GeneWebService` L128 | `GET /genes/{gene}/probes` | (no sort) | |
| `TaxaWebService` L156 | `GET /taxa/{taxon}/genes` | (no sort) | |
| `TaxaWebService` L193 | `GET /taxa/{taxon}/genes/{gene}/probes` | (no sort) | |
| `TaxaWebService` L242 | `GET /taxa/{taxon}/datasets` | sort param | |
| `AnnotationsWebService` L321, L378, L405, L460 | annotation search variants | sort param | search-style |
| `AnalysisResultSetsWebService` L111 | `GET /resultSets` | `+id` default | |
| `AnalysisResultSetsWebService` L170 | `GET /resultSets/{rs}` | n/a | offset is over result-rows inside one set |

**Tally:** 22 "true" paginated endpoints (the 4 dataset DE result
endpoints have offset semantics that are tied to result-row windowing
inside a single analysis, not collection pagination — flag separately).
One (`DatasetsWebService` L1528) has offset already deprecated.

### 1.3 Typical sort keys observed

- `+id` is the default for `/datasets`, `/resultSets`. `id` is the PK,
  always indexed, always unique → ideal cursor field.
- `searchResult.score` (datasets with query) — derived, not in DB,
  computed in-memory after a full-id fetch. Cursoring this is hard
  (see §5).
- Sort-by-name, sort-by-date — these are mostly on indexed columns but
  not unique → cursor must be `(sortField, id)` not `sortField` alone.
- A handful of endpoints have no sort param at all and rely on DAO
  default order. Those will need an explicit, stable sort order
  introduced before cursoring works correctly.

---

## 2. Cursor design

### 2.1 Two styles

**A. Opaque base64 cursor.** Server encodes `{sortField, lastSeenValue,
lastSeenId, direction}` into a base64-url string. Client treats it as
an opaque token: `?cursor=eyJzIjoiK2lkIiwidiI6MTIzNDV9`.

- ✓ Schema-independent — client can't construct a cursor from
  knowledge of the DB. Server can change the sort key shape later
  without breaking clients.
- ✓ Lets us version the cursor format (`v=2` field inside the JSON).
- ✓ Hides internal IDs from cursor strings (mildly useful for "feels
  scrape-resistant").
- ✗ Slightly more code (encode / decode helper).
- ✗ Slightly harder to debug from the URL alone.

**B. Field-and-value cursor.** Client passes `?after=2024-01-15T...&id=12345`
explicitly. The server reads them and constructs the WHERE clause.

- ✓ Trivially debuggable.
- ✓ Matches the `?filter=` style Gemma's REST API already uses
  (filter strings are user-constructible).
- ✗ Couples client to sort-key choice. If we change `+id` to
  `+lastModified`, all client cursor URLs break.
- ✗ Per-endpoint sort key means per-endpoint cursor param names.

### 2.2 Recommendation: **Opaque base64 cursor (A)**

Reasons specific to Gemma's REST API style:

1. The `?filter=` and `?sort=` args are already user-facing schema
   coupling. Adding `?after=...` is a third axis of "client knows
   what the columns are called" — we don't want to deepen that.
2. Gemma's filter/sort grammar (see `FilterArg`, `SortArg`) is parsed
   server-side from strings; the parser layer already handles the
   "client-supplied opaque-ish blob, server validates" shape. A
   `CursorArg` slots in next to those naturally.
3. Sort changes happen. `+id` becomes `+lastUpdated` becomes a
   compound. With opaque cursors we re-encode and the client is fine;
   with field-value cursors every change is a breaking change.
4. We can stuff a `v` field into the JSON for future format migrations
   without touching the URL shape.

Cursor payload (JSON, before base64-url encoding):

```json
{
  "v": 1,
  "s": "+id",
  "k": [12345],
  "d": "forward"
}
```

`s` = sort spec (server-validated against the endpoint's allowed
sorts), `k` = key tuple (one value per sort component, ending in `id`
to break ties), `d` = direction (`forward` / `backward` for prev-page).

### 2.3 Query rewrite — keyset pagination

For sort `+id`, sliding from "after id=12345 give me 20":

```sql
SELECT … FROM EXPRESSION_EXPERIMENT ee
WHERE ee.id > 12345
ORDER BY ee.id ASC
LIMIT 21    -- one extra to detect hasNext
```

For sort `+lastUpdated, +id`:

```sql
SELECT … FROM EXPRESSION_EXPERIMENT ee
WHERE (ee.last_updated, ee.id) > (?, ?)
ORDER BY ee.last_updated ASC, ee.id ASC
LIMIT 21
```

Two notes:

- `LIMIT N+1` so the server knows whether to emit `nextCursor`. The
  N+1th row is dropped before serialization.
- Composite key comparisons (`(a, b) > (?, ?)`) work in MySQL 5.7+ but
  need an index covering `(a, b)`. For the `id`-only cursor the
  existing PK is enough. For compound sorts, audit the index list
  before turning on cursor mode for that endpoint.

### 2.4 Response shape

```json
{
  "data": [ … ],
  "groupBy": ["id"],
  "sort": { … existing shape … },
  "limit": 20,
  "nextCursor": "eyJ2IjoxLCJzIjoiK2lkIiwiayI6WzEyMzY1XX0",
  "prevCursor": "eyJ2IjoxLCJzIjoiK2lkIiwiayI6WzEyMzQ2XSwiZCI6ImJhY2t3YXJkIn0",
  "totalElements": null
}
```

`totalElements` becomes `null` by default in cursor mode (we don't run
COUNT(*) on every request). Optional `?includeTotal=true` query param
opts in to the extra COUNT query for clients that genuinely need it.

---

## 3. Migration mechanics

### 3.1 New types

- `ubic.gemma.rest.util.args.CursorArg` — parallels `OffsetArg`. Holds
  decoded `Cursor` record after base64-url decode + JSON parse.
- `ubic.gemma.persistence.util.CursorPage<O>` — parallels `Slice<O>`.
  Holds `data`, `sort`, `limit`, `nextCursor`, `prevCursor`,
  `Optional<Long> totalElements`. Probably a record once we're on
  Java 17 fully in core (or POJO if not).
- `ubic.gemma.rest.util.CursorPaginatedResponseDataObject<T>` —
  parallels `PaginatedResponseDataObject<T>`.
- `ubic.gemma.persistence.util.Cursor` — POJO `{ int version, String
  sortSpec, Object[] keyTuple, Direction direction }` + base64
  encode/decode static methods.

### 3.2 Dual-mode parameter handling

Each endpoint gets both params during the transition:

```java
public PaginatedOrCursorResponse<T> getThings(
    …filters…,
    @QueryParam("offset")  @DefaultValue("0") OffsetArg offsetArg,
    @QueryParam("cursor")  CursorArg cursorArg,
    @QueryParam("limit")   @DefaultValue("20") LimitArg limitArg,
    @QueryParam("sort")    SortArg<…> sortArg
) {
    if (cursorArg != null) {
        // cursor mode
        return paginateByCursor(...);
    }
    // legacy offset mode (unchanged)
    return paginate(...);
}
```

Validation rule: `cursor` and `offset` are mutually exclusive. If both
are present, 400.

In OpenAPI / Swagger, mark `offset` with `deprecated=true` once cursor
support is in for that endpoint. Keep `limit` shared between both
modes (it means the same thing).

### 3.3 DAO layer

Each paginated DAO method needs a sister method:

```java
// existing
Slice<EEVO> loadValueObjects(@Nullable Filters f, @Nullable Sort s,
                             int offset, int limit);

// new
CursorPage<EEVO> loadValueObjectsAfter(@Nullable Filters f, @Nullable Sort s,
                                       @Nullable Cursor cursor, int limit);
```

Implementation translates the cursor's key tuple into a WHERE clause
on top of the existing Filters builder. The Sort component already
present in `Sort` should encode enough info to drive the cursor's key
extraction; we just need an `extractKey(entity, sort)` helper.

### 3.4 Sort guarantees

**Hard requirement before cursoring an endpoint:** the resolved sort
must be deterministic — i.e., must end with a unique key (typically
`id`). Many endpoints today implicitly assume "sort by X" is good
enough, even when X has duplicates. The cursor layer must append `+id`
to any sort that doesn't already end in a unique column. Add a unit
test asserting this for every cursor-enabled endpoint.

---

## 4. Phased plan

### Phase A — types + one high-traffic endpoint (~200 LoC)

1. Add `Cursor`, `CursorPage<T>`, `CursorArg`,
   `CursorPaginatedResponseDataObject<T>`.
2. Add `loadValueObjectsAfter(...)` to
   `ExpressionExperimentDao` + impl, keyset query on `id` first
   (then on `(lastUpdated, id)` as a follow-up).
3. Add `?cursor=` param to `DatasetsWebService.getDatasets` (L253).
   Branch on cursor-vs-offset.
4. OpenAPI: document the new param, response shape, mutual-exclusion
   rule.
5. Integration tests:
   - cursor=null → behaves exactly as today
   - cursor=valid → returns next-page rows with correct ordering
   - hasNext / nextCursor correct at end of list
   - cursor + offset → 400
   - cursor stable when a row is inserted between page 1 and page 2
   - cursor sort must end in `id` — assertion test

Estimated 200–250 LoC including tests. Leaves all 25 other
endpoints untouched.

### Phase B — sweep remaining endpoints (~600 LoC)

Add cursor support to the remaining 21 "true" paginated endpoints.
Bulk of the work is per-DAO `loadXxxAfter(...)` methods. Group by DAO
to amortize.

Priority order:
- Platforms (7 endpoints) — most uniform shape.
- Taxa (3), Gene (2) — small, similar shape.
- Annotations (4) — search-style, may need compound cursors.
- AnalysisResultSets — careful: one is "rows inside a result set",
  cursor on `(pvalue, id)` makes sense.

Skip or defer:
- `DatasetsWebService` L1528 — offset already deprecated.
- The "query-driven datasets" branch — see §5.

### Phase C — deprecate + remove offset (~50 LoC delete)

1. One release with both `?offset` and `?cursor` working, with
   `?offset` marked `@Deprecated` in OpenAPI and emitting a
   `Warning:` response header.
2. Following release: remove `OffsetArg` params, remove the
   `setFirstResult(offset)` paths in DAOs, drop
   `PaginatedResponseDataObject` (or repurpose it). Net code
   reduction.
3. Coordinate with gemma-curation-ui release schedule (§5).

---

## 5. Open questions / known hard cases

1. **Stable, indexed sort keys.** Many DAO methods sort on derived /
   joined columns. Audit indexes before cursoring on those — a cursor
   query on an unindexed sort column is *worse* than offset/limit.
   Concretely: `ExpressionExperimentDaoImpl.loadValueObjects` allows
   sorting on a lot of nested properties via `Filters`/`Sort` strings.
   Cursor mode should validate that the resolved column is in a known
   "indexed and cursor-safe" allowlist.

2. **gemma-curation-ui compatibility.** The new UI almost certainly
   builds page navigation against `?offset=N`. Phase A and Phase B
   are back-compat (offset still works); Phase C breaks any UI that
   hasn't switched. Coordination required before Phase C ships. The
   curation-ui repo is not in this checkout — confirm in the
   gemma-curation-ui repo what it depends on before scheduling the
   removal.

3. **Sort-by-relevance (search endpoints).** `DatasetsWebService`
   L253 with `?query=…` ranks by `searchResult.score`, which is
   computed in-memory after a full `loadIdsWithCache(filters, null)`.
   There is no DB column to cursor on. Options:
   - Keep offset for the query branch only.
   - Cache the full sorted ID list under a server-side session ID
     and cursor by "position in the cached list" — but that's
     effectively offset with extra steps.
   - Skip the query branch entirely. Flag in §4 Phase B.

4. **`totalElements` expectations.** Some clients (paginated UI grids
   with a "page N of M" widget) will break if `totalElements` is
   null. The opt-in `?includeTotal=true` query param keeps the COUNT
   query available, but UIs need to know it has to be passed. Phase
   B should ship a doc note that "page N of M" widgets must opt in.

5. **Cursor signing.** Right now the proposal is "base64-url the
   JSON, no HMAC". A client can forge an arbitrary cursor (which
   already requires the server to validate the sort spec). Probably
   fine — the cursor is read-only and access control is enforced by
   ACL anyway — but if we ever want "trust the cursor without
   re-validating", we'll want HMAC. Flag, defer.

6. **Backward cursor (prev page).** Easy with `direction=backward`
   in the cursor payload: reverse the WHERE and ORDER BY, re-reverse
   the returned rows. Slight extra complexity in the DAO. Optional
   for Phase A.

7. **Page-size cap interaction.** `LimitArg.MAXIMUM = 100`. We need
   `LIMIT N+1` for has-next detection. Make sure the DAO bumps the
   effective `setMaxResults` by 1, not the user-facing cap.

---

## 6. Summary

- 22 true paginated endpoints across 7 web-service classes use
  offset/limit today; shared `OffsetArg` / `LimitArg` / `Slice` /
  `PaginatedResponseDataObject` plumbing.
- Recommend opaque base64 cursor (A) — schema-independent, matches
  Gemma's existing arg-parsing idiom, future-proof against sort changes.
- Phase A: types + `/datasets` (~200–250 LoC). Phase B: 21 endpoints
  (~600 LoC). Phase C: deprecate-then-remove offset, gated on
  curation-ui migration.
- Blockers: indexed-sort-key audit, curation-ui coordination, search
  relevance sort can't be keyset-cursored without redesign.
