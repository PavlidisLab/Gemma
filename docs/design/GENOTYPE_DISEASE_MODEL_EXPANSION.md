# Design — inferring the disease a genotype models

> **STATUS, 2026-08-18 — the MECHANISM below is superseded; the METHOD is not.**
>
> This shipped as PR #1685 (`GET /annotations/diseaseModels`), which derived the
> relation live from `CharacteristicDao` on every request. `ANNOTATION_RELATION`
> replaced that shape: the derivation belongs in the maintenance job and the read
> becomes an indexed lookup over a purpose-built table — see
> `AnnotationRelationDaoImpl`, whose javadoc names the candidate cut, the EE2C
> self-join and the string-keyed second query as things that "existed only to fit a
> corpus-wide derivation inside a request".
>
> **So the PR is closed and this document is retained as the specification for two
> bases that are designed and NOT YET BUILT:**
>
> - `AnnotationRelationBasis.CORPUS` — the derivation described here
> - `AnnotationRelationBasis.EXTERNAL` — the MGI cross-check described at the end
>
> Read "the API" below as "the producer": the ranking, the specificity rule, the
> taxon rule and the value-level key all carry over unchanged. What does not carry
> over is the request-time query and the `/annotations/diseaseModels` endpoint.
>
> Two rules were added by oganm after the original draft and are recorded in
> **Corrections** at the end — both are properties of the derivation, so the CORPUS
> producer inherits them.

*Numbers in the offline table regenerate from
`scripts/build_genotype_disease_model_table.py`; do not hand-edit them. The live
derivation described below is the one the API serves. Companion handoff:
`~/Dev/Gemma/handoffs/GENOTYPE_DISEASE_MODEL_SEARCH_EXPANSION_2026_08_12.md`.
Manuscript prose lives with the paper.*

## Rationale

Curation policy treats a model-organism experiment's mutant/wild-type contrast
as a **genotype** factor rather than a `disease model` one: the mutation is what
varies across samples, and a factor is the annotation for a property that
varies. `disease model` remains valid as a whole-experiment tag when every
sample is a model.

That policy is only defensible if the model-of relation stays **derivable**. A
user ticking "autism" in the disease selector must still find an experiment
annotated only as `Chd8` mutant, even though no disease annotation is present on
it. Rather than materialize an `is model of` predicate onto every experiment —
which would duplicate, per experiment, a fact that is a property of the genotype
— Gemma derives the relation on demand from annotations the corpus already
carries.

## Construction

The relation is recovered by a self-join over Gemma's own curation on the
denormalised `EXPRESSION_EXPERIMENT2CHARACTERISTIC` (EE2C) view. Any experiment
carrying **both** a `disease`/`disease model` annotation and a value in the
requested categories contributes a candidate; the number of distinct experiments
supporting it is the support.

Both annotation levels are read — an experiment tag, a factor value and a sample
characteristic are the same annotation entity differing only in what the property
holds of — and the split is reported as evidence rather than collapsed.

Wild-type values are excluded: a control arm models nothing. Recognition is
`BaselineSelection`'s, the same one that picks a DEA baseline, so there is one
list and not two.

Ask it either way round. The disease side is identified by CATEGORY, never by
which side was seeded, so a row means the same thing whether the question was
"what models Alzheimer disease?" (the browse selector) or "what does this
genotype model?" (the experiment page).

### 🛑 Support is not evidence — specificity is

A first cut ranked pairs by support alone. On real data that ranking is
confidently wrong, and Paul's audit of production tags shows why. These tags are
recoverable from the genotype, and could be dropped:

| dataset | tag | what carries it |
|---|---|---|
| GSE245831 | Alzheimer disease | strain `APP/PS1` |
| GSE271616.1 | hepatosplenic T-cell lymphoma | `STAT5B N642H` transgene |
| GSE298402 | prostate cancer | `NPp53` |
| GSE303043.1 | small cell lung carcinoma | `Tp53/Rb1 DKO` |
| GSE79061 | retinal degeneration | `Abca4` null |

These must be kept, and every one of them co-occurs exactly like the ones above:

| dataset | tag | what co-occurs | why the tag stays |
|---|---|---|---|
| GSE102415 | obesity | strain `C57BL/6J` | diet-induced; the strain says nothing |
| GSE154383 | ischemic stroke | `GFAP-ARO-KO` | surgically induced; the genotype is a modifier |
| GSE241529 | noise-induced hearing loss | `Fabp7` KO | the noise causes it |
| GSE306137 | lesion of sciatic nerve | strain `Sprague Dawley` | surgical |
| GSE31486 | Burkitt lymphoma | `CTCF` knockdown | Burkitt is the cell line, not the knockdown |
| GSE99114 | experimental autoimmune encephalomyelitis | `FTY720` | EAE is immunization-induced; the drug is what is being tested |

Co-occurrence cannot separate these two tables. What separates them is the
**fraction of the value's experiments the disease accounts for**:

    specificity = experiments attesting (value, disease) / experiments carrying value

