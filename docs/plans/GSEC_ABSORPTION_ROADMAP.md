# gsec absorption roadmap

Reconnaissance, no code changes. Branch: `phase2-acl-migrate`, baseline
`ab1507ace4`.

This roadmap covers the absorption of `pavlab:gemma-gsec:0.0.24-RENOVATIONS-SNAPSHOT`
into `gemma-core`. gsec was extracted from Gemma years ago as a "general" security
library but has only ever had Gemma as a consumer; Phase 3 modernization has revealed
it is now a liability (parallel Sid hierarchies, XML config, separate release cadence
for a single-tenant dependency). The decision is to absorb gsec into gemma-core and
"make it do just what we need."

Source repo for gsec: `~/Dev/gsec/gsec` (GitHub `PavlidisLab/gsec`), HEAD
`63d3817ea3` ("Bump Spring 6.2.8 / SS 6.5.1 / HB 6.6.18 for Gemma Phase 3
alignment"). Maven artifact: `~/maven.repository/pavlab/gemma-gsec/0.0.24-RENOVATIONS-SNAPSHOT/gemma-gsec-0.0.24-RENOVATIONS-SNAPSHOT.jar`
(122 KB, 65 top-level classes).

---

## Section 1: gsec surface inventory

65 production classes across 9 packages, **7126 LoC** in `src/main/java`
(27 additional test files not absorbed).

| Package | Files | LoC | Concern |
|---|---:|---:|---|
| `gemma.gsec` | 4 | 1390 | `SecurityService` (interface + impl), `AuthorityConstants`, `package-info` |
| `gemma.gsec.acl` | 8 | 1315 | `BaseAclAdvice`, `AclEventListener`, `AclAuthorizationStrategyImpl`, `AclSidRetrievalStrategyImpl`, `ObjectIdentityRetrievalStrategyImpl`, `ObjectTransientnessRetrievalStrategy(Impl)` |
| `gemma.gsec.acl.afterinvocation` | 14 | 900 | 13 after-invocation providers + abstract base |
| `gemma.gsec.acl.annotation` | 1 | 16 | `IgnoreAcl` marker |
| `gemma.gsec.acl.domain` | 12 | 1889 | ACL JPA entities (`AclSid`, `AclPrincipalSid`, `AclGrantedAuthoritySid`, `AclObjectIdentity`, `AclEntry`, `AclImpl`, `Sids`) + Hibernate `AclDao(Impl)` + `AclService(Impl)` |
| `gemma.gsec.acl.voter` | 5 | 418 | `AclEntryVoter`, `AclEntryCollectionVoter`, `AclEntryMapVoter`, `AclEntryMapValueVoter` |
| `gemma.gsec.authentication` | 11 | 713 | `UserDetailsImpl`, `ManualAuthenticationService(Impl)`, `UserService`, `UserDetailsManager`, `GroupManager`, `UserExistsException`, `LoginDetailsValueObject`, `Ajax*Handler`s |
| `gemma.gsec.model` | 8 | 287 | `Securable`, `SecuredChild`, `SecuredNotChild`, `SecureValueObject`, `User`, `UserGroup`, `GroupAuthority` (interfaces) |
| `gemma.gsec.util` | 2 | 198 | `SecurityUtil` static helpers |
| **TOTAL** | **65** | **7126** | |

Resources shipped in the JAR:

- `gemma/gsec/applicationContext-gsec.xml` (481 lines) — bean definitions for
  `securityService`, `aclCache` (`SpringCacheBasedAclCache` over `ConcurrentMapCache`),
  `permissionEvaluator`, `securityExpressionHandler`, `accessDecisionManager`
  (`UnanimousBased` with 8 ACL voters + roleHierarchy + AuthenticatedVoter + SpEL),
  `roleHierarchy`, 14 after-invocation providers, `objectIdentityRetrievalStrategy`,
  `sidRetrievalStrategy`, `authenticationTrustResolver`, `manualAuthenticationService`,
  `anonymousAuthenticationProvider`, `sessionRegistry`. The historical `aclDao` and
  `aclService` bean definitions are commented out — Gemma's `GemmaAclConfiguration`
  now owns them.
- `gemma/gsec/model/AclSid.hbm.xml` — single-table inheritance: abstract `AclSid`
  with `principal` BIT discriminator (-1 abstract, 1 `AclPrincipalSid`, 0
  `AclGrantedAuthoritySid`); both subclasses share the `sid` VARCHAR(255) column.
  `mutable="false"` — Hibernate reads only; writes flow through
  `JdbcMutableAclService`.
- `gemma/gsec/model/AclObjectIdentity.hbm.xml` — read-only mapping; `type` derived
  via formula from `acl_class.class`; `objectIdClass` Long mapped for FK column
  generation.
- `gemma/gsec/model/AclEntry.hbm.xml` — read-only mapping; many-to-one to `AclSid`.
- `gemma/gsec/hibernate.cfg.xml` — points to the three hbm files.

### Public surface vs internal

"Public" = imported by Gemma somewhere:

- **High-traffic public** (30+ imports each): `SecurityService`, `SecurityUtil`
- **Medium** (10–20 imports): `UserDetailsImpl`, `AclObjectIdentity`, `AclService`,
  `ManualAuthenticationService`
- **Low** (3–9 imports): `AuthorityConstants`, `SecureValueObject`,
  `ObjectIdentityRetrievalStrategyImpl`, `AclSid`, `AclPrincipalSid`,
  `AclGrantedAuthoritySid`, `UserExistsException`
- **One-off** (1–2 imports): `BaseAclAdvice`, `AclEventListener`, `AclSidRetrievalStrategyImpl`,
  `AclAuthorizationStrategyImpl`, `AclDao(Impl)`, `AclServiceImpl`, `AclEntry`,
  `AclEntryAfterInvocationCollectionFilteringProvider`, `User`, `UserGroup`,
  `Securable`, `GroupAuthority`, `LoginDetailsValueObject`, `GroupManager`,
  `UserDetailsManager`

### Internal — never imported by Gemma source (13 classes)

Zero Java imports across `gemma-core`/`gemma-rest`/`gemma-web`/`gemma-cli`:

```
AclEntryAfterInvocationMapFilteringProvider          # XML-wired only
AclEntryAfterInvocationMapValueFilteringProvider     # XML-wired only
AclEntryAfterInvocationValueObjectMapFilteringProvider  # XML-wired only
AclEntryAfterInvocationValueObjectMapValueFilteringProvider  # XML-wired only (and unused)
AclEntryAfterInvocationValueObjectProvider           # XML-wired only
AclEntryVoter, AclEntryCollectionVoter,              # XML-wired only
AclEntryMapVoter, AclEntryMapValueVoter
AclImpl                                              # Hibernate-only; not directly referenced
IgnoreAcl                                            # marker annotation, no users
ManualAuthenticationServiceImpl                      # XML-wired only (interface used)
ObjectTransientnessRetrievalStrategy                 # XML-wired only
```

The XML-wired classes are referenced via bean class names in
`applicationContext-gsec.xml`; they go away when Phase C inlines the XML.
`IgnoreAcl` and `AclImpl` are dead.

---

## Section 2: Gemma's actual usage

**95 source files** under `gemma-{core,rest,web,cli}/src/main/java` import from
`gemma.gsec.*`; 36 more under `src/test/java`. **29 distinct gsec classes** are
imported.

Module breakdown:

| Module | Files importing gsec |
|---|---:|
| `gemma-core` | 68 |
| `gemma-web` | 15 |
| `gemma-cli` | 10 |
| `gemma-rest` | 2 |

Other (non-Java) references:

- `pom.xml` line 443: the dependency declaration (`pavlab:gemma-gsec:${gsec.version}`,
  pinned to `0.0.24-RENOVATIONS-SNAPSHOT` at line 1283).
- `gemma-core/src/main/resources/hibernate.cfg.xml` lines 18-20: includes three
  `gemma/gsec/model/Acl*.hbm.xml` resources by classpath name.
- `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml` lines
  27, 31: wires gsec's `AjaxAuthenticationSuccessHandler` / `AjaxAuthenticationFailureHandler`
  by class name.
- `gemma-core/src/main/java/ubic/gemma/core/security/SecurityConfig.java` line 122:
  `@ImportResource("classpath:gemma/gsec/applicationContext-*.xml")`.

### Usage by gsec class (top-of-list with consumer pattern)

| gsec class | Files | Surface |
|---|---:|---|
| `gemma.gsec.SecurityService` | 30 | `@Autowired` interface; ACL grants/revokes via service code |
| `gemma.gsec.util.SecurityUtil` | 26 | Static helpers (`isUserAnonymous`, `isUserAdmin`, current principal lookups) |
| `gemma.gsec.authentication.UserDetailsImpl` | 17 | `@Autowired`/instantiated in auth + user-mgmt flows |
| `gemma.gsec.acl.domain.AclObjectIdentity` | 15 | HQL queries (`AclQueryUtils`, `ExpressionExperimentDaoImpl`); load-bearing Hibernate entity |
| `gemma.gsec.acl.domain.AclService` | 13 | `@Autowired` interface (Gemma adapter implements it) |
| `gemma.gsec.authentication.ManualAuthenticationService` | 12 | `@Autowired` interface; runAs / batch / CLI auth |
| `gemma.gsec.AuthorityConstants` | 8 | Constants (`GROUP_USER`, `GROUP_AGENT`, etc.) |
| `gemma.gsec.model.SecureValueObject` | 7 | Marker interface implemented by VOs |
| `gemma.gsec.acl.domain.AclPrincipalSid` | 3 | HQL `WHERE sid.class = AclPrincipalSid` filter (`AclQueryUtils`) |
| `gemma.gsec.acl.domain.AclGrantedAuthoritySid` | 3 | HQL joins for group-based ACL filtering (`AclQueryUtils`) |
| `gemma.gsec.acl.domain.AclSid` | 3 | abstract entity, referenced where needed |
| `gemma.gsec.acl.ObjectIdentityRetrievalStrategyImpl` | 3 | Spring bean; subclassed/wired |
| `gemma.gsec.acl.BaseAclAdvice` | 2 | Subclassed by `ubic.gemma.core.security.authorization.acl.AclAdvice` |
| `gemma.gsec.acl.domain.AclDao` / `AclDaoImpl` / `AclServiceImpl` | 2 each | Live in test fixtures + the one HQL helper path |
| `gemma.gsec.acl.AclEventListener` | 1 | `AclEventListenerConfig` instantiates it |
| `gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider` | 1 | Base for `AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider` etc. |
| `gemma.gsec.acl.AclAuthorizationStrategyImpl` / `AclSidRetrievalStrategyImpl` | 2 each | Wired in Gemma's `GemmaAclConfiguration` + test contexts |
| `gemma.gsec.authentication.UserExistsException` | 3 | Thrown by `UserManagerImpl` |
| `gemma.gsec.model.User` / `UserGroup` / `GroupAuthority` | 1 each | Domain interfaces — Gemma's persistence model implements them |
| `gemma.gsec.model.Securable` | 1 | Marker interface implemented by all Gemma securables |
| `gemma.gsec.authentication.LoginDetailsValueObject` / `GroupManager` / `UserDetailsManager` | 1 each | Auth + user-mgmt support types |

Three Gemma classes that are already gsec-equivalents (Phase 3 has been
quietly migrating functionality in-tree):

- `ubic.gemma.core.security.acl.GemmaAclConfiguration` — owns `aclService`,
  `aclCache`, `aclAuthorizationStrategy`, `permissionGrantingStrategy`, `lookupStrategy`,
  the `GsecAclServiceAdapter` (adapts `JdbcMutableAclService` to gsec's `AclService`
  interface) and dedupe/normalize/`deleteSid` JDBC implementations.
- `ubic.gemma.core.security.acl.AclEventListenerConfig` — wires gsec's
  `AclEventListener` into the Hibernate session factory.
- `ubic.gemma.core.security.authorization.acl.*` — already houses **20+** ACL
  classes that mostly subclass or replace gsec types: `AclAdvice` extends
  `BaseAclAdvice`; 12 `AclEntryAfterInvocation*` providers (some shadow gsec, some
  are Gemma-specific by-association filters); `AclLinterService(Impl)`,
  `ParentIdentityRetrievalStrategy(Impl)`, `AclClassMetadata`, `AclVoterAuthorizationManager`.

---

## Section 3: Trim scope

### 3.1 Sid types — the central problem

gsec ships parallel `Sid` implementations that do NOT inherit from Spring
Security's stock `org.springframework.security.acls.domain.PrincipalSid` /
`GrantedAuthoritySid`:

- `gemma.gsec.acl.domain.AclSid` — abstract Hibernate-mapped entity (read-only,
  `mutable="false"`); subclasses:
- `gemma.gsec.acl.domain.AclPrincipalSid` — discriminator `principal=1`
- `gemma.gsec.acl.domain.AclGrantedAuthoritySid` — discriminator `principal=0`

Both subclasses *also* implement `org.springframework.security.acls.model.Sid` —
but `equals()` between a gsec `AclPrincipalSid` and a Spring `PrincipalSid` returns
**false** because Spring's `PrincipalSid#equals` does an `instanceof PrincipalSid`
check that the gsec class fails.

Phase 3 sidestepped the bug by ensuring no gsec Sid ever leaks into the
security-path: `GsecAclServiceAdapter` keeps the read API surface but
`JdbcMutableAclService` always constructs Spring sids, and HQL queries that
return gsec sids stay within the Hibernate read path. This is fragile — every
new code path that touches sids is one more place to remember the rule.

**Target shape after absorption:**

- Drop `gemma.gsec.acl.domain.AclPrincipalSid` and `AclGrantedAuthoritySid`
  as `Sid` implementations entirely.
- Keep the Hibernate-mapped abstract `AclSid` entity, but rename to
  `AclSidEntity` (or similar) and have it NOT implement `Sid`. It becomes a
  pure DB-row type used by HQL queries.
- The two subclasses become discriminator-mapped data classes
  (`AclPrincipalSidRow`, `AclGrantedAuthoritySidRow`) that simply hold the
  string and the discriminator; conversion to a Spring `Sid` is a one-line
  method (`toSid()` returning `new PrincipalSid(name)` or
  `new GrantedAuthoritySid(name)`).
- Every code path that creates a `Sid` for the security path uses Spring's
  stock types directly.

Risk: 3 files import each of `AclPrincipalSid` and `AclGrantedAuthoritySid`
(`AclQueryUtils`, plus tests). Each call site already produces an `AclSidEntity`-
shaped result; converting to "select stock-Sid in code" is mechanical.

### 3.2 AclService — drop the interface

gsec's `gemma.gsec.acl.domain.AclService` interface extends Spring's
`MutableAclService` with three Gemma-specific methods that the adapter implements:

- `readAclById(ObjectIdentity, Session)` — legacy session-aware overload; the
  adapter no-ops the session arg.
- `openSession()` — returned a Hibernate session; the adapter returns `null`.
- `deleteSid(Sid)` — pure-JDBC implementation in the adapter.

After absorption:

- Use Spring Security's stock `MutableAclService` interface in every wiring point.
- The two session-aware methods are dead (legacy artifacts of the Hibernate-backed
  `AclDaoImpl`); they get **deleted from all 13 consumers** (mostly DAO mass-ACL
  helpers and tests).
- `deleteSid` migrates to a Gemma-side `AclMaintenanceService` or stays as a
  utility on a renamed `GemmaAclService` class — the 1-2 call sites pick the
  service explicitly.
- `GsecAclServiceAdapter` is **deleted** — `aclService` bean becomes a plain
  `JdbcMutableAclService` with the `setClassIdentityQuery`/`setSidIdentityQuery`
  MySQL overrides applied directly. The dedupe behaviour and `ObjectIdentity`
  normalization either move into a wrapping `MutableAclService` (if still needed)
  or into the call sites that build `ObjectIdentity` (use `ObjectIdentityImpl`
  consistently to eliminate the equals/hashCode mismatch).

### 3.3 BaseAclAdvice, AclEventListener, after-invocation providers

These are mostly migrated already (Phase 3 copied 12 after-invocation providers
into `ubic.gemma.core.security.authorization.acl`). Finishing the migration:

- `BaseAclAdvice`: 472 LoC abstract base. Gemma's `AclAdvice` is the only
  subclass. Two options: collapse `BaseAclAdvice` into `AclAdvice` (no other
  consumers), or move it intact under `ubic.gemma.core.security.authorization.acl`.
  Prefer collapse — eliminates the inheritance-for-its-own-sake.
- `AclEventListener`: single small class. Move under `ubic.gemma.core.security.acl`.
- After-invocation providers (`AclEntryAfterInvocation*` family): 13 in gsec,
  most subclassed or shadowed in Gemma already. Finish the migration: rehome the
  base (`AclEntryAfterInvocationProvider`) + the few providers Gemma still
  references directly (`AclEntryAfterInvocationCollectionFilteringProvider`,
  `AclEntryAfterInvocationOwnedCollectionFilteringProvider`,
  `AclEntryAfterInvocationPrivateCollectionFilteringProvider`,
  `AclEntryAfterInvocationStreamFilteringProvider`). Note: the
  `AFTER_INVOCATION_MIGRATION.md` roadmap is the eventual successor — it migrates
  this whole stack to Spring Security 6 `@PostAuthorize`/`@PostFilter`. The
  absorption only needs to **rehome the bytes**, not redesign the API.

### 3.4 Voters (AclEntryVoter family)

Five classes. Per `ACL_ENTRY_VOTER_MIGRATION.md`, this whole family is being
retired in favour of `AuthorizationManager` (Spring Security 6 modern API);
`AclVoterAuthorizationManager` is already in `ubic.gemma.core.security.authorization.acl`.
Absorption can either:

- (a) Copy the 5 voter classes verbatim and let the voter-migration session
  delete them when complete.
- (b) Skip them — wait for voter migration to land first, then drop the gsec
  dep entirely.

Recommend (a) for ordering safety (don't block on voter-migration).

### 3.5 Authentication

11 classes, 713 LoC. Light surface, narrow consumers:

- `UserDetailsImpl` (17 consumers) — leaf POJO. Move verbatim.
- `ManualAuthenticationService(Impl)` (12 consumers) — move; rename probably
  unnecessary.
- `UserService` / `UserDetailsManager` / `GroupManager` — interfaces implemented
  by Gemma's `UserManagerImpl`. Move + collapse.
- `UserExistsException`, `LoginDetailsValueObject` — small support types.
  Move verbatim.
- `Ajax*Handler`s — referenced from `applicationContext-security.xml` in
  `gemma-web`. Move verbatim, update XML class FQNs.

### 3.6 SecurityService

`gemma.gsec.SecurityServiceImpl` (973 LoC) is the largest single class in gsec.
30 Gemma consumers. The implementation is mostly Hibernate-free ACL plumbing
(grant/revoke/owner-set, transitively-propagate-to-children); no architectural
change needed, just rehome.

### 3.7 Marker interfaces (`gemma.gsec.model.*`)

- `Securable`, `SecuredChild`, `SecuredNotChild`, `SecureValueObject` —
  implemented by Gemma's domain types. Renaming the package is a sed-rename;
  no behaviour change.
- `User`, `UserGroup`, `GroupAuthority` — implemented by Gemma's
  `ubic.gemma.model.common.auditAndSecurity.User` etc. Likewise.

### 3.8 What gets deleted outright

- gsec source repo (after Phase D lands): archive on a tag (`absorbed-into-gemma-vX.Y`)
  and stop publishing snapshots; the GitHub repo can stay read-only for archive.
- gsec test fixtures (27 test files) — Gemma has its own test fixtures and the
  ACL behaviour is exercised end-to-end by Gemma's integration tests; gsec's
  unit tests can be cherry-picked for the parts that aren't already covered.
- `applicationContext-gsec.xml` (481 lines) — inlined into Java config in Phase C.
- 13 classes that Gemma's source never imports (see Section 1): they exist only
  to be XML-wired in `applicationContext-gsec.xml`, so they vanish with the XML.

---

## Section 4: Phased absorption plan

### Phase A: copy + drop dependency (~2 sessions, ~7 KLOC moved)

**Goal.** Every `gemma.gsec.*` import in Gemma resolves to a class that lives
under `ubic.gemma.core.security.gsec.*` (preserved sub-structure: `gsec.acl.*`,
`gsec.acl.domain.*`, `gsec.acl.afterinvocation.*`, `gsec.acl.voter.*`,
`gsec.authentication.*`, `gsec.model.*`, `gsec.util.*`). The `pavlab:gemma-gsec`
dependency is removed from the root pom. Behaviour is bit-identical to today.

**Steps.**

1. Copy `~/Dev/gsec/gsec/src/main/java/gemma/gsec/**` into
   `gemma-core/src/main/java/ubic/gemma/core/security/gsec/`. 65 files, ~7126 LoC.
2. Replace package declarations: `package gemma.gsec` → `package ubic.gemma.core.security.gsec`
   (sed with the matching internal-import rewrites). Internal `gemma.gsec.*`
   imports become `ubic.gemma.core.security.gsec.*`.
3. Copy `applicationContext-gsec.xml`, `hibernate.cfg.xml`, three `Acl*.hbm.xml`
   files into `gemma-core/src/main/resources/ubic/gemma/core/security/gsec/`.
   Update class FQNs inside the resources (the 19 `class="gemma.gsec…"`
   attributes in the XML; the `hbm.xml` files' `<class name="...">` and
   `<many-to-one class="...">`).
4. Update Gemma's three external references to gsec resources:
   - `gemma-core/src/main/resources/hibernate.cfg.xml` lines 18-20 — point at
     the new `ubic/gemma/core/security/gsec/Acl*.hbm.xml` paths.
   - `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml`
     lines 27, 31 — `gemma.gsec.authentication.Ajax*Handler` → `ubic.gemma.core.security.gsec.authentication.Ajax*Handler`.
   - `gemma-core/src/main/java/ubic/gemma/core/security/SecurityConfig.java`
     line 122 — `@ImportResource("classpath:gemma/gsec/applicationContext-*.xml")`
     → `@ImportResource("classpath:ubic/gemma/core/security/gsec/applicationContext-*.xml")`.
5. Mechanical import rewrite across Gemma: 95 main + 36 test files. One
   `sed -i 's|import gemma\.gsec\.|import ubic.gemma.core.security.gsec.|g'`
   pass.
6. Update `GemmaAclConfiguration.GsecAclServiceAdapter` and the references to
   `gemma.gsec.acl.domain.AclObjectIdentity`/`AclPrincipalSid`/`AclGrantedAuthoritySid`
   inside the comments and JDBC code.
7. Drop the `pavlab:gemma-gsec` dependency from the root `pom.xml` (lines
   442-451) and the `gsec.version` property (line 1283).
8. `mvn -DskipTests=true verify` should be green. Run the full
   `mvn -Pgemma,h2 verify -DfailIfNoTests=false` against `gemdtest` (single-tenant
   MySQL — coordinate with parallel agents).

**Scope.** ~7126 LoC moved verbatim + ~150 LoC of Gemma source touched
(import rewrites). 109 Gemma source files modified.

**Risk.** Low — pure rehome, no signature changes, no behaviour changes. The
biggest catch is XML class FQN drift; the second is HBM file path drift in
`hibernate.cfg.xml`.

**Validation.**

- `mvn verify` clean.
- `grep -r 'gemma\.gsec\b' .` returns zero hits in Gemma source.
- Integration tests on `gemdtest` pass; the ACL test classes in particular
  (`gemma-core/src/test/java/ubic/gemma/core/security/**`).
- Spot-check: log into a running Gemma instance, exercise a permission-checked
  read path (e.g. `/expressionExperiment/showAllExpressionExperiments.html`),
  observe no behaviour change.

**Rollback.** Revert the single commit. The gsec JAR is still in the local maven
repo so the previous build works.

---

### Phase B: unify Sid hierarchies, drop adapter (~2 sessions, ~500 LoC touched)

**Goal.** No class implementing `Sid` outside of Spring Security's stock
`PrincipalSid` / `GrantedAuthoritySid`. The Hibernate-mapped `AclSid` entity
exists for HQL but is NOT a `Sid` implementation. `GsecAclServiceAdapter` and
the `gemma.gsec.acl.domain.AclService` interface are deleted.

**Steps.**

1. Rename `AclSid` → `AclSidEntity`, `AclPrincipalSid` → `AclPrincipalSidEntity`,
   `AclGrantedAuthoritySid` → `AclGrantedAuthoritySidEntity` (or pick names
   that emphasize "DB row, not security path"). Remove `implements
   org.springframework.security.acls.model.Sid` from all three.
2. Add a `toSid()` method on each that returns the corresponding stock Spring
   `Sid`. HQL callers that need to compare/use the result in the security path
   call `toSid()` at the boundary.
3. Update the three HBM files: class names in `<class name=...>` and the
   `<subclass>` elements.
4. Update `AclQueryUtils` (the main HQL site for these types) to either yield
   stock Spring sids at the boundary (`toSid()` on the loaded rows) or stay
   purely DB-side (use the entity for the join but never return it to a
   security-path caller).
5. Delete `GsecAclServiceAdapter` (lines 237-443 of `GemmaAclConfiguration.java`).
   The `aclService` bean becomes:
   ```java
   @Bean(name="aclService")
   public MutableAclService aclService(DataSource ds, LookupStrategy ls, AclCache c) {
       JdbcMutableAclService j = new JdbcMutableAclService(ds, ls, c);
       j.setClassIdentityQuery("SELECT @@IDENTITY");
       j.setSidIdentityQuery("SELECT @@IDENTITY");
       return j;
   }
   ```
6. Delete the `gemma.gsec.acl.domain.AclService` interface (now under
   `ubic.gemma.core.security.gsec.acl.domain.AclService`). 13 consumers update
   their field type to `org.springframework.security.acls.model.MutableAclService`.
7. Migrate `deleteSid` JDBC logic to a Gemma-side `AclMaintenanceService` (or
   inline it at the 1-2 call sites).
8. Migrate the dedupe-on-update behaviour. Two options: (a) wrap
   `JdbcMutableAclService` in a Gemma decorator that dedupes in `updateAcl`;
   (b) audit the call sites that *create* the duplicates and dedupe at source.
   Prefer (b) — `SecurityServiceImpl.makeReadableByGroup` is the main offender;
   it can do its own duplicate-check before calling `insertAce`.
9. Migrate the `ObjectIdentity` normalization. Audit the few places that build
   `gemma.gsec.acl.domain.AclObjectIdentity` (HQL boundary) and convert to
   `ObjectIdentityImpl` at the boundary, or stop crossing.
10. `mvn verify` + `gemdtest` integration suite.

**Scope.** ~50 file edits (the 3+ files using each renamed type + the 13
`AclService` field-type changes + the adapter deletion).

**Risk.** Medium. The dedupe + ObjectIdentity-normalize behaviour the adapter
provides today must be preserved or the eliminated. The `gemdtest`
integration tests will surface any regression in ACL write paths
(`SecurityServiceImpl.makeReadableByGroup` etc.).

**Validation.**

- `grep -rn "implements .*Sid\b" ubic.gemma.core.security` → only Spring's
  stock types appear.
- ACL write tests in `ubic.gemma.core.security.*Test` cover create / update /
  duplicate-grant / delete-sid paths.
- Manual: grant + revoke + re-grant the same permission on a test EE; verify
  `acl_entry` has only one row.

**Rollback.** Revert the Phase B commit; Phase A still in place so behaviour
returns to "with adapter."

---

### Phase C: inline applicationContext-gsec.xml (~1 session, ~500 LoC of Java)

**Goal.** No XML in the ACL/auth wiring path. `applicationContext-gsec.xml`
deleted; its bean definitions absorbed into `GemmaAclConfiguration`,
`SecurityConfig`, and `MethodSecurityConfig` (which already exist) plus a new
`GemmaAfterInvocationConfig` for the after-invocation provider beans.

**Steps.**

1. Inventory the XML: 481 lines, ~24 beans:
   - `anonymousAuthenticationProvider` → `SecurityConfig`.
   - `manualAuthenticationService` → `SecurityConfig` or a new
     `GemmaAuthenticationConfig`.
   - `sessionRegistry` → `SecurityConfig` (already declared there? confirm —
     XML may be redundant).
   - `securityService` → `GemmaAclConfiguration` or a new `GemmaSecurityServiceConfig`.
   - `aclCache` → `GemmaAclConfiguration` (already owns it).
   - `permissionEvaluator`, `securityExpressionHandler` → `MethodSecurityConfig`.
   - `authenticationTrustResolver` → `SecurityConfig`.
   - `objectIdentityRetrievalStrategy`, `objectTransientnessRetrievalStrategy`,
     `sidRetrievalStrategy` → `GemmaAclConfiguration`.
   - `accessDecisionManager` (8 ACL voters + roleHierarchy + AuthenticatedVoter +
     SpEL) → `MethodSecurityConfig`.
   - `roleHierarchy` → `MethodSecurityConfig`.
   - 14 after-invocation provider beans → new `GemmaAfterInvocationConfig`.
   - `postInvocationAdviceProvider` → `MethodSecurityConfig`.
2. Each `<bean>` becomes a `@Bean` method, each `<constructor-arg>` becomes
   a method argument or autowired field, each `<property>` becomes a setter call
   in the `@Bean` body.
3. Delete `applicationContext-gsec.xml` and the `@ImportResource` line in
   `SecurityConfig`.
4. `mvn verify` + integration suite. Beans wired by ID (autowire-by-name) must
   be reproduced; bean naming is `@Bean(name=...)`.

**Scope.** ~500 LoC of Java config added, 481 lines of XML deleted.

**Risk.** Medium. Spring's XML defaulting (autowire-by-name + bean
post-processors) doesn't always carry over cleanly to Java config. The
`AfterInvocationProviderManager` wiring in `MethodSecurityConfig.AFTER_INVOCATION_PROVIDER_BEAN_NAMES`
relies on bean lookup by name; the new `@Bean` methods must keep those names.

**Validation.** Same as Phase B; add an explicit assertion test that all 14
after-invocation provider beans are present in the `ApplicationContext` with
the expected names.

**Rollback.** Revert the commit; the XML import is restored.

---

### Phase D: rename packages to final homes (~1 session, ~7 KLOC sed)

**Goal.** Final package layout. `ubic.gemma.core.security.gsec.*` (intermediate
holding pen from Phase A) goes away; its contents move to permanent homes:

- `gsec.acl.*` → `ubic.gemma.core.security.acl.*` (merge with existing classes
  there; resolve any name collisions).
- `gsec.acl.afterinvocation.*` → `ubic.gemma.core.security.authorization.acl.*`
  (merge with existing).
- `gsec.acl.domain.*` → `ubic.gemma.model.common.auditAndSecurity.acl.*` (or
  similar — these are JPA entities, they belong in the model module).
- `gsec.acl.voter.*` → `ubic.gemma.core.security.authorization.acl.voter.*`.
- `gsec.authentication.*` → `ubic.gemma.core.security.authentication.*` (merge
  with existing).
- `gsec.model.*` (the marker interfaces `Securable`/`SecureValueObject`/etc.)
  → `ubic.gemma.model.common.auditAndSecurity.*` (these are domain markers,
  not security classes).
- `gsec.util.*` → `ubic.gemma.core.security.util.*` (or fold `SecurityUtil`'s
  static methods into an existing utility class).
- `gsec.SecurityService(Impl)` → `ubic.gemma.core.security.SecurityService(Impl)`.
- `gsec.AuthorityConstants` → `ubic.gemma.core.security.AuthorityConstants`.

**Steps.** Big mechanical sed rename, one package at a time. Each rename is its
own commit (revertable). Java refactor tooling (IntelliJ "Move package")
handles imports + references; just confirm XML class references update too
(if any survive Phase C, they shouldn't).

**Scope.** ~7126 LoC moved (the same files renamed again). All Gemma source
imports rewritten one more time. Touches ~200 files (95 import-only + 65
moved files + the moves' touchpoints).

**Risk.** Low (mechanical). Highest risk is name collisions when merging
`gsec.acl.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider`
with an existing class of the same name in
`ubic.gemma.core.security.authorization.acl`. Decide ahead of time which one
wins (audit Gemma source for subclasses; the base from gsec is the parent of
Gemma's specialized providers, so the gsec one wins for the base, the Gemma
specialised ones get a `By*` suffix as they already do).

**Validation.** `grep -r '\.security\.gsec\b' .` returns zero hits. Full
integration suite green.

**Rollback.** Per-rename commit revert. Phase A through C still hold; the
intermediate `gsec.*` package layout is restored.

---

## Section 5: Risks & blockers

### Risk 1: ACL data migration — **NOT a risk**

The `acl_sid` table schema (per `~/Dev/gsec/gsec/src/main/java/gemma/gsec/model/AclSid.hbm.xml`)
is **canonical Spring**: `id BIGINT PK`, `principal BIT discriminator`, `sid
VARCHAR(255)`. No Java class names live in the DB. Renaming the Hibernate
entities is a pure code change; no Flyway migration is needed.

Same for `acl_object_identity` and `acl_entry` — both `mutable="false"`,
Hibernate is read-only, writes flow through `JdbcMutableAclService` which
uses the canonical Spring schema.

**Caveat.** Spring Security's stock `acl_class` table stores Java class
names (`object_id_class` FK → `acl_class.class`). After Phase D renames
`gemma.gsec.model.Securable` implementations' package paths, the
`acl_class.class` rows would need updating — except none of the renamed types
in Phase D are concrete `Securable` *implementations*; the gsec `model` package
holds interfaces only. The concrete `Securable` classes
(`ExpressionExperiment`, `ArrayDesign`, etc.) live in `ubic.gemma.model.*`
and don't change. **`acl_class` is unaffected.**

Verify by snapshot of `acl_class.class` from `gemd` (read-only port-forward,
local 8000) before Phase D to confirm.

### Risk 2: other consumers of gsec — **low**

gsec is `pavlab:gemma-gsec` on GitHub `PavlidisLab/gsec`. A check of public
PavlidisLab repos for `import gemma.gsec` references is owed before the final
delete. **Action**: `gh search code --owner PavlidisLab "import gemma.gsec"`
before Phase D lands. If anything outside Gemma is found, gsec gets archived
at the absorption tag (Gemma is no longer the upstream); the absorbed copy in
Gemma diverges and other consumers stay on the archived tag.

The `gemma-gsec-0.0.24-RENOVATIONS-SNAPSHOT.pom` lists scm
`https://github.com/ppavlidis/gsec` (an old fork URL) — the actual repo at
`PavlidisLab/gsec` is the live one. Confirm + close the fork.

### Risk 3: AfterInvocation legacy stack entanglement — **medium**

The 14 after-invocation providers (Section 3.3) live in 4 packages across
gsec + Gemma:

- `gsec/acl/afterinvocation/*` — 13 classes (base + 12 implementations)
- `ubic.gemma.core.security.authorization.acl/*` — 12 implementations (subclasses
  or shadows of gsec's)
- 9 of Gemma's 12 wired providers in `MethodSecurityConfig.AFTER_INVOCATION_PROVIDER_BEAN_NAMES`
  are gsec classes (i.e. they are NOT yet copied into Gemma); 3 are Gemma's
  `By*` specialisations.

`AFTER_INVOCATION_MIGRATION.md` plans to retire the entire stack
(`AfterInvocationProviderManager` → `@PostAuthorize` / `@PostFilter` /
`AuthorizationManager`). The voter family migration (`ACL_ENTRY_VOTER_MIGRATION.md`)
similarly retires the 4 voter classes.

**Ordering.** The absorption (this roadmap) does NOT redesign the
after-invocation or voter API. It just rehomes the bytes. Phase A's
"copy verbatim" means the 13 after-invocation providers and 5 voter classes
land in `ubic.gemma.core.security.gsec.acl.afterinvocation/voter` — they
keep working exactly as before. The two follow-on migrations
(`AFTER_INVOCATION_MIGRATION.md`, `ACL_ENTRY_VOTER_MIGRATION.md`) then drive
them to zero.

**Risk surface.** If after-invocation migration happens BEFORE Phase D,
the gsec versions become orphaned (no callers); Phase D's "merge" of
`gsec.acl.afterinvocation` with `ubic.gemma.core.security.authorization.acl`
finds the gsec base unused and deletes most of it. If absorption is faster than
the after-invocation migration (likely), the absorbed gsec providers stay
around but are clearly marked "owned by us, retire when after-invocation
migrates."

### Risk 4: `BaseAclAdvice` is a 472-line aspect — **medium**

`gsec.acl.BaseAclAdvice` is an `@Aspect` that wires post-persist /
post-delete ACL row creation/deletion to Hibernate operations on `Securable`s.
Gemma's `AclAdvice` extends it. Inside the aspect: identity-retrieval, sid
construction, parent-link wiring (recursive into `SecuredChild`),
class-metadata caching. The "parallel sid hierarchy" problem (Section 3.1)
has its biggest payload here — `BaseAclAdvice` constructs `AclPrincipalSid`
instances to write ownership info to `acl_sid` via gsec's old `AclDaoImpl`.
Phase B's "drop gsec sid types" must verify this code path still writes
correctly via `JdbcMutableAclService`.

**Validation.** Per-EE create + delete round-trip in an integration test;
verify `acl_object_identity` and `acl_sid` rows are correct after each step.

### Risk 5: parallel-agent serialization on `gemdtest` — **operational**

Per `MEMORY.md`, `gemdtest` is single-tenant. Phases A-D each end with a
full-suite integration run; coordinate with other Phase 3 agents to serialize.

### Risk 6: ehcache exclusion in pom — **trivial cleanup**

`pom.xml:442-451` excludes `net.sf.ehcache:ehcache-core` from the gsec
dependency. When the gsec dep is removed in Phase A, that exclusion goes
away with it. Ehcache wiring lives in `GemmaAclConfiguration` (the
`SpringCacheBasedAclCache` over `ConcurrentMapCache`) — unaffected.

---

## Summary

| Phase | Sessions | LoC moved/touched | Risk | Validation |
|---|---:|---:|---|---|
| A: copy + drop dep | ~2 | 7126 moved + 150 in-source | Low | mvn verify + integration |
| B: unify Sids, drop adapter | ~2 | ~500 | Medium | ACL write-path integration |
| C: inline XML | ~1 | ~500 (Java) − 481 (XML) | Medium | bean-presence assertion |
| D: rename packages | ~1 | 7126 renamed | Low (mechanical) | integration |

Total estimated: ~6 sessions to fully absorb and rename. After Phase D, the
`pavlab:gemma-gsec` dependency is gone, `applicationContext-gsec.xml` is gone,
`GsecAclServiceAdapter` is gone, the parallel-Sid problem is gone, and every
gsec class lives at its final home in `ubic.gemma.*`.
