# CompositeSequenceGeneMapperServiceTest setUp failure — recce + GoldenPath partial-construction leak fix

Branch `agent-fix-csgenemap-pool-leak`, baseline `a0676bf084`.

## TL;DR

The slow-sweep findings doc (`SLOW_SWEEP_FINDINGS_2026_05_23.md`) classified the
`CompositeSequenceGeneMapperServiceTest` failure as a **CODE bottleneck —
pool exhaustion / connection leak**. After reproducing locally and tracing the
call chain, the proximate cause is **environmental, not a leak**: the test's
`blatCollapsedSequences` step constructs a `GoldenPathQuery(taxon)` which
opens a HikariCP pool to the host configured in
`gemma.goldenpath.db.host` (Paul's `~/Gemma.properties` overrides it to
`prod-db`). On any dev machine where `prod-db` is not in DNS / `/etc/hosts`,
the connection attempt fails with `UnknownHostException` → wrapped as
`CannotGetJdbcConnection`. Both `@Test` methods fail identically because both
re-run setUp from scratch — there is no carried-over state.

That said, while inspecting the path I did find **a small, real production-code
leak in `GoldenPath`'s constructor** that was unrelated to the immediate
symptom but worth fixing in the same scope: a partially-constructed
`GoldenPath` (e.g. `USE <db>` probe fails after the `HikariDataSource` was
already built) leaked the Hikari pool because try-with-resources at the call
site does not cover constructor failure. Fix landed in this commit.

## Reproduction

```bash
mvn -pl gemma-core verify \
    -DskipUnitTests=true \
    -DexcludedGroups=network \
    -Dit.test='CompositeSequenceGeneMapperServiceTest' \
    -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
    -Dgemma.hibernate.hbm2ddl.auto=create
```

Failure (post-fix; the test still fails for the same environmental reason —
the fix only prevents the Hikari leak, not the missing host):

```
CompositeSequenceGeneMapperServiceTest.setUp:138->loadData:212->blatCollapsedSequences:194
  » CannotGetJdbcConnection Failed to obtain JDBC Connection
  Caused by: java.net.UnknownHostException: prod-db: nodename nor servname provided
```

Stack walks down through:
- `aligner.processArrayDesign(ad, taxon, results)` (test line 194)
- `ArrayDesignSequenceAlignmentServiceImpl.getGoldenPathAlignments(...)`
- `new GoldenPathQuery(taxon)` → `super(taxon)` → `GoldenPath` constructor
- `createJdbcTemplateFromConfig(dataSource, taxon)` → `jdbcTemplate.execute("use hg38")`
- Hikari attempts to open a connection to `prod-db:3306` → DNS fails.

## The leak (real, but secondary)

`gemma-core/.../core/goldenpath/GoldenPath.java` lines 50-55 (pre-fix):

```java
public GoldenPath( Taxon taxon ) {
    this.dataSource = createDataSource( taxon );
    this.jdbcTemplate = createJdbcTemplateFromConfig( this.dataSource, taxon );
    this.searchedDatabase = createExternalDatabase( taxon );
    this.taxon = taxon;
}
```

If `createJdbcTemplateFromConfig` throws (e.g. `USE <db>` rejected, or Hikari
times out on connection acquisition), the `HikariDataSource` allocated on the
preceding line is dangling. Try-with-resources at the call site —
`try (GoldenPathQuery gpq = new GoldenPathQuery(taxon)) {...}` — does NOT
invoke `close()` because the constructor never returned: `gpq` was never
bound, so the try-resource cleanup path doesn't see anything to close. Every
failed construction leaks a Hikari pool with its housekeeping thread, even
when no JDBC connection was acquired.

In the immediate `UnknownHostException` case this is a small leak (Hikari's
housekeeping thread + a couple of internal data structures, no actual MySQL
connections). The leak is more material when `USE hg38` fails after a real
connection has been opened — that connection would be held until GC reaped
the dataSource. Either way, fix the leak rather than rely on GC.

