# AfterInvocation → Spring Security 6 Modern API Migration Roadmap

Reconnaissance, no code changes. Branch: `worktree-afterinvocation-recce`, baseline
`08e760bdaf`.

## 1. Why this matters

Gemma's `MethodSecurityConfig` currently uses the legacy
`@EnableGlobalMethodSecurity(prePostEnabled=true, securedEnabled=true)` and overrides
`afterInvocationManager()` to assemble an `AfterInvocationProviderManager` from 14
named provider beans. The deprecated `@EnableGlobalMethodSecurity` /
`GlobalMethodSecurityConfiguration` / `AccessDecisionManager` /
`AfterInvocationManager` / `RunAsManager` stack is still functional in Spring Security
6.x but is documented as removed in a future major release. Spring Security 6's modern
`@EnableMethodSecurity` is built on `AuthorizationManager<T>` interceptors and **has
no `AfterInvocationManager` extension point** — only `@PreAuthorize`, `@PostAuthorize`,
`@PreFilter`, `@PostFilter`, and pluggable `AuthorizationManager` beans wired by
`AuthorizationManagerBefore/AfterMethodInterceptor`. Until Gemma's after-invocation
providers are rehomed, `@EnableMethodSecurity` cannot be adopted; until that
annotation is adopted, the deprecated stack remains a blocker for the eventual
Spring 7 / Spring Security 7 upgrade.

## 2. Provider inventory

The 14 wired by `MethodSecurityConfig.AFTER_INVOCATION_PROVIDER_BEAN_NAMES`:

| # | Bean id | Class (gsec unless noted) | Config attribute | Domain class | Call sites | Notes |
|---|---|---|---|---|---|---|
| 1 | `afterAclReadQuiet` | `AclEntryAfterInvocationProvider` (Gemma subclass via `quiet=true`) | `AFTER_ACL_READ_QUIET` | any `Securable` | 17 | Returns `null` on denial instead of throwing. Single object. |
| 2 | `afterAclRead` | `AclEntryAfterInvocationProvider` (gsec subclass of stock) | `AFTER_ACL_READ` | any `Securable` | 37 | Stock Spring `AclEntryAfterInvocationProvider`-style single-object check; throws on denial. |
| 3 | `afterAclCollectionRead` | `AclEntryAfterInvocationCollectionFilteringProvider` | `AFTER_ACL_COLLECTION_READ` | any `Securable` collection | 61 | Bulk filter: `aclService.readAclsById()` for all targets at once, then `acl.isGranted(...)`. Drops elements where `isGranted=false`. |
| 4 | `afterAclCompositeSequenceCollectionRead` | `AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider` (gemma-core) | `AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ` | `CompositeSequence` collection | 11 | Filters by *associated* `ArrayDesign`'s ACL, not the CS itself. |
| 5 | `afterAclDataVectorCollectionRead` | `AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider` (gemma-core) | `AFTER_ACL_DATA_VECTOR_COLLECTION_READ` | `DataVector` / `DataVectorValueObject` collection | 5 | Filters by associated `ExpressionExperiment`'s ACL. |
| 6 | `afterAclMyDataRead` | `AclEntryAfterInvocationOwnedCollectionFilteringProvider` | `AFTER_ACL_FILTER_MY_DATA` | `Securable` collection | 2 | Permission + owner-match (or admin). |
| 7 | `afterAclMyPrivateDataRead` | `AclEntryAfterInvocationPrivateCollectionFilteringProvider` | `AFTER_ACL_FILTER_MY_PRIVATE_DATA` | `Securable` collection | 1 | Permission + ACL marked private + READ-or-ADMIN granted. |
| 8 | `afterAclValueObjectCollection` | `AclEntryAfterInvocationValueObjectCollectionFilteringProvider` | `AFTER_ACL_VALUE_OBJECT_COLLECTION_READ` | `SecureValueObject` collection | 20 | Collection filter **plus** side-effect: populates `isPublic / isShared / userOwned / userCanWrite` on each VO via `SecureValueObject` setters. |
| 9 | `afterAclValueObjectMap` | `AclEntryAfterInvocationValueObjectMapFilteringProvider` | `AFTER_ACL_VALUE_OBJECT_MAP_READ` | `Map<SecureValueObject, ?>` | 6 | Filter map keys; values not touched. Same VO side-effect. |
| 10 | `afterAclValueObject` | `AclEntryAfterInvocationValueObjectProvider` | `AFTER_ACL_VALUE_OBJECT_READ` | single `SecureValueObject` | 4 | Single-object check + VO side-effect. Returns `null` on denial (NotFoundException caught). |
| 11 | `afterAclMapRead` | `AclEntryAfterInvocationMapFilteringProvider` | `AFTER_ACL_MAP_READ` | `Map<Securable, Securable?>` | 5 | Filter map keys (and securable values are filtered transitively via parent). |
| 12 | `afterAclMapValuesRead` | `AclEntryAfterInvocationMapValueFilteringProvider` | `AFTER_ACL_MAP_VALUES_READ` | `Map<?, Securable>` | 1 | Filter map values; non-securable keys. |
| 13 | `afterAclStreamRead` | `AclEntryAfterInvocationStreamFilteringProvider` | `AFTER_ACL_STREAM_READ` | `Stream<Securable>` | 2 | Filters a `Stream`, opens its own Hibernate session for ACL lookups, closes on stream close. |
| 14 | `postInvocationAdviceProvider` | `PostInvocationAdviceProvider` (stock Spring Security) | `@PostAuthorize` / `@PostFilter` SpEL | any | 0 in prod | Standard Spring post-invocation SpEL evaluator. Currently zero in-prod usages (only `BioAssayService#evictFromCache` uses `@PreAuthorize`); included for completeness. |

