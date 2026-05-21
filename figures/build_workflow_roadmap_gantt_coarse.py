"""Roadmap Gantt — coarse (one row per stream).

Companion to build_workflow_roadmap_gantt_coarse.py. Same time axis +
piecewise compression; this one collapses each workstream into a
single row for at-a-glance reading.
"""
from __future__ import annotations

import os
import sys
from datetime import date, timedelta

SKILL_PATH = os.path.expanduser(
    "~/.claude/skills/architecture-figures/python"
)
if SKILL_PATH not in sys.path:
    sys.path.insert(0, SKILL_PATH)

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

from pavlab_arch.style import apply_rcparams
from pavlab_arch.palette import (
    ACCENT, ACCENT_2, ACCENT_3, ACCENT_4, ACCENT_5,
    DET, SUBTLE, GRID, TEXT, SOFT_BG, tint,
)
from pavlab_arch.primitives import GanttTask, gantt_bar, today_line


apply_rcparams()

# ----------------------------------------------------------------------
# Time axis — weeks from 2026-05-18
# ----------------------------------------------------------------------
EPOCH = date(2026, 5, 18)
TODAY = date(2026, 5, 21)


def w(d: date) -> float:
    """Convert a calendar date to "days from EPOCH"."""
    return float((d - EPOCH).days)


# "today" boundary = end of TODAY (so all of today's work falls in the
# linear half of the chart).
TODAY_X = w(TODAY) + 1.0

# ----------------------------------------------------------------------
# Task list — ordered top-to-bottom for the chart.
# ----------------------------------------------------------------------
# Status taxonomy (per GanttTask):
#   "done"     — landed; bar entirely emerald
#   "inflight" — branch active; amber overlay on the remaining part
#   "planned"  — gray planned bar, no overlay
#   "blocked"  — red hatched fill (needs product / ops / external answer)
#   "deferred" — dotted hatched fill (parked for a focused later session)

def _done(d_start: date, d_end: date | None = None) -> tuple[float, float, float]:
    """Helper: (plan_start, plan_end, done_end) for a 'done' task spanning d_start..d_end."""
    end = d_end or d_start
    return (w(d_start), w(end) + 0.95, w(end) + 0.95)


def _plan(d_start: date, d_end: date) -> tuple[float, float, float]:
    return (w(d_start), w(d_end), 0.0)


D18, D19, D20, D21 = (date(2026, 5, d) for d in (18, 19, 20, 21))

