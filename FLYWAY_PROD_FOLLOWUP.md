# Flyway production wiring — follow-on session

The H2 test path is now Flyway-managed (see `gemma-core/src/main/resources/db/migration/h2/V1__hibernate_baseline.sql`, `V2__schema_extras.sql`, `V3__seed_data.sql`, and the `Flyway` bean in `BaseDatabaseTest`). Production MySQL is still on implicit Hibernate-generated schema with `hbm2ddl.auto=update`. This doc tracks what the next session needs to do to flip prod to Flyway too.

## Why this is its own session

- We need a real prod schema dump as the source of truth, not Hibernate's view of what prod *should* look like. The two have drifted over years of manual DBA-applied migrations in `gemma-core/src/main/resources/sql/migrations/db.*.sql`.
- The cutover is irreversible-without-rollback in the sense that once Flyway baselines a live database, every future change has to go through Flyway. Ops sign-off + a rehearsed rollback plan is mandatory.
- The H2 baseline (`V1__hibernate_baseline.sql`) is the Hibernate metadata's view of the schema, not the prod schema. A separate `db/migration/mysql/V1__prod_baseline.sql` should come from `mysqldump --no-data` against staging or prod.

## Concrete steps for the follow-on session

1. **Capture the canonical prod schema dump.** From a freshly-restored staging clone of prod:
   ```bash
   mysqldump --no-data --routines --triggers --skip-add-drop-table --skip-comments \
       -h <staging-host> -u root -p gemd > V1__prod_baseline.sql
   ```
   Strip `AUTO_INCREMENT=NN` values and `DEFINER=` clauses. Manually diff against
   `gemma-core/src/main/resources/db/migration/h2/V1__hibernate_baseline.sql` to
   catalogue Hibernate-vs-real-prod drift (column casing, FK names, index
   variants, charset/collation). Most diffs should be cosmetic; flag any that
   represent actual schema gaps (Hibernate would emit X but prod has Y or
   vice versa) and reconcile.

2. **Add the `flyway-mysql` module.** Flyway 10+ split MySQL out into a
   separate artifact:
   ```xml
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-mysql</artifactId>
     <version>${flyway.version}</version>
   </dependency>
   ```
   `flyway-core` is also needed (already a `test` dep — promote to `runtime`
   or move into the main `gemma-core` deps without `<scope>`).

3. **Drop `V1__prod_baseline.sql` under `db/migration/mysql/`.** Mirror the
   H2 layout. Add `V2__prod_schema_extras.sql` (the equivalent of H2's V2 —
   `acl_class` + `init-entities.sql` content + vendor-specific indices from
   `sql/mysql/init-entities.sql`) and skip V3 in prod (no seed data).

4. **Wire a production Flyway bean** in `applicationContext-hibernate.xml`
   (or a sibling `applicationContext-flyway.xml`) keyed on a non-test
   profile. The bean depends on `dataSource` and ships ahead of
   `sessionFactory` (declare `depends-on="flyway"` on the SessionFactory).

5. **Use baseline-on-migrate for the existing prod DB.** First boot after
   the cutover sees a fully-populated schema but no `flyway_schema_history`
   table. The config should be:
   ```java
   Flyway.configure()
       .dataSource(dataSource)
       .locations("classpath:db/migration/mysql")
       .baselineOnMigrate(true)
       .baselineVersion("1")          // matches V1__prod_baseline.sql
       .baselineDescription("Production baseline as of <YYYY-MM-DD>")
       .load();
   ```
   On the first run Flyway records V1 as already-applied and runs nothing
   else. From then on every DDL change is a new `V<n>__description.sql`
   under `db/migration/mysql/`.

6. **Switch production Hibernate to `validate`** (or `none` if the H2
   round-trip taught us that `validate` is too noisy). The decision
   hinges on whether the prod schema dump matches Hibernate's metadata
   exactly. If validate complains, document each diff and either accept
   `none` or adjust the baseline.

7. **Retire `sql/migrations/db.*.sql`** as the change-mechanism (keep the
   files in-tree for historical reference and as the chronological source
   for any forward-port-to-V<n>.sql migration). Subsequent migrations live
   under `db/migration/mysql/V<n+1>__*.sql` and Flyway applies them.

## Ops checklist (cutover day)

- [ ] Backup prod DB (`mysqldump --single-transaction --routines --triggers`).
- [ ] Have the rollback dump verified by restoring to a scratch instance.
- [ ] Coordinate downtime window (Flyway baseline + first migrate is usually
      < 10 s on a populated schema; reserve 5 minutes to be safe).
- [ ] Deploy with `hbm2ddl.auto=validate` (or `none`) AND the new Flyway
      bean. Confirm the `flyway_schema_history` table appears with a single
      V1 row marked `success=1`, `type='BASELINE'`.
- [ ] Smoke-test a few read paths in prod immediately.
- [ ] If validation fails, roll back the deploy (revert Hibernate to
      `update`, comment out the Flyway bean), restore DB if needed.

## Test plan for the follow-on session

- New: a MySQL-tier integration test that boots the prod-style Flyway bean
  against a freshly-restored staging-clone DB and verifies (a)
  `flyway_schema_history` shows the V1 baseline and (b) Hibernate validate
  succeeds.
- Existing: full `mvn verify` integration suite (already covers ~50
  Spring contexts pointing at the shared `gemdtest` MySQL DB) must stay
  green with `hbm2ddl.auto=validate` and the Flyway bean wired in.

## Known issue carried into the follow-on session

- `H2Dialect` under `MODE=MYSQL` maps `BIT` to JDBC `BOOLEAN`, so
  Hibernate-`validate` against the Flyway-built H2 schema fails on
  `acl_sid.principal` (expected BIT/INTEGER, got BOOLEAN). The H2 path
  runs `hbm2ddl.auto=none`; MySQL doesn't share the BIT/BOOLEAN quirk so
  validate should work against the real prod schema. Verify on day 1 of
  the follow-on session before locking it in.
