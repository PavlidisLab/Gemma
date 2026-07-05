# docs/ index

Working design docs, recces, audits, plans, and session notes for Gemma. Auto-listed; 143 files. Moved out of the repo root 2026-07-05 to declutter. `README.md` and `CLAUDE.md` stay at root.

## Recces (35)

- [`ACTUATOR_RECCE.md`](ACTUATOR_RECCE.md) — Actuator-style observability endpoints — Phase 3 recce
- [`AGENT_WRITEBACK_RECCE.md`](AGENT_WRITEBACK_RECCE.md) — Agent writeback — Gemma-side reconnaissance
- [`AUDIT_AS_WORKFLOW_RECCE.md`](AUDIT_AS_WORKFLOW_RECCE.md) — Audit-system as workflow / ticket tracker — Phase 3 recce
- [`AUDIT_MIGRATION_PHASE_C_RECCE.md`](AUDIT_MIGRATION_PHASE_C_RECCE.md) — Audit migration Phase C — scoping recce
- [`AUDIT_PHASE_C_RECCE.md`](AUDIT_PHASE_C_RECCE.md) — AUDIT_PHASE_C_RECCE.md
- [`AUTH_FOR_SPA_RECCE.md`](AUTH_FOR_SPA_RECCE.md) — Auth-for-SPA recce: gemma-rest auth flows for the curation-UI
- [`BASECODE_MATH_LINEARMODELS_RECCE.md`](BASECODE_MATH_LINEARMODELS_RECCE.md) — baseCode `math` + `linearmodels` (+ neighbours) — pull-in recce
- [`BASECODE_MATRIX_RECCE.md`](BASECODE_MATRIX_RECCE.md) — baseCode `dataStructure.matrix` subsystem — pull-in recce
- [`COEXPRESSION_ORPHAN_RECCE.md`](COEXPRESSION_ORPHAN_RECCE.md) — Coexpression-tables orphan recce
- [`CONTAINER_IMAGE_RECCE.md`](CONTAINER_IMAGE_RECCE.md) — Container image recce -- gemma-rest WAR on Tomcat 10.1
- [`CONTAINER_RECCE.md`](CONTAINER_RECCE.md) — Container / Docker readiness recce
- [`CORS_RECCE.md`](CORS_RECCE.md) — CORS Recce — gemma-rest standalone WAR for curation-UI
- [`CURSOR_PAGINATION_RECCE.md`](CURSOR_PAGINATION_RECCE.md) — Cursor-Based Pagination — Reconnaissance
- [`DEA_FINDBYGENE_COLDCACHE_RECCE.md`](DEA_FINDBYGENE_COLDCACHE_RECCE.md) — DEA findByGene cold-cache — fix recce
- [`EXTERNAL_PIPELINE_HANDOFF_RECCE.md`](EXTERNAL_PIPELINE_HANDOFF_RECCE.md) — External pipeline handoff via Tickets + Externalized events — Phase 3 recce
- [`GEMMA_REST_STANDALONE_RECCE.md`](GEMMA_REST_STANDALONE_RECCE.md) — gemma-rest standalone packaging — RECCE
- [`GENE_PAGE_REWORK_RECCE.md`](GENE_PAGE_REWORK_RECCE.md) — Gene page rework — call-surface recce
- [`GSEC_PHASE_C_RECCE.md`](GSEC_PHASE_C_RECCE.md) — gsec Phase C recce — drop `GsecAclServiceAdapter`
- [`HEATMAP_REWRITE_RECCE.md`](HEATMAP_REWRITE_RECCE.md) — Heatmap data generation — client-side rewrite recce
- [`JDK21_FEATURES_RECCE.md`](JDK21_FEATURES_RECCE.md) — JDK 21 features — migration recce
- [`LOAD_DETAILS_COLD_PATH_RECCE.md`](LOAD_DETAILS_COLD_PATH_RECCE.md) — `loadDetailsValueObjectsByIds` cold-path anatomy
- [`LOGGING_MODERNIZATION_RECCE.md`](LOGGING_MODERNIZATION_RECCE.md) — Structured logging + OpenTelemetry — Phase 3 recce
- [`LUCENE_HS_READINESS_RECCE.md`](LUCENE_HS_READINESS_RECCE.md) — Lucene 10 + Hibernate Search 8 readiness recce
- [`MOCK_FIXTURE_CONVERSION_RECCE.md`](MOCK_FIXTURE_CONVERSION_RECCE.md) — Mock fixture conversion recce — top-N tests to convert
- [`OBJECT_STORAGE_RECCE.md`](OBJECT_STORAGE_RECCE.md) — Object-Storage Abstraction — Reconnaissance
- [`OPENTELEMETRY_RECCE.md`](OPENTELEMETRY_RECCE.md) — OpenTelemetry tracing — Phase 3 detailed recce
- [`PERSISTER_BK_STEP1_RECCE.md`](PERSISTER_BK_STEP1_RECCE.md) — persisterHelper retirement -- Step 1 (BK consolidation) recce
- [`PERSISTER_CACHE_LIFT_RECCE.md`](PERSISTER_CACHE_LIFT_RECCE.md) — persisterHelper retirement -- cache-lift recce
- [`PERSISTER_SHRINK_RECCE.md`](PERSISTER_SHRINK_RECCE.md) — PERSISTER_SHRINK_RECCE.md — shortest path to retiring `persisterHelper` post-Caches
- [`PIPELINES_AND_SCHEDULER_RECCE.md`](PIPELINES_AND_SCHEDULER_RECCE.md) — Pipelines + scheduler architecture recce
- [`PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md`](PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md) — Pipeline compute: test harness + monitoring/control surface — recce
- [`SAMPLES_DESIGN_PERF_RECCE.md`](SAMPLES_DESIGN_PERF_RECCE.md) — `/datasets/{id}/samples` and `/datasets/{id}/design` perf recce
- [`SEARCH_RECCE.md`](SEARCH_RECCE.md) — Search Subsystem Restoration — Reconnaissance
- [`SPRING_MODULITH_RECCE.md`](SPRING_MODULITH_RECCE.md) — Spring Modulith — Phase 3 reconnaissance
- [`WORKFLOW_GROUPS_RECCE.md`](WORKFLOW_GROUPS_RECCE.md) — Workflow Groups CRUD + `/datasets/{id}/groups` — Reconnaissance

