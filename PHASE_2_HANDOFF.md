# Phase 2 (Spring 6 / Hibernate 6 / jakarta) — handoff

Filed 2026-05-17, refreshed at end-of-session-5 (with session-5 still in
flight via a couple of parallel agents — refresh against `git log` if
this is older than your HEAD). **Phase 2 is substantially done; the
integration tier is now in flight.** Steps 8 and 9 closed (bytecode
11→17, `dependencyConvergence` enforcer re-enabled with 8 transitive
pins); the legacy SHA-with-username-salt password-hash migration
landed (Task C); the JPA Criteria filter machinery now handles subquery
+ `.size` filters and `Sort.NullMode` (Tasks D + E); the failsafe
bootstrap is wired up and now boots a real MySQL test DB; and the
broader unit sweep across gemma-core / gemma-cli / gemma-rest is
broadly green.

## Tests passing under Phase 2 (this session)

**Unit tier — broadly green across 4 modules:**

| Module | Tests | Notes |
|---|---|---|
| gemma-core (DAO/service/util/model) | ~215 smoke + sweep cleanup | DAO smoke set + broader unit sweep; major Hibernate-6 session-state clusters cleared (ExpressionExperimentDaoTest 53/0/0, SingleCellExpressionExperimentServiceTest 17/0/0, FactorValue* tests green) |
| gemma-cli | 41 (56 in latest re-run) | 0 errors; CLI infra + Spring-context CLI tests |
| gemma-rest | 105 | 0 errors; REST utility + Spring-context controller tests (`DatasetsRestTest` now correctly excluded as `@Category({SlowTest, IntegrationTest})`) |
| gemma-web | 11 | Legacy webapp being replaced by `~/Dev/gemma-curation-ui`, so heavy investment not warranted |

**Integration tier — bootstrap unblocked, sweep now executable.** The
~61 `@Category(IntegrationTest)` tests can now boot against a real
MySQL test DB; commit `cc21cf6504` closed the bootstrap wiring (Spring
6 `<ref local>` removal in `applicationContext-schedule.xml`,
sessionFactory-before-data-INSERT ordering for the
`createDatabaseInitializer` → `sessionFactory` → `dataSourceInitializer`
chain, named-column AUDIT_EVENT INSERTs). See the **MySQL test DB
recipe** section below for run-time invocation.

## Build state

All 5 modules install + test-compile green at **bytecode 17 with
`dependencyConvergence` on**:

```
mvn -P fast install -DskipTests=true:
  Gemma .............................................. SUCCESS
  Gemma Core ......................................... SUCCESS
  Gemma CLI .......................................... SUCCESS
  Gemma REST ......................................... SUCCESS
  Gemma Web .......................................... SUCCESS
```

## Session-5 commits (on `phase2`)

