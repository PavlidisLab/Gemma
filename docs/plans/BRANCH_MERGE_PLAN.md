# Phase 3 Branch Merge Plan

**Baseline:** `phase2-acl-migrate` HEAD `08e760bdaf` (Phase 3 fan-out point).
**Worktree:** `worktree-branch-merge-plan` (this doc; not merging anything).
**Scope:** 124 `worktree-*` branches accumulated during the Phase 3 fan-out
session. 105 root directly on the baseline; 19 are pre-Phase-3 leftovers
(mostly already-merged Phase 2 work or sub-merges captured here for
traceability).

This is recce-only. No branches are merged. The goal is to give Paul a
defensible landing order plus per-file conflict expectations so the
integration session that follows knows where the pain is.

---

## 1. Inventory

### 1a. Phase 3 branches (105, off baseline `08e760bdaf`)

Columns: `Cat` = category (see Section 2). `Chain` = identified
stacked-commit chain (see Section 2.C). `Ahead` = commits ahead of baseline.

| Branch | HEAD | Ahead | Cat | Chain | Subject |
|---|---|---|---|---|---|
| `worktree-aclentryvoter-recce` | `56f71171ab` | 1 | A | - | Phase 3 ACL voter: migration roadmap (long-pole blocker for @EnableMethodSecurity) |
| `worktree-aclvoter-x1-wrappers` | `033c2953da` | 2 | B | - | Phase 3 ACL voter: unit test for wrapper delegation |
| `worktree-actuator-recce` | `f4024d98bd` | 1 | A | - | Phase 3 actuator: observability endpoints recce |
| `worktree-afterinv-phase-a` | `3067faad86` | 4 | C | AFTERINV | Phase 3 AfterInvocation: unwire Phase-A-redundant providers from MethodSecurityConfig |
| `worktree-afterinv-phase-b-cs-dv` | `ba9e8b4618` | 7 | C | AFTERINV | Phase 3 AfterInvocation: replace gsec afterAcl{MyData,MyPrivateData,Stream}Read with Gemma-owned providers |
| `worktree-afterinv-phase-b-quiet` | `bf287e9a53` | 5 | C | AFTERINV | Phase 3 AfterInvocation: replace gsec afterAclReadQuiet with Gemma-owned provider |
| `worktree-afterinv-phase-b-vo` | `2e1528172d` | 6 | C | AFTERINV | Phase 3 AfterInvocation: replace gsec afterAclValueObject{,Collection,Map}Read with Gemma-owned providers |
| `worktree-afterinv-phase-c-prep` | `c587f38640` | 1 | A | - | Phase 3 AfterInvocation: Phase C (@EnableMethodSecurity) migration plan |
| `worktree-afterinvocation-recce` | `94c2b35f67` | 1 | A | - | Phase 3 AfterInvocation: modernization roadmap |
| `worktree-agent-a4bed887e0022e00c` | `8b8d4b0ad2` | 4 | B | - | Phase 3 auth: tests for ThreadLocal-free legacy verify + upgrade hook |
| `worktree-agent-a4de64513d998f6a3` | `b32a4043b4` | 2 | B | - | Phase 3 cleanup: remove Phase 2 Coexpression stubs |
| `worktree-agent-a719cad6c20a655be` | `66658b3a78` | 8 | B | - | Phase 3 fixtures: revert ArrayDesignReportServiceTest migration |
| `worktree-agent-a7f525a3ad7b3f575` | `08e760bdaf` | 0 | B | - | ACL upper->lower data migration: done on prod + guard against partial reruns |
| `worktree-annotations-writeback` | `32359648ce` | 4 | B | - | Phase 3 annotations: integration test for PUT /datasets/{id}/annotations |
| `worktree-aspectj-deeper` | `b16450a5e8` | 1 | A | - | Phase 3 AspectJ: deeper proxy-bug recce + remediation plan |
| `worktree-aspectj-ehcache-audit` | `3ec4abf455` | 1 | B | - | Phase 3 infra: AspectJ + ehcache + JCache audit + roadmap |
| `worktree-basejersey-cleanup` | `45f8a2915b` | 1 | B | - | Phase 3 test: retire @RunWith(SpringJUnit4ClassRunner) from BaseJerseyTest |
| `worktree-bk-consolidation` | `ffe6b0293e` | 1 | C | BK-PERSISTER | Phase 3 BusinessKey: lift Chromosome/QuantitationType/BioAssayDimension BK lookups from persisters |
| `worktree-branch-merge-plan` | `08e760bdaf` | 0 | B | - | ACL upper->lower data migration: done on prod + guard against partial reruns |
| `worktree-cacheable-audit` | `76a6afabb8` | 1 | A | - | Phase 3 cache: @Cacheable annotation audit |
| `worktree-commonslog-to-slf4j` | `8f44c7da3b` | 2 | B | - | Phase 3 logging: fix SLF4J-incompatible log call sites (3 files, 12 sites) |
| `worktree-curation-ui-contract` | `a010b84b1f` | 1 | A | - | Phase 3 gemma-curation-ui: REST API contract audit |
| `worktree-ee-proxy-fix` | `0dd5cf599b` | 7 | C | BK-PERSISTER | Phase 3 EeWriteService: fix proxy autowire (use PersisterHelper interface) |
| `worktree-ee-svc-decomp-p1` | `8d2f82afc0` | 2 | C | EE-SVC-DECOMP | Phase 3 EE service: break two cycles via direct ReadService injection |
| `worktree-ee-svc-decomp-p15` | `64331fff1c` | 4 | C | EE-SVC-DECOMP | Phase 3 EE service: @Lazy on read-service field deps to break Spring construction cycle |
| `worktree-ee-svc-decomp-recce` | `504b51b126` | 1 | A | - | Phase 3 EE service: decomposition recce + roadmap |
| `worktree-ehcache-cachemanager-fix` | `08cc312b86` | 2 | C | CACHE | Phase 3 cache: teach CacheUtils to introspect JCache native caches |
| `worktree-executor-virtual-prep` | `4c67cc25a6` | 1 | B | - | Phase 3 concurrency: executor centralization audit + virtual-thread prep |
| `worktree-executor-vt-callers` | `00ac03e8ef` | 3 | C | EXECUTOR-VT | Phase 3 concurrency: migrate UnifiedOntologyUpdaterCli + GeoSingleCellDetector to VT-aware factory |
| `worktree-executor-vt-callers-2` | `7ff9758b13` | 5 | C | EXECUTOR-VT | Phase 3 concurrency: document deferral of scheduled-executor callsites (Groups 3 + 5) |
| `worktree-expression-chunk-e1` | `0e3e26f694` | 3 | C | BK-PERSISTER | Phase 3 ExpressionPersister: collapse cascade-override + per-FV create |
| `worktree-expression-chunk-e2` | `41f5c981d9` | 4 | C | BK-PERSISTER | Phase 3 ExpressionPersister: extract EeWriteService skeleton + delegate simple find-or-create |
| `worktree-expression-chunk-e3` | `7060457261` | 5 | C | BK-PERSISTER | Phase 3 EeWriteService: collapse ExpressionPersister to thin delegate |
| `worktree-expression-chunk-e4` | `b39decc767` | 6 | C | BK-PERSISTER | Phase 3 EeWriteService: cut over GeoServiceImpl + SimpleExpressionDataLoaderServiceImpl from ExpressionPersister |
| `worktree-expression-chunk-e5` | `dbb1f603f4` | 8 | C | BK-PERSISTER | Phase 3 ExpressionPersister: mark deprecated, document final deletion path |
| `worktree-expressionpersister-recce` | `142469a408` | 3 | B | - | Phase 3 persister: ExpressionPersister migration plan (risk-5 recce) |
| `worktree-fixture-bioassay` | `c4ddefe5a9` | 6 | B | - | Phase 3 fixtures: document BioAssayFactory in TEST_FIXTURE_FACTORIES.md |
| `worktree-fixture-factories-2` | `b7222d8f64` | 3 | B | - | Phase 3 fixtures: document TaxonFactory + ArrayDesignFactory |
| `worktree-framework-bump-recce` | `5812f71be5` | 4 | B/E | - | Phase 3 framework: version bump feasibility report |
| `worktree-gemma-rest-bootstrap` | `13501bf4f8` | 4 | B | - | Phase 3 gemma-rest: GEMMA_REST_BOOTSTRAP_PHASE1.md (what landed, what is deferred) |
| `worktree-gemma-rest-standalone-recce` | `e634e0009e` | 1 | A | - | Phase 3 gemma-rest: standalone packaging recce + roadmap |
| `worktree-gemma-web-retire` | `58c237c838` | 1 | A | - | Phase 3 gemma-web: retirement plan |
| `worktree-genome-chunk-51` | `7a476582e9` | 3 | C | BK-PERSISTER | Phase 3 persister: rewire GenomePersister.persist{BioSequence,Chromosome} to BusinessKey.find |
| `worktree-genome-chunk-52` | `200bc90166` | 4 | C | BK-PERSISTER | Phase 3 persister: rewire GenomePersister.persistTaxon to BusinessKey.find |
| `worktree-genome-chunk-53-prep` | `74e607c32d` | 6 | C | BK-PERSISTER | Phase 3 GeneWriteService: quirk-pinning tests (drosophila / GI rotation / NCBI merge / history) |
| `worktree-genome-chunk-53-taxonfix` | `74e607c32d` | 6 | C | BK-PERSISTER | Phase 3 GeneWriteService: quirk-pinning tests (drosophila / GI rotation / NCBI merge / history) |
| `worktree-genome-chunk-54-cutover` | `74e607c32d` | 6 | C | BK-PERSISTER | Phase 3 GeneWriteService: quirk-pinning tests (drosophila / GI rotation / NCBI merge / history) |
| `worktree-gsec-hql-continued` | `88a379127a` | 2 | B | - | Phase 3 gsec HQL: convert AclLinterServiceImpl line 132 to JdbcTemplate |
| `worktree-gsec-hql-v2` | `351a8bc574` | 2 | B | - | Phase 3 gsec HQL: convert two simple AclLinterServiceImpl queries to JdbcTemplate |
| `worktree-gsec-version-align` | `d941f414e3` | 1 | A | - | Phase 3 gsec: version alignment recce + bump plan |
| `worktree-hibernate-envers-audit` | `5b4db694b4` | 1 | A | - | Phase 3 Hibernate envers audit |
| `worktree-hibernate-l2-tune` | `93db601c9b` | 1 | A | - | Phase 3 Hibernate: L2 cache region audit + tuning recommendations |
| `worktree-hibernate-type-audit` | `3b483d5fad` | 1 | A | - | Phase 3 Hibernate: @Type audit |
| `worktree-hikari-modernize` | `f592a3fcda` | 1 | B | - | Phase 3 build: HikariCP audit + modernization |
| `worktree-ignore-audit-v2` | `40a3526017` | 3 | B | - | Phase 3 test triage: audit doc for monorepo @Ignore inventory |
| `worktree-impl-autowire-rule` | `fd66a209ff` | 2 | B/E | - | Phase 3 architecture: ArchUnit rule forbidding @Autowired Impl-typed fields |
| `worktree-java21-phase1` | `97c726ebb1` | 1 | A | - | Phase 3 Java 21: pre-bump Lombok/AspectJ/JaCoCo to JDK-21 floors (still on JDK 17) |
| `worktree-java21-readiness` | `86858b7569` | 1 | A | - | Phase 3 Java 21 readiness recce + phased plan |
| `worktree-jsr305-cleanup` | `62184aae53` | 1 | B | - | Phase 3 Spring 6: javax.annotation -> org.springframework.lang |
| `worktree-jstl-jakarta` | `337548e0a5` | 1 | B | - | Phase 3 gemma-web: swap taglibs-standard 1.2.5 -> jakarta.servlet.jsp.jstl 3.0 |
| `worktree-junit5-phase-a` | `81f498537a` | 3 | C/E | JUNIT5 | Phase 3 JUnit 5: Phase A result doc |
| `worktree-junit5-phase-b0` | `70d58842a9` | 6 | C/E | JUNIT5 | Phase 3 JUnit 5 B0: pilot JUnit 5 IT (@Tag + @ExtendWith(SpringExtension.class)) |
| `worktree-junit5-recce` | `4dcb5e77a1` | 1 | A | - | Phase 3 JUnit 5: migration recce + roadmap |
| `worktree-l2-cache-bound` | `71bcc9ee51` | 4 | C | CACHE | Phase 3 cache: delete dead Ehcache 2.x ehcache.xml |
| `worktree-listenablefuture` | `3538453700` | 1 | A | - | Phase 3 Spring 6+ audit: ListenableFuture surface is clean |
| `worktree-logging-modernize` | `3749e37ea0` | 1 | A | - | Phase 3 logging: audit + version bumps |
| `worktree-lombok-audit` | `07cb9bdbd1` | 1 | A | - | Phase 3 Lombok: usage audit + recommendation |
| `worktree-lombok-cleanup` | `1dca9d7ff9` | 3 | B | - | Phase 3 Lombok: convert 3 lombok.Value classes to Java records |
| `worktree-maven-modernize` | `bbf5210bcf` | 3 | B/E | - | Phase 3 build: document Maven modernization audit + applied bumps |
| `worktree-maven-release-recce` | `2133878b89` | 1 | A | - | Phase 3 build: maven release / version-management audit |
| `worktree-metrics-jcache-restore` | `c7eed8477c` | 3 | C | CACHE | Phase 3 metrics: restore JCache metrics binder for ehcache caches |
| `worktree-mockito-modernize` | `ff32b5b6b1` | 1 | A | - | Phase 3 Mockito: modernize legacy idioms (recce, no renames) |
| `worktree-openapi-audit` | `b68a52edc1` | 1 | B/E | - | Phase 3 OpenAPI: audit + swagger 2.2.42 -> 2.2.50 bump |
| `worktree-persister-delete-plan` | `da7b8e9d15` | 1 | A | - | Phase 3 persister: deletion + dispatch-facade plan |
| `worktree-persister-genome` | `baf8a6c919` | 3 | B | - | Phase 3 persister: GenomePersister migration plan (risk-5 recce) |
| `worktree-persister-recce` | `753c258481` | 1 | A | - | Phase 3 persisterHelper: recce + replacement roadmap |
| `worktree-persister-step2` | `a1ca482301` | 2 | C | BK-PERSISTER | Phase 3 persister: rewire CommonPersister to BusinessKey.find + session.persist |
| `worktree-persister-step3-ad` | `72a2c1016d` | 3 | C | BK-PERSISTER | Phase 3 persister: rewire ArrayDesignPersister to BusinessKey.find + cascade |
| `worktree-profile-cleanup` | `9b8067f2a9` | 3 | B | - | Phase 3 profiles: replace string literals with EnvironmentProfiles constants in @Profile annotations |
| `worktree-querycache-shard` | `71bcc9ee51` | 4 | C | CACHE | Phase 3 cache: delete dead Ehcache 2.x ehcache.xml |
| `worktree-relationshippersister` | `bff8828d06` | 3 | C | BK-PERSISTER | Phase 3 persister: rewire RelationshipPersister to BusinessKey.find |
| `worktree-release-small-fixes` | `594afce1f6` | 3 | B/E | - | Phase 3 docs: add RELEASING.md |
| `worktree-rest-security-config` | `1fb94ff038` | 1 | B | - | Phase 3 gemma-rest: add RestSecurityConfig (not yet wired) |
| `worktree-restclient-migrate` | `56c077be8d` | 1 | B | - | Phase 3 Spring 6: migrate GoogleAnalytics4Provider RestTemplate -> RestClient |
| `worktree-resttemplate-audit` | `287c15bc4f` | 1 | A | - | Phase 3 recce: RESTTEMPLATE_AUDIT.md inventory |
| `worktree-secured-prauthorize` | `7c9af60b4d` | 1 | B | - | Phase 3 security: consolidate @Secured -> @PreAuthorize in gemma-rest |
| `worktree-servlet6-audit` | `ae324ddd13` | 1 | B | - | Phase 3 Servlet 6: audit + jakarta.servlet survivors fix |
| `worktree-session-getreference` | `e626258044` | 1 | B | - | Phase 3 HB6: Session.load -> Session.getReference (7 sites) |
| `worktree-session-refresh-v2` | `17fdb4a5b2` | 1 | A | - | Phase 3 session.refresh: mark residual resolved (no callsites exist) |
| `worktree-slf4j-bump` | `3cd1bff92b` | 2 | B/E | - | Phase 3 logging: bump slf4j-api 1.7.36 -> 2.0.16 and swap log4j-slf4j-impl -> log4j-slf4j2-impl |
| `worktree-spring-boot-3-recce` | `45630f60ef` | 1 | A | - | Phase 3 Spring Boot 3: feasibility recce |
| `worktree-spring-boot-bom` | `bcc42b2905` | 3 | B/E | - | Phase 3 build: spring-boot BOM adoption doc |
| `worktree-spring-profiles-audit` | `6a0f8836c6` | 1 | A | - | Phase 3 Spring profiles: audit + recommendations |
| `worktree-spring-security-7-recce` | `173548c3f6` | 1 | A | - | Phase 3 Spring Security 7: readiness recce |
| `worktree-spring6-deprecation-hunt` | `06de3500fc` | 2 | B | - | Phase 3 Spring 6 modernization: trivial rename for HB6-removed dialect |
| `worktree-static-analysis-audit` | `77db7a2d92` | 1 | A | - | Phase 3 build: static analysis audit + version bumps |
| `worktree-test-failure-triage` | `08e760bdaf` | 0 | B | - | ACL upper->lower data migration: done on prod + guard against partial reruns |
| `worktree-validation-audit` | `6a04942973` | 1 | A | - | Phase 3 validation: audit + recommendations |
| `worktree-xml-config-kickoff` | `dcb758a615` | 2 | B | - | Phase 3 XML->Java: migrate applicationContext-serviceBeans.xml to @Configuration |
| `worktree-xml-datasource` | `6eb398fe2c` | 1 | B | - | Phase 3 XML->Java: migrate applicationContext-dataSource.xml to @Configuration |
| `worktree-xml-gemma-cli` | `122338b6e9` | 1 | B | - | Phase 3 XML->Java: migrate gemma-cli applicationContext-component-scan.xml to @Configuration |
| `worktree-xml-gemma-rest` | `6647648d76` | 1 | B | - | Phase 3 XML->Java: migrate gemma-rest applicationContext-*.xml to @Configuration |
| `worktree-xml-hibernate` | `88b320b9af` | 2 | B | - | Phase 3 XML->Java: add HibernateConfig.java (companion to prior commit) |
| `worktree-xml-schedule` | `e4185dbd10` | 1 | B | - | Phase 3 XML->Java: migrate applicationContext-schedule.xml to @Configuration |
| `worktree-xml-security` | `bc3518ffb8` | 1 | B | - | Phase 3 XML->Java: migrate applicationContext-security.xml to @Configuration |

