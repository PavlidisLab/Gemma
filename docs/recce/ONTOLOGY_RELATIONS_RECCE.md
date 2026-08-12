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

**Two shapes, and only one of them is easy.** `CLO_0000015` and `CLO_0000179` are
flat `someValuesFrom` restrictions — a property and a target, which is exactly
what `getRestrictions()` returns. But `RO_0001000 derives from` on the same MCF7
class is a nested intersection that reads, unwound: an epithelial cell
(`CL_0000066`) `part_of` breast (`UBERON_0000310`) `part_of` a human
(`NCBITaxon_9606`) who has `DOID_3008`. Cell type, organism part, species and the
donor's disease are all in that one axiom — four of the annotations we want, in
the structure that is hardest to read. `getRestrictions()` surfaces the top level
of it and no more. Take the flat relations first and treat the nested chain as its
own piece of work; do not scope them together.

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
**MONDO**, which is the vocabulary these are being consolidated into — MONDO
supersedes DOID for our purposes, and Gemma does not load DOID at all
(`/annotations/term?uri=…DOID_3459` returns 404 on frink today).

**So DOID is an identifier space to translate OUT of on read, never one to adopt,
resolve against, or store.** A DOID that reaches a Gemma annotation, a stored
inference or an API response is a bug. This is what makes the index below the
first piece of work rather than an optimization: without it, the only thing CLO's
disease assertions can be compared against is a label, which is exactly the wall
CAB hit — normalized-label matching cost them two tokenisation bugs in one day
(`B-cell` destroyed by treating `cell` as a stopword; `lymphoma.` ≠ `lymphoma`).

It needs no new ontology. **MONDO carries the cross-references and Gemma already
serves them.** Verified end to end on 2026-08-12, both sides from data already
loaded here:

| source | assertion |
|---|---|
| CLO (OLS) | `CLO_0007606` MCF7 cell — `CLO_0000015 derives from patient having disease` → `DOID_3458`; `CLO_0000179 is disease model for` → `DOID_299`, `DOID_3458` |
| Gemma's MONDO (frink) | `MONDO_0004988` breast adenocarcinoma — `dbXrefs` contains `DOID:3458` |
| Gemma's MONDO (frink) | `MONDO_0004970` adenocarcinoma — `dbXrefs` contains `DOID:299` |

Invert the xrefs and `DOID:3458` → `MONDO_0004988` is an exact join, in process,
against a model already in memory. The same index carries `NCIT:` for Cellosaurus
and retires the label-matching in the offline genotype/disease-model builder, whose
17% MGI agreement figure is reported as a floor *precisely because* no DOID↔MONDO
map was available.

Caveats worth designing for:

* **The REST `dbXrefs` list is flat, and MONDO's xrefs are not.** MONDO qualifies
  many cross-references (exact / narrow / broad, plus a source) as axiom
  annotations, and the string list served today drops that. Build the index from
  the Jena model where the qualifier is still there, and keep it: a narrow xref
  resolved as though it were exact is a wrong disease reported with full
  confidence. Serving the qualifier through the API is a second, smaller change.
* Xref CURIEs vary in prefix case (`DOID:` / `NCIT:` / `NCIt:`), so normalize.
* The mapping is many-to-many in both directions.
* **Coverage is unmeasured.** One verified pair is not a rate. Counting how many
  distinct DOIDs referenced by CLO resolve to a MONDO term belongs in stage 1.

## 4. Staged plan

**S1 — invert the xrefs.** A reverse index from foreign CURIE to MONDO term, built
from the Jena model once per ontology load and invalidated with it, keeping the
exact/narrow/broad qualifier. Small, self-contained, independently useful, and
every later stage depends on it. Expose it as a service method plus a lookup on
`/annotations/term` so callers stop label-matching. Report DOID and NCIt coverage
into this document while building it.

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
* **Loading DOID to resolve CLO's targets.** MONDO is what DOID is being
  consolidated into and what Gemma annotates in; adding DOID would mean carrying a
  superseded vocabulary, its download, its memory and its refresh schedule, in
  order to answer a question S1 answers from a model already in memory. Translate
  out of DOID on read; never store one.
* Parsing the `disease: a;   b` comment string into structured terms. It is CLO's
  own convention, irregularly spaced, and unreliable as a key; serve it as prose
  (PR #1686) and get structure from restrictions and Cellosaurus instead.
