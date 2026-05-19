"""Gantt chart of the Phase 3 renovations plan vs. landed progress.

Inputs (read by hand, transcribed below — the source markdown is too
narrative to parse mechanically and the worktree branch list is
authoritative for what shipped):

  - /Users/pzoot/Dev/eclipseworkspace/Gemma/PHASE_3_VISION.md
    (original plan, six-dimension framing + three first-wave items)
  - /Users/pzoot/.claude/projects/.../memory/project_phase3_progress.md
    (in-flight + done snapshot at 2026-05-18 end-of-day)
  - git log --since=2026-05-15 --all --pretty=format:'%ad %h %s'
    (per-commit confirmation of what landed on the worktree-* branches)

Output:
  - figures/renovations_gantt.svg  (lab-style flat horizontal Gantt)

Style is pinned to the user's global figure rules:
  - Helvetica + Arial fallback ONLY (no DejaVu)
  - ASCII glyphs only; status encoded via colour
  - svg.fonttype="none"; clipPath wrappers stripped before save
  - White facecolor; y-axis grid only; top/right/left/bottom spines off
  - Title left-aligned, normal weight
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import datetime

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.lines import Line2D


# ---------------------------------------------------------------- palette
ACCENT_DONE       = "#10b981"  # emerald-500
ACCENT_INFLIGHT   = "#f59e0b"  # amber-500
ACCENT_BLOCKED    = "#ef4444"  # red-500 (used as edge)
GRID              = "#e5e7eb"  # gray-200 (not-started bar fill)
TEXT              = "#1f2937"  # gray-800
SUBTLE            = "#6b7280"  # gray-500


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
# Sessions are the natural time unit per the original plan ("each
# first-wave item is ~1-2 weeks of focused work"). The actual landed
# work compressed into a marathon kickoff (S1) + an evening parallel
# wave (S2). S3+ is forward-looking.
#
# Session map:
#   S1 = 2026-05-18 day      kickoff session (4 parallel agents)
#   S2 = 2026-05-18 evening  six more agents in parallel
#   S3 = next planned session
#   S4+ = downstream / queued
#   S8+ = blocked / deliberately deferred

TODAY_X = 4.0  # end of S4 (now = 2026-05-19 early hours)

@dataclass
class Task:
    category: str
    label: str
    plan_start: float   # planned start session
    plan_end: float     # planned end session
    done_end: float     # how far the done-bar fills (<= plan_end if not done)
    status: str         # "done", "inflight", "planned", "blocked", "deferred"
    note: str = ""


TASKS: list[Task] = [
    # ---- First wave (foundational, the three lighthouses) -----------------
    Task("First wave",  "Flyway / Liquibase schema versioning",
         0.0, 5.0, 3.0, "inflight",
         "H2 + MySQL baseline landed; prod wiring blocked on ops"),
    Task("First wave",  "Streaming-by-default DAOs",
         0.0, 5.0, 0.0, "deferred",
         "deprioritized this session (perf-flavor)"),
    Task("First wave",  "Test-fixture rewrite (factories)",
         0.0, 6.0, 3.0, "inflight",
         "Experiment + BioMaterial + ArrayDesign factories; ~5 entities remain"),

    # ---- ACL / security (Phase 2 residual completed in Phase 3) ----------
    Task("ACL & security", "ACL listener cutover (gsec)",
         0.0, 1.0, 1.0, "done"),
    Task("ACL & security", "Sid-type unification (Spring stock)",
         0.0, 1.0, 1.0, "done"),
    Task("ACL & security", "ACL upper->lower data migration (prod)",
         0.0, 1.0, 1.0, "done"),
    Task("ACL & security", "applicationContext-security.xml -> SecurityConfig",
         3.0, 4.0, 4.0, "done",
         "500 LoC Java config; 23 @Beans; runtime smoke pending"),
    Task("ACL & security", "Drop old uppercase ACL tables",
         3.0, 4.0, 0.0, "blocked",
         "blocked on 1 release cycle of write cutover"),
    Task("ACL & security", "gsec HQL deprecation",
         1.0, 5.0, 3.0, "inflight",
         "12 sites inventoried; 5 converted; AclQueryUtils high-risk"),
    Task("ACL & security", "@EnableMethodSecurity migration (14 providers)",
         1.0, 3.0, 3.0, "done",
         "AfterInvocation Phases A+B landed; on legacy stack by design"),

    # ---- Easier to maintain ----------------------------------------------
    Task("Maintainability", "XML -> @Configuration (6 modules)",
         0.0, 4.0, 4.0, "done",
         "component-scan, serviceBeans, dataSource, hibernate, schedule, security, gemma-rest, gemma-cli"),
    Task("Maintainability", "Decompose ExpressionExperimentServiceImpl",
         2.0, 6.0, 4.0, "inflight",
         "recce + roadmap landed; decomposition not yet started"),
    Task("Maintainability", "persisterHelper retirement (~9.5 sessions)",
         2.0, 8.0, 3.0, "inflight",
         "BusinessKey lifts done; CommonPersister, GenomePersister, RelationshipPersister, ArrayDesignPersister, ExpressionPersister rewired"),
    Task("Maintainability", "Externalize ACL (OPA / Cedar)",
         6.0, 10.0, 0.0, "planned"),
    Task("Maintainability", "Deprecate ensureInSession / findOrCreate",
         4.0, 7.0, 0.0, "planned"),
    Task("Maintainability", "@Ignore'd test audit",
         1.0, 2.0, 2.0, "done",
         "2 re-enabled, 1 split, 41 deferred as pre-existing flake"),
    Task("Maintainability", "session.refresh edge cases",
         1.0, 2.0, 2.0, "done",
         "callsites were phantom"),

    # ---- Framework / dependency bumps ------------------------------------
    Task("Framework bumps", "Spring Framework 6.1 -> 6.2",
         1.0, 3.0, 3.0, "done"),
    Task("Framework bumps", "Spring Security 6.3 -> 6.5",
         1.0, 3.0, 3.0, "done"),
    Task("Framework bumps", "Hibernate 6.4 -> 6.6",
         1.0, 3.0, 3.0, "done"),
    Task("Framework bumps", "Spring Boot dep. BOM 3.3 -> 3.5",
         1.0, 4.0, 4.0, "done",
         "BOM 3.5.6 matches SF/SS/HB natively; shrunk override surface"),
    Task("Framework bumps", "HikariCP 5 -> 6",
         3.0, 4.0, 4.0, "done",
         "5.1.0 -> 6.3.3 via Boot BOM"),
    Task("Framework bumps", "gsec 0.0.23 -> 0.0.24",
         3.0, 3.0, 3.0, "done"),
    Task("Framework bumps", "Java 21 readiness (still on 17)",
         1.0, 6.0, 3.0, "inflight",
         "Lombok/AspectJ/JaCoCo pre-bumped to JDK-21 floors"),
    Task("Framework bumps", "Maven plugin modernization",
         1.0, 2.0, 2.0, "done"),
    Task("Framework bumps", "JUnit 5 migration",
         2.0, 7.0, 4.0, "inflight",
         "recce + roadmap; BaseJerseyTest already off SpringJUnit4"),

    # ---- Cleanups & audits -----------------------------------------------
    Task("Cleanups", "Coexpression stub removal",
         0.0, 1.0, 1.0, "done"),
    Task("Cleanups", "ThreadLocal removal (encoder + provider)",
         0.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Logging: @CommonsLog -> @Slf4j (188 sites)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "HikariCP modernize / Hibernate envers audit",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "RestTemplate -> RestClient",
         1.0, 4.0, 3.0, "inflight",
         "GoogleAnalytics4Provider done; rest audited"),
    Task("Cleanups", "Lombok cleanup (records, @SneakyThrows)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Cache modernization (JCache, @Cacheable)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Metrics profile restore",
         1.0, 4.0, 4.0, "done",
         "MeterRegistryJCacheConfigurer + MetricsConfigTest landed S4"),
    Task("Cleanups", "JUnit jupiter version alignment",
         4.0, 4.0, 4.0, "done",
         "5.11.4 -> 5.12.2 to match Boot 3.5.6 BOM"),

    # ---- Cloud-ready -----------------------------------------------------
    Task("Cloud-ready", "gemma-rest standalone packaging",
         2.0, 6.0, 4.0, "inflight",
         "standalone-recce flagged 3 blockers; ~1.75 sessions to first WAR"),
    Task("Cloud-ready", "12-factor config (env vars, profiles)",
         3.0, 6.0, 3.0, "inflight",
         "Spring profiles audit landed"),
    Task("Cloud-ready", "Object storage abstraction (S3/GCS)",
         5.0, 8.0, 0.0, "planned"),
    Task("Cloud-ready", "Container image with sane defaults",
         5.0, 8.0, 0.0, "planned"),
    Task("Cloud-ready", "Structured logging + OpenTelemetry",
         5.0, 8.0, 0.0, "planned"),

    # ---- Mobile-friendly -------------------------------------------------
    Task("Mobile-friendly", "Retire gemma-web (gemma-curation-ui)",
         2.0, 9.0, 0.0, "planned",
         "tracked separately"),
    Task("Mobile-friendly", "Selective field projection / GraphQL",
         5.0, 8.0, 0.0, "planned"),
    Task("Mobile-friendly", "Cursor-based pagination",
         3.0, 6.0, 0.0, "planned"),

    # ---- AI-driven -------------------------------------------------------
    Task("AI-driven", "Vector store for similarity (pgvector)",
         6.0, 9.0, 0.0, "planned"),
    Task("AI-driven", "Embeddings on metadata fields",
         6.0, 9.0, 0.0, "planned"),
    Task("AI-driven", "LLM-friendly API surface",
         4.0, 7.0, 0.0, "planned"),
    Task("AI-driven", "Promote gemma-curation-agents in-tree",
         4.0, 6.0, 0.0, "planned"),
]


# ---------------------------------------------------------------- render
def render() -> None:
    apply_rcparams()

    # Order: keep category groups together, in the order they appear in TASKS.
    # Reverse so first category sits at the top of the chart.
    n = len(TASKS)
    fig_h = max(6.5, 0.26 * n + 2.0)
    fig, ax = plt.subplots(figsize=(8.0, fig_h))

    bar_height = 0.62

    # Plot bars bottom-up so the first TASK is at the TOP.
    y_positions = list(range(n))
    for i, t in enumerate(reversed(TASKS)):
        y = i
        plan_w = t.plan_end - t.plan_start
        done_w = max(0.0, t.done_end - t.plan_start)
        remaining_start = t.plan_start + done_w
        remaining_w = plan_w - done_w

        # background "planned" bar (gray-200, no edge)
        ax.barh(y, plan_w, left=t.plan_start, height=bar_height,
                color=GRID, edgecolor="none", zorder=2)

        # remaining-portion overlay coloured by status
        if remaining_w > 0:
            if t.status == "inflight":
                ax.barh(y, remaining_w, left=remaining_start,
                        height=bar_height,
                        color=ACCENT_INFLIGHT, edgecolor="none",
                        alpha=0.55, zorder=2.5)
            elif t.status == "blocked":
                # red edge, hatched fill on the unstarted portion
                ax.barh(y, remaining_w, left=remaining_start,
                        height=bar_height,
                        facecolor=GRID,
                        edgecolor=ACCENT_BLOCKED, linewidth=1.4,
                        hatch="//", zorder=2.5)
            elif t.status == "deferred":
                # subtle pattern, no edge change
                ax.barh(y, remaining_w, left=remaining_start,
                        height=bar_height,
                        facecolor=GRID, edgecolor=SUBTLE, linewidth=0.6,
                        hatch="..", zorder=2.5)

        # done-portion overlay (emerald)
        if done_w > 0:
            ax.barh(y, done_w, left=t.plan_start, height=bar_height,
                    color=ACCENT_DONE, edgecolor="none", zorder=3)

    # ---- y axis: labels grouped by category ----
    labels = [t.label for t in reversed(TASKS)]
    ax.set_yticks(y_positions)
    ax.set_yticklabels(labels, fontsize=8.5, color=TEXT)
    ax.tick_params(axis="y", length=0, pad=4)

    # Category band separators + right-side category labels.
    # Walk tasks in display order (top-to-bottom = reversed index high-to-low)
    cat_ranges: list[tuple[str, int, int]] = []  # (cat, y_low, y_high)
    rev = list(reversed(TASKS))
    cur_cat = rev[0].category
    cur_lo = 0
    for i in range(1, len(rev)):
        if rev[i].category != cur_cat:
            cat_ranges.append((cur_cat, cur_lo, i - 1))
            cur_cat = rev[i].category
            cur_lo = i
    cat_ranges.append((cur_cat, cur_lo, len(rev) - 1))

    # Light alternating row-band shading for category groups.
    for idx, (cat, lo, hi) in enumerate(cat_ranges):
        if idx % 2 == 0:
            ax.axhspan(lo - 0.5, hi + 0.5, color="#f9fafb", zorder=1)
        # category label at right margin, vertically centered on the group
        ax.text(11.05, (lo + hi) / 2.0, cat, fontsize=9,
                color=SUBTLE, va="center", ha="left",
                fontweight="normal")

    # ---- x axis: session ticks ----
    ax.set_xlim(0, 11.0)
    ax.set_ylim(-0.5, n - 0.5)
    xticks = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    xlabels = ["S0", "S1", "S2\n(today)", "S3", "S4", "S5",
               "S6", "S7", "S8", "S9", "S10+"]
    ax.set_xticks(xticks)
    ax.set_xticklabels(xlabels, fontsize=8, color=SUBTLE)
    ax.tick_params(axis="x", length=0, pad=3)

    # Y-axis grid only (per lab style), light gray-200
    ax.yaxis.grid(False)
    ax.xaxis.grid(True, color=GRID, linewidth=0.6, zorder=0)
    ax.set_axisbelow(True)

    # "Today" reference line
    ax.axvline(TODAY_X, color=SUBTLE, linewidth=0.9, linestyle="--",
               zorder=4)
    ax.text(TODAY_X + 0.08, n - 0.7, "today",
            fontsize=8, color=SUBTLE, va="top", ha="left")

    # ---- title + subtitle ----
    fig.suptitle("Phase 3 renovations — plan vs. progress",
                 x=0.02, y=0.985,
                 ha="left", fontsize=14, fontweight="normal",
                 color=TEXT)
    ax.set_title("Per PHASE_3_VISION.md; horizontal bars are the planned span, "
                 "green is shipped, amber is in flight, red hatch is blocked.",
                 fontsize=9, color=SUBTLE, loc="left", pad=10)

    # ---- legend ----
    legend_handles = [
        mpatches.Patch(facecolor=ACCENT_DONE,    label="Done"),
        mpatches.Patch(facecolor=ACCENT_INFLIGHT, alpha=0.55,
                       label="In flight"),
        mpatches.Patch(facecolor=GRID,           label="Planned"),
        mpatches.Patch(facecolor=GRID, edgecolor=ACCENT_BLOCKED,
                       hatch="//", linewidth=1.2,
                       label="Blocked on ops"),
        mpatches.Patch(facecolor=GRID, edgecolor=SUBTLE,
                       hatch="..", linewidth=0.6,
                       label="Deferred"),
    ]
    leg = ax.legend(handles=legend_handles, loc="lower right",
                    bbox_to_anchor=(1.0, -0.10),
                    ncol=5, frameon=False, fontsize=8,
                    handlelength=1.4, handleheight=1.0,
                    columnspacing=1.2)
    for txt in leg.get_texts():
        txt.set_color(TEXT)

    # ---- source caption ----
    fig.text(0.02, 0.005,
             "Source: PHASE_3_VISION.md; memory/project_phase3_progress.md; "
             "git log across worktree-* branches. "
             f"Generated {datetime.now():%Y-%m-%d %H:%M}.",
             fontsize=7.5, color=SUBTLE, ha="left", va="bottom")

    # ---- layout + clipPath strip + save ----
    fig.subplots_adjust(left=0.36, right=0.86, top=0.93, bottom=0.07)

    # Strip clipPath wrappers so SVG round-trips through Illustrator
    # cleanly (per the user's CLAUDE.md SVG rules).
    ax.set_clip_on(False)
    for a in (list(ax.patches) + list(ax.lines) + list(ax.texts)
              + list(ax.collections) + list(ax.images)):
        a.set_clip_on(False)

    stamp = datetime.now().strftime("%Y-%m-%d_%H%M")
    out = f"/Users/pzoot/Dev/eclipseworkspace/Gemma/figures/renovations_gantt_{stamp}.svg"
    fig.savefig(out, format="svg", bbox_inches="tight", facecolor="white")

    # Strip clipPath wrappers post-write — matplotlib's SVG backend
    # emits them regardless of artist-level set_clip_on(False), and
    # Illustrator's Tiny SVG import drops them with a warning anyway.
    with open(out, "r", encoding="utf-8") as fh:
        svg = fh.read()
    svg = re.sub(r' clip-path="url\(#[^"]+\)"', "", svg)
    svg = re.sub(r"<clipPath[^>]*>.*?</clipPath>", "", svg, flags=re.S)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(svg)
    print(f"wrote {out}")


if __name__ == "__main__":
    render()
