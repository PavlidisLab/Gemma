# Cursor Pagination — Step 1 Plan + Status

**Source recce:** `CURSOR_PAGINATION_RECCE.md` (Phase A, §4).
**Branch:** `cursor-pagination-step1` off `phase2-acl-migrate`.

Phase A of the recce was intentionally split into sub-steps because the
DAO + endpoint work (A.2/A.3) needs integration tests against the real
data layer, while the type scaffolding (A.1) is a self-contained,
compile-pure deliverable that can land first and unblock the rest.

## Step 1a — type scaffolding (THIS COMMIT)

Foundational types from recce §3.1 only. No DAO impact, no endpoint
impact, no behavior change. All four types ship with unit tests.

Files added:

- `gemma-core/src/main/java/ubic/gemma/persistence/util/Cursor.java`
  POJO + base64url-JSON encode/decode. Carries version, sortSpec, key
  tuple, direction. Normalizes integral Number key components to Long
  so encode/decode round-trips compare equal regardless of which
  numeric subtype the JSON parser picked.
- `gemma-core/src/main/java/ubic/gemma/persistence/util/CursorPage.java`
  Parallels `Slice<O>`; carries `nextCursor`, `prevCursor`, optional
  `totalElements`, no `offset`. `map(...)` preserves cursor metadata.
- `gemma-rest/src/main/java/ubic/gemma/rest/util/args/CursorArg.java`
  Parallels `OffsetArg`. Decodes via `Cursor.decode`, wraps
  `IllegalArgumentException` as `MalformedArgException` → 400.
- `gemma-rest/src/main/java/ubic/gemma/rest/util/CursorPaginatedResponseDataObject.java`
  Parallels `PaginatedResponseDataObject<T>`. Drops `offset`, adds
  `nextCursor` / `prevCursor`. `totalElements` is nullable and is
  `null` by default (cursor mode skips `COUNT(*)`).

Tests added (4 files, 30 assertions across 30 test methods):

- `CursorTest` (19) — round-trip forward/backward/compound,
  base64url-safe wire format, forward-direction omitted from wire,
  rejects empty/null/non-base64/non-JSON/missing-fields/unsupported-
  version/empty-key-tuple/invalid-direction, defensive copy of
  key tuple, equals/hashCode contract.
- `CursorPageTest` (4) — metadata round-trip, optional totalElements,
  empty page, `map(...)` preserves cursor + total + limit.
- `CursorArgTest` (5) — decode happy path, malformed inputs (null,
  empty, garbage, valid-base64-but-not-JSON) all surface as
  `MalformedArgException`.
- `CursorPaginatedResponseDataObjectTest` (2) — fields copied from
  `CursorPage`; `totalElements` forwarded when present.

Validation bar: compile-clean on `gemma-rest,gemma-core`; targeted
test run `mvn -pl gemma-rest test -Dtest='*Cursor*,*Pagination*'`
green (7 passing); `mvn -pl gemma-core test
-Dtest='CursorTest,CursorPageTest'` green (23 passing).

## Step 1b — DAO `loadValueObjectsAfter` (queued, NOT in this commit)

Recce §3.3 + §4 Phase A.2. Add
`ExpressionExperimentDao.loadValueObjectsAfter(Filters, Sort, Cursor,
int)` and impl. Keyset query on `id` first; index audit before
adding `(lastUpdated, id)`. Needs `extractKey(entity, sort)` helper.
Integration tests against `gemdtest`.

## Step 1c — `DatasetsWebService.getDatasets` cursor branch (queued)

Recce §4 Phase A.3. Add `?cursor=` query param next to `?offset=`;
mutual-exclusion check (400 if both); branch into `paginateByCursor`
on the existing endpoint. OpenAPI: document new param + response shape
+ mutual-exclusion rule. Mark `?offset=` deprecated *only after* Phase
B has full coverage (per recce §4 Phase C).

## Step 1d — integration tests (queued, depends on 1b + 1c)

Recce §4 Phase A.5. Six scenarios:
cursor=null behaves as legacy offset; cursor=valid returns correct
next page; hasNext / nextCursor correct at list end; cursor+offset →
400; cursor stable when row inserted between pages; sort-must-end-in-
id assertion test for every cursor-enabled endpoint.

## Why split

Step 1a is type-only and compile-pure; landing it independently lets
parallel agents pick up 1b/1c against a known-stable type API and
reduces merge conflict risk in `phase2-acl-migrate`. It also gives
the integration-test work (1d) something concrete to mock against
before the DAO and web service wiring exist.
