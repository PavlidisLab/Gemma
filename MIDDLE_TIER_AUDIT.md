# Middle-tier audit — gemma-cli, gemma-rest, gemma-web

Audit date: 2026-05-18 (branch `phase2-acl-migrate`). Static read only — no
tests, no maven, no edits beyond this file.

The deep layer (gemma-core persistence + gsec ACL) is now solid (Hibernate-event
ACL maintenance, unified Spring Sids, 23/23 ACL ITs, 63 gsec unit tests). This
report characterizes what is built on top, where modernization next-bites, and
what to do first.

---

## 1. Module inventory

### gemma-cli — batch / loader CLIs

| metric | value |
|---|---|
| Java files (main) | 201 |
| LOC (main) | 26,279 |
| `apps/` CLI classes | 105 |
| Test files | 28 |
| XML config files | 1 (`applicationContext-component-scan.xml`) |

- **Entry point**: `gemma-cli/src/main/java/ubic/gemma/cli/main/GemmaCLI.java`
  (481 LOC) — single `main()` that resolves a shorthand command name, builds a
  Spring context via `SpringContextUtils`, instantiates the named CLI bean,
  and runs it.
- **Bootstrap**: classpath-scanned `applicationContext-*.xml` plus
  `ubic.gemma.cli.config` Java configs. No web container; pure Spring +
  Apache Commons CLI + picocli.
- **Package shape**:
  - `ubic.gemma.cli.main` — `GemmaCLI` dispatcher.
  - `ubic.gemma.cli.util` — `AbstractCLI` (629 LOC), `EntityLocatorImpl`
    (443 LOC), `OptionsUtils` (420 LOC); shared infra for every CLI.
  - `ubic.gemma.cli.batch` — `BatchTaskExecutorService` +
    `BatchTaskProgressReporter` (the only ThreadLocal users in the module).
  - `ubic.gemma.cli.authentication`, `cli.logging`, `cli.metrics`,
    `cli.completion`, `cli.options`.
  - `ubic.gemma.apps` — 105 CLI command classes. Big ones:
    `DifferentialExpressionAnalysisCli` (860 LOC),
    `ExpressionExperimentManipulatingCLI` (843 LOC),
    `SingleCellDataLoaderCli` (698 LOC),
    `ArrayDesignProbeMapperCli` (684 LOC),
    `GeoSingleCellDataDownloaderCli` (681 LOC),
    `GeoGrabberCli` (654 LOC).

### gemma-rest — JAX-RS API surface (the modern path)

| metric | value |
|---|---|
| Java files (main) | 159 |
| LOC (main) | 16,578 |
| Test files | 27 (4,206 LOC) |
| XML config | `applicationContext-component-scan.xml`, `applicationContext-analytics.xml` |

- **Entry point**: 11 `*WebService.java` JAX-RS resources at
  `gemma-rest/src/main/java/ubic/gemma/rest/`. Jersey-served via the
  `gemma-rest` servlet declared in `gemma-web/src/main/webapp/WEB-INF/web.xml`
  lines 176–198 (`org.glassfish.jersey.servlet.ServletContainer`,
  `jersey.config.server.provider.packages=…,ubic.gemma.rest`).
- **Bootstrap**: lives **inside the gemma-web WAR**. No standalone deployment
  artifact. Spring discovery via `applicationContext-*.xml` glob from
  `web.xml` line 28–31.
- **Major packages**: `rest/` (resources), `rest/util/args/` (the Jersey
  arg-parsing layer — `DatasetArgService`, `FilterArg`, `SortArg`), `rest/swagger/`,
  `rest/providers/`, `rest/serializers/`, `rest/security/`, `rest/servlet/`
  (filters), `rest/analytics/ga4/`, `rest/annotations/`.
- **Largest by far**: `DatasetsWebService.java` (3,102 LOC, 113 `@Path` sub-paths,
  28 `expressionExperimentService.*` callsites) — the kitchen-sink endpoint.
  Distant second: `AnnotationsWebService.java` (597 LOC).

### gemma-web — Spring MVC SSR app (legacy)

Per project memory, being replaced by gemma-curation-ui. Treat as walking dead;
do not chase test fixes here.

