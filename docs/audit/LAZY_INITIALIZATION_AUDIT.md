# Lazy-initialization and proxy-blindness audit

Prompted by three production failures on 2026-08-23, each of which stopped a routine
workflow (`makeProcessedData` over 300 datasets) dead. All three are the same underlying
mistake in different clothes: **code assumes it holds a real, session-attached entity
when what it actually holds is a Hibernate proxy, an uninitialized collection, or a
detached instance.**

Fixed so far: `cb3838c718`, `7342805af5`. Still open: the refresh endpoint (below).

## The three shapes

| # | shape | symptom | fixed in |
|---|---|---|---|
| A | `instanceof` chain on a value that arrives as a proxy | falls through to `throw`, returns `null`, or takes the wrong branch | `cb3838c718` (one site) |
| B | iterating a lazy collection with no session | `LazyInitializationException: could not initialize proxy - no Session` | `7342805af5` (one site) |
| C | building a value object across two transactions | same exception, on an association the thaw did not cover | open |

`Hibernate.unproxy` is the remedy for A, `Hibernate.isInitialized` for B. Neither is
exotic — the codebase already used `unproxy` in five places before this audit.

## A — proxy-blind dispatch

58 `instanceof` sites across the three modules test for `ExpressionExperiment` /
`ExpressionExperimentSubSet` / `BioAssaySet`. 32 dispatch on a value reached through an
association (`getExperimentAnalyzed()`, a `BioAssaySet` parameter, `entity`, `parent`,
`curatable`) and carry no `unproxy` in the enclosing method. Query results are generally
safe; **association-sourced values are not**, and that is the whole population below.

Ranked by what the miss costs, worst first:

### A1 — silent wrong answer, security-relevant

`model/analysis/SingleExperimentAnalysis.java:72-82` — `getSecurityOwner()` returns
**null** for anything that is not literally an `ExpressionExperiment` or
`ExpressionExperimentSubSet`. This is the hook the ACL machinery consults to decide
which ACL an analysis inherits (`AclEventListener`'s `securityOwner` path). A proxy here
does not throw; the analysis simply gets no parent ACL. Wrong visibility rather than a
stack trace, so nothing surfaces.

### A2 — hard failure on hot paths

Both throw `UnsupportedOperationException( "Couldn't handle a " + bas.getClass() )`, the
exact shape that killed `makeProcessedData`:

- `persistence/util/CommonQueries.java:106-113` — `getExperiment( @MayBeUninitialized
  BioAssaySet bas )`. The parameter is **annotated `@MayBeUninitialized`**, so the risk
  is documented on the signature and contradicted by the body.
- `persistence/service/expression/bioAssayData/CachedProcessedExpressionDataVectorServiceImpl.java:737-748`
  — `getExperiment( BioAssaySet bas )`, on the vector-retrieval path.

### A3 — blind cast

`core/analysis/service/DiffExAnalysisResultSetWriter.java:478-486` —
`experimentForBioAssaySet` tests only for the subset case and casts everything else with
`( ExpressionExperiment ) bas`. A proxy takes the else branch and throws
`ClassCastException` rather than anything diagnosable.

### A4 — the rest

`DiffExAnalyzerUtils` (2 methods), `DiffExMetaAnalyzerServiceImpl:612`,
`DifferentialExpressionAnalysisHelperServiceImpl:62`,
`DifferentialExpressionAnalyzerServiceImpl:589`, `BatchConfound:50`,
`ExpressionExperimentBatchInformationServiceImpl:161-164`, `WhatsNew:87,100`,
`WhatsNewServiceImpl:213`, `GeoServiceImpl:1164`, `CharacteristicUpdateTaskImpl:298`,
`PersisterHelperImpl:225-237`, `DifferentialExpressionAnalysisDaoImpl:548`,
`CurationDetailsServiceImpl:142`, `GenericCuratableDaoImpl:32`,
`PublicationAssociationServiceImpl:378`,
`CachedProcessedExpressionDataVectorServiceImpl:631`, `EntityUrlBuilder:223,229,348`.

Several of these are reached only with entities the caller just loaded, so they may
never see a proxy in practice. Each needs a caller check rather than a blanket rewrite —
but the cost of the guard is one line and the cost of being wrong is a dead workflow.

## B — lazy collections in flush-time listeners

Contained. Only two Hibernate event listeners exist:

- `core/security/acl/AclEventListener.java` — walks every collection association of the
  entity being inserted. One walk, now guarded by `Hibernate.isInitialized`
  (`7342805af5`). Note the pre-existing `try/catch ( RuntimeException )` around
  `getPropertyValue` does **not** cover this: Hibernate returns the unloaded
  `PersistentCollection` without touching it, so the exception lands on the `for`, not
  on the guarded call.
- `persistence/audit/AuditTrailEventListener.java` — no collection walks.

Anything added to this package later should carry the same guard. Forcing a lazy load
inside a flush listener is not a fix; skipping is correct, because a transient child
cannot be inside an uninitialized collection.

## C — value objects built across two transactions

`rest/DatasetsWebService.java:8186` (`refreshDataset`) is the open one:

```java
ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( id );
...
new ResponseDataObject...( expressionExperimentService.loadValueObject( ee ) )
```

`loadAndThawLiteWithRefreshCacheMode` is `@Transactional(readOnly = true)`
(`ExpressionExperimentReadServiceImpl:286`). `loadValueObject` is a separate
`@Transactional(readOnly = true)` (`AbstractVoEnabledService:25`). With no transaction
spanning the JAX-RS method, these are **two sessions**: `ee` is detached by the time the
VO is built, and any association `thawLite` did not cover is a dead proxy.

The association that bites is the curation details' audit events —
`AbstractCuratableValueObject:66-67` and `CurationDetailsValueObject:43-51` read
`getLastTroubledEvent()`, `getLastNeedsAttentionEvent()` and `getLastNoteUpdateEvent()`.
Result: `Could not initialize proxy [AuditEvent#<id>] - no session`, a 500 on every
`GET /datasets/{id}/refresh`, so the post-write cache eviction never runs.

**The recurrence rule for C:** any REST method that calls one `@Transactional` service
method and then hands the result to another one is exposed. The entity crosses a session
boundary in between. Candidates worth checking on the same grounds:
`HeatmapDataService:264`, `DatasetsWebService:4772`, `:4815`, `:5439`,
`DatasetArgService:274` — each thaws and then builds VOs.

## Suggested order

1. **A1** — `getSecurityOwner()`, because it fails silently and the failure is an ACL.
2. **C** — the refresh endpoint, because it fires on every CLI write today.
3. **A2/A3** — one-line `unproxy` each, on paths that are known to be hot.
4. **A4** — as touched, not as a sweep.

A cheaper structural option for A: give `BioAssaySet` an `asExpressionExperiment()` (or
put the unproxy inside a single shared resolver) and route the 32 sites through it, so
the pattern cannot be got wrong again. `CommonQueries.getExperiment` and
`CachedProcessedExpressionDataVectorServiceImpl.getExperiment` are already the same
method written twice.
