# STATUS_CLI_AUDIT_MIGRATION.md

Status of the imperative `auditTrailService.addUpdateEvent(...)` residue
in `gemma-cli/src/main/java`. Companion to
`handoffs/AUDIT_RESIDUAL_INVENTORY.md` (which covers `gemma-core` and
treats `gemma-cli` as out-of-scope).

Baseline: `phase2-acl-migrate` at `4955ebca52`, branch
`fix-audit-gemma-cli-migration`.

## TL;DR

**All 12 CLI callsites deferred.** Every one of them is reached only via
self-invocation through the CLI runner's base-class dispatch chain
(`executeCommand` -> `doWork` -> `doAuthenticatedWork` ->
`processExpressionExperiment(ee)` / `processArrayDesign(ad)`). Spring
AOP advice is bypassed on `this.foo(...)` calls inside the same proxy
target, so `@Audited` / `@AuditedConditional` / `@AuditedOnError`
annotations on the override or on the private `audit(...)` helper would
never fire.

Migrating any of them requires the same "hoist into a co-bean" surgery
that bucket 2g calls out in `AUDIT_PHASE_C_RECCE.md` (private-helper
hoist). The brief's promise of 4-6 easy migrations does not hold:
**the easy-looking sites have the same self-invoke blocker as the hard
ones**.

## The blocker, in one paragraph

`CliComponentScanConfig` (gemma-cli) declares every CLI tool a Spring
prototype bean via `<includeFilter type=ASSIGNABLE_TYPE class=CLI>`. The
core `ComponentScanConfig` adds `@EnableAspectJAutoProxy` (default
`proxyTargetClass=false, exposeProxy=false`). When `GemmaCLI.main`
resolves a CLI by `ctx.getBeanNamesForType(CLI.class)` it gets the
proxied reference, so the outer `cli.executeCommand(ctx)` call IS
proxied. From that point on every internal call uses bare `this`, which
references the underlying target — not the proxy — so `AuditedAspect`
never sees them. `exposeProxy=true` would let us reach the proxy via
`AopContext.currentProxy()`, but it isn't enabled and turning it on
project-wide is out of scope for this task.

## Site-by-site

