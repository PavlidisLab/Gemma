# gemma-curation-ui REST API contract audit

Phase 3 recce: which `/rest/v2/**` endpoints does the React-based
replacement frontend (`gemma-curation-ui`) consume, and how do they
line up against gemma-rest's actual surface?

Recce only - no code changes. Source repos read-only.

Baseline: `phase2-acl-migrate` @ `08e760bdaf`. UI snapshot taken
2026-05-18 from `~/Dev/gemma-curation-ui/` working tree.

---

## 1. UI repo location and structure

**Canonical name: `gemma-curation-ui`** at
`/Users/pzoot/Dev/gemma-curation-ui/`. The MEMORY.md alias
`gemma-ui` matches the same path; `~/Dev/gemma-ui/` does NOT exist.

Monorepo with two Vite/React apps under `apps/`:

| App | Path | Purpose | Backend target |
|---|---|---|---|
| **curation** | `apps/curation/` | Curator workflow UI (audits, proposals, dispositions, design comparison, calibration) | FastAPI mock (`gemma-curation-agents`) on `localhost:8080`, **NOT real gemma-rest**; also a proposer service on `localhost:8090` for `/propose` + `/audit/*/stream` (SSE) |
| **browser** | `apps/browser/` | Public dataset/platform browser, replaces legacy Vue site | Real gemma-rest (`staging-gemma.msl.ubc.ca` default) |

Critical context from `apps/curation/CROSS_REPO_COMPAT.md`: the
curation app's `/rest/v2/...` namespace is **the wire contract of the
mock FastAPI in gemma-curation-agents**, NOT the Java gemma-rest
service. Names collide intentionally to keep one hostname in
production, but **most curation paths have no Java implementation
and never will** - they live in Python on the agent side.

Vite proxy config:

- `apps/browser/vite.config.ts`: `/rest` proxies to `GEMMA_TARGET`
  (defaults to real Gemma staging).
- `apps/curation/vite.config.ts`: `/rest` proxies to
  `GEMMA_CURATION_URL` (defaults to `http://localhost:8080`, but
  this is the FastAPI mock, not gemma-rest); `/propose`, `/audit/*`,
  `/health`, `/feedback` route to a separate agent service on
  `:8090`.

---

## 2. /rest/v2/** endpoints the UI consumes

Aggregated from the 86 matches across `apps/curation/src/**` and
`apps/browser/src/**`. Path templates normalized; `{id}` for path
params.

### 2a. Browser app (`apps/browser/`)

These are the real gemma-rest consumers.

| Method | Path | File:line |
|---|---|---|
| GET | `/rest/v2/datasets` | `api/endpoints.ts:43,244` |
| GET | `/rest/v2/datasets/categories` | `api/endpoints.ts:84` |
| GET | `/rest/v2/datasets/annotations` | `api/endpoints.ts:112` |
| GET | `/rest/v2/datasets/platforms` | `api/endpoints.ts:366` |
| GET | `/rest/v2/datasets/taxa` | `api/endpoints.ts:385` |
| GET | `/rest/v2/datasets/{id}` | `api/endpoints.ts:417` |
| GET | `/rest/v2/datasets/{id}/annotations` | `api/endpoints.ts:404` |
| GET | `/rest/v2/platforms` | `api/endpoints.ts:155,196` |
| GET | `/rest/v2/platforms/{id}` | `api/endpoints.ts:185` |
| GET | `/rest/v2/platforms/{id}/elements` | `api/endpoints.ts:174,266` |
| GET | `/rest/v2/platforms/{id}/elements/{eid}/genes` | `api/endpoints.ts:292` |
| GET | `/rest/v2/users/me` | `api/endpoints.ts:390` |
| GET | `/rest/v2/annotations/search` | `api/endpoints.ts:439` |
| GET | `/rest/v2/openapi.json` | `api/endpoints.ts:457` |

Browser app distinct count: **14 endpoints**, all GET.

### 2b. Curation app (`apps/curation/`)

These hit the FastAPI mock under a /rest/v2 namespace. Many overlap
in shape (datasets) but the **mock's behaviour and persistence layer
are independent of gemma-rest**.

