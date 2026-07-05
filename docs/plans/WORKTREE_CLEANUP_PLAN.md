# Worktree cleanup plan

Generated 2026-05-18 on branch `phase2-acl-migrate` (HEAD 11c5cc09).

## Scope

The main checkout at `/Users/pzoot/Dev/eclipseworkspace/Gemma` has accumulated **139 git worktrees** (136 with physical directories, 3 admin-only entries with the directory already removed) under `.claude/worktrees/`. Total disk usage: **23 GB**.

Most are merged into `phase2-acl-migrate` and safe to remove. A handful carry unmerged Phase 2 / Phase 3 work, or are still locked (likely live or recently-stopped agents). This document categorizes every entry. The companion script `cleanup_worktrees.sh` only touches the SAFE bucket.

## Summary

| Category | Count | Action |
|---|---|---|
| SAFE | 107 | Removed by `cleanup_worktrees.sh` |
| CAUTION (unmerged branch) | 7 | Leave alone — has commits not in `phase2-acl-migrate` |
| CAUTION (dirty working tree) | 2 | Leave alone — uncommitted changes |
| DEFERRED | 1 | Intentionally unmerged (`worktree-xml-security`) |
| ACTIVE / LOCKED (merged) | 21 | Leave alone — locked, may be live agent |
| SELF | 1 | This agent's own worktree — cannot self-delete |
| Admin-only (no physical dir) | 3 | Handled by `git worktree prune` (in script) |

Total: 139 + 3 admin-only orphans = 142 admin entries, 136 physical dirs.

## Section 1: Inventory

### SAFE — 107 entries

Branch is merged into `phase2-acl-migrate`, working tree clean, no lock file. The script removes these.

