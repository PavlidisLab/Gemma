# Worktree cleanup plan — v2

Regenerated 2026-05-19 on branch `worktree-cleanup-recce` (off `phase2-acl-migrate` @ `c4b88913`).
Supersedes `WORKTREE_CLEANUP_PLAN.md` (which was based on a snapshot of 139 worktrees on 2026-05-18; the count has since ballooned to 226).

## Scope

The main checkout at `/Users/pzoot/Dev/eclipseworkspace/Gemma` has accumulated **227 git worktrees** (226 agent worktrees + 1 main checkout), corresponding to:

- **223** physical directories under `.claude/worktrees/`
- **226** admin entries under `.git/worktrees/` (so 3 admin-only orphans where the physical dir was already removed)
- **111** of those admin entries carry a `locked` file (likely from prior agent runs that didn't clean up)
- **Total disk usage**: `du -sh .claude/worktrees/` = **36 GB**

Most are merged into `phase2-acl-migrate` and safe to remove. A handful carry unmerged Phase 2 / Phase 3 work, or are still locked. This document categorizes every entry and the companion script `cleanup_worktrees.sh` only touches the SAFE bucket.

## Summary

| Category | Count | Action |
|---|---|---|
| SAFE | 110 | Removed by `cleanup_worktrees.sh` |
| LOCKED_MERGED | 94 | Leave alone — locked (likely live or stuck agent). Unlock by hand when sure the agent is dead, then re-run plan. |
| LOCKED_UNMERGED | 17 | Leave alone — locked AND has commits not in `phase2-acl-migrate`. Review individually. |
| CAUTION (dirty working tree) | 1 | Leave alone — uncommitted changes (`agent-branch-merge-plan`). |
| AVOID-listed | 3 | Leave alone — explicit avoid-list (`bc-math-lm`, `junit5-batch11`, `shrink-s2exec`). |
| SELF | 1 | This agent's own worktree (`agent-wt-cleanup`) — cannot self-delete. |
| Admin-only (no physical dir) | 3 | Handled by `git worktree prune` (in script). |

Total: 110 + 94 + 17 + 1 + 3 + 1 = 226 agent worktrees in porcelain output + 1 main = 227.

The 3 admin-only orphans (`agent-container-recce`, `agent-junit5-phase-a`, `agent-validation-optin`) are nested inside two SAFE parent worktrees (`agent-branch-merge-plan` is CAUTION, `agent-expression-chunk-e2` is SAFE). Their branches are merged. `git worktree prune` will clean them up at the end of the script.

## Note on parent-vs-child ordering

Three of the SAFE entries are **nested worktrees** inside other worktrees:

- `agent-branch-merge-plan/.claude/worktrees/agent-container-recce` (parent is CAUTION_DIRTY, so the nested entry will outlive the cleanup pass)
- `agent-branch-merge-plan/.claude/worktrees/agent-validation-optin` (same)
- `agent-expression-chunk-e2/.claude/worktrees/agent-junit5-phase-a` (parent IS in SAFE)

The script must remove the **nested child** before the **parent SAFE entry**, otherwise `git worktree remove` on the parent will delete the nested dir on disk while leaving the admin entry orphaned. The script handles this by sorting the SAFE list lexicographically — child paths sort after parent paths in plain string order, BUT `git worktree remove` of the parent forcibly removes the nested children's working tree too. The safest path is: handle children first, then parents. We add an explicit pre-pass for the three known nested entries.

## Disk reclaim estimate

Sampled 5 SAFE worktrees: 104-209 MB each. With 110 SAFE entries at a ~150 MB average, the script should reclaim **~16-18 GB** out of the 36 GB total. The remaining ~18 GB stays with the LOCKED + CAUTION + AVOID + SELF buckets (123 worktrees).

To free more, the user must:

1. Manually verify no live agent corresponds to each LOCKED worktree.
2. `rm /Users/pzoot/Dev/eclipseworkspace/Gemma/.git/worktrees/<name>/locked` for the dead ones.
3. Re-run this planning agent to re-categorize: the LOCKED_MERGED ones will land in SAFE.

## Section 1: Inventory

### SAFE — 110 entries

Branch is merged into `phase2-acl-migrate`, working tree clean, no lock file, not in avoid-list, not SELF. The script removes these.

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
| `agent-branch-merge-plan/.../agent-container-recce` | `worktree-container-recce` |
| `agent-branch-merge-plan/.../agent-validation-optin` | `worktree-validation-optin` |
| `agent-cacheable-audit` | `worktree-cacheable-audit` |
| `agent-commonslog-to-slf4j` | `worktree-commonslog-to-slf4j` |
| `agent-curation-ui-contract` | `worktree-curation-ui-contract` |
| `agent-cursor-1s` | `cursor-step1s` |
| `agent-ee-proxy-fix` | `worktree-ee-proxy-fix` |
| `agent-ee-svc-decomp-p1` | `worktree-ee-svc-decomp-p1` |
| `agent-ee-svc-decomp-p15` | `worktree-ee-svc-decomp-p15` |
| `agent-ee-svc-decomp-p2` | `worktree-ee-svc-decomp-p2` |
| `agent-ee-svc-decomp-recce` | `worktree-ee-svc-decomp-recce` |
| `agent-ehcache-cachemanager-fix` | `worktree-ehcache-cachemanager-fix` |
| `agent-executor-virtual-prep` | `worktree-executor-virtual-prep` |
| `agent-executor-vt-callers-2` | `worktree-executor-vt-callers-2` |
| `agent-executor-vt-callers` | `worktree-executor-vt-callers` |
| `agent-expression-chunk-e1` | `worktree-expression-chunk-e1` |
| `agent-expression-chunk-e2/.../agent-junit5-phase-a` | `worktree-junit5-phase-a` |
| `agent-expression-chunk-e2` | `worktree-expression-chunk-e2` |
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
| `agent-xml-config-kickoff` | `worktree-xml-config-kickoff` |
| `agent-xml-datasource` | `worktree-xml-datasource` |
| `agent-xml-gemma-cli` | `worktree-xml-gemma-cli` |
| `agent-xml-gemma-rest` | `worktree-xml-gemma-rest` |
| `agent-xml-hibernate` | `worktree-xml-hibernate` |
| `agent-xml-schedule` | `worktree-xml-schedule` |
| `agent-xml-security` | `worktree-xml-security` |

Notable change vs v1: `worktree-xml-security` is now MERGED (formerly DEFERRED). The v1 plan had it carved out; v2 lets it go.

### LOCKED_UNMERGED — 17 entries

Locked AND has commits not in `phase2-acl-migrate`. Review individually before unlocking + removing. Several carry real Phase 2/3 work worth examining for cherry-pick.

Sampled head commits on a few:

- `worktree-agent-a1f3a63d1ab1cf5ee` -> `Phase 3 fix: restore RoleHierarchy bean`
- `worktree-agent-a4cb317565e5b7768` -> `Phase 2 Step 7: normalize Date subtypes from JPA Metamodel`
- `worktree-agent-a70e5589471a816ac` -> `Phase 3 cleanup: delete 5 @Deprecated CLIs (-733 LoC)`
- `worktree-agent-a6a984701e76aa60a` -> 95 commits, looks like upstream-development cherry-picks
- `worktree-agent-aaf428afcceabcb01` -> `Phase 2 Task C: legacy SHA + username-salt password-hash migration`
- `phase2-worktree` -> `Phase 2 Step 7: clear EntityNotFoundException cluster on detached-EE merge`

| Worktree dir | Branch |
|---|---|
| `agent-a1f3a63d1ab1cf5ee` | `worktree-agent-a1f3a63d1ab1cf5ee` |
| `agent-a4cb317565e5b7768` | `worktree-agent-a4cb317565e5b7768` |
| `agent-a54b95715b2699369` | `worktree-agent-a54b95715b2699369` |
| `agent-a6a984701e76aa60a` | `worktree-agent-a6a984701e76aa60a` |
| `agent-a70e5589471a816ac` | `worktree-agent-a70e5589471a816ac` |
| `agent-a7e3331f00d073b2f` | `worktree-agent-a7e3331f00d073b2f` |
| `agent-a86afb830197b0204` | `worktree-agent-a86afb830197b0204` |
| `agent-a876ed0da6fe33d78` | `worktree-agent-a876ed0da6fe33d78` |
| `agent-a8da44b20e16c4248` | `worktree-agent-a8da44b20e16c4248` |
| `agent-aaf428afcceabcb01` | `worktree-agent-aaf428afcceabcb01` |
| `agent-ab35ee697c290ea57` | `worktree-agent-ab35ee697c290ea57` |
| `agent-ab43c1385f4eb9331` | `phase2-worktree` |
| `agent-ac9b43bbef7bb9243` | `worktree-agent-ac9b43bbef7bb9243` |
| `agent-ad9b8643cb07c548d` | `worktree-agent-ad9b8643cb07c548d` |
| `agent-ada13ef134bc59047` | `worktree-agent-ada13ef134bc59047` |
| `agent-af4b6e885b84c3a42` | `worktree-agent-af4b6e885b84c3a42` |
| `agent-afbe5e416b5ef88bf` | `worktree-agent-afbe5e416b5ef88bf` |

### CAUTION — dirty working tree (1 entry)

| Worktree dir | Branch | Notes |
|---|---|---|
| `agent-branch-merge-plan` | `worktree-branch-merge-plan` | dirty: `A FRAMEWORK_BUMP_FEASIBILITY.md`, `UU pom.xml` (unresolved merge conflict). Hosts two SAFE nested child worktrees that the script will still clean up. |

### AVOID-listed (3 entries)

Explicit avoid-list from the task brief. NOT touched even if SAFE-eligible.

| Worktree dir | Branch | Merged | Locked |
|---|---|---|---|
| `agent-bc-math-lm` | `basecode-math-linearmodels-recce` | yes | no |
| `agent-junit5-batch11` | `junit5-batch11` | yes | no |
| `agent-shrink-s2exec` | `persister-shrink-s2exec` | no | no |

### SELF (1 entry)

| Worktree dir | Branch |
|---|---|
| `agent-wt-cleanup` | `worktree-cleanup-recce` |

### LOCKED_MERGED — 94 entries

Branch is merged BUT the worktree carries a `.git/worktrees/<name>/locked` file. The lock typically means a Claude Code agent is (or was) running here. Removing the worktree would race with that agent. The script skips these entirely.

When the user is sure all the corresponding agents are dead, the lock files can be deleted by hand:

```bash
rm /Users/pzoot/Dev/eclipseworkspace/Gemma/.git/worktrees/<name>/locked
```

Then re-run this planning agent — the entries will re-categorize to SAFE.

| Worktree dir | Branch |
|---|---|
| `agent-a00c1c183b55fda9b` | `worktree-agent-a00c1c183b55fda9b` |
| `agent-a00d3a5957c3cdc95` | `worktree-agent-a00d3a5957c3cdc95` |
| `agent-a01debfea4d53e002` | `worktree-agent-a01debfea4d53e002` |
| `agent-a01ebac05c56a7052` | `worktree-agent-a01ebac05c56a7052` |
| `agent-a0312acd276a5e131` | `worktree-agent-a0312acd276a5e131` |
| `agent-a060e73e4353b5592` | `worktree-agent-a060e73e4353b5592` |
| `agent-a06b403c67706e749` | `worktree-agent-a06b403c67706e749` |
| `agent-a10a5cb63f3108fd7` | `worktree-agent-a10a5cb63f3108fd7` |
| `agent-a1b429df344ee6b9e` | `worktree-agent-a1b429df344ee6b9e` |
| `agent-a20108f6dbe4ed87a` | `worktree-agent-a20108f6dbe4ed87a` |
| `agent-a23dd44fdb8a18f87` | `worktree-agent-a23dd44fdb8a18f87` |
| `agent-a257a86ff4056fe66` | `worktree-agent-a257a86ff4056fe66` |
| `agent-a2a8f49a3408139e1` | `worktree-agent-a2a8f49a3408139e1` |
| `agent-a2c18ea63c3822e48` | `worktree-agent-a2c18ea63c3822e48` |
| `agent-a2c5dbd8d1007cf74` | `worktree-agent-a2c5dbd8d1007cf74` |
| `agent-a2c76204fcec2224f` | `worktree-agent-a2c76204fcec2224f` |
| `agent-a2d9d53e4e818f4a2` | `worktree-agent-a2d9d53e4e818f4a2` |
| `agent-a2f4c02204c33a206` | `worktree-agent-a2f4c02204c33a206` |
| `agent-a2ff520b2e4f5ba7c` | `worktree-agent-a2ff520b2e4f5ba7c` |
| `agent-a35e8cccb8a8dd194` | `worktree-agent-a35e8cccb8a8dd194` |
| `agent-a373bf22d37e204e5` | `worktree-agent-a373bf22d37e204e5` |
| `agent-a3766a3c936057c6c` | `worktree-agent-a3766a3c936057c6c` |
| `agent-a3889078e9feeb05d` | `worktree-agent-a3889078e9feeb05d` |
| `agent-a38bd4a2025541439` | `worktree-agent-a38bd4a2025541439` |
| `agent-a3b9b59b68e7e58fa` | `worktree-agent-a3b9b59b68e7e58fa` |
| `agent-a3cbf1b955cea3bce` | `worktree-agent-a3cbf1b955cea3bce` |
| `agent-a42565c51ae3794de` | `worktree-agent-a42565c51ae3794de` |
| `agent-a4351ba5168efab19` | `worktree-agent-a4351ba5168efab19` |
| `agent-a45514004c747e80d` | `worktree-agent-a45514004c747e80d` |
| `agent-a45539d9a2742aa46` | `worktree-agent-a45539d9a2742aa46` |
| `agent-a48f3321e57e8d1ed` | `worktree-agent-a48f3321e57e8d1ed` |
| `agent-a4bed887e0022e00c` | `worktree-agent-a4bed887e0022e00c` |
| `agent-a4c73b97cc3f75e50` | `worktree-agent-a4c73b97cc3f75e50` |
| `agent-a4de64513d998f6a3` | `worktree-agent-a4de64513d998f6a3` |
| `agent-a526e140685c299ae` | `worktree-agent-a526e140685c299ae` |
| `agent-a5405b1bd3a8bb0c6` | `worktree-agent-a5405b1bd3a8bb0c6` |
| `agent-a546af84c0482e499` | `worktree-agent-a546af84c0482e499` |
| `agent-a5d54c1cf12a68338` | `worktree-agent-a5d54c1cf12a68338` |
| `agent-a5fba2255aeef9f91` | `worktree-agent-a5fba2255aeef9f91` |
| `agent-a6115a2756474a2a1` | `worktree-agent-a6115a2756474a2a1` |
| `agent-a65ce15ffc025989c` | `worktree-agent-a65ce15ffc025989c` |
| `agent-a6bceef813487e612` | `worktree-agent-a6bceef813487e612` |
| `agent-a6d773b6f9dcd21bc` | `worktree-agent-a6d773b6f9dcd21bc` |
| `agent-a6e464b1258fbdd0f` | `worktree-agent-a6e464b1258fbdd0f` |
| `agent-a719cad6c20a655be` | `worktree-agent-a719cad6c20a655be` |
| `agent-a7205a8780daaba0d` | `worktree-agent-a7205a8780daaba0d` |
| `agent-a72728457644a589a` | `worktree-agent-a72728457644a589a` |
| `agent-a76b7e2b948c6cdaf` | `worktree-agent-a76b7e2b948c6cdaf` |
| `agent-a78335bfacb9b5067` | `worktree-agent-a78335bfacb9b5067` |
| `agent-a7f525a3ad7b3f575` | `worktree-agent-a7f525a3ad7b3f575` |
| `agent-a800fdc2227e75b2c` | `worktree-agent-a800fdc2227e75b2c` |
| `agent-a81ad5c9f008a26dc` | `worktree-agent-a81ad5c9f008a26dc` |
| `agent-a82f1d75d71a925a5` | `worktree-agent-a82f1d75d71a925a5` |
| `agent-a877fedfa7aa06f2d` | `worktree-agent-a877fedfa7aa06f2d` |
| `agent-a8baacce613fc220e` | `worktree-agent-a8baacce613fc220e` |
| `agent-a8bbbd403545bd143` | `worktree-agent-a8bbbd403545bd143` |
| `agent-a8ec45b544d99440a` | `phase2-acl-leftovers` |
| `agent-a8fd0b9ced991d4d3` | `worktree-agent-a8fd0b9ced991d4d3` |
| `agent-a90cb02386ed6f363` | `worktree-agent-a90cb02386ed6f363` |
| `agent-a90d8a7e2feaed50e` | `worktree-agent-a90d8a7e2feaed50e` |
| `agent-a9acda100206da6b0` | `worktree-agent-a9acda100206da6b0` |
| `agent-a9ad2e0974291acb8` | `worktree-agent-a9ad2e0974291acb8` |
| `agent-a9ba7a302536b7f89` | `worktree-agent-a9ba7a302536b7f89` |
| `agent-a9feefd55f0720f40` | `worktree-agent-a9feefd55f0720f40` |
| `agent-aa6def6d546b486d5` | `worktree-agent-aa6def6d546b486d5` |
| `agent-aa86ddd15a2ccb3fe` | `worktree-agent-aa86ddd15a2ccb3fe` |
| `agent-aac5e08c30ee2ec82` | `worktree-agent-aac5e08c30ee2ec82` |
| `agent-aaee620b0170114f1` | `worktree-agent-aaee620b0170114f1` |
| `agent-aaf5f90112b6ec52a` | `worktree-agent-aaf5f90112b6ec52a` |
| `agent-ab3ab93a70a58c23d` | `worktree-agent-ab3ab93a70a58c23d` |
| `agent-ab565e8fc80d7f087` | `worktree-agent-ab565e8fc80d7f087` |
| `agent-ab9fee6d9207a929e` | `worktree-agent-ab9fee6d9207a929e` |
| `agent-abcd884c6f019d86b` | `worktree-agent-abcd884c6f019d86b` |
| `agent-abd3ad8a0a0664bc8` | `worktree-agent-abd3ad8a0a0664bc8` |
| `agent-abe60b2a827c4fe2f` | `worktree-agent-abe60b2a827c4fe2f` |
| `agent-abef53c86c763f5a6` | `worktree-agent-abef53c86c763f5a6` |
| `agent-ac089d826e6f79d97` | `worktree-agent-ac089d826e6f79d97` |
| `agent-ac0f133d8203e9989` | `worktree-agent-ac0f133d8203e9989` |
| `agent-ac4c150d1f2b4bf27` | `worktree-agent-ac4c150d1f2b4bf27` |
| `agent-ac75874f216cb7a41` | `worktree-agent-ac75874f216cb7a41` |
| `agent-aca7a32638dcaff35` | `worktree-agent-aca7a32638dcaff35` |
| `agent-acfabc27a493cf42c` | `worktree-agent-acfabc27a493cf42c` |
| `agent-ae03810701b241daf` | `worktree-agent-ae03810701b241daf` |
| `agent-ae169e81f53b31ebb` | `worktree-agent-ae169e81f53b31ebb` |
| `agent-ae6dcdc2fbd87656e` | `worktree-agent-ae6dcdc2fbd87656e` |
| `agent-ae7f6767fa2b512a9` | `worktree-agent-ae7f6767fa2b512a9` |
| `agent-ae7fc3f98a9289bf0` | `worktree-agent-ae7fc3f98a9289bf0` |
| `agent-aeb9f848c17af88fe` | `worktree-agent-aeb9f848c17af88fe` |
| `agent-aeda22c308587acfc` | `worktree-agent-aeda22c308587acfc` |
| `agent-af308b68e4049a0ff` | `worktree-agent-af308b68e4049a0ff` |
| `agent-af42f1b5b8d3f0a9a` | `worktree-agent-af42f1b5b8d3f0a9a` |
| `agent-af453a8d6f20bcfe0` | `worktree-agent-af453a8d6f20bcfe0` |
| `agent-aff47784173d10afd` | `worktree-agent-aff47784173d10afd` |
| `agent-aff8989fb47bfd9c8` | `worktree-agent-aff8989fb47bfd9c8` |

### Admin-only orphans (3 entries)

These have admin entries inside parent worktrees' `.git/worktrees/` but the physical worktree dir is reachable only via nested path:

- `agent-container-recce` (nested in `agent-branch-merge-plan`) -> branch `worktree-container-recce` (merged)
- `agent-junit5-phase-a` (nested in `agent-expression-chunk-e2`) -> branch `worktree-junit5-phase-a` (merged)
- `agent-validation-optin` (nested in `agent-branch-merge-plan`) -> branch `worktree-validation-optin` (merged)

These are listed in SAFE with their nested paths. After the SAFE pass, `git worktree prune` is a no-op on these (the admin entries live inside the parent worktrees' git dirs).

## Section 2: Cleanup script

See `cleanup_worktrees.sh` at the repo root — refreshed alongside this plan.

Key properties:

- Hard-coded SAFE list of `path|branch` pairs (110 entries).
- Per-entry: `git worktree remove --force <path>` then `git branch -D <branch>`.
- Idempotent: skips entries whose path already doesn't exist AND whose branch is already gone.
- Bails on the first true error (`set -euo pipefail`) so a partial run is recoverable.
- Confirmation prompt at the top (`Continue? [y/N]`).
- Final step: `git worktree prune` to clear any admin orphans.
- **Does NOT touch** any LOCKED / CAUTION / AVOID / SELF entry.

## Section 3: Re-running the plan

```bash
# from any agent worktree, in the same task as produced this file
# (the categorization logic lives in the planning agent prompt, not in a checked-in script)
```

If lock files get cleared or unmerged work gets merged, re-run the planning agent task — it will regenerate this file. The script's SAFE list is rebuilt from scratch each run, so newly-mergeable + newly-unlocked entries flow into SAFE naturally.
