# SpotBugs first-pass findings (2026-05-21)

Wired `spotbugs-maven-plugin` 4.8.6.4 (+ `findsecbugs-plugin` 1.13.0) into
the parent `pom.xml`. Opt-in only — invoked as `mvn -pl gemma-core
spotbugs:spotbugs` / `mvn -pl gemma-core spotbugs:check`. NOT bound to the
verify phase.

Run config: `effort=Max`, `threshold=Medium`, `includeTests=false`,
`fork=true`, `maxHeap=4096`. Exclude filter at `spotbugs-exclude.xml`
suppresses the 33 entity-class false positives flagged in
`STATIC_ANALYSIS_SWEEP.md` (every `model.*` entity inherits
`equals`/`hashCode` from `AbstractIdentifiable`/`AbstractDescribable`,
which declares both abstract).

## Headline numbers

| metric | value |
|---|---|
| total bugs reported | **3 639** |
| priority 1 (high confidence) | 187 |
| priority 2 (normal) | 3 452 |
| rank ≤ 4 ("scariest") | 3 |
| rank 5–9 ("scary") | 106 |
| rank 10–14 ("troubling") | 993 |
| rank 15–20 ("of concern") | 2 537 |

By category: MALICIOUS_CODE 1 503 (mostly `EI_EXPOSE_REP*`), STYLE 1 375
(largely `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` + `RCN_REDUNDANT_NULLCHECK`),
BAD_PRACTICE 238, **SECURITY 232** (findsecbugs), PERFORMANCE 118,
CORRECTNESS 107, I18N 48, MT_CORRECTNESS 12, EXPERIMENTAL 6.

## Value-add over the regex sweep

The 2026-05-20 regex sweep (`STATIC_ANALYSIS_SWEEP.md`) covered: flipped
`.equals("literal")`, raw-stream resource leaks, static `SimpleDateFormat`,
DCL, empty catch, `equals`/`hashCode`, NaN map keys, `URL.equals`, `wait()`
outside loop, `Arrays.asList` mutation, `compareTo` subtraction,
`StringBuffer`.

SpotBugs surfaced **73 priority-1 findings that the regex sweep could not
possibly find** (priority 1 = HIGH confidence; rank ≤ 12 = troubling or
worse), dominated by dataflow-driven detectors:

| count | type | what it catches |
|---|---|---|
| 60 | `NP_NONNULL_RETURN_VIOLATION` | a method declared `@Nonnull` that can in fact return `null` on some path |
| 5 | `OBJECT_DESERIALIZATION` | `ObjectInputStream.readObject` on untrusted bytes (findsecbugs) |
| 2 | `NP_NONNULL_PARAM_VIOLATION` | passing `null` to a `@Nonnull` parameter |
| 2 | `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` | dereferencing a value that another path proves is `null` |
| 1 | `NP_NULL_ON_SOME_PATH` | same, less symmetric |
| 1 | `NP_NULL_PARAM_DEREF` | a `null` is forced into a non-null parameter |
| 1 | `NP_GUARANTEED_DEREF` | a guaranteed-null deref reached by control flow |
| 1 | `RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE` | nullcheck after deref — the deref already crashed if it would |

## Top 5 NEW priority-1 findings

(One per unique bug type, sorted by rank — picking the smoking-gun
shape over picking 5 NP-violations.)

### 1. `GC_UNRELATED_TYPES` — `Set<String>.contains(Gene)` always false
`gemma-core/.../ArrayDesignAnnotationServiceImpl.java:424` (rank 1).
Line 369 declares `Set<String> genes = new LinkedHashSet<>()`; line 424
calls `genes.contains( g )` with `g` a `Gene` entity. Generic erasure
lets it compile; the runtime check returns false unconditionally, so the
intended duplicate-skip never fires. Annotation files for platforms with
gene-product-level redundancy emit duplicate rows.

### 2. `GC_UNRELATED_TYPES` — `skippableQuantitationTypes.contains(qtIndex)`
`gemma-core/.../geo/model/GeoValues.java:708` (rank 1). The set is keyed
by QT name (String); the loop variable `qType` is the QT index (Integer).
A `// FIXME!` comment on line 707 acknowledges the bug. GEO loads with
skippable QTs never actually skip them — they fall through into the
sample-dimension validator and waste cycles.

