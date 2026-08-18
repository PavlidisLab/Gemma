# docs/ index

Working design docs, recces, audits, plans, and session notes for Gemma, sorted into subfolders (2026-07-05 cleanup). `README.md` and `CLAUDE.md` stay at the repo root.
Total: 151 docs.


## Pipeline / compute / job-management (Nextflow) — `docs/pipeline-compute/` (1)

**Start here:** the consolidated developer handoff below. The 4 source recces it was built from are archived in `docs/pipeline-compute/archive/`.

- [`PIPELINE_COMPUTE_AND_JOB_MANAGEMENT.md`](pipeline-compute/PIPELINE_COMPUTE_AND_JOB_MANAGEMENT.md) — Pipeline compute & job management — developer handoff

  <sub>archived sources (`pipeline-compute/archive/`): EXTERNAL_PIPELINE_HANDOFF_RECCE.md, PIPELINES_AND_SCHEDULER_RECCE.md, PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md, WORKFLOW_AND_COMPUTE_ARCHITECTURE.md</sub>

## Recces — `docs/recce/` (20 live, 16 archived)

16 recces whose subject shipped, whose premise is gone, or that a live doc supersedes were moved to [`docs/recce/archive/`](recce/archive/README.md) on 2026-08-11 — that README lists each one with the evidence checked.

- [`ACTUATOR_RECCE.md`](recce/ACTUATOR_RECCE.md) — Actuator-style observability endpoints — Phase 3 recce
- [`API_KEY_AUTH_RECCE.md`](recce/API_KEY_AUTH_RECCE.md) — API-key admission + layered auth (TLS/TOTP/keys) for `/rest/v2` once off the VPN
- [`AUDIT_AS_WORKFLOW_RECCE.md`](recce/AUDIT_AS_WORKFLOW_RECCE.md) — Audit-system as workflow / ticket tracker — Phase 3 recce
- [`AUDIT_PHASE_C_RECCE.md`](recce/AUDIT_PHASE_C_RECCE.md) — AUDIT_PHASE_C_RECCE.md
- [`COEXPRESSION_ORPHAN_RECCE.md`](recce/COEXPRESSION_ORPHAN_RECCE.md) — Coexpression-tables orphan recce
- [`CURATION_PROVENANCE_RECCE.md`](recce/CURATION_PROVENANCE_RECCE.md) — what Gemma can answer today about where an annotation came from, and what it would take
- [`DEA_FINDBYGENE_COLDCACHE_RECCE.md`](recce/DEA_FINDBYGENE_COLDCACHE_RECCE.md) — DEA findByGene cold-cache — fix recce
- [`GEMMA_WEB_ONLY_AUDIT.md`](recce/GEMMA_WEB_ONLY_AUDIT.md) — what `@GemmaWebOnly` is really hiding: all 79 sites bucketed, and why a bulk removal is a security question
- [`HEATMAP_REWRITE_RECCE.md`](recce/HEATMAP_REWRITE_RECCE.md) — Heatmap data generation — client-side rewrite recce
- [`LOAD_DETAILS_COLD_PATH_RECCE.md`](recce/LOAD_DETAILS_COLD_PATH_RECCE.md) — `loadDetailsValueObjectsByIds` cold-path anatomy
- [`LOGGING_MODERNIZATION_RECCE.md`](recce/LOGGING_MODERNIZATION_RECCE.md) — Structured logging + OpenTelemetry — Phase 3 recce
- [`LUCENE_HS_READINESS_RECCE.md`](recce/LUCENE_HS_READINESS_RECCE.md) — Lucene 10 + Hibernate Search 8 readiness recce
- [`MOCK_FIXTURE_CONVERSION_RECCE.md`](recce/MOCK_FIXTURE_CONVERSION_RECCE.md) — Mock fixture conversion recce — top-N tests to convert
- [`OBJECT_STORAGE_RECCE.md`](recce/OBJECT_STORAGE_RECCE.md) — Object-Storage Abstraction — Reconnaissance
- [`OPENTELEMETRY_RECCE.md`](recce/OPENTELEMETRY_RECCE.md) — OpenTelemetry tracing — Phase 3 detailed recce
- [`PERSISTER_SHRINK_RECCE.md`](recce/PERSISTER_SHRINK_RECCE.md) — PERSISTER_SHRINK_RECCE.md — shortest path to retiring `persisterHelper` post-Caches
- [`SAMPLES_DESIGN_PERF_RECCE.md`](recce/SAMPLES_DESIGN_PERF_RECCE.md) — `/datasets/{id}/samples` and `/datasets/{id}/design` perf recce
- [`SPRING_MODULITH_RECCE.md`](recce/SPRING_MODULITH_RECCE.md) — Spring Modulith — Phase 3 reconnaissance
- [`WORKFLOW_GROUPS_RECCE.md`](recce/WORKFLOW_GROUPS_RECCE.md) — Workflow Groups CRUD + `/datasets/{id}/groups` — Reconnaissance
- [`WORKFLOW_STATE_TRACKS_RECCE.md`](recce/WORKFLOW_STATE_TRACKS_RECCE.md) — Workflow state: the curation/analysis fork — proposal

