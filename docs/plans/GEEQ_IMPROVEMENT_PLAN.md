# GEEQ improvement plan — wider inputs, and a calibration that isn't vibes

**Status: PLANNED, not started.** Opened 2026-08-20 (Paul). This doc is the plan
item; no code has moved.

## The ask

Two things, plus a corollary:

1. **Widen and improve the metrics that feed the score.** The agents' curation
   output is a candidate input we don't use today.
2. **Calibrate it.** Today the thresholds and the ±1 ladder are calibrated on
   vibes. The working prior is that **~20% of experiments are "bad", possibly
   more** — that number has to come out of a measurement, not go into one.
3. **Corollary — decide as much as possible up front.** The agent already
   scrapes GEO before import. Anything decidable at scrape time saves the import
   + preprocess effort on experiments we would not have wanted.

## The distinction that makes this tractable

**"Suitability" is the easy half.** It asks: is this experiment worth Gemma's
effort? It is definable, mostly decidable from GEO metadata alone, and its job is
to *prioritize effort*. That is the half to push on first, and the half that can
run before import.

**"Bad" is the hard half.** It means *there's something possibly wrong here* — a
suspicion, not a verdict. There is no good definition of it today and inventing
a crisp one would be dishonest. Treat it as a flag that routes an experiment to
a look, and keep it calibrated against an actual labelled set rather than
against an adjective.

Calibrate the two separately, against different targets. They are already
separate fields; the plan keeps them separate.

## What exists today

Grounded on `phase2-acl-migrate`, so the plan doesn't get re-derived next session:

* `Geeq` (entity) — **8 suitability subscores** (publication, platform amount,
  platform tech multi, avg platform popularity, avg platform size, sample size,
  raw data, missing values) and **9 quality subscores** (outliers, sample
  mean/median correlation, correlation variance, platform tech, replicates,
  batch info, batch effect, batch confound). Each lives in `[-1, 1]`.
* `GeeqServiceImpl` computes them; the public score is a **weighted mean** of the
  subscore array (`getWeightedMean`), stored as `detectedQualityScore` /
  `detectedSuitabilityScore`.
* **Weights**: suitability `{1,1,1,1,1,1,1,1}`; quality `{1,0,1,0,1,1,1,1,1}`.
  🛑 `qScoreSampleMeanCorrelation` and `qScoreSampleCorrelationVariance` are
  computed, stored, and serialized — and contribute **nothing**. Any re-weighting
  starts here: either they earn a weight or they stop being presented as
  subscores.
* The subscores are step functions over hand-picked cutoffs — platform
  popularity at 10/20/50/100 experiments, per-taxon platform-size cutoffs, ±1
  flags for raw data / publication presence / two-colour. **This is the vibes
  calibration**: the cutoffs and the −1/−0.5/0/+0.5/+1 ladder were chosen, never
  fit to anything.
* **Manual override**: `manualQualityScore` / `manualSuitabilityScore` +
  `manual*Override` booleans, admin-only via `GeeqAdminValueObject`.
* **Modes**: `GeeqService.ScoreMode.{all,batch,reps,pub}`, driven from `GeeqCli`
  and `PreprocessorService`; each scoring run writes a `GeeqEvent`.
* **Read + write surface** (already on REST): `GET /datasets/{id}/geeq` and
  `/geeq/public` return `GeeqValueObject`, per-subscore breakdown included;
  `GeeqAdminValueObject` adds the detected/manual override scores and the
  free-text `otherIssues`; `PUT /datasets/{id}/geeq` sets the manual override;
  `POST /datasets/{id}/geeq/recalculate` (alias `/recompute`) re-scores.
  `geeq.publicQualityScore` / `geeq.publicSuitabilityScore` are filterable and
  sortable EE properties.

## Work items

### G1. Audit the subscores we already have (do first, cheap)

