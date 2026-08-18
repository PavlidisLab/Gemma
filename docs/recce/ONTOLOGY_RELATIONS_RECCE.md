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

*Update 2026-08-17 — S1 and S2 are built (`OntologyXrefIndex`,
`OntologyRelationProducer`, `updateOntologyRelations`), and the coverage this
document kept asking for is counted in §2b. Two of the guesses in §2 were wrong in
a way that matters: `is disease model for` is ten times the volume of the property
this recce led with, and cell line → species / organism part are not reachable
from the flat restrictions at all.*

## 1. What exists

**`OntologyTerm.getRestrictions()`** — declared on the interface
(`model/OntologyTerm.java:93`), implemented by `OntologyClassRestrictionImpl`,
which exposes `getRestrictionOn()` (the property) and `getRestrictedTo()` (the
target term). `RestrictionFactory`, `PropertyFactory`, `ObjectPropertyImpl`,
`RestrictionWithOnPropertyFilter` are all present and working.

🛑 **It was called from nowhere in `gemma-core` or `gemma-rest`.** The capability
was complete and dead. That was the single most useful fact in this recce: an
adoption problem, not a construction problem. As of 2026-08-17
`OntologyRelationProducer` calls it, and nothing else does — the interactive read
paths still do not, which is deliberate (see the cost note in §5).

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

**Counted, 2026-08-17 — see §2b.** The guesses in this section were close on
shape and wrong on proportion, and two of the properties listed above assert
nothing at all.

## 2b. Measured coverage

*Counted 2026-08-17 against the release artifacts themselves — `clo.owl`
(`owl:versionInfo` 2026-06-19), `mondo.obo` (`data-version releases/2026-08-04`)
and `chebi.obo` (`data-version 254`) — by parsing the files, not by loading them
into Gemma. That is the point of `SOURCE_VERSION` on every row: what a
deployment actually loaded can be a slim or a `-base` artifact, and these numbers
are the ceiling those are measured against, not a claim about any running
instance. The producer emits the same tally per run so a load can be compared to
this table.*

**CLO — flat `someValuesFrom` restrictions.** 40,851 `owl:Class` declarations;
all 9,583 restrictions on the properties below are declared on `CLO_` classes, so
filtering subjects to CLO's own namespace loses nothing.

| property | reads | classes | restrictions | nested |
|---|---|---:|---:|---:|
| `CLO:0000179` | is disease model for | **8,513** | **8,580** | 0 |
| `CLO:0000015` | derives from patient having disease | 868 | 893 | 0 |
| `RO:0001000` | derives from | 340 | 340 | **7,776** |
| `CLO:0037209` | derives from cell | 67 | 67 | 0 |
| `CLO:0037207` | derives from organism | 23 | 23 | 5 |
| `CLO:0037208` | derives from anatomic part | 18 | 18 | 0 |
| `CLO:0037210` | derived from cell line | 2 | 2 | 0 |
| `CLO:0037227`, `CLO:0037229`, `CLO:0037375`–`CLO:0037378` | — | **0** | **0** | 0 |

Four things in that table change the plan:

1. **`CLO:0000179 is disease model for` is the volume, by ten to one.** §2 above
   treats it as an interesting curiosity beside `CLO:0000015`; it is 8,513 classes
   against 868. It is also **not in `Relation.terms.txt`**, which is where every
   other predicate this feature emits already sits.
2. **The alternates listed in §2 assert nothing.** `CLO:0037227`,
   `CLO:0037229` and the four gene-modification properties
   (`CLO:0037375`–`CLO:0037378`) carry no flat restriction in this release. A
   plan that lists them as a fallback for the ones that do is planning around
   an empty set.
3. **Cell line → species and cell line → organism part are not available from
   the flat properties.** 23 and 18 classes respectively — and one of the 23
   `derives from organism` targets is `UBERON:0003101 male organism`, not a
   taxon at all. Both facts are really in the nested `RO:0001000` chain, which
   is 7,776 axioms against 340 flat ones. **S3 items 2 and 3 are therefore
   blocked on the nested unwind, not deliverable from `getRestrictions()`
   today.** That is the single most consequential correction here.
