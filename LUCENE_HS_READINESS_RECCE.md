# Lucene 10 + Hibernate Search 8 readiness recce

Read-only research from baseline `251ad09f48` on branch `recce-lucene-readiness`.
Scope: what would break in Gemma if we bumped to Lucene 10.x and Hibernate
Search 8.x.

## TL;DR

**Recommendation: stay on Lucene 9.11.1 / Hibernate Search 7.2.x for now.**

Two hard upstream pins block the bump, regardless of any code work Gemma would
do:

1. **`jena-text` 4.10.0 pins Lucene 9.x** transitively via its parent POM
   (we override 9.7.0 → 9.11.1 to converge with HS 7.2.4). Jena 5 still tracks
   Lucene 9; there is no Jena release that pairs with Lucene 10. Until the
   ontology stack moves off `jena-text` (or `jena-text` itself moves to
   Lucene 10), we cannot drop Lucene 9 from the classpath.
2. **Hibernate Search 8 requires Hibernate ORM 7.** Gemma is on Hibernate ORM
   6.6.18. The ORM 6 → 7 upgrade is itself a significant lift (Jakarta-only
   in places, new bootstrap APIs, behaviour changes around lazy loading and
   stateless sessions). It is not on any in-flight roadmap; the recent HB6
   cascade audit (`HIBERNATE6_CASCADE_AUDIT.md`) tied the codebase tightly to
   6.x semantics.

The Gemma-side direct API surface, by contrast, is **very small and mostly
already forward-compatible** — only 4 files import `org.apache.lucene.*`, the
indexed-entity annotations are all on the modern HS 6+ POJO mapping (no legacy
`@Field`/`@AnalyzerDef`), and the HS API usage (`Search.session(...)`,
`SearchSession.search().select(...).where(...).fetch(...)`,
`MassIndexer.startAndWait()`) is the canonical HS 6/7/8 surface. There is no
Gemma-internal blocker. The blockers are entirely in the dep graph.

Revisit when **either** (a) the ontology stack moves off `jena-text` (this is
already partly the case: the in-memory `LuceneOntologySearchIndex` is direct
Lucene 9, not jena-text, and could go to Lucene 10 standalone) **or** (b) we
queue the Hibernate ORM 7 migration. Until then, the Lucene 10 / HS 8 bump
buys nothing material for Gemma's use case (full-text over MySQL-backed
entities; index sizes in the tens of MB; queries are millisecond-class).

## Current versions

| Component | Version | Note |
|---|---|---|
| `lucene.version` | **9.11.1** | `pom.xml:1619`. Convergence pin between HS 7.2.4 (Lucene 9.11.1) and jena-text 4.10.0 (Lucene 9.7.0). |
| `hibernate.search.version` | **7.2.4.Final** | `pom.xml`. |
| `hibernate.version` (ORM) | **6.6.18.Final** | `pom.xml`. |
| `jena.version` | **4.10.0** | `pom.xml:1614`. Last 4.x; Jena 5 dropped APIs Gemma's in-tree baseCode port references. |
| Java | **21 LTS** | per `CLAUDE.md`. |

Lucene jars declared in dependencyManagement (parent `pom.xml`):
`lucene-core`, `lucene-analysis-common`, `lucene-queryparser`,
`lucene-highlighter`, `lucene-backward-codecs`, `lucene-queries`,
`lucene-sandbox`, `lucene-memory`, `lucene-join`, `lucene-facet`.

## Direct API usage inventory

### Lucene direct imports (4 files in `gemma-core/src/main/java`)

