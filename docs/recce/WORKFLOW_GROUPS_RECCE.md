# Workflow Groups CRUD + `/datasets/{id}/groups` — Reconnaissance

**Status:** recce only. No production code touched.
**Scope:** Curation-UI top-3 blocker #2, set-navigator panel (the primary
in-batch navigation UI). Per `CURATION_UI_HANDOFF_INVENTORY.md` row 18 +
section 2's "Workflow Groups" entry (size **M**).
**Branch:** `phase2-acl-migrate`, worktree `feat-batch89` off `a5701f0`.

---

## 0. Why this is a recce, not a patch

The brief said: "If building a new entity requires a Flyway migration:
STOP and write a recce. Don't add new DB tables tonight."

Implementation requires a Flyway migration. Three reasons, in order of
how load-bearing they are:

1. **The UI's `Group` shape carries fields the existing
   `ExpressionExperimentSet` entity doesn't have**: `type`
   (`"screening" | "pipeline" | "review"`), `created_by`, `created_at`,
   plus a stable string ID. Two of those (`type`, `created_by`) have no
   column on `EXPRESSION_EXPERIMENT_SET`. Cannot piggyback without
   schema change.
2. **Screening-group membership uses non-numeric IDs.** UI's
   `GroupMembersAdd.member_ids: (string | number)[]` allows candidate
   UUIDs (from the candidate-screening surface, also missing per the
   handoff inventory). EESet's `EXPERIMENTS2EXPRESSION_EXPERIMENT_SETS`
   many-to-many is FK-typed to `EXPRESSION_EXPERIMENT.ID` and won't
   accept UUIDs. Even if we restrict workflow groups to numeric IDs,
   the screening-group sub-type can't share the table.
3. **Membership order matters.** UI says "Ordered by `added_at`
   (insertion time) — predictable + stable across reads, so the
   navigator's prev/next can index by position". A `<set>` mapping in
   Hibernate is unordered. We'd need either an order column on the
   join table or to switch to a `<list>` mapping — both schema changes.

So: this is a **new entity** ask, regardless of how creatively we try
to reuse `ExpressionExperimentSet`.

---

## 1. UI wire contract (what we have to deliver)

Sources: `/Users/pzoot/Dev/gemma-curation-ui/apps/curation/src/api/workflowTypes.ts`
(lines 132–212) + `workflow.ts` (lines 282–398).

### Endpoints

| Verb | Path | Body | Returns |
|---|---|---|---|
| GET | `/rest/v2/groups[?type=&created_by=]` | — | `Group[]` |
| POST | `/rest/v2/groups` | `GroupCreate {name, type, description?}` | `Group` |
| GET | `/rest/v2/groups/{id}[?include_summaries=true]` | — | `Group` (with `member_summaries` if flag) |
| PATCH | `/rest/v2/groups/{id}` | `GroupPatch {name?, description?}` | `Group` |
| DELETE | `/rest/v2/groups/{id}` | — | 204 |
| POST | `/rest/v2/groups/{id}/members` | `{member_ids: (string\|number)[]}` | `Group` |
| DELETE | `/rest/v2/groups/{id}/members/{memberId}` | — | 204 |
| GET | `/rest/v2/datasets/{id}/groups[?include_summaries=true]` | — | `Group[]` |

### `Group` JSON shape

```ts
interface Group {
  id: string;             // stable string id; mock uses uuid
  name: string;
  type: "screening" | "pipeline" | "review";
  description: string;
  created_by: string;     // username of creator
  created_at: string;     // ISO-8601
  member_ids: string[];   // insertion-ordered
  member_count: number;
  member_summaries?: ExperimentSummary[] | null;  // when ?include_summaries=true
}
interface ExperimentSummary {
  experiment_id: number;
  short_name: string;
  title: string;
  taxon: string;
  troubled: boolean;
  needs_attention: boolean;
  is_public: boolean;
  audit_status?: "none" | "in_progress" | "closed";
}
```

Notes:
- `member_ids` are strings even for numeric experiment IDs ("100" not
  `100`). The UI is willing to stringify Long IDs on its side, but the
  wire shape is `string[]`.
- `audit_status` is sourced from a curation-side AuditReport surface
  that doesn't exist on gemma-rest. Safe to always omit on first cut.

---

## 2. Why `ExpressionExperimentSet` can't host this (detail)

