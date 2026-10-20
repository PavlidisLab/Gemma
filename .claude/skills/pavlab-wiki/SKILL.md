---
name: pavlab-wiki
description: Read, create or update pages on the Pavlidis Lab Confluence wiki (wiki.pavlab.msl.ubc.ca) through its WebDAV plugin, via .claude/skills/pavlab-wiki/wiki.sh — reads freely, writes only under an explicit --confirm token that is the exact page title. Trigger when the user asks to read, edit, publish to, or push something onto "the wiki", a pavlab wiki page, or a Confluence page by title/path; when a Gemma change (CLI options, REST behaviour, deployment, curation workflow) leaves a wiki page describing the old behaviour; and when republishing the generated "List of Gemma CLI Tools" pages by hand. Covers the page-path model, Confluence storage format (XHTML, not wiki markup), the entity-normalization trap that fails an upload with "sizes differ", the listing quirk that makes plain rclone show no child pages, and the failure table. 🛑 A put is lab-visible the moment it lands: `get` a backup first, show the announce block (page path, diff stat, backup path) and wait for the user's yes BEFORE the put — the --confirm title is the user's say-so, never supplied by habit, and approval for one page does not cover the next. Not for Claude Docs, artifacts, or repo docs/.
---

# pavlab-wiki — reading and writing the lab Confluence wiki from the Gemma repo

The wiki is a Confluence instance exposed over WebDAV; every page is a directory whose body
is one storage-format XHTML file. `wiki.sh` wraps rclone so a session never configures a
remote and never reads anybody's `rclone.conf`.

> **Same script as `ga-wiki`.** `wiki.sh` here is a verbatim copy of
> `scripts/wiki.sh` in `gemma-curation-agents` (the `ga-wiki` skill). Fix a bug in one,
> copy it to the other. The curation-manual rules (the wiki is upstream of that repo's
> `docs/curation_rules/`) live in `ga-wiki`; read it before editing a curation-manual page.

## When a Gemma change should touch the wiki

Documentation for users and curators lives on the wiki, not in this repo — the CLI tool
reference, curation how-tos, setup pages. After a change that alters what a page tells
people (a CLI option renamed or added, a REST response reshaped, a deployment or setup
step changed), find the page and propose the edit **in the same session**, as a separate
announced step. Don't write it silently alongside the commit, and don't skip it. Pages
under `Global/gemma/Gemma Landing Page/` are the usual place; walk `ls` rather than guess.

## Script

```bash
W="$(git rev-parse --show-toplevel)/.claude/skills/pavlab-wiki/wiki.sh"
"$W" ls  'Global/gemma/Gemma Landing Page'                  # child pages
"$W" get 'Global/gemma/Gemma Landing Page/Gemma Curation'   > page.xhtml
"$W" put 'Global/gemma/Gemma Landing Page/Gemma Curation'   page.xhtml \
         --confirm 'Gemma Curation'                          # or '-' for stdin
"$W" rclone lsf ':webdav:Global/gemma' --dirs-only          # raw escape hatch
```

* **`put` refuses without `--confirm '<exact page title>'`** — the last component of the
  page path, verbatim. A missing or mismatched token, or a missing input file, is refused
  **locally, before anything is sent**. The token is where the user's agreement enters.
* **Where it runs.** rclone if installed locally; otherwise the script re-runs itself on
  `$PAVLAB_WIKI_RCLONE_HOST` (default `chalmers`) over ssh at the path `readlink -f "$0"`
  gives. That relies on the NFS-shared `$HOME`, so 🛑 **the checkout must live under
  `$HOME`**; a laptop needs rclone installed locally. File input and a locally exported
  token go over ssh stdin, never argv.
* The raw `rclone` subcommand has **no** confirm guard. Reads and the new-page `mkdir`
  fallback only; any other write through it is announced and confirmed like a `put`.

## Credentials

Resolved on the host where rclone runs, first hit wins:

1. a pre-set `PAVLAB_WIKI_TOKEN` or `RCLONE_WEBDAV_BEARER_TOKEN` (a locally
   exported one is forwarded to the ssh host as the first stdin line);
2. macOS Keychain: `security find-generic-password -s PAVLAB_WIKI_TOKEN -w`;
3. `pass show pavlab-wiki/token` (entry overridable by `PAVLAB_WIKI_PASS_ENTRY`);
4. `secret-tool lookup service pavlab-wiki`.

None → **exit 1**, naming `PAVLAB_WIKI_TOKEN` and the stores. `rclone.conf` is
never consulted (`RCLONE_CONFIG=/dev/null`; the remote is defined by
`RCLONE_WEBDAV_*` env), so a stale token in it cannot be picked up. The token is
a Confluence personal access token (wiki profile → Personal Access Tokens). Never
read `.env`, never ask for a paste, and don't go looking for tokens elsewhere.

## Context — the path model, the format, and the traps

### Path model

- `Global/<SPACEKEY>/<space home page>/<child>/<grandchild>…`. Each page is a
  **directory**; its body is `<dir>/<dir-name>.txt`, which `get`/`put` resolve.
  Titles are used verbatim, spaces and parentheses included. Quote every path.
- `@exports/`, `@versions/` and `*.url` inside a page dir are plugin artefacts.
  Never write to them, and never `rclone sync`/`delete`/`purge` — sync deletes
  sibling pages.
- If the user gives a page title or URL rather than a path, find it by walking
  `ls` from the space. Don't guess. The wiki root and `Global/` list as EMPTY —
  start from a known space such as `Global/gemma/` (its body is `Gemma.txt`; the
  home page is `Gemma Landing Page/`).
