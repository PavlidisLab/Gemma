# External URL reachability probe

A single out-of-band test that probes every external URL Gemma depends on
(ontology endpoints, NCBI/Entrez APIs, FTP archives, BioMart, CELLxGENE,
UCSC) and emits a structured JSON report. Designed to be run nightly by an
ops dashboard so URL drift (NCBI moves an endpoint, an ontology release
breaks a PURL) gets caught before a curator hits the broken loader in
production.

## What it does

`gemma-core/src/test/java/ubic/gemma/core/util/test/ExternalUrlReachabilityTest.java`
holds a curated `ENDPOINTS` list — currently ~40 URLs across the
categories below — and probes each one in parallel with a 5s connect
timeout and a 5s read timeout:

| Category    | Examples                                                            |
| ----------- | ------------------------------------------------------------------- |
| Entrez      | eutils.ncbi.nlm.nih.gov esearch/esummary/efetch/elink/einfo         |
| NCBI-FTP    | ftp.ncbi.nlm.nih.gov gene/DATA, pub/taxonomy, pub/HomoloGene        |
| GEO         | www.ncbi.nlm.nih.gov/geo/query/acc.cgi + ftp.ncbi.nlm.nih.gov/geo   |
| SRA         | www.ncbi.nlm.nih.gov/sra term search                                |
| EBI-FTP     | ftp.ebi.ac.uk pub/databases (GOA HUMAN, ArrayExpress experiments)   |
| GO          | ftp.geneontology.org godatabase archive                             |
| Ontology    | purl.obolibrary.org/obo/{uberon, cl, go, doid, mondo, ...}, EBI EFO |
| BioMart     | grch37.ensembl.org/biomart/martservice                              |
| CELLxGENE   | api.cellxgene.cziscience.com dp/v1/collections + datasets indexes   |
| UCSC        | cells.ucsc.edu (UCSC Cell Browser)                                  |
| Gemma       | gemma.msl.ubc.ca (used as ontology hosting via TGEMO)               |

HTTP probes use HEAD (with a Range:bytes=0-0 GET fallback when the server
returns 405/501). FTP probes connect, login anonymously, and STAT-or-CWD
the target path.

## How to invoke

```bash
# JDK 21 + the canonical worktree env
export JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Override excludedGroups to clear the "network" exclusion the parent pom
# adds for the default mvn verify pass.
mvn -pl gemma-core test \
    -Dtest=ExternalUrlReachabilityTest \
    -DexcludedGroups=
```

## Output

The test writes a flat JSON report to `gemma-core/target/external-url-reachability.json`:

```json
{
  "timestamp": "2026-05-21T11:34:56Z",
  "totalCount": 40,
  "okCount": 38,
  "failCount": 2,
  "results": [
    {"label": "NCBI eutils - esearch", "category": "Entrez",
     "url": "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=gemma&retmax=1",
     "ftp": false, "ok": true, "status": 200, "elapsedMs": 412},
    {"label": "Uberon ontology", "category": "Ontology",
     "url": "http://purl.obolibrary.org/obo/uberon.owl",
     "ftp": false, "ok": false, "status": -1, "elapsedMs": 5012,
     "error": "SocketTimeoutException: connect timed out"}
  ]
}
```

The schema is intentionally flat so a CI artifact, ops dashboard, or
ad-hoc `jq` filter can consume it directly:

```bash
jq -r '.results[] | select(.ok==false) | "\(.category)\t\(.label)\t\(.url)\t\(.error)"' \
    gemma-core/target/external-url-reachability.json
```

## Failure behaviour

- If **all** URLs fail the test SKIPS (`Assumptions.assumeTrue(okCount > 0)`).
  The host network is almost certainly down and the result would be
  uninformative; this prevents the dashboard from flooding with false
  positives.
- If **some** URLs fail the test PASSES. The dashboard alerts on per-URL
  drift from the JSON, not on the JUnit verdict.

## Why it's tagged `@Tag("network")`

The parent `pom.xml` surefire configuration excludes `integration,network`
from the default `mvn verify` pass (around line 1121). This keeps the
day-to-day signal fast and deterministic. The probe is meant to be run
on demand (CI nightly job, or manually via the command above) — never as
part of `mvn verify`.

## Adding a new endpoint

When wiring a new external service into Gemma, add an entry to the
`ENDPOINTS` list in `ExternalUrlReachabilityTest`:

```java
new Endpoint( "Human-readable label", "CategoryTag",
        "https://example.com/some/endpoint?with=args", false /* ftp? */ ),
```

- `label` shows up in the dashboard — make it specific (`"NCBI eutils - esearch"`,
  not `"NCBI"`).
- `category` groups related endpoints — reuse an existing one (`"Entrez"`,
  `"NCBI-FTP"`, `"Ontology"`, `"GEO"`, `"EBI-FTP"`, `"BioMart"`,
  `"CELLxGENE"`, `"UCSC"`, `"SRA"`, `"GO"`, `"Gemma"`) unless the new
  service is genuinely a new family.
- `url` should hit a small, cheap endpoint — an index/listing page or a
  cheap query, not a multi-GB ontology download. The probe does HEAD
  (or Range-limited GET) so the payload doesn't matter much, but
  pointing at a small endpoint is faster and friendlier.
- `ftp=true` switches to the `commons-net` FTP probe (anonymous login +
  STAT/CWD). Use it for any plain `ftp://` URL.
