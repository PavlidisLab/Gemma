#!/usr/bin/env python3
"""
Backfill ``Investigation.sourceMetadata`` for a named list of experiments, by
driving the ``updateGeoSourceMetadata`` CLI.

Defaults to the 500 reference datasets the curation agent was evaluated on
(``polished_gold_500/gse_list_500.txt``), which is a 500-line list of Gemma
short names — including the ``.1`` / ``.2`` split forms.

What it does, in order:

1. Reads the list (``#`` comments and blank lines skipped, first tab-field
   taken — the same rules the CLI's own ``-f`` parser uses).
2. Resolves every name against a running Gemma over REST, in chunks, and
   reports what will and will not be processed.
3. Writes a clean ``-f`` list file holding only the names that resolved, with
   a provenance header.
4. Prints the ``gemma-cli`` invocation; with ``--run``, executes it and
   summarizes the per-experiment batch TSV it produces.

Step 2 is the point of the script. ``ExpressionExperimentManipulatingCLI``
resolves the whole ``-f`` file up front and ``EntityLocatorImpl`` throws on the
first name it cannot find, so ONE stale accession in a 500-line list aborts the
run before a single experiment is touched.

🛑 gemma-cli targets PRODUCTION (``~/Gemma.properties`` sets the whole
``gemma.db.url``). ``--run`` writes to the production database. The write is
narrow — ``GeoUpdateConfig.sourceMetadata(true)`` and nothing else, so no
curated field is touched — but it is production, and it is one GEO fetch per
experiment on the rate-limited path.

Resumable: the CLI skips an experiment that already has a document (a
projection, not a read of the LONGTEXT), so re-issuing the same list after an
interrupted run costs only the skips. ``--force`` refetches and replaces.

Usage:
    scripts/backfill_geo_source_metadata.py                      # preflight only, prints the command
    scripts/backfill_geo_source_metadata.py --limit 200          # first 200 of the list
    scripts/backfill_geo_source_metadata.py --limit 200 --run    # ...and actually run it
    scripts/backfill_geo_source_metadata.py --run --threads 2
    scripts/backfill_geo_source_metadata.py --summarize out/batch.tsv
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

# The list lives in the eval repo on a workstation; on frink it is shipped
# alongside this script, so look there when the eval repo is not present.
_EVAL_LIST = Path.home() / "Dev/gemma-curation-agents-eval/data/polished_gold_500/gse_list_500.txt"
_SIBLING_LIST = Path(__file__).resolve().parent / "gse_list_500.txt"
DEFAULT_LIST = _EVAL_LIST if _EVAL_LIST.exists() else _SIBLING_LIST

# Output goes under the deployment root on frink rather than into $HOME.
_GEMMA_ROOT = Path.home() / "Gemma2.0"

# On frink the 2.0 CLI is the wrapper under the deployment root. /space/opt/bin/gemma-cli
# is a different deployment and need not carry updateGeoSourceMetadata.
_FRINK_CLI = _GEMMA_ROOT / "bin" / "gemma-cli-2.0.sh"


def default_cli() -> str:
    if os.environ.get("GEMMA_CLI"):
        return os.environ["GEMMA_CLI"]
    if _FRINK_CLI.exists():
        return str(_FRINK_CLI)
    return "gemma-cli"
DEFAULT_BASE = "https://gemma2.msl.ubc.ca"
CHUNK = 50          # short names per /datasets?filter= request
PAGE_LIMIT = 100    # server-side page cap


# ---------------------------------------------------------------------------
# Credentials & HTTP  (same shape as scripts/perf_search.py)
# ---------------------------------------------------------------------------

def keychain(*services: str) -> str | None:
    """First macOS Keychain entry that answers, or None. No-op off macOS —
    frink has no `security`, so credentials there come from the environment."""
    for service in services:
        if not service:
            continue
        try:
            out = subprocess.run(
                ["security", "find-generic-password", "-s", service, "-w"],
                capture_output=True, text=True, check=False,
            )
        except FileNotFoundError:
            return None
        if out.returncode == 0 and out.stdout.strip():
            return out.stdout.strip()
    return None


def resolve_secret(var: str, *services: str) -> str | None:
    """Honour a pre-set env var, else fall back to the Keychain."""
    if os.environ.get(var):
        return os.environ[var]
    return keychain(os.environ.get(f"{var}_KEYCHAIN_ENTRY", ""), *services)


def http_get(url: str, token: str | None = None, timeout: float = 60.0):
    headers = {"Accept-Encoding": "gzip", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
    except urllib.error.HTTPError as e:
        raise SystemExit(f"GET {url} -> {e.code}: {e.read().decode(errors='replace')[:300]}")
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    return json.loads(raw)


def login(base: str, username: str, password: str) -> str:
    req = urllib.request.Request(
        f"{base}/rest/v2/login",
        data=json.dumps({"username": username, "password": password}).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        body = json.loads(resp.read())
    token = (body.get("data") or {}).get("token")
    if not token:
        raise SystemExit(f"login failed: {body}")
    return token


# ---------------------------------------------------------------------------
# The list
# ---------------------------------------------------------------------------

def read_list(path: Path) -> list[str]:
    """Read a dataset list the way the CLI's own -f parser reads it."""
    names: list[str] = []
    seen: set[str] = set()
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        name = line.split("\t")[0].strip()
        if name and name not in seen:
            seen.add(name)
            names.append(name)
    return names


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    h.update(path.read_bytes())
    return h.hexdigest()


