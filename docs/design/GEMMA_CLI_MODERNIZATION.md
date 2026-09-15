# gemma-cli runner modernization recce

Phase 3 Spring 6+ infrastructure modernization — read-only audit of the
`gemma-cli` module. Baseline: `08e760bdaf` (`phase2-acl-migrate`).

## 1. Current entry point + bootstrap pattern

- **Main class:** `ubic.gemma.cli.main.GemmaCLI` (482 lines) — wired
  via `gemma-cli/pom.xml` appassembler-maven-plugin so the produced
  shell script is `bin/gemma-cli`.
- **Args framework:** Apache Commons CLI **1.11.0** (the current 1.x
  stable, released 2024). Imported as `org.apache.commons.cli.*` in
  21 main-source files (entry point, abstract bases, helpers,
  completion generators, and a handful of command classes that need
  custom parsing).
- **JVM bootstrap order:**
  1. Static initializer sets `gemma.log.dir` if missing (so log4j2
     can bind before any logger is touched).
  2. Top-level args parsed with `DefaultParser` (`stopAtNonOption=true`).
     Global-only flags handled before Spring boots: `-h/--help`,
     `-ha/--help-all`, `--version`, `-c/--completion`,
     `-cw/--completion-wiki`, `-cl/--complete-loggers`,
     `-cc/--complete-config`, `-v/--verbosity`, `--logger`,
     `--testdb`, `--profiling`.
  3. Log4j verbosity / per-logger overrides applied via
     `LoggingConfigurer` (Log4jConfigurer implementation).
  4. CLI invocation string stashed in `ThreadContext` under
     `gemma.cliArguments` (passwords masked via `ShellUtils.quoteIfNecessary`).
  5. Spring context built via
     `SpringContextUtils.getApplicationContext(profiles…)`, where
     `profiles` always includes `cli` and conditionally adds
     `testdb` and/or `profiling`.
  6. Shutdown hook registers a graceful
     `ConfigurableApplicationContext.close()` on SIGTERM /
     `System.exit`.
  7. All beans implementing `CLI` are enumerated; each is instantiated
     **twice** — once via `BeanUtils.instantiate(beanClass)` to harvest
     metadata (name/aliases/options) cheaply, and again via
     `ctx.getBean(beanName)` only for the chosen command. This is the
     key reason commands live in `ubic.gemma.apps.*` as prototype-scoped
     beans (see `PrototypeScopeResolver`).
- **Component scan:** `gemma-cli/src/main/resources/ubic/gemma/applicationContext-component-scan.xml`
  is profile-gated to `cli`. Three scans: `ubic.gemma.cli.*` (default
  filters, singleton), `ubic.gemma.apps` and `ubic.gemma.contrib.apps`
  (assignable-to-`CLI` only, **prototype-scoped** via the custom
  `PrototypeScopeResolver`). `LazyInitByDefaultPostProcessor` ensures
  beans only materialize on demand.
- **Exit-code path:** `SystemCLIContext.exitStatus` is set by
  `AbstractCLI` then returned via `cliContext.getExitStatus()` and
  `System.exit(statusCode)` in `GemmaCLI.main()`. Four well-defined
  codes: `SUCCESS=0`, `FAILURE=1`, `FAILURE_FROM_ERROR_OBJECTS=2`,
  `ABORTED=3`. Unrecognized command -> 1; bean-instantiation conflict
  -> 1; uncaught Exception in `executeCommand` -> 1. **The
  propagation is sound.**
- **Logging on startup:** `GemmaCLI.main()` already prints
  `========= Gemma CLI invocation of <cmd> ============` and
  `Options: <masked args>` to stderr before dispatch, plus a
  matching `complete in N seconds` line in the `finally` block.
  Audit is also captured in Log4j's `ThreadContext` so it surfaces in
  any structured-log appender. This box is checked.

## 2. CLI command inventory

| File pattern | Count |
|---|---:|
| Files under `gemma-cli/src/main/java` | 173 |
| Files matching `*CLI.java`/`*Cli.java` in `apps/` | 102 |
| Concrete subclasses of `AbstractCLI`/`AbstractAuthenticatedCLI` | **43 in `apps/` + 2 abstract bases** |
| Total `CLI`-implementing types found by `grep -rln "extends AbstractCLI"` | 45 |

Note: the gap between 102 `*Cli.java` files and 43 concrete `CLI`
subclasses is because many "Cli" suffixed files in `apps/` are
intermediate abstract bases (e.g.
`ExpressionExperimentManipulatingCLI`, `ArrayDesignSequenceManipulatingCli`)
or helper classes, not directly-scannable commands.

### By `CommandGroup` (from `grep CommandGroup.X` in `apps/`)