### 1b. Pre-Phase-3 / orphan branches (19, NOT off baseline)

These are leftover worktrees from earlier sessions. Most have `ahead=0` (no
unique commits over their merge-base) and are effectively dead. The three
`a6a984701`/`a8ec45b54`/`ab43c1385` clones with `ahead=95/95/96` look like
re-aligned phase2 worktrees - ignore unless someone claims work was lost
there.

| Branch | HEAD | Merge-base | Ahead | Subject |
|---|---|---|---|---|
| `worktree-agent-a060e73e4353b5592` | `ee1c752e56` | `ee1c752e56` | 0 | REST checkIsAdmin: document why these can't fold into @PreAuthorize |
| `worktree-agent-a1b429df344ee6b9e` | `6e5576dd37` | `6e5576dd37` | 0 | Revert "Phase 2 Step 7: CreateDatabasePopulator drops only on first context bootstrap" |
| `worktree-agent-a2a8f49a3408139e1` | `a7743d3724` | `a7743d3724` | 0 | Merge branch 'worktree-agent-a42565c51ae3794de' into phase2-acl-migrate |
| `worktree-agent-a3cbf1b955cea3bce` | `2f9a051ac9` | `2f9a051ac9` | 0 | Phase 2: ExternalDatabaseServiceTest passes mutable Set into setExternalDatabases |
| `worktree-agent-a42565c51ae3794de` | `5904e58f62` | `5904e58f62` | 0 | Phase 3 lighthouse #1: batched getSubSetsWithBioAssays kills the REST N+1 |
| `worktree-agent-a4cb317565e5b7768` | `cc635b573c` | `1175a1fbe0` | 1 | Phase 2 Step 7: normalize Date subtypes from JPA Metamodel - fixes REST OpenAPI |
| `worktree-agent-a54b95715b2699369` | `af5687de9b` | `6e5576dd37` | 3 | PHASE_2_HANDOFF.md: document the multi-context schema-drop fix |
| `worktree-agent-a65ce15ffc025989c` | `64b9411a0f` | `64b9411a0f` | 0 | Phase 3 first-wave #1: Flyway schema versioning for the H2 test path |
| `worktree-agent-a6a984701e76aa60a` | `936bc4ce27` | `a8dcbeaef0` | 95 | Remove usage of var in OntologySearchSource.java |
| `worktree-agent-a7e3331f00d073b2f` | `b0751f8329` | `63bffbf6fc` | 2 | Phase 2: JPA-Criteria port supports subquery + .size filters |
| `worktree-agent-a8ec45b544d99440a` | `936bc4ce27` | `a8dcbeaef0` | 95 | Remove usage of var in OntologySearchSource.java |
| `worktree-agent-aaf428afcceabcb01` | `a5a2ce3c0d` | `63bffbf6fc` | 1 | Phase 2 Task C: legacy SHA + username-salt password-hash migration |
| `worktree-agent-ab43c1385f4eb9331` | `b5aa51cbe9` | `a8dcbeaef0` | 96 | Align worktree to phase2 state |
| `worktree-agent-ac75874f216cb7a41` | `9322b2943d` | `9322b2943d` | 0 | Phase 3 fixtures: TEST_FIXTURE_FACTORIES.md (template + migration playbook) |
| `worktree-agent-ada13ef134bc59047` | `1956485493` | `1175a1fbe0` | 1 | Phase 2 Step 7: port ExpressionAnalysisResultSetDaoImpl.findByBioAssaySetInAndDatabaseEntryInLimit to JPA Criteria |
| `worktree-agent-ae169e81f53b31ebb` | `6e5576dd37` | `6e5576dd37` | 0 | Revert "Phase 2 Step 7: CreateDatabasePopulator drops only on first context bootstrap" |
| `worktree-agent-ae6dcdc2fbd87656e` | `a7743d3724` | `a7743d3724` | 0 | Merge branch 'worktree-agent-a42565c51ae3794de' into phase2-acl-migrate |
| `worktree-agent-ae7fc3f98a9289bf0` | `2f9a051ac9` | `2f9a051ac9` | 0 | Phase 2: ExternalDatabaseServiceTest passes mutable Set into setExternalDatabases |
| `worktree-agent-aeb9f848c17af88fe` | `a2db1d8ed4` | `a2db1d8ed4` | 0 | Phase 2 Task C: legacy SHA + username-salt password-hash migration |

