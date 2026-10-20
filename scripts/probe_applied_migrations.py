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
    kind: str          # table | column | column_type | not_null | index | fk_cascade | no_table | no_index
    obj: str           # table name
    member: str = ""   # column / index / constraint name
    label: str = ""
    # Only meaningful for column_type: the information_schema DATA_TYPE the
    # column must have. A MODIFY COLUMN migration changes a type in place, so
    # presence of the column proves nothing — only its type does.
    expected: str = ""

    def describe(self) -> str:
        if self.label:
            return self.label
        if self.kind == "table":
            return f"table {self.obj}"
        if self.kind == "no_table":
            return f"table {self.obj} absent"
        if self.kind == "column":
            return f"{self.obj}.{self.member}"
        if self.kind == "column_type":
            return f"{self.obj}.{self.member} is {self.expected.upper()}"
        if self.kind == "not_null":
            return f"{self.obj}.{self.member} NOT NULL"
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


def col_type(table: str, column: str, data_type: str) -> Check:
    return Check("column_type", table, column, expected=data_type)


def not_null(table: str, column: str) -> Check:
    return Check("not_null", table, column)


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
    # Alternative corroboration for a drop-only migration: strings that, if
    # present in V1__prod_baseline.sql, prove the dropped object existed on this
    # database's lineage. The baseline is a mysqldump of prod taken before these
    # migrations, so it is a second, earlier snapshot the live schema can't give
    # us — if the object is in the dump and gone now, something dropped it.
    baseline_evidence: tuple[str, ...] = ()


MIGRATIONS: tuple[Migration, ...] = (
    # V1__prod_baseline.sql is a dump of prod itself — always "applied" by
    # definition, nothing to probe.
    Migration("V2", "audit_event_payload", (col("AUDIT_EVENT", "PAYLOAD"),)),
    Migration("V3", "ticket_layer",
              (tbl("TICKET"), tbl("TICKET_TARGET"), tbl("TICKET_EVENT"))),
    Migration("V4", "drop_duplicate_indexes",
              (no_idx("BIO_ASSAY_DIMENSIONS2BIO_ASSAYS", "BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC"),
               no_idx("RELEVANT_PUBLICATIONS", "INVESTIGATION_OTHER_RELEVANT_PUBLICATIONS_FKC")),
              caveat="drop-only migration: the live schema cannot tell whether these indexes "
                     "were dropped or never existed. Resolved against V1__prod_baseline.sql — "
                     "if the dump has them and the database does not, they were dropped",
              drop_only=True,
              baseline_evidence=("BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC",
                                 "INVESTIGATION_OTHER_RELEVANT_PUBLICATIONS_FKC")),
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
    Migration("V26", "annotation_relation",
              (tbl("ANNOTATION_RELATION"),)),
    Migration("V27", "annotation_relation_status",
              (col("ANNOTATION_RELATION", "STATUS"),)),
    Migration("V28", "annotation_relation_evidence",
              (col("ANNOTATION_RELATION", "EVIDENCE"),
               col("ANNOTATION_RELATION", "SUPPORTING_EVIDENCE"))),
    # MODIFY COLUMN, not ADD: the column exists either way, so only its type
    # separates applied from not.
    Migration("V29", "widen_bib_ref_annotation_term",
              (col_type("BIB_REF_ANNOTATION", "TERM", "text"),)),
    Migration("V30", "annotation_set_triage",
              (col("ANNOTATION_SET", "TRIAGE"), col("ANNOTATION_SET", "TRIAGED_BY"),
               col("ANNOTATION_SET", "TRIAGED_AT")),
              caveat="V32 moves triage into its own table and drops these three "
                     "columns; if V32 ran, V30 is unknowable from the schema"),
    Migration("V31", "curation_lock",
              (tbl("CURATION_LOCK"),)),
    Migration("V32", "annotation_set_triage_rows",
              (tbl("ANNOTATION_SET_TRIAGE"),)),
    Migration("V33", "annotation_set_agent_name_and_run_sha",
              (col("ANNOTATION_SET", "RUN_SHA"), col("ANNOTATION_SET", "AGENT_NAME"))),
    Migration("V34", "curation_lock_holder_identity",
              (col("CURATION_LOCK", "RUN_ID"), col("CURATION_LOCK", "AGENT_NAME"))),
    Migration("V35", "ticket_target_screening_result",
              (col("TICKET_TARGET", "SCREENING_RESULT"),
               col("TICKET_TARGET", "SCREENING_RESULT_REASON"))),
    # V36-V38 tighten nullability on columns that already exist. Presence proves
    # nothing here either — IS_NULLABLE is the fingerprint.
    Migration("V36", "bio_assay_sample_used_not_null",
              (not_null("BIO_ASSAY", "SAMPLE_USED_FK"),)),
    Migration("V37", "array_design_curation_details_not_null",
              (not_null("ARRAY_DESIGN", "CURATION_DETAILS_FK"),)),
    Migration("V38", "vector_quantitation_type_not_null",
              (not_null("RAW_EXPRESSION_DATA_VECTOR", "QUANTITATION_TYPE_FK"),
               not_null("PROCESSED_EXPRESSION_DATA_VECTOR", "QUANTITATION_TYPE_FK"))),
    Migration("V39", "ticket_accepts_targets",
              (col("TICKET", "ACCEPTS_TARGETS"),)),
    Migration("V40", "ticket_scratchpad_unique_per_curator",
              (col("TICKET", "SCRATCHPAD_OWNER_FK"),
               idx("TICKET", "TICKET_ONE_SCRATCHPAD_PER_CURATOR"))),
)


