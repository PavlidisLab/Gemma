#!/usr/bin/env python3
"""
Reconstruct which db/migration/mysql/V*.sql scripts have actually been applied
to a MySQL database, by probing the schema itself.

Why this exists: production `gemd` has no `flyway_schema_history` table. Flyway
is wired into the H2 test path only (BaseDatabaseTest5); production wiring is
still pending — see docs/design/FLYWAY_PROD_FOLLOWUP.md. Schema changes have
reached prod through the DBA channel (sql/migrations/db.*.sql) and, because
phase2 shares the live `gemd` with the deployed 1.32.x, through ad-hoc hand
application of individual V*.sql scripts. There is therefore no ledger of what
prod has received. This script rebuilds one.

Each migration leaves a distinctive schema fingerprint — a table, a column, an
index, or a foreign-key delete rule. Probing information_schema for every
fingerprint tells us, per migration, APPLIED / MISSING / PARTIAL. PARTIAL
matters most: a hand-applied script that died halfway leaves exactly that, and
it is the state most likely to break a later Flyway baseline.

Read-only. Issues nothing but SELECTs against information_schema; no DDL, no
writes, no access to table data.

Usage:
    scripts/probe_applied_migrations.py --host homer.msl.ubc.ca --database gemd
    scripts/probe_applied_migrations.py --login-path gemd-ro
    scripts/probe_applied_migrations.py --defaults-file ~/.my.cnf --out report.md

Credentials: see resolve_credentials() below. Never passed on the command line —
argv is world-readable via ps(1) on a shared host.
"""

from __future__ import annotations

import argparse
import getpass
import os
import platform
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from typing import Callable, Sequence


# ---------------------------------------------------------------------------
# Credential resolution
# ---------------------------------------------------------------------------
#
# Tried in order; the first that yields a usable credential wins.
#
#   1. --defaults-file PATH   a MySQL option file ([client] user=/password=).
#                             The canonical MySQL mechanism, works everywhere.
#   2. --login-path NAME      an entry made with `mysql_config_editor set`,
#                             stored obfuscated in ~/.mylogin.cnf. Preferred on
#                             Linux: no plaintext on disk, no desktop keyring
#                             daemon needed, survives headless/cron.
#   3. GEMMA_DB_USER / GEMMA_DB_PASSWORD environment variables, if already set.
#   4. Python `keyring`, if installed — cross-platform front end to the Secret
#      Service / kwallet on Linux, Keychain on macOS, Credential Manager on
#      Windows. Service name "gemma-db".
#   5. macOS Keychain via `security find-generic-password` (macOS only), which
#      is what scripts/perf_search.py uses.
#   6. Interactive prompt, if stdin is a TTY.
#
# Anything resolved as a bare (user, password) pair in steps 3-6 is handed to
# the mysql client through a 0600 temp option file, never through argv and
# never through MYSQL_PWD.

KEYRING_SERVICE = "gemma-db"


@dataclass
class Credentials:
    """Either a ready-made option file / login-path, or a user+password pair."""
    defaults_file: str | None = None
    login_path: str | None = None
    user: str | None = None
    password: str | None = None
    source: str = "unknown"


def _from_keyring() -> tuple[str, str] | None:
    try:
        import keyring  # type: ignore
    except ImportError:
        return None
    try:
        user = keyring.get_password(KEYRING_SERVICE, "GEMMA_DB_USER")
        password = keyring.get_password(KEYRING_SERVICE, "GEMMA_DB_PASSWORD")
    except Exception:
        # No backend available (headless box with no Secret Service, etc.).
        return None
    if user and password:
        return user, password
    return None


def _from_macos_keychain() -> tuple[str, str] | None:
    if platform.system() != "Darwin":
        return None

    def fetch(service: str) -> str | None:
        out = subprocess.run(
            ["security", "find-generic-password", "-s", service, "-w"],
            capture_output=True, text=True, check=False,
        )
        return out.stdout.strip() if out.returncode == 0 else None

    user, password = fetch("GEMMA_DB_USER"), fetch("GEMMA_DB_PASSWORD")
    if user and password:
        return user, password
    return None


