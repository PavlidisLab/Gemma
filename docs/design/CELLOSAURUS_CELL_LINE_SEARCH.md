# Cellosaurus as a cell-line name-resolution source (backup for CLO)

**Status:** design / plan — not yet implemented.
**Date:** 2026-08-02.
**Goal:** stop the curation agent (and curators) from *missing* cell lines that
the Cell Line Ontology (CLO) does not cover, by adding Cellosaurus as a second
cell-line source behind the existing `/annotations/search` surface.

---

## TL;DR

Cellosaurus is **not an ontology** in any structural sense — it is a flat,
actively-maintained catalogue of ~169 000 cell lines with dense synonyms and
cross-references but **no `is_a` hierarchy**. So we do **not** load it as a Jena
`OntModel` the way CLO/EFO/UBERON are loaded. We add a lightweight
**lexical name-resolution provider** that conforms to the existing
`ubic.gemma.core.ontology.providers.OntologyService` interface, is backed by a
Lucene index built from a streamed parse of `cellosaurus.obo`, and participates
in the existing search fan-out as a peer. It emits Cellosaurus's own canonical
URIs (`https://www.cellosaurus.org/CVCL_<id>`).

The point is **CLO's coverage gaps**, so backlinking Cellosaurus hits onto CLO
URIs is deliberately *not* the primary mechanism — the cell lines we care about
are exactly the ones CLO lacks, which by definition carry no CLO xref.

---

## Source facts (cellosaurus.obo, v56.0, June 2026)

Fetched from `https://ftp.expasy.org/databases/cellosaurus/cellosaurus.obo`.

| Property | Value |
|---|---|
| Format | OBO `format-version: 1.2` |
| Size | ~113 MB |
| `[Term]` stanzas | ~168 970 |
| `is_a:` lines | **0** |
| Relationships | `derived_from` (69 528, transitive) · `originate_from_same_individual_as` (38 718, symmetric) |
| ID prefix | `CVCL_` |
| Licence | CC BY 4.0 |

Per-term fields we can read: `id`, `name`, `synonym` (with scope, e.g.
`"HeLa-S3" RELATED []`), `subset` (category + sex), `xref`, `comment`,
`relationship`, `creation_date`.

The most frequent `xref` prefixes (matters for both matching and enrichment):

```
NCBI_TaxID (species) · Wikidata · PubMed · NCIt (disease) · Coriell ·
ORDO (rare disease) · GEO · CLO (34 125) · Cosmic · cancercelllines ·
MMRRC · BioSamples · hPSCreg · ECACC · ATCC · RCB · JCRB · DepMap …
```

Two parsing quirks to respect (a battle-tested parser handles both; a hand
streamer must too):
- `name:` is written with **no space** after the colon (`name:#132 PC3-1-SC-E8`).
- synonyms carry a trailing scope + modifier list (`"..." RELATED []`).

---

## Corpus evidence — does Cellosaurus actually cover our data? (prod gemd, 2026-08-02)

Measured against the live production corpus, not assumed. Pulled every distinct
`cell line`-category value from biomaterial characteristics
(`WHERE BIO_MATERIAL_FK IS NOT NULL AND LOWER(CATEGORY) LIKE '%cell line%'`,
raw = `COALESCE(NULLIF(ORIGINAL_VALUE,''), VALUE)`) and matched each against a
normalized name+synonym index built from `cellosaurus.obo` v56 (261,568 keys).

- **10,603 distinct values / 118,232 sample occurrences.** Volume is
  concentrated on the usual common lines (`MCF7`, `HEK293T`, `HeLa`, `HepG2`,
  `LNCaP`, `A549`, `U2OS`…); a long tail carries the variety.
- **Cellosaurus coverage:** exact-normalized match = **45.4% of distinct /
  61.2% of occurrences**; adding a specific-token match tier = **60.1% of
  distinct / 70.1% of occurrences.** Both are *lower bounds* (exact/­token only,
  no fuzzy).
- **The misses are mostly not our target.** The residual after both tiers is
  dominated by generic cell-type/tissue terms miscategorized as `cell line`
  (`PBMCs`, `osteoblast`, `Liver`, `iPSC`, `mixed`, `primary …-macrophages`),
  lab-internal derivative IDs no ontology will hold (`MSN01`–`MSN09`, `H1-NPC`,
  `WM989-A6-G3-Cas9 5a3`), and **ATCC catalogue refs** (`ATCC HTB-22`,
  `ATCC (Cat No CRL-1740)`) — which Cellosaurus alone could recover via its ATCC
  **xref** (HTB-22 = MCF7, CRL-1740 = LNCaP), a match CLO cannot make.
- Descriptive wrappers around a real line (`breast cancer cell line MCF7`,
  `BEAS-2B human airway epithelial cell line`) are the main *recoverable* miss —
  a prefix/label-strip step in the resolver would lift them.

