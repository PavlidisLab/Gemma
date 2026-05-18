# Renovations

Working notes for the top-to-bottom modernization of Gemma. Living document — edit as decisions are made.

Branch: `renovations` (based on `hotfix-1.32.7`).

## The four prongs

1. **Infrastructure** — Java / Spring / Hibernate / build chain modernization (sticking with Java + MySQL).
2. **UI** — replace `gemma-web` (Ext JS + DWR + JSP) and `GemBrow` (Vue 2) with a single React app extending the curation-ui patterns.
3. **Data model** — flexibility for multiomics, heterogeneous data types and species.
4. **Agentic integration** — full integration of agentic assists and workflow management.

Order: 1 + 3 (design) in parallel first, then 4 against a real write-API, then 2.

---

## Phase 0 (infra housekeeping) — done; Phase 1 (Spring/Hibernate climb) — substantially done

### Status

| Item | State | Notes |
|---|---|---|
| Java 8 → 17 runtime, bytecode 11 | ✅ Phase 0 | Committed `53eafb23d1` |
| **Spring 3.2 → 4.3, Spring Security 3.2 → 4.2** | ✅ Phase 1a | Committed `5c657de93c`; full reactor + 446 unit tests pass under JDK 17 |
| **Spring 4.3 → 5.3, Spring Security 4.2 → 5.8, Hibernate 4.2 → 5.6, Hibernate Search 4.4 → 5.11, Lucene 3.6 → 5.5** | ✅ Phase 1b | Committed `5c657de93c..cf0de2e0ae`; full reactor compiles; ~95% of unit tests pass (real Spring-5 regressions down to ~10–15) |
| **gsec forked** (`~/Dev/gsec/`, branch `renovations`) | ✅ | 0.0.23-RENOVATIONS-SNAPSHOT, 25/25 tests pass |
| **baseCode forked** (`~/Dev/eclipseworkspace/baseCode/`, branch `renovations`) | ✅ | 1.1.34-RENOVATIONS-SNAPSHOT; Lucene 3 ontology indexer + R support gutted |
| HDF5 native lib loading on macOS | ✅ | Surefire env vars + ~/.m2/settings.xml |
| Flyway adoption | ⏳ deferred | Existing `sql/migrations/` scheme works; needs prod coordination |
| JUnit 4 → 5 | ⏳ todo | Vintage engine for coexistence |
| hbm.xml → JPA annotations | ⏳ todo | Per-entity, distributable; foundation for Hibernate 6 |
| Drop dead deps | ⏳ todo | |
| Re-enable `dependencyConvergence` enforcer | ✅ Phase 2 Step 9 | Committed `c80975d8d8` on `phase2`; 8 transitive pins in root `<dependencyManagement>` |
| Bytecode 11 → 17 | ✅ Phase 2 Step 8 | Committed `94b7435766` on `phase2`; Spring 6 ASM handles class file v61 cleanly |

### Java version

- Runtime: Amazon Corretto 17 (`~/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/`, no sudo install).
- Build: `<maven.compiler.release>11</maven.compiler.release>` in root `pom.xml`.
- Enforcer: `requireJavaVersion` bumped to `[17,)`.
- Full reactor compile passes under JDK 17 in ~36s.
- Unit tests: 1090 run, 1061 pass. Failures bucket:
  - 22 errors: HDF5 native lib not installed (env, pre-existing).
  - 1 error: cellranger binary not on box (env, pre-existing).
  - 1 error: scratch dir missing (env, pre-existing).
  - **5 errors: `SearchServiceTest` — Spring 3.2's bundled CGLIB/ASM cannot read Java 17 bytecode (class file version 61).** This is why bytecode is pinned to 11; will lift in Phase 1.

### Why bytecode 11, not 17

Spring 3.2 ships an ancient ASM that throws `IllegalArgumentException` from `ClassReader.<init>` when it tries to CGLIB-proxy a `@Configuration` class compiled to bytecode 61. Only one test currently hits this (most contexts use XML config), but adopting more `@Configuration` would explode the failure count. JDK 17 *runtime* is fine — only the bytecode target is constrained.

Lift `release=11` → `release=17` once Spring is on 5.x (or 6.x), as part of Phase 1.

---

## Infrastructure baseline (audited)

