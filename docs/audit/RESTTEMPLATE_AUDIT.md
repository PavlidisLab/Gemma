# RestTemplate audit (Phase 3 Spring 6+ modernization)

Audit date: 2026-05-18. Baseline: `08e760bdaf` on `phase2-acl-migrate`.

## Context

`RestTemplate` is not deprecated in Spring 6 but is in maintenance mode.
Spring recommends `RestClient` (synchronous, fluent, near drop-in for
`RestTemplate`, Spring 6.1+) or `WebClient` (reactive) for new code.

## Inventory

Search patterns:

- `RestTemplate\b` / `AsyncRestTemplate\b`: 7 hits across **2 files**
- `new RestTemplate\b`: 2 hits (1 production, 1 test)
- `WebClient\b` / `RestClient\b`: **0 hits** (no migration started)

## Per-site disposition

| # | File | Role | Disposition |
|---|------|------|-------------|
| 1 | `gemma-rest/src/main/java/ubic/gemma/rest/analytics/ga4/GoogleAnalytics4Provider.java` | Production: GA4 measurement-protocol client | **Defer** — non-trivial migration |
| 2 | `gemma-rest/src/test/java/ubic/gemma/rest/analytics/ga4/GoogleAnalytics4ProviderTest.java` | Test that wires a `RestTemplate` with a request-logging interceptor and passes it to the provider | **Defer** — migrates with #1 |

Bean wiring: `gemma-rest/src/main/resources/ubic/gemma/applicationContext-analytics.xml`
instantiates `GoogleAnalytics4Provider` via the `(String, String)`
constructor — no `RestTemplate` bean is exposed to the rest of the
context. The `RestTemplate` instance is fully encapsulated inside the
GA4 provider.

## Why #1 is not a near-drop-in

1. **Two call shapes**: `restTemplate.postForObject(url, body, ValidationResult.class, apiSecret, measurementId)` and
   `restTemplate.postForLocation(url, body, apiSecret, measurementId)`,
   both using positional URI template variables.
   - `postForObject` maps cleanly to `restClient.post().uri(url, apiSecret, measurementId).body(payload).retrieve().body(ValidationResult.class)`.
   - `postForLocation` (returns `Location` header) has no single-call
     equivalent on `RestClient`; needs `.retrieve().toBodilessEntity()`
     plus a `.getHeaders().getLocation()` and the call site here
     actually discards the return value, so the conversion is a
     `.retrieve().toBodilessEntity()` (drop) — fine, but coupled with
     the exception remap below.
2. **Exception handling on `RestClientException`** with an
   `instanceof ClientHttpRequestExecution` branch. That `instanceof`
   check is **dead code** (`ClientHttpRequestExecution` is the
   interceptor-chain interface, not an exception type) — flag for
   cleanup as part of any migration. `RestClient` throws
   `RestClientException` subtypes too, so the catch shape stays, but
   the dead branch should be removed and replaced with a real
   client-vs-server discrimination (e.g. catching
   `HttpClientErrorException` vs `HttpServerErrorException` vs
   `ResourceAccessException`).
3. **Public API surface**: the
   `GoogleAnalytics4Provider(RestTemplate, String, String)` constructor
   is part of the class's testable public API and is invoked from the
   test with `restTemplate.getInterceptors().add(...)` to log
   requests. A `RestClient`-based constructor would need to accept a
   `RestClient` (or a `RestClient.Builder`) and the test interceptor
   would convert to a `ClientHttpRequestInterceptor` registered via
   `RestClient.Builder.requestInterceptor(...)`.

None of this is hard, but it crosses the "trivial / drop-in" line set
for this audit pass: not a single-line `getForObject` swap.

## Recommended migration shape (for future work)

```java
// Constructor
public GoogleAnalytics4Provider( RestClient restClient, String measurementId, String apiSecret ) { ... }
public GoogleAnalytics4Provider( String measurementId, String apiSecret ) {
    this( RestClient.create(), measurementId, apiSecret );
}

// In flush():
if ( debug ) {
    ValidationResult v = restClient.post()
        .uri( debugEndpoint, apiSecret, measurementId )
        .body( payload )
        .retrieve()
        .body( ValidationResult.class );
    if ( v != null && !v.validationMessages.isEmpty() ) {
        throw new IllegalArgumentException( v.toString() );
    }
} else {
    restClient.post()
        .uri( endpoint, apiSecret, measurementId )
        .body( payload )
        .retrieve()
        .toBodilessEntity();
}
```

Plus: drop the dead `instanceof ClientHttpRequestExecution` branch
and replace with proper `HttpClientErrorException` /
`HttpServerErrorException` / `ResourceAccessException` discrimination.

Test-side change: build the `RestClient` with
`RestClient.builder().requestInterceptor((request, body, execution) -> { log...; return execution.execute(request, body); }).build()`.

## Disposition summary

- 0 trivial conversions applied this pass.
- 1 production callsite + its test deferred with a recommended
  migration shape documented above.
- No `RestTemplate` `@Bean` declarations exist; surface is fully
  contained inside `GoogleAnalytics4Provider`.

Pickup cost when prioritized: ~1 hour including dead-branch cleanup
and test rewrite. Low risk — single class, fully encapsulated.
