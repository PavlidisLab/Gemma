# gsec ACL HQL Deprecation — Inventory and Migration Playbook

Phase 3 infrastructure work: retire HQL/Hibernate references to the legacy
`gemma.gsec.acl.domain.*` entity mappings (`AclObjectIdentity`, `AclEntry`,
`AclSid`, `AclGrantedAuthoritySid`, `AclPrincipalSid`) and replace them with
raw SQL via `JdbcTemplate` against the canonical Spring Security ACL schema
(`acl_class`, `acl_object_identity`, `acl_sid`, `acl_entry`).

**Why now:** gsec is being deprecated as Gemma migrates to stock Spring
Security 6 ACLs. As long as HQL queries hard-bind to gsec's entity classes,
the dependency is structural. Converting them to JDBC against the schema
(which is stable and Spring-Security-canonical) decouples Gemma from gsec
one query at a time.

**Schema (lowercase since Phase 2 upper→lower migration; uppercase tables
remain in prod as a safety net but do not write to them):**

```text
acl_class            (id, class)
acl_object_identity  (id, object_id_class FK→acl_class.id, object_id_identity,
                      parent_object FK→acl_object_identity.id, owner_sid FK→acl_sid.id,
                      entries_inheriting)
acl_sid              (id, principal, sid)   -- principal: 0=GrantedAuthoritySid, 1=PrincipalSid
acl_entry            (id, acl_object_identity FK, ace_order, sid FK,
                      mask, granting, audit_success, audit_failure)
```

---

## Inventory (bucket A = HQL string naming a gsec entity; bucket B =
`createQuery(... AclObjectIdentity.class)` / Hibernate session row materialisation)

| # | File | Line(s) | Bucket | Query gist | SQL-equivalent tables | Complexity | Status |
|---|------|---------|--------|------------|-----------------------|------------|--------|
| 1 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 162–166 | A | `select e.id from Entity e where e.id not in (select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type)` | `acl_object_identity` JOIN `acl_class` | **simple** (returns ids only) | **CONVERTED (kickoff)** |
| 2 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 189–194 | A | `select count(*) > 0 from Entity e where e.id = :id and e.id not in (select aoi.identifier from AclObjectIdentity aoi where aoi.type=:t and aoi.identifier=:id)` | `acl_object_identity` JOIN `acl_class` | **simple** (existence check) | **CONVERTED (kickoff)** |
| 3 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 132–142 | B | `select aoi from AclObjectIdentity aoi where aoi.type=:t and aoi.identifier not in (select e.id from Entity e)` — dangling-AOI detection. Materialises `List<AclObjectIdentity>` and reads `aoi.getIdentifier()` only. | `acl_object_identity` JOIN `acl_class` (left-anti against entity table) | medium (subquery against dynamic entity table) | pending |
| 4 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 214–220 | B | `select aoi from AclObjectIdentity aoi where aoi.type=:t and aoi.parentObject is null` — SecuredChild without parent. Caller mutates `aoi.setParentObject(...)`. | `acl_object_identity` JOIN `acl_class` where `parent_object IS NULL` | **high** (mutation via managed entity) | pending |
| 5 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 252–259 | B | Single-id form of #4. Same mutation pattern. | as #4 | high | pending |
| 6 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 292–301 | B | `select aoi from AclObjectIdentity aoi join aoi.parentObject parentAoi where aoi.type=:t and (parentAoi.type<>:pt or parentAoi.identifier <> (<dynamic-HQL>))` — incorrect parent detection. Materialises + mutates. | self-join on `acl_object_identity` JOIN `acl_class` (twice) + arbitrary inner HQL fragment via `expectedParentIdQueries` | **high** (dynamic subqueries) | pending |
| 7 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 334–344 | B | Single-id form of #6. | as #6 | high | pending |
| 8 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 377–383 | B | `select aoi from AclObjectIdentity aoi where aoi.type=:t and aoi.parentObject is not null` — SecuredNotChild with parent. Caller mutates `aoi.setParentObject(null)`. | `acl_object_identity` JOIN `acl_class` where `parent_object IS NOT NULL` | high (mutation) | pending |
| 9 | `gemma-core/.../authorization/acl/AclLinterServiceImpl.java` | 407–414 | B | Single-id form of #8. | as #8 | high | pending |
| 10 | `gemma-core/.../authorization/acl/ParentIdentityRetrievalStrategyImpl.java` | 92–96 | B | `select aoi from AclObjectIdentity aoi where aoi.type=:t and aoi.identifier=:id` — return the AOI as `ObjectIdentity`. Consumed by callers via `aoi.setParentObject(returned)` (Hibernate-managed mutation). | `acl_object_identity` JOIN `acl_class` | **medium-high** (return type is `ObjectIdentity`; conversion away from gsec AOI requires returning either a `Long acl_object_identity.id` for downstream JDBC update OR a stock Spring `ObjectIdentityImpl`) | pending |
| 11 | `gemma-core/.../persistence/util/AclQueryUtils.java` | 56–59, 62, 133–157 | A | HQL `formAclRestrictionClause()` — `, AclObjectIdentity aoi join aoi.ownerSid sid left join aoi.entries ace where ...`. **Used as a fragment composed into many other HQL queries across DAOs.** | `acl_object_identity` JOIN `acl_class` JOIN `acl_sid` LEFT JOIN `acl_entry` (the native equivalent at lines 172–229 already exists) | **structural / high impact** (every DAO that builds a query around `formAclRestrictionClause` would have to switch to the native flavour) | pending |
| 12 | `gemma-core/.../persistence/service/expression/experiment/ExpressionExperimentDaoImpl.java` | 2103–2105, 2259–2262 | B | `transformTuple` casts `row[1]` to `AclObjectIdentity` and `row[2]` to `AclSid`, then passes them to VO constructors. The HQL that produces these rows lives in `AbstractCuratableDao` / VO-loading machinery and is fed by `AclQueryUtils.formAclRestrictionClause`. | as #11 | **structural / high impact** (touches the EE VO loader; coupled to #11) | pending |