| Layer | Current | EOL | Phase to address |
|---|---|---|---|
| Spring Framework | 3.2.18 | Dec 2020 | Phase 1 (→5), Phase 2 (→6) |
| Spring Security | 3.2.10 | Dec 2020 | Phase 1 (→5) |
| Hibernate ORM | 4.2.21 + hbm.xml | 2013 | Phase 1 (→5), Phase 2 (→6) |
| Hibernate Search + Lucene | 4.4 / 3.6.2 | 2011 | Phase 3 (Search rewrite — possibly replace) |
| Web frontend | Ext JS + DWR 2.0 + JSP + SiteMesh | ~2010 | Phase 2 cutover; new UI is React |
| JAX-RS (Jersey) | 2.25.1 | stale | Phase 1 (→2.39), Phase 2 (→3) |
| Servlet namespace | `javax.*` (Tomcat 9) | — | Phase 2 jakarta flag day |
| Java source/target | inherited 8; runtime ≥11 | — | **Phase 0: runtime 17, bytecode 11** (now) |
| Schema migrations | none (hand-rolled SQL in `sql/migrations/`) | — | Phase 0 if Flyway adopted |
| Tests | JUnit 4 only, 446 unit + ~100 IT | — | Phase 0 JUnit 5 vintage engine |

**Modern bits**: HikariCP 5.1, Log4j2 2.25, Jackson 2.21, MySQL connector 8.4, Maven plugins 2024–2025.

---

## Phase 1 — done (Spring 3→5, Hibernate 4→5, Search/Lucene 4→5; still javax)

### Sequence taken

1. ✅ Spring 3.2 → Spring 4 → Spring 5. Each step ~10 source touches; surprisingly small.
2. ✅ Hibernate 4.2 → 5.6 (forced by Spring 5 dropping `org.springframework.orm.hibernate4`).
3. ✅ Hibernate Search 4.4 → 5.11 + Lucene 3.6 → 5.5 (forced by Hibernate 5).
4. ⏳ Jersey 2.25 → 2.39 — not bumped yet; Spring 5 didn't force it.
5. ⏳ DWR endpoint conversion — not started; Phase 2 prerequisite.

### Substantial stubs left behind (TODOs)

