#!/usr/bin/env python3
"""
Plot the CUMULATIVE lines-of-code over time on a Gemma branch, derived from a
single-pass `git log --numstat --no-merges` walk.

This is the sibling of `scripts/loc_delta_history.py`: instead of plotting the
NET +/- per month as diverging bars, we plot the running sum of (additions −
deletions) over all months up to and including each month. Same code-only
classifier as the delta script — no configuration overlay, no separate
config-only mode. Paul asked for "just lines of code", one series.

Why this exists alongside `loc_history.py`:
    - `loc_history.py` checks out each monthly sample and counts lines per
      blob. Accurate but slow and finicky to keep working across months.
    - `loc_delta_history.py` is fast (one git log pass) but only shows
      per-month deltas, not the cumulative total.
    - This script gets cumulative LoC for free off the same fast numstat pass.
      It will drift slightly from the ground-truth tree count because of
      renames, history rewrites, and binary/skipped files; in practice it
      lands within a few percent of `git diff EMPTY_TREE..HEAD --shortstat`.

Usage:
    python scripts/loc_history_cumulative.py
        # default: branch=phase2-acl-migrate, full available history
    python scripts/loc_history_cumulative.py --years 5
    python scripts/loc_history_cumulative.py --since 2018-01 --until 2026-05
    python scripts/loc_history_cumulative.py --no-renov-marker

Outputs (under --out, default /tmp/loc_history_cumulative):
    loc_history_cumulative.svg     Illustrator-editable
    loc_history_cumulative.png     for slides / Slack
    loc_history_cumulative.json    raw per-month adds/dels/net + running total
    CAPTIONS.md                    appended on every rerun

Numbers come straight from `git log --numstat` — no hardcoded counts.
Reruns regenerate every artifact.
"""

from __future__ import annotations
import argparse
import json
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import matplotlib
matplotlib.use("Agg")
import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import matplotlib.ticker

# --- Pavlab figure style (per ~/.claude/CLAUDE.md "Figures, plots, data display") ---
ACCENT   = "#2563eb"   # blue-600    — primary (cumulative code)
ACCENT_3 = "#f59e0b"   # amber-500   — inflection marker
GRID     = "#e5e7eb"   # gray-200
TEXT     = "#1f2937"   # gray-800
SUBTLE   = "#6b7280"   # gray-500

matplotlib.rcParams.update({
    "font.family":      ["Helvetica", "Arial", "sans-serif"],
    "font.size":        10,
    "figure.facecolor": "white",
    "axes.facecolor":   "white",
    "svg.fonttype":     "none",
    "axes.edgecolor":   TEXT,
    "axes.labelcolor":  TEXT,
    "axes.titlecolor":  TEXT,
    "axes.titleweight": "normal",
    "axes.titlelocation": "left",
    "axes.spines.top":   False,
    "axes.spines.right": False,
    "axes.spines.left":  False,
    "axes.spines.bottom": True,
    "xtick.color":       SUBTLE,
    "ytick.color":       SUBTLE,
    "xtick.labelsize":   9,
    "ytick.labelsize":   9,
    "legend.frameon":    False,
})

REPO_ROOT = Path(__file__).resolve().parent.parent

# --- File classification (mirrors scripts/loc_delta_history.py classify()) ---
CODE_EXTS = {
    ".java", ".scala", ".kt", ".groovy",
    ".js", ".jsx", ".mjs", ".cjs",
    ".ts", ".tsx",
    ".py", ".R", ".r",
    ".sh", ".bash",
    ".sql",
    ".vue",
}

EXCLUDED_PATH_PARTS = (
    "target/", "node_modules/", "/.git/", "build/", "dist/",
    "/vendor/", "/coverage/", ".min.js", "-lock.json",
    "package-lock.json", "yarn.lock", "/__pycache__/",
)


def is_code(path: str) -> bool:
    if any(p in path for p in EXCLUDED_PATH_PARTS):
        return False
    base = path.rsplit("/", 1)[-1]
    if "." not in base:
        return False
    ext = "." + base.rsplit(".", 1)[-1].lower()
    return ext in CODE_EXTS


# --- git helpers ---

def git(*args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(REPO_ROOT), *args],
        text=True, errors="replace",
    )


def commit_date(sha: str) -> datetime:
    iso = git("log", "-1", "--format=%cI", sha).strip()
    return datetime.fromisoformat(iso)


