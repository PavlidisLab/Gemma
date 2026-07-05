# Phase 3 cruft inventory + LoC endstate projection

**Date:** 2026-05-19
**Branch baseline:** `phase2-acl-migrate` HEAD `342cbb0a1b`
**Status:** recce only. No code changes; this is a measurement + projection doc.

> **Audit note (2026-05-19, cruft-delete agent off `db5c0540af`):** ran a
> conservative deletion sweep against this inventory and deleted **zero**
> files. Every Section 1/2/3 line item is either (a) on an in-flight
> agent's avoid-list (persister `*`), (b) gated on a multi-session migration
> not yet landed (AfterInvocation Phase C, gsec Phase C adapter drop,
> gemma-web retirement), (c) explicitly KEEP-INVESTIGATE pending audit (§4),
> or (d) the verdict turned out wrong on grep: `Reporter.java` is referenced
> by `SequenceManipulation.java`; `SidValueObject` is referenced by
> `SecurityController` + `SecurityInfoValueObject`; the §1.7 XML stubs
> carry in-tree comments stating they MUST remain to preserve the
> `classpath*:ubic/gemma/applicationContext-*.xml` wildcard-loader
> contract. The "Phase 3 cruft inventory" is best read as **a projection
> of where the LoC will land after the planned migrations finish**, not as
> a queue of safe-now deletions.

This doc inventories Java (and adjacent config) that is **queued to disappear** or
**queued to shrink** as Phase 3 modernization lands, with concrete file paths +
line counts + a verdict per item (DELETE / SHRINK-BY / KEEP-INVESTIGATE). Section
5 turns the inventory into a single endstate Java LoC projection with an error
bar.

`gemma.gsec.*` is excluded — a separate agent is migrating it; the gsec absorption
roadmap (`GSEC_ABSORPTION_ROADMAP.md`) covers that surface separately and is
referenced where relevant.

---

## Current Java baseline

`find gemma-{core,rest,web,cli}/src/main/java -name '*.java' | xargs wc -l`:

| Module | Java LoC | Files |
|---|---:|---:|
| `gemma-core` | 209 751 | (large) |
| `gemma-rest` | 13 401 | 143 |
| `gemma-web` | 25 597 | 160 |
| `gemma-cli` | 23 468 | 174 |
| **TOTAL** | **272 217** | |

XML config under `src/main/resources`:

| Module | XML LoC |
|---|---:|
| `gemma-core/src/main/resources` | 3 349 |
| `gemma-rest/src/main/resources` | 218 |
| `gemma-web/src/main/resources` | 1 416 |
| `gemma-cli/src/main/resources` | (small) |

Of the gemma-core XML, **3 221 LoC are `.hbm.xml`** (63 files); the rest is Spring
context + `hibernate.cfg.xml` (97) + a 31-line `applicationContext-component-scan.xml`.

---

## Section 1: definitely-going (already-planned retirements)

### 1.1 persisterHelper family — **DELETE 2 581 LoC, retain ~126**

Per `PERSISTER_REPLACEMENT_ROADMAP.md`, the entire chain retires over ~9.5 sessions.
Already in flight: `EeWriteServiceImpl.java` exists at 539 LoC as the explicit
replacement for `ExpressionPersister.java`.

Files under `gemma-core/src/main/java/ubic/gemma/persistence/persister/`:

| File | LoC | Verdict |
|---|---:|---|
| `Persister.java` | 80 | DELETE |
| `PersisterHelper.java` | 25 | DELETE |
| `AbstractPersister.java` | 190 | DELETE |
| `CommonPersister.java` | 230 | DELETE |
| `GenomePersister.java` | 950 | DELETE (logic rehomes to `GeneWriteService`, ~200 LoC) |
| `ArrayDesignPersister.java` | 139 | DELETE |
| `ExpressionPersister.java` | 154 | DELETE (already partly replaced; `EeWriteServiceImpl` is the target) |
| `RelationshipPersister.java` | 97 | DELETE |
| `PersisterHelperImpl.java` | 45 | DELETE |
| `package-info.java` | 6 | DELETE |
| `ArrayDesignsForExperimentCache.java` | 126 | KEEP — referenced by GEO loader pre-prepare path |
| `EeWriteServiceImpl.java` | 539 | KEEP — the replacement |
| **Subtotal — DELETE** | **1 916** | |
| **Subtotal — KEEP** | **665** | |

