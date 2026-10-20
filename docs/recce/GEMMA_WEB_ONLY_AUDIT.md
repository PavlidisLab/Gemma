# `@GemmaWebOnly` — what it is really hiding

**Date:** 2026-08-10 · **Status:** ACTED ON 2026-08-18 — `@GemmaWebOnly` is deleted; all 79
sites now carry `@WithheldFromApi(Reason)`, enforced by `WithheldFromApiInventoryTest`
**Scope:** all 79 annotation sites across 15 files in `gemma-core/src/main` +
`gemma-rest/src/main` (as of `3646111985`)

> **Read this first.** The buckets below are the reasoning; the applied assignments are in
> `gemma-core/src/test/resources/withheld-from-api-inventory.txt`, which is the pinned
> record. Where the two disagree, the inventory is what ships. Three deliberate deviations
> from bucket D: `TaxonValueObject.isGenesUsable`
> moved to `UNTRIAGED` (it is populated in production code, so "redundant" was not true of
> it), while `TaxonValueObject.isSpecies` and most of bucket D were confirmed
> `REDUNDANT` by checking that nothing writes them at all. Bucket B's `POLICY` framing became
> `PUBLIC_PROJECTION_EXISTS` and then, once the wire shape was actually probed, `REDUNDANT`
> (see the correction in bucket B).
>
> **Taxonomy revised 2026-08-19.** `PUBLIC_PROJECTION_EXISTS` no longer exists: its only
> claimed instance was the false one in bucket B, and what remained of its meaning was
> indistinguishable from `REDUNDANT`. `REDUNDANT` is now narrowly "the data is already on
> the wire elsewhere, or trivially derivable from it" — the one reason asserting *no*
> hazard, and therefore the only one exempt from the suppression enforcement. Everything
> that is merely unusable moved to a new `INTERNAL_ONLY`: nothing populates it, its shape
> is lossy, or it sits on a VO nothing serves. That split matters because it decides
> whether the guard watches a member: all 62 of these previously sat under `REDUNDANT`
> and none were checked. Current counts — 3 `CALLER_IDENTITY`, 1 `DISCLOSURE`,
> 35 `REDUNDANT`, 37 `INTERNAL_ONLY`, **0** `UNTRIAGED`, 0 `POLICY` (76 sites; four members were
> deleted rather than re-reasoned — two on `DiffExResultSetSummaryValueObject`, plus
> `ExpressionExperimentSubsetValueObject.accession` (replaced by an annotated `getAccession()`)
> and its `getSourceExperiment()`).
> Bucket C folded into
> `INTERNAL_ONLY` — the VO is deprecated and unserved, which is a structural fact about
> the member, not a duplication.

## Why this exists

`@GemmaWebOnly` is `@JacksonAnnotationsInside @JsonIgnore`. It works: an annotated
field or getter is excluded from every REST response. What no longer holds is its
*stated reason* — "Indicate that a property is exclusively used for Gemma Web" —
because Gemma Web was deleted in `bb154eee88`.

That gap is the hazard. The annotation invites exactly one argument: *"Gemma Web is
gone, so this hiding is vestigial; remove it."* That argument is correct for some of
these fields, and a security bug for others, and the annotation gives you no way to
tell which is which. It was used correctly once already — `originalValue` on
`CharacteristicValueObject` (`3646111985`) — and the same reasoning applied to
`ExpressionExperimentValueObject.getCurrentUserIsOwner()` would leak per-caller
authorization state onto a cacheable response.

This document classifies all 79 so the question is settled once instead of
re-litigated per field.

## Verdict

**Do not sweep.** Two of the five buckets must stay hidden for reasons that have
nothing to do with Gemma Web, and one of those is already solved by a parallel VO
that would be duplicated by a naive removal. The recommended change is to make the
*reason* legible, not to remove the annotation.

| bucket | sites | action |
|---|---:|---|
| A — caller-identity / disclosure | 4 | keep hidden; re-annotate so it cannot be swept |
| B — ~~parallel VO exists~~ **premise false, see below** | 17 | already public; suppression is inert |
| C — dead VO (`@Deprecated`, off the REST path) | 12 | annotation is moot; retire the VO instead |
| D — display shims redundant with exposed data | 35 | harmless either way; delete the field or leave |
| E — real data withheld only because Web consumed it | 11 | expose on request, one at a time, after tracing |

Buckets are exhaustive and disjoint: 4 + 17 + 12 + 35 + 11 = 79.

---

## A — caller-identity and disclosure (4 sites) — KEEP HIDDEN

These depend on **who is asking**, not on the entity. Serializing them onto a
response that is cached by URL is the classic cross-user leak.

