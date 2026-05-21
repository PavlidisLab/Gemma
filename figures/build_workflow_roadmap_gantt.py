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
    # ----- Foundations (test infra, build hygiene, JUnit5) ----------
    GanttTask("Retire JUnit 4 base-test chain (Base*Test families)",
              *_done(D19), "done", "Foundations"),
    GanttTask("HomologeneServiceTest isolation (spring-test 6.2 trap)",
              *_done(D19), "done", "Foundations"),
    GanttTask("HibernateConfig: coerce searchIndexBase off CWD",
              *_done(D19), "done", "Foundations"),
    GanttTask("Lucene 9 EnglishAnalyzer + stem-exclusion fix",
              *_done(D19), "done", "Foundations"),
    GanttTask("CLO ontology trim + cached fixture (ROBOT)",
              *_done(D19), "done", "Foundations"),
    GanttTask("Cache 38 SOFT files + @Tag('integration')",
              *_done(D19), "done", "Foundations"),
    GanttTask("@EnabledOnOs(LINUX) on /proc/locks tests",
              *_done(D19), "done", "Foundations"),
    GanttTask("ArchUnit: @SuppressArchUnit + RelationshipPersister",
              *_done(D19), "done", "Foundations"),
    GanttTask("Repo-level CLAUDE.md (build/test/feature rules)",
              *_done(D19), "done", "Foundations"),

    # ----- ACL — JOIN -> EXISTS refactor -----------------------------
    GanttTask("Mixed-ACL fixture seed + contract baseline",
              *_done(D20), "done", "ACL refactor"),
    GanttTask("formAclRestrictionClause -> EXISTS (S2)",
              *_done(D20), "done", "ACL refactor",
              note="19x cold / 3x warm validated"),
    GanttTask("EE DAO filtering migrated to post-fetch ACL loader",
              *_done(D20), "done", "ACL refactor"),
    GanttTask("Delete dead JOIN scaffolding (6 commits)",
              *_done(D20), "done", "ACL refactor"),
    GanttTask("Wire 12 remaining ACL callsites in contract test",
              *_done(D20), "done", "ACL refactor"),

    # ----- Audit Phase C migrations + AuditAdvice retire -------------
    GanttTask("@AuditedOnError + @Repeatable + AfterThrowing",
              *_done(D20), "done", "Audit Phase C"),
    GanttTask("Bucket 2e: 5 catch-block sites -> @AuditedOnError",
              *_done(D20), "done", "Audit Phase C"),
    GanttTask("Bucket 2d: BatchInfo + Preprocessor + reprocessAffy",
              *_done(D20), "done", "Audit Phase C"),
    GanttTask("Bucket 2c-ii: BatchInfoPopulation branch-extract",
              *_done(D20), "done", "Audit Phase C"),
    GanttTask("Bucket 2g: DataUpdater.addData / ADMerge / GEO update",
              *_done(D20), "done", "Audit Phase C"),
    GanttTask("@AuditedConditional batch effect/confound (#7+#8)",
              *_done(D20), "done", "Audit Phase C"),
    GanttTask("AuditAdvice retire (-765 LoC) — Phase C terminal",
              *_done(D20), "done", "Audit Phase C"),

    # ----- Audit lastEvent denorm + getLastEvents perf ---------------
    GanttTask("audit_trail.last_event_id denorm + entity + migration",
              *_done(D20), "done", "Audit perf"),
    GanttTask("AuditTrail.addEvent helper; writers + tests migrated",
              *_done(D20), "done", "Audit perf"),
    GanttTask("getLastEvents whole-corpus -> denormalised FK",
              *_done(D20), "done", "Audit perf"),
    GanttTask("getLastEvents SQL-side MAX (8.7s -> 5.3s)",
              *_done(D20), "done", "Audit perf"),
    GanttTask("V8/V10 migration deconflict (collision resolved)",
              *_done(D20), "done", "Audit perf"),

    # ----- HB6 cascade audit + regression guards ---------------------
    GanttTask("HB6 cascade audit doc + 2026-05-20 reassessment",
              *_done(D20), "done", "HB6"),
    GanttTask("ArrayDesign DAO remove() cascade fix",
              *_done(D20), "done", "HB6"),
    GanttTask("SC vector DAO cleanup cascade fix",
              *_done(D20), "done", "HB6"),
    GanttTask("Regression guards: SCD / BAD / AnalysisResultSet",
              *_done(D20), "done", "HB6"),
    GanttTask("hitListSizes cross-session-reload guard",
              *_done(D20), "done", "HB6"),
    GanttTask("HitListSize cache KEEP decision + doc",
              *_done(D20), "done", "HB6"),
    GanttTask("ArrayDesign getMostRecentEvents 2N -> 1 batch",
              *_done(D20), "done", "HB6"),

    # ----- Perf - DEA -----------------------------------------------
    GanttTask("DEA findByGene cold-cache recce (A/B/C strategies)",
              *_done(D20), "done", "DEA"),
    GanttTask("getContrasts N+1 collapse (~1.5s cold)",
              *_done(D20), "done", "DEA"),
    GanttTask("probe-init N+1 folded with contrasts",
              *_done(D20), "done", "DEA"),
    GanttTask("@Scheduled top-50 warm-up (gated on scheduler profile)",
              *_done(D21), "done", "DEA"),
    GanttTask("DEA archive ZIP async via expressionDataFileTaskExecutor",
              *_done(D21), "done", "DEA"),
    GanttTask("/datasets/{id}/data/dea endpoint (sendfile)",
              *_done(D21), "done", "DEA"),

    # ----- Perf - Data + matrix --------------------------------------
    GanttTask("perf-probe rounds 1-4 (live gemd hot paths)",
              *_done(D20), "done", "Data + matrix"),
    GanttTask("Matrix: kill double[]->Double[] boxing + single-pass NaN",
              *_done(D20), "done", "Data + matrix"),
    GanttTask("Processed-vector blob fast-path (JOIN FETCH +CS)",
              *_done(D20), "done", "Data + matrix",
              note="kills 425 N+1 hydration"),
    GanttTask("RAW vector (EE,QT) composite index",
              *_done(D20), "done", "Data + matrix"),
    GanttTask("ArrayDesign loadAsMap + 3 callsites migrated",
              *_done(D20), "done", "Data + matrix"),
    GanttTask("BioMaterial source-chain thaw (15.8s -> 150ms)",
              *_done(D20), "done", "Data + matrix"),
    GanttTask("/design fv.measurement defensive fetch",
              *_done(D20), "done", "Data + matrix"),
    GanttTask("Data exports async-build: /data/processed + /data/raw",
              *_done(D21), "done", "Data + matrix"),
    GanttTask("/resultSets/{id} TSV disk-cache + sendfile",
              *_done(D21), "done", "Data + matrix"),

    # ----- Perf - Annotation + Search --------------------------------
    GanttTask("perf-probe: annotations + characteristic",
              *_done(D20), "done", "Annotation + Search"),
    GanttTask("perf-probe: search service (5-URI cliff)",
              *_done(D20), "done", "Annotation + Search"),
    GanttTask("findExperimentsByUris -> UNION ALL (19x at 10 URIs)",
              *_done(D21), "done", "Annotation + Search"),
    GanttTask("findByValueLike autocomplete via EE2C.VALUE",
              *_done(D21), "done", "Annotation + Search"),
    GanttTask("Server-side LRU on /annotations/search",
              *_plan(date(2026, 5, 28), date(2026, 6, 4)),
              "planned", "Annotation + Search"),
    GanttTask("Hibernate Search auto-indexing decision",
              *_plan(date(2026, 5, 28), date(2026, 6, 11)),
              "blocked", "Annotation + Search",
              note="Listeners off today; need product call"),
    GanttTask("Per-hit session.get N+1 in HibernateSearchSource",
              *_plan(date(2026, 6, 4), date(2026, 6, 11)),
              "planned", "Annotation + Search"),

    # ----- Perf - Single-cell ---------------------------------------
    GanttTask("SC filtering inventory recce",
              *_done(D20), "done", "Single-cell"),
    GanttTask("perf-probe round 4 — SC vector hot paths",
              *_done(D20), "done", "Single-cell"),
    GanttTask("SCDE link table: entity + HBM + DAO + 30-callsite migrate",
              *_done(D20), "done", "Single-cell",
              note="528 rows backfilled to prod"),
    GanttTask("SCEDV (EE,QT) composite index",
              *_done(D20), "done", "Single-cell"),
    GanttTask("SC streaming hygiene (4 foot-gun fixes)",
              *_done(D20), "done", "Single-cell"),
    GanttTask("SC DAO bugs: qt filter + :ee bind",
              *_done(D20), "done", "Single-cell"),

    # ----- Curation workflow ----------------------------------------
    GanttTask("Curation call-surface inventory (15 URLs)",
              *_done(D20), "done", "Curation workflow"),
    GanttTask("Curation feature wishlist (30 gaps)",
              *_done(D20), "done", "Curation workflow"),
    GanttTask("Heatmap rewrite recce (client-driven)",
              *_done(D20), "done", "Curation workflow"),
    GanttTask("Gene-page legacy DWR surface recce",
              *_done(D20), "done", "Curation workflow"),
    GanttTask("Curator workflow vision figure",
              *_done(D21), "done", "Curation workflow"),
    GanttTask("POST /datasets/{id}/curation-proposals",
              *_plan(date(2026, 5, 28), date(2026, 6, 18)),
              "planned", "Curation workflow",
              note="Wishlist #1"),
    GanttTask("POST /datasets/{id}/audits",
              *_plan(date(2026, 6, 4), date(2026, 6, 18)),
              "planned", "Curation workflow",
              note="Wishlist #2"),
    GanttTask("Bulk gene resolver: /genes?officialSymbol IN (...)",
              *_plan(date(2026, 5, 28), date(2026, 6, 4)),
              "planned", "Curation workflow"),
    GanttTask("Bulk URI lookup: /annotations/term?uri IN (...)",
              *_plan(date(2026, 6, 4), date(2026, 6, 11)),
              "planned", "Curation workflow"),
    GanttTask("Fat-VO: /datasets/{id}/skeleton",
              *_plan(date(2026, 6, 4), date(2026, 6, 18)),
              "planned", "Curation workflow"),
    GanttTask("Whole-design PUT (wishlist keystone)",
              *_plan(date(2026, 6, 11), date(2026, 7, 2)),
              "planned", "Curation workflow"),
    GanttTask("WhatsNew dashboard semantics decision",
              *_plan(date(2026, 5, 28), date(2026, 6, 25)),
              "blocked", "Curation workflow",
              note="Needs curator-team sign-off"),

    # ----- Pipelines + scheduler ------------------------------------
    GanttTask("Pipelines + scheduler architecture recce",
              *_done(D21), "done", "Pipelines + scheduler"),
    GanttTask("PIPELINE_RUN table + executor SPI",
              *_plan(date(2026, 6, 4), date(2026, 6, 25)),
              "planned", "Pipelines + scheduler"),
    GanttTask("Slurm/Nextflow dispatch (sc-annotation PoC)",
              *_plan(date(2026, 6, 25), date(2026, 7, 16)),
              "planned", "Pipelines + scheduler"),
    GanttTask("rnaseq Luigi -> Nextflow port",
              *_plan(date(2026, 7, 16), date(2026, 9, 10)),
              "planned", "Pipelines + scheduler",
              note="After scheduler skeleton"),
    GanttTask("Curator UI: per-EE Pipelines tab",
              *_plan(date(2026, 6, 25), date(2026, 7, 30)),
              "planned", "Pipelines + scheduler"),

    # ----- UI -------------------------------------------------------
    GanttTask("Heatmap endpoint scaffold + baseline_relevance feature",
              *_done(D20), "done", "UI"),
    GanttTask("Heatmap S3: widget against new endpoint",
              *_plan(date(2026, 6, 4), date(2026, 6, 25)),
              "planned", "UI",
              note="gemma-curation-ui"),
    GanttTask("Gene-page rework (legacy -> new shape)",
              *_plan(date(2026, 6, 25), date(2026, 8, 6)),
              "deferred", "UI"),
    GanttTask("Curator dashboard: shared live tickets",
              *_plan(date(2026, 6, 11), date(2026, 7, 16)),
              "planned", "UI"),

    # ----- Static analysis / concurrency ----------------------------
    GanttTask("Static analysis sweep (3 critical fixes)",
              *_done(D20), "done", "Static analysis",
              note="Agilent SDF race + 2 FD leaks"),
    GanttTask("Concurrency sweep + 2 volatile fixes",
              *_done(D20), "done", "Static analysis",
              note="SlackAppender + ExtendedRuntime DCL"),
    GanttTask("3 HIGH-RISK concurrency fixes",
              *_done(D20), "done", "Static analysis",
              note="BLAT busy-spin / CorrelationStats / SVD"),
    GanttTask("CorrelationStats concurrency regression guard",
              *_done(D21), "done", "Static analysis"),
    GanttTask("Wire spotbugs-maven-plugin + findsecbugs",
              *_done(D21), "done", "Static analysis"),
    GanttTask("SpotBugs first-pass report (187 priority-1)",
              *_done(D21), "done", "Static analysis"),
    GanttTask("SpotBugs Top-5 priority-1 fixes",
              *_plan(date(2026, 5, 28), date(2026, 6, 4)),
              "planned", "Static analysis"),
    GanttTask("SQL_INJECTION_HIBERNATE triage (85 hits)",
              *_plan(date(2026, 6, 4), date(2026, 6, 25)),
              "planned", "Static analysis"),
    GanttTask("OBJECT_DESERIALIZATION allow-list (5 hits)",
              *_plan(date(2026, 5, 28), date(2026, 6, 4)),
              "planned", "Static analysis"),

    # ----- Ops / schema ---------------------------------------------
    GanttTask("Drop 2 redundant FK indexes (~25-30 MB reclaim)",
              *_done(D20), "done", "Ops / schema"),
    GanttTask("Coexpression orphan recce (~146 GB drop pending)",
              *_done(D20), "done", "Ops / schema"),
    GanttTask("Coexpression tables drop (~146 GB)",
              *_plan(date(2026, 6, 4), date(2026, 6, 18)),
              "blocked", "Ops / schema",
              note="Needs ops sign-off"),
    GanttTask("Flyway prod baseline reconciliation",
              *_plan(date(2026, 6, 18), date(2026, 7, 9)),
              "planned", "Ops / schema"),

    # ----- Hotfix / catch-up ----------------------------------------
    GanttTask("hotfix-1.32.7 catch-up merge (empty-design / dataless)",
              *_done(D20), "done", "Hotfix"),
    GanttTask("PUT /datasets/{id}/design + DesignPreflightReport",
              *_done(D20), "done", "Hotfix",
              note="PR #1657"),
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