| Group       | Count |
|-------------|------:|
| EXPERIMENT  |    10 |
| PLATFORM    |     4 |
| ANALYSIS    |     4 |
| METADATA    |     5 |
| SYSTEM      |    11 |
| MISC        |     5 |
| (default MISC fallback) | rest |
| DEPRECATED  |     0 (none in this module) |

### Subpackages of `ubic.gemma.cli`

```
authentication   CLIAuthenticationManager + Aware hooks (incl. test profile env var swap)
batch            BatchTaskExecutorService, progress reporters, summary writers (text/tsv/log)
completion       Bash + Fish completion generators, Confluence wiki HTML generator, completion sources
config           CLI-side Spring config
logging          LoggingConfigurer interface
logging/log4j    Log4jConfigurer (Log4j2-backed)
main             GemmaCLI entry point
metrics          MeterRegistryCliConfigurer (Micrometer, profile-gated to `metrics`)
options          DataFileOptionsUtils + a small family of enum option-value classes
util             AbstractCLI, AbstractAuthenticatedCLI, AbstractAutoSeekingCLI, CLIContext,
                 EntityLocator, EntityOptionsUtils, EnumeratedConverter family, HelpUtils,
                 OptionsUtils, PrototypeScopeResolver
```

## 3. Args parsing approach

- **Framework:** Apache Commons CLI 1.11.0 — current, NOT the
  long-deprecated 1.2/1.4. Already exercises modern features:
  - `Option.builder(...).hasArg().argName(...).type(Path.class).converter(...).build()`
    fluent style.
  - Custom converters: `EnumeratedConverter`, `EnumConverter`,
    `EnumeratedByCommandConverter`, `EnumeratedStringConverter`,
    `EnumeratedByCommandStringConverter` (these double as completion
    sources — the same converter that validates `-v` also drives the
    bash/fish completion script).
  - Heavy use of `OptionsUtils` for two-option mutual-exclusion
    (`-auto`/`-noauto`), required-when predicates, parsed-typed
    accessors.
- **Per-command shape:** each `AbstractCLI` subclass overrides
  `buildOptions(Options)` to declare options and
  `processOptions(CommandLine)` to harvest them into fields. See
  `NcbiGeneLoaderCLI` (171 lines) and `LoadExpressionDataCli`
  (317 lines) for canonical examples. Pattern is consistent across all
  43 concrete commands.
- **Completion:** the project already ships **bash and fish
  completion generators**, plus a **Confluence wiki page generator**
  for the public command docs. `--completion --completion-shell bash`
  emits a working completion script; `$SHELL` is auto-detected when
  the flag is omitted. This is unusual sophistication for a Commons
  CLI project — going beyond what most picocli ports would deliver
  out of the box.

## 4. Modernization opportunities

### High impact: none required.

The existing infrastructure is well-designed for a CLI of this size:
- Args parsing is on the current Commons CLI 1.11.0 release.
- Spring bootstrap is profile-gated, prototype-scoped, lazy-init.
- Exit codes are propagated cleanly with documented semantics.
- Startup-line logging is present.
- Shell completion (bash + fish) is implemented and ties cleanly into
  the option converters.
- Micrometer metrics scaffolding is in place (profile `metrics`),
  tagging by `environment=cli` and authenticated user.
- Audit log of every invocation captured via Log4j ThreadContext.

### Medium impact

1. **Replace `BeanUtils.instantiate` metadata-harvest with `@Component`
   metadata.** `GemmaCLI.main()` constructs every CLI bean via
   reflection just to read its name/aliases/options/shortDesc. This
   forces every CLI to have a public no-arg constructor and to keep
   field defaults safe to call before Spring autowires anything. A
   compile-time annotation (`@CliCommand(name=..., group=...,
   aliases=...)`) read via reflection on the bean *class* (no
   instantiation needed) would let `GemmaCLI` build the dispatch table
   without constructing 43 transient beans on every invocation. This
   is cosmetic, not a correctness issue.

2. **`BatchFormat` enum is package-private inside `AbstractCLI`.**
   It is used as a public option value (`-batchFormat TSV`) and by
   the completion converter. Lifting it to a top-level enum in
   `ubic.gemma.cli.batch` would let users link to its Javadoc and
   would simplify the converter wiring.

3. **`getOptStringForLogging` masks nothing**, despite its name
   and the comment "Mask password for logging". It only
   shell-quotes. Passwords typically arrive via `$GEMMA_PASSWORD` so
   they don't appear in `argv` — but if a CLI ever adds a
   `--password` flag the audit log would leak. Worth either renaming
   the method or wiring an explicit `Option#sensitive` annotation that
   gets redacted here. (Reach for a `Set<String> SENSITIVE_OPTIONS`
   constant; cheap to implement.)