## Audits (30)

- [`ASPECTJ_DEEPER_AUDIT.md`](ASPECTJ_DEEPER_AUDIT.md) — AspectJ Deeper Audit (Phase 3, post-`ASPECTJ_EHCACHE_AUDIT`)
- [`ASPECTJ_EHCACHE_AUDIT.md`](ASPECTJ_EHCACHE_AUDIT.md) — AspectJ + ehcache + JCache audit (Phase 3 infra)
- [`AUDIT_ADVICE_RETIREMENT_PLAN.md`](AUDIT_ADVICE_RETIREMENT_PLAN.md) — AuditAdvice retirement plan
- [`AUDIT_PHASE_C_HELPER_BUCKET_DISPOSITION.md`](AUDIT_PHASE_C_HELPER_BUCKET_DISPOSITION.md) — Audit migration Phase C — helper-bucket disposition
- [`AUDIT_SYSTEM_AUDIT.md`](AUDIT_SYSTEM_AUDIT.md) — AUDIT_SYSTEM_AUDIT.md
- [`BASECODE_DEP_AUDIT.md`](BASECODE_DEP_AUDIT.md) — baseCode dep audit — post-ontology pull-in
- [`CACHEABLE_AUDIT.md`](CACHEABLE_AUDIT.md) — `@Cacheable` Annotation Audit — Phase 3
- [`CI_CD_AUDIT.md`](CI_CD_AUDIT.md) — CI/CD Pipeline Audit
- [`CONFIG_AUDIT.md`](CONFIG_AUDIT.md) — Config Audit — Spring Profiles, `@Value`, Property Files
- [`DEPENDENCY_AUDIT.md`](DEPENDENCY_AUDIT.md) — Dependency vulnerability recce — post Boot-3.5.6 bump
- [`DWR_REST_GAP_AUDIT.md`](DWR_REST_GAP_AUDIT.md) — DWR → REST parity gap audit
- [`GEMMA_CLI_DEAD_CODE_AUDIT.md`](GEMMA_CLI_DEAD_CODE_AUDIT.md) — Gemma CLI dead-code audit
- [`HIBERNATE6_CASCADE_AUDIT.md`](HIBERNATE6_CASCADE_AUDIT.md) — Hibernate 6 cascade-strictness audit
- [`HIBERNATE_ENVERS_AUDIT.md`](HIBERNATE_ENVERS_AUDIT.md) — Hibernate Envers Audit
- [`HIBERNATE_L2_CACHE_AUDIT.md`](HIBERNATE_L2_CACHE_AUDIT.md) — Hibernate L2 cache region audit + tuning recommendations
- [`HIBERNATE_TYPE_AUDIT.md`](HIBERNATE_TYPE_AUDIT.md) — Hibernate 6 `@Type` / UserType audit
- [`HIKARICP_AUDIT.md`](HIKARICP_AUDIT.md) — HikariCP audit + modernization
- [`HQL_SQL_AUDIT.md`](HQL_SQL_AUDIT.md) — HQL / native SQL audit — gemma-core
- [`LISTENABLEFUTURE_AUDIT.md`](LISTENABLEFUTURE_AUDIT.md) — ListenableFuture Audit (Phase 3 Spring 6+ Modernization)
- [`LOGGING_AUDIT.md`](LOGGING_AUDIT.md) — Logging stack audit — Phase 3 Spring 6+ infrastructure modernization
- [`LOMBOK_AUDIT.md`](LOMBOK_AUDIT.md) — Lombok Audit — Phase 3 Spring 6+ Modernization Recce
- [`MAVEN_RELEASE_AUDIT.md`](MAVEN_RELEASE_AUDIT.md) — Maven Release / Version-Management Audit
- [`MIDDLE_TIER_AUDIT.md`](MIDDLE_TIER_AUDIT.md) — Middle-tier audit — gemma-cli, gemma-rest, gemma-web
- [`OPENAPI_AUDIT.md`](OPENAPI_AUDIT.md) — OpenAPI / Swagger Integration Audit (Phase 3)
- [`PIPELINESTATUS_WIRE_AUDIT.md`](PIPELINESTATUS_WIRE_AUDIT.md) — Pipeline Status wire-shape audit — UI vs gemma-rest
- [`RESTTEMPLATE_AUDIT.md`](RESTTEMPLATE_AUDIT.md) — RestTemplate audit (Phase 3 Spring 6+ modernization)
- [`SERVLET6_AUDIT.md`](SERVLET6_AUDIT.md) — Servlet 6 / Jakarta Servlet API Compliance Audit
- [`SPRING_PROFILES_AUDIT.md`](SPRING_PROFILES_AUDIT.md) — Spring Profiles Audit (Phase 3 — Spring 6 modernization)
- [`STATIC_ANALYSIS_AUDIT.md`](STATIC_ANALYSIS_AUDIT.md) — Static Analysis Audit — Phase 3 Spring 6+ Infrastructure
- [`VALIDATION_AUDIT.md`](VALIDATION_AUDIT.md) — Jakarta Validation Audit (Bean Validation 3.0 / JSR-380)

