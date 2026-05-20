"""Phase 3 renovations Gantt v8 - done-only, day-0 anchored, no future.

Reframe vs. v7: drop in-flight, queued, blocked, and deferred rows.
Show ONLY items that are actually done, with bars spanning real-time
first-commit -> last-commit. X-axis starts at day 0 = 2026-05-18 00:00
(no negative days; pre-day-0 starts clipped to 0) and ends at today's
"now" line - nothing extends into the future.

Compared to v7 (93 rows: 60 done + 14 in flight + 16 queued + 2
deferred + 1 blocked) v8 keeps just the 60 done items. The chart is
roughly half the height of v7 and has zero unfinished bars.

Bars are emerald (the lab's "done / good" tone). Rows are grouped
by category with a banded background and a right-margin category
label, identical to v7.

Sources:
  - git log --since=2026-05-17 across phase2-acl-migrate
  - RENOVATIONS.md, PHASE_2_HANDOFF.md, PHASE_3_VISION.md
  - SESSION_CLOSE_NOTE_2026-05-19.md
  - memory/project_phase3_progress.md

Output:
  - figures/renovations_gantt.svg                       (canonical)
  - figures/renovations_gantt_<YYYY-MM-DD>_<HHMM>.svg   (stamped)

Style follows ~/.claude/CLAUDE.md (flat / modern / Helvetica
fallback / svg.fonttype=none / clipPath stripped / ASCII only).
"""
from __future__ import annotations

import os
import re
import time
from dataclasses import dataclass
from datetime import datetime

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches


# ---------------------------------------------------------------- palette
ACCENT_DONE     = "#10b981"  # emerald-500
GRID            = "#e5e7eb"  # gray-200
TEXT            = "#1f2937"  # gray-800
SUBTLE          = "#6b7280"  # gray-500
BAND            = "#f9fafb"  # gray-50 (alternating row band)


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
        "axes.spines.top":    False,
        "axes.spines.right":  False,
        "axes.spines.bottom": False,
        "axes.spines.left":   False,
    })


# ---------------------------------------------------------------- time axis
# Day 0 = 2026-05-18 00:00 local. Pre-day-0 starts (Phase 2 residual
# work on 2026-05-17) are clipped to 0 - the chart never goes negative.
DAY0_EPOCH = datetime(2026, 5, 18, 0, 0, 0).timestamp()


def ts_to_day(ts: float | None) -> float | None:
    if ts is None:
        return None
    return (ts - DAY0_EPOCH) / 86400.0


NOW_TS = time.time()
NOW_DAY = ts_to_day(NOW_TS)


# ---------------------------------------------------------------- data
# Done-only view: every row here has status="done" and both first_ts
# and last_ts populated from git log. v7's in-flight / queued / blocked /
# deferred entries have been removed.

@dataclass
class Task:
    category: str
    label: str
    first_ts: int
    last_ts:  int


