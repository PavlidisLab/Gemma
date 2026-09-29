#!/usr/bin/env python3
"""
Remove BioAssays (and the BioMaterials / BioAssayDimensions that go with them)
that were stranded by ExpressionExperimentSubSet deletion.

Why this exists: until the fix in ExpressionExperimentSubSetDaoImpl, `remove()`
refused to delete a subset-owned BioAssay while any BioAssayDimension still
listed it. When the dimension was used *only* by those subsets' assays -- the
normal shape for single-cell population subsets, where one dimension spans every
cell-type subset of a split -- nothing ever deleted the dimension, so it pinned
its assays (and their samples) in the database permanently, with no experiment
and no subset left pointing at them. The DAO no longer creates this state; this
script clears what earlier runs already left behind.

Two phases, deliberately separate:

    plan    read-only. Derives the orphan set from the database, runs every
            safety guard, and writes a manifest (JSON) listing exactly which
            rows would be deleted, with a sha256 over the id lists.
    apply   re-derives the set, re-runs every guard, checks the sha256 against
            the manifest, and only then deletes -- in one transaction.

The sha256 is the point of the split: if anything about the orphan set changed
between planning and applying (a concurrent load, a subset created, an assay
re-attached), the digest moves and apply refuses. Nothing is deleted on the
strength of a stale plan.

Usage:
    scripts/prune_orphaned_subset_bioassays.py plan  --login-path gemd-ro \\
        --out runs/prune-$(date +%Y%m%d)/manifest.json
    scripts/prune_orphaned_subset_bioassays.py apply --login-path gemd-rw \\
        --manifest runs/prune-$(date +%Y%m%d)/manifest.json

`plan` issues nothing but SELECTs and is safe against production at any time.
`apply` needs an account with DELETE on gemd; it prints the row counts it
removed per table and rewrites the manifest with the applied result.

Credentials: resolved the same way as scripts/probe_applied_migrations.py --
never passed on the command line, since argv is world-readable via ps(1).
"""

from __future__ import annotations

import argparse
import getpass
import hashlib
import json
import os
import platform
import subprocess
import sys
import tempfile
from dataclasses import dataclass, asdict, field
from datetime import datetime, timezone

KEYRING_SERVICE = "gemma-db"

# Chunk size for `WHERE id IN (...)`. Keeps statements well inside
# max_allowed_packet and keeps the optimizer on the index.
CHUNK = 1000


# ---------------------------------------------------------------------------
# Credential resolution (mirrors scripts/probe_applied_migrations.py)
# ---------------------------------------------------------------------------

@dataclass
class Credentials:
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
        return None
    return (user, password) if user and password else None


def _from_macos_keychain() -> tuple[str, str] | None:
    if platform.system() != "Darwin":
        return None

    def fetch(service: str) -> str | None:
        out = subprocess.run(["security", "find-generic-password", "-s", service, "-w"],
                             capture_output=True, text=True, check=False)
        return out.stdout.strip() if out.returncode == 0 else None

    user, password = fetch("GEMMA_DB_USER"), fetch("GEMMA_DB_PASSWORD")
    return (user, password) if user and password else None


def resolve_credentials(args: argparse.Namespace) -> Credentials:
    if args.defaults_file:
        return Credentials(defaults_file=os.path.expanduser(args.defaults_file),
                           source=f"--defaults-file {args.defaults_file}")
    if args.login_path:
        return Credentials(login_path=args.login_path, source=f"--login-path {args.login_path}")

    env_user, env_pass = os.environ.get("GEMMA_DB_USER"), os.environ.get("GEMMA_DB_PASSWORD")
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
        "  --login-path NAME        (mysql_config_editor set --login-path=NAME ...)\n"
        "  --defaults-file PATH     (a [client] option file, chmod 600)\n"
        "  GEMMA_DB_USER / GEMMA_DB_PASSWORD in the environment\n"
        f"  python keyring, service '{KEYRING_SERVICE}'\n"
        "  or run interactively to be prompted."
    )


# ---------------------------------------------------------------------------
# mysql client
# ---------------------------------------------------------------------------