## Plans & roadmaps (16)

- [`AFTER_INVOCATION_PHASE_C_PLAN.md`](AFTER_INVOCATION_PHASE_C_PLAN.md) — AfterInvocation Phase C: @EnableMethodSecurity migration plan
- [`BRANCH_MERGE_PLAN.md`](BRANCH_MERGE_PLAN.md) — Phase 3 Branch Merge Plan
- [`CURSOR_PAGINATION_STEP1_PLAN.md`](CURSOR_PAGINATION_STEP1_PLAN.md) — Cursor Pagination — Step 1 Plan + Status
- [`EE_SERVICE_DECOMPOSITION_ROADMAP.md`](EE_SERVICE_DECOMPOSITION_ROADMAP.md) — `ExpressionExperimentService` Decomposition Roadmap
- [`EXPRESSIONPERSISTER_MIGRATION_PLAN.md`](EXPRESSIONPERSISTER_MIGRATION_PLAN.md) — ExpressionPersister retirement: migration plan (risk-5 recce)
- [`GEMMA_REST_STANDALONE_ROADMAP.md`](GEMMA_REST_STANDALONE_ROADMAP.md) — gemma-rest standalone packaging — roadmap
- [`GEMMA_WEB_RETIREMENT_PLAN.md`](GEMMA_WEB_RETIREMENT_PLAN.md) — gemma-web retirement plan
- [`GENOMEPERSISTER_MIGRATION_PLAN.md`](GENOMEPERSISTER_MIGRATION_PLAN.md) — GenomePersister retirement: migration plan (risk-5 recce)
- [`GSEC_ABSORPTION_ROADMAP.md`](GSEC_ABSORPTION_ROADMAP.md) — gsec absorption roadmap
- [`JUNIT5_MIGRATION_ROADMAP.md`](JUNIT5_MIGRATION_ROADMAP.md) — JUnit 5 (Jupiter) Migration Roadmap
- [`PERSISTER_DELETION_PLAN.md`](PERSISTER_DELETION_PLAN.md) — PersisterHelper retirement: deletion + dispatch-facade plan
- [`PERSISTER_REPLACEMENT_ROADMAP.md`](PERSISTER_REPLACEMENT_ROADMAP.md) — persisterHelper retirement: replacement roadmap
- [`SPRING6_DEPRECATION_ROADMAP.md`](SPRING6_DEPRECATION_ROADMAP.md) — Spring 6 / Spring Security 6 / Hibernate 6 deprecation roadmap
- [`WORKTREE_CLEANUP_PLAN.md`](WORKTREE_CLEANUP_PLAN.md) — Worktree cleanup plan
- [`WORKTREE_CLEANUP_PLAN_v2.md`](WORKTREE_CLEANUP_PLAN_v2.md) — Worktree cleanup plan — v2
- [`WORKTREE_CLEANUP_PLAN_v3.md`](WORKTREE_CLEANUP_PLAN_v3.md) — Worktree cleanup plan v3

