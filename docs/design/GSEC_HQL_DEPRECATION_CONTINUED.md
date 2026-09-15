# Phase 3 — gsec HQL deprecation (continued)

This is the second slice of the gsec HQL deprecation effort. The first slice
landed on branch `worktree-gsec-hql-v2` (commits `e9f78cc160` + `351a8bc574`)
and produced `GSEC_HQL_DEPRECATION.md` at repo root; that doc isn't on this
branch yet. The two docs will merge later.

Branch: `worktree-gsec-hql-continued`
Baseline: `08e760bdaf` (`phase2-acl-migrate`)

## Sites converted in this slice

Two medium-risk sites (AOI fetched, identifier read; no dirty-flush
mutations). High-risk mutation sites (lines 214/252/292/334/377/407 in
`AclLinterServiceImpl`) deliberately deferred — they call
`aoi.setParentObject(...)` on managed entities, and the gsec
`AclObjectIdentity` mapping is `mutable="false"`, so the path through which
those writes actually persist needs a separate audit.

| # | Site | Risk | Status |
|---|---|---|---|
| 1 | `ParentIdentityRetrievalStrategyImpl:92` | medium | converted |
| 2 | `AclLinterServiceImpl:132` (`lintAclObjectIdentityLackingSecurable`) | medium | converted |

## Site 1 — ParentIdentityRetrievalStrategyImpl

**Before** — HQL select against gsec's `AclObjectIdentity` entity mapping,
returning a managed Hibernate entity:

```java
return ( AclObjectIdentity ) sessionFactory.getCurrentSession()
        .createQuery( "select aoi from AclObjectIdentity aoi where aoi.type = :type and aoi.identifier = :identifier" )
        .setParameter( "type", parentType.getName() )
        .setParameter( "identifier", parentIdentifier )
        .uniqueResult();
```

**After** — raw SQL against the canonical Spring Security tables
(`acl_object_identity JOIN acl_class`), `RowMapper` builds a fresh
`AclObjectIdentity` carrying id + type + identifier:

```java
List<AclObjectIdentity> rows = jdbcTemplate.query(
        "select aoi.id, cls.class, aoi.object_id_identity "
                + "from acl_object_identity aoi "
                + "join acl_class cls on aoi.object_id_class = cls.id "
                + "where cls.class = ? and aoi.object_id_identity = ?",
        ( rs, rowNum ) -> {
            AclObjectIdentity oid = new AclObjectIdentity( rs.getString( "class" ), rs.getLong( "object_id_identity" ) );
            oid.setId( rs.getLong( "id" ) );
            return oid;
        },
        parentType.getName(), parentIdentifier );
// uniqueResult() semantics: rows.isEmpty() -> null;
// rows.size() > 1 -> IllegalStateException (defensive — schema makes it impossible).
```

The `SessionFactory` field is dropped from the strategy (no longer needed).
Caller-side semantics preserved: downstream code casts to `AclObjectIdentity`
and reads three fields (`id`, `type`, `identifier`); the gsec mapping is
`mutable="false"`, so the prior path's "managed" status bought nothing for the
mutation paths anyway. Those mutations go through `JdbcMutableAclService` /
`AclDao` (JDBC), not through Hibernate.

## Site 2 — AclLinterServiceImpl line 132

**Before** — HQL "not in" subquery against gsec's `AclObjectIdentity`,
returning a list of managed entities, then `deleteAcl` called on each:

```java
List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
        .createQuery( "select aoi from AclObjectIdentity aoi "
                + "where aoi.type = :type and aoi.identifier not in (select e.id from " + sessionFactory.getMetamodel().entity( clazz ).getName() + " e)" )
        .setParameter( "type", clazz.getName() )
        .setReadOnly( !config.isApplyFixes() )
        .list();
// for each aoi: aclService.deleteAcl( aoi, true ); results.add(... aoi.getIdentifier() ...)
```

