# `testStreamExperiments` ACL path recce

Worktree: `.claude/worktrees/agent-stream-exp-1779522547`
Branch: `agent-stream-experiments-acl-v2`
Baseline: `c01ecffca4`
Status: blocked on production-code defect; **no commit made** — see "Why no fix landed" below.

## Test under investigation

`gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceIntegrationTest.java#testStreamExperiments`
(`ExpressionExperimentServiceIntegrationTest` — extends `BaseSpringContextTest5`, runs under failsafe).

The test exercises `ExpressionExperimentService.streamAll(true)` (the only call site of `streamAll(boolean)` in the test tree) under four security contexts: admin, bob, joe, anonymous. It expects per-user ACL filtering of the streamed EEs.

## Two distinct defects uncovered

### Defect 1 — test setup is unrunnable as authored (test-side)

The test calls `runAsUser("bob")` then immediately
`getTestPersistentBasicExpressionExperiment()`. That helper transitively calls
`externalDatabaseService.findOrCreate(...)` which is `@Secured("GROUP_ADMIN")`.
`runAsUser("bob")` grants only `GROUP_USER`, so the helper throws
`AccessDeniedException` before any EE is persisted. Stack trace from the
failsafe run:

```
ExpressionExperimentServiceIntegrationTest.testStreamExperiments:577
  -> BaseSpringContextTest5.getTestPersistentBasicExpressionExperiment:236
  -> PersistentDummyObjectHelper.getTestPersistentBasicExpressionExperiment:606
  -> PersistentDummyObjectHelper.getTestPersistentDatabaseEntry:782
  -> externalDatabaseService.findOrCreate(ed)
  -> AccessDeniedException: Access is denied
```

The fix shape is well-precedented (see `SecureValueObjectAuthorizationTest`):
persist as admin, then transfer ownership via
`securityService.makeOwnedByUser(ee, "bob")` + `securityService.makePrivate(ee)`
before flipping the security context. The fix is ~16 lines, test-only.

### Defect 2 — `streamAll(true)` returns an empty stream under admin (production)

After applying Defect-1's fix (persist-as-admin + `makeOwnedByUser` to bob/joe),
the very first assertion in the test —

```java
runAsAdmin();
assertThat( expressionExperimentService.streamAll( true ) )
    .contains( bobExperiment, joeExperiment );
```

— fails with `ListFromStream: []`. The persisted EEs (with valid IDs) are
not visible in the stream **even under admin**, where the ACL after-invocation
filter should pass everything through.

Switching the same calls to `streamAll(false)` (current session, same ACL
provider) reproduces the access path but trips a different defect: the
`@Transactional(readOnly=true)` proxy on `streamAll` commits and releases the
JDBC connection before AssertJ consumes the stream, raising
`SQLException: Operation not allowed after ResultSet closed`.

Both failures are downstream of the same architectural fact: `streamAll`
returns a one-shot resource that has to outlive the service method's
transaction boundary. The `createNewSession=true` path (`SessionFactory.openSession()`)
is designed for exactly this — but the test demonstrates it isn't producing rows.

Suspected root cause (un-verified): `AclEntryAfterInvocationStreamFilteringProvider`
attaches `onClose(session::close)` to the *filtered* stream where `session` is
`aclService.openSession()` (returns `null` in the Gemma adapter — handled).
The DAO-side `S2 = sessionFactory.openSession()` is attached to the *original*
stream via `onClose(S2::close)` in `QueryUtils.createStream`. Stream
composition (`filter().filter().onClose()`) preserves close handlers, so close
order should be fine. But the underlying scroll cursor on `S2` may not survive
the Spring tx commit on the parent session if `S2.openSession()` actually
returns a session sharing the same JDBC connection via Spring's
`TransactionSynchronizationManager`. That would explain both the
`[]` (cursor abandoned post-commit) and the `ResultSet closed` errors.

Files in the blast radius:
- `gemma-core/src/main/java/ubic/gemma/persistence/service/AbstractDao.java` — `streamAll(boolean)`
- `gemma-core/src/main/java/ubic/gemma/persistence/util/QueryUtils.java` — `createStream` + `stream(Query, ...)`
- `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/AclEntryAfterInvocationStreamFilteringProvider.java` — onClose handler
- `gemma-core/src/main/java/ubic/gemma/persistence/service/AbstractService.java:182` — `@Transactional(readOnly=true)` on the `streamAll(boolean)` service entry

## Why no fix landed

Brief says: "If the fix balloons beyond ~50 lines or touches production code
beyond the streamExperiments call site, STOP and file a recce instead." The
test-only fix for Defect 1 is necessary but insufficient — Defect 2 is in the
production stream-ACL plumbing and needs broader review (transaction +
session-lifecycle interaction on the streaming path). Landing only the
test-side fix would surface Defect 2 as a *new* failure on the very next
`mvn verify`, replacing one always-failing test with another. Better to fix
both in one pass once Defect 2's root cause is settled.

## Reproduction (cheap, ~2 min focused failsafe)

```bash
cd .claude/worktrees/agent-stream-exp-1779522547
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="--enable-native-access=ALL-UNNAMED"
# Defect 1 (baseline test as-authored):
/usr/local/opt/maven/bin/mvn -pl gemma-core -DskipUnitTests \
    -Dit.test=ExpressionExperimentServiceIntegrationTest#testStreamExperiments \
    -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
    -Dgemma.hibernate.hbm2ddl.auto=create verify
# Apply the persist-as-admin + makeOwnedByUser patch (see Defect 1)
# and re-run the same command to surface Defect 2.
```

## Recommended next step

Sub-agent (or driver) takes both defects in one branch:

1. Apply Defect-1 test reshape (persist-as-admin → `makeOwnedByUser` → switch user).
2. Diagnose Defect 2:
   - Add a `count()` instrumentation before the first assertion to confirm zero rows.
   - Log the JDBC connection identity inside `streamAll(true)`'s opened session
     vs. the parent tx's connection — confirm/refute shared-connection hypothesis.
   - If shared: either detach S2 from `TransactionSynchronizationManager`, or
     keep the parent tx alive until the stream is consumed (less invasive: drop
     `@Transactional(readOnly=true)` from `AbstractService.streamAll(boolean)`
     when `createNewSession=true`, since S2 manages its own connection).
3. Compile-clean + focused failsafe must pass before commit.