4. **Inheritance is not a volume problem.** `getRestrictions()` walks the
   transitive superclass closure, so a restriction declared on a parent is
   returned again for every descendant. Of the 8,609 classes carrying one of
   these restrictions only 50 have any descendant, adding 869 (class,
   restriction) pairs — about 9%, and semantically correct. Deduplicating on the
   triple is enough; no special handling is needed.

**DOID → MONDO — the join is essentially total.** The MONDO xref index inverts to
145,897 distinct foreign CURIEs over 33,295 MONDO terms.

| property | distinct DOID targets | resolve to ≥1 MONDO term | reach a *live* MONDO term | restrictions covered | ambiguous |
|---|---:|---:|---:|---:|---:|
| `CLO:0000015` | 128 | 128 (100%) | 128 (100%) | 893 / 893 (100%) | 0 |
| `CLO:0000179` | 436 | 436 (100%) | 433 (99%) | 8,568 / 8,577 (99.9%) | 0 |

The three misses are `DOID:0050444` (7 restrictions), `DOID:13809` and
`DOID:9080`, each of which resolves only to an *obsolete* MONDO term. **Zero DOID
targets map to more than one live MONDO term**, so the many-to-many the design
guards against is not exercised by CLO today — the code keeps every framing
anyway, because the guarantee is about the mapping and not about this release.

Qualifiers over the DOID/NCIT half of MONDO's index, as term–xref pairs:
`MONDO:equivalentTo` 16,288 · `MONDO:equivalentObsolete` 191 ·
`MONDO:obsoleteEquivalent` 175 · `MONDO:relatedTo` 33 ·
`MONDO:obsoleteEquivalentObsolete` 12. 🛑 **A single xref can carry several
`source=` values** — `{source="MONDO:obsoleteEquivalent", source="EFO:0002616"}`
— where only one names a mapping predicate and the rest are provenance. Reading
the first and stopping files a real equivalence as unqualified; this cost one
wrong pass during the build and has a test on it.

**CHEBI — `RO:0000087 has role`.** 218,709 terms; 31,606 carry at least one
direct `has_role`, over 59,102 direct assertions. Inheriting ancestors' roles the
way `getRestrictions()` does takes that to 194,244 (term, role) pairs, which is
the worst case against full CHEBI. What a deployment actually loads is the
corpus-seeded slim, so the real figure is far smaller — and it is exactly the
difference `SOURCE_VERSION` exists to make visible.

🛑 **A CHEBI role is not an indication.** Imatinib carries `antiviral agent`,
`antihypertensive agent` and `hepatoprotective agent`; acetylsalicylic acid
carries `antidepressant` and `anti-asthmatic agent`. These are reported
activities from the literature. The object of a `has role` row is filed under
CHEBI's own role root (`CHEBI:50906`), never under `disease`, and no lexical or
heuristic route from a role to a disease is acceptable — drug → indication is a
different source (MED-RT / DrugCentral) and a different piece of work.

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
* ~~**Coverage is unmeasured.**~~ Measured 2026-08-17 — see §2b. Every distinct
  DOID CLO references resolves to a MONDO term, and 99.9% of CLO's disease
  restrictions reach a live one. One verified pair turned out to be
  representative, which it had no right to be.

## 4. Staged plan

**S1 — invert the xrefs. DONE 2026-08-17.** `OntologyXref` +
`OntologyService.getCrossReferences()` (bulk, off the Jena model, qualifier kept)
and `OntologyXrefIndex`, which inverts it. Substitutes only across `exactMatch` /
unqualified mappings; narrow and broad are readable but not substitutable, which
is the whole reason the qualifier is carried. Coverage in §2b. **Still open:** the
lookup on `/annotations/term`, so REST callers stop label-matching.

**S2 — read restrictions and count coverage. DONE 2026-08-17.**
`OntologyRelationProducer` calls `getRestrictions()`, filters to the allow-list in
`OntologyRelationSource`, resolves foreign targets through S1 and writes
`ANNOTATION_RELATION` rows with `BASIS='ONTOLOGY'`, `EVIDENCE_CODE='IEA'`, no
experiment and no ACL mask. Run it with `updateOntologyRelations`; it is a command
of its own because it needs CLO, CHEBI and MONDO warmed up, which the EE2C-driven
`updateEe2c --relations` has no business waiting on. Per-property coverage is
logged per run as a tab-separated block. **Still open:** surfacing the relations on
`/annotations/term`, kept out of `parents` for the reason in §1.

