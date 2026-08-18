#!/usr/bin/env python3
"""
Performance probe for the Gemma REST search-adjacent endpoints we've been
tuning. Runs against a real running instance — defaults to frink — and
reports min / median / p95 / max wall-clock time per query over N runs.

Designed to be re-run after every perf-touching commit lands on frink so we
can spot regressions before users do.

Endpoints exercised:
  - GET  /rest/v2/genes/search?query=&taxon=
  - GET  /rest/v2/annotations/search?query=&prefixes=&limit=
  - GET  /rest/v2/goTerms/{shortId}/genes?taxon=&propagate=&maxTerms=
  - GET  /rest/v2/goTerms/{shortId}/genes/count?taxon=&propagate=&maxTerms=
  - GET  /rest/v2/datasets?offset=&limit=&sort=
  - GET  /rest/v2/datasets/{id}/expressions/differential?diffExSet=...

Auth resolution mirrors the rest of the repo: GEMMA_USERNAME / GEMMA_PASSWORD
out of macOS Keychain, traded for a Bearer token via POST /rest/v2/login.

Usage:
    scripts/perf_search.py                          # default frink, 3 runs, stdout
    scripts/perf_search.py --runs 5 --out perf.md   # write markdown report
    scripts/perf_search.py --base http://localhost:8080 --evict
    scripts/perf_search.py --only annotations,goterms
    scripts/perf_search.py --only relations   # ANNOTATION_RELATION reads + the search-widening A/B

--evict re-runs /annotations/search/cache/evict (admin) before each query so
each timing is "warm Lucene, cold response cache" rather than memoised noise.
It can't clear deeper caches (Hibernate L2, ontology cache); for true cold
restart the container and run with --evict.
"""

from __future__ import annotations

import argparse
import json
import statistics
import subprocess
import sys
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from typing import Any, Callable, Iterable


# ---------------------------------------------------------------------------
# Auth & HTTP
# ---------------------------------------------------------------------------

def keychain(service: str) -> str:
    out = subprocess.run(
        ["security", "find-generic-password", "-s", service, "-w"],
        capture_output=True, text=True, check=False,
    )
    if out.returncode != 0:
        raise SystemExit(f"keychain entry '{service}' not found: {out.stderr.strip()}")
    return out.stdout.strip()


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


