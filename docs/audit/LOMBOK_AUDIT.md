# Lombok Audit — Phase 3 Spring 6+ Modernization Recce

Date: 2026-05-18
Branch: `worktree-lombok-audit` (from `phase2-acl-migrate` @ `08e760bdaf`)
Scope: recce only — no code changes.

This audit decides whether to phase Lombok out, keep it intentionally,
or perform a targeted partial cleanup. Lombok works with JDK 17 + Spring 6
today, but it has historically been a friction point on JDK upgrades
(every major release has needed a Lombok patch to keep the annotation
processor running) and on JPA equality semantics.

---

## 1. Total usage

| Metric | Value |
|---|---|
| Files importing `lombok.*` | **421** (408 main, 13 test) |
| Total `import lombok.*` lines | **573** |
| Lombok declared in | root `pom.xml` (compile dep); `lombok-maven-plugin` 1.18.20.0 for delombok-to-javadoc |
| Lombok version | managed by `pavlab-starter-parent` 1.2.29 (not pinned in this repo) |

Per-annotation breakdown (counted via `import lombok.X` lines so spring's
`@Value` / JPA `@Entity` don't pollute the numbers):

| Annotation | Count | Notes |
|---|---:|---|
| `lombok.extern.apachecommons.CommonsLog` | **188** | logger (`log` field). Dominant usage. |
| `lombok.Data` | 90 | mostly DTOs (REST `*ValueObject`, SRA/GEO/cellxgene/UCSC loader models) |
| `lombok.Setter` | 87 | |
| `lombok.Getter` | 77 | |
| `lombok.Value` | 52 | immutable value classes — record candidates |
| `lombok.EqualsAndHashCode` | 32 | mostly on `*ValueObject` DTOs |
| `lombok.Builder` | 15 | `*Config` builders, REST error payloads, `SearchSettings` |
| `lombok.ToString` | 6 | |
| `lombok.experimental.SuperBuilder` | 6 | |
| `lombok.extern.jackson.Jacksonized` | 5 | works with Jackson + builder |
| `lombok.AllArgsConstructor` | 5 | |
| `lombok.SneakyThrows` | 3 | flagged below |
| `lombok.With` | 2 | |
| `lombok.NoArgsConstructor` | 2 | |
| `lombok.Singular` | 1 | |
| `lombok.RequiredArgsConstructor` | 1 | |

Lombok-related test code is small (13 files); migration risk concentrates
in main sources.

---

## 2. By-package distribution

Module split (file counts of files with at least one `import lombok.`):

| Module | Files |
|---|---:|
| `gemma-core` | 312 |
| `gemma-web` | 43 |
| `gemma-rest` | 42 |
| `gemma-cli` | 24 |

Top sub-packages (Lombok-using-file count):

| Sub-package | Files |
|---|---:|
| `gemma-core .../loader/expression/sra/model` | 24 |
| `gemma-rest .../rest/util` | 16 |
| `gemma-core .../model/expression/bioAssayData` | 16 |
| `gemma-core .../model/expression/experiment` | 14 |
| `gemma-core .../persistence/service/expression/experiment` | 12 |
| `gemma-rest .../rest` | 11 |
| `gemma-web .../web/taglib` | 10 |
| `gemma-core .../loader/expression/singleCell` | 10 |
| `gemma-core .../loader/expression/geo/singleCell` | 10 |
| `gemma-core .../loader/expression/singleCell/transform` | 9 |
| `gemma-core .../analysis/expression/diff` | 9 |

Concentration is in **external-data loader DTOs** (SRA, GEO, UCSC,
cellxgene, cellbrowser, anndata) — files that just deserialize JSON/XML
into Java objects, plus a long tail of REST `*ValueObject` classes.

---

## 3. Risk patterns

### 3a. `@Data` on JPA / Hibernate-mapped entities — RISK PRESENT BUT NARROW

Gemma's persistent domain model is mapped via Hibernate XML (`*.hbm.xml`,
182 mapped classes), not JPA `@Entity` annotations. Cross-referencing the
182 mapped class FQNs against the 421 Lombok-using files yields **8**
mapped persistent classes that use Lombok:

| File | Lombok used | Severity |
|---|---|---|
| `gemma-core/.../model/expression/bioAssayData/CellTypeAssignment.java` | `@Getter @Setter` | low |
| `gemma-core/.../model/expression/bioAssayData/GenericCellLevelCharacteristics.java` | `@Getter @Setter` | low |
| `gemma-core/.../model/expression/bioAssayData/ProcessedExpressionDataVector.java` | `@Getter @Setter` | low |
| `gemma-core/.../model/expression/bioAssayData/RawExpressionDataVector.java` | `@Getter @Setter` | low |
| `gemma-core/.../model/expression/bioAssayData/SingleCellDimension.java` | `@Getter @Setter` | low |
| `gemma-core/.../model/expression/bioAssayData/SingleCellExpressionDataVector.java` | `@Getter @Setter` | low |
| `gemma-core/.../model/expression/experiment/ExpressionExperiment.java` | `@CommonsLog` (logger only — does NOT modify entity contract) | low |
| `gemma-core/.../model/genome/sequenceAnalysis/BlatResult.java` | `@Getter @Setter @EqualsAndHashCode(callSuper = true)` | **HIGH** |