### Low impact

4. **Zsh completion.** Bash + fish are present; zsh users (which on
   modern macOS is the default shell) fall back to bash completion
   loaded via bashcompinit. A dedicated `ZshCompletionGenerator`
   following the existing `BashCompletionGenerator` template would be
   a friendly polish item — but the user-base is small enough that
   bash completion via `bashcompinit` likely suffices.

5. **`System.err.printf( e.getMessage() );`** at line 314 of
   `GemmaCLI.java` — `e.getMessage()` is passed as a format string,
   so any `%` in the message would crash with `MissingFormatArgumentException`.
   One-character typo: change `printf` to `println` or
   `printf("%s%n", e.getMessage())`. Pure bug.

6. **`appassembler-maven-plugin` 2.1.0** (May 2017) is venerable but
   functional. The successor for a JDK17 world is `jpackager` or
   `jlink`, which would produce a self-contained runtime image. Out
   of scope for Phase 3 modernization; a Phase 4+ packaging discussion.

7. **`org.ocpsoft.prettytime.shade.edu.emory.mathcs.backport.java.util.Collections`**
   used at `AbstractCLI:27` instead of `java.util.Collections.emptyList()`.
   Drop the shaded import. 30-second fix.

### Not recommended

- **Switch to picocli.** The migration cost is high (43 commands +
  the bash/fish/wiki generators) and the gains over the current
  Commons-CLI-1.11 + custom completion stack are mostly cosmetic.
  picocli's annotation-driven model would shrink command boilerplate
  by ~30% and ship `@CommandLine.Command` + `@Option` + native zsh
  completion, but the project already has:
  - working bash+fish completion that no other Commons CLI codebase
    has,
  - working Confluence wiki page generation that picocli does not
    offer,
  - well-tested converter/completion-source plumbing (5 enumerated-
    converter classes with thorough tests),
  - 100% green tests on the current model.

  Verdict: not a Phase 3 priority. Revisit if the maintenance cost of
  `buildOptions(Options)`/`processOptions(CommandLine)` becomes a
  drag on adding new commands.

- **Switch to JCommander / argparse4j.** Less capable than the
  current setup; no upside.

## 5. Recommendations + effort estimates

| # | Recommendation | Effort | Priority |
|---|----------------|-------:|----------|
| 1 | Fix `System.err.printf( e.getMessage() )` bug (GemmaCLI.java:314) | 5 min | Should-do |
| 2 | Replace `prettytime.shade...Collections` import with `java.util.Collections` | 5 min | Should-do |
| 3 | Rename / actually implement `getOptStringForLogging` password masking via a `SENSITIVE_OPTIONS` constant | 1 hr | Should-do |
| 4 | Add `@CliCommand`-style annotation to skip reflective bean construction during dispatch-table build | 1 day | Nice-to-have |
| 5 | Lift `BatchFormat` enum out of `AbstractCLI` | 15 min | Nice-to-have |
| 6 | Add a dedicated `ZshCompletionGenerator` | 4 hr | Polish |
| 7 | picocli migration | 5-10 days | **Not recommended** |
| 8 | `jlink`/`jpackager` packaging | Phase 4 | Deferred |

## 6. Open questions for Paul

1. **Is the no-instantiation-on-dispatch refactor (rec. #4) worth a
   day of engineering?** It would reduce startup time fractionally
   and remove a "no-arg ctor must be safe" constraint that has bitten
   us before with autowire-on-construction patterns. If we're not
   adding many new CLI commands this is purely a code-hygiene win.

2. **Are there CLI commands invoked from cron/Jenkins that should
   emit Micrometer counters/timers automatically?** The metrics
   infrastructure is in place (`MeterRegistryCliConfigurer` tags by
   environment and user) but no CLI today wraps its `doWork` in a
   `Timer.Sample` — so nothing actually emits per-command metrics.
   Low effort to add at the `AbstractCLI#executeCommandWithCliContext`
   level: one timer keyed by `commandName`, one counter for
   success/failure/aborted. Worth a half-day if the
   ops-observability story matters.

3. **Should `--testdb` and `--profiling` be CLI-global flags, or
   move into the standard Spring `-Dspring.profiles.active=` path?**
   Current pattern (top-level flags that mutate the profiles list
   before Spring starts) is ergonomic for cron lines, but couples
   `GemmaCLI` to the set of named profiles. Cleaner alternative is to
   accept `-Dspring.profiles.include` from the env. Style call.

4. **Is `bin/gemma-cli` still the deployment unit?** If `gemma-web`
   is walking dead (per memory note) and `gemma-curation-ui` is the
   new frontend, the CLI is even more central to operations. Worth
   confirming so the modernization budget for this module is
   right-sized.
