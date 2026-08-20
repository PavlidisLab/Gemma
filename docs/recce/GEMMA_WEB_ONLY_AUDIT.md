# `@GemmaWebOnly` — what it is really hiding

**Date:** 2026-08-10 · **Status:** ACTED ON 2026-08-18 — `@GemmaWebOnly` is deleted; all 79
sites now carry `@WithheldFromApi(Reason)`, enforced by `WithheldFromApiInventoryTest`
**Scope:** all 79 annotation sites across 15 files in `gemma-core/src/main` +
`gemma-rest/src/main` (as of `3646111985`)

> **Read this first.** The buckets below are the reasoning; the applied assignments are in
> `gemma-core/src/test/resources/withheld-from-api-inventory.txt`, which is the pinned
> record. Where the two disagree, the inventory is what ships. Four deliberate deviations
> from bucket D: `CharacteristicValueObject.numTimesUsed` and `TaxonValueObject.isGenesUsable`
> moved to `UNTRIAGED` (both are populated in production code, so "redundant" was not true of
> them), while `TaxonValueObject.isSpecies` and most of bucket D were confirmed
> `REDUNDANT` by checking that nothing writes them at all. Bucket B's `POLICY` framing became
> `PUBLIC_PROJECTION_EXISTS` and then, once the wire shape was actually probed, `REDUNDANT`
> (see the correction in bucket B). Bucket C folded into `REDUNDANT` rather than getting a
> constant of its own — the VO is deprecated, not the suppression.

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
> `PUBLIC_PROJECTION_EXISTS`. `PublicGeeqValueObject` adds no reach that
> `GeeqValueObject` does not already give, which makes its existence an open question rather
> than the justification this section treated it as. `GeeqValueObject`'s own class javadoc says
> "Represents publicly available geeq information", which points at retiring the duplicate
> rather than suppressing the originals — but that is a decision, not a finding.

Every per-factor `sScore*` / `qScore*` getter on `GeeqValueObject`.

This is not a Gemma Web artifact. `PublicGeeqValueObject` exists precisely to answer
the REST need, and says so:

> Public per-factor GEEQ breakdown. Mirrors `GeeqValueObject` but without the
> `@GemmaWebOnly` JSON-suppression on the per-factor sScore* / qScore* getters, so
> the decomposed scores reach REST clients. Admin-only fields exposed by
> `GeeqAdminValueObject` (detected/manual override scores, free-text `otherIssues`)
> are deliberately omitted.

So the decomposed scores **are** reachable, through a VO built to expose exactly the
safe subset. Removing the annotation on `GeeqValueObject` would not add a capability;
it would duplicate one and bypass the admin-field exclusion that the public VO makes
explicit. A three-tier design (`Geeq` / `PublicGeeq` / `GeeqAdmin`) is doing real
work here.

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
  `privateGeneCount`, `publicGeneCount`, `taxon`, `valueDefinition`. Mostly
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

## E — real data, withheld only because Web was the consumer (11 sites) — CANDIDATES

The bucket `originalValue` came from. Each is genuine data that a REST client might
legitimately want and currently cannot get. **None should be exposed speculatively** —
the `originalValue` precedent is the right process: a consumer names it, trace where
it is populated and whether null is meaningful, then un-hide with a `NON_NULL` guard
and a serialization test.

| file | member | note before exposing |
|---|---|---|
| `StatementValueObject` | `secondPredicate`, `secondPredicateUri`, `secondObject`, `secondObjectUri` | `AnnotationValueObject` already exposes the same four. The asymmetry looks accidental — most likely a real gap. |
| `ExpressionExperimentValueObject` | `minPvalue` | check whether it is meaningful without the analysis context |
| `ExpressionExperimentSubsetValueObject` | `accession`, `minPvalue`, `getSourceExperiment()` | `accession` on a subset is plausibly wanted; `getSourceExperiment()` may be reachable already via another field |
| `DiffExResultSetSummaryValueObject` | `qValue`, `getResultSetId()` | a result-set id that clients cannot see is suspicious; check whether it duplicates `id` |
| `QuantitationTypeValueObject` | `expressionExperimentId` | back-pointer; check for a redundant path |

The `StatementValueObject` four are the strongest lead: the identical fields are
public on `AnnotationValueObject`, so the data is not considered sensitive, and a
client reading statements through the factor-value serializer sees a truncated
statement.

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