Distribution of every subscore across the corpus (~23.5k experiments): what
fraction of experiments each one separates, how they correlate with each other,
how much each contributes to the variance of the final score. The expected
finding is that several are near-constant — they carry no information but dilute
the mean, and two carry weight 0 already. Deliverable: one table + a figure
regenerated from live data by a script, not hand-transcribed.

### G2. Turn "bad" into a labelled set

Nothing downstream can be calibrated against an adjective. Assemble a panel with
a real disposition per experiment, from what already exists:

* rows where a curator set `manualQualityOverride` — **the strongest existing
  label is a curator disagreeing with the score**; count them first;
* `curationDetails.troubled` / `needsAttention`;
* the `otherIssues` free text;
* agent audit findings (below).

Then measure the prevalence rather than assuming it. Paul's ~20% is a prior to
test; the calibrated flag rate is an output of this step.

### G3. Agent curation output as an input

`gemma-curation-agents` already produces per experiment: proposed
factors/FVs/statements, audit findings with issue codes, and (for the audited
panels) adjudicated precision. Signals worth testing as GEEQ inputs:

* **design legibility** — does the agent recover a usable design from the GEO
  metadata at all? A design that neither a curator nor an agent can read is a
  suitability problem, and it is knowable before import;
* **disagreement with existing curation** — per-experiment counts of
  `missing_factor`, `wrong_category`, `conflated`;
* ungrounded-tag / term-grounding failure counts;
* title/summary text hygiene.

🛑 Guard: audit findings carry a measured false-positive rate (audit50
adjudicated to ~70% real). Feed **adjudicated** categories, or route the raw
signal to the "possibly wrong" flag — never straight into a public number.

### G4. Score before import — the scrape-level decision

Most of the suitability half needs nothing Gemma-side: publication presence,
platform identity / popularity / size, sample size, raw-data availability, and
design legibility are all derivable from what the scrape already has. Proposal: a
**pre-import suitability estimate computed at scrape time** and carried on the
preboarded GEO source-metadata blob, so triage happens before we spend a full
import + preprocess.

Validation is free: compute the pre-import estimate for experiments already in
Gemma and compare it against their post-import `sScore*`. That tells us how much
of suitability is knowable up front, which is the whole question.

### G5. Recalibrate the aggregation

In increasing order of ambition: (a) re-weight the existing subscores to fit the
G2 panel; (b) replace the step functions with continuous transforms; (c) fit a
model on the panel, keeping the subscores as interpretable features. Whatever
wins must stay explainable per subscore — the breakdown is on the wire and
curators read it.

Any change to weights or to the `[-1, 1]` convention **is a version bump on the
score**: old and new values are not comparable. Plan the corpus re-scoring sweep
(`GeeqCli`, mode `all`) and the fact that every re-score writes a `GeeqEvent`,
which bumps `curationDetails.lastUpdated` on every experiment it touches.

## Open questions for Paul

* Is "bad" allowed to move the public **quality score**, or is it a separate flag
  (like `troubled`) that routes to a review? Paul's phrasing — *"something
  possibly wrong"* — reads like a flag.
* 20% of **what**: the whole corpus, or incoming experiments only?
* Does suitability become a **gate** on import (don't import below X), or stay
  advisory for prioritizing curator effort?

## References

* `gemma-core/.../persistence/service/expression/experiment/GeeqServiceImpl.java`,
  `.../model/expression/experiment/Geeq.java`, `GeeqValueObject.java`,
  `GeeqAdminValueObject.java`, `gemma-cli/.../apps/GeeqCli.java`,
  `gemma-rest/.../DatasetsWebService.java` (the `/geeq*` paths)
* `docs/design/GEMMA_CURATION_FEATURE_WISHLIST.md` §A9 and
  `gemma-curation-agents/TODO-gemma-api-2.md` §3 asked for the GEEQ subscore
  read, the manual-override write and a recalculate trigger — **all three have
  since landed**; both docs still describe them as gaps. The plumbing is not
  what's missing here, the calibration is.