def resolve_credentials(args: argparse.Namespace) -> Credentials:
    if args.defaults_file:
        return Credentials(defaults_file=os.path.expanduser(args.defaults_file),
                           source=f"--defaults-file {args.defaults_file}")
    if args.login_path:
        return Credentials(login_path=args.login_path,
                           source=f"--login-path {args.login_path}")

    env_user = os.environ.get("GEMMA_DB_USER")
    env_pass = os.environ.get("GEMMA_DB_PASSWORD")
    if env_user and env_pass:
        return Credentials(user=env_user, password=env_pass,
                           source="GEMMA_DB_USER / GEMMA_DB_PASSWORD")

    pair = _from_keyring()
    if pair:
        return Credentials(user=pair[0], password=pair[1],
                           source=f"python keyring (service '{KEYRING_SERVICE}')")

    pair = _from_macos_keychain()
    if pair:
        return Credentials(user=pair[0], password=pair[1], source="macOS Keychain")

    if sys.stdin.isatty():
        user = env_user or input("MySQL user: ").strip()
        password = getpass.getpass(f"Password for {user}: ")
        if user and password:
            return Credentials(user=user, password=password, source="interactive prompt")

    raise SystemExit(
        "No database credentials found. Provide them by any one of:\n"
        "  --login-path NAME        (mysql_config_editor set --login-path=NAME "
        "--host=... --user=... --password)\n"
        "  --defaults-file PATH     (a [client] option file, chmod 600)\n"
        "  GEMMA_DB_USER / GEMMA_DB_PASSWORD in the environment\n"
        f"  python keyring, service '{KEYRING_SERVICE}', keys GEMMA_DB_USER / GEMMA_DB_PASSWORD\n"
        "  or run interactively to be prompted.\n"
        "Passing a password as a command-line argument is not supported: argv is "
        "visible to every user on the host via ps(1)."
    )


# ---------------------------------------------------------------------------
# Schema fingerprints
# ---------------------------------------------------------------------------
#
# One entry per migration in db/migration/mysql/. `checks` are all expected to
# hold if the migration ran. `caveat` marks migrations whose fingerprint cannot
# distinguish "never applied" from "applied, then undone by a later migration" —
# these are reported INDETERMINATE rather than MISSING.

@dataclass(frozen=True)
class Check:
    kind: str          # table | column | index | fk_cascade | no_table | no_index
    obj: str           # table name
    member: str = ""   # column / index / constraint name
    label: str = ""

    def describe(self) -> str:
        if self.label:
            return self.label
        if self.kind == "table":
            return f"table {self.obj}"
        if self.kind == "no_table":
            return f"table {self.obj} absent"
        if self.kind == "column":
            return f"{self.obj}.{self.member}"
        if self.kind == "index":
            return f"index {self.member} on {self.obj}"
        if self.kind == "no_index":
            return f"index {self.member} on {self.obj} absent"
        if self.kind == "fk_cascade":
            return f"{self.member} ON DELETE CASCADE"
        return f"{self.kind} {self.obj} {self.member}"


def tbl(name: str) -> Check:
    return Check("table", name)


def no_tbl(name: str) -> Check:
    return Check("no_table", name)


def col(table: str, column: str) -> Check:
    return Check("column", table, column)


def idx(table: str, index: str) -> Check:
    return Check("index", table, index)


def no_idx(table: str, index: str) -> Check:
    return Check("no_index", table, index)


def fk_cascade(table: str, constraint: str) -> Check:
    return Check("fk_cascade", table, constraint)


@dataclass(frozen=True)
class Migration:
    version: str
    name: str
    checks: tuple[Check, ...]
    caveat: str = ""
    # A drop-only migration is satisfied by the *absence* of something, which is
    # also what a database that never had it looks like. Satisfied checks are
    # therefore not evidence of application, and the verdict is INDETERMINATE
    # unless `corroborated_by` — a fingerprint proving the dropped object once
    # existed here — is present.
    drop_only: bool = False
    corroborated_by: Check | None = None


