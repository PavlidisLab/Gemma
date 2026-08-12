# Reading the relations our ontologies already assert — reconnaissance

*2026-08-12. Prompted by Paul: "in general, we should think about making better
use of the ontologies" and "the general issue: not curating information we can
infer." Companion to `docs/design/GENOTYPE_DISEASE_MODEL_EXPANSION.md`, which
infers the same class of fact from the corpus rather than from an ontology.*

## Summary

Gemma loads whole ontologies and reads a thin slice of them: labels, synonyms,
`subClassOf`, `part_of`, and (as of PR #1686) the description. Everything an
ontology states with an object property — what a cell line's disease is, what
tissue it came from, what gene was knocked out in it, what it is a model of — is
in the model we have already paid to load and parse, and nothing reads it.

The reading machinery is **already built and never called**. What is missing is
smaller than it looks, and one piece of it unblocks the rest: an index that turns
the foreign identifiers other ontologies point at (DOID, NCIt, Orphanet) into the
MONDO terms Gemma actually annotates with. MONDO's cross-references are already
being served; they have simply never been inverted.

Everything below was checked against the live ontologies and against frink on
2026-08-12. Where something is sampled rather than counted, it says so.

## 1. What exists

**`OntologyTerm.getRestrictions()`** — declared on the interface
(`model/OntologyTerm.java:93`), implemented by `OntologyClassRestrictionImpl`,
which exposes `getRestrictionOn()` (the property) and `getRestrictedTo()` (the
target term). `RestrictionFactory`, `PropertyFactory`, `ObjectPropertyImpl`,
`RestrictionWithOnPropertyFilter` are all present and working.

🛑 **It is called from nowhere in `gemma-core` or `gemma-rest`.** The capability
is complete and dead. That is the single most useful fact in this recce: this is
an adoption problem, not a construction problem.

**The `additionalPropertyUris` mechanism** — `AbstractOntologyService` computes
`additionalRestrictions` from a configurable set of property URIs and threads it
through `getParents` / `getChildren`. The default set is `RO.partOf` and its
sub-properties (`AbstractOntologyService:49`); `ChebiOntologyService:105` adds
`RO.hasRole` to it, so per-ontology widening is established practice.

🛑 **Do not reach for it here.** It works by folding a restriction's target into
the term's PARENTS. That is right for `part_of` and defensible for `has_role`,
because both are hierarchy-shaped. Applied to `derives from patient having
disease` it would make `MCF7 cell` a subclass of adenocarcinoma — so a disease
query expanded to its sub-terms would return cell lines as if they were diseases,
and the disease-model inference would start counting cell lines as diseases. The
mechanism available is a hierarchy flattener; what these relations need is to be
read AS relations. Use `getRestrictions()`.

## 2. What the ontologies actually assert

CLO defines these object properties (listed from CLO's property set):

| property | reads |
|---|---|
| `CLO:0000015` | derives from patient having disease |
| `CLO:0000179` | **is disease model for** |
| `CLO:0037207` / `CLO:0037229` | derives from organism / cell line cell derived from organism |
| `CLO:0037208` / `CLO:0037227` | derives from anatomic part / … derived from anatomical part |
| `CLO:0037375`–`CLO:0037378` | derives from cell with knockout / isogenic / knockin / transgenic modification of gene |

`CLO:0000179` is worth pausing on: the predicate the genotype/disease-model
handoff said "would be the one to discuss" if the relation were ever materialized
already exists, in an ontology we already load.

**Coverage is uneven, and this is the main limit.** Sampled, not counted:

* `CLO_0007606` (MCF7) carries a real restriction — `{property: CLO_0000015,
  value: DOID_…}` — a structured link to a disease term.
* `CLO_0008127` (NCI-H929) carries no such restriction. Its disease exists only as
  the `rdfs:comment` string `disease: plasmacytoma;   myeloma`, which is what
  PR #1686 makes readable.

So the string path and the relation path cover different lines and both are
needed. Cellosaurus is a third, disjoint source — `CVCL_0511` (Raji) has
`disease-list` → NCIt, `species-list` → NCBITaxon, `derived-from-site-list` →
UBERON, all as URIs — and our OBO parser reads id/name/synonym/xref/subset/comment
and discards the rest. CAB measured CLO answering 2 lines Cellosaurus had no
disease for, and Cellosaurus answering 24 CLO did not reach.

**Someone should count this before we build on it.** The measurement is a pass
over the loaded CLO model tallying classes with each property, and it is the first
thing to do in stage 2 below.

## 3. The blocker, and why it is already solved

CLO points at **DOID**. Cellosaurus points at **NCIt**. Gemma annotates in
**MONDO**. And Gemma does not load DOID — `/annotations/term?uri=…DOID_3459`
returns 404 on frink today.

This is exactly the wall CAB hit: they compare disease strings by normalized
label, which cost them two tokenisation bugs in one day (`B-cell` destroyed by
treating `cell` as a stopword; `lymphoma.` ≠ `lymphoma`).

It does not need a new ontology. **MONDO already carries the cross-references, and
Gemma already serves them.** From frink today:

```
MONDO_0004975 (Alzheimer disease) dbXrefs:
  DOID:10652, NCIT:C2866, Orphanet:238616, UMLS:C0002395, MESH:D000544, HP:0002511, …
```

Invert that and `DOID:10652` → `MONDO_0004975` is an exact join. One index over a
model already in memory turns every foreign disease identifier in CLO, Cellosaurus
and MGI into the vocabulary Gemma searches in. It also retires the label-matching
in the offline genotype/disease-model builder, whose 17% MGI agreement figure is
reported as a floor *precisely because* no DOID↔MONDO map was available.

Caveats worth designing for: xref CURIEs vary in prefix case (`DOID:` vs `NCIT:`
vs `NCIt:`) so normalization is needed; xrefs are many-to-one in both directions
and MONDO marks some as narrow/broad rather than exact, so an inverted index must
either keep the qualifier or accept that a small fraction of joins are
approximate. Reporting which it was beats silently picking one.

## 4. Staged plan

**S1 — invert the xrefs.** A reverse index from foreign CURIE to MONDO term, built
once per ontology load and invalidated with it. Small, self-contained, independently
useful, and every later stage depends on it. Expose it as a service method plus a
lookup on `/annotations/term` so callers stop label-matching.

**S2 — read restrictions and count coverage.** Call `getRestrictions()`, filter to
a configured property allow-list, resolve foreign targets through S1, and surface
the result on `/annotations/term` as relations — property URI, property label,
target term — kept out of `parents` for the reason in §1. Tally per-property
coverage across CLO while doing it, and put the numbers in this document.

**S3 — use it where curation is being asked for something derivable.** In priority
order, because each maps to a curation cost we are already paying:
1. **cell line → disease.** 26 of 33 cell-line experiments in the 500-experiment
   gold carry a disease tag that restates the line's own disease.
2. **cell line → species.** The check CAB asked for on 2026-08-11; it kills the
   confident-wrong groundings where a mouse line was matched into a human study.
3. **cell line → anatomical part**, which is an `organism part` annotation nobody
   should be typing.
4. **cell line → knocked-out gene**, which is a `genotype` annotation, and which
   also feeds the disease-model inference.

**S4 — let assertion outrank attestation.** Where `CLO:0000179 is disease model
for` is stated, it is a claim by an ontology, not a count over our corpus. The
disease-model endpoint should prefer it and say which it used. That is a change to
`/annotations/diseaseModels` and should wait until PR #1685 has landed.

## 5. Risks

* **Hierarchy pollution** — covered in §1. The failure mode is silent and would
  corrupt disease browsing, so any change here needs a test asserting that a cell
  line never appears among a disease term's children.
* **Cost is unmeasured.** `getRestrictions()` walks a term's superclasses;
  per-term cost is unknown and both target call paths (experiment page, browse)
  are interactive. Measure before wiring, and cache in `OntologyCache` alongside
  the existing parent/child caches rather than beside them.
* **Index staleness.** Swapping an ontology source updates the Jena model but
  reuses the Lucene index, so `/annotations/term` and `/annotations/search`
  disagree until `refresh?forceIndexing=true`. A derived xref index will inherit
  this and must be invalidated on the same signal.
* **`-base` and slim artifacts drop axioms.** Several ontologies load from
  upstream `-base` files and CHEBI/MONDO have slim paths. Relations can be present
  in the full artifact and absent in what we actually load — CLO is currently
  loaded whole (`url.cellLineOntology=…/clo.owl`), but this is a standing trap for
  anything that starts depending on axioms rather than labels.
* **Inference stays inference.** None of this is written as an annotation. A
  relation read from an ontology is better evidence than a co-occurrence count,
  and it is still not a curator's claim.

## 6. Not recommended

* Adding CLO's disease property to `additionalPropertyUris` (§1).
* Loading DOID to resolve CLO's targets — S1 answers it from MONDO, which is
  already loaded, and adds no download, no memory and no refresh schedule.
* Parsing the `disease: a;   b` comment string into structured terms. It is CLO's
  own convention, irregularly spaced, and unreliable as a key; serve it as prose
  (PR #1686) and get structure from restrictions and Cellosaurus instead.