# --- bucketing ---

MONTH_KEY = "%Y-%m"


def month_key(dt: datetime) -> str:
    return dt.strftime(MONTH_KEY)


def month_dt(key: str) -> datetime:
    return datetime.strptime(key, MONTH_KEY).replace(tzinfo=timezone.utc)


def iter_months(start_key: str, end_key: str) -> List[str]:
    sy, sm = map(int, start_key.split("-"))
    ey, em = map(int, end_key.split("-"))
    out: List[str] = []
    y, m = sy, sm
    while (y, m) <= (ey, em):
        out.append(f"{y:04d}-{m:02d}")
        m += 1
        if m > 12:
            y += 1
            m = 1
    return out


@dataclass
class MonthBucket:
    additions: int = 0
    deletions: int = 0

    @property
    def net(self) -> int:
        return self.additions - self.deletions


def load_numstat(branch: str, since: Optional[str], until: Optional[str]) -> Dict[str, MonthBucket]:
    """Read `git log --numstat --no-merges` and bucket per-file +/- counts by
    calendar month, restricted to code-classified paths.
    """
    args = [
        "log", branch,
        "--no-merges",
        "--numstat",
        "--pretty=format:\x01%cI",
    ]
    if since:
        args.append(f"--since={since}")
    if until:
        args.append(f"--until={until}")

    buckets: Dict[str, MonthBucket] = defaultdict(MonthBucket)
    current_month: Optional[str] = None

    proc = subprocess.Popen(
        ["git", "-C", str(REPO_ROOT), *args],
        stdout=subprocess.PIPE, text=True, errors="replace",
    )
    assert proc.stdout is not None
    for raw in proc.stdout:
        line = raw.rstrip("\n")
        if not line:
            continue
        if line.startswith("\x01"):
            iso = line[1:]
            try:
                dt = datetime.fromisoformat(iso)
            except ValueError:
                current_month = None
                continue
            current_month = month_key(dt)
            continue
        if current_month is None:
            continue
        parts = line.split("\t", 2)
        if len(parts) != 3:
            continue
        adds_s, dels_s, path = parts
        if adds_s == "-" or dels_s == "-":
            continue
        try:
            adds = int(adds_s)
            dels = int(dels_s)
        except ValueError:
            continue
        if not is_code(path):
            continue
        b = buckets[current_month]
        b.additions += adds
        b.deletions += dels
    proc.wait()
    if proc.returncode != 0:
        raise RuntimeError(f"git log failed with exit {proc.returncode}")
    return buckets


# --- plot ---

def write_plot(
    months: List[str],
    cumulative: List[int],
    nets: List[int],
    *,
    branch: str,
    renov_date: Optional[datetime],
    out_dir: Path,
    basename: str,
) -> Tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    xs = [month_dt(m) for m in months]

    fig, ax = plt.subplots(figsize=(9.0, 4.8))
    ax.yaxis.grid(True, color=GRID, linewidth=0.8, zorder=0)
    ax.set_axisbelow(True)

    # Filled area + line for the cumulative LoC. The fill emphasizes "this is
    # a running total"; the line carries the precise read.
    ax.fill_between(xs, 0, cumulative, color=ACCENT, alpha=0.12, zorder=2)
    ax.plot(xs, cumulative, color=ACCENT, linewidth=2.0, zorder=3,
            label="Cumulative lines of code (Σ additions − deletions)")

    # Zero baseline (rarely visible but anchors the axes when early history
    # straddles zero).
    ax.axhline(0, color=TEXT, linewidth=0.6, zorder=1, alpha=0.4)

    # Identify the largest single-month DROP. The renovation-era gemma-web
    # retirement should dominate; label it as the headline finding.
    renov_key = renov_date.strftime(MONTH_KEY) if renov_date is not None else None
    in_window = renov_key is not None and months and months[0] <= renov_key <= months[-1]

    if nets:
        drop_idx = min(range(len(nets)), key=lambda i: nets[i])  # most negative
        drop_v = nets[drop_idx]
        if drop_v < 0:
            ax.annotate(
                f"{months[drop_idx]}: {drop_v:+,} net\ncumulative drops to {cumulative[drop_idx]:,}",
                xy=(xs[drop_idx], cumulative[drop_idx]),
                xytext=(15, -30), textcoords="offset points",
                color=TEXT, fontsize=8.5,
                ha="left", va="top",
                arrowprops=dict(arrowstyle="-", color=SUBTLE, linewidth=0.8),
            )

    if in_window:
        ax.axvline(renov_date, color=ACCENT_3, linewidth=1.2, linestyle="--",
                   alpha=0.9, zorder=2)
        ymin, ymax = ax.get_ylim()
        ax.text(
            renov_date, ymax * 0.95,
            f"  Renovations begin\n  {renov_date.strftime('%Y-%m-%d')}",
            color=ACCENT_3, fontsize=8.5,
            ha="right", va="top",
        )

    ax.set_title(
        f"Gemma cumulative lines of code · branch {branch}",
        color=TEXT, pad=10,
    )
    ax.set_ylabel("Cumulative lines of code", color=TEXT)

    ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(
        lambda v, p: f"{int(v):,}" if v != 0 else "0"
    ))

    _format_date_axis(ax, xs)

    leg = ax.legend(loc="upper left", fontsize=8.5)
    for text in leg.get_texts():
        text.set_color(TEXT)

    _add_source_caption(fig, branch, len(months), renov_date)
    _disable_clip(ax)

    fig.tight_layout(rect=(0, 0.04, 1, 1))
    svg = out_dir / f"{basename}.svg"
    png = out_dir / f"{basename}.png"
    fig.savefig(svg)
    fig.savefig(png, dpi=180)
    plt.close(fig)
    return svg, png