MIGRATIONS: tuple[Migration, ...] = (
    # V1__prod_baseline.sql is a dump of prod itself — always "applied" by
    # definition, nothing to probe.
    Migration("V2", "audit_event_payload", (col("AUDIT_EVENT", "PAYLOAD"),)),
    Migration("V3", "ticket_layer",
              (tbl("TICKET"), tbl("TICKET_TARGET"), tbl("TICKET_EVENT"))),
    Migration("V4", "drop_duplicate_indexes",
              (no_idx("BIO_ASSAY_DIMENSIONS2BIO_ASSAYS", "BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC"),
               no_idx("RELEVANT_PUBLICATIONS", "INVESTIGATION_OTHER_RELEVANT_PUBLICATIONS_FKC")),
              caveat="drop-only migration, and nothing else in the schema records that these "
                     "indexes ever existed here. A Hibernate-built database satisfies it "
                     "without ever having run it; only the prod lineage can",
              drop_only=True),
    Migration("V5", "raw_vector_ee_qt_index",
              (idx("RAW_EXPRESSION_DATA_VECTOR", "experimentRawVectorByQt"),)),
    Migration("V6", "experimental_factor_baseline_relevance",
              (col("EXPERIMENTAL_FACTOR", "BASELINE_RELEVANCE"),
               col("EXPERIMENTAL_FACTOR", "BASELINE_RELEVANCE_REASON"))),
    Migration("V7", "single_cell_dimension_experiment",
              (tbl("SINGLE_CELL_DIMENSION_EXPERIMENT"),)),
    Migration("V8", "audit_trail_last_event_id",
              (col("AUDIT_TRAIL", "LAST_EVENT_FK"),)),
    Migration("V9", "sc_vector_ee_qt_index",
              (idx("SINGLE_CELL_EXPRESSION_DATA_VECTOR", "experimentSingleCellVectorByQt"),)),
    Migration("V10", "investigation_workflow_state",
              (col("INVESTIGATION", "WORKFLOW_STATE"),
               col("INVESTIGATION", "WORKFLOW_STATE_ENTERED_AT"),
               idx("INVESTIGATION", "INVESTIGATION_WORKFLOW_STATE"))),
    # AGENT_PROPOSAL is dropped again by V21, but the INVESTIGATION columns V11
    # adds are never dropped — so they, not the table, are the durable marker.
    Migration("V11", "agent_proposal_preboarded_experiment",
              (col("INVESTIGATION", "PREBOARDED_ACCESSION"),
               col("INVESTIGATION", "PREBOARDED_SOURCE"),
               col("INVESTIGATION", "PREBOARDED_IDENTIFYING_METADATA"))),
    Migration("V12", "curation_draft", (tbl("CURATION_DRAFT"),),
              caveat="V21 drops CURATION_DRAFT and V12 leaves nothing else behind; "
                     "if V21 ran, V12 is unknowable from the schema"),
    Migration("V13", "agent_proposal_kind", (col("AGENT_PROPOSAL", "KIND"),),
              caveat="target table is dropped by V21; only meaningful if V21 has not run"),
    Migration("V14", "audit_event_trail_id_index",
              (idx("AUDIT_EVENT", "IDX_AUDIT_EVENT_TRAIL_ID"),)),
    Migration("V15", "agent_proposal_lifecycle",
              (col("AGENT_PROPOSAL", "STATUS"), col("AGENT_PROPOSAL", "DISPOSITION")),
              caveat="target table is dropped by V21; only meaningful if V21 has not run"),
    Migration("V16", "user_soft_delete",
              (col("CONTACT", "DELETED_AT"), col("CONTACT", "DELETED_BY"),
               idx("CONTACT", "CONTACT_DELETED_AT_IDX"))),
    Migration("V17", "geo_scrape_watermark",
              (tbl("GEO_SCRAPE_WATERMARK"),
               col("INVESTIGATION", "PREBOARDED_MATCHED_CRITERIA"))),
    Migration("V18", "pipeline_jobs",
              (tbl("PIPELINE_JOB_BATCH"), tbl("PIPELINE_JOB"), tbl("PIPELINE_JOB_EVENT"))),
    Migration("V19", "ticket_mode_and_target_status",
              (col("TICKET", "MODE"), col("TICKET_TARGET", "STATUS"))),
    Migration("V20", "annotation_set", (tbl("ANNOTATION_SET"),)),
    Migration("V21", "drop_legacy_curation_tables",
              (no_tbl("CURATION_DRAFT"), no_tbl("AGENT_PROPOSAL")),
              caveat="drop-only migration: AGENT_PROPOSAL is equally absent from a database "
                     "that never ran V11. Corroborated by V11's INVESTIGATION columns, which "
                     "V21 does not drop — present columns + absent table means V21 ran",
              drop_only=True,
              corroborated_by=col("INVESTIGATION", "PREBOARDED_ACCESSION")),
    Migration("V22", "tag_supporting_evidence",
              (col("CHARACTERISTIC", "SUPPORTING_EVIDENCE"),)),
    # V23+ are still on unmerged branches; probing them is harmless and tells us
    # whether anyone has pushed them to this host ahead of the merge.
    Migration("V23", "scde_cascade_on_parent_delete",
              (fk_cascade("SINGLE_CELL_DIMENSION_EXPERIMENT", "FK_SCDE_EXPRESSION_EXPERIMENT"),
               fk_cascade("SINGLE_CELL_DIMENSION_EXPERIMENT", "FK_SCDE_QUANTITATION_TYPE"),
               fk_cascade("SINGLE_CELL_DIMENSION_EXPERIMENT", "FK_SCDE_SINGLE_CELL_DIMENSION"))),
    Migration("V24", "pipeline_job_attempts",
              (col("PIPELINE_JOB", "ATTEMPT"), col("PIPELINE_JOB", "RETRY_OF_FK"),
               col("PIPELINE_JOB", "SUPERSEDED_BY_FK"))),
    Migration("V25", "pipeline_batch_throttle",
              (col("PIPELINE_JOB_BATCH", "MAX_CONCURRENT"),
               col("PIPELINE_JOB_BATCH", "HELD"))),
)


