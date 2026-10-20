#!/usr/bin/env python3
"""Build the precomputed MeSH-entry-term -> MONDO synonym table.

Why this exists
---------------
The OBO disease ontologies publish *formal* nomenclature ("malignant neoplasm of
larynx"); MeSH entry terms are the phrasing clinicians and authors actually write
("laryngeal cancer"). A disease named from a GEO series title therefore often fails
to ground even though the concept is present in MONDO. This script emits the missing
strings as a deterministic, precomputed table of (MONDO URI, synonym) pairs, which
`MeshDiseaseSynonymOntologyService` serves as a supplementary lexical source.

Conservatism, deliberately
--------------------------
Every rule here exists to keep a wrong synonym out of the index, because a false
positive grounds an experiment to the wrong disease and that is worse than a miss:

1. Category filter. Only MeSH descriptors in the disease branch (tree numbers C* or
   F03 "Mental Disorders"). A descriptor outside those trees never contributes.
2. MONDO-asserted joins only. MONDO's own `MESH:` xrefs plus its SSSOM
   `skos:exactMatch` rows. The DOID route (MeSH <- DOID xref, DOID -> MONDO) is
   deliberately NOT used: it is 8.9% ambiguous and maps MeSH D007822 onto BOTH
   "larynx cancer" and "benign laryngeal neoplasm" -- exactly the error class this
   table must not introduce.
3. Unambiguous only. A MeSH descriptor resolving to more than one live MONDO term is
   dropped entirely rather than guessed at.
4. Preferred concept only. A MeSH descriptor bundles several Concepts, and MeSH states
   each non-preferred one's relation to the preferred one. Across the 5,194
   disease-branch descriptors those relations are 3,859 NRW (narrower), 723 REL
   (related), 139 BRD (broader) -- and not one exact synonym. So every non-preferred
   concept's terms are the wrong breadth for the descriptor's MONDO target: NRW and
   REL over-broaden a match, BRD over-narrows it. Only the preferred concept's terms
   are true synonyms of the heading. --all-concepts exists to reproduce this finding,
   not to be used.

   This is why GSE25727 is not fixed here: MeSH files "Laryngeal Cancer" under concept
   M0332985, which it marks NRW of the "Laryngeal Neoplasms" heading that MONDO maps to
   MONDO:0021071 (laryngeal neoplasm). Attaching it would ground "laryngeal cancer" to
   the benign-or-malignant parent instead of MONDO:0002352 (larynx cancer). Reaching
   the right term needs a CONCEPT-level mapping, not this descriptor-level one.
5. Obsolete MONDO targets are dropped.
6. Strings already carried by the MONDO term (label or synonym, normalized) are
   dropped -- this table holds only what MONDO lacks.
7. MeSH inverted headings ("Neoplasms, Laryngeal") are dropped: nobody writes them in
   a paper title, so they are index weight with no recall. --keep-inverted restores.
8. Strings shorter than 3 characters are dropped; `OntologyServiceImpl` has a 3-char
   search floor, so they could never match anyway.

Usage
-----
    scripts/build_mesh_disease_synonyms.py --out gemma-core/src/main/resources/ubic/gemma/core/ontology/mesh-disease-synonyms.tsv
    scripts/build_mesh_disease_synonyms.py --check   # compare source versions to the sidecar; exit 1 on drift

Sources are public domain (NLM MeSH) or CC-BY (MONDO); neither carries the notify /
hyperlink obligations that made CTD and the redistribution terms that made UMLS
unattractive.
"""
from __future__ import annotations

import argparse
import csv
import datetime
import gzip
import hashlib
import io
import json
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

MESH_DESC_URL = "https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc{year}.gz"
MONDO_OBO_URL = "http://purl.obolibrary.org/obo/mondo.obo"
MONDO_SSSOM_URL = "https://raw.githubusercontent.com/monarch-initiative/mondo/master/src/ontology/mappings/mondo.sssom.tsv"

MONDO_URI_PREFIX = "http://purl.obolibrary.org/obo/MONDO_"