`Abca4` null is annotated retinal degeneration nearly every time it appears.
`C57BL/6J` appears against obesity a handful of times out of the many hundreds of
experiments that use the strain, and against hundreds of other diseases besides.
Results are therefore ranked by `support × specificity`, and every row reports
`numberOfExperimentsWithValue` and `numberOfDiseasesAttested` alongside so a
client can set its own bar. The same measure demotes the drug-vs-model confusion
without a special case: `FTY720` is tested against everything, so its specificity
against EAE is low.

No threshold is applied by default (`minSpecificity=0`). None has been tuned
against curator judgement, and the shape of the distribution is worth seeing in
the UI before one is fixed in the API. The tables above are the evaluation set
when someone does tune it.

### Taxon decides what the inference says

A mouse carrying the `Mecp2` null is a **model of** Rett syndrome. A human line
carrying `LRRK2 G2019S` is not modelling Parkinson disease — it **has** it. The
taxon is therefore part of the row grain, and each row reports whether the
annotation it implies would be `disease model` or `disease`. Taxon-unknown
experiments fall to `disease model`, the weaker claim.

### Keyed on the value, not the gene

**Pairs are keyed on the full annotation value, not on the gene.** Of 1,622 genes
in the offline table, **434 have different alleles or perturbation directions
mapping to different diseases** — `Myc` overexpression accompanies breast cancer
while `Myc` knockdown accompanies pancreatic ductal adenocarcinoma; `Apoe` null
and the human `E2/E3`, `E3/E3`, `E3/E4` variants are separately attested against
Alzheimer disease; `Htt` appears as `Q111/Q111` and as a 180-CAG repeat
expansion. A gene-keyed table would collapse these.

The key also admits values that name no gene at all — `APP/PS1`, `5xFAD`,
`trisomy 21`, `Tp53/Rb1 DKO` — which account for 115 pairs and would otherwise
be unrepresentable. Many of these have no URI either, which is why both the value
and the value URI can seed a query and why the generated `filter` carries a
free-text leg.

## What it is not

The relation is **many-to-many by nature and is not reduced to a function**.
`Trp53` homozygous-null is attested against lung adenocarcinoma (8 experiments),
medulloblastoma (4) and breast cancer (4) — which is biologically correct, as the
same null models several malignancies. The result is a ranked candidate list,
appropriate for query expansion and for showing a curator, and inappropriate for
automatic annotation.

Nothing writes annotations. If the relation is ever materialized that is a
separate decision, and `is model of` would be the predicate to discuss, not
`has role disease model`, which says a thing is a model without saying of what.

## API

`GET /rest/v2/annotations/diseaseModels`

| parameter | meaning |
|---|---|
| `uri` | disease term; ask "what models this?" |
| `value`, `valueUri` | model side; ask "what does this model?" |
| `category` | model-side categories, default `genotype,strain`; `category=` for any. `disease` and `disease model` are never accepted — they identify the other side of the relation |
| `inferSubTerms` | fold the disease term's sub-classes in (default true) |
| `minSupport`, `minSpecificity` | thresholds, both off by default; applied before `limit`, so raising one does not thin the page |
| `excludeDatasets` | hold datasets out of the evidence |
| `limit` | default 50 |

Every row carries `numberOfExperiments`, `numberOfExperimentsWithValue`,
`numberOfDiseasesAttested`, `specificity`, the evidence split by annotation
level, an example dataset, and a `filter` string that returns the datasets the
inference was read from. That filter is a slight over-approximation, not an
identity: `GET /datasets` expands annotation URIs to their sub-terms, so a
dataset annotated with a sub-class of the disease matches it too.
`AnnotationsWebServiceDiseaseModelsRestTest` round-trips the string through the
dataset query, which is the only place the two halves of the contract meet. The response also carries one `filter` that
widens the original disease question to everything inferred — hand it straight to
`GET /datasets?filter=`.

Two uses drive the shape:

* **Experiment page.** Seed the model side with the dataset's own genotype values
  to caption "inferred: model of Alzheimer disease (11 datasets, specificity
  0.85)". Pass the dataset in `excludeDatasets` so a dataset that already carries
  the tag is not shown its own tag as an inference.
* **Browse.** The disease checkboxes call it once per ticked disease and OR the
  returned `filter` into the dataset query. The two result classes stay
  distinguishable because the client knows which terms it added and why — the API
  deliberately does not merge them behind the client's back.

`excludeDatasets` is also what makes "this `disease model` tag is inferable, so it
can be dropped" an honest claim: hold the dataset out and ask whether the rest of
the corpus still recovers the disease.

## Performance