TASKS: list[Task] = [
    # ----- First wave (foundational) ------------------------------------
    Task("First wave", "Flyway schema versioning (H2 + MySQL)",
         1779146310, 1779154230),
    Task("First wave", "Test-fixture factories (Experiment/BioMaterial/ArrayDesign/Taxon)",
         1779153137, 1779166909),

    # ----- ACL & security -----------------------------------------------
    Task("ACL & security", "Phase 2 ACL canonical schema migration",
         1779092497, 1779147834),
    Task("ACL & security", "ACL listener cutover + AOP unwiring",
         1779119034, 1779140301),
    Task("ACL & security", "Sid unification (Gemma-owned)",
         1779115714, 1779184409),
    Task("ACL & security", "javac -parameters (SpEL named bindings)",
         1779151208, 1779151208),
    Task("ACL & security", "ACL upper->lower prod data migration",
         1779155126, 1779155126),
    Task("ACL & security", "applicationContext-security.xml -> @Configuration",
         1779146786, 1779159152),
    Task("ACL & security", "@EnableMethodSecurity migration (legacy stack)",
         1779146786, 1779159866),
    Task("ACL & security", "AfterInvocation provider migration (Phases A/B/C)",
         1779159042, 1779168189),
    Task("ACL & security", "AclEntryVoter -> AuthorizationManager wrappers",
         1779164176, 1779168204),
    Task("ACL & security", "gsec absorption A (copy + drop dep)",
         1779179186, 1779179186),
    Task("ACL & security", "gsec absorption B (Sid unification)",
         1779184409, 1779184409),
    Task("ACL & security", "gsec absorption D (package rename)",
         1779207138, 1779207631),
    Task("ACL & security", "PermissionEvaluator + RoleHierarchy bean restore",
         1779183888, 1779183920),

    # ----- Maintainability ----------------------------------------------
    Task("Maintainability", "XML -> @Configuration (6 modules)",
         1779157420, 1779159152),
    Task("Maintainability", "AbstractDao idempotent create",
         1779042438, 1779140301),
    Task("Maintainability", "@Ignore'd test triage",
         1779068735, 1779223154),
    Task("Maintainability", "session.refresh edge cases",
         1779082720, 1779168282),
    Task("Maintainability", "Lighthouse N+1 fix (subsets+bioassays)",
         1779149389, 1779149389),
    Task("Maintainability", "ExpressionExperimentServiceImpl decomp (Phase 2)",
         1779159638, 1779179679),
    Task("Maintainability", "Service decomp: 21 Read-service extractions",
         1779177438, 1779233872),

    # ----- Framework bumps ----------------------------------------------
    Task("Framework bumps", "Spring Framework 6.1 -> 6.2",
         1779160643, 1779160643),
    Task("Framework bumps", "Spring Security 6.3 -> 6.5",
         1779160852, 1779160852),
    Task("Framework bumps", "Hibernate 6.4 -> 6.6",
         1779160713, 1779160713),
    Task("Framework bumps", "Spring Boot dep BOM 3.3 -> 3.5",
         1779163259, 1779176971),
    Task("Framework bumps", "HikariCP 5 -> 6",
         1779161216, 1779161216),
    Task("Framework bumps", "Maven plugin modernization",
         1779158872, 1779159202),
    Task("Framework bumps", "Hibernate Search 7 + Lucene 9 restoration",
         1779218964, 1779232469),

    # ----- Cleanups & audits --------------------------------------------
    Task("Cleanups", "Coexpression stub cleanup",
         1779051802, 1779156015),
    Task("Cleanups", "ThreadLocal removal (encoder + provider)",
         1779079365, 1779156465),
    Task("Cleanups", "@CommonsLog -> @Slf4j (188 sites)",
         1779161531, 1779163245),
    Task("Cleanups", "Hibernate envers audit",
         1779161485, 1779168206),
    Task("Cleanups", "Cache modernization (JCache, @Cacheable)",
         1779160931, 1779175408),
    Task("Cleanups", "Metrics profile restore (JCache binder)",
         1779162143, 1779175408),
    Task("Cleanups", "Delete 5 deprecated CLIs",
         1779179980, 1779180030),
    Task("Cleanups", "protobuf-java CVE-2024-7254 pin",
         1779182559, 1779182585),
    Task("Cleanups", "AuditTrail/AuditEvent L2 cache bug fix",
         1779169651, 1779184409),
    Task("Cleanups", "HB6 cascade fixes (EE/DEA DAO remove)",
         1779041591, 1779205064),
    Task("Cleanups", "Static analysis audit",
         1779161473, 1779161473),
    Task("Cleanups", "Validation/AspectJ/Mockito/Spring-profiles audits",
         1779160190, 1779176767),
    Task("Cleanups", "Executor virtual-thread prep",
         1779162181, 1779168882),
    Task("Cleanups", "Cruft inventory audit",
         1779179051, 1779215661),

    # ----- Cloud-ready --------------------------------------------------
    Task("Cloud-ready", "12-factor config (env-var fallback)",
         1779178861, 1779178899),
    Task("Cloud-ready", "Container image (Dockerfile + recce)",
         1779179558, 1779179639),
    Task("Cloud-ready", "Structured logging (JSON + MDC)",
         1779179520, 1779204558),

    # ----- AI-driven / workflow -----------------------------------------
    Task("AI / workflow", "@Audited annotation foundation (Phase A)",
         1779180298, 1779202954),
    Task("AI / workflow", "WhatsNew typed-event refactor",
         1779183841, 1779183841),
    Task("AI / workflow", "Ticket entity + DAO + read REST",
         1779204566, 1779212684),
    Task("AI / workflow", "Ticket write-side REST (POST/PUT/DELETE)",
         1779208575, 1779211651),
    Task("AI / workflow", "External pipeline handoff (recce)",
         1779206663, 1779206931),
    Task("AI / workflow", "Spring Modulith feasibility (recce)",
         1779205614, 1779205614),

    # ----- Recces & audits (deliverable docs themselves) -----------------
    Task("Recces & docs", "Hibernate6 cascade audit",
         1779183728, 1779183728),
    Task("Recces & docs", "Hibernate Type audit",
         1779165693, 1779165693),
    Task("Recces & docs", "Framework bump feasibility recce",
         1779160910, 1779160910),
    Task("Recces & docs", "PR / issue triage scoping",
         1779177683, 1779207791),
    Task("Recces & docs", "CI/CD audit",
         1779182559, 1779182559),
    Task("Recces & docs", "Dependency audit (baseCode + Boot)",
         1779165693, 1779235484),

    # ----- Release plan -------------------------------------------------
    Task("Release plan", "Session-close note + release plan",
         1779207791, 1779207791),
    Task("Release plan", "Worktree cleanup plan + script",
         1779173361, 1779174034),
]