class Mysql:
    def __init__(self, creds: Credentials, args: argparse.Namespace, option_file: str | None):
        self.creds, self.args, self.option_file = creds, args, option_file

    def _cmd(self) -> list[str]:
        cmd = ["mysql"]
        # --defaults-file/--login-path must come first, before any other option.
        if self.option_file:
            cmd.append(f"--defaults-file={self.option_file}")
        elif self.creds.defaults_file:
            cmd.append(f"--defaults-file={self.creds.defaults_file}")
        elif self.creds.login_path:
            cmd.append(f"--login-path={self.creds.login_path}")
        if self.args.host:
            cmd.append(f"--host={self.args.host}")
        if self.args.port:
            cmd.append(f"--port={self.args.port}")
        return cmd + ["--batch", "--raw", "--skip-column-names", "--database", self.args.database]

    def run(self, sql: str) -> list[list[str]]:
        out = subprocess.run(self._cmd() + ["-e", sql], capture_output=True, text=True, check=False)
        if out.returncode != 0:
            raise SystemExit(f"mysql failed ({out.returncode}): {out.stderr.strip()}")
        return [line.split("\t") for line in out.stdout.splitlines() if line.strip()]

    def ids(self, sql: str) -> list[int]:
        return sorted(int(r[0]) for r in self.run(sql))

    def scalar(self, sql: str) -> int:
        rows = self.run(sql)
        return int(rows[0][0]) if rows else 0

    def count_in(self, table: str, column: str, ids: list[int], extra: str = "") -> int:
        """COUNT(*) over `table` for `column IN (ids)`, chunked."""
        total = 0
        for chunk in chunks(ids):
            where = f"{column} IN ({join_ids(chunk)})"
            if extra:
                where += f" AND {extra}"
            total += self.scalar(f"SELECT COUNT(*) FROM {table} WHERE {where}")
        return total

    def script(self, sql: str) -> str:
        """Run a multi-statement script; used only by apply().

        Fed on stdin, not `-e`: the delete script for one 3,050-assay dimension already exceeds the 128 KiB a
        single argv string may hold (E2BIG, "Argument list too long"), and it grows with the manifest. mysql still
        stops at the first failing statement in batch mode, and the script runs with autocommit off, so a failure
        part-way leaves nothing committed.
        """
        out = subprocess.run(self._cmd(), input=sql, capture_output=True, text=True, check=False)
        if out.returncode != 0:
            raise SystemExit(f"mysql failed ({out.returncode}): {out.stderr.strip()}")
        return out.stdout


def chunks(ids: list[int], size: int = CHUNK):
    for i in range(0, len(ids), size):
        yield ids[i:i + size]


def join_ids(ids: list[int]) -> str:
    return ",".join(str(i) for i in ids)


# ---------------------------------------------------------------------------
# Discovery
# ---------------------------------------------------------------------------
#
# "Orphaned" means the row is unreachable from any surviving experiment:
#
#   BioAssay          no EXPRESSION_EXPERIMENT_FK and no subset membership
#   BioMaterial       every BioAssay that uses it is an orphaned BioAssay
#   BioAssayDimension every BioAssay it lists is an orphaned BioAssay, and
#                     nothing indexes data by position within it

Q_ORPHAN_BIOASSAYS = """
SELECT ba.ID
FROM BIO_ASSAY ba
LEFT JOIN BIO_ASSAYS2EXPRESSION_EXPERIMENT_SUB_SET s ON s.BIO_ASSAYS_FK = ba.ID
WHERE ba.EXPRESSION_EXPERIMENT_FK IS NULL AND s.BIO_ASSAYS_FK IS NULL
"""

Q_ORPHAN_BIOMATERIALS = """
SELECT ba.SAMPLE_USED_FK
FROM BIO_ASSAY ba
LEFT JOIN BIO_ASSAYS2EXPRESSION_EXPERIMENT_SUB_SET s ON s.BIO_ASSAYS_FK = ba.ID
GROUP BY ba.SAMPLE_USED_FK
HAVING SUM(ba.EXPRESSION_EXPERIMENT_FK IS NOT NULL OR s.BIO_ASSAYS_FK IS NOT NULL) = 0
"""

Q_FULLY_ORPHAN_DIMENSIONS = """
SELECT d2b.BIO_ASSAY_DIMENSIONS_FK
FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS d2b
JOIN BIO_ASSAY ba ON ba.ID = d2b.BIO_ASSAYS_FK
LEFT JOIN BIO_ASSAYS2EXPRESSION_EXPERIMENT_SUB_SET s ON s.BIO_ASSAYS_FK = ba.ID
GROUP BY d2b.BIO_ASSAY_DIMENSIONS_FK
HAVING SUM(ba.EXPRESSION_EXPERIMENT_FK IS NOT NULL OR s.BIO_ASSAYS_FK IS NOT NULL) = 0
"""