def _format_date_axis(ax, xs):
    if not xs:
        return
    span_years = (xs[-1] - xs[0]).days / 365.25
    if span_years >= 15:
        ax.xaxis.set_major_locator(mdates.YearLocator(4))
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y"))
        ax.xaxis.set_minor_locator(mdates.YearLocator(1))
    elif span_years >= 6:
        ax.xaxis.set_major_locator(mdates.YearLocator(2))
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y"))
        ax.xaxis.set_minor_locator(mdates.YearLocator(1))
    elif span_years >= 2:
        ax.xaxis.set_major_locator(mdates.YearLocator())
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y"))
        ax.xaxis.set_minor_locator(mdates.MonthLocator((1, 4, 7, 10)))
    else:
        ax.xaxis.set_major_locator(mdates.MonthLocator((1, 4, 7, 10)))
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y-%m"))
        ax.xaxis.set_minor_locator(mdates.MonthLocator())


def _add_source_caption(fig, branch: str, n_months: int, renov_date: Optional[datetime]):
    bits = [
        f"Source: running sum of `git log {branch} --numstat --no-merges`",
        f"bucketed by calendar month ({n_months} buckets)",
        "code = .java/.js/.ts/.py/.R/.scala/.kt/.groovy/.sh/.sql/.vue",
        "excludes target/, node_modules/, build/, dist/, *.min.js, lockfiles",
    ]
    if renov_date is not None:
        bits.append(f"renovations marker at {renov_date.strftime('%Y-%m-%d')}")
    fig.text(
        0.01, 0.005,
        ". ".join(bits) + ".",
        color=SUBTLE, fontsize=7.5, ha="left", va="bottom",
    )


def _disable_clip(ax):
    ax.set_clip_on(False)
    for a in list(ax.patches) + list(ax.lines) + list(ax.texts) + list(ax.collections) + list(ax.images):
        a.set_clip_on(False)


def write_json(
    out_dir: Path,
    branch: str,
    months: List[str],
    buckets: Dict[str, MonthBucket],
    cumulative: List[int],
    *,
    renov_date: Optional[datetime],
    basename: str,
) -> Path:
    payload = {
        "branch": branch,
        "renov_date": renov_date.isoformat() if renov_date else None,
        "months": months,
        "additions": [buckets.get(m, MonthBucket()).additions for m in months],
        "deletions": [buckets.get(m, MonthBucket()).deletions for m in months],
        "net_loc":   [buckets.get(m, MonthBucket()).net       for m in months],
        "cumulative_loc": cumulative,
    }
    p = out_dir / f"{basename}.json"
    p.write_text(json.dumps(payload, indent=2))
    return p


