#!/usr/bin/env bash
#
# cleanup_worktrees_v3.sh — remove worktrees whose branches are already merged
# into phase2-acl-migrate and whose working trees are clean and unlocked.
#
# Regenerated 2026-05-20 against tip 06ce2fa558.
# See WORKTREE_CLEANUP_PLAN_v3.md for the full plan (147 SAFE entries,
# ~31.7 GB reclaim).
#
# Safety rails:
#   * Hard-coded SAFE list — anything outside this list is untouched.
#   * Idempotent: skips entries already gone.
#   * Asks for confirmation before doing anything destructive.
#   * Bails out on the first unexpected error.
#   * Self-protection: feat-batch95 (this recce agent) never appears in SAFE.
#   * Locked worktrees and dirty worktrees are NOT in SAFE.
#
# Ordering: 3 nested child worktrees come first so they get removed before
# their (dirty, untouched) parents are encountered.
#
# Usage:
#   bash cleanup_worktrees_v3.sh                # interactive, asks y/N
#   bash cleanup_worktrees_v3.sh --yes          # scripted, skip confirmation
#
set -euo pipefail

REPO_ROOT="/Users/pzoot/Dev/eclipseworkspace/Gemma"

SAFE=(
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan/.claude/worktrees/agent-container-recce|worktree-container-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-branch-merge-plan/.claude/worktrees/agent-validation-optin|worktree-validation-optin"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e2/.claude/worktrees/agent-junit5-phase-a|worktree-junit5-phase-a"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aclentryvoter-recce|worktree-aclentryvoter-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aclvoter-x1-wrappers|worktree-aclvoter-x1-wrappers"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-actuator-impl|worktree-actuator-impl"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-actuator-recce|worktree-actuator-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-a|worktree-afterinv-phase-a"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-b-cs-dv|worktree-afterinv-phase-b-cs-dv"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-b-quiet|worktree-afterinv-phase-b-quiet"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-b-vo|worktree-afterinv-phase-b-vo"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinv-phase-c-prep|worktree-afterinv-phase-c-prep"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-afterinvocation-recce|worktree-afterinvocation-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-annotations-writeback|worktree-annotations-writeback"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aspectj-deeper|worktree-aspectj-deeper"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aspectj-ehcache-audit|worktree-aspectj-ehcache-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-aspectj-invariant|worktree-aspectj-invariant"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-basejersey-cleanup|worktree-basejersey-cleanup"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-bk-consolidation|worktree-bk-consolidation"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-cacheable-audit|worktree-cacheable-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-commonslog-to-slf4j|worktree-commonslog-to-slf4j"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-curation-ui-contract|worktree-curation-ui-contract"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-proxy-fix|worktree-ee-proxy-fix"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-p1|worktree-ee-svc-decomp-p1"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-p15|worktree-ee-svc-decomp-p15"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-p2|worktree-ee-svc-decomp-p2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ee-svc-decomp-recce|worktree-ee-svc-decomp-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ehcache-cachemanager-fix|worktree-ehcache-cachemanager-fix"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-executor-virtual-prep|worktree-executor-virtual-prep"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-executor-vt-callers|worktree-executor-vt-callers"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-executor-vt-callers-2|worktree-executor-vt-callers-2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e1|worktree-expression-chunk-e1"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e3|worktree-expression-chunk-e3"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e4|worktree-expression-chunk-e4"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expression-chunk-e5|worktree-expression-chunk-e5"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-expressionpersister-recce|worktree-expressionpersister-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-fixture-bioassay|worktree-fixture-bioassay"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-fixture-factories-2|worktree-fixture-factories-2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-fixture-gene-cs|worktree-fixture-gene-cs"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-framework-bump-recce|worktree-framework-bump-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-cli-modernize|worktree-gemma-cli-modernize"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-rest-bootstrap|worktree-gemma-rest-bootstrap"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-rest-standalone-recce|worktree-gemma-rest-standalone-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gemma-web-retire|worktree-gemma-web-retire"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-51|worktree-genome-chunk-51"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-52|worktree-genome-chunk-52"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-53-prep|worktree-genome-chunk-53-prep"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-53-taxonfix|worktree-genome-chunk-53-taxonfix"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-54-cutover|worktree-genome-chunk-54-cutover"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-genome-chunk-54-retry|worktree-genome-chunk-54-retry"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-bump-exec|worktree-gsec-bump-exec"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-hql-continued|worktree-gsec-hql-continued"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-hql-v2|worktree-gsec-hql-v2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-gsec-version-align|worktree-gsec-version-align"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hibernate-envers-audit|worktree-hibernate-envers-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hibernate-l2-tune|worktree-hibernate-l2-tune"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hibernate-type-audit|worktree-hibernate-type-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-hikari-modernize|worktree-hikari-modernize"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-ignore-audit-v2|worktree-ignore-audit-v2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-impl-autowire-rule|worktree-impl-autowire-rule"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-java21-phase1|worktree-java21-phase1"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-java21-readiness|worktree-java21-readiness"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-jsr305-cleanup|worktree-jsr305-cleanup"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-jstl-jakarta|worktree-jstl-jakarta"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-junit5-phase-b0|worktree-junit5-phase-b0"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-junit5-recce|worktree-junit5-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-l2-cache-bound|worktree-l2-cache-bound"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-listenablefuture|worktree-listenablefuture"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-logging-modernize|worktree-logging-modernize"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-lombok-audit|worktree-lombok-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-lombok-cleanup|worktree-lombok-cleanup"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-maven-modernize|worktree-maven-modernize"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-maven-release-recce|worktree-maven-release-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-metrics-jcache-restore|worktree-metrics-jcache-restore"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-mockito-modernize|worktree-mockito-modernize"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-openapi-audit|worktree-openapi-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-delete-plan|worktree-persister-delete-plan"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-genome|worktree-persister-genome"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-recce|worktree-persister-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-step2|worktree-persister-step2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-persister-step3-ad|worktree-persister-step3-ad"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-profile-cleanup|worktree-profile-cleanup"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-querycache-shard|worktree-querycache-shard"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-relationshippersister|worktree-relationshippersister"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-release-small-fixes|worktree-release-small-fixes"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-rest-security-config|worktree-rest-security-config"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-restclient-migrate|worktree-restclient-migrate"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-resttemplate-audit|worktree-resttemplate-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-secured-prauthorize|worktree-secured-prauthorize"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-servlet6-audit|worktree-servlet6-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-session-getreference|worktree-session-getreference"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-session-refresh-v2|worktree-session-refresh-v2"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-slf4j-bump|worktree-slf4j-bump"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-boot-3-recce|worktree-spring-boot-3-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-boot-bom|worktree-spring-boot-bom"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-profiles-audit|worktree-spring-profiles-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring-security-7-recce|worktree-spring-security-7-recce"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-spring6-deprecation-hunt|worktree-spring6-deprecation-hunt"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-static-analysis-audit|worktree-static-analysis-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-test-failure-triage|worktree-test-failure-triage"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit|worktree-validation-audit"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-config-kickoff|worktree-xml-config-kickoff"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-datasource|worktree-xml-datasource"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-gemma-cli|worktree-xml-gemma-cli"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-gemma-rest|worktree-xml-gemma-rest"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-hibernate|worktree-xml-hibernate"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-schedule|worktree-xml-schedule"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-xml-security|worktree-xml-security"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch66|decomp-batch66"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch67|decomp-batch67"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch68|decomp-batch68"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch69|decomp-batch69"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch70|decomp-batch70"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/decomp-batch71|decomp-batch71"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch85|feat-batch85"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch86|feat-batch86"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch87|feat-batch87"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch88|feat-batch88"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch89|feat-batch89"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch90|feat-batch90"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch91|feat-batch91"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/feat-batch96|feat-batch96"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch46|junit5-batch46"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch47|junit5-batch47"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch48|junit5-batch48"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch49|junit5-batch49"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch50|junit5-batch50"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch51|junit5-batch51"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch52|junit5-batch52"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch53|junit5-batch53"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch54|junit5-batch54"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch55|junit5-batch55"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch57|junit5-batch57"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch58|junit5-batch58"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch59|junit5-batch59"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch61|junit5-batch61"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch62|junit5-batch62"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/junit5-batch63|junit5-batch63"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch73|standalone-batch73"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch74|standalone-batch74"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch75|standalone-batch75"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch76|standalone-batch76"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch77|standalone-batch77"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch78|standalone-batch78"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch79|standalone-batch79"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch80|standalone-batch80"
    "/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch81|standalone-batch81"
)

