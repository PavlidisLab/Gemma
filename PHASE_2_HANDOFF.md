# Phase 2 (Spring 6 / Hibernate 6 / jakarta) — handoff

Filed 2026-05-17. The previous session ended mid-Phase-2 because the
deletion-stub-fix loop went negative-progress (each delete cascading to
2-3 more compile errors). This doc tells a fresh agent exactly where
things stand and how to finish the job — with the user's explicit
permissions to do the substantial rewrites that block it.

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
