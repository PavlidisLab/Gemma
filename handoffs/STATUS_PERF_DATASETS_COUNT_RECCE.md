# Perf recce: `/rest/v2/datasets/count` hangs against prod gemd

**Date**: 2026-05-22
**Branch**: `perf-datasets-count-recce` off `phase2-acl-migrate@a5a99a606e`
**Container**: `gemma-rest:dev` (image built 2026-05-23T04:21Z, gemma-core-1.32.7-SNAPSHOT.jar = phase2 code; the "1.32.7-SNAPSHOT" version label is just an artefact of how the phase2 branch is versioned, the deployed `AclQueryUtils.class` is the new EXISTS-rewrite).
**Symptom**: anonymous `curl http://localhost:8080/rest/v2/datasets/count` and `/rest/v2/datasets?limit=5` hang past 35 s; staging-gemma serves both in <200 ms on the same prod DB.

## What's running

Thread dump shows all `http-nio-*` workers parked in `MySQL NativeProtocol.readMessage` inside `JdbcSelectExecutorStandardImpl`. The DB is the bottleneck. Live `information_schema.processlist` during a hung request shows 6 queries piled up, each running 230 s–1054 s, all owned by `gemmaadmin@willie.pavlab.msl.ubc.ca` (one per failed `curl` attempt during the recce). Two query shapes:

**Count** (`/datasets/count`):

```sql
select count(ee1_0.ID)
from INVESTIGATION ee1_0
left join CURATION_DETAILS cd1_0 on cd1_0.ID = ee1_0.CURATION_DETAILS_FK
where exists (
        select 1 from acl_object_identity aoi1_0
                  join acl_entry e1_0 on aoi1_0.id = e1_0.acl_object_identity
        where aoi1_0.object_id_identity = ee1_0.ID
          and (select c.class from acl_class c where c.id = aoi1_0.object_id_class)
              = 'ubic.gemma.model.expression.experiment.ExpressionExperiment'
          and (e1_0.mask & 1) <> 0
          and e1_0.sid in (select agas1_0.id from acl_sid agas1_0
                           where agas1_0.sid = 'IS_AUTHENTICATED_ANONYMOUSLY' and agas1_0.principal = 0))
  and (cd1_0.TROUBLED = 0) and ee1_0.class = 'ExpressionExperiment';
```

The `limit 5` listing variant has the same EXISTS clause with all the GEEQ/CurationDetails/AuditEvent joins layered on top.

## EXPLAIN — slow shape (Hibernate-emitted)

```
PRIMARY            cd1_0  ref   TROUBLED_IX           rows=13214  Using index
PRIMARY            ee1_0  ref   FKF2B9BAE2A03372D0    rows=2
DEPENDENT SUBQUERY agas1_0 const principal             rows=1
DEPENDENT SUBQUERY e1_0   ref   fk_ace_sid            rows=505398   <-- per outer EE row
DEPENDENT SUBQUERY aoi1_0 eq_ref PRIMARY              rows=1
DEPENDENT SUBQUERY c      eq_ref PRIMARY              rows=1        <-- per acl_entry row
```

For ~13k EE rows the optimiser drives the inner EXISTS off `acl_entry` via `fk_ace_sid` (505k anonymous-SID-bearing entries). Cost ≈ 13k × 505k ≈ 6.5 G row examinations + a `c.class` lookup per match. Hours, not seconds.

## EXPLAIN — fast shape (manually rewritten)

Same query, but the `(select c.class ...)` scalar subquery replaced with `JOIN acl_class aoi1_0_cls ON aoi1_0_cls.id = aoi1_0.object_id_class WHERE aoi1_0_cls.class = '...'`:

```
PRIMARY            cd1_0       ref     TROUBLED_IX  rows=13214  Using index
PRIMARY            ee1_0       ref     FKF2B9BAE2A03372D0 rows=2
DEPENDENT SUBQUERY aoi1_0_cls  const   class        rows=1      <-- folded to const
DEPENDENT SUBQUERY agas1_0     const   principal    rows=1
DEPENDENT SUBQUERY aoi1_0      eq_ref  object_id_class rows=1   <-- (object_id_class, object_id_identity) UNIQUE
DEPENDENT SUBQUERY e1_0        ref     acl_object_identity rows=2
```

