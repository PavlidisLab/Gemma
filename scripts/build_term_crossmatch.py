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
      🛑 But only when a named sibling exists IN THE GROUP. C2C12's named CLO class does
      exist -- CLO_0002071, labelled 'C subscript(2) C subscript(12) cell', a broken
      subscript ingest with no definition (verified in clo.owl, 2026-08-18) -- but no
      string a submitter types reaches that label, so within any group RCB0987 (53
      annotations) has no named sibling and demoting it would leave the line with nothing.
      🛑 R2 HAS DECIDED ZERO GROUPS, and structurally cannot on this group-former: groups
      form on normalized-LABEL collisions, and a catalogue label ('rcb0987') never
      collides with a line name ('c2c12').  It is a guard for a wider group-former that
      does not exist yet.  If grouping ever widens to synonyms/aliases -- where cab
      measured 92 catalogue-vs-named pairs -- DO NOT inherit this preference unexamined:
      cab measured the catalogue class carrying the textual definition in 92/92 against
      33/92 for the named class, and strictly MORE specific parents in 8 of 92
      (CAB_TO_UIB_AND_GEMMA_BACKEND 2026-08-18).  Their SLVL case, where "prefer the
      named class" lands on an obsolete disease term, is caught here by R1 outranking
      R2; the specificity losses would not be.
  R3  the term another ontology CROSS-REFERENCES wins.  Paul's rule, 2026-08-18: *"the one
      with the best xrefs should lead, or which has a definition"*.  🛑 The OUTBOUND half is
      inert for CLO -- measured, every CLO cell-line class carries zero dbXrefs -- so what does
      the work is the INBOUND half: EFO had to point at one of the twins, and that is an outside
      editor's judgement made without reference to us.  `--efo-obo` supplies it.
  R4  the term that carries a DEFINITION wins.  A defined term is one somebody curated; an
      undefined twin is usually the stub.
      (Favoured ontology, per scripts/term_crossmatch_preferences.tsv, sits here unnumbered: it
      can only discriminate a cross-ontology group, so it never reaches a CLO twin.)
  R5  usage, as the last tie-break among equals, AND ONLY ABOVE AN EVIDENCE FLOOR.
      🛑 The winner needs >=2 annotations and must lead by >=2.  Without the floor, 24 CLO groups
      are decided by a SINGLE annotation -- one curator's spelling, typed once, promoted to the
      answer an external consumer reads as settled.  `LOVO` x4 over `LoVo` x3 is the case that
      proves it.  Below the floor: abstain.
      🛑 Safe here ONLY because R1 has already removed every obsolete term.  cab's objection
      to "most-used wins" is that an obsolete term accrues usage while its successor sits at
      zero; that is real, and it is why usage is LAST and never overrides R1.  Measured on the
      corpus: none of the 17 CLO twin groups contains an obsolete term, so R1 never fires on
      them and R5 decides them all without ties.
  R6  otherwise ABSTAIN -- `needs_curator`.  A forced choice manufactures a confident wrong
      answer; abstaining is a first-class outcome, not a failure.

TWO WAYS TO FORM GROUPS, AND THE DEFAULT IS THE NARROW ONE.

  corpus-anchored (default) -- members come from the census, so a group is only visible if the
      corpus USES its members.  Good for "what do we hold that needs repairing".
  ontology-anchored (`--clo-owl` + `--efo-obo`) -- members come from CLO's own label collisions,
      read off disk, with census usage joined afterwards as tie-break evidence only.  🛑 This is
      the one that can see a twin with ZERO annotations, and that twin is not a curiosity: an
      outside resolver MINTS `CLO_0001199` (22RV1, zero uses) out of its own file order and asks
      Gemma which twin to keep.  The corpus-anchored pass is structurally blind to precisely the
      row such a caller needs.  Writes TermUriMigration.tsv rows directly.

`--decide-label-collisions` promotes normalized-label collisions to real groups in the
corpus-anchored pass.  OFF by default, and it must stay that way: a clone and its parent line normalize alike, and the
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
    # The census comes out of MySQL with latin-1 bytes in a few labels; strict utf-8 dies on
    # them and takes the whole run with it. Replace rather than fail -- a mangled character in
    # a label never changes which URI a row is about.
    with path.open(encoding="utf-8", errors="replace") as fh:
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


