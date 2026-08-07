# Experiment deletion under dual-version operation — incident summary + remediation plan

**Date:** 2026-08-04 · **Trigger:** `deleteExperiments -e GSE277430` failing on both branches
**Database:** `gemd` on `prod-db.msl.ubc.ca` = `homer.msl.ubc.ca` (Percona 5.7.44, `BINLOG_FORMAT=STATEMENT`, no triggers)
**Branches in scope:** `phase2-acl-migrate` (Gemma 2.0) and `hotfix-1.32.x` / `fix/scde-reverse-compat`

Both the deployed 1.32.x and phase2 run against this one database at the same time. Every defect below
is a consequence of that arrangement, in one direction or the other.

---

## 1. TL;DR

| # | Defect | Direction | Status |
|---|---|---|---|
| 1 | `SINGLE_CELL_DIMENSION_EXPERIMENT` FKs are `RESTRICT` and invisible to 1.32.x → old code cannot delete any SC experiment, QT or dimension | 1.32.x deletes | **Open on prod.** Fix written (`V23`, branch `fix/scde-reverse-compat`, commit `9e7add1106`), never applied |
| 2 | `removeAllSingleCellDataVectors` loaded every SC vector + blob to collect QTs → OOM | phase2 deletes | **Fixed** — `c4f1357ba6` |
| 3 | prettytime-nlp's shaded SLF4J 1.x shadowed `slf4j-api` 2.x → NOP logger, all Gemma logging and stack traces discarded | diagnosis | **Fixed** — `d855244de7` |
| 4 | Retry advice catches `OutOfMemoryError` and retries on a desynced connection | phase2 | **Open** |
| 5 | phase2 deletion does not remove the experiment's ACL from either ACL store | phase2 deletes | **Open** — 115 rows cleaned by hand for GSE277430 |
| 6 | 1.32.x cannot maintain phase2's canonical ACL tables; the two stores drift on create, delete and permission change | both | **Open, structural** |
| 7 | `TICKET_TARGET.TARGET_ID` references experiments with no FK → 1.32.x deletes leave dangling ticket targets | 1.32.x deletes | **Open** (not triggered by GSE277430 — it had no ticket) |
| 8 | phase2 finds single-cell dimensions via the `SINGLE_CELL_DIMENSION_EXPERIMENT` cache, which 1.32.x does not maintain → 17 experiments cannot be deleted on phase2 (fails loudly on the bio-assay FK) | phase2 deletes | **Open** — see §4.1 |

Deleting experiment *data* can be made reliable with #1 and #5. ACL consistency across the two
versions cannot be fixed by deletion changes alone — see §6.

---

## 2. What happened

**Attempt 1 — prod 1.32.x.** `deleteExperiments -e GSE277430` removed the SC vectors and the
SingleCellDimension, then failed deleting the QuantitationType on `FK_SCDE_QUANTITATION_TYPE` and
rolled the whole transaction back. `SINGLE_CELL_DIMENSION_EXPERIMENT` (mysql `V7`) is a phase2-only
link table with three plain `RESTRICT` FKs; 1.32.x has no mapping for it, so it can neither see nor
clear the link rows. This is defect #1 and is what `fix/scde-reverse-compat` was written to solve.

**Attempt 2 — phase2.** Expected to succeed, because the phase2 DAO clears the link rows itself via
`removeByEE`. It failed after ~34 s with:

```
WARN  o.h.e.j.s.SqlExceptionHelper | SQL Error: 0, SQLState: S1000
ERROR o.h.e.j.s.SqlExceptionHelper | arraycopy: last source index 16 out of bounds for byte[14]
```

and nothing else — no stack trace, no Gemma log lines at all.

**Attempt 3 — phase2, 200 GB heap.** `OutOfMemoryError`.

**Attempt 4 — phase2 with `c4f1357ba6`, default heap.** Succeeded.

---

## 3. The failure chain

The `arraycopy` message was three layers removed from the cause:

1. `removeAllSingleCellDataVectors` streamed `ee.getSingleCellExpressionDataVectors()` to collect the
   distinct QTs, forcing the lazy collection to initialize.
