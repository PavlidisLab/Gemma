# SQL_INJECTION_HIBERNATE — pre-2.0 triage (2026-05-21)

Triages the 85 priority-2 `SQL_INJECTION_HIBERNATE` findsecbugs findings
reported in `SPOTBUGS_FIRST_PASS.md`. Authoritative source: the most
recent SpotBugs run at
`.claude/worktrees/agent-wire-spotbugs/gemma-core/target/spotbugsXml.xml`
(85 BugInstance entries with `type="SQL_INJECTION_HIBERNATE"`,
priority 2, rank 12).

Read-only research — no production source touched.

## Summary

| bucket | meaning | count |
|---|---:|---:|
| A | internal-only concat (class/entity/alias names, hard-coded literals, internal-computed fragments) | **80** |
| B | trusted typed param, easy parameterizable | 0 |
| C | RAW user-controlled string flowing into HQL | **0** |
| D | dynamic property/column name | 5 |
| **total** | | **85** |

**Headline**: zero bucket-C findings. Every concatenated value is one of
- `Class.getSimpleName()` / `Class.getName()` (entity dispatch by typed Class parameter),
- `sessionFactory.getMetamodel().entity(clazz).getName()` (same idea, metamodel-resolved),
- `HibernateUtils.getEntityName(sf, clazz)` (same),
- a `private static final String` HQL fragment,
- a hard-coded literal table/column name passed from the public overload,
- an internally computed branch ("asc"/"desc"/"and id > :cursorId") off a typed boolean,
- a numeric literal built from `int` arithmetic,
- an internal alias-builder method on a typed inner class (`*Initializer.createSelect`),
- the central whitelist-validated `FilterQueryUtils.formRestrictionClause(filters)` /
  `formOrderByClause(sort)` chain (whitelist enforced at the REST layer; see "Bucket D" below).

The pattern is uniform: HQL builders use string concat to splice in
entity names typed as `Class<?>` or to switch between literal branches,
never to interpolate user-controlled text. None of the findings is a
pre-2.0 security blocker.

## Bucket C — user-controlled input flowing into HQL

**None.** Direct grep + per-callsite read confirms that no
`createQuery`/`createNativeQuery` concat in any of the 85 findings
splices a string that originates from a `@PathParam`, `@QueryParam`,
HTTP body, or CLI argument without first passing through:
- typed entity dispatch (`Class<? extends DataVector>` etc.), OR
- the `FilterArg` ANTLR parser whose property names are whitelisted by
  `service.getFilterableProperties().contains(property)`
  (FilterArg.java:187) before reaching the SQL builder.

If a future findsecbugs run flags a callsite outside the patterns
inventoried here, that is the regression to look at.

## Bucket B — easy parameterizations

**None worth listing.** Every parameter that *could* be a `:setParameter`
in HQL already is one in these 85 findings. The string concats remaining
are not parameterizable in HQL at all (entity name, alias name,
column name, or asc/desc keyword — none of which HQL/JDBC parameter
binding can carry).

## Bucket D — dynamic property/column names (verify whitelist)

These 5 sites genuinely build SQL/HQL with a column or entity-name
fragment that varies at runtime, which HQL cannot parameterize.

| file | line | dynamic value | source | whitelist verdict |
|---|---:|---|---|---|
| `CharacteristicDaoImpl.java` | 121 | `"order by c." + orderField + (desc?" desc":" asc")` | `CharacteristicBrowserController.java:81-93` switch → "category" / "value" / "evidenceCode" / IAE | **safe** (3-element whitelist, default throws) |
| `BibliographicReferenceDaoImpl.java` | 176 | `:orderField` is already a bound param (the only literal concat is the `desc`/`""` keyword) | `BibliographicReferenceReadServiceImpl.browse(int,int,String,boolean)` — no REST/web caller | **dead** (no caller in gemma-rest or gemma-web; remove the orderField overload) |
| `ExpressionExperimentDaoImpl.java` | 4148 | `groupBy` fragment in `getLoadValueObjectsQueryString` | private internal — only called with hard-coded `"ee"` | **safe** (constant) |
| `ArrayDesignDaoImpl.java` | 1126 | same `groupBy` pattern as above | private | **safe** |
| `FilterQueryUtils.formRestrictionClause/formOrderByClause` callers (many of the 85 hit this transitively) | — | column/alias from `Filter.getPropertyName()` | `FilterArg.getFilters()` checks `service.getFilterableProperties().contains(property)` and throws `MalformedArgException` otherwise | **safe** (whitelist enforced before SQL construction) |