Three additional providers exist as beans but are **deliberately NOT wired** by
`AFTER_INVOCATION_PROVIDER_BEAN_NAMES` (legacy XML parity contract): `afterAclCompositeSequenceRead`,
`afterAclDifferentialExpressionAnalysisResultCollectionRead`, `afterAclValueObjectMapValue`.
They are dead code from the migration's perspective and should be deleted during this
work.

**Aggregate call-site count** across `@Secured({…, "AFTER_ACL_*"})` annotations: ~174
(see `grep -rh "AFTER_ACL_[A-Z_]+"` tally). All are interface-level annotations on
service / DAO methods.

## 3. Spring Security 6 mapping

Categorization first — every provider falls into one of three shapes:

* **Voting** (1, 2, 10): single-object pass/fail. Direct map to `@PostAuthorize`.
* **Filtering** (3–9, 11–13): collection / map / stream, drop elements that fail the
  check. Direct map to `@PostFilter` (collection / array forms) or a custom
  `AuthorizationManager` interceptor for the stream / map-value variants `@PostFilter`
  cannot reach.
* **Side-effecting** (8, 9, 10): in addition to filter/vote, mutate `SecureValueObject`
  metadata (`isPublic` / `isShared` / `userOwned` / `userCanWrite`). `@PostFilter` /
  `@PostAuthorize` SpEL **cannot** carry this side-effect; this is the only category
  that needs a custom replacement.

