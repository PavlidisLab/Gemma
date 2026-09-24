# Term-URI canonicalization — why Gemma returns a term it did not store

**Status 2026-08-18: read-time shim LIVE in code, database migration WRITTEN AND PARKED.**

Gemma resolves 97 annotation URIs to a different URI when it reads them — 48 of which the
corpus actually holds, and 49 of which it has never held at all. This document says which,
why, how they were chosen, and what has to happen for the shim to go away.

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
source. `CharacteristicUtils#canonicalUri` loads it at runtime; `scripts/gen_term_uri_migration.py`
generates the SQL from it, and `--check` fails if the two have drifted.

🛑 **They must not be maintained separately.** A shim and a migration that disagree about
what the corpus contains is the failure this arrangement exists to prevent — the same reason
`Relation.terms.txt` is one file rather than a list in prose and a list in code.

## 3. What is remapped

| lane | URIs | annotations | zero-usage | basis |
|---|---:|---:|---:|---|
| `malformed` | 29 | 266 | 0 | the URI is wrong on its face |
| `clo_twin` | 68 | 97 | 49 | two live CLO classes, one cell line |
| **total** | **97** | **363** | **49** | |

🛑 **The 49 zero-usage rows are not migration rows.** They map a URI this corpus has never
stored, so as `UPDATE`s they would match nothing, and `gen_term_uri_migration.py` keeps them
out of the SQL. They exist for the *resolver*: a client-side synonym table mints a twin out of
file order and asks Gemma which one to keep, and a table built from what the corpus uses is
structurally blind to exactly that twin. Serving them is the whole reason
`GET /annotations/canonicalUris` exists.

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
R3  the class EFO cross-references wins            -> decides 31 of 68 rows
R4  else the class carrying a definition wins      -> decides 2
R5  else usage, IF it clears the evidence floor    -> decides 35
R6  else abstain (needs_curator)                   -> 194 of 262 groups
```

**R5 has an evidence floor: the winner needs ≥2 annotations and must lead by ≥2.** Without it
24 groups are decided by a single annotation — one curator's spelling, typed once, becomes the
answer Gemma hands an external consumer as settled. `LOVO` x4 over `LoVo` x3 is the case that
proves the point: a margin of one is not a measurement. Below the floor the ladder abstains,
which is a first-class outcome here (Paul, 2026-08-18).

**R3 is the good signal and it was Paul's.** EFO cross-references 494 CLO classes; an
external vocabulary picking one class over the other is an editorial judgement made
independently of us. Across the 257 groups it decides **30** outright. In **27** more it points
at *both* twins, which is itself informative — EFO does not know there are two, and those
groups fall through to R4/R5. `22Rv1` is one of them, which is why corpus usage is the only
thing that can decide it and why an outside consumer cannot derive that row from EFO.

**R5 is safe here, and only here.** CAB's objection to "most-used wins" is real: an obsolete
term keeps accruing annotations while its successor sits at zero, so usage is biased toward
the term that should lose. It does not apply to this population, because **no member of any
of these groups is obsolete** — R1 never fires. And where R3 has an opinion it agrees with
usage in **9 of 9** groups, so R5 is corroborated extrapolation rather than a coin flip. The
floor above is the second guard: corroboration establishes the *direction*, not that any given
margin is large enough to read.

🛑 **The rule optimizes for consistency, not for the better-looking label.** `K 562 cell`
beats `K-562 cell` (30 uses to 8, equal on every other rule). If the nicer spelling should
win, that is a different rule and it is not mechanical.

### Why not the rule everyone proposed

UIB recommended, and CAB endorsed, *"CVCL groups, usage picks within the group"*. It cannot
run: **CLO records a Cellosaurus accession for 543 of its 40,851 classes (1.3%), and for
exactly one member of one of these groups.** Grouping needs both members, so the rule groups
**zero**. The accessions sit on the well-curated classes and are absent from the duplicated
ones — HeLa and MCF7, never duplicated, both have one.

CLO also carries no `hasDbXref` for cell-line identity at all; its CVCLs are in `rdfs:seeAlso`,
which Gemma does not read. Reading it would surface 543 classes and still not rescue the rule.

## 4a. 🛑 Groups are anchored on the ontology, not on the corpus

An earlier pass formed groups from the terms Gemma *uses*. That cannot see a pair whose twin
has zero usage — `22Rv1` (`CLO_0001200` x19 vs `CLO_0001199` x0) was invisible to it. CAB hit
that gap first: their Tier-0 synonym table *mints* `CLO_0001199` out of file order, asks Gemma
about it, and got nothing back. A zero-usage twin is not an edge case in that traffic; it is
the common case, because the disfavoured spelling is disfavoured precisely by being unused.

The producer is `scripts/build_term_crossmatch.py --clo-owl clo.owl --efo-obo efo.obo`, which
reads CLO and EFO off disk and forms groups from **CLO's own label collisions**, joining corpus
usage only afterwards as tie-break evidence. It is offline and reproducible; the numbers below
come out of it rather than out of a one-off run.

🛑 **The `' cell'`-suffix guard.** Group labels are normalized by dropping a trailing `' cell'`,
so `SW 480 cell` meets `SW480 cell`. That same strip also makes `cell line cell` collide with
`cell line`, and `immortal cell line` with `immortal cell line cell` — upper-level CLO classes,
not duplicate cell lines, and canonicalizing one onto the other is corruption dressed as a
repair. A group is therefore kept only if its labels still match *without* the strip. Four
artifacts fail it. One real pair fails it too (`SK-MEL-1 cell` / `SKMEL1`); both members have
zero usage, no definition and no EFO xref, so the ladder abstains on it anyway.

🛑 **And it bounds what this table can ever be.** CLO has **262** label-collision groups and
these rules decide **63**. The other **194** have no evidence that separates the twins — no
EFO xref pointing at one, no definition on one, and no usage margin clearing the floor — so
nothing we have decides them. **An absent URI means no mapping is known — never that the URI
is correct.** That is the failure-open shape, and a lookup table that reads as authoritative
where it is merely silent is worse than no lookup table at all.

### A row whose stated rule was false

The first cut carried `CLO_0007377 'LOVO cell' (x4) → CLO_0007378 'LoVo cell' (x3)`, justified
as *"R4 carries a definition; its twin does not"*. Neither twin carries a definition — checked
in `clo.owl` — so R4 cannot have fired, and under the ladder as documented the row inverts
(usage 4 > 3 picks `LOVO`). The destination was probably right by external convention
(Cellosaurus spells it `LoVo`), but it was a hand judgement recorded as a mechanical one, which
is the specific way a table like this loses the trust that makes it worth consuming. The row is
gone: 4-vs-3 does not clear the floor, so the group is now openly undecided.

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

- `GET /annotations/canonicalUris` — the table, queryable, for clients that resolve before asking
- `scripts/sql/term_uri_migration.sql` — the parked migration
- `scripts/sql/annotation_uri_census.sql` — the census that found all of this
- `scripts/build_term_crossmatch.py` — the grouping and precedence, as code
- `docs/design/ONTOLOGY_SUPPLEMENTARY_METHODS.md` §Term canonicalization — the Methods prose
- `gemma-curation-agents-eval/analysis/2026-08-18_cell_line_crossmatch/` — the measurements
