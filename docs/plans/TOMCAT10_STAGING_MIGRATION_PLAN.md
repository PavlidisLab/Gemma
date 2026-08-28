# Tomcat 10.1 migration for the staging instance (`gemma-staging` on chalmers)

Runbook for putting `gemma-rest.war` where `Gemma.war` currently serves, without
touching Apache, the vhost, or any port. Written 2026-08-28 against the live
state of chalmers; every fact below was read off the host rather than assumed,
and the appendix says how to re-check each one.

Companion documents:
- [`GEMMA_WEB_RETIREMENT_PLAN.md`](GEMMA_WEB_RETIREMENT_PLAN.md) — the module
  removal this is the deployment consequence of.
- [`GEMMA_REST_STANDALONE_ROADMAP.md`](GEMMA_REST_STANDALONE_ROADMAP.md) —
  gemma-rest as its own deployable.
- `Dockerfile` — the container path, which already solves this by running
  `tomcat:10.1-jdk25-temurin`. This plan is the non-container equivalent.

## Why this is needed

The staging host runs the OS-packaged Tomcat 9:

    tomcat-9.0.117-2.el9_8.noarch        CATALINA_HOME=/usr/share/tomcat
    tomcat-servlet-4.0-api-9.0.117       → javax/servlet/Servlet.class

gemma-rest is built against `jakarta.servlet` with `<tomcat.version>10.1.34</tomcat.version>`
(`pom.xml`, `gemma-rest/pom.xml`). Tomcat 9 will not load it — the WAR references
`jakarta.*` exclusively since the servlet 6 cutover.

So the WAR can be *deployed* to the instance harmlessly today (it just sits in the
directory, unreferenced), but it cannot be *served* until the instance runs
Tomcat 10.1.

## What makes this cheap: CATALINA_BASE vs CATALINA_HOME