def choose(members: list[dict], prefs: dict[tuple[str, str], int],
           efo_xref: frozenset[str] = frozenset(),
           min_winner: int = 0, min_margin: int = 0) -> tuple[dict | None, str]:
    """Apply R1-R5. Returns (favoured_or_None, reason).

    `efo_xref` is the set of URIs an outside ontology (EFO) points at; `min_winner` /
    `min_margin` are the evidence floor R5 must clear before usage is allowed to decide.
    """
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

    # R3 -- an INBOUND xref from another ontology: EFO had to pick one of the twins to point at,
    # and that is an outside editor's judgement rather than ours. 🛑 Not to be confused with CLO's
    # own OUTBOUND dbXrefs, which are empty for every cell-line class -- see project memory.
    if efo_xref:
        xrefed = [m for m in live if m["uri"] in efo_xref]
        if xrefed and len(xrefed) < len(live):
            live = xrefed
            if len(live) == 1:
                return live[0], "R3 EFO xrefs it (an external ontology's editorial pick)"

    # R4 -- a defined term beats an undefined one (Paul's rule). A defined term is one somebody
    # curated; an undefined twin is usually the stub.
    defined = [m for m in live if m.get("hasDefinition")]
    if defined and len(defined) < len(live):
        live = defined
        if len(live) == 1:
            return live[0], "R4 carries a definition; its twin does not"

    # Favoured ontology, by category (data, not code). Deliberately unnumbered: it can only
    # discriminate a cross-ontology group, so it never fires on a CLO twin.
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
    top, runner = finalists[0].get("usage", 0), finalists[1].get("usage", 0) if len(finalists) > 1 else 0
    if len(finalists) > 1 and top == runner:
        return None, "R6 abstain: tie on every rule including usage"
    # 🛑 THE EVIDENCE FLOOR. Without it, 24 of these groups are decided by a SINGLE annotation --
    # one curator's spelling, typed once, becomes the answer we hand an external consumer as
    # settled. A margin of one is not a measurement, and `LOVO` x4 over `LoVo` x3 is the case that
    # proves it. Below the floor we abstain, which is a first-class outcome here (Paul, 2026-08-18).
    if top < min_winner or (top - runner) < min_margin:
        return None, (f"R6 abstain: usage margin too thin to decide "
                      f"({top} vs {runner}; floor is winner>={min_winner}, margin>={min_margin})")
    return finalists[0], "R5 usage, among members equal on every stronger rule"



# ---------------------------------------------------------------------------------------
# Ontology-anchored mode.  The corpus path above can only see a group whose members the
# corpus USES; a twin with zero annotations is invisible to it.  That is not a corner case:
# cab's Tier-0 synonym table MINTS `CLO_0001199` (22RV1, zero uses) out of file order, so the
# twin we most need to answer for is exactly the one no census can show us.  These read CLO
# and EFO straight off disk instead, so the groups come from the ontology.
# ---------------------------------------------------------------------------------------

RDF_NS = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}"
RDFS_NS = "{http://www.w3.org/2000/01/rdf-schema#}"
OWL_NS = "{http://www.w3.org/2002/07/owl#}"
OBO_NS = "{http://purl.obolibrary.org/obo/}"


def parse_clo_owl(path: pathlib.Path) -> dict[str, dict]:
    """Every CLO class, with the three facts the ladder needs: definition, obsolescence, successor.

    🛑 Clear ONLY the owl:Class elements.  A bare `el.clear()` on every end event wipes each
    <rdfs:label> before its parent class closes, and you get 39,084 classes with empty labels
    and zero collision groups -- which reads exactly like "CLO has no duplicates".
    """
    import xml.etree.ElementTree as ET
    classes: dict[str, dict] = {}
    for _, el in ET.iterparse(str(path), events=("end",)):
        if el.tag != OWL_NS + "Class":
            continue
        uri = el.get(RDF_NS + "about")
        if uri and "/CLO_" in uri:
            label = el.find(RDFS_NS + "label")
            defn = el.find(OBO_NS + "IAO_0000115")
            dep = el.find(OWL_NS + "deprecated")
            rb = el.find(OBO_NS + "IAO_0100001")
            classes[uri] = {
                "uri": uri,
                "label": (label.text or "").strip() if label is not None and label.text else "",
                "hasDefinition": bool(defn is not None and (defn.text or "").strip()),
                "obsolete": bool(dep is not None and (dep.text or "").strip().lower() == "true"),
                "termReplacedBy": (rb.get(RDF_NS + "resource") or (rb.text or "").strip())
                                  if rb is not None else None,
            }
        el.clear()
    return classes


def parse_efo_clo_xrefs(path: pathlib.Path) -> frozenset[str]:
    """CLO URIs that EFO points at -- the INBOUND half of the xref evidence (R3)."""
    hits: set[str] = set()
    with path.open(encoding="utf-8", errors="replace") as fh:
        for line in fh:
            if line.startswith("xref: CLO:"):
                hits.add("http://purl.obolibrary.org/obo/CLO_"
                         + line.split("CLO:", 1)[1].strip().split()[0])
    return frozenset(hits)


def strict_label_key(label: str) -> str:
    """Normalize for case and punctuation ONLY -- deliberately NOT stripping a trailing ' cell'.

    🛑 This is the guard on the twin lane.  `normalize_label` drops ' cell' so that `SW 480 cell`
    and `SW480 cell` meet, which is right; but it also makes `cell line cell` collide with
    `cell line`, and `immortal cell line` with `immortal cell line cell`.  Those are upper-level
    CLO classes, not duplicate cell lines, and canonicalizing one onto the other is a corruption
    dressed as a repair.  Requiring the labels to match WITHOUT the suffix strip admits every real
    twin and the upper-ontology artifacts fail it.

    KNOWN FALSE NEGATIVE, measured and accepted: a real twin pair where only ONE member carries
    the ' cell' suffix is rejected too -- `SK-MEL-1 cell` / `SKMEL1` is the one such pair in CLO.
    It is not worth a subtler rule: both its members have zero usage, no definition and no EFO
    xref, so the ladder abstains on it anyway and a smarter guard would change no output.
    """
    return re.sub(r"[^a-z0-9]", "", label.strip().lower())


