# Correcting obsolete ontology terms in place

Status: **step 1 landed (read-only report), step 2 proposed and not built.**

## What exists now

`GET /admin/ontologies/obsolete-terms` reports every obsolete term Gemma's annotations still use, with the
replacement its ontology asserts. It is the in-application port of `FindObsoleteTermsCli`, and it is read-only.

The CLI existed because the check needs ontologies in memory and a CLI must load them itself — hence its refusal to
run unless `load.ontologies=false`, and the warm-up loop it spends its first stretch in. A running application
already holds them, so that cost disappears.

The scan cost also disappeared, and that mattered more than it looks. The CLI walked **every characteristic in the
corpus** in pages of 5,000 and asked each one's URI whether it was obsolete — while guarding the question with a
`checkedUris` set, so each distinct URI was actually only ever checked once. It paid for millions of row reads to
answer a question about tens of thousands of distinct URIs. The report groups CHARACTERISTIC by URI instead, which
is the same answer from one query.

Two things the CLI got wrong, not carried forward:

- **Its `Count` column was always 1.** `checkForObsolete` increments the count, but it is only reached inside the
  `!checkedUris.contains(uri)` guard, so no term could ever be counted twice. The endpoint reports
  `experimentCount` from `countExperimentsByUris` instead, which is a real number.
- **It called `ch.setId(null)` on managed entities** to "make occurrences non-unique". Mutating the identifier of a
  persistent instance is safe only because nothing flushes that session; moving it into a webapp would have made
  that a live hazard. The grouped query reads no entities at all.

## The correction, and why it is safe to derive

An obsolete OBO term usually carries `IAO:0100001 term replaced by`, naming its exact substitute. This is checkable
rather than assumed — EFO says of `EFO_0000408`:

```
label:            obsolete_disease
is_obsolete:      true
term replaced by: http://purl.obolibrary.org/obo/MONDO_0000001
```

That is the same `EFO_0000408 → MONDO_0000001` migration we have been carrying as a manual note. **The ontology
already published the answer**; we never had to decide it.

So the correction splits cleanly, and the split is the whole design:

| signal | meaning | may a machine act on it |
|---|---|---|
| `IAO:0100001 term replaced by` | the ontology asserts an exact substitute | **yes** — following it is reading, not guessing |
| `oboInOwl:consider` | suggestions for a curator, no claim of equivalence, often several | **no** — picking one is inventing curation |
| neither | obsoleted with no successor named | **no** |

`autoCorrectable` on each report row is true only when a replacement is asserted **and** itself resolves **and** is
not itself obsolete. Everything else carries a `blockedReason` naming what a human has to settle. On the numbers we
have, this is not a formality: the terms with the largest usage are exactly the ones EFO/MONDO retired cleanly.

## Step 2 — the proposal

`POST /admin/ontologies/obsolete-terms/apply`, **dry-run by default**, applying only `autoCorrectable` rows.

1. **Select.** Take the report; keep `autoCorrectable` rows. Accept an explicit `uris` list so a curator can apply
   one term rather than the world, and an `experimentIds` scope for a rehearsal on a handful of datasets.
2. **Rewrite the annotation.** For each affected characteristic, set `valueUri`/`objectUri`/`secondObjectUri` to the
   replacement and the matching label to the replacement's. All three slots, per the Statement shape — a pass that
   only rewrites `VALUE_URI` silently leaves two thirds of the surface obsolete.
3. **Record why, in `supportingEvidence`.** This is the slot V22 added for structured provenance, and it is the
   right home: `evidence` is a one-line string a UI renders verbatim, and this is a payload. Proposed shape:

   ```json
   {"obsoleteTermCorrection": {
      "from": "http://www.ebi.ac.uk/efo/EFO_0000408",
      "fromLabel": "disease",
      "to": "http://purl.obolibrary.org/obo/MONDO_0000001",
      "assertedBy": "IAO:0100001",
      "sourceOntology": "EFO",
      "ontologyVersion": "3.88.0",
      "appliedAt": "2026-08-19T…"}}
   ```

   The point of `assertedBy` is that a later reader can tell a derived correction from a curator's decision, and
   `ontologyVersion` says which release made the claim. Without those two fields this is indistinguishable from
   someone having retyped the annotation.
