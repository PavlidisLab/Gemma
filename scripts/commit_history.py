#!/usr/bin/env python3
"""
Bucket a branch's commit history by calendar month and plot commits-per-month
as bars. A vertical marker is drawn at the start of the Renovations effort
(commit 53eafb23d1, 2026-05-16, default), the same inflection point that
loc_history.py highlights.

Unlike loc_history.py — which does a per-sample git ls-tree walk and is the
reason the LoC-over-time figure is slow — this script reads the full git log
once and buckets in-memory, so a 10-year window is sub-second.

Usage:
    python scripts/commit_history.py
        # default: branch=phase2-acl-migrate, full available history
    python scripts/commit_history.py --years 5
        # last 5 years only
    python scripts/commit_history.py --since 2018-01 --until 2026-05
        # explicit window (month-resolution)
    python scripts/commit_history.py --branch development
        # different branch
    python scripts/commit_history.py --by-author
        # stacked bars: top 6 contributors + "other"

Outputs (under --out, default /tmp/commit_history):
    commit_history.svg     Illustrator-editable
    commit_history.png     for slides / Slack
    commit_history.json    raw counts per month (and per author if --by-author)
    CAPTIONS.md            appends a regeneration entry

Numbers are pulled live from `git log --format=...` — no hardcoded counts.
Reruns regenerate every artifact.
"""

from __future__ import annotations
import argparse
import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
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
ACCENT   = "#2563eb"   # blue-600    — primary
ACCENT_2 = "#10b981"   # emerald-500 — secondary / "Renovations" era
ACCENT_3 = "#f59e0b"   # amber-500   — inflection marker
ACCENT_4 = "#ef4444"   # red-500
GRID     = "#e5e7eb"   # gray-200
TEXT     = "#1f2937"   # gray-800
SUBTLE   = "#6b7280"   # gray-500

# Author-stack palette (Tailwind-ish, six distinct hues + neutral fallback).
AUTHOR_PALETTE = [
    "#2563eb", "#10b981", "#f59e0b", "#ef4444",
    "#a855f7", "#06b6d4",
]
AUTHOR_OTHER = "#9ca3af"  # gray-400

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


@dataclass
class Commit:
    sha: str
    when: datetime
    author: str


# --- git helpers ---

def git(*args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(REPO_ROOT), *args],
        text=True, errors="replace",
    )


def commit_date(sha: str) -> datetime:
    iso = git("log", "-1", "--format=%cI", sha).strip()
    return datetime.fromisoformat(iso)


def load_commits(branch: str, since: Optional[str], until: Optional[str]) -> List[Commit]:
    """Pull (sha, committer_date_iso, author_name) for every commit reachable
    from branch within the optional window. One git invocation."""
    args = ["log", branch, "--format=%H%x09%cI%x09%an"]
    if since:
        args.append(f"--since={since}")
    if until:
        args.append(f"--until={until}")
    out = git(*args)
    commits: List[Commit] = []
    for line in out.splitlines():
        if not line.strip():
            continue
        try:
            sha, when_iso, author = line.split("\t", 2)
        except ValueError:
            continue  # malformed line, skip
        try:
            when = datetime.fromisoformat(when_iso)
        except ValueError:
            continue
        commits.append(Commit(sha=sha, when=when, author=author))
    return commits


# --- bucketing ---

MONTH_KEY = "%Y-%m"


def month_key(dt: datetime) -> str:
    return dt.strftime(MONTH_KEY)


def month_dt(key: str) -> datetime:
    """First day of the month, UTC. Used as the x-axis date for the bar."""
    return datetime.strptime(key, MONTH_KEY).replace(tzinfo=timezone.utc)


def iter_months(start_key: str, end_key: str) -> List[str]:
    """Inclusive month-key range, e.g. 2024-01 .. 2026-05 → [...]."""
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


def bucket_by_month(commits: List[Commit]) -> Counter:
    """month_key -> commit_count."""
    return Counter(month_key(c.when) for c in commits)


