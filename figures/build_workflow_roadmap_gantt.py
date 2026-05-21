"""Roadmap Gantt — curator-workflow + perf-renovation horizon.

A forward-looking schedule that pulls the threads from this session's
recces and ships into a single planning view. Done = work that's
already landed (emerald), in-flight = active branches, planned = the
next obvious commit on each thread, blocked = needs product/ops
decision, deferred = bigger refactor parked for a focused session.

Categories:
  - Perf — DEA               (DEA findByGene + warmup + N+1s + cold-cache)
  - Perf — Data + matrix     (vector retrieval, matrix assembly, exports)
  - Perf — Annotation/Search (5-URI cliff, autocomplete, MassIndexer)
  - Perf — Single-cell       (SCDE link, streaming hygiene, indexes)
  - Curation workflow        (proposals/audits API, fat-VO /skeleton, batch resolvers)
  - Pipelines + scheduler    (PIPELINE_RUN, executor SPI, Nextflow integration)
  - UI                       (heatmap session 3, gene page rework)
  - Static analysis / sweeps (SpotBugs, concurrency, deserialization, SQL injection)
  - Ops / schema             (Flyway baseline, coex drop, dupe indexes)

X-axis is "calendar weeks from 2026-05-18" (the session-cluster start).
Week 0 = this session. Today line = week ~0.5 (2026-05-21 = Thursday of
week 0). Plan horizon: ~Q3 2026.
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


TODAY_X = w(TODAY)

# ----------------------------------------------------------------------
# Task list — ordered top-to-bottom for the chart.
# ----------------------------------------------------------------------
# Status taxonomy (per GanttTask):
#   "done"     — landed; bar entirely emerald
#   "inflight" — branch active; amber overlay on the remaining part
#   "planned"  — gray planned bar, no overlay
#   "blocked"  — red hatched fill (needs product / ops / external answer)
#   "deferred" — dotted hatched fill (parked for a focused later session)

TASKS: list[GanttTask] = [
    # ----- Perf — DEA -------------------------------------------------
    GanttTask("DEA findByGene cold-cache recce",
              w(date(2026, 5, 20)), w(date(2026, 5, 20)) + 0.05,
              w(date(2026, 5, 20)) + 0.05,
              "done", "DEA"),
    GanttTask("DEA getContrasts N+1 collapse",
              w(date(2026, 5, 20)) + 0.10, w(date(2026, 5, 20)) + 0.15,
              w(date(2026, 5, 20)) + 0.15,
              "done", "DEA"),
    GanttTask("DEA probe-init N+1 (fold w/ contrasts)",
              w(date(2026, 5, 21)) + 0.05, w(date(2026, 5, 21)) + 0.10,
              w(date(2026, 5, 21)) + 0.10,
              "done", "DEA"),
    GanttTask("DEA warm-up service (@Scheduled)",
              w(date(2026, 5, 21)) + 0.10, w(date(2026, 5, 21)) + 0.20,
              w(date(2026, 5, 21)) + 0.20,
              "done", "DEA"),
    GanttTask("DEA archive ZIP async write",
              w(date(2026, 5, 21)) + 0.20, w(date(2026, 5, 21)) + 0.25,
              w(date(2026, 5, 21)) + 0.25,
              "done", "DEA"),
    GanttTask("/datasets/{id}/data/dea endpoint",
              w(date(2026, 5, 21)) + 0.30, w(date(2026, 5, 28)),
              0.0, "inflight", "DEA",
              note="Agent #59 still in flight"),

    # ----- Perf — Data + matrix --------------------------------------
    GanttTask("Matrix in-JVM boxing + single-pass init",
              w(date(2026, 5, 20)) + 0.10, w(date(2026, 5, 20)) + 0.15,
              w(date(2026, 5, 20)) + 0.15,
              "done", "Data + matrix"),
    GanttTask("Processed-vector blob fast-path",
              w(date(2026, 5, 20)) + 0.15, w(date(2026, 5, 20)) + 0.20,
              w(date(2026, 5, 20)) + 0.20,
              "done", "Data + matrix"),
    GanttTask("RAW vector (EE,QT) composite index",
              w(date(2026, 5, 20)) + 0.20, w(date(2026, 5, 20)) + 0.22,
              w(date(2026, 5, 20)) + 0.22,
              "done", "Data + matrix"),
    GanttTask("ArrayDesign batch load-as-map",
              w(date(2026, 5, 20)) + 0.05, w(date(2026, 5, 20)) + 0.10,
              w(date(2026, 5, 20)) + 0.10,
              "done", "Data + matrix"),
    GanttTask("/samples + /design N+1 fixes",
              w(date(2026, 5, 20)), w(date(2026, 5, 20)) + 0.05,
              w(date(2026, 5, 20)) + 0.05,
              "done", "Data + matrix"),
    GanttTask("Data exports async-build (processed/raw/resultSets)",
              w(date(2026, 5, 21)) + 0.20, w(date(2026, 6, 4)),
              0.0, "inflight", "Data + matrix",
              note="Agent #59 in flight"),

    # ----- Perf — Annotation + Search --------------------------------
    GanttTask("Annotation hot-path probe",
              w(date(2026, 5, 21)) - 0.05, w(date(2026, 5, 21)),
              w(date(2026, 5, 21)),
              "done", "Annotation + Search"),
    GanttTask("Search hot-path probe",
              w(date(2026, 5, 21)) - 0.05, w(date(2026, 5, 21)),
              w(date(2026, 5, 21)),
              "done", "Annotation + Search"),
    GanttTask("UNION ALL: findExperimentsByUris (19x)",
              w(date(2026, 5, 21)) + 0.15, w(date(2026, 5, 21)) + 0.25,
              w(date(2026, 5, 21)) + 0.25,
              "done", "Annotation + Search"),
    GanttTask("EE2C fast-path: autocomplete LIKE",
              w(date(2026, 5, 21)) + 0.20, w(date(2026, 5, 21)) + 0.25,
              w(date(2026, 5, 21)) + 0.25,
              "done", "Annotation + Search"),
    GanttTask("Server-side LRU on /annotations/search",
              w(date(2026, 5, 28)), w(date(2026, 6, 4)),
              0.0, "planned", "Annotation + Search"),
    GanttTask("Hibernate Search auto-indexing decision",
              w(date(2026, 5, 28)), w(date(2026, 6, 11)),
              0.0, "blocked", "Annotation + Search",
              note="Listeners off today; need product call"),
    GanttTask("Per-hit session.get N+1 in HibernateSearchSource",
              w(date(2026, 6, 4)), w(date(2026, 6, 11)),
              0.0, "planned", "Annotation + Search"),

    # ----- Perf — Single-cell ---------------------------------------
    GanttTask("SCDE link table — migration + scaffold",
              w(date(2026, 5, 20)) + 0.30, w(date(2026, 5, 20)) + 0.45,
              w(date(2026, 5, 20)) + 0.45,
              "done", "Single-cell"),
    GanttTask("SCEDV (EE,QT) composite index",
              w(date(2026, 5, 20)) + 0.45, w(date(2026, 5, 20)) + 0.47,
              w(date(2026, 5, 20)) + 0.47,
              "done", "Single-cell"),
    GanttTask("SC streaming hygiene (4 foot-gun fixes)",
              w(date(2026, 5, 21)) + 0.05, w(date(2026, 5, 21)) + 0.15,
              w(date(2026, 5, 21)) + 0.15,
              "done", "Single-cell"),
    GanttTask("SC DAO pre-existing bugs (qt + :ee)",
              w(date(2026, 5, 21)) + 0.10, w(date(2026, 5, 21)) + 0.15,
              w(date(2026, 5, 21)) + 0.15,
              "done", "Single-cell"),

    # ----- Curation workflow -----------------------------------------
    GanttTask("Curation call-surface inventory",
              w(date(2026, 5, 21)) - 0.05, w(date(2026, 5, 21)),
              w(date(2026, 5, 21)),
              "done", "Curation workflow"),
    GanttTask("Curation feature wishlist (30 gaps)",
              w(date(2026, 5, 21)), w(date(2026, 5, 21)) + 0.05,
              w(date(2026, 5, 21)) + 0.05,
              "done", "Curation workflow"),
    GanttTask("POST /datasets/{id}/curation-proposals",
              w(date(2026, 5, 28)), w(date(2026, 6, 18)),
              0.0, "planned", "Curation workflow",
              note="Wishlist #1; replaces FastAPI mock"),
    GanttTask("POST /datasets/{id}/audits",
              w(date(2026, 6, 4)), w(date(2026, 6, 18)),
              0.0, "planned", "Curation workflow",
              note="Wishlist #2"),
    GanttTask("Bulk gene resolver: /genes?officialSymbol IN (...)",
              w(date(2026, 5, 28)), w(date(2026, 6, 4)),
              0.0, "planned", "Curation workflow"),
    GanttTask("Bulk URI lookup: /annotations/term?uri IN (...)",
              w(date(2026, 6, 4)), w(date(2026, 6, 11)),
              0.0, "planned", "Curation workflow"),
    GanttTask("Fat-VO: /datasets/{id}/skeleton",
              w(date(2026, 6, 4)), w(date(2026, 6, 18)),
              0.0, "planned", "Curation workflow",
              note="Collapses 4-5 round-trips per EE"),
    GanttTask("Whole-design PUT (wishlist keystone)",
              w(date(2026, 6, 11)), w(date(2026, 7, 2)),
              0.0, "planned", "Curation workflow",
              note="Read-only without it"),
    GanttTask("WhatsNew dashboard semantics decision",
              w(date(2026, 5, 28)), w(date(2026, 6, 25)),
              0.0, "blocked", "Curation workflow",
              note="Needs curator-team sign-off"),

    # ----- Pipelines + scheduler -------------------------------------
    GanttTask("Pipelines + scheduler arch recce",
              w(date(2026, 5, 21)), w(date(2026, 5, 21)) + 0.05,
              w(date(2026, 5, 21)) + 0.05,
              "done", "Pipelines + scheduler"),
    GanttTask("PIPELINE_RUN table + executor SPI",
              w(date(2026, 6, 4)), w(date(2026, 6, 25)),
              0.0, "planned", "Pipelines + scheduler"),
    GanttTask("Slurm/Nextflow dispatch (sc-annotation PoC)",
              w(date(2026, 6, 25)), w(date(2026, 7, 16)),
              0.0, "planned", "Pipelines + scheduler",
              note="Replaces Jenkins button"),
    GanttTask("rnaseq Luigi -> Nextflow port",
              w(date(2026, 7, 16)), w(date(2026, 9, 10)),
              0.0, "planned", "Pipelines + scheduler",
              note="GO after scheduler skeleton"),
    GanttTask("Curator UI: per-EE Pipelines tab",
              w(date(2026, 6, 25)), w(date(2026, 7, 30)),
              0.0, "planned", "Pipelines + scheduler"),

    # ----- UI --------------------------------------------------------
    GanttTask("Heatmap endpoint scaffold (S2)",
              w(date(2026, 5, 20)) + 0.40, w(date(2026, 5, 20)) + 0.55,
              w(date(2026, 5, 20)) + 0.55,
              "done", "UI"),
    GanttTask("Heatmap UI spec (continuous strips + grouping)",
              w(date(2026, 5, 20)) + 0.55, w(date(2026, 5, 20)) + 0.60,
              w(date(2026, 5, 20)) + 0.60,
              "done", "UI"),
    GanttTask("Heatmap S3: widget against new endpoint",
              w(date(2026, 6, 4)), w(date(2026, 6, 25)),
              0.0, "planned", "UI",
              note="gemma-ui browser app"),
    GanttTask("Gene-page rework (legacy -> new shape)",
              w(date(2026, 6, 25)), w(date(2026, 8, 6)),
              0.0, "deferred", "UI",
              note="Paul: 'heavily redo' — design pending"),
    GanttTask("Curator dashboard: shared live tickets",
              w(date(2026, 6, 11)), w(date(2026, 7, 16)),
              0.0, "planned", "UI"),

    # ----- Static analysis / sweeps ----------------------------------
    GanttTask("Concurrency sweep (post-Collections.sync*)",
              w(date(2026, 5, 21)), w(date(2026, 5, 21)) + 0.05,
              w(date(2026, 5, 21)) + 0.05,
              "done", "Static analysis"),
    GanttTask("3 HIGH-RISK concurrency fixes",
              w(date(2026, 5, 21)) + 0.10, w(date(2026, 5, 21)) + 0.15,
              w(date(2026, 5, 21)) + 0.15,
              "done", "Static analysis"),
    GanttTask("Wire spotbugs-maven-plugin + findsecbugs",
              w(date(2026, 5, 21)) + 0.15, w(date(2026, 5, 21)) + 0.25,
              w(date(2026, 5, 21)) + 0.25,
              "done", "Static analysis"),
    GanttTask("SpotBugs Top-5 priority-1 fixes",
              w(date(2026, 5, 28)), w(date(2026, 6, 4)),
              0.0, "planned", "Static analysis",
              note="Set.contains wrong type; NPE on no-factory; ..."),
    GanttTask("SQL_INJECTION_HIBERNATE triage (85 hits)",
              w(date(2026, 6, 4)), w(date(2026, 6, 25)),
              0.0, "planned", "Static analysis",
              note="Pre-Gemma-2.0 security"),
    GanttTask("OBJECT_DESERIALIZATION allow-list (5 hits)",
              w(date(2026, 5, 28)), w(date(2026, 6, 4)),
              0.0, "planned", "Static analysis"),
    GanttTask("CorrelationStats concurrency regression test",
              w(date(2026, 5, 21)) + 0.20, w(date(2026, 5, 21)) + 0.25,
              w(date(2026, 5, 21)) + 0.25,
              "done", "Static analysis"),

    # ----- Ops / schema ---------------------------------------------
    GanttTask("Coexpression tables drop (~146 GB)",
              w(date(2026, 6, 4)), w(date(2026, 6, 18)),
              0.0, "blocked", "Ops / schema",
              note="Needs ops sign-off; recce done"),
    GanttTask("Flyway prod baseline reconciliation",
              w(date(2026, 6, 18)), w(date(2026, 7, 9)),
              0.0, "planned", "Ops / schema",
              note="gemd has no flyway_schema_history today"),
    GanttTask("Dup-index drops + RAW/SCEDV indexes deployed",
              w(date(2026, 5, 20)), w(date(2026, 5, 21)) + 0.30,
              w(date(2026, 5, 21)) + 0.30,
              "done", "Ops / schema",
              note="Applied to prod"),
    GanttTask("AuditTrail.lastEvent denorm + backfill",
              w(date(2026, 5, 21)), w(date(2026, 5, 21)) + 0.20,
              w(date(2026, 5, 21)) + 0.20,
              "done", "Ops / schema",
              note="216k of 236k trails backfilled"),
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


# Plan horizon — tasks that extend beyond this are clipped + flagged.
X_MAX = w(date(2026, 7, 17))  # ~60 days
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
    "DEA",
    "Data + matrix",
    "Annotation + Search",
    "Single-cell",
    "Curation workflow",
    "Pipelines + scheduler",
    "UI",
    "Static analysis",
    "Ops / schema",
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
ax.set_xlim(-0.3, X_MAX + 0.15)
# Tick every 7 days; label as ISO date.
n_days = int(X_MAX) + 1
tick_days = list(range(0, n_days + 1, 7))
ax.set_xticks(tick_days)
ax.set_xticklabels(
    [(EPOCH + timedelta(days=k)).strftime("%b %d") for k in tick_days],
    fontsize=7.5, color=SUBTLE,
)

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
    "Curator-workflow + perf-renovation roadmap",
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
    f"build_workflow_roadmap_gantt.py · {date.today().isoformat()}",
    ha="right", va="bottom",
    fontsize=6.8, color=SUBTLE,
)

plt.subplots_adjust(left=0.27, right=0.85, top=0.93, bottom=0.07)

# ----------------------------------------------------------------------
# Save
# ----------------------------------------------------------------------
out_dir = os.path.dirname(os.path.abspath(__file__))
canonical = os.path.join(out_dir, "workflow_roadmap_gantt.svg")
stamped = os.path.join(
    out_dir,
    f"workflow_roadmap_gantt_{date.today().isoformat()}.svg",
)
for p in (canonical, stamped):
    fig.savefig(p, format="svg", bbox_inches="tight", facecolor="white")
    print(f"wrote {p}")

png_path = os.path.join(out_dir, "workflow_roadmap_gantt.png")
fig.savefig(png_path, format="png", dpi=170,
            bbox_inches="tight", facecolor="white")
print(f"wrote {png_path}")
