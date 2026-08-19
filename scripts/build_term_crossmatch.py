#!/usr/bin/env python3
"""Assemble the cross-match table: which annotation URIs denote the same thing, and which
one we should be writing.

Paul, 2026-08-18: *"not blacklist, but RESOLVE them to ONE identifier, on the server side.
Don't show the one we 'disfavour' but list it as an alternative, and it can be used as a
source of metadata."*  This script builds the table that decision needs. It proposes; it
writes nothing to Gemma.

    scripts/build_term_crossmatch.py --census census.tsv --out crossmatch.tsv

WHY TWO SOURCES.  Neither half can do this alone:

  * The corpus cannot be enumerated over REST -- /annotations/search 400s on an empty query
    and cell-line parents have no children in the index -- so the URIs in use come from SQL
    (scripts/sql/annotation_uri_census.sql, run separately; it is read-only).
  * The equivalence evidence is not in the database -- dbXrefs, alternativeIds, obsolete,
    termReplacedBy and consider[] all come from /annotations/term, which already serves them
    (923c42531b).

HOW GROUPS ARE FORMED.  Union-find over three edge kinds, strongest first. Every edge is
recorded so a reviewer can see WHY two URIs were joined:

  declared   an obsolete term's `termReplacedBy` names its successor.  Strongest: the
             ontology itself says these are the same thing.
  altid      one term's `alternativeIds` contains another's id (OBO hasAlternativeId --
             identifiers merged into it).
  xref       two terms share an external accession in `dbXrefs` (a CVCL_ accession is the
             only identifier that reconciles cell lines, precisely because Cellosaurus
             treats catalogue numbers as xrefs rather than as entities).

  🛑 NORMALIZED-LABEL collisions are deliberately NOT an edge.  A clone and its parent line
  normalize alike, so a label match is a question, not an answer.  They are emitted in a
  separate `label_candidates` report for eyeballing, and never auto-grouped.

HOW THE FAVOURED MEMBER IS CHOSEN.  Strict precedence, and the first two exist because
usage is the wrong instrument:

  R1  ontology-declared obsolescence beats usage, ALWAYS.
      🛑 cab measured CLO_0002950 'obsolete ES-D3 cell' at usage 4 while its declared
      successor CLO_0002949 sits at usage 0.  That is the steady state, not an oddity: an
      obsolete term keeps accruing annotations because curators and resolvers keep reaching
      the familiar label, while the successor is unused *because* it is the replacement.
      Usage is therefore systematically biased toward the term that should lose, and any
      "most-used wins" rule re-enshrines obsolete terms.
  R2  a catalogue class loses to a named class in the same group.
      A catalogue number ('RCB0009 cell') is not an entity, whatever its usage count.
      🛑 But only when a named sibling EXISTS -- C2C12 has no named CLO class at all, and
      its 53 annotations sit on RCB0987; demoting that would leave the line with nothing.
  R3  the term that carries a DEFINITION wins.  Paul's rule, 2026-08-18: *"the one with the
      best xrefs should lead, or which has a definition"*.  The xref half is inert for CLO --
      measured, every CLO cell-line class has zero -- so the definition half is what does the
      work.  A defined term is one somebody curated; an undefined twin is usually the stub.
  R4  favoured ontology, per scripts/term_crossmatch_preferences.tsv (data, not code).
  R5  usage, as the last tie-break among equals.
      🛑 Safe here ONLY because R1 has already removed every obsolete term.  cab's objection
      to "most-used wins" is that an obsolete term accrues usage while its successor sits at
      zero; that is real, and it is why usage is LAST and never overrides R1.  Measured on the
      corpus: none of the 17 CLO twin groups contains an obsolete term, so R1 never fires on
      them and R5 decides them all without ties.
  R6  otherwise ABSTAIN -- `needs_curator`.  A forced choice manufactures a confident wrong
      answer; abstaining is a first-class outcome, not a failure.

`--decide-label-collisions` promotes normalized-label collisions to real groups.  OFF by
default, and it must stay that way: a clone and its parent line normalize alike, and the
largest collision in the corpus by usage (`CLO_0037307` induced-pluripotent-stem-cell-line,
663 annotations) is a legitimate class term, not a duplicate.  Turning it on is a decision a
person makes with the candidate list in front of them, not a default.

WHAT THE LOSER IS.  Never deleted and never refused: it is emitted as an alternative, and
stays readable as a metadata source.  Resolution here means "show one, keep both".

🛑 THIS IS DETECTION, NOT MIGRATION.  Two holds are in force: the Gemma 2.0 write path is
held until PR #1667 merges and 1.0 redeploys (2.0 emits 21 audit discriminators 1.32.7
cannot load), and the corpus-wide retirement is deferred until after 2.0 ships.  Paul:
*"it's more about being ready."*
"""
from __future__ import annotations