2. That emits a select over `SINGLE_CELL_EXPRESSION_DATA_VECTOR` carrying `DATA` + `DATA_INDICES`
   **and** `SINGLE_CELL_DIMENSION.CELL_IDS`, joined through
   `BIO_ASSAYS2SINGLE_CELL_DIMENSIONS`. For GSE277430 the fan-out is 25,050 vectors × 23 bio assays =
   **576,150 rows**, each repeating the 1,372,836-byte `CELL_IDS` blob ≈ **790 GB** on the wire. No
   JDBC fetch size is set, so Connector/J buffers the entire result set client-side.
3. The OOM landed inside `SimplePacketReader.readMessageLocal`, aborting mid-resultset and leaving the
   connection's protocol stream **desynced**.
4. Spring's rollback then issued `rollback` as a plain text query; the reply was parsed against the
   stale buffer → `ArrayIndexOutOfBoundsException`, which Connector/J's
   `SQLExceptionsMapping.translateException` wraps as `SQLState S1000, vendor code 0` (its fallback for
   any unrecognized `Throwable`).
5. Spring reported `Application exception overridden by rollback exception`, **discarding the OOM**.
6. The retry advice (`RetryLogger`: `Retry attempt #1 failed`) re-ran the delete on the broken
   connection, generating further noise.

Two independent lessons: an `S1000` / vendor-code-`0` MySQL error is always a client-side fault, never
a server-reported one; and a bounds error out of the driver's packet reader means a truncated or
misaligned buffer, so look for what aborted the previous read.

