# Worktree cleanup plan v3

Generated 2026-05-20 on branch `phase2-acl-migrate` (HEAD `06ce2fa558`).

Refresh of the 2026-05-18 / 2026-05-19 baseline (`WORKTREE_CLEANUP_PLAN.md` / `_v2`).
Re-categorized every worktree against the current `phase2-acl-migrate` tip.

## Summary

| Category | Count | Action |
|---|---|---|
| SAFE | 147 | Removed by `cleanup_worktrees_v3.sh` (~31.7 GB freed) |
| CAUTION (dirty) | 4 | Leave alone — uncommitted edits |
| CAUTION (unmerged) | 0 | Leave alone — branch commits not in `phase2-acl-migrate` |
| LOCKED (active) | 111 | Leave alone — locked, may be live agent |
| SELF | 1 | This agent's own worktree (`feat-batch95`) |
| NESTED (info only) | 3 | Already counted in SAFE; removed nested-first |

Total non-main worktree admin entries: **263**.
Estimated disk reclaim from SAFE bucket: **31.7 GB** of the 48 GB total under `.claude/worktrees/`.

## SAFE — full path|branch pairs

Branch merged into `phase2-acl-migrate`, working tree clean, no lockfile, not SELF.
Nested children appear first (3 entries) so they are removed before their dirty parents are touched.

