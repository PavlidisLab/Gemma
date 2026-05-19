"""Updated Gantt chart of the Phase 3 renovations plan vs. landed progress.

Snapshot taken 2026-05-18 21:32. Many items that were "in flight" in the
first chart at figures/renovations_gantt.svg have now landed; this
chart re-marks them done and adds task bars for renovations that were
recce'd or executed after the first chart was rendered.

Inputs:
  - /Users/pzoot/Dev/eclipseworkspace/Gemma/PHASE_3_VISION.md
  - figures/build_renovations_gantt.py (prior chart, data shape source)
  - git log --all --since=2026-05-17 --pretty=format:'%ad %h %s' --date=short

Output:
  - figures/renovations_gantt_2026-05-18_2132.svg

Style: same hard-rules as the first chart (Helvetica + Arial fallback,
ASCII glyphs only, svg.fonttype="none", strip clipPath wrappers,
white facecolor, y-axis grid only, title left-aligned normal weight,
8pt gray-500 source caption).
"""
from __future__ import annotations

import re
from dataclasses import dataclass

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches


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
# Sessions are the natural time unit per the original plan. The actual
# landed work compressed into a marathon kickoff (S1) + an evening
# parallel wave (S2) + a late-evening second wave that closed many
# items (still S2 by clock, but new bars carry done_end=2.0). S3+ is
# forward-looking.
#
# Session map:
#   S1 = 2026-05-18 day      kickoff (4 parallel agents)
#   S2 = 2026-05-18 evening  multi-wave parallel push (this snapshot)
#   S3 = next planned session
#   S4+ = downstream / queued
#   S8+ = blocked / deliberately deferred

TODAY_X = 2.0  # end of S2 (today = 2026-05-18 21:32)

@dataclass
class Task:
    category: str
    label: str
    plan_start: float
    plan_end: float
    done_end: float
    status: str         # "done", "inflight", "planned", "blocked", "deferred"
    note: str = ""