# ---------------------------------------------------------------------------
# information_schema probing
# ---------------------------------------------------------------------------

class Schema:
    """Case-insensitive snapshot of the bits of information_schema we need."""

    def __init__(self, tables: set[str], columns: set[tuple[str, str]],
                 indexes: set[tuple[str, str]], cascading_fks: set[tuple[str, str]]):
        self.tables = tables
        self.columns = columns
        self.indexes = indexes
        self.cascading_fks = cascading_fks

    def holds(self, check: Check) -> bool:
        o, m = check.obj.upper(), check.member.upper()
        if check.kind == "table":
            return o in self.tables
        if check.kind == "no_table":
            return o not in self.tables
        if check.kind == "column":
            return (o, m) in self.columns
        if check.kind == "index":
            return (o, m) in self.indexes
        if check.kind == "no_index":
            return (o, m) not in self.indexes
        if check.kind == "fk_cascade":
            return (o, m) in self.cascading_fks
        raise ValueError(f"unknown check kind {check.kind}")


QUERIES = {
    "tables": "SELECT TABLE_NAME, '' FROM information_schema.TABLES "
              "WHERE TABLE_SCHEMA = DATABASE()",
    "columns": "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
               "WHERE TABLE_SCHEMA = DATABASE()",
    "indexes": "SELECT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS "
               "WHERE TABLE_SCHEMA = DATABASE()",
    "cascading_fks": "SELECT TABLE_NAME, CONSTRAINT_NAME "
                     "FROM information_schema.REFERENTIAL_CONSTRAINTS "
                     "WHERE CONSTRAINT_SCHEMA = DATABASE() AND DELETE_RULE = 'CASCADE'",
}


def run_mysql(sql: str, creds: Credentials, args: argparse.Namespace,
              option_file: str | None) -> list[tuple[str, str]]:
    """Run one SELECT through the mysql client, return rows as (col1, col2)."""
    cmd = ["mysql"]
    # --defaults-file/--login-path must come first, before any other option.
    if option_file:
        cmd.append(f"--defaults-file={option_file}")
    elif creds.defaults_file:
        cmd.append(f"--defaults-file={creds.defaults_file}")
    elif creds.login_path:
        cmd.append(f"--login-path={creds.login_path}")
    if args.host:
        cmd.append(f"--host={args.host}")
    if args.port:
        cmd.append(f"--port={args.port}")
    cmd += ["--batch", "--raw", "--skip-column-names", "--database", args.database, "-e", sql]

    out = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if out.returncode != 0:
        raise SystemExit(f"mysql failed ({out.returncode}): {out.stderr.strip()}")
    rows = []
    for line in out.stdout.splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        rows.append((parts[0], parts[1] if len(parts) > 1 else ""))
    return rows


def load_schema(creds: Credentials, args: argparse.Namespace,
                option_file: str | None) -> Schema:
    def fetch(key: str) -> set[tuple[str, str]]:
        return {(a.upper(), b.upper()) for a, b in run_mysql(QUERIES[key], creds, args, option_file)}

    tables = {a for a, _ in fetch("tables")}
    return Schema(tables, fetch("columns"), fetch("indexes"), fetch("cascading_fks"))


# ---------------------------------------------------------------------------
# Verdicts & reporting
# ---------------------------------------------------------------------------

APPLIED, MISSING, PARTIAL, INDETERMINATE = "APPLIED", "MISSING", "PARTIAL", "INDETERMINATE"


def verdict(migration: Migration, schema: Schema) -> tuple[str, list[str]]:
    held = [c for c in migration.checks if schema.holds(c)]
    failed = [c for c in migration.checks if not schema.holds(c)]
    if failed and held:
        return PARTIAL, [c.describe() for c in failed]
    if failed:
        state = INDETERMINATE if migration.caveat else MISSING
        return state, [c.describe() for c in failed]
    # Every check holds. For a drop-only migration that is not evidence of
    # application unless something proves the dropped object was ever here.
    if migration.drop_only:
        corroborated = (migration.corroborated_by is not None
                        and schema.holds(migration.corroborated_by))
        if not corroborated:
            return INDETERMINATE, []
    return APPLIED, []