```
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan/.claude/worktrees/agent-container-recce|worktree-container-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan/.claude/worktrees/agent-validation-optin|worktree-validation-optin
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e2/.claude/worktrees/agent-junit5-phase-a|worktree-junit5-phase-a
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aclentryvoter-recce|worktree-aclentryvoter-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aclvoter-x1-wrappers|worktree-aclvoter-x1-wrappers
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-actuator-impl|worktree-actuator-impl
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-actuator-recce|worktree-actuator-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-a|worktree-afterinv-phase-a
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-b-cs-dv|worktree-afterinv-phase-b-cs-dv
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-b-quiet|worktree-afterinv-phase-b-quiet
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-b-vo|worktree-afterinv-phase-b-vo
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-c-prep|worktree-afterinv-phase-c-prep
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinvocation-recce|worktree-afterinvocation-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-annotations-writeback|worktree-annotations-writeback
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aspectj-deeper|worktree-aspectj-deeper
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aspectj-ehcache-audit|worktree-aspectj-ehcache-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aspectj-invariant|worktree-aspectj-invariant
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-basejersey-cleanup|worktree-basejersey-cleanup
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-bk-consolidation|worktree-bk-consolidation
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-cacheable-audit|worktree-cacheable-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-commonslog-to-slf4j|worktree-commonslog-to-slf4j
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-curation-ui-contract|worktree-curation-ui-contract
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-proxy-fix|worktree-ee-proxy-fix
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-p1|worktree-ee-svc-decomp-p1
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-p15|worktree-ee-svc-decomp-p15
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-p2|worktree-ee-svc-decomp-p2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-recce|worktree-ee-svc-decomp-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ehcache-cachemanager-fix|worktree-ehcache-cachemanager-fix
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-executor-virtual-prep|worktree-executor-virtual-prep
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-executor-vt-callers|worktree-executor-vt-callers
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-executor-vt-callers-2|worktree-executor-vt-callers-2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e1|worktree-expression-chunk-e1
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e3|worktree-expression-chunk-e3
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e4|worktree-expression-chunk-e4
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e5|worktree-expression-chunk-e5
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expressionpersister-recce|worktree-expressionpersister-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-fixture-bioassay|worktree-fixture-bioassay
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-fixture-factories-2|worktree-fixture-factories-2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-fixture-gene-cs|worktree-fixture-gene-cs
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-framework-bump-recce|worktree-framework-bump-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-cli-modernize|worktree-gemma-cli-modernize
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-rest-bootstrap|worktree-gemma-rest-bootstrap
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-rest-standalone-recce|worktree-gemma-rest-standalone-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-web-retire|worktree-gemma-web-retire
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-51|worktree-genome-chunk-51
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-52|worktree-genome-chunk-52
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-53-prep|worktree-genome-chunk-53-prep
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-53-taxonfix|worktree-genome-chunk-53-taxonfix
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-54-cutover|worktree-genome-chunk-54-cutover
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-54-retry|worktree-genome-chunk-54-retry
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-bump-exec|worktree-gsec-bump-exec
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-hql-continued|worktree-gsec-hql-continued
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-hql-v2|worktree-gsec-hql-v2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-version-align|worktree-gsec-version-align
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hibernate-envers-audit|worktree-hibernate-envers-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hibernate-l2-tune|worktree-hibernate-l2-tune
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hibernate-type-audit|worktree-hibernate-type-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hikari-modernize|worktree-hikari-modernize
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ignore-audit-v2|worktree-ignore-audit-v2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-impl-autowire-rule|worktree-impl-autowire-rule
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-java21-phase1|worktree-java21-phase1
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-java21-readiness|worktree-java21-readiness
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-jsr305-cleanup|worktree-jsr305-cleanup
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-jstl-jakarta|worktree-jstl-jakarta
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-junit5-phase-b0|worktree-junit5-phase-b0
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-junit5-recce|worktree-junit5-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-l2-cache-bound|worktree-l2-cache-bound
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-listenablefuture|worktree-listenablefuture
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-logging-modernize|worktree-logging-modernize
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-lombok-audit|worktree-lombok-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-lombok-cleanup|worktree-lombok-cleanup
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-maven-modernize|worktree-maven-modernize
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-maven-release-recce|worktree-maven-release-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-metrics-jcache-restore|worktree-metrics-jcache-restore
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-mockito-modernize|worktree-mockito-modernize
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-openapi-audit|worktree-openapi-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-delete-plan|worktree-persister-delete-plan
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-genome|worktree-persister-genome
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-recce|worktree-persister-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-step2|worktree-persister-step2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-step3-ad|worktree-persister-step3-ad
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-profile-cleanup|worktree-profile-cleanup
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-querycache-shard|worktree-querycache-shard
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-relationshippersister|worktree-relationshippersister
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-release-small-fixes|worktree-release-small-fixes
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-rest-security-config|worktree-rest-security-config
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-restclient-migrate|worktree-restclient-migrate
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-resttemplate-audit|worktree-resttemplate-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-secured-prauthorize|worktree-secured-prauthorize
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-servlet6-audit|worktree-servlet6-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-session-getreference|worktree-session-getreference
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-session-refresh-v2|worktree-session-refresh-v2
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-slf4j-bump|worktree-slf4j-bump
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-boot-3-recce|worktree-spring-boot-3-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-boot-bom|worktree-spring-boot-bom
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-profiles-audit|worktree-spring-profiles-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-security-7-recce|worktree-spring-security-7-recce
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring6-deprecation-hunt|worktree-spring6-deprecation-hunt
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-static-analysis-audit|worktree-static-analysis-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-test-failure-triage|worktree-test-failure-triage
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit|worktree-validation-audit
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-config-kickoff|worktree-xml-config-kickoff
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-datasource|worktree-xml-datasource
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-gemma-cli|worktree-xml-gemma-cli
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-gemma-rest|worktree-xml-gemma-rest
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-hibernate|worktree-xml-hibernate
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-schedule|worktree-xml-schedule
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-security|worktree-xml-security
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch66|decomp-batch66
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch67|decomp-batch67
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch68|decomp-batch68
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch69|decomp-batch69
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch70|decomp-batch70
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch71|decomp-batch71
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch85|feat-batch85
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch86|feat-batch86
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch87|feat-batch87
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch88|feat-batch88
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch89|feat-batch89
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch90|feat-batch90
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch91|feat-batch91
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch96|feat-batch96
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch46|junit5-batch46
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch47|junit5-batch47
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch48|junit5-batch48
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch49|junit5-batch49
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch50|junit5-batch50
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch51|junit5-batch51
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch52|junit5-batch52
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch53|junit5-batch53
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch54|junit5-batch54
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch55|junit5-batch55
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch57|junit5-batch57
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch58|junit5-batch58
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch59|junit5-batch59
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch61|junit5-batch61
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch62|junit5-batch62
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch63|junit5-batch63
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch73|standalone-batch73
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch74|standalone-batch74
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch75|standalone-batch75
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch76|standalone-batch76
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch77|standalone-batch77
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch78|standalone-batch78
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch79|standalone-batch79
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch80|standalone-batch80
/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch81|standalone-batch81
```

