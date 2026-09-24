# Gemma's ontology subsystem — a manifest

**What this is.** An inventory of what Gemma does with ontologies: which vocabularies it
loads, what a query goes through between arriving and coming back ranked, and every switch
that changes either. Written as a manifest rather than a narrative — the intended use is
"I am about to change X, what reads it" and "the client got Y back, which step did that".

**How to check it.** Every claim cites `file:line`. Derived at commit `cdf49196b4`
(2026-08-18) by inventorying the code, so line numbers drift; the file and symbol names
are the durable part. Where a fact was measured rather than read, the measurement is
stated with its corpus. `NOT FOUND` means looked for and absent, not "assumed absent".

Companion doc: [`ONTOLOGY_SUPPLEMENTARY_METHODS.md`](ONTOLOGY_SUPPLEMENTARY_METHODS.md),
the same subsystem written as paper Methods prose.

---

## 1. What is loaded

Spring wiring: `gemma-core/.../ontology/OntologyConfig.java` (`@Profile({"!test"})`, `:36`).
Two property mechanisms are in play and they are **not** interchangeable — see §5.

| Vocabulary | Class | Kind | URL property | Enable property | Default |
|---|---|---|---|---|---|
| Gemma Unified (TDB) | `TdbOntologyService` | on-disk TDB | — (`gemma.ontology.unified.tdb.dir`) | `gemma.ontology.unified.enabled` | off |
| TGEMO | `GemmaOntologyService` | URL | `url.gemmaOntology` | `load.gemmaOntology` | **on** |
| Gene Ontology | `GeneOntologyServiceImpl` | URL | `url.geneOntology` | `load.geneOntology` | off |
| EFO | `ExperimentalFactorOntologyService` | URL (`efo-base`) | `url.efOntology` | `load.experimentalFactorOntology` | off |
| OBI | `ObiService` | URL | `url.obiOntology` | `load.obiOntology` | off |
| CL | `CellTypeOntologyService` | URL (`cl-base`) | `url.cellTypeOntology` | `load.cellTypeOntology` | off |
| CHEBI | `ChebiOntologyService` | URL + slimmable | `url.chebiOntology` | `load.chebiOntology` | off |
| MONDO | `MondoOntologyService` | URL + slimmable | `url.mondoOntology` | `load.mondoOntology` | off |
| PATO | `PatoOntologyService` | URL | `url.patoOntology` | `load.patoOntology` | off |
| MP | `MammalianPhenotypeOntologyService` | URL (`mp-base`) | `url.mammalPhenotypeOntology` | `load.mammalPhenotypeOntology` | off |
| HP | `HumanPhenotypeOntologyService` | URL (`hp-base`) | `url.humanPhenotypeOntology` | `load.humanPhenotypeOntology` | off |
| EMAPA | `MouseDevelopmentOntologyService` | URL | `url.mouseDevelOntology` | `load.mouseDevelOntology` | off |
| SO | `SequenceOntologyService` | URL | `url.seqOntology` | `load.seqOntology` | off |
| CLO | `CellLineOntologyService` | URL | `url.cellLineOntology` | `load.cellLineOntology` | off (no declared default) |
| UBERON | `UberonOntologyService` | URL (`uberon-base`) | `url.uberonOntology` | `load.uberonOntology` | off |
| NBO | `NeuroBehaviorOntologyService` | URL | `url.neuroBehaviorOntology` | `load.neuroBehaviorOntology` | off |
| GENO | `GenotypeOntologyService` | URL | `url.genotypeOntology` | `load.genotypeOntology` | off |
| Cellosaurus | `CellosaurusOntologyService` | **lexical** | `url.cellosaurus` | `load.cellosaurus` | off |
| MGI strains | `MgiStrainOntologyService` | **lexical** | `url.mgiStrain` | `load.mgiStrain` | off |
| MeSH disease synonyms | `MeshDiseaseSynonymOntologyService` | **lexical**, classpath | — | `load.meshDiseaseSynonyms` | off |

Live on gemma2: 20 beans, 19 loaded (TDB is not). `GET /admin/ontologies` is the authority.