Wall clock: `count(*) = 23549` in **< 1 second** (vs > 1000 s and counting for the Hibernate-emitted variant).

## Root cause

`gemma-core/src/main/resources/ubic/gemma/core/security/model/AclObjectIdentity.hbm.xml` lines 38–39:

```xml
<property name="type" type="java.lang.String"
          formula="(select c.class from acl_class c where c.id = object_id_class)"/>
```

The HQL ACL EXISTS clause in `AclQueryUtils.formAclRestrictionClause` references `aoi.type = :aoiType`. Hibernate substitutes the formula verbatim, producing a scalar correlated subquery on `acl_class`. MySQL 5.7's optimiser cannot fold a scalar subquery on a UNIQUE column the way it folds an inner `JOIN acl_class cls ON aoi.object_id_class = cls.id WHERE cls.class = :const` (which becomes a `const` row that lets `acl_object_identity` be driven through its `(object_id_class, object_id_identity)` UNIQUE index as `eq_ref`). With the formula in place the planner can't see that `aoi.object_id_class` is functionally constant, so it drives the EXISTS off `acl_entry`'s `fk_ace_sid` index instead. On 25k EEs × 505k acl_entry-anonymous-SID rows this never completes.

Note: this is a regression introduced when `phase2-acl-migrate` moved from `OBJECT_CLASS` as a denormalised string column on the legacy `ACLOBJECTIDENTITY` table (Hibernate `<property>` on a real column → no subquery) to Spring Security 6's canonical `acl_object_identity (object_id_class FK → acl_class.id)` shape with the formula-based `type` accessor. The new shape is correct against gemdtest (small enough that the bad plan still finishes in seconds) but degrades pathologically against prod cardinalities. Staging (1.32.x older code line) is on the legacy table where `OBJECT_CLASS` is a column, so the same logical query plans well there.

## Proposed fix direction

**Replace the formula-derived `aoi.type` with an explicit HQL join** in `AclQueryUtils.formAclRestrictionClause` (and the parallel native form in `formNativeAclRestrictionClause`). Map a real `AclClass` entity (or just expose `objectIdClass` as today and join through it in HQL), then rewrite the EXISTS body to:

```hql
exists (select 1 from AclObjectIdentity aoi
          join aoi.objectIdClassEntity cls
          ...
        where aoi.identifier = <id>
          and cls.className = :aoiType
          and ...)
```

so the generated SQL is `... join acl_class cls on cls.id = aoi.object_id_class ... where cls.class = :param`. The fix is a 1-file mapping addition (`AclClass` entity + hbm) plus the ~10 LOC tweak in `AclQueryUtils`. The formula stays available for code paths that *load* an `AclObjectIdentity` and want `.getType()` to work — only the filter clause changes shape.

Alternative quick mitigation if a mapping change is too invasive for a hot patch: in `AclQueryUtils`, switch the HQL EXISTS body to filter on `aoi.objectIdClass = :aoiClassId` and resolve the class id once up-front via a cached lookup (`select id from acl_class where class = :type`), bypassing the formula entirely. This is a < 30 LOC change in `AclQueryUtils` + `addAclParameters` only.

Either path eliminates the scalar correlated subquery and restores the `eq_ref` plan. Validation: re-run `EXPLAIN` on prod after the change; expect `aoi1_0` to switch from a `DEPENDENT SUBQUERY` driven through `acl_entry` to `eq_ref` on the `object_id_class` composite UNIQUE index.

## Outstanding

6 piled-up queries from this recce are still consuming CPU on prod (oldest 1054 s as of last check). They are owned by `gemmaadmin@willie.pavlab.msl.ubc.ca` (Paul's session). Recommend KILLing them once the recce is acknowledged; this recce did not issue KILL since it falls outside read-only.
