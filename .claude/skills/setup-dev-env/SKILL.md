---
name: setup-dev-env
description: >
  Set up a new developer's Gemma 2.0 dev environment from scratch. Walks
  the user through cloning both repos (Gemma backend + gemma-ui frontend
  monorepo), choosing an upstream Gemma to target (frink for read-only
  shared dev, GHCR-pulled docker for local backend, or full source-build
  flow), wiring the .env files, installing npm workspaces deps, and
  optionally seeding macOS Keychain entries for the various creds. Trigger
  when the user says "set up the gemma 2.0 dev env", "set up gemma dev",
  "onboard a new dev", or pastes a fresh-machine question about cloning
  / running the dev stack. Don't trigger for unrelated build issues on an
  already-set-up tree — that's a regular debug session, not setup.
---

# setup-gemma-dev-env

Onboards a developer to the Gemma 2.0 stack on macOS.

## What this skill orchestrates

There are three real machines worth of stuff that have to line up:

1. **Backend** — `PavlidisLab/Gemma` (this repo). Java/Maven WAR served
   by a Tomcat container. For a new dev there are three viable launch
   modes; pick one in step 2:
   - **frink-tunneled** (default, fastest onboarding): SPA hits
     `http://frink.msl.ubc.ca:8080` directly; no local backend, no
     docker, no DB. Requires lab network / VPN access to frink.
   - **GHCR docker (recommended for offline / VPN-less)**: pull
     `ghcr.io/pavlidislab/gemma-rest:2.0.0-alpha` and run it locally.
     Backend hits a real gemd via the prod SSH tunnel; needs an SSH key
     on the user's machine that's authorized for frink's tunnel relay.
   - **source build**: clone + `mvn package -P gemma-rest-war`, run the
     resulting WAR in a docker container. Needs JDK 25 + Maven + the DB
     creds. Only worth it for backend devs editing Java.

2. **Frontend** — `PavlidisLab/gemma-ui` (npm workspaces monorepo with
   `apps/browser` for public typeahead/heatmap and `apps/curation` for
   curator workflow). React + Vite, talks to backend via the dev server's
   `/rest` proxy. The proxy target is configured in
   `apps/browser/.env.local` and `apps/curation/.env` (`GEMMA_BASE_URL`).

3. **Credentials + keychain** — `GEMMA_USERNAME`, `GEMMA_PASSWORD`,
   optionally `GEMMA_CURATION_API_KEY` for the curation app. Stored in
   macOS Keychain so scripts (`scripts/perf_search.py`,
   `scripts/setup-dev-env.sh`, etc.) can resolve them via
   `security find-generic-password -s <entry> -w`. New devs need their
   own lab account; the skill walks them through adding entries.

## What you do (the model) when triggered

### Step 1 — confirm prerequisites

Run `scripts/setup-dev-env.sh --check` and report missing tools.
Required: `git`, `node` (≥20), `npm`, `docker` (only if launch mode is
GHCR or source). Optional: `temurin-25` JDK (only for source-build).

If anything's missing tell the user how to install (`brew install node
docker`, JDK via `brew install --cask temurin-25`).

### Step 2 — ask the user three questions via AskUserQuestion

1. **Backend launch mode**: frink-tunneled / GHCR docker / source build.
   Default first option unless the user explicitly mentioned offline,
   VPN-less, or backend editing.
2. **Clone location**: default `~/Dev/eclipseworkspace/Gemma` and
   `~/Dev/gemma-curation-ui` (matches Paul's machine and the path
   references in CLAUDE.md). If the user wants different paths, accept
   them but remember the script's default lookups assume the default.
3. **Frontend app to start**: browser, curation, or both.

### Step 3 — run the setup script

Invoke `scripts/setup-dev-env.sh` with the answers as flags:

```bash
scripts/setup-dev-env.sh \
    --mode=frink|ghcr|source \
    --gemma-dir=$HOME/Dev/eclipseworkspace/Gemma \
    --ui-dir=$HOME/Dev/gemma-curation-ui \
    --frontend=browser|curation|both
```

The script is idempotent — re-runs are safe; it skips work that's
already done. It:

- Clones both repos if missing (or fast-forwards if dirty-but-tracking).
- For `--mode=ghcr` or `source`: pulls/builds the WAR image, starts the
  gemma-rest container on `:8080`.
- Writes `apps/browser/.env.local` and `apps/curation/.env` with the
  chosen `GEMMA_BASE_URL` (and a generated random
  `VITE_GEMMA_CURATION_API_KEY` if curation is selected).
- Runs `npm install` at the gemma-ui root (workspace install).
- Probes `http://<chosen-upstream>/rest/v2/` to sanity-check connectivity.

### Step 4 — keychain setup (interactive)

After the script, ask the user via `AskUserQuestion` whether they have
lab credentials yet. If yes, walk them through:

```bash
security add-generic-password -s GEMMA_USERNAME -a $USER -w 'theirusername'
security add-generic-password -s GEMMA_PASSWORD -a $USER -w 'theirpassword'
```

If no, tell them to ping Paul for an account and note that anonymous
read-only browsing works without creds (only admin / curator features
need them).

### Step 5 — print run commands

Tell them exactly what to type:

```bash
cd ~/Dev/gemma-curation-ui
npm run dev:browser        # http://localhost:5183/browser
npm run dev:curation       # http://localhost:5174 (curation app)
```

And if `--mode=ghcr|source`: `docker logs -f gemma-rest` for backend
logs.

## What NOT to do

- Don't try to build the WAR if `--mode=frink` — frink is the backend.
- Don't write the user's Keychain password to disk or log it. Use the
  `security` CLI inline.
- Don't seed a local MySQL — the dev stack tunnels to prod gemd; the
  `gemdtest` schema is for `mvn verify` only, not for runtime dev.
- Don't commit the user's `.env.local` files (they're already in
  `.gitignore` in both apps).

## When the user comes back with an issue

If they hit a specific failure during one of the three modes (frink
unreachable, docker pull auth issue, npm install ESM error), treat it
as a regular debug session, NOT a re-run of this skill. Re-running the
setup is fine but the failure is what they need help with first.