# MeSH tree branches that are diseases. C = Diseases; F03 = Mental Disorders (the rest
# of F is behaviour/psychology, which is not a disease and must not leak in).
DISEASE_TREE_PREFIXES = ("C", "F03")

MIN_SYNONYM_LENGTH = 3


def log(msg: str) -> None:
    print(msg, file=sys.stderr)


def normalize(s: str) -> str:
    """Match-equivalence for dedup against MONDO's existing strings."""
    return re.sub(r"\s+", " ", s.strip().lower())


def fetch(url: str, cache_dir: Path, name: str, force: bool = False) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)
    dest = cache_dir / name
    if dest.exists() and not force:
        log(f"  cached  {name} ({dest.stat().st_size:,} bytes)")
        return dest
    log(f"  fetching {url}")
    tmp = dest.with_suffix(dest.suffix + ".part")
    with urllib.request.urlopen(url) as r, open(tmp, "wb") as f:
        while chunk := r.read(1 << 20):
            f.write(chunk)
    tmp.replace(dest)
    log(f"  saved   {name} ({dest.stat().st_size:,} bytes)")
    return dest


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(1 << 20):
            h.update(chunk)
    return h.hexdigest()


# ----------------------------------------------------------------------------
# MeSH
# ----------------------------------------------------------------------------

def parse_mesh(path: Path, all_concepts: bool) -> tuple[dict, str | None]:
    """DescriptorUI -> {name, terms, trees} for disease-branch descriptors."""
    out = {}
    version = None
    with gzip.open(path, "rb") as f:
        for event, elem in ET.iterparse(f, events=("end",)):
            if elem.tag == "DescriptorRecordSet" and version is None:
                version = elem.get("LanguageCode")
            if elem.tag != "DescriptorRecord":
                continue
            trees = [t.text for t in elem.findall("./TreeNumberList/TreeNumber") if t.text]
            if not any(t.startswith(DISEASE_TREE_PREFIXES) for t in trees):
                elem.clear()
                continue
            ui = elem.findtext("./DescriptorUI")
            name = elem.findtext("./DescriptorName/String")
            terms = []
            for concept in elem.findall("./ConceptList/Concept"):
                if not all_concepts and concept.get("PreferredConceptYN") != "Y":
                    continue
                for s in concept.findall("./TermList/Term/String"):
                    if s.text:
                        terms.append(s.text)
            if ui and name:
                out[ui] = {"name": name, "terms": terms, "trees": trees}
            elem.clear()
    return out, version


# ----------------------------------------------------------------------------
# MONDO
# ----------------------------------------------------------------------------

def parse_mondo_obo(path: Path) -> tuple[dict, str | None]:
    terms: dict[str, dict] = {}
    version = None
    cur = None
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if version is None and line.startswith("data-version:"):
                version = line.split(":", 1)[1].strip()
            if line.startswith("["):
                if cur is not None and cur["id"]:
                    terms[cur["id"]] = cur
                cur = {"id": None, "name": None, "strings": set(), "mesh": set(), "obs": False} \
                    if line == "[Term]" else None
                continue
            if cur is None:
                continue
            if line.startswith("id: "):
                cur["id"] = line[4:].strip()
            elif line.startswith("name: "):
                cur["name"] = line[6:].strip()
                cur["strings"].add(normalize(cur["name"]))
            elif line.startswith("synonym: "):
                m = re.match(r'synonym: "((?:[^"\\]|\\.)*)"', line)
                if m:
                    cur["strings"].add(normalize(m.group(1).replace('\\"', '"')))
            elif line.startswith("xref: "):
                x = line[6:].split(" ")[0].split("{")[0].strip()
                if x.upper().startswith("MESH:"):
                    cur["mesh"].add(x.split(":", 1)[1])
            elif line.startswith("is_obsolete: true"):
                cur["obs"] = True
        if cur is not None and cur["id"]:
            terms[cur["id"]] = cur
    live = {k: v for k, v in terms.items() if k.startswith("MONDO:") and not v["obs"]}
    return live, version