TASKS: list[GanttTask] = [
    GanttTask("Foundations: JUnit5 chain retire, HomologeneTest, "
              "Lucene/HS7, CLO trim, SOFT cache, ArchUnit",
              *_done(D19), "done", "Foundations"),

    GanttTask("ACL JOIN -> EXISTS refactor (fixture + S2 + cleanup + wire-all)",
              *_done(D20), "done", "ACL refactor",
              note="19x cold / 3x warm validated"),

    GanttTask("Audit Phase C migrations + AuditAdvice retire (-765 LoC)",
              *_done(D20), "done", "Audit Phase C"),

    GanttTask("AuditTrail.lastEvent denorm + getLastEvents O(1) + SQL-MAX",
              *_done(D20), "done", "Audit perf",
              note="216k of 236k trails backfilled"),

    GanttTask("HB6 cascade fixes + regression guards + getMostRecentEvents",
              *_done(D20), "done", "HB6"),

    GanttTask("DEA cold-cache: contrasts N+1 + probe N+1 + warmup + "
              "archive async + /data/dea endpoint",
              w(D20), w(D21) + 0.95, w(D21) + 0.95,
              "done", "DEA"),

    GanttTask("Data + matrix: probes, boxing, blob fast-path, "
              "ArrayDesign loadAsMap, BM thaw, data-exports async",
              w(D20), w(D21) + 0.95, w(D21) + 0.95,
              "done", "Data + matrix"),

    GanttTask("Annotation + Search: probes + UNION-ALL (19x) + EE2C autocomplete",
              w(D20), w(D21) + 0.95, w(D21) + 0.95,
              "done", "Annotation + Search"),

    GanttTask("Single-cell: SCDE link table + indexes + streaming hygiene + DAO bugs",
              *_done(D20), "done", "Single-cell",
              note="528 rows backfilled to prod"),

    GanttTask("Curation recces: call-surface + 30-gap wishlist + heatmap + workflow vision",
              w(D20), w(D21) + 0.95, w(D21) + 0.95,
              "done", "Curation workflow"),
    GanttTask("Curation API: proposals + audits + bulk resolvers + skeleton",
              *_plan(date(2026, 5, 28), date(2026, 6, 18)),
              "planned", "Curation workflow",
              note="Wishlist top-3"),
    GanttTask("Whole-design PUT (wishlist keystone)",
              *_plan(date(2026, 6, 11), date(2026, 7, 2)),
              "planned", "Curation workflow"),

    GanttTask("Pipelines + scheduler recce",
              *_done(D21), "done", "Pipelines + scheduler"),
    GanttTask("PIPELINE_RUN + executor SPI + Slurm/Nextflow dispatch",
              *_plan(date(2026, 6, 4), date(2026, 7, 16)),
              "planned", "Pipelines + scheduler"),
    GanttTask("rnaseq Luigi -> Nextflow port",
              *_plan(date(2026, 7, 16), date(2026, 9, 10)),
              "planned", "Pipelines + scheduler"),

    GanttTask("Heatmap endpoint scaffold + baseline_relevance",
              *_done(D20), "done", "UI"),
    GanttTask("Heatmap S3 widget + curator dashboard",
              *_plan(date(2026, 6, 4), date(2026, 7, 16)),
              "planned", "UI"),
    GanttTask("Gene-page rework",
              *_plan(date(2026, 6, 25), date(2026, 8, 6)),
              "deferred", "UI"),

    GanttTask("Static analysis: 3 critical fixes + concurrency sweep + 3 HIGH-RISK fixes",
              *_done(D20), "done", "Static analysis"),
    GanttTask("SpotBugs + findsecbugs wired + first-pass report",
              *_done(D21), "done", "Static analysis"),
    GanttTask("SpotBugs priority-1 fixes + SQL_INJECTION_HIBERNATE triage",
              *_plan(date(2026, 5, 28), date(2026, 6, 25)),
              "planned", "Static analysis"),

    GanttTask("Schema: drop 2 dupe FK indexes + RAW/SCEDV composite indexes + coex orphan recce",
              *_done(D20), "done", "Ops / schema"),
    GanttTask("Coexpression tables drop (~146 GB)",
              *_plan(date(2026, 6, 4), date(2026, 6, 18)),
              "blocked", "Ops / schema",
              note="Needs ops sign-off"),
    GanttTask("Flyway prod baseline reconciliation",
              *_plan(date(2026, 6, 18), date(2026, 7, 9)),
              "planned", "Ops / schema"),

    GanttTask("hotfix-1.32.7 catch-up + PUT /design + DesignPreflightReport",
              *_done(D20), "done", "Hotfix"),
]


# ----------------------------------------------------------------------
# Bar-width normalization: make every "done" task render at least 1 day
# wide so the eye can read it next to multi-week planned bars.
# ----------------------------------------------------------------------
_MIN_DONE_WIDTH = 1.0  # days
_normalized: list[GanttTask] = []
for _t in TASKS:
    if _t.status == "done":
        _w = _t.plan_end - _t.plan_start
        if _w < _MIN_DONE_WIDTH:
            _mid = (_t.plan_start + _t.plan_end) / 2.0
            _new_start = _mid - _MIN_DONE_WIDTH / 2.0
            _new_end = _mid + _MIN_DONE_WIDTH / 2.0
            _normalized.append(GanttTask(
                _t.label, _new_start, _new_end, _new_end,
                _t.status, _t.category, _t.note,
            ))
            continue
    _normalized.append(_t)
TASKS = _normalized

# ----------------------------------------------------------------------
# Status palette (drives the bottom legend)
# ----------------------------------------------------------------------
STATUS_COLOR = {
    "done":     ACCENT_2,
    "inflight": ACCENT_3,
    "planned":  GRID,
    "blocked":  ACCENT_4,
    "deferred": SUBTLE,
}



# ----------------------------------------------------------------------
# Piecewise x-axis transform: keep [0, today] linear, compress
# [today, real X_MAX] into [today, 2*today] so today lands at 50% width.
# ----------------------------------------------------------------------
REAL_X_MAX = float((date(2026, 7, 17) - EPOCH).days)   # 60 days
COMPRESSED_HALF = TODAY_X                              # display width for post-today
DISPLAY_X_MAX = TODAY_X + COMPRESSED_HALF              # = 2 * TODAY_X = 6
_POST_TODAY_SCALE = COMPRESSED_HALF / (REAL_X_MAX - TODAY_X)  # ~0.053

