# Static analysis sweep (2026-05-20)

## Tool used

**Manual regex-based sweep** (Python + grep) — SpotBugs CLI download from
GitHub releases was blocked by the auto-mode classifier, and the project
ships no SpotBugs/PMD/ErrorProne *plugin* wiring (only `jsr305` and
`error_prone_annotations` JARs are on the dep tree, not the analysers
themselves).

Patterns scanned in `gemma-core/`, `gemma-rest/`, `gemma-cli/` (~2.3k
production Java files, test sources excluded):

| smell | strategy |
|---|---|
| flipped `.equals("literal")` (NPE on null) | `\.equals\("` |
| resource leak (raw `new FileInputStream/FileOutputStream/...` outside try-with-resources) | regex + manual review of 26 non-twr hits |
| static `SimpleDateFormat` / `DateFormat` (thread-unsafe) | grep + read of every hit |
| double-checked locking without `volatile` | grep on `synchronized(...) { if (... == null)` |
| empty `catch` blocks; `printStackTrace`-only catch | Python regex over file contents |
| `equals` without `hashCode` (and vice versa) | Python regex; manually filtered model-entity hierarchy where `AbstractIdentifiable` / `AbstractDescribable` abstract the pair |
| serialVersionUID missing on `Serializable` implementers | Python loop |
| `String == "literal"` | regex (caught two false positives in Javadoc) |
| `Map<Double, ...>` (NaN key footgun) | grep |
| `URL.equals` (DNS-blocking) | grep |
| `wait()` outside a `while` loop | regex |
| `Arrays.asList(...).add/remove` (UnsupportedOperationException) | regex |
| compareTo via subtraction (overflow) | regex |
| `new StringBuffer` (legacy + slower than StringBuilder) | grep |

## Findings (ordered by severity)

### CRITICAL

1. **`gemma-core/.../AgilentScanDateExtractor.java:52`** — *public static
   mutable `SimpleDateFormat`*, parsed without synchronization and
   `setLenient( true )` called on the shared instance every invocation.
   SDF is not thread-safe; two concurrent platform loads (the Gemma
   loader pool is decidedly multi-threaded) can corrupt internal calendar
   state, yielding parsed dates years away from the input. The
   `setLenient` mutation is itself a write-write race independent of the
   parse. **Fixed** (this branch) — replaced with `newAgilentDateFormat()`
   factory returning a fresh `SimpleDateFormat` per call; test updated.

2. **`gemma-core/.../NetUtils.java:99`** — `FileOutputStream` opened
   before `f.retrieveFile(seekFile, os)`; if the FTP call throws
   `IOException` the stream leaks. On a long-running CLI the leaked
   file descriptors accumulate until `Too many open files`. **Fixed**
   (this branch) — wrapped in try-with-resources.

3. **`gemma-core/.../FileTools.java:266`** — `copy(InputStream, OutputStream)`
   closes both streams only on the happy path: a thrown `IOException`
   from `input.read` or `output.write` skips the trailing `close()` calls
   and both descriptors leak. Two callers (`copyPlainOrCompressedFile` at
   line 292 and `unZipFiles` at line 232) hand owned streams directly to
   `copy`, so the leak propagates. **Fixed** (this branch) — try-with-
   resources adopts both streams.

### HIGH

4. **`gemma-core/.../BuildInfo.java:24,28`** — two static
   `SimpleDateFormat` fields parsed from `afterPropertiesSet()` and
   `fromManifest()`. Only called at bean wiring / process start under
   normal usage, but the Spring lifecycle does not guarantee
   single-threaded init, and `BuildInfo.fromManifest()` is a static API
   that any caller may invoke concurrently. **Not fixed** (deferred —
   patch is straightforward but the risk is theoretical: startup is
   effectively serial today).