**`BlatResult.java:30`** is the single clear anti-pattern: it's a
Hibernate-mapped subclass (`extends SequenceSimilaritySearchResult`) with
`@EqualsAndHashCode(callSuper = true)`. Lombok auto-generates `equals` /
`hashCode` over all 17 non-transient fields plus super (which includes
the `id`). This is the textbook problem with Lombok on a persistent
entity:

- `hashCode()` touches every field, including lazy-mapped associations
  to `querySequence` / `targetChromosome` — accessing the entity inside
  a `HashSet` / as a `Map` key triggers proxy init and potential N+1.
- Lombok defines `equals` symmetric across all fields, breaking
  Hibernate's id-based identity contract for persisted entities.

**No `@Data` on any Hibernate-mapped persistent class.** The `@Data`
usage is entirely on DTO / value-object / loader-model territory.

### 3b. `@Builder` on entities — NOT PRESENT

None of the 15 `@Builder` users overlap with the 182 Hibernate-mapped
classes. All are `*Config`, `*ValueObject`, REST error payloads, or
internal request shapes (`SearchSettings`, `WellComposedError`,
`DataLoaderConfig`, …).

### 3c. `@CommonsLog` / `@Slf4j` — LOAD-BEARING

`@CommonsLog` is on **188 files**. Every one of them refers to a `log`
field that the Lombok annotation processor generates as

```java
private static final org.apache.commons.logging.Log log =
    LogFactory.getLog(<ThisClass>.class);
```

If we ever migrate off Lombok, this single annotation accounts for ~46%
of all `import lombok.*` lines and every removal also has to add the
field declaration plus the `LogFactory` import to that file.

No `@Slf4j` and no `@Log4j2` usage — the project is consistently on
Apache Commons Logging via Lombok.

### 3d. `@SneakyThrows` — SMALL FOOTPRINT, REVIEWABLE

3 main-source usages:
- `gemma-cli/.../batch/CompositeBatchTaskSummaryWriter.java`
- `gemma-core/.../util/StrictBeanDefinitionValidator.java`
- `gemma-core/.../loader/expression/geo/singleCell/GeoSingleCellDetector.java`

`@SneakyThrows` defeats checked-exception discipline and should always
be revisited at migration time; with only 3 sites this is trivial to
unwrap manually.

### 3e. Delombok-for-Javadoc dependency

Root pom drives `lombok-maven-plugin 1.18.20.0` at `prepare-package`
into a `delombok` output dir consumed by the Javadoc plugin. Migrating
Lombok off means the javadoc aggregator config (~lines 959–974 of root
pom) also needs to drop the `${project.basedir}/gemma-*/target/delombok`
source paths.

---

## 4. Records-as-alternative candidates

Best `lombok.Value` → `record` candidates (immutable, no inheritance
beyond interfaces, no JPA mapping):

| File | Why it's a clean record candidate |
|---|---|
| `gemma-core/.../model/expression/experiment/ExpressionExperimentIdAndShortName.java` | 2 fields (`Long id`, `String shortName`); implements `Identifiable` only |
| `gemma-core/.../core/util/runtime/CpuInfo.java` | runtime info DTO |
| `gemma-core/.../core/util/runtime/MemInfo.java` | runtime info DTO |
| `gemma-core/.../core/util/runtime/FileLockInfo.java` | runtime info DTO |
| `gemma-core/.../core/util/locking/FileLockInfo.java` | distinct lock-info DTO |
| `gemma-core/.../core/util/SimpleRetryPolicy.java` | retry config |
| `gemma-core/.../core/util/SimpleRetryContext.java` | retry context |
| `gemma-core/.../core/analysis/preprocess/svd/SVDResult.java` | analysis output |
| `gemma-core/.../core/analysis/preprocess/batcheffects/BatchConfound.java` | analysis output |
| `gemma-core/.../persistence/util/Filter.java` | filter expression value |
| `gemma-core/.../persistence/util/Subquery.java` | sub-query value |
| `gemma-core/.../persistence/util/Sort.java` | sort spec |
| `gemma-core/.../loader/expression/simple/model/SimpleCharacteristic.java` | loader DTO |

**Caveat: the `Identifiable` interface contract.** Most likely it
exposes `Long getId()` and Java records auto-generate the accessor
`id()`, not `getId()`. Records cannot define a same-named instance field
with a custom accessor name without an explicit method, so the
conversion needs either an explicit `Long getId() { return id; }` on the
record or an interface-shape adjustment. Same caveat for any candidate
implementing a getter-style interface.