**Recommendation:** delete all 19 of these once Paul confirms nothing on them
is uniquely valuable. They will never merge cleanly into the modern
`phase2-acl-migrate` tip and are noise in `git branch` output.

---

## 2. Categorization

The categories overlap (a branch can be both `B` and `E`, both `C` and `E`).
That is by design - `Cat` tells you the **safety** of the merge, while `E`
tells you the **file-conflict footprint** that drives ordering within a batch.

### Category A: independent, doc-only (31 branches)

Adds a single `.md` (recce, audit, roadmap). Zero risk of code conflict; the
only collision is if two branches add the *same filename*. Spot-check the
file list at merge time.

Members (31):
```
aclentryvoter-recce          actuator-recce           afterinv-phase-c-prep
afterinvocation-recce        aspectj-deeper           cacheable-audit
curation-ui-contract         ee-svc-decomp-recce      gemma-rest-standalone-recce
gemma-web-retire             gsec-version-align       hibernate-envers-audit
hibernate-l2-tune            hibernate-type-audit     java21-phase1
java21-readiness             junit5-recce             listenablefuture
logging-modernize            lombok-audit             maven-release-recce
mockito-modernize            persister-delete-plan    persister-recce
resttemplate-audit           session-refresh-v2       spring-boot-3-recce
spring-profiles-audit        spring-security-7-recce  static-analysis-audit
validation-audit
```

