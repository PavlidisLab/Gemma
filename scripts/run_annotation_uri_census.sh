#!/usr/bin/env bash
# Run the annotation URI census against production and land the results in the data dir.
#
#   scripts/run_annotation_uri_census.sh                 # local, through the :8000 tunnel
#   scripts/run_annotation_uri_census.sh --on-frink      # print the frink-side invocation
#   scripts/run_annotation_uri_census.sh --dry-run       # show what would run, connect to nothing
#
# READ-ONLY. The script refuses to run if the SQL file contains anything but SELECT -- see
# assert_read_only(). That guard is not decoration: the account this connects as
# (gemmaadmin) can write to production, and the only thing standing between this script and
# a production write is the check below.
#
# Output: one TSV per query in ~/Data/gemma-curation-agents-data/annotation_uri_census/,
# because the census is large and regenerable and does not belong in a git repo.
set -euo pipefail

SQL_FILE="${SQL_FILE:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/sql/annotation_uri_census.sql}"
OUT_DIR="${OUT_DIR:-$HOME/Data/gemma-curation-agents-data/annotation_uri_census}"
STAMP="$(date +%Y%m%d)"

# 🛑 127.0.0.1, never `localhost`: the mysql client treats `localhost` as a unix-socket
# connection and IGNORES -P, silently routing to whatever mysqld is running locally. That
# looks exactly like a successful prod connection and is not one.
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-8000}"
DB_NAME="${DB_NAME:-gemd}"
DB_USER="${DB_USER:-gemmaadmin}"

DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=1 ;;
        --on-frink)
            cat <<'FRINK'
Run this on frink (it reaches prod-db directly, no tunnel needed):

    ssh -p 22000 paul@frink.msl.ubc.ca
    cd ~/Gemma2.0
    set -a; . ./env.gemma; set +a          # picks up GEMMA_DB_PASSWORD without echoing it
    mysql -h "$GEMMA_DB_HOST" -P "$GEMMA_DB_PORT" -u "$GEMMA_DB_USER" \
          -p"$GEMMA_DB_PASSWORD" --batch --raw "$GEMMA_DB_NAME" \
          < ~/sql/annotation_uri_census.sql > ~/annotation_uri_census_$(date +%Y%m%d).tsv

Then bring it back:

    scp -P 22000 paul@frink.msl.ubc.ca:~/annotation_uri_census_*.tsv \
        ~/Data/gemma-curation-agents-data/annotation_uri_census/
FRINK
            exit 0 ;;
        *) echo "unknown argument: $arg" >&2; exit 2 ;;
    esac
done

keychain_export() {
    local var="$1"; shift
    local val=""
    [ -n "${!var:-}" ] && return 0
    for entry in "$@"; do
        [ -z "$entry" ] && continue
        # -a pins the account: a bare service query returns an older duplicate entry first.
        if val=$(security find-generic-password -s "$entry" -a "$DB_USER" -w 2>/dev/null); then
            export "$var=$val"; return 0
        fi
    done
    return 1
}

assert_read_only() {
    # Strip comments and string literals, then look for any mutating verb. Cheap, and it
    # fails closed: an unparseable file is a refusal, not a run.
    local stripped
    # Drop comments, then drop the one statement we explicitly permit: a session-scoped
    # group_concat_max_len, which Q3 needs and which cannot alter data. Anything else that
    # looks like a write still trips the check below.
    # Drop comments, then drop the one statement we explicitly permit: a session-scoped
    # group_concat_max_len, which Q3 needs and which cannot alter data. Anything else that
    # looks like a write still trips the check below.
    # (No sed /I flag here -- BSD sed does not have one, and an unsupported flag would make
    # the exemption silently never apply, which is how this was caught.)
    stripped=$(sed -e 's/--.*$//' \
                   -e 's/[Ss][Ee][Tt] [Ss][Ee][Ss][Ss][Ii][Oo][Nn] group_concat_max_len *= *[0-9]* *;//' \
                   "$SQL_FILE" | tr '\n' ' ')
    # REPLACE is matched only as `REPLACE INTO`: REPLACE() is also an ordinary string
    # function, and Q3 calls it nine times. A guard that fires on legitimate SQL is a guard
    # somebody switches off, which is worse than not having one.
    if grep -qiE '\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|REVOKE)\b|\bREPLACE[[:space:]]+INTO\b|\bSET[[:space:]]+[A-Za-z_@.]+[[:space:]]*=' <<<"$stripped"; then
        echo "REFUSING: $SQL_FILE contains a non-SELECT statement." >&2
        echo "This connects as an account that can write to PRODUCTION. Fix the file." >&2
        exit 1
    fi
}

