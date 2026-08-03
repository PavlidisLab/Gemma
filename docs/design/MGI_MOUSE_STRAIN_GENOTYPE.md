# MGI as a mouse-strain / genotype resolution source

**Status:** design / plan — not yet implemented.
**Date:** 2026-08-02.
**Goal:** annotate the **complicated mouse genotypes/strains** that today fall
through to free text. When a sample's strain/genotype is registered in MGI
(Mouse Genome Informatics), resolve it to the canonical MGI strain record and
decompose it into background + allele(s) + affected gene(s), so the curator/agent
can do better than "paste the string verbatim."

Companion to `CELLOSAURUS_CELL_LINE_SEARCH.md` — same "not an ontology, add a
lexical/resolution source" pattern, but with an extra decomposition step because
mouse genotypes are compositional, not single terms.

---

## TL;DR

MGI is **not an ontology** and does not even ship OBO for the data we want — the
strain, allele, and genotype resources are **tab-delimited `.rpt` reports**
(only the Mammalian Phenotype ontology is OBO/OWL, and Gemma already loads MP).
So we do not load MGI as a Jena `OntModel`. Two capabilities, both built on a
streamed parse of the reports into a Lucene index / lookup:

1. **Strain name resolution** — a lexical `OntologyService`-conforming provider
   (mirrors the Cellosaurus provider) that matches a strain nomenclature string
   → MGI strain record, emitting `https://www.informatics.jax.org/strain/MGI:<id>`.
   Also fills the *missing* `strain:` prefix route.
2. **Allele-symbol → gene disambiguation** (a *helper*, not an annotation
   source) — use `MGI_PhenotypicAllele.rpt` to strip allele decoration
   (`Rag1<tm1Mom>`, `Cx43-fl/fl`, `Tg(...)` transgene symbols) down to the
   **gene marker**, so Gemma's existing gene resolver can ground the correct
   `NCBI_GENE` for the correct taxon. This *feeds* the established rule-based
   genotype curation; it does **not** emit its own genotype annotations.

> **This must obey the genotype curation rules** (authority:
> `gemma-curation-agents/docs/curation_rules/05_genotype_efc.md`). Genotype is
> curated as a Statement `gene (NCBI_GENE, correct taxon) + has_genotype
> (GENO_0000222) + manipulation-type (TGEMO)`. MGI supplies **neither** the gene
> URI (Gemma's gene service owns that) **nor** the manipulation type (TGEMO owns
> that). MGI's only job on the genotype side is decoding allele nomenclature to
> the right gene. See the constraints section below — there are traps.

The point is the long tail: of ~118 k MGI strains, **~84 k are coisogenic +
~17 k congenic** — specific *mutant* strains that EFO/TGEMO will never
enumerate. Only ~3.3 k are classic inbred backgrounds (C57BL/6 etc.), which EFO
already covers.

---

## Source facts (MGI reports, fetched 2026-08-02)

From `https://www.informatics.jax.org/downloads/reports/`.

| Report | Size | Rows | Shape |
|---|---|---|---|
| `MGI_Strain.rpt` | 6.2 MB | ~117 884 | `MGI:<id>` · strain nomenclature · strain type |
| `MGI_PhenotypicAllele.rpt` | 31 MB | ~141 076 | `MGI:<id>` · allele symbol · allele name · generation · **allele type** · … · **marker (gene) MGI ID** · gene symbol · RefSeq · Ensembl · gene name |
| `MGI_PhenoGenoMP.rpt` | — | — | genotype → Mammalian Phenotype annotations |
| `MGI_Geno_DiseaseDO.rpt` | — | — | genotype → Disease Ontology (DO) — model-of-disease link |
| `MGI_Nonstandard_Strain.rpt` | — | — | unreviewed nonstandard strain/stock nomenclature |

Everything above is **tab-delimited**, `#`-comment header lines, no hierarchy.
The only OBO/OWL MGI ships is `mp.owl` / `MPheno_OBO.ontology` (already loaded in
Gemma as the MP provider).

**Strain-type distribution** (`MGI_Strain.rpt` col 3):

```
84 577 coisogenic   ← single mutation on a background = the "complicated genotype" tail
16 849 congenic
11 156 Not Applicable
 3 348 inbred strain ← classic backgrounds; EFO already has these
   ...  recombinant inbred / consomic / conplastic
```

**Allele-type distribution** (`MGI_PhenotypicAllele.rpt` col 5, top):