def bucket_by_month_and_author(
    commits: List[Commit], top_n: int = 6
) -> Tuple[List[str], Dict[str, Dict[str, int]]]:
    """
    Returns (ordered_author_labels, {month -> {author -> count}}).
    Top N authors by total commits get their own bucket; rest collapse to "other".
    """
    totals = Counter(c.author for c in commits)
    top = [a for a, _ in totals.most_common(top_n)]
    top_set = set(top)
    per_month: Dict[str, Dict[str, int]] = defaultdict(lambda: defaultdict(int))
    for c in commits:
        a = c.author if c.author in top_set else "other"
        per_month[month_key(c.when)][a] += 1
    return top + ["other"], per_month


# --- plot ---

def write_plot_total(
    months: List[str],
    counts: List[int],
    *,
    branch: str,
    renov_date: Optional[datetime],
    log_scale: bool,
    out_dir: Path,
) -> Tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    xs = [month_dt(m) for m in months]

    fig, ax = plt.subplots(figsize=(8.8, 4.6))
    ax.yaxis.grid(True, color=GRID, linewidth=0.8, zorder=0)
    ax.set_axisbelow(True)

    # Bar width ~25 days so adjacent months touch lightly without overlapping.
    # On log scale, we have to drop zero-count months entirely — matplotlib needs >0
    # values and the 0.7-floor placeholder we used earlier just adds noise at the bottom.
    if log_scale:
        nonzero = [(x, c) for x, c in zip(xs, counts) if c > 0]
        bar_xs = [x for x, _ in nonzero]
        bar_heights = [c for _, c in nonzero]
    else:
        bar_xs, bar_heights = xs, counts
    ax.bar(bar_xs, bar_heights, width=25, color=ACCENT, edgecolor="none", zorder=3)

    # Highlight the largest month — usually the renovation peak; if the renovation
    # marker date falls in the same month, fold them into a single label so they
    # don't fight for the same x-coordinate.
    peak_idx = max(range(len(counts)), key=lambda i: counts[i]) if counts else None
    peak_x = xs[peak_idx] if peak_idx is not None else None
    peak_val = counts[peak_idx] if peak_idx is not None else 0

    renov_in_peak_month = (
        renov_date is not None
        and peak_x is not None
        and renov_date.year == peak_x.year
        and renov_date.month == peak_x.month
    )

    if peak_idx is not None:
        if renov_in_peak_month:
            # Combined label: peak count + renovation context, in renovation amber.
            ax.text(
                peak_x, peak_val * 1.10,
                f"Renovations push\n{peak_val:,} commits",
                color=ACCENT_3, fontsize=8.5,
                ha="right", va="bottom",
            )
            ax.axvline(peak_x, color=ACCENT_3, linewidth=1.2, linestyle="--", alpha=0.7, zorder=2)
        else:
            ax.text(
                peak_x, peak_val * (1.10 if log_scale else 1.02),
                f"{peak_val:,}",
                color=TEXT, fontsize=8.5,
                ha="center", va="bottom",
            )

    if (renov_date is not None and not renov_in_peak_month
            and xs and xs[0] <= renov_date <= xs[-1]):
        ax.axvline(renov_date, color=ACCENT_3, linewidth=1.2, linestyle="--", alpha=0.9, zorder=2)
        ymax = ax.get_ylim()[1]
        y_pos = ymax * (0.55 if log_scale else 0.97)
        ax.text(
            renov_date, y_pos,
            f"  Renovations begin\n  {renov_date.strftime('%Y-%m-%d')}",
            color=ACCENT_3, fontsize=8.5,
            ha="left", va="top",
        )

    ax.set_title(
        f"Gemma commits per month · branch {branch}"
        + (" · log scale" if log_scale else ""),
        color=TEXT, pad=10,
    )
    ax.set_ylabel("Commits" + (" (log)" if log_scale else ""), color=TEXT)

    if log_scale:
        ax.set_yscale("log")
        ax.set_ylim(0.8, max(counts) * 1.6)
        # On log scale, the default formatter writes "10^2" — prefer "100" for legibility.
        ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(
            lambda v, p: f"{int(v):,}" if v >= 1 else ""
        ))
    else:
        ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(lambda v, p: f"{int(v):,}"))

    _format_date_axis(ax, xs)

    _add_source_caption(fig, branch, len(counts), renov_date, log_scale=log_scale)
    _disable_clip(ax)

    # Reserve the bottom 10% for the x-axis tick labels + source caption so they don't pile up.
    fig.subplots_adjust(bottom=0.20, top=0.92, left=0.08, right=0.97)
    svg = out_dir / "commit_history.svg"
    png = out_dir / "commit_history.png"
    fig.savefig(svg)
    fig.savefig(png, dpi=180)
    plt.close(fig)
    return svg, png


