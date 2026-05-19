# AspectJ Deeper Audit (Phase 3, post-`ASPECTJ_EHCACHE_AUDIT`)

Reconnaissance only. No source changes. Baseline: `phase2-acl-migrate` HEAD
`08e760bdafb486a0b67705fb527ab8472d02d386`. Branch:
`worktree-aspectj-deeper`.

## TL;DR

- **Proxy mode: JDK dynamic proxies, interface-based.** Three Spring XML
  files enable `<aop:aspectj-autoproxy/>` with no attributes, and
  `<tx:annotation-driven>` is declared with no `proxy-target-class`. Result:
  every advised bean that implements at least one non-lifecycle interface
  is proxied via a JDK `$ProxyN` class. Beans with no business interface
  fall back to CGLIB.
- **Self-invocation bugs: zero confirmed HIGH-severity, zero MEDIUM.** 220
  self-invocation candidates flagged automatically, all 220 resolve to
  outer-`@Transactional` calling inner-`@Transactional` (propagation
  preserved, no functional bug). No `REQUIRES_NEW`, no `@Secured`, no
  `@Cacheable` inner-call patterns from a non-advised outer method.
- **Concrete-class autowire risk list: zero current instances.** No
  `@Autowired` field anywhere in main has a type ending in `Impl`, and no
  advised class is injected by its concrete type. The
  `EeWriteServiceImpl.persisterHelper` bug pattern is theoretically
  reachable in this codebase but no instance survives in this snapshot.
- **Top remediation:** standardize on interface-typed autowires (already
  enforced today); leave proxy mode as-is (JDK proxies are working);
  document the convention to keep it from regressing.

## 1. Proxy mode (JDK vs CGLIB)

### 1.1 Configuration found

```text
gemma-core/src/main/resources/ubic/gemma/applicationContext-hibernate.xml:24:
  <aop:aspectj-autoproxy/>
gemma-core/src/main/resources/ubic/gemma/applicationContext-hibernate.xml:72:
  <tx:annotation-driven order="3"/>
gemma-core/src/main/resources/ubic/gemma/applicationContext-security.xml:104:
  <aop:aspectj-autoproxy/>
gemma-core/src/main/resources/ubic/gemma/applicationContext-serviceBeans.xml:75:
  <aop:aspectj-autoproxy/>
```

`grep -rn "proxy-target-class"` over the whole tree returns nothing.
`grep -rn "AopContext.currentProxy\|expose-proxy\|preserveTargetClass"`
returns nothing. There is no `@EnableLoadTimeWeaving`, no
`<context:load-time-weaver/>`, no AspectJ compile-time weaving plugin in
the POM.

### 1.2 What that implies at runtime

Spring AOP defaults with `proxy-target-class` unset:

- If the target bean implements at least one user-defined interface ->
  JDK dynamic proxy implementing all of the bean's interfaces. The proxy
  is **not** assignable to the concrete class (`ClassCastException` /
  `BeanNotOfRequiredTypeException` when injected as the concrete type).
- If the target implements only Spring lifecycle interfaces
  (`InitializingBean`, `DisposableBean`, `BeanFactoryAware`, etc.) or
  no interfaces at all, Spring transparently falls back to CGLIB
  subclassing. Lifecycle interfaces are excluded from
  `AopUtils.getAllInterfacesForClass`.

Gemma's `*Service` / `*Dao` / `*Persister` beans all have a real
interface (`*Service`, `*Dao`, `Persister` / `PersisterHelper`), so the
JDK-proxy path is the dominant one. A handful of `@Component` utility
beans like `BuildInfo` implement only `InitializingBean` and are
CGLIB-proxied if advised (today they are not advised at all -- see s3).

### 1.3 Decision: leave as JDK proxies

CGLIB subclassing has well-known costs (final fields, no-arg
constructor requirement, GraalVM friction, larger class metadata). The
codebase has converged on the interface-everywhere pattern; switching
the proxy mode would only matter if we wanted to relax that pattern,
which we don't. **Recommendation: keep `proxy-target-class` unset.**

## 2. Self-invocation hot spots

Classic Spring AOP gotcha: `proxy.method()` triggers advice;
`this.method()` (or a bare `method()` call inside the same class) does
not. The inner method's `@Transactional`, `@Secured`, `@Cacheable`,
`@Retryable` advice is bypassed.

### 2.1 Method

Wrote a Python parser that, for every `*ServiceImpl.java` under
`gemma-core/src/main/java`:

1. Tokenises the class into methods + per-method annotation set
   (including class-level `@Transactional` propagated to every method).