| file | member |
|---|---|
| `ExpressionExperimentValueObject` | `getCurrentUserHasWritePermission()` |
| `ExpressionExperimentValueObject` | `getCurrentUserIsOwner()` |
| `GeneSetValueObject` | `getCurrentUserIsOwner()` |
| `BioMaterialValueObject` | `fastqHeaders` |

The dataset endpoints carry `@CacheControl(maxAge = 1200)` alongside
`@CacheControl(isPrivate = true, authorities = {"GROUP_USER"})`. The private
qualifier is what currently keeps a per-user field from being shared — a second,
independent control from this annotation. Removing `@GemmaWebOnly` here would make
correctness depend entirely on that qualifier staying right forever.

`fastqHeaders` is raw sequencer header text; it can carry internal paths, run
identifiers and submitter-local naming. It is not per-user but it is disclosure.

**These four are the reason not to bulk-remove.** If nothing else in this document
is acted on, keep these hidden.

## B — deliberate policy, and the alternative already exists (17 sites) — ~~KEEP HIDDEN~~ WRONG

> **Corrected 2026-08-18.** Everything in this section rests on the premise that these getters
> are suppressed. They are not, and never were. Each backing field carries an explicit
> `@JsonProperty`, which Jackson keeps in preference to the `@JsonIgnore` on the parallel
> getter, so `GeeqValueObject` publishes all 17 decomposed scores today — inline on
> `GET /datasets/{dataset}` and through `PipelineStatusValueObject` too. Verified by
> serializing the VOs, not by reading the annotations. `GeeqValueObject.java:54` is the
> giveaway: the field is `sScorePlatformsTechMulti` but its `@JsonProperty` says
> `"sScorePlatformTechMulti"`, so the wire name matches neither field nor getter.
>
> The 17 are therefore recorded as `REDUNDANT` ("the suppression is inert"), not
> `PUBLIC_PROJECTION_EXISTS`. `PublicGeeqValueObject` added no reach that
> `GeeqValueObject` does not already give, so it was retired — see below.

Every per-factor `sScore*` / `qScore*` getter on `GeeqValueObject`.

The section originally argued that `PublicGeeqValueObject` existed precisely to answer
the REST need, and quoted its javadoc as evidence. That javadoc was itself describing a
suppression that did not work, so it evidenced nothing.

Probing the two VOs showed they serialized **identical 25-key payloads**, so the
"public projection" projected nothing. `PublicGeeqValueObject` was retired and
`GET /datasets/{dataset}/geeq/public` now returns `GeeqValueObject` directly — a
byte-identical response. The surviving split is two-tier, not three: `Geeq` for everyone
and `GeeqAdmin` for the detected/manual override scores and `otherIssues`, which are
genuinely admin-only because they are declared on the subclass rather than hidden on the
parent.

## C — dead VO (12 sites) — THE ANNOTATION IS MOOT

Every site on `FactorValueValueObject`, which is `@Deprecated` at class level:
`charId`, `value`, `valueUri`, `predicate`, `predicateUri`, `object`, `objectUri`,
`secondPredicate`, `secondPredicateUri`, `secondObject`, `secondObjectUri`,
`needsAttention`.

REST serializes factor values through `FactorValueBasicValueObject`
(`BioMaterialValueObject.getFactorValues()` returns the basic VOs; the
`factorValueObjects` collection beside it is plain `@JsonIgnore`). Nothing here is
withheld from a REST client that has any way to reach it.

Do not spend effort re-annotating these. They disappear when the deprecated VO is
retired; until then they are inert.

## D — display shims redundant with data already exposed (35 sites) — HARMLESS

Editor-rendering state and denormalized convenience copies. No client is missing
anything: the underlying data is already on the wire in a better shape.

* `CharacteristicValueObject` (11) — `urlId`, `alreadyPresentInDatabase`,
  `alreadyPresentOnGene`, `child`, `root`, `numTimesUsed`, `ontologyUsed`,
  `privateGeneCount`, `publicGeneCount`, `taxon`, `valueDefinition`. `numTimesUsed` was
  briefly moved to `UNTRIAGED` on the belief that production code populated it; the writer
  is `OntologyServiceImpl.countOccurrences`, reachable only from `findTermsInexact`, whose
  last production caller went with gemma-web. It counts characteristic rows (sample-level
  and factor-value-level occurrences), whereas the endpoint already publishes the
  more useful per-experiment tally as `usageCount` — so it is back in this bucket. Mostly
  Phenocarta-era, flagged as such in the source. `privateGeneCount` sounds
  disclosure-shaped but is a Phenocarta gene tally, not an ACL count — still, do not
  expose it without checking what it counts today.
