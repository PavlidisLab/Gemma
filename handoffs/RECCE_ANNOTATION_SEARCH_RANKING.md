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

### 6. Synonym attribution — required, but smaller than first thought (Paul, 2026-05-23)

**Correction from the first draft of this section:** synonym walking IS already working in `JenaTextOntologySearchService`. Empirically against staging (2026-05-23):

- Query `cornu ammonis` (Latin name for hippocampus, NOT in any preferred label) → returns `hippocampus` as the top hit.
- Query `ammon` → returns both `ammon's horn` (UBERON_0001954) AND `hippocampus` (EMAPA_32845) in the same 18-hit response.

Jena Text indexes synonym properties (`oboInOwl:hasExactSynonym`, `altLabel`, etc.) — query expansion happens at the Lucene layer, not in our code. **What's missing is per-hit attribution**: the UI doesn't know WHY each hit matched.

For Paul's curator UX:
- Query "ammon's horn" → currently surfaces "hippocampus" silently. Curator sees "hippocampus" and wonders "why?"
- Query "hippocampus" → currently surfaces 120 hits including the real hippocampus, but doesn't tell the UI which of those hits matched via synonym vs preferred-label.

#### The actual fix

Add to `AnnotationSearchResultValueObject`:
- `@Nullable String matchedVia` — one of `preferred_label` (default), `exact_synonym`, `narrow_synonym`, `related_synonym`, `alt_label`, `definition`. Whatever the underlying Lucene index field names map to.
- `@Nullable String matchedText` — the actual synonym text that scored the match (so the UI can render "↪ matches synonym 'Ammon's horn'").

#### Where the data comes from

This needs a recce on the Lucene `Highlighter` / `MatchHighlighter` / `Explanation` API exposed by Jena Text. The `JenaTextOntologySearchService` issues a TextDataset query and gets back URIs; to know which Lucene FIELD matched per hit, we either:
1. Run the query in "highlighter" mode and read which field's spans hit.
2. After the search, for each hit URI, re-fetch the term's synonyms and string-match against the original query tokens to back-compute which synonym matched.

(2) is implementable without Jena internals. (1) is cleaner but needs Jena Text's highlighter integration which may or may not be wired in current JenaTextOntologySearchService.

#### Don't fold into the ranker. Same lesson.

The ranker (lucene / usage / coverage / composite) is orthogonal — it reorders results, doesn't touch metadata. Synonym attribution is a separate enrichment stage. Sequence: search → rank → enrich (definition + parents + matchedVia/matchedText for top-N).

#### Out of scope here

If "hippocampus" doesn't rank UBERON_0002421 above EMAPA_32845 (mouse-anatomy hippocampus), that's a SEPARATE issue — namespace-priority ranking. The Jena search returns EMAPA first probably because of which ontology was loaded into the TDB first / index-order; needs its own recce. Not blocking the per-hit attribution work.

#### Cross-references

- `gemma-core/src/main/java/ubic/gemma/core/ontology/search/JenaTextOntologySearchService.java` — Lucene-text wrapper.
- Empirical evidence: probe staging via `curl 'https://staging-gemma.msl.ubc.ca/rest/v2/annotations/search?query=ammon'`, query=`cornu+ammonis`, etc.

### 6b. ORIGINAL (incorrect) framing kept for diff context only

> "when the query is 'hippocampus' synonyms have to be returned like 'ammon's horn' which is actually what we use; then, if the ontology actually says that ammon's horn is a synonym of the query (per the ontology) that has to be surfaced."

Right now `AnnotationsWebService.getTerms` runs pure Lucene token match against preferred term labels. Gemma's `CHARACTERISTIC` table can be annotated with synonym strings ("ammon's horn" — the curator-typed text), and the ontology knows those are `oboInOwl:hasExactSynonym` of `UBERON_0002421` ("hippocampus"). Today neither side bridges these.

#### Two layers, each separable

