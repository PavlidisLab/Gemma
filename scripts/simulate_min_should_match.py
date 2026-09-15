#!/usr/bin/env python3
"""Simulate a minimum-should-match constraint on /annotations/search, offline.

Why this exists
---------------
`LuceneOntologySearchIndex` builds its query with `new QueryParser(...)`, whose default
operator is OR. A three-token query therefore matches any term sharing ONE token, so
"Gorlin Goltz Syndrome" returns twenty HP terms that contain only "syndrome". Two
consequences:

  * those twenty fill `maxResults` in the conventional pool, and the supplementary tier's
    quota is `max(maxResults - conventional, 0)` = 0, so MeSH synonyms contribute nothing;
  * the exact match is displaced by partial ones, which is backwards.

Requiring a query to cover N of its tokens would fix both. That is a server change, so the
"after" arm cannot be measured without deploying it. What CAN be measured read-only is the
RISK side: how much gold ranking a threshold would destroy. This script does that by
re-ranking the candidates the live server already returns.

🛑 Read the asymmetry before trusting a number here. Filtering a returned list can only
REMOVE hits, never add the ones the server never sent. So:

  * the recall/MRR deltas below are a faithful measure of the COST of a threshold;
  * the BENEFIT is systematically under-measured — every MeSH hit currently starved by the
    quota is invisible here, because it is not in the response to filter.

Treat a threshold that costs ~nothing as safe to try, not as proven to help. The benefit
arm needs the change deployed.

Usage
-----
    scripts/simulate_min_should_match.py <fold.json> [--limit 100] [--n 60] [--jobs 2]

<fold.json> is a ranker fold from the eval repo (`pairs` of query/category/gold_uri/shape).
Defaults are deliberately gentle: frink is a shared box.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = os.environ.get("GEMMA_BASE_URL", "https://gemma2.msl.ubc.ca").rstrip("/") + "/rest/v2"

# Mirrors gemma-rest's QueryTokens.SEARCH_STOP_WORDS. Kept in sync by eye; if the server
# list grows, a token counted here that the server drops only makes this simulation
# CONSERVATIVE (it would over-count coverage requirements), never optimistic.
STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
    "in", "into", "is", "it", "of", "on", "or", "such", "that", "the",
    "their", "then", "there", "these", "they", "this", "to", "was",
    "will", "with",
}
MIN_CONTENT_TOKEN_LENGTH = 2


def content_tokens(text: str) -> list[str]:
    out, seen = [], set()
    for t in re.split(r"[^a-z0-9]+", (text or "").lower()):
        if len(t) < MIN_CONTENT_TOKEN_LENGTH or t in STOP_WORDS or t in seen:
            continue
        seen.add(t)
        out.append(t)
    return out


def coverage(value: str, tokens: list[str]) -> float:
    """Fraction of query content tokens present in some text."""
    if not tokens:
        return 1.0
    hay = set(content_tokens(value))
    return sum(1 for t in tokens if t in hay) / len(tokens)


def hit_coverage(hit: dict, tokens: list[str]) -> float:
    """
    Best coverage over the hit's label AND the text it actually matched on.

    Mirrors CompositeRankingStrategy:101, which scores
    max(tokenCoverageFraction(value), tokenCoverageFraction(matchedText)). Using the label
    alone is wrong here and not conservatively so: a term that matched through a SYNONYM
    would score 0 coverage and be dropped, which real minimum-should-match — operating on
    the indexed text, synonyms included — would never do. That inflates both the measured
    cost and the measured benefit.
    """
    return max(coverage(hit.get("value") or "", tokens),
               coverage(hit.get("matchedText") or "", tokens))


def search(query: str, limit: int, timeout: int = 60) -> list[dict]:
    url = f"{BASE}/annotations/search?query={urllib.parse.quote(query)}&limit={limit}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.load(r).get("data") or []


def rank_of(hits: list[dict], gold: str) -> int | None:
    for i, h in enumerate(hits):
        if (h.get("valueUri") or "") == gold:
            return i + 1
    return None


def metrics(ranks: list[int | None]) -> dict:
    n = len(ranks)
    if n == 0:
        return {"n": 0, "mrr": 0.0, "r5": 0.0, "r20": 0.0}
    return {
        "n": n,
        "mrr": sum(1.0 / r for r in ranks if r) / n,
        "r5": sum(1 for r in ranks if r and r <= 5) / n,
        "r20": sum(1 for r in ranks if r and r <= 20) / n,
    }


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("fold")
    ap.add_argument("--limit", type=int, default=100, help="how deep to fetch per query")
    ap.add_argument("--n", type=int, default=60, help="sample this many pairs (0 = all)")
    ap.add_argument("--jobs", type=int, default=2, help="concurrent requests; keep low, shared box")
    ap.add_argument("--thresholds", default="0,0.5,0.67,1.0",
                    help="minimum coverage fractions to simulate; 0 = today's OR behaviour")
    ap.add_argument("--out", help="write the per-query detail as JSON")
    args = ap.parse_args()

    pairs = json.load(open(args.fold))["pairs"]
    if args.n:
        pairs = pairs[: args.n]
    thresholds = [float(t) for t in args.thresholds.split(",")]

    print(f"fold: {args.fold}", file=sys.stderr)
    print(f"{len(pairs)} pairs, limit={args.limit}, jobs={args.jobs}, base={BASE}", file=sys.stderr)

    def fetch(p):
        try:
            return p, search(p["query"], args.limit)
        except Exception as e:  # a dead query should not lose the whole run
            return p, e

    rows = []
    with ThreadPoolExecutor(max_workers=args.jobs) as ex:
        for i, (p, hits) in enumerate(ex.map(fetch, pairs), 1):
            if isinstance(hits, Exception):
                print(f"  !! {p['query']}: {hits}", file=sys.stderr)
                continue
            toks = content_tokens(p["query"])
            scored = [(h, hit_coverage(h, toks)) for h in hits]
            row = {"query": p["query"], "shape": p["shape"], "category": p.get("category"),
                   "gold_uri": p["gold_uri"], "returned": len(hits), "by_threshold": {}}
            for t in thresholds:
                kept = [h for h, c in scored if c >= t] if t > 0 else hits
                row["by_threshold"][str(t)] = {
                    "kept": len(kept),
                    "rank": rank_of(kept, p["gold_uri"]),
                }
            rows.append(row)
            if i % 20 == 0:
                print(f"  {i}/{len(pairs)}", file=sys.stderr)

    # ---- report ----
    print()
    print(f"{'threshold':>9}  {'split':<13} {'n':>4} {'MRR':>7} {'r@5':>7} {'r@20':>7} "
          f"{'kept':>6} {'saturated':>10}")
    for t in thresholds:
        key = str(t)
        for split in ("ALL", "single-token", "multi-word"):
            sel = [r for r in rows if split == "ALL" or r["shape"] == split]
            if not sel:
                continue
            m = metrics([r["by_threshold"][key]["rank"] for r in sel])
            kept = sum(r["by_threshold"][key]["kept"] for r in sel) / len(sel)
            # A query whose surviving conventional pool still fills the visible list leaves
            # the supplementary tier a quota of zero -- this column is the MeSH-relevant one.
            sat = sum(1 for r in sel if r["by_threshold"][key]["kept"] >= 20) / len(sel)
            label = "OR (today)" if t == 0 else f"{t:.2f}"
            print(f"{label:>9}  {split:<13} {m['n']:>4} {m['mrr']:>7.3f} {m['r5']:>7.3f} "
                  f"{m['r20']:>7.3f} {kept:>6.1f} {sat:>9.0%}")
        print()

    print("kept      = mean surviving candidates per query", file=sys.stderr)
    print("saturated = share of queries still filling the 20-slot list, i.e. still starving", file=sys.stderr)
    print("            the supplementary tier. THIS is the column MeSH depends on.", file=sys.stderr)

    if args.out:
        with open(args.out, "w") as f:
            json.dump(rows, f, indent=2)
        print(f"\nwrote {args.out}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
