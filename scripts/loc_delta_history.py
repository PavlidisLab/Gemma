#!/usr/bin/env python3
"""
Bucket a branch's git history by calendar month and plot the NET lines-of-code
change per month (insertions minus deletions across all non-merge commits).

This is the cheap-cousin of loc_history.py: instead of walking the tree at
monthly samples and counting newlines per blob (slow), this script consumes
`git log --numstat --no-merges` in one pass and sums per-file +/- counts
restricted to hand-written source files. A 10-year window is sub-second.

Usage:
    python scripts/loc_delta_history.py
        # default: branch=phase2-acl-migrate, full available history, code only
    python scripts/loc_delta_history.py --years 5
    python scripts/loc_delta_history.py --since 2018-01 --until 2026-05
    python scripts/loc_delta_history.py --include-config
        # overlay a config-only series alongside the code series
    python scripts/loc_delta_history.py --config-only
        # bars are net-config instead of net-code; output filenames suffixed _config
    python scripts/loc_delta_history.py --linear
        # default is symlog (renovation month likely dwarfs the historical record)

Outputs (under --out, default /tmp/loc_delta_history or /tmp/loc_delta_history_config):
    loc_delta_history[_config].svg     Illustrator-editable
    loc_delta_history[_config].png     for slides / Slack
    loc_delta_history[_config].json    raw per-month adds/dels/net, code and config
    CAPTIONS.md                        appends a regeneration entry

Numbers come straight from `git log --numstat` — no hardcoded counts.
Reruns regenerate every artifact.
"""

from __future__ import annotations
import argparse
import json
import statistics
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import matplotlib
matplotlib.use("Agg")
import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import matplotlib.ticker

# --- Pavlab figure style (per ~/.claude/CLAUDE.md "Figures, plots, data display") ---
ACCENT   = "#2563eb"   # blue-600    — primary (net additions, code)
ACCENT_2 = "#10b981"   # emerald-500 — config series
ACCENT_3 = "#f59e0b"   # amber-500   — inflection marker
ACCENT_4 = "#ef4444"   # red-500     — net deletions
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

# --- File classification (mirrors scripts/loc_history.py classify()) ---
CODE_EXTS = {
    ".java", ".scala", ".kt", ".groovy",
    ".js", ".jsx", ".mjs", ".cjs",
    ".ts", ".tsx",
    ".py", ".R", ".r",
    ".sh", ".bash",
    ".sql",
    ".vue",
}
CONFIG_EXTS = {
    # Operational configuration only. Deliberately omits .json — most .json in
    # this tree is Swagger example payloads (restapidocs/examples/), test
    # fixtures, or package.json metadata, NOT operator-tunable config. If we
    # later need to count e.g. log4j JSON config files, add a narrower
    # CONFIG_JSON path-prefix allowlist rather than reopening the floodgates.
    ".xml", ".yml", ".yaml",
    ".properties", ".toml", ".ini", ".cfg", ".conf",
}
CONFIG_BASENAMES = {"Dockerfile", "Makefile", "Jenkinsfile", ".dockerignore", ".gitignore"}

EXCLUDED_PATH_PARTS = (
    "target/", "node_modules/", "/.git/", "build/", "dist/",
    "/vendor/", "/coverage/", ".min.js", "-lock.json",
    "package-lock.json", "yarn.lock", "/__pycache__/",
)

# Paths whose contents are DATA / FIXTURES even when the extension matches
# CONFIG_EXTS. Excluded from the "config" bucket so the counted volume
# reflects real configuration burden, not Swagger example bytes or test
# fixture .xml files. Substring match against the full path.
CONFIG_EXCLUDED_PATH_PARTS = (
    "/src/test/resources/",          # all test fixtures (GEO SOFT, ontology subsets, etc.)
    "/restapidocs/examples/",        # Swagger UI sample payloads (data, not config)
    "/handoffs/",                    # design recces / status notes (not config)
)