**After** — JDBC pulls existing AOI identifiers for the class via
`acl_object_identity JOIN acl_class`; Hibernate gets the entity ids; the
dangling-set is computed in Java. For the `applyFixes` branch we construct a
fresh `AclObjectIdentity(class, identifier)` to hand to `aclService.deleteAcl`
(`MutableAclService.deleteAcl` accepts any `ObjectIdentity`):

```java
List<Long> aoiIdentifiers = jdbcTemplate.queryForList(
        "select aoi.object_id_identity "
                + "from acl_object_identity aoi "
                + "join acl_class cls on aoi.object_id_class = cls.id "
                + "where cls.class = ?",
        Long.class, clazz.getName() );
Set<Long> entityIds = new HashSet<>( sessionFactory.getCurrentSession()
        .createQuery( "select e.id from " + sessionFactory.getMetamodel().entity( clazz ).getName() + " e" )
        .list() );
List<Long> danglingIdentifiers = new ArrayList<>();
for ( Long aoiId : aoiIdentifiers ) {
    if ( !entityIds.contains( aoiId ) ) danglingIdentifiers.add( aoiId );
}
// for each identifier: aclService.deleteAcl( new AclObjectIdentity( clazz, identifier ), true );
```

Pattern mirrors the bulk variant of `lintSecurableLackingObjectIdentity` from
the first slice (`worktree-gsec-hql-v2`).

## Gotchas (new in this slice)

1. **`mutable="false"` on the gsec `AclObjectIdentity` mapping.** This matters
   for the high-risk bucket (deferred). The mapping says Hibernate will not
   dirty-flush mutations — so callers doing `aoi.setParentObject(parentAoi)`
   on a managed AOI rely on something *other than* Hibernate's auto-flush to
   persist that write. The mutation must flow through `AclDao` / JDBC
   somewhere. When those sites are converted, the new strategy of returning a
   non-managed `AclObjectIdentity` from `ParentIdentityRetrievalStrategy` is
   safe *only because* of `mutable="false"`. Document this in the high-risk
   migration playbook.

2. **`deleteAcl` accepts any `ObjectIdentity`.** Spring Security's
   `MutableAclService.deleteAcl(ObjectIdentity, boolean)` does not require the
   caller to hand it a managed gsec entity — type + identifier suffice. The
   `JdbcMutableAclService` adapter (and gsec's overlay) resolves the row by
   `(acl_class.class, object_id_identity)` internally.

3. **Removed unused `SessionFactory` autowire.** After the conversion,
   `ParentIdentityRetrievalStrategyImpl` no longer touches Hibernate at all.
   Dropping the `@Autowired SessionFactory` field cleans up the bean's
   dependencies and is one fewer cross-wire to audit when retiring Hibernate
   on the gsec entities later.

## Tests added

- `gemma-core/src/test/java/ubic/gemma/core/security/authorization/acl/ParentIdentityRetrievalStrategyTest.java`
  (new). Three cases: happy path (seeded AOI returned with id+type+identifier),
  empty path (no AOI → null), null-identifier short-circuit.
- `gemma-core/src/test/java/ubic/gemma/core/security/authorization/acl/AclLinterServiceTest.java`
  (extended). Two new cases for `lintAclObjectIdentityLackingSecurable`:
  reports dangling, empty path.

Tests were compiled but NOT run (gemdtest concurrency reservation). The next
session that owns gemdtest should run `mvn verify -pl gemma-core` and confirm.

## Compile result

```
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl gemma-core compile test-compile -DskipTests -q
# (clean — no warnings, no errors)
```

## Files touched

- `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/ParentIdentityRetrievalStrategyImpl.java`
- `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/AclLinterServiceImpl.java`
- `gemma-core/src/test/java/ubic/gemma/core/security/authorization/acl/ParentIdentityRetrievalStrategyTest.java` (new)
- `gemma-core/src/test/java/ubic/gemma/core/security/authorization/acl/AclLinterServiceTest.java`
- `GSEC_HQL_DEPRECATION_CONTINUED.md` (this file)
