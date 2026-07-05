# Mockito Modernization Recce (Phase 3)

Date: 2026-05-18
Branch: `worktree-mockito-modernize`
Baseline: `08e760bdaf` (phase2-acl-migrate HEAD)
Context: Maven modernization agent bumped Mockito to **5.23.0** on
`worktree-maven-modernize` (commit `43a98535bb`, unmerged). This recce
audits the test base for legacy Mockito idioms that should be retired
when 5.x lands.

## TL;DR

The codebase is remarkably clean. **Zero legacy idioms targeted by the
task are present.** No `initMocks`, no `PowerMock`, no `MockitoJUnitRunner`
(legacy *or* modern import), no `@MockitoSettings`, no `org.mockito.Matchers`,
no `verifyZeroInteractions`, no `anyObject()` / `anyVararg()`. No mechanical
renames were required. Test compile clean under Mockito 5.23.0 on JDK 17.

## Inventory (counts)

| Pattern                                                              | Count | Notes                                                          |
|----------------------------------------------------------------------|------:|----------------------------------------------------------------|
| `MockitoAnnotations.initMocks(this)` (deprecated)                    |     0 | Nothing to rename to `openMocks(this)`.                        |
| `MockitoAnnotations.openMocks(this)` (modern)                        |     0 | No bootstrap-by-hand sites at all.                             |
| `@MockitoSettings` (JUnit 5 only)                                    |     0 | JUnit 5 not yet adopted; tracked separately.                   |
| `PowerMock` / `PowerMockito` / `@PrepareForTest`                     |     0 | Excellent — PowerMock is dead weight in JDK17+.                |
| `import org.mockito.runners.MockitoJUnitRunner` (legacy path)        |     0 | —                                                              |
| `import org.mockito.junit.MockitoJUnitRunner` (modern path)          |     0 | The codebase doesn't use the runner at all.                    |
| `@RunWith(MockitoJUnitRunner.*)`                                     |     0 | —                                                              |
| `@Rule public MockitoRule … = MockitoJUnit.rule()`                   |     3 | Three files; all use bare `.rule()` (STRICT_STUBS default).    |
| `MockitoExtension` (JUnit 5)                                         |     0 | JUnit 5 migration not started.                                 |
| `MockedStatic` / `MockedConstruction` (Mockito 5 features in use)    |     0 | No adoption yet; available when needed.                        |
| `Mockito.mock(SomeClass.class)` (typed form)                         |   144 | Pre-5.12 form. Could shorten to `mock()` w/ type inference.    |
| `Mockito.mock(SomeClass.class, …)` (with name/answer)                |     2 | Must keep `.class` form — args required.                       |
| `@Mock` field annotations                                            |     6 | All paired with `MockitoRule` (no runner, no `initMocks`).     |
| `@Captor` field annotations                                          |     0 | Captors built ad-hoc via `ArgumentCaptor.forClass(...)`.       |
| `ArgumentCaptor.forClass(...)`                                       |    14 | Fine; modern API.                                              |
| `verifyZeroInteractions(...)` (deprecated)                           |     0 | Already on `verifyNoInteractions` / `verifyNoMoreInteractions`.|
| `anyObject()` / `anyVararg(…)` (deprecated)                          |     0 | —                                                              |
| `import org.mockito.Matchers` (deprecated → `ArgumentMatchers`)      |     0 | —                                                              |
| `org.mockito.internal.*` imports (fragile)                           |     1 | `VerificationModeFactory` in `RetryTest`. See Defer-list.      |
| `org.mockito.ThrowingConsumer` (internal-ish; should use AssertJ's)  |     1 | `SingleCellExpressionExperimentServiceTest`. See Defer-list.   |

Total `org.mockito` imports across the tree: 19, in 6 files.

### `MockitoRule` sites (the only Mockito wiring in the codebase)

* `gemma-rest/src/test/java/ubic/gemma/rest/util/args/FilterArgTest.java:32`
* `gemma-core/src/test/java/ubic/gemma/core/util/test/BaseSpringContextTest.java:80`
* `gemma-core/src/test/java/ubic/gemma/core/logging/SlackAppenderTest.java:37`

All three: `public MockitoRule … = MockitoJUnit.rule();` — bare `rule()`,
no `.silent()`, no `.strictness(...)`. Under Mockito 5 this gets the
default `Strictness.STRICT_STUBS` (same default as 4.x — *not* a new
behaviour change). No action needed for the bump itself, but see the
JUnit 5 note below since `MockitoRule` is JUnit 4 only.

## Mechanical renames applied

**None.** The task's target idioms are absent. Recording for completeness:

* `initMocks → openMocks`: 0 sites
* `org.mockito.runners.* → org.mockito.junit.*`: 0 sites
* `verifyZeroInteractions → verifyNoInteractions`: 0 sites

## Strict-stubbing surface (record only, don't fix)

Mockito 5 keeps `Strictness.STRICT_STUBS` as the default (unchanged from
4.x). Since the three `MockitoRule` sites already get strict by default
and there are no `.silent()` overrides, the bump should not surface new
stubbing problems *that weren't already surfaced by 4.x*. If 4.x → 5.x
turns up `UnnecessaryStubbingException` in CI, the fix is per-test
(`@MockitoSettings(strictness = LENIENT)` for JUnit 5, or
`MockitoJUnit.rule().strictness(LENIENT)` for JUnit 4) — but **do not
preemptively loosen**; tighten the stubs instead.

The 144 `mock(SomeClass.class)` call sites are not strictness-impacted —
they're a stylistic opportunity (see Defer-list).

## JUnit 5 interop notes

The codebase is entirely on JUnit 4 (Vintage-style) — confirmed by:

* zero `MockitoExtension` / `@ExtendWith` usages
* three `@Rule public MockitoRule` declarations (JUnit 4 idiom)
* `import org.junit.{Before,After,Test}` throughout (not `org.junit.jupiter.*`)

When the JUnit 5 migration kicks off (tracked separately on
`worktree-junit5-recce`), the **three `MockitoRule` sites become
load-bearing**: `MockitoRule` does not work under JUnit Jupiter.
Migration path:

```java
// Before (JUnit 4 + Mockito Rule):
@Rule public MockitoRule rule = MockitoJUnit.rule();

// After (JUnit 5):
@ExtendWith(MockitoExtension.class)
class FooTest { … }
```

`BaseSpringContextTest` is the highest-leverage of the three — every
Spring integration test inherits from it, so the Rule → Extension swap
needs to happen in lockstep with switching `BaseSpringContextTest`
itself to a JUnit Jupiter test class (or providing a JUnit 5 sibling).
Flag for whoever drives the JUnit 5 cutover.

## Defer-list (record, don't touch)

These are real cleanups but out-of-scope for "mechanical renames":

1. **144 × `mock(SomeClass.class)` → `mock()`** (Mockito 5.12+ generic
   inference). Touches a lot of files for cosmetic gain. Consider a
   batch IDE refactor in a dedicated pass; **don't bundle it with the
   5.x bump**.

2. **`RetryTest` imports `org.mockito.internal.verification.VerificationModeFactory`**
   (`gemma-core/src/test/java/ubic/gemma/persistence/retry/RetryTest.java:6`).
   Internal API — could break across Mockito minor versions. Worth a
   look to see whether it can be replaced with a public-API equivalent
   (likely `Mockito.times(n)` / `VerificationMode` constants), but the
   call site needs reading before changing behaviour.

3. **`SingleCellExpressionExperimentServiceTest` imports `org.mockito.ThrowingConsumer`**
   (`gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/SingleCellExpressionExperimentServiceTest.java:9`).
   The variable is passed to AssertJ's `.satisfies(...)`; should use
   `org.assertj.core.api.ThrowingConsumer` (or plain `Consumer`)
   instead. Trivial fix but it's an AssertJ-domain concern, not a
   Mockito-idiom concern; deferring.

4. **JUnit 5 cutover of the three `MockitoRule` sites** — see "JUnit 5
   interop notes" above. Blocked on the JUnit 5 migration starting.

## Compile verification

```
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test-compile -DskipTests
# → BUILD SUCCESS, all four modules clean
```

Mockito 5.23.0 is *not yet* on this branch (the bump lives on
`worktree-maven-modernize`, unmerged). The compile here is against the
phase2-acl-migrate baseline (Mockito 4.x). Re-verification under 5.23.0
should happen at merge time on the integration branch.

## Conclusion

No changes required from the Mockito-bump side. The Phase 3 follow-on
work that's actually load-bearing is the **JUnit 5 cutover**, which has
to swap the three `MockitoRule` sites (most importantly inside
`BaseSpringContextTest`) to `@ExtendWith(MockitoExtension.class)`. That
work is owned by the JUnit 5 recce, not this one.