Both paths are interactive, so the derivation has to stay well under the 100 ms
line. It is two indexed queries — the pair self-join (both sides indexed on
`VALUE_URI` and on the primary key's `EXPRESSION_EXPERIMENT_FK`) and the
specificity denominator (indexed on `VALUE`) — with Hibernate query caching on
both, invalidated on the EE2C query space so curator edits show through.

🛑 **Not yet measured against production.** `scripts/perf_search.py --only
disease-models` probes both directions plus the two worst cases (a broad
sub-term fan-out, and a background strain's denominator scan). Run it on frink
before the browse checkboxes depend on it. If the derivation cannot be made fast
enough live, the fallback is materializing it on the EE2C maintenance job's
schedule — which needs a migration, and therefore an explicit ask.

## Offline table

`scripts/build_genotype_disease_model_table.py` produces the same derivation from
a fixed production dump, for the manuscript and for offline analysis:
**4,731 pairs spanning 1,622 genes and 938 diseases**, from the 2,509 experiments
of the 2026-05-16 snapshot that carry both annotation types. It predates the
specificity measure and ranks on support alone; the API is the current statement
of the method. Artifacts carry `sha256`, source snapshot and build time.

### External cross-check

MGI's Disease Ontology report (`MGI_DO.rpt`, 19,832 rows) provides an independent
gene-level reference. Joined on gene symbol, 3,515 pairs are checkable and 597
agree (17%).

That figure is a **floor**, for two reasons that both understate agreement.
First, MGI annotates to Disease Ontology while Gemma annotates to MONDO, and no
DOID↔MONDO mapping was available locally, so the comparison is made on normalized
disease labels; MONDO's DOID cross-references would be the rigorous path. Second,
MGI is gene-level and unranked, so a disagreement frequently reflects the resource
listing other diseases for the same gene rather than contradicting ours — `Sod1`
returns Down syndrome and Parkinson disease rather than amyotrophic lateral
sclerosis, and `Trp53` returns CHARGE syndrome and Li-Fraumeni syndrome ahead of
any of the malignancies our corpus attests.

MGI is consequently used as corroboration and coverage estimation, never as an
input to a curation decision. Where the two agree, confidence rises; where MGI
holds a gene our corpus lacks, a coverage gap is exposed; and where our corpus
holds allele-specific knowledge MGI cannot express, the difference is the
justification for the value-level key.


## Corrections (oganm, 2026-08-17) — inherited by the CORPUS producer

Two defects found while the endpoint was live. Both are properties of the
derivation rather than of the endpoint, so they hold for the offline producer too.

**1. A second disease on a study does not model the first.** The self-join told
its two sides apart by category and nothing else: `D` was an annotation under
`disease` or `disease model`, `S` was *anything else* on the same experiment.
"Anything else" included another disease, so a study annotated with two comorbid
diseases reported each as a model of the other — and a caller passing no category,
which is documented as the way to ask for any kind of model, got exactly that. The
model side must be the **complement** of the disease side, not the unconstrained
set, or what a row means depends on how narrowly the caller happened to ask.

Write it as two null-safe legs rather than one negation: an uncategorised
annotation has a null `CATEGORY`, `null not in (...)` is null, and a plain `NOT`
therefore drops the row.

**2. The cut runs before the cap, never after.** `minSpecificity` was applied to
the rows that came back, after the query had already been capped at the caller's
limit — "the top N, minus the ones below the bar" when what was asked for is "the
best N that clear the bar". For a disease whose top rows are all background
strains the two answers differ by almost everything, and raising the threshold
thinned the page instead of filling it with better rows.

🛑 The identical defect was live in `/annotations/relations/implies` and was found
by reading this one: the breadth cut ran after a SQL `LIMIT` that had no
`ORDER BY`. Fixed 2026-08-18 in `54b4940729`. **Any filter that selects among
candidates must be applied before the row cap, in every producer and every read.**

## External source provenance — MGI_DO.rpt

The artifact itself is not in the repository (this repo tracks code, not data).
Its record, from `MGI_DO.meta.json` on the closed branch:

| field | value |
|---|---|
| source | `https://www.informatics.jax.org/downloads/reports/MGI_DO.rpt` |
| fetched | 2026-08-12T17:06:59Z |
| rows | 19,832 |
| sha256 | `36ab46c14511b6f543a35456a7b32d8ea16bdd755cfce1ca65ec15076083b992` |
| columns | DO Disease ID · DO Disease Name · OMIM IDs · Common Organism Name · NCBI Taxon ID · Symbol · EntrezGene ID · Mouse MGI ID |

Limits, verbatim: gene-level, unranked, many-to-many; cannot express allele
direction (`Myc` overexpression vs knockdown) or strain-shaped genotypes
(`APP/PS1`, `5xFAD`).

✅ **The rigorous path this document asks for now exists.** The 17% agreement
figure above is a floor partly because the comparison was made on normalized
disease labels, "because DO and MONDO share no identifier available locally —
MONDO's DOID xrefs would be the rigorous path". Those xrefs are now inverted and
in memory: `OntologyXrefIndex`, built from MONDO's SOURCE artifact rather than the
loaded slim, holds 145,917 cross-references of which 12,091 are DOID. A rebuilt
cross-check should join on identifiers and report a real agreement rate rather
than a floor.