| File | Lucene surface used | Lucene 10 status |
|---|---|---|
| `ubic/gemma/core/ontology/basecode/jena/LuceneOntologySearchIndex.java` | `Analyzer`, `CharArraySet`, `EnglishAnalyzer`, `Document`, `Field`, `StringField`, `TextField`, `DirectoryReader`, `IndexWriter`, `IndexWriterConfig`, `QueryParser`, `ParseException`, `IndexSearcher`, `Query`, `ScoreDoc`, `TopDocs`, `ByteBuffersDirectory`, `Directory` | **Mostly forward-compatible.** Already uses `searcher.storedFields().document(sd.doc)` (line 124) — the new Lucene 9 API that replaces the now-removed `searcher.doc()`. `ByteBuffersDirectory`, `EnglishAnalyzer`, the `IndexWriterConfig.OpenMode.CREATE` API, and `QueryParser` (still in `lucene-queryparser` in 10) all survive. **One mild snag:** Lucene 10 split some analyzers out and renamed `setAllowLeadingWildcard` semantics; the `parser.setAllowLeadingWildcard(true)` call (line 110) needs to be re-verified. **Trivial fix.** |
| `ubic/gemma/core/ontology/search/JenaTextOntologySearchService.java` | `StandardAnalyzer`, `FSDirectory` (passed into `TextDatasetFactory.createLuceneIndex(...)`) | **Blocked by `jena-text` 4.10.0**, which targets Lucene 9. This file's direct Lucene usage is trivial (two classes) but the file's *consumer*, `TextDatasetFactory`, is Apache Jena's, not Gemma's. The block is upstream. |
| `ubic/gemma/core/search/lucene/LuceneQueryUtils.java` | `KeywordAnalyzer`, `Term`, `QueryParser`, `ParseException`, `BooleanClause`, `BooleanQuery`, `PrefixQuery`, `Query`, `TermQuery`, `WildcardQuery` | **Forward-compatible.** Uses the parser-DSL classic public API and the `BooleanQuery` / `TermQuery` / `WildcardQuery` types that are stable in Lucene 10. The `BooleanClause.isRequired() / isProhibited() / getQuery()` accessors remain. |
| `ubic/gemma/core/search/lucene/LuceneParseSearchException.java` | `ParseException` (re-throw wrapper) | **Forward-compatible.** Just wraps `org.apache.lucene.queryparser.classic.ParseException`, which is still in `lucene-queryparser` in 10. |

**No Lucene imports** in `gemma-cli/src/main/java`, `gemma-rest/src/main/java`,
`gemma-web/src/main/java`, or any test source (`gemma-core/src/test/java`).
That is the whole production-Java Lucene API surface.

### Hibernate Search direct imports (26 files)

Two service classes carry the entire HS API call surface; the remaining 24
are entity classes carrying annotations only.

**Call-site usage (2 files):**

| File | HS API surface | HS 8 status |
|---|---|---|
| `ubic/gemma/core/search/source/HibernateSearchSource.java` | `Search.session(session)`, `SearchSession.search(clazz).select(f -> f.composite(...)).where(f -> f.simpleQueryString().fields(...).matching(...).defaultOperator(...)).fetch(N)`, `SearchResult.hits()`, `f.entityReference().toProjection()`, `f.score().toProjection()`, `f.field(name, String.class).toProjection()`, `EntityReference.id()`, `BooleanOperator.OR`, `SearchProjection<?>` | **Forward-compatible.** This is canonical HS 6/7/8 DSL. The `Search.session(...)` entry point, the `SearchSession.search()` builder, the projection DSL (`f.composite(...)`), the `simpleQueryString` predicate, and `SearchResult.hits()` are all stable across the HS 6→7→8 line. |
| `ubic/gemma/core/search/indexer/IndexerServiceImpl.java` | `Search.session(session).massIndexer(class).threadsToLoadObjects(n).batchSizeToLoadObjects(n).idFetchSize(n).mergeSegmentsOnFinish(true).purgeAllOnStart(true).startAndWait()` | **Forward-compatible.** Mass-indexer fluent API is stable across HS 6/7/8. |

**Annotation usage (24 entity classes)** — all on the modern HS 6+ POJO
mapping (`org.hibernate.search.mapper.pojo.mapping.definition.annotation.*`
and `org.hibernate.search.engine.backend.types.*`). Annotations used:

- `@Indexed` (8 roots: `ExpressionExperiment`, `Gene`, `ArrayDesign`,
  `CompositeSequence`, `BioSequence`, `GeneSet`, `ExpressionExperimentSet`,
  `BibliographicReference`, plus `BioMaterial`, `Compound`,
  `MedicalSubjectHeading`, `DatabaseEntry`, `ExperimentalFactor` — 13 total)
- `@DocumentId`
- `@FullTextField`, `@FullTextField(projectable = Projectable.YES)`
- `@KeywordField`
- `@IndexedEmbedded`, `@IndexedEmbedded(includePaths = {...})`
- `Projectable.YES`

**Crucially: zero usage of the legacy HS 5 annotation surface** (no
`org.hibernate.search.annotations.@Field`, no `@AnalyzerDef`, no
`@TokenFilterDef`, no `@FullTextIndexed`). The recent HS 5 → HS 7 migration
(noted in `SEARCH_RECCE.md`) already moved every entity to the post-HS-6
contract that HS 8 also targets. Forward-compatible.

### Configuration surface (HibernateConfig)

`gemma-core/.../persistence/hibernate/HibernateConfig.java` (lines 219-237)
sets the following HS 7 properties:

```
hibernate.search.backend.type=lucene
hibernate.search.backend.directory.type=local-filesystem
hibernate.search.backend.directory.root=<resolved-abs-path>
hibernate.search.indexing.listeners.enabled=false
hibernate.search.indexing.plan.synchronization.strategy=write-sync
hibernate.search.schema_management.strategy=create-or-update
```

All of these keys are in the `hibernate.search.backend.*` /
`hibernate.search.indexing.*` namespaces introduced in HS 6 and retained in
HS 7 and (per the HS 8 changelog) HS 8. **No HS 5 `default.indexBase` /
`indexing_strategy` / `lucene_version` keys remain** — those were cleaned up
in commit `04d720c666` (the `directory.root` placeholder coercion workaround
referenced in `CLAUDE.md`). The `resolveSearchIndexBase` defensive coercion
in `HibernateConfig.java` is HS-version-agnostic; it should survive HS 8.

## Lucene 10 incompatibilities found in this codebase

Going through the headline Lucene 10 breaking changes against the actual
Gemma surface:

| Lucene 10 break | Hit in Gemma? | Effort |
|---|---|---|
| `IndexSearcher#doc()` removed (use `storedFields().document(...)`) | **No** — `LuceneOntologySearchIndex.java:124` already uses `searcher.storedFields().document(sd.doc)`. The forward-compatible API was adopted when the class was written for Lucene 9. | 0 |
| `IndexWriterConfig.setUseCompoundFile()` removed | **No** — Gemma never calls it. | 0 |
| `org.apache.lucene.codecs.simpletext` package removed | **No** — Gemma never imports `codecs.simpletext`. | 0 |
| `QueryParser` moved out of `lucene-core` to `lucene-queryparser` (already done in 9, kept in 10) | Already handled — Gemma depends on `lucene-queryparser` explicitly. | 0 |
| `Analyzer` lifecycle (close semantics, `tokenStream(...)` finalization) | `LuceneOntologySearchIndex.close()` calls `analyzer.close()` — already follows the modern pattern. | 0 |
| Custom `Tokenizer` / `TokenFilter` implementations | **No** — Gemma writes no custom analyzers. Uses stock `EnglishAnalyzer`, `KeywordAnalyzer`, `StandardAnalyzer`. | 0 |
| `BooleanQuery.setMaxClauseCount` static-method removal in 10 (now per-instance) | **No** — Gemma never sets max-clause-count. | 0 |
| `lucene-backward-codecs` semantics in 10 | Gemma includes `lucene-backward-codecs` in dependencyManagement but only as a passthrough; no codec is named in Gemma code. The reason it sits in the dep tree is HS 7's transitive ask. HS 8 still depends on it. | 0 |
| `IndexSearcher` parallelism/`Executor` ctor signature changes | **No** — Gemma constructs `new IndexSearcher(reader)` with the single-arg ctor (line 85), which is preserved in Lucene 10. | 0 |
| `QueryParser.setAllowLeadingWildcard(true)` | Still exists in 10 but its perf characteristics changed slightly. | 0 (semantically equivalent) |
| `RAMDirectory` (long-since removed) | **No** — Gemma uses `ByteBuffersDirectory`, the documented replacement (line 162). | 0 |