def _xt(x: float) -> float:
    """Map real-time x (days from EPOCH) to display x."""
    if x <= TODAY_X:
        return x
    return TODAY_X + (min(x, REAL_X_MAX) - TODAY_X) * _POST_TODAY_SCALE

_transformed: list[GanttTask] = []
for _t in TASKS:
    _transformed.append(GanttTask(
        _t.label,
        _xt(_t.plan_start),
        _xt(_t.plan_end),
        _xt(_t.done_end) if _t.done_end else 0.0,
        _t.status, _t.category, _t.note,
    ))
TASKS = _transformed

# Plan horizon (display space) — clipping now happens via the transform.
X_MAX = DISPLAY_X_MAX
# Clip any plan_end beyond the horizon and flag those tasks visually.
_clipped: list[GanttTask] = []
for _t in TASKS:
    if _t.plan_end > X_MAX:
        _clipped.append(GanttTask(
            _t.label + "  (continues Q3+)",
            _t.plan_start, X_MAX, _t.done_end,
            _t.status, _t.category, _t.note,
        ))
    else:
        _clipped.append(_t)
TASKS = _clipped

# ----------------------------------------------------------------------
# Group tasks by category in input order for top-to-bottom rendering
# (we reverse later so the first category sits at the top of the chart)
# ----------------------------------------------------------------------
CATEGORIES = [
    "Foundations",
    "ACL refactor",
    "Audit Phase C",
    "Audit perf",
    "HB6",
    "DEA",
    "Data + matrix",
    "Annotation + Search",
    "Single-cell",
    "Curation workflow",
    "Pipelines + scheduler",
    "UI",
    "Static analysis",
    "Ops / schema",
    "Hotfix",
]
# Sort tasks: by category index then by plan_start within category.
cat_index = {c: i for i, c in enumerate(CATEGORIES)}
sorted_tasks = sorted(
    TASKS,
    key=lambda t: (cat_index.get(t.category, 99), t.plan_start),
)
# Rendering: first task in `sorted_tasks` should be at the TOP, so
# reverse before iterating with barh-style row indexes.
display_tasks = list(reversed(sorted_tasks))
n_rows = len(display_tasks)

# Figure height scales with row count; ~0.28 in per row plus margins.
fig_h = max(7.5, 1.6 + 0.30 * n_rows)
fig, ax = plt.subplots(figsize=(13.2, fig_h))

# ----------------------------------------------------------------------
# Category background bands (alternating) + right-margin labels
# ----------------------------------------------------------------------
# Compute the y range for each category.
ordered_cats = list(reversed(CATEGORIES))   # bottom-to-top for matplotlib
y_cursor = 0
cat_bands: list[tuple[str, float, float]] = []
for cat in ordered_cats:
    rows_for_cat = [t for t in display_tasks if t.category == cat]
    if not rows_for_cat:
        continue
    y_lo = y_cursor - 0.5
    y_hi = y_cursor + len(rows_for_cat) - 0.5
    cat_bands.append((cat, y_lo, y_hi))
    y_cursor += len(rows_for_cat)

for i, (cat, y_lo, y_hi) in enumerate(cat_bands):
    if i % 2 == 0:
        ax.axhspan(y_lo, y_hi, facecolor=SOFT_BG, alpha=0.55,
                   zorder=0)

# Right-margin category labels.
for cat, y_lo, y_hi in cat_bands:
    ax.text(
        X_MAX + 0.3, (y_lo + y_hi) / 2,
        cat,
        ha="left", va="center",
        fontsize=8.5, color=TEXT, fontweight="bold",
    )

# ----------------------------------------------------------------------
# Bars
# ----------------------------------------------------------------------
for i, t in enumerate(display_tasks):
    gantt_bar(
        ax, i,
        plan_start=t.plan_start, plan_end=t.plan_end,
        done_end=t.done_end, status=t.status,
    )

# Task labels on the y-axis (left side).
ax.set_yticks(range(n_rows))
ax.set_yticklabels(
    [t.label for t in display_tasks],
    fontsize=8.5,
)

# Today line.
today_line(ax, x=TODAY_X, label="today")