def parse_sssom(path: Path) -> dict[str, set[str]]:
    """MeSH id -> {MONDO curie}, exactMatch rows only."""
    out: dict[str, set[str]] = defaultdict(set)
    with open(path, encoding="utf-8") as f:
        lines = [l for l in f if not l.startswith("#")]
    for row in csv.DictReader(lines, delimiter="\t"):
        obj = (row.get("object_id") or "")
        if obj.lower().startswith("mesh:") and row.get("predicate_id") == "skos:exactMatch":
            out[obj.split(":", 1)[1]].add(row["subject_id"])
    return out


# ----------------------------------------------------------------------------
# Build
# ----------------------------------------------------------------------------

def build(args) -> int:
    cache = Path(args.cache)
    log("Sources:")
    mesh_path = fetch(MESH_DESC_URL.format(year=args.mesh_year), cache, f"desc{args.mesh_year}.gz", args.force)
    obo_path = fetch(MONDO_OBO_URL, cache, "mondo.obo", args.force)
    sssom_path = fetch(MONDO_SSSOM_URL, cache, "mondo.sssom.tsv", args.force)

    log("Parsing ...")
    mesh, _ = parse_mesh(mesh_path, args.all_concepts)
    mondo, mondo_version = parse_mondo_obo(obo_path)
    sssom = parse_sssom(sssom_path)
    log(f"  MeSH disease-branch descriptors : {len(mesh):,}")
    log(f"  MONDO live terms                : {len(mondo):,}  (data-version {mondo_version})")

    # Join: MONDO-asserted only, both directions unioned.
    joined: dict[str, set[str]] = defaultdict(set)
    for mondo_id, t in mondo.items():
        for mid in t["mesh"]:
            joined[mid].add(mondo_id)
    for mid, subjects in sssom.items():
        for s in subjects:
            if s in mondo:
                joined[mid].add(s)

    stats = {
        "mesh_descriptors_joined": 0,
        "dropped_ambiguous": 0,
        "dropped_already_in_mondo": 0,
        "dropped_inverted": 0,
        "dropped_too_short": 0,
    }
    rows = []
    for mid in sorted(joined):
        if mid not in mesh:
            continue  # not in the disease branch -- category filter
        targets = joined[mid]
        if len(targets) != 1:
            stats["dropped_ambiguous"] += 1
            continue
        mondo_id = next(iter(targets))
        term = mondo[mondo_id]
        stats["mesh_descriptors_joined"] += 1
        seen = set()
        for s in [mesh[mid]["name"]] + mesh[mid]["terms"]:
            n = normalize(s)
            if n in seen:
                continue
            seen.add(n)
            if n in term["strings"]:
                stats["dropped_already_in_mondo"] += 1
                continue
            if len(n) < MIN_SYNONYM_LENGTH:
                stats["dropped_too_short"] += 1
                continue
            if "," in s and not args.keep_inverted:
                stats["dropped_inverted"] += 1
                continue
            rows.append((
                MONDO_URI_PREFIX + mondo_id.split(":", 1)[1],
                term["name"],
                s.strip(),
                "MESH:" + mid,
            ))

    rows.sort()
    distinct_terms = len({r[0] for r in rows})
    log("Result:")
    log(f"  MeSH descriptors joined         : {stats['mesh_descriptors_joined']:,}")
    log(f"  dropped, ambiguous (->2+ MONDO) : {stats['dropped_ambiguous']:,}")
    log(f"  dropped, already in MONDO       : {stats['dropped_already_in_mondo']:,}")
    log(f"  dropped, inverted heading       : {stats['dropped_inverted']:,}")
    log(f"  dropped, under {MIN_SYNONYM_LENGTH} chars          : {stats['dropped_too_short']:,}")
    log(f"  SYNONYM ROWS                    : {len(rows):,} over {distinct_terms:,} MONDO terms")

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    built = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat()
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("# MeSH entry terms as MONDO disease synonyms -- PRECOMPUTED, do not hand-edit.\n")
        f.write(f"# script: scripts/build_mesh_disease_synonyms.py\n")
        f.write(f"# built: {built}\n")
        f.write(f"# mesh: desc{args.mesh_year}.gz (NLM, public domain)\n")
        f.write(f"# mondo: {mondo_version}\n")
        f.write(f"# join: MONDO-asserted only (obo MESH xref + sssom exactMatch); unambiguous only; disease branch (C*/F03)\n")
        f.write(f"# rows: {len(rows)} over {distinct_terms} MONDO terms\n")
        f.write("mondo_uri\tmondo_label\tsynonym\tmesh_id\n")
        for r in rows:
            f.write("\t".join(r) + "\n")

    meta = {
        "artifact": out.name,
        "sha256": sha256(out),
        "built": built,
        "rows": len(rows),
        "mondo_terms": distinct_terms,
        "stats": stats,
        "policy": {
            "disease_tree_prefixes": list(DISEASE_TREE_PREFIXES),
            "join": "mondo_obo_mesh_xref UNION mondo_sssom_exactMatch",
            "doid_route": "excluded (8.9% ambiguous)",
            "ambiguous": "dropped",
            "concepts": "all" if args.all_concepts else "preferred only",
            "inverted_headings": "kept" if args.keep_inverted else "dropped",
            "min_synonym_length": MIN_SYNONYM_LENGTH,
        },
        "sources": {
            "mesh": {"url": MESH_DESC_URL.format(year=args.mesh_year), "version": str(args.mesh_year),
                     "sha256": sha256(mesh_path)},
            "mondo_obo": {"url": MONDO_OBO_URL, "version": mondo_version, "sha256": sha256(obo_path)},
            "mondo_sssom": {"url": MONDO_SSSOM_URL, "sha256": sha256(sssom_path)},
        },
    }
    meta_path = out.with_name(out.stem + "_meta.json")
    meta_path.write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    log(f"\nWrote {out} ({out.stat().st_size:,} bytes)")
    log(f"Wrote {meta_path}")
    return 0