```
2f9a051ac9      Phase 2: ExternalDatabaseServiceTest passes mutable Set into setExternalDatabases
a2db1d8ed4      Phase 2 Task C: legacy SHA + username-salt password-hash migration
3d265bee86      Phase 2: honour Sort.NullMode FIRST/LAST via Hibernate 6 JpaOrder
f14373925b      Phase 2: JPA-Criteria port supports subquery + .size filters
e79e9c82b2      Phase 2: HQL `<plural>.size` -> `size(<plural>)` + stub deleted-coexpression query
63bffbf6fc      Phase 2: AbstractPersister.formatEntity tolerates transient/detached entities
cc21cf6504      Phase 2 Step 7: integration-test bootstrap wiring
12fcea8567      PHASE_2_HANDOFF.md: refresh after Initializer wiring + detached-replace fix
e304d1c2b3      Phase 2: re-resolve managed EE in replaceProcessedDataVectors to dodge stale PersistentSet snapshot
764004c2ea      Phase 2: testReplaceVectors snapshots persisted vectors instead of detached
eee9400743      Phase 2: wire SingleCellDataVectorInitializer via setTupleTransformer
5268d28ac5      PHASE_2_HANDOFF.md: refresh after cellIds delimiter fix
c0719e0213      Phase 2: fix SingleCellDimension.cellIds delimiter mismatch + CompressedStringListType stream impl
1e090a2470      PHASE_2_HANDOFF.md: refresh after BLOB-HQL + Subquery FQN + EE @After
d660c34b63      Phase 2: ExpressionExperimentDaoTest @After re-resolves managed EE before remove
5cb418b360      Phase 2: Subquery#toString uses FQN — fixes 4 EE DAO test methods
2b0028a4e5      Phase 2: BinaryFunctionContributor + getBioAssayDimensions HQL rewrite
bbce3024d9      PHASE_2_HANDOFF.md: refresh after FactorValue + TupleTransformer batches
1d01e08308      Phase 2: complete TypedResultTransformer migration to Hibernate 6 TupleTransformer / ResultListTransformer
1a49a43435      Phase 2: fix FactorValueDaoTest + FactorValueServiceTest
8a37f8529a      PHASE_2_HANDOFF.md: refresh after sweep batches 2 + 3
e60028feb6      Phase 2: @Ignore one BLOB-substring HQL test, document the rest
f0337b7057      Phase 2: three more broader-sweep DAO failures
1b0a1c9418      Phase 2 docs: session-5 closeout (Steps 8 + 9 + sweep cleanup)
c80975d8d8      Phase 2 Step 9: re-enable dependencyConvergence enforcer
8caa98fb4d      Phase 2: pick off two broader-sweep gemma-core unit failures
175407d6b8      Phase 2: fix applicationContext-security.xml for Spring Security 6
4a812a0de0      Phase 2 docs: refresh handoff + RENOVATIONS.md after Step 8
94b7435766      Phase 2 Step 8: bump bytecode 11 -> 17
```

## Session-4 commits (on `phase2`)

```
c41ba711e9      PHASE_2_HANDOFF.md: refresh after Step 7 round 5
46820d1b42      Phase 2 Step 7 (round 5): restore Highlighter on /datasets; broader unit sweep green
d31c268142      Phase 2 Step 7 (round 4): port AbstractCriteriaFilteringVoEnabledDao to JPA Criteria
30aa12702c      Phase 2 Step 7 (round 3): junit-vintage for gemma-rest; CLI + REST tests green
e76e33bf4b      Phase 2 Step 7 (round 2): bulk test fixes (MergeMode, BigInteger->Number, bitand, H2 bitwise)
6a9d20654c      Phase 2 Step 5a/7: native Hibernate bootstrap; first DAO tests green
901effa053      Phase 2 Step 5a: Spring 6 JPA migration of SessionFactory wiring
5ff67490aa      PHASE_2_HANDOFF.md: refresh for full mvn install green; next is Step 5a (JPA)
```

## Session-3 commits (on `phase2`)

```
e49f573b07 Phase 2 Step 7 prep: test-compile clean for all 5 modules
e1a7360b48 Phase 2 Step 6: Jersey 3 migration + gemma-web compile-green; all 5 modules clean
967b9d24ce PHASE_2_HANDOFF.md: refresh for next-session-4 (Step 6 / Jersey 3)
0cbca4df84 Phase 2 Step 3 (closeout): delete coexpression subsystem; gemma-core + gemma-cli compile clean
5a286be03b Phase 2 Step 3 (followup): refresh handoff + last Assert stragglers
897acb3d79 Phase 2 Step 3: rewrite BusinessKey + 22 DAOs on JPA Criteria / HQL
```

Plus the session-2 commits.

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

## TL;DR for a fresh session (next session-6)

Build is install-green and test-compile-green for all five modules
**at bytecode 17 with `dependencyConvergence` on**. Phase 2 is in good
shape. Session 5 ran ~29 commits and closed the bulk of the remaining
work:

- **Step 7 integration tier is now in flight** (was "untouched").
  Commit `cc21cf6504` closed the bootstrap wiring; `mvn verify` now
  boots a real MySQL test DB and runs the failsafe sweep. Latest run
  on `2f9a051ac9`: **471 integration tests, ~197 passing, ~259 errors,
  ~15 failures** — see "Integration sweep error buckets" below.
- **Step 8** (bytecode 11→17) — root-pom change plus `clean install`
  verification (commit `94b7435766`). Spring 6 ships ASM that handles
  class-file v61 cleanly.
