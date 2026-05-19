# Search Subsystem Restoration — Reconnaissance

**Status:** recce only. No production code touched.
**Scope:** Phase 3, restoration of free-text + field-match search on Gemma's
JPA entities. Targeted technology: **Hibernate Search 7 + Lucene 9** (decided
by user; this recce plans the migration, not the choice).
**Branch:** `phase2-acl-migrate` HEAD `0b3a0d5b31`.
**Sister recce:** baseCode ontology indexer (`jena-text` + Jena 4.x). See
section 6; treat as a **separate effort** in Phase 3.
**Cross-team:** a parallel agent in `~/Dev/gemma-curation-ui` is auditing
gap impact from the UI consumer side. When that inventory lands here we
should reconcile (section 7).

---

## 0. TL;DR

- Search is a stub on `phase2-acl-migrate`. `SearchServiceImpl.search(...)`
  returns an empty `SearchResultMap`. All REST `search*` endpoints and the
  `gemma-web` general-search controller silently return zero hits.
- The full real implementation — including the Hibernate Search source,
  every `@Indexed`/`@Field`/`@IndexedEmbedded` annotation on 20+ entities,
  the Lucene-backed CLI `IndexGemmaCLI`, the index-on-cron mechanism, and
  the `CompositeSearchSource` chain that combined HS + database + ontology
  paths — was deleted wholesale in `ed93c2f023` ("Phase 2 Step 2:
  stub/delete search subsystem cascade") so the rest of the Phase 2
  upgrade could compile.
- The pre-strip stack was **Hibernate Search 5.11.12 / Lucene 5.5.5**
  bound to Hibernate ORM 5.6. To land on the current Hibernate 6.6 we
  must upgrade Hibernate Search to 7.x (drops `FullTextSession`, adopts
  the Search 6 mapping API with `@FullTextField`/`@KeywordField` etc.)
  and Lucene to 9.x.
- **Effort estimate: ~6 sessions** to a working free-text + field-match
  restoration covering the eight entity types previously indexed.
  Parallelizable into roughly three tracks (annotation port, source
  re-wiring, indexer + cron).
- The baseCode ontology-side indexer is **a separate Phase-3 effort**
  (Jena 4 + `jena-text`). Do not conflate.

---

## 1. Current state — what's stubbed, what's still in tree

### 1.1 The stub

`gemma-core/src/main/java/ubic/gemma/core/search/SearchServiceImpl.java`
(lines 64-71):

```java
/**
 * Stubbed search service for the post-Hibernate-Search era. Returns empty
 * results; preserves the VO-conversion path so callers can still convert
 * known results to their VO flavour.
 *
 * The full free-text/ontology/database search implementation lives in
 * git history (renovations branch, pre-phase2).
 */
```

Behaviour:

- `search(SearchSettings, SearchContext)` → empty `SearchResultMapImpl`.
- `getFields(...)` → `Collections.emptySet()` for every type.
- `getSupportedResultTypes()` still returns the eight historical
  `Identifiable` types so OpenAPI/swagger stays coherent (sec 1.2).
- `loadValueObject(...)` and `loadValueObjects(...)` still work — they
  route through the `valueObjectConversionService`, so once results
  come back they convert cleanly.

This is intentional: the VO conversion path is still load-bearing for
result rendering, and the stub keeps it warm.

### 1.2 What's still in tree but disconnected

| File | State | Notes |
|---|---|---|
| `SearchService.java` | interface, kept | Five-method contract: `search`, two overloads, `getFields`, `getSupportedResultTypes`, `loadValueObject(s)` |
| `SearchServiceImpl.java` | stubbed | See 1.1 |
| `SearchResultDisplayObject.java` | live | Used by the general-search-controller for the legacy MVC view |
| `SearchException`, `ParseSearchException`, `SearchTimeoutException` | live | Re-thrown from the (empty) search path; will be reused |
| `SearchContext.java` | live | Carries `Highlighter` + `Consumer<String>` issueReporter |
| `Highlighter.java`, `OntologyHighlighter.java`, `getOntologyTermFormatter.java` | live but unused | Will plug into the HS-7 highlighter when restored |
| `GeneSetSearch.java`, `GeneSetSearchImpl.java` | live, consumed | Used by `GeneServiceImpl`, `GeneSetServiceImpl`, `GenePickerController` etc. — but no longer hits any Lucene path; DB-only |
| `SearchSettings`, `SearchSettings.SearchMode`, `SearchSettingsValueObject` | live, consumed | `useFullTextIndex=true` default still flips through the stub |

There are **no surviving `@Indexed`/`@Field`/`@IndexedEmbedded`
annotations** anywhere under `gemma-core/src/main/java/ubic/gemma/model`.
The Phase 2 cascade stripped them all.

### 1.3 What was deleted (`ed93c2f023`)

Stat from the strip commit:

| Path | Disposition |
|---|---|
| `gemma-core/.../core/search/source/CompositeSearchSource.java` | deleted (203 LOC) |
| `gemma-core/.../core/search/source/DatabaseSearchSource.java` | deleted (672 LOC) |
| `gemma-core/.../core/search/source/GeneOntologySearchSource.java` | deleted (167 LOC) |
| `gemma-core/.../core/search/source/OntologySearchSource.java` | deleted (416 LOC) |
| `gemma-core/.../core/search/source/SearchSourceUtils.java` | deleted (24 LOC) |
| `gemma-core/.../core/search/SearchServiceImpl.java` | stubbed (469 → 192 LOC) |
| `gemma-core/.../core/search/SearchSource.java` | deleted |
| `gemma-core/.../core/search/FieldAwareSearchSource.java` | deleted |
| `gemma-core/.../core/search/IndexerService.java` | deleted |
| `gemma-core/.../core/search/BaseCodeOntologySearchException.java` | deleted |
| `gemma-core/.../core/tasks/maintenance/IndexerTask{,Impl,Command}.java` | deleted |
| `gemma-cli/.../apps/IndexGemmaCLI.java` | deleted (106 LOC) |
| `gemma-web/.../controller/common/IndexController.java` | deleted |
| Tests under `core/search/` and `rest/SearchWebServiceTest.java` | deleted |

The pre-Phase-2 `HibernateSearchSource.java` lived at
`gemma-core/src/main/java/ubic/gemma/core/search/source/HibernateSearchSource.java`.
The strip ran one commit later than the WIP that pulled in Hibernate 6
APIs, so `HibernateSearchSource` is in git at
`ed93c2f023^^:gemma-core/src/main/java/ubic/gemma/core/search/source/HibernateSearchSource.java`
(commit hash visible via `git log -- gemma-core/.../HibernateSearchSource.java`).

### 1.4 Consumers still wired to the stub

REST endpoints that compile and return empty results today:

| Service | Path | Method |
|---|---|---|
| `SearchWebService` | `/search` | `search(QueryArg, DatasetArg, TaxonArg, PlatformArg, ...)` — primary multi-type endpoint |
| `DatasetsWebService` | `/datasets` (filter via `DatasetArgService`) | `datasetArgService.getFilters(...)` → `searchService.search(...)` for query-string filtering |
| `AnnotationsWebService` | `/annotations/...` | `searchService.search(settings.withQuery(value))` for term-driven dataset finding |
| Web MVC `GeneralSearchController` | `/searcher.html` (legacy) | walking dead, but still wired |
| Web MVC `ArrayDesignController` | `/arrays/showAllArrayDesigns.html` filter | `SearchSettings.arrayDesignSearch(filter)` |

`gemma-curation-ui` consumes `/search`, `/datasets?query=...`, and
`/annotations/parents|children|term` — all four of these silently return
empty today.

### 1.5 Configuration leftovers

`default.properties` still defines:

```
gemma.search.dir=${gemma.appdata.home}/searchIndices
gemma.compass.dir=${gemma.search.dir}                  # legacy compat alias
basecode.ontology.index.dir=${gemma.search.dir}
```

`applicationContext-hibernate.xml` in the pre-strip world set:

```
hibernate.search.lucene_version=LUCENE_36              # ← drop in HS 7
hibernate.search.default.indexBase=${gemma.search.dir}
hibernate.search.indexing_strategy=manual
```

These properties name-changed in HS 6+; see section 3.5.

---

## 2. Inventory of entities that need re-indexing

From the pre-strip commit, 24 model classes carried HS annotations.
Eight were top-level `@Indexed` ("indexed roots" — full Lucene
documents); the rest were `@IndexedEmbedded` targets contributing fields
to those roots.

### 2.1 Indexed roots (`@Indexed`)

| Entity | File (pre-strip) | Fields | Embedded paths |
|---|---|---|---|
| `ExpressionExperiment` | `model/expression/experiment/ExpressionExperiment.java` | `shortName`, `name`, `description`, plus `accession.accession` (analyze=NO) | `bioAssays.*`, `experimentalDesign.*`, `experimentalDesign.experimentalFactors.factorValues.characteristics.*` (deep — 12 sub-fields), `characteristics.value/valueUri`, `primaryPublication.*`, `otherRelevantPublications.*` |
| `Gene` | `model/genome/Gene.java` | `name`, `ncbiGeneId` (analyze=NO), `officialSymbol` (analyze=NO), `officialName` (analyze=NO), `ensemblId` (analyze=NO) | `accessions.accession`, `aliases.alias`, `products.{name,ncbiGi,accessions.accession,previousNcbiId}` |
| `ArrayDesign` | `model/expression/arrayDesign/ArrayDesign.java` | `shortName`, `name`, `description`, `alternateNames.name`, `externalReferences.accession` | `alternateNames.*`, `externalReferences.*` |
| `CompositeSequence` | `model/expression/designElement/CompositeSequence.java` | `name`, `description` | `biologicalCharacteristic.*` (a `BioSequence`) |
| `BioSequence` | `model/genome/biosequence/BioSequence.java` | `name` | `sequenceDatabaseEntry.accession` |
| `GeneSet` | `model/genome/gene/GeneSet.java` | `name`, `description` | `characteristics.{value,valueUri}`, `sourceAccession.accession`, `literatureSources.*`, `members.gene.*` |
| `ExpressionExperimentSet` | `model/analysis/expression/ExpressionExperimentSet.java` | `name`, `description` | (planned: `experiments.*` — TODO marker in pre-strip source) |
| `BibliographicReference` | `model/common/description/BibliographicReference.java` | `name`, `title`, `abstractText`, `authorList`, `fullTextUri` | `chemicals.{name,registryNumber}`, `keywords.term`, `meshTerms.term`, `pubAccession.accession` |

### 2.2 Embedded contributors (`@IndexedEmbedded` target, no own `@Indexed`)

| Entity | Contributes to |
|---|---|
| `BibRefAnnotation` | `BibliographicReference.keywords` / `meshTerms` |
| `Characteristic` | `ExpressionExperiment`, `FactorValue`, `BioMaterial`, `GeneSet` — **load-bearing path for free-text-over-ontology-terms; see section 5** |
| `Statement` | `FactorValue` (a `Characteristic` subtype with predicate + object) |
| `DatabaseEntry` | accession holder for many roots |
| `Keyword`, `MedicalSubjectHeading` | publication metadata |
| `AlternateName` | `ArrayDesign` |
| `BioAssay`, `BioMaterial`, `Compound` | `ExpressionExperiment.bioAssays.*` graph |
| `ExperimentalDesign`, `ExperimentalFactor`, `FactorValue` | `ExpressionExperiment.experimentalDesign.*` graph |
| `GeneAlias`, `GeneProduct`, `GeneSetMember` | gene + gene-set graphs |
| `Contact` | publication author etc. |

**Important field-mode breakdown** (from pre-strip
`HibernateSearchSource.SEARCHABLE_CLASSES` field lists):

- "Exact / KeywordField" fields (`analyze=NO` historically) — short
  names, accessions, IDs, official symbols: 13 across all types.
- "Full-text / FullTextField" fields (default analyzed) — names,
  descriptions, value, valueUri, abstractText, title, authorList: ~40
  across all types when you count the embedded paths.

In HS 7's idiom that is roughly **40 `@FullTextField` annotations and
13-15 `@KeywordField` annotations** to port, plus all the embedded
paths.

---

## 3. Hibernate Search 7 + Lucene 9 — the target stack

### 3.1 Maven coordinates

Hibernate 6.6.x pairs with **Hibernate Search 7.2.x** (latest stable
in the 7.x line as of Phase 3) and **Lucene 9.x** (HS 7 ships against
Lucene 9, not 8 or 10 — confirm exact patch level at impl time but 9.11.x
is current).

```xml
<dependency>
    <groupId>org.hibernate.search</groupId>
    <artifactId>hibernate-search-mapper-orm</artifactId>
    <version>${hibernate.search.version}</version>
</dependency>
<dependency>
    <groupId>org.hibernate.search</groupId>
    <artifactId>hibernate-search-backend-lucene</artifactId>
    <version>${hibernate.search.version}</version>
</dependency>
```

Property block (parent `pom.xml`):

```xml
<hibernate.search.version>7.2.4.Final</hibernate.search.version>
<!-- Lucene version is transitively pinned by HS 7's BOM; do NOT
     pin it explicitly unless overriding. -->
```

Replaces the pre-Phase-2:
- `hibernate.search.version=5.11.12.Final`
- `lucene.version=5.5.5`

If we want highlighting beyond the HS-7 native projection, add:

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-highlighter</artifactId>
    <!-- version follows the HS 7 BOM -->
</dependency>
```

> **Step 1 footnote (2026-05-19):** `hibernate-search-backend-lucene:7.2.4.Final`
> already pulls `lucene-highlighter:9.11.1` transitively (verified via
> `mvn dependency:tree`), so we get the `org.apache.lucene.search.highlight.*`
> classes for free — no explicit `lucene-highlighter` dependency required when we
> reach Step 5's highlighter restoration. Lucene resolved to **9.11.1** under
> HS 7.2.4.

### 3.2 API differences — HS 5 → HS 7 (the big ones)

| HS 5 (pre-strip) | HS 7 (target) | Notes |
|---|---|---|
| `org.hibernate.search.annotations.*` | `org.hibernate.search.mapper.pojo.mapping.definition.annotation.*` | Whole new package |
| `@Indexed` | `@Indexed` | Same name, different class |
| `@Field` (with `Analyze.YES/NO`) | `@FullTextField` (analyzed) **or** `@KeywordField` (not analyzed) | Split into two annotations |
| `@Field(store=Store.YES)` | `@FullTextField(projectable=Projectable.YES)` | Storage semantics renamed |
| `@DocumentId` | `@DocumentId` | Still on the PK; auto-inferred if absent on the `@Id` field |
| `@IndexedEmbedded(includePaths={...})` | `@IndexedEmbedded(includePaths={...})` | Same annotation, **collection element type now inferred from the field's generic type** instead of the `targetElement` attribute |
| `@Boost(2.0f)` | `@FullTextField(searchAnalyzer=..., projectable=...)` + per-query boosts in the new DSL | Static boosts dropped — boost at query time |
| `@Analyzer(definition="...")` | `@FullTextField(analyzer="myAnalyzer")` referencing a programmatic `LuceneAnalysisConfigurer` | Definitions are now Java config, not XML/annotations |
| `org.hibernate.search.FullTextSession` | `org.hibernate.search.mapper.orm.session.SearchSession` | Acquired via `Search.session(entityManager)` |
| `FullTextSession.createFullTextQuery(luceneQuery, Class...)` | `SearchSession.search(Class).where(f -> f.match()...)` | Type-safe DSL; raw Lucene Query still possible via `f.fromLuceneQuery(query)` (`extension(LuceneExtension.get())`) |
| `Search.getFullTextSession(session)` | `Search.session(entityManager)` | Different entry point |
| Mass-indexer: `fts.createIndexer(Class...).startAndWait()` | `searchSession.massIndexer(Class.class).startAndWait()` | Renamed; otherwise similar fluent API |
| `MultiFieldQueryParser` from Lucene + `QueryParser` | HS 7 native DSL: `f.match().fields("name", "shortName", "description").matching(query)` | The Lucene-classic-QueryParser path is still available via `extension(LuceneExtension.get())` if we want to preserve operator parsing |

### 3.3 Mapping-config strategy

The pre-strip stack annotated entities directly. Two options for HS 7:

1. **Direct annotations on entities** (status quo, lowest delta) — port
   each `@Field` → `@FullTextField`/`@KeywordField`, keep
   `@IndexedEmbedded`, drop `@Analyzer(definition=...)` in favour of
   `analyzer="<name>"` referencing the programmatic configurer.
2. **Programmatic `HibernateOrmSearchMappingConfigurer`** — define the
   whole mapping in Java code, leaving model entities annotation-free.
   This decouples search config from JPA classes but is a much larger
   delta from the legacy structure.

**Recommend option 1.** It maps 1:1 to the pre-strip layout and is
recoverable from git history almost mechanically. Programmatic mapping
is a follow-up if we later want to split index schemas from JPA classes
(e.g. for a future Elasticsearch backend).

### 3.4 Backend

Lucene-direct (filesystem) backend, not Elasticsearch. Justification:

- One JVM, one disk, one index. Same single-server profile as today.
- No operational overhead of running an ES/Opensearch cluster.
- Re-indexing time at production scale is on the order of minutes for
  the eight indexed roots — overnight cron + on-demand CLI is the
  proven pattern from pre-Phase-2.
- The HS 7 Lucene backend is API-compatible with the new DSL; if we
  ever need ES we re-point the backend without rewriting the search
  service.

### 3.5 Property keys — old → new

| Pre-Phase-2 (HS 5) | HS 7 |
|---|---|
| `hibernate.search.lucene_version=LUCENE_36` | (gone — HS picks its own Lucene) |
| `hibernate.search.default.indexBase=${gemma.search.dir}` | `hibernate.search.backend.directory.root=${gemma.search.dir}` |
| `hibernate.search.indexing_strategy=manual` | `hibernate.search.automatic_indexing.synchronization.strategy=write-sync` (or `async` if we want non-blocking commits) + `hibernate.search.automatic_indexing.enabled=false` for the manual reindex-from-CLI pattern |
| (n/a) | `hibernate.search.backend.type=lucene` (explicit; defaults to lucene anyway when only the lucene backend artifact is on classpath) |

`gemma.search.dir` stays. `gemma.compass.dir` legacy alias stays (still
referenced in `default.properties` for back-compat).

> **Step 1 footnote (2026-05-19):** property keys verified against the
> shipped HS 7.2.4 jars (`org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings`,
> `org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings`,
> `LuceneIndexSettings`). The HS 6+ `automatic_indexing.*` namespace was renamed
> to `indexing.listeners.enabled` (on-write entity listeners) and
> `indexing.plan.synchronization.strategy` (write-sync / async commit behaviour)
> in HS 7.0; the recce's old key `hibernate.search.automatic_indexing.enabled`
> still works (kept as a deprecated alias on `HibernateOrmMapperSettings`) but
> the recommended Step-1 key is `hibernate.search.indexing.listeners.enabled`.
> Step 1 landed three property keys in `HibernateConfig`:
> `hibernate.search.backend.type=lucene`,
> `hibernate.search.backend.directory.type=local-filesystem`,
> `hibernate.search.backend.directory.root=${gemma.search.dir}`,
> plus `hibernate.search.indexing.listeners.enabled=false` and
> `hibernate.search.schema_management.strategy=none` to tolerate the
> empty-mapping case before Step 2 lands `@Indexed` entities.

### 3.6 Schema on disk

Same general shape as the legacy 5.11 layout:

```
${gemma.search.dir}/
  ExpressionExperiment/       # one Lucene index per @Indexed root
  Gene/
  ArrayDesign/
  CompositeSequence/
  BioSequence/
  GeneSet/
  ExpressionExperimentSet/
  BibliographicReference/
```

HS 7 segment format is **not backward-compatible** with HS 5.11's
on-disk indexes. Plan: drop the old directories during the cutover,
mass-reindex from scratch (overnight job).

---

## 4. Step-by-step migration plan

Effort labels: `S` ≈ half a session; `M` ≈ one session; `L` ≈ two
sessions. Total ≈ 6 sessions for a working free-text restoration with
parity across the eight pre-strip indexed roots.

### Step 1 — POM + bootstrap (`M`)

- Add HS-7 + Lucene-9 deps to `gemma-core/pom.xml` and version pins to
  parent `pom.xml`.
- Remove the dangling Lucene 5 transitive (`lucene-analyzers-common`,
  `lucene-queryparser`, `lucene-highlighter`) declarations — those poms
  no longer reference them post-strip but worth a sweep.
- Wire HS bootstrap into `applicationContext-hibernate.xml` /
  `HibernateConfig`:
  - new property keys (sec 3.5)
  - register the programmatic analyzer config bean (a
    `LuceneAnalysisConfigurer` providing a stock English analyzer +
    any custom analyzers we historically used — the pre-strip code
    relied on Lucene's default English analyzer, so this is small).
- Smoke-test: `mvn -pl gemma-core compile` clean against an empty
  search package; the JVM starts and the new HS context initializes.

**Parallelizable with Step 2.**

### Step 2 — Annotation port (`L`)

For each of the 24 entities from section 2: port HS 5 annotations to
HS 7 annotations. Per-class checklist:

- `import org.hibernate.search.annotations.*;` → new package
- `@Indexed` (no change in name, new package)
- `@Field` → `@FullTextField` (analyzed) or `@KeywordField` (not
  analyzed). Map by the historical `Analyze.NO`/`Analyze.YES` flag.
- `@IndexedEmbedded(includePaths=...)` → same annotation, but **drop
  `targetElement` if present** (HS 7 infers from collection generics).
- `@Field(analyze=Analyze.NO, store=Store.YES)` →
  `@KeywordField(projectable=Projectable.YES)`.
- Drop `@Boost` (not supported; query-time boost only).

**Recover-from-git pattern:** for each entity, diff the pre-strip
revision and re-apply the same field set under the new API. The pre-strip
source-of-truth is `ed93c2f023^^:gemma-core/.../<Entity>.java`.

This step is **independent per entity** — split across two agents
trivially. Wire-up of the new `@FullTextField(analyzer=...)` references
depends on Step 1's analyzer config bean.

### Step 3 — Re-introduce `SearchSource` + `HibernateSearchSource` (`L`)

- Restore (port from git) the deleted classes:
  - `SearchSource` interface
  - `FieldAwareSearchSource` interface
  - `CompositeSearchSource` (no API delta needed — it's a fan-out
    coordinator)
  - `DatabaseSearchSource` (no Lucene contact; restore as-is, fix only
    the gsec → `ubic.gemma.core.security` rename that happened in
    Phase 3)
  - `OntologySearchSource` (calls `baseCode`'s ontology services;
    largely API-stable. Will need the Jena 4 follow-up — sec 6 — for
    full free-text-over-ontology coverage. Restore now, accept the
    limitation that ontology free-text is single-term until baseCode
    catches up.)
  - `GeneOntologySearchSource`
- Rewrite `HibernateSearchSource` against the HS-7 API:
  - Replace `Search.getFullTextSession(...)` →
    `Search.session(entityManager)`.
  - Replace `MultiFieldQueryParser` + raw Lucene `Query` with
    `searchSession.search(Class).where(f -> f.match().fields(...)
    .matching(query).toPredicate())`.
  - For "EXACT" mode, use `f.match().analyzer("keyword")` or the
    keyword-field variant.
  - Replace `FullTextQuery.SCORE`/`.DOCUMENT` projections with HS-7's
    `f.composite()`/`f.score()`/`f.highlight()` projection DSL.
  - For highlighting: use HS-7's native `f.highlight("name").of(...)`
    if it covers our needs, otherwise drop down to Lucene 9's
    `UnifiedHighlighter` via the Lucene-extension projection.
- Re-implement `getFields(...)` returning the same per-type field
  lists as the pre-strip code (verbatim copy from
  `HibernateSearchSource.ALL_FIELDS` / `ALL_EXACT_FIELDS`).
- Re-wire `SearchServiceImpl`:
  - Inject `List<SearchSource>` (Spring auto-wires the components).
  - Replace the empty-`SearchResultMapImpl` shortcut with a delegation
    to `CompositeSearchSource`.
  - Restore the `SecurityUtil`-based ACL filter step (this is the
    `gsec`-rename-aware path — now `ubic.gemma.core.security.SecurityUtil`).

**Depends on Steps 1 + 2.** Splittable: `HibernateSearchSource`
rewrite is one track; `DatabaseSearchSource` + `OntologySearchSource`
restoration is another (mostly mechanical).

### Step 4 — Restore the indexer (`M`)

- Re-introduce `IndexerService` interface + impl:
  - One method per indexable type
    (`indexExpressionExperiments(Class...)`).
  - Each calls `searchSession.massIndexer(Class.class)
    .threadsToLoadObjects(N).startAndWait()`.
- Re-introduce `IndexerTask` + `IndexerTaskImpl` + `IndexerTaskCommand`
  for the in-process Quartz / `@Scheduled` driver (the pre-strip
  cron mechanism scheduled this via `gemma.quartz`-style beans;
  re-stand up the same trigger).
- Re-introduce `IndexGemmaCLI` (the `index-gemma` CLI sub-command) —
  port from `ed93c2f023^^:gemma-cli/.../apps/IndexGemmaCLI.java`.
  Wire it as a `CLI` bean in `gemma-cli`.
- Restore `IndexController` (gemma-web admin "reindex" button) — low
  priority since the curation UI is the consumer of record now;
  document as deferred.

**Depends on Steps 1 + 2 + 3.**

### Step 5 — Re-wire REST + web consumers (`M`)

- `SearchWebService` already passes `SearchSettings` + `SearchContext`
  through to `searchService.search(...)`. **Nothing should change
  here** once the service stops being a stub.
- `DatasetsWebService` + `DatasetArgService.getFilters(query)`: ditto.
- `AnnotationsWebService.searchTerm(...)`: ditto.
- Highlighter wiring: re-introduce `LuceneHighlighter` (the pre-strip
  highlighter that produced HTML-wrapped match snippets). Plug into
  `SearchContext.getHighlighter()`.
- Re-introduce `SearchWebServiceTest` (274 LOC) and
  `SearchServiceIntegrationTest` (418 LOC) ported from
  `ed93c2f023^^`. These are the regression bar.

**Depends on Step 3 for actual results to flow.**

### Step 6 — Initial reindex + smoke testing (`S`)

- Drop any historical `${gemma.search.dir}` content on the target
  deployment (HS 7 segment format is incompatible).
- Run `index-gemma` against `gemdtest`. Verify per-type document
  counts vs. `EE | count(*)` etc.
- Hit each REST `search*` endpoint manually and confirm at least one
  known query returns the expected hit:
  - `/search?query=GSE2018` → finds that EE
  - `/search?query=BRCA1&resultType=Gene` → finds that gene
  - `/search?query=parkinson&resultType=ExpressionExperiment` →
    Parkinson EEs surface
- Production cutover: schedule the reindex inside a maintenance window,
  flip the new HS-7 build, monitor `/search` latency.

### Effort + parallelism summary

| Step | Effort | Can run in parallel with |
|---|---|---|
| 1. POM + bootstrap | M | 2 |
| 2. Annotation port | L | 1 (after Step 1 lands the analyzer bean, port can finish independently) |
| 3. Search source restore | L | (sequential after 1 + 2) — can internally split DB / Ontology / Hibernate sources across two agents |
| 4. Indexer + CLI + cron | M | 5 (after 3 lands the source chain, indexer and REST re-wire can split) |
| 5. REST + web re-wire | M | 4 |
| 6. Reindex + smoke | S | (sequential, last) |

**Total: ~6 sessions** with one agent. **~4 calendar sessions** with
two agents running in parallel post-Step-1.

---

## 5. The free-text-on-ontology-terms gap

Two distinct things get conflated under "free-text on ontology terms"
in the user-side ask:

### 5.1 Free-text over ontology-tagged characteristics (inside Gemma)

A query like "parkinson" needs to surface every `ExpressionExperiment`
whose `characteristics.value` or `characteristics.valueUri` contains
"parkinson". This is **satisfied directly by the HS-7 restoration
above** — `Characteristic.value` and `Characteristic.valueUri` are
`@FullTextField`s (`@Field` in HS 5) embedded into `ExpressionExperiment`
via `@IndexedEmbedded`. Same for `FactorValue.characteristics.*` and
`BioMaterial.characteristics.*` (deep paths in the EE field list).

The pre-strip `DATASET_FIELDS` array in `HibernateSearchSource`
explicitly includes:

```
"characteristics.value", "characteristics.valueUri",
"bioAssays.sampleUsed.characteristics.value",
"bioAssays.sampleUsed.characteristics.valueUri",
"experimentalDesign.experimentalFactors.factorValues.characteristics.value",
"experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri",
```

— so "free-text-over-ontology-terms-as-they-appear-on-datasets" is
**not a separate effort**; it falls out of Step 2 (annotation port)
+ Step 3 (source restoration).

### 5.2 Free-text over the ontology graph itself

A query like "neurodegener*" needs to surface ontology terms whose
**label or definition** matches — even when no Gemma entity is yet
tagged with them. This goes through `baseCode`'s ontology services
(`OntologyService.findTerm(...)` etc.), which today are backed by Jena
+ a fragile in-process Lucene 3 index built by baseCode itself.

This path is **not restored by Hibernate-Search 7 on the Gemma side.**
The HS index doesn't contain ontology terms — only the Gemma entities
that reference them. Ontology-term free-text discovery lives in
baseCode and needs its own modernization. See section 6.

---

## 6. baseCode ontology indexer (in-Gemma, refactor out later)

**Decision (user, 2026-05-19):** the needed baseCode ontology-indexer code
gets pulled INTO Gemma rather than upgraded in baseCode upstream. We will
refactor it back out later if/when that makes sense, but for now we
explicitly accept the duplication to avoid multi-repo coordination during
Phase 3.

### 6.1 Where baseCode is wired into Gemma today

- Dependency: `pom.xml` declares
  `baseCode:baseCode:1.1.34-RENOVATIONS-SNAPSHOT`.
- Consumers under `gemma-core/src/main/java/ubic/gemma/core/ontology/`:
  - `OntologyConfig.java` — bootstraps a `TdbOntologyService` (Jena TDB
    backed) for the unified ontology + per-ontology beans for MONDO,
    PATO, etc.
  - `OntologyService.java` (Gemma's, wraps baseCode's) — exposes
    `findTerm`, `findExperiments`, `getParents`, etc.
  - `OntologyCache.java` — in-memory cache around baseCode results.
  - `FactorValueOntologyService.java`, `OntologyExternalLinks.java`,
    `OntologyUtils.java`, `GoMetricImpl.java` — pure consumers.

### 6.2 The problem

baseCode's `OntologySearchService` (pre-renovations) built a per-ontology
Lucene 3-era in-memory index keyed off the Jena triples. With Lucene 5+
the API surface shifted enough that baseCode's index code degraded —
the Phase 2 strip on the Gemma side removed *Gemma's* consumption of
`LuceneQueryUtils.escape()` and the Compass/Lucene 3 escape helpers
from the ontology code path, but the baseCode dep itself still ships
its own (now disconnected from Gemma's HS index) Lucene-based ontology
search.

### 6.3 Recommended path — in-Gemma `jena-text` + Jena 4.x port

**`jena-text` + Jena 4.x namespace upgrade, hosted inside Gemma.**

`jena-text` (`org.apache.jena:jena-text`) is Apache Jena's standard
integration of Lucene over RDF datasets — it indexes literal values
(rdfs:label, skos:prefLabel, IAO_0000115 definition annotations, etc.)
and exposes them as a SPARQL property function. baseCode already uses
Jena TDB; adding `jena-text` turns "give me OBO terms matching
'neurodegen*'" into a single SPARQL `?term text:query 'neurodegen*'`
clause backed by a properly-maintained Lucene index.

Jena 4.x is the right anchor: the modern Jena release line, Lucene 8/9
compatible, and Apache-maintained. Going to Jena 5 is premature — Jena
5 requires Java 17 minimum but also drops some `org.apache.jena.atlas`
APIs the existing code references; Jena 4.10.x is the sweet-spot.

**Suggested sub-steps (in Gemma):**

1. Add `org.apache.jena:jena-tdb2:4.10.0`, `org.apache.jena:jena-text:4.10.0`,
   and the Jena 4 transitive deps directly to `gemma-core/pom.xml`. Keep
   the existing baseCode dep for now (we still consume non-search baseCode
   pieces).
2. Create `ubic.gemma.core.ontology.search` package; port the needed
   pieces of baseCode's `OntologySearchService` interface + Jena-TDB
   wiring inline (mark the new files with a Javadoc header noting they
   originated in baseCode and were pulled in for the Phase 3 search
   restoration).
3. Build a `TextDataset` wrapping the existing TDB dataset (the one
   `OntologyConfig` already manages); register the index inside Gemma's
   Spring context.
4. Replace the disconnected baseCode `LuceneQueryUtils.escape()`
   consumption with Jena 4 `TextSearch.escape` (or inline equivalent).
5. Update `OntologySearchSource` (restored in Step 3 of the main
   migration) to call into the in-Gemma `OntologySearchService` rather
   than the baseCode-shipped one. The Spring `@Qualifier` selects the
   in-Gemma bean.
6. **Do not bump baseCode**: the non-search baseCode classes Gemma still
   uses (`TdbOntologyService`, ontology-loader scaffolding, GO term
   structures) keep working against `1.1.34-RENOVATIONS-SNAPSHOT`.

**Effort estimate: 2-3 sessions in Gemma.** (Faster than the
sibling-repo path because we don't pay the cross-repo cut/release tax.)

### 6.4 Refactor-out checkpoint (deferred)

When the dust settles on Phase 3 we can decide whether to push the
ontology-search subset back to baseCode (where other downstream consumers
of baseCode could benefit) or leave it in Gemma permanently. The
in-Gemma classes are tagged with the originated-in-baseCode Javadoc
header so the future extraction has a clear target list.

### 6.5 Cross-reference with main search plan

The Gemma-side `OntologySearchSource` (restored in Step 3) consumes
whichever `OntologySearchService` bean is wired. Until the in-Gemma
port lands, ontology-term free-text discovery is partially functional
(single-term, no fuzzy matching, no compound-term boosting). Order the
work so the main HS 7 / Lucene 9 stack lands first; the in-Gemma
ontology index can follow without blocking the rest of the
free-text-over-EE search.

---

## 7. Cross-team note — `gemma-curation-ui`

A parallel agent under `~/Dev/gemma-curation-ui` (aka `gemma-ui`) is
auditing **where the curation UI currently calls `search*` REST
endpoints and what it expects back**. When that inventory lands here,
the cross-reference job is:

- Confirm every endpoint the UI hits is in section 1.4.
- Cross-check the field-list expectations against section 2.1's
  per-type field inventory — flag any UI feature that depended on a
  field the pre-strip code did **not** index (and therefore won't be
  in the HS-7 restoration either).
- Verify the UI's free-text widget hits `/search` (multi-type) vs.
  `/datasets?query=` (single-type filter) and that both come back
  populated post-restoration.

Until that lands: **assume parity with the pre-strip behaviour is
sufficient**, since the curation UI is being authored against the
same REST surface that pre-strip Gemma served.

---

## 8. What we lose if we don't restore search

In rough priority order:

1. **`gemma-curation-ui` cannot do free-text dataset discovery.** The
   primary curator workflow ("find the EE that matches this incoming
   GEO record / publication abstract") goes through
   `/datasets?query=...` which silently returns empty. Curators have
   to fall back on direct DB queries or pre-computed lists. This is
   the single biggest functional gap.
2. **REST `/search` returns empty.** Third-party API users
   (notably bioconductor's `gemma.R`) see zero results for every
   query. Already user-visible.
3. **REST `/annotations/...` term-driven dataset discovery silently
   skips the search step.** `searchService.search(settings.withQuery(value))`
   in `AnnotationsWebService` line 481 returns empty; downstream we
   only get the small "exactly matched the ontology URI" subset, not
   anything that mentions the term in free text.
4. **`/platforms` filter-by-text** silently empty. Less critical than
   datasets, but a UI gap.
5. **`/genes` symbol/name search** also goes through `SearchService`
   in `DatasetArgService` and other paths; the convenience of "type
   'BRCA1' get the gene" stops working. Note: most gene lookups
   already go through `GeneService.findByOfficialSymbol(...)`
   (database, not search), so impact is small but non-zero for
   synonyms / NCBI IDs / Ensembl IDs.
6. **Web MVC search page** (`/searcher.html`) silently empty —
   walking-dead frontend, low impact, but it does leak through to
   any external bookmark / link.

---

## 9. Open questions

Things that need user / operator input before implementation begins:

1. **Acceptable downtime for the initial mass-reindex on production.**
   Pre-strip experience suggests ~hours for the full EE + Gene +
   ArrayDesign + BibliographicReference set. Overnight is fine; do we
   want a maintenance window or a hot reindex (HS 7 supports
   write-sync during indexing, with a small latency cost)?
2. **Reindex cadence in production.** Pre-strip: full reindex via the
   Quartz scheduler at midnight + on-write incremental indexing
   (`automatic_indexing.enabled=true`). Do we keep that pattern, or
   shift to **CLI-only reindex** triggered from the curation UI's
   admin actions? Recommendation: keep automatic-on-write +
   nightly-full, but make the nightly-full configurable.
3. **Search-result ranking acceptance criteria.** The pre-strip code
   applied a `0.9` static penalty to full-text scores vs. exact
   database hits. HS 7's score normalization differs from Lucene
   5's. Do we want a regression suite of "for query X the top hit
   must be Y" cases (~20 known-good pairs)? If yes, who curates the
   pairs?
4. **`gemma.search.dir` deployment topology.** Currently on local
   disk. With `OBJECT_STORAGE_RECCE.md` in flight, do we anticipate
   moving search indexes to object storage in the next 18 months?
   Recommendation: **no**. Lucene indexes are not designed to live
   on object storage; they are fast iff they live on a real
   filesystem. Even if we ever go S3, search indexes stay on local
   disk per replica (re-build on startup if missing).
5. **Search-API contract: include / exclude blacklisted entities by
   default?** Pre-strip `SearchService.search` filtered out
   `BlacklistedEntity` by default. Curation-UI side may want
   blacklisted EEs surfaced (for blacklist-management workflows).
   Decision: keep default filter, add `includeBlacklisted=true`
   knob on `SearchSettings`.
6. **Custom analyzers.** The pre-strip stack used Lucene 5's default
   `StandardAnalyzer`. Anything domain-specific (gene-symbol-aware
   tokenization? accession-number boundary handling?) that we want
   in HS 7 from day one? Recommendation: ship with `StandardAnalyzer`
   parity, log notable hits-we-expected-but-missed against
   `notable_cases.md`, iterate the analyzer config in a follow-up.
7. **Highlighter scope.** Pre-strip used Lucene's
   `org.apache.lucene.search.highlight.Highlighter` to wrap matches in
   the result list. HS 7 has a native `highlight()` projection.
   Confirm that the curation UI uses the highlighted snippet (vs.
   doing its own client-side highlighting) — if yes, we keep parity;
   if no, we can simplify Step 3.

---

## 10. References

- Pre-Phase-2 implementation: `git show ed93c2f023^^ -- gemma-core/src/main/java/ubic/gemma/core/search/`
- The strip commit: `ed93c2f023` ("Phase 2 Step 2: stub/delete search subsystem cascade")
- HS introduction (historical baseline for annotations): `aebcc4e7b4` / `c7999bf404` ("Introduce Hibernate Search")
- Phase 1b anchor: `e367b9f58f` ("Spring 5.3 / Hibernate 5.6 / Lucene 5.5 (Renovations Phase 1b)")
- Stub source: `gemma-core/src/main/java/ubic/gemma/core/search/SearchServiceImpl.java`
- REST consumers: `gemma-rest/src/main/java/ubic/gemma/rest/{SearchWebService,DatasetsWebService,AnnotationsWebService}.java`
- baseCode coords: `pom.xml` L450-454
- Ontology bootstrap: `gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyConfig.java`
- Sibling recce: `OBJECT_STORAGE_RECCE.md` (engine-by-engine cloud-readiness — Lucene marked as "stays local")
- Living plan: `RENOVATIONS.md`
