#!/usr/bin/env bash
#
# Read and write pages on the Pavlidis Lab Confluence wiki through its WebDAV plugin.
#
# Usage:
#   wiki.sh ls   <path>            list children of a space or page
#   wiki.sh get  <page-path>       print a page's storage-format body to stdout
#   wiki.sh put  <page-path> <file|-> --confirm '<page title>'
#                                  replace (or create) a page's body; '-' reads stdin.
#                                  The confirm token must equal the page title (the last
#                                  component of <page-path>) or nothing is sent.
#   wiki.sh rclone <args...>       raw rclone, with the wiki remote as ':webdav:'
#
# <page-path> is the WebDAV path of the page directory, e.g.
#   'Global/gemma/Gemma Landing Page/Gemma Curation/List of Gemma CLI Tools (staging)'
# The body lives at '<page-path>/<leaf>.txt', which this script resolves for you.
#
# Token resolution (first hit wins), on the host where rclone runs:
#   $PAVLAB_WIKI_TOKEN, $RCLONE_WEBDAV_BEARER_TOKEN,
#   macOS Keychain `security find-generic-password -s PAVLAB_WIKI_TOKEN -w`,
#   `pass show $PAVLAB_WIKI_PASS_ENTRY` (default pavlab-wiki/token),
#   `secret-tool lookup service pavlab-wiki`.
# No token -> exit 1, naming the override. rclone.conf is never consulted: the remote is
# defined on the fly from environment variables, so a stale token in that file cannot be
# picked up.
#
# rclone is not installed on every box. When it is missing locally, this script re-runs
# itself on $PAVLAB_WIKI_RCLONE_HOST (default chalmers) over ssh, at the path
# `readlink -f "$0"` gives. That works only because $HOME is NFS-shared, so the checkout
# holding this script must live under $HOME. A locally exported token and any file input
# are streamed over stdin (token first), so neither reaches argv and the file need not be
# on shared storage.

set -euo pipefail

WIKI_URL="${PAVLAB_WIKI_URL:-https://wiki.pavlab.msl.ubc.ca/plugins/servlet/confluence/default}"
RCLONE_HOST="${PAVLAB_WIKI_RCLONE_HOST:-chalmers}"
PASS_ENTRY="${PAVLAB_WIKI_PASS_ENTRY:-pavlab-wiki/token}"

die() { echo "wiki.sh: $1" >&2; exit "${2:-1}"; }

usage() { sed -n '3,15p' "$0" | sed 's/^# \{0,1\}//' >&2; exit 1; }

