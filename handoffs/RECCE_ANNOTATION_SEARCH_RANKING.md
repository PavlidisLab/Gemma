# Recce — annotation-search ranking

**Filed:** 2026-05-23, post-perf-wave session.
**Status:** Fixture landed; ranker interface + tests + endpoint param queued.
**Owner:** Whoever picks up the next search-side session.

## Why this exists

Paul's curation UI typeahead for `chronic itch` against `staging-gemma.msl.ubc.ca/rest/v2/annotations/search` ranks the way you'd expect from a pure BM25 endpoint:

```
liver fibrosis (chronic viral hepatitis) venous blood platelet count assay
chronic allograft damage index score
chronic rhinosinusitis without nasal polyps
chronic post-operative pain measurement
chronic myelogenous leukemia cell line
chronic mountain sickness
multisite chronic pain
chronic lung disease
chronic human papillomavirus infection
chronic eosinophilic leukemia, not otherwise specified
chronic disease
…
```

"Pruritus" / "itch" / "antipruritic" terms exist in MP and ChEBI and are loaded on staging — confirmed earlier in the session via `/annotations/term?uri=…MP_0010073` returning `decreased pruritus`. They just rank low because:

- The query splits to {`chronic`, `itch`}.
- `chronic` is a dominant token across MONDO (chronic-* disease) + PATO (`chronic` qualifier) + OBI (`chronic *` measurements).
- `itch` is barely tokenised in any term's label — synonyms (`pruritus`) aren't substring-matched by the current Lucene posture.
- Lucene-only scoring → "chronic" pulls all matches up; "itch" hits live deep.

We also already compute `usageCount` per hit (commit 101 in the curation API wave) but it's purely informational — `AnnotationsWebService.getTerms` (line 547 of `gemma-rest/src/main/java/ubic/gemma/rest/AnnotationsWebService.java`) attaches `usageCount` to the response but returns a `LinkedHashSet` that preserves the raw Lucene order. The UI gets a 435-element list where 88% have `usageCount=0` and the most clinically interesting term is page 4.

## What's already shipped (this session)

`gemma-rest/src/test/resources/data/annotations/search-chronic-itch.staging-2026-05-23.json`
— a frozen snapshot of staging's response (435 hits, with `_meta` provenance and a per-hit `lucene_rank` 0-based index). This is the regression baseline for any ranker work: any new strategy must be able to consume this list and produce a different (better) ordering, and we can assert exact orderings against fixed expected positions.

## What still needs to ship

### 1. `AnnotationSearchRankingStrategy` interface

Inserted into `AnnotationsWebService.getTerms` between the existing `countsByUri` computation and the `LinkedHashSet` collection step. Sketch:

```java
public interface AnnotationSearchRankingStrategy {
    /**
     * Re-order rawHits. Implementations get the query string, the raw
     * Lucene-ordered hits (with their original index implicit in list
     * position), and a per-URI usage count map. Return a new list in the
     * desired display order. Must be stable + side-effect-free.
     */
    List<CharacteristicValueObject> rank(
            String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri);
}
```

`LuceneOrderRankingStrategy` is the no-op default — returns `rawHits` unchanged. Wire it as the `@Bean` for the strategy unless an override property is set. This is regression-safe.

### 2. Alternative strategies (mock-testable, no dependencies on Jena/Lucene/DB)

- **`UsageWeightedRankingStrategy`** — score = `(rank_weight * 1/(1 + originalRank)) + (usage_weight * log(1 + usageCount))`. Tunable weights via `@Value`. Pulls used terms forward without abandoning relevance order entirely.

- **`TokenCoverageRankingStrategy`** — tokenise the query, compute fraction-of-tokens-covered for each hit's label; primary sort by coverage DESC, secondary by original rank. Bumps "chronic itch" exact match (if any) to the top; pushes "chronic *" with no itch substring down.

- **`CompositeRankingStrategy`** — coverage primary, usage secondary, original rank tertiary. Probably what we actually want, but ship simple ones first and benchmark against the fixture.

### 3. Unit tests — pure JUnit + Mockito, no Spring

Per Paul: "for sorting of results, a mock is all needed."

Tests at `gemma-rest/src/test/java/ubic/gemma/rest/AnnotationSearchRankingStrategyTest.java`. Pattern:

```java
class AnnotationSearchRankingStrategyTest {
    // Load the fixture once into a List<CharacteristicValueObject> + Map<String,Integer> usageCounts.
    private static List<CharacteristicValueObject> hits;
    private static Map<String, Integer> usageCounts;

    @BeforeAll static void loadFixture() throws IOException {
        // Read search-chronic-itch.staging-2026-05-23.json from resources, deserialise the 'hits' array.
    }

    @Test
    void luceneOrder_keepsRawOrder() {
        var ranked = new LuceneOrderRankingStrategy().rank("chronic itch", hits, usageCounts);
        // Asserts first 10 are exactly the fixture's lucene_rank 0..9.
    }

    @Test
    void usageWeighted_chronicItch_floatsPrurirusOrCommonTermsHigher() {
        var ranked = new UsageWeightedRankingStrategy(rankW, usageW).rank("chronic itch", hits, usageCounts);
        // Find the position of the highest-usageCount term in the new list. Assert it's earlier than its
        // position in the fixture (lucene_rank). Don't pin exact numbers — pin "moved forward".
    }

    @Test
    void tokenCoverage_chronicItch_termsWithBothTokensFirst() {
        // If/when a real 'chronic itch'-containing term exists, asserts it ranks above 'chronic foo'.
        // Today no such term in the fixture; the test should at minimum assert "pruritus" / "itch" /
        // "antipruritic" terms move forward when coverage counts a synonym dictionary [see below].
    }
}
```