Net Section 1.1: **−1 916 LoC**, partially offset by ~200 LoC new
`GeneWriteService` + small per-entity `BusinessKeyResolver` classes (~150 LoC est).
**Net delta: ~−1 600 LoC.**

### 1.2 Coexpression subsystem stubs — **NOT a thing anymore**

The brief asked for `CoexpressionService` / `GeneCoexpressionService` /
`Probe2ProbeCoexpressionService` stub LoC. Verified by
`find … -iname '*probe2probe*' -o -iname '*genecoexpressionanalysis*'`: **zero
hits**. The gene-gene-network coexpression code path was fully removed in an
earlier session; what survives under `…/analysis/expression/sampleCoexpression/`
is the **sample-coexpression** (per-EE sample-vs-sample correlation matrix) used
by outlier detection and is in active use (`PreprocessorServiceImpl`,
`OutlierDetectionServiceImpl`, `ExpressionExperimentReportServiceImpl`,
`AnalysisUtilServiceImpl`, `DataUpdaterImpl`, `ExpressionExperimentPlatformSwitchService`).

**Net delta: 0 LoC.** Recategorize as KEEP — surviving code is live, not a stub.

### 1.3 `BaseAclAdvice` + `AclAdvice` — **gsec-side; rolls into absorption**

`gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/AclAdvice.java`
is **133 LoC** (the Gemma subclass). The 472-LoC `BaseAclAdvice` lives in the gsec
jar and per `GSEC_ABSORPTION_ROADMAP.md` Phase A "moves" then Phase D "collapses
into `AclAdvice`" — net **0 LoC added to Gemma, ~472 LoC of inheritance
eliminated** when the absorption finishes. **Counted under Section 1.5** below
(gsec absorption row) to avoid double-counting.

### 1.4 AfterInvocation providers — **DELETE ~1 086 LoC at Phase C completion**

Per `AFTER_INVOCATION_MIGRATION.md` + `AFTER_INVOCATION_PHASE_C_PLAN.md`, once
`@EnableMethodSecurity` adoption completes (Phase C), the 14 wired providers + the
`AfterInvocationProviderManager` bridge go away. All survive long enough to land
the `@PostAuthorize`/`@PostFilter` annotation migration (call sites already
converted in Phase A/B per the roadmap).

Files under `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/`
(14 files, **1 086 LoC**):

| File | LoC |
|---|---:|
| `AclEntryAfterInvocationPrivateCollectionFilteringProvider.java` | 60 |
| `AclEntryAfterInvocationByAssociationFilteringProvider.java` | 51 |
| `AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider.java` | 31 |
| `AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider.java` | 33 |
| `AclEntryAfterInvocationCollectionFilteringProvider.java` | 185 |
| `AclEntryAfterInvocationStreamFilteringProvider.java` | 91 |
| `AclEntryAfterInvocationValueObjectReadProvider.java` | 104 |
| `AclEntryAfterInvocationCompositeSequenceCollectionByArrayDesignFilteringProvider.java` | 53 |
| `AclEntryAfterInvocationOwnedCollectionFilteringProvider.java` | 58 |
| `AclEntryAfterInvocationByAssociationCollectionFilteringProvider.java` | 72 |
| `AclEntryAfterInvocationValueObjectCollectionReadProvider.java` | 123 |
| `AclEntryAfterInvocationDataVectorCollectionByExpressionExperimentFilteringProvider.java` | 52 |
| `AclEntryAfterInvocationValueObjectMapReadProvider.java` | 87 |
| `AclEntryAfterInvocationQuietReadProvider.java` | 86 |
| **TOTAL** | **1 086** |