* `AnnotationValueObject` (7) — `description` and the six `parent*` /
  `parentOfParent*` fields, which rendered an ontology tree in the Web UI. Superseded
  by `/annotations/term` and the `parents` field on search hits.
* `BioMaterialValueObject` (7 of 8) — `assayName`, `assayDescription`,
  `assayProcessingDate`, `characteristicValues`, `characteristicOriginalValues`,
  `factorValues`, `factorIdToFactorValueId`. The two maps are category-keyed and
  therefore lossy (two characteristics sharing a category collide); the
  per-characteristic fields on `characteristics` are strictly better and already
  serialize. The assay fields duplicate the `BioAssay` payload.
* `TaxonValueObject` (2) — `isSpecies`, `isGenesUsable`.
* `GeneValueObject` (3), `GeneSetValueObject` (2), `ArrayDesignValueObject` (1) —
  flattened `getTaxonId()` / `getTaxonName()` / `getTaxon()` accessors beside a taxon
  object that is already serialized.
* `ExperimentalFactorValueObject` (1) — `getNumValues()`, derivable from the values.
* `ExpressionExperimentValueObject` (1) — `getTaxon()`, same flattening.

Deleting these fields outright is cleaner than re-annotating them, but there is no
urgency and no risk in leaving them.

## E — real data, withheld only because Web was the consumer (11 sites) — DRAINED

The bucket `originalValue` came from. Each was thought to be genuine data a REST client
might legitimately want and could not get. **None should be exposed speculatively** —
the `originalValue` precedent is the right process: a consumer names it, trace where
it is populated and whether null is meaningful, then un-hide with a `NON_NULL` guard
and a serialization test.

> **Closed out 2026-08-20.** The bucket is empty and `UNTRIAGED_CEILING` is `0`, so any new
> `UNTRIAGED` now fails `WithheldFromApiInventoryTest`. Applying that process to all 13 exposed
> nothing: every entry resolved to a deletion or a re-reason, because in each case the *stated*
> reason turned out to be wrong about the data rather than the exposure being wrong.
>
> Deleted, once tracing showed nothing wrote them or the datum was already public under an
> accurate name: `DiffExResultSetSummaryValueObject.qValue` and `getResultSetId()`,
> `ExpressionExperimentSubsetValueObject.accession` (replaced by an annotated `getAccession()`,
> which the interface requires) and its `getSourceExperiment()`.
>
> Re-reasoned: the four `StatementValueObject` `second*` fields → `REDUNDANT` (already published,
> flattened, by `AbstractFactorValueValueObjectSerializer` — see the correction below);
> `CharacteristicValueObject.numTimesUsed` → `REDUNDANT` (the endpoint publishes the better
> per-experiment tally as `usageCount`); `QuantitationTypeValueObject.expressionExperimentId` →
> `REDUNDANT` (every serving endpoint addresses the experiment in the request path); both
> `minPvalue` copies → `INTERNAL_ONLY` (nothing originates a value); `TaxonValueObject.isGenesUsable`
> → `INTERNAL_ONLY`.
>
> `isGenesUsable` is the one whose reason rests on data rather than structure, and is worth
> revisiting on that basis: it *is* populated, and it usefully bounds an untargeted GO search's
> fan-out internally. It is withheld because production serves only genes-usable taxa, making it a
> constant `true` on the wire — but `GET /taxa` serves every taxon unfiltered and `GeoConverterImpl`
> still writes `false` for GEO-imported taxa, so if genes-less taxa start being served the field is
> informative again.

`ExpressionExperimentSubsetValueObject.getSourceExperiment()` was deleted rather than exposed. It
returned `sourceExperimentId` verbatim, so the datum was already public under the accurate name;
its `@deprecated` tag pointed at `getSourceExperimentId()`, which is Lombok-generated and therefore
had no javadoc of its own to carry the redirect back. It had zero callers at bytecode level, and
the name collided with the live, non-deprecated `ExpressionExperimentSubSet.getSourceExperiment()`
on the entity — same name, different return type, only the VO's copy dead. Removing it changed no
payload, since it was suppressed.