The `gemma-web` `CharacteristicBrowserController` callsite is moot —
`gemma-web` is on the retirement path (per memory
`project_gemma_web_replacement.md`). Even if it survived, the switch is
a tight whitelist.

## Bucket A — internal concat (count by file)

| count | file | pattern |
|---:|---|---|
| 27 | `ExpressionExperimentDaoImpl.java` | mix of `Class.getSimpleName()` dispatch (single-cell, raw/processed vectors), `*Initializer.createSelect(alias)` for typed-tuple selection, and one private overload with hard-coded `"CELL_LEVEL_CHARACTERISTICS"` / `"INDICES"` literals (line 3164, 3217) |
| 9 | `GeneDaoImpl.java` | `AclQueryUtils.formAclRestrictionClause(...)` literal + `boolean includeDummyProducts` branching to ` and gp.dummy = false` literal; `SequenceBinUtils.addBinToQuery("pl", ...)` builds bin clause from int constants |
| 8 | `QuantitationTypeDaoImpl.java` | every concat is `Class.getSimpleName()` / `Class.getName()` of a typed `Class<? extends DataVector>` parameter, OR `HibernateUtils.getEntityName(sf, clazz)` |
| 5 | `DifferentialExpressionResultDaoImpl.java` | boolean-conditional HQL fragments (`baselineMap != null ? ", b.id, be.type" : ""`, `limit > 0 ? " order by ..." : ""`, `keepNonSpecificProbes ? "" : " and (subquery)"`) |
| 5 | `BibliographicReferenceDaoImpl.java` | `AclQueryUtils.formAclRestrictionClause("e.id")` literal — fragment is itself a constant `String` returned by AclQueryUtils |
| 5 | `CompositeSequenceDaoImpl.java` | `CS_BY_GENE_QUERY` / `CS_BY_GENE_GENE2CS_QUERY` `private static final String` constants + cursor `comparator` from boolean ("and cs.id > :cursorId" literal) |
| 5 | `FactorValueDaoImpl.java` | `AclQueryUtils.formAclRestrictionClause("ee.id")` literal |
| 4 | `AclLinterServiceImpl.java` | `sessionFactory.getMetamodel().entity(clazz).getName()` for the dangling-ACL set-difference query |
| 4 | `AuditEventDaoImpl.java` | `HibernateUtils.getEntityName(sf, auditableClass)` + `classes != null ? "and type(et) in :classes " : ""` |
| 4 | `ArrayDesignDaoImpl.java` | `AclQueryUtils.formAclRestrictionClause("ad.id")` literal; `FilterQueryUtils.form*` chain (see bucket D row 5) |
| 2 | `TaxonDaoImpl.java` | `FilterQueryUtils.form*` chain |
| 1 | `ExpressionExperimentSetDaoImpl.java` | `ids != null ? "where eeset.id in (:ids) " : ""` literal switch |
| 1 | `AbstractCuratableDao.java` | `getElementClass().getSimpleName()` |
| 1 | `BioAssayDaoImpl.java` | cursor `comparator` + `orderDirection` from typed boolean |
| 1 | `ProcessedExpressionDataVectorDaoImpl.java` | `ees != null ? " and dedv.expressionExperiment in :ees" : ""` |
| 1 | `GeneSetDaoImpl.java` | `taxon != null ? "and g.taxon = :taxon " : ""` |
| **80** | | |