Plus `MethodSecurityConfig.java` (226 LoC) shrinks by ~150 LoC when the
`@EnableGlobalMethodSecurity` override chain (`afterInvocationManager`,
`accessDecisionManager`, `runAsManager`) is replaced with plain
`@EnableMethodSecurity`. Three Gemma-specific by-association filters
(`*ByArrayDesignFilteringProvider`, `*ByResultSetFilteringProvider`) need to
survive as **custom `AuthorizationManager<T>` beans** (estimated ~200 LoC each →
~600 LoC). Plus annotation churn on ~174 `@Secured(AFTER_ACL_*)` call sites that
become `@PostAuthorize`/`@PostFilter` (small, ~250 LoC net edit; mostly textual).

**Net delta Section 1.4: −1 086 − 150 + 600 = ~−636 LoC.**

### 1.5 gsec 13 XML-wired-only classes — **DELETE ~600 LoC, but offset by absorption**

Per `GSEC_ABSORPTION_ROADMAP.md` Section 1:

- gsec total: **7 126 LoC** across 65 classes shipped from the external jar.
- 13 classes never imported by Gemma source (XML-wired only) vanish with the XML
  in Phase C: `AclEntryAfterInvocationMapFilteringProvider`,
  `AclEntryAfterInvocationMapValueFilteringProvider`,
  4× `*ValueObject*FilteringProvider`, 4× `AclEntry*Voter`, `AclImpl`,
  `IgnoreAcl`, `ManualAuthenticationServiceImpl`,
  `ObjectTransientnessRetrievalStrategy`. Roughly ~600 LoC (the 4 ValueObject
  filtering providers + 4 voters dominate).
- `GsecAclServiceAdapter` (Phase B) — 206 LoC inside `GemmaAclConfiguration` (gemma-core).
- `BaseAclAdvice` parent collapses into `AclAdvice` (Phase D), losing ~150 LoC of
  inheritance plumbing.
- Total gsec material that **stays** (rehomed to gemma-core but not deleted):
  ~6 526 LoC. So gemma-core Java grows by ~6 526 LoC at Phase A landing, and
  the `pavlab:gemma-gsec` jar disappears from `pom.xml`.

**The honest accounting:** gsec absorption is **net +6 526 LoC into gemma-core**
(material moves IN from a separate jar), partially offset by ~−800 LoC of
deduplication (Sid-hierarchy unification, adapter deletion, XML retirement).
Counted as **+5 700 LoC into the gemma-core Java total** in Section 5.

### 1.6 `SidValueObject` + parallel gsec sid types — **DELETE 120 LoC**

`gemma-web/src/main/java/ubic/gemma/web/controller/common/auditAndSecurity/SidValueObject.java`
(120 LoC) is the gemma-web mirror; Phase B of absorption deletes it along with
gsec's `AclPrincipalSid`/`AclGrantedAuthoritySid` `Sid`-implementing classes.

**Net delta: ~−120 LoC** (gemma-web).

### 1.7 `applicationContext-*` XML stragglers — **DELETE 186 LoC XML**

Spring context XML still in tree:

| File | LoC | Verdict |
|---|---:|---|
| `gemma-core/.../applicationContext-component-scan.xml` | 31 | DELETE (covered by `@ComponentScan` annotations) |
| `gemma-rest/.../applicationContext-analytics.xml` | 13 | DELETE |
| `gemma-rest/.../applicationContext-component-scan.xml` | 12 | DELETE |
| `gemma-web/.../applicationContext-security.xml` | 86 | KEEP (auth filter chain — converts to Java config when gemma-web replaced) |
| `gemma-web/.../applicationContext-metrics.xml` | 9 | DELETE |
| `gemma-web/.../applicationContext-component-scan.xml` | 11 | DELETE |
| `gemma-web/.../applicationContext-serviceBeans.xml` | 8 | DELETE |
| `gemma-cli/.../applicationContext-component-scan.xml` | 16 | DELETE |
| **DELETE subtotal** | **100** | XML, not Java |
| **gemma-web KEEP** | **86** | dies with module |

