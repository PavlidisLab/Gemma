# gemma-ui ↔ gemma-rest endpoint gap analysis

Recce date: 2026-05-21. Worktree: `agent-gemma-ui-endpoint-gap` (baseline `59d9ffbf34`).

Purpose: inventory every REST URL the `gemma-ui` frontend
(`/Users/pzoot/Dev/gemma-ui`, two sub-apps: `apps/browser` and
`apps/curation`) calls under `/rest/v2/...`, cross-reference each
against the actual `*WebService.java` surface in
`gemma-rest/src/main/java/ubic/gemma/rest/`, and produce a precise gap
list the orchestrator can use to plan implementation work.

Out of scope: agents-side endpoints (`/propose/*`, `/find-publication/*`,
`/find-term`) which are proxied to a separate FastAPI service and
intentionally bypass gemma-rest.

---

## 1. Inventory totals

| Side  | Count |
|-------|-------|
| UI distinct `(method, path-template)` calls           | **48** |
| gemma-rest endpoints (handler methods, all classes)   | **112** |
| UI calls with an exact gemma-rest match               | **22** |
| UI calls with NO gemma-rest match (or wrong shape)    | **26** |
| **Match rate**                                        | **46%** |

The match rate looks worse than it is — see §5 (Dubious cases):
several "missing" routes are actually UI mocks / agents-side concerns
(`/candidates`, `/curation-proposals`, `/audits`, `/categories`,
`/me`/`/login`/`/logout`) that may never have been intended for
gemma-rest. Filtering those out, the "real" gap is roughly **12
endpoints** the orchestrator should think hard about.

### gemma-rest surface (by WebService class)

| Class                            | Class @Path           | Handler count |
|----------------------------------|-----------------------|---------------|
| `AnalysisResultSetsWebService`   | `/resultSets`         | 3             |
| `AnnotationsWebService`          | `/annotations`        | 10            |
| `AuthWebService`                 | `/`                   | 3 (`/login`, `/logout`, `/me`) |
| `DatasetVisualizationWebService` | `/datasets`           | 1 (`/{dataset}/heatmap-data`) |
| `DatasetsWebService`             | `/datasets`           | ~60           |
| `GeneWebService`                 | `/genes`              | 6             |
| `PlatformsWebService`            | `/platforms`          | 10            |
| `RootWebService`                 | `/`                   | 3 (root, `/users/me`, `/users/{username}`) |
| `SearchWebService`               | `/search`             | 1             |
| `TasksWebService`                | `/tasks`              | 1             |
| `TaxaWebService`                 | `/taxa`               | 8             |
| `TicketsWebService`              | `/tickets`            | 6             |

---

## 2. Implemented (UI ↔ rest match — sample)

These UI calls land on real endpoints with matching method:

| UI call                                                  | rest                                                                                          |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `GET /rest/v2/datasets`                                  | `DatasetsWebService.@GET /datasets`                                                           |
| `GET /rest/v2/datasets/{id}`                             | `DatasetsWebService.@GET /datasets/{dataset}`                                                  |
| `GET /rest/v2/datasets/{id}/samples`                     | `DatasetsWebService.@GET /datasets/{dataset}/samples`                                          |
| `GET /rest/v2/datasets/{id}/publications`                | `DatasetsWebService.@GET /datasets/{dataset}/publications`                                     |
| `GET /rest/v2/datasets/{id}/auditEvents`                 | `DatasetsWebService.@GET /datasets/{dataset}/auditEvents`                                      |
| `GET /rest/v2/datasets/{id}/design`                      | `DatasetsWebService.@GET /datasets/{dataset}/design`                                           |
| `PUT /rest/v2/datasets/{id}/design`                      | `DatasetsWebService.@PUT /datasets/{dataset}/design`                                           |
| `GET /rest/v2/datasets/{id}/geeq`                        | `DatasetsWebService.@GET /datasets/{dataset}/geeq`                                             |
| `GET /rest/v2/datasets/{id}/svd`                         | `DatasetsWebService.@GET /datasets/{dataset}/svd`                                              |
| `GET /rest/v2/datasets/{id}/quantitationTypes`           | `DatasetsWebService.@GET /datasets/{dataset}/quantitationTypes`                                |
| `GET /rest/v2/datasets/{id}/analyses/differential`       | `DatasetsWebService.@GET /datasets/{dataset}/analyses/differential`                            |
| `POST /rest/v2/datasets/{id}/analyses/differential`      | `DatasetsWebService.@POST /datasets/{dataset}/analyses/differential`                           |
| `PUT /rest/v2/datasets/{id}/samples/{bioAssayId}/outlier`| `DatasetsWebService.@PUT /datasets/{dataset}/samples/{bioAssayId}/outlier`                     |
| `GET /rest/v2/datasets/{id}/curationDetails`             | `DatasetsWebService.@GET /datasets/{dataset}/curationDetails`                                  |
| `PUT /rest/v2/datasets/{id}/curationDetails`             | `DatasetsWebService.@PUT /datasets/{dataset}/curationDetails`                                  |
| `GET /rest/v2/datasets/taxa`                             | `DatasetsWebService.@GET /datasets/taxa`                                                       |
| `GET /rest/v2/datasets/annotations`                      | `DatasetsWebService.@GET /datasets/annotations`                                                |
| `GET /rest/v2/datasets/categories`                       | `DatasetsWebService.@GET /datasets/categories`                                                 |
| `GET /rest/v2/platforms`                                 | `PlatformsWebService.@GET /platforms`                                                          |
| `GET /rest/v2/platforms/{id}`                            | `PlatformsWebService.@GET /platforms/{platform}`                                               |
| `GET /rest/v2/platforms/{id}/elements`                   | `PlatformsWebService.@GET /platforms/{platform}/elements`                                      |
| `GET /rest/v2/platforms/{id}/elements/{el}/genes`        | `PlatformsWebService.@GET /platforms/{platform}/elements/{probe}/genes`                        |
| `GET /rest/v2/genes/{id}`                                | `GeneWebService.@GET /genes/{genes}`                                                           |
| `GET /rest/v2/genes/{id}/locations`                      | `GeneWebService.@GET /genes/{gene}/locations`                                                   |
| `GET /rest/v2/genes/{id}/goTerms`                        | `GeneWebService.@GET /genes/{gene}/goTerms`                                                     |
| `GET /rest/v2/annotations/search`                        | `AnnotationsWebService.@GET /annotations/search`                                               |
| `GET /rest/v2/tasks/{taskId}`                            | `TasksWebService.@GET /tasks/{taskId}`                                                         |
| `GET /rest/v2/users/me`                                  | `RootWebService.@GET /users/me`                                                                |

---

## 3. Missing — by category

### 3a. Workflow / pipeline dispatch (path-shape drift)

The UI uses a flatter path scheme (`/datasets/{id}/<action>`) than
gemma-rest's task-namespaced scheme (`/datasets/{id}/tasks/<action>`).
Also `pipelineStatus` is `pipeline-status` in UI vs camelCase in rest.

| UI call (method + path)                                     | rest expected location                                                          | gap type           | effort   |
|-------------------------------------------------------------|---------------------------------------------------------------------------------|--------------------|----------|
| `GET  /rest/v2/datasets/{id}/pipeline-status`               | rest has `@GET /datasets/{dataset}/pipelineStatus` — path-shape drift           | path-rename or alias | trivial |
| `POST /rest/v2/datasets/pipeline-status` (batch, body=ids)  | rest has `@POST /datasets/pipeline-status` ✓ — **already matches** (re-verify response shape) | already matches    | verify   |
| `POST /rest/v2/datasets/{id}/preprocess`                    | rest: `@POST /datasets/{dataset}/tasks/preprocess`                              | path-rename or alias | trivial |
| `POST /rest/v2/datasets/{id}/preprocess/diagnostics`        | rest: `@POST /datasets/{dataset}/tasks/diagnostics`                             | path-rename or alias | trivial |
| `POST /rest/v2/datasets/{id}/batchInformation/fetch`        | rest: `@POST /datasets/{dataset}/tasks/batchInfo`                               | path-rename or alias | trivial |
| `POST /rest/v2/datasets/{id}/geeq/recalculate`              | rest: `@POST /datasets/{dataset}/geeq/recompute`                                | path-rename or alias | trivial |
| `POST /rest/v2/datasets/{id}/analyses/differential/{aid}/redo` | rest: `@POST /datasets/{dataset}/tasks/redo/{analysisId}`                    | path-rename or alias | trivial |
| `DELETE /rest/v2/datasets/{id}/analyses/differential/{aid}` | rest: `@DELETE /datasets/{dataset}/tasks/differential/{analysisId}`             | path-rename or alias | trivial |
| `POST /rest/v2/datasets/{id}/makePublic`                    | not in gemma-rest (legacy gemma-web action)                                     | new endpoint       | trivial  |
| `POST /rest/v2/datasets/{id}/makePrivate`                   | not in gemma-rest (legacy gemma-web action)                                     | new endpoint       | trivial  |

