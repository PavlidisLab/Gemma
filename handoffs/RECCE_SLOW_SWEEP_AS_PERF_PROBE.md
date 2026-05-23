# Recce — slow-tagged tests as a perf probe (NOT a retag exercise)

**Filed:** 2026-05-23, mid-session.
**Status:** Brief for the next agent; supersedes the inventory-and-retag framing of pending tasks #89 / #96.
**Owner:** Whoever picks up the slow-sweep follow-up.

## Paul's redirect (verbatim 2026-05-23)

> "the slow tests are good as they are integration tests; watch for performance bottlenecks that are due to code (fix them) or to the fixture being massive (mock/chop)"

The 77 `@Tag("slow")` classes are not candidates for tag removal or `default-run` promotion. They're valuable integration coverage that stays slow-tagged because each one takes seconds-to-minutes. The **slowness itself is the signal** — for each slow test, ask:

1. **Is the slowness driven by a code-side bottleneck?** N+1 storms, missing `JOIN FETCH`, eager load over a large association, blocking I/O on a hot path, a `LazyInitializationException`-induced retry loop. If yes → **fix the code** (mirrors the perf-wave pattern from this phase). The test is the regression guard.
2. **Is the slowness driven by a massive fixture?** A 500 MB GEO archive, a full Uberon OWL, an h5ad with 10⁵ cells. If yes → **mock the loader OR chop the fixture** to the minimum subset that exercises the code path. Per the `feedback_fast_tests_playbook` diagnostic ladder (ROBOT for OWL trims, `anndata` for h5ad, MEX row-trim, etc.).
3. **Neither — genuinely heavy compute?** Real ANOVA over a real-world matrix, real BLAT alignment. These earn their `slow` tag honestly. Document the timing baseline so future regressions are detectable.

## Inputs the next agent has

- `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md` (Agent F, this session) — 77-class inventory with per-class scope (class-level vs method-level slow tag, network-bound or not, what each class exercises). This is the **what**.
- The slow-sweep mvn run that's about to finish (or has finished) — **per-class elapsed time**, **fail/hang status**. This is the **how-slow**. Pull timings out of `/tmp/slow-sweep.log` via `grep -E "Tests run:.*Time elapsed:" /tmp/slow-sweep.log | awk -F'Time elapsed: ' '{print $2}' | sort -rn`. (Or read the surefire/failsafe XML reports directly under `gemma-core/target/{surefire,failsafe}-reports/` in the slow-sweep worktree.)
- This document — the **why** (perf-probe lens, not retag lens).

## Triage procedure

For each slow class, in descending order of measured elapsed time:

1. **Profile the test once** if it's > 60 s. Run with `-XX:+UnlockDiagnosticVMOptions -XX:StartFlightRecording=duration=60s,filename=/tmp/slow-<class>.jfr` or just instrument the suspected hot path with a `StopWatch` + log line. Look for the dominant cost.
2. **Classify**:
   - **Code bottleneck** (N+1, eager-load storm, lazy-init retry): file a focused fix in a separate commit. Validate the test still passes + measure the new elapsed time. Don't bundle the fix with retagging.
   - **Massive fixture** (download/parse/load dominates the wall clock): use the playbook ladder to chop / mock. Validate.
   - **Honest heavy compute**: write a one-line baseline assertion `assert elapsed < 120s` (or whatever) so future regressions trip the build. Don't change the tag.
3. **Don't promote slow tests to default-run.** Even after a fix takes them from 60 s to 5 s, they stay `@Tag("slow")` because the default-`mvn verify` budget is precious. The win is the production-code fix that the slow test was hiding, not the test itself.

## Don'ts

- Don't remove `@Tag("slow")` from any class.
- Don't bundle multiple per-class fixes into one commit — each perf fix earns its own message + before/after timing.
- Don't propose ContentX flag-flips or "just give it more memory" — the goal is finding real bottlenecks, not papering them over.
- Don't touch `DatasetCombinerTest` (already exempt per #98).

## Cross-references

- `feedback_fast_tests_playbook.md` (user memory) — diagnostic ladder for trimming fixtures.
- `project_perf_hotspot_priorities.md` (user memory) — the 4 hotspots the perf wave already addressed; new slow-sweep findings shouldn't re-recce these.
- `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md` — 77-class inventory.
- `handoffs/STATUS_SESSION_SNAPSHOT_2026_05_23.md` — current tip + the failsafe baseline this work runs against (376/0F+0E+9S, post task #63).