## Audits — `docs/audit/` (30)

- [`ASPECTJ_DEEPER_AUDIT.md`](audit/ASPECTJ_DEEPER_AUDIT.md) — AspectJ Deeper Audit (Phase 3, post-`ASPECTJ_EHCACHE_AUDIT`)
- [`ASPECTJ_EHCACHE_AUDIT.md`](audit/ASPECTJ_EHCACHE_AUDIT.md) — AspectJ + ehcache + JCache audit (Phase 3 infra)
- [`AUDIT_ADVICE_RETIREMENT_PLAN.md`](audit/AUDIT_ADVICE_RETIREMENT_PLAN.md) — AuditAdvice retirement plan
- [`AUDIT_PHASE_C_HELPER_BUCKET_DISPOSITION.md`](audit/AUDIT_PHASE_C_HELPER_BUCKET_DISPOSITION.md) — Audit migration Phase C — helper-bucket disposition
- [`AUDIT_SYSTEM_AUDIT.md`](audit/AUDIT_SYSTEM_AUDIT.md) — AUDIT_SYSTEM_AUDIT.md
- [`BASECODE_DEP_AUDIT.md`](audit/BASECODE_DEP_AUDIT.md) — baseCode dep audit — post-ontology pull-in
- [`CACHEABLE_AUDIT.md`](audit/CACHEABLE_AUDIT.md) — `@Cacheable` Annotation Audit — Phase 3
- [`CI_CD_AUDIT.md`](audit/CI_CD_AUDIT.md) — CI/CD Pipeline Audit
- [`CONFIG_AUDIT.md`](audit/CONFIG_AUDIT.md) — Config Audit — Spring Profiles, `@Value`, Property Files
- [`DEPENDENCY_AUDIT.md`](audit/DEPENDENCY_AUDIT.md) — Dependency vulnerability recce — post Boot-3.5.6 bump
- [`DWR_REST_GAP_AUDIT.md`](audit/DWR_REST_GAP_AUDIT.md) — DWR → REST parity gap audit
- [`GEMMA_CLI_DEAD_CODE_AUDIT.md`](audit/GEMMA_CLI_DEAD_CODE_AUDIT.md) — Gemma CLI dead-code audit
- [`HIBERNATE6_CASCADE_AUDIT.md`](audit/HIBERNATE6_CASCADE_AUDIT.md) — Hibernate 6 cascade-strictness audit
- [`HIBERNATE_ENVERS_AUDIT.md`](audit/HIBERNATE_ENVERS_AUDIT.md) — Hibernate Envers Audit
- [`HIBERNATE_L2_CACHE_AUDIT.md`](audit/HIBERNATE_L2_CACHE_AUDIT.md) — Hibernate L2 cache region audit + tuning recommendations
- [`HIBERNATE_TYPE_AUDIT.md`](audit/HIBERNATE_TYPE_AUDIT.md) — Hibernate 6 `@Type` / UserType audit
- [`HIKARICP_AUDIT.md`](audit/HIKARICP_AUDIT.md) — HikariCP audit + modernization
- [`HQL_SQL_AUDIT.md`](audit/HQL_SQL_AUDIT.md) — HQL / native SQL audit — gemma-core
- [`LISTENABLEFUTURE_AUDIT.md`](audit/LISTENABLEFUTURE_AUDIT.md) — ListenableFuture Audit (Phase 3 Spring 6+ Modernization)
- [`LOGGING_AUDIT.md`](audit/LOGGING_AUDIT.md) — Logging stack audit — Phase 3 Spring 6+ infrastructure modernization
- [`LOMBOK_AUDIT.md`](audit/LOMBOK_AUDIT.md) — Lombok Audit — Phase 3 Spring 6+ Modernization Recce
- [`MAVEN_RELEASE_AUDIT.md`](audit/MAVEN_RELEASE_AUDIT.md) — Maven Release / Version-Management Audit
- [`MIDDLE_TIER_AUDIT.md`](audit/MIDDLE_TIER_AUDIT.md) — Middle-tier audit — gemma-cli, gemma-rest, gemma-web
- [`OPENAPI_AUDIT.md`](audit/OPENAPI_AUDIT.md) — OpenAPI / Swagger Integration Audit (Phase 3)
- [`PIPELINESTATUS_WIRE_AUDIT.md`](audit/PIPELINESTATUS_WIRE_AUDIT.md) — Pipeline Status wire-shape audit — UI vs gemma-rest
- [`RESTTEMPLATE_AUDIT.md`](audit/RESTTEMPLATE_AUDIT.md) — RestTemplate audit (Phase 3 Spring 6+ modernization)
- [`SERVLET6_AUDIT.md`](audit/SERVLET6_AUDIT.md) — Servlet 6 / Jakarta Servlet API Compliance Audit
- [`SPRING_PROFILES_AUDIT.md`](audit/SPRING_PROFILES_AUDIT.md) — Spring Profiles Audit (Phase 3 — Spring 6 modernization)
- [`STATIC_ANALYSIS_AUDIT.md`](audit/STATIC_ANALYSIS_AUDIT.md) — Static Analysis Audit — Phase 3 Spring 6+ Infrastructure
- [`VALIDATION_AUDIT.md`](audit/VALIDATION_AUDIT.md) — Jakarta Validation Audit (Bean Validation 3.0 / JSR-380)