**6a. Query expansion**. Before searching, expand each query token:
1. For each token, find ontology term(s) whose preferred label matches.
2. Pull each matched term's synonyms via `OntologyTerm.getSynonyms()` (or the equivalent — needs a recce on the basecode/Jena API since the current `OntologyService` doesn't expose synonyms directly per a quick grep; may live on `OntologyTerm` itself or via TDB query).
3. OR the synonyms into the Lucene query — `hippocampus OR "ammon's horn" OR "Ammon horn" OR "cornu ammonis"` etc.

The TDB-backed `JenaTextOntologySearchService` may already index synonym text on the term entries (Jena Text supports `multilingual` and `analyzer-per-property` configs). Recce first: does `?query=ammon's+horn` already find UBERON_0002421 today? If yes, query expansion may be cheaper than it sounds — Lucene already knows about synonyms, we just need to FIND the synonym-set per query token rather than asking the user to type each variant.

**6b. Per-hit synonym annotation**. For each hit, attach metadata that lets the UI explain WHY this hit matched:
- New field on `AnnotationSearchResultValueObject`: `@Nullable String matchedVia` — one of `preferred_label` (default), `exact_synonym`, `narrow_synonym`, `related_synonym`, `alt_label`.
- New field: `@Nullable String matchedText` — the actual synonym text that matched (so the UI can render "↪ via synonym 'ammon's horn'").
- Computation: for each hit, walk the hit's term against each query token; if the preferred label matches, `matched_via=preferred_label`; otherwise check the term's synonyms and pick the one with the closest match.

For the typeahead UX Paul described: a hit like `UBERON_0002421 hippocampus` matched by query "ammon's horn" should render the preferred label "hippocampus" prominently with a secondary line "↪ matches synonym 'Ammon's horn' (exact_synonym)".

#### Don't fold into the ranker. Don't fold into the ranker.

The ranker (lucene / usage / coverage / composite) is orthogonal — it reorders whatever results the SEARCH stage produces. The synonym walk is in the SEARCH stage (Lucene query expansion). Two distinct PRs:

1. Query expansion + per-hit synonym annotation — touches `AnnotationsWebService.getTerms` (search call) and `JenaTextOntologySearchService` (if Lucene index needs synonym fields added).
2. Optional follow-up: a `SynonymWeightedRankingStrategy` that downranks pure-synonym matches relative to preferred-label matches (curator preference). But that's debatable — synonym matches ARE the desired hits sometimes ("ammon's horn" usage is real Gemma data).

#### Cross-references for this section

- `gemma-core/src/main/java/ubic/gemma/core/ontology/search/JenaTextOntologySearchService.java` — Lucene-text wrapper; check what properties are indexed.
- `OntologyTerm.getSynonyms()` — confirm signature; baseCode side.
- Paul's "hippocampus / ammon's horn" framing → typical curator-side mental model is: typeahead helps me find the term Gemma's data is already tagged with. Synonyms ARE the bridge.

## What NOT to do

- Don't change the default sort. The UI's contract is "Lucene relevance," and breaking it without an opt-in flag will surprise downstream consumers.
- Don't pre-compute usageCount on the wrong side. It's already wired via `characteristicService.findExperimentsByUris` — don't duplicate.
- Don't hit the Jena TDB or Lucene index in unit tests. The fixture is the test corpus; the ranker is pure data transform.

## Cross-references

- `/Users/pzoot/Dev/eclipseworkspace/Gemma/gemma-rest/src/main/java/ubic/gemma/rest/AnnotationsWebService.java:547` — `getTerms` is where the ranker plugs in.
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/gemma-rest/src/test/resources/data/annotations/search-chronic-itch.staging-2026-05-23.json` — the regression baseline fixture.
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/gemma-core/src/main/java/ubic/gemma/core/ontology/search/JenaTextOntologySearchService.java` — current Lucene-backed search; ranker work happens above this, not inside.
- Paul's curation UI typeahead (screenshot 2026-05-23) — the original report.