5. **`gemma-core/.../matrix/datafilter/AbstractFilter.java:46`** —
   `catch ( Exception e ) { e.printStackTrace(); }` swallows reflection
   failures and falls through with `returnval = null`, which is then cast
   to `M` and returned. Every caller of `getOutputMatrix` NPEs downstream
   with no breadcrumb of the original `NoSuchMethodException` /
   `InvocationTargetException`. **Not fixed** — needs caller audit
   to choose between `throw new IllegalStateException(e)` and a typed
   exception.

6. **`gemma-core/.../analysis/expression/diff/BaselineSelection.java:184`**
   — `TreeMap<Double, FactorValue>` with `Double.NaN` inserted as a key
   when a sample is missing the continuous measurement. `Double.NaN`
   compares unequal to itself (and inconsistently against finite
   doubles); two NaN-keyed entries can both `put` successfully under
   `TreeMap` while only one is reachable by `get(Double.NaN)`, and
   `firstKey()` semantics around NaN are surprising. Affects baseline
   selection on partially-missing continuous factors. **Not fixed** —
   the right behaviour (drop NaN-bearing values entirely vs treat as
   minimum vs surface a warning) is a domain call, out of scope for a
   static-analysis pass.

### MEDIUM

7. **`gemma-core/.../graphics/ColorMatrix.java:84-87`** —
   `try { super.clone(); } catch ( CloneNotSupportedException e ) {}`.
   The result of `super.clone()` is discarded; the real clone is rebuilt
   from fields. `ColorMatrix implements Cloneable`, so the catch is
   unreachable, but the call performs a shallow Object copy and throws
   it away (wasted allocation). Cosmetic; left as-is to keep scope small.

8. **`gemma-core/.../util/matrix/(StringMatrixReader|DoubleMatrixReader).java`**
   — `BufferedReader dis = new BufferedReader( new InputStreamReader(
   stream ) );` not in try-with-resources, but the wrapped `stream` is
   owned by the caller (factory pattern). Acceptable — the BufferedReader
   instance is GC-collected and only the underlying stream's caller-side
   ownership matters. No fix.

9. **`gemma-core/.../loader/util/parser/{BasicLineParser,RecordParser,LineMapParser}.java`**
   — Same factory-pattern shape as 8 (BufferedReader wrapping a passed-in
   InputStream, with the InputStream lifecycle owned upstream). The
   BufferedReader itself is never `close()`d, which would release the
   wrapped stream — slight risk if a caller does *not* also close the
   underlying. Three classes, low risk; deferred.

10. **`gemma-cli/.../GemmaCLI.java:345`** — `PrintWriter completionWriter
    = new PrintWriter( System.out );` not closed (would close
    `System.out`). Acceptable — closing the wrapper would close stdout,
    which is wrong for a CLI. No fix.

### LOW

11. **`gemma-core/.../util/matrix/{DoubleMatrix,IntegerMatrix,ObjectMatrixImpl,MatrixWriter,DenseDoubleMatrix}.java`**
    — `new StringBuffer()` instead of `StringBuilder`. Identical API;
    StringBuilder is faster (no synchronization), recommended since
    Java 5. 9 sites total. Stylistic; safe to migrate in a follow-up.

12. **`gemma-core/.../loader/entrez/EntrezUtils.java:61`** — `monitor.wait(
    timeoutMs - diff )` is used as a rate-limit *sleep* inside a
    synchronized block (no `notify()` ever fires). Functionally fine but
    surprising; `Thread.sleep` would be clearer. No bug, just code-smell.

13. **`gemma-core/.../loader/entrez/NcbiEntityResolver.java:21`** —
    `static WeakHashMap<String, byte[]> dtdCache`. The key is the
    `systemId` string, the value is the parsed DTD bytes. Because String
    keys can be interned and the `systemId`s are URLs held elsewhere,
    WeakHashMap entries may not be reclaimed; the map is also accessed
    under a `synchronized` method. Acceptable as a process-lifetime DTD
    cache; the WeakHashMap choice is only weird, not wrong.