def build_twin_rows(classes: dict[str, dict], efo_xref: frozenset[str],
                    usage: dict[str, int], prefs: dict[tuple[str, str], int],
                    min_winner: int, min_margin: int) -> tuple[list[list], dict[str, int]]:
    """Ontology-anchored twin groups -> TermUriMigration.tsv rows, one per losing member."""
    for c in classes.values():
        c["usage"] = usage.get(c["uri"], 0)
        c["categories"] = ()

    by_norm: dict[str, list[dict]] = collections.defaultdict(list)
    for c in classes.values():
        if c["label"]:
            by_norm[normalize_label(c["label"])].append(c)

    rows: list[list] = []
    tally = collections.Counter()
    for norm, members in by_norm.items():
        if not norm or len(members) < 2:
            continue
        tally["groups"] += 1
        if len({strict_label_key(m["label"]) for m in members}) > 1:
            tally["dropped_suffix_artifact"] += 1
            continue
        favoured, why = choose(members, prefs, efo_xref, min_winner, min_margin)
        if favoured is None:
            tally["undecided"] += 1
            continue
        tally["decided"] += 1
        for m in sorted(members, key=lambda m: -m["usage"]):
            if m["uri"] == favoured["uri"]:
                continue
            rows.append(["clo_twin", m["uri"], m["label"], favoured["uri"], favoured["label"],
                         m["usage"], why])
            tally["rows"] += 1
            tally["rows_zero_usage" if m["usage"] == 0 else "rows_with_usage"] += 1
    rows.sort(key=lambda r: (-r[5], r[1]))
    return rows, tally

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
    ap.add_argument("--clo-owl", type=pathlib.Path,
                    help="CLO in RDF/XML. Switches to ONTOLOGY-ANCHORED twin mode: groups come "
                         "from CLO's own label collisions rather than from the corpus, so a twin "
                         "with zero annotations is visible. Writes TermUriMigration.tsv rows.")
    ap.add_argument("--efo-obo", type=pathlib.Path,
                    help="EFO in OBO format, for the R3 inbound-xref rule. Required with --clo-owl.")
    ap.add_argument("--min-usage-winner", type=int, default=2,
                    help="R5 evidence floor: the winner needs at least this many annotations (2).")
    ap.add_argument("--min-usage-margin", type=int, default=2,
                    help="R5 evidence floor: the winner must lead by at least this much (2).")
    ap.add_argument("--decide-label-collisions", action="store_true",
                    help="promote normalized-label collisions to groups and decide them "
                         "(OFF by default -- a clone and its parent normalize alike)")
    ap.add_argument("--anonymous", action="store_true")
    ap.add_argument("--workers", type=int, default=8)
    args = ap.parse_args()

    if args.clo_owl:
        if not args.efo_obo:
            print("ERROR: --clo-owl needs --efo-obo (the R3 inbound-xref rule reads it).",
                  file=sys.stderr)
            return 2
        census = read_census(args.census)
        usage = {u: r["usage"] for u, r in census.items()}
        print(f"census: {len(census)} distinct URIs", file=sys.stderr)
        classes = parse_clo_owl(args.clo_owl)
        print(f"CLO: {len(classes)} classes from {args.clo_owl}", file=sys.stderr)
        efo_xref = parse_efo_clo_xrefs(args.efo_obo)
        print(f"EFO: points at {len(efo_xref)} CLO classes", file=sys.stderr)
        prefs = load_preferences(pathlib.Path(__file__).with_name("term_crossmatch_preferences.tsv"))
        rows, tally = build_twin_rows(classes, efo_xref, usage, prefs,
                                      args.min_usage_winner, args.min_usage_margin)
        # lineterminator="\n": csv.writer defaults to CRLF, and the output of this script is
        # meant to be diffed against -- and pasted into -- a LF file. A CRLF copy compares as
        # 100% changed against an identical LF one, which reads as "nothing reproduces".
        with args.out.open("w", newline="") as fh:
            w = csv.writer(fh, delimiter="\t", lineterminator="\n")
            w.writerow(["lane", "from_uri", "from_label", "to_uri", "to_label",
                        "n_annotations", "rule"])
            w.writerows(rows)
        print(f"CLO label-collision groups: {tally['groups']} "
              f"({tally['dropped_suffix_artifact']} dropped as ' cell'-suffix artifacts)",
              file=sys.stderr)
        print(f"  decided {tally['decided']}, undecided {tally['undecided']} "
              f"(floor: winner>={args.min_usage_winner}, margin>={args.min_usage_margin})",
              file=sys.stderr)
        print(f"wrote {tally['rows']} clo_twin rows to {args.out} "
              f"-- {tally['rows_with_usage']} the corpus uses, "
              f"{tally['rows_zero_usage']} with zero usage (invisible to the corpus-anchored path)",
              file=sys.stderr)
        return 0

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
