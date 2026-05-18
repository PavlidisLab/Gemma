# Phase 3 — vision

Phase 2 finishes the runtime upgrade (Spring 6 / Hibernate 6 / jakarta).
Phase 3 is about **improvement** under six dimensions the project lead
articulated:

- **Faster** — measurable latency cuts on hot paths
- **More efficient** — reduced resource use per request and per dataset
- **Easier to maintain** — less code surface, more uniform patterns,
  better test ergonomics
- **Cloud-ready** — horizontally scalable, stateless, externalized config
- **Mobile-friendly** — API-first, slim payloads, no SSR
- **AI-driven** — vector search, NL-to-query, agent-friendly API shape

Each Phase 2 fix that papered over a structural issue is logged below
under the dimension it should be properly resolved within.

---

## First-wave priorities

Three foundational moves. Each is independent. Each unlocks several
downstream dimensions. Each is ~1–2 weeks of focused work.

### 1. Flyway / Liquibase schema versioning

**Why first**: kills an entire bug class. The Phase 2 multi-context
schema-drop bug, the `init-data.sql` AUDIT_EVENT column-order bug, the
`hbm2ddl.auto=create` test-DB drift, and `TestBootstrapState`'s
JVM-static workaround are all symptoms of unversioned schema. Today
the production schema is implicit (Hibernate-generated) and the test
schema is regenerated on every CI run.

**Deliverable**: every DDL change is a versioned `V<n>__description.sql`
file, applied in order, idempotently. Test and production share the
same migrations. Hibernate runs in `validate` mode.

**Unlocks**: Easier-to-maintain (whole bug class gone), Cloud-ready
(blue/green deploys become safe), Faster CI (skip the drop+create
dance).

### 2. Streaming-by-default DAOs

**Why first**: the new modalities (single-cell, spatial, proteomics)
don't fit in memory. Today's `Collection<T>` returns force whole-result
loading. Phase 2 already touched this with the `TupleTransformer`
migration and the new `CompressedStringListType.decompressToStream`.
Systematize.

**Deliverable**: every `*Dao` returns `Stream<T>` for any path that can
exceed ~10k rows. Spring's `@Transactional(readOnly = true)` plus
`Query#stream()` plus cursor-based pagination at the REST layer.

**Unlocks**: Multi-modality (single-cell becomes feasible), Faster
(no memory pressure on hot paths), Mobile-friendly (cursor pagination
is the right shape for touch UIs).

### 3. Test-fixture rewrite

**Why first**: ~30% of Phase 2 session debug time was test-fixture
mismatches with HB6, not production bugs.
`PersistentDummyObjectHelper` (1083 lines) builds entities the way HB5
tolerated; HB6 rejected several patterns. Future migrations (and
single-cell, spatial test fixtures) will hit the same wall.

**Deliverable**: typed factory pattern (e.g., `ExperimentFactory`,
`BioMaterialFactory`) returning entities with sensible defaults and
fluent overrides. One-line creation in tests:
`var ee = experimentFactory.bulkRna().withSamples(50).build();`
Built on a fixture loader that respects HB6 cascade and PersistentSet
semantics by construction.

**Unlocks**: Easier-to-maintain (no more session-spent debugging
fixtures), Multi-modality (single-cell factory drops in cleanly), Faster
CI (parallelizable, isolated per test).

---

## Full inventory by dimension

### Faster

- **N+1 elimination** on hot paths via `@EntityGraph` fetch joins.
  Every `ensureInSession` / `ensureEeInSession` we added in Phase 2 is
  a partial workaround for lazy-load cascades.
- **L2 cache via Redis or similar** (Spring Cache abstraction).
  Hibernate's session cache is single-request only; ACL voting and EE
  metadata re-evaluate per call.
- **Materialized views for `ExpressionExperimentValueObject`** —
  today's VO assembly joins ~12 tables. Precompute, invalidate on EE
  update.
- **JPA Criteria over string-HQL.** `ExpressionExperimentDaoImpl` is
  4000+ lines of HQL strings; Criteria precompiles + benefits from
  Hibernate's query plan cache.

### More efficient

