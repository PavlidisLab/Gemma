#!/usr/bin/env python3
"""Build the genotype → disease-model lookup table from Gemma's own corpus.

The premise of the 2026-08-09 rule (`docs/curation_rules/05_genotype_efc.md`)
is that the model-of relation is DERIVABLE, so a model/WT contrast can be a
`genotype` factor instead of a `disease model` one without losing anything.
This table is what makes that claim honest rather than a way of dropping
information — Paul, 2026-08-12:

  *"what we really need is a separate resource to say what different genotypes
  are modeling. It's not something we need to track. but we want to not lose
  the ability for users looking for 'autism' studies or whatever to find this."*

The join is free and already local: an experiment carrying BOTH a genotype
annotation and a `disease` / `disease model` annotation yields a (genotype,
disease) candidate pair, and the frequency across 23,477 experiments doubles as
the confidence signal. Nothing here writes annotations.

🛑 Reads the ON-DISK dump, never the REST API
(`~/Data/.../gemma_gold_snapshot_2026-05-16.jsonl`, production snapshot).

Scope (Paul, 2026-08-09): the 500 we work with are all in play, test100
included; what stays set aside is the REST of the 2,500-escrow pool. Built in
rather than remembered.

Gene symbols are extracted from the genotype value so the result joins to
external sources on the same key — MGI's `MGI_DO.rpt` (gene → Disease Ontology,
19,833 rows) recovers Chd8→autism, App/Psen1→Alzheimer's, Ercc6→Cockayne, but
is unranked and many-to-many (Trp53 returns CHARGE, Li-Fraumeni, breast cancer
before medulloblastoma). Our corpus counts are what disambiguate it.

    python scripts/build_genotype_disease_model_table.py
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

# Runs standalone: the only REQUIRED input is a Gemma dump. The escrow
# exclusion below is an eval-side concern and is optional, so this script is
# portable into the Gemma tree without dragging the eval repo behind it.
HERE = Path(__file__).resolve().parent
REPO = HERE.parent
SNAP = Path("/Users/pzoot/Data/gemma-curation-agents-eval/data/"
            "gemma_gold_snapshot_2026-05-16.jsonl")

# `Homozygous negative  Trp53 [mouse] transformation related protein 53`
# `Overexpression of  R495X/+  FUS [human] FUS RNA binding protein`
# `gene knockdown of  POM121 [human] POM121 transmembrane nucleoporin`
_SYMBOL = re.compile(r"\b([A-Za-z0-9][A-Za-z0-9\-]{1,14})\s*\[(?:mouse|human|rat)\]")
_LEADING_VERB = re.compile(
    r"^(?:homozygous negative|heterozygous|overexpression of|gene knockdown of|"
    r"knockdown of|knockout of|deletion of|mutation of|conditional)\s+", re.I)


def _gene_symbol(value: str) -> str:
    """The perturbed gene, or "" when the value names no gene (e.g.
    `wild type genotype`, a strain-shaped value like `APP/PS1 transgenic`)."""
    m = _SYMBOL.search(value or "")
    if m:
        return m.group(1).upper()
    v = _LEADING_VERB.sub("", str(value or "")).strip()
    if v and " " not in v and len(v) <= 15 and not v.lower().startswith("wild"):
        return v.upper()
    return ""


def _mgi_path(out: Path) -> Path:
    return out.parent / "MGI_DO.rpt"


def _norm_disease(s: str) -> str:
    """Normalize a disease LABEL for cross-vocabulary comparison.

    🛑 A LABEL join, and deliberately flagged as such. The repo rule is to
    compare URIs — but MGI speaks Disease Ontology (`DOID:`) and Gemma speaks
    MONDO, and no DOID<->MONDO mapping is available locally (the ontology index
    carries id/label/synonyms/parents, no xrefs). MONDO's DOID xrefs are the
    rigorous path if this ever becomes load-bearing. Until then MGI is a
    CROSS-CHECK, never an input to a decision, and its agreement rate is an
    underestimate by exactly the labels this misses.
    """
    t = (s or "").lower().replace("'", "").replace("’", "")
    for junk in (" disease", " syndrome", " disorder"):
        pass  # keep them: `Alzheimer disease` vs `Alzheimers disease` differ only by the apostrophe
    return " ".join(sorted(t.replace(",", " ").replace("-", " ").split()))


def _load_mgi(path: Path) -> dict[str, set[str]]:
    """gene symbol -> {normalized DO disease name}."""
    out: dict[str, set[str]] = defaultdict(set)
    if not path.is_file():
        return out
    with path.open() as fh:
        for r in csv.DictReader(fh, delimiter="\t"):
            sym = str(r.get("Symbol") or "").strip().upper()
            dis = str(r.get("DO Disease Name") or "").strip()
            if sym and dis:
                out[sym].add(_norm_disease(dis))
    return out


def _excluded(escrow: set[str], keep: set[str]) -> set[str]:
    return {g for g in escrow if g not in keep}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--snapshot", type=Path, default=SNAP)
    ap.add_argument("--out", type=Path,
                    default=REPO / "data/genotype_disease_model/"
                                   "genotype_disease_model.tsv")
    ap.add_argument("--min-n", type=int, default=1)
    ap.add_argument("--no-mgi", action="store_true",
                    help="skip the MGI cross-check columns")
    ap.add_argument("--exclude-gses", type=Path, default=None,
                    help="TSV whose first column lists accessions to EXCLUDE "
                         "(eval-side escrow policy; omit to scan everything)")
    ap.add_argument("--keep-gses", type=Path, default=None,
                    help="JSONL of {accession: ...} kept even when excluded")
    a = ap.parse_args()

    escrow, keep = set(), set()
    if a.exclude_gses and a.exclude_gses.is_file():
        for i, ln in enumerate(a.exclude_gses.open()):
            if i and ln.split("\t")[0].strip():
                escrow.add(ln.split("\t")[0].strip())
    if a.keep_gses and a.keep_gses.is_file():
        for ln in a.keep_gses.open():
            if not ln.strip():
                continue
            try:
                acc = json.loads(ln).get("accession")
            except json.JSONDecodeError:
                continue
            if acc:
                keep.add(acc)
    skip = _excluded(escrow, keep)
    if skip:
        print(f"[table] exclusion list {len(escrow)}; {len(keep)} kept anyway; "
              f"{len(skip)} set aside")
    else:
        print("[table] no exclusion list — scanning every experiment in the dump")

    pairs: dict[tuple[str, str], dict] = defaultdict(
        lambda: {"n": 0, "gses": [], "evidence": Counter(), "uris": Counter()})
    n_exp = n_used = 0
    for line in open(a.snapshot):
        if not line.strip():
            continue
        try:
            r = json.loads(line)
        except json.JSONDecodeError:
            continue
        n_exp += 1
        gse = str(r.get("short_name") or "")
        if gse in skip:
            continue
        genos: list[tuple[str, str, str]] = []   # (value, uri, objectClass)
        diseases: list[tuple[str, str]] = []     # (value, uri)
        for an in ((r.get("annotations") or {}).get("data") or []):
            cn = str(an.get("className") or "").strip().lower()
            tn = str(an.get("termName") or "").strip()
            tu = str(an.get("termUri") or "").strip()
            oc = str(an.get("objectClass") or "")
            if not tn:
                continue
            if cn == "genotype":
                genos.append((tn, tu, oc))
            elif cn in ("disease", "disease model"):
                diseases.append((tn, tu))
        if not genos or not diseases:
            continue
        n_used += 1
        for gv, gu, oc in genos:
            if gv.lower().startswith("wild type"):
                continue          # the control arm is not a model of anything
            for dv, du in diseases:
                k = (gv, dv)
                e = pairs[k]
                e["n"] += 1
                if len(e["gses"]) < 8:
                    e["gses"].append(gse)
                e["evidence"][oc] += 1
                if du:
                    e["uris"][du] += 1
                e["gene"] = _gene_symbol(gv)
                e["geno_uri"] = gu

    mgi = {} if a.no_mgi else _load_mgi(_mgi_path(a.out))
    if mgi:
        print(f"[table] MGI cross-check loaded: {len(mgi)} gene(s)")
    rows = []
    for (gv, dv), e in pairs.items():
        if e["n"] < a.min_n:
            continue
        rows.append({
            "gene_symbol": e.get("gene", ""),
            "genotype_value": gv,
            "genotype_uri": e.get("geno_uri", ""),
            "disease_value": dv,
            "disease_uri": (e["uris"].most_common(1)[0][0] if e["uris"] else ""),
            "n_experiments": e["n"],
            "evidence": "+".join(f"{k}:{v}" for k, v in e["evidence"].most_common()),
            "example_gses": ",".join(e["gses"]),
            "mgi_agrees": "", "mgi_diseases": "",
        })
        if mgi and rows[-1]["gene_symbol"]:
            known = mgi.get(rows[-1]["gene_symbol"], set())
            if known:
                rows[-1]["mgi_agrees"] = "yes" if _norm_disease(dv) in known else "no"
                rows[-1]["mgi_diseases"] = "; ".join(sorted(known)[:4])
    rows.sort(key=lambda r: (-r["n_experiments"], r["gene_symbol"]))

    a.out.parent.mkdir(parents=True, exist_ok=True)
    cols = ["gene_symbol", "genotype_value", "genotype_uri", "disease_value",
            "disease_uri", "n_experiments", "evidence", "example_gses",
            "mgi_agrees", "mgi_diseases"]
    with a.out.open("w", newline="") as fh:
        fh.write(f"# artifact: genotype -> disease-model lookup\n")
        fh.write(f"# script: scripts/build_genotype_disease_model_table.py\n")
        fh.write(f"# source: {a.snapshot.name} (production snapshot, "
                 f"{n_exp} experiments)\n")
        fh.write(f"# scope: {len(skip)} accession(s) excluded"
                 + (f" (kept {len(keep)} in play)" if keep else "") + "\n")
        fh.write(f"# built_at_utc: {datetime.now(timezone.utc).isoformat()}\n")
        fh.write("# NOT an annotation source — a lookup that makes the "
                 "model-of relation derivable on demand\n")
        w = csv.DictWriter(fh, fieldnames=cols, delimiter="\t")
        w.writeheader()
        w.writerows(rows)

    meta = {
        "artifact": a.out.name,
        "n_experiments_scanned": n_exp,
        "n_experiments_with_both": n_used,
        "n_pairs": len(rows),
        "n_distinct_genes": len({r["gene_symbol"] for r in rows if r["gene_symbol"]}),
        "n_distinct_diseases": len({r["disease_value"] for r in rows}),
        "escrow_set_aside": len(skip),
        "sha256": hashlib.sha256(a.out.read_bytes()).hexdigest(),
        "built_at_utc": datetime.now(timezone.utc).isoformat(),
    }
    a.out.with_suffix(".meta.json").write_text(json.dumps(meta, indent=2) + "\n")

    print(f"[table] {n_used} experiment(s) carry BOTH a genotype and a "
          f"disease/disease-model annotation")
    print(f"[table] {len(rows)} pair(s), {meta['n_distinct_genes']} gene(s), "
          f"{meta['n_distinct_diseases']} disease(s) → {a.out}")
    if mgi:
        ag = Counter(r["mgi_agrees"] for r in rows)
        checkable = ag["yes"] + ag["no"]
        print(f"\n[mgi] gene in MGI: {checkable} pair(s) checkable "
              f"({len(rows) - checkable} not — no gene symbol, or gene absent from MGI)")
        if checkable:
            print(f"[mgi]   agrees: {ag['yes']} ({ag['yes']/checkable:.0%})  "
                  f"differs: {ag['no']}")
        print("[mgi]   a LABEL join across DO vs MONDO — agreement is a floor, "
              "not a measurement")
    print("\ntop pairs:")
    for r in rows[:12]:
        print(f"   {r['n_experiments']:3d}  {r['gene_symbol']:8s} "
              f"{r['genotype_value'][:44]:44s} → {r['disease_value']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