4. **Resync the denormalizations,** per affected experiment, reusing `TableMaintenanceUtil`:
   - `updateExpressionExperiment2CharacteristicEntries(ee, null)` — EE2C.
   - `updateAnnotationRelationEntries(ee)` — ANNOTATION_RELATION, which is derived from the curated statements EE2C
     carries, so it must run **after** EE2C and not concurrently with it.
   - Then evict: the EE2C refresh covers its own query space only; the relation rows live in another, and
     `addSynchronizedQuerySpace` is scoped to the JVM that did the write, which is not necessarily the one serving
     rows. `RestCacheEviction.evictAfterRebuild` is the existing answer and `UpdateEE2CCli` is the worked example.
5. **Reindex.** `term` and the characteristic fields are Hibernate Search mapped; a rewritten URI that never reaches
   Lucene means `/search` and `/annotations/term` disagree — the failure mode already recorded in
   `project_ontology_index_not_invalidated_on_source_change`.

### Settled (Paul, 2026-08-19)

- **Per-experiment EE2C upserts are fine.** EE2C is rebuilt nightly, so the non-winner rows an upsert cannot reach
  are corrected on the next cycle. No truncate-rebuild needed.
- **One audit event per experiment**, of a type 1.32.7 already loads. Not one per characteristic.
- **Runs as a submitted task**, not synchronously.

## The tail: obsolete terms with no replacement

Gemma should exhaust every *mechanical* rung before anything reaches a curator or an agent, because each rung is
free, deterministic, and leaves a provenance record a reader can check. The ladder, in order:

| # | rule | status | what it catches |
|---|---|---|---|
| 1 | `IAO:0100001` asserted directly | **built** | the ordinary retirement — EFO_0000408 → MONDO_0000001 |
| 2 | `IAO:0100001` followed through obsolete intermediates | **built** | a term retired twice; cycle-guarded, capped at 5 hops |
| 3 | `hasAlternativeId` reverse lookup | **built** | **merges**, where the ontology writes nothing on the dead term and records it on the survivor instead |
| 4 | cross-ontology xrefs, `substitutableOnly` | proposed | the DOID → MONDO case |

Rung 3 matters more than it sounds. An ontology that *merges* X into Y often leaves X with no `replaced by` at all
— the merge is recorded on Y, as `hasAlternativeId: X`. Reading only rung 1 declares those unresolvable when the
answer is sitting there, written from the other end. `OntologyService.findUsingAlternativeId` already existed.

Rung 4 is the one unbuilt mechanical rung. It costs more than the others: `OntologyXrefIndex` is built per run
rather than being a standing bean, and it has to be built from a **full** source ontology — a slim will silently
under-resolve. Worth building only if the measured tail justifies it, which is a question the report can now answer.

### What Gemma does with what survives — nothing

Once the mechanical rungs are exhausted, what is left is not a hard mechanical problem, it is a **judgment**: the
ontology retired a term and declined to say what replaces it. Gemma should not guess, and should not be taught to.

Its job ends at emitting a candidate set with provenance — the obsolete term, its usage count, the
`oboInOwl:consider` candidates the ontology offered, and which rung failed and why (`blockedReason`). That payload
is what an agent turns into a curation proposal, and the proposal goes through the normal review path like any
other. The division stays where it already is: Gemma asserts what is derivable, agents propose what is arguable.

### On embeddings

Permitted, but they are not the bottleneck and should not be the next thing built.

The reason is not cost, it is **provenance**. Every correction this system applies writes an `assertedBy` into
`supportingEvidence` so a later reader can tell a derived correction from a decided one. `IAO:0100001` and
`hasAlternativeId` are citable — someone can go and check the claim in the ontology. A cosine similarity of 0.91 is
not a claim about the world and cannot be audited; writing it into an evidence field would put a number where a
justification belongs.

There is also a practical point. Embeddings would *rank* candidates, but candidate lists here are tiny — an
`oboInOwl:consider` list is typically one to three terms. Ranking three items is not the hard part; deciding whether
any of them is actually the same thing is, and that is the judgment being deferred to the agent regardless. Build
rung 4 before building an embedding index.

## Retiring the CLI

`FindObsoleteTermsCli` is `@Deprecated` as of the read-side landing, with the pointer in its `getShortDesc()` so it
shows up in the command list rather than only in the source. **Delete it once the correction path lands** — the
endpoint already supersedes everything it does, and the two defects above mean there is nothing in it worth keeping.
Nothing else calls it; `PubMedSearcherIntegrationTest`-style coverage does not exist for it.

## Next step

Run `GET /admin/ontologies/obsolete-terms` on frink and read the actual distribution: how many obsolete terms are in
use, how many each rung resolves, and how heavy the tail really is. Every remaining decision — whether rung 4 is
worth building, how much the agent is being handed — is a question about numbers nobody has yet.
