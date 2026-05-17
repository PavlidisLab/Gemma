# Phase 2 (Spring 6 / Hibernate 6 / jakarta) — handoff

Filed 2026-05-17, refreshed at end-of-session-3 second push (still 2026-05-17).
**gemma-core and gemma-cli now compile clean.** The only build blocker left
is `org.glassfish.jersey.ext:jersey-spring3:3.1.10` which Jersey 3 stopped
publishing — that is Step 6 (Jersey 3 migration) work. Coexpression was
deleted in this push at the user's call ("old and fallow").

## Session-3 second push (on `phase2`)

```
0cbca4df84 Phase 2 Step 3 (closeout): delete coexpression subsystem; gemma-core + gemma-cli compile clean
```

Original session-3 commits below.

---

Original session-3 message:
Session-3 rewrote `BusinessKey` and 22 concrete DAOs off the
Hibernate Criteria API (mostly to HQL, a few to JPA Criteria),
swapped `org.hibernate.Query` for `org.hibernate.query.Query`, fixed
Spring 6's removed single-arg `Assert.*` overloads (97 callsites
across 49 files via a paren-balanced Python pass), and inlined a
MySQL bitwise-AND that used to route through the dialect's
`SqlFunctionRegistry`. **gemma-core compile errors went from 200
(45 files) to ~100 unique (21 files).** Most of the residue is
Hibernate-6 API drift that was masked by the earlier showstoppers
plus a few stubbed-but-still-uncovered Criteria callsites.

## Session-3 commit (on `phase2`)

```
897acb3d79 Phase 2 Step 3: rewrite BusinessKey + 22 DAOs on JPA Criteria / HQL
```

Plus the session-2 commits below.

## Session-2 commits (on `phase2`)

```
eee579253c Add PHASE_2_HANDOFF.md (cherry-picked from renovations)
6f12e10b74 Phase 2 Step 3/4: MySQL57 dialect + delete Criteria utils
b465772e65 Phase 2 Step 3 (partial): rewrite filtering-DAO abstractions
811a2fcef6 Phase 2 Step 3+4 (partial): rewrite AbstractDao / QueryUtils / HibernateUtils
4a79164013 Phase 2 Step 5b: gut EhCache 2 cache subsystem
ed93c2f023 Phase 2 Step 2: stub/delete search subsystem cascade
ab94b884a4 WIP: Phase 2 (Spring 6 / Hibernate 6 / jakarta) — in-progress (was the WIP head)
```

Verify with `git -C ~/Dev/eclipseworkspace/Gemma log --oneline phase2`.

## Compile state at end of session-3

```bash
cd ~/Dev/eclipseworkspace/Gemma && git checkout phase2
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -P fast -Denforcer.skip=true -pl gemma-core -am compile -DskipTests=true -q 2>&1 \
  | grep '^\[ERROR\] /' | sort -u | wc -l
# expect: ~100 unique errors in ~21 files
```

The biggest remaining offenders at session-3 end:

```
 13 CharacteristicDaoImpl                      (createSQLQuery / NativeQuery API drift)
 13 CoexpressionDaoImpl                        (still has stale Criteria refs)
 11 QuantitationTypeDaoImpl                    (TypedResultTransformer no longer exists)
  9 ArrayDesignDaoImpl                         (createSQLQuery / SessionFactoryImplementor drift)
  8 AbstractPersister                          (FlushMode -> FlushModeType, Serializable -> Object)
  6 DifferentialExpressionAnalysisDaoImpl      (Criteria conversion not yet done)
  6 ExpressionAnalysisResultSetDaoImpl         (residue from the UOE-stubbing)
  5 DifferentialExpressionResultDaoImpl        (Criteria conversion not yet done)
  5 AbstractFilteringVoEnabledDao              (Criteria refs that escaped session 2)
  4 AuditEventDaoImpl                          (createCriteria refs)
  3 PrincipalComponentAnalysisDaoImpl, CompressedStringListType, ByteArrayType
  2 AbstractCuratableDao, ExpressionPersister, BeanInitializationTimeMonitor
  1 H2Dialect, AuditAdvice, AuditTrailServiceImpl, BioAssayDaoImpl,
    ProcessedDataVectorByGeneCacheImpl
```

## TL;DR for a fresh session (next session-4)

