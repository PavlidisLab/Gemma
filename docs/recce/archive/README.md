# Archived recces

Recces whose subject has shipped, whose premise is gone, or that a later doc
supersedes. Kept for the reasoning, not as a plan — none of these describe
outstanding work. Archived 2026-08-11; the evidence column is what was checked
in the tree at `20e8433db2`.

## Shipped

| Doc | Evidence it landed |
|---|---|
| `AUTH_FOR_SPA_RECCE.md` | `gemma-rest/.../security/BearerTokenAuthenticationFilter.java`; the SPA authenticates by bearer token as recommended |
| `CONTAINER_RECCE.md`, `CONTAINER_IMAGE_RECCE.md` | `Dockerfile` + `docker-compose.yml` at the repo root; GHCR image deployed to frink |
| `CORS_RECCE.md` | CORS handling in `gemma-rest/src/main` |
| `CURSOR_PAGINATION_RECCE.md` | `CursorArg`; cursor tests on `TicketsWebService` |
| `GEMMA_REST_STANDALONE_RECCE.md` | standalone WAR is how Gemma 2.0 ships |
| `GSEC_PHASE_C_RECCE.md` | `GsecAclServiceAdapter` no longer exists |
| `SEARCH_RECCE.md` | `CompositeSearchSource` / `HibernateSearchSource` live; search restoration complete |
| `BASECODE_MATRIX_RECCE.md`, `BASECODE_MATH_LINEARMODELS_RECCE.md` | both subsystems ported in-tree; `baseCode` Maven dep removed at `9f216558d5`, zero `import ubic.basecode.*` remain. Outcome recorded in `docs/audit/BASECODE_DEP_AUDIT.md`. |

## Premise gone

| Doc | Why |
|---|---|
| `GENE_PAGE_REWORK_RECCE.md` | scoped to the gemma-web gene page; gemma-web was deleted in `bb154eee88` |
| `JDK21_FEATURES_RECCE.md` | the repo builds on JDK 25; the adoption question it scopes was overtaken |
| `AGENT_WRITEBACK_RECCE.md` | the writeback surfaces it proposed shipped as `ANNOTATION_SET` (V20/V21) and the ticket layer (V3/V19) |

## Superseded by a live doc

| Doc | Superseded by |
|---|---|
| `PERSISTER_BK_STEP1_RECCE.md`, `PERSISTER_CACHE_LIFT_RECCE.md` | `../PERSISTER_SHRINK_RECCE.md`, which says so in its own header — the `Caches` POJO is gone and the per-step plan with it |
| `AUDIT_MIGRATION_PHASE_C_RECCE.md` | `../AUDIT_PHASE_C_RECCE.md` (the bucket-by-bucket inventory `CLAUDE.md` points at) and `../../audit/AUDIT_ADVICE_RETIREMENT_PLAN.md` |