- **Listing quirk.** `ls` prints child pages with a trailing `/` and the page's
  own body as `<Title>.txt`. Plain `rclone lsf` shows NO child pages here (the
  plugin's mixed 404/200 propstats make rclone drop every collection);
  `wiki.sh ls` parses the PROPFIND response itself.
- **Stray files.** A page dir can hold `.txt` files besides its body:
  `List of Gemma CLI Tools (staging)/` carries a
  `List of Gemma CLI Tools (generated).txt` from some earlier deploy. Only
  `<dir-name>.txt` is the page.

### Body format: Confluence *storage format* (XHTML), not wiki markup

- `<p>`, `<h2>`, `<ul>`, `<table><tbody><tr><th>…`, `<code>`, `<a href>`;
  macros are `<ac:structured-macro ac:name="code|info|note|toc" …>` with
  `<ac:parameter>` / `<ac:plain-text-body><![CDATA[…]]></ac:plain-text-body>` /
  `<ac:rich-text-body>`. Copy idioms from an existing page's `get` output, or
  from `gemma-cli/src/main/java/ubic/gemma/cli/completion/ConfluenceWikiHtmlGenerator.java`
  in the Gemma repo.
- 🛑 **Escape only `& < > "`. Write every other character as literal UTF-8** —
  no `&mdash;`, `&nbsp;`, `&rarr;`. Confluence normalizes named entities on
  store, the page reads back shorter than was uploaded, and rclone fails with
  `corrupted on transfer: sizes differ` (Gemma commit `5a8ddcbcdd`). The same
  error after other edits means Confluence normalized something else; `get` the
  page and conform to what it stored.

### Workflow (every write)

1. **`get` first**, even for a "full replace": keep the current body in the
   scratchpad as a backup and so you can see how it's structured. Preserve
   anything the user didn't ask to change, including macros you don't
   recognise.
2. Edit locally; show the user the diff (or, for a new page, the parent path
   and title) in the announce block (below).
3. **Get explicit confirmation before `put`.** Approval for one page does not
   cover the next.
4. `put … --confirm '<title>'`, then `get` again and diff it against what you
   sent to verify.
5. New page: `put` to `<existing parent>/<New Title>` creates it. This is
   unverified: if it fails, `"$W" rclone mkdir ':webdav:<parent>/<New Title>'`
   first (announced like a put), then `put`.

### Gemma CLI tool pages (by hand)

Jenkins publishes the generated `List of Gemma CLI Tools (…)` pages from the CLI deploy
stage via `gemma-cli/deploy-wiki.sh` (`.jenkins/Jenkinsfile`, the `deploy-wiki.sh` call).
That script needs rclone plus a configured `pavlab-wiki:` remote locally, so from a session
run its two halves instead:

```bash
mvn -pl gemma-cli -am package -DskipTests        # refresh target/appassembler
JAVA_OPTS=-Dspring.profiles.active=dev ./gemma-cli/target/appassembler/bin/gemma-cli \
  --completion --completion-wiki --completion-wiki-output-dir "$SCRATCH/wiki" \
  --completion-wiki-page-suffix ' (staging)'
```

then `put` each generated `<Page>/<Page>.txt` under
`Global/gemma/Gemma Landing Page/Gemma Curation/List of Gemma CLI Tools (staging)/…`,
announcing the set as one block. Suffix by branch, as the Jenkinsfile assigns it:
`master` → ` (generated)` (production), `hotfix-*`/`release-*` → ` (staging)`, anything
else → ` (development)`. 🛑 Never write ` (generated)` from a non-master build.

### Failures

| symptom | meaning | remedy |
|---|---|---|
| exit 1 `no wiki token on <host>` | nothing in the env or any store on the host rclone runs on | the user creates a Confluence personal access token and stores it (`pass insert pavlab-wiki/token` on chalmers, Keychain service `PAVLAB_WIKI_TOKEN` on a Mac) or exports `PAVLAB_WIKI_TOKEN` for the session. Don't go looking for tokens elsewhere |
| `pass` prompts or fails over ssh | gpg-agent is locked (non-interactive ssh cannot unlock it) | the user runs `! ssh -t chalmers pass show pavlab-wiki/token >/dev/null` once to unlock |
| `refusing to write: …--confirm…` | the write guard; nothing was sent | get the user's yes, then pass the exact page title |
| `401` | token expired / revoked | same as no token |
| `403` | the token's user lacks permission on that space/page | ask a space admin; not fixable from here |
| `corrupted on transfer: sizes differ` | Confluence normalized the body (usually a named entity) | see the escaping rule above; `get` and conform |
| `empty or failed listing` | bad path, or an auth failure | walk `ls` from the space; check the token |
| `rclone not found on <host> either` | the delegation host has no rclone | set `PAVLAB_WIKI_RCLONE_HOST` to a box that has it |

## Announce before every `put`

Print this and wait for the user's yes **before** the `put`:

| | |
|---|---|
| **what** | the `put` command verbatim, `--confirm` title included |
| **page** | the full page path; for a new page, the existing parent and the new title |
| **change** | bytes before → after and a diff stat; the diff itself if short |
| **backup** | the scratch path holding the `get` from step 1 |
| **visibility** | 🛑 lab-visible immediately (Confluence keeps page history) |

## Report when done

Pages written, bytes and diff stat as landed, whether the read-back `get` matched what was
sent (and what Confluence normalized if not), the backup path. A read-only job says
"wrote nothing".
