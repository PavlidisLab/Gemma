# Data-quality metrics and how to evaluate them — project proposal

**Status: PROPOSAL. Nothing built, nothing scheduled.** Written 2026-08-20 from a
conversation with Paul; he intends to pick it up another time. This is the
quality half only — the suitability half of GEEQ was removed in `dd56e791d7` and
is not coming back.

## 1. The question

Not "is this dataset good", which has no referent, but:

> **Can we predict, before or without looking at the results, that a dataset will
> give untrustworthy differential-expression results?**

That version is answerable because it names an outcome. Every metric below is a
candidate predictor, and the substance of the project is §3 — deciding what to
predict *against*. Gathering the statistics is interesting on its own even if no
metric ends up in a score.

The existing quality subscores were never evaluated this way. They are step
functions over chosen cutoffs, and two of them (`qScoreSampleMeanCorrelation`,
`qScoreSampleCorrelationVariance`) carry weight 0 — computed, stored, published,
and contributing nothing, which is what "tricky to use with confidence" looks
like after a few years.

## 2. Candidate metrics

### M1. Ribosomal-protein coexpression

RP genes are highly expressed and highly coexpressed. If that structure is absent
in an experiment's own expression matrix, something is wrong with the data.

* **Computation**: within-experiment gene×gene correlation across the RP set
  (RPL*/RPS* symbols, or the GO cytosolic-ribosome set), summarized as mean
  pairwise correlation.
* **Reference**: compare against random gene sets **matched on expression level**,
  otherwise "highly expressed genes correlate" is scored as signal.
* **Not** from the coexpression subsystem — that is retired (Java DAOs gone, the
  four `*_GENE_COEXPRESSION` tables dormant orphans, drop drafted in
  `db.1.34.0_drop_coexpression.sql`). This is a fresh pass over the processed
  matrix, and the right object anyway: within-experiment, not the cross-corpus
  network.
* **Known reasons for a low value that are not quality problems**: ribo-depletion
  vs polyA selection, pseudobulked single-cell, degraded RNA, very small n,
  extreme tissue composition. These travel with the metric as context; the output
  is a flag with an explanation, not a number on its own.

### M2. Sex as a positive control

Sex is **annotated** in Gemma (`Categories.BIOLOGICAL_SEX`, PATO_0000047), so it
is never inferred from expression — see the circularity note in §4.

Two distinct readings, which must not be conflated:

* **M2a — label consistency.** Does XIST / RPS4Y1 / DDX3Y expression agree with
  the annotated sex, per sample? Disagreement means a sample swap or a
  mislabelling, and is directly actionable regardless of any score.
* **M2b — dynamic range.** *How big* is the XIST difference between annotated
  males and females. It should be enormous. If XIST separates the groups by ~2×,
  or is well detected in males, that indicates compressed dynamic range, mixed or
  contaminated samples, or a platform that barely measures it.

**Preconditions**: both sexes annotated in the experiment, and the genes actually
measured on the platform — not guaranteed on older arrays. Applicability has to
be reported, because "no signal" and "not measurable here" are different answers.

### M3. Sample-to-sample correlation, standardized

The cormat is already computed and persisted
(`SampleCoexpressionAnalysisService.loadBestMatrix`), so this leg costs no new
data pull. The problem is interpretation: mean and median correlation are not
comparable across experiments. A cell-line panel is trivially high; a
heterogeneous tissue cohort trivially lower; platform and normalization shift the
whole distribution. Read raw, the metric reports biology and calls it quality.

**Proposal**: standardize each experiment against matched peers (platform class,
taxon, sample-count band) and evaluate whether the standardized value predicts the
outcome. Keep the raw value too — the comparison of raw vs standardized is itself
a result worth having.

### M4. What already exists

Outliers, batch info / effect / confound, and replicates-per-condition
(`GeeqServiceImpl.leastReplicates`). These enter the evaluation on the same
footing as the new candidates rather than being assumed correct. Replicates per
condition is the one existing feature that reads the design rather than the
technology, and is the strongest prior.

## 3. The hard part: what to evaluate against

No option here is clean. The recommendation is to use three in combination and be
explicit about what each one can and cannot support.

| Option | What it gives | Why it is not enough alone |
|---|---|---|
| **O1. P-value distribution shape** — `PvalueDistribution` (`numBins`, `binCounts`) is stored per result set, corpus-wide, already computed | Corpus-scale screening at zero marginal cost | **A flat histogram is not a failure.** A dataset with no real effects *should* give a uniform distribution. Only pathological shapes diagnose anything: a spike near 1, bimodality, a mid-range hump, sawtooth from discretized p-values. Restrict O1 to those. |
| **O2. Deliberate degradation** — take clean datasets, permute labels, inject batch structure, swap samples, subsample | Ground truth by construction, unlimited n, no scarcity | Synthetic damage may not resemble real failure modes. Measures sensitivity, not prevalence. |
| **O3. Sex-contrast recovery** — for experiments with both sexes annotated, does a DE analysis on sex recover the Y genes and XIST | A real positive control with known biology, from real data | Scarce (needs both sexes annotated + genes on platform), and **conflicts with M2** — see §4. |
| **O4. Cross-experiment reproducibility** — concordance of DE signatures between independent experiments of the same contrast | The strongest possible evidence | Matching contrasts across experiments is the hard curation problem itself, and disagreement is often biology. Aspirational. |
| **O5. Curator judgment** — `troubled` / `needsAttention`, GEEQ manual overrides, agent audit findings | Real human labels | Sparse, inconsistent, and partly a record of who looked rather than what is wrong. Useful as a check on the others, not as the target. |