- **Step 9** (`dependencyConvergence` enforcer re-enabled — commit
  `c80975d8d8`). 8 transitive-version pins added to root
  `<dependencyManagement>`: micrometer-commons + micrometer-observation
  1.13.11, jaxb-runtime 4.0.5, jakarta.xml.bind-api 4.0.2,
  javax.cache 1.1.1, jackson-core 2.21.0,
  jackson-module-jakarta-xmlbind-annotations 2.19.2, antlr4-runtime
  4.13.2. Full `mvn -P fast clean install` green with the rule on.
- **Task C — legacy SHA+username-salt password migration** (commit
  `a2db1d8ed4`). `GemmaLegacyAwarePasswordEncoder` recognizes both the
  legacy bare-40-char-hex SHA-1 hash (computed as
  `SHA-1(rawPassword + "{" + username + "}")` per Spring Security
  3/4's `ShaPasswordEncoder` + `ReflectionSaltSource`) and a
  `{bcrypt}`-prefixed BCrypt hash. Username flows in via ThreadLocal
  that `LegacyAwareDaoAuthenticationProvider` sets/clears around each
  auth attempt. `upgradeEncoding()` returns true on legacy hashes so
  the next successful login transparently upgrades to bcrypt. The
  production-blocking concern called out in earlier handoff revisions
  is resolved.
- **Tasks D + E — JPA Criteria subquery + `.size` + Sort null-precedence**
  (commits `f14373925b`, `3d265bee86`). `FilterJpaUtils` now builds
  `jakarta.persistence.criteria.Subquery<Long>` for inSubquery /
  notInSubquery filters and uses `cb.size(...)` for `.size`-suffix
  filters; `AbstractCriteriaFilteringVoEnabledDao.buildOrders` casts
  to Hibernate 6's `JpaOrder` (which extends `Order`) to apply
  `NullPrecedence.FIRST/LAST`.
- **Various Hibernate 6 strictness fixes**:
  - `AbstractPersister.formatEntity` now tolerates transient/detached
    entities (commit `63bffbf6fc`). Hibernate 6's
    `Session.getIdentifier()` now throws `TransientObjectException`
    for unattached input; pre-Phase-2 the API was lenient and returned
    null. This was blocking 217 of the 396-test integration sweep
    before the fix — the persist() debug-log construction was
    hard-failing before the actual persist could run.
  - HQL `<plural>.size` → `size(<plural>)` function form (commit
    `e79e9c82b2`); plus a stub of `getExpressionExperimentIdsWithCoexpression`
    (the entity was deleted in Step 3, callers only use it to set
    `hasCoexpressionAnalysis(false)`).
  - `applicationContext-schedule.xml`: 10 occurrences of `<ref local="...">`
    (the `local` attribute was deprecated in Spring 4 and removed in
    later schemas; Spring 6's strict XML schema validation flags it).
    Replaced with `<ref bean="...">`.
  - `applicationContext-dataSourceInitializer.xml` + `applicationContext-hibernate.xml`
    + `applicationContext-dataSource.xml`: reorder the bean depends-on
    chain to `createDatabaseInitializer → sessionFactory → dataSourceInitializer`
    so the SQL data scripts INSERT into tables that exist (Hibernate 6
    materializes them via `hbm2ddl.auto=create`, not by the now-no-op
    `HibernateSchemaPopulator` branch).
  - `sql/init-data.sql`: 3 unnamed-column AUDIT_EVENT INSERTs were
    positional and assumed hbm.xml-order columns; Hibernate 6 doesn't
    preserve that order. Named every column explicitly.

## MySQL test DB recipe (for the failsafe integration sweep)

Local MySQL 5.7.31 on `localhost:3306` (matches `docker-compose.yml`'s
`mysql:5.7`); root password lives in macOS Keychain under the entry
`mysql-root`. **Neither the password nor the destructive
`hbm2ddl.auto=create` flag is persisted to disk**; both are resolved
at script entry.

```bash
cd ~/Dev/eclipseworkspace/Gemma
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -P fast verify -pl gemma-core \
    -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
    -Dgemma.hibernate.hbm2ddl.auto=create
```

On every run the bootstrap drops + recreates `gemdtest`, Hibernate
materializes the schema from the hbm.xml mappings via
`hbm2ddl=create`, then `dataSourceInitializer` runs `init-acls.sql` +
`init-entities.sql` + `mysql/init-entities.sql` + `init-data.sql`.

## Integration sweep error buckets

Latest run on `2f9a051ac9` (snapshot in
`gemma-core/target/failsafe-reports/`): **471 tests, 259 errors,
15 failures, ~197 passing**. The errors cluster strongly — fixing one
root cause typically clears tens of tests at once:

| Count | Type | Notes |
|---|---|---|
| 110 | `AccessDeniedException` | "Access is denied" / "Authentication administrator has NO permissions to the domain object BioMaterial Id=...". Concentrated in test fixture bootstrap — possibly a per-test ACL grant that the new auth wiring is rejecting. **In flight under another agent.** |
| 78 | `NullPointerException` | 31 of these are `AclObjectIdentity.equals` reading `this.type` null — gsec-side bug, lives in `~/Dev/gsec`. 8 are `Identifiable.getId()` on a null entity (test-fixture shape). |
| 22 | `HibernateException` | All 22 are "identifier of an instance of `ubic.gemma.model.genome.Gene` was altered from null to N" — a test fixture is mutating an id after persist, or 2nd-level cache is handing back a stale Gene. |
| 19 | `UnsupportedOperationException` | All 19 are at `org.hibernate.collection.spi.PersistentSet.clear` — same shape as the `ExternalDatabaseServiceTest` immutable-collection fix in `2f9a051ac9`; test fixtures pass `Collections.singleton(...)` / `Collections.unmodifiableSet(...)` into HB6's merge path which then clears() it. Mechanical sweep. |
| 15 | `AssertionError` | Mix of real test assertions. |
| 7 | `IllegalArgumentException` | |
| 4 | `BatchInfoPopulationException` | Domain-specific. |
| 3 | `IncompatibleClassChangeError` | Likely a stale-class-on-classpath or `mvn clean` needed. |
| 3 | `TestCouldNotBeSkippedException` | JUnit runner bookkeeping. |
| 2 | `PathElementException` | "Could not resolve attribute 'value' of `ExpressionAnalysisResultSet`" — HQL property-path drift, mechanical. |
| 2 | `ObjectDeletedException` | |
| 2 | `ConstraintViolationException` | |
| 2 | `NoClassDefFoundError` | `org/quartz/impl/JobDetailImpl` — Quartz dep is missing or wrong scope. |
| 1 each | `TransactionRequiredException`, `EntityNotFoundException`, `CannotGetJdbcConnectionException`, ... | Tail. |

The `failsafe-summary.xml` itself is stale (it was written by an
earlier partial run and reports 3 completed, 0 errors); the per-class
`TEST-*.xml` files have the real numbers.

## What's still left for Phase 2

1. **The AccessDeniedException cluster (110 errors)** — in flight under
   another agent. Concentrated in test fixture bootstrap; root cause
   is the same per-test ACL grant being rejected by the new auth
   wiring, so it should resolve in one or two coherent commits.
2. **The deeper `BaseSpringContextTest` test-fixture cluster** — the
   78-NPE bucket (test-fixture shape: `entity` null) and the 22
   `Gene altered from null to N` bucket point at a common test-fixture
   helper (`PersistentDummyObjectHelper` and friends) that needs a
   pass against Hibernate 6's stricter merge / id-assignment
   semantics. Same shape of bug as several already-fixed cases in
   session 5 (`FactorValueServiceImpl` re-resolve, EE `@After`
   re-resolve, `replaceProcessedDataVectors` re-resolve).
3. **The immutable-collection cluster (19 UOE at PersistentSet.clear)** —
   mechanical sweep, same shape as the `ExternalDatabaseServiceTest`
   fix in `2f9a051ac9`. Wrap `Collections.singleton(x)` /
   `Collections.unmodifiableSet(x)` in `new HashSet<>(...)` at every
   test-fixture call site.
4. **gemma-rest / gemma-web integration sweep** — in flight under
   another agent. The unit tier passes; the failsafe tier hasn't been
   run for those modules yet (no `target/failsafe-reports/` directory
   exists).
5. **gsec `AclObjectIdentity.equals` NPE** — 31 of the 78 NPEs in
   gemma-core's integration sweep come from `this.type == null` inside
   `AclObjectIdentity.equals(Object)`. Fix is in `~/Dev/gsec/gsec`
   (separate repo, branch `renovations`). Once fixed and re-installed,
   those 31 errors clear.
6. **Env-pre-existing macOS failures** stay as-is — already in
   `RENOVATIONS.md`: `FileLockManagerTest`, `ReadWriteFileLockTest`
   (`/proc/locks` is Linux-only), `GeoMexSingleCellDataLoaderConfigurerTest`
   (cellranger binary not on the local box),
   `AnnDataSingleCellDataLoaderTest` (Python `anndata` module not
   installed).

### Completed in session 5 (struck through from earlier handoffs)

- ~~Step 8 (bytecode 11→17)~~ — done (`94b7435766`).
- ~~Step 9 (`dependencyConvergence` enforcer)~~ — done (`c80975d8d8`).
- ~~`applicationContext-security.xml` Spring Security 6 schema fix~~ —
  done (`175407d6b8`); Task C then replaced the BCrypt placeholder
  with the real legacy-aware encoder.
- ~~Task C: legacy SHA + username-salt password-hash migration~~ —
  done (`a2db1d8ed4`).
- ~~Task D: JPA Criteria subquery + `.size` filter support~~ — done
  (`f14373925b`).
- ~~Task E: Sort null-precedence via `JpaOrder`~~ — done (`3d265bee86`).
- ~~`AclClassMetadataTest`~~ — fixed (`8caa98fb4d`, `RETURNS_DEEP_STUBS`).
- ~~`DiseaseOntologyTest`~~ — `@Ignore`'d (`8caa98fb4d`, Phase 3
  search rebuild blocker).
- ~~`DatasetsRestTest` (11 errors)~~ — fixed (`175407d6b8`); `@Category`
  inheritance restored.
- ~~`ArrayDesignDaoTest`~~ — fixed (`f0337b7057`, FQN in error message).
- ~~`CompositeSequenceDaoTest.testGetGenesWithGene2Cs`~~ — fixed
  (`f0337b7057`, bind id not entity to NativeQuery).
- ~~`RawAndProcessedExpressionDataVectorDaoTest`~~ — fixed (`f0337b7057`,
  `AbstractDao` tolerates non-entity supertypes).
- ~~`FactorValueDaoTest` / `FactorValueServiceTest`~~ — fixed
  (`1a49a43435`).
- ~~`ExpressionExperimentDaoTest`~~ — 53/0/0 (chain of commits ending
  at `e304d1c2b3`); was ~14 failing methods.
- ~~`SingleCellExpressionExperimentServiceTest`~~ — 17/0/0 (chain
  ending at `764004c2ea`); was 7 failing.
- ~~Full `TypedResultTransformer` migration to HB6 `TupleTransformer` +
  `ResultListTransformer`~~ — done (`1d01e08308`).
- ~~`BinaryFunctionContributor` (`bytes_substring` / `bytes_length`)~~ —
  done (`2b0028a4e5`); un-Ignore'd `testGetRawDataVectors`.
- ~~`getBioAssayDimensions` HQL rewrite~~ — done (`2b0028a4e5`, dodges
  HB6 SQM AssertionError).
- ~~`Subquery#toString` FQN restoration~~ — done (`5cb418b360`).
- ~~`AbstractPersister.formatEntity` transient-tolerance~~ — done
  (`63bffbf6fc`).
- ~~HQL `<plural>.size` → `size(<plural>)`~~ — done (`e79e9c82b2`).
- ~~`applicationContext-schedule.xml` `<ref local="...">` → `<ref bean="...">`~~ —
  done (`cc21cf6504`).
- ~~Integration-test bootstrap ordering (`createDatabaseInitializer` →
  `sessionFactory` → `dataSourceInitializer`)~~ — done (`cc21cf6504`).
- ~~`init-data.sql` named-column AUDIT_EVENT INSERTs~~ — done
  (`cc21cf6504`).
- ~~`ExternalDatabaseServiceTest` mutable-Set fix~~ — done (`2f9a051ac9`).

**Pinned context from the user**: `gemma-web` is being replaced by
`~/Dev/gemma-curation-ui`. Don't invest in gemma-web test fixes or new
features beyond a smoke check; `gemma-rest` IS load-bearing for the
React port — keep it healthy.

### Quick wins still open

- **`CompletionGeneratorTest`** (gemma-cli): 1 environmental failure
  on macOS — bash 3.x doesn't ship `mapfile`. Use Homebrew bash if you
  need it green locally. Not Phase 2.

### Bugs found and fixed earlier in this climb

- **`AbstractFilteringVoEnabledDao.registerEntity`** double-registered
  the `@Id` attribute (JPA Metamodel's `getAttributes()` includes the
  `@Id`, unlike the legacy Hibernate `ClassMetadata.getPropertyNames()`
  it replaced). One-line fix in the Phase-2 rewrite.
- **`Analysis.hbm.xml`** still had a `<subclass>` for the deleted
  `CoexpressionAnalysis` entity (pointing at deleted
  `CoexpCorrelationDistribution`). Removed; `SampleCoexpressionAnalysis`
  retained.
- **`sql/init-entities.sql`** had `ALTER TABLE` statements for the
  retired `{HUMAN,MOUSE,RAT,OTHER}_{GENE,EXPERIMENT}_COEXPRESSION`
  tables that Hibernate no longer creates. Deleted those lines.
- **`AclLinterServiceTest`** had
  `@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)`
  without `MergeMode.MERGE_WITH_DEFAULTS`, which silently dropped the
  default DependencyInjection listener so `@Autowired` fields stayed
  null. Added `MERGE_WITH_DEFAULTS`.

### Step 7 round-3 fixes (session-4 third push — gemma-cli + gemma-rest)

- **`gemma-rest` ran 0 tests** before this push. Jersey 3's test
  framework pulls JUnit 5 onto the classpath, so Surefire auto-picked
  the `JUnitPlatformProvider`, but no `junit-vintage-engine` was on
  the classpath — so JUnit 4 tests (which is what Gemma's REST tests
  are) were invisible. Surefire silently reported `BUILD SUCCESS,
  Tests run: 0`. Fixed by adding `org.junit.vintage:junit-vintage-engine`
  to `gemma-rest/pom.xml` as a test dep.

### Step 7 round-2 fixes (session-4 second push)

- **`@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)`**
  without `MergeMode.MERGE_WITH_DEFAULTS` was silently dropping the
  default DependencyInjection listener, leaving `@Autowired` fields
  null in **26 test classes** across all 4 modules. Bulk-fixed with a
  Python script.
- **MySQL bitwise-AND for H2**: new `BitwiseUtils.bitand(Dialect, a, b)`
  emits `(a & b)` on MySQL and `BITAND(a, b)` on H2. Used in
  `AclQueryUtils`, `EE2CAclQueryUtils`, `AclLinterServiceImpl`.
- **`BigInteger` → `Number` casts on native query results**: Hibernate
  6 returns `Long` (not `BigInteger`) for `BIGINT` / `COUNT(*)` columns.
  Bulk-replaced in 6 DAO/service files.
- **HQL `bitwise_and(x, y)` → `bitand(x, y)`**: Hibernate 6's built-in
  HQL function is `bitand`; the pre-Phase-2 `bitwise_and` name was
  registered through the now-removed dialect SQLFunction API.

## Step 5a notes (Spring 6 JPA migration — done in session 4)

Pattern: `LocalContainerEntityManagerFactoryBean` →
`HibernateJpaVendorAdapter` → expose `SessionFactory` via
`factory-bean="entityManagerFactory" factory-method="unwrap"` with
constructor-arg `"org.hibernate.SessionFactory"`. Transaction manager
is `org.springframework.orm.jpa.JpaTransactionManager`.

Design choices:

- **`persistence.xml` as the canonical mapping list**, not inline XML
  in `applicationContext-hibernate.xml`. Reason: tests
  (`BaseDatabaseTest`) construct their own EMF and can simply set
  `persistenceUnitName="gemma"` to pick up the same mapping list —
  zero duplication.
- **Cache wiring**: dropped Spring 6's removed
  `org.springframework.cache.ehcache.EhCacheCacheManager` bean from
  the XML. The `EhcacheConfig` `@Bean(name="ehcache")` is now aliased
  to `cacheManager` via `<alias>`, so the existing
  `ConcurrentMapCacheManager` stub services any `@Autowired
  CacheManager` injection. Proper EhCache 3 integration is a later
  phase.
- **L2 cache**: `hibernate.cache.region.factory_class=jcache` +
  `hibernate.javax.cache.provider=org.ehcache.jsr107.EhcacheCachingProvider`,
  matching the JCache + Ehcache 3 jakarta deps already in pom.xml.
- **`SampleCoexpressionMatrix.hbm.xml`** is the lone surviving
  coexpression mapping (per-EE QC artifact, kept per Phase 2 Step 3
  decision).

## Jersey 3 migration notes (Step 6)

- `org.glassfish.jersey.ext:jersey-spring3:${jersey.version}` →
  `jersey-spring6:${jersey.version}`. The artifact is `jersey-spring6`
  for Spring 6 integration (Jersey 3.1 dropped Spring 3 support).
- `javax.ws.rs:javax.ws.rs-api:2.0.1` → `jakarta.ws.rs:jakarta.ws.rs-api:3.1.0`.
- `org.hibernate:hibernate-validator` exclusion → `org.hibernate.validator:hibernate-validator`
  (the artifact moved out of `org.hibernate` in HV 7).
- Added `jakarta.servlet:jakarta.servlet-api:6.0.0` at `provided` scope.
- Six swagger artifacts swapped to their `-jakarta` siblings.
- `javax.annotation.Priority` → `jakarta.annotation.Priority` in the
  four provider classes.
- `GeneWebService`: dropped `/genes/{gene}/coexpression` endpoint along
  with the coex subsystem deletion.

## gemma-web migration notes

- `org.hibernate.stat.SecondLevelCacheStatistics` →
  `org.hibernate.stat.CacheRegionStatistics`. The accessor on
  `Statistics` is now `getCacheRegionStatistics(String)`. Used in
  `HibernateMonitorImpl`.
- Spring 6 dropped Commons FileUpload support, which we used via custom
  `CommonsMultipartFile` / `CommonsMultipartMonitoredResolver` /
  `UploadListener`. **All three deleted.** `gemma-servlet.xml` now wires
  `org.springframework.web.multipart.support.StandardServletMultipartResolver`
  instead.
- Micrometer 1.13 pre-jakarta `DefaultHttpServletRequestTagsProvider`
  takes javax.servlet types. The jakarta-aware replacement lives in
  `io.micrometer.observation`. For now `ServletMetricsFilter` inlines
  the tags itself.
- EE QC controller no longer has the persistent
  `CoexpCorrelationDistribution` read/backfill path.

## spring-security-test consolidation

Pre-Phase 2, `gemma-core/pom.xml` unpacked
`spring-security-test:4.0.4.RELEASE:sources` and recompiled them under
our classpath. Those 4.0 sources reference `javax.servlet` which
doesn't exist in Spring 6. **Deleted the unpack-and-recompile
plumbing** and added a regular test-scoped dependency on
`spring-security-test:${spring.security.version}` (6.x) to each module
that needed it.

## Hibernate 6 API replacements (cheat sheet for grep)

| Old (gone) | New |
|---|---|
| `org.hibernate.SQLQuery` | `org.hibernate.query.NativeQuery<?>` |
| `session.createSQLQuery(s)` | `session.createNativeQuery(s)` |
| `org.hibernate.Query` (deprecated) | `org.hibernate.query.Query<T>` |
| `query.setLong/setString/setDouble/etc` | `query.setParameter` (PreparedStatement still uses setLong) |
| `query.setFlushMode(...)` | `query.setHibernateFlushMode(...)` |
| `query.setResultTransformer(X)` | `query.setTupleTransformer(X)` + `setResultListTransformer(X)` via `TypedResultTransformer` (now extends both HB6 interfaces) |
| `Restrictions.* / Projections.* / Criteria` | JPA Criteria (jakarta.persistence.criteria) or plain HQL |
| `sessionFactory.getClassMetadata(X)` | `sessionFactory.getMetamodel().entity(X)` or `((SessionFactoryImplementor) sf).getMappingMetamodel().getEntityDescriptor(X)` |
| `sessionFactory.getCache().evictCollection(role, id)` | `evictCollectionData(role, id)` |
| `Dialect.registerFunction(name, fn)` | `FunctionContributor` SPI (see `BinaryFunctionContributor`); or inline the SQL |
| HQL `<plural>.size` | HQL `size(<plural>)` |
| HQL `substring()` / `length()` on BLOB | HQL `bytes_substring(b,s,l)` / `bytes_length(b)` (Gemma-registered) |
| `UserType.sqlTypes() : int[]` | `UserType.getSqlType() : int` |
| `UserType.nullSafeGet(rs, String[], session, owner)` | `UserType.nullSafeGet(rs, int position, session, owner)` |
| `UserType` (raw) | `UserType<T>` (generic) |
| `IdentifierGeneratorHelper.getGeneratedIdentity(rs, idProp, idType, dialect)` | `(idProp, rs, PostInsertIdentityPersister, WrapperOptions)` |

Spring 6 single-arg Assert removals:

| Old | New |
|---|---|
| `Assert.notNull(x)` | `Assert.notNull(x, "must not be null")` |
| `Assert.isTrue(b)` | `Assert.isTrue(b, "expected true")` |
| `Assert.state(b)` | `Assert.state(b, "illegal state")` |
| `Assert.isNull(x)` | `Assert.isNull(x, "must be null")` |
| `Assert.hasText(s)` | `Assert.hasText(s, "must not be blank")` |
| `Assert.hasLength(s)` | `Assert.hasLength(s, "must not be empty")` |

Spring 6 BeanPostProcessor:

| Old | New |
|---|---|
| `postProcessPropertyValues(PropertyValues, PropertyDescriptor[], Object, String)` | `postProcessProperties(PropertyValues, Object, String)` |

## Stubbed / temporarily disabled

- `ExpressionAnalysisResultSetDaoImpl#findByBioAssaySetInAndDatabaseEntryInLimit`
  → UOE. The `getFilteringCriteria` method was deleted entirely; the
  filtering-by-VO path needs a JPA-Criteria reimplementation.
- `ExpressionDataFileServiceImpl#writeOrLocateCoexpressionDataFile` → UOE
  (coexpression subsystem gone).
- `ExpressionExperimentDaoImpl#getExpressionExperimentIdsWithCoexpression`
  → `Collections.emptyList()` (the `CoexpressionAnalysis` entity is
  gone; callers only use it to set `hasCoexpressionAnalysis(false)`).
- `AbstractFilteringVoEnabledDao#registerEntity` was rewritten on JPA
  Metamodel — the old code branched on `MaterializedBlobType`/
  `MaterializedClobType`/`CustomType` for skip-fields; the new code
  falls back to `log.trace` for any non-basic, non-association,
  non-collection attribute.
- All `CoexpressionAnalysis` entities, DAOs, services, caches, link
  analysis services, GeneCoexpressionSearchService, LinkAnalysisCli,
  CoexpressionWriter, and the species-specific
  `Gene2GeneCoexpression`/`ExperimentCoexpressionLink` subclasses are
  **deleted**. `SampleCoexpression*` is **kept**.
- Some EE DAO `Initializer` classes are typed `TypedResultTransformer<Object>`
  because they return either an entity or `Object[]` depending on a flag.

## File system layout summary

```
~/Dev/eclipseworkspace/Gemma                  # main monorepo
  - renovations branch    : Phase 1c, working
  - phase2 branch         : Phase 2 in flight (this file)
  - RENOVATIONS.md        : full phase history
  - PHASE_2_HANDOFF.md    : this file

~/Dev/gsec                                    # Pavlab security lib
  - renovations branch    : HEAD at 44ecead (Spring 6, 25/25 tests pass)
                            AclObjectIdentity.equals NPE pending

~/Dev/eclipseworkspace/baseCode               # Pavlab utility lib
  - renovations branch    : 1.1.34-RENOVATIONS-SNAPSHOT (Lucene/R gutted)

~/Dev/GemBrow                                 # parallel React port (different agent)
~/Dev/gemma-curation-agents                   # WIP, do not touch
~/Dev/gemma-curation-ui                       # the React app gemma-ui will grow into
```