| metric | value |
|---|---|
| Java files (main) | 187 |
| LOC (main) | 29,126 |
| Controllers | 115 (in `web/controller/...`) |
| JSPs | 79 |
| XML config | 15 (incl. `applicationContext-{security,serviceBeans,component-scan,metrics}.xml`, `web.xml`, `gemma-servlet.xml`, `sitemesh.xml`, `decorators.xml`) |
| Test files | 27 (3,479 LOC) |

- **Entry point**: `gemma-web/src/main/webapp/WEB-INF/web.xml`. Two servlets
  share the WAR: `gemma` (Spring `DispatcherServlet` for SSR controllers) and
  `gemma-rest` (Jersey for the API). Filters: `springSecurityFilterChain`,
  `sitemesh`, `CorsFilter`, `gemmaWebMetricsFilter`, `restapidocsFilter`.
- **Listeners**: `StartupListener`, `UserCounterListener`,
  `HttpSessionEventPublisher`, `IntrospectorCleanupListener`. Context init
  hook: `ubic.gemma.web.context.InitializeContext`.
- **Bootstrap**: XML-heavy. `applicationContext-security.xml` is the live
  Spring Security config (`<s:http>` chains, role hierarchy voter, form-login,
  remember-me, JSESSIONID concurrency control) — **not yet
  `@EnableMethodSecurity`-equivalent at the HTTP layer**.
- **Major packages**:
  - `web/controller/expression/experiment/` — the heaviest cluster:
    `ExpressionExperimentController` (1,777 LOC),
    `ExpressionExperimentQCController` (1,573 LOC),
    `ExpressionExperimentEditController` (1,045 LOC),
    `ExperimentalDesignController` (1,011 LOC),
    `DEDVController` (1,269 LOC), `ExpressionExperimentSetController`
    (480 LOC), `ExpressionExperimentDataFetchController` (434 LOC).
  - `web/controller/common/auditAndSecurity/` — `SecurityController` (612 LOC)
    is the live ACL admin surface.
  - `web/controller/analysis/`, `web/controller/genome/`,
    `web/controller/search/`, `web/controller/visualization/`,
    `web/controller/persistence/` (session-bound EE sets),
    `web/controller/job/` (long-running task status), `web/controller/monitoring/`.
  - `web/service/` — *thin* helper services that exist solely to host
    `@Transactional` boundaries the controllers need (see §4).
  - `web/taglib/`, `web/listener/`, `web/metrics/`.

---

## 2. Deep-layer consumption patterns

### Top gemma-core / persistence imports per module

CLI (134 unique `ubic.gemma.core.*` import paths; 208 `@Autowired`):

| count | service / type |
|---|---|
| 12 | `ExpressionExperimentService` (persistence) |
| 12 | `core.analysis.service.ExpressionDataFileService` |
| 11 | `SingleCellExpressionExperimentService` |
| 10 | `ArrayDesignService` |
| 9 | `core.util.locking.LockedPath` |
| 8 | `TaxonService` |
| 8 | `core.util.TsvUtils` |
| 7 | `ExternalDatabaseService` |
| 5 | `ProtocolService`, `AuditTrailService`, `core.util.locking.FileLockManager`, `ExpressionDataFileUtils`, `core.util.GemmaRestApiClient`, `SimpleRetryPolicy` |

REST (41 unique `core` imports; 78 `@Autowired`):

| count | service / type |
|---|---|
| 11 | `ExpressionExperimentService` |
| 10 | `core.util.BuildInfo` |
| 10 | `GeneService` |
| 9 | `CompositeSequenceService` |
| 9 | `ArrayDesignService` |
| 8 | `TaxonService` |
| 6 | `persistence.service.FilteringService` (the new typed-query layer) |
| 5 | `QuantitationTypeService`, `DatabaseEntryService` |
| 4 | `ExpressionAnalysisResultSetService` |
| 3 | `FactorValueService`, `core.search.*` |

Web (101 unique `core` imports; 265 `@Autowired`):

| count | service / type |
|---|---|
| 25 | `ExpressionExperimentService` |
| 18 | `core.job.TaskRunningService` |
| 9 | `ArrayDesignService` |
| 9 | `core.analysis.report.ExpressionExperimentReportService` |
| 8 | `BuildInfo`, `core.search.SearchException` |
| 6 | `TaxonService`, `CompositeSequenceService`, `core.security.authentication.UserManager` |
| 5 | `GeneSetService`, `GeneService`, `ExpressionExperimentSetService`, `core.job.TaskResult` |