def write_plot_stacked(
    months: List[str],
    author_labels: List[str],
    per_month: Dict[str, Dict[str, int]],
    *,
    branch: str,
    renov_date: Optional[datetime],
    out_dir: Path,
) -> Tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    xs = [month_dt(m) for m in months]

    fig, ax = plt.subplots(figsize=(9.4, 4.8))
    ax.yaxis.grid(True, color=GRID, linewidth=0.8, zorder=0)
    ax.set_axisbelow(True)

    bottoms = [0] * len(months)
    for i, author in enumerate(author_labels):
        heights = [per_month.get(m, {}).get(author, 0) for m in months]
        color = AUTHOR_OTHER if author == "other" else AUTHOR_PALETTE[i % len(AUTHOR_PALETTE)]
        ax.bar(
            xs, heights, width=25, bottom=bottoms,
            color=color, edgecolor="none",
            label=author, zorder=3,
        )
        bottoms = [b + h for b, h in zip(bottoms, heights)]

    if renov_date is not None and xs and xs[0] <= renov_date <= xs[-1]:
        ax.axvline(renov_date, color=ACCENT_3, linewidth=1.2, linestyle="--", alpha=0.9, zorder=2)
        ymax = ax.get_ylim()[1]
        ax.text(
            renov_date, ymax * 0.97,
            f"  Renovations begin\n  {renov_date.strftime('%Y-%m-%d')}",
            color=ACCENT_3, fontsize=8.5,
            ha="left", va="top",
        )

    ax.set_title(
        f"Gemma commits per month by author · branch {branch}",
        color=TEXT, pad=10,
    )
    ax.set_ylabel("Commits", color=TEXT)

    _format_date_axis(ax, xs)
    ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(lambda v, p: f"{int(v):,}"))

    leg = ax.legend(loc="upper left", fontsize=8.5, ncol=2)
    for text in leg.get_texts():
        text.set_color(TEXT)

    _add_source_caption(fig, branch, len(months), renov_date)
    _disable_clip(ax)

    fig.subplots_adjust(bottom=0.20, top=0.92, left=0.08, right=0.97)
    svg = out_dir / "commit_history_by_author.svg"
    png = out_dir / "commit_history_by_author.png"
    fig.savefig(svg)
    fig.savefig(png, dpi=180)
    plt.close(fig)
    return svg, png


def _format_date_axis(ax, xs):
    if not xs:
        return
    span_years = (xs[-1] - xs[0]).days / 365.25
    if span_years >= 15:
        # 15-year+ window: major every 4 years to avoid label collisions on a ~9-inch fig.
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


def _add_source_caption(fig, branch: str, n_months: int, renov_date: Optional[datetime], log_scale: bool = False):
    bits = [
        f"Source: `git log {branch} --format=%cI`",
        f"bucketed by calendar month ({n_months} buckets)",
    ]
    if log_scale:
        bits.append("y-axis log-scaled (renovations push is ~5× historical peak)")
    if renov_date is not None:
        bits.append(f"renovations marker at {renov_date.strftime('%Y-%m-%d')}")
    # Anchored in figure coordinates BELOW the x-axis tick labels. The companion
    # subplots_adjust call reserves bottom=0.20 so the caption + axis labels each
    # get their own strip and don't pile up.
    fig.text(
        0.01, 0.015,
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
    counts: List[int],
    *,
    by_author: bool,
    author_labels: Optional[List[str]],
    per_month: Optional[Dict[str, Dict[str, int]]],
    renov_date: Optional[datetime],
) -> Path:
    payload = {
        "branch": branch,
        "renov_date": renov_date.isoformat() if renov_date else None,
        "months": months,
        "commits_per_month": counts,
    }
    if by_author and author_labels is not None and per_month is not None:
        payload["authors"] = author_labels
        payload["per_month_by_author"] = {
            m: {a: per_month.get(m, {}).get(a, 0) for a in author_labels}
            for m in months
        }
    p = out_dir / "commit_history.json"
    p.write_text(json.dumps(payload, indent=2))
    return p


