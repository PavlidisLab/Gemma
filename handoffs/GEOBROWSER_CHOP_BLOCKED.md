# GeoBrowserTest chop — BLOCKED

Date: 2026-05-23
Worktree: `.claude/worktrees/agent-geobrowser-fixture-chop`
Baseline tip: `7e18daa1f5`
Brief: per `handoffs/SLOW_SURVIVORS_PERF_PROBE_2026_05_23.md` row #2

## TL;DR

The "FIXTURE BLOAT (cached MINiML XML)" classification in the perf-probe
doc is **incorrect** for `GeoBrowserTest`. **There is no cached MINiML
fixture for GSE97948 or GSE8579** anywhere in `gemma-core/src/test/resources`.

The 75 s + 52 s wall-clock for `testGSE97948` and `testGSE8579` is real
HTTPS network I/O against GEO — `GeoBrowserImpl.fetchDetailedGeoSeriesFamilyFromGeoFtp(...)`
calls `GeoUtils.getUrl( geoAccession, GeoSource.FTP_VIA_HTTPS, ... ).openStream()`
directly with no classpath cache lookup or fallback.

No file to chop. No knob to turn that wouldn't change semantics of the
test (it exists to exercise the real GEO MINiML parser against pathological
real-world payloads: GSE97948 = ~1000 samples with hundreds of
characteristics each, GSE8579 = invalid 1-byte UTF-8 sequence 0x1b).

## Evidence

1. **No fixture files present.** Filesystem scan of test resources:
   ```bash
   find gemma-core/src/test/resources -iname "*97948*" -o -iname "*8579*"
   # → no matches
   grep -rln "GSE97948\|GSE8579" gemma-core/src/test/resources
   # → no matches
   ```
   The only cached GEO MINiML fixtures are `GSE730_family.xml`,
   `GSE180363.miniml.xml`, `GSM5230452.xml`, `GSE171682.xml` — none of
   them are referenced by the two slow methods.

2. **Production code has no fixture-preferring path.**
   `GeoBrowserImpl.fetchDetailedGeoSeriesFamilyFromGeoFtp` (line 700) and
   `fetchDetailedGeoSeriesFamilyFromGeoQuery` (line 729) both open
   network URLs directly. There is no `classpath:` resolution path the
   test could prime.

3. **Test class is network-gated.** Class-level
   `@NetworkAvailable(url = EntrezUtils.ESUMMARY)` + the methods
   carrying `@Tag("slow")` (which default excludes) mean these tests
   are skipped under default `mvn verify` (default
   `excludedGroups=network,slow`). The 159 s figure only materialises
   under the probe's explicit `-DexcludedGroups=network` (drop slow
   exclusion).

## Why I didn't chop anyway

The brief allows reasonable scope creep. Options I considered and
rejected:

- **Cache the live MINiML to a fixture, then chop the cache.** Would
  require (a) downloading the full GSE97948 payload once to capture, (b)
  adding a classpath-preferring branch to `GeoBrowserImpl` (production
  code change for a test). This is feature work, not the
  fixture-chop pattern the brief calls for. It would also weaken the
  test — these methods specifically exist as regression guards for
  "what happens when GEO ships a 1000-sample MINiML" and "what happens
  when GEO ships invalid UTF-8" — a chopped fixture wouldn't reproduce
  the 1000-sample memory/parse pressure. The 0x1b UTF-8 byte from
  GSE8579 could be cached cleanly (it's small surface), but that's a
  Windows-1252-fallback path test and chopping it doesn't shave wall-clock
  if the goal is just to keep coverage.
- **`@Disable` or retag.** Already correctly `@Tag("slow")`. The slow
  exclusion already keeps them out of default verify; the wall-clock
  is only paid by deliberate `-DexcludedGroups=network` runs. No
  retagging gain available.
- **Convert to a unit test against a mocked URL stream.** Not the
  brief's mandate, would duplicate `EntrezXmlUtils` unit coverage, and
  again loses the integration value of exercising the real GEO byte
  stream.

## Recommendation

Drop `GeoBrowserTest` from the FIXTURE BLOAT action queue. The 159 s
class is already correctly partitioned out of default `mvn verify` via
`@Tag("slow")` + `@NetworkAvailable`. The cost is only paid when a
maintainer explicitly opts in to the slow + network path, which is the
right tradeoff for "regression guard against real GEO pathology."

If pressed to reduce slow-tag wall-clock further:

1. **Profile-driven choice.** Capture the wire-level payload size for
   GSE97948 and confirm whether parse-time or download-time dominates.
   `curl -o /dev/null -w '%{time_total} %{size_download}\n' "$(java -cp ... GeoUtils ...)"`.
2. **If download dominates:** cache the MINiML once as a binary fixture
   and add a classpath-prefer branch to `GeoBrowserImpl` gated on a
   `gemma.geo.test.fixturePath` system property. Chop the cached copy
   to first-N samples. Preserve the network variant under
   `@Tag("network")` to keep the URL-drift signal.
3. **If parse dominates:** this is a real perf bug in the MINiML
   parser, not a fixture problem. Open a separate issue.

The probe doc's other rows (MEX, GeoSingleCellDetector SOFT,
GeoMexSingleCellDataLoaderConfigurer) likely DO have cached fixtures
to chop — those classifications should be re-verified before launching
chop agents, the same way I had to verify this one.

## Files touched

None (no commit produced).

## Anything else noticed

The perf-probe doc references "cached classpath SOFT" in the
GeoBrowserTest row, but `GeoBrowserImpl` doesn't parse SOFT — only
MINiML. That phrasing was a copy-paste from the
`GeoSingleCellDetectorTest` row (which DOES use SOFT). Worth a typo
fix on next probe-doc revision.