def classify(path: str) -> Optional[str]:
    if any(p in path for p in EXCLUDED_PATH_PARTS):
        return None
    base = path.rsplit("/", 1)[-1]
    if base in CONFIG_BASENAMES:
        if any(p in path for p in CONFIG_EXCLUDED_PATH_PARTS):
            return None
        return "config"
    if "." not in base:
        return None
    ext = "." + base.rsplit(".", 1)[-1].lower()
    if ext in CODE_EXTS:
        return "code"
    if ext in CONFIG_EXTS:
        if any(p in path for p in CONFIG_EXCLUDED_PATH_PARTS):
            return None
        return "config"
    return None


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
    additions_code: int = 0
    deletions_code: int = 0
    additions_config: int = 0
    deletions_config: int = 0

    @property
    def net_code(self) -> int:
        return self.additions_code - self.deletions_code

    @property
    def net_config(self) -> int:
        return self.additions_config - self.deletions_config


def load_numstat(branch: str, since: Optional[str], until: Optional[str]) -> Dict[str, MonthBucket]:
    """Read `git log --numstat --no-merges` and bucket per-file +/- counts by
    calendar month, separated into code vs config buckets.

    Output stream looks like:
        %cI<NL>
        <adds>\t<dels>\t<path>
        <adds>\t<dels>\t<path>
        ...
        <blank>
        %cI<NL>
        ...

    We use --pretty=format:%x01%cI as a sentinel so we can distinguish a commit
    header line from a numstat row in a single pass.
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
        # numstat row: "<adds>\t<dels>\t<path>"  ; binary files come through
        # as "-\t-\t<path>" which we skip.
        parts = line.split("\t", 2)
        if len(parts) != 3:
            continue
        adds_s, dels_s, path = parts
        if adds_s == "-" or dels_s == "-":
            continue
        # Rename rows look like "path/{old => new}" or "{old => new}/path".
        # The +/- counts still apply to the new path; for classification we
        # use the path verbatim — close enough since classification is by
        # extension/basename.
        try:
            adds = int(adds_s)
            dels = int(dels_s)
        except ValueError:
            continue
        cat = classify(path)
        if cat is None:
            continue
        b = buckets[current_month]
        if cat == "code":
            b.additions_code += adds
            b.deletions_code += dels
        else:
            b.additions_config += adds
            b.deletions_config += dels
    proc.wait()
    if proc.returncode != 0:
        raise RuntimeError(f"git log failed with exit {proc.returncode}")
    return buckets


# --- plot ---

def write_plot(
    months: List[str],
    buckets: Dict[str, MonthBucket],
    *,
    branch: str,
    renov_date: Optional[datetime],
    log_scale: bool,
    include_config: bool,
    config_only: bool,
    out_dir: Path,
    basename: str,
) -> Tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    xs = [month_dt(m) for m in months]
    net_code = [buckets.get(m, MonthBucket()).net_code for m in months]
    net_cfg  = [buckets.get(m, MonthBucket()).net_config for m in months]

    # Choose the primary series: code (default) or configuration (--config-only).
    if config_only:
        primary = net_cfg
        primary_label = "configuration"
        bar_pos_label = "Net config added"
        bar_neg_label = "Net config removed"
    else:
        primary = net_code
        primary_label = "code"
        bar_pos_label = "Net code added"
        bar_neg_label = "Net code removed"

    fig, ax = plt.subplots(figsize=(9.0, 4.8))
    ax.yaxis.grid(True, color=GRID, linewidth=0.8, zorder=0)
    ax.set_axisbelow(True)

    # Diverging bars: positive blue, negative red. On a symlog axis, both
    # sides keep their direction; bar baseline at zero.
    pos = [max(0, v) for v in primary]
    neg = [min(0, v) for v in primary]
    ax.bar(xs, pos, width=25, color=ACCENT,   edgecolor="none", zorder=3, label=bar_pos_label)
    ax.bar(xs, neg, width=25, color=ACCENT_4, edgecolor="none", zorder=3, label=bar_neg_label)

    if include_config and not config_only:
        # Overlay config as a thin line so the bars stay the headline series.
        # Config rarely diverges as wildly from zero so a line reads cleaner
        # than a second pair of bars.
        ax.plot(
            xs, net_cfg,
            color=ACCENT_2, linewidth=1.6, marker="o", markersize=2.8,
            label="Net config",
            zorder=4,
        )

    # Zero baseline
    ax.axhline(0, color=TEXT, linewidth=0.8, zorder=2)

    # Identify the renovations month bar and the all-time |net| peak (which
    # may or may not be the renovations month). Label both, unless they
    # coincide, in which case fold into a single renovations callout.
    # xs are month-starts; renov_date is a precise day. Compare by month key so
    # a mid-month renovations commit doesn't fall outside [first month start,
    # last month start].
    renov_key = renov_date.strftime(MONTH_KEY) if renov_date is not None else None
    in_window = renov_key is not None and months and months[0] <= renov_key <= months[-1]
    renov_month_idx: Optional[int] = None
    if in_window:
        for i, m in enumerate(months):
            if m == renov_key:
                renov_month_idx = i
                break

    if primary:
        peak_idx = max(range(len(primary)), key=lambda i: abs(primary[i]))
        peak_x = xs[peak_idx]
        peak_v = primary[peak_idx]
        if peak_idx == renov_month_idx:
            # Same bar: combined amber callout.
            ax.text(
                peak_x, peak_v,
                f"  Renovations push\n  {peak_v:+,} net LoC",
                color=ACCENT_3, fontsize=8.5,
                ha="right",
                va="bottom" if peak_v >= 0 else "top",
            )
        else:
            ax.text(
                peak_x, peak_v,
                f" {peak_v:+,}",
                color=TEXT, fontsize=8.5,
                ha="left",
                va="bottom" if peak_v >= 0 else "top",
            )

    # Renovations marker: always draw the vertical line + label when the
    # renovations date lands inside the plotted window, regardless of where
    # the |net| peak is.
    if in_window:
        ax.axvline(renov_date, color=ACCENT_3, linewidth=1.2, linestyle="--", alpha=0.9, zorder=2)
        # On symlog the upper edge is the renovations bar height itself; pull the
        # label down a bit so it doesn't sit on top of the bar.
        ymin, ymax = ax.get_ylim()
        if renov_month_idx is not None and primary[renov_month_idx] == max(primary, default=0):
            # Bar already labelled in amber by the peak block above; just place
            # the "Renovations begin" date below the legend on the left.
            label_y = ymax * 0.5
        else:
            label_y = ymax * 0.95
        ax.text(
            renov_date, label_y,
            f"  Renovations begin\n  {renov_date.strftime('%Y-%m-%d')}",
            color=ACCENT_3, fontsize=8.5,
            ha="left", va="top",
        )

    title_noun = "configuration LoC" if config_only else "lines-of-code"
    ax.set_title(
        f"Gemma net {title_noun} per month · branch {branch}"
        + (" · symlog scale" if log_scale else ""),
        color=TEXT, pad=10,
    )
    ax.set_ylabel(
        f"Net {primary_label} lines (added − removed)"
        + (" (symlog)" if log_scale else ""),
        color=TEXT,
    )

    if log_scale:
        # symlog handles signed magnitudes: linear inside [-linthresh, +linthresh],
        # log outside. linthresh=1000 keeps small months readable while the
        # renovation peak (~1e5+) doesn't crush everything else.
        ax.set_yscale("symlog", linthresh=1000)
        ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(
            lambda v, p: f"{int(v):+,}" if v != 0 else "0"
        ))
    else:
        ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(
            lambda v, p: f"{int(v):+,}" if v != 0 else "0"
        ))

    _format_date_axis(ax, xs)

    leg = ax.legend(loc="upper left", fontsize=8.5)
    for text in leg.get_texts():
        text.set_color(TEXT)

    _add_source_caption(fig, branch, len(months), renov_date, log_scale=log_scale,
                        include_config=include_config, config_only=config_only)
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


def _add_source_caption(fig, branch: str, n_months: int, renov_date: Optional[datetime],
                        log_scale: bool, include_config: bool, config_only: bool = False):
    bits = [
        f"Source: `git log {branch} --numstat --no-merges`",
        f"bucketed by calendar month ({n_months} buckets)",
    ]
    if config_only:
        bits.append("config = .xml/.yml/.json/.properties/.toml + Dockerfile/Makefile")
    else:
        bits.append("code = .java/.js/.ts/.py/.R/.scala/.kt/.groovy/.sh/.sql/.vue")
        if include_config:
            bits.append("config = .xml/.yml/.json/.properties + Dockerfile/Makefile (overlay)")
    bits.append("excludes target/, node_modules/, build/, dist/, *.min.js, lockfiles")
    if log_scale:
        bits.append("y-axis symlog (linthresh=1000) so the renovation month doesn't crush historical bars")
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
    *,
    renov_date: Optional[datetime],
    basename: str,
) -> Path:
    payload = {
        "branch": branch,
        "renov_date": renov_date.isoformat() if renov_date else None,
        "months": months,
        "net_loc": [buckets.get(m, MonthBucket()).net_code for m in months],
        "additions": [buckets.get(m, MonthBucket()).additions_code for m in months],
        "deletions": [buckets.get(m, MonthBucket()).deletions_code for m in months],
        "net_config": [buckets.get(m, MonthBucket()).net_config for m in months],
        "additions_config": [buckets.get(m, MonthBucket()).additions_config for m in months],
        "deletions_config": [buckets.get(m, MonthBucket()).deletions_config for m in months],
    }
    p = out_dir / f"{basename}.json"
    p.write_text(json.dumps(payload, indent=2))
    return p


def write_caption(
    out_dir: Path,
    branch: str,
    months: List[str],
    buckets: Dict[str, MonthBucket],
    renov_date: Optional[datetime],
    *,
    config_only: bool,
    basename: str,
) -> Path:
    cap = out_dir / "CAPTIONS.md"
    if config_only:
        nets = [buckets.get(m, MonthBucket()).net_config for m in months]
        kind_label = "configuration LoC"
        bucket_blurb = ("config files (.xml/.yml/.yaml/.json/.properties/.toml/"
                        ".ini/.cfg/.conf + Dockerfile/Makefile/Jenkinsfile)")
        rerun_flag = "--config-only "
    else:
        nets = [buckets.get(m, MonthBucket()).net_code for m in months]
        kind_label = "lines-of-code"
        bucket_blurb = ("hand-written source files (.java + ECMAScript "
                        "+ Python + R + shell + SQL + Vue)")
        rerun_flag = ""
    if not nets:
        median_net = 0
        peak_month = ""
        peak_val = 0
    else:
        median_net = int(statistics.median(nets))
        peak_idx = max(range(len(nets)), key=lambda i: abs(nets[i]))
        peak_month = months[peak_idx]
        peak_val = nets[peak_idx]

    entry = f"""
