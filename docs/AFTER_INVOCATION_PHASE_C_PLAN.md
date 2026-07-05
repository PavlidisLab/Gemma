# AfterInvocation Phase C: @EnableMethodSecurity migration plan

Reconnaissance + plan, no code changes. Branch:
`worktree-afterinv-phase-c-prep`, baseline `08e760bdaf`.

Companion documents:
- `AFTER_INVOCATION_MIGRATION.md` (recce roadmap, on
  `worktree-afterinvocation-recce`) — the upstream plan that defines
  Phases A/B/C.
- Phase A landed on `worktree-afterinv-phase-a` (`3067faad`): seven
  fully-redundant providers retired, ~111 call sites switched to
  `@PostAuthorize` / `@PostFilter`.
- Phase B in flight on `worktree-afterinv-phase-b-quiet` (`bf287e9a`),
  `worktree-afterinv-phase-b-cs-dv` (`ba9e8b46`), and
  `worktree-afterinv-phase-b-vo`: the gsec-owned `AclEntryAfterInvocation*`
  providers replaced by Gemma-owned classes under
  `ubic.gemma.core.security.authorization.acl`, same bean ids preserved
  where unchanged or renamed with the `gemma*` prefix where new.

This document **does not** execute the migration. It captures the
shape of the Phase C commit(s) so a follow-up session can land them
without re-doing the analysis.

## 1. Current state (post-Phase-B)

`MethodSecurityConfig` after every Phase B branch lands will wire
**ten** post-invocation provider beans into the legacy
`AfterInvocationProviderManager` bridge:

| # | Bean id | Origin | Shape |
|---|---|---|---|
| 1 | `gemmaAfterAclReadQuiet` | Gemma (Phase B) | single-object, null-on-denial |
| 2 | `afterAclCompositeSequenceCollectionRead` | Gemma (Phase B port) | collection filter, bulk fetch by associated `ArrayDesign` |
| 3 | `afterAclDataVectorCollectionRead` | Gemma (Phase B port) | collection filter, bulk fetch by associated `ExpressionExperiment` |
| 4 | `gemmaAfterAclMyDataRead` | Gemma (Phase B) | collection filter, owner-or-admin + read |
| 5 | `gemmaAfterAclMyPrivateDataRead` | Gemma (Phase B) | collection filter, private-and-readable |
| 6 | `afterAclValueObjectCollection` | gsec, Gemma-owned subclass | collection filter + `SecureValueObject` mutation |
| 7 | `afterAclValueObjectMap` | gsec, Gemma-owned subclass | map-key filter + `SecureValueObject` mutation |
| 8 | `afterAclValueObject` | gsec, Gemma-owned subclass | single-object check + `SecureValueObject` mutation |
| 9 | `gemmaAfterAclStreamRead` | Gemma (Phase B) | `Stream<Securable>` filter, own Hibernate session |
| 10 | `postInvocationAdviceProvider` | stock Spring Security | dispatcher for `@PostAuthorize` / `@PostFilter` SpEL |

After Phase B closes, the only remaining piece of the legacy stack is
the `MethodSecurityConfig` itself: the
`@EnableGlobalMethodSecurity(securedEnabled=true, prePostEnabled=true,
order=1)` annotation, the `GlobalMethodSecurityConfiguration`
superclass, and the three `@Override`s — `createExpressionHandler()`,
`accessDecisionManager()`, `afterInvocationManager()`, `runAsManager()`.

## 2. Target state

`@EnableMethodSecurity` is the Spring Security 6
`AuthorizationManager<T>`-based replacement. It:

- Provides four built-in interceptors out of the box: `@PreAuthorize`,
  `@PostAuthorize`, `@PreFilter`, `@PostFilter`. These use the
  registered `MethodSecurityExpressionHandler` (gsec's
  `securityExpressionHandler`, autowired exactly as it is today)
  and the registered `PermissionEvaluator` (gsec's
  `AclPermissionEvaluator`, also unchanged).