def check(args) -> int:
    """Compare live source versions against the sidecar; exit 1 if the table is stale."""
    meta_path = Path(args.out).with_name(Path(args.out).stem + "_meta.json")
    if not meta_path.exists():
        log(f"No sidecar at {meta_path}; nothing to check.")
        return 1
    meta = json.loads(meta_path.read_text())
    cache = Path(args.cache)
    obo_path = fetch(MONDO_OBO_URL, cache, "mondo.obo", force=True)
    _, mondo_version = parse_mondo_obo(obo_path)
    stale = []
    if mondo_version != meta["sources"]["mondo_obo"]["version"]:
        stale.append(f"MONDO {meta['sources']['mondo_obo']['version']} -> {mondo_version}")
    if str(args.mesh_year) != meta["sources"]["mesh"]["version"]:
        stale.append(f"MeSH {meta['sources']['mesh']['version']} -> {args.mesh_year}")
    if stale:
        log("STALE: " + "; ".join(stale))
        return 1
    log(f"Up to date (MONDO {mondo_version}, MeSH {args.mesh_year}).")
    return 0


def main() -> int:
    default_out = "gemma-core/src/main/resources/ubic/gemma/core/ontology/mesh-disease-synonyms.tsv"
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--out", default=default_out, help=f"output TSV (default: {default_out})")
    p.add_argument("--cache", default=os.path.expanduser("~/gemmaData/meshBuildCache"),
                   help="download cache directory")
    p.add_argument("--mesh-year", type=int, default=datetime.date.today().year, help="MeSH descriptor year")
    p.add_argument("--force", action="store_true", help="re-download sources, ignoring the cache")
    p.add_argument("--keep-inverted", action="store_true",
                   help="keep MeSH inverted headings ('Neoplasms, Laryngeal')")
    p.add_argument("--all-concepts", action="store_true",
                   help="include non-preferred MeSH concepts (looser; can be narrower than the heading)")
    p.add_argument("--check", action="store_true", help="report whether the committed table is stale")
    args = p.parse_args()
    return check(args) if args.check else build(args)


if __name__ == "__main__":
    sys.exit(main())