Plus `applicationContext-gsec.xml` (481 lines, ships in the gsec jar) — DELETE
during gsec absorption Phase C. Replaced by ~500 LoC of new Java config.

**Net delta: −100 XML LoC (not in Java total), +500 LoC Java config inside
gsec absorption.** Already counted in 1.5.

### 1.8 Hand-written getters/setters in ValueObjects — **SHRINK ~2 500 LoC (estimate)**

`find gemma-core/src/main/java -name '*ValueObject.java' | wc -l` → **79 files,
10 778 LoC**. Filtering for files without `@Data` or `@Getter`:

```
files-without-Lombok: 35
LoC-of-those:        5 206
```

Per project memory, the in-flight Lombok cleanup converts these one batch at a
time (`96ce45d4ce`, "convert 6 ValueObjects to Lombok", removed ~180 LoC, so
~30 LoC saved per VO). With 35 files remaining: **~30 × 35 = ~1 050 LoC saved**.
Confidence is decent — the conversion ratio is from real recent commits.

**Net delta Section 1.8: ~−1 050 LoC.**

---

### Section 1 sum

| Item | LoC delta (Java) |
|---|---:|
| 1.1 persisterHelper retirement | −1 600 |
| 1.2 Coexpression network stubs | 0 (already gone) |
| 1.3 BaseAclAdvice inheritance | (in 1.5) |
| 1.4 AfterInvocation providers | −636 |
| 1.5 gsec absorption (net IN to gemma-core) | **+5 700** |
| 1.6 SidValueObject + gsec sid types | −120 |
| 1.7 applicationContext XML stragglers | (not Java) |
| 1.8 ValueObject Lombok cleanup | −1 050 |
| **Section 1 net** | **+2 294 LoC** |

The Section 1 net is **positive** because gsec absorption is a large net-in
event. If we instead measure "Java currently inside Gemma source tree that
disappears" (excluding the gsec rehome), Section 1 is **−3 406 LoC**.

---

## Section 2: gemma-web retirement — the big one

Per memory: `gemma-web` is "walking dead", being replaced by `gemma-curation-ui`
(a SPA frontend, not in this repo). When the replacement lands the entire module
goes.

Inventory by file type:

| Path | LoC / count | Verdict |
|---|---:|---|
| `gemma-web/src/main/java` (Java) | **25 597 LoC** (160 files) | DELETE (entire module) |
| `gemma-web/src/main/webapp/**/*.jsp` | 4 387 LoC | DELETE |
| `gemma-web/src/main/webapp/**/*.js` | 142 295 LoC | DELETE |
| `gemma-web/src/main/webapp/**/*.css` | 5 876 LoC | DELETE |
| `gemma-web/src/main/webapp/**` (all files) | **1 147 files** | DELETE |
| `gemma-web/src/main/webapp/**` (jsp+js+css+xml+html+tag+tld) | 153 541 LoC | DELETE |
| `gemma-web/src/main/resources` | 1 416 LoC | DELETE |
| `gemma-web/pom.xml` | 260 LoC | DELETE |

**Section 2 net (Java-only): −25 597 LoC.**

Caveat: a small amount of "controller logic that should have been in services"
will need to migrate INTO gemma-rest (the SPA backend). Rough estimate: 10% of
gemma-web's java will rehome — ~2 500 LoC into gemma-rest. That makes the **net
Java delta −23 100 LoC**; gemma-rest grows by ~2 500 LoC.

This is the single largest Phase 3 retirement target. The non-Java payload
(JS/JSP/CSS, ~150 KLOC) is even larger but outside the Java endstate
projection.