### 4. Endpoint parameter

`AnnotationsWebService.searchAnnotations` gains an optional `?rank=lucene|usage|coverage|composite` (default: `lucene` for back-compat). Resolves via a `Map<String, AnnotationSearchRankingStrategy>` so adding a new strategy is one bean. Document in the OpenAPI block.

### 5. Enrich search hits with `definition` + `parentLabel(s)` (Paul, 2026-05-23)

The current `/annotations/search` response carries `value`, `valueUri`, `category`, `categoryUri`, `usageCount` — no `definition`, no parent. Paul wants both surfaced so the typeahead can disambiguate near-name terms ("chronic disease" vs "chronic kidney disease") via the term's definition and its nearest is-a / part-of parent.

**Surface exists.** `gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyService.java`:
- `getDefinition(String uri, long timeout, TimeUnit)` — returns the obo:IAO_0000115 / rdfs:comment text.
- `getParents(Collection<OntologyTerm>, boolean direct, boolean includeAdditionalProperties, long timeout, TimeUnit)` — `direct=true` gives the nearest parents only; `includeAdditionalProperties=true` walks `part_of` (BFO:0000050) alongside the default `rdfs:subClassOf`. The combo is exactly "nearest is_a OR part_of".

**Shape change.** Add to `AnnotationSearchResultValueObject`:
```java
@Nullable String definition;
@Nullable List<TermRef> parents;   // each {uri, label}; null = not enriched (lazy-load via /annotations/term)
```
`TermRef` is a tiny record/value-class; reuse `OntologyTermSimpleValueObject` (already exists in this file, lines ~615) if its shape fits.

**Cost mitigation — top-N only.** 435 hits × 2 lookups each is too much per typeahead keystroke. Enrich only the top 25 hits inline; leave `definition=null, parents=null` for the rest as a sentinel the UI can use to trigger lazy-load via `/annotations/term?uri=X`.

```java
List<CharacteristicValueObject> topN = rawHits.subList(0, Math.min(25, rawHits.size()));
Set<String> topUris = topN.stream()
    .map(CharacteristicValueObject::getValueUri).filter(Objects::nonNull)
    .collect(Collectors.toSet());
Map<String, String> defByUri = batchGetDefinitions(topUris, remainingTimeoutMs);
Map<String, List<OntologyTermSimpleValueObject>> parentsByUri = batchGetDirectParents(topUris, remainingTimeoutMs);
// Then in the merge loop: if vo.valueUri in topUris, attach def + parents; otherwise leave null.
```

`batchGetDefinitions` and `batchGetDirectParents` are thin helpers that call `OntologyService` once with the full set rather than per-URI (the existing service API takes collections).

**Lazy-load endpoint already exists.** `/annotations/term?uri=X` (`AnnotationsWebService:186`) already returns definition + label + obsolete + usageCount. The UI's hover-fetch path is one round-trip. Add `parents: List<OntologyTermSimpleValueObject>` to the existing `OntologyTermValueObject` (lines ~607-613) for consistency with the search VO.

**Mock-test alongside the ranker.** Same fixture pattern — pre-canned definition strings and parent lists per URI; the test asserts that the top-N positions carry non-null `definition` and `parents`, the rest carry nulls, and the lazy-load endpoint returns the same shape.

**What NOT to do.** Don't pre-resolve definitions in `OntologyService.findExperimentsCharacteristicTags` itself — keep ontology-search and metadata-enrichment as separate stages so the ranker can reorder before enrichment happens.

### 6. Optional — synonym walk for query expansion

Out of scope for the first iteration, but the real win for "chronic itch" requires the search to recognise `itch ≈ pruritus`. Options:
- Walk the in-memory ontology service for `exactSynonym` / `relatedSynonym` annotations and OR them into the Lucene query.
- Pre-build a synonym dictionary file and feed it to Lucene as a `SynonymGraphFilter`.

Both are real work. Mention in the recce, don't try to do them in the first ranker PR.

## What NOT to do

- Don't change the default sort. The UI's contract is "Lucene relevance," and breaking it without an opt-in flag will surprise downstream consumers.
- Don't pre-compute usageCount on the wrong side. It's already wired via `characteristicService.findExperimentsByUris` — don't duplicate.
- Don't hit the Jena TDB or Lucene index in unit tests. The fixture is the test corpus; the ranker is pure data transform.

## Cross-references

- `/Users/pzoot/Dev/eclipseworkspace/Gemma/gemma-rest/src/main/java/ubic/gemma/rest/AnnotationsWebService.java:547` — `getTerms` is where the ranker plugs in.
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/gemma-rest/src/test/resources/data/annotations/search-chronic-itch.staging-2026-05-23.json` — the regression baseline fixture.
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/gemma-core/src/main/java/ubic/gemma/core/ontology/search/JenaTextOntologySearchService.java` — current Lucene-backed search; ranker work happens above this, not inside.
- Paul's curation UI typeahead (screenshot 2026-05-23) — the original report.