# ----------------------------------------------------------------------
# X axis — weekly ticks
# ----------------------------------------------------------------------
ax.set_xlim(-0.3, X_MAX + 0.05)
# Faint divider where the time scale changes from linear to compressed.
ax.axvline(TODAY_X, color=GRID, linewidth=0.5, linestyle=(0, (1, 2)), zorder=1)
# Ticks: linear in the pre-today half (every 1 day, weekday-only labels),
# then a handful of milestone ticks in the compressed post-today half
# showing weekday + date.
pre_tick_days = list(range(0, int(TODAY_X) + 1))
post_milestones_real = [TODAY_X + 7, TODAY_X + 14, TODAY_X + 30, REAL_X_MAX]
post_tick_positions = [_xt(d) for d in post_milestones_real]
all_ticks = pre_tick_days + post_tick_positions
all_labels = (
    [(EPOCH + timedelta(days=d)).strftime("%a %b %d") for d in pre_tick_days]
    + [(EPOCH + timedelta(days=int(d))).strftime("%a %b %d") for d in post_milestones_real]
)
ax.set_xticks(all_ticks)
ax.set_xticklabels(all_labels, fontsize=7.5, color=SUBTLE)

ax.set_ylim(-0.7, n_rows - 0.3)

# Lab style: y-grid off, x-grid on, top/right spines off.
ax.yaxis.grid(False)
ax.xaxis.grid(True, color=GRID, linewidth=0.6, alpha=0.7)
ax.set_axisbelow(True)
for side in ("top", "right"):
    ax.spines[side].set_visible(False)
ax.spines["left"].set_color(GRID)
ax.spines["bottom"].set_color(GRID)
ax.tick_params(axis="both", colors=SUBTLE, length=3, width=0.6)

# ----------------------------------------------------------------------
# Title + legend
# ----------------------------------------------------------------------
ax.set_title(
    "Curator-workflow + perf-renovation roadmap — coarse",
    loc="left", fontsize=13, fontweight="bold", color=TEXT,
    pad=14,
)
fig.text(
    0.02, 0.965,
    f"Snapshot {date.today().isoformat()} · "
    "done = landed this session · in-flight = active branches · "
    "blocked = waiting on product/ops",
    ha="left", va="top",
    fontsize=8.5, color=SUBTLE, style="italic",
)


# Annotate the compressed half so readers know the scale shifts there.
ax.text(
    TODAY_X + COMPRESSED_HALF * 0.5,
    n_rows + 0.5,
    f"horizon compressed: real {int(REAL_X_MAX - TODAY_X)} days -> half-width "
    f"(scale {_POST_TODAY_SCALE:.2f}x)",
    ha="center", va="bottom",
    fontsize=7.5, color=SUBTLE, style="italic",
)
legend_specs = [
    ("done",     "Done"),
    ("inflight", "In flight"),
    ("planned",  "Planned"),
    ("blocked",  "Blocked / decision needed"),
    ("deferred", "Deferred — focused session"),
]
patches = []
for status_key, label in legend_specs:
    color = STATUS_COLOR[status_key]
    if status_key == "blocked":
        patches.append(mpatches.Patch(
            facecolor="white", edgecolor=color, linewidth=1.3,
            hatch="//", label=label,
        ))
    elif status_key == "deferred":
        patches.append(mpatches.Patch(
            facecolor="white", edgecolor=color, linewidth=1.3,
            hatch="..", label=label,
        ))
    elif status_key == "planned":
        patches.append(mpatches.Patch(
            facecolor=color, edgecolor=SUBTLE, linewidth=0.4,
            label=label,
        ))
    else:
        patches.append(mpatches.Patch(
            facecolor=color, edgecolor=color, label=label,
        ))
ax.legend(
    handles=patches,
    loc="lower left",
    bbox_to_anchor=(0.0, -0.13 / max(1, fig_h / 7.5)),
    ncol=5,
    frameon=False,
    fontsize=8.5,
)

# Source caption
fig.text(
    0.98, 0.005,
    f"build_workflow_roadmap_gantt_coarse.py · {date.today().isoformat()}",
    ha="right", va="bottom",
    fontsize=6.8, color=SUBTLE,
)

plt.subplots_adjust(left=0.27, right=0.85, top=0.93, bottom=0.07)

# ----------------------------------------------------------------------
# Save
# ----------------------------------------------------------------------
out_dir = os.path.dirname(os.path.abspath(__file__))
canonical = os.path.join(out_dir, "workflow_roadmap_gantt_coarse.svg")
stamped = os.path.join(
    out_dir,
    f"workflow_roadmap_gantt_coarse_{date.today().isoformat()}.svg",
)
for p in (canonical, stamped):
    fig.savefig(p, format="svg", bbox_inches="tight", facecolor="white")
    print(f"wrote {p}")

png_path = os.path.join(out_dir, "workflow_roadmap_gantt_coarse.png")
fig.savefig(png_path, format="png", dpi=170,
            bbox_inches="tight", facecolor="white")
print(f"wrote {png_path}")