### 3. `NP_GUARANTEED_DEREF` — `SimpleDownloader.ftpClientFactory` may be null
`gemma-core/.../SimpleDownloader.java:267` (rank 6). The field is
optional (set via setter); `downloadFtp()` dereferences it without a
null-check. Any FTP retrieval through `SimpleDownloader` without
explicit factory wiring NPEs at runtime — a regression risk for the
CLI path after the recent FTP/HTTP split.

### 4. `RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE` — backwards null-check
`gemma-core/.../GeneSearchServiceImpl.java:294` (rank 9). `taxon` is
dereferenced at line ~294 and then null-checked at line 309. If `taxon`
ever was null, the deref already threw — the guard is dead code AND it
documents an intent the actual control flow violates.

### 5. `OBJECT_DESERIALIZATION` — `ObjectInputStream` on disk-cached report
`gemma-core/.../ArrayDesignReportServiceImpl.java:237` (rank 10,
SECURITY priority 1). `getSummaryObject()` reads a serialized cache file
with `ObjectInputStream.readObject` without a type allow-list filter
(`ObjectInputFilter`). Lower exposure since the file is local — but if
the cache directory is ever shared / mounted from less-trusted storage
the gadget chain risk is real. Five similar findings across the
expression-report layer.

## Notable category totals

* **SECURITY (findsecbugs) — 232 findings.** Dominated by
  `SQL_INJECTION_HIBERNATE` (85), `PATH_TRAVERSAL_IN` (74), and
  `URLCONNECTION_SSRF_FD` (27). The Hibernate hits warrant a closer look
  on the JAX-RS surface (gemma-rest, not yet scanned).
* **`EI_EXPOSE_REP` / `EI_EXPOSE_REP2` — 1 475 combined.** Defensive-copy
  / unmodifiable-wrap nags on getters and setters. Largely noise on
  internal entity classes; not actionable as a blanket policy.
* **`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` — 722.** Priority 2 / rank
  13–18 dominated. Real ones get masked by the volume; sample a few per
  package rather than triaging linearly.
* **`CT_CONSTRUCTOR_THROW` — 114.** Hibernate-3-era anti-pattern (a
  partially-constructed Serializable instance is a finaliser-attack
  vector). Low real-world risk in our deployment; consider class-level
  suppression for `@Entity` types.

## Surprises

* **Zero findings** from popular detectors: `EQ_COMPARING_CLASS_NAMES`
  (string-compare Class.name instead of `==`), `IL_INFINITE_RECURSION`
  (no infinite recursion via direct self-call) — that's the surprise:
  for a ~2 300-file codebase, the absence of any infinite-recursion or
  Class-name-equality bug suggests the equality / dispatch hygiene is
  decent.
* **`SE_BAD_FIELD` — 44.** Entity classes carry non-serializable fields
  (Hibernate proxies, Lucene readers); not actually serialized in
  practice but reachable via Hessian-style endpoints if anyone tries.
* **`WMI_WRONG_MAP_ITERATOR` — 101.** Idiom inefficiency
  (`for (K k : map.keySet()) { v = map.get(k); }` vs `entrySet()`).
  Trivial to migrate; not on the perf hotspot list, but worth a sweep
  before the next 2.0 perf review.
* **`SQL_INJECTION_HIBERNATE` — 85.** Most are inside `*Dao` classes
  using `createQuery("from X where foo = " + parameter)`-style
  concatenation. HQL escapes a lot of what JDBC would let through, but
  the precedent is bad — worth a triage pass before Gemma 2.0.

## What this commit does NOT do

Instrumentation only. No findings fixed in this commit. Subsequent
sessions can pull the top-N priority-1 by file and remediate
incrementally; the report at `gemma-core/target/spotbugsXml.xml`
regenerates on demand.

## Quick re-run

```bash
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl gemma-core spotbugs:spotbugs   # writes target/spotbugsXml.xml
mvn -pl gemma-core spotbugs:gui        # interactive viewer
```

Triage one-liner:
```bash
python3 -c "
import xml.etree.ElementTree as ET
from collections import Counter
bugs = ET.parse('gemma-core/target/spotbugsXml.xml').findall('.//BugInstance')
print(Counter((b.get('priority'), b.get('type')) for b in bugs).most_common(20))
"
```
