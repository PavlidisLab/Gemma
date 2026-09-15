# gemma-web retirement plan

Recce date: 2026-05-18. Branch `worktree-gemma-web-retire` off
`phase2-acl-migrate` HEAD `08e760bdaf`. Static read only — no maven, no
edits to gemma-web source. Doc-only output.

Companions:

- `MIDDLE_TIER_AUDIT.md` (this repo, on `phase2-acl-migrate`) — §1 module
  inventory, §3.4 SecurityController, §4.2 REST has no standalone artifact.
- `GEMMA_REST_STANDALONE_ROADMAP.md` (commit `e634e0009e`, unmerged
  `worktree-gemma-rest-standalone-recce`) — the recce for packaging
  gemma-rest WAR-standalone.
- `GEMMA_REST_BOOTSTRAP_PHASE1.md` (commit `13501bf4f8`, unmerged
  `worktree-gemma-rest-bootstrap`) — what landed on the bootstrap branch
  (gemma-rest's own `web.xml` + `gemma-rest-war` build profile, opt-in).
- Project memory `project_gemma_web_replacement.md` — "walking dead",
  replaced by `gemma-curation-ui` (now `gemma-ui`, at
  `~/Dev/gemma-curation-ui/`).

---

## 1. Why retire

1. **Walking-dead status already declared.** Project memory:
   > gemma-web (the legacy Spring MVC + JSP + ExtJS frontend in this repo)
   > is going to be heavily redone via the React app at
   > `~/Dev/gemma-curation-ui`. ... Don't propose features or refactors
   > targeting gemma-web's controllers, JSP views, or static assets unless
   > the user explicitly asks.

2. **Replacement is real and active.** `~/Dev/gemma-curation-ui` is a
   live npm-workspaces repo with two React 18 + TypeScript + Vite +
   TanStack Query apps:
   - `apps/curation/` — curator workflow (talks to the agent service +
     the curation write-API).
   - `apps/browser/` — public-facing browse/search; the React port of
     the long-standing Vue 2 GemBrow.
   The curation app already speaks `/rest/v2/**` exclusively
   (`apps/curation/src/api/*.ts` — 17 files of `api.get/post/patch`
   calls all rooted at `/rest/v2/`).

3. **gemma-rest standalone is in flight.** The bootstrap branch
   (`worktree-gemma-rest-bootstrap`, commit `13501bf4f8`) has already
   landed:
   - `gemma-rest/src/main/webapp/WEB-INF/web.xml` (Servlet 6 / Jakarta
     EE 9, JSON-only, no JSPs, no static asset serving).
   - `gemma-rest-war` opt-in maven profile that flips packaging from
     jar to war without breaking the default gemma-web WAR build.
   - `RestSecurityConfig` Java config (`@EnableWebSecurity` +
     `SecurityFilterChain` bean) replacing the XML
     `<s:http pattern="/rest/v2/**">` from
     `gemma-web/.../applicationContext-security.xml`.
   Once that branch merges, gemma-rest can be deployed without
   gemma-web. That unblocks deleting gemma-web entirely.

4. **Maintenance tax is high.** Phase 2 Spring 6 already showed
   gemma-web breaks on every modernization step (DWR removed, Commons
   FileUpload dropped, CommonsMultipartFile lost, upload-progress monitor
   gone). 187 Java files, 29 kLOC, 79 JSPs, 284 JS files, 15 XML configs,
   ExtJS/jQuery/DWR/Sitemesh stack — nobody is reading this code, but
   every Spring/Jakarta upgrade still pays the bill to fix its compile +
   smoke tests.

---

## 2. Inventory of gemma-web's surface

Measured by `find` / `grep` on `worktree-gemma-web-retire` at baseline
`08e760bdaf`.

### Code

| metric | value |
|---|---|
| Java files (main) | 160 (`find gemma-web/src/main/java -name '*.java'`) |
| Java LOC (main) | 25,647 |
| Controllers (`@Controller` or `@RequestMapping`) | 52 files |
| `@RequestMapping` URL declarations — total unique strings | 77 |
| - of which `*.html` (legacy view endpoints) | 73 |
| - of which non-html (json-ish AJAX or REST-ish) | 29 (some controller-class-level prefixes like `/dedv`, `/ee`) |
| Controllers returning JSON via `@ResponseBody` | 11 files, 29 method-level uses |
| Controllers returning views (`ModelAndView` / forward / redirect) | 24 files |
| `@RestController` | 0 (none — every JSON endpoint is `@Controller`+`@ResponseBody`) |

### Views + static

| metric | value |
|---|---|
| JSPs | 79 (`gemma-web/src/main/webapp/pages/`) |
| HTML | 2 (`debug.html`, `WEB-INF/error.jsp` aside) |
| JavaScript files | 284 (ExtJS app + DWR shims + jQuery utils under `webapp/scripts/`) |
| XML configs under WEB-INF + resources | 15+ (`web.xml`, `gemma-servlet.xml`, `sitemesh.xml`, `decorators.xml`, `applicationContext-{security,serviceBeans,component-scan,metrics}.xml`, `Gemma.tld`, ...) |
| `web.xml` LOC | 308 |

### Spring Security + Spring contexts that live here

- `applicationContext-security.xml` — **the** Spring Security http chain
  (`<s:http pattern="/**">` form-login + remember-me + JSESSIONID
  concurrency control, AND `<s:http pattern="/rest/v2/**">` for the REST
  chain). Owns `httpAccessDecisionManager`, `restAuthEntryPoint`,
  `roleHierarchyVoter`, etc.
- `applicationContext-serviceBeans.xml` — gemma-web-owned facade beans
  (`Expression*HelperService` `@Transactional` wrappers, etc.).
- `applicationContext-component-scan.xml` — `<context:component-scan
  base-package="ubic.gemma.web"/>`.
- `applicationContext-metrics.xml` — gemma-web's Micrometer integration.

### Listeners + filters + servlet config

- Listeners: `IntrospectorCleanupListener`,
  `ubic.gemma.web.listener.StartupListener`,
  `ubic.gemma.web.listener.UserCounterListener`,
  `HttpSessionEventPublisher`.
- Filters: `springSecurityFilterChain`, `sitemesh`,
  `CorsFilter` (the class lives in gemma-rest but is *declared* here),
  `gemmaWebMetricsFilter`, `restapidocsFilter`.
- Context initializer: `ubic.gemma.web.context.InitializeContext`.
- Servlets: `gemma` (Spring `DispatcherServlet` for SSR controllers) +
  `gemma-rest` (Jersey, hosting gemma-rest's `@Path` classes).

### Build coupling

- Root `pom.xml` `<modules>`: `gemma-core`, `gemma-cli`, `gemma-rest`,
  `gemma-web`.
- Reverse deps on `gemma-web`: **none from `gemma-cli`, `gemma-core`,
  `gemma-rest`** (grep on their pom.xml). Only the root build aggregates
  gemma-web.
- `gemma-web` is the only `<packaging>war</packaging>` in the repo today.

---

## 3. Gap analysis — what gemma-web does that gemma-rest does NOT

Classify each gemma-web endpoint into one of four bins:

- **API-already-in-rest** — same JSON contract is reachable on
  `/rest/v2/**`. Just delete.
- **API-port-to-rest** — JSON / AJAX endpoint that GemBrow or
  gemma-curation-ui depends on, with no equivalent in gemma-rest yet.
  Must be ported before gemma-web dies.
- **HTML-replaced-by-ui** — server-side rendered page (`.jsp`). The
  React frontend re-implements client-side; the SSR endpoint just dies.
  Risk: external bookmarks / paper-cited URLs break.
- **Admin-or-internal** — only used by ops + the SSR admin pages. Two
  sub-options: port to a small `/rest/v2/admin/**` resource (preferred),
  or replace with `/actuator/*` once Spring Boot lands.

### 3.1 gemma-rest current coverage (`/rest/v2/**`)

9 JAX-RS `@Path` resource classes (`grep -rln '@Path\b' gemma-rest/src/main/java`):

| class | scope |
|---|---|
| `RootWebService` | `/rest/v2/` root, API version, search-index status |
| `DatasetsWebService` | `/rest/v2/datasets/**` (113 sub-paths; the kitchen-sink resource — read, write, audits, curation, single-cell, FV, design, samples, QC, sub-sets) |
| `PlatformsWebService` | `/rest/v2/platforms/**` (array designs) |
| `GeneWebService` | `/rest/v2/genes/**` |
| `TaxaWebService` | `/rest/v2/taxa/**` |
| `AnnotationsWebService` | `/rest/v2/annotations/**` (search + categories) |
| `SearchWebService` | `/rest/v2/search` (general cross-entity search) |
| `AnalysisResultSetsWebService` | `/rest/v2/resultSets/**` (differential expression) |
| `TasksWebService` | `/rest/v2/tasks/**` (long-running task status) |

This already covers the vast majority of what GemBrow / curation-ui
need: dataset list/detail/search, platform list/detail, gene list/detail,
taxon list, annotation search, free-text search, DE result sets, task
status, single-cell metadata, factor values, experimental design,
audits, curation details.

### 3.2 The gap list

For each item: source endpoint → consumer → destination.

| # | gemma-web endpoint | consumer | destination |
|---|---|---|---|
| G1 | `SecurityController` (gemma-web/.../auditAndSecurity/) — 49 `securityService.*` callsites; `/securityInfo`, `makePrivate`, `makeReadableByGroup`, `makeOwnedByUser`, group CRUD, ACL admin AJAX | SSR admin JSP (`pages/admin/userManager.jsp` etc.) + power-curator workflows | **Port to `/rest/v2/security/**`** (new resource in gemma-rest) OR confirm with Paul that the curation-ui replicates the admin surface and we're killing the AJAX endpoints. MIDDLE_TIER_AUDIT §3.4 already flags this. |
| G2 | `ExpressionExperimentQCController` — `/expressionExperiment/visualizeCorrMat.html`, `pcaScree.html`, `visualizeMeanVariance.html`, `visualizeSingleCellSparsityHeatmap.html`, ... (14 visualization sub-routes producing PNG/JSON) | GemBrow legacy pages; possibly the React browser app | Confirm whether the React browser app re-derives these client-side or hits server-rendered PNGs. If server-side, port to `/rest/v2/datasets/{id}/qc/{plot}` returning either `image/png` or the underlying matrix as JSON. |
| G3 | `DEDVController` (1,269 LOC) — `/dedv/**`, `/downloadDEDV.html?pca=` / `?rs=` / default. Expression vector visualization data. | GemBrow probe-detail pages | Likely partially covered by `DatasetsWebService` processed-vector endpoints; verify. The CSV download (`/downloadDEDV.html`) is the load-bearing case that may need a dedicated `/rest/v2/datasets/{id}/processedVectors/tsv` (probably already exists — confirm in `DatasetsWebService`). |
| G4 | `GeneController` AJAX + `GeneSetController`, `ExpressionExperimentSetController` — set-management AJAX | GemBrow set-builder | Largely covered (`GeneWebService`, dataset-set in `DatasetsWebService`). Diff: gemma-web supports *session-scoped* (un-persisted) sets stored in `HttpSession` via `web/controller/persistence/`. That doesn't move cleanly to a stateless REST API — design call. |
| G5 | `GeneralSearchController` (`/searcher/**`) | GemBrow search bar | Largely covered by `SearchWebService`. Diff: the legacy returns SitMesh-decorated JSP results; the new path returns JSON. The React browser app already speaks JSON. Just delete the legacy. |
| G6 | `SignupController`, `UserFormMultiActionController`, `UserListController`, `passwordHint.jsp`, `register.jsp`, `editUser.html`, `login.jsp` | Login + signup form-post flow | Port to `/rest/v2/users/**` (the `/users` URL pattern is already reserved in `applicationContext-security.xml:46` — `<s:intercept-url pattern="/rest/v2/users/**" access="GROUP_USER"/>` — but there's no `UsersWebService` yet). New resource needed. Login itself is currently form-post to `/j_spring_security_check` producing a JSESSIONID; the cookie/session model survives if Phase W keeps cookie auth (per standalone roadmap §7). |
| G7 | `FileUploadController` (file-upload + progress) | Curator upload UI | Already broken (per project memory: "deleted DWR, dropped Commons FileUpload, lost CommonsMultipartFile, upload-progress monitor gone"). curation-ui will reimplement against `/rest/v2/datasets/import` (which already exists per `apps/curation/src/api/datasets.ts:30`). Just delete. |
| G8 | `RssFeedController` (`/rssfeed/**`) | Any external RSS subscriber to "what's new in Gemma" | Decide: port to `/rest/v2/whatsnew/rss` OR retire. Almost certainly retire. |
| G9 | `OntologyController` — `/ont/**`, `/TGEMO**`, `/TGFVO**` — serves Gemma's own ontologies (TGEMO, TGFVO) as OWL files | Bioportal, external ontology consumers; deep-linked from publications | **MUST PRESERVE.** Either (a) port to a `/rest/v2/ontologies/{ns}` resource that serves OWL, (b) serve the OWL files statically from nginx in front of the new stack, or (c) keep an URL-rewrite alias from the old paths. **Load-bearing for citing publications**; do not delete without a 301 plan. |
| G10 | `HomePageController` (`/`, `/home.html`) → `home.jsp` | Anyone who lands at `https://gemma.msl.ubc.ca/` | Replaced by gemma-ui/browser at the same URL. Coordinate the nginx flip with the deployment of the React app. |
| G11 | `SystemMonitorController`, `SystemStatsController`, `MaintenanceModeController`, `IndexerController` (`reIndexOntologies.jsp`), `WhatsNewController` | Ops + admin staff | Port to `/rest/v2/admin/**` OR adopt Spring Boot Actuator (deferred to gemma-rest Phase 8/embedded-Tomcat). Until then, keep these reachable via a *minimal* admin shim. |
| G12 | `JavascriptLogger` — `/log.html` POST that takes a client-side JS error and writes it to the server log | GemBrow's window.onerror handler | Port to `/rest/v2/clientLog` or just kill (the React app uses Sentry-equivalent). |
| G13 | `TaskCompletionController`, `ProgressStatusController` | Long-running job polling from GemBrow | Already covered by `TasksWebService` at `/rest/v2/tasks/**`. Delete. |
| G14 | `ArrayDesignFormController`, `ArrayDesignProbeMapperController`, `ArrayDesignRepeatScanController` admin actions | Ops admin (probe-mapping reruns) | Port to `/rest/v2/platforms/{id}/probeMapping` / similar, OR rely on gemma-cli (these are batchy operations already mirrored as CLI commands). |
| G15 | `PreprocessController`, `SvdController`, `TwoChannelMissingValueController`, `BatchInfoFetchController` | Ops admin (kick off preprocessing) | Same as G14 — port to `/rest/v2/datasets/{id}/preprocess` or rely on gemma-cli. |
| G16 | `CharacteristicBrowserController` (`/characteristicBrowser.html`) — admin tag-cleanup browser | Curator | Covered by `AnnotationsWebService` + the curation-ui annotations editor; delete. |
| G17 | `BibliographicReferenceController`, `PubMedQueryController` | Curator bib-ref UI | Verify coverage in `DatasetsWebService` (`/rest/v2/datasets/{id}/bibRef` is the likely shape). Port any gap. |
| G18 | `BioAssayController`, `BioMaterialController`, `CompositeSequenceController`, `GenePickerController` — detail-page AJAX | GemBrow | All covered by `/rest/v2/datasets/{id}/samples`, `/rest/v2/platforms/{id}/elements`, `/rest/v2/genes/**`. Delete. |
| G19 | `AuditController` (audit-event browser) | Curator | Already covered by `apps/curation/src/api/audits.ts` against `/rest/v2/datasets/{id}/audits`. Delete. |
| G20 | `ExperimentalDesignController` (1,011 LOC) — design upload + edit + factor-value AJAX | Curator | Covered by the curation app + `DatasetsWebService` design endpoints. Delete the form-driven SSR; verify any AJAX-only methods are dual-covered. |

**Top-5 highest-risk gaps (the ones that block deletion):**

1. **G1 — `SecurityController`** (ACL admin surface). Unique
   functionality, no REST equivalent. Must port or formally decide to drop.
2. **G9 — `OntologyController`** (TGEMO/TGFVO OWL serving). External
   consumers; preserve URL with 301 or serve from a new resource.
3. **G6 — User CRUD + login** (`SignupController`, password reset,
   `/login.jsp`). `/rest/v2/users/**` URL pattern is reserved but the
   resource doesn't exist yet.
4. **G2 — `ExpressionExperimentQCController`** (QC plots). Verify the
   React browser app coverage; if it relies on the server-rendered PNGs,
   port to a `/rest/v2/datasets/{id}/qc/**` resource.
5. **G11 — Admin/ops endpoints** (`SystemStats`, `MaintenanceMode`,
   indexer). Decide: small REST admin resource vs Actuator vs keep an
   ops-only shim. Without these, ops can't drain the running site for a
   deploy.

### 3.3 What gemma-curation-agents consumes

`/Users/pzoot/Dev/gemma-curation-agents` is the Python agent service. It
talks to the same `/rest/v2/**` REST API the curation-ui talks to (per
`apps/curation/CLAUDE.md` "wire shapes" — Python is canonical). It does
**not** depend on any gemma-web SSR endpoint. Safe to delete gemma-web
from the agent service's perspective.

---

## 4. Migration path

Each phase is sized so it can be reviewed + committed in one slice. They
are deliberately *sequential* — do not start phase W.N+1 until W.N is
green in prod.

### Phase W.1 — gap enumeration + destination decisions (recce-only, this doc)

**Deliverable: this doc, reviewed by Paul.** Outcome is a per-row
decision on G1–G20: port / drop / 301-alias.

### Phase W.2 — port the still-needed endpoints

For each gap with a "port" disposition, add the equivalent
`/rest/v2/**` resource. Sequence by risk:

- **W.2a — `UsersWebService`** (G6). Already-reserved URL pattern; add a
  JAX-RS resource. Pulls in login (`/rest/v2/users/login` form-post or
  basic-auth-driven JSESSIONID) + signup + password reset.
- **W.2b — `SecurityWebService`** (G1). Port the 49 `securityService.*`
  callsites. MIDDLE_TIER_AUDIT §3.4 already estimated this as a
  high-leverage slice. **Big — likely 2 sessions.**
- **W.2c — `OntologyWebService`** (G9). Serve TGEMO/TGFVO OWL at
  `/rest/v2/ontologies/{ns}.owl`. Plus an nginx alias rule from
  `/ont/TGEMO.OWL` → new URL (preserves citation links).
- **W.2d — `AdminWebService`** (G11). Minimum viable:
  `/rest/v2/admin/maintenanceMode`, `/rest/v2/admin/systemStats`,
  `/rest/v2/admin/reindex` — gated `@Secured("GROUP_ADMIN")`.
- **W.2e — Verify QC coverage** (G2) + close any verified gap. Likely
  the qc plot endpoints just need a dedicated `/rest/v2/datasets/{id}/qc/**`
  family if the React browser app doesn't reproduce them client-side.
- **W.2f — Delete-without-replacement** (G7, G8, G12, G13, G16, G17, G18,
  G19, G20). These either are already covered or are explicitly OK to
  drop. Per-gap one-liner stub in the rest API if needed for back-compat.

### Phase W.3 — stop deploying gemma-web; route traffic to gemma-rest + gemma-ui

Prereq: `worktree-gemma-rest-bootstrap` merged + standalone-roadmap phases
4–5 (WAR build default + CI smoke) green.

1. Deploy `gemma-rest.war` to the production Tomcat 10.1 install at
   `/rest/v2/**`. Keep gemma-web running at `/` in parallel for one
   sprint as fallback.
2. Deploy `gemma-ui` (curation + browser) to the production web root
   (nginx + static React bundle).
3. Cut nginx routing: `/` → curation-ui (or browser, depending on
   auth state), `/rest/v2/**` → gemma-rest standalone, `/ont/**` → new
   `/rest/v2/ontologies/**` with a 301.
4. Smoke production for one sprint. Watch error rates, watch the
   "what GemBrow URL just 404'd" log.

### Phase W.4 — delete gemma-web module

Once W.3 is stable for one sprint:

1. Remove `<module>gemma-web</module>` from root `pom.xml`.
2. `git rm -r gemma-web/`.
3. Remove gemma-web-specific artifacts elsewhere:
   - `applicationContext-security.xml`'s `<s:http pattern="/**">` block
     (gemma-rest's `RestSecurityConfig` already replaces the `/rest/v2/**`
     half).
   - The five XML files in `gemma-web/src/main/resources/ubic/gemma/`.
   - Anything in `gemma-core` `applicationContext-*.xml` that was scoped
     to gemma-web only.
4. Verify CI: `mvn verify -DskipWebpack=true` should pass with three
   modules (`gemma-core`, `gemma-cli`, `gemma-rest`).
5. Move the gemma-rest-war profile to the default packaging:
   `<packaging>war</packaging>` outright, drop the profile.
6. Remove the `gemma-rest`-as-jar dependency wiring from anywhere
   downstream (likely just CI / deployment scripts).

---

## 5. Risks

1. **Broken external bookmarks / paper-cited URLs.** Mitigate via nginx
   301-rewrite rules for the top-traffic legacy paths
   (`/home.html`, `/expressionExperiment/showAllExpressionExperiments.html`,
   `/gene/showGene.html`, `/ont/TGEMO.OWL`). Pull the access log from prod,
   list the top 20 `*.html` URLs, write rewrite rules for each.
2. **TGEMO/TGFVO OWL URLs cited in published papers.** Cannot delete
   blindly. **Hard blocker** for phase W.4; phase W.2c must land first.
3. **Session-scoped gene/EE sets** (`web/controller/persistence/`) —
   GemBrow lets users build an un-persisted set in their browser session
   and use it across pages. The React app needs a story (client-side
   state, or a `/rest/v2/sets/scratch/**` flow).
4. **Login flow change.** Today: form-post to `/j_spring_security_check`
   producing JSESSIONID. Tomorrow: TBD per standalone-roadmap §7. If we
   keep cookie/session (recommended for W.3), the curation-ui works
   unchanged. If we move to token-bearer, that's a coordinated cut.
5. **The two manual `accessDecisionManager.decide(...)` callsites in
   gemma-rest** (`DatasetsWebService:3082`, `PlatformsWebService:318`).
   They resolve `httpAccessDecisionManager` by name; that bean is
   defined in `gemma-web/.../applicationContext-security.xml:9–18` today.
   Per standalone-roadmap §9 #6, the bean must move to gemma-rest or
   gemma-core before W.4 (gemma-web XML deletion). The standalone bootstrap
   branch may not have moved this yet — verify.
6. **`StartupListener` + `UserCounterListener`** (gemma-web-specific
   listeners). Anything load-bearing in them (e.g. startup health logs,
   scheduled-task kickoff)? **Need to read both before W.4.** Per the
   standalone roadmap §6 item 7, "active-profile injection" needs porting;
   anything else they do is currently unclear.
7. **`InitializeContext` port not yet landed.** Standalone bootstrap §
   "What is NOT verified yet" — the gemma-rest WAR boots without
   active-profile injection. If gemma-core beans depend on the `web`
   profile being active, they'll silently mis-wire in the standalone
   gemma-rest. **Blocks W.3.**
8. **`gemma-rest` smoke-test gap.** The bootstrap branch's WAR has never
   been deployed to a real Tomcat. The first deploy will surface gaps
   (CORS property resolution, missing listeners, restapidocs static
   assets). Estimate one extra session for the inevitable "real Tomcat
   boot fix-up".
9. **`SidValueObject` cleanup** (MIDDLE_TIER_AUDIT §2, §4 #1). Imports
   `gemma.gsec.acl.domain.{AclPrincipalSid, AclGrantedAuthoritySid}` —
   the Phase 2 holdouts. Lives in gemma-web. Either fix in place
   pre-deletion (5-line edit) or just delete it with gemma-web.

---

## 6. Estimated effort

In agent-session units (one session ≈ one workday of focused work).

| phase | scope | sessions |
|---|---|---:|
| W.1 | This doc + Paul's per-row decisions on G1–G20 | 0.5 |
| W.2a | `UsersWebService` (login + signup + password reset) | 1.5 |
| W.2b | `SecurityWebService` (49 securityService.* callsites; ACL admin surface) | 2 |
| W.2c | `OntologyWebService` + nginx 301 for TGEMO/TGFVO | 1 |
| W.2d | `AdminWebService` (maintenance mode + system stats + reindex) | 1 |
| W.2e | Verify + close any QC plot gap | 0.5–2 (depends on how much the React browser re-derives client-side) |
| W.2f | Verify the "delete without replacement" list — one smoke pass per gap | 0.5 |
| pre-W.3 | Land `worktree-gemma-rest-bootstrap` (still unmerged); land standalone-roadmap phases 2–5 (delete XML chain, port `InitializeContext`, move `restapidocs`, flip default packaging to war, CI smoke) | ~4 (already estimated in standalone-roadmap §8) |
| W.3 | Deploy WAR + UI to prod, nginx flip, one-sprint dual-running | 1 (ops-only) |
| W.4 | Delete gemma-web module + cleanup downstream | 1 |
| **total** | **W.1 → W.4 inclusive of pre-W.3 dependencies** | **~13** |

If we *skip* W.2b (SecurityController port — accept that ACL admin moves
to the CLI or just disappears from the UI), drop ~2. If the React
browser app fully re-derives QC plots client-side, drop ~1.5.

Most-likely range: **10–13 agent-sessions to gemma-web fully deleted
from main.**

---

## 7. Open questions for Paul

1. **G1 — `SecurityController` (ACL admin)**: port to
   `/rest/v2/security/**` and rebuild a thin admin UI in
   gemma-ui/curation, OR accept that fine-grained ACL admin moves to
   gemma-cli + DB-only operations?
2. **G9 — TGEMO/TGFVO OWL URL preservation**: which exact URLs are
   cited in published papers? (Confirms the 301 rewrite list before
   anything moves.)
3. **G2 — QC plots**: does the React browser app re-derive them
   client-side from raw vectors, or expect server-side PNG/JSON? Affects
   whether W.2e is 0.5 sessions or 2.
4. **Auth model for the cutover**: cookie/session (status quo,
   simplest) or token-bearer (cleaner, larger change)? Standalone-roadmap
   §7 recommends defer to post-W.3.
5. **Session-scoped sets** (G4 / `web/controller/persistence/`): does
   the curation-ui need a "scratch set" concept, or is that a
   GemBrow-era thing that just dies?
6. **The legacy URLs gemma-cli or external scripts might still hit**:
   any cron job, monitoring probe, or external integration on
   `*.gemma.msl.ubc.ca/something.html`? Pull `nginx access.log` for
   one week before W.4.
7. **`UserCounterListener` + `StartupListener`**: do these do anything
   load-bearing beyond active-profile injection? (Needs a read pass
   before W.4.)
8. **CI / build profile**: should gemma-rest become `<packaging>war</packaging>`
   *outright* at W.4 (dropping the `gemma-rest-war` opt-in profile), or
   stay opt-in for one more cycle? Standalone-bootstrap left the profile
   in to avoid breaking gemma-web's build; once gemma-web is gone the
   profile is redundant.

---

## 8. Status

Recce only. No code changes. No maven runs. Doc committed on
`worktree-gemma-web-retire`.

Next action: Paul reviews this doc + decides G1–G20 dispositions + the
open questions in §7. Once those are answered, Phase W.2 can start in
parallel with the pre-W.3 gemma-rest standalone work (they don't conflict
— W.2 adds new resources to gemma-rest; pre-W.3 finishes the
gemma-rest WAR cutover).