[ $# -ge 1 ] || usage

cmd="$1"; shift

# --- put: parse and check the write guard BEFORE anything leaves this host --------------
if [ "$cmd" = put ]; then
    confirm=""; have_confirm=""; pos=()
    while [ $# -gt 0 ]; do
        case "$1" in
            --confirm)   [ $# -ge 2 ] || die "--confirm needs a value (the page title)"
                         confirm="$2"; have_confirm=1; shift 2 ;;
            --confirm=*) confirm="${1#--confirm=}"; have_confirm=1; shift ;;
            *)           pos+=("$1"); shift ;;
        esac
    done
    [ ${#pos[@]} -eq 2 ] || usage
    page="${pos[0]%/}"; src="${pos[1]}"
    title="${page##*/}"
    [ -n "$have_confirm" ] || die "refusing to write: put needs --confirm '$title' (the page title)"
    [ "$confirm" = "$title" ] || die "refusing to write: --confirm '$confirm' does not match the page title '$title'"
    if [ "$src" != - ] && [ ! -f "$src" ]; then die "no such file: $src"; fi
    set -- "$page" "$src" --confirm "$confirm"
fi

# --- delegate to a host that has rclone -------------------------------------------------
if ! command -v rclone >/dev/null 2>&1; then
    [ -z "${PAVLAB_WIKI_DELEGATED:-}" ] || die "rclone not found on $(hostname) either"
    self="$(readlink -f "$0")"
    # A locally exported token is forwarded as the first stdin line (never argv, which
    # is world-readable in ps); the remote side reads it back before anything else.
    tok="${PAVLAB_WIKI_TOKEN:-${RCLONE_WEBDAV_BEARER_TOKEN:-}}"
    input=/dev/null
    if [ "$cmd" = put ]; then
        if [ "$2" = - ]; then input=/dev/stdin; else input="$2"; fi
        set -- "$1" - --confirm "$4"
    fi
    remote_cmd="$(printf '%q ' env PAVLAB_WIKI_DELEGATED=1 "$self" "$cmd" "$@")"
    { printf '%s\n' "$tok"; cat "$input"; } |
        ssh -o BatchMode=yes -o LogLevel=error "$RCLONE_HOST" "$remote_cmd"
    exit "${PIPESTATUS[1]}"
fi

if [ -n "${PAVLAB_WIKI_DELEGATED:-}" ]; then
    IFS= read -r fwd_token || true
    [ -z "$fwd_token" ] || export PAVLAB_WIKI_TOKEN="$fwd_token"
    unset fwd_token
fi

# --- resolve the token ------------------------------------------------------------------
token="${PAVLAB_WIKI_TOKEN:-${RCLONE_WEBDAV_BEARER_TOKEN:-}}"
if [ -z "$token" ] && command -v security >/dev/null 2>&1; then
    token="$(security find-generic-password -s PAVLAB_WIKI_TOKEN -w 2>/dev/null || true)"
fi
if [ -z "$token" ] && command -v pass >/dev/null 2>&1; then
    token="$(pass show "$PASS_ENTRY" 2>/dev/null | head -n1 || true)"
fi
if [ -z "$token" ] && command -v secret-tool >/dev/null 2>&1; then
    token="$(secret-tool lookup service pavlab-wiki 2>/dev/null || true)"
fi
[ -n "$token" ] || die "no wiki token on $(hostname): set PAVLAB_WIKI_TOKEN, or store one as
  macOS Keychain service PAVLAB_WIKI_TOKEN, or  pass insert $PASS_ENTRY
(a Confluence personal access token: wiki profile -> Personal Access Tokens)" 1

export RCLONE_WEBDAV_URL="$WIKI_URL" RCLONE_WEBDAV_VENDOR=other RCLONE_WEBDAV_BEARER_TOKEN="$token"
unset token
# Keep the user's rclone.conf (and its plaintext token) out of the picture entirely.
export RCLONE_CONFIG=/dev/null

body_path() {
    local p="${1%/}"
    printf ':webdav:%s/%s.txt' "$p" "${p##*/}"
}

case "$cmd" in
    ls)
        [ $# -le 1 ] || usage
        # `rclone lsf` hides every collection here: the plugin reports each child page
        # with a 404 propstat alongside its 200 one, and rclone drops such entries. So
        # parse the PROPFIND hrefs ourselves (rclone still does the auth). Pages print
        # with a trailing '/'. '@exports' / '@versions' / '*.url' are plugin artefacts.
        rclone lsf --dump responses ":webdav:${1:-}" 2>&1 >/dev/null | python3 -c '
import re, sys, urllib.parse as u
hrefs = list(dict.fromkeys(re.findall(r"<D:href>([^<]*)</D:href>", sys.stdin.read())))
if not hrefs:
    sys.exit("wiki.sh: empty or failed listing (bad path, or auth failure)")
base = hrefs[0].rstrip("/") + "/"
for h in hrefs[1:]:
    name = u.unquote(h[len(base):]) if h.startswith(base) else None
    if name and not name.startswith("@") and not name.endswith(".url"):
        print(name)
'
        ;;
    get)
        [ $# -eq 1 ] || usage
        rclone cat "$(body_path "$1")"
        ;;
    put)
        # Arguments were parsed and the --confirm token checked at the top.
        if [ "$2" = - ]; then
            rclone rcat "$(body_path "$1")"
        else
            rclone copyto "$2" "$(body_path "$1")"
        fi
        ;;
    rclone)
        rclone "$@"
        ;;
    *)
        usage
        ;;
esac
