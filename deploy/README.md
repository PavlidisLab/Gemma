# `deploy/` — gemma-rest on Rocky Linux 9

This directory holds **sample configuration** for bringing up `gemma-rest` 2.0 on a Rocky Linux 9 host using rootful `podman` + `systemd` Quadlet container units, fronted by `Caddy` for TLS termination. It assumes the container image has already been built and pushed to a registry the host can pull from, that `podman` and `caddy` are installed via `dnf`, and that the host has DNS + outbound network for LetsEncrypt cert issuance. **These files are a starting point for the sysop, not a runbook to execute verbatim.** Every value that depends on the host environment is flagged with a `# TODO(sysop):` comment — `grep -rn 'TODO(sysop)' deploy/` to enumerate them.

## 5-step bring-up (after reviewing every `TODO(sysop)`)

1. **Install runtime**
   ```bash
   sudo dnf install -y podman caddy
   sudo systemctl enable --now caddy
   ```
2. **Place config**
   ```bash
   sudo install -d -m 0755 /etc/gemma
   sudo install -m 0600 deploy/env.example            /etc/gemma/env.conf
   sudo install -m 0644 deploy/gemma-rest.container.example \
                                                       /etc/containers/systemd/gemma-rest.container
   sudo install -m 0644 deploy/Caddyfile.example      /etc/caddy/Caddyfile
   sudo install -m 0644 deploy/logrotate-gemma-rest   /etc/logrotate.d/gemma-rest
   ```
3. **Create credential file**
   ```bash
   sudo deploy/credential-setup.sh.example     # prompts for the DB password
   ```
4. **Reload + start**
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now gemma-rest
   sudo systemctl reload caddy
   ```
5. **Smoke test**
   ```bash
   curl -fsS http://127.0.0.1:8080/rest/v2/info
   curl -fsS https://gemma.example.org/rest/v2/info    # TODO(sysop): real domain
   ```

## Caveats — read before deploying to production

- **Every `TODO(sysop)` needs a deliberate decision.** Domain name, DB URL, DB user, image tag, host paths, RAM envelope, UID/GID mapping, and SELinux relabel scope are all host-specific. Do not ship to prod without the sysop's pass.
- **Never use `:latest`** on the container image. Pin to an immutable tag (digest preferred) so a registry push can't silently replace what's running.
- **Volumes are mounted with the `:Z` SELinux relabel flag.** If the host paths are shared with another service, `:Z` will privatise the label and break the other consumer. Use `:z` (lowercase) for shared label, or drop the flag and manage `chcon` manually.
- **`systemd-creds` over plaintext.** The sample writes the DB password as a 0400 file at `/etc/gemma/db.password`. For production prefer `systemd-creds encrypt` so the credential is TPM-sealed and only decryptable by this unit on this host. See `credential-setup.sh.example` for the comment block.
- **Caddy fronts on 443; the container binds 127.0.0.1:8080.** Do NOT expose 8080 publicly; the Quadlet `PublishPort=` is scoped to loopback for that reason.
- **Backups are out of scope.** Configure restic / borg / your platform's snapshotting against `/var/lib/gemma/data` separately.
- **Logs land on disk.** Tomcat writes to `/usr/local/tomcat/logs/` inside the container, mounted to `/var/log/gemma/` on the host. `logrotate-gemma-rest` handles rotation. If you forward to a log aggregator, change `JAVA_OPTS` / log4j config to emit JSON to stdout and let `podman logs` / `journald` carry it.