The SRA / cellxgene / cellbrowser / UCSC / GEO **loader DTOs** look
record-shaped on the surface but are deserialized by Jackson / XML
bindings and may use mutable setter-based binding. Records work with
Jackson (constructor binding) but each loader needs its binding mode
verified before migration. Treat the loader package as record-eligible
but **after** a deserialization-binding audit.

---

## 5. Recommendation: **KEEP + targeted partial cleanup**

Rationale:

1. **Risk surface is small and tractable.** Out of 421 files, the only
   genuinely problematic Lombok use against the Hibernate model is
   `BlatResult.java`'s `@EqualsAndHashCode(callSuper = true)`. The
   `@Getter`/`@Setter`-only entities are mechanically equivalent to
   hand-written accessors and carry no JPA risk.

2. **`@CommonsLog` carries 188 files of churn for zero structural
   benefit.** If we ripped out Lombok wholesale, ~46% of the diff would
   be mechanical logger re-declarations. That's a poor trade for a
   library that currently works on the target stack (JDK 17 + Spring 6).

3. **Lombok currently works on JDK 17 + Spring 6.** The historical
   pattern is that each JDK major release needs a Lombok point-release
   patch, but the ecosystem catches up within weeks. The
   `pavlab-starter-parent` pins the version centrally, so JDK 21 / 25
   bumps are a parent-parent change, not a Gemma change.

4. **Records are a future win, not a current must-do.** The
   `lombok.Value` → `record` migration is mechanical for the ~13 clean
   candidates listed in §4. None of them block Spring 6 work; they're a
   gradual-cleanup pile.

Recommended action: **MIGRATE_PARTIAL.** Keep Lombok as the project
default for new code, but address the one real anti-pattern now and the
checked-exception escapes opportunistically.

---

## 6. Specific migration targets (if MIGRATE_PARTIAL is approved)

Priority 1 (do this; small, surgical, fixes real JPA risk):

- [ ] `gemma-core/src/main/java/ubic/gemma/model/genome/sequenceAnalysis/BlatResult.java`
      Replace `@EqualsAndHashCode(callSuper = true)` with a hand-written
      id-based `equals`/`hashCode` (Hibernate-safe identity, matches the
      pattern used elsewhere in the Gemma model layer). Keep
      `@Getter`/`@Setter`.

Priority 2 (review when touching the file anyway):

- [ ] `gemma-cli/.../batch/CompositeBatchTaskSummaryWriter.java` — unwrap `@SneakyThrows`
- [ ] `gemma-core/.../util/StrictBeanDefinitionValidator.java` — unwrap `@SneakyThrows`
- [ ] `gemma-core/.../loader/expression/geo/singleCell/GeoSingleCellDetector.java` — unwrap `@SneakyThrows`

Priority 3 (record conversions — opportunistic, one or two per session):

- [ ] `ExpressionExperimentIdAndShortName.java` (smallest, ideal first cut)
- [ ] `CpuInfo.java`, `MemInfo.java`, `FileLockInfo.java` ×2
- [ ] `SimpleRetryPolicy.java`, `SimpleRetryContext.java`
- [ ] `Sort.java`, `Filter.java`, `Subquery.java` (after checking they're not extended)
- [ ] Loader DTOs — only after deserialization-binding audit (§4 caveat)

Explicitly **out of scope** for Phase 3:

- Mass `@CommonsLog` removal (188-file churn for no functional gain).
- Mass `@Data` removal on REST `*ValueObject` (no risk; serialization
  contracts are stable; replacement is verbose).
- The `delombok` javadoc pipeline (keep until Lombok itself is removed).

---

## 7. Open questions for Paul

1. **`BlatResult` equality fix.** Is there an established id-based
   `equals`/`hashCode` template in the Gemma codebase the migration
   should mirror, or should I write a fresh one based on the
   `AbstractIdentifiable` pattern? (Quick grep suggests
   `AbstractIdentifiable` and `AbstractDescribable` may already provide
   `id`-based equals; if so, `BlatResult` may just need to drop the
   Lombok annotation and inherit.)

2. **JDK upgrade roadmap.** Is the Phase 3 target strictly JDK 17, or
   are JDK 21 / 25 on the horizon? Lombok point-release lag matters
   more if we're chasing the latest LTS; if we're settling on 17 for
   the foreseeable future, the "Lombok breaks on every JDK release"
   concern is moot.

3. **`pavlab-starter-parent` ownership.** Lombok version is centrally
   pinned in the parent (`1.2.29`). Do we own that parent? If a CVE or
   JDK-compat patch is needed, can we cut a parent release ourselves,
   or are we blocked on an upstream maintainer?

4. **Records vs `@Value` policy.** Is the team comfortable adopting
   Java records as the default for new immutable value classes (so the
   `@Value` import count stops growing), or do you want to keep
   project-wide consistency with the existing Lombok style?

5. **Delombok-for-Javadoc.** The javadoc aggregator currently depends
   on delomboked sources. Are we still producing/publishing Gemma
   javadoc? If not, the `lombok-maven-plugin` invocation can probably
   be retired independent of any Lombok migration.