def http_get(base: str, path: str, *, token: str | None = None, timeout: float = 120.0):
    """Time a single GET. Returns (elapsed_seconds, status, decoded_body_or_text)."""
    headers = {"Accept-Encoding": "gzip"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{base}{path}", headers=headers)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            status = resp.status
    except urllib.error.HTTPError as e:
        return time.perf_counter() - t0, e.code, e.read().decode(errors="replace")
    elapsed = time.perf_counter() - t0
    # decompress if gzip'd
    if raw[:2] == b"\x1f\x8b":
        import gzip
        raw = gzip.decompress(raw)
    try:
        return elapsed, status, json.loads(raw)
    except Exception:
        return elapsed, status, raw.decode(errors="replace")[:200]


def http_post(base: str, path: str, *, token: str) -> int:
    """One-shot POST, returns status. Used for cache eviction."""
    req = urllib.request.Request(
        f"{base}{path}",
        headers={"Authorization": f"Bearer {token}"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            resp.read()
            return resp.status
    except urllib.error.HTTPError as e:
        return e.code


# ---------------------------------------------------------------------------
# Test matrix
# ---------------------------------------------------------------------------

@dataclass
class Case:
    """One performance case: a label and the URL path (relative to /rest/v2)."""
    group: str
    label: str
    path: str
    # If set, count rows in the JSON's data[] / data.something so we surface
    # "did the server actually return results" rather than just timing 0-row
    # responses as wins.
    expected_nonzero: bool = True
    # Extract a count field for the report (key in the response data).
    count_path: tuple[str, ...] = ("data",)


def gene_cases() -> list[Case]:
    return [
        Case("genes", "grin1 (no taxon)", "/rest/v2/genes/search?query=grin1"),
        Case("genes", "grin1 + mouse",   "/rest/v2/genes/search?query=grin1&taxon=mouse"),
        Case("genes", "grin1 + human",   "/rest/v2/genes/search?query=grin1&taxon=human"),
        Case("genes", "tp53 + human",    "/rest/v2/genes/search?query=tp53&taxon=human"),
        Case("genes", "bdnf + mouse",    "/rest/v2/genes/search?query=bdnf&taxon=mouse"),
        Case("genes", "single-letter A", "/rest/v2/genes/search?query=A&limit=20"),
        Case("genes", "ncbi-id 7157",    "/rest/v2/genes/search?query=7157"),
    ]


def annotation_cases() -> list[Case]:
    qs = [
        ("synaptic",            "synaptic"),
        ("apoptosis",           "apoptosis"),
        ("metabolism",          "metabolism"),
        ("cell cycle",          "cell+cycle"),
        ("mitosis",             "mitosis"),
        ("neuron",              "neuron"),
        ("ion channel",         "ion+channel"),
        ("tumor necrosis",      "tumor+necrosis"),
        ("transcription factor","transcription+factor"),
        ("hippocampus",         "hippocampus"),
    ]
    out = []
    for label, enc in qs:
        out.append(Case("annotations-GO", label,
                        f"/rest/v2/annotations/search?query={enc}&prefixes=GO_&limit=15"))
    # also one without prefix to cover the EFO/MONDO/UBERON path
    out.append(Case("annotations-any", "wild type (no prefix)",
                    "/rest/v2/annotations/search?query=wild+type&limit=15"))
    out.append(Case("annotations-any", "liver (no prefix)",
                    "/rest/v2/annotations/search?query=liver&limit=15"))
    return out


def goterm_cases() -> list[Case]:
    # Mix of narrow and broad GO subtrees; uses the GO:N short form (URL-safe
    # in path; the full URI is rejected by Tomcat's allowEncodedSlash=false).
    narrow = "GO%3A0001889"     # liver development — small subtree
    medium = "GO%3A0006915"     # apoptotic process — medium subtree
    broad  = "GO%3A0008152"     # metabolic process — very broad subtree
    return [
        Case("goterms-count", "narrow exact",
             f"/rest/v2/goTerms/{narrow}/genes/count?taxon=mouse"),
        Case("goterms-count", "narrow propagate",
             f"/rest/v2/goTerms/{narrow}/genes/count?taxon=mouse&propagate=true"),
        Case("goterms-count", "medium propagate",
             f"/rest/v2/goTerms/{medium}/genes/count?taxon=mouse&propagate=true"),
        Case("goterms-count", "broad propagate (unbounded)",
             f"/rest/v2/goTerms/{broad}/genes/count?taxon=mouse&propagate=true"),
        Case("goterms-count", "broad maxTerms=50",
             f"/rest/v2/goTerms/{broad}/genes/count?taxon=mouse&propagate=true&maxTerms=50"),
        Case("goterms-count", "broad maxTerms=200",
             f"/rest/v2/goTerms/{broad}/genes/count?taxon=mouse&propagate=true&maxTerms=200"),
        Case("goterms-genes", "narrow exact",
             f"/rest/v2/goTerms/{narrow}/genes?taxon=mouse&limit=100"),
        Case("goterms-genes", "medium propagate",
             f"/rest/v2/goTerms/{medium}/genes?taxon=mouse&propagate=true&limit=100"),
        Case("goterms-genes", "broad maxTerms=50",
             f"/rest/v2/goTerms/{broad}/genes?taxon=mouse&propagate=true&maxTerms=50&limit=100"),
    ]


def dataset_cases() -> list[Case]:
    return [
        Case("datasets-list", "anon -id 25",   "/rest/v2/datasets?offset=0&limit=25&sort=-id"),
        Case("datasets-list", "anon -id 100",  "/rest/v2/datasets?offset=0&limit=100&sort=-id"),
        Case("datasets-list", "admin -id 25",  "/rest/v2/datasets?offset=0&limit=25&sort=-id"),
    ]


def diffex_cases() -> list[Case]:
    # Single known-good fixture; same one Paul tracked during the diffex perf pass.
    return [
        Case("diffex", "ds26 set518311 lim50",
             "/rest/v2/datasets/26/expressions/differential?diffExSet=518311&threshold=1&limit=50"),
    ]


def relation_cases() -> list[Case]:
    """
    ANNOTATION_RELATION reads.

    The claim being tested is that moving the derivation into the maintenance job turned an
    interactive request into an indexed lookup. That claim is currently unmeasured, and "should be
    fast" is not a measurement -- these are the cases that would falsify it.

    Four shapes, chosen because each stresses a different part of the read:

    * asserted lookup -- the common path. One indexed seek, no specificity denominator, because a
      CURATED row has nothing to divide by. Should be the fastest thing here.
    * background value -- the denominator's worst case. A value carried by hundreds of experiments
      makes the CORPUS specificity query count over all of them; C57BL/6J is the value that broke
      the ranking in the first place and it is the right stress case for the counting.
    * dataset-seeded -- the experiment page. Exercises the `exists` against EE2C that exists
      precisely so this is one query rather than a round trip to collect the dataset's annotations.
    * widened search -- the only case that puts the relation read on an already-interactive path.
      Compare it against the same query with the flag off; the difference IS the feature's cost, and
      it is the number that decides whether the browse checkboxes can turn this on.
    """
    # Chosen because it HAS curated relations. A seed with none times an empty result and
    # reports it as a win.
    alz = "http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FMONDO_0004975"
    return [
        Case("relations", "implies (gate, asserted)",
             f"/rest/v2/annotations/relations/implies?from={alz}&basis=CURATED,ONTOLOGY&limit=100"),
        Case("relations", "by subject (disease)",
             f"/rest/v2/annotations/relations?subject={alz}&limit=50"),
        # Background strain: the specificity denominator has to count every experiment carrying the
        # value, and this value is carried by a great many of them.
        Case("relations", "background value C57BL/6J",
             "/rest/v2/annotations/relations?object=C57BL%2F6J&limit=50"),
        Case("relations", "dataset-seeded (experiment page)",
             "/rest/v2/annotations/relations?dataset=27325&limit=50"),
        # The A/B that matters. Same query twice; the delta is what widening costs.
        Case("relations", "search, widening OFF",
             "/rest/v2/search?query=Alzheimer+disease"
             "&resultTypes=ubic.gemma.model.expression.experiment.ExpressionExperiment&limit=20",
             expected_nonzero=False),
        Case("relations", "search, widening ON",
             "/rest/v2/search?query=Alzheimer+disease"
             "&resultTypes=ubic.gemma.model.expression.experiment.ExpressionExperiment&limit=20&inferRelations=true",
             expected_nonzero=False),
    ]


def all_cases(only: set[str] | None) -> list[Case]:
    matrix = {
        "genes":       gene_cases,
        "annotations": annotation_cases,
        "goterms":     goterm_cases,
        "datasets":    dataset_cases,
        "diffex":      diffex_cases,
        "relations":   relation_cases,
    }
    if only:
        unknown = only - set(matrix)
        if unknown:
            raise SystemExit(f"unknown --only group(s): {sorted(unknown)}; valid: {sorted(matrix)}")
        chosen = [k for k in matrix if k in only]
    else:
        chosen = list(matrix)
    out: list[Case] = []
    for k in chosen:
        out.extend(matrix[k]())
    return out


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

@dataclass
class CaseResult:
    case: Case
    times: list[float] = field(default_factory=list)
    status: int = 0
    rows: int = 0
    sample: Any = None
    error: str | None = None

    def stats(self) -> dict[str, float]:
        ts = self.times
        if not ts:
            return {}
        ts_sorted = sorted(ts)
        return {
            "n":   len(ts),
            "min": ts_sorted[0],
            "p50": statistics.median(ts_sorted),
            "p95": ts_sorted[int(0.95 * (len(ts_sorted) - 1))],
            "max": ts_sorted[-1],
        }


def row_count(body: Any) -> int:
    """Best-effort 'how many results did the server return' for the response."""
    if not isinstance(body, dict):
        return 0
    data = body.get("data")
    if isinstance(data, list):
        return len(data)
    if isinstance(data, dict):
        # paginated: totalElements wins, fall back to data length
        if "totalElements" in data:
            try:
                return int(data["totalElements"])
            except (TypeError, ValueError):
                return 0
        # /goTerms/.../genes/count
        if "geneCount" in data:
            try:
                return int(data["geneCount"])
            except (TypeError, ValueError):
                return 0
    return 0


def run(base: str, cases: Iterable[Case], runs: int, token: str | None,
        evict_admin_token: str | None) -> list[CaseResult]:
    results: list[CaseResult] = []
    for case in cases:
        res = CaseResult(case=case)
        for _ in range(runs):
            if evict_admin_token is not None and case.group.startswith("annotations"):
                # Evict the /annotations/search response cache so we time the real path,
                # not a memoised hit. Deeper caches (Lucene index, ontology cache) survive.
                http_post(base, "/rest/v2/annotations/search/cache/evict",
                          token=evict_admin_token)
            elapsed, status, body = http_get(base, case.path, token=token)
            res.times.append(elapsed)
            res.status = status
            if status != 200:
                res.error = body if isinstance(body, str) else json.dumps(body)[:200]
                break
            res.rows = row_count(body)
            res.sample = body
        results.append(res)
    return results


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------

def fmt_ms(seconds: float) -> str:
    ms = seconds * 1000.0
    if ms >= 1000:
        return f"{ms/1000.0:.2f}s"
    return f"{ms:.0f}ms"


def render_markdown(base: str, runs: int, results: list[CaseResult],
                    started: float, elapsed: float, evict: bool) -> str:
    lines: list[str] = []
    lines.append(f"# Gemma REST search perf — {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(started))}")
    lines.append("")
    lines.append(f"- Base: `{base}`")
    lines.append(f"- Runs per case: {runs}")
    lines.append(f"- Response-cache eviction between runs: {evict}")
    lines.append(f"- Wall time: {elapsed:.1f}s")
    lines.append("")
    # Group by Case.group
    groups: dict[str, list[CaseResult]] = {}
    for r in results:
        groups.setdefault(r.case.group, []).append(r)
    for group_name, group_results in groups.items():
        lines.append(f"## {group_name}")
        lines.append("")
        lines.append("| case | n | min | p50 | p95 | max | rows | status |")
        lines.append("|---|---:|---:|---:|---:|---:|---:|---:|")
        for r in group_results:
            s = r.stats()
            if not s:
                lines.append(f"| {r.case.label} | 0 | — | — | — | — | — | {r.status or 'err'} |")
                continue
            lines.append(
                f"| {r.case.label} "
                f"| {s['n']} "
                f"| {fmt_ms(s['min'])} "
                f"| {fmt_ms(s['p50'])} "
                f"| {fmt_ms(s['p95'])} "
                f"| {fmt_ms(s['max'])} "
                f"| {r.rows} "
                f"| {r.status} |"
            )
        lines.append("")
    # Surface any errors at the bottom so they don't get lost in the tables.
    errors = [r for r in results if r.error]
    if errors:
        lines.append("## Errors")
        lines.append("")
        for r in errors:
            lines.append(f"- `{r.case.group}` / `{r.case.label}`: HTTP {r.status} — `{r.error}`")
        lines.append("")
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", default="http://frink.msl.ubc.ca:8080",
                    help="Base URL of the running gemma-rest instance.")
    ap.add_argument("--runs", type=int, default=3,
                    help="Number of runs per case (default 3).")
    ap.add_argument("--only", default="",
                    help="Comma-separated groups to include: genes,annotations,goterms,datasets,diffex,relations.")
    ap.add_argument("--evict", action="store_true",
                    help="Evict /annotations/search response cache before each annotations probe.")
    ap.add_argument("--anonymous", action="store_true",
                    help="Skip login; run all probes unauthenticated.")
    ap.add_argument("--out", default="",
                    help="Write markdown report here (default stdout).")
    args = ap.parse_args()

    only = set(s.strip() for s in args.only.split(",") if s.strip()) or None
    cases = all_cases(only)

    token: str | None = None
    evict_admin_token: str | None = None
    if not args.anonymous:
        username = keychain("GEMMA_USERNAME")
        password = keychain("GEMMA_PASSWORD")
        token = login(args.base, username, password)
        if args.evict:
            # Eviction endpoint is GROUP_ADMIN-only; the same token works for it.
            evict_admin_token = token

    started = time.time()
    t0 = time.perf_counter()
    results = run(args.base, cases, args.runs, token, evict_admin_token)
    elapsed = time.perf_counter() - t0

    report = render_markdown(args.base, args.runs, results, started, elapsed, args.evict)
    if args.out:
        with open(args.out, "w") as f:
            f.write(report)
        print(f"wrote {len(report)} bytes to {args.out}", file=sys.stderr)
    else:
        print(report)
    return 0


if __name__ == "__main__":
    sys.exit(main())
