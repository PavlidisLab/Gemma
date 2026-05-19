# ACL AclEntryVoter family migration plan

Reconnaissance + plan, no code changes. Branch:
`worktree-aclentryvoter-recce`, baseline `08e760bdaf`.

Companion documents:
- `AFTER_INVOCATION_PHASE_C_PLAN.md` (commit `c587f38640` on
  `worktree-afterinv-phase-c-prep`) - the recce that identified this
  voter-family migration as the **long-pole blocker** to retiring
  `MethodSecurityConfig`'s `GlobalMethodSecurityConfiguration` base
  class in favour of `@EnableMethodSecurity`. Phase C is gated on
  this plan closing with either a retirement path or an explicit
  accept-the-bridge decision.

This document **does not** execute the migration. It captures the
shape of the cutover so a follow-up session can land it without
re-doing the analysis.

## 1. Voter inventory

All four pre-invocation voters live in gsec at
`/Users/pzoot/Dev/gsec/gsec/src/main/java/gemma/gsec/acl/voter/`.

| Class | Base | Domain object source | Notes |
|---|---|---|---|
| `AclEntryVoter` | extends `org.springframework.security.acls.AclEntryVoter` (stock) | first matching method argument | Adds `_IGNORE_TRANSIENT` suffix support: when configured with an `ObjectTransientnessRetrievalStrategy`, transient (un-persisted) instances bypass the ACL check (grant or abstain per `grantOnTransient`). Used by the `securableEditVoter` so create/save methods that accept a not-yet-persisted entity don't trip on a missing ACL row. |
| `AclEntryCollectionVoter` | extends `AbstractAclVoter` (stock) | first argument whose generic element type or contents are assignable to `processDomainObjectClass` (default `gemma.gsec.model.Securable`) | Bulk-loads ACLs via `aclService.readAclsById(ids)`. Decision: **all** elements must be granted - one denial denies the call. Empty collection -> `AuthorizationServiceException`. Same `_IGNORE_TRANSIENT` shortcut as above. |
| `AclEntryMapVoter` | extends `AclEntryCollectionVoter` | first `Map` argument's **key set** | Hands the key collection off to the parent's loop. Domain object class still defaults to `Securable`. |
| `AclEntryMapValueVoter` | extends `AclEntryCollectionVoter` | first `Map` argument's **value collection** | Same as above but the map's values are the ACL subjects. |

The bean definitions live in `gsec/applicationContext-gsec.xml`
(lines 133-251). Eight voter beans are wired - read + edit variants
of each of the four classes - producing eight `ACL_SECURABLE_*`
config attributes. The decision-manager that hosts them is also
defined in gsec (lines 98-124): `accessDecisionManager` is a
`UnanimousBased` containing the eight ACL voters plus
`roleHierarchyVoter`, `AuthenticatedVoter`, and the SpEL
`PreInvocationAuthorizationAdviceVoter` (so `@PreAuthorize` works).
`allowIfAllAbstainDecisions=false` is set: if every voter abstains
the call is denied.

`MethodSecurityConfig.accessDecisionManager()` returns this XML bean
verbatim via an `ObjectProvider` lookup - the legacy stack is
**100% bean-referenced from gsec**. Migrating the voters means
replacing the bean wiring on the Gemma side.

## 2. Config-attribute -> permission map (the contract)