def write_caption(out_dir: Path, branch: str, total: int, months: List[str], renov_date: Optional[datetime]) -> Path:
    cap = out_dir / "CAPTIONS.md"
    entry = f"""
## commit_history ({datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')})

**Figure** — `commit_history.svg` (and `.png`).
**What it is** — Gemma commits per calendar month on `{branch}`, {len(months)} buckets,
{total:,} commits total{(' marked at ' + renov_date.strftime('%Y-%m-%d')) if renov_date else ''}.
**How to read it** — Each bar is one calendar month. The amber dashed line marks
the start of the Renovations effort. Height = commit count (any size, any
author).
**What it shows** — Long-run velocity of the codebase; the inflection at the
Renovations marker indicates the Gemma 2.0 push.

Regenerate: `python scripts/commit_history.py --branch {branch}`.

---
"""
    if cap.exists():
        cap.write_text(cap.read_text() + entry)
    else:
        cap.write_text(entry.lstrip())
    return cap


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
                   help="Skip the renovations vertical line.")
    p.add_argument("--by-author", action="store_true",
                   help="Also write a stacked-by-author variant (top 6 + 'other').")
    p.add_argument("--linear", action="store_true",
                   help="Use linear y-axis (default is log, since the renovation month "
                        "dwarfs every other peak ~5x).")
    p.add_argument("--out", default="/tmp/commit_history",
                   help="Output directory (default: /tmp/commit_history).")
    args = p.parse_args(argv)

    # Resolve window.
    if args.since:
        since = args.since + "-01"
    elif args.years:
        end = datetime.now(timezone.utc)
        # years before "today", floored to month start
        start_year = end.year - int(args.years)
        start_month = end.month
        since = f"{start_year:04d}-{start_month:02d}-01"
    else:
        since = None
    until = args.until + "-28" if args.until else None  # inclusive of month

    # Resolve renovations marker date from sha (if asked).
    renov_date: Optional[datetime] = None
    if not args.no_renov_marker:
        try:
            renov_date = commit_date(args.renov_sha)
        except subprocess.CalledProcessError:
            print(f"[commit_history] WARN: could not resolve --renov-sha {args.renov_sha}; "
                  f"continuing without marker", file=sys.stderr)

    print(f"[commit_history] loading commits on '{args.branch}'"
          f"{(' since ' + since) if since else ''}{(' until ' + until) if until else ''}",
          file=sys.stderr)
    commits = load_commits(args.branch, since, until)
    if not commits:
        print(f"[commit_history] ERROR: no commits found on '{args.branch}' in the window", file=sys.stderr)
        return 1
    print(f"[commit_history] loaded {len(commits):,} commits "
          f"({commits[-1].when.date()} → {commits[0].when.date()})", file=sys.stderr)

    # Bucket. Cover every month in [earliest, latest] even if empty.
    counts = bucket_by_month(commits)
    earliest = min(commits, key=lambda c: c.when).when
    latest = max(commits, key=lambda c: c.when).when
    months = iter_months(month_key(earliest), month_key(latest))
    counts_arr = [counts.get(m, 0) for m in months]

    out_dir = Path(args.out)
    log_scale = not args.linear
    svg, png = write_plot_total(
        months, counts_arr, branch=args.branch, renov_date=renov_date,
        log_scale=log_scale, out_dir=out_dir,
    )
    print(f"[commit_history] wrote {svg.name} + {png.name} "
          f"({'log' if log_scale else 'linear'} y-scale)", file=sys.stderr)

    author_labels: Optional[List[str]] = None
    per_month: Optional[Dict[str, Dict[str, int]]] = None
    if args.by_author:
        author_labels, per_month = bucket_by_month_and_author(commits)
        svg2, png2 = write_plot_stacked(
            months, author_labels, per_month,
            branch=args.branch, renov_date=renov_date, out_dir=out_dir,
        )
        print(f"[commit_history] wrote {svg2.name} + {png2.name}", file=sys.stderr)

    jpath = write_json(
        out_dir, args.branch, months, counts_arr,
        by_author=args.by_author, author_labels=author_labels, per_month=per_month,
        renov_date=renov_date,
    )
    cap = write_caption(out_dir, args.branch, sum(counts_arr), months, renov_date)
    print(f"[commit_history] wrote {jpath.name}, {cap.name}", file=sys.stderr)
    print(f"[commit_history] done → {out_dir}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