> **Corrected 2026-08-19.** This section previously called the four `StatementValueObject`
> `second*` fields "the strongest lead", on the argument that `AnnotationValueObject` exposes
> the same four so the asymmetry must be accidental, and that a client reading statements
> through the factor-value serializer therefore "sees a truncated statement". That last claim
> is exactly backwards, and the git history says so.
>
> All eight relational slots arrived `@GemmaWebOnly` in `4b21c3a06c` (2023-09-21). The split was
> made deliberately two months later in `dff752727c` ("Serialize statements", **fix #814**),
> which un-hid `predicate*` / `object*` **and in the same commit added
> `AbstractFactorValueValueObjectSerializer`** — a custom serializer that flattens a compound
> statement into *two* entries in the `statements` array sharing one subject, the second
> carrying the second clause under the generic `predicate` / `object` keys. So the client does
> not see a truncated statement; it sees the second clause as its own statement. That
> flattening is still in place today.
>
> The four are therefore `REDUNDANT` — already on the wire under another name — and exposing
> them would be a duplication bug, not a fix: a client reading both `statements[]` and the raw
> fields would see a compound statement's second clause twice.
>
> `AnnotationValueObject` exposing its own `second*` directly (`91c42152b5`, 2026-06-14, 2.7
> years later) is consistent rather than contradictory: it is serialized as a plain bean with
> no flattener, so direct fields are the only way to carry the compound shape there.

Both `minPvalue` copies left this table as `INTERNAL_ONLY`. The note here read "check whether it
is meaningful without the analysis context", which had the premise backwards: there is no analysis
context to be meaningful in, because nothing ever writes the field. It is declared twice on sibling
classes (`ExpressionExperimentValueObject:138`, `ExpressionExperimentSubsetValueObject:59` — not
inherited; `BioAssaySetValueObject` does not declare it), and across all three modules it appears
at only three lines. The third, `ExpressionExperimentValueObject:316`, is a copy constructor
propagating it — so nothing originates a value. Ruled out as sources: every constructor on both
classes, the single `select new` projection in the repo (which targets
`AnnotationSetSummaryValueObject`), all four `aliasToBean` call sites (all entities, no VO), the
EE VO's own result transformer and the base `doLoadValueObject` path, and the schema — there is no
`MIN_PVALUE` column. Deleting both fields is the follow-up; the EE VO's line 316 goes with it.

`ExpressionExperimentSubsetValueObject.accession` left this table by being deleted. Nothing had
ever written it — no constructor set it and no call site in the reactor invoked the setter — so it
could only serialize a permanent `null`, matching `INTERNAL_ONLY`'s first shape rather than
"real data withheld". The field is gone; `getAccession()` survives as an explicit
`return null;` because `BioAssaySetValueObject` declares it, and carries the annotation so the
null stays off the wire. Whether that method belongs on the interface at all — one implementor
can never answer it — is the open question left behind.

`QuantitationTypeValueObject.expressionExperimentId` left this table as `REDUNDANT`. All
six endpoints that put the VO on the wire — `GET`/`PATCH` on
`/datasets/{dataset}/quantitationTypes[/{qtId}[/preferred]]`, plus
`/datasets/{dataset}/subSetGroups`, `/subSetGroups/{subSetGroup}` and
`/datasets/{dataset}/heatmap-data` — address the experiment in the request path, so a client
holding the response already holds the id this field would repeat.

`DiffExResultSetSummaryValueObject` used to hold two entries here; both are now gone
rather than exposed. `qValue` was never written by any code since its introduction in
2009, and its only reader was a null-guarded line in the retired gemma-web summary
tree, so it was deleted. `getResultSetId()` returned `id` verbatim — the datum was
already public under the right name — so its two internal call sites were pointed at
`getId()` and the alias deleted. Neither removal changes the JSON: `qValue` only ever
serialized as null, and `getResultSetId()` was suppressed.

---

## Recommended change (not made here)

Split the annotation by **reason**, so the sweep argument cannot reach the fields
where it is wrong:

* keep `@GemmaWebOnly` for bucket D only — genuinely vestigial display state, and its
  name is then accurate again;
* introduce something like `@NotForPublicApi` (or reuse an existing marker if one
  turns up) for buckets A and B, with the rationale in the javadoc — caller-identity,
  disclosure, or "a public projection VO exists, use it";
* leave bucket C alone; it dies with `FactorValueValueObject`;
* handle bucket E one field at a time, on request.

After that, "Gemma Web is gone, delete `@GemmaWebOnly`" becomes a safe mechanical
change instead of a security question, which is the actual goal.

## Method

```bash
rg -n --type java "^\s*@GemmaWebOnly\s*$" gemma-core/src/main gemma-rest/src/main
```

Anchoring the pattern to line start matters: an unanchored `@GemmaWebOnly` also
matches prose in javadoc (this document's own subject appears in
`CharacteristicValueObject`'s comment), which inflates the count to 81 and mis-pairs
fields when walking forward to the next member.
