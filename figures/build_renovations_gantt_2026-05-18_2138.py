"""Phase 3 renovations Gantt with v1-vs-now status overlay.

The two prior charts (renovations_gantt.svg and
renovations_gantt_2026-05-18_2132.svg) looked nearly identical at a
glance because status was encoded the same way on both: rows that
flipped from "in flight" to "done" between the two snapshots looked
visually similar to rows that didn't move at all.

This chart fixes that. For each task row we draw two stacked
horizontal bars in the same time-axis slot:

  - TOP half  = status as of the v1 snapshot (build_renovations_gantt.py)
                rendered as a pale tint (Tailwind 200-level hues)
  - BOTTOM half = status now (this script)
                  rendered in the full Tailwind 500-level palette

Rows where the two halves are the same colour look visually quiet.
Rows where the bottom half went green while the top half is amber
(or where the top half is gray-200 because v1 didn't have the task
at all) jump out -- that IS the answer to "did anything move?"

Inputs:
  - figures/build_renovations_gantt.py            (v1 task table)
  - figures/build_renovations_gantt_2026-05-18_2132.py (v2/now task table)

Output:
  - figures/renovations_gantt_2026-05-18_2138.svg

Style: same hard-rules (Helvetica + Arial fallback, ASCII glyphs
only, svg.fonttype="none", strip clipPath wrappers, white facecolor,
y-axis grid only, title left-aligned normal weight).
"""
from __future__ import annotations

import re
from dataclasses import dataclass

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches


# ---------------------------------------------------------------- palette
# Tailwind 500-level (full saturation) -- the "now" tier.
ACCENT_DONE       = "#10b981"  # emerald-500
ACCENT_INFLIGHT   = "#f59e0b"  # amber-500
ACCENT_BLOCKED    = "#ef4444"  # red-500 (edge)
GRID              = "#e5e7eb"  # gray-200 (planned / not-on-v1-plan)
TEXT              = "#1f2937"  # gray-800
SUBTLE            = "#6b7280"  # gray-500 (deferred + tick labels)

# Tailwind 200-level (pale tints) -- the "v1 ghost" tier.
GHOST_DONE        = "#a7f3d0"  # emerald-200
GHOST_INFLIGHT    = "#fde68a"  # amber-200
GHOST_BLOCKED     = "#fecaca"  # red-200
GHOST_PLANNED     = "#f3f4f6"  # gray-100 (lighter than GRID)
GHOST_DEFERRED    = "#d1d5db"  # gray-300

# Maps a status string to (solid_face, edge, hatch) for the bottom tier
SOLID_STYLE = {
    "done":     (ACCENT_DONE,    "none",         None),
    "inflight": (ACCENT_INFLIGHT, "none",        None),
    "planned":  (GRID,            "none",        None),
    "blocked":  (GRID,            ACCENT_BLOCKED, "//"),
    "deferred": (GRID,            SUBTLE,         ".."),
    "absent":   (GRID,            "none",        None),
}

# Maps a status string to (pale_face, edge, hatch) for the top tier
GHOST_STYLE = {
    "done":     (GHOST_DONE,     "none",        None),
    "inflight": (GHOST_INFLIGHT, "none",        None),
    "planned":  (GHOST_PLANNED,  "none",        None),
    "blocked":  (GHOST_PLANNED,  GHOST_BLOCKED, "//"),
    "deferred": (GHOST_PLANNED,  GHOST_DEFERRED, ".."),
    "absent":   (GHOST_PLANNED,  "none",        None),
}


def apply_rcparams() -> None:
    plt.rcParams.update({
        "font.family": ["Helvetica", "Arial", "sans-serif"],
        "svg.fonttype": "none",
        "figure.facecolor": "white",
        "axes.facecolor": "white",
        "axes.edgecolor": "white",
        "axes.labelcolor": TEXT,
        "axes.titlesize": 14,
        "axes.titleweight": "normal",
        "axes.titlelocation": "left",
        "xtick.color": SUBTLE,
        "ytick.color": TEXT,
        "axes.spines.top": False,
        "axes.spines.right": False,
        "axes.spines.bottom": False,
        "axes.spines.left": False,
    })