# ---------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------

def resolve_all(base: str, names: list[str], token: str | None) -> dict[str, dict]:
    """short name -> dataset VO, for every name Gemma knows. Missing names are absent."""
    found: dict[str, dict] = {}
    for i in range(0, len(names), CHUNK):
        chunk = names[i:i + CHUNK]
        query = urllib.parse.urlencode({
            "filter": "shortName in (%s)" % ",".join(chunk),
            "limit": PAGE_LIMIT,
        })
        body = http_get(f"{base}/rest/v2/datasets?{query}", token=token)
        for vo in body.get("data") or []:
            found[vo["shortName"]] = vo
        sys.stderr.write(f"\r  resolved {min(i + CHUNK, len(names))}/{len(names)} ...")
        sys.stderr.flush()
    sys.stderr.write("\n")
    return found


def classify(names: list[str], found: dict[str, dict]) -> dict[str, list]:
    buckets: dict[str, list] = {"ok": [], "missing": [], "non_geo": [], "troubled": []}
    for name in names:
        vo = found.get(name)
        if vo is None:
            buckets["missing"].append(name)
            continue
        if (vo.get("externalDatabase") or "").upper() != "GEO":
            # The CLI counts these as successes ("nothing to store"), but there
            # is no reason to send them.
            buckets["non_geo"].append(name)
            continue
        if vo.get("troubled"):
            # removeTroubledExperiments() drops these unless --force is given.
            buckets["troubled"].append(name)
        buckets["ok"].append(name)
    return buckets


def write_list_file(dest: Path, names: list[str], header: list[str]) -> None:
    with dest.open("w") as fh:
        for line in header:
            fh.write(f"# {line}\n")
        for name in names:
            fh.write(f"{name}\n")


# ---------------------------------------------------------------------------
# Run & summarize
# ---------------------------------------------------------------------------

def build_command(cli: str, list_file: Path, batch_tsv: Path,
                  threads: int, force: bool) -> list[str]:
    cmd = [cli, "updateGeoSourceMetadata",
           "-f", str(list_file),
           "-threads", str(threads),
           "-batchFormat", "TSV",
           "-batchOutputFile", str(batch_tsv)]
    if force:
        cmd.append("-force")
    return cmd


def summarize(batch_tsv: Path) -> int:
    """Tally the CLI's per-experiment TSV: source, resultType, message, rootCause."""
    if not batch_tsv.exists():
        print(f"no batch summary at {batch_tsv}")
        return 1
    rows = []
    for line in batch_tsv.read_text().splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        parts += [""] * (4 - len(parts))
        rows.append(parts[:4])

    by_type: dict[str, int] = {}
    by_message: dict[str, int] = {}
    errors = []
    for source, result_type, message, cause in rows:
        by_type[result_type] = by_type.get(result_type, 0) + 1
        by_message[message] = by_message.get(message, 0) + 1
        if result_type == "ERROR":
            errors.append((source, message, cause))

    print(f"\n{len(rows)} experiments reported in {batch_tsv}")
    for result_type, n in sorted(by_type.items(), key=lambda kv: -kv[1]):
        print(f"  {result_type:<8} {n}")
    print("\n  by message:")
    for message, n in sorted(by_message.items(), key=lambda kv: -kv[1]):
        print(f"    {n:>5}  {message}")
    if errors:
        print(f"\n  {len(errors)} error object(s) — the root cause is the last column:")
        for source, message, cause in errors:
            print(f"    {source}\t{message}\t{cause}")
    return 0


