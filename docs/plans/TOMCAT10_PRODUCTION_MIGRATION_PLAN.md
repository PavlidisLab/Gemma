# Tomcat 10.1 migration for the production instance (`gemma` on moe)

Runbook for putting `gemma-rest.war` where `Gemma.war` currently serves on
production, without touching Apache, the vhost, or any port.

Written 2026-09-08 against the live state of moe. Every fact in "Verified
current state" was read off the host; the appendix says how to re-check each
one. This is the counterpart the staging plan anticipated at its
"Afterwards: the other two instances" section — **not a copy of it**. Five
things differ and each one changes a step.

Companion documents:
- [`TOMCAT10_STAGING_MIGRATION_PLAN.md`](TOMCAT10_STAGING_MIGRATION_PLAN.md) —
  the staging migration, completed 2026-09-03. Read it first; this document
  assumes it and only records the deltas in full.
- [`GEMMA_WEB_RETIREMENT_PLAN.md`](GEMMA_WEB_RETIREMENT_PLAN.md) — the module
  removal this is the deployment consequence of.
- `docs/design/FLYWAY_PROD_FOLLOWUP.md` + `prod-migrations.md` — the database
  half, which is a harder gate than anything here.

## Status

**Staging already runs this exact WAR against the production database.**
`staging-gemma` and `gemma` both resolve
`jdbc:mysql:replication://prod-db,prod-db,repl-db/gemd` as `gemmaadmin`, and
`prod-db` is `homer`. The two instances differ in `gemma.appdata.home`,
`gemma.hosturl` and the Tomcat instance config — not in the database. Since
2026-09-03 the 2.0 code has therefore been exercising the production schema
continuously.

That collapses most of what an untried deployment would normally be gated on.
What is left:

| # | Item | Kind | Where |
|---|---|---|---|
| 1 | `2.0.0-alpha-SNAPSHOT` — a master build hard-errors on the version guard | blocker, if deploying via Jenkins | Pre-flight 0 |
| 2 | `gemma.anonymousAuth.key` absent from prod `Gemma.properties` | blocker | Pre-flight 2 |
| 3 | The `scheduler` profile — eleven jobs, none of which has ever run in its 2.0 form | new exposure, prod-only | "Scheduled jobs" |
| 4 | Prod schema is not a contiguous Flyway prefix (V5, V23, V24, V25, V38 below V40) | pre-existing, not new | Pre-flight 6 |

Item 1 is avoidable: build the WAR locally and push it with
`gemma-rest/deploy.sh`, which skips the version guard and the CLI symlink flip
both. Item 4 is real but is **not** a reason to hold the Tomcat swap — the same
code is already reading that schema from staging. Item 3 is the only genuinely
new risk this migration introduces.

## Why this is needed

moe runs the OS-packaged Tomcat 9:

    tomcat-9.0.117-2.el9_8.noarch          CATALINA_HOME=/usr/share/tomcat
    tomcat-servlet-4.0-api-9.0.117         -> javax/servlet/Servlet.class

gemma-rest is built against `jakarta.servlet`. Tomcat 9 will not load it. The
WAR can be *deployed* harmlessly today — Jenkins rsyncs it to the instance
root, not to `webapps/`, so nothing references it — but it cannot be *served*
until the instance runs Tomcat 10.1.

## The five deltas from staging

Everything else about the mechanism is identical, and the staging plan's
"What makes this cheap: CATALINA_BASE vs CATALINA_HOME" section applies
verbatim. These five do not.

**1. The `conf/` symlinks point into the RPM tree, not a tarball.**

    staging:  conf/web.xml -> /usr/local/tomcat/conf/web.xml      (9.0.94 tarball)
    prod:     conf/web.xml -> /usr/share/tomcat/conf/web.xml      (RPM 9.0.117)

Same eight files, same reason for moving them (`/usr/share/tomcat/conf/web.xml`
declares `xmlns="http://xmlns.jcp.org/xml/ns/javaee"`), different source path.
Migration step 3 must be rewritten, not copied — the staging plan says so at
its line 420.

**2. Tomcat 10.1 is not installed on moe.** `/opt/apache-tomcat*` does not
exist. Staging's step 1 was already done by the time that runbook was executed;
here it is real work. `/` has 93 G free, so space is not a concern.

**3. There is a live AJP connector.** `conf/server.xml` has two uncommented
connectors, where staging had exactly one:

```xml
<Connector port="8181" protocol="HTTP/1.1" connectionTimeout="20000"
           redirectPort="8544" maxHttpHeaderSize="65536" />
<Connector port="8110" protocol="AJP/1.3" redirectPort="8544"
           packetSize="65536" maxHttpHeaderSize="65536" />
```

8110 is listening on `127.0.0.1`. **Nothing uses it** — there is no `ajp://`
anywhere in `/etc/httpd/conf.d/`, and the gemma vhost proxies plain HTTP to
`localhost:8181`. Under Tomcat 10.1 this connector has no `secret` and
`secretRequired` defaults to `true`, so it will refuse to start and log an
error on every boot. Comment it out during the window (step 3b).

Also note the HTTP connector does **not** set `maxParameterCount`. Tomcat 10.1
lowers that default from 10000 to 1000. Fine for a REST API; staging carried an
explicit `maxParameterCount="1000"` inherited from the `minimal_test_instance`
template, and prod never got it.

**4. Prod runs the `scheduler` profile.**

    staging:  -Dspring.profiles.active=production,metrics
    prod:     -Dspring.profiles.active=production,scheduler,metrics

This is the single biggest behavioural difference between the two instances, and
— because they share a database — it is the *only* part of the 2.0 WAR that
staging does not already exercise. See "Scheduled jobs" below.

**5. `gemma2.msl.ubc.ca` already exists** as a vhost, serving
`/space/web/gemma-ui/gemma2testing` with `/rest/v2/` proxied to
`frink.msl.ubc.ca:8080`. That gives a rehearsal surface staging never had:
repoint that one `ProxyPass` at `moe:8181` and the production WAR can be
validated against the production database without touching
`gemma.msl.ubc.ca` or the ROOT symlink. Strongly recommended as step 5a.

Minor differences, recorded so nobody trips on them:

- `/var/local` on moe is a **real directory**, not a symlink. On chalmers it is
  a symlink to `/local`, so `readlink -f` output differs between the hosts.
- moe's instance git repo (see below) **tracks `webapps/ROOT.war`**; the
  staging repo does not. The ROOT flip will therefore show up as a tracked
  change on moe and should be committed.

## Verified current state (moe, 2026-09-08)