**Net: zero Gemma-side Lucene 10 incompatibilities in actual call sites.**
The four direct-Lucene files all happen to be on the modern Lucene 9 API
already, which is forward-compatible into Lucene 10.

## Hibernate Search 8 incompatibilities

| HS 8 break | Hit in Gemma? | Effort |
|---|---|---|
| Requires Hibernate ORM 7 | **YES — HARD BLOCKER.** Gemma is on ORM 6.6.18. | "more — significant rewrite" |
| Requires Java 17+ | No — Gemma is on Java 21. | 0 |
| `@Field` deprecated → removed in HS 7 already | No usage of the legacy `@Field` annotation anywhere in tree. | 0 |
| Property-key rename (HS 5 `hibernate.search.default.*` → HS 6+ `hibernate.search.backend.*`) | Already done in current `HibernateConfig.java`. | 0 |
| `SearchSession` / `Search.session(...)` API | Already in use; HS 8 keeps the entry point. | 0 |
| `simpleQueryString` predicate DSL | Already in use; stable across HS 6/7/8. | 0 |
| `MassIndexer` fluent API | Already in use; stable. | 0 |
| `automatic_indexing.enabled` → `indexing.listeners.enabled` rename (HS 7) | Already on the new key (`HibernateConfig.java:230`). | 0 |
| `automatic_indexing.synchronization.strategy` → `indexing.plan.synchronization.strategy` rename (HS 7) | Already on the new key (`HibernateConfig.java:233`). | 0 |
| Projection DSL signature tweaks (`toProjection()` chaining) | Already in use; the form used in `HibernateSearchSource` (`f.entityReference().toProjection()`, `f.field(name, String.class).toProjection()`) is the HS 7+ canonical shape. | 0 |
| Schema-management API changes (`schema_management.strategy=create-or-update`) | Already on the modern key; HS 8 retains it. | 0 |
| Backend pin (Lucene 10) | Blocked transitively by jena-text. | "more" |

**Net: the only HS 8 incompatibility is its ORM 7 requirement.** Everything
else is already on the HS 7 surface that HS 8 retains.

## Risk areas specific to Gemma

1. **`jena-text` → Lucene 9 pin (the show-stopper).** `JenaTextOntologySearchService`
   wraps `TextDatasetFactory.createLuceneIndex(luceneDir, entityDef, new StandardAnalyzer())`.
   The `TextDatasetFactory` lives in Apache Jena (`org.apache.jena.query.text`),
   not in Gemma. Jena 4.10.0 and Jena 5 both target Lucene 9.x. Until either
   (a) Jena releases a Lucene-10-targeting version, or (b) Gemma replaces
   `jena-text` with a direct-Lucene index over its TDB (which is feasible —
   `LuceneOntologySearchIndex.java` is already a direct-Lucene proof of
   concept against an in-memory `OntModel`), we cannot drop Lucene 9 from
   the classpath. **Effort to remove the jena-text dependency: ~1 week**
   (rewrite indexing pipeline, re-validate every ontology search test).

2. **Hibernate ORM 6.6 → 7 lift (the second show-stopper).** HS 8 ships
   against ORM 7. The HB5 → HB6 migration in this repo (see
   `HIBERNATE6_CASCADE_AUDIT.md`) was non-trivial and pinned a lot of
   Gemma-specific behaviour around cascades, dirty checking, and the
   `MySQL57InnoDBDialect`. Repeating that lift for ORM 7 has not been
   scoped. **Effort: ~weeks**, mostly external (audit Spring 6.2's ORM 7
   compatibility, redo cascade audit, smoke every entity loader). The work
   is not currently on the Phase 3 roadmap (per
   `project_release_plan.md`).

