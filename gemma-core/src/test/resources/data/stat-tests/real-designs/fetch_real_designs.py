#!/usr/bin/env python3
"""
Builds the fixtures for RealDesignsFitTest from Gemma (read-only REST calls; no credentials):
for each dataset a design table (sample, block, cond) and the 50 highest-variance probes of the
processed expression data. Run, then run real-designs-golden-gen.R to (re)generate the limma references.

    python3 fetch_real_designs.py [GSE ...]

Samples are named by GEO sample accession (the part of the data column name after '___').
"""
import collections, csv, json, math, os, re, subprocess, sys, urllib.parse

BASE = os.environ.get("GEMMA_REST", "https://gemma2.msl.ubc.ca/rest/v2")
HERE = os.path.dirname(os.path.abspath(__file__))

# block: experimental factor id of the subject-like factor; cond: (factor id, {gemma level -> short label}) or None.
# Baseline (first label) is the level R and Gemma both treat as reference.
SPECS = {
    "GSE33658": dict(block=17464, cond=(17437, {"initial time point": "pre", "post-treatment": "post"}),
                     why="11 patients x 2 (balanced pairs)"),
    "GSE7400": dict(block=28069, cond=(1691, {"reference substance role": "ctrl", "CSF3 [human] colony stimulating factor 3": "csf3"}),
                    why="5 pairs, n=10: residual dof 4"),
    "GSE95298": dict(block=54500, cond=(54504, {"reference substance role": "ctrl", "metformin delivered for duration 72 h": "metformin"}),
                     why="20 cell lines, 11 with one pair and 9 with two replicate pairs: unequal block sizes 2 and 4"),
    "GSE246769": dict(block=50425, cond=(50426, {"initial time point": "d0", "2 d": "d2", "5 d": "d5", "9 d": "d9"}),
                      why="8 subjects with 3-4 samples, 4-level within-subject factor, 29 samples"),
    "GSE32473": dict(block=13474, cond=(13475, {"reference substance role": "ctrl", "betamethasone valerate delivered for duration 3 week": "betamethasone",
                                                "pimecrolimus delivered for duration 3 week": "pimecrolimus"}),
                     why="10 patients x 3 arms: three-level within-subject factor"),
    "GSE63857": dict(block=32589, cond=(21578, {"wild type genotype": "wt", "Homozygous negative  Brca1 [mouse] breast cancer 1": "brca1ko",
                                                "Overexpression of  Esr1 [mouse] estrogen receptor 1 (alpha)": "esr1oe",
                                                "Overexpression of  CYP19A1 [human] cytochrome P450, family 19, subfamily A, polypeptide 1": "cyp19a1oe"}),
                     why="RNA-seq; 7 litter blocks of size 4, 2 and 1 (a singleton block)"),
    "GSE55468": dict(block=19471, cond=None, why="26 samples, every individual measured once: no repeated measures"),
    "GSE137": dict(block=246, cond=None, why="35 individuals x 2 samples, no recorded condition: intercept-only design with a block"),
}

def get(path, **q):
    url = BASE + path + ("?" + urllib.parse.urlencode(q) if q else "")
    out = subprocess.run(["curl", "-s", "--compressed", "-m", "300", url], capture_output=True, text=True).stdout
    return json.loads(out)["data"]

def build(gse, spec):
    samples = get(f"/datasets/{gse}/samples")
    rows = []
    for s in samples:
        fv = {f["experimentalFactorId"]: f["summary"] for f in s["sample"]["factorValues"]}
        gsm = s["shortName"]
        if spec["block"] not in fv:
            raise SystemExit(f"{gse}: {gsm} has no block value")
        cond = ""
        if spec["cond"]:
            fid, levels = spec["cond"]
            cond = levels[fv[fid]]
        rows.append((gsm, re.sub(r"\W+", "_", fv[spec["block"]]), cond))
    raw = subprocess.run(["curl", "-s", "--compressed", "-m", "600", f"{BASE}/datasets/{gse}/data?filter=true"],
                         capture_output=True, text=True).stdout
    lines = [l for l in raw.split("\n") if l and not l.startswith("#")]
    header = lines[0].split("\t")
    cols = {h.split("___")[-1]: i for i, h in enumerate(header) if "___" in h}
    # a sample can be in the design without a column in the data (GSE55468 has one such sample)
    # or a column with no values at all
    body = [l.split("\t") for l in lines[1:]]
    has_values = {g for g, i in cols.items() if any(len(f) > i and f[i].strip() not in ("", "NaN") for f in body[:200])}
    rows = [r for r in rows if r[0] in cols and r[0] in has_values]
    probes = []
    for l in lines[1:]:
        f = l.split("\t")
        try:
            v = [float(f[cols[r[0]]]) for r in rows]
        except (KeyError, ValueError):
            continue
        if any(math.isnan(x) for x in v):
            continue
        m = sum(v) / len(v)
        probes.append((sum((x - m) ** 2 for x in v) / (len(v) - 1), f[0], v))
    probes.sort(reverse=True)
    top = probes[:50]
    d = os.path.join(HERE, gse)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, "design.txt"), "w") as o:
        o.write(f"# {gse} (Gemma REST /datasets/{gse}/samples): {spec['why']}\n")
        o.write("sample\tblock\tcond\n")
        for r in rows:
            o.write("\t".join(r) + "\n")
    with open(os.path.join(d, "expmat.txt"), "w") as o:
        o.write("probe\t" + "\t".join(r[0] for r in rows) + "\n")
        for _, name, v in top:
            o.write(name + "\t" + "\t".join(repr(round(x, 6)) for x in v) + "\n")
    print(gse, len(rows), "samples", len(top), "probes")

if __name__ == "__main__":
    for g in (sys.argv[1:] or SPECS):
        build(g, SPECS[g])