# ---------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--list", type=Path, default=DEFAULT_LIST,
                    help=f"dataset list, one short name per line (default: {DEFAULT_LIST})")
    ap.add_argument("--out-dir", type=Path, default=None,
                    help="where the resolved list, preflight report and batch TSV go "
                         "(default: ~/Gemma2.0/handoffs/geo_source_metadata_<date> "
                         "where that directory exists, else ./)")
    ap.add_argument("--offset", type=int, default=0, help="skip the first N names")
    ap.add_argument("--limit", type=int, default=None,
                    help="process at most N names (start small: the fetch is rate-limited)")
    ap.add_argument("--threads", type=int, default=4,
                    help="CLI worker threads (default 4; GEO rate-limits)")
    ap.add_argument("--force", action="store_true",
                    help="refetch and REPLACE documents that already exist, and keep "
                         "troubled experiments in the run")
    ap.add_argument("--run", action="store_true",
                    help="actually invoke gemma-cli (writes to PRODUCTION)")
    ap.add_argument("--gemma-cli", default=default_cli(),
                    help="path to the CLI launcher (default: $GEMMA_CLI, else "
                         "~/Gemma2.0/bin/gemma-cli-2.0.sh where it exists, else gemma-cli)")
    ap.add_argument("--base", default=None,
                    help=f"Gemma REST base for the preflight (default: keychain "
                         f"GEMMA_BASE_URL, else {DEFAULT_BASE})")
    ap.add_argument("--anonymous", action="store_true",
                    help="skip login; private datasets will look missing")
    ap.add_argument("--summarize", type=Path, default=None,
                    help="just re-summarize an existing batch TSV and exit")
    args = ap.parse_args()

    if args.summarize:
        return summarize(args.summarize)

    if not args.list.exists():
        raise SystemExit(f"list file not found: {args.list}")

    base = (args.base or resolve_secret("GEMMA_BASE_URL", "GEMMA_BASE_URL", "gemma-base-url")
            or DEFAULT_BASE).rstrip("/")

    names = read_list(args.list)
    sliced = names[args.offset:]
    if args.limit is not None:
        sliced = sliced[:args.limit]
    if not sliced:
        raise SystemExit("the slice selected no datasets")

    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    # One directory per job under the deployment root's handoffs/, which is where
    # batch_repair_2026_08_22 and acl_backfill_2026_08_24 already live. Nothing
    # this script writes belongs in $HOME.
    job = f"geo_source_metadata_{datetime.now(timezone.utc).strftime('%Y_%m_%d')}"
    handoffs = _GEMMA_ROOT / "handoffs"
    out_dir = args.out_dir or (handoffs / job if handoffs.is_dir() else Path(job))
    out_dir.mkdir(parents=True, exist_ok=True)
    # gemma-cli-2.0.sh runs the container as `-u 999:$(id -g)` — uid 999 with
    # paul's primary group — and bind-mounts $HOME at the same path. The batch
    # TSV is opened by that process, so a dir created here with the default
    # drwxr-xr-x is unwritable to it and the run dies before any work: setgid
    # plus group-write is what ~/logs/cli2 already carries.
    try:
        out_dir.chmod(0o2775)
    except OSError:
        pass  # not our directory to re-permission; --out-dir was someone else's

    token = None
    if not args.anonymous:
        username = resolve_secret("GEMMA_USERNAME", "GEMMA_USERNAME", "gemma-username")
        password = resolve_secret("GEMMA_PASSWORD", "GEMMA_PASSWORD", "gemma-password")
        if username and password:
            token = login(base, username, password)
        else:
            print("WARNING: no GEMMA_USERNAME / GEMMA_PASSWORD in env or Keychain; "
                  "preflighting anonymously — private datasets will look missing.",
                  file=sys.stderr)

    print(f"{args.list}  ({len(names)} names, {len(sliced)} in this slice)")
    print(f"preflight against {base}" + ("" if token else "  [anonymous]"))
    found = resolve_all(base, sliced, token)
    buckets = classify(sliced, found)

    report = [
        f"list:        {args.list}",
        f"sha256:      {sha256(args.list)}",
        f"slice:       offset={args.offset} limit={args.limit} -> {len(sliced)} names",
        f"base:        {base}" + ("" if token else "  (anonymous)"),
        f"resolved:    {len(buckets['ok'])} GEO experiments to send",
        f"missing:     {len(buckets['missing'])} not found in Gemma",
        f"non-GEO:     {len(buckets['non_geo'])} skipped (no GEO record to store)",
        f"troubled:    {len(buckets['troubled'])} "
        + ("kept (--force)" if args.force else "will be DROPPED by the CLI without -force"),
        f"generated:   {stamp} by scripts/backfill_geo_source_metadata.py",
    ]
    print("\n" + "\n".join(report))
    if buckets["missing"]:
        print("\n  missing (excluded from the list file, they would abort the run):")
        for name in buckets["missing"]:
            print(f"    {name}")
    if buckets["non_geo"]:
        print("\n  non-GEO (excluded):")
        for name in buckets["non_geo"]:
            print(f"    {name}")
    if buckets["troubled"] and not args.force:
        print("\n  troubled (sent, but the CLI drops them without -force):")
        for name in buckets["troubled"]:
            print(f"    {name}")

    if not buckets["ok"]:
        raise SystemExit("nothing resolvable to process")

    list_file = out_dir / "datasets.txt"
    write_list_file(list_file, buckets["ok"], report)
    (out_dir / "preflight.txt").write_text("\n".join(report) + "\n")

    batch_tsv = out_dir / "batch.tsv"
    cmd = build_command(args.gemma_cli, list_file, batch_tsv, args.threads, args.force)
    print(f"\nlist file:  {list_file}")
    print(f"batch TSV:  {batch_tsv}")
    print("\ncommand:\n  " + " ".join(cmd))

    if not args.run:
        print("\n(dry run — pass --run to execute; gemma-cli writes to PRODUCTION)")
        return 0

    print("\n🛑 running against PRODUCTION — one GEO fetch per experiment, "
          f"{len(buckets['ok'])} to go\n")
    t0 = time.time()
    proc = subprocess.run(cmd)
    elapsed = time.time() - t0
    print(f"\ngemma-cli exited {proc.returncode} after {elapsed / 60:.1f} min")
    summarize(batch_tsv)
    return proc.returncode


if __name__ == "__main__":
    sys.exit(main())