| Worktree dir | Branch |
|---|---|
| `agent-aclentryvoter-recce` | `worktree-aclentryvoter-recce` |
| `agent-aclvoter-x1-wrappers` | `worktree-aclvoter-x1-wrappers` |
| `agent-actuator-impl` | `worktree-actuator-impl` |
| `agent-actuator-recce` | `worktree-actuator-recce` |
| `agent-afterinv-phase-a` | `worktree-afterinv-phase-a` |
| `agent-afterinv-phase-b-cs-dv` | `worktree-afterinv-phase-b-cs-dv` |
| `agent-afterinv-phase-b-quiet` | `worktree-afterinv-phase-b-quiet` |
| `agent-afterinv-phase-b-vo` | `worktree-afterinv-phase-b-vo` |
| `agent-afterinv-phase-c-prep` | `worktree-afterinv-phase-c-prep` |
| `agent-afterinvocation-recce` | `worktree-afterinvocation-recce` |
| `agent-annotations-writeback` | `worktree-annotations-writeback` |
| `agent-aspectj-deeper` | `worktree-aspectj-deeper` |
| `agent-aspectj-ehcache-audit` | `worktree-aspectj-ehcache-audit` |
| `agent-aspectj-invariant` | `worktree-aspectj-invariant` |
| `agent-basejersey-cleanup` | `worktree-basejersey-cleanup` |
| `agent-bk-consolidation` | `worktree-bk-consolidation` |
| `agent-cacheable-audit` | `worktree-cacheable-audit` |
| `agent-commonslog-to-slf4j` | `worktree-commonslog-to-slf4j` |
| `agent-container-recce` | `worktree-container-recce` |
| `agent-curation-ui-contract` | `worktree-curation-ui-contract` |
| `agent-ee-proxy-fix` | `worktree-ee-proxy-fix` |
| `agent-ee-svc-decomp-p1` | `worktree-ee-svc-decomp-p1` |
| `agent-ee-svc-decomp-p15` | `worktree-ee-svc-decomp-p15` |
| `agent-ee-svc-decomp-p2` | `worktree-ee-svc-decomp-p2` |
| `agent-ee-svc-decomp-recce` | `worktree-ee-svc-decomp-recce` |
| `agent-ehcache-cachemanager-fix` | `worktree-ehcache-cachemanager-fix` |
| `agent-executor-virtual-prep` | `worktree-executor-virtual-prep` |
| `agent-executor-vt-callers` | `worktree-executor-vt-callers` |
| `agent-executor-vt-callers-2` | `worktree-executor-vt-callers-2` |
| `agent-expression-chunk-e1` | `worktree-expression-chunk-e1` |
| `agent-expression-chunk-e3` | `worktree-expression-chunk-e3` |
| `agent-expression-chunk-e4` | `worktree-expression-chunk-e4` |
| `agent-expression-chunk-e5` | `worktree-expression-chunk-e5` |
| `agent-expressionpersister-recce` | `worktree-expressionpersister-recce` |
| `agent-fixture-bioassay` | `worktree-fixture-bioassay` |
| `agent-fixture-factories-2` | `worktree-fixture-factories-2` |
| `agent-fixture-gene-cs` | `worktree-fixture-gene-cs` |
| `agent-framework-bump-recce` | `worktree-framework-bump-recce` |
| `agent-gemma-cli-modernize` | `worktree-gemma-cli-modernize` |
| `agent-gemma-rest-bootstrap` | `worktree-gemma-rest-bootstrap` |
| `agent-gemma-rest-standalone-recce` | `worktree-gemma-rest-standalone-recce` |
| `agent-gemma-web-retire` | `worktree-gemma-web-retire` |
| `agent-genome-chunk-51` | `worktree-genome-chunk-51` |
| `agent-genome-chunk-52` | `worktree-genome-chunk-52` |
| `agent-genome-chunk-53-prep` | `worktree-genome-chunk-53-prep` |
| `agent-genome-chunk-53-taxonfix` | `worktree-genome-chunk-53-taxonfix` |
| `agent-genome-chunk-54-cutover` | `worktree-genome-chunk-54-cutover` |
| `agent-genome-chunk-54-retry` | `worktree-genome-chunk-54-retry` |
| `agent-gsec-bump-exec` | `worktree-gsec-bump-exec` |
| `agent-gsec-hql-continued` | `worktree-gsec-hql-continued` |
| `agent-gsec-hql-v2` | `worktree-gsec-hql-v2` |
| `agent-gsec-version-align` | `worktree-gsec-version-align` |
| `agent-hibernate-envers-audit` | `worktree-hibernate-envers-audit` |
| `agent-hibernate-l2-tune` | `worktree-hibernate-l2-tune` |
| `agent-hibernate-type-audit` | `worktree-hibernate-type-audit` |
| `agent-hikari-modernize` | `worktree-hikari-modernize` |
| `agent-ignore-audit-v2` | `worktree-ignore-audit-v2` |
| `agent-impl-autowire-rule` | `worktree-impl-autowire-rule` |
| `agent-java21-phase1` | `worktree-java21-phase1` |
| `agent-java21-readiness` | `worktree-java21-readiness` |
| `agent-jsr305-cleanup` | `worktree-jsr305-cleanup` |
| `agent-jstl-jakarta` | `worktree-jstl-jakarta` |
| `agent-junit5-phase-a` | `worktree-junit5-phase-a` |
| `agent-junit5-phase-b0` | `worktree-junit5-phase-b0` |
| `agent-junit5-recce` | `worktree-junit5-recce` |
| `agent-l2-cache-bound` | `worktree-l2-cache-bound` |
| `agent-listenablefuture` | `worktree-listenablefuture` |
| `agent-logging-modernize` | `worktree-logging-modernize` |
| `agent-lombok-audit` | `worktree-lombok-audit` |
| `agent-lombok-cleanup` | `worktree-lombok-cleanup` |
| `agent-maven-modernize` | `worktree-maven-modernize` |
| `agent-maven-release-recce` | `worktree-maven-release-recce` |
| `agent-metrics-jcache-restore` | `worktree-metrics-jcache-restore` |
| `agent-mockito-modernize` | `worktree-mockito-modernize` |
| `agent-openapi-audit` | `worktree-openapi-audit` |
| `agent-persister-delete-plan` | `worktree-persister-delete-plan` |
| `agent-persister-genome` | `worktree-persister-genome` |
| `agent-persister-recce` | `worktree-persister-recce` |
| `agent-persister-step2` | `worktree-persister-step2` |
| `agent-persister-step3-ad` | `worktree-persister-step3-ad` |
| `agent-profile-cleanup` | `worktree-profile-cleanup` |
| `agent-querycache-shard` | `worktree-querycache-shard` |
| `agent-relationshippersister` | `worktree-relationshippersister` |
| `agent-release-small-fixes` | `worktree-release-small-fixes` |
| `agent-rest-security-config` | `worktree-rest-security-config` |
| `agent-restclient-migrate` | `worktree-restclient-migrate` |
| `agent-resttemplate-audit` | `worktree-resttemplate-audit` |
| `agent-secured-prauthorize` | `worktree-secured-prauthorize` |
| `agent-servlet6-audit` | `worktree-servlet6-audit` |
| `agent-session-getreference` | `worktree-session-getreference` |
| `agent-session-refresh-v2` | `worktree-session-refresh-v2` |
| `agent-slf4j-bump` | `worktree-slf4j-bump` |
| `agent-spring-boot-3-recce` | `worktree-spring-boot-3-recce` |
| `agent-spring-boot-bom` | `worktree-spring-boot-bom` |
| `agent-spring-profiles-audit` | `worktree-spring-profiles-audit` |
| `agent-spring-security-7-recce` | `worktree-spring-security-7-recce` |
| `agent-spring6-deprecation-hunt` | `worktree-spring6-deprecation-hunt` |
| `agent-static-analysis-audit` | `worktree-static-analysis-audit` |
| `agent-test-failure-triage` | `worktree-test-failure-triage` |
| `agent-validation-audit` | `worktree-validation-audit` |
| `agent-validation-optin` | `worktree-validation-optin` |
| `agent-xml-config-kickoff` | `worktree-xml-config-kickoff` |
| `agent-xml-datasource` | `worktree-xml-datasource` |
| `agent-xml-gemma-cli` | `worktree-xml-gemma-cli` |
| `agent-xml-gemma-rest` | `worktree-xml-gemma-rest` |
| `agent-xml-hibernate` | `worktree-xml-hibernate` |
| `agent-xml-schedule` | `worktree-xml-schedule` |

