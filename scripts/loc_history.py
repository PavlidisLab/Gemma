#!/usr/bin/env python3
"""
Walk a branch's git history at monthly resolution and plot lines-of-code +
lines-of-configuration over time. A vertical marker is drawn at the start of
the Renovations effort (commit 53eafb23d1, 2026-05-16).

Usage:
    python scripts/loc_history.py                       # last 5y, monthly, phase2-acl-migrate
    python scripts/loc_history.py --years 10            # last 10y
    python scripts/loc_history.py --years 5 --step 2    # last 5y, every 2 months
    python scripts/loc_history.py --branch development  # different branch
    python scripts/loc_history.py --out /tmp/figures    # output dir

Outputs (under --out, default /tmp/loc_history):
    loc_history.svg          editable in Illustrator/Inkscape
    loc_history.png          for slides / Slack
    loc_history.json         raw numbers per sample, with file-bucket counts
    CAPTIONS.md              one entry per regeneration

Numbers are pulled live from `git ls-tree` + `git cat-file --batch` — no
hardcoded values. Reruns regenerate every artifact.
"""

from __future__ import annotations
import argparse
import json
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import List, Optional, Tuple

import matplotlib
matplotlib.use("Agg")
import matplotlib.dates as mdates
import matplotlib.pyplot as plt

# --- Pavlab figure style (per ~/.claude/CLAUDE.md "Figures, plots, data display") ---
ACCENT   = "#2563eb"   # blue-600    — primary (code)
ACCENT_2 = "#10b981"   # emerald-500 — secondary (config)
ACCENT_3 = "#f59e0b"   # amber-500   — warning / inflection
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
    "xtick.color":      SUBTLE,
    "ytick.color":      SUBTLE,
    "axes.labelcolor":  TEXT,
})

# --- File classification ---
# "code" = languages the team writes by hand; "config" = settings the team
# also writes by hand but in declarative form. Generated artifacts, lockfiles,
# minified bundles, and vendored deps are excluded.
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
    ".xml", ".yml", ".yaml", ".json",
    ".properties", ".toml", ".ini", ".cfg", ".conf",
}
# Files whose basename alone identifies them as config regardless of ext.
CONFIG_BASENAMES = {"Dockerfile", "Makefile", "Jenkinsfile", ".dockerignore", ".gitignore"}

EXCLUDED_PATH_PARTS = (
    "target/", "node_modules/", "/.git/", "build/", "dist/",
    "/vendor/", "/coverage/", ".min.js", "-lock.json",
    "package-lock.json", "yarn.lock", "/__pycache__/",
)


def classify(path: str) -> Optional[str]:
    if any(p in path for p in EXCLUDED_PATH_PARTS):
        return None
    base = path.rsplit("/", 1)[-1]
    if base in CONFIG_BASENAMES:
        return "config"
    # extension check
    if "." not in base:
        return None
    ext = "." + base.rsplit(".", 1)[-1].lower()
    if ext in CODE_EXTS:
        return "code"
    if ext in CONFIG_EXTS:
        return "config"
    return None


# --- git helpers ---

def git(repo: Path, *args: str) -> str:
    return subprocess.run(
        ["git", "-C", str(repo), *args],
        capture_output=True, text=True, check=True
    ).stdout


def commit_before(repo: Path, branch: str, when: datetime) -> Optional[str]:
    """Tip of branch at or before `when`."""
    out = git(
        repo, "log", branch,
        "--before", when.strftime("%Y-%m-%d"),
        "--first-parent", "-1", "--format=%H",
    ).strip()
    return out or None


def commit_date(repo: Path, sha: str) -> datetime:
    s = git(repo, "show", "-s", "--format=%cI", sha).strip()
    return datetime.fromisoformat(s)


@dataclass
class CommitMetric:
    sha: str
    date: datetime
    code_loc: int
    config_loc: int
    n_code_files: int
    n_config_files: int


def count_at(repo: Path, sha: str) -> CommitMetric:
    """List blobs, classify, then batch-count newlines per blob."""
    ls = git(repo, "ls-tree", "-r", sha)
    code_blobs: List[str] = []
    config_blobs: List[str] = []
    for line in ls.splitlines():
        try:
            meta, path = line.split("\t", 1)
        except ValueError:
            continue
        parts = meta.split()
        if len(parts) < 3 or parts[1] != "blob":
            continue
        cat = classify(path)
        if cat == "code":
            code_blobs.append(parts[2])
        elif cat == "config":
            config_blobs.append(parts[2])

    code_loc = _batch_count_lines(repo, code_blobs)
    config_loc = _batch_count_lines(repo, config_blobs)
    return CommitMetric(
        sha=sha,
        date=commit_date(repo, sha),
        code_loc=code_loc,
        config_loc=config_loc,
        n_code_files=len(code_blobs),
        n_config_files=len(config_blobs),
    )