---

## Section 3: SHRINK-BY items

### 3.1 `ExpressionExperimentServiceImpl` — **SHRINK ~600 LoC**

`gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceImpl.java`:
**1 235 LoC** (down from the ~2 073 LoC quoted in `EE_SERVICE_DECOMPOSITION_ROADMAP.md`
because slice 2 + slice 3 of the decomposition have already landed).

Already in tree as sibling services (the decomposition is partly done):
`ExpressionExperimentReadService(Impl)`, `ExpressionExperimentWriteService(Impl)`,
`ExpressionExperimentSearchService(Impl)`, `ExpressionExperimentSubSetService(Impl)`,
`ExpressionExperimentGeoService(Impl)`,
`ExpressionExperimentFilterRewriteHelperService.java`,
`ExpressionExperimentSetService(Impl)`,
`ExpressionExperimentSetValueObjectHelper(Impl)`.

Per the roadmap, future slices target ~600–800 LoC remaining facade.

**Net delta: ~−500 LoC** (impl shrinks; some of the removed lines stay as
delegation glue).

### 3.2 `*DaoImpl` Hibernate boilerplate — **SHRINK ~3 000 LoC (estimate)**

`find gemma-core/src/main/java -name '*DaoImpl.java'`: **51 files, 15 967 LoC**.
Many have repetitive HQL boilerplate (string-concatenated queries, manual
`getCurrentSession().createQuery(...)`, manual result-list casts) that Spring
Data JPA repositories or HQL-as-resources could compress by ~20%.

This is **not on the Phase 3 critical path** — it's listed for visibility. No
migration is queued, but the surface is large enough to mention.

**Net delta: 0 LoC committed; ~−3 000 LoC potential.** Recategorize as
KEEP-INVESTIGATE.

### 3.3 `BusinessKey.java` split — **SHRINK ~200 LoC**

`gemma-core/src/main/java/ubic/gemma/persistence/util/BusinessKey.java`:
**934 LoC**. Per persister roadmap section 4, once `Chromosome`/`QuantitationType`/
`BioAssayDimension` BK logic is lifted **in** (~150 LoC added), then the file is
split per-entity into smaller resolvers (the new `BusinessKeyResolver<T>`
interface). The net is essentially neutral: ~+150 LoC for lifted logic, ~−250
LoC of structural deduplication when the resolvers become Spring beans with
focused responsibilities.

**Net delta: ~−100 LoC.**

### 3.4 `*.hbm.xml` files — **SHRINK ~2 000 LoC (long-horizon)**

`find gemma-{core,rest,web,cli}/src/main/resources -name '*.hbm.xml'`: **63 files,
3 221 LoC**. Once JPA annotations are the single source of truth on entity
classes, ~60% of HBM XML duplicates already-existing annotations and can be
deleted.

This is **NOT in Phase 3 scope** (it requires entity-by-entity validation
against the live DB). Listed for context; no Java delta this phase.

**Net delta: 0 (Java); ~−2 000 (XML, deferred).**

### Section 3 sum (Java only)

| Item | LoC delta |
|---|---:|
| 3.1 EESI shrink | −500 |
| 3.2 DaoImpl boilerplate | 0 (KEEP-INVESTIGATE) |
| 3.3 BusinessKey split | −100 |
| 3.4 hbm.xml | 0 (XML, not Java) |
| **Section 3 net** | **−600 LoC** |

---

## Section 4: KEEP-INVESTIGATE

### 4.1 `gemma-cli/src/main/java/ubic/gemma/apps/*`

105 files, **17 956 LoC**. 5 carry `@Deprecated` markers:
`ArrayDesignProbeRenamerCli.java`, `ExpressionExperimentDataFileGeneratorCli.java`,
`FactorValueMigratorCLI.java`, `MeshTermFetcherCli.java`, `RNASeqBatchInfoCli.java`.
Without a production-cron audit, these can't be confidently retired.