# Anything that indexes its values by an assay's position in a dimension. Pulling
# an assay out of such a dimension would silently misalign the data, so a
# dimension named here is off limits -- and so are its assays.
DIMENSION_DATA_TABLES = [
    "RAW_EXPRESSION_DATA_VECTOR",
    "PROCESSED_EXPRESSION_DATA_VECTOR",
    "ANALYSIS",                    # PrincipalComponentAnalysis
    "SAMPLE_COEXPRESSION_MATRIX",
]

# Child rows that must go before the BioMaterial itself.
BIOMATERIAL_CHILD_TABLES = [
    ("CHARACTERISTIC", "BIO_MATERIAL_FK"),
    ("BIO_MATERIAL_FACTOR_VALUES", "BIO_MATERIALS_FK"),
    ("TREATMENT", "BIO_MATERIALS_FK"),
]


@dataclass
class Plan:
    generated_at: str
    server: str
    database: str
    bio_assay_ids: list[int] = field(default_factory=list)
    bio_material_ids: list[int] = field(default_factory=list)
    bio_assay_dimension_ids: list[int] = field(default_factory=list)
    child_row_counts: dict[str, int] = field(default_factory=dict)
    sha256: str = ""
    applied_at: str | None = None
    applied_deletions: dict[str, int] = field(default_factory=dict)

    def digest(self) -> str:
        """Fingerprint of *what* would be deleted, independent of when."""
        payload = json.dumps({
            "server": self.server,
            "database": self.database,
            "bio_assay_ids": self.bio_assay_ids,
            "bio_material_ids": self.bio_material_ids,
            "bio_assay_dimension_ids": self.bio_assay_dimension_ids,
        }, sort_keys=True, separators=(",", ":"))
        return hashlib.sha256(payload.encode()).hexdigest()


def discover(db: Mysql, args: argparse.Namespace) -> Plan:
    server = db.run("SELECT @@hostname")[0][0]

    bas = db.ids(Q_ORPHAN_BIOASSAYS)
    bms = db.ids(Q_ORPHAN_BIOMATERIALS)
    candidate_bads = db.ids(Q_FULLY_ORPHAN_DIMENSIONS)

    # Drop any dimension that still indexes data. In practice this never fires
    # (a dimension whose assays are all orphaned has no experiment left to own
    # its vectors), but a dimension that did would make its assays undeletable,
    # so it is checked rather than assumed.
    bads, in_use = [], []
    for bad in candidate_bads:
        used = any(db.count_in(t, "BIO_ASSAY_DIMENSION_FK", [bad]) for t in DIMENSION_DATA_TABLES)
        (in_use if used else bads).append(bad)
    if in_use:
        print(f"note: {len(in_use)} fully-orphaned dimension(s) still index data and are left "
              f"alone, along with their assays: {in_use}", file=sys.stderr)
        keep_bas: set[int] = set()
        for chunk in chunks(in_use):
            keep_bas.update(db.ids("SELECT BIO_ASSAYS_FK FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS "
                                   f"WHERE BIO_ASSAY_DIMENSIONS_FK IN ({join_ids(chunk)})"))
        keep_bms: set[int] = set()
        for chunk in chunks(sorted(keep_bas)):
            keep_bms.update(db.ids("SELECT SAMPLE_USED_FK FROM BIO_ASSAY "
                                   f"WHERE ID IN ({join_ids(chunk)})"))
        bas = [i for i in bas if i not in keep_bas]
        bms = [i for i in bms if i not in keep_bms]

    plan = Plan(
        generated_at=datetime.now(timezone.utc).isoformat(),
        server=server,
        database=args.database,
        bio_assay_ids=bas,
        bio_material_ids=bms,
        bio_assay_dimension_ids=bads,
    )
    plan.child_row_counts = {
        f"{table}.{column}": db.count_in(table, column, bms)
        for table, column in BIOMATERIAL_CHILD_TABLES
    }
    plan.child_row_counts["BIO_ASSAY_DIMENSIONS2BIO_ASSAYS.BIO_ASSAYS_FK"] = \
        db.count_in("BIO_ASSAY_DIMENSIONS2BIO_ASSAYS", "BIO_ASSAYS_FK", bas)
    plan.sha256 = plan.digest()
    return plan


# ---------------------------------------------------------------------------
# Guards
# ---------------------------------------------------------------------------
#
# Every one of these must hold before a single row is deleted. They restate the
# orphan definition as independent assertions, so a bug in the discovery queries
# above cannot on its own destroy live data.