def _batch_count_lines(repo: Path, blob_shas: List[str]) -> int:
    """Stream blobs through `git cat-file --batch` and count newlines."""
    if not blob_shas:
        return 0
    p = subprocess.Popen(
        ["git", "-C", str(repo), "cat-file", "--batch"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, bufsize=0,
    )
    p.stdin.write(("\n".join(blob_shas) + "\n").encode())
    p.stdin.close()
    out = p.stdout.read()
    p.wait()
    # Each entry: "<sha> blob <size>\n<size bytes content>\n"
    total = 0
    pos = 0
    n = len(out)
    while pos < n:
        nl = out.find(b"\n", pos)
        if nl == -1:
            break
        header = out[pos:nl]
        parts = header.split()
        if len(parts) >= 3 and parts[1] == b"blob":
            size = int(parts[2])
            content_start = nl + 1
            content_end = content_start + size
            content = out[content_start:content_end]
            total += content.count(b"\n")
            pos = content_end + 1  # skip trailing newline
        else:
            pos = nl + 1
    return total


# --- sampling ---

def month_step(d: datetime, n: int = 1) -> datetime:
    """Advance d by n calendar months."""
    y, m = d.year, d.month + n
    while m > 12:
        y += 1
        m -= 12
    while m < 1:
        y -= 1
        m += 12
    day = min(d.day, 28)
    return d.replace(year=y, month=m, day=day)


def sample_dates(years: float, step_months: int, end: Optional[datetime] = None) -> List[datetime]:
    end = end or datetime.now(timezone.utc)
    days = int(round(years * 365.25))
    start = end - timedelta(days=days)
    out: List[datetime] = []
    cur = start
    while cur <= end:
        out.append(cur)
        cur = month_step(cur, step_months)
    if out[-1] != end:
        out.append(end)
    return out


# --- plot ---

def write_plot(
    metrics: List[CommitMetric],
    *,
    branch: str,
    years: float,
    step_months: int,
    renov_date: Optional[datetime],
    out_dir: Path,
) -> Tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    xs = [m.date for m in metrics]
    code = [m.code_loc for m in metrics]
    cfg  = [m.config_loc for m in metrics]

    fig, ax = plt.subplots(figsize=(8.2, 4.6))
    ax.yaxis.grid(True, color=GRID, linewidth=0.8, zorder=0)
    ax.set_axisbelow(True)

    line_code, = ax.plot(xs, code, color=ACCENT,   linewidth=2.0, marker="o", markersize=3.5, label="Code", zorder=3)
    line_cfg,  = ax.plot(xs, cfg,  color=ACCENT_2, linewidth=2.0, marker="o", markersize=3.5, label="Configuration", zorder=3)

    if renov_date is not None and renov_date >= xs[0] and renov_date <= xs[-1]:
        ax.axvline(renov_date, color=ACCENT_3, linewidth=1.2, linestyle="--", alpha=0.9, zorder=2)
        ymax = ax.get_ylim()[1]
        ax.text(
            renov_date, ymax * 0.97,
            f"  Renovations begin\n  {renov_date.strftime('%Y-%m-%d')}",
            color=ACCENT_3, fontsize=8.5,
            ha="left", va="top",
        )

    ax.set_title(f"Gemma lines of code & configuration · branch {branch}", color=TEXT, pad=10)
    ax.set_ylabel("Lines", color=TEXT)

    # Date axis
    ax.xaxis.set_major_locator(mdates.YearLocator())
    ax.xaxis.set_major_formatter(mdates.DateFormatter("%Y"))
    ax.xaxis.set_minor_locator(mdates.MonthLocator((1, 4, 7, 10)))

    # Thousand separator on Y
    ax.yaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(lambda v, p: f"{int(v):,}"))

    # Legend bottom-right is conventional; here we put it inside top-left so it doesn't crash with the renovations callout.
    leg = ax.legend(loc="upper left", frameon=False, fontsize=9)
    for text in leg.get_texts():
        text.set_color(TEXT)

    # source caption
    fig.text(
        0.01, 0.005,
        f"Source: git log on `{branch}`, monthly resolution (step={step_months}), window={years:g}y. "
        f"Code = .java/.js/.ts/.py/.R/.scala/.kt/.groovy/.sh/.sql/.vue. "
        f"Config = .xml/.yml/.yaml/.json/.properties/.toml + Dockerfile/Makefile. "
        f"Excludes target/, node_modules/, build/, dist/, *.min.js, lockfiles.",
        color=SUBTLE, fontsize=7.5, ha="left", va="bottom",
    )

    # Strip clipPath wrappers so the figure round-trips through Illustrator cleanly
    ax.set_clip_on(False)
    for a in list(ax.patches) + list(ax.lines) + list(ax.texts) + list(ax.collections) + list(ax.images):
        a.set_clip_on(False)

    fig.tight_layout(rect=(0, 0.03, 1, 1))
    svg_path = out_dir / "loc_history.svg"
    png_path = out_dir / "loc_history.png"
    fig.savefig(svg_path)
    fig.savefig(png_path, dpi=180)
    plt.close(fig)
    return svg_path, png_path


