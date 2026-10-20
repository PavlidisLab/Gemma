# DWR → REST parity gap audit

**Date:** 2026-07-05
**Trigger:** publications had no write route; checked *all* retired gemma-web DWR
controller methods for REST counterparts to catch other gaps.
**Method:** enumerated every DWR-exposed method (232 across 38 controllers) from
`bb154eee88^:gemma-web/.../dwr/interface/*.js`, read each Java controller for
semantics, matched against the current `gemma-rest/.../*.java` surface by behaviour
(not name). Five parallel audit passes.

Scope note: pure UI plumbing (ExtJS grid paging, progress polling, task-completion
callbacks, HTML-fragment returns, picker autocomplete, session-only working sets) is
**not** counted as a gap — it has no place in REST by design. Coexpression
(`LinkAnalysis`, `CoexpressionSearch`, coexp visualization) is a **retired subsystem**
(removed Phase 1c), not a regression.

---

## Gaps by severity

### Being addressed now
- **Dataset publications write** — `updatePubMed` / `removePrimaryPublication`. Closed by
  `PUT /datasets/{dataset}/publications` (PubMed IDs landed; DOI/CrossRef pending). See the
  2026-07-05 CAB/UIB handoffs.

### Real gaps — curation writes with no REST home
- **Gene-set CRUD + retrieval** — create/update/updateMembers/updateNameDesc/remove of
  DB-backed gene sets; load-by-id; list-members; "my gene sets"; findGeneSetsByGene. No
  REST. (`/search` only loosely covers name search; `GroupsWebService` is *user* groups.)
- **Experiment-set (dataset group) CRUD + retrieval** — same story: create/update/
  updateMembers/remove, loadAll, load(id), getExperimentsInSet. Zero REST home.
- **Group-based ACL sharing (any entity)** — `makeGroupReadable/Writeable` +
  `removeGroupReadable/Writeable`, and the `updatePermission(s)` group reader/writer lists.
  `PUT /datasets/{id}/permissions` only toggles public/private (datasets only). No way over
  REST to grant/revoke a *group's* read/write on a securable. **Biggest security-side gap.**
- **BibliographicReference surface** — `load`, `loadFromPubmedID`, `search`, `update`
  (PubMed refresh). No `/bibrefs` REST at all (partially eased by the new publications route,
  but the general bibref browse/search/refresh is still absent).
- **Dataset delete** — `deleteById`. No `DELETE /datasets/{dataset}` (only sub-resources).
- **Dataset name/description edit** — `updateBasics`. Only `PUT /datasets/{id}/short-name`
  exists; name and description are not editable over REST.
- **Diff-ex meta-analysis (whole controller)** — analyze/save/remove/load meta-analyses. None.
- **Platform (ArrayDesign) mutations** — updateReport, addAlternateName, remove, repeat-scan.
  `PlatformsWebService` is read-only.
- **GEO record curation** — `GeoRecordBrowser.browse` (paged/free-text) + `toggleUsability`.
- **Flat-file EE upload** — `ExpressionDataFileUpload.validate/load`. `/datasets/import` is
  GEO/AE accession-only.
- **Generic characteristic lookup/browse** — `CharacteristicBrowser.findCharacteristicsCustom`
  ("where is this term used" across EE/BM/FV) + global `browse`. `/annotations/search` is
  ontology typeahead, not a scan of persisted Characteristic rows.
- **`unmatchAllBioAssays`** — split shared BioMaterials so each BioAssay gets its own.
- **Generic `addAuditEvent(type, comment, detail)`** — REST writes only specific event types.

### Minor / borderline
- Bulk report regeneration (`runAll`), diff-ex `refreshStats` / `determineAnalysisType`,
  `getDEDVForVisualizationByProbe` (probe-keyed vectors), `TwoChannelMissingValue.run`
  (legacy two-colour), Hibernate stats enable/disable, task email-alert, ArrayDesign/GeneSet/
  EE-set ACL+audit (wired for EEs only), `loadExpressionExperimentsWithQcIssues`,
  `getBioMaterialCharacteristicCategories`.

### Cleanly covered (no action)
Experimental-design edits (subsumed by the declarative `PUT /datasets/{id}/design`),
sample outlier + characteristic writes, all analysis triggers (preprocess/diagnostics/svd/
batchInfo/differential run·redo·remove), GEO-accession import, expression/metadata/diffex
data-file downloads, gene detail + GO + probes, global `/search`, cache/hibernate/reindex
admin ops, user & group CRUD, task status/cancel.

---

*Full per-method tables (all 232 methods) were produced during the audit; this doc keeps
the actionable gap list. Regenerate from `bb154eee88^` if the per-method detail is needed.*