### Category B: independent code-only (54 branches)

Single-purpose code changes that - per the per-branch file lists - do not
collide with any other CODE branch we identified. Most are 1-3 commits ahead.
Still need a smoke build before merge (compile + targeted tests), but they
will not interlock.

Notable members (representative): `aclvoter-x1-wrappers`, `basejersey-cleanup`,
`commonslog-to-slf4j`, `hikari-modernize`, `jsr305-cleanup`, `jstl-jakarta`,
`profile-cleanup`, `lombok-cleanup`, `restclient-migrate`, `secured-prauthorize`,
`servlet6-audit`, `session-getreference`, `spring6-deprecation-hunt`, all six
XML->Java conversion branches (`xml-config-kickoff`, `xml-datasource`,
`xml-gemma-cli`, `xml-gemma-rest`, `xml-hibernate`, `xml-schedule`,
`xml-security`), `rest-security-config`, `gemma-rest-bootstrap`, plus the
single-commit fixture work (`fixture-factories-2`, `fixture-bioassay`).

### Category C: stacked code chains (15 branches across 6 chains)

These share prefix commits. Identified by `git log baseline..branch` showing
the same SHAs across multiple branches. **Merge the root first; later
branches will be empty merges (already-present commits) after their prereq
lands.**

#### C.1 - BK->Persister chain (15 branches, the giant one)