**Honest caveat:** this measures Cellosaurus coverage of the corpus, not the
incremental gain over CLO — the CLO-gap premise is assumed, not measured here
(would need CLO loaded to diff). What it does establish: even naive matching puts
Cellosaurus at ~70% of cell-line occurrences, and the ATCC-xref angle is a
concrete capability CLO lacks.

---

## Why not "load it as an ontology"

The Gemma ontology loader is **Jena RDF/XML only**:
`jena/OntologyLoader` reads every model with the two-arg
`model.read(is, base)` (defaults to RDF/XML) and hardcodes
`Accept: application/rdf+xml`. There is no OBO branch, and
`owlapi-oboformat` is **deliberately excluded** in `gemma-core/pom.xml`
(comment: "CHEBI is OWL/RDF, not OBO, so we don't need it").

So consuming the OBO requires new parse logic regardless of approach. Given
that, loading it as a full ontology is the wrong trade:

- **The hierarchy it would model does not exist.** With zero `is_a`, a Jena
  `OntModel` buys no parent/child/inference value. The search fan-out never
  walks Cellosaurus hierarchy because there is none.
- **It would cost RAM we don't have.** A RDF/XML rendering of 169 k terms with
  synonyms/xrefs is a ~300 MB+ artifact and a heavyweight `OntModel` +
  inference model in heap. CLO already cannot run on a local dev box for RAM
  reasons; a full Cellosaurus `OntModel` is worse.
- **It needs an OWL artifact we don't want.** Cellosaurus publishes OBO, not
  OWL. Converting (e.g. `robot convert`) and hosting a `cellosaurus.owl` adds a
  recurring pipeline step for a rendering nobody upstream maintains.

The search path that cell-line queries actually use is **lexical**:
`OntologyServiceImpl.combineInThreads` fans out one task per loaded provider and
calls `ontologyCache.findTerm(service, query, max)` — a Lucene text lookup,
name/synonym → URI. That is exactly, and only, what Cellosaurus needs to do.

---

## Recommended architecture

### A lexical `OntologyService` provider

Add `providers/CellosaurusOntologyService` that **satisfies the
`OntologyService` interface** so the existing fan-out consumes it with no
changes to `OntologyServiceImpl`, but is backed by a lexical index rather than a
Jena model:

- **Parse** `cellosaurus.obo` as a stream, one `[Term]` stanza at a time,
  keeping only the fields we need. OBO stanzas are line-delimited, so this needs
  no Jena and never holds 169 k class objects in heap.
  - *Adopt-alternative:* re-add `owlapi-oboformat` purely as the parser (it
    handles the `name:`/synonym-modifier quirks for free), walk its classes to
    build the index, then discard the OWL model. Preferred only if we would
    rather not own the (small) OBO grammar. For four fields, a streamer avoids
    pulling the dependency back and keeps peak memory flat.
- **Index** into Lucene: `name` + all `synonym`s → `CVCL_` URI, mirroring what
  `OntologyCache.findTerm` expects. Reuse the existing disk-cache directory
  convention (`${ontology.cache.dir}/ontology/...`).
- **Interface conformance:**
  - `isOntologyLoaded()` → true once the index is built.
  - `findTerm(query, …)` → Lucene lookup.
  - `getTerm(uri)` → resolve a `CVCL_` URI back to its term (label + metadata).
  - `getParents(...)` / `getChildren(...)` → **empty**. Honest: there is no
    subsumption. (`derived_from` lineage is available if we ever want it, but it
    is not `is_a` and must not be surfaced as such.)

### Wire as a peer, not a fallback

Add it to the autowired `List<OntologyService> ontologyServices` that
`combineInThreads` fans out over — **as an ordinary peer**, always consulted.

Do **not** copy the GeneOntology `if (results.isEmpty()) …` fallback pattern.
That guard is *global*: it suppresses the fallback whenever *any* ontology
returned a hit. For a cell line CLO lacks, a weak partial match from EFO would
suppress Cellosaurus exactly when it is needed. And `findTerm` returns only
matches to the query — not all 169 k terms — so there is no "flooding" risk that
a fallback would be protecting against. Peer is both simpler and correct for
gap-filling: Cellosaurus contributes the matches CLO missed, ranked by the
existing score / `getCharacteristicComparator`.

### Dedup via the CLO xref (the one real use of the backlink)

When Cellosaurus *does* carry a `CLO:` xref and that CLO term is already present
in the same result set, collapse to the CLO term so a single cell line does not
surface under two URIs. This is display/dedup only — never term emission.

### URIs

Cellosaurus has a stable, resolvable canonical URI; we use it verbatim:

| Candidate | HTTP | Use |
|---|---|---|
| `https://www.cellosaurus.org/CVCL_0030` | **200** | ✅ emit this |
| `https://identifiers.org/cellosaurus:CVCL_0030` | 200 | mirror only |
| `https://purl.obolibrary.org/obo/CVCL_0030` | **404** | ✗ do not mint a fake OBO purl |