**Why no stack trace appeared** (defect #3): `prettytime-nlp` 5.0.6 is an uber-jar bundling a shaded
SLF4J **1.x** `org/slf4j/LoggerFactory`. Maven orders direct dependencies before transitives, so
prettytime-nlp sat at classpath entry 110 and `slf4j-api` 2.0.16 at 141. The 1.x factory won, looked
for `org.slf4j.impl.StaticLoggerBinder`, didn't find it (the binding is `log4j-slf4j2-impl`) and
installed a **NOP logger** — silently discarding every `log.info`/`log.error` in Gemma, including
`AbstractCLI`'s `"<command> failed:"` stack trace. Hibernate's lines survived because Hibernate 6 logs
through JBoss Logging straight to log4j2. The pre-existing mitigation (`gemma-cli/pom.xml`, seeding
`slf4j-api` into `appassembler/contrib`) only covers the packaged launcher, never IDE runs or
`exec:java`.

---

## 4. Verified prod facts (read-only, 2026-08-04)

GSE277430 = `INVESTIGATION.ID 88007`: 23 samples, 333,570 cells, 25,050 SC vectors, one
`SINGLE_CELL_DIMENSION` (id 1191, `CELL_IDS` 1,372,836 bytes). Individual vector blobs are small
(1–7 KB sampled); the fan-out, not row size, is what killed it.

Post-deletion, all zero: `INVESTIGATION` 88007, `SINGLE_CELL_DIMENSION_EXPERIMENT` link rows,
`SINGLE_CELL_DIMENSION` 1191, SC vectors by EE and by dimension,
`BIO_ASSAYS2SINGLE_CELL_DIMENSIONS`, `BIO_ASSAY`, `CURATION_DETAILS` 15180981, raw/processed vectors,
`EXPRESSION_EXPERIMENT2CHARACTERISTIC`.

**Reverse-compatibility surface.** 22 FKs reference `INVESTIGATION` — 19 `RESTRICT`, 3 `CASCADE`
(`ANNOTATION_SET`, `EXPRESSION_EXPERIMENT2ARRAY_DESIGN`, `EXPRESSION_EXPERIMENT2CHARACTERISTIC`).
FK-enforced references cannot leak: InnoDB would have rejected the parent delete. **Leaks are only
possible through soft (non-FK) references**, of which there are exactly four kinds:

| Soft reference | For EE 88007 |
|---|---|
| `ACLOBJECTIDENTITY.OBJECT_ID` (legacy store) | **leaked** — 1 identity + 2 entries + 56 children |
| `acl_object_identity.object_id_identity` (canonical store) | **leaked** — 1 identity + 2 entries + 53 children |
| `TICKET_TARGET.TARGET_ID` (`TARGET_TYPE='EXPRESSION_EXPERIMENT'`, no FK) | 0 — dodged, no ticket |
| `{HUMAN,MOUSE,RAT,OTHER,USER}_PROBE_CO_EXPRESSION.EXPRESSION_EXPERIMENT_FK` (named `_FK`, no constraint) | 0 — all five tables empty |

The 56 ACL children were the experiment's own graph: 23 BioMaterials, 23 BioAssays, 7 FactorValues,
2 ExperimentalFactors, 1 ExperimentalDesign. None of their targets still existed and there were no
grandchildren, so removal was safe. 115 rows deleted in one transaction; verified from a separate
connection; shared SIDs (1, 3, owner 715) untouched.

**Dual ACL stores.** `sql/migrations/db.1.33.0.sql` copied gsec's legacy ACL schema to Spring's
canonical four-table schema, run on prod **2026-05-18** (6,598,028 source ACEs → 3,470,753 rows),
deliberately keeping the old tables "as a safety net until the application has run successfully
against the new tables for a release". There is **no ongoing sync** (prod has zero triggers). Each
version reads and writes only its own store:

- 1.32.x → `ACLOBJECTIDENTITY` / `ACLENTRY` / `ACLSID`
- phase2 → `acl_object_identity` / `acl_entry` / `acl_class` / `acl_sid` (`AclObjectIdentity` is
  `@Table(name = "acl_object_identity")`, via stock `BasicLookupStrategy` + `JdbcMutableAclService`)

Measured drift after ~2.5 months:

| | |
|---|---|
| Experiments with a legacy ACL but **no** phase2 ACL | **75**, including the 8 newest (`GSE306925`, `GSE315736`, …) |
| phase2 ACL identities for experiments that no longer exist | **44** |
| Max identity id — legacy vs phase2 | 6,932,038 vs 6,811,525 |

**The drift is active, not a fixed backlog.** Re-measured 2026-08-05, after deleting GSE306819 and
cleaning its ACLs by hand: legacy orphans **0**, canonical orphans **46**. Two new orphans appeared in
~14 hours, and neither is ours (zero canonical identities remain for object 89293). The direction
identifies the source: a phase2 deletion leaks *both* stores, so a deletion that cleaned legacy but
not canonical can only have come from 1.32.x. Any reconciliation must therefore handle the continuous
case, not just sweep a known backlog.

---

### 4.1 Single-cell components — orphan audit (2026-08-05)

Prompted by the concern that single-cell components hang off an experiment more loosely than the rest.
They do — but the weak edge is backstopped, and nothing is orphaned today.

**Everything below a dimension is FK-enforced `RESTRICT`**, so it cannot be orphaned; the dimension
delete would fail first:

| Child | → parent | Rule |
|---|---|---|
| `SINGLE_CELL_EXPRESSION_DATA_VECTOR.SINGLE_CELL_DIMENSION_FK` | `SINGLE_CELL_DIMENSION` | RESTRICT |
| `CELL_LEVEL_CHARACTERISTICS.SINGLE_CELL_DIMENSION_FK` | `SINGLE_CELL_DIMENSION` | RESTRICT |
| `CHARACTERISTIC.CELL_LEVEL_CHARACTERISTICS_FK` | `CELL_LEVEL_CHARACTERISTICS` | RESTRICT |
| `BIO_ASSAYS2SINGLE_CELL_DIMENSIONS.SINGLE_CELL_DIMENSIONS_FK` | `SINGLE_CELL_DIMENSION` | RESTRICT |
| `BIO_ASSAYS2SINGLE_CELL_DIMENSIONS.BIO_ASSAYS_FK` | `BIO_ASSAY` | RESTRICT |
| `ANALYSIS.SINGLE_CELL_DIMENSION_FK` | `SINGLE_CELL_DIMENSION` | RESTRICT |
| `SINGLE_CELL_DIMENSION_EXPERIMENT.{EE,QT,SCD}_FK` | resp. | RESTRICT |

`CellTypeAssignment` and `GenericCellLevelCharacteristics` share the `CELL_LEVEL_CHARACTERISTICS`
table, and cell-type `Characteristic` rows hang off it by FK, so both are covered by the above.

**The genuinely weak edge is dimension → experiment: `SINGLE_CELL_DIMENSION` has no FK to
`INVESTIGATION` at all.** A dimension is reachable from its experiment only through the vectors or
through the `SINGLE_CELL_DIMENSION_EXPERIMENT` link table. Measured database-wide:

| | |
|---|---|
| `SINGLE_CELL_DIMENSION` rows | 543 |
| … with no link row | **17** |
| … with no vectors | 0 |
| … orphaned (no link row, no vectors, no analysis) | **0** |
| … with no bio-assay links | 0 |

So no dimension has ever been orphaned, and none can be silently orphaned in future: every dimension
holds bio-assay links, and `BIO_ASSAYS2SINGLE_CELL_DIMENSIONS.BIO_ASSAYS_FK` is `RESTRICT`. A surviving
dimension therefore blocks the deletion of its experiment's `BIO_ASSAY` rows, converting a would-be
silent orphan into a hard, visible failure. (This is exactly what the H2 fixture for
`removeWithUninitializedSingleCellDataVectors` hit before the link row was recorded:
`BIO_ASSAYS_SC_FKC … FOREIGN KEY(BIO_ASSAYS_FK) REFERENCES BIO_ASSAY`.)

**But the two branches discover dimensions differently, and phase2's source is less reliable:**

- `hotfix-1.32.8` — `getSingleCellDimensions(ee)` derives them from the vectors:
  `select scedv.singleCellDimension from SingleCellExpressionDataVector scedv … group by …`.
  Always correct, never misses one.
- `phase2` (`ExpressionExperimentDaoImpl:2872`, marked *PERF_PROBE_REPORT_ROUND4 B1: dimension lookup
  via link table (was: scan SCEDV)*) — reads `SingleCellDimensionExperiment`. Fast, but only as
  complete as the cache.

The cache is maintained by the phase2 DAO. **Production 1.32.x loads single-cell data without knowing
the link table exists**, so anything it loads is missing from it — the same reverse-compatibility shape
as the ACL stores. Consequence: **17 live experiments currently cannot be deleted by phase2.** Their
dimension is invisible to `getSingleCellDimensions(ee)`, so it is never scheduled for removal, and
`super.remove(ee)` then trips the bio-assay FK and rolls back:

`GSE317144` (91993), `GSE305400` (92041), `GSE325586` (92315), `GSE306263` (92325), `GSE308748`
(92641), `GSE311334` (92642), `GSE314609` (92644), `GSE289589` (92645), `GSE316011` (92647),
`GSE222430` (92648), `GSE315059` (92651), `GSE279550` (92652), `GSE313257` (92653), `GSE300690`
(92655), `GSE291600` (92777), `GSE299894` (92778), `GSE292137.1` (92828)

The same gap affects any other phase2 feature backed by that table (home-page single-cell counts,
`findDimensionByEEAndQt`), not just deletion.

## 5. Remediation — Gemma 2.0 (`phase2-acl-migrate`)

**Done.**

- `c4f1357ba6` — `removeAllSingleCellDataVectors` resolves QTs with a projection query when the
  collection is not already loaded, mirroring `removeAllRawDataVectors`. Statement order is preserved
  so no query runs between `clear()` and the bulk delete (the HB6 autoflush hazard). The collection is
  still initialized once by `super.remove(ee)`'s cascade walk, but that happens *after* the bulk
  delete, so it selects zero rows. Test `removeWithUninitializedSingleCellDataVectors` asserts via
  Hibernate statistics that no vector entity is loaded — verified to fail (`1`, expected `0`) with the
  fix reverted. An emptiness assertion does not discriminate, because the broken path loads the
  collection and then clears it.
- `d855244de7` — `slf4j-api` declared first in `gemma-cli/pom.xml` so it wins classpath order in IDE
  runs and `exec:java`, not just the packaged launcher.
- `649efa8737` — `AbstractCLI` imports `java.util.Collections`, not prettytime-nlp's shaded backport.

**To do.**

1. **Fix ACL removal on delete** (defect #5). phase2 deleted the experiment but left its identity,
   entries and 53–56 inheriting children in *both* stores. Establish why the ACL advice / 
   `JdbcMutableAclService.deleteAcl` path did not fire for `ExpressionExperimentService.remove`, and
   whether the 44 pre-existing canonical-store orphans share the cause or are purely 1.32.x drift
   (§6). Add a regression test that deletes an experiment and asserts no `acl_object_identity` row
   survives for it.
2. **Stop retrying `Error`** (defect #4). The retry advice caught `OutOfMemoryError` and re-ran the
   delete on a connection whose protocol stream was already desynced, which manufactured the
   misleading secondary failure. Exclude `Error` from the retry policy.
3. **Add an FK from `TICKET_TARGET` to `INVESTIGATION`** (defect #7), or teach the delete path to clear
   ticket targets. Its only `TARGET_TYPE` in use is literally `EXPRESSION_EXPERIMENT`, and there is no
   constraint, so a ticketed experiment deleted by *either* version leaves a dangling target. An
   `ON DELETE CASCADE` FK would also make it reverse-compatible for 1.32.x deletes, the same shape as
   the `V23` fix.
4. **Consider bounding the SC vector collection mapping.** The fatal query is reachable from anywhere
   that initializes `ExpressionExperiment.singleCellExpressionDataVectors`; the delete path was one
   caller, not the only one. Worth auditing whether that collection should be mapped at all, or only
   reachable through paged/streamed DAO methods.
5. **Make dimension discovery fall back to the vectors** (§4.1). `getSingleCellDimensions(ee)` trusts
   the `SINGLE_CELL_DIMENSION_EXPERIMENT` cache, which production does not maintain, so 17 experiments
   are currently undeletable on phase2. Fall back to (or union with) 1.32.x's vector-derived query when
   the link table yields nothing — keeping the fast path for the common case. Backfilling the missing
   link rows fixes today's 17 but not tomorrow's, since production keeps loading single-cell data.
6. **Set a JDBC fetch size** or otherwise stop Connector/J buffering unbounded result sets. Today
   `useCursorFetch=true` is set but no fetch size is, so no cursor is used and the entire result set is
   materialized client-side. This is what turned an expensive query into a heap exhaustion.

## 6. Remediation — hotfix branch (1.32.x)

1. **Apply `V23` to prod** (defect #1) — the only blocker for old-code deletes, and a hard failure
   today. `mysql/V23__scde_cascade_on_parent_delete.sql` on `fix/scde-reverse-compat`
   (`9e7add1106`) flips all three `SINGLE_CELL_DIMENSION_EXPERIMENT` FKs to `ON DELETE CASCADE`; the H2
   sister is `V24`. Prod `gemd` has no `flyway_schema_history`, so the two `ALTER`s must be run by hand.
   Confirmed still unapplied: `information_schema` shows `DELETE_RULE = RESTRICT`. Merge the branch so
   the migration ships, then run it on prod.
2. **Nothing else is fixable inside 1.32.x.** The old code cannot maintain phase2's canonical ACL
   tables or `TICKET_TARGET` because it has no knowledge of them. Every 1.32.x deletion will leave an
   orphan in `acl_object_identity`, and every 1.32.x creation will be missing from it. That must be
   absorbed on the phase2 side:
   - a periodic reconciliation job — `AclLinterServiceImpl` already exists in the tree and may be the
     intended tool; or
   - re-run a delta of `db.1.33.0.sql` at cutover so phase2 starts from a fresh snapshot, which is what
     the migration's "safety net until a release" wording implies was the plan; or
   - accept the drift and treat phase2 as non-authoritative for ACLs until 1.32.x is retired.
3. **Do not assume the canonical store is only stale for deletions.** Creates and permission changes
   drift identically — a `MakePublic` in 1.32.x updates only `ACLENTRY`, so phase2 can disagree with
   production about whether a dataset is public. Not quantified; needs a per-object entry-set diff
   between the two stores before phase2's authorization view of prod is trusted.

---

## 7. Runbook — cleaning ACL leftovers after a phase2 deletion

Exercised twice: GSE277430 / EE 88007 / identity 6292626 (115 rows, 2026-08-04) and GSE306819 /
EE 89293 / identity 6324568 (342 rows: 2 entries + 168 children + 1 identity per store, 2026-08-05).
In both cases the data side was clean and only the ACL rows survived. Needed until §5 item 1 lands. Replace `:ee_id` and `:oi_id`; every FK here is `RESTRICT`, so order
matters. Requires a write-capable account (`gemd-ro` cannot do this).

```sql
-- 1. locate the orphan identity, and confirm the experiment is really gone
SELECT o.ID, o.OBJECT_ID, o.OBJECT_CLASS FROM ACLOBJECTIDENTITY o
 WHERE o.OBJECT_ID = :ee_id AND o.OBJECT_CLASS LIKE '%ExpressionExperiment';
SELECT COUNT(*) AS must_be_zero FROM INVESTIGATION WHERE ID = :ee_id;

-- 2. safety: no grandchildren, and no child ACL still guards a live object
SELECT COUNT(*) FROM ACLOBJECTIDENTITY g JOIN ACLOBJECTIDENTITY o ON o.ID = g.PARENT_OBJECT_FK
 WHERE o.PARENT_OBJECT_FK = :oi_id;                       -- must be 0
-- repeat per class for BIO_MATERIAL / BIO_ASSAY / FACTOR_VALUE / EXPERIMENTAL_FACTOR /
-- EXPERIMENTAL_DESIGN, joining the child's OBJECT_ID to the table's ID; all must be 0

-- 3. delete, children before parents
START TRANSACTION;
DELETE FROM ACLENTRY            WHERE OBJECTIDENTITY_FK = :oi_id;
DELETE FROM ACLOBJECTIDENTITY   WHERE PARENT_OBJECT_FK  = :oi_id;
DELETE FROM ACLOBJECTIDENTITY   WHERE ID                = :oi_id;
DELETE FROM acl_entry           WHERE acl_object_identity = :oi_id;
DELETE FROM acl_object_identity WHERE parent_object       = :oi_id;
DELETE FROM acl_object_identity WHERE id                  = :oi_id;

-- 4. verify all four are 0, then COMMIT (else ROLLBACK)
SELECT (SELECT COUNT(*) FROM ACLOBJECTIDENTITY WHERE ID=:oi_id OR PARENT_OBJECT_FK=:oi_id) AS legacy_left,
       (SELECT COUNT(*) FROM acl_object_identity WHERE id=:oi_id OR parent_object=:oi_id)   AS canonical_left,
       (SELECT COUNT(*) FROM ACLENTRY WHERE OBJECTIDENTITY_FK=:oi_id)                       AS legacy_entries_left,
       (SELECT COUNT(*) FROM acl_entry WHERE acl_object_identity=:oi_id)                     AS canonical_entries_left;
COMMIT;
```

Then flush the ACL cache on every running instance: `DELETE /admin/caches/aclCache`.

Never delete from `ACLSID` / `acl_sid` — sids are shared database-wide. Avoid `IN (SELECT …)` against
the table being deleted from: prod is `BINLOG_FORMAT=STATEMENT` and such statements log a
`Note 1592` unsafe-for-replication warning (benign here, since the child sets carry no entries, but
literal predicates are deterministic and simpler).

## 8. Reusable verification queries

```sql
-- experiments whose ACL is missing from phase2's store (drift on create)
SELECT COUNT(*) FROM ACLOBJECTIDENTITY o
 WHERE o.OBJECT_CLASS LIKE '%ExpressionExperiment'
   AND NOT EXISTS ( SELECT 1 FROM acl_object_identity oi JOIN acl_class c ON c.id = oi.object_id_class
                     WHERE oi.object_id_identity = o.OBJECT_ID AND c.class LIKE '%ExpressionExperiment' );

-- phase2 ACL identities for experiments that no longer exist (drift on delete)
SELECT COUNT(*) FROM acl_object_identity oi JOIN acl_class c ON c.id = oi.object_id_class
 WHERE c.class LIKE '%ExpressionExperiment'
   AND NOT EXISTS ( SELECT 1 FROM INVESTIGATION i WHERE i.ID = oi.object_id_identity );

-- same, legacy store (1.32.x's view); should stay at 0
SELECT COUNT(*) FROM ACLOBJECTIDENTITY o
 WHERE o.OBJECT_CLASS LIKE '%ExpressionExperiment'
   AND NOT EXISTS ( SELECT 1 FROM INVESTIGATION i WHERE i.ID = o.OBJECT_ID );

-- is V23 applied? RESTRICT means no
SELECT k.CONSTRAINT_NAME, r.DELETE_RULE FROM information_schema.KEY_COLUMN_USAGE k
  JOIN information_schema.REFERENTIAL_CONSTRAINTS r
    ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
 WHERE k.TABLE_SCHEMA = 'gemd' AND k.TABLE_NAME = 'SINGLE_CELL_DIMENSION_EXPERIMENT';
```

## 9. Not investigated

- Why the ACL removal path did not fire (§5 item 1) — the open question this plan hands off.
- Whether the 44 canonical-store orphans are purely 1.32.x drift or partly the same bug as #5.
- Magnitude of ACL **entry** drift for objects that exist in both stores (permission changes since
  2026-05-18); only identity-level drift was measured.
- Whether other soft references to experiments exist outside the four found by the column-name sweep
  (`%EXPRESSION_EXPERIMENT%`, `%INVESTIGATION%`, `OBJECT_ID`, `%TARGET%`, `EE%ID%`, `%ENTITY_ID%`).
- Full `mvn verify` has not been run against these commits; only the four SC/EE test classes
  (90 tests, H2).