**Lexical vs ontology.** Cellosaurus, MGI and the MeSH synonym table are flat catalogues
of names + synonyms with no subsumption hierarchy, served through
`AbstractLexicalOntologyService` rather than Jena. They are `isSupplementary()` — their
hits are ranked below conventional ontology hits rather than merged by score, because each
source scores against its own Lucene index and the numbers are not on a common scale
(`providers/OntologyService.java:172-185`).

**Dead classes.** `MedicOntologyService`, `HumanDevelopmentOntologyService`,
`UnitsOntologyService` have no bean anywhere; their `load.*` / `url.*` keys still exist, so
setting them does nothing.

**Not loaded, consulted over the network.** OLS (`gemma.ols.baseurl`, always on) resolves
URIs Gemma has not loaded; ChEMBL (`gemma.chembl.enabled`, off) resolves trial codes.

---

## 2. The resolver — ordered pipeline

What `GET /annotations/search` does. `AWS` = `gemma-rest/.../AnnotationsWebService.java`,
`OSI` = `gemma-core/.../ontology/OntologyServiceImpl.java`.

| # | Step | Where | Can it drop rows? |
|---|---|---|---|
| 1 | Split `query` on commas; optional base64-gzip decode | `util/args/AbstractArrayArg.java:72` | splits one query into N |
| 2 | Coalesce legacy aliases (`exact_label`, `suppress_near_matches`) | `AWS:1325-1326` | no |
| 3 | Validate (empty query, `limit ∉ [1,100]`, unknown `rank`, …) → 400 | `AWS:1327-1341` | rejects |
| 4 | Resolve `taxon` | `AWS:1344` | no |
| 5 | **Response-cache lookup** — a hit skips steps 6-22 entirely | `AWS:1382-1400` | serves frozen results |
| 6 | Dispatch: URI/CURIE lookup · `upstream` · free text | `AWS:1838-1867` | URI path bypasses lexical search |
| 7 | **Minimum query length** — under 2 chars returns empty, silently | `OSI:235-236` | drops everything |
| 8 | Corpus leg: prefix-`LIKE` over `CHARACTERISTIC`, limit 1000 | `OSI:243-256`, `:1413-1419` | no |
| 9 | Ontology fan-out, parallel, one shared 30 s deadline | `OSI:1041-1064` | drops any ontology that misses the budget |
| 10 | Per-index Lucene query; min-should-match 0.67 | `OntologyQueries.java:61-87` | drops docs under the clause floor |
| 11 | **Analysis / code folding** (§8) | `OntologyAnalyzers.java:134` | — |
| 12 | Scoring: Lucene BM25 default, no custom similarity, **no score threshold** | — | no |
| 13 | Per-provider dedup + `OntologyCache` truncation | `OntologyCache.java:60-96` | truncates per ontology |
| 14 | Cross-ontology merge, sorted score-desc then URI | `OSI:1067-1074` | drops corpus duplicates |
| 15 | GO fallback appended **un-scored** | `OSI:1081-1091` | — |
| 16 | Four-bucket relevance sort (exact ≺ startsWith ≺ contains ≺ none) + **candidate cut at 1000** | `OSI:268-314` | anything past 1000 is unrankable |
| 17 | REST relevance tiers (`tierFn`, prefix rank, cell-line preference, supplementary demotion) | `AWS:2006-2013` | reorders only |
| 18 | Candidate attribution for the first 200 distinct URIs | `AWS:3012-3062` | tail left unattributed |
| 19 | Synonym-exact lift | `AWS:2049-2065` | reorders only |
| 20 | Near-match suppression (`suppressNearMatches` **and** designation-shaped query) | `AWS:2139-2170` | **drops** |
| 21 | Category exclusion (deny-list namespaces) | `AWS:2171-2208` | **drops** |
| 22 | Negative-evidence assembly, optional ChEMBL bridge | `AWS:2209-2220` | — |
| 23 | Category promotion (preferred namespaces) | `AWS:2221-2240` | reorders only |
| 24 | `exactLabel` filter | `AWS:2247-2277` | **drops** |
| 25 | `prefixes` allow-list filter | `AWS:2279-2293` | **drops**, incl. all free-text rows |
| 26-27 | Usage counts / string prior, if the strategy needs them | `AWS:2302-2331` | — |
| 28 | **Ranking strategy** (§7) | `AWS:2337-2349` | reorders only, by contract |
| 29 | Re-apply category promotion + exact tier after ranking | `AWS:2357-2373` | reorders only |
| 30 | Unused-derivative demotion | `AWS:3332-3357` | partitions |
| 31 | **Truncate to `limit`** | `AWS:2379-2382` | **drops** |
| 32-33 | Display usage counts, prior-category breakdown | `AWS:2390-2425` | — |
| 34 | **Enrich top 25**: definition, parents, matchedVia, source metadata, taxon | `AWS:2920-2985` | degrades fields to null |
| 35 | VO assembly, label-attribution fallback for rows past the top 25 | `AWS:2460-2500` | — |
| 36 | **Gene merge** — strong symbol/name hits **prepended above every ontology hit**, alias hits appended; re-truncated to `limit` | `AWS:2516-2562` | **reorders and drops** |
| 37-38 | Gene counts, example usage, `priorCuration` | `AWS:1403-1414` | — |
| 39 | Populate response cache — **only if non-empty** | `AWS:1416-1432` | — |