# ---------------------------------------------------------------- data
TODAY_X = 2.0


@dataclass
class Row:
    category: str
    label: str
    plan_start: float
    plan_end: float
    # v1 = original chart (build_renovations_gantt.py)
    v1_status: str    # "done"/"inflight"/"planned"/"blocked"/"deferred"/"absent"
    v1_done_end: float
    # now = this snapshot (mirrors build_renovations_gantt_2026-05-18_2132.py)
    now_status: str
    now_done_end: float


# Manual reconciliation between v1 and v2 task labels.
# Where v2 split a v1 row into multiple sub-tasks, the v1 ghost
# carries on each sub-row (the v1 chart treated the work as a single
# rolled-up item, so each sub-row honestly reflects the prior view).
# Where v2 added an entirely new task that v1 didn't track, v1 is
# marked "absent" (pale gray-100 ghost, "not on the plan yet").

ROWS: list[Row] = [
    # ---- First wave ----------------------------------------------------
    Row("First wave", "Flyway / Liquibase schema versioning",
        0.0, 4.0,
        "inflight", 2.0,
        "inflight", 2.0),
    Row("First wave", "Streaming-by-default DAOs",
        0.0, 5.0,
        "deferred", 0.0,
        "deferred", 0.0),
    Row("First wave", "Test-fixture rewrite (factories)",
        0.0, 6.0,
        "inflight", 2.0,
        "inflight", 2.0),

    # ---- ACL & security ------------------------------------------------
    Row("ACL & security", "ACL listener cutover (gsec)",
        0.0, 1.0,
        "done", 1.0,
        "done", 1.0),
    Row("ACL & security", "Sid-type unification (Spring stock)",
        0.0, 1.0,
        "done", 1.0,
        "done", 1.0),
    Row("ACL & security", "ACL upper->lower data migration (prod)",
        0.0, 1.0,
        "done", 1.0,
        "done", 1.0),
    Row("ACL & security", "Drop old uppercase ACL tables",
        3.0, 4.0,
        "blocked", 0.0,
        "blocked", 0.0),
    Row("ACL & security", "gsec HQL deprecation",
        1.0, 5.0,
        "inflight", 2.0,
        "inflight", 2.0),
    # v1 had a single rolled-up "@EnableMethodSecurity migration (14
    # providers)" marked done; v2 split it into Phase A+B (done) and
    # Phase C (inflight) -- so the Phase C row carries a "done" ghost
    # because v1 considered the whole thing finished.
    Row("ACL & security", "AfterInvocation Phase A+B (Gemma-owned providers)",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("ACL & security", "AfterInvocation Phase C (AuthorizationManager)",
        2.0, 5.0,
        "done", 2.0,         # v1 thought this was finished
        "inflight", 2.0),    # v2 split it back out, still in progress
    Row("ACL & security", "AclEntryVoter recce + Phase X.1 wrappers",
        2.0, 3.0,
        "absent", 0.0,
        "done", 2.0),
    Row("ACL & security", "AclEntryVoter Phase X.2-X.4 (interceptor + sweep)",
        3.0, 6.0,
        "absent", 0.0,
        "planned", 0.0),
    Row("ACL & security", "Spring Security 7 readiness recce",
        2.0, 3.0,
        "absent", 0.0,
        "done", 2.0),

    # ---- Maintainability -----------------------------------------------
    Row("Maintainability", "XML -> @Configuration (6 modules)",
        0.0, 3.0,
        "done", 2.0,
        "done", 2.0),
    # v1 had "Decompose ExpressionExperimentServiceImpl" as inflight
    # (recce only). v2 split that into Phase 1 (done) + Phase 1.5/2
    # (inflight). Both sub-rows ghost from v1's inflight state.
    Row("Maintainability", "EE service Phase 1 (ReadService extract)",
        2.0, 3.0,
        "inflight", 2.0,
        "done", 2.0),
    Row("Maintainability", "EE service Phase 1.5 + 2 (continued decomp)",
        2.0, 6.0,
        "inflight", 2.0,
        "inflight", 2.0),
    Row("Maintainability", "persisterHelper retirement",
        2.0, 8.0,
        "inflight", 2.0,
        "inflight", 2.0),
    Row("Maintainability", "ExpressionPersister actual deletion",
        3.0, 5.0,
        "absent", 0.0,
        "planned", 0.0),
    Row("Maintainability", "GenomePersister chunks 5.4 + 5.5",
        2.0, 4.0,
        "absent", 0.0,
        "inflight", 1.7),
    Row("Maintainability", "Impl-autowire enforcer rule",
        2.0, 3.0,
        "absent", 0.0,
        "inflight", 1.7),
    Row("Maintainability", "gemma-curation-ui contract check",
        2.0, 3.0,
        "absent", 0.0,
        "inflight", 1.7),
    Row("Maintainability", "Externalize ACL (OPA / Cedar)",
        6.0, 10.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("Maintainability", "Deprecate ensureInSession / findOrCreate",
        4.0, 7.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("Maintainability", "@Ignore'd test audit",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Maintainability", "session.refresh edge cases",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),

    # ---- Framework bumps -----------------------------------------------
    Row("Framework bumps", "Spring Framework 6.1 -> 6.2",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Framework bumps", "Spring Security 6.3 -> 6.5",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Framework bumps", "Hibernate 6.4 -> 6.6",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Framework bumps", "Spring Boot 3 BOM-only adoption",
        1.0, 2.0,
        "done", 2.0,         # v1: "Spring Boot 3 feasibility recce" done
        "done", 2.0),
    Row("Framework bumps", "gsec version alignment recce",
        2.0, 3.0,
        "absent", 0.0,
        "done", 2.0),
    Row("Framework bumps", "Java 21 readiness recce + Phase 1",
        1.0, 6.0,
        "inflight", 2.0,
        "inflight", 2.0),
    Row("Framework bumps", "Maven plugin modernization + build hygiene",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Framework bumps", "Maven release plugin recce",
        2.0, 3.0,
        "absent", 0.0,
        "done", 2.0),
    Row("Framework bumps", "JUnit 5 Phase A + B0 (pilot)",
        2.0, 3.0,
        "inflight", 2.0,     # v1 had "JUnit 5 migration" inflight
        "done", 2.0),
    Row("Framework bumps", "JUnit 5 Phase B1+ (inheritance chain)",
        3.0, 7.0,
        "inflight", 2.0,     # rolled into v1's "JUnit 5 migration" inflight
        "planned", 0.0),

    # ---- Cleanups ------------------------------------------------------
    Row("Cleanups", "Coexpression stub removal",
        0.0, 1.0,
        "done", 1.0,
        "done", 1.0),
    Row("Cleanups", "ThreadLocal removal (encoder + provider)",
        0.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Cleanups", "slf4j-api 1.7 -> 2.0 + @CommonsLog -> @Slf4j (188)",
        1.0, 2.0,
        "done", 2.0,         # v1: "Logging: @CommonsLog -> @Slf4j (188 sites)"
        "done", 2.0),
    # v1 had a single row "HikariCP modernize / Hibernate envers audit"
    # marked done; v2 split it.
    Row("Cleanups", "HikariCP audit (5.1.0 current)",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Cleanups", "Hibernate envers audit (not used)",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Cleanups", "RestTemplate -> RestClient",
        1.0, 4.0,
        "inflight", 2.0,
        "inflight", 2.0),
    Row("Cleanups", "Lombok cleanup (records, @SneakyThrows, BlatResult)",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Cleanups", "@Cacheable audit (not annotated; programmatic)",
        1.0, 2.0,
        "done", 2.0,         # v1: "Cache modernization (JCache, @Cacheable)"
        "done", 2.0),
    Row("Cleanups", "Validation audit (not wired)",
        1.0, 2.0,
        "absent", 0.0,
        "done", 2.0),
    Row("Cleanups", "AspectJ deeper recce (0 high, 0 medium)",
        1.0, 2.0,
        "absent", 0.0,
        "done", 2.0),
    Row("Cleanups", "Mockito modernization (clean)",
        1.0, 2.0,
        "absent", 0.0,
        "done", 2.0),
    Row("Cleanups", "Metrics profile restore",
        1.0, 2.0,
        "done", 2.0,
        "done", 2.0),
    Row("Cleanups", "Hibernate L2 cache region audit + declarations",
        1.0, 3.0,
        "absent", 0.0,
        "inflight", 2.0),
    Row("Cleanups", "Spring profile cleanup (constants + dead 'testing')",
        1.0, 2.0,
        "absent", 0.0,
        "done", 2.0),
    Row("Cleanups", "VT executor caller migration (Groups 1+2+4)",
        1.0, 3.0,
        "absent", 0.0,
        "inflight", 2.0),

    # ---- Cloud-ready ---------------------------------------------------
    Row("Cloud-ready", "gemma-rest standalone Phase 1 (war-ready)",
        2.0, 3.0,
        "inflight", 2.0,     # v1: "gemma-rest standalone packaging" inflight
        "done", 2.0),
    Row("Cloud-ready", "gemma-rest standalone Phase 2 (embedded Tomcat)",
        3.0, 5.0,
        "inflight", 2.0,     # rolled into v1's "gemma-rest standalone packaging"
        "planned", 0.0),
    Row("Cloud-ready", "12-factor config (env vars, profiles)",
        3.0, 6.0,
        "inflight", 2.0,
        "inflight", 2.0),
    Row("Cloud-ready", "Object storage abstraction (S3/GCS)",
        5.0, 8.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("Cloud-ready", "Container image with sane defaults",
        5.0, 8.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("Cloud-ready", "Structured logging + OpenTelemetry",
        5.0, 8.0,
        "planned", 0.0,
        "planned", 0.0),

    # ---- Mobile-friendly -----------------------------------------------
    Row("Mobile-friendly", "Retire gemma-web (10-13 sessions)",
        2.0, 9.0,
        "planned", 0.0,      # v1: "Retire gemma-web (gemma-curation-ui)" planned
        "inflight", 2.0),
    Row("Mobile-friendly", "Selective field projection / GraphQL",
        5.0, 8.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("Mobile-friendly", "Cursor-based pagination",
        3.0, 6.0,
        "planned", 0.0,
        "planned", 0.0),

    # ---- AI-driven -----------------------------------------------------
    Row("AI-driven", "Vector store for similarity (pgvector)",
        6.0, 9.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("AI-driven", "Embeddings on metadata fields",
        6.0, 9.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("AI-driven", "LLM-friendly API surface",
        4.0, 7.0,
        "planned", 0.0,
        "planned", 0.0),
    Row("AI-driven", "Promote gemma-curation-agents in-tree",
        4.0, 6.0,
        "planned", 0.0,
        "planned", 0.0),
]


# ---------------------------------------------------------------- render
def _draw_tier(ax, y_center, height, row: Row, tier: str) -> None:
    """Draw one half of the two-tier row.

    tier="ghost" draws the v1 snapshot at pale tint.
    tier="solid" draws the now snapshot at full saturation.
    """
    if tier == "ghost":
        status = row.v1_status
        done_end = row.v1_done_end
        style_map = GHOST_STYLE
        done_color = GHOST_DONE
        inflight_color = GHOST_INFLIGHT
        bg_color = GHOST_PLANNED
    else:
        status = row.now_status
        done_end = row.now_done_end
        style_map = SOLID_STYLE
        done_color = ACCENT_DONE
        inflight_color = ACCENT_INFLIGHT
        bg_color = GRID

    plan_w = row.plan_end - row.plan_start
    done_w = max(0.0, done_end - row.plan_start)
    remaining_start = row.plan_start + done_w
    remaining_w = plan_w - done_w

    # background "planned span" rectangle
    ax.barh(y_center, plan_w, left=row.plan_start, height=height,
            color=bg_color, edgecolor="none", zorder=2)

    # remaining-portion overlay coloured by status
    face, edge, hatch = style_map[status]
    if remaining_w > 0:
        if status == "inflight":
            ax.barh(y_center, remaining_w, left=remaining_start,
                    height=height,
                    color=inflight_color, edgecolor="none",
                    alpha=(0.55 if tier == "solid" else 1.0),
                    zorder=2.5)
        elif status == "blocked":
            ax.barh(y_center, remaining_w, left=remaining_start,
                    height=height,
                    facecolor=face, edgecolor=edge, linewidth=1.0,
                    hatch=hatch, zorder=2.5)
        elif status == "deferred":
            ax.barh(y_center, remaining_w, left=remaining_start,
                    height=height,
                    facecolor=face, edgecolor=edge, linewidth=0.5,
                    hatch=hatch, zorder=2.5)
        # "planned" and "absent" stay as background

    # done-portion overlay
    if done_w > 0 and status == "done":
        ax.barh(y_center, done_w, left=row.plan_start, height=height,
                color=done_color, edgecolor="none", zorder=3)
    elif done_w > 0 and status == "inflight":
        # partial "done" prefix exists even for in-flight rows
        ax.barh(y_center, done_w, left=row.plan_start, height=height,
                color=done_color, edgecolor="none", zorder=3)


def render() -> None:
    apply_rcparams()

    n = len(ROWS)
    fig_h = max(7.0, 0.30 * n + 2.0)
    fig, ax = plt.subplots(figsize=(8.6, fig_h))

    row_h = 0.78          # total height of each (ghost + solid) pair
    tier_h = row_h / 2.0 - 0.02
    ghost_off = +row_h / 4.0   # ghost sits above row center
    solid_off = -row_h / 4.0   # solid sits below row center

    # Plot rows bottom-up so the first ROW is at the TOP.
    y_positions = list(range(n))
    rev = list(reversed(ROWS))
    for i, r in enumerate(rev):
        y = i
        _draw_tier(ax, y + ghost_off, tier_h, r, "ghost")
        _draw_tier(ax, y + solid_off, tier_h, r, "solid")

        # subtle horizontal hairline separating the two tiers, so
        # the visual split reads as "two stacked bars" rather than
        # "one bar with a colour change"
        ax.plot([r.plan_start, r.plan_end],
                [y, y],
                color="white", linewidth=0.8, zorder=3.2)

    # ---- y axis labels: highlight rows with progress delta ----
    labels = [r.label for r in rev]
    ax.set_yticks(y_positions)
    ax.set_yticklabels(labels, fontsize=8.0, color=TEXT)
    ax.tick_params(axis="y", length=0, pad=4)

    # bold tick labels on rows where v1 != now -- gives a second
    # visual cue alongside the colour delta
    rank = {"absent": 0, "planned": 1, "deferred": 1,
            "blocked": 2, "inflight": 3, "done": 4}
    for tick_label, r in zip(ax.get_yticklabels(), rev):
        if rank.get(r.now_status, 0) > rank.get(r.v1_status, 0):
            tick_label.set_fontweight("bold")
            tick_label.set_color(TEXT)
        elif r.v1_status == "absent" and r.now_status != "absent":
            tick_label.set_fontweight("bold")
            tick_label.set_color(TEXT)

    # category bands + right-side labels
    cat_ranges: list[tuple[str, int, int]] = []
    cur_cat = rev[0].category
    cur_lo = 0
    for i in range(1, len(rev)):
        if rev[i].category != cur_cat:
            cat_ranges.append((cur_cat, cur_lo, i - 1))
            cur_cat = rev[i].category
            cur_lo = i
    cat_ranges.append((cur_cat, cur_lo, len(rev) - 1))

    for idx, (cat, lo, hi) in enumerate(cat_ranges):
        if idx % 2 == 0:
            ax.axhspan(lo - 0.5, hi + 0.5, color="#f9fafb", zorder=1)
        ax.text(11.05, (lo + hi) / 2.0, cat, fontsize=9,
                color=SUBTLE, va="center", ha="left",
                fontweight="normal")

    # ---- x axis ----
    ax.set_xlim(0, 11.0)
    ax.set_ylim(-0.6, n - 0.4)
    xticks = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    xlabels = ["S0", "S1", "S2\n(today)", "S3", "S4", "S5",
               "S6", "S7", "S8", "S9", "S10+"]
    ax.set_xticks(xticks)
    ax.set_xticklabels(xlabels, fontsize=8, color=SUBTLE)
    ax.tick_params(axis="x", length=0, pad=3)

    ax.yaxis.grid(False)
    ax.xaxis.grid(True, color=GRID, linewidth=0.6, zorder=0)
    ax.set_axisbelow(True)

    # "today" reference line
    ax.axvline(TODAY_X, color=SUBTLE, linewidth=0.9, linestyle="--",
               zorder=4)
    ax.text(TODAY_X + 0.08, n - 0.7, "today (21:38)",
            fontsize=8, color=SUBTLE, va="top", ha="left")

    # ---- title + subtitle ----
    fig.suptitle("Phase 3 renovations - what moved between the first chart and now",
                 x=0.02, y=0.985,
                 ha="left", fontsize=14, fontweight="normal",
                 color=TEXT)
    ax.set_title(
        "Each row is two stacked bars: pale top half = status in the "
        "original chart; solid bottom half = status now. Rows where the "
        "two halves differ are rows where work landed in this session. "
        "Bold labels flag a status promotion.",
        fontsize=9, color=SUBTLE, loc="left", pad=10)

    # ---- legend: two rows, ghost tier above solid tier ----
    legend_handles = [
        mpatches.Patch(facecolor=GHOST_DONE,     label="v1: done"),
        mpatches.Patch(facecolor=GHOST_INFLIGHT, label="v1: in flight"),
        mpatches.Patch(facecolor=GHOST_PLANNED,  label="v1: planned / absent"),
        mpatches.Patch(facecolor=ACCENT_DONE,    label="now: done"),
        mpatches.Patch(facecolor=ACCENT_INFLIGHT, alpha=0.55,
                       label="now: in flight"),
        mpatches.Patch(facecolor=GRID,           label="now: planned"),
        mpatches.Patch(facecolor=GRID, edgecolor=ACCENT_BLOCKED,
                       hatch="//", linewidth=1.0,
                       label="now: blocked"),
        mpatches.Patch(facecolor=GRID, edgecolor=SUBTLE,
                       hatch="..", linewidth=0.5,
                       label="now: deferred"),
    ]
    leg = ax.legend(handles=legend_handles, loc="lower right",
                    bbox_to_anchor=(1.0, -0.08),
                    ncol=4, frameon=False, fontsize=8,
                    handlelength=1.4, handleheight=1.0,
                    columnspacing=1.2)
    for txt in leg.get_texts():
        txt.set_color(TEXT)

    # ---- source caption ----
    fig.text(0.02, 0.005,
             "Source: figures/build_renovations_gantt.py (v1 status) + "
             "figures/build_renovations_gantt_2026-05-18_2132.py (now status). "
             "Snapshot 2026-05-18 21:38.",
             fontsize=7.5, color=SUBTLE, ha="left", va="bottom")

    # ---- layout + clipPath strip + save ----
    fig.subplots_adjust(left=0.36, right=0.86, top=0.93, bottom=0.07)

    ax.set_clip_on(False)
    for a in (list(ax.patches) + list(ax.lines) + list(ax.texts)
              + list(ax.collections) + list(ax.images)):
        a.set_clip_on(False)

    out = ("/Users/pzoot/Dev/eclipseworkspace/Gemma/figures/"
           "renovations_gantt_2026-05-18_2138.svg")
    fig.savefig(out, format="svg", bbox_inches="tight", facecolor="white")

    # Strip clipPath wrappers post-write
    with open(out, "r", encoding="utf-8") as fh:
        svg = fh.read()
    svg = re.sub(r' clip-path="url\(#[^"]+\)"', "", svg)
    svg = re.sub(r"<clipPath[^>]*>.*?</clipPath>", "", svg, flags=re.S)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(svg)

    # Counts for the report
    changed = 0
    unchanged = 0
    for r in ROWS:
        if r.v1_status != r.now_status:
            changed += 1
        else:
            unchanged += 1
    print(f"wrote {out}")
    print(f"  rows total: {len(ROWS)}")
    print(f"  rows with v1 -> now status delta: {changed}")
    print(f"  rows unchanged: {unchanged}")


if __name__ == "__main__":
    render()
