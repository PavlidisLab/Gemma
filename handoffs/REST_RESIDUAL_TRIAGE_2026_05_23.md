# gemma-rest residual surefire triage (2026-05-23)

Worktree: `.claude/worktrees/agent-rest-residual-triage`, branch `agent-rest-residual-triage`, baselined off `304a2ff854`.

Triage of the 15 pre-existing gemma-rest surefire failures the prior agent (`agent-fix-datasetswebservicetest-residuals`) called out of scope.

## Baseline

`mvn -pl gemma-rest test`: **Tests run: 464, Failures: 2, Errors: 13, Skipped: 0**.

## Per-test inventory

| Class#method | Error shape | Classification | Action | Status |
|---|---|---|---|---|
| `AnnotationsWebServiceTest#testSearchAnnotationsPopulatesUsageCount` | `containsEntry "usageCount"=2` got `1` | Test cross-pollution via static `SEARCH_CACHE` in `AnnotationsWebService` | Clear the static cache via reflection in `@BeforeEach` | **fixed** |
| `OpenApiTest#testEnsureThatAllEndpointHaveADefaultGetResponseOrIsARedirection` | Soft-assertion multi-failure: ~12 endpoints have spec gaps (missing response codes, `*/*` content type on 200/204, missing content blocks) | Production-shape drift: real OpenAPI spec gaps across `/admin/caches*`, `/admin/hibernate/reset`, `/annotations/datasets/*/annotations/*`, `/candidates`, `/genes/*/{differentialExpression,homologues,overview}`, `/platforms/*/elements/*/mappingSummary`, `/health`, `/metrics` | Filed (see below) | **filed** |
| `OpenApiTest#testEnsureThatAllErrorResponsesUseResponseErrorObjectWithJsonMediaType` | NPE: `MediaType.getSchema()` returned `null` on an `application/json` error response | Production-shape drift: some 4xx/5xx ApiResponse entries have a content block but no schema. Likely related to the same gaps as the first OpenApi failure | Filed (see below) | **filed** |
| `DatasetsWebServiceBlacklistedCursorTest#cursorModeForwardsFiltersBuiltByArgService` | Mockito `UnnecessaryStubbingException` on `setUp:82` | Strict-stubbing gap: shared setUp stub re-stubbed by per-test branch | Wrap setUp stub in `lenient()` | **fixed** |
| `DatasetsWebServiceBlacklistedCursorTest#cursorModeInferredTermsEchoedInResponse` | Same as above | Same | Same | **fixed** |
| `DatasetsWebServiceByIdsCursorTest#cursorModeForwardsComposedFiltersIncludingPathDatasetArg` | Mockito strict-stubbing (2 unused stubs) | Same | Same | **fixed** |
| `DatasetsWebServiceByIdsCursorTest#cursorModeInferredTermsEchoedInResponse` | Same | Same | Same | **fixed** |
| `DatasetsWebServiceExpressionLevelsForGeneCursorTest#cursorModeInferredTermsEchoedInResponse` | Same | Same | Same | **fixed** |
| `PlatformsWebServiceByIdsCursorTest#cursorModePreservesPathIdSetPredicate` | Same | Same | Same | **fixed** |
| `PlatformsWebServiceElementCursorTest#cursorModeDecodesCursorAndForwardsToArgService` | Same | Same | Same | **fixed** |
| `PlatformsWebServiceElementCursorTest#cursorModeForwardsBothPlatformArgAndProbesArg` | Same | Same | Same | **fixed** |
| `PlatformsWebServiceElementCursorTest#cursorModeRoutesToCursorHelperAndReturnsFilteredCursorResponse` | Same | Same | Same | **fixed** |
| `PlatformsWebServiceElementGenesCursorTest#cursorModeDecodesCursorAndForwardsToArgService` | Same | Same | Same | **fixed** |
| `PlatformsWebServiceElementGenesCursorTest#cursorModeEmptyPageProducesEmptyResponseWithNoNextCursor` | Same (2 unused stubs) | Same | Same | **fixed** |
| `PlatformsWebServiceElementGenesCursorTest#cursorModeForwardsBothPlatformArgAndProbeArg` | Same | Same | Same | **fixed** |