## Fix shape (landed in this commit)

Wrap the post-`createDataSource` initialization in try-catch; on any
`RuntimeException`, close the dataSource before propagating:

```java
public GoldenPath( Taxon taxon ) {
    HikariDataSource ds = createDataSource( taxon );
    try {
        this.jdbcTemplate = createJdbcTemplateFromConfig( ds, taxon );
        this.searchedDatabase = createExternalDatabase( taxon );
        this.taxon = taxon;
        this.dataSource = ds;
    } catch ( RuntimeException e ) {
        ds.close();
        throw e;
    }
}
```

Single file change. Covers both subclasses (`GoldenPathQuery`,
`GoldenPathSequenceAnalysis`) because the fix is in the shared base constructor.

## Why the test still fails after the fix

The test depends on:

1. A reachable host at `${gemma.goldenpath.db.host}` (Paul's prod-db tunnel /
   the lab production server).
2. A populated UCSC mirror schema (`hg38` for human) on that host.
3. Local BLAT (`gfClient`) is not invoked in this path — the test uses
   pre-canned `.psl.gz` blat results. Only the GoldenPath query layer is
   exercised.

If item 1 or 2 is unmet, the test fails at `blatCollapsedSequences` regardless
of the leak fix. The failure is not a code bug per Paul's framing.

## What this means for the slow-sweep findings doc

The line in `SLOW_SWEEP_FINDINGS_2026_05_23.md`:

> CompositeSequenceGeneMapperServiceTest.setUp × 2 | CannotGetJdbcConnection
> ... | **CODE — pool exhaustion / connection leak** | ... Test failing TWICE in a row ... implies leak, not just slowness.

is wrong on this branch. The two methods fail identically not because the
first leaked the pool but because the **same environmental dependency** is
unmet on each setUp. The earlier `SLOW_SWEEP_INVENTORY_2026_05_23.md` row
classified the test correctly as "**externally dependent**":

> Currently fails with JDBC connection error trying to reach external BLAT
> — externally dependent.

The inventory recommendation (retag to `@Tag("integration")` + `@Tag("network")`
or fixture-cache the BLAT/JDBC dependency) is still the right next step IF
we want this test to run anywhere other than Paul's box. Not in scope for
this commit.

## Validation

- `mvn -pl gemma-core compile` — clean (post-fix).
- Focused failsafe (`-DskipUnitTests=true -Dit.test=CompositeSequenceGeneMapperServiceTest -DexcludedGroups=network`):
  still 2/0F+2E+0S — same `UnknownHostException` chain, just no longer
  leaks a Hikari pool on the way out.
- Default `mvn verify` baseline preserved (no semantic change for paths
  where `GoldenPath` constructs successfully; the catch only fires on
  constructor failure).

## Deferred follow-ups (not landed here)

- **Retag** `CompositeSequenceGeneMapperServiceTest` with
  `@Tag("network")` (in addition to `@Tag("slow")` + `@Tag("goldenPath")`)
  so accidental `-DexcludedGroups=network` overrides keep it excluded. The
  current state lets `-DexcludedGroups=network` re-include it even though it
  has a hard environmental dependency that may not be met. Trivial follow-up
  but a separate-commit concern per Paul's framing.
- **Stub or fixture-cache** the GoldenPath query layer so the test can run
  off-network. Likely a real refactor (or a Testcontainers-based MySQL stub
  with hg38 fixture). Worth a dedicated ticket.
- **Audit `gemma.goldenpath.db.host` default** — currently
  `${gemma.db.host}` (= `localhost`). For local dev that's reasonable, but
  Paul's overrides intentionally repoint to `prod-db`. Document this in
  `deploy/env.example` or similar so the next agent doesn't relearn the
  trap.

## Files touched

- `gemma-core/src/main/java/ubic/gemma/core/goldenpath/GoldenPath.java` —
  partial-construction leak fix in the constructor.
- `handoffs/CSGENEMAPPER_POOL_LEAK_RECCE.md` — this doc.