## CAUTION (unmerged branch)

_None._ All non-locked, non-dirty worktrees have branches fully merged into `phase2-acl-migrate`.

## CAUTION (dirty working tree)

- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan` (branch `worktree-branch-merge-plan`) — 3 dirty file(s)
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e2` (branch `worktree-expression-chunk-e2`) — 1 dirty file(s)
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch93` (branch `feat-batch93`) — 6 dirty file(s)
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch94` (branch `feat-batch94`) — 3 dirty file(s)

Note: `feat-batch93` and `feat-batch94` are sibling recce agents running in parallel.
`agent-branch-merge-plan` and `agent-expression-chunk-e2` are older containers holding
nested children that ARE in the SAFE list (removed first by the cleanup script).

## LOCKED (active) — 111 entries

`.git/worktrees/<id>/locked` is present. These are live or recently-stopped agents.
Leave alone — `git worktree remove --force` refuses to touch them without `--force-remove-lock`.

First 20 (full list in JSON sidecar `/tmp/worktree_classification.json`):

- `agent-a00c1c183b55fda9b` (branch `worktree-agent-a00c1c183b55fda9b`)
- `agent-a00d3a5957c3cdc95` (branch `worktree-agent-a00d3a5957c3cdc95`)
- `agent-a01debfea4d53e002` (branch `worktree-agent-a01debfea4d53e002`)
- `agent-a01ebac05c56a7052` (branch `worktree-agent-a01ebac05c56a7052`)
- `agent-a0312acd276a5e131` (branch `worktree-agent-a0312acd276a5e131`)
- `agent-a060e73e4353b5592` (branch `worktree-agent-a060e73e4353b5592`)
- `agent-a06b403c67706e749` (branch `worktree-agent-a06b403c67706e749`)
- `agent-a10a5cb63f3108fd7` (branch `worktree-agent-a10a5cb63f3108fd7`)
- `agent-a1b429df344ee6b9e` (branch `worktree-agent-a1b429df344ee6b9e`)
- `agent-a1f3a63d1ab1cf5ee` (branch `worktree-agent-a1f3a63d1ab1cf5ee`)
- `agent-a20108f6dbe4ed87a` (branch `worktree-agent-a20108f6dbe4ed87a`)
- `agent-a23dd44fdb8a18f87` (branch `worktree-agent-a23dd44fdb8a18f87`)
- `agent-a257a86ff4056fe66` (branch `worktree-agent-a257a86ff4056fe66`)
- `agent-a2a8f49a3408139e1` (branch `worktree-agent-a2a8f49a3408139e1`)
- `agent-a2c18ea63c3822e48` (branch `worktree-agent-a2c18ea63c3822e48`)
- `agent-a2c5dbd8d1007cf74` (branch `worktree-agent-a2c5dbd8d1007cf74`)
- `agent-a2c76204fcec2224f` (branch `worktree-agent-a2c76204fcec2224f`)
- `agent-a2d9d53e4e818f4a2` (branch `worktree-agent-a2d9d53e4e818f4a2`)
- `agent-a2f4c02204c33a206` (branch `worktree-agent-a2f4c02204c33a206`)
- `agent-a2ff520b2e4f5ba7c` (branch `worktree-agent-a2ff520b2e4f5ba7c`)
- … and 91 more.

## NESTED (info only)

- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan/.claude/worktrees/agent-container-recce`
  - parent: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan`
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan/.claude/worktrees/agent-validation-optin`
  - parent: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan`
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e2/.claude/worktrees/agent-junit5-phase-a`
  - parent: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e2`

## Recommendation

Running `cleanup_worktrees_v3.sh --yes` should free approximately **31.7 GB** by
removing **147** merged-and-clean worktrees. Risk profile is low:

- Every SAFE branch has zero commits ahead of `phase2-acl-migrate` (the squash/merge
  commits already landed).
- Every SAFE working tree is clean (no `M ` / `?? ` per `git status --porcelain`).
- LOCKED entries (111) are skipped — those are live agents or recently-stopped ones.
- 4 CAUTION-dirty trees keep their uncommitted edits.
- SELF (this agent's `feat-batch95` worktree) is hardcoded out of the list.

After the cleanup, expect ~16 GB still on disk under `.claude/worktrees/` (mostly the
111 locked agent worktrees + the 4 dirty ones + self).