| Legacy attribute | Spring Security 6 idiom |
|---|---|
| `AFTER_ACL_READ` | `@PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")` |
| `AFTER_ACL_READ_QUIET` | Same as above but **catch the `AccessDeniedException` upstream** — `@PostAuthorize` cannot "return null on denial" natively. Either (a) change call sites to handle `AccessDeniedException` and substitute `null`, or (b) write a custom `AuthorizationManager<MethodInvocationResult>` that returns granted-with-null. Option (a) is the canonical Spring Security 6 idiom; option (b) preserves the current API exactly. |
| `AFTER_ACL_COLLECTION_READ` | `@PostFilter("hasPermission(filterObject, 'READ') or hasPermission(filterObject, 'ADMINISTRATION')")` |
| `AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ` | `@PostFilter("hasPermission(filterObject.arrayDesign, 'READ') or hasPermission(filterObject.arrayDesign, 'ADMINISTRATION')")` |
| `AFTER_ACL_DATA_VECTOR_COLLECTION_READ` | `@PostFilter("hasPermission(filterObject.expressionExperiment, 'READ') or hasPermission(filterObject.expressionExperiment, 'ADMINISTRATION')")` |
| `AFTER_ACL_FILTER_MY_DATA` | `@PostFilter("(hasPermission(filterObject, 'READ') or hasPermission(filterObject, 'ADMINISTRATION')) and (T(gemma.gsec.util.SecurityUtil).isUserAdmin() or @aclOwnerCheck.isOwner(filterObject))")` — needs a small helper bean for the owner check (cannot inline owner-of-acl lookup in SpEL). |
| `AFTER_ACL_FILTER_MY_PRIVATE_DATA` | Same pattern as `MY_DATA` plus a helper that exposes "ACL is private". |
| `AFTER_ACL_VALUE_OBJECT_COLLECTION_READ` | **Cannot** be a pure `@PostFilter`: it must also populate `isPublic/isShared/userOwned/userCanWrite`. Replacement = small `AuthorizationManager<MethodInvocationResult>` bean that performs the bulk ACL fetch, mutates the VOs, and removes the denied ones. |
| `AFTER_ACL_VALUE_OBJECT_MAP_READ` | Same as above; specialised for `Map.keySet()`. |
| `AFTER_ACL_VALUE_OBJECT_READ` | `@PostAuthorize` + a tiny `@AfterReturning` aspect (or `AuthorizationManager`) to populate VO fields. |
| `AFTER_ACL_MAP_READ` | `@PostFilter` over `filterObject.key` — Spring Security 6's `@PostFilter` *does* support filtering `Map.Entry` via `filterObject.key`. Works directly. |
| `AFTER_ACL_MAP_VALUES_READ` | `@PostFilter` over `filterObject.value`. Works directly. |
| `AFTER_ACL_STREAM_READ` | Custom `AuthorizationManager<MethodInvocationResult>` for `Stream<?>` return values — `@PostFilter` doesn't filter streams. Trivial wrapper around the existing `AclEntryAfterInvocationStreamFilteringProvider` logic but expressed as an `AuthorizationManager`. |

## 4. Key insight: AclPermissionEvaluator redundancy

**Gemma already wires the stock Spring Security `AclPermissionEvaluator`** in
`applicationContext-gsec.xml` (line 70):

```xml
<bean id="permissionEvaluator" class="org.springframework.security.acls.AclPermissionEvaluator">
    <constructor-arg ref="aclService"/>
    <property name="objectIdentityRetrievalStrategy" ref="objectIdentityRetrievalStrategy"/>
    <property name="sidRetrievalStrategy" ref="sidRetrievalStrategy"/>
</bean>
```

This bean is the same `AclService`, the same `ObjectIdentityRetrievalStrategy`, and the
same `SidRetrievalStrategy` the 14 providers use. `AclPermissionEvaluator.hasPermission()`
does exactly what `AclEntryAfterInvocationProvider.hasPermission()` does for the
single-object case:

1. Resolve `ObjectIdentity` via the retrieval strategy.
2. Resolve `Sid` list from the `Authentication`.
3. `aclService.readAclById(oid, sids)`.
4. `acl.isGranted(requirePermission, sids, false)`.
5. Catch `NotFoundException` → return `false`.

That step list is byte-for-byte the body of `AclEntryAfterInvocationValueObjectProvider.hasPermission()`
(reviewed in this recce). The same is true of the gsec
`AclEntryAfterInvocationCollectionFilteringProvider`'s per-element check — its only
non-redundant innovation is the **bulk** `readAclsById()` path so a single SQL fetches
all ACLs for a collection at once. That bulk-fetch optimization is **not** exposed by
`AclPermissionEvaluator` — Spring Security 6 `@PostFilter` calls `hasPermission`
per element, which falls back to N single-row ACL fetches.