def check(db: Mysql, plan: Plan) -> list[str]:
    bas, bms, bads = plan.bio_assay_ids, plan.bio_material_ids, plan.bio_assay_dimension_ids
    failures: list[str] = []

    def fail_if(count: int, message: str) -> None:
        if count:
            failures.append(f"{message}: {count}")

    if not bas and not bms and not bads:
        return failures  # nothing to do; caller reports it

    # 1. no candidate assay belongs to an experiment or a subset
    fail_if(db.count_in("BIO_ASSAY", "ID", bas, "EXPRESSION_EXPERIMENT_FK IS NOT NULL"),
            "candidate BioAssays still attached to an ExpressionExperiment")
    fail_if(db.count_in("BIO_ASSAYS2EXPRESSION_EXPERIMENT_SUB_SET", "BIO_ASSAYS_FK", bas),
            "candidate BioAssays still in a subset")

    # 2. no candidate assay sits in a dimension we are not also deleting
    bad_set = set(bads)
    stray = 0
    for chunk in chunks(bas):
        rows = db.run("SELECT DISTINCT BIO_ASSAY_DIMENSIONS_FK FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS "
                      f"WHERE BIO_ASSAYS_FK IN ({join_ids(chunk)})")
        stray += sum(1 for r in rows if int(r[0]) not in bad_set)
    fail_if(stray, "candidate BioAssays sit in BioAssayDimensions outside the delete set")

    # 3. no candidate assay is in a SingleCellDimension (immutable; would misalign)
    fail_if(db.count_in("BIO_ASSAYS2SINGLE_CELL_DIMENSIONS", "BIO_ASSAYS_FK", bas),
            "candidate BioAssays still in a SingleCellDimension")

    # 4. no candidate dimension indexes data
    for table in DIMENSION_DATA_TABLES:
        fail_if(db.count_in(table, "BIO_ASSAY_DIMENSION_FK", bads),
                f"candidate BioAssayDimensions referenced by {table}")

    # 5. every dimension we delete contains only assays we delete
    ba_set = set(bas)
    kept = 0
    for chunk in chunks(bads):
        rows = db.run("SELECT BIO_ASSAYS_FK FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS "
                      f"WHERE BIO_ASSAY_DIMENSIONS_FK IN ({join_ids(chunk)})")
        kept += sum(1 for r in rows if int(r[0]) not in ba_set)
    fail_if(kept, "candidate BioAssayDimensions still list BioAssays outside the delete set")

    # 6. every sample we delete is used only by assays we delete
    survivors = 0
    for chunk in chunks(bms):
        rows = db.run(f"SELECT ID FROM BIO_ASSAY WHERE SAMPLE_USED_FK IN ({join_ids(chunk)})")
        survivors += sum(1 for r in rows if int(r[0]) not in ba_set)
    fail_if(survivors, "candidate BioMaterials still used by BioAssays outside the delete set")

    # 7. nothing outside the delete set derives from a sample we delete
    bm_set = set(bms)
    derived = 0
    for chunk in chunks(bms):
        rows = db.run("SELECT ID FROM BIO_MATERIAL "
                      f"WHERE SOURCE_BIO_MATERIAL_FK IN ({join_ids(chunk)})")
        derived += sum(1 for r in rows if int(r[0]) not in bm_set)
    fail_if(derived, "candidate BioMaterials are the source of BioMaterials outside the delete set")

    return failures


# ---------------------------------------------------------------------------
# Apply
# ---------------------------------------------------------------------------

def build_delete_script(plan: Plan) -> str:
    """FK-safe deletion order, wrapped in one transaction."""
    bas, bms, bads = plan.bio_assay_ids, plan.bio_material_ids, plan.bio_assay_dimension_ids
    stmts: list[str] = ["SET autocommit = 0;", "START TRANSACTION;"]

    def emit(sql_for: str, ids: list[int]) -> None:
        for chunk in chunks(ids):
            stmts.append(sql_for.format(ids=join_ids(chunk)))

    # children of BioMaterial
    for table, column in BIOMATERIAL_CHILD_TABLES:
        emit(f"DELETE FROM {table} WHERE {column} IN ({{ids}});", bms)
    # a derived sample inside the set would block its own source; break the link
    emit("UPDATE BIO_MATERIAL SET SOURCE_BIO_MATERIAL_FK = NULL "
         "WHERE ID IN ({ids}) AND SOURCE_BIO_MATERIAL_FK IS NOT NULL;", bms)
    # join rows, then the assays
    emit("DELETE FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS WHERE BIO_ASSAYS_FK IN ({ids});", bas)
    emit("DELETE FROM BIO_ASSAY WHERE ID IN ({ids});", bas)
    # the now-empty dimensions
    emit("DELETE FROM BIO_ASSAY_DIMENSION WHERE ID IN ({ids});", bads)
    # and finally the samples
    emit("DELETE FROM BIO_MATERIAL WHERE ID IN ({ids});", bms)

    stmts.append("COMMIT;")
    return "\n".join(stmts)