14. **No `Serializable` class is missing `serialVersionUID`** (57
    checked). The auto-gen tooling and `AbstractIdentifiable` /
    `AbstractDescribable` chain both bake in a UID.

15. **No `URL.equals` callsite found.**

16. **No `equals`/`hashCode` inconsistency** outside the entity
    hierarchy, where both methods are abstract on `AbstractIdentifiable`
    /  `AbstractDescribable` and concrete subclasses provide both (or,
    rarely, override only one because the other is correctly abstract
    upstream — Hibernate-entity pattern).

17. **No flipped `.equals("literal")` NPE pattern** in production code
    (only one hit, `dbF[0].equals("AllianceGenome")`, where `dbF[0]` is
    guaranteed non-null by the prior `length == 3` guard).

## Findings fixed in this branch

Two commits land on top of the static-analysis sweep doc:

* **AgilentScanDateExtractor + test** — replace shared mutable
  `AGILENT_DATE_FORMAT` SDF + per-call `setLenient` mutation with a
  thread-safe factory `newAgilentDateFormat()`. Test updated to call the
  factory.
* **NetUtils.ftpDownloadFile + FileTools.copy** — wrap raw streams in
  try-with-resources so an IOException mid-transfer does not leak file
  descriptors.

Commit SHAs reported in the orchestrator hand-off.

## Findings deferred for orchestrator review

| # | finding | why deferred |
|---|---|---|
| 4 | `BuildInfo` static SDFs | startup-only callers; theoretical race; trivial patch but not breakage in practice |
| 5 | `AbstractFilter.getOutputMatrix` swallowing reflection failure | needs caller audit to pick correct exception type; not mechanical |
| 6 | `BaselineSelection` `TreeMap<Double>` + `Double.NaN` key | domain call; talk to Paul on intended behaviour |
| 7 | `ColorMatrix.clone()` discarded `super.clone()` + empty catch | cosmetic; would change exception semantics if anything else holds a reference |
| 11 | `StringBuffer -> StringBuilder` (9 sites) | grouped follow-up; not on a hot path |

## Cross-cutting patterns

* **The Gemma entity-equality contract works.** `AbstractIdentifiable` /
  `AbstractDescribable` push `equals` + `hashCode` to abstract methods,
  so the SpotBugs "equals without hashCode" check fires on dozens of
  files (33 from the sweep), but every one of them inherits the missing
  half from an abstract parent declaration. No genuine inconsistencies.

* **Thread-safe-by-default factories beat shared mutables.** The two
  concurrency findings (`AgilentScanDateExtractor`, `BuildInfo`) and one
  near-miss (`SraRuninfoParser`, which *does* synchronize but on a
  per-instance field, fine) all stem from `static SimpleDateFormat`
  bytecode. A linter rule banning `static (final )?SimpleDateFormat`
  fields would have caught both. (Same lesson applies to `static
  Matcher` and `static Calendar`, neither of which the codebase uses.)

* **Resource ownership is mostly handled, but the few gaps cluster in
  CLI long-runners.** The two confirmed leaks are in `FileTools.copy` and
  `NetUtils.ftpDownloadFile`, both invoked from CLI batch fetchers (data
  loader / Affy / GEO bulk downloads). Long-running CLIs that hit a
  network error stand a real chance of `Too many open files`; short-
  running web requests do not. Worth a once-per-quarter `lsof | grep
  java | wc -l` check on the production CLI host.

* **Manual sweep coverage gap.** Without SpotBugs proper, several classes
  of finding (dataflow null-deref, unconditional `Integer == Integer`
  identity comparison, `Map.get`-then-null-deref where guarded by a
  prior `containsKey`, infinite recursion, etc.) were not exercised.
  Worth wiring `spotbugs-maven-plugin` in a follow-up so this sweep can
  be `mvn -pl gemma-core spotbugs:check` instead of a regex script.