Two things worth internalizing: **`limit` is enforced twice**, against different
populations (step 31, then step 36 after genes are merged); and **`rank=lucene` being a
no-op does not mean raw Lucene order reaches the client** — steps 16-19, 23, 29, 30 have
all reordered it first.

---

## 3. Switches — REST parameters

`GET /annotations/search`, exhaustive.

| Parameter | Values | Default | Affects |
|---|---|---|---|
| `query` | text, term URI, CURIE, or base64-gzip; comma-splits | — (empty ⇒ 400) | 1, 6 |
| `rank` | `lucene` · `usage` · `coverage` · `composite` · `commonality` | `lucene` | 26-28 |
| `limit` | 1-100 | 20 | 31, 36 |
| `prefixes` | URI substrings, order significant (`CL_,EFO_`) | — | 17, 23, 25 |
| `upstream` | boolean | false | 6 |
| `exactLabel` / `exact_label` | boolean | false | 24 |
| `category` | label, EFO URI, or any case/separator variant | — | 21, 23 |
| `suppressNearMatches` / `suppress_near_matches` | boolean | false | 20 |
| `taxon` | common/scientific name, NCBI id, Gemma id | — | **genes only** |
| `includeGenes` | boolean | **true** | 36 |
| `includeGeneCount` / `geneCountMaxTerms` | boolean / int (0 = unbounded) | false / 50 | 37 |
| `includeExampleUsage` | boolean | false | 37 |
| `excludeExperiments` | dataset ids or GSE accessions | — | 26, 27, 38; bypasses the usage-count cache |
| `includePriorCuration` | boolean | false | 38 |

Siblings: `POST /annotations/search/batch` (≤200 items, no `upstream` /
`excludeExperiments` / `includePriorCuration`, body is not comma-split);
`GET /annotations/term`; `GET /annotations/parents|children` (`direct`, no ranking or
limit); `GET /annotations/search/{query}` (deprecated, hardcoded).

**`taxon` does not constrain ontology hits.** It filters the gene fan-out only.

---

## 4. Switches — ranking strategies

All implement `AnnotationSearchRankingStrategy`, whose contract is **reorder-only**; no
strategy drops anything. Every drop in the pipeline is outside the strategy.

| `rank=` | Formula | Fetches extra data? |
|---|---|---|
| `lucene` (default) | none — returns a defensive copy, order unchanged | no |
| `usage` | `0.5·(1/(1+i)) + 0.3·presence(usage)` | usage counts |
| `coverage` | matched query tokens / total, by substring against the label only | no |
| `composite` | `0.5·coverage + 0.3·log-scaled usage + 0.2·(1/(1+i))` | usage counts + matched text |
| `commonality` | `0.35·(1/(1+i)) + 0.65·prior − 0.25·designation-shape penalty` | per-string prior |

All four scoring strategies tie-break on the incoming order (stable). `usage` cannot
displace the rank-0 hit, because its usage weight (0.3) is below its rank weight (0.5) —
the class javadoc records that raising it turns the ranker into a partition.

---

## 5. Switches — configuration

**The prefix trap.** Two mechanisms resolve properties and the `-D` rule is opposite in each:

* **Gemma `Settings` / `@Value`** (`config/SettingsConfig.java`) — `-Dgemma.load.X=true`
  works; a bare `-Dload.X=true` is **logged at WARN and dropped** (`:313-318`).
