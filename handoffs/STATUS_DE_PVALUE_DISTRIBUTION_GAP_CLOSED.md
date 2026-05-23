# Status — DE p-value distribution endpoint: gap closed pre-baseline

**From:** bro (Gemma Java REST)
**For:** GUI Claude (apps/browser, gemma-ui)
**Filed:** 2026-05-23
**Status:** closed — endpoint already shipped on `phase2-acl-migrate` (`11ffc7464c`, merged via `30dc96eb2c`).

## TL;DR

UIB's ask in `DE_PVALUE_DISTRIBUTION_HANDOFF.md` is already satisfied by
the in-flight endpoint `GET /resultSets/{id}/pvalueDistribution`. No
delta required. Wire the browser-side `PvalueHistogramStrip` against the
shape below; the bin objects use the same `lo` / `hi` / `count` keys
UIB proposed in Option A.

## Receipts — UIB ask vs shipped

| UIB ask (Option A) | Shipped (`11ffc7464c`) | Match |
|---|---|---|
| `GET /rest/v2/resultSets/{resultSet}/pvalueDistribution` | `GET /resultSets/{resultSet}/pvalueDistribution` on `AnalysisResultSetsWebService` | yes |
| `?bins=20` default, configurable | `@QueryParam("bins") @DefaultValue("20")`, validated 1..1000 | yes |
| `?column=corrected` default, `raw` \| `corrected` | `@QueryParam("column") @DefaultValue("corrected")`, validated to those two values | yes |
| `data.resultSetId` | `PvalueDistributionValueObject.resultSetId` (Long) | yes |
| `data.column` | `PvalueDistributionValueObject.column` (String) | yes |
| `data.n` (probes counted) | `PvalueDistributionValueObject.n` (long, derived from bin counts) | yes |
| `data.bins[{ lo, hi, count }]` | `PvalueDistributionValueObject.bins` → `List<Bin>` with `lo`, `hi`, `count` fields | yes — key names match exactly |
| 404 on missing result set | `expressionAnalysisResultSetArgService.getEntity(...)` 404s on missing / ACL-blocked | yes |
| 204 when no p-values | Server returns `Response.noContent()` when total count is zero | yes |
| 200 + payload otherwise | Standard `ResponseDataObject` envelope (`{ "data": { ... } }`) | yes |
| Public-readable (same posture as TSV at `/resultSets/{id}`) | ACL check via the same `ExpressionAnalysisResultSetArgService` path the TSV endpoint uses; no auth requirement layered on top | yes |
| ~200 ms for ~20K-probe result set | Single `GROUP BY FLOOR(p * :bins)` aggregation on `DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT.RESULT_SET_FK` (indexed); no per-row Java work | expected — not benchmarked here |

## Explicitly-optional fields not shipped

UIB flagged two items as "if cheap" / "optional"; neither is in the
shipped endpoint. Call them out if you want them:

- `data.lambda` — pi0-style null-hypothesis proportion estimate. UIB:
  *"optional pi0-style proportion null hypothesis estimate, if cheap"*.
  Not shipped. Cheap-ish to compute (count fraction above some
  threshold, e.g. p>0.5) but would require a second aggregation
  query; flag if you want it and bro can add a parameter or always-on
  field.
- `?contrast=<factorValueId>` — per-contrast slicing for multi-factor
  result sets. UIB: *"if splitting is cheap, accept `?contrast=<factorValueId>`"*.
  Not shipped. Result-set p-values are stored per-result-row, not
  per-contrast-column; would need either a join through the contrast
  table or a different storage shape. Flag if needed and bro will scope.

## Wire shape (concrete, for the browser binding)

```jsonc
// GET /resultSets/642421/pvalueDistribution?bins=20&column=corrected
{
  "data": {
    "resultSetId": 642421,
    "column": "corrected",
    "n": 18691,
    "bins": [
      { "lo": 0.00, "hi": 0.05, "count": 5236 },
      { "lo": 0.05, "hi": 0.10, "count":  812 },
      // … 20 bins total by default
      { "lo": 0.95, "hi": 1.00, "count":  402 }
    ]
  }
}
```

Bin semantics: bin `i` covers `[i/bins, (i+1)/bins)`; the **last bin
is closed on the right** so a p-value of exactly `1.0` is counted in
the final bin. Rows with a `NULL` p-value in the selected column are
excluded from `n`.

Error responses use the standard `ResponseErrorObject` envelope:
- `400` — invalid `bins` (must be 1..1000) or invalid `column`.
- `404` — result set missing or ACL-blocked.
- `204` — result set exists but every row's chosen p-value column is `NULL`.

## OpenAPI

Endpoint is annotated on `AnalysisResultSetsWebService` and surfaces a
concrete `PvalueDistributionResponseDataObject` schema for Swagger
(the generic `ResponseDataObject<T>` doesn't expose `T` to the spec).
Browse via `/resources/restapidocs/` once UIB hits the server.

## Provenance

- Endpoint commit: `11ffc7464c` (Paul, 2026-05-22)
- Merge: `30dc96eb2c` (`agent-de-expression-pvalue-distribution` → `phase2-acl-migrate`)
- Files of record:
  - `gemma-rest/src/main/java/ubic/gemma/rest/AnalysisResultSetsWebService.java` (handler)
  - `gemma-rest/src/main/java/ubic/gemma/rest/PvalueDistributionValueObject.java` (VO)
  - `gemma-core/src/main/java/ubic/gemma/persistence/service/analysis/expression/diff/ExpressionAnalysisResultSetDao{Impl,}.java` (DAO + GROUP BY)
  - `gemma-core/src/main/java/ubic/gemma/persistence/service/analysis/expression/diff/ExpressionAnalysisResultSetService{Impl,}.java` (service hop)

## Pattern note

This is the second pre-baselined UIB ask closed by receipts-only this
session — `DE_EXPRESSIONS_ENRICH_GENE_INFO_HANDOFF` was the first
(closed by `3829961887`, receipts in `STATUS_DE_EXPRESSIONS_ENRICH_GAP_CLOSED.md`).
UIB asks landing on the agent queue while parallel feature work is
in flight against the same surface — worth a quick `git log` grep on
the endpoint name before scoping the response.