- **Streaming-by-default DAOs** (see first-wave #2).
- **Read-replica routing** via `AbstractRoutingDataSource` + read-only
  transaction annotations. One-day add when needed.
- **Batch ACL grants on entity creation.** Inheritance is supposed to
  prevent per-child ACLs but the fixture layer creates them anyway.
  Audit + remove.
- **Slim VO projections** — return minimal payloads for list endpoints,
  full detail only on explicit fetch.

### Easier to maintain

- **Flyway/Liquibase** (see first-wave #1).
- **Test-fixture rewrite** (see first-wave #3).
- **Decompose `ExpressionExperimentServiceImpl`** — ~50 methods, four
  responsibilities (curation, retrieval, mutation, analytics).
  Split into facade classes.
- **XML → `@Configuration`.** Phase 2 started this (`GemmaAclConfiguration`
  in the in-flight rip-and-replace). Finish: every
  `applicationContext-*.xml` becomes a Java config.
- **Deprecate `ensureInSession` / `findOrCreate`.** Replace with
  re-fetch-by-id at transaction boundaries. Eliminates the
  EntityNotFound class of bugs entirely.
- **Externalize ACL.** Decouple authz cadence from data-model cadence.
  OPA / Cedar / Spring Authorization Server. Today's gsec dependency
  is a known fragility point.

### Cloud-ready

- **Remove ThreadLocal state.** `BaseAclAdvice` and
  `GemmaLegacyAwarePasswordEncoder` rely on ThreadLocal username
  bindings. Hostile to async/reactive request flows and to per-request
  thread-pool reuse.
- **12-factor config.** Env vars + Spring profiles. The Keychain
  resolution pattern from global CLAUDE.md is the right primitive.
  Eliminate `~/Gemma.properties`.
- **Object storage for big artifacts.** `gemma.appdata.home` is a local
  filesystem assumption. Spring's `Resource` abstraction over
  S3/GCS-compatible backends.
- **Container image** with sane defaults. Current `docker-compose.yml`
  is dev-only.
- **Structured logging + OpenTelemetry.** Replace Commons Logging.
  Correlation IDs across services, trace-driven debugging.
- **Health checks + graceful shutdown.** Spring Boot Actuator if/when
  the bootstrap is Springified. Otherwise a thin custom equivalent.

### Mobile-friendly

- **Retire gemma-web (Spring MVC SSR).** Already planned per project
  memory; gemma-curation-ui is the React replacement. Accelerate.
- **Selective field projection** at the REST layer — `?fields=...` or
  GraphQL. Today's full-VO returns are MB-scale and hostile to mobile.
- **Cursor-based pagination** everywhere (consistent with the
  streaming DAO refactor).
- **PWA-ready asset hosting + CORS.** Static assets behind CDN, API on
  a separate origin.

### AI-driven

- **Vector store for similarity.** pgvector or external (Pinecone,
  Weaviate). EE-to-EE similarity, gene-set similarity,
  query-by-example. Today this requires re-running batch correlations.
- **Embeddings on metadata fields** — experiment descriptions, sample
  annotations, factor values. Enables natural-language search.
- **LLM-friendly API surface**: idempotency keys, structured tool
  definitions, deterministic error codes, OpenAPI JSON schemas.
  Agents (gemma-curation-agents repo) consume this directly.
- **Promote gemma-curation-agents to a Gemma module.** Today it's
  external; integration tests would benefit from being in-tree.
- **NL-to-query.** Conversational filter construction.
  The JPA Criteria refactor (Faster #4) directly enables this — `Filter`
  objects are already structured, just need an LLM-driven builder.

---

## Phase 2 residuals inherited

Items deferred during Phase 2 that Phase 3 should pick up:

- **Spring 6 `testContextFailureThreshold=1` cascade.** Today one
  context-load failure cascades into N test-class errors. Either set
  `-Dspring.test.context.failure.threshold=0` globally or fix the
  underlying first-failure each time. Tied to the Flyway work — once
  schema is stable, context-load failures should be rare.
- **`session.refresh` edge cases.** Two known callsites need it
  (`DataUpdaterImpl.replaceData` line 672, `ExternalFileGeneLoaderServiceTest`
  line 123). Symptom of the "stop using `ensureInSession` escape
  hatches" item under Easier-to-maintain.
- **gsec deprecation.** Phase 2 replaces gsec's `AclDaoImpl` with
  `JdbcMutableAclService` but keeps gsec's domain types
  (`AclObjectIdentity`, `AclSid`, `AclEntry`) because they're embedded
  in business HQL. Phase 3 should finish the job: replace those types
  with `org.springframework.security.acls.model.*` and rewrite the
  HQL queries accordingly.
- **H2 unit-test ACL config migration** (`BaseDatabaseAclConfig`,
  `BaseDatabaseTest`). Still wires gsec's `AclDaoImpl` directly.
  Migrate to `JdbcMutableAclService` once production wiring proves
  out.
- **Stubbed Coexpression callsites.** Phase 2 returned `emptyList()` or
  `UnsupportedOperationException` from a handful of methods because
  the Coexpression subsystem was deleted in Phase 1c. Either remove
  the callsites entirely or wire them to a replacement subsystem.
- **`@Ignore`'d tests.** Any tests Phase 2 disabled with
  `// PHASE_2_RESIDUAL` should be re-evaluated — either fix or
  formally retire.

---

## Out of scope for Phase 3 (deliberately)

- **Microservices split.** Gemma is a monolith. Splitting is a
  separate strategic decision; cloud-readiness here means making the
  monolith deployable on a cloud runtime, not breaking it apart.
- **Multi-tenant SaaS.** Single-tenant model continues.
- **Frontend rewrite.** gemma-curation-ui is the React replacement and
  is its own track.
- **Database engine swap.** MySQL stays. Postgres-compatible
  abstractions (pgvector) require a separate decision later.

---

## Working agreement

- One first-wave item at a time, complete before starting next.
- Each first-wave item ships with: migration plan, rollback plan,
  before/after metrics, and updates to this document.
- Phase 2 residuals are addressed opportunistically when touching
  adjacent code, not as a dedicated sprint.
- AI/cloud/mobile dimensions are most cheaply tackled AFTER the three
  foundational items land — the dependency analysis is in the
  individual dimension sections above.
