# Spring Security 7 Readiness Recce

Reconnaissance only, no code changes. Branch:
`worktree-spring-security-7-recce`, baseline `08e760bdaf`.

Companion documents (all on unmerged worktree branches off
`phase2-acl-migrate`):

- `AFTER_INVOCATION_MIGRATION.md` — Phase A/B/C roadmap for the
  after-invocation stack (`worktree-afterinvocation-recce`).
- `AFTER_INVOCATION_PHASE_C_PLAN.md` — `@EnableMethodSecurity` cutover
  plan (`worktree-afterinv-phase-c-prep`, commit `c587f38640`).
- `ACLENTRYVOTER_MIGRATION.md` — gsec pre-invocation ACL voter
  migration plan (`worktree-aclentryvoter-recce`, commit
  `56f71171ab`); 281 `ACL_SECURABLE_*` call sites.
- The in-flight code branches: `worktree-afterinv-phase-a` (landed
  Phase A on the recce branch), `worktree-afterinv-phase-b-quiet`,
  `worktree-afterinv-phase-b-cs-dv`, `worktree-afterinv-phase-b-vo`,
  `worktree-secured-prauthorize`, `worktree-rest-security-config`.

This document **does not** execute anything. It inventories what
Gemma still depends on that Spring Security 7 (target mid-2026)
removes, cross-references the existing migration pipeline, and
flags the gaps.

## 1. Spring Security 7 target and scope

Spring Security 7.0 GA is currently targeted at mid-2026. Per the
SS6.x deprecation javadoc plus the SS7 milestone notes, the
following SS6-deprecated machinery is **slated for removal** in 7.0:

| Removed in SS7 | Replacement in SS6+ |
|---|---|
| `AccessDecisionManager` interface | `AuthorizationManager<T>` |
| `AffirmativeBased`, `ConsensusBased`, `UnanimousBased` | `AuthorizationManagers.allOf` / `anyOf` / built-in composition |
| `AccessDecisionVoter` family (`RoleVoter`, `RoleHierarchyVoter`, `AuthenticatedVoter`, `WebExpressionVoter`) | per-feature `AuthorizationManager`s |
| `AfterInvocationProvider` / `AfterInvocationManager` / `AfterInvocationProviderManager` | `AuthorizationManager<MethodInvocationResult>` + `AuthorizationManagerAfterMethodInterceptor` |
| `RunAsManager` / `RunAsImplAuthenticationProvider` / `@Secured("RUN_AS_*")` | no drop-in equivalent; programmatic `SecurityContext` swap is the canonical workaround |
| `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity` |
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` bean |
| `authorizeRequests()` / `antMatchers()` DSL methods | `authorizeHttpRequests()` / `requestMatchers()` |
| `@Secured` interceptor's voter-driven dispatch (i.e. anything other than role-style attributes) | `@PreAuthorize` SpEL, or custom `AuthorizationManager` advisors |

`@Secured` itself is **not** removed — but its SS7 interceptor only
dispatches role-style attributes (the ones an `AuthorizationManager`
built from the registered authorities can decide on). Voter-driven
attributes (`ACL_SECURABLE_*`, `AFTER_ACL_*`, `RUN_AS_*`) stop
working when `AccessDecisionManager` and `AfterInvocationManager`
go away unless a parallel `AuthorizationManager` advisor is wired
to handle them.

## 2. Gemma inventory (counts at baseline `08e760bdaf`)

### 2a. `AccessDecisionManager` family

- 1 production XML wiring of `AffirmativeBased` as `httpAccessDecisionManager`
  (`gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml:9-18`,
  with `WebExpressionVoter`, `RoleHierarchyVoter` reference, and
  `AuthenticatedVoter` voters inside).
- 2 production XML `<s:http access-decision-manager-ref="httpAccessDecisionManager">`
  uses (`/rest/v2/**` chain and `/**` chain in the same file).
- 1 production XML `RoleHierarchyVoter` reference (line 14, same file).
- `UnanimousBased` method-security `accessDecisionManager` is wired
  by `MethodSecurityConfig.accessDecisionManager()` via gsec's voter
  list — not visible to grep at `UnanimousBased\b` (gsec hides it),
  but is present as the bean returned to
  `GlobalMethodSecurityConfiguration`.
- 5 production Java fields/lookups of `AccessDecisionManager`:
  - `gemma-rest/.../DatasetsWebService.java:222,3082` (one field,
    one `.decide()` call inside `getDatasetsCategoryTermsAsync` /
    similar admin-gated endpoints).
  - `gemma-rest/.../PlatformsWebService.java:87,318` (mirror of the
    above on the platforms endpoint).
  - `gemma-rest/.../providers/CacheControlHeaderDecorator.java:31,54`
    (cache-control header writer that consults the http ADM to decide
    whether a response is admin-only).
  - `gemma-web/.../ExceptionTag.java:113-116` (JSP tag that consults
    the http ADM for permission gating).
- 1 production Java field on `AbstractFilteringVoEnabledService`
  (`gemma-core/.../persistence/service/AbstractFilteringVoEnabledService.java:29`)
  — autowired but checked for usage on a follow-up; appears unused in
  the read paths but is held in the base class.
- 7 test-only mocks (`AnnotationsWebServiceTest`, `DatasetsWebServiceTest`,
  `OpenApiTest`, `FactorValueServiceTest`, `ExpressionExperimentServiceTest`,
  `ExpressionExperimentServiceImplTest`, et al). Test fixtures move
  with the production cutover; counted but not load-bearing.

`AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY` constant is also
referenced as a **string-literal source** in 4 places
(`UserManagerImpl`, `SecurityUtils`, two test classes). The constant
goes away with `AuthenticatedVoter`; the literal `"IS_AUTHENTICATED_ANONYMOUSLY"`
itself is interpreted by the gsec / Gemma anonymous-auth plumbing,
not by `AuthenticatedVoter`, so the SS7 impact is limited to the
constant import (5-minute fix per file).

### 2b. `AfterInvocation` family

- 1 production wiring (`MethodSecurityConfig.afterInvocationManager()`)
  that instantiates `AfterInvocationProviderManager` and feeds it
  ten provider beans (post-Phase-B; nine Gemma-owned, one
  `postInvocationAdviceProvider` stock).
- 14 `AfterInvocationProvider` implementations at baseline (subject
  to Phase A/B reductions); the `AclEntryAfterInvocation*` family in
  `gemma-core/.../security/authorization/acl/` is the bulk.
- ~149 `@Secured("AFTER_ACL_*")` call sites at baseline (pre-Phase-A
  reductions); post-Phase-A the figure is in the low-30s remaining
  for `AFTER_ACL_QUIET_READ`, `AFTER_ACL_VALUE_OBJECT_*`,
  `AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ`,
  `AFTER_ACL_DATA_VECTOR_COLLECTION_READ`, `AFTER_ACL_MY_DATA_*`,
  `AFTER_ACL_STREAM_READ` (per AFTER_INVOCATION_MIGRATION.md).

### 2c. `@EnableGlobalMethodSecurity`

- 1 production site:
  `gemma-core/.../security/MethodSecurityConfig.java:74`
  (`@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, order = 1)`).
  This is the **single** cutover point for the method-security
  annotation; the Phase C prep document targets exactly this line.

### 2d. `RunAsManager` / `RUN_AS_*`

- 1 production XML wiring of `RunAsImplAuthenticationProvider`
  (`gemma-core/.../applicationContext-security.xml:67`).
- 1 production wiring of `runAsManager()` in `MethodSecurityConfig`
  (`MethodSecurityConfig.java:185`).
- 1 production `GemmaAclConfiguration` reference (commentary at
  `GemmaAclConfiguration.java:99-112` documenting the
  `GROUP_RUN_AS_ADMIN > GROUP_ADMIN` hierarchy entry that supports
  RunAs token escalation).
- **11 `@Secured("RUN_AS_*")` call sites** across two production
  files (`UserManager` has 10, `ExpressionExperimentReportService`
  has 1). Per the Phase C prep document, these are explicitly
  scoped out of Phase C and called out as a "Phase D concern".

### 2e. Web-side deprecations

- 0 `WebSecurityConfigurerAdapter` Java sites (Gemma never adopted
  Java config for HTTP security; the web security chain is XML-only).
- 0 `authorizeRequests` / `antMatchers` Java sites (same reason).
- 18 `<s:intercept-url>` declarations across two `<s:http>` blocks
  in `gemma-web/.../applicationContext-security.xml` — the XML
  equivalent of `antMatchers`. The Spring Security XML schema
  remains supported in SS7 (per the SS7 milestone notes), but
  the `access-decision-manager-ref` attribute and the embedded
  `AffirmativeBased` bean both go away with SS6's
  `AccessDecisionManager`. The XML chain has to be rewritten
  against `authorizationManagerRef` / per-pattern
  `AuthorizationManager` beans, or migrated to a Java
  `SecurityFilterChain` bean.

### 2f. `@Secured("ACL_SECURABLE_*")`

- **281 call sites** in production (`@Secured` annotations carrying
  any `ACL_SECURABLE_*` attribute). The AclEntryVoter recce
  (`56f71171ab`) breaks this down as 176 READ + 87 EDIT +
  12 COLLECTION_READ + 2 COLLECTION_EDIT + 2 _IGNORE_TRANSIENT
  variants + 1 typo (`ACL_SECURABLE__READ`, the always-denying
  bug in `BioMaterialService` masked by a co-attribute).

### 2g. Non-ACL `@Secured` (role-style)

- 612 production `@Secured` sites in gemma-core (includes ACL +
  AFTER_ACL + role attributes — same annotation, mixed contents).
- 19 production `@Secured` sites in gemma-rest (target of the
  `worktree-secured-prauthorize` branch, which rewrites them to
  `@PreAuthorize`).
- 0 production `@Secured` sites in gemma-web.
- 170 `@Secured` literals that are pure role-style (`GROUP_*` /
  `IS_AUTHENTICATED_ANONYMOUSLY` only, no ACL / AFTER_ACL / RUN_AS
  attribute). These are decidable by SS7's role-style
  `@Secured` interceptor and need no work as long as
  `@EnableMethodSecurity(securedEnabled = true)` is on.

## 3. Coverage by in-flight migration branches

| In-flight branch (or recce doc) | Covers |
|---|---|
| `worktree-afterinv-phase-a` (landed on recce branch) | 7 fully-redundant `AfterInvocationProvider`s retired; ~111 `@Secured("AFTER_ACL_READ")` / `AFTER_ACL_COLLECTION_READ` / `AFTER_ACL_MAP_READ` sites converted to `@PostAuthorize` / `@PostFilter`. |
| `worktree-afterinv-phase-b-quiet` (`bf287e9a`) | `afterAclReadQuiet` gsec→Gemma swap. |
| `worktree-afterinv-phase-b-cs-dv` (`ba9e8b46`) | `afterAclCompositeSequenceCollectionRead` + `afterAclDataVectorCollectionRead`. |
| `worktree-afterinv-phase-b-vo` (`2e152817`) | `afterAclValueObject`, `afterAclValueObjectCollection`, `afterAclValueObjectMap`. |
| `worktree-afterinv-phase-c-prep` (`c587f38640`) | Plan: switch `@EnableGlobalMethodSecurity` → `@EnableMethodSecurity`; nine `AuthorizationManager<MethodInvocationResult>` adapters; new `@AfterAcl*` annotations; drop `AfterInvocationProviderManager` bridge. |
| `worktree-aclentryvoter-recce` (`56f71171ab`) | Plan: handle the 281 `ACL_SECURABLE_*` sites via either (Path B) parallel `AuthorizationManager` advisors that delegate to the existing voter beans, or (Path A) `@PreAuthorize` SpEL sweep. Plus retire the dead `AclEntryMapVoter` / `AclEntryMapValueVoter` from gsec. |
| `worktree-secured-prauthorize` | gemma-rest's 19 `@Secured("GROUP_ADMIN")` → `@PreAuthorize`. |
| `worktree-rest-security-config` | New `RestSecurityConfig` for gemma-rest (Java config; replaces the `/rest/v2/**` XML chain). |

The pipeline as a whole closes out **method-security**:
`AccessDecisionManager`, `AfterInvocationManager`,
`@EnableGlobalMethodSecurity`, plus the bulk of `@Secured` voter
dispatch.

## 4. Gaps not covered by any in-flight branch

### Gap 4a. Web-side HTTP security XML (1 file, 2 `<s:http>` chains, 18 `<s:intercept-url>`s)

`gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml`
is **not** addressed by any of the in-flight branches. It contains:

- The `httpAccessDecisionManager` bean (`AffirmativeBased` with
  `WebExpressionVoter`, `RoleHierarchyVoter`, `AuthenticatedVoter`).
- Two `<s:http>` chains (`/rest/v2/**`, `/**`) wired via
  `access-decision-manager-ref="httpAccessDecisionManager"`.
- 18 `<s:intercept-url>` declarations on the second chain.

`worktree-rest-security-config` will replace **one** of these chains
(`/rest/v2/**`) with a Java `SecurityFilterChain`, but the `/**` chain
plus the `httpAccessDecisionManager` bean itself remain XML-resident.

### Gap 4b. Direct `accessDecisionManager.decide(...)` call sites (5 production)

Five places call the http `accessDecisionManager` bean directly to
gate behaviour outside the `<s:http>` chain:

- `gemma-rest/.../DatasetsWebService.java:3082`
- `gemma-rest/.../PlatformsWebService.java:318`
- `gemma-rest/.../providers/CacheControlHeaderDecorator.java:54`
- `gemma-web/.../taglib/common/auditAndSecurity/ExceptionTag.java:116`
- `gemma-core/.../persistence/service/AbstractFilteringVoEnabledService.java:29`
  (field-only; verify if `.decide()` is called downstream)

These will not compile under SS7 once `AccessDecisionManager` is
removed. Each one is a 5-line rewrite to either `AuthorizationManager`
or to a programmatic role check; the Phase C prep document flags
two of them (DatasetsWebService / PlatformsWebService) but does not
plan the fix.

### Gap 4c. `@Secured("RUN_AS_*")` (11 production sites)

The Phase C prep explicitly punts these to "Phase D". Path forward
is unclear — Spring Security 6/7 has no `RunAsManager` equivalent.
Three options:

1. **Programmatic SecurityContext swap.** Wrap each call site in a
   service-layer helper that saves the current `SecurityContext`,
   installs an elevated `Authentication` (a real `GROUP_ADMIN`
   token, not a derived RunAs token), invokes the delegate, and
   restores the previous context in a `finally` block. Requires
   audit: any code path that today expects to see
   `GROUP_RUN_AS_ADMIN` in the authentication's authorities (e.g.
   the role hierarchy entry `GROUP_RUN_AS_ADMIN > GROUP_ADMIN`)
   has to be reviewed.
2. **`@WithMockUser`-style proxy.** Custom AOP advice that installs
   the elevated context for the call duration. Same surface as (1)
   with the wiring moved out of the call site.
3. **Eliminate the need.** All 11 call sites today are
   `@IS_AUTHENTICATED_ANONYMOUSLY` + `RUN_AS_ADMIN` (user CRUD
   inside `UserManager`) or `GROUP_AGENT` + `RUN_AS_ADMIN`
   (`ExpressionExperimentReportService.recalculateBatchEffect`).
   The semantic is "ordinary callers cannot reach this, but when
   they do, run it as admin so it can touch admin-only resources".
   Refactoring to inject `UserDetailsService` / `AclService`
   directly and bypass the per-call ACL check is feasible but
   touches sensitive code.

### Gap 4d. `AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY` constant references (3 production + 2 test)

`AuthenticatedVoter` goes away with SS7. Three production files
(`UserManagerImpl`, `SecurityUtils`, `UserManager.java` indirectly
via the `@Secured` string) and two tests reference the constant.
The literal string `"IS_AUTHENTICATED_ANONYMOUSLY"` continues to
work as a `@Secured` attribute (interpreted by Gemma's anonymous
auth plumbing, not by `AuthenticatedVoter`). The fix is: copy the
constant to a Gemma-owned utility class, switch the imports.

### Gap 4e. Test-fixture `AccessDecisionManager` mocks (7 tests)

Seven production tests mock `AccessDecisionManager`. After
`AccessDecisionManager` is removed they will not compile. Each
mock is 2 lines; the fix migrates with the production cutover.

## 5. Risk per gap

| Gap | Risk if not fixed before SS7 | Severity |
|---|---|---|
| 4a (web XML) | App will not start. `AccessDecisionManager`-based XML chain fails on schema load. Touches every HTTP request. | **Hard blocker.** |
| 4b (5 ADM call sites) | Compile failure in gemma-rest and gemma-web. Two of them gate admin-only REST endpoints; the cache-control decorator gates response caching for authenticated browsers. | **High.** Compile-time, catches in CI immediately, but blocks the SS7 bump. |
| 4c (11 RUN_AS sites) | Compile failure on the `@Secured("RUN_AS_*")` interceptor (RunAsManager removed), and silent semantic change if the literal is ignored. The 10 sites in `UserManager` cover user-creation paths that anonymous browsers walk during signup. | **High.** Silent-failure mode is much worse than 4b's compile-time mode. |
| 4d (`AuthenticatedVoter` constant) | Compile failure on 5 files. Trivial fix. | **Low.** |
| 4e (test mocks) | Test compile failure. | **Low.** |

The in-flight work (Phases A through C, AclEntryVoter, prauthorize,
rest-security-config) **does not** make the codebase SS7-clean on
its own. The four method-security gaps above land Gemma on a
working SS6 / `@EnableMethodSecurity` stack, but the web-side
(4a/4b) and the RUN_AS semantic (4c) remain SS7 blockers.

## 6. Recommended sequencing

The whole plan is gated on Spring Boot / Spring Framework upgrades
landing first (Phase 2 / `phase2-acl-migrate` is Spring 6 + Spring
Security 6 already; SS7 will arrive with Spring Framework 7 /
Spring Boot 4).

Within the security work itself, the dependency chain is:

1. **Phase B finishes** — three branches land
   (`afterinv-phase-b-quiet`, `-cs-dv`, `-vo`). No SS7 work blocked
   on this; it is the precondition for Phase C.
2. **AclEntryVoter cutover lands** (`aclentryvoter-recce` →
   implementation branch). Phase C is gated on this per the Phase C
   prep document — the gsec ACL voters cannot live alongside
   `@EnableMethodSecurity` without a parallel `AuthorizationManager`
   advisor.
3. **Phase C lands** (`@EnableGlobalMethodSecurity` →
   `@EnableMethodSecurity`, nine `AuthorizationManager` adapters).
   This closes 2b, 2c, and the bulk of the SS6 method-security
   deprecations.
4. **gemma-rest `RestSecurityConfig` + `@Secured`→`@PreAuthorize`**
   land (the two existing branches). Closes 2e for the
   `/rest/v2/**` chain and gemma-rest's 19 `@Secured` sites.
5. **Gap 4a: rewrite gemma-web HTTP security.** Either rewrite
   `applicationContext-security.xml` against
   `authorizationManagerRef`, or convert the whole web chain to
   a Java `SecurityFilterChain` config (cleaner; matches the
   gemma-rest direction from step 4). Includes replacing the
   `httpAccessDecisionManager` bean with `AuthorizationManager`s.
6. **Gap 4b: rewrite the 5 direct `accessDecisionManager.decide()`
   call sites.** Each one becomes either an `AuthorizationManager`
   call or a direct authority check; mechanical sweep.
7. **Gap 4d: switch `AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY`
   references** to a Gemma-owned constant.
8. **Gap 4c: RUN_AS replacement.** Requires a design decision
   first (option 1, 2, or 3 from §4c). Doing this last keeps the
   compile clean for as long as possible while the rest of the
   SS6→SS7 sweep happens.
9. **SS7 dependency bump.** With steps 1–8 landed, the bump should
   be a clean compile.
10. **Drop the back-compat layer** (Phase C's `@AfterAcl*`
    annotations can probably be replaced with stock `@PostAuthorize`
    once side-effecting VO mutation is refactored out of the
    `AuthorizationManager` adapters — Phase E concern).

Step 8 (RUN_AS) is the only step that has to land **before** the
SS7 bump but **cannot** be done mechanically. Everything else is
either a recce-with-plan-already (steps 1–4) or a sweep with
clear targets (steps 5–7).

## 7. Effort estimate

Rough estimates assume the existing recce plans are accurate.

| Step | Effort | Notes |
|---|---|---|
| 1. Phase B finish | 0.5 day | Three branches mostly green. |
| 2. AclEntryVoter cutover | 5–10 days | 281 call sites; Path B is faster (parallel advisors, no SpEL rewrite), Path A is cleaner long-term. |
| 3. Phase C `@EnableMethodSecurity` | 3–5 days | Nine adapters + annotation sweep on ~38 remaining `AFTER_ACL_*` sites. |
| 4. gemma-rest finish | 1 day | Both branches small. |
| 5. Gemma-web HTTP security rewrite | 2–4 days | Touches every HTTP request path; needs an integration-test pass. |
| 6. ADM call-site sweep | 0.5 day | 5 sites, mechanical. |
| 7. `AuthenticatedVoter` constant | 0.25 day | 5 imports. |
| 8. RUN_AS replacement | 2–10 days | Depends on which option; option 3 is the upper end. |
| 9. SS7 dependency bump | 1 day | Should be clean after 1–8. |
| 10. Drop back-compat | 2–4 days | Stretch; can defer to Phase E. |
| **Total** | **17–36 days** | Spread across the SS7 GA timeline. |

All estimates are upper bounds on focused work; the calendar
duration depends on how the parallel branches sequence into
`development`.

## 8. Open questions

1. **RUN_AS path forward** — option 1 (programmatic context swap),
   option 2 (AOP wrapper), or option 3 (refactor to avoid the
   need). Decision needed before step 8 can start. The 10
   `UserManager` sites are the hot path; the 1
   `ExpressionExperimentReportService` site is admin-batch and
   easier.
2. **gemma-web HTTP security: XML or Java?** Step 5 can preserve
   the existing XML (rewriting `access-decision-manager-ref` to
   `authorization-manager-ref` and bean classes) or migrate to a
   `SecurityFilterChain` bean. The Java option matches gemma-rest's
   direction and removes the gemma-web XML config tree entirely;
   the XML option is a smaller diff but doubles down on a config
   style Spring is de-emphasizing.
3. **`AbstractFilteringVoEnabledService.accessDecisionManager`** —
   is the field actually used at runtime, or dead? Quick audit
   before step 6.
4. **AclEntryMapVoter / AclEntryMapValueVoter dead-code drop** —
   the AclEntryVoter recce identifies these as having zero call
   sites in Gemma. Are they used by any other gsec consumer? If
   not, drop from gsec itself (separate gsec PR) before step 2,
   or carry them along.
5. **Spring Security 7 timeline confirmation** — the mid-2026
   target is based on the SS7 milestone roadmap as of writing.
   If the timeline slips (it often does), we get more runway;
   if it accelerates, gaps 4a and 4c become urgent.
6. **gsec gemma fork** — gsec is Gemma-owned (`gemma.gsec` package).
   Some of the SS7 cleanup might be easier done **inside** gsec
   (e.g. retiring `AclEntryMapVoter`, providing
   `AuthorizationManager`-shaped voter beans alongside the existing
   `AccessDecisionVoter`-shaped ones). Open question whether to
   do this in-tree as part of the Phase 3 work, or as a separate
   gsec major version bump.

## 9. Out of scope for this recce

- The actual cutover code for any of the gaps above.
- gsec library changes (separate decision per §8.6).
- The Spring Boot 4 / Spring Framework 7 dependency bumps — those
  are Phase 4 work, prerequisite to SS7 but not part of this
  inventory.
- Authentication-side changes (login flow, remember-me, session
  management). SS7 does not break the authentication APIs Gemma
  uses today.
