# Publication links carry evidence

**Status:** landed on `phase2-acl-migrate`, 2026-08-17. MySQL migration `V25`, H2 `V26`.
**Origin:** `handoffs/PUBLICATION_LINKS_NEED_EVIDENCE_IN_GEMMA_2026_08_17.md` (cab).

## What was missing

Gemma could record *"the publication for this experiment is X"* and nothing else. It could
not record that Y had been considered and ruled out, that X came from GEO's own cross-link
rather than from anyone reading the paper, or who decided and when. Annotations have carried
`EVIDENCE_CODE` and (since V22) `SUPPORTING_EVIDENCE` for years; the publication link was the
one assertion in the model with no evidence slot at all.

The practical consequence is that every rejection had to be remembered somewhere Gemma cannot
see — currently a hand-maintained file of 16 exclusions in the eval repo — and every re-run of
a publication finder re-proposes the paper a curator already threw out. At 500 datasets that
is annoying. At 50,000, with a measured 3.8% wrong-or-missing rate, it is ~1,900 rows nobody
maintains.

## The shape

One new table, `PUBLICATION_ASSOCIATION`, one row per (experiment, publication):

| column | notes |
|---|---|
| `STATUS` | `ACCEPTED` / `REJECTED`. The rejection is the addition that matters. |
| `ROLE` | `PRIMARY` / `OTHER_RELEVANT` on accepted rows; null on rejected ones. |
| `SOURCE` | who asserted it — see the ranking below. |
| `EVIDENCE` | the one-line quotable basis, safe to show as-is. |
| `SUPPORTING_EVIDENCE` | opaque JSON array, agents' `FindingEvidence` shape. Never parsed by Gemma. |
| `EVIDENCE_CODE` | `GOEvidenceCode`, same vocabulary annotations use. `IC` / `TAS` / `IEA` / `IIA` do the work here. |
| `CONFIDENCE` | `[0,1]`, for machine assertions. |
| `ASSERTED_BY` / `ASSERTED_AT` | free-text actor and timestamp. |

Unique on `(INVESTIGATION_FK, PUBLICATION_FK)`.

### Why a new table and not columns on `RELEVANT_PUBLICATIONS`

Two reasons, either one decisive:

1. **The primary publication isn't in that table.** It is a plain FK on `INVESTIGATION`. The
   link that matters most — and the one GEO gets wrong — would have had nowhere to hang
   evidence.
2. **A rejection must not be visible as a publication.** Parked in `RELEVANT_PUBLICATIONS`
   with a `STATUS` column, production Gemma 1.32.x — which shares this database and does not
   read `STATUS` — would list every rejected paper as a relevant publication of the
   experiment. No additive column avoids that.

So the migration is a bare `CREATE TABLE` plus a backfill. Nothing existing is altered,
renamed or dropped, and 1.32.x is unaffected in any deploy order. Same reasoning as V24.

### Precedence is a rank, not a list

```
CURATOR 40  >  GEO_SUBMITTER_LINK 30 == EXTERNAL_IMPORT 30  >  AGENT 20  >  LEGACY 10
```

A writer may only displace an assertion it outranks; ties go to the later write, so two
curators in sequence behave as expected. This is evaluated at the one point every writer
passes through, which is the whole difference between this and the exclusion file it replaces
— a denylist protects exactly the code paths that remember to read it.

Concretely: a curator rejects GEO's link at rank 40; the nightly GEO refresh re-proposes it at
rank 30 and is refused. On the eval side, the absence of that property is what let a
correction applied on 2026-08-13 get silently reverted by a cache rebuild on 08-14.

**Scope of the refusal.** Rank blocks *accepting* a publication that stands rejected, and
blocks *overwriting the stated basis* of a higher-ranked assertion. It does not block a
curator changing their mind through the write API: reaching that endpoint means passing
`ACL_SECURABLE_EDIT`, and a human must not need an escape hatch to reverse themselves. What
the rule stops is an unattended writer undoing a human by accident, which is the failure that
has actually happened.

## Where it is enforced

| writer | behaviour |
|---|---|
| `ExpressionExperimentService.updatePublications` (4-arg) | owns both halves — writes the links and reconciles the assertions in one transaction. The 3-arg form still works and records bare `CURATOR` assertions. |
| `ExpressionExperimentService.commitCuration` | routes its publications section through the same reconcile, so a composite commit cannot leave a stale `ACCEPTED` row. |
| `GeoServiceImpl` refresh | asks `findBlockingRejection` before taking `!Series_pubmed_id`; logs the standing rejection and leaves the link unset. |
| `GeoServiceImpl` fresh import | records `GEO_SUBMITTER_LINK` / `TAS` for the links `convertPubMedIds` created. Best-effort, alongside `storeSourceMetadata`. |
| `UpdatePubMedCli`, `ExpressionExperimentPrimaryPubCli` | same check, and record the right source — the primary-pub CLI distinguishes an id from its input file (`CURATOR`/`IC`) from one it located in GEO (`GEO_SUBMITTER_LINK`/`TAS`). |
| `MergeDuplicateBibRefsCli` | calls `rebindPublication(dup, canonical)` alongside the link repoint. The FK to the reference deliberately does not cascade, so without this the delete that follows hits the constraint, gets swallowed by the CLI's `catch`, and the merge silently stops merging while the assertions go on naming a reference nothing else uses. Where the dataset already asserts something about the canonical row, that assertion wins and the duplicate's is dropped — the unique key allows only one. |