* **`Configuration`** (`config/Configuration.java`, backs `basecode.properties`) — a bare
  `-Dload.chebiOntology=true` **does** work (`:83`), but in a *file* it must be written
  `basecode.load.chebiOntology=true` or `BaseCodeConfigurer` warns and ignores it (`:57-59`).

| Property | Effect | Default |
|---|---|---|
| `load.ontologies` | master auto-load-at-startup gate; with it off nothing loads regardless of its own flag | false |
| `load.<name>Ontology` (18) | per-vocabulary enable | off except TGEMO |
| `url.<name>Ontology` (16) | source URL | see §1 |
| `ontology.cache.dir` | downloaded sources, slims, sidecars | `${gemma.cache.dir}/ontologyCache` |
| `ontology.cache.pinned` | read cached bytes, never open a connection (bypassed by `forceLoad`) | false |
| `gemma.ontology.validation.timeout.ms` | per-characteristic budget for commit-time URI resolution | 8000 |
| `gemma.ontology.validation.olsFailClosed` | an unverifiable URI blocks the commit | **true** |
| `gemma.search.inferredRelations.maxTermsPerSeed` | relation expansion cap per seed | 5 |
| `gemma.search.inferredRelations.minSpecificity` | CORPUS-basis specificity floor | 0 |
| `gemma.search.inferredRelations.maxObjectBreadth` | drop relations whose object is too broad | 25 |
| `gemma.search.ontology.timeoutMs` | ontology search budget | 10000 |

`ontology.index.dir` / `basecode.ontology.index.dir` are **inert** — no code reads them.

---

## 6. Constants and thresholds

| Name | Value | Gates |
|---|---|---|
| minimum query length | **2** (was 3 until `9f3106350e`) | `OSI:235`; silent empty below it |
| `maxResults` | 1000 | DB `LIKE` limit, per-ontology Lucene limit, and the candidate cut — one literal, three jobs |
| `DEFAULT_MIN_SHOULD_MATCH` | 0.67, floor 2 clauses | `OntologyQueries.java:33` |
| `EXACT_MATCH_BOOST` | 100f | lexical indexes only; the only boost in the stack |
| `FIND_CHARACTERISTICS_TIMEOUT_MS` | 30000 | whole request; exhaustion ⇒ 503 + `Retry-After` |
| `SEARCH_DEFAULT_LIMIT` / `SEARCH_MAX_LIMIT` | 20 / 100 | `?limit` |
| `CANDIDATE_ATTRIBUTION_CAP` | 200 | distinct URIs attributed |
| `ENRICH_TOP_N` | 25 | rows getting definition/parents/metadata |
| response cache | 500 entries, 30 min | empties never cached |
| score threshold | **none** | scores order, they never cut |

---

## 7. Text analysis

One analyzer factory, `OntologyAnalyzers.english(Set)`, and the **same instance** goes to
both the `IndexWriter` and the `QueryParser` in both index stacks. That symmetry is what
makes the folding below safe: every transform applies to the query and the index alike, so
nothing that matched before stops matching.

