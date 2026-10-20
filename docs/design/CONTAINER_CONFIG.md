# Container Config — Env-Var-Only Gemma Configuration

**Status**: implemented in `SettingsConfig.java` and `default.properties` (Phase 3 fix
of HIGH issue #1 from [CONFIG_AUDIT.md](CONFIG_AUDIT.md)).

Gemma can now boot **without** a `Gemma.properties` file on disk. All required
properties may be supplied by:

1. **Environment variables** — `GEMMA_FOO_BAR` is translated to the Gemma key
   `gemma.foo.bar`. Only keys that already appear in `default.properties` or
   `project.properties` are honoured; unknown variables are ignored. (Highest
   precedence — overrides anything on disk.)
2. **JVM system properties** — `-Dgemma.foo.bar=...` (also `-Dgemma.<key>` form
   to override a `<key>` declared without the `gemma.` prefix, e.g. `mail.host`
   → `-Dgemma.mail.host=...`). Filtered the same way as env vars.
3. **`Gemma.properties` on disk** — resolved in this order: `-Dgemma.config=<path>`,
   `$CATALINA_BASE/Gemma.properties`, `$HOME/Gemma.properties`. If none of these
   resolves Gemma now emits a `WARN` and continues; previously startup threw a
   `RuntimeException`.
4. **`default.properties` / `project.properties`** — shipped in the classpath,
   always last.

Properties without a default value (e.g. `gemma.db.url`, `gemma.db.user`,
`gemma.db.password`, `gemma.agent.password`, `gemma.runas.password`) will still
fail loudly when they remain unresolved, but the failure mode is now a clear
"could not resolve placeholder" at the `@Value` injection site rather than a
generic "Gemma.properties not found" at startup.

## Standard JVM placeholders

`default.properties` may now reference a small set of JVM-standard system
properties via `${...}`:

| Placeholder       | Typical value                                             |
|-------------------|-----------------------------------------------------------|
| `${java.io.tmpdir}` | `/tmp` (Linux), `/var/folders/.../T/` (macOS), `C:\...\Temp` (Windows) |
| `${user.home}`    | `$HOME`                                                   |
| `${user.dir}`     | CWD                                                       |
| `${user.name}`    | current user                                              |
| `${os.name}`      | e.g. `Linux`, `Mac OS X`, `Windows 11`                    |
| `${file.separator}`, `${line.separator}`, `${path.separator}` | platform-specific |

This made it possible to change the `gemma.appdata.home` default from
the Linux-only `/var/tmp/gemmaData` to the portable
`${java.io.tmpdir}/gemmaData`.

## Example: container startup with no on-disk Gemma.properties

```bash
docker run --rm \
  -e GEMMA_DB_HOST=mysql.internal \
  -e GEMMA_DB_PORT=3306 \
  -e GEMMA_DB_NAME=gemd \
  -e GEMMA_DB_USER=gemmauser \
  -e GEMMA_DB_PASSWORD="$(security find-generic-password -s gemma-db -w)" \
  -e GEMMA_DB_URL='jdbc:mysql://${gemma.db.host}:${gemma.db.port}/${gemma.db.name}?useSSL=true' \
  -e GEMMA_APPDATA_HOME=/srv/gemmaData \
  -e GEMMA_HOSTURL=https://gemma.example.com \
  -e GEMMA_ADMIN_EMAIL=admin@example.com \
  -e GEMMA_NOREPLY_EMAIL=noreply@example.com \
  -e GEMMA_SUPPORT_EMAIL=support@example.com \
  -e GEMMA_AGENT_PASSWORD="$(security find-generic-password -s gemma-agent -w)" \
  -e GEMMA_RUNAS_PASSWORD="$(security find-generic-password -s gemma-runas -w)" \
  -e GEMMA_ANONYMOUSAUTH_KEY="$(security find-generic-password -s gemma-anon -w)" \
  -e MAIL_HOST=smtp.example.com \
  -v gemma-data:/srv/gemmaData \
  -p 8080:8080 \
  ghcr.io/pavlidislab/gemma:1.32.7
```

Notes:

- `GEMMA_DB_URL` contains literal `${gemma.db.host}` etc.; those resolve at
  placeholder-substitution time against the other env vars + defaults, so you
  don't need to repeat host/port/name in the URL itself. (You may also pass a
  fully-built URL.)
- Secrets resolved via `security find-generic-password` per the keychain
  pattern documented in `~/.claude/CLAUDE.md`. In a real container orchestrator
  use Docker secrets / Kubernetes secrets / Vault.
- The `appdata.home` volume mount (`-v gemma-data:/srv/gemmaData`) is
  load-bearing — the JVM-tmpdir default works for ephemeral / test installs,
  but production needs a persistent path.

## What did NOT change

- The Spring profile inference bug (HIGH issue #3 — `dev` profile shares the
  prod `dataSource` bean) is unchanged. Containerized images should set
  `-Dspring.profiles.active=production` explicitly to avoid the `dev` fallback
  in `SpringContextUtils`.
- `CATALINA_BASE` and `$HOME` lookup paths remain. They're still honoured for
  back-compat — only the "throw if no file resolves" behaviour was relaxed.
- Properties with `XXXXXX` placeholder values in `default.properties`
  (`gemma.db.password`, `mail.username`, `mail.password`, etc.) still need an
  explicit override; the placeholder will be substituted literally if you
  forget to override and won't trigger a placeholder-resolution failure. This
  is a separate hardening pass (recommend MEDIUM follow-up: detect `XXXXXX`
  sentinel and fail at startup).

## See also

- [CONFIG_AUDIT.md](CONFIG_AUDIT.md) — full inventory of `@Value` sites,
  profiles, property files. HIGH issue #1 is now fixed; HIGH issues #2 (appdata
  Unix-only) is fixed by the `${java.io.tmpdir}` default; HIGH issue #3
  (Spring profile inference) is **not** addressed by this change.