| Method | Path | File:line | In gemma-rest? |
|---|---|---|---|
| GET  | `/rest/v2/me` | `api/session.ts:61` | NO (gemma-rest has `/users/me`) |
| POST | `/rest/v2/login` | `api/session.ts:71` | NO |
| POST | `/rest/v2/logout` | `api/session.ts:82` | NO |
| GET  | `/rest/v2/datasets` | `api/datasets.ts:67`, `api/workflow.ts:115` | YES |
| GET  | `/rest/v2/datasets/search` | `api/datasets.ts:115` | NO (gemma-rest uses `/search` for free-text; datasets are filtered via `/datasets?query=`) |
| POST | `/rest/v2/datasets/import` | `api/datasets.ts:144` | NO |
| PUT  | `/rest/v2/datasets/{id}/visibility` | `api/datasets.ts:222` | NO |
| POST | `/rest/v2/datasets/{id}/publish` | `api/datasets.ts:239` | NO (closest: `/datasets/{id}/permissions`) |
| GET  | `/rest/v2/datasets/{id}/design` | `api/design.ts:12` | NO (gemma-rest has it but as TSV download, not JSON) |
| POST | `/rest/v2/datasets/{id}/design` | `api/design.ts:39` | YES (gemma-rest: `PUT /datasets/{id}/design`, TSV upload) - **verb mismatch** |
| GET  | `/rest/v2/datasets/{id}/curationDetails` | `api/curation.ts:39` | YES |
| PUT  | `/rest/v2/datasets/{id}/curationDetails` | `api/curation.ts:64` | YES |
| GET  | `/rest/v2/datasets/{id}/auditEvents` | `api/history.ts:44` | YES |
| GET  | `/rest/v2/datasets/{id}/pipeline-status` | `api/workflow.ts:76` | YES (gemma-rest: `/pipelineStatus`, **camelCase mismatch**) |
| GET  | `/rest/v2/datasets/pipeline-status` (batch) | `api/workflow.ts:91` | NO |
| GET  | `/rest/v2/datasets/{id}/geeq` | `api/workflow.ts:210` | YES |
| POST | `/rest/v2/datasets/{id}/geeq/recalculate` | `api/workflow.ts:153` | NO (gemma-rest: `PUT /datasets/{id}/geeq` - **verb + path mismatch**) |
| GET  | `/rest/v2/datasets/{id}/analyses/differential` | `api/workflow.ts:166` | YES |
| POST | `/rest/v2/datasets/{id}/analyses/differential/{aid}/redo` | `api/workflow.ts:180` | NO (gemma-rest: `POST /datasets/{id}/tasks/redo/{aid}`) |
| DELETE | `/rest/v2/datasets/{id}/analyses/differential/{aid}` | `api/workflow.ts:194` | YES (gemma-rest: `DELETE /datasets/{id}/tasks/differential/{aid}` - **path mismatch**) |
| POST | `/rest/v2/datasets/{id}/{taskPath}` (generic) | `api/workflow.ts:130` | varies - typically maps to `/tasks/*` |
| POST | `/rest/v2/datasets/{id}/samples/{sid}/outlier` | `api/workflow.ts:240` | NO |
| POST | `/rest/v2/datasets/{id}/quantitationTypes/{qid}` | `api/workflow.ts:254` | NO (gemma-rest has `GET /datasets/{id}/quantitationTypes` but no per-QT mutation) |
| GET  | `/rest/v2/datasets/{id}/quantitationTypes` | `api/quantitation.ts:37` | YES |
| POST | `/rest/v2/datasets/{id}/makePublic` | `api/workflow.ts:272` | NO |
| POST | `/rest/v2/datasets/{id}/makePrivate` | `api/workflow.ts:272` | NO |
| GET  | `/rest/v2/tasks/{taskId}` | `api/workflow.ts:222` | YES |
| GET  | `/rest/v2/groups` | `api/workflow.ts:293` | NO |
| GET  | `/rest/v2/groups/{gid}` | `api/workflow.ts:307` | NO |
| POST | `/rest/v2/groups` | `api/workflow.ts:344` | NO |
| PATCH | `/rest/v2/groups/{gid}` | `api/workflow.ts:355` | NO |
| DELETE | `/rest/v2/groups/{gid}` | `api/workflow.ts:367` | NO |
| POST | `/rest/v2/groups/{gid}/members` | `api/workflow.ts:378` | NO |
| DELETE | `/rest/v2/groups/{gid}/members/{mid}` | `api/workflow.ts:392` | NO |
| GET  | `/rest/v2/datasets/{id}/groups` | `api/workflow.ts:334` | NO |
| GET  | `/rest/v2/candidates` | `api/workflow.ts:419` | NO (curation pipeline construct, only on the mock) |
| GET  | `/rest/v2/candidates/{cid}` | `api/workflow.ts:427` | NO |
| POST | `/rest/v2/candidates` | `api/workflow.ts:436` | NO |
| POST | `/rest/v2/candidates/bulk` | `api/workflow.ts:447` | NO |
| PATCH | `/rest/v2/candidates/{cid}` | `api/workflow.ts:458` | NO |
| DELETE | `/rest/v2/candidates/{cid}` | `api/workflow.ts:470` | NO |
| GET  | `/rest/v2/datasets/{id}/curation-proposals` | `api/proposals.ts:26` | NO |
| GET  | `/rest/v2/curation-proposals` | `api/proposals.ts:46` | NO |
| GET  | `/rest/v2/curation-proposals/{pid}` | `api/proposals.ts:54` | NO |
| PATCH | `/rest/v2/curation-proposals/{pid}` | `api/proposals.ts:181` | NO |
| GET  | `/rest/v2/datasets/{id}/audits` | `api/audits.ts:48,184` | NO |
| GET  | `/rest/v2/audits` | `api/audits.ts:60` | NO |
| GET  | `/rest/v2/audits/{aid}` | `api/audits.ts:71` | NO |
| PATCH | `/rest/v2/audits/{aid}` | `api/audits.ts:93` | NO |
| POST | `/rest/v2/audits/{aid}/finalize` | `api/audits.ts:136` | NO |
| POST | `/rest/v2/audits/{aid}/reopen` | `api/audits.ts:162` | NO |
| GET  | `/rest/v2/categories` | `api/categories.ts:22` | partial (gemma-rest: `/annotations/categories` + `/datasets/categories`) |
| GET  | `/rest/v2/annotations/search` | `api/annotations.ts:42` | YES |