## After

`mvn -pl gemma-rest test`: **Tests run: 464, Failures: 1, Errors: 1, Skipped: 0** (13 closed, 2 remaining — both in `OpenApiTest`).

## Summary

- **13 fixed**: 12 Mockito strict-stubbing residuals across 6 cursor tests + 1 static-cache cross-pollution in `AnnotationsWebServiceTest`.
- **2 filed**: both `OpenApiTest` failures, which assert REST API spec contract invariants that genuinely don't hold today.

## Filed: OpenApiTest residuals (next session to route)

Both failures exercise the OpenAPI 3 spec emitted by Swagger/Jakarta-REST integration and assert structural invariants. The diagnoses:

### (1) `testEnsureThatAllEndpointHaveADefaultGetResponseOrIsARedirection`

For each operation, asserts:
- It has at least one 2xx OR 3xx response code, and
- For 200 responses, content map has at least one entry AND does not contain `*/*`,
- For 201 responses, content map does not contain `*/*`,
- For 204/3xx responses, content map is null.

Actual gaps (from the failure soft-assertion bundle):
- `DELETE /admin/caches (clearAllCaches)` → 200 content has `*/*` instead of `application/json`.
- `DELETE /admin/caches/{cacheName} (clearCache)` → has only 404, missing a 2xx/3xx default.
- `POST /admin/hibernate/reset (resetHibernateStats)` → 200 content has `*/*`.
- `DELETE /annotations/datasets/{dataset}/annotations/{annotationId} (removeDatasetAnnotation)` → 204 content not null.
- `GET /candidates (getCandidates)` → 200 content has `*/*`.
- `GET /genes/{gene}/{differentialExpression,homologues,overview}` → only 404, missing 2xx.
- `GET /platforms/{platform}/elements/{probe}/mappingSummary` → only 404, missing 2xx.
- `GET /health (getHealth)` → 200 content has `*/*`.
- `GET /metrics (scrape)` → 200/401/404/503 all have `*/*`.

Root cause is per-endpoint: either missing `@ApiResponse(responseCode=…, …, content=@Content(schema=@Schema(implementation=…)))` annotations, missing media-type qualifiers, or stale 200-without-`@Produces` declarations that fall through to `*/*`. Routing decision: this is a spec-hygiene clean-up that touches ~10 resource methods; should be one focused commit per resource class.

### (2) `testEnsureThatAllErrorResponsesUseResponseErrorObjectWithJsonMediaType`

NPE: an `application/json` error-response media type has `getSchema() == null`. The test wants every 4xx/5xx response with an `application/json` content block to declare `schema=@Schema(implementation=ResponseErrorObject.class)`. Same family of fix — add the missing schema on whichever endpoint's error response is malformed. The test bails on the first NPE so we don't know how many endpoints are affected; the spec-hygiene clean-up above should cover this one too.

## Files touched

- `gemma-rest/src/test/java/ubic/gemma/rest/AnnotationsWebServiceTest.java` — clear static `SEARCH_CACHE` between tests.
- `gemma-rest/src/test/java/ubic/gemma/rest/DatasetsWebServiceBlacklistedCursorTest.java` — `lenient()` wrap.
- `gemma-rest/src/test/java/ubic/gemma/rest/DatasetsWebServiceByIdsCursorTest.java` — `lenient()` wrap.
- `gemma-rest/src/test/java/ubic/gemma/rest/DatasetsWebServiceExpressionLevelsForGeneCursorTest.java` — `lenient()` wrap.
- `gemma-rest/src/test/java/ubic/gemma/rest/PlatformsWebServiceByIdsCursorTest.java` — `lenient()` wrap.
- `gemma-rest/src/test/java/ubic/gemma/rest/PlatformsWebServiceElementCursorTest.java` — `lenient()` wrap.
- `gemma-rest/src/test/java/ubic/gemma/rest/PlatformsWebServiceElementGenesCursorTest.java` — `lenient()` wrap.