The 5 deprecated files sum to ~600 LoC (rough); audit needed.

**Net delta: 0 firmly; potential −600 LoC if all 5 confirmed obsolete.**

### 4.2 `Reporter.java`

`gemma-core/src/main/java/ubic/gemma/core/loader/expression/arrayDesign/Reporter.java`:
127 LoC. Project memory hints this is unused. Worth verifying with a `grep -rln
'Reporter'` audit before deletion (risk of false hit on the common word
"Reporter"; needs `import ubic.gemma.core.loader.expression.arrayDesign.Reporter`
to be reliable).

**Net delta: 0 firmly; potential −127 LoC.**

### 4.3 `BioAssay.java` JSON fields

`gemma-core/src/main/java/ubic/gemma/model/expression/bioAssay/BioAssay.java`:
**356 LoC**. Some JSON-serialized fields may be dead; needs a usage audit by
`grep` against gemma-rest serializers and gemma-curation-ui consumers. Not
currently scheduled.

**Net delta: 0 firmly.**

### 4.4 `@Deprecated` annotations

Counts of `@Deprecated` occurrences across `src/main/java`:

| Module | `@Deprecated` occurrences |
|---|---:|
| gemma-core | 123 |
| gemma-rest | 3 |
| gemma-web | 7 (dies with module) |
| gemma-cli | 5 |