- Emitted `valueUri`: `https://www.cellosaurus.org/CVCL_<id>`.
- `allowedUriPrefix` on the bean: `https://www.cellosaurus.org/CVCL_`.
- Category routing token `CVCL_` matches on the URI local part.

---

## Cellosaurus carries metadata CLO does not — a second win

Beyond closing name gaps, Cellosaurus records per-cell-line detail that CLO has
no slot for. This is genuinely useful to the agent/curator when confirming or
describing a cell line, and it comes "for free" once the term is parsed:

- **Provenance / source** — originating repository accessions (ATCC, ECACC,
  Coriell, RCB, JCRB, DepMap…), i.e. where the line came from / can be obtained;
  `derived_from` parent-line lineage.
- **Species** — `NCBI_TaxID` xref (human vs mouse vs rat vs …), which directly
  disambiguates same-named lines across organisms.
- **Sex** — from the `Female`/`Male`/… subset.
- **Category** — `Cancer cell line`, `Hybridoma`, `Induced pluripotent stem
  cell`, `Finite cell line`, etc. from `subset`.
- **Disease** — `NCIt` / `ORDO` xrefs (~81 k NCIt xrefs across the file).
- **Derived-from site / cell type** — `UBERON=` / `CL=` tokens in `comment`.
- **Problem flags** — Cellosaurus is the reference source for
  *misidentified / contaminated* cell lines. Surfacing that flag would let the
  agent warn a curator that a proposed line is a known contaminant — a
  curation-quality signal CLO cannot provide.

**Plan:** capture these fields on the parsed term now (cheap), but treat
*exposing* them as a **follow-on** past the search-gap fix. Natural surface:
have `getTerm(cvclUri)` return the enrichment, and let the annotation
`/term`-style read-back carry it, so the agent can fetch "what is CVCL_XXXX"
detail on demand. Scope the first landing to name resolution; note the
enrichment endpoint as the next step rather than building it up front.

---

## Integration points (confirmed against the tree)

| # | Change | File |
|---|---|---|
| 1 | New lexical provider | `gemma-core/.../ontology/providers/CellosaurusOntologyService.java` (+ streamed OBO parser + Lucene index) |
| 2 | Register `@Bean` with prefix `https://www.cellosaurus.org/CVCL_` | `gemma-core/.../ontology/OntologyConfig.java` (mirror the `cellLineOntologyService` bean, ~L196-199) |
| 3 | `url.cellosaurus=…/cellosaurus.obo` + `load.cellosaurus=false` | `gemma-core/src/main/resources/basecode.properties` |
| 4 | Add `CVCL_` to the `cellLine` (and `cellType`) prefix lists | `gemma-core/src/main/resources/default.properties` L362 `annotation.category.prefixes` |
| 5 | (No change) fan-out consumes it automatically | `gemma-core/.../ontology/OntologyServiceImpl.java` `combineInThreads` |
| 6 | Dedup-on-CLO-xref in result assembly | `OntologyServiceImpl` result merge / `AnnotationsWebService.getTerms` |

Refresh/ops reuse what already exists: disk cache + byte-compare `hasChanged`,
and the unified `POST /admin/ontologies/{name}/refresh` async re-init with
atomic state swap. No new admin surface.

---

## Loading posture (RAM)

`load.cellosaurus=false` by default, enabled on frink — same posture as CLO
(`load.cellLineOntology`, on in prod, off locally for RAM). Because this is
**index-only, not an `OntModel`**, its footprint is a Lucene index of ~169 k
short docs, which is plausibly light enough to run on a local dev box even
though CLO is not. Worth measuring rather than promising.

---

## Open decisions

1. **First-landing scope** — name resolution only, with the metadata-enrichment
   read-back (`getTerm` detail / problem flags) as an explicit follow-on. (Lean:
   yes — ship the gap-fill, note the enrichment.)
2. **Parser choice** — hand streamer (no new dep, flat memory) vs. re-adding
   `owlapi-oboformat` (adopt a tested OBO grammar). Lean: streamer.
3. **Emit CVCL for lines that *do* have a CLO xref?** Or prefer CLO whenever a
   xref exists and only emit CVCL for the genuine gap tail? (Affects how much
   new `CVCL_` vocabulary enters annotations.) — curation-policy call for Paul.

## Risks / watch-items

- **Version drift** — Cellosaurus releases roughly monthly; the `hasChanged`
  byte-compare + admin refresh handles re-index, but confirm the refresh path
  tolerates a 113 MB source without wedging.
- **Parser quirks** — `name:` no-space and synonym scope modifiers must be
  handled (see Source facts).
- **Duplicate-URI display** — mitigated by the CLO-xref dedup; verify it
  actually collapses in `/annotations/search` output.
- **Ranking** — a Cellosaurus exact match must not out-rank a CLO exact match
  for the same term; validate against `getCharacteristicComparator` behaviour.
