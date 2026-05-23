# STATUS: `GET /rest/v2/datasets/{id}` bare-metadata recce

**Branch**: `fix-perf-dataset-bare-metadata` (off `phase2-acl-migrate` @ `3213fc767b`)
**Date**: 2026-05-22

## Smoke test

```
$ time curl -sS http://localhost:8080/rest/v2/datasets/1
{"apiVersion":"2.9.4","buildInfo":{...},"error":{"code":500,
 "message":"Cannot instantiate query result type 'java.lang.Number' due to:
  Result class must have a single constructor with exactly 1 parameters
  'java.lang.Number'"}}
real    0m0.267s
```

**Not 8s.** 267ms and a hard 500. The "8s" symptom in UIB's handoff §2 was
the previous bug (formula scan); the ACL fixes in `26c7e0d620` +
`3213fc767b` resolved the latency, but introduced a new failure mode the
recce caught: `resolveAclClassId` throws on every ACL-filtered request.

## Root cause

`AclQueryUtils.resolveAclClassId` (introduced by `739b5fc86c`) does:

```java
Number id = (Number) ss.createNativeQuery(
        "select id from acl_class where class = :c", Number.class )
    .setParameter( "c", className )
    .getSingleResult();
```

Hibernate 6 enforces that the result class passed to `createNativeQuery`
be a concrete type with a single-arg constructor matching the JDBC
column type. `java.lang.Number` is abstract — instantiation fails on
every call. The exception bubbles out of `addAclParameters`, the request
returns 500, and ACL filtering never gets to bind `:aclClassId`.

This affects EVERY ACL-filtered HQL endpoint, not just `/datasets/{id}`
— `/datasets`, `/datasets/count`, anything that goes through
`formAclRestrictionClause` + `addAclParameters`. The UI is wedged on the
500, not the 8s.

## Fix applied (this branch)

Drop the result-class argument and rely on the raw `Object`-cast path
that worked everywhere else in this codebase pre-HB6:

```java
Number id = (Number) ss.createNativeQuery(
        "select id from acl_class where class = :c" )
    .setParameter( "c", className )
    .getSingleResult();
```

MySQL `BIGINT` deserialises to `java.lang.Long` (a `Number`), so the
existing `.longValue()` call still works. 7-line change to one file.

Compile-clean against `gemma-core,gemma-rest`.

## Validation pending

- Restart Paul's container with the new build, re-curl `/rest/v2/datasets/1`,
  confirm 200 + sub-second.
- Re-run UIB's §1+§2 smoke (`/datasets`, `/datasets/count`,
  `/datasets/{id}`) end-to-end.

## What the recce did NOT do

- No `information_schema.processlist` capture — the request never got
  past the ACL-class lookup, so there was nothing to capture. Once the
  500 is cleared, if `/datasets/{id}` still lingers, re-run the
  processlist + EXPLAIN ladder.
- No `KILL` against prod (read-only mandate honoured).