def apply(db: Mysql, plan: Plan, args: argparse.Namespace) -> dict[str, int]:
    before = {
        "BIO_ASSAY": db.count_in("BIO_ASSAY", "ID", plan.bio_assay_ids),
        "BIO_MATERIAL": db.count_in("BIO_MATERIAL", "ID", plan.bio_material_ids),
        "BIO_ASSAY_DIMENSION": db.count_in("BIO_ASSAY_DIMENSION", "ID", plan.bio_assay_dimension_ids),
    }
    for table, column in BIOMATERIAL_CHILD_TABLES:
        before[table] = db.count_in(table, column, plan.bio_material_ids)
    before["BIO_ASSAY_DIMENSIONS2BIO_ASSAYS"] = db.count_in(
        "BIO_ASSAY_DIMENSIONS2BIO_ASSAYS", "BIO_ASSAYS_FK", plan.bio_assay_ids)

    script = build_delete_script(plan)
    if args.sql_out:
        with open(args.sql_out, "w") as fh:
            fh.write(script + "\n")
        print(f"wrote SQL to {args.sql_out}")
    db.script(script)

    after = {
        "BIO_ASSAY": db.count_in("BIO_ASSAY", "ID", plan.bio_assay_ids),
        "BIO_MATERIAL": db.count_in("BIO_MATERIAL", "ID", plan.bio_material_ids),
        "BIO_ASSAY_DIMENSION": db.count_in("BIO_ASSAY_DIMENSION", "ID", plan.bio_assay_dimension_ids),
    }
    for table, column in BIOMATERIAL_CHILD_TABLES:
        after[table] = db.count_in(table, column, plan.bio_material_ids)
    after["BIO_ASSAY_DIMENSIONS2BIO_ASSAYS"] = db.count_in(
        "BIO_ASSAY_DIMENSIONS2BIO_ASSAYS", "BIO_ASSAYS_FK", plan.bio_assay_ids)

    deleted = {t: before[t] - after[t] for t in before}
    leftovers = {t: n for t, n in after.items() if n}
    if leftovers:
        raise SystemExit(f"rows survived the delete, transaction may have rolled back: {leftovers}")
    return deleted


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------

def describe(db: Mysql, plan: Plan) -> None:
    print(f"server            {plan.server}  db={plan.database}")
    print(f"generated_at      {plan.generated_at}")
    print(f"sha256            {plan.sha256}")
    print()
    print(f"BioAssay          {len(plan.bio_assay_ids):>7}")
    print(f"BioMaterial       {len(plan.bio_material_ids):>7}")
    print(f"BioAssayDimension {len(plan.bio_assay_dimension_ids):>7}")
    for key, n in sorted(plan.child_row_counts.items()):
        print(f"  {key:<52} {n:>7}")

    if not plan.bio_assay_dimension_ids:
        return
    print()
    print("by dimension:")
    rows = db.run(
        "SELECT d2b.BIO_ASSAY_DIMENSIONS_FK, COUNT(*), MIN(d2b.BIO_ASSAYS_FK), MAX(d2b.BIO_ASSAYS_FK) "
        "FROM BIO_ASSAY_DIMENSIONS2BIO_ASSAYS d2b "
        f"WHERE d2b.BIO_ASSAY_DIMENSIONS_FK IN ({join_ids(plan.bio_assay_dimension_ids)}) "
        "GROUP BY d2b.BIO_ASSAY_DIMENSIONS_FK ORDER BY d2b.BIO_ASSAY_DIMENSIONS_FK")
    print(f"  {'dimension':>10} {'assays':>8}  assay id range")
    for bad, n, lo, hi in rows:
        print(f"  {bad:>10} {n:>8}  {lo}-{hi}")