Recommendation: pick one naming convention and add aliases. The UI's
flatter `/datasets/{id}/<action>` reads better; the rest's
`/datasets/{id}/tasks/<action>` is more searchable. Either way the
gap is just `@Path` renames on existing handlers — no new business
logic. `makePublic` / `makePrivate` may already exist on a permissions
service — see §3c.

### 3b. Quantitation type — wrong shape

| UI call (method + path)                                                     | rest expected location                                                                  | gap type            | effort   |
|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|---------------------|----------|
| `PATCH /rest/v2/datasets/{id}/quantitationTypes/{qtId}` (body `{is_preferred}`) | rest has `@PATCH /datasets/{dataset}/quantitationTypes/{qtId}/preferred`              | path-shape drift + body-vs-suffix | trivial |

The UI sends a generic patch with `is_preferred` in the body; rest
has a dedicated `/preferred` suffix endpoint. Either widen the UI to
target the suffix, or wire a body-driven dispatcher on the rest side.

### 3c. Groups CRUD (entire feature missing from gemma-rest)

| UI call (method + path)                                  | rest expected location | gap type     | effort   |
|----------------------------------------------------------|------------------------|--------------|----------|
| `GET    /rest/v2/groups[?...]`                           | NOT IN GEMMA-REST      | new class    | medium   |
| `GET    /rest/v2/groups/{id}[?include_summaries=true]`   | NOT IN GEMMA-REST      | new endpoint | medium   |
| `POST   /rest/v2/groups`                                 | NOT IN GEMMA-REST      | new endpoint | medium   |
| `PATCH  /rest/v2/groups/{id}`                            | NOT IN GEMMA-REST      | new endpoint | medium   |
| `DELETE /rest/v2/groups/{id}`                            | NOT IN GEMMA-REST      | new endpoint | medium   |
| `POST   /rest/v2/groups/{id}/members`                    | NOT IN GEMMA-REST      | new endpoint | medium   |
| `DELETE /rest/v2/groups/{id}/members/{memberId}`         | NOT IN GEMMA-REST      | new endpoint | medium   |
| `GET    /rest/v2/datasets/{id}/groups[?include_summaries=true]` | NOT IN GEMMA-REST | new endpoint | medium   |

Needs a new `GroupsWebService` class wired to the existing
`UserGroupService` (or whatever owns group membership in the gsec
chain). Each handler is small; the cost is the DTO surface and ACL
gating, not the routes themselves.

### 3d. Candidates (screening queue)

| UI call (method + path)                            | rest expected location | gap type      | effort   |
|----------------------------------------------------|------------------------|---------------|----------|
| `GET    /rest/v2/candidates[?...]`                 | NOT IN GEMMA-REST      | new class     | large    |
| `GET    /rest/v2/candidates/{id}`                  | NOT IN GEMMA-REST      | new endpoint  | medium   |
| `POST   /rest/v2/candidates`                       | NOT IN GEMMA-REST      | new endpoint  | medium   |
| `POST   /rest/v2/candidates/bulk`                  | NOT IN GEMMA-REST      | new endpoint  | medium   |
| `PATCH  /rest/v2/candidates/{id}`                  | NOT IN GEMMA-REST      | new endpoint  | medium   |
| `DELETE /rest/v2/candidates/{id}`                  | NOT IN GEMMA-REST      | new endpoint  | medium   |

`Candidate` looks like a screening-queue concept (pre-import GEO
accession + curator notes). It may not be a real Gemma domain model
yet — see Dubious cases. If it isn't, the persistence layer is the
work, not the rest.

### 3e. Curation proposals (agents-side or gemma-rest?)

| UI call (method + path)                                              | rest expected location | gap type        | effort   |
|----------------------------------------------------------------------|------------------------|-----------------|----------|
| `GET   /rest/v2/datasets/{id}/curation-proposals[?status_filter=X]`  | NOT IN GEMMA-REST      | new endpoint    | large    |
| `GET   /rest/v2/curation-proposals?status_filter=X&limit=N`          | NOT IN GEMMA-REST      | new class       | large    |
| `GET   /rest/v2/curation-proposals/{id}`                             | NOT IN GEMMA-REST      | new endpoint    | medium   |
| `PATCH /rest/v2/curation-proposals/{id}`                             | NOT IN GEMMA-REST      | new endpoint    | medium   |

The UI's `proposals.ts` calls these. Adjacent calls in the same file
(`POST /propose/{accession}`, `POST /find-publication/{accession}`)
go to the agents-side FastAPI proxy. The `/rest/v2/curation-proposals`
routes may also be intended for the agents-side service — see Dubious
cases.

### 3f. Audits (separate from auditEvents)