def write_caption(
    out_dir: Path,
    branch: str,
    months: List[str],
    cumulative: List[int],
    nets: List[int],
    renov_date: Optional[datetime],
    *,
    basename: str,
) -> Path:
    cap = out_dir / "CAPTIONS.md"
    if not months:
        first_month = last_month = ""
        first_cum = last_cum = 0
        peak_month = ""
        peak_cum = 0
        drop_month = ""
        drop_net = 0
    else:
        first_month, last_month = months[0], months[-1]
        first_cum, last_cum = cumulative[0], cumulative[-1]
        peak_idx = max(range(len(cumulative)), key=lambda i: cumulative[i])
        peak_month = months[peak_idx]
        peak_cum = cumulative[peak_idx]
        drop_idx = min(range(len(nets)), key=lambda i: nets[i])
        drop_month = months[drop_idx]
        drop_net = nets[drop_idx]

    entry = f"""
## {basename} ({datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')})

**Figure** — `{basename}.svg` (and `.png`).
**What it is** — Cumulative lines of code on Gemma `{branch}`, derived from a
single-pass `git log --numstat --no-merges` walk and bucketed by calendar
month ({len(months)} buckets). For each month M, the y-value is the running
sum of (additions − deletions) over all months ≤ M, restricted to
hand-written source files (.java + ECMAScript + Python + R + shell + SQL +
Vue), excluding target/, node_modules/, build/, dist/, minified JS, lockfiles.
**How to read it** — Blue line + light fill is the cumulative LoC. The amber
dashed vertical marks the start of the Renovations effort
({renov_date.strftime('%Y-%m-%d') if renov_date else 'n/a'}). The labeled
arrow flags the largest single-month net drop — driven by the gemma-web
retirement.
**What it shows** — Window: **{first_month} → {last_month}**. Cumulative LoC
moved from **{first_cum:,}** at the start to **{last_cum:,}** at the tip,
peaking at **{peak_cum:,}** in **{peak_month}**. Biggest monthly drop:
**{drop_month}** ({drop_net:+,} net), reflecting the Gemma 2.0 retirement of
gemma-web.

Regenerate: `python scripts/loc_history_cumulative.py --branch {branch}`.

---
"""
    if cap.exists():
        cap.write_text(cap.read_text() + entry)
    else:
        cap.write_text(entry.lstrip())
    return cap


# --- sanity check ---

def empty_tree_diff_shortstat() -> Optional[Tuple[int, int]]:
    """Run `git diff EMPTY_TREE..HEAD --shortstat -- <code globs>` and parse
    the (insertions, deletions) numbers. The difference is the
    ground-truth tree count we can compare cumulative against. Returns None on
    error.
    """
    try:
        empty_tree = subprocess.check_output(
            ["git", "-C", str(REPO_ROOT), "hash-object", "-t", "tree", "--stdin"],
            input=b"", text=False,
        ).decode().strip()
    except subprocess.CalledProcessError:
        return None
    globs = ["*.java", "*.scala", "*.kt", "*.groovy",
             "*.js", "*.jsx", "*.mjs", "*.cjs",
             "*.ts", "*.tsx",
             "*.py", "*.R", "*.r",
             "*.sh", "*.bash",
             "*.sql", "*.vue"]
    try:
        out = subprocess.check_output(
            ["git", "-C", str(REPO_ROOT), "diff",
             f"{empty_tree}..HEAD", "--shortstat", "--", *globs],
            text=True, errors="replace",
        )
    except subprocess.CalledProcessError:
        return None
    # Output looks like: " 1234 files changed, 5678 insertions(+), 90 deletions(-)"
    ins = dels = 0
    for tok in out.replace(",", "").split():
        pass
    # Robust parse:
    import re
    m_ins  = re.search(r"(\d+)\s+insertion", out)
    m_dels = re.search(r"(\d+)\s+deletion", out)
    if m_ins:
        ins = int(m_ins.group(1))
    if m_dels:
        dels = int(m_dels.group(1))
    return (ins, dels)


# --- main ---