Curation app distinct path templates: **~52**.

Cross-app unique total: ~62 distinct path templates (15 overlap, all
on `/datasets`-prefixed shapes).

---

## 3. gemma-rest endpoints actually defined

Spring Jersey JAX-RS resources under
`gemma-rest/src/main/java/ubic/gemma/rest/`. Base path is
`/rest/v2/` (confirmed via `OpenApiConfig.java:33`).

| Resource | @Path root | Endpoint count (verbs) |
|---|---|---|
| `RootWebService.java` | `/` | 3 GET (`/`, `/users/me`, `/users/{username}`) |
| `DatasetsWebService.java` | `/datasets` | 62 (mix of GET/POST/PUT/DELETE - see below) |
| `PlatformsWebService.java` | `/platforms` | 9 GET |
| `GeneWebService.java` | `/genes` | 6 GET |
| `TaxaWebService.java` | `/taxa` | 8 GET |
| `AnnotationsWebService.java` | `/annotations` | 10 GET |
| `AnalysisResultSetsWebService.java` | `/resultSets` | 3 GET |
| `TasksWebService.java` | `/tasks` | 1 GET (`/{taskId}`) |
| `SearchWebService.java` | `/search` | 1 GET |

Total ~103 endpoints across 9 resource classes.

Verb spread (across all 99 verb annotations in `gemma-rest/src/main`):
the bulk is GET; mutating endpoints are concentrated in
`DatasetsWebService` (curation details update, geeq update, tasks
trigger, design upload, etc.).

DatasetsWebService highlights relevant to the UI:

- `GET /datasets` (paginated)
- `GET /datasets/categories`, `/datasets/annotations`,
  `/datasets/platforms`, `/datasets/taxa` (faceted)
- `GET /datasets/{id}` and sub-resources: `/platforms`, `/samples`,
  `/publications`, `/auditEvents`, `/curationDetails`,
  `/permissions`, `/pipelineStatus`, `/geeq`, `/annotations`,
  `/quantitationTypes`, `/singleCellDimension`,
  `/cellTypeAssignment`, `/cellLevelCharacteristics`, `/data`,
  `/data/processed`, `/data/raw`, `/data/singleCell`, `/design`,
  `/hasbatch`, `/batchInformation`, `/svd`, `/subSetGroups`,
  `/subSets`
- `PUT /datasets/{id}/curationDetails`, `/permissions`, `/geeq`,
  `/design`
- `POST /datasets/{id}/tasks/preprocess`, `/tasks/diagnostics`,
  `/tasks/batchInfo`, `/tasks/differential`,
  `/tasks/redo/{analysisId}`
- `DELETE /datasets/{id}/tasks/differential/{analysisId}`
- Bulk expression endpoints under `/datasets/.../expressions/*`

