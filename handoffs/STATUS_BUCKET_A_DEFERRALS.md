# Status — Bucket A deferrals (2026-05-22)

Two of the three failures in the misc-residuals batch fall in
`FAILSAFE_RESIDUAL_TRIAGE.md` Bucket A ("Deep Hibernate-6 merge /
matrix-assembly cascade"). The triage doc explicitly says **do NOT
chase test-side**; the production behavior needs investigation
upstream. Both are `@Tag("slow")` so they do NOT fire on default
`mvn verify`. Documenting here and deferring.

## Failure 2 — `TwoChannelExpressionDataDoubleMatrixTest.testMatrixConversion`

Reported symptom (this batch):
```
IllegalArgumentException: ExpressionExperiment already has processed
vectors, remove them before creating new ones or use
replaceProcessedDataVectors().
  at ExpressionExperimentDaoImpl.createProcessedDataVectors:4708
  called from ExpressionExperimentDataVectorServiceImpl.createProcessedDataVectors:187
  called from ProcessedExpressionDataVectorCreationHelperServiceImpl:149
  called from ProcessedExpressionDataVectorServiceImpl.createProcessedDataVectors:94
  called from TwoChannelExpressionDataDoubleMatrixTest.testMatrixConversion:322
```

Bucket A symptom for the same test:
`IllegalState: No dimensions to setup columns from` from
`AbstractMultiAssayExpressionDataMatrix.java:429`.

Different symptoms, same root cause family. The strict Assert at
`ExpressionExperimentDaoImpl:4708`
(`ee.getProcessedExpressionDataVectors().isEmpty()`) was added in
`2490d715d0` (2024-05-10, "Improve deletion of raw and processed
vectors"). The helper (`ProcessedExpressionDataVectorCreationHelperServiceImpl:59`)
calls `expressionExperimentService.removeProcessedDataVectors( ee )`
BEFORE building new vectors, so the assertion ought to pass in a
correctly-flushed session. It doesn't here, which mirrors the same
HB6 PersistentSet snapshot drift that Bucket A documents for sibling
tests in this family (`BaselineDetectionTest`, `DiffExTest`,
`SplitExperimentTest`, etc).

**Test is `@Tag("slow")` + `@NetworkAvailable(url=EntrezUtils.ESEARCH)`**,
so it's already off the default mvn-verify path.

**Fix surface:** upstream — investigate why the helper-level
`removeProcessedDataVectors` does not leave the EE in-memory bag empty
by the time control returns to the same transactional method's later
`createProcessedDataVectors` call. Candidate areas:
- HB6 cascade behaviour through the inverse="true" mapping
  (called out in `2490d715d0`'s commit message)
- `ensureEeInSession` interplay across the remove → create flow
- Whether the `removeProcessedDataVectors` service-method's
  `@Transactional` propagation flushes the bag before the helper's
  outer transaction resumes

Out of scope for this batch. Per brief: "If you can't diagnose
statically with high confidence, document in handoffs/ and report
back."

## Failure 3 — `SampleCoexpressionAnalysisServiceTest.test`

Reported symptom: `CannotCreateTransactionException: Could not open
Hibernate Session for transaction`.

Bucket A symptom: `IllegalState: No dimensions to setup columns from`
from `AbstractMultiAssayExpressionDataMatrix.java:429`.

Test is `@Tag("slow")` — off the default mvn-verify path. The shift
to `CannotCreateTransactionException` between the triage snapshot
and the current run suggests the failure is now happening at a
different point (test setup or context init) — but since the test
is gated `slow`, this is not a default-run regression.

**Fix surface:** infrastructure-level (Hikari pool / SessionFactory
lifecycle). Possible cause is a leak from one of the production
fixes landed in this batch that destabilises connection-pool state
for a later `@Tag("slow")` opt-in run. Diagnosis requires running
the slow tier (`mvn verify -DexcludedGroups=network`) and walking
the failure forward — not something to chase statically.

Out of scope for this batch. Defer with the rest of Bucket A.