`ExpressionExperimentService` is the single most-loaded service in every module.
Aggregate hot methods (CLI+REST+Web combined):

| count | EES method |
|---|---|
| 25 | `load` (by id) |
| 24 | `loadAndThawLiteOrFail` |
| 20 | `loadOrFail` |
| 9 | `thawLite` (called as a follow-up to plain `load`) |
| 5 | `update`, `getSubSetsWithBioAssays`, `getProcessedQuantitationType`, `getBioAssayDimension`, `findByShortName` |

### gsec types that reach the middle tier

| module | imports of `gemma.gsec.*` |
|---|---|
| CLI | 2× `SecurityService`, 2× `ManualAuthenticationService` |
| REST | 1× `SecurityService`, 1× `SecurityUtil` |
| Web | 8× `SecurityUtil`, 4× `SecurityService`, 4× `UserDetailsImpl`, 1× each of `AuthorityConstants`, `LoginDetailsValueObject`, **`AclPrincipalSid`**, **`AclGrantedAuthoritySid`** |

The last two — `gemma.gsec.acl.domain.AclPrincipalSid` and
`AclGrantedAuthoritySid` — are the holdovers Phase 3 should already have
removed. They live in exactly one file:
`gemma-web/src/main/java/ubic/gemma/web/controller/common/auditAndSecurity/SidValueObject.java`
lines 21–22, 44, 108–117. The class does pattern-matching on Sid type to render
JSON for the SSR admin UI; swapping to Spring's stock `PrincipalSid` /
`GrantedAuthoritySid` is a 5-line edit. **Flagged for cleanup.**

### `SecurityContextHolder`, `Authentication`, ACL voter touchpoints

- CLI: 7 SecurityContextHolder refs (mostly `ManualAuthenticationService` /
  `BatchTaskProgressReporter`); **no `@Secured`, no `@PreAuthorize`**.
- REST: 9 SecurityContextHolder refs. Notably
  `DatasetsWebService.java:3071` and `PlatformsWebService.java:308` invoke
  `accessDecisionManager.decide(...)` directly with a synthesized
  `SecurityConfig("GROUP_ADMIN")` — manual authorization instead of an
  annotation.
- REST has **19 `@Secured("GROUP_ADMIN")`** (17 in `DatasetsWebService`, 1 in
  `TasksWebService`, 1 in `PlatformsWebService`) plus **2 `@PreAuthorize`** on
  `RootWebService` (`isAuthenticated()` and a principal-or-admin check).
- Web has **0 `@Secured` / `@PreAuthorize`** in controllers — authorization is
  entirely URL-pattern-driven (`<s:intercept-url>` in
  `applicationContext-security.xml`) and method-level enforcement is delegated
  to the gemma-core services it calls. Plus 49 direct `securityService.*`
  calls, 47 of which are in `SecurityController` (the ACL admin UI).
- **`AclService` / `MutableAclService` direct refs in middle tier: 0** —
  good. All ACL goes through `gemma.gsec.SecurityService` or service-layer
  `@Secured` checks.
- `AFTER_ACL_*` / `ACL_SECURABLE_*` ConfigAttributes are declared exclusively
  at the gemma-core service interface — e.g.
  `ExpressionExperimentService.java` carries 122 `@Secured` annotations. The
  middle tier never names these attrs directly.

---

## 3. Hot spots for modernization

Ranked, top first.

### 3.1 `DatasetsWebService` (3,102 LOC) — REST collapse target
The single biggest file in the codebase except for `ExpressionExperimentDaoImpl`
and `ExpressionExperimentServiceImpl`. 113 `@Path` sub-routes, 17
`@Secured("GROUP_ADMIN")` mutations, manual `accessDecisionManager.decide`
escape-hatch at line 3071, and a heavy reliance on `loadValueObjectsWithCache`
+ `loadValueObjectsByIdsWithRelationsAndCache` (lines 297, 305, 772, 2771).
Already uses `StreamingOutput` in 9 places (1743, 1850, 1943, 1984, 2089,
2158, 2261, 2341, 2362) — so the streaming pattern is in-house but unevenly
applied. **Decompose into per-resource sub-services (DatasetsRead,
DatasetsCuration, DatasetsAnalytics, DatasetsExport)**; convert the two
manual `accessDecisionManager.decide` callsites to `@Secured` /
`@PreAuthorize`; promote the rest of the cacheable list endpoints to streamed
projections.