(2 of the 85 are the bucket-D `CharacteristicDaoImpl.browse` +
`BibliographicReferenceDaoImpl.browse`; 3 more are the `FilterQueryUtils`
chain hits aggregated under bucket D row 5. Net: 80 bucket A + 5 bucket D.)

## Surprises / notes

- **No findings in gemma-rest.** The 85 are entirely in the `gemma-core`
  DAO layer. The REST controllers all use typed args (`FilterArg`,
  `SortArg`, `OffsetArg`, `LimitArg`, `StringArrayArg`) that bind
  parameters or normalize through whitelist-validated parsers before
  reaching the DAO.
- **One non-DAO finding**: `AclLinterServiceImpl` (4 findings). Reads
  like a DAO but lives in `core.security.authorization.acl`. The
  concatenated value is `sessionFactory.getMetamodel().entity(clazz)
  .getName()` — Hibernate metamodel-resolved entity name typed as
  `Class<? extends Securable>`. Bucket A.
- **Dead code candidate**: `BibliographicReferenceDao.browse(int, int,
  String orderField, boolean descending)` and its
  `BibliographicReferenceService` / `BibliographicReferenceReadService`
  wrappers have no caller in `gemma-rest` or `gemma-web`. The legacy
  gemma-web `BibliographicReferenceController` predates the
  ordered-browse overload. Pruning the 4-arg overload would remove one
  of the 85 findings for free.
- **HQL `order by :orderField` doesn't bind**: `BibliographicReferenceDaoImpl.java:176`
  calls `setParameter("orderField", orderField)` against a position
  HQL doesn't parameter-bind. Functionally a no-op (Hibernate emits
  `order by ?` and MySQL ignores it). Not a security issue but a
  separate latent bug — orthogonal to this triage.
- **`CharacteristicDaoImpl.java:121`** is the one site where a raw
  string is concatenated into an `order by` clause without going
  through `FilterQueryUtils.formOrderByClause`. The 3-element switch
  whitelist in `CharacteristicBrowserController` keeps it safe; if
  that controller were ever exposed to gemma-rest without the switch,
  it would slot into bucket C. Worth a comment on the DAO method
  pinning the whitelist contract — or migrating to `Sort` so the
  whitelist lives in `getFilterableProperties()`.
- **`spotbugs-exclude.xml`** has no `SQL_INJECTION_HIBERNATE` suppression
  today. After this triage, a class-level (or file-level) suppression
  bucket of "verified internal concat — entity/alias name not user
  input" would let the next priority-1 sweep surface signal from any
  *new* SQL_INJECTION_HIBERNATE finding. Suggested categories to add:
  - all `*Dao` methods that build `... + Class.getSimpleName() + ...` /
    `... + Class.getName() + ...` against a typed `Class<?>` parameter,
  - any callsite where the only concat is a `private static final String`
    HQL fragment constant,
  - any callsite where the only concat is the result of
    `AclQueryUtils.formAclRestrictionClause(...)` /
    `FilterQueryUtils.formRestrictionClause(...)` /
    `FilterQueryUtils.formOrderByClause(...)`.
  That suppression set would clear ~75 of the 85 without losing real
  signal.

## Recommendation

Not a Gemma 2.0 blocker. Bucket-A patterns are uniformly safe by
construction; bucket-D sites are either whitelist-enforced or dead.
The findings have value as a forcing function to add the suggested
spotbugs suppression so future SpotBugs runs surface only genuinely
new SQL_INJECTION_HIBERNATE shapes.

Optional follow-up (not blocking):
1. Prune `BibliographicReferenceDao.browse(int,int,String,boolean)` and
   its service overloads as dead code (removes 1 finding, simplifies
   the interface).
2. Pin a comment on `CharacteristicDaoImpl.browse(int,int,String,boolean)`
   stating the orderField-whitelist contract that lives in
   `CharacteristicBrowserController.java:81-93`.
3. Extend `spotbugs-exclude.xml` with the three pattern categories above
   to keep the next SpotBugs sweep readable.