I considered piggybacking on `ExpressionExperimentSet` to avoid a new
table. The lure: it already has name + description + members + audit
trail + ACL via `Securable`. The blockers:

| What we'd need | What EESet has | Fix cost |
|---|---|---|
| `type` discriminator (screening/pipeline/review) | nothing — no string column free | ALTER TABLE add column + index |
| `created_by` (username, not auditable creation event) | derivable via `auditTrail.events[0].performer` — but expensive at list time | join + projection rewrite |
| Stable insertion order on members | `Set<ExpressionExperiment>` (unordered) | join-table order column |
| Non-numeric (UUID) members for screening groups | `EXPERIMENTS2EXPRESSION_EXPERIMENT_SETS.EXPERIMENTS_FK BIGINT NOT NULL` | new table or new column |
| Cheap `/datasets/{id}/groups` reverse lookup | exists via the many-to-many — this is the one thing that maps cleanly | — |

Even if we restricted to "pipeline" + "review" types and dropped the
screening UUID story (deferring that to the candidates surface), we
still need `type` + ordering + `created_by` materialization. That's a
non-trivial migration, and one that changes the semantics of an entity
used across the analysis subsystem.

**Conclusion: build a new entity. Do not stretch EESet to cover this.**

---

## 3. Proposed schema

Two new tables. Names follow Gemma's existing UPPER_SNAKE conventions.

### `WORKFLOW_GROUP`

```sql
CREATE TABLE WORKFLOW_GROUP (
    ID              BIGINT       NOT NULL AUTO_INCREMENT,
    EXTERNAL_ID     VARCHAR(36)  NOT NULL,           -- UUID; UI's `id: string`
    NAME            VARCHAR(255) NOT NULL,
    DESCRIPTION     TEXT         NULL,
    TYPE            VARCHAR(16)  NOT NULL,           -- 'screening'|'pipeline'|'review'
    CREATED_BY      VARCHAR(255) NOT NULL,           -- username at creation
    CREATED_AT      DATETIME     NOT NULL,
    UPDATED_AT      DATETIME     NULL,
    AUDIT_TRAIL_FK  BIGINT       NULL UNIQUE,        -- optional, matches EESet pattern
    PRIMARY KEY (ID),
    UNIQUE KEY UQ_WORKFLOW_GROUP_EXTERNAL_ID (EXTERNAL_ID),
    KEY IX_WORKFLOW_GROUP_TYPE (TYPE),
    KEY IX_WORKFLOW_GROUP_CREATED_BY (CREATED_BY)
);
```

Rationale:
- `EXTERNAL_ID` UUID column lets the wire contract use string IDs
  without coupling to the autoincrement BIGINT. Same pattern other
  external-ID layers use.
- `TYPE` as VARCHAR + index, not enum — cheaper to evolve. JPA-side
  enum mapping with `@Enumerated(STRING)` equivalent in HBM.
- `CREATED_BY` as a copied-in string rather than FK to USER — survives
  user renames/deletes; the UI just wants "who made this".

### `WORKFLOW_GROUP_MEMBER`

```sql
CREATE TABLE WORKFLOW_GROUP_MEMBER (
    ID                  BIGINT       NOT NULL AUTO_INCREMENT,
    WORKFLOW_GROUP_FK   BIGINT       NOT NULL,
    MEMBER_ID           VARCHAR(64)  NOT NULL,       -- holds either stringified Long or UUID
    EXPERIMENT_FK       BIGINT       NULL,           -- when MEMBER_ID is a numeric EE id
    ADDED_AT            DATETIME     NOT NULL,
    POSITION            INT          NOT NULL,
    PRIMARY KEY (ID),
    UNIQUE KEY UQ_WORKFLOW_GROUP_MEMBER (WORKFLOW_GROUP_FK, MEMBER_ID),
    KEY IX_WORKFLOW_GROUP_MEMBER_EXPERIMENT (EXPERIMENT_FK),
    KEY IX_WORKFLOW_GROUP_MEMBER_GROUP_POSITION (WORKFLOW_GROUP_FK, POSITION),
    CONSTRAINT FK_WGM_GROUP FOREIGN KEY (WORKFLOW_GROUP_FK)
        REFERENCES WORKFLOW_GROUP (ID) ON DELETE CASCADE,
    CONSTRAINT FK_WGM_EXPERIMENT FOREIGN KEY (EXPERIMENT_FK)
        REFERENCES INVESTIGATION (ID) ON DELETE CASCADE
);
```