### CAUTION — unmerged work (7 entries)

These branches have commits not in `phase2-acl-migrate`. Review individually before removing. Some look like Phase 2 step-7 work that may still be wanted; others (`worktree-agent-a6a984701e76aa60a`) look like upstream-development cherry-picks that should probably be rebased or dropped.

### `worktree-agent-a4cb317565e5b7768`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-a4cb317565e5b7768`
- Locked: True
- Unmerged commits (1, first 5):
  - `cc635b573c Phase 2 Step 7: normalize Date subtypes from JPA Metamodel — fixes REST OpenAPI`

### `worktree-agent-a54b95715b2699369`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-a54b95715b2699369`
- Locked: True
- Unmerged commits (3, first 5):
  - `af5687de9b PHASE_2_HANDOFF.md: document the multi-context schema-drop fix`
  - `9b25eaabab Phase 2 Step 7: scope the multi-context guards to the shared MySQL test DB`
  - `99a16f5fc4 Phase 2 Step 7: JVM-static one-shot guards for multi-context test bootstrap`

### `worktree-agent-a6a984701e76aa60a`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-a6a984701e76aa60a`
- Locked: True
- Unmerged commits (95, first 5):
  - `936bc4ce27 Remove usage of var in OntologySearchSource.java`
  - `2b78b661a9 Inherit version for HikariCP from pavlab-starter-parent`
  - `3e290c50b1 Revert "Update HDF5 to 1.12.3"`
  - `dab694df04 Update HDF5 to 1.12.3`
  - `611ad6a9df Rename group ID to ca.ubc.msl.pavlab`

### `worktree-agent-a7e3331f00d073b2f`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-a7e3331f00d073b2f`
- Locked: True
- Unmerged commits (2, first 5):
  - `b0751f8329 Phase 2: JPA-Criteria port supports subquery + .size filters`
  - `14879676a2 Phase 2: honour Sort.NullMode FIRST/LAST via Hibernate 6 JpaOrder`

### `worktree-agent-aaf428afcceabcb01`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aaf428afcceabcb01`
- Locked: True
- Unmerged commits (1, first 5):
  - `a5a2ce3c0d Phase 2 Task C: legacy SHA + username-salt password-hash migration`

### `phase2-worktree`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ab43c1385f4eb9331`
- Locked: True
- Unmerged commits (1, first 5):
  - `ba13de33b2 Phase 2 Step 7: clear EntityNotFoundException cluster on detached-EE merge`

### `worktree-agent-ada13ef134bc59047`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ada13ef134bc59047`
- Locked: True
- Unmerged commits (1, first 5):
  - `1956485493 Phase 2 Step 7: port ExpressionAnalysisResultSetDaoImpl.findByBioAssaySetInAndDatabaseEntryInLimit to JPA Criteria`


### CAUTION — dirty working tree (2 entries)

Branch is merged but the working tree has uncommitted changes. Inspect, stash or commit, then re-run the plan.

### `worktree-branch-merge-plan`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan`
- Working-tree dirty. `git status --porcelain`:
```
A  FRAMEWORK_BUMP_FEASIBILITY.md
UU pom.xml
?? .claude/worktrees/
```

### `worktree-expression-chunk-e2`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e2`
- Working-tree dirty. `git status --porcelain`:
```
?? .claude/worktrees/
```


### DEFERRED (1 entry)

Intentionally unmerged. Do not remove without a deliberate decision.

