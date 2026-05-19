# Hibernate 6 `@Type` / UserType audit

**Scope:** Phase 3 Spring 6+ infrastructure modernization. Deeper check on
top of the Spring 6 deprecation hunt (commit `60d156b1b0` on
`worktree-spring6-deprecation-hunt`), motivated by the recent Hibernate
6.4 -> 6.6 bump.

**Base:** `phase2-acl-migrate` @ `08e760bdaf`. Hibernate version pinned at
`6.4.10.Final` (`pom.xml` line 1024). Audit applies to 6.4.x and the
imminent 6.6.x bump.

## Findings (counts)

| Surface                                              | Hits |
|------------------------------------------------------|-----:|
| `@Type` annotation on Java entity fields             |    0 |
| `@TypeDef` / `@TypeDefs` legacy registration         |    0 |
| `org.hibernate.annotations.Type` imports             |    0 |
| Java classes implementing `UserType`                 |    2 |
| Java classes extending `StandardBasicType`/`BasicType`|   0 |
| HBM XML `<type name="...">` elements                 |   29 |
| HBM `type="..."` attribute references to `MaterializedClobType` | 42 |
| HBM `type="..."` attribute references to `MaterializedBlobType` |  4 |

## Java UserType implementations

Both impls already use the typed `UserType<T>` shape (the
Hibernate 6 modern form). No migration needed.

- `gemma-core/src/main/java/ubic/gemma/persistence/hibernate/CompressedStringListType.java`
  -- `implements UserType<List<String>>, ParameterizedType`
- `gemma-core/src/main/java/ubic/gemma/persistence/hibernate/ByteArrayType.java`
  -- `implements UserType<Object>, ParameterizedType`

Both are referenced from HBM XML via `<type name="ubic.gemma.persistence.hibernate.*">`
with `<param>` children for the typed/parameterized configuration. This
is the supported HB6 wiring for `ParameterizedType` user types and
requires no change.

## HBM XML built-in type references

`<property type="org.hibernate.type.MaterializedClobType">` (42 hits)
and `MaterializedBlobType` (4 hits) reference HB6 built-in basic types
by their fully-qualified class name. Both classes still exist in
`org.hibernate.type` in HB 6.4 and HB 6.6; they remain registered
basic types resolvable from `hbm.xml`. The modern equivalent is
`StandardBasicTypes.MATERIALIZED_BLOB` / `MATERIALIZED_CLOB`, but
the FQN form is not deprecated as an HBM string-typed mapping. No
breaking change in the 6.4 -> 6.6 bump.

Other distinct `type="..."` values in HBM are either short Java type
names (`boolean`, `double`, `byte`, `text`, `longtext`), JDK FQNs
(`java.lang.Long`, `java.lang.String`, `java.util.Date`, `java.net.URL`),
or SQL type names used in `sql-type` attributes; none are deprecated.

## Annotations

There are **zero** uses of `org.hibernate.annotations.Type` (either the
deprecated string-based form `@Type(type = "...")` or the typed form
`@Type(SomeUserType.class)`) anywhere in the Java source tree, and zero
`@TypeDef` / `@TypeDefs`. The codebase uses HBM XML exclusively for the
two custom user types.

## Conclusion

**Clean.** No `@Type` migration is required. The two custom `UserType`
impls already use the typed HB6 shape. HBM XML references to
`MaterializedClobType` / `MaterializedBlobType` remain valid in HB 6.4
and HB 6.6 with no rename needed. No source changes applied.