| # | File | Line | Event | Method on which `@Audited` would sit | Self-invoked? | Notes |
|---|---|---|---|---|---|---|
| 1 | `ArrayDesignBlatCli` | 187 | `ArrayDesignSequenceAnalysisEvent` | private `audit(ArrayDesign, String)` | **Yes** | Helper called from `processArrayDesign`. Hoist target. |
| 2 | `MakeExperimentPrivateCli` | 26 | `MakePrivateEvent` | `processExpressionExperiment(ExpressionExperiment)` | **Yes** | Called from base-class loop via `this.processExpressionExperiment(...)`. Looks easy, isn't. |
| 3 | `ArrayDesignSequenceAssociationCli` | 243 | `ArrayDesignSequenceUpdateEvent` | private `audit(ArrayDesign, String)` | **Yes** | Same shape as #1. |
| 4 | `ArrayDesignBioSequenceDetachCli` | 114 | `ArrayDesignSequenceRemoveEvent` | private `audit(ArrayDesign, String)` | **Yes** | Same shape as #1. |
| 5 | `ArrayDesignProbeRenamerCli` | 132 | `ArrayDesignProbeRenamingEvent` | private `audit(ArrayDesign, String)` | **Yes** | Same shape as #1. |
| 6 | `GenericGenelistDesignGenerator` | 325 | `AnnotationBasedGeneMappingEvent` | inline in `doAuthenticatedWork` | **Yes** | The call site sits directly in the framework method. Even more tightly coupled than the helper pattern. Plus the `!noDB` guard would need `@AuditedConditional(when=...)`. |
| 7 | `ArrayDesignRepeatScanCli` | 159 | `ArrayDesignRepeatAnalysisEvent` | private `audit(ArrayDesign, String)` | **Yes** | Same shape as #1. |
| 8 | `ExpressionDataCorrMatCli` | 73 | `FailedSampleCorrelationAnalysisEvent` | `processExpressionExperiment` catch(`FilteringException`) | **Yes** | Would be a clean `@AuditedOnError(value=FailedSampleCorrelationAnalysisEvent.class, exception=FilteringException.class)` if the entry-point were proxied. It isn't. |
| 9 | `ExpressionDataCorrMatCli` | 76 | `FailedSampleCorrelationAnalysisEvent` | `processExpressionExperiment` catch(`Exception`) | **Yes** | Repeatable `@AuditedOnError` second branch. Same blocker. |
| 10 | `ArrayDesignSubsumptionTesterCli` | 152 | `ArrayDesignSubsumeCheckEvent` | private `audit(ArrayDesign, String)` | **Yes** | Same shape as #1. Helper is even currently dead-code (the only caller is commented out at :148). |
| 11 | `MakeExperimentsPublicCli` | 45 | `MakePublicEvent` | `processExpressionExperiment` | **Yes** | Mirror of #2. |
| 12 | `ArrayDesignProbeMapperCli` | 484 | dynamic `Class<? extends ArrayDesignGeneMappingEvent>` | private `audit(ArrayDesign, String, Class)` | **Yes** | Doubly blocked: self-invoke AND dynamic event class. Needs `valueSpel` infra (parallel to gemma-core inventory #15/#16) PLUS the hoist. Lowest priority. |

## Inventory drift from the brief

The brief listed two files that no longer host any
`auditTrailService.addUpdateEvent(...)` calls in this baseline:

- `ExpressionExperimentPlatformSwitchCli.java:77, 82` — no occurrences.
- `ExpressionExperimentDataFileGeneratorCli.java:77` — no occurrences.

So the true count is 12 callsites across 11 files (CorrMat has two).

## What it would take to actually migrate any of these

Two paths, both out of scope for this branch:

**Path A — hoist into a co-bean (the bucket 2g playbook).** Create a
new `*CliAuditServiceImpl` (one per CLI, or one shared
`CliAuditServiceImpl` if the event classes can be parameterised). Move
the imperative `auditTrailService.addUpdateEvent(...)` call into a
method on the co-bean, annotated `@Audited`. The CLI autowires the
co-bean and calls `cliAuditService.recordFoo(ad, note)` instead of the
inline call. The aspect now fires because the call crosses a proxy
boundary. Mechanical but each CLI is its own commit; expect ~12 small
PRs and a new co-bean per event family.

**Path B — flip `exposeProxy=true` on `@EnableAspectJAutoProxy` and
rewrite each self-invoke as `((CLI) AopContext.currentProxy()).foo(...)`.**
Smaller diff per site but the global flag flip has subtle perf and
ergonomics impact (every aspect-annotated bean pays the `currentProxy`
ThreadLocal cost). Not recommended for this localized task.

**Path C — annotate the public framework method (`executeCommand`).**
Discarded: it doesn't have an `Auditable` argument (it takes
`CLIContext`), and even if we contrived to supply one via SpEL on
`#result`, it would emit ONE event per CLI invocation regardless of how
many experiments were processed. Wrong semantics for batch tools.

## Recommendation

Leave the 12 callsites as imperative `auditTrailService.addUpdateEvent`
for now. They are CORRECT today (the legacy imperative path still
writes audit rows; `AuditAdvice` retirement only removed the *generic
auto-UPDATE* path, not the explicit `addUpdateEvent` API). The audit
framework's `AuditTrailService.addUpdateEvent(...)` is the supported
imperative entry-point alongside the annotations.

Revisit when:

1. A wider Phase C wave funds the bucket 2g hoist for gemma-core sites
   #3, #12, #13 — the co-bean pattern from that work transfers here
   one-for-one.
2. `valueSpel` lands on `@Audited` (needed to unblock #12).
3. Someone wants to converge gemma-cli onto the same audit shape as
   gemma-core for documentation cleanliness — but it's a hygiene win,
   not a correctness fix.

## Bottom line

- **0 sites migrated this branch.**
- **12 sites deferred** with a documented blocker (self-invoke / AOP
  proxy bypass).
- **2 inventory entries proven stale** (Platform-Switch and
  DataFileGenerator CLIs — no audit calls in baseline).
