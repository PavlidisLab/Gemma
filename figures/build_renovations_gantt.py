"""Phase 3 renovations Gantt v7 — final-scope, day-by-day view.

Reframe vs. v1-v6: bars represent the FULL Phase 3 scope ("everything
that ever had to be done") and their length spans real-time
"first day work began" -> "completion day" derived from `git log`
timestamps. Provisional final-day = today. At a glance the chart
answers: how much of the final scope is done by day N, and what's
still outstanding.

Status grammar:
  done       — green; bar spans earliest -> latest commit on the item
  inflight   — amber; bar spans earliest commit -> now (still moving)
  queued     — gray dot at today; not yet started (the "outstanding"
               complement of done + inflight)
  blocked    — red hatch dot; blocked on ops / coordination
  deferred   — dotted dot; intentionally pushed past 2.0

Time axis: fractional days from 2026-05-18 00:00 (Phase 3 vision
commit). Latest commit ~1.75d; today's "now" marker rendered as a
dashed vertical line. Phase 2 prep work (2026-05-17) is shown as
day-0 negative offset for the few items whose roots predate Phase 3.

Sources:
  - git log --since=2026-05-15 across phase2-acl-migrate
  - RENOVATIONS.md, PHASE_2_HANDOFF.md, PHASE_3_VISION.md
  - SESSION_CLOSE_NOTE_2026-05-19.md
  - memory/project_phase3_progress.md
  - in-tree recces (GSEC_*, AUDIT_*, CURSOR_*, SEARCH_*, etc.)

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
from datetime import datetime, timezone

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches


# ---------------------------------------------------------------- palette
ACCENT_DONE     = "#10b981"  # emerald-500
ACCENT_INFLIGHT = "#f59e0b"  # amber-500
ACCENT_BLOCKED  = "#ef4444"  # red-500
GRID            = "#e5e7eb"  # gray-200 (queued / planned fill)
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
# Day-1 epoch = 2026-05-18 00:00 local (Phase 3 vision commit landed at
# 2026-05-18 00:23). Tasks whose first commit predates day 1 (Phase 2
# residual ACL work on 2026-05-17) get negative offsets; the chart
# clips to [-0.5, today + 0.05] so they're visible.
DAY1_EPOCH = datetime(2026, 5, 18, 0, 0, 0).timestamp()


def ts_to_day(ts: float | None) -> float | None:
    if ts is None:
        return None
    return (ts - DAY1_EPOCH) / 86400.0


NOW_TS = time.time()
NOW_DAY = ts_to_day(NOW_TS)


# ---------------------------------------------------------------- data
# (label, category, status, first_ts, last_ts)
#   first_ts / last_ts:  Unix seconds, derived from git log
#   for "queued" / "blocked" / "deferred" items both may be None
#                        (no commit history yet; rendered as a marker at NOW)
#   for "inflight" last_ts is None  (bar spans first_ts -> NOW)
#
# Status legend:
#   "done"     full bar spans first -> last
#   "inflight" full bar spans first -> NOW
#   "queued"   marker at NOW (not yet started)
#   "blocked"  marker at NOW with red edge (blocked on coordination)
#   "deferred" marker at NOW with dotted edge (pushed past 2.0)

@dataclass
class Task:
    category: str
    label: str
    status: str           # done / inflight / queued / blocked / deferred
    first_ts: int | None
    last_ts:  int | None
    note: str = ""


TASKS: list[Task] = [
    # ----- First wave (foundational) ------------------------------------
    Task("First wave", "Flyway schema versioning (H2 + MySQL)",
         "done", 1779146310, 1779154230),
    Task("First wave", "Test-fixture factories (Experiment/BioMaterial/ArrayDesign/Taxon)",
         "done", 1779153137, 1779166909),
    Task("First wave", "Streaming-by-default DAOs",
         "deferred", None, None, "perf-flavor; pushed past 2.0"),

    # ----- ACL & security -----------------------------------------------
    Task("ACL & security", "Phase 2 ACL canonical schema migration",
         "done", 1779092497, 1779147834),
    Task("ACL & security", "ACL listener cutover + AOP unwiring",
         "done", 1779119034, 1779140301),
    Task("ACL & security", "Sid unification (Gemma-owned)",
         "done", 1779115714, 1779184409),
    Task("ACL & security", "javac -parameters (SpEL named bindings)",
         "done", 1779151208, 1779151208),
    Task("ACL & security", "ACL upper->lower prod data migration",
         "done", 1779155126, 1779155126),
    Task("ACL & security", "applicationContext-security.xml -> @Configuration",
         "done", 1779146786, 1779159152),
    Task("ACL & security", "@EnableMethodSecurity migration (legacy stack, by design)",
         "done", 1779146786, 1779159866),
    Task("ACL & security", "AfterInvocation provider migration (Phases A/B/C)",
         "done", 1779159042, 1779168189),
    Task("ACL & security", "AclEntryVoter -> AuthorizationManager wrappers",
         "done", 1779164176, 1779168204),
    Task("ACL & security", "gsec absorption A (copy + drop dep)",
         "done", 1779179186, 1779179186),
    Task("ACL & security", "gsec absorption B (Sid unification)",
         "done", 1779184409, 1779184409),
    Task("ACL & security", "gsec absorption D (package rename)",
         "done", 1779207138, 1779207631),
    Task("ACL & security", "gsec absorption C (drop adapter)",
         "deferred", 1779208231, 1779211134,
         "recce landed; migration deferred to 2.0.x"),
    Task("ACL & security", "gsec HQL deprecation",
         "inflight", 1779157248, None,
         "12 sites inventoried; 5 converted"),
    Task("ACL & security", "PermissionEvaluator + RoleHierarchy bean restore",
         "done", 1779183888, 1779183920),
    Task("ACL & security", "Drop old uppercase ACL tables",
         "blocked", None, None, "needs 1 release of clean writes"),
    Task("ACL & security", "Externalize ACL (OPA / Cedar)",
         "queued", None, None, "post-2.0"),

    # ----- Maintainability ----------------------------------------------
    Task("Maintainability", "XML -> @Configuration (6 modules)",
         "done", 1779157420, 1779159152),
    Task("Maintainability", "AbstractDao idempotent create",
         "done", 1779042438, 1779140301),
    Task("Maintainability", "@Ignore'd test triage",
         "done", 1779068735, 1779223154),
    Task("Maintainability", "session.refresh edge cases",
         "done", 1779082720, 1779168282),
    Task("Maintainability", "Lighthouse N+1 fix (subsets+bioassays)",
         "done", 1779149389, 1779149389),
    Task("Maintainability", "ExpressionExperimentServiceImpl decomp (Phase 2)",
         "done", 1779159638, 1779179679),
    Task("Maintainability", "Service decomp: 21 Read-service extractions",
         "done", 1779177438, 1779233872),
    Task("Maintainability", "persisterHelper retirement (BK + cache lifts)",
         "inflight", 1779046644, None,
         "Caches POJO deleted; 8 entities still on dispatch"),
    Task("Maintainability", "Deprecate ensureInSession / findOrCreate",
         "queued", None, None),

    # ----- Framework bumps ----------------------------------------------
    Task("Framework bumps", "Spring Framework 6.1 -> 6.2",
         "done", 1779160643, 1779160643),
    Task("Framework bumps", "Spring Security 6.3 -> 6.5",
         "done", 1779160852, 1779160852),
    Task("Framework bumps", "Hibernate 6.4 -> 6.6",
         "done", 1779160713, 1779160713),
    Task("Framework bumps", "Spring Boot dep BOM 3.3 -> 3.5",
         "done", 1779163259, 1779176971),
    Task("Framework bumps", "HikariCP 5 -> 6",
         "done", 1779161216, 1779161216),
    Task("Framework bumps", "Java 21 readiness (pre-bumps)",
         "inflight", 1779160572, None, "still on JDK 17"),
    Task("Framework bumps", "Maven plugin modernization",
         "done", 1779158872, 1779159202),
    Task("Framework bumps", "JUnit 5 migration (per-class)",
         "inflight", 1779159059, None, "100+ classes migrated, Vintage still in tree"),
    Task("Framework bumps", "JUnit 5 BaseTest hierarchy migration",
         "inflight", 1779164087, None, "BaseTest5 / Database5 / SpringContext5 / Integration5"),
    Task("Framework bumps", "Hibernate Search 7 + Lucene 9 restoration",
         "done", 1779218964, 1779232469),

    # ----- Cleanups & audits --------------------------------------------
    Task("Cleanups", "Coexpression stub cleanup",
         "done", 1779051802, 1779156015),
    Task("Cleanups", "ThreadLocal removal (encoder + provider)",
         "done", 1779079365, 1779156465),
    Task("Cleanups", "@CommonsLog -> @Slf4j (188 sites)",
         "done", 1779161531, 1779163245),
    Task("Cleanups", "Hibernate envers audit",
         "done", 1779161485, 1779168206),
    Task("Cleanups", "RestTemplate -> RestClient",
         "inflight", 1779160031, None, "GoogleAnalytics4Provider done; rest audited"),
    Task("Cleanups", "Lombok cleanup (records / @SneakyThrows / @Value)",
         "inflight", 1779160126, None, "50+ VOs across 6 batches"),
    Task("Cleanups", "Cache modernization (JCache, @Cacheable)",
         "done", 1779160931, 1779175408),
    Task("Cleanups", "Metrics profile restore (JCache binder)",
         "done", 1779162143, 1779175408),
    Task("Cleanups", "Delete 5 deprecated CLIs",
         "done", 1779179980, 1779180030),
    Task("Cleanups", "protobuf-java CVE-2024-7254 pin",
         "done", 1779182559, 1779182585),
    Task("Cleanups", "AuditTrail/AuditEvent L2 cache bug fix",
         "done", 1779169651, 1779184409),
    Task("Cleanups", "HB6 cascade fixes (EE/DEA DAO remove)",
         "done", 1779041591, 1779205064),
    Task("Cleanups", "Static analysis audit",
         "done", 1779161473, 1779161473),
    Task("Cleanups", "Validation/AspectJ/Mockito/Spring-profiles audits",
         "done", 1779160190, 1779176767),
    Task("Cleanups", "Executor virtual-thread prep",
         "done", 1779162181, 1779168882),
    Task("Cleanups", "Cruft inventory audit",
         "done", 1779179051, 1779215661),
    Task("Cleanups", "baseCode in-tree port (ontology + utils + Lucene9)",
         "inflight", 1779218964, None, "3 substantial subsystems remain"),

    # ----- Search subsystem ---------------------------------------------
    Task("Search", "Ontology search restoration (Jena 4 + in-mem Lucene 9)",
         "inflight", 1779157195, None, "findTerm restored; reindex IT landed"),

    # ----- Cloud-ready --------------------------------------------------
    Task("Cloud-ready", "gemma-rest standalone packaging (WAR profile)",
         "inflight", 1779159700, None, "WAR builds; 8 gaps before prod"),
    Task("Cloud-ready", "12-factor config (env-var fallback)",
         "done", 1779178861, 1779178899),
    Task("Cloud-ready", "Container image (Dockerfile + recce)",
         "done", 1779179558, 1779179639),
    Task("Cloud-ready", "Structured logging (JSON + MDC)",
         "done", 1779179520, 1779204558),
    Task("Cloud-ready", "OpenTelemetry traces",
         "queued", 1779179520, 1779204591, "recce only"),
    Task("Cloud-ready", "Object storage abstraction (S3/GCS)",
         "queued", 1779204354, 1779204381, "recce only"),
    Task("Cloud-ready", "Container deploy validation",
         "queued", None, None, "8 gaps before prod"),

    # ----- API / cursors / UI -------------------------------------------
    Task("API / UI", "Cursor-based pagination (REST endpoints)",
         "inflight", 1779204814, None, "step 1a-1n: 15 endpoints converted"),
    Task("API / UI", "Retire gemma-web (gemma-curation-ui replacement)",
         "queued", 1779205552, None, "planning only"),
    Task("API / UI", "Selective field projection / GraphQL",
         "queued", None, None, "post-2.0"),

    # ----- AI-driven / workflow -----------------------------------------
    Task("AI / workflow", "@Audited annotation foundation (Phase A)",
         "done", 1779180298, 1779202954),
    Task("AI / workflow", "Audit migration Phase B (callers)",
         "inflight", 1779206172, None, "7/76 callers; ~70 deferred-shape"),
    Task("AI / workflow", "Audit migration Phase C (listener-based)",
         "queued", 1779208231, 1779211687, "recce landed; blocked on listener design"),
    Task("AI / workflow", "WhatsNew typed-event refactor",
         "done", 1779183841, 1779183841),
    Task("AI / workflow", "Ticket entity + DAO + read REST",
         "done", 1779204566, 1779212684),
    Task("AI / workflow", "Ticket write-side REST (POST/PUT/DELETE)",
         "done", 1779208575, 1779211651),
    Task("AI / workflow", "CurationDetailsService -> Tickets shim",
         "inflight", 1779205743, None, "3-5 callers to migrate"),
    Task("AI / workflow", "External pipeline handoff (recce)",
         "done", 1779206663, 1779206931),
    Task("AI / workflow", "Spring Modulith feasibility (recce)",
         "done", 1779205614, 1779205614),
    Task("AI / workflow", "Vector store + embeddings (pgvector)",
         "queued", None, None, "post-2.0"),
    Task("AI / workflow", "LLM-friendly API surface",
         "queued", None, None, "post-2.0"),
    Task("AI / workflow", "Promote gemma-curation-agents in-tree",
         "queued", None, None, "post-2.0"),

    # ----- Recces & audits (deliverable docs themselves) -----------------
    Task("Recces & docs", "Hibernate6 cascade audit",
         "done", 1779183728, 1779183728),
    Task("Recces & docs", "Hibernate Type audit",
         "done", 1779165693, 1779165693),
    Task("Recces & docs", "Framework bump feasibility recce",
         "done", 1779160910, 1779160910),
    Task("Recces & docs", "PR / issue triage scoping",
         "done", 1779177683, 1779207791),
    Task("Recces & docs", "CI/CD audit",
         "done", 1779182559, 1779182559),
    Task("Recces & docs", "Dependency audit (baseCode + Boot)",
         "done", 1779165693, 1779235484),

    # ----- Release plan (three gates) ------------------------------------
    Task("Release plan", "Session-close note + release plan",
         "done", 1779207791, 1779207791),
    Task("Release plan", "Worktree cleanup plan + script",
         "done", 1779173361, 1779174034),
    Task("Release plan", "Gantt charts (v1..v7)",
         "inflight", 1779172034, None),
    Task("Release plan", "Full mvn verify against gemdtest",
         "queued", None, None, "gates 2.0 release"),
    Task("Release plan", "Gate 1: hotfix-1.32.7 -> 1.32.7 minor",
         "queued", None, None, "ancestor of phase2-acl-migrate"),
    Task("Release plan", "Gate 2: catch-up merge dev -> phase2-acl-migrate",
         "queued", None, None),
    Task("Release plan", "Gate 3: phase2-acl-migrate -> 2.0 release",
         "queued", None, None, "version bump precedes"),
    Task("Release plan", "PR / issue triage passes 1+2+3",
         "queued", None, None),
]


# ---------------------------------------------------------------- helpers
def _status_color(status: str) -> str:
    return {
        "done":     ACCENT_DONE,
        "inflight": ACCENT_INFLIGHT,
        "queued":   GRID,
        "blocked":  GRID,
        "deferred": GRID,
    }[status]


def _bar_span(t: Task) -> tuple[float, float]:
    """Return (start_day, end_day) for plotting."""
    s = ts_to_day(t.first_ts) if t.first_ts is not None else NOW_DAY
    if t.status == "inflight":
        e = NOW_DAY
    elif t.status in ("done", "deferred", "queued"):
        if t.last_ts is not None:
            e = ts_to_day(t.last_ts)
        else:
            e = NOW_DAY
    else:  # blocked
        e = NOW_DAY
    if s is None:
        s = NOW_DAY
    if e is None:
        e = NOW_DAY
    return s, e


# ---------------------------------------------------------------- render
def render() -> None:
    apply_rcparams()

    n = len(TASKS)
    fig_h = max(8.0, 0.24 * n + 2.5)
    fig, ax = plt.subplots(figsize=(10.5, fig_h))

    bar_h = 0.62

    # Plot bottom-up so first TASK sits at the TOP of the chart.
    rev = list(reversed(TASKS))
    for i, t in enumerate(rev):
        y = i
        s, e = _bar_span(t)
        # Make zero-width bars visible: render as small marker
        w = max(e - s, 0.0)
        if w < 0.04 and t.status not in ("queued", "blocked", "deferred"):
            w = 0.04  # min visible width for a tiny single-commit task

        color = _status_color(t.status)

        if t.status == "queued":
            # gray pill marker at NOW (or at last_ts if scoped recce exists)
            # If there's a recce window (first/last_ts not None), show that window
            # as a faint gray bar with hatch to indicate "scoping only, not work".
            if t.first_ts is not None and t.last_ts is not None:
                ax.barh(y, max(ts_to_day(t.last_ts) - ts_to_day(t.first_ts), 0.04),
                        left=ts_to_day(t.first_ts), height=bar_h,
                        facecolor=GRID, edgecolor=SUBTLE, linewidth=0.6,
                        hatch="..", zorder=2)
            else:
                # zero-width pill at "today"
                ax.barh(y, 0.06, left=NOW_DAY - 0.03, height=bar_h * 0.55,
                        facecolor=GRID, edgecolor=SUBTLE, linewidth=0.6,
                        zorder=2)
        elif t.status == "blocked":
            ax.barh(y, 0.08, left=NOW_DAY - 0.04, height=bar_h * 0.55,
                    facecolor=GRID, edgecolor=ACCENT_BLOCKED, linewidth=1.2,
                    hatch="//", zorder=2)
        elif t.status == "deferred":
            # show recce window if it exists; else dotted pill at today
            if t.first_ts is not None and t.last_ts is not None:
                ax.barh(y, max(ts_to_day(t.last_ts) - ts_to_day(t.first_ts), 0.04),
                        left=ts_to_day(t.first_ts), height=bar_h,
                        facecolor=GRID, edgecolor=SUBTLE, linewidth=0.6,
                        hatch="..", zorder=2)
            else:
                ax.barh(y, 0.06, left=NOW_DAY - 0.03, height=bar_h * 0.55,
                        facecolor=GRID, edgecolor=SUBTLE, linewidth=0.8,
                        linestyle=":", zorder=2)
        else:
            # done / inflight: solid filled bar across [s, e]
            alpha = 1.0 if t.status == "done" else 0.7
            ax.barh(y, w, left=s, height=bar_h,
                    color=color, edgecolor="none",
                    alpha=alpha, zorder=2.5)

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
        ax.text((NOW_DAY + 0.10), (lo + hi) / 2.0, cat,
                fontsize=9, color=SUBTLE, va="center", ha="left",
                fontweight="normal")

    # ---- x-axis: fractional-day ticks ----
    # Spans roughly [-0.5, NOW_DAY + 0.25]; tick every 6h on day 1+
    xmin = -0.6
    xmax = NOW_DAY + 0.55
    ax.set_xlim(xmin, xmax)
    ax.set_ylim(-0.5, n - 0.5)

    # Major ticks at day boundaries; minor at 6h
    day_lo = int(xmin) - 1
    day_hi = int(xmax) + 1
    major_ticks = list(range(day_lo, day_hi + 1))
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

    # phase-3 vision-commit reference
    p3_start_day = ts_to_day(1778996277)  # 2026-05-18 00:23:47
    ax.axvline(p3_start_day, color=SUBTLE, linewidth=0.7,
               linestyle=":", zorder=4)
    ax.text(p3_start_day - 0.02, n - 0.7, "Phase 3 begins",
            fontsize=7.5, color=SUBTLE, va="top", ha="right")

    # ---- title + subtitle ----
    fig.suptitle("Phase 3 renovations - final-scope view (v7)",
                 x=0.02, y=0.995,
                 ha="left", fontsize=14, fontweight="normal", color=TEXT)
    ax.set_title("Each row is part of the full Phase 3 plate. Bar length = "
                 "real-time span from first commit on the item to its last "
                 "commit (done) or to 'now' (in flight). Queued rows are the "
                 "outstanding scope. Today is the provisional final.",
                 fontsize=9, color=SUBTLE, loc="left", pad=10)

    # ---- legend ----
    done_n     = sum(1 for t in TASKS if t.status == "done")
    inflight_n = sum(1 for t in TASKS if t.status == "inflight")
    queued_n   = sum(1 for t in TASKS if t.status == "queued")
    blocked_n  = sum(1 for t in TASKS if t.status == "blocked")
    deferred_n = sum(1 for t in TASKS if t.status == "deferred")

    handles = [
        mpatches.Patch(facecolor=ACCENT_DONE,
                       label=f"Done ({done_n})"),
        mpatches.Patch(facecolor=ACCENT_INFLIGHT, alpha=0.7,
                       label=f"In flight ({inflight_n})"),
        mpatches.Patch(facecolor=GRID, edgecolor=SUBTLE, linewidth=0.6,
                       label=f"Queued ({queued_n})"),
        mpatches.Patch(facecolor=GRID, edgecolor=ACCENT_BLOCKED,
                       hatch="//", linewidth=1.2,
                       label=f"Blocked ({blocked_n})"),
        mpatches.Patch(facecolor=GRID, edgecolor=SUBTLE, hatch="..",
                       linewidth=0.6,
                       label=f"Deferred ({deferred_n})"),
    ]
    leg = ax.legend(handles=handles, loc="lower right",
                    bbox_to_anchor=(1.0, -0.045),
                    ncol=5, frameon=False, fontsize=8.5,
                    handlelength=1.4, handleheight=1.0,
                    columnspacing=1.2)
    for txt in leg.get_texts():
        txt.set_color(TEXT)

    # ---- source caption ----
    fig.text(0.02, 0.005,
             "Source: git log --since=2026-05-15 on phase2-acl-migrate; "
             "RENOVATIONS.md; SESSION_CLOSE_NOTE_2026-05-19.md; "
             "memory/project_phase3_progress.md. "
             f"Generated {datetime.now():%Y-%m-%d %H:%M}.",
             fontsize=7.5, color=SUBTLE, ha="left", va="bottom")

    # ---- layout + clipPath strip + save ----
    fig.subplots_adjust(left=0.36, right=0.82, top=0.95, bottom=0.05)

    ax.set_clip_on(False)
    for a in (list(ax.patches) + list(ax.lines) + list(ax.texts)
              + list(ax.collections) + list(ax.images)):
        a.set_clip_on(False)

    stamp = datetime.now().strftime("%Y-%m-%d_%H%M")
    fig_dir = os.path.dirname(os.path.abspath(__file__))
    stamped = f"{fig_dir}/renovations_gantt_{stamp}.svg"
    canonical = f"{fig_dir}/renovations_gantt.svg"
    fig.savefig(stamped, format="svg", bbox_inches="tight", facecolor="white")

    # Strip clipPath wrappers post-write (matplotlib emits them even
    # when set_clip_on(False)). Illustrator's Tiny SVG drops them with
    # a warning anyway.
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

    Day 1 = Phase 3 kickoff (2026-05-18); day 2 = 2026-05-19; etc.
    Negative days are Phase 2 residual (2026-05-17 = day 0).
    """
    if d == 0:
        return "day 0\n2026-05-17"
    if d >= 1:
        date = datetime.fromtimestamp(DAY1_EPOCH + (d - 1) * 86400)
        return f"day {d}\n{date.strftime('%Y-%m-%d')}"
    # negative day
    date = datetime.fromtimestamp(DAY1_EPOCH + d * 86400)
    return f"day {d}\n{date.strftime('%Y-%m-%d')}"


if __name__ == "__main__":
    render()
