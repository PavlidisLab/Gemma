# PERSISTER_SHRINK_S4_PROGRESS.md — recce for retiring `Persister` / `PersisterHelper` / `PersisterHelperImpl`

**Baseline:** `phase2-acl-migrate` @ `12bddcb10619ef79cc42a119ce9a86ad8b1c04ab`
**Status:** recce-only — no code changes in this commit. Picks Option C from the S4 brief.

---

## 1. Why no code in S4

The S4 brief lists three options:

- **A** — full delete (migrate ~70 test sites + 5 gemma-web callsites + delete the 3 interfaces).
- **B** — partial: migrate test fixtures only, leave gemma-web for a later session.
- **C** — recce-only.

After surveying the live dependency graph (Section 2) the only reasonable next slice is **C**. Both A and B require building out typed write-services that don't yet exist (Section 3), or threading per-call `Map<String, ExternalDatabase>` etc. through every fixture method. Both touch >30 test classes and are S5-shaped work, not S4-shaped. The agent brief explicitly accepts conservative outcomes; this is the conservative outcome with the migration shape worked out.

The cheap parts of S4 (delete `ExpressionPersister` + `PersisterHelper` interface methods that test fixtures still hit) have already happened in S1/S2/S3 — what's left are the high-fanout sites.

---

## 2. Live dependency graph (post-S3)

### 2.1 Production callers of `Persister` / `PersisterHelper` interfaces

`PersisterHelper` is the bean type external code injects via `@Autowired`. Direct field-injection sites in production code outside `gemma-core/src/main/java/ubic/gemma/persistence/persister/`:

| File | Module | Field type | Calls |
|---|---|---|---|
| `gemma-web/.../BibliographicReferenceController.java` | gemma-web | `Persister persisterHelper` | `persist(bibRef)` |
| `gemma-web/.../ExpressionExperimentController.java` | gemma-web | `Persister persisterHelper` | `persist(newMaterial)`, `persist(publication)` x2 |
| `gemma-web/.../ExpressionExperimentEditController.java` | gemma-web | `Persister persisterHelper` | `persist(newMaterial)` |
| `gemma-core/.../EeWriteServiceImpl.java` | gemma-core | `PersisterHelper persisterHelper` | reaches `PersisterHelperImpl` via `AopProxyUtils.getSingletonTarget` for the `persistTaxon` / `persistBibliographicReference` / `persistExternalDatabase` / `fillInDatabaseEntry` / `getSessionFactory` helpers (no `persist(T)` call) |

**Five production call sites total, all in gemma-web (walking-dead per `feedback_gemma_web_replacement` memory).** The `EeWriteServiceImpl` reference is structural — it doesn't call the `Persister` interface, only the package-private dispatch helpers that PHI re-exposes (`persistTaxon`, etc.).

### 2.2 Test callers of `persisterHelper`

70 source lines across 20 test files in `gemma-core/src/test`:

- **Central fixtures (the blockers):**
  - `PersistentDummyObjectHelper.java` — 26 lines; the canonical test data factory. Persists ArrayDesign, BioAssay, BioMaterial, BioSequence, Chromosome, ExternalDatabase, ExternalDatabasePopulation, ExpressionExperiment (+ `prepare`), Gene, GeneProduct, QuantitationType, Taxon, BibliographicReference, Bio2GeneProduct collection.
  - `BaseSpringContextTest.java` / `BaseSpringContextTest5.java` — 2 lines each (`persisterHelper.persist(ad)` for ArrayDesign fixture).
- **Direct test callers (would migrate alongside the central fixture conversion):** GenomePersisterTest, ArrayDesignServiceTest (6 sites), GeneWriteServiceTest (3), GeneServiceTest, GeneSearchTest, MassIndexerSmokeIntegrationTest, BioSequencePersistTest, ProcessedExpressionDataVectorServiceTest (2), AclAdviceTest, AclAuthorizationTest, AuditTrailDaoTest, AuditEventServiceTest, AuditTrailServiceImplTest, CuratableValueObjectTest (4), TwoChannelMissingValuesTest (4 `persist(ee, prepare(ee))` calls), ExpressionDataSVDTest, NCBIGene2GOAssociationLoaderCLITest, ExpressionExperimentEditControllerTest, AutowireImplRuleTest (comment only).

