# gsec Phase C recce — drop `GsecAclServiceAdapter`

Branch: `gsec-phase-C` off `phase2-acl-migrate` (HEAD `95e9e70fc6`).
Status: **recce, no code changes**. Adapter retained.

Phase A (copy + drop gsec dep), B (unify Sid types — drop `implements
Sid` from the entity-mapped subclasses) and D (rename `gemma.gsec.*` →
`ubic.gemma.core.security.*`) have landed. Phase C as scoped in
`GSEC_ABSORPTION_ROADMAP.md` Section 3.2 — drop the adapter, drop the
`AclService` extension interface, use stock `MutableAclService`
everywhere — is the residual.

The session close note (2026-05-19) flagged this with: "Phase C
deferred — adapter still has real semantics." This recce enumerates
those semantics, what would need to change to drop each, and the
ordering / validation that the migration requires.

---

## 1. What the adapter actually does

`GemmaAclConfiguration$GsecAclServiceAdapter` (inner class, ~210
lines) wraps a `JdbcMutableAclService` and implements the
gsec-extended `AclService` interface
(`ubic.gemma.core.security.acl.domain.AclService`, which extends
Spring's `MutableAclService` with three Gemma-only methods). It
encodes five distinct behaviours; three of them are real semantics
that must be preserved, two are no-op legacy stubs.

### 1.1 `dedupeEntries(MutableAcl)` in `updateAcl` — REAL

Walks the ACL's entries from the tail, deletes any ACE whose
`(sidKey, mask, granting)` triple has already been seen.

Why it exists: gsec's pre-migration `AclDaoImpl.update` deduplicated
implicitly via a `Set<AclEntry>` with content-based `equals` on
`(granting, mask, sid)`. Business code — most notably
`SecurityServiceImpl.makeReadableByGroup` — relies on that contract
to avoid compounding duplicate ACEs on repeated grants. Spring
Security's stock `AclImpl.insertAce` doesn't dedupe and
`JdbcMutableAclService.updateAcl` writes whatever it's given.

Callers exercising this path:
- `SecurityServiceImpl.makeReadableByGroup` (and the broader
  grant/revoke transitive walk in `SecurityServiceImpl`)
- `SecurityController.makeReadableByGroup` (web side, 4 call sites)
- Tests: `SecurityServiceTest`, `UserGroupServiceTest`,
  `AclAdviceTest`

Migration options:
- **(a) Wrap `JdbcMutableAclService` in a Spring-typed
  `DedupingMutableAclService`** that overrides `updateAcl` to dedupe
  before delegating. Same behaviour, but it's now a stock-typed bean
  and the gsec `AclService` interface stops being needed on this
  axis. Mechanically simple; cleanest.
- **(b) Dedupe at the call sites that create duplicates.** Audit
  `SecurityServiceImpl` paths that call `insertAce` and add a
  pre-check. Riskier — there are several methods and the gsec
  behaviour was a single chokepoint.

Recommend (a). It's a 30-line decorator class.

### 1.2 `normalize(ObjectIdentity)` and `rekey(...)` — REAL

`ObjectIdentityImpl.equals` does an `instanceof ObjectIdentityImpl`
check; gsec's `AclObjectIdentity` is a different class implementing
the same interface, so the equality fails one-way. `hashCode` is
also asymmetric (`31*type + identifier` vs `Objects.hash(type,
identifier)`).

Spring's `BasicLookupStrategy` keys its result `Map<ObjectIdentity,
Acl>` by `ObjectIdentityImpl` instances built from DB rows; if the
caller passes in an `AclObjectIdentity`, `containsKey` fails and the
service throws `NotFoundException` even though the row exists.

The adapter normalises every inbound OID to `ObjectIdentityImpl`
before handing to the delegate, then rekeys the result map by the
caller's original OID instances.

Why it exists: many production paths construct
`AclObjectIdentity` directly:
- `ObjectIdentityRetrievalStrategyImpl` — the canonical
  `@Bean(name="objectIdentityRetrievalStrategy")` used by the entire
  method-security expression handler, voter chain, after-invocation
  providers, `AclAdvice`, etc. **Every ACL lookup driven by Spring
  Security's expression evaluator flows through this strategy and
  produces an `AclObjectIdentity`.**
- `AclLinterServiceImpl` — 4 call sites
- `ParentIdentityRetrievalStrategyImpl` — parent-link discovery
- Many tests

Replacing `AclObjectIdentity` with `ObjectIdentityImpl` at all
construction sites is the alternative; roadmap Section 3.2 suggests
this. Audit count: ~10 production call sites + ~10 test call sites.
Concern: `AclObjectIdentity` is the Hibernate-mapped entity used by
the HQL boundary (`AclQueryUtils`); the type is doing double duty as
"HQL row" AND "security-path identifier." Roadmap Section 3.1
("Phase B target shape") proposed renaming `AclSid` →
`AclSidEntity` etc. and removing `implements Sid` to break that
double duty. Phase B partially landed (the `implements Sid` removal)
but the symmetric move on `AclObjectIdentity` did NOT happen — the
class still implements `ObjectIdentity` AND is Hibernate-mapped.

So the normalize / rekey workaround is the *price of not finishing
the OID side of Phase B*. To drop the adapter cleanly, one of:
- **(a) Finish Phase B on the OID side**: rename
  `AclObjectIdentity` → `AclObjectIdentityEntity`, remove
  `implements ObjectIdentity`, add a `toObjectIdentity()` returning a
  fresh `ObjectIdentityImpl`. Then every production site that
  currently does `new AclObjectIdentity(securable)` instead does
  `new ObjectIdentityImpl(type, id)`. ~30 file edits, all
  mechanical. HQL queries keep using the entity.
- **(b) Keep the normalize/rekey behaviour but inline it into a
  Spring-typed wrapper.** Same as option (a) for §1.1 — fold the
  normalize+rekey into the `DedupingMutableAclService` decorator (or
  rename to `GemmaMutableAclService`). The decorator implements
  stock `MutableAclService`, so the gsec interface still goes away.

Recommend (b) for this session's purposes; (a) is the
longer-term-correct move but it's a separate edit. (b) preserves
behaviour exactly with no surface change.

### 1.3 Empty-list short-circuit in `readAclsById(List, List)` — REAL

Spring's `BasicLookupStrategy.readAclsById` asserts `notEmpty(objects)`.
After-invocation collection filters can legitimately end up with an
empty list (e.g. when an association extractor filters out all
targets). Adapter short-circuits to an empty map instead of throwing.

Migration: same wrapper. Three lines.

### 1.4 `deleteSid(Sid)` JDBC — REAL

One production caller: `UserServiceImpl.deleteGroup` calls
`aclService.deleteSid(new GrantedAuthoritySid(authority))` when
deleting a user group. The adapter issues raw JDBC against
`acl_sid` / `acl_entry` / `acl_object_identity` to clean up ACEs
and null out owner FKs.

This method is NOT on Spring's `MutableAclService` interface — it's
a gsec extension. Migration:
- Move the JDBC into a small `AclMaintenanceService` (or fold into
  `SecurityServiceImpl` since `UserServiceImpl` already
  `@Autowired` injects `SecurityService`).
- `UserServiceImpl` injects the new service instead of using
  `aclService.deleteSid`.

Trivial. One method moves; one caller updates.

### 1.5 `readAclById(oid, Session)` and `openSession()` — NO-OP STUBS

The adapter's implementations are:
- `openSession()` returns `null`
- `readAclById(oid, Session)` ignores the session arg and delegates
  to `readAclById(oid)`

Callers (Production):
- `core.security.acl.afterinvocation.AclEntryAfterInvocationStreamFilteringProvider` (one of two copies)
- `core.security.authorization.acl.AclEntryAfterInvocationStreamFilteringProvider` (the Phase 3 port of the same class)

Both call `aclService.openSession()`, thread the (null) session into
`aclService.readAclById(oid, session)` for each stream element, and
`session::close` on stream close (which NPEs against null — but the
calling stream is closed via `try-with-resources` higher up; the
NPE would happen if a stream is *actually* closed; needs check).

Migration: drop both calls, use plain `readAclById(oid)` in the
filter predicate. The "fresh session per stream" intent is dead —
JDBC manages its own connections, there is no Hibernate session
contention to avoid.

Caveat: `session::close` on a null Session — `Session.close()`
throws nothing, but `null::close` would NPE. Check current behaviour:
the lambda is `onClose(session::close)` — Java evaluates `session`
at the time the method reference is captured, which is when
`openSession()` returns null, so this captures `null::close` which
will NPE when the stream is closed. Either this code path is never
exercised (streams not closed), or there's a bug here today. Either
way it gets cleaner after this migration.

### 1.6 `AclService` interface — REMOVABLE

The `gsec.acl.domain.AclService` interface (now under
`ubic.gemma.core.security.acl.domain`) adds three methods to
Spring's `MutableAclService`:
- `readAclById(ObjectIdentity, Session)` — kill (§1.5)
- `openSession()` — kill (§1.5)
- `deleteSid(Sid)` — move out (§1.4)

Once those three are gone, the interface has no members. Delete it,
along with the dormant `AclServiceImpl` (Hibernate-backed legacy
impl, no longer wired). Every `@Autowired AclService` becomes
`@Autowired MutableAclService`.

Audit: 13 fields/parameters typed as the gsec `AclService` interface
(11 in production, 2 in tests-only — see `AclEntryMapVoter`,
`AclEntryMapValueVoter`, `BaseAclAdvice`, `AclAdvice`, the two
`AclEntryAfterInvocationStreamFilteringProvider` copies,
`AclLinterServiceImpl`, `SecurityServiceImpl`, `UserServiceImpl`,
plus test fixtures). All become `MutableAclService` field-type
changes — one-line each.

---

## 2. Net migration plan

### Order

1. **Add `DedupingNormalizingMutableAclService` decorator** in
   `ubic.gemma.core.security.acl`. Implements
   `MutableAclService`. Constructor takes the underlying
   `JdbcMutableAclService` and `DataSource`. Implements:
   - `updateAcl`: dedupe, then delegate. (§1.1)
   - All `readAclById`/`readAclsById`/`createAcl`/`deleteAcl`/`findChildren`:
     normalize OIDs in, rekey result map out. (§1.2, §1.3)
2. **Move `deleteSid` JDBC** out of the adapter into a new
   `AclMaintenanceService` (or as a method on `SecurityServiceImpl`).
   Update `UserServiceImpl` to call the new service. (§1.4)
3. **Drop `openSession()`/`readAclById(oid, Session)` calls** from
   both `AclEntryAfterInvocationStreamFilteringProvider` copies.
   Switch to plain `readAclById(oid)` in the filter. (§1.5)
4. **Rebind the `aclService` bean** in `GemmaAclConfiguration`
   to construct + return the decorator typed as `MutableAclService`
   (not the gsec `AclService` interface).
5. **Field-type sweep**: 13+ `@Autowired AclService` →
   `@Autowired MutableAclService`. (§1.6)
6. **Delete** the gsec `AclService` interface, `AclServiceImpl`, the
   inner `GsecAclServiceAdapter` class, all related dead imports.

### Files touched (estimate)

- New: 1 file (`DedupingNormalizingMutableAclService.java`,
  ~120 LoC).
- New: 1 file (`AclMaintenanceService.java`, ~50 LoC).
- Modified: ~17 files (the 13 `AclService` field types + 2 stream
  providers + `GemmaAclConfiguration` + `UserServiceImpl`).
- Deleted: 2 files (`AclService.java`, `AclServiceImpl.java`).

Total ~20 files; the actual diff is dominated by import-list
changes.

### Behaviour preserved

- `updateAcl` still dedupes ACEs by `(sid, mask, granting)`.
- `readAclById`/`readAclsById` still tolerate `AclObjectIdentity`
  inputs (until §1.2 option (a) is taken later).
- `readAclsById` with empty list still returns empty map instead of
  throwing.
- `deleteSid` still works, just via a different bean.
- Stream-after-invocation still reads ACLs per-element, just without
  the `openSession()` ceremony.

### Behaviour changed

- `openSession()` on the ACL service ceases to exist. The two
  callers (both `AclEntryAfterInvocationStreamFilteringProvider`
  copies) lose the `onClose(session::close)` — which is a no-op /
  latent bug today anyway.
- Anyone depending on the runtime type of the `aclService` bean
  being the gsec `AclService` interface breaks at compile time.
  The sweep catches all of them.

---

## 3. Risk + validation

Roadmap labels Phase B (which is the umbrella that includes "drop
adapter") **Medium risk**. The risk surface is exactly the three
real-semantics items above:

- **Dedupe drift** would manifest as compounding duplicate ACEs in
  `acl_entry` on repeated `makeReadableByGroup` calls. Symptom:
  permission grants succeed but the UI shows duplicate group entries
  on the permissions panel, or a "revoke" leaves orphan rows.
- **Normalize drift** would manifest as `NotFoundException` thrown
  from `AclService.readAclById` for objects whose ACL row exists.
  Symptom: random "access denied"-ish errors in any code path that
  builds an `AclObjectIdentity` directly (e.g. via the
  identity-retrieval strategy).
- **Empty-list drift** would manifest as
  `IllegalArgumentException("notEmpty")` from the after-invocation
  collection filters when an upstream filter empties the list.
  Symptom: an EE list page or REST endpoint 500s when no items pass
  a pre-filter.

None of these are unit-testable in isolation: the failure modes are
in the JDBC + Hibernate + Spring-Security stack interactions. The
validation bar this session's brief gave me — `mvn -pl gemma-core
compile test-compile -q` plus targeted ACL/Security/GsecAclService
unit tests — does NOT exercise these paths. The roadmap's Phase B
validation says explicitly: "ACL write tests in
`ubic.gemma.core.security.*Test` cover create / update /
duplicate-grant / delete-sid paths" — those are integration tests
against `gemdtest`.

The brief also explicitly forbids running `mvn verify`.

**This is why the recce, not the migration.** The migration is
mechanical and low-effort (~3 sessions of code, mostly mechanical
sweeps); the *validation* is the long pole and requires
single-tenant access to `gemdtest` and serialization with the other
Phase 3 agents.

### Recommended next session

1. Land the `DedupingNormalizingMutableAclService` decorator and
   the `AclMaintenanceService` extraction on a separate branch off
   `phase2-acl-migrate`. Get a `gemdtest` window. Run the full
   ACL/Security integration suite:
   - `mvn -pl gemma-core test -Dtest='*Acl*,*Security*'` with
     `gemdtest` profile active.
   - Manual: per-EE create + delete + grant + revoke +
     re-grant round-trip; verify `acl_entry` has no duplicate rows.
2. If green, then do the field-type sweep and delete the gsec
   interface + adapter + `AclServiceImpl`. Second commit; second
   integration run.
3. Optional follow-up (own session): finish the OID side of Phase B
   by renaming `AclObjectIdentity` → `AclObjectIdentityEntity` and
   replacing call-site constructors with `ObjectIdentityImpl`. Once
   that lands, the `normalize`/`rekey` behaviour in the decorator
   becomes dead code and can be deleted, leaving only the dedupe and
   empty-list short-circuit. The decorator survives but its surface
   shrinks.

### Effort estimate

- Decorator + maintenance service + field-type sweep + adapter
  deletion: **1 session of code**, ~20 files.
- Validation pass: **1 `gemdtest` window** (~30 min of clock time)
  plus debugging budget for any surprises (~1 session).

Total: **~2 sessions, gated on a `gemdtest` window**.

---

## 4. Why this is non-blocking for Gemma 2.0

The adapter is a 210-line internal class. It compiles, runs, passes
all current tests (134 ACL/Security/AclLinter/etc. tests green after
Phase D per the session close note), and its behaviour is documented
inline. It is not a security risk, not a correctness risk, and not
a maintenance burden. The only reasons to remove it are:

- the gsec `AclService` interface is the last surface that
  prevents typing the ACL bean as plain `MutableAclService`
  (aesthetic + simplifying-the-graph)
- the `openSession()` no-op is mildly misleading inside the two
  after-invocation stream providers
- deleting `AclServiceImpl` (legacy gsec impl, currently dead code)
  removes a 195-line file

None of those are 2.0-blocking. Keep the adapter; ship 2.0; do
Phase C as a 2.0.x or 2.1 cleanup with proper integration coverage.

---

End of recce.