def render(results: list[tuple[Migration, str, list[str]]], creds: Credentials,
           args: argparse.Namespace) -> str:
    lines = [
        f"# Applied-migration probe — {args.host or 'localhost'}/{args.database}",
        "",
        f"Credential source: {creds.source}",
        "",
        "Reconstructed from schema fingerprints; this database has no "
        "`flyway_schema_history` to consult.",
        "",
        "| Migration | Verdict | Unsatisfied fingerprints |",
        "|---|---|---|",
    ]
    for m, v, failed in results:
        if failed:
            detail = ", ".join(failed)
        elif v == INDETERMINATE:
            detail = "_satisfied, but absence is not evidence — see below_"
        else:
            detail = "—"
        lines.append(f"| `V{m.version[1:]}__{m.name}` | **{v}** | {detail} |")

    partial = [m for m, v, _ in results if v == PARTIAL]
    applied = [m for m, v, _ in results if v == APPLIED]
    indet = [(m, v) for m, v, _ in results if v == INDETERMINATE]

    lines += ["", "## Reading this", ""]
    if partial:
        lines.append(
            "- **PARTIAL is the finding that matters.** These migrations left some "
            "of their fingerprint but not all, which is what a hand-applied script "
            "that failed mid-way looks like. Reconcile each before any Flyway "
            "baseline: " + ", ".join(f"`{m.version}`" for m in partial))
    else:
        lines.append("- No PARTIAL results — every migration is cleanly all-or-nothing here.")
    if indet:
        lines.append("- **INDETERMINATE** migrations are drop-only or fully undone by a "
                     "later migration, so the schema cannot distinguish "
                     "\"never ran\" from \"ran, then was reversed\":")
        for m, _ in indet:
            lines.append(f"  - `{m.version}` — {m.caveat}")
    if applied:
        highest = applied[-1]
        lines += [
            "",
            "## Implication for the Flyway cutover",
            "",
            f"Highest cleanly-applied migration: **{highest.version}** "
            f"(`{highest.version}__{highest.name}`).",
            "",
            "docs/design/FLYWAY_PROD_FOLLOWUP.md currently specifies "
            "`baselineVersion(\"1\")`. That is only correct for a database that has "
            "received nothing since V1__prod_baseline. With the migrations above "
            "already present, a baseline at 1 would make Flyway attempt to re-apply "
            "each of them on first migrate, and they will fail — a duplicate column "
            "or duplicate table error, mid-cutover. Set the baseline to the highest "
            "migration this host actually has, and hand-reconcile anything PARTIAL "
            "first.",
        ]
    return "\n".join(lines) + "\n"


def main() -> int:
    p = argparse.ArgumentParser(
        description="Determine which db/migration/mysql/V*.sql scripts a database has received.")
    p.add_argument("--host", default=None, help="MySQL host (default: from option file / socket)")
    p.add_argument("--port", type=int, default=None, help="MySQL port")
    p.add_argument("--database", default="gemd", help="database name (default: gemd)")
    p.add_argument("--defaults-file", default=None,
                   help="MySQL option file holding [client] credentials")
    p.add_argument("--login-path", default=None,
                   help="mysql_config_editor login-path name (~/.mylogin.cnf)")
    p.add_argument("--out", default=None, help="write the markdown report here instead of stdout")
    args = p.parse_args()

    creds = resolve_credentials(args)

    option_file = None
    try:
        if creds.password is not None:
            # Hand the password to mysql via a 0600 option file rather than argv.
            fd, option_file = tempfile.mkstemp(prefix="gemma-probe-", suffix=".cnf")
            with os.fdopen(fd, "w") as fh:
                fh.write("[client]\n")
                fh.write(f"user={creds.user}\n")
                fh.write(f"password={creds.password}\n")
            os.chmod(option_file, 0o600)

        schema = load_schema(creds, args, option_file)
    finally:
        if option_file and os.path.exists(option_file):
            os.unlink(option_file)

    results = [(m, *verdict(m, schema)) for m in MIGRATIONS]
    report = render(results, creds, args)

    if args.out:
        with open(args.out, "w") as fh:
            fh.write(report)
        print(f"wrote {args.out}")
    else:
        print(report, end="")

    return 1 if any(v == PARTIAL for _, v, _ in results) else 0


if __name__ == "__main__":
    sys.exit(main())