`gemma-core` and `gemma-cli` compile clean. `gemma-rest` fails at
dependency resolution because Jersey 3 stopped publishing
`org.glassfish.jersey.ext:jersey-spring3`. That's the next blocker.

Resume on `phase2` branch. Work in this order:

1. **Step 6 — Jersey 3 migration of `gemma-rest`.** The pom needs
   `jersey-spring3` removed and Jersey 3's Spring integration replaced
   with either:
   - direct `jakarta.ws.rs` + Spring's own JAX-RS adapter, OR
   - the `jersey-spring6` artifact (recent versions), OR
   - inversion: register Spring's `RequestMappingHandlerMapping` and
     drop Jersey altogether for the REST layer.
   Pavlab convention to date has been Jersey + Spring DI; check what
   the gsec recipe uses if it has a REST module.
   After the pom is fixed, the gemma-rest java sources may still need
   the same `javax.* → jakarta.*` sweep + Hibernate 6 drift fixes that
   gemma-core just went through; chase compile errors there.
2. **Step 4b — Strip coexpression from `gemma-web`.** Session 3 deleted
   the model + DAO + service + CLI surface, but gemma-web's
   `CoexpressionSearchController`, `LinkAnalysisController`, and
   `CoexSearchTaskCommand` were deleted in this push too — verify nothing
   in `gemma-web` still imports any `coexpression` symbol (other than
   `SampleCoexpression*` which we kept).
3. **Step 5a — Spring 6 JPA migration.** Replace
   `LocalSessionFactoryBean` + `HibernateTransactionManager` with Spring 6
   JPA + the unwrap pattern from gsec. Until this lands, app context
   bootstrap will fail at runtime even though compile is clean.
4. **Step 7 — selective per-module tests.** Do NOT run the full suite
   (Paul: ~30 min); pick a representative DAO test per module to verify
   the BusinessKey / JPA Criteria / TypedResultTransformer migrations
   work at runtime. Expect issues — the `applyTo`/`list`/`uniqueResult`
   path through `TypedResultTransformer` is a new pattern that wasn't
   exercised pre-Phase-2.
5. **Steps 8–10** — bytecode 11→17 (in root pom), re-enable
   `dependencyConvergence` enforcer, update RENOVATIONS.md.

## What this session changed (cheat sheet for grep)

Hibernate 6 API replacements applied across gemma-core:

| Old (gone) | New |
|---|---|
| `org.hibernate.SQLQuery` | `org.hibernate.query.NativeQuery<?>` |
| `session.createSQLQuery(s)` | `session.createNativeQuery(s)` |
| `org.hibernate.Query` (deprecated) | `org.hibernate.query.Query<T>` |
| `query.setLong/setString/setDouble/etc` | `query.setParameter` (PreparedStatement still uses setLong) |
| `query.setFlushMode(...)` | `query.setHibernateFlushMode(...)` |
| `query.setResultTransformer(X)` | `X.list(query)` / `X.uniqueResult(query)` via TypedResultTransformer helpers |
| `Restrictions.* / Projections.* / Criteria` | JPA Criteria (jakarta.persistence.criteria) or plain HQL |
| `sessionFactory.getClassMetadata(X)` | `sessionFactory.getMetamodel().entity(X)` or `((SessionFactoryImplementor) sf).getMappingMetamodel().getEntityDescriptor(X)` |
| `sessionFactory.getCache().evictCollection(role, id)` | `evictCollectionData(role, id)` |
| `Dialect.registerFunction(name, fn)` | Use a `FunctionContributor`; or inline the SQL (we did the latter for `bitwise_and`) |
| `UserType.sqlTypes() : int[]` | `UserType.getSqlType() : int` |
| `UserType.nullSafeGet(rs, String[], session, owner)` | `UserType.nullSafeGet(rs, int position, session, owner)` |
| `UserType` (raw) | `UserType<T>` (generic) |
| `IdentifierGeneratorHelper.getGeneratedIdentity(rs, idProp, idType, dialect)` | `(idProp, rs, PostInsertIdentityPersister, WrapperOptions)` |
| `Transformers.aliasToBean(X.class)` | still works in HB6 (deprecated) |
| `setResultTransformer(...).list()` | TypedResultTransformer.list(query) |

Spring 6 single-arg Assert removals:

| Old | New |
|---|---|
| `Assert.notNull(x)` | `Assert.notNull(x, "must not be null")` |
| `Assert.isTrue(b)` | `Assert.isTrue(b, "expected true")` |
| `Assert.state(b)` | `Assert.state(b, "illegal state")` |
| `Assert.isNull(x)` | `Assert.isNull(x, "must be null")` |
| `Assert.hasText(s)` | `Assert.hasText(s, "must not be blank")` |
| `Assert.hasLength(s)` | `Assert.hasLength(s, "must not be empty")` |

Apply with `/tmp/fix_asserts.py` — point it at `/tmp/assert_errors.txt`
(the file is just the lines of `mvn ... | grep '^\[ERROR\]' | sort -u`
matching the relevant patterns). It does paren-balanced argument
parsing so it handles nested calls.

Spring 6 BeanPostProcessor:

| Old | New |
|---|---|
| `postProcessPropertyValues(PropertyValues, PropertyDescriptor[], Object, String)` | `postProcessProperties(PropertyValues, Object, String)` |

## Stubbed / temporarily disabled

Things that compile but throw UOE or return empty — track for Step 7
follow-up:

- `ExpressionAnalysisResultSetDaoImpl#findByBioAssaySetInAndDatabaseEntryInLimit`
  → UOE. The `getFilteringCriteria` method was deleted entirely; the
  filtering-by-VO path needs a JPA-Criteria reimplementation.
- `ExpressionDataFileServiceImpl#writeOrLocateCoexpressionDataFile` → UOE
  (coexpression subsystem gone).
- Single-cell vector streaming in EE DAO lost its per-row
  `setResultTransformer(initializer)` call; the consumer must invoke
  `SingleCellDataVectorInitializer.transformTuple` itself when reading
  from the stream. (See the comment at the streamSingleCellDataVectors
  callsite.)
- `AbstractFilteringVoEnabledDao#registerEntity` was rewritten on JPA
  Metamodel — the old code branched on `MaterializedBlobType`/
  `MaterializedClobType`/`CustomType` for skip-fields; the new code
  falls back to `log.trace` for any non-basic, non-association,
  non-collection attribute. If anyone reports "my BLOB column shows up
  as a filterable property", that's why.
- All `CoexpressionAnalysis` entities, DAOs, services, caches, link
  analysis services, GeneCoexpressionSearchService, LinkAnalysisCli,
  CoexpressionWriter, and the species-specific
  `Gene2GeneCoexpression`/`ExperimentCoexpressionLink` subclasses are
  **deleted**. `SampleCoexpression*` is **kept** (it's a per-EE sample QC
  artifact, not gene-gene coexp).
- Some EE DAO `Initializer` classes are typed `TypedResultTransformer<Object>`
  because they return either an entity or `Object[]` depending on a flag.
  Callers cast the result back to the entity. Not ideal but matches the
  pre-Phase-2 behavior.

## Things I tried that wasted time

- Trying to give `Query` a `setResultTransformer` extension method via a
  wrapper — too invasive given Hibernate's API surface. The
  `TypedResultTransformer.list(query)` / `uniqueResult(query)` helper on
  the transformer side was much cleaner.
- Initial Python script for `setResultTransformer` rewrite walked
  comments incorrectly and mashed two statements onto one line in a
  handful of cases. Fixed by hand on the few callsites that broke.