```
51 670 Null/knockout        ← canonical KO genotypes
15 541 Conditional ready|No functional change
 4 838 Null/knockout|Reporter
 4 154 Reporter
 3 044 Recombinase
   ...  Humanized sequence, Inserted expressed sequence, QTL, …
```

Each allele row carries its **gene marker + Ensembl/NCBI IDs**, e.g.
`Rag1<tm1Mom>` → gene `Rag1` → `ENSMUSG…`. That is the decomposition hook.

---

## Corpus evidence — the "complicated genotype" long tail is real (prod gemd, 2026-08-02)

Measured against the live production corpus. Pulled every distinct
genotype/strain-family value from biomaterial characteristics
(`WHERE BIO_MATERIAL_FK IS NOT NULL` and `LOWER(CATEGORY)` matching
`genotype|strain|background|variation`, raw =
`COALESCE(NULLIF(ORIGINAL_VALUE,''), VALUE)`) and classified each string.

**18,773 distinct values / 413,150 occurrences.** Single-bucket classification
(lower bounds — the residual bucket is mostly strains and gene-models the regex
missed, so A and C are undercounts):

| Bucket | % distinct | % occurrences | Meaning |
|---|--:|--:|---|
| Named strain | ~10% | **44%** | `C57BL/6(J/N)`, `BALB/c`, `Sprague-Dawley`, `FVB`, `129S6/SvEv`, crosses |
| WT / control / non-informative | ~7% | ~18% | trivial baseline arms |
| **Gene perturbation** | **~52%** | 24% | KO/KI/cKO/DKO, `fl/fl`+Cre, `Tg(...)`, `tm`-alleles, point muts |
| SNP genotype | 0.1% | 0.1% | negligible |
| residual / prose | ~31% | ~14% | more missed strains + model names + junk |

**Headline:** the complicated genotypes are the **majority of the variety
(~52%+ of distinct strings) but a minority of the volume (~24% of
occurrences)** — a long tail where free text currently wins by default. Common
backgrounds (44% of volume) are already covered by EFO + the 68-row preset
table; MGI's payoff is the tail, not the backgrounds.

Three things the real strings confirm:
- **Verbatim MGI nomenclature is already in the data** — nothing but MGI
  resolves these: `Gt(ROSA)26Sortm4(ACTB-tdTomato,-EGFP)Luo/J`,
  `Tg(SOD1*G93A)1Gur/J`, `B6;129S6-Gt(ROSA)26Sortm1(TARDBP*M337V/Ypet)Tlbt/J`.
- **The curation-rule traps occur in the wild** (Capability 2 must stay
  advisory): `Rbp4-Cre:RiboTag`, `Leprfl/fl::Dhh-Cre`,
  `Ribotag(Homo) X Dhh cre(Het)` — Cre-driver + floxed + RiboTag methodology.
- **`CATEGORY` is unreliable as a filter** — hundreds of spellings, GEO tags
  verbatim (`apoe genotype`, `8q24 genotype rs6983267`), typos
  (`gentoype/variation`), per-sample junk. The resolver should work off the
  **value text**, not the category string. The raw value also embeds the GEO key
  prefix (`strain: C57BL/6`) which must be stripped.

---

## Canonical URIs

MGI accessions resolve; no OBO purl exists (do not mint one):

| Candidate | HTTP | Use |
|---|---|---|
| `https://www.informatics.jax.org/strain/MGI:2160170` | **200** | ✅ emit for strains |
| `https://www.informatics.jax.org/accession/MGI:2160170` | 200 | generic accession resolver |
| `https://identifiers.org/MGI:2160170` | 200 | mirror only |
| `https://purl.obolibrary.org/obo/MGI_2160170` | **404** | ✗ no fake OBO purl |

**Local-part caveat:** MGI CURIEs use a **colon** (`MGI:2160170`), unlike the
OBO underscore local parts (`CLO_`, `GENO_`, `EFO_`) that
`annotation.category.prefixes` tokens match on. Whatever token we add to the
route (`MGI_` vs `MGI:`) must match the emitted URI's local part — decide the
scheme once and keep the emitted URI, the `allowedUriPrefix`, and the route
token consistent. (Emit the resolvable `https://www.informatics.jax.org/strain/MGI:<id>`;
`allowedUriPrefix = https://www.informatics.jax.org/`.)

---

## The gaps this closes (Gemma side, confirmed)

- **`strain` (EFO_0005135) is a first-class category but has no prefix route and
  no dedicated provider.** Today strain resolves via EFO + a hand-maintained
  68-row `valueStringToOntologyTermMappings.txt` preset table (+ a few TGEMO
  disease-model lines) + free text. The 84 k mutant strains have no home.