Distinct entity types persisted via test fixtures: `ArrayDesign`, `Gene`, `GeneProduct`, `BioSequence`, `BioMaterial`, `BioAssay`, `Chromosome`, `Taxon`, `ExpressionExperiment`, `ExpressionExperimentSet`, `BibliographicReference`, `ExternalDatabase`, `QuantitationType`, `Gene2GOAssociation` collection, `BlatAssociation`.

---

## 3. Migration shape

To get to A (full delete), every callsite needs a typed bean call. Current state of typed-write-service coverage:

| Entity | Existing typed bean | Status |
|---|---|---|
| ExpressionExperiment | `EeWriteService.create(ee[, cache])` | exists; PHI's `persist(EE)` already delegates here |
| Gene | `GeneWriteService.upsert(gene)` | exists |
| ExternalDatabase | `ExternalDatabaseService.findOrCreate` | exists |
| BibliographicReference | `BibliographicReferenceService.findOrCreate` | exists |
| Taxon | (none typed) — go through `genomePersister.persistTaxon(taxon, new HashMap<>())` | leaf bean reachable, no service |
| BioSequence | (none typed) — `genomePersister.persistBioSequence(...)` | leaf bean reachable, no service |
| BioMaterial | (none typed) — currently in `EeWriteServiceImpl.persistBioMaterial` (package-private) | not on a public API |
| BioAssay | (none typed) — `EeWriteServiceImpl.persistBioAssay` (package-private) | not on a public API |
| ArrayDesign | (none typed) — `arrayDesignPersister.persistNewArrayDesign(...)` | leaf bean reachable, no public method |
| Chromosome | (none typed) — `genomePersister.persistChromosome(...)` | leaf bean reachable |
| GeneProduct | (none typed) — `genomePersister.persistGeneProduct(...)` | leaf bean reachable |
| QuantitationType | `QuantitationTypeService.findOrCreate` | exists |
| Gene2GOAssociation | (none typed) — `relationshipPersister.persistGene2GOAssociation(...)` | leaf bean reachable |
| ExpressionExperimentSet | (none typed) — `relationshipPersister.persistExpressionExperimentSet(...)` | only one test caller (AclAdviceTest) |
| BlatAssociation | (none typed) — `genomePersister.persistBioSequence2GeneProduct(...)` | leaf bean reachable |

So Option A needs **eight new public methods on the four leaf persister beans** (Common/Genome/ArrayDesign/Relationship). The methods exist as `protected` inside the inheritance chain (now collapsed; see S2_DETAIL); promoting them to `public` is the bulk of the work. Each promotion also needs the per-call `Map<String, ExternalDatabase>` cache parameter (or a varargs-style overload that allocates a fresh map). The PHI's `persist(T)` envelope (FlushMode.MANUAL window + flush) needs to either move into each promoted method or be replaced by `@Transactional`-driven flush-on-commit (see RECCE.md §5 risk #1).

### Suggested execution path for a future S4-real session (2–3 sessions)

1. **S4a (1 session) — promote leaf helpers to public methods on each `@Component`.** Add public no-cache overloads on `CommonPersister`, `GenomePersister`, `ArrayDesignPersister`, `RelationshipPersister` that each open their own FlushMode window. Names: `persistArrayDesign`, `persistGene` (delegates to `GeneWriteService.upsert`), `persistGeneProduct`, `persistBioSequence`, `persistTaxon`, `persistChromosome`, `persistBibliographicReference`, `persistBioAssay`, `persistBioMaterial`, `persistExpressionExperimentSet`, `persistGene2GOAssociations(Collection)`, `persistBlatResults(Collection)`, `persistBlatAssociation`. Compile-clean per add; no callsite changes yet. (BioAssay/BioMaterial methods need to move off `EeWriteServiceImpl` to wherever — keep them where they are and expose them via the EeWriteService interface.)

2. **S4b (1 session) — migrate `PersistentDummyObjectHelper` + the two `BaseSpringContextTest*` files** to inject the typed beans instead of `PersisterHelper`. ~30 of the 70 lines. Everything else compiles because the interfaces still exist. Validate with `mvn -pl gemma-core -am test-compile`.