Linear dependency: `bk-consolidation` (commit `ffe6b0293e`)
  -> `persister-step2 / CommonPersister` (`a1ca482301`)
    -> leaf branches each picking up further work:
      - `persister-step3-ad`        (ArrayDesign rewire)        +1 commit
      - `relationshippersister`     (relationships rewire)      +1 commit
      - `genome-chunk-51`           (BioSequence/Chromosome)    +1 commit
        -> `genome-chunk-52`        (Taxon)                     +1 commit
          -> `genome-chunk-53-prep` (GeneWriteService skeleton + tests)
            -> `genome-chunk-53-taxonfix` (Chromosome.taxon-fill + GI test)
            -> `genome-chunk-54-cutover`  (NOTE: HEAD is same SHA as 5.3-prep
              and 5.3-taxonfix in inventory - the cutover work may live on a
              different HEAD than the branch name implies; verify before
              merge.)
      - `expression-chunk-e1`       (cascade-override collapse) +1 commit
        -> `e2` (EeWriteService skeleton + delegate)
          -> `e3` (collapse ExpressionPersister to thin delegate)
            -> `e4` (cut over GeoServiceImpl + SimpleEEDataLoaderImpl)
              -> `ee-proxy-fix` (proxy autowire)
                -> `e5` (deprecate ExpressionPersister)

`expressionpersister-recce` and `persister-genome` are doc-class but
classified B (3-commit doc-with-related-touches) - fold their recce docs in
as Cat A quickfires before the chain.

#### C.2 - AfterInvocation chain (4 branches)

Linear:
- `afterinv-phase-a`        (3 commits: replace AFTER_ACL_READ + COLLECTION + MAP, then unwire) - 4 ahead
- `afterinv-phase-b-quiet`  (+1 commit on top of phase-a)
- `afterinv-phase-b-vo`     (+1 commit on top of phase-b-quiet)
- `afterinv-phase-b-cs-dv`  (+2 commits on top of phase-b-quiet, **parallel** to b-vo)

So `phase-a -> phase-b-quiet -> {phase-b-vo, phase-b-cs-dv}` is a Y-shape.
The two B leaves diverge after b-quiet and must merge independently. Watch
for overlap in `MethodSecurityConfig.java` and
`applicationContext-security.xml`.

#### C.3 - JUnit 5 chain (2 branches, both pom.xml-touching)

- `junit5-phase-a`     (3 commits: deps + pilot + result doc)
- `junit5-phase-b0`    (3 more commits on top: @Tag + dual selector + IT pilot)

Linear `phase-a -> phase-b0`. Pre-conflicts with all the other pom-touchers
in Category E.

#### C.4 - Cache/Ehcache chain (4 branches)