**Not every link has a row, and code should not assume one.** Experiment splitting copies the
parent's publication to each split, and the CELLxGENE and simple-metadata loaders take theirs
from the source file; none records an assertion. A null association means "nothing was
recorded about where this came from", the same standing as a `LEGACY` row.

## REST

`GET /datasets/{id}/publications` returns `DatasetPublicationValueObject` — every field the
old `BibliographicReferenceValueObject` had, plus a nullable `association`. Strictly additive
on the wire. `?includeRejected=true` appends the ruled-out papers: the "do not re-propose" set
a finder should read before it starts. Off by default, because a rejection is a record of a
decision and must not be picked up by anything listing a dataset's papers.

`PUT /datasets/{id}/publications` takes evidence on every entry and gains
`rejectedPublications`:

```json
{
  "primaryPublication": {
    "pubMedId": "38165001", "source": "curator", "evidenceCode": "IC",
    "evidence": "The series title names this paper almost verbatim."
  },
  "otherRelevantPublications": [],
  "rejectedPublications": [{
    "pubMedId": "38088204", "source": "curator", "evidenceCode": "IC",
    "evidence": "GEO's !Series_pubmed_id, but it names a different NAR 2024 paper by the same lab."
  }]
}
```

`source` defaults to `curator`, which outranks everything — **an agent must set
`"source": "agent"` explicitly** or its proposals will outrank the curators they are meant to
defer to. An unrecognised `source` is a 400 rather than a silent fall back to the top of the
ranking. An acceptance that stands rejected by a higher authority is a 409.

Dropping a publication from the accepted lists *retracts* its assertion; naming it under
`rejectedPublications` *records why*. That is the difference between forgetting and deciding,
and only the second one is enforced against later writers.

## The backfill, and what it claims

V25 seeds an assertion for every link that existed when it ran.

- **Primary publication on a GEO-accessioned dataset** → `GEO_SUBMITTER_LINK`, code `IIA`,
  with evidence text saying in words that this was inferred from the import path and not
  checked against GEO. The GEO importer is the only writer that sets a primary without a human
  in the loop, so the inference is good; it is not verification, and a curator who replaced the
  link by hand is mislabelled by it. The cost of that is nil — a curator ruling still outranks
  GEO, and the refresh path only writes when there is no primary at all.
- **Everything else, including all other-relevant links** → `LEGACY`, no evidence. Other-relevant
  is not given the GEO treatment even on GEO datasets: GEO does land there (the second and later
  `!Series_pubmed_id` values) but so does every curator who has attached a follow-up paper, and
  nothing in the row distinguishes them. For the primary slot the automated writer dominates;
  for this one it does not.

`SELECT COUNT(*) ... WHERE SOURCE = 'LEGACY'` is the answer to "how many links still have no
recorded basis?".

## GSE227854

The worked example, and the only one of the nineteen known-wrong links whose error is
*upstream*. GEO's `!Series_pubmed_id` is `38088204` (NAR 2024-02-09, U2AF1/ZRSR2 splicing);
the series title, "Dissolution of RNA condensates by the embryonic stem cell protein L1TD1",
names `38165001` (NAR 2024-04-12) almost verbatim. Same lab, two NAR papers two months apart —
the submitter cross-linked the wrong one of their own.

As of 2026-08-17 the dataset (id 27929) has **no publication in Gemma at all**, so nothing is
currently wrong. What needs preventing is the next GEO refresh, which sets a primary precisely
when there is none. The rejection SQL, and the `PUT /datasets/{id}/publications` equivalent that
is preferable once the code is deployed (it resolves the reference from PubMed instead of
requiring it to already exist), live in `~/Dev/Gemma/handoffs/` alongside the review list —
`PUBLICATION_BACKFILL_AND_GEO_EXPOSURE_2026_08_17.md`. This repo keeps the design; per-dataset
curation decisions and measured corpus tallies are data and are not tracked here.

The other eighteen corrections in the eval repo are that repo's gold being wrong, not Gemma,
and are deliberately not loaded.

## Follow-ups

- Retire the eval-side denylist and the `rebuild_paper_context_cache.py` patch once this is
  deployed. Two sources of truth for "this link is wrong" is the same failure in a new place.
- **Make this table the read source (Paul, 2026-08-17: "that's fine").** Two separable halves, and
  only one is available now. *Reads* can derive primary / other-relevant from `ROLE` where
  `STATUS='ACCEPTED'` — but not until assertion coverage is complete, because a link with no row
  would simply vanish from the dataset. That is a data-completeness gate, not a code one: close the
  three bypass writers below, then reconcile the pre-deploy drift, then flip. *Writes* to
  `INVESTIGATION.PRIMARY_PUBLICATION_FK` / `RELEVANT_PUBLICATIONS` **cannot stop while 1.32.x is
  live** — it shares this database and reads only those, so dropping the legacy write makes
  publications disappear from Gemma 1.0. Dropping the columns is strictly a cutover step.
- Assertions for the writers that still bypass this (split, CELLxGENE, simple loader) — small, and a
  prerequisite for the read flip above rather than the optional cleanup it looked like initially.
- **Verify the 23,066 backfilled `IIA` rows against GEO.** Batched esummary over the accessions
  (~120 requests) yields, for each, whether Gemma's primary still matches GEO's `!Series_pubmed_id`.
  Matches can be promoted `IIA` → `TAS`. Mismatches are the interesting set and split two ways that
  no automated rule can separate: a curator corrected GEO (GSE102911), or GEO is wrong (GSE227854).
  Either way it turns 23,066 unexamined rows into a reviewable list — the corpus-scale version of
  the audit cab did by hand for 500.
