# AspectJ JDK-Proxy Invariant: Carry-Forward Checklist for `@Configuration` Migration

Companion to `ASPECTJ_DEEPER_AUDIT.md` (commit `b16450a5e8`, unmerged branch
`worktree-aspectj-deeper`). That audit established the lab-wide invariant
that Gemma runs on **interface-based JDK proxies** (i.e.
`proxy-target-class=false`), never on CGLIB subclass proxies. Recommendation
#3 of the recce calls for explicitly carrying this invariant forward to
every XML -> `@Configuration` migration via:

- `@EnableTransactionManagement(proxyTargetClass = false)`
- `@EnableAspectJAutoProxy(proxyTargetClass = false)`
- (and, for consistency, any other `@Enable*` annotation that produces an
  AOP advisor / MethodInterceptor and exposes a `proxyTargetClass`
  attribute -- e.g. `@EnableGlobalMethodSecurity`, `@EnableCaching`,
  `@EnableAsync`)

The framework default is already `false`, so adding the attribute is a
**no-op behavioural change** -- it is paranoia / forward-protection. The
goal is that the invariant lives at the call site, visible to any reviewer
who touches the annotation, instead of relying on the Spring default to
stay where it currently is.

---

## Baseline configs on `phase2-acl-migrate` (commit `08e760bdaf`)

Three `@Configuration` classes had already landed before this commit; this
audit checked each one and added the explicit attribute where appropriate.

| Config class | Path | `@Enable*` annotation? | Action |
|---|---|---|---|
| `MethodSecurityConfig` | `gemma-core/src/main/java/ubic/gemma/core/security/MethodSecurityConfig.java` | `@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, order = 1)` | Added `proxyTargetClass = false` |
| `GemmaAclConfiguration` | `gemma-core/src/main/java/ubic/gemma/core/security/acl/GemmaAclConfiguration.java` | none | No action |
| `EhcacheConfig` | `gemma-core/src/main/java/ubic/gemma/persistence/cache/EhcacheConfig.java` | none | No action |

`GemmaAclConfiguration` and `EhcacheConfig` only declare `@Bean` factory
methods -- they don't enable any AOP advisor stack, so there's no
`proxyTargetClass` knob to set. If someone later adds e.g.
`@EnableCaching` to `EhcacheConfig`, the invariant rule applies; see
"Merge-time reviewer checklist" below.

---

## Merge-time reviewer checklist

The following `@Configuration` classes are landing on **unmerged** branches
during the Phase 3 XML -> Java config push and **must be checked against
the invariant when their branch merges**:

- `ComponentScanConfig`
- `ServiceBeansConfig`
- `MetricsConfig`
- `SchedulerConfig`
- `HibernateConfig`
- `DataSourceConfig`
- `SecurityConfig`
- `RestSecurityConfig`
- `AnalyticsConfig`
- `RestComponentScanConfig`
- `CliComponentScanConfig`

(Plus any new `@Configuration` classes added in subsequent Phase 3 work.)

For each one, the reviewer must:

1. **Grep** the new file for `@Enable`:
   ```
   grep -n "^import org.springframework.*Enable\|@Enable" <path>
   ```
2. For every `@Enable*` annotation hit, check whether it exposes a
   `proxyTargetClass` attribute. The ones we care about:
   - `@EnableTransactionManagement` -- MUST set `proxyTargetClass = false`
   - `@EnableAspectJAutoProxy` -- MUST set `proxyTargetClass = false`
   - `@EnableGlobalMethodSecurity` (legacy) -- SHOULD set `proxyTargetClass = false`
   - `@EnableMethodSecurity` (new) -- SHOULD set `proxyTargetClass = false`
   - `@EnableCaching` -- SHOULD set `proxyTargetClass = false`
   - `@EnableAsync` -- SHOULD set `proxyTargetClass = false`
   - `@EnableScheduling` -- N/A (no `proxyTargetClass` attribute; uses TaskScheduler, not AOP advisor)
   - `@EnableWebSecurity` -- N/A (filter chain, not AOP advisor)
3. If the attribute is missing on a MUST/SHOULD annotation, **block the
   merge** until it is added. The fix is a one-line edit (default value
   is already `false`, so no behaviour changes; the attribute just makes
   the invariant explicit).

The cost of the explicit attribute is one extra token per annotation; the
cost of *not* having it is that some future contributor flips a Spring
default and all of Gemma's `@PreAuthorize` SpEL silently moves to CGLIB
proxies -- which would re-introduce the parametric-interface
self-invocation bugs the AspectJ recce ruled out.

---

## Why this matters (one-paragraph recap)

Gemma's service layer is heavily based on parametric base interfaces
(`BaseService<T>`, `BaseVoEnabledService<T, VO>`, etc.) with
`@PreAuthorize` SpEL declared **on the interface**. JDK dynamic proxies
correctly preserve the SpEL annotation for runtime evaluation because the
proxy is generated from the interface. CGLIB subclass proxies generate the
proxy from the concrete `*ServiceImpl`, where the SpEL annotation may not
be visible -- and where method dispatch goes through the subclass `super`
call chain rather than the interface, potentially bypassing transactional
and security advice on parametric methods. The AspectJ deeper recce
verified that no current code path requires CGLIB, and the proxy mode is
JDK across the board. Keeping it that way is a hard invariant.
