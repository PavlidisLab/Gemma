# NCBI eutils rate-limit note

The external URL reachability test was returning HTTP 429 against
`eutils.ncbi.nlm.nih.gov` for the `efetch.fcgi` and `elink.fcgi` probes.

## Root cause

NCBI's published eutils rate cap is:

- **3 requests/second** without an API key
- **10 requests/second** with an `api_key=<key>` query param

The reachability test was firing all six eutils endpoints
(`einfo`, `esearch`, `esummary`, `efetch`, `elink`, plus the
non-eutils `pubmed.ncbi.nlm.nih.gov` landing) in parallel via
`ForkJoinPool`, which immediately tripped the 3 req/s cap and triggered
the 429s.

## Production-side status: API key wiring is already in place

Runtime Entrez calls go through `EntrezUtils.doNicely(EntrezCall<T>, String apiKey)`
(`gemma-core/src/main/java/ubic/gemma/core/loader/entrez/EntrezUtils.java`).
The method synchronizes globally and waits the per-call interval
(`TIMEOUT_AUTHENTICATED_MS` vs `TIMEOUT_ANONYMOUS_MS`) before each
call, then appends `&api_key=<key>` to the URL when one is configured.

The API key is wired into every Entrez caller via
`@Value("${entrez.efetch.apikey}")`:

- `GeoBrowserServiceImpl`
- `GeoServiceImpl`
- `SimpleExpressionDataLoaderServiceImpl`
- `CellXGeneDataLoaderServiceImpl`
- `BibliographicReferenceServiceImpl`

The default in `project.properties` is blank:

```properties
entrez.efetch.apikey=
```

Operators who want the 10 req/s cap on production loaders need to set
the property in their site-local `Gemma.properties`:

```properties
entrez.efetch.apikey=<key-from-https://www.ncbi.nlm.nih.gov/account/settings/>
```

No code change is needed — just a config edit on the deployment.

## What the reachability test now does

The 2026-05-21 commit
(`test(reachability): serialize NCBI eutils probes ...`) splits the
endpoint inventory into two passes:

1. **Parallel pass** for everything that doesn't rate-limit (ontologies,
   FTP archives, BioMart, CELLxGENE, UCSC, the non-eutils NCBI pages).
2. **Serial pass** for `eutils.ncbi.nlm.nih.gov` endpoints with a
   400ms sleep between calls (`EUTILS_INTERVAL_MS`), keeping the test
   under the 3 req/s anonymous cap with headroom.

This is a test-only change. The production rate-limit handling in
`EntrezUtils.doNicely` is unchanged.