---

## 4. Gaps (UI references that gemma-rest doesn't provide)

### 4a. Browser app gaps

**Zero gaps.** All 14 browser endpoints map to existing gemma-rest
paths. (The curator-only `/users/me` is GROUP_USER protected; see
auth section.)

### 4b. Curation app gaps

These are intentional: the curation app's `/rest/v2/...` namespace
is the **FastAPI mock** in gemma-curation-agents. The fact that
gemma-rest doesn't implement them is by design - they will never be
on the Java side unless the integration plan changes. Listed here so
the standalone migration knows not to chase them.

**Auth/session (not gemma-rest's job):**
- `GET /rest/v2/me` - gemma-rest exposes `GET /users/me`; UI path is
  mock-only.
- `POST /rest/v2/login`, `POST /rest/v2/logout` - mock-only; real
  Gemma uses Spring's form-login + HTTP Basic on the REST side.

**Curation pipeline (mock-only, lives in gemma-curation-agents):**
- `/rest/v2/candidates/*` (CRUD + bulk) - candidates pipeline
- `/rest/v2/curation-proposals/*` (CRUD) - proposer output
- `/rest/v2/audits/*` (`/finalize`, `/reopen`, PATCH disposition) -
  audit pipeline
- `/rest/v2/datasets/{id}/audits` - per-dataset audit list
- `/rest/v2/datasets/{id}/curation-proposals` - per-dataset
  proposals

**Workflow extensions (need adding to gemma-rest if curation app
ever shifts to real backend):**
- `GET /rest/v2/datasets/pipeline-status` (batch) - mock has it for
  the queue view; gemma-rest only has the per-dataset variant.
- `POST /rest/v2/datasets/{id}/geeq/recalculate` - gemma-rest has
  `PUT /datasets/{id}/geeq` which writes user-supplied scores; the
  UI wants a server-side recompute trigger.
- `POST /rest/v2/datasets/{id}/samples/{sid}/outlier` - outlier
  flagging.
- `POST /rest/v2/datasets/{id}/quantitationTypes/{qid}` - mutate
  preferred / processed QT.
- `POST /rest/v2/datasets/{id}/makePublic`,
  `POST /rest/v2/datasets/{id}/makePrivate` - shortcut for visibility
  toggle; gemma-rest has `PUT /datasets/{id}/permissions` and
  `PUT /datasets/{id}/visibility` (latter on the mock only).
- `POST /rest/v2/datasets/{id}/publish` - publication / freeze flow.
- `POST /rest/v2/datasets/import` - GEO/SRA import trigger.

**Groups CRUD (UserGroup management surface, not on gemma-rest):**
- `GET/POST/PATCH/DELETE /rest/v2/groups[/{id}]`
- Member add/remove: `/rest/v2/groups/{id}/members[/{mid}]`
- `GET /rest/v2/datasets/{id}/groups` - groups with access to a
  dataset (this one IS needed cross-app eventually; see workflow.ts
  comment line 314).

**Verb / path mismatches** (UI path != gemma-rest path):
- UI: `GET/POST /rest/v2/datasets/{id}/design`; gemma-rest:
  `GET/PUT /datasets/{id}/design`. Verb mismatch on write; content
  type difference (UI sends JSON, gemma-rest expects TSV upload).
- UI: `pipeline-status` (kebab); gemma-rest: `pipelineStatus`
  (camel).
- UI: `/datasets/{id}/analyses/differential/{aid}` for DELETE;
  gemma-rest: `/datasets/{id}/tasks/differential/{aid}`.
- UI: `/datasets/{id}/analyses/differential/{aid}/redo` for POST;
  gemma-rest: `/datasets/{id}/tasks/redo/{aid}`.
- UI: `GET /rest/v2/categories`; gemma-rest exposes
  `/annotations/categories` and `/datasets/categories`.

**Top 5 gaps (load-bearing for any future "curation UI on real
gemma-rest" path):**

1. **Auth model gap**: `POST /rest/v2/login` + bearer-token session.
   Real Gemma is HTTP Basic + session cookie via form-login.
2. **Audits & proposals subsurface**: ~12 endpoints with no Java
   counterpart - candidate, audit, proposal lifecycle. Lives in the
   Python agent and is unlikely to migrate.
3. **Groups management**: full CRUD on user groups missing from
   gemma-rest. Real Gemma has it in `gemma-web`'s legacy MVC
   controllers (`manageGroups.html`), not REST.
4. **Batch pipeline status**: `GET /rest/v2/datasets/pipeline-status`
   (no `{id}`) for queue/dashboard views.
5. **Design write contract drift**: gemma-rest uses
   `PUT /datasets/{id}/design` with TSV; UI sends JSON via POST.
   Either gemma-rest adds a JSON write or UI conforms.

---

## 5. Dead surface (gemma-rest endpoints not consumed by the UI)

Endpoints exposed but unreferenced from either UI app. Most are still
load-bearing for non-UI consumers (R Gemma.R client, scripted users,
Python clients); the audit is just "the React UI doesn't touch
these." **Do not remove blindly** - check Gemma.R + the public
analytics pipeline first.

- All of `GeneWebService` (6 endpoints under `/genes`) - the
  React UI has no gene-detail page yet; Gemma.R uses these.
- All of `TaxaWebService` (8 endpoints under `/taxa`) - taxon
  facet on dataset filter goes through `/datasets/taxa` instead.
- All of `AnalysisResultSetsWebService` (3 endpoints under
  `/resultSets`) - the browser DatasetPage comments reference
  `/datasets/{id}/analyses/differential/resultSets/{rsId}/results`
  as a TODO but doesn't call it.
- `SearchWebService` `/search` - the browser uses
  `/annotations/search` only.
- Most `/annotations/**` paths beyond `/annotations/search` and
  `/annotations/categories` (taxon-scoped search,
  `/annotations/predicates`, parent/child traversal).
- Bulk expression endpoints:
  `/datasets/{ids}/expressions/genes/{genes}`,
  `/datasets/{ids}/expressions/taxa/...`,
  `/datasets/{ids}/expressions/pca`,
  `/datasets/{ids}/expressions/differential`.
- Refresh endpoints: `/datasets/platforms/refresh`,
  `/datasets/annotations/refresh`, `/datasets/{id}/refresh`,
  `/genes/probes/refresh`.
- `/datasets/blacklisted`, `/platforms/blacklisted` -
  admin-only.
- Single-cell sub-resources:
  `/datasets/{id}/singleCellDimension`, `/cellTypeAssignment`,
  `/cellLevelCharacteristics`, `/data/singleCell` - browser
  DatasetPage has only stub references.
- Sub-set group endpoints under `/datasets/{id}/subSetGroups`,
  `/subSets` - browser doesn't render.
- Task triggers: `/tasks/preprocess`, `/tasks/diagnostics`,
  `/tasks/batchInfo`, `/tasks/differential` (POST) - curation app
  uses a different verb/shape via the mock.

Headcount of "exposed but UI-unreferenced": roughly 60+ of the ~103
gemma-rest endpoints. The browser app touches 14 distinct shapes;
the curation app touches mostly mock-only paths.

---

## 6. Auth model

**gemma-rest (`applicationContext-security.xml`):**

```xml
<s:http pattern="/rest/v2/users/**" ...>
  <s:anonymous granted-authority="IS_AUTHENTICATED_ANONYMOUSLY"/>
  <s:http-basic entry-point-ref="restAuthEntryPoint"/>
  <s:intercept-url pattern="/rest/v2/users/**" access="GROUP_USER"/>
</s:http>
<s:http pattern="/rest/v2/**" ...>
  <s:anonymous granted-authority="IS_AUTHENTICATED_ANONYMOUSLY"/>
  <s:http-basic/>
</s:http>
```

Read-only REST is open + anonymous; `/users/**` requires HTTP Basic
+ GROUP_USER. Session cookie also works (form-login on /login.jsp
sets a session that the same filter chain picks up).

**Browser app (`apps/browser/src/api/client.ts`):**

```ts
fetch(path + q, {
  method: "GET",
  credentials: "include",   // <-- session cookie passes through
  headers: { Accept: "application/json", "X-Requested-With": "XMLHttpRequest" },
});
```

Cookie-based. Compatible with gemma-rest's HTTP Basic + anonymous
chain - anonymous reads work for everything except `/users/me`,
which requires the user to have logged in via the Spring form-login
page first (cookie then flows on every subsequent fetch).

**Curation app (`apps/curation/src/api/client.ts`):**

```ts
fetch(path, {
  method,
  headers: {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  },
});
```

Bearer-token. `bearerToken()` reads from `localStorage` under
`gemma-curation-session`, falls back to `VITE_GEMMA_CURATION_API_KEY`
build-time env. Login happens via `POST /rest/v2/login`, returns a
`LoginResponse` with a token the UI persists.

**Verdict:** the **browser app is compatible** with gemma-rest as
shipped (cookie auth, anonymous reads, GROUP_USER gates on
mutating + `/users/me`). The **curation app is NOT compatible** with
gemma-rest's auth chain - it expects an OAuth/JWT-style bearer +
`/login` exchange that gemma-rest does not provide, and which is
out of scope for the standalone gemma-rest migration because the
curation app's contract is with the FastAPI mock, not Java
gemma-rest.

If the curation app ever needs to talk to real gemma-rest endpoints
(e.g., for `/datasets`, `/datasets/{id}/curationDetails`,
`/datasets/{id}/auditEvents`), the gemma-rest side would need either:
- a Spring Security `BearerTokenAuthenticationFilter` (OAuth2
  resource-server style), OR
- the curation app accepts that it must use cookie + HTTP Basic for
  the gemma-rest passthrough endpoints, OR
- the FastAPI mock proxies those endpoints through itself, doing the
  Basic-auth translation server-side.

---

## 7. Recommendations for the gemma-rest standalone migration

1. **Don't try to absorb the curation-only `/rest/v2/...` surface.**
   The audit, proposal, candidate, group, and login paths live in
   the gemma-curation-agents FastAPI mock by design. The
   compatibility doc names this as a two-repo contract; trying to
   re-implement them in gemma-rest doubles the surface and breaks
   the agent-side pipeline.

2. **Browser app is the real consumer.** The 14 endpoints it
   actually hits are all standard gemma-rest paths and will keep
   working after the standalone split, **as long as** the
   `/rest/v2/**` mount point and Spring Security cookie / HTTP
   Basic chain survive the split.

3. **Auth chain preservation is the hard requirement.** When
   gemma-rest splits out of the WAR, the
   `applicationContext-security.xml` `/rest/v2/users/**` and
   `/rest/v2/**` HTTP elements must remain intact. The browser app's
   `credentials: "include"` only works if the session cookie domain
   matches; in a standalone deploy that means same hostname or
   correct `SameSite`/`Domain` cookie config. **Test the
   `/users/me` round-trip after the split** - a single broken cookie
   is the most likely regression.

4. **Path-naming mismatches the curation team has flagged are NOT
   gemma-rest's problem** (they're talking to the mock). But if the
   integration ever flips to "curation UI on real backend," gemma-rest
   would need to expose `/pipeline-status` aliases, JSON-body design
   POST, etc. - all low-priority for the standalone migration.

5. **Dead-surface cleanup is downstream of gemma-web retirement.**
   The 60+ unreferenced gemma-rest endpoints are mostly used by
   Gemma.R and scripted clients. The React UI not consuming them
   doesn't mean they're safe to remove. Defer this audit until after
   gemma-web is gone and you can sweep both real legacy callers and
   the Gemma.R surface together.

6. **Three-way coordination point:** the curation team's
   "backendGaps" list at `apps/browser/src/api/endpoints.ts:298-332`
   names four gemma-rest extensions they want (gene-symbol probe
   filter, gene info on bulk element list, probe sequence, BLAT /
   alignment data). These are real product asks against gemma-rest,
   not the mock. They're not blockers for the standalone migration
   but should be on the post-split roadmap.

7. **OpenAPI as the contract truth.** Both UI apps consume
   `/rest/v2/openapi.json` (browser pulls
   `FilterArgExpressionExperiment` allowedValues for the filter UI).
   The standalone migration must keep the OpenAPI generation
   pipeline working - regression here breaks the dataset browser's
   filter facets silently.

---

## Provenance

- Repos read: `~/Dev/gemma-curation-ui/` (working tree, no commit
  pin captured), `~/Dev/eclipseworkspace/Gemma/` @ `08e760bdaf`.
- Source-of-truth files for endpoint inventory:
  - `apps/browser/src/api/endpoints.ts`
  - `apps/curation/src/api/{session,workflow,datasets,design,curation,history,audits,proposals,annotations,categories,quantitation}.ts`
  - `gemma-rest/src/main/java/ubic/gemma/rest/*WebService.java`
  - `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml`
- Cross-repo doc consulted:
  `apps/curation/CROSS_REPO_COMPAT.md` (canonical for two-repo
  wire-contract design).
- Method: ripgrep for `rest/v2`, `@Path`, `@(GET|POST|PUT|DELETE|PATCH)`,
  then manual reconciliation of path templates.

Done 2026-05-18.