def ensure_writable(path: str, what: str) -> None:
    """Fail before the database work, not after it."""
    directory = os.path.dirname(os.path.abspath(path)) or "."
    try:
        os.makedirs(directory, exist_ok=True)
    except OSError as e:
        raise SystemExit(f"cannot create {what} directory {directory}: {e.strerror}")
    if not os.access(directory, os.W_OK):
        hint = ""
        if directory == "/":
            hint = ("\n(a path like '/manifest.json' usually means an unset shell variable -- "
                    "check that $RUN is exported in this shell)")
        raise SystemExit(f"cannot write {what} to {path}: {directory} is not writable{hint}")


def write_manifest(plan: Plan, path: str) -> None:
    ensure_writable(path, "manifest")
    with open(path, "w") as fh:
        json.dump(asdict(plan), fh, indent=2, sort_keys=True)
        fh.write("\n")
    print(f"\nmanifest -> {path}")


def read_manifest(path: str) -> Plan:
    with open(path) as fh:
        data = json.load(fh)
    return Plan(**data)


# ---------------------------------------------------------------------------

def main() -> int:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("mode", choices=["plan", "apply"])
    p.add_argument("--host", default=None, help="MySQL host (default: from option file)")
    p.add_argument("--port", type=int, default=None, help="MySQL port")
    p.add_argument("--database", default="gemd", help="database name (default: gemd)")
    p.add_argument("--defaults-file", default=None, help="a [client] option file, chmod 600")
    p.add_argument("--login-path", default=None, help="a mysql_config_editor login path")
    p.add_argument("--out", default=None, help="plan: write the manifest here")
    p.add_argument("--manifest", default=None, help="apply: the manifest from a prior plan")
    p.add_argument("--sql-out", default=None, help="apply: also write the DELETE script here")
    args = p.parse_args()

    if args.mode == "apply" and not args.manifest:
        p.error("apply requires --manifest from a prior `plan` run")

    # Check the output paths up front: the scan below takes a while and it is
    # maddening to lose it to an unwritable destination at the last step.
    if args.out:
        ensure_writable(args.out, "manifest")
    if args.mode == "apply":
        ensure_writable(args.manifest, "manifest")
        if args.sql_out:
            ensure_writable(args.sql_out, "SQL script")

    creds = resolve_credentials(args)
    option_file = None
    try:
        if creds.user and creds.password:
            fd, option_file = tempfile.mkstemp(prefix="gemma-prune-", suffix=".cnf")
            os.chmod(option_file, 0o600)
            with os.fdopen(fd, "w") as fh:
                fh.write(f"[client]\nuser={creds.user}\npassword={creds.password}\n")

        db = Mysql(creds, args, option_file)
        print(f"credentials from {creds.source}\n")

        plan = discover(db, args)

        if args.mode == "plan":
            describe(db, plan)
            failures = check(db, plan)
            print()
            if failures:
                print("GUARDS FAILED -- do not apply:")
                for f in failures:
                    print(f"  - {f}")
                return 1
            print("all guards passed")
            if not plan.bio_assay_ids:
                print("nothing to prune")
            if args.out:
                write_manifest(plan, args.out)
            return 0

        # apply
        recorded = read_manifest(args.manifest)
        if plan.sha256 != recorded.sha256:
            print("REFUSING TO APPLY: the orphan set has changed since the plan was written.")
            print(f"  manifest  {recorded.sha256}  ({recorded.generated_at})")
            print(f"  now       {plan.sha256}  ({plan.generated_at})")
            print(f"  BioAssay          {len(recorded.bio_assay_ids)} -> {len(plan.bio_assay_ids)}")
            print(f"  BioMaterial       {len(recorded.bio_material_ids)} -> {len(plan.bio_material_ids)}")
            print(f"  BioAssayDimension {len(recorded.bio_assay_dimension_ids)} -> "
                  f"{len(plan.bio_assay_dimension_ids)}")
            print("Re-run `plan` and review the delta before applying.")
            return 1

        failures = check(db, plan)
        if failures:
            print("GUARDS FAILED -- nothing deleted:")
            for f in failures:
                print(f"  - {f}")
            return 1

        describe(db, plan)
        print()
        deleted = apply(db, plan, args)
        plan.applied_at = datetime.now(timezone.utc).isoformat()
        plan.applied_deletions = deleted
        print("deleted:")
        for table, n in sorted(deleted.items()):
            print(f"  {table:<40} {n:>7}")
        write_manifest(plan, args.manifest)
        return 0
    finally:
        if option_file and os.path.exists(option_file):
            os.unlink(option_file)


if __name__ == "__main__":
    sys.exit(main())
