# Term-URI canonicalization — why Gemma returns a term it did not store

**Status 2026-08-18: read-time shim LIVE in code, database migration WRITTEN AND PARKED.**

Gemma resolves 46 annotation URIs to a different URI when it reads them. This document says
which, why, how they were chosen, and what has to happen for the shim to go away.

## 1. Why a shim and not the migration

The migration exists — `scripts/sql/term_uri_migration.sql`, reviewed, with preflight and
postflight counts. It is deliberately not run. Two independent reasons:

1. **The agent pipeline is calibrated against a May snapshot of Gemma.** Rewriting live rows
   now desynchronizes the corpus from what the agents were measured on. Paul, 2026-08-18:
   *"I don't want to get too out of sync. So we won't actually do the migration now."*
2. **The Gemma 2.0 write path is held.** 2.0 emits 21 audit discriminators Gemma 1.32.7
   cannot load (PR #1667, open); anything that audits degrades the 1.0 experiment page.

Paul's instruction was therefore: *"Gemma has to return the right ones — just hard-code it
for now. List it as a migration to be done."* That is what this is.

## 2. One file, two consumers

`gemma-core/src/main/resources/ubic/gemma/core/ontology/TermUriMigration.tsv` is the single
source. `CharacteristicUtils#canonicalUri` loads it at runtime; the SQL is generated from it.

🛑 **They must not be maintained separately.** A shim and a migration that disagree about
what the corpus contains is the failure this arrangement exists to prevent — the same reason
`Relation.terms.txt` is one file rather than a list in prose and a list in code.

## 3. What is remapped

| lane | URIs | annotations | basis |
|---|---:|---:|---|
| `malformed` | 29 | 266 | the URI is wrong on its face |
| `clo_twin` | 17 | 84 | two live CLO classes, one cell line |
| **total** | **46** | **350** | |

**Malformed** — bare CURIEs (`CL:0000236`), OBO IRIs punctuated with a colon
(`obo/CL:0000115`), an id concatenated with itself (`CL_0000669000669`) or truncated
(`CL_000062`). Every repair was verified by resolving the repaired IRI against the live
ontology **and** matching its label against the stored label. Repairs that failed either
check were left alone and listed for a person.

**CLO twins** — see §4.

🛑 **Not remapped: categories and predicates.** Gemma 1.0 reads categories from the same
database while it is live and they cannot move much; the predicate vocabulary is separately
constrained by `Relation.terms.txt`. Paul: *"put categories and predicates aside for now —
just regular ontology entities."*

## 4. How a CLO twin is decided

Strict precedence. The first two rules exist because usage is the wrong instrument on its own.

```
R1  an obsolete term loses to its declared successor, always
R2  a catalogue class loses to a named class -- but only where a named one exists
R3  the class EFO cross-references wins            -> decides 9 of 17
R4  else the class carrying a definition wins      -> decides 2
R5  else usage                                     -> decides 6
R6  else abstain (needs_curator)                   -> 0
```

**R3 is the good signal and it was Paul's.** EFO cross-references 495 CLO classes; an
external vocabulary picking one class over the other is an editorial judgement made
independently of us. In four groups EFO points at *both* twins, which is itself informative —
EFO does not know there are two.

**R5 is safe here, and only here.** CAB's objection to "most-used wins" is real: an obsolete
term keeps accruing annotations while its successor sits at zero, so usage is biased toward
the term that should lose. It does not apply to this population, because **no member of any
of the 17 groups is obsolete** — R1 never fires. And where R3 has an opinion it agrees with
usage in **9 of 9** groups, so R5 is corroborated extrapolation rather than a coin flip.

🛑 **The rule optimizes for consistency, not for the better-looking label.** `K 562 cell`
beats `K-562 cell` (30 uses to 8, equal on every other rule). If the nicer spelling should
win, that is a different rule and it is not mechanical.

### Why not the rule everyone proposed

UIB recommended, and CAB endorsed, *"CVCL groups, usage picks within the group"*. It cannot
run: **CLO records a Cellosaurus accession for 543 of its 40,851 classes (1.3%), and for
exactly one member of one of the 17 groups.** Grouping needs both members, so the rule groups
**zero**. The accessions sit on the well-curated classes and are absent from the duplicated
ones — HeLa and MCF7, never duplicated, both have one.

CLO also carries no `hasDbXref` for cell-line identity at all; its CVCLs are in `rdfs:seeAlso`,
which Gemma does not read. Reading it would surface 543 classes and still not rescue the rule.

## 5. What the shim covers, and what it does not

Applied in the three read VOs — `CharacteristicValueObject`, `AnnotationValueObject`,
`StatementValueObject` — across **all three value slots** (subject, object, second object),
because a term is as often in the object position as the subject.

🛑 **Not covered:** anything reading `EXPRESSION_EXPERIMENT2CHARACTERISTIC` directly, which
includes the annotation-usage aggregates and the search index. Those still see the stored
URI. This is a shim, not a fix, and the fix is §1's migration.

## 6. Retiring it

When the migration runs: apply the SQL, run `updateEe2c`, run
`resync_ee2c_from_characteristic.sql` for the rows the upsert cannot reach, reindex, then
**empty `TermUriMigration.tsv`**. A shim left in place over a corrected corpus silently
rewrites rows that are already right — and `CharacteristicUtilsUriMigrationTest` asserts the
table is non-empty, so it will go red and tell you.

## Related

- `scripts/sql/term_uri_migration.sql` — the parked migration
- `scripts/sql/annotation_uri_census.sql` — the census that found all of this
- `scripts/build_term_crossmatch.py` — the grouping and precedence, as code
- `docs/design/ONTOLOGY_SUPPLEMENTARY_METHODS.md` §Term canonicalization — the Methods prose
- `gemma-curation-agents-eval/analysis/2026-08-18_cell_line_crossmatch/` — the measurements