Chain: two `PatternReplaceCharFilter`s → stock `EnglishAnalyzer` (StandardTokenizer,
possessive, lowercase, Lucene's default English stop set, keyword marker, Porter stem).
Stem exclusions are exactly two words, `connective` and `connectivity`. No ASCII folding.

**Separator folding.** Submitters write `SU11248`; CHEBI stores `SU-11248`. The tokenizer
splits on the separator, so the two never meet. Two patterns fold them together:

```
CODE_RUN        (?i)\b([a-z]{1,5})[\s-]?([0-9]+(?:-[0-9]+)+|[0-9]{3,})\b
SHORT_CODE_RUN  (?i)(?<!\S)([a-z]{1,3})[\s-]([0-9]{1,2})(?!\S)
```

The second one's bounds are the interesting part, because **a one-digit run is also what
IUPAC uses for a locant**: `MEC-1 cell` and `2-(1H-indol-3-yl)ethanamine` are the same
shape, and CHEBI is 713k of the 1.53M labels+synonyms across the loaded ontologies. So the
bound is positional, not numeric — a locant is always welded mid-chain, a designation
stands alone:

* **whitespace-delimited both sides** excludes `indol-3-yl`, `pregn-4-ene`, and `EC 1.5.1.3`
  (digit followed by a period);
* **prefix ≤3 chars** (vs 5 above) excludes `type 2`, `grade 3`, `group 1`, `class 2`,
  `alpha-2` while admitting `IL`, `CD`, `AE`, `MEC`, `EMT`, `PAX`.

Measured over those 1,530,566 strings, the two together touch **8,156 (0.53%)** — 728 in
CLO, all cell lines, and 1,201 in CHEBI, all trial codes. Dropping the digit floor without
the positional bound instead rewrites **27% of CHEBI**. 429 welds are genuinely ambiguous
(`of 6`, `and 49`) and are left in, because excluding them needs a stop list that would
also have to exclude `a`, and `A 72 cell` is a real CLO cell line.

**Retrieval vs ranking — different stages, do not conflate.** The analyzer above decides
what Lucene *retrieves*. `AnnotationsWebService.canonicaliseForExactMatch` (lowercase,
collapse whitespace, strip a trailing `" cell"`/`" cell line"`, delete hyphens) decides
*ranking and filtering* over rows already returned. It cannot recover a document the index
did not retrieve — which is why the `MEC1` fix had to be in the analyzer, not the ranker.
A third normalizer, `normaliseForEquality`, converts hyphens to spaces rather than deleting
them; the two are not interchangeable.

**Changing a pattern needs a restart, not a reindex.** Ontology Lucene indexes are never
persisted — `OntologyIndexer.getSubjectIndex` returns null unconditionally and both
builders allocate a `ByteBuffersDirectory`, so the rebuild predicate
(`forceReindexing || changed || sourceChanged || !indexExists`) is always satisfied by its
last term and no index outlives the JVM. `refresh?forceIndexing=true` is for a changed
**source**, where the disk-cached copy would be reused across a restart and leave the model
stale. Same symptom, different cause.

---

## 8. The write side

**Model.** `Characteristic` and `Statement` share one table (`SINGLE_TABLE`); a `Statement`
*is* a `Characteristic` whose subject is the inherited `value`/`valueUri`, carrying up to
**two** predicate/object pairs and no more. Annotations hang off `Investigation` (dataset
tags), `BioMaterial` (sample characteristics), `ExperimentalFactor` (its category **is** a
`Characteristic`) and `FactorValue` (statements).

`originalValue` is **the raw source line, not the previous label** — GEO stores
`"tissue: Hypothalamus"` there while `value` becomes `"Hypothalamus"`, and for many rows
the two are simply equal. Presence is not evidence of curation.

**EE2C** (`EXPRESSION_EXPERIMENT2CHARACTERISTIC`) is an ACL-aware denormalized projection
of every annotation onto its experiment, carrying a per-row anonymous-permission bitmask so
anonymous reads never join the ACL tables. Refreshed by Quartz on weekday evenings and by
`updateEe2c`; it is an **upsert**, which cannot fix or delete non-winner rows — 1,008 rows
survived a full refresh on prod. Its incremental predicate is the experiment's
`lastUpdated`, not the characteristic's.

**Controlled vocabularies.** `EFO.factor.categories.txt` (28 active entries) defines
categories; `Relation.terms.txt` (29) defines predicates. Both are tab-separated
`URI<TAB>label`. Enforcement differs by path: the design-file importer **rejects** an
unknown category, while the REST validator treats both files as an authoritative *label
source* and falls through to loaded ontologies then OLS for anything not listed. Values are
unconstrained by design.

**Write-time validation** runs on exactly one path — the composite
`PUT /datasets/{id}/curation` and its preflight (and restore, which replays through the
same commit). Per slot: skip blank URIs; skip allow-listed non-ontology prefixes; canonicalize
Gemma-owned URIs; resolve the label from Gemma's vocabulary → loaded ontologies → OLS; then
compare labels, filling a blank one in, accepting a case/whitespace variant, and rejecting a
genuine mismatch. Every failing slot in the request is accumulated and returned as one
**400** with a body path per violation. `PUT /datasets/{id}/annotations`, the sample
characteristic endpoints, `PUT /design`, GEO import and every CLI are **exempt** — a
hallucinated term passes design preflight and fails only at `PUT /curation`.

---

## 9. Derived relations

`ANNOTATION_RELATION` holds `(subject, predicate, object)` triples that are **not**
annotations — nothing in it has been written onto any experiment. One row per triple **per
basis**; corroboration is a read-time aggregation, never stored. There is deliberately no
UNIQUE constraint (MySQL permits duplicate NULLs, so an upsert would silently insert), and
every producer therefore deletes-then-inserts within its own scope.

| Basis | Asserted by | Sources |
|---|---|---|
| `CURATED` | Gemma curators, harvested from their statements | `Gemma` |
| `ONTOLOGY` | a loaded ontology's axioms | `CLO`, `CHEBI` |
| `EXTERNAL` | a third party | `MGI`, `CELLOSAURUS` |
| `CORPUS` | computed from co-occurrence; never self-sufficient | — (no writer today) |

**Readable from both ends, inferable from one.** Direction is a property of the predicate,
not of the query: `RelationInferenceDirection` classifies each into
`SUBJECT_IMPLIES_OBJECT`, `OBJECT_IMPLIES_SUBJECT` or `NEITHER`, closed by default. A
`derives from` row licenses nothing until its object is typed.

Two operational facts that have each cost a day: MONDO's xref index must be built from the
**source artifact, never a slim** (a corpus-seeded slim holds exactly the wrong terms —
32,594 xrefs vs 145,917); and after any rebuild the query cache must be flushed through
`DELETE /admin/caches/...` because the CLI runs in a different JVM from gemma-rest.

---

## 10. Caches and lifecycle

`initialize(forceLoad, forceIndexing)`: `forceLoad` overrides both the already-loaded
short-circuit and the disabled check, and bypasses `ontology.cache.pinned`. `forceIndexing`
is currently **inert** (§7 — every load reindexes anyway).

`POST /admin/ontologies/{name}/refresh?forceIndexing=` hardcodes `forceLoad=true`, returns
202, and on completion evicts the ontology-keyed caches and drops the whole
`/annotations/search` response region. `{name}` accepts the abbreviation, identifier, class
name or `dc:title`. NBO, GENO and the MeSH synonyms have no abbreviation and are reachable
only by identifier or class name.

| Cache | Holds | Size / TTL |
|---|---|---|
| `OntologyService.search` | per-(ontology, query, maxResults) hits | 5k / 1 h |
| `OntologyService.parents` / `.children` | hierarchy walks | 10k / 6 h each |
| `AnnotationsSearchResponseCache` | whole search responses | 500 / 30 min |
| `AnnotationsUsageCountCache` | per-(readScope, URI) counts | 200k / 12 h |
| `OlsTermResolver.terms` / `ChemblCodeResolver.compounds` | remote lookups | 10k / 12 h, 24 h |

All are listed and flushable through `GET|DELETE /admin/caches[/{name}]`. Do not add
bespoke per-endpoint evict routes; two were retired for duplicating this surface.

---

## 11. Traps

Collected because each has cost real time. Full citations in the sections above.

1. **A bare `-Dload.X=true` is silently dropped** for Gemma-mechanism properties and
   honoured for baseCode ones. In a file the rule inverts.
2. **`category` on a search row is usage-derived** — it reports the category a curator
   already applied to that URI, not what kind of thing the term is. An unused CLO cell line
   is null exactly like an unused Cellosaurus row. Absent means unused, in every vocabulary.
3. **The response cache key omits the caller's ACL scope** while the `usageCount` values
   baked into the payload are ACL-scoped.
4. **Strong gene hits are prepended above every ontology hit**, after ranking and after
   truncation. A gene row does not merely appear — it displaces.
5. **The disease category is an obsolete EFO term.** `getCategoryTerms()` substitutes the
   live EFO term when EFO is loaded, so `EFO_0000408` resolves to the label
   `obsolete_disease` while ~15k corpus annotations still sit on that URI.
6. **`UNCATEGORIZED` is a random string regenerated every JVM start.** In-process sentinel
   only; never persist or hard-code it.
7. **A short query returning nothing is the length floor**, not a missing term.
8. **`forceIndexing` does nothing**, because no ontology index is persisted to begin with.
9. **Design preflight is structural, not semantic** — it checks referential integrity, not
   whether a term exists.
10. **The 1000-item candidate cut is one literal doing three jobs.** Anything past it cannot
    be ranked, promoted or recovered downstream.