| Attribute | Voter bean | Permissions required (OR'd) | Acts on |
|---|---|---|---|
| `ACL_SECURABLE_READ` | `securableReadVoter` (`AclEntryVoter`) | `ADMINISTRATION` OR `READ` | single argument |
| `ACL_SECURABLE_EDIT` | `securableEditVoter` (`AclEntryVoter`, transient-aware, grant-on-transient) | `ADMINISTRATION` OR `WRITE` | single argument |
| `ACL_SECURABLE_EDIT_IGNORE_TRANSIENT` | same bean, suffix branch | same | single argument; transient -> grant |
| `ACL_SECURABLE_COLLECTION_READ` | `securableCollectionReadVoter` (`AclEntryCollectionVoter`) | `ADMINISTRATION` OR `READ` | collection argument |
| `ACL_SECURABLE_COLLECTION_EDIT` | `securableCollectionEditVoter` (transient-aware) | `ADMINISTRATION` OR `WRITE` | collection argument |
| `ACL_SECURABLE_COLLECTION_EDIT_IGNORE_TRANSIENT` | same bean, suffix branch | same | collection; transient -> grant |
| `ACL_SECURABLE_MAP_READ` | `securableMapReadVoter` (`AclEntryMapVoter`) | `ADMINISTRATION` OR `READ` | map keys |
| `ACL_SECURABLE_MAP_EDIT` | `securableMapEditVoter` (transient-aware) | `ADMINISTRATION` OR `WRITE` | map keys |
| `ACL_SECURABLE_MAP_VALUE_READ` | `securableMapValueReadVoter` (`AclEntryMapValueVoter`) | `ADMINISTRATION` OR `READ` | map values |
| `ACL_SECURABLE_MAP_VALUE_EDIT` | `securableMapValueEditVoter` (transient-aware) | `ADMINISTRATION` OR `WRITE` | map values |

`ADMINISTRATION` always co-grants every other permission - the
voters always accept `ADMINISTRATION` alongside the requested
`READ` or `WRITE`. This is the canonical "ACL admin owns the
object, can do anything" rule; any SpEL rewrite has to preserve it
explicitly (`hasPermission(..., 'READ') or hasPermission(..., 'ADMINISTRATION')`)
or factor it into the `AclPermissionEvaluator` (it does not factor
it in today - the disjunction is currently expressed by passing
**both** permissions to the voter constructor).

## 3. Call-site inventory

Counted via:
```
grep -rhoE '"ACL_[A-Z_]+"' --include='*.java' \
  gemma-core gemma-rest gemma-web gemma-cli | sort | uniq -c | sort -rn
```

| Attribute | Count | Verdict |
|---|---|---|
| `ACL_SECURABLE_READ` | **176** | active, single-arg read gate |
| `ACL_SECURABLE_EDIT` | **87** | active, single-arg write gate |
| `ACL_SECURABLE_COLLECTION_READ` | **12** | active |
| `ACL_SECURABLE_COLLECTION_EDIT` | **2** | active, vanishingly rare |
| `ACL_SECURABLE_EDIT_IGNORE_TRANSIENT` | 1 | `SecurableBaseService` |
| `ACL_SECURABLE_COLLECTION_EDIT_IGNORE_TRANSIENT` | 1 | `SecurableBaseService` |
| `ACL_SECURABLE__READ` (double underscore) | 1 | **TYPO BUG** in `BioMaterialService.java:79` - this is the legacy form that doesn't match `supports()` on any voter; the call falls through to the `UnanimousBased.allowIfAllAbstainDecisions=false` rule and **always denies** unless another voter (auth / role) grants. Worth fixing in the Phase X.3 sweep. |
| `ACL_SECURABLE_MAP_READ` | 0 | **dead voter bean** - no call sites |
| `ACL_SECURABLE_MAP_EDIT` | 0 | **dead voter bean** |
| `ACL_SECURABLE_MAP_VALUE_READ` | 0 | **dead voter bean** |
| `ACL_SECURABLE_MAP_VALUE_EDIT` | 0 | **dead voter bean** |

**Total: 281** `ACL_*` call sites (not ~340 as estimated in the
Phase C recce). The eight wired voter beans include four
zero-usage beans (`securableMap{Read,Edit,Value{Read,Edit}}Voter`)
and the two `AclEntryMap*Voter` classes in gsec have **zero call
sites** across the entire Gemma codebase - they can be dropped
from the wiring (and arguably from gsec) at the same time as the
migration. The non-map call sites concentrate in the
`gemma-core/.../persistence/service/expression/experiment`
package: `SingleCellExpressionExperimentService` (87 attribute
references), `ExpressionExperimentService` (66),
`ArrayDesignService` (28), `ProcessedExpressionDataVectorService`
(19) account for ~71% of all call sites.

Top five most-used attributes overall (sanity-checked: these are
what the migration has to cover):

1. `IS_AUTHENTICATED_ANONYMOUSLY` - 380 (out of scope; this is the
   `AuthenticatedVoter`, not an ACL voter)
2. `ACL_SECURABLE_READ` - 176
3. `GROUP_USER` - 150 (out of scope; `roleHierarchyVoter`)
4. `GROUP_ADMIN` - 135 (out of scope; `roleHierarchyVoter`)
5. `ACL_SECURABLE_EDIT` - 87

Of the ACL voter family proper, the active set is
**278 call sites across 4 attributes** (`READ`, `EDIT`,
`COLLECTION_READ`, `COLLECTION_EDIT`) plus 2 `IGNORE_TRANSIENT`
variants plus 1 typo. The map and map-value voters are dead and
their attributes have zero callers.

## 4. Spring Security 6 target architecture

`@EnableMethodSecurity` is the modern method-security stack. It:

- Provides four built-in interceptors: `@PreAuthorize`,
  `@PostAuthorize`, `@PreFilter`, `@PostFilter`. SpEL is evaluated
  via the registered `MethodSecurityExpressionHandler` (gsec's
  `securityExpressionHandler` - same bean, same `AclPermissionEvaluator`,
  same `RoleHierarchy`).
- Has **no** `AccessDecisionManager` extension point. The equivalent
  is `AuthorizationManager<T>` - an interface that returns an
  `AuthorizationDecision` (granted / denied / abstain) given a
  `Supplier<Authentication>` and a target `T`. For method security
  the targets are `MethodInvocation` (pre-invocation) and
  `MethodInvocationResult` (post-invocation).
- Lets you register custom `AuthorizationManager` beans as advisors
  via `AuthorizationManagerBeforeMethodInterceptor.beforeMethodSafe(...)`
  (pre-invocation) or `...AfterMethodInterceptor` (post-invocation),
  pointcut by annotation or method matcher.
- Still supports `securedEnabled=true` for `@Secured` and
  `jsr250Enabled=true` for `@RolesAllowed`. **Importantly**, the
  `@Secured` interceptor in `@EnableMethodSecurity` mode only
  recognises `ROLE_`-prefixed authorities (well, the role prefix the
  app's `RoleHierarchy` is configured for - `GROUP_` in our case),
  not arbitrary `ACL_*` config-attribute strings. The legacy stack
  treated `@Secured` values as opaque `ConfigAttribute` strings that
  each voter could opt into via `supports()`; the new stack does
  not. **There is no way to teach `@EnableMethodSecurity`'s
  `@Secured` interceptor to dispatch to our ACL voters** - the
  attribute strings have to either move to `@PreAuthorize` SpEL or
  to a parallel custom annotation that we own.

This is the architectural reason the migration is a 278-call-site
sweep and not a config swap.

### 4a. Path A - annotation rewrite to `@PreAuthorize`

Direct SpEL translation:

```
@Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
public void update(MyEntity e) { ... }
```

becomes

```
@PreAuthorize("hasRole('USER') and (hasPermission(#e, 'WRITE') or hasPermission(#e, 'ADMINISTRATION'))")
public void update(MyEntity e) { ... }
```

The `hasPermission(Object, Object)` SpEL form dispatches to
`AclPermissionEvaluator` which does an `objectIdentityRetrievalStrategy`
lookup followed by `acl.isGranted(permissions, sids, false)` -
**identical** to the path the voter takes. The
`ADMINISTRATION` co-grant must be made explicit (the voter
constructor's two-permission array becomes a SpEL `or`).

Gotchas the recce surfaced:

1. **Argument naming**: SpEL needs parameter names. The current
   `@Secured` voters introspect the method-invocation arguments by
   type, not by name. To use `#paramName` we need `-parameters`
   compilation (or `@P` annotations on each parameter). Gemma's
   Maven build does **not** currently pass `-parameters`; this has
   to be enabled in the parent POM as a prerequisite. Without it
   the SpEL has to use `#root.args[0]` which is unreadable at scale.
2. **Collection / map arity**: `@PreAuthorize("@aclSpel.allHavePermission(#coll, 'READ')")`
   - SpEL has no concise "all elements" expression; we need a
   helper bean (e.g. `@aclSpel`) that does the collection /
   map-key / map-value loop. This is one helper class with five
   methods, callable from SpEL. The 14 collection/edit call sites
   plus the 0 map sites means this helper is needed regardless.
3. **`IGNORE_TRANSIENT` shortcut**: SpEL cannot express "skip the
   check if the object is transient" without another helper call -
   `@aclSpel.canEditAllowingTransient(#e)`. Three call sites, one
   helper method, trivial.
4. **The `ACL_SECURABLE__READ` typo** (BioMaterialService.java:79):
   silently always-denies today (covered by the
   `IS_AUTHENTICATED_ANONYMOUSLY` co-attribute on that same line,
   so the method actually grants anonymous read). The rewrite fixes
   the typo - and at the same time has to preserve the current
   "anonymous read works" behaviour (probably by dropping the ACL
   clause entirely, since the original intent appears to be "no
   ACL check, anyone can read" given the broken attribute string).
   Worth a dedicated commit in the sweep with a careful test.
5. **Argument-resolution differences**: the voters use the **first
   matching argument** (by `processDomainObjectClass`); `@PreAuthorize`
   needs the specific parameter. Most call sites take only one
   `Securable`, but a handful take two or more; the recce did not
   enumerate these but the sweep needs to.

### 4b. Path B - custom `AuthorizationManager` wrappers

For Phase X.1, we wrap each of the four voter classes in an
`AuthorizationManager<MethodInvocation>` adapter that delegates to
the existing voter's `vote()` method. The adapter:

```java
public class AclVoterAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    private final AccessDecisionVoter<MethodInvocation> voter;
    private final List<ConfigAttribute> attributes; // one per @Secured value

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, MethodInvocation mi) {
        int decision = voter.vote(authentication.get(), mi, attributes);
        if (decision == AccessDecisionVoter.ACCESS_GRANTED)  return new AuthorizationDecision(true);
        if (decision == AccessDecisionVoter.ACCESS_DENIED)   return new AuthorizationDecision(false);
        return null; // abstain
    }
}
```

Wired as a `BeforeMethodInterceptor` pointcut at `@Secured`
annotations whose value matches an `ACL_*` string. This lets
`@EnableMethodSecurity` and the legacy `@Secured("ACL_*")`
annotations co-exist during the sweep.

Cost: ~150 lines of glue (one adapter class, one configuration
bean that builds eight adapter instances - one per
config-attribute - and registers them as advisors). Zero call-site
changes.

### Verdict

**Use Path B for the cutover, then Path A for the sweep.** Path B
preserves all 278 call sites verbatim while replacing
`@EnableGlobalMethodSecurity` with `@EnableMethodSecurity`,
unblocking the AfterInvocation Phase C work and getting Gemma off
the deprecated `GlobalMethodSecurityConfiguration`. Path A is then
applied attribute-by-attribute as a normal codebase modernization,
with parallel running guaranteed by Path B for the duration.

Path A alone (direct annotation rewrite without the adapter) is
viable but means a 278-site big-bang in a single commit per
attribute family - too risky given the
`AclPermissionEvaluator`-vs-voter equivalence is currently
**asserted** but not proven by tests. The adapter path gives us a
"the new stack uses the same voter the old stack did" guarantee for
the cutover commit, and lets Path A be split into per-attribute
commits, each with its own test pass.

## 5. Phasing

Following the Phase C / AfterInvocation pattern:

### Phase X.1 - `AuthorizationManager` voter wrappers (parallel-run)
- Add `ubic.gemma.core.security.authorization.acl.AclVoterAuthorizationManager` (one class).
- Add a `MethodSecurityConfig`-adjacent `@Configuration` that creates one
  `AuthorizationManagerBeforeMethodInterceptor` per `ACL_*` config attribute,
  delegating to the existing voter bean. Pointcut: `@Secured` annotation
  whose value list contains the matching string.
- Switch `MethodSecurityConfig` from `@EnableGlobalMethodSecurity` to
  `@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)`.
- `securedEnabled = true` stays on because the codebase still uses
  `@Secured("RUN_AS_ADMIN")` (11 sites) and `@Secured("GROUP_*")` (~322 sites).
  The new stack's `@Secured` interceptor only handles role-style attributes; the
  ACL strings are handled by the adapter advisors registered above.
- Delete `accessDecisionManager()`, `afterInvocationManager()`, `runAsManager()`
  overrides only after **AfterInvocation Phase C** has landed (it owns the
  after-invocation half of the transition). The pre-invocation
  `accessDecisionManager()` override goes away in this phase **only if**
  Phase C has already landed - otherwise it stays as a no-op pointing at
  a now-unused bean while the after-invocation bridge keeps it alive.
- Drop the four dead voter beans (`securableMap*Voter`) and their
  config-attribute strings - they have **zero call sites** and ship as
  pure dead code today.
- Tests: green `gemma-core` + `gemma-rest` + `gemma-web` test suites with
  the new stack active and the adapter wrappers handling all 278 sites
  unchanged.

### Phase X.2 - SpEL helper bean + `-parameters` build flag
- Add `-parameters` to the maven-compiler-plugin in the parent POM.
- Add `ubic.gemma.core.security.AclSecuritySpel` `@Component`:
  - `boolean canEdit(Object subject)` - voter-equivalent for `EDIT`
  - `boolean canRead(Object subject)` - voter-equivalent for `READ`
  - `boolean canEditAllowingTransient(Object subject)`
  - `boolean canReadAll(Collection<?> coll)` / `canEditAll(Collection<?> coll)`
- Register as `@aclSec` via the SpEL `BeanResolver`.
- No call-site changes yet.

### Phase X.3 - `@PreAuthorize` sweep (the long pole)
- Per-attribute commits:
  - `ACL_SECURABLE_READ` -> `@PreAuthorize("@aclSec.canRead(#param)")` (or
    direct `hasPermission(#param, 'READ') or hasPermission(#param, 'ADMINISTRATION')`)
    - 176 sites, ~6 dev-days at 30/day with test pass per commit batch.
  - `ACL_SECURABLE_EDIT` -> `@aclSec.canEdit(#param)` - 87 sites, ~3 dev-days.
  - `ACL_SECURABLE_COLLECTION_READ` / `_EDIT` - 14 sites, ~1 dev-day.
  - `_IGNORE_TRANSIENT` variants - 2 sites, trivial.
  - `ACL_SECURABLE__READ` typo fix - 1 site, dedicated commit with test.
- Each commit can land independently; the adapter wrappers from Phase X.1
  keep the un-swept sites working throughout.
- Lock-step Q: what to do about call sites that mix `@Secured` and `@PreAuthorize`?
  The new stack runs **both** interceptors and ANDs them. This is
  semantically equivalent to the old `UnanimousBased` decision manager so
  no logic change is needed, but the sweep can naturally fold a
  `@Secured({"GROUP_USER", "ACL_SECURABLE_EDIT"})` into a single
  `@PreAuthorize("hasRole('USER') and @aclSec.canEdit(#e)")` rather than
  splitting the role and ACL clauses.

### Phase X.4 - delete the adapter wrappers
- Once Phase X.3 has swept every `ACL_*` call site, the
  `AclVoterAuthorizationManager` advisors have zero pointcut matches.
- Delete the adapter class, the configuration bean, the eight voter
  beans in gsec XML, and the four `AclEntry*Voter` classes from gsec
  itself.
- The `accessDecisionManager` XML bean can be deleted from gsec at
  this point too - nothing references it anymore.

### Phase X.5 - drop `securedEnabled`
- Out of scope for this plan, listed for completeness. Requires
  `RUN_AS_ADMIN` (11 sites) to have been retired - that is a Phase D
  concern in the AfterInvocation Phase C recce.

## 6. Risk callouts

1. **`AclPermissionEvaluator` vs voter equivalence is asserted, not
   proven.** The `MethodSecurityConfig` comment block flags a
   suspected ACL-evaluation bug on the admin test path. Until that
   bug is closed, a direct SpEL rewrite (Path A) could regress
   behaviour. The adapter (Path B) sidesteps this by keeping the
   exact same voter code in play.
2. **`-parameters` compile flag**: required for readable SpEL.
   Switching it on can surface Spring proxy / AOP issues that were
   previously latent. Worth a dedicated commit before Phase X.3
   starts.
3. **`@Secured` interpretation drift**: the new stack's `@Secured`
   interceptor evaluates the value-array as an OR (any role grants)
   and only handles role-style strings. The legacy
   `UnanimousBased` behaviour was AND across voters but per-voter
   per-attribute. The adapter wrappers must replicate per-attribute
   semantics or we'll silently flip OR -> AND boundaries on
   multi-attribute annotations. The adapter advisor pointcut must
   trigger **once per ACL config-attribute on the annotation**, not
   once per annotation; the test pass after Phase X.1 must cover a
   `@Secured({"ACL_SECURABLE_READ", "ACL_SECURABLE_EDIT"})`-style
   site (if any exist) to prove this.
4. **Transient-entity edits**: the `_IGNORE_TRANSIENT` shortcut is
   load-bearing for `SecurableBaseService.create` flows where the
   ACL row doesn't exist yet. The SpEL helper has to call
   `ObjectTransientnessRetrievalStrategy` and skip the ACL check
   for transient instances - if the helper doesn't, save operations
   start failing with `AccessDenied` at the entity-creation step.
5. **The `ACL_SECURABLE__READ` typo**: probably a latent bug. The
   sweep needs a dedicated test that proves the current behaviour
   (anonymous read of `BioMaterialService.find...`) and the new
   `@PreAuthorize` clause that replaces it.
6. **gsec dependency**: dropping the four `AclEntry*Voter` classes
   from gsec is fine for Gemma but gsec is a library; if anything
   else depends on it those callers need a heads-up. The gsec
   `applicationContext-gsec.xml` voter beans are the primary
   consumers and they all live in Gemma, so the blast radius is
   small but non-zero.

## 7. Estimated effort

| Phase | Scope | Effort |
|---|---|---|
| X.1 | Adapter class, config, switch to `@EnableMethodSecurity`, drop dead map voters | **3-4 dev-days** |
| X.2 | `-parameters` flag, `AclSecuritySpel` helper, no call-site changes | **2 dev-days** |
| X.3 | 278 call-site sweep across 4 attribute families + edge cases | **12-15 dev-days** (paced as one PR per attribute family + one for `IGNORE_TRANSIENT` + one for the typo) |
| X.4 | Delete adapters, delete voter classes from gsec, delete dead XML beans | **1-2 dev-days** |
| **Total** | | **18-23 dev-days** (slightly below the 20-30 day estimate in the prompt, because the actual call-site count is 278 not ~340 and four of the eight voter beans are dead code) |

## 8. Open questions for a follow-up session

- Should we ship Path A (`hasPermission` direct) or Path A' (`@aclSec`
  helper) as the long-term form? The helper hides the
  `ADMINISTRATION`-co-grant logic, which is arguably the right
  abstraction, but it adds a layer between the annotation and the
  ACL evaluator that future readers will have to follow.
- Is there an appetite to **fix the `AclPermissionEvaluator`
  admin-path bug** (`MethodSecurityConfig` comment lines 56-63)
  before Phase X.3? Fixing it removes the only behavioural reason
  to prefer Path B's adapter over a direct Path A rewrite.
- Should the AfterInvocation Phase C and this Phase X.1 land as one
  commit or two? They both touch `MethodSecurityConfig`; landing
  them together avoids a no-op intermediate state where one half
  of the method-security stack is modern and the other is bridged.