- `gemma-web/.../compat/SimpleFormController` — compile shim for the removed Spring form-controller hierarchy. One subclass (`ArrayDesignFormController`) still references it; proper `@Controller` rewrite is part of the DWR-to-REST sprint.
- `DatabaseSchemaPopulator` + `DatabaseSchemaUpdatePopulator` — no-op stubs (Hibernate 5 removed `Configuration.generateSchemaCreationScript()`, `DatabaseMetadata`, `SchemaUpdateScript`). Test schema bootstrapped via `hbm2ddl.auto=create`; production uses `sql/migrations/*.sql`. Reinstating proper schema management needs the Hibernate 5 `SchemaCreator`/`SchemaUpdate` APIs or a Flyway adoption.
- `GenerateDatabaseUpdateCli` — prints a "use sql/migrations/ instead" notice; same root cause as above.
- `LocalSessionFactoryBean.afterPropertiesSet()` — `sfb.setEntityResolver(new XSDEntityResolver())` is commented out (Spring 5's `LocalSessionFactoryBuilder` removed that setter). Offline DTD resolution may need a custom `SchemaLocator` later.
- `UserManagerImpl` — switched to Spring Security 5's salt-less `PasswordEncoder`. Production user-hash migration via `DelegatingPasswordEncoder` is a real TODO before this can log in real users.
- `baseCode` — Lucene 3-based ontology search-index removed entirely (`OntologyIndexer`/`SearchIndex` are stubs returning null). `AbstractOntologyService` already handles a null index gracefully (logs + empty results).
- `baseCode` — R support (`ubic.basecode.util.r.*`, rJava) removed entirely. Per Paul's note, baseCode is effectively Gemma's only consumer at this point; absorbing the R bridge into Gemma directly (if still wanted) is future work.
- `CharacteristicDaoTest.testGetParents`, `HibernateConfigTest.testCacheConfigurations` — `@Ignore`'d; they walked `SessionFactory.getAllClassMetadata()` for property names. Need to rewrite against the JPA metamodel + `SessionFactoryImplementor.getMetamodel().entityPersister(Class)`.
- 8 R-dependent DE analyzer tests deleted (`AncovaTest`, `OneWayAnovaAnalyzerTest`, `TTestAnalyzerTest`, etc.) — they all extended a now-defunct `BaseAnalyzerConfigurationTest` that initialized `RClient`.
- `LuceneTest` deleted — exercised Lucene 3 APIs directly.
- `dependencyConvergence` enforcer rule disabled — too many transitive version conflicts during the climb. Re-enable once stable.

### Known remaining Spring 5 / Hibernate 5 runtime issues (small list)

- 4 `LuceneParseSearch` / `TokenMgrError` — Lucene 5's QueryParser is stricter about backslash-escaped tokens (`/GO\\:1234`, `/d OR \"a quoted...`). Either the production query escaping logic needs adjustment or the test inputs do.
- 1 `NullPointer` in `TableMaintenanceUtilTest` — `session.createSQLQuery()` returns null in the mock after the SQLQuery→NativeQuery swap. Mock needs updating.
- 1 `IllegalState Failed to load ApplicationContext` in `AclClassMetadataTest` — last one; needs investigation.
- 1 `HibernateSearchException` in `HibernateSearchSourceTest`.
- 1 `AssertionFailure` "null id in ExpressionExperimentSet" in `ExpressionExperimentSetDaoTest` (`flushAndClearSession` after exception).
- 19 Failures (uncategorized; likely a mix of the above + behavior changes).
- 1 `cellranger` missing — env, ignored.

### Phase 1c (B) — DWR stripped from gemma-web

✅ Done (commit). Rather than convert the 40 controllers / 279 methods to REST, the DWR runtime wiring was deleted entirely:
- `gemma-web/pom.xml` — `org.directwebremoting:dwr` dropped.
- `gemma-servlet.xml` shrank 916 → 121 lines (all `<dwr:remote>` / `<dwr:configuration>` removed).
- `web.xml` `/dwr/*` url-pattern removed.
- `applicationContext-security.xml` 5 `/dwr/**` rules removed.
- `gemma-web/.../controller/dwr/` package (7 Converter classes) deleted.
- `gemma-web/.../test/util/dwr/` test infrastructure deleted.
- 3 DWR-dependent controller tests deleted (`DifferentialExpressionAnalysisControllerTest`, `ExpressionExperimentControllerTest`, `ExpressionDataFileUploadControllerTest`).
- `FileUploadController.getUploadStatus()` — DWR-only method removed (used `WebContextFactory.get().getHttpServletRequest()`).

Full reactor still builds. The 40 DWR-remoted controller classes themselves are now dead code; they'll be deleted (or migrated to REST in `gemma-rest`) as gemma-web is decommissioned in favour of the new React frontend.

### Phase 2 (E) — Spring 6 / Hibernate 6 / jakarta — substantially done on `phase2` branch

Live on the `phase2` branch (5 sessions, dozens of commits). Full
play-by-play is in `PHASE_2_HANDOFF.md`. **Build state**: all 5 modules
install + test-compile green at **bytecode 17**; the SessionFactory
bootstrap works (native Hibernate via JPA EMF unwrap); the shared
JPA-Criteria filtering machinery is rebuilt; and the unit-test layer
is broadly green across gemma-core / gemma-cli / gemma-rest. Big
ticket items closed:

- Spring 5 → 6.1.20; Spring Security 5 → 6.3.10.
- Hibernate 5 → 6.4.10 (`org.hibernate.orm` groupId).
- Hibernate Search **gutted** — Lucene/HS deps removed entirely;
  `SearchServiceImpl` stubs to empty results; the real HS-7 rewrite
  is Phase 3.
- EhCache 2 cache subsystem **gutted** — replaced with
  `ConcurrentMapCacheManager` (no L2 cache) + `hibernate-jcache` +
  Ehcache 3.10.8-jakarta for the L2 wiring that survives. The
  jaxb-runtime snapshot pin is solved by an explicit 4.0.5 override.
- All Hibernate Criteria (`org.hibernate.criterion.*`) ports done —
  22 DAOs + BusinessKey rewritten on HQL or JPA Criteria;
  `AbstractCriteriaFilteringVoEnabledDao` ported to JPA Criteria
  (new `FilterJpaUtils` mirrors the deleted `FilterCriteriaUtils`).
- ~400 `javax.* → jakarta.*` imports swapped.
- Jersey 2 → 3.1.10 (incl. jersey-spring6, swagger-jakarta artifacts).
- Tomcat 9 → 10.1.34, jakarta.servlet 6.0.0, jakarta.xml.bind-api 4.0.2.
- DWR stripped (already done in Phase 1c); `CommonsMultipartFile`
  / progress-monitored upload deleted; replaced with
  `StandardServletMultipartResolver`.
- gemma-web `HibernateMonitorImpl` ported off
  `SecondLevelCacheStatistics` to `CacheRegionStatistics`.
- spring-security-test 4.0 unpack-and-recompile plumbing **deleted**;
  replaced with a normal test-scoped 6.x dependency.
- Coexpression subsystem **deleted** wholesale (all `CoexpressionAnalysis`
  entities/DAOs/services/caches/link-analysis/CLI/writer); only
  `SampleCoexpression*` (per-EE sample QC) retained.
- Bytecode 11 → 17 (Step 8, committed `94b7435766`).
- `applicationContext-security.xml` ported to Spring Security 6's
  stricter schema; `ShaPasswordEncoder` (removed in SS 5) swapped for
  `BCryptPasswordEncoder`. Production password verification still
  needs a `DelegatingPasswordEncoder` migration before the legacy
  SHA-with-username-salt hashes will verify — flagged in the XML and
  `UserManagerImpl` comments. Committed `175407d6b8`.
- `dependencyConvergence` enforcer re-enabled (Step 9, committed
  `c80975d8d8`). 8 transitive-version pins added to root
  `<dependencyManagement>`. See `PHASE_2_HANDOFF.md` for the table.

**What's left for Phase 2** (full detail in `PHASE_2_HANDOFF.md`):

- Step 7 integration tier — ~61 `@Category(IntegrationTest)` tests
  need a real MySQL test DB + the failsafe plugin path. Untouched.
- Broader unit-sweep cleanup — ~12 failing test classes remaining in
  gemma-core (FactorValue Hibernate-session-state issues, the
  SingleCellExpressionExperimentService sweep, plus a few env-pre-
  existing ones already catalogued: file-lock OS asserts, cellranger).
- `AbstractCriteriaFilteringVoEnabledDao` subquery and `.size`-suffix
  filters still throw UOE (subclasses can override or use HQL).
- Null-precedence on `Sort` — JPA Criteria ignores
  `Sort.NullMode.FIRST/LAST`; Hibernate 6 has a vendor extension
  not yet plumbed.
- Production password verification — `BCryptPasswordEncoder` is a
  placeholder; legacy hashes need a `DelegatingPasswordEncoder` shim.

### Phase 1b cleanup deferred to future session

- ~10 Lucene 5 parse errors on URI queries (Lucene 5 stricter `/` and `\:` handling).
- 1 NullPointer in `TableMaintenanceUtilTest` (mock returned null after `SQLQuery` → `NativeQuery` swap).
- 1 `IllegalState` in `AclClassMetadataTest`.
- 1 `HibernateSearchException` in `HibernateSearchSourceTest.test`.
- 1 `AssertionFailure` "null id in `ExpressionExperimentSet`" in `ExpressionExperimentSetDaoTest`.
- 19 uncategorized failures.
- ~~Re-enable `dependencyConvergence` enforcer rule.~~ Done in Phase 2 Step 9.
- ~~Bump bytecode 11 → 17.~~ Done in Phase 2 Step 8.

---

## Phase 2 — the jakarta flag day

One coordinated cutover:
- Spring 5 → 6
- Hibernate 5 → 6
- Jersey 2 → 3
- Tomcat 9 → 10.1
- Servlet 4 → 5 (`javax.servlet` → `jakarta.servlet`)
- JAXB / JavaMail → jakarta equivalents
- **DWR dies** — no jakarta successor. All DWR endpoints must already be REST.

---

## Phase 3 — Search

Decision later. Options:
1. Hibernate Search 7 + Lucene 9 (continuity).
2. MySQL `FULLTEXT` (kills a dep; loses faceting).
3. External OpenSearch / Elasticsearch (ops burden; proper search).

---

## DWR inventory

Single file: `gemma-web/src/main/webapp/WEB-INF/gemma-servlet.xml`.

- **40 remoted Spring beans**
- **279 exposed methods** total
- **~114 unique JS call sites** across `scripts/api/` and `scripts/app/` (suggests many exposed methods are dead — pre-pass to remove)
- 45 generated DWR interface JS files

Top controllers by method count:

| Controller | Methods |
|---|---|
| `expressionExperimentController` | 37 |
| `geneSetController` | 23 |
| `securityController` | 20 |
| `expressionExperimentSetController` | 20 |
| `experimentalDesignController` | 18 |
| `genePickerController` | 15 |
| `arrayDesignController` | 11 |
| `dEDVController` | 9 |

Easy wins (1–2 method controllers, 14 of them): convert first as muscle-memory builder.

Hardest: `expressionExperimentController`, `geneSetController` — entangled with Ext JS grids.

Cleanup target: remove dead `<dwr:include>` declarations before migration. Inventory of "exposed but not called from JS" reduces the 279 number substantially.

---

## Schema migrations — already exist

`gemma-core/src/main/resources/sql/migrations/` has **50 versioned files** named `db.X.Y.Z.sql` matching Gemma releases. Some `_rollback` and `_postponed` variants. Latest: `db.1.32.5.sql`.

**No runner in code** — applied manually by DBA.

Fresh-install bootstrap: `sql/init-entities.sql`, `sql/h2/init-entities.sql`, `sql/mysql/init-entities.sql`, plus `init-acls.sql` + `init-data*.sql`.

### Flyway adoption — three options

| Option | Effort | Pros | Cons |
|---|---|---|---|
| A. Rename to `V<n>__desc.sql`, baseline = concatenated current state | Low | Clean | Loses release-anchored filenames |
| B. Use Flyway versioned naming `V1.32.5__name.sql` with `baseline-version=1.32.5` | Medium | Preserves naming, real Flyway | Need to rename existing files |
| C. Hand-rolled `MigrationRunner` Spring bean + `gemma_migration_history` table | Lowest | Zero new deps | Reinvents Flyway |

Decision pending. Needs prod DBA confirmation of current applied version.

Action item this phase: write `sql/migrations/README.md` documenting current convention + future plan.

---

## Frontend strategy

### Inventory

| App | Stack | Scope | Plan |
|---|---|---|---|
| `gemma-web` (this repo) | Ext JS + DWR + JSP | Everything (legacy) | Decommission incrementally |
| `GemBrow` (`~/Dev/GemBrow`) | Vue 2.7 + Vuetify 2.7 | Browser + search (`Dataset.vue` is a stub) | Port to React (one-time) |
| `gemma-curation-ui` | React + TS + Tailwind | Curation review | Becomes the foundation |

### Decisions made

- **One app** going forward, not multiple SPAs.
- Built on the curation-ui (React + TS) foundation.
- Will eventually move into this monorepo (not yet).
- **Pixel fidelity to GemBrow is not a goal** — free to redesign.
- Visual identity / look-and-feel: TBD. Default Tailwind-y look is *not* the target. Decide before Stage 1 ships.

### Staged rollout (deferred — not started)

1. **Home page** — landing, search entry, login. Owns the shared shell.
2. **Browser** — React port of `Browser.vue` + `AnnotationSelector.vue` (~3.2K Vue lines total).
3. **Experiment pages** — extend curation-ui with visualization (heatmap, PCA, sample correlation, design tile) and download (raw, processed, DE, design).

---

## Sister repos (for context)

| Path | Role |
|---|---|
| `~/Dev/gemma-curation-agents` | Python agentic helpers (scrape-screen, curation-proposer, audit). Posts to a **mock** write-API queue. |
| `~/Dev/gemma-curation-agents-eval` | Eval harness, calibration packages, decks, notable_cases. |
| `~/Dev/gemma-curation-ui` | React/TS curator review UI. The seed for the future single React frontend. |
| `~/Dev/gemma-mcp` | MCP server wrapping `gemmapy` (16 tools). |
| `~/Dev/GemBrow` | Vue 2 browse/search UI (v0.4.8). To be ported to React. |

Prong #4 (agentic) needs: replace the mock write-API with a real `gemma-rest` endpoint. Can be built on existing Jersey 2 (javax) now, ported in Phase 2.

---

## Environment

- JDK 17 at `~/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home`.
- Python 3.10 venv at `.venv/` (gitignored) — for any glue scripts.
- Build: `mvn -P fast install` for a no-tests no-webpack no-delombok pass.
- Full unit tests run in ~34 min on Paul's laptop. Integration tests need MySQL (docker-compose.yml provides), skip on laptop.