**S3 — use it where curation is being asked for something derivable.** In priority
order, because each maps to a curation cost we are already paying. §2b changes
what is actually reachable:
1. **cell line → disease.** ✅ Available now: 9,473 disease restrictions across
   the two properties, 9,461 of which reach a live MONDO term. 26 of 33 cell-line
   experiments in the 500-experiment gold carry a disease tag that restates the
   line's own disease.
2. **cell line → species.** 🛑 **Not reachable from the flat properties** — 23
   classes. The species really lives in the nested `RO:0001000` chain. The check
   CAB asked for on 2026-08-11 needs the nested unwind first, or Cellosaurus's
   `species-list`, which is a disjoint source and a smaller job.
3. **cell line → anatomical part.** 🛑 Same: 18 classes. Nested, or Cellosaurus's
   `derived-from-site-list`.
4. **cell line → knocked-out gene.** 🛑 Zero: `CLO:0037375`–`CLO:0037378` carry no
   flat restriction at all.

**S3a — chemical → role.** ✅ Shipped alongside S2, and not in the original plan.
CHEBI's `RO:0000087 has role` makes `imatinib` findable as an antineoplastic agent
and a tyrosine kinase inhibitor. It is already in `Relation.terms.txt` and already
in CHEBI's `additionalPropertyUris`, which means the roles come back as a term's
*parents* today — reading them as relations is what lets a caller tell a role from
chemistry. 🛑 It is not an indication: see the warning at the end of §2b.

**S4 — let assertion outrank attestation.** Where `CLO:0000179 is disease model
for` is stated, it is a claim by an ontology, not a count over our corpus. The
disease-model endpoint should prefer it and say which it used. That is a change to
`/annotations/diseaseModels` and should wait until PR #1685 has landed.

## 5. Risks

* **Hierarchy pollution** — covered in §1. The failure mode is silent and would
  corrupt disease browsing, so any change here needs a test asserting that a cell
  line never appears among a disease term's children.
* **Cost is why the read runs offline.** `getRestrictions()` walks the transitive
  superclass closure twice and throws an exception per non-restriction superclass
  on the second pass, which is most of them. Rather than measure it and hope, the
  producer runs it in a maintenance job and stores the answer, so no interactive
  path pays it at all — the point of a derived table. Wiring it to the experiment
  page or the browse selector directly would still need the measurement, and a
  cache in `OntologyCache` alongside the existing parent/child caches.
  🛑 It also **throws outright** on a restriction shape it cannot convert
  (`RestrictionFactory` refuses a property that is neither datatype nor object),
  so a caller must guard per term or one bad class ends the pass over 40,000 good
  ones.
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

## 5a. Open: the predicate vocabulary is short of what CLO asserts

`Relation.terms.txt` is what Gemma sanctions as a predicate, and it already
carries `CLO:0000015 derives from patient having disease`, `CLO:0037209`,
`CLO:0037210`, `RO:0001000` and `RO:0000087 has role`. Three of the properties the
producer reads are **missing** from it:

| property | reads | restrictions it would sanction |
|---|---|---:|
| `CLO:0000179` | is disease model for | 8,580 |
| `CLO:0037207` | derives from organism | 23 |
| `CLO:0037208` | derives from anatomic part | 18 |

The producer writes those rows and **logs a WARN naming every unsanctioned
predicate** rather than dropping them: `ANNOTATION_RELATION` is a derived index,
not a curation surface — the vocabulary constrains what a curator may write into a
`Statement`, which `OntologyTermValidatorImpl` enforces separately — and dropping
them would discard 90% of what CLO asserts because a text file was not updated.
But the file should be extended deliberately, not by a maintenance job, so this
needs a decision. `CLO:0000179` is the one that matters; note that
`RO:0003301 is model of` is already in the file and is a *different* URI, so
adopting it instead would mean rewriting CLO's own predicate, which is worse.

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