# ---------------------------------------------------------------------------
# information_schema probing
# ---------------------------------------------------------------------------

class Schema:
    """Case-insensitive snapshot of the bits of information_schema we need."""

    def __init__(self, tables: set[str], columns: set[tuple[str, str]],
                 indexes: set[tuple[str, str]], cascading_fks: set[tuple[str, str]],
                 column_types: dict[str, str] | None = None,
                 not_null_columns: set[tuple[str, str]] | None = None):
        self.tables = tables
        self.columns = columns
        self.indexes = indexes
        self.cascading_fks = cascading_fks
        self.column_types = column_types or {}
        self.not_null_columns = not_null_columns or set()
        self.server = ""   # @@hostname, filled in by load_schema

    def holds(self, check: Check) -> bool:
        o, m = check.obj.upper(), check.member.upper()
        if check.kind == "table":
            return o in self.tables
        if check.kind == "no_table":
            return o not in self.tables
        if check.kind == "column":
            return (o, m) in self.columns
        if check.kind == "column_type":
            return self.column_types.get(f"{o}.{m}") == check.expected.upper()
        if check.kind == "not_null":
            return (o, m) in self.not_null_columns
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
    # Keyed "TABLE.COLUMN" so the (col1, col2) row shape still carries a value.
    "column_types": "SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME), DATA_TYPE "
                    "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()",
    "not_null_columns": "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS "
                        "WHERE TABLE_SCHEMA = DATABASE() AND IS_NULLABLE = 'NO'",
    "indexes": "SELECT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS "
               "WHERE TABLE_SCHEMA = DATABASE()",
    # Reported in the header. With --login-path the host lives inside
    # ~/.mylogin.cnf and never reaches argv, so asking the server is the only
    # way the report can name the database it actually probed.
    "server": "SELECT @@hostname, ''",
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
    column_types = dict(fetch("column_types"))
    schema = Schema(tables, fetch("columns"), fetch("indexes"), fetch("cascading_fks"),
                    column_types, fetch("not_null_columns"))
    server = run_mysql(QUERIES["server"], creds, args, option_file)
    schema.server = server[0][0] if server else ""
    return schema


# ---------------------------------------------------------------------------
# Verdicts & reporting
# ---------------------------------------------------------------------------

APPLIED, MISSING, PARTIAL, INDETERMINATE = "APPLIED", "MISSING", "PARTIAL", "INDETERMINATE"


def vnum(version: str) -> int:
    """'V23' -> 23. Fractional versions ('V23_1') sort by their integer part."""
    return int(version[1:].split("_")[0])


def verdict(migration: Migration, schema: Schema,
            baseline: str | None = None) -> tuple[str, list[str]]:
    held = [c for c in migration.checks if schema.holds(c)]
    failed = [c for c in migration.checks if not schema.holds(c)]
    if failed and held:
        return PARTIAL, [c.describe() for c in failed]
    if failed:
        state = INDETERMINATE if migration.caveat else MISSING
        return state, [c.describe() for c in failed]
    # Every check holds. For a drop-only migration that is not evidence of
    # application unless something proves the dropped object was ever here.
    if migration.drop_only and not corroborates(migration, schema, baseline):
        return INDETERMINATE, []
    return APPLIED, []