2. For each `@Transactional`-or-other-AOP method, scans the body for
   self-invocations of any other AOP-advised method in the same class
   (both `this.foo(` and bare `foo(` forms).
3. Classifies each hit by severity:
   - **HIGH** - inner is `@Transactional(propagation = REQUIRES_NEW)`
     (silent propagation drop, real bug) or `@Secured` / `@PreAuthorize`
     (security advice bypassed, real bug).
   - **MEDIUM** - inner is `@Cacheable`/`@CacheEvict` (cache silently
     bypassed) or `@Transactional` from a *non*-transactional outer
     (inner tx never opens).
   - **LOW** - inner `@Transactional` from an outer that is also
     `@Transactional` (current tx propagates, no functional bug).

### 2.2 Results

| severity | count | comment |
|---|---:|---|
| HIGH | 0 | No `REQUIRES_NEW` self-invocations, no `@Secured` self-invocations |
| MEDIUM | 3 (false positives on re-inspection) | All three turn out to be either Assert message text containing a method name, or outer-overload calls where the outer overload IS `@Transactional` (parser missed class-level / multi-overload merging) |
| LOW | 220 | Inner-tx call from outer-tx method. Propagation = `REQUIRED` (default) means the inner annotation is effectively ignored at runtime since a tx is already open. Not a bug. |

### 2.3 Top files (raw self-invocation count)

These are all in the LOW bucket; listed because they would be the first
places to break if someone removed `@Transactional` from the outer
method:

```text
  33  SingleCellExpressionExperimentServiceImpl.java
  10  ExpressionExperimentServiceImpl.java
   6  GeneServiceImpl.java
   5  ExpressionExperimentBatchInformationServiceImpl.java
   5  ExpressionExperimentReportServiceImpl.java
   5  DifferentialExpressionAnalysisServiceImpl.java
   4  ExpressionAnalysisResultSetServiceImpl.java
   4  ProcessedExpressionDataVectorServiceImpl.java
   4  GeneSetServiceImpl.java
   3  UserServiceImpl.java
   3  SingleCellDataLoaderServiceImpl.java
   3  BibliographicReferenceServiceImpl.java
   2  SVDServiceImpl.java
   2  CompositeSequenceServiceImpl.java
   2  FactorValueMigratorServiceImpl.java
   2  CachedProcessedExpressionDataVectorServiceImpl.java
  ... 10 files with 1
```

### 2.4 Class-level `@Transactional` ServiceImpls

Seven `*ServiceImpl` classes are annotated `@Transactional` at the class
level (every method becomes advised). These are the densest pools of
potential self-invocation regressions if the pattern changes:

```text
SplitExperimentServiceImpl
PreprocessorServiceImpl
DifferentialExpressionAnalyzerServiceImpl
ExpressionDataDeleterServiceImpl
ExpressionDataFileServiceImpl
CellXGeneDataLoaderServiceImpl
ExpressionExperimentGeoServiceImpl
```

No action required today. Documented so future refactors know that any
method-level removal of `@Transactional` on these classes is a behavior
change.

## 3. Concrete-class autowire risk list

The `EeWriteServiceImpl.persisterHelper` `$ProxyN` bug pattern:

```java
@Autowired
private PersisterHelperImpl persisterHelper;   // BAD
```

When `PersisterHelperImpl` gets AOP-advised, Spring publishes a JDK
proxy whose type is `$Proxy42 implements PersisterHelper, ...`. That
proxy is not assignable to `PersisterHelperImpl`, so Spring throws
`BeanNotOfRequiredTypeException` at startup.

### 3.1 Scan: `@Autowired` field with type ending in `Impl`

```text
gemma-core/src/main/java/**: 0
gemma-rest/src/main/java/**: 0
gemma-web/src/main/java/**: 0
gemma-cli/src/main/java/**: 0
```

Zero instances in main. Three in `src/test/java` (test-only,
non-load-bearing).

### 3.2 Scan: `@Autowired` of an advised class that has at least one
non-lifecycle interface

