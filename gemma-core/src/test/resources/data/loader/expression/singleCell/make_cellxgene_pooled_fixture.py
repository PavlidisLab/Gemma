#!/usr/bin/env python3
"""
Generate a tiny CELLxGENE-style AnnData fixture for
CellXGeneAnnDataSingleCellDataLoaderTest#testKeepPooledSample.

Source dataset (CELLxGENE, "HypoMap" -- a unified single-cell gene expression
atlas of the murine hypothalamus):
    dataset id (original): d3be7423-d664-4913-89a9-a506cae4c28f
The original dataset id was removed from CELLxGENE's public index when the atlas
was re-published, so the test can no longer download it. As of this writing
HypoMap lives under collection d86517f0-fa7e-4266-b82e-a521350d6d36, dataset
87b802cc-73ca-422a-8cc7-6d6d38449b3f. This script trims a locally cached copy of
the original .h5ad down to a small, self-contained fixture that preserves the
exact `donor_id` structure the test asserts on (23 SRR donors + a "pooled"
donor).

Only the obs (cell) dataframe matters for the test, which exercises
getSampleNames() and getSamplesCharacteristics(). We keep all obs columns but
subsample to a few cells per donor and a handful of genes, and drop the heavy
raw/layers/embeddings that the test never reads.

Run from a machine with access to the cached source file:
    python3 make_cellxgene_pooled_fixture.py \
        /path/to/d3be7423-d664-4913-89a9-a506cae4c28f.h5ad \
        cellxgene-pooled-sample.h5ad
"""
import sys
import anndata as ad
import numpy as np

CELLS_PER_DONOR = 3
N_GENES = 30


def main(src, dst):
    adata = ad.read_h5ad(src, backed="r")
    donor = adata.obs["donor_id"]
    # deterministic: first N cells (by position) for each donor category, in
    # the category order defined in the file (which is what the test asserts)
    row_idx = []
    for cat in donor.cat.categories:
        hits = np.where(donor.values == cat)[0][:CELLS_PER_DONOR]
        row_idx.extend(hits.tolist())
    row_idx = sorted(row_idx)
    gene_idx = list(range(min(N_GENES, adata.n_vars)))

    sub = adata[row_idx, gene_idx].to_memory()

    # drop everything the loader/test does not read to keep the fixture tiny
    sub.raw = None
    sub.layers.clear()
    sub.obsm.clear()
    sub.obsp.clear()
    sub.varm.clear()
    sub.varp.clear()
    sub.uns.clear()

    # densify X (values are irrelevant to the test; small so dense is fine)
    if not isinstance(sub.X, np.ndarray):
        sub.X = sub.X.toarray()

    sub.write_h5ad(dst, compression="gzip")
    print(f"wrote {dst}: {sub.n_obs} cells x {sub.n_vars} genes")
    print("donor_id categories:", list(sub.obs["donor_id"].cat.categories))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