## Plans & roadmaps — `docs/plans/` (17)

- [`AFTER_INVOCATION_PHASE_C_PLAN.md`](plans/AFTER_INVOCATION_PHASE_C_PLAN.md) — AfterInvocation Phase C: @EnableMethodSecurity migration plan
- [`BRANCH_MERGE_PLAN.md`](plans/BRANCH_MERGE_PLAN.md) — Phase 3 Branch Merge Plan
- [`CURSOR_PAGINATION_STEP1_PLAN.md`](plans/CURSOR_PAGINATION_STEP1_PLAN.md) — Cursor Pagination — Step 1 Plan + Status
- [`EE_SERVICE_DECOMPOSITION_ROADMAP.md`](plans/EE_SERVICE_DECOMPOSITION_ROADMAP.md) — `ExpressionExperimentService` Decomposition Roadmap
- [`EXPERIMENT_DELETION_REMEDIATION_PLAN.md`](plans/EXPERIMENT_DELETION_REMEDIATION_PLAN.md) — Experiment deletion under dual-version operation — incident summary + remediation plan (2026-08-04)
- [`EXPRESSIONPERSISTER_MIGRATION_PLAN.md`](plans/EXPRESSIONPERSISTER_MIGRATION_PLAN.md) — ExpressionPersister retirement: migration plan (risk-5 recce)
- [`GEMMA_REST_STANDALONE_ROADMAP.md`](plans/GEMMA_REST_STANDALONE_ROADMAP.md) — gemma-rest standalone packaging — roadmap
- [`GEMMA_WEB_RETIREMENT_PLAN.md`](plans/GEMMA_WEB_RETIREMENT_PLAN.md) — gemma-web retirement plan
- [`GENOMEPERSISTER_MIGRATION_PLAN.md`](plans/GENOMEPERSISTER_MIGRATION_PLAN.md) — GenomePersister retirement: migration plan (risk-5 recce)
- [`GSEC_ABSORPTION_ROADMAP.md`](plans/GSEC_ABSORPTION_ROADMAP.md) — gsec absorption roadmap
- [`JUNIT5_MIGRATION_ROADMAP.md`](plans/JUNIT5_MIGRATION_ROADMAP.md) — JUnit 5 (Jupiter) Migration Roadmap
- [`PERSISTER_DELETION_PLAN.md`](plans/PERSISTER_DELETION_PLAN.md) — PersisterHelper retirement: deletion + dispatch-facade plan
- [`PERSISTER_REPLACEMENT_ROADMAP.md`](plans/PERSISTER_REPLACEMENT_ROADMAP.md) — persisterHelper retirement: replacement roadmap
- [`SPRING6_DEPRECATION_ROADMAP.md`](plans/SPRING6_DEPRECATION_ROADMAP.md) — Spring 6 / Spring Security 6 / Hibernate 6 deprecation roadmap
- [`WORKTREE_CLEANUP_PLAN.md`](plans/WORKTREE_CLEANUP_PLAN.md) — Worktree cleanup plan
- [`WORKTREE_CLEANUP_PLAN_v2.md`](plans/WORKTREE_CLEANUP_PLAN_v2.md) — Worktree cleanup plan — v2
- [`WORKTREE_CLEANUP_PLAN_v3.md`](plans/WORKTREE_CLEANUP_PLAN_v3.md) — Worktree cleanup plan v3