Everything instance-specific — ports, config, logs, JVM settings, the webapp —
lives in `CATALINA_BASE`. The Tomcat *install* is a separate, swappable
`CATALINA_HOME`. Migrating the version means repointing one variable.

    systemd  tomcat@gemma-staging
      ├─ EnvironmentFile   /etc/tomcat/tomcat.conf
      │                      CATALINA_HOME=/usr/share/tomcat      ← what we override
      │                      TOMCAT_CFG_LOADED=1
      │                      JAVA_HOME=/usr/lib/jvm/jre           (superseded below)
      ├─ EnvironmentFile  -/etc/sysconfig/tomcat@gemma-staging    ← DOES NOT EXIST YET
      │                      (optional, loaded second, so it wins)
      └─ ExecStart         /usr/libexec/tomcat/server start
           └─ . /usr/libexec/tomcat/preamble
                ├─ re-sources tomcat.conf ONLY if TOMCAT_CFG_LOADED is empty
                │    → it is already "1", so our override is NOT clobbered
                ├─ . /etc/tomcat/conf.d/*.conf
                │    ├─ java-9-start-up-parameters.conf → JDK_JAVA_OPTIONS --add-opens ×5
                │    └─ pavlab-base.conf
                │         TOMCATS_BASE=/var/local/tomcat/
                │         CATALINA_BASE=$TOMCATS_BASE$NAME
                │         . $CATALINA_BASE/bin/setenv.sh
                └─ CLASSPATH = $CATALINA_HOME/bin/{bootstrap,tomcat-juli}.jar

Two consequences worth internalising:

1. **The `CATALINA_HOME` override survives.** `tomcat.conf` sets
   `TOMCAT_CFG_LOADED=1`, which systemd puts in the environment, so `preamble`
   skips re-sourcing it. A value set in `/etc/sysconfig/tomcat@gemma-staging`
   therefore reaches the JVM intact.
2. **`setenv.sh` is keyed to `CATALINA_BASE`, not `CATALINA_HOME`.** Changing the
   Tomcat install does *not* drop the instance's JVM settings — Java 25, ZGC,
   `-Xms10g -Xmx80g`, the JMX and JDWP ports all keep applying. This is the
   single most important reason the swap is low-risk: without it, the instance
   would silently fall back to `JAVA_HOME=/usr/lib/jvm/jre` from `tomcat.conf`
   and the `release 25` class files would not load.

## Verified current state

**Instance root** — `/var/local/tomcat/gemma-staging`. Note `/var/local` is itself
a symlink, so the canonical path is `/local/tomcat/gemma-staging`; `readlink -f`
output will not show `/var`.

    bin/setenv.sh          JAVA_HOME=/usr/lib/jvm/java-25-openjdk, -XX:+UseZGC,
                           -Xms10g -Xmx80g, JDWP 5559, JMX 5560
    conf/server.xml        one live connector (below)
    conf/context.xml       .
    conf/jmxremote.*       .
    lib/log4j2.xml         written by the gemma-web deploy (jenkins, Aug 26)
    Gemma.properties       runtime config
    Gemma.war              the gemma-web WAR
    webapps/ROOT.war  →  ../Gemma.war       symlink, created Oct 2023
    webapps/ROOT/          exploded gemma-web (decorators/, error.jsp, bundles/)
    logs/  temp→/scratch/…  work→/scratch/…

The exploded application is `webapps/ROOT/`, **not** `webapps/Gemma/` — the
context comes from the `ROOT.war` symlink, so the WAR's own filename is
irrelevant. `/var/local/tomcat/gemma` (the dev instance) has the identical shape.

**The only live connector** — everything else in `server.xml` is inside XML
comments (the 8080 executor connector, both 8443 SSL connectors, and the AJP 8009
connector):

```xml
<Connector port="8100" protocol="HTTP/1.1" connectionTimeout="20000"
           redirectPort="8443" maxParameterCount="1000" />
```

A single plain `HTTP/1.1` connector is valid unchanged in Tomcat 10.1. There is
no APR connector (deprecated in 10.1) and no AJP connector (which would have
needed `secretRequired`) in play.

**Apache** — `/etc/httpd/conf.d/staging-gemma.msl.ubc.ca.conf`, `httpd` active:

    ProxyPass        /resources/restapidocs/  http://localhost:8100/resources/restapidocs/
    ProxyPass        /resources  !
    ProxyPass        /            http://localhost:8100/  retry=0 nocanon
    ProxyPassReverse /            http://localhost:8100/

Plain HTTP to `localhost:8100`; no `ajp://` anywhere in the vhost. TLS terminates
at Apache. Because the port is unchanged and the host is unchanged, **the vhost
needs no edit** — this is the whole reason the migration is invisible to the web
tier.

Note the vhost is shaped around gemma-web: a long list of `ProxyPass … !`
exclusions (`/browse`, `/data`, `/wiki`, `/mainMenu.html`, …) that Apache serves
itself. Those are outside the scope of this migration but will need revisiting
once gemma-web is genuinely gone.

## Pre-flight

1. **`gemma-rest.war` is present.** The Jenkins `Deploy Gemma REST` stage puts it
   at `/var/local/tomcat/gemma-staging/gemma-rest.war` on any `hotfix-*` build.
   Confirm it is there and recent before starting.
2. **Check the common loader.** The instance has no `conf/catalina.properties`,
   so `common.loader` comes from `CATALINA_HOME`. If `${catalina.base}/lib` is
   not on it, `lib/log4j2.xml` stops being found. Both stock Tomcat 10.1 and the
   RHEL package normally include it; verify rather than assume:

       grep -n "^common.loader" /opt/apache-tomcat-10.1.x/conf/catalina.properties

3. **Confirm how `Gemma.properties` is discovered** and that the mechanism still
   holds under the new install. It sits at the instance root, which is not on the
   classpath by default.
4. **Know who owns `:8080`.** It is listening on chalmers even though
   `gemma-staging` has no live 8080 connector, and `ports.list` assigns 8081 to
   `gemma` and 8180 to `gotrack`. Not in this path, but do not assume 8080 is free.

## Migration

Root is required for steps 1, 2 and 5.

    # 1. install Tomcat 10.1.x (match or exceed the pom's 10.1.34)
    #    local disk, not /space — avoids an NFS dependency at boot
    cd /opt && curl -fLO https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.x/bin/apache-tomcat-10.1.x.tar.gz
    tar xzf apache-tomcat-10.1.x.tar.gz
    chown -R root:tomcat /opt/apache-tomcat-10.1.x

    # 2. point ONLY this instance at it
    echo 'CATALINA_HOME=/opt/apache-tomcat-10.1.x' > /etc/sysconfig/tomcat@gemma-staging

    # 3. swap which WAR is ROOT
    cd /var/local/tomcat/gemma-staging
    ln -sfT ../gemma-rest.war webapps/ROOT.war

    # 4. drop the stale exploded app so Tomcat re-expands from the new WAR
    rm -rf webapps/ROOT

    # 5. restart
    systemctl restart tomcat@gemma-staging

`server.xml`, `ports.list`, the Apache vhost and the systemd unit are all
untouched. The existing drop-in
`/etc/systemd/system/tomcat@gemma-staging.service.d/override.conf`
(`Restart=on-failure`) continues to apply.

## Verification

    systemctl status tomcat@gemma-staging
    ps -eo args | grep catalina | grep -o 'catalina.home=[^ ]*'     # → /opt/apache-tomcat-10.1.x
    curl -sS -o /dev/null -w '%{http_code}\n' http://localhost:8100/rest/v2/
    curl -sS https://staging-gemma.msl.ubc.ca/rest/v2/                 # through Apache
    ls /var/local/tomcat/gemma-staging/webapps/ROOT/                   # re-expanded from gemma-rest.war
    tail -100 /var/local/tomcat/gemma-staging/logs/catalina.*.log

Check the log for the Swagger UI at `/resources/restapidocs` too — that path has
its own Apache rule and is gemma-rest's sole home for it now.

## Rollback — two steps, not one

Because Tomcat 10.1 will not run the `javax`-based `Gemma.war` either, reverting
the symlink alone is **not** sufficient. Both changes must come back:

    ln -sfT ../Gemma.war /var/local/tomcat/gemma-staging/webapps/ROOT.war
    rm -rf /var/local/tomcat/gemma-staging/webapps/ROOT
    rm -f /etc/sysconfig/tomcat@gemma-staging
    systemctl restart tomcat@gemma-staging

`Gemma.war` is never deleted by this procedure, so the old application stays one
restart away for as long as the file is kept.

## Risks and open questions

- **Brief outage.** One process per port, so this is stop-then-start on 8100.
- **`common.loader` / `log4j2.xml`** — see pre-flight 2. The most likely thing to
  break quietly.
- **`Gemma.properties` discovery** — see pre-flight 3.
- **`/etc/tomcat/tomcat.conf` sets a javax-era global**
  `JAVA_OPTS=-Djavax.sql.DataSource.Factory=org.apache.commons.dbcp.BasicDataSourceFactory`,
  applied to every instance on the host. Probably inert for gemma-rest, but worth
  a look once it is up.
- **Staging is single-tenant.** `hotfix-*` is the only branch pattern targeting
  this instance, and the Jenkinsfile says so outright. Once `hotfix-1.32.8`-style
  branches and a 2.0 branch coexist, they take turns writing into this directory:
  the older branch's `Gemma.war` pushes become harmless once ROOT points
  elsewhere, but its `lib/log4j2.xml` push still lands on the shared instance.

## Afterwards: the other two instances

`/var/local/tomcat/gemma` on chalmers (dev) has the identical layout and can take
the same treatment with its own `/etc/sysconfig/tomcat@gemma`. Production is
`moe:/var/local/tomcat/gemma`, which this plan has **not** inspected — verify its
connector set and vhost separately before assuming symmetry.

## Appendix — how each fact was checked

    rpm -q tomcat tomcat-lib
    unzip -l /usr/share/java/tomcat/tomcat-servlet-api.jar | grep servlet/Servlet.class
    ps -eo args | grep -o 'catalina.home=[^ ]*'
    ls -l /var/local/tomcat/gemma-staging/webapps/ROOT.war
    readlink -f /var/local/tomcat/gemma-staging/webapps/ROOT.war
    python3 -c "strip XML comments from server.xml, print surviving <Connector>"
    ss -ltnp | grep -E ':8100|:8080|:8025'
    grep -nE '^[^#]*(ProxyPass|ProxyPassReverse)' /etc/httpd/conf.d/staging-gemma.msl.ubc.ca.conf
    systemctl cat tomcat@gemma-staging
    cat /usr/libexec/tomcat/server /usr/libexec/tomcat/preamble
    cat /etc/tomcat/conf.d/pavlab-base.conf
    grep -vE '^\s*#|^\s*$' /etc/tomcat/tomcat.conf