cd "$REPO_ROOT"

assume_yes=0
if [ "${1:-}" = "--yes" ] || [ "${1:-}" = "-y" ]; then
    assume_yes=1
fi

echo "Will remove ${#SAFE[@]} merged-and-clean worktrees from $REPO_ROOT/.claude/worktrees/"
echo "Then runs 'git worktree prune' to clear any admin-only orphans left behind."
echo
echo "First 5 worktrees that will be removed (nested children first):"
for entry in "${SAFE[@]:0:5}"; do
    path="${entry%%|*}"
    branch="${entry##*|}"
    echo "  $path  ($branch)"
done
echo "  ... and $((${#SAFE[@]} - 5)) more."
echo

if [ "$assume_yes" -ne 1 ]; then
    read -r -p "About to remove ${#SAFE[@]} worktrees. Continue? [y/N] " ans
    case "$ans" in
        y|Y|yes|YES) ;;
        *) echo "Aborted."; exit 0 ;;
    esac
fi

removed=0
skipped=0
errors=0

for entry in "${SAFE[@]}"; do
    path="${entry%%|*}"
    branch="${entry##*|}"

    have_path=0
    have_branch=0
    [ -e "$path" ] && have_path=1
    if git show-ref --verify --quiet "refs/heads/$branch"; then
        have_branch=1
    fi

    if [ "$have_path" -eq 0 ] && [ "$have_branch" -eq 0 ]; then
        skipped=$((skipped + 1))
        continue
    fi

    echo "-- $branch"

    if [ "$have_path" -eq 1 ]; then
        if git worktree remove --force "$path" 2>&1; then
            :
        else
            echo "   worktree remove failed for $path"
            errors=$((errors + 1))
            continue
        fi
    fi

    if [ "$have_branch" -eq 1 ]; then
        if git branch -D "$branch" >/dev/null 2>&1; then
            :
        else
            echo "   branch delete failed for $branch"
            errors=$((errors + 1))
            continue
        fi
    fi

    removed=$((removed + 1))
done

echo
echo "Pruning admin-only orphan entries..."
git worktree prune -v || true

echo
echo "Done."
echo "  removed: $removed"
echo "  skipped (already gone): $skipped"
echo "  errors:  $errors"

if [ "$errors" -gt 0 ]; then
    exit 1
fi