import argparse
import collections
import csv
import json
import os
import pathlib
import re
import subprocess
import sys
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

DEFAULT_BASE = "https://gemma2.msl.ubc.ca"

# A catalogue number: letters then digits with no internal word break, optionally suffixed
# ' cell'.  Anchored deliberately -- a measurement leads with digits, a catalogue code with
# letters, and substring matching would swallow ordinary line names.
CATALOGUE_RE = re.compile(r"^(RCB|JCRB|IFO|ACC|CRL|HTB|CCL|TIB|KCLB|BCRC)\s?\d{3,}", re.I)

NAMESPACE_PATTERNS = [
    ("CLO", "/CLO_"), ("Cellosaurus", "CVCL_"), ("EFO", "/EFO_"), ("MONDO", "/MONDO_"),
    ("UBERON", "/UBERON_"), ("CL", "/CL_"), ("CHEBI", "/CHEBI_"), ("PATO", "/PATO_"),
    ("GO", "/GO_"), ("OBI", "/OBI_"), ("MP", "/MP_"), ("HP", "/HP_"),
    ("GENO", "/GENO_"), ("TGEMO", "TGEMO_"), ("NCBI Gene", "ncbi_gene"),
]


def keychain(var: str, *entries: str) -> str | None:
    """Resolve a credential from the macOS Keychain, honouring a pre-set env var."""
    if os.environ.get(var):
        return os.environ[var]
    for entry in (os.environ.get(f"{var}_KEYCHAIN_ENTRY"), *entries):
        if not entry:
            continue
        try:
            return subprocess.run(
                ["security", "find-generic-password", "-s", entry, "-w"],
                capture_output=True, text=True, check=True).stdout.strip()
        except subprocess.CalledProcessError:
            continue
    return None


def namespace_of(uri: str) -> str:
    for name, pat in NAMESPACE_PATTERNS:
        if pat in uri:
            return name
    return "other"


def is_catalogue(label: str | None) -> bool:
    return bool(label and CATALOGUE_RE.match(label.strip()))


def normalize_label(label: str) -> str:
    return re.sub(r"[^a-z0-9]", "", re.sub(r"\s+cell$", "", label.strip().lower()))


class DSU:
    def __init__(self) -> None:
        self.parent: dict[str, str] = {}

    def find(self, x: str) -> str:
        self.parent.setdefault(x, x)
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]
            x = self.parent[x]
        return x

    def union(self, a: str, b: str) -> None:
        ra, rb = self.find(a), self.find(b)
        if ra != rb:
            self.parent[rb] = ra


def read_census(path: pathlib.Path) -> dict[str, dict]:
    """Read the SQL census. Expects a header with at least uri, label, n_annotations."""
    rows: dict[str, dict] = {}
    with path.open() as fh:
        sniff = fh.read(4096)
        fh.seek(0)
        delim = "\t" if "\t" in sniff.splitlines()[0] else ","
        for r in csv.DictReader(fh, delimiter=delim):
            uri = (r.get("uri") or "").strip()
            if not uri:
                continue
            n = int(r.get("n_annotations") or 0)
            cur = rows.setdefault(uri, {"uri": uri, "label": (r.get("label") or "").strip(),
                                        "usage": 0, "categories": set()})
            cur["usage"] += n
            if not cur["label"] and r.get("label"):
                cur["label"] = r["label"].strip()
            if r.get("category"):
                cur["categories"].add(r["category"].strip())
    return rows


def fetch_term(base: str, uri: str, auth: str | None, cache: pathlib.Path) -> dict | None:
    key = cache / (re.sub(r"[^A-Za-z0-9]", "_", uri)[-120:] + ".json")
    if key.exists():
        try:
            return json.loads(key.read_text())
        except json.JSONDecodeError:
            key.unlink(missing_ok=True)
    url = f"{base.rstrip('/')}/rest/v2/annotations/term?uri={urllib.parse.quote(uri, safe='')}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    if auth:
        req.add_header("Authorization", auth)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            payload = json.load(resp).get("data")
    except Exception as exc:                                  # noqa: BLE001 - reported, not raised
        print(f"  ! {uri}: {exc}", file=sys.stderr)
        return None
    if payload is not None:
        key.write_text(json.dumps(payload))
    return payload


def load_preferences(path: pathlib.Path) -> dict[tuple[str, str], int]:
    prefs: dict[tuple[str, str], int] = {}
    if not path.exists():
        return prefs
    for line in path.read_text().splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) >= 3:
            prefs[(parts[0].strip(), parts[1].strip())] = int(parts[2])
    return prefs