3. **`HibernateConfig.resolveSearchIndexBase` workaround.** The HS 7 quirk
   where a blank `directory.root` defaults to CWD (commit `04d720c666`)
   may or may not survive HS 8 — the workaround is harmless (always
   forcing an absolute path), but if HS 8 changed the default the
   workaround becomes a no-op rather than load-bearing. Worth re-running
   the empty-`gemma.search.dir` regression test on the first HS 8 build,
   but no advance code change needed.

Lower-risk items, listed for completeness:

- The Step-5 highlighter design in `HibernateSearchSource.java` (lines 138-175,
  256-260, 356-368) intentionally **avoids** HS 7's native
  `f.highlight(field)` projection to dodge the `highlightable = Highlightable.ANY`
  schema requirement. This was a deliberate choice to keep the Step-6
  reindex cost down; HS 8 does not affect it.
- The `lucene-backward-codecs` jar will need a version bump in lockstep
  with `lucene-core` (the convergence pin already covers this).
- `LuceneOntologySearchIndex.parser.setAllowLeadingWildcard(true)` — the
  semantics in Lucene 10 are unchanged but it's worth confirming via the
  ontology search test suite on first build.

## Effort table

| Area | Item | Bucket |
|---|---|---|
| Lucene direct API | `LuceneOntologySearchIndex` (in-memory Jena → Lucene index) | 1 hour — verify on Lucene 10, no API breaks expected |
| Lucene direct API | `LuceneQueryUtils` (query-parser DSL) | 0 work |
| Lucene direct API | `LuceneParseSearchException` | 0 work |
| Lucene direct API | `JenaTextOntologySearchService` | BLOCKED (jena-text pin); >= 1 week to migrate off jena-text |
| HS annotations | 24 entity classes | 0 work — all on the HS 6+ POJO mapping |
| HS call sites | `HibernateSearchSource`, `IndexerServiceImpl` | 0 work — already on the HS 7 DSL HS 8 retains |
| HS config | `HibernateConfig` search properties | 0 work — already on the HS 6+ key namespace |
| HS framework | ORM 6.6 → ORM 7 migration | BLOCKED; weeks of work, not scoped |
| Build/dep | Bump `lucene.version`, `hibernate.search.version` in `pom.xml` + drop the `9.7.0 → 9.11.1` convergence override | 0 work (the bump itself) once the above blockers clear |
| Test | Empty-`gemma.search.dir` regression (HS 7 CWD quirk) | 1 hour — re-run the regression guard on first HS 8 build |

**Aggregate Gemma-side cost (excluding upstream blockers):** ~1 hour of
verification work. The pure Gemma surface is forward-compatible.

**Aggregate cost including upstream blockers:** weeks (ORM 7 migration +
jena-text replacement).

## Bottom-line recommendation

**Stay on Lucene 9.11.1 / Hibernate Search 7.2.x.**

The Gemma-side surface is forward-compatible — no entity-annotation rewrites,
no analyzer rewrites, no DSL rewrites needed. But two upstream pins make the
bump unfeasible:

- **`jena-text` 4.10.0 pins Lucene 9.** Replacing `jena-text` with a direct-
  Lucene index over the TDB is feasible (the `LuceneOntologySearchIndex`
  proof-of-concept already exists) but a ~1-week project on its own.
- **HS 8 requires Hibernate ORM 7.** The 6.6 → 7 ORM migration is weeks of
  work and is not on any current roadmap.

The bumps do not buy anything material for Gemma's use case: full-text
search over tens of MB of indexed entity text from MySQL, with millisecond-
class queries. Lucene 10's headline wins (KNN / vector-search performance,
some sequential-scan improvements) are not load paths Gemma exercises. HS 8's
headline wins (Quarkus integration, projection-constructor sugar) overlap
zero with Gemma's stack.

Revisit when **either** the ontology refactor (drop `jena-text`) lands,
**or** the Hibernate ORM 7 migration gets queued. At that point, the Gemma-
side bump itself becomes a `<lucene.version>` + `<hibernate.search.version>`
flip plus the 1-hour verification pass listed above. Until then, the
current stack is the right place to sit.