TASKS: list[Task] = [
    # ---- First wave (foundational, the three lighthouses) -----------------
    Task("First wave",  "Flyway / Liquibase schema versioning",
         0.0, 4.0, 2.0, "inflight",
         "H2 + MySQL baseline landed; prod wiring still blocked on ops"),
    Task("First wave",  "Streaming-by-default DAOs",
         0.0, 5.0, 0.0, "deferred",
         "deprioritized this session (perf-flavor)"),
    Task("First wave",  "Test-fixture rewrite (factories)",
         0.0, 6.0, 2.0, "inflight",
         "Experiment/BioMaterial/ArrayDesign done; ~5 entities remain"),

    # ---- ACL / security --------------------------------------------------
    Task("ACL & security", "ACL listener cutover (gsec)",
         0.0, 1.0, 1.0, "done"),
    Task("ACL & security", "Sid-type unification (Spring stock)",
         0.0, 1.0, 1.0, "done"),
    Task("ACL & security", "ACL upper->lower data migration (prod)",
         0.0, 1.0, 1.0, "done"),
    Task("ACL & security", "Drop old uppercase ACL tables",
         3.0, 4.0, 0.0, "blocked",
         "blocked on 1 release cycle of write cutover"),
    Task("ACL & security", "gsec HQL deprecation",
         1.0, 5.0, 2.0, "inflight",
         "5 sites converted; AclQueryUtils still high-risk"),
    Task("ACL & security", "AfterInvocation Phase A+B (Gemma-owned providers)",
         1.0, 2.0, 2.0, "done",
         "QUIET + CS/DV + VO providers all Gemma-owned"),
    Task("ACL & security", "AfterInvocation Phase C (AuthorizationManager)",
         2.0, 5.0, 2.0, "inflight",
         "PREP + 9-provider plan landed; execution next"),
    Task("ACL & security", "AclEntryVoter recce + Phase X.1 wrappers",
         2.0, 3.0, 2.0, "done",
         "281 sites mapped; 6 AuthorizationManager wrappers landed"),
    Task("ACL & security", "AclEntryVoter Phase X.2-X.4 (interceptor + sweep)",
         3.0, 6.0, 0.0, "planned"),
    Task("ACL & security", "Spring Security 7 readiness recce",
         2.0, 3.0, 2.0, "done",
         "4 gaps, 17-36 day plan documented"),

    # ---- Easier to maintain ----------------------------------------------
    Task("Maintainability", "XML -> @Configuration (6 modules)",
         0.0, 3.0, 2.0, "done"),
    Task("Maintainability", "EE service Phase 1 (ReadService extract)",
         2.0, 3.0, 2.0, "done",
         "58 retrieval methods; 2 cycles broken"),
    Task("Maintainability", "EE service Phase 1.5 + 2 (continued decomp)",
         2.0, 6.0, 2.0, "inflight",
         "Phase 1.5 in flight; bucket B/G extracted"),
    Task("Maintainability", "persisterHelper retirement",
         2.0, 8.0, 2.0, "inflight",
         "Common/ArrayDesign/Relationship done; ExpressionPersister now 89 LoC delegate; Genome 5.4-5.5 next"),
    Task("Maintainability", "ExpressionPersister actual deletion",
         3.0, 5.0, 0.0, "planned",
         "deferred; delegate currently still wired"),
    Task("Maintainability", "GenomePersister chunks 5.4 + 5.5",
         2.0, 4.0, 1.7, "inflight",
         "5.4 attempted + rolled back; Chromosome.taxon fix pending"),
    Task("Maintainability", "Impl-autowire enforcer rule",
         2.0, 3.0, 1.7, "inflight"),
    Task("Maintainability", "gemma-curation-ui contract check",
         2.0, 3.0, 1.7, "inflight"),
    Task("Maintainability", "Externalize ACL (OPA / Cedar)",
         6.0, 10.0, 0.0, "planned"),
    Task("Maintainability", "Deprecate ensureInSession / findOrCreate",
         4.0, 7.0, 0.0, "planned"),
    Task("Maintainability", "@Ignore'd test audit",
         1.0, 2.0, 2.0, "done"),
    Task("Maintainability", "session.refresh edge cases",
         1.0, 2.0, 2.0, "done"),

    # ---- Framework / dependency bumps ------------------------------------
    Task("Framework bumps", "Spring Framework 6.1 -> 6.2",
         1.0, 2.0, 2.0, "done"),
    Task("Framework bumps", "Spring Security 6.3 -> 6.5",
         1.0, 2.0, 2.0, "done"),
    Task("Framework bumps", "Hibernate 6.4 -> 6.6",
         1.0, 2.0, 2.0, "done"),
    Task("Framework bumps", "Spring Boot 3 BOM-only adoption",
         1.0, 2.0, 2.0, "done",
         "BOM imported; 4 properties reclaimed"),
    Task("Framework bumps", "gsec version alignment recce",
         2.0, 3.0, 2.0, "done",
         "zero source changes needed for bump"),
    Task("Framework bumps", "Java 21 readiness recce + Phase 1",
         1.0, 6.0, 2.0, "inflight",
         "Phase 1 audit-only landed; deps already at JDK21 floor"),
    Task("Framework bumps", "Maven plugin modernization + build hygiene",
         1.0, 2.0, 2.0, "done",
         "git-commit-id 9->9.2; reactor convergence enforcer; RELEASING.md"),
    Task("Framework bumps", "Maven release plugin recce",
         2.0, 3.0, 2.0, "done",
         "no change needed"),
    Task("Framework bumps", "JUnit 5 Phase A + B0 (pilot)",
         2.0, 3.0, 2.0, "done",
         "Jupiter + Vintage on classpath; dual-selector wired"),
    Task("Framework bumps", "JUnit 5 Phase B1+ (inheritance chain)",
         3.0, 7.0, 0.0, "planned"),

    # ---- Cleanups & audits -----------------------------------------------
    Task("Cleanups", "Coexpression stub removal",
         0.0, 1.0, 1.0, "done"),
    Task("Cleanups", "ThreadLocal removal (encoder + provider)",
         0.0, 2.0, 2.0, "done"),
    Task("Cleanups", "slf4j-api 1.7 -> 2.0 + @CommonsLog -> @Slf4j (188)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "HikariCP audit (5.1.0 current)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Hibernate envers audit (not used)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "RestTemplate -> RestClient",
         1.0, 4.0, 2.0, "inflight",
         "GoogleAnalytics4Provider done; rest audited"),
    Task("Cleanups", "Lombok cleanup (records, @SneakyThrows, BlatResult)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "@Cacheable audit (not annotated; programmatic)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Validation audit (not wired)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "AspectJ deeper recce (0 high, 0 medium)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Mockito modernization (clean)",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Metrics profile restore",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "Hibernate L2 cache region audit + declarations",
         1.0, 3.0, 2.0, "inflight",
         "~93 regions unbounded; declarations in flight"),
    Task("Cleanups", "Spring profile cleanup (constants + dead 'testing')",
         1.0, 2.0, 2.0, "done"),
    Task("Cleanups", "VT executor caller migration (Groups 1+2+4)",
         1.0, 3.0, 2.0, "inflight",
         "Groups 3+5 deferred (scheduled-executor blocker)"),

    # ---- Cloud-ready -----------------------------------------------------
    Task("Cloud-ready", "gemma-rest standalone Phase 1 (war-ready)",
         2.0, 3.0, 2.0, "done",
         "web.xml bootstrap landed"),
    Task("Cloud-ready", "gemma-rest standalone Phase 2 (embedded Tomcat)",
         3.0, 5.0, 0.0, "planned"),
    Task("Cloud-ready", "12-factor config (env vars, profiles)",
         3.0, 6.0, 2.0, "inflight"),
    Task("Cloud-ready", "Object storage abstraction (S3/GCS)",
         5.0, 8.0, 0.0, "planned"),
    Task("Cloud-ready", "Container image with sane defaults",
         5.0, 8.0, 0.0, "planned"),
    Task("Cloud-ready", "Structured logging + OpenTelemetry",
         5.0, 8.0, 0.0, "planned"),

    # ---- Mobile-friendly -------------------------------------------------
    Task("Mobile-friendly", "Retire gemma-web (10-13 sessions)",
         2.0, 9.0, 2.0, "inflight",
         "retirement plan landed; gemma-curation-ui contract in flight"),
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

    n = len(TASKS)
    fig_h = max(6.5, 0.26 * n + 2.0)
    fig, ax = plt.subplots(figsize=(8.4, fig_h))

    bar_height = 0.62

    y_positions = list(range(n))
    for i, t in enumerate(reversed(TASKS)):
        y = i
        plan_w = t.plan_end - t.plan_start
        done_w = max(0.0, t.done_end - t.plan_start)
        remaining_start = t.plan_start + done_w
        remaining_w = plan_w - done_w

        # background "planned" bar
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
                ax.barh(y, remaining_w, left=remaining_start,
                        height=bar_height,
                        facecolor=GRID,
                        edgecolor=ACCENT_BLOCKED, linewidth=1.4,
                        hatch="//", zorder=2.5)
            elif t.status == "deferred":
                ax.barh(y, remaining_w, left=remaining_start,
                        height=bar_height,
                        facecolor=GRID, edgecolor=SUBTLE, linewidth=0.6,
                        hatch="..", zorder=2.5)

        # done-portion overlay
        if done_w > 0:
            ax.barh(y, done_w, left=t.plan_start, height=bar_height,
                    color=ACCENT_DONE, edgecolor="none", zorder=3)

    # ---- y axis labels ----
    labels = [t.label for t in reversed(TASKS)]
    ax.set_yticks(y_positions)
    ax.set_yticklabels(labels, fontsize=8.0, color=TEXT)
    ax.tick_params(axis="y", length=0, pad=4)

    # category groupings
    rev = list(reversed(TASKS))
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
    ax.set_ylim(-0.5, n - 0.5)
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
    ax.text(TODAY_X + 0.08, n - 0.7, "today (21:32)",
            fontsize=8, color=SUBTLE, va="top", ha="left")

    # ---- title + subtitle ----
    fig.suptitle("Phase 3 renovations - plan vs. progress (2026-05-18 21:32 snapshot)",
                 x=0.02, y=0.985,
                 ha="left", fontsize=14, fontweight="normal",
                 color=TEXT)
    ax.set_title("Updated from the earlier S2 chart: items closed since the first render "
                 "are re-marked green; new bars track work surfaced this session.",
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
                    bbox_to_anchor=(1.0, -0.08),
                    ncol=5, frameon=False, fontsize=8,
                    handlelength=1.4, handleheight=1.0,
                    columnspacing=1.2)
    for txt in leg.get_texts():
        txt.set_color(TEXT)

    # ---- source caption ----
    fig.text(0.02, 0.005,
             "Source: PHASE_3_VISION.md; figures/build_renovations_gantt.py (prior); "
             "git log --all --since=2026-05-17. "
             "Snapshot 2026-05-18 21:32.",
             fontsize=7.5, color=SUBTLE, ha="left", va="bottom")

    # ---- layout + clipPath strip + save ----
    fig.subplots_adjust(left=0.36, right=0.86, top=0.93, bottom=0.07)

    ax.set_clip_on(False)
    for a in (list(ax.patches) + list(ax.lines) + list(ax.texts)
              + list(ax.collections) + list(ax.images)):
        a.set_clip_on(False)

    out = ("/Users/pzoot/Dev/eclipseworkspace/Gemma/figures/"
           "renovations_gantt_2026-05-18_2132.svg")
    fig.savefig(out, format="svg", bbox_inches="tight", facecolor="white")

    with open(out, "r", encoding="utf-8") as fh:
        svg = fh.read()
    svg = re.sub(r' clip-path="url\(#[^"]+\)"', "", svg)
    svg = re.sub(r"<clipPath[^>]*>.*?</clipPath>", "", svg, flags=re.S)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(svg)
    print(f"wrote {out}")


if __name__ == "__main__":
    render()