def choose(members: list[dict], prefs: dict[tuple[str, str], int]) -> tuple[dict | None, str]:
    """Apply R1-R5. Returns (favoured_or_None, reason)."""
    live = [m for m in members if not m.get("obsolete")]

    # R1 -- an ontology-declared successor wins outright, whatever the usage says.
    for m in members:
        if m.get("obsolete") and m.get("termReplacedBy"):
            successor = next((x for x in members if x["uri"] == m["termReplacedBy"]), None)
            if successor is not None:
                return successor, "R1 declared successor (obsolescence beats usage)"
    if not live:
        return None, "R6 abstain: every member is obsolete and none names a successor here"
    if len(live) == 1:
        return live[0], "R1 sole live member; the rest are obsolete"

    # R2 -- a catalogue number is not an entity, but only demote it if a named sibling exists.
    named = [m for m in live if not is_catalogue(m.get("label"))]
    if named and len(named) < len(live):
        live = named
        if len(live) == 1:
            return live[0], "R2 catalogue class demoted in favour of a named class"

    # R3 -- a defined term beats an undefined one (Paul's rule; the xref half is inert for CLO).
    defined = [m for m in live if m.get("hasDefinition")]
    if defined and len(defined) < len(live):
        live = defined
        if len(live) == 1:
            return live[0], "R3 carries a definition; its twin does not"

    # R4 -- favoured ontology, by category. Unknown category or namespace ranks last.
    cats = {c for m in live for c in m.get("categories", ())}
    ranked: list[tuple[int, dict]] = []
    for m in live:
        best = min((prefs.get((c, namespace_of(m["uri"])), 99) for c in cats), default=99)
        ranked.append((best, m))
    top = min(r for r, _ in ranked)
    finalists = [m for r, m in ranked if r == top]
    if top < 99 and len(finalists) == 1:
        return finalists[0], f"R4 favoured ontology for {sorted(cats)}"

    # R5 -- usage, and only among otherwise-equal members. Never reached by an obsolete term:
    # R1 removed those, which is the whole reason this is safe to use at all.
    finalists.sort(key=lambda m: (-m.get("usage", 0), m["uri"]))
    if len(finalists) > 1 and finalists[0].get("usage", 0) == finalists[1].get("usage", 0):
        return None, "R6 abstain: tie on every rule including usage"
    return finalists[0], "R5 usage, among members equal on every stronger rule"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--census", required=True, type=pathlib.Path,
                    help="TSV/CSV from annotation_uri_census.sql (needs uri, label, n_annotations)")
    ap.add_argument("--out", required=True, type=pathlib.Path)
    ap.add_argument("--label-candidates", type=pathlib.Path,
                    help="where to write normalized-label collisions (candidates, never grouped)")
    ap.add_argument("--base", default=os.environ.get("GEMMA_BASE_URL") or DEFAULT_BASE)
    ap.add_argument("--namespace", help="comma-separated namespaces to restrict to, e.g. CLO,EFO")
    ap.add_argument("--cache", type=pathlib.Path, default=pathlib.Path(".term-cache"))
    ap.add_argument("--decide-label-collisions", action="store_true",
                    help="promote normalized-label collisions to groups and decide them "
                         "(OFF by default -- a clone and its parent normalize alike)")
    ap.add_argument("--anonymous", action="store_true")
    ap.add_argument("--workers", type=int, default=8)
    args = ap.parse_args()

    auth = None
    if not args.anonymous:
        user = keychain("GEMMA_USERNAME", "GEMMA_USERNAME", "gemma", "Gemma")
        pw = keychain("GEMMA_PASSWORD", "GEMMA_PASSWORD", "gemma", "Gemma")
        if not (user and pw):
            print("ERROR: no Gemma credentials in keychain; set GEMMA_USERNAME_KEYCHAIN_ENTRY "
                  "/ GEMMA_PASSWORD_KEYCHAIN_ENTRY, or pass --anonymous.", file=sys.stderr)
            return 1
        import base64
        auth = "Basic " + base64.b64encode(f"{user}:{pw}".encode()).decode()

    args.cache.mkdir(parents=True, exist_ok=True)
    census = read_census(args.census)
    if args.namespace:
        want = {n.strip() for n in args.namespace.split(",")}
        census = {u: r for u, r in census.items() if namespace_of(u) in want}
    print(f"census: {len(census)} distinct URIs", file=sys.stderr)

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        terms = dict(zip(census, pool.map(
            lambda u: fetch_term(args.base, u, auth, args.cache), census)))
    resolved = sum(1 for t in terms.values() if t)
    print(f"resolved {resolved}/{len(census)} through /annotations/term", file=sys.stderr)

    members: dict[str, dict] = {}
    for uri, row in census.items():
        t = terms.get(uri) or {}
        members[uri] = {
            "uri": uri, "label": t.get("label") or row["label"],
            "usage": row["usage"], "categories": row["categories"],
            "obsolete": bool(t.get("obsolete")),
            "hasDefinition": bool((t.get("definition") or "").strip()),
            "termReplacedBy": t.get("termReplacedBy"),
            "consider": [c.get("uri") for c in (t.get("consider") or []) if c.get("uri")],
            "obsoletedInVersion": t.get("obsoletedInVersion"),
            "dbXrefs": t.get("dbXrefs") or [], "alternativeIds": t.get("alternativeIds") or [],
            "resolved": bool(t),
        }

    dsu, edges = DSU(), collections.defaultdict(list)
    by_altid = collections.defaultdict(list)
    for m in members.values():
        for aid in m["alternativeIds"]:
            by_altid[aid.strip()].append(m["uri"])
    by_xref = collections.defaultdict(list)
    for m in members.values():
        for x in m["dbXrefs"]:
            if x and x.strip():
                by_xref[x.strip()].append(m["uri"])

    for m in members.values():
        rb = m["termReplacedBy"]
        if rb and rb in members:
            dsu.union(m["uri"], rb)
            edges[dsu.find(rb)].append(f"declared:{m['uri']}->{rb}")
    for other in members:
        short = other.rsplit("/", 1)[-1].replace("_", ":")
        for holder in by_altid.get(short, []):
            dsu.union(holder, other)
            edges[dsu.find(holder)].append(f"altid:{holder}~{other}")
    for xref, uris in by_xref.items():
        if len(uris) > 1:
            for u in uris[1:]:
                dsu.union(uris[0], u)
            edges[dsu.find(uris[0])].append(f"xref:{xref}={'+'.join(sorted(uris))}")

    if args.decide_label_collisions:
        by_norm_pre = collections.defaultdict(list)
        for m in members.values():
            if m["label"]:
                by_norm_pre[normalize_label(m["label"])].append(m["uri"])
        promoted = 0
        for nl, uris in by_norm_pre.items():
            if nl and len(uris) > 1 and len({dsu.find(u) for u in uris}) > 1:
                for u in uris[1:]:
                    dsu.union(uris[0], u)
                edges[dsu.find(uris[0])].append(f"label:{nl}={'+'.join(sorted(uris))}")
                promoted += 1
        print(f"--decide-label-collisions: promoted {promoted} label collisions to groups",
              file=sys.stderr)

    groups = collections.defaultdict(list)
    for uri in members:
        groups[dsu.find(uri)].append(members[uri])

    prefs = load_preferences(pathlib.Path(__file__).with_name("term_crossmatch_preferences.tsv"))
    written = 0
    with args.out.open("w", newline="") as fh:
        w = csv.writer(fh, delimiter="\t")
        w.writerow(["group", "favoured_uri", "favoured_label", "rule", "n_members",
                    "group_usage", "alternatives", "evidence", "unresolved"])
        for root, ms in sorted(groups.items(), key=lambda kv: -sum(m["usage"] for m in kv[1])):
            if len(ms) < 2:
                continue
            fav, why = choose(ms, prefs)
            alts = "  |  ".join(
                f"{m['uri']} ({m['label']}) x{m['usage']}"
                + ("  [obsolete" + (f"->{m['termReplacedBy']}" if m["termReplacedBy"] else "") + "]"
                   if m["obsolete"] else "")
                + ("  [catalogue]" if is_catalogue(m["label"]) else "")
                for m in sorted(ms, key=lambda m: -m["usage"]) if not fav or m["uri"] != fav["uri"])
            w.writerow([root, fav["uri"] if fav else "", fav["label"] if fav else "", why,
                        len(ms), sum(m["usage"] for m in ms), alts,
                        "; ".join(edges.get(root, [])),
                        ",".join(m["uri"] for m in ms if not m["resolved"])])
            written += 1
    print(f"wrote {written} cross-match groups to {args.out}", file=sys.stderr)

    if args.label_candidates:
        by_norm = collections.defaultdict(list)
        for m in members.values():
            if m["label"]:
                by_norm[normalize_label(m["label"])].append(m)
        with args.label_candidates.open("w", newline="") as fh:
            w = csv.writer(fh, delimiter="\t")
            w.writerow(["normalized_label", "n_uris", "already_grouped", "members"])
            n = 0
            for norm, ms in sorted(by_norm.items(), key=lambda kv: -sum(m["usage"] for m in kv[1])):
                if not norm or len(ms) < 2:
                    continue
                grouped = len({dsu.find(m["uri"]) for m in ms}) == 1
                w.writerow([norm, len(ms), "yes" if grouped else "NO -- eyeball this",
                            "  |  ".join(f"{m['uri']} ({m['label']}) x{m['usage']}"
                                         for m in sorted(ms, key=lambda m: -m["usage"]))])
                n += 1
            print(f"wrote {n} label-collision candidates to {args.label_candidates} "
                  f"(candidates, NOT groups -- a clone and its parent normalize alike)",
                  file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
