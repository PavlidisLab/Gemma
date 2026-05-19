# Figure captions

## renovations_gantt.svg (v6 — 2026-05-19_1027)

**What it is.** Phase 3 renovations Gantt updated for the 2026-05-19
mid-day session close. Same encoding as the earlier snapshots; today
moved from S2 to S6 to reflect five additional half-day sessions
since the v1 chart.

**How to read it.** Identical encoding to the earlier snapshots:
rows grouped by renovation category, x-axis in agent-session units
S0 through S10+, dashed line at S6 = today. Emerald is shipped,
amber is in flight, gray is planned, red-hatch blocked on ops,
dotted deferred. A new **Release plan** category at the bottom
tracks the three-gate path to Gemma 2.0 (1.32.7 minor first,
catch-up merge dev -> phase2-acl-migrate, then 2.0 release).

**What it shows.** Net changes since `renovations_gantt_2026-05-19_0745.svg`:
gsec absorption Phase D landed (emerald); gsec Phase C marked
deferred (dotted) — recce done, migration pushed to 2.0.x.
Ticket/workflow layer (read + write REST) flipped to done.
Audit-workflow Phase B fully amber-to-emerald progressed
(B-1/B-2/B-3 all landed). JUnit 5 per-class migration ticked
forward (125+ classes off Vintage); JUnit 5 BaseTest hierarchy
migration added as a new amber row (the next unlock, in flight on
a parallel agent branch). Lombok cleanup advanced to 50 VOs.
Three new "in flight on parallel agents" rows for audit-Phase-C
recce, CurationDetailsService write-method deprecation, and the
10th service decomp (CharacteristicReadService).

## renovations_gantt.svg

**What it is.** Horizontal Gantt of the Phase 3 renovations plan from
`PHASE_3_VISION.md`, with progress marked against each item as of the
end of the 2026-05-18 session.

**How to read it.** One row per planned item, grouped by category
(first wave / ACL & security / maintainability / framework bumps /
cleanups / cloud-ready / mobile-friendly / AI-driven). Each row's
horizontal bar is the planned span in agent-session units (S0
through S10+). Emerald is the shipped portion, amber is in flight,
gray-200 is planned-but-not-started, red-hatch is blocked on ops,
dotted is deliberately deferred. Dashed vertical line marks "today"
(end of S2 = 2026-05-18 evening).

**What it shows.** The session converted most ACL/security, framework
bumps, and cleanups items to done; first-wave Flyway and test-fixture
factories are partway through; streaming-by-default DAOs sit deferred
by user direction (perf-flavor); persisterHelper retirement and
gemma-rest standalone are on multi-session arcs; cloud-ready,
mobile-friendly, and AI-driven dimensions remain unbroken ground.

## renovations_gantt_2026-05-18_2132.svg

**What it is.** Updated snapshot of the Phase 3 renovations Gantt
taken 2026-05-18 at 21:32, after a late-evening parallel wave of
agent work closed a large batch of items that the first chart had
marked in flight. 63 task bars (vs. 43 in the first chart). Same
style + colour grammar as `renovations_gantt.svg`.

**How to read it.** Identical encoding to the first chart: rows
grouped by renovation category, x-axis in agent-session units S0
through S10+, dashed line at S2 = today (21:32 stamp). Emerald is
shipped, amber is in flight, gray is planned, red-hatch blocked on
ops, dotted deferred. New bars surfaced this session (e.g. ACL voter
phases X.1-X.4, AfterInvocation Phase C, GenomePersister chunks
5.4-5.5, JUnit 5 B0/B1+, Spring Security 7 readiness, gemma-rest
standalone phases, retire gemma-web, Spring Boot BOM-only, gsec
alignment, several cleanups split out as individual bars) appear
slotted into their categories.

**What it shows.** Status totals: 31 done (vs. 19), 15 in flight
(vs. 10), 15 planned (vs. 12), 1 blocked, 1 deferred. Cleanups and
framework bumps are now almost entirely emerald; ACL & security has
flipped from "mostly first-wave done" to "AfterInvocation +
AclEntryVoter modernization mid-stream"; maintainability is the
broadest in-flight surface (persisterHelper retirement on six rows,
EE service decomp on two); cloud-ready / mobile-friendly /
AI-driven remain mostly gray, but gemma-rest Phase 1 and the
gemma-web retirement plan have first emerald slices.

## renovations_gantt_2026-05-18_2138.svg

**What it is.** The same Phase 3 renovations Gantt as the two
earlier snapshots, but with the v1 (original) status overlaid on
the now (current) status so the visual delta between the two is
the headline. Built because the v1 and v2 charts looked nearly
identical at a glance even though 22 rows had actually changed
state between them.

**How to read it.** Each task row is split into two horizontally-
stacked bars in the same time-axis slot: pale top half = status in
the original `renovations_gantt.svg`, solid bottom half = status
now. Pale tints use the Tailwind 200-level palette (emerald-200
done-ghost, amber-200 inflight-ghost, gray-100 absent-ghost) so
they sit visually quiet. The solid bottom half uses the
established 500-level palette and full grammar (emerald done,
amber inflight, gray-200 planned, red-hatch blocked, dotted
deferred). Rows where v1 != now also have **bold** y-axis tick
labels as a second cue. Dashed line at S2 = today (21:38).

**What it shows.** 22 of 63 rows show a visible delta -- mostly
amber-200 -> emerald (work that closed since the first chart)
plus several gray-100 -> emerald / amber rows (new work that
wasn't on the v1 plan at all, including AclEntryVoter recce,
Spring Security 7 readiness recce, gsec alignment, Maven release
plugin recce, Validation/AspectJ/Mockito audits, VT executor
caller migration, Hibernate L2 cache region audit). The 41
unchanged rows mostly are: foundational items finished before v1
(emerald/emerald), the always-planned cloud/mobile/AI columns
(gray/gray), and the persistent in-flights (amber/amber:
persisterHelper retirement, 12-factor config, RestTemplate
migration). The visual signal you want -- "did anything actually
move?" -- is now the dominant feature of the chart.
