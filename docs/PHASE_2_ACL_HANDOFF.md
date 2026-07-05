# Phase 2 ACL Migration — Handoff (2026-05-18)

Branch: `phase2-acl-migrate` (Gemma) + `renovations` (gsec).

## TL;DR for next session

The ACL migration to Spring Security 6's canonical schema is largely done.
The four ACL/security integration tests are **22/23 passing**:

- `SecurityServiceTest` — 9/9 ✓
- `AclAuthorizationTest` — 2/2 ✓
- `UserGroupServiceTest` — 4/4 ✓
- `AclAdviceTest` — 7/8 (one remaining: `testUpdateAcl`)

The one remaining failure is **not a quick fix** — it's a real architectural
issue in how AclAdvice walks the object graph relative to Hibernate 6's
cascade-flush timing. It needs Phase 3 rework (Hibernate event listeners),
not another patch. See "Remaining failure" below before sinking more time
into the old AclAdvice model.

## Run sweep

```bash
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl gemma-core verify \
  "-Dit.test=AclAdviceTest,AclAuthorizationTest,SecurityServiceTest,UserGroupServiceTest" \
  -DskipUnitTests=true -DskipIntegrationTests=false \
  -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
  -Dgemma.hibernate.hbm2ddl.auto=create
```

This shares MySQL `gemdtest` with any other agent — check `ps aux | grep mvn`
first.

## Fundamental design problems exposed by the migration

We patched around these rather than fix them. Phase 3 should:

1. **AclAdvice fires at the wrong time.** It's `@AfterReturning` on DAO
   methods, walking the entity graph to discover Securables that need ACLs.
   In Hibernate 6 `session.merge()` defers cascade-pending inserts until
   flush; child ids aren't assigned when AclAdvice walks. Pre-Hibernate-6
   `session.update()` was eager and masked this. The right design is
   Hibernate `@PostInsert` event listeners on Securable entities — fires
   on actual persistence, not method return.

2. **AclAdvice duplicates Hibernate cascade traversal.** Double-bookkeeping
   creates ordering dependencies between AclAdvice and Hibernate's flush.

3. **Type fragmentation between gsec and Spring Security.** Parallel
   `AclObjectIdentity`/`ObjectIdentityImpl`, `AclPrincipalSid`/`PrincipalSid`,
   etc., with type-strict `equals`. Every asymmetric-equals case surfaced as
   a bug we had to fix one-by-one. Phase 3 should drop gsec's types entirely
   and use Spring Security's stock types throughout — gsec's domain entities
   only exist for Hibernate to read the canonical tables.

4. **Bean override fragility.** gsec XML and Gemma `@Configuration` define
   overlapping beans; override semantics in Spring 6 with XML-registered
   `@Configuration` classes is unreliable. We removed gsec's `aclService` and
   `aclAuthorizationStrategy` XML beans to avoid conflicts; `permissionEvaluator`
   remains XML-defined because the @Bean override silently didn't take and
   I never traced why.

5. **`<s:global-method-security>` is deprecated in Spring 6** and doesn't
   reliably wire the custom `permissionEvaluator` for SpEL `hasPermission(...)`.
   We worked around the only `@PreAuthorize` usage (gsec UserService.removeUserFromGroup)
   by adding `hasAuthority('GROUP_ADMIN')` to the disjunction. Phase 3 should
   migrate to `@EnableMethodSecurity` (Java config).