### `worktree-xml-security`
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-security`
- Intentionally unmerged per Phase 3 progress memory (xml-security migration deferred).

### ACTIVE / LOCKED (21 entries)

Branch is merged into `phase2-acl-migrate` BUT the worktree carries a `.git/worktrees/<name>/locked` file. The lock typically means a Claude Code agent is (or was) running here — removing it would race with that agent. The script skips these entirely.

When you're sure all the corresponding agents are dead, the lock file can be deleted by hand (`rm .git/worktrees/<name>/locked`) and the entry re-classified to SAFE on the next run of this plan.

| Worktree dir | Branch | Notes |
|---|---|---|
| `agent-a8ec45b544d99440a` | `phase2-acl-leftovers` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a060e73e4353b5592` | `worktree-agent-a060e73e4353b5592` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a1b429df344ee6b9e` | `worktree-agent-a1b429df344ee6b9e` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a2a8f49a3408139e1` | `worktree-agent-a2a8f49a3408139e1` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a3cbf1b955cea3bce` | `worktree-agent-a3cbf1b955cea3bce` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a42565c51ae3794de` | `worktree-agent-a42565c51ae3794de` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a4bed887e0022e00c` | `worktree-agent-a4bed887e0022e00c` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a4de64513d998f6a3` | `worktree-agent-a4de64513d998f6a3` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a65ce15ffc025989c` | `worktree-agent-a65ce15ffc025989c` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a6d773b6f9dcd21bc` | `worktree-agent-a6d773b6f9dcd21bc` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a719cad6c20a655be` | `worktree-agent-a719cad6c20a655be` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a72728457644a589a` | `worktree-agent-a72728457644a589a` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a7f525a3ad7b3f575` | `worktree-agent-a7f525a3ad7b3f575` | locked, branch is merged — leave alone (may be live agent) |
| `agent-a86afb830197b0204` | `worktree-agent-a86afb830197b0204` | locked, branch is merged — leave alone (may be live agent) |
| `agent-aa6def6d546b486d5` | `worktree-agent-aa6def6d546b486d5` | locked, branch is merged — leave alone (may be live agent) |
| `agent-ac75874f216cb7a41` | `worktree-agent-ac75874f216cb7a41` | locked, branch is merged — leave alone (may be live agent) |
| `agent-ae169e81f53b31ebb` | `worktree-agent-ae169e81f53b31ebb` | locked, branch is merged — leave alone (may be live agent) |
| `agent-ae6dcdc2fbd87656e` | `worktree-agent-ae6dcdc2fbd87656e` | locked, branch is merged — leave alone (may be live agent) |
| `agent-ae7fc3f98a9289bf0` | `worktree-agent-ae7fc3f98a9289bf0` | locked, branch is merged — leave alone (may be live agent) |
| `agent-aeb9f848c17af88fe` | `worktree-agent-aeb9f848c17af88fe` | locked, branch is merged — leave alone (may be live agent) |
| `agent-af308b68e4049a0ff` | `worktree-agent-af308b68e4049a0ff` | locked, branch is merged — leave alone (may be live agent) |

### SELF (1 entry)

### `worktree-agent-aff47784173d10afd`  (THIS agent — do NOT remove from inside itself)
- Path: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aff47784173d10afd`

### Admin-only orphans (3 entries)

These have a `.git/worktrees/<name>/` directory but the physical worktree dir was already removed. `git worktree prune` clears them:

- `agent-container-recce` -> branch `worktree-container-recce` (merged)
- `agent-junit5-phase-a` -> branch `worktree-junit5-phase-a` (merged)
- `agent-validation-optin` -> branch `worktree-validation-optin` (merged)

The script runs `git worktree prune` at the end and then deletes the three branches.

## Section 2: Cleanup script

See `cleanup_worktrees.sh` at the repo root.

Key properties:

- Hard-coded SAFE list of `path|branch` pairs (107 entries).
- Per-entry: `git worktree remove --force <path>` then `git branch -D <branch>`.
- Idempotent: skips entries whose path already doesn't exist AND whose branch is already gone.
- Bails on the first true error (`set -euo pipefail`) so a partial run is recoverable.
- Confirmation prompt at the top (`Continue? [y/N]`).
- Final step: `git worktree prune` to clear the 3 admin-only orphans, then deletes their branches.
- **Does NOT touch** any CAUTION / DEFERRED / ACTIVE_LOCKED / SELF entry.

## Section 3: Disk usage

```
$ du -sh /Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/
23G  /Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/
```

107 SAFE entries out of 136 physical dirs, so the script should reclaim roughly **18 GB** (assuming uniform ~170 MB per worktree; mileage will vary — sibling worktrees hardlink some git objects, so the actual savings will be reported by `du` after the run).

## Re-running the plan

If lock files get cleared or unmerged work gets merged, just regenerate this document:

```bash
# from any worktree, in the agent that produced this file
python3 -c 'see WORKTREE_CLEANUP_PLAN.md generator above'
```

(Or, more honestly: re-run this same agent task. The categorization logic lives in the agent prompt, not in a checked-in script.)