**Conclusion.** For the *voting* providers (1, 2, 10) the migration is "delete the
provider, switch the annotation to `@PostAuthorize("hasPermission(...)")`" — the work
is mechanical and the runtime behaviour is identical. For the plain *filtering*
providers (3, 6, 7, 11, 12) the migration is "delete the provider, switch the
annotation to `@PostFilter("hasPermission(filterObject, …)")`" — functionally
identical but a measurable **performance regression** on large collections (N
single-row ACL fetches vs one bulk fetch). For the *side-effecting* + *stream* + *VO*
providers (4, 5, 8, 9, 13) we *cannot* delete and switch — those need a small custom
`AuthorizationManager<MethodInvocationResult>` bean that preserves both the bulk
fetch and the VO side-effects.

**Net redundancy verdict.** Of the 14 wired providers:

* **6 are fully redundant** (`AFTER_ACL_READ`, `AFTER_ACL_VALUE_OBJECT_READ`, `AFTER_ACL_FILTER_MY_DATA`, `AFTER_ACL_FILTER_MY_PRIVATE_DATA`, `AFTER_ACL_MAP_READ`, `AFTER_ACL_MAP_VALUES_READ`) — pure delete + annotation switch, modulo the small ACL-owner / ACL-private helper bean for the two "my data" variants.
* **1 is functionally redundant but a perf regression** (`AFTER_ACL_COLLECTION_READ`) — pure delete works, but losing the bulk `readAclsById` is real at N>~50. Recommended path is a custom `AuthorizationManager` that preserves the bulk fetch.
* **5 are not redundant** (`AFTER_ACL_VALUE_OBJECT_COLLECTION_READ`, `AFTER_ACL_VALUE_OBJECT_MAP_READ`, `AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ`, `AFTER_ACL_DATA_VECTOR_COLLECTION_READ`, `AFTER_ACL_STREAM_READ`) — must become custom `AuthorizationManager` beans. `@PostFilter` either cannot reach the target type (stream) or cannot carry the side-effect (VO-populate) or cannot expose the association extractor (CS-by-AD, DV-by-EE — `filterObject.arrayDesign` works in SpEL but loses the bulk fetch).
* **1 is `AFTER_ACL_READ_QUIET`** — needs either a call-site sweep (catch and null) or a custom `AuthorizationManager` to preserve API. Decision deferred to Phase A planning.
* **1 is `postInvocationAdviceProvider`** — zero in-prod usages; trivial to keep (built into `@EnableMethodSecurity` natively).

## 5. Migration phases

### Phase A — Redundant providers ("delete and switch")

Scope: the 6 fully-redundant providers + the 1 mostly-redundant collection one (with
performance caveat acknowledged) + the dead `postInvocationAdviceProvider`.