[ -r "$SQL_FILE" ] || { echo "ERROR: cannot read $SQL_FILE" >&2; exit 1; }
assert_read_only
echo "read-only check passed: $(basename "$SQL_FILE")"

if [ "$DRY_RUN" -eq 1 ]; then
    echo "would connect: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
    echo "would write:   $OUT_DIR/*_$STAMP.tsv"
    # `|| true`: under `set -e` a grep that matches nothing exits 1 and would abort the
    # dry run, reporting a refusal that never happened.
    grep -oE '^-- Q[0-9]+ --.*' "$SQL_FILE" | sed 's/^/  /' || true
    exit 0
fi

# Is the tunnel actually up? A refused connection here is far clearer than mysql's.
if ! nc -z "$DB_HOST" "$DB_PORT" 2>/dev/null; then
    cat >&2 <<TUNNEL
ERROR: nothing is listening on $DB_HOST:$DB_PORT -- the prod port-forward is not up.

Start it with:
    ssh -fN -L8000:prod-db.msl.ubc.ca:3306 paul@willie.pavlab.msl.ubc.ca -p 22000

Or run the census on frink instead:
    $0 --on-frink
TUNNEL
    exit 1
fi

keychain_export DB_PASSWORD "${GEMMA_DB_KEYCHAIN_ENTRY:-}" "mysql-gemd-pavlab-8000" \
    || { echo "ERROR: no password for $DB_USER in keychain (tried mysql-gemd-pavlab-8000). Set GEMMA_DB_KEYCHAIN_ENTRY." >&2; exit 1; }

# An option file rather than -p on the command line: a password in argv is visible in ps.
CNF=$(mktemp); chmod 600 "$CNF"
trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' \
    "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"

mkdir -p "$OUT_DIR"

# Run each -- Qn -- block separately so every query lands in its own TSV. Feeding the whole
# file at once concatenates the result sets with no way to tell where one ends.
awk '
    /^-- Q[0-9]+ --/ { if (name) close(out); name=$2; out=dir "/" name "_" stamp ".sql"; }
    { if (name) print > (dir "/" name "_" stamp ".sql") }
' dir="$OUT_DIR" stamp="$STAMP" "$SQL_FILE"

# If the file carries no `-- Qn --` markers the glob below matches nothing and the loop
# would run once with a literal `Q*` filename. Fail here, with the reason.
shopt -s nullglob
parts=( "$OUT_DIR"/Q*_"$STAMP".sql )
if [ ${#parts[@]} -eq 0 ]; then
    echo "ERROR: $SQL_FILE contains no '-- Qn --' block markers, so there is nothing to run." >&2
    echo "Each query must be preceded by a line like '-- Q1 -- description'." >&2
    exit 1
fi

echo "running against $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
for part in "${parts[@]}"; do
    q=$(basename "$part" "_$STAMP.sql")
    printf '  %-4s ... ' "$q"
    if mysql --defaults-extra-file="$CNF" --batch --raw "$DB_NAME" < "$part" \
            > "$OUT_DIR/${q}_${STAMP}.tsv" 2>"$OUT_DIR/${q}_${STAMP}.err"; then
        printf '%s rows\n' "$(( $(wc -l < "$OUT_DIR/${q}_${STAMP}.tsv") - 1 ))"
        rm -f "$OUT_DIR/${q}_${STAMP}.err"
    else
        printf 'FAILED -- see %s\n' "$OUT_DIR/${q}_${STAMP}.err"
    fi
    rm -f "$part"
done

echo
echo "wrote $OUT_DIR/*_$STAMP.tsv"
echo
echo "🛑 Quote the Q4 staleness number alongside any count from this run --"
echo "   it says whether EE2C agrees with CHARACTERISTIC on the surface you just read:"
[ -f "$OUT_DIR/Q4_$STAMP.tsv" ] && cat "$OUT_DIR/Q4_$STAMP.tsv"
echo
echo "Next: scripts/build_term_crossmatch.py --census $OUT_DIR/Q1_$STAMP.tsv \\"
echo "        --out crossmatch_$STAMP.tsv --label-candidates label_candidates_$STAMP.tsv"
