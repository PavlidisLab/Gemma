# Supplementary Methods — ontology handling in Gemma

Draft for the supplementary methods of the Gemma 2.0 manuscript. Describes the system as
implemented at commit `cdf49196b4` (2026-08-18). The engineering manifest behind it, with
per-claim source citations, is [`ONTOLOGY_SUBSYSTEM.md`](ONTOLOGY_SUBSYSTEM.md); anything
stated here should be checkable there.

---

## Controlled vocabularies

Gemma annotates experiments, samples and experimental factors with terms drawn from
public biomedical ontologies. Nineteen vocabularies are loaded in the production
deployment: the Experimental Factor Ontology (EFO), Mondo Disease Ontology, Cell Ontology
(CL), Cell Line Ontology (CLO), Uberon, ChEBI, the Human and Mammalian Phenotype
ontologies (HP, MP), Gene Ontology, the Ontology for Biomedical Investigations (OBI), PATO,
the Sequence Ontology, EMAPA, GENO, the Neuro Behaviour Ontology, and Gemma's own
TGEMO, which supplies terms the public ontologies do not carry. Where an upstream project
publishes a `-base` release — EFO, CL, MP, HP and Uberon do — Gemma loads that rather than
the fully merged artefact, so that terms are imported once from their owning ontology
rather than several times through import closures.

Three sources are handled differently because they are not ontologies. Cellosaurus
(cell lines), the MGI mouse-strain report, and a table of MeSH disease synonyms are large,
flat, actively maintained catalogues of names and synonyms with no subsumption hierarchy.
Rather than force them through the OWL machinery, Gemma indexes each as a flat lexical
vocabulary. Their hits are returned but ranked below every conventional ontology hit: each
source scores against its own index, so the scores are not on a common scale, and an
exact-name match in a 150,000-entry catalogue would otherwise displace ontology terms
wholesale. The intent is gap-filling — when the ontologies return nothing, the catalogue
hit is still first.

Sources are fetched over HTTP, cached on disk, and revalidated with a conditional request
on reload. Two vocabularies (ChEBI, Mondo) additionally support a corpus-seeded slim: a
module extracted around the terms Gemma actually annotates, which reduces ChEBI's load time
from over an hour to a few minutes. Slims carry a sidecar recording the seed policy, a hash
of the seed URIs and a schema version, and are treated as stale when any of those or a
seven-day age limit is exceeded.

## Indexing and text normalization

Each vocabulary is indexed into an in-memory Lucene index over its terms' labels, synonyms,
cross-references and identifiers. The analysis recipe is an English analyzer with Porter
stemming and Lucene's default stop-word list, and the same analyzer instance is given both
to the index writer and to the query parser. That symmetry is a correctness requirement
rather than a convenience: every normalization described below applies to the stored text
and to the query alike, so no spelling that matched before a change stops matching after it.

Ahead of tokenization, two character filters fold designation-shaped strings that differ
only in an internal separator. Submitters write a compound's trial code as `SU11248` while
ChEBI stores `SU-11248`; the standard tokenizer splits on the separator, so the stored form
indexes as two tokens and the written form as one, and the two never meet. The first filter
folds a short alphabetic prefix followed by a run of at least three digits, or by
hyphen-separated digit groups. The second folds one- and two-digit designations of the kind
CLO uses for cell lines — `MEC-1`, `IL-6`, `EMT-6`.

The bounds on the second filter are constrained by an ambiguity that is worth stating,
because it determines what such a system can safely do. A one-digit run is also the shape
IUPAC uses for a locant: `MEC-1 cell` and `2-(1H-indol-3-yl)ethanamine` differ only in
where the run sits. ChEBI supplies 713,295 of the 1,530,566 labels and synonyms across the
loaded vocabularies, so a rule that merely lowered the digit floor would rewrite 27% of
ChEBI, welding away tokens — `amino`, `yl`, `deoxy` — on which tens of thousands of other
compounds are matched. Restricting the relaxation to hyphens rather than spaces does not
help, since the hyphen is itself the locant separator. The bound Gemma applies is therefore
positional rather than numeric: a locant is always welded into a longer chain, whereas a
catalogue designation stands alone, so folding is applied only to runs delimited by
whitespace or a string boundary on both sides, and only where the alphabetic prefix is at
most three characters. Measured across the full corpus of labels and synonyms, the two
filters together alter 8,156 strings (0.53%) — 728 in CLO, all of them cell lines, and
1,201 in ChEBI, all of them trial codes — while leaving ordinary prose such as
`type 2 diabetes mellitus`, `grade 3` and `group 1 innate lymphoid cell` unchanged.