- Has **no** `AfterInvocationManager` extension point. There is no
  drop-in replacement for the `AfterInvocationProviderManager` bridge.
- Allows custom `AuthorizationManager<MethodInvocationResult>` beans
  to be wired as advisors with
  `AuthorizationManagerAfterMethodInterceptor` so post-invocation
  authorisation can still mutate / filter return values.
- Supports `securedEnabled=true` (gates `@Secured`) and
  `jsr250Enabled=true` (gates `@RolesAllowed`) just like the legacy
  annotation.

## 3. Per-provider port

**Nine** Gemma-owned providers (rows 1–9 above) require
`AuthorizationManager<MethodInvocationResult>` adapters. Row 10
(`postInvocationAdviceProvider`) is replaced for free by the built-in
`@PostAuthorize` / `@PostFilter` interceptors of
`@EnableMethodSecurity` — it is **removed** in Phase C, not adapted.

For each Gemma-owned provider, write a small adapter class:

```java
@Component
public class GemmaAfterAclReadQuietAuthorizationManager
        implements AuthorizationManager<MethodInvocationResult> {

    private final AclEntryAfterInvocationQuietReadProvider delegate;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> auth,
                                       MethodInvocationResult mir) {
        // Delegate.decide(authentication, methodInvocation, configAttribs, returnedObject)
        // returns either the (possibly filtered) object or throws AccessDenied.
        // For the quiet provider the post-invocation return value substitution
        // (denial -> null) is enacted by REWRITING mir.getResult() via the
        // interceptor's result-replacement contract, not by an AccessDecision.
        // The wrapper translates between the two contracts.
    }
}
```

Pair each `AuthorizationManager` with an advisor bean that pointcuts
either a custom annotation (`@AfterAclReadQuiet`, `@AfterAclValueObject`,
...) or, where feasible, a method-name / return-type pointcut so we do
**not** have to sweep the ~149 remaining call sites a second time.

Annotation set (Phase C introduces, callers adopt via Phase B-style
sweep):

| Provider | New annotation |
|---|---|
| `gemmaAfterAclReadQuiet` | `@AfterAclReadQuiet` |
| `afterAclCompositeSequenceCollectionRead` | `@AfterAclCompositeSequenceCollectionRead` |
| `afterAclDataVectorCollectionRead` | `@AfterAclDataVectorCollectionRead` |
| `gemmaAfterAclMyDataRead` | `@AfterAclMyDataRead` |
| `gemmaAfterAclMyPrivateDataRead` | `@AfterAclMyPrivateDataRead` |
| `afterAclValueObjectCollection` | `@AfterAclValueObjectCollectionRead` |
| `afterAclValueObjectMap` | `@AfterAclValueObjectMapRead` |
| `afterAclValueObject` | `@AfterAclValueObjectRead` |
| `gemmaAfterAclStreamRead` | `@AfterAclStreamRead` |

Each annotation is `@Target(ElementType.METHOD)`,
`@Retention(RetentionPolicy.RUNTIME)`, and is referenced by an
`AuthorizationManagerAfterMethodInterceptor` registered as an
`Advisor` infrastructure bean. The `AfterInvocationProviderManager`
bridge bean and the `AFTER_INVOCATION_PROVIDER_BEAN_NAMES` list are
deleted in the same commit.

## 4. Annotation rewiring at call sites