| UI call (method + path)                                      | rest expected location | gap type        | effort   |
|--------------------------------------------------------------|------------------------|-----------------|----------|
| `GET   /rest/v2/datasets/{id}/audits`                        | NOT IN GEMMA-REST      | new endpoint    | medium   |
| `POST  /rest/v2/datasets/{id}/audits` (start audit)          | NOT IN GEMMA-REST      | new endpoint    | medium   |
| `GET   /rest/v2/audits`                                      | NOT IN GEMMA-REST      | new class       | medium   |
| `GET   /rest/v2/audits/{id}`                                 | NOT IN GEMMA-REST      | new endpoint    | medium   |
| `PATCH /rest/v2/audits/{id}` (disposition update)            | NOT IN GEMMA-REST      | new endpoint    | medium   |
| `POST  /rest/v2/audits/{id}/finalize`                        | NOT IN GEMMA-REST      | new endpoint    | medium   |
| `POST  /rest/v2/audits/{id}/reopen`                          | NOT IN GEMMA-REST      | new endpoint    | medium   |

These are the curator-led audit reports (not the per-row
`auditEvents` audit-trail). Different concept; likely a separate
`AuditsWebService` if it belongs in gemma-rest at all (could be
agents-side — see Dubious cases).

### 3g. Publish

| UI call (method + path)                                       | rest expected location                                                | gap type   | effort   |
|----------------------------------------------------------------|------------------------------------------------------------------------|------------|----------|
| `POST /rest/v2/datasets/{id}/publish?reviewer=X`               | NOT IN GEMMA-REST                                                      | new endpoint | medium |
| `GET  /rest/v2/datasets/{id}/visibility`                       | NOT IN GEMMA-REST (rest has `@GET /datasets/{dataset}/permissions`)   | path-rename | trivial |

`publish` appears separate from `makePublic` in the UI's mental
model — it carries a `reviewer` query parameter, suggesting it's a
state-machine transition (curator-approves → publish) not just an ACL
flip. See Dubious cases.

### 3h. Auth — session helpers (RootWebService overlap)

| UI call (method + path)               | rest expected location                                       | gap type           | effort   |
|----------------------------------------|--------------------------------------------------------------|--------------------|----------|
| `GET  /rest/v2/me`                    | rest has `@GET /users/me` (RootWebService)                   | path-rename or alias | trivial |
| `POST /rest/v2/login`                 | rest has `@POST /login` (AuthWebService)                     | already matches    | verify   |
| `POST /rest/v2/logout`                | rest has `@POST /logout` (AuthWebService)                    | already matches    | verify   |

`/me` vs `/users/me` is a one-line `@Path` alias in `RootWebService`.

### 3i. Misc

| UI call (method + path)                            | rest expected location                                                | gap type    | effort   |
|----------------------------------------------------|------------------------------------------------------------------------|-------------|----------|
| `GET  /rest/v2/categories`                         | rest has `@GET /datasets/categories` and `@GET /annotations/categories` — UI wants a top-level alias | path-rename | trivial |
| `GET  /rest/v2/datasets/search?query=X&limit=N`    | NOT IN GEMMA-REST (rest has the global `@GET /search` w/ filters)     | new endpoint | medium  |
| `POST /rest/v2/datasets/import` (GEO accession)    | NOT IN GEMMA-REST (this is normally an agents-side action)            | new endpoint OR agents-side | medium |

`GET /datasets/search?query=...` is a UI convenience for the
`browser` import dialog (autocomplete-style). Either alias to the
global `/search` (filtered to datasets) or add a dedicated route on
`DatasetsWebService`.

---

## 4. Top 10 implementation priorities

Lead with low-effort high-value (curator workflow basics).

1. **`@Path` aliases on existing handlers** — `pipeline-status`,
   `geeq/recalculate`, `tasks/preprocess|diagnostics|batchInfo` →
   `preprocess|preprocess/diagnostics|batchInformation/fetch`,
   `tasks/redo/{aid}` → `analyses/differential/{aid}/redo`,
   `tasks/differential/{aid}` → `analyses/differential/{aid}`,
   `users/me` alias `/me`. **Trivial** — one line per route, no new
   service code. Unblocks ~8 UI features immediately.
2. **`POST /datasets/{id}/makePublic` + `makePrivate`** — wire to the
   existing security ACL service (the same machinery the gemma-web
   page used). **Trivial** if the service method already exists.
3. **`GET /datasets/{id}/visibility`** — rename or alias of
   `permissions`. **Trivial**.
4. **`PATCH /datasets/{id}/quantitationTypes/{qtId}`** — accept a
   body-driven patch so the UI doesn't need to know about the
   `/preferred` suffix. **Trivial**.