**Recommended combination**: O2 for sensitivity ("does the metric detect damage we
know we caused"), O3 for a real-data positive control, O1 restricted to
pathological shapes for corpus-scale screening. O5 as a sanity check. O4 parked.

## 4. Two circularity traps

1. **Do not infer sex from expression.** Sex is annotated; inferring it from the
   very genes used to check the labels makes M2a vacuous.
2. **M2b and O3 are the same measurement.** If sex-gene dynamic range is a
   predictor *and* sex-contrast recovery is the outcome, the evaluation is
   scoring a metric against itself. Pick one role for the sex contrast per
   analysis and say which. The cleanest split: use **O3 as the outcome** and drop
   M2b from the predictor set for that analysis, keeping M2a (label consistency),
   which is a different quantity.

## 5. Statistical design

* **Stratify.** Every metric here is confounded by platform technology, taxon,
  sample size, and RNA-seq vs microarray. Corpus-wide AUC without stratification
  will mostly measure the corpus composition.
* **Hold out.** Thresholds chosen on the same experiments used to evaluate them
  will look excellent and generalize badly. Split before looking.
* **Expect the metrics to be correlated** with each other and with n. Report
  marginal contribution, not just individual AUC — a metric that adds nothing over
  sample size is not a quality metric.
* **The base rate is unknown.** The working prior is that ~20% of experiments have
  something wrong, possibly more. That number is an output of this work, not an
  input to it.
* **Report applicability separately from score.** For M1 and M2 a large fraction
  of the corpus will be out of scope; a metric that only applies to a third of
  experiments can still be worth having, but the denominator has to be visible.

## 6. What Gemma has, and what is new

**Has**: processed vectors in the database, probe→gene mappings, sex annotations
as characteristics, persisted sample cormats, `PvalueDistribution` per result set,
DEA result-set summaries (`numberOfDiffExpressedProbes`, `upregulatedCount`), and
in GEEQ a working pattern for storing per-experiment computed scores with an audit
event.

**New**: a pass over each experiment's expression matrix computing M1 and M2, and
somewhere to put the results. Cost is dominated by vector retrieval, which is the
top item on the perf-hotspot list — so this is a batch job (CLI, like `GeeqCli`)
that stores what it computes, not something evaluated per request.

## 7. Staging

* **S1 — measure.** A few hundred experiments with existing DEA results. Compute
  M1, M2a/M2b where applicable, M3 raw and standardized. Record applicability.
* **S2 — assemble outcomes.** O1 shape classification over the same experiments;
  O3 where the sex contrast exists; O2 degradation series on a clean subset.
* **S3 — evaluate.** Stratified, held out, marginal contribution over n.
* **S4 — decide.** Only then, whether anything enters GEEQ, and with what weight.
  A metric that survives S3 and stays out of the score is still a useful flag.

## 8. Open questions

* Should these become GEEQ subscores at all, or a separate diagnostic surface that
  reports findings with reasons? A flag with an explanation survives the
  "there might be reasons" problem better than a number does.
* How many experiments in the corpus actually have both sexes annotated **and** the
  sex genes on the platform? That count decides whether O3 is a real option or a
  footnote. It is one query and worth running first.
* `qScorePlatformsTech` (two-colour) — **settled 2026-08-20, leave it alone.** Paul:
  two-colour data is heading for not being used for anything, or only as a last
  resort, and the GEEQ score should be dinged for it precisely so the score keeps
  reminding us to avoid those experiments. The −1 is the reminder, and it is
  deliberate. Do not re-weight it, do not make it a 0-when-inapplicable, and do not
  move it to a filter — a DEA-suitability event (`isSuitableForDEA`) would hide the
  fact in a flag nobody reads, where a visibly lower quality score steers effort by
  itself. The consequence, accepted: every experiment that is not two-colour keeps
  scoring +1 on this feature.

## 9. References

* `GeeqServiceImpl` (quality scoring, `leastReplicates`), `Geeq`, `GeeqCli`
* `SampleCoexpressionAnalysisService.loadBestMatrix` — persisted cormats
* `PvalueDistribution`, `DiffExResultSetSummaryValueObject`
* `Categories.BIOLOGICAL_SEX` (PATO_0000047)
* `docs/recce/COEXPRESSION_ORPHAN_RECCE.md` — why M1 is computed fresh