def corroborates(migration: Migration, schema: Schema, baseline: str | None) -> bool:
    """Is there independent evidence that this drop-only migration really ran?"""
    if migration.corroborated_by is not None and schema.holds(migration.corroborated_by):
        return True
    if baseline and migration.baseline_evidence:
        return all(marker in baseline for marker in migration.baseline_evidence)
    return False


DEFAULT_BASELINE = "gemma-core/src/main/resources/db/migration/mysql/V1__prod_baseline.sql"


def load_baseline(explicit: str | None) -> str | None:
    """Read V1__prod_baseline.sql, the pre-migration snapshot of prod."""
    if explicit == "":
        return None
    path = explicit
    if path is None:
        repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path = os.path.join(repo_root, DEFAULT_BASELINE)
        if not os.path.exists(path):
            return None
    try:
        with open(os.path.expanduser(path), encoding="utf-8", errors="replace") as fh:
            return fh.read()
    except OSError as exc:
        raise SystemExit(f"cannot read baseline {path}: {exc}")


def render(results: list[tuple[Migration, str, list[str]]], creds: Credentials,
           args: argparse.Namespace, used_baseline: bool = False,
           server: str = "") -> str:
    lines = [
        f"# Applied-migration probe — {server or args.host or 'localhost'}/{args.database}",
        "",
        f"Credential source: {creds.source}",
        "",
        "Drop-only migrations cross-checked against V1__prod_baseline.sql."
        if used_baseline else
        "No baseline dump consulted — drop-only migrations cannot be resolved.",
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
        highest_n = vnum(highest.version)
        # A baseline is only meaningful over a CONTIGUOUS run of applied
        # migrations. Anything known-missing below the high-water mark is a hole
        # that a baseline would paper over permanently: Flyway marks every
        # version <= baseline as applied and never revisits it.
        holes = [m for m, v, _ in results
                 if v in (MISSING, PARTIAL) and vnum(m.version) < highest_n]
        lines += [
            "",
            "## Implication for the Flyway cutover",
            "",
            f"Highest applied migration: **{highest.version}** "
            f"(`{highest.version}__{highest.name}`).",
            "",
            "docs/design/FLYWAY_PROD_FOLLOWUP.md currently specifies "
            "`baselineVersion(\"1\")`. That is only correct for a database that has "
            "received nothing since V1__prod_baseline. With the migrations above "
            "already present, a baseline at 1 would make Flyway attempt to re-apply "
            "each of them on first migrate and fail on a duplicate column or table, "
            "mid-cutover.",
        ]
        if holes:
            hole_list = ", ".join(f"`{m.version}__{m.name}`" for m in holes)
            safe = min(vnum(m.version) for m in holes) - 1
            lines += [
                "",
                f"**This host cannot simply be baselined at {highest.version}.** The "
                f"applied set is not a contiguous prefix — {hole_list} "
                f"{'is' if len(holes) == 1 else 'are'} missing *below* the high-water "
                "mark. Baselining above a hole marks it applied and Flyway will never "
                "run it, so the gap becomes permanent and silent.",
                "",
                "Two ways out, in order of preference:",
                "",
                f"1. Apply the missing migration{'' if len(holes) == 1 else 's'} by hand "
                f"first, making the chain contiguous, then baseline at {highest.version}.",
                f"2. Baseline at V{safe} — the last version before the first hole — and let "
                f"Flyway apply everything above it. Only viable if the already-applied "
                f"migrations above V{safe} are individually re-runnable, which most are "
                f"not: a second ADD COLUMN or CREATE TABLE errors out.",
            ]
        else:
            lines += [
                "",
                f"The applied set is a contiguous prefix, so `baselineVersion(\"{highest_n}\")` "
                "is safe here. Reconcile anything PARTIAL first.",
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
    p.add_argument("--baseline", default=None,
                   help="V1__prod_baseline.sql, used as an earlier snapshot to resolve "
                        "drop-only migrations (default: the copy in this repo; "
                        "--baseline '' to skip)")
    args = p.parse_args()

    baseline = load_baseline(args.baseline)

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

    results = [(m, *verdict(m, schema, baseline)) for m in MIGRATIONS]
    report = render(results, creds, args, baseline is not None, schema.server)

    if args.out:
        with open(args.out, "w") as fh:
            fh.write(report)
        print(f"wrote {args.out}")
    else:
        print(report, end="")

    return 1 if any(v == PARTIAL for _, v, _ in results) else 0


if __name__ == "__main__":
    sys.exit(main())
