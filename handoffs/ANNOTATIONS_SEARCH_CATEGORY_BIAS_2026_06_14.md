# Handoff — `/rest/v2/annotations/search` should accept an ontology-preference signal scoped by category

**From:** Bro 1 (agents) + Paul
**To:** gemma-rest
**Filed:** 2026-06-14

## TL;DR

When the agent grounds a `cell line` factor value (e.g. searches
`nci-h358`), the search endpoint ranks EFO above CLO even though the
canonical Gemma representation of cell lines is CLO. CLO is in the
index — it just doesn't win the rank because the agent's bare query
form is an exact label match against EFO. The Python agent has no
clean way to express "for cell-line queries, prefer CLO when both
ontologies match"; the right place for that bias is in `/annotations/
search` itself, scoped by category.

Paul 2026-06-14: "gemma-rest should do some of the lifting here."

## Concrete shape

GSE81642 / GSE83875 / every NSCLC cell-line study surfaces this on
the agent's grounded URIs. Probe:

```bash
curl -s -u "$U:$P" \
  "$GEMMA_BASE_URL/rest/v2/annotations/search?query=nci-h358&limit=5" | jq
```

returns:

| Rank | termName       | termUri                                      |
|------|----------------|----------------------------------------------|
| 1    | nci-h358       | `http://www.ebi.ac.uk/efo/EFO_0002291`       |
| 2    | NCI-H358 cell  | `http://purl.obolibrary.org/obo/CLO_0008085` |

The agent stamped EFO; the live curation has CLO; the eval surfaces
the URI mismatch as a finding the curator has to triage even though
the underlying biology is the same line. With `?query=NCI-H358 cell`
the order flips and CLO wins — i.e. the index has both URIs cleanly
indexed; only the rank is wrong for the most natural agent query.

The same pattern affects:

* `A549` (EFO:0001086) vs `A549 cell` (CLO:0001601)
* `H460` (EFO:0003044) vs `H460 cell` (CLO:0003601)
* presumably most NCI / ATCC line variants

## Ask

Pick one:

### A. Category-scoped ontology preference (preferred)

Add a server-side preference: when the query is intended for a
`cell line` category, rank CLO above EFO; when it's intended for a
disease category, rank MONDO above DOID; when it's a developmental
stage, UBERON above EFO. The categories Gemma cares about are a
small fixed set — embedding the table server-side beats every
client carrying its own.

API options for "tell the server the target category":

1. New optional query param `?categoryUri=http://purl.obolibrary.org/obo/CLO_0000031`
   — explicit, no inference. Caller passes whatever it knows about
   the category it's grounding for.
2. New optional `?categoryHint=cell_line` enum — softer, server
   maps the enum to its preference table. Less brittle to URI
   changes.

(1) is more flexible (callers can ground unknown categories without
the server having to know about them); (2) is more constrained but
easier to keep in sync with Gemma's canonical category set.

### B. Server-side rerank without a hint

Re-rank within the result set: when CLO and EFO both match a
cell-line-shaped query (heuristic: the query string starts with a
known cell-line accession prefix, or the EFO term carries a CLO
xref), promote CLO. Doesn't need a new param but is harder to make
right — cell-line accession patterns are messy.

(A.1) is the cleanest. The Python agent already knows which category
it's grounding for; passing the category URI as a hint is one line on
the call side.

## Adjacent

Symmetric for disease (MONDO over DOID), developmental stage (UBERON
over EFO), organism part (UBERON over EFO), strain (EFO over NCBI
taxonomy when both match). Same one-line param on the call site
gives all of these the right ranking. Embedding the preference
table server-side avoids per-client drift.

## Out of scope

- Agent-side workaround (append `" cell"` when the category is `cell
  line`) — possible but every client would have to reimplement the
  category→suffix rules, and the suffixes aren't uniform across
  ontologies (CLO uses `" cell"`, MONDO doesn't use any suffix on
  the disease name, etc.). Server-side is the only place to centralise.
- Re-ranking the existing live curations to use CLO where they
  currently use EFO — separate housekeeping job, not blocking this.