5. **`GET /datasets/search?query=...`** — small filter-wrapper around
   the existing search service or aliased to `/search`. **Medium**.
6. **`GroupsWebService` (CRUD + per-dataset listing)** — the eight
   `/groups` and `/datasets/{id}/groups` routes. Underlying
   `UserGroupService` exists; this is mostly DTO + ACL gating.
   **Medium**, but unlocks the curator UI's permissions screen.
7. **`POST /datasets/{id}/publish?reviewer=X`** — decide whether this
   is `makePublic` + audit-event, or its own workflow (see Dubious).
   **Medium** once the semantics are nailed.
8. **`GET /datasets/{id}/auditEvents`** — already wired; double-check
   the DTO matches what the UI expects (the UI calls it with `?limit=N`).
9. **`POST /datasets/{id}/audits` + `GET /audits` + `PATCH /audits/{id}`** —
   *if* curator audit reports belong in gemma-rest at all (see
   Dubious). **Medium per route**.
10. **`/candidates` and `/curation-proposals` family** — only if they
    are gemma-rest concerns. Likely better as agents-side endpoints
    (the UI already hits `/propose/*` and `/find-publication/*` on the
    same FastAPI). Defer until Paul clarifies. **Large** if real.

---

## 5. Dubious cases (need Paul's input)

These are the calls where the right answer isn't "implement on
gemma-rest" — it's "is this even a gemma-rest concern?". 6 cases:

1. **`/rest/v2/candidates` family (6 routes)** — screening queue.
   Looks like a curator-only concept that lives upstream of any
   imported dataset (pre-import GEO accession + curator notes).
   Could be (a) a real new Gemma domain model with its own table,
   (b) an agents-side concept proxied through `/rest/v2/` for UI
   convenience, (c) a UI-only mock that should never be wired. Which?

2. **`/rest/v2/curation-proposals` family (4 routes)** — the
   adjacent `/propose/*` and `/find-publication/*` already go to the
   agents-side FastAPI. Are these proposals also agents-side, or do
   they cross the boundary into gemma-rest (e.g. once a proposal is
   accepted, it gets persisted to the gemd schema)?

3. **`/rest/v2/audits` family (7 routes)** — curator-led audit
   reports (distinct from per-row `auditEvents`). Likely agents-side
   given the LLM-evaluation aspect, but if findings get persisted
   back to gemd they need a gemma-rest endpoint too. Mixed?

4. **`POST /rest/v2/datasets/{id}/publish?reviewer=X`** vs
   `POST /rest/v2/datasets/{id}/makePublic`** — synonyms or distinct
   workflow steps? The `reviewer` query param suggests the former is
   a state-machine transition (proposal accepted → published, with a
   curator on record); the latter is the legacy ACL flip. If they're
   one operation, kill `publish` and use `makePublic` + a
   reviewer-carrying request body. If they're distinct, keep both.

5. **`POST /rest/v2/datasets/import` (GEO accession import)** — this
   has historically been an agents-side concern. Should it become a
   gemma-rest endpoint that the UI calls directly, or should the UI
   continue to hit the agents-side proposer and let it persist via
   the existing gemd write path?

6. **`/rest/v2/categories` (top-level)** — does it alias
   `/datasets/categories`, `/annotations/categories`, or is it a
   third concept (top-level ontology category list for the curator
   "what category does this term belong to" picker)? The UI comment
   says "once the real Gemma side ships an endpoint we'll wire it
   up" — meaning today it's mocked.

---

## 6. Methodology + provenance

- UI grep:
  `grep -rnE '/rest/v2/' /Users/pzoot/Dev/gemma-ui/apps --include='*.ts' --include='*.tsx'`
  79 raw lines, deduplicated to 48 unique `(method, path-template)`
  pairs (dynamic `${id}` interpolations normalized to `{id}`; query
  strings dropped; doc-comment-only mentions excluded).
- rest grep:
  `find ... -name '*WebService.java' -exec grep -HnE '@(Path|GET|POST|PUT|PATCH|DELETE)' {} +`
  348 raw annotation lines across 12 WebService classes; combined
  class-level `@Path` + method-level `@Path` + HTTP verb gives 112
  handler-method endpoints.
- Match heuristic: exact template alignment (`{id}` ↔ any rest
  `{name}` PathParam treated as equivalent regardless of name); HTTP
  verb must match.
- Out-of-scope endpoints not counted in UI inventory: `/propose/*`,
  `/find-publication/*`, `/find-term` (proxied to agents-side
  FastAPI), and `/openapi.json` (Swagger meta).

End of recce.