## Handoffs (3)

- [`CURATION_UI_HANDOFF_INVENTORY.md`](CURATION_UI_HANDOFF_INVENTORY.md) — Curation-UI HANDOFF asks vs gemma-rest implementation
- [`PHASE_2_ACL_HANDOFF.md`](PHASE_2_ACL_HANDOFF.md) — Phase 2 ACL Migration — Handoff (2026-05-18)
- [`PHASE_2_HANDOFF.md`](PHASE_2_HANDOFF.md) — Phase 2 (Spring 6 / Hibernate 6 / jakarta) — handoff

## Session notes & snapshots (3)

- [`NCBI_RATE_LIMIT_NOTE.md`](NCBI_RATE_LIMIT_NOTE.md) — NCBI eutils rate-limit note
- [`SESSION_CLOSE_NOTE_2026-05-19.md`](SESSION_CLOSE_NOTE_2026-05-19.md) — Session close note — 2026-05-19
- [`VALIDATION_OPTIN_NOTES.md`](VALIDATION_OPTIN_NOTES.md) — Jakarta Bean Validation (JSR-380) — opt-in notes

## Other design docs (56)

- [`ACL_ENTRY_VOTER_MIGRATION.md`](ACL_ENTRY_VOTER_MIGRATION.md) — ACL AclEntryVoter family migration plan
- [`AFTER_INVOCATION_MIGRATION.md`](AFTER_INVOCATION_MIGRATION.md) — AfterInvocation → Spring Security 6 Modern API Migration Roadmap
- [`ASPECTJ_INVARIANT_CHECKLIST.md`](ASPECTJ_INVARIANT_CHECKLIST.md) — AspectJ JDK-Proxy Invariant: Carry-Forward Checklist for `@Configuration` Migration
- [`CONTAINER_CONFIG.md`](CONTAINER_CONFIG.md) — Container Config — Env-Var-Only Gemma Configuration
- [`CRUFT_INVENTORY.md`](CRUFT_INVENTORY.md) — Phase 3 cruft inventory + LoC endstate projection
- [`CURATION_COMMIT_SPEC.md`](CURATION_COMMIT_SPEC.md) — Curation commit — composite all-or-none write (`PUT /datasets/{id}/curation`)
- [`CURATION_DETAILS_RETIREMENT.md`](CURATION_DETAILS_RETIREMENT.md) — CurationDetails write-path retirement — phase 1
- [`EXECUTOR_VIRTUAL_THREAD_PREP.md`](EXECUTOR_VIRTUAL_THREAD_PREP.md) — Executor centralization audit + virtual-thread prep
- [`EXPRESSION_PERSISTER_DELETION.md`](EXPRESSION_PERSISTER_DELETION.md) — ExpressionPersister Final Deletion Tracker
- [`EXTERNAL_URL_REACHABILITY.md`](EXTERNAL_URL_REACHABILITY.md) — External URL reachability probe
- [`FAILSAFE_RESIDUAL_TRIAGE.md`](FAILSAFE_RESIDUAL_TRIAGE.md) — Failsafe Residual Triage
- [`FLYWAY_PROD_FOLLOWUP.md`](FLYWAY_PROD_FOLLOWUP.md) — Flyway production wiring — follow-on session
- [`FRAMEWORK_BUMP_FEASIBILITY.md`](FRAMEWORK_BUMP_FEASIBILITY.md) — Phase 3 framework bump — feasibility recce
- [`GEMMA_CLI_MODERNIZATION.md`](GEMMA_CLI_MODERNIZATION.md) — gemma-cli runner modernization recce
- [`GEMMA_CURATION_CALL_SURFACE.md`](GEMMA_CURATION_CALL_SURFACE.md) — Gemma curation-flow call-surface map
- [`GEMMA_CURATION_FEATURE_WISHLIST.md`](GEMMA_CURATION_FEATURE_WISHLIST.md) — Gemma curation-agents — feature wishlist
- [`GEMMA_CURATION_UI_CONTRACT.md`](GEMMA_CURATION_UI_CONTRACT.md) — gemma-curation-ui REST API contract audit
- [`GEMMA_REST_BOOTSTRAP_PHASE1.md`](GEMMA_REST_BOOTSTRAP_PHASE1.md) — gemma-rest standalone bootstrap — Phase 1 landed
- [`GEMMA_UI_ENDPOINT_GAP.md`](GEMMA_UI_ENDPOINT_GAP.md) — gemma-ui ↔ gemma-rest endpoint gap analysis
- [`GEMMA_UI_FEATURE_CATALOG.md`](GEMMA_UI_FEATURE_CATALOG.md) — Gemma UI feature catalog — what's needed to fully replace gemma-web
- [`GSEC_HQL_DEPRECATION.md`](GSEC_HQL_DEPRECATION.md) — gsec ACL HQL Deprecation — Inventory and Migration Playbook
- [`GSEC_HQL_DEPRECATION_CONTINUED.md`](GSEC_HQL_DEPRECATION_CONTINUED.md) — Phase 3 — gsec HQL deprecation (continued)
- [`GSEC_VERSION_ALIGNMENT.md`](GSEC_VERSION_ALIGNMENT.md) — gsec version alignment — feasibility recce
- [`JAVA21_PHASE1_RESULT.md`](JAVA21_PHASE1_RESULT.md) — Java 21 Phase 1 — Pre-bump audit (Lombok / AspectJ / JaCoCo)
- [`JAVA21_READINESS.md`](JAVA21_READINESS.md) — Java 21 Readiness Recce
- [`JUNIT5_BASETEST_MIGRATION.md`](JUNIT5_BASETEST_MIGRATION.md) — JUnit 5 Migration — Phase B0: BaseTest hierarchy (parallel base classes)
- [`JUNIT5_PHASE_A_RESULT.md`](JUNIT5_PHASE_A_RESULT.md) — JUnit 5 (Jupiter) Migration — Phase A result
- [`MAVEN_MODERNIZATION.md`](MAVEN_MODERNIZATION.md) — Maven Plugin + Dependency Modernization
- [`MOCKITO_MODERNIZATION.md`](MOCKITO_MODERNIZATION.md) — Mockito Modernization Recce (Phase 3)
- [`PERF_PROBE_ANNOTATIONS.md`](PERF_PROBE_ANNOTATIONS.md) — Perf probe — Annotations + Characteristic (live gemd, 2026-05-20)
- [`PERF_PROBE_DATA_EXPORTS.md`](PERF_PROBE_DATA_EXPORTS.md) — Perf probe — TSV/data-export + DEA-run paths (live gemd)
- [`PERF_PROBE_REPORT.md`](PERF_PROBE_REPORT.md) — Live-gemd perf probe (2026-05-20 phase2-acl-migrate)
- [`PERF_PROBE_REPORT_ROUND2.md`](PERF_PROBE_REPORT_ROUND2.md) — Live-gemd perf probe — round 2 (post-ACL-EXISTS) (2026-05-20 phase2-acl-migrate)
- [`PERF_PROBE_REPORT_ROUND3.md`](PERF_PROBE_REPORT_ROUND3.md) — Live-gemd perf probe round 3 — vectors, matrices, DEA results, visualization
- [`PERF_PROBE_REPORT_ROUND4.md`](PERF_PROBE_REPORT_ROUND4.md) — Live-gemd perf probe round 4 — single-cell hot paths
- [`PERF_PROBE_SEARCH.md`](PERF_PROBE_SEARCH.md) — Live-gemd perf probe — search service hot paths
- [`PERSISTER_SHRINK_S2_DETAIL.md`](PERSISTER_SHRINK_S2_DETAIL.md) — PERSISTER_SHRINK_S2_DETAIL.md — promote the abstract persister chain to @Component beans
- [`PERSISTER_SHRINK_S4_PROGRESS.md`](PERSISTER_SHRINK_S4_PROGRESS.md) — PERSISTER_SHRINK_S4_PROGRESS.md — recce for retiring `Persister` / `PersisterHelper` / `PersisterHelperImpl`
- [`PHASE3_TEST_TRIAGE.md`](PHASE3_TEST_TRIAGE.md) — Phase 3 — `@Ignore` Test Audit
- [`PHASE3_TEST_TRIAGE_FAILURES.md`](PHASE3_TEST_TRIAGE_FAILURES.md) — Phase 3 Test Triage — Pre-existing Failure Batch (2026-05-18)
- [`PHASE_3_VISION.md`](PHASE_3_VISION.md) — Phase 3 — vision
- [`RELEASING.md`](RELEASING.md) — Releasing Gemma
- [`RENOVATIONS.md`](RENOVATIONS.md) — Renovations
- [`SEARCH_INDEX_OPERATIONS.md`](SEARCH_INDEX_OPERATIONS.md) — Search Index Operations — Hibernate Search 7 / Lucene 9
- [`SINGLE_CELL_FILTERING_INVENTORY.md`](SINGLE_CELL_FILTERING_INVENTORY.md) — Single-cell filtering inventory
- [`SPOTBUGS_FIRST_PASS.md`](SPOTBUGS_FIRST_PASS.md) — SpotBugs first-pass findings (2026-05-21)
- [`SPRING_BOOT_3_FEASIBILITY.md`](SPRING_BOOT_3_FEASIBILITY.md) — Spring Boot 3 — Feasibility Recce
- [`SPRING_BOOT_BOM_ADOPTION.md`](SPRING_BOOT_BOM_ADOPTION.md) — Spring Boot Dependencies BOM Adoption (Phase 3)
- [`SPRING_SECURITY_7_READINESS.md`](SPRING_SECURITY_7_READINESS.md) — Spring Security 7 Readiness Recce
- [`SQL_INJECTION_HIBERNATE_TRIAGE.md`](SQL_INJECTION_HIBERNATE_TRIAGE.md) — SQL_INJECTION_HIBERNATE — pre-2.0 triage (2026-05-21)
- [`STATIC_ANALYSIS_SWEEP.md`](STATIC_ANALYSIS_SWEEP.md) — Static analysis sweep (2026-05-20)
- [`SWEEP_CONCURRENCY_ANTIPATTERNS.md`](SWEEP_CONCURRENCY_ANTIPATTERNS.md) — Concurrency anti-pattern sweep (post-15006ca9c0)
- [`SWEEP_SYNCHRONIZED_COLLECTION_ITERATION.md`](SWEEP_SYNCHRONIZED_COLLECTION_ITERATION.md) — Synchronized-collection iteration sweep (post 15006ca9c0)
- [`TEST_FIXTURE_FACTORIES.md`](TEST_FIXTURE_FACTORIES.md) — Test fixture factories
- [`WORKFLOW_AND_COMPUTE_ARCHITECTURE.md`](WORKFLOW_AND_COMPUTE_ARCHITECTURE.md) — Workflow management + distributed compute — unified architecture
- [`notable_cases.md`](notable_cases.md) — Spring-MVC legacy cruft — SimpleFormController shim (2026-05-19)