Rationale:
- Separate row per member (not a join table) so we can carry
  `ADDED_AT` + `POSITION` per the UI's "ordered by added_at" promise.
- `MEMBER_ID` VARCHAR + nullable `EXPERIMENT_FK` accommodates both
  numeric EE members ("pipeline"/"review" groups) and UUID candidate
  members ("screening" groups, once the candidate surface lands).
  When `MEMBER_ID` parses as Long AND that EE exists, we populate
  `EXPERIMENT_FK` for fast reverse-lookup; otherwise leave it null.
- `ON DELETE CASCADE` on both FKs so deleting a group cleans its
  members, and deleting an EE removes it from groups (matching
  `removeFromSets` behavior on EESet).
- Reverse lookup `/datasets/{id}/groups` becomes a single index hit:
  `SELECT WORKFLOW_GROUP_FK FROM WORKFLOW_GROUP_MEMBER WHERE EXPERIMENT_FK = ?`.

### Migration files

- `gemma-core/src/main/resources/db/migration/mysql/V4__workflow_groups.sql`
  (next free: V1 baseline, V2 audit_event_payload, V3 ticket_layer.)
- `gemma-core/src/main/resources/db/migration/h2/V6__workflow_groups.sql`
  (next free after V5 ticket_layer.)

Both write the same DDL in their respective dialects. Existing
sister-pair pattern (V4__audit_event_payload.sql ↔ V2__audit_event_payload.sql)
is the template.

---

## 4. Proposed Java layout

### Model