# ---------------------------------------------------------------- helpers
def _bar_span(t: Task) -> tuple[float, float]:
    """Return (start_day, end_day), clipped so start >= 0 and end <= NOW."""
    s = ts_to_day(t.first_ts)
    e = ts_to_day(t.last_ts)
    if s < 0.0:
        s = 0.0
    if e is None or e > NOW_DAY:
        e = NOW_DAY
    if e < s:
        e = s
    return s, e


# ---------------------------------------------------------------- render
def render() -> None:
    apply_rcparams()

    n = len(TASKS)
    # Compact: tighter row spacing than v7 (was 0.24 d/row)
    fig_h = max(6.0, 0.20 * n + 2.0)
    fig, ax = plt.subplots(figsize=(10.5, fig_h))

    bar_h = 0.62

    # Plot bottom-up so first TASK sits at the TOP of the chart.
    rev = list(reversed(TASKS))
    for i, t in enumerate(rev):
        y = i
        s, e = _bar_span(t)
        w = max(e - s, 0.0)
        # Min visible width for instant / single-commit done items
        if w < 0.04:
            w = 0.04

        ax.barh(y, w, left=s, height=bar_h,
                color=ACCENT_DONE, edgecolor="none",
                alpha=1.0, zorder=2.5)

    # ---- y-axis labels ----
    labels = [t.label for t in rev]
    ax.set_yticks(range(n))
    ax.set_yticklabels(labels, fontsize=8.0, color=TEXT)
    ax.tick_params(axis="y", length=0, pad=4)

    # ---- category band shading + right-margin category label ----
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
            ax.axhspan(lo - 0.5, hi + 0.5, color=BAND, zorder=1)
        # right-side label
        ax.text((NOW_DAY + 0.06), (lo + hi) / 2.0, cat,
                fontsize=9, color=SUBTLE, va="center", ha="left",
                fontweight="normal")

    # ---- x-axis: fractional-day ticks, 0 -> now ----
    xmin = 0.0
    xmax = NOW_DAY + 0.30
    ax.set_xlim(xmin, xmax)
    ax.set_ylim(-0.5, n - 0.5)

    # Major ticks at day boundaries; minor at 6h
    day_hi = int(xmax) + 1
    major_ticks = list(range(0, day_hi + 1))
    minor_ticks = [d + h / 24.0 for d in major_ticks
                   for h in (6, 12, 18)]
    ax.set_xticks(major_ticks)
    ax.set_xticklabels([_day_label(d) for d in major_ticks],
                       fontsize=9, color=SUBTLE)
    ax.set_xticks(minor_ticks, minor=True)
    ax.tick_params(axis="x", which="major", length=0, pad=4)
    ax.tick_params(axis="x", which="minor", length=0)

    # y-axis grid off; x-axis grid on at major ticks (light)
    ax.yaxis.grid(False)
    ax.xaxis.grid(True, which="major", color=GRID, linewidth=0.7, zorder=0)
    ax.xaxis.grid(True, which="minor", color=GRID, linewidth=0.3,
                  linestyle=":", zorder=0)
    ax.set_axisbelow(True)

    # ---- "today" reference line ----
    ax.axvline(NOW_DAY, color=SUBTLE, linewidth=1.0, linestyle="--",
               zorder=4)
    ax.text(NOW_DAY + 0.02, n - 0.7,
            f"now ({datetime.now().strftime('%Y-%m-%d %H:%M')})",
            fontsize=8, color=SUBTLE, va="top", ha="left")

    # ---- title + subtitle ----
    fig.suptitle("Phase 3 renovations - done items, day-0 anchored (v8)",
                 x=0.02, y=0.995,
                 ha="left", fontsize=14, fontweight="normal", color=TEXT)
    ax.set_title(f"{n} items shipped between day 0 (2026-05-18) and "
                 f"now. Each bar runs from the first commit on the item "
                 f"to its last commit. Pre-day-0 starts (Phase 2 residual) "
                 f"clipped to 0. No in-flight, queued, or future bars.",
                 fontsize=9, color=SUBTLE, loc="left", pad=10)

    # ---- legend ----
    handles = [
        mpatches.Patch(facecolor=ACCENT_DONE,
                       label=f"Done ({n})"),
    ]
    leg = ax.legend(handles=handles, loc="lower right",
                    bbox_to_anchor=(1.0, -0.045),
                    ncol=1, frameon=False, fontsize=8.5,
                    handlelength=1.4, handleheight=1.0,
                    columnspacing=1.2)
    for txt in leg.get_texts():
        txt.set_color(TEXT)

    # ---- source caption ----
    fig.text(0.02, 0.005,
             "Source: git log --since=2026-05-17 on phase2-acl-migrate; "
             "RENOVATIONS.md; SESSION_CLOSE_NOTE_2026-05-19.md; "
             "memory/project_phase3_progress.md. "
             f"Generated {datetime.now():%Y-%m-%d %H:%M}.",
             fontsize=7.5, color=SUBTLE, ha="left", va="bottom")

    # ---- layout + clipPath strip + save ----
    fig.subplots_adjust(left=0.36, right=0.84, top=0.94, bottom=0.06)

    ax.set_clip_on(False)
    for a in (list(ax.patches) + list(ax.lines) + list(ax.texts)
              + list(ax.collections) + list(ax.images)):
        a.set_clip_on(False)

    stamp = datetime.now().strftime("%Y-%m-%d_%H%M")
    fig_dir = os.path.dirname(os.path.abspath(__file__))
    stamped = f"{fig_dir}/renovations_gantt_{stamp}.svg"
    canonical = f"{fig_dir}/renovations_gantt.svg"
    fig.savefig(stamped, format="svg", bbox_inches="tight", facecolor="white")

    # Strip clipPath wrappers post-write.
    with open(stamped, "r", encoding="utf-8") as fh:
        svg = fh.read()
    svg = re.sub(r' clip-path="url\(#[^"]+\)"', "", svg)
    svg = re.sub(r"<clipPath[^>]*>.*?</clipPath>", "", svg, flags=re.S)
    with open(stamped, "w", encoding="utf-8") as fh:
        fh.write(svg)
    with open(canonical, "w", encoding="utf-8") as fh:
        fh.write(svg)
    print(f"wrote {stamped}")
    print(f"wrote {canonical}")


def _day_label(d: int) -> str:
    """Human-friendly day label for the major tick at integer day d.

    Day 0 = Phase 3 kickoff (2026-05-18); day 1 = 2026-05-19; etc.
    """
    date = datetime.fromtimestamp(DAY0_EPOCH + d * 86400)
    return f"day {d}\n{date.strftime('%Y-%m-%d')}"


if __name__ == "__main__":
    render()