def main(argv: List[str]) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--branch", default="phase2-acl-migrate",
                   help="Branch to walk (default: phase2-acl-migrate).")
    p.add_argument("--years", type=float, default=None,
                   help="Window in years from today (default: full available history).")
    p.add_argument("--since", default=None,
                   help="Window start YYYY-MM (overrides --years). Inclusive.")
    p.add_argument("--until", default=None,
                   help="Window end YYYY-MM (default: today). Inclusive.")
    p.add_argument("--renov-sha", default="53eafb23d1",
                   help="Commit to mark as the renovations inflection (default 53eafb23d1).")
    p.add_argument("--no-renov-marker", action="store_true",
                   help="Skip the renovations vertical marker.")
    p.add_argument("--out", default=None,
                   help="Output directory (default: /tmp/loc_history_cumulative).")
    args = p.parse_args(argv)

    # Resolve window.
    if args.since:
        since = args.since + "-01"
    elif args.years:
        end = datetime.now(timezone.utc)
        start_year = end.year - int(args.years)
        start_month = end.month
        since = f"{start_year:04d}-{start_month:02d}-01"
    else:
        since = None
    until = args.until + "-28" if args.until else None

    # Resolve renovations marker date.
    renov_date: Optional[datetime] = None
    if not args.no_renov_marker:
        try:
            renov_date = commit_date(args.renov_sha)
        except subprocess.CalledProcessError:
            print(f"[loc_history_cumulative] WARN: could not resolve --renov-sha "
                  f"{args.renov_sha}; continuing without marker", file=sys.stderr)

    print(f"[loc_history_cumulative] reading numstat on '{args.branch}'"
          f"{(' since ' + since) if since else ''}{(' until ' + until) if until else ''}",
          file=sys.stderr)
    buckets = load_numstat(args.branch, since, until)
    if not buckets:
        print(f"[loc_history_cumulative] ERROR: no commits found on '{args.branch}' in the window",
              file=sys.stderr)
        return 1

    earliest = min(buckets.keys())
    latest = max(buckets.keys())
    months = iter_months(earliest, latest)
    nets = [buckets.get(m, MonthBucket()).net for m in months]

    # Running cumulative.
    cumulative: List[int] = []
    running = 0
    for n in nets:
        running += n
        cumulative.append(running)

    total_adds = sum(buckets.get(m, MonthBucket()).additions for m in months)
    total_dels = sum(buckets.get(m, MonthBucket()).deletions for m in months)
    total_net  = total_adds - total_dels
    print(f"[loc_history_cumulative] {len(months)} month buckets · "
          f"+{total_adds:,} / -{total_dels:,} / net {total_net:+,} lines of code",
          file=sys.stderr)
    print(f"[loc_history_cumulative] cumulative range: "
          f"{cumulative[0]:,} → {cumulative[-1]:,}  (peak {max(cumulative):,})",
          file=sys.stderr)

    # Headline drop for the report.
    drop_idx = min(range(len(nets)), key=lambda i: nets[i])
    print(f"[loc_history_cumulative] largest single-month drop: "
          f"{months[drop_idx]} = {nets[drop_idx]:+,} net "
          f"(cumulative {cumulative[drop_idx]:,})",
          file=sys.stderr)

    # Sanity check against empty-tree shortstat (current HEAD only). This is
    # ONLY a meaningful comparison when the window covers full history.
    if since is None and until is None:
        truth = empty_tree_diff_shortstat()
        if truth is not None:
            t_ins, t_dels = truth
            truth_net = t_ins - t_dels
            drift = cumulative[-1] - truth_net
            pct = (drift / truth_net * 100.0) if truth_net else 0.0
            print(f"[loc_history_cumulative] sanity check: tree-diff vs empty = "
                  f"+{t_ins:,} / -{t_dels:,} / net {truth_net:+,}  "
                  f"(cumulative drift {drift:+,}, {pct:+.2f}%)",
                  file=sys.stderr)

    basename = "loc_history_cumulative"
    default_out = "/tmp/loc_history_cumulative"
    out_dir = Path(args.out) if args.out else Path(default_out)
    svg, png = write_plot(
        months, cumulative, nets,
        branch=args.branch, renov_date=renov_date,
        out_dir=out_dir, basename=basename,
    )
    print(f"[loc_history_cumulative] wrote {svg.name} + {png.name}", file=sys.stderr)

    jpath = write_json(out_dir, args.branch, months, buckets, cumulative,
                       renov_date=renov_date, basename=basename)
    cap = write_caption(out_dir, args.branch, months, cumulative, nets,
                        renov_date, basename=basename)
    print(f"[loc_history_cumulative] wrote {jpath.name}, {cap.name}", file=sys.stderr)
    print(f"[loc_history_cumulative] done → {out_dir}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