def write_caption(out_dir: Path, branch: str, years: float, step_months: int,
                  metrics: List[CommitMetric], renov_date: Optional[datetime]) -> Path:
    """Append a CAPTIONS.md entry."""
    cap = out_dir / "CAPTIONS.md"
    first, last = metrics[0], metrics[-1]
    body = f"""## loc_history — generated {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}

What it is: Gemma `{branch}` lines-of-code and lines-of-configuration sampled
monthly across the last {years:g} years (step={step_months}mo).

How to read it: x = sample date, y = total lines at that commit. Blue =
code (.java + ECMAScript + Python + R + shell + SQL + Vue); emerald =
configuration (XML + YAML + JSON + .properties + Dockerfile/Makefile).
Vertical amber dashed line marks the start of the Renovations effort
(`53eafb23d1`, 2026-05-16).

What it shows: at the start of the window ({first.date.strftime('%Y-%m')})
the repo carried {first.code_loc:,} lines of code and {first.config_loc:,} lines of
config; at the tip ({last.date.strftime('%Y-%m')}) {last.code_loc:,} code and
{last.config_loc:,} config. Renovations-era delta: code
{last.code_loc - (next((m.code_loc for m in metrics if renov_date and m.date >= renov_date), first.code_loc)):+,};
config
{last.config_loc - (next((m.config_loc for m in metrics if renov_date and m.date >= renov_date), first.config_loc)):+,}.

Regenerate: `python scripts/loc_history.py --years {years:g} --step {step_months}`.
"""
    with cap.open("a", encoding="utf-8") as fh:
        fh.write(body)
        fh.write("\n")
    return cap


# --- main ---

def main(argv: List[str]) -> int:
    p = argparse.ArgumentParser(description="Plot Gemma LOC + LOConfig over git history.")
    p.add_argument("--repo",    default=str(Path(__file__).resolve().parents[1]),
                   help="Path to the Gemma git repo (default: repo this script lives in).")
    p.add_argument("--branch",  default="phase2-acl-migrate",
                   help="Branch to walk (default: phase2-acl-migrate).")
    p.add_argument("--years",   type=float, default=5.0,
                   help="Years of history to cover (default: 5).")
    p.add_argument("--step",    type=int, default=1,
                   help="Months between samples (default: 1).")
    p.add_argument("--out",     default="/tmp/loc_history",
                   help="Output directory (default: /tmp/loc_history).")
    p.add_argument("--renovations-sha", default="53eafb23d1",
                   help="Commit SHA marking the Renovations start; vertical marker on the plot. "
                        "Set to '' to suppress.")
    p.add_argument("--workers", type=int, default=4,
                   help="Parallel git workers (default: 4).")
    args = p.parse_args(argv)

    repo = Path(args.repo).resolve()
    out_dir = Path(args.out).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    dates = sample_dates(args.years, args.step)
    print(f"[loc_history] sampling {len(dates)} commits on '{args.branch}' "
          f"(window={args.years:g}y, step={args.step}mo)", file=sys.stderr)

    shas_seen: List[Tuple[datetime, str]] = []
    for d in dates:
        sha = commit_before(repo, args.branch, d)
        if sha and (not shas_seen or shas_seen[-1][1] != sha):
            shas_seen.append((d, sha))

    print(f"[loc_history] {len(shas_seen)} distinct commits", file=sys.stderr)

    def work(d_sha):
        d, sha = d_sha
        m = count_at(repo, sha)
        print(f"[loc_history]   {m.date.strftime('%Y-%m-%d')}  {sha[:10]}  "
              f"code={m.code_loc:>9,}  config={m.config_loc:>8,}", file=sys.stderr)
        return m

    metrics: List[CommitMetric] = []
    with ThreadPoolExecutor(max_workers=args.workers) as ex:
        for m in ex.map(work, shas_seen):
            metrics.append(m)
    metrics.sort(key=lambda x: x.date)

    # Resolve renovations sha to a real date
    renov_date: Optional[datetime] = None
    if args.renovations_sha:
        try:
            renov_date = commit_date(repo, args.renovations_sha)
        except subprocess.CalledProcessError:
            print(f"[loc_history] renovations SHA {args.renovations_sha!r} not found; no marker drawn.", file=sys.stderr)

    # JSON dump
    j_path = out_dir / "loc_history.json"
    j_path.write_text(json.dumps(
        {
            "branch": args.branch,
            "years": args.years,
            "step_months": args.step,
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "renovations_start": renov_date.isoformat() if renov_date else None,
            "samples": [
                {
                    "date": m.date.isoformat(),
                    "sha": m.sha,
                    "code_loc": m.code_loc,
                    "config_loc": m.config_loc,
                    "n_code_files": m.n_code_files,
                    "n_config_files": m.n_config_files,
                }
                for m in metrics
            ],
        },
        indent=2,
    ), encoding="utf-8")

    svg, png = write_plot(
        metrics,
        branch=args.branch,
        years=args.years,
        step_months=args.step,
        renov_date=renov_date,
        out_dir=out_dir,
    )
    cap = write_caption(out_dir, args.branch, args.years, args.step, metrics, renov_date)

    print(f"[loc_history] wrote {svg}", file=sys.stderr)
    print(f"[loc_history] wrote {png}", file=sys.stderr)
    print(f"[loc_history] wrote {j_path}", file=sys.stderr)
    print(f"[loc_history] appended {cap}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