- **`genotype:GENO_,EFO_` routes GENO-first, but GENO is not loaded** — no bean,
  no `url.`/`load.` flag; `GENO_` appears only as the `has_genotype` predicate
  constant (`GENO_0000222`) and display links. So the GENO-first preference
  currently resolves nothing locally and falls through to EFO. MGI does not fix
  GENO (GENO is a *structural* vocabulary for how genotypes are composed); it
  supplies the actual genotype **instances** GENO was never going to list.
- **A gene-valued-genotype path already exists and is the reuse target.**
  `OntologyServiceImpl.searchForGenes` (called from `findTermsInexact`) already
  turns a genotype value into an NCBI **gene** characteristic "only for certain
  category URIs (genotype…)". It can match `Il10` but not an allele symbol like
  `Il10<tm1Cgn>`. MGI's allele→gene table is exactly what bridges that.

---

## Curation-rule constraints (read before touching genotype)

Authority: `gemma-curation-agents/docs/curation_rules/05_genotype_efc.md`. The
"complicated genotype" is *not* annotated as a single MGI record. It is a
Statement, and MGI must stay in its lane:

- **The genotype formula is fixed:** `gene (NCBI_GENE) + has_genotype
  (GENO_0000222) + manipulation-type`. The **gene** is grounded through Gemma's
  gene service to `NCBI_GENE` for the **actual taxon** (human ALL CAPS, mouse/rat
  Title Case; the promoter's organism is irrelevant). The **manipulation type**
  is **TGEMO** (Homozygous negative `TGEMO_00001`, Overexpression `TGEMO_00004`,
  Heterozygous `TGEMO_00002`, …) plus OBI/SO/PATO/EFO and free text for specific
  alleles (`K23L/K23L`, `Deletion of exon 3`). MGI provides *none* of these
  vocabularies — at most an MGI allele *type* could **hint** the TGEMO term
  (`Null/knockout` → `TGEMO_00001`), but the rule owns that mapping and the
  agent/curator decides.
- **Do NOT auto-decompose a strain into all its alleles → genotype FVs.** MGI's
  strain/allele records list the *driver* and *reporter* alleles alongside the
  perturbed gene. The rules are explicit that these are **not** the perturbation:
  - **Cre driver genes are cell-type markers, not the target.** In
    `Gfap-Cre Cx43-fl/fl`, the genotype is `Gja1 (=Cx43) + has_genotype +
    Homozygous negative` — Gfap is the astrocyte driver and must not be grounded
    as the perturbed gene. (MGI is useful here for the opposite reason: it maps
    the *floxed* symbol `Cx43` → marker `Gja1`.)
  - **RiboTag / TRAP / INTACT / Cre-reporter genes** (`Rpl22`, `Sun1`, `Ai9`,
    `tdTomato`) are methodology markers, not perturbations (the GSE110374 trap).
  So allele→gene decomposition is **advisory** — surface candidates for the
  agent/curator to pick the *floxed / recombination-substrate* target from; never
  auto-emit every allele as a genotype FV.
- **Strain ≠ genotype.** The background strain maps to the **strain** category
  (`EFO_0005135`); the perturbation maps to a **genotype** FV. MGI strain
  resolution (Capability 1) serves the strain slot and helps decode nomenclature;
  it does not by itself produce the genotype annotation.

## Recommended architecture

### Capability 1 — strain name resolution (lexical provider)

`providers/MgiStrainOntologyService` implementing `OntologyService`, backed by a
streamed parse of `MGI_Strain.rpt` (+ `MGI_Nonstandard_Strain.rpt`) into a Lucene
index: strain nomenclature (and synonyms, see gap below) → MGI strain URI.
`getParents`/`getChildren` empty. Slots into the existing
`OntologyServiceImpl.combineInThreads` fan-out as a **peer** (same reasoning as
Cellosaurus: no global `isEmpty` fallback — a weak EFO partial must not suppress
the exact MGI match).

Wiring:
- `@Bean` in `OntologyConfig.java` with `allowedUriPrefix = https://www.informatics.jax.org/`.
- `url.mgiStrain` + `load.mgiStrain=false` in `basecode.properties` (parser reads
  the `.rpt`, not RDF — so this may need a small loader branch or a self-contained
  provider that fetches its own source rather than going through the Jena
  `OntologyLoader`).
- **Add the missing `strain:` entry** to `annotation.category.prefixes` — e.g.
  `strain:<mgi-token>,EFO_` — and add the MGI token to `genotype:` as well.

**Emit policy (same question as Cellosaurus/CLO):** prefer EFO/TGEMO where the
strain already exists there (the ~3 k inbred backgrounds), emit an MGI URI only
for the tail EFO lacks. Keeps common backgrounds on the established vocabulary
and introduces MGI URIs only where they add coverage. — curation-policy call.

### Capability 2 — allele-symbol → gene disambiguation (feeds the rule, does not replace it)

This is the piece that helps with the "complicated genotypes," but it stays a
*resolver* behind the existing rule-based curation, never an annotation emitter.

Parse `MGI_PhenotypicAllele.rpt` into an allele-symbol → {marker gene symbol,
NCBI/Ensembl gene id, allele type} lookup. Given a sample label that carries an
allele string Gemma's gene resolver chokes on (the `<tm1Mom>` superscripts,
`-fl/fl`, `Tg(...)` transgene forms that `GEMMA_GENE_SEARCH_TODO.md` documents as
gene-search misses):