`@PostAuthorize` and `@PostFilter` annotations introduced in Phase A
**carry over unchanged**. Both stacks use the same SpEL evaluator
under the hood (gsec's `AclPermissionEvaluator`), and
`@EnableMethodSecurity` builds in `@PostAuthorize` / `@PostFilter`
support natively. The ~111 Phase A call sites need **zero edits** in
Phase C.

The Phase B `@Secured("AFTER_ACL_*")` call sites (the ~38 remaining
across the five non-redundant providers + the quiet provider) need a
mechanical sweep: drop the `AFTER_ACL_*` token from `@Secured` and
add the corresponding `@AfterAcl*` annotation. The pre-invocation
`@Secured` content (`GROUP_*`, `IS_AUTHENTICATED_ANONYMOUSLY`,
`ACL_SECURABLE_*`) is unaffected — those are a separate, parallel
migration (the gsec `AclEntryVoter` family; ~340+ call sites; see
risk callout #2 below).

Side-effecting providers (VO collection / VO map / VO single) cannot
be expressed in `@PostAuthorize` / `@PostFilter` SpEL because they
must mutate `SecureValueObject` fields
(`isPublic`/`isShared`/`userOwned`/`userCanWrite`). The
`AuthorizationManager` wrapper has access to `MethodInvocationResult`
and **can** perform the mutation in its `check()` body before
returning `new AuthorizationDecision(true)`. No SpEL hand-off
required.

## 5. `securedEnabled` consideration

`@EnableGlobalMethodSecurity(securedEnabled=true)` is currently
required because Gemma uses `@Secured` heavily for pre-invocation
checks (the `GROUP_*` / `RUN_AS_ADMIN` / `ACL_SECURABLE_*` tokens).
The `worktree-secured-prauthorize` branch (HEAD `7c9af60b4d`,
unmerged) consolidates gemma-rest's `@Secured` annotations to
`@PreAuthorize`. **It does not touch gemma-core**, where most
`@Secured` usage lives:

- `RUN_AS_ADMIN` is used in 11 call sites in gemma-core
  (`ExpressionExperimentReportService`, `UserManager` — 9 places —
  plus the doc reference in `MethodSecurityConfig` itself).
  `@Secured("RUN_AS_ADMIN")` triggers the `runAsManager()` chain;
  there is **no Spring Security 6 modern equivalent** for run-as
  elevation (the recce explicitly notes this: "Spring Security 6 has
  no built-in `@RunAs` replacement"). Per-call manual
  `SecurityContextHolder` push, or a custom Gemma advisor, is
  required to retire those 11 call sites.
- `ACL_SECURABLE_*` pre-invocation voters (the `AclEntryVoter`
  family) are still wired in gsec's
  `applicationContext-gsec.xml` and consumed via
  `@Secured("ACL_SECURABLE_*")` across hundreds of call sites.

**Decision.** Phase C keeps `securedEnabled=true` on
`@EnableMethodSecurity`. Removing `securedEnabled` is gated on **both**
the gsec voter migration (separate recce) **and** a `RUN_AS_ADMIN`
replacement strategy. Phase C does not block on either.

## 6. Risk callouts

1. **Web-side `AccessDecisionManager` survives.** The
   `httpAccessDecisionManager` referenced at
   `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:222`
   and `.../PlatformsWebService.java:87` is wired into the JAX-RS
   filter chain for fine-grained per-request ACL voting on dataset /
   platform listings. It is **web-side**, not method-security side. It
   is **not** affected by `MethodSecurityConfig` and survives Phase C
   independently. (The web-side migration would track Spring Security
   6's `AuthorizationManagerFilter` and is out of scope for the
   after-invocation work.)
2. **`AclEntryVoter` family blocks `accessDecisionManager()`
   removal.** The `accessDecisionManager()` override in
   `MethodSecurityConfig` returns the `UnanimousBased`
   `accessDecisionManager` bean wired in
   `applicationContext-gsec.xml` with the `aclEntrySecurableReadVoter`,
   `aclEntrySecurableEditVoter`, and friends. As long as any
   `@Secured("ACL_SECURABLE_*")` annotation exists in the codebase
   this method-security `AccessDecisionManager` must keep working.
   `@EnableMethodSecurity` accepts a manually-wired
   `MethodSecurityInterceptor` advisor that delegates to a legacy
   `AccessDecisionManager`, but this is essentially the same bridge
   we are trying to remove. **Phase C therefore depends on the gsec
   voter recce + migration completing first**, OR on Phase C accepting
   a residual `AccessDecisionManager` bridge advisor alongside the
   new `AuthorizationManager` advisors. Recommended: gate Phase C on
   the voter migration.
3. **`runAsManager()` removal is out of scope.** The 11
   `@Secured("RUN_AS_ADMIN")` call sites need a separate replacement
   strategy. Phase C keeps the override in place; closing
   `MethodSecurityConfig` entirely is a Phase D concern.
4. **`securityExpressionHandler` autowire.** The gsec
   `securityExpressionHandler` bean is the singleton
   `MethodSecurityExpressionHandler` in the context. Spring Security
   6's `MethodSecurityConfigurer` autowires it the same way the
   legacy `GlobalMethodSecurityConfiguration` does, so Phase C does
   **not** need to override `createExpressionHandler()`. Verify
   during integration testing that the gsec bean is still picked up
   (there should be no second `MethodSecurityExpressionHandler` in
   the context).
5. **Order of advisors.** The new
   `AuthorizationManagerAfterMethodInterceptor` advisors must run at
   an order strictly higher than `POST_FILTER.getOrder()` so they
   execute **after** any `@PostFilter` already applied to the same
   method (e.g. a method may pre-filter by ACL via `@PostFilter` and
   then need `SecureValueObject` population on top). Misordering
   would either skip the side-effect or double-filter.
6. **Bean-id contract.** The Phase B branches deliberately preserve
   the legacy bean ids (`afterAclValueObjectCollection`, ...) so
   Phase C is a clean cutover. Phase C deletes both the bean ids
   themselves and the `applicationContext-security.xml` /
   `applicationContext-gsec.xml` bean definitions for the providers.
   Verify no other consumer references them: a `grep` for each bean
   id outside `MethodSecurityConfig` and the bean definitions
   themselves must return zero hits before each delete commits.
7. **`postInvocationAdviceProvider` deletion.** This stock-Spring
   bean is wired today as the dispatcher for `@PostAuthorize` /
   `@PostFilter` through the `AfterInvocationProviderManager`.
   `@EnableMethodSecurity` builds those interceptors directly; the
   bean has no other consumer. Safe to drop with the bridge.

## 7. Phased plan + effort estimate

Phase C as currently scoped lands in three reviewable commits.

**Commit C1 — adapters and annotations.** Introduce the nine
`AuthorizationManager<MethodInvocationResult>` adapters under
`ubic.gemma.core.security.authorization.acl.authmanager` plus the
nine `@AfterAcl*` annotations and a single
`@Configuration` class that registers them as `Advisor` beans. The
`MethodSecurityConfig` bridge stays intact; both the legacy and the
new advisors are live in parallel. Effort: 3 days. Risk: low —
parallel-running is the natural feature-flag.

**Commit C2 — call-site sweep.** Rewrite the ~38 Phase B call sites
from `@Secured("AFTER_ACL_*")` to the new `@AfterAcl*` annotations.
Keep the `@Secured` content stripped of `AFTER_ACL_*` only — leave
`GROUP_*` / `ACL_SECURABLE_*` / `RUN_AS_ADMIN` tokens in place. With
both stacks live the call sites get authorised once by the new
adapter and once again by the legacy bridge (idempotent for the pure
filters; the VO-mutation providers must guard against double-mutation
— add an idempotency check in the adapter). Effort: 1 day. Risk:
medium (VO double-mutation).

**Commit C3 — cutover.** Flip `@EnableGlobalMethodSecurity` to
`@EnableMethodSecurity(prePostEnabled=true, securedEnabled=true)`,
delete `afterInvocationManager()`, delete the
`AFTER_INVOCATION_PROVIDER_BEAN_NAMES` list, delete the
`postInvocationAdviceProvider` bean definition, delete the nine
provider beans from `applicationContext-security.xml` and
`applicationContext-gsec.xml`, delete the provider classes from
gemma-core and (with a coordinated gsec release) from gsec.
`accessDecisionManager()` and `runAsManager()` overrides survive —
they are gated on the voter recce and a run-as replacement
respectively. Effort: 2 days. Risk: low — the new advisors are
already proven by C1 + C2.

**Total Phase C effort: ~6 days dev + 2 days integration test +
1 day docs.**

## 8. Sequencing relative to in-flight work

Phase C **must not start** until:

1. All three Phase B branches (`worktree-afterinv-phase-b-quiet`,
   `worktree-afterinv-phase-b-cs-dv`, `worktree-afterinv-phase-b-vo`)
   merge to `phase2-acl-migrate` (or its successor).
2. The gsec `AclEntryVoter` recce (the "pre-invocation voter
   migration") produces a plan that says either (a) the voters can
   be retired before Phase C, or (b) Phase C ships with the
   `accessDecisionManager()` override surviving and we close it in
   Phase D.

Phase C **does not depend on**:

- `worktree-secured-prauthorize` (gemma-rest `@Secured` cleanup). That
  branch reduces the `securedEnabled` blast radius in gemma-rest but
  Phase C keeps `securedEnabled=true` regardless.
- The web-side `httpAccessDecisionManager` migration.
- A `RUN_AS_ADMIN` replacement strategy.

## 9. Open questions for Phase C session

1. Custom annotation vs. `@PostFilter`-with-side-effect hybrid: should
   the side-effecting providers (VO collection / VO map / VO single)
   keep their own `@AfterAcl*` annotation, or should we standardise
   on `@PostFilter` + an `@AfterReturning` aspect that runs `populate*`
   methods? The annotation route is cleaner per-method but introduces
   nine new annotation types; the aspect route is two-step but uses
   only standard Spring Security idioms. Recommended: dedicated
   `@AfterAcl*` annotations, mirroring what the recce sketches.
2. Should the `gemmaAfterAclMyDataRead` / `gemmaAfterAclMyPrivateDataRead`
   helpers also surface `@aclMeta.isOwner(o)` / `@aclMeta.isPrivate(o)`
   SpEL components so future call sites can use plain `@PostFilter`
   instead of the dedicated annotation? Worth adding in C1 — same
   effort, lower friction for downstream maintenance.
3. Idempotency contract for VO-mutation adapters during the C1+C2
   parallel-running window: short-circuit if `isPublic` is already
   set to a non-default value? Or rely on the legacy bridge running
   first and the new adapter being a no-op when fields are already
   populated? Decide before C2.

## 10. Validation plan

- Unit: each `AuthorizationManager` adapter has a test that asserts
  the same input → same output as its underlying provider, using the
  existing provider-level tests as the contract (port them).
- Integration: dataset browser (VO collection), dataset detail page
  (VO single + populate), platform listing (CS-by-AD), batch effect
  endpoint (DV-by-EE), user-data page (my-data + my-private-data),
  any stream endpoint (audit trail stream). Each must render the
  same set of items with the same public/shared/owner badges as the
  legacy stack.
- Regression: the existing `MethodSecurityConfigIntegrationTest` (if
  it exists; otherwise create one) must pass both before and after
  the cutover with the same fixtures.
- Manual: log in as anonymous, regular user, group owner, admin —
  walk the four standard views and diff against pre-cutover screenshots.

## 11. Out of scope (Phase D and beyond)

- Retiring `accessDecisionManager()` override (gated on gsec voter
  migration).
- Retiring `runAsManager()` override (gated on `RUN_AS_ADMIN`
  replacement strategy — 11 call sites).
- Web-side `httpAccessDecisionManager` migration (Spring Security 6
  `AuthorizationManagerFilter`).
- gsec library deprecation / merge of remaining gsec classes into
  gemma-core. After Phase C the only gsec method-security consumers
  are the `AclEntryVoter` family + `AclPermissionEvaluator`; both
  are slated for parallel migrations.
