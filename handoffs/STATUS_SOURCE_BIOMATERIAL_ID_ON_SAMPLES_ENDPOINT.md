# STATUS: `sourceBioMaterialId` on `/datasets/{id}/samples` — reply to HANDOFF

**Filed:** 2026-05-23, in response to `handoffs/HANDOFF_SOURCE_BIOMATERIAL_ID_ON_SAMPLES_ENDPOINT.md`.
**Investigator:** Paul Pavlidis (via gemma-rest Claude).

---

## TL;DR

The REST endpoint is fine. The DB linkage is missing **systemically** for single-cell datasets — not just GSE237718. **Production gemd has 523 single-cell EEs and ZERO of them carry any `SOURCE_BIO_MATERIAL_FK` linkage on their BioMaterials.** The curator-UI grouping codepath has no real prod data to exercise against until the single-cell ingestion path starts creating the linkage.

## What I checked

### (1) REST endpoint shape — confirmed correct

`GET /datasets/{id}/samples` returns `BioAssayValueObject[]`. Each item has `.sample` populated as `BioMaterialValueObject`. The VO's `sourceBioMaterialId` field is wired:

- `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:983-1031` — `/{dataset}/samples` resolves via `datasetArgService.getSamples(...)` → returns `BioAssayValueObject` page.
- `gemma-core/src/main/java/ubic/gemma/model/expression/bioAssay/BioAssayValueObject.java:51,158` — `private BioMaterialValueObject sample`, constructed at line 158 from `bioAssay.getSampleUsed()`.
- `gemma-core/src/main/java/ubic/gemma/model/expression/biomaterial/BioMaterialValueObject.java:68,199` — `private Long sourceBioMaterialId`, populated `bm.getSourceBioMaterial() != null ? .getId() : null`.

So **if the entity has it, the response carries it.** The importer's `item.sample.sourceBioMaterialId` lift in `gemma_curation_agents/local_api/import_from_gemma.py:293-327` reads the correct path.

### (2) GSE237718 prod data — confirmed empty

```sql
SELECT i.SHORT_NAME, COUNT(*) AS total_bm,
       SUM(bm.SOURCE_BIO_MATERIAL_FK IS NULL)     AS null_src,
       SUM(bm.SOURCE_BIO_MATERIAL_FK IS NOT NULL) AS with_src
FROM INVESTIGATION i
JOIN BIO_ASSAY ba ON ba.EXPRESSION_EXPERIMENT_FK = i.ID
JOIN BIO_MATERIAL bm ON ba.SAMPLE_USED_FK = bm.ID
WHERE i.SHORT_NAME='GSE237718'
GROUP BY i.SHORT_NAME;
```

| SHORT_NAME | total_bm | null_src | with_src |
|---|---|---|---|
| GSE237718 | 56 | **56** | **0** |

Matches the importer's `0/56` output exactly.

### (3) Systemic check — single-cell prod posture

```sql
SELECT COUNT(DISTINCT EXPRESSION_EXPERIMENT_FK) FROM SINGLE_CELL_DIMENSION_EXPERIMENT;
-- → 523 single-cell EEs in prod

SELECT COUNT(DISTINCT i.ID) FROM INVESTIGATION i
JOIN SINGLE_CELL_DIMENSION_EXPERIMENT scde ON scde.EXPRESSION_EXPERIMENT_FK = i.ID
JOIN BIO_ASSAY ba ON ba.EXPRESSION_EXPERIMENT_FK = i.ID
JOIN BIO_MATERIAL bm ON ba.SAMPLE_USED_FK = bm.ID
WHERE bm.SOURCE_BIO_MATERIAL_FK IS NOT NULL;
-- → 0
```

**Zero of 523 single-cell EEs carry the source-biomaterial linkage.** Compare against the wider DB: 631 940 / 1 407 081 BioMaterials (45 %) DO have `SOURCE_BIO_MATERIAL_FK` set, spanning 7 047 distinct source-biomaterial groupings. The data model + the field both work; the single-cell ingestion path just isn't populating it.

## Recommendations

### For the curator-UI single-cell codepath testing

Pick one of:

- **(a) Manual backfill in `local_curation.sqlite`** for one dataset (curator groups GSE237718's 56 cell-buckets into N source samples by hand). Workaround documented in the original handoff. Lets the `SampleDetailsPanel.tsx` `groupedRows` / `aggregateFvId` codepath exercise.
- **(b) `--fake-singlecell-grouping` flag on the importer** that synthesises a deterministic `source_biomaterial_id` mapping by sample-name prefix or similar. Testing-only; should print a loud banner so nobody confuses it with real data.
- **(c) Wait for the upstream fix** below to land + a real single-cell dataset to get re-ingested with the linkage populated.

(a) is fastest for unblocking the UI today. (b) is more reusable. (c) is the only durable answer.

### For the upstream fix

The single-cell ingestion path (gemma-cli's `SingleCellDataLoaderCli` + the Java loaders under `ubic.gemma.core.loader.expression.singleCell`) is not setting `BioMaterial.sourceBioMaterial` when it creates per-cell-bucket BioMaterials. This is the load-side gap — the cell-bucket BioMaterials should reference the **original sample** (GSM-level BioMaterial that maps to the FASTQ submission) via `sourceBioMaterial`.

Without that linkage:
- Aggregation views in `SampleDetailsPanel` can't collapse N cell-buckets → source sample.
- Curators editing a factor value on one cell-bucket can't "fan to siblings" because the sibling set is implicit (would have to be reconstructed from naming conventions per dataset).
- The Gemma audit log can't attribute curation edits to a logical source sample.

Fix shape (recce, not a commit):

1. In the SC ingestion path, when creating cell-bucket BioMaterials, locate the original GSM-level BioMaterial that corresponds to the sample the cell came from. Should already be in the EE's BioAssay set (it's the BA's `sampleUsed`).
2. Set `cellBucketBM.setSourceBioMaterial(gsmBM)` before persist.
3. Audit-event the change so the linkage is traceable.

This is gemma-core + gemma-cli work, not gemma-rest. Separate handoff if Paul wants me to file it.

## What's NOT needed

- **No gemma-rest code change.** The endpoint already serialises the field.
- **No curator-UI code change** on the import-side. The importer is reading the correct path.
- **No DB schema change.** `SOURCE_BIO_MATERIAL_FK` already exists, is populated for 45 % of BioMaterials, just isn't set on the single-cell ones.

## Cross-references

- `handoffs/HANDOFF_SOURCE_BIOMATERIAL_ID_ON_SAMPLES_ENDPOINT.md` — the original ask.
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/` — the loaders that need to set `sourceBioMaterial`.
- `gemma-curation-agents/gemma_curation_agents/local_api/import_from_gemma.py:293-327` — importer lift; correct shape; just receives 0/56 because the data isn't there to lift.
- Memory `reference_production_database.md` — port-forward + creds used for the empirical check above.