- `ehcache-cachemanager-fix`   (2 commits: JCacheCacheManager + introspection)
- `metrics-jcache-restore`     (+1 commit: metrics binder)
- `l2-cache-bound`             (+2 commits: bounded regions + delete dead ehcache.xml)
- `querycache-shard`           (same HEAD `71bcc9ee51` as `l2-cache-bound` per inventory - looks like one of these was a no-op or accidental clone)

Tree: `ehcache-cachemanager-fix -> {metrics-jcache-restore, l2-cache-bound}`.
The `querycache-shard` / `l2-cache-bound` SHA collision needs verification
before merging - likely safe to merge once after picking the canonical one.

#### C.5 - Executor / virtual-thread chain (2 branches)

- `executor-vt-callers`   (3 commits: fetcher + cli migrations)
- `executor-vt-callers-2` (+2 commits: GeoFamilyParser/BrowserService + deferral doc)

Linear: `executor-vt-callers -> executor-vt-callers-2`.
`executor-virtual-prep` is the recce that precedes them.

#### C.6 - EE service decomposition chain (2 branches)

- `ee-svc-decomp-p1`  (2 commits: extract ReadService + break cycles)
- `ee-svc-decomp-p15` (+2 commits: bucket B/G extract + @Lazy fix)

Linear: `p1 -> p15`. `ee-svc-decomp-recce` (Cat A) precedes.

### Category D: doc that references unmerged code (5 branches)

Pure-`.md` files that reference branch names which haven't landed yet. They
**are safe to merge** (still Cat A merge mechanics) but the prose will look
oddly forward-referential until the referenced branches land. The captioning
contract is fine because the recce doc *is* the description of the unmerged
work.

Identified by branch-name + content:
- `gemma-web-retire`           - references gemma-rest-bootstrap + standalone-recce
- `spring-security-7-recce`    - references afterinv-phase-* chain
- `aclentryvoter-recce`        - references gsec hql + aclvoter-x1-wrappers
- `persister-delete-plan`      - references the full C.1 chain
- `afterinv-phase-c-prep`      - references all phase-a/b leaves

Not breaking anything; just note in the commit message that the doc previews
work which lands later.

### Category E: pom.xml-touching (9 branches, **chronic conflict hot zone**)

These all modify root `pom.xml` and will collide pairwise on every merge:

```
maven-modernize       (plugin + dep bumps)        - 3 commits
framework-bump-recce  (doc only, but touches pom) - 4 commits
slf4j-bump            (slf4j 1.7 -> 2.0)          - 2 commits
spring-boot-bom       (import BOM + pin overrides)- 3 commits
release-small-fixes   (git-commit-id + enforcer)  - 3 commits
junit5-phase-a        (jupiter/vintage)           - 3 commits
junit5-phase-b0       (failsafe selector)         - 6 commits (stacks on phase-a)
impl-autowire-rule    (archunit dep + rule)       - 2 commits
openapi-audit         (swagger bump)              - 1 commit
```

Each one needs a re-resolve after the previous lands. See Section 4 for the
predicted conflict matrix and Section 3 for the consolidated merge order.

---

## 3. Recommended Merge Order

### Wave 1 - Cat A doc-only quick wins (no risk, day-1 flurry)

Land all 31 doc-only branches in any order. They produce no merge conflicts
(disjoint file paths) and immediately give Paul + reviewers visibility into
the recces.

**First batch (recommended 10 highest-signal doc-only landings):**
1. `worktree-spring-boot-3-recce`     - biggest strategic gate
2. `worktree-java21-readiness`        - companion to spring-boot-3-recce
3. `worktree-spring-security-7-recce` - explains the afterinv chain target
4. `worktree-afterinvocation-recce`   - the afterinv roadmap itself
5. `worktree-persister-recce`         - frames C.1
6. `worktree-aspectj-deeper`          - frames the cache/ehcache cluster
7. `worktree-hibernate-l2-tune`       - companion to cache cluster
8. `worktree-junit5-recce`            - frames C.3
9. `worktree-gemma-web-retire`        - strategic; frames Cat D references
10. `worktree-mockito-modernize`      - orthogonal but cheap

After that batch, drain the remaining 21 Cat-A branches without ceremony.

### Wave 2 - Cat B small code wins (compile + smoke per branch)