## {basename} ({datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')})

**Figure** — `{basename}.svg` (and `.png`).
**What it is** — Gemma net {kind_label} change per calendar month on
`{branch}`, {len(months)} buckets. Net = insertions − deletions across all
non-merge commits, restricted to {bucket_blurb}.
**How to read it** — Blue bars are months that net-added {("config" if config_only else "code")},
red bars are months that net-removed {("config" if config_only else "code")}.
The zero line is the gray baseline. The amber dashed line / annotation marks
the start of the Renovations effort. Y-axis defaults to symlog (linthresh=1000)
so the renovations push doesn't crush historical bars; pass `--linear` for a
linear axis.
**What it shows** — Peak month: **{peak_month}** at **{peak_val:+,}** net {kind_label}.
Median historical month: **{median_net:+,}** net {kind_label}. The renovations marker
separates the steady-state pre-renovation churn from the Gemma 2.0 push.

Regenerate: `python scripts/loc_delta_history.py {rerun_flag}--branch {branch}`.

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
                   help="Skip the renovations vertical marker.")
    series_group = p.add_mutually_exclusive_group()
    series_group.add_argument("--include-config", action="store_true",
                              help="Overlay a net-config series (line) alongside the code bars.")
    series_group.add_argument("--config-only", action="store_true",
                              help="Plot net configuration LoC per month as the primary "
                                   "series (instead of code). Output files suffixed _config.")
    p.add_argument("--linear", action="store_true",
                   help="Linear y-axis (default is symlog so the renovation month "
                        "doesn't crush historical bars).")
    p.add_argument("--out", default=None,
                   help="Output directory (default: /tmp/loc_delta_history, or "
                        "/tmp/loc_delta_history_config in --config-only mode).")
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
            print(f"[loc_delta_history] WARN: could not resolve --renov-sha {args.renov_sha}; "
                  f"continuing without marker", file=sys.stderr)

    print(f"[loc_delta_history] reading numstat on '{args.branch}'"
          f"{(' since ' + since) if since else ''}{(' until ' + until) if until else ''}",
          file=sys.stderr)
    buckets = load_numstat(args.branch, since, until)
    if not buckets:
        print(f"[loc_delta_history] ERROR: no commits found on '{args.branch}' in the window",
              file=sys.stderr)
        return 1

    # Cover every month in [earliest, latest] even if empty (renders as an
    # invisible zero-height bar but the x-axis stays continuous).
    earliest = min(buckets.keys())
    latest = max(buckets.keys())
    months = iter_months(earliest, latest)
    if args.config_only:
        total_net  = sum(buckets.get(m, MonthBucket()).net_config        for m in months)
        total_adds = sum(buckets.get(m, MonthBucket()).additions_config  for m in months)
        total_dels = sum(buckets.get(m, MonthBucket()).deletions_config  for m in months)
        kind = "lines of configuration"
    else:
        total_net  = sum(buckets.get(m, MonthBucket()).net_code      for m in months)
        total_adds = sum(buckets.get(m, MonthBucket()).additions_code for m in months)
        total_dels = sum(buckets.get(m, MonthBucket()).deletions_code for m in months)
        kind = "lines of code"
    print(f"[loc_delta_history] {len(months)} month buckets · "
          f"+{total_adds:,} / -{total_dels:,} / net {total_net:+,} {kind}",
          file=sys.stderr)

    # Output dir + basename vary by mode so config-only doesn't clobber code-mode artifacts.
    basename = "loc_delta_history_config" if args.config_only else "loc_delta_history"
    default_out = "/tmp/loc_delta_history_config" if args.config_only else "/tmp/loc_delta_history"
    out_dir = Path(args.out) if args.out else Path(default_out)
    log_scale = not args.linear
    svg, png = write_plot(
        months, buckets,
        branch=args.branch, renov_date=renov_date,
        log_scale=log_scale, include_config=args.include_config,
        config_only=args.config_only,
        out_dir=out_dir, basename=basename,
    )
    print(f"[loc_delta_history] wrote {svg.name} + {png.name} "
          f"({'symlog' if log_scale else 'linear'} y-scale)", file=sys.stderr)

    jpath = write_json(out_dir, args.branch, months, buckets,
                      renov_date=renov_date, basename=basename)
    cap = write_caption(out_dir, args.branch, months, buckets, renov_date,
                        config_only=args.config_only, basename=basename)
    print(f"[loc_delta_history] wrote {jpath.name}, {cap.name}", file=sys.stderr)
    print(f"[loc_delta_history] done → {out_dir}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