### 3.2 `ExpressionExperimentController` (1,777 LOC) — N+1 / VO assembly
30 `@Autowired` deps, 25 `expressionExperimentService.*` calls, and the
`applyFilter(...)` switch at line 1373 fetches all EE entities by ID in a
loop just to do `auditEventService.retainLackingEvent(...)` (lines 1380,
1385, 1389, 1393, 1407, 1412, 1416, 1420). Eight near-identical full-entity
re-loads per request. The `getBioMaterialsForEE` path (line 933) does
`bioMaterialService.thaw(...)` over `ee.getBioAssays().stream()...` — N+1 on
sample count. This controller is the canonical Phase 3 "Faster +
Streaming-by-default" target on the SSR side, but per project memory
gemma-web is being retired, so don't invest here directly — let the lessons
flow into the REST-side equivalents.

### 3.3 `AnalysisResultSetsWebService.getResultSets` — REST N+1
`gemma-rest/.../AnalysisResultSetsWebService.java:113-130`: for each EE in
the input, calls `expressionExperimentService.getSubSetsWithBioAssays(ee)`
inside a `for` loop. Trivially batchable into a single `findByExperimentsIn`
DAO method.

### 3.4 `SecurityController` (612 LOC) — ACL admin surface, untested
49 `securityService.*` calls, all concentrated here. This is the *only*
middle-tier surface that exercises ACL mutation (`makePrivate`,
`makeReadableByGroup`, `makeOwnedByUser`, group CRUD). The new
`InMemoryAclService` fixture + `Acl*Test` patterns from this Phase 2 ACL work
would make this testable without MySQL — currently it has a
`SecurityControllerTest` that `extends BaseSpringWebTest` (Spring + DB +
WebMVC; slow). **High-leverage modernization target** if the SSR admin UI is
not actually being killed — clarify status.

### 3.5 `ExpressionExperimentControllerHelperService` (502 LOC) — facade candidate
Holds 4 `@Transactional(readOnly=true)` boundaries (lines 118, 294, 433, 496)
that the EEController calls into purely to extend a Hibernate session across
a lazy-load chain. These methods are the smell of the
"deprecate `ensureInSession`" residual — they exist because the controller
needs `ee.getBioAssays().getBioMaterial().getCharacteristics()` after the
service call returns. With streaming DAOs + explicit fetch-graph
projections, this whole file can disappear.

### 3.6 CLI thaw-loop pattern in `ExpressionExperimentManipulatingCLI`
`gemma-cli/.../ExpressionExperimentManipulatingCLI.java:514` and
`LoadExpressionDataCli.java:272,308` show the per-EE loop pattern that ports
of these CLIs to `Stream<EE>` would clean up. Lower-priority than the REST
work because CLIs are throughput-tolerant.

---

## 4. Risks and surprises

1. **One file still imports `gemma.gsec.acl.domain.{AclPrincipalSid,
   AclGrantedAuthoritySid}`** —
   `gemma-web/.../SidValueObject.java:21-22`. Phase 3 ACL unification said
   this would already be gone. It's not; one cleanup edit remains.
2. **REST has no standalone deployment artifact.** `gemma-rest` is a JAR
   that the `gemma-web` WAR's `web.xml` mounts at `/rest/v2/*` via a Jersey
   servlet (web.xml:176–198). Retiring gemma-web (per memory) requires
   either packaging gemma-rest as its own WAR/Spring Boot app or splitting
   `web.xml` first. Currently the CORS filter (`web.xml:84–103`) and Spring
   Security filter (line 58–60) live in the web module, so the REST API
   inherits both. **This is the load-bearing surprise for the
   gemma-curation-ui migration.**