- Sed for `setLong/setString → setParameter` was too aggressive — it
  also swapped `PreparedStatement.setLong` (which is correct) into
  `setParameter` (which doesn't exist on PreparedStatement). Reverted the
  three or four PreparedStatement callsites by hand.

## Notes on session-3 design choices

* **`BusinessKey` public API**: every `addRestrictions(Criteria, X)` /
  `createQueryObject(...)` / `attachCriteria(...)` is gone. The new
  surface per entity type is:
  - `BusinessKey.find(Session, X) -> X` — full single-result query.
  - `BusinessKey.matches(CriteriaBuilder, From<?, X>, X) -> List<Predicate>`
    — predicate-list builder for callers composing into a larger
    JPA-Criteria query of their own.
  - `BusinessKey.attachBioSequence` / `attachDatabaseEntry` — convenience
    wrappers that join a parent's property and add the matching
    predicates in one call.
  - All `checkKey` / `checkValidKey` methods are unchanged.
* **Spring 6 `Assert.*` single-arg removals**: Spring 6 dropped
  `Assert.notNull(Object)`, `Assert.isTrue(boolean)`, `Assert.state(boolean)`,
  `Assert.isNull(Object)`, `Assert.hasText(String)`, `Assert.hasLength(String)`.
  Session 3 patched 97 callsites in 49 files with a default message via
  `/tmp/fix_asserts.py` (paren-balanced argument parsing, default
  messages keyed on method name like "must not be null" / "expected
  true"). If new ones appear after this session's fixes uncover more
  compile-down stack, the same script will pick them up — point it at
  a fresh `/tmp/assert_errors.txt`.
* **MySQL bitwise AND**: `AclQueryUtils` and `EE2CAclQueryUtils` no
  longer route through `sessionFactory.getSqlFunctionRegistry()` (which
  changed shape in Hibernate 6). The rendered SQL is just `(a & b)`
  inlined. Gemma is MySQL-only via `MySQL57Dialect`, so the dialect
  indirection was carrying no weight.
* **`createSQLQuery` vs `createNativeQuery`**: `createSQLQuery` still
  exists on Hibernate 6's `Session` (deprecated) and returns
  `org.hibernate.query.NativeQuery`. Existing callsites compile; only
  the return-type variable's declared type changed from `SQLQuery` to
  `NativeQuery` (the only mechanical fix needed).
* **`org.hibernate.metadata.ClassMetadata`** still works in Hibernate 6
  (kept for backward compat). `sessionFactory.getClassMetadata(X)` is
  fine. If we hit `getAllClassMetadata()` issues, swap to
  `sessionFactory.getMetamodel().getEntities()` (already done in
  `QuantitationTypeDaoImpl` constructor).

## TL;DR for a fresh session

You're picking up a Spring/Hibernate/jakarta climb that has finished
Phase 0 (JDK 17), Phase 1a (Spring 3 → 4), Phase 1b (Spring 4 → 5 +
Hibernate 4 → 5 + HS 4 → 5 + Lucene 3 → 5), and Phase 1c (DWR
stripped). Phase 2 (Spring 6 + Hibernate 6 + jakarta) is partly done
and parked on a branch.

Three repos, all local-only on `renovations` branches, **nothing
pushed to GitHub**:

| Repo | Branch | State |
|---|---|---|
| `~/Dev/eclipseworkspace/Gemma` | `renovations` | Phase 1c, all 5 modules compile, ~95% unit tests pass |
| `~/Dev/eclipseworkspace/Gemma` | `phase2-wip` | Phase 2 attempt, **does not compile** — your starting point |
| `~/Dev/gsec/gsec` | `renovations` (HEAD: `44ecead`) | Spring 6 + Hibernate 6 + jakarta, **all 25 tests pass** — the recipe |
| `~/Dev/eclipseworkspace/baseCode` | `renovations` | Lucene + R support gutted, builds clean |

Read [RENOVATIONS.md](RENOVATIONS.md) for the full phase history.

## User decisions (explicit permissions granted)

These are the load-bearing decisions Paul made before the previous
session ended. You do not need to re-litigate them.

1. **Hibernate Search is dead. Rewrite search completely.** The
   existing Lucene/HS-based search subsystem (HibernateSearchSource,
   IndexerService, the @Indexed/@Field entity annotations, the
   /search REST endpoints) can be replaced wholesale. You decide the
   new architecture (Lucene 9 native, OpenSearch, MySQL FULLTEXT,
   Postgres FTS — your call). For now you may stub the search service
   to return empty results; the rewrite is its own future phase.
2. **Hibernate Criteria → JPA Criteria "just needs to be done."**
   No shortcut. Convert every DAO that uses `org.hibernate.criterion`
   to JPA Criteria (or HQL where simpler). Write more tests if you
   need them.
3. **Drop EhCache temporarily.** EhCache 2 → EhCache 3 + JCache
   integration is too much surface for now. Replace caches with
   something simpler (Caffeine via JCache, ConcurrentMapCache, or
   no L2 cache at all). Cache stats UI (`CacheMonitorImpl`) can be
   stubbed.
4. **No push to GitHub.** All work stays local on `renovations`
   branches across the three repos.
5. **agents (`~/Dev/gemma-curation-agents/`) are still WIP.** Don't
   build the real write-API for them yet — the Pydantic schemas are
   in flux.
6. **GemBrow React port** is being done by a *different* agent in
   `~/Dev/GemBrow/` (see `~/Dev/GemBrow/REACT_PORT_HANDOFF.md`). It
   doesn't touch Gemma. You don't touch GemBrow.

## What's at `phase2-wip`

```
git -C ~/Dev/eclipseworkspace/Gemma checkout phase2-wip
```

Single commit on top of `079c79349e` (the Phase 1c head):

- **pom.xml** bumped to Spring 6.1.20, Spring Security 6.3.10,
  Hibernate 6.4.10 (`org.hibernate.orm`), Tomcat 10.1.34, Jersey
  3.1.10, jakarta.servlet 6.0.0, jakarta.xml.bind-api 4.0.2,
  ehcache 3.10.8 jakarta + `hibernate-jcache` + jaxb-runtime 4.0.5
  pinned (the snapshot jaxb-runtime in ehcache's transitive is
  excluded). Hibernate Search and Lucene deps removed entirely.
  `dependencyManagement` includes the
  `hibernate-commons-annotations` 6.0.6 override that the parent
  pom otherwise pins to 4.0.5 (Hibernate 4 era — missing
  `ReflectionManager.reset()`).
- **`javax.* → jakarta.*` mass sed** applied across ~400 imports
  in gemma-core/cli/rest/web. JSR-305 (`javax.annotation.Nullable`
  etc.) and JDK `javax.*` (swing/imageio/sql/xml.xpath) left
  alone — they don't move.
- **209 Lucene + Hibernate Search annotation lines** stripped from
  26 entity files via a Python script that also dropped the
  matching imports.
- **Search subsystem files deleted**: `core/search/lucene/` (all 5
  files), `HibernateSearchSource`, `HibernateSearchException`,
  `DefaultHighlighter`, `IndexerServiceImpl`,
  `metrics/binder/cache/EhCache24Metrics`,
  `metrics/MeterRegistryEhcacheConfigurer`. 5 search-related test
  files also deleted.
- **gemma-web DWR removal** carried over from Phase 1c.

**The compile is currently broken at this commit** — that's the
whole point of the handoff.

## The cascade you'll need to finish

These are the production files that still reference deleted search
classes (as of last compile attempt):

```
gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyUtils.java
gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyServiceImpl.java
gemma-core/src/main/java/ubic/gemma/core/search/source/OntologySearchSource.java
gemma-core/src/main/java/ubic/gemma/core/search/source/GeneOntologySearchSource.java
gemma-core/src/main/java/ubic/gemma/core/search/source/DatabaseSearchSource.java
gemma-core/src/main/java/ubic/gemma/core/search/SearchServiceImpl.java
gemma-core/src/main/java/ubic/gemma/core/search/GeneSetSearchImpl.java
gemma-rest/src/main/java/ubic/gemma/rest/SearchWebService.java
gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java
gemma-rest/src/main/java/ubic/gemma/rest/AnnotationsWebService.java
gemma-web/src/main/java/ubic/gemma/web/controller/search/GeneralSearchController.java
gemma-web/src/main/java/ubic/gemma/web/controller/monitoring/CacheMonitorImpl.java
```

Plus untouched-by-the-Phase-2-attempt deeper issues:

- Hibernate 5 → 6 Criteria removal in DAOs that still compile
  under Phase 1c (`org.hibernate.criterion` is gone). Affects
  multiple DAOs in gemma-core. gsec's `AclDaoImpl` already shows
  the pattern: convert the Criteria query to HQL (the simpler
  option) or JPA Criteria.
- Spring 5 → 6 deprecated-API removals (probably dozens of small
  fixes once everything else compiles).
- Jersey 2 → 3 might surface API changes in `gemma-rest`'s
  providers/filters (not yet probed).

## Suggested playbook for the new session

### Step 0 — get oriented

```bash
# Read the two docs
cat ~/Dev/eclipseworkspace/Gemma/RENOVATIONS.md
cat ~/Dev/eclipseworkspace/Gemma/PHASE_2_HANDOFF.md   # this file

# Verify the Phase 1c baseline still works
cd ~/Dev/eclipseworkspace/Gemma
git status                                              # should be on renovations
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -P fast -Denforcer.skip=true clean install -DskipTests=true
# expect: BUILD SUCCESS for 5 modules (~50s)

# And gsec at Spring 6 still builds in isolation
cd ~/Dev/gsec/gsec
git log --oneline -3   # HEAD should be 44ecead
mvn install -Denforcer.skip=true
# expect: 25 tests pass; jar installed
```

After Step 0 you should have a clear picture of where the boundaries
are.

### Step 1 — switch into Phase 2 working state

```bash
cd ~/Dev/eclipseworkspace/Gemma
git checkout phase2-wip       # the WIP commit
git checkout -b phase2         # work on a new branch named "phase2"

# Reinstall gsec at Spring 6 so Gemma can consume it
cd ~/Dev/gsec/gsec
mvn install -Denforcer.skip=true   # this installs the Spring 6 jar
```

You're now on a `phase2` branch whose parent is the WIP commit.
Commit your fixes incrementally so you can bisect if something
regresses.

### Step 2 — make the search subsystem compile (stubs first)

Delete or stub the 11 production consumers of deleted search
classes. Order suggested:

1. `SearchServiceImpl` — stub `search()` to throw
   `UnsupportedOperationException` or return empty
   `SearchResultMap`. Same for `loadValueObjects`.
2. `OntologySearchSource`, `GeneOntologySearchSource`,
   `DatabaseSearchSource` — delete entirely; remove their
   `@Component` registrations. `CompositeSearchSource` (if it
   composes them) becomes a no-op.
3. `OntologyServiceImpl` / `OntologyUtils` — delete the Lucene
   query construction methods; leave ontology lookup methods
   that don't use Lucene intact.
4. `GeneSetSearchImpl` — delete or stub the methods that need
   Lucene.
5. `gemma-rest/.../SearchWebService.java` and `AnnotationsWebService`
   and `DatasetsWebService` `/search` endpoints — make them
   return 501 Not Implemented or an empty result set with a
   header noting "search disabled on this build."
6. `gemma-web/.../GeneralSearchController` — same treatment;
   probably just delete the form processing.
7. `gemma-web/.../monitoring/CacheMonitorImpl` — replace the
   ehcache 2 introspection with a simple `cacheManager.getCacheNames()`
   list, no statistics.

After this round you should be looking at Hibernate 6 / Spring 6
errors rather than search-cascade errors.

### Step 3 — Hibernate Criteria → JPA Criteria (or HQL)

For each DAO in gemma-core that uses `org.hibernate.criterion.*`
(grep for `import org.hibernate.criterion`), convert the Criteria
queries. gsec's `AclDaoImpl.loadAcls()` shows the HQL pattern.

If a DAO's Criteria query is small, HQL is easier. For complex
dynamic queries with conditionals, use the JPA Criteria API
(`CriteriaBuilder` / `CriteriaQuery`) — but be prepared for it to
be verbose.

Write new unit tests for the converted DAOs. Paul has explicitly
said "write more tests if you need to" — use that license.

### Step 4 — get the rest of Hibernate 6 to compile

Expect to fix small things:
- `session.getFlushMode()` → `getHibernateFlushMode()`
- `sessionFactory.getClassMetadata(class)` →
  `((SessionFactoryImplementor) sf).getMappingMetamodel().getEntityDescriptor(class)`
- `CascadingAction.X` → `CascadingActions.X`
- `IdentifierGeneratorHelper.getGeneratedIdentity` signature gained
  a Dialect arg
- `Expectation.verifyOutcome` signature gained a sql String arg
- `Configuration.generateSchemaCreationScript()` removed —
  `DatabaseSchemaPopulator`'s inner class is already a stub on the
  Phase 1c head; verify it's still stubbed on phase2-wip.
- Hibernate `org.hibernate.SQLQuery` → `org.hibernate.query.NativeQuery`
- 1-based positional parameters (was 0-based in HB 4)

All of these were already done in Phase 1b/Phase 2-attempt-1 for
some files — but the mass sed and the Hibernate-6-only bumps may
have introduced new instances. Grep methodically.

### Step 5 — Spring 6 leftovers

- Spring's `org.springframework.orm.hibernate5` package was *kept*
  in Spring 6 but only works with Hibernate 5. With Hibernate 6,
  use Spring's JPA support: `LocalContainerEntityManagerFactoryBean`
  +  `JpaTransactionManager`, then expose a `SessionFactory` bean
  via `factory-method="unwrap"`. gsec's `testContext.xml` shows
  the pattern.
- Gemma's custom `HibernateTransactionManager` and
  `LocalSessionFactoryBean` (which extend Spring's hibernate4/5
  classes) need replacement. Probably delete them and use Spring's
  JPA classes directly with the unwrap trick.

### Step 6 — Jersey 2 → 3

`gemma-rest/pom.xml` deps will need bumping when you get there.
`jersey.version` in the root pom is already set to `3.1.10`.
`@PreMatching` and other annotations should work the same way once
the imports are jakarta.

### Step 7 — run unit tests, fix runtime issues

By now you should have a compiling build. The unit tests will
surface runtime issues. Pattern from Phase 1b: ~95% pass after
the compile is clean; the residue is small.

### Step 8 — bump bytecode 11 → 17

Once everything is on Spring 6 (which itself requires Java 17),
flip `maven.compiler.release` from `11` to `17` in the root pom.
This was a Phase 0 leftover.

### Step 9 — re-enable `dependencyConvergence` enforcer

Was disabled across all three repos during the climb. Re-enable
and chase the convergence errors with explicit
`dependencyManagement` overrides.

### Step 10 — update `RENOVATIONS.md`

Move Phase 2 from "deferred" to "done." Document any new stubs.

## Things I tried that wasted time

So you don't repeat them:

- **Spring 6's `org.springframework.orm.hibernate5.LocalSessionFactoryBean`
  with Hibernate 6** → fails at runtime with `NoSuchMethodError:
  ReflectionManager.reset()`. The hibernate5 package was kept for
  source compat with Hibernate 5 only; with Hibernate 6 you must
  use Spring's JPA support.
- **The ehcache 3.10.x jakarta classifier without a jaxb-runtime
  override** → its transitive `jaxb-runtime 2.3.0-b170127.1453` is
  a snapshot from the decommissioned `maven.java.net` repo. Always
  pair with `<exclusion>` + a direct `jaxb-runtime 4.0.x` pin.
- **Trying to align Spring framework + Spring Security to the
  same patch version** → use `${spring.version}` 6.1.20 (one minor
  below the latest in the line) so Spring Security 6.3.10's
  transitive doesn't conflict. Or just disable the enforcer
  rule during the climb.

## File system layout summary

```
~/Dev/eclipseworkspace/Gemma                  # main monorepo
  - renovations branch    : Phase 1c, working
  - phase2-wip branch     : Phase 2 partial, broken
  - RENOVATIONS.md        : full phase history
  - PHASE_2_HANDOFF.md    : this file

~/Dev/gsec                                    # Pavlab security lib
  - renovations branch    : HEAD at 44ecead (Spring 6, all tests pass)
                            HEAD~1 at 6bbccb3 (Spring 5, also works)

~/Dev/eclipseworkspace/baseCode               # Pavlab utility lib
  - renovations branch    : 1.1.34-RENOVATIONS-SNAPSHOT (Lucene/R gutted)

~/Dev/GemBrow                                 # parallel React port (different agent)
  - REACT_PORT_HANDOFF.md : that other agent's brief; don't touch

~/Dev/gemma-curation-agents                   # WIP, do not touch
~/Dev/gemma-curation-ui                       # the React app gemma-ui will grow into
```

## Local maven state (heads up)

The local maven repo (`~/maven.repository/`) currently has the
Spring 5 gsec jar installed (so the renovations branch builds).
When you start Phase 2:

```bash
cd ~/Dev/gsec/gsec && mvn install   # installs the Spring 6 gsec jar
```

Gemma's `renovations` branch will then fail to build (it expects
Spring 5 gsec). That's fine — you're working on `phase2` from
that point.

## Status summary

- **gsec at Spring 6**: ✅ committed `44ecead`, 25/25 tests pass.
  Recipe is proven.
- **Gemma compile under Spring 6**: ⏳ in `phase2-wip` branch, ~13
  source files still failing due to deleted-search-subsystem
  cascade. Stubbing/deleting them is straightforward Step 2 work.
- **Gemma Hibernate 6 Criteria conversion**: ⏳ not started. The
  user explicitly said this "just needs to be done."
- **Gemma cache UI / Ehcache 3 integration**: ⏳ not started. The
  user said dropping ehcache temporarily is acceptable.
- **Gemma unit tests under Spring 6**: ⏳ unmeasured.

Realistic estimate to compile-green: one focused session.
Realistic estimate to tests-green: another session after that.