This is the more thorough version (catches a renamed concrete class
that doesn't end in `Impl`).

Built a class index (kind, implements list). Walked all `@Autowired`
field and constructor-param types. Filtered to types whose source class
carries any of `@Transactional / @Cacheable / @CacheEvict / @CachePut /
@Secured / @PreAuthorize / @PostAuthorize / @Async`, AND implements a
non-lifecycle interface.

```text
HITS: 0
```

### 3.3 Scan: concrete class with interface, injected anywhere
(includes non-advised classes for completeness)

```text
FIELD: 18 hits, all BuildInfo / StaticAssetResolver / *CliConfig beans
        that implement only Spring lifecycle interfaces
        (InitializingBean) or one of the CLI marker interfaces.
        None carry AOP annotations, none are advised, no risk today.
CTOR:   1 hit, BuildInfoThreadContextPopulator -- same story.
```

### 3.4 Conclusion

The mine field exists in theory but is empty in practice. The risk is
that the next person adding `@Transactional` to one of those `BuildInfo`
-like beans, or adding a concrete-typed `@Autowired` to a service that
becomes advised, blows it up at startup. Treat as a regression-only
risk.

## 4. AOP advice inventory (recap from `ASPECTJ_EHCACHE_AUDIT`)

For completeness, every advice site that participates in the proxy
chain:

- `AuditAdvice` (`@Aspect @Component`) - 4 `@Before` advice methods
  bound to `Pointcuts.creator() / updater() / saver() / deleter()`.
  Operates on DAO beans.
- `Pointcuts` (`@Aspect`) - 13 `@Pointcut` definitions. Two are
  externally referenced from XML
  (`retryableOrTransactionalServiceMethod`, used by the
  `retryAdvice` advisor in `applicationContext-hibernate.xml`).
- `<aop:advisor>` for `retryAdvice` (Spring Retry, order=2) on
  `retryableOrTransactionalServiceMethod`.
- `<tx:annotation-driven order="3"/>` -> Spring's standard
  `TransactionInterceptor`.
- `<cache:annotation-driven order="2"/>` -> Spring's `CacheInterceptor`
  (Caffeine / ConcurrentMapCacheManager-backed).
- `<aop:aspectj-autoproxy/>` declared three times across the XML; the
  three declarations are redundant (one is enough) but harmless.

Test-only: `PointcutsTest` defines its own `@Aspect` and
`@EnableAspectJAutoProxy` to exercise the pointcut grammar.
`GenericMeterRegistryConfigurerTest` uses `@EnableAspectJAutoProxy` for
a Micrometer registry test.

## 5. Recommended remediation

In order of value / risk reduction:

### 5.1 Document and enforce "inject by interface" (one-line lint)

Add a checkstyle / archunit rule:

> No `@Autowired` field or constructor parameter may declare a type
> ending in `Impl` (excluding `src/test/**`).

The codebase is already 100% compliant. The rule is cheap insurance
against the `EeWriteServiceImpl.persisterHelper` style bug ever landing
again. Filed as a follow-up; not done in this branch.

### 5.2 Add an explicit assertion in
`applicationContext-hibernate.xml` (or its Java-config replacement) that
documents the proxy mode

Two lines of XML comment immediately above `<aop:aspectj-autoproxy/>`:

```xml
<!-- proxy-target-class is intentionally unset:
     - JDK dynamic proxies for any bean that implements a user-facing interface
     - CGLIB fallback for beans whose only interfaces are Spring lifecycle markers
     Do NOT add proxy-target-class="true" without auditing every @Autowired site
     for concrete-class fields (see ASPECTJ_DEEPER_AUDIT.md). -->
```

### 5.3 Self-invocation: no remediation needed today

The 220 LOW hits are all benign (inner-tx absorbed by outer-tx). If a
future refactor introduces a `REQUIRES_NEW` inner or strips an outer
`@Transactional`, that's the moment to either:

- Inject the service into itself by interface (the `@Autowired
  SomeService self` pattern) and call `self.foo()` instead of `foo()`.
- Or call `((SomeService) AopContext.currentProxy()).foo()` if
  `expose-proxy="true"` is set on the proxy config.

Both patterns are absent from the codebase today. Adding either should
go through code review with a pointer to this audit.

### 5.4 Hot-spot watch list

The 16 `*ServiceImpl` files at >= 2 self-invocations are the leading
indicators. If a future commit removes `@Transactional` from any method
in those files, treat as a regression risk and verify the call graph.

## 6. Open questions / follow-ups

- **`EeWriteServiceImpl.persisterHelper`** - that bug is on a different
  worktree (E3 work-in-progress, not in `phase2-acl-migrate`). The fix
  there should land as "change field type from concrete impl to
  interface". The recce here confirms that *as of HEAD* the codebase
  has no other ticking instance of the same pattern.
- **Archunit rule** - good Phase 3 follow-up ticket; out of scope of
  this recce.
- **Java-config replacement** - Phase 3 plans to migrate Spring XML to
  `@Configuration`. When that lands, the `proxy-target-class=false`
  invariant becomes `@EnableTransactionManagement(proxyTargetClass =
  false)` plus `@EnableAspectJAutoProxy(proxyTargetClass = false)`. The
  documentation note from 5.2 carries forward.