3. **Two manual `accessDecisionManager.decide(...)` callsites in REST**
   (`DatasetsWebService.java:3071`, `PlatformsWebService.java:308`) bypass
   `@Secured` for runtime-computed authorization decisions. Easy to convert
   once `@EnableMethodSecurity` lands.
4. **`@Transactional` in middle tier is a smell** — 10 occurrences, all
   inside `gemma-web/src/main/java/ubic/gemma/web/service/Expression*HelperService.java`
   (`ExpressionExperimentControllerHelperService.java`,
   `ExpressionExperimentEditControllerHelperService.java`). Each is a
   read-only boundary opened solely to make lazy-load work post-service-call.
   None of gemma-cli or gemma-rest annotates `@Transactional` — good.
5. **ThreadLocal usage is small but real**: 7 occurrences in
   gemma-cli's `BatchTaskProgressReporter` / `BatchTaskExecutorService`
   (`BatchTaskProgressReporter.java:27-28,50,59,68`). They're confined to
   batch progress reporting per-thread, not authentication. Still — per
   Phase 3 Vision "Cloud-ready / Remove ThreadLocal state" — flag for the
   eventual async/reactive port. None in gemma-rest or gemma-web.
6. **One direct DAO import in REST**:
   `gemma-rest/.../CompositeSequenceArrayArg.java:7` imports
   `CompositeSequenceDao` — but only for the static string constant
   `CompositeSequenceDao.OBJECT_ALIAS` used in a `Filter.parse(...)` call
   (line 35). Cosmetic, not architectural.
7. **No direct Hibernate Session use in middle tier** —
   `HibernateMonitorImpl.java` is the only file importing
   `org.hibernate.Session*`, and it exists to *monitor* sessionFactory
   statistics, not to manipulate sessions. Clean.