- `ubic/gemma/model/workflow/WorkflowGroup.java` extends `AbstractAuditable`
  implements `Securable` (matches EESet's ACL story).
  - Fields: `externalId`, `type` (enum `WorkflowGroupType`), `createdBy`,
    `createdAt`, `members: List<WorkflowGroupMember>` (List, not Set, ordered by POSITION).
- `ubic/gemma/model/workflow/WorkflowGroupMember.java`
  - Fields: `id`, `memberId` (String), `experiment: ExpressionExperiment?`,
    `addedAt`, `position`.
- `ubic/gemma/model/workflow/WorkflowGroupType.java` enum.
- Mappings: `WorkflowGroup.hbm.xml`, `WorkflowGroupMember.hbm.xml`
  under `gemma-core/src/main/resources/ubic/gemma/model/workflow/`.

### Persistence

- `ubic/gemma/persistence/service/workflow/WorkflowGroupDao.java` +
  `WorkflowGroupDaoImpl.java`.
- `WorkflowGroupService` + `WorkflowGroupServiceImpl` — `@Secured` annotations
  cribbed from `ExpressionExperimentSetService`:
  - `loadAll`: `IS_AUTHENTICATED_ANONYMOUSLY` + `PostFilter` READ/ADMIN.
  - `create`/`update`/`delete`: `IS_AUTHENTICATED_ANONYMOUSLY` + `ACL_SECURABLE_EDIT`
    (creation gets `GROUP_USER` write on the new ACL via the standard
    `AfterAcl` pattern; verify by reading `ExpressionExperimentSetServiceImpl.create`).
- Method surface:
  - `Collection<WorkflowGroup> loadAll(WorkflowGroupType?, String createdBy?)`
  - `WorkflowGroup loadByExternalId(String)`
  - `WorkflowGroup create(name, type, description, createdBy)`
  - `WorkflowGroup update(id, name?, description?)`
  - `WorkflowGroup addMembers(id, List<String> memberIds)` — resolves
    numeric ids to EEs where possible, dedupes against existing.
  - `void removeMember(id, String memberId)`
  - `void delete(id)`
  - `Collection<WorkflowGroup> findByExperiment(ExpressionExperiment)` — the
    reverse lookup, ACL-filtered to READ on the group.

### REST

- `gemma-rest/src/main/java/ubic/gemma/rest/WorkflowGroupsWebService.java`
  — `@Path("/groups")`. CRUD + member ops listed above.
- `gemma-rest/src/main/java/ubic/gemma/rest/WorkflowGroupValueObject.java`
  — wire-shape VO matching the UI's `Group`. Hand-mapped from entity.
  - Snake-case JSON via `@JsonProperty` (matches the UI's snake-case
    expectation — Gemma's existing VOs use camelCase, so this is a
    deliberate deviation for the curation-UI subset; see
    `PipelineStatusValueObject` for precedent).
- `gemma-rest/src/main/java/ubic/gemma/rest/ExperimentSummaryValueObject.java`
  — expanded summary shape for `?include_summaries=true`. Pulls from
  existing `ExpressionExperimentDetailsValueObject`.
- New method on `DatasetsWebService.java`:
  ```java
  @GET @Path("/{dataset}/groups")
  @Operation(summary = "List workflow groups containing this dataset")
  public List<WorkflowGroupValueObject> getGroupsForDataset(
      @PathParam("dataset") DatasetArg<?> datasetArg,
      @QueryParam("include_summaries") @DefaultValue("false") boolean includeSummaries
  );
  ```
  Delegates to `workflowGroupService.findByExperiment(ee)`.

---

## 5. Open questions

1. **`type` immutability.** PATCH allows `name`/`description` only — does
   that mean `type` is fixed at creation? UI sends `type` on POST, never
   PATCHes it. Treat as immutable. (Easy to relax later.)
2. **ACL on a "shared review group".** Workflow groups are likely to be
   shared across curators. Default: creator gets `WRITE`, group `GROUP_USER`
   gets `READ`. Matches the mock's behavior, where every curator sees every
   group. May need a per-group sharing toggle later.
3. **`member_count` on list endpoint.** UI displays a count in the group
   list. Cheapest: a `COUNT(*)` subquery / formula on `WorkflowGroup`. A
   denormalized `MEMBER_COUNT` column is faster but invites drift. Recommend
   the formula until we hit a measured perf issue.
4. **Screening-group member resolution.** Until candidates exist on
   gemma-rest, screening groups will have UUID members that resolve to
   nothing. UI handles this (sets `experiment_id=0`, `short_name=member_id`).
   Implementation must not 404 on those.
5. **GEMMA-side `@Path` routing.** UI hits `/rest/v2/groups`. Gemma's
   JAX-RS context root is `/rest/v2`, so `@Path("/groups")` lands at the
   right URL. Verify against `RestApplication.java` if any prefix
   surprise (`AnnotationsWebService` is `@Path("/annotations")` and lands
   at `/rest/v2/annotations`, so we're good).
6. **OpenAPI / Swagger.** Add `@Operation` / `@Tag` annotations to keep
   the auto-generated spec coherent. Cribbed from existing web services.

---

## 6. Sizing the actual patch

If a future session greenlights the migration, the patch is roughly:

| Component | LOC | Complexity |
|---|---|---|
| 2× Flyway migrations (mysql + h2) | ~60 | trivial DDL |
| 3× entity classes + 2× HBM mappings | ~250 | mechanical, follow EESet template |
| DAO + DAOImpl | ~200 | one or two HQL queries; Criteria for filter |
| Service + ServiceImpl | ~250 | ACL annotations, factory method |
| REST resource (`WorkflowGroupsWebService`) | ~300 | 7 endpoints + VO mapping |
| New method on `DatasetsWebService` | ~30 | one route + delegate |
| Wire VOs (`WorkflowGroupValueObject`, `ExperimentSummaryValueObject`) | ~150 | hand-map |
| Service-level integration tests | ~250 | follow `ExpressionExperimentSetServiceTest` |
| REST integration tests | ~250 | follow `DatasetsWebServiceTest` |

Estimate: **~1,700 LOC, M-size as already sized in `CURATION_UI_HANDOFF_INVENTORY.md`** —
one focused session with the migration approved up front.

---

## 7. Adjacent precedent worth re-reading before implementing

- `ExpressionExperimentSet` — closest entity shape; HBM mapping
  template; service ACL pattern.
- `Ticket` / `TicketLayer` (V3 migration + ticket code on this branch)
  — most recent "new entity" added; freshest example of the full
  end-to-end recipe.
- `PipelineStatusValueObject` — JSON-shape precedent for a snake-case,
  curation-UI-targeted VO living alongside camelCase Gemma VOs.
- `DatasetsWebService.getAuditEvents` — closest reverse-lookup pattern
  to `/datasets/{id}/groups`.

---

## 8. What I did NOT do tonight

- Did not write the Flyway migration.
- Did not add entity classes, DAOs, services, REST resources.
- Did not edit `DatasetsWebService.java` (per the brief: "you'll be
  editing it alongside another agent on batch 90"; the reverse-lookup
  method ships with the rest of the patch in one go, not split across
  agents).
- Did not run `mvn package`. Nothing to build.