3. **S4c (1 session) — migrate the remaining test classes** that field-inject `Persister` / `PersisterHelper` directly (GenomePersisterTest, ArrayDesignServiceTest, etc.). Each is 1–6 sites.

4. **S4d — delete the gemma-web 3 controllers' `persisterHelper` field** (either replace with typed beans or accept compile breakage; per memory, gemma-web is being replaced by gemma-curation-ui and can probably be left alone or compile-broken).

5. **S4e — final delete.** Move PHI's `doPersist` dispatch table into the typed-bean methods (the EE arms are still wired through `EeWriteServiceImpl.persistExpressionExperiment` etc. via the AOP-unwrap; that connection survives). Drop `Persister.java`, `PersisterHelper.java`, `PersisterHelperImpl.java`. `EeWriteServiceImpl`'s `persister()` AOP-unwrap helper goes away — replace with direct autowires of `CommonPersister` + `GenomePersister`.

### Risks specific to this migration

- **`EeWriteServiceImpl` still depends on PHI structurally** (see Section 2.1). Not for `persist(T)` — for the helper methods PHI re-exposes. Deleting PHI also requires `EeWriteServiceImpl` to autowire `CommonPersister` + `GenomePersister` directly. Already noted in the recce as Step S5; the bones are in place since S2e broke the chain.

- **FlushMode.MANUAL semantics on `persist(Collection)`.** Today `AbstractPersister.persist(Collection)` opens *one* window for the whole collection. After migration each typed-bean method opens its own window. For batch callers (`PersistentDummyObjectHelper.persistGene2GOAssociations` persists a collection in one shot) this is a perf regression unless the typed-bean method takes a Collection overload. Cheap to add (one method per batch caller — there are 2: Gene2GOAssociation, BlatResult).

- **`prepare(EE)` + `persist(EE, cache)` test fixture pattern.** `PersistentDummyObjectHelper` and `TwoChannelMissingValuesTest` do `c = prepare(ee); persistedEe = persist(ee, c)`. Modern equivalent is `EeWriteService.create(ee, ExpressionExperimentPrePersistService.prepare(ee))` — both already exist. Trivial swap.

- **`AutowireImplRuleTest.java:52`** — comment-only reference to `EeWriteServiceImpl.persisterHelper`; this test enforces an architecture rule (interfaces preferred over impls in injection). When `EeWriteServiceImpl.persisterHelper` field disappears, update the comment.

---

## 4. What's deletable today with zero churn

Examined for "could be killed independently in S4 without the migration above":

- **`PersisterHelper.persist(EE, cache)` and `prepare(EE)` interface methods** — both `@Deprecated` and one-line delegates in PHI. But they have test callers (TwoChannelMissingValuesTest x4, PersistentDummyObjectHelper x4), so deleting forces migration of those sites. Not zero-churn.

- **`PersisterHelperImpl.persistBibliographicReference` / `fillInDatabaseEntry` / `persistExternalDatabase` / `persistTaxon` forwarder helpers** (lines 218–243). All four forward one-line to the autowired leaf bean. They exist because `EeWriteServiceImpl.persister()` reaches them via AOP-unwrap. *Could* delete by having `EeWriteServiceImpl` autowire `CommonPersister` + `GenomePersister` directly. Estimated 30 LOC removed in PHI + 4 callsite changes in `EeWriteServiceImpl`. Zero behavioural risk, but no interface deletion — pure tidy. Punt unless a future agent has slack.

Conclusion: zero deletable interfaces in this session.

---

## 5. Recommendation

Defer S4 to a dedicated 2–3-session arc once the gemma-web replacement (gemma-curation-ui) lands and the 3 controllers can be deleted outright. Then S4a → S4b → S4c → S4e becomes a clean ~3-session sequence with no remaining external `Persister` / `PersisterHelper` callers.

Until then, the chain is contained: `PersisterHelperImpl` is a 300-LOC dispatcher with no further work needed to keep it correct (S2 already broke the inheritance chain and S3 cut the production callers). The cost of leaving it alive is ~12 files of dead-ish surface area; the cost of removing it is high-fanout test fixture churn that isn't worth a single session.