8. **Three `@Ignore`'d tests** are pre-existing and unrelated to Phase 2/3:
   `gemma-cli/.../NCBIGene2GOAssociationLoaderCLITest.java:90`
   (slow — issue #1056),
   `gemma-cli/.../CompletionGeneratorTest.java:65` (CI-only flake),
   `gemma-web/.../bibref/BibRefControllerTest.java:49` (CGLIB proxy issue).
   No `PHASE_2_RESIDUAL`-tagged tests found in the middle tier.
9. **`gemma-rest` has no XML security config of its own** — it relies on the
   gemma-web `applicationContext-security.xml`'s `<s:http pattern="/rest/v2/**">`
   chain (line 41–47). When gemma-web goes, this needs a Java-config
   replacement in gemma-rest. **Quietly load-bearing.**

---

## 5. Recommendation: first lighthouse modernization slice

**Lighthouse: `AnalysisResultSetsWebService.getResultSets` →
`getResultSetsByDatasetIds` streamed projection.**

Why this one:

- **Small enough**: ~297 LOC file, the offending endpoint is 28 lines
  (`AnalysisResultSetsWebService.java:103-130`). The fix is a single new DAO
  method (`findByExperimentIdsInWithSubSets(...)`) plus a 5-line controller
  rewrite.
- **Exercises four Phase 3 Vision dimensions**:
  - *Faster* — kills an N+1 (`for ee : ees -> getSubSetsWithBioAssays(ee)`)
    on the REST hot path.
  - *More efficient* — converts a `Collection<BioAssaySet>` accumulation to
    a streamed projection.
  - *Mobile-friendly* — endpoint already returns
    `FilteredAndPaginatedResponseDataObject`; can land cursor-pagination
    semantics as a side effect.
  - *Easier to maintain* — the new `findByExperimentIdsInWithSubSets` DAO
    method becomes the template for batching all the
    `for ee : ees -> service.thingForEe(ee)` patterns elsewhere
    (counted 30+ such loops middle-tier-wide; same shape).
- **Actively used**: this is the API GemBrow and gemma-curation-ui hit to
  list result sets for a dataset cohort — high-traffic, user-facing.
- **Tests reusable**: `AnalysisResultSetsWebServiceTest` and
  `AnalysisResultSetsJerseyTest` already exist
  (`gemma-rest/src/test/java/ubic/gemma/rest/`). Add one ACL-aware test
  using the new `InMemoryAclService` fixture pattern from Phase 2 ACL —
  proves that fixture is usable beyond gemma-core.

**Scope estimate** (one session, ~one workday):

| change | files touched | LOC |
|---|---|---|
| New `ExpressionExperimentDao` method + impl | 2 | ~40 |
| New `ExpressionExperimentService` facade method + `@Secured` | 2 | ~15 |
| `AnalysisResultSetsWebService.getResultSets` rewrite | 1 | ~20 (net -10) |
| Unit test in gemma-core using InMemoryAclService | 1 new | ~80 |
| Update `AnalysisResultSetsWebServiceTest` assertions | 1 | ~10 |
| **Total** | **6** | **~165** |

**Before/after metrics worth capturing**:
- Query count per request (Hibernate stats): N+1 → 1, expect 1+N → 2.
- p50/p95 latency on `/rest/v2/resultSets?datasets=...` with N=10 EEs.
- Memory: peak heap delta on a request returning ~5k result sets.

**Reusable pattern produced**: the
`findByEntityIdsInWith<Children>` DAO shape becomes the template the next
3.x slice applies to `getSubSetsByFactorValue`,
`getProcessedQuantitationType`, `getBioAssayDimension` (the other top-5 EES
methods called from REST/web).

---

### 5.1 Lighthouse slice — landed pattern

The first slice landed as the batched-subset variant of
`getSubSetsWithBioAssays`. Concrete shape:

| layer | signature |
|---|---|
| DAO | `Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> getSubSetsByExpressionExperiments(Collection<ExpressionExperiment>)` |
| Service | `@Secured({"IS_AUTHENTICATED_ANONYMOUSLY","ACL_SECURABLE_COLLECTION_READ"}) Map<EE,Collection<EESubSet>> getSubSetsWithBioAssays(Collection<EE>)` |
| Caller | `AnalysisResultSetsWebService.getResultSets` — one call, then `bas.addAll(subSetsByEE.values()…)` |

Pattern rules — apply these verbatim to every follow-on slice:

1. **Always take and return a `Collection`/`Map` keyed by the input
   entity.** Never reach into the result by id; the input itself is the
   key. This survives proxy/managed-instance shifts and reads naturally at
   the call site.
2. **Seed the result map with every input entity** (empty bucket if no
   children). Callers iterate without null-checks; this matches
   `getSampleRemovalEvents`'s contract.
3. **Guard empty input at the top** (`return emptyMap()`). Avoid the
   round-trip; avoid surprising the IN-list rewriter.
4. **Batch via `QueryUtils.listByIdentifiableBatch(query, "ees",
   inputs, 2048)`** so we get the existing
   `MAX_PARAMETER_LIST_SIZE`-aware splitter for free instead of a fresh
   `OPTIMIZE_*` cargo-cult.
5. **ACL annotation: `ACL_SECURABLE_COLLECTION_READ`** at the service
   interface — validates every input entity is readable up-front so the
   DAO can assume a pre-filtered set. The returned children inherit
   ACL semantics from their parent.
6. **Don't bypass the service.** The DAO method exists, but the REST/web
   caller goes through the service-layer facade so the `@Secured` voter
   still runs.

Next candidates that match this shape (drop-in conversions):

| current per-EE method | batched replacement |
|---|---|
| `EES.getBioAssayDimension(ee)` | `Map<EE,BioAssayDimension> getBioAssayDimensionByExpressionExperiments(Collection<EE>)` |
| `EES.getProcessedQuantitationType(ee)` | `Map<EE,QuantitationType> getProcessedQuantitationTypeByExpressionExperiments(Collection<EE>)` |
| `EES.getSubSetsByDimension(ee)` | `Map<EE,Map<BioAssayDimension,Set<EESubSet>>> getSubSetsByDimensionByExpressionExperiments(Collection<EE>)` |
| `auditEventService.retainLackingEvent(ees, type)` loop in `ExpressionExperimentController.applyFilter` | already plural, but used 8× — wrap into one call that takes a `Set<Class<? extends AuditEventType>>` |

The `ExpressionExperimentBatchInformationServiceImpl` and
`ExpressionExperimentPlatformSwitchService` callers of
`getSubSetsWithBioAssays(ee)` are CLI-side and run in non-throughput-critical
paths — leave them alone for now; the new batched method is available if a
follow-on slice needs it.