1. **Strip the allele decoration to the marker gene**, then hand that gene +
   taxon to Gemma's existing gene service so it grounds `NCBI_GENE` the normal
   way. (Example the rules call out: `Cx43-fl/fl` → marker `Gja1` → Gemma gene.)
2. **Surface, don't decide.** When a strain/label resolves to *several* alleles
   (target + Cre driver + reporter), present them as ranked candidates flagged by
   role (floxed/substrate vs `-Cre`/`-CreER` driver vs reporter) so the agent/
   curator applies the rule (pick the floxed target; ignore driver/reporter). Do
   not auto-populate FVs.
3. **Manipulation type still comes from TGEMO.** Optionally *hint* it from the
   MGI allele type (`Null/knockout` → `TGEMO_00001`), but the object of the
   Statement is a TGEMO/OBI/SO/PATO/free-text term per the rules, never an MGI
   string or URI.

Shape: a **resolver the curation agent calls** (a `/genotype/resolve`- or
`/gene/resolve-allele`-style helper, or an enrichment on gene-search), **not** a
peer in the ontology `findTerm` fan-out — its output is structured, multi-
candidate, and role-annotated, which `findTerm` cannot express. It complements,
and should reuse, the existing gene-valued-genotype path
(`OntologyServiceImpl.searchForGenes`, ~L1049-1076) rather than bypass it.

### Bonus enrichment (later)

MGI records carry detail no ontology term does, useful to the agent/curator:
- **Strain type** (inbred vs coisogenic vs congenic) — tells background vs mutant.
- **Background strain** of a mutant line (decompose "mutant on B6" → B6 + allele).
- **Allele → gene → disease model** via `MGI_Geno_DiseaseDO.rpt` (genotype → DO),
  which could also feed the `disease` category.
- **JAX stock numbers / strain synonyms** — *not* in `MGI_Strain.rpt`; see gap.

Capture what the parsed reports give now; expose via `getTerm(mgiUri)` detail as
a follow-on, same staging as the Cellosaurus enrichment.

---

## Data acquisition — reports vs MouseMine

- **Bulk `.rpt` download + index** (recommended baseline): self-contained,
  cacheable, offline, reuses the disk-cache + admin-refresh pattern. Needs a small
  multi-file join (strain + allele reports). Downside: `MGI_Strain.rpt` has **no
  synonyms and no JAX stock numbers**, so string matching is limited to the
  official nomenclature — many curator-pasted strings (JAX stock #, common
  abbreviations) won't hit.