## Other design docs — `docs/design/` (60)

- [`ACL_ENTRY_VOTER_MIGRATION.md`](design/ACL_ENTRY_VOTER_MIGRATION.md) — ACL AclEntryVoter family migration plan
- [`AFTER_INVOCATION_MIGRATION.md`](design/AFTER_INVOCATION_MIGRATION.md) — AfterInvocation → Spring Security 6 Modern API Migration Roadmap
- [`ASPECTJ_INVARIANT_CHECKLIST.md`](design/ASPECTJ_INVARIANT_CHECKLIST.md) — AspectJ JDK-Proxy Invariant: Carry-Forward Checklist for `@Configuration` Migration
- [`CELLOSAURUS_CELL_LINE_SEARCH.md`](design/CELLOSAURUS_CELL_LINE_SEARCH.md) — Cellosaurus as a lexical cell-line name-resolution source (backup for CLO's coverage gaps)
- [`CONTAINER_CONFIG.md`](design/CONTAINER_CONFIG.md) — Container Config — Env-Var-Only Gemma Configuration
- [`CRUFT_INVENTORY.md`](design/CRUFT_INVENTORY.md) — Phase 3 cruft inventory + LoC endstate projection
- [`GENOTYPE_DISEASE_MODEL_EXPANSION.md`](design/GENOTYPE_DISEASE_MODEL_EXPANSION.md) — deriving what a genotype models; spec for the unbuilt CORPUS + EXTERNAL relation bases (PR #1685's mechanism is superseded)
- [`CURATION_COMMIT_SPEC.md`](design/CURATION_COMMIT_SPEC.md) — Curation commit — composite all-or-none write (`PUT /datasets/{id}/curation`)
- [`CURATION_DETAILS_RETIREMENT.md`](design/CURATION_DETAILS_RETIREMENT.md) — CurationDetails write-path retirement — phase 1
- [`EXECUTOR_VIRTUAL_THREAD_PREP.md`](design/EXECUTOR_VIRTUAL_THREAD_PREP.md) — Executor centralization audit + virtual-thread prep
- [`EXPRESSION_PERSISTER_DELETION.md`](design/EXPRESSION_PERSISTER_DELETION.md) — ExpressionPersister Final Deletion Tracker
- [`EXTERNAL_URL_REACHABILITY.md`](design/EXTERNAL_URL_REACHABILITY.md) — External URL reachability probe
- [`FAILSAFE_RESIDUAL_TRIAGE.md`](design/FAILSAFE_RESIDUAL_TRIAGE.md) — Failsafe Residual Triage
- [`FLYWAY_PROD_FOLLOWUP.md`](design/FLYWAY_PROD_FOLLOWUP.md) — Flyway production wiring — follow-on session
- [`FRAMEWORK_BUMP_FEASIBILITY.md`](design/FRAMEWORK_BUMP_FEASIBILITY.md) — Phase 3 framework bump — feasibility recce
- [`GEMMA_CLI_MODERNIZATION.md`](design/GEMMA_CLI_MODERNIZATION.md) — gemma-cli runner modernization recce
- [`GEMMA_CURATION_CALL_SURFACE.md`](design/GEMMA_CURATION_CALL_SURFACE.md) — Gemma curation-flow call-surface map
- [`GEMMA_CURATION_FEATURE_WISHLIST.md`](design/GEMMA_CURATION_FEATURE_WISHLIST.md) — Gemma curation-agents — feature wishlist
- [`GEMMA_CURATION_UI_CONTRACT.md`](design/GEMMA_CURATION_UI_CONTRACT.md) — gemma-curation-ui REST API contract audit
- [`GEMMA_REST_BOOTSTRAP_PHASE1.md`](design/GEMMA_REST_BOOTSTRAP_PHASE1.md) — gemma-rest standalone bootstrap — Phase 1 landed
- [`GEMMA_UI_ENDPOINT_GAP.md`](design/GEMMA_UI_ENDPOINT_GAP.md) — gemma-ui ↔ gemma-rest endpoint gap analysis
- [`GEMMA_UI_FEATURE_CATALOG.md`](design/GEMMA_UI_FEATURE_CATALOG.md) — Gemma UI feature catalog — what's needed to fully replace gemma-web
- [`GENOTYPE_DISEASE_MODEL_EXPANSION.md`](design/GENOTYPE_DISEASE_MODEL_EXPANSION.md) — inferring the disease a genotype models, so the disease selector reaches studies annotated only with the model
- [`GSEC_HQL_DEPRECATION.md`](design/GSEC_HQL_DEPRECATION.md) — gsec ACL HQL Deprecation — Inventory and Migration Playbook
- [`GSEC_HQL_DEPRECATION_CONTINUED.md`](design/GSEC_HQL_DEPRECATION_CONTINUED.md) — Phase 3 — gsec HQL deprecation (continued)
- [`GSEC_VERSION_ALIGNMENT.md`](design/GSEC_VERSION_ALIGNMENT.md) — gsec version alignment — feasibility recce
- [`JAVA21_PHASE1_RESULT.md`](design/JAVA21_PHASE1_RESULT.md) — Java 21 Phase 1 — Pre-bump audit (Lombok / AspectJ / JaCoCo)
- [`JAVA21_READINESS.md`](design/JAVA21_READINESS.md) — Java 21 Readiness Recce
- [`JUNIT5_BASETEST_MIGRATION.md`](design/JUNIT5_BASETEST_MIGRATION.md) — JUnit 5 Migration — Phase B0: BaseTest hierarchy (parallel base classes)
- [`JUNIT5_PHASE_A_RESULT.md`](design/JUNIT5_PHASE_A_RESULT.md) — JUnit 5 (Jupiter) Migration — Phase A result
- [`MAVEN_MODERNIZATION.md`](design/MAVEN_MODERNIZATION.md) — Maven Plugin + Dependency Modernization
- [`MGI_MOUSE_STRAIN_GENOTYPE.md`](design/MGI_MOUSE_STRAIN_GENOTYPE.md) — MGI as a mouse-strain resolution source + rule-aware allele→gene disambiguation for complicated genotypes
- [`MOCKITO_MODERNIZATION.md`](design/MOCKITO_MODERNIZATION.md) — Mockito Modernization Recce (Phase 3)
- [`ONTOLOGY_SUBSYSTEM.md`](design/ONTOLOGY_SUBSYSTEM.md) — manifest of the ontology subsystem: what is loaded, what the resolver does step by step, and every switch
- [`ONTOLOGY_SUPPLEMENTARY_METHODS.md`](design/ONTOLOGY_SUPPLEMENTARY_METHODS.md) — the same subsystem as paper Methods prose, for the Gemma 2.0 manuscript
- [`PERF_PROBE_ANNOTATIONS.md`](design/PERF_PROBE_ANNOTATIONS.md) — Perf probe — Annotations + Characteristic (live gemd, 2026-05-20)
- [`PERF_PROBE_DATA_EXPORTS.md`](design/PERF_PROBE_DATA_EXPORTS.md) — Perf probe — TSV/data-export + DEA-run paths (live gemd)
- [`PERF_PROBE_REPORT.md`](design/PERF_PROBE_REPORT.md) — Live-gemd perf probe (2026-05-20 phase2-acl-migrate)
- [`PERF_PROBE_REPORT_ROUND2.md`](design/PERF_PROBE_REPORT_ROUND2.md) — Live-gemd perf probe — round 2 (post-ACL-EXISTS) (2026-05-20 phase2-acl-migrate)
- [`PERF_PROBE_REPORT_ROUND3.md`](design/PERF_PROBE_REPORT_ROUND3.md) — Live-gemd perf probe round 3 — vectors, matrices, DEA results, visualization
- [`PERF_PROBE_REPORT_ROUND4.md`](design/PERF_PROBE_REPORT_ROUND4.md) — Live-gemd perf probe round 4 — single-cell hot paths
- [`PERF_PROBE_SEARCH.md`](design/PERF_PROBE_SEARCH.md) — Live-gemd perf probe — search service hot paths
- [`PERSISTER_SHRINK_S2_DETAIL.md`](design/PERSISTER_SHRINK_S2_DETAIL.md) — PERSISTER_SHRINK_S2_DETAIL.md — promote the abstract persister chain to @Component beans
- [`PERSISTER_SHRINK_S4_PROGRESS.md`](design/PERSISTER_SHRINK_S4_PROGRESS.md) — PERSISTER_SHRINK_S4_PROGRESS.md — recce for retiring `Persister` / `PersisterHelper` / `PersisterHelperImpl`
- [`PHASE3_TEST_TRIAGE.md`](design/PHASE3_TEST_TRIAGE.md) — Phase 3 — `@Ignore` Test Audit
- [`PUBLICATION_LINK_EVIDENCE.md`](design/PUBLICATION_LINK_EVIDENCE.md) — evidence and rejections on the experiment↔publication link (V25); precedence by rank replaces the exclusion file
- [`PHASE3_TEST_TRIAGE_FAILURES.md`](design/PHASE3_TEST_TRIAGE_FAILURES.md) — Phase 3 Test Triage — Pre-existing Failure Batch (2026-05-18)
- [`PHASE_3_VISION.md`](design/PHASE_3_VISION.md) — Phase 3 — vision
- [`RELEASING.md`](design/RELEASING.md) — Releasing Gemma
- [`RENOVATIONS.md`](design/RENOVATIONS.md) — Renovations
- [`SEARCH_INDEX_OPERATIONS.md`](design/SEARCH_INDEX_OPERATIONS.md) — Search Index Operations — Hibernate Search 7 / Lucene 9
- [`SINGLE_CELL_FILTERING_INVENTORY.md`](design/SINGLE_CELL_FILTERING_INVENTORY.md) — Single-cell filtering inventory
- [`SPOTBUGS_FIRST_PASS.md`](design/SPOTBUGS_FIRST_PASS.md) — SpotBugs first-pass findings (2026-05-21)
- [`SPRING_BOOT_3_FEASIBILITY.md`](design/SPRING_BOOT_3_FEASIBILITY.md) — Spring Boot 3 — Feasibility Recce
- [`SPRING_BOOT_BOM_ADOPTION.md`](design/SPRING_BOOT_BOM_ADOPTION.md) — Spring Boot Dependencies BOM Adoption (Phase 3)
- [`SPRING_SECURITY_7_READINESS.md`](design/SPRING_SECURITY_7_READINESS.md) — Spring Security 7 Readiness Recce
- [`SQL_INJECTION_HIBERNATE_TRIAGE.md`](design/SQL_INJECTION_HIBERNATE_TRIAGE.md) — SQL_INJECTION_HIBERNATE — pre-2.0 triage (2026-05-21)
- [`STATIC_ANALYSIS_SWEEP.md`](design/STATIC_ANALYSIS_SWEEP.md) — Static analysis sweep (2026-05-20)
- [`SWEEP_CONCURRENCY_ANTIPATTERNS.md`](design/SWEEP_CONCURRENCY_ANTIPATTERNS.md) — Concurrency anti-pattern sweep (post-15006ca9c0)
- [`SWEEP_SYNCHRONIZED_COLLECTION_ITERATION.md`](design/SWEEP_SYNCHRONIZED_COLLECTION_ITERATION.md) — Synchronized-collection iteration sweep (post 15006ca9c0)
- [`TEST_FIXTURE_FACTORIES.md`](design/TEST_FIXTURE_FACTORIES.md) — Test fixture factories
- [`notable_cases.md`](design/notable_cases.md) — Spring-MVC legacy cruft — SimpleFormController shim (2026-05-19)

## Handoffs — `docs/handoffs/` (3)

- [`CURATION_UI_HANDOFF_INVENTORY.md`](handoffs/CURATION_UI_HANDOFF_INVENTORY.md) — Curation-UI HANDOFF asks vs gemma-rest implementation
- [`PHASE_2_ACL_HANDOFF.md`](handoffs/PHASE_2_ACL_HANDOFF.md) — Phase 2 ACL Migration — Handoff (2026-05-18)
- [`PHASE_2_HANDOFF.md`](handoffs/PHASE_2_HANDOFF.md) — Phase 2 (Spring 6 / Hibernate 6 / jakarta) — handoff

## Session notes & snapshots — `docs/notes/` (3)

- [`NCBI_RATE_LIMIT_NOTE.md`](notes/NCBI_RATE_LIMIT_NOTE.md) — NCBI eutils rate-limit note
- [`SESSION_CLOSE_NOTE_2026-05-19.md`](notes/SESSION_CLOSE_NOTE_2026-05-19.md) — Session close note — 2026-05-19
- [`VALIDATION_OPTIN_NOTES.md`](notes/VALIDATION_OPTIN_NOTES.md) — Jakarta Bean Validation (JSR-380) — opt-in notes