**Bucket C (type signature / Javadoc only, no Hibernate runtime dependency):**
`SecuredChild`, `ExpressionExperimentValueObject`, `ExpressionExperimentDetailsValueObject`,
`SecurityUtils`, `AclClassMetadata`, `Slice` (Javadoc). These are constructor
parameters or doc references and can be retyped once the consuming DAO sites
are converted (#11 / #12). Not on the critical path until then.

**Out of scope** (security/auth path — other agents own this):
`UserServiceImpl`, `AclAdvice`, `GemmaAclConfiguration` (already uses
`JdbcTemplate` directly), `AclEntryAfterInvocation*Provider` (uses gsec's
`afterinvocation.*`, not `domain.*`).

**REST + Web modules** (`gemma-rest/src/main/java`, `gemma-web/src/main/java`):
zero references to `gemma.gsec.acl.domain.*` types. The deprecation effort is
entirely a `gemma-core` story.

---

## Pattern: converting a simple HQL ACL query to JdbcTemplate

### Before

```java
@Autowired
private SessionFactory sessionFactory;

private void lintSecurableLackingObjectIdentity( Class<? extends Securable> clazz, ... ) {
    List<Long> list = sessionFactory.getCurrentSession()
        .createQuery( "select e.id from " + entityName + " e "
                + "where e.id not in (select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type)" )
        .setParameter( "type", clazz.getName() )
        .list();
    // ... iterate
}
```

### After

```java
@Autowired
public void setDataSource( DataSource dataSource ) {
    this.jdbcTemplate = new JdbcTemplate( dataSource );
}

private JdbcTemplate jdbcTemplate;

private void lintSecurableLackingObjectIdentity( Class<? extends Securable> clazz, ... ) {
    // Pull existing AOI identifiers via raw SQL against the canonical schema, then do the
    // set-difference in Java. Replaces an HQL subquery against gsec's AclObjectIdentity mapping.
    Set<Long> existingAoiIdentifiers = new HashSet<>( jdbcTemplate.queryForList(
        "select aoi.object_id_identity "
            + "from acl_object_identity aoi "
            + "join acl_class cls on aoi.object_id_class = cls.id "
            + "where cls.class = ?",
        Long.class, clazz.getName() ) );
    List<Long> allEntityIds = sessionFactory.getCurrentSession()
        .createQuery( "select e.id from " + entityName + " e" )
        .list();
    List<Long> list = new ArrayList<>();
    for ( Long id : allEntityIds ) {
        if ( !existingAoiIdentifiers.contains( id ) ) list.add( id );
    }
    // ... iterate
}
```

### Pattern notes

1. **Inject `DataSource`, build the `JdbcTemplate` in a setter** (not in a
   field initializer). `@Autowired` on a setter integrates with Spring's
   field-injection style used by the existing services.
2. **Keep the entity-side query as HQL.** Only the gsec-side subquery is
   converted. Splitting the two halves and doing the set-difference in Java is
   structurally simpler than building one giant native query that joins
   across Hibernate-managed entity tables and JDBC tables.
3. **Schema column names are lowercase, snake_case** —
   `acl_object_identity.object_id_identity`, `acl_object_identity.object_id_class`,
   `acl_class.class`. Phase 2 lower-cased these from the legacy
   `ACL_OBJECT_IDENTITY` / `OBJECT_ID_IDENTITY` casing.
4. **Class names go through `acl_class`, not a string column on
   `acl_object_identity`.** This is the canonical Spring Security shape (gsec
   used to denormalise the class name onto the AOI; the prod migration has
   already normalised).
5. **Position-based `?` placeholders** keep the JDBC call shorter than the
   named-parameter alternative. Use named (`NamedParameterJdbcTemplate`) only
   when there are many bindings or repetition.

### Mutation-path warning

For queries in bucket B that materialise `AclObjectIdentity` so the caller
can call `aoi.setParentObject(...)` (#4, #5, #6, #7, #8, #9, #10), conversion
is more involved:

- Reading: return `(acl_object_identity.id, object_id_identity, parent_object)`
  as a small DTO via `RowMapper`.
- Writing: replace the implicit Hibernate dirty-checking flush with an explicit
  `jdbcTemplate.update("update acl_object_identity set parent_object = ? where id = ?", ...)`.
- Caveat: the gsec `AclDaoImpl` does additional cache invalidation on AOI
  mutation; `GsecAclServiceAdapter` and the surrounding cache wiring need
  audit before bypassing the Hibernate write path.

---

## Latent bug surfaced during conversion (worth a follow-up)

Original `AclLinterServiceImpl#lintSecurableLackingObjectIdentity(clazz, identifier, ...)`
HQL (line 189-194 pre-conversion):

```hql
select count(*) > 0 from <Entity> e
 where e.id = :identifier
   and e.id not in (select aoi.identifier from AclObjectIdentity aoi
                    where aoi.type = :type and aoi.identifier = :identifier)
```

The result was assigned to a variable named `hasAoi`. The boolean returned
true when the entity row existed AND its id was **not** in the AOI table —
i.e. **lacks an AOI**, the opposite of `hasAoi`'s name. The downstream
branches then treated `hasAoi=true` as "all good, return" and `hasAoi=false`
as "create the missing AOI". Net effect: the lint had inverted semantics for
the single-id variant.

The conversion uses straightforward existence-check semantics
(`count(*) from acl_object_identity join acl_class ... where class=? and identifier=?`),
which matches the downstream branch comments and the bulk variant's behaviour.
This is technically a behaviour change relative to the pre-conversion HQL,
but it aligns the single-id variant with the bulk variant. Recommend a brief
review by someone who knows the linter's intended semantics before
extending this conversion further.

---

## Estimated remaining work

| Complexity | Sites | Est effort per site | Notes |
|------------|-------|---------------------|-------|
| simple (id/count returns, no mutation) | #1 (done), #2 (done) | 30 min | Pattern above. |
| medium (read-only AOI fetch consumed as a value) | #3, #10 | 1–2 h | RowMapper to a small DTO; rewire callers; smoke-test with `AclLinterServiceTest` + an integration test that exercises `ParentIdentityRetrievalStrategyImpl`. |
| high (read + mutation) | #4, #5, #6, #7, #8, #9 | 2–4 h each | Replace Hibernate dirty-flush with explicit `jdbcTemplate.update`. Audit cache invalidation in `GsecAclServiceAdapter`. Confirm with an integration test that lints and then re-lints to verify the fix landed. |
| structural | #11, #12 | 1–2 d | `formAclRestrictionClause` is woven into the EE / curatable DAO HQL builder. Switching to `formNativeAclJoinClause` everywhere means every DAO that uses it must move to native SQL. The native version already exists; the work is converting consumers. |

Total: roughly **2–3 weeks** to complete the deprecation if pursued
end-to-end. The structural items dominate; the easier ones could be picked
off in a single session each.

## Reference: existing JdbcTemplate usage in security code

`gemma-core/src/main/java/ubic/gemma/core/security/acl/GemmaAclConfiguration.java`
already uses `JdbcTemplate` directly against `acl_sid` / `acl_object_identity`
in the `GsecAclServiceAdapter.deleteSid(...)` path. That's the precedent for
the pattern proposed here.

`gemma-core/src/main/java/ubic/gemma/persistence/util/AclQueryUtils.java`
already has a `formNativeAclJoinClause` + `formNativeAclRestrictionClause` —
the same SQL the structural conversions in #11 / #12 would target.