- **MouseMine (InterMine) API** as an alternative/supplement. *What it is:*
  MGI's queryable data warehouse — the same MGI data as the `.rpt` files, but
  exposed as a **joinable database** (built on the InterMine framework, the same
  platform as FlyMine/HumanMine; run by Jackson Lab at `www.mousemine.org`). It
  integrates genes, alleles, genotypes, strains, and phenotypes into one store
  with **synonyms + cross-references**, and offers a real **web-services API**
  (REST + Python/Java/R clients). It can answer the joined queries the flat files
  can't — "strain → alleles → marker genes → roles/phenotypes" — and carries the
  strain **synonyms + JAX registry IDs** that `MGI_Strain.rpt` lacks. Cost: a
  **live external dependency** (network, rate limits, availability). Reasonable
  split: bulk reports for the offline strain index; MouseMine to enrich matching
  + drive Capability 2's allele→gene resolution.
  - ⚠️ **Availability-check TODO (do before committing to it):** the
    authoritative MouseMine paper is from 2015 (*Mammalian Genome*,
    [PMC4534495](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4534495/)) and the
    site is currently up ([www.mousemine.org](https://www.mousemine.org/mousemine/customQuery.do),
    [about](https://informatics.jax.org/mgihome/projects/aboutMouseMine.shtml)),
    but InterMine instances have had funding turbulence. Confirm its current
    maintenance state, API stability, and rate/SLA posture before taking a
    runtime dependency — and prefer degrading to the offline `.rpt` index if
    MouseMine is unavailable, rather than hard-failing curation.

**Open item:** find a synonym/JAX-stock source for strains — `MGI_Strain.rpt`
alone will under-match. Confirm whether a bulk MGI strain-synonym report exists
(preferred: keeps the source offline) or whether MouseMine is the only route.

---

## Integration points

| # | Change | File |
|---|---|---|
| 1 | New strain provider (`.rpt` parse + Lucene index) | `gemma-core/.../ontology/providers/MgiStrainOntologyService.java` (+ loader branch for tab-delimited source) |
| 2 | Register `@Bean`, `allowedUriPrefix = https://www.informatics.jax.org/` | `gemma-core/.../ontology/OntologyConfig.java` |
| 3 | `url.mgiStrain` + `load.mgiStrain=false` | `gemma-core/src/main/resources/basecode.properties` |
| 4 | Add `strain:` route + MGI token; add MGI token to `genotype:` | `gemma-core/src/main/resources/default.properties:362` `annotation.category.prefixes` |
| 5 | Allele→gene disambiguation resolver (advisory, role-flagged) | new resolver reusing `OntologyServiceImpl.searchForGenes` (~L1049-1076); must obey `curation_rules/05_genotype_efc.md` — gene→NCBI_GENE, type→TGEMO |
| 6 | (Optional) seed common strains as presets | `.../ontology/valueStringToOntologyTermMappings.txt` (existing 68 strain rows) — hand table can't scale to 118 k; presets only for high-frequency lines |

Note: the agent could not find a **server-side consumer** of
`annotation.category.prefixes` in gemma-core/gemma-rest — it may be read by
gemma-ui / the curation agent. Confirm where it's consumed before relying on a
new `strain:` route to take effect server-side.

---

## Open decisions

1. **Scope of first landing** — strain lexical resolution (Capability 1) only,
   with genotype decomposition (Capability 2) and enrichment as follow-ons? (Lean:
   yes; Capability 1 is the direct mirror of the Cellosaurus work.)
2. **Reports vs MouseMine** for matching + decomposition (synonym gap forces this).
3. **Emit MGI URI vs prefer EFO/TGEMO** for strains that exist in both (tail-only
   MGI emission) — curation-policy call.
4. **URI/token scheme** — settle the `MGI:` colon vs `MGI_` underscore local-part
   question once, across emitted URI + `allowedUriPrefix` + route token.
5. **Should Capability 2 live in the ontology fan-out or as a dedicated resolver?**
   (Lean: dedicated advisory resolver — structured, multi-candidate, role-flagged
   output doesn't fit `findTerm`.)
6. **How much does the agent already do this?** The curation agent's
   `gemma_gene_resolver.py` already preprocesses mutant suffixes / bracket forms
   (see `GEMMA_GENE_SEARCH_TODO.md`, G1-G7). Decide whether MGI allele→gene lives
   server-side (this doc) or augments that client-side resolver — avoid building
   it twice.

## Risks / watch-items

- **Rule violation is the headline risk.** Auto-emitting genotype FVs from MGI
  strain decomposition would reproduce the exact misannotations the curation
  rules warn against (Cre-driver-as-target, RiboTag/reporter-as-perturbation).
  Capability 2 must stay advisory + role-flagged and defer gene→NCBI_GENE /
  type→TGEMO to the existing curation path.
- **Under-matching** — nomenclature-only strings will miss curator-pasted JAX
  stock numbers / abbreviations without a synonym source.
- **Genotype strings are compositional** — `B6.129S7-Rag1<tm1Mom>/J` bundles
  background + allele in one string; robust matching needs to parse, not just
  exact-match. Manage scope: exact strain-record hits first, allele disambiguation
  second, and always as candidates for a human/agent to filter.
- **`.rpt` is not RDF** — the strain provider likely needs its own source-fetch
  path rather than the Jena `OntologyLoader`; keep it inside the provider so the
  fan-out sees a normal `OntologyService`.
- **Two big files, monthly cadence** — reuse `hasChanged` byte-compare + admin
  refresh; confirm the refresh tolerates a 31 MB allele report.
- **Load posture** — `load.mgiStrain=false` by default, on in prod; index-only
  footprint (no `OntModel`), so plausibly local-runnable — measure.