**Instance root** — `/var/local/tomcat/gemma`, owned `tomcat:pavlab`.

    bin/setenv.sh          JAVA_HOME=java-25-openjdk, -XX:+UseZGC, -Xms10g -Xmx140g,
                           JMX, -Dgemma.log.dir, -Dgemma.slack.token (plaintext),
                           -Dspring.profiles.active=production,scheduler,metrics
                           NO -Dlog4j2.configurationFile
    conf/server.xml        real file; HTTP 8181 + AJP 8110 (see delta 3)
    conf/context.xml       real file
    conf/jmxremote.*       real files
    conf/Catalina/localhost/   empty -- no per-context descriptors to port
    conf/*                 the other eight are symlinks -> /usr/share/tomcat/conf/
    Gemma.properties       runtime config, mode 0660 tomcat:pavlab-sa
    Gemma.war              106 MB, jenkins, 2026-08-26 16:09
    lib/log4j2.xml         written by the last gemma-web deploy, 2026-08-26
    webapps/ROOT.war  ->  ../Gemma.war        symlink, created Sep 2017
    webapps/ROOT/          exploded gemma-web
    logs/  temp -> /scratch/gemma/tomcat/temp   work -> /scratch/gemma/tomcat/work
    .git/                  see "Instance configuration is in git"

The exploded application is `webapps/ROOT/`, not `webapps/Gemma/` — the context
comes from the `ROOT.war` symlink, so the WAR's own filename is irrelevant.

**Running JVM** — already Java 25 (`/usr/lib/jvm/java-25-openjdk/bin/java`),
ZGC, `-Xms10g -Xmx140g`. Note that JDK tree has no `bin/javac`; irrelevant for
running Tomcat, relevant if anything tries to compile there.

**systemd plumbing — identical to staging, so the override mechanism carries
over unchanged:**

    tomcat@.service
      EnvironmentFile   /etc/tomcat/tomcat.conf         CATALINA_HOME=/usr/share/tomcat
                                                        TOMCAT_CFG_LOADED="1"
      EnvironmentFile  -/etc/sysconfig/tomcat@gemma     DOES NOT EXIST YET
      ExecStart         /usr/libexec/tomcat/server start
                          preamble re-sources tomcat.conf only if TOMCAT_CFG_LOADED is empty
                          conf.d/pavlab-base.conf -> CATALINA_BASE, sources bin/setenv.sh

`CATALINA_HOME` is assigned in exactly one place across `tomcat.conf`,
`/etc/tomcat/conf.d/` and `bin/setenv.sh` (`tomcat.conf:26`), and
`TOMCAT_CFG_LOADED="1"` is already in the environment when `preamble` runs, so
a value placed in `/etc/sysconfig/tomcat@gemma` reaches the JVM intact. There is
an existing drop-in `/etc/systemd/system/tomcat@gemma.service.d/override.conf`
(`Restart=on-failure`) which continues to apply.

**Apache** — `/etc/httpd/conf.d/gemma.msl.ubc.ca.conf`, `httpd` active:

    ProxyPass        /resources/restapidocs/  http://localhost:8181/resources/restapidocs/ retry=0 nocanon
    ProxyPassReverse /resources/restapidocs/  http://localhost:8181/resources/restapidocs/
    ProxyPass        /resources  !
    ProxyPass        /            http://localhost:8181/ retry=0 nocanon
    ProxyPassReverse /            http://localhost:8181/

Plain HTTP to `localhost:8181`, no `ajp://`. TLS terminates at Apache. Port and
host are unchanged by this migration, so **the vhost needs no edit.**

The vhost is shaped around gemma-web: a long list of `ProxyPass ... !`
exclusions (`/browse`, `/data`, `/dist`, `/annots`, `/ws/xml`,
`/datasetdownload`, `/phenocarta/*`, `/wiki`, `/mainMenu.html`, `/server-info`,
`/qos`, `/cellbrowser`, `/icons`) served from `/var/www/gemmaHome` and
`/space/web/gembrow/production`. Those are outside this migration's scope but
will need revisiting once gemma-web is genuinely gone. `/resources` in
particular is an `Alias` to `/var/www/gemmaHome/gemma-devsite` with a
`restapidocs/` hole punched through it to Tomcat — that hole is what serves
Swagger UI and it must keep working.

**Database** — `gemma.db.url` is
`jdbc:mysql:replication://prod-db,prod-db,repl-db/gemd`. `prod-db` resolves to
`137.82.176.37 = homer.pavlab.msl.ubc.ca`, `repl-db` to
`137.82.176.32 = lenny`. So the probe recorded in `prod-migrations.md`, which
was run against `homer/gemd`, was run against the real production database.

**`Gemma.properties` sentinel audit** (status only, never values):

    mail.username             set
    gemma.db.password         set
    gemma.runas.password      set
    gemma.agent.password      set
    gemma.anonymousAuth.key   ABSENT -> resolves to XXXXXXXX -> THROWS (pre-flight 2)
    gemma.hibernate.hbm2ddl.auto  ABSENT -> empty -> Hibernate will NOT touch the schema

That last line is the good news: `DatabaseSchemaUpdatePopulator` is a no-op stub
and `gemma.hibernate.hbm2ddl.auto` is empty in `default.properties:292`, so the
2.0 WAR cannot mutate the production schema on boot. It also means nothing
applies the Flyway migrations for you — see pre-flight 6.

`gemma.search.dir=${tomcat.work.dir}/searchIndices` is set, which avoids the
Hibernate Search 7 placeholder-coercion trap.

### Instance configuration is in git

`/var/local/tomcat/gemma` is itself a git repository — the repo root and
`CATALINA_BASE` are the same directory. Branch `master`, **no remote**, root
commit `4a00d01 Initial import`. Tracked: `.gitignore`, `Gemma.properties`,
`bin/setenv.sh`, all of `conf/`, `lib/log4j2.xml`, `temp`, `work`, and
**`webapps/ROOT.war`**.

chalmers has the equivalent repos at `/var/local/tomcat/gemma-staging` and
`/var/local/tomcat/gemma`. The staging one carries a dead `upstream` remote
(`../minimal_test_instance/`, removed from disk some time after 2023-10-18) and
needs `git -c safe.directory=/local/tomcat/gemma-staging` to read, since it is
owned by another user.

**The two repos share no history** — moe does not have the staging repo's
objects and vice versa. You cannot cherry-pick chalmers' migration commit onto
moe; apply the equivalent diff by hand, with the symlink source changed from
`/usr/local/tomcat/conf/` to `/usr/share/tomcat/conf/`.

Commit each configuration change on moe as you make it. The repo is the only
record of what this instance's config looked like before the window.

## Scheduled jobs — the one thing staging does not cover

`gemma-staging` runs `production,metrics`; moe runs `production,scheduler,metrics`.
That single difference is the whole of this migration's untested surface, because
everything else about the WAR has five days of production-database mileage on
staging.

Sort the jobs by how much evidence exists for them:

- **Proven on staging against the production database.** `HomeStatsRefresher`,
  `DiffExGeneWarmupService`, `SubmittedTasksMaintenance`. These are `@Scheduled`
  beans with no profile of their own, and `AnnotationDrivenSchedulingConfig` is
  `@Profile({PRODUCTION, SCHEDULER})`, so staging has been running them since
  2026-09-03.
- **Same work as today, new wiring.** The nine Quartz triggers in
  `SchedulerConfig` — gene2Cs, the three ee2c jobs, ee2ad, batch info, the two
  monthly reports, index experiments. Prod runs all nine today under
  `Gemma.war`, from `applicationContext-schedule.xml`. 2.0 ports them to a
  `@Configuration` class with the same crons and the same targets, so the job
  bodies are exercised nightly already; it is the bean wiring that is new.
- **Never run anywhere.** `ScheduledSearchReindexer` (03:00 daily) and
  `JobReconciler` (every 5 min). Both are `@Profile(SCHEDULER)`, so staging has
  never instantiated them and master has no equivalent. **This is the actual new
  exposure.**

`ScheduledSearchReindexer` is the one to watch. `IndexerService.index()` sets
`purgeAllOnStart(true)`, so it is a destructive rebuild of the Lucene directory
for `ExpressionExperiment` and `ArrayDesign`, not an incremental update. On a
first boot there is no
`${gemma.appdata.home}/search/last_reindex.<Entity>.timestamp` marker, so both
classes count as stale and both get rebuilt at the first 03:00 after the
restart. On prod `gemma.appdata.home` is `/space/gemmaData` and
`gemma.search.dir` is `${tomcat.work.dir}/searchIndices` — different paths from
staging, so this rebuilds the indices real users search against.

If you keep `scheduler` on for the first boot, the cheapest hedge is to disable
just that one job, which needs no code change and no profile surgery:

    gemma.search.reindex.cron=-

`-` is Spring's `Scheduled.CRON_DISABLED`. Take it out once you have watched a
manual `gemma-cli searchIndex` succeed against the 2.0 code. See "Disabling
jobs, and enabling them one at a time" below for the full set of levers and for
what is *not* controllable today.

## Pre-flight

### 0. Version guard — a master build errors before it deploys anything

`.jenkins/Jenkinsfile:82-85`:

```groovy
if (productionBuild) {
    if (gemmaVersion.endsWith('-SNAPSHOT')) {
        error("A production build must not have a -SNAPSHOT suffix.")
    }
```

`hotfix-2.0` / `phase2-acl-migrate` are at `2.0.0-alpha-SNAPSHOT`. Merging that
to master fails in *Checkout scm* — no WAR, no CLI, no Maven artifacts. Bump to
a release version first.

Note also what a master build does **besides** the REST deploy, in the same
parallel block:

- **`Deploy Gemma CLI` sets `cliRef = 'production'`**, and
  `gemma-cli/deploy.sh:79-82` repoints
  `/space/opt/gemma-cli/refs/production` at the new build. That is an
  immediate, live cutover of the CLI curators and cron jobs run through — it is
  **not** covered by "the WAR sits inert until we flip ROOT". Today that symlink
  points at build `a29afd33` (2026-08-26); `refs/staging` already points at
  `7cba4a75` (the 2.0 tip). Gate the stage or accept the CLI cutover as part of
  the same window.
- **`Deploy artifacts`** publishes a non-SNAPSHOT release to the pavlab Maven
  repo, which burns those coordinates permanently.
- **`Deploy Maven website`** rewrites `${DATA_DIR}/gemma-devsite` and
  `${DATA_DIR}/baseCode-site` symlinks under `/space/gemmaData`. Harmless, but
  it does touch the production data directory.

### 1. `indexerService` — RESOLVED, no action needed

Recorded because an earlier draft of this document called it a hard startup
blocker. It is not, and the reasoning that produced that claim is a trap worth
naming.

`SchedulerConfig.java:245` injects `@Qualifier("indexerService") Object`. The
only implementation is `IndexerServiceImpl`, and in 2.0 it carries a bare
`@Service` — where master had an explicit `@Service("indexerService")`. Reading
just those two files, Spring's default naming gives `indexerServiceImpl`, the
qualifier does not match, and the context fails.

It does not, because the component scan does not use Spring's default naming:

    @ComponentScan( basePackages = { "ubic.gemma.core", "ubic.gemma.persistence" },
                    nameGenerator = BeanNameGenerator.class, ... )

`ubic.gemma.core.context.BeanNameGenerator` strips `"Impl"` from every generated
name, so `IndexerServiceImpl` registers as **`indexerService`** and the
qualifier resolves. Master's explicit annotation and 2.0's implicit generation
produce the same bean name; nothing regressed in the move to
`core/search/indexer/`.

Two follow-ups, neither blocking:

- The javadoc at `SchedulerConfig:59` and `:238` says `indexerService` is a
  service "that doesn't currently exist in this codebase" and that startup under
  `scheduler` "will fail at this bean". Both sentences are stale — they were
  written before `IndexerService` was reinstated — and they are what made this
  look like a live defect. Worth deleting.
- If you ever drop the custom `nameGenerator`, this qualifier breaks silently at
  runtime rather than at compile time. An explicit `@Service("indexerService")`
  would cost nothing and remove the coupling.

### 2. Add `gemma.anonymousAuth.key` to the instance `Gemma.properties`

Hard blocker, verified failing on moe today. `SentinelPropertyValidator`
(`24b31839c7`) is `@Profile({PRODUCTION, DEV})` and throws
`IllegalStateException` for any guarded key still matching `X{4,32}`;
`gemma.anonymousAuth.key` is absent from the prod file and falls through to
`XXXXXXXX` from `default.properties:151`.

Any random string works — it backs Spring Security's anonymous-token equality
check and is not shared with anything external. The escape hatch
`gemma.sentinels.ignore=true` exists but sets aside the check for *every* key;
prefer setting the value.

The `Gemma.war` currently serving predates the validator, which is why prod is
up today despite the gap.

Needs only the `tomcat` user; do it ahead of the window. Commit it.

### 3. Pin log4j at the instance config, or lose the log files

Verified absent: the running JVM has no `-Dlog4j2.configurationFile`, and
`bin/setenv.sh` declares `-Dgemma.log.dir` but not the config file. Add, beside
the `gemma.log.dir` line:

    export CATALINA_OPTS="$CATALINA_OPTS -Dlog4j2.configurationFile=$CATALINA_BASE/lib/log4j2.xml"

Without it, production application logging silently moves to journald.
gemma-rest carries `src/main/resources/log4j2.xml`, which lands at
`WEB-INF/classes/log4j2.xml`; `WEB-INF/classes` is searched ahead of the common
loader, so log4j-core resolves the WAR's copy and `lib/log4j2.xml` never loads.
The WAR's config is a single `Console` appender to `SYSTEM_OUT`, and the unit is
`Type=simple` with no `StandardOutput=`, so that is the journal.

What stops being written are the `RollingFile` appenders in `lib/log4j2.xml` —
`gemma.log`, `gemma-errors.log`, `gemma-warnings.log`, `gemma-audit.log`,
`gemma-annotations.log`, `gemma-javascript.log`, all under `${gemma.log.dir}`.
The property is JVM-wide, not per-context; fine here, since this instance serves
only ROOT.

`setenv.sh` is keyed to `CATALINA_BASE`, so this survives the Tomcat swap.
Needs only the `tomcat` user; do it ahead of the window. Commit it.

The failure mode is silent, so what is *not* an option is leaving this
undecided. If you deliberately prefer the journal, drop the line and say so in
the commit message.

### 4. Confirm the common loader on the new install

`common.loader` decides whether `${catalina.base}/lib` is searched. It comes
from `conf/catalina.properties`, which is a symlink into the RPM tree today and
will point at the new install after step 3. On the 10.1.59 tree used for
staging it reads:

    common.loader="${catalina.base}/lib","${catalina.base}/lib/*.jar","${catalina.home}/lib","${catalina.home}/lib/*.jar"

`${catalina.base}/lib` first, so `lib/log4j2.xml` resolves as it does under
Tomcat 9. Re-check if you install a differently-configured tree. Lower stakes
than it sounds: pre-flight 3 decides logging regardless.

### 5. `Gemma.properties` discovery — no action needed

Not a classpath lookup. `SettingsConfig.settingsPropertySources()` reads
`System.getenv("CATALINA_BASE")` and appends `Gemma.properties`, precedence
`-Dgemma.config` -> `$CATALINA_BASE` -> `$HOME`.
`/etc/tomcat/conf.d/pavlab-base.conf` exports `CATALINA_BASE`, and `setenv.sh`
sets no `-Dgemma.config`. The lookup never consults `CATALINA_HOME`, so the
Tomcat swap cannot affect it.

### 6. The database — pre-existing, and not a gate on this migration

Nothing in the runtime applies migrations: `DatabaseSchemaUpdatePopulator` is a
no-op stub, `gemma.hibernate.hbm2ddl.auto` is empty in
`default.properties:292`, and Flyway is wired only into the H2 test path and the
`flyway-maven-plugin`, never into the production Spring context. So the 2.0 WAR
will neither mutate the schema nor repair it.

`prod-migrations.md` probed `homer/gemd` and found the applied set is **not a
contiguous prefix**: `V5__raw_vector_ee_qt_index`,
`V23__scde_cascade_on_parent_delete`, `V24__pipeline_job_attempts`,
`V25__pipeline_batch_throttle` and `V38__vector_quantitation_type_not_null` are
missing *below* the V40 high-water mark. Baselining above a hole marks it
applied and Flyway will never run it. That probe is also stale — it lists
`V41__annotation_set_payload_longtext` where the branch now carries
`V41__ticket_payload.sql` and runs to V51.

**All of that is true and none of it is new.** `staging-gemma` points at the
same `gemd` on the same `prod-db`, so the 2.0 code has been reading and writing
this exact schema since 2026-09-03. Moving moe's Tomcat to the same WAR adds no
database exposure that staging is not already carrying. An earlier draft of this
document listed this as a blocker on the Tomcat swap; that was wrong.

It stays on the list for two narrower reasons:

- The gaps are mostly benign shapes — an index (V5), a `NOT NULL` constraint
  (V38), FK cascade behaviour (V23), a column widening (V41) — which is
  consistent with staging not tripping over them. `V24` and `V25` add columns to
  `PIPELINE_JOB` / `PIPELINE_JOB_BATCH`, so anything that maps those columns
  fails when it is first touched. Staging's traffic may simply never have gone
  there. Prod's will be broader.
- Flyway cutover still cannot proceed until the chain is made contiguous. That
  is its own workstream (`docs/design/FLYWAY_PROD_FOLLOWUP.md`), on its own
  schedule, and it is not what this runbook is waiting for.

## Migration

Pre-flight 2 and 3 need only the `tomcat` user and should be done ahead of the
window; neither takes effect until the restart in step 6. Everything below needs
root, and steps 1-6 are one maintenance window.

    # 1. install Tomcat 10.1.x (match or exceed the pom's 10.1.34; staging runs 10.1.59)
    #    local disk, not /space -- avoids an NFS dependency at boot
    #    DONE on moe 2026-09-08 17:57: /opt/apache-tomcat-10.1.59, root:tomcat.
    cd /opt && curl -fLO https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.59/bin/apache-tomcat-10.1.59.tar.gz
    tar xzf apache-tomcat-10.1.59.tar.gz
    chown -R root:tomcat /opt/apache-tomcat-10.1.59

    # 1b. MAKE conf/ READABLE BY THE tomcat USER -- required, not cosmetic.
    #     The tarball unpacks conf/ as 0700 root-only and the files inside as 0600.
    #     Step 3 below symlinks the instance's conf/* INTO this directory, and the
    #     service runs User=tomcat, which then cannot traverse it. See
    #     "The conf/ permission trap" below for what that silently costs.
    #     Use ONE recursive chmod. Do NOT write `chmod 0640 .../conf/*`: the glob is
    #     expanded by YOUR shell before sudo runs, your account cannot list a 0700
    #     directory, so `*` stays literal and you get
    #         chmod: cannot access '/opt/apache-tomcat-10.1.59/conf/*': No such file or directory
    #     even though the path is fine. Same trap as `sudo echo ... > file` in step 2.
    #     `X` grants execute only where it already exists for someone, so the directory
    #     goes 0700 -> 0750 and the files 0600 -> 0640, with no config file made executable.
    chmod -R g+rX /opt/apache-tomcat-10.1.59/conf
    #     verify as the SERVICE USER before going further -- your own account is not in
    #     group tomcat, so `ls` still failing for you proves nothing either way:
    sudo -u tomcat cat /opt/apache-tomcat-10.1.59/conf/logging.properties > /dev/null && echo OK

    # 2. point ONLY this instance at it
    #    The unit declares EnvironmentFile=-/etc/sysconfig/tomcat@%i; the leading
    #    `-` makes it optional, so creating the file is what activates it, and a
    #    missing or misnamed file fails SILENTLY -- Tomcat simply starts on 9.0.117
    #    again with no error. Use tee: with `sudo echo ... > file` the redirect runs
    #    in YOUR shell, before sudo, and fails with "Permission denied".
    echo 'CATALINA_HOME=/opt/apache-tomcat-10.1.59' | sudo tee /etc/sysconfig/tomcat@gemma
    cat /etc/sysconfig/tomcat@gemma          # verify

    # 3. repoint the config symlinks off the RPM tree
    #    NOTE: source is /usr/share/tomcat/conf, NOT /usr/local/tomcat/conf as on staging.
    #    web.xml is the one that must move: the 9.x copy is javaee-namespaced.
    cd /var/local/tomcat/gemma/conf
    for f in catalina.properties catalina.policy logging.properties web.xml \
             tomcat-users.xml tomcat-users.xsd \
             jaspic-providers.xml jaspic-providers.xsd; do
        ln -sfT /opt/apache-tomcat-10.1.59/conf/"$f" "$f"
    done
    #    server.xml, context.xml, jmxremote.* and Catalina/ are real files: leave them.

    # 3b. comment out the AJP connector in conf/server.xml (prod-only step)
    #     Nothing proxies to it -- no ajp:// anywhere in /etc/httpd/conf.d/ -- and
    #     under 10.1 it has no `secret` while secretRequired defaults true, so it
    #     will refuse to start and log an error every boot.
    #        <Connector port="8110" protocol="AJP/1.3" ... />
    #     Optionally also add maxParameterCount="1000" to the 8181 connector to make
    #     the 10.1 default explicit rather than inherited.

    # 4. decide the scheduler profile. Leaving it as-is
    #        -Dspring.profiles.active=production,scheduler,metrics
    #    is defensible: nine of the eleven jobs already run nightly on this host under
    #    Gemma.war, and the three @Scheduled beans have five days of staging mileage
    #    against the same database. Only ScheduledSearchReindexer and JobReconciler
    #    have never run anywhere.
    #    If you keep it on, disable just the destructive one for the first night by
    #    adding to Gemma.properties:
    #        gemma.search.reindex.cron=-
    #    See "Scheduled jobs -- the one thing staging does not cover".

    # 5. swap which WAR is ROOT, and drop the stale exploded app so Tomcat
    #    re-expands from the new WAR
    cd /var/local/tomcat/gemma
    ln -sfT ../gemma-rest.war webapps/ROOT.war
    rm -rf webapps/ROOT

    # 6. restart
    systemctl restart tomcat@gemma

    # 7. commit the instance config repo
    cd /var/local/tomcat/gemma && git add -A && git commit

`server.xml`'s live HTTP connector, `ports.list`, the Apache vhost and the
systemd unit are otherwise untouched.

### 5a. Optional rehearsal through `gemma2.msl.ubc.ca`

Between steps 4 and 5, the WAR can be validated on moe without touching
`gemma.msl.ubc.ca`. Note this is about validating *this instance* — the WAR
against the production database is already covered by staging. `gemma2.msl.ubc.ca` currently proxies
`/rest/v2/` and `/resources/restapidocs/` to `frink.msl.ubc.ca:8080`. Deploy
`gemma-rest.war` into a *second* Tomcat instance on its own port, or flip ROOT
and immediately point gemma2 at `localhost:8181` while `gemma.msl.ubc.ca` is
still on `Gemma.war`. Staging had no equivalent; use it.

### The conf/ permission trap

Verified on chalmers 2026-09-08, after the staging migration had been running
for five days. **Staging is currently running on container defaults for three
config files rather than the ones its symlinks name**, and nothing reported an
error.

`/opt/apache-tomcat-10.1.59/conf` is `drwx------ root:tomcat` on both hosts —
the tarball's own mode, dated Aug 13, with no POSIX ACL. Migration step 3
repoints the instance's eight `conf/*` symlinks into that directory, and the
unit is `User=tomcat`, so from that moment the running JVM cannot read any of
them.

The staging plan's step-1 note — "Nothing in the launch path reads conf/ as the
tomcat user, so this is not a blocker" — was written when the tarball was
unpacked but *before* step 3 repointed the symlinks. Step 3 is precisely what
makes the tomcat user read `conf/`.

Tomcat does not fail on this. It falls back, silently and per-file:

- `catalina.properties` unreadable -> Tomcat uses the copy bundled inside
  `catalina.jar`. That fallback's `common.loader` still includes
  `${catalina.base}/lib`, which is why `lib/log4j2.xml` still resolves and
  `gemma.log` keeps being written. Pre-flight 4 is satisfied by accident.
- `logging.properties` unreadable -> JULI falls back to a console handler, so
  no `catalina.<date>.log` is written at all. The staging plan's own
  verification section tells you to `tail` a file that had stopped existing.

  Treat a missing `catalina.<date>.log` as a *hint*, not as evidence. These
  files are only written when Tomcat starts or stops, not continuously — the
  staging directory holds one per restart day (Jun 23, Jun 29, Jul 7, ...), so a
  five-day gap is equally consistent with nobody having restarted. The last one
  before the fix was `catalina.2026-09-03.log`, timestamped 19:36, which is
  *after* the 19:34 symlink repoint: that content is the outgoing JVM logging
  its own shutdown through the config it had already loaded. The incoming JVM
  wrote nothing. Only the `sudo -u tomcat` denial above is proof.
- `web.xml` unreadable -> no global servlet descriptor, so no container-supplied
  `DefaultServlet` or `JspServlet`.
- `catalina.policy`, `tomcat-users.xml`, `jaspic-providers.*` are inert.

**Do not read a 200 from `/resources/restapidocs/` as proof that `conf/web.xml`
was loaded.** `gemma-rest/src/main/webapp/WEB-INF/web.xml:300-320` declares its
own `DefaultServlet`, mapped to `/resources/*`, `*.js`, `*.css`, `*.png` and so
on, specifically so the WAR does not depend on the container's global
descriptor. Static serving works whether or not the global `web.xml` was read,
which is what let this go unnoticed.

Fix both hosts (step 1b above). On chalmers it is the same two `chmod` lines
plus a `systemctl restart tomcat@gemma-staging`; confirm afterwards that
`logs/catalina.<date>.log` reappears.

**CONFIRMED on chalmers, 2026-09-08**, as the service's own identity:

    $ sudo -u tomcat cat /opt/apache-tomcat-10.1.59/conf/logging.properties > /dev/null
    cat: /opt/apache-tomcat-10.1.59/conf/logging.properties: Permission denied

This is not an inference from the missing log files any more. Staging has been
running on container defaults for all five readable-config files since
2026-09-03.

Note that an ordinary `pavlab` account can run neither this check nor
`journalctl -u tomcat@gemma-staging`, which needs `adm`, `systemd-journal`,
`wheel` or root. That is the other half of why it stayed quiet: the two commands
that reveal it are the two an ordinary account cannot run.

#### Restoring conf/ is itself a behaviour change

Fixing the mode makes Tomcat read the global `conf/web.xml` again, which
contributes a `/` -> `DefaultServlet` mapping and `*.jsp` -> `JspServlet` that
the application has been running without. Expect both to reappear; verify rather
than assume the merge semantics.

Checked against the WAR, and the blast radius is nil:

- The webapp root contains only `WEB-INF/` and the packaged `resources/`. There
  is **no `index.html` at the context root**, so a restored `/` mapping has
  nothing new to serve and unmapped paths keep 404ing.
- `/resources/restapidocs/` stays on the more-specific `/resources/*` prefix
  mapping the WAR declares, so `RestapidocsIndexRewriteFilter` keeps handling
  the directory URL exactly as it does today.
- gemma-rest ships no JSPs, so `JspServlet` is inert.
- `catalina.policy` is inert without a SecurityManager, and `tomcat-users.xml`
  is inert without the manager app.

What comes back is `logs/catalina.<date>.log`, which is the file the
Verification section below tells you to read when a context fails to start.

Sequence the fix: chalmers first, restart, confirm `catalina.<date>.log`
reappears and both endpoints still answer 200 — then moe, before its window.

**DONE on chalmers 2026-09-08 18:08.** `catalina.2026-09-08.log` reappeared,
`/rest/v2/` and `/resources/restapidocs/` both still 200, and the restored
banner finally confirms the install from its own bytes rather than from the
directory name:

    Server version name:   Apache Tomcat/10.1.59
    Server version number: 10.1.59.0
    Server built:          Aug 13 2026 16:59:10 UTC
    Command line argument: -Dcatalina.home=/opt/apache-tomcat-10.1.59
    Server startup in [54003] milliseconds

Two log lines at that boot are expected and benign; do not read either as a
migration fault when they reappear on moe:

    SEVERE [main] org.apache.catalina.core.AprLifecycleListener.init An incompatible
    version [1.3.0] of the Apache Tomcat Native library is installed, while Tomcat
    requires at least version [1.3.8] of Tomcat Native 1.x

  Logged at SEVERE, but Tomcat continues. APR/native is optional; the connector
  is a plain NIO `HTTP/1.1` one and TLS terminates at Apache, so nothing in this
  deployment uses it. The 1.3.0 library comes from the OS `tomcat-native`
  package, so expect the same line on moe. Silence it by removing the
  `AprLifecycleListener` from `conf/server.xml` if the noise bothers you — it is
  not required for the migration.

    WARNING [main] org.apache.lucene.internal.vectorization.VectorizationProvider.lookup
    You are running with Java 23 or later. To make full use of the Vector API,
    please update Apache Lucene.

  Cosmetic; a consequence of running Hibernate Search's Lucene on JDK 25.

## Verification

    systemctl status tomcat@gemma
    ps -eo args | grep catalina | grep -o 'catalina.home=[^ ]*'      # -> /opt/apache-tomcat-10.1.59
    ps -eo args | grep catalina | grep -o 'spring.profiles.active=[^ ]*'
    curl -sS -o /dev/null -w '%{http_code}\n' http://localhost:8181/rest/v2/
    curl -sS https://gemma.msl.ubc.ca/rest/v2/                       # through Apache
    ls /var/local/tomcat/gemma/webapps/ROOT/                         # re-expanded from gemma-rest.war

Check the Swagger UI at `/resources/restapidocs` came up — that path has its own
Apache rule punched through the `/resources` Alias and is gemma-rest's sole home
for it now. It ships inside the WAR at `resources/restapidocs/`, so its absence
means the WAR did not expand, not that Apache is misrouting.

Confirm the AJP connector is gone rather than failing:

    ss -ltn | grep 8110          # expect no output after step 3b

### Logging — where to actually look

    # with the pre-flight 3 pin (the RollingFile appenders keep working)
    tail -f /var/local/tomcat/gemma/logs/gemma.log
    tail -f /var/local/tomcat/gemma/logs/gemma-errors.log

    # without it (the WAR's Console appender, so stdout -> journald)
    journalctl -u tomcat@gemma -f

    # confirm which one is live
    ps -eo args | grep catalina | grep -o 'log4j2.configurationFile=[^ ]*'

Tomcat's own logging is unaffected either way — `catalina.*.log` and
`localhost_access_log.*.txt` come from `conf/logging.properties` (JULI), not
log4j.

Startup failures are the case worth rehearsing: if the context dies before log4j
initialises — the `gemma.anonymousAuth.key` throw in pre-flight 2 and the
`indexerService` throw in pre-flight 1 are both this shape — the stack trace
goes to `catalina.*.log`, not to `gemma-errors.log`. Look there first when the
application does not come up at all.

## Rollback — every change comes back, not just the ROOT symlink

Tomcat 10.1 will not run the `javax`-based `Gemma.war` either, so reverting the
ROOT symlink alone is **not** sufficient.

    ln -sfT ../Gemma.war /var/local/tomcat/gemma/webapps/ROOT.war
    rm -rf /var/local/tomcat/gemma/webapps/ROOT
    rm -f /etc/sysconfig/tomcat@gemma
    cd /var/local/tomcat/gemma/conf
    for f in catalina.properties catalina.policy logging.properties web.xml \
             tomcat-users.xml tomcat-users.xsd \
             jaspic-providers.xml jaspic-providers.xsd; do
        ln -sfT /usr/share/tomcat/conf/"$f" "$f"
    done
    # restore the AJP connector and the scheduler profile if you changed them
    git -C /var/local/tomcat/gemma checkout -- conf/server.xml bin/setenv.sh
    systemctl restart tomcat@gemma

`Gemma.war` is never deleted by this procedure — `gemma-rest/deploy.sh` rsyncs
one file and removes nothing — so the old application stays one restart away for
as long as the file is kept.

The two pre-flight edits do **not** need reverting. `gemma.anonymousAuth.key` is
a real value gemma-web reads too, and the `log4j2.configurationFile` pin names
the same `lib/log4j2.xml` gemma-web was already using — under a rollback it
re-states what was implicit. Leave both; they are what makes a second attempt
cheap.

Because the instance config is in git, `git -C /var/local/tomcat/gemma diff` is
the authoritative statement of what the window changed. Commit before the
window so that diff is meaningful.

## Risks and open questions

- **Brief outage.** One process per port; this is stop-then-start on 8181.
- **The four blockers** in the Status table above.
- **The AJP connector** — will fail to start under 10.1 if left uncommented.
- **Application logging moves to the journal** unless pre-flight 3 is done. The
  quietest failure here: the service comes up, answers requests, reports
  healthy, and `gemma.log` simply stops growing.
- **A Slack bot token sits in plaintext in `bin/setenv.sh`**, on the
  `-Dgemma.slack.token=` line of a group-readable file — and that file is
  committed to the instance git repo. Same finding as staging. Out of scope for
  the migration, but it should be rotated and moved into `Gemma.properties`
  (mode 0660, as the DB password already is).
- **`/etc/tomcat/tomcat.conf` sets a javax-era global**
  `JAVA_OPTS=-Djavax.sql.DataSource.Factory=org.apache.commons.dbcp.BasicDataSourceFactory`,
  applied to every instance on the host. Probably inert for gemma-rest; worth a
  look once it is up.
- **moe hosts other Tomcat instances** (`gotrack`, `varicarta`, `hbaset`, `rdp`)
  on the same RPM `CATALINA_HOME`. The `/etc/sysconfig/tomcat@gemma` override is
  `%i`-scoped, so they are unaffected — but do not "upgrade Tomcat on moe"
  globally.
- **The vhost's gemma-web exclusions** (`/browse`, `/data`, `/wiki`, ...) still
  point at `/var/www/gemmaHome` content that gemma-rest does not serve. They will
  keep working as static Aliases; they just stop being part of one application.

## Scheduled jobs: 2.0 vs the deployed master build

Master wires these in
`gemma-core/src/main/resources/ubic/gemma/applicationContext-schedule.xml`
(`profile="scheduler"`); 2.0 ports the same list to
`gemma-core/src/main/java/ubic/gemma/core/config/SchedulerConfig.java`. It is
close to a like-for-like port: ten triggers on master, nine on 2.0.

**Unchanged — same cron, same target, nine of them:**

| Trigger | Cron | Target |
|---|---|---|
| `expressionExperimentReportTrigger` | 1st of month 00:15 | `ExpressionExperimentReportService.generateSummaryObjects` |
| `batchInfoTrigger` | daily 00:30 | `BatchInfoRepopulationJob` |
| `gene2CsUpdateTrigger` | daily 00:40 | `TableMaintenanceUtil.updateGene2CsEntries` |
| `arrayDesignReportTrigger` | 1st of month 01:30 | `ArrayDesignReportService.generateArrayDesignReport` |
| `ee2cExperimentUpdateTrigger` | Mon-Fri 19:00 | `Ee2cUpdateJob` / `ExpressionExperiment` |
| `ee2cSampleUpdateTrigger` | Mon-Fri 19:10 | `Ee2cUpdateJob` / `BioMaterial` |
| `ee2cExperimentalDesignUpdateTrigger` | Mon-Fri 19:20 | `Ee2cUpdateJob` / `ExperimentalDesign` |
| `ee2adUpdateTrigger` | Mon-Fri 19:30 | `Ee2AdUpdateJob` |
| `indexExperimentsTrigger` | Mon-Fri 23:00 | `indexerService.index(ExpressionExperiment)` |

`indexExperimentsTrigger` is the one that no longer wires up — see pre-flight 1.

**Removed in 2.0 — one:**

- `whatsNewTrigger`, daily 00:15, `whatsNewService.generateWeeklyReport`.
  `generateWeeklyReport` no longer exists. `WhatsNewService` survives but is now
  consumed by `HomeStatsServiceImpl`, so the nightly report write is superseded
  by `HomeStatsRefresher` at 04:00. Nothing to migrate; noted so its absence
  from the Quartz log is not read as a fault.

**New in 2.0, gated on `scheduler` — two:**

- `ScheduledSearchReindexer.reindexStale` — daily 03:00
  (`gemma.search.reindex.cron`). Runs Hibernate Search's MassIndexer for
  `ExpressionExperiment` and `ArrayDesign` when the audit-trail or
  document-count staleness check trips. **`IndexerService.index()` sets
  `purgeAllOnStart(true)`, so this is a destructive rebuild of the entity's
  Lucene directory, not an incremental update.** On a first boot there is no
  `${gemma.appdata.home}/search/last_reindex.<Entity>.timestamp` marker, so both
  classes count as stale and both get rebuilt.
- `JobReconciler.tick` — every 5 min
  (`gemma.pipeline.reconciler.intervalMs`), reconciling stalled pipeline jobs.

**New in 2.0, and they fire under `production` alone — two:**

- `HomeStatsRefresher.refreshDaily` — daily 04:00
  (`gemma.homeStats.refresh.cron`), timed to follow the 03:00 reindex.
- `DiffExGeneWarmupService.warmTopGenes` — 5 min after boot, then every 6 h
  (`gemma.diffex.warmup.initialDelay` / `.fixedDelay`).

**Changed scope — one, and it is the subtle one:**

`SubmittedTasksMaintenance` (`@Scheduled(fixedDelay = 120000)`, every 2 min)
exists on both. On master, `<task:annotation-driven/>` sits *inside* the
`profile="scheduler"` block, so **every** `@Scheduled` method on master is
dormant unless `scheduler` is active. In 2.0 that moved to
`AnnotationDrivenSchedulingConfig`, which is
`@Profile({PRODUCTION, SCHEDULER})`.

The consequence for this migration: **dropping `scheduler` on 2.0 is not
equivalent to dropping it on master.** On master it silences everything; on 2.0
it silences Quartz, `ScheduledSearchReindexer` and `JobReconciler`, while
`SubmittedTasksMaintenance`, `HomeStatsRefresher` and `DiffExGeneWarmupService`
keep running under `production`.

`SchedulerFactoryBean` is created with no `DataSource`, so Quartz uses the
in-memory `RAMJobStore`. No trigger state persists across a restart, and there
is no job-store table to edit.

## Disabling jobs, and enabling them one at a time

**The honest answer: per-job enablement does not exist today.** The nine cron
expressions are Java string literals in `SchedulerConfig` (`t.setCronExpression(
"0 30 0 * * ?" )` and friends) with no property indirection. Only the
annotation-driven beans have knobs.

### Levers that exist right now, no code change

| Target | Lever | Effect |
|---|---|---|
| All nine Quartz triggers + `ScheduledSearchReindexer` + `JobReconciler` | remove `scheduler` from `-Dspring.profiles.active` in `bin/setenv.sh` | all-or-nothing |
| `ScheduledSearchReindexer` | `gemma.search.reindex.cron=-` | disabled (`Scheduled.CRON_DISABLED`) |
| `HomeStatsRefresher` | `gemma.homeStats.refresh.cron=-` | disabled |
| `DiffExGeneWarmupService` | `gemma.diffex.warmup.enabled=false` | disabled (explicit guard in the method) |
| `JobReconciler` | `gemma.pipeline.reconciler.intervalMs` | no disable value; set it very large |
| `SubmittedTasksMaintenance` | none | `fixedDelay = 120000` is hardcoded |

The `-` sentinel is Spring's `Scheduled.CRON_DISABLED`; it works for
`@Scheduled(cron = ...)` only, which is why the `fixedDelay` beans have no
equivalent.

**Trap: `quartzOn=false` does not disable anything.**
`default.properties:225` sets `quartzOn=false`, and `SchedulerConfig`'s javadoc
points at it, which reads as a master switch. It is not.
`SpringContextUtils:154` only *adds* the `scheduler` profile when `quartzOn` is
true; it never removes it. With `scheduler` already in
`spring.profiles.active`, the property is inert.

**Trap: pausing through JMX does not survive a restart.** `RAMJobStore` — see
above.

### Staged enablement — the smallest change that buys it

Make the trigger list filterable by a property. Roughly fifteen lines in
`SchedulerConfig`, no change to any job:

```java
@Value("${gemma.scheduler.enabledJobs:}")   // empty = all, as today
private String enabledJobs;

@Bean
public SchedulerFactoryBean schedulerFactoryBean( /* ...the nine triggers... */ ) {
    Map<String, Trigger> all = new LinkedHashMap<>();
    all.put( "batchInfo", batchInfoTrigger );
    all.put( "gene2Cs", gene2CsUpdateTrigger );
    // ...
    Set<String> wanted = StringUtils.hasText( enabledJobs )
            ? new HashSet<>( Arrays.asList( enabledJobs.split( "\\s*,\\s*" ) ) )
            : all.keySet();
    SchedulerFactoryBean factory = new SchedulerFactoryBean();
    factory.setTriggers( all.entrySet().stream()
            .filter( e -> wanted.contains( e.getKey() ) )
            .map( Map.Entry::getValue ).toArray( Trigger[]::new ) );
    return factory;
}
```

Then `gemma.scheduler.enabledJobs=gene2Cs,batchInfo` in `Gemma.properties`, and
each addition is a restart rather than a redeploy. Defaulting the property to
empty keeps today's behaviour for staging and dev, so nothing else has to
change.

Do not disable a trigger by giving it a cron that never fires — Quartz rejects
a trigger whose schedule yields no fire time and fails context startup.

### Running the same work by hand in the meantime

Every Quartz job but one has a `gemma-cli` equivalent, so booting without
`scheduler` costs coverage only in scheduling, not in capability:

| Job | CLI command |
|---|---|
| `gene2CsUpdateTrigger` | `gemma-cli updateGene2Cs` |
| `ee2c*UpdateTrigger` | `gemma-cli updateEe2c` |
| `ee2adUpdateTrigger` | `gemma-cli updateEe2Ad` |
| `arrayDesignReportTrigger` | `gemma-cli updatePlatformReports` |
| `batchInfoTrigger` | `gemma-cli fillBatchInfo` |
| `indexExperimentsTrigger` / `ScheduledSearchReindexer` | `gemma-cli searchIndex` |
| `expressionExperimentReportTrigger` | **no CLI equivalent** |

These run against the same production database with the same write footprint —
the gain is that you choose when, and watch one at a time. Note the CLI
`production` symlink is itself flipped by a master build (pre-flight 0), so
confirm which build `/space/opt/gemma-cli/refs/production` resolves to before
relying on it.

## Appendix — how each fact was checked

    ssh moe
    rpm -q tomcat tomcat-lib tomcat-servlet-4.0-api
    unzip -l /usr/share/java/tomcat/tomcat-servlet-api.jar | grep servlet/Servlet.class
    grep -m1 -o 'xmlns="[^"]*"' /usr/share/tomcat/conf/web.xml
    ps -eo args | grep '[c]atalina'
    ls -la /var/local/tomcat/gemma/ /var/local/tomcat/gemma/{conf,webapps,lib,bin}
    readlink -f /var/local/tomcat/gemma/webapps/ROOT.war
    find /var/local/tomcat/gemma/conf/Catalina -maxdepth 2
    python3 -c "strip XML comments from server.xml, print surviving <Connector>"
    ss -ltn | grep -E ':(8181|8110|8544)'
    grep -nE '^[^#]*(ProxyPass|ProxyPassReverse|Alias|ServerName)' \
        /etc/httpd/conf.d/gemma.msl.ubc.ca.conf
    grep -rn 'ajp://' /etc/httpd/conf.d/            # expect no hits
    systemctl cat tomcat@gemma
    cat /etc/tomcat/conf.d/pavlab-base.conf
    grep -rn CATALINA_HOME /etc/tomcat/tomcat.conf /etc/tomcat/conf.d/ \
        /var/local/tomcat/gemma/bin/setenv.sh       # expect exactly one hit
    grep -n TOMCAT_CFG_LOADED /etc/tomcat/tomcat.conf /usr/libexec/tomcat/preamble
    ls -la /etc/sysconfig/tomcat@gemma              # expect: does not exist
    getent hosts prod-db repl-db
    git -C /var/local/tomcat/gemma log --oneline -5
    git -C /var/local/tomcat/gemma ls-files
    df -h /opt

Option names only, so no secret is printed:

    grep -oE '\-D[a-zA-Z0-9._]+' /var/local/tomcat/gemma/bin/setenv.sh | sort -u

`Gemma.properties` sentinel audit — prints key status, never values:

    f=/var/local/tomcat/gemma/Gemma.properties
    for k in mail.username gemma.db.password gemma.runas.password \
             gemma.anonymousAuth.key gemma.agent.password \
             gemma.hibernate.hbm2ddl.auto; do
      v=$(grep -E "^\s*${k//./\\.}\s*=" "$f" | tail -1 | cut -d= -f2-)
      case "$v" in
        "")     echo "$k ABSENT" ;;
        XXXX*)  echo "$k SENTINEL" ;;
        *)      echo "$k set" ;;
      esac
    done

Which log4j configuration the WAR will use — run against the built artifact,
before it reaches the server:

    unzip -l gemma-rest/target/gemma-rest.war | grep log4j2.xml
    unzip -p gemma-rest/target/gemma-rest.war WEB-INF/classes/log4j2.xml \
        | grep -E '<(Console|RollingFile|File) '
    grep -E 'fileName=' /var/local/tomcat/gemma/lib/log4j2.xml