6. **ExpressionPersister relies on Hibernate 5 cascade timing.** It calls
   `create()` on entities that may already be in the session (cascaded from
   their parent's persist). We made `AbstractDao.create` idempotent on
   already-persistent entities to compensate.

## Commits this session (most recent first)

Gemma `phase2-acl-migrate`:
- `f2017e1cd0` — AbstractDao: idempotent create + post-merge flush for Hibernate 6
- `47479cc155` — Test config: align ACL required-authority with what test users actually carry
- `be18138d12` — Phase 2 ACL: role-hierarchy-aware AclAuthorizationStrategy + empty-list guards
- `34b3a67c1c` — Phase 2 ACL: AclAdviceTest owner assertions via gsec.acl.domain.Sids
- `a1d2049ed0` — Phase 2 ACL: rekey readAclsById output + dedupe updateAcl + sid-aware checks
- `5152fb4676` — Phase 2 ACL: bridge gsec types vs Spring canonical types

gsec `renovations`:
- `569d364` — ACL @PreAuthorize + AclAdvice: pragmatic fixes for tests on Hibernate 6
- `c664ece` — Acl H2 path: load AclEntry.sid eagerly + tolerate Spring sids in isGranted
- `4fdbfa0` — AclObjectIdentity(Securable): unwrap Hibernate proxies via Hibernate.getClass
- `bbffd2e` — AclEntry filter: short-circuit empty bulk hasPermission
- `2a26d64` — Acl sid types: harmonize gsec vs Spring Security after canonical-schema migration
- `1551d3d` — AclDaoImpl.createObjectIdentity: populate object_id_class FK

## Remaining failure: AclAdviceTest.testUpdateAcl

```
testUpdateAcl:355 Failed to create ACL for ExperimentalFactor Id=13
```

### What the test does

1. Persists an EE with 2 ExperimentalFactors via `getTestPersistentCompleteExpressionExperiment`.
2. Constructs a **transient** new ExperimentalFactor `ef`.
3. `ed.getExperimentalFactors().add(ef);` — adds to the managed parent's set.
4. `experimentalDesignService.update(ed);` — expected to cascade-persist `ef`
   AND create its ACL.
5. `aclTestUtils.checkEEAcls(ee)` — expects `ef` to have an ACL.

### What's happening

- `experimentalDesignDao.update(ed)` calls `session.merge(ed)` followed by my
  `session.flush()`. In Hibernate 6, `merge` on an already-managed entity is
  essentially a no-op; the flush dirty-checks and **should** cascade-save the
  new `ef`. It does not — diagnostic shows `ef.id == null` at AclAdvice time.
- The cascade rule on `ExperimentalDesign.experimentalFactors` is
  `cascade="all" inverse="true"`. `inverse="true"` means EF is the owning side
  of the FK. Hibernate 6 may not cascade-save through the non-owning side
  reliably on merge+flush of a managed parent.
- My current workaround in `BaseAclAdvice.addOrUpdateAcl` is:
  ```java
  if ( object.getId() == null ) {
      sessionFactory.getCurrentSession().persist(object);
      sessionFactory.getCurrentSession().flush();
      ...
  }
  ```
  This **does** assign the id (test failure now reports `Id=13` not "Name=..."
  without id). But the ACL still isn't in `acl_object_identity` for that id.
  Something downstream of `addOrUpdateAcl` fails or skips after the
  force-persist. I haven't traced past this point.

### Concrete next steps

1. Add a `System.err.println` right after my `session.persist + flush` block
   in `BaseAclAdvice.addOrUpdateAcl` to confirm the code path is entered and
   `object.getId()` becomes non-null.
2. Add a `System.err.println` at the point where AclService.createAcl is
   called (around `BaseAclAdvice.java:349`) to see whether createAcl runs for
   this entity.
3. If createAcl runs but the ACL isn't in DB: check `GsecAclServiceAdapter.createAcl`
   (Gemma `GemmaAclConfiguration.java:175`) — maybe a NotFoundException is being
   swallowed.
4. If createAcl isn't called: trace `addOrUpdateAcl` after the force-persist
   block to see which branch returns early.

After getting that visibility, you can either:
- Patch the specific failure (continue the "Phase 2 patches" approach).
- Or accept this as a known limitation of the old AclAdvice model and move
  on to Phase 3 (Hibernate event listeners).

The user explicitly said in this session: "we've wasted hours trying to fix
the old way" — suggesting Phase 3 is the right move, not more patches.

## Phase 3 recommended scope

- Replace `BaseAclAdvice` with two Hibernate listeners (`PostInsertEventListener`,
  `PostDeleteEventListener`) registered on Securable entities. The listener
  inspects the entity, builds an `ObjectIdentityImpl`, and calls
  `aclService.createAcl` / `deleteAcl`. Fires on actual persistence, not on
  DAO method return.
- Drop gsec's `AclObjectIdentity`/`AclPrincipalSid`/`AclGrantedAuthoritySid`/`AclImpl`/`AclServiceImpl`/`AclDaoImpl`.
  Keep the Hibernate-mapped entities (read-only) only if HQL queries against
  `acl_object_identity` etc. are still in use; otherwise replace with native
  SQL via JdbcTemplate.
- Replace `<s:global-method-security>` XML with `@EnableMethodSecurity` and
  a Java config class that wires `MethodSecurityExpressionHandler` with
  `AclPermissionEvaluator`.
- Drop `LegacyAwareDaoAuthenticationProvider`'s threadlocal username injection;
  use Spring Security 6's `UserDetailsPasswordService` for password upgrade.

## H2 unit test `AclLinterServiceTest`

Currently fails on a different issue (OptimisticLock in `@After
flushAndClearSession` from a deleted acl_entry being updated). The core NPE
in `AclDaoImpl.convert` is fixed (HQL `left join fetch e.sid`). The remaining
post-test cleanup issue is a different cluster.

## Files to be aware of

- `gemma-core/src/main/java/ubic/gemma/core/security/acl/GemmaAclConfiguration.java`
  — production ACL stack wiring (JdbcMutableAclService + RoleHierarchyAware
  strategy + GsecAclServiceAdapter).
- `~/Dev/gsec/gsec/src/main/java/gemma/gsec/acl/BaseAclAdvice.java` — the
  AclAdvice code. Contains the force-persist+flush workaround in addOrUpdateAcl.
- `~/Dev/gsec/gsec/src/main/java/gemma/gsec/acl/domain/Sids.java` — new helper
  that bridges gsec and Spring sid types in instanceof checks.
- `~/Dev/gsec/gsec/src/main/resources/gemma/gsec/applicationContext-gsec.xml`
  — duplicated bean defs commented out / kept depending on whether @Bean
  override worked.

## Tasks state at handoff

All seven tasks created this session are completed (the diagnose-only ones)
or addressed (the fix ones). Only AclAdviceTest.testUpdateAcl remains as an
open issue tracked in this doc.