Ontology indexes are held in memory and rebuilt from the model on every load, so an
analysis change takes effect when the process restarts and no stale index can persist
across one.

## Query resolution

A term query is dispatched on its form: a URI or CURIE is resolved directly against the
corpus and then the loaded vocabularies, and free text is searched across all of them.
Free-text search fans out across the vocabularies in parallel under a single request-wide
deadline, in parallel with a prefix match against terms already used in the corpus, so that
a term a curator has applied before is available alongside terms that merely exist. Each
index is queried with a minimum-should-match constraint requiring roughly two-thirds of the
query's terms to be present; without it, two unrelated queries sharing one common word
return the same documents, and `Gorlin Goltz Syndrome` retrieves `down syndrome` at rank
one on the strength of `syndrome` alone.

Results are merged by score, capped, and then ordered by a relevance tier computed from how
the query matched: an exact label first, then a prefix match, then full coverage of the
query's content tokens, then a word-boundary substring, then a bare substring, and last a
match on a URI or synonym only. Whether a hit matched a preferred label, an exact synonym or
a related synonym is computed explicitly and returned to the client, since a resolver needs
to distinguish "this term is named by your query" from "your query appears somewhere in this
term's record". Callers may then request one of several ranking strategies over that
ordering — by Lucene score (the default), by corpus usage, by query-token coverage, by a
composite of the three, or by a corpus-derived prior. Ranking strategies reorder and never
filter; all filtering is done by explicit switches (category, namespace prefix, exact-label,
near-match suppression), each of which is reported back to the caller when it removes a row.

The top-ranked hits are enriched with definitions, immediate parents, corpus usage counts,
the per-category breakdown of how prior curators have used the term, and — for the flat
catalogues — source-declared attributes such as the species a cell line derives from, which
is what allows a mouse line to be refused on a human study at grounding time.

## Validation of submitted terms

Terms arriving through the curation API are validated before they are stored. For each
slot of a submitted annotation — category, value, and for statements the predicate and
object — Gemma resolves the URI's canonical label, consulting its own controlled
vocabularies first, then its loaded ontologies, then the EBI Ontology Lookup Service for
URIs from vocabularies it does not load. A submitted label that is blank is filled in; one
that differs only in case or whitespace is accepted and normalized to the canonical form;
one that genuinely disagrees with the term's label is rejected, as is a URI that resolves
nowhere. Violations across the whole request are accumulated and returned together, each
identified by its path in the submitted document, so that a client sees every problem at
once rather than one per round trip. The same validation runs on a dry-run endpoint, so a
client can check a proposed change without applying it.

This gate is deliberately confined to the composite curation endpoint. Bulk annotation
replacement, the sample-characteristic endpoints, experimental-design updates and data
import do not run it, which is a limitation rather than a design goal.

## Derived relations

Alongside annotations, Gemma maintains a store of derived relations: subject-predicate-object
triples stating, for example, which disease a cell line's donor had, or which anatomical
part it came from. These are explicitly not annotations — no experiment carries them — and
exist so that facts inferable from what is already recorded need not be curated onto each
experiment separately.

Each triple is stored once per basis, recording whether it was curated by Gemma's curators,
read from a loaded ontology's axioms, supplied by an external resource such as Cellosaurus
or MGI, or computed from co-occurrence in the corpus. Corroboration between bases is
computed at read time rather than stored. Relations are readable from either end but
inferable in only one direction, and which direction is a property of the predicate: a
cell line being a disease model licenses a claim about the disease, while the converse does
not hold. Predicates whose direction is not classified license nothing, and relations whose
object is untyped license nothing, so the default is closed.

Rows carry the asserting source and, where available, the source's own wording as evidence
alongside the resolved term — the raw record often carries information the ontology term
does not, such as whether a sample was primary or metastatic.

## Limitations

Retrieval and ranking are distinct stages, and normalization applied at the ranking stage
cannot recover a term the index did not retrieve; several recall problems that presented as
ranking problems were of this kind. The corpus-usage signals attached to search results
report curation history, including its mistakes, and a term used many times under a given
category is evidence with a denominator rather than a verdict. The denormalized annotation
table that supports anonymous access is refreshed on a schedule and by an upsert that cannot
correct rows its query no longer produces. Finally, one widely used category term
(`EFO_0000408`, "disease") has been obsoleted upstream while several thousand Gemma
annotations still reference it; migrating them is outstanding.