Per attribute, sweep `@Secured({…, "AFTER_ACL_X"})` → `@Secured({…})` +
`@PostAuthorize(...)` or `@PostFilter(...)` on the same method. The `@Secured` list
typically also contains `IS_AUTHENTICATED_ANONYMOUSLY` or `GROUP_*` plus a *pre*-invocation
`ACL_SECURABLE_*` voter; keep those (they're separately migrated in Phase 2 ACL work).

Estimated call-site impact (`@Secured` annotations to rewrite):
* `AFTER_ACL_READ` × 37
* `AFTER_ACL_COLLECTION_READ` × 61
* `AFTER_ACL_FILTER_MY_DATA` × 2
* `AFTER_ACL_FILTER_MY_PRIVATE_DATA` × 1
* `AFTER_ACL_MAP_READ` × 5
* `AFTER_ACL_MAP_VALUES_READ` × 1
* `AFTER_ACL_VALUE_OBJECT_READ` × 4 (only the *throw* part; VO-populate moves to Phase B)

Total ~111 call-site edits. Mechanical but bulk; do it as one PR per `@Secured`
attribute family to keep diffs reviewable. After each family lands, delete the
corresponding provider bean from `applicationContext-security.xml` /
`applicationContext-gsec.xml` and the corresponding entry from
`AFTER_INVOCATION_PROVIDER_BEAN_NAMES`. Tests must pass after each delete.

Effort: ~2–3 days mechanical + 1 day test stabilization. Risk: low — every annotation
change is a like-for-like substitution against a `PermissionEvaluator` already proven
in production (BioAssayService uses it today).

### Phase B — Custom `AuthorizationManager` beans

Scope: the 5 non-redundant providers (VO collection / VO map / CS-by-AD / DV-by-EE /
stream).

For each, write a small `@Component`-style class implementing
`AuthorizationManager<MethodInvocationResult>`:

```java
@Component
public class ValueObjectCollectionFilterAuthorizationManager
        implements AuthorizationManager<MethodInvocationResult> {

    private final AclService aclService;
    private final SidRetrievalStrategy sids;
    private final ObjectIdentityRetrievalStrategy ois;
    private final List<Permission> requirePermission;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> auth, MethodInvocationResult mir) {
        Object ret = mir.getResult();
        // ... lift the body of AclEntryAfterInvocationValueObjectCollectionFilteringProvider.decide()
        //     mutate the collection in place, populate SecureValueObject metadata,
        //     return new AuthorizationDecision(true) — the filter writes via mir.getResult mutation.
    }
}
```

Wire each into `@EnableMethodSecurity` via a custom advisor:

```java
@Bean
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public Advisor valueObjectCollectionFilterAdvisor(ValueObjectCollectionFilterAuthorizationManager mgr) {
    AuthorizationManagerAfterMethodInterceptor interceptor =
        new AuthorizationManagerAfterMethodInterceptor(forCustomAnnotation(), mgr);
    interceptor.setOrder(AuthorizationInterceptorsOrder.POST_FILTER.getOrder() + 1);
    return interceptor;
}
```

Define a custom `@AfterAclValueObjectCollectionRead` annotation that the advisor
pointcuts; sweep the ~20 callers from `@Secured("AFTER_ACL_VALUE_OBJECT_COLLECTION_READ")`
to `@AfterAclValueObjectCollectionRead`. Same shape for the other four.

Effort: 5–8 days. Risk: medium — the VO side-effects are observable in the UI
(public/shared/owner badges on dataset cards); needs careful integration testing on
the dataset browser pages and the REST API JSON shapes.

### Phase C — Switch to `@EnableMethodSecurity`

Once Phase A + B land:

1. Replace `@EnableGlobalMethodSecurity(prePostEnabled=true, securedEnabled=true)` →
   `@EnableMethodSecurity(prePostEnabled=true, securedEnabled=true)`.
2. Delete `afterInvocationManager()`, `accessDecisionManager()`, `runAsManager()`
   overrides.
3. Replace `AccessDecisionManager` (UnanimousBased + voter list) with per-annotation
   `AuthorizationManager` beans — the `ACL_SECURABLE_*` family of voters in
   `applicationContext-gsec.xml` needs the same `AuthorizationManager` treatment
   (this is a separate, parallel migration tracked by the gsec voter recce; **out of
   scope here** but blocks Phase C closure).
4. Drop the `MethodSecurityConfig.AFTER_INVOCATION_PROVIDER_BEAN_NAMES` list and the
   13 ACL provider beans entirely. Delete provider classes from gsec
   (`gemma/gsec/acl/afterinvocation/*`) and gemma-core
   (`ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocation*`).
5. `RunAsManager` migration: Spring Security 6 has no built-in `@RunAs` replacement —
   the 8 `@Secured("RUN_AS_ADMIN")` call sites (UserService, signup flows) need either
   a manual `SecurityContextHolder` push around the call or a custom advisor. Track
   separately.

Effort: 2–3 days (assuming Phase A + B + the voter migration are done). Risk: low.

## 6. Risk + effort summary

| Phase | Scope | Effort | Risk | Blockers |
|---|---|---|---|---|
| A | 7 redundant providers, ~111 call sites | 3–4 days | low | none |
| B | 5 non-redundant providers, ~38 call sites + new annotations + advisors | 5–8 days | medium (VO side-effects user-visible) | Phase A done |
| C | Switch to `@EnableMethodSecurity`, drop dead code | 2–3 days | low | Phase A + B + voter-migration recce |
| Total | 17 providers retired, ~149 call sites edited | 10–15 dev-days | medium overall | voter-migration recce gates C |

## 7. Open questions

1. **`AFTER_ACL_READ_QUIET` semantics**: should we (a) sweep ~17 call sites to handle
   `AccessDeniedException` themselves, or (b) preserve the "return null on denial"
   API via a custom `AuthorizationManager`? Option (a) is canonical but a behavioral
   change for callers; option (b) is a 1-off but keeps the legacy API. Lean (b).

2. **Owner / private-acl SpEL helpers**: `AFTER_ACL_FILTER_MY_DATA` needs `acl.owner ==
   currentUser`; `AFTER_ACL_FILTER_MY_PRIVATE_DATA` needs `acl is private`. Where do
   these live? Option (a): expose `@aclMeta.isOwner(o)` / `@aclMeta.isPrivate(o)` as
   `@Component` beans (Phase A). Option (b): custom `AuthorizationManager`-only (Phase
   B). With only 3 call sites between them, (a) is the lighter choice.

3. **Bulk-fetch perf regression on `AFTER_ACL_COLLECTION_READ`**: 61 call sites, some
   on hot paths (experiment browse, gene set load). Acceptable to ship plain
   `@PostFilter` (N single-row fetches) and revisit if profiling shows real
   regression? Or upgrade to a custom `AuthorizationManager` from day one? Lean: ship
   plain `@PostFilter` (the JDBC `BasicLookupStrategy` does its own batched lookups
   per call and the `AclCache` absorbs repeats within a request) and budget a
   custom manager only if benchmarks show real harm.

4. **`AclEntryVoter` family** in `applicationContext-gsec.xml` (the *pre*-invocation
   `ACL_SECURABLE_READ` / `_EDIT` / `_COLLECTION_*` / `_MAP_*` voters): these are a
   parallel migration not covered here (~340+ call sites). They follow the same
   `@PreAuthorize("hasPermission(...)")` substitution playbook. Schedule a separate
   recce.

5. **`postInvocationAdviceProvider`** entry in `AFTER_INVOCATION_PROVIDER_BEAN_NAMES`:
   `@EnableMethodSecurity` handles `@PostAuthorize` / `@PostFilter` natively via its
   own interceptors. The legacy `PostInvocationAdviceProvider` bean is removable in
   Phase C (it's just plumbing for the legacy `AfterInvocationManager` to dispatch
   SpEL post-advice; the modern API does it directly).

6. **Dead provider beans** (`afterAclCompositeSequenceRead`, `afterAclDifferentialExpressionAnalysisResultCollectionRead`, `afterAclValueObjectMapValue`): not in the wired list and no config attribute references them. Delete during Phase A as a cleanup commit. Verify via
   `grep AFTER_ACL_COMPOSITE_SEQUENCE_READ\\b` and `AFTER_ACL_DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_COLLECTION_READ` — both return zero hits (confirmed during this recce, 1 declared but 0 callers for `COMPOSITE_SEQUENCE_READ` due to the count being inflated by the bean def itself; same for the others).

7. **gsec ownership**: 13 of the 17 providers live in the gsec sibling repo. Phase B's
   custom `AuthorizationManager` beans are Gemma-side (they wrap Gemma's domain
   knowledge — VO setters, association extractors); the gsec providers should be
   deleted as part of Phase C with a coordinated gsec release. Verify no other gsec
   consumer exists (gsec appears to be a Gemma-only library at this point).