Single-commit code branches with no overlap. Recommend in this order:
1. `jsr305-cleanup`          - wide-impact rename, do first to maximize follow-on rebase reuse
2. `spring6-deprecation-hunt`- small surface, low risk
3. `session-getreference`    - 7-site rename
4. `jstl-jakarta`            - single gemma-web pom bump (isolated to gemma-web/pom.xml)
5. `restclient-migrate`      - single-class migration
6. `secured-prauthorize`     - single-class @Secured->@PreAuthorize
7. `servlet6-audit`          - jakarta.servlet cleanup
8. `hikari-modernize`        - HikariCP config audit
9. `basejersey-cleanup`      - @RunWith retirement
10. `aclvoter-x1-wrappers`   - parallel-run wrappers (doesn't unwire legacy)
11. `commonslog-to-slf4j`    - broad rename (188 sites)
12. `profile-cleanup`        - @Profile constants
13. `lombok-cleanup`         - Value->record + sneakythrows
14. `gsec-hql-v2`            - gsec HQL -> JdbcTemplate (2 queries)
15. `gsec-hql-continued`     - gsec HQL -> JdbcTemplate (continuation; verify no
                                overlap with gsec-hql-v2 first)
16. `executor-vt-callers` -> `executor-vt-callers-2`  (chain C.5)
17. `ee-svc-decomp-p1` -> `ee-svc-decomp-p15`         (chain C.6)
18. `rest-security-config` -> `gemma-rest-bootstrap`  (sequential, both gemma-rest)
19. `fixture-factories-2` -> `fixture-bioassay`       (TaxonFactory -> BioAssayFactory)
20. `annotations-writeback`  - 4 commits, touches EE service

### Wave 3 - pom.xml consolidation (Cat E, sequence-critical)

Merge in this order, running `mvn validate` between each (Paul, not the
agent):

1. `maven-modernize`        - plugin + dep bumps, foundational
2. `release-small-fixes`    - enforcer + git-commit-id bump
3. `slf4j-bump`             - slf4j 1.7 -> 2.0 + log4j-slf4j2-impl
4. `openapi-audit`          - swagger 2.2.42 -> 2.2.50
5. `impl-autowire-rule`     - adds ArchUnit dep
6. `junit5-phase-a` -> `junit5-phase-b0`  (linear; both modify pom for jupiter/failsafe)
7. `spring-boot-bom`        - biggest blast radius, do last
8. `framework-bump-recce`   - doc-only but pom-touching; merge after BOM lands
                              so the doc's version numbers match reality

Each merge in this wave is expected to need a hand-resolve on `<version>`
clusters. The conflict-resolution strategy is "newer of the two; re-check
`mvn validate` afterwards".

### Wave 4 - XML->Java config migrations (Cat B but sensitive)

All single-commit, but they collectively rewire context loading. Recommend
landing all together in a single integration session:

1. `xml-config-kickoff`     (serviceBeans + component-scan)
2. `xml-hibernate`          (HibernateConfig)
3. `xml-datasource`         (dataSource)
4. `xml-schedule`           (schedule)
5. `xml-gemma-cli`          (gemma-cli component-scan)
6. `xml-gemma-rest`         (gemma-rest applicationContext-*)
7. `xml-security`           (applicationContext-security - **last**; this is
                            the chunk that the AfterInvocation chain expects
                            to still exist when it modifies it)

**Critical ordering note:** `xml-security` conflicts with the entire
AfterInvocation chain (see Section 4). Land **before** Wave 5 if going Java-
config; land **after** if keeping XML as the source of truth.

### Wave 5 - AfterInvocation chain (Cat C.2)

Sequence: `afterinv-phase-a -> afterinv-phase-b-quiet -> {afterinv-phase-b-vo, afterinv-phase-b-cs-dv}`.
After all four leaves: optional `afterinv-phase-c-prep` (Cat A doc, can
merge in Wave 1 actually). Touches `MethodSecurityConfig.java` and
`applicationContext-security.xml` repeatedly - each merge needs a re-resolve
even within the chain.

### Wave 6 - BK->Persister chain (Cat C.1, biggest)

Strict order:

1. `bk-consolidation`        - foundation
2. `persister-step2`         - CommonPersister rewire
3. (parallel after step2):
   - `persister-step3-ad`    - ArrayDesignPersister
   - `relationshippersister` - RelationshipPersister
   - `genome-chunk-51`       - GenomePersister BioSeq/Chrom
4. `genome-chunk-52`         - GenomePersister Taxon (after 5.1)
5. **VERIFY:** `genome-chunk-53-prep`, `genome-chunk-53-taxonfix`,
   `genome-chunk-54-cutover` all show `HEAD=74e607c32d` in the inventory.
   That looks wrong - open question for Paul (see Section 7).
6. `expression-chunk-e1 -> e2 -> e3 -> e4 -> ee-proxy-fix -> e5` (strict linear)

After Wave 6, ExpressionPersister is deprecated and GenomePersister is
mostly delegated to a GeneWriteService.

### Wave 7 - Cache/Ehcache chain (Cat C.4)

`ehcache-cachemanager-fix -> metrics-jcache-restore -> l2-cache-bound`.
Verify `querycache-shard` is not a duplicate before deciding whether it has
unique commits to land.

### Wave 8 - Cleanup orphans

Delete the 19 pre-Phase-3 branches in Section 1b after Paul confirms.

---

## 4. Predicted Conflict Files

Top 10 most-touched files across Phase-3 branches:

| File | Branches | Conflict regions |
|---|---|---|
| `gemma-core/.../persistence/util/BusinessKey.java` | 18 | Helper methods added by C.1 root, then referenced by leaves |
| `gemma-core/.../persistence/util/BusinessKeyTest.java` | 17 | New test cases per leaf |
| `gemma-core/.../persister/CommonPersister.java` | 16 | Re-write in step2, then leaves further trim |
| **`pom.xml`** (root) | 9 | Version blocks; every E-branch touches |
| `gemma-core/.../persister/ExpressionPersister.java` | 7 | E1->E5 progressively gut it |
| `gemma-core/.../applicationContext-security.xml` | 6 | AfterInvocation chain + `xml-security` |
| `gemma-core/.../experiment/ExpressionExperimentService.java` | 6 | AfterInvocation @PostAuthorize/@PostFilter annotations + `annotations-writeback` |
| `gemma-core/.../persister/GenomePersister.java` | 6 | 5.1 -> 5.2 -> 5.3/5.4 chain + `jsr305-cleanup` rename |
| various `*Service.java` (~5 each) | 5 each | AfterInvocation annotation sweep across read services |
| `gemma-rest/.../DatasetsWebService.java` | 4 | `annotations-writeback` + secured->preauth + others |

### 4a. Conflict-resolution strategy per chronic file

**`pom.xml` (root)** - accept-both on `<version>` updates (newer wins),
accept-both on new `<dependency>` blocks (concatenate in alphabetical order),
re-run `mvn validate` after each merge to flush convergence issues.

**`BusinessKey.java` / `BusinessKeyTest.java`** - accept-both within the C.1
chain because each leaf adds a distinct helper. If two leaves add the *same*
helper signature, that means C.1 has an unrecorded merge inside it; flag and
inspect.

**`CommonPersister.java`** - strict linear; only `persister-step2` rewrites
the core; everything after should be additive. Conflict here means a leaf
made an undocumented edit; investigate.

**`ExpressionPersister.java`** - E1 through E5 are linear progressive trims.
If you merge them out of order, accept the *later* version (it's strictly
smaller).

**`applicationContext-security.xml`** - between AfterInvocation chain and
`xml-security`: pick one source-of-truth axis. If keeping XML, merge
AfterInvocation chain first and accept its `<bean>` removals, then never
merge `xml-security`. If migrating to Java config, merge `xml-security`
*last* and re-port the AfterInvocation removals into the new Java config.

**`MethodSecurityConfig.java`** - AfterInvocation chain progressively
unwires providers. Strict linear merge order resolves cleanly. Out-of-order
merges hit "method removed" / "method renamed" pairs.

**`GenomePersister.java`** - 5.1 -> 5.2 -> 5.3-prep -> 5.3-taxonfix -> 5.4
linear. Out-of-order causes "field already initialized" semantics
collisions. `jsr305-cleanup` only renames imports - accept the chain side.

---

## 5. Smoke-test gates between merges

Per wave, the agent (or Paul) should:

- **Wave 1 (docs):** No build needed. Visual diff of merged tree.
- **Wave 2 (small code):** `mvn -pl gemma-core -am compile` after every 3-5
  merges; targeted test for any branch that has its own test (`mvn -pl
  gemma-core -Dtest=BusinessKeyTest test`, etc.).
- **Wave 3 (pom):** `mvn validate` after **every** merge. Then
  `mvn dependency:tree -Dverbose=true` after BOM lands.
- **Wave 4 (XML->Java):** `mvn -pl gemma-core compile` after every merge,
  plus one boot-the-context smoke test after `xml-security` lands.
- **Wave 5 (AfterInvocation):** `mvn -pl gemma-core compile` + targeted
  AfterInvocation tests after every leaf.
- **Wave 6 (Persister chain):** Compile + run `ExpressionPersisterTest`,
  `GenomePersisterTest`, `BusinessKeyTest` after every step.
  ExpressionPersister chain needs a full `mvn verify` on gemma-core after E5.
- **Wave 7 (Cache):** Compile + boot-context smoke (Ehcache region init
  logs must not change beyond the documented additions).

---

## 6. Conflict-resolution strategy (high-level)

1. **Prefer accept-both for additive changes.** New dependency blocks, new
   tests, new helper methods - concatenate, sort if appropriate.
2. **Prefer the chain-leaf for progressive-trim conflicts.** ExpressionPersister
   E5 is strictly smaller than E1; the larger version is stale.
3. **Run `mvn validate` after every pom merge.** Dependency convergence is
   the most common silent regression.
4. **Use `git rerere` if not already enabled.** Paul will hit the same
   `pom.xml` `<version>` conflicts repeatedly during Wave 3.
5. **Re-rebase each chain on the just-merged tip before continuing.** E.g.,
   after Wave 3 lands, before Wave 6, rebase the BK->Persister chain HEAD onto
   the new `phase2-acl-migrate` tip. Otherwise the C.1 leaves will conflict
   on `BusinessKey.java` against the new BOM-driven Hibernate types.

---

## 7. Open questions for Paul

1. **`genome-chunk-53-prep`, `genome-chunk-53-taxonfix`, `genome-chunk-54-cutover`
   all have the same HEAD `74e607c32d`.** The inventory step shows three
   branches with identical SHAs and identical ahead-counts (6/6/6 - they all
   share the same chain, but the cutover should be ahead of taxonfix which
   should be ahead of prep). That means either (a) the three branches were
   created from the same commit and never diverged, (b) the cutover work
   landed somewhere we haven't found, or (c) the worktree dirs hold
   uncommitted work. **Action:** verify before merging any of the genome
   5.3/5.4 work. Suggested: `git status` in each worktree dir and check for
   uncommitted staged or working-tree changes.
2. **`l2-cache-bound` and `querycache-shard` have the same HEAD `71bcc9ee51`.**
   Same diagnostic question - are they duplicates, or is one missing the
   actual shard work?
3. **`xml-security` vs AfterInvocation:** which is the source-of-truth axis
   going forward? See Section 4a. Picking up-front avoids a re-port.
4. **Do we delete the 19 orphan/pre-Phase-3 branches** listed in Section 1b
   now, or keep them until Phase 4 lands?
5. **`commonslog-to-slf4j` (188 sites) plus `slf4j-bump`:** is the @CommonsLog
   rename meant to land before or after the slf4j 1.7->2.0 bump? Either
   order works; recommend renaming first (more files touched), then bumping
   the API.
6. **AfterInvocation Y-branches (`b-vo` parallel to `b-cs-dv`):** do we merge
   both into `phase2-acl-migrate`, or pick one as authoritative? They diverge
   from `phase-b-quiet`; merging both means accepting two independent
   additions to `MethodSecurityConfig.java`. Recommend: merge `b-vo` first
   (alphabetical), then `b-cs-dv` and resolve.
7. **Cat D forward-references in doc-only branches:** OK to land these
   "early" (as part of Wave 1) and accept the temporary forward reference,
   or hold them until their referenced code lands? Recommend: land early -
   the recce *is* the description of the unmerged work.

---

## 8. Summary statistics

- **Total `worktree-*` branches:** 124
- **Phase 3 (off baseline `08e760bdaf`):** 105
- **Pre-Phase-3 orphans:** 19
- **Category A (doc-only):** 31
- **Category B (independent code):** 54
- **Category C (stacked chain):** 15 (across 6 chains: BK-PERSISTER, AFTERINV,
  JUNIT5, CACHE, EXECUTOR-VT, EE-SVC-DECOMP)
- **Category E (pom.xml-touching):** 9
- **Cat overlap (B/E):** `framework-bump-recce`, `impl-autowire-rule`,
  `maven-modernize`, `openapi-audit`, `release-small-fixes`, `slf4j-bump`,
  `spring-boot-bom`
- **Cat overlap (C/E):** `junit5-phase-a`, `junit5-phase-b0`

**Recommended first batch (Wave 1, 10 doc-only quick wins):** see Section 3.