123 in gemma-core is significant. Most are method-level (annotation on a method
that's been superseded), not class-level. A focused audit could probably retire
~30% of these confidently → ~40 methods, ~400 LoC.

**Net delta: 0 firmly; potential ~−400 LoC.**

### Section 4 sum

Potential range: **0 to −1 100 LoC**. Treated as **−500 ± 500 LoC** in the final
projection.

---

## Section 5: endstate projection

### Current

Java source LoC (sum across `gemma-{core,rest,web,cli}/src/main/java`):
**272 217 LoC** (as measured 2026-05-19).

XML config (`src/main/resources`): **4 983 LoC** of which **3 221 LoC** is
`.hbm.xml` (entity mappings) and **1 762 LoC** is Spring/Hibernate context XML.

### Phase 3 deltas

| Driver | Java delta |
|---|---:|
| §1.1 persisterHelper retirement | −1 600 |
| §1.4 AfterInvocation providers | −636 |
| §1.5 gsec absorption (net IN to gemma-core, after dedupe) | +5 700 |
| §1.6 SidValueObject + parallel sid types | −120 |
| §1.8 ValueObject Lombok cleanup | −1 050 |
| §2 gemma-web entire module DELETE | −25 597 |
| §2 small rehome to gemma-rest | +2 500 |
| §3.1 ExpressionExperimentServiceImpl shrink | −500 |
| §3.3 BusinessKey split | −100 |
| §4 conservative audit gains (mid-range) | −500 |
| **Sum** | **−21 903** |

### Projection

**Phase 3 endstate Java LoC: ~272 200 − 21 900 = ~250 300.**

With error bar: **250 300 ± 4 000**.

The ±4 000 covers:
- gsec absorption net could land between +5 000 and +6 500 (depends on how much
  Sid-hierarchy / adapter dedupe actually saves).
- gemma-web's contribution to a future gemma-rest service layer could be
  anywhere from +1 000 to +5 000 (the SPA backend needs SOME controller/service
  logic).
- Section 4 audit gains could be 0 or could be −1 100.
- New Java replacing inlined XML (Phase C gsec) is ~+500 LoC already counted in
  the gsec absorption row but could swing ±300.

### Sanity check vs the brief's hand-wave

The original brief estimated "roughly −15K to −30K Java net". The
**concrete-inventory answer is −21.9K**, comfortably inside that band. The two
largest individual drivers (gemma-web −25.6K, gsec absorption +5.7K) **net
−19.9K on their own**, and everything else combined is **another −2K**. The
top-line number is dominated by the gemma-web retirement; if that slips by a
year, the whole projection slips by a year.

### What we are NOT counting (out of scope for "endstate Java")

- `.hbm.xml` retirement (~−2 000 lines XML, when JPA annotations become the
  source of truth — deferred indefinitely).
- `applicationContext-gsec.xml` (~−481 lines XML, replaced by ~+500 LoC Java
  inside the gsec absorption row).
- gemma-web's 153 KLOC of JSP/JS/CSS — outside Java scope but is the dominant
  bytes-on-disk retirement.
- gemma-rest growth driven by the gemma-curation-ui contract (counted: +2 500;
  real value depends on UI requirements not visible from the Gemma side).

---

## Appendix A: file-path summary for quick-grep verification

| Item | Path |
|---|---|
| persisterHelper family | `gemma-core/src/main/java/ubic/gemma/persistence/persister/` |
| AfterInvocation providers | `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/AclEntryAfterInvocation*.java` |
| MethodSecurityConfig | `gemma-core/src/main/java/ubic/gemma/core/security/MethodSecurityConfig.java` (226 LoC) |
| AclVoterAuthorizationManager | `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/AclVoterAuthorizationManager.java` (114 LoC) |
| ExpressionExperimentServiceImpl | `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceImpl.java` (1 235 LoC) |
| EeWriteServiceImpl | `gemma-core/src/main/java/ubic/gemma/persistence/persister/EeWriteServiceImpl.java` (539 LoC) |
| BusinessKey | `gemma-core/src/main/java/ubic/gemma/persistence/util/BusinessKey.java` (934 LoC) |
| BioAssay | `gemma-core/src/main/java/ubic/gemma/model/expression/bioAssay/BioAssay.java` (356 LoC) |
| Reporter | `gemma-core/src/main/java/ubic/gemma/core/loader/expression/arrayDesign/Reporter.java` (127 LoC) |
| SidValueObject (gemma-web mirror) | `gemma-web/src/main/java/ubic/gemma/web/controller/common/auditAndSecurity/SidValueObject.java` (120 LoC) |
| gemma-web module root | `gemma-web/` (25 597 Java + 153 541 webapp + 1 416 resources LoC) |
| `applicationContext-gsec.xml` | ships in `pavlab:gemma-gsec` jar (481 lines) |

## Appendix B: reproducing the counts

```bash
# Total Java LoC
find gemma-{core,rest,web,cli}/src/main/java -name '*.java' | xargs wc -l | tail -1
# Per-module Java LoC
for m in gemma-core gemma-rest gemma-web gemma-cli; do
  find "$m/src/main/java" -name '*.java' | xargs wc -l | tail -1 | awk -v m=$m '{print m": "$1}'
done
# XML config
find gemma-{core,rest,web,cli}/src/main/resources -name '*.xml' | xargs wc -l | tail -1
# hbm.xml subset
find gemma-{core,rest,web,cli}/src/main/resources -name '*.hbm.xml' | xargs wc -l | tail -1
# VOs without Lombok
find gemma-core/src/main/java -name '*ValueObject.java' | xargs grep -L '@Data\|@Getter' | xargs wc -l | tail -1
# DaoImpl total
find gemma-core/src/main/java -name '*DaoImpl.java' | xargs wc -l | tail -1
# gemma-web breakdowns
find gemma-web/src/main/java -name '*.java' | xargs wc -l | tail -1
find gemma-web/src/main/webapp -type f \( -name '*.jsp' -o -name '*.js' -o -name '*.css' \) | xargs wc -l | tail -1
# @Deprecated per module
for m in gemma-core gemma-rest gemma-web gemma-cli; do
  grep -rh '@Deprecated' "$m/src/main/java" | wc -l | awk -v m=$m '{print m": "$1" @Deprecated"}'
done
```

All numbers in this doc are reproducible from a clean checkout at HEAD
`342cbb0a1b` (or whatever HEAD is current on `phase2-acl-migrate`) using those
commands.
